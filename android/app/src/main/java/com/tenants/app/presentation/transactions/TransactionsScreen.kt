package com.tenants.app.presentation.transactions

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.tenants.app.R
import com.tenants.app.data.remote.model.PaymentRecordDto
import com.tenants.app.data.remote.model.RoomDto
import com.tenants.app.data.remote.model.TenantDto
import com.tenants.app.presentation.theme.*
import org.koin.androidx.compose.koinViewModel

/**
 * Top-level Stateful Route for the All Transactions History Screen.
 *
 * @param onNavigateBack Callback when back navigation is requested.
 * @param viewModel Injected [TransactionsViewModel].
 */
@Composable
fun TransactionsScreen(
    onNavigateBack: () -> Unit,
    viewModel: TransactionsViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TransactionsContent(
        state = state,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack
    )
}

/**
 * Stateless Content Composable for the Transactions History Screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsContent(
    state: TransactionsUiState,
    onEvent: (TransactionsEvent) -> Unit,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val refreshRotation = remember { Animatable(0f) }

    LaunchedEffect(state.isPullRefreshing) {
        if (state.isPullRefreshing) {
            refreshRotation.animateTo(
                targetValue = refreshRotation.value + 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 800, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        }
    }

    val pullRefreshState = rememberPullToRefreshState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.all_transactions_title),
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cancel_button),
                            tint = Primary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        coroutineScope.launch {
                            refreshRotation.animateTo(
                                targetValue = refreshRotation.value + 360f,
                                animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
                            )
                        }
                        onEvent(TransactionsEvent.Refresh)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Primary,
                            modifier = Modifier.rotate(refreshRotation.value)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        },
        containerColor = Surface
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = state.isPullRefreshing,
            onRefresh = { onEvent(TransactionsEvent.PullRefresh) },
            state = pullRefreshState,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullRefreshState,
                    isRefreshing = state.isPullRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter),
                    containerColor = SurfaceContainerHigh,
                    color = Primary
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Search Input Field
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { onEvent(TransactionsEvent.SearchQueryChanged(it)) },
                    placeholder = { Text(stringResource(R.string.search_transactions_hint), color = Outline) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Primary) },
                    trailingIcon = if (state.searchQuery.isNotBlank()) {
                        {
                            IconButton(onClick = { onEvent(TransactionsEvent.ClearSearch) }) {
                                Icon(Icons.Default.Clear, contentDescription = null, tint = Outline)
                            }
                        }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = OutlineVariant,
                        cursorColor = Primary,
                        focusedTextColor = OnSurface,
                        unfocusedTextColor = OnSurface,
                        focusedContainerColor = SurfaceContainerLow,
                        unfocusedContainerColor = SurfaceContainerLow
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Mode Filter Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filterModes = listOf("ALL", "UPI", "CASH", "BANK_TRANSFER")
                    filterModes.forEach { mode ->
                        val isSelected = state.selectedFilterMode.equals(mode, ignoreCase = true)
                        val displayLabel = if (mode == "ALL") stringResource(R.string.filter_all) else mode
                        FilterChip(
                            selected = isSelected,
                            onClick = { onEvent(TransactionsEvent.FilterModeSelected(mode)) },
                            label = {
                                Text(
                                    text = displayLabel,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            shape = RoundedCornerShape(9999.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Primary,
                                selectedLabelColor = OnPrimary,
                                containerColor = SurfaceContainerLow,
                                labelColor = OnSurface
                            )
                        )
                    }
                }

                // Summary Stats Bar
                val totalAmount = state.filteredTransactions.sumOf { it.amountPaid.toDoubleOrNull() ?: 0.0 }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(
                            R.string.transactions_count_summary,
                            state.filteredTransactions.size,
                            "%.2f".format(totalAmount)
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Primary
                    )
                }

                // Transactions List or States
                if (state.isLoading && state.allTransactions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Primary)
                    }
                } else if (state.filteredTransactions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(color = Primary.copy(alpha = 0.1f), shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = null,
                                    tint = Primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Text(
                                text = stringResource(R.string.no_transactions_found),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = OnSurface
                            )
                            Text(
                                text = stringResource(R.string.no_transactions_found_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = Outline
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(state.filteredTransactions, key = { it.id }) { tx ->
                            TransactionRecordCard(record = tx)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Polished transaction record item styled with Emerald & Parchment design tokens.
 */
