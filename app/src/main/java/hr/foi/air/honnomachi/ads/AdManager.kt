package hr.foi.air.honnomachi.ads

import android.content.Context
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import hr.foi.air.honnomachi.CrashlyticsManager
import javax.inject.Inject
import javax.inject.Named

interface AdManager {
    fun createBannerAdView(
        context: Context,
        onAdLoaded: () -> Unit,
        onAdFailed: (String) -> Unit,
    ): AdView
}

class AdManagerImpl
    @Inject
    constructor(
        @Named("bannerAdUnitId") private val adUnitId: String,
    ) : AdManager {
        override fun createBannerAdView(
            context: Context,
            onAdLoaded: () -> Unit,
            onAdFailed: (String) -> Unit,
        ): AdView {
            val adView =
                AdView(context).apply {
                    setAdSize(AdSize.BANNER)
                    adUnitId = this@AdManagerImpl.adUnitId
                    adListener =
                        object : AdListener() {
                            override fun onAdLoaded() {
                                onAdLoaded()
                            }

                            override fun onAdFailedToLoad(error: LoadAdError) {
                                val message = "Ad failed to load: ${error.code} - ${error.message}"
                                CrashlyticsManager.instance.logException(
                                    RuntimeException(message),
                                )
                                onAdFailed(message)
                            }
                        }
                }
            adView.loadAd(AdRequest.Builder().build())
            return adView
        }
    }
