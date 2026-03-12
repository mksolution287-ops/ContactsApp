package com.mktech.contactsapp.data

import android.os.Bundle
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.crashlytics.crashlytics


object AnalyticsTracker {

    private val analytics: FirebaseAnalytics by lazy { Firebase.analytics }

    // ── Crashlytics helpers ──────────────────────────────────────────────

    fun setUserId(userId: String) {
        analytics.setUserId(userId)
        Firebase.crashlytics.setUserId(userId)
    }

    fun logNonFatalError(throwable: Throwable, context: String = "") {
        Firebase.crashlytics.apply {
            if (context.isNotEmpty()) setCustomKey("error_context", context)
            recordException(throwable)
        }
    }

    // ── Screen tracking ──────────────────────────────────────────────────

    fun logScreenView(screenName: String, screenClass: String) {
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
        })
    }

    // ── Contact events ───────────────────────────────────────────────────

    fun logContactOpened(contactId: Long, hasFavorite: Boolean) {
        analytics.logEvent("contact_opened", Bundle().apply {
            putLong("contact_id", contactId)
            putBoolean("is_favorite", hasFavorite)
        })
    }

    fun logContactAdded() {
        analytics.logEvent("contact_added", Bundle.EMPTY)
    }

    fun logContactCalled(hasImage: Boolean) {
        analytics.logEvent("contact_called", Bundle().apply {
            putBoolean("contact_has_image", hasImage)
        })
    }

    fun logFavoriteToggled(contactId: Long, newState: Boolean) {
        analytics.logEvent("favorite_toggled", Bundle().apply {
            putLong("contact_id", contactId)
            putBoolean("is_now_favorite", newState)
        })
    }

    // ── UI events ────────────────────────────────────────────────────────

    fun logAlphabetScrubberUsed(letter: String) {
        analytics.logEvent("scrubber_used", Bundle().apply {
            putString("letter", letter)
        })
    }

    fun logFilterToggled(showFavoritesOnly: Boolean) {
        analytics.logEvent("filter_toggled", Bundle().apply {
            putBoolean("favorites_only", showFavoritesOnly)
        })
    }

    fun logSearchUsed(resultCount: Int) {
        analytics.logEvent("search_used", Bundle().apply {
            putInt("result_count", resultCount)
        })
    }

    // ── Generic event logger (for one-off events) ────────────────────────────
    fun logEvent(eventName: String, params: Map<String, String> = emptyMap()) {
        analytics.logEvent(eventName, Bundle().apply {
            params.forEach { (k, v) -> putString(k, v) }
        })
    }

    // ── Bottom nav tab tracking ──────────────────────────────────────────────
    fun logBottomNavTapped(route: String) {
        analytics.logEvent("bottom_nav_tapped", Bundle().apply {
            putString("destination", route)
        })
    }
}