package com.example.contactsapp.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.contactsapp.data.repository.CallLogRepository
import com.example.contactsapp.data.repository.ContactRepository
import com.example.contactsapp.data.repository.SettingsRepository

class ContactViewModelFactory(
    private val application: Application,
    private val contactRepository: ContactRepository,
    private val callLogRepository: CallLogRepository,
    private val settingsRepository: SettingsRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ContactViewModel::class.java)) {
            return ContactViewModel(application, contactRepository, callLogRepository, settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
