package dev.privateeye.stealth

import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.privateeye.common.log.Logger
import rikka.shizuku.Shizuku
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for Shizuku integration
 * 
 * Handles Shizuku permission requests, service binding, and communication
 * with PrivateEyeUserService running with Shell privileges.
 */
@Singleton
class ShizukuManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: Logger
) {
    companion object {
        private const val TAG = "ShizukuManager"
        private const val SHIZUKU_PERMISSION_REQUEST_CODE = 1001
    }
    
    private var service: IPrivateEyeService? = null
    
    /**
     * Check if Shizuku is installed on the device
     */
    fun isShizukuInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
    
    /**
     * Check if Shizuku is running
     */
    fun isShizukuRunning(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check if we have Shizuku permission
     */
    fun hasShizukuPermission(): Boolean {
        return if (isShizukuRunning()) {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
    }
    
    /**
     * Request Shizuku permission
     */
    fun requestShizukuPermission() {
        if (isShizukuRunning() && !hasShizukuPermission()) {
            try {
                Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
                logger.info(TAG, "Shizuku permission requested")
            } catch (e: Exception) {
                logger.error(TAG, "Failed to request Shizuku permission: ${e.message}")
            }
        }
    }
    
    /**
     * Bind to PrivateEyeUserService
     */
    fun bindService() {
        try {
            if (!hasShizukuPermission()) {
                logger.warning(TAG, "Cannot bind service: Shizuku permission not granted")
                return
            }
            
            val userServiceArgs = Shizuku.UserServiceArgs(
                android.content.ComponentName(
                    context.packageName,
                    PrivateEyeUserService::class.java.name
                )
            )
                .daemon(false)
                .processNameSuffix("privateeye_service")
                .debuggable(false)
                .version(1)
            
            Shizuku.bindUserService(userServiceArgs, object : android.content.ServiceConnection {
                override fun onServiceConnected(name: android.content.ComponentName?, binder: android.os.IBinder?) {
                    if (binder != null && binder.pingBinder()) {
                        service = IPrivateEyeService.Stub.asInterface(binder)
                        logger.info(TAG, "PrivateEyeUserService connected successfully")
                        
                        // Log service info
                        service?.let {
                            val hasPrivileges = it.hasShellPrivileges()
                            logger.info(TAG, "Service PID: ${it.getPid()}, Shell privileges: $hasPrivileges")
                        }
                    }
                }
                
                override fun onServiceDisconnected(name: android.content.ComponentName?) {
                    service = null
                    logger.warning(TAG, "PrivateEyeUserService disconnected")
                }
            })
        } catch (e: Exception) {
            logger.error(TAG, "Failed to bind service: ${e.message}")
        }
    }
    
    /**
     * Unbind from PrivateEyeUserService
     */
    fun unbindService() {
        try {
            service?.destroy()
            service = null
            logger.info(TAG, "Service unbound")
        } catch (e: Exception) {
            logger.error(TAG, "Error unbinding service: ${e.message}")
        }
    }
    
    /**
     * Capture screen using the Shizuku service
     */
    fun captureScreen(outputPath: String) {
        try {
            service?.captureScreen(outputPath)
            logger.info(TAG, "Screen capture initiated: $outputPath")
        } catch (e: Exception) {
            logger.error(TAG, "Failed to capture screen: ${e.message}")
        }
    }
    
    /**
     * Get the current service instance
     */
    fun getService(): IPrivateEyeService? = service
}
