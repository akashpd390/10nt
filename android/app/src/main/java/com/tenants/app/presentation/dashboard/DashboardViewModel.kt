package com.tenants.app.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tenants.app.data.remote.TenantsApiService
import com.tenants.app.data.remote.model.DashboardResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = false,
    val data: DashboardResponse? = null,
    val errorMessage: String? = null
)

sealed interface DashboardEvent {
    data object Refresh : DashboardEvent
    data object ClearError : DashboardEvent
}

/**
 * ViewModel for property metrics and dashboard insights.
 */
class DashboardViewModel(
    private val apiService: TenantsApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState(isLoading = true))
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun onEvent(event: DashboardEvent) {
        when (event) {
            is DashboardEvent.Refresh -> loadDashboard()
            is DashboardEvent.ClearError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val response = apiService.getDashboardOverview()
                if (response.isSuccessful && response.body() != null) {
                    _uiState.update { it.copy(isLoading = false, data = response.body()) }
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to load dashboard metrics") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage ?: "Network error") }
            }
        }
    }
}
