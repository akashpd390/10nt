package com.tenants.app.presentation.rooms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tenants.app.R
import com.tenants.app.data.remote.model.RoomDto
import com.tenants.app.presentation.components.ConfirmationDialog
import com.tenants.app.presentation.theme.*
import org.koin.androidx.compose.koinViewModel

/**
 * Stateful entry point for the Rooms management screen.
 */
@Composable
fun RoomsScreen(viewModel: RoomsViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    RoomsContent(state = state, onEvent = viewModel::onEvent)
}

/**
 * Pure declarative presentation for Rooms screen matching Emerald & Parchment design guidelines.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomsContent(
    state: RoomsUiState,
    onEvent: (RoomsEvent) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Short
            )
            onEvent(RoomsEvent.ClearError)
        }
    }

    LaunchedEffect(state.successMessage) {
        state.successMessage?.let { success ->
            snackbarHostState.showSnackbar(
                message = success,
                duration = SnackbarDuration.Short
            )
            onEvent(RoomsEvent.ClearSuccess)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.nav_rooms),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Surface,
                    titleContentColor = Primary
                )
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = SurfaceContainerHighest,
                    contentColor = OnSurface,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onEvent(RoomsEvent.ShowAddDialog) },
                containerColor = SecondaryContainer,
                contentColor = Primary,
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_room)
                )
            }
        },
        containerColor = Surface
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { onEvent(RoomsEvent.Refresh) },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state.isLoading && state.rooms.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Primary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.rooms, key = { it.id }) { room ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceContainer)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(
                                                color = if (room.isOccupied) SurfaceContainerHighest else SecondaryContainer,
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MeetingRoom,
                                            contentDescription = null,
                                            tint = Primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Room ${room.roomNo}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = OnSurface
                                        )
                                        Text(
                                            text = "Floor ${room.floor} • Base: ₹${room.basePrice}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Outline
                                        )
                                        room.description?.takeIf { it.isNotBlank() }?.let { desc ->
                                            Text(
                                                text = desc,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = OutlineVariant,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    SuggestionChip(
                                        onClick = {},
                                        label = {
                                            Text(
                                                text = if (room.isOccupied) stringResource(R.string.occupied_badge)
                                                else stringResource(R.string.vacant_badge),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = if (room.isOccupied) SurfaceContainerHighest else SecondaryContainer,
                                            labelColor = if (room.isOccupied) Outline else Primary
                                        ),
                                        border = null,
                                        shape = RoundedCornerShape(9999.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(onClick = { onEvent(RoomsEvent.RequestEditRoom(room)) }) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = stringResource(R.string.edit_room),
                                            tint = Primary
                                        )
                                    }
                                    IconButton(onClick = { onEvent(RoomsEvent.RequestDeleteRoom(room)) }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = stringResource(R.string.delete_button),
                                            tint = Error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add Room Dialog (Emerald & Parchment styled)
        if (state.showAddDialog) {
            AddRoomDialog(
                onDismiss = { onEvent(RoomsEvent.DismissAddDialog) },
                onConfirm = { roomNo, floor, basePrice, description ->
                    onEvent(RoomsEvent.CreateRoom(roomNo, floor, basePrice, description))
                }
            )
        }

        // Edit Room Dialog (Emerald & Parchment styled)
        state.roomToEdit?.let { room ->
            EditRoomDialog(
                room = room,
                onDismiss = { onEvent(RoomsEvent.DismissEditDialog) },
                onConfirm = { floor, basePrice, description ->
                    onEvent(RoomsEvent.UpdateRoom(room.id, floor, basePrice, description))
                }
            )
        }

        // Deletion Confirmation Dialog
        state.roomToDelete?.let { room ->
            ConfirmationDialog(
                title = stringResource(R.string.confirm_delete_title),
                message = stringResource(R.string.delete_room_warning, room.roomNo),
                confirmText = stringResource(R.string.delete_button),
                onConfirm = { onEvent(RoomsEvent.ConfirmDeleteRoom) },
                onDismiss = { onEvent(RoomsEvent.DismissDeleteDialog) }
            )
        }
    }
}

/**
 * Polished Add Room Dialog matching Emerald & Parchment showcase specifications.
 */
