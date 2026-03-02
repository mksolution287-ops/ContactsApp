package com.mktech.contactsapp

import android.content.Intent
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import com.mktech.contactsapp.data.CallManager
import com.mktech.contactsapp.data.CallNotificationManager
import com.mktech.contactsapp.data.ContactLookup

class CallService : InCallService() {

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            CallManager.notifyListeners()
            when (state) {
                Call.STATE_RINGING -> showIncomingCallUI(call)
                // Outgoing call — dialing or active
                Call.STATE_DIALING,
                Call.STATE_CONNECTING -> showOutgoingCallUI(call)

                Call.STATE_ACTIVE -> {
                    // Notify listeners so IncomingCallActivity recomposes to ActiveCallScreen
                    CallManager.notifyListeners()
                }
                Call.STATE_DISCONNECTED,
                Call.STATE_DISCONNECTING -> {
                    CallManager.setCall(null)
                    stopIncomingCallUI()
                }
            }
        }
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        CallManager.inCallService = this
        call.registerCallback(callCallback)
        CallManager.setCall(call)
        when (call.state) {
            Call.STATE_RINGING             -> showIncomingCallUI(call)
            Call.STATE_DIALING,
            Call.STATE_CONNECTING          -> showOutgoingCallUI(call)
            Call.STATE_ACTIVE              -> showOutgoingCallUI(call) // answered immediately
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        call.unregisterCallback(callCallback)
        CallManager.setCall(null)
        CallManager.inCallService = null
        CallManager.resetAudioState()   // ← reset for next call
        stopIncomingCallUI()
    }

    private fun showIncomingCallUI(call: Call) {
        val rawNumber = call.details?.handle?.schemeSpecificPart ?: ""

        // Look up name from contacts, fall back to number
        val callerName = call.details?.callerDisplayName
            ?.takeIf { it.isNotBlank() }
            ?: ContactLookup.getCallerName(this, rawNumber)

        val phoneNumber = ContactLookup.cleanNumber(rawNumber)  // ← make cleanNumber internal

        val intent = Intent(this, IncomingCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION
            putExtra("caller_name", callerName)
            putExtra("phone_number", phoneNumber)
            putExtra("is_outgoing", false)
        }
        startActivity(intent)

        CallNotificationManager.showIncomingCallNotification(this, callerName, phoneNumber)
    }
    private fun showOutgoingCallUI(call: Call) {
        val rawNumber = call.details?.handle?.schemeSpecificPart ?: ""

        // Look up name from contacts, fall back to number
        val callerName = call.details?.callerDisplayName
            ?.takeIf { it.isNotBlank() }
            ?: ContactLookup.getCallerName(this, rawNumber)

        val phoneNumber = ContactLookup.cleanNumber(rawNumber)  // ← make cleanNumber internal

        val intent = Intent(this, IncomingCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION
            putExtra("caller_name", callerName)
            putExtra("phone_number", phoneNumber)
            putExtra("is_outgoing", true)
        }
        startActivity(intent)

        CallNotificationManager.showOngoingCallNotification(this, callerName, phoneNumber)
    }

    private fun stopIncomingCallUI() {
        CallNotificationManager.cancelNotification(this)
        sendBroadcast(Intent(CallActionReceiver.ACTION_CALL_ENDED).apply {
            setPackage(packageName)
        })
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState) {
        super.onCallAudioStateChanged(audioState)
        // Sync external changes (headset plugged in, BT connected, etc.)
        CallManager.onAudioStateChanged(audioState)
    }
}