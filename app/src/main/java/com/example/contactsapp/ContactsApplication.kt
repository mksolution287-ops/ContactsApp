package com.example.contactsapp

import android.app.Application
import androidx.room.Room
import com.example.contactsapp.data.local.ContactDatabase
import com.example.contactsapp.data.repository.CallLogRepository
import com.example.contactsapp.data.repository.ContactRepository
import com.example.contactsapp.data.repository.SettingsRepository

class ContactsApplication : Application() {

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
