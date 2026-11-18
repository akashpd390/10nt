package com.tenants.app.presentation.tenants

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.tenants.app.R
import com.tenants.app.data.remote.model.LedgerEntryDto
import com.tenants.app.data.remote.model.RoomDto
import com.tenants.app.data.remote.model.TenantDto
import com.tenants.app.presentation.theme.*
import org.koin.androidx.compose.koinViewModel

/**
 * Top-level Stateful Route for Tenant Details, Ledger Breakdown, and Invoice Export.
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
        onPullRefresh = { viewModel.onEvent(TenantDetailsEvent.PullRefresh) },
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
    onPullRefresh: () -> Unit,
    onDownloadPdf: () -> Unit
) {
    var showFullDocumentDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.tenant?.name ?: stringResource(R.string.tenant_profile_title), fontWeight = FontWeight.SemiBold) },
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
        val pullRefreshState = rememberPullToRefreshState()

        PullToRefreshBox(
            isRefreshing = state.isPullRefreshing,
            onRefresh = onPullRefresh,
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
            if (state.isLoading && state.tenant == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
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
                                // Profile Avatar with photo or fallback
                                if (!tenant.profilePicUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = tenant.profilePicUrl,
                                        contentDescription = tenant.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(76.dp)
                                            .clip(CircleShape)
                                            .border(2.dp, Primary, CircleShape)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(76.dp)
                                            .background(SecondaryContainer, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = null,
                                            tint = Primary,
                                            modifier = Modifier.size(44.dp)
                                        )
                                    }
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

                        // Identity Document (Aadhaar / ID) Card
                        if (!tenant.adhaarCardUrl.isNullOrBlank()) {
                            Text(
                                text = stringResource(R.string.identity_document_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Primary
                            )

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showFullDocumentDialog = true },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        AsyncImage(
                                            model = tenant.adhaarCardUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(52.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .border(1.dp, Outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = stringResource(R.string.identity_document_title),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = OnSurface
                                            )
                                            Text(
                                                text = stringResource(R.string.view_document_full),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Primary
                                            )
                                        }
                                    }
                                    Icon(
                                        Icons.Default.OpenInNew,
                                        contentDescription = null,
                                        tint = Primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // Key Tenancy Information
                        Text(
                            text = stringResource(R.string.tenancy_info_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Primary
                        )

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
                                if (!tenant.joinedDate.isNullOrBlank()) {
                                    val formattedJoined = try {
                                        tenant.joinedDate.substring(0, 10)
                                    } catch (_: Exception) {
                                        tenant.joinedDate
                                    }
                                    InfoRow(
                                        icon = Icons.Default.CalendarToday,
                                        label = stringResource(R.string.joined_date_label),
                                        value = formattedJoined
                                    )
                                }
                            }
                        }

                        // Recent Activity (Ledger History - Last 10 Entries)
                        Text(
                            text = stringResource(R.string.recent_activity_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Primary
                        )

                        val entries = tenant.ledgerEntries ?: emptyList()
                        if (entries.isEmpty()) {
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
                                        text = stringResource(R.string.no_ledger_records),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Outline
                                    )
                                }
                            }
                        } else {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    entries.forEachIndexed { index, entry ->
                                        LedgerEntryRow(entry = entry)
                                        if (index < entries.size - 1) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(vertical = 8.dp),
                                                color = Outline.copy(alpha = 0.15f)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

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
                                Text(stringResource(R.string.generating_statement), color = OnPrimary)
                            } else {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = OnPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.download_share_invoice),
                                    fontWeight = FontWeight.Bold,
                                    color = OnPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Full Document Preview Dialog
    if (showFullDocumentDialog && state.tenant?.adhaarCardUrl != null) {
        Dialog(onDismissRequest = { showFullDocumentDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.identity_document_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    AsyncImage(
                        model = state.tenant.adhaarCardUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    FilledTonalButton(
                        onClick = { showFullDocumentDialog = false },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = SecondaryContainer,
                            contentColor = Primary
                        ),
                        shape = RoundedCornerShape(9999.dp)
                    ) {
                        Text(stringResource(R.string.close_document))
                    }
                }
            }
        }
    }
}

@Composable
private fun LedgerEntryRow(entry: LedgerEntryDto) {
    val isPayment = entry.type == "PAYMENT_RECEIVED"
    val isBill = entry.type == "BILL_CHARGE"

    val icon = when {
        isPayment -> Icons.Default.ArrowDownward
        isBill -> Icons.Default.ReceiptLong
        else -> Icons.Default.Tune
    }

    val iconContainerColor = when {
        isPayment -> SecondaryContainer
        isBill -> ErrorContainer.copy(alpha = 0.5f)
        else -> SurfaceContainerHigh
    }

    val iconColor = when {
        isPayment -> Primary
        isBill -> Error
        else -> Outline
    }

    val amountPrefix = if (isPayment) "+₹" else if (isBill) "-₹" else "₹"
    val amountColor = if (isPayment) Primary else if (isBill) Error else OnSurface

    val cleanDate = try {
        entry.createdAt.substring(0, 10)
    } catch (_: Exception) {
        entry.createdAt
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(iconContainerColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.description,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = cleanDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = Outline
                )
                Text(
                    text = "•",
                    style = MaterialTheme.typography.labelSmall,
                    color = Outline
                )
                Text(
                    text = stringResource(R.string.running_balance_prefix, entry.runningBalance),
                    style = MaterialTheme.typography.labelSmall,
                    color = Primary
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "$amountPrefix${entry.amount}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = amountColor
        )
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
                    room = RoomDto("room-1", "201", "2BHK", 2, "6000.00", true),
                    joinedDate = "2026-01-15T00:00:00.000Z",
                    ledgerEntries = listOf(
                        LedgerEntryDto(
                            id = "led-1",
                            tenantId = "tenant-101",
                            type = "BILL_CHARGE",
                            amount = "6500.00",
                            runningBalance = "-6500.00",
                            description = "Monthly Rent & Electricity Bill - 2026-11",
                            createdAt = "2026-11-01T08:00:00.000Z"
                        ),
                        LedgerEntryDto(
                            id = "led-2",
                            tenantId = "tenant-101",
                            type = "PAYMENT_RECEIVED",
                            amount = "5000.00",
                            runningBalance = "-1500.00",
                            description = "Payment via UPI",
                            createdAt = "2026-11-05T10:30:00.000Z"
                        )
                    )
                )
            ),
            onNavigateBack = {},
            onPullRefresh = {},
            onDownloadPdf = {}
        )
    }
}
