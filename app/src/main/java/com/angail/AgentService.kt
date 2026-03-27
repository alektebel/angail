package com.angail

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log

class AgentService : Service() {

    private lateinit var agentController: AgentController

    companion object {
        private const val TAG = "AgentService"
        private const val NOTIFICATION_ID = 1000
        private const val CHANNEL_ID = "angail_service"

        const val EXTRA_SOCIAL_MEDIA_THRESHOLD = "social_media_threshold"
        const val EXTRA_SCREEN_TIME_THRESHOLD = "screen_time_threshold"

        fun start(context: Context, socialMediaThreshold: Int, screenTimeThreshold: Int) {
            val intent = Intent(context, AgentService::class.java).apply {
                putExtra(EXTRA_SOCIAL_MEDIA_THRESHOLD, socialMediaThreshold)
                putExtra(EXTRA_SCREEN_TIME_THRESHOLD, screenTimeThreshold)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AgentService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        agentController = AgentController(this, GitHubAuth(this))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val socialMediaThreshold = intent?.getIntExtra(EXTRA_SOCIAL_MEDIA_THRESHOLD, 30) ?: 30
        val screenTimeThreshold = intent?.getIntExtra(EXTRA_SCREEN_TIME_THRESHOLD, 120) ?: 120

        startForeground(NOTIFICATION_ID, buildNotification())
        agentController.startAgent(socialMediaThreshold, screenTimeThreshold)

        Log.d(TAG, "AgentService started")
        return START_STICKY
    }

    override fun onDestroy() {
        agentController.stopAgent()
        Log.d(TAG, "AgentService stopped")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        createNotificationChannel()

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Angail Guardian")
                .setContentText("Monitoring app usage")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("Angail Guardian")
                .setContentText("Monitoring app usage")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .build()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Angail Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Angail usage monitoring service"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
