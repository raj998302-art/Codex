package com.codex.carjam.ui

import android.app.Activity
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.codex.carjam.game.CarColor
import com.codex.carjam.game.CarType
import com.codex.carjam.game.Achievements
import com.codex.carjam.game.CloudSave
import com.codex.carjam.game.DailyMissions
import com.codex.carjam.game.DailyRewards
import com.codex.carjam.game.Events
import com.codex.carjam.game.LevelTheme
import com.codex.carjam.game.LeaderboardApi
import com.codex.carjam.game.LiveBoard
import com.codex.carjam.game.Prefs
import com.codex.carjam.game.SoundManager
import com.codex.carjam.game.render.GameIconKind
import com.codex.carjam.game.render.Painters
import com.codex.carjam.monetize.AdsManager
import com.codex.carjam.monetize.BillingManager
import com.codex.carjam.monetize.PlayGamesManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import kotlin.math.min
import kotlin.math.sin

@Composable
fun HomeScreen(
    prefs: Prefs,
    sound: SoundManager,
    ads: AdsManager,
    billing: BillingManager,
    pgs: PlayGamesManager,
    online: Boolean,
    onPlay: (Int) -> Unit,
    onPractice: () -> Unit,
    onRefreshNet: () -> Unit,
) {
    var showLevels by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showShop by remember { mutableStateOf(false) }
    var showDaily by remember { mutableStateOf(false) }
    var showEvents by remember { mutableStateOf(false) }
    var showRank by remember { mutableStateOf(false) }
    var showProfile by remember { mutableStateOf(false) }
    var showQuests by remember { mutableStateOf(false) }
    var showWelcome by remember { mutableStateOf(!prefs.welcomed.value) }
    var seasonPrize by remember { mutableStateOf<SeasonPrize?>(null) }
    val theme = LevelTheme.entries[(prefs.maxLevel.intValue - 1) % LevelTheme.entries.size]
    val event = remember { Events.today() }
    val dailyReady = DailyRewards.canClaim(prefs)
    val activity = LocalActivity()
    LaunchedEffect(online) {
        if (online) {
            activity?.let { pgs.silentCheck(it) }
            LiveBoard.sync(prefs)
            CloudSave.sync(prefs)
            // weekly season prize: auto-credit once per finished season
            LeaderboardApi.fetchLastWeek(prefs.deviceId) { weekId, rank, _ ->
                if (weekId != null && rank != null && weekId > prefs.lastSeasonWeek.intValue) {
                    val (coins, gems) = seasonPrizeForRank(rank)
                    prefs.addCoins(coins)
                    if (gems > 0) prefs.addGems(gems)
                    prefs.setLastSeasonWeek(weekId)
                    CloudSave.sync(prefs, force = true)
                    seasonPrize = SeasonPrize(weekId, rank, coins, gems)
                }
            }
        }
    }
    // live-ops popup: pitch today's event once per day (after onboarding)
    val todayEpoch = (System.currentTimeMillis() / 86_400_000L).toInt()
    LaunchedEffect(Unit) {
        if (prefs.welcomed.value && prefs.eventSeenDay.intValue != todayEpoch) {
            prefs.markEventSeen(todayEpoch)
            showEvents = true
        }
    }

    Box(Modifier.fillMaxSize()) {
        HomeBackdrop(theme)

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
        ) {
            // top HUD: gear + flexible identity chip + compact wallets (never overflows)
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                GearButton { sound.tap(); showSettings = true }
                SpacerW(8.dp)
                Row(
                    Modifier
                        .weight(1f)
                        .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                        .border(2.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                        .clickable { sound.tap(); showProfile = true }
                        .padding(start = 4.dp, top = 4.dp, bottom = 4.dp, end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AvatarIcon(prefs.avatarId.intValue, 30.dp)
                    SpacerW(6.dp)
                    BasicText(
                        text = prefs.playerName,
                        style = TextStyle(color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.ExtraBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                SpacerW(8.dp)
                CoinPill(prefs.coins.intValue, compact = true, onPlus = { sound.tap(); if (!online) onRefreshNet(); showShop = true })
                SpacerW(5.dp)
                GemPill(prefs.gems.intValue, compact = true, onPlus = { sound.tap(); if (!online) onRefreshNet(); showShop = true })
            }

            SpacerH(34.dp)
            OutlinedTextC("CAR JAM", 64.dp, fill = Color.White, outline = Color(0xFF20303C))
            OutlinedTextC("SOLVER", 30.dp, fill = Color(0xFFFFD32E), outline = Color(0xFF7A4A00))
            SpacerH(8.dp)
            if (online) {
                EventBannerCard(
                    event = event,
                    height = 86.dp,
                    modifier = Modifier.padding(horizontal = 6.dp),
                    trailingText = "LIVE NOW — tap for schedule",
                    onClick = { sound.tap(); showEvents = true },
                )
            } else {
                Pill(
                    text = "OFFLINE MODE — everything still playable",
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    bg = Color(0xFF607D8B).copy(alpha = 0.9f),
                    icon = { GameIcon(GameIconKind.WIFI_OFF, 20.dp) },
                )
            }

            SpacerH(44.dp)

            // big round PLAY button with a sonar pulse ring
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            val playPulse = rememberInfiniteTransition(label = "play-pulse")
            val pulse by playPulse.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
                label = "pulse",
            )
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(148.dp),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val ringR = 57.dp.toPx() + pulse * 16.dp.toPx()
                    drawCircle(
                        color = Color.White.copy(alpha = (1f - pulse) * 0.38f),
                        radius = ringR,
                        center = center,
                        style = Stroke((1f - pulse) * 3.dp.toPx() + 1f),
                    )
                    drawCircle(
                        color = Color(0xFF6FEE85).copy(alpha = (1f - pulse) * 0.16f),
                        radius = ringR,
                        center = center,
                    )
                }
                Box(
                    Modifier
                        .size(if (pressed) 106.dp else 112.dp)
                        .background(Brush.verticalGradient(listOf(Color(0xFF6FEE85), Color(0xFF1FA94F))), CircleShape)
                        .border(6.dp, Color.White, CircleShape)
                        .clickable(interactionSource = interaction, indication = null) {
                            sound.tap()
                            onPlay(prefs.maxLevel.intValue)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.size(46.dp)) {
                        val path = Path().apply {
                            moveTo(size.width * 0.30f, size.height * 0.16f)
                            lineTo(size.width * 0.86f, size.height * 0.50f)
                            lineTo(size.width * 0.30f, size.height * 0.84f)
                            close()
                        }
                        drawPath(path, Color.White)
                    }
                }
            }
            SpacerH(14.dp)
            Pill(
                text = "LEVEL ${prefs.maxLevel.intValue}",
                modifier = Modifier.align(Alignment.CenterHorizontally),
                bg = Color.Black.copy(alpha = 0.45f),
            )

            SpacerH(22.dp)

            // quick actions row
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                val questBadge = DailyMissions.anyClaimable(prefs) || Achievements.anyClaimable(prefs)
                ActionChip(GameIconKind.CART, "SHOP") { sound.tap(); showShop = true }
                ActionChip(GameIconKind.BOLT, "QUESTS", badge = questBadge) { sound.tap(); showQuests = true }
                ActionChip(GameIconKind.GIFT, "GIFT", badge = dailyReady) { sound.tap(); showDaily = true }
                ActionChip(GameIconKind.CALENDAR, "EVENTS") { sound.tap(); showEvents = true }
                ActionChip(GameIconKind.TROPHY, "RANK") { sound.tap(); showRank = true }
            }

            SpacerH(16.dp)
            SquishyButton(
                "SELECT LEVEL",
                onClick = { sound.tap(); showLevels = true },
                modifier = Modifier.width(240.dp).align(Alignment.CenterHorizontally),
                top = Color(0xFF6FB6FF),
                bottom = Color(0xFF3B7FE0),
                textSize = 18.dp,
                height = 50.dp,
            )
            SpacerH(10.dp)
            SquishyButton(
                "PRACTICE MODE",
                onClick = { sound.tap(); onPractice() },
                modifier = Modifier.width(240.dp).align(Alignment.CenterHorizontally),
                top = Color(0xFFB678E8),
                bottom = Color(0xFF8A45C4),
                textSize = 16.dp,
                height = 46.dp,
                icon = { GameIcon(GameIconKind.GRAD_CAP, 22.dp) },
            )

            Spacer(Modifier.weight(1f))

            // AdMob banner (hidden when No-Ads pack owned or offline)
            if (online && !prefs.removeAds.value) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    factory = { ctx ->
                        AdView(ctx).apply {
                            setAdSize(AdSize.BANNER)
                            adUnitId = AdsManager.BANNER_ID
                            loadAd(AdRequest.Builder().build())
                        }
                    },
                )
            } else {
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(Modifier.weight(1f))
                    Pill(
                        if (online) "Tap cars • match colours • clear the jam" else "Offline — ads & shop take a break",
                        bg = Color.Black.copy(alpha = 0.35f),
                        icon = if (online) null else ({ GameIcon(GameIconKind.WIFI_OFF, 18.dp) }),
                    )
                    Spacer(Modifier.weight(1f))
                }
            }
        }

        // ---- dialogs
        seasonPrize?.let { prize ->
            SeasonPrizeDialog(prize = prize, onClose = { seasonPrize = null })
        }
        if (showWelcome) {
            WelcomeDialog(
                onContinuePlay = {
                    prefs.markWelcomed()
                    showWelcome = false
                    activity?.let { pgs.signIn(it) }
                },
                onGuest = {
                    prefs.markWelcomed()
                    showWelcome = false
                },
            )
        }
        if (showLevels) {
            LevelSelectDialog(
                maxLevel = prefs.maxLevel.intValue,
                onPick = { lvl -> showLevels = false; onPlay(lvl) },
                onClose = { showLevels = false },
            )
        }
        if (showSettings) {
            SettingsDialog(
                prefs = prefs,
                showRestart = false,
                onResume = { showSettings = false },
                onRestart = {},
                onHome = { showSettings = false },
            )
        }
        if (showShop) {
            (LocalActivity())?.let { act ->
                ShopDialog(
                    billing = billing,
                    ads = ads,
                    prefs = prefs,
                    activity = act,
                    onClose = { showShop = false },
                )
            } ?: run { showShop = false }
        }
        if (showQuests) {
            QuestsDialog(
                prefs = prefs,
                onClaimed = { sound.coin(); CloudSave.sync(prefs, force = true) },
                onClose = { showQuests = false },
            )
        }
        if (showDaily) {
            DailyRewardDialog(
                prefs = prefs,
                onClaimed = { sound.coin(); CloudSave.sync(prefs, force = true) },
                onClose = { showDaily = false },
            )
        }
        if (showEvents) {
            EventsDialog(onClose = { showEvents = false })
        }
        if (showRank) {
            LeaderboardDialog(prefs = prefs, onClose = { showRank = false })
        }
        if (showProfile) {
            activity?.let {
                ProfileDialog(prefs = prefs, pgs = pgs, activity = it, onClose = { showProfile = false })
            } ?: run { showProfile = false }
        }
    }
}

