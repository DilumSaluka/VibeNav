package com.vibenav

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Trip(
    val id: Long,
    val placeName: String,
    val destLat: Double,
    val destLon: Double,
    val startTime: Long,
    val endTime: Long,
    val maxDistance: Int
)

class TripHistoryManager {

    companion object {
        private const val PREFS_NAME = "vibenav_trips"
        private const val KEY_TRIPS = "trips"
        private var nextId = 1L

        fun getTrips(context: Context): List<Trip> {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = prefs.getString(KEY_TRIPS, "[]") ?: "[]"
            val arr = JSONArray(json)
            val list = mutableListOf<Trip>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(Trip(
                    id = obj.getLong("id"),
                    placeName = obj.getString("placeName"),
                    destLat = obj.getDouble("destLat"),
                    destLon = obj.getDouble("destLon"),
                    startTime = obj.getLong("startTime"),
                    endTime = obj.getLong("endTime"),
                    maxDistance = obj.getInt("maxDistance")
                ))
                if (obj.getLong("id") >= nextId) nextId = obj.getLong("id") + 1
            }
            return list.sortedByDescending { it.endTime }
        }

        fun addTrip(context: Context, trip: Trip) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = prefs.getString(KEY_TRIPS, "[]") ?: "[]"
            val arr = JSONArray(json)
            val obj = JSONObject().apply {
                put("id", nextId++)
                put("placeName", trip.placeName)
                put("destLat", trip.destLat)
                put("destLon", trip.destLon)
                put("startTime", trip.startTime)
                put("endTime", trip.endTime)
                put("maxDistance", trip.maxDistance)
            }
            arr.put(obj)
            prefs.edit().putString(KEY_TRIPS, arr.toString()).apply()
        }

        fun deleteTrip(context: Context, tripId: Long) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = prefs.getString(KEY_TRIPS, "[]") ?: "[]"
            val arr = JSONArray(json)
            val out = JSONArray()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                if (obj.getLong("id") != tripId) {
                    out.put(obj)
                }
            }
            prefs.edit().putString(KEY_TRIPS, out.toString()).apply()
        }

        fun formatDuration(millis: Long): String {
            val seconds = millis / 1000
            val hours = seconds / 3600
            val minutes = (seconds % 3600) / 60
            val secs = seconds % 60
            return when {
                hours > 0 -> String.format("%dh %02dm %02ds", hours, minutes, secs)
                minutes > 0 -> String.format("%dm %02ds", minutes, secs)
                else -> String.format("%ds", secs)
            }
        }

        fun formatDate(millis: Long): String {
            val sdf = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.US)
            return sdf.format(java.util.Date(millis))
        }
    }
}
