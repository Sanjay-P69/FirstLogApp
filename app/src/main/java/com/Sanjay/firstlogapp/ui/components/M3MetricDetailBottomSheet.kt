package com.Sanjay.firstlogapp.ui.components

import com.Sanjay.firstlogapp.NetworkDataHelper
import com.Sanjay.firstlogapp.ui.model.MetricItem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.Sanjay.firstlogapp.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale

/**
 * Modern Material 3 Modal Bottom Sheet for deep metric logs and breakdowns.
 * Replaces old raw dialog popups with swipeable bottom sheets and standard drag handles.
 */
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
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Icon + Title + Live Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (metric.iconResId != 0) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = metric.iconResId),
                                contentDescription = metric.title,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = metric.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Today’s activity",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Primary Stat Pill
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 1.dp
                ) {
                    Text(
                        text = metric.duration,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Metric Detailed Content Breakdown
            when (metric.id) {
                "wifi" -> WifiDetailBreakdown(metric)
                "data" -> MobileDataDetailBreakdown(metric)
                "battery" -> BatteryDetailBreakdown(metric)
                "unlock" -> UnlockDetailBreakdown(metric)
                "bluetooth" -> BluetoothDetailBreakdown(metric)
                "call_in", "call_out" -> CallDetailBreakdown(metric)
                else -> GenericDetailBreakdown(metric)
            }

            // Quick Actions / Historical Analytics Deep-link
            if (onNavigateToAnalytics != null) {
                Button(
                    onClick = {
                        onDismiss()
                        onNavigateToAnalytics(metric.id)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.BarChart,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "View activity history",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun WifiDetailBreakdown(metric: MetricItem) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DetailRowCard(
            label = "Connected Wi-Fi SSID",
            value = metric.wifiSsid ?: "Not connected",
            highlight = true
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DetailStatCard(
                label = "Total Transferred",
                value = NetworkDataHelper.formatBytes(metric.wifiDataBytes),
                modifier = Modifier.weight(1f)
            )
            val avg = if (metric.wifiSessionCount > 0) metric.wifiDataBytes / metric.wifiSessionCount else metric.wifiDataBytes
            DetailStatCard(
                label = "Avg per Session",
                value = NetworkDataHelper.formatBytes(avg),
                modifier = Modifier.weight(1f)
            )
        }
        DetailRowCard(
            label = "Recorded Sessions Today",
            value = "${metric.wifiSessionCount} active connects",
            highlight = false
        )
    }
}

@Composable
private fun MobileDataDetailBreakdown(metric: MetricItem) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DetailRowCard(
            label = "Cellular Data Consumption",
            value = NetworkDataHelper.formatBytes(metric.mobileDataBytes),
            highlight = true
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DetailStatCard(
                label = "Active Sessions",
                value = "${metric.mobileSessionCount}",
                modifier = Modifier.weight(1f)
            )
            val avg = if (metric.mobileSessionCount > 0) metric.mobileDataBytes / metric.mobileSessionCount else metric.mobileDataBytes
            DetailStatCard(
                label = "Avg Consumption",
                value = NetworkDataHelper.formatBytes(avg),
                modifier = Modifier.weight(1f)
            )
        }
        if (metric.bothWifiDataTime != null && metric.bothWifiDataTime != "00m 00s") {
            DetailRowCard(
                label = "Dual Wi-Fi & Data Concurrency",
                value = "${metric.bothWifiDataTime} (${metric.bothWifiDataCount} events)",
                highlight = false
            )
        }
    }
}

@Composable
private fun BatteryDetailBreakdown(metric: MetricItem) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DetailRowCard(
            label = "Active Power Profile",
            value = metric.status,
            highlight = true
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DetailStatCard(
                label = "Charging Sessions",
                value = "${metric.chargingSessionsCount}",
                modifier = Modifier.weight(1f)
            )
            val rate = if (metric.screenOnDrainRate > 0) String.format(Locale.getDefault(), "%.1f%%/hr", metric.screenOnDrainRate) else "Normal"
            DetailStatCard(
                label = "Screen Drain Rate",
                value = rate,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun UnlockDetailBreakdown(metric: MetricItem) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DetailStatCard(
                label = "Active Usages",
                value = "${metric.actualUsageCount}",
                subtext = stringResource(R.string.active_session_threshold),
                modifier = Modifier.weight(1f)
            )
            DetailStatCard(
                label = "Quick Glances",
                value = "${metric.quickGlanceCount}",
                subtext = stringResource(R.string.quick_glance_threshold),
                modifier = Modifier.weight(1f)
            )
        }
        DetailRowCard(
            label = stringResource(R.string.estimated_unlock_methods),
            value = stringResource(R.string.unlock_method_counts, metric.unlockFingerprint, metric.unlockFace, metric.unlockPin),
            highlight = false
        )
        Text(stringResource(R.string.unlock_estimate_hint), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun BluetoothDetailBreakdown(metric: MetricItem) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DetailRowCard(
            label = "Headphones & Earbuds",
            value = metric.btBudsDur ?: "00m 00s",
            highlight = true
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DetailStatCard(
                label = "Smartwatch",
                value = metric.btWatchDur ?: "00m 00s",
                modifier = Modifier.weight(1f)
            )
            DetailStatCard(
                label = "Car / Android Auto",
                value = metric.btAutoDur ?: "00m 00s",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CallDetailBreakdown(metric: MetricItem) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DetailRowCard(
            label = "Total Talk Time",
            value = metric.connectedCallsDuration ?: metric.duration,
            highlight = true
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DetailStatCard(
                label = "Connected Calls",
                value = "${metric.connectedCallsCount}",
                modifier = Modifier.weight(1f)
            )
            DetailStatCard(
                label = "Missed Calls",
                value = "${metric.missedCallsCount}",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun GenericDetailBreakdown(metric: MetricItem) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DetailRowCard(
            label = "Real-time Telemetry State",
            value = metric.status,
            highlight = true
        )
    }
}

@Composable
private fun DetailRowCard(
    label: String,
    value: String,
    highlight: Boolean
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun DetailStatCard(
    label: String,
    value: String,
    subtext: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtext != null) {
                Text(
                    text = subtext,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

