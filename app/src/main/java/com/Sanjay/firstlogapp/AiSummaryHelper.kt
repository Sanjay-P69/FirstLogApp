package com.Sanjay.firstlogapp

import android.content.Context
import android.os.Environment
import com.sanjay.tracewise.R
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class DailyActivitySummaryData(
    val dateStr: String,
    val screenTimeMs: Long,
    val unlockCount: Int,
    val actualUsageCount: Int,
    val quickGlanceCount: Int,
    val unlockFingerprint: Int,
    val unlockFace: Int,
    val unlockPin: Int,
    val wifiTimeMs: Long,
    val wifiSsid: String,
    val wifiDataBytes: Long,
    val wifiSessionCount: Int,
    val mobileDataTimeMs: Long,
    val mobileDataBytes: Long,
    val mobileDataSessionCount: Int,
    val bothWifiDataMs: Long,
    val bothWifiDataCount: Int,
    val bluetoothTimeMs: Long,
    val btBudsMs: Long,
    val btWatchMs: Long,
    val btAutoMs: Long,
    val gpsTimeMs: Long,
    val batteryUsedPct: Int,
    val screenOnDrainRatePctPerHour: Double,
    val chargingTimeMs: Long,
    val chargingSessionCount: Int,
    val chargingSessionsDetails: List<String>,
    val incomingTotalCount: Int,
    val incomingConnectedCount: Int,
    val incomingConnectedDurSec: Long,
    val incomingMissedCount: Int,
    val incomingMissedDetails: List<String>,
    val outgoingTotalCount: Int,
    val outgoingConnectedCount: Int,
    val outgoingConnectedDurSec: Long,
    val outgoingUnconnectedCount: Int,
    val outgoingUnconnectedDetails: List<String>,
    // App name to time in milliseconds.
    val topApps: List<Pair<String, Long>>
)

object AiSummaryHelper {

    suspend fun generateDailySummary(
        context: Context,
        data: DailyActivitySummaryData,
        geminiApiKey: String?
    ): String = withContext(Dispatchers.IO) {
        val apiKey = geminiApiKey?.trim() ?: ""
        if (apiKey.isNotEmpty()) {
            try {
                val aiText = callGeminiApi(context, data, apiKey)
                if (aiText.isNotBlank()) {
                    return@withContext aiText
                }
            } catch (_: Exception) {
                // Fall back to built-in generator
            }
        }
        return@withContext generateLocalSummary(context, data)
    }

