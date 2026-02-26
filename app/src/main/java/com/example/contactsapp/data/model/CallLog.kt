package com.example.contactsapp.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class CallType { INCOMING, OUTGOING, MISSED }

@Entity(tableName = "call_logs",
    indices = [Index(value = ["phoneNumber", "timestamp"], unique = true)])
data class CallLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val contactName: String,
    val phoneNumber: String,
    val callType: CallType,
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Int = 0,
    val profileImageUri: String? = null
) {
    fun getFormattedDuration(): String {
        if (durationSeconds == 0) return ""
        val mins = durationSeconds / 60
        val secs = durationSeconds % 60
        return if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
    }

    fun getInitials(): String {
        return contactName?.split(" ")
            ?.take(2)
            ?.mapNotNull { it.firstOrNull()?.uppercaseChar() }
            ?.joinToString("")
            ?.ifEmpty { "?" } ?: "Unknown"
    }

}
//data class CallLogWithContact(
//    val id: Long,
//    val phoneNumber: String,
//    val timestamp: Long,
//    val callType: CallType,
//    val durationSeconds: Int,
//    val profileImageUri: String?,
//    val contactName: String?
//) {
//    fun getFormattedDuration(): String {
//        if (durationSeconds == 0) return ""
//        val mins = durationSeconds / 60
//        val secs = durationSeconds % 60
//        return if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
//    }
//
//    fun getInitials(): String {
//        return contactName
//            ?.split(" ")
//            ?.take(2)
//            ?.mapNotNull { it.firstOrNull()?.uppercaseChar() }
//            ?.joinToString("")
//            ?.ifEmpty { "?" }
//            ?: "?"
//    }
//}
