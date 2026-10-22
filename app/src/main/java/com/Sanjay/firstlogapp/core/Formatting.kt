package com.Sanjay.firstlogapp.core

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatDurationMillis(millis: Long): String {
    val s = millis / 1000
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) String.format(Locale.getDefault(), "%dh %02dm %02ds", h, m, sec)
    else String.format(Locale.getDefault(), "%02dm %02ds", m, sec)
}

fun formatTime(millis: Long): String =
    if (millis == 0L) "--:--" else SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date(millis))

fun formatTimeShort(millis: Long): String =
    if (millis <= 0L) "Recent" else SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(millis))
