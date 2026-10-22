package com.Sanjay.firstlogapp.data.repository

import android.provider.CallLog
import com.Sanjay.firstlogapp.data.device.CallHistoryReader
import com.Sanjay.firstlogapp.data.local.DataManager
import com.Sanjay.firstlogapp.data.model.BatteryLevelSample
import com.Sanjay.firstlogapp.data.model.MetricDayRecord
import com.Sanjay.firstlogapp.data.model.MetricHistory
import org.json.JSONArray

interface MetricHistoryRepository {
    fun read(metricId: String): MetricHistory
}

class StoredMetricHistoryRepository(
    private val storage: DataManager,
    private val calls: CallHistoryReader
) : MetricHistoryRepository {
    override fun read(metricId: String): MetricHistory = when (metricId) {
        "battery" -> {
            val now = System.currentTimeMillis()
            val raw = JSONArray(storage.getPrefs().getString("battery_hourly_log", "[]") ?: "[]")
            val samples = (0 until raw.length()).map { index ->
                val sample = raw.getJSONObject(index)
                BatteryLevelSample(sample.getLong("t"), sample.getInt("l"))
            }.filter { it.timestamp in (now - 7_200_000L)..now && it.level in 0..100 }
                .sortedBy { it.timestamp }
            val live = if (storage.isCharging() && storage.getLastChargeStartTime() > 0) {
                (now - storage.getLastChargeStartTime()).coerceAtLeast(0)
            } else 0L
            MetricHistory.Battery(storage.getCurrentBatteryLevel(), storage.getBatteryStartLevel(),
                storage.getTodayBatteryUsed(), storage.getTodayChargingTime() + live,
                storage.getScreenOnDrainRate(), samples, storage.getTodayChargingSessions(),
                storage.getStackedBatteryHistoryByMonth())
        }
        "unlock" -> MetricHistory.Unlock(
            storage.getTodayUnlockCount(), storage.getTodayActualUsageCount(),
            storage.getTodayActualUsageDuration(), storage.getTodayQuickGlanceCount(),
            storage.getTodayQuickGlanceDuration(), storage.getTodayLockScreenPeekCount(),
            storage.getTodayUnlockFingerprint(), storage.getTodayUnlockFace(),
            storage.getTodayUnlockPin(), storage.getTodayUnlockSessions())
        "call_in", "call_out" -> {
            val now = System.currentTimeMillis()
            val records = calls.read(storage.getCycleStartTimestamp(now), now).filter {
                if (metricId == "call_in") it.type in setOf(CallLog.Calls.INCOMING_TYPE,
                    CallLog.Calls.MISSED_TYPE, CallLog.Calls.REJECTED_TYPE)
                else it.type == CallLog.Calls.OUTGOING_TYPE
            }
            MetricHistory.Calls(records)
        }
        else -> MetricHistory.Radio(storage.getStackedHistoryByMonth(metricId).values.flatten()
            .map { MetricDayRecord(it.first.take(10), it.second) }.sortedByDescending { it.date })
    }
}
