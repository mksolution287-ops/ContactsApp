package com.example.contactsapp.util

import android.content.Context
import android.provider.CallLog.Calls
import android.provider.ContactsContract
import android.util.Log
import com.example.contactsapp.data.model.CallLog
import com.example.contactsapp.data.model.CallType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeviceCallLogHelper(private val context: Context) {

    suspend fun loadDeviceCallLogs(limit: Int = 150): List<CallLog> = withContext(Dispatchers.IO) {
        val logs = mutableListOf<CallLog>()

        try {
            val cursor = context.contentResolver.query(
                Calls.CONTENT_URI,
                arrayOf(
                    Calls.CACHED_NAME,
                    Calls.NUMBER,
                    Calls.TYPE,
                    Calls.DATE,
                    Calls.DURATION
                ),
                null,
                null,
                "${Calls.DATE} DESC"
            )

            if (cursor == null) {
                Log.e("CallLogHelper", "Cursor is null - permission denied?")
                return@withContext logs
            }

            cursor.use {
                Log.d("CallLogHelper", "Found ${it.count} call log entries")

                val nameIdx = it.getColumnIndex(Calls.CACHED_NAME)
                val numIdx  = it.getColumnIndex(Calls.NUMBER)
                val typeIdx = it.getColumnIndex(Calls.TYPE)
                val dateIdx = it.getColumnIndex(Calls.DATE)
                val durIdx  = it.getColumnIndex(Calls.DURATION)

                var count = 0
                while (it.moveToNext() && count < limit) {
                    val number   = it.getString(numIdx) ?: continue
                    val name     = it.getString(nameIdx) ?: number
                    val type     = when (it.getInt(typeIdx)) {
                        Calls.INCOMING_TYPE -> CallType.INCOMING
                        Calls.OUTGOING_TYPE -> CallType.OUTGOING
                        else                -> CallType.MISSED
                    }
                    val date     = it.getLong(dateIdx)
                    val duration = it.getInt(durIdx)

                    val profileImage = getProfileImageForNumber(number)

                    logs.add(CallLog(
                        contactName     = name,
                        phoneNumber     = number,
                        callType        = type,
                        timestamp       = date,
                        durationSeconds = duration,
                        profileImageUri = profileImage
                    ))
                    count++
                }
            }

            Log.d("CallLogHelper", "Successfully loaded ${logs.size} call logs")
        } catch (e: Exception) {
            Log.e("CallLogHelper", "Error loading call logs", e)
        }

        logs
    }

    private fun getProfileImageForNumber(number: String): String? {
        try {
            val uri = ContactsContract.PhoneLookup.CONTENT_FILTER_URI
            val lookupUri = android.net.Uri.withAppendedPath(uri, android.net.Uri.encode(number))

            val cursor = context.contentResolver.query(
                lookupUri,
                arrayOf(ContactsContract.PhoneLookup.PHOTO_URI),
                null, null, null
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    val photoIdx = it.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_URI)
                    return it.getString(photoIdx)
                }
            }
        } catch (e: Exception) {
            Log.e("CallLogHelper", "Error getting profile image for $number", e)
        }
        return null
    }
}