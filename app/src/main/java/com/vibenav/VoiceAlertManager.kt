package com.vibenav

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class VoiceAlertManager(context: Context) {

    private var tts: TextToSpeech? = null
    private var isReady = false
    private var lastSpokenDistance = -1
    private var lastSpokenMessage = ""
    private val prefs = context.getSharedPreferences("vibenav", Context.MODE_PRIVATE)

    init {
        tts = TextToSpeech(context) { status ->
            isReady = (status == TextToSpeech.SUCCESS)
            tts?.language = Locale.US
        }
    }

    private fun isVoiceEnabled(): Boolean {
        return prefs.getBoolean("voice_alerts_enabled", true)
    }

    fun announceDistance(meters: Int) {
        if (!isReady || !isVoiceEnabled()) return

        val rounded = ((meters + 50) / 100) * 100
        val message = when {
            rounded <= 0 -> "Arrived at destination"
            rounded <= 100 -> "100 meters away"
            rounded <= 200 -> "200 meters away"
            rounded <= 300 -> "300 meters away"
            rounded <= 400 -> "400 meters away"
            rounded <= 500 -> "500 meters away"
            rounded <= 600 -> "600 meters away"
            rounded <= 700 -> "700 meters away"
            rounded <= 800 -> "800 meters away"
            rounded <= 900 -> "900 meters away"
            rounded <= 1000 -> "One kilometer away"
            else -> null
        }

        if (message != null && message != lastSpokenMessage) {
            speak(message)
            lastSpokenMessage = message
        }
    }

    fun announceArrived() {
        if (!isReady || !isVoiceEnabled()) return
        speak("You have arrived at your destination")
        lastSpokenMessage = "You have arrived at your destination"
    }

    private fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        lastSpokenMessage = ""
        lastSpokenDistance = -1
        tts?.stop()
        tts?.shutdown()
    }
}
