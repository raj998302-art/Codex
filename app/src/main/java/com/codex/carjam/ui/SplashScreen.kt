package com.codex.carjam.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import com.codex.carjam.game.render.GameIconKind

/**
 * v2.2 premium splash: the AI-rendered key art (traffic-jam city with the gummy
 * citizens at their station) full-bleed under a cinematic scrim, chunky logo
 * lock-up up top, shimmering progress bar + studio line at the bottom.
 * Offline swaps the bar for the friendly NO-INTERNET gate.
 */
@Composable
fun SplashScreen(
    progress: Float,
    showOfflineGate: Boolean,
    onRetry: () -> Unit,
    onPlayOffline: () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "splash")
    val shine by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Restart),
        label = "shine",
    )

    Box(Modifier.fillMaxSize()) {
        // key art
        Image(
            painter = painterResource(R.drawable.splash_art),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        // cinematic scrim: darker top (logo contrast) + darker bottom (controls)
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xB30A1622),
                            Color(0x220A1622),
                            Color(0x140A1622),
                            Color(0xD9060C14),
                        ),
                    ),
                ),
        )

        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(92.dp))
            OutlinedTextC("CAR JAM", 62.dp, fill = Color.White, outline = Color(0xFF0C2E4E))
            OutlinedTextC("SOLVER", 32.dp, fill = Color(0xFFFFD32E), outline = Color(0xFF7A4A00))
            Spacer(Modifier.height(10.dp))
            BasicText(
                "Unblock the traffic. Seat every passenger.",
                style = TextStyle(color = Color.White.copy(alpha = 0.92f), fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
            )

            Spacer(Modifier.weight(1f))

            if (!showOfflineGate) {
                // shimmering loading bar
                val p = progress.coerceIn(0f, 1f)
                Box(
                    Modifier
                        .fillMaxWidth(0.74f)
                        .height(18.dp)
                        .background(Color(0x40000000), RoundedCornerShape(9.dp))
                        .border(2.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(9.dp)),
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val fillW = (w - 6f) * p
                        if (fillW > 2f) {
                            drawRoundRect(
                                brush = Brush.horizontalGradient(listOf(Color(0xFFFFD32E), Color(0xFFFF9F45))),
                                topLeft = Offset(3f, 3f),
                                size = Size(fillW, h - 6f),
                                cornerRadius = CornerRadius(7f),
                            )
                            val band = 26f
                            val bx = 3f + shine * (fillW + band) - band
                            if (bx > 3f && bx + band < 3f + fillW) {
                                drawRoundRect(
                                    color = Color.White.copy(alpha = 0.35f),
                                    topLeft = Offset(bx, 3f),
                                    size = Size(band, h - 6f),
                                    cornerRadius = CornerRadius(7f),
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                BasicText(
                    "LOADING  ${(p * 100).toInt()}%",
                    style = TextStyle(color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
                )
            } else {
                // offline gate
                Column(
                    Modifier
                        .fillMaxWidth(0.86f)
                        .background(Color(0xFFFFFDF6), RoundedCornerShape(24.dp))
                        .border(3.dp, Color(0xFFE3B36B), RoundedCornerShape(24.dp))
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    GameIcon(GameIconKind.WIFI_OFF, 42.dp)
                    SpacerH(6.dp)
                    BasicText(
                        "NO INTERNET",
                        style = TextStyle(color = Color(0xFF4A3826), fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp),
                    )
                    SpacerH(4.dp)
                    BasicText(
                        "Shop, ads and rankings need a connection — but the jam never stops.",
                        style = TextStyle(color = Color(0xFF8C6A3F), fontSize = 12.5.sp, textAlign = TextAlign.Center),
                    )
                    SpacerH(14.dp)
                    SquishyButton("RETRY", onClick = onRetry, top = Color(0xFF6FB6FF), bottom = Color(0xFF3B7FE0), height = 46.dp, textSize = 15.dp)
                    SpacerH(8.dp)
                    SquishyButton("PLAY OFFLINE", onClick = onPlayOffline, height = 46.dp, textSize = 15.dp, icon = { GameIcon(GameIconKind.GRAD_CAP, 22.dp) })
                }
            }
            Spacer(Modifier.height(16.dp))
            BasicText(
                "CODEX GAMES  •  v2.3",
                style = TextStyle(color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
            )
            Spacer(Modifier.height(30.dp))
        }
    }
}
