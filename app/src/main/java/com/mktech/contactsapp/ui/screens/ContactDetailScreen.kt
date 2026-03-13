package com.mktech.contactsapp.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mktech.contactsapp.data.model.CallLog
import com.mktech.contactsapp.data.model.CallType
import com.mktech.contactsapp.data.model.Contact
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.mktech.contactsapp.R
import com.mktech.contactsapp.ui.components.BannerAd

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ContactDetailScreen(
    contact: Contact?,
    isNewContact: Boolean,
    prefilledPhone: String? = null,
    callHistory: List<CallLog> = emptyList(),
    onSave: (Contact) -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    onCallNow: (String) -> Unit = {}
) {
    var name             by remember { mutableStateOf(contact?.name ?: "") }
    var phoneNumber      by remember { mutableStateOf(prefilledPhone ?: "") }
    var email            by remember { mutableStateOf(contact?.email ?: "") }
    var profileImageUri  by remember { mutableStateOf<String?>(null) }
    var isFavorite       by remember { mutableStateOf(contact?.isFavorite ?: false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isEditMode       by remember { mutableStateOf(isNewContact) }

    val activity = LocalContext.current

    // ── Photo permission ──────────────────────────────────────────────────────
    val photoPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        Manifest.permission.READ_MEDIA_IMAGES
    else
        Manifest.permission.READ_EXTERNAL_STORAGE

    val photoPermissionState = rememberPermissionState(photoPermission)

    // Track if picker should open once permission is granted
    var pendingPickerOpen by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { profileImageUri = it.toString() }
    }

    // Auto-open picker if permission was just granted after a request
    LaunchedEffect(photoPermissionState.status.isGranted) {
        if (photoPermissionState.status.isGranted && pendingPickerOpen) {
            pendingPickerOpen = false
            imagePicker.launch("image/*")
        }
    }

//    val onPickImage: () -> Unit = {
//        if (photoPermissionState.status.isGranted) {
//            imagePicker.launch("image/*")
//        } else {
//            pendingPickerOpen = true
//            photoPermissionState.launchPermissionRequest()
//            activity.startActivity(
//                Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
//                    data = android.net.Uri.fromParts("package", activity.packageName, null)
//                }
//            )
//        }
//    }
    var photoPermissionDeniedOnce by remember { mutableStateOf(false) }

    val onPickImage: () -> Unit = {
        when {
            photoPermissionState.status.isGranted -> {
                imagePicker.launch("image/*")                        // ✅ granted → open picker
            }
            !photoPermissionDeniedOnce || photoPermissionState.status.shouldShowRationale -> {
                pendingPickerOpen = true
                photoPermissionDeniedOnce = true
                photoPermissionState.launchPermissionRequest()       // 1st/2nd ask → show dialog
            }
            else -> {
                activity.startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.fromParts("package", activity.packageName, null)
                })                          // permanently denied → settings
            }
        }
    }

    // ── Populate fields when contact data arrives ─────────────────────────────
    LaunchedEffect(contact?.id, contact?.name) {
        contact?.let {
            name            = it.name
            phoneNumber     = it.phoneNumber
            email           = it.email
            profileImageUri = it.profileImageUri
            isFavorite      = it.isFavorite
        }
    }

    LaunchedEffect(prefilledPhone) {
        if (isNewContact && prefilledPhone != null && phoneNumber.isEmpty()) {
            phoneNumber = prefilledPhone
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isNewContact) stringResource(R.string.new_contact)
                        else if (isEditMode) stringResource(R.string.edit_contact)
                        else contact?.getDisplayName() ?: "Contact",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (!isNewContact) {
                        if (isEditMode) {
                            IconButton(onClick = { showDeleteDialog = true }) {
                                Icon(
                                    Icons.Default.Delete, "Delete",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        } else {
                            if (phoneNumber.isNotBlank()) {
                                IconButton(onClick = { onCallNow(phoneNumber) }) {
                                    Icon(
                                        Icons.Default.Call, "Call",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            IconButton(onClick = { isEditMode = true }) {
                                Icon(
                                    Icons.Default.Edit, "Edit",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Avatar ───────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .then(
                            if (isEditMode) Modifier.clickable { onPickImage() }
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (profileImageUri != null) {
                        AsyncImage(
                            model = profileImageUri,
                            contentDescription = null,
                            modifier = Modifier
                                .size(110.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (isEditMode) {
                                Icon(
                                    Icons.Default.CameraAlt, null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(Modifier.height(4.dp))
                            }
                            Text(
                                text = if (name.isNotEmpty()) name.take(2).uppercase() else "?",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                // ── Remove button — OUTSIDE the inner Box, INSIDE the outer Box ──
                if (isEditMode && profileImageUri != null) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .align(Alignment.Center)
                            .offset(x = 38.dp, y = (-38).dp)   // top-right edge of 110dp circle
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .clickable { profileImageUri = null },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove photo",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                // Call history in view mode
                if (!isEditMode && callHistory.isNotEmpty()) {
                    CallHistorySection(callHistory = callHistory)
                    Spacer(Modifier.height(24.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(24.dp))
                }

                // Favorite toggle - only in edit mode
                if (isEditMode) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isFavorite = !isFavorite },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isFavorite) MaterialTheme.colorScheme.primary.copy(0.1f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                null,
                                tint = if (isFavorite) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                if (isFavorite) stringResource(R.string.remove_from_favorites) else stringResource(R.string.add_to_favorites),
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isFavorite) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                }

                // Name field
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (isEditMode) name = it },
                    label = { Text(stringResource(R.string.name)) },
                    leadingIcon = {
                        Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    enabled = isEditMode,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledBorderColor  = MaterialTheme.colorScheme.surfaceVariant,
                        disabledTextColor    = MaterialTheme.colorScheme.onSurface
                    )
                )

                Spacer(Modifier.height(12.dp))

                // Phone field
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { if (isEditMode) phoneNumber = it },
                    label = { Text(stringResource(R.string.phone_number)) },
                    leadingIcon = {
                        Icon(Icons.Default.Phone, null, tint = MaterialTheme.colorScheme.primary)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    enabled = isEditMode,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledBorderColor  = MaterialTheme.colorScheme.surfaceVariant,
                        disabledTextColor    = MaterialTheme.colorScheme.onSurface
                    )
                )

                Spacer(Modifier.height(12.dp))

                // Email field
                OutlinedTextField(
                    value = email,
                    onValueChange = { if (isEditMode) email = it },
                    label = { Text(stringResource(R.string.email_optional)) },
                    leadingIcon = {
                        Icon(Icons.Default.Email, null, tint = MaterialTheme.colorScheme.primary)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    enabled = isEditMode,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledBorderColor  = MaterialTheme.colorScheme.surfaceVariant,
                        disabledTextColor    = MaterialTheme.colorScheme.onSurface
                    )
                )

                Spacer(Modifier.height(28.dp))

                // Save button - only in edit mode
                if (isEditMode) {
                    Button(
                        onClick = {
                            if (name.isNotBlank() && phoneNumber.isNotBlank()) {
                                val updated = if (isNewContact) {
                                    Contact(
                                        name            = name.trim(),
                                        phoneNumber     = phoneNumber.trim(),
                                        email           = email.trim(),
                                        profileImageUri = profileImageUri,
                                        isFavorite      = isFavorite
                                    )
                                } else {
                                    contact!!.copy(
                                        name            = name.trim(),
                                        phoneNumber     = phoneNumber.trim(),
                                        email           = email.trim(),
                                        profileImageUri = profileImageUri,
                                        isFavorite      = isFavorite
                                    )
                                }
                                Log.d("ContactSave", "Save button clicked, contact=$updated id=${updated.id}")
                                onSave(updated)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        enabled = name.isNotBlank() && phoneNumber.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor   = Color.White
                        )
                    ) {
                        Text(stringResource(R.string.save_contact), fontWeight = FontWeight.SemiBold)
                    }
                }

                // Banner ad at TOP of the bottom bar area
                BannerAd(modifier = Modifier
                    .fillMaxWidth()
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_contact), fontWeight = FontWeight.Bold) },
            text  = { Text("Delete ${contact?.getDisplayName()}? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = { showDeleteDialog = false; onDelete() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

// ── Call history section ──────────────────────────────────────────────────────

@Composable
private fun CallHistorySection(callHistory: List<CallLog>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.recent_calls),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "${callHistory.size} total",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(12.dp))

        callHistory.take(5).forEach { log ->
            CallHistoryItem(log)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CallHistoryItem(log: CallLog) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when (log.callType) {
                CallType.MISSED   -> Icons.Default.PhoneMissed
                CallType.INCOMING -> Icons.Default.CallReceived
                CallType.OUTGOING -> Icons.Default.CallMade
            },
            contentDescription = null,
            tint = when (log.callType) {
                CallType.MISSED   -> MaterialTheme.colorScheme.error
                CallType.INCOMING -> MaterialTheme.colorScheme.primary
                CallType.OUTGOING -> Color(0xFF10B981)
            },
            modifier = Modifier.size(20.dp)
        )

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = log.callType.name.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = formatTime(log.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (log.durationSeconds > 0) {
            Text(
                text = log.getFormattedDuration(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun formatTime(ts: Long): String {
    val now  = System.currentTimeMillis()
    val diff = now - ts

    return when {
        diff < 60_000      -> stringResource(R.string.just_now)
        diff < 3_600_000   -> "${diff / 60_000}m ago"
        diff < 86_400_000  -> "${diff / 3_600_000}h ago"
        diff < 172_800_000 -> stringResource(R.string.yesterday)
        else -> {
            val fmt = java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())
            fmt.format(java.util.Date(ts))
        }
    }
}