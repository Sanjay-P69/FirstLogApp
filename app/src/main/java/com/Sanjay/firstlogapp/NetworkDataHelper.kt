package com.Sanjay.firstlogapp

import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import com.sanjay.tracewise.data.device.DeviceStateHelper
import java.util.Locale

object NetworkDataHelper {

    fun getWifiSsid(context: Context): String {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (cm != null) {
                val activeNetwork = cm.activeNetwork
                if (activeNetwork != null) {
                    val caps = cm.getNetworkCapabilities(activeNetwork)
                    if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val transportInfo = caps.transportInfo
                            if (transportInfo is WifiInfo) {
                                val s = cleanSsid(transportInfo.ssid)
                                if (s.isNotBlank() && s != "<unknown ssid>") return s
                            }
                        }
                    }
                }
            }

            val wm = context.applicationContext.getSystemService(
                Context.WIFI_SERVICE
            ) as? WifiManager
            val info = wm?.connectionInfo
            val ssid = cleanSsid(info?.ssid)
            if (ssid.isNotBlank() && ssid != "<unknown ssid>") {
                return ssid
            }
        } catch (_: Exception) {}

        return if (DeviceStateHelper.isWifiEnabled(context)) "Wi-Fi Connected" else "Disconnected"
    }

    private fun cleanSsid(raw: String?): String {
        if (raw == null) return ""
        var s = raw.trim()
        if (s.startsWith("\"") && s.endsWith("\"") && s.length >= 2) {
            s = s.substring(1, s.length - 1).trim()
        }
        return s
    }

    fun getNetworkBytes(context: Context, isWifi: Boolean, startTime: Long, endTime: Long): Long {
        if (endTime <= startTime) return 0L
        val transport = if (isWifi) {
            NetworkCapabilities.TRANSPORT_WIFI
        } else {
            NetworkCapabilities.TRANSPORT_CELLULAR
        }

        // Try NetworkStatsManager (requires PACKAGE_USAGE_STATS permission)
        try {
            val nsm = context.getSystemService(
                Context.NETWORK_STATS_SERVICE
            ) as? NetworkStatsManager
            if (nsm != null) {
                val bucket = nsm.querySummaryForDevice(transport, null, startTime, endTime)
                val total = (bucket.rxBytes + bucket.txBytes).coerceAtLeast(0L)
                if (total > 0L) return total
            }
        } catch (_: Exception) {}

        // Fallback: estimate from TrafficStats if available
        try {
            val totalMobile = TrafficStats.getMobileRxBytes() + TrafficStats.getMobileTxBytes()
            val totalAll = TrafficStats.getTotalRxBytes() + TrafficStats.getTotalTxBytes()
            val totalWifi = (totalAll - totalMobile).coerceAtLeast(0L)
            val current = if (isWifi) totalWifi else totalMobile
            if (current > 0L) {
                // Return a scaled proportion if needed
                return current
            }
        } catch (_: Exception) {}

        return 0L
    }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "0 MB"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0

        return when {
            gb >= 1.0 -> String.format(Locale.getDefault(), "%.2f GB", gb)
            mb >= 1.0 -> String.format(Locale.getDefault(), "%.1f MB", mb)
            kb >= 1.0 -> String.format(Locale.getDefault(), "%.0f KB", kb)
            else -> "$bytes B"
        }
    }

    fun formatUsagePerSession(totalBytes: Long, sessionCount: Int): String {
        if (sessionCount <= 0 || totalBytes <= 0L) return "0 MB / session"
        val avgBytes = totalBytes / sessionCount
        return "${formatBytes(avgBytes)} / session"
    }

    fun getTodayWifiBytes(
        context: Context,
        startTime: Long,
        endTime: Long = System.currentTimeMillis()
    ): Long = getNetworkBytes(context, isWifi = true, startTime = startTime, endTime = endTime)

    fun getTodayMobileDataBytes(
        context: Context,
        startTime: Long,
        endTime: Long = System.currentTimeMillis()
    ): Long = getNetworkBytes(context, isWifi = false, startTime = startTime, endTime = endTime)
}
