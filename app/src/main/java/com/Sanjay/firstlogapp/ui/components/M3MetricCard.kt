package com.Sanjay.firstlogapp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Sanjay.firstlogapp.MetricItem
import com.Sanjay.firstlogapp.NetworkDataHelper
import java.util.Locale

/**
 * Material 3 Metric Card adhering to top-tier utility app design standards.
 *
 * Implements a strict 3-level information hierarchy:
 * 1. Icon + Title (Top row, tonal icon squircle, muted slate label)
 * 2. Primary Stat (Bold, high contrast, tabular figures enabled)
 * 3. Contextual Subtitle (Single line, subtle, meaningful summary)
 *
 * Noisy secondary sub-cards are eliminated from the card face and surfaced
 * in the detailed M3 ModalBottomSheet on tap.
 */
@Composable
fun M3MetricCard(
    item: MetricItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 1.dp,
            pressedElevation = 4.dp,
            hoveredElevation = 2.dp
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Level 1: Icon + Title + Status dot
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    if (item.iconResId != 0) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(item.iconTint.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = item.iconResId),
                                contentDescription = item.title,
                                tint = item.iconTint,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Subtle active indicator dot
                val isMonitoring = item.status.contains("Active", ignoreCase = true) ||
                        item.status.contains("Monitoring", ignoreCase = true) ||
                        item.status.contains("Connected", ignoreCase = true) ||
                        item.status.contains("Live", ignoreCase = true)

                val dotColor by animateColorAsState(
                    targetValue = if (isMonitoring) Color(0xFF4EBA6F) else MaterialTheme.colorScheme.outlineVariant,
                    label = "statusDotColor"
                )

                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
            }

            // Level 2: Primary Stat (Large, Bold, Tabular figures)
            Text(
                text = item.duration,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Level 3: Contextual Subtitle (Single clean line, muted tone)
            val subtitle = getContextualSubtitle(item)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Computes a concise, contextual single-line summary for the 3rd level of the card hierarchy.
 */
private fun getContextualSubtitle(item: MetricItem): String {
    return when (item.id) {
        "screen" -> {
            if (item.status.isNotBlank() && !item.status.startsWith("Monitoring")) item.status else "Live session tracking"
        }
        "wifi" -> {
            val ssid = item.wifiSsid?.takeIf { it.isNotBlank() } ?: "Connected"
            val bytesStr = NetworkDataHelper.formatBytes(item.wifiDataBytes)
            "$ssid • $bytesStr"
        }
        "data" -> {
            val bytesStr = NetworkDataHelper.formatBytes(item.mobileDataBytes)
            if (item.mobileSessionCount > 0) "$bytesStr • ${item.mobileSessionCount} sessions" else "$bytesStr total"
        }
        "battery" -> {
            val drain = if (item.screenOnDrainRate > 0.0) {
                String.format(Locale.getDefault(), "%.1f%%/hr", item.screenOnDrainRate)
            } else "Normal"
            "${item.chargingSessionsCount} charges • $drain"
        }
        "unlock" -> {
            val total = item.actualUsageCount + item.quickGlanceCount
            if (total > 0) {
                "${item.actualUsageCount} active • ${item.quickGlanceCount} glance"
            } else if (item.unlockFingerprint > 0 || item.unlockFace > 0) {
                "${item.unlockFingerprint} FP • ${item.unlockFace} Face"
            } else {
                "Screen unlock breakdown"
            }
        }
        "call_in" -> {
            "${item.connectedCallsCount} answered • ${item.missedCallsCount} missed"
        }
        "call_out" -> {
            val dur = item.connectedCallsDuration ?: "0m"
            "${item.connectedCallsCount} calls • $dur"
        }
        "bluetooth" -> {
            val buds = item.btBudsDur?.takeIf { it != "00m 00s" && it != "0m" }
            if (buds != null) "$buds buds • Connected" else "Audio & accessories"
        }
        "gps" -> {
            if (item.status.isNotBlank() && !item.status.startsWith("Monitoring")) item.status else "Precise location active"
        }
        else -> item.status.ifBlank { "Tap for detailed analytics" }
    }
}
