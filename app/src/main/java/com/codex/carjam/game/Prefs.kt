package com.codex.carjam.game

import android.content.Context
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import java.util.UUID

/**
 * SharedPreferences-backed progress wallet; Compose-observable.
 * Economy-critical values (coins, earnings, No-Ads ownership) go through
 * [SecureVault]: checksum-verified payloads with an automatic mirror restore
 * when tampering is detected.
 */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("car_jam_prefs", Context.MODE_PRIVATE)
    internal val installSalt: String = SecureVault.installSalt(sp)

    var tamperDetected = mutableIntStateOf(sp.getInt(KEY_TAMPER, 0))
        private set

    // ---------------------------------------------------------------- economy

    var coins = mutableIntStateOf(securedGet(KEY_COINS_S, KEY_COINS_B, KEY_COINS, 120).toInt().coerceIn(0, MAX_COINS))
        private set
    var totalCoinsEarned = mutableIntStateOf(securedGet(KEY_EARN_S, KEY_EARN_B, KEY_TOTAL_EARNED, 0).toInt().coerceIn(0, 100_000_000))
        private set
    var removeAds = mutableStateOf(securedFlagGet(KEY_NOADS_S, KEY_NOADS_B, KEY_NO_ADS))
        private set
    var gems = mutableIntStateOf(securedGet(KEY_GEMS_S, KEY_GEMS_B, "", 30).toInt().coerceIn(0, MAX_GEMS))
        private set

    // ---------------------------------------------------------------- progress

    var maxLevel = mutableIntStateOf(sp.getInt(KEY_MAX_LEVEL, 1).coerceAtLeast(1))
        private set
    var soundOn = mutableStateOf(sp.getBoolean(KEY_SOUND, true))
    var vibrateOn = mutableStateOf(sp.getBoolean(KEY_VIBRATE, true))
    var dailyStreak = mutableIntStateOf(sp.getInt(KEY_STREAK, 0))
        private set
    var lastClaimDay = mutableLongStateOf(sp.getLong(KEY_LAST_CLAIM, -1L))
        private set

    // ---------------------------------------------------------------- profile

    private var nameState = mutableStateOf(sp.getString(KEY_NAME, "Racer") ?: "Racer")

    /** Observable during composition (reads underlying state). */
    val playerName: String get() = nameState.value

    var avatarId = mutableIntStateOf(sp.getInt(KEY_AVATAR, 0).coerceIn(0, AVATAR_COUNT - 1))
        private set
    var wins = mutableIntStateOf(sp.getInt(KEY_WINS, 0))
        private set
    var losses = mutableIntStateOf(sp.getInt(KEY_LOSSES, 0))
        private set
    var practiceWins = mutableIntStateOf(sp.getInt(KEY_PRACTICE_WINS, 0))
        private set
    var referredBy = mutableStateOf<String?>(sp.getString(KEY_REFERRED, null))
        private set

    /** Last day (epoch) the daily-event popup was auto-shown. */
    var eventSeenDay = mutableIntStateOf(sp.getInt(KEY_EVENT_DAY, -1))
        private set

    /** First-run onboarding (Google Play Games connect sheet) completed/dismissed. */
    var welcomed = mutableStateOf(sp.getBoolean(KEY_WELCOMED, false))
        private set

    val myReferralCode: String get() = Referral.myCode(installSalt)

    /** Stable anonymous install id — only used to claim this player's leaderboard slot. */
    val deviceId: String = sp.getString(KEY_DEVICE_ID, null)
        ?: UUID.randomUUID().toString().also { sp.edit().putString(KEY_DEVICE_ID, it).apply() }

    /** Leaderboard rating: level progress dominates, small coin/streak flavour on top. */
    fun rating(): Int = maxLevel.intValue * 120 + totalCoinsEarned.intValue / 5 + dailyStreak.intValue * 10

    fun winRate(): Int {
        val total = wins.intValue + losses.intValue
        return if (total == 0) 0 else (wins.intValue * 100) / total
    }

    // ---------------------------------------------------------------- secured io

    private fun securedGet(key: String, backupKey: String, legacyKey: String, def: Long): Long {
        SecureVault.unpack(sp.getString(key, null), installSalt)?.let { return it }
        SecureVault.unpack(sp.getString(backupKey, null), installSalt)?.let {
            noteTamper()
            securedSet(key, backupKey, it)
            return it
        }
        // migrate legacy plain-int installs (pre-vault)
        if (legacyKey.isNotEmpty() && sp.contains(legacyKey)) {
            return sp.getInt(legacyKey, def.toInt()).toLong()
        }
        return def
    }

    private fun securedFlagGet(key: String, backupKey: String, legacyKey: String): Boolean {
        SecureVault.unpack(sp.getString(key, null), installSalt)?.let { return it == 1L }
        SecureVault.unpack(sp.getString(backupKey, null), installSalt)?.let {
            noteTamper()
            securedSet(key, backupKey, it)
            return it == 1L
        }
        if (sp.contains(legacyKey)) return sp.getBoolean(legacyKey, false)
        return false
    }

    private fun securedSet(key: String, backupKey: String, value: Long) {
        val packed = SecureVault.pack(value, installSalt)
        sp.edit().putString(key, packed).putString(backupKey, packed).apply()
    }

    private fun noteTamper() {
        val v = tamperDetected.intValue + 1
        tamperDetected.intValue = v
        sp.edit().putInt(KEY_TAMPER, v).apply()
    }

    // ---------------------------------------------------------------- mutators

    fun addCoins(amount: Int) {
        val grant = if (amount > 0) amount.coerceAtMost(MAX_SINGLE_GRANT) else amount
        val v = (coins.intValue + grant).coerceIn(0, MAX_COINS)
        coins.intValue = v
        securedSet(KEY_COINS_S, KEY_COINS_B, v.toLong())
        if (grant > 0) {
            val t = (totalCoinsEarned.intValue + grant).coerceIn(0, 100_000_000)
            totalCoinsEarned.intValue = t
            securedSet(KEY_EARN_S, KEY_EARN_B, t.toLong())
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
        securedSet(KEY_NOADS_S, KEY_NOADS_B, if (owned) 1L else 0L)
    }

    fun setDailyClaim(streak: Int, epochDay: Long) {
        dailyStreak.intValue = streak
        lastClaimDay.longValue = epochDay
        sp.edit()
            .putInt(KEY_STREAK, streak)
            .putLong(KEY_LAST_CLAIM, epochDay)
            .apply()
    }

    /** Free-form but cleaned: letters/digits/space/' . - _, max 14 chars. */
    fun setPlayerName(raw: String) {
        val cleaned = raw
            .trim()
            .take(14)
            .filter { it.isLetterOrDigit() || it == ' ' || it == '-' || it == '_' || it == '.' || it == '\'' }
            .trim()
        if (cleaned.isNotEmpty() && cleaned != nameState.value) {
            nameState.value = cleaned
            sp.edit().putString(KEY_NAME, cleaned).apply()
        }
    }

    fun setAvatar(id: Int) {
        val v = id.coerceIn(0, AVATAR_COUNT - 1)
        avatarId.intValue = v
        sp.edit().putInt(KEY_AVATAR, v).apply()
    }

    fun recordWin() {
        val v = wins.intValue + 1
        wins.intValue = v
        sp.edit().putInt(KEY_WINS, v).apply()
    }

    fun recordLoss() {
        val v = losses.intValue + 1
        losses.intValue = v
        sp.edit().putInt(KEY_LOSSES, v).apply()
    }

    fun recordPractice() {
        val v = practiceWins.intValue + 1
        practiceWins.intValue = v
        sp.edit().putInt(KEY_PRACTICE_WINS, v).apply()
    }

    fun markReferred(code: String) {
        referredBy.value = code
        sp.edit().putString(KEY_REFERRED, code).apply()
    }

    fun addGems(amount: Int) {
        val grant = if (amount > 0) amount.coerceAtMost(MAX_SINGLE_GRANT) else amount
        val v = (gems.intValue + grant).coerceIn(0, MAX_GEMS)
        gems.intValue = v
        securedSet(KEY_GEMS_S, KEY_GEMS_B, v.toLong())
    }

    /** True when affordable (and deducted), false otherwise. */
    fun spendGems(cost: Int): Boolean {
        if (cost <= 0) return true
        if (gems.intValue < cost) return false
        addGems(-cost)
        return true
    }

    fun markEventSeen(epochDay: Int) {
        eventSeenDay.intValue = epochDay
        sp.edit().putInt(KEY_EVENT_DAY, epochDay).apply()
    }

    fun markWelcomed() {
        welcomed.value = true
        sp.edit().putBoolean(KEY_WELCOMED, true).apply()
    }

    companion object {
        const val AVATAR_COUNT = 8
        private const val MAX_COINS = 10_000_000
        private const val MAX_GEMS = 500_000
        private const val MAX_SINGLE_GRANT = 500_000

        private const val KEY_COINS = "coins"                    // legacy plain
        private const val KEY_COINS_S = "coins.s"
        private const val KEY_COINS_B = "coins.b"
        private const val KEY_TOTAL_EARNED = "total_earned"      // legacy plain
        private const val KEY_EARN_S = "earned.s"
        private const val KEY_EARN_B = "earned.b"
        private const val KEY_NO_ADS = "no_ads"                  // legacy plain
        private const val KEY_NOADS_S = "noads.s"
        private const val KEY_NOADS_B = "noads.b"
        private const val KEY_MAX_LEVEL = "max_level"
        private const val KEY_SOUND = "sound"
        private const val KEY_VIBRATE = "vibrate"
        private const val KEY_STREAK = "daily_streak"
        private const val KEY_LAST_CLAIM = "last_claim_day"
        private const val KEY_NAME = "player_name"
        private const val KEY_AVATAR = "avatar_id"
        private const val KEY_WINS = "wins"
        private const val KEY_LOSSES = "losses"
        private const val KEY_PRACTICE_WINS = "practice_wins"
        private const val KEY_REFERRED = "referred_by"
        private const val KEY_GEMS_S = "gems.s"
        private const val KEY_GEMS_B = "gems.b"
        private const val KEY_EVENT_DAY = "event_seen_day"
        private const val KEY_WELCOMED = "welcomed"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_TAMPER = "tamper_flags"
    }
}
