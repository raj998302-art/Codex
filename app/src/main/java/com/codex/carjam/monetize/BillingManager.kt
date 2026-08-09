package com.codex.carjam.monetize

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.codex.carjam.game.Prefs

/**
 * Google Play Billing wrapper. Create the matching products in Play Console:
 *   remove_ads  (non-consumable, ₹99)
 *   coins_120 / coins_400 / coins_1000 / coins_2500 (consumables)
 * Everything degrades gracefully when Play Billing is unavailable (F-Droid builds,
 * emulators without Play services) — buttons then just show the default price.
 */
class BillingManager(
    private val context: Context,
    private val prefs: Prefs,
) : PurchasesUpdatedListener {

    var ready = mutableStateOf(false)
        private set
    val products = mutableStateMapOf<String, ProductDetails>()

    private val client: BillingClient? = try {
        BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder().enableOneTimeProducts().build(),
            )
            .build()
    } catch (t: Throwable) {
        null
    }

    init {
        connect()
    }

    private fun connect() {
        val c = client ?: return
        try {
            c.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        ready.value = true
                        queryProducts()
                        restorePurchases()
                    }
                }

                override fun onBillingServiceDisconnected() {
                    ready.value = false
                }
            })
        } catch (_: Throwable) {
        }
    }

    private fun queryProducts() {
        val c = client ?: return
        val list = PRODUCT_IDS.map { id ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(id)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder().setProductList(list).build()
        try {
            c.queryProductDetailsAsync(params) { result, detailsList ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    for (d in detailsList) products[d.productId] = d
                }
            }
        } catch (_: Throwable) {
        }
    }

    fun priceFor(productId: String): String? =
        // PBL 7.x exposes oneTimePurchaseOfferDetails as a Kotlin PROPERTY (getter),
        // not a function — calling it with () fails to compile.
        products[productId]?.oneTimePurchaseOfferDetails?.formattedPrice

    fun launchPurchase(activity: Activity, productId: String) {
        val c = client ?: return
        val details = products[productId] ?: run {
            // Product not created in Play Console (or store unavailable): try once more later.
            queryProducts()
            return
        }
        val pdParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .build()
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(pdParams))
            .build()
        try {
            c.launchBillingFlow(activity, flowParams)
        } catch (_: Throwable) {
        }
    }

    fun restorePurchases() {
        val c = client ?: return
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        try {
            c.queryPurchasesAsync(params) { result, purchases ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    for (p in purchases) handlePurchase(p)
                }
            }
        } catch (_: Throwable) {
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (p in purchases) handlePurchase(p)
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        for (productId in purchase.products) {
            when (productId) {
                PRODUCT_NO_ADS -> {
                    prefs.setRemoveAds(true)
                    if (!purchase.isAcknowledged) {
                        val params = AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase.purchaseToken)
                            .build()
                        try {
                            client?.acknowledgePurchase(params) { }
                        } catch (_: Throwable) {
                        }
                    }
                }

                else -> {
                    val reward = PRODUCT_REWARDS[productId]
                    if (reward != null && (reward.first > 0 || reward.second > 0)) {
                        if (reward.first > 0) prefs.addCoins(reward.first)
                        if (reward.second > 0) prefs.addGems(reward.second)
                        val params = ConsumeParams.newBuilder()
                            .setPurchaseToken(purchase.purchaseToken)
                            .build()
                        try {
                            client?.consumeAsync(params) { _, _ -> }
                        } catch (_: Throwable) {
                        }
                    }
                }
            }
        }
    }

    fun release() {
        try {
            client?.endConnection()
        } catch (_: Throwable) {
        }
        ready.value = false
    }

    companion object {
        const val PRODUCT_NO_ADS = "remove_ads"
        const val PRICE_NO_ADS_DEFAULT = "₹99"

        /** productId → (coins, gems) granted on purchase. */
        val PRODUCT_REWARDS: Map<String, Pair<Int, Int>> = mapOf(
            "coins_120" to (120 to 0),
            "coins_400" to (400 to 0),
            "coins_1000" to (1000 to 0),
            "coins_2500" to (2500 to 0),
            "gems_80" to (0 to 80),
            "gems_250" to (0 to 250),
            "gems_700" to (0 to 700),
        )

        val COINS_BY_PRODUCT: Map<String, Int> = PRODUCT_REWARDS
            .filterValues { it.first > 0 }
            .mapValues { it.value.first }

        val GEMS_BY_PRODUCT: Map<String, Int> = PRODUCT_REWARDS
            .filterValues { it.second > 0 }
            .mapValues { it.value.second }

        val DEFAULT_PRICES: Map<String, String> = mapOf(
            "coins_120" to "₹29",
            "coins_400" to "₹79",
            "coins_1000" to "₹149",
            "coins_2500" to "₹299",
            "gems_80" to "₹49",
            "gems_250" to "₹129",
            "gems_700" to "₹299",
        )

        val PRODUCT_IDS: List<String> = listOf(PRODUCT_NO_ADS) + PRODUCT_REWARDS.keys
    }
}
