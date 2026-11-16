package com.tenants.app.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tenants.app.data.local.DataSyncBus
import com.tenants.app.data.local.SyncTopic
import com.tenants.app.data.remote.TenantsApiService
import com.tenants.app.data.remote.model.DashboardResponse
import com.tenants.app.data.remote.model.parseApiError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false, // TopAppBar icon spinning state
    val isPullRefreshing: Boolean = false, // PullToRefreshBox drop-down indicator state
    val data: DashboardResponse? = null,
    val errorMessage: String? = null
)

sealed interface DashboardEvent {
    data object Refresh : DashboardEvent // TopAppBar icon tap
    data object PullRefresh : DashboardEvent // PullToRefreshBox swipe gesture
    data object ClearError : DashboardEvent
}

/**
 * ViewModel for property metrics and dashboard insights with reactive sync support.
 */
class DashboardViewModel(
    private val apiService: TenantsApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState(isLoading = true))
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard(isSilent = false)
        observeSyncEvents()
    }

    private fun observeSyncEvents() {
        viewModelScope.launch {
            DataSyncBus.syncEvents.collect { topic ->
                if (topic == SyncTopic.DASHBOARD || topic == SyncTopic.ALL) {
                    loadDashboard(isSilent = true)
                }
            }
        }
    }

    fun onEvent(event: DashboardEvent) {
        when (event) {
            is DashboardEvent.Refresh -> loadDashboard(isSilent = false, isPullRefresh = false)
            is DashboardEvent.PullRefresh -> loadDashboard(isSilent = false, isPullRefresh = true)
            is DashboardEvent.ClearError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    fun loadDashboard(isSilent: Boolean = false, isPullRefresh: Boolean = false) {
        viewModelScope.launch {
            if (!isSilent) {
                if (isPullRefresh) {
                    _uiState.update { it.copy(isPullRefreshing = true, errorMessage = null) }
                } else if (_uiState.value.data != null) {
                    _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
                } else {
                    _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                }
            }
            try {
                val response = apiService.getDashboardOverview()
                if (response.isSuccessful && response.body() != null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            isPullRefreshing = false,
                            data = response.body(),
                            errorMessage = null
                        )
                    }
                } else {
                    val errMsg = parseApiError(response)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            isPullRefreshing = false,
                            errorMessage = errMsg
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isPullRefreshing = false,
                        errorMessage = e.localizedMessage ?: "Network error"
                    )
                }
            }
        }
    }
}
