package com.Sanjay.firstlogapp.data.repository

import com.Sanjay.firstlogapp.data.model.BucketQuality
import com.Sanjay.firstlogapp.data.model.CallRecord
import com.Sanjay.firstlogapp.data.model.MetricTimeSnapshot
import java.text.SimpleDateFormat
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeBucketAggregationTest {
    private val utc = TimeZone.getTimeZone("UTC")
    private fun instant(value: String, zone: TimeZone = utc): Long =
        SimpleDateFormat("yyyy-MM-dd HH:mm").apply { timeZone = zone }.parse(value)!!.time

    @Test
    fun aFullDayFitsAllHoursWithoutInventingActivityDuringSleep() {
        val readings = (0..23).associate { hour ->
            "2026-10-22_$hour" to if (hour in 9..16) 3_600_000L else 0L
        }
        val result = metricBuckets(
            MetricTimeSnapshot(readings, emptyMap()),
            instant("2026-10-22 23:59"),
            "Day",
            utc
        )
        assertEquals(24, result.size)
        assertEquals(8 * 3_600_000L, result.sumOf { it.durationMillis ?: 0L })
        assertEquals(0L, result.first().durationMillis)
        assertEquals(0L, result[20].durationMillis)
    }

    @Test
    fun missingAndFutureHoursAreNotRecordedZeroes() {
        val result = metricBuckets(
            MetricTimeSnapshot(mapOf("2026-10-22_8" to 0L), emptyMap()),
            instant("2026-10-22 12:30"),
            "Day",
            utc
        )
        assertEquals(BucketQuality.NO_RECORD, result[7].quality)
        assertNull(result[7].durationMillis)
        assertEquals(0L, result[8].durationMillis)
        assertEquals(BucketQuality.FUTURE, result[23].quality)
        assertNull(result[23].durationMillis)
    }

    @Test
    fun liveActivityIsSplitByRealHourBoundaries() {
        val snapshot = MetricTimeSnapshot(emptyMap(), emptyMap(), instant("2026-10-22 10:30"))
        val result = metricBuckets(snapshot, instant("2026-10-22 12:15"), "Day", utc)
        assertEquals(1_800_000L, result[10].durationMillis)
        assertEquals(3_600_000L, result[11].durationMillis)
        assertEquals(900_000L, result[12].durationMillis)
    }

    @Test
    fun calendarMonthAndYearKeepAllCategoriesIncludingFutureOnes() {
        val now = instant("2026-10-22 12:00")
        val month = metricBuckets(MetricTimeSnapshot(emptyMap(), emptyMap()), now, "Month", utc)
        assertEquals(31, month.size)
        assertEquals(BucketQuality.FUTURE, month.last().quality)
        assertEquals(12, timeWindows(now, "Year", utc).size)
    }

    @Test
    fun callsCrossingAnHourAreSplitInsteadOfAssignedToTheirStartHour() {
        val call = CallRecord("Fixture", "", instant("2026-10-22 10:50"), 1_200)
        val result = callBuckets(listOf(call), instant("2026-10-22 12:00"), "Day", utc)
        assertEquals(600_000L, result[10].durationMillis)
        assertEquals(600_000L, result[11].durationMillis)
    }

    @Test
    fun dstDaysKeepTheirActualLengthAndAmbiguousLegacyHoursAreNotDuplicated() {
        val zone = TimeZone.getTimeZone("America/New_York")
        assertEquals(23, timeWindows(instant("2026-03-08 12:00", zone), "Day", zone).size)
        val now = instant("2026-11-01 12:00", zone)
        val result = metricBuckets(
            MetricTimeSnapshot(
                mapOf("2026-11-01_1" to 3_600_000L),
                emptyMap()
            ),
            now,
            "Day",
            zone
        )
        assertEquals(25, result.size)
        assertTrue(result.all { it.durationMillis == null })
    }
}
