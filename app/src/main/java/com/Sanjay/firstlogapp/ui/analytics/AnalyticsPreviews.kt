package com.Sanjay.firstlogapp.ui.analytics

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.Sanjay.firstlogapp.core.formatDurationMillis
import com.Sanjay.firstlogapp.ui.charts.ChartPoint
import com.Sanjay.firstlogapp.ui.theme.FirstLogAppTheme

private fun previewState(count: Int) = AnalyticsUiState(
    points = (0 until count).map { index ->
        val hours = if (index in 8..18) (index % 4 + 1) / 4.0 else 0.0
        ChartPoint(index.toString(), "Period $index", hours,
            formatDurationMillis((hours * 3_600_000).toLong()))
    }, totalMillis = 8 * 3_600_000L)

@Preview(name = "All 24 hours", widthDp = 360, heightDp = 760)
@Preview(name = "Compact dark", widthDp = 320, heightDp = 720,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Large text", widthDp = 360, heightDp = 900, fontScale = 1.5f)
@Composable
private fun AnalyticsDayPreview() {
    FirstLogAppTheme(dynamicColor = false) {
        AnalyticsContent(previewState(24), "screen", "Day", {}, {})
    }
}

@Preview(name = "31 days", widthDp = 360, heightDp = 760)
@Composable
private fun AnalyticsMonthPreview() {
    FirstLogAppTheme(dynamicColor = false) {
        AnalyticsContent(previewState(31), "screen", "Month", {}, {})
    }
}
