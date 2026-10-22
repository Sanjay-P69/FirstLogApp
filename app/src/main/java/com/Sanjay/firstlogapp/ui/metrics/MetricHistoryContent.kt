package com.Sanjay.firstlogapp.ui.metrics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.Sanjay.firstlogapp.R
import com.Sanjay.firstlogapp.ui.analytics.AnalyticsChartCard
import com.Sanjay.firstlogapp.ui.charts.CompactChart
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetricHistoryContent(
    state: MetricHistoryUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onDial: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable(state.title) { mutableStateOf<String?>(null) }
    Scaffold(modifier, topBar = {
        TopAppBar(title = { Text(state.title) }, navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.navigate_back))
            }
        })
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (state.isLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            if (state.hasError || state.accessDenied) item {
                Text(stringResource(if (state.accessDenied) R.string.chart_calls_denied
                    else R.string.activity_load_error), color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
            }
            state.analytics?.let { analytics -> item { AnalyticsChartCard(analytics) } }
            state.plot?.let { plot ->
                item {
                    Card(shape = MaterialTheme.shapes.extraLarge) {
                        Column(Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(plot.title, style = MaterialTheme.typography.titleMedium)
                            CompactChart(plot.points, plot.title, axisValue = { value ->
                                String.format(Locale.getDefault(), if (plot.isPercentage)
                                    "%.0f%%" else "%.0f", value)
                            }, style = plot.style, fixedMaximum = plot.maximum)
                        }
                    }
                }
            }
            state.sections.forEachIndexed { sectionIndex, section ->
                item(key = "heading-$sectionIndex") {
                    if (section.expandable) {
                        val open = expanded == section.title
                        val status = stringResource(if (open) R.string.expanded else R.string.collapsed)
                        Card(onClick = { expanded = if (open) null else section.title },
                            modifier = Modifier.fillMaxWidth().semantics {
                                stateDescription = status
                            }) {
                            ListItem(headlineContent = { Text(section.title) }, trailingContent = {
                                Icon(if (open) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                    contentDescription = null)
                            })
                        }
                    } else Text(section.title, style = MaterialTheme.typography.titleMedium)
                }
                if (!section.expandable || expanded == section.title) {
                    items(section.rows.withIndex().toList(), key = { "row-$sectionIndex-${it.index}" }) {
                        (_, row) ->
                        ListItem(headlineContent = { Text(row.title) },
                            supportingContent = {
                                Column {
                                    Text(row.value, style = MaterialTheme.typography.bodyLarge)
                                    row.supporting?.let { Text(it) }
                                }
                            }, trailingContent = {
                                row.dialNumber?.let { number ->
                                    IconButton(onClick = { onDial(number) }) {
                                        Icon(Icons.Outlined.Call,
                                            contentDescription = stringResource(R.string.dial_number))
                                    }
                                }
                            })
                    }
                }
            }
        }
    }
}
