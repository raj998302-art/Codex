package com.codex.carjam.game

import android.content.SharedPreferences
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Tamper-evident storage for the soft economy. Every sensitive value is stored
 * twice, each copy as `value.checksum` where checksum = SHA-256(value | tag |
 * per-install random salt). A rooted user editing SharedPreferences by hand
 * breaks the checksum and the untouched mirror is restored instead.
 *
 * Honest note: client-side-only hardening raises the bar but a determined
 * attacker on a rooted device can still win — real cheat-proofing needs scores
 * and purchases validated server-side (Play Billing already server-verifies
 * purchase tokens itself; see README).
 */
object SecureVault {
    private const val SALT_KEY = "sv.k1"
    private const val TAG = "carjam-v1"

    fun installSalt(sp: SharedPreferences): String {
        val existing = sp.getString(SALT_KEY, null)
        if (existing != null) return existing
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        val fresh = bytes.joinToString("") { "%02x".format(it) }
        sp.edit().putString(SALT_KEY, fresh).apply()
        return fresh
    }

    private fun signature(value: Long, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("$value|$TAG|$salt".toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }.take(20)
    }

    fun pack(value: Long, salt: String): String = "$value.${signature(value, salt)}"

    /** Decoded value, or null when absent / corrupted / tampered. */
    fun unpack(raw: String?, salt: String): Long? {
        if (raw.isNullOrEmpty()) return null
        val dot = raw.lastIndexOf('.')
        if (dot <= 0 || dot == raw.length - 1) return null
        val value = raw.substring(0, dot).toLongOrNull() ?: return null
        val sig = raw.substring(dot + 1)
        return if (signature(value, salt) == sig) value else null
    }
}
