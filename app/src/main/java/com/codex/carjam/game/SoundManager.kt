package com.codex.carjam.game

import android.media.AudioManager
import android.media.ToneGenerator
import kotlin.concurrent.thread

/**
 * Zero-asset sound effects, synthesised with [ToneGenerator] so the APK stays tiny.
 * All calls are safe no-ops when sound is disabled or the device has no tonegen.
 */
class SoundManager(private val prefs: Prefs) {
    private var tone: ToneGenerator? = null

    @Volatile
    private var failed = false

    @Synchronized
    private fun ensure(): ToneGenerator? {
        if (failed) return null
        if (tone == null) {
            tone = try {
                ToneGenerator(AudioManager.STREAM_MUSIC, 78)
            } catch (t: Throwable) {
                failed = true
                null
            }
        }
        return tone
    }

    private fun play(toneType: Int, ms: Int) {
        if (!prefs.soundOn.value) return
        thread {
            try {
                ensure()?.startTone(toneType, ms)
            } catch (_: Throwable) {
            }
        }
    }

    fun tap() = play(ToneGenerator.TONE_PROP_ACK, 45)

    fun blocked() = play(ToneGenerator.TONE_SUP_CONGESTION, 110)

    fun whoosh() = play(ToneGenerator.TONE_CDMA_PIP, 90)

    fun board() = play(ToneGenerator.TONE_PROP_BEEP, 40)

    fun coin() = play(ToneGenerator.TONE_PROP_PROMPT, 60)

    fun depart() = play(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 140)

    fun reveal() = play(ToneGenerator.TONE_PROP_BEEP2, 90)

    fun win() {
        thread {
            try {
                val t = ensure() ?: return@thread
                t.startTone(ToneGenerator.TONE_CDMA_ALERT_INCALL_LITE, 160)
                Thread.sleep(170)
                t.startTone(ToneGenerator.TONE_PROP_ACK, 160)
                Thread.sleep(170)
                t.startTone(ToneGenerator.TONE_PROP_PROMPT, 320)
            } catch (_: Throwable) {
            }
        }
    }

    fun lose() {
        thread {
            try {
                val t = ensure() ?: return@thread
                t.startTone(ToneGenerator.TONE_SUP_ERROR, 260)
                Thread.sleep(220)
                t.startTone(ToneGenerator.TONE_SUP_CONGESTION, 320)
            } catch (_: Throwable) {
            }
        }
    }

    fun release() {
        try {
            tone?.release()
        } catch (_: Throwable) {
        }
        tone = null
    }
}
