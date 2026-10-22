package com.Sanjay.firstlogapp.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
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
    BackHandler(enabled = showApps || selectedTab != 0) {
        if (showApps) showApps = false else selectedTab = 0
    }
    if (showApps) {
        AppUsageContent(state.apps, onBack = { showApps = false })
    } else {
        BoxWithConstraints {
            val expanded = maxWidth >= 600.dp
            Scaffold(bottomBar = {
                if (!expanded) M3BottomNavigationBar(selectedTab, { selectedTab = it })
            }) { padding ->
                Row(Modifier.padding(padding)) {
                    if (expanded) M3NavigationRail(selectedTab, { selectedTab = it })
                    Box(Modifier.weight(1f)) {
                        when (selectedTab) {
                            0 -> DashboardRoute(state, dataManager, { showApps = true }, onRefresh, currentThumbnailStyle)
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
