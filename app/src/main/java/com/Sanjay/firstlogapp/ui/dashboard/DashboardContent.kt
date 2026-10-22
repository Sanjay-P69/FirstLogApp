package com.Sanjay.firstlogapp.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.Sanjay.firstlogapp.R
import com.Sanjay.firstlogapp.ui.components.M3DashboardHeader
import com.Sanjay.firstlogapp.ui.components.M3MetricCard

/** Stateless UI: all Android permissions, queries, navigation, and storage live outside. */
@Composable
fun DashboardContent(
    state: DashboardUiState,
    onAction: (DashboardAction) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!state.hasLoadedData) {
        Column(modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
            if (state.isLoading) {
                CircularProgressIndicator()
                Text(stringResource(R.string.loading_activity), style = MaterialTheme.typography.bodyLarge)
            } else {
                Text(stringResource(R.string.activity_load_error), style = MaterialTheme.typography.bodyLarge)
                Button({ onAction(DashboardAction.Refresh) }) { Text(stringResource(R.string.retry)) }
            }
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(180.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
            M3DashboardHeader(state.date)
        }
        if (state.isLoading) {
            item(key = "loading", span = { GridItemSpan(maxLineSpan) }) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }
        if (state.hasError) {
            item(key = "error", span = { GridItemSpan(maxLineSpan) }) {
                Text(stringResource(R.string.dashboard_refresh_error), color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium)
            }
        }
        item(key = "screen", span = { GridItemSpan(maxLineSpan) }) {
            Card(
                onClick = { onAction(DashboardAction.OpenMetric(state.screen)) },
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(state.screen.title, style = MaterialTheme.typography.titleMedium)
                    Text(state.screen.duration, style = MaterialTheme.typography.displayMedium)
                    Text(state.screen.status, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        item(key = "summary", span = { GridItemSpan(maxLineSpan) }) {
            FilledTonalButton(onClick = { onAction(DashboardAction.OpenSummary) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.daily_summary))
            }
        }
        item(key = "activity_heading", span = { GridItemSpan(maxLineSpan) }) { SectionHeading(R.string.activity_heading) }
        item(key = "apps", span = { GridItemSpan(maxLineSpan) }) {
            Card(shape = MaterialTheme.shapes.extraLarge) {
                ActionRow(stringResource(R.string.app_usage), if (state.canReadApps) state.appUsage else stringResource(R.string.usage_access_hint)) {
                    onAction(if (state.canReadApps) DashboardAction.OpenApps else DashboardAction.GrantUsage)
                }
                if (state.canReadApps) {
                    state.apps.take(3).forEach { app ->
                        ListItem(headlineContent = { Text(app.name) }, supportingContent = { Text(app.duration) },
                            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest))
                    }
                    if (state.apps.isEmpty() && !state.isLoading) Text(stringResource(R.string.no_app_activity), Modifier.padding(20.dp))
                } else Text(stringResource(R.string.usage_access_hint), Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyMedium)
            }
        }
        item(key = "battery") { M3MetricCard(state.battery, { onAction(DashboardAction.OpenMetric(state.battery)) }) }
        item(key = "unlocks") { M3MetricCard(state.unlocks, { onAction(DashboardAction.OpenMetric(state.unlocks)) }) }
        item(key = "connections_heading", span = { GridItemSpan(maxLineSpan) }) { SectionHeading(R.string.connections_heading) }
        items(state.connections, key = { it.id }) { metric ->
            val displayMetric = if (metric.id == "bluetooth" && !state.canReadBluetooth) {
                metric.copy(duration = stringResource(R.string.access_needed), status = stringResource(R.string.bluetooth_access_hint))
            } else metric
            M3MetricCard(displayMetric, {
                onAction(if (metric.id == "bluetooth" && !state.canReadBluetooth) DashboardAction.GrantBluetooth
                    else DashboardAction.OpenMetric(metric))
            })
        }
        item(key = "communication_heading", span = { GridItemSpan(maxLineSpan) }) { SectionHeading(R.string.communication_heading) }
        if (state.canReadCalls) {
            items(state.calls, key = { it.id }) { metric ->
                M3MetricCard(metric, { onAction(DashboardAction.OpenMetric(metric)) })
            }
        } else {
            item(key = "call_permission", span = { GridItemSpan(maxLineSpan) }) {
                OutlinedCard(shape = MaterialTheme.shapes.extraLarge) {
                    Text(stringResource(R.string.call_access_hint), Modifier.padding(20.dp), style = MaterialTheme.typography.bodyLarge)
                    TextButton(onClick = { onAction(DashboardAction.GrantCalls) }, modifier = Modifier.padding(horizontal = 12.dp)) {
                        Text(stringResource(R.string.enable_call_tracking))
                    }
                }
            }
        }
        item(key = "notifications", span = { GridItemSpan(maxLineSpan) }) {
            Card(shape = MaterialTheme.shapes.extraLarge) {
                ActionRow(
                    stringResource(R.string.notifications_title),
                    if (state.canReadNotifications) stringResource(R.string.notifications_count, state.activeNotifications, state.notificationHistory)
                    else stringResource(R.string.notification_access_hint)
                ) { onAction(if (state.canReadNotifications) DashboardAction.OpenNotifications else DashboardAction.GrantNotifications) }
            }
        }
    }
}

@Composable
private fun SectionHeading(title: Int) {
    Text(stringResource(title), Modifier.padding(top = 16.dp, bottom = 4.dp), style = MaterialTheme.typography.titleLarge)
}

@Composable
private fun ActionRow(title: String, supporting: String, onClick: () -> Unit) {
    // Clickable Material surface supplies ripple, semantics, and keyboard focus.
    Surface(onClick = onClick, color = MaterialTheme.colorScheme.surfaceContainerHighest) {
        ListItem(
            headlineContent = { Text(title) },
            supportingContent = { Text(supporting) },
            trailingContent = { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null) },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
        )
    }
}
