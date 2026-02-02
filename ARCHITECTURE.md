# PrivateEye Architecture Diagram

## System Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                         USER INTERFACE                          │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │             MainActivity (FLAG_SECURE)                     │  │
│  │  - Excludes own UI from recordings                        │  │
│  │  - Hosts RecordingScreen Composable                       │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │             RecordingScreen (Jetpack Compose)             │  │
│  │  - Permission management UI                               │  │
│  │  - Recording controls                                     │  │
│  │  - Console log display                                    │  │
│  │  - Status LED indicator                                   │  │
│  └───────────────────────────────────────────────────────────┘  │
│                              │                                   │
│                              │ StateFlow                         │
│                              ▼                                   │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │           RecordingViewModel (Hilt)                       │  │
│  │  - State management (isRecording, logs, permissions)      │  │
│  │  - MediaProjection flow coordination                      │  │
│  │  - Service lifecycle management                           │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ Service Start/Stop
                              │
        ┌─────────────────────┴─────────────────────┐
        │                                           │
        ▼                                           ▼
┌────────────────────┐                    ┌──────────────────────┐
│  OverlayService    │                    │ScreenRecordingService│
│                    │                    │                      │
│ ┌────────────────┐ │                    │ ┌─────────────────┐  │
│ │ WindowManager  │ │                    │ │MediaProjection  │  │
│ │ - Floating FAB │ │◄─────Broadcast────►│ │Manager          │  │
│ │ - Draggable    │ │   (Start/Stop)     │ └─────────────────┘  │
│ │ - TYPE_APP_    │ │                    │          │           │
│ │   OVERLAY      │ │                    │          ▼           │
│ └────────────────┘ │                    │ ┌─────────────────┐  │
│                    │                    │ │MediaProjection  │  │
│ Broadcasts:        │                    │ │Instance         │  │
│ - START_RECORDING  │                    │ └─────────────────┘  │
│ - STOP_RECORDING   │                    │          │           │
└────────────────────┘                    │          ▼           │
                                          │ ┌─────────────────┐  │
                                          │ │ VirtualDisplay  │  │
                                          │ │ - Captures      │  │
                                          │ │   screen        │  │
                                          │ └─────────────────┘  │
                                          │          │           │
                                          │          ▼           │
                                          │ ┌─────────────────┐  │
                                          │ │   MediaCodec    │  │
                                          │ │ - H.264/AVC     │  │
                                          │ │ - COLOR_Format  │  │
                                          │ │   Surface       │  │
                                          │ │ - 6 Mbps        │  │
                                          │ │ - 30 fps        │  │
                                          │ └─────────────────┘  │
                                          │          │           │
                                          │          ▼           │
                                          │ ┌─────────────────┐  │
                                          │ │  MediaMuxer     │  │
                                          │ │ - MP4 output    │  │
                                          │ │ - File writing  │  │
                                          │ └─────────────────┘  │
                                          │          │           │
                                          │          ▼           │
                                          │ ┌─────────────────┐  │
                                          │ │ Notification    │  │
                                          │ │ - Foreground    │  │
                                          │ │ - Stop action   │  │
                                          │ └─────────────────┘  │
                                          └──────────────────────┘
                                                      │
                                                      ▼
                                          ┌──────────────────────┐
                                          │   Output File        │
                                          │ recording_<time>.mp4 │
                                          │ Downloads/PrivateEye/│
                                          └──────────────────────┘
```

## Data Flow Diagram

```
User Action Flow:
─────────────────

1. Initial Setup:
   User → Tap "Grant Overlay" → System Settings → Permission Granted
   User → Tap "Setup Screen Capture" → MediaProjection Dialog → Permission Granted

