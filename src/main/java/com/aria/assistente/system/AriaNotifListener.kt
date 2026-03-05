package com.aria.assistente.system

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import java.util.Locale

class AriaNotifListener : NotificationListenerService(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this, this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName ?: return
        if (pkg != "com.whatsapp") return

        val extras = sbn.notification.extras
        val title = extras.getString("android.title") ?: "WhatsApp"
        val text = extras.getCharSequence("android.text")?.toString() ?: return

        // Starter behavior: announce + read directly.
        // Upgrade idea: store message and wait for "sim" in AriaVoiceService before reading.
        speak("Mensagem de $title no WhatsApp.")
        speak(text)
    }

    private fun speak(msg: String) {
        tts?.speak(msg, TextToSpeech.QUEUE_ADD, null, "notif")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("pt", "BR")
            tts?.setSpeechRate(1.0f)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tts?.shutdown()
        tts = null
    }
}
