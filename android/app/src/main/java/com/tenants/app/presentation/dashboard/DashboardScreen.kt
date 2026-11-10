package com.tenants.app.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tenants.app.R
import com.tenants.app.data.remote.model.*
import com.tenants.app.presentation.theme.*
import org.koin.androidx.compose.koinViewModel

/**
 * Top-level stateful route for Dashboard.
 */
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DashboardContent(
        state = state,
        onEvent = viewModel::onEvent
    )
}

/**
 * Pure declarative, stateless Dashboard Composable adhering to UDF.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent(
    state: DashboardUiState,
    onEvent: (DashboardEvent) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.dashboard_title), fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Surface,
                    titleContentColor = Primary
                ),
                actions = {
                    IconButton(onClick = { onEvent(DashboardEvent.Refresh) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Primary)
                    }
                }
            )
        },
        containerColor = Surface
    ) { paddingValues ->
        when {
            state.isLoading && state.data == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Loading property metrics...", style = MaterialTheme.typography.bodyMedium, color = Outline)
                    }
                }
            }
            state.errorMessage != null && state.data == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("⚠️ Could Not Load Dashboard", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Error)
                            Text(
                                text = state.errorMessage ?: "Network or server connection failed.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Outline,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { onEvent(DashboardEvent.Refresh) },
                                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                shape = RoundedCornerShape(9999.dp)
                            ) {
                                Text("Retry Connection", color = OnPrimary)
                            }
                        }
                    }
                }
            }
            state.data == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("No Property Data Available", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Primary)
                        Text("Add rooms and tenants to populate this dashboard.", style = MaterialTheme.typography.bodyMedium, color = Outline)
                        Button(
                            onClick = { onEvent(DashboardEvent.Refresh) },
                            colors = ButtonDefaults.buttonColors(containerColor = Primary),
                            shape = RoundedCornerShape(9999.dp)
                        ) {
                            Text("Refresh", color = OnPrimary)
                        }
                    }
                }
            }
            else -> {
                val data = state.data
                if (data != null) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Occupancy & Tenants Summary Cards
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                MetricCard(
                                    title = stringResource(R.string.occupancy_label),
                                    value = "${data.occupancy.occupiedRooms}/${data.occupancy.totalRooms} Rooms",
                                    subtitle = "${data.occupancy.occupancyPercentage}% Occupied",
                                    icon = Icons.Default.MeetingRoom,
                                    modifier = Modifier.weight(1f)
                                )
                                MetricCard(
                                    title = "Tenants",
                                    value = "${data.tenantsSummary.totalActiveTenants} Active",
                                    subtitle = "${data.tenantsSummary.totalPeopleCount} Total People",
                                    icon = Icons.Default.People,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Financial Highlights
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceContainer)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Finances Overview",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(stringResource(R.string.total_pending_label), style = MaterialTheme.typography.bodySmall, color = Outline)
                                            Text(
                                                text = "₹${"%.2f".format(data.finances.totalPendingBalance)}",
                                                style = MaterialTheme.typography.titleLarge,
                                                color = Error,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(stringResource(R.string.total_advance_label), style = MaterialTheme.typography.bodySmall, color = Outline)
                                            Text(
                                                text = "₹${"%.2f".format(data.finances.totalAdvanceBalance)}",
                                                style = MaterialTheme.typography.titleLarge,
                                                color = Primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Unpaid Tenants Section Header
                        item {
                            Text(
                                text = stringResource(R.string.unpaid_tenants_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Primary
                            )
                        }

                        if (data.unpaidTenants.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = stringResource(R.string.no_unpaid_tenants),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Outline
                                        )
                                    }
                                }
                            }
                        } else {
                            items(data.unpaidTenants) { unpaid ->
                                UnpaidTenantCard(unpaid = unpaid)
                            }
                        }

                        // Recent Payments
                        item {
                            Text(
                                text = stringResource(R.string.recent_payments_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Primary
                            )
                        }

                        items(data.recentTransactions) { tx ->
                            RecentTxCard(tx = tx)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = title, tint = Primary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, style = MaterialTheme.typography.bodySmall, color = Outline)
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = Primary)
        }
    }
}

@Composable
fun UnpaidTenantCard(unpaid: UnpaidTenantDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = unpaid.tenantName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(text = "Room: ${unpaid.roomNo} • ${unpaid.phone}", style = MaterialTheme.typography.bodySmall, color = Outline)
            }
            Text(
                text = "Due: ₹${"%.2f".format(unpaid.dueAmount)}",
                style = MaterialTheme.typography.titleSmall,
                color = Error,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun RecentTxCard(tx: RecentTxDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = tx.tenantName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(text = "Mode: ${tx.mode}", style = MaterialTheme.typography.labelSmall, color = Outline)
            }
            Text(
                text = "+₹${"%.2f".format(tx.amount)}",
                style = MaterialTheme.typography.titleSmall,
                color = Primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Preview(name = "Dashboard Content Preview", showBackground = true)
@Composable
private fun DashboardContentPreview() {
    TenantsTheme {
        DashboardContent(
            state = DashboardUiState(
                isLoading = false,
                data = DashboardResponse(
                    occupancy = OccupancyDto(totalRooms = 10, occupiedRooms = 8, availableRooms = 2, occupancyPercentage = 80.0),
                    tenantsSummary = TenantsSummaryDto(totalActiveTenants = 8, totalPeopleCount = 18),
                    finances = FinancesDto(totalPendingBalance = 12500.0, totalAdvanceBalance = 4000.0),
                    unpaidTenants = listOf(
                        UnpaidTenantDto(
                            tenantId = "1",
                            tenantName = "Rahul Sharma",
                            roomNo = "101",
                            phone = "9876543210",
                            dueAmount = 6500.0,
                            profilePicUrl = null
                        ),
                        UnpaidTenantDto(
                            tenantId = "2",
                            tenantName = "Amit Verma",
                            roomNo = "104",
                            phone = "9876543211",
                            dueAmount = 3200.0,
                            profilePicUrl = null
                        )
                    ),
                    recentTransactions = listOf(
                        RecentTxDto(id = "1", tenantName = "Rahul Sharma", amount = 5000.0, mode = "UPI", date = "2026-11-09", balanceAfter = -1500.0)
                    )
                )
            ),
            onEvent = {}
        )
    }
}
