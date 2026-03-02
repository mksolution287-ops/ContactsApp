//package com.example.contactsapp.data.local
//
//import kotlinx.coroutines.flow.combine
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.example.contactsapp.data.local.CallLogDao
//import com.example.contactsapp.data.model.CallLogWithContact
//import com.example.contactsapp.data.repository.ContactRepository
//import kotlinx.coroutines.flow.SharingStarted
//import kotlinx.coroutines.flow.stateIn
//import kotlin.collections.emptyList
////
//class CallLogViewModel(
//    private val callLogDao: CallLogDao,
//    private val contactRepository: ContactRepository
//) : ViewModel() {
//
//    val callLogsUi = combine(
//        callLogDao.getAllCallLogs(),          // Flow<List<CallLog>>
//        contactRepository.getAllContacts() // Flow<List<Contact>>
//    ) { logs, contacts ->
//
//        val contactMap = contacts.associateBy { it.phoneNumber }
//
//        logs.map { log ->
//            CallLogWithContact(
//                id = log.id,
//                phoneNumber = log.phoneNumber,
//                timestamp = log.timestamp,
//                callType = log.callType,
//                durationSeconds = log.durationSeconds,
//                profileImageUri = contactMap[log.phoneNumber]?.profileImageUri,
//                contactName = contactMap[log.phoneNumber]?.name
//            )
//        }
//    }.stateIn(
//        viewModelScope,
//        SharingStarted.WhileSubscribed(5000),
//        emptyList()
//    )
//}