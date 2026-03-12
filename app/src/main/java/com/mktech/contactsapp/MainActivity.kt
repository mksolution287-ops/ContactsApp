package com.mktech.contactsapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.mktech.contactsapp.ui.navigation.ContactNavigation
import com.mktech.contactsapp.ui.theme.ContactsAppTheme
import com.mktech.contactsapp.ui.viewmodel.ContactViewModel
import com.mktech.contactsapp.ui.viewmodel.ContactViewModelFactory
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.mktech.contactsapp.data.model.AppLanguage
import com.mktech.contactsapp.util.LocaleHelper
import kotlinx.coroutines.delay
import android.telephony.PhoneNumberUtils

// ── Permission metadata ───────────────────────────────────────────────────────

private data class PermissionInfo(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val color: Color
)

private val permissionMeta = mapOf(
    "READ_CONTACTS"         to PermissionInfo(Icons.Default.Contacts,    "Contacts",       "View and manage your contacts",         Color(0xFF4FC3F7)),
    "CALL_PHONE"            to PermissionInfo(Icons.Default.Call,         "Phone Calls",    "Make and manage phone calls",           Color(0xFF81C784)),
    "READ_CALL_LOG"         to PermissionInfo(Icons.Default.History,      "Call History",   "Access your call history",              Color(0xFFFFB74D)),
    "READ_PHONE_STATE"      to PermissionInfo(Icons.Default.PhoneAndroid, "Phone State",    "Monitor call status and state",         Color(0xFFCE93D8)),
    "READ_MEDIA_IMAGES"     to PermissionInfo(Icons.Default.Image,        "Photos",         "Set profile pictures for contacts",     Color(0xFFF48FB1)),
    "READ_EXTERNAL_STORAGE" to PermissionInfo(Icons.Default.Image,        "Storage",        "Access photos for contact avatars",     Color(0xFFF48FB1)),
    "POST_NOTIFICATIONS"    to PermissionInfo(Icons.Default.Notifications,"Notifications",  "Get notified about calls and messages", Color(0xFFFFCC80)),
)

private val hiddenFromCards = setOf("WRITE_CONTACTS", "WRITE_CALL_LOG")
private val optionalPermissionNames = setOf(
    "READ_MEDIA_IMAGES",
    "READ_EXTERNAL_STORAGE",
    "POST_NOTIFICATIONS"
)
private val notRequiredForApp = setOf(
    Manifest.permission.READ_EXTERNAL_STORAGE,
    Manifest.permission.READ_MEDIA_IMAGES,
    Manifest.permission.POST_NOTIFICATIONS
)

// ─────────────────────────────────────────────────────────────────────────────
// MAIN ACTIVITY
// ─────────────────────────────────────────────────────────────────────────────

class MainActivity : BaseActivity() {

    // Shared state between onCreate and onNewIntent
    private var dialIntentNumber by mutableStateOf<String?>(null)
    private var shouldAutoCall by mutableStateOf(false)

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        registerPhoneAccount(this)
        super.onCreate(savedInstanceState)
        val activity = this

        // ── Read flags BEFORE setContent, at Activity level ──────────────
        val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val skipSplash = prefs.getBoolean("skip_splash", false)
        if (skipSplash) {
            prefs.edit().putBoolean("skip_splash", false).apply()  // clear immediately
        }

        // ── Extract number from external dial intent ──────────────────────
        val intentNumber = when (intent?.action) {
            Intent.ACTION_DIAL, Intent.ACTION_CALL -> {
                intent.data?.schemeSpecificPart
                    ?.let { android.net.Uri.decode(it) }
                    ?.replace(" ", "")
            }
            else -> null
        }

