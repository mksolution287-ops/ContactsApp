package com.mktech.contactsapp.util

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object AdManager {

    // ── Flip to false for production. When true, EVERY format uses Google's
    //    test ad units regardless of Remote Config. ──────────────────────────
    private const val USE_TEST_ADS = true

    // ── Test Ad Unit IDs ──────────────────────────────────────────────────
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

    // ── State ──────────────────────────────
    // Application instance (not an Activity), so it lives as long as the process anyway.
    // Typed as Application rather than Context so lint's StaticFieldLeak check passes.
    private lateinit var appCtx: Application
    private val mainHandler = Handler(Looper.getMainLooper())

    // The AdMob SDK objects below are loaded with the application Context, so keeping
    // them in this singleton cannot leak an Activity.
    @SuppressLint("StaticFieldLeak")
    private var interstitialAd: InterstitialAd? = null
    private var isLoadingInterstitial = false
    private var interstitialRetryCount = 0
    private var actionCount = 0

    private val _adsEnabled = MutableStateFlow(false)
    val adsEnabled: StateFlow<Boolean> = _adsEnabled

    private val _bannerEnabled = MutableStateFlow(false)
    val bannerEnabled: StateFlow<Boolean> = _bannerEnabled

    private val _nativeEnabled = MutableStateFlow(false)
    val nativeEnabled: StateFlow<Boolean> = _nativeEnabled

    private val _appOpenEnabled = MutableStateFlow(false)
    val appOpenEnabled: StateFlow<Boolean> = _appOpenEnabled

    // ── Native ad pool ────────────────────────────────────────────────────
    // Ads are loaded ahead of time (one request, several ads) so each screen
    // can grab one instantly instead of waiting on the network.
    private const val NATIVE_POOL_TARGET = 3
    private val nativePool = ArrayDeque<NativeAd>()
    private val _nativePoolSize = MutableStateFlow(0)
    val nativePoolSize: StateFlow<Int> = _nativePoolSize

    @SuppressLint("StaticFieldLeak")   // built with the application Context — see note above
    private var nativeLoader: AdLoader? = null
    private var nativeRetryCount = 0

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

    // ── Init Remote Config (MobileAds.initialize is called once in the Application) ──
    fun init(context: Context) {
        appCtx = context.applicationContext as Application

        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder()
                .setTestDeviceIds(
                    listOf(
                        "4F095E69A38FF00B02937165814FE45E",
                        "863d0058" + "048303634511317ff8c7c"
                    )
                )
                .build()
        )

        val remoteConfig = Firebase.remoteConfig
        remoteConfig.setConfigSettingsAsync(
            remoteConfigSettings {
                minimumFetchIntervalInSeconds = 3600   // 0 is for debugging only
            }
        )

        // Apply defaults / cached values IMMEDIATELY — don't wait for the network,
        // so ad loading starts at app launch.
        remoteConfig.setDefaultsAsync(remoteConfigDefaults).addOnCompleteListener {
            applyRemoteConfig()
        }

        // Fetch fresh values in the background
        remoteConfig.fetchAndActivate().addOnCompleteListener {
            applyRemoteConfig()
        }

        // Real-time updates when you publish changes in Firebase
        remoteConfig.addOnConfigUpdateListener(object : ConfigUpdateListener {
            override fun onUpdate(configUpdate: ConfigUpdate) {
                remoteConfig.activate().addOnCompleteListener { applyRemoteConfig() }
            }

            override fun onError(error: FirebaseRemoteConfigException) {
                Log.e("AdManager", "Real-time config error: ${error.message}")
            }
        })
    }

    private fun applyRemoteConfig() {
        val config = Firebase.remoteConfig

        _adsEnabled.value = config.getBoolean(KEY_ADS_ENABLED)
        _bannerEnabled.value = config.getBoolean(KEY_ADS_ENABLED) &&
                config.getBoolean(KEY_BANNER_ENABLED)
        _nativeEnabled.value = config.getBoolean(KEY_ADS_ENABLED) &&
                config.getBoolean(KEY_NATIVE_ENABLED)
        _appOpenEnabled.value = config.getBoolean(KEY_ADS_ENABLED) &&
                config.getBoolean(KEY_APP_OPEN_ENABLED)

        Log.d("AdManager", "Config applied: " +
                "ads=${_adsEnabled.value} " +
                "banner=${_bannerEnabled.value} " +
                "native=${_nativeEnabled.value} " +
                "appOpen=${_appOpenEnabled.value} " +
                "testAds=$USE_TEST_ADS")

        if (_adsEnabled.value) {
            if (interstitialAd == null) preloadInterstitial(appCtx)
            if (_nativeEnabled.value) preloadNativeAds()
        }
    }

    // Cooldown: blocks app open ad briefly after interstitial dismisses
    private var interstitialDismissedAt = 0L
    private const val INTERSTITIAL_COOLDOWN_MS = 2000L // 2 seconds

    fun isInterstitialRecentlyActive(): Boolean {
        return isInterstitialShowing ||
                (System.currentTimeMillis() - interstitialDismissedAt < INTERSTITIAL_COOLDOWN_MS)
    }

    // ── Ad unit ID getters (all honor USE_TEST_ADS) ───────────────────────

    fun isAppOpenAdEnabled(): Boolean {
        val config = Firebase.remoteConfig
        return config.getBoolean(KEY_ADS_ENABLED) &&
                config.getBoolean(KEY_APP_OPEN_ENABLED)
    }

    fun getAppOpenAdUnitId(): String {
        if (USE_TEST_ADS) return TEST_APP_OPEN_ID
        return Firebase.remoteConfig.getString(KEY_APP_OPEN_AD_UNIT).ifBlank { TEST_APP_OPEN_ID }
    }

    fun getBannerAdUnitId(): String {
        if (USE_TEST_ADS) return TEST_BANNER_ID
        return Firebase.remoteConfig.getString(KEY_BANNER_AD_UNIT).ifBlank { TEST_BANNER_ID }
    }

    fun getInterstitialAdUnitId(): String {
        if (USE_TEST_ADS) return TEST_INTERSTITIAL_ID
        return Firebase.remoteConfig.getString(KEY_INTERSTITIAL_AD_UNIT)
            .ifBlank { TEST_INTERSTITIAL_ID }
    }

    fun getNativeAdUnitId(): String {
        if (USE_TEST_ADS) return TEST_NATIVE_ID
        return Firebase.remoteConfig.getString(KEY_NATIVE_AD_UNIT).ifBlank { TEST_NATIVE_ID }
    }

    // ── Preload interstitial ──────────────────────────────────────────────
    fun preloadInterstitial(context: Context) {
        val config = Firebase.remoteConfig
        if (!config.getBoolean(KEY_ADS_ENABLED) ||
            !config.getBoolean(KEY_INTERSTITIAL_ENABLED)) return
        if (isLoadingInterstitial || interstitialAd != null) return

        isLoadingInterstitial = true
        val adUnitId = getInterstitialAdUnitId()
        val appContext = context.applicationContext

        InterstitialAd.load(
            appContext,
            adUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    isLoadingInterstitial = false
                    interstitialRetryCount = 0
                    interstitialAd = ad

                    // 💰 Revenue tracking — fires once per actual impression
                    ad.onPaidEventListener = OnPaidEventListener { adValue ->
                        RevenueTracker.recordPaidEvent(
                            adFormat = "interstitial",
                            adUnitId = adUnitId,
                            adValue = adValue,
                            responseInfo = ad.responseInfo
                        )
                    }
                    Log.d("AdManager", "Interstitial loaded ✅")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoadingInterstitial = false
                    interstitialAd = null
                    Log.e("AdManager", "Interstitial failed ❌ code=${error.code} ${error.message}")

                    // Retry with backoff (3s, 6s, 12s)
                    if (interstitialRetryCount < 3) {
                        val delay = 3000L shl interstitialRetryCount++
                        mainHandler.postDelayed({ preloadInterstitial(appContext) }, delay)
                    }
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

        isInterstitialShowing = true
        InAppMessagingManager.setSuppressed(true)

        interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                isInterstitialShowing = false
                InAppMessagingManager.setSuppressed(false)
                interstitialDismissedAt = System.currentTimeMillis()
                onDismiss()
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitialAd = null
                isInterstitialShowing = false
                InAppMessagingManager.setSuppressed(false)
                onDismiss()
            }
        }
        interstitialAd?.show(activity)
    }

    // ─────────────────────────────────────────────────────────────────────
    // Native ad POOL
    // ─────────────────────────────────────────────────────────────────────
    fun preloadNativeAds() {
        if (!::appCtx.isInitialized) return
        if (!_adsEnabled.value || !_nativeEnabled.value) return
        if (nativeLoader?.isLoading == true) return

        val needed = (NATIVE_POOL_TARGET - nativePool.size).coerceIn(0, 5)
        if (needed <= 0) return

        val adUnitId = getNativeAdUnitId()
        Log.d("AdManager", "preloadNativeAds: loading $needed ads, adUnitId=$adUnitId")

        val loader = AdLoader.Builder(appCtx, adUnitId)
            .forNativeAd { ad ->
                nativeRetryCount = 0

                // 💰 Revenue tracking for native ads
                ad.setOnPaidEventListener { adValue ->
                    RevenueTracker.recordPaidEvent(
                        adFormat = "native",
                        adUnitId = adUnitId,
                        adValue = adValue,
                        responseInfo = ad.responseInfo
                    )
                }

                nativePool.addLast(ad)
                _nativePoolSize.value = nativePool.size
                Log.d("AdManager", "Native pooled ✅ size=${nativePool.size}")
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e("AdManager", "Native failed ❌ code=${error.code} msg=${error.message}")
                    if (nativeRetryCount < 3) {
                        val delay = 2000L shl nativeRetryCount++   // 2s, 4s, 8s
                        mainHandler.postDelayed({ preloadNativeAds() }, delay)
                    }
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                    .build()
            )
            .build()

        nativeLoader = loader
        loader.loadAds(AdRequest.Builder().build(), needed)   // ONE request, several ads
    }

    /** Hands an ad to a card (the card owns and destroys it) and refills the pool. */
    fun takeNativeAd(): NativeAd? {
        val ad = nativePool.removeFirstOrNull()
        _nativePoolSize.value = nativePool.size
        preloadNativeAds()
        return ad
    }

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
            InAppMessagingManager.setSuppressed(true)

            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    isInterstitialShowing = false
                    InAppMessagingManager.setSuppressed(false)
                    interstitialDismissedAt = System.currentTimeMillis()
                    preloadInterstitial(activity)
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    interstitialAd = null
                    isInterstitialShowing = false
                    InAppMessagingManager.setSuppressed(false)
                    preloadInterstitial(activity)
                }
            }
            interstitialAd?.show(activity)
        } else {
            preloadInterstitial(activity)
        }
    }
}