package dev.privateeye.stealth;

/**
 * Interface for PrivateEye system-level service running with Shell privileges via Shizuku
 */
interface IPrivateEyeService {
    /**
     * Get the process ID of the service
     */
    int getPid();
    
    /**
     * Capture screen and save to specified path
     */
    void captureScreen(String outputPath);
    
    /**
     * Check if the service is running with elevated privileges
     */
    boolean hasShellPrivileges();
    
    /**
     * Destroy the service
     */
    void destroy();
}
