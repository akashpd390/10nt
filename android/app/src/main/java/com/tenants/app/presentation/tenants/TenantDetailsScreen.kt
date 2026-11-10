package com.tenants.app.presentation.tenants

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tenants.app.R
import com.tenants.app.data.remote.model.RoomDto
import com.tenants.app.data.remote.model.TenantDto
import com.tenants.app.presentation.theme.*
import org.koin.androidx.compose.koinViewModel

/**
 * Top-level Stateful Route for Tenant Details and Invoice Export.
 */
@Composable
fun TenantDetailsScreen(
    tenantId: String,
    onNavigateBack: () -> Unit,
    viewModel: TenantDetailsViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(tenantId) {
        viewModel.onEvent(TenantDetailsEvent.LoadDetails(tenantId))
    }

    LaunchedEffect(state.successMessage) {
        state.successMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.onEvent(TenantDetailsEvent.ClearMessages)
        }
    }

    TenantDetailsContent(
        state = state,
        onNavigateBack = onNavigateBack,
        onDownloadPdf = { viewModel.onEvent(TenantDetailsEvent.DownloadAndSharePdf(context)) }
    )
}

/**
 * Pure declarative, stateless Tenant Details Composable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TenantDetailsContent(
    state: TenantDetailsUiState,
    onNavigateBack: () -> Unit,
    onDownloadPdf: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.tenant?.name ?: "Tenant Profile", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Surface,
                    titleContentColor = Primary
                )
            )
        },
        containerColor = Surface
    ) { paddingValues ->
        if (state.isLoading && state.tenant == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Primary)
            }
        } else {
            val tenant = state.tenant
            if (tenant != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Profile Header Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceContainer)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(SecondaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Primary, modifier = Modifier.size(40.dp))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(tenant.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(
                                text = "Room ${tenant.room?.roomNo ?: "Unassigned"} • Floor ${tenant.room?.floor ?: 0}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Outline
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // Signed Balance Badge
                            val bal = tenant.currentBalance.toDoubleOrNull() ?: 0.0
                            val balanceBadgeText = when {
                                bal < 0 -> "Overdue / Arrears: ₹${"%.2f".format(kotlin.math.abs(bal))}"
                                bal > 0 -> "Advance Credit: ₹${"%.2f".format(bal)}"
                                else -> "Account Settled (₹0.00)"
                            }
                            SuggestionChip(
                                onClick = {},
                                label = { Text(balanceBadgeText, fontWeight = FontWeight.Bold) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = if (bal < 0) ErrorContainer else SurfaceContainerHigh,
                                    labelColor = if (bal < 0) OnErrorContainer else Primary
                                )
                            )
                        }
                    }

                    // Key Tenancy Information
                    Text("Tenancy Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Primary)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            InfoRow(icon = Icons.Default.Phone, label = "Phone Number", value = tenant.phone)
                            InfoRow(icon = Icons.Default.Groups, label = "Family Members", value = "${tenant.familyMembersCount} Persons")
                            InfoRow(icon = Icons.Default.AttachMoney, label = "Agreed Monthly Rent", value = "₹${tenant.allottedPrice}")
                            InfoRow(icon = Icons.Default.Bolt, label = "Electricity Rate", value = "₹${tenant.electricityRatePerUnit} / unit")
                            InfoRow(icon = Icons.Default.Speed, label = "Last Meter Reading", value = "${tenant.lastMeterReading} units")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Download / Share PDF Invoice Action
                    Button(
                        onClick = onDownloadPdf,
                        enabled = !state.isPdfDownloading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(9999.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        if (state.isPdfDownloading) {
                            CircularProgressIndicator(color = OnPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Generating Statement...", color = OnPrimary)
                        } else {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = OnPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Download / Share PDF Invoice", fontWeight = FontWeight.Bold, color = OnPrimary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, color = Outline)
        }
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Preview(name = "Tenant Details Preview", showBackground = true)
@Composable
private fun TenantDetailsContentPreview() {
    TenantsTheme {
        TenantDetailsContent(
            state = TenantDetailsUiState(
                tenant = TenantDto(
                    id = "tenant-101",
                    name = "Vikram Singh",
                    phone = "9876543210",
                    familyMembersCount = 3,
                    roomId = "room-1",
                    allottedPrice = "6000.00",
                    electricityRatePerUnit = "8.00",
                    lastMeterReading = "250.00",
                    currentBalance = "-1500.00",
                    isActive = true,
                    room = RoomDto("room-1", "201", "2BHK", 2, "6000.00", true)
                )
            ),
            onNavigateBack = {},
            onDownloadPdf = {}
        )
    }
}
