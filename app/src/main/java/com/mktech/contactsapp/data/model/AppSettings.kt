package com.mktech.contactsapp.data.model

enum class AppTheme { LIGHT, DARK, SYSTEM }

enum class AppLanguage(val code: String, val displayName: String, val nativeName: String) {
    SYSTEM("system", "System Default", "System Default"),
    ENGLISH("en",    "English",        "English"),
    HINDI("hi",      "Hindi",          "हिन्दी"),
    SPANISH("es",    "Spanish",        "Español"),
    FRENCH("fr",     "French",         "Français"),
    ARABIC("ar",     "Arabic",         "العربية"),
    GERMAN("de",     "German",         "Deutsch"),
//    PORTUGUESE("pt", "Portuguese",     "Português"),
//    RUSSIAN("ru",    "Russian",        "Русский"),
    CHINESE("zh",    "Chinese",        "中文"),
    JAPANESE("ja",   "Japanese",       "日本語"),
    KOREAN("ko",     "Korean",         "한국어")
}

enum class AccentColor(val hex: Long) {
    INDIGO(0xFF6366F1),
    VIOLET(0xFF8B5CF6),
    ROSE(0xFFF43F5E),
    EMERALD(0xFF10B981),
    AMBER(0xFFF59E0B),
    CYAN(0xFF06B6D4),
    BLUE(0xFF3B82F6),
    ORANGE(0xFFF97316)
}

data class AppSettings(
    val theme: AppTheme = AppTheme.SYSTEM,
    val accentColor: AccentColor = AccentColor.INDIGO,
    val sortByFirstName: Boolean = true,
    val showPhoneNumberInList: Boolean = true,
    val confirmBeforeDelete: Boolean = true,
    val language: AppLanguage = AppLanguage.SYSTEM
)
