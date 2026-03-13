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
    private const val TEST_NATIVE_ID = "ca-app-pub-3940256099942544/2247696110"

    // ── Remote Config keys ────────────────────────────────────────────────
    private const val KEY_ADS_ENABLED              = "ads_enabled"
    private const val KEY_BANNER_ENABLED           = "banner_ad_enabled"
    private const val KEY_INTERSTITIAL_ENABLED     = "interstitial_ad_enabled"
    private const val KEY_NATIVE_ENABLED  = "native_ad_enabled"
    private const val KEY_INTERSTITIAL_TRIGGER     = "interstitial_trigger_count"
    private const val KEY_BANNER_AD_UNIT           = "banner_ad_unit_id"
    private const val KEY_INTERSTITIAL_AD_UNIT     = "interstitial_ad_unit_id"
    private const val KEY_NATIVE_AD_UNIT  = "native_ad_unit_id"

    // ── State ─────────────────────────────────────────────────────────────
    private var interstitialAd: InterstitialAd? = null
    private var actionCount = 0

    private val _adsEnabled = MutableStateFlow(true)
    val adsEnabled: StateFlow<Boolean> = _adsEnabled

    private val _bannerEnabled = MutableStateFlow(true)
    val bannerEnabled: StateFlow<Boolean> = _bannerEnabled

    // ── Native Ad state ───────────────────────────────────────────────────────
    private var nativeAd: NativeAd? = null
    private val _nativeAdReady = MutableStateFlow(false)
    val nativeAdReady: StateFlow<Boolean> = _nativeAdReady

    // ── Remote Config defaults ────────────────────────────────────────────
    private val remoteConfigDefaults = mapOf(
        KEY_ADS_ENABLED          to true,
        KEY_BANNER_ENABLED       to true,
        KEY_INTERSTITIAL_ENABLED to true,
        KEY_INTERSTITIAL_TRIGGER to 3L,
        KEY_BANNER_AD_UNIT       to TEST_BANNER_ID,
        KEY_INTERSTITIAL_AD_UNIT to TEST_INTERSTITIAL_ID,
    )

    // ── Init Remote Config and fetch ──────────────────────────────────────
    fun init(context: Context) {
        val remoteConfig = Firebase.remoteConfig
        remoteConfig.setConfigSettingsAsync(
            remoteConfigSettings {
                // Fetch every 1 hour in production (minimum 1 hour)
                // Use 0 for debug builds
                minimumFetchIntervalInSeconds = 3600
            }
        )
        remoteConfig.setDefaultsAsync(remoteConfigDefaults)
        fetchRemoteConfig()
        preloadInterstitial(context)
        preloadNativeAd(context)
    }

    fun fetchRemoteConfig() {
        Firebase.remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val config = Firebase.remoteConfig
                _adsEnabled.value    = config.getBoolean(KEY_ADS_ENABLED)
                _bannerEnabled.value = config.getBoolean(KEY_ADS_ENABLED) &&
                        config.getBoolean(KEY_BANNER_ENABLED)
                Log.d("AdManager", "Remote config fetched: ads=${_adsEnabled.value}")
            }
        }
    }

    // ── Banner ad unit ID from Remote Config ──────────────────────────────
    fun getBannerAdUnitId(): String {
        return Firebase.remoteConfig.getString(KEY_BANNER_AD_UNIT)
            .ifBlank { TEST_BANNER_ID }
    }

    // ── Preload interstitial ───────────────────────────────────────────────
    fun preloadInterstitial(context: Context) {
        val config  = Firebase.remoteConfig
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
                    Log.d("AdManager", "Interstitial loaded")
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    Log.e("AdManager", "Interstitial failed: ${error.message}")
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
            showInterstitial(activity) {
                preloadInterstitial(context) // preload next one
            }
        }
    }

    // ── Show interstitial if ready ────────────────────────────────────────
    private fun showInterstitial(activity: Activity?, onDismiss: () -> Unit) {
        if (activity == null || interstitialAd == null) {
            onDismiss()
            return
        }
        interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                onDismiss()
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitialAd = null
                onDismiss()
            }
        }
        interstitialAd?.show(activity)
    }

    // ── Preload native ad ─────────────────────────────────────────────────────
    fun preloadNativeAd(context: Context) {
        if (!_adsEnabled.value) return

        val adUnitId = try {
            Firebase.remoteConfig.getString(KEY_NATIVE_AD_UNIT).ifBlank { TEST_NATIVE_ID }
        } catch (e: Exception) {
            TEST_NATIVE_ID
        }

        Log.d("AdManager", "preloadNativeAd: loading adUnitId=$adUnitId")

        val adLoader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { ad ->
                // Destroy old ad before replacing
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

    // ── Get the loaded native ad (call after nativeAdReady = true) ───────────
    fun getNativeAd(): NativeAd? = nativeAd

    // ── Must call when the screen/composable is destroyed ────────────────────
    fun destroyNativeAd() {
        nativeAd?.destroy()
        nativeAd = null
        _nativeAdReady.value = false
    }
}