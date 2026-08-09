package com.codex.carjam.ui

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codex.carjam.R
import com.codex.carjam.game.CloudSave
import com.codex.carjam.game.DailyRewards
import com.codex.carjam.game.Events
import com.codex.carjam.game.LeaderboardApi
import com.codex.carjam.game.LiveBoard
import com.codex.carjam.game.Prefs
import com.codex.carjam.game.render.GameIconKind
import com.codex.carjam.monetize.AdsManager
import com.codex.carjam.monetize.BillingManager
import java.util.Calendar
import kotlinx.coroutines.delay

private val Dark = Color(0xFF4A3826)
private val Muted = Color(0xFF8C6A3F)

private fun formatHMS(ms: Long): String {
    var s = ms / 1000
    val h = s / 3600; s %= 3600
    val m = s / 60; val sec = s % 60
    return "%02d:%02d:%02d".format(h, m, sec)
}

private fun formatDH(ms: Long): String {
    val h = ms / 3_600_000
    val d = h / 24
    return if (d > 0) "${d}d ${h % 24}h" else formatHMS(ms)
}

// ---------------------------------------------------------------------- SHOP

@Composable
fun ShopDialog(
    billing: BillingManager,
    ads: AdsManager,
    prefs: Prefs,
    activity: Activity,
    onClose: () -> Unit,
) {
    DialogOverlay {
        PanelCard(Modifier.width(360.dp)) {
            DialogTitleText("SHOP")
            SpacerH(12.dp)
            Column(
                Modifier
                    .fillMaxWidth()
                    .height(430.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                // ---- Featured No-Ads pack with designed thumbnail
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFF123A6E)),
                ) {
                    Image(
                        painter = painterResource(R.drawable.pack_no_ads),
                        contentDescription = "No Ads pack ₹99",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentScale = ContentScale.Crop,
                    )
                    Column(Modifier.padding(14.dp)) {
                        BasicText(
                            "NO ADS — FOREVER!",
                            style = TextStyle(color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold),
                        )
                        BasicText(
                            "Removes banner + interstitial ads.\nOne-time purchase, works on all your devices.",
                            style = TextStyle(color = Color(0xFFBFD4F5), fontSize = 12.5.sp),
                        )
                        SpacerH(10.dp)
                        if (prefs.removeAds.value) {
                            SquishyButton("ACTIVE", onClick = {}, top = Color(0xFF58D76B), bottom = Color(0xFF28A745), height = 44.dp, textSize = 15.dp, icon = { GameIcon(GameIconKind.SHIELD, 18.dp) })
                        } else {
                            SquishyButton(
                                "BUY  ${billing.priceFor(BillingManager.PRODUCT_NO_ADS) ?: BillingManager.PRICE_NO_ADS_DEFAULT}",
                                onClick = { billing.launchPurchase(activity, BillingManager.PRODUCT_NO_ADS) },
                                top = Color(0xFFFFB340),
                                bottom = Color(0xFFE07F00),
                                height = 44.dp,
                                textSize = 15.dp,
                            )
                        }
                    }
                }

                SpacerH(14.dp)
                BasicText("COIN PACKS", style = TextStyle(color = Muted, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp))
                SpacerH(8.dp)
                Image(
                    painter = painterResource(R.drawable.pack_coins),
                    contentDescription = "Coin packs",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .clip(RoundedCornerShape(18.dp)),
                    contentScale = ContentScale.Crop,
                )
                SpacerH(8.dp)

                // ---- Coin packs (2 per row)
                val packs = BillingManager.COINS_BY_PRODUCT.entries.sortedBy { it.value }
                for (row in packs.chunked(2)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (p in row) {
                            Column(
                                Modifier
                                    .weight(1f)
                                    .background(Color.White, RoundedCornerShape(18.dp))
                                    .border(2.dp, Color(0xFFE3B36B), RoundedCornerShape(18.dp))
                                    .padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CoinIcon(22.dp)
                                    SpacerW(6.dp)
                                    BasicText("${p.value}", style = TextStyle(color = Dark, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold))
                                }
                                if (p.value >= 1000) {
                                    BasicText("BEST VALUE", style = TextStyle(color = Color(0xFF2FA84F), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold))
                                }
                                SpacerH(8.dp)
                                SquishyButton(
                                    billing.priceFor(p.key) ?: (BillingManager.DEFAULT_PRICES[p.key] ?: "₹--"),
                                    onClick = { billing.launchPurchase(activity, p.key) },
                                    top = Color(0xFF6FB6FF),
                                    bottom = Color(0xFF3B7FE0),
                                    height = 36.dp,
                                    textSize = 13.dp,
                                )
                            }
                        }
                        if (row.size == 1) Box(Modifier.weight(1f))
                    }
                    SpacerH(10.dp)
                }

                // ---- Free coins via rewarded ad
                SquishyButton(
                    "FREE +50 COINS",
                    onClick = {
                        ads.showRewarded(activity = activity, onReward = { prefs.addCoins(50) })
                    },
                    top = Color(0xFFB678E8),
                    bottom = Color(0xFF8A45C4),
                    height = 44.dp,
                    textSize = 14.dp,
                    icon = { GameIcon(GameIconKind.PLAY_AD, 20.dp) },
                )

                SpacerH(14.dp)
                BasicText("GEM PACKS", style = TextStyle(color = Muted, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp))
                SpacerH(8.dp)
                Image(
                    painter = painterResource(R.drawable.pack_gems),
                    contentDescription = "Gem packs",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .clip(RoundedCornerShape(18.dp)),
                    contentScale = ContentScale.Crop,
                )
                SpacerH(8.dp)
                // ---- Gem packs (3 per row)
                val gemPacks = BillingManager.GEMS_BY_PRODUCT.entries.sortedBy { it.value }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (g in gemPacks) {
                        Column(
                            Modifier
                                .weight(1f)
                                .background(Color(0xFFEFF7FF), RoundedCornerShape(18.dp))
                                .border(2.dp, Color(0xFF38BDF8), RoundedCornerShape(18.dp))
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            GameIcon(GameIconKind.GEM, 26.dp)
                            SpacerH(2.dp)
                            BasicText("${g.value}", style = TextStyle(color = Dark, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold))
                            if (g.value >= 700) {
                                BasicText("BEST VALUE", style = TextStyle(color = Color(0xFF2FA84F), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold))
                            }
                            SpacerH(6.dp)
                            SquishyButton(
                                billing.priceFor(g.key) ?: (BillingManager.DEFAULT_PRICES[g.key] ?: "₹--"),
                                onClick = { billing.launchPurchase(activity, g.key) },
                                top = Color(0xFF38BDF8),
                                bottom = Color(0xFF0E7BC0),
                                height = 34.dp,
                                textSize = 12.dp,
                            )
                        }
                    }
                }

                SpacerH(10.dp)
                BasicText(
                    "Restore purchases",
                    style = TextStyle(color = Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { billing.restorePurchases() }
                        .padding(vertical = 6.dp),
                )
            }
            SpacerH(8.dp)
            SquishyButton(
                "CLOSE",
                onClick = {
                    CloudSave.sync(prefs, force = true)
                    onClose()
                },
                top = Color(0xFF9AA5B1),
                bottom = Color(0xFF6E7883),
                height = 46.dp,
                textSize = 15.dp,
            )
            SpacerH(4.dp)
        }
    }
}

