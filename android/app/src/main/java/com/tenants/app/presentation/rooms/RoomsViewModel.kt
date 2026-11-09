package com.tenants.app.presentation.rooms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tenants.app.data.remote.TenantsApiService
import com.tenants.app.data.remote.model.RoomDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RoomsUiState(
    val isLoading: Boolean = false,
    val rooms: List<RoomDto> = emptyList(),
    val showAddDialog: Boolean = false,
    val roomToDelete: RoomDto? = null,
    val errorMessage: String? = null
)

sealed interface RoomsEvent {
    data object Refresh : RoomsEvent
    data object ShowAddDialog : RoomsEvent
    data object DismissAddDialog : RoomsEvent
    data class CreateRoom(val roomNo: String, val floor: Int, val basePrice: Double) : RoomsEvent
    data class RequestDeleteRoom(val room: RoomDto) : RoomsEvent
    data object ConfirmDeleteRoom : RoomsEvent
    data object DismissDeleteDialog : RoomsEvent
    data object ClearError : RoomsEvent
}

class RoomsViewModel(
    private val apiService: TenantsApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoomsUiState(isLoading = true))
    val uiState: StateFlow<RoomsUiState> = _uiState.asStateFlow()

    init {
        loadRooms()
    }

    fun onEvent(event: RoomsEvent) {
        when (event) {
            is RoomsEvent.Refresh -> loadRooms()
            is RoomsEvent.ShowAddDialog -> _uiState.update { it.copy(showAddDialog = true) }
            is RoomsEvent.DismissAddDialog -> _uiState.update { it.copy(showAddDialog = false) }
            is RoomsEvent.CreateRoom -> createRoom(event.roomNo, event.floor, event.basePrice)
            is RoomsEvent.RequestDeleteRoom -> _uiState.update { it.copy(roomToDelete = event.room) }
            is RoomsEvent.ConfirmDeleteRoom -> deleteRoom()
            is RoomsEvent.DismissDeleteDialog -> _uiState.update { it.copy(roomToDelete = null) }
            is RoomsEvent.ClearError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun loadRooms() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val response = apiService.getRooms()
                if (response.isSuccessful && response.body() != null) {
                    _uiState.update { it.copy(isLoading = false, rooms = response.body()!!.rooms) }
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to load rooms") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage ?: "Network error") }
            }
        }
    }

    private fun createRoom(roomNo: String, floor: Int, basePrice: Double) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showAddDialog = false) }
            try {
                val payload = mapOf(
                    "roomNo" to roomNo,
                    "floor" to floor,
                    "basePrice" to basePrice
                )
                val response = apiService.createRoom(payload)
                if (response.isSuccessful) {
                    loadRooms()
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to create room: ${response.message()}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage ?: "Creation failed") }
            }
        }
    }

    private fun deleteRoom() {
        val room = _uiState.value.roomToDelete ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, roomToDelete = null) }
            try {
                val response = apiService.deleteRoom(room.id)
                if (response.isSuccessful) {
                    loadRooms()
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Cannot delete: Room occupied by tenant") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage ?: "Delete failed") }
            }
        }
    }
}
