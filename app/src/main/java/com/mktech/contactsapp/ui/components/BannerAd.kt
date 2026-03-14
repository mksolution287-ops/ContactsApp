package com.mktech.contactsapp.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
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
//        modifier = modifier.fillMaxWidth().fillMaxWidth().height(50.dp),
//        factory  = { context ->
//            AdView(context).apply {
//                setAdSize(AdSize.BANNER)
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

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            val displayMetrics = context.resources.displayMetrics
            val adWidth = (displayMetrics.widthPixels / displayMetrics.density).toInt()
            val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth)

            AdView(context).apply {
                setAdSize(adSize)
                adUnitId = AdManager.getBannerAdUnitId()
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}