        setContent {
            val app = application as ContactsApplication
            val viewModel: ContactViewModel = viewModel(
                factory = ContactViewModelFactory(
                    application, app.contactRepository,
                    app.callLogRepository, app.settingsRepository
                )
            )
            val settings by viewModel.settings.collectAsState()


            ContactsAppTheme(appTheme = settings.theme, accentColor = settings.accentColor) {

                // ── Handle external dial intent ───────────────────────
                LaunchedEffect(dialIntentNumber) {
                    val number = dialIntentNumber
                    if (!number.isNullOrEmpty()) {
                        viewModel.dialPadSetNumber(number)
                        if (shouldAutoCall) {
                            delay(200)
                            // Only auto-call if permission is granted
                            if (androidx.core.content.ContextCompat.checkSelfPermission(
                                    activity,
                                    android.Manifest.permission.CALL_PHONE
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            ) {
                                viewModel.makeCall(activity, number)
                            }
                            // If no permission, number is still pre-filled in dialer
                            shouldAutoCall = false
                        }
                        dialIntentNumber = null
                    }
                }

                val permissions = buildList {
                    add(Manifest.permission.READ_CONTACTS)
                    add(Manifest.permission.CALL_PHONE)
                    add(Manifest.permission.READ_CALL_LOG)
                    add(Manifest.permission.WRITE_CONTACTS)
                    add(Manifest.permission.WRITE_CALL_LOG)
                    add(Manifest.permission.READ_PHONE_STATE)
                    add(Manifest.permission.POST_NOTIFICATIONS)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                        add(Manifest.permission.READ_MEDIA_IMAGES)
                    else
                        add(Manifest.permission.READ_EXTERNAL_STORAGE)
                }

                val permsState = rememberMultiplePermissionsState(permissions)

                // ── skipSplash is now read at Activity level, safe to use here ──
                var showSplash by remember { mutableStateOf(!skipSplash) }

                var defaultSkipped   by remember { mutableStateOf(false) }

//                // Check if language has been chosen before (first launch detection)
//                val prefs = activity.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

                val languageAlreadyChosen = remember {
                    prefs.contains("language")
                }
                var languagePicked by remember { mutableStateOf(languageAlreadyChosen) }

                val isDefaultDialer = remember { mutableStateOf(isDefaultDialer(this)) }

                val defaultDialerLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) {
                    isDefaultDialer.value = isDefaultDialer(this)
                    if (isDefaultDialer.value) defaultSkipped = true
                }

//                LaunchedEffect(Unit) {
//                    delay(2800)
//                    showSplash = false
//                }
                LaunchedEffect(Unit) {
                    if (!skipSplash) {
                        delay(2800)
                    }
                    showSplash = false  // instant if skipSplash, delayed otherwise
                }

                LaunchedEffect(permsState.allPermissionsGranted) {
                    if (permsState.allPermissionsGranted) {
                        viewModel.loadDeviceContacts()
                        viewModel.loadDeviceCallLogs()
                    }
                }

                val requiredPermissionsGranted = permsState.permissions
                    .filter { it.permission !in notRequiredForApp }
                    .all { it.status.isGranted }

                val currentScreen = when {
                    showSplash                                 -> "splash"
                    !languagePicked                            -> "language"
                    !isDefaultDialer.value && !defaultSkipped -> "set_default"
                    !requiredPermissionsGranted                -> "permission"
                    else                                       -> "main"
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF050A14)
                ) {
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            fadeIn(tween(600)) togetherWith fadeOut(tween(400))
                        },
                        label = "screen_transition"
                    ) { screen ->
                        when (screen) {
                            "splash" -> SplashScreen()

                            "language" -> LanguagePickerScreen(        // ← NEW
                                onLanguageSelected = { language ->
                                    viewModel.setLanguage(language)
                                    prefs.edit().putBoolean("skip_splash", true).apply()
//                                    languagePicked = true
                                    activity.recreate()
                                }
                            )

                            "permission" -> {
                                var deniedCount by remember { mutableStateOf(0) }
                                PermissionScreen(
                                    denied = permsState.permissions
                                        .filter { !it.status.isGranted }
                                        .map { it.permission.substringAfterLast(".") }
                                        .filter { it !in hiddenFromCards },
                                    optionalPermissions = optionalPermissionNames,
                                    onRequest = {
                                        val allRequiredDenied = permsState.permissions
                                            .filter { it.permission !in notRequiredForApp }
                                            .none { it.status.isGranted }
                                        if (deniedCount >= 2 && allRequiredDenied) {
                                            activity.startActivity(
                                                Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                    data = android.net.Uri.fromParts("package", activity.packageName, null)
                                                }
                                            )
                                        } else {
                                            deniedCount++
                                            permsState.launchMultiplePermissionRequest()
                                        }
                                    }
                                )
                            }

                            "set_default" -> SetDefaultScreen(
                                onSetDefault = {
                                    try {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                            val roleManager = activity.getSystemService(RoleManager::class.java)
                                                ?: return@SetDefaultScreen
                                            if (!roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)) return@SetDefaultScreen
                                            if (roleManager.isRoleHeld(RoleManager.ROLE_DIALER)) return@SetDefaultScreen
                                            defaultDialerLauncher.launch(
                                                roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
                                            )
                                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                            defaultDialerLauncher.launch(
                                                Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                                                    putExtra(
                                                        TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME,
                                                        activity.packageName
                                                    )
                                                }
                                            )
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                },
                                onSkip = { defaultSkipped = true }
                            )

