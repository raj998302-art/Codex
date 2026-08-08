package com.codex.carjam.monetize

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.mutableStateOf
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.PlayGamesSdk

/**
 * Google Play Games Services sign-in (PGS v2). Everything degrades gracefully:
 * until the game is registered with Play Games Services in a Play Console
 * project, sign-in simply reports "not connected" and the local profile stays
 * the source of truth. Zero crashes, zero blocking.
 */
class PlayGamesManager(private val context: Context) {

    var signedIn = mutableStateOf(false)
        private set
    var gamerName = mutableStateOf<String?>(null)
        private set

    private var silentTried = false

    fun initialize() {
        try {
            PlayGamesSdk.initialize(context)
        } catch (_: Throwable) {
        }
    }

    /** Silent auth probe at startup — never pops any UI. */
    fun silentCheck(activity: Activity) {
        if (silentTried) return
        silentTried = true
        try {
            PlayGames.getGamesSignInClient(activity).isAuthenticated
                .addOnCompleteListener { task ->
                    val ok = task.isSuccessful &&
                        task.result != null &&
                        task.result.isAuthenticated
                    signedIn.value = ok
                    if (ok) loadGamerName(activity)
                }
        } catch (_: Throwable) {
            signedIn.value = false
        }
    }

    /** Interactive sign-in — shows the official Play Games sheet. */
    fun signIn(activity: Activity) {
        try {
            PlayGames.getGamesSignInClient(activity).signIn()
                .addOnCompleteListener { task ->
                    val ok = task.isSuccessful &&
                        task.result != null &&
                        task.result.isAuthenticated
                    signedIn.value = ok
                    if (ok) loadGamerName(activity)
                }
        } catch (_: Throwable) {
            signedIn.value = false
        }
    }

    private fun loadGamerName(activity: Activity) {
        try {
            PlayGames.getPlayersClient(activity).currentPlayer
                .addOnSuccessListener { p -> gamerName.value = p?.displayName }
        } catch (_: Throwable) {
        }
    }
}
