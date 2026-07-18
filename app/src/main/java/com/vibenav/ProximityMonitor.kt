package com.vibenav

import android.location.Location
import kotlin.math.*

class ProximityMonitor {

    private var destinationLat: Double = 0.0
    private var destinationLon: Double = 0.0
    private var thresholdMeters: Double = 300.0
    private var wasInRange = false

    fun setDestination(lat: Double, lon: Double) {
        destinationLat = lat
        destinationLon = lon
        wasInRange = false
    }

    fun setThreshold(meters: Double) {
        thresholdMeters = meters
    }

    fun getDestinationLat(): Double = destinationLat
    fun getDestinationLon(): Double = destinationLon
    fun getThreshold(): Double = thresholdMeters

    fun hasDestination(): Boolean = destinationLat != 0.0 || destinationLon != 0.0

    fun calculateDistance(currentLat: Double, currentLon: Double): Double {
        val R = 6371000.0
        val dLat = Math.toRadians(destinationLat - currentLat)
        val dLon = Math.toRadians(destinationLon - currentLon)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(currentLat)) *
                cos(Math.toRadians(destinationLat)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }

    fun calculateBearing(currentLat: Double, currentLon: Double): Float {
        val dLon = Math.toRadians(destinationLon - currentLon)
        val lat1 = Math.toRadians(currentLat)
        val lat2 = Math.toRadians(destinationLat)
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        val bearing = Math.toDegrees(atan2(y, x))
        return ((bearing + 360) % 360).toFloat()
    }

    fun checkProximity(currentLocation: Location): ProximityResult {
        val distance = calculateDistance(currentLocation.latitude, currentLocation.longitude)
        val isInRange = distance <= thresholdMeters
        val justEntered = isInRange && !wasInRange
        val bearing = calculateBearing(currentLocation.latitude, currentLocation.longitude)

        if (isInRange) wasInRange = true

        return ProximityResult(
            distanceMeters = distance,
            isInRange = isInRange,
            justEnteredRange = justEntered,
            bearing = bearing
        )
    }

    fun reset() {
        destinationLat = 0.0
        destinationLon = 0.0
        wasInRange = false
    }

    data class ProximityResult(
        val distanceMeters: Double,
        val isInRange: Boolean,
        val justEnteredRange: Boolean,
        val bearing: Float = 0f
    )
}
