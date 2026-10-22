package com.Sanjay.firstlogapp.ui.dashboard

import com.Sanjay.firstlogapp.ui.model.MetricItem

/** Presentation-only contract; fake this in previews without Android services. */
data class DashboardUiState(
    val date: String,
    val screen: MetricItem,
    val battery: MetricItem,
    val connections: List<MetricItem>,
    val calls: List<MetricItem>,
    val unlocks: MetricItem,
    val appUsage: String,
    val apps: List<AppUsageUiItem> = emptyList(),
    val activeNotifications: Int = 0,
    val notificationHistory: Int = 0,
    val canReadCalls: Boolean = false,
    val canReadBluetooth: Boolean = false,
    val canReadApps: Boolean = false,
    val canReadNotifications: Boolean = false,
    val isLoading: Boolean = false,
    val hasLoadedData: Boolean = true,
    val hasError: Boolean = false
)

data class AppUsageUiItem(val id: String, val name: String, val duration: String, val launches: Int = 0,
    val foreground: String = "", val background: String = "", val lastSession: String = "")

sealed interface DashboardAction {
    data class OpenMetric(val metric: MetricItem) : DashboardAction
    data object Refresh : DashboardAction
    data object OpenSummary : DashboardAction
    data object OpenApps : DashboardAction
    data object OpenNotifications : DashboardAction
    data object GrantCalls : DashboardAction
    data object GrantBluetooth : DashboardAction
    data object GrantUsage : DashboardAction
    data object GrantNotifications : DashboardAction
}
