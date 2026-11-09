package com.tenants.app.presentation.payments

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tenants.app.R
import com.tenants.app.data.remote.model.PaymentSummaryDto
import com.tenants.app.data.remote.model.RoomDto
import com.tenants.app.data.remote.model.TenantDto
import com.tenants.app.presentation.theme.*
import com.tenants.app.utils.SmsSender
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * Top-level Stateful Route for Manual Payment Entry.
 */
@Composable
fun PaymentScreen(
    viewModel: PaymentViewModel = koinViewModel(),
    smsSender: SmsSender = koinInject()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    PaymentContent(
        state = state,
        onEvent = viewModel::onEvent,
        onSendSms = { summary ->
            smsSender.sendSms(context, summary.tenantPhone, summary.smsTemplate)
            viewModel.onEvent(PaymentEvent.DismissSuccessDialog)
        }
    )
}

/**
 * Stateless Payment Composable implementing Unidirectional Data Flow (UDF).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentContent(
    state: PaymentUiState,
    onEvent: (PaymentEvent) -> Unit,
    onSendSms: (PaymentSummaryDto) -> Unit = {}
) {
    var isDropdownExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.record_payment_title), fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Surface,
                    titleContentColor = Primary
                )
            )
        },
        containerColor = Surface
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tenant Selector Dropdown
            Text(
                text = stringResource(R.string.select_tenant),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Primary
            )

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isDropdownExpanded = true },
                    shape = RoundedCornerShape(12.dp)
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
                            Text(
                                text = state.selectedTenant?.let { "${it.name} (Room ${it.room?.roomNo ?: "N/A"})" }
                                    ?: "Tap to choose tenant",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Outline)
                    }
                }

                DropdownMenu(
                    expanded = isDropdownExpanded,
                    onDismissRequest = { isDropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    // Search Filter Field inside dropdown
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = { onEvent(PaymentEvent.SearchQueryChanged(it)) },
                        placeholder = { Text("Search by name or room...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        singleLine = true
                    )

                    state.filteredTenants.forEach { tenant ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(tenant.name, fontWeight = FontWeight.Bold)
                                    Text(
                                        "Room: ${tenant.room?.roomNo ?: "N/A"} • Rent: ₹${tenant.allottedPrice}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Outline
                                    )
                                }
                            },
                            onClick = {
                                onEvent(PaymentEvent.TenantSelected(tenant))
                                isDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Selected Tenant Quick Stats
            state.selectedTenant?.let { tenant ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Current Balance", style = MaterialTheme.typography.labelSmall, color = Outline)
                            val bal = tenant.currentBalance.toDoubleOrNull() ?: 0.0
                            Text(
                                text = if (bal < 0) "Due: ₹${"%.2f".format(kotlin.math.abs(bal))}"
                                else if (bal > 0) "Advance: ₹${"%.2f".format(bal)}"
                                else "Settled (₹0.00)",
                                color = if (bal < 0) Error else Primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Last Meter Reading", style = MaterialTheme.typography.labelSmall, color = Outline)
                            Text("${tenant.lastMeterReading} units", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Amount Paid Input
            OutlinedTextField(
                value = state.amountPaid,
                onValueChange = { onEvent(PaymentEvent.AmountPaidChanged(it)) },
                label = { Text(stringResource(R.string.amount_paid_hint)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Current Electricity Meter Reading Input (Optional)
            OutlinedTextField(
                value = state.currentElectricityReading,
                onValueChange = { onEvent(PaymentEvent.ElectricityReadingChanged(it)) },
                label = { Text("Current Electricity Reading (Units)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Payment Mode Selector (Cash / UPI / Bank Transfer)
            Text(
                text = "Payment Mode",
                style = MaterialTheme.typography.labelMedium,
                color = Primary,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("UPI", "CASH", "BANK_TRANSFER").forEach { mode ->
                    FilterChip(
                        selected = state.paymentMode == mode,
                        onClick = { onEvent(PaymentEvent.PaymentModeChanged(mode)) },
                        label = { Text(mode) },
                        shape = RoundedCornerShape(9999.dp)
                    )
                }
            }

            // Notes / Remarks
            OutlinedTextField(
                value = state.notes,
                onValueChange = { onEvent(PaymentEvent.NotesChanged(it)) },
                label = { Text(stringResource(R.string.notes_hint)) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Submit Button
            Button(
                onClick = { onEvent(PaymentEvent.SubmitPayment) },
                enabled = !state.isLoading && state.selectedTenant != null && state.amountPaid.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(9999.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(color = OnPrimary, modifier = Modifier.size(20.dp))
                } else {
                    Text(stringResource(R.string.save_payment_button), fontWeight = FontWeight.Bold)
                }
            }
        }

        // SMS Receipt Prompt Modal Dialog
        state.paymentSuccessSummary?.let { summary ->
            AlertDialog(
                onDismissRequest = { onEvent(PaymentEvent.DismissSuccessDialog) },
                icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Primary) },
                title = { Text(stringResource(R.string.sms_receipt_prompt)) },
                text = {
                    Column {
                        Text("Payment of ₹${summary.amountPaid} recorded successfully!")
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = summary.smsTemplate,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { onSendSms(summary) },
                        shape = RoundedCornerShape(9999.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Text(stringResource(R.string.send_sms_button))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { onEvent(PaymentEvent.DismissSuccessDialog) }) {
                        Text(stringResource(R.string.skip_button))
                    }
                }
            )
        }
    }
}

@Preview(name = "Payment Screen Preview", showBackground = true)
@Composable
private fun PaymentContentPreview() {
    TenantsTheme {
        PaymentContent(
            state = PaymentUiState(
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
                        currentBalance = "-5500.00",
                        isActive = true,
                        room = RoomDto("1", "101", "1BHK", 1, "5500.00", true)
                    )
                ),
                selectedTenant = TenantDto(
                    id = "1",
                    name = "Rahul Sharma",
                    phone = "9876543210",
                    familyMembersCount = 2,
                    roomId = "1",
                    allottedPrice = "5500.00",
                    electricityRatePerUnit = "8.00",
                    lastMeterReading = "140.00",
                    currentBalance = "-5500.00",
                    isActive = true,
                    room = RoomDto("1", "101", "1BHK", 1, "5500.00", true)
                ),
                amountPaid = "5500",
                paymentMode = "UPI",
                currentElectricityReading = "190"
            ),
            onEvent = {}
        )
    }
}
