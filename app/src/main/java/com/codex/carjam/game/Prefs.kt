package com.codex.carjam.game

import android.content.Context
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf

/** SharedPreferences-backed progress wallet; Compose-observable. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("car_jam_prefs", Context.MODE_PRIVATE)

    var coins = mutableIntStateOf(sp.getInt(KEY_COINS, 120))
        private set
    var maxLevel = mutableIntStateOf(sp.getInt(KEY_MAX_LEVEL, 1).coerceAtLeast(1))
        private set
    var soundOn = mutableStateOf(sp.getBoolean(KEY_SOUND, true))
    var vibrateOn = mutableStateOf(sp.getBoolean(KEY_VIBRATE, true))

    fun addCoins(amount: Int) {
        val v = (coins.intValue + amount).coerceAtLeast(0)
        coins.intValue = v
        sp.edit().putInt(KEY_COINS, v).apply()
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

    companion object {
        private const val KEY_COINS = "coins"
        private const val KEY_MAX_LEVEL = "max_level"
        private const val KEY_SOUND = "sound"
        private const val KEY_VIBRATE = "vibrate"
    }
}