// ---------------------------------------------------------------------- EVENTS

@Composable
fun EventsDialog(onClose: () -> Unit) {
    val ev = Events.today()
    DialogOverlay {
        PanelCard(Modifier.width(350.dp)) {
            DialogTitleText("DAILY EVENTS")
            SpacerH(10.dp)
            EventBannerCard(
                event = ev,
                height = 104.dp,
                trailingText = "Ends in ${formatHMS(Events.msUntilMidnight())}",
            )
            SpacerH(12.dp)
            BasicText("COMING UP", style = TextStyle(color = Muted, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp))
            SpacerH(6.dp)
            for (i in 1..4) {
                val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, i) }
                val e = Events.forDay(cal)
                val dayName = when (cal.get(Calendar.DAY_OF_WEEK)) {
                    Calendar.MONDAY -> "Mon"; Calendar.TUESDAY -> "Tue"; Calendar.WEDNESDAY -> "Wed"
                    Calendar.THURSDAY -> "Thu"; Calendar.FRIDAY -> "Fri"; Calendar.SATURDAY -> "Sat"
                    else -> "Sun"
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(Color.White, RoundedCornerShape(14.dp))
                        .border(2.dp, Color(0x66E3B36B), RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GameIcon(eventIconKind(e.id), 26.dp)
                    SpacerW(8.dp)
                    Column(Modifier.weight(1f)) {
                        BasicText(e.title, style = TextStyle(color = Dark, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold))
                        BasicText(e.subtitle, style = TextStyle(color = Muted, fontSize = 11.sp))
                    }
                    BasicText(dayName, style = TextStyle(color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold))
                }
            }
            SpacerH(12.dp)
            SquishyButton("CLOSE", onClick = onClose, height = 46.dp, textSize = 15.dp)
            SpacerH(4.dp)
        }
    }
}

