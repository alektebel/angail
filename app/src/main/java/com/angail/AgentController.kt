package com.angail

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.TimeUnit

class AgentController(
    private val context: Context,
    private val githubAuth: GitHubAuth
) {

    private val notificationHelper = NotificationHelper(context)
    private val appLauncher = AppLauncher(context)
    private val usageStatsReader = UsageStatsReader(context)
    private val copilotClient = CopilotClient()

    private var serverJob: Job? = null
    private var monitorJob: Job? = null
    private var serverSocket: ServerSocket? = null

    // Packages considered social media for threshold evaluation
    private val socialMediaPackages = setOf(
        "com.instagram.android", "com.twitter.android", "com.facebook.katana",
        "com.tiktok.video", "com.snapchat.android", "com.reddit.frontpage",
        "com.zhiliaoapp.musically", "com.pinterest", "com.linkedin.android",
        "com.tumblr", "com.discord"
    )

    companion object {
        private const val TAG = "AgentController"
        private const val SERVER_PORT = 9999
        private val MONITOR_INTERVAL_MS = TimeUnit.SECONDS.toMillis(30)
    }

    fun startAgent(socialMediaThreshold: Int, screenTimeThreshold: Int) {
        Log.d(TAG, "Starting agent on port $SERVER_PORT")
        startSocketServer()
        startMonitoringLoop(socialMediaThreshold, screenTimeThreshold)
    }

    private fun startSocketServer() {
        serverJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                serverSocket = ServerSocket(SERVER_PORT)
                Log.d(TAG, "Socket server listening on $SERVER_PORT")
                while (isActive) {
                    try {
                        val client = serverSocket?.accept()
                        client?.let { launch { handleClientConnection(it) } }
                    } catch (e: Exception) {
                        if (isActive) Log.e(TAG, "Accept error: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Server error: ${e.message}")
            }
        }
    }

    private fun startMonitoringLoop(socialMediaThreshold: Int, screenTimeThreshold: Int) {
        monitorJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                delay(MONITOR_INTERVAL_MS)
                val token = githubAuth.getToken() ?: continue
                checkAndNotify(token, socialMediaThreshold, screenTimeThreshold)
            }
        }
    }

    private suspend fun checkAndNotify(
        token: String,
        socialMediaThreshold: Int,
        screenTimeThreshold: Int
    ) {
        val topApps = usageStatsReader.getTopApps(hours = 1, limit = 20)
        if (topApps.isEmpty()) return

        val socialMs = topApps
            .filter { it.packageName in socialMediaPackages }
            .sumOf { it.totalTimeMs }
        val totalMs = topApps.sumOf { it.totalTimeMs }

        val socialMin = TimeUnit.MILLISECONDS.toMinutes(socialMs)
        val totalMin = TimeUnit.MILLISECONDS.toMinutes(totalMs)

        if (socialMin < socialMediaThreshold && totalMin < screenTimeThreshold) return

        val summary = buildString {
            append("Phone usage in the past hour:\n")
            topApps.take(5).forEach { app ->
                val min = TimeUnit.MILLISECONDS.toMinutes(app.totalTimeMs)
                append("- ${app.appName}: ${min}min\n")
            }
            if (socialMin >= socialMediaThreshold)
                append("Social media total: ${socialMin}min (limit: ${socialMediaThreshold}min)\n")
            if (totalMin >= screenTimeThreshold)
                append("Total screen time: ${totalMin}min (limit: ${screenTimeThreshold}min)\n")
        }

        val message = copilotClient.generateNotification(token, summary)
        if (message != null) {
            notificationHelper.showNotification(message)
            Log.d(TAG, "Copilot notification sent")
        }
    }

    private suspend fun handleClientConnection(socket: Socket) {
        withContext(Dispatchers.IO) {
            try {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val writer = OutputStreamWriter(socket.getOutputStream())

                val input = reader.readLine()
                Log.d(TAG, "Received: $input")

                val response = processCommand(input ?: "")
                writer.write(response)
                writer.flush()

                reader.close()
                writer.close()
                socket.close()
            } catch (e: Exception) {
                Log.e(TAG, "Client handler error: ${e.message}")
            }
        }
    }

    private fun processCommand(input: String): String {
        return try {
            when {
                input.startsWith("NOTIFICATION:") -> {
                    val message = input.removePrefix("NOTIFICATION:").trim()
                    notificationHelper.showNotification(message)
                    "OK"
                }
                input.startsWith("OPEN_APP:") -> {
                    val parts = input.removePrefix("OPEN_APP:").trim().split("|")
                    val pkg = parts[0]
                    val message = parts.getOrNull(1) ?: ""
                    if (appLauncher.isAppInstalled(pkg)) {
                        notificationHelper.showNotification(message, pkg)
                        "OK"
                    } else {
                        "ERROR: App not installed"
                    }
                }
                input == "PING" -> "PONG"
                input == "GET_USAGE_STATS" -> {
                    val stats = usageStatsReader.getTopApps(hours = 1, limit = 20)
                    usageStatsReader.toJson(stats)
                }
                else -> "ERROR: Unknown command"
            }
        } catch (e: Exception) {
            Log.e(TAG, "processCommand error: ${e.message}")
            "ERROR: ${e.message}"
        }
    }

    fun stopAgent() {
        Log.d(TAG, "Stopping agent")
        monitorJob?.cancel()
        serverJob?.cancel()
        serverSocket?.close()
        monitorJob = null
        serverJob = null
        serverSocket = null
    }

    fun isRunning(): Boolean = serverJob?.isActive == true
}
