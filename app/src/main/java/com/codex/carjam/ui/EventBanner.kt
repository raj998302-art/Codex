package com.codex.carjam.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codex.carjam.game.EventId
import com.codex.carjam.game.GameEvent
import com.codex.carjam.game.render.GameIconKind
import kotlin.math.cos
import kotlin.math.sin

/** Maps each live-ops event to its hand-drawn badge icon. */
fun eventIconKind(id: EventId): GameIconKind = when (id) {
    EventId.COIN_RUSH -> GameIconKind.COIN_STACK
    EventId.MYSTERY_MAYHEM -> GameIconKind.MYSTERY
    EventId.SLOT_SALE -> GameIconKind.PARKING
    EventId.LUCKY_LANE -> GameIconKind.CLOVER
    EventId.DOUBLE_FRIDAY -> GameIconKind.COIN_STACK
    EventId.WEEKEND_FEVER -> GameIconKind.FIRE
}

/**
 * Designed event banner (not a text label!): radiant burst background in the
 * event's accent colour, sparkle cuts, big badge icon and outlined title —
 * the classic live-ops "event is live" card seen in top-grossing puzzlers.
 */
@Composable
fun EventBannerCard(
    event: GameEvent,
    height: Dp,
    modifier: Modifier = Modifier,
    trailingText: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(Color(0xFF233043))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .border(3.dp, Color.White.copy(alpha = 0.55f), shape),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w * 0.82f
            val cy = h * 0.5f
            // accent diagonal wash
            drawRect(
                Brush.linearGradient(
                    listOf(event.accent, event.accent.copy(alpha = 0.55f), Color.Transparent),
                    start = Offset(0f, 0f),
                    end = Offset(w, h),
                ),
            )
            // radiant burst behind the badge
            val rays = 12
            for (i in 0 until rays) {
                val a0 = (i * 2 * Math.PI / rays).toFloat()
                val a1 = ((i + 0.45f) * 2 * Math.PI / rays).toFloat()
                val r0 = h * 0.22f
                val r1 = h * 1.15f
                val p = Path().apply {
                    moveTo(cx + cos(a0) * r0, cy + sin(a0) * r0)
                    lineTo(cx + cos(a0) * r1, cy + sin(a0) * r1)
                    lineTo(cx + cos(a1) * r1, cy + sin(a1) * r1)
                    close()
                }
                drawPath(p, Color.White.copy(alpha = if (i % 2 == 0) 0.10f else 0.05f))
            }
            drawCircle(Color.White.copy(alpha = 0.16f), radius = h * 0.46f, center = Offset(cx, cy))
            // sparkles
            for (k in listOf(0.14f to 0.24f, 0.38f to 0.78f, 0.55f to 0.18f, 0.26f to 0.62f)) {
                Icons.run { star(w * k.first, h * k.second, h * 0.07f, Color.White.copy(alpha = 0.85f)) }
            }
        }

        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                BasicText(
                    event.title,
                    style = TextStyle(color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.8.sp),
                )
                BasicText(
                    event.subtitle,
                    style = TextStyle(color = Color.White.copy(alpha = 0.92f), fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold),
                )
                if (trailingText != null) {
                    BasicText(
                        trailingText,
                        style = TextStyle(color = Color(0xFFFFE38A), fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold),
                    )
                }
            }
            GameIcon(eventIconKind(event.id), 44.dp)
        }
    }
}
