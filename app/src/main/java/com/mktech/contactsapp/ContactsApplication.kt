package com.mktech.contactsapp

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.mktech.contactsapp.data.local.ContactDatabase
import com.mktech.contactsapp.data.model.AppLanguage
import com.mktech.contactsapp.data.repository.CallLogRepository
import com.mktech.contactsapp.data.repository.ContactRepository
import com.mktech.contactsapp.data.repository.SettingsRepository
import com.mktech.contactsapp.util.LocaleHelper

class ContactsApplication : Application() {

    //for multiple languages
    override fun attachBaseContext(base: Context) {
        // Read saved language preference before UI loads
        val prefs = base.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val langCode = prefs.getString("language", AppLanguage.SYSTEM.code) ?: AppLanguage.SYSTEM.code
        val language = AppLanguage.values().find { it.code == langCode } ?: AppLanguage.SYSTEM
        val context = LocaleHelper.applyLanguage(base, language)
        super.attachBaseContext(context)
    }

    lateinit var database: ContactDatabase
        private set

    lateinit var contactRepository: ContactRepository
        private set

    lateinit var callLogRepository: CallLogRepository
        private set

    lateinit var settingsRepository: SettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()

        database = Room.databaseBuilder(
            applicationContext,
            ContactDatabase::class.java,
            "contacts_database"
        )
            .fallbackToDestructiveMigration()
            .build()

        contactRepository = ContactRepository(database.contactDao())
        callLogRepository = CallLogRepository(database.callLogDao())
        settingsRepository = SettingsRepository(applicationContext)
    }
}
