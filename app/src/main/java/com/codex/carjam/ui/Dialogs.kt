package com.codex.carjam.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codex.carjam.R
import com.codex.carjam.game.CloudSave
import com.codex.carjam.game.LayoutStyle
import com.codex.carjam.game.LeaderboardApi
import com.codex.carjam.game.LevelTheme
import com.codex.carjam.game.Prefs
import com.codex.carjam.game.SoundManager
import com.codex.carjam.game.render.GameIconKind
import kotlinx.coroutines.delay

@Composable
private fun DialogTitle(text: String, fill: Color, outline: Color) {
    OutlinedTextC(text = text, size = 30.dp, fill = fill, outline = outline)
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = label,
            style = TextStyle(color = Color(0xFF4A3826), fontSize = 20.sp, fontWeight = FontWeight.Bold),
        )
        val thumbOffset by animateDpAsState(targetValue = if (checked) 32.dp else 4.dp, label = "thumb")
        Box(
            Modifier
                .width(68.dp)
                .height(38.dp)
                .background(
                    if (checked) Color(0xFF3DDC5F) else Color(0xFFC2B49A),
                    RoundedCornerShape(19.dp),
                )
                .border(2.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(19.dp))
                .clickable { onChange(!checked) },
        ) {
            Box(
                Modifier
                    .offset(x = thumbOffset, y = 4.dp)
                    .size(30.dp)
                    .background(Color.White, CircleShape)
                    .border(2.dp, Color(0x22000000), CircleShape),
            )
        }
    }
}

@Composable
fun SettingsDialog(
    prefs: Prefs,
    showRestart: Boolean,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onHome: () -> Unit,
) {
    DialogOverlay {
        PanelCard {
            DialogTitle("SETTINGS", fill = Color(0xFFFFFFFF), outline = Color(0xFF7A4A12))
            SpacerH(10.dp)
            ToggleRow("Sound FX", prefs.soundOn.value) { prefs.setSound(it) }
            ToggleRow("Music", prefs.musicOn.value) {
                prefs.setMusic(it)
                SoundManager.active?.applyMusicPref()
            }
            ToggleRow("Vibration", prefs.vibrateOn.value) { prefs.setVibrate(it) }

            // ---- cloud save (progress, coins & gems backed up to MongoDB)
            SpacerH(6.dp)
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Box(
                    Modifier
                        .size(10.dp)
                        .background(
                            when (CloudSave.status.value) {
                                true -> Color(0xFF3DDC5F)
                                false -> Color(0xFFC2B49A)
                                null -> Color(0xFFFFC93C)
                            },
                            CircleShape,
                        ),
                )
                SpacerW(8.dp)
                BasicText(
                    "CLOUD SAVE — " + when (CloudSave.status.value) {
                        true -> "progress backed up"
                        false -> "offline, will retry"
                        null -> "syncing…"
                    },
                    style = TextStyle(color = Color(0xFF8C6A3F), fontSize = 12.5.sp, fontWeight = FontWeight.Bold),
                )
            }
            SpacerH(6.dp)
            var showCode by remember { mutableStateOf(false) }
            var showRestore by remember { mutableStateOf(false) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SquishyButton(
                    "MY SYNC CODE",
                    onClick = { showCode = true },
                    modifier = Modifier.weight(1f),
                    top = Color(0xFF6FB6FF),
                    bottom = Color(0xFF3B7FE0),
                    height = 40.dp,
                    textSize = 12.dp,
                )
                SquishyButton(
                    "RESTORE SAVE",
                    onClick = { showRestore = true },
                    modifier = Modifier.weight(1f),
                    top = Color(0xFFFFB340),
                    bottom = Color(0xFFE07F00),
                    height = 40.dp,
                    textSize = 12.dp,
                )
            }
            if (showCode) {
                SyncCodeDialog(prefs = prefs, onClose = { showCode = false })
            }
            if (showRestore) {
                RestoreSaveDialog(prefs = prefs, onClose = { showRestore = false })
            }
            SpacerH(10.dp)
            SquishyButton("RESUME", onClick = onResume)
            SpacerH(10.dp)
            if (showRestart) {
                SquishyButton(
                    "RESTART LEVEL",
                    onClick = onRestart,
                    top = Color(0xFFFFB340),
                    bottom = Color(0xFFE07F00),
                )
                SpacerH(10.dp)
            }
            SquishyButton(
                "HOME",
                onClick = onHome,
                top = Color(0xFF9AA5B1),
                bottom = Color(0xFF6E7883),
            )
            SpacerH(4.dp)
        }
    }
}

