package com.Sanjay.firstlogapp.data.repository

import com.Sanjay.firstlogapp.data.model.AnalyticsBucket
import com.Sanjay.firstlogapp.data.model.BucketQuality
import com.Sanjay.firstlogapp.data.model.CallRecord
import com.Sanjay.firstlogapp.data.model.MetricTimeSnapshot
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/** Calendar arithmetic preserves 23/25-hour DST days instead of forcing 24 observations. */
internal fun timeWindows(now: Long, period: String, zone: TimeZone): List<Pair<Long, Long>> {
    val calendar = Calendar.getInstance(zone).apply {
        timeInMillis = now
        set(Calendar.MILLISECOND, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.HOUR_OF_DAY, 0)
        if (period == "Month" || period == "Year") set(Calendar.DAY_OF_MONTH, 1)
        if (period == "Year") set(Calendar.MONTH, Calendar.JANUARY)
    }
    val end = (calendar.clone() as Calendar).apply {
        add(
            when (period) {
                "Day" -> Calendar.DAY_OF_MONTH
                "Month" -> Calendar.MONTH
                else -> Calendar.YEAR
            },
            1
        )
    }.timeInMillis
    val step = when (period) {
        "Day" -> Calendar.HOUR_OF_DAY
        "Month" -> Calendar.DAY_OF_MONTH
        else -> Calendar.MONTH
    }
    return buildList {
        while (calendar.timeInMillis < end) {
            val start = calendar.timeInMillis
            calendar.add(step, 1)
            add(start to calendar.timeInMillis)
        }
    }
}

internal fun metricBuckets(
    snapshot: MetricTimeSnapshot,
    now: Long,
    period: String,
    zone: TimeZone
): List<AnalyticsBucket> {
    val date = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply { timeZone = zone }
    val month = SimpleDateFormat("yyyy-MM", Locale.ROOT).apply { timeZone = zone }
    val windows = timeWindows(now, period, zone)
    val hourKeys = windows.map { (start, _) ->
        val cal = Calendar.getInstance(zone).apply { timeInMillis = start }
        "${date.format(start)}_${cal.get(Calendar.HOUR_OF_DAY)}"
    }
    return windows.mapIndexed { index, (start, end) ->
        if (start > now) return@mapIndexed AnalyticsBucket(start, end, null, BucketQuality.FUTURE)
        val recorded = when (period) {
            // Legacy storage merges repeated DST hours. Do not duplicate their combined value.
            "Day" -> if (hourKeys.count { it == hourKeys[index] } > 1) {
                null
            } else {
                snapshot.hourly[hourKeys[index]]
            }
            "Month" -> snapshot.daily[date.format(start)]
            else -> snapshot.daily.filterKeys { it.startsWith(month.format(start)) }
                .values.takeIf { it.isNotEmpty() }?.sum()
        }
        val live = if (period == "Day" && snapshot.liveStartMillis != null) {
            (minOf(end, now) - maxOf(start, snapshot.liveStartMillis)).coerceAtLeast(0)
        } else {
            0L
        }
        val value = if (recorded != null || live > 0L) (recorded ?: 0L) + live else null
        val quality = when {
            value == null -> BucketQuality.NO_RECORD
            end > now || period == "Year" -> BucketQuality.PARTIAL
            else -> BucketQuality.RECORDED
        }
        AnalyticsBucket(start, end, value, quality)
    }
}

internal fun callBuckets(
    calls: List<CallRecord>,
    now: Long,
    period: String,
    zone: TimeZone
): List<AnalyticsBucket> = timeWindows(now, period, zone).map { (start, end) ->
    if (start > now) {
        AnalyticsBucket(start, end, null, BucketQuality.FUTURE)
    } else {
        val duration = calls.sumOf { call ->
            val callEnd = call.timestamp + call.durationSeconds.coerceAtLeast(0) * 1_000L
            (minOf(end, now, callEnd) - maxOf(start, call.timestamp)).coerceAtLeast(0)
        }
        AnalyticsBucket(
            start,
            end,
            duration,
            if (end > now) BucketQuality.PARTIAL else BucketQuality.RECORDED
        )
    }
}

/** Legacy preference dates were formatted in the device locale; read without rewriting keys. */
internal fun normalizedDateKey(value: String): String = value.map { character ->
    val digit = Character.digit(character, 10)
    if (digit >= 0) ('0'.code + digit).toChar() else character
}.joinToString("")
