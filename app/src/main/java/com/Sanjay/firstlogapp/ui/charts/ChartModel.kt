package com.Sanjay.firstlogapp.ui.charts

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

enum class ChartPointState { RECORDED, PARTIAL, NO_RECORD, FUTURE }
enum class ChartStyle { BARS, LINE }

data class ChartAxisLabel(val label: String, val position: Float)

data class ChartPoint(
    val label: String,
    val description: String,
    val value: Double?,
    val valueLabel: String,
    val state: ChartPointState = ChartPointState.RECORDED,
    val position: Float? = null
)

/** Scale follows observations; zero never receives a fabricated minimum bar height. */
internal fun chartMaximum(values: List<Double?>): Double {
    val maximum = values.filterNotNull().filter { it.isFinite() && it > 0 }.maxOrNull() ?: 1.0
    val magnitude = 10.0.pow(floor(log10(maximum)))
    return ceil(maximum / magnitude) * magnitude
}

internal fun tickIndices(count: Int): List<Int> =
    if (count < 2) listOf(0) else (0..4).map { it * (count - 1) / 4 }.distinct()
