package com.Sanjay.firstlogapp.ui.analytics

import com.Sanjay.firstlogapp.core.formatDurationMillis
import com.Sanjay.firstlogapp.data.model.AnalyticsSeries
import com.Sanjay.firstlogapp.data.model.BucketQuality
import com.Sanjay.firstlogapp.ui.charts.ChartPoint
import com.Sanjay.firstlogapp.ui.charts.ChartPointState
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

internal fun AnalyticsSeries.toUiState(period: String, locale: Locale = Locale.getDefault()):
    AnalyticsUiState {
    val zone = TimeZone.getTimeZone(timezoneId)
    val shortFormat = SimpleDateFormat(when (period) {
        "Day" -> "HH"
        "Month" -> "d"
        else -> "MMM"
    }, locale).apply { timeZone = zone }
    val detailFormat = SimpleDateFormat(if (period == "Day") "EEE d MMM · HH:mm z"
        else if (period == "Month") "EEE d MMM yyyy" else "MMMM yyyy", locale)
        .apply { timeZone = zone }
    return AnalyticsUiState(
        points = buckets.map { bucket ->
            ChartPoint(shortFormat.format(bucket.startMillis),
                detailFormat.format(bucket.startMillis),
                bucket.durationMillis?.div(3_600_000.0),
                bucket.durationMillis?.let(::formatDurationMillis).orEmpty(),
                when (bucket.quality) {
                    BucketQuality.RECORDED -> ChartPointState.RECORDED
                    BucketQuality.PARTIAL -> ChartPointState.PARTIAL
                    BucketQuality.NO_RECORD -> ChartPointState.NO_RECORD
                    BucketQuality.FUTURE -> ChartPointState.FUTURE
                })
        },
        totalMillis = buckets.sumOf { it.durationMillis ?: 0L },
        usesTrackingCycles = usesTrackingCycles
    )
}
