package com.sanjay.tracewise.ui.apps

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sanjay.tracewise.R
import com.sanjay.tracewise.ui.dashboard.AppUsageUiItem

/** All rows are supplied by the presentation layer; opening this screen performs no I/O. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUsageContent(apps: List<AppUsageUiItem>, onBack: () -> Unit, modifier: Modifier = Modifier) {
    var expanded by rememberSaveable { mutableStateOf<String?>(null) }
    Scaffold(modifier = modifier, topBar = {
        TopAppBar(title = { Text(stringResource(R.string.app_usage)) }, navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.navigate_back))
            }
        })
    }) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (apps.isEmpty()) item { Text(stringResource(R.string.no_app_activity)) }
            items(apps, key = { it.id }) { app ->
                Card(onClick = {
                    expanded = if (expanded ==
                        app.id
                    ) {
                        null
                    } else {
                        app.id
                    }
                }, shape = MaterialTheme.shapes.extraLarge) {
                    ListItem(
                        headlineContent = {
                            Text(app.name)
                        },
                        supportingContent = { Text(app.duration) },
                        trailingContent = {
                            Text(stringResource(R.string.app_opens, app.launches))
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    )
                    AnimatedVisibility(expanded == app.id) {
                        Column(
                            Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(app.id, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                stringResource(R.string.app_foreground, app.foreground),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                stringResource(R.string.app_background, app.background),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                stringResource(R.string.app_last_session, app.lastSession),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}
