package com.manichord.mgit.update

import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Scanner

sealed class UpdateCheckResult {
    data class UpdateAvailable(val versionName: String, val releaseUrl: String) : UpdateCheckResult()
    object UpToDate : UpdateCheckResult()
    data class Failed(val message: String) : UpdateCheckResult()
}

/**
 * Checks GitHub Releases for a newer version than the one currently installed. This app isn't
 * on Google Play, so there's no Play Store auto-update -- once it's also on F-Droid, F-Droid's
 * own client covers that case, but this still matters for anyone who installed the GitHub
 * release APK directly.
 */
class UpdateChecker {

    companion object {
        private const val LATEST_RELEASE_API_URL =
            "https://api.github.com/repos/maneeshacooray/Gitling/releases/latest"
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    /** [currentVersionName] should be BuildConfig.VERSION_NAME (e.g. "1.0.13"); [onResult] always
     * runs on the main thread. */
    fun checkForUpdate(currentVersionName: String, onResult: (UpdateCheckResult) -> Unit) {
        Thread {
            try {
                val conn = (URL(LATEST_RELEASE_API_URL).openConnection() as HttpURLConnection).apply {
                    setRequestProperty("Accept", "application/vnd.github+json")
                }
                if (conn.responseCode != 200) {
                    postResult(onResult, UpdateCheckResult.Failed("HTTP ${conn.responseCode}"))
                    return@Thread
                }
                val json = JSONObject(readBody(conn.inputStream))
                val latestVersionName = json.getString("tag_name").removePrefix("v")
                val releaseUrl = json.optString(
                    "html_url",
                    "https://github.com/maneeshacooray/Gitling/releases/latest"
                )
                if (isNewer(latestVersionName, currentVersionName)) {
                    postResult(onResult, UpdateCheckResult.UpdateAvailable(latestVersionName, releaseUrl))
                } else {
                    postResult(onResult, UpdateCheckResult.UpToDate)
                }
            } catch (e: IOException) {
                Timber.w(e, "Update check failed")
                postResult(onResult, UpdateCheckResult.Failed(e.message ?: "Network error"))
            } catch (e: Exception) {
                Timber.e(e, "Update check failed unexpectedly")
                postResult(onResult, UpdateCheckResult.Failed(e.message ?: "Unknown error"))
            }
        }.start()
    }

    /** Compares dotted version strings (e.g. "1.0.13" vs "1.0.9") numerically per component,
     * rather than as strings -- "1.0.9" must compare as older than "1.0.10". */
    private fun isNewer(candidate: String, current: String): Boolean {
        val candidateParts = candidate.split(".")
        val currentParts = current.split(".")
        for (i in 0 until maxOf(candidateParts.size, currentParts.size)) {
            val c = candidateParts.getOrNull(i)?.toIntOrNull() ?: 0
            val cur = currentParts.getOrNull(i)?.toIntOrNull() ?: 0
            if (c != cur) return c > cur
        }
        return false
    }

    private fun postResult(onResult: (UpdateCheckResult) -> Unit, result: UpdateCheckResult) {
        mainHandler.post { onResult(result) }
    }

    private fun readBody(stream: java.io.InputStream) = Scanner(stream).useDelimiter("\\A").let {
        if (it.hasNext()) it.next() else ""
    }
}
