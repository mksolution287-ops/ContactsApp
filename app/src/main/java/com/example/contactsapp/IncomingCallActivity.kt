package com.example.contactsapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.contactsapp.data.CallManager
import com.example.contactsapp.data.CallNotificationManager
import com.example.contactsapp.ui.theme.ContactsAppTheme
import kotlinx.coroutines.delay

class IncomingCallActivity : ComponentActivity() {

    private val callEndedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = finish()
    }

    private val callStateListener = { invalidateContent() }
    private var recomposeKey = mutableStateOf(0)

    private fun invalidateContent() { recomposeKey.value++ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isOutgoing = intent.getBooleanExtra("is_outgoing", false)


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        // Block back button during call
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Just move to background — notification stays visible
                moveTaskToBack(true)
            }
        })

        val callerName  = intent.getStringExtra("caller_name")  ?: "Unknown"
        val phoneNumber = intent.getStringExtra("phone_number") ?: ""

        val filter = IntentFilter(CallActionReceiver.ACTION_CALL_ENDED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(callEndedReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(callEndedReceiver, filter)
        }

        CallManager.addListener(callStateListener)

        setContent {
            ContactsAppTheme {
                val key by recomposeKey
                key(key) {
                    when {
                        // Outgoing call OR already active → go straight to ActiveCallScreen
                        isOutgoing || CallManager.isActive || CallManager.isOnHold -> {
                            ActiveCallScreen(
                                callerName  = callerName,
                                phoneNumber = phoneNumber,
                                onHangUp = {
                                    CallManager.hangUp()
                                    finish()
                                }
                            )
                        }

                        CallManager.isRinging -> {
                            IncomingCallScreen(
                                callerName  = callerName,
                                phoneNumber = phoneNumber,
                                onAnswer = {
                                    CallManager.answer()
                                    CallNotificationManager.cancelNotification(this)
                                },
                                onDecline = {
                                    CallManager.decline()
                                    CallNotificationManager.cancelNotification(this)
                                    finish()
                                }
                            )
                        }
                        CallManager.isActive || CallManager.isOnHold -> {
                            ActiveCallScreen(
                                callerName  = callerName,
                                phoneNumber = phoneNumber,
                                onHangUp = {
                                    CallManager.hangUp()
                                    finish()
                                }
                            )
                        }
                        else -> {
                            // Call disconnected
                            LaunchedEffect(Unit) { finish() }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        recomposeKey.value++
    }

    override fun onDestroy() {
        super.onDestroy()
        CallManager.removeListener(callStateListener)
        unregisterReceiver(callEndedReceiver)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// INCOMING CALL SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun IncomingCallScreen(
    callerName: String,
    phoneNumber: String,
    onAnswer: () -> Unit,
    onDecline: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0D1B2A), Color(0xFF1B2838), Color(0xFF0D1B2A))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            // Top section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 80.dp)
            ) {
                Text(
                    "Incoming Call",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 13.sp,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.height(40.dp))

                // Pulsing avatar
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .size((100 * pulseScale).dp)
                            .background(Color.White.copy(alpha = 0.06f), CircleShape)
                    )
                    Box(
                        Modifier
                            .size((85 * pulseScale).dp)
                            .background(Color.White.copy(alpha = 0.1f), CircleShape)
                    )
                    Surface(
                        modifier = Modifier.size(76.dp),
                        shape    = CircleShape,
                        color    = Color(0xFF1E3A5F)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Person, null,
                                tint = Color.White,
                                modifier = Modifier.size(42.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(28.dp))
                Text(
                    callerName,
                    color      = Color.White,
                    fontSize   = 30.sp,
                    fontWeight = FontWeight.Bold
                )
                if (phoneNumber.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        phoneNumber,
                        color    = Color.White.copy(alpha = 0.65f),
                        fontSize = 16.sp
                    )
                }
            }

            // Bottom buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 72.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Decline
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FloatingActionButton(
                        onClick          = onDecline,
                        modifier         = Modifier.size(72.dp),
                        shape            = CircleShape,
                        containerColor   = Color(0xFFE53935),
                        contentColor     = Color.White
                    ) {
                        Icon(Icons.Default.CallEnd, "Decline", Modifier.size(34.dp))
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Decline", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                }

                // Answer
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FloatingActionButton(
                        onClick        = onAnswer,
                        modifier       = Modifier.size(72.dp),
                        shape          = CircleShape,
                        containerColor = Color(0xFF43A047),
                        contentColor   = Color.White
                    ) {
                        Icon(Icons.Default.Call, "Answer", Modifier.size(34.dp))
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Answer", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ACTIVE CALL SCREEN  (like Truecaller screenshot)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ActiveCallScreen(
    callerName: String,
    phoneNumber: String,
    onHangUp: () -> Unit
) {
//    var isMuted   by remember { mutableStateOf(false) }
//    var isSpeaker by remember { mutableStateOf(false) }
    var isOnHold   by remember { mutableStateOf(false) }
    var elapsedSec by remember { mutableStateOf(0) }
    var isDialing  by remember { mutableStateOf(CallManager.callState == android.telecom.Call.STATE_DIALING || CallManager.callState == android.telecom.Call.STATE_CONNECTING) }
    var showKeypad by remember { mutableStateOf(false) }
    var keypadInput by remember { mutableStateOf("") }



    var isMuted   by remember { mutableStateOf(CallManager.isMuted()) }
    var isSpeaker by remember { mutableStateOf(CallManager.isSpeakerOn()) }

// Use a State<Int> counter that increments on every CallManager update
    var updateTick by remember { mutableStateOf(0) }

    DisposableEffect(Unit) {
        val listener: () -> Unit = {
            isMuted   = CallManager.isMuted()
            isSpeaker = CallManager.isSpeakerOn()
            updateTick++   // ← forces recomposition
        }
        CallManager.addListener(listener)
        onDispose { CallManager.removeListener(listener) }
    }

    // Update dialing state when call becomes active
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            val state = CallManager.callState
            isDialing = state == android.telecom.Call.STATE_DIALING ||
                    state == android.telecom.Call.STATE_CONNECTING
        }
    }

    // Timer — only runs when active
    LaunchedEffect(isDialing) {
        if (!isDialing) {
            while (true) {
                delay(1000)
                elapsedSec++
            }
        }
    }

    // Call timer
//    LaunchedEffect(Unit) {
//        while (true) {
//            delay(1000)
//            elapsedSec++
//        }
//    }

    val timerText = remember(elapsedSec) {
        val m = elapsedSec / 60
        val s = elapsedSec % 60
        "%02d:%02d".format(m, s)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Top blue section ──────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.52f)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF0D2137), Color(0xFF1565C0), Color(0xFF1E88E5))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 48.dp)
                ) {
                    // App name
                    Text(
                        text       = "ContactsApp",
                        color      = Color.White,
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    // Timer
                    Text(
                        timerText,
                        color    = Color.White.copy(alpha = 0.85f),
                        fontSize = 14.sp
                    )

                    Spacer(Modifier.height(28.dp))

                    // Avatar
                    Surface(
                        modifier = Modifier.size(86.dp),
                        shape    = CircleShape,
                        color    = Color(0xFF1E3A5F)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Person, null,
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Text(
                        callerName,
                        color      = Color.White,
                        fontSize   = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (phoneNumber.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            phoneNumber,
                            color    = Color.White.copy(alpha = 0.7f),
                            fontSize = 15.sp
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // View profile button
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier.clickable { /* open contact */ }
                    ) {
                        Text(
                            "View profile",
                            color    = Color.White,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 28.dp, vertical = 10.dp)
                        )
                    }
                }
            }

            // ── Bottom black section ──────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.48f)
                    .background(Color(0xFF0A0A0A))
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {

                // Row 1: Mute, Keypad, Speaker
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
//                    CallControlButton(
//                        icon    = Icons.Default.MicOff,
//                        label   = "Mute",
//                        active  = isMuted,
//                        onClick = {
//                            CallManager.toggleMute()
//                            isMuted = CallManager.isMuted()
//                        }
//                    )
//                    CallControlButton(
//                        icon    = Icons.Default.Dialpad,
//                        label   = "Keypad",
//                        active  = false,
//                        onClick = { /* show keypad */ }
//                    )
//                    CallControlButton(
//                        icon    = Icons.Default.VolumeUp,
//                        label   = "Speaker",
//                        active  = isSpeaker,
//                        onClick = {
//                            CallManager.toggleSpeaker()
//                            isSpeaker = CallManager.isSpeakerOn()
//                        }
//                    )
                }

                // Row 2: Record, End, Hold/More
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
//                    CallControlButton(
//                        icon    = Icons.Default.MicOff,
//                        label   = "Mute",
//                        active  = isMuted,
//                        onClick = {
//                            CallManager.toggleMute()
//                            isMuted = CallManager.isMuted()
//                        }
//                    )
                    CallControlButton(
                        icon   = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        label  = if (isMuted) "Unmute" else "Mute",
                        active = isMuted,
                        onClick = { CallManager.toggleMute() }  // ← listener updates isMuted automatically
                    )
//                    CallControlButton(
//                        icon    = Icons.Default.FiberManualRecord,
//                        label   = "Record call",
//                        active  = false,
//                        onClick = { /* record */ }
//                    )

                    // End call — large red button
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FloatingActionButton(
                            onClick        = onHangUp,
                            modifier       = Modifier.size(68.dp),
                            shape          = CircleShape,
                            containerColor = Color(0xFFE53935),
                            contentColor   = Color.White
                        ) {
                            Icon(Icons.Default.CallEnd, "End", Modifier.size(30.dp))
                        }
                        Spacer(Modifier.height(6.dp))
                        Text("End", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    }

//                    CallControlButton(
//                        icon    = Icons.Default.VolumeUp,
//                        label   = "Speaker",
//                        active  = isSpeaker,
//                        onClick = {
//                            CallManager.toggleSpeaker()
//                            isSpeaker = CallManager.isSpeakerOn()
//                        }
//                    )
                    CallControlButton(
                        icon   = Icons.Default.VolumeUp,
                        label  = if (isSpeaker) "Earpiece" else "Speaker",
                        active = isSpeaker,
                        onClick = { CallManager.toggleSpeaker() }  // ← listener updates isSpeaker automatically
                    )

                    // More (hold + add call stacked)
//                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
//                        Box(
//                            modifier = Modifier
//                                .size(56.dp)
//                                .clip(CircleShape)
//                                .background(Color(0xFF1C1C1C))
//                                .clickable { /* more options */ },
//                            contentAlignment = Alignment.Center
//                        ) {
//                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
//                                Icon(
//                                    Icons.Default.Pause, null,
//                                    tint     = Color.White,
//                                    modifier = Modifier.size(14.dp)
//                                )
//                                Spacer(Modifier.height(2.dp))
//                                Row {
//                                    Icon(
//                                        Icons.Default.Add, null,
//                                        tint     = Color.White,
//                                        modifier = Modifier.size(12.dp)
//                                    )
//                                    Spacer(Modifier.width(2.dp))
//                                    Icon(
//                                        Icons.Default.Message, null,
//                                        tint     = Color.White,
//                                        modifier = Modifier.size(12.dp)
//                                    )
//                                }
//                            }
//                        }
//                        Spacer(Modifier.height(6.dp))
//                        Text("More", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
//                    }
                }
            }
        }
    }
}

@Composable
private fun CallControlButton(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(if (active) Color(0xFF1E88E5) else Color(0xFF1C1C1C))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
    }
}