    private fun callGeminiApi(
        context: Context,
        data: DailyActivitySummaryData,
        apiKey: String
    ): String {
        val prompt = buildPromptForGemini(context, data)
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/" +
            "gemini-1.5-flash:generateContent?key=$apiKey"
        val url = URL(endpoint)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        conn.doOutput = true
        conn.connectTimeout = 15000
        conn.readTimeout = 25000

        val requestBody = JSONObject().apply {
            val contentsArray = JSONArray().apply {
                val partObj = JSONObject().apply {
                    put(
                        "parts",
                        JSONArray().apply {
                            put(
                                JSONObject().apply {
                                    put("text", prompt)
                                }
                            )
                        }
                    )
                }
                put(partObj)
            }
            put("contents", contentsArray)
        }

        OutputStreamWriter(conn.outputStream, "UTF-8").use { os ->
            os.write(requestBody.toString())
            os.flush()
        }

        val code = conn.responseCode
        val inputStream = if (code in 200..299) conn.inputStream else conn.errorStream
        val response = BufferedReader(InputStreamReader(inputStream, "UTF-8")).use { it.readText() }

        if (code in 200..299) {
            val json = JSONObject(response)
            val candidates = json.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val first = candidates.getJSONObject(0)
                val content = first.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    return parts.getJSONObject(0).optString("text", "")
                }
            }
        }
        return ""
    }

    private fun buildPromptForGemini(context: Context, data: DailyActivitySummaryData): String = """
You are ${context.getString(R.string.app_name)}'s AI Activity Analyst.
Generate an executive daily activity summary report for the user based strictly on the tracked metrics below.
You MUST format the output according to the exact structure specified below. Include clear numbers, durations, call breakdown (connected vs missed/unconnected), connectivity, battery drain rate, and provide an insightful 2-3 paragraph 'AI Executive Insights & Digital Wellness' analysis at the end.

METRICS FOR ${data.dateStr}:
1. Calls:
- Incoming: ${data.incomingTotalCount} total, ${data.incomingConnectedCount} connected (${formatDur(
        data.incomingConnectedDurSec * 1000L
    )}), ${data.incomingMissedCount} missed/rejected. Missed details: ${data.incomingMissedDetails.joinToString(
        ", "
    ).ifBlank {
        "None"
    }}
- Outgoing: ${data.outgoingTotalCount} total, ${data.outgoingConnectedCount} connected (${formatDur(
        data.outgoingConnectedDurSec * 1000L
    )}), ${data.outgoingUnconnectedCount} unconnected/unanswered. Unconnected details: ${data.outgoingUnconnectedDetails.joinToString(
        ", "
    ).ifBlank {
        "None"
    }}
2. Screen & Unlocks:
- Screen On Time: ${formatDur(data.screenTimeMs)}
- Total Unlocks: ${data.unlockCount} (Actual Usage Sessions: ${data.actualUsageCount}, Quick Lock Screen Glances: ${data.quickGlanceCount})
- Unlock Types: Fingerprint: ${data.unlockFingerprint}, Face: ${data.unlockFace}, PIN: ${data.unlockPin}
3. Connectivity & Data:
- Wi-Fi: ${formatDur(
        data.wifiTimeMs
    )} (SSID: ${data.wifiSsid}), Consumed: ${NetworkDataHelper.formatBytes(
        data.wifiDataBytes
    )}, Sessions: ${data.wifiSessionCount}
- Mobile Data: ${formatDur(
        data.mobileDataTimeMs
    )}, Consumed: ${NetworkDataHelper.formatBytes(
        data.mobileDataBytes
    )}, Sessions: ${data.mobileDataSessionCount}
- Simultaneous Wi-Fi & Mobile Data: ${formatDur(
        data.bothWifiDataMs
    )} (${data.bothWifiDataCount} times)
- Bluetooth: ${formatDur(
        data.bluetoothTimeMs
    )} (Buds: ${formatDur(
        data.btBudsMs
    )}, Watch: ${formatDur(data.btWatchMs)}, Auto: ${formatDur(data.btAutoMs)})
- GPS: ${formatDur(data.gpsTimeMs)}
4. Battery:
- Battery Dropped: ${data.batteryUsedPct}%
- Screen On Drain Rate: ${String.format(
        Locale.getDefault(),
        "%.1f",
        data.screenOnDrainRatePctPerHour
    )}% / hour
- Charging: ${formatDur(
        data.chargingTimeMs
    )} (${data.chargingSessionCount} sessions). Log: ${data.chargingSessionsDetails.joinToString(
        "; "
    ).ifBlank {
        "None"
    }}
5. Top Apps Used:
${data.topApps.take(6).joinToString("\n") {
        "• ${it.first}: ${formatDur(it.second)}"
    }.ifBlank { "None recorded" }}

FORMAT REQUIREMENT:
==================================================
           ${context.getString(R.string.summary_report_title)}
==================================================
Date: ${data.dateStr}
Generated: [Time]
AI Engine: Google Gemini AI

--------------------------------------------------
1. TELEPHONY & CALL METRICS
--------------------------------------------------
[Details as specified above]

--------------------------------------------------
2. SCREEN & UNLOCK ENGAGEMENT
--------------------------------------------------
[Details as specified above]

--------------------------------------------------
3. NETWORK & CONNECTIVITY USAGE
--------------------------------------------------
[Details as specified above]

--------------------------------------------------
4. BATTERY & POWER EFFICIENCY
--------------------------------------------------
[Details as specified above]

--------------------------------------------------
5. APPLICATION ENGAGEMENT (TOP APPS)
--------------------------------------------------
[Details as specified above]

--------------------------------------------------
6. AI EXECUTIVE SUMMARY & PRODUCTIVITY INSIGHTS
--------------------------------------------------
[Personalized AI analysis covering communication balance, glance vs focus sessions, power drain efficiency, and digital well-being recommendation]
==================================================
    """.trimIndent()

    fun generateLocalSummary(context: Context, data: DailyActivitySummaryData): String {
        val nowStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
        val sb = StringBuilder()
        sb.appendLine("==================================================")
        sb.appendLine(context.getString(R.string.summary_report_title))
        sb.appendLine("==================================================")
        sb.appendLine("Date: ${data.dateStr}")
        sb.appendLine("Generated: $nowStr")
        sb.appendLine(context.getString(R.string.local_summary_engine))
        sb.appendLine()

        sb.appendLine("--------------------------------------------------")
        sb.appendLine("1. TELEPHONY & CALL METRICS")
        sb.appendLine("--------------------------------------------------")
        sb.appendLine("• Incoming Calls:")
        sb.appendLine("  - Total: ${data.incomingTotalCount}")
        sb.appendLine(
            "  - Connected: ${data.incomingConnectedCount} (${formatDur(
                data.incomingConnectedDurSec * 1000L
            )})"
        )
        sb.appendLine("  - Missed / Rejected: ${data.incomingMissedCount}")
        if (data.incomingMissedDetails.isNotEmpty()) {
            sb.appendLine("  - Missed Call Details:")
            data.incomingMissedDetails.take(5).forEach { sb.appendLine("    * $it") }
        }
        sb.appendLine("• Outgoing Calls:")
        sb.appendLine("  - Total: ${data.outgoingTotalCount}")
        sb.appendLine(
            "  - Connected: ${data.outgoingConnectedCount} (${formatDur(
                data.outgoingConnectedDurSec * 1000L
            )})"
        )
        sb.appendLine("  - Unconnected / Unanswered: ${data.outgoingUnconnectedCount}")
        if (data.outgoingUnconnectedDetails.isNotEmpty()) {
            sb.appendLine("  - Unconnected Outgoing Details:")
            data.outgoingUnconnectedDetails.take(5).forEach { sb.appendLine("    * $it") }
        }
        sb.appendLine()

        sb.appendLine("--------------------------------------------------")
        sb.appendLine("2. SCREEN & UNLOCK ENGAGEMENT")
        sb.appendLine("--------------------------------------------------")
        sb.appendLine("• Total Screen Time: ${formatDur(data.screenTimeMs)}")
        sb.appendLine("• Total Screen Unlocks: ${data.unlockCount} times")
        sb.appendLine("  - Actual Usage Sessions: ${data.actualUsageCount} times")
        sb.appendLine("  - Quick Lock Screen Glances: ${data.quickGlanceCount} times")
        sb.appendLine(
            "• Unlock Methods: Fingerprint: ${data.unlockFingerprint} • Face: ${data.unlockFace} • PIN: ${data.unlockPin}"
        )
        sb.appendLine()

        sb.appendLine("--------------------------------------------------")
        sb.appendLine("3. NETWORK & CONNECTIVITY USAGE")
        sb.appendLine("--------------------------------------------------")
        sb.appendLine("• Wi-Fi Activity:")
        sb.appendLine("  - Active Duration: ${formatDur(data.wifiTimeMs)}")
        sb.appendLine("  - Connected SSID: ${data.wifiSsid}")
        sb.appendLine("  - Data Consumed: ${NetworkDataHelper.formatBytes(data.wifiDataBytes)}")
        sb.appendLine(
            "  - Sessions: ${data.wifiSessionCount} (${NetworkDataHelper.formatUsagePerSession(
                data.wifiDataBytes,
                data.wifiSessionCount
            )})"
        )
        sb.appendLine("• Mobile Data Activity:")
        sb.appendLine("  - Active Duration: ${formatDur(data.mobileDataTimeMs)}")
        sb.appendLine("  - Data Consumed: ${NetworkDataHelper.formatBytes(data.mobileDataBytes)}")
        sb.appendLine(
            "  - Sessions: ${data.mobileDataSessionCount} (" +
                "${NetworkDataHelper.formatUsagePerSession(
                    data.mobileDataBytes,
                    data.mobileDataSessionCount
                )})"
        )
        sb.appendLine(
            "• Dual Connection (Both Wi-Fi & Data active): ${formatDur(
                data.bothWifiDataMs
            )} (${data.bothWifiDataCount} occurrences)"
        )
        sb.appendLine("• Bluetooth Active: ${formatDur(data.bluetoothTimeMs)}")
        sb.appendLine(
            "  - Audio/Buds: ${formatDur(
                data.btBudsMs
            )} | Smartwatch: ${formatDur(data.btWatchMs)} | Car/Auto: ${formatDur(data.btAutoMs)}"
        )
        sb.appendLine("• GPS / Location Active: ${formatDur(data.gpsTimeMs)}")
        sb.appendLine()

        sb.appendLine("--------------------------------------------------")
        sb.appendLine("4. BATTERY & POWER EFFICIENCY")
        sb.appendLine("--------------------------------------------------")
        sb.appendLine("• Total Battery Discharged: ${data.batteryUsedPct}%")
        val drainRateStr = String.format(
            Locale.getDefault(),
            "%.1f",
            data.screenOnDrainRatePctPerHour
        )
        sb.appendLine("• Screen On Drain Rate: $drainRateStr% / hour")
        sb.appendLine("• Charging Activity:")
        sb.appendLine("  - Total Charging Time: ${formatDur(data.chargingTimeMs)}")
        sb.appendLine("  - Charging Sessions: ${data.chargingSessionCount} sessions")
        if (data.chargingSessionsDetails.isNotEmpty()) {
            data.chargingSessionsDetails.forEach { sb.appendLine("    * $it") }
        }
        sb.appendLine()

        sb.appendLine("--------------------------------------------------")
        sb.appendLine("5. APPLICATION ENGAGEMENT (TOP APPS)")
        sb.appendLine("--------------------------------------------------")
        if (data.topApps.isNotEmpty()) {
            data.topApps.take(6).forEach { app ->
                sb.appendLine("• ${app.first}: ${formatDur(app.second)}")
            }
        } else {
            sb.appendLine("• No foreground app statistics recorded yet.")
        }
        sb.appendLine()

        sb.appendLine("--------------------------------------------------")
        sb.appendLine("6. AI EXECUTIVE SUMMARY & PRODUCTIVITY INSIGHTS")
        sb.appendLine("--------------------------------------------------")
        val glanceRatio = if (data.unlockCount >
            0
        ) {
            ((data.quickGlanceCount.toDouble() / data.unlockCount) * 100).toInt()
        } else {
            0
        }
        sb.appendLine(
            "• Behavioral Pattern: You checked your device ${data.unlockCount} times today. $glanceRatio% were quick lock-screen glances, while ${data.actualUsageCount} developed into active work sessions."
        )
        if (data.screenOnDrainRatePctPerHour > 18.0) {
            sb.appendLine(
                "• Power Health: Your screen-on battery drain rate is currently elevated ($drainRateStr%/hr). Reducing screen brightness and background radio usage may extend endurance."
            )
        } else {
            sb.appendLine(
                "• Power Health: Battery drain rate ($drainRateStr%/hr) is within optimal power efficiency margins."
            )
        }
        val callTotal = data.incomingTotalCount + data.outgoingTotalCount
        sb.appendLine(
            "• Communication Load: Handled $callTotal calls total. ${data.incomingMissedCount} incoming calls were missed and ${data.outgoingUnconnectedCount} outgoing calls remained unanswered."
        )
        sb.appendLine("==================================================")

        return sb.toString()
    }

    fun saveReportToFile(
        context: Context,
        reportContent: String,
        dateStr: String,
        googleAccount: String?
    ): String = try {
        val docsDir =
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
        val prefix = if (!googleAccount.isNullOrBlank()) "Drive_Report" else "Local_Report"
        val file = File(docsDir, "FirstLogApp_${prefix}_$dateStr.txt")
        file.writeText(reportContent)
        file.absolutePath
    } catch (_: Exception) {
        ""
    }

    private fun formatDur(millis: Long): String {
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
}
