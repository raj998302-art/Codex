package com.codex.carjam.ui

import android.app.Activity
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.codex.carjam.game.CarColor
import com.codex.carjam.game.CarType
import com.codex.carjam.game.Garage
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
    var showPiggy by remember { mutableStateOf(false) }
    var showWelcome by remember { mutableStateOf(!prefs.welcomed.value) }
    var seasonPrize by remember { mutableStateOf<SeasonPrize?>(null) }
    val theme = run {
        // mirror the generator: only themes already unlocked at this level
        val pool = LevelTheme.entries.filter { prefs.maxLevel.intValue >= it.minLevel }
        pool[((prefs.maxLevel.intValue - 1) % pool.size).coerceAtLeast(0)]
    }
    val event = remember { Events.today() }
    val dailyReady = DailyRewards.canClaim(prefs)
    val activity = LocalActivity()
    val maxLevel = prefs.maxLevel.intValue
    // locked decorative pedestals (tease upcoming toys, exactly like the reference home)
    val leftPedestals = listOf(13, 20, 26, 45)
    val rightPedestals = listOf(4, 11, 13)
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
                .navigationBarsPadding(),
        ) {
            // ---- top HUD: gear + identity chip + compact wallets (never overflows)
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
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
                    AvatarIcon(prefs.avatarId.intValue, 30.dp, frameId = prefs.avatarFrame.intValue)
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

            SpacerH(12.dp)

            // ---- level road: your garage ride driving toward the next unlock
            LevelRoad(
                ride = Garage.selected(prefs),
                level = maxLevel,
                modifier = Modifier.padding(horizontal = 20.dp),
            )

            // ---- pedestal zone: locked teasers on the sides, action in the middle
            Row(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 8.dp),
            ) {
                // left pedestal column
                Column(
                    Modifier.height(430.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    FeaturePedestal(level = leftPedestals[0], current = maxLevel) { locked -> pedestalVs(locked) }
                    SpacerH(10.dp)
                    FeaturePedestal(level = leftPedestals[1], current = maxLevel) { locked -> pedestalStar(locked) }
                    SpacerH(10.dp)
                    FeaturePedestal(level = leftPedestals[2], current = maxLevel) { locked -> pedestalTv(locked) }
                    SpacerH(10.dp)
                    FeaturePedestal(level = leftPedestals[3], current = maxLevel) { locked -> pedestalBurger(locked) }
                }

                // centre: Car-Tour event banner + giant LEVEL button (the reference centrepiece)
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.weight(1.2f))
                    OutlinedTextC("CAR JAM", 34.dp, fill = Color.White, outline = Color(0xFF20303C))
                    OutlinedTextC("SOLVER", 18.dp, fill = Color(0xFFFFD32E), outline = Color(0xFF7A4A00))
                    SpacerH(14.dp)
                    if (online) {
                        Box(Modifier.padding(horizontal = 4.dp)) {
                            EventBannerCard(
                                event = event,
                                height = 80.dp,
                                trailingText = "CAR TOUR — tap for schedule",
                                onClick = { sound.tap(); showEvents = true },
                            )
                            if (event.coinMult > 1f || event.bonusSlots > 0 || event.mysteryBoost > 0) {
                                Box(
                                    Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 7.dp, y = (-7).dp)
                                        .size(20.dp)
                                        .background(Color(0xFFE53935), CircleShape)
                                        .border(2.dp, Color.White, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    BasicText(
                                        "!",
                                        style = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black),
                                    )
                                }
                            }
                        }
                    } else {
                        Pill(
                            text = "OFFLINE MODE — everything still playable",
                            bg = Color(0xFF607D8B).copy(alpha = 0.9f),
                            icon = { GameIcon(GameIconKind.WIFI_OFF, 20.dp) },
                        )
                    }
                    SpacerH(20.dp)
                    SquishyButton(
                        "LEVEL $maxLevel",
                        onClick = { sound.tap(); onPlay(maxLevel) },
                        modifier = Modifier.width(250.dp),
                        top = Color(0xFFFFD93D),
                        bottom = Color(0xFFF0A500),
                        textSize = 26.dp,
                        height = 64.dp,
                    )
                    SpacerH(12.dp)
                    Row {
                        SquishyButton(
                            "LEVELS",
                            onClick = { sound.tap(); showLevels = true },
                            modifier = Modifier.width(118.dp),
                            top = Color(0xFF6FB6FF),
                            bottom = Color(0xFF3B7FE0),
                            textSize = 13.dp,
                            height = 40.dp,
                        )
                        SpacerW(10.dp)
                        SquishyButton(
                            "PRACTICE",
                            onClick = { sound.tap(); onPractice() },
                            modifier = Modifier.width(118.dp),
                            top = Color(0xFFB678E8),
                            bottom = Color(0xFF8A45C4),
                            textSize = 13.dp,
                            height = 40.dp,
                            icon = { GameIcon(GameIconKind.GRAD_CAP, 18.dp) },
                        )
                    }
                    Spacer(Modifier.weight(1f))
                }

                // right pedestal column (piggy bank on top, live!)
                Column(
                    Modifier.height(430.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    FeaturePedestal(
                        level = 1,
                        current = maxLevel,
                        icon = GameIconKind.PIGGY,
                        caption = "${prefs.piggy.intValue}/${Prefs.PIGGY_CAP}",
                        highlight = prefs.piggyFull,
                        onClick = { sound.tap(); showPiggy = true },
                    )
                    SpacerH(10.dp)
                    FeaturePedestal(level = rightPedestals[0], current = maxLevel) { locked -> pedestalWheel(locked) }
                    SpacerH(10.dp)
                    FeaturePedestal(level = rightPedestals[1], current = maxLevel) { locked -> pedestalBag(locked) }
                    SpacerH(10.dp)
                    FeaturePedestal(level = rightPedestals[2], current = maxLevel) { locked -> pedestalChest(locked) }
                }
            }

            // ---- AdMob banner (hidden when No-Ads pack owned or offline)
            if (online && !prefs.removeAds.value) {
                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = { ctx ->
                        AdView(ctx).apply {
                            setAdSize(AdSize.BANNER)
                            adUnitId = AdsManager.BANNER_ID
                            loadAd(AdRequest.Builder().build())
                        }
                    },
                )
            }

            // ---- bottom nav (reference-style 5 tabs)
            val questBadge = DailyMissions.anyClaimable(prefs) || Achievements.anyClaimable(prefs)
            HomeBottomNav(
                dailyReady = dailyReady,
                questBadge = questBadge,
                onDaily = { sound.tap(); showDaily = true },
                onQuests = { sound.tap(); showQuests = true },
                onPlay = { sound.tap(); onPlay(maxLevel) },
                onShop = { sound.tap(); if (!online) onRefreshNet(); showShop = true },
                onRank = { sound.tap(); showRank = true },
            )
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
        if (showPiggy) {
            activity?.let {
                PiggyBankDialog(prefs = prefs, billing = billing, activity = it, onClose = { showPiggy = false })
            } ?: run { showPiggy = false }
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

// ---------------------------------------------------------------- level road

/** The "Unlock at Lv.N" road strip from the reference home, with your garage ride on the left. */
@Composable
private fun LevelRoad(ride: Garage.Ride, level: Int, modifier: Modifier = Modifier) {
    val nextUnlock = (((level / 5) + 1) * 5)
    val frac = (1f - (nextUnlock - level) / 5f).coerceIn(0f, 1f)
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        // ride preview bubble
        Box(
            Modifier
                .size(46.dp)
                .background(Color.White.copy(alpha = 0.92f), CircleShape)
                .border(3.dp, Color(0xFF7A4A12).copy(alpha = 0.5f), CircleShape),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val u = size.minDimension
                with(Painters) {
                    drawCar(
                        u * 0.5f,
                        u * 0.58f,
                        0f,
                        ride.type,
                        CarColor.RED,
                        scale = u / 210f,
                        arrowVisible = false,
                        variant = ride.variant,
                    )
                }
            }
        }
        SpacerW(8.dp)
        // road bar with progress fill + caption
        Box(Modifier.weight(1f).height(34.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                drawRoundRect(
                    Color(0xFF3C4654),
                    Offset.Zero,
                    Size(w, h),
                    CornerRadius(h / 2f),
                )
                drawRoundRect(
                    Color(0xFF2A3038),
                    Offset(0f, h - 5f),
                    Size(w, 5f),
                    CornerRadius(h / 4f),
                )
                if (frac > 0.01f) {
                    drawRoundRect(
                        Brush.horizontalGradient(listOf(Color(0xFFFFC93C), Color(0xFFF0A500))),
                        Offset(3f, 3f),
                        Size((w - 6f) * frac, h - 6f),
                        CornerRadius((h - 6f) / 2f),
                    )
                }
            }
            BasicText(
                "Unlock at Lv.$nextUnlock",
                style = TextStyle(color = Color(0xFFFFE9B0), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp),
                modifier = Modifier.align(Alignment.Center),
            )
        }
        SpacerW(8.dp)
        Box(
            Modifier
                .size(30.dp)
                .background(Color(0xFFB9C1C8), CircleShape)
                .border(2.dp, Color(0xFF8D959C), CircleShape),
            contentAlignment = Alignment.Center,
        ) { GameIcon(GameIconKind.LOCK, 16.dp) }
    }
}

