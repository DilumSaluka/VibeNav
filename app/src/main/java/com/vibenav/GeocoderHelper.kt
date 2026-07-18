package com.vibenav

import android.content.Context
import android.location.Address
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class GeocoderHelper(private val context: Context) {

    suspend fun search(query: String): List<Address> = withContext(Dispatchers.IO) {
        val geocoder = Geocoder(context, Locale.getDefault())
        val results = mutableListOf<Address>()

        try {
            val fromGeocoder = geocoder.getFromLocationName(query, 5)
            if (fromGeocoder != null && fromGeocoder.isNotEmpty()) {
                results.addAll(fromGeocoder)
            }
        } catch (_: Exception) { }

        if (results.isEmpty()) {
            try {
                val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
                val url = URL("https://nominatim.openstreetmap.org/search?q=$encodedQuery&format=json&limit=5")
                val conn = url.openConnection() as HttpURLConnection
                conn.setRequestProperty("User-Agent", "VibeNav/1.0 (Android)")
                conn.connectTimeout = 5000
                conn.readTimeout = 5000

                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                reader.close()
                conn.disconnect()

                val jsonArray = JSONArray(response)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val lat = obj.getString("lat").toDouble()
                    val lon = obj.getString("lon").toDouble()
                    val displayName = obj.optString("display_name", "")

                    val address = Address(Locale.getDefault())
                    address.latitude = lat
                    address.longitude = lon
                    address.featureName = displayName.split(",").firstOrNull()?.trim() ?: displayName
                    address.setAddressLine(0, displayName)
                    results.add(address)
                }
            } catch (_: Exception) { }
        }

        results
    }

    suspend fun reverseGeocode(lat: Double, lon: Double): AddressInfo = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val addr = addresses[0]
                val subLocality = addr.subLocality
                val thoroughfare = addr.thoroughfare
                val featureName = addr.featureName
                val locality = addr.locality
                return@withContext AddressInfo(
                    area = subLocality ?: locality,
                    street = thoroughfare,
                    landmark = if (featureName != subLocality && featureName != thoroughfare) featureName else null,
                    city = locality,
                    fullDisplayName = addr.getAddressLine(0)
                )
            }
        } catch (_: Exception) { }

        try {
            val url = URL("https://nominatim.openstreetmap.org/reverse?format=json&lat=$lat&lon=$lon")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "VibeNav/1.0 (Android)")
            conn.connectTimeout = 5000
            conn.readTimeout = 5000

            val reader = BufferedReader(InputStreamReader(conn.inputStream))
            val response = reader.readText()
            reader.close()
            conn.disconnect()

            val json = org.json.JSONObject(response)
            val addrObj = json.optJSONObject("address")
            val displayName = json.optString("display_name", "$lat, $lon")
            return@withContext AddressInfo(
                area = addrObj?.optString("suburb") ?: addrObj?.optString("neighbourhood") ?: addrObj?.optString("city_district"),
                street = addrObj?.optString("road"),
                landmark = addrObj?.optString("amenity") ?: addrObj?.optString("bus_stop") ?: addrObj?.optString("railway"),
                city = addrObj?.optString("city") ?: addrObj?.optString("town") ?: addrObj?.optString("village"),
                fullDisplayName = displayName
            )
        } catch (_: Exception) {
            AddressInfo(fullDisplayName = "$lat, $lon")
        }
    }
}
