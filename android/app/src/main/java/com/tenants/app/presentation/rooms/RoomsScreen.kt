package com.tenants.app.presentation.rooms

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tenants.app.R
import com.tenants.app.data.remote.model.RoomDto
import com.tenants.app.presentation.components.ConfirmationDialog
import com.tenants.app.presentation.theme.*
import org.koin.androidx.compose.koinViewModel

@Composable
fun RoomsScreen(viewModel: RoomsViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    RoomsContent(state = state, onEvent = viewModel::onEvent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomsContent(
    state: RoomsUiState,
    onEvent: (RoomsEvent) -> Unit
) {
    var newRoomNo by remember { mutableStateOf("") }
    var newFloor by remember { mutableStateOf("0") }
    var newBasePrice by remember { mutableStateOf("5000") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_rooms), fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface, titleContentColor = Primary)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onEvent(RoomsEvent.ShowAddDialog) },
                containerColor = SecondaryContainer,
                contentColor = Primary,
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_room))
            }
        },
        containerColor = Surface
    ) { paddingValues ->
        if (state.isLoading && state.rooms.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.rooms) { room ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MeetingRoom, contentDescription = null, tint = Primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Room ${room.roomNo}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Floor: ${room.floor} • Base: ₹${room.basePrice}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Outline
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SuggestionChip(
                                    onClick = {},
                                    label = {
                                        Text(
                                            text = if (room.isOccupied) stringResource(R.string.occupied_badge)
                                            else stringResource(R.string.vacant_badge),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = if (room.isOccupied) SurfaceContainerHighest else SecondaryContainer
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(onClick = { onEvent(RoomsEvent.RequestDeleteRoom(room)) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Error)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add Room Dialog
        if (state.showAddDialog) {
            AlertDialog(
                onDismissRequest = { onEvent(RoomsEvent.DismissAddDialog) },
                title = { Text(stringResource(R.string.add_room)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = newRoomNo,
                            onValueChange = { newRoomNo = it },
                            label = { Text(stringResource(R.string.room_no_hint)) },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = newFloor,
                            onValueChange = { newFloor = it },
                            label = { Text(stringResource(R.string.floor_hint)) },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = newBasePrice,
                            onValueChange = { newBasePrice = it },
                            label = { Text(stringResource(R.string.base_price_hint)) },
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val floorInt = newFloor.toIntOrNull() ?: 0
                            val priceDouble = newBasePrice.toDoubleOrNull() ?: 0.0
                            if (newRoomNo.isNotBlank()) {
                                onEvent(RoomsEvent.CreateRoom(newRoomNo, floorInt, priceDouble))
                                newRoomNo = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        shape = RoundedCornerShape(9999.dp)
                    ) {
                        Text(stringResource(R.string.confirm_button))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { onEvent(RoomsEvent.DismissAddDialog) }) {
                        Text(stringResource(R.string.cancel_button))
                    }
                }
            )
        }

        // Deletion Confirmation Dialog
        state.roomToDelete?.let { room ->
            ConfirmationDialog(
                title = stringResource(R.string.confirm_delete_title),
                message = "Are you sure you want to delete Room ${room.roomNo}? This cannot be undone.",
                onConfirm = { onEvent(RoomsEvent.ConfirmDeleteRoom) },
                onDismiss = { onEvent(RoomsEvent.DismissDeleteDialog) }
            )
        }
    }
}

@Preview(name = "Rooms Content Preview", showBackground = true)
@Composable
private fun RoomsContentPreview() {
    TenantsTheme {
        RoomsContent(
            state = RoomsUiState(
                isLoading = false,
                rooms = listOf(
                    RoomDto("1", "101", "1BHK with Kitchen", 1, "5500.00", true),
                    RoomDto("2", "102", "Single Room", 1, "4000.00", false)
                )
            ),
            onEvent = {}
        )
    }
}
