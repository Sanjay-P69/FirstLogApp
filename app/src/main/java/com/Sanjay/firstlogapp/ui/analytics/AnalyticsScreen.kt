package com.Sanjay.firstlogapp.ui.analytics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.Sanjay.firstlogapp.data.device.CallHistoryReader
import com.Sanjay.firstlogapp.data.local.DataManager
import com.Sanjay.firstlogapp.data.repository.StoredAnalyticsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AnalyticsScreen(dataManager: DataManager) {
    val context = LocalContext.current.applicationContext
    val repository = remember(dataManager, context) {
        StoredAnalyticsRepository(dataManager, CallHistoryReader(context))
    }
    var metric by rememberSaveable { mutableStateOf("screen") }
    var period by rememberSaveable { mutableStateOf("Day") }
    val state by produceState(AnalyticsUiState(isLoading = true), metric, period) {
        value = AnalyticsUiState(isLoading = true)
        value = try {
            withContext(Dispatchers.IO) { repository.readChart(metric, period).toUiState(period) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: SecurityException) {
            AnalyticsUiState(accessDenied = true)
        } catch (_: Exception) {
            AnalyticsUiState(hasError = true)
        }
    }
    AnalyticsContent(state, metric, period, { metric = it }, { period = it })
}
