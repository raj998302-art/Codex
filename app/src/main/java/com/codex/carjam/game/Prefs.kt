package com.codex.carjam.game

import android.content.Context
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf

/** SharedPreferences-backed progress wallet; Compose-observable. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("car_jam_prefs", Context.MODE_PRIVATE)

    var coins = mutableIntStateOf(sp.getInt(KEY_COINS, 120))
        private set
    var totalCoinsEarned = mutableIntStateOf(sp.getInt(KEY_TOTAL_EARNED, 0))
        private set
    var maxLevel = mutableIntStateOf(sp.getInt(KEY_MAX_LEVEL, 1).coerceAtLeast(1))
        private set
    var soundOn = mutableStateOf(sp.getBoolean(KEY_SOUND, true))
    var vibrateOn = mutableStateOf(sp.getBoolean(KEY_VIBRATE, true))
    var removeAds = mutableStateOf(sp.getBoolean(KEY_NO_ADS, false))
        private set
    var dailyStreak = mutableIntStateOf(sp.getInt(KEY_STREAK, 0))
        private set
    var lastClaimDay = mutableLongStateOf(sp.getLong(KEY_LAST_CLAIM, -1L))
        private set

    val playerName: String get() = "You"

    /** Leaderboard rating: level progress dominates, small coin/streak flavour on top. */
    fun rating(): Int = maxLevel.intValue * 120 + totalCoinsEarned.intValue / 5 + dailyStreak.intValue * 10

    fun addCoins(amount: Int) {
        val v = (coins.intValue + amount).coerceAtLeast(0)
        coins.intValue = v
        sp.edit().putInt(KEY_COINS, v).apply()
        if (amount > 0) {
            val t = totalCoinsEarned.intValue + amount
            totalCoinsEarned.intValue = t
            sp.edit().putInt(KEY_TOTAL_EARNED, t).apply()
        }
    }

    fun unlockLevel(level: Int) {
        if (level > maxLevel.intValue) {
            maxLevel.intValue = level
            sp.edit().putInt(KEY_MAX_LEVEL, level).apply()
        }
    }

    fun setSound(on: Boolean) {
        soundOn.value = on
        sp.edit().putBoolean(KEY_SOUND, on).apply()
    }

    fun setVibrate(on: Boolean) {
        vibrateOn.value = on
        sp.edit().putBoolean(KEY_VIBRATE, on).apply()
    }

    fun setRemoveAds(owned: Boolean) {
        removeAds.value = owned
        sp.edit().putBoolean(KEY_NO_ADS, owned).apply()
    }

    fun setDailyClaim(streak: Int, epochDay: Long) {
        dailyStreak.intValue = streak
        lastClaimDay.longValue = epochDay
        sp.edit()
            .putInt(KEY_STREAK, streak)
            .putLong(KEY_LAST_CLAIM, epochDay)
            .apply()
    }

    companion object {
        private const val KEY_COINS = "coins"
        private const val KEY_TOTAL_EARNED = "total_earned"
        private const val KEY_MAX_LEVEL = "max_level"
        private const val KEY_SOUND = "sound"
        private const val KEY_VIBRATE = "vibrate"
        private const val KEY_NO_ADS = "no_ads"
        private const val KEY_STREAK = "daily_streak"
        private const val KEY_LAST_CLAIM = "last_claim_day"
    }
}
