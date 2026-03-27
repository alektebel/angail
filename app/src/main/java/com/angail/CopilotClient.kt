package com.angail

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class CopilotClient {

    companion object {
        private const val TAG = "CopilotClient"
        // GitHub Models inference endpoint — works with any GitHub OAuth token (read:user scope)
        private const val ENDPOINT =
            "https://models.inference.ai.azure.com/chat/completions"
        private const val MODEL = "gpt-4o-mini"
    }

    suspend fun generateNotification(
        token: String,
        usageSummary: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val messages = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content",
                        "You are a mindful phone usage coach. Given a summary of the user's " +
                        "recent screen time, write a short, warm nudge (1-2 sentences max) to " +
                        "help them be more intentional. Never be preachy or judgmental."
                    )
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", usageSummary)
                })
            }
            val body = JSONObject().apply {
                put("model", MODEL)
                put("messages", messages)
                put("max_tokens", 120)
                put("temperature", 0.7)
            }

            val url = URL(ENDPOINT)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                outputStream.write(body.toString().toByteArray())
            }

            if (conn.responseCode == 200) {
                val response = JSONObject(conn.inputStream.bufferedReader().readText())
                response
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                    .trim()
            } else {
                val err = conn.errorStream?.bufferedReader()?.readText()
                Log.e(TAG, "API error ${conn.responseCode}: $err")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "generateNotification failed: ${e.message}")
            null
        }
    }
}