                            else -> {
                                val navController = rememberNavController()
                                ContactNavigation(
                                    navController = navController,
                                    viewModel = viewModel,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    //for making calls from outside apps
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDialIntent(intent)
    }

//    private fun handleDialIntent(intent: Intent?) {
//        val number = extractNumber(intent) ?: return
//        dialIntentNumber = number
//        shouldAutoCall = intent?.action == Intent.ACTION_CALL
//    }
private fun handleDialIntent(intent: Intent?) {

    val number = extractNumber(intent) ?: return

    dialIntentNumber = number
    shouldAutoCall = intent?.action == Intent.ACTION_CALL
}

//    private fun extractNumber(intent: Intent?): String? {
//        if (intent?.action !in listOf(Intent.ACTION_DIAL, Intent.ACTION_CALL)) return null
//        return intent?.data?.schemeSpecificPart
//            ?.let { android.net.Uri.decode(it) }
//            ?.replace(" ", "")
//            ?.replace("-", "")
//            ?.replace("(", "")
//            ?.replace(")", "")
//            ?.trim()
//            .takeIf { !it.isNullOrEmpty() }
//    }
private fun extractNumber(intent: Intent?): String? {

    if (intent == null) return null

    // 1️⃣ Phone number from extras
    intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER)?.let {
        return sanitizeNumber(it)
    }

    // 2️⃣ Handle URI schemes
    intent.data?.let { uri ->

        when (uri.scheme) {

            "tel", "telprompt" -> {
                uri.schemeSpecificPart?.let {
                    return sanitizeNumber(it)
                }
            }

            "sip" -> {
                uri.schemeSpecificPart
                    ?.substringBefore("@")
                    ?.let { return sanitizeNumber(it) }
            }

            "sms", "smsto" -> {
                uri.schemeSpecificPart?.let {
                    return sanitizeNumber(it)
                }
            }
        }
    }

    // 3️⃣ Some apps send dataString
    intent.dataString?.let { raw ->
        if (raw.startsWith("tel:")) {
            return sanitizeNumber(raw.removePrefix("tel:"))
        }
    }

    return null
}

//    override fun attachBaseContext(newBase: Context) {
//        val prefs = newBase.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
//        val langCode = prefs.getString("language", AppLanguage.SYSTEM.code) ?: AppLanguage.SYSTEM.code
//        val language = AppLanguage.values().find { it.code == langCode } ?: AppLanguage.SYSTEM
//        super.attachBaseContext(LocaleHelper.applyLanguage(newBase, language))
//    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun isDefaultDialer(context: Context): Boolean {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val tm = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            context.packageName == tm.defaultDialerPackage
        } else false
    } catch (e: Exception) { false }
}

private fun registerPhoneAccount(context: Context) {
    val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
    val handle = PhoneAccountHandle(
        ComponentName(context, CallConnectionService::class.java),
        "ContactsAppAccount"
    )
    val phoneAccount = PhoneAccount.builder(handle, "Contacts App")
        .setCapabilities(PhoneAccount.CAPABILITY_CALL_PROVIDER)
        .build()
    telecomManager.registerPhoneAccount(phoneAccount)
}

private fun sanitizeNumber(number: String): String {

    var clean = PhoneNumberUtils.stripSeparators(number)

    // Remove spaces and symbols
    clean = clean.trim()

    // Case 1: 10 digit Indian number
    if (clean.length == 10) {
        clean = "+91$clean"
    }

    // Case 2: Starts with 0
    else if (clean.length == 11 && clean.startsWith("0")) {
        clean = "+91${clean.substring(1)}"
    }

    // Case 3: 12 digits starting with 91
    else if (clean.length == 12 && clean.startsWith("91")) {
        clean = "+$clean"
    }

    return clean
}