@Composable
private fun LeagueRow(rank: Int, name: String, rating: Int, highlight: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(
                if (highlight) Color(0xFFFFC93C) else Color.White,
                RoundedCornerShape(10.dp),
            )
            .border(1.5.dp, if (highlight) Color(0xFFE09B13) else Color(0xFFEAD9BC), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            "#$rank",
            style = TextStyle(
                color = if (rank == 1) Color(0xFFB8860B) else Color(0xFF8C6A3F),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.ExtraBold,
            ),
            modifier = Modifier.width(42.dp),
        )
        BasicText(
            name.uppercase(),
            style = TextStyle(color = Color(0xFF4A3826), fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
        GameIcon(GameIconKind.TROPHY, 14.dp)
        SpacerW(4.dp)
        BasicText("$rating", style = TextStyle(color = Color(0xFF8C6A3F), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold))
    }
}

@Composable
fun WinDialog(
    level: Int,
    coinsEarned: Int,
    practice: Boolean,
    prefs: Prefs,
    canMultiply: Boolean,
    onMultiplyX5: () -> Unit,
    onNext: () -> Unit,
    onHome: () -> Unit,
) {
    // global league (live from the MongoDB leaderboard) + rank-climb animation
    var top by remember { mutableStateOf<List<LeaderboardApi.Entry>?>(null) }
    var me by remember { mutableStateOf<LeaderboardApi.Entry?>(null) }
    var panelIn by remember { mutableStateOf(false) }
    var climb by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!practice) {
            LeaderboardApi.fetchTop(prefs.deviceId) { t, m ->
                top = t
                me = m
                panelIn = true
            }
            delay(700)
            climb = true
        }
    }
    val shownRank by animateIntAsState(
        targetValue = if (climb) (me?.rank ?: 0) else (me?.rank ?: 0) + 15,
        animationSpec = tween(900),
        label = "rankClimb",
    )
    // what unlocks next? (maps, boards) — the teaser strip keeps players chasing
    val nextUnlock = remember(level) {
        val ups = ArrayList<Pair<Int, String>>()
        for (t in LevelTheme.entries) {
            if (t.minLevel > level) {
                ups.add(t.minLevel to t.name.lowercase().replaceFirstChar { it.uppercase() } + " MAP")
            }
        }
        for (s in LayoutStyle.entries) {
            if (s.minLevel > level) {
                ups.add(s.minLevel to s.name.lowercase().replaceFirstChar { it.uppercase() } + " BOARD")
            }
        }
        ups.minByOrNull { it.first }
    }

    DialogOverlay {
        PanelCard {
            DialogTitle("LEVEL COMPLETE!", fill = Color(0xFFFFC93C), outline = Color(0xFF7A3E00))
            SpacerH(8.dp)
            BasicText(
                text = "Level $level cleared",
                style = TextStyle(color = Color(0xFF8C6A3F), fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
            )
            SpacerH(10.dp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                CoinIcon(32.dp)
                SpacerW(10.dp)
                BasicText(
                    text = "+$coinsEarned",
                    style = TextStyle(color = Color(0xFF4A3826), fontSize = 32.sp, fontWeight = FontWeight.ExtraBold),
                )
            }

            // ---- global league (your rank climbing the table)
            me?.let { you ->
                SpacerH(10.dp)
                AnimatedVisibility(
                    visible = panelIn,
                    enter = slideInVertically(tween(450)) { it / 3 } + fadeIn(tween(450)),
                ) {
                    androidx.compose.foundation.layout.Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            GameIcon(GameIconKind.TROPHY, 26.dp)
                            SpacerW(6.dp)
                            BasicText(
                                "GLOBAL LEAGUE",
                                style = TextStyle(color = Color(0xFF7A4A12), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp),
                            )
                            SpacerW(6.dp)
                            BasicText(
                                "#$shownRank",
                                style = TextStyle(color = Color(0xFFB8860B), fontSize = 20.sp, fontWeight = FontWeight.Black),
                            )
                        }
                        SpacerH(6.dp)
                        val rows = (top?.take(3) ?: emptyList())
                        for (e in rows) {
                            LeagueRow(e.rank, e.name, e.rating, highlight = e.deviceId == you.deviceId)
                            SpacerH(4.dp)
                        }
                        if (you.rank > 3) {
                            LeagueRow(you.rank, "YOU", you.rating, highlight = true)
                            SpacerH(4.dp)
                        }
                    }
                }
            }

            // ---- next unlock teaser
            if (nextUnlock != null) {
                SpacerH(8.dp)
                Pill(
                    "AT L${nextUnlock.first}: ${nextUnlock.second}",
                    bg = Color(0xFF7E3FC8).copy(alpha = 0.85f),
                    icon = { GameIcon(GameIconKind.LOCK, 16.dp) },
                )
            }

            SpacerH(14.dp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                SquishyButton(
                    "NEXT LEVEL",
                    onClick = onNext,
                    modifier = Modifier.weight(1f),
                    height = 52.dp,
                    textSize = 17.dp,
                )
                if (!practice) {
                    SpacerW(10.dp)
                    SquishyButton(
                        "GET X5",
                        onClick = { if (canMultiply) onMultiplyX5() },
                        modifier = Modifier.weight(1f),
                        top = if (canMultiply) Color(0xFF6FEE85) else Color(0xFFC7BBA6),
                        bottom = if (canMultiply) Color(0xFF1FA94F) else Color(0xFFA89B86),
                        height = 52.dp,
                        textSize = 17.dp,
                        icon = { GameIcon(GameIconKind.PLAY_AD, 22.dp) },
                    )
                }
            }
            SpacerH(10.dp)
            SquishyButton(
                "HOME",
                onClick = onHome,
                top = Color(0xFF9AA5B1),
                bottom = Color(0xFF6E7883),
                height = 46.dp,
                textSize = 15.dp,
            )
            SpacerH(4.dp)
        }
    }
}

