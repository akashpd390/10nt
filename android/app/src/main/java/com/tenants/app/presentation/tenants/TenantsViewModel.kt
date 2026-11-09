package com.tenants.app.presentation.tenants

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tenants.app.data.remote.TenantsApiService
import com.tenants.app.data.remote.model.RoomDto
import com.tenants.app.data.remote.model.TenantDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TenantsUiState(
    val isLoading: Boolean = false,
    val tenants: List<TenantDto> = emptyList(),
    val availableRooms: List<RoomDto> = emptyList(),
    val showAddDialog: Boolean = false,
    val tenantToDelete: TenantDto? = null,
    val errorMessage: String? = null
)

sealed interface TenantsEvent {
    data object Refresh : TenantsEvent
    data object ShowAddDialog : TenantsEvent
    data object DismissAddDialog : TenantsEvent
    data class CreateTenant(
        val name: String,
        val phone: String,
        val membersCount: Int,
        val roomId: String,
        val rent: Double,
        val electricityRate: Double,
        val meterReading: Double,
        val openingBalance: Double
    ) : TenantsEvent
    data class RequestDeleteTenant(val tenant: TenantDto) : TenantsEvent
    data object ConfirmDeleteTenant : TenantsEvent
    data object DismissDeleteDialog : TenantsEvent
    data object ClearError : TenantsEvent
}

class TenantsViewModel(
    private val apiService: TenantsApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(TenantsUiState(isLoading = true))
    val uiState: StateFlow<TenantsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun onEvent(event: TenantsEvent) {
        when (event) {
            is TenantsEvent.Refresh -> loadData()
            is TenantsEvent.ShowAddDialog -> _uiState.update { it.copy(showAddDialog = true) }
            is TenantsEvent.DismissAddDialog -> _uiState.update { it.copy(showAddDialog = false) }
            is TenantsEvent.CreateTenant -> createTenant(event)
            is TenantsEvent.RequestDeleteTenant -> _uiState.update { it.copy(tenantToDelete = event.tenant) }
            is TenantsEvent.ConfirmDeleteTenant -> deleteTenant()
            is TenantsEvent.DismissDeleteDialog -> _uiState.update { it.copy(tenantToDelete = null) }
            is TenantsEvent.ClearError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val tenantsRes = apiService.getTenants()
                val roomsRes = apiService.getRooms()

                val tenantsList = tenantsRes.body()?.tenants ?: emptyList()
                val roomsList = roomsRes.body()?.rooms ?: emptyList()
                val vacantRooms = roomsList.filter { !it.isOccupied }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        tenants = tenantsList,
                        availableRooms = vacantRooms
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage ?: "Network error") }
            }
        }
    }

    private fun createTenant(event: TenantsEvent.CreateTenant) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showAddDialog = false) }
            try {
                val payload = mapOf(
                    "name" to event.name,
                    "phone" to event.phone,
                    "familyMembersCount" to event.membersCount,
                    "roomId" to event.roomId,
                    "allottedPrice" to event.rent,
                    "electricityRatePerUnit" to event.electricityRate,
                    "initialMeterReading" to event.meterReading,
                    "initialAdvanceOrBalance" to event.openingBalance
                )
                val response = apiService.createTenant(payload)
                if (response.isSuccessful) {
                    loadData()
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to register tenant") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage ?: "Creation failed") }
            }
        }
    }

    private fun deleteTenant() {
        val tenant = _uiState.value.tenantToDelete ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, tenantToDelete = null) }
            try {
                val response = apiService.deleteTenant(tenant.id)
                if (response.isSuccessful) {
                    loadData()
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Cannot delete: Tenant has pending balance dues") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage ?: "Delete failed") }
            }
        }
    }
}
