package com.vibenav

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class TutorialActivity : AppCompatActivity() {

    data class Page(val icon: String, val title: String, val desc: String)

    private val pages = listOf(
        Page("👋", "Welcome to VibeNav",
             "Your smart travel companion.\n\nSet a destination and start walking or driving.\nYour phone will alert you when you get close."),
        Page("🔍", "Search for Places",
             "Type a place name in the search bar.\n\nTry \"Colombo Fort\" or \"Galle Face\".\nSuggestions appear as you type — just tap one to set the pin.\nThe map will show you the location."),
        Page("👆", "Set a Pin",
             "Press and hold anywhere on the map.\nA red pin will appear at that spot.\nA green circle shows the alert area around it."),
        Page("🛰", "Switch Map View",
             "Tap the 🛰 button on the map to switch between street view and satellite view.\n\nSatellite shows real photos from above."),
        Page("🌤", "Live Weather",
             "See current temperature and weather in the top bar.\nIt updates when you open the app."),
        Page("📍", "Find Your Location",
             "Tap the blue circle at the bottom right.\nThe map will center on your current location."),
        Page("⭐", "Save and Share",
             "After setting a pin:\n• ⭐ Save — keep places for later\n• 📤 Share — send location to family or friends"),
        Page("✕", "Clear the Pin",
             "Tap the red X to remove the pin and green alert zone from the map."),
        Page("🚀", "Start Tracking",
             "Tap the blue button below to begin.\nDuring tracking you will see:\n• Distance to destination\n• Your speed\n• Arrival time\n• Your current street name\n• A blue arrow pointing the way\n• 🚗 A blue route line on the map\n  showing the road path to your destination"),
        Page("📳", "Arrival Alert",
             "When you enter the green zone:\n• Phone vibrates\n• Loud beep sounds\n• Voice says \"You have arrived\"\n• Full screen alert appears\n\nTap Silence to stop noise.\nTap Stop to end tracking.\n\nTracking stops by itself after 60 seconds."),
        Page("⚙", "Settings",
             "Tap the ⚙ icon to change settings:\n• Alert distance (50m to 1000m)\n• Saved places\n• Trip history\n• Battery settings\n• Map cache"),
        Page("🛡️", "Your Safety",
             "Your current street name appears at the bottom during tracking.\n\nShare your destination with family so they know your route."),
        Page("🚗", "Route Line",
             "A blue line appears on the map showing the actual road path from your location to the destination.\n\nThe road distance and travel time are shown below the place name."),
        Page("🔊", "Voice & Theme",
             "🔊 Voice toggle button (top-right of the map) — mute or unmute voice alerts.\n\n🌙 Theme toggle button (top bar, between ? and ⚙) — switch between light and dark mode.")
    )

    private var currentPage = 0
    private var fromSettings = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tutorial)

        fromSettings = intent.getBooleanExtra("fromSettings", false)

        findViewById<TextView>(R.id.skipText).setOnClickListener { finishTutorial() }
        findViewById<MaterialButton>(R.id.prevButton).setOnClickListener { goToPage(currentPage - 1) }
        findViewById<MaterialButton>(R.id.nextButton).setOnClickListener {
            if (currentPage < pages.size - 1) goToPage(currentPage + 1)
            else finishTutorial()
        }

        goToPage(0)
    }

    private fun goToPage(index: Int) {
        currentPage = index.coerceIn(0, pages.size - 1)
        val page = pages[currentPage]

        findViewById<TextView>(R.id.tutorialIcon).text = page.icon
        findViewById<TextView>(R.id.tutorialTitle).text = page.title
        findViewById<TextView>(R.id.tutorialDesc).text = page.desc

        findViewById<MaterialButton>(R.id.prevButton).visibility =
            if (currentPage == 0) View.INVISIBLE else View.VISIBLE

        val nextBtn = findViewById<MaterialButton>(R.id.nextButton)
        nextBtn.text = if (currentPage < pages.size - 1) "Next" else "Done"

        findViewById<TextView>(R.id.counterText).text = "${currentPage + 1} / ${pages.size}"

        updateDots()
    }

    private fun updateDots() {
        val container = findViewById<LinearLayout>(R.id.dotIndicator)
        container.removeAllViews()
        for (i in pages.indices) {
            val dot = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(12, 12).apply { setMargins(5, 0, 5, 0) }
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setSize(12, 12)
                    setColor(if (i == currentPage) -0x1 else 0x66666666.toInt())
                }
            }
            container.addView(dot)
        }
    }

    private fun finishTutorial() {
        getSharedPreferences("vibenav", Context.MODE_PRIVATE)
            .edit().putBoolean("tutorial_done", true).apply()
        if (fromSettings) {
            finish()
        } else {
            val intent = android.content.Intent(this, MainActivity::class.java)
            intent.flags = android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }
    }
}
