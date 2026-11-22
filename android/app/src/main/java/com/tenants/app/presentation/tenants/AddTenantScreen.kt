package com.tenants.app.presentation.tenants

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.tenants.app.R
import com.tenants.app.data.remote.model.RoomDto
import com.tenants.app.presentation.theme.*
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

/**
 * Full dedicated screen for registering a new tenant.
 * Replaces the cramped modal dialog with rich document uploads and clear input fields.
 */
@Composable
fun AddTenantScreen(
    viewModel: TenantsViewModel = koinViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(state.successMessage) {
        state.successMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.onEvent(TenantsEvent.ClearSuccess)
            onNavigateBack()
        }
    }

    AddTenantContent(
        state = state,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        onUploadImage = { bytes, mime, name -> viewModel.uploadImageFile(bytes, mime, name) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTenantContent(
    state: TenantsUiState,
    onEvent: (TenantsEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onUploadImage: (suspend (ByteArray, String, String) -> String?)? = null
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var membersCount by remember { mutableStateOf("1") }
    var selectedRoomId by remember { mutableStateOf("") }
    var rent by remember { mutableStateOf("") }
    var electricityRate by remember { mutableStateOf("8.00") }
    var meterReading by remember { mutableStateOf("0.00") }
    var openingBalance by remember { mutableStateOf("0.00") }
    var profilePicUrl by remember { mutableStateOf<String?>(null) }
    var adhaarCardUrl by remember { mutableStateOf<String?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Media Pickers
    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                isUploading = true
                val (bytes, mime) = readBytesAndMime(context, it)
                if (bytes != null && onUploadImage != null) {
                    val uploaded = onUploadImage(bytes, mime, "photo_${System.currentTimeMillis()}.jpg")
                    if (uploaded != null) profilePicUrl = uploaded
                }
                isUploading = false
            }
        }
    }

    val adhaarPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                isUploading = true
                val (bytes, mime) = readBytesAndMime(context, it)
                if (bytes != null && onUploadImage != null) {
                    val uploaded = onUploadImage(bytes, mime, "adhaar_${System.currentTimeMillis()}.jpg")
                    if (uploaded != null) adhaarCardUrl = uploaded
                }
                isUploading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_tenant_title), fontWeight = FontWeight.Bold) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Photo Selector
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(SurfaceContainerHigh)
                        .clickable { photoPickerLauncher.launch("image/*") }
                        .border(2.dp, Primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (profilePicUrl != null) {
                        AsyncImage(
                            model = profilePicUrl,
                            contentDescription = "Profile Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Primary)
                            Text("Add Photo", style = MaterialTheme.typography.labelSmall, color = Primary)
                        }
                    }
                }
            }

            // Tenant Details Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Personal Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Primary)

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.tenant_name_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text(stringResource(R.string.phone_hint)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = membersCount,
                        onValueChange = { membersCount = it },
                        label = { Text(stringResource(R.string.members_count_hint)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Room & Rent Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Room & Financial Terms", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Primary)

                    Text(stringResource(R.string.select_room_label), style = MaterialTheme.typography.labelMedium, color = Primary)
                    val vacantRooms: List<RoomDto> = state.availableRooms
                    if (vacantRooms.isEmpty()) {
                        Text(stringResource(R.string.no_vacant_rooms), color = Error, style = MaterialTheme.typography.bodySmall)
                    } else {
                        var roomExpanded by remember { mutableStateOf(false) }
                        val selectedRoom = vacantRooms.find { it.id == selectedRoomId }

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { roomExpanded = true },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (selectedRoom != null) "Room ${selectedRoom.roomNo} (Base: ₹${selectedRoom.basePrice})" else stringResource(R.string.select_vacant_room_hint),
                                        color = if (selectedRoom != null) OnSurface else Outline
                                    )
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Primary)
                                }
                            }
                            DropdownMenu(
                                expanded = roomExpanded,
                                onDismissRequest = { roomExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                vacantRooms.forEach { room: RoomDto ->
                                    DropdownMenuItem(
                                        text = { Text("Room ${room.roomNo} • Floor ${room.floor} • Base ₹${room.basePrice}") },
                                        onClick = {
                                            selectedRoomId = room.id
                                            rent = room.basePrice
                                            roomExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = rent,
                        onValueChange = { rent = it },
                        label = { Text(stringResource(R.string.allotted_rent_hint)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = electricityRate,
                            onValueChange = { electricityRate = it },
                            label = { Text("Rate / Unit (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = meterReading,
                            onValueChange = { meterReading = it },
                            label = { Text("Initial Meter") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    OutlinedTextField(
                        value = openingBalance,
                        onValueChange = { openingBalance = it },
                        label = { Text(stringResource(R.string.opening_balance_hint)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Aadhaar / ID Card Section
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.identity_document_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Primary)

                    if (adhaarCardUrl != null) {
                        Box(modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp))) {
                            AsyncImage(
                                model = adhaarCardUrl,
                                contentDescription = "Aadhaar Card",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            IconButton(
                                onClick = { adhaarCardUrl = null },
                                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Document", tint = Error)
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = { adhaarPickerLauncher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, tint = Primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.pick_adhaar_action), color = Primary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Submit Button
            Button(
                onClick = {
                    val rentNum = rent.toDoubleOrNull() ?: 0.0
                    val rateNum = electricityRate.toDoubleOrNull() ?: 8.0
                    val meterNum = meterReading.toDoubleOrNull() ?: 0.0
                    val balNum = openingBalance.toDoubleOrNull() ?: 0.0

                    if (name.isBlank() || phone.isBlank() || selectedRoomId.isBlank() || rentNum <= 0) {
                        Toast.makeText(context, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    onEvent(
                        TenantsEvent.CreateTenant(
                            name = name,
                            phone = phone,
                            membersCount = membersCount.toIntOrNull() ?: 1,
                            roomId = selectedRoomId,
                            rent = rentNum,
                            electricityRate = rateNum,
                            meterReading = meterNum,
                            openingBalance = balNum,
                            adhaarCardUrl = adhaarCardUrl,
                            profilePicUrl = profilePicUrl
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(9999.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                enabled = !state.isLoading && !isUploading
            ) {
                if (state.isLoading || isUploading) {
                    CircularProgressIndicator(color = OnPrimary, modifier = Modifier.size(24.dp))
                } else {
                    Text("Register Tenant", fontWeight = FontWeight.Bold, color = OnPrimary)
                }
            }
        }
    }
}

private fun readBytesAndMime(context: Context, uri: Uri): Pair<ByteArray?, String> {
    val contentResolver = context.contentResolver
    val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
    val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
    return Pair(bytes, mimeType)
}
