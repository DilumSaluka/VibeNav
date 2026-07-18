package com.vibenav

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class AlertManager(private val context: Context) {

    private var isVibrating = false
    private var toneGen: ToneGenerator? = null

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
            playBeep()
        } catch (_: Exception) { }
    }

    private fun playBeep() {
        try {
            toneGen?.release()
            toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            toneGen?.startTone(ToneGenerator.TONE_PROP_ACK, 1500)
        } catch (_: Exception) { }
    }

    fun stopAlert() {
        isVibrating = false
        try {
            vibrator.cancel()
            toneGen?.stopTone()
            toneGen?.release()
            toneGen = null
        } catch (_: Exception) { }
    }

    fun isAlertActive(): Boolean = isVibrating
}