@Composable
fun LoseDialog(
    reason: String,
    canRevive: Boolean,
    gemsAvailable: Boolean,
    onRevive: () -> Unit,
    onReviveGems: () -> Unit,
    onRetry: () -> Unit,
    onHome: () -> Unit,
) {
    DialogOverlay {
        PanelCard {
            DialogTitle("OUT OF MOVES!", fill = Color(0xFFFF6B57), outline = Color(0xFF7A1610))
            SpacerH(8.dp)
            BasicText(
                text = reason,
                style = TextStyle(color = Color(0xFF8C6A3F), fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
            )
            SpacerH(18.dp)
            SquishyButton(
                "REVIVE  +2 SLOTS",
                onClick = onRevive,
                top = Color(0xFF58D76B),
                bottom = Color(0xFF28A745),
                icon = { GameIcon(GameIconKind.PLAY_AD, 22.dp) },
            )
            SpacerH(6.dp)
            BasicText(
                text = if (canRevive) "ad ready" else "free revive, no ad needed",
                style = TextStyle(color = Color(0xFF8C6A3F), fontSize = 11.sp),
            )
            SpacerH(8.dp)
            SquishyButton(
                "REVIVE  25 GEMS",
                onClick = onReviveGems,
                top = Color(0xFF38BDF8),
                bottom = Color(0xFF0E7BC0),
                textSize = 18.dp,
                icon = { GameIcon(GameIconKind.GEM, 20.dp) },
            )
            BasicText(
                text = if (gemsAvailable) "instant, no ad" else "not enough gems",
                style = TextStyle(color = Color(0xFF8C6A3F), fontSize = 11.sp),
            )
            SpacerH(8.dp)
            SquishyButton(
                "TRY AGAIN",
                onClick = onRetry,
                top = Color(0xFFFFB340),
                bottom = Color(0xFFE07F00),
            )
            SpacerH(10.dp)
            SquishyButton(
                "HOME",
                onClick = onHome,
                top = Color(0xFF9AA5B1),
                bottom = Color(0xFF6E7883),
            )
            SpacerH(4.dp)
        }
    }
}

@Composable
fun LevelSelectDialog(maxLevel: Int, onPick: (Int) -> Unit, onClose: () -> Unit) {
    val total = maxOf(60, maxLevel + 12)
    DialogOverlay {
        PanelCard(Modifier.width(360.dp)) {
            DialogTitle("LEVELS", fill = Color(0xFFFFFFFF), outline = Color(0xFF7A4A12))
            SpacerH(12.dp)
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier.height(340.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(total) { i ->
                    val lvl = i + 1
                    val unlocked = lvl <= maxLevel
                    val isCurrent = lvl == maxLevel
                    Box(
                        Modifier
                            .height(56.dp)
                            .background(
                                if (unlocked) {
                                    Brush.verticalGradient(
                                        if (isCurrent) listOf(Color(0xFF58D76B), Color(0xFF28A745))
                                        else listOf(Color(0xFF6FB6FF), Color(0xFF3B7FE0)),
                                    )
                                } else {
                                    Brush.verticalGradient(listOf(Color(0xFFC7BBA6), Color(0xFFA89B86)))
                                },
                                RoundedCornerShape(16.dp),
                            )
                            .border(2.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .clickable(enabled = unlocked) { onPick(lvl) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (unlocked) {
                            BasicText(
                                text = "$lvl",
                                style = TextStyle(color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold),
                            )
                        } else {
                            GameIcon(GameIconKind.LOCK, 22.dp)
                        }
                    }
                }
            }
            SpacerH(14.dp)
            SquishyButton("CLOSE", onClick = onClose)
            SpacerH(4.dp)
        }
    }
}

/** Shows the account key (copyable) — the key IS the account, so warn first. */
@Composable
private fun SyncCodeDialog(prefs: Prefs, onClose: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    val pretty = prefs.syncKey.chunked(4).joinToString(" ")
    DialogOverlay {
        PanelCard {
            DialogTitle("MY SYNC CODE", fill = Color(0xFF6FB6FF), outline = Color(0xFF123A6E))
            SpacerH(8.dp)
            BasicText(
                "Ye code hi tumharra account hai — level, coins aur gems isi mein safe hain. Naye phone par RESTORE SAVE mein ye daalna. Kisi ko mat batana!",
                style = TextStyle(color = Color(0xFF8C6A3F), fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center),
                modifier = Modifier.fillMaxWidth(),
            )
            SpacerH(12.dp)
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .border(2.dp, Color(0xFFE3B36B), RoundedCornerShape(12.dp))
                    .padding(12.dp),
            ) {
                BasicText(
                    pretty,
                    style = TextStyle(color = Color(0xFF4A3826), fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                )
            }
            SpacerH(12.dp)
            SquishyButton(
                if (copied) "COPIED!" else "COPY CODE",
                onClick = {
                    clipboard.setText(AnnotatedString(prefs.syncKey))
                    copied = true
                },
                top = Color(0xFF6FB6FF),
                bottom = Color(0xFF3B7FE0),
                height = 44.dp,
                textSize = 14.dp,
            )
            SpacerH(8.dp)
            SquishyButton(
                "CLOSE",
                onClick = onClose,
                top = Color(0xFF9AA5B1),
                bottom = Color(0xFF6E7883),
                height = 44.dp,
                textSize = 14.dp,
            )
            SpacerH(2.dp)
        }
    }
}

/** Paste a sync code from another device to adopt that account here. */
@Composable
private fun RestoreSaveDialog(prefs: Prefs, onClose: () -> Unit) {
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    DialogOverlay {
        PanelCard {
            DialogTitle("RESTORE SAVE", fill = Color(0xFFFFB340), outline = Color(0xFF7A4A00))
            SpacerH(8.dp)
            BasicText(
                "Apne purane phone ka sync code yahan paste karo — us account ka saara progress is device par aa jayega.",
                style = TextStyle(color = Color(0xFF8C6A3F), fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center),
                modifier = Modifier.fillMaxWidth(),
            )
            SpacerH(12.dp)
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .border(2.dp, Color(0xFFE3B36B), RoundedCornerShape(12.dp))
                    .padding(12.dp),
            ) {
                if (text.isEmpty()) {
                    BasicText(
                        "paste sync code…",
                        style = TextStyle(color = Color(0xFFBBA987), fontSize = 13.sp),
                    )
                }
                BasicTextField(
                    value = text,
                    onValueChange = {
                        text = it.lowercase().filter { c -> c.isLetterOrDigit() }.take(96)
                        error = null
                    },
                    textStyle = TextStyle(color = Color(0xFF4A3826), fontSize = 13.sp, fontFamily = FontFamily.Monospace),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            error?.let {
                SpacerH(6.dp)
                BasicText(
                    it,
                    style = TextStyle(color = Color(0xFFD0342C), fontSize = 12.sp, fontWeight = FontWeight.Bold),
                )
            }
            SpacerH(12.dp)
            SquishyButton(
                if (busy) "RESTORING…" else "RESTORE THIS ACCOUNT",
                onClick = {
                    if (!busy) {
                        if (!prefs.setSyncKey(text)) {
                            error = "Code galat lag raha hai — pura code paste karo."
                        } else {
                            busy = true
                            CloudSave.sync(prefs, force = true) { ok ->
                                busy = false
                                if (ok) {
                                    onClose()
                                } else {
                                    error = "Server tak pahunch nahi paya — internet check karke retry karo."
                                }
                            }
                        }
                    }
                },
                height = 46.dp,
                textSize = 14.dp,
            )
            SpacerH(6.dp)
            BasicText(
                "Cancel",
                style = TextStyle(
                    color = Color(0xFF8C6A3F),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClose() }
                    .padding(vertical = 8.dp),
            )
            SpacerH(2.dp)
        }
    }
}

// ---------------------------------------------------------------- boosters

private data class BoostOffer(
    val kind: GameIconKind,
    val title: String,
    val desc: String,
    val count: Int,
    val price: Int,
    val gemPrice: Int = 0,
)

/** Booster shop: small coin-priced top-ups for the in-game booster belt. */
@Composable
fun BoosterShopDialog(prefs: Prefs, onClose: () -> Unit) {
    val offers = remember {
        listOf(
            BoostOffer(GameIconKind.HAMMER, "ICE HAMMER x1", "Shatters a frozen car in one blow.", 1, 250),
            BoostOffer(GameIconKind.HAMMER, "ICE HAMMER x3", "Stock up for the deep ice levels.", 3, 650),
            BoostOffer(GameIconKind.SHUFFLE, "QUEUE MIX x1", "Reshuffles the line so the front one fits.", 1, 200),
            BoostOffer(GameIconKind.SHUFFLE, "QUEUE MIX x3", "A pocket full of second chances.", 3, 500),
            BoostOffer(GameIconKind.ELIMINATE, "ELIMINATE x1", "Instantly sends one picked car out of the jam.", 1, 0, gemPrice = 12),
            BoostOffer(GameIconKind.ELIMINATE, "ELIMINATE x3", "Clear the nastiest blockers on sight.", 3, 0, gemPrice = 30),
        )
    }
    DialogOverlay {
        PanelCard(Modifier.padding(20.dp).width(340.dp)) {
            DialogTitleText("BOOSTER BELT")
            SpacerH(8.dp)
            for (offer in offers) {
                val afford = if (offer.gemPrice > 0) {
                    prefs.gems.intValue >= offer.gemPrice
                } else {
                    prefs.coins.intValue >= offer.price
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(16.dp))
                        .border(2.dp, Color(0xFFE3B36B), RoundedCornerShape(16.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GameIcon(offer.kind, 34.dp)
                    SpacerW(8.dp)
                    androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                        BasicText(
                            offer.title,
                            style = TextStyle(color = Color(0xFF4A3826), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold),
                        )
                        BasicText(
                            offer.desc,
                            style = TextStyle(color = Color(0xFF8C6A3F), fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold),
                        )
                    }
                    SpacerW(6.dp)
                    SquishyButton(
                        if (offer.gemPrice > 0) "${offer.gemPrice}" else "${offer.price}",
                        onClick = {
                            val paid = if (offer.gemPrice > 0) {
                                if (prefs.gems.intValue >= offer.gemPrice) {
                                    prefs.addGems(-offer.gemPrice)
                                    true
                                } else {
                                    false
                                }
                            } else if (prefs.coins.intValue >= offer.price) {
                                prefs.addCoins(-offer.price)
                                true
                            } else {
                                false
                            }
                            if (paid) {
                                when (offer.kind) {
                                    GameIconKind.HAMMER -> prefs.addHammers(offer.count)
                                    GameIconKind.ELIMINATE -> prefs.addElims(offer.count)
                                    else -> prefs.addShuffles(offer.count)
                                }
                                CloudSave.sync(prefs, force = true)
                            }
                        },
                        modifier = Modifier.width(84.dp),
                        top = if (afford) Color(0xFFFFCF5C) else Color(0xFFC7BBA6),
                        bottom = if (afford) Color(0xFFE09B13) else Color(0xFFA89B86),
                        height = 36.dp,
                        textSize = 13.dp,
                        icon = {
                            if (offer.gemPrice > 0) {
                                GameIcon(GameIconKind.GEM, 16.dp)
                            } else {
                                CoinIcon(16.dp)
                            }
                        },
                    )
                }
                SpacerH(8.dp)
            }
            SquishyButton(
                "CLOSE",
                onClick = onClose,
                top = Color(0xFF9AA5B1),
                bottom = Color(0xFF6E7883),
                height = 44.dp,
                textSize = 15.dp,
            )
            SpacerH(4.dp)
        }
    }
}

/** More Spot: open a locked spare parking slot for this level, coins or ad. */
@Composable
fun MoreSpotDialog(
    prefs: Prefs,
    rewardedReady: Boolean,
    onCoin: () -> Unit,
    onFree: () -> Unit,
    onClose: () -> Unit,
    price: Int = 100,
) {
    DialogOverlay {
        PanelCard(Modifier.padding(20.dp).width(340.dp)) {
            DialogTitleText("MORE SPOT")
            SpacerH(10.dp)
            GameIcon(GameIconKind.PARKING, 64.dp)
            SpacerH(8.dp)
            BasicText(
                "Unlock one extra parking spot for this level — more room to untangle the jam!",
                style = TextStyle(
                    color = Color(0xFF8C6A3F),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            SpacerH(14.dp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                SquishyButton(
                    "$price",
                    onClick = { if (prefs.coins.intValue >= price) onCoin() },
                    modifier = Modifier.weight(1f),
                    top = if (prefs.coins.intValue >= price) Color(0xFF6FEE85) else Color(0xFFC7BBA6),
                    bottom = if (prefs.coins.intValue >= price) Color(0xFF1FA94F) else Color(0xFFA89B86),
                    height = 50.dp,
                    textSize = 17.dp,
                    icon = { CoinIcon(20.dp) },
                )
                if (rewardedReady) {
                    SpacerW(10.dp)
                    SquishyButton(
                        "FREE",
                        onClick = onFree,
                        modifier = Modifier.weight(1f),
                        top = Color(0xFFFFCF5C),
                        bottom = Color(0xFFE09B13),
                        height = 50.dp,
                        textSize = 17.dp,
                        icon = { GameIcon(GameIconKind.PLAY_AD, 22.dp) },
                    )
                }
            }
            SpacerH(10.dp)
            SquishyButton(
                "CLOSE",
                onClick = onClose,
                top = Color(0xFF9AA5B1),
                bottom = Color(0xFF6E7883),
                height = 44.dp,
                textSize = 15.dp,
            )
            SpacerH(4.dp)
        }
    }
}
