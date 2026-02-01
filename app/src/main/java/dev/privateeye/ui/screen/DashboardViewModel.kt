package dev.privateeye.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.privateeye.common.log.LogEntry
import dev.privateeye.common.log.Logger
import dev.privateeye.stealth.ShizukuManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Dashboard screen
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val shizukuManager: ShizukuManager,
    private val logger: Logger
) : ViewModel() {
    
    companion object {
        private const val TAG = "DashboardViewModel"
    }
    
    // Logs from Logger
    val logs: StateFlow<List<LogEntry>> = logger.logs
    
    // Shizuku status
    private val _shizukuStatus = MutableStateFlow(ShizukuStatus())
    val shizukuStatus: StateFlow<ShizukuStatus> = _shizukuStatus.asStateFlow()
    
    init {
        logger.info(TAG, "PrivateEye initialized")
        checkShizukuStatus()
    }
    
    /**
     * Check and update Shizuku status
     */
    fun checkShizukuStatus() {
        viewModelScope.launch {
            val status = ShizukuStatus(
                isInstalled = shizukuManager.isShizukuInstalled(),
                isRunning = shizukuManager.isShizukuRunning(),
                hasPermission = shizukuManager.hasShizukuPermission(),
                isServiceBound = shizukuManager.getService() != null
            )
            _shizukuStatus.value = status
            
            logger.info(TAG, "Shizuku installed: ${status.isInstalled}")
            logger.info(TAG, "Shizuku running: ${status.isRunning}")
            logger.info(TAG, "Shizuku permission: ${status.hasPermission}")
            logger.info(TAG, "Service bound: ${status.isServiceBound}")
        }
    }
    
    /**
     * Request Shizuku permission
     */
    fun requestShizukuPermission() {
        viewModelScope.launch {
            shizukuManager.requestShizukuPermission()
            // Wait a bit and check status again
            kotlinx.coroutines.delay(500)
            checkShizukuStatus()
        }
    }
    
    /**
     * Bind to Shizuku service
     */
    fun bindService() {
        viewModelScope.launch {
            shizukuManager.bindService()
            // Wait a bit for binding to complete
            kotlinx.coroutines.delay(1000)
            checkShizukuStatus()
        }
    }
    
    /**
     * Capture screen
     */
    fun captureScreen() {
        viewModelScope.launch {
            val outputPath = "/sdcard/Pictures/privateeye_${System.currentTimeMillis()}.png"
            shizukuManager.captureScreen(outputPath)
        }
    }
    
    /**
     * Clear logs
     */
    fun clearLogs() {
        logger.clear()
        logger.info(TAG, "Logs cleared")
    }
    
    override fun onCleared() {
        super.onCleared()
        shizukuManager.unbindService()
    }
}

/**
 * Data class representing Shizuku status
 */
data class ShizukuStatus(
    val isInstalled: Boolean = false,
    val isRunning: Boolean = false,
    val hasPermission: Boolean = false,
    val isServiceBound: Boolean = false
)
