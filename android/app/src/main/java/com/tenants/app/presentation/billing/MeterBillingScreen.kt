package com.tenants.app.presentation.billing

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tenants.app.R
import com.tenants.app.presentation.theme.*
import org.koin.androidx.compose.koinViewModel
import java.io.File
import java.io.FileOutputStream

@Composable
fun MeterBillingScreen(
    viewModel: MeterBillingViewModel = koinViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(state.exportedCsvBytes) {
        state.exportedCsvBytes?.let { bytes ->
            val month = state.billingMonth.ifBlank { "current" }
            saveCsvToDownloads(context, bytes, "rent-roll-$month.csv")
            Toast.makeText(context, "CSV downloaded to Downloads folder", Toast.LENGTH_LONG).show()
            viewModel.onEvent(MeterBillingEvent.ClearMessages)
        }
    }

    LaunchedEffect(state.successMessage) {
        state.successMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.onEvent(MeterBillingEvent.ClearMessages)
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.onEvent(MeterBillingEvent.ClearMessages)
        }
    }

    MeterBillingContent(
        state = state,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeterBillingContent(
    state: MeterBillingUiState,
    onEvent: (MeterBillingEvent) -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.meter_readings_title), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        if (state.billingMonth.isNotBlank()) {
                            Text("Month: ${state.billingMonth}", style = MaterialTheme.typography.labelSmall, color = Outline)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Primary)
                    }
                },
                actions = {
                    IconButton(onClick = { onEvent(MeterBillingEvent.ExportBillsCsv) }, enabled = !state.isExporting) {
                        if (state.isExporting) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Primary)
                        } else {
                            Icon(Icons.Default.Download, contentDescription = "Export CSV", tint = Primary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface, titleContentColor = Primary)
            )
        },
        containerColor = Surface
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Action Banner for Idempotent Rent Generation
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Monthly Rent Roll",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = Primary
                        )
                        Text(
                            text = "Charge this month's fixed rent across all active tenants (idempotent).",
                            style = MaterialTheme.typography.bodySmall,
                            color = Outline
                        )
                    }
                    Button(
                        onClick = { onEvent(MeterBillingEvent.GenerateRentRoll) },
                        enabled = !state.isGeneratingRent,
                        shape = RoundedCornerShape(9999.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        if (state.isGeneratingRent) {
                            CircularProgressIndicator(color = OnPrimary, modifier = Modifier.size(18.dp))
                        } else {
                            Text("Charge Rent", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Meter List Header & Save Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tenant Meter Readings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
                Button(
                    onClick = { onEvent(MeterBillingEvent.SaveReadings) },
                    enabled = !state.isLoading && state.tenantMeters.isNotEmpty(),
                    shape = RoundedCornerShape(9999.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(color = OnPrimary, modifier = Modifier.size(18.dp))
                    } else {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save All", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Tenants Meter Readings List
            if (state.isLoading && state.tenantMeters.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            } else if (state.tenantMeters.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No active tenants found.", color = Outline)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.tenantMeters, key = { it.summary.tenantId }) { item ->
                        val prev = item.summary.lastMeterReading
                        val curr = item.currentReadingInput.toDoubleOrNull() ?: prev
                        val rate = item.unitRateInput.toDoubleOrNull() ?: item.summary.electricityRatePerUnit
                        val units = if (curr >= prev) curr - prev else 0.0
                        val elecAmount = units * rate
                        val totalMonth = item.summary.allottedRent + elecAmount

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(item.summary.tenantName, fontWeight = FontWeight.Bold, color = OnSurface)
                                        Text("Room: ${item.summary.roomNo} • Base Rent: ₹${item.summary.allottedRent}", style = MaterialTheme.typography.bodySmall, color = Outline)
                                    }
                                    if (item.summary.isBilledForMonth) {
                                        Surface(
                                            color = SecondaryContainer,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "Billed",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Primary,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Prev Reading", style = MaterialTheme.typography.labelSmall, color = Outline)
                                        Text("${prev.toInt()}", fontWeight = FontWeight.Bold, color = OnSurface)
                                    }

                                    OutlinedTextField(
                                        value = item.currentReadingInput,
                                        onValueChange = { onEvent(MeterBillingEvent.ReadingChanged(item.summary.tenantId, it)) },
                                        label = { Text("Curr Reading") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1.3f),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp)
                                    )

                                    OutlinedTextField(
                                        value = item.unitRateInput,
                                        onValueChange = { onEvent(MeterBillingEvent.RateChanged(item.summary.tenantId, it)) },
                                        label = { Text("₹/Unit") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1.1f),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }

                                HorizontalDivider(color = OutlineVariant.copy(alpha = 0.5f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Units: ${"%.1f".format(units)} (₹${"%.2f".format(elecAmount)})", style = MaterialTheme.typography.bodySmall, color = Primary, fontWeight = FontWeight.SemiBold)
                                    Text("Total Bill: ₹${"%.2f".format(totalMonth)}", style = MaterialTheme.typography.bodySmall, color = Primary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun saveCsvToDownloads(context: Context, bytes: ByteArray, fileName: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            resolver.openOutputStream(it)?.use { stream ->
                stream.write(bytes)
            }
        }
    } else {
        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloads, fileName)
        FileOutputStream(file).use { it.write(bytes) }
    }
}
