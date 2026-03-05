package com.aria.assistente

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.aria.assistente.voice.AriaVoiceService
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        status = findViewById(R.id.status)

        findViewById<MaterialButton>(R.id.btn_start).setOnClickListener {
            ensurePermissions()
            ensureOverlayPermission()
            startService(Intent(this, AriaVoiceService::class.java).apply {
                action = AriaVoiceService.ACTION_START
            })
            status.text = "Status: rodando (tentando wake phrases)"
        }

        findViewById<MaterialButton>(R.id.btn_stop).setOnClickListener {
            startService(Intent(this, AriaVoiceService::class.java).apply {
                action = AriaVoiceService.ACTION_STOP
            })
            status.text = "Status: parado"
        }
    }

    private fun ensurePermissions() {
        val need = mutableListOf<String>()
        val perms = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CONTACTS
        )
        for (p in perms) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                need.add(p)
            }
        }
        if (need.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, need.toTypedArray(), 1001)
        }
    }

    private fun ensureOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val i = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(i)
        }
    }
}
