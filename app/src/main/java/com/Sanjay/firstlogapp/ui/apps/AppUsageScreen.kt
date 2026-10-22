package com.Sanjay.firstlogapp.ui.apps

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.Sanjay.firstlogapp.R
import com.Sanjay.firstlogapp.ui.dashboard.AppUsageUiItem

/** All rows are supplied by the presentation layer; opening this screen performs no I/O. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUsageContent(apps: List<AppUsageUiItem>, onBack: () -> Unit, modifier: Modifier = Modifier) {
    var expanded by rememberSaveable { mutableStateOf<String?>(null) }
    Scaffold(modifier = modifier, topBar = {
        TopAppBar(title = { Text(stringResource(R.string.app_usage)) }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.navigate_back)) }
        })
    }) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (apps.isEmpty()) item { Text(stringResource(R.string.no_app_activity)) }
            items(apps, key = { it.id }) { app ->
                Card(onClick = { expanded = if (expanded == app.id) null else app.id }, shape = MaterialTheme.shapes.extraLarge) {
                    ListItem(headlineContent = { Text(app.name) }, supportingContent = { Text(app.duration) },
                        trailingContent = { Text(stringResource(R.string.app_opens, app.launches)) },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest))
                    AnimatedVisibility(expanded == app.id) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(app.id, style = MaterialTheme.typography.bodyMedium)
                            Text(stringResource(R.string.app_foreground, app.foreground), style = MaterialTheme.typography.bodyMedium)
                            Text(stringResource(R.string.app_background, app.background), style = MaterialTheme.typography.bodyMedium)
                            Text(stringResource(R.string.app_last_session, app.lastSession), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
