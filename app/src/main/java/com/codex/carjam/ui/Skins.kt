package com.codex.carjam.ui

import android.app.Activity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codex.carjam.game.AvatarFrames
import com.codex.carjam.game.CarColor
import com.codex.carjam.game.CarType
import com.codex.carjam.game.CloudSave
import com.codex.carjam.game.Deco
import com.codex.carjam.game.Events
import com.codex.carjam.game.Garage
import com.codex.carjam.game.LeaderboardApi
import com.codex.carjam.game.LevelTheme
import com.codex.carjam.game.LiveBoard
import com.codex.carjam.game.Prefs
import com.codex.carjam.game.Referral
import com.codex.carjam.game.RideCollection
import com.codex.carjam.game.SkinShopCatalog
import com.codex.carjam.game.SkinSource
import com.codex.carjam.game.SkinTab
import com.codex.carjam.game.SkinTile
import com.codex.carjam.game.VehicleSkins
import com.codex.carjam.game.render.GameIconKind
import com.codex.carjam.game.render.Painters
import com.codex.carjam.monetize.AdsManager
import kotlinx.coroutines.delay
import java.util.Calendar
import kotlin.math.min

private val Sky = Color(0xFF1E5FD8)
private val SkyDark = Color(0xFF0E2E78)
private val InkS = Color(0xFF4A3826)
private val SubS = Color(0xFF8C6A3F)
private val Cream = Color(0xFFFFF3DC)
private val CreamDark = Color(0xFFF4E3C2)

// ================================================================ scaffold

/** Full-screen page scaffold matching the reference banner UI: deep-blue sky,
 *  title plate, content, and the bottom banner nav. */
@Composable
fun ScreenScaffold(
    title: String,
    current: BannerTab,
    onNav: (BannerTab) -> Unit,
    coinSlot: (@Composable () -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Sky, SkyDark)))) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            // title bar
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 6.dp),
            ) {
                OutlinedTextC(
                    text = title,
                    size = 26.dp,
                    fill = Color.White,
                    outline = Color(0xFF123A6E),
                    modifier = Modifier.align(Alignment.Center),
                )
                if (coinSlot != null) {
                    Box(Modifier.align(Alignment.CenterEnd).padding(end = 14.dp)) { coinSlot() }
                }
                if (onClose != null) {
                    Box(
                        Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 14.dp)
                            .size(34.dp)
                            .background(Color(0xFFE53935), CircleShape)
                            .border(2.5.dp, Color.White, CircleShape)
                            .clickable { onClose() },
                        contentAlignment = Alignment.Center,
                    ) {
                        BasicText("X", style = TextStyle(color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black))
                    }
                }
            }
            Column(Modifier.fillMaxWidth().weight(1f)) { content() }
            BottomBannerNav(current = current, onPick = onNav)
        }
    }
}

enum class BannerTab { HOME, COLLECTION, LEADERBOARD, SKIN, PROFILE }

/** The reference bottom banner: blue bar with vector tab icons. */
@Composable
fun BottomBannerNav(current: BannerTab, onPick: (BannerTab) -> Unit) {
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
            BannerItem(BannerTab.HOME, "HOME", current, onPick, Modifier.weight(1f)) { c, s -> navHouse(c, s) }
            BannerItem(BannerTab.COLLECTION, "CARDS", current, onPick, Modifier.weight(1f)) { c, s -> navCards(c, s) }
            BannerItem(BannerTab.SKIN, "SKIN", current, onPick, Modifier.weight(1f)) { c, s -> navCar(c, s) }
            BannerItem(BannerTab.LEADERBOARD, "RANK", current, onPick, Modifier.weight(1f)) { c, s -> navCup(c, s) }
        }
    }
}

@Composable
private fun BannerItem(
    tab: BannerTab,
    label: String,
    current: BannerTab,
    onPick: (BannerTab) -> Unit,
    modifier: Modifier = Modifier,
    painter: DrawScope.(Color, Float) -> Unit,
) {
    val active = tab == current
    Column(
        modifier
            .padding(vertical = 2.dp)
            .background(
                if (active) Color.White.copy(alpha = 0.16f) else Color.Transparent,
                RoundedCornerShape(12.dp),
            )
            .clickable { onPick(tab) }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(Modifier.size(30.dp)) {
            painter(this, if (active) Color(0xFFFFD93D) else Color.White, size.minDimension)
        }
        BasicText(
            label,
            style = TextStyle(
                color = if (active) Color(0xFFFFD93D) else Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
            ),
        )
    }
}

// tiny nav painters (zero emoji, all vector)
private fun DrawScope.navHouse(color: Color, s: Float) {
    val roof = Path().apply {
        moveTo(s * 0.08f, s * 0.50f)
        lineTo(s * 0.50f, s * 0.10f)
        lineTo(s * 0.92f, s * 0.50f)
        close()
    }
    drawPath(roof, Color(0xFFE53935))
    drawRoundRect(color, Offset(s * 0.22f, s * 0.44f), Size(s * 0.56f, s * 0.44f), CornerRadius(s * 0.06f))
    drawRect(Color(0xFF1565C0), Offset(s * 0.42f, s * 0.62f), Size(s * 0.16f, s * 0.26f))
}

private fun DrawScope.navCards(color: Color, s: Float) {
    drawRoundRect(Color(0xFF9A6A3A), Offset(s * 0.14f, s * 0.20f), Size(s * 0.58f, s * 0.68f), CornerRadius(s * 0.08f))
    drawRoundRect(color, Offset(s * 0.26f, s * 0.12f), Size(s * 0.58f, s * 0.68f), CornerRadius(s * 0.08f))
    drawRoundRect(Color(0xFF1565C0), Offset(s * 0.34f, s * 0.30f), Size(s * 0.42f, s * 0.32f), CornerRadius(s * 0.05f))
}

private fun DrawScope.navCar(color: Color, s: Float) {
    drawRoundRect(color, Offset(s * 0.10f, s * 0.38f), Size(s * 0.80f, s * 0.34f), CornerRadius(s * 0.10f))
    drawRoundRect(color, Offset(s * 0.28f, s * 0.22f), Size(s * 0.44f, s * 0.22f), CornerRadius(s * 0.08f))
    drawRect(Color(0xFF1565C0), Offset(s * 0.32f, s * 0.26f), Size(s * 0.14f, s * 0.14f))
    drawRect(Color(0xFF1565C0), Offset(s * 0.54f, s * 0.26f), Size(s * 0.14f, s * 0.14f))
    drawCircle(Color(0xFF23262E), radius = s * 0.10f, center = Offset(s * 0.28f, s * 0.72f))
    drawCircle(Color(0xFF23262E), radius = s * 0.10f, center = Offset(s * 0.72f, s * 0.72f))
    drawCircle(Color(0xFF8D959C), radius = s * 0.05f, center = Offset(s * 0.28f, s * 0.72f))
    drawCircle(Color(0xFF8D959C), radius = s * 0.05f, center = Offset(s * 0.72f, s * 0.72f))
}

