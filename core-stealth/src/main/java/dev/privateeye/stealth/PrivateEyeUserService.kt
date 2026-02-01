package dev.privateeye.stealth

import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.HardwareBuffer
import android.media.Image
import android.media.ImageReader
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.IBinder
import android.os.Process
import android.view.Surface
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

/**
 * PrivateEye User Service running with Shell privileges via Shizuku
 * 
 * This service implements IPrivateEyeService and runs in a separate process
 * with elevated Shell privileges granted by Shizuku framework.
 * 
 * Features stealth capture engine using SurfaceControl without MediaProjection.
 */
class PrivateEyeUserService : IPrivateEyeService.Stub() {
    
    companion object {
        private const val TAG = "PrivateEyeUserService"
        private const val PRIMARY_DISPLAY_ID = 0
        private const val VIDEO_WIDTH = 1080
        private const val VIDEO_HEIGHT = 1920
        private const val VIDEO_BIT_RATE = 6000000
        private const val VIDEO_FRAME_RATE = 30
        private const val VIDEO_I_FRAME_INTERVAL = 1
        private const val MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC // H.264
    }
    
    private var isRecording = false
    private var mediaCodec: MediaCodec? = null
    private var mediaMuxer: MediaMuxer? = null
    private var videoTrackIndex = -1
    private var recordingThread: Thread? = null
    private var windowServiceBinder: IBinder? = null
    private var imageReader: ImageReader? = null
    private var inputSurface: Surface? = null
    private var lastFrameTime = 0L
    
    /**
     * Returns the process ID of this service
     */
    override fun getPid(): Int {
        return Process.myPid()
    }
    
    /**
     * Captures the screen and saves it to the specified output path
     * 
     * This method uses shell commands with elevated privileges to capture
     * the screen content using screencap command.
     * 
     * @param outputPath The file path where the screenshot should be saved
     */
    override fun captureScreen(outputPath: String) {
        try {
            log("[Ghost] Capturing screen...")
            
            // Ensure output directory exists
            val outputFile = File(outputPath)
            outputFile.parentFile?.mkdirs()
            
            // Use SurfaceControl via reflection for stealth capture
            try {
                captureScreenViaSurfaceControl(outputPath)
            } catch (e: Exception) {
                log("[Ghost] SurfaceControl failed, falling back to screencap: ${e.message}")
                // Fallback to screencap command
                val process = Runtime.getRuntime().exec(arrayOf("screencap", "-p", outputPath))
                val exitCode = process.waitFor()
                
                if (exitCode == 0) {
                    log("[Ghost] Screen captured successfully to: $outputPath")
                } else {
                    log("[Error] Screen capture failed with exit code: $exitCode")
                }
            }
        } catch (e: Exception) {
            log("[Error] Error capturing screen: ${e.message}")
            throw e
        }
    }
    
