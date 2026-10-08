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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
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
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.mktech.contactsapp.ui.components.BannerAd
import com.mktech.contactsapp.ui.components.NativeAdCard
import com.mktech.contactsapp.ui.screens.LanguagePickerContent
import com.mktech.contactsapp.util.AdManager
import com.mktech.contactsapp.util.AppOpenAdManager
import kotlinx.coroutines.launch
import android.provider.Settings
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.runtime.saveable.rememberSaveable

// ── Permission metadata ───────────────────────────────────────────────────────

private data class PermissionInfo(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val color: Color
)

private data class OnboardingPage(
    val image: Int,   // drawable resource id, e.g. R.drawable.onboarding_image_1
    val titleLine1: Int,
    val titleLine2: Int,
    val subtitle: Int
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

    private var dialIntentNumber by mutableStateOf<String?>(null)
    private var shouldAutoCall by mutableStateOf(false)

    @RequiresApi(Build.VERSION_CODES.M)
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        registerPhoneAccount(this)
        super.onCreate(savedInstanceState)
        val activity = this

        val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)

        // ── Read skip_splash flag but do NOT clear it here ────────────────
        // It will be cleared inside the LaunchedEffect AFTER it has been used,
        // so that activity.recreate() (triggered by language change) also skips splash.
        val skipSplash = prefs.getBoolean("skip_splash", false)

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

                // ── Handle external dial intent ───────────────────────────
                LaunchedEffect(dialIntentNumber) {
                    val number = dialIntentNumber
                    if (!number.isNullOrEmpty()) {
                        viewModel.dialPadSetNumber(number)
                        if (shouldAutoCall) {
                            delay(200)
                            if (androidx.core.content.ContextCompat.checkSelfPermission(
                                    activity,
                                    android.Manifest.permission.CALL_PHONE
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            ) {
                                viewModel.makeCall(activity, number)
                            }
                            shouldAutoCall = false
                        }
                        dialIntentNumber = null
                    }
                }
                val permissions = buildList {
                    add(Manifest.permission.POST_NOTIFICATIONS)
                    add(Manifest.permission.READ_CONTACTS)
                    add(Manifest.permission.CALL_PHONE)
                    add(Manifest.permission.READ_CALL_LOG)
                    add(Manifest.permission.WRITE_CONTACTS)
                    add(Manifest.permission.WRITE_CALL_LOG)
                    add(Manifest.permission.READ_PHONE_STATE)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                        add(Manifest.permission.READ_MEDIA_IMAGES)
                    else
                        add(Manifest.permission.READ_EXTERNAL_STORAGE)
                }

                val permsState = rememberMultiplePermissionsState(permissions)

                var showSplash    by remember { mutableStateOf(!skipSplash) }
                var defaultSkipped by remember { mutableStateOf(false) }

                val languageAlreadyChosen = remember { prefs.contains("language") }
                var languagePicked by remember { mutableStateOf(languageAlreadyChosen) }

                var onboardingDone by remember { mutableStateOf(prefs.getBoolean("onboarding_done", false)) }
                val isDefaultDialer = remember { mutableStateOf(isDefaultDialer(this)) }

                val defaultDialerLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) {
                    isDefaultDialer.value = isDefaultDialer(this)
                    if (isDefaultDialer.value) defaultSkipped = true
                }

                LaunchedEffect(Unit) {
                    if (!skipSplash) {
                        delay(2800)
                    }
                    showSplash = false

                    // ── Clear the flag HERE, after we've already used it ──
                    // This way a recreate() triggered by language change also
                    // reads skip_splash=true before we clear it.
                    prefs.edit().putBoolean("skip_splash", false).apply()

                    // Interstitial ad after splash
                    AdManager.immediateInterstitialAd(activity)
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
                    !onboardingDone                            -> "onboarding"
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

                            "language" -> LanguagePickerScreen(
                                onLanguageSelected = { language ->
                                    viewModel.setLanguage(language)
                                    // Set skip_splash BEFORE recreate() so the next
                                    // onCreate reads it as true and skips the splash
                                    prefs.edit().putBoolean("skip_splash", true).apply()
                                    // 🚫 Skip the interstitial that would otherwise fire after splash
                                    AdManager.skipNextInterstitial = true
                                    activity.recreate()
                                }
                            )

                            "onboarding" -> OnboardingScreen(
                                onFinish = {
                                    prefs.edit().putBoolean("onboarding_done", true).apply()
                                    onboardingDone = true
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
                                    isDefaultDialer.value = isDefaultDialer(activity)
                                    defaultSkipped = true
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDialIntent(intent)
    }

    private fun handleDialIntent(intent: Intent?) {
        val number = extractNumber(intent) ?: return
        dialIntentNumber = number
        shouldAutoCall = intent?.action == Intent.ACTION_CALL
    }

    private fun extractNumber(intent: Intent?): String? {
        if (intent == null) return null

        intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER)?.let {
            return sanitizeNumber(it)
        }

        intent.data?.let { uri ->
            when (uri.scheme) {
                "tel", "telprompt" -> {
                    uri.schemeSpecificPart?.let { return sanitizeNumber(it) }
                }
                "sip" -> {
                    uri.schemeSpecificPart
                        ?.substringBefore("@")
                        ?.let { return sanitizeNumber(it) }
                }
                "sms", "smsto" -> {
                    uri.schemeSpecificPart?.let { return sanitizeNumber(it) }
                }
            }
        }

        intent.dataString?.let { raw ->
            if (raw.startsWith("tel:")) {
                return sanitizeNumber(raw.removePrefix("tel:"))
            }
        }

        return null
    }
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

@RequiresApi(Build.VERSION_CODES.M)
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
    var clean = PhoneNumberUtils.stripSeparators(number).trim()

    return when {
        clean.length == 10                               -> "+91$clean"
        clean.length == 11 && clean.startsWith("0")     -> "+91${clean.substring(1)}"
        clean.length == 12 && clean.startsWith("91")    -> "+$clean"
        else                                             -> clean
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LANGUAGE PICKER SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LanguagePickerScreen(
    onLanguageSelected: (AppLanguage) -> Unit
) {
    LanguagePickerContent(onLanguageSelected = onLanguageSelected)
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
        "POST_NOTIFICATIONS"    to PermissionInfo(Icons.Default.Notifications, stringResource(R.string.perm_notifications_title), stringResource(R.string.perm_notifications_desc),  Color(0xFFFFCC80)),
        "READ_CONTACTS"         to PermissionInfo(Icons.Default.Contacts,     stringResource(R.string.perm_contacts_title),      stringResource(R.string.perm_contacts_desc),      Color(0xFF4FC3F7)),
        "CALL_PHONE"            to PermissionInfo(Icons.Default.Call,          stringResource(R.string.perm_calls_title),         stringResource(R.string.perm_calls_title),         Color(0xFF81C784)),
        "READ_CALL_LOG"         to PermissionInfo(Icons.Default.History,       stringResource(R.string.perm_call_history_title),  stringResource(R.string.perm_call_history_desc),   Color(0xFFFFB74D)),
        "READ_PHONE_STATE"      to PermissionInfo(Icons.Default.PhoneAndroid,  stringResource(R.string.perm_phone_state_title),   stringResource(R.string.perm_phone_state_desc),    Color(0xFFCE93D8)),
        "READ_MEDIA_IMAGES"     to PermissionInfo(Icons.Default.Image,         stringResource(R.string.perm_photos_title),        stringResource(R.string.perm_photos_desc),         Color(0xFFF48FB1)),
        "READ_EXTERNAL_STORAGE" to PermissionInfo(Icons.Default.Image,         stringResource(R.string.perm_storage_title),       stringResource(R.string.perm_storage_desc),        Color(0xFFF48FB1)),
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
        BannerAd(modifier = Modifier.requiredWidth(LocalConfiguration.current.screenWidthDp.dp))
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
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val screeningAvailable = remember { isCallScreeningRoleAvailable(context) }
    var isDialerGranted by remember { mutableStateOf(isDialerRoleGranted(context)) }
    var isCallScreeningGranted by remember { mutableStateOf(isCallScreeningRoleGranted(context)) }

// User switched these off in the UI (the system role itself can't be revoked from code)
    var dialerOptOut by rememberSaveable { mutableStateOf(false) }
    var screeningOptOut by rememberSaveable { mutableStateOf(false) }

// What the switches show
    val dialerOn = isDialerGranted && !dialerOptOut
    val screeningOn = isCallScreeningGranted && !screeningOptOut

    val allGranted = dialerOn && (screeningOn || !screeningAvailable)

    fun refreshRoles() {
        isDialerGranted = isDialerRoleGranted(context)
        isCallScreeningGranted = isCallScreeningRoleGranted(context)
        // If another app took the role, reset the opt-out so the state is clean
        if (!isDialerGranted) dialerOptOut = false
        if (!isCallScreeningGranted) screeningOptOut = false
    }

    // Re-check whenever the user comes back (e.g. from system Default apps settings)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshRoles()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val dialerRoleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { refreshRoles() }

    val callScreeningLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { refreshRoles() }

    // requestDialerRole() and requestCallScreeningRole() stay exactly as they are

    fun requestDialerRole() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val rm = context.getSystemService(RoleManager::class.java)
                if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_DIALER) && !rm.isRoleHeld(RoleManager.ROLE_DIALER)) {
                    dialerRoleLauncher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_DIALER))
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                dialerRoleLauncher.launch(
                    Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                        putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.packageName)
                    }
                )
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun requestCallScreeningRole() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val rm = context.getSystemService(RoleManager::class.java)
                if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) && !rm.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
                    callScreeningLauncher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(48.dp))

        // ── Illustration (approximated — swap for a real asset if you have one) ──

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Box(
                modifier = Modifier
                    .size(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(painterResource(R.drawable.default_screen_image), null, modifier = Modifier.fillMaxSize())
            }
        }

        Spacer(Modifier.height(28.dp))

        Text(
            text = stringResource(R.string.set_as_default_1),
            color = Color.Black,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.set_as_default_desc),
            color = Color.Black.copy(0.7f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(28.dp))

        DefaultAppToggleRow(
            icon = Icons.Default.Call,
            iconBg = Color(0xFF1976D2),
            title = stringResource(R.string.default_phone_app),
            subtitle = stringResource(R.string.default_phone_app_desc),
            checked = dialerOn,
            onCheckedChange = { wantOn ->
                if (wantOn) {
                    if (isDialerGranted) dialerOptOut = false   // already default, just switch back on
                    else requestDialerRole()
                } else {
                    dialerOptOut = true                         // disable in place, no navigation
                }
            }
        )

        if (screeningAvailable) {
            Spacer(Modifier.height(14.dp))
            DefaultAppToggleRow(
                icon = Icons.Default.Shield,
                iconBg = Color(0xFF43A047),
                title = stringResource(R.string.caller_id_spam_app),
                subtitle = stringResource(R.string.caller_id_spam_app_desc),
                checked = screeningOn,
                onCheckedChange = { wantOn ->
                    if (wantOn) {
                        if (isCallScreeningGranted) screeningOptOut = false
                        else requestCallScreeningRole()
                    } else {
                        screeningOptOut = true
                    }
                }
            )
        }

        Spacer(Modifier.height(28.dp))

        val privacyUrl = "https://sites.google.com/view/mksolutioncontactdilaerprivacy/home"
        val annotatedPrivacy = buildAnnotatedString {
            append(stringResource(R.string.privacy_policy_desc))
            pushStringAnnotation(tag = "privacy", annotation = privacyUrl)
            withStyle(SpanStyle(color = Color(0xFF64B5F6))) { append(stringResource(R.string.privacy_policy)) }
            pop()
        }
        ClickableText(
            text = annotatedPrivacy,
            style = LocalTextStyle.current.copy(
                color = Color.Black.copy(alpha = 0.7f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            ),
            modifier = Modifier.fillMaxWidth(),
            onClick = { offset ->
                annotatedPrivacy.getStringAnnotations("privacy", offset, offset).firstOrNull()?.let {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it.item)))
                }
            }
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { if (allGranted) onSetDefault() },
            enabled = allGranted,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1976D2),
                disabledContainerColor = Color(0xFF1976D2).copy(alpha = 0.35f)
            )
        ) {
            Text(
                stringResource(R.string.continue_btn),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (allGranted) Color.White else Color.White.copy(alpha = 0.5f)
            )
        }

        Spacer(Modifier.height(10.dp))
        BannerAd(Modifier.requiredWidth(LocalConfiguration.current.screenWidthDp.dp))
    }
}

