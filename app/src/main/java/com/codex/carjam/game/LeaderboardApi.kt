package com.codex.carjam.game

import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.Executors

/**
 * Thin HTTPS client for the leaderboard backend (see backend/ — a tiny
 * Node/Express + Mongoose service you deploy free on Render).
 *
 * IMPORTANT: base URL goes here, never the MongoDB connection string — the
 * database password lives only in the server's environment. Until BASE_URL is
 * replaced after deployment, CONFIGURED stays false and the game shows its
 * offline board state instead of ever crashing or spamming requests.
 */
object LeaderboardApi {

    /** Live leaderboard service (Render — backend/ in this repo). */
    const val BASE_URL = "https://carjam.onrender.com"

    val CONFIGURED: Boolean get() = !BASE_URL.contains("YOUR-")

    data class Entry(
        val rank: Int,
        val name: String,
        val avatarId: Int,
        val rating: Int,
        val maxLevel: Int,
        val deviceId: String,
    )

    private val main = Handler(Looper.getMainLooper())
    private val io = Executors.newCachedThreadPool()

    private fun request(method: String, path: String, body: JSONObject?, cb: (JSONObject?) -> Unit) {
        if (!CONFIGURED) {
            main.post { cb(null) }
            return
        }
        io.execute {
            var conn: HttpURLConnection? = null
            try {
                conn = (URL(BASE_URL + path).openConnection() as HttpURLConnection).apply {
                    requestMethod = method
                    connectTimeout = 5000
                    readTimeout = 6000
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    setRequestProperty("Accept", "application/json")
                    doInput = true
                    if (body != null) doOutput = true
                }
                if (body != null) {
                    conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                }
                val code = conn.responseCode
                val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
                    ?.bufferedReader()?.readText() ?: ""
                val json = if (code in 200..299) JSONObject(text) else null
                main.post { cb(json) }
            } catch (_: Throwable) {
                main.post { cb(null) }
            } finally {
                conn?.disconnect()
            }
        }
    }

    /** Upserts this device's score; the server recomputes & clamps the rating. */
    fun pushScore(prefs: Prefs, cb: (Boolean) -> Unit = {}) {
        val body = JSONObject()
            .put("deviceId", prefs.deviceId)
            .put("name", prefs.playerName)
            .put("avatarId", prefs.avatarId.intValue)
            .put("maxLevel", prefs.maxLevel.intValue)
            .put("totalCoinsEarned", prefs.totalCoinsEarned.intValue)
            .put("dailyStreak", prefs.dailyStreak.intValue)
            .put("wins", prefs.wins.intValue)
            .put("losses", prefs.losses.intValue)
        request("POST", "/api/score", body) { cb(it?.optBoolean("ok") == true) }
    }

    /** Top 100 + this device's own entry (even when outside the top 100). */
    fun fetchTop(deviceId: String, cb: (top: List<Entry>?, me: Entry?) -> Unit) {
        val q = try {
            URLEncoder.encode(deviceId, "UTF-8")
        } catch (_: Throwable) {
            ""
        }
        request("GET", "/api/leaderboard?deviceId=$q", null) { j ->
            if (j == null || !j.optBoolean("ok")) {
                cb(null, null)
                return@request
            }
            val arr = j.optJSONArray("top")
            val top = mutableListOf<Entry>()
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.let { top.add(entry(it)) }
                }
            }
            val meJson = j.optJSONObject("me")
            cb(top, meJson?.let { entry(it) })
        }
    }

    /**
     * Cloud save: pushes our snapshot; the server clamps it (absolute caps +
     * per-hour growth budget) and returns the canonical state to merge back.
     */
    fun pushSave(key: String, deviceId: String, state: JSONObject, cb: ((JSONObject?) -> Unit)?) {
        val body = JSONObject()
            .put("key", key)
            .put("deviceId", deviceId)
            .put("state", state)
        request("POST", "/api/save", body) { j ->
            if (j == null || !j.optBoolean("ok")) {
                cb?.invoke(null)
                return@request
            }
            cb?.invoke(j.optJSONObject("state"))
        }
    }

    private fun entry(j: JSONObject) = Entry(
        rank = j.optInt("rank"),
        name = j.optString("name", "Racer"),
        avatarId = j.optInt("avatarId", 0),
        rating = j.optInt("rating", 0),
        maxLevel = j.optInt("maxLevel", 1),
        deviceId = j.optString("deviceId", ""),
    )
}
