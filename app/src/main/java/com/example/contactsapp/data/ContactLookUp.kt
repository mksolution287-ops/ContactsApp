package com.example.contactsapp.data

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract

object ContactLookup {

    fun getCallerName(context: Context, phoneNumber: String): String {
        if (phoneNumber.isBlank()) return "Unknown"

        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            val cursor = context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null, null, null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    it.getString(it.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME))
                } else null
            } ?: cleanNumber(phoneNumber)
        } catch (e: Exception) {
            cleanNumber(phoneNumber)
        }
    }

    // Strips tel: prefix if present
    internal fun cleanNumber(raw: String) =
        raw.removePrefix("tel:").trim().ifBlank { "Unknown" }
}