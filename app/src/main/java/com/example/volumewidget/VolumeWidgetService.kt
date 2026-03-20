package com.example.volumewidget

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.SeekBar
import androidx.core.app.NotificationCompat
import kotlin.math.abs

class VolumeWidgetService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var expandedView: View
    private lateinit var dimOverlayView: View
    private lateinit var audioManager: AudioManager
    private lateinit var prefs: SharedPreferences

    private val CHANNEL_ID = "VolumeWidgetServiceChannel"
    private var isExpanded = false

    private lateinit var floatingParams: WindowManager.LayoutParams
    private lateinit var expandedParams: WindowManager.LayoutParams
    private lateinit var dimParams: WindowManager.LayoutParams

    override fun onBind(intent: Intent): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        
        prefs = getSharedPreferences("VolumeWidgetPrefs", Context.MODE_PRIVATE)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        createNotificationChannel()
        startForeground(1, createNotification())

        setupViews()
    }

    private fun setupViews() {
        val inflater = LayoutInflater.from(this)
        floatingView = inflater.inflate(R.layout.widget_floating, null)
        expandedView = inflater.inflate(R.layout.widget_expanded, null)
        dimOverlayView = inflater.inflate(R.layout.view_dim_overlay, null)

        setupFloatingParams()
        setupExpandedParams()
        setupDimParams()

        windowManager.addView(floatingView, floatingParams)

        setupFloatingTouchListener()
        setupExpandedSliders()
        
        dimOverlayView.setOnClickListener {
            if (isExpanded) toggleExpandedView()
        }
    }

    private fun setupFloatingParams() {
        val savedX = prefs.getInt("widget_x", 0)
        val savedY = prefs.getInt("widget_y", 200)

        floatingParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) 
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY 
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = savedX
            y = savedY
        }
    }

    private fun setupExpandedParams() {
        expandedParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) 
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY 
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }
    }

    private fun setupDimParams() {
        dimParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) 
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY 
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
    }

    private fun setupFloatingTouchListener() {
        floatingView.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isDragging = false
            private val CLICK_THRESHOLD = 15

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = floatingParams.x
                        initialY = floatingParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isDragging) {
                            toggleExpandedView()
                        } else {
                            prefs.edit().putInt("widget_x", floatingParams.x).putInt("widget_y", floatingParams.y).apply()
                        }
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val diffX = (event.rawX - initialTouchX).toInt()
                        val diffY = (event.rawY - initialTouchY).toInt()
                        if (abs(diffX) > CLICK_THRESHOLD || abs(diffY) > CLICK_THRESHOLD) isDragging = true
                        if (isDragging) {
                            floatingParams.x = initialX + diffX
                            floatingParams.y = initialY + diffY
                            windowManager.updateViewLayout(floatingView, floatingParams)
                        }
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun setupExpandedSliders() {
        setupSlider(expandedView.findViewById(R.id.slider_media), AudioManager.STREAM_MUSIC)
        setupSlider(expandedView.findViewById(R.id.slider_ring), AudioManager.STREAM_RING)
        setupSlider(expandedView.findViewById(R.id.slider_alarm), AudioManager.STREAM_ALARM)
    }

    private fun setupSlider(seekBar: SeekBar, streamType: Int) {
        seekBar.max = audioManager.getStreamMaxVolume(streamType)
        seekBar.progress = audioManager.getStreamVolume(streamType)
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) audioManager.setStreamVolume(streamType, progress, 0)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
    }

    private fun toggleExpandedView() {
        if (!isExpanded) {
            // Refresh volumes before showing
            expandedView.findViewById<SeekBar>(R.id.slider_media).progress = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            expandedView.findViewById<SeekBar>(R.id.slider_ring).progress = audioManager.getStreamVolume(AudioManager.STREAM_RING)
            expandedView.findViewById<SeekBar>(R.id.slider_alarm).progress = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)

            windowManager.addView(dimOverlayView, dimParams)
            windowManager.addView(expandedView, expandedParams)
            isExpanded = true
        } else {
            windowManager.removeView(expandedView)
            windowManager.removeView(dimOverlayView)
            // No need to add back floatingView as it's never removed, just obscured or moved
            // Wait, actually I should probably move the floating icon or hide it?
            // Premium design: keeping the icon exactly where it was.
            isExpanded = false
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Volume Widget Service", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Volume Widget Active")
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::floatingView.isInitialized && floatingView.parent != null) windowManager.removeView(floatingView)
        if (::expandedView.isInitialized && expandedView.parent != null) windowManager.removeView(expandedView)
        if (::dimOverlayView.isInitialized && dimOverlayView.parent != null) windowManager.removeView(dimOverlayView)
    }
}
