package com.Sanjay.firstlogapp.data.model

/** Values remain in milliseconds until presentation. Absence is distinct from measured zero. */
enum class BucketQuality { RECORDED, PARTIAL, NO_RECORD, FUTURE }

data class AnalyticsBucket(
    val startMillis: Long,
    val endMillis: Long,
    val durationMillis: Long?,
    val quality: BucketQuality
)

data class AnalyticsSeries(
    val buckets: List<AnalyticsBucket>,
    val timezoneId: String,
    val usesTrackingCycles: Boolean = false
)

data class MetricTimeSnapshot(
    val hourly: Map<String, Long>,
    val daily: Map<String, Long>,
    val liveStartMillis: Long? = null
)