// ---------------------------------------------------------------------- LEADERBOARD

@Composable
fun LeaderboardDialog(prefs: Prefs, onClose: () -> Unit) {
    // first paint: force a fresh sync; then keep it real-time while the dialog is open
    LaunchedEffect(Unit) {
        LiveBoard.sync(prefs, force = true)
        while (true) {
            delay(20_000)
            LiveBoard.sync(prefs)
        }
    }
    val live = LiveBoard.live.value
    val entries = LiveBoard.entries.value
    val me = LiveBoard.me.value
    val myId = prefs.deviceId

    DialogOverlay {
        PanelCard(Modifier.width(350.dp)) {
            DialogTitleText("TOP RACERS")
            SpacerH(6.dp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                // live/offline status dot
                Box(
                    Modifier
                        .size(10.dp)
                        .background(
                            when (live) {
                                true -> Color(0xFF3DDC5F)
                                false -> Color(0xFFC2B49A)
                                null -> Color(0xFFFFC93C)
                            },
                            CircleShape,
                        ),
                )
                SpacerW(8.dp)
                BasicText(
                    when (live) {
                        true -> if (me != null) "LIVE  •  you are #${me.rank}" else "LIVE  •  real players, real ratings"
                        false -> "OFFLINE  •  ranks sync when the server connects"
                        null -> "CONNECTING…"
                    },
                    style = TextStyle(color = Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                )
            }
            SpacerH(10.dp)
            // v3.0 champion podium for the top 3 (like the League podium in the reference game)
            if (live == true && entries.size >= 3) {
                PodiumRow(entries[0], entries[1], entries[2], myId)
                SpacerH(8.dp)
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                when {
                    live == true && entries.isEmpty() -> {
                        BasicText(
                            "The board is fresh — win a level and be the FIRST racer on it!",
                            style = TextStyle(color = Muted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                        )
                    }

                    live == true -> {
                        for (e in entries) {
                            RankRow(
                                rank = e.rank,
                                avatarId = e.avatarId,
                                name = e.name,
                                level = e.maxLevel,
                                rating = e.rating,
                                isYou = e.deviceId == myId,
                            )
                        }
                        // pinned "you" row when outside the top 100
                        if (me != null && entries.none { it.deviceId == myId }) {
                            SpacerH(6.dp)
                            RankRow(
                                rank = me.rank,
                                avatarId = me.avatarId,
                                name = me.name,
                                level = me.maxLevel,
                                rating = me.rating,
                                isYou = true,
                            )
                        }
                    }

                    else -> {
                        // offline / server not deployed yet: honest local card, no fake bots
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 18.dp)
                                .background(Color.White, RoundedCornerShape(16.dp))
                                .border(2.dp, Color(0xFFE3B36B), RoundedCornerShape(16.dp))
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AvatarIcon(prefs.avatarId.intValue, 40.dp)
                                SpacerW(10.dp)
                                Column {
                                    BasicText(
                                        prefs.playerName,
                                        style = TextStyle(color = Dark, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold),
                                    )
                                    BasicText(
                                        "Level ${prefs.maxLevel.intValue}  •  rating ${prefs.rating()}",
                                        style = TextStyle(color = Muted, fontSize = 12.5.sp, fontWeight = FontWeight.Bold),
                                    )
                                }
                            }
                            SpacerH(10.dp)
                            BasicText(
                                if (com.codex.carjam.game.LeaderboardApi.CONFIGURED) {
                                    "Couldn't reach the leaderboard server — check your internet and tap REFRESH."
                                } else {
                                    "Your stats are saved on this device. Real players will race you here once the server connects."
                                },
                                style = TextStyle(color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center),
                            )
                        }
                    }
                }
            }
            SpacerH(10.dp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SquishyButton(
                    "REFRESH",
                    onClick = { LiveBoard.sync(prefs, force = true) },
                    modifier = Modifier.weight(1f),
                    top = Color(0xFF6FB6FF),
                    bottom = Color(0xFF3B7FE0),
                    height = 46.dp,
                    textSize = 14.dp,
                )
                SquishyButton("CLOSE", onClick = onClose, modifier = Modifier.weight(1f), height = 46.dp, textSize = 14.dp)
            }
            SpacerH(4.dp)
        }
    }
}