private fun DrawScope.navCup(color: Color, s: Float) {
    drawRoundRect(color, Offset(s * 0.26f, s * 0.14f), Size(s * 0.48f, s * 0.40f), CornerRadius(s * 0.10f))
    drawCircle(Color.Transparent, radius = s * 0.12f, center = Offset(s * 0.16f, s * 0.30f), style = Stroke(s * 0.06f))
    drawCircle(Color.Transparent, radius = s * 0.12f, center = Offset(s * 0.84f, s * 0.30f), style = Stroke(s * 0.06f))
    drawRect(color, Offset(s * 0.44f, s * 0.54f), Size(s * 0.12f, s * 0.20f))
    drawRoundRect(color, Offset(s * 0.28f, s * 0.74f), Size(s * 0.44f, s * 0.10f), CornerRadius(s * 0.05f))
    drawCircle(Color(0xFF1565C0), radius = s * 0.09f, center = Offset(s * 0.50f, s * 0.32f))
}

// ================================================================ collection

@Composable
fun CollectionScreen(prefs: Prefs, onNav: (BannerTab) -> Unit) {
    var tab by remember { mutableStateOf(SkinTab.VEHICLE) } // VEHICLE="CARS wall", SCENE="CARDS wall"
    ScreenScaffold(
        title = "Collection",
        current = BannerTab.COLLECTION,
        onNav = onNav,
        coinSlot = { CoinPill(prefs.coins.intValue, compact = true) },
    ) {
        // tab strip like the reference (CARDS locked tab | CARS active tab)
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .background(Color(0xFF0A2C6E), RoundedCornerShape(16.dp))
                .padding(4.dp),
        ) {
            TabButton("CARDS", selected = tab == SkinTab.SCENE, modifier = Modifier.weight(1f)) {
                tab = SkinTab.SCENE
            }
            TabButton("CARS", selected = tab == SkinTab.VEHICLE, modifier = Modifier.weight(1f)) {
                tab = SkinTab.VEHICLE
            }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp),
        ) {
            if (tab == SkinTab.VEHICLE) {
                // ---- CARS wall: the 70-card book, silhouettes until earned
                val cards = RideCollection.ALL
                val rows = cards.chunked(3)
                for (row in rows) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (card in row) {
                            CarWallCell(card.no, card.title, card.type, card.color, card.variant, card.skin, prefs.maxLevel.intValue, Modifier.weight(1f))
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                    SpacerH(8.dp)
                }
            } else {
                // ---- CARDS wall: scene stickers unlocked by level milestones
                val dekos = Deco.entries
                repeat(7) { band ->
                    SpacerH(6.dp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (c in 0 until 3) {
                            val no = band * 3 + c + 1
                            val need = no * 4
                            val deco = dekos[(no - 1) % dekos.size]
                            CardWallCell(no, need, deco, prefs.maxLevel.intValue, Modifier.weight(1f))
                        }
                    }
                    SpacerH(8.dp)
                }
            }
            SpacerH(6.dp)
            // footer progress (mirrors "Collection Progress: 0/70")
            val found = RideCollection.unlockedCount(prefs.maxLevel.intValue)
            BasicText(
                if (tab == SkinTab.VEHICLE) "Collection Progress: $found/${RideCollection.TOTAL}" else "Cards Progress: ${prefs.maxLevel.intValue / 4}/21",
                style = TextStyle(color = Color(0xFFFFE9B0), fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center),
                modifier = Modifier.fillMaxWidth(),
            )
            SpacerH(10.dp)
        }
    }
}

@Composable
private fun TabButton(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .background(
                if (selected) Brush.verticalGradient(listOf(Color(0xFFFFD93D), Color(0xFFF0A500))) else Brush.verticalGradient(listOf(Color(0xFF2A55A8), Color(0xFF1B3E86))),
                RoundedCornerShape(12.dp),
            )
            .border(2.dp, if (selected) Color.White.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            label,
            style = TextStyle(
                color = if (selected) Color(0xFF7A4A00) else Color(0xFF9DC0FF),
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
            ),
        )
    }
}

/** One cell of the CARS wall — parchment tile, No.N chip, ride or beige silhouette. */
@Composable
private fun CarWallCell(
    no: Int,
    title: String,
    type: CarType,
    color: CarColor,
    variant: Int,
    skin: Int,
    level: Int,
    modifier: Modifier = Modifier,
) {
    val unlocked = level >= CollectionUnlockLevel(no)
    Column(
        modifier
            .background(Cream, RoundedCornerShape(14.dp))
            .border(2.dp, CreamDark, RoundedCornerShape(14.dp))
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .background(Color(0xFFE9B23C), RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 1.dp),
            ) {
                BasicText("No.$no", style = TextStyle(color = Color(0xFF7A4A00), fontSize = 9.5.sp, fontWeight = FontWeight.Black))
            }
            if (!unlocked) {
                Spacer(Modifier.weight(1f))
                GameIcon(GameIconKind.LOCK, 11.dp)
            }
        }
        SpacerH(4.dp)
        Box(Modifier.fillMaxWidth().height(56.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val sc = min(size.width / (type.len + 90f), size.height / (type.wid + 60f))
                with(Painters) {
                    if (unlocked) {
                        drawCar(size.width / 2f, size.height / 2f + 6f, 0f, type, color, scale = sc, arrowVisible = false, variant = variant)
                        if (skin > 0) drawCarSkinOverlay(skin, size.width / 2f, size.height / 2f + 6f, 0f, type, sc)
                    } else {
                        drawCar(size.width / 2f, size.height / 2f + 6f, 0f, type, CarColor.GRAY, scale = sc, alpha = 0.45f, arrowVisible = false)
                        drawRect(Color(0xFFD9C6A5).copy(alpha = 0.55f))
                    }
                }
            }
            if (!unlocked) {
                BasicText("???", style = TextStyle(color = Color(0xFF8A6F4A), fontSize = 14.sp, fontWeight = FontWeight.Black))
            }
        }
        SpacerH(3.dp)
        BasicText(
            if (unlocked) title else "????",
            style = TextStyle(color = if (unlocked) InkS else Color(0xFF9A8868), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (!unlocked) {
            BasicText("LVL ${CollectionUnlockLevel(no)}", style = TextStyle(color = Color(0xFFB09A76), fontSize = 8.sp, fontWeight = FontWeight.Bold))
        }
    }
}

/** Mirrors RideCollection's per-card unlock ladder without dragging the list in here. */
private fun CollectionUnlockLevel(no: Int): Int =
    RideCollection.ALL.firstOrNull { it.no == no }?.unlockLevel ?: Int.MAX_VALUE

