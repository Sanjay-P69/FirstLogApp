package com.Sanjay.firstlogapp.data.repository

import android.provider.CallLog
import com.Sanjay.firstlogapp.data.device.CallHistoryReader
import com.Sanjay.firstlogapp.data.local.DataManager
import com.Sanjay.firstlogapp.data.model.AnalyticsSeries
import com.Sanjay.firstlogapp.data.model.MetricTimeSnapshot
import org.json.JSONArray
import java.util.TimeZone

interface AnalyticsRepository {
    fun readChart(metricId: String, period: String): AnalyticsSeries
}

class StoredAnalyticsRepository(
    private val storage: DataManager,
    private val calls: CallHistoryReader,
    private val clock: () -> Long = System::currentTimeMillis,
    private val zone: TimeZone = TimeZone.getDefault()
) : AnalyticsRepository {
    override fun readChart(metricId: String, period: String): AnalyticsSeries {
        require(period in setOf("Day", "Month", "Year"))
        val now = clock()
        if (metricId == "call_in" || metricId == "call_out") {
            val windows = timeWindows(now, period, zone)
            val records = calls.read(windows.first().first, windows.last().second).filter {
                if (metricId == "call_in") it.type == CallLog.Calls.INCOMING_TYPE
                else it.type == CallLog.Calls.OUTGOING_TYPE
            }
            return AnalyticsSeries(callBuckets(records, now, period, zone), zone.id)
        }
        require(metricId in setOf("screen", "wifi", "data", "bluetooth", "gps", "both_wifi_data"))
        // Read a preference snapshot without invoking rollover or writing from presentation.
        val prefs = storage.getPrefs().all
        val prefix = "hourly_${metricId}_"
        val hourly = prefs.filterKeys { it.startsWith(prefix) }
            .mapKeys { it.key.removePrefix(prefix) }.mapValues { (it.value as Number).toLong() }
        val history = JSONArray(prefs["history_records_json"] as? String ?: "[]")
        val daily = buildMap {
            for (index in 0 until history.length()) {
                val record = history.getJSONObject(index)
                if (record.has(metricId)) put(record.getString("date"), record.getLong(metricId))
            }
        }.toMutableMap()
        val start = (prefs["last_on_time_$metricId"] as? Long)?.takeIf {
            prefs["metric_active_$metricId"] == true && it > 0L && it <= now
        }
        val cycle = prefs["current_cycle_date"] as? String
        if (cycle != null) {
            daily[cycle] = (prefs["today_$metricId"] as? Long ?: 0L) +
                if (start != null) (now - start) else 0L
        }
        return AnalyticsSeries(metricBuckets(MetricTimeSnapshot(hourly, daily, start),
            now, period, zone), zone.id, usesTrackingCycles = period != "Day")
    }
}
