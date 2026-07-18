package com.vibenav

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class TripHistoryActivity : AppCompatActivity() {

    private lateinit var tripList: RecyclerView
    private lateinit var emptyText: TextView
    private var trips = mutableListOf<Trip>()
    private var adapter: TripAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trip_history)

        tripList = findViewById(R.id.tripList)
        emptyText = findViewById(R.id.emptyText)

        tripList.layoutManager = LinearLayoutManager(this)

        loadTrips()
    }

    override fun onResume() {
        super.onResume()
        loadTrips()
    }

    private fun loadTrips() {
        trips = TripHistoryManager.getTrips(this).toMutableList()
        if (trips.isEmpty()) {
            emptyText.visibility = View.VISIBLE
            tripList.visibility = View.GONE
        } else {
            emptyText.visibility = View.GONE
            tripList.visibility = View.VISIBLE
            adapter = TripAdapter(trips) { trip ->
                val intent = Intent().apply {
                    putExtra("lat", trip.destLat)
                    putExtra("lon", trip.destLon)
                }
                setResult(RESULT_OK, intent)
                finish()
            }
            tripList.adapter = adapter
        }
    }

    class TripAdapter(
        private val trips: List<Trip>,
        private val onClick: (Trip) -> Unit
    ) : RecyclerView.Adapter<TripAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_trip, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val trip = trips[position]
            holder.nameText.text = trip.placeName
            holder.dateText.text = TripHistoryManager.formatDate(trip.startTime)
            holder.durationText.text = "Duration: ${TripHistoryManager.formatDuration(trip.endTime - trip.startTime)}"
            holder.distanceText.text = "Max distance: ${trip.maxDistance}m"
            holder.itemView.setOnClickListener { onClick(trip) }
            holder.itemView.setOnLongClickListener {
                TripHistoryManager.deleteTrip(holder.itemView.context, trip.id)
                Toast.makeText(holder.itemView.context, "Trip deleted", Toast.LENGTH_SHORT).show()
                val pos = (trips as MutableList).indexOfFirst { it.id == trip.id }
                if (pos >= 0) {
                    (trips as MutableList).removeAt(pos)
                    notifyItemRemoved(pos)
                }
                true
            }
        }

        override fun getItemCount() = trips.size

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val nameText: TextView = view.findViewById(R.id.tripName)
            val dateText: TextView = view.findViewById(R.id.tripDate)
            val durationText: TextView = view.findViewById(R.id.tripDuration)
            val distanceText: TextView = view.findViewById(R.id.tripDistance)
        }
    }
}