/** CARD sticker cell: mini themed scene chip, locked until level milestone. */
@Composable
private fun CardWallCell(no: Int, need: Int, deco: Deco, level: Int, modifier: Modifier = Modifier) {
    val unlocked = level >= need
    Column(
        modifier
            .background(Cream, RoundedCornerShape(14.dp))
            .border(2.dp, CreamDark, RoundedCornerShape(14.dp))
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .background(Color(0xFF7EC8FF), RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 1.dp),
            ) {
                BasicText("No.$no", style = TextStyle(color = Color(0xFF123A6E), fontSize = 9.5.sp, fontWeight = FontWeight.Black))
            }
        }
        SpacerH(4.dp)
        Box(Modifier.fillMaxWidth().height(52.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                if (unlocked) {
                    val cols = when (deco) {
                        Deco.SEA -> listOf(Color(0xFF7FD8E6), Color(0xFF3FA6C8))
                        Deco.ZOO, Deco.JUNGLE -> listOf(Color(0xFFA8D479), Color(0xFF7CB24C))
                        Deco.WINTER, Deco.FROZEN -> listOf(Color(0xFFBFE6FF), Color(0xFF6FA8DC))
                        Deco.LAVA -> listOf(Color(0xFFFF9F2E), Color(0xFFD63030))
                        Deco.NIGHT -> listOf(Color(0xFF5C6BC0), Color(0xFF131B3A))
                        Deco.DESERT -> listOf(Color(0xFFFFD93D), Color(0xFFE0A050))
                        Deco.BEACH -> listOf(Color(0xFF8FD8F0), Color(0xFF3FA6C8))
                        else -> listOf(Color(0xFF9FC3E8), Color(0xFF6FB6FF))
                    }
                    drawRoundRect(Brush.verticalGradient(cols), Offset(6f, 6f), Size(size.width - 12f, size.height - 12f), CornerRadius(10f))
                    drawRect(cols[1], Offset(6f, size.height * 0.62f), Size(size.width - 12f, size.height * 0.20f))
                    drawCircle(Color(0xFFFFF3B8), radius = 8f, center = Offset(size.width * 0.78f, size.height * 0.30f))
                } else {
                    drawRoundRect(Color(0xFFB9A98C), Offset(6f, 6f), Size(size.width - 12f, size.height - 12f), CornerRadius(10f))
                }
            }
            if (!unlocked) BasicText("???", style = TextStyle(color = Color(0xFF7A6B52), fontSize = 13.sp, fontWeight = FontWeight.Black))
        }
        SpacerH(2.dp)
        BasicText(
            if (unlocked) deco.name else "????",
            style = TextStyle(color = if (unlocked) InkS else Color(0xFF9A8868), fontSize = 8.5.sp, fontWeight = FontWeight.ExtraBold),
        )
    }
}

// ================================================================ leaderboard

@Composable
fun LeaderboardScreen(prefs: Prefs, onNav: (BannerTab) -> Unit, onOpenProfile: () -> Unit) {
    var leagueTab by remember { mutableStateOf(false) }
    val myId = prefs.deviceId
    LaunchedEffect(Unit) {
        LiveBoard.sync(prefs, force = true)
        while (true) {
            delay(20_000)
            LiveBoard.sync(prefs)
        }
    }
    val entries = LiveBoard.entries.value
    val me = LiveBoard.me.value
    ScreenScaffold(
        title = "Leaderboard",
        current = BannerTab.LEADERBOARD,
        onNav = onNav,
        coinSlot = { CoinPill(prefs.coins.intValue, compact = true) },
    ) {
        // WEEKLY WINNERS | LEAGUE tabs + the season countdown chip
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .background(Color(0xFF0A2C6E), RoundedCornerShape(16.dp))
                .padding(4.dp),
        ) {
            TabButton("WEEKLY WINNERS", selected = !leagueTab, modifier = Modifier.weight(1f)) { leagueTab = false }
            TabButton("LEAGUE", selected = leagueTab, modifier = Modifier.weight(1f)) { leagueTab = true }
        }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Row(
                Modifier
                    .padding(bottom = 6.dp)
                    .background(Color(0xFF0A2C6E).copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                    .border(1.5.dp, Color(0xFF9DC0FF).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GameIcon(GameIconKind.CALENDAR, 14.dp)
                SpacerW(6.dp)
                BasicText(weekCountdown(), style = TextStyle(color = Color(0xFFFFD93D), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold))
                SpacerW(8.dp)
                BasicText("PROMOTION PRIZE x10", style = TextStyle(color = Color(0xFF9DC0FF), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold))
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp),
        ) {
            if (!leagueTab) {
                // ---- weekly podium: #1 high block centre with crowns + winner chests
                if (entries.size >= 3) {
                    WeeklyPodium(entries[0], entries[1], entries[2], myId, onOpenProfile)
                    SpacerH(10.dp)
                }
                if (entries.isEmpty()) {
                    EmptyBoardNote()
                } else {
                    for (e in entries.drop(3)) {
                        BoardRow("#${e.rank}", e.name, e.avatarId, "Levels:${e.maxLevel}", e.deviceId == myId)
                        SpacerH(6.dp)
                    }
                }
            } else {
                // ---- league: trophy ladder with promotion chip
                LeagueBanner()
                SpacerH(8.dp)
                if (entries.isEmpty()) {
                    EmptyBoardNote()
                } else {
                    for (e in entries) {
                        BoardRow(
                            if (e.rank <= 3) "#${e.rank}" else "${e.rank}",
                            e.name,
                            e.avatarId,
                            "Trophy ${e.rating}",
                            e.deviceId == myId,
                        )
                        SpacerH(6.dp)
                    }
                }
            }
            // YOU pinned at the bottom (rank from the live board)
            SpacerH(4.dp)
            BoardRow(
                me?.let { "#${it.rank}" } ?: "-",
                "YOU",
                prefs.avatarId.intValue,
                "Levels:${prefs.maxLevel.intValue}",
                true,
            )
            SpacerH(12.dp)
        }
    }
}

private fun weekCountdown(): String {
    val now = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
    val next = now.clone() as Calendar
    next.set(Calendar.HOUR_OF_DAY, 0)
    next.set(Calendar.MINUTE, 0)
    next.set(Calendar.SECOND, 0)
    next.set(Calendar.MILLISECOND, 0)
    next.add(Calendar.DAY_OF_YEAR, (8 - next.get(Calendar.DAY_OF_WEEK)) % 7 + 1) // next Monday
    val diffMin = ((next.timeInMillis - now.timeInMillis) / 60000L).coerceAtLeast(0)
    val days = diffMin / (60 * 24)
    val hours = (diffMin / 60) % 24
    val mins = diffMin % 60
    return if (days > 0) "${days}d ${hours}h left" else "${hours}h ${mins}m left"
}

