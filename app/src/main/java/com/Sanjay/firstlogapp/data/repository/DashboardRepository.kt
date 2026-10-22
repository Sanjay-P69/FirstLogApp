package com.Sanjay.firstlogapp.data.repository

import android.content.Context
import com.Sanjay.firstlogapp.AppNotificationListenerService
import com.Sanjay.firstlogapp.NetworkDataHelper
import com.Sanjay.firstlogapp.BluetoothCategoryHelper
import com.Sanjay.firstlogapp.data.device.CallLogHelper
import com.Sanjay.firstlogapp.data.device.AppUsageHelper
import com.Sanjay.firstlogapp.data.model.CallRecord
import com.Sanjay.firstlogapp.data.local.DataManager
import com.Sanjay.firstlogapp.data.model.AppUsageRecord

/** Raw dashboard data. No Compose types, colors, resources, or UI strings. */
data class TrackedMetricSnapshot(
    val id: String, val durationMs: Long, val sessionCount: Int, val active: Boolean
)

data class DashboardSnapshot(
    val metrics: List<TrackedMetricSnapshot> = emptyList(),
    val batteryUsed: Int = 0,
    val btBudsMs: Long = 0,
    val btWatchMs: Long = 0,
    val btAutoMs: Long = 0,
    val chargingTimeMs: Long = 0,
    val unlockCount: Int = 0,
    val unlockFingerprint: Int = 0,
    val unlockFace: Int = 0,
    val unlockPin: Int = 0,
    val incomingCalls: List<CallRecord> = emptyList(),
    val outgoingCalls: List<CallRecord> = emptyList(),
    val wifiSsid: String = "",
    val wifiBytes: Long = 0,
    val mobileBytes: Long = 0,
    val wifiSessions: Int = 0,
    val mobileSessions: Int = 0,
    val chargingSessions: Int = 0,
    val screenOnDrainRate: Double = 0.0,
    val actualUsageCount: Int = 0,
    val quickGlanceCount: Int = 0,
    val hasCallPermission: Boolean = false,
    val hasBluetoothPermission: Boolean = false,
    val hasUsagePermission: Boolean = false,
    val hasNotificationPermission: Boolean = false,
    val activeNotifications: Int = 0,
    val notificationHistory: Int = 0,
    val apps: List<AppUsageRecord> = emptyList()
)

interface DashboardRepository {
    /** Blocking platform queries; callers must dispatch off the main thread. */
    fun readSnapshot(): DashboardSnapshot
}

class AndroidDashboardRepository(
    context: Context,
    private val storage: DataManager
) : DashboardRepository {
    private val context = context.applicationContext

    override fun readSnapshot(): DashboardSnapshot {
        val cycleStart = storage.getCycleStartTimestamp()
        val usageGranted = AppUsageHelper.hasPermission(context)
        return DashboardSnapshot(
            metrics = listOf("screen", "wifi", "data", "bluetooth", "gps", "both_wifi_data").map { id ->
                TrackedMetricSnapshot(id, storage.getTodayTotalMetricTime(id), storage.getTodayMetricCount(id), storage.isMetricActive(id))
            },
            batteryUsed = storage.getTodayBatteryUsed(),
            btBudsMs = storage.getTodayBtCategoryDuration(BluetoothCategoryHelper.CAT_BUDS),
            btWatchMs = storage.getTodayBtCategoryDuration(BluetoothCategoryHelper.CAT_WATCH),
            btAutoMs = storage.getTodayBtCategoryDuration(BluetoothCategoryHelper.CAT_AUTO),
            chargingTimeMs = storage.getTodayChargingTime() + if (storage.isCharging() && storage.getLastChargeStartTime() > 0)
                (System.currentTimeMillis() - storage.getLastChargeStartTime()).coerceAtLeast(0) else 0,
            unlockCount = storage.getTodayUnlockCount(),
            unlockFingerprint = storage.getTodayUnlockFingerprint(),
            unlockFace = storage.getTodayUnlockFace(),
            unlockPin = storage.getTodayUnlockPin(),
            incomingCalls = calls(true),
            outgoingCalls = calls(false),
            wifiSsid = NetworkDataHelper.getWifiSsid(context),
            wifiBytes = NetworkDataHelper.getTodayWifiBytes(context, cycleStart),
            mobileBytes = NetworkDataHelper.getTodayMobileDataBytes(context, cycleStart),
            wifiSessions = storage.getTodayMetricCount("wifi"),
            mobileSessions = storage.getTodayMetricCount("data"),
            chargingSessions = storage.getTodayChargingSessionCount(),
            screenOnDrainRate = storage.getScreenOnDrainRate(),
            actualUsageCount = storage.getTodayActualUsageCount(),
            quickGlanceCount = storage.getTodayQuickGlanceCount(),
            hasCallPermission = granted(android.Manifest.permission.READ_CALL_LOG),
            hasBluetoothPermission = android.os.Build.VERSION.SDK_INT < 31 ||
                granted(android.Manifest.permission.BLUETOOTH_CONNECT),
            hasUsagePermission = usageGranted,
            hasNotificationPermission = AppNotificationListenerService.isPermissionGranted(context),
            activeNotifications = AppNotificationListenerService.notifications.value.size,
            notificationHistory = AppNotificationListenerService.history.value.size,
            apps = if (usageGranted) AppUsageHelper.getTodayUsageStats(context, storage, cycleStart) else emptyList()
        )
    }

    private fun calls(incoming: Boolean): List<CallRecord> =
        if (granted(android.Manifest.permission.READ_CALL_LOG)) {
            CallLogHelper.readCalls(context, incoming).filter { CallLogHelper.isCallInCurrentCycle(it.timestamp, storage) }
        } else emptyList()

    private fun granted(permission: String): Boolean =
        androidx.core.content.ContextCompat.checkSelfPermission(context, permission) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
}
