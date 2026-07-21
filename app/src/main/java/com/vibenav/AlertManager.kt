package com.vibenav

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class AlertManager(private val context: Context) {

    private var isVibrating = false
    private var toneGen: ToneGenerator? = null
    private var mediaPlayer: MediaPlayer? = null

    private val prefs by lazy { context.getSharedPreferences("vibenav", Context.MODE_PRIVATE) }

    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    fun startVibration() {
        if (isVibrating) return
        isVibrating = true
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 800, 300, 800, 300, 800, 300)
                val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255, 0)
                val effect = VibrationEffect.createWaveform(timings, amplitudes, 0)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 800, 300, 800, 300, 800, 300), 0)
            }
            playAlertSound()
        } catch (_: Exception) { }
    }

    private fun playAlertSound() {
        val soundUri = prefs.getString("alert_sound_uri", null)
        if (soundUri != null) {
            playCustomSound(Uri.parse(soundUri))
        } else {
            playDefaultBeep()
        }
    }

    private fun playDefaultBeep() {
        try {
            toneGen?.release()
            toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            toneGen?.startTone(ToneGenerator.TONE_PROP_ACK, 1500)
        } catch (_: Exception) { }
    }

    private fun playCustomSound(uri: Uri) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build())
                setDataSource(context, uri)
                setOnCompletionListener { stopAlert() }
                prepare()
                start()
            }
        } catch (_: Exception) {
            playDefaultBeep()
        }
    }

    fun playTestSound(uri: Uri?) {
        try {
            if (uri != null) {
                MediaPlayer().apply {
                    setAudioAttributes(android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build())
                    setDataSource(context, uri)
                    setOnCompletionListener { release() }
                    prepare()
                    start()
                }
            } else {
                ToneGenerator(AudioManager.STREAM_ALARM, 100).apply {
                    startTone(ToneGenerator.TONE_PROP_ACK, 1500)
                    release()
                }
            }
        } catch (_: Exception) { }
    }

    fun stopAlert() {
        isVibrating = false
        try {
            vibrator.cancel()
            toneGen?.stopTone()
            toneGen?.release()
            toneGen = null
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (_: Exception) { }
    }

    fun isAlertActive(): Boolean = isVibrating
}
