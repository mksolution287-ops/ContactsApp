package com.example.contactsapp.data.repository

import com.example.contactsapp.data.local.ContactDao
import com.example.contactsapp.data.model.Contact
import kotlinx.coroutines.flow.Flow

class ContactRepository(
    private val contactDao: ContactDao
) {
    
    fun getAllContacts(): Flow<List<Contact>> {
        return contactDao.getAllContacts()
    }
    
    fun getFavoriteContacts(): Flow<List<Contact>> {
        return contactDao.getFavoriteContacts()
    }
    
    suspend fun getContactById(id: Long): Contact? {
        return contactDao.getContactById(id)
    }
    
    fun searchContacts(query: String): Flow<List<Contact>> {
        return contactDao.searchContacts(query)
    }
    
    suspend fun insertContact(contact: Contact): Long {
        return contactDao.insertContact(contact)
    }

    suspend fun insertContacts(incoming: List<Contact>) {
        incoming.forEach { systemContact ->
            val local = contactDao.getByDeviceContactId(systemContact.deviceContactId)

            when {
                local == null -> {
                    // New contact from system
                    contactDao.insertContact(systemContact)
                }

                systemContact.lastUpdatedAt > local.lastUpdatedAt -> {
                    // System contact is newer → update local
                    contactDao.updateContact(
                        id = local.id,
                        name = systemContact.name,
                        phone = systemContact.phoneNumber,
                        email = systemContact.email,
                        image = systemContact.profileImageUri,
                        favorite = local.isFavorite, // preserve user choice
                        updatedAt = systemContact.lastUpdatedAt
                    )
                }

                else -> {
                    // ❌ Local contact is newer → IGNORE system
                    // This is the bug fix
                }
            }
        }
    }
    
//    suspend fun updateContact(contact: Contact) {
//        contactDao.updateContact(contact.copy(updatedAt = System.currentTimeMillis()))
//    }
suspend fun updateContact(contact: Contact) {
    contactDao.updateContact(
        id = contact.id,
        name = contact.name,
        phone = contact.phoneNumber,
        email = contact.email,
        image = contact.profileImageUri,
        favorite = contact.isFavorite,
        updatedAt = System.currentTimeMillis()
    )
}
    
    suspend fun deleteContact(contact: Contact) {
        contactDao.deleteContact(contact)
    }

//    suspend fun getContactByPhone(number: String): Contact? {
//        return contactDao.getContactByPhone(number)
//    }
suspend fun getContactByPhone(number: String): Contact? {
    val normalized = normalizePhone(number)
    return contactDao.getContactByPhoneLoose(normalized)
}
    
    suspend fun deleteContactById(id: Long) {
        contactDao.deleteContactById(id)
    }

    fun normalizePhone(number: String): String {
        return number.filter { it.isDigit() }.takeLast(10)
    }
    suspend fun toggleFavorite(id: Long, isFavorite: Boolean) {
        contactDao.updateFavoriteStatus(id, isFavorite)
    }
}
