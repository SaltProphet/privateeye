# PrivateEye Application Outline

## Application Overview
PrivateEye is a professional-grade Android screen recording utility with MediaProjection API integration. The application features a floating overlay controller and uses Jetpack Compose for the UI. Recordings are captured using Android's standard MediaProjection API with hardware-accelerated encoding.

---

## Architecture

### Module Structure
The application follows a modular architecture with three main modules:

1. **:app** - User interface layer (Jetpack Compose) with MediaProjection integration
2. **:core-stealth** - Legacy Shizuku service and capture engine (deprecated, not used)
3. **:common** - Shared utilities and logging framework

### Technology Stack
- **Language**: Kotlin 1.9.20
- **UI Framework**: Jetpack Compose (BOM 2024.01.00)
- **Dependency Injection**: Hilt 2.48.1
- **Screen Capture**: MediaProjection API (Android standard)
- **Video Encoding**: MediaCodec (H.264/AVC with COLOR_FormatSurface)
- **Container Format**: MediaMuxer (MP4)
- **Overlay System**: WindowManager with TYPE_APPLICATION_OVERLAY
- **Build System**: Gradle with Kotlin DSL
- **Min SDK**: Android 26 (Android 8.0)

---

## Module 1: :app (UI Layer)

### Implemented and Functional Components

#### 1. MainActivity.kt
- **Status**: ✅ Fully Implemented
- **Functions**:
  - `onCreate()` - Initializes the activity and sets up the Compose content
  - Edge-to-edge display support
  - FLAG_SECURE application (Android 13+) to prevent recording own UI
  - Hilt dependency injection integration via `@AndroidEntryPoint`

#### 2. PrivateEyeApplication.kt
- **Status**: ✅ Fully Implemented
- **Functions**:
  - `onCreate()` - Application initialization
  - Hilt Android app setup via `@HiltAndroidApp`

#### 3. RecordingScreen.kt
- **Status**: ✅ Fully Implemented
- **Functions**:
  - `RecordingScreen()` - Main composable for MediaProjection-based recording interface
  - `RecordingHeader()` - Displays app title and LED recording status indicator
  - `RecordingControls()` - Permission buttons, overlay controls, and recording buttons
  - `RecordingConsole()` - Scrollable terminal-style log display with auto-scroll
