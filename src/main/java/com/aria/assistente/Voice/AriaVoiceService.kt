package com.aria.assistente.voice

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import com.aria.assistente.MainActivity
import com.aria.assistente.system.OverlayBubble
import java.util.Locale

class AriaVoiceService : Service(), TextToSpeech.OnInitListener {

    companion object {
        const val ACTION_START = "com.aria.assistente.voice.START"
        const val ACTION_STOP = "com.aria.assistente.voice.STOP"
        private const val CHANNEL_ID = "aria_voice"
        private const val NOTIF_ID = 6006

        // Wake phrases (normalized: lower-case, no accents)
        private val WAKE_PHRASES = listOf(
            "aria",
            "hey aria",
            "hello aria",
            "ola aria"
        )
    }

    private var tts: TextToSpeech? = null
    private var sr: SpeechRecognizer? = null
    private var listening = false
    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var overlay: OverlayBubble

    override fun onCreate() {
        super.onCreate()
        createChannel()
        tts = TextToSpeech(this, this)
        overlay = OverlayBubble(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startAria()
            ACTION_STOP -> stopAria()
        }
        return START_STICKY
    }

    private fun startAria() {
        startForeground(NOTIF_ID, buildNotif("ARIA ativa — esperando 'Hey Ária'..."))
        acquireWakeLock()
        startWakeLoop()
    }

    private fun stopAria() {
        listening = false
        sr?.destroy()
        sr = null
        overlay.hide()
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startWakeLoop() {
        if (listening) return
        listening = true

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            speak("Reconhecimento de voz não disponível neste aparelho.")
            return
        }

        sr = SpeechRecognizer.createSpeechRecognizer(this)
        sr?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: android.os.Bundle?) {
                overlay.showListening()
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {
                overlay.pulse(rmsdB)
            }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                if (listening) restartListening(350)
            }
            override fun onResults(results: android.os.Bundle?) {
                val texts = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                val best = texts.firstOrNull().orEmpty()
                handleHeard(best)
                if (listening) restartListening(200)
            }
            override fun onPartialResults(partialResults: android.os.Bundle?) {}
            override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
        })

        restartListening(0)
    }

    private fun restartListening(delayMs: Long) {
        overlay.showIdle()
        val handler = android.os.Handler(mainLooper)
        handler.postDelayed({
            try {
                val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                }
                sr?.startListening(i)
            } catch (_: Exception) { }
        }, delayMs)
    }

    private fun handleHeard(text: String) {
        val norm = normalize(text)
        if (norm.isBlank()) return

        val woke = WAKE_PHRASES.any { w ->
            norm == w || norm.startsWith("$w ") || norm.contains(" $w ")
        }

        if (woke) {
            overlay.showActivated()
            speak("Pode falar!")
            val cmd = extractCommand(norm)
            if (cmd.isNotBlank()) runCommand(cmd)
        }
    }

    private fun extractCommand(norm: String): String {
        var s = norm
        for (w in WAKE_PHRASES.sortedByDescending { it.length }) {
            s = s.replace(Regex("^$w\s+"), "")
        }
        return s.trim()
    }

    private fun runCommand(cmd: String) {
        when {
            cmd.contains("abrir whatsapp") -> openApp("com.whatsapp")
            cmd.contains("abrir camera") || cmd.contains("abrir câmera") -> openCamera()
            cmd.contains("status") || cmd.contains("como esta") || cmd.contains("como está") -> speak(SystemStatus.summary(this))
            else -> speak("Comando offline ainda não cadastrado: $cmd")
        }
    }

    private fun openApp(pkg: String) {
        val launch = packageManager.getLaunchIntentForPackage(pkg)
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(launch)
            speak("Abrindo.")
        } else {
            speak("Não achei esse app instalado.")
        }
    }

    private fun openCamera() {
        val i = Intent("android.media.action.IMAGE_CAPTURE").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(i)
            speak("Abrindo câmera.")
        } catch (_: Exception) {
            speak("Não consegui abrir a câmera.")
        }
    }

    private fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "aria")
        startForeground(NOTIF_ID, buildNotif(text.take(80)))
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("pt", "BR")
            tts?.setSpeechRate(1.0f)
        }
    }

    private fun buildNotif(line: String): Notification {
        val pi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("ARIA")
            .setContentText(line)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "ARIA Voz", NotificationManager.IMPORTANCE_LOW)
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(ch)
        }
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ARIA::WakeLock")
        wakeLock?.acquire(10 * 60 * 1000L)
    }

    private fun releaseWakeLock() {
        try { wakeLock?.release() } catch (_: Exception) {}
        wakeLock = null
    }

    override fun onDestroy() {
        super.onDestroy()
        sr?.destroy()
        sr = null
        tts?.shutdown()
        tts = null
        overlay.hide()
        releaseWakeLock()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun normalize(s: String): String {
        val lower = s.lowercase(Locale.ROOT).trim()
        val noAcc = java.text.Normalizer.normalize(lower, java.text.Normalizer.Form.NFD)
            .replace(Regex("\p{InCombiningDiacriticalMarks}+"), "")
        return noAcc.replace(Regex("\s+"), " ")
    }
}
