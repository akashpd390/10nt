package com.tenants.app.presentation.tenants

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
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
import com.tenants.app.data.remote.model.TenantDto
import com.tenants.app.presentation.components.ConfirmationDialog
import com.tenants.app.presentation.theme.*
import org.koin.androidx.compose.koinViewModel

@Composable
fun TenantsScreen(viewModel: TenantsViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TenantsContent(state = state, onEvent = viewModel::onEvent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TenantsContent(
    state: TenantsUiState,
    onEvent: (TenantsEvent) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var membersCount by remember { mutableStateOf("1") }
    var selectedRoomId by remember { mutableStateOf("") }
    var rent by remember { mutableStateOf("") }
    var electricityRate by remember { mutableStateOf("8.00") }
    var meterReading by remember { mutableStateOf("0.00") }
    var openingBalance by remember { mutableStateOf("0.00") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_tenants), fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface, titleContentColor = Primary)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onEvent(TenantsEvent.ShowAddDialog) },
                containerColor = SecondaryContainer,
                contentColor = Primary,
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_tenant))
            }
        },
        containerColor = Surface
    ) { paddingValues ->
        if (state.isLoading && state.tenants.isEmpty()) {
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
                items(state.tenants) { tenant ->
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
                                Icon(Icons.Default.Person, contentDescription = null, tint = Primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = tenant.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Room: ${tenant.room?.roomNo ?: "None"} • ${tenant.phone}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Outline
                                    )
                                    val bal = tenant.currentBalance.toDoubleOrNull() ?: 0.0
                                    Text(
                                        text = if (bal < 0) "Due: ₹${"%.2f".format(kotlin.math.abs(bal))}"
                                        else if (bal > 0) "Advance: ₹${"%.2f".format(bal)}"
                                        else "Settled",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (bal < 0) Error else Primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            IconButton(onClick = { onEvent(TenantsEvent.RequestDeleteTenant(tenant)) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Error)
                            }
                        }
                    }
                }
            }
        }

        // Add Tenant Dialog
        if (state.showAddDialog) {
            AlertDialog(
                onDismissRequest = { onEvent(TenantsEvent.DismissAddDialog) },
                title = { Text(stringResource(R.string.add_tenant)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text(stringResource(R.string.tenant_name_hint)) },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text(stringResource(R.string.phone_hint)) },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = rent,
                            onValueChange = { rent = it },
                            label = { Text(stringResource(R.string.allotted_rent_hint)) },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = meterReading,
                            onValueChange = { meterReading = it },
                            label = { Text(stringResource(R.string.meter_reading_hint)) },
                            singleLine = true
                        )

                        // Room selection dropdown
                        Text("Select Room", style = MaterialTheme.typography.labelMedium, color = Primary)
                        state.availableRooms.forEach { room ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedRoomId = room.id
                                        rent = room.basePrice
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = selectedRoomId == room.id,
                                    onClick = {
                                        selectedRoomId = room.id
                                        rent = room.basePrice
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Room ${room.roomNo} (Base: ₹${room.basePrice})")
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (name.isNotBlank() && phone.isNotBlank() && selectedRoomId.isNotBlank()) {
                                onEvent(
                                    TenantsEvent.CreateTenant(
                                        name = name,
                                        phone = phone,
                                        membersCount = membersCount.toIntOrNull() ?: 1,
                                        roomId = selectedRoomId,
                                        rent = rent.toDoubleOrNull() ?: 0.0,
                                        electricityRate = electricityRate.toDoubleOrNull() ?: 8.0,
                                        meterReading = meterReading.toDoubleOrNull() ?: 0.0,
                                        openingBalance = openingBalance.toDoubleOrNull() ?: 0.0
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        shape = RoundedCornerShape(9999.dp)
                    ) {
                        Text(stringResource(R.string.confirm_button))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { onEvent(TenantsEvent.DismissAddDialog) }) {
                        Text(stringResource(R.string.cancel_button))
                    }
                }
            )
        }

        // Deletion Confirmation Dialog
        state.tenantToDelete?.let { tenant ->
            ConfirmationDialog(
                title = stringResource(R.string.confirm_delete_title),
                message = "Are you sure you want to remove tenant ${tenant.name}? Room ${tenant.room?.roomNo ?: ""} will be released.",
                onConfirm = { onEvent(TenantsEvent.ConfirmDeleteTenant) },
                onDismiss = { onEvent(TenantsEvent.DismissDeleteDialog) }
            )
        }
    }
}

@Preview(name = "Tenants Content Preview", showBackground = true)
@Composable
private fun TenantsContentPreview() {
    TenantsTheme {
        TenantsContent(
            state = TenantsUiState(
                isLoading = false,
                tenants = listOf(
                    TenantDto(
                        id = "1",
                        name = "Rahul Sharma",
                        phone = "9876543210",
                        familyMembersCount = 2,
                        roomId = "1",
                        allottedPrice = "5500.00",
                        electricityRatePerUnit = "8.00",
                        lastMeterReading = "140.00",
                        currentBalance = "-2500.00",
                        isActive = true,
                        room = RoomDto("1", "101", "1BHK", 1, "5500.00", true)
                    )
                ),
                availableRooms = listOf(RoomDto("2", "102", "Single Room", 1, "4000.00", false))
            ),
            onEvent = {}
        )
    }
}
