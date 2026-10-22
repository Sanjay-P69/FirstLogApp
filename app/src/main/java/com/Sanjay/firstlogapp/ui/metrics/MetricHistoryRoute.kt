package com.Sanjay.firstlogapp.ui.metrics

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.Sanjay.firstlogapp.data.device.CallHistoryReader
import com.Sanjay.firstlogapp.data.local.DataManager
import com.Sanjay.firstlogapp.data.model.MetricHistory
import com.Sanjay.firstlogapp.data.repository.StoredAnalyticsRepository
import com.Sanjay.firstlogapp.data.repository.StoredMetricHistoryRepository
import com.Sanjay.firstlogapp.ui.analytics.toUiState
import com.Sanjay.firstlogapp.ui.model.MetricItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Platform adapters and IO remain outside the rendering-only full-screen destination. */
@Composable
fun MetricHistoryRoute(metric: MetricItem, dataManager: DataManager, onBack: () -> Unit) {
    val context = LocalContext.current
    val resources = context.resources
    val reader = remember(context) { CallHistoryReader(context.applicationContext) }
    val repository = remember(dataManager, reader) {
        StoredMetricHistoryRepository(dataManager, reader)
    }
    val analytics = remember(dataManager, reader) { StoredAnalyticsRepository(dataManager, reader) }
    var retry by remember { mutableIntStateOf(0) }
    val state by produceState(MetricHistoryUiState(metric.title, isLoading = true), metric.id, retry) {
        value = MetricHistoryUiState(metric.title, isLoading = true)
        value = try {
            withContext(Dispatchers.IO) {
                val raw = repository.read(metric.id)
                val mapped = raw.toUiState(metric.title, resources)
                if (raw is MetricHistory.Radio) {
                    mapped.copy(analytics = analytics.readChart(metric.id, "Month").toUiState("Month"))
                } else mapped
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: SecurityException) {
            MetricHistoryUiState(metric.title, accessDenied = true)
        } catch (_: Exception) {
            MetricHistoryUiState(metric.title, hasError = true)
        }
    }
    MetricHistoryContent(state, onBack, { retry++ }, onDial = { number ->
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", number, null)))
    })
}