@Composable
private fun AddRoomDialog(
    onDismiss: () -> Unit,
    onConfirm: (roomNo: String, floor: Int, basePrice: Double, description: String?) -> Unit
) {
    var roomNo by remember { mutableStateOf("") }
    var floor by remember { mutableStateOf("0") }
    var basePrice by remember { mutableStateOf("5000") }
    var description by remember { mutableStateOf("") }
    var hasAttemptedSubmit by remember { mutableStateOf(false) }

    val isRoomNoValid = roomNo.trim().isNotBlank()
    val isPriceValid = (basePrice.toDoubleOrNull() ?: -1.0) >= 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = SurfaceContainerHigh,
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color = Primary.copy(alpha = 0.12f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MeetingRoom,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        title = {
            Text(
                text = stringResource(R.string.add_room),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Primary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = roomNo,
                    onValueChange = { roomNo = it },
                    label = { Text(stringResource(R.string.room_no_hint)) },
                    leadingIcon = { Icon(Icons.Default.MeetingRoom, contentDescription = null, tint = Primary) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    isError = hasAttemptedSubmit && !isRoomNoValid,
                    supportingText = if (hasAttemptedSubmit && !isRoomNoValid) {
                        { Text(stringResource(R.string.room_no_required), color = Error) }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = floor,
                    onValueChange = { floor = it },
                    label = { Text(stringResource(R.string.floor_hint)) },
                    leadingIcon = { Icon(Icons.Default.Layers, contentDescription = null, tint = Primary) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = basePrice,
                    onValueChange = { basePrice = it },
                    label = { Text(stringResource(R.string.base_price_hint)) },
                    leadingIcon = { Icon(Icons.Default.CurrencyRupee, contentDescription = null, tint = Primary) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    isError = hasAttemptedSubmit && !isPriceValid,
                    supportingText = if (hasAttemptedSubmit && !isPriceValid) {
                        { Text(stringResource(R.string.base_price_invalid), color = Error) }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.room_description_hint)) },
                    leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, tint = Primary) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    hasAttemptedSubmit = true
                    if (isRoomNoValid && isPriceValid) {
                        val floorInt = floor.toIntOrNull() ?: 0
                        val priceDouble = basePrice.toDoubleOrNull() ?: 0.0
                        onConfirm(roomNo.trim(), floorInt, priceDouble, description.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    contentColor = OnPrimary
                ),
                shape = RoundedCornerShape(9999.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = stringResource(R.string.confirm_button),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            FilledTonalButton(
                onClick = onDismiss,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = SecondaryContainer,
                    contentColor = Primary
                ),
                shape = RoundedCornerShape(9999.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = stringResource(R.string.cancel_button),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}

/**
 * Polished Edit Room Dialog matching Emerald & Parchment showcase specifications.
 */
@Composable
private fun EditRoomDialog(
    room: RoomDto,
    onDismiss: () -> Unit,
    onConfirm: (floor: Int, basePrice: Double, description: String?) -> Unit
) {
    var floor by remember(room) { mutableStateOf(room.floor.toString()) }
    var basePrice by remember(room) { mutableStateOf(room.basePrice) }
    var description by remember(room) { mutableStateOf(room.description ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = SurfaceContainerHigh,
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color = Primary.copy(alpha = 0.12f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        title = {
            Text(
                text = stringResource(R.string.edit_room_title, room.roomNo),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Primary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = floor,
                    onValueChange = { floor = it },
                    label = { Text(stringResource(R.string.floor_hint)) },
                    leadingIcon = { Icon(Icons.Default.Layers, contentDescription = null, tint = Primary) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = basePrice,
                    onValueChange = { basePrice = it },
                    label = { Text(stringResource(R.string.base_price_hint)) },
                    leadingIcon = { Icon(Icons.Default.CurrencyRupee, contentDescription = null, tint = Primary) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.room_description_hint)) },
                    leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, tint = Primary) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val floorInt = floor.toIntOrNull() ?: room.floor
                    val priceDouble = basePrice.toDoubleOrNull() ?: room.basePrice.toDoubleOrNull() ?: 0.0
                    onConfirm(floorInt, priceDouble, description.trim())
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    contentColor = OnPrimary
                ),
                shape = RoundedCornerShape(9999.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = stringResource(R.string.confirm_button),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            FilledTonalButton(
                onClick = onDismiss,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = SecondaryContainer,
                    contentColor = Primary
                ),
                shape = RoundedCornerShape(9999.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = stringResource(R.string.cancel_button),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
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
