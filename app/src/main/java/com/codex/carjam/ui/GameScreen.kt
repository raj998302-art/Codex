package com.codex.carjam.ui

import android.app.Activity
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
import com.codex.carjam.game.CloudSave
import com.codex.carjam.game.Dim
import com.codex.carjam.game.Events
import com.codex.carjam.game.Fx
import com.codex.carjam.game.GameEngine
import com.codex.carjam.game.GameResult
import com.codex.carjam.game.LevelGenerator
import com.codex.carjam.game.LiveBoard
import com.codex.carjam.game.Prefs
import com.codex.carjam.game.SoundManager
import com.codex.carjam.game.render.GameIconKind
import com.codex.carjam.game.render.Painters
import com.codex.carjam.monetize.AdsManager
import com.codex.carjam.monetize.BillingManager
import kotlin.math.min

@Composable
fun GameScreen(
    level: Int,
    attempt: Int,
    practice: Boolean,
    prefs: Prefs,
    sound: SoundManager,
    ads: AdsManager,
    billing: BillingManager,
    onHome: () -> Unit,
    onNext: () -> Unit,
    onRetry: () -> Unit,
) {
    val view = LocalView.current
    val event = remember { Events.today() }

    val fx = remember(sound, prefs, view) {
        { f: Fx ->
            when (f) {
                Fx.TAP -> sound.tap()
                Fx.BLOCKED -> {
                    sound.blocked()
                    if (prefs.vibrateOn.value) view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                }

                Fx.WHOOSH -> sound.whoosh()
                Fx.BOARD -> {
                    sound.board()
                    if (!practice) prefs.noteBoard()
                }
                Fx.COIN -> sound.coin()
                Fx.DEPART -> sound.depart()
                Fx.REVEAL -> sound.reveal()
                Fx.WIN -> {
                    sound.win()
                    if (practice) prefs.recordPractice() else prefs.recordWin()
                    if (!practice) {
                        LiveBoard.sync(prefs, force = true)
                        CloudSave.sync(prefs, force = true)
                    }
                }

                Fx.LOSE -> {
                    sound.lose()
                    if (!practice) prefs.recordLoss()
                }
            }
        }
    }

    val activeMult = if (practice) 0f else event.coinMult
    val winBonus = (40 * activeMult).toInt()
    val spec = remember(level, attempt) { LevelGenerator.generate(level, mysteryBoost = if (practice) 0 else event.mysteryBoost) }
    val engine = remember(level, attempt) {
        GameEngine(
            spec = spec,
            onFx = fx,
            onCoinLanded = { v -> if (!practice) prefs.addCoins(v) },
            onWin = {
                if (!practice) {
                    prefs.unlockLevel(level + 1)
                    prefs.addCoins(winBonus)
                }
            },
        ).apply {
            coinMult = activeMult
            grantBonusSlots(if (practice) 0 else event.bonusSlots)
        }
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
    var showShop by remember { mutableStateOf(false) }

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
                CoinPill(prefs.coins.intValue, onPlus = { showShop = true; sound.coin() })
                SpacerW(6.dp)
                GemPill(prefs.gems.intValue, onPlus = { showShop = true; sound.coin() })
            }
            Row(
                Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.weight(1f))
                Pill(
                    if (practice) "PRACTICE MODE — free play, no coins at stake" else event.title,
                    bg = if (practice) Color(0xFF607D8B).copy(alpha = 0.9f) else event.accent.copy(alpha = 0.85f),
                    icon = { GameIcon(if (practice) GameIconKind.GRAD_CAP else eventIconKind(event.id), 20.dp) },
                )
                Spacer(Modifier.weight(1f))
            }
        }

        // recompose on every frame so result delays evaluate live
        engine.frame.longValue
        val result = engine.result
        if (result == GameResult.WON && engine.resultAge() > 900f) {
            WinDialog(
                level = level,
                coinsEarned = if (practice) 0 else engine.coinsEarned + winBonus,
                onNext = {
                    if (!practice) (view.context as? Activity)?.let { act -> ads.maybeShowInterstitial(act, level) }
                    onNext()
                },
                onHome = onHome,
            )
        }
        if (result == GameResult.LOST && engine.resultAge() > 600f) {
            LoseDialog(
                reason = engine.loseReason,
                canRevive = ads.rewardedReady.value,
                gemsAvailable = prefs.gems.intValue >= 25,
                onRevive = {
                    val act = view.context as? Activity
                    if (act != null) {
                        ads.showRewarded(act, onReward = { engine.revive(2) })
                    } else {
                        engine.revive(2)
                    }
                },
                onReviveGems = {
                    if (prefs.spendGems(25)) engine.revive(2)
                },
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
        if (showShop) {
            val act = view.context as? Activity
            if (act != null) {
                ShopDialog(
                    billing = billing,
                    ads = ads,
                    prefs = prefs,
                    activity = act,
                    onClose = { showShop = false },
                )
            } else {
                showShop = false
            }
        }
    }
}