@Composable
private fun DefaultAppToggleRow(
    icon: ImageVector,
    iconBg: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier.size(44.dp).background(iconBg, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Color.Black.copy(0.7f), fontSize = 12.sp, lineHeight = 16.sp)
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF1976D2)
            )
        )
    }
}

// ── Role-check helpers ───────────────────────────────────────────────────────

private fun isDialerRoleGranted(context: Context): Boolean {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.getSystemService(RoleManager::class.java)
                ?.isRoleHeld(RoleManager.ROLE_DIALER) == true
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val tm = context.getSystemService(TelecomManager::class.java)
            context.packageName == tm?.defaultDialerPackage
        } else false
    } catch (e: Exception) { false }
}

private fun isCallScreeningRoleAvailable(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
    return try {
        context.getSystemService(RoleManager::class.java)
            ?.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) == true
    } catch (e: Exception) { false }
}

private fun isCallScreeningRoleGranted(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
    return try {
        val rm = context.getSystemService(RoleManager::class.java) ?: return false
        rm.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) &&
                rm.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
    } catch (e: Exception) { false }
}

private fun openDefaultAppsSettings(context: Context) {
    try {
        context.startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
    } catch (e: Exception) {
        try { context.startActivity(Intent(Settings.ACTION_SETTINGS)) } catch (_: Exception) {}
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ONBOARDING SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OnboardingScreen(onFinish: () -> Unit) {
    val pages = remember {
        listOf(
            OnboardingPage(
                image = R.drawable.onboarding_image_1,
                titleLine1 = R.string.onboarding_title_1_line1,
                titleLine2 = R.string.onboarding_title_1_line2,
                subtitle = R.string.onboarding_subtitle_1
            ),
            OnboardingPage(
                image = R.drawable.onboarding_image_2,
                titleLine1 = R.string.onboarding_title_2_line1,
                titleLine2 = R.string.onboarding_title_2_line2,
                subtitle = R.string.onboarding_subtitle_2
            ),
            OnboardingPage(
                image = R.drawable.onboarding_image_3,
                titleLine1 = R.string.onboarding_title_3_line1,
                titleLine2 = R.string.onboarding_title_3_line2,
                subtitle = R.string.onboarding_subtitle_3
            )
        )
    }

    val pagerState = rememberPagerState(
        pageCount = { pages.size }
    )

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {

        // Background + Pager + Bottom Controls
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            OnboardingBackground(
                modifier = Modifier.fillMaxSize()
            )

            Column(
                modifier = Modifier.fillMaxSize()
            ) {

                // Pager takes only the available space
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) { index ->

                    OnboardingPageContent(
                        pages[index]
                    )
                }

                // Bottom dots + button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 28.dp,
                            vertical = 32.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    // Page indicators
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        pages.indices.forEach { index ->

                            val isActive =
                                pagerState.currentPage == index

                            Box(
                                modifier = Modifier
                                    .height(6.dp)
                                    .width(
                                        if (isActive) {
                                            22.dp
                                        } else {
                                            6.dp
                                        }
                                    )
                                    .clip(
                                        RoundedCornerShape(3.dp)
                                    )
                                    .background(
                                        if (isActive) {
                                            Color(0xFF1976D2)
                                        } else {
                                            Color(0xFFD8E2EC)
                                        }
                                    )
                            )
                        }
                    }

                    // Next / Get Started
                    val isLastPage =
                        pagerState.currentPage == pages.lastIndex

                    Text(
                        text = if (isLastPage) {
                            "Get Started"
                        } else {
                            "Next"
                        },
                        color = Color(0xFF0D1B2A),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {

                            if (isLastPage) {

                                onFinish()

                            } else {

                                scope.launch {
                                    pagerState.animateScrollToPage(
                                        pagerState.currentPage + 1
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }

        // Native ad gets its own space
        NativeAdCard(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        )
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(72.dp))

        Box(modifier = Modifier.size(340.dp), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(page.image),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(Modifier.height(40.dp))

        Text(stringResource(page.titleLine1), color = Color(0xFF0D1B2A), fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(stringResource(page.titleLine2), color = Color(0xFF1976D2), fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)

        Spacer(Modifier.height(14.dp))

        Text(
            text = stringResource(page.subtitle),
            color = Color(0xFF78889B),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun OnboardingBackground(modifier: Modifier = Modifier) {
    Box(modifier = modifier.background(Color.White)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // ── Soft blue blobs in the corners ──────────────────────────
            // Top-left: tight, more saturated blob
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF9EC5F0).copy(alpha = 0.55f), Color.Transparent),
                    center = Offset(w * 0.02f, h * 0.02f),
                    radius = w * 0.55f
                ),
                radius = w * 0.55f,
                center = Offset(w * 0.02f, h * 0.02f)
            )
            // Top-left: larger, paler wash spreading right/down
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFDCEBFB).copy(alpha = 0.7f), Color.Transparent),
                    center = Offset(w * 0.15f, h * 0.02f),
                    radius = w * 0.9f
                ),
                radius = w * 0.9f,
                center = Offset(w * 0.15f, h * 0.02f)
            )
            // Right-middle, pale wash
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFE3EEFC).copy(alpha = 0.8f), Color.Transparent),
                    center = Offset(w * 1.05f, h * 0.42f),
                    radius = w * 0.55f
                ),
                radius = w * 0.55f,
                center = Offset(w * 1.05f, h * 0.42f)
            )
            // Bottom-right, pale wash
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFDCEBFB).copy(alpha = 0.7f), Color.Transparent),
                    center = Offset(w * 1.05f, h * 1.0f),
                    radius = w * 0.75f
                ),
                radius = w * 0.75f,
                center = Offset(w * 1.05f, h * 1.0f)
            )
            // Bottom-left: tight, more saturated blob (mirrors top-left)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF9EC5F0).copy(alpha = 0.6f), Color.Transparent),
                    center = Offset(w * -0.02f, h * 1.02f),
                    radius = w * 0.6f
                ),
                radius = w * 0.6f,
                center = Offset(w * -0.02f, h * 1.02f)
            )

            // ── Dot-grid accents ─────────────────────────────────────────
            val dotColor = Color(0xFFB9C9DC).copy(alpha = 0.6f)
            val dotRadius = 1.6.dp.toPx()
            val dotSpacing = 12.dp.toPx()

            fun drawDotGrid(originX: Float, originY: Float, cols: Int, rows: Int) {
                for (row in 0 until rows) {
                    for (col in 0 until cols) {
                        drawCircle(
                            color = dotColor,
                            radius = dotRadius,
                            center = Offset(originX + col * dotSpacing, originY + row * dotSpacing)
                        )
                    }
                }
            }
            
            drawDotGrid(w * 0.82f, h * 0.045f, 4, 4)
            drawDotGrid(w * 0.05f, h * 0.80f, 4, 4)

            // ── Thin wavy line near the bottom ────────────────────────────
            val wavePath = Path().apply {
                moveTo(0f, h * 0.86f)
                cubicTo(w * 0.25f, h * 0.80f, w * 0.4f, h * 0.94f, w * 0.65f, h * 0.88f)
                cubicTo(w * 0.85f, h * 0.83f, w * 0.95f, h * 0.92f, w, h * 0.87f)
            }
            drawPath(
                path = wavePath,
                color = Color(0xFFB9C9DC).copy(alpha = 0.4f),
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
}