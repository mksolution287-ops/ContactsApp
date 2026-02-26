package com.example.contactsapp.data.local

import androidx.room.*
import com.example.contactsapp.data.model.CallLog
import com.example.contactsapp.data.model.CallType
import com.example.contactsapp.data.model.Contact
import kotlinx.coroutines.flow.Flow

@Dao
interface CallLogDao {

    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC")
    fun getAllCallLogs(): Flow<List<CallLog>>

    @Query("SELECT * FROM call_logs WHERE callType = 'MISSED' ORDER BY timestamp DESC")
    fun getMissedCalls(): Flow<List<CallLog>>

    @Query("SELECT * FROM call_logs WHERE phoneNumber = :number ORDER BY timestamp DESC")
    fun getCallLogsForNumber(number: String): Flow<List<CallLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallLog(callLog: CallLog)

    @Query("DELETE FROM call_logs WHERE id = :id")
    suspend fun deleteCallLog(id: Long)

    @Query("DELETE FROM call_logs")
    suspend fun deleteAllCallLogs()

    @Query("SELECT COUNT(*) FROM call_logs WHERE callType = 'MISSED'")
    fun getMissedCallCount(): Flow<Int>

    @Query("SELECT * FROM contacts WHERE deviceContactId = :deviceId LIMIT 1")
    suspend fun getContactByDeviceId(deviceId: String): Contact?


}