/** v3.0 visual podium: #1 centre on the high block, #2 left, #3 right. */
@Composable
private fun PodiumRow(
    first: LeaderboardApi.Entry,
    second: LeaderboardApi.Entry,
    third: LeaderboardApi.Entry,
    myId: String,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFF6E0), RoundedCornerShape(16.dp))
            .border(2.dp, Color(0xFFE3B36B), RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom,
    ) {
        PodiumSpot(second, GameIconKind.MEDAL_2, 44.dp, Color(0xFFB9C1C8), myId)
        PodiumSpot(first, GameIconKind.MEDAL_1, 58.dp, Color(0xFFFFC93C), myId)
        PodiumSpot(third, GameIconKind.MEDAL_3, 44.dp, Color(0xFFCD8B4B), myId)
    }
}

@Composable
private fun PodiumSpot(e: LeaderboardApi.Entry, medal: GameIconKind, size: androidx.compose.ui.unit.Dp, accent: Color, myId: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        GameIcon(medal, 22.dp)
        SpacerH(2.dp)
        Box(
            Modifier
                .background(Color.White, CircleShape)
                .border(3.dp, accent, CircleShape)
                .padding(3.dp),
        ) {
            AvatarIcon(e.avatarId, size)
        }
        SpacerH(2.dp)
        BasicText(
            (if (e.deviceId == myId) "YOU" else e.name).uppercase(),
            style = TextStyle(
                color = if (e.deviceId == myId) Color(0xFFB8860B) else Dark,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.ExtraBold,
            ),
            maxLines = 1,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            GameIcon(GameIconKind.TROPHY, 12.dp)
            SpacerW(3.dp)
            BasicText("${e.rating}", style = TextStyle(color = Muted, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold))
        }
    }
}

@Composable
private fun RankRow(rank: Int, avatarId: Int, name: String, level: Int, rating: Int, isYou: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .background(
                if (isYou) Color(0xFFFFF0C2) else if (rank <= 3) Color(0xFFFFFFFF) else Color(0x00FFFFFF),
                RoundedCornerShape(12.dp),
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(42.dp), contentAlignment = Alignment.CenterStart) {
            when (rank) {
                1 -> GameIcon(GameIconKind.MEDAL_1, 30.dp)
                2 -> GameIcon(GameIconKind.MEDAL_2, 30.dp)
                3 -> GameIcon(GameIconKind.MEDAL_3, 30.dp)
                else -> BasicText(
                    "#$rank",
                    style = TextStyle(color = if (isYou) Dark else Muted, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold),
                )
            }
        }
        AvatarIcon(avatarId, 30.dp)
        SpacerW(8.dp)
        Column(Modifier.weight(1f)) {
            BasicText(
                if (isYou) "$name (YOU)" else name,
                style = TextStyle(color = Dark, fontSize = 14.5.sp, fontWeight = if (isYou) FontWeight.ExtraBold else FontWeight.SemiBold),
            )
            BasicText(
                "LV $level",
                style = TextStyle(color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold),
            )
        }
        BasicText(
            "$rating",
            style = TextStyle(color = Muted, fontSize = 14.sp, fontWeight = FontWeight.Bold),
        )
    }
}

