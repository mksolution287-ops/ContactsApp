package com.mktech.contactsapp.ui.components

import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.mktech.contactsapp.R
import com.mktech.contactsapp.util.AdManager

/**
 * Native ad card.
 *
 * Claims a preloaded ad from AdManager's pool (instant when the pool is warm),
 * owns it, and destroys only that ad when it leaves composition — so one screen
 * disposing never wipes the ad another screen is showing.
 */
@Composable
fun NativeAdCard(modifier: Modifier = Modifier) {
    val adsEnabled by AdManager.adsEnabled.collectAsState()
    val nativeEnabled by AdManager.nativeEnabled.collectAsState()
    val poolSize by AdManager.nativePoolSize.collectAsState()

    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }

    // Claim an ad the moment one is available (re-runs when the pool refills)
    LaunchedEffect(adsEnabled, nativeEnabled, poolSize) {
        if (adsEnabled && nativeEnabled && nativeAd == null) {
            nativeAd = AdManager.takeNativeAd()
        }
    }

    // Destroy only the ad this card owns
    DisposableEffect(Unit) {
        onDispose {
            nativeAd?.destroy()
            nativeAd = null
        }
    }

    val ad = nativeAd
    if (!adsEnabled || !nativeEnabled || ad == null) return

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            val themedCtx = ContextThemeWrapper(ctx, R.style.Theme_ContactsApp)
            val adView = LayoutInflater.from(themedCtx)
                .inflate(R.layout.native_ad_view, null) as NativeAdView
            adView.headlineView     = adView.findViewById(R.id.ad_headline)
            adView.bodyView         = adView.findViewById(R.id.ad_body)
            adView.iconView         = adView.findViewById(R.id.ad_app_icon)
            adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
            adView
        },
        update = { adView ->
            // Populate each view with ad content
            (adView.headlineView as? TextView)?.text = ad.headline
            (adView.bodyView as? TextView)?.apply {
                text = ad.body
                visibility = if (ad.body == null) View.GONE else View.VISIBLE
            }
            (adView.callToActionView as? Button)?.apply {
                text = ad.callToAction
                visibility = if (ad.callToAction == null) View.GONE else View.VISIBLE
            }
            (adView.iconView as? ImageView)?.apply {
                val drawable = ad.icon?.drawable
                setImageDrawable(drawable)
                visibility = if (drawable == null) View.GONE else View.VISIBLE
            }

            // Required — registers all views for click tracking
            adView.setNativeAd(ad)
        }
    )
}