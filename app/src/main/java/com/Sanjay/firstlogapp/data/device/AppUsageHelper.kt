package com.Sanjay.firstlogapp.data.device

import com.Sanjay.firstlogapp.data.local.DataManager
import com.Sanjay.firstlogapp.data.model.AppUsageRecord

import android.app.Activity
import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.util.Locale

object AppUsageHelper {
    fun hasPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun cleanPackageName(pkg: String): String {
        val parts = pkg.split(".")
        val candidates = parts.filter { it !in listOf("com", "org", "net", "io", "android", "google", "app", "apps", "mobile", "inc") }
        val name = candidates.firstOrNull() ?: parts.lastOrNull() ?: pkg
        return name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

    fun resolveAppName(context: Context, pkg: String): String {
        return try {
            val pm = context.packageManager
            val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION") pm.getApplicationInfo(pkg, 0)
            }
            val label = pm.getApplicationLabel(appInfo).toString()
            if (label.isBlank() || label == pkg) cleanPackageName(pkg) else label
        } catch (_: Exception) {
            cleanPackageName(pkg)
        }
    }

    fun isIgnoredSystemPackage(pkg: String, myPkg: String = ""): Boolean {
        // Only ignore pure system UI, Android OS framework, and home launchers
        if (pkg == "com.android.systemui") return true
        if (pkg == "android") return true
        if (pkg.contains("launcher") || pkg.contains("quickstep") || pkg.contains("trebuchet")) return true
        return false
    }

    fun getTopForegroundPackage(usm: UsageStatsManager, now: Long): String? {
        try {
            val events = usm.queryEvents(now - 4000L, now)
            val event = UsageEvents.Event()
            var lastResumedPkg: String? = null
            var lastResumedTime = 0L

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED || event.eventType == 1) {
                    if (event.timeStamp >= lastResumedTime) {
                        lastResumedTime = event.timeStamp
                        lastResumedPkg = event.packageName
                    }
                }
            }
            return lastResumedPkg
        } catch (_: Exception) {
            return null
        }
    }

    fun getTodayUsageStats(
        context: Context,
        dataManager: DataManager,
        cycleStartTimeMs: Long = System.currentTimeMillis()
    ): List<AppUsageRecord> {
        if (!hasPermission(context)) return emptyList()
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return emptyList()
        val now = System.currentTimeMillis()

        class AppStats(
            var foregroundTimeMs: Long = 0L,
            var backgroundServiceTimeMs: Long = 0L,
            var launchCount: Int = 0,
            var lastSessionTime: Long = 0L,
            var lastSessionDurationMs: Long = 0L,
            var ongoingActivityStart: Long = 0L,
            var ongoingServiceStart: Long = 0L
        )

        val statsMap = mutableMapOf<String, AppStats>()
        val lookbackStart = (cycleStartTimeMs - 2 * 3600_000L).coerceAtLeast(0L)

        try {
            val events = usm.queryEvents(lookbackStart, now)
            val event = UsageEvents.Event()

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val pkg = event.packageName ?: continue
                if (isIgnoredSystemPackage(pkg, context.packageName)) continue

                val t = event.timeStamp
                val eventType = event.eventType
                val appStats = statsMap.getOrPut(pkg) { AppStats() }

                when (eventType) {
                    // 1: Activity Resumed (Foreground Screen On)
                    UsageEvents.Event.ACTIVITY_RESUMED, 1 -> {
                        val clampedStart = if (t < cycleStartTimeMs) cycleStartTimeMs else t
                        appStats.ongoingActivityStart = clampedStart
                        appStats.lastSessionTime = t
                    }
                    // 2: Activity Paused, 23: Activity Stopped
                    UsageEvents.Event.ACTIVITY_PAUSED, 2, 23 -> {
                        if (appStats.ongoingActivityStart > 0L) {
                            val end = t.coerceAtMost(now)
                            val dur = end - appStats.ongoingActivityStart
                            if (dur >= 2000L) {
                                appStats.foregroundTimeMs += dur
                                appStats.lastSessionDurationMs = dur
                                if (t >= cycleStartTimeMs) {
                                    appStats.launchCount++
                                } else if (appStats.launchCount == 0) {
                                    appStats.launchCount = 1
                                }
                            }
                            appStats.ongoingActivityStart = 0L
                        }
                        appStats.lastSessionTime = t
                    }
                    // 19: Foreground Service Start (Audio, Meditation, Prana Breath, Workouts)
                    19 -> {
                        val clampedStart = if (t < cycleStartTimeMs) cycleStartTimeMs else t
                        appStats.ongoingServiceStart = clampedStart
                        appStats.lastSessionTime = t
                    }
                    // 20: Foreground Service Stop
                    20 -> {
                        if (appStats.ongoingServiceStart > 0L) {
                            val end = t.coerceAtMost(now)
                            val dur = end - appStats.ongoingServiceStart
                            if (dur >= 2000L) {
                                appStats.backgroundServiceTimeMs += dur
                                if (appStats.launchCount == 0) appStats.launchCount = 1
                            }
                            appStats.ongoingServiceStart = 0L
                        }
                        appStats.lastSessionTime = t
                    }
                }
            }

            // Close any currently ongoing sessions up to now
            for ((_, appStats) in statsMap) {
                if (appStats.ongoingActivityStart > 0L) {
                    val liveDur = (now - appStats.ongoingActivityStart).coerceAtLeast(0L)
                    if (liveDur >= 2000L) {
                        appStats.foregroundTimeMs += liveDur
                        appStats.lastSessionDurationMs = liveDur
                        if (appStats.launchCount == 0) appStats.launchCount = 1
                    }
                    appStats.ongoingActivityStart = 0L
                }
                if (appStats.ongoingServiceStart > 0L) {
                    val liveDur = (now - appStats.ongoingServiceStart).coerceAtLeast(0L)
                    if (liveDur >= 2000L) {
                        appStats.backgroundServiceTimeMs += liveDur
                        if (appStats.launchCount == 0) appStats.launchCount = 1
                    }
                    appStats.ongoingServiceStart = 0L
                }
            }
        } catch (_: Exception) {}

        // Corroborate with system aggregated stats
        try {
            val aggregated = usm.queryAndAggregateUsageStats(cycleStartTimeMs, now)
            for ((pkg, stats) in aggregated) {
                if (isIgnoredSystemPackage(pkg, context.packageName)) continue
                val entry = statsMap.getOrPut(pkg) { AppStats() }
                val fg = stats.totalTimeInForeground
                if (fg > entry.foregroundTimeMs) {
                    entry.foregroundTimeMs = fg
                }
                if (stats.lastTimeUsed > entry.lastSessionTime) {
                    entry.lastSessionTime = stats.lastTimeUsed
                }
            }
        } catch (_: Exception) {}

        val result = mutableListOf<AppUsageRecord>()
        for ((pkg, stats) in statsMap) {
            val totalTime = stats.foregroundTimeMs + stats.backgroundServiceTimeMs
            if (totalTime >= 1000L) {
                val appName = resolveAppName(context, pkg)
                result.add(
                    AppUsageRecord(
                        packageName = pkg,
                        appName = appName,
                        usageTimeMs = totalTime,
                        lastSessionTime = stats.lastSessionTime,
                        lastSessionDurationMs = stats.lastSessionDurationMs,
                        launchCount = stats.launchCount.coerceAtLeast(1),
                        foregroundTimeMs = stats.foregroundTimeMs,
                        backgroundServiceTimeMs = stats.backgroundServiceTimeMs
                    )
                )
            }
        }
        return result.sortedByDescending { it.usageTimeMs }
    }
}
