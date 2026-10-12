package com.mktech.contactsapp.util

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.appopen.AppOpenAd
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.util.Date

class AppOpenAdManager(private val application: Application) :
    Application.ActivityLifecycleCallbacks {

    companion object {
        private const val TAG = "AppOpenAdManager"
        private const val AD_EXPIRY_MS = 4 * 60 * 60 * 1000L
        private const val HOLD_SAFETY_MS = 3 * 60 * 60 * 1000L
        private const val MAX_LOAD_RETRIES = 3

        @Volatile
        var skipNextAd: Boolean = false

        // ── Suppression ───────────────────────────────────────────────────
        @Volatile private var suppressUntil = 0L
        @Volatile private var heldAt = 0L

        /** Timed block. Use for one-off events (placing a call, SIM picker). */
        fun suppress(ms: Long = 30_000L) {
            suppressUntil = SystemClock.elapsedRealtime() + ms
        }

        /** Block until releaseSuppression() is called. Use while a call or post-call screen is alive. */
        fun holdSuppression() {
            heldAt = SystemClock.elapsedRealtime()
        }

        /** Ends the hold and keeps blocking for a short window afterwards. */
        fun releaseSuppression(afterMs: Long = 30_000L) {
            heldAt = 0L
            suppress(afterMs)
        }

        fun isSuppressed(): Boolean {
            val now = SystemClock.elapsedRealtime()
            // The hold expires after 3 hours as a safety net, in case a release never runs
            val held = heldAt != 0L && now - heldAt < HOLD_SAFETY_MS
            return held || now < suppressUntil
        }
    }

    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var isShowingAd = false
    private var loadTime: Long = 0
    private var currentActivity: Activity? = null
    private var loadedAdUnitId: String = ""
    private var loadRetryCount = 0

    // Tracks whether the app genuinely went to background
    private var wasInBackground = false
    // Count of started (foreground) activities — app is in background when this hits 0
    private var startedActivityCount = 0

    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    init {
        application.registerActivityLifecycleCallbacks(this)

        // Remote Config may not be ready when this manager is created, which used to make
        // the first loadAd() bail out silently. Load as soon as app-open ads become enabled
        // (fires immediately if they already are, and again if config changes).
        scope.launch {
            AdManager.appOpenEnabled
                .filter { it }
                .collect { loadAd(application) }
        }
    }

    private fun isAdExpired(): Boolean = Date().time - loadTime > AD_EXPIRY_MS
    private fun isAdAvailable(): Boolean = appOpenAd != null && !isAdExpired()

    fun loadAd(context: Context) {
        if (!AdManager.isAppOpenAdEnabled()) return
        if (isLoadingAd || isAdAvailable()) return
        isLoadingAd = true

        val adUnitId = AdManager.getAppOpenAdUnitId()
        val appContext = context.applicationContext

        AppOpenAd.load(
            appContext,
            adUnitId,
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isLoadingAd = false
                    loadRetryCount = 0
                    loadTime = Date().time
                    loadedAdUnitId = adUnitId

                    // 💰 Revenue tracking for app open ads
                    ad.onPaidEventListener = OnPaidEventListener { adValue ->
                        RevenueTracker.recordPaidEvent(
                            adFormat = "app_open",
                            adUnitId = adUnitId,
                            adValue = adValue,
                            responseInfo = ad.responseInfo
                        )
                    }

                    Log.d(TAG, "App open ad loaded ✅")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoadingAd = false
                    Log.e(TAG, "App open ad failed ❌ code=${error.code} ${error.message}")

                    // Retry with backoff (3s, 6s, 12s) so a single no-fill doesn't leave us empty
                    if (loadRetryCount < MAX_LOAD_RETRIES) {
                        val delay = 3000L shl loadRetryCount++
                        mainHandler.postDelayed({ loadAd(appContext) }, delay)
                    }
                }
            }
        )
    }

    fun showAdIfAvailable(activity: Activity) {
        if (!AdManager.isAppOpenAdEnabled()) return

        // 🔑 Call-related suppression (call placed, in progress, post-call screen)
        if (isSuppressed()) {
            Log.d(TAG, "Skipping app open — suppressed (call flow)")
            wasInBackground = false   // consume this foreground event so it doesn't fire later
            return
        }

        if (isShowingAd) return
        if (!wasInBackground) return

        if (skipNextAd) {
            Log.d(TAG, "Skipping app open — skipNextAd flag was set")
            skipNextAd = false
            wasInBackground = false
            return
        }

        // 🔑 Hard block — interstitial is literally on screen right now
        if (AdManager.isInterstitialShowing) {
            Log.d(TAG, "Skipping app open — interstitial is on screen")
            return
        }

        // 🔑 Soft block — interstitial just dismissed within cooldown window
        if (AdManager.isInterstitialRecentlyActive()) {
            Log.d(TAG, "Skipping app open — interstitial cooldown active")
            wasInBackground = false  // ← clear the flag so it doesn't retry on next resume
            return
        }

        if (!isAdAvailable()) {
            loadAd(application)
            return
        }

        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                isShowingAd = true
                Log.d(TAG, "App open ad shown ✅")
            }
            override fun onAdDismissedFullScreenContent() {
                appOpenAd = null
                isShowingAd = false
                wasInBackground = false  // ← Reset HERE, not in onAdShowed
                Log.d(TAG, "App open ad dismissed")
                loadAd(application)
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                appOpenAd = null
                isShowingAd = false
                wasInBackground = false  // ← Also reset on failure
                Log.e(TAG, "App open ad failed to show ❌ ${error.message}")
                loadAd(application)
            }
        }

        appOpenAd?.show(activity)
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────

    override fun onActivityStarted(activity: Activity) {
        startedActivityCount++
        // App came back from background — flag it here, before onResume fires
        if (wasInBackground) {
            currentActivity = activity
        }
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
        if (wasInBackground) {
            showAdIfAvailable(activity)
        }
    }

    override fun onActivityStopped(activity: Activity) {
        startedActivityCount--
        // When no activities are started, the app is truly in the background
        if (startedActivityCount == 0) {
            wasInBackground = true
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity == activity) currentActivity = null
    }
}