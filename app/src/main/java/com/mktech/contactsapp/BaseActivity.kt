package com.mktech.contactsapp

import android.content.Context
import androidx.activity.ComponentActivity
import com.mktech.contactsapp.data.repository.SettingsRepository
import com.mktech.contactsapp.util.LocaleHelper

abstract class BaseActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val repo = SettingsRepository(newBase)
        val lang = repo.settings.value.language
        val context = LocaleHelper.applyLanguage(newBase, lang)
        super.attachBaseContext(context)
    }
}