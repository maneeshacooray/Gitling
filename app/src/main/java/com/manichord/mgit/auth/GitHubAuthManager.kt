package com.manichord.mgit.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.manichord.mgit.models.Account
import com.manichord.mgit.models.AccountType
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Scanner

/** Progress callback states for the GitHub OAuth Device Flow. */
sealed class DeviceFlowStatus {
    /** Browser has been opened; show the user_code only if verification_uri_complete wasn't available. */
    data class WaitingForUser(val userCode: String, val verificationUri: String, val codePreFilled: Boolean) : DeviceFlowStatus()
    data class Success(val account: Account) : DeviceFlowStatus()
    data class Failed(val message: String) : DeviceFlowStatus()
}

/**
 * GitHub sign-in via OAuth Device Flow (https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/authorizing-oauth-apps#device-flow).
 *
 * Device Flow needs no client_secret and no redirect URI, so it's the only flow that's safe to
 * ship in a distributed APK without a backend. The client_id below is not a secret -- it just
 * identifies the OAuth App registered for this app build.
 */
class GitHubAuthManager(private val context: Context, private val accountManager: AccountManager) {

    companion object {
        // Register an OAuth App at https://github.com/settings/developers with
        // "Enable Device Flow" turned on, then replace this with its Client ID.
        const val CLIENT_ID = "REPLACE_WITH_YOUR_GITHUB_OAUTH_CLIENT_ID"
        private const val DEVICE_CODE_URL = "https://github.com/login/device/code"
        private const val TOKEN_URL = "https://github.com/login/oauth/access_token"
        private const val API_URL = "https://api.github.com"
        private const val GRANT_TYPE = "urn:ietf:params:oauth:grant-type:device_code"
        private const val MIN_POLL_INTERVAL_SECONDS = 5
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    fun isConfigured(): Boolean = CLIENT_ID.isNotBlank() && CLIENT_ID != "REPLACE_WITH_YOUR_GITHUB_OAUTH_CLIENT_ID"

    /** Starts the device flow on a background thread; [onStatus] is always invoked on the main thread. */
    fun startDeviceFlow(onStatus: (DeviceFlowStatus) -> Unit) {
        if (!isConfigured()) {
            postStatus(onStatus, DeviceFlowStatus.Failed("GitHub sign-in isn't configured for this build (missing OAuth Client ID)."))
            return
        }

        Thread {
            try {
                val deviceCode = requestDeviceCode()
                if (deviceCode == null) {
                    postStatus(onStatus, DeviceFlowStatus.Failed("Couldn't reach GitHub. Check your connection and try again."))
                    return@Thread
                }

                val verificationUriComplete = deviceCode.optString("verification_uri_complete", "")
                val verificationUri = deviceCode.getString("verification_uri")
                val userCode = deviceCode.getString("user_code")
                val openUri = verificationUriComplete.ifBlank { verificationUri }

                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(openUri)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                postStatus(
                    onStatus,
                    DeviceFlowStatus.WaitingForUser(
                        userCode = userCode,
                        verificationUri = verificationUri,
                        codePreFilled = verificationUriComplete.isNotBlank()
                    )
                )

                val intervalSeconds = deviceCode.optInt("interval", MIN_POLL_INTERVAL_SECONDS)
                    .coerceAtLeast(MIN_POLL_INTERVAL_SECONDS)
                val deviceCodeValue = deviceCode.getString("device_code")
                val expiresAtMillis = System.currentTimeMillis() + deviceCode.optInt("expires_in", 900) * 1000L

                while (System.currentTimeMillis() < expiresAtMillis) {
                    Thread.sleep(intervalSeconds * 1000L)
                    val accessToken = pollForToken(deviceCodeValue) ?: continue
                    if (accessToken.isEmpty()) {
                        postStatus(onStatus, DeviceFlowStatus.Failed("GitHub sign-in was cancelled or denied."))
                        return@Thread
                    }
                    val account = completeSignIn(accessToken)
                    if (account == null) {
                        postStatus(onStatus, DeviceFlowStatus.Failed("Connected, but couldn't load your GitHub profile."))
                    } else {
                        postStatus(onStatus, DeviceFlowStatus.Success(account))
                    }
                    return@Thread
                }
                postStatus(onStatus, DeviceFlowStatus.Failed("GitHub sign-in timed out. Please try again."))
            } catch (e: Exception) {
                Timber.e(e, "GitHub device flow error")
                postStatus(onStatus, DeviceFlowStatus.Failed("GitHub sign-in failed: ${e.message}"))
            }
        }.start()
    }

    private fun completeSignIn(token: String): Account? {
        val user = fetchUser(token) ?: return null
        val username = user.getString("login")
        val existing = accountManager.getAccounts().find { it.username == username && it.type == AccountType.GITHUB }
        val account = existing?.copy(token = token) ?: Account(
            name = "GitHub ($username)",
            username = username,
            token = token,
            type = AccountType.GITHUB
        )
        if (existing != null) accountManager.updateAccount(account) else accountManager.addAccount(account)
        return account
    }

    private fun postStatus(onStatus: (DeviceFlowStatus) -> Unit, status: DeviceFlowStatus) {
        mainHandler.post { onStatus(status) }
    }

    private fun requestDeviceCode(): JSONObject? {
        val conn = (URL(DEVICE_CODE_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            doOutput = true
        }
        val body = "client_id=${encode(CLIENT_ID)}&scope=${encode("repo user")}"
        OutputStreamWriter(conn.outputStream).use { it.write(body) }
        if (conn.responseCode != 200) return null
        return JSONObject(readBody(conn.inputStream))
    }

    /**
     * Polls the token endpoint once.
     * Returns null while still pending (keep polling), "" if denied/expired, or the access token on success.
     */
    private fun pollForToken(deviceCode: String): String? {
        val conn = (URL(TOKEN_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            doOutput = true
        }
        val body = "client_id=${encode(CLIENT_ID)}&device_code=${encode(deviceCode)}&grant_type=${encode(GRANT_TYPE)}"
        OutputStreamWriter(conn.outputStream).use { it.write(body) }

        val stream = if (conn.responseCode == 200) conn.inputStream else conn.errorStream
        val json = JSONObject(readBody(stream))

        val accessToken = json.optString("access_token")
        if (accessToken.isNotBlank()) return accessToken

        return when (json.optString("error")) {
            "authorization_pending", "slow_down" -> null
            else -> "" // access_denied, expired_token, or any other terminal error
        }
    }

    private fun fetchUser(token: String): JSONObject? {
        val conn = (URL("$API_URL/user").openConnection() as HttpURLConnection).apply {
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Accept", "application/json")
        }
        if (conn.responseCode != 200) return null
        return JSONObject(readBody(conn.inputStream))
    }

    /** Lists the signed-in user's repos for the clone-screen repo picker. Callback runs on the main thread. */
    fun fetchRepos(token: String, onResult: (List<JSONObject>) -> Unit) {
        Thread {
            try {
                val conn = (URL("$API_URL/user/repos?sort=updated&per_page=100").openConnection() as HttpURLConnection).apply {
                    setRequestProperty("Authorization", "Bearer $token")
                    setRequestProperty("Accept", "application/json")
                }
                val repos = if (conn.responseCode == 200) {
                    val reposJson = JSONArray(readBody(conn.inputStream))
                    (0 until reposJson.length()).map { reposJson.getJSONObject(it) }
                } else {
                    emptyList()
                }
                mainHandler.post { onResult(repos) }
            } catch (e: Exception) {
                Timber.e(e, "Error fetching repos")
                mainHandler.post { onResult(emptyList()) }
            }
        }.start()
    }

    private fun readBody(stream: java.io.InputStream) = Scanner(stream).useDelimiter("\\A").let {
        if (it.hasNext()) it.next() else ""
    }

    private fun encode(s: String) = URLEncoder.encode(s, "UTF-8")
}
