package com.mktech.contactsapp.data.model


data class ResolvedCallLog(
    val id: Long,
    val phoneNumber: String,
    val callType: CallType,
    val timestamp: Long,
    val durationSeconds: Int,
    val profileImageUri: String?,
    val contactName: String
) {
    fun getFormattedDuration(): String {
        if (durationSeconds == 0) return ""
        val mins = durationSeconds / 60
        val secs = durationSeconds % 60
        return if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
    }

    fun getInitials(): String {
        return contactName.split(" ")
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")
            .ifEmpty { "?" }
    }
}