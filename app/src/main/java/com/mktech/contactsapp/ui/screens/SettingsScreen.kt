package com.mktech.contactsapp.ui.screens

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.telecom.TelecomManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mktech.contactsapp.R
import com.mktech.contactsapp.data.model.AccentColor
import com.mktech.contactsapp.data.model.AppLanguage
import com.mktech.contactsapp.data.model.AppSettings
import com.mktech.contactsapp.data.model.AppTheme

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onThemeChange: (AppTheme) -> Unit,
    onAccentColorChange: (AccentColor) -> Unit,
    onSortOrderChange: (Boolean) -> Unit,
    onShowPhoneChange: (Boolean) -> Unit,
    onConfirmDeleteChange: (Boolean) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit
) {
    val context = LocalContext.current
    var isDefaultApp by remember { mutableStateOf(isDefaultDialerApp(context)) }
    var showLanguagePicker by remember { mutableStateOf(false) }

    val defaultDialerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        isDefaultApp = isDefaultDialerApp(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {

        // ── Default App ──────────────────────────────────────────────────
        if (!isDefaultApp) {
            SettingsSection(title = stringResource(R.string.default_app)) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { requestDefaultDialer(context, defaultDialerLauncher) }
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
                                text = stringResource(R.string.set_as_default_contacts_app),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = stringResource(R.string.set_as_default_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
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
        SettingsSection(title = stringResource(R.string.appearance)) {

            SettingsLabel(icon = Icons.Default.Palette, label = stringResource(R.string.theme))
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

            SettingsLabel(icon = Icons.Default.ColorLens, label = stringResource(R.string.accent_color))
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

        Spacer(Modifier.height(8.dp))

        // ── Language ─────────────────────────────────────────────────────
        SettingsSection(title = stringResource(R.string.language)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showLanguagePicker = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.language),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = settings.language.nativeName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (showLanguagePicker) {
            AlertDialog(
                onDismissRequest = { showLanguagePicker = false },
                title = {
                    Text(stringResource(R.string.select_language), fontWeight = FontWeight.Bold)
                },
                text = {
                    LazyColumn {
                        item {
                            AppLanguage.values().forEach { lang ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onLanguageChange(lang)
                                            showLanguagePicker = false
                                        }
                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = settings.language == lang,
                                        onClick = {
                                            onLanguageChange(lang)
                                            showLanguagePicker = false
                                        }
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(lang.nativeName, fontWeight = FontWeight.Medium)
                                        if (lang.nativeName != lang.displayName) {
                                            Text(
                                                lang.displayName,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showLanguagePicker = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Contacts ─────────────────────────────────────────────────────
        SettingsSection(title = stringResource(R.string.contacts_section)) {

            SettingsToggleRow(
                icon = Icons.Default.SortByAlpha,
                title = stringResource(R.string.sort_by_first_name),
                subtitle = stringResource(
                    if (settings.sortByFirstName) R.string.sort_first_name_first
                    else R.string.sort_last_name_first
                ),
                checked = settings.sortByFirstName,
                onCheckedChange = onSortOrderChange
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SettingsToggleRow(
                icon = Icons.Default.Phone,
                title = stringResource(R.string.show_phone_in_list),
                subtitle = stringResource(R.string.show_phone_subtitle),
                checked = settings.showPhoneNumberInList,
                onCheckedChange = onShowPhoneChange
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SettingsToggleRow(
                icon = Icons.Default.DeleteForever,
                title = stringResource(R.string.confirm_before_delete),
                subtitle = stringResource(R.string.confirm_delete_subtitle),
                checked = settings.confirmBeforeDelete,
                onCheckedChange = onConfirmDeleteChange
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Legal ─────────────────────────────────────────────────────────
        SettingsSection(title = stringResource(R.string.legal)) {
            SettingsLinkRow(
                icon = Icons.Default.Description,
                title = stringResource(R.string.terms_and_conditions),
                url = "https://sites.google.com/view/mksolutionappstermsandcondtion/home",
                context = context
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            SettingsLinkRow(
                icon = Icons.Default.PrivacyTip,
                title = stringResource(R.string.privacy_policy),
                url = "https://sites.google.com/view/mksolutioncontactdilaerprivacy/home",
                context = context
            )
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun isDefaultDialerApp(context: Context): Boolean {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            context.packageName == telecomManager.defaultDialerPackage
        } else false
    } catch (e: Exception) { false }
}

private fun requestDefaultDialer(
    context: Context,
    launcher: androidx.activity.result.ActivityResultLauncher<Intent>
) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java) ?: return
            if (!roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)) return
            if (roleManager.isRoleHeld(RoleManager.ROLE_DIALER)) return
            launcher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            launcher.launch(
                Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                    putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.packageName)
                }
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
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
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsLinkRow(
    icon: ImageVector,
    title: String,
    url: String,
    context: Context
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
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
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
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