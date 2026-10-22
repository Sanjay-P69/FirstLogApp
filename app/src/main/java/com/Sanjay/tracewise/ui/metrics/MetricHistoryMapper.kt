package com.sanjay.tracewise.ui.metrics

import android.content.res.Resources
import android.provider.CallLog
import com.sanjay.tracewise.R
import com.sanjay.tracewise.core.formatDurationMillis
import com.sanjay.tracewise.data.model.CallRecord
import com.sanjay.tracewise.data.model.MetricHistory
import com.sanjay.tracewise.ui.charts.ChartAxisLabel
import com.sanjay.tracewise.ui.charts.ChartPoint
import com.sanjay.tracewise.ui.charts.ChartStyle
import java.text.SimpleDateFormat
import java.util.Locale

internal fun MetricHistory.toUiState(title: String, resources: Resources): MetricHistoryUiState {
    fun text(id: Int) = resources.getString(id)
    fun number(id: Int, value: Int) = HistoryRow(text(id), value.toString())
    fun duration(id: Int, value: Long) = HistoryRow(text(id), formatDurationMillis(value))
    val date = SimpleDateFormat("EEE d MMM · HH:mm", Locale.getDefault())
    val time = SimpleDateFormat("HH:mm", Locale.getDefault())
    return when (this) {
        is MetricHistory.Radio -> MetricHistoryUiState(
            title,
            sections = days.groupBy { it.date.take(7) }.map { (month, entries) ->
                HistorySection(
                    month,
                    entries.map {
                        HistoryRow(
                            it.date,
                            formatDurationMillis(it.durationMillis)
                        )
                    },
                    expandable = true
                )
            }
        )
        is MetricHistory.Battery -> {
            val first = samples.firstOrNull()?.timestamp ?: 0L
            val span = ((samples.lastOrNull()?.timestamp ?: first) - first).coerceAtLeast(1)
            val plot = samples.takeIf { it.isNotEmpty() }?.let { values ->
                HistoryPlot(
                    text(R.string.battery_recent_plot),
                    values.map { sample ->
                        ChartPoint(
                            time.format(sample.timestamp),
                            date.format(sample.timestamp),
                            sample.level.toDouble(),
                            "${sample.level}%",
                            position = if (values.size == 1) {
                                0.5f
                            } else {
                                (sample.timestamp - first).toFloat() / span
                            }
                        )
                    },
                    ChartStyle.LINE,
                    maximum = 100.0,
                    isPercentage = true,
                    axisLabels = if (values.size == 1) {
                        listOf(ChartAxisLabel(time.format(first), 0.5f))
                    } else {
                        (0..4).map { tick ->
                            ChartAxisLabel(time.format(first + span * tick / 4), tick / 4f)
                        }
                    }
                )
            }
            val summary = listOf(
                HistoryRow(
                    text(R.string.battery_level),
                    if (level >= 0) "$level%" else text(R.string.chart_no_record)
                ),
                HistoryRow(
                    text(R.string.battery_cycle_start),
                    if (startLevel >= 0) "$startLevel%" else text(R.string.chart_no_record)
                ),
                HistoryRow(text(R.string.battery_used_points), used.toString()),
                duration(R.string.charging_duration, chargingMillis),
                HistoryRow(
                    text(R.string.screen_drain_rate),
                    String.format(Locale.getDefault(), "%.1f %%/h", drainPerHour)
                )
            )
            val charging = HistorySection(
                text(R.string.charging_sessions),
                sessions.map { session ->
                    val interval = time.format(session.startTime) + " – " +
                        time.format(session.endTime)
                    HistoryRow(
                        interval,
                        formatDurationMillis(session.durationMs),
                        "${session.startLevel}% → ${session.endLevel}%"
                    )
                }.ifEmpty { listOf(HistoryRow(text(R.string.no_charging_sessions), "")) },
                expandable = true
            )
            val archived = days.map { (month, records) ->
                HistorySection(
                    month,
                    records.map { record ->
                        HistoryRow(
                            record.date.take(10),
                            resources.getString(
                                R.string.battery_day_value,
                                record.batteryUsedPct,
                                formatDurationMillis(record.chargeDurationMs)
                            )
                        )
                    },
                    expandable = true
                )
            }
            val overview = HistorySection(text(R.string.cycle_overview), summary)
            MetricHistoryUiState(
                title,
                sections = listOf(overview, charging) + archived,
                plot = plot
            )
        }
        is MetricHistory.Unlock -> {
            val counts = sessionHistogram(sessions.map { it.durationMs })
            val shortLabels = listOf(
                R.string.histogram_short_ten,
                R.string.histogram_short_minute,
                R.string.histogram_short_five,
                R.string.histogram_short_long
            ).map(::text)
            val bins = listOf(
                R.string.histogram_under_ten,
                R.string.histogram_under_minute,
                R.string.histogram_under_five,
                R.string.histogram_over_five
            )
            val plot = sessions.takeIf { it.isNotEmpty() }?.let {
                HistoryPlot(
                    resources.getString(R.string.session_distribution, it.size),
                    counts.mapIndexed { index, count ->
                        ChartPoint(
                            shortLabels[index],
                            text(bins[index]),
                            count.toDouble(),
                            resources.getString(R.string.session_count, count)
                        )
                    },
                    ChartStyle.BARS
                )
            }
            val summary = listOf(
                number(R.string.unlock_total, count),
                number(R.string.active_session_threshold, activeCount),
                duration(R.string.active_session_duration, activeMillis),
                number(R.string.quick_glance_threshold, glanceCount),
                duration(R.string.quick_glance_duration, glanceMillis),
                number(R.string.lockscreen_peeks, peekCount),
                HistoryRow(
                    text(R.string.estimated_unlock_methods),
                    resources.getString(R.string.unlock_method_counts, fingerprint, face, pin),
                    text(R.string.unlock_estimate_hint)
                )
            )
            val recent = HistorySection(
                text(R.string.recent_sessions),
                sessions.sortedByDescending { it.timestamp }.map {
                    val ended = resources.getString(
                        R.string.session_ended,
                        date.format(it.timestamp)
                    )
                    HistoryRow(ended, formatDurationMillis(it.durationMs))
                },
                expandable = true
            )
            MetricHistoryUiState(
                title,
                sections = listOf(HistorySection(text(R.string.cycle_overview), summary), recent),
                plot = plot
            )
        }
        is MetricHistory.Calls -> {
            val connected = records.filter {
                it.type == CallLog.Calls.INCOMING_TYPE ||
                    (it.type == CallLog.Calls.OUTGOING_TYPE && it.durationSeconds > 0)
            }
            val missed = records.filter { it !in connected }
            fun rows(calls: List<CallRecord>) = calls.map {
                val kind = when (it.type) {
                    CallLog.Calls.MISSED_TYPE -> text(R.string.call_missed)
                    CallLog.Calls.REJECTED_TYPE -> text(R.string.call_rejected)
                    else -> text(R.string.call_recorded)
                }
                HistoryRow(
                    it.name.ifBlank { text(R.string.unknown_caller) },
                    formatDurationMillis(it.durationSeconds * 1_000),
                    "${date.format(it.timestamp)} · $kind",
                    it.number.takeIf(String::isNotBlank)
                )
            }
            val callers = records.groupBy { it.number }.values.sortedByDescending { calls ->
                calls.sumOf { it.durationSeconds }
            }.map { calls ->
                val caller = calls.first()
                HistoryRow(
                    caller.name.ifBlank { text(R.string.unknown_caller) },
                    formatDurationMillis(calls.sumOf { it.durationSeconds } * 1_000),
                    resources.getString(R.string.call_count, calls.size),
                    caller.number.takeIf(String::isNotBlank)
                )
            }
            val connectedMillis = connected.sumOf { it.durationSeconds } * 1_000
            MetricHistoryUiState(
                title,
                sections = listOf(
                    HistorySection(
                        text(R.string.cycle_overview),
                        listOf(
                            number(R.string.connected_calls, connected.size),
                            duration(R.string.connected_duration, connectedMillis),
                            number(R.string.missed_calls, missed.size)
                        )
                    ),
                    HistorySection(
                        text(R.string.connected_calls),
                        rows(connected).ifEmpty {
                            listOf(HistoryRow(text(R.string.no_calls_recorded), ""))
                        },
                        expandable = true
                    ),
                    HistorySection(
                        text(R.string.missed_calls),
                        rows(missed).ifEmpty {
                            listOf(HistoryRow(text(R.string.no_calls_recorded), ""))
                        },
                        expandable = true
                    ),
                    HistorySection(text(R.string.top_callers), callers, expandable = true)
                )
            )
        }
    }
}

internal fun sessionHistogram(durations: List<Long>): List<Int> = listOf(
    durations.count { it in 0L until 10_000L },
    durations.count { it in 10_000L until 60_000L },
    durations.count { it in 60_000L until 300_000L },
    durations.count { it >= 300_000 }
)
