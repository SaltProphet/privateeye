# PrivateEye Application Outline

## Application Overview
PrivateEye is a modular Android stealth utility application with Shizuku integration for system-level screen capture capabilities. The application leverages SurfaceControl API for stealth capture without MediaProjection and uses Jetpack Compose for the UI.

---

## Architecture

### Module Structure
The application follows a modular architecture with three main modules:

1. **:app** - User interface layer (Jetpack Compose)
2. **:core-stealth** - Shizuku service and capture engine
3. **:common** - Shared utilities and logging framework

### Technology Stack
- **Language**: Kotlin 1.9.20
- **UI Framework**: Jetpack Compose (BOM 2024.01.00)
- **Dependency Injection**: Hilt 2.48.1
- **IPC**: Shizuku API 13.1.5 with AIDL
- **Video Encoding**: MediaCodec (H.264/AVC)
- **Container Format**: MediaMuxer (MP4)
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
  - Hilt dependency injection integration via `@AndroidEntryPoint`

#### 2. PrivateEyeApplication.kt
- **Status**: ✅ Fully Implemented
- **Functions**:
  - `onCreate()` - Application initialization
  - Hilt Android app setup via `@HiltAndroidApp`

#### 3. MainScreen.kt
- **Status**: ✅ Fully Implemented
- **Functions**:
  - `MainScreen()` - Main composable for terminal-style interface
  - `Header()` - Displays app title and LED connection status indicator
  - `Controls()` - Ghost Mode toggle and Clear Logs button
  - `Console()` - Scrollable terminal-style log display with auto-scroll
