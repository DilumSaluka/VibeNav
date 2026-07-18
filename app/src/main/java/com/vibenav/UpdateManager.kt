package com.vibenav

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class UpdateManager(private val context: Context) {

    private val apiUrl = "https://api.github.com/repos/DilumSaluka/VibeNav/releases/latest"
    private val currentVersion: String
        get() {
            return try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            } catch (_: Exception) { "0.0" }
        }

    data class UpdateInfo(
        val latestVersion: String,
        val apkUrl: String,
        val releaseNotes: String,
        val tagName: String
    )

    fun checkForUpdate(onResult: (UpdateInfo?) -> Unit) {
        Thread {
            try {
                // codeql[java/android/missing-certificate-pinning] — GitHub API, no cert pinning needed
                val url = URL(apiUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                conn.setRequestProperty("User-Agent", "VibeNav/1.0")
                conn.connectTimeout = 8000
                conn.readTimeout = 8000

                val json = JSONObject(conn.inputStream.bufferedReader().readText())
                conn.disconnect()

                val tagName = json.optString("tag_name", "")
                val latestVersion = tagName.removePrefix("v")
                val body = json.optString("body", "")
                val assets = json.optJSONArray("assets")

                var apkUrl: String? = null
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk")) {
                            apkUrl = asset.optString("browser_download_url")
                            break
                        }
                    }
                }

                if (apkUrl == null) return@Thread

                val info = UpdateInfo(latestVersion, apkUrl, body, tagName)
                onResult(info)
            } catch (_: Exception) {
                onResult(null)
            }
        }.start()
    }

    fun needsUpdate(latestVersion: String): Boolean {
        val current = currentVersion.split(".").map { it.toIntOrNull() ?: 0 }
        val latest = latestVersion.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(current.size, latest.size)) {
            val c = current.getOrElse(i) { 0 }
            val l = latest.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    fun downloadAndInstall(apkUrl: String, tagName: String) {
        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val uri = Uri.parse(apkUrl)
            val fileName = "VibeNav-$tagName.apk"
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

            val request = DownloadManager.Request(uri).apply {
                setTitle("VibeNav $tagName")
                setDescription("Downloading update...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }

            downloadManager.enqueue(request)
            Toast.makeText(context, "Downloading to Downloads folder...", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            directDownloadAndInstall(apkUrl, tagName)
        }
    }

    private fun directDownloadAndInstall(apkUrl: String, tagName: String) {
        Thread {
            try {
                // codeql[java/android/missing-certificate-pinning] — GitHub releases, no cert pinning
                val url = URL(apkUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                val inputStream = conn.inputStream

                val apkFile = File(context.cacheDir, "VibeNav-$tagName.apk")
                apkFile.outputStream().use { output ->
                    inputStream.copyTo(output)
                }
                inputStream.close()
                conn.disconnect()

                installApk(apkFile)
            } catch (e: Exception) {
                android.os.Handler(context.mainLooper).post {
                    Toast.makeText(context, "Update failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun installApk(file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
