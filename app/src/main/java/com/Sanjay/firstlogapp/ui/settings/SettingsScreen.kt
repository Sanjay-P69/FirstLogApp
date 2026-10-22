package com.Sanjay.firstlogapp.ui.settings

import android.accounts.AccountManager
import android.app.Activity
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.Sanjay.firstlogapp.AutoBackupHelper
import com.Sanjay.firstlogapp.R
import com.Sanjay.firstlogapp.data.local.DataManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Platform adapter for settings; all file operations run on Dispatchers.IO. */
@Composable
fun SettingsScreen(
    dataManager: DataManager,
    currentThemeMode: String,
    onThemeChange: (String) -> Unit,
    currentThumbnailStyle: String = "system_icon",
    onThumbnailStyleChange: (String) -> Unit = {},
    onForceResetToday: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var state by remember { mutableStateOf(SettingsUiState(busy = true)) }
    var confirmReset by remember { mutableStateOf(false) }
    var confirmRestore by remember { mutableStateOf<SettingsAction?>(null) }

    suspend fun reload() {
        state = withContext(Dispatchers.IO) {
            SettingsUiState(resetTime = dataManager.getResetTimeString(), account = dataManager.getGoogleAccount().orEmpty(),
                autoBackup = AutoBackupHelper.isAutoBackupEnabled(dataManager), lastBackup = AutoBackupHelper.getLastBackupInfo(dataManager),
                apiKey = dataManager.getGeminiApiKey(), batteryExempt =
                    (context.getSystemService(Context.POWER_SERVICE) as? PowerManager)?.isIgnoringBatteryOptimizations(context.packageName) == true)
        }
    }
    fun work(after: () -> Unit = {}, operation: () -> Boolean) {
        if (state.busy) return
        state = state.copy(busy = true)
        scope.launch {
            try {
                val ok = withContext(Dispatchers.IO) { operation() }
                reload()
                if (ok) after()
                snackbar.showSnackbar(context.getString(if (ok) R.string.operation_success else R.string.operation_failed))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                snackbar.showSnackbar(context.getString(R.string.operation_failed))
            } finally { state = state.copy(busy = false) }
        }
    }
    LaunchedEffect(dataManager) { reload() }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) work { dataManager.exportBackupToUri(uri) }
    }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) work(after = {
            onThemeChange(dataManager.getThemeMode())
            onThumbnailStyleChange(dataManager.getThumbnailStyle())
        }) { dataManager.restoreBackupFromUri(uri) }
    }
    val account = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)?.let { email -> work { dataManager.saveGoogleAccount(email); true } }
        }
    }
    val battery = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        scope.launch { reload() }
    }
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        SettingsContent(state, currentThemeMode, currentThumbnailStyle, onAction = { action ->
            when (action) {
                is SettingsAction.Theme -> onThemeChange(action.value)
                is SettingsAction.Thumbnail -> onThumbnailStyleChange(action.value)
                is SettingsAction.EditKey -> state = state.copy(apiKey = action.value)
                is SettingsAction.AutoBackup -> work {
                    AutoBackupHelper.setAutoBackupEnabled(dataManager, action.enabled)
                    if (action.enabled) AutoBackupHelper.scheduleSundayAlarm(context)
                    true
                }
                SettingsAction.SaveKey -> { val key = state.apiKey; work { dataManager.setGeminiApiKey(key); true } }
                SettingsAction.Export -> export.launch("FirstLog_Backup.json")
                SettingsAction.Restore -> confirmRestore = action
                SettingsAction.LocalExport -> work { dataManager.exportLocalBackup(); true }
                SettingsAction.LocalRestore -> confirmRestore = action
                SettingsAction.BackupNow -> work { AutoBackupHelper.checkAndRunSundayAutoBackup(context, dataManager, forceRun = true) }
                SettingsAction.Account -> account.launch(AccountManager.newChooseAccountIntent(null, null, arrayOf("com.google"), null, null, null, null))
                SettingsAction.BatterySettings -> battery.launch(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                SettingsAction.ResetTime -> {
                    val (hour, minute) = dataManager.getResetTime()
                    TimePickerDialog(context, { _, h, m -> work { dataManager.setResetTime(h, m); true } }, hour, minute, true).show()
                }
                SettingsAction.ResetToday -> confirmReset = true
            }
        }, modifier = androidx.compose.ui.Modifier.padding(padding))
    }
    if (confirmReset) {
        AlertDialog(onDismissRequest = { confirmReset = false }, title = { Text(stringResource(R.string.start_new_cycle)) },
            text = { Text(stringResource(R.string.reset_today_hint)) },
            confirmButton = { TextButton({ confirmReset = false; onForceResetToday() }) { Text(stringResource(R.string.confirm)) } },
            dismissButton = { TextButton({ confirmReset = false }) { Text(stringResource(R.string.cancel)) } })
    }
    confirmRestore?.let { action ->
        AlertDialog(onDismissRequest = { confirmRestore = null }, title = { Text(stringResource(R.string.restore_backup)) },
            text = { Text(stringResource(R.string.restore_warning)) },
            confirmButton = { TextButton({
                confirmRestore = null
                if (action == SettingsAction.Restore) restore.launch(arrayOf("application/json"))
                else work(after = {
                    onThemeChange(dataManager.getThemeMode())
                    onThumbnailStyleChange(dataManager.getThumbnailStyle())
                }) { dataManager.restoreLocalBackup() }
            }) { Text(stringResource(R.string.confirm)) } },
            dismissButton = { TextButton({ confirmRestore = null }) { Text(stringResource(R.string.cancel)) } })
    }
}
