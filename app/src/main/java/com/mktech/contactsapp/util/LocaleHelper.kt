package com.mktech.contactsapp.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.mktech.contactsapp.data.model.AppLanguage
import java.util.Locale

object LocaleHelper {

    fun applyLanguage(context: Context, language: AppLanguage): Context {
        if (language == AppLanguage.SYSTEM) return context

        return setLocale(context, language.code)
    }

    // ── Called when user changes language in Settings ─────────────────────
    // Uses AppCompatDelegate — no activity restart, stays on current screen
    fun changeLanguage(languageCode: String) {
        Log.d("LocaleHelper", "changeLanguage called: $languageCode")
        val localeList = if (languageCode == AppLanguage.SYSTEM.code) {
            LocaleListCompat.getEmptyLocaleList()  // revert to system
        } else {
            LocaleListCompat.forLanguageTags(languageCode)
        }
        Log.d("LocaleHelper", "Setting locales: $localeList")
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    fun setLocale(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
            context.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            context
        }
    }
}