@Composable
private fun WeeklyPodium(
    first: LeaderboardApi.Entry,
    second: LeaderboardApi.Entry,
    third: LeaderboardApi.Entry,
    myId: String,
    onOpenProfile: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF0A2C6E).copy(alpha = 0.5f), RoundedCornerShape(18.dp))
            .border(2.dp, Color(0xFF9DC0FF).copy(alpha = 0.4f), RoundedCornerShape(18.dp))
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom,
    ) {
        PodiumBlock(second, Color(0xFF9AD5FF), GameIconKind.MEDAL_2, 40.dp, myId, onOpenProfile)
        PodiumBlock(first, Color(0xFFFFD93D), GameIconKind.MEDAL_1, 56.dp, myId, onOpenProfile)
        PodiumBlock(third, Color(0xFFE0A26A), GameIconKind.MEDAL_3, 40.dp, myId, onOpenProfile)
    }
}

@Composable
private fun PodiumBlock(
    e: LeaderboardApi.Entry,
    pedestal: Color,
    medal: GameIconKind,
    size: androidx.compose.ui.unit.Dp,
    myId: String,
    onOpenProfile: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onOpenProfile() },
    ) {
        // crown above the medal
        Canvas(Modifier.size(22.dp)) {
            val path = Path().apply {
                moveTo(2f, size.height * 0.78f)
                lineTo(2f, size.height * 0.30f)
                lineTo(size.width * 0.32f, size.height * 0.52f)
                lineTo(size.width * 0.5f, size.height * 0.18f)
                lineTo(size.width * 0.68f, size.height * 0.52f)
                lineTo(size.width - 2f, size.height * 0.30f)
                lineTo(size.width - 2f, size.height * 0.78f)
                close()
            }
            drawPath(path, Color(0xFFFFC93C))
            drawPath(path, Color(0xFFB8860B), style = Stroke(1.6f))
        }
        GameIcon(medal, 24.dp)
        SpacerH(3.dp)
        Box(
            Modifier
                .background(Color.White, CircleShape)
                .border(3.dp, pedestal, CircleShape)
                .padding(3.dp),
        ) { AvatarIcon(e.avatarId, size) }
        SpacerH(3.dp)
        BasicText(
            if (e.deviceId == myId) "YOU" else e.name,
            style = TextStyle(color = if (e.deviceId == myId) Color(0xFFFFD93D) else Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        // winner chest pedestal
        Box(
            Modifier
                .width(size + 14.dp)
                .background(
                    Brush.verticalGradient(listOf(pedestal, pedestal.copy(alpha = 0.7f))),
                    RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
                )
                .border(2.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                .padding(horizontal = 6.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Canvas(Modifier.size(18.dp)) {
                    drawRoundRect(Color(0xFF8A5A2B), Offset(2f, 6f), Size(size.width - 4f, size.height - 8f), CornerRadius(3f))
                    drawArc(Color(0xFFA9713D), 180f, 180f, true, Offset(2f, 1f), Size(size.width - 4f, 10f))
                    drawCircle(Color(0xFFFFD32E), radius = 2.4f, center = Offset(size.width / 2f, size.height * 0.56f))
                }
                BasicText("Levels:${e.maxLevel}", style = TextStyle(color = Color(0xFF3A2A18), fontSize = 8.5.sp, fontWeight = FontWeight.Black))
            }
        }
    }
}

@Composable
private fun LeagueBanner() {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color(0xFFB26A00), Color(0xFF8A4E00))), RoundedCornerShape(16.dp))
            .border(2.dp, Color(0xFFFFD93D).copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GameIcon(GameIconKind.TROPHY, 44.dp)
        SpacerW(10.dp)
        Column(Modifier.weight(1f)) {
            BasicText("Novice League", style = TextStyle(color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black))
            BasicText(
                "Finish top of this ladder to get promoted — every weekly season pays out coins, gems and the WEEKLY CROWN frame.",
                style = TextStyle(color = Color(0xFFFFE9B0), fontSize = 10.5.sp),
            )
        }
        Box(
            Modifier
                .background(Color(0xFFFFD93D), RoundedCornerShape(12.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CoinIcon(16.dp)
                SpacerW(4.dp)
                BasicText("x10", style = TextStyle(color = Color(0xFF7A4A00), fontSize = 14.sp, fontWeight = FontWeight.Black))
            }
        }
    }
}

@Composable
private fun BoardRow(rank: String, name: String, avatarId: Int, meta: String, isYou: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (isYou) Color(0xFFFFD93D) else Color(0xFFF4F7FF), RoundedCornerShape(14.dp))
            .border(2.dp, if (isYou) Color(0xFFB8860B) else Color(0xFF9DC0FF).copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            rank,
            style = TextStyle(color = if (isYou) Color(0xFF7A4A00) else Color(0xFF123A6E), fontSize = 15.sp, fontWeight = FontWeight.Black),
            modifier = Modifier.width(46.dp),
        )
        Box(
            Modifier
                .background(Color.White, CircleShape)
                .border(2.dp, if (isYou) Color(0xFFB8860B) else Color(0xFF9AD5FF), CircleShape)
                .padding(2.dp),
        ) { AvatarIcon(avatarId, 34.dp) }
        SpacerW(10.dp)
        BasicText(
            name.uppercase(),
            style = TextStyle(color = if (isYou) Color(0xFF3A2A18) else Color(0xFF123A6E), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        BasicText(meta, style = TextStyle(color = if (isYou) Color(0xFF7A4A00) else Color(0xFF8C9DCB), fontSize = 13.sp, fontWeight = FontWeight.Black))
    }
}

