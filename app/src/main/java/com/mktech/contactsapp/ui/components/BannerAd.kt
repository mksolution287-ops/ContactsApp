package com.mktech.contactsapp.ui.components

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnPaidEventListener
import com.mktech.contactsapp.util.AdManager
import com.mktech.contactsapp.util.RevenueTracker

private const val BANNER_MAX_RETRIES = 3

@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    val adsEnabled by AdManager.adsEnabled.collectAsState()
    val bannerEnabled by AdManager.bannerEnabled.collectAsState()

    if (!adsEnabled || !bannerEnabled) return

    val context = LocalContext.current
    val handler = remember { Handler(Looper.getMainLooper()) }

    // Created once per composition so it can be destroyed when the banner leaves the screen.
    val adView = remember {
        val displayMetrics = context.resources.displayMetrics
        val adWidth = (displayMetrics.widthPixels / displayMetrics.density).toInt()

        // Anchored Adaptive Banner (recommended by Google)
        val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth)
        val bannerAdUnitId = AdManager.getBannerAdUnitId()   // honors USE_TEST_ADS

        AdView(context).apply {
            setAdSize(adSize)
            adUnitId = bannerAdUnitId

            // 💰 Revenue tracking — banners fire this once per refreshed impression
            setOnPaidEventListener(OnPaidEventListener { adValue ->
                RevenueTracker.recordPaidEvent(
                    adFormat = "banner",
                    adUnitId = bannerAdUnitId,
                    adValue = adValue,
                    responseInfo = this.responseInfo
                )
            })

            var retryCount = 0
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    retryCount = 0
                    Log.d("BannerAd", "✅ Banner LOADED successfully")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e("BannerAd", "❌ Banner FAILED: ${error.code} - ${error.message}")
                    // Retry with backoff (3s, 6s, 12s) so one no-fill doesn't leave the slot empty
                    if (retryCount < BANNER_MAX_RETRIES) {
                        val delay = 3000L shl retryCount++
                        handler.postDelayed({ loadAd(AdRequest.Builder().build()) }, delay)
                    }
                }

                override fun onAdImpression() {
                    Log.d("BannerAd", "Banner impression recorded")
                }
            }

            loadAd(AdRequest.Builder().build())
        }
    }

    DisposableEffect(adView) {
        onDispose {
            handler.removeCallbacksAndMessages(null)
            adView.destroy()
        }
    }

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { adView }
    )
}