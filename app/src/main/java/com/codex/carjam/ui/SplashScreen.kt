package com.codex.carjam.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codex.carjam.game.CarColor
import com.codex.carjam.game.CarType
import com.codex.carjam.game.render.GameIconKind
import com.codex.carjam.game.render.Painters

/**
 * Brand splash: logo lock-up, bobbing toon cars, animated loading bar.
 * When the device is offline the loading bar is replaced by a friendly
 * "no internet" gate with RETRY / PLAY OFFLINE.
 */
@Composable
fun SplashScreen(
    progress: Float,
    showOfflineGate: Boolean,
    onRetry: () -> Unit,
    onPlayOffline: () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "splash")
    val bob by transition.animateFloat(
        initialValue = 0f,
        targetValue = (Math.PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(tween(3600), RepeatMode.Restart),
        label = "bob",
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF19B6E0), Color(0xFF0B3D6E)))),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val y = h * 0.62f
            with(Painters) {
                drawCar(w * 0.22f, y + kotlin.math.sin(bob) * 14f, -28f, CarType.VAN, CarColor.entries[1], scale = w / 1050f)
                drawCar(w * 0.80f, y - h * 0.05f + kotlin.math.sin(bob + 2f) * 14f, 24f, CarType.SEDAN, CarColor.entries[0], scale = w / 1150f)
                drawCar(w * 0.52f, y + h * 0.14f + kotlin.math.sin(bob + 4f) * 12f, 166f, CarType.BUS, CarColor.entries[2 % CarColor.entries.size], scale = w / 1200f)
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(120.dp))
            OutlinedTextC("CAR JAM", 66.dp, fill = Color.White, outline = Color(0xFF0C2E4E))
            OutlinedTextC("SOLVER", 32.dp, fill = Color(0xFFFFD32E), outline = Color(0xFF7A4A00))
            Spacer(Modifier.height(10.dp))
            BasicText(
                "Unblock the traffic. Seat every passenger.",
                style = TextStyle(color = Color.White.copy(alpha = 0.85f), fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
            )

            Spacer(Modifier.weight(1f))

            if (!showOfflineGate) {
                // loading bar
                Box(
                    Modifier
                        .fillMaxWidth(0.72f)
                        .height(16.dp)
                        .background(Color(0x33000000), RoundedCornerShape(8.dp))
                        .border(2.dp, Color.White.copy(alpha = 0.55f), RoundedCornerShape(8.dp)),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .height(16.dp)
                            .background(
                                Brush.horizontalGradient(listOf(Color(0xFFFFD32E), Color(0xFFFF9F45))),
                                RoundedCornerShape(8.dp),
                            ),
                    )
                }
                Spacer(Modifier.height(12.dp))
                BasicText(
                    "loading…",
                    style = TextStyle(color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp),
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
            Spacer(Modifier.height(64.dp))
        }
    }
}
