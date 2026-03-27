package com.angail

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ServerSocket
import java.net.Socket

class AgentController(private val context: Context) {
    
    private val notificationHelper = NotificationHelper(context)
    private val appLauncher = AppLauncher(context)
    private var serverJob: Job? = null
    private var serverSocket: ServerSocket? = null
    
    companion object {
        private const val TAG = "AgentController"
        private const val SERVER_PORT = 9999
    }
    
    fun startAgent(socialMediaThreshold: Int, screenTimeThreshold: Int) {
        Log.d(TAG, "Starting agent on port $SERVER_PORT")
        
        serverJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                serverSocket = ServerSocket(SERVER_PORT)
                Log.d(TAG, "Server started, waiting for connections...")
                
                while (isActive) {
                    try {
                        val client = serverSocket?.accept()
                        client?.let { handleClientConnection(it) }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error accepting connection: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Server error: ${e.message}")
            }
        }
    }
    
    private suspend fun handleClientConnection(socket: Socket) {
        withContext(Dispatchers.IO) {
            try {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val writer = OutputStreamWriter(socket.getOutputStream())
                
                val input = reader.readLine()
                Log.d(TAG, "Received: $input")
                
                val response = processCommand(input)
                writer.write(response)
                writer.flush()
                
                reader.close()
                writer.close()
                socket.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error handling client: ${e.message}")
            }
        }
    }
    
    private fun processCommand(input: String): String {
        return try {
            when {
                input.startsWith("NOTIFICATION:") -> {
                    val message = input.substring("NOTIFICATION:".length).trim()
                    notificationHelper.showNotification(message)
                    "OK"
                }
                input.startsWith("OPEN_APP:") -> {
                    val parts = input.substring("OPEN_APP:".length).trim().split("|")
                    val packageName = parts[0]
                    val message = if (parts.size > 1) parts[1] else ""
                    
                    if (appLauncher.isAppInstalled(packageName)) {
                        notificationHelper.showNotification(message, packageName)
                        "OK"
                    } else {
                        "ERROR: App not installed"
                    }
                }
                input == "PING" -> {
                    "PONG"
                }
                else -> {
                    "ERROR: Unknown command"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing command: ${e.message}")
            "ERROR: ${e.message}"
        }
    }
    
    fun stopAgent() {
        Log.d(TAG, "Stopping agent")
        serverJob?.cancel()
        serverSocket?.close()
        serverJob = null
        serverSocket = null
    }
    
    fun isRunning(): Boolean {
        return serverJob?.isActive == true
    }
}