// ---------------------------------------------------------------- pedestals

private val PedestalTop = Color(0xFFF2C891)
private val PedestalMid = Color(0xFFD99F66)
private val PedestalBase = Color(0xFFB97F49)
private val PedestalInk = Color(0xFF8A5A2B)

/**
 * Reference-home feature pedestal: stepped stone disc with a toy on top.
 * Locked toys render in plain gold; unlocked ones go full colour and tap through.
 */
@Composable
private fun FeaturePedestal(
    level: Int,
    current: Int,
    icon: GameIconKind? = null,
    caption: String? = null,
    highlight: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: (DrawScope.(locked: Boolean) -> Unit)? = null,
) {
    val locked = current < level
    Column(
        Modifier
            .width(64.dp)
            .clickable(enabled = !locked && onClick != null) { onClick?.invoke() },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(58.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                val u = size.minDimension / 58f
                // stepped stone pedestal
                drawRoundRect(
                    PedestalBase,
                    Offset(cx - 22f * u, 46f * u),
                    Size(44f * u, 10f * u),
                    CornerRadius(5f * u),
                )
                drawRoundRect(
                    PedestalMid,
                    Offset(cx - 18f * u, 38f * u),
                    Size(36f * u, 10f * u),
                    CornerRadius(4f * u),
                )
                drawOval(
                    PedestalTop,
                    Offset(cx - 15f * u, 32f * u),
                    Size(30f * u, 9f * u),
                )
                drawOval(
                    Color.White.copy(alpha = 0.35f),
                    Offset(cx - 11f * u, 33f * u),
                    Size(22f * u, 5f * u),
                )
                // toy content (painters draw gold when locked, colour when free)
                if (content != null && icon == null) {
                    withTransform({ translate(cx - 18f * u, -2f * u); scale(u * 1.5f, u * 1.5f, Offset.Zero) }) {
                        content(locked)
                    }
                }
            }
            if (icon != null) {
                GameIcon(
                    icon,
                    30.dp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = 2.dp),
                )
            }
            if (locked) {
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = (-12).dp)
                        .size(18.dp)
                        .background(Color(0xFF8D959C), CircleShape)
                        .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) { GameIcon(GameIconKind.LOCK, 10.dp) }
            }
            if (highlight) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 3.dp, y = (-1).dp)
                        .size(16.dp)
                        .background(Color(0xFFE53935), CircleShape)
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText("!", style = TextStyle(color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black))
                }
            }
        }
        SpacerH(2.dp)
        if (!locked && caption != null) {
            BasicText(
                caption,
                style = TextStyle(
                    color = Color(0xFF2FA84F),
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                ),
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(7.dp))
                    .border(1.5.dp, Color(0xFF9BD8A5), RoundedCornerShape(7.dp))
                    .padding(horizontal = 5.dp, vertical = 1.dp),
            )
        } else {
            BasicText(
                "Unlock at Lv.$level",
                style = TextStyle(
                    color = PedestalInk,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                ),
                maxLines = 2,
            )
        }
    }
}

