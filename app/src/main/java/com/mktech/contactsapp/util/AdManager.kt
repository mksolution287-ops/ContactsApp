package com.mktech.contactsapp.util

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object AdManager {

    // ── Test Ad Unit IDs (replace with real ones for production) ─────────
    private const val TEST_BANNER_ID       = "ca-app-pub-3940256099942544/6300978111"
    private const val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
    private const val TEST_NATIVE_ID       = "ca-app-pub-3940256099942544/2247696110"
    private const val TEST_APP_OPEN_ID     = "ca-app-pub-3940256099942544/9257395921"


    // ── Remote Config keys ────────────────────────────────────────────────
    private const val KEY_ADS_ENABLED          = "ads_enabled"
    private const val KEY_BANNER_ENABLED       = "banner_ad_enabled"
    private const val KEY_INTERSTITIAL_ENABLED = "interstitial_ad_enabled"
    private const val KEY_NATIVE_ENABLED       = "native_ad_enabled"
    private const val KEY_APP_OPEN_ENABLED     = "app_open_ad_enabled"
    private const val KEY_INTERSTITIAL_TRIGGER = "interstitial_trigger_count"
    private const val KEY_BANNER_AD_UNIT       = "banner_ad_unit_id"
    private const val KEY_INTERSTITIAL_AD_UNIT = "interstitial_ad_unit_id"
    private const val KEY_NATIVE_AD_UNIT       = "native_ad_unit_id"
    private const val KEY_APP_OPEN_AD_UNIT     = "app_open_ad_unit_id"


    // ── State ─────────────────────────────────────────────────────────────
    private var interstitialAd: InterstitialAd? = null
    private var actionCount = 0

    private val _adsEnabled = MutableStateFlow(false)
    val adsEnabled: StateFlow<Boolean> = _adsEnabled

    private val _bannerEnabled = MutableStateFlow(false)
    val bannerEnabled: StateFlow<Boolean> = _bannerEnabled

    private val _nativeEnabled = MutableStateFlow(false)
    val nativeEnabled: StateFlow<Boolean> = _nativeEnabled

    private val _appOpenEnabled = MutableStateFlow(false)
    val appOpenEnabled: StateFlow<Boolean> = _appOpenEnabled

    // ── Native Ad state ───────────────────────────────────────────────────
    private var nativeAd: NativeAd? = null
    private val _nativeAdReady = MutableStateFlow(false)
    val nativeAdReady: StateFlow<Boolean> = _nativeAdReady

    var isInterstitialShowing = false
        private set

    @Volatile
    var skipNextInterstitial: Boolean = false

    // ── Remote Config defaults ────────────────────────────────────────────
    private val remoteConfigDefaults = mapOf(
        KEY_ADS_ENABLED          to true,
        KEY_BANNER_ENABLED       to true,
        KEY_INTERSTITIAL_ENABLED to true,
        KEY_NATIVE_ENABLED       to true,
        KEY_APP_OPEN_ENABLED     to true,
        KEY_INTERSTITIAL_TRIGGER to 3L,
        KEY_BANNER_AD_UNIT       to TEST_BANNER_ID,
        KEY_INTERSTITIAL_AD_UNIT to TEST_INTERSTITIAL_ID,
        KEY_NATIVE_AD_UNIT       to TEST_NATIVE_ID,
        KEY_APP_OPEN_AD_UNIT     to TEST_APP_OPEN_ID
    )

    // ── Init Remote Config and fetch ──────────────────────────────────────
    fun init(context: Context) {
        val remoteConfig = Firebase.remoteConfig
        remoteConfig.setConfigSettingsAsync(
            remoteConfigSettings {
                minimumFetchIntervalInSeconds = 0 //temp 0, in production 3600
            }
        )
        remoteConfig.setDefaultsAsync(remoteConfigDefaults)

        Firebase.remoteConfig.fetchAndActivate().addOnCompleteListener {
            val config = Firebase.remoteConfig

            _adsEnabled.value = config.getBoolean(KEY_ADS_ENABLED)
            _bannerEnabled.value = config.getBoolean(KEY_ADS_ENABLED) &&
                    config.getBoolean(KEY_BANNER_ENABLED)
            _nativeEnabled.value = config.getBoolean(KEY_ADS_ENABLED) &&
                    config.getBoolean(KEY_NATIVE_ENABLED)
            _appOpenEnabled.value = config.getBoolean(KEY_ADS_ENABLED) &&
                    config.getBoolean(KEY_APP_OPEN_ENABLED)

            Log.d("AdManager", "Remote config fetched: " +
                    "ads=${_adsEnabled.value} " +
                    "banner=${_bannerEnabled.value} " +
                    "native=${_nativeEnabled.value} " +
                    "appOpen=${_appOpenEnabled.value}")

            if (_adsEnabled.value) {
                preloadInterstitial(context)
                if (_nativeEnabled.value) preloadNativeAd(context)
            }
        }
    }

    // Cooldown: blocks app open ad briefly after interstitial dismisses
    private var interstitialDismissedAt = 0L
    private const val INTERSTITIAL_COOLDOWN_MS = 2000L // 2 seconds

    fun isInterstitialRecentlyActive(): Boolean {
        return isInterstitialShowing ||
                (System.currentTimeMillis() - interstitialDismissedAt < INTERSTITIAL_COOLDOWN_MS)
    }

    // ── App Open Ad helpers (called by AppOpenAdManager) ──────────────────
    fun isAppOpenAdEnabled(): Boolean {
        val config = Firebase.remoteConfig
        return config.getBoolean(KEY_ADS_ENABLED) &&
                config.getBoolean(KEY_APP_OPEN_ENABLED)
    }

    fun getAppOpenAdUnitId(): String =
        Firebase.remoteConfig.getString(KEY_APP_OPEN_AD_UNIT).ifBlank { TEST_APP_OPEN_ID }


    // ── Banner ad unit ID from Remote Config ──────────────────────────────
    fun getBannerAdUnitId(): String {
        return Firebase.remoteConfig.getString(KEY_BANNER_AD_UNIT)
            .ifBlank { TEST_BANNER_ID }
    }

    // ── Preload interstitial ──────────────────────────────────────────────
    fun preloadInterstitial(context: Context) {
        val config = Firebase.remoteConfig
        if (!config.getBoolean(KEY_ADS_ENABLED) ||
            !config.getBoolean(KEY_INTERSTITIAL_ENABLED)) return

        val adUnitId = config.getString(KEY_INTERSTITIAL_AD_UNIT).ifBlank { TEST_INTERSTITIAL_ID }

        InterstitialAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    Log.d("AdManager", "Interstitial loaded ✅")
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    Log.e("AdManager", "Interstitial failed ❌ ${error.message}")
                }
            }
        )
    }

    // ── Call this on user actions (calls made, contacts opened, etc.) ─────
    fun trackAction(context: Context, activity: Activity?) {
        if (!_adsEnabled.value) return
        val config = Firebase.remoteConfig
        if (!config.getBoolean(KEY_INTERSTITIAL_ENABLED)) return

        actionCount++
        val triggerCount = config.getLong(KEY_INTERSTITIAL_TRIGGER).toInt()

//        if (actionCount >= triggerCount) {
//            actionCount = 0
//            showInterstitial(activity) {
//                preloadInterstitial(context)
//            }
//        }
        if (actionCount >= triggerCount) {
            actionCount = 0

            // 🚫 Block interstitial if app just resumed (App Open should take priority)
            if (isInterstitialRecentlyActive()) {
                Log.d("AdManager", "Skipping interstitial due to recent app open")
                return
            }

            showInterstitial(activity) {
                preloadInterstitial(context)
            }
        }
    }

    // ── Show interstitial if ready ────────────────────────────────────────
    private fun showInterstitial(activity: Activity?, onDismiss: () -> Unit) {
        if (activity == null || interstitialAd == null) {
            onDismiss()
            return
        }

        isInterstitialShowing = true  // ← ADD THIS

        interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                isInterstitialShowing = false
                interstitialDismissedAt = System.currentTimeMillis()  // ← ADD THIS too
                onDismiss()
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitialAd = null
                isInterstitialShowing = false
                onDismiss()
            }
        }
        interstitialAd?.show(activity)
    }


    // ── Preload native ad ─────────────────────────────────────────────────
    fun preloadNativeAd(context: Context) {
        if (!_adsEnabled.value || !_nativeEnabled.value) return

        val adUnitId = try {
            Firebase.remoteConfig.getString(KEY_NATIVE_AD_UNIT).ifBlank { TEST_NATIVE_ID }
        } catch (e: Exception) {
            TEST_NATIVE_ID
        }

        Log.d("AdManager", "preloadNativeAd: loading adUnitId=$adUnitId")

        val adLoader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { ad ->
                nativeAd?.destroy()
                nativeAd = ad
                _nativeAdReady.value = true
                Log.d("AdManager", "Native ad loaded ✅")
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    nativeAd = null
                    _nativeAdReady.value = false
                    Log.e("AdManager", "Native ad failed ❌ code=${error.code} msg=${error.message}")
                }
            })
            .withNativeAdOptions(NativeAdOptions.Builder().build())
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    // ── Get the loaded native ad (call after nativeAdReady = true) ───────
    fun getNativeAd(): NativeAd? = nativeAd

    // ── Must call when the screen/composable is destroyed ────────────────
    fun destroyNativeAd() {
        nativeAd?.destroy()
        nativeAd = null
        _nativeAdReady.value = false
    }

    // ── Show immediately after splash, bypasses counter ──────────────────
