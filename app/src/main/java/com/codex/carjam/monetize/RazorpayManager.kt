package com.codex.carjam.monetize

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.mutableStateOf
import com.codex.carjam.game.Prefs
import com.razorpay.Checkout
import org.json.JSONObject

/**
 * Razorpay checkout (UPI / cards / netbanking / wallets) — meant for the
 * DIRECT-DISTRIBUTION build (website APK, third-party stores).
 *
 * GOOGLE PLAY POLICY: digital goods sold inside a Play-Store build must use
 * Google Play Billing. That's why this whole module ships DISABLED in the Play
 * build. To light it up for a direct-distribution flavor:
 *   1. Get your Key ID from dashboard.razorpay.com → Settings → API Keys.
 *   2. Set ENABLED = true and paste it into KEY_ID.
 *   3. (Optional, server-side) verify payment signatures before granting; on a
 *      pure client build the wallet grant below happens on the success callback.
 * Every call is exception-proof so a bad key can never crash the game.
 */
class RazorpayManager(private val context: Context, private val prefs: Prefs) {

    /** Latest user-visible payment status (success/failure toast line in the shop). */
    var status = mutableStateOf<String?>(null)
        private set

    private var pendingProduct: String? = null
    private var pendingCoins = 0
    private var pendingGems = 0

    val enabled: Boolean get() = ENABLED

    /** Warm the checkout SDK so the sheet opens instantly (call once at startup). */
    fun preload() {
        if (!ENABLED) return
        try {
            Checkout.preload(context.applicationContext)
        } catch (_: Throwable) {
        }
    }

    /** Opens the official Razorpay sheet for [productId] (id shared with BillingManager). */
    fun buy(activity: Activity, productId: String) {
        if (!ENABLED) return
        val priceInr = PRICES_INR[productId] ?: return
        status.value = null
        try {
            val co = Checkout()
            co.setKeyID(KEY_ID)
            try {
                co.setImage(com.codex.carjam.R.drawable.hero_car)
            } catch (_: Throwable) {
            }
            val options = JSONObject()
            options.put("name", "Car Jam Solver")
            options.put("description", LABELS[productId] ?: productId)
            options.put("currency", "INR")
            options.put("amount", priceInr * 100) // paise
            options.put("theme.color", "#0FA3E0")
            val retry = JSONObject()
            retry.put("enabled", true)
            retry.put("max_count", 3)
            options.put("retry", retry)

            pendingProduct = productId
            val reward = BillingManager.PRODUCT_REWARDS[productId]
            pendingCoins = reward?.first ?: 0
            pendingGems = reward?.second ?: 0
            co.open(activity, options)
        } catch (_: Throwable) {
            pendingProduct = null
            status.value = "Couldn't start Razorpay checkout"
        }
    }

    /** Called by MainActivity (it implements PaymentResultListener for the SDK). */
    fun handleSuccess(paymentId: String?) {
        val product = pendingProduct
        pendingProduct = null
        if (product == null) return
        if (product == BillingManager.PRODUCT_NO_ADS) prefs.setRemoveAds(true)
        if (pendingCoins > 0) prefs.addCoins(pendingCoins)
        if (pendingGems > 0) prefs.addGems(pendingGems)
        status.value = "Payment successful — ${LABELS[product] ?: product} added!"
    }

    fun handleError(code: Int, response: String?) {
        pendingProduct = null
        status.value = "Payment cancelled or failed"
    }

    companion object {
        // ---- the two switches for the direct-distribution build ----
        const val ENABLED = false
        const val KEY_ID = "rzp_test_REPLACE_WITH_YOUR_KEY"

        /** The manager currently bound to the UI (MainActivity forwards SDK callbacks here). */
        var active: RazorpayManager? = null

        /** Mirror of the Play Billing catalogue prices (whole ₹). */
        val PRICES_INR: Map<String, Int> = mapOf(
            BillingManager.PRODUCT_NO_ADS to 99,
            "coins_120" to 29,
            "coins_400" to 79,
            "coins_1000" to 149,
            "coins_2500" to 299,
            "gems_80" to 49,
            "gems_250" to 129,
            "gems_700" to 299,
        )

        val LABELS: Map<String, String> = mapOf(
            BillingManager.PRODUCT_NO_ADS to "No Ads Forever",
            "coins_120" to "120 Coins",
            "coins_400" to "400 Coins",
            "coins_1000" to "1,000 Coins",
            "coins_2500" to "2,500 Coins",
            "gems_80" to "80 Gems",
            "gems_250" to "250 Gems",
            "gems_700" to "700 Gems",
        )
    }
}
