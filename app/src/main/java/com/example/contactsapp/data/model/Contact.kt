package com.example.contactsapp.data.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "contacts",
    indices = [
        Index(value = ["phoneNumber"], unique = true),      // ← prevents duplicate phone numbers
        Index(value = ["deviceContactId"], unique = true)   // ← prevents duplicate device contacts
    ])
data class Contact(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phoneNumber: String,
    val email: String = "",
    val profileImageUri: String? = null,
    val isFavorite: Boolean = false,
    val deviceContactId: String? = null, // To sync with device contacts
    @ColumnInfo(name = "last_updated_at")
    val lastUpdatedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) : Parcelable {
    fun getInitials(): String {
        return name.split(" ")
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")
            .ifEmpty { "?" }
    }
    
    fun getDisplayName(): String {
        return name.ifEmpty { "Unknown" }
    }
}