// ─────────────────────────────────────────────────────────────────────────────
// LANGUAGE PICKER SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LanguagePickerScreen(
    onLanguageSelected: (AppLanguage) -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf(AppLanguage.ENGLISH) }

    LaunchedEffect(Unit) { delay(100); visible = true }

    val headerAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(700), label = "header"
    )
    val headerOffset by animateFloatAsState(
        targetValue = if (visible) 0f else -40f,
        animationSpec = tween(700, easing = EaseOut), label = "header_offset"
    )
    val buttonAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(700, delayMillis = 600), label = "button"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0A1628), Color(0xFF050A14))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(72.dp))

            // Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .alpha(headerAlpha)
                    .offset(y = headerOffset.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF1565C0), Color(0xFF42A5F5))
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Language,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(Modifier.height(28.dp))

                Text(
                    text = "Choose Language",
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "Select your preferred language\nto continue",
                    color = Color(0xFF90CAF9),
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }

            Spacer(Modifier.height(32.dp))

            // Language list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .alpha(headerAlpha),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                val languages = AppLanguage.values().filter { it != AppLanguage.SYSTEM }
                itemsIndexed(languages) { index, lang ->
                    val cardAlpha by animateFloatAsState(
                        targetValue = if (visible) 1f else 0f,
                        animationSpec = tween(500, delayMillis = 200 + index * 80),
                        label = "lang_card_$index"
                    )
                    val cardOffset by animateFloatAsState(
                        targetValue = if (visible) 0f else 30f,
                        animationSpec = tween(500, delayMillis = 200 + index * 80, easing = EaseOut),
                        label = "lang_offset_$index"
                    )

                    val isSelected = selectedLanguage == lang

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(cardAlpha)
                            .offset(y = cardOffset.dp)
                            .clickable { selectedLanguage = lang },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) Color(0xFF1565C0).copy(alpha = 0.3f)
                        else Color(0xFF0D1B2A),
                        border = if (isSelected)
                            androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF42A5F5))
                        else null
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = lang.nativeName,
                                    color = Color.White,
                                    fontSize = 16.sp,
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

            // Continue button
            Column(
                modifier = Modifier
                    .alpha(buttonAlpha)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = { onLanguageSelected(selectedLanguage) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1976D2)
                    )
                ) {
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Continue",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SPLASH SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SplashScreen() {
    val infiniteTransition = rememberInfiniteTransition(label = "splash_pulse")

    val ringScale by infiniteTransition.animateFloat(
        initialValue = 0.85f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = EaseInOut), RepeatMode.Reverse),
        label = "ring"
    )
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f, targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(1400, easing = EaseInOut), RepeatMode.Reverse),
        label = "ring_alpha"
    )

    var visible by remember { mutableStateOf(false) }
    val logoScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.3f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 200f),
        label = "logo_scale"
    )
    val logoAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(600),
        label = "logo_alpha"
    )
    val textAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(800, delayMillis = 400),
        label = "text_alpha"
    )
    val taglineAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(800, delayMillis = 700),
        label = "tagline_alpha"
    )

    LaunchedEffect(Unit) { delay(100); visible = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF0A1628), Color(0xFF050A14)),
                    radius = 1200f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(340.dp)
                .scale(ringScale)
                .alpha(ringAlpha)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF1565C0).copy(alpha = 0.4f), Color.Transparent)
                    ),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(240.dp)
                .scale(ringScale * 0.9f)
                .alpha(ringAlpha * 0.8f)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF42A5F5).copy(alpha = 0.3f), Color.Transparent)
                    ),
                    CircleShape
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .scale(logoScale)
                    .alpha(logoAlpha)
                    .size(110.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF1976D2), Color(0xFF42A5F5))
                        ),
                        shape = RoundedCornerShape(30.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(52.dp)
                )
            }

            Spacer(Modifier.height(32.dp))

            Text(
                text = stringResource(R.string.app_name),
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                modifier = Modifier.alpha(textAlpha)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.tagline),
                color = Color(0xFF90CAF9),
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.3.sp,
                modifier = Modifier.alpha(taglineAlpha)
            )

            Spacer(Modifier.height(80.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.alpha(taglineAlpha)
            ) {
                repeat(3) { index ->
                    val dotAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.2f, targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            tween(600, delayMillis = index * 200, easing = EaseInOut),
                            RepeatMode.Reverse
                        ),
                        label = "dot_$index"
                    )
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .alpha(dotAlpha)
                            .background(Color(0xFF42A5F5), CircleShape)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PERMISSION SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@SuppressLint("SuspiciousIndentation")
@Composable
private fun PermissionScreen(
    denied: List<String>,
    optionalPermissions: Set<String> = emptySet(),
    onRequest: () -> Unit
) {

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(100); visible = true }

    val headerAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(700), label = "header"
    )
    val headerOffset by animateFloatAsState(
        targetValue = if (visible) 0f else -40f,
        animationSpec = tween(700, easing = EaseOut), label = "header_offset"
    )
    val buttonAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(700, delayMillis = 800), label = "button"
    )
    val taglineAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(800, delayMillis = 700), label = "tagline_alpha"
    )
    val context = LocalContext.current

    val translatedMeta = mapOf(
        "READ_CONTACTS"         to PermissionInfo(Icons.Default.Contacts,     stringResource(R.string.perm_contacts_title),      stringResource(R.string.perm_contacts_desc),      Color(0xFF4FC3F7)),
        "CALL_PHONE"            to PermissionInfo(Icons.Default.Call,          stringResource(R.string.perm_calls_title),         stringResource(R.string.perm_calls_title),         Color(0xFF81C784)),
        "READ_CALL_LOG"         to PermissionInfo(Icons.Default.History,       stringResource(R.string.perm_call_history_title),      stringResource(R.string.perm_call_history_desc),      Color(0xFFFFB74D)),
        "READ_PHONE_STATE"      to PermissionInfo(Icons.Default.PhoneAndroid,  stringResource(R.string.perm_phone_state_title),   stringResource(R.string.perm_phone_state_desc),   Color(0xFFCE93D8)),
        "READ_MEDIA_IMAGES"     to PermissionInfo(Icons.Default.Image,         stringResource(R.string.perm_photos_title),        stringResource(R.string.perm_photos_desc),        Color(0xFFF48FB1)),
        "READ_EXTERNAL_STORAGE" to PermissionInfo(Icons.Default.Image,         stringResource(R.string.perm_storage_title),       stringResource(R.string.perm_storage_desc),       Color(0xFFF48FB1)),
        "POST_NOTIFICATIONS"    to PermissionInfo(Icons.Default.Notifications, stringResource(R.string.perm_notifications_title), stringResource(R.string.perm_notifications_desc), Color(0xFFFFCC80)),
    )

        Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0A1628), Color(0xFF050A14))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(72.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .alpha(headerAlpha)
                    .offset(y = headerOffset.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF1565C0), Color(0xFF42A5F5))
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(Modifier.height(28.dp))

                Text(
                    text = stringResource(R.string.allow_access),
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = stringResource(R.string.permission_subtitle),
                    color = Color(0xFF90CAF9),
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }

            Spacer(Modifier.height(40.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 52.dp)
            ) {
                itemsIndexed(denied) { index, permission ->
                    val meta = translatedMeta[permission] ?: PermissionInfo(
                        Icons.Default.Lock, permission, "Required permission", Color(0xFF90CAF9)
                    )
                    val isOptional = permission in optionalPermissions

                    val cardAlpha by animateFloatAsState(
                        targetValue = if (visible) 1f else 0f,
                        animationSpec = tween(600, delayMillis = 300 + index * 120),
                        label = "card_$index"
                    )
                    val cardOffset by animateFloatAsState(
                        targetValue = if (visible) 0f else 30f,
                        animationSpec = tween(600, delayMillis = 300 + index * 120, easing = EaseOut),
                        label = "card_offset_$index"
                    )

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(cardAlpha)
                            .offset(y = cardOffset.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF0D1B2A),
                        tonalElevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(
                                        meta.color.copy(alpha = 0.15f),
                                        RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = meta.icon,
                                    contentDescription = null,
                                    tint = meta.color,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = meta.title,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = meta.description,
                                    color = Color(0xFF607D8B),
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isOptional)
                                    Color(0xFF37474F).copy(alpha = 0.4f)
                                else
                                    meta.color.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = if (isOptional) stringResource(R.string.optional)
                                    else stringResource(R.string.required),
                                    color = if (isOptional) Color(0xFF90A4AE) else meta.color,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 10.dp)
                            .alpha(taglineAlpha),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.permission_terms_prefix),
                            color = Color(0xFF607D8B),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        Row(horizontalArrangement = Arrangement.Center) {
                            Text(
                                text = stringResource(R.string.terms_and_conditions),
                                color = Color(0xFF0466FA),
                                fontSize = 12.sp,
                                modifier = Modifier.clickable {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse("https://sites.google.com/view/mksolutionappstermsandcondtion/home"))
                                    )
                                }
                            )
                            Text(text = "  ·  ", color = Color(0xFF607D8B), fontSize = 12.sp)
                            Text(
                                text = stringResource(R.string.privacy_policy),
                                color = Color(0xFF0466FA),
                                fontSize = 12.sp,
                                modifier = Modifier.clickable {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse("https://sites.google.com/view/mksolutioncontactdilaerprivacy/home"))
                                    )
                                }
                            )
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(8.dp))
                    Column(
                        modifier = Modifier.alpha(buttonAlpha),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(
                            onClick = onRequest,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.grant_permissions),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = stringResource(R.string.data_privacy_note),
                            color = Color(0xFF455A64),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SET AS DEFAULT SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SetDefaultScreen(
    onSetDefault: () -> Unit,
    onSkip: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(100); visible = true }

    val headerAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(700), label = "alpha"
    )
    val headerOffset by animateFloatAsState(
        targetValue = if (visible) 0f else -40f,
        animationSpec = tween(700, easing = EaseOut), label = "offset"
    )
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(1200, easing = EaseInOut), RepeatMode.Reverse),
        label = "pulse"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 0.45f,
        animationSpec = infiniteRepeatable(tween(1200, easing = EaseInOut), RepeatMode.Reverse),
        label = "pulse_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0A1628), Color(0xFF050A14))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(80.dp))

            Box(
                modifier = Modifier.alpha(headerAlpha).offset(y = headerOffset.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size((100 * pulseScale).dp)
                        .alpha(pulseAlpha)
                        .background(
                            Brush.radialGradient(colors = listOf(Color(0xFF1976D2), Color.Transparent)),
                            CircleShape
                        )
                )
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .background(
                            Brush.linearGradient(colors = listOf(Color(0xFF1565C0), Color(0xFF42A5F5))),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(46.dp)
                    )
                }
            }

            Spacer(Modifier.height(40.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.alpha(headerAlpha)
            ) {
                Text(
                    stringResource(R.string.set_as_default),
                    color = Color.White, fontSize = 30.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.set_default_subtitle),
                    color = Color(0xFF90CAF9), fontSize = 15.sp,
                    textAlign = TextAlign.Center, lineHeight = 22.sp
                )
            }

            Spacer(Modifier.height(44.dp))

            val features = listOf(
                Triple(Icons.Default.CallEnd,  Color(0xFF81C784), stringResource(R.string.one_tap_call_controls)),
                Triple(Icons.Default.Contacts, Color(0xFFFFB74D), stringResource(R.string.unified_contacts)),
            )

            val featAlpha by animateFloatAsState(
                targetValue = if (visible) 1f else 0f,
                animationSpec = tween(600, delayMillis = 200), label = "feat"
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.alpha(featAlpha)
            ) {
                features.forEachIndexed { index, (icon, color, title) ->
                    val cardAlpha by animateFloatAsState(
                        targetValue = if (visible) 1f else 0f,
                        animationSpec = tween(500, delayMillis = 200 + index * 100),
                        label = "card_$index"
                    )
                    val cardOffset by animateFloatAsState(
                        targetValue = if (visible) 0f else 24f,
                        animationSpec = tween(500, delayMillis = 200 + index * 100, easing = EaseOut),
                        label = "offset_$index"
                    )
                    Surface(
                        modifier = Modifier.fillMaxWidth().alpha(cardAlpha).offset(y = cardOffset.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF0D1B2A)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(color.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
                            }
                            Spacer(Modifier.width(14.dp))
                            Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            val ctaAlpha by animateFloatAsState(
                targetValue = if (visible) 1f else 0f,
                animationSpec = tween(700, delayMillis = 700), label = "cta"
            )

            Column(
                modifier = Modifier.alpha(ctaAlpha).padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onSetDefault,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                ) {
                    Icon(Icons.Default.PhoneAndroid, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.set_as_default_dialer), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                TextButton(onClick = onSkip) {
                    Text(stringResource(R.string.skip_for_now), color = Color(0xFF546E7A), fontSize = 14.sp)
                }
            }
        }
    }
}