package com.codex.carjam.monetize

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.mutableStateOf
import com.codex.carjam.game.Prefs

/**
 * STUB (bisect build): same public API as the real Play Billing wrapper but with
 * zero external dependencies, so CI can isolate whether the build failure comes
 * from the billing/ads SDKs or from our own code. Real SDK version restored after
 * the culprit is found. Prices fall back to the documented defaults.
 */
class BillingManager(
    @Suppress("unused") private val context: Context,
    @Suppress("unused") private val prefs: Prefs,
) {
    var ready = mutableStateOf(false)
        private set

    fun priceFor(productId: String): String? =
        if (productId == PRODUCT_NO_ADS) PRICE_NO_ADS_DEFAULT else DEFAULT_PRICES[productId]

    fun launchPurchase(activity: Activity, productId: String) {
        // stub: no Play Billing in this build
    }

    fun restorePurchases() {
        // stub
    }

    fun release() {
        ready.value = false
    }

    companion object {
        const val PRODUCT_NO_ADS = "remove_ads"
        const val PRICE_NO_ADS_DEFAULT = "₹99"

        val COINS_BY_PRODUCT: Map<String, Int> = mapOf(
            "coins_120" to 120,
            "coins_400" to 400,
            "coins_1000" to 1000,
            "coins_2500" to 2500,
        )

        val DEFAULT_PRICES: Map<String, String> = mapOf(
            "coins_120" to "₹29",
            "coins_400" to "₹79",
            "coins_1000" to "₹149",
            "coins_2500" to "₹299",
        )

        val PRODUCT_IDS: List<String> = listOf(PRODUCT_NO_ADS) + COINS_BY_PRODUCT.keys
    }
}
