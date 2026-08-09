package com.codex.carjam.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codex.carjam.game.CarColor
import com.codex.carjam.game.Prefs
import com.codex.carjam.game.RideCard
import com.codex.carjam.game.RideCollection
import com.codex.carjam.game.render.GameIconKind
import com.codex.carjam.game.render.Painters
import kotlin.math.min

private val InkC = Color(0xFF4A3826)
private val SubC = Color(0xFF8C6A3F)

/**
 * v3.0 CARDS collection — 24 ride cards. Locked cards show a gray "???"
 * silhouette (like the reference game's No.1…No.70 wall); reaching the
 * card's level unlocks the full-colour ride.
 */
@Composable
fun CollectionDialog(prefs: Prefs, onClose: () -> Unit) {
    val found = RideCollection.unlockedCount(prefs.maxLevel.intValue)
    DialogOverlay {
        PanelCard(Modifier.width(360.dp)) {
            DialogTitleText("CARD COLLECTION")
            SpacerH(6.dp)
            BasicText(
                "Win levels to discover every ride!",
                style = TextStyle(color = SubC, fontSize = 12.sp, fontWeight = FontWeight.Bold),
            )
            SpacerH(10.dp)
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(RideCollection.ALL, key = { it.no }) { card ->
                    RideCardCell(card, locked = prefs.maxLevel.intValue < card.unlockLevel)
                }
            }
            SpacerH(10.dp)
            // progress footer (mirrors "Collection Progress: X/24")
            BasicText(
                "COLLECTION PROGRESS: $found/${RideCollection.TOTAL}",
                style = TextStyle(color = InkC, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp),
            )
            SpacerH(6.dp)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .background(Color.White, RoundedCornerShape(7.dp))
                    .border(2.dp, Color(0xFFE3B36B), RoundedCornerShape(7.dp)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(found / RideCollection.TOTAL.toFloat())
                        .height(14.dp)
                        .background(Color(0xFF8E7BFF), RoundedCornerShape(7.dp)),
                )
            }
            SpacerH(12.dp)
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

@Composable
private fun RideCardCell(card: RideCard, locked: Boolean) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(
                if (locked) Color(0xFFB9AE96) else Color.White,
                RoundedCornerShape(14.dp),
            )
            .border(
                2.dp,
                if (locked) Color(0xFF9A8F78) else Color(0xFFE3B36B),
                RoundedCornerShape(14.dp),
            )
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BasicText(
            "No.${card.no}",
            style = TextStyle(color = if (locked) Color(0xFF6E6350) else SubC, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold),
        )
        SpacerH(2.dp)
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1.25f),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxWidth().height(62.dp)) {
                val sc = min(
                    size.width / (card.type.len + 70f),
                    size.height / (card.type.wid + 46f),
                )
                with(Painters) {
                    if (locked) {
                        drawCar(
                            size.width / 2f,
                            size.height / 2f,
                            0f,
                            card.type,
                            CarColor.GRAY,
                            scale = sc,
                            alpha = 0.55f,
                            mystery = false,
                            arrowVisible = false,
                            variant = 0,
                        )
                    } else {
                        drawCar(
                            size.width / 2f,
                            size.height / 2f,
                            0f,
                            card.type,
                            card.color,
                            scale = sc,
                            arrowVisible = false,
                            variant = card.variant,
                        )
                    }
                }
            }
            if (locked) {
                BasicText(
                    "???",
                    style = TextStyle(color = Color(0xFF5C5342), fontSize = 16.sp, fontWeight = FontWeight.Black),
                )
            }
        }
        SpacerH(2.dp)
        BasicText(
            if (locked) "L${card.unlockLevel}" else card.title,
            style = TextStyle(
                color = if (locked) Color(0xFF6E6350) else InkC,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            ),
            maxLines = 1,
        )
        if (locked) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GameIcon(GameIconKind.LOCK, 10.dp)
                SpacerW(3.dp)
                BasicText("LVL ${card.unlockLevel}", style = TextStyle(color = Color(0xFF6E6350), fontSize = 8.sp, fontWeight = FontWeight.Bold))
            }
        }
    }
}
