package com.mktech.contactsapp

import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.util.Log
import androidx.annotation.RequiresApi
import com.mktech.contactsapp.data.CallManager
import com.mktech.contactsapp.data.CallNotificationManager
import com.mktech.contactsapp.data.ContactLookup
import com.mktech.contactsapp.data.repository.SettingsRepository
import com.mktech.contactsapp.ui.overlay.FloatingCallerIdOverlay
import com.mktech.contactsapp.ui.postcall.PostCallActivity
import com.mktech.contactsapp.util.AppOpenAdManager

class CallService : InCallService() {

    private var lastCallerName: String = "Unknown"
    private var lastPhoneNumber: String = ""

    // Guards against endCallCleanup() running twice (it can be triggered
    // from both onStateChanged and onCallRemoved).
    private var notificationCleared = false

    // The call whose UI (activity + notification + overlay) is already shown.
    // Prevents onCallAdded() and onStateChanged() from launching the UI twice
    // for the same call.
    private var uiShownForCall: Call? = null

    private val callCallback = object : Call.Callback() {
        @RequiresApi(Build.VERSION_CODES.P)
        override fun onStateChanged(call: Call, state: Int) {
            CallManager.notifyListeners()
            when (state) {

                Call.STATE_RINGING -> {
                    notificationCleared = false
                    showIncomingCallUI(call)
                }

                // Outgoing call — dialing or connecting
                Call.STATE_DIALING,
                Call.STATE_CONNECTING -> {
                    notificationCleared = false
                    showOutgoingCallUI(call)
                }

                Call.STATE_ACTIVE -> {
                    // Notify listeners so IncomingCallActivity recomposes to ActiveCallScreen
                    CallManager.notifyListeners()
                    FloatingCallerIdOverlay.hide()
                    // Swap the "incoming"/"dialing" notification for the
                    // pinned ongoing-call one (same NOTIF_ID → replaces it).
                    enterForegroundOngoing()
                }

                Call.STATE_DISCONNECTED,
                Call.STATE_DISCONNECTING -> {
                    CallManager.setCall(null)
                    endCallCleanup()
                }
            }
        }
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        AppOpenAdManager.holdSuppression()
        notificationCleared = false
        CallManager.inCallService = this
        call.registerCallback(callCallback)
        CallManager.setCall(call)
        when (call.state) {
            Call.STATE_RINGING             -> showIncomingCallUI(call)
            Call.STATE_DIALING,
            Call.STATE_CONNECTING          -> showOutgoingCallUI(call)
            Call.STATE_ACTIVE              -> showOutgoingCallUI(call)
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        call.unregisterCallback(callCallback)
        if (uiShownForCall === call) uiShownForCall = null
        CallManager.setCall(null)
        CallManager.inCallService = null
        CallManager.resetAudioState()
        endCallCleanup()
        AppOpenAdManager.releaseSuppression()
        PostCallActivity.launch(this, lastCallerName, lastPhoneNumber)
    }

    private fun showIncomingCallUI(call: Call) {
        if (uiShownForCall === call) return
        uiShownForCall = call

        val rawNumber = call.details?.handle?.schemeSpecificPart ?: ""

        val lookedUpName = call.details?.callerDisplayName
            ?.takeIf { it.isNotBlank() }
            ?: ContactLookup.getCallerName(this, rawNumber)
        val callerName = if (lookedUpName.isBlank() || lookedUpName == rawNumber) "Unknown" else lookedUpName

        val phoneNumber = ContactLookup.cleanNumber(rawNumber)

        // Assign first so PostCallActivity always has the right data,
        // even if something below throws.
        lastCallerName = callerName
        lastPhoneNumber = phoneNumber

        val intent = Intent(this, IncomingCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION
            putExtra("caller_name", callerName)
            putExtra("phone_number", phoneNumber)
            putExtra("is_outgoing", false)
        }
        startActivity(intent)
//        CallNotificationManager.showIncomingCallNotification(this, callerName, phoneNumber)

        val notification = CallNotificationManager.buildIncomingCallNotification(this, callerName, phoneNumber)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                CallNotificationManager.NOTIF_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
            )
        } else {
            startForeground(CallNotificationManager.NOTIF_ID, notification)
        }
        maybeShowFloatingCallerId(callerName, phoneNumber, isOutgoing = false)
    }

    private fun showOutgoingCallUI(call: Call) {
        if (uiShownForCall === call) return
        uiShownForCall = call

        val rawNumber = call.details?.handle?.schemeSpecificPart ?: ""

        // Look up name from contacts, fall back to number
        val callerName = call.details?.callerDisplayName
            ?.takeIf { it.isNotBlank() }
            ?: ContactLookup.getCallerName(this, rawNumber)

        val phoneNumber = ContactLookup.cleanNumber(rawNumber)

        // Assign first so PostCallActivity always has the right data,
        // even if something below throws.

        lastCallerName = callerName
        lastPhoneNumber = phoneNumber

        val intent = Intent(this, IncomingCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION
            putExtra("caller_name", callerName)
            putExtra("phone_number", phoneNumber)
            putExtra("is_outgoing", true)
        }
//        startActivity(intent)
//
//        CallNotificationManager.showOngoingCallNotification(this, callerName, phoneNumber)
//        maybeShowFloatingCallerId(callerName, phoneNumber, isOutgoing = true)

        startActivity(intent)

        val notification = CallNotificationManager.buildIncomingCallNotification(this, callerName, phoneNumber)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                CallNotificationManager.NOTIF_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
            )
        } else {
            startForeground(CallNotificationManager.NOTIF_ID, notification)
        }
        maybeShowFloatingCallerId(callerName, phoneNumber, isOutgoing = false)
    }

    /**
     * Wrapped in runCatching: a failure in the floating overlay must never
     * crash the InCallService (that would take down the whole call UI).
     */
    private fun maybeShowFloatingCallerId(callerName: String, phoneNumber: String, isOutgoing: Boolean) {
        runCatching {
            val enabled = SettingsRepository(this).settings.value.showFloatingCallerId
            if (enabled && FloatingCallerIdOverlay.hasPermission(this)) {
                FloatingCallerIdOverlay.show(this, callerName, phoneNumber, isOutgoing)
            }
        }.onFailure {
            Log.e("CallService", "Floating caller ID failed", it)
        }
    }

    /**
     * Promotes the call notification to a pinned, foreground-service
     * "ongoing call" notification once the call is active. Uses the same
     * NOTIF_ID as the incoming/dialing notification, so this replaces it
     * in place rather than stacking a second one.
     */
    @RequiresApi(Build.VERSION_CODES.P)
    private fun enterForegroundOngoing() {
        val notification = CallNotificationManager.buildOngoingCallNotification(
            this, lastCallerName, lastPhoneNumber
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                CallNotificationManager.NOTIF_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
            )
        } else {
            startForeground(CallNotificationManager.NOTIF_ID, notification)
        }
    }

    /**
     * Single source of truth for "the call is over, kill every trace of it".
     * Called from both onStateChanged(DISCONNECTED/DISCONNECTING) and
     * onCallRemoved() — whichever fires first wins, the guard makes the
     * second call a no-op, so hang-up from ANY path (in-app button,
     * notification action, or the other party disconnecting) reliably
     * clears the notification.
     */
    private fun endCallCleanup() {
        // Always reset, even if the cleanup below was already done,
        // so the next call can show its UI.
        uiShownForCall = null

        if (notificationCleared) return
        notificationCleared = true

        // Required to actually remove a notification posted via startForeground();
        // NotificationManager.cancel() alone cannot dismiss it.
        stopForeground(STOP_FOREGROUND_REMOVE)
        // Belt-and-suspenders for the pre-active / non-foreground notification.
        CallNotificationManager.cancelNotification(this)

        sendBroadcast(Intent(CallActionReceiver.ACTION_CALL_ENDED).apply {
            setPackage(packageName)
        })
        FloatingCallerIdOverlay.hide()
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState) {
        super.onCallAudioStateChanged(audioState)
        // Sync external changes (headset plugged in, BT connected, etc.)
        CallManager.onAudioStateChanged(audioState)
    }
}