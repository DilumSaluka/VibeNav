package com.vibenav

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class SavedPlacesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved_places)

        refreshList()
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun refreshList() {
        val places = SavedPlacesManager.getPlaces(this)
        val container = findViewById<android.widget.LinearLayout>(R.id.placesContainer)
        container.removeAllViews()

        if (places.isEmpty()) {
            val tv = TextView(this).apply {
                text = "No saved places yet"
                textSize = 14f
                setTextColor(0x88FFFFFF.toInt())
                textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            }
            container.addView(tv)
            return
        }

        places.forEachIndexed { index, place ->
            val row = layoutInflater.inflate(R.layout.item_saved_place, container, false)
            row.findViewById<TextView>(R.id.placeName).text = place.name
            row.findViewById<TextView>(R.id.placeCoords).text = String.format("%.4f, %.4f", place.latitude, place.longitude)

            row.setOnClickListener {
                val intent = Intent().apply {
                    putExtra("lat", place.latitude)
                    putExtra("lon", place.longitude)
                    putExtra("name", place.name)
                }
                setResult(RESULT_OK, intent)
                finish()
            }

            row.setOnLongClickListener {
                AlertDialog.Builder(this)
                    .setTitle("Remove place?")
                    .setMessage(place.name)
                    .setPositiveButton("Remove") { _, _ ->
                        SavedPlacesManager.removePlace(this, index)
                        refreshList()
                        Toast.makeText(this, "Removed", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
                true
            }

            container.addView(row)
        }
    }
}
