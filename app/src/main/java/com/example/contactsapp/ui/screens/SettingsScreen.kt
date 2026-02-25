package com.example.contactsapp.ui.screens

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.telecom.TelecomManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.contactsapp.data.model.AccentColor
import com.example.contactsapp.data.model.AppSettings
import com.example.contactsapp.data.model.AppTheme

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onThemeChange: (AppTheme) -> Unit,
    onAccentColorChange: (AccentColor) -> Unit,
    onSortOrderChange: (Boolean) -> Unit,
    onShowPhoneChange: (Boolean) -> Unit,
    onConfirmDeleteChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var isDefaultApp by remember { mutableStateOf(isDefaultDialerApp(context)) }

    val defaultDialerLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            // Called when user returns from system dialog
            isDefaultApp = isDefaultDialerApp(context)
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {

        // ── Default App ──────────────────────────────────────────────────
        // ── Default App ──────────────────────────────────────────────────
        if (!isDefaultApp) {
            SettingsSection(title = "Default App") {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            Log.e("DEFAULT_DIALER", "Set as default button clicked")
                            requestDefaultDialer(context, defaultDialerLauncher)
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    color = Color.Transparent
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Set as default contacts app",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Make this your default contacts and dialer app",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Open settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Appearance ───────────────────────────────────────────────────
        SettingsSection(title = "Appearance") {

            // Theme
            SettingsLabel(icon = Icons.Default.Palette, label = "Theme")
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppTheme.values().forEach { theme ->
                    val selected = settings.theme == theme
                    FilterChip(
                        selected = selected,
                        onClick = { onThemeChange(theme) },
                        label = { Text(theme.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Accent Color
            SettingsLabel(icon = Icons.Default.ColorLens, label = "Accent Color")
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AccentColor.values().forEach { accent ->
                    val selected = settings.accentColor == accent
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(accent.hex))
                            .then(
                                if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
                                else Modifier.border(2.dp, Color.Transparent, CircleShape)
                            )
                            .clickable { onAccentColorChange(accent) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Contacts ─────────────────────────────────────────────────────
        SettingsSection(title = "Contacts") {

            SettingsToggleRow(
                icon = Icons.Default.SortByAlpha,
                title = "Sort by first name",
                subtitle = if (settings.sortByFirstName) "Currently: First name first" else "Currently: Last name first",
                checked = settings.sortByFirstName,
                onCheckedChange = onSortOrderChange
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SettingsToggleRow(
                icon = Icons.Default.Phone,
                title = "Show phone number in list",
                subtitle = "Display phone number below contact name",
                checked = settings.showPhoneNumberInList,
                onCheckedChange = onShowPhoneChange
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SettingsToggleRow(
                icon = Icons.Default.DeleteForever,
                title = "Confirm before delete",
                subtitle = "Show dialog before deleting contacts",
                checked = settings.confirmBeforeDelete,
                onCheckedChange = onConfirmDeleteChange
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── About ────────────────────────────────────────────────────────
//        SettingsSection(title = "About") {
//            SettingsInfoRow(
//                icon = Icons.Default.Info,
//                title = "Version",
//                value = "1.0.0"
//            )
//            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
//            SettingsInfoRow(
//                icon = Icons.Default.Storage,
//                title = "Storage",
//                value = "Room DB (local)"
//            )
//        }
    }
}

// ← REPLACE the isDefaultDialerApp function
private fun isDefaultDialerApp(context: Context): Boolean {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as android.telecom.TelecomManager
            context.packageName == telecomManager.defaultDialerPackage
        } else {
            false
        }
    } catch (e: Exception) {
        false
    }
}

// ← REPLACE the openDefaultAppSettings function
private fun requestDefaultDialer(
    context: Context,
    launcher: androidx.activity.result.ActivityResultLauncher<Intent>
) {
    Log.e("DEFAULT_DIALER", "requestDefaultDialer() called")

    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Log.e("DEFAULT_DIALER", "Android 10+ detected")

            val roleManager = context.getSystemService(RoleManager::class.java)

            if (roleManager == null) {
                Log.e("DEFAULT_DIALER", "❌ RoleManager is NULL")
                return
            }

            Log.e(
                "DEFAULT_DIALER",
                "Role available: ${roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)}"
            )
            Log.e(
                "DEFAULT_DIALER",
                "Role held: ${roleManager.isRoleHeld(RoleManager.ROLE_DIALER)}"
            )

            if (!roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)) {
                Log.e("DEFAULT_DIALER", "❌ ROLE_DIALER is NOT available on this device")
                return
            }

            if (roleManager.isRoleHeld(RoleManager.ROLE_DIALER)) {
                Log.e("DEFAULT_DIALER", "ℹ️ App is already default dialer")
                return
            }

            Log.e("DEFAULT_DIALER", "✅ Launching ROLE_DIALER system dialog")
            launcher.launch(
                roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
            )

        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Log.e("DEFAULT_DIALER", "Android 6–9 detected")

            val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                putExtra(
                    TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME,
                    context.packageName
                )
            }

            Log.e("DEFAULT_DIALER", "✅ Launching legacy default dialer dialog")
            launcher.launch(intent)

        } else {
            Log.e("DEFAULT_DIALER", "❌ Android version < 6, not supported")
        }

    } catch (e: Exception) {
        Log.e("DEFAULT_DIALER", "🔥 Exception while requesting default dialer", e)
    }
}

// ← ADD THIS: Legacy method for Android 6-9
private fun openLegacyDefaultAppSettings(
    context: Context,
    launcher: androidx.activity.result.ActivityResultLauncher<Intent>
) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(android.telecom.TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
            intent.putExtra(
                android.telecom.TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME,
                context.packageName
            )
            launcher.launch(intent)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        openManualDefaultAppSettings(context, launcher)
    }
}

// ← ADD THIS: Manual fallback
private fun openManualDefaultAppSettings(
    context: Context,
    launcher: androidx.activity.result.ActivityResultLauncher<Intent>
) {
    try {
        // Try opening app settings
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = android.net.Uri.parse("package:${context.packageName}")
        }
        launcher.launch(intent)

        android.widget.Toast.makeText(
            context,
            "Please set as default in 'Set as default' or 'Open by default' section",
            android.widget.Toast.LENGTH_LONG
        ).show()
    } catch (e: Exception) {
        e.printStackTrace()
        android.widget.Toast.makeText(
            context,
            "Could not open settings. Please set manually in system settings.",
            android.widget.Toast.LENGTH_LONG
        ).show()
    }
}

// ── Sub-components ────────────────────────────────────────────────────────────

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp)
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsLabel(icon: ImageVector, label: String) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SettingsInfoRow(
    icon: ImageVector,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}