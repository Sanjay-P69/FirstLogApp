package com.Sanjay.firstlogapp.ui.metrics

import com.Sanjay.firstlogapp.NetworkDataHelper
import com.Sanjay.firstlogapp.core.formatDurationMillis
import com.Sanjay.firstlogapp.core.formatTime
import com.Sanjay.firstlogapp.data.device.CallLogHelper
import com.Sanjay.firstlogapp.data.local.DataManager
import com.Sanjay.firstlogapp.data.model.BatteryHistoryRecord
import com.Sanjay.firstlogapp.ui.model.MetricItem

import android.content.Context
import android.content.Intent
import android.net.Network
import android.net.Uri
import android.provider.CallLog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.Sanjay.firstlogapp.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun StackedMonthSection(monthKey: String, monthTotalFormatted: String, dates: List<Pair<String, Long>>, initiallyExpanded: Boolean) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = if (initiallyExpanded) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(if (initiallyExpanded) "$monthKey (Current Month)" else monthKey, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Total: $monthTotalFormatted", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                }
                Text(if (expanded) "▲" else "▼", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp).fillMaxWidth()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(4.dp))
                    dates.forEach { (dateStr, millis) ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(dateStr, fontSize = 12.sp)
                            Text(formatDurationMillis(millis), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RadioDetailDialog(metric: MetricItem, dataManager: DataManager, onDismiss: () -> Unit) {
    val currentYear = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())
    val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    val stackedHistory = dataManager.getStackedHistoryByMonth(metric.id)
    val yearlyTotal = formatDurationMillis(dataManager.getYearlyMetricTotal(metric.id, currentYear))
    val todayCount = dataManager.getTodayMetricCount(metric.id)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (metric.iconResId != 0) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = metric.iconResId),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text("${metric.title} History", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Today: ${metric.duration} ($todayCount times opened)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(2.dp))
                Text("Yearly Total: $yearlyTotal", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))

                // Wi-Fi Details in Dialog
                if (metric.id == "wifi") {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF00ACC1).copy(alpha = 0.12f)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("📶 Wi-Fi Network & Data", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00838F))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Connected SSID:", fontSize = 12.sp)
                                Text(metric.wifiSsid ?: "Not connected", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Data Consumed:", fontSize = 12.sp)
                                Text(NetworkDataHelper.formatBytes(metric.wifiDataBytes), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            val avg = if (metric.wifiSessionCount > 0) metric.wifiDataBytes / metric.wifiSessionCount else metric.wifiDataBytes
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Usage Per Session:", fontSize = 12.sp)
                                Text("${NetworkDataHelper.formatBytes(avg)} (${metric.wifiSessionCount} sessions)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                // Mobile Data Consumption Details in Dialog
                if (metric.id == "data") {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFB8C00).copy(alpha = 0.12f)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("📊 Mobile Data Consumption", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Data Consumed:", fontSize = 12.sp)
                                Text(NetworkDataHelper.formatBytes(metric.mobileDataBytes), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            val avg = if (metric.mobileSessionCount > 0) metric.mobileDataBytes / metric.mobileSessionCount else metric.mobileDataBytes
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Usage Per Session:", fontSize = 12.sp)
                                Text("${NetworkDataHelper.formatBytes(avg)} (${metric.mobileSessionCount} sessions)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                // Dual Wi-Fi & Data breakdown in Dialog
                if (metric.id == "both_wifi_data" || (metric.id == "data" && metric.bothWifiDataTime != null && metric.bothWifiDataTime != "00m 00s")) {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFF9800).copy(alpha = 0.12f)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("⚡ Simultaneous Wi-Fi & Mobile Data", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                            Text("Active together for: ${metric.bothWifiDataTime ?: metric.duration}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text("Number of sessions: ${metric.bothWifiDataCount}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }

                // Point 3: Bluetooth Connected Devices breakdown in Dialog
                if (metric.id == "bluetooth") {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Connected Devices Duration Today", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("🎧 Buds / Audio:", fontSize = 12.sp)
                                Text(metric.btBudsDur ?: "00m 00s", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("⌚ Smartwatch / Band:", fontSize = 12.sp)
                                Text(metric.btWatchDur ?: "00m 00s", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("🚗 Android Auto / Car:", fontSize = 12.sp)
                                Text(metric.btAutoDur ?: "00m 00s", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(stackedHistory.keys.toList()) { monthKey ->
                        val dates = stackedHistory[monthKey] ?: emptyList()
                        StackedMonthSection(monthKey, formatDurationMillis(dates.sumOf { it.second }), dates, monthKey == currentMonth)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
fun UnlockDetailDialog(dataManager: DataManager, onDismiss: () -> Unit) {
    val count = dataManager.getTodayUnlockCount()
    val printCount = dataManager.getTodayUnlockFingerprint()
    val faceCount = dataManager.getTodayUnlockFace()
    val pinCount = dataManager.getTodayUnlockPin()
    val actualCount = dataManager.getTodayActualUsageCount()
    val actualDur = dataManager.getTodayActualUsageDuration()
    val glanceCount = dataManager.getTodayQuickGlanceCount()
    val glanceDur = dataManager.getTodayQuickGlanceDuration()
    val peekCount = dataManager.getTodayLockScreenPeekCount()
    val sessions = remember { dataManager.getTodayUnlockSessions().reversed() }
    val lastUnlock = dataManager.getLastUnlockTime()
    val lastType = dataManager.getLastUnlockType().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    val now = System.currentTimeMillis()
    val agoText = if (lastUnlock > 0L) "${formatDurationMillis(now - lastUnlock)} ago" else "None"

    val printPct = if (count > 0) (printCount * 100f / count) else 0f
    val facePct = if (count > 0) (faceCount * 100f / count) else 0f
    val pinPct = if (count > 0) (pinCount * 100f / count) else 0f
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Screen Unlocks Breakdown", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Today's Total Unlocks: $count times", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.unlock_estimate_hint), style = MaterialTheme.typography.bodyMedium)
                if (lastUnlock > 0L) {
                    Text("Last: ${formatTime(lastUnlock)} ($agoText) via $lastType", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // Actual Usage vs Quick Glance Breakdown
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Session Intent Breakdown", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("📱 Actual Usage (≥10s)", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Text("$actualCount times", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), fontSize = 12.sp)
                            }
                            Text("Total time: ${formatDurationMillis(actualDur)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(start = 18.dp))
                        }
                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("👁️ Quick Glance (<10s)", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Text("$glanceCount times", fontWeight = FontWeight.Bold, color = Color(0xFF558B2F), fontSize = 12.sp)
                            }
                            Text("Total time: ${formatDurationMillis(glanceDur)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(start = 18.dp))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("🔒 Lock Screen Peeks", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text("$peekCount glances", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("👆 Fingerprint:", fontSize = 13.sp)
                    Text("$printCount times (${String.format(Locale.getDefault(), "%.1f%%", printPct)})", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("👤 Face Recognition:", fontSize = 13.sp)
                    Text("$faceCount times (${String.format(Locale.getDefault(), "%.1f%%", facePct)})", fontWeight = FontWeight.Bold, color = Color(0xFF00897B), fontSize = 13.sp)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("🔢 PIN / Password:", fontSize = 13.sp)
                    Text("$pinCount times (${String.format(Locale.getDefault(), "%.1f%%", pinPct)})", fontWeight = FontWeight.Bold, color = Color(0xFF7B1FA2), fontSize = 13.sp)
                }

                if (sessions.isNotEmpty()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Text("Recent Sessions History", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    sessions.take(20).forEach { s ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val tag = if (s.isQuickGlance) "👁️ Glance" else "📱 Session"
                            Text("${formatTime(s.timestamp)} ($tag)", fontSize = 11.sp)
                            Text(formatDurationMillis(s.durationMs), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
fun BatteryDetailDialog(dataManager: DataManager, onDismiss: () -> Unit) {
    val startLevel = dataManager.getBatteryStartLevel()
    val currentLevel = dataManager.getCurrentBatteryLevel()
    val now = System.currentTimeMillis()
    val isCharging = dataManager.isCharging()
    val lastChargeStart = dataManager.getLastChargeStartTime()
    val liveChargeExtra = if (isCharging && lastChargeStart > 0L) (now - lastChargeStart).coerceAtLeast(0L) else 0L
    val totalChargingTime = dataManager.getTodayChargingTime() + liveChargeExtra
    val batteryUsedToday = dataManager.getTodayBatteryUsed()
    val lastHourDrop = dataManager.getBatteryUsageInLastHour(currentLevel, now)
    val lastHourText = when {
        lastHourDrop > 0 -> "Used $lastHourDrop%"
        lastHourDrop < 0 -> "+${-lastHourDrop}% (charging)"
        else -> "0% used"
    }
    val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    val stackedBattery = remember { dataManager.getStackedBatteryHistoryByMonth() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Battery Usage Details", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Total Battery Used Today:", fontSize = 12.sp, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$batteryUsedToday%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, textAlign = TextAlign.End)
                }
                val drainRate = dataManager.getScreenOnDrainRate()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Screen On Drain Rate:", fontSize = 12.sp, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        if (drainRate > 0.0) String.format(Locale.getDefault(), "%.1f%% / hour", drainRate) else "Measuring...",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE65100),
                        fontSize = 13.sp,
                        textAlign = TextAlign.End
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Total Charging Duration:", fontSize = 12.sp, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        formatDurationMillis(totalChargingTime),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        softWrap = false
                    )
                }
                val chargingSessions = remember { dataManager.getTodayChargingSessions() }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Charging Sessions Today:", fontSize = 12.sp, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "${chargingSessions.size} ${if (chargingSessions.size == 1) "session" else "sessions"}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.End
                    )
                }

                if (chargingSessions.isNotEmpty()) {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("⚡ Today's Charging Log", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFFE65100))
                            chargingSessions.forEach { cs ->
                                val gain = cs.endLevel - cs.startLevel
                                val gainStr = if (gain >= 0) "+$gain%" else "$gain%"
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${formatTime(cs.startTime)} - ${formatTime(cs.endTime)}", fontSize = 10.sp)
                                    Text("${formatDurationMillis(cs.durationMs)} ($gainStr)", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Current Battery Level:", fontSize = 12.sp, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val statusStr = if (isCharging) "Charging" else "Discharging"
                    Text(
                        if (currentLevel != -1) "$currentLevel% ($statusStr)" else "Unknown",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        softWrap = false
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Cycle Start Level:", fontSize = 12.sp, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(if (startLevel != -1) "$startLevel%" else "Unknown", fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.End)
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Usage in Last Hour:", fontSize = 12.sp, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        lastHourText,
                        fontWeight = FontWeight.Bold,
                        color = if (lastHourDrop > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), modifier = Modifier.padding(top = 4.dp))
                Text("Calendar Dates History", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)

                LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                    items(stackedBattery.keys.toList()) { monthKey ->
                        val entries = stackedBattery[monthKey] ?: emptyList()
                        val totalMonthUsed = entries.sumOf { it.batteryUsedPct }
                        val totalMonthCharge = formatDurationMillis(entries.sumOf { it.chargeDurationMs })
                        StackedBatteryMonthSection(monthKey, totalMonthUsed, totalMonthCharge, entries, monthKey == currentMonth)
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
fun StackedBatteryMonthSection(
    monthKey: String,
    totalUsed: Int,
    totalChargeFormatted: String,
    records: List<BatteryHistoryRecord>,
    initiallyExpanded: Boolean
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (initiallyExpanded) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(if (initiallyExpanded) "$monthKey (Current Month)" else monthKey, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Total Used: $totalUsed% • Total Charge: $totalChargeFormatted", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                }
                Text(if (expanded) "▲" else "▼", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
            }
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp).fillMaxWidth()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(4.dp))
                    records.forEach { rec ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    rec.date,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "Used ${rec.batteryUsedPct}%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (rec.batteryUsedPct > 80) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Charging Time",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    formatDurationMillis(rec.chargeDurationMs),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CallDetailDialog(
    context: Context,
    isIncoming: Boolean,
    title: String,
    initialTab: Int = 0,
    onDismiss: () -> Unit
) {
    val calls = remember { CallLogHelper.readCalls(context, isIncoming) }
    val connectedCalls = remember(calls) { CallLogHelper.getConnectedCalls(calls, isIncoming) }
    val missedCalls = remember(calls) { CallLogHelper.getMissedCalls(calls, isIncoming) }
    val callerStats = remember(calls) { CallLogHelper.getCallerStats(connectedCalls) }
    val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    val currentYear = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())
    var selectedTab by remember { mutableStateOf(initialTab.coerceIn(0, 2)) }

    val missedLabel = if (isIncoming) "Missed Calls" else "Unanswered Calls"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("$title Details", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                val dataManager = remember { DataManager(context) }
                val todayConnected = connectedCalls.filter { CallLogHelper.isCallInCurrentCycle(it.timestamp, dataManager) }
                val todayMissed = missedCalls.filter { CallLogHelper.isCallInCurrentCycle(it.timestamp, dataManager) }
                val todayDur = formatDurationMillis(todayConnected.sumOf { it.durationSeconds } * 1000)
                Text(
                    text = "Today: $todayDur (${todayConnected.size} connected) • ${todayMissed.size} $missedLabel",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Connected (${connectedCalls.size})", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                if (isIncoming) "Missed (${missedCalls.size})" else "Unanswered (${missedCalls.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (missedCalls.isNotEmpty()) Color(0xFFD32F2F) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Top Callers", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                when (selectedTab) {
                    0 -> {
                        // Connected calls grouped by date
                        if (connectedCalls.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                Text("No connected calls found.", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                            }
                        } else {
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            val dateGroups = connectedCalls.groupBy { sdf.format(Date(it.timestamp)) }
                            LazyColumn(modifier = Modifier.heightIn(max = 330.dp)) {
                                items(dateGroups.keys.toList()) { dateStr ->
                                    val dayCalls = dateGroups[dateStr] ?: emptyList()
                                    Card(
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(dateStr, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                                Text(
                                                    "${dayCalls.size} calls • ${formatDurationMillis(dayCalls.sumOf { it.durationSeconds } * 1000)}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            dayCalls.forEachIndexed { idx, rec ->
                                                if (idx > 0) {
                                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                                }
                                                val tStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(rec.timestamp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                                        Text(
                                                            text = rec.name.ifBlank { rec.number },
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            maxLines = 2,
                                                            softWrap = true,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        if (rec.name != rec.number && rec.number.isNotBlank()) {
                                                            Text(rec.number, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                                        }
                                                    }
                                                    Column(horizontalAlignment = Alignment.End) {
                                                        Text(
                                                            tStr,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                        Text(
                                                            formatDurationMillis(rec.durationSeconds * 1000),
                                                            fontSize = 10.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        // Missed / Unanswered calls with caller name, number, timestamp, date, and dial button
                        if (missedCalls.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                Text("No $missedLabel recorded.", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                            }
                        } else {
                            LazyColumn(modifier = Modifier.heightIn(max = 330.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(missedCalls) { call ->
                                    val timeStr = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date(call.timestamp))
                                    val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(call.timestamp))
                                    val isRejected = call.type == CallLog.Calls.REJECTED_TYPE
                                    val typeTag = if (isRejected) "Rejected" else if (isIncoming) "Missed" else "Unanswered"
                                    val tagColor = if (isRejected) Color(0xFFE65100) else Color(0xFFC62828)

                                    Card(
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            // Line 1: Type badge + Contact name (wrapable, non-clipping) + Dial Button
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(tagColor.copy(alpha = 0.15f))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(typeTag, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = tagColor)
                                                    }
                                                    Text(
                                                        text = call.name.ifBlank { call.number },
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 2,
                                                        softWrap = true,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f, fill = false)
                                                    )
                                                }

                                                // Call back dialer button
                                                FilledTonalButton(
                                                    onClick = {
                                                        try {
                                                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${call.number}"))
                                                            context.startActivity(dialIntent)
                                                        } catch (_: Exception) {}
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(30.dp)
                                                ) {
                                                    Text("📞 Call", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }

                                            // Line 2: Phone number if name is different
                                            if (call.name.isNotBlank() && call.name != call.number && call.number.isNotBlank()) {
                                                Text(
                                                    text = call.number,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Normal,
                                                    color = MaterialTheme.colorScheme.outline
                                                )
                                            }

                                            // Line 3: Clean Date & Time layout without wrapping
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "🗓️ $dateStr",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                                )
                                                Text(
                                                    text = "⏰ $timeStr",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = tagColor
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        // Top callers tab
                        if (callerStats.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                Text("No call statistics available.", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                            }
                        } else {
                            LazyColumn(modifier = Modifier.heightIn(max = 330.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(callerStats) { stat ->
                                    Card(
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                                Text(stat.displayName, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 2, softWrap = true, overflow = TextOverflow.Ellipsis)
                                                if (stat.displayName != stat.number && stat.number.isNotBlank()) {
                                                    Text(stat.number, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                                }
                                                Text("${stat.callCount} calls", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                                            }
                                            Text(
                                                formatDurationMillis(stat.totalSeconds * 1000),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
