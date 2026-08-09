package com.codex.carjam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import com.codex.carjam.monetize.RazorpayManager
import com.codex.carjam.ui.CarJamApp
import com.razorpay.PaymentResultListener

class MainActivity : ComponentActivity(), PaymentResultListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = false
        }
        setContent {
            CarJamApp()
        }
    }

    // Razorpay Checkout delivers results to the host activity's listener; we just
    // forward them to whichever manager is bound to the UI right now.
    override fun onPaymentSuccess(razorpayPaymentId: String?) {
        RazorpayManager.active?.handleSuccess(razorpayPaymentId)
    }

    override fun onPaymentError(code: Int, response: String?) {
        RazorpayManager.active?.handleError(code, response)
    }
}
