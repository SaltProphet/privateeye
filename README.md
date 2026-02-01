# PrivateEye

Modular Android stealth utility with Shizuku integration for system-level screen capture.

## Features

- **Stealth Capture Engine**: Screen recording using SurfaceControl (no MediaProjection)
- **Shizuku Integration**: System-level privileges for enhanced capabilities
- **Modular Architecture**: Clean separation of concerns across three modules
- **Terminal UI**: Matrix-themed dark interface with real-time log console
- **Ghost Mode**: One-touch activation of stealth recording

## Architecture

### Modules
- **:app** - User interface layer (Jetpack Compose)
- **:core-stealth** - Shizuku service and capture engine
- **:common** - Shared utilities and logging framework

### Technology Stack
- Kotlin 1.9.20
- Jetpack Compose
- Hilt (Dependency Injection)
- Shizuku API 13.1.5
- MediaCodec (H.264/AVC encoding)
- AIDL for IPC

## Quick Start

See [BUILD_INSTRUCTIONS.md](BUILD_INSTRUCTIONS.md) for detailed build and setup instructions.

### Requirements
- Android Studio Arctic Fox+
- Android SDK 26+
- Shizuku app installed on device

### Build
```bash
./gradlew assembleDebug
```

### Run
1. Install Shizuku and start service
2. Install PrivateEye APK
3. Grant Shizuku permission
4. Toggle "Ghost Mode" to start recording

## Screenshots

The UI features:
- Pure black background with Matrix Green theme
- Connection status LED indicator
- Ghost Mode toggle switch
- Real-time scrolling log console
- Timestamped log entries [HH:mm:ss]

## Security & Permissions

PrivateEye requires:
- Shizuku API permission
- Storage access (for saving recordings)
- Project Media permission
- Post Notifications (Android 13+)

All operations are logged in the console for transparency.

## Output

Recordings are saved to:
```
/storage/emulated/0/Download/PrivateEye/capture_<timestamp>.mp4
```

Format: MP4 container with H.264/AVC video encoding

## License

See LICENSE file.

