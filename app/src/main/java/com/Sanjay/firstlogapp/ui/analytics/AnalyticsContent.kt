package com.Sanjay.firstlogapp.ui.analytics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.Sanjay.firstlogapp.R
import java.util.Locale

data class TrendPoint(val label: String, val hours: Float)
data class AnalyticsUiState(val points: List<TrendPoint> = emptyList(), val isLoading: Boolean = false, val hasError: Boolean = false)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsContent(
    state: AnalyticsUiState,
    metric: String,
    period: String,
    onMetric: (String) -> Unit,
    onPeriod: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val metrics = listOf("screen" to R.string.screen_time, "wifi" to R.string.wifi,
        "data" to R.string.mobile_data, "bluetooth" to R.string.bluetooth,
        "gps" to R.string.location_enabled, "call_in" to R.string.incoming_calls, "call_out" to R.string.outgoing_calls)
    val periods = listOf("Day" to R.string.day, "Month" to R.string.month, "Year" to R.string.year)
    val maximum = state.points.maxOfOrNull { it.hours }?.coerceAtLeast(0.01f) ?: 1f
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text(stringResource(R.string.trends_tab), style = MaterialTheme.typography.displaySmall) }
        item { Text(stringResource(R.string.trends_subtitle), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(metrics, key = { it.first }) { (id, title) ->
                    FilterChip(metric == id, { onMetric(id) }, label = { Text(stringResource(title)) })
                }
            }
        }
        item {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                periods.forEachIndexed { index, (id, label) ->
                    SegmentedButton(selected = period == id, onClick = { onPeriod(id) },
                        shape = SegmentedButtonDefaults.itemShape(index, periods.size)) { Text(stringResource(label)) }
                }
            }
        }
        if (state.isLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        if (state.hasError) item { Text(stringResource(R.string.trends_error), color = MaterialTheme.colorScheme.error) }
        if (!state.isLoading && !state.hasError) {
            item {
                Card(shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.recorded_time), style = MaterialTheme.typography.titleMedium)
                        Text(String.format(Locale.getDefault(), "%.1f h", state.points.sumOf { it.hours.toDouble() }),
                            style = MaterialTheme.typography.displaySmall)
                        Text(stringResource(R.string.trend_scale_hint), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            if (state.points.none { it.hours > 0f }) item { Text(stringResource(R.string.no_trend_data), style = MaterialTheme.typography.bodyLarge) }
            items(state.points.withIndex().toList(), key = { "${it.index}-${it.value.label}" }) { (_, point) ->
                ListItem(
                    headlineContent = { Text(point.label) },
                    supportingContent = {
                        LinearProgressIndicator(progress = { (point.hours / maximum).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                    },
                    trailingContent = { Text(String.format(Locale.getDefault(), "%.1f h", point.hours)) }
                )
            }
        }
    }
}
