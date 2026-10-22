package com.sanjay.tracewise.ui.summary

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.Sanjay.firstlogapp.AiSummaryGraphicGenerator
import com.Sanjay.firstlogapp.AiSummaryGraphicReportView
import com.Sanjay.firstlogapp.AiSummaryHelper
import com.Sanjay.firstlogapp.BluetoothCategoryHelper
import com.Sanjay.firstlogapp.DailyActivitySummaryData
import com.Sanjay.firstlogapp.NetworkDataHelper
import com.sanjay.tracewise.R
import com.sanjay.tracewise.core.formatDurationMillis
import com.sanjay.tracewise.core.formatTime
import com.sanjay.tracewise.data.device.AppUsageHelper
import com.sanjay.tracewise.data.device.CallLogHelper
import com.sanjay.tracewise.data.local.DataManager
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AiSummaryDialog(dataManager: DataManager, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var summaryText by remember { mutableStateOf("") }
    var loadedSummaryData by remember { mutableStateOf<DailyActivitySummaryData?>(null) }
    var viewMode by remember { mutableStateOf("Graphic") } // "Graphic", "Both", "Text"
    var saveStatus by remember { mutableStateOf("") }

    val createDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(summaryText.toByteArray())
                }
                saveStatus = "Saved text report to chosen location!"
                Toast.makeText(context, saveStatus, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                saveStatus = "Failed to save: ${e.message}"
                Toast.makeText(context, saveStatus, Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val dateStr = dataManager.getCurrentCycleDate()
            val screenMs = dataManager.getTodayTotalMetricTime("screen")
            val unlockCount = dataManager.getTodayUnlockCount()
            val actualCount = dataManager.getTodayActualUsageCount()
            val glanceCount = dataManager.getTodayQuickGlanceCount()
            val printCount = dataManager.getTodayUnlockFingerprint()
            val faceCount = dataManager.getTodayUnlockFace()
            val pinCount = dataManager.getTodayUnlockPin()
            val wifiMs = dataManager.getTodayTotalMetricTime("wifi")
            val wifiSsid = NetworkDataHelper.getWifiSsid(context)
            val wifiBytes = NetworkDataHelper.getTodayWifiBytes(
                context,
                dataManager.getCycleStartTimestamp()
            )
            val wifiSessions = dataManager.getTodayMetricCount("wifi")
            val mobileMs = dataManager.getTodayTotalMetricTime("data")
            val mobileBytes = NetworkDataHelper.getTodayMobileDataBytes(
                context,
                dataManager.getCycleStartTimestamp()
            )
            val mobileSessions = dataManager.getTodayMetricCount("data")
            val dualMs = dataManager.getTodayBothWifiDataTime()
            val dualCount = dataManager.getTodayBothWifiDataCount()
            val btMs = dataManager.getTodayTotalMetricTime("bluetooth")
            val btBuds = dataManager.getTodayBtCategoryDuration(BluetoothCategoryHelper.CAT_BUDS)
            val btWatch = dataManager.getTodayBtCategoryDuration(BluetoothCategoryHelper.CAT_WATCH)
            val btAuto = dataManager.getTodayBtCategoryDuration(BluetoothCategoryHelper.CAT_AUTO)
            val gpsMs = dataManager.getTodayTotalMetricTime("gps")
            val batteryDrop = dataManager.getTodayBatteryUsed()
            val drainRate = dataManager.getScreenOnDrainRate()
            val chargeMs = dataManager.getTodayChargingTime()
            val chargeCount = dataManager.getTodayChargingSessionCount()
            val chargeDetails = dataManager.getTodayChargingSessions().map {
                val gain = it.endLevel - it.startLevel
                val gainStr = if (gain >= 0) "+$gain%" else "$gain%"
                "${formatTime(
                    it.startTime
                )} - ${formatTime(it.endTime)} (${formatDurationMillis(it.durationMs)}, $gainStr)"
            }

            var inTotal = 0
            var inConn = 0
            var inConnDur = 0L
            var inMissed = 0
            val inMissedDetails = mutableListOf<String>()

            var outTotal = 0
            var outConn = 0
            var outConnDur = 0L
            var outUnconn = 0
            val outUnconnDetails = mutableListOf<String>()

            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                val inCalls = CallLogHelper.readCalls(context, true).filter {
                    CallLogHelper.isCallInCurrentCycle(it.timestamp, dataManager)
                }
                val connectedIn = CallLogHelper.getConnectedCalls(inCalls, true)
                val missedIn = CallLogHelper.getMissedCalls(inCalls, true)
                inTotal = inCalls.size
                inConn = connectedIn.size
                inConnDur = connectedIn.sumOf { it.durationSeconds }
                inMissed = missedIn.size
                missedIn.take(5).forEach {
                    val name = it.name.ifBlank { it.number }
                    inMissedDetails.add("$name (${formatTime(it.timestamp)})")
                }

                val outCalls = CallLogHelper.readCalls(context, false).filter {
                    CallLogHelper.isCallInCurrentCycle(it.timestamp, dataManager)
                }
                val connectedOut = CallLogHelper.getConnectedCalls(outCalls, false)
                val missedOut = CallLogHelper.getMissedCalls(outCalls, false)
                outTotal = outCalls.size
                outConn = connectedOut.size
                outConnDur = connectedOut.sumOf { it.durationSeconds }
                outUnconn = missedOut.size
                missedOut.take(5).forEach {
                    val name = it.name.ifBlank { it.number }
                    outUnconnDetails.add("$name (${formatTime(it.timestamp)})")
                }
            }

            val topAppsList = mutableListOf<Pair<String, Long>>()
            if (AppUsageHelper.hasPermission(context)) {
                val apps = AppUsageHelper.getTodayUsageStats(
                    context,
                    dataManager,
                    dataManager.getCycleStartTimestamp()
                )
                apps.take(7).forEach {
                    topAppsList.add(Pair(it.appName, it.usageTimeMs))
                }
            }

            val summaryData = DailyActivitySummaryData(
                dateStr = dateStr,
                screenTimeMs = screenMs,
                unlockCount = unlockCount,
                actualUsageCount = actualCount,
                quickGlanceCount = glanceCount,
                unlockFingerprint = printCount,
                unlockFace = faceCount,
                unlockPin = pinCount,
                wifiTimeMs = wifiMs,
                wifiSsid = wifiSsid,
                wifiDataBytes = wifiBytes,
                wifiSessionCount = wifiSessions,
                mobileDataTimeMs = mobileMs,
                mobileDataBytes = mobileBytes,
                mobileDataSessionCount = mobileSessions,
                bothWifiDataMs = dualMs,
                bothWifiDataCount = dualCount,
                bluetoothTimeMs = btMs,
                btBudsMs = btBuds,
                btWatchMs = btWatch,
                btAutoMs = btAuto,
                gpsTimeMs = gpsMs,
                batteryUsedPct = batteryDrop,
                screenOnDrainRatePctPerHour = drainRate,
                chargingTimeMs = chargeMs,
                chargingSessionCount = chargeCount,
                chargingSessionsDetails = chargeDetails,
                incomingTotalCount = inTotal,
                incomingConnectedCount = inConn,
                incomingConnectedDurSec = inConnDur,
                incomingMissedCount = inMissed,
                incomingMissedDetails = inMissedDetails,
                outgoingTotalCount = outTotal,
                outgoingConnectedCount = outConn,
                outgoingConnectedDurSec = outConnDur,
                outgoingUnconnectedCount = outUnconn,
                outgoingUnconnectedDetails = outUnconnDetails,
                topApps = topAppsList
            )

            loadedSummaryData = summaryData

            val generated = AiSummaryHelper.generateDailySummary(
                context = context,
                data = summaryData,
                geminiApiKey = dataManager.getGeminiApiKey()
            )
            summaryText = generated
            isLoading = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.96f),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_firstlog_logo),
                            contentDescription = stringResource(R.string.brand_logo_description),
                            tint = Color.Unspecified,
                            modifier = Modifier.size(20.dp)
                        )
                        Text("AI Daily Summary", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    val isGemini = dataManager.getGeminiApiKey().isNotBlank()
                    Text(
                        text = if (isGemini) "Gemini Flash AI" else "Smart Engine",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // 2 Option Toggles: Graphic, Text
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = viewMode == "Graphic",
                        onClick = { viewMode = "Graphic" },
                        label = {
                            Text("🎨 Graphic", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = viewMode == "Text",
                        onClick = { viewMode = "Text" },
                        label = {
                            Text("📝 Text", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        text = {
            if (isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        "Synthesizing daily activity...",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Analyzing telephony, screen usage, power & connectivity",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 520.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val summaryData = loadedSummaryData
                    if (summaryData != null && viewMode == "Graphic") {
                        AiSummaryGraphicReportView(data = summaryData)
                    }

                    if (viewMode == "Text") {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = summaryText,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // 1. Share Graphic PNG
                    IconButton(
                        onClick = {
                            val data = loadedSummaryData
                            if (data != null) {
                                val bmp = AiSummaryGraphicGenerator.generateBitmap(context, data)
                                val file = AiSummaryGraphicGenerator.saveBitmapToFile(
                                    context,
                                    bmp,
                                    data.dateStr
                                )
                                if (file != null) {
                                    val intent = AiSummaryGraphicGenerator.getShareIntent(
                                        context,
                                        file
                                    )
                                    context.startActivity(
                                        Intent.createChooser(intent, "Share Graphic Report Image")
                                    )
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Could not generate report image",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        enabled = !isLoading && loadedSummaryData != null
                    ) {
                        Text("📸", fontSize = 16.sp)
                    }

                    // 2. Share Text
                    IconButton(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, summaryText)
                                putExtra(
                                    Intent.EXTRA_SUBJECT,
                                    context.getString(
                                        R.string.summary_share_subject,
                                        dataManager.getCurrentCycleDate()
                                    )
                                )
                                type = "text/plain"
                            }
                            context.startActivity(
                                Intent.createChooser(sendIntent, "Share Daily Summary Text")
                            )
                        },
                        enabled = !isLoading && summaryText.isNotBlank()
                    ) {
                        Text("📤", fontSize = 16.sp)
                    }

                    // 4. Save Text File
                    IconButton(
                        onClick = {
                            val dateStr = dataManager.getCurrentCycleDate()
                            createDocLauncher.launch("FirstLogApp_Summary_$dateStr.txt")
                        },
                        enabled = !isLoading && summaryText.isNotBlank()
                    ) {
                        Text("💾", fontSize = 16.sp)
                    }

                    // 5. Copy Text
                    IconButton(
                        onClick = {
                            val cm = context.getSystemService(
                                Context.CLIPBOARD_SERVICE
                            ) as? ClipboardManager
                            cm?.setPrimaryClip(
                                ClipData.newPlainText("Daily AI Summary", summaryText)
                            )
                            Toast.makeText(
                                context,
                                "Summary text copied to clipboard!",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        enabled = !isLoading && summaryText.isNotBlank()
                    ) {
                        Text("📋", fontSize = 16.sp)
                    }
                }

                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )
}
