package com.Sanjay.firstlogapp.ui.analytics

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.Sanjay.firstlogapp.data.local.DataManager
import com.Sanjay.firstlogapp.data.repository.StoredAnalyticsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Android/data adapter. Rendering never reads a provider or storage. */
@Composable
fun AnalyticsScreen(dataManager: DataManager) {
    val repository = remember(dataManager) { StoredAnalyticsRepository(dataManager) }
    var metric by rememberSaveable { mutableStateOf("screen") }
    var period by rememberSaveable { mutableStateOf("Day") }
    val state by produceState(AnalyticsUiState(isLoading = true), metric, period) {
        value = AnalyticsUiState(isLoading = true)
        value = try {
            val points = withContext(Dispatchers.IO) { repository.readChart(metric, period) }
            AnalyticsUiState(points = points.map { TrendPoint(it.first, it.second) })
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            AnalyticsUiState(hasError = true)
        }
    }
    AnalyticsContent(state, metric, period, { metric = it }, { period = it })
}
