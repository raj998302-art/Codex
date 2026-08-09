package com.codex.carjam.game

import androidx.compose.runtime.mutableStateOf
import org.json.JSONObject

/**
 * Cloud save orchestrator.
 *
 * Every sync = POST our snapshot; the server answers with the canonical state
 * (already clamped to the absolute caps and the per-hour growth budget, so a
 * hacked client can never inflate the cloud). We MAX-merge that into local
 * progress, then push once more if we were ahead anywhere — convergence in
 * at most two round trips.
 *
 * Anti-cheat rails around it:
 *  - local wallet writes additionally pass SecureVault checksums (see Prefs);
 *  - no-ads NEVER travels through the cloud — Play Billing restore is the only
 *    path for purchases, so a forged save cannot unlock paid content;
 *  - the sync key is a 256-bit random whose hash alone identifies the save.
 */
object CloudSave {

    /** true = synced, false = offline/error, null = syncing/never tried. */
    val status = mutableStateOf<Boolean?>(null)
    val syncing = mutableStateOf(false)

    private var lastTryMs = 0L

    fun sync(prefs: Prefs, force: Boolean = false, onDone: ((Boolean) -> Unit)? = null) {
        if (!LeaderboardApi.CONFIGURED) {
            status.value = false
            onDone?.invoke(false)
            return
        }
        if (syncing.value) {
            onDone?.invoke(status.value == true)
            return
        }
        val now = System.currentTimeMillis()
        if (!force && now - lastTryMs < 45_000L) {
            onDone?.invoke(true)
            return
        }
        lastTryMs = now
        syncing.value = true
        status.value = null
        val snap = snapshot(prefs)
        LeaderboardApi.pushSave(prefs.syncKey, prefs.deviceId, snap) { canonical ->
            syncing.value = false
            if (canonical == null) {
                status.value = false
                onDone?.invoke(false)
                return@pushSave
            }
            status.value = true
            val ahead = localAhead(snap, canonical)
            prefs.applyCloudRestore(canonical)
            SoundManager.active?.applyMusicPref() // merged musicOn reaches the live player
            prefs.noteCloudSync()
            if (ahead) {
                LeaderboardApi.pushSave(prefs.syncKey, prefs.deviceId, snapshot(prefs), null)
            }
            onDone?.invoke(true)
        }
    }

    fun snapshot(prefs: Prefs): JSONObject = JSONObject()
        .put("name", prefs.playerName)
        .put("avatarId", prefs.avatarId.intValue)
        .put("coins", prefs.coins.intValue)
        .put("gems", prefs.gems.intValue)
        .put("maxLevel", prefs.maxLevel.intValue)
        .put("totalCoinsEarned", prefs.totalCoinsEarned.intValue)
        .put("dailyStreak", prefs.dailyStreak.intValue)
        .put("lastClaimDay", prefs.lastClaimDay.longValue)
        .put("wins", prefs.wins.intValue)
        .put("losses", prefs.losses.intValue)
        .put("practiceWins", prefs.practiceWins.intValue)
        .put("referredBy", prefs.referredBy.value ?: "")
        .put("welcomed", prefs.welcomed.value)
        .put("musicOn", prefs.musicOn.value)
        .put("rides", prefs.ownedRides.value.joinToString(","))
        .put("rideSel", prefs.selectedRide.value)
        .put("hammers", prefs.hammers.intValue)
        .put("shuffles", prefs.shufflesStock.intValue)
        .put("elims", prefs.elims.intValue)
        .put("refreshes", prefs.refreshes.intValue)
        .put("piggy", prefs.piggy.intValue)
        .put("frames", prefs.framesTaggedCsv())
        .put("frameSel", prefs.avatarFrame.intValue)
        .put("scene", prefs.manualScene.value ?: "")
        .put("sceneAuto", prefs.autoSceneSwitch.value)
        .put("vehPack", prefs.hasVehiclePack.value)

    private val NUM_KEYS = listOf(
        "coins", "gems", "maxLevel", "totalCoinsEarned",
        "dailyStreak", "wins", "losses", "practiceWins",
    )

    private fun localAhead(local: JSONObject, canonical: JSONObject): Boolean {
        for (k in NUM_KEYS) if (local.optLong(k) > canonical.optLong(k)) return true
        return false
    }
}
