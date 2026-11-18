package com.tenants.app.presentation.tenants

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.tenants.app.R
import com.tenants.app.data.remote.model.RoomDto
import com.tenants.app.data.remote.model.TenantDto
import com.tenants.app.presentation.components.ConfirmationDialog
import com.tenants.app.presentation.theme.*
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

/**
 * Stateful entry point for the Tenants management screen.
 */
@Composable
fun TenantsScreen(
    viewModel: TenantsViewModel = koinViewModel(),
    onTenantClick: (String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TenantsContent(
        state = state,
        onEvent = viewModel::onEvent,
        onTenantClick = onTenantClick,
        onUploadImage = { bytes, mime, name -> viewModel.uploadImageFile(bytes, mime, name) }
    )
}

/**
 * Pure declarative presentation for Tenants screen adhering strictly to Emerald & Parchment tokens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TenantsContent(
    state: TenantsUiState,
    onEvent: (TenantsEvent) -> Unit,
    onTenantClick: (String) -> Unit = {},
    onUploadImage: (suspend (ByteArray, String, String) -> String?)? = null
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Short
            )
            onEvent(TenantsEvent.ClearError)
        }
    }

    LaunchedEffect(state.successMessage) {
        state.successMessage?.let { success ->
            snackbarHostState.showSnackbar(
                message = success,
                duration = SnackbarDuration.Short
            )
            onEvent(TenantsEvent.ClearSuccess)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.nav_tenants),
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
                onClick = { onEvent(TenantsEvent.ShowAddDialog) },
                containerColor = SecondaryContainer,
                contentColor = Primary,
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_tenant)
                )
            }
        },
        containerColor = Surface
    ) { paddingValues ->
        val pullRefreshState = rememberPullToRefreshState()
        PullToRefreshBox(
            isRefreshing = state.isPullRefreshing,
            onRefresh = { onEvent(TenantsEvent.PullRefresh) },
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
            if (state.isLoading && state.tenants.isEmpty()) {
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
                    items(state.tenants, key = { it.id }) { tenant ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onTenantClick(tenant.id) },
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
                                if (!tenant.profilePicUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = tenant.profilePicUrl,
                                        contentDescription = tenant.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .border(1.dp, Primary.copy(alpha = 0.5f), CircleShape)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(
                                                color = if (tenant.isActive) SecondaryContainer else SurfaceContainerHighest,
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = Primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = tenant.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = OnSurface
                                        )
                                        Text(
                                            text = "Room ${tenant.room?.roomNo ?: "None"} • ${tenant.phone}",
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { onEvent(TenantsEvent.RequestEditTenant(tenant)) }) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = stringResource(R.string.edit_tenant),
                                            tint = Primary
                                        )
                                    }
                                    IconButton(onClick = { onEvent(TenantsEvent.RequestDeleteTenant(tenant)) }) {
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

        // Add Tenant Dialog
        if (state.showAddDialog) {
            AddTenantDialog(
                availableRooms = state.availableRooms,
                onDismiss = { onEvent(TenantsEvent.DismissAddDialog) },
                onUploadImage = onUploadImage,
                onConfirm = { event -> onEvent(event) }
            )
        }

        // Edit Tenant Dialog
        state.tenantToEdit?.let { tenant ->
            EditTenantDialog(
                tenant = tenant,
                onDismiss = { onEvent(TenantsEvent.DismissEditDialog) },
                onUploadImage = onUploadImage,
                onConfirm = { id, name, phone, members, rent, electricity, adhaarUrl, profileUrl, isActive ->
                    onEvent(TenantsEvent.UpdateTenant(id, name, phone, members, rent, electricity, adhaarUrl, profileUrl, isActive))
                }
            )
        }

        // Deletion Confirmation Dialog
        state.tenantToDelete?.let { tenant ->
            ConfirmationDialog(
                title = stringResource(R.string.confirm_delete_title),
                message = stringResource(R.string.delete_tenant_warning, tenant.name),
                confirmText = stringResource(R.string.delete_button),
                onConfirm = { onEvent(TenantsEvent.ConfirmDeleteTenant) },
                onDismiss = { onEvent(TenantsEvent.DismissDeleteDialog) }
            )
        }
    }
}

/**
 * Polished Add Tenant Dialog with room allocation selector and Emerald & Parchment styling.
 */
@Composable
private fun AddTenantDialog(
    availableRooms: List<RoomDto>,
    onDismiss: () -> Unit,
    onUploadImage: (suspend (ByteArray, String, String) -> String?)? = null,
    onConfirm: (TenantsEvent.CreateTenant) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var membersCount by remember { mutableStateOf("1") }
    var selectedRoomId by remember { mutableStateOf(availableRooms.firstOrNull()?.id ?: "") }
    var rent by remember { mutableStateOf(availableRooms.firstOrNull()?.basePrice ?: "") }
    var electricityRate by remember { mutableStateOf("8.00") }
    var meterReading by remember { mutableStateOf("0.00") }
    var openingBalance by remember { mutableStateOf("0.00") }
    var profilePicUrl by remember { mutableStateOf<String?>(null) }
    var adhaarCardUrl by remember { mutableStateOf<String?>(null) }
    var isUploadingProfile by remember { mutableStateOf(false) }
    var isUploadingAdhaar by remember { mutableStateOf(false) }
    var hasAttemptedSubmit by remember { mutableStateOf(false) }

    val profilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null && onUploadImage != null) {
            scope.launch {
                isUploadingProfile = true
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                if (bytes != null) {
                    profilePicUrl = onUploadImage(bytes, mime, "profile_${System.currentTimeMillis()}.jpg")
                }
                isUploadingProfile = false
            }
        }
    }

    val adhaarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null && onUploadImage != null) {
            scope.launch {
                isUploadingAdhaar = true
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                if (bytes != null) {
                    adhaarCardUrl = onUploadImage(bytes, mime, "adhaar_${System.currentTimeMillis()}.jpg")
                }
                isUploadingAdhaar = false
            }
        }
    }

    val isNameValid = name.trim().isNotBlank()
    val isPhoneValid = phone.trim().length == 10
    val isRoomSelected = selectedRoomId.isNotBlank()
    val isRentValid = (rent.toDoubleOrNull() ?: 0.0) > 0.0

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
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        title = {
            Text(
                text = stringResource(R.string.add_tenant),
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
                // Room Allocation Dropdown Section
                Text(
                    text = stringResource(R.string.select_room_label),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )

                if (availableRooms.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SecondaryContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.no_vacant_rooms),
                                style = MaterialTheme.typography.bodySmall,
                                color = Primary
                            )
                        }
                    }
                } else {
                    var isRoomDropdownOpen by remember { mutableStateOf(false) }
                    val selectedRoom = availableRooms.firstOrNull { it.id == selectedRoomId }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isRoomDropdownOpen = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.outlinedCardColors(containerColor = SurfaceContainerLow),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = if (selectedRoom != null) Primary else OutlineVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MeetingRoom,
                                        contentDescription = null,
                                        tint = Primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = selectedRoom?.let { "Room ${it.roomNo} (Floor ${it.floor})" }
                                                ?: stringResource(R.string.select_vacant_room_hint),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = OnSurface
                                        )
                                        if (selectedRoom != null) {
                                            Text(
                                                text = "Base Rent: ₹${selectedRoom.basePrice}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Outline
                                            )
                                        }
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = Primary
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = isRoomDropdownOpen,
                            onDismissRequest = { isRoomDropdownOpen = false },
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .background(SurfaceContainerHigh)
                        ) {
                            availableRooms.forEach { room ->
                                val isSelected = room.id == selectedRoomId
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = "Room ${room.roomNo} (Floor ${room.floor})",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Primary else OnSurface
                                            )
                                            Text(
                                                text = "Base Rent: ₹${room.basePrice}${if (room.description.isNullOrBlank()) "" else " • ${room.description}"}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Outline
                                            )
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.MeetingRoom,
                                            contentDescription = null,
                                            tint = if (isSelected) Primary else Outline
                                        )
                                    },
                                    onClick = {
                                        selectedRoomId = room.id
                                        rent = room.basePrice
                                        isRoomDropdownOpen = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (hasAttemptedSubmit && !isRoomSelected) {
                    Text(
                        text = stringResource(R.string.room_selection_required),
                        color = Error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Tenant Personal Details
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.tenant_name_hint)) },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Primary) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    isError = hasAttemptedSubmit && !isNameValid,
                    supportingText = if (hasAttemptedSubmit && !isNameValid) {
                        { Text(stringResource(R.string.tenant_name_required), color = Error) }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }
                        if (digits.length <= 10) {
                            phone = digits
                        }
                    },
                    label = { Text(stringResource(R.string.phone_hint)) },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Primary) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(12.dp),
                    isError = hasAttemptedSubmit && !isPhoneValid,
                    supportingText = if (hasAttemptedSubmit && !isPhoneValid) {
                        { Text(stringResource(R.string.phone_invalid), color = Error) }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = membersCount,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }
                        if (digits.length <= 2) {
                            membersCount = digits
                        }
                    },
                    label = { Text(stringResource(R.string.members_count_hint)) },
                    leadingIcon = { Icon(Icons.Default.Group, contentDescription = null, tint = Primary) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = rent,
                    onValueChange = { rent = it },
                    label = { Text(stringResource(R.string.allotted_rent_hint)) },
                    leadingIcon = { Icon(Icons.Default.CurrencyRupee, contentDescription = null, tint = Primary) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    isError = hasAttemptedSubmit && !isRentValid,
                    supportingText = if (hasAttemptedSubmit && !isRentValid) {
                        { Text(stringResource(R.string.rent_invalid), color = Error) }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = electricityRate,
                    onValueChange = { electricityRate = it },
                    label = { Text(stringResource(R.string.electricity_rate_hint)) },
                    leadingIcon = { Icon(Icons.Default.ElectricBolt, contentDescription = null, tint = Primary) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = meterReading,
                    onValueChange = { meterReading = it },
                    label = { Text(stringResource(R.string.meter_reading_hint)) },
                    leadingIcon = { Icon(Icons.Default.Speed, contentDescription = null, tint = Primary) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = openingBalance,
                    onValueChange = { openingBalance = it },
                    label = { Text(stringResource(R.string.opening_balance_hint)) },
                    leadingIcon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Primary) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Optional Photo & Document Attachments
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Profile Photo Attachment
                    Column(modifier = Modifier.weight(1f)) {
                        if (isUploadingProfile) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .background(SurfaceContainerLow, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Primary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.uploading_media), style = MaterialTheme.typography.labelSmall, color = Outline)
                            }
                        } else if (!profilePicUrl.isNullOrBlank()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .background(SecondaryContainer, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                AsyncImage(
                                    model = profilePicUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                )
                                IconButton(
                                    onClick = { profilePicUrl = null },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = stringResource(R.string.remove_attachment),
                                        tint = Error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    profilePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.pick_photo_action), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                            }
                        }
                    }

                    // Aadhaar Attachment
                    Column(modifier = Modifier.weight(1f)) {
                        if (isUploadingAdhaar) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .background(SurfaceContainerLow, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Primary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.uploading_media), style = MaterialTheme.typography.labelSmall, color = Outline)
                            }
                        } else if (!adhaarCardUrl.isNullOrBlank()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .background(SecondaryContainer, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                AsyncImage(
                                    model = adhaarCardUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                )
                                IconButton(
                                    onClick = { adhaarCardUrl = null },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = stringResource(R.string.remove_attachment),
                                        tint = Error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    adhaarPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Default.Badge, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.pick_adhaar_action), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    hasAttemptedSubmit = true
                    if (isNameValid && isPhoneValid && isRoomSelected && isRentValid) {
                        onConfirm(
                            TenantsEvent.CreateTenant(
                                name = name.trim(),
                                phone = phone.trim(),
                                membersCount = membersCount.toIntOrNull() ?: 1,
                                roomId = selectedRoomId,
                                rent = rent.toDoubleOrNull() ?: 0.0,
                                electricityRate = electricityRate.toDoubleOrNull() ?: 8.0,
                                meterReading = meterReading.toDoubleOrNull() ?: 0.0,
                                openingBalance = openingBalance.toDoubleOrNull() ?: 0.0,
                                adhaarCardUrl = adhaarCardUrl,
                                profilePicUrl = profilePicUrl
                            )
                        )
                    }
                },
                enabled = availableRooms.isNotEmpty(),
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
 * Polished Edit Tenant Dialog matching Emerald & Parchment showcase specifications.
 */
@Composable
private fun EditTenantDialog(
    tenant: TenantDto,
    onDismiss: () -> Unit,
    onUploadImage: (suspend (ByteArray, String, String) -> String?)? = null,
    onConfirm: (id: String, name: String, phone: String, members: Int, rent: Double, electricity: Double, adhaarUrl: String?, profileUrl: String?, isActive: Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var editName by remember(tenant) { mutableStateOf(tenant.name) }
    var editPhone by remember(tenant) { mutableStateOf(tenant.phone) }
    var editMembers by remember(tenant) { mutableStateOf(tenant.familyMembersCount.toString()) }
    var editRent by remember(tenant) { mutableStateOf(tenant.allottedPrice) }
    var editElectricityRate by remember(tenant) { mutableStateOf(tenant.electricityRatePerUnit) }
    var profilePicUrl by remember(tenant) { mutableStateOf(tenant.profilePicUrl) }
    var adhaarCardUrl by remember(tenant) { mutableStateOf(tenant.adhaarCardUrl) }
    var isUploadingProfile by remember { mutableStateOf(false) }
    var isUploadingAdhaar by remember { mutableStateOf(false) }
    var isActive by remember(tenant) { mutableStateOf(tenant.isActive) }
    var hasAttemptedSubmit by remember { mutableStateOf(false) }

    val profilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null && onUploadImage != null) {
            scope.launch {
                isUploadingProfile = true
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                if (bytes != null) {
                    profilePicUrl = onUploadImage(bytes, mime, "profile_${System.currentTimeMillis()}.jpg")
                }
                isUploadingProfile = false
            }
        }
    }

    val adhaarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null && onUploadImage != null) {
            scope.launch {
                isUploadingAdhaar = true
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                if (bytes != null) {
                    adhaarCardUrl = onUploadImage(bytes, mime, "adhaar_${System.currentTimeMillis()}.jpg")
                }
                isUploadingAdhaar = false
            }
        }
    }

    val isNameValid = editName.trim().isNotBlank()
    val isPhoneValid = editPhone.trim().length == 10

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
                text = stringResource(R.string.edit_tenant_title, tenant.name),
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
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text(stringResource(R.string.tenant_name_hint)) },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Primary) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    isError = hasAttemptedSubmit && !isNameValid,
                    supportingText = if (hasAttemptedSubmit && !isNameValid) {
                        { Text(stringResource(R.string.tenant_name_required), color = Error) }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = editPhone,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }
                        if (digits.length <= 10) {
                            editPhone = digits
                        }
                    },
                    label = { Text(stringResource(R.string.phone_hint)) },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Primary) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(12.dp),
                    isError = hasAttemptedSubmit && !isPhoneValid,
                    supportingText = if (hasAttemptedSubmit && !isPhoneValid) {
                        { Text(stringResource(R.string.phone_invalid), color = Error) }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = editMembers,
                    onValueChange = { editMembers = it },
                    label = { Text(stringResource(R.string.members_count_hint)) },
                    leadingIcon = { Icon(Icons.Default.Group, contentDescription = null, tint = Primary) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = editRent,
                    onValueChange = { editRent = it },
                    label = { Text(stringResource(R.string.allotted_rent_hint)) },
                    leadingIcon = { Icon(Icons.Default.CurrencyRupee, contentDescription = null, tint = Primary) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = editElectricityRate,
                    onValueChange = { editElectricityRate = it },
                    label = { Text(stringResource(R.string.electricity_rate_hint)) },
                    leadingIcon = { Icon(Icons.Default.ElectricBolt, contentDescription = null, tint = Primary) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Active Status Row
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isActive = !isActive },
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceContainerLow
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.active_status_label),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = OnSurface
                            )
                            Text(
                                text = if (isActive) stringResource(R.string.active_tenant_badge)
                                else stringResource(R.string.inactive_tenant_badge),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isActive) Primary else Error
                            )
                        }
                        Switch(
                            checked = isActive,
                            onCheckedChange = { isActive = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = OnPrimary,
                                checkedTrackColor = Primary
                            )
                        )
                    }
                }

                // Optional Photo & Document Attachments
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Profile Photo Attachment
                    Column(modifier = Modifier.weight(1f)) {
                        if (isUploadingProfile) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .background(SurfaceContainerLow, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Primary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.uploading_media), style = MaterialTheme.typography.labelSmall, color = Outline)
                            }
                        } else if (!profilePicUrl.isNullOrBlank()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .background(SecondaryContainer, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                AsyncImage(
                                    model = profilePicUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                )
                                IconButton(
                                    onClick = { profilePicUrl = null },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = stringResource(R.string.remove_attachment),
                                        tint = Error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    profilePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.pick_photo_action), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                            }
                        }
                    }

                    // Aadhaar Attachment
                    Column(modifier = Modifier.weight(1f)) {
                        if (isUploadingAdhaar) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .background(SurfaceContainerLow, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Primary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.uploading_media), style = MaterialTheme.typography.labelSmall, color = Outline)
                            }
                        } else if (!adhaarCardUrl.isNullOrBlank()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .background(SecondaryContainer, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                AsyncImage(
                                    model = adhaarCardUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                )
                                IconButton(
                                    onClick = { adhaarCardUrl = null },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = stringResource(R.string.remove_attachment),
                                        tint = Error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    adhaarPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Default.Badge, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.pick_adhaar_action), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    hasAttemptedSubmit = true
                    if (isNameValid && isPhoneValid) {
                        val membersInt = editMembers.toIntOrNull() ?: tenant.familyMembersCount
                        val rentDouble = editRent.toDoubleOrNull() ?: tenant.allottedPrice.toDoubleOrNull() ?: 0.0
                        val electricityDouble = editElectricityRate.toDoubleOrNull() ?: tenant.electricityRatePerUnit.toDoubleOrNull() ?: 8.0
                        onConfirm(tenant.id, editName.trim(), editPhone.trim(), membersInt, rentDouble, electricityDouble, adhaarCardUrl, profilePicUrl, isActive)
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
