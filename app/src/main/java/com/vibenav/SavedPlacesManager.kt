package com.vibenav

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class SavedPlace(
    val name: String,
    val latitude: Double,
    val longitude: Double
)

object SavedPlacesManager {

    private const val PREFS_NAME = "vibenav"
    private const val KEY_PLACES = "saved_places"

    fun getPlaces(context: Context): List<SavedPlace> {
        val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PLACES, "[]") ?: "[]"
        val arr = JSONArray(json)
        return (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            SavedPlace(
                name = obj.getString("name"),
                latitude = obj.getDouble("lat"),
                longitude = obj.getDouble("lon")
            )
        }
    }

    fun savePlace(context: Context, name: String, lat: Double, lon: Double) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_PLACES, "[]") ?: "[]"
        val arr = JSONArray(json)

        val existing = (0 until arr.length()).find { i ->
            val obj = arr.getJSONObject(i)
            obj.getDouble("lat") == lat && obj.getDouble("lon") == lon
        }
        if (existing != null) return

        val obj = JSONObject().apply {
            put("name", name)
            put("lat", lat)
            put("lon", lon)
        }
        arr.put(obj)
        prefs.edit().putString(KEY_PLACES, arr.toString()).apply()
    }

    fun removePlace(context: Context, index: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_PLACES, "[]") ?: "[]"
        val arr = JSONArray(json)
        if (index in 0 until arr.length()) {
            arr.remove(index)
            prefs.edit().putString(KEY_PLACES, arr.toString()).apply()
        }
    }
}
