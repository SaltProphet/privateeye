# PrivateEye

Professional-grade Android screen recording utility with MediaProjection API and floating overlay controls.

## Features

- **MediaProjection Recording**: High-fidelity screen capture using Android's MediaProjection API
- **Floating Controller**: WindowManager-based overlay button that remains visible during recording
- **Self-Hiding UI**: App's configuration screens are excluded from recordings using FLAG_SECURE
- **Foreground Service**: Transparent notification with Start/Stop recording controls
- **High Performance**: MediaCodec with COLOR_FormatSurface for optimized encoding
- **Modular Architecture**: Clean separation of concerns across three modules

## Architecture

### Modules
- **:app** - User interface layer (Jetpack Compose) with MediaProjection integration
- **:core-stealth** - Legacy Shizuku service and capture engine (deprecated)
- **:common** - Shared utilities and logging framework

### Technology Stack
- Kotlin 1.9.20
- Jetpack Compose
- Hilt (Dependency Injection)
- MediaProjection API for screen capture
- MediaCodec (H.264/AVC encoding with COLOR_FormatSurface)
- MediaMuxer (MP4 container)

## Quick Start

See [BUILD_INSTRUCTIONS.md](BUILD_INSTRUCTIONS.md) for detailed build and setup instructions.

### Requirements
- Android Studio Arctic Fox+
- Android SDK 26+
- Android device or emulator

### Build
```bash
./gradlew assembleDebug
```

### Run
1. Install PrivateEye APK
2. Grant "Display Over Other Apps" permission when prompted
3. Tap "Setup Screen Capture" to grant MediaProjection permission
4. Tap "Start Overlay" to show the floating controller
5. Use the floating button or in-app controls to start/stop recording

## UI Features

The app features a Matrix-themed terminal interface with:
- Pure black background with Matrix Green theme
- Real-time recording status LED indicator
- Permission management buttons
- Overlay service controls
- Recording start/stop buttons
- Real-time scrolling log console with timestamps [HH:mm:ss]

## Key Features

### 1. Floating Overlay Controller
- `OverlayService.kt` implements a draggable floating button
- Uses `TYPE_APPLICATION_OVERLAY` for overlay window
- Applies `FLAG_NOT_FOCUSABLE` and `FLAG_LAYOUT_IN_SCREEN` flags
- Remains interactive without interfering with other apps
- Toggle button changes color based on recording state

### 2. MediaProjection Pipeline
- `ScreenRecordingService.kt` manages screen capture
- MediaProjectionManager for high-quality screen recording
- MediaCodec configured with `COLOR_FormatSurface` for performance
- H.264/AVC video encoding at 6 Mbps bitrate, 30 fps
- MP4 container format via MediaMuxer

### 3. Clean UI (Self-Exclusion)
- MainActivity applies `FLAG_SECURE` on Android 13+
- Prevents the app's own UI from being captured
- Ensures clean recordings without configuration screens

### 4. User Notification
- Foreground service notification (required for MediaProjection)
- Clear "Stop" action in notification
- Low-priority notification to minimize intrusion

## Security & Permissions

PrivateEye requires:
- `SYSTEM_ALERT_WINDOW` - Display overlay floating button
- `FOREGROUND_SERVICE` - Background recording service
- `FOREGROUND_SERVICE_MEDIA_PROJECTION` - MediaProjection in foreground
- `PROJECT_MEDIA` - Media projection permission
- `RECORD_AUDIO` - Audio capture (optional)
- `WRITE_EXTERNAL_STORAGE` / `READ_EXTERNAL_STORAGE` - Save recordings
- `POST_NOTIFICATIONS` - Show recording notification (Android 13+)

All operations are logged in the console for transparency.

## Output

Recordings are saved to:
```
/storage/emulated/0/Download/PrivateEye/recording_<timestamp>.mp4
```

Format: MP4 container with H.264/AVC video encoding
- Resolution: Matches device screen resolution
- Bitrate: 6 Mbps
- Frame Rate: 30 fps
- I-Frame Interval: 1 second

## License

See LICENSE file.

