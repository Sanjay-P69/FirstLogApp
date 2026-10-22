package com.Sanjay.firstlogapp.data.device

import android.content.Context
import android.provider.CallLog
import com.Sanjay.firstlogapp.data.model.CallRecord

/** A failed or denied provider read propagates to UI state rather than inventing zero calls. */
class CallHistoryReader(context: Context) {
    private val resolver = context.applicationContext.contentResolver

    fun read(start: Long, end: Long): List<CallRecord> {
        val date = CallLog.Calls.DATE
        val duration = CallLog.Calls.DURATION
        val selection = "$date < ? AND ($date >= ? OR ($date + $duration * 1000) > ?)"
        val args = arrayOf(end.toString(), start.toString(), start.toString())
        val columns = arrayOf(
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.NUMBER,
            date,
            duration,
            CallLog.Calls.TYPE
        )
        val cursor =
            resolver.query(CallLog.Calls.CONTENT_URI, columns, selection, args, "$date DESC")
                ?: error("Call history provider unavailable")
        return cursor.use {
            val nameIndex = it.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
            val numberIndex = it.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
            val dateIndex = it.getColumnIndexOrThrow(date)
            val durationIndex = it.getColumnIndexOrThrow(duration)
            val typeIndex = it.getColumnIndexOrThrow(CallLog.Calls.TYPE)
            buildList {
                while (it.moveToNext()) {
                    val number = it.getString(numberIndex).orEmpty()
                    add(
                        CallRecord(
                            it.getString(nameIndex).orEmpty().ifBlank { number },
                            number,
                            it.getLong(dateIndex),
                            it.getLong(durationIndex),
                            it.getInt(typeIndex)
                        )
                    )
                }
            }
        }
    }
}