@Composable
private fun EmptyBoardNote() {
    BasicText(
        "The board is warming up — win levels and claim the podium for your crew!",
        style = TextStyle(color = Color(0xFF9DC0FF), fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
    )
}

// ================================================================ profile (Info dialog)

enum class ProfileTab { AVATAR, FRAME, BADGE, SHOP }

/**
 * Reference "Info" profile: avatar + name + pencil, Avatar / Frame / Badge /
 * Shop tabs, Save at the bottom. Frames are grouped by SOURCE (level / shop
 * coins / gems / weekly winners / event / watch-ads) just like the badges
 * wall, and the whole thing reads the same [Prefs.inventory] ledger the
 * save-loop keeps.
 */
@Composable
fun ProfileScreen(
    prefs: Prefs,
    ads: AdsManager,
    activity: Activity,
    onOpenShop: (() -> Unit)? = null,
    onClose: () -> Unit,
) {
    var tab by remember { mutableStateOf(ProfileTab.AVATAR) }
    var nameDraft by remember { mutableStateOf(prefs.playerName) }
    var editName by remember { mutableStateOf(false) }
    val inventory = prefs.inventory()

    ScreenScaffold(title = "Info", current = BannerTab.PROFILE, onNav = { if (it == BannerTab.HOME) onClose() }, onClose = onClose) {
        // header identity row
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .background(Color(0xFF0A2C6E).copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                .border(2.dp, Color(0xFF9DC0FF).copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .background(Color.White, CircleShape)
                    .border(3.dp, Color(0xFFFFD93D), CircleShape)
                    .padding(3.dp),
            ) { AvatarIcon(prefs.avatarId.intValue, 52.dp, frameId = prefs.avatarFrame.intValue) }
            SpacerW(10.dp)
            if (editName) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.foundation.text.BasicTextField(
                        value = nameDraft,
                        onValueChange = { nameDraft = it.take(14) },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold),
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF123A6E), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    )
                    SpacerW(8.dp)
                    SquishyButton(
                        "OK",
                        onClick = { prefs.setPlayerName(nameDraft); editName = false },
                        top = Color(0xFF6FEE85),
                        bottom = Color(0xFF1FA94F),
                        height = 34.dp,
                        textSize = 12.dp,
                        modifier = Modifier.width(64.dp),
                    )
                }
            } else {
                Column(Modifier.weight(1f)) {
                    BasicText(prefs.playerName, style = TextStyle(color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black))
                    BasicText(
                        "Level ${prefs.maxLevel.intValue}  •  ${prefs.wins.intValue} wins  •  ${inventory.framesOwned} frames  •  ${inventory.ridesOwned} rides",
                        style = TextStyle(color = Color(0xFF9DC0FF), fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                    )
                }
                Box(
                    Modifier
                        .size(36.dp)
                        .background(Color(0xFF58D76B), CircleShape)
                        .border(2.dp, Color.White, CircleShape)
                        .clickable { editName = true },
                    contentAlignment = Alignment.Center,
                ) { GameIcon(GameIconKind.GRAD_CAP, 18.dp) }
            }
        }
        SpacerH(8.dp)
        // tab strip
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .background(Color(0xFF0A2C6E), RoundedCornerShape(16.dp))
                .padding(4.dp),
        ) {
            TabButton("AVATOR", selected = tab == ProfileTab.AVATAR, modifier = Modifier.weight(1f)) { tab = ProfileTab.AVATAR }
            TabButton("FRAME", selected = tab == ProfileTab.FRAME, modifier = Modifier.weight(1f)) { tab = ProfileTab.FRAME }
            TabButton("BADGE", selected = tab == ProfileTab.BADGE, modifier = Modifier.weight(1f)) { tab = ProfileTab.BADGE }
            TabButton("SHOP", selected = tab == ProfileTab.SHOP, modifier = Modifier.weight(1f)) { tab = ProfileTab.SHOP }
        }
        SpacerH(8.dp)
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            when (tab) {
                ProfileTab.AVATAR -> {
                    BasicText("CHOOSE YOUR DP", style = TextStyle(color = Color(0xFF9DC0FF), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp))
                    SpacerH(6.dp)
                    for (row in listOf(listOf(0, 1, 2, 3), listOf(4, 5, 6, 7))) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (id in row) {
                                val selected = prefs.avatarId.intValue == id
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .background(if (selected) Color(0xFFFFF0C2) else Color(0xFFF4F7FF), RoundedCornerShape(16.dp))
                                        .border(3.dp, if (selected) Color(0xFFFFB300) else Color(0xFF9DC0FF).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                        .clickable { prefs.setAvatar(id) }
                                        .padding(6.dp),
                                    contentAlignment = Alignment.Center,
                                ) { AvatarIcon(id, 52.dp) }
                            }
                        }
                        SpacerH(8.dp)
                    }
                }

                ProfileTab.FRAME -> {
                    FrameShelf("FREE & LEVELS", AvatarFrames.framesOf("u"), prefs, ads, activity, showBuy = false)
                    FrameShelf("SHOP — COINS", AvatarFrames.framesOf("c"), prefs, ads, activity, showBuy = true)
                    FrameShelf("SHOP — GEMS", AvatarFrames.framesOf("g"), prefs, ads, activity, showBuy = true)
                    FrameShelf("WEEKLY WINNERS", AvatarFrames.framesOf("w"), prefs, ads, activity, note = "Finish the weekly podium top-3 to earn this crown.")
                    FrameShelf("EVENT REWARDS", AvatarFrames.framesOf("e"), prefs, ads, activity, note = "Claim on special Car-Tour days from the Skin Shop.")
                    FrameShelf("WATCH ADS", AvatarFrames.framesOf("a"), prefs, ads, activity, note = "Watch ${AvatarFrames.byId(8).adWatches} reward ads to adopt the cat squad.")
                }

                ProfileTab.BADGE -> {
                    BadgeRow("EARLY RISER", "Reach level 10", prefs.maxLevel.intValue >= 10)
                    SpacerH(6.dp)
                    BadgeRow("JAM MASTER", "Win 50 levels", prefs.wins.intValue >= 50)
                    SpacerH(6.dp)
                    BadgeRow("BIG COLLECTOR", "Unlock 24+ rides in the collection", RideCollection.unlockedCount(prefs.maxLevel.intValue) >= 24)
                    SpacerH(6.dp)
                    BadgeRow("BANK BREAKER", "Fill the piggy bank to 800/800", prefs.piggyFull)
                    SpacerH(6.dp)
                    BadgeRow("RANKED STAR", "Break into the global top 100", (LiveBoard.me.value?.rank ?: 10_000) <= 100)
                    SpacerH(6.dp)
                    BadgeRow("SOCIAL SPARK", "Redeem a friend's referral code", prefs.referredBy.value != null)
                    SpacerH(10.dp)
                }

                ProfileTab.SHOP -> {
                    BasicText(
                        "FRAMES STOREFRONT — coin & gem shelves straight into your ledger.",
                        style = TextStyle(color = Color(0xFF9DC0FF), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp),
                    )
                    SpacerH(6.dp)
                    FrameShelf(null, AvatarFrames.framesOf("c") + AvatarFrames.framesOf("g"), prefs, ads, activity, showBuy = true)
                    if (onOpenShop != null) {
                        SquishyButton("OPEN THE FULL SHOP", onClick = onOpenShop, height = 44.dp, textSize = 14.dp, top = Color(0xFFFFB340), bottom = Color(0xFFE07F00))
                        SpacerH(8.dp)
                    }
                }
            }
        }
        // Save row (persist + cloud)
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            SquishyButton(
                "SAVE",
                onClick = {
                    CloudSave.sync(prefs, force = true)
                    onClose()
                },
                modifier = Modifier.weight(1f),
                top = Color(0xFF6FEE85),
                bottom = Color(0xFF1FA94F),
                height = 50.dp,
                textSize = 17.dp,
            )
        }
    }
}

/** One source shelf of frame tiles with buy/select behaviour. */
@Composable
private fun FrameShelf(
    title: String?,
    list: List<com.codex.carjam.game.AvatarFrame>,
    prefs: Prefs,
    ads: AdsManager,
    activity: Activity,
    showBuy: Boolean = false,
    note: String? = null,
) {
    if (title != null) {
        SpacerH(4.dp)
        BasicText(title, style = TextStyle(color = Color(0xFF9DC0FF), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp))
    }
    if (note != null) {
        BasicText(note, style = TextStyle(color = Color(0xFF7FA8F0), fontSize = 10.sp, fontWeight = FontWeight.SemiBold))
    }
    SpacerH(6.dp)
    val rows = list.chunked(3)
    for (row in rows) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (f in row) {
                FrameTile(f, prefs, ads, activity, showBuy, Modifier.weight(1f))
            }
            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
        }
        SpacerH(8.dp)
    }
}

