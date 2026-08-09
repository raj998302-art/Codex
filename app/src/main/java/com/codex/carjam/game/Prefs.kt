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
    var musicOn = mutableStateOf(sp.getBoolean(KEY_MUSIC, true))
        private set
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
        // garage: unlocks union-merge (never lose a purchase), pick follows cloud
        val cRides = s.optString("rides", "")
            .split(',')
            .map { it.trim() }
            .filter { it.matches(Regex("[a-z]{2,20}")) }
            .toSet()
        if (cRides.isNotEmpty()) {
            val merged = ownedRides.value + cRides
            if (merged != ownedRides.value) {
                ownedRides.value = merged
                sp.edit().putStringSet(KEY_RIDES, merged).apply()
                raised = true
            }
            val cSel = s.optString("rideSel", "")
            if (cSel.isNotEmpty() && merged.contains(cSel) && cSel != selectedRide.value) {
                selectRide(cSel)
                raised = true
            }
        }
        // v3.0 avatar frames: union-merge unlocks (never lose a paid frame)
        val cFrames = s.optString("frames", "")
            .split(',')
            .mapNotNull { it.trim().substringBefore(':').toIntOrNull() }
            .filter { it in 0..99 }
            .toSet()
        if (cFrames.isNotEmpty()) {
            val mergedF = ownedFrames.value + cFrames
            if (mergedF != ownedFrames.value) {
                ownedFrames.value = mergedF
                // server stores plain ids; keep any local source tags we know, cloud wins on ids
                sp.edit().putString(KEY_FRAMES, mergedF.joinToString(",")).apply()
                raised = true
            }
            val cFrameSel = s.optInt("frameSel", -1)
            if (cFrameSel in 0..99 && mergedF.contains(cFrameSel) && cFrameSel != avatarFrame.intValue) {
                selectFrame(cFrameSel)
                raised = true
            }
        }
        // v3.3 scene + pack flags ride the same cloud save
        if (s.has("vehPack")) {
            if (s.optBoolean("vehPack", false)) {
                if (!hasVehiclePack.value) {
                    grantVehiclePack()
                    raised = true
                }
            }
        }
        if (s.has("scene")) {
            val cScene = s.optString("scene", "")
            val theme = if (cScene.isEmpty()) null else runCatching { LevelTheme.valueOf(cScene) }.getOrNull()
            if (theme != null && theme.name != manualScene.value && maxLevel.intValue >= theme.minLevel) {
                setManualScene(theme.name)
                raised = true
            }
        }
        if (s.has("musicOn")) {
            val cMusic = s.optBoolean("musicOn", musicOn.value)
            if (cMusic != musicOn.value) {
                setMusic(cMusic)
                raised = true
            }
        }
        // boosters ride along (count-up merge like other wallet fields)
        val cHam = s.optInt("hammers", -1)
        if (cHam in 0..99 && cHam > hammers.intValue) {
            hammers.intValue = cHam
            sp.edit().putInt(KEY_HAMMERS, cHam).apply()
            raised = true
        }
        val cShu = s.optInt("shuffles", -1)
        if (cShu in 0..99 && cShu > shufflesStock.intValue) {
            shufflesStock.intValue = cShu
            sp.edit().putInt(KEY_SHUFFLES, cShu).apply()
            raised = true
        }
        val cElim = s.optInt("elims", -1)
        if (cElim in 0..99 && cElim > elims.intValue) {
            elims.intValue = cElim
            sp.edit().putInt(KEY_ELIMS, cElim).apply()
            raised = true
        }
        // boosters: refreshes join the belt ledger as well (v3.3)
        val cRefre = s.optInt("refreshes", -1)
        if (cRefre in 0..99 && cRefre > refreshes.intValue) {
            refreshes.intValue = cRefre
            sp.edit().putInt(KEY_REFRESHES, cRefre).apply()
            raised = true
        }
        val cPiggy = s.optInt("piggy", -1)
        if (cPiggy in 0..PIGGY_CAP && cPiggy > piggy.intValue) {
            piggy.intValue = cPiggy
            sp.edit().putInt(KEY_PIGGY, cPiggy).apply()
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

    fun setMusic(on: Boolean) {
        musicOn.value = on
        sp.edit().putBoolean(KEY_MUSIC, on).apply()
    }

    // ---------------------------------------------------------------- garage

    var ownedRides = mutableStateOf<Set<String>>(
        (sp.getStringSet(KEY_RIDES, emptySet())?.toSet() ?: emptySet()) + "sedan",
    )
        private set

    fun unlockRide(id: String) {
        if (ownedRides.value.contains(id)) return
        val v = ownedRides.value + id
        ownedRides.value = v
        sp.edit().putStringSet(KEY_RIDES, v).apply()
    }

    var selectedRide = mutableStateOf(sp.getString(KEY_RIDE_SEL, "sedan") ?: "sedan")
        private set

    fun selectRide(id: String) {
        if (!ownedRides.value.contains(id)) return
        selectedRide.value = id
        sp.edit().putString(KEY_RIDE_SEL, id).apply()
    }

    // ---------------------------------------------------------------- boosters

    var hammers = mutableIntStateOf(sp.getInt(KEY_HAMMERS, 0))
        private set
    var shufflesStock = mutableIntStateOf(sp.getInt(KEY_SHUFFLES, 0))
        private set

    fun addHammers(n: Int) {
        if (n == 0) return
        val v = (hammers.intValue + n).coerceIn(0, 99)
        hammers.intValue = v
        sp.edit().putInt(KEY_HAMMERS, v).apply()
    }

    fun addShuffles(n: Int) {
        if (n == 0) return
        val v = (shufflesStock.intValue + n).coerceIn(0, 99)
        shufflesStock.intValue = v
        sp.edit().putInt(KEY_SHUFFLES, v).apply()
    }

    /** Spend one hammer; false when the belt is empty. */
    fun useHammer(): Boolean {
        if (hammers.intValue <= 0) return false
        addHammers(-1)
        return true
    }

    fun useShuffle(): Boolean {
        if (shufflesStock.intValue <= 0) return false
        addShuffles(-1)
        return true
    }

    /** One-time v2.7 welcome gift so every player meets the new boosters. */
    fun grantBoosterGiftIfNeeded(): Boolean {
        if (sp.getBoolean(KEY_BOOST_GIFT, false)) return false
        sp.edit().putBoolean(KEY_BOOST_GIFT, true).apply()
        addHammers(2)
        addShuffles(3)
        return true
    }

    // ---------------------------------------------------------------- eliminate + piggy

    var elims = mutableIntStateOf(sp.getInt(KEY_ELIMS, 1)) // everyone starts with one free ticket
        private set

    fun addElims(n: Int) {
        if (n == 0) return
        val v = (elims.intValue + n).coerceIn(0, 99)
        elims.intValue = v
        sp.edit().putInt(KEY_ELIMS, v).apply()
    }

    fun useElim(): Boolean {
        if (elims.intValue <= 0) return false
        addElims(-1)
        return true
    }

    /** Piggy bank: fills with level wins, breakable via the Play Store purchase. */
    // v3.3: queue refresh booster (safe chaos reshuffle — never deadlocks)
    var refreshes = mutableIntStateOf(sp.getInt(KEY_REFRESHES, 1))
        private set

    fun addRefreshes(n: Int) {
        val v = (refreshes.intValue + n).coerceIn(0, 99)
        refreshes.intValue = v
        sp.edit().putInt(KEY_REFRESHES, v).apply()
    }

    fun useRefresh(): Boolean {
        if (refreshes.intValue <= 0) return false
        addRefreshes(-1)
        return true
    }

    /** One-time v3.3 Skin-Shop welcome starter: Sprinter frame + wallet boosts. */
    fun grantSkinsWelcomeIfNeeded(): Boolean {
        if (sp.getBoolean(KEY_SKINS_WELCOME, false)) return false
        sp.edit().putBoolean(KEY_SKINS_WELCOME, true).apply()
        unlockFrame(1, "c")
        addCoins(100)
        addShuffles(1)
        addRefreshes(1)
        return true
    }

    // frame watch-ad strip progress (v3.3 Meow Squad frame)
    var frameAdWatches = mutableIntStateOf(sp.getInt(KEY_FRAME_ADS, 0))
        private set

    fun noteFrameAdWatch(): Int {
        val v = (frameAdWatches.intValue + 1).coerceIn(0, 99)
        frameAdWatches.intValue = v
        sp.edit().putInt(KEY_FRAME_ADS, v).apply()
        return v
    }

    var piggy = mutableIntStateOf(sp.getInt(KEY_PIGGY, 0))
        private set

    fun addPiggy(n: Int) {
        if (n == 0) return
        val v = (piggy.intValue + n).coerceIn(0, PIGGY_CAP)
        piggy.intValue = v
        sp.edit().putInt(KEY_PIGGY, v).apply()
    }

    val piggyFull: Boolean get() = piggy.intValue >= PIGGY_CAP

    /** Purchase success: pour the bank into the wallet and reset it. */
    fun breakPiggyBank(): Int {
        val v = piggy.intValue
        if (v > 0) addCoins(v)
        piggy.intValue = 0
        sp.edit().putInt(KEY_PIGGY, 0).apply()
        return v
    }

    // ---------------------------------------------------------------- avatar frames

    var ownedFrames = mutableStateOf<Set<Int>>(
        (sp.getString(KEY_FRAMES, "0") ?: "0")
            .split(',')
            .mapNotNull { it.trim().substringBefore(':').toIntOrNull() }
            .toSet() + 0,
    )
        private set

    /** v3.3: frame ownership carries a source tag (`id:src`, src = u/c/g/e/w/a) so the Info dialog can group them. */
    private fun frameSources(): Map<Int, String> =
        (sp.getString(KEY_FRAMES, "0") ?: "0")
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .associate { entry ->
                val id = entry.substringBefore(':').toIntOrNull() ?: 0
                val src = entry.substringAfter(':', "u")
                id to src
            }

    /** Frames earned through one source: c=shop(coins) g=gems u=level e=event w=weekly a=ads. */
    fun framesOf(src: String): List<Int> =
        frameSources().filter { it.value == src }.keys.sorted()

    /** Tagged csv (what we persist + send to the cloud): "0:u,1:c,2:g". */
    fun framesTaggedCsv(): String =
        ownedFrames.value.joinToString(",") { id -> "$id:" + (frameSources()[id] ?: "u") }

    /**
     * Structured inventory the Save-loop mirrors 1:1 (keeps Profile + shop +
     * save-loop all reading the same ledger).
     */
    data class Inventory(
        val framesOwned: Int,
        val framesBySource: Map<String, List<Int>>,
        val ridesOwned: Int,
        val activeFrame: Int,
        val activeRide: String,
    )

    fun inventory(): Inventory = Inventory(
        framesOwned = ownedFrames.value.size,
        framesBySource = frameSources().entries.groupBy({ it.value }, { it.key }).mapValues { it.value.sorted() },
        ridesOwned = ownedRides.value.size,
        activeFrame = avatarFrame.intValue,
        activeRide = selectedRide.value,
    )

    fun unlockFrame(id: Int, source: String = "u") {
        if (ownedFrames.value.contains(id)) return
        val v = ownedFrames.value + id
        ownedFrames.value = v
        sp.edit().putString(KEY_FRAMES, v.joinToString(",") { "$it:" + (frameSources()[it] ?: source) }).apply()
    }

    var avatarFrame = mutableIntStateOf(sp.getInt(KEY_FRAME, 0))
        private set

    fun selectFrame(id: Int) {
        if (!ownedFrames.value.contains(id)) return
        avatarFrame.intValue = id
        sp.edit().putInt(KEY_FRAME, id).apply()
    }

    // ---------------------------------------------------------------- v3.3 skin shop state

    /** Manually picked scene (theme name) or null when Auto Switch is on. */
    var manualScene = mutableStateOf(sp.getString(KEY_SCENE, null))
        private set

    var autoSceneSwitch = mutableStateOf(sp.getBoolean(KEY_SCENE_AUTO, true))
        private set

    fun setManualScene(theme: String?) {
        manualScene.value = theme
        autoSceneSwitch.value = theme == null
        sp.edit()
            .putString(KEY_SCENE, theme)
            .putBoolean(KEY_SCENE_AUTO, theme == null)
            .apply()
    }

    fun setAutoScene(on: Boolean) {
        autoSceneSwitch.value = on
        if (on) {
            manualScene.value = null
            sp.edit().remove(KEY_SCENE).apply()
        }
        sp.edit().putBoolean(KEY_SCENE_AUTO, on).apply()
    }

    /** Vehicle pack (cars bundle) — one-time coin purchase opening the PACKS lane. */
    var hasVehiclePack = mutableStateOf(sp.getBoolean(KEY_VEH_PACK, false))
        private set

    fun grantVehiclePack() {
        hasVehiclePack.value = true
        sp.edit().putBoolean(KEY_VEH_PACK, true).apply()
    }

    /** Weekly-winners frame ownership flows through [unlockFrame] with source "w"; event frames use "e". */

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
        const val PIGGY_CAP = 800
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
        private const val KEY_MUSIC = "music"
        private const val KEY_HAMMERS = "boost_hammers"
        private const val KEY_SHUFFLES = "boost_shuffles"
        private const val KEY_ELIMS = "boost_elims"
        private const val KEY_PIGGY = "piggy_bank_fill"
        private const val KEY_FRAME = "avatar_frame"
        private const val KEY_FRAMES = "avatar_frames_owned"
        private const val KEY_SCENE = "scene_manual"
        private const val KEY_SCENE_AUTO = "scene_auto"
        private const val KEY_VEH_PACK = "veh_pack_owned"
        private const val KEY_REFRESHES = "refresh_stock"
        private const val KEY_FRAME_ADS = "frame_ad_watches"
        private const val KEY_SKINS_WELCOME = "skins_welcome_v33"
        private const val KEY_BOOST_GIFT = "boost_gift_v27"
        private const val KEY_RIDES = "owned_rides"
        private const val KEY_RIDE_SEL = "selected_ride"
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