// Pedestal toy painters (DrawScope in a ~24x24 box, gold-tinted when locked)

private fun DrawScope.withGold(body: DrawScope.() -> Unit) = body()

private fun DrawScope.pedestalVs(locked: Boolean) {
    val disc = if (locked) Color(0xFFE8B23C) else Color(0xFFBF6CF2)
    val rim = if (locked) Color(0xFFB8860B) else Color(0xFF8A45C4)
    drawCircle(rim, radius = 12f, center = Offset(12f, 13f))
    drawCircle(disc, radius = 10f, center = Offset(12f, 12f))
    // "VS" fork: two white strokes + bar
    drawLine(Color.White, Offset(7f, 8f), Offset(12f, 15f), strokeWidth = 2.6f)
    drawLine(Color.White, Offset(17f, 8f), Offset(12f, 15f), strokeWidth = 2.6f)
    drawLine(Color.White, Offset(12f, 15f), Offset(12f, 19f), strokeWidth = 2.6f)
}

private fun DrawScope.pedestalStar(locked: Boolean) {
    val wing = if (locked) Color(0xFFE8B23C) else Color(0xFF9FC3E8)
    val starC = if (locked) Color(0xFFFFF0A0) else Color(0xFFFFD32E)
    // crossed racing flags / wings behind the star
    drawLine(wing, Offset(1f, 6f), Offset(9f, 14f), strokeWidth = 3f)
    drawLine(wing, Offset(23f, 6f), Offset(15f, 14f), strokeWidth = 3f)
    val path = Path()
    val cx = 12f
    val cy = 13f
    for (i in 0 until 10) {
        val rad = if (i % 2 == 0) 8.5f else 3.6f
        val a = Math.toRadians((i * 36.0) - 90.0)
        val x = cx + (kotlin.math.cos(a) * rad).toFloat()
        val y = cy + (kotlin.math.sin(a) * rad).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, starC)
    drawPath(path, Color(0xFFB8860B), style = Stroke(1.2f))
}

