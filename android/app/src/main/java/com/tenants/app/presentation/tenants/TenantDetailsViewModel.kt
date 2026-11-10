package com.tenants.app.presentation.tenants

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tenants.app.data.remote.TenantsApiService
import com.tenants.app.data.remote.model.TenantDto
import com.tenants.app.utils.PdfDownloadHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TenantDetailsUiState(
    val tenantId: String = "",
    val tenant: TenantDto? = null,
    val isLoading: Boolean = false,
    val isPdfDownloading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

sealed interface TenantDetailsEvent {
    data class LoadDetails(val tenantId: String) : TenantDetailsEvent
    data class DownloadAndSharePdf(val context: Context) : TenantDetailsEvent
    data object ClearMessages : TenantDetailsEvent
}

/**
 * ViewModel for viewing detailed tenant information, billing status, and downloading PDF statements.
 */
class TenantDetailsViewModel(
    private val apiService: TenantsApiService,
    private val pdfHelper: PdfDownloadHelper = PdfDownloadHelper()
) : ViewModel() {

    private val _uiState = MutableStateFlow(TenantDetailsUiState())
    val uiState: StateFlow<TenantDetailsUiState> = _uiState.asStateFlow()

    fun onEvent(event: TenantDetailsEvent) {
        when (event) {
            is TenantDetailsEvent.LoadDetails -> loadTenant(event.tenantId)
            is TenantDetailsEvent.DownloadAndSharePdf -> downloadAndShareInvoice(event.context)
            is TenantDetailsEvent.ClearMessages -> _uiState.update { it.copy(errorMessage = null, successMessage = null) }
        }
    }

    private fun loadTenant(tenantId: String) {
        _uiState.update { it.copy(tenantId = tenantId, isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val response = apiService.getTenants()
                if (response.isSuccessful && response.body() != null) {
                    val found = response.body()!!.tenants.find { it.id == tenantId }
                    if (found != null) {
                        _uiState.update { it.copy(isLoading = false, tenant = found) }
                    } else {
                        _uiState.update { it.copy(isLoading = false, errorMessage = "Tenant record not found") }
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to load tenant details") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage ?: "Network error") }
            }
        }
    }

    private fun downloadAndShareInvoice(context: Context) {
        val tenant = _uiState.value.tenant ?: return
        _uiState.update { it.copy(isPdfDownloading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val response = apiService.downloadTenantPdf(tenant.id)
                if (response.isSuccessful && response.body() != null) {
                    val bytes = response.body()!!.bytes()
                    val fileName = "Invoice_${tenant.name.replace("\\s+".toRegex(), "_")}.pdf"
                    pdfHelper.saveAndSharePdf(context, bytes, fileName)
                    _uiState.update { it.copy(isPdfDownloading = false, successMessage = "PDF Invoice ready to view or share") }
                } else {
                    _uiState.update { it.copy(isPdfDownloading = false, errorMessage = "Failed to generate PDF statement") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isPdfDownloading = false, errorMessage = e.localizedMessage ?: "PDF download failed") }
            }
        }
    }
}
