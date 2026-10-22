package com.sanjay.tracewise.ui.metrics

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.sanjay.tracewise.ui.charts.ChartPoint
import com.sanjay.tracewise.ui.charts.ChartStyle
import com.sanjay.tracewise.ui.theme.FirstLogAppTheme

@Preview(name = "Native metric history", widthDp = 360, heightDp = 800)
@Preview(
    name = "History dark large text",
    widthDp = 320,
    heightDp = 900,
    fontScale = 1.5f,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun MetricHistoryPreview() {
    FirstLogAppTheme(dynamicColor = false) {
        MetricHistoryContent(
            MetricHistoryUiState(
                "Battery",
                sections = listOf(
                    HistorySection(
                        "Current tracking cycle",
                        listOf(
                            HistoryRow("Current level", "72%"),
                            HistoryRow("Charging duration", "1h 20m")
                        )
                    ),
                    HistorySection(
                        "Charging sessions",
                        listOf(HistoryRow("09:00 – 10:20", "1h 20m")),
                        expandable = true
                    )
                ),
                plot = HistoryPlot(
                    "Battery level · last two hours",
                    listOf(
                        ChartPoint("09:00", "09:00", 45.0, "45%", position = 0f),
                        ChartPoint("10:00", "10:00", 70.0, "70%", position = 0.5f),
                        ChartPoint("11:00", "11:00", 72.0, "72%", position = 1f)
                    ),
                    ChartStyle.LINE,
                    100.0,
                    true
                )
            ),
            {},
            {},
            {}
        )
    }
}
