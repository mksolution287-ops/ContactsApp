package com.mktech.contactsapp

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.room.Room
import com.google.android.gms.ads.MobileAds
import com.google.firebase.messaging.FirebaseMessaging
import com.mktech.contactsapp.data.local.ContactDatabase
import com.mktech.contactsapp.data.model.AppLanguage
import com.mktech.contactsapp.data.repository.CallLogRepository
import com.mktech.contactsapp.data.repository.ContactRepository
import com.mktech.contactsapp.data.repository.SettingsRepository
import com.mktech.contactsapp.util.AdManager
import com.mktech.contactsapp.util.AppOpenAdManager
import com.mktech.contactsapp.util.InAppMessagingManager
import com.mktech.contactsapp.util.LocaleHelper
import com.mktech.contactsapp.util.NotificationHelper
import com.mktech.contactsapp.util.OneSignalManager.init

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

    // ── App Open Ad Manager ───────────────────────────────────────────────
    lateinit var appOpenAdManager: AppOpenAdManager
        private set


    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate() {
        super.onCreate()

        // ── Ads FIRST: start Remote Config + native/interstitial preloading right away.
        // (Previously this waited for MobileAds.initialize's callback, which delayed
        // every ad request by the SDK init time.)
        AdManager.init(this)

        // Initialize AdMob once. AppOpenAdManager loads its ad as soon as app-open ads
        // are enabled, so it no longer depends on init ordering.
        MobileAds.initialize(this) { initStatus ->
            Log.d("AdMob", "Initialized: ${initStatus.adapterStatusMap}")
            appOpenAdManager = AppOpenAdManager(this)
            appOpenAdManager.loadAd(this)
        }

        // ── Push notifications: channel + FCM + OneSignal ────────────────
        NotificationHelper.createNotificationChannels(this)

        // Log the current FCM token once on startup (also captured on refresh
        // in MyFirebaseMessagingService.onNewToken).
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("FCM", "Token: ${task.result}")
            } else {
                Log.e("FCM", "Fetching FCM token failed", task.exception)
            }
        }

        // OneSignal — verbose logging only on debuggable builds.
        // (The old code imported com.google.firebase.messaging.BuildConfig, which is the
        // Firebase library's flag, not this app's.)
        val isDebuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        init(this, debug = isDebuggable)

        // Firebase In-App Messaging — starts un-suppressed; AdManager toggles
        // suppression on/off around interstitial ad display so the two never overlap.
        InAppMessagingManager.init()

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