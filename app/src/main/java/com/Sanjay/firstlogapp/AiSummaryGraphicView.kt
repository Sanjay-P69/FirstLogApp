package com.Sanjay.firstlogapp

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanjay.tracewise.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AiSummaryGraphicReportView(data: DailyActivitySummaryData, modifier: Modifier = Modifier) {
    val bgSlate900 = Color(0xFF0F172A)
    val cardBg = Color(0xFF1E293B)
    val cardBorder = Color(0xFF334155)
    val textWhite = Color(0xFFF8FAFC)
    val textMuted = Color(0xFF94A3B8)
    val accentTeal = Color(0xFF14B8A6)
    val accentCyan = Color(0xFF06B6D4)
    val accentOrange = Color(0xFFF97316)
    val accentBlue = Color(0xFF3B82F6)
    val accentGreen = Color(0xFF10B981)
    val accentAmber = Color(0xFFF59E0B)
    val accentPurple = Color(0xFF8B5CF6)

    val nowStr = remember {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgSlate900),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ==========================================
            // 1. HEADER (Non-wrapping, full width)
            // ==========================================
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_firstlog_logo),
                        contentDescription = stringResource(R.string.brand_logo_description),
                        tint = Color.Unspecified,
                        modifier = Modifier.size(24.dp)
                    )

                    Text(
                        stringResource(R.string.app_name),
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = accentCyan
                    )
                    Text("|", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = cardBorder)
                    Text(
                        "DAILY ACTIVITY REPORT",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = textWhite
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "DATE: ${data.dateStr}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = accentTeal
                    )
                    Text(
                        "GENERATED: ${nowStr.uppercase(Locale.getDefault())}",
                        fontSize = 10.sp,
                        color = textMuted
                    )
                }
            }

            // ==========================================
            // 2. VERTICALLY STACKED CARDS (Full Width, One After Another)
            // ==========================================

            // CARD 1: TELEPHONY
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "📞 TELEPHONY",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = accentTeal
                    )

                    // Incoming Calls (Line 1: Title & Total, Line 2: Details)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("↙", fontSize = 13.sp, color = accentCyan)
                            Text(
                                "Incoming Calls: ${data.incomingTotalCount}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = textWhite
                            )
                        }
                        Text(
                            text = "✔ ${data.incomingConnectedCount} Connected (${formatDurCompact(
                                data.incomingConnectedDurSec * 1000L
                            )}) • 📵 ${data.incomingMissedCount} Missed",
                            fontSize = 11.sp,
                            color = textMuted,
                            modifier = Modifier.padding(start = 18.dp)
                        )
                    }

                    // Missed Call Details Box
                    val lastMissed = data.incomingMissedDetails.firstOrNull() ?: "None recorded"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(accentOrange.copy(alpha = 0.12f))
                            .border(1.dp, accentOrange.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "📵 Last Missed:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = accentOrange
                        )
                        Text(
                            lastMissed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = textWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Outgoing Calls (Line 1: Title & Total, Line 2: Details)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("↗", fontSize = 13.sp, color = accentCyan)
                            Text(
                                "Outgoing Calls: ${data.outgoingTotalCount}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = textWhite
                            )
                        }
                        Text(
                            text = "✔ ${data.outgoingConnectedCount} Connected (${formatDurCompact(
                                data.outgoingConnectedDurSec * 1000L
                            )}) • 📞 ${data.outgoingUnconnectedCount} Unanswered",
                            fontSize = 11.sp,
                            color = textMuted,
                            modifier = Modifier.padding(start = 18.dp)
                        )
                    }
                }
            }

            // CARD 2: SCREEN ENGAGEMENT (Redesigned: Clean, non-wrapping full-width stacked boxes)
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "🖥️ SCREEN ENGAGEMENT",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = accentTeal
                    )

                    // Row of 2 Symmetrical Stat Boxes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Box 1: Screen Time
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(bgSlate900)
                                .border(1.dp, cardBorder, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("⌛", fontSize = 13.sp)
                                    Text("Screen Time", fontSize = 10.sp, color = textMuted)
                                }
                                Text(
                                    text = formatDurCompact(data.screenTimeMs),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = textWhite
                                )
                            }
                        }

                        // Box 2: Unlock Count & Breakdown
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(bgSlate900)
                                .border(1.dp, cardBorder, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("🔒", fontSize = 13.sp)
                                    Text("Total Unlocks", fontSize = 10.sp, color = textMuted)
                                }
                                Text(
                                    text = "${data.unlockCount} unlocks",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = textWhite
                                )
                                Text(
                                    text = "Actual: ${data.actualUsageCount} • " +
                                        "Glance: ${data.quickGlanceCount}",
                                    fontSize = 9.sp,
                                    color = accentTeal
                                )
                            }
                        }
                    }

                    // Unlock Methods Full-Width Breakdown
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(bgSlate900)
                            .border(1.dp, cardBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Unlock Methods",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textMuted
                            )
                            val recordedUnlocks =
                                data.unlockPin + data.unlockFace + data.unlockFingerprint
                            val totalU = recordedUnlocks.coerceAtLeast(1)
                            Text("$totalU tracked", fontSize = 10.sp, color = textMuted)
                        }

                        // Clean Segmented Bar for Unlock Types
                        val recordedUnlocks =
                            data.unlockPin + data.unlockFace + data.unlockFingerprint
                        val totalU = recordedUnlocks.coerceAtLeast(1)
                        val pinFrac = (data.unlockPin.toFloat() / totalU).coerceIn(0f, 1f)
                        val faceFrac = (data.unlockFace.toFloat() / totalU).coerceIn(0f, 1f)
                        val fingerFrac = (data.unlockFingerprint.toFloat() / totalU).coerceIn(
                            0f,
                            1f
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(cardBorder)
                        ) {
                            if (pinFrac > 0f) {
                                Box(
                                    modifier = Modifier.weight(
                                        pinFrac
                                    ).fillMaxHeight().background(accentBlue)
                                )
                            }
                            if (faceFrac > 0f) {
                                Box(
                                    modifier = Modifier.weight(
                                        faceFrac
                                    ).fillMaxHeight().background(accentCyan)
                                )
                            }
                            if (fingerFrac > 0f || totalU == 1) {
                                Box(
                                    modifier = Modifier.weight(
                                        if (fingerFrac >
                                            0f
                                        ) {
                                            fingerFrac
                                        } else {
                                            1f
                                        }
                                    ).fillMaxHeight().background(accentOrange)
                                )
                            }
                        }

                        // 3 Legend Badges (Line 2)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "🔵 PIN: ${data.unlockPin}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = textWhite
                            )
                            Text(
                                "💠 Face: ${data.unlockFace}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = textWhite
                            )
                            Text(
                                "🟠 Print: ${data.unlockFingerprint}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = textWhite
                            )
                        }
                    }
                }
            }

            // CARD 3: CONNECTIVITY (Line 1: Title & Time, Line 2: Usage & other details)
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "↔️ CONNECTIVITY",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = accentTeal
                    )

                    // 1. Wi-Fi (Line 1: Title with duration, Line 2: Data usage & SSID)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("📶", fontSize = 13.sp, color = accentCyan)
                            Text(
                                "Wi-Fi (${formatDurCompact(data.wifiTimeMs)})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = textWhite
                            )
                        }
                        Text(
                            text = "📊 Data: ${NetworkDataHelper.formatBytes(
                                data.wifiDataBytes
                            )} • SSID: ${data.wifiSsid} • ${data.wifiSessionCount} sess",
                            fontSize = 11.sp,
                            color = textMuted,
                            modifier = Modifier.padding(start = 18.dp)
                        )
                    }

                    // 2. Mobile Data (Line 1: Title with duration, Line 2: Data usage & sessions)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("📶", fontSize = 13.sp, color = accentOrange)
                            Text(
                                "Mobile Data (${formatDurCompact(data.mobileDataTimeMs)})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = textWhite
                            )
                        }
                        Text(
                            text = "📊 Total Data: ${NetworkDataHelper.formatBytes(
                                data.mobileDataBytes
                            )} • ${data.mobileDataSessionCount} sess",
                            fontSize = 11.sp,
                            color = textMuted,
                            modifier = Modifier.padding(start = 18.dp)
                        )
                    }

                    // 3. Bluetooth (Line 1: Title with duration, Line 2: Buds, Watch, Auto)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("ᛒ", fontSize = 13.sp, color = accentBlue)
                            Text(
                                "Bluetooth (${formatDurCompact(data.bluetoothTimeMs)})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = textWhite
                            )
                        }
                        Text(
                            text = "🎧 Buds: ${formatDurCompact(
                                data.btBudsMs
                            )} • ⌚ Watch: ${formatDurCompact(
                                data.btWatchMs
                            )} • 🚗 Auto: ${formatDurCompact(data.btAutoMs)}",
                            fontSize = 11.sp,
                            color = textMuted,
                            modifier = Modifier.padding(start = 18.dp)
                        )
                    }
                }
            }

            // CARD 4: BATTERY
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "🔋 BATTERY",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = accentTeal
                    )

                    // Line 1: Discharged % and Drain Rate
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("🪫", fontSize = 13.sp, color = accentOrange)
                            Text(
                                "Discharged: ${data.batteryUsedPct}%",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = textWhite
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("⚡", fontSize = 13.sp, color = accentGreen)
                            val rateStr = String.format(
                                Locale.getDefault(),
                                "%.1f",
                                data.screenOnDrainRatePctPerHour
                            )
                            Text(
                                "Drain Rate: $rateStr%/hr",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = accentGreen
                            )
                        }
                    }

                    // Line 2: Charging time & sessions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("🔌", fontSize = 13.sp, color = accentGreen)
                            Text(
                                "Charging: ${formatDurCompact(data.chargingTimeMs)}",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = textWhite
                            )
                        }
                        Text(
                            "${data.chargingSessionCount} sessions",
                            fontSize = 11.sp,
                            color = textMuted
                        )
                    }
                }
            }

            // CARD 5: APPLICATION ENGAGEMENT (TOP APPS) (Non-wrapping clean stacked list)
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val apps = data.topApps.take(6)
                    val totalAppTime = apps.sumOf { it.second }.coerceAtLeast(1L)
                    val appColors =
                        listOf(
                            accentGreen,
                            accentCyan,
                            accentAmber,
                            accentOrange,
                            accentPurple,
                            accentBlue
                        )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "📱 TOP APPLICATIONS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = accentTeal
                        )
                        Text("${apps.size} apps logged", fontSize = 10.sp, color = textMuted)
                    }

                    apps.forEachIndexed { i, (name, dur) ->
                        val color = appColors[i % appColors.size]
                        val pct = (dur.toFloat() / totalAppTime) * 100f
                        val progress = (dur.toFloat() / totalAppTime).coerceIn(0.04f, 1f)

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(color),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            name.firstOrNull()?.uppercase() ?: "A",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )
                                    }
                                    Text(
                                        text = name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = textWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "${formatDurCompact(
                                        dur
                                    )} (${String.format(Locale.getDefault(), "%.1f%%", pct)})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = color
                                )
                            }

                            // Progress bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(cardBorder)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction = progress)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(
                                            Brush.horizontalGradient(listOf(color, accentCyan))
                                        )
                                )
                            }
                        }
                    }
                }
            }

            // CARD 6: BOTTOM BANNER: AI EXECUTIVE SUMMARY & PRODUCTIVITY INSIGHTS
            // (Title in Line 1, Text in Line 2 and subsequent lines)
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "💡 AI EXECUTIVE SUMMARY & PRODUCTIVITY INSIGHTS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = accentTeal,
                        modifier = Modifier.fillMaxWidth()
                    )

                    val glanceRatio = if (data.unlockCount >
                        0
                    ) {
                        ((data.quickGlanceCount.toDouble() / data.unlockCount) * 100).toInt()
                    } else {
                        0
                    }
                    val drainRateStr = String.format(
                        Locale.getDefault(),
                        "%.1f",
                        data.screenOnDrainRatePctPerHour
                    )
                    val totalCalls = data.incomingTotalCount + data.outgoingTotalCount

                    // Section 1: BEHAVIORAL PATTERN (Title in line 1, text in line 2)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "BEHAVIORAL PATTERN",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = accentCyan
                        )
                        Text(
                            text = "You checked your device ${data.unlockCount} times today. " +
                                "$glanceRatio% were quick glances, " +
                                "while ${data.actualUsageCount} developed into active sessions.",
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = textMuted
                        )
                    }

                    // Section 2: POWER HEALTH (Title in line 1, text in line 2)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "POWER HEALTH",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = accentGreen
                        )
                        val powerMsg = if (data.screenOnDrainRatePctPerHour > 18.0) {
                            "Screen-on battery drain rate ($drainRateStr%/hr) is elevated. Screen brightness & radios may be tuned for endurance."
                        } else {
                            "Battery drain rate ($drainRateStr%/hr) is within optimal power efficiency margins."
                        }
                        Text(
                            text = powerMsg,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = textMuted
                        )
                    }

                    // Section 3: COMMUNICATION LOAD (Title in line 1, text in line 2)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "COMMUNICATION LOAD",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = accentOrange
                        )
                        Text(
                            text = "Handled $totalCalls calls total. " +
                                "${data.incomingMissedCount} missed incoming and " +
                                "${data.outgoingUnconnectedCount} unanswered outgoing calls.",
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = textMuted
                        )
                    }
                }
            }
        }
    }
}

private fun formatDurCompact(millis: Long): String {
    if (millis <= 0L) return "00m 00s"
    val totalSec = millis / 1000L
    val h = totalSec / 3600L
    val m = (totalSec % 3600L) / 60L
    val s = totalSec % 60L
    return when {
        h > 0L -> String.format(Locale.getDefault(), "%02dh %02dm %02ds", h, m, s)
        else -> String.format(Locale.getDefault(), "%02dm %02ds", m, s)
    }
}
