package com.example.contactsapp.ui.viewmodel

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.contactsapp.data.model.*
import com.example.contactsapp.data.repository.CallLogRepository
import com.example.contactsapp.data.repository.ContactRepository
import com.example.contactsapp.data.repository.SettingsRepository
import com.example.contactsapp.util.DeviceCallLogHelper
import com.example.contactsapp.util.DeviceContactsHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ContactViewModel(
    application: Application,
    val contactRepository: ContactRepository,
    val callLogRepository: CallLogRepository,
    val settingsRepository: SettingsRepository
) : AndroidViewModel(application) {

    private val deviceContactsHelper = DeviceContactsHelper(application)
    private val deviceCallLogHelper  = DeviceCallLogHelper(application)

    private val _searchQuery       = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _showFavoritesOnly = MutableStateFlow(false)
    val showFavoritesOnly: StateFlow<Boolean> = _showFavoritesOnly.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val contacts: StateFlow<List<Contact>> = combine(
        _searchQuery, _showFavoritesOnly
    ) { q, fav -> q to fav }
        .flatMapLatest { (q, fav) ->
            when {
                q.isNotBlank() -> contactRepository.searchContacts(q)
                fav            -> contactRepository.getFavoriteContacts()
                else           -> contactRepository.getAllContacts()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCallLogs: StateFlow<List<CallLog>> =
        callLogRepository.getAllCallLogs()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val missedCalls: StateFlow<List<CallLog>> =
        callLogRepository.getMissedCalls()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val missedCallCount: StateFlow<Int> =
        callLogRepository.getMissedCallCount()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val settings: StateFlow<AppSettings> = settingsRepository.settings

    private val _dialPadNumber = MutableStateFlow("")
    val dialPadNumber: StateFlow<String> = _dialPadNumber.asStateFlow()

    fun updateSearchQuery(q: String) { _searchQuery.value = q }
    fun toggleFavoritesFilter() { _showFavoritesOnly.value = !_showFavoritesOnly.value }

    fun loadDeviceContacts() = viewModelScope.launch {
        _isLoading.value = true
        runCatching { deviceContactsHelper.loadDeviceContacts() }
            .onSuccess { contactRepository.insertContacts(it) }
        _isLoading.value = false
    }

    fun loadDeviceCallLogs() = viewModelScope.launch {
        _isLoading.value = true
        try {
            Log.d("ContactViewModel", "Starting call log sync...")
            val logs = deviceCallLogHelper.loadDeviceCallLogs()
            Log.d("ContactViewModel", "Fetched ${logs.size} logs from device")

            logs.forEach { log ->
                callLogRepository.insertCallLog(log)
                Log.d("ContactViewModel", "Inserted: ${log.contactName} - ${log.callType}")
            }

            Log.d("ContactViewModel", "Call log sync complete")
        } catch (e: Exception) {
            Log.e("ContactViewModel", "Error syncing call logs", e)
        } finally {
            _isLoading.value = false
        }
    }

    fun addContact(c: Contact)    = viewModelScope.launch { contactRepository.insertContact(c) }
    fun updateContact(c: Contact) = viewModelScope.launch { contactRepository.updateContact(c) }
    fun deleteContact(c: Contact) = viewModelScope.launch { contactRepository.deleteContact(c) }
    fun toggleFavorite(id: Long, current: Boolean) =
        viewModelScope.launch { contactRepository.toggleFavorite(id, !current) }
    suspend fun getContactById(id: Long): Contact? = contactRepository.getContactById(id)

    fun deleteCallLog(id: Long) = viewModelScope.launch { callLogRepository.deleteCallLog(id) }
    fun clearAllCallLogs()      = viewModelScope.launch { callLogRepository.deleteAllCallLogs() }

    fun dialPadAppend(c: String) { _dialPadNumber.value += c }
    fun dialPadDelete() { if (_dialPadNumber.value.isNotEmpty()) _dialPadNumber.value = _dialPadNumber.value.dropLast(1) }
    fun dialPadClear()  { _dialPadNumber.value = "" }
    fun dialPadSetNumber(n: String) { _dialPadNumber.value = n }

    @RequiresPermission(Manifest.permission.CALL_PHONE)
    fun makeCall(context: Context, number: String) {
        val cleanNumber = number.trim().removePrefix("tel:").trim()
        if (cleanNumber.isBlank()) return

        try {
            if (isDefaultDialer(context)) {
                // We are default — place call via TelecomManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val telecomManager = context.getSystemService(Context.TELECOM_SERVICE)
                            as TelecomManager
                    val uri = Uri.fromParts("tel", cleanNumber, null)
                    telecomManager.placeCall(uri, android.os.Bundle())
                } else {
                    val intent = Intent(Intent.ACTION_CALL).apply {
                        data = Uri.parse("tel:$cleanNumber")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }
            } else {
                // Not default — find the actual default dialer package and launch it directly
                // This skips the bottom drawer chooser entirely
                val defaultDialerPackage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val telecomManager = context.getSystemService(Context.TELECOM_SERVICE)
                            as TelecomManager
                    telecomManager.defaultDialerPackage
                } else null

                val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$cleanNumber")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }

                if (!defaultDialerPackage.isNullOrBlank()) {
                    // Set the package explicitly so ONLY the default dialer opens — no chooser
                    dialIntent.setPackage(defaultDialerPackage)
                }

                try {
                    context.startActivity(dialIntent)
                } catch (e: Exception) {
                    // Default dialer package couldn't handle it — remove package restriction and try again
                    dialIntent.setPackage(null)
                    context.startActivity(dialIntent)
                }
            }
        } catch (e: SecurityException) {
            Log.e("makeCall", "Permission denied", e)
            // Last resort fallback
            try {
                context.startActivity(Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$cleanNumber")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            } catch (ex: Exception) {
                Log.e("makeCall", "Could not launch dialer", ex)
            }
        } catch (e: Exception) {
            Log.e("makeCall", "Call failed", e)
            try {
                context.startActivity(Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$cleanNumber")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            } catch (ex: Exception) {
                Log.e("makeCall", "Could not launch dialer", ex)
            }
        }

        // Log outgoing call
        viewModelScope.launch {
            val contact = contactRepository.getContactByPhone(cleanNumber)
            callLogRepository.insertCallLog(
                CallLog(
                    contactName     = contact?.name ?: cleanNumber,
                    phoneNumber     = cleanNumber,
                    callType        = CallType.OUTGOING,
                    timestamp       = System.currentTimeMillis(),
                    profileImageUri = contact?.profileImageUri
                )
            )
        }
    }

    fun isDefaultDialer(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val telecom = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                context.packageName == telecom.defaultDialerPackage
            } else false
        } catch (e: Exception) { false }
    }

    fun getMatchingContacts(dialedNumber: String): List<Contact> {
        if (dialedNumber.isEmpty()) return emptyList()

        return contacts.value.filter { contact ->
            contact.phoneNumber.replace(Regex("[^0-9+]"), "")
                .contains(dialedNumber.replace(Regex("[^0-9+]"), ""))
        }.take(5) // Limit to 5 suggestions
    }

    fun setTheme(t: AppTheme)          { settingsRepository.updateTheme(t) }
    fun setAccentColor(c: AccentColor) { settingsRepository.updateAccentColor(c) }
    fun setSortOrder(b: Boolean)       { settingsRepository.updateSortOrder(b) }
    fun setShowPhone(b: Boolean)       { settingsRepository.updateShowPhone(b) }
    fun setConfirmDelete(b: Boolean)   { settingsRepository.updateConfirmDelete(b) }
}
