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
    private lateinit var audioManager: AudioManager
    private lateinit var prefs: SharedPreferences

    private val CHANNEL_ID = "VolumeWidgetServiceChannel"
    private var isExpanded = false

    private lateinit var params: WindowManager.LayoutParams

    override fun onBind(intent: Intent): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        
        prefs = getSharedPreferences("VolumeWidgetPrefs", Context.MODE_PRIVATE)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        createNotificationChannel()
        startForeground(1, createNotification())

        setupFloatingView()
        setupExpandedView()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Volume Widget Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Volume Widget Active")
            .setContentText("Floating volume control is running")
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun setupFloatingView() {
        floatingView = LayoutInflater.from(this).inflate(R.layout.widget_floating, null)
        
        val savedX = prefs.getInt("widget_x", 0)
        val savedY = prefs.getInt("widget_y", 100)

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) 
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY 
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START
        params.x = savedX
        params.y = savedY

        windowManager.addView(floatingView, params)

        floatingView.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isDragging = false
            private val CLICK_THRESHOLD = 10

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isDragging) {
                            toggleExpandedView()
                        } else {
                            prefs.edit()
                                .putInt("widget_x", params.x)
                                .putInt("widget_y", params.y)
                                .apply()
                        }
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val diffX = (event.rawX - initialTouchX).toInt()
                        val diffY = (event.rawY - initialTouchY).toInt()

                        if (abs(diffX) > CLICK_THRESHOLD || abs(diffY) > CLICK_THRESHOLD) {
                            isDragging = true
                        }

                        if (isDragging) {
                            params.x = initialX + diffX
                            params.y = initialY + diffY
                            windowManager.updateViewLayout(floatingView, params)
                        }
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun setupExpandedView() {
        expandedView = LayoutInflater.from(this).inflate(R.layout.widget_expanded, null)
        
        val seekBar = expandedView.findViewById<SeekBar>(R.id.volume_slider)
        val btnClose = expandedView.findViewById<ImageView>(R.id.btn_close)

        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        seekBar.max = maxVolume
        
        btnClose.setOnClickListener {
            toggleExpandedView()
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun toggleExpandedView() {
        if (!isExpanded) {
            val seekBar = expandedView.findViewById<SeekBar>(R.id.volume_slider)
            seekBar.progress = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

            windowManager.removeView(floatingView)
            windowManager.addView(expandedView, params)
            isExpanded = true
        } else {
            windowManager.removeView(expandedView)
            windowManager.addView(floatingView, params)
            isExpanded = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::floatingView.isInitialized && floatingView.parent != null) {
            windowManager.removeView(floatingView)
        }
        if (::expandedView.isInitialized && expandedView.parent != null) {
            windowManager.removeView(expandedView)
        }
    }
}
