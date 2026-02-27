package com.example.contactsapp.ui.viewmodel

import android.Manifest
import android.app.Application
import android.content.ContentProviderOperation
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    fun addContact(c: Contact) = viewModelScope.launch {
        // 1️⃣ Write to Android system contacts first
        withContext(Dispatchers.IO) {
            addSystemContact(getApplication(), c)
        }

        // 2️⃣ Insert into Room with a future timestamp so sync never overwrites it
        val id = contactRepository.insertContact(
            c.copy(lastUpdatedAt = System.currentTimeMillis() + 5000L)
        )
        Log.d("ContactSave", "Inserted contact id=$id name=${c.name}")
    }

    fun addSystemContact(context: Context, contact: Contact) {
        val resolver = context.contentResolver
        val ops      = ArrayList<ContentProviderOperation>()

        // Step 1: Insert a new RawContact row
        ops.add(
            ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build()
        )

        // Step 2: Insert StructuredName (display name + given name)
        ops.add(
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(
                    ContactsContract.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                )
                .withValue(
                    ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
                    contact.name
                )
                .withValue(
                    ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,
                    contact.name
                )
                .build()
        )

        // Step 3: Insert phone number
        if (contact.phoneNumber.isNotBlank()) {
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                    )
                    .withValue(
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                        contact.phoneNumber
                    )
                    .withValue(
                        ContactsContract.CommonDataKinds.Phone.TYPE,
                        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                    )
                    .build()
            )
        }

        // Step 4: Insert email if provided
        if (contact.email.isNotBlank()) {
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE
                    )
                    .withValue(
                        ContactsContract.CommonDataKinds.Email.DATA,
                        contact.email
                    )
                    .withValue(
                        ContactsContract.CommonDataKinds.Email.TYPE,
                        ContactsContract.CommonDataKinds.Email.TYPE_HOME
                    )
                    .build()
            )
        }

        try {
            val results = resolver.applyBatch(ContactsContract.AUTHORITY, ops)
            Log.d("addSystemContact", "Created system contact name=${contact.name} results=${results.size}")
        } catch (e: Exception) {
            Log.e("addSystemContact", "Failed to create system contact", e)
        }
    }

//    fun updateContact(c: Contact) = viewModelScope.launch {
//        Log.d("ContactSave", "Updating contact id=${c.id} name=${c.name}")
//        contactRepository.updateContact(c)
//        Log.d("ContactSave", "Update complete for id=${c.id}")
//    }

    fun updateContact(c: Contact) = viewModelScope.launch {
        // 1️⃣ Update system contact FIRST
        updateSystemContact(
            context = getApplication(),
            phone = c.phoneNumber,
            newName = c.name
        )

        // 2️⃣ Update local Room DB
        contactRepository.updateContact(
            c.copy(lastUpdatedAt = System.currentTimeMillis())
        )

        // 3️⃣ Update contactName in all matching call logs
        callLogRepository.updateContactNameByPhone(
            phone = c.phoneNumber,
            newName = c.name
        )

    }



    fun deleteContact(c: Contact) = viewModelScope.launch {
        // 1️⃣ Delete from Android system contacts
        withContext(Dispatchers.IO) {
            deleteSystemContact(getApplication(), c.phoneNumber)
        }

        // 2️⃣ Delete from Room
        contactRepository.deleteContact(c)
    }

    fun deleteSystemContact(context: Context, phone: String) {
        val resolver        = context.contentResolver
        val normalizedInput = phone.replace(Regex("[^0-9+]"), "")

        // Find contactId via normalized last-10-digit match
        var rawContactId: Long? = null

        val phoneCursor = resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.RAW_CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null, null, null
        )

        phoneCursor?.use {
            while (it.moveToNext()) {
                val storedRaw  = it.getString(1) ?: continue
                val storedNorm = storedRaw.replace(Regex("[^0-9+]"), "")
                if (storedNorm.takeLast(10) == normalizedInput.takeLast(10)) {
                    rawContactId = it.getLong(0)
                    break
                }
            }
        }

        if (rawContactId == null) {
            Log.w("deleteSystemContact", "No system contact found for phone=$phone")
            return
        }

        // Delete by RAW_CONTACT_ID — cascades to all Data rows automatically
        val deleteUri = android.content.ContentUris.withAppendedId(
            ContactsContract.RawContacts.CONTENT_URI,
            rawContactId!!
        )

        try {
            val rows = resolver.delete(deleteUri, null, null)
            Log.d("deleteSystemContact", "Deleted $rows raw contact row(s) for phone=$phone")
        } catch (e: Exception) {
            Log.e("deleteSystemContact", "Failed to delete system contact", e)
        }
    }

    fun deleteCallLog(id: Long) = viewModelScope.launch {
        // Get the log before deleting so we have phone + timestamp
        val log = callLogRepository.getCallLogById(id)

        // Delete from Room
        callLogRepository.deleteCallLog(id)

        // Delete from system call log
        if (log != null) {
            withContext(Dispatchers.IO) {
                deleteSystemCallLog(getApplication(), log.phoneNumber, log.timestamp)
            }
        }
    }

    fun deleteSystemCallLog(context: Context, phoneNumber: String, timestamp: Long) {
        try {
            val deleted = context.contentResolver.delete(
                android.provider.CallLog.Calls.CONTENT_URI,
                "${android.provider.CallLog.Calls.NUMBER} = ? AND ${android.provider.CallLog.Calls.DATE} = ?",
                arrayOf(phoneNumber, timestamp.toString())
            )
            Log.d("deleteSystemCallLog", "Deleted $deleted system call log(s) for $phoneNumber")
        } catch (e: Exception) {
            Log.e("deleteSystemCallLog", "Failed to delete system call log", e)
        }
    }

    fun toggleFavorite(id: Long, current: Boolean) =
        viewModelScope.launch { contactRepository.toggleFavorite(id, !current) }
    suspend fun getContactById(id: Long): Contact? = contactRepository.getContactById(id)

//    fun deleteCallLog(id: Long) = viewModelScope.launch { callLogRepository.deleteCallLog(id) }
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

    fun updateSystemContact(
        context: Context,
        phone: String,
        newName: String
    ) {
        val resolver = context.contentResolver

        val cursor = resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.CONTACT_ID),
            "${ContactsContract.CommonDataKinds.Phone.NUMBER} = ?",
            arrayOf(phone),
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val contactId = it.getLong(0)

                val ops = ArrayList<ContentProviderOperation>()

                ops.add(
                    ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                        .withSelection(
                            "${ContactsContract.Data.CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?",
                            arrayOf(
                                contactId.toString(),
                                ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                            )
                        )
                        .withValue(
                            ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
                            newName
                        )
                        .build()
                )

                resolver.applyBatch(ContactsContract.AUTHORITY, ops)
            }
        }
    }

    val allResolvedCallLogs: StateFlow<List<ResolvedCallLog>> =
        callLogRepository.getAllResolvedCallLogs()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val missedResolvedCalls: StateFlow<List<ResolvedCallLog>> =
        callLogRepository.getMissedResolvedCalls()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    fun setTheme(t: AppTheme)          { settingsRepository.updateTheme(t) }
    fun setAccentColor(c: AccentColor) { settingsRepository.updateAccentColor(c) }
    fun setSortOrder(b: Boolean)       { settingsRepository.updateSortOrder(b) }
    fun setShowPhone(b: Boolean)       { settingsRepository.updateShowPhone(b) }
    fun setConfirmDelete(b: Boolean)   { settingsRepository.updateConfirmDelete(b) }
}
