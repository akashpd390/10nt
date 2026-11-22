package com.tenants.app.presentation.billing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tenants.app.data.local.DataSyncBus
import com.tenants.app.data.local.SyncTopic
import com.tenants.app.data.remote.TenantsApiService
import com.tenants.app.data.remote.model.BatchMeterUpdateRequest
import com.tenants.app.data.remote.model.GenerateRentRollRequest
import com.tenants.app.data.remote.model.MeterUpdateItem
import com.tenants.app.data.remote.model.TenantBillingSummaryDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.ResponseBody

data class EditableTenantMeter(
    val summary: TenantBillingSummaryDto,
    val currentReadingInput: String,
    val unitRateInput: String
)

data class MeterBillingUiState(
    val isLoading: Boolean = false,
    val isGeneratingRent: Boolean = false,
    val isExporting: Boolean = false,
    val billingMonth: String = "",
    val tenantMeters: List<EditableTenantMeter> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val exportedCsvBytes: ByteArray? = null
)

sealed interface MeterBillingEvent {
    data object Refresh : MeterBillingEvent
    data object GenerateRentRoll : MeterBillingEvent
    data class ReadingChanged(val tenantId: String, val newReading: String) : MeterBillingEvent
    data class RateChanged(val tenantId: String, val newRate: String) : MeterBillingEvent
    data object SaveReadings : MeterBillingEvent
    data object ExportBillsCsv : MeterBillingEvent
    data object ClearMessages : MeterBillingEvent
}

class MeterBillingViewModel(
    private val apiService: TenantsApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(MeterBillingUiState(isLoading = true))
    val uiState: StateFlow<MeterBillingUiState> = _uiState.asStateFlow()

    init {
        loadSummary()
    }

    fun onEvent(event: MeterBillingEvent) {
        when (event) {
            is MeterBillingEvent.Refresh -> loadSummary()
            is MeterBillingEvent.GenerateRentRoll -> generateRentRoll()
            is MeterBillingEvent.ReadingChanged -> {
                _uiState.update { state ->
                    val updated = state.tenantMeters.map {
                        if (it.summary.tenantId == event.tenantId) it.copy(currentReadingInput = event.newReading)
                        else it
                    }
                    state.copy(tenantMeters = updated)
                }
            }
            is MeterBillingEvent.RateChanged -> {
                _uiState.update { state ->
                    val updated = state.tenantMeters.map {
                        if (it.summary.tenantId == event.tenantId) it.copy(unitRateInput = event.newRate)
                        else it
                    }
                    state.copy(tenantMeters = updated)
                }
            }
            is MeterBillingEvent.SaveReadings -> saveBatchReadings()
            is MeterBillingEvent.ExportBillsCsv -> exportBillsCsv()
            is MeterBillingEvent.ClearMessages -> _uiState.update { it.copy(errorMessage = null, successMessage = null, exportedCsvBytes = null) }
        }
    }

    private fun loadSummary() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val response = apiService.getMonthlyBillingSummary()
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val meters = body.tenants.map {
                        EditableTenantMeter(
                            summary = it,
                            currentReadingInput = it.currentMeterReading.toString(),
                            unitRateInput = it.electricityRatePerUnit.toString()
                        )
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            billingMonth = body.billingMonth,
                            tenantMeters = meters
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to load billing summary") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage ?: "Network error") }
            }
        }
    }

    private fun generateRentRoll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingRent = true, errorMessage = null) }
            try {
                val response = apiService.generateRentRoll(GenerateRentRollRequest())
                if (response.isSuccessful && response.body() != null) {
                    val res = response.body()!!
                    DataSyncBus.emitChange(SyncTopic.TENANTS)
                    DataSyncBus.emitChange(SyncTopic.DASHBOARD)
                    _uiState.update {
                        it.copy(
                            isGeneratingRent = false,
                            successMessage = "Rent roll generated: ${res.generatedCount} charged, ${res.skippedCount} skipped"
                        )
                    }
                    loadSummary()
                } else {
                    _uiState.update { it.copy(isGeneratingRent = false, errorMessage = "Failed to generate rent roll") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isGeneratingRent = false, errorMessage = e.localizedMessage ?: "Failed to generate rent") }
            }
        }
    }

    private fun saveBatchReadings() {
        val state = _uiState.value
        val items = mutableListOf<MeterUpdateItem>()

        for (item in state.tenantMeters) {
            val reading = item.currentReadingInput.toDoubleOrNull()
            if (reading == null || reading < item.summary.lastMeterReading) {
                _uiState.update {
                    it.copy(errorMessage = "Reading for ${item.summary.tenantName} cannot be less than previous (${item.summary.lastMeterReading})")
                }
                return
            }
            val rate = item.unitRateInput.toDoubleOrNull() ?: item.summary.electricityRatePerUnit
            items.add(
                MeterUpdateItem(
                    tenantId = item.summary.tenantId,
                    currentMeterReading = reading,
                    electricityRatePerUnit = rate
                )
            )
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val response = apiService.updateMeterReadings(
                    BatchMeterUpdateRequest(
                        billingMonth = state.billingMonth.ifBlank { null },
                        updates = items
                    )
                )
                if (response.isSuccessful) {
                    DataSyncBus.emitChange(SyncTopic.TENANTS)
                    DataSyncBus.emitChange(SyncTopic.DASHBOARD)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Meter readings and balances updated successfully!"
                        )
                    }
                    loadSummary()
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to update meter readings") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage ?: "Error saving readings") }
            }
        }
    }

    private fun exportBillsCsv() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, errorMessage = null) }
            try {
                val response = apiService.downloadMonthlyBillsCsv(month = _uiState.value.billingMonth)
                if (response.isSuccessful && response.body() != null) {
                    val bytes = response.body()!!.bytes()
                    _uiState.update {
                        it.copy(
                            isExporting = false,
                            exportedCsvBytes = bytes,
                            successMessage = "Rent roll CSV generated successfully!"
                        )
                    }
                } else {
                    _uiState.update { it.copy(isExporting = false, errorMessage = "Failed to export CSV") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isExporting = false, errorMessage = e.localizedMessage ?: "CSV export failed") }
            }
        }
    }
}
