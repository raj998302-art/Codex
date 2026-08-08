package com.codex.carjam.game

import java.security.MessageDigest

/**
 * Referral codes. Fully offline-friendly: a deterministic code per install,
 * one redemption per device. Crediting BOTH sides of a real referral needs a
 * backend (documented in README) — here the redeeming player gets the welcome
 * bonus, which is the standard soft-launch behaviour.
 */
object Referral {
    private const val ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789" // no I/L/O/0/1 confusion
    const val WELCOME_BONUS = 150

    fun myCode(salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("$salt|carjam-referral".toByteArray(Charsets.UTF_8))
        val sb = StringBuilder("CJ-")
        for (i in 0 until 6) {
            sb.append(ALPHABET[(digest[i].toInt() and 0xFF) % ALPHABET.length])
        }
        return sb.toString()
    }

    private val FORMAT = Regex("^CJ-[A-Z2-9]{6}$")

    enum class Result { SUCCESS, OWN_CODE, ALREADY_USED, BAD_FORMAT }

    fun redeem(prefs: Prefs, rawInput: String): Result {
        val code = rawInput.trim().uppercase()
        if (!FORMAT.matches(code)) return Result.BAD_FORMAT
        if (code == prefs.myReferralCode) return Result.OWN_CODE
        if (prefs.referredBy.value != null) return Result.ALREADY_USED
        prefs.markReferred(code)
        prefs.addCoins(WELCOME_BONUS)
        return Result.SUCCESS
    }
}