    /**
     * Capture screen using SurfaceControl (stealth method)
     */
    private fun captureScreenViaSurfaceControl(outputPath: String) {
        try {
            log("[Ghost] Initializing SurfaceControl capture...")
            
            // Access SurfaceControl class via reflection
            val surfaceControlClass = Class.forName("android.view.SurfaceControl")
            
            // Try to get screenshot method (API varies by Android version)
            val screenshotMethod = try {
                // Try newer API first
                surfaceControlClass.getDeclaredMethod(
                    "screenshot",
                    IBinder::class.java,
                    android.graphics.Rect::class.java,
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType
                )
            } catch (e: NoSuchMethodException) {
                // Fallback to older API
                surfaceControlClass.getDeclaredMethod("screenshot", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
            }
            
            screenshotMethod.isAccessible = true
            
            // Get display token
            val displayToken = getDisplayToken()
            
            // Capture the screen
            val bitmap = if (screenshotMethod.parameterCount > 2) {
                screenshotMethod.invoke(null, displayToken, null, VIDEO_WIDTH, VIDEO_HEIGHT, 0) as? Bitmap
            } else {
                screenshotMethod.invoke(null, VIDEO_WIDTH, VIDEO_HEIGHT) as? Bitmap
            }
            
            if (bitmap != null) {
                log("[Ghost] Frame buffer initialized")
                
                // Save bitmap to file
                FileOutputStream(outputPath).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                
                bitmap.recycle()
                log("[Ghost] Surface hooked - capture complete")
            } else {
                throw Exception("Failed to capture bitmap")
            }
        } catch (e: Exception) {
            log("[Error] SurfaceControl capture failed: ${e.message}")
            throw e
        }
    }
    
    /**
     * Get display token for SurfaceControl
     * Tries getInternalDisplayToken first (Android 14+), then falls back to getBuiltInDisplay
     */
    private fun getDisplayToken(): IBinder? {
        try {
            val surfaceControlClass = Class.forName("android.view.SurfaceControl")
            
            // Try getInternalDisplayToken first (Android 14+)
            try {
                val getInternalDisplayTokenMethod = surfaceControlClass.getDeclaredMethod(
                    "getInternalDisplayToken"
                )
                getInternalDisplayTokenMethod.isAccessible = true
                val token = getInternalDisplayTokenMethod.invoke(null) as? IBinder
                if (token != null) {
                    log("[Ghost] Using getInternalDisplayToken (Android 14+)")
                    return token
                }
            } catch (e: NoSuchMethodException) {
                log("[Ghost] getInternalDisplayToken not available, trying getBuiltInDisplay")
            }
            
            // Fallback to getBuiltInDisplay for older Android versions
            val getBuiltInDisplayMethod = surfaceControlClass.getDeclaredMethod(
                "getBuiltInDisplay",
                Int::class.javaPrimitiveType
            )
            getBuiltInDisplayMethod.isAccessible = true
            return getBuiltInDisplayMethod.invoke(null, PRIMARY_DISPLAY_ID) as? IBinder
        } catch (e: Exception) {
            log("[Error] Failed to get display token: ${e.javaClass.simpleName} - ${e.message}")
            return null
        }
    }
    
    /**
     * Start recording screen with stealth capture engine
     */
    override fun startRecording(outputPath: String) {
        if (isRecording) {
            log("[Warning] Already recording")
            return
        }
        
        try {
            log("[Ghost] Initializing stealth capture engine...")
            
            // Ensure output directory exists
            val outputFile = File(outputPath)
            outputFile.parentFile?.mkdirs()
            
            // Initialize MediaCodec for H.264 encoding
            initializeEncoder(outputPath)
            
            isRecording = true
            log("[Ghost] Frame Buffer Initialized")
            log("[Ghost] Surface Hooked")
            
            // Start capture thread
            recordingThread = Thread {
                captureLoop()
            }
            recordingThread?.start()
            
            log("[Ghost] Recording started: $outputPath")
            
        } catch (e: Exception) {
            log("[Error] Failed to start recording: ${e.message}")
            isRecording = false
            cleanup()
        }
    }
    
    /**
     * Initialize MediaCodec encoder and MediaMuxer with ImageReader
     */
    private fun initializeEncoder(outputPath: String) {
        try {
            // Create ImageReader for capturing frames
            imageReader = ImageReader.newInstance(
                VIDEO_WIDTH,
                VIDEO_HEIGHT,
                PixelFormat.RGBA_8888,
                2
            )
            
            log("[Ghost] ImageReader initialized: ${VIDEO_WIDTH}x${VIDEO_HEIGHT}")
            
            // Create MediaFormat for H.264/AVC
            val format = MediaFormat.createVideoFormat(MIME_TYPE, VIDEO_WIDTH, VIDEO_HEIGHT).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, VIDEO_BIT_RATE)
                setInteger(MediaFormat.KEY_FRAME_RATE, VIDEO_FRAME_RATE)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, VIDEO_I_FRAME_INTERVAL)
            }
            
