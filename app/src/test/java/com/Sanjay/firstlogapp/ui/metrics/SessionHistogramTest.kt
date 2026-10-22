package com.Sanjay.firstlogapp.ui.metrics

import org.junit.Assert.assertEquals
import org.junit.Test

class SessionHistogramTest {
    @Test
    fun binBoundariesCountEachValidSessionExactlyOnce() {
        val values = listOf(0L, 9_999L, 10_000L, 59_999L, 60_000L, 299_999L, 300_000L)
        val counts = sessionHistogram(values)
        assertEquals(listOf(2, 2, 2, 1), counts)
        assertEquals(values.size, counts.sum())
    }
}
