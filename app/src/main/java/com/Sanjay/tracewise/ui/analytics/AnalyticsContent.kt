package com.sanjay.tracewise.ui.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sanjay.tracewise.R
import com.sanjay.tracewise.core.formatDurationMillis
import com.sanjay.tracewise.ui.charts.ChartPointState
import com.sanjay.tracewise.ui.charts.CompactChart
import java.util.Locale

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
    val metrics = listOf(
        "screen" to R.string.screen_time,
        "wifi" to R.string.wifi,
        "data" to R.string.mobile_data,
        "bluetooth" to R.string.bluetooth,
        "gps" to R.string.location_enabled,
        "call_in" to R.string.incoming_calls,
        "call_out" to R.string.outgoing_calls
    )
    val periods = listOf("Day" to R.string.day, "Month" to R.string.month, "Year" to R.string.year)
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                stringResource(R.string.trends_tab),
                style = MaterialTheme.typography.headlineMedium
            )
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(metrics, key = { it.first }) { (id, title) ->
                    FilterChip(
                        metric == id,
                        { onMetric(id) },
                        label = { Text(stringResource(title)) }
                    )
                }
            }
        }
        item {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                periods.forEachIndexed { index, (id, label) ->
                    SegmentedButton(
                        selected = period == id,
                        onClick = { onPeriod(id) },
                        shape = SegmentedButtonDefaults.itemShape(index, periods.size)
                    ) {
                        Text(stringResource(label))
                    }
                }
            }
        }
        if (state.isLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        if (state.hasError || state.accessDenied) {
            item {
                Text(
                    stringResource(
                        if (state.accessDenied) {
                            R.string.chart_calls_denied
                        } else {
                            R.string.trends_error
                        }
                    ),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        if (!state.isLoading && !state.hasError && !state.accessDenied) {
            item {
                AnalyticsChartCard(state)
            }
        }
    }
}

@Composable
internal fun AnalyticsChartCard(state: AnalyticsUiState, modifier: Modifier = Modifier) {
    val noRecord = stringResource(R.string.chart_no_record)
    val future = stringResource(R.string.chart_future)
    val partial = stringResource(R.string.chart_partial)
    val points = state.points.map { point ->
        point.copy(
            valueLabel = when (point.state) {
                ChartPointState.NO_RECORD -> noRecord
                ChartPointState.FUTURE -> future
                ChartPointState.PARTIAL -> "${point.valueLabel} · $partial"
                ChartPointState.RECORDED -> point.valueLabel
            }
        )
    }
    val description = stringResource(R.string.chart_description, points.size)
    Card(
        modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.recorded_time),
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                formatDurationMillis(state.totalMillis),
                style = MaterialTheme.typography.headlineMedium
            )
            CompactChart(points, description, axisValue = { hours ->
                String.format(Locale.getDefault(), if (hours >= 10) "%.0f h" else "%.1f h", hours)
            })
            Text(
                stringResource(
                    if (state.usesTrackingCycles) {
                        R.string.chart_cycle_scope
                    } else {
                        R.string.chart_calendar_scope
                    }
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