- **Features**:
  - Pure black background (#000000) with Matrix Green (#00FF00) theme
  - Real-time recording status LED (red = recording, green = ready)
  - Overlay permission and MediaProjection permission request buttons
  - Overlay service start/stop controls
  - Recording start/stop buttons
  - Auto-scrolling console with timestamped logs [HH:mm:ss]
  - Clear logs functionality

#### 4. RecordingViewModel.kt
- **Status**: ✅ Fully Implemented
- **Functions**:
  - `requestOverlayPermission()` - Opens system settings for overlay permission
  - `requestMediaProjection()` - Launches MediaProjection permission dialog
  - `handleMediaProjectionResult()` - Stores MediaProjection permission result
  - `startRecording()` - Starts ScreenRecordingService with MediaProjection data
  - `stopRecording()` - Stops active recording
  - `startOverlayService()` - Shows floating overlay button
  - `stopOverlayService()` - Hides floating overlay button
  - `checkOverlayPermission()` - Verifies overlay permission status
  - `addLog()` - Adds timestamped log entries [HH:mm:ss] format
  - `clearLogs()` - Clears console logs
- **State Management**:
  - `isRecording` - Recording on/off state
  - `hasOverlayPermission` - Overlay permission status
  - `consoleLogs` - List of timestamped console log messages
  - `mediaProjectionResultCode` - Stored MediaProjection permission result code
  - `mediaProjectionResultData` - Stored MediaProjection permission data
- **Broadcast Receivers**:
  - Listens for ACTION_START_RECORDING from OverlayService
  - Listens for ACTION_STOP_RECORDING from OverlayService

#### 5. Services

##### a. ScreenRecordingService.kt
- **Status**: ✅ Fully Implemented
- **Functions**:
  - `onStartCommand()` - Handles start/stop recording commands
  - `startRecording()` - Initializes MediaProjection recording pipeline
  - `stopRecording()` - Stops recording and cleans up resources
  - `initializeMediaCodec()` - Configures H.264/AVC encoder with COLOR_FormatSurface
  - `encodeLoop()` - Background thread for processing encoded frames
  - `createNotificationChannel()` - Sets up notification channel
  - `createNotification()` - Creates foreground service notification
- **Features**:
  - Foreground service with mediaProjection service type
  - MediaProjection instance creation and management
  - VirtualDisplay for screen capture
  - MediaCodec with COLOR_FormatSurface for hardware-accelerated encoding
  - MediaMuxer for MP4 output
  - Notification with Stop action button
  - Native device resolution capture
  - 6 Mbps bitrate, 30 fps frame rate

##### b. OverlayService.kt
- **Status**: ✅ Fully Implemented
- **Functions**:
  - `onStartCommand()` - Handles service start
  - `setupFloatingButton()` - Creates draggable floating action button
  - `toggleRecording()` - Broadcasts recording start/stop commands
  - `updateButtonState()` - Updates button appearance based on recording state
  - `onDestroy()` - Removes floating button
- **Features**:
  - WindowManager-based floating button
  - TYPE_APPLICATION_OVERLAY window type
  - FLAG_NOT_FOCUSABLE and FLAG_LAYOUT_IN_SCREEN flags
  - Draggable to any screen position
  - Button color changes based on recording state (red = ready, green = recording)
  - Broadcasts to coordinate with RecordingViewModel

#### 6. Legacy Components (Not Currently Used)

##### a. MainScreen.kt
- **Status**: ⚠️ Implemented but Unused (Legacy Shizuku UI)
- **Note**: This was the old Ghost Mode interface for Shizuku-based recording
- **Components**: Ghost Mode toggle, Shizuku connection status
- **Replaced by**: RecordingScreen.kt

##### b. MainViewModel.kt
- **Status**: ⚠️ Implemented but Unused (Legacy Shizuku ViewModel)
- **Note**: This was the ViewModel for Shizuku-based recording
- **Features**: Ghost Mode toggle, Shizuku service connection
- **Replaced by**: RecordingViewModel.kt

#### 7. UI Theme

##### a. Color.kt
- **Status**: ✅ Fully Implemented
- **Colors Defined**:
  - DarkBackground = #0A0E14
  - DarkSurface = #151A21
  - DarkPrimary = #00D9FF (cyan)
  - DarkSecondary = #7C4DFF (purple)
  - DarkTertiary = #FF6B6B (red)
  - TerminalBackground = #0D1117
  - TerminalText = #58A6FF (blue)
  - TerminalError = #FF6B6B (red)
  - TerminalWarning = #FFA657 (orange)
  - TerminalSuccess = #56D364 (green)

##### b. Theme.kt
- **Status**: ✅ Fully Implemented
- **Functions**:
  - `PrivateEyeTheme()` - Application theme composable
- **Features**:
  - Dark color scheme using Material 3
  - Always uses dark theme

##### c. Type.kt
- **Status**: ✅ Fully Implemented
- **Typography Defined**:
  - bodyLarge (14sp, Monospace)
  - bodyMedium (12sp, Monospace)
  - bodySmall (10sp, Monospace)
  - titleLarge (22sp, Bold)
  - titleMedium (16sp, Bold)

---

## Module 2: :core-stealth (Legacy Shizuku Engine - DEPRECATED)

**⚠️ IMPORTANT**: This module contains the legacy Shizuku-based implementation and is **NO LONGER USED** in the current application. The app now uses the standard MediaProjection API instead. This module is kept for reference but is not actively maintained or integrated.

### Legacy Components (Not Used)

#### 1. IPrivateEyeService.aidl
- **Status**: ⚠️ Legacy/Unused
- **Interface Methods**:
  - `int getPid()` - Returns service process ID
  - `void captureScreen(String outputPath)` - Captures single screenshot
  - `boolean hasShellPrivileges()` - Checks if running with Shell UID (2000)
  - `void startRecording(String outputPath)` - Starts video recording
  - `void stopRecording()` - Stops video recording
  - `boolean isRecording()` - Checks recording status
  - `void destroy()` - Terminates service
- **Note**: AIDL interface for Shizuku IPC, not used in current MediaProjection implementation

#### 2. PrivateEyeUserService.kt
- **Status**: ⚠️ Legacy/Unused - Shizuku-based service
- **Note**: This service was designed to run with Shizuku privileges and use SurfaceControl API via reflection. The current implementation uses MediaProjection instead, which is a standard Android API that doesn't require elevated privileges.
- **Legacy Features**:
  - Shell privilege verification
  - SurfaceControl-based capture via reflection
  - Single screenshot capture
  - Video recording (incomplete implementation)

#### 3. PrivateEyeConnector.kt
- **Status**: ⚠️ Legacy/Unused
- **Note**: Connector for Shizuku service binding. Not used in MediaProjection implementation.
- **Legacy Features**:
  - Shizuku listener registration
  - Service binding/unbinding
  - Connection callbacks

#### 4. ShizukuManager.kt
- **Status**: ⚠️ Legacy/Unused
- **Note**: Manager for Shizuku integration. Not used in MediaProjection implementation.
- **Legacy Features**:
  - Shizuku installation/running checks
  - Permission management
  - Service binding

---

## Module 3: :common (Shared Utilities)

### Implemented and Functional Components

#### 1. Logger.kt
- **Status**: ✅ Fully Implemented
- **Functions**:
  - `debug()` - Logs DEBUG level message
  - `info()` - Logs INFO level message
  - `warning()` - Logs WARNING level message
  - `error()` - Logs ERROR level message
  - `clear()` - Clears all logs
  - `addLog()` - Internal function to add log entry
- **State Management**:
  - `logs` - StateFlow of LogEntry list
- **Features**:
  - Centralized logging with StateFlow
  - Automatic Android Logcat integration
  - Singleton via Hilt

#### 2. LogEntry.kt
- **Status**: ✅ Fully Implemented
- **Data Class Properties**:
  - timestamp (Long)
  - level (LogLevel enum)
  - tag (String)
  - message (String)
- **Functions**:
  - `formatTimestamp()` - Formats timestamp as HH:mm:ss.SSS
  - `formatForConsole()` - Formats complete log entry for console display
- **Enum**:
  - LogLevel: DEBUG, INFO, WARNING, ERROR

---

## Permissions (AndroidManifest.xml)

### Required Permissions
- ✅ `SYSTEM_ALERT_WINDOW` - Display overlay floating button (runtime permission via Settings)
- ✅ `FOREGROUND_SERVICE` - Background recording service
- ✅ `FOREGROUND_SERVICE_MEDIA_PROJECTION` - MediaProjection service type (Android 14+)
- ✅ `POST_NOTIFICATIONS` - Show recording notification (Android 13+)
- ✅ `WRITE_EXTERNAL_STORAGE` - File storage (maxSdkVersion 32)
- ✅ `READ_EXTERNAL_STORAGE` - File access (maxSdkVersion 32)
- ✅ `RECORD_AUDIO` - Audio capture (optional, if user wants audio in recordings)

### Legacy Permissions (Not Used)
- ⚠️ `moe.shizuku.manager.permission.API_V23` - Shizuku API access (legacy, not used)
- ⚠️ `android.permission.PROJECT_MEDIA` - Media projection permission (legacy, incorrect permission name - MediaProjection doesn't use manifest permissions)

### Runtime Permissions Flow
1. **Overlay Permission** - User must grant via Settings.ACTION_MANAGE_OVERLAY_PERMISSION
2. **MediaProjection Permission** - User must grant via MediaProjectionManager screen capture intent
3. **Storage Permissions** - Automatically granted on Android 10+ with scoped storage
4. **Notification Permission** - Requested at runtime on Android 13+

---

## Key Features Summary

### ✅ Fully Functional Features

1. **MediaProjection Integration**
   - Standard Android screen capture API
   - Permission dialog integration
   - No root or special setup required
   - Works on all Android devices (API 26+)

2. **Floating Overlay Controller**
   - WindowManager-based floating action button
   - Draggable to any screen position
   - Visual feedback (color changes based on recording state)
   - Always-on-top during recording
   - Broadcasts start/stop commands to app

3. **User Interface**
   - Matrix-themed terminal UI with pure black background
   - Real-time recording status LED indicator (red = recording, green = ready)
   - Permission management buttons (Overlay, MediaProjection)
   - Overlay service controls (Start/Stop overlay)
   - Recording controls (Start/Stop recording)
   - Auto-scrolling console with timestamped logs [HH:mm:ss]
   - Clear logs functionality

4. **Screen Recording**
   - High-quality video recording using MediaProjection
   - Hardware-accelerated encoding (COLOR_FormatSurface)
   - H.264/AVC video codec
   - MP4 container format
   - Native device resolution capture
   - 6 Mbps bitrate, 30 fps frame rate
   - Foreground service with notification
   - Stop action in notification

5. **UI Self-Exclusion**
   - FLAG_SECURE applied to MainActivity (Android 13+)
   - App's own UI automatically excluded from recordings
   - Clean output without configuration screens
   - MediaProjection respects FLAG_SECURE

6. **Logging System**
   - Centralized Logger with StateFlow
   - Timestamped entries [HH:mm:ss]
   - Real-time console display
   - Transparent operation logging

7. **Architecture**
   - Modular design (app, core-stealth, common)
   - Hilt dependency injection
   - Clean separation of concerns
   - StateFlow for reactive UI
   - Service-based background processing

### ⚠️ Legacy Features (No Longer Used)

1. **Shizuku Integration** (Deprecated)
   - Old implementation used Shizuku for elevated privileges
   - Required ADB setup or root access
   - Replaced by standard MediaProjection API

2. **Ghost Mode** (Removed)
   - Previous "stealth" mode using SurfaceControl
   - Required reflection and shell privileges
   - No longer needed with MediaProjection approach

3. **SurfaceControl Capture** (Deprecated)
   - Low-level API access via reflection
   - Fragile and version-dependent
   - Replaced by stable MediaProjection API

### ❌ Not Implemented

1. **Audio Recording** - RECORD_AUDIO permission exists but audio capture not yet implemented
2. **Quality Settings** - Fixed bitrate and resolution (no user configuration)
3. **Pause/Resume** - Recording must be stopped and restarted
4. **Video Editing** - No built-in trimming or editing features

---

## Output Configuration

### Recording Output
- **Location**: `/storage/emulated/0/Download/PrivateEye/recording_<timestamp>.mp4`
- **Format**: MP4 container with H.264/AVC video encoding
- **Resolution**: Native device screen resolution (e.g., 1080x1920, 1440x3040)
- **Bitrate**: 6 Mbps
- **Frame Rate**: 30 fps
- **I-Frame Interval**: 1 second
- **Color Format**: COLOR_FormatSurface (hardware-accelerated)
- **Encoding**: Hardware MediaCodec encoder

### File Naming Convention
- **Pattern**: `recording_<unix_timestamp_milliseconds>.mp4`
- **Example**: `recording_1738456789123.mp4`
  - Unix timestamp: 1738456789123 ms
  - Represents: 2026-02-01 12:33:09 UTC

### Storage Requirements
- **6 Mbps bitrate** = ~45 MB per minute
- 10-minute recording ≈ 450 MB
- 1-hour recording ≈ 2.7 GB

---

## Current State and Limitations

### ✅ Working Functionality

1. **MediaProjection Recording**
   - Full screen recording using standard Android API
   - Hardware-accelerated encoding
   - High-quality MP4 output
   - Foreground service with notification
   - Stable and reliable

2. **Floating Overlay**
   - Draggable floating button
   - Always accessible during recording
   - Visual feedback for recording state
   - Does not interfere with other apps

3. **UI Self-Exclusion**
   - App's own UI excluded from recordings (Android 13+)
   - Clean output without configuration screens
   - Automatic via FLAG_SECURE

### ⚠️ Known Limitations

1. **Android Version Compatibility**
   - FLAG_SECURE self-exclusion requires Android 13+ (API 33+)
   - On older versions, app UI may appear in recordings
   - MediaProjection works on all versions API 26+

2. **DRM and Secure Content**
   - Cannot record DRM-protected content (Netflix, Disney+, etc.)
   - Cannot record apps with FLAG_SECURE enabled
   - This is by design for security and copyright protection

3. **Audio Recording**
   - RECORD_AUDIO permission exists but not implemented yet
   - Video-only recordings currently
   - TODO: Add audio capture (microphone or system audio)

4. **User Configuration**
   - Fixed resolution (native device resolution)
   - Fixed bitrate (6 Mbps)
   - Fixed frame rate (30 fps)
   - TODO: Add settings UI for quality options

5. **Battery and Performance**
   - Foreground service keeps screen on
   - Hardware encoding is efficient but still drains battery
   - No battery optimization or frame skipping
   - 30 fps may be too high for some devices

6. **Storage Management**
   - No automatic cleanup of old recordings
   - No storage space checking before recording
   - Large files can fill storage quickly (45 MB/minute)

### 🔴 Breaking Changes from Legacy Version

1. **Shizuku No Longer Required**
   - Old version required Shizuku app installation
   - Old version required ADB setup or root
   - New version uses standard MediaProjection (no special setup)

2. **Ghost Mode Removed**
   - "Ghost Mode" toggle no longer exists
   - Old stealth SurfaceControl approach deprecated
   - New approach uses standard MediaProjection with user consent

3. **Permission Flow Changed**
   - Old: Shizuku permission required
   - New: Overlay permission + MediaProjection permission
   - New approach is more user-friendly

4. **Screenshot Feature Removed**
   - Old version supported single screenshot capture
   - New version is video recording only
   - Can be added back if needed

### 🏗️ Architecture Changes

1. **Core Module Deprecated**
   - `:core-stealth` module still exists but is unused
   - All Shizuku-related code is legacy
   - Can be removed in future cleanup

2. **New Services Added**
   - `ScreenRecordingService` - MediaProjection recording
   - `OverlayService` - Floating controller
   - Both replace old Shizuku service approach

3. **ViewModels Replaced**
   - `MainViewModel` → `RecordingViewModel`
   - `MainScreen` → `RecordingScreen`
   - Old files still exist but are not used

---

## User Flow

### Initial Setup
1. **Launch PrivateEye**
   - App displays Matrix-themed terminal interface
   - Console shows initialization message

2. **Grant Overlay Permission**
   - Tap "GRANT OVERLAY PERMISSION" button
   - System opens Settings page for overlay permission
   - User enables "Display over other apps"
   - Return to PrivateEye

3. **Setup MediaProjection**
   - Tap "SETUP SCREEN CAPTURE" button
   - Android shows MediaProjection permission dialog
   - Dialog warns: "PrivateEye will start capturing everything that's displayed on your screen"
   - User taps "Start Now" to grant permission
   - Console logs permission granted

### Recording Session

#### Method 1: In-App Controls
1. Tap "START RECORDING" button in app
2. ScreenRecordingService starts with notification
3. Status LED turns red
4. Recording begins, all screen content captured (except PrivateEye UI)
5. Notification shows "PrivateEye Recording" with Stop button
6. Tap "STOP RECORDING" button in app or notification
7. Recording stops and file is saved
8. Console shows output path: `/storage/emulated/0/Download/PrivateEye/recording_<timestamp>.mp4`

#### Method 2: Floating Overlay
1. Tap "START OVERLAY" button
2. Floating red button appears on screen (draggable)
3. Tap floating button to start recording
4. Button turns green during recording
5. Can drag button to any position while recording
6. Tap floating button again to stop recording
7. Tap "STOP OVERLAY" to remove floating button

### Console Logging
All operations are logged with timestamps in [HH:mm:ss] format:
```
[12:34:56] PrivateEye MediaProjection Recorder initialized
[12:34:58] [System] Overlay permission granted
[12:35:02] [System] Requesting MediaProjection permission
[12:35:05] [System] MediaProjection permission granted
[12:35:05] [Info] Ready to start recording
[12:35:10] [System] Overlay service started
[12:35:15] [Recording] Started: /storage/emulated/0/Download/PrivateEye/recording_1738456515000.mp4
[12:36:20] [Recording] Stopped
```

### Managing Recordings
- Recordings saved to: `/storage/emulated/0/Download/PrivateEye/`
- Files named: `recording_<timestamp>.mp4`
- Can be viewed in Files app or Gallery
- Can be shared, uploaded, or deleted manually

---

## Build and Development

### Build System
- **Gradle Version**: 8.2.0
- **Build Files**:
  - Root: `build.gradle.kts`
  - Modules: `app/build.gradle.kts`, `core-stealth/build.gradle.kts`, `common/build.gradle.kts`
  - Settings: `settings.gradle.kts`
  - Dependencies: `gradle/libs.versions.toml` (implied but not examined)

### Build Commands
```bash
./gradlew assembleDebug    # Build debug APK
./gradlew installDebug     # Install on device
./gradlew test             # Run tests
```

### Documentation
- ✅ `README.md` - Project overview and quick start
- ✅ `BUILD_INSTRUCTIONS.md` - Comprehensive build guide
- ✅ `LICENSE` - License file
- ✅ `.gitignore` - Git ignore configuration

---

## Summary

**Total Components**: RecordingScreen, RecordingViewModel, ScreenRecordingService, OverlayService, MainActivity, plus legacy components

**Implementation Status**:
- ✅ **Fully Functional**: ~95% (MediaProjection recording, floating overlay, UI, logging, architecture)
- ⚠️ **Not Yet Implemented**: ~5% (Audio recording, quality settings, storage management)
- 🗑️ **Deprecated/Legacy**: Shizuku-based components in :core-stealth module

**Current State**: The application is **fully functional** as a professional screen recording utility using Android's standard MediaProjection API. Key features include:

✅ **Working Features**:
- High-quality screen recording with MediaProjection
- Floating overlay controller that stays on top
- Hardware-accelerated video encoding
- UI self-exclusion (Android 13+)
- Foreground service with notification
- Matrix-themed terminal interface
- Real-time logging console
- MP4 video output

❌ **Not Yet Implemented**:
- Audio recording (permission exists, feature not implemented)
- User-configurable quality settings
- Storage space management
- Pause/resume functionality

**Recommended Next Steps**:
1. Implement audio recording (microphone and/or system audio)
2. Add quality settings UI (resolution, bitrate, frame rate)
3. Add storage space checking and cleanup
4. Remove deprecated :core-stealth module
5. Add pause/resume recording
6. Implement screenshot mode (single frame capture)

**Comparison to Legacy Implementation**:
- **Old**: Shizuku + SurfaceControl + reflection + root/ADB required + incomplete implementation
- **New**: MediaProjection + standard APIs + no root required + fully working + user-friendly

The new implementation is more stable, maintainable, and user-friendly than the previous Shizuku-based approach.
