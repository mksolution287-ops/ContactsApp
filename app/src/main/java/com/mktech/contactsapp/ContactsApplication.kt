package com.mktech.contactsapp

import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.room.Room
import com.google.android.gms.ads.MobileAds
import com.mktech.contactsapp.data.local.ContactDatabase
import com.mktech.contactsapp.data.model.AppLanguage
import com.mktech.contactsapp.data.repository.CallLogRepository
import com.mktech.contactsapp.data.repository.ContactRepository
import com.mktech.contactsapp.data.repository.SettingsRepository
import com.mktech.contactsapp.util.AdManager
import com.mktech.contactsapp.util.LocaleHelper

class ContactsApplication : Application() {

    //for multiple languages
    override fun attachBaseContext(base: Context) {
        // Read saved language preference before UI loads
        val prefs = base.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
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

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate() {
        super.onCreate()

        // Initialize AdMob
        MobileAds.initialize(this) { initStatus ->
            Log.d("AdMob", "Initialized: ${initStatus.adapterStatusMap}")
            // ← Init AdManager INSIDE the callback, after MobileAds is ready
            AdManager.init(this)
        }

        // ── Detect fresh install and clear stale prefs ───────────────────
        val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val currentVersionCode = packageManager
            .getPackageInfo(packageName, 0)
            .longVersionCode
        val savedVersionCode = prefs.getLong("install_version_code", -1L)

        if (savedVersionCode == -1L) {
            // Fresh install — clear everything so language picker shows again
            prefs.edit().clear().putLong("install_version_code", currentVersionCode).commit()
        } else if (savedVersionCode != currentVersionCode) {
            // App was updated — keep language but update version code
            val savedLanguage = prefs.getString("language", null)
            prefs.edit().clear().apply {
                putLong("install_version_code", currentVersionCode)
                if (savedLanguage != null) putString("language", savedLanguage)
            }.commit()
        }

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
