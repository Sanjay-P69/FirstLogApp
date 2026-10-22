package com.sanjay.tracewise.ui.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.sanjay.tracewise.ui.model.MetricItem
import com.sanjay.tracewise.ui.theme.FirstLogAppTheme

private val previewState = DashboardUiState(
    date = "Thursday, October 22",
    screen = MetricItem("screen", "Screen time", "2h 18m", "8 sessions · Active"),
    battery = MetricItem("battery", "Battery used", "24%", "1 charging session"),
    connections = listOf(
        MetricItem("wifi", "Wi-Fi", "4h 12m", "3 sessions · Active"),
        MetricItem("data", "Mobile data", "38m", "2 sessions · Inactive")
    ),
    calls = listOf(MetricItem("call_in", "Incoming calls", "3 calls", "12m connected")),
    unlocks = MetricItem("unlock", "Unlocks", "28", "21 active · 7 quick glances"),
    appUsage = "2h 04m", apps = listOf(AppUsageUiItem("example", "Calendar", "24m")),
    activeNotifications = 4, notificationHistory = 32,
    canReadCalls = true, canReadApps = true, canReadNotifications = true
)

@Preview(name = "Compact light", widthDp = 360, heightDp = 840, showBackground = true)
@Preview(
    name = "Large text",
    widthDp = 320,
    heightDp = 840,
    fontScale = 1.5f,
    showBackground = true
)
@Preview(name = "Tablet", widthDp = 840, heightDp = 900, showBackground = true)
@Composable
private fun DashboardPreview() {
    FirstLogAppTheme(dynamicColor = false) { DashboardContent(previewState, {}) }
}

@Preview(name = "Compact dark", widthDp = 360, heightDp = 840, showBackground = true)
@Composable
private fun DashboardDarkPreview() {
    FirstLogAppTheme(darkTheme = true, dynamicColor = false) { DashboardContent(previewState, {}) }
}
