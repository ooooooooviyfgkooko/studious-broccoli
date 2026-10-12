package com.blackx.helper

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.app.Activity

class MainActivity : Activity() {
    private val gamePkg = "com.dts.freefiremax"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(loginView())
    }

    private fun header(root: LinearLayout) {
        root.addView(TextView(this).apply {
            text = "BLACK X"; textSize = 38f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
        })
        root.addView(TextView(this).apply {
            text = "HELPER"; textSize = 20f; letterSpacing = 0.5f
            setTextColor(Color.parseColor("#FF2D3D")); gravity = Gravity.CENTER
        })
        root.addView(TextView(this).apply {
            text = "Developer: @TheModderr"
            textSize = 14f; setTextColor(Color.GRAY); gravity = Gravity.CENTER; setPadding(0, 14, 0, 50)
        })
    }

    private fun loginView(): LinearLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#050506"))
            setPadding(60, 60, 60, 60)
        }
        header(root)
        val key = EditText(this).apply {
            hint = "ENTER KEY"; setHintTextColor(Color.GRAY); setTextColor(Color.WHITE)
            gravity = Gravity.CENTER; inputType = InputType.TYPE_CLASS_NUMBER
        }
        val err = TextView(this).apply { setTextColor(Color.parseColor("#FF2D3D")); gravity = Gravity.CENTER }
        root.addView(key); root.addView(err)
        root.addView(Button(this).apply {
            text = "LOGIN"; setTextColor(Color.WHITE); setBackgroundColor(Color.parseColor("#E5202F"))
            setOnClickListener {
                if (key.text.toString().trim() == "55") setContentView(startView())
                else err.text = "Invalid key"
            }
        })
        return root
    }

    private fun startView(): LinearLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#050506"))
            setPadding(60, 60, 60, 60)
        }
        header(root)
        root.addView(Button(this).apply {
            text = "START"
            setTextColor(Color.WHITE); setBackgroundColor(Color.parseColor("#E5202F"))
            setOnClickListener { start() }
        })
        return root
    }

    private fun start() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Allow 'Display over other apps', phir START dabao", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            return
        }
        val svc = Intent(this, OverlayService::class.java)
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(svc) else startService(svc)
        val game = packageManager.getLaunchIntentForPackage(gamePkg)
        if (game != null) startActivity(game)
        else Toast.makeText(this, "Free Fire MAX installed nahi hai", Toast.LENGTH_LONG).show()
    }
}
