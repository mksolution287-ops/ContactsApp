package com.example.contactsapp.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.telecom.TelecomManager

object CallHelper {

    fun isDefaultDialer(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val telecom = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                context.packageName == telecom.defaultDialerPackage
            } else false
        } catch (e: Exception) { false }
    }

    fun makeCall(context: Context, phoneNumber: String) {
        val cleanNumber = phoneNumber.trim()
        if (cleanNumber.isBlank()) return

        if (isDefaultDialer(context)) {
            // App is default — use CALL intent directly
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$cleanNumber")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } else {
            // Not default — hand off to system dialer
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$cleanNumber")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }
}