package com.example.ui.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object AdMobManager {

    private const val TAG = "AdMobManager"

    @Volatile
    var isInitialized: Boolean = false
        private set

    private var interstitialAd: InterstitialAd? = null
    private var isLoadingInterstitial: Boolean = false

    fun initialize(context: Context) {
        try {
            MobileAds.initialize(context.applicationContext) { initStatus ->
                isInitialized = true
                Log.d(TAG, "Google MobileAds initialized: $initStatus")
                if (AdMobConfig.ENABLE_INTERSTITIAL_ADS) {
                    loadInterstitialAd(context.applicationContext)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MobileAds: ${e.message}")
        }
    }

    fun loadInterstitialAd(context: Context) {
        if (!AdMobConfig.ENABLE_INTERSTITIAL_ADS || isLoadingInterstitial || interstitialAd != null) {
            return
        }
        isLoadingInterstitial = true
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context.applicationContext,
            AdMobConfig.activeInterstitialAdUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoadingInterstitial = false
                    Log.d(TAG, "InterstitialAd loaded successfully.")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    interstitialAd = null
                    isLoadingInterstitial = false
                    Log.w(TAG, "InterstitialAd failed to load: ${loadAdError.message}")
                }
            }
        )
    }

    fun showInterstitialIfReady(activity: Activity, onDismissed: () -> Unit = {}) {
        val ad = interstitialAd
        if (!AdMobConfig.ENABLE_INTERSTITIAL_ADS || ad == null) {
            onDismissed()
            if (AdMobConfig.ENABLE_INTERSTITIAL_ADS) {
                loadInterstitialAd(activity.applicationContext)
            }
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                loadInterstitialAd(activity.applicationContext)
                onDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                interstitialAd = null
                loadInterstitialAd(activity.applicationContext)
                onDismissed()
            }
        }
        ad.show(activity)
    }
}
