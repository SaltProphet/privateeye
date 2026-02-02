package dev.privateeye.service

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import androidx.core.content.ContextCompat

/**
 * OverlayService - Floating action button overlay for screen recording control
 * 
 * Implements a WindowManager-based floating button that:
 * - Uses TYPE_APPLICATION_OVERLAY for the layout type
 * - Applies FLAG_NOT_FOCUSABLE and FLAG_LAYOUT_IN_SCREEN
 * - Remains interactive but doesn't interfere with background apps
 */
class OverlayService : Service() {
    
    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var isRecording = false
    
    companion object {
        const val ACTION_START_RECORDING = "dev.privateeye.action.START_RECORDING"
        const val ACTION_STOP_RECORDING = "dev.privateeye.action.STOP_RECORDING"
        const val ACTION_SHOW_OVERLAY = "dev.privateeye.action.SHOW_OVERLAY"
        const val ACTION_HIDE_OVERLAY = "dev.privateeye.action.HIDE_OVERLAY"
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onCreate() {
        super.onCreate()
        setupFloatingButton()
    }
    
    private fun setupFloatingButton() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        // Create a simple floating button programmatically
        val floatingButton = ImageButton(this).apply {
            // Set button icon based on recording state
            setImageResource(android.R.drawable.ic_menu_camera)
            setBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
            setPadding(20, 20, 20, 20)
            
            // Handle button clicks
            setOnClickListener {
                toggleRecording()
            }
        }
        
        floatingView = floatingButton
        
        // Configure window layout parameters
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        
        // Set initial position
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 100
        params.y = 100
        
        // Add touch listener for dragging
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        
        floatingView?.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager?.updateViewLayout(floatingView, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    // If movement is small, treat as click
                    if (dx * dx + dy * dy < 100) {
                        view.performClick()
                    }
                    true
                }
                else -> false
            }
        }
        
        // Add the floating view to window
        windowManager?.addView(floatingView, params)
    }
    
    private fun toggleRecording() {
        if (isRecording) {
            stopRecording()
        } else {
            startRecording()
        }
    }
    
    private fun startRecording() {
        isRecording = true
        updateButtonState()
        
        // Send broadcast to start recording
        val intent = Intent(ACTION_START_RECORDING)
        sendBroadcast(intent)
    }
    
    private fun stopRecording() {
        isRecording = false
        updateButtonState()
        
        // Send broadcast to stop recording
        val intent = Intent(ACTION_STOP_RECORDING)
        sendBroadcast(intent)
    }
    
    private fun updateButtonState() {
        (floatingView as? ImageButton)?.apply {
            if (isRecording) {
                setBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_green_dark))
                setImageResource(android.R.drawable.ic_media_pause)
            } else {
                setBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
                setImageResource(android.R.drawable.ic_menu_camera)
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        floatingView?.let { view ->
            windowManager?.removeView(view)
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_HIDE_OVERLAY -> {
                floatingView?.visibility = View.GONE
            }
            ACTION_SHOW_OVERLAY -> {
                floatingView?.visibility = View.VISIBLE
            }
        }
        return START_STICKY
    }
}
