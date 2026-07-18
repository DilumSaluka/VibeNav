package com.vibenav

data class AddressInfo(
    val area: String? = null,
    val street: String? = null,
    val landmark: String? = null,
    val city: String? = null,
    val fullDisplayName: String? = null
) {
    fun formatCurrentLocation(): String {
        val parts = mutableListOf<String>()

        val areaPart = area ?: landmark
        if (areaPart != null) parts.add("You are now in $areaPart")
        if (street != null && street != areaPart) parts.add(street)
        if (landmark != null && landmark != area && landmark != street) parts.add("near $landmark")

        return if (parts.isNotEmpty()) parts.joinToString(", ") else fullDisplayName ?: "📍 Your location"
    }
}
