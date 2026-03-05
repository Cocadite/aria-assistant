package com.aria.assistente.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aria.assistente.voice.AriaVoiceService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.intent.action.BOOT_COMPLETED") {
            val i = Intent(context, AriaVoiceService::class.java).apply {
                action = AriaVoiceService.ACTION_START
            }
            // Requires Android 8+: startForegroundService when possible
            context.startForegroundService(i)
        }
    }
}
