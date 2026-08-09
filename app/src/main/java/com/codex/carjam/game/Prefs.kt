package com.codex.carjam.game

import android.content.Context
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import org.json.JSONObject
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

    /** Lifetime seated-passenger counter (drives missions + achievements). */
    var boardsTotal = mutableIntStateOf(sp.getInt(KEY_BOARDS, 0))
        private set

    fun noteBoard() {
        val v = boardsTotal.intValue + 1
        boardsTotal.intValue = v
        sp.edit().putInt(KEY_BOARDS, v).apply()
    }

    // ---------------------------------------------------------------- daily missions

    /** Baselines snapshot for the current mission day (null = not initialised). */
    data class MissionBars(
        val day: Int,
        val wins: Long,
        val boards: Long,
        val earned: Long,
        val level: Long,
        val claimed: Int,
    )

    var missionClaimed = mutableIntStateOf(sp.getInt(KEY_MSN_CLAIMED, 0))
        private set

    /** Rollover-safe: first read of a new day re-anchors all counters. */
    fun ensureMissionDay(epochDay: Int): MissionBars {
        if (sp.getInt(KEY_MSN_DAY, -1) != epochDay) {
            missionClaimed.intValue = 0
            sp.edit()
                .putInt(KEY_MSN_DAY, epochDay)
                .putLong(KEY_MSN_BASE_WINS, wins.intValue.toLong())
                .putLong(KEY_MSN_BASE_BOARDS, boardsTotal.intValue.toLong())
                .putLong(KEY_MSN_BASE_EARNED, totalCoinsEarned.intValue.toLong())
                .putLong(KEY_MSN_BASE_LEVEL, maxLevel.intValue.toLong())
                .putInt(KEY_MSN_CLAIMED, 0)
                .apply()
        }
        return MissionBars(
            day = epochDay,
            wins = sp.getLong(KEY_MSN_BASE_WINS, wins.intValue.toLong()),
            boards = sp.getLong(KEY_MSN_BASE_BOARDS, boardsTotal.intValue.toLong()),
            earned = sp.getLong(KEY_MSN_BASE_EARNED, totalCoinsEarned.intValue.toLong()),
            level = sp.getLong(KEY_MSN_BASE_LEVEL, maxLevel.intValue.toLong()),
            claimed = missionClaimed.intValue,
        )
    }

    fun setMissionClaimBit(index: Int) {
        val v = missionClaimed.intValue or (1 shl index)
        missionClaimed.intValue = v
        sp.edit().putInt(KEY_MSN_CLAIMED, v).apply()
    }

    // ---------------------------------------------------------------- achievements

    var achievementsClaimed = mutableStateOf<Set<String>>(
        sp.getStringSet(KEY_ACHV, emptySet())?.toSet() ?: emptySet(),
    )
        private set

    fun claimAchievement(id: String) {
        if (achievementsClaimed.value.contains(id)) return
        val v = achievementsClaimed.value + id
        achievementsClaimed.value = v
        sp.edit().putStringSet(KEY_ACHV, v).apply()
    }

    // ---------------------------------------------------------------- weekly season prize

    var lastSeasonWeek = mutableIntStateOf(sp.getInt(KEY_SEASON_WEEK, 0))
        private set

    fun setLastSeasonWeek(weekId: Int) {
        lastSeasonWeek.intValue = weekId
        sp.edit().putInt(KEY_SEASON_WEEK, weekId).apply()
    }

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

    // ---------------------------------------------------------------- cloud save

    /**
     * 256-bit anonymous account key (64 hex chars). The server stores ONLY its
     * SHA-256 hash — knowing/sharing this key is sharing the account itself,
     * which is exactly what powers the "restore on a new phone" flow.
     */
    var syncKey: String = sp.getString(KEY_SYNC_KEY, null) ?: run {
        val gen = (UUID.randomUUID().toString() + UUID.randomUUID().toString()).replace("-", "")
        sp.edit().putString(KEY_SYNC_KEY, gen).apply()
        gen
    }
        private set

    fun setSyncKey(raw: String): Boolean {
        val cleaned = raw.lowercase().filter { it.isLetterOrDigit() }
        if (cleaned.length !in 24..128) return false
        syncKey = cleaned
        sp.edit().putString(KEY_SYNC_KEY, cleaned).apply()
        return true
    }

    var lastCloudSyncMs = mutableLongStateOf(sp.getLong(KEY_CLOUD_MS, 0L))
        private set

    fun noteCloudSync() {
        val now = System.currentTimeMillis()
        lastCloudSyncMs.longValue = now
        sp.edit().putLong(KEY_CLOUD_MS, now).apply()
    }

    /**
     * MAX-merges the server-canonical snapshot into local progress. Purchases
     * are deliberately NOT touched: the no-ads entitlement is granted only by
     * Google Play Billing's own restore flow — the cloud can never mint it.
     * Returns true when the cloud raised anything locally.
     */
    fun applyCloudRestore(s: JSONObject): Boolean {
        var raised = false
        val cCoins = s.optLong("coins", -1L)
        if (cCoins > coins.intValue) {
            val v = cCoins.coerceIn(0, MAX_COINS.toLong())
            coins.intValue = v.toInt()
            securedSet(KEY_COINS_S, KEY_COINS_B, v)
            raised = true
        }
        val cGems = s.optLong("gems", -1L)
        if (cGems > gems.intValue) {
            val v = cGems.coerceIn(0, MAX_GEMS.toLong())
            gems.intValue = v.toInt()
            securedSet(KEY_GEMS_S, KEY_GEMS_B, v)
            raised = true
        }
        val cEarned = s.optLong("totalCoinsEarned", -1L)
        if (cEarned > totalCoinsEarned.intValue) {
            val v = cEarned.coerceIn(0, 100_000_000L)
            totalCoinsEarned.intValue = v.toInt()
            securedSet(KEY_EARN_S, KEY_EARN_B, v)
            raised = true
        }
        val cLevel = s.optInt("maxLevel", 0)
        if (cLevel > maxLevel.intValue) {
            unlockLevel(cLevel)
            raised = true
        }
        val cStreak = s.optInt("dailyStreak", -1)
        if (cStreak > dailyStreak.intValue) {
            setDailyClaim(cStreak, s.optLong("lastClaimDay", lastClaimDay.longValue))
            raised = true
        }
        val cWins = s.optInt("wins", -1)
        if (cWins > wins.intValue) {
            wins.intValue = cWins
            sp.edit().putInt(KEY_WINS, cWins).apply()
            raised = true
        }
        val cLosses = s.optInt("losses", -1)
        if (cLosses > losses.intValue) {
            losses.intValue = cLosses
            sp.edit().putInt(KEY_LOSSES, cLosses).apply()
            raised = true
        }
        val cPractice = s.optInt("practiceWins", -1)
        if (cPractice > practiceWins.intValue) {
            practiceWins.intValue = cPractice
            sp.edit().putInt(KEY_PRACTICE_WINS, cPractice).apply()
            raised = true
        }
        val cName = s.optString("name", "").trim()
        if (cName.isNotEmpty() && cName != playerName) {
            setPlayerName(cName)
            raised = true
        }
        val cAvatar = s.optInt("avatarId", -1)
        if (cAvatar in 0 until AVATAR_COUNT && cAvatar != avatarId.intValue) {
            setAvatar(cAvatar)
            raised = true
        }
        if (s.optBoolean("welcomed") && !welcomed.value) {
            markWelcomed()
            raised = true
        }
        val cRef = s.optString("referredBy", "")
        if (referredBy.value == null && cRef.isNotEmpty()) {
            markReferred(cRef)
            raised = true
        }
        return raised
    }

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
        private const val KEY_BOARDS = "boards_total"
        private const val KEY_MSN_DAY = "mission_day"
        private const val KEY_MSN_BASE_WINS = "mission_base_wins"
        private const val KEY_MSN_BASE_BOARDS = "mission_base_boards"
        private const val KEY_MSN_BASE_EARNED = "mission_base_earned"
        private const val KEY_MSN_BASE_LEVEL = "mission_base_level"
        private const val KEY_MSN_CLAIMED = "mission_claimed"
        private const val KEY_ACHV = "achievements_claimed"
        private const val KEY_SEASON_WEEK = "season_week"
        private const val KEY_SYNC_KEY = "sync_key"
        private const val KEY_CLOUD_MS = "last_cloud_sync_ms"
        private const val KEY_TAMPER = "tamper_flags"
    }
}
