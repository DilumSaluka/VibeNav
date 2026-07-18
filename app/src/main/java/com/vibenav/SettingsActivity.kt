package com.vibenav

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import java.io.File

class SettingsActivity : AppCompatActivity() {

    private lateinit var fineLocationStatus: TextView
    private lateinit var backgroundLocationStatus: TextView
    private lateinit var notificationsStatus: TextView
    private lateinit var radiusValueText: TextView
    private lateinit var radiusSeekBar: SeekBar

    private val prefs by lazy { getSharedPreferences("vibenav", Context.MODE_PRIVATE) }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        refreshPermissionStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        fineLocationStatus = findViewById(R.id.fineLocationStatus)
        backgroundLocationStatus = findViewById(R.id.backgroundLocationStatus)
        notificationsStatus = findViewById(R.id.notificationsStatus)
        radiusValueText = findViewById(R.id.radiusValueText)
        radiusSeekBar = findViewById(R.id.radiusSeekBar)

        refreshPermissionStatus()
        setupRadiusSlider()

        findViewById<MaterialButton>(R.id.requestPermissionsButton).setOnClickListener {
            requestDeniedPermissions()
        }

        findViewById<MaterialButton>(R.id.batteryOptimizationButton).setOnClickListener {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = android.net.Uri.parse("package:$packageName")
            }
            startActivity(intent)
        }

        findViewById<MaterialButton>(R.id.savedPlacesButton).setOnClickListener {
            val intent = Intent(this, SavedPlacesActivity::class.java)
            startActivity(intent)
        }

        findViewById<MaterialButton>(R.id.tripHistoryButton).setOnClickListener {
            val intent = Intent(this, TripHistoryActivity::class.java)
            startActivity(intent)
        }

        findViewById<MaterialButton>(R.id.clearCacheButton).setOnClickListener {
            clearMapCache()
        }

        findViewById<MaterialButton>(R.id.guideButton).setOnClickListener {
            val intent = Intent(this, HelpActivity::class.java)
            startActivity(intent)
        }

        findViewById<MaterialButton>(R.id.clearDataButton).setOnClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.parse("package:$packageName")
            }
            startActivity(intent)
        }

        findViewById<MaterialButton>(R.id.tutorialHelpButton).setOnClickListener {
            val intent = Intent(this, TutorialActivity::class.java)
            intent.putExtra("fromSettings", true)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionStatus()
    }

    private fun setupRadiusSlider() {
        val savedRadius = prefs.getInt("alert_radius", 300)
        val progress = radiusToProgress(savedRadius)
        radiusSeekBar.progress = progress
        radiusValueText.text = "$savedRadius m"

        radiusSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val meters = progressToRadius(progress)
                radiusValueText.text = "$meters m"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                val meters = progressToRadius(seekBar.progress)
                prefs.edit().putInt("alert_radius", meters).apply()
                Toast.makeText(this@SettingsActivity, "Radius set to $meters m", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun progressToRadius(progress: Int): Int {
        return 50 + progress * 50
    }

    private fun radiusToProgress(radius: Int): Int {
        val p = (radius - 50) / 50
        return p.coerceIn(0, 19)
    }

    private fun refreshPermissionStatus() {
        val fineLoc = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        fineLocationStatus.text = "Fine Location: ${if (fineLoc) "Granted" else "Denied"}"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            backgroundLocationStatus.visibility = TextView.VISIBLE
            val bgLoc = ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            backgroundLocationStatus.text = "Background Location: ${if (bgLoc) "Granted" else "Denied"}"
        } else {
            backgroundLocationStatus.visibility = TextView.GONE
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationsStatus.visibility = TextView.VISIBLE
            val notif = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            notificationsStatus.text = "Notifications: ${if (notif) "Granted" else "Denied"}"
        } else {
            notificationsStatus.visibility = TextView.GONE
        }
    }

    private fun requestDeniedPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val denied = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (denied.isEmpty()) {
            Toast.makeText(this, "All permissions already granted", Toast.LENGTH_SHORT).show()
            return
        }

        val permanentlyDenied = denied.any {
            !shouldShowRequestPermissionRationale(it)
        }

        if (permanentlyDenied) {
            Toast.makeText(this, "Permission permanently denied — opening Settings", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.parse("package:$packageName")
            }
            startActivity(intent)
        } else {
            requestPermissionLauncher.launch(denied.toTypedArray())
        }
    }

    private fun clearMapCache() {
        try {
            val cacheDir = File(cacheDir, "osmdroid")
            if (cacheDir.exists()) {
                cacheDir.deleteRecursively()
            }
            Toast.makeText(this, "Map cache cleared", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to clear cache: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
