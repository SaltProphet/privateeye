# PrivateEye Implementation Summary

## Overview
This document summarizes the implementation of a professional-grade screen recording utility for PrivateEye based on Android's MediaProjection API.

## Changes Made

### 1. MainActivity.kt - UI Self-Exclusion
**File**: `app/src/main/java/dev/privateeye/MainActivity.kt`

**Changes**:
- Added `FLAG_SECURE` to the window on Android 13+ (API 33+)
- Prevents the app's own configuration screens from appearing in recordings
- Updated to use the new `RecordingScreen` instead of legacy `MainScreen`

**Code**:
```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    window.setFlags(
        WindowManager.LayoutParams.FLAG_SECURE,
        WindowManager.LayoutParams.FLAG_SECURE
    )
}
```

### 2. OverlayService.kt - Floating Controller
**File**: `app/src/main/java/dev/privateeye/service/OverlayService.kt`

**Features**:
- Implements a draggable floating action button using WindowManager
- Uses `TYPE_APPLICATION_OVERLAY` for the overlay window type
- Applies `FLAG_NOT_FOCUSABLE` and `FLAG_LAYOUT_IN_SCREEN` flags
- Button remains interactive but doesn't interfere with other apps
- Touch listener supports both dragging and clicking
- Button color changes based on recording state (red = ready, green = recording)
- Broadcasts recording start/stop commands

**Key Methods**:
- `setupFloatingButton()` - Creates and configures the floating button
- `toggleRecording()` - Starts or stops recording
- `updateButtonState()` - Updates button appearance based on state

### 3. ScreenRecordingService.kt - MediaProjection Recording
**File**: `app/src/main/java/dev/privateeye/service/ScreenRecordingService.kt`

**Features**:
- Foreground service with MediaProjection for screen capture
- MediaCodec H.264/AVC encoder with `COLOR_FormatSurface`
- MediaMuxer for MP4 container output
- Captures at native device resolution
- 6 Mbps bitrate, 30 fps frame rate
- Creates notification with Stop action button

**Key Components**:
- **initializeMediaCodec()** - Configures MediaCodec with COLOR_FormatSurface
- **startRecording()** - Initializes MediaProjection and starts encoding
- **encodeLoop()** - Main encoding thread that processes frames
- **stopRecording()** - Cleanly shuts down recording and releases resources

**Performance Optimization**:
```kotlin
format.setInteger(
    MediaFormat.KEY_COLOR_FORMAT,
    MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
)
```
This uses hardware-accelerated surface rendering for maximum performance.

### 4. RecordingViewModel.kt - State Management
**File**: `app/src/main/java/dev/privateeye/RecordingViewModel.kt`

**Features**:
- Manages MediaProjection permission flow
- Handles overlay permission checks
- Coordinates between OverlayService and ScreenRecordingService
- Provides console logging for transparency
- Uses Kotlin StateFlow for reactive UI updates

**Key State**:
- `isRecording` - Current recording status
- `consoleLogs` - List of timestamped log messages
- `hasOverlayPermission` - Overlay permission status

**Key Methods**:
- `requestMediaProjection()` - Launches MediaProjection permission dialog
- `handleMediaProjectionResult()` - Stores permission result
- `startRecording()` - Starts the ScreenRecordingService
- `stopRecording()` - Stops the ScreenRecordingService
- `startOverlayService()` - Shows the floating button
- `stopOverlayService()` - Hides the floating button

### 5. RecordingScreen.kt - User Interface
**File**: `app/src/main/java/dev/privateeye/RecordingScreen.kt`

**Features**:
- Jetpack Compose UI with Matrix theme
- Permission management buttons
- Overlay service controls
- Recording start/stop buttons
- Real-time console log display
- Recording status LED indicator

**UI Components**:
- **RecordingHeader** - Title and status LED
- **RecordingControls** - Permission and recording buttons
- **RecordingConsole** - Scrollable log display with auto-scroll

### 6. AndroidManifest.xml - Permissions and Services
**File**: `app/src/main/AndroidManifest.xml`

**New Permissions**:
- `SYSTEM_ALERT_WINDOW` - Display overlay floating button
- `FOREGROUND_SERVICE_MEDIA_PROJECTION` - MediaProjection service type

**New Services**:
```xml
<service
    android:name=".service.ScreenRecordingService"
    android:enabled="true"
    android:exported="false"
    android:foregroundServiceType="mediaProjection" />

<service
    android:name=".service.OverlayService"
    android:enabled="true"
    android:exported="false" />
```

## User Flow

### Initial Setup
1. Launch PrivateEye
2. App requests "Display Over Other Apps" permission
3. User grants permission in system settings
4. Tap "SETUP SCREEN CAPTURE" button
5. Android shows MediaProjection permission dialog
6. User accepts to allow screen recording

