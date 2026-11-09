package com.tenants.app.presentation.payments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tenants.app.data.remote.TenantsApiService
import com.tenants.app.data.remote.model.CreatePaymentRequest
import com.tenants.app.data.remote.model.PaymentSummaryDto
import com.tenants.app.data.remote.model.TenantDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PaymentUiState(
    val isLoading: Boolean = false,
    val tenants: List<TenantDto> = emptyList(),
    val filteredTenants: List<TenantDto> = emptyList(),
    val searchQuery: String = "",
    val selectedTenant: TenantDto? = null,
    val amountPaid: String = "",
    val paymentMode: String = "UPI",
    val currentElectricityReading: String = "",
    val notes: String = "",
    val paymentSuccessSummary: PaymentSummaryDto? = null,
    val errorMessage: String? = null
)

sealed interface PaymentEvent {
    data class SearchQueryChanged(val query: String) : PaymentEvent
    data class TenantSelected(val tenant: TenantDto) : PaymentEvent
    data class AmountPaidChanged(val amount: String) : PaymentEvent
    data class PaymentModeChanged(val mode: String) : PaymentEvent
    data class ElectricityReadingChanged(val reading: String) : PaymentEvent
    data class NotesChanged(val notes: String) : PaymentEvent
    data object SubmitPayment : PaymentEvent
    data object DismissSuccessDialog : PaymentEvent
    data object ClearError : PaymentEvent
}

/**
 * ViewModel for manual payment entry, tenant dropdown filtering, and SMS receipt trigger.
 */
class PaymentViewModel(
    private val apiService: TenantsApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaymentUiState(isLoading = true))
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    init {
        loadTenants()
    }

    fun onEvent(event: PaymentEvent) {
        when (event) {
            is PaymentEvent.SearchQueryChanged -> {
                _uiState.update { state ->
                    val filtered = if (event.query.isBlank()) {
                        state.tenants
                    } else {
                        state.tenants.filter {
                            it.name.contains(event.query, ignoreCase = true) ||
                            (it.room?.roomNo?.contains(event.query, ignoreCase = true) == true)
                        }
                    }
                    state.copy(searchQuery = event.query, filteredTenants = filtered)
                }
            }
            is PaymentEvent.TenantSelected -> {
                _uiState.update {
                    it.copy(
                        selectedTenant = event.tenant,
                        amountPaid = event.tenant.allottedPrice, // Default suggestion to monthly rent
                        currentElectricityReading = event.tenant.lastMeterReading
                    )
                }
            }
            is PaymentEvent.AmountPaidChanged -> _uiState.update { it.copy(amountPaid = event.amount) }
            is PaymentEvent.PaymentModeChanged -> _uiState.update { it.copy(paymentMode = event.mode) }
            is PaymentEvent.ElectricityReadingChanged -> _uiState.update { it.copy(currentElectricityReading = event.reading) }
            is PaymentEvent.NotesChanged -> _uiState.update { it.copy(notes = event.notes) }
            is PaymentEvent.SubmitPayment -> submitPayment()
            is PaymentEvent.DismissSuccessDialog -> _uiState.update { it.copy(paymentSuccessSummary = null) }
            is PaymentEvent.ClearError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun loadTenants() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val response = apiService.getTenants()
                if (response.isSuccessful && response.body() != null) {
                    val list = response.body()!!.tenants
                    _uiState.update {
                        it.copy(isLoading = false, tenants = list, filteredTenants = list)
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to load tenants list") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage ?: "Network error") }
            }
        }
    }

    private fun submitPayment() {
        val state = _uiState.value
        val tenant = state.selectedTenant ?: run {
            _uiState.update { it.copy(errorMessage = "Please select a tenant") }
            return
        }

        val amount = state.amountPaid.toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid amount") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val reading = state.currentElectricityReading.toDoubleOrNull()
                val request = CreatePaymentRequest(
                    tenantId = tenant.id,
                    amountPaid = amount,
                    paymentMode = state.paymentMode,
                    currentElectricityReading = reading,
                    notes = state.notes.ifBlank { null }
                )
                val response = apiService.createPayment(request)
                if (response.isSuccessful && response.body() != null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            paymentSuccessSummary = response.body()!!.summary,
                            amountPaid = "",
                            notes = ""
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to record payment") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage ?: "Payment failed") }
            }
        }
    }
}