@Composable
private fun LocalActivity(): Activity? =
    androidx.compose.ui.platform.LocalContext.current as? Activity

@Composable
private fun ActionChip(kind: GameIconKind, label: String, badge: Boolean = false, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(60.dp)
                .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                .border(2.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                .clickable { onClick() },
            contentAlignment = Alignment.Center,
        ) {
            GameIcon(kind, 34.dp)
            if (badge) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-6).dp)
                        .size(16.dp)
                        .background(Color(0xFFFF4757), CircleShape)
                        .border(2.dp, Color.White, CircleShape),
                )
            }
        }
        BasicText(
            text = label,
            style = TextStyle(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp),
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/** Animated toy-town backdrop reusing the game's own painters. */
@Composable
private fun HomeBackdrop(theme: LevelTheme) {
    val inf = rememberInfiniteTransition(label = "home-bg")
    val phase by inf.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(3600), RepeatMode.Restart),
        label = "bob",
    )

    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        // sky + ground
        drawRect(Brush.verticalGradient(listOf(theme.skyTop, theme.skyBottom)))
        drawRect(theme.road, topLeft = Offset(0f, h * 0.60f), size = Size(w, h * 0.075f))
        drawRect(theme.arenaBg, topLeft = Offset(0f, h * 0.675f), size = Size(w, h * 0.325f))
        drawRect(
            theme.arenaEdge,
            topLeft = Offset(0f, h * 0.675f - 10f),
            size = Size(w, 10f),
        )

        val u = min(w, h) / 26f

        // queue of passengers, gently bobbing
        val palette = CarColor.playable
        val midY = h * 0.545f
        for (i in 0 until 9) {
            val x = w * 0.16f + i * w * 0.085f
            val dy = sin(phase + i * 0.7f) * u * 0.16f
            with(Painters) {
                drawPassenger(x, midY + dy, palette[i % palette.size], scale = u / 34f)
            }
        }

        // mini jam board bottom area
        val boardCx = w / 2f
        val boardTop = h * 0.72f
        val cols = 5
        val rows = 3
        val cw = w * 0.84f / cols
        val chh = (h * 0.24f) / rows
        var k = 0
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val x = boardCx - cw * 2f + cw * (c + 0.5f)
                val y = boardTop + chh * (r + 0.5f)
                val angle = when ((r + c) % 4) {
                    0 -> 8f
                    1 -> 90f
                    2 -> 186f
                    else -> 272f
                } + sin(phase * 0.4f + k) * 2f
                val type = if ((r * cols + c) % 7 == 3) CarType.VAN else CarType.SEDAN
                val color = palette[(k * 3 + r) % palette.size]
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.10f),
                    topLeft = Offset(x - u * 1.05f, y - u * 1.7f),
                    size = Size(u * 2.1f, u * 3.4f),
                    cornerRadius = CornerRadius(u * 0.6f),
                )
                with(Painters) {
                    drawCar(x, y, angle, type, color, scale = u / 46f, variant = k)
                }
                k++
            }
        }

        // parked cars on the road strip like the slot lane
        for (i in 0 until 3) {
            val x = w * (0.22f + i * 0.28f)
            val y = h * 0.585f
            val a = if (i % 2 == 0) 90f else 270f
            with(Painters) {
                drawCar(x, y, a, CarType.SEDAN, palette[(i * 4 + 1) % palette.size], scale = u / 52f, variant = i + 1)
            }
        }
    }
}