2. Start Recording:
   User → Tap "Start Overlay" → OverlayService Started → Floating Button Shown
   User → Tap "Start Recording" → RecordingViewModel.startRecording()
   ViewModel → Start ScreenRecordingService with MediaProjection data
   Service → Initialize MediaCodec + MediaProjection + VirtualDisplay
   VirtualDisplay → Render screen to MediaCodec Surface
   MediaCodec → Encode frames to H.264
   MediaMuxer → Write encoded data to MP4 file
   Service → Show foreground notification

3. During Recording:
   Screen Content → VirtualDisplay → MediaCodec Surface → Encoded → MP4 File
   (PrivateEye UI excluded due to FLAG_SECURE)

4. Stop Recording:
   User → Tap "Stop Recording" → RecordingViewModel.stopRecording()
   ViewModel → Send stop intent to ScreenRecordingService
   Service → Signal end of stream → Stop codec → Release resources
   Service → Hide notification → Stop foreground service
   File saved to Downloads/PrivateEye/
```

## Permission Flow

```
┌─────────────────────────────────────────────────────────────┐
│                   Permission Lifecycle                      │
└─────────────────────────────────────────────────────────────┘

1. App Launch:
   ├─ Check SYSTEM_ALERT_WINDOW permission
   │  ├─ Granted → Enable overlay buttons
   │  └─ Not Granted → Show "Grant Overlay Permission" button
   │
   └─ MediaProjection permission not yet requested

2. User Taps "Grant Overlay Permission":
   ├─ Open Settings.ACTION_MANAGE_OVERLAY_PERMISSION
   ├─ User grants in system settings
   ├─ User returns to app
   └─ App detects permission granted → Enable overlay features

3. User Taps "Setup Screen Capture":
   ├─ Create MediaProjection screen capture intent
   ├─ Launch ActivityResultLauncher
   ├─ Android shows MediaProjection warning dialog
   ├─ User accepts
   ├─ Result delivered to ViewModel
   └─ Store resultCode and resultData for later use

4. Recording Phase:
   ├─ Use stored MediaProjection permission
   ├─ Create MediaProjection instance
   ├─ Start VirtualDisplay with projection
   └─ Capture screen content

Permission States:
─────────────────
hasOverlayPermission: Boolean (checked via Settings.canDrawOverlays)
mediaProjectionResultCode: Int (stored from ActivityResult)
mediaProjectionResultData: Intent? (stored from ActivityResult)
```

## Component Communication

```
┌────────────────────────────────────────────────────────────┐
│              Inter-Component Communication                 │
└────────────────────────────────────────────────────────────┘

RecordingViewModel ←→ RecordingScreen
  Protocol: Kotlin StateFlow
  Data: isRecording, consoleLogs, hasOverlayPermission

RecordingViewModel → OverlayService
  Protocol: Service Intent (startService)
  Actions: START, STOP

OverlayService → RecordingViewModel
  Protocol: BroadcastReceiver
  Actions: ACTION_START_RECORDING, ACTION_STOP_RECORDING

RecordingViewModel → ScreenRecordingService
  Protocol: Service Intent (startForegroundService)
  Data: resultCode, resultData, outputPath
  Actions: ACTION_START, ACTION_STOP

MainActivity → RecordingViewModel
  Protocol: Hilt Injection + Compose hiltViewModel()
  Lifecycle: Activity lifecycle manages ViewModel
```

## Thread Model

```
┌────────────────────────────────────────────────────────────┐
│                     Thread Architecture                     │
└────────────────────────────────────────────────────────────┘

Main Thread (UI):
├─ MainActivity
├─ RecordingScreen (Compose)
└─ RecordingViewModel (coroutines on Main dispatcher)

Service Thread:
├─ OverlayService (Main thread)
│  └─ WindowManager operations
│
└─ ScreenRecordingService (Main thread)
   ├─ Service lifecycle callbacks
   └─ Spawns encoding thread ─────────────┐
                                          │
Background Thread:                        │
└─ encodeLoop() ◄─────────────────────────┘
   ├─ MediaCodec.dequeueOutputBuffer (blocking)
   ├─ Process encoded frames
   └─ MediaMuxer.writeSampleData