### Recording Session
1. Tap "START OVERLAY" to show floating button
2. Floating button appears on screen (draggable to any position)
3. Tap "START RECORDING" (in-app or floating button)
4. Recording begins with foreground notification
5. Status LED turns red during recording
6. All screen content is captured except PrivateEye's own UI
7. Tap "STOP RECORDING" (in-app or floating button)
8. Recording saved to `/storage/emulated/0/Download/PrivateEye/recording_<timestamp>.mp4`

### Console Logging
All actions are logged with timestamps:
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

## Technical Details

### MediaProjection Pipeline
1. **MediaProjectionManager** requests screen capture permission
2. **MediaProjection** instance created from permission result
3. **MediaCodec** configured with H.264/AVC and COLOR_FormatSurface
4. **VirtualDisplay** created pointing to MediaCodec's input surface
5. Screen content automatically rendered to surface
6. **Encoding thread** dequeues encoded frames from MediaCodec
7. **MediaMuxer** writes frames to MP4 file

### Floating Overlay Implementation
1. **WindowManager** service manages overlay window
2. **TYPE_APPLICATION_OVERLAY** requires SYSTEM_ALERT_WINDOW permission
3. **FLAG_NOT_FOCUSABLE** allows touches to pass through to apps below
4. **FLAG_LAYOUT_IN_SCREEN** positions overlay within screen bounds
5. **OnTouchListener** implements drag with click detection
6. Small movements trigger click, large movements drag the button

### UI Self-Exclusion
1. **FLAG_SECURE** applied to MainActivity window
2. Android treats FLAG_SECURE windows as sensitive
3. MediaProjection automatically excludes FLAG_SECURE content
4. Result: PrivateEye's UI never appears in recordings
5. Ensures clean output without configuration screens

## Output Specifications

### Video File Format
- **Container**: MP4 (MPEG-4 Part 14)
- **Video Codec**: H.264/AVC (video/avc)
- **Resolution**: Native device screen resolution (e.g., 1080x1920)
- **Bitrate**: 6,000,000 bits/sec (6 Mbps)
- **Frame Rate**: 30 frames per second
- **I-Frame Interval**: 1 second
- **Color Format**: COLOR_FormatSurface (hardware-accelerated)

### File Location
```
/storage/emulated/0/Download/PrivateEye/recording_<timestamp>.mp4
```
Example: `recording_1738456789123.mp4`

## Performance Characteristics

### CPU Usage
- MediaCodec with COLOR_FormatSurface uses hardware encoder
- Minimal CPU overhead (mostly delegated to GPU/DSP)
- Encoding happens asynchronously in separate thread

### Battery Impact
- Hardware encoding is power-efficient
- Foreground service keeps screen on during recording
- Normal battery drain comparable to video playback

### Storage Requirements
- 6 Mbps bitrate = ~45 MB per minute of recording
- 10-minute recording ≈ 450 MB
- 1-hour recording ≈ 2.7 GB

## Comparison with Previous Implementation

### Old (Shizuku-based)
- Required Shizuku app installation
- Required ADB setup or root access
- Used SurfaceControl with reflection (fragile)
- "Ghost Mode" toggle
- Shell privileges (UID 2000)

### New (MediaProjection-based)
- Standard Android API (no root required)
- Simple permission dialog
- Stable public API
- Professional UI with overlay
- Works on all Android devices (API 26+)

## Testing Recommendations

### Manual Testing
1. Test on multiple Android versions (API 26, 29, 33+)
2. Verify overlay button appears and is draggable
3. Test recording various app content
4. Confirm PrivateEye UI is excluded from recordings
5. Check notification appears during recording
6. Verify MP4 files play correctly
7. Test with different screen resolutions
8. Test recording with audio (if RECORD_AUDIO granted)

### Edge Cases
- Handle MediaProjection permission denial
- Handle overlay permission denial
- Test recording with low storage space
- Test stopping recording during encoding
- Test app backgrounding during recording
- Test device rotation during recording

## Future Enhancements

### Potential Improvements
1. **Audio recording** - Capture system audio and/or microphone
2. **Quality settings** - Allow user to choose bitrate/resolution
3. **Pause/resume** - Support pausing recordings
4. **Video trimming** - Built-in editor for recorded videos
5. **Screenshot mode** - Capture single frames instead of video
6. **Scheduled recording** - Timer-based start/stop
7. **Gesture controls** - Volume buttons or shake to start/stop
8. **Cloud upload** - Automatic backup to cloud storage

### Known Limitations
1. Cannot record DRM-protected content (Netflix, etc.)
2. Cannot record other apps with FLAG_SECURE enabled
3. Requires user consent (MediaProjection dialog)
4. No internal audio on some devices without root

## Conclusion

This implementation provides a professional, standards-compliant screen recording utility that:
- Uses official Android APIs
- Respects user privacy and security
- Provides transparent operation via console logging
- Excludes its own UI from recordings
- Offers both in-app and floating overlay controls
- Produces high-quality MP4 video files
- Works on any Android device without special setup

The architecture is modular, maintainable, and follows Android best practices for services, permissions, and UI design.
