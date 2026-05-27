package com.mktech.contactsapp.ui.screens

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.telecom.TelecomManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.mktech.contactsapp.R
import com.mktech.contactsapp.data.model.AccentColor
import com.mktech.contactsapp.data.model.AppLanguage
import com.mktech.contactsapp.data.model.AppSettings
import com.mktech.contactsapp.data.model.AppTheme
import kotlinx.coroutines.delay

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
                        label = {
                            Text(
                                when (theme) {
                                    AppTheme.SYSTEM -> stringResource(R.string.theme_system)
                                    AppTheme.LIGHT -> stringResource(R.string.theme_light)
                                    AppTheme.DARK -> stringResource(R.string.theme_dark)
                                }
                            )
                        },
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
            LanguagePickerDialog(
                currentLanguage = settings.language,
                onLanguageSelected = { lang ->
                    onLanguageChange(lang)
                    showLanguagePicker = false
                },
                onDismiss = { showLanguagePicker = false }
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

        // ── SIM Preferences ──────────────────────────────────────────────
        SettingsSection(title = stringResource(R.string.sim_preferences)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS)
                        } else {
                            Intent(Settings.ACTION_DATA_ROAMING_SETTINGS)
                        }
                        context.startActivity(intent)
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.SimCard,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.sim_preferences),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = stringResource(R.string.sim_preferences_subtitle),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguagePickerDialog(
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedLanguage by remember { mutableStateOf(currentLanguage) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { delay(80); visible = true }

    val headerAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(600), label = "header"
    )
    val headerOffset by animateFloatAsState(
        targetValue = if (visible) 0f else -30f,
        animationSpec = tween(600, easing = EaseOut), label = "header_offset"
    )
    val buttonAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(600, delayMillis = 500), label = "button"
    )

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxSize(),
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x99000000))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { /* consume */ },
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = Color(0xFF0A1628)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                        .padding(top = 16.dp, bottom = 20.dp)
                ) {
                    // Drag handle
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .size(width = 40.dp, height = 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF263D5C))
                    )

                    Spacer(Modifier.height(20.dp))

                    // Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .alpha(headerAlpha)
                            .offset(y = headerOffset.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF1565C0), Color(0xFF42A5F5))
                                    ),
                                    RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Language,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.select_language),
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.set_as_default_subtitle),
                                color = Color(0xFF90CAF9),
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Language list
                    val languages = AppLanguage.values().filter { it != AppLanguage.SYSTEM }

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .alpha(headerAlpha),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        itemsIndexed(languages) { index, lang ->
                            val cardAlpha by animateFloatAsState(
                                targetValue = if (visible) 1f else 0f,
                                animationSpec = tween(500, delayMillis = 150 + index * 60),
                                label = "card_$index"
                            )
                            val cardOffset by animateFloatAsState(
                                targetValue = if (visible) 0f else 24f,
                                animationSpec = tween(500, delayMillis = 150 + index * 60, easing = EaseOut),
                                label = "offset_$index"
                            )
                            val isSelected = selectedLanguage == lang

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .alpha(cardAlpha)
                                    .offset(y = cardOffset.dp)
                                    .clickable { selectedLanguage = lang },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) Color(0xFF1565C0).copy(alpha = 0.28f)
                                else Color(0xFF0D1B2A),
                                border = if (isSelected)
                                    BorderStroke(1.5.dp, Color(0xFF42A5F5))
                                else null
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = lang.flag,
                                        fontSize = 26.sp,
                                        modifier = Modifier.padding(end = 12.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = lang.nativeName,
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (lang.nativeName != lang.displayName) {
                                            Text(
                                                text = lang.displayName,
                                                color = Color(0xFF607D8B),
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .background(Color(0xFF1976D2), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Confirm button
                    Button(
                        onClick = { onLanguageSelected(selectedLanguage) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .alpha(buttonAlpha),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.confirm),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(buttonAlpha)
                    ) {
                        Text(
                            text = stringResource(R.string.cancel),
                            color = Color(0xFF546E7A),
                            fontSize = 14.sp
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}