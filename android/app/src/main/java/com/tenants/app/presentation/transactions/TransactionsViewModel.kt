package com.tenants.app.presentation.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tenants.app.data.local.DataSyncBus
import com.tenants.app.data.local.SyncTopic
import com.tenants.app.data.remote.TenantsApiService
import com.tenants.app.data.remote.model.PaymentRecordDto
import com.tenants.app.data.remote.model.parseApiError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI State for the Transactions / Payment History screen.
 *
 * @property isLoading True when initial load or foreground sync is taking place.
 * @property isPullRefreshing True only when the user executes a swipe-to-refresh gesture.
 * @property allTransactions Complete cached list of payments returned from the backend (up to 50).
 * @property filteredTransactions Filtered and searched slice of payments displayed in the UI.
 * @property searchQuery Current text query matching tenant name, room number, or notes.
 * @property selectedFilterMode Payment mode filter ("ALL", "UPI", "CASH", "BANK_TRANSFER").
 * @property errorMessage Error text if network request failed.
 */
data class TransactionsUiState(
    val isLoading: Boolean = false,
    val isPullRefreshing: Boolean = false,
    val allTransactions: List<PaymentRecordDto> = emptyList(),
    val filteredTransactions: List<PaymentRecordDto> = emptyList(),
    val searchQuery: String = "",
    val selectedFilterMode: String = "ALL",
    val errorMessage: String? = null
)

/**
 * User actions and events for the Transactions screen.
 */
sealed interface TransactionsEvent {
    data object Refresh : TransactionsEvent
    data object PullRefresh : TransactionsEvent
    data class SearchQueryChanged(val query: String) : TransactionsEvent
    data class FilterModeSelected(val mode: String) : TransactionsEvent
    data object ClearSearch : TransactionsEvent
    data object ClearError : TransactionsEvent
}

/**
 * ViewModel responsible for fetching, filtering, and caching payment history records.
 * Integrates reactively with [DataSyncBus] to automatically update when new payments are registered.
 */
class TransactionsViewModel(
    private val apiService: TenantsApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionsUiState(isLoading = true))
    val uiState: StateFlow<TransactionsUiState> = _uiState.asStateFlow()

    init {
        loadPayments(isSilent = false, isPullRefresh = false)
        observeSyncEvents()
    }

    private fun observeSyncEvents() {
        viewModelScope.launch {
            DataSyncBus.syncEvents.collect { topic ->
                if (topic == SyncTopic.DASHBOARD || topic == SyncTopic.TENANTS || topic == SyncTopic.ALL) {
                    loadPayments(isSilent = true, isPullRefresh = false)
                }
            }
        }
    }

    fun onEvent(event: TransactionsEvent) {
        when (event) {
            is TransactionsEvent.Refresh -> loadPayments(isSilent = false, isPullRefresh = false)
            is TransactionsEvent.PullRefresh -> loadPayments(isSilent = false, isPullRefresh = true)
            is TransactionsEvent.SearchQueryChanged -> {
                _uiState.update { current ->
                    val filtered = applyFilterAndSearch(current.allTransactions, event.query, current.selectedFilterMode)
                    current.copy(searchQuery = event.query, filteredTransactions = filtered)
                }
            }
            is TransactionsEvent.FilterModeSelected -> {
                _uiState.update { current ->
                    val filtered = applyFilterAndSearch(current.allTransactions, current.searchQuery, event.mode)
                    current.copy(selectedFilterMode = event.mode, filteredTransactions = filtered)
                }
            }
            is TransactionsEvent.ClearSearch -> {
                _uiState.update { current ->
                    val filtered = applyFilterAndSearch(current.allTransactions, "", current.selectedFilterMode)
                    current.copy(searchQuery = "", filteredTransactions = filtered)
                }
            }
            is TransactionsEvent.ClearError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    fun loadPayments(isSilent: Boolean = false, isPullRefresh: Boolean = false) {
        viewModelScope.launch {
            if (!isSilent) {
                if (isPullRefresh) {
                    _uiState.update { it.copy(isPullRefreshing = true, errorMessage = null) }
                } else if (_uiState.value.allTransactions.isNotEmpty()) {
                    _uiState.update { it.copy(errorMessage = null) }
                } else {
                    _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                }
            }
            try {
                val response = apiService.getPayments()
                if (response.isSuccessful && response.body() != null) {
                    val rawPayments = response.body()!!.payments
                    _uiState.update { current ->
                        val filtered = applyFilterAndSearch(rawPayments, current.searchQuery, current.selectedFilterMode)
                        current.copy(
                            isLoading = false,
                            isPullRefreshing = false,
                            allTransactions = rawPayments,
                            filteredTransactions = filtered,
                            errorMessage = null
                        )
                    }
                } else {
                    val errMsg = parseApiError(response)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isPullRefreshing = false,
                            errorMessage = errMsg
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isPullRefreshing = false,
                        errorMessage = e.localizedMessage ?: "Failed to load payments"
                    )
                }
            }
        }
    }

    private fun applyFilterAndSearch(
        list: List<PaymentRecordDto>,
        query: String,
        mode: String
    ): List<PaymentRecordDto> {
        val trimmedQuery = query.trim()
        return list.filter { item ->
            val matchesMode = if (mode.equals("ALL", ignoreCase = true)) true
            else item.paymentMode.equals(mode, ignoreCase = true)

            val matchesQuery = if (trimmedQuery.isEmpty()) true
            else {
                (item.tenant?.name?.contains(trimmedQuery, ignoreCase = true) == true) ||
                        (item.tenant?.room?.roomNo?.contains(trimmedQuery, ignoreCase = true) == true) ||
                        (item.notes?.contains(trimmedQuery, ignoreCase = true) == true) ||
                        (item.paymentMode.contains(trimmedQuery, ignoreCase = true))
            }

            matchesMode && matchesQuery
        }
    }
}
