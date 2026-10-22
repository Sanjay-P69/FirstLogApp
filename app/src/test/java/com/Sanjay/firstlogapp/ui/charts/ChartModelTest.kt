package com.Sanjay.firstlogapp.ui.charts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartModelTest {
    @Test
    fun sparseAxisTicksCoverTheWholeRangeAtEveryPeriodSize() {
        listOf(12, 24, 30, 31).forEach { count ->
            val ticks = tickIndices(count)
            assertEquals(0, ticks.first())
            assertEquals(count - 1, ticks.last())
            assertTrue(ticks.size <= 5)
        }
    }

    @Test
    fun scaleIncludesLargeValuesAndZeroDataHasAFiniteScale() {
        assertTrue(chartMaximum(listOf(8.0, 12.0, 180.0)) >= 180.0)
        assertTrue(chartMaximum(listOf(0.0, null)).isFinite())
        assertTrue(chartMaximum(listOf(0.0, null)) > 0)
    }
}