//    fun immediateInterstitialAd(activity: Activity?) {
//        if (!_adsEnabled.value) return
//        if (interstitialAd == null) {
//            Log.w("AdManager", "Splash ad not ready yet")
//            return
//        }
//        showInterstitial(activity) {
//            preloadInterstitial(activity ?: return@showInterstitial)
//        }
//    }

    // ── Show immediately after splash, bypasses counter ──────────────────
    fun immediateInterstitialAd(activity: Activity?) {
        if (!_adsEnabled.value) return

        // 🚫 One-shot skip — used after language change + recreate(), etc.
        if (skipNextInterstitial) {
            Log.d("AdManager", "Skipping immediate interstitial — skipNextInterstitial was set")
            skipNextInterstitial = false
            return
        }

        if (interstitialAd == null) {
            Log.w("AdManager", "Splash ad not ready yet")
            return
        }
        showInterstitial(activity) {
            preloadInterstitial(activity ?: return@showInterstitial)
        }
    }

    fun showInterstitialOnAppResume(activity: Activity?) {
        if (!_adsEnabled.value) return
        if (activity == null) return

        // 🚫 Block if already showing something
        if (isInterstitialShowing) return

        if (interstitialAd != null) {
            isInterstitialShowing = true

            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    isInterstitialShowing = false
                    interstitialDismissedAt = System.currentTimeMillis()
                    preloadInterstitial(activity)
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    interstitialAd = null
                    isInterstitialShowing = false
                    preloadInterstitial(activity)
                }
            }

            interstitialAd?.show(activity)
        } else {
            preloadInterstitial(activity)
        }
    }

}