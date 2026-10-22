package com.Sanjay.firstlogapp

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import androidx.core.app.NotificationCompat
import com.sanjay.tracewise.R
import com.sanjay.tracewise.data.local.DataManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AutoBackupReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let { ctx ->
            val dataManager = DataManager(ctx)
            AutoBackupHelper.checkAndRunSundayAutoBackup(ctx, dataManager, forceRun = false)
            AutoBackupHelper.scheduleSundayAlarm(ctx)
        }
    }
}

object AutoBackupHelper {

    private const val PREFS_KEY_LAST_SUNDAY_BACKUP = "last_sunday_autobackup_date"
    private const val PREFS_KEY_AUTO_BACKUP_ENABLED = "auto_backup_sunday_enabled"
    private const val NOTIFICATION_CHANNEL_ID = "firstlog_backup_channel"

    fun isAutoBackupEnabled(dataManager: DataManager): Boolean =
        dataManager.getPrefs().getBoolean(PREFS_KEY_AUTO_BACKUP_ENABLED, true)

    fun setAutoBackupEnabled(dataManager: DataManager, enabled: Boolean) {
        dataManager.getPrefs().edit().putBoolean(PREFS_KEY_AUTO_BACKUP_ENABLED, enabled).apply()
    }

    fun getLastBackupInfo(dataManager: DataManager): String =
        dataManager.getPrefs().getString("last_sunday_autobackup_info", "None yet")
            ?: "None yet"

    fun checkAndRunSundayAutoBackup(
        context: Context,
        dataManager: DataManager,
        forceRun: Boolean = false
    ): Boolean {
        if (!isAutoBackupEnabled(dataManager) && !forceRun) return false

        val now = Calendar.getInstance()
        val isSunday = now.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
        val hour = now.get(Calendar.HOUR_OF_DAY)

        // Must be Sunday at or after 6:00 AM
        if (!forceRun && (!isSunday || hour < 6)) {
            return false
        }

        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now.time)
        val lastBackupDate =
            dataManager.getPrefs().getString(PREFS_KEY_LAST_SUNDAY_BACKUP, "") ?: ""

        if (!forceRun && todayDate == lastBackupDate) {
            // Already backed up this Sunday!
            return false
        }

        return performBackup(context, dataManager, todayDate)
    }

    fun performBackup(context: Context, dataManager: DataManager, dateStr: String): Boolean = try {
        val backupJson = dataManager.buildBackupJsonString()
        val googleEmail = dataManager.getGoogleAccount()
        val hasGoogleDrive = !googleEmail.isNullOrBlank()

        val prefix = if (hasGoogleDrive) "GoogleDrive_Sunday" else "Local_Sunday"
        val fileName = "FirstLogApp_${prefix}_Backup_$dateStr.json"

        // Save to primary documents directory
        val docsDir =
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
        val backupFile = File(docsDir, fileName)
        backupFile.writeText(backupJson)

        // Also keep standard FirstLogApp_Backup.json updated
        dataManager.exportLocalBackup()

        val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        val info = if (hasGoogleDrive) {
            "Saved locally on $dateStr at $timeStr"
        } else {
            "Saved locally on $dateStr at $timeStr"
        }

        dataManager.getPrefs().edit()
            .putString(PREFS_KEY_LAST_SUNDAY_BACKUP, dateStr)
            .putString("last_sunday_autobackup_info", info)
            .apply()

        showBackupNotification(context, hasGoogleDrive, googleEmail, fileName)
        true
    } catch (e: Exception) {
        false
    }

    private fun showBackupNotification(
        context: Context,
        hasGoogleDrive: Boolean,
        email: String?,
        fileName: String
    ) {
        val nm =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Auto Backup Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            nm.createNotificationChannel(channel)
        }

        val destText = "local device storage"
        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_save)
            .setContentTitle("Sunday 6:00 AM Auto Backup Completed")
            .setContentText("Weekly backup saved to $destText ($fileName)")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Weekly backup saved successfully to $destText.\nFile: $fileName"
                )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        nm.notify(1005, notification)
    }

    fun scheduleSundayAlarm(context: Context) {
        try {
            val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, AutoBackupReceiver::class.java)
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pi = PendingIntent.getBroadcast(context, 2001, intent, flags)

            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 6)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // Find next Sunday at 6:00 AM
            while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY ||
                cal.timeInMillis <= System.currentTimeMillis()
            ) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            } else {
                am.set(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            }
        } catch (_: Exception) {}
    }
}
