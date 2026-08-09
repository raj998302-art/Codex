package com.codex.carjam.ui

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codex.carjam.game.Achievements
import com.codex.carjam.game.DailyMissions
import com.codex.carjam.game.Prefs
import com.codex.carjam.game.render.GameIconKind

private val QDark = Color(0xFF4A3826)
private val QMuted = Color(0xFF8C6A3F)

// ---------------------------------------------------------------- weekly prizes

data class SeasonPrize(val weekId: Int, val rank: Int, val coins: Int, val gems: Int)

/** Rank → (coins, gems) payout for a finished season. */
fun seasonPrizeForRank(rank: Int): Pair<Int, Int> = when {
    rank == 1 -> 500 to 25
    rank <= 3 -> 250 to 10
    rank <= 10 -> 100 to 5
    rank <= 25 -> 40 to 2
    else -> 10 to 0
}

/** Celebrates a finished week's placement with its auto-credited reward. */
@Composable
fun SeasonPrizeDialog(prize: SeasonPrize, onClose: () -> Unit) {
    DialogOverlay {
        PanelCard(Modifier.width(350.dp)) {
            DialogTitleText("SEASON PRIZE!")
            SpacerH(6.dp)
            GameIcon(GameIconKind.TROPHY, 46.dp)
            SpacerH(6.dp)
            BasicText(
                "You finished #${prize.rank} last week!",
                style = TextStyle(color = QMuted, fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
            )
            SpacerH(12.dp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                CoinIcon(26.dp)
                SpacerW(8.dp)
                BasicText("+${prize.coins}", style = TextStyle(color = QDark, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold))
                if (prize.gems > 0) {
                    SpacerW(16.dp)
                    GameIcon(GameIconKind.GEM, 24.dp)
                    SpacerW(6.dp)
                    BasicText("+${prize.gems}", style = TextStyle(color = Color(0xFF0E7BC0), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold))
                }
            }
            SpacerH(14.dp)
            SquishyButton("COLLECT", onClick = onClose, height = 46.dp, textSize = 15.dp)
            SpacerH(4.dp)
        }
    }
}

// ---------------------------------------------------------------- QUESTS (missions + awards)

@Composable
fun QuestsDialog(prefs: Prefs, onClaimed: () -> Unit, onClose: () -> Unit) {
    var tab by remember { mutableStateOf(0) } // 0 = missions, 1 = awards
    val epochDay = DailyMissions.todayEpoch()
    val missions = DailyMissions.todaysMissions(epochDay)

    DialogOverlay {
        PanelCard(Modifier.width(360.dp)) {
            DialogTitleText("QUESTS")
            SpacerH(8.dp)
            // tab switcher
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SquishyButton(
                    "MISSIONS",
                    onClick = { tab = 0 },
                    modifier = Modifier.weight(1f),
                    top = if (tab == 0) Color(0xFF6FB6FF) else Color(0xFFC7BBA6),
                    bottom = if (tab == 0) Color(0xFF3B7FE0) else Color(0xFFA89B86),
                    height = 38.dp,
                    textSize = 13.dp,
                    icon = { GameIcon(GameIconKind.BOLT, 18.dp) },
                )
                SquishyButton(
                    "AWARDS",
                    onClick = { tab = 1 },
                    modifier = Modifier.weight(1f),
                    top = if (tab == 1) Color(0xFF6FB6FF) else Color(0xFFC7BBA6),
                    bottom = if (tab == 1) Color(0xFF3B7FE0) else Color(0xFFA89B86),
                    height = 38.dp,
                    textSize = 13.dp,
                    icon = { GameIcon(GameIconKind.MEDAL_1, 18.dp) },
                )
            }
            SpacerH(10.dp)
            Column(
                Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                if (tab == 0) {
                    BasicText(
                        "New missions every day — finish all 3!",
                        style = TextStyle(color = QMuted, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold),
                    )
                    SpacerH(8.dp)
                    missions.forEachIndexed { i, m ->
                        MissionCard(
                            prefs = prefs,
                            epochDay = epochDay,
                            mission = m,
                            index = i,
                            onClaimed = onClaimed,
                        )
                        SpacerH(8.dp)
                    }
                } else {
                    BasicText(
                        "One-time milestones — the medals stay forever.",
                        style = TextStyle(color = QMuted, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold),
                    )
                    SpacerH(8.dp)
                    for (a in Achievements.all) {
                        AwardCard(prefs = prefs, award = a, onClaimed = onClaimed)
                        SpacerH(8.dp)
                    }
                }
            }
            SpacerH(8.dp)
            SquishyButton(
                "CLOSE",
                onClick = onClose,
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
private fun RewardChips(coins: Int, gems: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CoinIcon(18.dp)
        SpacerW(4.dp)
        BasicText("+$coins", style = TextStyle(color = QDark, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold))
        if (gems > 0) {
            SpacerW(8.dp)
            GameIcon(GameIconKind.GEM, 16.dp)
            SpacerW(4.dp)
            BasicText("+$gems", style = TextStyle(color = Color(0xFF0E7BC0), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold))
        }
    }
}

@Composable
private fun MissionCard(prefs: Prefs, epochDay: Int, mission: DailyMissions.Mission, index: Int, onClaimed: () -> Unit) {
    val prog = DailyMissions.progress(prefs, epochDay, mission)
    val done = prog >= mission.target
    val claimable = DailyMissions.canClaim(prefs, epochDay, mission, index)
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(2.dp, if (done) Color(0xFF3DDC5F) else Color(0xFFE3B36B), RoundedCornerShape(16.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GameIcon(mission.icon, 30.dp)
        SpacerW(8.dp)
        Column(Modifier.weight(1f)) {
            BasicText(mission.title, style = TextStyle(color = QDark, fontSize = 14.5.sp, fontWeight = FontWeight.ExtraBold))
            SpacerH(4.dp)
            // progress bar
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .background(Color(0xFFEADFCD), RoundedCornerShape(5.dp)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(if (mission.target > 0) prog.toFloat() / mission.target else 0f)
                        .height(10.dp)
                        .background(
                            Brush.horizontalGradient(listOf(Color(0xFF6FEE85), Color(0xFF1FA94F))),
                            RoundedCornerShape(5.dp),
                        ),
                )
            }
            SpacerH(2.dp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText(
                    "$prog / ${mission.target}",
                    style = TextStyle(color = QMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f),
                )
                RewardChips(mission.coins, mission.gems)
            }
        }
        SpacerW(8.dp)
        if (claimable) {
            SquishyButton(
                "CLAIM",
                onClick = {
                    DailyMissions.claim(prefs, epochDay, mission, index)
                    onClaimed()
                },
                modifier = Modifier.width(76.dp),
                top = Color(0xFF6FB6FF),
                bottom = Color(0xFF3B7FE0),
                height = 34.dp,
                textSize = 12.dp,
            )
        } else {
            BasicText(
                if (done) "DONE" else "",
                style = TextStyle(color = Color(0xFF2FA84F), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center),
                modifier = Modifier.width(76.dp),
            )
        }
    }
}

@Composable
private fun AwardCard(prefs: Prefs, award: Achievements.Award, onClaimed: () -> Unit) {
    val cur = award.value(prefs).coerceAtMost(award.target)
    val done = cur >= award.target
    val claimed = prefs.achievementsClaimed.value.contains(award.id)
    val claimable = Achievements.canClaim(prefs, award)
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (claimed) Color(0xFFF3ECE0) else Color.White, RoundedCornerShape(16.dp))
            .border(2.dp, if (claimed) Color(0xFFC7BBA6) else if (done) Color(0xFF3DDC5F) else Color(0xFFE3B36B), RoundedCornerShape(16.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GameIcon(award.icon, 30.dp)
        SpacerW(8.dp)
        Column(Modifier.weight(1f)) {
            BasicText(award.title, style = TextStyle(color = QDark, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold))
            BasicText(award.desc, style = TextStyle(color = QMuted, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold))
            SpacerH(4.dp)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(Color(0xFFEADFCD), RoundedCornerShape(4.dp)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(if (award.target > 0) cur.toFloat() / award.target else 0f)
                        .height(8.dp)
                        .background(
                            Brush.horizontalGradient(listOf(Color(0xFFFFD32E), Color(0xFFFF9F45))),
                            RoundedCornerShape(4.dp),
                        ),
                )
            }
            SpacerH(2.dp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText(
                    "$cur / ${award.target}",
                    style = TextStyle(color = QMuted, fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f),
                )
                RewardChips(award.coins, award.gems)
            }
        }
        SpacerW(8.dp)
        if (claimable) {
            SquishyButton(
                "CLAIM",
                onClick = {
                    Achievements.claim(prefs, award)
                    onClaimed()
                },
                modifier = Modifier.width(76.dp),
                top = Color(0xFF6FB6FF),
                bottom = Color(0xFF3B7FE0),
                height = 34.dp,
                textSize = 12.dp,
            )
        } else {
            BasicText(
                if (claimed) "OWNED" else "",
                style = TextStyle(color = if (claimed) Color(0xFFA89B86) else Color(0xFF2FA84F), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center),
                modifier = Modifier.width(76.dp),
            )
        }
    }
}
