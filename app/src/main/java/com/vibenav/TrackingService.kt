package com.vibenav

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class TrackingService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val proximityMonitor = ProximityMonitor()
    private lateinit var alertManager: AlertManager
    private lateinit var voiceAlertManager: VoiceAlertManager
    private var isTracking = false
    private var currentDistanceMeters = 0
    private var isAlerting = false
    private var prevLocation: Location? = null
    private var prevLocationTime: Long = 0
    private var currentSpeedMs: Float = 0f
    private var lastSpeedKmh: Float = 0f
    private var lastEtaSeconds: Int = -1
    private var destLat: Double = 0.0
    private var destLon: Double = 0.0
    private var screenOnReceiver: BroadcastReceiver? = null
    private val autoStopHandler = Handler(Looper.getMainLooper())
    private var autoStopPending = false

    companion object {
        const val CHANNEL_ID = "vibenav_tracking"
        const val ALERT_CHANNEL_ID = "vibenav_alert"
        const val NOTIFICATION_ID = 1001
        const val ALERT_NOTIFICATION_ID = 1002
        const val ACTION_START = "com.vibenav.START_TRACKING"
        const val ACTION_STOP = "com.vibenav.STOP_TRACKING"
        const val ACTION_SILENCE_ALERT = "com.vibenav.SILENCE_ALERT"
        const val ACTION_UPDATE_DESTINATION = "com.vibenav.UPDATE_DESTINATION"
        const val EXTRA_DEST_LAT = "dest_lat"
        const val EXTRA_DEST_LON = "dest_lon"
        const val EXTRA_THRESHOLD = "threshold"
        const val BROADCAST_DISTANCE = "com.vibenav.DISTANCE_UPDATE"
        const val EXTRA_DISTANCE = "distance"
        const val EXTRA_PROXIMITY = "proximity"
        const val EXTRA_USER_LAT = "user_lat"
        const val EXTRA_USER_LON = "user_lon"
        const val EXTRA_DEST_ADDRESS = "dest_address"
        const val EXTRA_SPEED = "speed"
        const val EXTRA_ETA = "eta"
        const val ACTION_ARRIVED = "com.vibenav.ARRIVED"
        const val EXTRA_AUTO_STOPPED = "auto_stopped"
    }

    override fun onCreate() {
        super.onCreate()
        cancelAlertNotification()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        alertManager = AlertManager(this)
        voiceAlertManager = VoiceAlertManager(this)
        createNotificationChannel()
        createAlertNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        cancelAlertNotification()

        if (intent == null) return START_STICKY

        when (intent.action) {
            ACTION_START -> {
                val lat = intent.getDoubleExtra(EXTRA_DEST_LAT, 0.0)
                val lon = intent.getDoubleExtra(EXTRA_DEST_LON, 0.0)
                val threshold = intent.getDoubleExtra(EXTRA_THRESHOLD, 300.0)
                destLat = lat
                destLon = lon
                proximityMonitor.setDestination(lat, lon)
                proximityMonitor.setThreshold(threshold)
                startTracking()
            }
            ACTION_UPDATE_DESTINATION -> {
                val lat = intent.getDoubleExtra(EXTRA_DEST_LAT, 0.0)
                val lon = intent.getDoubleExtra(EXTRA_DEST_LON, 0.0)
                val threshold = intent.getDoubleExtra(EXTRA_THRESHOLD, 300.0)
                proximityMonitor.setDestination(lat, lon)
                proximityMonitor.setThreshold(threshold)
            }
            ACTION_STOP -> stopTracking()
            ACTION_SILENCE_ALERT -> silenceAlert()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        unregisterScreenOnReceiver()
        cancelAlertNotification()
        autoStopHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun registerScreenOnReceiver() {
        if (screenOnReceiver != null) return
        screenOnReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Intent.ACTION_SCREEN_ON && isAlerting) {
                    showAlertNotification(currentDistanceMeters)
                }
            }
        }
        val filter = IntentFilter(Intent.ACTION_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenOnReceiver, filter, 0)
        } else {
            registerReceiver(screenOnReceiver, filter)
        }
    }

    private fun unregisterScreenOnReceiver() {
        try {
            screenOnReceiver?.let { unregisterReceiver(it) }
        } catch (_: Exception) { }
        screenOnReceiver = null
    }

    private fun startTracking() {
        if (isTracking) return
        isTracking = true
        isAlerting = false
        cancelAlertNotification()

        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateIntervalMillis(3000)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            stopTracking()
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val location = locationResult.lastLocation ?: return
            processLocation(location)
        }
    }

    private fun processLocation(location: Location) {
        if (!proximityMonitor.hasDestination()) return

        val result = proximityMonitor.checkProximity(location)
        val meters = result.distanceMeters.toInt()
        currentDistanceMeters = meters

        val prev = prevLocation
        if (prev != null && prevLocationTime > 0) {
            val dt = (location.time - prevLocationTime) / 1000.0
            if (dt > 0) {
                currentSpeedMs = (location.distanceTo(prev) / dt).toFloat()
            }
        }
        prevLocation = location
        prevLocationTime = location.time

        val speedKmh = currentSpeedMs * 3.6f
        val etaSeconds = if (currentSpeedMs > 0.5f) (meters / currentSpeedMs).toInt() else -1
        lastSpeedKmh = speedKmh
        lastEtaSeconds = etaSeconds

        val distanceIntent = Intent(BROADCAST_DISTANCE).apply {
            putExtra(EXTRA_DISTANCE, meters)
            putExtra(EXTRA_PROXIMITY, result.isInRange)
            putExtra(EXTRA_USER_LAT, location.latitude)
            putExtra(EXTRA_USER_LON, location.longitude)
            putExtra(EXTRA_SPEED, speedKmh)
            putExtra(EXTRA_ETA, etaSeconds)
        }
        sendBroadcast(distanceIntent)

        updateNotification(meters, result.isInRange)

        if (result.justEnteredRange) {
            isAlerting = true
            alertManager.startVibration()
            voiceAlertManager.announceArrived()
            showAlertNotification(meters)
            registerScreenOnReceiver()
            updateNotification(meters, inRange = true)
            scheduleAutoStop()
        } else if (result.isInRange && isAlerting) {
            showAlertNotification(meters)
        } else if (!result.isInRange) {
            voiceAlertManager.announceDistance(meters)
            autoStopHandler.removeCallbacksAndMessages(null)
            autoStopPending = false
        }
    }

    private fun scheduleAutoStop() {
        autoStopPending = true
        autoStopHandler.postDelayed({
            if (autoStopPending && isAlerting) {
                sendBroadcast(Intent(ACTION_ARRIVED).apply {
                    putExtra(EXTRA_AUTO_STOPPED, true)
                })
                stopTracking()
            }
        }, 60000)
    }

    private fun silenceAlert() {
        isAlerting = false
        autoStopPending = false
        autoStopHandler.removeCallbacksAndMessages(null)
        unregisterScreenOnReceiver()
        alertManager.stopAlert()
        voiceAlertManager.stop()
        cancelAlertNotification()
        updateNotificationSilenced()
    }

    private fun stopTracking() {
        isTracking = false
        isAlerting = false
        autoStopPending = false
        autoStopHandler.removeCallbacksAndMessages(null)
        unregisterScreenOnReceiver()
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } catch (_: Exception) { }
        alertManager.stopAlert()
        voiceAlertManager.shutdown()
        cancelAlertNotification()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.deleteNotificationChannel(CHANNEL_ID)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "VibeNav Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows tracking status silently"
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
                setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            }
            nm.createNotificationChannel(channel)
        }
    }

    private fun createAlertNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.deleteNotificationChannel(ALERT_CHANNEL_ID)
            val channel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "VibeNav Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when you reach your destination"
                setShowBadge(true)
                setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                enableVibration(true)
            }
            nm.createNotificationChannel(channel)
        }
    }

    private fun formatDistance(meters: Int): String {
        return if (meters >= 1000) {
            String.format("%.1f km", meters / 1000.0)
        } else {
            "$meters m"
        }
    }

    private fun buildNotification(): Notification {
        val stopIntent = PendingIntent.getService(
            this, 10, Intent(this, TrackingService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openIntent = PendingIntent.getActivity(
            this, 11, Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VibeNav")
            .setContentText("Tracking to destination...")
            .setSubText("Tap to open map")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(openIntent)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun showAlertNotification(meters: Int) {
        val alertIntent = Intent(this, AlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, alertIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val silenceIntent = PendingIntent.getService(
            this, 1, Intent(this, TrackingService::class.java).apply { action = ACTION_SILENCE_ALERT },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this, 2, Intent(this, TrackingService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setContentTitle("VibeNav - Arrived!")
            .setContentText("${formatDistance(meters)} from destination")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true)
            .addAction(android.R.drawable.ic_media_play, "Silence", silenceIntent)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopIntent)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()

        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(ALERT_NOTIFICATION_ID, notification)
    }

    private fun cancelAlertNotification() {
        try {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(ALERT_NOTIFICATION_ID)
        } catch (_: Exception) { }
    }

    private fun updateNotification(meters: Int, inRange: Boolean) {
        val speedKmh = lastSpeedKmh
        val etaSec = lastEtaSeconds
        val speedText = if (speedKmh > 1f) String.format("%.0f km/h", speedKmh) else ""
        val etaText = if (etaSec > 0) {
            val min = etaSec / 60
            val sec = etaSec % 60
            if (min > 0) "${min}m ${sec}s" else "${sec}s"
        } else ""

        val text = when {
            inRange -> "${formatDistance(meters)} - ARRIVED!"
            speedText.isNotEmpty() && etaText.isNotEmpty() ->
                "${formatDistance(meters)} · $etaText · $speedText"
            speedText.isNotEmpty() -> "${formatDistance(meters)} · $speedText"
            else -> "${formatDistance(meters)} from destination"
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VibeNav")
            .setContentText(text)
            .setSubText("Tap to open map")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 11, Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .addAction(android.R.drawable.ic_media_pause, "Stop",
                PendingIntent.getService(
                    this, 10, Intent(this, TrackingService::class.java).apply { action = ACTION_STOP },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, notification)
    }

    private fun updateNotificationSilenced() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VibeNav - Alert Silenced")
            .setContentText("Tracking continues")
            .setSubText("Tap to open map")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
