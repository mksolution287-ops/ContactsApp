package com.example.contactsapp.data.local

import androidx.room.*
import com.example.contactsapp.data.model.Contact
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    
    @Query("SELECT * FROM contacts ORDER BY name COLLATE NOCASE ASC")
    fun getAllContacts(): Flow<List<Contact>>
    
    @Query("SELECT * FROM contacts WHERE isFavorite = 1 ORDER BY name COLLATE NOCASE ASC")
    fun getFavoriteContacts(): Flow<List<Contact>>
    
    @Query("SELECT * FROM contacts WHERE id = :id")
    suspend fun getContactById(id: Long): Contact?

    @Query("SELECT * FROM contacts WHERE phoneNumber = :number LIMIT 1")
    suspend fun getContactByPhone(number: String): Contact?
    
    @Query("SELECT * FROM contacts WHERE name LIKE '%' || :query || '%' OR phoneNumber LIKE '%' || :query || '%' OR email LIKE '%' || :query || '%' ORDER BY name COLLATE NOCASE ASC")
    fun searchContacts(query: String): Flow<List<Contact>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: Contact): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<Contact>)

    @Query("SELECT * FROM contacts WHERE phoneNumber = :number LIMIT 1")
    fun observeContactByPhone(number: String): Flow<Contact?>
    
//    @Update
//    suspend fun updateContact(contact: Contact)
@Query("""
    UPDATE contacts SET
        name = :name,
        phoneNumber = :phone,
        email = :email,
        profileImageUri = :image,
        isFavorite = :favorite,
        updatedAt = :updatedAt
    WHERE id = :id
""")
suspend fun updateContact(
    id: Long,
    name: String,
    phone: String,
    email: String,
    image: String?,
    favorite: Boolean,
    updatedAt: Long,
)

    @Query("SELECT * FROM contacts WHERE deviceContactId = :deviceId LIMIT 1")
    suspend fun getByDeviceContactId(deviceId: String?): Contact?
    
    @Delete
    suspend fun deleteContact(contact: Contact)
    
    @Query("DELETE FROM contacts WHERE id = :id")
    suspend fun deleteContactById(id: Long)
    
    @Query("DELETE FROM contacts")
    suspend fun deleteAllContacts()
    
    @Query("UPDATE contacts SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Long, isFavorite: Boolean)
}