private fun DrawScope.pedestalTv(locked: Boolean) {
    val body = if (locked) Color(0xFFE8B23C) else Color(0xFF35C4B5)
    val screen = if (locked) Color(0xFFFFF0C2) else Color(0xFFEAF6FF)
    // antenna
    drawLine(Color(0xFF6B5340), Offset(9f, 5f), Offset(6f, 1f), strokeWidth = 1.6f)
    drawLine(Color(0xFF6B5340), Offset(15f, 5f), Offset(18f, 1f), strokeWidth = 1.6f)
    // tv body
    drawRoundRect(body, Offset(3f, 5f), Size(18f, 13f), CornerRadius(3f))
    drawRoundRect(Color(0xFF1B5E58), Offset(5f, 7f), Size(14f, 9f), CornerRadius(2f))
    drawRoundRect(screen, Offset(6f, 8f), Size(12f, 7f), CornerRadius(1.5f))
    // pup face in the screen
    drawCircle(Color(0xFFD9A066), radius = 2.6f, center = Offset(12f, 11.5f))
    drawCircle(Color(0xFF3A2A18), radius = 0.5f, center = Offset(11f, 10.8f))
    drawCircle(Color(0xFF3A2A18), radius = 0.5f, center = Offset(13f, 10.8f))
    drawCircle(Color(0xFF5A3B1E), radius = 0.6f, center = Offset(12f, 12.2f))
}

private fun DrawScope.pedestalWheel(locked: Boolean) {
    drawCircle(Color(0xFFB8860B), radius = 11.5f, center = Offset(12f, 13f))
    if (locked) {
        drawCircle(Color(0xFFE8B23C), radius = 9.5f, center = Offset(12f, 13f))
    } else {
        val cols = listOf(Color(0xFFFF4757), Color(0xFFFFD32E), Color(0xFF2ED573), Color(0xFF3B9BFF))
        for (i in 0..3) {
            drawArc(cols[i], i * 90f, 90f, true, Offset(2.5f, 3.5f), Size(19f, 19f))
        }
    }
    drawCircle(if (locked) Color(0xFFFFF0C2) else Color.White, radius = 3.2f, center = Offset(12f, 13f))
    // pointer at top
    drawLine(Color(0xFF6B5340), Offset(12f, 1f), Offset(12f, 5f), strokeWidth = 2f)
}

private fun DrawScope.pedestalBag(locked: Boolean) {
    val body = if (locked) Color(0xFFE8B23C) else Color(0xFFE8734A)
    val strap = if (locked) Color(0xFFB8860B) else Color(0xFF8A3A2B)
    drawArc(strap, 180f, 180f, false, Offset(6f, 2f), Size(12f, 10f), style = Stroke(2.2f))
    drawRoundRect(body, Offset(4f, 8f), Size(16f, 12f), CornerRadius(3f))
    drawRoundRect(
        if (locked) Color(0xFFFFF0C2) else Color(0xFFFFB020),
        Offset(4f, 8f),
        Size(16f, 4f),
        CornerRadius(2f),
    )
    drawCircle(strap, radius = 1.4f, center = Offset(12f, 14f))
}

private fun DrawScope.pedestalChest(locked: Boolean) {
    val wood = if (locked) Color(0xFFE8B23C) else Color(0xFFA9713D)
    val trim = if (locked) Color(0xFFB8860B) else Color(0xFFFFD32E)
    drawRoundRect(wood, Offset(3f, 8f), Size(18f, 11f), CornerRadius(2.5f))
    drawArc(wood, 180f, 180f, true, Offset(3f, 2f), Size(18f, 10f))
    drawRoundRect(trim, Offset(10.5f, 4f), Size(3f, 15f), CornerRadius(1f))
    drawCircle(trim, radius = 1.8f, center = Offset(12f, 12f))
    drawCircle(Color(0xFF5A3B1E), radius = 0.9f, center = Offset(12f, 12f))
}

