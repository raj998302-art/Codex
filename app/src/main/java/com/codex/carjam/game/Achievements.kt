package com.codex.carjam.game

import com.codex.carjam.game.render.GameIconKind

/**
 * Lifetime achievements — milestones with one-time rewards. Each award tracks a
 * live metric from Prefs, shows progress in the QUESTS dialog, and pays out
 * once (claimed ids persisted in Prefs.achievementsClaimed).
 */
object Achievements {

    data class Award(
        val id: String,
        val title: String,
        val desc: String,
        val icon: GameIconKind,
        val target: Int,
        val coins: Int,
        val gems: Int,
        val value: (Prefs) -> Int,
    )

    val all = listOf(
        Award("first_win", "FIRST BLOOD", "Win your first game", GameIconKind.MEDAL_1, 1, 50, 0) { it.wins.intValue },
        Award("wins10", "TEN PACK", "Win 10 games", GameIconKind.MEDAL_2, 10, 100, 0) { it.wins.intValue },
        Award("wins50", "JAM MASTER", "Win 50 games", GameIconKind.MEDAL_3, 50, 250, 10) { it.wins.intValue },
        Award("wins250", "LEGEND", "Win 250 games", GameIconKind.TROPHY, 250, 1000, 30) { it.wins.intValue },
        Award("lvl10", "GETTING WARM", "Reach level 10", GameIconKind.FIRE, 10, 80, 0) { it.maxLevel.intValue },
        Award("lvl25", "QUARTER CENTURY", "Reach level 25", GameIconKind.FIRE, 25, 150, 8) { it.maxLevel.intValue },
        Award("lvl50", "CENTURION", "Reach level 50", GameIconKind.STAR, 50, 300, 15) { it.maxLevel.intValue },
        Award("lvl100", "GRANDMASTER", "Reach level 100", GameIconKind.STAR, 100, 700, 40) { it.maxLevel.intValue },
        Award("coins5k", "MONEY MOVES", "Earn 5,000 coins in total", GameIconKind.COIN_STACK, 5000, 100, 5) { it.totalCoinsEarned.intValue },
        Award("coins50k", "TYCOON", "Earn 50,000 coins in total", GameIconKind.COIN_STACK, 50000, 400, 25) { it.totalCoinsEarned.intValue },
        Award("boards500", "PEOPLE PERSON", "Seat 500 passengers", GameIconKind.PARKING, 500, 150, 8) { it.boardsTotal.intValue },
        Award("streak7", "FAITHFUL", "Reach a 7-day login streak", GameIconKind.CLOVER, 7, 120, 25) { it.dailyStreak.intValue },
    )

    fun canClaim(prefs: Prefs, award: Award): Boolean =
        !prefs.achievementsClaimed.value.contains(award.id) && award.value(prefs) >= award.target

    /** One-time payout; returns the coin grant (0 when not claimable). */
    fun claim(prefs: Prefs, award: Award): Int {
        if (!canClaim(prefs, award)) return 0
        prefs.claimAchievement(award.id)
        if (award.coins > 0) prefs.addCoins(award.coins)
        if (award.gems > 0) prefs.addGems(award.gems)
        return award.coins
    }

    fun anyClaimable(prefs: Prefs): Boolean = all.any { canClaim(prefs, it) }
}
