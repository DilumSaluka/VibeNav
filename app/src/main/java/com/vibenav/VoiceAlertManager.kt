package com.vibenav

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class VoiceAlertManager(context: Context) {

    private var tts: TextToSpeech? = null
    private var isReady = false
    private var lastSpokenDistance = -1
    private var lastSpokenMessage = ""

    init {
        tts = TextToSpeech(context) { status ->
            isReady = (status == TextToSpeech.SUCCESS)
            tts?.language = Locale.US
        }
    }

    fun announceDistance(meters: Int) {
        if (!isReady) return

        val message = when {
            meters <= 50 -> "Arrived at destination"
            meters <= 100 -> "100 meters away"
            meters <= 200 -> "200 meters away"
            meters <= 300 -> "300 meters away"
            meters <= 500 -> "500 meters away"
            meters <= 800 -> "800 meters away"
            meters <= 1000 -> "One kilometer away"
            else -> null
        }

        if (message != null && message != lastSpokenMessage) {
            speak(message)
            lastSpokenMessage = message
        }
    }

    fun announceArrived() {
        if (!isReady) return
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
