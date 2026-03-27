package com.angail

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class GitHubAuth(private val context: Context) {

    companion object {
        private const val TAG = "GitHubAuth"
        // Create an OAuth App at https://github.com/settings/developers
        // Set "Application type" to "Public client" — no secret needed for device flow
        const val CLIENT_ID = "YOUR_GITHUB_CLIENT_ID"
        private const val SCOPE = "read:user"
        private const val PREFS_NAME = "github_auth"
        private const val KEY_TOKEN = "access_token"
        private const val KEY_LOGIN = "login"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isConfigured(): Boolean = CLIENT_ID != "YOUR_GITHUB_CLIENT_ID"
    fun isAuthenticated(): Boolean = prefs.getString(KEY_TOKEN, null) != null
    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)
    fun getLogin(): String? = prefs.getString(KEY_LOGIN, null)

    fun logout() = prefs.edit().remove(KEY_TOKEN).remove(KEY_LOGIN).apply()

    data class DeviceCodeResponse(
        val deviceCode: String,
        val userCode: String,
        val verificationUri: String,
        val expiresIn: Int,
        val intervalSeconds: Int
    )

    // Returns null on success-path error; throws IllegalStateException with human-readable message
    // on configuration or API errors so callers can show the right message.
    suspend fun requestDeviceCode(): DeviceCodeResponse = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            throw IllegalStateException(
                "GitHub Client ID not set.\n" +
                "1. Go to github.com/settings/developers\n" +
                "2. Create an OAuth App (type: Public client)\n" +
                "3. Paste the Client ID into GitHubAuth.CLIENT_ID"
            )
        }
        try {
            val url = URL("https://github.com/login/device/code")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                doOutput = true
                outputStream.write("client_id=$CLIENT_ID&scope=$SCOPE".toByteArray())
            }
            val body = (conn.inputStream ?: conn.errorStream)?.bufferedReader()?.readText()
                ?: throw IllegalStateException("Empty response from GitHub (HTTP ${conn.responseCode})")
            val json = JSONObject(body)
            if (json.has("error")) {
                throw IllegalStateException("GitHub error: ${json.optString("error_description", json.getString("error"))}")
            }
            DeviceCodeResponse(
                deviceCode = json.getString("device_code"),
                userCode = json.getString("user_code"),
                verificationUri = json.getString("verification_uri"),
                expiresIn = json.getInt("expires_in"),
                intervalSeconds = json.getInt("interval")
            )
        } catch (e: IllegalStateException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "requestDeviceCode: ${e.message}")
            throw IllegalStateException("Could not reach GitHub: ${e.message}")
        }
    }

    // Returns the access token once the user has authorized, or null on timeout/error
    suspend fun pollForToken(deviceCode: DeviceCodeResponse): String? =
        withContext(Dispatchers.IO) {
            val deadline = System.currentTimeMillis() + deviceCode.expiresIn * 1000L
            var intervalMs = deviceCode.intervalSeconds * 1000L

            while (System.currentTimeMillis() < deadline) {
                delay(intervalMs)
                val (token, slowDown) = tryExchangeToken(deviceCode.deviceCode)
                if (slowDown) intervalMs += 5_000
                if (token != null) {
                    prefs.edit().putString(KEY_TOKEN, token).apply()
                    fetchLogin(token)
                    return@withContext token
                }
            }
            null
        }

    // Returns Pair(token, slowDown)
    private fun tryExchangeToken(deviceCode: String): Pair<String?, Boolean> {
        return try {
            val url = URL("https://github.com/login/oauth/access_token")
            val body = "client_id=$CLIENT_ID&device_code=$deviceCode" +
                "&grant_type=urn:ietf:params:oauth:grant-type:device_code"
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                doOutput = true
                outputStream.write(body.toByteArray())
            }
            val json = JSONObject(conn.inputStream.bufferedReader().readText())
            val error = json.optString("error")
            when {
                json.has("access_token") -> Pair(json.getString("access_token"), false)
                error == "slow_down" -> Pair(null, true)
                else -> Pair(null, false) // authorization_pending or expired
            }
        } catch (e: Exception) {
            Log.e(TAG, "tryExchangeToken: ${e.message}")
            Pair(null, false)
        }
    }

    private fun fetchLogin(token: String) {
        try {
            val url = URL("https://api.github.com/user")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Accept", "application/vnd.github+json")
            }
            if (conn.responseCode == 200) {
                val json = JSONObject(conn.inputStream.bufferedReader().readText())
                prefs.edit().putString(KEY_LOGIN, json.getString("login")).apply()
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchLogin: ${e.message}")
        }
    }
}
