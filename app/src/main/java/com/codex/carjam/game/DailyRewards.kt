package com.codex.carjam.game

/** 7-day login-streak calendar. Fully offline; one claim per calendar day. */
object DailyRewards {
    val prizes = listOf(20, 30, 45, 60, 80, 110, 200)

    /** Diamonds granted on top of the coin prize (day → gems). */
    val gemPrizes = mapOf(3 to 5, 6 to 10, 7 to 25)

    private fun todayEpoch(): Long = System.currentTimeMillis() / 86_400_000L

    fun canClaim(prefs: Prefs): Boolean = prefs.lastClaimDay.longValue < todayEpoch()

    /** Day number that the next claim will grant (1..7). */
    fun nextDay(prefs: Prefs): Int {
        val yesterday = todayEpoch() - 1
        return if (prefs.lastClaimDay.longValue == yesterday) {
            (prefs.dailyStreak.intValue % prizes.size) + 1
        } else {
            1
        }
    }

    /** Grants today's prize (coins + day-specific gems), returns the coin amount.
     *  Idempotent: a second call on the same day (double-tap, recomposition
     *  re-fire, any UI glitch) grants 0 — never a duplicate prize. */
    fun claim(prefs: Prefs): Int {
        if (!canClaim(prefs)) return 0
        val day = nextDay(prefs)
        val prize = prizes[(day - 1).coerceIn(0, prizes.size - 1)]
        prefs.addCoins(prize)
        gemPrizes[day]?.let { prefs.addGems(it) }
        prefs.setDailyClaim(day, todayEpoch())
        return prize
    }
}
