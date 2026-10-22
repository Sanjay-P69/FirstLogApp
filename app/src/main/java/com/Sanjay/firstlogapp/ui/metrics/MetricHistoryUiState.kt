package com.Sanjay.firstlogapp.ui.metrics

import com.Sanjay.firstlogapp.ui.analytics.AnalyticsUiState
import com.Sanjay.firstlogapp.ui.charts.ChartAxisLabel
import com.Sanjay.firstlogapp.ui.charts.ChartPoint
import com.Sanjay.firstlogapp.ui.charts.ChartStyle

data class HistoryRow(
    val title: String,
    val value: String,
    val supporting: String? = null,
    val dialNumber: String? = null
)

data class HistorySection(
    val title: String,
    val rows: List<HistoryRow>,
    val expandable: Boolean = false
)

data class HistoryPlot(
    val title: String,
    val points: List<ChartPoint>,
    val style: ChartStyle,
    val maximum: Double? = null,
    val isPercentage: Boolean = false,
    val axisLabels: List<ChartAxisLabel> = emptyList()
)

data class MetricHistoryUiState(
    val title: String,
    val sections: List<HistorySection> = emptyList(),
    val plot: HistoryPlot? = null,
    val analytics: AnalyticsUiState? = null,
    val isLoading: Boolean = false,
    val hasError: Boolean = false,
    val accessDenied: Boolean = false
)
