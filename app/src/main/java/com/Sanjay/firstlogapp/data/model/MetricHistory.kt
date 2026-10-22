package com.Sanjay.firstlogapp.data.model

data class MetricDayRecord(val date: String, val durationMillis: Long)
data class BatteryLevelSample(val timestamp: Long, val level: Int)

sealed interface MetricHistory {
    data class Radio(val days: List<MetricDayRecord>) : MetricHistory
    data class Battery(
        val level: Int,
        val startLevel: Int,
        val used: Int,
        val chargingMillis: Long,
        val drainPerHour: Double,
        val samples: List<BatteryLevelSample>,
        val sessions: List<ChargingSessionRecord>,
        val days: Map<String, List<BatteryHistoryRecord>>
    ) : MetricHistory
    data class Unlock(
        val count: Int,
        val activeCount: Int,
        val activeMillis: Long,
        val glanceCount: Int,
        val glanceMillis: Long,
        val peekCount: Int,
        val fingerprint: Int,
        val face: Int,
        val pin: Int,
        val sessions: List<UnlockSessionRecord>
    ) : MetricHistory
    data class Calls(val records: List<CallRecord>) : MetricHistory
}
