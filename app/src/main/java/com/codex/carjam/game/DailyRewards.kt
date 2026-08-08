package com.codex.carjam.game

/** 7-day login-streak calendar. Fully offline; one claim per calendar day. */
object DailyRewards {
    val prizes = listOf(20, 30, 45, 60, 80, 110, 200)

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

    /** Grants today's prize, returns the amount. */
    fun claim(prefs: Prefs): Int {
        val day = nextDay(prefs)
        val prize = prizes[(day - 1).coerceIn(0, prizes.size - 1)]
        prefs.addCoins(prize)
        prefs.setDailyClaim(day, todayEpoch())
        return prize
    }
}
