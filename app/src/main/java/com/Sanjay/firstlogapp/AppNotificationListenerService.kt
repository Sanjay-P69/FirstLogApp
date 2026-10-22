package com.Sanjay.firstlogapp

import android.app.Notification
import android.app.RemoteInput
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class ActiveNotificationItem(
    val key: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val content: String,
    val postTime: Long,
    val canReply: Boolean,
    val category: String = "Others"
)

data class NotificationHistoryItem(
    val id: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val content: String,
    val timestamp: Long,
    val category: String, // "Call", "Message", "Gmail", "WhatsApp", "Others"
    val canReply: Boolean = false
)

class AppNotificationListenerService : NotificationListenerService() {

    companion object {
        private var instance: AppNotificationListenerService? = null
        private val _notifications = MutableStateFlow<List<ActiveNotificationItem>>(emptyList())
        val notifications: StateFlow<List<ActiveNotificationItem>> = _notifications.asStateFlow()

        private val _history = MutableStateFlow<List<NotificationHistoryItem>>(emptyList())
        val history: StateFlow<List<NotificationHistoryItem>> = _history.asStateFlow()

        private const val PREFS_NAME = "notification_history_prefs"
        private const val KEY_HISTORY_DATE = "history_date"
        private const val KEY_HISTORY_JSON = "history_json"

        fun isPermissionGranted(context: Context): Boolean {
            val flat =
                Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
                    ?: ""
            val comp = ComponentName(
                context,
                AppNotificationListenerService::class.java
            ).flattenToString()
            return flat.contains(comp) || flat.contains(context.packageName)
        }

        fun classifyCategory(packageName: String, categoryTag: String? = null): String {
            val lower = packageName.lowercase(Locale.getDefault())
            if (lower.contains("whatsapp")) return "WhatsApp"
            if (lower == "com.google.android.gm" || lower.contains("gmail") ||
                lower.contains(".mail") ||
                lower.contains("outlook")
            ) {
                return "Gmail"
            }
            if (lower.contains("dialer") || lower.contains("telecom") || lower.contains("phone") ||
                lower.contains("truecaller") ||
                categoryTag == Notification.CATEGORY_CALL ||
                categoryTag == Notification.CATEGORY_MISSED_CALL
            ) {
                return "Call"
            }
            if (lower.contains("messaging") || lower.contains("mms") || lower.contains("sms") ||
                lower.contains("telegram") ||
                lower.contains("signal") ||
                categoryTag == Notification.CATEGORY_MESSAGE
            ) {
                return "Message"
            }
            return "Others"
        }

        fun loadHistory(context: Context) {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val savedDate = prefs.getString(KEY_HISTORY_DATE, "") ?: ""
                if (savedDate != todayStr) {
                    // Start fresh for new day
                    prefs.edit().putString(
                        KEY_HISTORY_DATE,
                        todayStr
                    ).putString(KEY_HISTORY_JSON, "[]").apply()
                    _history.value = emptyList()
                    return
                }
                val jsonStr = prefs.getString(KEY_HISTORY_JSON, "[]") ?: "[]"
                val array = JSONArray(jsonStr)
                val list = mutableListOf<NotificationHistoryItem>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        NotificationHistoryItem(
                            id = obj.optString("id"),
                            packageName = obj.optString("packageName"),
                            appName = obj.optString("appName"),
                            title = obj.optString("title"),
                            content = obj.optString("content"),
                            timestamp = obj.optLong("timestamp"),
                            category = obj.optString("category", "Others"),
                            canReply = obj.optBoolean("canReply", false)
                        )
                    )
                }
                _history.value = list
            } catch (_: Exception) {}
        }

        fun saveHistory(context: Context, items: List<NotificationHistoryItem>) {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val array = JSONArray()
                items.take(300).forEach { item ->
                    val obj = JSONObject().apply {
                        put("id", item.id)
                        put("packageName", item.packageName)
                        put("appName", item.appName)
                        put("title", item.title)
                        put("content", item.content)
                        put("timestamp", item.timestamp)
                        put("category", item.category)
                        put("canReply", item.canReply)
                    }
                    array.put(obj)
                }
                prefs.edit()
                    .putString(KEY_HISTORY_DATE, todayStr)
                    .putString(KEY_HISTORY_JSON, array.toString())
                    .apply()
                _history.value = items
            } catch (_: Exception) {}
        }

        fun clearHistory(context: Context) {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                prefs.edit()
                    .putString(KEY_HISTORY_DATE, todayStr)
                    .putString(KEY_HISTORY_JSON, "[]")
                    .apply()
                _history.value = emptyList()
            } catch (_: Exception) {}
        }

        fun markAsRead(key: String): Boolean = try {
            instance?.cancelNotification(key)
            _notifications.value = _notifications.value.filter { it.key != key }
            true
        } catch (_: Exception) {
            false
        }

        fun directReply(context: Context, key: String, replyText: String): Boolean {
            val inst = instance ?: return false
            val sbns =
                try {
                    inst.activeNotifications
                } catch (_: Exception) {
                    null
                } ?: emptyArray()
            val targetSbn = sbns.firstOrNull { it.key == key } ?: return false
            val actions = targetSbn.notification.actions ?: return false

            for (action in actions) {
                val remoteInputs = action.remoteInputs ?: continue
                for (remoteInput in remoteInputs) {
                    if (remoteInput.allowFreeFormInput || remoteInput.choices != null) {
                        val intent = Intent()
                        val bundle = Bundle()
                        bundle.putCharSequence(remoteInput.resultKey, replyText)
                        RemoteInput.addResultsToIntent(arrayOf(remoteInput), intent, bundle)
                        return try {
                            action.actionIntent.send(context, 0, intent)
                            markAsRead(key)
                            true
                        } catch (_: Exception) {
                            false
                        }
                    }
                }
            }
            return false
        }

        fun refreshNotifications() {
            instance?.syncActiveNotifications()
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        loadHistory(this)
        syncActiveNotifications()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        instance = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn != null) {
            recordToHistory(sbn)
        }
        syncActiveNotifications()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        syncActiveNotifications()
    }

    private fun recordToHistory(sbn: StatusBarNotification) {
        try {
            val flags = sbn.notification.flags
            val isOngoing = (flags and Notification.FLAG_ONGOING_EVENT) != 0
            val isForeground = (flags and Notification.FLAG_FOREGROUND_SERVICE) != 0
            val isLocalBiz = (flags and Notification.FLAG_LOCAL_ONLY) != 0 && isOngoing
            if (isOngoing || isForeground || isLocalBiz) return

            if (sbn.packageName == packageName) return

            val extras = sbn.notification.extras
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
                ?: extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()
                ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
                ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
                ?: extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
                ?: ""

            if (title.isBlank() && text.isBlank()) return

            val appName = resolveAppName(sbn.packageName)
            val category = classifyCategory(sbn.packageName, sbn.notification.category)
            val canReply = hasReplyAction(sbn.notification)

            val current = _history.value.toMutableList()
            val isDuplicate = current.any {
                it.packageName == sbn.packageName &&
                    it.title == title &&
                    it.content == text &&
                    kotlin.math.abs(it.timestamp - sbn.postTime) < 3000L
            }

            if (!isDuplicate) {
                val newItem = NotificationHistoryItem(
                    id = "${sbn.key}_${sbn.postTime}",
                    packageName = sbn.packageName,
                    appName = appName,
                    title = title,
                    content = text,
                    timestamp = sbn.postTime,
                    category = category,
                    canReply = canReply
                )
                current.add(0, newItem)
                saveHistory(this, current)
            }
        } catch (_: Exception) {}
    }

    private fun syncActiveNotifications() {
        try {
            val sbns = activeNotifications ?: emptyArray()
            val list = mutableListOf<ActiveNotificationItem>()

            for (sbn in sbns) {
                val flags = sbn.notification.flags
                val isOngoing = (flags and Notification.FLAG_ONGOING_EVENT) != 0
                val isForeground = (flags and Notification.FLAG_FOREGROUND_SERVICE) != 0
                val isLocalBiz = (flags and Notification.FLAG_LOCAL_ONLY) != 0 && isOngoing
                if (isOngoing || isForeground || isLocalBiz) continue

                if (sbn.packageName == packageName) return

                val extras = sbn.notification.extras
                val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
                    ?: extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()
                    ?: ""
                val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
                    ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
                    ?: extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
                    ?: ""

                if (title.isBlank() && text.isBlank()) continue

                val appName = resolveAppName(sbn.packageName)
                val canReply = hasReplyAction(sbn.notification)
                val category = classifyCategory(sbn.packageName, sbn.notification.category)

                list.add(
                    ActiveNotificationItem(
                        key = sbn.key,
                        packageName = sbn.packageName,
                        appName = appName,
                        title = title,
                        content = text,
                        postTime = sbn.postTime,
                        canReply = canReply,
                        category = category
                    )
                )
            }

            _notifications.value = list.sortedByDescending { it.postTime }
        } catch (_: Exception) {}
    }

    private fun hasReplyAction(notification: Notification): Boolean {
        val actions = notification.actions ?: return false
        for (action in actions) {
            val remoteInputs = action.remoteInputs ?: continue
            for (remoteInput in remoteInputs) {
                if (remoteInput.allowFreeFormInput || remoteInput.choices != null) {
                    return true
                }
            }
        }
        return false
    }

    private fun resolveAppName(pkg: String): String = try {
        val pm = packageManager
        val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getApplicationInfo(pkg, 0)
        }
        pm.getApplicationLabel(appInfo).toString()
    } catch (_: Exception) {
        val parts = pkg.split(".")
        val name = parts.lastOrNull() ?: pkg
        name.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
    }
}
