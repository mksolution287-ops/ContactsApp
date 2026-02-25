package com.example.contactsapp.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.contactsapp.CallActionReceiver
import com.example.contactsapp.IncomingCallActivity

object CallNotificationManager {

    private const val CHANNEL_ID  = "incoming_call_channel"
    private const val NOTIF_ID    = 1001

    fun showIncomingCallNotification(
        context: Context,
        callerName: String,
        phoneNumber: String
    ) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        createChannel(nm)

        // Full-screen intent → opens IncomingCallActivity
        val fullScreenIntent = Intent(context, IncomingCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION
            putExtra("caller_name", callerName)
            putExtra("phone_number", phoneNumber)
        }
        val fullScreenPi = PendingIntent.getActivity(
            context, 0, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val answerIntent = Intent(CallActionReceiver.ACTION_ANSWER).apply {
            setClass(context, CallActionReceiver::class.java)  // ← explicit
            putExtra("caller_name", callerName)                // ← pass caller info too
            putExtra("phone_number", phoneNumber)
        }

        val answerPi = PendingIntent.getBroadcast(
            context, 1, answerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

// Decline action — explicit component
        val declineIntent = Intent(CallActionReceiver.ACTION_DECLINE).apply {
            setClass(context, CallActionReceiver::class.java)  // ← explicit
        }
        val declinePi = PendingIntent.getBroadcast(
            context, 2, declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle("Incoming Call")
            .setContentText("$callerName • $phoneNumber")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(fullScreenPi, true)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(android.R.drawable.ic_menu_call, "Answer", answerPi)
            .addAction(android.R.drawable.ic_delete, "Decline", declinePi)
            .build()

        nm.notify(NOTIF_ID, notification)
    }

    // Add this function alongside showIncomingCallNotification:

    fun showOngoingCallNotification(
        context: Context,
        callerName: String,
        phoneNumber: String
    ) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createChannel(nm)

        // Tap notification → open active call screen
        val openPi = PendingIntent.getActivity(
            context, 3,
            Intent(context, IncomingCallActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("caller_name", callerName)
                putExtra("phone_number", phoneNumber)
                putExtra("is_outgoing", true)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Hang up from notification
        val hangUpPi = PendingIntent.getBroadcast(
            context, 4,
            Intent(CallActionReceiver.ACTION_DECLINE).apply {
                setClass(context, CallActionReceiver::class.java)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle("Ongoing Call")
            .setContentText("$callerName • $phoneNumber")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setContentIntent(openPi)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(android.R.drawable.ic_delete, "Hang Up", hangUpPi)
            .build()

        nm.notify(NOTIF_ID, notification)
    }

    fun cancelNotification(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
        nm.cancel(NOTIF_ID)
    }

    private fun createChannel(nm: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Incoming Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows incoming call notifications"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            nm.createNotificationChannel(channel)
        }
    }
}