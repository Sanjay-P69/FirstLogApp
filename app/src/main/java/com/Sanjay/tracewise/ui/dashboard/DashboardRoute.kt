package com.sanjay.tracewise.ui.dashboard

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.sanjay.tracewise.data.local.DataManager
import com.sanjay.tracewise.ui.components.M3MetricDetailBottomSheet
import com.sanjay.tracewise.ui.model.MetricItem
import com.sanjay.tracewise.ui.notifications.NotificationDetailDialog
import com.sanjay.tracewise.ui.summary.AiSummaryDialog
import kotlinx.coroutines.launch

/** Android integration and legacy detail adapters. DashboardContent stays platform independent. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardRoute(
    state: DashboardUiState,
    dataManager: DataManager,
    onNavigateToAppUsage: () -> Unit,
    onRefresh: suspend () -> Unit,
    thumbnailStyle: String,
    onOpenHistory: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedMetric by remember { mutableStateOf<MetricItem?>(null) }
    var showNotifications by remember { mutableStateOf(false) }
    var showSummary by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    val permissions =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            scope.launch { onRefresh() }
        }
    PullToRefreshBox(isRefreshing = refreshing, onRefresh = {
        scope.launch {
            refreshing = true
            try {
                onRefresh()
            } finally {
                refreshing = false
            }
        }
    }) {
        DashboardContent(state, onAction = { action ->
            when (action) {
                is DashboardAction.OpenMetric -> selectedMetric = action.metric
                DashboardAction.Refresh -> scope.launch { onRefresh() }
                DashboardAction.OpenSummary -> showSummary = true
                DashboardAction.OpenApps -> onNavigateToAppUsage()
                DashboardAction.OpenNotifications -> showNotifications = true
                DashboardAction.GrantCalls -> permissions.launch(
                    arrayOf(Manifest.permission.READ_CALL_LOG, Manifest.permission.READ_CONTACTS)
                )
                DashboardAction.GrantBluetooth -> if (Build.VERSION.SDK_INT >=
                    31
                ) {
                    permissions.launch(arrayOf(Manifest.permission.BLUETOOTH_CONNECT))
                }
                DashboardAction.GrantUsage -> context.startActivity(
                    Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                )
                DashboardAction.GrantNotifications -> context.startActivity(
                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                )
            }
        })
    }
    if (showSummary) AiSummaryDialog(dataManager) { showSummary = false }
    if (showNotifications) {
        NotificationDetailDialog(context, thumbnailStyle) {
            showNotifications =
                false
        }
    }
    selectedMetric?.let { metric ->
        M3MetricDetailBottomSheet(
            metric,
            rememberModalBottomSheetState(skipPartiallyExpanded = true),
            onDismiss = { selectedMetric = null },
            onNavigateToAnalytics = {
                onOpenHistory(metric.id)
                selectedMetric = null
            }
        )
    }
}
