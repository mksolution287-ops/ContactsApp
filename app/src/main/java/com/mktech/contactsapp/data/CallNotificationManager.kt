//package com.mktech.contactsapp.data
//
//import android.app.NotificationChannel
//import android.app.NotificationManager
//import android.app.PendingIntent
//import android.content.Context
//import android.content.Intent
//import android.os.Build
//import androidx.annotation.RequiresApi
//import androidx.compose.ui.res.stringResource
//import androidx.core.app.NotificationCompat
//import com.mktech.contactsapp.CallActionReceiver
//import com.mktech.contactsapp.IncomingCallActivity
//import com.mktech.contactsapp.R
//import android.app.Notification
//import android.content.pm.ServiceInfo
//import androidx.core.app.Person
//
//object CallNotificationManager {
//
//    private const val CHANNEL_ID  = "incoming_call_channel"
//    const val NOTIF_ID    = 1001
//    fun showIncomingCallNotification(
//        context: Context,
//        callerName: String,
//        phoneNumber: String
//    ) {
//        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE)
//                as NotificationManager
//        createChannel(nm)
//
//        // Full-screen intent → opens IncomingCallActivity
//        val fullScreenIntent = Intent(context, IncomingCallActivity::class.java).apply {
//            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION
//            putExtra("caller_name", callerName)
//            putExtra("phone_number", phoneNumber)
//        }
//        val fullScreenPi = PendingIntent.getActivity(
//            context, 0, fullScreenIntent,
//            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )
//
//        val answerIntent = Intent(CallActionReceiver.ACTION_ANSWER).apply {
//            setClass(context, CallActionReceiver::class.java)  // ← explicit
//            putExtra("caller_name", callerName)                // ← pass caller info too
//            putExtra("phone_number", phoneNumber)
//        }
//
//        val answerPi = PendingIntent.getBroadcast(
//            context, 1, answerIntent,
//            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )
//
//        // Decline action — explicit component
//        val declineIntent = Intent(CallActionReceiver.ACTION_DECLINE).apply {
//            setClass(context, CallActionReceiver::class.java)  // ← explicit
//        }
//        val declinePi = PendingIntent.getBroadcast(
//            context, 2, declineIntent,
//            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )
//
//        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
//            .setSmallIcon(android.R.drawable.ic_menu_call)
//            .setContentTitle(context.getString(R.string.incoming_call))
//            .setContentText("$callerName • $phoneNumber")
//            .setPriority(NotificationCompat.PRIORITY_MAX)
//            .setCategory(NotificationCompat.CATEGORY_CALL)
//            .setFullScreenIntent(fullScreenPi, true)
//            .setOngoing(true)
//            .setAutoCancel(false)
//            .addAction(android.R.drawable.ic_menu_call, context.getString(R.string.answer), answerPi)
//            .addAction(android.R.drawable.ic_delete, context.getString(R.string.decline), declinePi)
//            .build()
//
//        nm.notify(NOTIF_ID, notification)
//    }
//
//    // Add this function alongside showIncomingCallNotification:
//
//    fun showOngoingCallNotification(
//        context: Context,
//        callerName: String,
//        phoneNumber: String
//    ) {
//        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//        createChannel(nm)
//
//        // Tap notification → open active call screen
//        val openPi = PendingIntent.getActivity(
//            context, 3,
//            Intent(context, IncomingCallActivity::class.java).apply {
//                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
//                putExtra("caller_name", callerName)
//                putExtra("phone_number", phoneNumber)
//                putExtra("is_outgoing", true)
//            },
//            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )
//
//        // Hang up from notification
//        val hangUpPi = PendingIntent.getBroadcast(
//            context, 4,
//            Intent(CallActionReceiver.ACTION_DECLINE).apply {
//                setClass(context, CallActionReceiver::class.java)
//            },
//            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )
//
//        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
//            .setSmallIcon(android.R.drawable.ic_menu_call)
//            .setContentTitle("Ongoing Call")
//            .setContentText("$callerName • $phoneNumber")
//            .setPriority(NotificationCompat.PRIORITY_MAX)
//            .setCategory(NotificationCompat.CATEGORY_CALL)
//            .setContentIntent(openPi)
//            .setOngoing(true)
//            .setAutoCancel(false)
//            .addAction(android.R.drawable.ic_delete, "Hang Up", hangUpPi)
//            .build()
//
//        nm.notify(NOTIF_ID, notification)
//    }
//
//    fun cancelNotification(context: Context) {
//        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//        nm.cancel(NOTIF_ID)
//    }
//
//    private fun createChannel(nm: NotificationManager) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val channel = NotificationChannel(
//                CHANNEL_ID,
//                "Incoming Calls",
//                NotificationManager.IMPORTANCE_HIGH
//            ).apply {
//                description = "Shows incoming call notifications"
//                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
//            }
//            nm.createNotificationChannel(channel)
//        }
//    }
//
//    @RequiresApi(Build.VERSION_CODES.P)
//    fun buildOngoingCallNotification(
//        context: Context,
//        callerName: String,
//        phoneNumber: String
//    ): Notification {
//        createChannel(context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
//
//        val person: Person = Person.Builder()
//            .setName(callerName)
//            .build()
//
//        val openPi = PendingIntent.getActivity(
//            context, 3,
//            Intent(context, IncomingCallActivity::class.java).apply {
//                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
//                putExtra("caller_name", callerName)
//                putExtra("phone_number", phoneNumber)
//                putExtra("is_outgoing", true)
//            },
//            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )
//
//        val hangUpPi = PendingIntent.getBroadcast(
//            context, 4,
//            Intent(CallActionReceiver.ACTION_DECLINE).apply {
//                setClass(context, CallActionReceiver::class.java)
//            },
//            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )
//
//        val style = NotificationCompat.CallStyle.forOngoingCall(person, hangUpPi)
//
//        return NotificationCompat.Builder(context, CHANNEL_ID)
//            .setSmallIcon(android.R.drawable.ic_menu_call)
//            .setContentText("$callerName • $phoneNumber")
//            .setStyle(style)
//            .setCategory(NotificationCompat.CATEGORY_CALL)
//            .setContentIntent(openPi)
//            .setOngoing(true)
//            .setAutoCancel(false)
//            .build()
//    }
//}


