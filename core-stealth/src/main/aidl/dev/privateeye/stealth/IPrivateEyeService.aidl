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
     * Start recording screen with stealth capture engine
     */
    void startRecording(String outputPath);
    
    /**
     * Stop recording screen
     */
    void stopRecording();
    
    /**
     * Check if currently recording
     */
    boolean isRecording();
    
    /**
     * Destroy the service
     */
    void destroy();
}
