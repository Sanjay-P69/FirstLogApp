package com.sanjay.tracewise.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sanjay.tracewise.data.repository.DashboardRepository
import com.sanjay.tracewise.data.repository.DashboardSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withTimeoutOrNull

/** Reads storage; never registers sensors or changes tracking state. */
class DashboardViewModel(repository: DashboardRepository) : ViewModel() {
    private val refreshRequests = Channel<Unit>(Channel.CONFLATED)
    val state = flow {
        var previous = DashboardSnapshot().toUiState().copy(isLoading = true, hasLoadedData = false)
        while (true) {
            previous = try {
                repository.readSnapshot().toUiState()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                previous.copy(isLoading = false, hasError = true)
            }
            emit(previous)
            withTimeoutOrNull(3_000) { refreshRequests.receive() }
        }
    }.flowOn(Dispatchers.IO).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardSnapshot().toUiState().copy(isLoading = true, hasLoadedData = false)
    )

    suspend fun refresh() {
        refreshRequests.send(Unit)
    }

    class Factory(private val repository: DashboardRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(DashboardViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(repository) as T
        }
    }
}
