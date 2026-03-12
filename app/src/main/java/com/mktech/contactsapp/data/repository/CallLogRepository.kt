package com.mktech.contactsapp.data.repository

import com.mktech.contactsapp.data.local.CallLogDao
import com.mktech.contactsapp.data.model.CallLog
import com.mktech.contactsapp.data.model.ResolvedCallLog
import kotlinx.coroutines.flow.Flow

class CallLogRepository(private val callLogDao: CallLogDao) {

    fun getAllCallLogs(): Flow<List<CallLog>> = callLogDao.getAllCallLogs()

    fun getMissedCalls(): Flow<List<CallLog>> = callLogDao.getMissedCalls()

    fun getMissedCallCount(): Flow<Int> = callLogDao.getMissedCallCount()

    fun getCallLogsForNumber(number: String): Flow<List<CallLog>> =
        callLogDao.getCallLogsForNumber(number)

    suspend fun insertCallLog(callLog: CallLog) = callLogDao.insertCallLog(callLog)

    suspend fun deleteCallLog(id: Long) = callLogDao.deleteCallLog(id)

    suspend fun deleteAllCallLogs() = callLogDao.deleteAllCallLogs()

    suspend fun getCallLogById(id: Long): CallLog? =
        callLogDao.getCallLogById(id)

    suspend fun updateContactNameByPhone(phone: String, newName: String) {
        callLogDao.updateContactNameByPhone(phone, newName)
    }

    suspend fun updateProfileImageByPhone(phone: String, newUri: String?) {
        callLogDao.updateProfileImageByPhone(phone, newUri)
    }

    fun getAllResolvedCallLogs(): Flow<List<ResolvedCallLog>> =
        callLogDao.getAllResolvedCallLogs()

    fun getMissedResolvedCalls(): Flow<List<ResolvedCallLog>> =
        callLogDao.getMissedResolvedCalls()
}
