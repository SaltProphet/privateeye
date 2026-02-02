package dev.privateeye.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import java.io.File
import java.io.IOException

/**
 * ScreenRecordingService - MediaProjection-based screen recording service
 * 
 * Implements:
 * - MediaProjectionManager for high-fidelity screen capture
 * - MediaCodec with COLOR_FormatSurface for performance optimization
 * - Foreground service notification with Start/Stop actions
 */
class ScreenRecordingService : Service() {
    
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaCodec: MediaCodec? = null
    private var mediaMuxer: MediaMuxer? = null
    private var isRecording = false
    private var videoTrackIndex = -1
    private var muxerStarted = false
    
    private var screenWidth = 1080
    private var screenHeight = 1920
    private var screenDensity = 0
    
    companion object {
        private const val TAG = "ScreenRecordingService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "screen_recording_channel"
        
        const val ACTION_START = "dev.privateeye.action.START"
        const val ACTION_STOP = "dev.privateeye.action.STOP"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        const val EXTRA_OUTPUT_PATH = "output_path"
        
        // Video encoding settings
        private const val VIDEO_MIME_TYPE = "video/avc" // H.264
        private const val VIDEO_BIT_RATE = 6000000 // 6 Mbps
        private const val VIDEO_FRAME_RATE = 30
        private const val VIDEO_I_FRAME_INTERVAL = 1
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        
        // Get screen dimensions
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display?.getRealMetrics(displayMetrics)
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(displayMetrics)
        }
        
        screenWidth = displayMetrics.widthPixels
        screenHeight = displayMetrics.heightPixels
        screenDensity = displayMetrics.densityDpi
        
        Log.i(TAG, "Screen dimensions: ${screenWidth}x${screenHeight} @ ${screenDensity}dpi")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, -1)
                val resultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_RESULT_DATA)
                }
                val outputPath = intent.getStringExtra(EXTRA_OUTPUT_PATH)
                
                if (resultCode != -1 && resultData != null && outputPath != null) {
                    startRecording(resultCode, resultData, outputPath)
                } else {
                    Log.e(TAG, "Invalid parameters for starting recording")
                    stopSelf()
                }
            }
            ACTION_STOP -> {
                stopRecording()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screen Recording",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notification for screen recording service"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        // Create stop intent
        val stopIntent = Intent(this, ScreenRecordingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("PrivateEye Recording")
            .setContentText("Screen recording in progress")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_media_pause,
                "Stop",
                stopPendingIntent
            )
            .build()
    }
    
    private fun startRecording(resultCode: Int, resultData: Intent, outputPath: String) {
        if (isRecording) {
            Log.w(TAG, "Recording already in progress")
            return
        }
        
        // Start as foreground service
        startForeground(NOTIFICATION_ID, createNotification())
        
        try {
            // Create output directory if needed
            val outputFile = File(outputPath)
            outputFile.parentFile?.mkdirs()
            
            // Initialize MediaProjection
            val mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, resultData)
            
            if (mediaProjection == null) {
                Log.e(TAG, "Failed to create MediaProjection")
                stopSelf()
                return
            }
            
            // Initialize MediaCodec with COLOR_FormatSurface
            initializeMediaCodec(outputPath)
            
            // Create VirtualDisplay
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "PrivateEyeCapture",
                screenWidth,
                screenHeight,
                screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaCodec?.createInputSurface(),
                null,
                null
            )
            
            // Start encoding
            mediaCodec?.start()
            isRecording = true
            
            // Start encoding thread
            Thread {
                encodeLoop()
            }.start()
            
            Log.i(TAG, "Recording started: $outputPath")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting recording", e)
            stopRecording()
            stopSelf()
        }
    }
    
    private fun initializeMediaCodec(outputPath: String) {
        try {
            // Configure MediaFormat for H.264/AVC encoding
            val format = MediaFormat.createVideoFormat(VIDEO_MIME_TYPE, screenWidth, screenHeight)
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            format.setInteger(MediaFormat.KEY_BIT_RATE, VIDEO_BIT_RATE)
            format.setInteger(MediaFormat.KEY_FRAME_RATE, VIDEO_FRAME_RATE)
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, VIDEO_I_FRAME_INTERVAL)
            
            // Create and configure MediaCodec
            mediaCodec = MediaCodec.createEncoderByType(VIDEO_MIME_TYPE)
            mediaCodec?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            
            // Initialize MediaMuxer
            mediaMuxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            
            Log.i(TAG, "MediaCodec and MediaMuxer initialized")
            
        } catch (e: IOException) {
            Log.e(TAG, "Failed to initialize MediaCodec/MediaMuxer", e)
            throw e
        }
    }
    
    private fun encodeLoop() {
        val bufferInfo = MediaCodec.BufferInfo()
        var encodedFrames = 0
        
        try {
            while (isRecording) {
                val encoderStatus = mediaCodec?.dequeueOutputBuffer(bufferInfo, 10000) ?: continue
                
                when {
                    encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        // No output available yet
                    }
                    encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        // Output format changed, add track to muxer
                        if (muxerStarted) {
                            Log.e(TAG, "Format changed after muxer started")
                            break
                        }
                        
                        val newFormat = mediaCodec?.outputFormat
                        if (newFormat != null) {
                            videoTrackIndex = mediaMuxer?.addTrack(newFormat) ?: -1
                            mediaMuxer?.start()
                            muxerStarted = true
                            Log.i(TAG, "MediaMuxer started")
                        }
                    }
                    encoderStatus >= 0 -> {
                        // Got encoded data
                        val encodedData = mediaCodec?.getOutputBuffer(encoderStatus)
                        
                        if (encodedData != null && bufferInfo.size > 0 && muxerStarted) {
                            // Write encoded data to muxer
                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)
                            
                            mediaMuxer?.writeSampleData(videoTrackIndex, encodedData, bufferInfo)
                            encodedFrames++
                            
                            if (encodedFrames % 30 == 0) {
                                Log.d(TAG, "Encoded $encodedFrames frames")
                            }
                        }
                        
                        mediaCodec?.releaseOutputBuffer(encoderStatus, false)
                        
                        // Check for end of stream
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            Log.i(TAG, "End of stream reached")
                            break
                        }
                    }
                }
            }
            
            Log.i(TAG, "Encoding loop finished. Total frames: $encodedFrames")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in encoding loop", e)
        }
    }
    
    private fun stopRecording() {
        if (!isRecording) {
            return
        }
        
        isRecording = false
        
        try {
            // Signal end of stream
            mediaCodec?.signalEndOfInputStream()
            
            // Give encoder time to finish
            Thread.sleep(500)
            
            // Stop and release resources
            virtualDisplay?.release()
            virtualDisplay = null
            
            if (muxerStarted) {
                mediaMuxer?.stop()
            }
            mediaMuxer?.release()
            mediaMuxer = null
            
            mediaCodec?.stop()
            mediaCodec?.release()
            mediaCodec = null
            
            mediaProjection?.stop()
            mediaProjection = null
            
            Log.i(TAG, "Recording stopped and resources released")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopRecording()
    }
}
