package com.vibenav

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.location.Address

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.osmdroid.util.MapTileIndex

class MainActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var searchInput: EditText
    private lateinit var trackingButton: MaterialButton
    private lateinit var removePinButton: MaterialButton
    private lateinit var locateMeFab: FloatingActionButton
    private lateinit var settingsFab: MaterialButton
    private lateinit var satToggleButton: MaterialButton
    private lateinit var helpButton: MaterialButton
    private lateinit var savePlaceButton: MaterialButton
    private lateinit var shareButton: MaterialButton
    private lateinit var distanceText: TextView
    private lateinit var currentLocationText: TextView
    private lateinit var destinationLabel: TextView
    private lateinit var weatherText: TextView
    private lateinit var buttonRow: android.widget.LinearLayout
    private lateinit var speedMeter: android.view.View
    private lateinit var speedValue: TextView
    private lateinit var voiceToggleButton: MaterialButton
    private lateinit var themeToggleButton: MaterialButton
    private var routePolyline: Polyline? = null
    private var roadDistanceKm: Double = 0.0
    private var roadDurationSec: Int = 0

    private lateinit var searchSuggestions: ListView
    private lateinit var searchAdapter: ArrayAdapter<String>
    private val searchResults = mutableListOf<Triple<String, Double, Double>>()
    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchJob: Job? = null

    private val geocoderHelper by lazy { GeocoderHelper(this) }
    private val proximityMonitor = ProximityMonitor()
    private var isTracking = false
    private var voiceEnabled = true
    private var destinationMarker: Marker? = null
    private var directionArrow: Marker? = null
    private var radiusCircle: Polygon? = null
    private var userRadiusCircle: Polygon? = null
    private var myLocationOverlay: MyLocationNewOverlay? = null
    private var selectedAddress: Address? = null
    private var gestureDetector: GestureDetector? = null
    private var currentUserLat: Double = 0.0
    private var currentUserLon: Double = 0.0
    private var isForeground = false

    private val prefs by lazy { getSharedPreferences("vibenav", Context.MODE_PRIVATE) }

    private var distanceUpdateReceiver: BroadcastReceiver? = null
    private var arrivedReceiver: BroadcastReceiver? = null

    private var permissionDialogShownThisSession = false
    private var isMapInitialized = false
    private var trackingStartTime: Long = 0
    private var maxTrackingDistance = 0
    private var lastReverseGeocodeLat = 0.0
    private var lastReverseGeocodeLon = 0.0
    private var lastReverseGeocodeTime: Long = 0

    private val savedPlacesLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val lat = result.data?.getDoubleExtra("lat", 0.0) ?: 0.0
            val lon = result.data?.getDoubleExtra("lon", 0.0) ?: 0.0
            if (lat != 0.0 || lon != 0.0) {
                placePin(GeoPoint(lat, lon))
                mapView.controller.animateTo(GeoPoint(lat, lon), 15.0, 1000L)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            initializeMap()
        } else {
            val permanentlyDenied = permissions.any { (perm, _) ->
                !shouldShowRequestPermissionRationale(perm)
            }
            if (permanentlyDenied) {
                showPermissionSettingsDialog()
            } else {
                Toast.makeText(this, "Location needed for tracking features", Toast.LENGTH_LONG).show()
                initializeMap()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val savedMode = prefs.getInt("night_mode", -1)
        if (savedMode != -1) {
            AppCompatDelegate.setDefaultNightMode(savedMode)
        }
        super.onCreate(savedInstanceState)

        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidTileCache = cacheDir.resolve("osmdroid")
        }

        setContentView(R.layout.activity_main)

        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)

        mapView = findViewById(R.id.mapView)
        searchInput = findViewById(R.id.searchInput)
        trackingButton = findViewById(R.id.trackingButton)
        removePinButton = findViewById(R.id.removePinButton)
        locateMeFab = findViewById(R.id.locateMeFab)
        settingsFab = findViewById(R.id.settingsGearButton)
        satToggleButton = findViewById(R.id.satToggleButton)
        distanceText = findViewById(R.id.distanceText)
        currentLocationText = findViewById(R.id.currentLocationText)
        destinationLabel = findViewById(R.id.destinationLabel)
        savePlaceButton = findViewById(R.id.savePlaceButton)
        shareButton = findViewById(R.id.shareButton)
        buttonRow = findViewById(R.id.buttonRow)
        weatherText = findViewById(R.id.weatherText)
        speedMeter = findViewById(R.id.speedMeter)
        speedValue = findViewById(R.id.speedValue)
        voiceToggleButton = findViewById(R.id.voiceToggleButton)
        themeToggleButton = findViewById(R.id.themeToggleButton)
        searchSuggestions = findViewById(R.id.searchSuggestions)
        helpButton = findViewById(R.id.helpButton)

        searchAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1)
        searchSuggestions.adapter = searchAdapter
        searchSuggestions.setOnItemClickListener { _, _, position, _ ->
            val (name, lat, lon) = searchResults[position]
            searchSuggestions.visibility = View.GONE
            searchInput.setText("")
            searchInput.clearFocus()
            hideKeyboard()
            placePin(GeoPoint(lat, lon))
            mapView.controller.animateTo(GeoPoint(lat, lon), 15.0, 1000L)
        }

        voiceEnabled = prefs.getBoolean("voice_alerts_enabled", true)
        updateVoiceButtonIcon()
        updateThemeButtonIcon()

        initializeMap()
        setupSearch()
        setupTrackingButton()
        setupRemovePinButton()
        setupSavePlaceButton()
        setupShareButton()
        setupLocateMeFab()
        setupSettingsFab()
        setupSatToggleButton()
        setupHelpButton()
        registerDistanceReceiver()
        registerArrivedReceiver()

        if (!prefs.getBoolean("tutorial_done", false)) {
            val intent = Intent(this, TutorialActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        isForeground = true
        mapView.onResume()
        updateThemeButtonIcon()
        if (!permissionDialogShownThisSession) {
            permissionDialogShownThisSession = true
            showPermissionChoiceDialog()
        }
    }

    private fun showPermissionChoiceDialog() {
        val fineLocGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val bgLocGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else true
        val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true

        if (fineLocGranted && bgLocGranted && notifGranted) {
            initializeMap()
            return
        }

        val finePermanently = !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
        val bgPermanently = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else false
        val notifPermanently = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            !shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
        } else false

        if (finePermanently || bgPermanently || notifPermanently) {
            showPermissionSettingsDialog()
            return
        }

        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("📍 Location Needed")
        builder.setMessage(
            "VibeNav needs your location to:\n\n" +
            "• Track your position on the map\n" +
            "• Alert you when you arrive\n" +
            "• Show your current street/area name\n\n" +
            "Tap 'Turn On' to allow location access."
        )
        builder.setPositiveButton("Turn On") { _, _ ->
            val needed = mutableListOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                needed.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                needed.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            requestPermissionLauncher.launch(needed.toTypedArray())
        }
        builder.setNegativeButton("Not Now") { _, _ ->
            initializeMap()
            Toast.makeText(this, "Location off — you can still search and set a destination", Toast.LENGTH_LONG).show()
        }
        builder.setCancelable(false)
        builder.show()
    }

    private fun showPermissionSettingsDialog() {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("🔒 Permission Blocked")
        builder.setMessage(
            "Location permission is blocked.\n\n" +
            "Tap 'Open Settings' to enable:\n" +
            "App Info → Permissions → Location → Allow all the time"
        )
        builder.setPositiveButton("Open Settings") { _, _ ->
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.parse("package:$packageName")
            }
            startActivity(intent)
        }
        builder.setNegativeButton("Not Now") { _, _ ->
            initializeMap()
            Toast.makeText(this, "You can enable later in Settings", Toast.LENGTH_LONG).show()
        }
        builder.setCancelable(false)
        builder.show()
    }

    private fun initializeMap() {
        if (isMapInitialized) return
        isMapInitialized = true
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.setTilesScaledToDpi(true)
        mapView.setBuiltInZoomControls(false)
        mapView.setClickable(true)

        val compassOverlay = CompassOverlay(this, mapView)
        compassOverlay.enableCompass()
        mapView.overlays.add(compassOverlay)

        val rotationOverlay = RotationGestureOverlay(mapView)
        rotationOverlay.isEnabled = true
        mapView.overlays.add(rotationOverlay)

        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onLongPress(e: MotionEvent) {
                if (isTracking) return
                val igp = mapView.projection.fromPixels(e.x.toInt(), e.y.toInt())
                placePin(GeoPoint(igp.latitude, igp.longitude))
            }
        })
        mapView.setOnTouchListener { _, event -> gestureDetector?.onTouchEvent(event) ?: false }

        myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), mapView).apply {
            enableMyLocation()
            mapView.overlays.add(this)
        }

        mapView.controller.setZoom(12.0)
        mapView.controller.setCenter(GeoPoint(6.9271, 79.8612))

        fetchWeather(6.9271, 79.8612)
    }

    private fun fetchWeather(lat: Double, lon: Double) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // codeql[java/android/missing-certificate-pinning] — public Open-Meteo API, no fixed cert to pin
                val url = URL("https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true&timezone=auto")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                val json = JSONObject(conn.inputStream.bufferedReader().readText())
                conn.disconnect()

                val cw = json.getJSONObject("current_weather")
                val temp = cw.getDouble("temperature")
                val code = cw.getInt("weathercode")

                val icon = when (code) {
                    0 -> "☀️"
                    1 -> "🌤"
                    2 -> "⛅"
                    3 -> "☁️"
                    in 45..48 -> "🌫"
                    in 51..55 -> "🌦"
                    in 61..65 -> "🌧"
                    in 71..77 -> "❄️"
                    in 80..82 -> "🌦"
                    in 95..99 -> "⛈"
                    else -> "🌡"
                }

                val display = "$icon ${temp.toInt()}°C"
                withContext(Dispatchers.Main) {
                    weatherText.text = display
                    weatherText.visibility = android.view.View.VISIBLE
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    weatherText.visibility = android.view.View.GONE
                }
            }
        }
    }

    private fun placePin(geoPoint: GeoPoint) {
        clearPin()
        selectedAddress = null

        destinationMarker = Marker(mapView).apply {
            position = geoPoint
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Destination"
            mapView.overlays.add(this)
        }

        radiusCircle = Polygon(mapView).apply {
            points = buildCirclePoints(geoPoint, 300.0)
            fillColor = Color.argb(40, 0, 255, 0)
            strokeColor = Color.argb(120, 0, 255, 0)
            strokeWidth = 3f
            mapView.overlays.add(this)
        }

        findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.destinationRow).visibility = android.view.View.VISIBLE
        destinationLabel.visibility = android.view.View.VISIBLE
        destinationLabel.text = "Loading address..."
        trackingButton.isEnabled = true

        mapView.invalidate()

        if (currentUserLat != 0.0 || currentUserLon != 0.0) {
            fetchRoute(currentUserLat, currentUserLon, geoPoint.latitude, geoPoint.longitude)
        } else {
            try {
                val lastLoc = myLocationOverlay?.lastFix
                if (lastLoc != null) {
                    fetchRoute(lastLoc.latitude, lastLoc.longitude, geoPoint.latitude, geoPoint.longitude)
                }
            } catch (_: Exception) {}
        }

        CoroutineScope(Dispatchers.IO).launch {
            val info = geocoderHelper.reverseGeocode(geoPoint.latitude, geoPoint.longitude)
            launch(Dispatchers.Main) {
                selectedAddress = Address(null as java.util.Locale?).also {
                    it.latitude = geoPoint.latitude
                    it.longitude = geoPoint.longitude
                    it.setAddressLine(0, info.fullDisplayName ?: info.formatCurrentLocation())
                }
                destinationLabel.text = (info.fullDisplayName ?: info.formatCurrentLocation()) + roadInfoSuffix()
            }
        }
    }

    private fun roadInfoSuffix(): String {
        return if (roadDistanceKm > 0) {
            val min = roadDurationSec / 60
            val sec = roadDurationSec % 60
            val timeStr = if (min > 0) "${min}m ${sec}s" else "${sec}s"
            "\n🚗 ${String.format("%.1f", roadDistanceKm)} km · $timeStr"
        } else ""
    }

    private fun buildCirclePoints(center: GeoPoint, radiusMeters: Double): ArrayList<GeoPoint> {
        val points = ArrayList<GeoPoint>()
        val R = 6371000.0
        val lat = Math.toRadians(center.latitude)
        val lon = Math.toRadians(center.longitude)
        for (i in 0..360 step 5) {
            val brng = Math.toRadians(i.toDouble())
            val dLat = asin(sin(lat) * cos(radiusMeters / R) + cos(lat) * sin(radiusMeters / R) * cos(brng))
            val dLon = lon + atan2(sin(brng) * sin(radiusMeters / R) * cos(lat), cos(radiusMeters / R) - sin(lat) * sin(dLat))
            points.add(GeoPoint(Math.toDegrees(dLat), Math.toDegrees(dLon)))
        }
        return points
    }

    private fun drawUserCircle(lat: Double, lon: Double) {
        userRadiusCircle?.let { mapView.overlays.remove(it) }
        userRadiusCircle = Polygon(mapView).apply {
            points = buildCirclePoints(GeoPoint(lat, lon), 20.0)
            fillColor = Color.argb(80, 41, 121, 255)
            strokeColor = Color.argb(150, 41, 121, 255)
            strokeWidth = 2f
            mapView.overlays.add(this)
        }
        drawDirectionArrow(lat, lon)
        mapView.invalidate()
    }

    private fun drawDirectionArrow(userLat: Double, userLon: Double) {
        val dest = selectedAddress ?: return
        val bearing = proximityMonitor.apply {
            setDestination(dest.latitude, dest.longitude)
        }.calculateBearing(userLat, userLon)

        directionArrow?.let { mapView.overlays.remove(it) }

        directionArrow = Marker(mapView).apply {
            position = GeoPoint(userLat, userLon)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            icon = createArrowIcon()
            rotation = -bearing
            mapView.overlays.add(this)
        }
    }

    private fun createArrowIcon(): android.graphics.drawable.Drawable {
        val d = GradientDrawable()
        d.setSize(48, 48)
        d.shape = GradientDrawable.OVAL
        d.setColor(Color.argb(180, 41, 121, 255))
        d.setStroke(2, Color.argb(255, 41, 121, 255))

        val bitmap = android.graphics.Bitmap.createBitmap(48, 48, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        d.setBounds(0, 0, 48, 48)
        d.draw(canvas)

        val paint = android.graphics.Paint().apply {
            color = Color.WHITE
            textSize = 28f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
        val x = 24f
        val y = 36f
        canvas.drawText("▲", x, y, paint)

        return android.graphics.drawable.BitmapDrawable(resources, bitmap)
    }

    private fun setupSearch() {
        searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard()
                searchSuggestions.visibility = View.GONE
                val query = searchInput.text.toString().trim()
                if (query.isNotEmpty()) {
                    geocodeSearch(query)
                }
                true
            } else false
        }

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchHandler.removeCallbacksAndMessages(null)
                val query = s?.toString()?.trim() ?: ""
                if (query.length >= 3) {
                    searchHandler.postDelayed({
                        fetchAutocompleteSuggestions(query)
                    }, 500)
                } else {
                    searchSuggestions.visibility = View.GONE
                }
            }
        })

        searchInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                searchSuggestions.postDelayed({ searchSuggestions.visibility = View.GONE }, 200)
            }
        }
    }

    private fun geocodeSearch(query: String) {
        Toast.makeText(this, "Searching...", Toast.LENGTH_SHORT).show()
        CoroutineScope(Dispatchers.IO).launch {
            val results = geocoderHelper.search(query)
            launch(Dispatchers.Main) {
                if (results.isNotEmpty()) {
                    val addr = results[0]
                    placePin(GeoPoint(addr.latitude, addr.longitude))
                    mapView.controller.animateTo(GeoPoint(addr.latitude, addr.longitude), 15.0, 1000L)
                } else {
                    Toast.makeText(this@MainActivity, "Location not found", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun fetchAutocompleteSuggestions(query: String) {
        searchJob?.cancel()
        searchJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("https://nominatim.openstreetmap.org/search?q=${URLEncoder.encode(query, "UTF-8")}&format=json&limit=5&addressdetails=1")
                val conn = url.openConnection() as HttpURLConnection
                conn.setRequestProperty("User-Agent", packageName)
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                val json = conn.inputStream.bufferedReader().readText()
                conn.disconnect()
                val arr = JSONArray(json)
                val items = mutableListOf<Triple<String, Double, Double>>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val name = obj.getString("display_name")
                    val lat = obj.getDouble("lat")
                    val lon = obj.getDouble("lon")
                    items.add(Triple(name, lat, lon))
                }
                launch(Dispatchers.Main) {
                    searchResults.clear()
                    val names = mutableListOf<String>()
                    items.forEach { (name, lat, lon) ->
                        searchResults.add(Triple(name, lat, lon))
                        names.add(name)
                    }
                    if (names.isNotEmpty()) {
                        searchAdapter.clear()
                        searchAdapter.addAll(names)
                        searchAdapter.notifyDataSetChanged()
                        searchSuggestions.visibility = View.VISIBLE
                    } else {
                        searchSuggestions.visibility = View.GONE
                    }
                }
            } catch (_: Exception) {
                launch(Dispatchers.Main) { searchSuggestions.visibility = View.GONE }
            }
        }
    }

    private fun fetchRoute(fromLat: Double, fromLon: Double, toLat: Double, toLon: Double) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("https://router.project-osrm.org/route/v1/driving/$fromLon,$fromLat;$toLon,$toLat?overview=full&geometries=geojson&steps=false")
                val conn = url.openConnection() as HttpURLConnection
                conn.setRequestProperty("User-Agent", packageName)
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                val json = conn.inputStream.bufferedReader().readText()
                conn.disconnect()
                val obj = JSONObject(json)
                if (obj.getString("code") != "Ok") {
                    launch(Dispatchers.Main) { return@launch }
                }
                val route = obj.getJSONArray("routes").getJSONObject(0)
                val distMeters = route.getDouble("distance")
                val durSeconds = route.getDouble("duration")
                val geometry = route.getJSONObject("geometry")
                val coords = geometry.getJSONArray("coordinates")
                val points = mutableListOf<GeoPoint>()
                for (i in 0 until coords.length()) {
                    val c = coords.getJSONArray(i)
                    points.add(GeoPoint(c.getDouble(1), c.getDouble(0)))
                }
                launch(Dispatchers.Main) {
                    roadDistanceKm = distMeters / 1000.0
                    roadDurationSec = durSeconds.toInt()
                    drawRoute(points)
                    updateRoadInfo()
                }
            } catch (_: Exception) {
                launch(Dispatchers.Main) { Toast.makeText(this@MainActivity, "⚠ Route unavailable", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    private fun drawRoute(points: List<GeoPoint>) {
        routePolyline?.let { mapView.overlays.remove(it) }
        if (points.size < 2) return
        routePolyline = Polyline(mapView).apply {
            setPoints(java.util.ArrayList(points))
            outlinePaint.color = Color.argb(200, 41, 121, 255)
            outlinePaint.strokeWidth = 6f
            outlinePaint.isAntiAlias = true
            mapView.overlays.add(this)
        }
        mapView.invalidate()
        Toast.makeText(this, "🛣 Route loaded", Toast.LENGTH_SHORT).show()
    }

    private fun updateRoadInfo() {
        val currentText = destinationLabel.text.toString()
        val baseText = currentText.split("\n")[0]
        destinationLabel.text = baseText + roadInfoSuffix()
    }

    private fun removeRoute() {
        routePolyline?.let { mapView.overlays.remove(it) }
        routePolyline = null
        roadDistanceKm = 0.0
        roadDurationSec = 0
        mapView.invalidate()
    }

    private fun setupTrackingButton() {
        trackingButton.setOnClickListener {
            if (isTracking) {
                stopTracking()
            } else {
                startTracking()
            }
        }
    }

    private fun setupRemovePinButton() {
        removePinButton.setOnClickListener { clearPin() }
    }

    private fun setupSavePlaceButton() {
        savePlaceButton.setOnClickListener {
            val address = selectedAddress ?: return@setOnClickListener
            val name = destinationLabel.text.toString()
            SavedPlacesManager.savePlace(this, name, address.latitude, address.longitude)
            Toast.makeText(this, "Place saved!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupShareButton() {
        shareButton.setOnClickListener {
            val address = selectedAddress ?: return@setOnClickListener
            val name = destinationLabel.text.toString()
            val uri = "https://maps.google.com/maps?q=${address.latitude},${address.longitude}"
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "$name\n$uri")
                putExtra(Intent.EXTRA_SUBJECT, "My destination on VibeNav")
            }
            startActivity(Intent.createChooser(sendIntent, "Share destination"))
        }
    }

    private fun setupLocateMeFab() {
        locateMeFab.setOnClickListener {
            centerOnMyLocation()
        }
    }

    private fun setupHelpButton() {
        helpButton.setOnClickListener {
            val intent = Intent(this, HelpActivity::class.java)
            startActivity(intent)
        }

        voiceToggleButton.setOnClickListener {
            voiceEnabled = !voiceEnabled
            prefs.edit().putBoolean("voice_alerts_enabled", voiceEnabled).apply()
            updateVoiceButtonIcon()
        }

        themeToggleButton.setOnClickListener {
            val currentMode = AppCompatDelegate.getDefaultNightMode()
            val newMode = when (currentMode) {
                AppCompatDelegate.MODE_NIGHT_YES -> AppCompatDelegate.MODE_NIGHT_NO
                else -> AppCompatDelegate.MODE_NIGHT_YES
            }
            AppCompatDelegate.setDefaultNightMode(newMode)
            prefs.edit().putInt("night_mode", newMode).apply()
            recreate()
        }
    }

    private fun updateVoiceButtonIcon() {
        voiceToggleButton.text = if (voiceEnabled) "🔊" else "🔇"
    }

    private fun updateThemeButtonIcon() {
        val isDark = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
        themeToggleButton.text = if (isDark) "☀️" else "🌙"
    }

    private fun setupSettingsFab() {
        settingsFab.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
        settingsFab.setOnLongClickListener {
            val intent = Intent(this, TutorialActivity::class.java)
            startActivity(intent)
            true
        }
    }

    private var isSatelliteView = false

    private fun setupSatToggleButton() {
        val googleSatSource = object : XYTileSource(
            "GoogleSat", 0, 22, 256, "",
            arrayOf(
                "https://mt0.google.com/vt/lyrs=y",
                "https://mt1.google.com/vt/lyrs=y",
                "https://mt2.google.com/vt/lyrs=y",
                "https://mt3.google.com/vt/lyrs=y"
            )
        ) {
            override fun getTileURLString(mapTileIndex: Long): String {
                val url = baseUrl ?: return ""
                return url +
                       "&x=${MapTileIndex.getX(mapTileIndex)}" +
                       "&y=${MapTileIndex.getY(mapTileIndex)}" +
                       "&z=${MapTileIndex.getZoom(mapTileIndex)}"
            }
        }
        satToggleButton.setOnClickListener {
            isSatelliteView = !isSatelliteView
            if (isSatelliteView) {
                mapView.setTileSource(googleSatSource)
                satToggleButton.text = "🗺"
            } else {
                mapView.setTileSource(TileSourceFactory.MAPNIK)
                satToggleButton.text = "🛰"
            }
            mapView.invalidate()
        }
    }

    private fun centerOnMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) return
        try {
            val fusedClient = com.google.android.gms.location.LocationServices
                .getFusedLocationProviderClient(this)
            fusedClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val point = GeoPoint(location.latitude, location.longitude)
                    mapView.controller.animateTo(point, 17.0, 800L)
                    myLocationOverlay?.enableFollowLocation()
                } else {
                    myLocationOverlay?.enableFollowLocation()
                    Toast.makeText(this, "Waiting for GPS fix...", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (_: SecurityException) { }
    }

    private fun clearPin() {
        if (isTracking) return
        destinationMarker?.let { mapView.overlays.remove(it) }
        destinationMarker = null
        directionArrow?.let { mapView.overlays.remove(it) }
        directionArrow = null
        radiusCircle?.let { mapView.overlays.remove(it) }
        radiusCircle = null
        removeRoute()
        selectedAddress = null
        proximityMonitor.reset()
        destinationLabel.visibility = android.view.View.GONE
        findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.destinationRow).visibility = android.view.View.GONE
        trackingButton.isEnabled = false
        shareButton.visibility = android.view.View.GONE
        buttonRow.visibility = android.view.View.GONE
        mapView.invalidate()
    }

    private fun startTracking() {
        val address = selectedAddress ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            )
        }

        val alertRadius = prefs.getInt("alert_radius", 300)
        val thresholdMeters = alertRadius.toDouble()

        isTracking = true
        trackingStartTime = System.currentTimeMillis()
        maxTrackingDistance = 0
        trackingButton.text = "Stop Tracking"
        trackingButton.setBackgroundColor(
            ContextCompat.getColor(this, android.R.color.holo_red_dark)
        )
        distanceText.visibility = android.view.View.VISIBLE
        currentLocationText.visibility = android.view.View.VISIBLE
        distanceText.text = "Starting tracking..."
        savePlaceButton.visibility = android.view.View.GONE
        shareButton.visibility = android.view.View.GONE
        buttonRow.visibility = android.view.View.GONE
        speedMeter.visibility = android.view.View.VISIBLE

        proximityMonitor.setDestination(address.latitude, address.longitude)
        proximityMonitor.setThreshold(thresholdMeters)

        radiusCircle?.let { mapView.overlays.remove(it) }
        radiusCircle = Polygon(mapView).apply {
            points = buildCirclePoints(GeoPoint(address.latitude, address.longitude), thresholdMeters)
            fillColor = Color.argb(40, 0, 255, 0)
            strokeColor = Color.argb(120, 0, 255, 0)
            strokeWidth = 3f
            mapView.overlays.add(this)
        }

        if (currentUserLat != 0.0 || currentUserLon != 0.0) {
            fetchRoute(currentUserLat, currentUserLon, address.latitude, address.longitude)
        }

        val intent = Intent(this, TrackingService::class.java).apply {
            action = TrackingService.ACTION_START
            putExtra(TrackingService.EXTRA_DEST_LAT, address.latitude)
            putExtra(TrackingService.EXTRA_DEST_LON, address.longitude)
            putExtra(TrackingService.EXTRA_THRESHOLD, thresholdMeters)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopTracking() {
        saveTrip()
        isTracking = false
        trackingButton.text = "Start Tracking"
        trackingButton.setBackgroundColor(
            ContextCompat.getColor(this, android.R.color.holo_blue_dark)
        )
        distanceText.visibility = android.view.View.GONE
        currentLocationText.visibility = android.view.View.GONE
        savePlaceButton.visibility = android.view.View.VISIBLE
        shareButton.visibility = android.view.View.VISIBLE
        buttonRow.visibility = android.view.View.VISIBLE
        speedMeter.visibility = android.view.View.GONE

        directionArrow?.let { mapView.overlays.remove(it) }
        directionArrow = null
        removeRoute()
        mapView.invalidate()

        val intent = Intent(this, TrackingService::class.java).apply {
            action = TrackingService.ACTION_STOP
        }
        startService(intent)
    }

    private fun updateCurrentLocation(lat: Double, lon: Double) {
        val now = System.currentTimeMillis()
        val dist = sqrt((lat - lastReverseGeocodeLat).pow(2) + (lon - lastReverseGeocodeLon).pow(2)) * 111319.9
        if (dist < 200 && now - lastReverseGeocodeTime < 30000) return

        lastReverseGeocodeLat = lat
        lastReverseGeocodeLon = lon
        lastReverseGeocodeTime = now

        CoroutineScope(Dispatchers.IO).launch {
            val info = geocoderHelper.reverseGeocode(lat, lon)
            launch(Dispatchers.Main) {
                currentLocationText.text = "📍 ${info.formatCurrentLocation()}"
            }
        }
    }

    private fun saveTrip() {
        val address = selectedAddress ?: return
        if (trackingStartTime == 0L) return
        val name = destinationLabel.text.toString()
        val trip = Trip(
            id = 0,
            placeName = name,
            destLat = address.latitude,
            destLon = address.longitude,
            startTime = trackingStartTime,
            endTime = System.currentTimeMillis(),
            maxDistance = maxTrackingDistance
        )
        TripHistoryManager.addTrip(this, trip)
    }

    private fun formatDistance(meters: Int): String {
        return if (meters >= 1000) {
            String.format("%.2f km", meters / 1000.0)
        } else {
            "$meters m"
        }
    }

    private fun registerDistanceReceiver() {
        val filter = IntentFilter(TrackingService.BROADCAST_DISTANCE)
        distanceUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val meters = intent.getIntExtra(TrackingService.EXTRA_DISTANCE, 0)
                val inRange = intent.getBooleanExtra(TrackingService.EXTRA_PROXIMITY, false)
                val speedKmh = intent.getFloatExtra(TrackingService.EXTRA_SPEED, 0f)
                val etaSeconds = intent.getIntExtra(TrackingService.EXTRA_ETA, -1)

                if (speedKmh > 1f) {
                    speedValue.text = String.format("%.0f", speedKmh)
                    speedMeter.visibility = android.view.View.VISIBLE
                } else {
                    speedMeter.visibility = android.view.View.INVISIBLE
                }

                val speedText = if (speedKmh > 1f) String.format("%.0f km/h", speedKmh) else ""
                val etaText = if (etaSeconds > 0) {
                    val min = etaSeconds / 60
                    val sec = etaSeconds % 60
                    if (min > 0) "${min}m ${sec}s" else "${sec}s"
                } else ""

                val text = when {
                    inRange -> "Arrived! (${formatDistance(meters)})"
                    etaText.isNotEmpty() -> "${formatDistance(meters)}  •  $etaText  •  $speedText"
                    speedText.isNotEmpty() -> "${formatDistance(meters)}  •  $speedText"
                    else -> "${formatDistance(meters)} away"
                }
                distanceText.text = text

                if (inRange) {
                    distanceText.setTextColor(
                        ContextCompat.getColor(this@MainActivity, android.R.color.holo_green_light)
                    )
                } else {
                    distanceText.setTextColor(
                        ContextCompat.getColor(this@MainActivity, android.R.color.white)
                    )
                }

                if (meters > maxTrackingDistance) maxTrackingDistance = meters

                val userLat = intent.getDoubleExtra(TrackingService.EXTRA_USER_LAT, 0.0)
                val userLon = intent.getDoubleExtra(TrackingService.EXTRA_USER_LON, 0.0)
                if (userLat != 0.0 || userLon != 0.0) {
                    currentUserLat = userLat
                    currentUserLon = userLon
                    drawUserCircle(userLat, userLon)
                    updateCurrentLocation(userLat, userLon)
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(distanceUpdateReceiver, filter, 0)
        } else {
            registerReceiver(distanceUpdateReceiver, filter)
        }
    }

    private fun registerArrivedReceiver() {
        val filter = IntentFilter(TrackingService.ACTION_ARRIVED)
        arrivedReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val autoStopped = intent.getBooleanExtra(TrackingService.EXTRA_AUTO_STOPPED, false)
                if (autoStopped) {
                    saveTrip()
                    isTracking = false
                    trackingButton.text = "Start Tracking"
                    trackingButton.setBackgroundColor(
                        ContextCompat.getColor(this@MainActivity, android.R.color.holo_blue_dark)
                    )
                    distanceText.visibility = android.view.View.GONE
                    currentLocationText.visibility = android.view.View.GONE
                    savePlaceButton.visibility = android.view.View.VISIBLE
                    shareButton.visibility = android.view.View.VISIBLE
                    buttonRow.visibility = android.view.View.VISIBLE
                    speedMeter.visibility = android.view.View.GONE
                    directionArrow?.let { mapView.overlays.remove(it) }
                    directionArrow = null
                    mapView.invalidate()
                    Toast.makeText(this@MainActivity, "Arrived! Tracking stopped automatically.", Toast.LENGTH_LONG).show()
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(arrivedReceiver, filter, 0)
        } else {
            registerReceiver(arrivedReceiver, filter)
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(searchInput.windowToken, 0)
    }

    override fun onPause() {
        super.onPause()
        isForeground = false
        mapView.onPause()
    }

    override fun onDestroy() {
        distanceUpdateReceiver?.let { unregisterReceiver(it) }
        arrivedReceiver?.let { unregisterReceiver(it) }
        mapView.onDetach()
        super.onDestroy()
    }
}