@Composable
private fun FrameTile(
    f: com.codex.carjam.game.AvatarFrame,
    prefs: Prefs,
    ads: AdsManager,
    activity: Activity,
    showBuy: Boolean,
    modifier: Modifier = Modifier,
) {
    val owned = prefs.ownedFrames.value.contains(f.id)
    val selected = prefs.avatarFrame.intValue == f.id
    val levelOk = f.unlockLevel <= 0 || prefs.maxLevel.intValue >= f.unlockLevel
    val adWatches = prefs.frameAdWatches.intValue
    Column(
        modifier
            .background(if (selected) Color(0xFFFFF0C2) else Color(0xFFF4F7FF), RoundedCornerShape(14.dp))
            .border(3.dp, if (selected) Color(0xFFFFB300) else Color(0xFF9DC0FF).copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .clickable {
                when {
                    owned -> prefs.selectFrame(f.id)
                    showBuy && AvatarFrames.purchase(f, prefs) -> {
                        prefs.unlockFrame(f.id, f.source)
                        prefs.selectFrame(f.id)
                        CloudSave.sync(prefs, force = true)
                    }
                }
            }
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            AvatarIcon(prefs.avatarId.intValue, 46.dp, frameId = f.id, modifier = Modifier.align(Alignment.Center))
            if (!owned && !levelOk) {
                Box(Modifier.align(Alignment.Center).size(20.dp).background(Color(0xFF9AA5B1), CircleShape).border(1.5.dp, Color.White, CircleShape)) {
                    GameIcon(GameIconKind.LOCK, 10.dp, modifier = Modifier.align(Alignment.Center))
                }
            }
        }
        SpacerH(3.dp)
        BasicText(f.title, style = TextStyle(color = InkS, fontSize = 8.5.sp, fontWeight = FontWeight.ExtraBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
        BasicText(
            when {
                selected -> "USING"
                owned -> "USE"
                f.adWatches > 0 -> "$adWatches/${f.adWatches} ADS"
                f.unlockLevel > 0 -> "LVL ${f.unlockLevel}"
                f.gemPrice > 0 -> "${f.gemPrice} GEMS"
                f.coinPrice > 0 -> "${f.coinPrice} COINS"
                f.source == "w" -> "WIN WEEKLY"
                f.source == "e" -> "EVENT"
                else -> "FREE"
            },
            style = TextStyle(
                color = if (selected) Color(0xFF2FA84F) else if (owned) Color(0xFF123A6E) else SubS,
                fontSize = 8.sp,
                fontWeight = FontWeight.ExtraBold,
            ),
        )
        // watch-ad strip on the Meow frame
        if (f.adWatches > 0 && !owned && ads.rewardedReady.value) {
            SpacerH(3.dp)
            SquishyButton(
                "WATCH AD",
                onClick = {
                    ads.showRewarded(activity) {
                        val n = prefs.noteFrameAdWatch()
                        if (n >= f.adWatches) {
                            prefs.unlockFrame(f.id, "a")
                            prefs.selectFrame(f.id)
                            CloudSave.sync(prefs, force = true)
                        }
                    }
                },
                top = Color(0xFF6FEE85),
                bottom = Color(0xFF1FA94F),
                height = 26.dp,
                textSize = 8.dp,
            )
        }
    }
}

@Composable
private fun BadgeRow(title: String, desc: String, earned: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (earned) Color(0xFFFFF0C2) else Color(0xFF0A2C6E).copy(alpha = 0.55f), RoundedCornerShape(14.dp))
            .border(2.dp, if (earned) Color(0xFFFFB300) else Color(0xFF9DC0FF).copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(30.dp)) {
            val c = if (earned) Color(0xFFFFC93C) else Color(0xFF4A6BC8)
            val cx = size.width / 2f
            val cy = size.height / 2f
            val path = Path()
            for (i in 0 until 10) {
                val rad = if (i % 2 == 0) size.width * 0.46f else size.width * 0.20f
                val a = Math.toRadians((i * 36.0) - 90.0)
                val x = cx + (kotlin.math.cos(a) * rad).toFloat()
                val y = cy + (kotlin.math.sin(a) * rad).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path, c)
            drawPath(path, if (earned) Color(0xFFB8860B) else Color(0xFF2A55A8), style = Stroke(1.6f))
        }
        SpacerW(10.dp)
        Column(Modifier.weight(1f)) {
            BasicText(title, style = TextStyle(color = if (earned) Color(0xFF7A4A00) else Color(0xFF9DC0FF), fontSize = 13.sp, fontWeight = FontWeight.Black))
            BasicText(desc, style = TextStyle(color = if (earned) SubS else Color(0xFF7FA8F0), fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold))
        }
        BasicText(
            if (earned) "EARNED" else "LOCKED",
            style = TextStyle(color = if (earned) Color(0xFF2FA84F) else Color(0xFF7FA8F0), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold),
        )
    }
}

// ================================================================ skin shop dialog

@Composable
fun SkinShopDialog(prefs: Prefs, ads: AdsManager, activity: Activity, onClose: () -> Unit) {
    var tab by remember { mutableStateOf(SkinTab.VEHICLE) }
    val event = remember { Events.today() }
    DialogOverlay {
        PanelCard(Modifier.padding(14.dp).width(360.dp)) {
            DialogTitleText("SKIN SHOP")
            SpacerH(6.dp)
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0A2C6E), RoundedCornerShape(14.dp))
                    .padding(4.dp),
            ) {
                TabButton("VEHICLE", selected = tab == SkinTab.VEHICLE, modifier = Modifier.weight(1f)) { tab = SkinTab.VEHICLE }
                TabButton("SCENE", selected = tab == SkinTab.SCENE, modifier = Modifier.weight(1f)) { tab = SkinTab.SCENE }
            }
            SpacerH(8.dp)
            Column(
                Modifier
                    .fillMaxWidth()
                    .height(430.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                val tiles = if (tab == SkinTab.VEHICLE) SkinShopCatalog.vehicles else SkinShopCatalog.scenes
                // SECTION: unlock by passing levels
                ShopSectionHeader("Unlock by Passing Levels")
                SpacerH(6.dp)
                ShopTileRows(
                    tiles = tiles.filter { it.source == SkinSource.LEVELS },
                    prefs = prefs,
                    tab = tab,
                    event = event,
                    ads = ads,
                    activity = activity,
                )
                // SECTION: events
                SpacerH(6.dp)
                ShopSectionHeader("Unlock in Event")
                SpacerH(6.dp)
                ShopTileRows(
                    tiles = tiles.filter { it.source == SkinSource.EVENT },
                    prefs = prefs,
                    tab = tab,
                    event = event,
                    ads = ads,
                    activity = activity,
                )
                // SECTION: packs
                SpacerH(6.dp)
                ShopSectionHeader("Unlock by Buying Packs")
                SpacerH(6.dp)
                ShopTileRows(
                    tiles = tiles.filter { it.source == SkinSource.PACKS },
                    prefs = prefs,
                    tab = tab,
                    event = event,
                    ads = ads,
                    activity = activity,
                )
            }
            SpacerH(8.dp)
            SquishyButton("CLOSE", onClick = onClose, top = Color(0xFF9AA5B1), bottom = Color(0xFF6E7883), height = 44.dp, textSize = 15.dp)
            SpacerH(2.dp)
        }
    }
}

