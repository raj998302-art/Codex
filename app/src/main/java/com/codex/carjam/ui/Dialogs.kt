package com.codex.carjam.ui

import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codex.carjam.R
import com.codex.carjam.game.Prefs
import com.codex.carjam.game.render.GameIconKind

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
            ToggleRow("Vibration", prefs.vibrateOn.value) { prefs.setVibrate(it) }
            SpacerH(14.dp)
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
fun WinDialog(level: Int, coinsEarned: Int, onNext: () -> Unit, onHome: () -> Unit) {
    DialogOverlay {
        PanelCard {
            DialogTitle("LEVEL COMPLETE!", fill = Color(0xFFFFC93C), outline = Color(0xFF7A3E00))
            SpacerH(8.dp)
            BasicText(
                text = "Level $level cleared",
                style = TextStyle(color = Color(0xFF8C6A3F), fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
            )
            SpacerH(12.dp)
            // showroom celebration strip (the 3D hero ride)
            Image(
                painter = painterResource(R.drawable.hero_car),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFFF7FBFF), Color(0xFFD9EAFB)))),
                contentScale = ContentScale.Fit,
            )
            SpacerH(12.dp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                CoinIcon(34.dp)
                SpacerW(10.dp)
                BasicText(
                    text = "+$coinsEarned",
                    style = TextStyle(color = Color(0xFF4A3826), fontSize = 34.sp, fontWeight = FontWeight.ExtraBold),
                )
            }
            SpacerH(18.dp)
            SquishyButton("NEXT LEVEL", onClick = onNext)
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
