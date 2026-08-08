package com.codex.carjam.ui

import android.app.Activity
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.codex.carjam.game.CarColor
import com.codex.carjam.game.CarType
import com.codex.carjam.game.DailyRewards
import com.codex.carjam.game.Events
import com.codex.carjam.game.LevelTheme
import com.codex.carjam.game.Prefs
import com.codex.carjam.game.SoundManager
import com.codex.carjam.game.render.Painters
import com.codex.carjam.monetize.AdsManager
import com.codex.carjam.monetize.BillingManager
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
    onPlay: (Int) -> Unit,
) {
    var showLevels by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showShop by remember { mutableStateOf(false) }
    var showDaily by remember { mutableStateOf(false) }
    var showEvents by remember { mutableStateOf(false) }
    var showRank by remember { mutableStateOf(false) }
    val theme = LevelTheme.entries[(prefs.maxLevel.intValue - 1) % LevelTheme.entries.size]
    val event = remember { Events.today() }
    val dailyReady = DailyRewards.canClaim(prefs)

    Box(Modifier.fillMaxSize()) {
        HomeBackdrop(theme)

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                GearButton { sound.tap(); showSettings = true }
                Spacer(Modifier.weight(1f))
                CoinPill(prefs.coins.intValue, onPlus = { sound.tap(); showShop = true })
            }

            SpacerH(34.dp)
            OutlinedTextC("CAR JAM", 64.dp, fill = Color.White, outline = Color(0xFF20303C))
            OutlinedTextC("SOLVER", 30.dp, fill = Color(0xFFFFD32E), outline = Color(0xFF7A4A00))
            SpacerH(8.dp)
            Pill(
                text = "${event.emoji} ${event.title} is LIVE",
                modifier = Modifier.align(Alignment.CenterHorizontally),
                bg = event.accent.copy(alpha = 0.9f),
            )

            SpacerH(44.dp)

            // big round PLAY button
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
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
                ActionChip("🛒", "SHOP") { sound.tap(); showShop = true }
                ActionChip("🎁", "GIFT", badge = dailyReady) { sound.tap(); showDaily = true }
                ActionChip("📅", "EVENTS") { sound.tap(); showEvents = true }
                ActionChip("🏆", "RANK") { sound.tap(); showRank = true }
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

            Spacer(Modifier.weight(1f))

            // AdMob banner (hidden when No-Ads pack owned)
            if (!prefs.removeAds.value) {
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
                    Pill("Tap cars • match colours • clear the jam", bg = Color.Black.copy(alpha = 0.35f))
                    Spacer(Modifier.weight(1f))
                }
            }
        }

        // ---- dialogs
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
                ShopDialog(billing = billing, ads = ads, prefs = prefs, activity = act, onClose = { showShop = false })
            } ?: run { showShop = false }
        }
        if (showDaily) {
            DailyRewardDialog(
                prefs = prefs,
                onClaimed = { sound.coin() },
                onClose = { showDaily = false },
            )
        }
        if (showEvents) {
            EventsDialog(onClose = { showEvents = false })
        }
        if (showRank) {
            LeaderboardDialog(prefs = prefs, onClose = { showRank = false })
        }
    }
}

@Composable
private fun LocalActivity(): Activity? =
    androidx.compose.ui.platform.LocalContext.current as? Activity

@Composable
private fun ActionChip(emoji: String, label: String, badge: Boolean = false, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(60.dp)
                .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                .border(2.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                .clickable { onClick() },
            contentAlignment = Alignment.Center,
        ) {
            BasicText(emoji, style = TextStyle(fontSize = 26.sp))
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
