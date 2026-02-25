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
    
    suspend fun insertContacts(contacts: List<Contact>) {
        contactDao.insertContacts(contacts)
    }
    
    suspend fun updateContact(contact: Contact) {
        contactDao.updateContact(contact.copy(updatedAt = System.currentTimeMillis()))
    }
    
    suspend fun deleteContact(contact: Contact) {
        contactDao.deleteContact(contact)
    }

    suspend fun getContactByPhone(number: String): Contact? {
        return contactDao.getContactByPhone(number)
    }
    
    suspend fun deleteContactById(id: Long) {
        contactDao.deleteContactById(id)
    }
    
    suspend fun toggleFavorite(id: Long, isFavorite: Boolean) {
        contactDao.updateFavoriteStatus(id, isFavorite)
    }
}
