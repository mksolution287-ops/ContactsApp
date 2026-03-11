package com.mktech.contactsapp.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.ui.platform.LocalContext
import com.mktech.contactsapp.data.model.AccentColor
import com.mktech.contactsapp.data.model.AppLanguage
import com.mktech.contactsapp.data.model.AppSettings
import com.mktech.contactsapp.data.model.AppTheme
import com.mktech.contactsapp.util.LocaleHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


private const val TAG = "SettingsRepository"
class SettingsRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun loadSettings() = AppSettings(
        theme = AppTheme.valueOf(prefs.getString("theme", AppTheme.SYSTEM.name)!!),
        accentColor = AccentColor.valueOf(prefs.getString("accent_color", AccentColor.INDIGO.name)!!),
        sortByFirstName = prefs.getBoolean("sort_first_name", true),
        showPhoneNumberInList = prefs.getBoolean("show_phone_in_list", false),
        confirmBeforeDelete = prefs.getBoolean("confirm_delete", true),
        //for language
        language = AppLanguage.values().find {
            it.code == prefs.getString("language", AppLanguage.SYSTEM.code)
        } ?: AppLanguage.SYSTEM
    )

    fun updateTheme(theme: AppTheme) {
        prefs.edit().putString("theme", theme.name).apply()
        _settings.value = _settings.value.copy(theme = theme)
    }

    fun updateAccentColor(color: AccentColor) {
        prefs.edit().putString("accent_color", color.name).apply()
        _settings.value = _settings.value.copy(accentColor = color)
    }

    fun updateSortOrder(sortByFirstName: Boolean) {
        prefs.edit().putBoolean("sort_first_name", sortByFirstName).apply()
        _settings.value = _settings.value.copy(sortByFirstName = sortByFirstName)
    }

    fun updateShowPhone(show: Boolean) {
        prefs.edit().putBoolean("show_phone_in_list", show).apply()
        _settings.value = _settings.value.copy(showPhoneNumberInList = show)
    }

    fun updateConfirmDelete(confirm: Boolean) {
        prefs.edit().putBoolean("confirm_delete", confirm).apply()
        _settings.value = _settings.value.copy(confirmBeforeDelete = confirm)
    }

    // for multiple languages
    fun updateLanguage(language: AppLanguage) {
        Log.d(TAG, "updateLanguage() called with language='${language.code}'")

        prefs.edit().putString("language", language.code).apply()

        _settings.value = _settings.value.copy(language = language)
    }

}