MediaCodec Internal Threads:
├─ Input surface rendering (system)
├─ Encoding (hardware accelerator)
└─ Output buffer management
```

## File System Layout

```
Android Device Storage:
/storage/emulated/0/
└── Download/
    └── PrivateEye/
        ├── recording_1738456789123.mp4
        ├── recording_1738457001234.mp4
        └── recording_1738457123456.mp4

File Naming Convention:
recording_<unix_timestamp_milliseconds>.mp4

Example:
recording_1738456789123.mp4
         └─ Unix timestamp: 1738456789123 ms
                           = 2026-02-01 12:33:09 UTC
```

## Module Dependencies

```
┌────────────────────────────────────────────────────────────┐
│                   Module Dependency Graph                   │
└────────────────────────────────────────────────────────────┘

:app
├── Depends on: :core-stealth (legacy, not actively used)
├── Depends on: :common (logging utilities)
└── Contains:
    ├── MainActivity.kt
    ├── RecordingScreen.kt
    ├── RecordingViewModel.kt
    ├── service/
    │   ├── OverlayService.kt
    │   └── ScreenRecordingService.kt
    └── MainScreen.kt (legacy)

:core-stealth (DEPRECATED)
├── Depends on: :common
└── Contains:
    ├── PrivateEyeUserService.kt (Shizuku-based, not used)
    ├── PrivateEyeConnector.kt (not used)
    └── ShizukuManager.kt (not used)

:common
├── No dependencies
└── Contains:
    ├── Logger.kt
    └── LogEntry.kt

External Dependencies:
├── Jetpack Compose (UI)
├── Hilt (Dependency Injection)
├── Kotlin Coroutines (Async)
└── Android SDK (MediaProjection, MediaCodec, MediaMuxer)
```

## State Machine

```
┌────────────────────────────────────────────────────────────┐
│              Recording State Machine                        │
└────────────────────────────────────────────────────────────┘

States:
┌────────────┐
│   IDLE     │  Initial state, no recording
└────────────┘
      │
      │ User taps "Start Recording"
      │
      ▼
┌────────────┐
│ STARTING   │  Initializing MediaProjection
└────────────┘
      │
      │ MediaCodec ready
      │
      ▼
┌────────────┐
│ RECORDING  │  Active recording, encoding frames
└────────────┘
      │
      │ User taps "Stop Recording"
      │
      ▼
┌────────────┐
│ STOPPING   │  Signaling end of stream, finalizing
└────────────┘
      │
      │ Resources released
      │
      ▼
┌────────────┐
│   IDLE     │  Ready for next recording
└────────────┘

Transitions:
IDLE → STARTING: startRecording() called
STARTING → RECORDING: MediaCodec started successfully
RECORDING → STOPPING: stopRecording() called
STOPPING → IDLE: Resources released, file saved
RECORDING → IDLE: Error occurred (direct transition)
```

## Notification States

```
┌────────────────────────────────────────────────────────────┐
│                  Notification Behavior                      │
└────────────────────────────────────────────────────────────┘

No Recording:
  └─ No notification shown

Recording Started:
  ├─ ScreenRecordingService.startForeground() called
  ├─ Notification appears in status bar
  ├─ Title: "PrivateEye Recording"
  ├─ Text: "Screen recording in progress"
  ├─ Icon: Camera icon
  ├─ Priority: Low (doesn't interrupt user)
  ├─ Ongoing: Yes (can't be dismissed by swipe)
  └─ Action: "Stop" button
      └─ Sends ACTION_STOP intent to service

Recording Stopped:
  ├─ ScreenRecordingService.stopForeground() called
  └─ Notification removed from status bar
```

This architecture provides a clean separation of concerns with:
- UI layer (Activity, Screen, ViewModel)
- Service layer (Recording, Overlay)
- Data layer (File system output)
- Permission management
- State management via StateFlow
- Background processing via services and threads
