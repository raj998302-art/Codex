package com.codex.carjam.monetize

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.mutableStateOf
import com.codex.carjam.game.Prefs

/**
 * STUB (bisect build): same public API as the real AdMob wrapper but with zero
 * external dependencies. Rewarded flows grant immediately, interstitials are
 * skipped, banner is a UI placeholder. Real SDK version restored afterwards.
 */
class AdsManager(
    @Suppress("unused") private val context: Context,
    private val prefs: Prefs,
) {
    var rewardedReady = mutableStateOf(true)
        private set

    fun initialize() {
        rewardedReady.value = !prefs.removeAds.value
    }

    fun loadRewarded() {
        rewardedReady.value = !prefs.removeAds.value
    }

    /** Stub: no ad to show — reward immediately so gameplay never blocks. */
    fun showRewarded(activity: Activity, onReward: () -> Unit, onUnavailable: () -> Unit = {}) {
        onUnavailable()
        onReward()
    }

    fun maybeShowInterstitial(activity: Activity, completedLevel: Int) {
        // stub: no-op
    }

    companion object {
        // Google-provided TEST ids — kept for API parity with the real wrapper.
        const val BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
        const val INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
        const val REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"
    }
}
