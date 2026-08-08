package com.codex.carjam.ui

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.codex.carjam.game.Dim
import com.codex.carjam.game.Fx
import com.codex.carjam.game.GameEngine
import com.codex.carjam.game.GameResult
import com.codex.carjam.game.LevelGenerator
import com.codex.carjam.game.Prefs
import com.codex.carjam.game.SoundManager
import com.codex.carjam.game.render.Painters
import kotlin.math.min

@Composable
fun GameScreen(
    level: Int,
    attempt: Int,
    prefs: Prefs,
    sound: SoundManager,
    onHome: () -> Unit,
    onNext: () -> Unit,
    onRetry: () -> Unit,
) {
    val view = LocalView.current

    val fx = remember(sound, prefs, view) {
        { f: Fx ->
            when (f) {
                Fx.TAP -> sound.tap()
                Fx.BLOCKED -> {
                    sound.blocked()
                    if (prefs.vibrateOn.value) view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                }

                Fx.WHOOSH -> sound.whoosh()
                Fx.BOARD -> sound.board()
                Fx.COIN -> sound.coin()
                Fx.DEPART -> sound.depart()
                Fx.REVEAL -> sound.reveal()
                Fx.WIN -> sound.win()
                Fx.LOSE -> sound.lose()
            }
        }
    }

    val spec = remember(level) { LevelGenerator.generate(level) }
    val engine = remember(level, attempt) {
        GameEngine(
            spec = spec,
            onFx = fx,
            onCoinLanded = { v -> prefs.addCoins(v) },
            onWin = {
                prefs.unlockLevel(level + 1)
                prefs.addCoins(40)
            },
        )
    }

    LaunchedEffect(engine) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last == 0L) last = now
                val dt = (now - last) / 1_000_000f
                last = now
                engine.tick(dt)
            }
        }
    }

    BackHandler { onHome() }

    var showSettings by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(Color(0xFF0E1B26))) {
        var viewSize by remember { mutableStateOf(IntSize(1, 1)) }
        Canvas(
            Modifier
                .fillMaxSize()
                .onSizeChanged { viewSize = it }
                .pointerInput(engine) {
                    detectTapGestures { off ->
                        val s = min(viewSize.width / Dim.VW, viewSize.height / Dim.VH)
                        val ox = (viewSize.width - Dim.VW * s) / 2f
                        val oy = (viewSize.height - Dim.VH * s) / 2f
                        engine.onTap((off.x - ox) / s, (off.y - oy) / s)
                    }
                },
        ) {
            engine.frame.longValue // redraw subscription
            val s = min(size.width / Dim.VW, size.height / Dim.VH)
            val ox = (size.width - Dim.VW * s) / 2f
            val oy = (size.height - Dim.VH * s) / 2f
            drawRect(Color(0xFF0E1B26))
            withTransform({ translate(ox, oy); scale(s, s, androidx.compose.ui.geometry.Offset.Zero) }) {
                with(Painters) { scene(engine) }
            }
        }

        // HUD
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GearButton { showSettings = true; sound.tap() }
                Spacer(Modifier.weight(1f))
                Pill("Level $level", bg = Color.Black.copy(alpha = 0.55f))
                Spacer(Modifier.weight(1f))
                CoinPill(prefs.coins.intValue, onPlus = { sound.coin() })
            }
        }

        // recompose on every frame so result delays evaluate live
        engine.frame.longValue
        val result = engine.result
        if (result == GameResult.WON && engine.resultAge() > 900f) {
            WinDialog(
                level = level,
                coinsEarned = engine.coinsEarned + 40,
                onNext = onNext,
                onHome = onHome,
            )
        }
        if (result == GameResult.LOST && engine.resultAge() > 600f) {
            LoseDialog(
                reason = engine.loseReason,
                onRetry = { onRetry() },
                onHome = onHome,
            )
        }
        if (showSettings) {
            SettingsDialog(
                prefs = prefs,
                showRestart = true,
                onResume = { showSettings = false },
                onRestart = {
                    showSettings = false
                    onRetry()
                },
                onHome = {
                    showSettings = false
                    onHome()
                },
            )
        }
    }
}
