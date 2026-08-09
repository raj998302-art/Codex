package com.codex.carjam.game

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import com.codex.carjam.R
import kotlin.concurrent.thread

/**
 * Sound effects via [ToneGenerator] + a soft looping background track
 * (res/raw/bg_music.wav, procedurally composed). Every call is a safe no-op
 * when audio is unavailable. [active] lets surfaces (settings, activity
 * lifecycle) reach the live instance without threading one through Compose.
 */
class SoundManager(private val prefs: Prefs) {
    private var tone: ToneGenerator? = null

    @Volatile
    private var failed = false

    private var appContext: Context? = null
    private var music: MediaPlayer? = null

    companion object {
        /** The SoundManager currently bound to the UI. */
        var active: SoundManager? = null
    }

    /** Bind the application context and honour the saved music preference. */
    fun attach(context: Context) {
        appContext = context.applicationContext
        applyMusicPref()
    }

    fun applyMusicPref() {
        if (prefs.musicOn.value) startMusic() else pauseMusic()
    }

    private fun startMusic() {
        val ctx = appContext ?: return
        if (music == null) {
            music = try {
                MediaPlayer.create(ctx, R.raw.bg_music)?.apply {
                    isLooping = true
                    setVolume(0.32f, 0.32f)
                }
            } catch (_: Throwable) {
                null
            }
        }
        try {
            music?.takeIf { !it.isPlaying }?.start()
        } catch (_: Throwable) {
        }
    }

    fun pauseMusic() {
        try {
            music?.takeIf { it.isPlaying }?.pause()
        } catch (_: Throwable) {
        }
    }

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

    fun crack() = play(ToneGenerator.TONE_CDMA_PIP, 35)

    fun hammer() = play(ToneGenerator.TONE_PROP_BEEP2, 150)

    fun shuffle() = play(ToneGenerator.TONE_PROP_ACK, 60)

    fun chainLocked() = play(ToneGenerator.TONE_PROP_NACK, 120)

    fun chainBreak() = play(ToneGenerator.TONE_PROP_BEEP, 90)

    fun unlock() = play(ToneGenerator.TONE_PROP_BEEP2, 110)

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
        try {
            music?.release()
        } catch (_: Throwable) {
        }
        music = null
    }
}