package com.mktech.contactsapp.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import com.mktech.contactsapp.CallActionReceiver
import com.mktech.contactsapp.IncomingCallActivity

/**
 * Builds the call notifications. These are NOT posted with nm.notify() any more:
 * CallService posts them through startForeground(), which is what makes them
 * non-dismissible (Android 14+ lets users swipe away ongoing notifications that
 * are not backed by a foreground service).
 */
object CallNotificationManager {

    private const val CHANNEL_ID = "incoming_call_channel"
    const val NOTIF_ID = 1001

    // ─────────────────────────────────────────────────────────────────────────
    // INCOMING (ringing)
    // ─────────────────────────────────────────────────────────────────────────
    fun buildIncomingCallNotification(
        context: Context,
        callerName: String,
        phoneNumber: String
    ): Notification {
        createChannel(context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)

        // Tap on notification + full-screen intent → (re)open the call screen
        val openIntent = Intent(context, IncomingCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            putExtra("caller_name", callerName)
            putExtra("phone_number", phoneNumber)
            putExtra("is_outgoing", false)
        }
        val openPi = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val answerPi = PendingIntent.getBroadcast(
            context, 1,
            Intent(CallActionReceiver.ACTION_ANSWER).apply {
                setClass(context, CallActionReceiver::class.java)
                putExtra("caller_name", callerName)
                putExtra("phone_number", phoneNumber)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val declinePi = PendingIntent.getBroadcast(
            context, 2,
            Intent(CallActionReceiver.ACTION_DECLINE).apply {
                setClass(context, CallActionReceiver::class.java)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val caller = Person.Builder()
            .setName(callerName.ifBlank { phoneNumber.ifBlank { "Unknown" } })
            .setImportant(true)
            .build()

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentText(phoneNumber)
            .setStyle(NotificationCompat.CallStyle.forIncomingCall(caller, declinePi, answerPi))
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openPi)            // tap → reopen call screen
            .setFullScreenIntent(openPi, true)   // lock screen / heads-up
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .build()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ONGOING (dialing + active)
    // ─────────────────────────────────────────────────────────────────────────
    fun buildOngoingCallNotification(
        context: Context,
        callerName: String,
        phoneNumber: String
    ): Notification {
        createChannel(context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)

        val person = Person.Builder()
            .setName(callerName.ifBlank { phoneNumber.ifBlank { "Unknown" } })
            .setImportant(true)
            .build()

        val openPi = PendingIntent.getActivity(
            context, 3,
            Intent(context, IncomingCallActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                putExtra("caller_name", callerName)
                putExtra("phone_number", phoneNumber)
                putExtra("is_outgoing", true)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val hangUpPi = PendingIntent.getBroadcast(
            context, 4,
            Intent(CallActionReceiver.ACTION_DECLINE).apply {
                setClass(context, CallActionReceiver::class.java)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentText(phoneNumber)
            .setStyle(NotificationCompat.CallStyle.forOngoingCall(person, hangUpPi))
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openPi)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .build()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────
    fun cancelNotification(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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