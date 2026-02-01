package dev.privateeye

import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.privateeye.stealth.IPrivateEyeService
import dev.privateeye.stealth.PrivateEyeConnector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * MainViewModel for PrivateEye app
 * Manages Ghost Mode state, Shizuku connection, and console logs
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val connector: PrivateEyeConnector
) : ViewModel() {
    
    // Ghost Mode state
    private val _isGhostModeActive = MutableStateFlow(false)
    val isGhostModeActive: StateFlow<Boolean> = _isGhostModeActive.asStateFlow()
    
    // Shizuku connection state
    private val _isShizukuConnected = MutableStateFlow(false)
    val isShizukuConnected: StateFlow<Boolean> = _isShizukuConnected.asStateFlow()
    
    // Console logs
    private val _consoleLogs = MutableStateFlow<List<String>>(emptyList())
    val consoleLogs: StateFlow<List<String>> = _consoleLogs.asStateFlow()
    
    private val connectionCallback = object : PrivateEyeConnector.ConnectionCallback {
        override fun onConnected(service: IPrivateEyeService) {
            _isShizukuConnected.value = true
            addLog("[System] PrivateEyeUserService Connected")
            try {
                val hasPrivileges = service.hasShellPrivileges()
                addLog("[System] Shell Privileges: $hasPrivileges")
            } catch (e: Exception) {
                addLog("[Error] Failed to query service: ${e.message}")
            }
        }
        
        override fun onDisconnected() {
            _isShizukuConnected.value = false
            addLog("[System] Service Disconnected")
            // Stop recording if active
            if (_isGhostModeActive.value) {
                _isGhostModeActive.value = false
                addLog("[Ghost] Recording stopped due to disconnection")
            }
        }
        
        override fun onError(message: String) {
            addLog("[Error] $message")
        }
        
        override fun onLog(message: String) {
            addLog(message)
        }
    }
    
    private val recordingCallback = object : PrivateEyeConnector.RecordingCallback {
        override fun onRecordingStarted() {
            addLog("[Ghost] Stealth capture engine activated")
        }
        
        override fun onRecordingStopped() {
            addLog("[Ghost] Stealth capture engine deactivated")
        }
        
        override fun onError(message: String) {
            addLog("[Error] $message")
            // Turn off Ghost Mode on error
            if (_isGhostModeActive.value) {
                _isGhostModeActive.value = false
            }
        }
        
        override fun onLog(message: String) {
            addLog(message)
        }
    }
    
    init {
        addLog("PrivateEye initialized")
        connector.initialize()
        checkShizukuStatus()
    }
    
    /**
     * Add a timestamped log message to the console
     * Format: [HH:mm:ss] message
     */
    fun addLog(msg: String) {
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val timestamp = timeFormat.format(Date())
        val logEntry = "[$timestamp] $msg"
        
        _consoleLogs.value = _consoleLogs.value + logEntry
    }
    
    /**
     * Toggle Ghost Mode on/off
     * When enabled, starts stealth screen recording
     * When disabled, stops recording
     */
    fun toggleGhostMode() {
        viewModelScope.launch {
            if (!_isShizukuConnected.value) {
                addLog("[Error] Cannot toggle Ghost Mode: Shizuku not connected")
                return@launch
            }
            
            val newState = !_isGhostModeActive.value
            _isGhostModeActive.value = newState
            
            if (newState) {
                // Start recording
                addLog("Ghost Mode ACTIVATED")
                startRecording()
            } else {
                // Stop recording
                addLog("Ghost Mode DEACTIVATED")
                stopRecording()
            }
        }
    }
    
    /**
     * Start stealth recording
     */
    private fun startRecording() {
        val timestamp = System.currentTimeMillis()
        val outputPath = "/storage/emulated/0/Download/PrivateEye/capture_$timestamp.mp4"
        
        addLog("[Ghost] Output: $outputPath")
        connector.startRecording(outputPath, recordingCallback)
    }
    
    /**
     * Stop stealth recording
     */
    private fun stopRecording() {
        connector.stopRecording(recordingCallback)
    }
    
    /**
     * Clear all console logs
     */
    fun clearLogs() {
        _consoleLogs.value = emptyList()
        addLog("Console cleared")
    }
    
    /**
     * Check Shizuku status and initiate connection
     * Includes security checks for permissions
     */
    fun checkShizukuStatus() {
        viewModelScope.launch {
            try {
                // Check if Shizuku is running
                val isRunning = try {
                    Shizuku.pingBinder()
                } catch (e: Exception) {
                    addLog("[Error] Shizuku Not Running")
                    _isShizukuConnected.value = false
                    return@launch
                }
                
                if (!isRunning) {
                    addLog("[Error] Shizuku Not Running")
                    _isShizukuConnected.value = false
                    return@launch
                }
                
                addLog("[System] Shizuku is running")
                
                // Security check: Verify if app has Shizuku permissions
                val hasPermission = try {
                    Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
                } catch (e: Exception) {
                    addLog("[Error] Failed to check Shizuku permission: ${e.message}")
                    false
                }
                
                if (!hasPermission) {
                    addLog("[Security] Shizuku permission not granted")
                    addLog("[System] Requesting Shizuku permission...")
                    
                    try {
                        // Request permission with request code 0
                        Shizuku.requestPermission(0)
                        addLog("[System] Permission request sent")
                    } catch (e: Exception) {
                        addLog("[Error] Failed to request permission: ${e.message}")
                    }
                    return@launch
                }
                
                addLog("[Security] Shizuku permission granted")
                
                // If Shizuku is running and we have permission, connect
                addLog("[System] Initiating connection...")
                connector.connect(connectionCallback)
                
            } catch (e: Exception) {
                addLog("[Error] Shizuku check failed: ${e.message}")
                _isShizukuConnected.value = false
            }
        }
    }
    
    /**
     * Request Shizuku permission manually
     */
    fun requestShizukuPermission() {
        viewModelScope.launch {
            try {
                addLog("[System] Requesting Shizuku permission...")
                Shizuku.requestPermission(0)
            } catch (e: Exception) {
                addLog("[Error] Failed to request permission: ${e.message}")
            }
        }
    }
    
    /**
     * Reconnect to Shizuku service
     */
    fun reconnect() {
        checkShizukuStatus()
    }
    
    override fun onCleared() {
        super.onCleared()
        connector.disconnect()
        connector.cleanup()
    }
}
