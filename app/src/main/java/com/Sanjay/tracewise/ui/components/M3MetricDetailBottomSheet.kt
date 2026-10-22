package com.sanjay.tracewise.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.Sanjay.firstlogapp.NetworkDataHelper
import com.sanjay.tracewise.R
import com.sanjay.tracewise.ui.model.MetricItem
import java.util.Locale

/** Brief contextual inspection. Long history opens a dedicated full-screen destination. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun M3MetricDetailBottomSheet(
    metric: MetricItem,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onNavigateToAnalytics: ((String) -> Unit)? = null
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(metric.title, style = MaterialTheme.typography.headlineSmall)
            Text(metric.duration, style = MaterialTheme.typography.displaySmall)
            Text(
                metric.status,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            when (metric.id) {
                "wifi" -> {
                    MetricFact(
                        stringResource(R.string.connected_network),
                        metric.wifiSsid ?: stringResource(R.string.not_connected)
                    )
                    MetricFact(
                        stringResource(R.string.transferred_data),
                        NetworkDataHelper.formatBytes(metric.wifiDataBytes)
                    )
                    MetricFact(
                        stringResource(R.string.recorded_sessions),
                        metric.wifiSessionCount.toString()
                    )
                }
                "data" -> {
                    MetricFact(
                        stringResource(R.string.transferred_data),
                        NetworkDataHelper.formatBytes(metric.mobileDataBytes)
                    )
                    MetricFact(
                        stringResource(R.string.recorded_sessions),
                        metric.mobileSessionCount.toString()
                    )
                }
                "both_wifi_data" -> {
                    MetricFact(
                        stringResource(R.string.recorded_sessions),
                        metric.bothWifiDataCount.toString()
                    )
                    Text(
                        stringResource(R.string.simultaneous_state_hint),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                "bluetooth" -> {
                    MetricFact(stringResource(R.string.bluetooth_audio), metric.btBudsDur.orEmpty())
                    MetricFact(
                        stringResource(R.string.bluetooth_watch),
                        metric.btWatchDur.orEmpty()
                    )
                    MetricFact(stringResource(R.string.bluetooth_car), metric.btAutoDur.orEmpty())
                }
                "battery" -> {
                    MetricFact(
                        stringResource(R.string.charging_sessions),
                        metric.chargingSessionsCount.toString()
                    )
                    MetricFact(
                        stringResource(R.string.screen_drain_rate),
                        String.format(Locale.getDefault(), "%.1f %%/h", metric.screenOnDrainRate)
                    )
                }
                "unlock" -> {
                    MetricFact(
                        stringResource(R.string.active_session_threshold),
                        metric.actualUsageCount.toString()
                    )
                    MetricFact(
                        stringResource(R.string.quick_glance_threshold),
                        metric.quickGlanceCount.toString()
                    )
                    Text(
                        stringResource(R.string.unlock_estimate_hint),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                "call_in", "call_out" -> {
                    MetricFact(
                        stringResource(R.string.connected_calls),
                        metric.connectedCallsCount.toString()
                    )
                    MetricFact(
                        stringResource(R.string.connected_duration),
                        metric.connectedCallsDuration.orEmpty()
                    )
                    MetricFact(
                        stringResource(R.string.missed_calls),
                        metric.missedCallsCount.toString()
                    )
                }
            }
            onNavigateToAnalytics?.let { navigate ->
                FilledTonalButton(
                    onClick = {
                        onDismiss()
                        navigate(metric.id)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.view_metric_history))
                }
            }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.close))
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun MetricFact(label: String, value: String) {
    ListItem(headlineContent = { Text(label) }, supportingContent = { Text(value) })
}
