# PrivateEye - Build Instructions

## Overview
PrivateEye is a modular Android application built with Kotlin and Jetpack Compose that provides stealth screen capture capabilities through Shizuku integration.

## Prerequisites
- Android Studio (Arctic Fox or later)
- Android SDK 26+
- JDK 8 or higher
- Shizuku app installed on target Android device

## Project Structure
```
privateeye/
├── app/                      # Main application module (UI layer)
│   ├── src/main/
│   │   ├── java/dev/privateeye/
│   │   │   ├── MainActivity.kt
│   │   │   ├── MainScreen.kt
│   │   │   ├── MainViewModel.kt
│   │   │   ├── PrivateEyeApplication.kt
│   │   │   └── ui/
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── core-stealth/             # Shizuku integration and stealth capture
│   ├── src/main/
│   │   ├── aidl/dev/privateeye/stealth/
│   │   │   └── IPrivateEyeService.aidl
│   │   └── java/dev/privateeye/stealth/
│   │       ├── PrivateEyeConnector.kt
│   │       ├── PrivateEyeUserService.kt
│   │       └── ShizukuManager.kt
│   └── build.gradle.kts
├── common/                   # Shared utilities and logging
│   ├── src/main/java/dev/privateeye/common/log/
│   │   ├── LogEntry.kt
│   │   └── Logger.kt
│   └── build.gradle.kts
├── gradle/
│   └── libs.versions.toml    # Centralized dependency management
├── build.gradle.kts
└── settings.gradle.kts
```

## Building the Project

### Using Android Studio
1. Open Android Studio
2. Select "Open an Existing Project"
3. Navigate to the `privateeye` directory and select it
4. Wait for Gradle sync to complete
5. Build the project: Build → Make Project
6. Run on device: Run → Run 'app'

### Using Command Line (with Android SDK configured)
```bash
# Sync dependencies
./gradlew --refresh-dependencies

# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Run tests
./gradlew test
```

## Key Features Implemented

### 1. Modular Architecture
- **:app** - UI module with Jetpack Compose
- **:core-stealth** - Shizuku integration and capture engine
- **:common** - Shared logging and utilities

### 2. Shizuku Integration
- PrivateEyeConnector implements Shizuku listeners
- Automatic permission handling
- Service binding with elevated privileges
- PrivateEyeUserService runs with Shell UID (2000)

### 3. Stealth Capture Engine
- SurfaceControl-based screen capture (no MediaProjection)
- MediaCodec H.264/AVC encoding pipeline
- MediaMuxer for MP4 container format
- Output to `/storage/emulated/0/Download/PrivateEye/`

### 4. UI Features
- Pure black (#000000) background with Matrix Green (#00FF00) theme
- Terminal-style monospace console with auto-scrolling
- Ghost Mode toggle for start/stop recording
- Real-time log display with timestamps [HH:mm:ss]
- Connection status LED indicator

### 5. Security
- Shizuku permission verification (Shizuku.checkSelfPermission)
- Permission request flow (Shizuku.requestPermission(0))
- Shell privilege validation (UID check)

## Required Permissions (AndroidManifest.xml)
- `moe.shizuku.manager.permission.API_V23` - Shizuku API access
- `android.permission.PROJECT_MEDIA` - Media projection
- `android.permission.POST_NOTIFICATIONS` - Notifications (Android 13+)
- `android.permission.WRITE_EXTERNAL_STORAGE` - File storage
- `android.permission.READ_EXTERNAL_STORAGE` - File access
- `android.permission.RECORD_AUDIO` - Audio capture
- `android.permission.FOREGROUND_SERVICE` - Service management

## Dependencies
- Kotlin 1.9.20
- Jetpack Compose (BOM 2024.01.00)
- Hilt 2.48.1 (Dependency Injection)
- Shizuku API 13.1.5
- Android Gradle Plugin 8.2.0

## Running the App

### Setup Shizuku
1. Install Shizuku app from GitHub or Play Store
2. Start Shizuku service (via ADB or root)
3. Grant PrivateEye permission in Shizuku settings

### Using PrivateEye
1. Launch PrivateEye app
2. Check connection status LED (green = connected)
3. Grant Shizuku permission when prompted
4. Wait for "Service Connected" log message
5. Toggle "Ghost Mode" to start/stop recording
6. View real-time logs in the console
7. Recordings saved to Download/PrivateEye/

## AIDL Interface (IPrivateEyeService)
```java
interface IPrivateEyeService {
    int getPid();                           // Get service process ID
    void captureScreen(String outputPath);  // Single screenshot
    boolean hasShellPrivileges();           // Check UID 2000
    void startRecording(String outputPath); // Start video recording
    void stopRecording();                   // Stop video recording
    boolean isRecording();                  // Check recording status
    void destroy();                         // Terminate service
}
```

## Troubleshooting

### Shizuku Not Running
- Ensure Shizuku service is started
- Check ADB connection if using wireless debugging
- Restart Shizuku service

### Permission Denied
- Grant Shizuku permission in app settings
- Check Shizuku permission list
- Re-request permission via app UI

### Recording Fails
- Verify Shell privileges (UID 2000)
- Check storage permissions
- Ensure sufficient storage space
- Review console logs for error messages

## Development Notes

### Testing Without Android Device
This project requires:
- Android SDK for building
- Physical Android device or emulator for testing
- Shizuku service running with proper privileges

The code is ready for compilation but requires Android build tools that are not available in this environment.

### Next Steps for Development
1. Open project in Android Studio
2. Sync Gradle dependencies
3. Connect Android device with Shizuku
4. Build and install APK
5. Test Ghost Mode recording functionality
6. Review captured video output

## License
See LICENSE file for details.
