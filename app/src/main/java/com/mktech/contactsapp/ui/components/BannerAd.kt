package com.mktech.contactsapp.ui.components

import android.util.Log
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.mktech.contactsapp.util.AdManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

//@Composable
//fun BannerAd(modifier: Modifier = Modifier) {
//    val adsEnabled by AdManager.adsEnabled.collectAsState()
//    val bannerEnabled by AdManager.bannerEnabled.collectAsState()
//
//    if (!adsEnabled || !bannerEnabled) return
//
//    AndroidView(
//        modifier = modifier.fillMaxWidth(),
//        factory = { context ->
//            val displayMetrics = context.resources.displayMetrics
//            val adWidth = (displayMetrics.widthPixels / displayMetrics.density).toInt()
//            val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth)
//
//            AdView(context).apply {
//                setAdSize(adSize)
//                adUnitId = AdManager.getBannerAdUnitId()
//                loadAd(AdRequest.Builder().build())
//            }
//        }
//    )
//}

@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    val adsEnabled by AdManager.adsEnabled.collectAsState()
    val bannerEnabled by AdManager.bannerEnabled.collectAsState()

    if (!adsEnabled || !bannerEnabled) return

    val context = LocalContext.current

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            val displayMetrics = ctx.resources.displayMetrics
            val adWidthPixels = displayMetrics.widthPixels
            val density = displayMetrics.density
            val adWidth = (adWidthPixels / density).toInt()

            // Use Large Anchored Adaptive Banner (Recommended by Google)
            val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, adWidth)

            AdView(ctx).apply {
                setAdSize(adSize)
//                adUnitId = "ca-app-pub-3940256099942544/6300978111"   // Official Test Banner ID
                adUnitId = AdManager.getBannerAdUnitId()

                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        Log.d("BannerAd", "✅ Banner LOADED successfully")
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.e("BannerAd", "❌ Banner FAILED: ${error.code} - ${error.message}")
                    }

                    override fun onAdImpression() {
                        Log.d("BannerAd", "Banner impression recorded")
                    }
                }

                loadAd(AdRequest.Builder().build())
            }
        }
    )
}