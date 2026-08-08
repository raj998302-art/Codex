package com.codex.carjam.game

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/** Tiny connectivity helper used by the splash gate and offline UI. */
object Net {
    fun isOnline(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return true // can't tell → assume online, never block the player
            val net = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(net) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Throwable) {
            true
        }
    }
}
