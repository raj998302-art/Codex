package com.codex.carjam.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codex.carjam.game.render.Painters

/** Big outlined game-style text (native-canvas double pass). */
@Composable
fun OutlinedTextC(
    text: String,
    size: Dp,
    fill: Color = Color.White,
    outline: Color = Color(0xFF2B2B33),
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.height(size * 1.35f).fillMaxWidth()) {
        with(Painters) {
            outlinedText(
                text,
                this@Canvas.size.width / 2f,
                this@Canvas.size.height / 2f,
                size.toPx(),
                fill,
                outline,
            )
        }
    }
}

/** Chunky raised button: dark slab under a glossy face slab that depresses on press. */
@Composable
fun SquishyButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    top: Color = Color(0xFF58D76B),
    bottom: Color = Color(0xFF28A745),
    textSize: Dp = 22.dp,
    height: Dp = 58.dp,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressOffset = if (pressed) 5.dp else 0.dp

    Box(modifier.height(height + 9.dp)) {
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(height)
                .background(bottom, RoundedCornerShape(height / 2)),
        )
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .offset(y = pressOffset)
                .fillMaxWidth()
                .height(height)
                .background(Brush.verticalGradient(listOf(top, bottom)), RoundedCornerShape(height / 2))
                .border(3.dp, Color.White.copy(alpha = 0.55f), RoundedCornerShape(height / 2))
                .clickable(interactionSource = interaction, indication = null) { onClick() },
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                text = label,
                style = TextStyle(
                    color = Color.White,
                    fontSize = textSize.value.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.1.sp,
                ),
            )
        }
    }
}

@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    bg: Color = Color.Black.copy(alpha = 0.5f),
    icon: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier
            .background(bg, RoundedCornerShape(24.dp))
            .border(2.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        icon?.invoke()
        if (icon != null) SpacerW(8.dp)
        BasicText(
            text = text,
            style = TextStyle(color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold),
        )
    }
}

@Composable
fun SpacerW(w: Dp) = Spacer(Modifier.width(w))

@Composable
fun SpacerH(h: Dp) = Spacer(Modifier.height(h))

/** Cartoony gear button like the reference HUD. */
@Composable
fun GearButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .size(62.dp)
            .shadow(4.dp, CircleShape)
            .background(Color.White, CircleShape)
            .border(4.dp, Color(0xFF3B9BFF), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(34.dp)) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val gear = Color(0xFF8A93A0)
            for (i in 0 until 8) {
                withTransform({ rotate(i * 45f, c) }) {
                    drawRoundRect(
                        color = gear,
                        topLeft = Offset(c.x - size.width * 0.10f, c.y - size.height * 0.5f),
                        size = Size(size.width * 0.20f, size.height * 0.26f),
                        cornerRadius = CornerRadius(size.width * 0.05f),
                    )
                }
            }
            drawCircle(gear, radius = size.width * 0.30f, center = c)
            drawCircle(Color.White, radius = size.width * 0.13f, center = c)
        }
    }
}

@Composable
fun CoinIcon(sizeDp: Dp = 26.dp) {
    Canvas(Modifier.size(sizeDp)) {
        val s = size.width // px, provided by DrawScope
        with(Painters) { drawCoin(s / 2f, s / 2f, s * 0.44f) }
    }
}

/** Toon profile avatar (see Painters.drawAvatar for the 8 styles). */
@Composable
fun AvatarIcon(id: Int, sizeDp: Dp = 42.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(sizeDp)) {
        val s = size.width
        with(Painters) { drawAvatar(id, s / 2f, s / 2f, s * 0.48f) }
    }
}

@Composable
fun CoinPill(coins: Int, modifier: Modifier = Modifier, onPlus: () -> Unit = {}) {
    Row(
        modifier
            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
            .border(2.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
            .padding(start = 6.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoinIcon(26.dp)
        SpacerW(8.dp)
        BasicText(
            text = "$coins",
            style = TextStyle(color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold),
        )
        SpacerW(8.dp)
        Box(
            Modifier
                .size(26.dp)
                .background(Color(0xFF3DDC5F), CircleShape)
                .border(2.dp, Color.White.copy(alpha = 0.7f), CircleShape)
                .clickable { onPlus() },
            contentAlignment = Alignment.Center,
        ) {
            BasicText("+", style = TextStyle(color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold))
        }
    }
}

/** Dialog surface shared by win/lose/settings panels. */
@Composable
fun PanelCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier
            .width(340.dp)
            .background(Brush.verticalGradient(listOf(Color(0xFFFFFDF6), Color(0xFFF6E8CC))), RoundedCornerShape(30.dp))
            .border(4.dp, Color(0xFFE3B36B), RoundedCornerShape(30.dp))
            .padding(horizontal = 26.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        content()
    }
}

/** Full-screen dimming overlay that hosts a dialog panel (and swallows outside taps). */
@Composable
fun DialogOverlay(content: @Composable () -> Unit) {
    val swallow = remember { MutableInteractionSource() }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(interactionSource = swallow, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
