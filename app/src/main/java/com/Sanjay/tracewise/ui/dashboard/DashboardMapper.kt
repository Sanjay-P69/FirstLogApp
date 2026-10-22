package com.sanjay.tracewise.ui.dashboard

import com.sanjay.tracewise.R
import com.sanjay.tracewise.core.formatDurationMillis
import com.sanjay.tracewise.data.repository.DashboardSnapshot
import com.sanjay.tracewise.ui.model.MetricItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Converts raw measurements at the presentation boundary, never in a Composable. */
internal fun DashboardSnapshot.toUiState(now: Long = System.currentTimeMillis()): DashboardUiState {
    fun metric(id: String, title: String, icon: Int): MetricItem {
        val data = metrics.firstOrNull { it.id == id }
        return MetricItem(
            id,
            title,
            formatDurationMillis(data?.durationMs ?: 0),
            if (data?.active ==
                true
            ) {
                "${data.sessionCount} sessions · Active"
            } else {
                "${data?.sessionCount ?: 0} sessions · Inactive"
            },
            icon
        )
    }
    val wifi = metric(
        "wifi",
        "Wi-Fi",
        R.drawable.ic_metric_wifi
    ).copy(wifiSsid = wifiSsid, wifiDataBytes = wifiBytes, wifiSessionCount = wifiSessions)
    val mobile = metric(
        "data",
        "Mobile data",
        R.drawable.ic_metric_data
    ).copy(mobileDataBytes = mobileBytes, mobileSessionCount = mobileSessions)
    val dual = metric(
        "both_wifi_data",
        "Wi-Fi & mobile enabled",
        R.drawable.ic_metric_both_wifi_data
    ).copy(
        bothWifiDataTime = formatDurationMillis(
            metrics.firstOrNull {
                it.id == "both_wifi_data"
            }?.durationMs ?: 0
        ),
        bothWifiDataCount = metrics.firstOrNull { it.id == "both_wifi_data" }?.sessionCount ?: 0
    )
    val bluetooth = metric(
        "bluetooth",
        "Bluetooth",
        R.drawable.ic_metric_bluetooth
    ).copy(
        btBudsDur = formatDurationMillis(btBudsMs),
        btWatchDur = formatDurationMillis(btWatchMs),
        btAutoDur = formatDurationMillis(btAutoMs)
    )
    fun calls(incoming: Boolean): MetricItem {
        val records = if (incoming) incomingCalls else outgoingCalls
        val answered = records.filter { it.durationSeconds > 0 }
        val duration = formatDurationMillis(answered.sumOf { it.durationSeconds } * 1_000)
        return MetricItem(
            id = if (incoming) "call_in" else "call_out",
            title = if (incoming) "Incoming calls" else "Outgoing calls",
            duration = "${records.size} calls",
            status = if (hasCallPermission) "$duration connected" else "Call access needed",
            iconResId = if (incoming) {
                R.drawable.ic_metric_call_in
            } else {
                R.drawable.ic_metric_call_out
            },
            connectedCallsCount = answered.size,
            connectedCallsDuration = duration,
            missedCallsCount = records.count { it.durationSeconds == 0L }
        )
    }
    return DashboardUiState(
        date = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date(now)),
        screen = metric("screen", "Screen time", R.drawable.ic_metric_screen),
        battery = MetricItem(
            "battery",
            "Battery used",
            "$batteryUsed%",
            "Charging · ${formatDurationMillis(chargingTimeMs)}",
            R.drawable.ic_metric_battery,
            chargingSessionsCount = chargingSessions,
            screenOnDrainRate = screenOnDrainRate
        ),
        connections = listOf(
            wifi,
            mobile.copy(
                bothWifiDataTime = dual.bothWifiDataTime,
                bothWifiDataCount = dual.bothWifiDataCount
            ),
            bluetooth,
            metric("gps", "Location enabled", R.drawable.ic_metric_gps),
            dual
        ),
        calls = listOf(calls(true), calls(false)),
        unlocks = MetricItem(
            "unlock",
            "Unlocks",
            unlockCount.toString(),
            "$actualUsageCount active · $quickGlanceCount quick glances",
            R.drawable.ic_metric_unlock,
            unlockFingerprint =
            unlockFingerprint,
            unlockFace = unlockFace, unlockPin = unlockPin,
            actualUsageCount = actualUsageCount, quickGlanceCount = quickGlanceCount
        ),
        appUsage = formatDurationMillis(apps.sumOf { it.usageTimeMs }),
        apps = apps.map {
            AppUsageUiItem(
                it.packageName,
                it.appName,
                formatDurationMillis(it.usageTimeMs),
                it.launchCount,
                formatDurationMillis(it.foregroundTimeMs),
                formatDurationMillis(it.backgroundServiceTimeMs),
                formatDurationMillis(it.lastSessionDurationMs)
            )
        },
        activeNotifications = activeNotifications, notificationHistory = notificationHistory,
        canReadCalls = hasCallPermission, canReadBluetooth = hasBluetoothPermission,
        canReadApps = hasUsagePermission, canReadNotifications = hasNotificationPermission
    )
}
