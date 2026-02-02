# PrivateEye Quick Start Guide

## What is PrivateEye?

PrivateEye is a professional Android screen recording utility that lets you capture high-quality video of your device screen with a floating control button. The app excludes its own interface from recordings for clean output.

## Quick Setup (First Time)

### Step 1: Install & Launch
1. Install the PrivateEye APK on your Android device
2. Launch the app
3. You'll see a Matrix-themed terminal interface

### Step 2: Grant Permissions
The app needs two permissions to work:

**A. Display Over Other Apps** (for floating button)
1. Tap the "GRANT OVERLAY PERMISSION" button
2. Android opens system settings
3. Find PrivateEye in the list
4. Toggle "Allow display over other apps" ON
5. Press back to return to PrivateEye
6. The button should now say "START OVERLAY"

**B. Screen Capture** (for recording)
1. Tap the "SETUP SCREEN CAPTURE" button
2. Android shows a warning dialog
3. Tap "Start now" to grant permission
4. You'll see "MediaProjection permission granted" in the console

### Step 3: You're Ready!
After granting both permissions, you can start recording.

## How to Record

### Method 1: Using the Floating Button

1. **Show the floating button:**
   - Tap "START OVERLAY" in the app
   - A red camera button appears on your screen
   - You can drag it anywhere you want

2. **Start recording:**
   - Tap the floating button once
   - Button turns green = recording is active
   - A notification appears saying "Screen recording in progress"

3. **Stop recording:**
   - Tap the green floating button again
   - Button turns red = recording stopped
   - Video is saved automatically

4. **Hide the floating button:**
   - Return to PrivateEye app
   - Tap "STOP OVERLAY"

### Method 2: Using In-App Controls

1. **Start recording:**
   - Tap the "START RECORDING" button in the app
   - Recording begins immediately
   - Exit the app to record other screens

2. **Stop recording:**
   - Return to PrivateEye app
   - Tap "STOP RECORDING"
   - Or tap "Stop" in the notification

## Where Are My Recordings?

Recordings are automatically saved to:
```
Phone Storage → Download → PrivateEye → recording_<timestamp>.mp4
```

You can find them using your file manager app under:
- `Downloads/PrivateEye/` folder

Or using a file browser at:
- `/storage/emulated/0/Download/PrivateEye/`

## Video Quality

Your recordings are:
- **Format:** MP4 (works everywhere)
- **Codec:** H.264/AVC (standard video codec)
- **Resolution:** Matches your phone's screen (e.g., 1080x1920)
- **Quality:** 6 Mbps bitrate, 30 frames per second
- **File Size:** About 45 MB per minute

## Tips & Tricks

### ✅ Best Practices
- **Use the floating button** for hands-free recording
- **Drag the button** to a corner where it won't be in the way
- **Check free space** before long recordings (1 hour ≈ 2.7 GB)
- **Watch the console logs** to see what's happening

### ⚠️ Important Notes
- **PrivateEye's own UI is excluded** from recordings automatically
- **Other apps with security flags** (like banking apps) may show as black screens
- **DRM content** (Netflix, etc.) cannot be recorded
- **Recording requires user consent** - you must tap "Start now" each time you set up

### 🔋 Battery & Performance
- **Hardware encoding** is used for efficiency
- **Battery drain** is similar to watching videos
- **Recording continues** even if you switch apps
- **Notification always visible** while recording (Android requirement)

## Console Logs

The app shows real-time logs at the bottom:
```
[12:34:56] PrivateEye MediaProjection Recorder initialized
[12:35:02] [System] Overlay permission granted
[12:35:05] [System] MediaProjection permission granted
[12:35:10] [Recording] Started: /storage/.../recording_1234567890.mp4
[12:36:20] [Recording] Stopped
```

Tap "CLEAR LOGS" to clean up the console.

## Troubleshooting

### Problem: "Grant Overlay Permission" button doesn't work
**Solution:** Open Settings → Apps → Special app access → Display over other apps → Find PrivateEye → Toggle ON

### Problem: Recording button greyed out
**Solution:** Make sure you've completed "Setup Screen Capture" first

### Problem: Floating button doesn't appear
**Solution:** 
1. Grant overlay permission (see above)
2. Tap "START OVERLAY" in the app
3. Check if you accidentally dragged it off-screen

### Problem: Recording shows black screen
**Solution:** Some apps use security flags to prevent recording. This is normal behavior for:
- Banking apps
- Payment apps
- Apps with sensitive content
- PrivateEye itself (intentional)

### Problem: App crashes when recording
**Solution:**
1. Check free storage space
2. Force close and restart PrivateEye
3. Re-grant MediaProjection permission
4. Try recording a shorter clip first

### Problem: Can't find my recordings
**Solution:** Open any file manager app and navigate to:
```
Downloads → PrivateEye
```
Or search for "*.mp4" files modified today.

## Support & More Info

For detailed technical information, see:
- **IMPLEMENTATION_SUMMARY.md** - Feature details and technical specs
- **ARCHITECTURE.md** - System architecture and diagrams
- **BUILD_INSTRUCTIONS.md** - How to build from source
- **README.md** - Project overview

## Privacy & Security

PrivateEye:
- ✅ Uses official Android APIs
- ✅ Requires explicit user permission
- ✅ Shows notification while recording
- ✅ Saves files locally (no cloud upload)
- ✅ Excludes its own UI from recordings
- ✅ All operations logged in console for transparency

---

**Need help?** Check the console logs for error messages, or refer to the documentation files included with the app.

**Enjoy your recordings!** 🎥
