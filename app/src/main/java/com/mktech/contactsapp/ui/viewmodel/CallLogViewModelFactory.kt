//package com.example.contactsapp.ui.viewmodel
//
//
//import android.app.Application
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.ViewModelProvider
//import com.example.contactsapp.data.repository.CallLogRepository
//import com.example.contactsapp.data.repository.ContactRepository
//
//class CallLogViewModelFactory(
//    private val application: Application,
//    private val callLogRepository: CallLogRepository,
//    private val contactRepository: ContactRepository
//) : ViewModelProvider.Factory {
//
//    override fun <T : ViewModel> create(modelClass: Class<T>): T {
//        if (modelClass.isAssignableFrom(CallLogViewModel::class.java)) {
//            @Suppress("UNCHECKED_CAST")
//            return CallLogViewModel(
//                application,
//                callLogRepository,
//                contactRepository
//            ) as T
//        }
//        throw IllegalArgumentException("Unknown ViewModel class")
//    }
////}