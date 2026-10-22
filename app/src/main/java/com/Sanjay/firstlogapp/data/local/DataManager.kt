package com.Sanjay.firstlogapp.data.local

import com.Sanjay.firstlogapp.BluetoothCategoryHelper
import com.Sanjay.firstlogapp.data.device.CallLogHelper
import com.Sanjay.firstlogapp.data.device.DeviceStateHelper
import com.Sanjay.firstlogapp.data.model.AppUsageRecord
import com.Sanjay.firstlogapp.data.model.BatteryHistoryRecord
import com.Sanjay.firstlogapp.data.model.ChargingSessionRecord
import com.Sanjay.firstlogapp.data.model.UnlockSessionRecord

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.BatteryManager
import android.os.Environment
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DataManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("FirstLogAppStorage", Context.MODE_PRIVATE)
    private val keyCurrentCycleDate = "current_cycle_date"
    private val keyHistoryRecords = "history_records_json"
    private val keyGoogleEmail = "google_account_email"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    @Volatile
    private var isPerformingRollover = false

    fun getPrefs(): SharedPreferences = prefs

    fun getGeminiApiKey(): String = prefs.getString("gemini_api_key", "") ?: ""
    fun setGeminiApiKey(key: String) {
        prefs.edit().putString("gemini_api_key", key.trim()).apply()
    }

    fun getCallResetCutoffTime(): Long = prefs.getLong("call_reset_cutoff_time", 0L)
    fun setCallResetCutoffTime(time: Long) {
        prefs.edit().putLong("call_reset_cutoff_time", time).apply()
    }

    fun saveGoogleAccount(email: String) {
        prefs.edit().putString(keyGoogleEmail, email).apply()
    }

    fun getGoogleAccount(): String? = prefs.getString(keyGoogleEmail, null)

    // Configurable daily reset time (default 12:01 AM -> hour 0, minute 1)
    fun getResetTime(): Pair<Int, Int> {
        val h = prefs.getInt("reset_hour", 0)
        val m = prefs.getInt("reset_minute", 1)
        return Pair(h, m)
    }

    fun setResetTime(hour: Int, minute: Int) {
        prefs.edit()
            .putInt("reset_hour", hour)
            .putInt("reset_minute", minute)
            .apply()
    }

    fun getResetTimeString(): String {
        val (h, m) = getResetTime()
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, m)
        }
        return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(cal.time)
    }

    // App theme mode: "system", "light", "dark"
    fun getThemeMode(): String = prefs.getString("app_theme_mode", "system") ?: "system"

    fun setThemeMode(mode: String) {
        prefs.edit().putString("app_theme_mode", mode).apply()
    }

    // App thumbnail style: "vibrant", "system_icon", "light_badge", "pastel"
    fun getThumbnailStyle(): String = prefs.getString("app_thumbnail_style", "system_icon") ?: "vibrant"

    fun setThumbnailStyle(style: String) {
        prefs.edit().putString("app_thumbnail_style", style).apply()
    }

    fun getCycleStartTimestamp(timeMs: Long = System.currentTimeMillis()): Long {
        val (h, m) = getResetTime()
        val target = Calendar.getInstance().apply {
            timeInMillis = timeMs
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, m)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (timeMs < target.timeInMillis) {
            target.add(Calendar.DAY_OF_YEAR, -1)
        }
        return target.timeInMillis
    }

    fun getCycleDateForTimestamp(timeMs: Long): String {
        val (h, m) = getResetTime()
        val cal = Calendar.getInstance().apply {
            timeInMillis = timeMs
        }
        val targetToday = Calendar.getInstance().apply {
            timeInMillis = timeMs
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, m)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (timeMs < targetToday.timeInMillis) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return dateFormat.format(cal.time)
    }

    fun getCurrentCycleDate(): String = getCycleDateForTimestamp(System.currentTimeMillis())

    fun checkAndPerformCycleRollover(now: Long = System.currentTimeMillis()) {
        if (isPerformingRollover) return
        val activeCycle = getCurrentCycleDate()
        val savedCycle = prefs.getString(keyCurrentCycleDate, null)
        if (savedCycle == null) {
            prefs.edit().putString(keyCurrentCycleDate, activeCycle).apply()
            return
        }
        if (savedCycle != activeCycle) {
            synchronized(this) {
                if (isPerformingRollover) return
                val currentSaved = prefs.getString(keyCurrentCycleDate, null)
                if (currentSaved == activeCycle) return
                isPerformingRollover = true
                try {
                    val boundary = getCycleStartTimestamp(now)
                    val metrics = listOf("screen", "wifi", "bluetooth", "data", "gps", "both_wifi_data")

                    // Credit portion before boundary to the old day's accumulators
                    for (m in metrics) {
                        if (isMetricActive(m)) {
                            val lastOn = getMetricLastOnTime(m)
                            if (lastOn > 0L && boundary > lastOn) {
                                val durOld = boundary - lastOn
                                val key = "today_$m"
                                prefs.edit().putLong(key, prefs.getLong(key, 0L) + durOld).apply()
                                recordSessionAcrossHours(m, lastOn, boundary)
                            }
                        }
                    }

                    // Bluetooth categories rollover
                    val btCats = listOf("buds", "watch", "auto", "other")
                    for (cat in btCats) {
                        if (isBtCategoryActive(cat)) {
                            val start = prefs.getLong("bt_start_time_$cat", 0L)
                            if (start > 0L && boundary > start) {
                                val durOld = boundary - start
                                prefs.edit().putLong("today_bt_${cat}_ms", prefs.getLong("today_bt_${cat}_ms", 0L) + durOld).apply()
                            }
                        }
                    }

                    if (isCharging()) {
                        val lastCharge = getLastChargeStartTime()
                        if (lastCharge > 0L && boundary > lastCharge) {
                            val durOld = boundary - lastCharge
                            prefs.edit().putLong("today_charge_time", prefs.getLong("today_charge_time", 0L) + durOld).apply()
                        }
                    }

                    // Archive yesterday's record
                    val dayJson = JSONObject().apply {
                        put("date", savedCycle)
                        put("screen", prefs.getLong("today_screen", 0L))
                        put("wifi", prefs.getLong("today_wifi", 0L))
                        put("bluetooth", prefs.getLong("today_bluetooth", 0L))
                        put("data", prefs.getLong("today_data", 0L))
                        put("both_wifi_data", prefs.getLong("today_both_wifi_data", 0L))
                        put("gps", prefs.getLong("today_gps", 0L))
                        put("unlock_count", prefs.getInt("today_unlock_count", 0))
                        put("unlock_fingerprint", prefs.getInt("today_unlock_fingerprint", 0))
                        put("unlock_face", prefs.getInt("today_unlock_face", 0))
                        put("unlock_pin", prefs.getInt("today_unlock_pin", 0))
                        put("actual_usage_count", prefs.getInt("today_actual_usage_sessions_count", 0))
                        put("quick_glance_count", prefs.getInt("today_quick_glances_count", 0))
                        put("bt_buds", prefs.getLong("today_bt_buds_ms", 0L))
                        put("bt_watch", prefs.getLong("today_bt_watch_ms", 0L))
                        put("bt_auto", prefs.getLong("today_bt_auto_ms", 0L))
                        put("battery_drop", prefs.getInt("today_battery_used_pct", 0))
                        put("charge_time", prefs.getLong("today_charge_time", 0L))
                        put("charging_sessions_count", prefs.getInt("today_charging_sessions_count", 0))
                        put("screen_on_drain_rate", getScreenOnDrainRate())
                    }
                    val array = try { JSONArray(prefs.getString(keyHistoryRecords, "[]") ?: "[]") } catch (_: Exception) { JSONArray() }
                    array.put(dayJson)

                    val currLvl = prefs.getInt("current_battery_level", -1)
                    val editor = prefs.edit()
                        .putString(keyHistoryRecords, array.toString())
                        .putString(keyCurrentCycleDate, activeCycle)
                        .putLong("call_reset_cutoff_time", boundary)
                        .putLong("today_screen", 0L)
                        .putLong("today_wifi", 0L)
                        .putLong("today_bluetooth", 0L)
                        .putLong("today_data", 0L)
                        .putLong("today_both_wifi_data", 0L)
                        .putInt("today_count_both_wifi_data", 0)
                        .putLong("today_gps", 0L)
                        .putInt("today_unlock_count", 0)
                        .putInt("today_unlock_fingerprint", 0)
                        .putInt("today_unlock_face", 0)
                        .putInt("today_unlock_pin", 0)
                        .putInt("today_actual_usage_sessions_count", 0)
                        .putLong("today_actual_usage_sessions_duration", 0L)
                        .putInt("today_quick_glances_count", 0)
                        .putLong("today_quick_glances_duration", 0L)
                        .putInt("today_lockscreen_peeks_count", 0)
                        .putString("today_unlock_sessions_json", "[]")
                        .putLong("today_bt_buds_ms", 0L)
                        .putLong("today_bt_watch_ms", 0L)
                        .putLong("today_bt_auto_ms", 0L)
                        .putLong("today_bt_other_ms", 0L)
                        .putString("custom_app_usage_json", "{}")
                        .putInt("today_battery_used_pct", 0)
                        .putInt("today_screen_on_battery_drop", 0)
                        .putInt("battery_start_level", currLvl)
                        .putInt("last_battery_pct", currLvl)
                        .putLong("today_charge_time", 0L)
                        .putInt("today_charging_sessions_count", 0)
                        .putString("today_charging_sessions_json", "[]")

                    // For active metrics, today's session starts from boundary!
                    for (m in metrics) {
                        val isActive = isMetricActive(m)
                        editor.putInt("today_count_$m", if (isActive) 1 else 0)
                        editor.putLong("last_session_dur_$m", 0L)
                        if (isActive) {
                            editor.putLong("last_on_time_$m", boundary)
                        }
                    }

                    for (cat in btCats) {
                        if (isBtCategoryActive(cat)) {
                            editor.putLong("bt_start_time_$cat", boundary)
                        }
                    }

                    if (isCharging()) {
                        editor.putLong("last_charge_start_time", boundary)
                    }

                    editor.apply()
                } catch (_: Exception) {
                    prefs.edit().putString(keyCurrentCycleDate, activeCycle).apply()
                } finally {
                    isPerformingRollover = false
                }
            }
        }
    }

    fun checkAndPerformMidnightRollover() {
        checkAndPerformCycleRollover(System.currentTimeMillis())
    }

    // Force manual reset of today's activity
    fun forceResetCycleNow(now: Long = System.currentTimeMillis()) {
        val savedCycle = prefs.getString(keyCurrentCycleDate, getCurrentCycleDate()) ?: getCurrentCycleDate()
        val metrics = listOf("screen", "wifi", "bluetooth", "data", "gps", "both_wifi_data")

        for (m in metrics) {
            if (isMetricActive(m)) {
                val lastOn = getMetricLastOnTime(m)
                if (lastOn > 0L && now > lastOn) {
                    val dur = now - lastOn
                    val key = "today_$m"
                    prefs.edit().putLong(key, prefs.getLong(key, 0L) + dur).apply()
                    recordSessionAcrossHours(m, lastOn, now)
                }
            }
        }

        val btCats = listOf("buds", "watch", "auto", "other")
        for (cat in btCats) {
            if (isBtCategoryActive(cat)) {
                val start = prefs.getLong("bt_start_time_$cat", 0L)
                if (start > 0L && now > start) {
                    val dur = now - start
                    prefs.edit().putLong("today_bt_${cat}_ms", prefs.getLong("today_bt_${cat}_ms", 0L) + dur).apply()
                }
            }
        }

        if (isCharging()) {
            val lastCharge = getLastChargeStartTime()
            if (lastCharge > 0L && now > lastCharge) {
                val dur = now - lastCharge
                prefs.edit().putLong("today_charge_time", prefs.getLong("today_charge_time", 0L) + dur).apply()
            }
        }

        val dayJson = JSONObject().apply {
            put("date", "$savedCycle (Reset)")
            put("screen", prefs.getLong("today_screen", 0L))
            put("wifi", prefs.getLong("today_wifi", 0L))
            put("bluetooth", prefs.getLong("today_bluetooth", 0L))
            put("data", prefs.getLong("today_data", 0L))
            put("both_wifi_data", prefs.getLong("today_both_wifi_data", 0L))
            put("gps", prefs.getLong("today_gps", 0L))
            put("unlock_count", prefs.getInt("today_unlock_count", 0))
            put("unlock_fingerprint", prefs.getInt("today_unlock_fingerprint", 0))
            put("unlock_face", prefs.getInt("today_unlock_face", 0))
            put("unlock_pin", prefs.getInt("today_unlock_pin", 0))
            put("actual_usage_count", prefs.getInt("today_actual_usage_sessions_count", 0))
            put("quick_glance_count", prefs.getInt("today_quick_glances_count", 0))
            put("bt_buds", prefs.getLong("today_bt_buds_ms", 0L))
            put("bt_watch", prefs.getLong("today_bt_watch_ms", 0L))
            put("bt_auto", prefs.getLong("today_bt_auto_ms", 0L))
            put("battery_drop", prefs.getInt("today_battery_used_pct", 0))
            put("charge_time", prefs.getLong("today_charge_time", 0L))
            put("charging_sessions_count", prefs.getInt("today_charging_sessions_count", 0))
            put("screen_on_drain_rate", getScreenOnDrainRate())
        }
        val array = try { JSONArray(prefs.getString(keyHistoryRecords, "[]") ?: "[]") } catch (_: Exception) { JSONArray() }
        array.put(dayJson)

        val currLvl = prefs.getInt("current_battery_level", -1)
        val editor = prefs.edit()
            .putString(keyHistoryRecords, array.toString())
            .putLong("call_reset_cutoff_time", now)
            .putLong("today_screen", 0L)
            .putLong("today_wifi", 0L)
            .putLong("today_bluetooth", 0L)
            .putLong("today_data", 0L)
            .putLong("today_both_wifi_data", 0L)
            .putInt("today_count_both_wifi_data", 0)
            .putLong("today_gps", 0L)
            .putInt("today_unlock_count", 0)
            .putInt("today_unlock_fingerprint", 0)
            .putInt("today_unlock_face", 0)
            .putInt("today_unlock_pin", 0)
            .putInt("today_actual_usage_sessions_count", 0)
            .putLong("today_actual_usage_sessions_duration", 0L)
            .putInt("today_quick_glances_count", 0)
            .putLong("today_quick_glances_duration", 0L)
            .putInt("today_lockscreen_peeks_count", 0)
            .putString("today_unlock_sessions_json", "[]")
            .putLong("today_bt_buds_ms", 0L)
            .putLong("today_bt_watch_ms", 0L)
            .putLong("today_bt_auto_ms", 0L)
            .putLong("today_bt_other_ms", 0L)
            .putString("custom_app_usage_json", "{}")
            .putInt("today_battery_used_pct", 0)
            .putInt("today_screen_on_battery_drop", 0)
            .putInt("battery_start_level", currLvl)
            .putInt("last_battery_pct", currLvl)
            .putLong("today_charge_time", 0L)
            .putInt("today_charging_sessions_count", 0)
            .putString("today_charging_sessions_json", "[]")

        for (m in metrics) {
            val isActive = isMetricActive(m)
            editor.putInt("today_count_$m", if (isActive) 1 else 0)
            editor.putLong("last_session_dur_$m", 0L)
            if (isActive) {
                editor.putLong("last_on_time_$m", now)
            }
        }

        for (cat in btCats) {
            if (isBtCategoryActive(cat)) {
                editor.putLong("bt_start_time_$cat", now)
            }
        }

        if (isCharging()) {
            editor.putLong("last_charge_start_time", now)
        }

        editor.apply()
    }

    fun addMetricTime(metricId: String, durationMillis: Long) {
        checkAndPerformCycleRollover()
        val key = "today_$metricId"
        prefs.edit().putLong(key, prefs.getLong(key, 0L) + durationMillis).apply()
    }

    private fun getHourlyKey(metricId: String, date: String, hour: Int) = "hourly_${metricId}_${date}_$hour"

    fun addHourlyMetricTime(metricId: String, durationMs: Long, now: Long = System.currentTimeMillis()) {
        if (durationMs <= 0L) return
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val date = dateFormat.format(cal.time)
        val key = getHourlyKey(metricId, date, hour)
        prefs.edit().putLong(key, prefs.getLong(key, 0L) + durationMs).apply()
    }

    fun getHourlyMetricTime(metricId: String, date: String, hour: Int): Long {
        val key = getHourlyKey(metricId, date, hour)
        return prefs.getLong(key, 0L)
    }

    fun recordSessionAcrossHours(metricId: String, startTime: Long, endTime: Long) {
        if (endTime <= startTime) return
        val cal = Calendar.getInstance()
        var cur = startTime
        val editor = prefs.edit()
        while (cur < endTime) {
            cal.timeInMillis = cur
            val curHour = cal.get(Calendar.HOUR_OF_DAY)
            val curDate = dateFormat.format(cal.time)

            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val nextHourStart = cal.timeInMillis + 1L
            val segmentEnd = minOf(endTime, nextHourStart)
            val segmentDur = segmentEnd - cur
            if (segmentDur > 0L) {
                val key = getHourlyKey(metricId, curDate, curHour)
                editor.putLong(key, prefs.getLong(key, 0L) + segmentDur)
            }
            cur = segmentEnd
        }
        editor.apply()
    }

    fun getTodayMetricTime(metricId: String): Long {
        checkAndPerformCycleRollover()
        return prefs.getLong("today_$metricId", 0L)
    }

    fun setMetricLastOnTime(metricId: String, time: Long) {
        prefs.edit().putLong("last_on_time_$metricId", time).apply()
    }

    fun getMetricLastOnTime(metricId: String): Long = prefs.getLong("last_on_time_$metricId", 0L)

    fun setMetricActive(metricId: String, active: Boolean) {
        prefs.edit().putBoolean("metric_active_$metricId", active).apply()
    }

    fun isMetricActive(metricId: String): Boolean = prefs.getBoolean("metric_active_$metricId", false)

    fun setMetricCount(metricId: String, count: Int) {
        val key = "today_count_$metricId"
        prefs.edit().putInt(key, count).apply()
    }

    fun setMetricLastOffTime(metricId: String, time: Long) {
        prefs.edit().putLong("last_off_time_$metricId", time).apply()
    }

    fun getMetricLastOffTime(metricId: String): Long = prefs.getLong("last_off_time_$metricId", 0L)

    fun setMetricLastSessionDuration(metricId: String, duration: Long) {
        prefs.edit().putLong("last_session_dur_$metricId", duration).apply()
    }

    fun getMetricLastSessionDuration(metricId: String): Long = prefs.getLong("last_session_dur_$metricId", 0L)

    fun getTodayMetricCount(metricId: String): Int {
        checkAndPerformCycleRollover()
        val raw = prefs.getInt("today_count_$metricId", 0)
        val hasActivity = isMetricActive(metricId) || prefs.getLong("today_$metricId", 0L) > 0L
        return if (raw <= 0 && hasActivity) 1 else raw
    }

    fun getTodayTotalMetricTime(metricId: String): Long {
        checkAndPerformCycleRollover()
        val accum = prefs.getLong("today_$metricId", 0L)
        val isActive = isMetricActive(metricId)
        val lastOn = getMetricLastOnTime(metricId)
        val live = if (isActive && lastOn > 0L) (System.currentTimeMillis() - lastOn).coerceAtLeast(0L) else 0L
        return accum + live
    }

    fun updateMetricState(metricId: String, isCurrentlyOn: Boolean, now: Long = System.currentTimeMillis()) {
        checkAndPerformCycleRollover(now)
        val wasActive = isMetricActive(metricId)
        val currentCount = prefs.getInt("today_count_$metricId", 0)

        if (isCurrentlyOn) {
            if (!wasActive) {
                setMetricActive(metricId, true)
                setMetricLastOnTime(metricId, now)
                val newCount = if (currentCount <= 0) 1 else currentCount + 1
                setMetricCount(metricId, newCount)
            } else {
                val lastOn = getMetricLastOnTime(metricId)
                if (lastOn <= 0L) {
                    setMetricLastOnTime(metricId, now)
                }
                if (currentCount <= 0) {
                    setMetricCount(metricId, 1)
                }
            }
        } else {
            if (wasActive) {
                val lastOn = getMetricLastOnTime(metricId)
                if (lastOn > 0L && now > lastOn) {
                    val sessionDur = now - lastOn
                    addMetricTime(metricId, sessionDur)
                    recordSessionAcrossHours(metricId, lastOn, now)
                    setMetricLastSessionDuration(metricId, sessionDur)
                }
                setMetricActive(metricId, false)
                setMetricLastOffTime(metricId, now)
            }
        }
    }

    // Point 1: Dual Wi-Fi & Data helper methods
    fun getTodayBothWifiDataTime(): Long = getTodayTotalMetricTime("both_wifi_data")
    fun getTodayBothWifiDataCount(): Int = getTodayMetricCount("both_wifi_data")

    // Point 2: Screen unlock tracking with Face, Fingerprint, and PIN breakdown
    fun recordUnlock(type: String, now: Long = System.currentTimeMillis()) {
        checkAndPerformCycleRollover(now)
        val editor = prefs.edit()
        editor.putInt("today_unlock_count", prefs.getInt("today_unlock_count", 0) + 1)
        when (type) {
            "fingerprint" -> editor.putInt("today_unlock_fingerprint", prefs.getInt("today_unlock_fingerprint", 0) + 1)
            "face" -> editor.putInt("today_unlock_face", prefs.getInt("today_unlock_face", 0) + 1)
            else -> editor.putInt("today_unlock_pin", prefs.getInt("today_unlock_pin", 0) + 1)
        }
        editor.putString("last_unlock_type", type)
        editor.putLong("last_unlock_time", now)
        editor.apply()
    }

    fun getTodayUnlockCount(): Int {
        checkAndPerformCycleRollover()
        return prefs.getInt("today_unlock_count", 0)
    }

    fun getTodayUnlockFingerprint(): Int {
        checkAndPerformCycleRollover()
        return prefs.getInt("today_unlock_fingerprint", 0)
    }

    fun getTodayUnlockFace(): Int {
        checkAndPerformCycleRollover()
        return prefs.getInt("today_unlock_face", 0)
    }

    fun getTodayUnlockPin(): Int {
        checkAndPerformCycleRollover()
        return prefs.getInt("today_unlock_pin", 0)
    }

    fun getLastUnlockType(): String = prefs.getString("last_unlock_type", "pin") ?: "pin"

    fun setLastUnlockTime(time: Long) {
        prefs.edit().putLong("last_unlock_time", time).apply()
    }

    fun getLastUnlockTime(): Long = prefs.getLong("last_unlock_time", 0L)

    fun recordUnlockSessionEnd(isQuickGlance: Boolean, durationMs: Long, now: Long = System.currentTimeMillis()) {
        checkAndPerformCycleRollover(now)
        val lastType = getLastUnlockType()
        val editor = prefs.edit()
        if (isQuickGlance) {
            editor.putInt("today_quick_glances_count", prefs.getInt("today_quick_glances_count", 0) + 1)
            editor.putLong("today_quick_glances_duration", prefs.getLong("today_quick_glances_duration", 0L) + durationMs)
        } else {
            editor.putInt("today_actual_usage_sessions_count", prefs.getInt("today_actual_usage_sessions_count", 0) + 1)
            editor.putLong("today_actual_usage_sessions_duration", prefs.getLong("today_actual_usage_sessions_duration", 0L) + durationMs)
        }

        try {
            val raw = prefs.getString("today_unlock_sessions_json", "[]") ?: "[]"
            val arr = JSONArray(raw)
            val obj = JSONObject().apply {
                put("timestamp", now)
                put("durationMs", durationMs)
                put("isQuickGlance", isQuickGlance)
                put("unlockType", lastType)
            }
            arr.put(obj)
            val trimmed = if (arr.length() > 50) {
                val newArr = JSONArray()
                for (i in (arr.length() - 50) until arr.length()) {
                    newArr.put(arr.getJSONObject(i))
                }
                newArr
            } else arr
            editor.putString("today_unlock_sessions_json", trimmed.toString())
        } catch (_: Exception) {}
        editor.apply()
    }

    fun recordLockScreenGlance(durationMs: Long, now: Long = System.currentTimeMillis()) {
        checkAndPerformCycleRollover(now)
        prefs.edit().putInt("today_lockscreen_peeks_count", prefs.getInt("today_lockscreen_peeks_count", 0) + 1).apply()
    }

    fun getTodayActualUsageCount(): Int {
        checkAndPerformCycleRollover()
        return prefs.getInt("today_actual_usage_sessions_count", 0)
    }

    fun getTodayActualUsageDuration(): Long {
        checkAndPerformCycleRollover()
        return prefs.getLong("today_actual_usage_sessions_duration", 0L)
    }

    fun getTodayQuickGlanceCount(): Int {
        checkAndPerformCycleRollover()
        return prefs.getInt("today_quick_glances_count", 0)
    }

    fun getTodayQuickGlanceDuration(): Long {
        checkAndPerformCycleRollover()
        return prefs.getLong("today_quick_glances_duration", 0L)
    }

    fun getTodayLockScreenPeekCount(): Int {
        checkAndPerformCycleRollover()
        return prefs.getInt("today_lockscreen_peeks_count", 0)
    }

    fun getTodayUnlockSessions(): List<UnlockSessionRecord> {
        checkAndPerformCycleRollover()
        val raw = prefs.getString("today_unlock_sessions_json", "[]") ?: "[]"
        val list = mutableListOf<UnlockSessionRecord>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    UnlockSessionRecord(
                        timestamp = obj.optLong("timestamp", 0L),
                        durationMs = obj.optLong("durationMs", 0L),
                        isQuickGlance = obj.optBoolean("isQuickGlance", false),
                        unlockType = obj.optString("unlockType", "pin")
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    // Point 3: Bluetooth Connected Devices Tracking (Buds, Smartwatch, Android Auto)
    fun onBluetoothDeviceConnected(category: String, deviceName: String, now: Long = System.currentTimeMillis()) {
        checkAndPerformCycleRollover(now)
        val isAlreadyActive = prefs.getBoolean("bt_active_$category", false)
        if (!isAlreadyActive) {
            prefs.edit()
                .putBoolean("bt_active_$category", true)
                .putLong("bt_start_time_$category", now)
                .putString("last_connected_bt_category", category)
                .putString("last_connected_bt_name", deviceName)
                .apply()
        }
    }

    fun onBluetoothDeviceDisconnected(category: String, now: Long = System.currentTimeMillis()) {
        checkAndPerformCycleRollover(now)
        val wasActive = prefs.getBoolean("bt_active_$category", false)
        if (wasActive) {
            val start = prefs.getLong("bt_start_time_$category", 0L)
            if (start > 0L && now > start) {
                val dur = now - start
                val key = "today_bt_${category}_ms"
                prefs.edit().putLong(key, prefs.getLong(key, 0L) + dur).apply()
            }
            prefs.edit()
                .putBoolean("bt_active_$category", false)
                .putLong("bt_start_time_$category", 0L)
                .apply()
        }
    }

    fun getTodayBtCategoryDuration(category: String, now: Long = System.currentTimeMillis()): Long {
        checkAndPerformCycleRollover(now)
        val accum = prefs.getLong("today_bt_${category}_ms", 0L)
        val isActive = prefs.getBoolean("bt_active_$category", false)
        val start = prefs.getLong("bt_start_time_$category", 0L)
        if (isActive && category == BluetoothCategoryHelper.CAT_BUDS) {
            if (!DeviceStateHelper.isBluetoothEnabled(context) || !BluetoothCategoryHelper.isBudsConnected(context)) {
                onBluetoothDeviceDisconnected(category, now)
                return prefs.getLong("today_bt_${category}_ms", 0L)
            }
        }
        val live = if (isActive && start > 0L && now > start) (now - start) else 0L
        return accum + live
    }

    fun isBtCategoryActive(category: String): Boolean = prefs.getBoolean("bt_active_$category", false)
    fun getLastConnectedBtCategory(): String = prefs.getString("last_connected_bt_category", "") ?: ""
    fun getLastConnectedBtName(): String = prefs.getString("last_connected_bt_name", "") ?: ""

    // Point 4: App's Own Verified Foreground App Tracker
    fun recordVerifiedAppOpen(pkg: String, appName: String, now: Long = System.currentTimeMillis()) {
        checkAndPerformCycleRollover(now)
        val raw = prefs.getString("custom_app_usage_json", "{}") ?: "{}"
        val root = try { JSONObject(raw) } catch (_: Exception) { JSONObject() }
        val appObj = root.optJSONObject(pkg) ?: JSONObject().apply {
            put("pkg", pkg)
            put("name", appName)
            put("openCount", 0)
            put("totalTimeMs", 0L)
            put("lastSessionTime", 0L)
            put("lastSessionDurationMs", 0L)
        }
        val currentCount = appObj.optInt("openCount", 0)
        appObj.put("openCount", currentCount + 1)
        appObj.put("lastSessionTime", now)
        appObj.put("name", appName)
        root.put(pkg, appObj)
        prefs.edit().putString("custom_app_usage_json", root.toString()).apply()
    }

    fun addAppUsageTick(pkg: String, appName: String, deltaMs: Long, now: Long = System.currentTimeMillis()) {
        val raw = prefs.getString("custom_app_usage_json", "{}") ?: "{}"
        val root = try { JSONObject(raw) } catch (_: Exception) { JSONObject() }
        val appObj = root.optJSONObject(pkg) ?: JSONObject().apply {
            put("pkg", pkg)
            put("name", appName)
            put("openCount", 1)
            put("totalTimeMs", 0L)
            put("lastSessionTime", now)
            put("lastSessionDurationMs", 0L)
        }
        appObj.put("totalTimeMs", appObj.optLong("totalTimeMs", 0L) + deltaMs)
        appObj.put("lastSessionTime", now)
        root.put(pkg, appObj)
        prefs.edit().putString("custom_app_usage_json", root.toString()).apply()
    }

    fun recordAppSessionEnd(pkg: String, sessionDurationMs: Long, now: Long = System.currentTimeMillis()) {
        val raw = prefs.getString("custom_app_usage_json", "{}") ?: "{}"
        val root = try { JSONObject(raw) } catch (_: Exception) { JSONObject() }
        val appObj = root.optJSONObject(pkg) ?: return
        appObj.put("lastSessionDurationMs", sessionDurationMs)
        appObj.put("lastSessionTime", now)
        root.put(pkg, appObj)
        prefs.edit().putString("custom_app_usage_json", root.toString()).apply()
    }

    fun getCustomAppUsageRecords(): List<AppUsageRecord> {
        checkAndPerformCycleRollover()
        val raw = prefs.getString("custom_app_usage_json", "{}") ?: "{}"
        val root = try { JSONObject(raw) } catch (_: Exception) { return emptyList() }
        val list = mutableListOf<AppUsageRecord>()
        val keys = root.keys()
        while (keys.hasNext()) {
            val pkg = keys.next()
            val obj = root.optJSONObject(pkg) ?: continue
            val timeMs = obj.optLong("totalTimeMs", 0L)
            if (timeMs >= 1000L) {
                list.add(
                    AppUsageRecord(
                        packageName = pkg,
                        appName = obj.optString("name", pkg),
                        usageTimeMs = timeMs,
                        lastSessionTime = obj.optLong("lastSessionTime", 0L),
                        lastSessionDurationMs = obj.optLong("lastSessionDurationMs", 0L),
                        launchCount = obj.optInt("openCount", 1).coerceAtLeast(1)
                    )
                )
            }
        }
        return list.sortedByDescending { it.usageTimeMs }
    }

    fun hasCustomAppUsageRecords(): Boolean {
        val raw = prefs.getString("custom_app_usage_json", "{}") ?: "{}"
        return raw.length > 5
    }

    // Battery & Charging tracking (Cumulative daily % used & clubbed charging)
    fun getTodayBatteryUsed(): Int {
        checkAndPerformCycleRollover()
        return prefs.getInt("today_battery_used_pct", 0)
    }

    fun setBatteryStartLevel(level: Int) {
        if (prefs.getInt("battery_start_level", -1) == -1) {
            prefs.edit().putInt("battery_start_level", level).apply()
        }
    }

    fun getBatteryStartLevel(): Int = prefs.getInt("battery_start_level", -1)

    fun setCurrentBatteryLevel(level: Int) {
        prefs.edit().putInt("current_battery_level", level).apply()
    }

    fun getCurrentBatteryLevel(): Int = prefs.getInt("current_battery_level", -1)

    fun addChargingTime(millis: Long) {
        checkAndPerformCycleRollover()
        val key = "today_charge_time"
        prefs.edit().putLong(key, prefs.getLong(key, 0L) + millis).apply()
    }

    fun getTodayChargingTime(): Long {
        checkAndPerformCycleRollover()
        return prefs.getLong("today_charge_time", 0L)
    }

    fun setLastChargeStartTime(time: Long) {
        prefs.edit().putLong("last_charge_start_time", time).apply()
    }

    fun getLastChargeStartTime(): Long = prefs.getLong("last_charge_start_time", 0L)

    fun setIsCharging(charging: Boolean) {
        prefs.edit().putBoolean("is_charging", charging).apply()
    }

    fun isCharging(): Boolean = prefs.getBoolean("is_charging", false)

    fun handleBatteryUpdate(level: Int, scale: Int, status: Int, plugged: Int, now: Long = System.currentTimeMillis()) {
        checkAndPerformCycleRollover(now)
        val currentPct = if (level >= 0 && scale > 0) (level * 100 / scale) else level
        if (currentPct >= 0) {
            val lastPct = prefs.getInt("last_battery_pct", -1)
            if (lastPct == -1) {
                prefs.edit()
                    .putInt("battery_start_level", currentPct)
                    .putInt("last_battery_pct", currentPct)
                    .putInt("current_battery_level", currentPct)
                    .apply()
            } else {
                if (currentPct < lastPct) {
                    val drop = lastPct - currentPct
                    addHourlyBatteryDrop(drop, now)
                    val prevUsed = prefs.getInt("today_battery_used_pct", 0)
                    val isScreenActive = isMetricActive("screen")
                    val prevScreenDrop = prefs.getInt("today_screen_on_battery_drop", 0)
                    val newScreenDrop = if (isScreenActive) prevScreenDrop + drop else prevScreenDrop
                    prefs.edit()
                        .putInt("today_battery_used_pct", prevUsed + drop)
                        .putInt("today_screen_on_battery_drop", newScreenDrop)
                        .putInt("last_battery_pct", currentPct)
                        .putInt("current_battery_level", currentPct)
                        .apply()
                } else {
                    prefs.edit()
                        .putInt("last_battery_pct", currentPct)
                        .putInt("current_battery_level", currentPct)
                        .apply()
                }
            }
            recordBatteryLevel(currentPct, now)
        }

        val isChargingNow = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL ||
                plugged > 0

        if (isChargingNow) {
            handleChargingStarted(now)
        } else if (status != -1) {
            handleChargingStopped(now)
        }
    }

    fun handleChargingStarted(now: Long = System.currentTimeMillis()) {
        if (!isCharging()) {
            setIsCharging(true)
            setLastChargeStartTime(now)
            prefs.edit().putInt("charge_start_level", getCurrentBatteryLevel()).apply()
        }
    }

    fun handleChargingStopped(now: Long = System.currentTimeMillis()) {
        if (isCharging()) {
            setIsCharging(false)
            val startTime = getLastChargeStartTime()
            if (startTime > 0L && now > startTime) {
                val dur = now - startTime
                addChargingTime(dur)
                val startLvl = prefs.getInt("charge_start_level", getCurrentBatteryLevel())
                val endLvl = getCurrentBatteryLevel()
                val session = ChargingSessionRecord(startTime, now, startLvl, endLvl, dur)
                addChargingSessionRecord(session)
            }
            setLastChargeStartTime(0L)
        }
    }

    fun addChargingSessionRecord(record: ChargingSessionRecord) {
        try {
            val raw = prefs.getString("today_charging_sessions_json", "[]") ?: "[]"
            val arr = JSONArray(raw)
            val obj = JSONObject().apply {
                put("startTime", record.startTime)
                put("endTime", record.endTime)
                put("startLevel", record.startLevel)
                put("endLevel", record.endLevel)
                put("durationMs", record.durationMs)
            }
            arr.put(obj)
            prefs.edit()
                .putString("today_charging_sessions_json", arr.toString())
                .putInt("today_charging_sessions_count", prefs.getInt("today_charging_sessions_count", 0) + 1)
                .apply()
        } catch (_: Exception) {}
    }

    fun getTodayChargingSessions(): List<ChargingSessionRecord> {
        checkAndPerformCycleRollover()
        val raw = prefs.getString("today_charging_sessions_json", "[]") ?: "[]"
        val list = mutableListOf<ChargingSessionRecord>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    ChargingSessionRecord(
                        startTime = obj.optLong("startTime", 0L),
                        endTime = obj.optLong("endTime", 0L),
                        startLevel = obj.optInt("startLevel", 0),
                        endLevel = obj.optInt("endLevel", 0),
                        durationMs = obj.optLong("durationMs", 0L)
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun getTodayChargingSessionCount(): Int {
        checkAndPerformCycleRollover()
        return prefs.getInt("today_charging_sessions_count", 0)
    }

    fun onScreenTurnedOn(now: Long = System.currentTimeMillis()) {
        checkAndPerformCycleRollover(now)
        prefs.edit().putLong("screen_on_start_time", now).apply()
    }

    fun onScreenTurnedOff(now: Long = System.currentTimeMillis()) {
        checkAndPerformCycleRollover(now)
        prefs.edit().putLong("screen_on_start_time", 0L).apply()
    }

    fun getTodayScreenOnBatteryDrop(): Int {
        checkAndPerformCycleRollover()
        return prefs.getInt("today_screen_on_battery_drop", 0)
    }

    fun getScreenOnDrainRate(): Double {
        checkAndPerformCycleRollover()
        val screenTimeMs = getTodayTotalMetricTime("screen")
        val drop = prefs.getInt("today_screen_on_battery_drop", 0)
        if (screenTimeMs < 60_000L || drop <= 0) return 0.0
        val hours = screenTimeMs / 3_600_000.0
        return if (hours > 0.0) drop / hours else 0.0
    }

    fun addHourlyBatteryDrop(drop: Int, now: Long = System.currentTimeMillis()) {
        if (drop <= 0) return
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val date = dateFormat.format(cal.time)
        val key = "hourly_battery_${date}_$hour"
        prefs.edit().putInt(key, prefs.getInt(key, 0) + drop).apply()
    }

    fun getHourlyBatteryDrop(date: String, hour: Int): Int {
        val key = "hourly_battery_${date}_$hour"
        return prefs.getInt(key, 0)
    }

    fun recordBatteryLevel(level: Int, now: Long = System.currentTimeMillis()) {
        if (level < 0) return
        val raw = prefs.getString("battery_hourly_log", "[]") ?: "[]"
        val array = try { JSONArray(raw) } catch (_: Exception) { JSONArray() }

        if (array.length() > 0) {
            val lastObj = array.getJSONObject(array.length() - 1)
            val lastLvl = lastObj.optInt("l", -1)
            val lastT = lastObj.optLong("t", 0L)
            if (lastLvl == level && (now - lastT) < 180_000L) {
                return
            }
        }

        val newObj = JSONObject().apply {
            put("t", now)
            put("l", level)
        }
        array.put(newObj)

        val cutoff = now - 7200_000L
        val trimmed = JSONArray()
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            if (item.optLong("t", 0L) >= cutoff) {
                trimmed.put(item)
            }
        }
        prefs.edit().putString("battery_hourly_log", trimmed.toString()).apply()
    }

    fun getBatteryUsageInLastHour(currentLevel: Int, now: Long = System.currentTimeMillis()): Int {
        if (currentLevel < 0) return 0
        val raw = prefs.getString("battery_hourly_log", "[]") ?: "[]"
        val array = try { JSONArray(raw) } catch (_: Exception) { JSONArray() }
        if (array.length() == 0) return 0

        val oneHourAgo = now - 3600_000L
        var closestLevel = -1
        var closestDiff = Long.MAX_VALUE

        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            val t = item.optLong("t", 0L)
            val diff = kotlin.math.abs(t - oneHourAgo)
            if (diff < closestDiff) {
                closestDiff = diff
                closestLevel = item.optInt("l", -1)
            }
        }

        if (closestLevel == -1) return 0
        return closestLevel - currentLevel
    }

    fun getStackedHistoryByMonth(metricId: String): Map<String, List<Pair<String, Long>>> {
        val resultMap = linkedMapOf<String, MutableList<Pair<String, Long>>>()
        val todayCycle = getCurrentCycleDate()
        val currentMonthKey = todayCycle.substring(0, 7)
        val todayTotal = getTodayTotalMetricTime(metricId)
        resultMap[currentMonthKey] = mutableListOf(Pair("$todayCycle (Today)", todayTotal))

        val array = JSONArray(prefs.getString(keyHistoryRecords, "[]") ?: "[]")
        for (i in array.length() - 1 downTo 0) {
            val item = array.getJSONObject(i)
            val dateStr = item.getString("date")
            val monthKey = if (dateStr.length >= 7) dateStr.substring(0, 7) else "Other"
            val millis = item.optLong(metricId, 0L)
            resultMap.getOrPut(monthKey) { mutableListOf() }.add(Pair(dateStr, millis))
        }
        return resultMap
    }

    fun getStackedBatteryHistoryByMonth(): Map<String, List<BatteryHistoryRecord>> {
        val resultMap = linkedMapOf<String, MutableList<BatteryHistoryRecord>>()
        val todayCycle = getCurrentCycleDate()
        val currentMonthKey = if (todayCycle.length >= 7) todayCycle.substring(0, 7) else "Current"

        val todayUsed = getTodayBatteryUsed()
        val isChargingNow = isCharging()
        val lastChargeStart = getLastChargeStartTime()
        val liveCharge = if (isChargingNow && lastChargeStart > 0L) (System.currentTimeMillis() - lastChargeStart).coerceAtLeast(0L) else 0L
        val todayCharge = getTodayChargingTime() + liveCharge

        resultMap[currentMonthKey] = mutableListOf(BatteryHistoryRecord("$todayCycle (Today)", todayUsed, todayCharge))

        val array = JSONArray(prefs.getString(keyHistoryRecords, "[]") ?: "[]")
        for (i in array.length() - 1 downTo 0) {
            val item = array.getJSONObject(i)
            val dateStr = item.getString("date")
            val monthKey = if (dateStr.length >= 7) dateStr.substring(0, 7) else "Other"
            val used = item.optInt("battery_drop", 0)
            val charge = item.optLong("charge_time", 0L)
            resultMap.getOrPut(monthKey) { mutableListOf() }.add(BatteryHistoryRecord(dateStr, used, charge))
        }
        return resultMap
    }

    fun getYearlyMetricTotal(metricId: String, year: String): Long {
        val array = JSONArray(prefs.getString(keyHistoryRecords, "[]") ?: "[]")
        var total = 0L
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            if (item.getString("date").startsWith(year)) total += item.optLong(metricId, 0L)
        }
        if (getCurrentCycleDate().startsWith(year)) total += getTodayTotalMetricTime(metricId)
        return total
    }

    fun getChartData(metricId: String, filter: String): List<Pair<String, Float>> {
        if (metricId == "call_in" || metricId == "call_out") {
            return CallLogHelper.getCallChartData(context, metricId == "call_in", filter)
        }

        val list = mutableListOf<Pair<String, Float>>()
        val todayCycle = getCurrentCycleDate()
        val todayTotalMs = getTodayTotalMetricTime(metricId)
        val calNow = Calendar.getInstance()

        when (filter) {
            "Day" -> {
                val currentHour = calNow.get(Calendar.HOUR_OF_DAY)
                // Distribute live duration if currently active
                val liveAcrossHours = mutableMapOf<Int, Long>()
                if (isMetricActive(metricId)) {
                    val lastOn = getMetricLastOnTime(metricId)
                    val now = System.currentTimeMillis()
                    if (lastOn > 0L && now > lastOn) {
                        var cur = maxOf(lastOn, getCycleStartTimestamp(now))
                        val cal = Calendar.getInstance()
                        while (cur < now) {
                            cal.timeInMillis = cur
                            val curH = cal.get(Calendar.HOUR_OF_DAY)
                            cal.set(Calendar.MINUTE, 59)
                            cal.set(Calendar.SECOND, 59)
                            cal.set(Calendar.MILLISECOND, 999)
                            val nextHourStart = cal.timeInMillis + 1L
                            val segmentEnd = minOf(now, nextHourStart)
                            val segmentDur = segmentEnd - cur
                            if (segmentDur > 0L) {
                                liveAcrossHours[curH] = (liveAcrossHours[curH] ?: 0L) + segmentDur
                            }
                            cur = segmentEnd
                        }
                    }
                }

                val hourlyMsList = MutableList(24) { h ->
                    getHourlyMetricTime(metricId, todayCycle, h) + (liveAcrossHours[h] ?: 0L)
                }
                val sumRecorded = hourlyMsList.sum()
                val missing = todayTotalMs - sumRecorded
                if (missing > 1000L) {
                    hourlyMsList[currentHour] = hourlyMsList[currentHour] + missing
                }

                for (h in 0..23) {
                    val label = when (h) {
                        0 -> "12 AM"
                        in 1..11 -> "$h AM"
                        12 -> "12 PM"
                        else -> "${h - 12} PM"
                    }
                    val hours = hourlyMsList[h] / (1000f * 3600f)
                    list.add(Pair(label, hours))
                }
            }
            "Month" -> {
                val maxDaysInMonth = calNow.getActualMaximum(Calendar.DAY_OF_MONTH)
                val currentDay = calNow.get(Calendar.DAY_OF_MONTH)
                val currentMonthPrefix = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calNow.time)
                val array = JSONArray(prefs.getString(keyHistoryRecords, "[]") ?: "[]")

                val historyByDate = mutableMapOf<String, Long>()
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    val d = item.getString("date")
                    if (d.startsWith(currentMonthPrefix)) {
                        historyByDate[d] = item.optLong(metricId, 0L)
                    }
                }

                for (day in 1..maxDaysInMonth) {
                    val dateStr = String.format(Locale.getDefault(), "%s-%02d", currentMonthPrefix, day)
                    val ms = when {
                        day == currentDay -> todayTotalMs
                        day < currentDay -> historyByDate[dateStr] ?: 0L
                        else -> 0L
                    }
                    list.add(Pair(day.toString(), ms / (1000f * 3600f)))
                }
            }
            "Year" -> {
                val currentYear = SimpleDateFormat("yyyy", Locale.getDefault()).format(calNow.time)
                val monthLetters = listOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")
                val currentMonthIndex = calNow.get(Calendar.MONTH) // 0..11
                val array = JSONArray(prefs.getString(keyHistoryRecords, "[]") ?: "[]")

                for (m in 0..11) {
                    val ym = String.format(Locale.getDefault(), "%s-%02d", currentYear, m + 1)
                    var mTotal = 0L
                    for (i in 0 until array.length()) {
                        val item = array.getJSONObject(i)
                        if (item.getString("date").startsWith(ym)) {
                            mTotal += item.optLong(metricId, 0L)
                        }
                    }
                    if (m == currentMonthIndex) {
                        mTotal += todayTotalMs
                    }
                    list.add(Pair(monthLetters[m], mTotal / (1000f * 3600f)))
                }
            }
        }
        return list
    }

    fun buildBackupJsonString(): String {
        return JSONObject().apply {
            put("google_account", getGoogleAccount() ?: "")
            put("current_date", getCurrentCycleDate())
            put("reset_hour", prefs.getInt("reset_hour", 0))
            put("reset_minute", prefs.getInt("reset_minute", 1))
            put("app_theme_mode", getThemeMode())
            put("today_battery_used_pct", getTodayBatteryUsed())
            put("today_screen", prefs.getLong("today_screen", 0L))
            put("today_wifi", prefs.getLong("today_wifi", 0L))
            put("today_bluetooth", prefs.getLong("today_bluetooth", 0L))
            put("today_data", prefs.getLong("today_data", 0L))
            put("today_both_wifi_data", prefs.getLong("today_both_wifi_data", 0L))
            put("today_gps", prefs.getLong("today_gps", 0L))
            put("today_count_screen", prefs.getInt("today_count_screen", 0))
            put("today_count_wifi", prefs.getInt("today_count_wifi", 0))
            put("today_count_data", prefs.getInt("today_count_data", 0))
            put("today_count_both_wifi_data", prefs.getInt("today_count_both_wifi_data", 0))
            put("today_count_bluetooth", prefs.getInt("today_count_bluetooth", 0))
            put("today_count_gps", prefs.getInt("today_count_gps", 0))
            put("today_unlock_count", prefs.getInt("today_unlock_count", 0))
            put("today_unlock_fingerprint", prefs.getInt("today_unlock_fingerprint", 0))
            put("today_unlock_face", prefs.getInt("today_unlock_face", 0))
            put("today_unlock_pin", prefs.getInt("today_unlock_pin", 0))
            put("today_bt_buds_ms", prefs.getLong("today_bt_buds_ms", 0L))
            put("today_bt_watch_ms", prefs.getLong("today_bt_watch_ms", 0L))
            put("today_bt_auto_ms", prefs.getLong("today_bt_auto_ms", 0L))
            put("last_unlock_time", prefs.getLong("last_unlock_time", 0L))
            put("last_unlock_type", prefs.getString("last_unlock_type", "pin"))
            put("battery_start_level", prefs.getInt("battery_start_level", -1))
            put("current_battery_level", prefs.getInt("current_battery_level", -1))
            put("today_charge_time", prefs.getLong("today_charge_time", 0L))
            put("last_charge_start_time", prefs.getLong("last_charge_start_time", 0L))
            put("is_charging", prefs.getBoolean("is_charging", false))
            put("battery_hourly_log", prefs.getString("battery_hourly_log", "[]"))
            put("custom_app_usage_json", prefs.getString("custom_app_usage_json", "{}"))
            put("history", JSONArray(prefs.getString(keyHistoryRecords, "[]") ?: "[]"))
        }.toString()
    }

    private fun applyBackupJsonString(jsonStr: String): Boolean {
        return try {
            val root = JSONObject(jsonStr)
            val savedEmail = root.optString("google_account", "")
            val editor = prefs.edit()
                .putString(keyCurrentCycleDate, root.optString("current_date", getCurrentCycleDate()))
                .putInt("reset_hour", root.optInt("reset_hour", 0))
                .putInt("reset_minute", root.optInt("reset_minute", 1))
                .putString("app_theme_mode", root.optString("app_theme_mode", "system"))
                .putInt("today_battery_used_pct", root.optInt("today_battery_used_pct", 0))
                .putLong("today_screen", root.optLong("today_screen", 0L))
                .putLong("today_wifi", root.optLong("today_wifi", 0L))
                .putLong("today_bluetooth", root.optLong("today_bluetooth", 0L))
                .putLong("today_data", root.optLong("today_data", 0L))
                .putLong("today_both_wifi_data", root.optLong("today_both_wifi_data", 0L))
                .putInt("today_count_both_wifi_data", root.optInt("today_count_both_wifi_data", 0))
                .putLong("today_gps", root.optLong("today_gps", 0L))
                .putInt("today_count_screen", root.optInt("today_count_screen", 0))
                .putInt("today_count_wifi", root.optInt("today_count_wifi", 0))
                .putInt("today_count_data", root.optInt("today_count_data", 0))
                .putInt("today_count_bluetooth", root.optInt("today_count_bluetooth", 0))
                .putInt("today_count_gps", root.optInt("today_count_gps", 0))
                .putInt("today_unlock_count", root.optInt("today_unlock_count", 0))
                .putInt("today_unlock_fingerprint", root.optInt("today_unlock_fingerprint", 0))
                .putInt("today_unlock_face", root.optInt("today_unlock_face", 0))
                .putInt("today_unlock_pin", root.optInt("today_unlock_pin", 0))
                .putLong("today_bt_buds_ms", root.optLong("today_bt_buds_ms", 0L))
                .putLong("today_bt_watch_ms", root.optLong("today_bt_watch_ms", 0L))
                .putLong("today_bt_auto_ms", root.optLong("today_bt_auto_ms", 0L))
                .putLong("last_unlock_time", root.optLong("last_unlock_time", 0L))
                .putString("last_unlock_type", root.optString("last_unlock_type", "pin"))
                .putInt("battery_start_level", root.optInt("battery_start_level", -1))
                .putInt("current_battery_level", root.optInt("current_battery_level", -1))
                .putLong("today_charge_time", root.optLong("today_charge_time", 0L))
                .putLong("last_charge_start_time", root.optLong("last_charge_start_time", 0L))
                .putBoolean("is_charging", root.optBoolean("is_charging", false))
                .putString("battery_hourly_log", root.optString("battery_hourly_log", "[]"))
                .putString("custom_app_usage_json", root.optString("custom_app_usage_json", "{}"))
                .putString(keyHistoryRecords, root.optJSONArray("history")?.toString() ?: "[]")
            if (savedEmail.isNotBlank()) editor.putString(keyGoogleEmail, savedEmail)
            editor.apply()
            true
        } catch (_: Exception) { false }
    }

    fun exportLocalBackup(): String {
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "FirstLogApp_Backup.json")
        file.writeText(buildBackupJsonString())
        return file.absolutePath
    }

    fun restoreLocalBackup(): Boolean {
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "FirstLogApp_Backup.json")
        if (!file.exists()) return false
        return applyBackupJsonString(file.readText())
    }

    fun exportBackupToUri(uri: Uri): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(buildBackupJsonString().toByteArray())
            }
            true
        } catch (_: Exception) { false }
    }

    fun restoreBackupFromUri(uri: Uri): Boolean {
        return try {
            val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: return false
            applyBackupJsonString(content)
        } catch (_: Exception) { false }
    }
}
