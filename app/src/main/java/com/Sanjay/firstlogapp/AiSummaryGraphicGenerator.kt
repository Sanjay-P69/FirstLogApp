package com.Sanjay.firstlogapp

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Environment
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.sanjay.tracewise.R
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AiSummaryGraphicGenerator {

    fun generateBitmap(
        context: Context,
        data: DailyActivitySummaryData,
        customInsights: List<String>? = null
    ): Bitmap {
        val width = 1280
        val height = 720
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Colors
        val bgDark = 0xFF0F172A.toInt() // Slate 900
        val cardBg = 0xFF1E293B.toInt() // Slate 800
        val cardBorder = 0xFF334155.toInt() // Slate 700
        val textWhite = 0xFFF8FAFC.toInt()
        val textMuted = 0xFF94A3B8.toInt()
        val accentTeal = 0xFF14B8A6.toInt()
        val accentCyan = 0xFF06B6D4.toInt()
        val accentOrange = 0xFFF97316.toInt()
        val accentBlue = 0xFF3B82F6.toInt()
        val accentGreen = 0xFF10B981.toInt()
        val accentPurple = 0xFF8B5CF6.toInt()
        val accentAmber = 0xFFF59E0B.toInt()

        // Background
        canvas.drawColor(bgDark)

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        // ==========================================
        // 1. HEADER SECTION
        // ==========================================
        // Reuse the same brand mark as the launcher and in-app summaries.
        val logoDrawable = ContextCompat.getDrawable(context, R.drawable.ic_firstlog_logo)
        logoDrawable?.let {
            it.setBounds(24, 20, 48, 44)
            it.draw(canvas)
        }

        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 24f
        textPaint.color = textWhite
        val appName = context.getString(R.string.app_name)
        canvas.drawText(appName, 54f, 40f, textPaint)
        val titleWidth = textPaint.measureText(appName)
        val logWidth = 0f

        // Divider
        fillPaint.color = cardBorder
        canvas.drawRect(54f + titleWidth + 14f, 22f, 54f + titleWidth + 16f, 44f, fillPaint)

        // Subtitle: DAILY ACTIVITY SUMMARY REPORT
        textPaint.color = textWhite
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 22f
        canvas.drawText("DAILY ACTIVITY SUMMARY REPORT", 54f + titleWidth + 28f, 40f, textPaint)

        // Subtitle timestamp
        val nowStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textPaint.textSize = 12f
        textPaint.color = textMuted
        canvas.drawText(
            "DATE: ${data.dateStr} | GENERATED: ${nowStr.uppercase(Locale.getDefault())}",
            248f,
            58f,
            textPaint
        )

        // Header Date on Right
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 24f
        textPaint.color = textWhite
        val dateText = data.dateStr
        val dateWidth = textPaint.measureText(dateText)
        canvas.drawText(dateText, width - 24f - dateWidth, 40f, textPaint)

        // ==========================================
        // 2. TOP ROW (4 CARDS)
        // y: 76f to 276f (height = 200f)
        // ==========================================
        val topY = 76f
        val cardHeight = 200f
        val cornerRadius = 14f

        fun drawCardContainer(rect: RectF) {
            fillPaint.color = cardBg
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, fillPaint)
            strokePaint.color = cardBorder
            strokePaint.strokeWidth = 1.5f
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, strokePaint)
        }

        // Card 1: TELEPHONY (x: 24 to 310)
        val card1Rect = RectF(24f, topY, 310f, topY + cardHeight)
        drawCardContainer(card1Rect)
        // Title
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 14f
        textPaint.color = accentTeal
        canvas.drawText("📞  TELEPHONY", 38f, topY + 28f, textPaint)

        // Incoming
        textPaint.color = accentCyan
        textPaint.textSize = 14f
        canvas.drawText("↙", 38f, topY + 62f, textPaint)
        textPaint.color = textWhite
        textPaint.textSize = 15f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("${data.incomingTotalCount} incoming", 58f, topY + 62f, textPaint)
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textPaint.textSize = 11f
        textPaint.color = textMuted
        canvas.drawText(
            "${data.incomingConnectedCount} Connected [${formatDurShort(
                data.incomingConnectedDurSec * 1000L
            )}], ${data.incomingMissedCount} Missed",
            58f,
            topY + 78f,
            textPaint
        )

        // Missed Call Detail
        textPaint.color = accentOrange
        textPaint.textSize = 14f
        canvas.drawText("📵", 38f, topY + 112f, textPaint)
        textPaint.color = textWhite
        textPaint.textSize = 15f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Missed", 58f, topY + 112f, textPaint)
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textPaint.textSize = 11f
        textPaint.color = textMuted
        val lastMissed = data.incomingMissedDetails.firstOrNull() ?: "None recorded"
        val clampedMissed = if (lastMissed.length >
            32
        ) {
            lastMissed.substring(0, 30) + "…"
        } else {
            lastMissed
        }
        canvas.drawText(clampedMissed, 58f, topY + 128f, textPaint)

        // Outgoing
        textPaint.color = accentCyan
        textPaint.textSize = 14f
        canvas.drawText("↗", 38f, topY + 162f, textPaint)
        textPaint.color = textWhite
        textPaint.textSize = 15f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("${data.outgoingTotalCount} outgoing", 58f, topY + 162f, textPaint)
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textPaint.textSize = 11f
        textPaint.color = textMuted
        canvas.drawText(
            "${data.outgoingConnectedCount} Connected [${formatDurShort(
                data.outgoingConnectedDurSec * 1000L
            )}], ${data.outgoingUnconnectedCount} Unanswered",
            58f,
            topY + 178f,
            textPaint
        )

        // Card 2: SCREEN ENGAGEMENT (x: 324 to 790)
        val card2Rect = RectF(324f, topY, 790f, topY + cardHeight)
        drawCardContainer(card2Rect)
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 14f
        textPaint.color = accentTeal
        canvas.drawText("🖥️  SCREEN ENGAGEMENT", 338f, topY + 28f, textPaint)

        // Left Side of Card 2: Screen Time & Unlocks
        textPaint.color = accentCyan
        textPaint.textSize = 18f
        canvas.drawText("⌛", 338f, topY + 72f, textPaint)
        textPaint.color = textWhite
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 18f
        canvas.drawText(formatDurShort(data.screenTimeMs), 366f, topY + 72f, textPaint)
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textPaint.textSize = 11f
        textPaint.color = textMuted
        canvas.drawText("Total Screen Time", 366f, topY + 88f, textPaint)

        textPaint.color = accentCyan
        textPaint.textSize = 18f
        canvas.drawText("🔒", 338f, topY + 140f, textPaint)
        textPaint.color = textWhite
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 18f
        canvas.drawText("${data.unlockCount} times", 366f, topY + 140f, textPaint)
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textPaint.textSize = 11f
        textPaint.color = textMuted
        canvas.drawText("Usage: ${data.actualUsageCount} times", 366f, topY + 158f, textPaint)
        canvas.drawText("Glances: ${data.quickGlanceCount} times", 366f, topY + 174f, textPaint)

        // Right Side of Card 2: Unlock Methods Donut Chart
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 12f
        textPaint.color = textMuted
        canvas.drawText("Unlock Methods", 615f, topY + 45f, textPaint)

        val donutCx = 620f
        val donutCy = topY + 120f
        val donutRadius = 45f
        val donutStroke = 18f
        val donutRect = RectF(
            donutCx - donutRadius,
            donutCy - donutRadius,
            donutCx + donutRadius,
            donutCy + donutRadius
        )

        strokePaint.strokeWidth = donutStroke
        val totalUnlocks =
            (data.unlockPin + data.unlockFace + data.unlockFingerprint).coerceAtLeast(
                1
            )
        var startAngle = -90f

        val pinSweep = (data.unlockPin.toFloat() / totalUnlocks) * 360f
        val faceSweep = (data.unlockFace.toFloat() / totalUnlocks) * 360f
        val fingerSweep = 360f - pinSweep - faceSweep

        // PIN Arc (Blue)
        if (data.unlockPin > 0 || totalUnlocks == 1) {
            strokePaint.color = accentBlue
            canvas.drawArc(donutRect, startAngle, pinSweep.coerceAtLeast(4f), false, strokePaint)
            startAngle += pinSweep
        }
        // Face Arc (Cyan)
        if (data.unlockFace > 0) {
            strokePaint.color = accentCyan
            canvas.drawArc(donutRect, startAngle, faceSweep.coerceAtLeast(4f), false, strokePaint)
            startAngle += faceSweep
        }
        // Fingerprint Arc (Orange)
        if (data.unlockFingerprint > 0) {
            strokePaint.color = accentOrange
            canvas.drawArc(donutRect, startAngle, fingerSweep.coerceAtLeast(4f), false, strokePaint)
        }

        // Legend next to donut chart
        val legendX = 700f
        fun drawLegendItem(ly: Float, color: Int, name: String, count: Int) {
            fillPaint.color = color
            canvas.drawCircle(legendX, ly - 4f, 5f, fillPaint)
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textPaint.textSize = 12f
            textPaint.color = textWhite
            canvas.drawText("$name  ($count)", legendX + 12f, ly, textPaint)
        }
        drawLegendItem(donutCy - 24f, accentBlue, "PIN", data.unlockPin)
        drawLegendItem(donutCy, accentCyan, "Face", data.unlockFace)
        drawLegendItem(donutCy + 24f, accentOrange, "Fingerprint", data.unlockFingerprint)

        // Card 3: CONNECTIVITY (x: 804 to 1030)
        val card3Rect = RectF(804f, topY, 1030f, topY + cardHeight)
        drawCardContainer(card3Rect)
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 14f
        textPaint.color = accentTeal
        canvas.drawText("↔️  CONNECTIVITY", 818f, topY + 28f, textPaint)

        // Wi-Fi
        textPaint.color = accentCyan
        textPaint.textSize = 16f
        canvas.drawText("⇅", 818f, topY + 68f, textPaint)
        textPaint.color = textWhite
        textPaint.textSize = 16f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(
            NetworkDataHelper.formatBytes(data.wifiDataBytes),
            842f,
            topY + 68f,
            textPaint
        )
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textPaint.textSize = 11f
        textPaint.color = textMuted
        canvas.drawText("Wi-Fi (${formatDurShort(data.wifiTimeMs)})", 842f, topY + 84f, textPaint)

        // Mobile Data
        textPaint.color = accentCyan
        textPaint.textSize = 16f
        canvas.drawText("📶", 818f, topY + 120f, textPaint)
        textPaint.color = textWhite
        textPaint.textSize = 16f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(
            NetworkDataHelper.formatBytes(data.mobileDataBytes),
            842f,
            topY + 120f,
            textPaint
        )
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textPaint.textSize = 11f
        textPaint.color = textMuted
        canvas.drawText(
            "Mobile Data (${formatDurShort(data.mobileDataTimeMs)})",
            842f,
            topY + 136f,
            textPaint
        )

        // Bluetooth
        textPaint.color = accentCyan
        textPaint.textSize = 16f
        canvas.drawText("ᛒ", 818f, topY + 172f, textPaint)
        textPaint.color = textWhite
        textPaint.textSize = 16f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(formatDurShort(data.bluetoothTimeMs), 842f, topY + 172f, textPaint)
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textPaint.textSize = 11f
        textPaint.color = textMuted
        canvas.drawText("Bluetooth", 842f, topY + 188f, textPaint)

        // Card 4: BATTERY (x: 1044 to 1256)
        val card4Rect = RectF(1044f, topY, 1256f, topY + cardHeight)
        drawCardContainer(card4Rect)
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 14f
        textPaint.color = accentTeal
        canvas.drawText("🔋  BATTERY", 1058f, topY + 28f, textPaint)

        // Total Discharged
        textPaint.color = accentOrange
        textPaint.textSize = 16f
        canvas.drawText("🪫", 1058f, topY + 68f, textPaint)
        textPaint.color = textWhite
        textPaint.textSize = 16f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("${data.batteryUsedPct}%", 1082f, topY + 68f, textPaint)
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textPaint.textSize = 11f
        textPaint.color = textMuted
        canvas.drawText("Total Discharged", 1082f, topY + 84f, textPaint)

        // Screen On Drain Rate
        textPaint.color = accentGreen
        textPaint.textSize = 16f
        canvas.drawText("⚡", 1058f, topY + 120f, textPaint)
        textPaint.color = textWhite
        textPaint.textSize = 16f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(
            "${String.format(
                Locale.getDefault(),
                "%.1f",
                data.screenOnDrainRatePctPerHour
            )}% / hour",
            1082f,
            topY + 120f,
            textPaint
        )
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textPaint.textSize = 11f
        textPaint.color = textMuted
        canvas.drawText("Screen On Drain", 1082f, topY + 136f, textPaint)

        // Charging
        textPaint.color = accentGreen
        textPaint.textSize = 16f
        canvas.drawText("🔌", 1058f, topY + 172f, textPaint)
        textPaint.color = textWhite
        textPaint.textSize = 16f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(formatDurShort(data.chargingTimeMs), 1082f, topY + 172f, textPaint)
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textPaint.textSize = 11f
        textPaint.color = textMuted
        canvas.drawText(
            "Charging (${data.chargingSessionCount} sessions)",
            1082f,
            topY + 188f,
            textPaint
        )

        // ==========================================
        // 3. MIDDLE SECTION: APPLICATION ENGAGEMENT (TOP APPS)
        // y: 290f to 560f (height = 270f)
        // ==========================================
        val midY = 290f
        val midHeight = 270f
        val midRect = RectF(24f, midY, width - 24f, midY + midHeight)
        drawCardContainer(midRect)

        // Centered Title
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 15f
        textPaint.color = textWhite
        val midTitle = "APPLICATION ENGAGEMENT (TOP APPS)"
        val midTitleW = textPaint.measureText(midTitle)
        canvas.drawText(midTitle, (width - midTitleW) / 2f, midY + 26f, textPaint)

        // Apps data breakdown
        val apps = data.topApps.take(7)
        val totalAppTime = apps.sumOf { it.second }.coerceAtLeast(1L)
        val appColors = intArrayOf(
            0xFF10B981.toInt(), // Emerald Green
            0xFF06B6D4.toInt(), // Cyan
            0xFFF59E0B.toInt(), // Amber
            0xFFF97316.toInt(), // Orange
            0xFF8B5CF6.toInt(), // Purple
            0xFF3B82F6.toInt(), // Blue
            0xFF14B8A6.toInt() // Teal
        )

        // Central Donut Chart
        val appDonutCx = width / 2f
        val appDonutCy = midY + 150f
        val appDonutRadius = 65f
        val appDonutStroke = 28f
        val appDonutRect = RectF(
            appDonutCx - appDonutRadius,
            appDonutCy - appDonutRadius,
            appDonutCx + appDonutRadius,
            appDonutCy + appDonutRadius
        )

        strokePaint.strokeWidth = appDonutStroke
        var appStartAngle = -90f

        if (apps.isEmpty()) {
            strokePaint.color = 0xFF475569.toInt()
            canvas.drawArc(appDonutRect, 0f, 360f, false, strokePaint)
        } else {
            apps.forEachIndexed { i, (_, dur) ->
                val slice = (dur.toFloat() / totalAppTime) * 360f
                strokePaint.color = appColors[i % appColors.size]
                canvas.drawArc(
                    appDonutRect,
                    appStartAngle,
                    slice.coerceAtLeast(2f),
                    false,
                    strokePaint
                )
                appStartAngle += slice
            }
        }

        // Left Column (Top 3 Apps)
        val leftApps = apps.take(3)
        var leftItemY = midY + 75f
        leftApps.forEachIndexed { index, (name, dur) ->
            val color = appColors[index % appColors.size]
            fillPaint.color = color
            canvas.drawRoundRect(250f, leftItemY - 14f, 280f, leftItemY + 16f, 8f, 8f, fillPaint)
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.textSize = 12f
            textPaint.color = 0xFF0F172A.toInt()
            val initial = name.firstOrNull()?.uppercase() ?: "A"
            canvas.drawText(initial, 260f, leftItemY + 6f, textPaint)

            textPaint.color = textWhite
            textPaint.textSize = 14f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(name, 292f, leftItemY, textPaint)

            textPaint.color = textMuted
            textPaint.textSize = 11f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val pct = (dur.toFloat() / totalAppTime) * 100f
            canvas.drawText(
                "${formatDurShort(dur)} (${String.format(Locale.getDefault(), "%.1f%%", pct)})",
                292f,
                leftItemY + 16f,
                textPaint
            )

            leftItemY += 56f
        }

        // Right Column (Apps 4 to 7)
        val rightApps = apps.drop(3).take(4)
        var rightItemY = midY + 65f
        rightApps.forEachIndexed { index, (name, dur) ->
            val realIdx = index + 3
            val color = appColors[realIdx % appColors.size]
            fillPaint.color = color
            canvas.drawRoundRect(800f, rightItemY - 14f, 830f, rightItemY + 16f, 8f, 8f, fillPaint)
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.textSize = 12f
            textPaint.color = 0xFF0F172A.toInt()
            val initial = name.firstOrNull()?.uppercase() ?: "A"
            canvas.drawText(initial, 810f, rightItemY + 6f, textPaint)

            textPaint.color = textWhite
            textPaint.textSize = 14f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(name, 842f, rightItemY, textPaint)

            textPaint.color = textMuted
            textPaint.textSize = 11f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val pct = (dur.toFloat() / totalAppTime) * 100f
            canvas.drawText(
                "${formatDurShort(dur)} (${String.format(Locale.getDefault(), "%.1f%%", pct)})",
                842f,
                rightItemY + 16f,
                textPaint
            )

            rightItemY += 46f
        }

        // ==========================================
        // 4. BOTTOM BANNER: AI EXECUTIVE SUMMARY & PRODUCTIVITY INSIGHTS
        // y: 574f to 704f (height = 130f)
        // ==========================================
        val botY = 574f
        val botHeight = 130f
        val botRect = RectF(24f, botY, width - 24f, botY + botHeight)
        drawCardContainer(botRect)

        // Banner Title
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 14f
        textPaint.color = textWhite
        val botTitle = "💡 AI EXECUTIVE SUMMARY & PRODUCTIVITY INSIGHTS 💡   🎉 ✦"
        val botTitleW = textPaint.measureText(botTitle)
        canvas.drawText(botTitle, (width - botTitleW) / 2f, botY + 26f, textPaint)

        // Insights Lines
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

        val line1Prefix = "BEHAVIORAL PATTERN: "
        val line1Body = "You checked your device ${data.unlockCount} times today. " +
            "$glanceRatio% were quick lock-screen glances, " +
            "while ${data.actualUsageCount} developed into active work sessions."

        val line2Prefix = "POWER HEALTH: "
        val line2Body = if (data.screenOnDrainRatePctPerHour > 18.0) {
            "Screen-on battery drain rate ($drainRateStr%/hr) is elevated. Screen brightness & radios may be tuned for endurance."
        } else {
            "Battery drain rate ($drainRateStr%/hr) is within optimal power efficiency margins."
        }

        val line3Prefix = "COMMUNICATION LOAD: "
        val line3Body = "Handled $totalCalls calls total. " +
            "${data.incomingMissedCount} incoming " +
            "call${if (data.incomingMissedCount == 1) "" else "s"} missed and " +
            "${data.outgoingUnconnectedCount} outgoing calls remained unanswered."

        fun drawInsightLine(ly: Float, prefix: String, body: String) {
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.textSize = 12f
            textPaint.color = textWhite
            canvas.drawText(prefix, 42f, ly, textPaint)
            val pW = textPaint.measureText(prefix)
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textPaint.color = textMuted
            canvas.drawText(body, 42f + pW, ly, textPaint)
        }

        drawInsightLine(botY + 54f, line1Prefix, line1Body)
        drawInsightLine(botY + 78f, line2Prefix, line2Body)
        drawInsightLine(botY + 102f, line3Prefix, line3Body)

        return bitmap
    }

    fun saveBitmapToFile(context: Context, bitmap: Bitmap, dateStr: String): File? = try {
        val picturesDir =
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.filesDir
        val file = File(picturesDir, "FirstLog_Summary_$dateStr.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        file
    } catch (_: Exception) {
        null
    }

    fun getShareIntent(context: Context, imageFile: File): Intent {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(
                Intent.EXTRA_SUBJECT,
                context.getString(R.string.summary_share_subject, imageFile.name)
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun formatDurShort(millis: Long): String {
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
