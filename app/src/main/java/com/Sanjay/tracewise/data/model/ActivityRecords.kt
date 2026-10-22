package com.sanjay.tracewise.data.model

import android.provider.CallLog

data class ChargingSessionRecord(
    val startTime: Long,
    val endTime: Long,
    val startLevel: Int,
    val endLevel: Int,
    val durationMs: Long
)

data class UnlockSessionRecord(
    val timestamp: Long,
    val durationMs: Long,
    val isQuickGlance: Boolean,
    val unlockType: String
)

data class CallRecord(
    val name: String,
    val number: String,
    val timestamp: Long,
    val durationSeconds: Long,
    val type: Int = CallLog.Calls.INCOMING_TYPE
)
data class CallerStat(
    val displayName: String,
    val number: String,
    val totalSeconds: Long,
    val callCount: Int
)
data class AppUsageRecord(
    val packageName: String,
    val appName: String,
    val usageTimeMs: Long,
    val lastSessionTime: Long = 0L,
    val lastSessionDurationMs: Long = 0L,
    val launchCount: Int = 1,
    val foregroundTimeMs: Long = usageTimeMs,
    val backgroundServiceTimeMs: Long = 0L
)
data class BatteryHistoryRecord(
    val date: String,
    val batteryUsedPct: Int,
    val chargeDurationMs: Long
)
