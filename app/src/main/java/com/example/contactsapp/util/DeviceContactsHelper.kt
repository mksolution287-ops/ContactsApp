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
            val resolver: ContentResolver = context.contentResolver

            val cursor = resolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
                    ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP
                ),
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )

            cursor?.use { c ->
                val idCol     = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameCol   = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numCol    = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val photoCol  = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
                val updateCol = c.getColumnIndex(
                    ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP
                )

                while (c.moveToNext()) {
                    val contactId = c.getString(idCol)
                    val name      = c.getString(nameCol) ?: "Unknown"
                    val number    = c.getString(numCol) ?: ""
                    val photoUri = c.getString(photoCol)

                    val systemUpdatedAt =
                        if (updateCol != -1) c.getLong(updateCol)
                        else System.currentTimeMillis()

                    val email = getEmailForContact(resolver, contactId)

                    contacts.add(
                        Contact(
                            name = name,
                            phoneNumber = number,
                            email = email,
                            profileImageUri = photoUri,
                            deviceContactId = contactId,
                            lastUpdatedAt = systemUpdatedAt // ✅ KEY
                        )
                    )
                }
            }

            contacts
        }

        private fun getEmailForContact(
            resolver: ContentResolver,
            contactId: String
        ): String {
            var email = ""

            val cursor = resolver.query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Email.DATA),
                "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
                arrayOf(contactId),
                null
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    val col = it.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA)
                    email = it.getString(col) ?: ""
                }
            }

            return email
        }
}