- **Features**:
  - Pure black background (#000000) with Matrix Green (#00FF00) theme
  - Real-time connection status LED (green = connected, red = disconnected)
  - Ghost Mode toggle switch
  - Auto-scrolling console with timestamped logs

#### 4. MainViewModel.kt
- **Status**: ✅ Fully Implemented
- **Functions**:
  - `toggleGhostMode()` - Starts/stops stealth recording
  - `startRecording()` - Initiates screen recording with output path generation
  - `stopRecording()` - Stops active recording
  - `addLog()` - Adds timestamped log entries [HH:mm:ss] format
  - `clearLogs()` - Clears console logs
  - `checkShizukuStatus()` - Verifies Shizuku running status and permissions
  - `requestShizukuPermission()` - Requests Shizuku API permission
  - `reconnect()` - Re-establishes Shizuku connection
- **State Management**:
  - `isGhostModeActive` - Ghost Mode on/off state
  - `isShizukuConnected` - Shizuku service connection state
  - `consoleLogs` - List of timestamped console log messages
- **Callbacks**:
  - `ConnectionCallback` - Handles service connection events
  - `RecordingCallback` - Handles recording lifecycle events

#### 5. UI Components

##### a. DashboardScreen.kt
- **Status**: ⚠️ Implemented but Unused (Alternative UI)
- **Functions**:
  - `DashboardScreen()` - Alternative Material Design interface
  - `StatusRow()` - Status indicator row component
- **Features**:
  - Material 3 design with TopAppBar
  - Shizuku status card with multiple status indicators
  - Control buttons (Request Permission, Bind Service, Capture Screen, Clear Logs)
  - Terminal console integration
- **Note**: This appears to be an alternative/experimental UI that is not currently used in MainActivity

##### b. DashboardViewModel.kt
- **Status**: ⚠️ Implemented but Unused
- **Functions**:
  - `checkShizukuStatus()` - Checks and updates Shizuku status
  - `requestShizukuPermission()` - Requests Shizuku permission
  - `bindService()` - Binds to Shizuku service
  - `captureScreen()` - Captures single screenshot
  - `clearLogs()` - Clears logs
- **State Management**:
  - `logs` - Log entries from Logger
  - `shizukuStatus` - ShizukuStatus data class with isInstalled, isRunning, hasPermission, isServiceBound
- **Note**: Associated with unused DashboardScreen

##### c. TerminalConsole.kt
- **Status**: ⚠️ Implemented but Unused
- **Functions**:
  - `TerminalConsole()` - Terminal-style console component
  - `LogLine()` - Individual log line with color coding by level
- **Features**:
  - Auto-scroll to latest logs
  - Color-coded log levels (DEBUG, INFO, WARNING, ERROR)
  - Monospace font for terminal aesthetic
- **Note**: Built for DashboardScreen, replaced by inline Console in MainScreen

#### 6. UI Theme

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

## Module 2: :core-stealth (Stealth Capture Engine)

### Implemented and Functional Components

#### 1. IPrivateEyeService.aidl
- **Status**: ✅ Fully Implemented
- **Interface Methods**:
  - `int getPid()` - Returns service process ID
  - `void captureScreen(String outputPath)` - Captures single screenshot
  - `boolean hasShellPrivileges()` - Checks if running with Shell UID (2000)
  - `void startRecording(String outputPath)` - Starts video recording
  - `void stopRecording()` - Stops video recording
  - `boolean isRecording()` - Checks recording status
  - `void destroy()` - Terminates service

#### 2. PrivateEyeUserService.kt
- **Status**: ⚠️ Partially Implemented (Core Complete, Some Functions Need Work)
- **Fully Implemented Functions**:
  - `getPid()` - Returns process ID
  - `hasShellPrivileges()` - Verifies Shell UID (2000)
  - `captureScreen()` - Single screenshot with fallback mechanisms
  - `captureScreenViaSurfaceControl()` - SurfaceControl-based stealth capture via reflection
  - `getDisplayToken()` - Gets display token for SurfaceControl
  - `destroy()` - Service cleanup and termination
  - `log()` - Internal logging helper

- **Partially Implemented Functions**:
  - `startRecording()` - ⚠️ Starts recording but video encoding needs work
    - ✅ Creates output directory
    - ✅ Initializes MediaCodec encoder
    - ✅ Sets up MediaMuxer
    - ✅ Starts capture thread
    - ❌ Actual frame capture using SurfaceControl incomplete
  
  - `initializeEncoder()` - ✅ Fully sets up H.264/AVC encoder
    - MediaFormat configuration (1080x1920, 6Mbps, 30fps)
    - MediaCodec initialization
    - MediaMuxer setup for MP4 output
  
  - `captureLoop()` - ⚠️ Main recording loop structure present but incomplete
    - ✅ Loop structure and frame rate control
    - ✅ Frame counting and logging
    - ❌ `captureFrameData()` uses screencap command as placeholder
    - ❌ Proper SurfaceControl frame capture not implemented
  
  - `captureFrameData()` - ❌ **PLACEHOLDER IMPLEMENTATION**
    - Currently uses `screencap` command which is not true stealth
    - TODO: Implement proper SurfaceControl-based frame capture
  
  - `encodeFrame()` - ⚠️ Basic implementation present
    - ✅ MediaCodec input/output buffer handling
    - ✅ Track addition and muxer start
    - ⚠️ Needs proper frame data formatting
  
  - `stopRecording()` - ✅ Fully implemented
    - Stops recording flag
    - Waits for recording thread
    - Calls cleanup
  
  - `finalizeRecording()` - ✅ Fully implemented
    - Stops and releases MediaCodec
    - Stops and releases MediaMuxer

- **Constants**:
  - PRIMARY_DISPLAY_ID = 0
  - VIDEO_WIDTH = 1080
  - VIDEO_HEIGHT = 1920
  - VIDEO_BIT_RATE = 6000000 (6 Mbps)
  - VIDEO_FRAME_RATE = 30
  - VIDEO_I_FRAME_INTERVAL = 1
  - MIME_TYPE = "video/avc" (H.264)

#### 3. PrivateEyeConnector.kt
- **Status**: ✅ Fully Implemented
- **Implements**: Shizuku.OnBinderReceivedListener, Shizuku.OnBinderDeadListener
- **Functions**:
  - `initialize()` - Registers Shizuku listeners
  - `cleanup()` - Unregisters Shizuku listeners
  - `connect()` - Binds to PrivateEyeUserService via Shizuku
  - `disconnect()` - Unbinds from service
  - `getService()` - Returns current service instance
  - `isConnected()` - Checks connection status
  - `startRecording()` - Starts recording with callback
  - `stopRecording()` - Stops recording with callback
  - `isRecording()` - Checks recording status
  - `onBinderReceived()` - Shizuku binder received callback
  - `onBinderDead()` - Shizuku binder dead callback
- **Service Connection**:
  - `onServiceConnected()` - Handles successful service binding
  - `onServiceDisconnected()` - Handles service disconnection
- **Callbacks**:
  - ConnectionCallback (onConnected, onDisconnected, onError, onLog)
  - RecordingCallback (onRecordingStarted, onRecordingStopped, onError, onLog)

#### 4. ShizukuManager.kt
- **Status**: ⚠️ Implemented but Not Used in Main Flow
- **Functions**:
  - `isShizukuInstalled()` - Checks if Shizuku app is installed
  - `isShizukuRunning()` - Checks if Shizuku service is running
  - `hasShizukuPermission()` - Checks Shizuku permission status
  - `requestShizukuPermission()` - Requests Shizuku permission
  - `bindService()` - Binds to PrivateEyeUserService
  - `unbindService()` - Unbinds from service
  - `captureScreen()` - Captures single screenshot
  - `getService()` - Returns service instance
- **Note**: This manager exists but MainViewModel uses PrivateEyeConnector directly instead

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
- ✅ `moe.shizuku.manager.permission.API_V23` - Shizuku API access
- ✅ `android.permission.PROJECT_MEDIA` - Media projection
- ✅ `android.permission.POST_NOTIFICATIONS` - Notifications (Android 13+)
- ✅ `android.permission.WRITE_EXTERNAL_STORAGE` - File storage
- ✅ `android.permission.READ_EXTERNAL_STORAGE` - File access
- ✅ `android.permission.RECORD_AUDIO` - Audio capture
- ✅ `android.permission.FOREGROUND_SERVICE` - Service management

---

## Key Features Summary

### ✅ Fully Functional Features

1. **Shizuku Integration**
   - Permission checking and requesting
   - Service binding with elevated privileges
   - Automatic reconnection handling
   - Binder lifecycle management

2. **User Interface**
   - Matrix-themed terminal UI with pure black background
   - Real-time connection status LED indicator
   - Ghost Mode toggle switch
   - Auto-scrolling console with timestamped logs [HH:mm:ss]
   - Clear logs functionality

3. **Single Screenshot Capture**
   - SurfaceControl-based stealth capture (via reflection)
   - Fallback to screencap command
   - PNG output format
   - Shell privilege verification

4. **Security**
   - Shizuku permission verification
   - Shell UID (2000) privilege checking
   - Process ID tracking
   - Secure service binding

5. **Logging System**
   - Centralized Logger with StateFlow
   - Color-coded log levels
   - Timestamped entries
   - Android Logcat integration

6. **Architecture**
   - Modular design (app, core-stealth, common)
   - Hilt dependency injection
   - AIDL for IPC
   - Clean separation of concerns

### ⚠️ Partially Implemented Features

1. **Video Recording (Ghost Mode)**
   - ✅ Recording UI and state management
   - ✅ MediaCodec H.264/AVC encoder setup
   - ✅ MediaMuxer MP4 container
   - ✅ Recording lifecycle (start/stop)
   - ✅ Frame rate control (30 fps)
   - ❌ **CRITICAL**: Proper SurfaceControl frame capture incomplete
   - ❌ **CRITICAL**: `captureFrameData()` uses placeholder screencap command
   - ❌ Frame data conversion and encoding needs work
   - ❌ True stealth capture without MediaProjection not fully working

### ❌ Non-Functional / Placeholder Components

1. **DashboardScreen.kt** - Alternative UI not used
2. **DashboardViewModel.kt** - ViewModel for unused Dashboard
3. **TerminalConsole.kt** - Component built for Dashboard, replaced in MainScreen
4. **ShizukuManager.kt** - Manager implemented but not used (PrivateEyeConnector used instead)
5. **captureFrameData()** in PrivateEyeUserService - **PLACEHOLDER**: Uses screencap command instead of proper SurfaceControl API

---

## Output Configuration

### Recording Output
- **Location**: `/storage/emulated/0/Download/PrivateEye/capture_<timestamp>.mp4`
- **Format**: MP4 container with H.264/AVC video encoding
- **Resolution**: 1080x1920
- **Bitrate**: 6 Mbps
- **Frame Rate**: 30 fps
- **I-Frame Interval**: 1 second

### Screenshot Output
- **Location**: Specified by caller (typically in PrivateEye folder)
- **Format**: PNG

---

## Critical Issues and Limitations

### 🔴 Critical Issues

1. **Video Recording Implementation Incomplete**
   - The `captureFrameData()` function in PrivateEyeUserService uses screencap command as a placeholder
   - True stealth capture using SurfaceControl for video recording is not implemented
   - The current implementation won't produce functional video recordings
   - This defeats the main "stealth" purpose of the application

2. **Frame Data Encoding**
   - Even if frames were captured, the current encoding pipeline may not properly handle the data format
   - MediaCodec COLOR_FormatSurface is configured but not used with an actual Surface

### ⚠️ Known Limitations

1. **Unused Components**
   - Alternative UI components (DashboardScreen, DashboardViewModel, TerminalConsole) exist but are not integrated
   - ShizukuManager exists but is not used; PrivateEyeConnector is used instead
   - This creates code redundancy and maintenance overhead

2. **Android Version Compatibility**
   - SurfaceControl API usage varies by Android version
   - Reflection is used to access hidden APIs which may break on future Android versions
   - No audio recording implementation (despite RECORD_AUDIO permission)

3. **Error Handling**
   - Limited error recovery in recording pipeline
   - No retry mechanism for failed captures

4. **Performance**
   - No frame skipping or adaptive quality
   - Fixed 30 fps may be too high for some devices
   - No battery optimization considerations

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

**Total Files Analyzed**: 16 Kotlin source files + 3 AndroidManifest + 4 build.gradle.kts + AIDL interface

**Implementation Status**:
- ✅ **Fully Functional**: ~75% (UI, Shizuku integration, single screenshot, logging, architecture)
- ⚠️ **Partially Implemented**: ~20% (Video recording structure exists but core capture incomplete)
- ❌ **Non-Functional/Unused**: ~5% (Alternative UI components, unused manager)

**Critical Gap**: The core stealth video recording feature is not fully functional. The application can successfully:
- Connect to Shizuku with elevated privileges
- Display a polished terminal-style UI
- Capture single screenshots using SurfaceControl
- Manage recording state and UI

However, it **cannot** currently:
- Record video using true SurfaceControl capture (uses placeholder screencap)
- Produce functional MP4 video files from screen recording

**Recommended Priority**: Implement proper SurfaceControl-based frame capture in `captureFrameData()` to complete the core stealth recording functionality.
