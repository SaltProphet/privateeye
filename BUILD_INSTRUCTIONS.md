# PrivateEye - Build Instructions

## Overview
PrivateEye is a professional-grade Android screen recording utility built with Kotlin and Jetpack Compose. It uses Android's MediaProjection API for high-quality screen capture with a floating overlay controller.

## Prerequisites
- Android Studio (Arctic Fox or later)
- Android SDK 26+
- JDK 8 or higher
- Android device or emulator for testing

## Project Structure
```
privateeye/
├── app/                      # Main application module (UI layer)
│   ├── src/main/
│   │   ├── java/dev/privateeye/
│   │   │   ├── MainActivity.kt
│   │   │   ├── RecordingScreen.kt
│   │   │   ├── RecordingViewModel.kt
│   │   │   ├── MainScreen.kt (Legacy)
│   │   │   ├── MainViewModel.kt (Legacy)
│   │   │   ├── PrivateEyeApplication.kt
│   │   │   ├── service/
│   │   │   │   ├── OverlayService.kt
│   │   │   │   └── ScreenRecordingService.kt
│   │   │   └── ui/
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── core-stealth/             # Legacy Shizuku integration (deprecated)
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
- **:app** - UI module with Jetpack Compose and MediaProjection
- **:core-stealth** - Legacy Shizuku integration (deprecated)
- **:common** - Shared logging and utilities

### 2. MediaProjection Recording
- ScreenRecordingService implements MediaProjection capture
- Foreground service with notification
- MediaCodec H.264/AVC encoding with COLOR_FormatSurface
- MediaMuxer for MP4 container format
- Output to `/storage/emulated/0/Download/PrivateEye/`

### 3. Floating Overlay Controller
- OverlayService provides draggable floating button
- TYPE_APPLICATION_OVERLAY window type
- FLAG_NOT_FOCUSABLE and FLAG_LAYOUT_IN_SCREEN flags
- Toggle button to start/stop recording
- Broadcast receiver integration

### 4. UI Features
- Pure black (#000000) background with Matrix Green (#00FF00) theme
- Terminal-style monospace console with auto-scrolling
- Permission management buttons
- Recording controls (Start/Stop)
- Real-time log display with timestamps [HH:mm:ss]
- Recording status LED indicator

### 5. Self-Hiding UI
- MainActivity applies FLAG_SECURE on Android 13+
- Prevents app's own configuration screens from being captured
- Ensures clean recordings

## Required Permissions (AndroidManifest.xml)
- `android.permission.SYSTEM_ALERT_WINDOW` - Display overlay
- `android.permission.FOREGROUND_SERVICE` - Background service
- `android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION` - MediaProjection service type
- `android.permission.PROJECT_MEDIA` - Media projection
- `android.permission.POST_NOTIFICATIONS` - Notifications (Android 13+)
- `android.permission.WRITE_EXTERNAL_STORAGE` - File storage
- `android.permission.READ_EXTERNAL_STORAGE` - File access
- `android.permission.RECORD_AUDIO` - Audio capture (optional)

## Dependencies
- Kotlin 1.9.20
- Jetpack Compose (BOM 2024.01.00)
- Hilt 2.48.1 (Dependency Injection)
- Android Gradle Plugin 8.2.0
- MediaProjection API (Android SDK)

## Running the App

### First Launch Setup
1. Launch PrivateEye app
2. Grant "Display Over Other Apps" permission when prompted
3. Tap "SETUP SCREEN CAPTURE" to request MediaProjection permission
4. Accept the screen capture permission dialog

### Using PrivateEye
1. Tap "START OVERLAY" to show the floating controller button
2. The floating button appears on screen (draggable)
3. Use either the floating button or in-app "START RECORDING" to begin
4. Recording status LED turns red when active
5. Tap "STOP RECORDING" or the floating button to stop
6. Recordings saved to Download/PrivateEye/
7. View real-time logs in the console

### Overlay Controls
- **Start Overlay**: Shows the floating controller button
- **Stop Overlay**: Hides the floating controller button
- Floating button can be dragged to any position
- Button color indicates recording state (red = ready, green = recording)

## Service Architecture

### ScreenRecordingService
- Foreground service with MediaProjection
- Creates VirtualDisplay for screen capture
- MediaCodec encoder with H.264/AVC
- MediaMuxer writes to MP4 file
- Handles service lifecycle and cleanup

### OverlayService
- Manages floating button overlay
- WindowManager for overlay positioning
- Touch listener for drag and click
- Broadcasts recording commands
- Independent of main activity

## Output Configuration

### Recording Output
- **Location**: `/storage/emulated/0/Download/PrivateEye/recording_<timestamp>.mp4`
- **Format**: MP4 container with H.264/AVC video encoding
- **Resolution**: Matches device screen resolution
- **Bitrate**: 6 Mbps
- **Frame Rate**: 30 fps
- **I-Frame Interval**: 1 second

## Troubleshooting

### Permission Issues
- Ensure "Display Over Other Apps" is granted in system settings
- Check MediaProjection permission was accepted
- Verify notification permission on Android 13+

### Recording Fails
- Check storage permissions
- Ensure sufficient storage space
- Review console logs for error messages
- Verify MediaProjection permission

### Overlay Not Showing
- Grant "Display Over Other Apps" permission
- Restart the app after granting permission
- Check if overlay service is running

## Development Notes

### Testing Without Android Device
This project requires:
- Android SDK for building
- Physical Android device or emulator for testing
- MediaProjection API requires Android 21+ (API level 21)

The code compiles but requires Android build tools and runtime environment.

### Architecture Changes
The app now uses MediaProjection API instead of Shizuku for screen recording:
- **Old**: Shizuku-based SurfaceControl capture (requires root/ADB)
- **New**: MediaProjection-based capture (standard Android API)
- Legacy Shizuku code remains in :core-stealth module but is not actively used

### Next Steps for Development
1. Open project in Android Studio
2. Sync Gradle dependencies
3. Connect Android device or start emulator
4. Build and install APK
5. Test overlay and recording functionality
6. Review captured video output

## License
See LICENSE file for details.