// ---------------------------------------------------------------------- DAILY REWARDS

@Composable
fun DailyRewardDialog(prefs: Prefs, onClaimed: (Int) -> Unit, onClose: () -> Unit) {
    val canClaim = DailyRewards.canClaim(prefs)
    val nextDay = DailyRewards.nextDay(prefs)
    DialogOverlay {
        PanelCard(Modifier.width(350.dp)) {
            DialogTitleText("DAILY REWARD")
            SpacerH(6.dp)
            BasicText(
                "Come back every day — day 7 is BIG! Gems on days 3, 6 & 7.",
                style = TextStyle(color = Muted, fontSize = 13.sp),
            )
            SpacerH(12.dp)
            // day tiles: 4 + 3
            for (row in listOf((1..4).toList(), (5..7).toList())) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (day in row) {
                        val prize = DailyRewards.prizes[day - 1]
                        val claimed = day < nextDay || (!canClaim && day <= nextDay)
                        val isNext = canClaim && day == nextDay
                        Column(
                            Modifier
                                .weight(1f)
                                .background(
                                    when {
                                        claimed -> Color(0xFFE8DCC2)
                                        isNext -> Brush.verticalGradient(listOf(Color(0xFFFFE38A), Color(0xFFFFC93C)))
                                        else -> Color.White
                                    }.let { brushOrColor ->
                                        // unify into brush for background()
                                        when (brushOrColor) {
                                            is Brush -> brushOrColor
                                            is Color -> Brush.verticalGradient(listOf(brushOrColor, brushOrColor))
                                            else -> Brush.verticalGradient(listOf(Color.White, Color.White))
                                        }
                                    },
                                    RoundedCornerShape(16.dp),
                                )
                                .border(
                                    2.dp,
                                    if (isNext) Color(0xFFFFB300) else Color(0xFFDDCEB0),
                                    RoundedCornerShape(16.dp),
                                )
                                .padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            BasicText("DAY $day", style = TextStyle(color = if (isNext) Dark else Muted, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold))
                            SpacerH(4.dp)
                            if (claimed) {
                                GameIcon(GameIconKind.SHIELD, 22.dp)
                            } else {
                                CoinIcon(if (day == 7) 30.dp else 22.dp)
                            }
                            SpacerH(4.dp)
                            BasicText("$prize", style = TextStyle(color = Dark, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold))
                            DailyRewards.gemPrizes[day]?.let { g ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    GameIcon(GameIconKind.GEM, 13.dp)
                                    SpacerW(2.dp)
                                    BasicText("+$g", style = TextStyle(color = Color(0xFF0E7BC0), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold))
                                }
                            }
                        }
                    }
                    if (row.size == 3) Box(Modifier.weight(1f))
                }
                SpacerH(8.dp)
            }
            SpacerH(8.dp)
            if (canClaim) {
                val claimCoins = DailyRewards.prizes[(nextDay - 1).coerceIn(0, DailyRewards.prizes.size - 1)]
                val claimGems = DailyRewards.gemPrizes[nextDay] ?: 0
                SquishyButton(if (claimGems > 0) "CLAIM +$claimCoins +$claimGems GEMS" else "CLAIM +$claimCoins", onClick = {
                    val got = DailyRewards.claim(prefs)
                    onClaimed(got)
                }, top = Color(0xFFFFB340), bottom = Color(0xFFE07F00), height = 50.dp, textSize = 17.dp)
            } else {
                SquishyButton("CLAIMED TODAY", onClick = {}, top = Color(0xFF9AA5B1), bottom = Color(0xFF6E7883), height = 50.dp, textSize = 15.dp)
            }
            SpacerH(10.dp)
            SquishyButton("CLOSE", onClick = onClose, top = Color(0xFF9AA5B1), bottom = Color(0xFF6E7883), height = 44.dp, textSize = 14.dp)
            SpacerH(4.dp)
        }
    }
}

// ---------------------------------------------------------------------- shared

@Composable
fun DialogTitleText(text: String) {
    OutlinedTextC(text = text, size = 30.dp, fill = Color.White, outline = Color(0xFF7A4A12))
}