private fun DrawScope.pedestalBurger(locked: Boolean) {
    val bun = if (locked) Color(0xFFE8B23C) else Color(0xFFF0A852)
    // top bun
    drawArc(bun, 180f, 180f, true, Offset(4f, 4f), Size(16f, 12f))
    if (!locked) {
        // sesame + lettuce + patty
        drawCircle(Color(0xFFFFF0C2), radius = 0.7f, center = Offset(9f, 7f))
        drawCircle(Color(0xFFFFF0C2), radius = 0.7f, center = Offset(14f, 6f))
        drawRoundRect(Color(0xFF7CB24C), Offset(4f, 12.6f), Size(16f, 2.4f), CornerRadius(1.2f))
        drawRoundRect(Color(0xFF8A4A2B), Offset(5f, 14.6f), Size(14f, 2.6f), CornerRadius(1.2f))
    } else {
        drawCircle(Color(0xFFFFF0C2), radius = 0.7f, center = Offset(11f, 7f))
        drawRoundRect(Color(0xFFD9A066), Offset(4f, 12.6f), Size(16f, 4.6f), CornerRadius(1.2f))
    }
    // bottom bun
    drawRoundRect(bun, Offset(4f, 17.4f), Size(16f, 3f), CornerRadius(1.5f))
}

// ---------------------------------------------------------------- bottom nav

/** Reference-style 5-tab bottom navigation bar with a raised PLAY centre cap. */
@Composable
private fun HomeBottomNav(
    dailyReady: Boolean,
    questBadge: Boolean,
    onDaily: () -> Unit,
    onQuests: () -> Unit,
    onPlay: () -> Unit,
    onShop: () -> Unit,
    onRank: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF2E93EE), Color(0xFF1565C0))),
                RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
            )
            .border(2.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .padding(top = 8.dp, bottom = 8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            NavTab(GameIconKind.GIFT, "GIFT", dailyReady, onDaily, Modifier.weight(1f))
            NavTab(GameIconKind.BOLT, "QUESTS", questBadge, onQuests, Modifier.weight(1f))
            // raised centre play cap (house + play triangle, like the reference Home cap)
            Column(
                Modifier
                    .weight(1.2f)
                    .offset(y = (-16).dp)
                    .clickable { onPlay() },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .size(58.dp)
                        .background(Color.White, CircleShape)
                        .border(4.dp, Color(0xFFE8F4FF), CircleShape),
                ) {
                    Canvas(Modifier.fillMaxSize().padding(10.dp)) {
                        val w = size.width
                        val h = size.height
                        // red roof
                        val roof = Path().apply {
                            moveTo(w * 0.08f, h * 0.52f)
                            lineTo(w * 0.50f, h * 0.08f)
                            lineTo(w * 0.92f, h * 0.52f)
                            close()
                        }
                        drawPath(roof, Color(0xFFE53935))
                        // body
                        drawRoundRect(
                            Color(0xFFFFE9B0),
                            Offset(w * 0.22f, h * 0.46f),
                            Size(w * 0.56f, h * 0.44f),
                            CornerRadius(w * 0.06f),
                        )
                        // green play triangle in the door hole
                        val play = Path().apply {
                            moveTo(w * 0.45f, h * 0.56f)
                            lineTo(w * 0.45f, h * 0.86f)
                            lineTo(w * 0.68f, h * 0.71f)
                            close()
                        }
                        drawPath(play, Color(0xFF1FA94F))
                    }
                }
                BasicText(
                    "PLAY",
                    style = TextStyle(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp),
                )
            }
            NavTab(GameIconKind.CART, "SHOP", false, onShop, Modifier.weight(1f))
            NavTab(GameIconKind.TROPHY, "RANK", false, onRank, Modifier.weight(1f))
        }
    }
}

@Composable
private fun NavTab(
    kind: GameIconKind,
    label: String,
    badge: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            GameIcon(kind, 30.dp)
            if (badge) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 7.dp, y = (-4).dp)
                        .size(12.dp)
                        .background(Color(0xFFE53935), CircleShape)
                        .border(2.dp, Color.White, CircleShape),
                )
            }
        }
        BasicText(
            label,
            style = TextStyle(color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp),
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
