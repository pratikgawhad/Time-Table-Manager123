package com.example.ui.ads

/**
 * AdMob Configuration for Time Table Manager
 *
 * Configured with REAL AdMob IDs per request.
 * Uses official Google Mobile Ads SDK (`com.google.android.gms:play-services-ads`).
 */
object AdMobConfig {

    // ─────────────────────────────────────────────────────────────────────────────
    // ⚙️ TESTING MODE FLAG:
    // When false -> uses your REAL AdMob IDs for Play Store release
    // When true  -> uses Google Test Ad IDs for emulator/debug verification
    // ─────────────────────────────────────────────────────────────────────────────
    var useTestAds: Boolean = false

    // REAL ADMOB IDs
    const val REAL_APP_ID = "ca-app-pub-7527835129661450~4783640462"
    const val REAL_BANNER_AD_UNIT_ID = "ca-app-pub-7527835129661450/9591356425"
    const val REAL_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

    // TEST ADMOB IDs (for debug / preview verification)
    const val TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713"
    const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

    // Banner Ads are active; set ENABLE_INTERSTITIAL_ADS = true if you also want Interstitial Ads
    const val ENABLE_INTERSTITIAL_ADS = false
    const val INTERSTITIAL_AD_UNIT_ID = TEST_INTERSTITIAL_AD_UNIT_ID

    // Active Banner Unit ID based on test mode flag
    val activeBannerAdUnitId: String
        get() = if (useTestAds) TEST_BANNER_AD_UNIT_ID else REAL_BANNER_AD_UNIT_ID

    // Active Interstitial Unit ID based on test mode flag
    val activeInterstitialAdUnitId: String
        get() = if (useTestAds) TEST_INTERSTITIAL_AD_UNIT_ID else REAL_INTERSTITIAL_AD_UNIT_ID
}
