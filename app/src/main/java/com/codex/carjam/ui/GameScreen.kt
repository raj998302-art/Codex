package com.codex.carjam.ui

import android.app.Activity
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                Fx.CRACK -> sound.crack()
                Fx.HAMMER -> {
                    sound.hammer()
                    if (prefs.vibrateOn.value) view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                }

                Fx.SHUFFLE -> sound.shuffle()
                Fx.CHAINED -> {
                    sound.chainLocked()
                    if (prefs.vibrateOn.value) view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                }

                Fx.CHAINBREAK -> sound.chainBreak()
                    Fx.UNLOCK -> sound.unlock()
                    Fx.ELIMINATE -> sound.eliminate()
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
                        // v3.0: every win drops coins into the Piggy Bank
                        prefs.addPiggy(20)
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
    val spec = remember(level, attempt) {
        LevelGenerator.generate(
            level,
            mysteryBoost = if (practice) 0 else event.mysteryBoost,
            manualScene = prefs.manualScene.value,
            specialDay = !practice && event.isSpecial(),
        )
    }
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
    var showBoostShop by remember { mutableStateOf(false) }
    var showMoreSpot by remember { mutableStateOf(false) }
    var hammerArmed by remember { mutableStateOf(false) }
    var elimArmed by remember { mutableStateOf(false) }
    var boostGift by remember { mutableStateOf(false) }

    // one-time v2.7 booster welcome gift
    LaunchedEffect(Unit) {
        if (prefs.grantBoosterGiftIfNeeded()) boostGift = true
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF0E1B26))) {
        var viewSize by remember { mutableStateOf(IntSize(1, 1)) }
        Canvas(
            Modifier
                .fillMaxSize()
                .onSizeChanged { viewSize = it }
                .pointerInput(engine, hammerArmed, elimArmed) {
                    detectTapGestures { off ->
                        val s = min(viewSize.width / Dim.VW, viewSize.height / Dim.VH)
                        val ox = (viewSize.width - Dim.VW * s) / 2f
                        val oy = (viewSize.height - Dim.VH * s) / 2f
                        val gx = (off.x - ox) / s
                        val gy = (off.y - oy) / s
                        // locked More-Spot slots open their purchase dialog
                        val lockedHit = engine.lockedSlotCenters().any { c ->
                            kotlin.math.abs(gx - c.x) < 84f && kotlin.math.abs(gy - c.y) < 130f
                        }
                        if (lockedHit && engine.result == GameResult.PLAYING) {
                            showMoreSpot = true
                            sound.tap()
                        } else if (elimArmed) {
                            // v3.0 Eliminate: insta-send ANY tapped car out of the arena
                            // (even frozen ones) — spent only on a successful hit
                            val hit = engine.hitCarAt(gx, gy)
                            if (hit != null && engine.eliminateCar(hit)) {
                                prefs.useElim()
                                elimArmed = false
                            }
                        } else if (hammerArmed) {
                            // armed hammer: only shatters ice, never moves cars,
                            // and is only spent on a successful smash
                            val hit = engine.hitCarAt(gx, gy)
                            if (hit != null && engine.smashIce(hit)) {
                                prefs.useHammer()
                                hammerArmed = false
                            }
                        } else {
                            engine.onTap(gx, gy)
                        }
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
            val wonCoins = if (practice) 0 else engine.coinsEarned + winBonus
            WinDialog(
                level = level,
                coinsEarned = wonCoins,
                practice = practice,
                prefs = prefs,
                canMultiply = ads.rewardedReady.value,
                onMultiplyX5 = {
                    val act = view.context as? Activity
                    if (act != null) {
                        ads.showRewarded(act, onReward = {
                            prefs.addCoins(wonCoins * 4)
                            CloudSave.sync(prefs, force = true)
                            onNext()
                        })
                    }
                },
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
        // armed-hammer hint chip
        if (hammerArmed && result == GameResult.PLAYING) {
            Pill(
                "TAP A FROZEN CAR TO SMASH ITS ICE",
                bg = Color(0xFF2E7CC4).copy(alpha = 0.92f),
                icon = { GameIcon(GameIconKind.HAMMER, 22.dp) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 84.dp),
            )
        }
        // armed-eliminate hint chip
        if (elimArmed && result == GameResult.PLAYING) {
            Pill(
                "TAP ANY CAR TO ELIMINATE IT",
                bg = Color(0xFF8E4BD6).copy(alpha = 0.92f),
                icon = { GameIcon(GameIconKind.ELIMINATE, 22.dp) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 84.dp),
            )
        }

        // booster belt
        Row(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BoostChip(
                kind = GameIconKind.HAMMER,
                count = prefs.hammers.intValue,
                armed = hammerArmed,
                onClick = {
                    when {
                        hammerArmed -> {
                            hammerArmed = false
                            sound.tap()
                        }

                        prefs.hammers.intValue > 0 && result == GameResult.PLAYING -> {
                            hammerArmed = true
                            elimArmed = false
                            sound.tap()
                        }

                        else -> {
                            showBoostShop = true
                            sound.tap()
                        }
                    }
                },
            )
            SpacerW(8.dp)
            SpacerW(8.dp)
            BoostChip(
                kind = GameIconKind.ELIMINATE,
                count = prefs.elims.intValue,
                armed = elimArmed,
                onClick = {
                    when {
                        elimArmed -> {
                            elimArmed = false
                            sound.tap()
                        }

                        prefs.elims.intValue > 0 && result == GameResult.PLAYING -> {
                            elimArmed = true
                            hammerArmed = false
                            sound.tap()
                        }

                        else -> {
                            showBoostShop = true
                            sound.tap()
                        }
                    }
                },
            )
            SpacerW(8.dp)
            BoostChip(
                kind = GameIconKind.REFRESH,
                count = prefs.refreshes.intValue,
                armed = false,
                onClick = {
                    when {
                        prefs.refreshes.intValue > 0 && result == GameResult.PLAYING -> {
                            if (engine.chaosRefresh()) prefs.useRefresh()
                        }

                        else -> {
                            showBoostShop = true
                            sound.tap()
                        }
                    }
                },
            )
            SpacerW(8.dp)
            BoostChip(
                kind = GameIconKind.SHUFFLE,
                count = prefs.shufflesStock.intValue,
                armed = false,
                onClick = {
                    when {
                        prefs.shufflesStock.intValue > 0 && result == GameResult.PLAYING -> {
                            if (engine.shuffleQueue()) prefs.useShuffle()
                        }

                        else -> {
                            showBoostShop = true
                            sound.tap()
                        }
                    }
                },
            )
        }

        // one-time booster welcome gift card
        if (boostGift) {
            DialogOverlay {
                PanelCard(Modifier.padding(24.dp)) {
                    DialogTitleText("WELCOME GIFT")
                    SpacerH(8.dp)
                    BasicText(
                        "Two new boosters just joined your belt — on the house!",
                        style = TextStyle(
                            color = Color(0xFF8C6A3F),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                        ),
                    )
                    SpacerH(10.dp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GameIcon(GameIconKind.HAMMER, 30.dp)
                        SpacerW(6.dp)
                        Pill("2 ICE HAMMERS", bg = Color(0xFF2E7CC4).copy(alpha = 0.9f))
                        SpacerW(8.dp)
                        GameIcon(GameIconKind.SHUFFLE, 30.dp)
                        SpacerW(6.dp)
                        Pill("3 QUEUE MIXES", bg = Color(0xFF0E9E94).copy(alpha = 0.9f))
                    }
                    SpacerH(12.dp)
                    SquishyButton(
                        "LET'S GO",
                        onClick = { boostGift = false; sound.tap() },
                        height = 46.dp,
                        textSize = 16.dp,
                    )
                    SpacerH(4.dp)
                }
            }
        }

        if (showBoostShop) {
            BoosterShopDialog(prefs = prefs, onClose = { showBoostShop = false })
        }

        if (showMoreSpot) {
            val act = view.context as? Activity
            MoreSpotDialog(
                prefs = prefs,
                rewardedReady = ads.rewardedReady.value && act != null,
                onCoin = {
                    if (prefs.coins.intValue >= 100 && engine.unlockSlot()) {
                        prefs.addCoins(-100)
                        CloudSave.sync(prefs, force = true)
                    }
                    showMoreSpot = false
                },
                onFree = {
                    if (act != null) {
                        ads.showRewarded(act, onReward = { engine.unlockSlot() })
                    }
                    showMoreSpot = false
                },
                onClose = { showMoreSpot = false },
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

/** Squishy booster button: rounded square icon tile with a count badge. */
@Composable
private fun BoostChip(
    kind: GameIconKind,
    count: Int,
    armed: Boolean,
    onClick: () -> Unit,
) {
    val top = when (kind) {
        GameIconKind.HAMMER -> Color(0xFF6FB6FF)
        GameIconKind.ELIMINATE -> Color(0xFFB983F5)
        GameIconKind.REFRESH -> Color(0xFF6FEE85)
        else -> Color(0xFF5FE8DC)
    }
    val bottom = when (kind) {
        GameIconKind.HAMMER -> Color(0xFF2E5FBB)
        GameIconKind.ELIMINATE -> Color(0xFF6D28B8)
        GameIconKind.REFRESH -> Color(0xFF1FA94F)
        else -> Color(0xFF0E9E94)
    }
    Box(
        Modifier
            .size(56.dp)
            .background(bottom, RoundedCornerShape(16.dp))
            .padding(top = 0.dp),
    ) {
        Box(
            Modifier
                .size(56.dp)
                .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(top, bottom)), RoundedCornerShape(16.dp))
                .border(
                    width = if (armed) 3.dp else 2.dp,
                    color = if (armed) Color(0xFFFFF3A6) else Color.White.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(16.dp),
                )
                .clickable { onClick() },
        ) {
            GameIcon(kind, 34.dp, modifier = Modifier.align(Alignment.Center))
            // count / buy-me badge
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(3.dp)
                    .background(if (count > 0) Color(0xFF2B2B33) else Color(0xFF3DDC5F), RoundedCornerShape(8.dp))
                    .border(1.5.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 4.dp, vertical = 0.dp),
            ) {
                BasicText(
                    if (count > 0) "$count" else "+",
                    style = TextStyle(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold),
                )
            }
        }
    }
}
