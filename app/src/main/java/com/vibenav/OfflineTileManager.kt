package com.vibenav

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class OfflineTileManager(private val context: Context) {

    data class Tile(val z: Int, val x: Int, val y: Int)
    data class BoundingBox(val minLat: Double, val maxLat: Double, val minLon: Double, val maxLon: Double)

    suspend fun prefetchTiles(
        centerLat: Double, centerLon: Double, radiusKm: Double,
        minZoom: Int = 10, maxZoom: Int = 18,
        updateProgress: (downloaded: Int, total: Int) -> Unit
    ) = withContext(Dispatchers.IO) {
        val cacheDir = File(context.cacheDir, "osmdroid/tiles/Mapnik")
        cacheDir.mkdirs()

        val latDelta = (radiusKm / 111.32)
        val lonDelta = (radiusKm / (111.32 * Math.cos(Math.toRadians(centerLat))))
        val bbox = BoundingBox(centerLat - latDelta, centerLat + latDelta, centerLon - lonDelta, centerLon + lonDelta)

        val allTiles = mutableListOf<Tile>()
        for (z in minZoom..maxZoom) {
            val minTileX = lonToTileX(bbox.minLon, z).toInt()
            val maxTileX = lonToTileX(bbox.maxLon, z).toInt()
            val minTileY = latToTileY(bbox.maxLat, z).toInt()
            val maxTileY = latToTileY(bbox.minLat, z).toInt()
            for (x in minTileX..maxTileX) {
                for (y in minTileY..maxTileY) {
                    allTiles.add(Tile(z, x, y))
                }
            }
        }

        var downloaded = 0
        val total = allTiles.size

        for (tile in allTiles) {
            val tileFile = File(cacheDir, "${tile.z}/${tile.x}/${tile.y}.tile")
            if (tileFile.exists()) {
                downloaded++
                continue
            }
            try {
                val url = URL("https://tile.openstreetmap.org/${tile.z}/${tile.x}/${tile.y}.png")
                val conn = url.openConnection() as HttpURLConnection
                conn.setRequestProperty("User-Agent", context.packageName)
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                val bytes = conn.inputStream.readBytes()
                conn.disconnect()
                if (bytes.isNotEmpty()) {
                    tileFile.parentFile?.mkdirs()
                    tileFile.writeBytes(bytes)
                }
                downloaded++
            } catch (_: Exception) {
                downloaded++
            }
            if (downloaded % 10 == 0 || downloaded == total) {
                withContext(Dispatchers.Main) { updateProgress(downloaded, total) }
            }
        }
    }

    private fun lonToTileX(lon: Double, zoom: Int): Double {
        return (lon + 180.0) / 360.0 * (1 shl zoom)
    }

    private fun latToTileY(lat: Double, zoom: Int): Double {
        val latRad = Math.toRadians(lat)
        return (1.0 - Math.log(Math.tan(latRad) + 1.0 / Math.cos(latRad)) / Math.PI) / 2.0 * (1 shl zoom)
    }

    fun getCacheSize(): Long {
        val cacheDir = File(context.cacheDir, "osmdroid")
        return if (cacheDir.exists()) cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() } else 0L
    }

    fun clearCache() {
        val cacheDir = File(context.cacheDir, "osmdroid")
        if (cacheDir.exists()) cacheDir.deleteRecursively()
    }
}
