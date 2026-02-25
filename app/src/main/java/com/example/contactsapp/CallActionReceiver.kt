package com.example.contactsapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.contactsapp.data.CallManager
import com.example.contactsapp.data.CallNotificationManager
import com.example.contactsapp.data.ContactLookup

class CallActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_ANSWER     = "com.example.contactsapp.ACTION_ANSWER"
        const val ACTION_DECLINE    = "com.example.contactsapp.ACTION_DECLINE"
        const val ACTION_CALL_ENDED = "com.example.contactsapp.ACTION_CALL_ENDED"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_ANSWER -> {
                val callerName  = intent.getStringExtra("caller_name")
                    ?: run {
                        val raw = CallManager.currentCall?.details?.handle?.schemeSpecificPart ?: ""
                        ContactLookup.getCallerName(context, raw)
                    }
                val phoneNumber = intent.getStringExtra("phone_number")
                    ?: ContactLookup.cleanNumber(
                        CallManager.currentCall?.details?.handle?.schemeSpecificPart ?: ""
                    )

                CallManager.answer()

                CallNotificationManager.cancelNotification(context)
                // Open active call screen
                val activityIntent = Intent(context, IncomingCallActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("caller_name", callerName)
                    putExtra("phone_number", phoneNumber)
                    putExtra("from_notification", true)
                }
                context.startActivity(activityIntent)
            }
            ACTION_DECLINE -> {
                CallManager.decline()
                CallNotificationManager.cancelNotification(context)
            }
        }
    }
}