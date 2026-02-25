package com.example.contactsapp.util

import android.content.ContentResolver
import android.content.Context
import android.provider.ContactsContract
import com.example.contactsapp.data.model.Contact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeviceContactsHelper(private val context: Context) {
    
    suspend fun loadDeviceContacts(): List<Contact> = withContext(Dispatchers.IO) {
        val contacts = mutableListOf<Contact>()
        val contentResolver: ContentResolver = context.contentResolver
        
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.PHOTO_URI
            ),
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )
        
        cursor?.use {
            val idColumn = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameColumn = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberColumn = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val photoColumn = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
            
            while (it.moveToNext()) {
                val contactId = it.getString(idColumn)
                val name = it.getString(nameColumn) ?: "Unknown"
                val number = it.getString(numberColumn) ?: ""
                val photoUri = it.getString(photoColumn)
                
                // Get email for this contact
                val email = getEmailForContact(contentResolver, contactId)
                
                contacts.add(
                    Contact(
                        name = name,
                        phoneNumber = number,
                        email = email,
                        profileImageUri = photoUri,
                        deviceContactId = contactId
                    )
                )
            }
        }
        
        contacts
    }
    
    private fun getEmailForContact(contentResolver: ContentResolver, contactId: String): String {
        var email = ""
        
        val emailCursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Email.DATA),
            "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
            arrayOf(contactId),
            null
        )
        
        emailCursor?.use {
            if (it.moveToFirst()) {
                val emailColumn = it.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA)
                email = it.getString(emailColumn) ?: ""
            }
        }
        
        return email
    }
}
