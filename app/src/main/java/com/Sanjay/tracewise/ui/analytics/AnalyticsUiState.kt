package com.sanjay.tracewise.ui.analytics

import com.sanjay.tracewise.ui.charts.ChartPoint

data class AnalyticsUiState(
    val points: List<ChartPoint> = emptyList(),
    val totalMillis: Long = 0,
    val isLoading: Boolean = false,
    val hasError: Boolean = false,
    val accessDenied: Boolean = false,
    val usesTrackingCycles: Boolean = false
)