@Composable
fun TransactionRecordCard(record: PaymentRecordDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mode Chip
                Surface(
                    shape = RoundedCornerShape(9999.dp),
                    color = when (record.paymentMode.uppercase()) {
                        "UPI" -> SecondaryContainer
                        "CASH" -> SurfaceContainerHigh
                        else -> SurfaceContainerLow
                    }
                ) {
                    Text(
                        text = record.paymentMode,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (record.paymentMode.uppercase()) {
                            "UPI" -> Primary
                            else -> OnSurface
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                // Amount
                val amountDouble = record.amountPaid.toDoubleOrNull() ?: 0.0
                Text(
                    text = "+₹${"%.2f".format(amountDouble)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val tenantName = record.tenant?.name ?: "Landlord Direct"
                    val roomNo = record.tenant?.room?.roomNo
                    Text(
                        text = tenantName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface
                    )
                    if (!roomNo.isNullOrBlank()) {
                        Text(
                            text = "Room $roomNo",
                            style = MaterialTheme.typography.bodySmall,
                            color = Outline
                        )
                    }
                    if (!record.notes.isNullOrBlank()) {
                        Text(
                            text = record.notes,
                            style = MaterialTheme.typography.labelSmall,
                            color = Outline,
                            maxLines = 1
                        )
                    }
                }

                // Balance after / Date
                Column(horizontalAlignment = Alignment.End) {
                    val bal = record.balanceAfter?.toDoubleOrNull()
                    if (bal != null) {
                        Text(
                            text = if (bal < 0) "Due: ₹${"%.2f".format(kotlin.math.abs(bal))}"
                            else if (bal > 0) "Advance: ₹${"%.2f".format(bal)}"
                            else "Settled (₹0.00)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (bal < 0) Error else Primary
                        )
                    }
                    val dateFormatted = record.paymentDate.take(10)
                    Text(
                        text = dateFormatted,
                        style = MaterialTheme.typography.labelSmall,
                        color = Outline
                    )
                }
            }
        }
    }
}

@Preview(name = "Transactions Screen Preview", showBackground = true)
@Composable
private fun TransactionsPreview() {
    TenantsTheme {
        TransactionsContent(
            state = TransactionsUiState(
                isLoading = false,
                allTransactions = listOf(
                    PaymentRecordDto(
                        id = "1",
                        tenantId = "t1",
                        amountPaid = "5500.00",
                        paymentMode = "UPI",
                        paymentDate = "2026-11-13T16:09:12.358Z",
                        notes = "Manual payment via UPI",
                        balanceAfter = "7180.00",
                        tenant = TenantDto(
                            id = "t1",
                            name = "Rahul Sharma",
                            phone = "9876543210",
                            familyMembersCount = 2,
                            roomId = "r1",
                            allottedPrice = "5500.00",
                            electricityRatePerUnit = "8.00",
                            lastMeterReading = "220.00",
                            currentBalance = "7180.00",
                            isActive = true,
                            room = RoomDto("r1", "101", "1BHK", 1, "5500.00", true)
                        )
                    ),
                    PaymentRecordDto(
                        id = "2",
                        tenantId = "t2",
                        amountPaid = "4000.00",
                        paymentMode = "CASH",
                        paymentDate = "2026-11-10T12:01:37.418Z",
                        notes = "Advance rent payment",
                        balanceAfter = "9000.00",
                        tenant = TenantDto(
                            id = "t2",
                            name = "Pooja Verma",
                            phone = "9876543211",
                            familyMembersCount = 1,
                            roomId = "r2",
                            allottedPrice = "4000.00",
                            electricityRatePerUnit = "8.00",
                            lastMeterReading = "120.00",
                            currentBalance = "9000.00",
                            isActive = true,
                            room = RoomDto("r2", "102", "Single", 1, "4000.00", true)
                        )
                    )
                ),
                filteredTransactions = listOf(
                    PaymentRecordDto(
                        id = "1",
                        tenantId = "t1",
                        amountPaid = "5500.00",
                        paymentMode = "UPI",
                        paymentDate = "2026-11-13T16:09:12.358Z",
                        notes = "Manual payment via UPI",
                        balanceAfter = "7180.00",
                        tenant = TenantDto(
                            id = "t1",
                            name = "Rahul Sharma",
                            phone = "9876543210",
                            familyMembersCount = 2,
                            roomId = "r1",
                            allottedPrice = "5500.00",
                            electricityRatePerUnit = "8.00",
                            lastMeterReading = "220.00",
                            currentBalance = "7180.00",
                            isActive = true,
                            room = RoomDto("r1", "101", "1BHK", 1, "5500.00", true)
                        )
                    )
                ),
                searchQuery = "",
                selectedFilterMode = "ALL"
            ),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}
