package com.Sanjay.firstlogapp.data.device

import com.Sanjay.firstlogapp.data.local.DataManager
import com.Sanjay.firstlogapp.data.model.CallRecord
import com.Sanjay.firstlogapp.data.model.CallerStat

import android.content.Context
import android.provider.CallLog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object CallLogHelper {
    fun readCalls(context: Context, isIncoming: Boolean): List<CallRecord> {
        val list = mutableListOf<CallRecord>()
        val selection = if (isIncoming) {
            "${CallLog.Calls.TYPE} IN (${CallLog.Calls.INCOMING_TYPE}, ${CallLog.Calls.MISSED_TYPE}, ${CallLog.Calls.REJECTED_TYPE})"
        } else {
            "${CallLog.Calls.TYPE} = ${CallLog.Calls.OUTGOING_TYPE}"
        }
        val projection = arrayOf(
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.NUMBER,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION,
            CallLog.Calls.TYPE
        )
        try {
            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI, projection, selection, null, "${CallLog.Calls.DATE} DESC"
            )
            cursor?.use {
                val nIdx = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
                val numIdx = it.getColumnIndex(CallLog.Calls.NUMBER)
                val dIdx = it.getColumnIndex(CallLog.Calls.DATE)
                val durIdx = it.getColumnIndex(CallLog.Calls.DURATION)
                val typeIdx = it.getColumnIndex(CallLog.Calls.TYPE)
                while (it.moveToNext()) {
                    val rawName = if (nIdx != -1) it.getString(nIdx) else null
                    val number = if (numIdx != -1) it.getString(numIdx) ?: "Unknown" else "Unknown"
                    val date = if (dIdx != -1) it.getLong(dIdx) else 0L
                    val duration = if (durIdx != -1) it.getLong(durIdx) else 0L
                    val type = if (typeIdx != -1) it.getInt(typeIdx) else (if (isIncoming) CallLog.Calls.INCOMING_TYPE else CallLog.Calls.OUTGOING_TYPE)
                    list.add(CallRecord(if (!rawName.isNullOrBlank()) rawName else number, number, date, duration, type))
                }
            }
        } catch (_: Exception) {}
        return list
    }

    fun getConnectedCalls(calls: List<CallRecord>, isIncoming: Boolean): List<CallRecord> =
        if (isIncoming) calls.filter { it.type == CallLog.Calls.INCOMING_TYPE }
        else calls.filter { it.type == CallLog.Calls.OUTGOING_TYPE && it.durationSeconds > 0L }

    fun getMissedCalls(calls: List<CallRecord>, isIncoming: Boolean): List<CallRecord> =
        if (isIncoming) calls.filter { it.type == CallLog.Calls.MISSED_TYPE || it.type == CallLog.Calls.REJECTED_TYPE }
        else calls.filter { it.type == CallLog.Calls.OUTGOING_TYPE && it.durationSeconds == 0L }

    fun isToday(timestamp: Long): Boolean {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date()) == sdf.format(Date(timestamp))
    }

    fun isCallInCurrentCycle(timestamp: Long, dataManager: DataManager): Boolean {
        val cutoff = dataManager.getCallResetCutoffTime()
        if (cutoff > 0L) {
            return timestamp >= cutoff
        }
        return isToday(timestamp)
    }

    fun getTodayTotalDuration(calls: List<CallRecord>): Long = calls.filter { isToday(it.timestamp) }.sumOf { it.durationSeconds }
    fun getMonthTotalDuration(calls: List<CallRecord>, ym: String): Long {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        return calls.filter { sdf.format(Date(it.timestamp)) == ym }.sumOf { it.durationSeconds }
    }
    fun getYearTotalDuration(calls: List<CallRecord>, y: String): Long {
        val sdf = SimpleDateFormat("yyyy", Locale.getDefault())
        return calls.filter { sdf.format(Date(it.timestamp)) == y }.sumOf { it.durationSeconds }
    }
    fun getCallerStats(calls: List<CallRecord>): List<CallerStat> =
        calls.groupBy { it.name }.map { (n, r) -> CallerStat(n, r.firstOrNull()?.number ?: "", r.sumOf { it.durationSeconds }, r.size) }.sortedByDescending { it.totalSeconds }

    fun getCallChartData(context: Context, isIncoming: Boolean, filter: String): List<Pair<String, Float>> {
        val allCalls = readCalls(context, isIncoming)
        val calls = getConnectedCalls(allCalls, isIncoming)
        val cal = Calendar.getInstance()
        val list = mutableListOf<Pair<String, Float>>()

        when (filter) {
            "Day" -> {
                val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
                val hourlyDurSec = LongArray(24) { 0L }
                for (call in calls) {
                    val callDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(call.timestamp))
                    if (callDateStr == todayDate) {
                        val callCal = Calendar.getInstance().apply { timeInMillis = call.timestamp }
                        val h = callCal.get(Calendar.HOUR_OF_DAY)
                        if (h in 0..23) {
                            hourlyDurSec[h] += call.durationSeconds
                        }
                    }
                }
                for (h in 0..23) {
                    val label = when (h) {
                        0 -> "12 AM"
                        in 1..11 -> "$h AM"
                        12 -> "12 PM"
                        else -> "${h - 12} PM"
                    }
                    list.add(Pair(label, hourlyDurSec[h] / 3600f))
                }
            }
            "Month" -> {
                val currentMonthStr = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(cal.time)
                val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                val dailyDurSec = LongArray(maxDays + 1) { 0L }
                for (call in calls) {
                    val callMonthStr = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(call.timestamp))
                    if (callMonthStr == currentMonthStr) {
                        val callCal = Calendar.getInstance().apply { timeInMillis = call.timestamp }
                        val d = callCal.get(Calendar.DAY_OF_MONTH)
                        if (d in 1..maxDays) {
                            dailyDurSec[d] += call.durationSeconds
                        }
                    }
                }
                for (d in 1..maxDays) {
                    list.add(Pair(d.toString(), dailyDurSec[d] / 3600f))
                }
            }
            "Year" -> {
                val currentYearStr = SimpleDateFormat("yyyy", Locale.getDefault()).format(cal.time)
                val monthLetters = listOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")
                val monthlyDurSec = LongArray(12) { 0L }
                for (call in calls) {
                    val callYearStr = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date(call.timestamp))
                    if (callYearStr == currentYearStr) {
                        val callCal = Calendar.getInstance().apply { timeInMillis = call.timestamp }
                        val m = callCal.get(Calendar.MONTH) // 0..11
                        if (m in 0..11) {
                            monthlyDurSec[m] += call.durationSeconds
                        }
                    }
                }
                for (m in 0..11) {
                    list.add(Pair(monthLetters[m], monthlyDurSec[m] / 3600f))
                }
            }
        }
        return list
    }
}
