package com.Sanjay.firstlogapp.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.Sanjay.firstlogapp.ui.components.M3NavigationRail
import com.Sanjay.firstlogapp.data.local.DataManager
import com.Sanjay.firstlogapp.ui.analytics.AnalyticsScreen
import com.Sanjay.firstlogapp.ui.apps.AppUsageContent
import com.Sanjay.firstlogapp.ui.components.M3BottomNavigationBar
import com.Sanjay.firstlogapp.ui.dashboard.DashboardRoute
import com.Sanjay.firstlogapp.ui.dashboard.DashboardUiState
import com.Sanjay.firstlogapp.ui.settings.SettingsScreen
import com.Sanjay.firstlogapp.ui.metrics.MetricHistoryRoute

/** Root Android adapters; feature Content composables only take state and actions. */
@Composable
fun MainNavigationHost(
    state: DashboardUiState,
    dataManager: DataManager,
    currentThemeMode: String,
    onThemeChange: (String) -> Unit,
    currentThumbnailStyle: String,
    onThumbnailStyleChange: (String) -> Unit,
    onForceResetToday: () -> Unit,
    onRefresh: suspend () -> Unit
) {
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var showApps by rememberSaveable { mutableStateOf(false) }
    var historyId by rememberSaveable { mutableStateOf<String?>(null) }
    val historyMetric = (listOf(state.screen, state.battery, state.unlocks) +
        state.connections + state.calls).firstOrNull { it.id == historyId }
    BackHandler(enabled = historyId != null || showApps || selectedTab != 0) {
        if (historyId != null) {
            historyId = null
            return@BackHandler
        }
        if (showApps) showApps = false else selectedTab = 0
    }
    if (historyMetric != null) {
        MetricHistoryRoute(historyMetric, dataManager) { historyId = null }
    } else if (showApps) {
        AppUsageContent(state.apps, onBack = { showApps = false })
    } else {
        BoxWithConstraints {
            val expanded = maxWidth >= 600.dp
            Scaffold(bottomBar = {
                if (!expanded) M3BottomNavigationBar(selectedTab, { selectedTab = it })
            }) { padding ->
                Row(Modifier.padding(padding).consumeWindowInsets(padding)) {
                    if (expanded) M3NavigationRail(selectedTab, { selectedTab = it })
                    Box(Modifier.weight(1f)) {
                        when (selectedTab) {
                            0 -> DashboardRoute(state, dataManager, { showApps = true }, onRefresh,
                                currentThumbnailStyle, onOpenHistory = { historyId = it })
                            1 -> AnalyticsScreen(dataManager)
                            2 -> SettingsScreen(
                                dataManager, currentThemeMode, onThemeChange,
                                currentThumbnailStyle, onThumbnailStyleChange, onForceResetToday
                            )
                        }
                    }
                }
            }
        }
    }
}
