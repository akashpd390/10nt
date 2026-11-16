package com.tenants.app.presentation.rooms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tenants.app.data.local.DataSyncBus
import com.tenants.app.data.local.SyncTopic
import com.tenants.app.data.remote.TenantsApiService
import com.tenants.app.data.remote.model.CreateRoomRequest
import com.tenants.app.data.remote.model.RoomDto
import com.tenants.app.data.remote.model.UpdateRoomRequest
import com.tenants.app.data.remote.model.parseApiError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RoomsUiState(
    val isLoading: Boolean = false,
    val isPullRefreshing: Boolean = false,
    val rooms: List<RoomDto> = emptyList(),
    val showAddDialog: Boolean = false,
    val roomToEdit: RoomDto? = null,
    val roomToDelete: RoomDto? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

sealed interface RoomsEvent {
    data object Refresh : RoomsEvent
    data object PullRefresh : RoomsEvent
    data object ShowAddDialog : RoomsEvent
    data object DismissAddDialog : RoomsEvent
    data class CreateRoom(val roomNo: String, val floor: Int, val basePrice: Double, val description: String? = null) : RoomsEvent
    data class RequestEditRoom(val room: RoomDto) : RoomsEvent
    data class UpdateRoom(val id: String, val floor: Int, val basePrice: Double, val description: String? = null) : RoomsEvent
    data object DismissEditDialog : RoomsEvent
    data class RequestDeleteRoom(val room: RoomDto) : RoomsEvent
    data object ConfirmDeleteRoom : RoomsEvent
    data object DismissDeleteDialog : RoomsEvent
    data object ClearError : RoomsEvent
    data object ClearSuccess : RoomsEvent
}

class RoomsViewModel(
    private val apiService: TenantsApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoomsUiState(isLoading = true))
    val uiState: StateFlow<RoomsUiState> = _uiState.asStateFlow()

    init {
        loadRooms(isSilent = false)
        observeSyncEvents()
    }

    private fun observeSyncEvents() {
        viewModelScope.launch {
            DataSyncBus.syncEvents.collect { topic ->
                if (topic == SyncTopic.ROOMS || topic == SyncTopic.ALL) {
                    loadRooms(isSilent = true)
                }
            }
        }
    }

    fun onEvent(event: RoomsEvent) {
        when (event) {
            is RoomsEvent.Refresh -> loadRooms(isSilent = false, isPullRefresh = false)
            is RoomsEvent.PullRefresh -> loadRooms(isSilent = false, isPullRefresh = true)
            is RoomsEvent.ShowAddDialog -> _uiState.update { it.copy(showAddDialog = true) }
            is RoomsEvent.DismissAddDialog -> _uiState.update { it.copy(showAddDialog = false) }
            is RoomsEvent.CreateRoom -> createRoom(event.roomNo, event.floor, event.basePrice, event.description)
            is RoomsEvent.RequestEditRoom -> _uiState.update { it.copy(roomToEdit = event.room) }
            is RoomsEvent.UpdateRoom -> updateRoom(event.id, event.floor, event.basePrice, event.description)
            is RoomsEvent.DismissEditDialog -> _uiState.update { it.copy(roomToEdit = null) }
            is RoomsEvent.RequestDeleteRoom -> _uiState.update { it.copy(roomToDelete = event.room) }
            is RoomsEvent.ConfirmDeleteRoom -> deleteRoom()
            is RoomsEvent.DismissDeleteDialog -> _uiState.update { it.copy(roomToDelete = null) }
            is RoomsEvent.ClearError -> _uiState.update { it.copy(errorMessage = null) }
            is RoomsEvent.ClearSuccess -> _uiState.update { it.copy(successMessage = null) }
        }
    }

    fun loadRooms(isSilent: Boolean = false, isPullRefresh: Boolean = false) {
        viewModelScope.launch {
            if (!isSilent) {
                if (isPullRefresh) {
                    _uiState.update { it.copy(isPullRefreshing = true, errorMessage = null) }
                } else if (_uiState.value.rooms.isNotEmpty()) {
                    _uiState.update { it.copy(errorMessage = null) }
                } else {
                    _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                }
            }
            try {
                val response = apiService.getRooms()
                if (response.isSuccessful && response.body() != null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isPullRefreshing = false,
                            rooms = response.body()!!.rooms
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
                        errorMessage = e.localizedMessage ?: "Network error"
                    )
                }
            }
        }
    }

    private fun createRoom(roomNo: String, floor: Int, basePrice: Double, description: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showAddDialog = false) }
            try {
                val request = CreateRoomRequest(
                    roomNo = roomNo,
                    floor = floor,
                    basePrice = basePrice,
                    description = description?.takeIf { it.isNotBlank() }
                )
                val response = apiService.createRoom(request)
                if (response.isSuccessful) {
                    _uiState.update { it.copy(successMessage = "Room $roomNo created successfully") }
                    DataSyncBus.emitChange(SyncTopic.DASHBOARD)
                    DataSyncBus.emitChange(SyncTopic.TENANTS)
                    loadRooms(isSilent = true)
                } else {
                    val errMsg = parseApiError(response)
                    _uiState.update { it.copy(isLoading = false, errorMessage = errMsg) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage ?: "Creation failed") }
            }
        }
    }

    private fun updateRoom(id: String, floor: Int, basePrice: Double, description: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, roomToEdit = null) }
            try {
                val request = UpdateRoomRequest(
                    floor = floor,
                    basePrice = basePrice,
                    description = description?.takeIf { it.isNotBlank() }
                )
                val response = apiService.updateRoom(id, request)
                if (response.isSuccessful) {
                    _uiState.update { it.copy(successMessage = "Room updated successfully") }
                    DataSyncBus.emitChange(SyncTopic.DASHBOARD)
                    DataSyncBus.emitChange(SyncTopic.TENANTS)
                    loadRooms(isSilent = true)
                } else {
                    val errMsg = parseApiError(response)
                    _uiState.update { it.copy(isLoading = false, errorMessage = errMsg) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage ?: "Update failed") }
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
                    _uiState.update { it.copy(successMessage = "Room ${room.roomNo} deleted successfully") }
                    DataSyncBus.emitChange(SyncTopic.DASHBOARD)
                    DataSyncBus.emitChange(SyncTopic.TENANTS)
                    loadRooms(isSilent = true)
                } else {
                    val errMsg = parseApiError(response)
                    _uiState.update { it.copy(isLoading = false, errorMessage = errMsg) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage ?: "Delete failed") }
            }
        }
    }
}
