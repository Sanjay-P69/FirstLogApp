package com.Sanjay.firstlogapp.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.Sanjay.firstlogapp.R

data class SettingsUiState(
    val resetTime: String = "",
    val account: String = "",
    val autoBackup: Boolean = false,
    val lastBackup: String = "",
    val apiKey: String = "",
    val batteryExempt: Boolean = false,
    val busy: Boolean = false
)

sealed interface SettingsAction {
    data class Theme(val value: String) : SettingsAction
    data class Thumbnail(val value: String) : SettingsAction
    data class AutoBackup(val enabled: Boolean) : SettingsAction
    data class EditKey(val value: String) : SettingsAction
    data object SaveKey : SettingsAction
    data object ResetTime : SettingsAction
    data object ResetToday : SettingsAction
    data object Export : SettingsAction
    data object Restore : SettingsAction
    data object LocalExport : SettingsAction
    data object LocalRestore : SettingsAction
    data object BackupNow : SettingsAction
    data object Account : SettingsAction
    data object BatterySettings : SettingsAction
}

@Composable
fun SettingsContent(
    state: SettingsUiState,
    theme: String,
    thumbnail: String,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item { Text(stringResource(R.string.settings_tab), style = MaterialTheme.typography.displaySmall) }
        if (state.busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        item {
            SettingsGroup(R.string.appearance_title) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("system" to R.string.theme_system, "light" to R.string.theme_light, "dark" to R.string.theme_dark)) { (id, label) ->
                        FilterChip(theme == id, { onAction(SettingsAction.Theme(id)) }, label = { Text(stringResource(label)) }, enabled = !state.busy)
                    }
                }
                Text(stringResource(R.string.thumbnail_style), style = MaterialTheme.typography.titleSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("system_icon" to R.string.thumbnail_system, "light_badge" to R.string.thumbnail_light,
                        "vibrant" to R.string.thumbnail_vibrant, "pastel" to R.string.thumbnail_pastel)) { (id, label) ->
                        FilterChip(thumbnail == id, { onAction(SettingsAction.Thumbnail(id)) }, label = { Text(stringResource(label)) }, enabled = !state.busy)
                    }
                }
            }
        }
        item {
            SettingsGroup(R.string.tracking_title) {
                Text(stringResource(R.string.cycle_time, state.resetTime), style = MaterialTheme.typography.bodyLarge)
                OutlinedButton({ onAction(SettingsAction.ResetTime) }, enabled = !state.busy) { Text(stringResource(R.string.change_cycle_time)) }
                TextButton({ onAction(SettingsAction.BatterySettings) }) {
                    Text(stringResource(if (state.batteryExempt) R.string.review_background_access else R.string.allow_background_access))
                }
                Text(stringResource(R.string.background_limit_hint), style = MaterialTheme.typography.bodyMedium)
            }
        }
        item {
            SettingsGroup(R.string.backups_title) {
                Text(stringResource(R.string.backups_hint), style = MaterialTheme.typography.bodyMedium)
                Button({ onAction(SettingsAction.Export) }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.export_backup)) }
                OutlinedButton({ onAction(SettingsAction.Restore) }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.restore_backup)) }
                TextButton({ onAction(SettingsAction.LocalExport) }, enabled = !state.busy) { Text(stringResource(R.string.local_export)) }
                TextButton({ onAction(SettingsAction.LocalRestore) }, enabled = !state.busy) { Text(stringResource(R.string.local_restore)) }
                ListItem(headlineContent = { Text(stringResource(R.string.weekly_backup)) },
                    supportingContent = { Text(stringResource(R.string.weekly_backup_hint)) },
                    trailingContent = { Switch(state.autoBackup, { onAction(SettingsAction.AutoBackup(it)) }, enabled = !state.busy) },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow))
                Text(stringResource(R.string.last_backup, state.lastBackup), style = MaterialTheme.typography.bodyMedium)
                TextButton({ onAction(SettingsAction.BackupNow) }, enabled = !state.busy) { Text(stringResource(R.string.backup_now)) }
            }
        }
        item {
            SettingsGroup(R.string.summary_settings_title) {
                Text(stringResource(R.string.summary_privacy_hint), style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(value = state.apiKey, onValueChange = { onAction(SettingsAction.EditKey(it)) },
                    label = { Text(stringResource(R.string.gemini_key)) }, visualTransformation = PasswordVisualTransformation(),
                    singleLine = true, enabled = !state.busy, modifier = Modifier.fillMaxWidth())
                FilledTonalButton({ onAction(SettingsAction.SaveKey) }, enabled = !state.busy) { Text(stringResource(R.string.save_key)) }
                Text(stringResource(R.string.account_label_hint, state.account), style = MaterialTheme.typography.bodyMedium)
                TextButton({ onAction(SettingsAction.Account) }, enabled = !state.busy) { Text(stringResource(R.string.choose_account_label)) }
            }
        }
        item {
            SettingsGroup(R.string.activity_management) {
                Text(stringResource(R.string.reset_today_hint), style = MaterialTheme.typography.bodyMedium)
                OutlinedButton({ onAction(SettingsAction.ResetToday) }, enabled = !state.busy) { Text(stringResource(R.string.start_new_cycle)) }
            }
        }
    }
}

@Composable
private fun SettingsGroup(title: Int, content: @Composable ColumnScope.() -> Unit) {
    Card(shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleLarge)
            content()
        }
    }
}
