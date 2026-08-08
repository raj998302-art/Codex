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
import com.codex.carjam.game.DailyRewards
import com.codex.carjam.game.Events
import com.codex.carjam.game.Leaderboard
import com.codex.carjam.game.Prefs
import com.codex.carjam.monetize.AdsManager
import com.codex.carjam.monetize.BillingManager
import java.util.Calendar

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
                            SquishyButton("ACTIVE ✓", onClick = {}, top = Color(0xFF58D76B), bottom = Color(0xFF28A745), height = 44.dp, textSize = 15.dp)
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
                    "FREE +50 COINS ▶ AD",
                    onClick = {
                        ads.showRewarded(activity = activity, onReward = { prefs.addCoins(50) })
                    },
                    top = Color(0xFFB678E8),
                    bottom = Color(0xFF8A45C4),
                    height = 44.dp,
                    textSize = 14.dp,
                )

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
            SquishyButton("CLOSE", onClick = onClose, top = Color(0xFF9AA5B1), bottom = Color(0xFF6E7883), height = 46.dp, textSize = 15.dp)
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
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(ev.accent, ev.accent.copy(alpha = 0.75f))), RoundedCornerShape(20.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BasicText(ev.emoji, style = TextStyle(fontSize = 38.sp))
                BasicText(ev.title, style = TextStyle(color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold))
                BasicText(ev.subtitle, style = TextStyle(color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp), modifier = Modifier.padding(top = 2.dp))
                SpacerH(6.dp)
                BasicText(
                    "Ends in ${formatHMS(Events.msUntilMidnight())}",
                    style = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold),
                )
            }
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
                    BasicText(e.emoji, style = TextStyle(fontSize = 18.sp))
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
    val (board, yourRank) = Leaderboard.weeklyBoard(Events.weekId(), prefs.rating(), prefs.playerName)
    DialogOverlay {
        PanelCard(Modifier.width(350.dp)) {
            DialogTitleText("WEEKLY RANK")
            SpacerH(6.dp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText("🏆", style = TextStyle(fontSize = 24.sp))
                SpacerW(8.dp)
                BasicText(
                    "You are #$yourRank  •  ends in ${formatDH(Events.msUntilNextWeek())}",
                    style = TextStyle(color = Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                )
            }
            SpacerH(10.dp)
            Column(
                Modifier
                    .fillMaxWidth()
                    .height(330.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                for (e in board) {
                    val medal = when (e.rank) { 1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> null }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .background(
                                if (e.isYou) Color(0xFFFFF0C2) else if (e.rank <= 3) Color(0xFFFFFFFF) else Color(0x00FFFFFF),
                                RoundedCornerShape(12.dp),
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BasicText(
                            medal ?: "#${e.rank}",
                            style = TextStyle(color = if (e.isYou) Dark else Muted, fontSize = if (medal != null) 18.sp else 13.sp, fontWeight = FontWeight.ExtraBold),
                            modifier = Modifier.width(42.dp),
                        )
                        BasicText(
                            e.name,
                            style = TextStyle(color = Dark, fontSize = 15.sp, fontWeight = if (e.isYou) FontWeight.ExtraBold else FontWeight.SemiBold),
                            modifier = Modifier.weight(1f),
                        )
                        BasicText(
                            "${e.score}",
                            style = TextStyle(color = Muted, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                        )
                    }
                }
            }
            SpacerH(8.dp)
            BasicText(
                "Top 3 this week win 🪙500 / 🪙250 / 🪙150",
                style = TextStyle(color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center),
                modifier = Modifier.fillMaxWidth(),
            )
            SpacerH(8.dp)
            SquishyButton("CLOSE", onClick = onClose, height = 46.dp, textSize = 15.dp)
            SpacerH(4.dp)
        }
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
                "Come back every day — day 7 is BIG!",
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
                                BasicText("✓", style = TextStyle(color = Color(0xFF4CAF50), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold))
                            } else {
                                CoinIcon(if (day == 7) 30.dp else 22.dp)
                            }
                            SpacerH(4.dp)
                            BasicText("$prize", style = TextStyle(color = Dark, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold))
                        }
                    }
                    if (row.size == 3) Box(Modifier.weight(1f))
                }
                SpacerH(8.dp)
            }
            SpacerH(8.dp)
            if (canClaim) {
                SquishyButton("CLAIM +${DailyRewards.prizes[(nextDay - 1)]}", onClick = {
                    val got = DailyRewards.claim(prefs)
                    onClaimed(got)
                }, top = Color(0xFFFFB340), bottom = Color(0xFFE07F00), height = 50.dp, textSize = 17.dp)
            } else {
                SquishyButton("CLAIMED TODAY ✓", onClick = {}, top = Color(0xFF9AA5B1), bottom = Color(0xFF6E7883), height = 50.dp, textSize = 15.dp)
            }
            SpacerH(10.dp)
            SquishyButton("CLOSE", onClick = onClose, top = Color(0xFF9AA5B1), bottom = Color(0xFF6E7883), height = 44.dp, textSize = 14.dp)
            SpacerH(4.dp)
        }
    }
}

// ---------------------------------------------------------------------- shared

@Composable
private fun DialogTitleText(text: String) {
    OutlinedTextC(text = text, size = 30.dp, fill = Color.White, outline = Color(0xFF7A4A12))
}