@Composable
private fun ShopSectionHeader(label: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color(0xFF2A55A8), Color(0xFF1B3E86))), RoundedCornerShape(12.dp))
            .border(1.5.dp, Color(0xFF9DC0FF).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(12.dp)) {
            val p = Path().apply {
                moveTo(size.width * 0.15f, size.height * 0.50f)
                lineTo(size.width * 0.50f, size.height * 0.15f)
                lineTo(size.width * 0.85f, size.height * 0.50f)
                lineTo(size.width * 0.50f, size.height * 0.85f)
                close()
            }
            drawPath(p, Color(0xFFFFD93D))
        }
        SpacerW(8.dp)
        BasicText(label, style = TextStyle(color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold))
    }
}

@Composable
private fun ShopTileRows(
    tiles: List<SkinTile>,
    prefs: Prefs,
    tab: SkinTab,
    event: com.codex.carjam.game.GameEvent,
    ads: AdsManager,
    activity: Activity,
) {
    val rows = tiles.chunked(2)
    for (row in rows) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (tile in row) {
                ShopTile(tile, prefs, tab, event, ads, activity, Modifier.weight(1f))
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
        SpacerH(8.dp)
    }
}

@Composable
private fun ShopTile(
    tile: SkinTile,
    prefs: Prefs,
    tab: SkinTab,
    event: com.codex.carjam.game.GameEvent,
    ads: AdsManager,
    activity: Activity,
    modifier: Modifier = Modifier,
) {
    val level = prefs.maxLevel.intValue
    val skinRef = tile.ref.toIntOrNull()
    val themeRef = runCatching { LevelTheme.valueOf(tile.ref) }.getOrNull()
    val isAuto = tile.ref == "auto"
    val unlocked = when (tile.source) {
        SkinSource.LEVELS -> if (isAuto) true else level >= tile.unlockLevel
        SkinSource.EVENT -> event.isSpecial()
        SkinSource.PACKS -> prefs.hasVehiclePack.value
    }
    val using = when {
        tab == SkinTab.SCENE && isAuto -> prefs.autoSceneSwitch.value
        tab == SkinTab.SCENE && themeRef != null -> prefs.manualScene.value == themeRef.name && !prefs.autoSceneSwitch.value
        else -> false
    }
    Column(
        modifier
            .background(if (using) Color(0xFFFFF0C2) else if (unlocked) Color(0xFFF4F7FF) else Color(0xFFB9AE96), RoundedCornerShape(14.dp))
            .border(2.5.dp, if (using) Color(0xFFFFB300) else if (unlocked) Color(0xFF9DC0FF) else Color(0xFF9A8F78), RoundedCornerShape(14.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // preview area
        Box(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(if (unlocked) Color.White else Color(0xFF9AA5B1).copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                if (tab == SkinTab.VEHICLE) {
                    // trio preview with the skin overlay
                    val colA = if (unlocked) CarColor.BLUE else CarColor.GRAY
                    val colB = if (unlocked) CarColor.YELLOW else CarColor.GRAY
                    val colC = if (unlocked) CarColor.RED else CarColor.GRAY
                    with(Painters) {
                        drawCar(w * 0.26f, h * 0.62f, 0f, CarType.SEDAN, colA, scale = 0.16f, alpha = if (unlocked) 1f else 0.55f, arrowVisible = false)
                        drawCar(w * 0.52f, h * 0.58f, 0f, CarType.BUS, colB, scale = 0.16f, alpha = if (unlocked) 1f else 0.55f, arrowVisible = false)
                        drawCar(w * 0.79f, h * 0.62f, 0f, CarType.VAN, colC, scale = 0.16f, alpha = if (unlocked) 1f else 0.55f, arrowVisible = false)
                        if (skinRef != null && unlocked) {
                            drawCarSkinOverlay(skinRef, w * 0.26f, h * 0.62f, 0f, CarType.SEDAN, 0.16f)
                            drawCarSkinOverlay(skinRef, w * 0.52f, h * 0.58f, 0f, CarType.BUS, 0.16f)
                            drawCarSkinOverlay(skinRef, w * 0.79f, h * 0.62f, 0f, CarType.VAN, 0.16f)
                        }
                    }
                } else {
                    // scene preview: themed mini-board
                    val t = themeRef
                    if (t != null) {
                        drawRoundRect(
                            Brush.verticalGradient(listOf(if (unlocked) t.skyTop else Color(0xFFB9C1C8), if (unlocked) t.skyBottom else Color(0xFF8D959C))),
                            Offset(8f, 6f),
                            Size(w - 16f, h - 12f),
                            CornerRadius(8f),
                        )
                        drawRect(if (unlocked) t.road else Color(0xFF6B7379), Offset(8f, h * 0.62f), Size(w - 16f, h * 0.14f))
                        drawRoundRect(if (unlocked) t.arenaBg else Color(0xFF9AA5B1), Offset(w * 0.2f, h * 0.76f), Size(w * 0.6f, h * 0.16f), CornerRadius(3f))
                    } else if (isAuto) {
                        // auto-switch: 4-quadrant mosaic
                        val quads = listOf(LevelTheme.SEA, LevelTheme.WINTER, LevelTheme.LAVA, LevelTheme.JUNGLE)
                        quads.forEachIndexed { i, q ->
                            val qx = 8f + (i % 2) * (w - 16f) / 2f
                            val qy = 6f + (i / 2) * (h - 12f) / 2f
                            drawRect(q.skyTop, Offset(qx, qy), Size((w - 16f) / 2f - 2f, (h - 12f) / 2f - 2f))
                        }
                        drawRoundRect(Color(0xFF1565C0).copy(alpha = 0.8f), Offset(w * 0.32f, h * 0.34f), Size(w * 0.36f, h * 0.32f), CornerRadius(8f))
                    } else {
                        drawRoundRect(Color(0xFF8D959C), Offset(8f, 6f), Size(w - 16f, h - 12f), CornerRadius(8f))
                    }
                }
            }
            if (!unlocked && tile.source != SkinSource.PACKS) {
                BasicText("???", style = TextStyle(color = Color(0xFF6E6350), fontSize = 14.sp, fontWeight = FontWeight.Black))
            }
        }
        SpacerH(5.dp)
        BasicText(tile.title, style = TextStyle(color = InkS, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
        SpacerH(2.dp)
        when {
            tile.source == SkinSource.EVENT && !unlocked -> BasicText("SPECIAL DAYS ONLY", style = TextStyle(color = SubS, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold))
            tile.source == SkinSource.EVENT && !prefs.ownedFrames.value.contains(6) -> SquishyButton(
                "CLAIM FRAME",
                onClick = {
                    prefs.unlockFrame(6, "e")
                    prefs.selectFrame(6)
                    prefs.addCoins(50)
                    CloudSave.sync(prefs, force = true)
                },
                top = Color(0xFF6FEE85),
                bottom = Color(0xFF1FA94F),
                height = 26.dp,
                textSize = 8.5.dp,
            )
            tile.source == SkinSource.PACKS && !prefs.hasVehiclePack.value -> SquishyButton(
                "500",
                onClick = {
                    if (prefs.coins.intValue >= 500) {
                        prefs.addCoins(-500)
                        prefs.grantVehiclePack()
                        prefs.addCoins(250)
                        CloudSave.sync(prefs, force = true)
                    }
                },
                top = Color(0xFFFFD93D),
                bottom = Color(0xFFF0A500),
                height = 28.dp,
                textSize = 10.dp,
                icon = { CoinIcon(14.dp) },
            )
            using -> BasicText("USING", style = TextStyle(color = Color(0xFF2FA84F), fontSize = 9.sp, fontWeight = FontWeight.Black))
            unlocked && tile.source == SkinSource.LEVELS && tab == SkinTab.SCENE -> SquishyButton(
                "USE",
                onClick = {
                    if (isAuto) {
                        prefs.setAutoScene(true)
                    } else if (themeRef != null) {
                        prefs.setManualScene(themeRef.name)
                    }
                    CloudSave.sync(prefs, force = true)
                },
                top = Color(0xFF6FEE85),
                bottom = Color(0xFF1FA94F),
                height = 26.dp,
                textSize = 9.dp,
            )
            unlocked && tab == SkinTab.VEHICLE && tile.source == SkinSource.LEVELS -> BasicText(
                "DRIPS IN LEVELS",
                style = TextStyle(color = Color(0xFF123A6E), fontSize = 8.sp, fontWeight = FontWeight.ExtraBold),
            )
            else -> BasicText(
                if (tile.source == SkinSource.PACKS) "OWNED" else "LVL ${tile.unlockLevel}",
                style = TextStyle(color = SubS, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold),
            )
        }
    }
}

// ================================================================ skin shop (full screen)

/** Full-page Skin Shop (banner screen) reusing the dialog sections. */
@Composable
fun SkinShopScreen(prefs: Prefs, ads: AdsManager, onNav: (BannerTab) -> Unit) {
    var tab by remember { mutableStateOf(SkinTab.VEHICLE) }
    val event = remember { Events.today() }
    val activity = androidx.compose.ui.platform.LocalContext.current as? Activity
    ScreenScaffold(
        title = "Skin Shop",
        current = BannerTab.SKIN,
        onNav = onNav,
        coinSlot = { CoinPill(prefs.coins.intValue, compact = true) },
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .background(Color(0xFF0A2C6E), RoundedCornerShape(14.dp))
                .padding(4.dp),
        ) {
            TabButton("VEHICLE", selected = tab == SkinTab.VEHICLE, modifier = Modifier.weight(1f)) { tab = SkinTab.VEHICLE }
            TabButton("SCENE", selected = tab == SkinTab.SCENE, modifier = Modifier.weight(1f)) { tab = SkinTab.SCENE }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp),
        ) {
            val tiles = if (tab == SkinTab.VEHICLE) SkinShopCatalog.vehicles else SkinShopCatalog.scenes
            ShopSectionHeader("Unlock by Passing Levels")
            SpacerH(6.dp)
            if (activity != null) {
                ShopTileRows(tiles.filter { it.source == SkinSource.LEVELS }, prefs, tab, event, ads, activity)
                SpacerH(6.dp)
                ShopSectionHeader("Unlock in Event")
                SpacerH(6.dp)
                ShopTileRows(tiles.filter { it.source == SkinSource.EVENT }, prefs, tab, event, ads, activity)
                SpacerH(6.dp)
                ShopSectionHeader("Unlock by Buying Packs")
                SpacerH(6.dp)
                ShopTileRows(tiles.filter { it.source == SkinSource.PACKS }, prefs, tab, event, ads, activity)
            }
            SpacerH(8.dp)
        }
    }
}

// ================================================================ welcome starter dialog

@Composable
fun WelcomeSkinsDialog(prefs: Prefs, onDone: () -> Unit) {
    var thanked by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        thanked = prefs.grantSkinsWelcomeIfNeeded()
        if (!thanked) onDone()
    }
    if (!thanked) return
    DialogOverlay {
        PanelCard(Modifier.padding(18.dp).width(330.dp)) {
            DialogTitleText("SKIN SHOP UNLOCKED")
            SpacerH(8.dp)
            BasicText(
                "Welcome to the garage lane! Here's your starter kit — a SPRINTER frame, a fat coin tip, and one of each queue tool. Hit SKIN on home whenever you want a fresh look.",
                style = TextStyle(color = Color(0xFF8C6A3F), fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center),
                modifier = Modifier.fillMaxWidth(),
            )
            SpacerH(10.dp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .background(Color.White, CircleShape)
                        .border(3.dp, Color(0xFF2E5FBB), CircleShape)
                        .padding(3.dp),
                ) { AvatarIcon(prefs.avatarId.intValue, 46.dp, frameId = 1) }
                SpacerW(10.dp)
                Column {
                    BasicText("SPRINTER FRAME", style = TextStyle(color = InkS, fontSize = 13.sp, fontWeight = FontWeight.Black))
                    BasicText("Wearing it already — swap anytime in PROFILE > FRAME.", style = TextStyle(color = SubS, fontSize = 10.sp))
                }
            }
            SpacerH(8.dp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                CoinIcon(24.dp)
                SpacerW(6.dp)
                BasicText("+100 coins", style = TextStyle(color = InkS, fontSize = 13.sp, fontWeight = FontWeight.Black))
                SpacerW(14.dp)
                GameIcon(GameIconKind.SHUFFLE, 20.dp)
                SpacerW(4.dp)
                BasicText("+1 mix", style = TextStyle(color = InkS, fontSize = 13.sp, fontWeight = FontWeight.Black))
                SpacerW(14.dp)
                GameIcon(GameIconKind.REFRESH, 20.dp)
                SpacerW(4.dp)
                BasicText("+1 refresh", style = TextStyle(color = InkS, fontSize = 13.sp, fontWeight = FontWeight.Black))
            }
            SpacerH(12.dp)
            SquishyButton(
                "LET'S RIDE",
                onClick = {
                    prefs.selectFrame(1)
                    CloudSave.sync(prefs, force = true)
                    onDone()
                },
                height = 46.dp,
                textSize = 16.dp,
            )
            SpacerH(2.dp)
        }
    }
}
