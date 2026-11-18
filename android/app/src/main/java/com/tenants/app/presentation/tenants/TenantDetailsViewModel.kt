package com.tenants.app.presentation.tenants

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tenants.app.data.local.DataSyncBus
import com.tenants.app.data.local.SyncTopic
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
    val isPullRefreshing: Boolean = false,
    val isPdfDownloading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

sealed interface TenantDetailsEvent {
    data class LoadDetails(val tenantId: String) : TenantDetailsEvent
    data object PullRefresh : TenantDetailsEvent
    data class DownloadAndSharePdf(val context: Context) : TenantDetailsEvent
    data object ClearMessages : TenantDetailsEvent
}

/**
 * ViewModel for viewing detailed tenant information, billing status, ledger breakdown, and downloading PDF statements.
 */
class TenantDetailsViewModel(
    private val apiService: TenantsApiService,
    private val pdfHelper: PdfDownloadHelper = PdfDownloadHelper()
) : ViewModel() {

    private val _uiState = MutableStateFlow(TenantDetailsUiState())
    val uiState: StateFlow<TenantDetailsUiState> = _uiState.asStateFlow()

    init {
        observeSyncEvents()
    }

    private fun observeSyncEvents() {
        viewModelScope.launch {
            DataSyncBus.syncEvents.collect { topic ->
                if (topic == SyncTopic.TENANTS || topic == SyncTopic.PAYMENTS || topic == SyncTopic.ALL) {
                    val currentId = _uiState.value.tenantId
                    if (currentId.isNotBlank()) {
                        loadTenant(currentId, isSilent = true)
                    }
                }
            }
        }
    }

    fun onEvent(event: TenantDetailsEvent) {
        when (event) {
            is TenantDetailsEvent.LoadDetails -> loadTenant(event.tenantId)
            is TenantDetailsEvent.PullRefresh -> {
                val currentId = _uiState.value.tenantId
                if (currentId.isNotBlank()) {
                    loadTenant(currentId, isPullRefresh = true)
                }
            }
            is TenantDetailsEvent.DownloadAndSharePdf -> downloadAndShareInvoice(event.context)
            is TenantDetailsEvent.ClearMessages -> _uiState.update { it.copy(errorMessage = null, successMessage = null) }
        }
    }

    private fun loadTenant(tenantId: String, isPullRefresh: Boolean = false, isSilent: Boolean = false) {
        _uiState.update {
            it.copy(
                tenantId = tenantId,
                isLoading = !isPullRefresh && !isSilent && it.tenant == null,
                isPullRefreshing = isPullRefresh,
                errorMessage = null
            )
        }
        viewModelScope.launch {
            try {
                // Fetch full tenant profile including 10 recent ledger transactions
                val response = apiService.getTenantById(tenantId)
                if (response.isSuccessful && response.body() != null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isPullRefreshing = false,
                            tenant = response.body()!!.tenant,
                            errorMessage = null
                        )
                    }
                } else {
                    // Fallback to getTenants if single endpoint returns 404 or fails
                    val fallbackRes = apiService.getTenants()
                    val found = fallbackRes.body()?.tenants?.find { it.id == tenantId }
                    if (found != null) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isPullRefreshing = false,
                                tenant = found,
                                errorMessage = null
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isPullRefreshing = false,
                                errorMessage = "Tenant record not found"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isPullRefreshing = false,
                        errorMessage = e.localizedMessage ?: "Network error"
                    )
                }
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
