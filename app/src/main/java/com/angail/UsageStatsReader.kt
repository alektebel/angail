package com.angail

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AppUsageStat(
    val packageName: String,
    val appName: String,
    val totalTimeMs: Long
)

class UsageStatsReader(private val context: Context) {

    fun getTopApps(hours: Int = 24, limit: Int = 20): List<AppUsageStat> {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val start = end - TimeUnit.HOURS.toMillis(hours.toLong())

        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end)
            ?: return emptyList()

        return stats
            .filter { it.totalTimeInForeground > 0 && it.packageName != context.packageName }
            .sortedByDescending { it.totalTimeInForeground }
            .take(limit)
            .map { stat ->
                AppUsageStat(
                    packageName = stat.packageName,
                    appName = resolveAppName(stat.packageName),
                    totalTimeMs = stat.totalTimeInForeground
                )
            }
    }

    // Uses event stream for precise foreground time of a single app in a short window
    fun getAppForegroundTime(packageName: String, hours: Int = 1): Long {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val start = end - TimeUnit.HOURS.toMillis(hours.toLong())

        val events = usm.queryEvents(start, end)
        var foregroundStart = 0L
        var totalTime = 0L
        val event = UsageEvents.Event()

        val foregroundType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            UsageEvents.Event.ACTIVITY_RESUMED else UsageEvents.Event.MOVE_TO_FOREGROUND
        val backgroundType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            UsageEvents.Event.ACTIVITY_PAUSED else UsageEvents.Event.MOVE_TO_BACKGROUND

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.packageName != packageName) continue
            when (event.eventType) {
                foregroundType -> foregroundStart = event.timeStamp
                backgroundType -> {
                    if (foregroundStart > 0) {
                        totalTime += event.timeStamp - foregroundStart
                        foregroundStart = 0
                    }
                }
            }
        }
        // Still in foreground at query time
        if (foregroundStart > 0) totalTime += end - foregroundStart

        return totalTime
    }

    fun toJson(stats: List<AppUsageStat>): String {
        val json = JSONObject()
        for (stat in stats) {
            json.put(stat.packageName, JSONObject().apply {
                put("name", stat.appName)
                put("totalMs", stat.totalTimeMs)
                put("totalMin", TimeUnit.MILLISECONDS.toMinutes(stat.totalTimeMs))
            })
        }
        return json.toString()
    }

    private fun resolveAppName(packageName: String): String {
        return try {
            val info = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(info).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }
}
