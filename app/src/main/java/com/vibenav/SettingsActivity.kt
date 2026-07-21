package com.vibenav

import android.Manifest
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
        val savedMode = prefs.getInt("night_mode", -1)
        if (savedMode != -1) {
            AppCompatDelegate.setDefaultNightMode(savedMode)
        }
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

        updateThemeButton()
        findViewById<MaterialButton>(R.id.themeToggleButton).setOnClickListener {
            val currentMode = AppCompatDelegate.getDefaultNightMode()
            val newMode = when (currentMode) {
                AppCompatDelegate.MODE_NIGHT_YES -> AppCompatDelegate.MODE_NIGHT_NO
                else -> AppCompatDelegate.MODE_NIGHT_YES
            }
            AppCompatDelegate.setDefaultNightMode(newMode)
            prefs.edit().putInt("night_mode", newMode).apply()
            recreate()
        }

        findViewById<MaterialButton>(R.id.checkUpdateButton).setOnClickListener {
            checkForUpdates()
        }

        findViewById<MaterialButton>(R.id.offlineDownloadButton).setOnClickListener {
            downloadOfflineArea()
        }

        updateAlertSoundName()
        findViewById<MaterialButton>(R.id.pickSoundButton).setOnClickListener {
            val uriString = prefs.getString("alert_sound_uri", null)
            val currentUri = if (uriString != null) Uri.parse(uriString) else null
            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alert Sound")
                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentUri)
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            }
            soundPickerLauncher.launch(intent)
        }
        findViewById<MaterialButton>(R.id.resetSoundButton).setOnClickListener {
            prefs.edit().remove("alert_sound_uri").apply()
            updateAlertSoundName()
            Toast.makeText(this, "Reset to default beep", Toast.LENGTH_SHORT).show()
        }
        findViewById<MaterialButton>(R.id.testSoundButton).setOnClickListener {
            val uriString = prefs.getString("alert_sound_uri", null)
            val uri = if (uriString != null) Uri.parse(uriString) else null
            AlertManager(this).playTestSound(uri)
        }
    }

    private val soundPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (uri != null) {
                prefs.edit().putString("alert_sound_uri", uri.toString()).apply()
                updateAlertSoundName()
                Toast.makeText(this, "Alert sound saved", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateAlertSoundName() {
        val textView = findViewById<TextView>(R.id.alertSoundName)
        val uriString = prefs.getString("alert_sound_uri", null)
        if (uriString != null) {
            val name = RingtoneManager.getRingtone(this, Uri.parse(uriString))?.getTitle(this)
            textView.text = name ?: "Custom Sound"
        } else {
            textView.text = "Default Beep"
        }
    }

    private fun updateThemeButton() {
        val btn = findViewById<MaterialButton>(R.id.themeToggleButton)
        val isDark = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
        btn.text = if (isDark) "☀️ Light Mode" else "🌙 Dark Mode"
    }

    private fun checkForUpdates() {
        val updateManager = UpdateManager(this)
        val btn = findViewById<MaterialButton>(R.id.checkUpdateButton)
        btn.isEnabled = false
        btn.text = "⏳ Checking..."

        updateManager.checkForUpdate { info ->
            runOnUiThread {
                btn.isEnabled = true
                btn.text = "🔍 Check for Updates"

                if (info == null) {
                    Toast.makeText(this, "Failed to check for updates. Check your internet connection.", Toast.LENGTH_LONG).show()
                    return@runOnUiThread
                }

                if (!updateManager.needsUpdate(info.latestVersion)) {
                    Toast.makeText(this, "✅ You're up to date (v${info.latestVersion})", Toast.LENGTH_LONG).show()
                    return@runOnUiThread
                }

                AlertDialog.Builder(this).apply {
                    setTitle("📲 Update Available")
                    setMessage("Version ${info.tagName} is available.\n\nCurrent: v${updateManager.currentVersion}\n\n${info.releaseNotes.take(300)}")
                    setPositiveButton("Download & Install") { _, _ ->
                        updateManager.downloadAndInstall(info.apkUrl, info.tagName)
                    }
                    setNegativeButton("Later", null)
                    show()
                }
            }
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

    private fun downloadOfflineArea() {
        val lm = getSystemService(LOCATION_SERVICE) as LocationManager
        val hasGps = try {
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (_: Exception) { false }

        val locText = if (hasGps) "your current location" else "Colombo (default area)"

        AlertDialog.Builder(this).apply {
            setTitle("📥 Download Offline Map")
            setMessage("This will download map tiles for $locText within a 5 km radius at zoom levels 10–18.\n\nThis uses mobile data. Approximate size: 10–50 MB.")
            setPositiveButton("Download") { _, _ ->
                startOfflineDownload()
            }
            setNegativeButton("Cancel", null)
            show()
        }
    }

    private fun startOfflineDownload() {
        val lm = getSystemService(LOCATION_SERVICE) as LocationManager
        var lat = 6.9271  // default Colombo
        var lon = 79.8612
        try {
            val gpsLoc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (gpsLoc != null) {
                lat = gpsLoc.latitude
                lon = gpsLoc.longitude
            }
        } catch (_: Exception) {}

        val progressDialog = ProgressDialog(this).apply {
            setTitle("Downloading Offline Map")
            setMessage("Downloading tiles...")
            setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            setCancelable(false)
            show()
        }

        val offlineManager = OfflineTileManager(this)
        val statusText = findViewById<TextView>(R.id.offlineStatus)

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            progressDialog.max = 100
            progressDialog.progress = 0

            offlineManager.prefetchTiles(
                centerLat = lat,
                centerLon = lon,
                radiusKm = 5.0,
                minZoom = 10,
                maxZoom = 18,
                updateProgress = { downloaded, total ->
                    progressDialog.max = total
                    progressDialog.progress = downloaded
                    progressDialog.setMessage("Downloaded $downloaded / $total tiles")
                }
            )

            progressDialog.dismiss()
            val cacheSize = offlineManager.getCacheSize()
            val mb = cacheSize / (1024.0 * 1024.0)
            statusText.text = "✅ Offline map ready — ${String.format("%.1f", mb)} MB cached"
            Toast.makeText(this@SettingsActivity, "✅ Offline map download complete", Toast.LENGTH_LONG).show()
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