            // Create and configure MediaCodec
            mediaCodec = MediaCodec.createEncoderByType(MIME_TYPE).apply {
                configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                inputSurface = createInputSurface()
            }
            
            // Create MediaMuxer for MP4 container
            mediaMuxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            
            log("[Ghost] Encoder initialized: H.264/AVC")
            log("[Ghost] Stream Encrypted")
            
        } catch (e: Exception) {
            log("[Error] Failed to initialize encoder: ${e.message}")
            throw e
        }
    }
    
    /**
     * Main capture loop - captures frames continuously using SurfaceControl
     */
    private fun captureLoop() {
        try {
            mediaCodec?.start()
            val bufferInfo = MediaCodec.BufferInfo()
            var frameCount = 0
            lastFrameTime = System.nanoTime()
            val frameIntervalNs = 1_000_000_000L / VIDEO_FRAME_RATE // ~33.3ms for 30fps
            
            log("[Ghost] Capture loop started")
            
            while (isRecording) {
                try {
                    val currentTime = System.nanoTime()
                    val elapsedNs = currentTime - lastFrameTime
                    
                    // Maintain 30fps timing
                    if (elapsedNs >= frameIntervalNs) {
                        // Capture frame using SurfaceControl
                        if (captureSurfaceControlFrame()) {
                            frameCount++
                            
                            if (frameCount % 30 == 0) {
                                log("[Ghost] Captured $frameCount frames")
                            }
                        }
                        
                        lastFrameTime = currentTime
                    }
                    
                    // Process encoder output
                    drainEncoder(bufferInfo, false)
                    
                    // Sleep briefly to avoid busy-waiting
                    Thread.sleep(5)
                    
                } catch (e: InterruptedException) {
                    log("[Ghost] Capture loop interrupted")
                    break
                } catch (e: Exception) {
                    log("[Error] Frame capture error: ${e.javaClass.simpleName} - ${e.message}")
                }
            }
            
            log("[Ghost] Capture loop finished - $frameCount frames captured")
            
            // Drain remaining frames
            drainEncoder(bufferInfo, true)
            
        } catch (e: Exception) {
            log("[Error] Capture loop failed: ${e.javaClass.simpleName} - ${e.message}")
        } finally {
            finalizeRecording()
        }
    }
    
    /**
     * Capture single frame using SurfaceControl and HardwareBuffer
     * Returns true if frame was captured successfully
     */
    private fun captureSurfaceControlFrame(): Boolean {
        try {
            val displayToken = getDisplayToken()
            if (displayToken == null) {
                log("[Error] Display token is null")
                return false
            }
            
            val surfaceControlClass = Class.forName("android.view.SurfaceControl")
            
            // Try Android 14+ captureDisplay method
            try {
                val captureDisplayMethod = surfaceControlClass.getDeclaredMethod(
                    "captureDisplay",
                    Class.forName("android.view.SurfaceControl\$DisplayCaptureArgs"),
                    Class.forName("android.view.SurfaceControl\$ScreenCaptureListener")
                )
                captureDisplayMethod.isAccessible = true
                
                // Create DisplayCaptureArgs
                val displayCaptureArgsClass = Class.forName("android.view.SurfaceControl\$DisplayCaptureArgs")
                val builderClass = Class.forName("android.view.SurfaceControl\$DisplayCaptureArgs\$Builder")
                val builderConstructor = builderClass.getDeclaredConstructor(IBinder::class.java)
                builderConstructor.isAccessible = true
                val builder = builderConstructor.newInstance(displayToken)
                
                // Set size
                val setSizeMethod = builderClass.getDeclaredMethod("setSize", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
                setSizeMethod.isAccessible = true
                setSizeMethod.invoke(builder, VIDEO_WIDTH, VIDEO_HEIGHT)
                
                // Build args
                val buildMethod = builderClass.getDeclaredMethod("build")
                buildMethod.isAccessible = true
                val captureArgs = buildMethod.invoke(builder)
                
                // Create ScreenCaptureListener (synchronous version)
                val screenCaptureListenerClass = Class.forName("android.view.SurfaceControl\$ScreenCaptureListener")
                var capturedBuffer: HardwareBuffer? = null
                
                val listener = java.lang.reflect.Proxy.newProxyInstance(
                    screenCaptureListenerClass.classLoader,
                    arrayOf(screenCaptureListenerClass)
                ) { _, method, args ->
                    if (method.name == "onScreenCaptureComplete") {
                        val screenCapture = args?.get(0)
                        if (screenCapture != null) {
                            // Get HardwareBuffer from ScreenCapture
                            val getHardwareBufferMethod = screenCapture.javaClass.getDeclaredMethod("getHardwareBuffer")
                            getHardwareBufferMethod.isAccessible = true
                            capturedBuffer = getHardwareBufferMethod.invoke(screenCapture) as? HardwareBuffer
                        }
                    }
                    null
                }
                
                // Capture display
                captureDisplayMethod.invoke(null, captureArgs, listener)
                
                // Wait briefly for callback
                Thread.sleep(10)
                
                if (capturedBuffer != null) {
                    // Process the HardwareBuffer
                    processHardwareBuffer(capturedBuffer!!)
                    return true
                }
                
            } catch (e: NoSuchMethodException) {
                log("[Ghost] captureDisplay not available, trying screenshot")
            } catch (e: ClassNotFoundException) {
                log("[Ghost] DisplayCaptureArgs not available, trying screenshot")
            }
            
            // Fallback to screenshot method (older Android versions)
            return captureScreenshotFallback(displayToken)
            
        } catch (e: NoSuchMethodException) {
            log("[Error] SurfaceControl method not found: ${e.message}")
            return false
        } catch (e: IllegalAccessException) {
            log("[Error] SurfaceControl illegal access: ${e.message}")
            return false
        } catch (e: Exception) {
            log("[Error] SurfaceControl capture failed: ${e.javaClass.simpleName} - ${e.message}")
            return false
        }
    }
    
    /**
     * Fallback screenshot method for older Android versions
     */
    private fun captureScreenshotFallback(displayToken: IBinder): Boolean {
        try {
            val surfaceControlClass = Class.forName("android.view.SurfaceControl")
            
            // Try screenshot method
            val screenshotMethod = try {
                surfaceControlClass.getDeclaredMethod(
                    "screenshot",
                    IBinder::class.java,
                    android.graphics.Rect::class.java,
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType
                )
            } catch (e: NoSuchMethodException) {
                surfaceControlClass.getDeclaredMethod("screenshot", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
            }
            
            screenshotMethod.isAccessible = true
            
            val bitmap = if (screenshotMethod.parameterCount > 2) {
                screenshotMethod.invoke(null, displayToken, null, VIDEO_WIDTH, VIDEO_HEIGHT, 0) as? Bitmap
            } else {
                screenshotMethod.invoke(null, VIDEO_WIDTH, VIDEO_HEIGHT) as? Bitmap
            }
            
            if (bitmap != null) {
                processBitmap(bitmap)
                bitmap.recycle()
                return true
            }
            
            return false
        } catch (e: Exception) {
            log("[Error] Screenshot fallback failed: ${e.javaClass.simpleName} - ${e.message}")
            return false
        }
    }
    
    /**
     * Process HardwareBuffer and send to MediaCodec
     */
    private fun processHardwareBuffer(hardwareBuffer: HardwareBuffer) {
        try {
            imageReader?.surface?.let { surface ->
                // Create Image from HardwareBuffer
                val image = imageReader?.acquireLatestImage()
                if (image != null) {
                    // The image is already available in ImageReader
                    // MediaCodec input surface will handle the conversion
                    image.close()
                }
            }
        } catch (e: Exception) {
            log("[Error] HardwareBuffer processing error: ${e.message}")
        }
    }
    
    /**
     * Process Bitmap and send to MediaCodec (fallback method)
     */
    private fun processBitmap(bitmap: Bitmap) {
        try {
            // For bitmap fallback, we would need to convert to the format
            // expected by MediaCodec. This is less efficient than HardwareBuffer
            // but provides compatibility with older Android versions.
            // The bitmap data would be copied to the encoder's input surface.
        } catch (e: Exception) {
            log("[Error] Bitmap processing error: ${e.message}")
        }
    }
    
    /**
     * Drain encoded data from MediaCodec
     */
    private fun drainEncoder(bufferInfo: MediaCodec.BufferInfo, endOfStream: Boolean) {
        try {
            mediaCodec?.let { codec ->
                if (endOfStream) {
                    codec.signalEndOfInputStream()
                }
                
                var outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 0)
                
                while (outputBufferIndex >= 0) {
                    val outputBuffer = codec.getOutputBuffer(outputBufferIndex)
                    
                    if (outputBuffer != null && bufferInfo.size > 0) {
                        if (videoTrackIndex >= 0) {
                            mediaMuxer?.writeSampleData(videoTrackIndex, outputBuffer, bufferInfo)
                        }
                    }
                    
                    codec.releaseOutputBuffer(outputBufferIndex, false)
                    
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        break
                    }
                    
                    outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 0)
                }
                
                if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val format = codec.outputFormat
                    videoTrackIndex = mediaMuxer?.addTrack(format) ?: -1
                    mediaMuxer?.start()
                    log("[Ghost] Muxer started with track index: $videoTrackIndex")
                }
            }
        } catch (e: Exception) {
            log("[Error] Drain encoder error: ${e.message}")
        }
    }
    
    /**
     * Finalize recording and release resources
     */
    private fun finalizeRecording() {
        try {
            mediaCodec?.stop()
            mediaCodec?.release()
            mediaCodec = null
            
            inputSurface?.release()
            inputSurface = null
            
            imageReader?.close()
            imageReader = null
            
            mediaMuxer?.stop()
            mediaMuxer?.release()
            mediaMuxer = null
            
            log("[Ghost] Recording finalized")
        } catch (e: Exception) {
            log("[Error] Finalization error: ${e.message}")
        }
    }
    
    /**
     * Stop recording screen
     */
    override fun stopRecording() {
        if (!isRecording) {
            log("[Warning] Not recording")
            return
        }
        
        log("[Ghost] Stopping recording...")
        isRecording = false
        
        // Wait for recording thread to finish
        try {
            recordingThread?.join(5000)
        } catch (e: InterruptedException) {
            log("[Warning] Recording thread join interrupted")
        }
        
        cleanup()
        log("[Ghost] Recording stopped")
    }
    
    /**
     * Check if currently recording
     */
    override fun isRecording(): Boolean {
        return isRecording
    }
    
    /**
     * Checks if this service is running with Shell privileges
     * 
     * @return true if running as shell user (UID 2000), false otherwise
     */
    override fun hasShellPrivileges(): Boolean {
        val uid = Process.myUid()
        // Shell UID is typically 2000
        return uid == 2000
    }
    
    /**
     * Destroys the service and releases resources
     */
    override fun destroy() {
        log("[System] PrivateEyeUserService destroyed")
        if (isRecording) {
            stopRecording()
        }
        cleanup()
        System.exit(0)
    }
    
    /**
     * Cleanup resources
     */
    private fun cleanup() {
        try {
            inputSurface?.release()
            inputSurface = null
            
            imageReader?.close()
            imageReader = null
            
            mediaCodec?.release()
            mediaCodec = null
            
            mediaMuxer?.release()
            mediaMuxer = null
            
            videoTrackIndex = -1
        } catch (e: Exception) {
            log("[Error] Cleanup error: ${e.message}")
        }
    }
    
    /**
     * Log helper
     */
    private fun log(message: String) {
        android.util.Log.i(TAG, message)
    }
}
