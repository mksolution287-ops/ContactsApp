package com.example.contactsapp.data.local

import androidx.room.*
import com.example.contactsapp.data.model.CallLog
import com.example.contactsapp.data.model.CallType
import com.example.contactsapp.data.model.Contact
import com.example.contactsapp.data.model.ResolvedCallLog
import kotlinx.coroutines.flow.Flow

@Dao
interface CallLogDao {

    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC")
    fun getAllCallLogs(): Flow<List<CallLog>>

    @Query("SELECT * FROM call_logs WHERE callType = 'MISSED' ORDER BY timestamp DESC")
    fun getMissedCalls(): Flow<List<CallLog>>

    @Query("SELECT * FROM call_logs WHERE phoneNumber = :number ORDER BY timestamp DESC")
    fun getCallLogsForNumber(number: String): Flow<List<CallLog>>

    @Query("SELECT * FROM call_logs WHERE id = :id LIMIT 1")
    suspend fun getCallLogById(id: Long): CallLog?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCallLog(callLog: CallLog)

    @Query("DELETE FROM call_logs WHERE id = :id")
    suspend fun deleteCallLog(id: Long)

    @Query("DELETE FROM call_logs")
    suspend fun deleteAllCallLogs()

    @Query("SELECT COUNT(*) FROM call_logs WHERE callType = 'MISSED'")
    fun getMissedCallCount(): Flow<Int>

    @Query("SELECT * FROM contacts WHERE deviceContactId = :deviceId LIMIT 1")
    suspend fun getContactByDeviceId(deviceId: String): Contact?

    @Query("UPDATE call_logs SET contactName = :newName WHERE phoneNumber = :phone")
    suspend fun updateContactNameByPhone(phone: String, newName: String)

    @Query("""
    SELECT 
        cl.id,
        cl.phoneNumber,
        cl.callType,
        cl.timestamp,
        cl.durationSeconds,
        cl.profileImageUri,
        COALESCE(
            (SELECT c.name FROM contacts c 
             WHERE REPLACE(REPLACE(REPLACE(REPLACE(c.phoneNumber,' ',''),'-',''),'(',''),')','')
                 = REPLACE(REPLACE(REPLACE(REPLACE(cl.phoneNumber,' ',''),'-',''),'(',''),')','')
             LIMIT 1),
            cl.contactName
        ) AS contactName
    FROM call_logs cl
    ORDER BY cl.timestamp DESC
""")
    fun getAllResolvedCallLogs(): Flow<List<ResolvedCallLog>>

    @Query("""
    SELECT 
        cl.id,
        cl.phoneNumber,
        cl.callType,
        cl.timestamp,
        cl.durationSeconds,
        cl.profileImageUri,
        COALESCE(
            (SELECT c.name FROM contacts c 
             WHERE REPLACE(REPLACE(REPLACE(REPLACE(c.phoneNumber,' ',''),'-',''),'(',''),')','')
                 = REPLACE(REPLACE(REPLACE(REPLACE(cl.phoneNumber,' ',''),'-',''),'(',''),')','')
             LIMIT 1),
            cl.contactName
        ) AS contactName
    FROM call_logs cl
    WHERE cl.callType = 'MISSED'
    ORDER BY cl.timestamp DESC
""")
    fun getMissedResolvedCalls(): Flow<List<ResolvedCallLog>>

}
