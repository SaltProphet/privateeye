package dev.privateeye

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.privateeye.service.OverlayService
import dev.privateeye.service.ScreenRecordingService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * RecordingViewModel - Manages MediaProjection-based screen recording
 */
@HiltViewModel
class RecordingViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()
    
    private val _consoleLogs = MutableStateFlow<List<String>>(emptyList())
    val consoleLogs: StateFlow<List<String>> = _consoleLogs.asStateFlow()
    
    private val _hasOverlayPermission = MutableStateFlow(false)
    val hasOverlayPermission: StateFlow<Boolean> = _hasOverlayPermission.asStateFlow()
    
    private var mediaProjectionResultCode: Int = Activity.RESULT_CANCELED
    private var mediaProjectionResultData: Intent? = null
    
    private val recordingReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                OverlayService.ACTION_START_RECORDING -> {
                    startRecording()
                }
                OverlayService.ACTION_STOP_RECORDING -> {
                    stopRecording()
                }
            }
        }
    }
    
    init {
        addLog("PrivateEye MediaProjection Recorder initialized")
        checkOverlayPermission()
        registerRecordingReceiver()
    }
    
    private fun registerRecordingReceiver() {
        val filter = IntentFilter().apply {
            addAction(OverlayService.ACTION_START_RECORDING)
            addAction(OverlayService.ACTION_STOP_RECORDING)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(recordingReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(recordingReceiver, filter)
        }
    }
    
    fun addLog(msg: String) {
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val timestamp = timeFormat.format(Date())
        val logEntry = "[$timestamp] $msg"
        
        _consoleLogs.value = _consoleLogs.value + logEntry
    }
    
    fun clearLogs() {
        _consoleLogs.value = emptyList()
        addLog("Console cleared")
    }
    
    fun checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            _hasOverlayPermission.value = Settings.canDrawOverlays(context)
            if (_hasOverlayPermission.value) {
                addLog("[System] Overlay permission granted")
            } else {
                addLog("[System] Overlay permission required")
            }
        } else {
            _hasOverlayPermission.value = true
        }
    }
    
    fun requestOverlayPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(context)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:${context.packageName}")
                )
                activity.startActivity(intent)
                addLog("[System] Opening overlay permission settings")
            }
        }
    }
    
    fun requestMediaProjection(launcher: ActivityResultLauncher<Intent>) {
        viewModelScope.launch {
            try {
                val mediaProjectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                val intent = mediaProjectionManager.createScreenCaptureIntent()
                launcher.launch(intent)
                addLog("[System] Requesting MediaProjection permission")
            } catch (e: Exception) {
                addLog("[Error] Failed to request MediaProjection: ${e.message}")
            }
        }
    }
    
    fun handleMediaProjectionResult(resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            mediaProjectionResultCode = resultCode
            mediaProjectionResultData = data
            addLog("[System] MediaProjection permission granted")
            addLog("[Info] Ready to start recording")
        } else {
            addLog("[Error] MediaProjection permission denied")
        }
    }
    
    fun startRecording() {
        if (_isRecording.value) {
            addLog("[Warning] Recording already in progress")
            return
        }
        
        if (mediaProjectionResultData == null) {
            addLog("[Error] MediaProjection not initialized. Request permission first.")
            return
        }
        
        viewModelScope.launch {
            try {
                val timestamp = System.currentTimeMillis()
                val outputPath = "/storage/emulated/0/Download/PrivateEye/recording_$timestamp.mp4"
                
                val intent = Intent(context, ScreenRecordingService::class.java).apply {
                    action = ScreenRecordingService.ACTION_START
                    putExtra(ScreenRecordingService.EXTRA_RESULT_CODE, mediaProjectionResultCode)
                    putExtra(ScreenRecordingService.EXTRA_RESULT_DATA, mediaProjectionResultData)
                    putExtra(ScreenRecordingService.EXTRA_OUTPUT_PATH, outputPath)
                }
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                
                _isRecording.value = true
                addLog("[Recording] Started: $outputPath")
                
            } catch (e: Exception) {
                addLog("[Error] Failed to start recording: ${e.message}")
            }
        }
    }
    
    fun stopRecording() {
        if (!_isRecording.value) {
            addLog("[Warning] No recording in progress")
            return
        }
        
        viewModelScope.launch {
            try {
                val intent = Intent(context, ScreenRecordingService::class.java).apply {
                    action = ScreenRecordingService.ACTION_STOP
                }
                context.startService(intent)
                
                _isRecording.value = false
                addLog("[Recording] Stopped")
                
            } catch (e: Exception) {
                addLog("[Error] Failed to stop recording: ${e.message}")
            }
        }
    }
    
    fun startOverlayService() {
        if (!_hasOverlayPermission.value) {
            addLog("[Error] Overlay permission not granted")
            return
        }
        
        viewModelScope.launch {
            try {
                val intent = Intent(context, OverlayService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                addLog("[System] Overlay service started")
            } catch (e: Exception) {
                addLog("[Error] Failed to start overlay: ${e.message}")
            }
        }
    }
    
    fun stopOverlayService() {
        viewModelScope.launch {
            try {
                val intent = Intent(context, OverlayService::class.java)
                context.stopService(intent)
                addLog("[System] Overlay service stopped")
            } catch (e: Exception) {
                addLog("[Error] Failed to stop overlay: ${e.message}")
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        try {
            context.unregisterReceiver(recordingReceiver)
        } catch (e: Exception) {
            // Receiver not registered
        }
    }
}
