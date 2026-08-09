package com.codex.carjam.game

import com.codex.carjam.game.render.GameIconKind

/**
 * Daily missions: three seeded tasks per UTC day, progress tracked from
 * day-anchored baselines (Prefs.ensureMissionDay handles rollover). One claim
 * per mission per day — practice-mode events never count (see GameScreen).
 */
object DailyMissions {

    enum class Metric { WINS, BOARDS, EARNED, LEVEL }

    data class Mission(
        val id: String,
        val title: String,
        val target: Int,
        val metric: Metric,
        val coins: Int,
        val gems: Int,
        val icon: GameIconKind,
    )

    private val pool = listOf(
        Mission("win1", "Win 1 game", 1, Metric.WINS, 40, 0, GameIconKind.TROPHY),
        Mission("board25", "Seat 25 passengers", 25, Metric.BOARDS, 60, 0, GameIconKind.PARKING),
        Mission("earn300", "Collect 300 coins", 300, Metric.EARNED, 50, 0, GameIconKind.COIN_STACK),
        Mission("win3", "Win 3 games", 3, Metric.WINS, 90, 4, GameIconKind.TROPHY),
        Mission("board60", "Seat 60 passengers", 60, Metric.BOARDS, 110, 6, GameIconKind.PARKING),
        Mission("earn800", "Collect 800 coins", 800, Metric.EARNED, 90, 5, GameIconKind.COIN_STACK),
        Mission("level2", "Reach 2 new levels", 2, Metric.LEVEL, 80, 5, GameIconKind.BOLT),
        Mission("win5", "Win 5 games", 5, Metric.WINS, 140, 8, GameIconKind.TROPHY),
    )

    fun todayEpoch(): Int = (System.currentTimeMillis() / 86_400_000L).toInt()

    /** Deterministic 3-mission draw for the epoch day (same for everyone that day). */
    fun todaysMissions(epochDay: Int = todayEpoch()): List<Mission> {
        val rng = java.util.Random(epochDay * 2654435761L + 97)
        val idx = pool.indices.shuffled(rng).take(3)
        return idx.map { pool[it] }
    }

    fun progress(prefs: Prefs, epochDay: Int, mission: Mission): Int {
        val bars = prefs.ensureMissionDay(epochDay)
        val (cur, base) = when (mission.metric) {
            Metric.WINS -> prefs.wins.intValue.toLong() to bars.wins
            Metric.BOARDS -> prefs.boardsTotal.intValue.toLong() to bars.boards
            Metric.EARNED -> prefs.totalCoinsEarned.intValue.toLong() to bars.earned
            Metric.LEVEL -> prefs.maxLevel.intValue.toLong() to bars.level
        }
        return (cur - base).coerceAtLeast(0L).coerceAtMost(mission.target.toLong()).toInt()
    }

    private fun isClaimed(prefs: Prefs, epochDay: Int, index: Int): Boolean {
        prefs.ensureMissionDay(epochDay)
        return (prefs.missionClaimed.intValue and (1 shl index)) != 0
    }

    fun canClaim(prefs: Prefs, epochDay: Int, mission: Mission, index: Int): Boolean =
        !isClaimed(prefs, epochDay, index) && progress(prefs, epochDay, mission) >= mission.target

    /** Idempotent within the day; returns the coin grant (0 when not claimable). */
    fun claim(prefs: Prefs, epochDay: Int, mission: Mission, index: Int): Int {
        if (!canClaim(prefs, epochDay, mission, index)) return 0
        prefs.setMissionClaimBit(index)
        if (mission.coins > 0) prefs.addCoins(mission.coins)
        if (mission.gems > 0) prefs.addGems(mission.gems)
        return mission.coins
    }

    fun anyClaimable(prefs: Prefs): Boolean {
        val day = todayEpoch()
        return todaysMissions(day).mapIndexedNotNull { i, m -> if (canClaim(prefs, day, m, i)) m else null }
            .isNotEmpty()
    }
}
