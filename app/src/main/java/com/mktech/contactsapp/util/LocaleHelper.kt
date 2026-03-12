//package com.mktech.contactsapp.util
//
//import android.content.Context
//import android.content.res.Configuration
//import android.os.Build
//import com.mktech.contactsapp.data.model.AppLanguage
//import java.util.Locale
//
//object LocaleHelper {
//
//    fun applyLanguage(context: Context, language: AppLanguage): Context {
//        if (language == AppLanguage.SYSTEM) return context
//
//        val locale = Locale(language.code)
//        Locale.setDefault(locale)
//
//        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//            val config = Configuration(context.resources.configuration)
//            config.setLocale(locale)
//            context.createConfigurationContext(config)
//        } else {
//            @Suppress("DEPRECATION")
//            val config = context.resources.configuration
//            config.locale = locale
//            @Suppress("DEPRECATION")
//            context.resources.updateConfiguration(config, context.resources.displayMetrics)
//            context
//        }
//    }
//}
package com.mktech.contactsapp.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import com.mktech.contactsapp.data.model.AppLanguage
import java.util.Locale

object LocaleHelper {

    fun applyLanguage(context: Context, language: AppLanguage): Context {
        if (language == AppLanguage.SYSTEM) return context

        return setLocale(context, language.code)
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