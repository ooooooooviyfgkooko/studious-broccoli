package com.blackx.helper

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.CountDownTimer
import android.os.Handler
import android.provider.Settings
import android.os.IBinder
import android.os.Looper
import android.view.*
import android.widget.*

class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private lateinit var bubble: TextView
    private lateinit var panel: LinearLayout
    private lateinit var panelWrap: ScrollView
    private lateinit var timerText: TextView
    private var timer: CountDownTimer? = null
    private lateinit var swText: TextView
    private lateinit var notes: EditText
    private val handler = Handler(Looper.getMainLooper())
    private var swStart = 0L
    private var swRunning = false
    private val swTick = object : Runnable {
        override fun run() {
            val s = (System.currentTimeMillis() - swStart) / 1000
            swText.text = "%02d:%02d".format(s / 60, s % 60)
            if (swRunning) handler.postDelayed(this, 500)
        }
    }
    private val prefs by lazy { getSharedPreferences("bx", Context.MODE_PRIVATE) }

    private val type get() = if (Build.VERSION.SDK_INT >= 26)
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE

    override fun onBind(i: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(1, notification())
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        buildBubble(); buildPanel()
    }

    private fun notification(): Notification {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel("bx", "Overlay", NotificationManager.IMPORTANCE_LOW))
        }
        val b = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, "bx") else Notification.Builder(this)
        return b.setContentTitle("BLACK X Helper running").setSmallIcon(android.R.drawable.ic_menu_info_details).build()
    }

    private fun buildBubble() {
        bubble = TextView(this).apply {
            text = "X"; textSize = 24f; setTextColor(Color.parseColor("#FF2D3D")); gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL; setColor(Color.parseColor("#0B0B0E")); setStroke(5, Color.parseColor("#FF2D3D"))
            }
        }
        val p = WindowManager.LayoutParams(150, 150, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.START; x = 20; y = 400
        }
        var sx = 0f; var sy = 0f; var ox = 0; var oy = 0; var moved = false
        bubble.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { sx = e.rawX; sy = e.rawY; ox = p.x; oy = p.y; moved = false }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (e.rawX - sx).toInt(); val dy = (e.rawY - sy).toInt()
                    if (Math.abs(dx) + Math.abs(dy) > 12) moved = true
                    if (moved) { p.x = ox + dx; p.y = oy + dy; wm.updateViewLayout(bubble, p) }
                }
                MotionEvent.ACTION_UP -> if (!moved) { panel.visibility = View.VISIBLE }
            }
            true
        }
        wm.addView(bubble, p)
    }

    private fun btn(label: String, onClick: () -> Unit) = Button(this).apply {
        text = label; setTextColor(Color.WHITE); setBackgroundColor(Color.parseColor("#7A1620")); setOnClickListener { onClick() }
    }

    private fun buildPanel() {
        panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(30, 30, 30, 30); visibility = View.GONE
            background = GradientDrawable().apply {
                cornerRadius = 30f; setColor(Color.parseColor("#F00D0D10")); setStroke(3, Color.parseColor("#FF2D3D"))
            }
        }
        panel.addView(TextView(this).apply {
            text = "BLACK X HELPER"; setTextColor(Color.WHITE); textSize = 18f
        })
        panel.addView(TextView(this).apply {
            text = "Developer: @TheModderr"; setTextColor(Color.GRAY); textSize = 12f; setPadding(0, 0, 0, 20)
        })
        panel.addView(TextView(this).apply { text = "Zone Timer"; setTextColor(Color.WHITE); textSize = 16f })
        timerText = TextView(this).apply { text = "00:00"; setTextColor(Color.parseColor("#FF2D3D")); textSize = 32f }
        panel.addView(timerText)
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf(1, 2, 3).forEach { m -> row.addView(btn("${m}m") { startTimer(m * 60_000L) }) }
        row.addView(btn("Stop") { timer?.cancel(); timerText.text = "00:00" })
        panel.addView(row)
        panel.addView(TextView(this).apply { text = "Round Stopwatch"; setTextColor(Color.WHITE); textSize = 16f; setPadding(0, 20, 0, 0) })
        swText = TextView(this).apply { text = "00:00"; setTextColor(Color.parseColor("#FF2D3D")); textSize = 32f }
        panel.addView(swText)
        val row2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row2.addView(btn("Start") {
            if (!swRunning) { swStart = System.currentTimeMillis(); swRunning = true; handler.post(swTick) }
        })
        row2.addView(btn("Stop") { swRunning = false })
        panel.addView(row2)
        panel.addView(TextView(this).apply { text = "Smooth FPS"; setTextColor(Color.WHITE); textSize = 16f; setPadding(0, 20, 0, 0) })
        @Suppress("DEPRECATION")
        val hz = wm.defaultDisplay.refreshRate.toInt()
        panel.addView(TextView(this).apply {
            text = "Screen refresh rate: $hz Hz"; setTextColor(Color.parseColor("#FF2D3D")); textSize = 14f
        })
        panel.addView(TextView(this).apply {
            text = "Tip: highest refresh rate on, battery saver off"; setTextColor(Color.GRAY); textSize = 11f
        })
        val row3 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row3.addView(btn("Display") { openSettings(Settings.ACTION_DISPLAY_SETTINGS) })
        row3.addView(btn("Battery Saver") { openSettings(Settings.ACTION_BATTERY_SAVER_SETTINGS) })
        panel.addView(row3)
        panel.addView(TextView(this).apply { text = "Notes (auto-saved)"; setTextColor(Color.WHITE); textSize = 16f; setPadding(0, 20, 0, 0) })
        notes = EditText(this).apply {
            hint = "Drop spot, team plan..."; setHintTextColor(Color.GRAY); setTextColor(Color.WHITE); minLines = 3
            setText(prefs.getString("notes", ""))
        }
        panel.addView(notes)
        panel.addView(btn("Close") {
            prefs.edit().putString("notes", notes.text.toString()).apply()
            panel.visibility = View.GONE
        })
        val p = WindowManager.LayoutParams(720, WindowManager.LayoutParams.WRAP_CONTENT, type,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.CENTER
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
        }
        panelWrap = ScrollView(this).apply { addView(panel) }
        wm.addView(panelWrap, p)
    }

    private fun openSettings(action: String) {
        try { startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (e: Exception) {
            Toast.makeText(this, "Settings khul nahi payi", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startTimer(ms: Long) {
        timer?.cancel()
        timer = object : CountDownTimer(ms, 1000) {
            override fun onTick(left: Long) {
                val s = left / 1000; timerText.text = "%02d:%02d".format(s / 60, s % 60)
            }
            override fun onFinish() { timerText.text = "00:00" }
        }.start()
    }

    override fun onDestroy() {
        timer?.cancel(); swRunning = false
        if (::notes.isInitialized) prefs.edit().putString("notes", notes.text.toString()).apply()
        if (::bubble.isInitialized) wm.removeView(bubble)
        if (::panelWrap.isInitialized) wm.removeView(panelWrap)
        super.onDestroy()
    }
}
