package com.codex.carjam.game

import androidx.compose.runtime.mutableStateOf

/**
 * Real-time leaderboard state, fed by the Render+MongoDB backend through
 * [LeaderboardApi]. No demo data anywhere: the list is exactly what the server
 * answers. [live] is null while the first sync is in flight, true when the
 * server answered, false when offline / server unreachable / not yet deployed.
 * A sync = push our score, then pull the top — cheap, effective and safe to
 * call liberally (calls are de-bounced to once per 15 s unless forced).
 */
object LiveBoard {

    val entries = mutableStateOf<List<LeaderboardApi.Entry>>(emptyList())
    val me = mutableStateOf<LeaderboardApi.Entry?>(null)
    val live = mutableStateOf<Boolean?>(null)

    private var lastFetchMs = 0L
    private var fetching = false

    fun sync(prefs: Prefs, force: Boolean = false) {
        if (!LeaderboardApi.CONFIGURED) {
            live.value = false
            return
        }
        if (fetching) return
        val now = System.currentTimeMillis()
        if (!force && now - lastFetchMs < 15_000L) return
        fetching = true
        lastFetchMs = now
        LeaderboardApi.pushScore(prefs) { pushed ->
            LeaderboardApi.fetchTop(prefs.deviceId) { top, mine ->
                fetching = false
                if (top != null) {
                    entries.value = top
                    me.value = mine
                    live.value = true
                } else {
                    live.value = if (pushed) live.value else false
                }
            }
        }
    }
}
