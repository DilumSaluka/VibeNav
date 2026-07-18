package com.vibenav

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView

class AlertActivity : AppCompatActivity() {

    private var distanceUpdateReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alert)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setTurnScreenOn(true)
            setShowWhenLocked(true)
        }

        findViewById<MaterialButton>(R.id.dismissButton).setOnClickListener {
            stopVibration()
            Intent(this, TrackingService::class.java).apply {
                action = TrackingService.ACTION_SILENCE_ALERT
                startService(this)
            }
            finish()
        }

        findViewById<MaterialButton>(R.id.stopMissionButton).setOnClickListener {
            stopVibration()
            Intent(this, TrackingService::class.java).apply {
                action = TrackingService.ACTION_STOP
                startService(this)
            }
            finish()
        }

        registerDistanceReceiver()
    }

    private fun stopVibration() {
        try {
            val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            vibrator.cancel()
        } catch (_: Exception) { }
    }

    private fun registerDistanceReceiver() {
        val filter = IntentFilter(TrackingService.BROADCAST_DISTANCE)
        distanceUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                try {
                    val meters = intent.getIntExtra(TrackingService.EXTRA_DISTANCE, 0)
                    val text = if (meters >= 1000) {
                        String.format("%.2f km away", meters / 1000.0)
                    } else {
                        "$meters m away"
                    }
                    findViewById<MaterialTextView>(R.id.alertMessage).text = text
                } catch (_: Exception) { }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(distanceUpdateReceiver, filter, 0)
        } else {
            registerReceiver(distanceUpdateReceiver, filter)
        }
    }

    override fun onDestroy() {
        try { distanceUpdateReceiver?.let { unregisterReceiver(it) } } catch (_: Exception) { }
        super.onDestroy()
    }
}
