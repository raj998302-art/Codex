package com.codex.carjam.monetize

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.mutableStateOf
import com.codex.carjam.game.Prefs
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * AdMob wrapper (GOOGLE TEST unit ids wired in — replace with your own before release).
 * Everything is skipped entirely when the No-Ads pack is owned.
 */
class AdsManager(
    private val context: Context,
    private val prefs: Prefs,
) {
    var rewardedReady = mutableStateOf(false)
        private set
    private var rewarded: RewardedAd? = null
    private var interstitial: InterstitialAd? = null
    private var initialized = false

    fun initialize() {
        if (initialized || prefs.removeAds.value) return
        initialized = true
        try {
            MobileAds.initialize(context) { }
        } catch (_: Throwable) {
        }
        loadRewarded()
        loadInterstitial()
    }

    fun loadRewarded() {
        if (prefs.removeAds.value) return
        try {
            RewardedAd.load(
                context,
                REWARDED_ID,
                AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        rewarded = ad
                        rewardedReady.value = true
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        rewarded = null
                        rewardedReady.value = false
                    }
                },
            )
        } catch (_: Throwable) {
            rewardedReady.value = false
        }
    }

    private fun loadInterstitial() {
        if (prefs.removeAds.value) return
        try {
            InterstitialAd.load(
                context,
                INTERSTITIAL_ID,
                AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        interstitial = ad
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        interstitial = null
                    }
                },
            )
        } catch (_: Throwable) {
        }
    }

    /** Shows the rewarded ad if ready; [onReward] fires with the earned coins. */
    fun showRewarded(activity: Activity, onReward: () -> Unit, onUnavailable: () -> Unit = {}) {
        val ad = rewarded
        if (prefs.removeAds.value || ad == null) {
            onUnavailable()
            // graceful fallback: still grant (offline / ad-blocked players keep playing)
            onReward()
            return
        }
        rewarded = null
        rewardedReady.value = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() = loadRewarded()

            override fun onAdFailedToShowFullScreenContent(e: AdError) = loadRewarded()
        }
        try {
            ad.show(activity) { onReward() }
        } catch (_: Throwable) {
            onReward()
        }
    }

    /** Interstitial every 3rd completed level, never when No-Ads is owned. */
    fun maybeShowInterstitial(activity: Activity, completedLevel: Int) {
        if (prefs.removeAds.value) return
        if (completedLevel % 3 != 0) return
        val ad = interstitial ?: return
        interstitial = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() = loadInterstitial()

            override fun onAdFailedToShowFullScreenContent(e: AdError) = loadInterstitial()
        }
        try {
            ad.show(activity)
        } catch (_: Throwable) {
        }
    }

    companion object {
        // Google-provided TEST ids — safe for development, serve no real ads.
        const val BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
        const val INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
        const val REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"
    }
}
