package dev.privateeye.stealth

import android.os.Process
import java.io.File

/**
 * PrivateEye User Service running with Shell privileges via Shizuku
 * 
 * This service implements IPrivateEyeService and runs in a separate process
 * with elevated Shell privileges granted by Shizuku framework.
 */
class PrivateEyeUserService : IPrivateEyeService.Stub() {
    
    companion object {
        private const val TAG = "PrivateEyeUserService"
    }
    
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
            // Ensure output directory exists
            val outputFile = File(outputPath)
            outputFile.parentFile?.mkdirs()
            
            // Execute screencap command with shell privileges
            val process = Runtime.getRuntime().exec(arrayOf("screencap", "-p", outputPath))
            val exitCode = process.waitFor()
            
            if (exitCode == 0) {
                android.util.Log.i(TAG, "Screen captured successfully to: $outputPath")
            } else {
                android.util.Log.e(TAG, "Screen capture failed with exit code: $exitCode")
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error capturing screen", e)
            throw e
        }
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
        android.util.Log.i(TAG, "PrivateEyeUserService destroyed")
        System.exit(0)
    }
}
