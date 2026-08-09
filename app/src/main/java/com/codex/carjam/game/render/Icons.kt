package com.codex.carjam.game.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.cos
import kotlin.math.sin

/** Icon ids for the procedural icon set — zero emoji, all hand-drawn vector art. */
enum class GameIconKind {
    CART, GIFT, CALENDAR, TROPHY, GRAD_CAP, WIFI_OFF, SHIELD, LOCK, PLAY_AD,
    GEM, MEDAL_1, MEDAL_2, MEDAL_3, FIRE, CLOVER, PARKING, MYSTERY, BOLT, COIN_STACK, STAR,
    GAMEPAD, HAMMER, SHUFFLE,
}

/**
 * AAA-style procedural icons drawn on a unit box of side [s] with top-left at
 * ([l], [t]). Every icon is painted with the same glossy cartoony language as
 * the cars: gradient faces, dark outline pass, white shine cuts.
 */
object Icons {

    private fun DrawScope.outlineCircle(c: Offset, r: Float, w: Float, col: Color) {
        drawCircle(Color(0xFF2B2B33), radius = r, center = c, style = Stroke(w))
        drawCircle(col, radius = r - w * 0.9f, center = c)
    }

    fun DrawScope.cart(l: Float, t: Float, s: Float) {
        val b = Color(0xFF3B7FE0)
        val body = Path().apply {
            moveTo(l + s * 0.16f, t + s * 0.30f)
            lineTo(l + s * 0.28f, t + s * 0.66f)
            lineTo(l + s * 0.80f, t + s * 0.66f)
            lineTo(l + s * 0.88f, t + s * 0.34f)
            close()
        }
        drawPath(body, Color(0xFF2B2B33))
        val inner = Path().apply {
            moveTo(l + s * 0.20f, t + s * 0.34f)
            lineTo(l + s * 0.30f, t + s * 0.62f)
            lineTo(l + s * 0.76f, t + s * 0.62f)
            lineTo(l + s * 0.82f, t + s * 0.38f)
            close()
        }
        drawPath(inner, b)
        drawPath(inner, Brush.verticalGradient(listOf(Color(0xFF6FB6FF), b), startY = t + s * 0.3f, endY = t + s * 0.66f))
        // handle
        drawLine(Color(0xFF2B2B33), Offset(l + s * 0.10f, t + s * 0.22f), Offset(l + s * 0.17f, t + s * 0.31f), strokeWidth = s * 0.07f)
        // wheels
        outlineCircle(Offset(l + s * 0.38f, t + s * 0.78f), s * 0.10f, s * 0.045f, Color.White)
        outlineCircle(Offset(l + s * 0.68f, t + s * 0.78f), s * 0.10f, s * 0.045f, Color.White)
        // basket shine
        drawLine(Color.White.copy(alpha = 0.7f), Offset(l + s * 0.34f, t + s * 0.44f), Offset(l + s * 0.74f, t + s * 0.44f), strokeWidth = s * 0.045f)
    }

    fun DrawScope.gift(l: Float, t: Float, s: Float) {
        val red = Color(0xFFFF5A5A)
        val dark = Color(0xFFD63C3C)
        // box
        drawRoundRect(Color(0xFF2B2B33), Offset(l + s * 0.14f, t + s * 0.34f), Size(s * 0.72f, s * 0.52f), borderRadiusFix(s * 0.10f))
        drawRoundRect(Brush.verticalGradient(listOf(red, dark), startY = t + s * 0.36f, endY = t + s * 0.84f), Offset(l + s * 0.17f, t + s * 0.37f), Size(s * 0.66f, s * 0.46f), borderRadiusFix(s * 0.08f))
        // lid
        drawRoundRect(Color(0xFF2B2B33), Offset(l + s * 0.10f, t + s * 0.26f), Size(s * 0.80f, s * 0.16f), borderRadiusFix(s * 0.08f))
        drawRoundRect(Color(0xFFFF7B7B), Offset(l + s * 0.13f, t + s * 0.29f), Size(s * 0.74f, s * 0.11f), borderRadiusFix(s * 0.05f))
        // ribbon
        drawRect(Color(0xFFFFD93D), Offset(l + s * 0.44f, t + s * 0.29f), Size(s * 0.12f, s * 0.55f))
        // bow
        outlineCircle(Offset(l + s * 0.42f, t + s * 0.20f), s * 0.10f, s * 0.05f, Color(0xFFFFD93D))
        outlineCircle(Offset(l + s * 0.58f, t + s * 0.20f), s * 0.10f, s * 0.05f, Color(0xFFFFD93D))
    }

    private fun borderRadiusFix(r: Float) = androidx.compose.ui.geometry.CornerRadius(r)

    fun DrawScope.calendar(l: Float, t: Float, s: Float) {
        // rings
        for (rx in listOf(0.30f, 0.70f)) {
            drawRoundRect(Color(0xFF2B2B33), Offset(l + s * rx - s * 0.045f, t + s * 0.08f), Size(s * 0.09f, s * 0.20f), borderRadiusFix(s * 0.045f))
        }
        // body
        drawRoundRect(Color(0xFF2B2B33), Offset(l + s * 0.12f, t + s * 0.18f), Size(s * 0.76f, s * 0.68f), borderRadiusFix(s * 0.12f))
        drawRoundRect(Color.White, Offset(l + s * 0.15f, t + s * 0.21f), Size(s * 0.70f, s * 0.62f), borderRadiusFix(s * 0.09f))
        // header
        drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF6FB6FF), Color(0xFF3B7FE0)), startY = t + s * 0.21f, endY = t + s * 0.42f), Offset(l + s * 0.15f, t + s * 0.21f), Size(s * 0.70f, s * 0.21f), borderRadiusFix(s * 0.09f))
        // date squares
        for (r in 0 until 2) {
            for (c in 0 until 3) {
                val hot = (r == 0 && c == 2)
                drawRoundRect(
                    if (hot) Color(0xFFFF4757) else Color(0xFFDDCEB0),
                    Offset(l + s * (0.22f + c * 0.20f), t + s * (0.50f + r * 0.17f)),
                    Size(s * 0.12f, s * 0.12f),
                    borderRadiusFix(s * 0.03f),
                )
            }
        }
    }

    fun DrawScope.trophy(l: Float, t: Float, s: Float) {
        val gold = Color(0xFFFFC93C)
        val deep = Color(0xFFB8860B)
        // cup
        val cup = Path().apply {
            moveTo(l + s * 0.26f, t + s * 0.18f)
            lineTo(l + s * 0.74f, t + s * 0.18f)
            cubicTo(l + s * 0.74f, t + s * 0.52f, l + s * 0.60f, t + s * 0.62f, l + s * 0.50f, t + s * 0.62f)
            cubicTo(l + s * 0.40f, t + s * 0.62f, l + s * 0.26f, t + s * 0.52f, l + s * 0.26f, t + s * 0.18f)
            close()
        }
        // handles
        for (sx in listOf(-1f, 1f)) {
            drawArc(Color(0xFF2B2B33), if (sx < 0) 90f else -90f, 180f, false, Offset(l + s * (if (sx < 0) 0.10f else 0.62f), t + s * 0.18f), Size(s * 0.28f, s * 0.28f), style = Stroke(s * 0.07f))
        }
        drawPath(cup, Color(0xFF2B2B33))
        val inner = Path().apply {
            moveTo(l + s * 0.30f, t + s * 0.22f)
            lineTo(l + s * 0.70f, t + s * 0.22f)
            cubicTo(l + s * 0.70f, t + s * 0.50f, l + s * 0.58f, t + s * 0.57f, l + s * 0.50f, t + s * 0.57f)
            cubicTo(l + s * 0.42f, t + s * 0.57f, l + s * 0.30f, t + s * 0.50f, l + s * 0.30f, t + s * 0.22f)
            close()
        }
        drawPath(inner, Brush.verticalGradient(listOf(Color(0xFFFFE38A), gold, deep), startY = t + s * 0.2f, endY = t + s * 0.6f))
        // stem + base
        drawRoundRect(Color(0xFF2B2B33), Offset(l + s * 0.44f, t + s * 0.58f), Size(s * 0.12f, s * 0.16f), borderRadiusFix(s * 0.03f))
        drawRoundRect(Color(0xFF2B2B33), Offset(l + s * 0.28f, t + s * 0.72f), Size(s * 0.44f, s * 0.12f), borderRadiusFix(s * 0.06f))
        drawRoundRect(gold, Offset(l + s * 0.32f, t + s * 0.74f), Size(s * 0.36f, s * 0.08f), borderRadiusFix(s * 0.04f))
        // star
        star(l + s * 0.50f, t + s * 0.38f, s * 0.11f, Color.White)
    }

    fun DrawScope.star(cx: Float, cy: Float, r: Float, color: Color) {
        val p = Path()
        for (i in 0 until 10) {
            val rr = if (i % 2 == 0) r else r * 0.45f
            val a = -Math.PI / 2 + i * Math.PI / 5
            val x = cx + cos(a).toFloat() * rr
            val y = cy + sin(a).toFloat() * rr
            if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        p.close()
        drawPath(p, color)
    }

    fun DrawScope.gradCap(l: Float, t: Float, s: Float) {
        // board
        val board = Path().apply {
            moveTo(l + s * 0.50f, t + s * 0.16f)
            lineTo(l + s * 0.90f, t + s * 0.36f)
            lineTo(l + s * 0.50f, t + s * 0.56f)
            lineTo(l + s * 0.10f, t + s * 0.36f)
            close()
        }
        drawPath(board, Color(0xFF2B2B33))
        val inner = Path().apply {
            moveTo(l + s * 0.50f, t + s * 0.21f)
            lineTo(l + s * 0.82f, t + s * 0.37f)
            lineTo(l + s * 0.50f, t + s * 0.52f)
            lineTo(l + s * 0.18f, t + s * 0.37f)
            close()
        }
        drawPath(inner, Brush.verticalGradient(listOf(Color(0xFF5B6B8C), Color(0xFF33405C)), startY = t + s * 0.2f, endY = t + s * 0.55f))
        // head band
        val band = Path().apply {
            moveTo(l + s * 0.28f, t + s * 0.48f)
            lineTo(l + s * 0.50f, t + s * 0.60f)
            lineTo(l + s * 0.72f, t + s * 0.48f)
            lineTo(l + s * 0.72f, t + s * 0.62f)
            cubicTo(l + s * 0.72f, t + s * 0.72f, l + s * 0.28f, t + s * 0.72f, l + s * 0.28f, t + s * 0.62f)
            close()
        }
        drawPath(band, Color(0xFF2B2B33))
        // tassel
        drawLine(Color(0xFFFFD93D), Offset(l + s * 0.82f, t + s * 0.37f), Offset(l + s * 0.82f, t + s * 0.70f), strokeWidth = s * 0.045f)
        drawCircle(Color(0xFFFFB300), radius = s * 0.07f, center = Offset(l + s * 0.82f, t + s * 0.74f))
    }

    fun DrawScope.wifiOff(l: Float, t: Float, s: Float) {
        val col = Color(0xFFB0BEC5)
        for (i in 0 until 3) {
            drawArc(col, 200f, 140f, false, Offset(l + s * (0.14f + i * 0.13f), t + s * (0.16f + i * 0.16f)), Size(s * (0.72f - i * 0.26f), s * (0.72f - i * 0.26f)), style = Stroke(s * 0.085f))
        }
        drawCircle(col, radius = s * 0.075f, center = Offset(l + s * 0.5f, t + s * 0.78f))
        // slash
        drawLine(Color(0xFFFF4757), Offset(l + s * 0.18f, t + s * 0.16f), Offset(l + s * 0.84f, t + s * 0.86f), strokeWidth = s * 0.10f)
        drawLine(Color.White.copy(alpha = 0.8f), Offset(l + s * 0.24f, t + s * 0.12f), Offset(l + s * 0.90f, t + s * 0.82f), strokeWidth = s * 0.035f)
    }

    fun DrawScope.shield(l: Float, t: Float, s: Float) {
        val sh = Path().apply {
            moveTo(l + s * 0.50f, t + s * 0.10f)
            cubicTo(l + s * 0.68f, t + s * 0.20f, l + s * 0.82f, t + s * 0.22f, l + s * 0.86f, t + s * 0.22f)
            cubicTo(l + s * 0.86f, t + s * 0.62f, l + s * 0.70f, t + s * 0.80f, l + s * 0.50f, t + s * 0.92f)
            cubicTo(l + s * 0.30f, t + s * 0.80f, l + s * 0.14f, t + s * 0.62f, l + s * 0.14f, t + s * 0.22f)
            cubicTo(l + s * 0.18f, t + s * 0.22f, l + s * 0.32f, t + s * 0.20f, l + s * 0.50f, t + s * 0.10f)
            close()
        }
        drawPath(sh, Color(0xFF2B2B33))
        val inner = Path().apply {
            moveTo(l + s * 0.50f, t + s * 0.17f)
            cubicTo(l + s * 0.64f, t + s * 0.26f, l + s * 0.78f, t + s * 0.28f, l + s * 0.79f, t + s * 0.28f)
            cubicTo(l + s * 0.78f, t + s * 0.60f, l + s * 0.66f, t + s * 0.75f, l + s * 0.50f, t + s * 0.84f)
            cubicTo(l + s * 0.34f, t + s * 0.75f, l + s * 0.22f, t + s * 0.60f, l + s * 0.21f, t + s * 0.28f)
            cubicTo(l + s * 0.22f, t + s * 0.28f, l + s * 0.36f, t + s * 0.26f, l + s * 0.50f, t + s * 0.17f)
            close()
        }
        drawPath(inner, Brush.verticalGradient(listOf(Color(0xFF7BE495), Color(0xFF28A745)), startY = t + s * 0.2f, endY = t + s * 0.85f))
        // check
        drawLine(Color.White, Offset(l + s * 0.36f, t + s * 0.50f), Offset(l + s * 0.46f, t + s * 0.62f), strokeWidth = s * 0.09f)
        drawLine(Color.White, Offset(l + s * 0.46f, t + s * 0.62f), Offset(l + s * 0.68f, t + s * 0.34f), strokeWidth = s * 0.09f)
    }

    fun DrawScope.lock(l: Float, t: Float, s: Float) {
        drawArc(Color(0xFF2B2B33), 180f, 180f, false, Offset(l + s * 0.28f, t + s * 0.14f), Size(s * 0.44f, s * 0.44f), style = Stroke(s * 0.10f))
        drawRoundRect(Color(0xFF2B2B33), Offset(l + s * 0.18f, t + s * 0.38f), Size(s * 0.64f, s * 0.48f), borderRadiusFix(s * 0.12f))
        drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFFFE38A), Color(0xFFFFB300)), startY = t + s * 0.4f, endY = t + s * 0.84f), Offset(l + s * 0.22f, t + s * 0.42f), Size(s * 0.56f, s * 0.41f), borderRadiusFix(s * 0.09f))
        drawCircle(Color(0xFF7A4A12), radius = s * 0.07f, center = Offset(l + s * 0.5f, t + s * 0.58f))
        drawRoundRect(Color(0xFF7A4A12), Offset(l + s * 0.465f, t + s * 0.60f), Size(s * 0.07f, s * 0.14f), borderRadiusFix(s * 0.03f))
    }

    fun DrawScope.playAd(l: Float, t: Float, s: Float) {
        outlineCircle(Offset(l + s * 0.5f, t + s * 0.5f), s * 0.40f, s * 0.08f, Color(0xFF3DDC5F))
        val tri = Path().apply {
            moveTo(l + s * 0.40f, t + s * 0.32f)
            lineTo(l + s * 0.72f, t + s * 0.50f)
            lineTo(l + s * 0.40f, t + s * 0.68f)
            close()
        }
        drawPath(tri, Color.White)
    }

    fun DrawScope.gem(l: Float, t: Float, s: Float) {
        val body = Path().apply {
            moveTo(l + s * 0.25f, t + s * 0.30f)
            lineTo(l + s * 0.40f, t + s * 0.14f)
            lineTo(l + s * 0.60f, t + s * 0.14f)
            lineTo(l + s * 0.75f, t + s * 0.30f)
            lineTo(l + s * 0.50f, t + s * 0.86f)
            close()
        }
        drawPath(body, Color(0xFF2B2B33))
        val face = Path().apply {
            moveTo(l + s * 0.28f, t + s * 0.32f)
            lineTo(l + s * 0.41f, t + s * 0.18f)
            lineTo(l + s * 0.59f, t + s * 0.18f)
            lineTo(l + s * 0.72f, t + s * 0.32f)
            lineTo(l + s * 0.50f, t + s * 0.81f)
            close()
        }
        drawPath(face, Brush.verticalGradient(listOf(Color(0xFF9BE7FF), Color(0xFF38BDF8), Color(0xFF0E7BC0)), startY = t + s * 0.15f, endY = t + s * 0.85f))
        // facets
        drawLine(Color.White.copy(alpha = 0.65f), Offset(l + s * 0.41f, t + s * 0.18f), Offset(l + s * 0.50f, t + s * 0.81f), strokeWidth = s * 0.03f)
        drawLine(Color.White.copy(alpha = 0.65f), Offset(l + s * 0.59f, t + s * 0.18f), Offset(l + s * 0.50f, t + s * 0.81f), strokeWidth = s * 0.03f)
        drawLine(Color.White.copy(alpha = 0.65f), Offset(l + s * 0.28f, t + s * 0.32f), Offset(l + s * 0.72f, t + s * 0.32f), strokeWidth = s * 0.03f)
        // sparkle
        star(l + s * 0.70f, t + s * 0.16f, s * 0.09f, Color.White)
    }

    fun DrawScope.medal(l: Float, t: Float, s: Float, fill: Color, ribbon: Color) {
        // ribbons
        val r1 = Path().apply {
            moveTo(l + s * 0.38f, t + s * 0.10f); lineTo(l + s * 0.50f, t + s * 0.42f); lineTo(l + s * 0.30f, t + s * 0.46f); lineTo(l + s * 0.22f, t + s * 0.14f); close()
        }
        val r2 = Path().apply {
            moveTo(l + s * 0.62f, t + s * 0.10f); lineTo(l + s * 0.50f, t + s * 0.42f); lineTo(l + s * 0.70f, t + s * 0.46f); lineTo(l + s * 0.78f, t + s * 0.14f); close()
        }
        drawPath(r1, ribbon); drawPath(r2, ribbon)
        drawCircle(Color(0xFF2B2B33), radius = s * 0.30f, center = Offset(l + s * 0.5f, t + s * 0.62f))
        drawCircle(fill, radius = s * 0.25f, center = Offset(l + s * 0.5f, t + s * 0.62f))
        drawCircle(Color.White.copy(alpha = 0.55f), radius = s * 0.10f, center = Offset(l + s * 0.43f, t + s * 0.55f))
    }

    fun DrawScope.fire(l: Float, t: Float, s: Float) {
        val flame = Path().apply {
            moveTo(l + s * 0.50f, t + s * 0.10f)
            cubicTo(l + s * 0.62f, t + s * 0.26f, l + s * 0.82f, t + s * 0.38f, l + s * 0.78f, t + s * 0.60f)
            cubicTo(l + s * 0.74f, t + s * 0.82f, l + s * 0.60f, t + s * 0.90f, l + s * 0.50f, t + s * 0.90f)
            cubicTo(l + s * 0.40f, t + s * 0.90f, l + s * 0.26f, t + s * 0.82f, l + s * 0.22f, t + s * 0.60f)
            cubicTo(l + s * 0.19f, t + s * 0.42f, l + s * 0.36f, t + s * 0.30f, l + s * 0.50f, t + s * 0.10f)
            close()
        }
        drawPath(flame, Color(0xFF2B2B33))
        val inner = Path().apply {
            moveTo(l + s * 0.50f, t + s * 0.18f)
            cubicTo(l + s * 0.60f, t + s * 0.32f, l + s * 0.75f, t + s * 0.42f, l + s * 0.72f, t + s * 0.60f)
            cubicTo(l + s * 0.69f, t + s * 0.78f, l + s * 0.58f, t + s * 0.84f, l + s * 0.50f, t + s * 0.84f)
            cubicTo(l + s * 0.42f, t + s * 0.84f, l + s * 0.31f, t + s * 0.78f, l + s * 0.28f, t + s * 0.60f)
            cubicTo(l + s * 0.25f, t + s * 0.45f, l + s * 0.39f, t + s * 0.35f, l + s * 0.50f, t + s * 0.18f)
            close()
        }
        drawPath(inner, Brush.verticalGradient(listOf(Color(0xFFFFE38A), Color(0xFFFF7043), Color(0xFFE64A19)), startY = t + s * 0.2f, endY = t + s * 0.9f))
        drawCircle(Color(0xFFFFF0A8), radius = s * 0.10f, center = Offset(l + s * 0.5f, t + s * 0.66f))
    }

    fun DrawScope.clover(l: Float, t: Float, s: Float) {
        val green = Color(0xFF4CAF50)
        outlineCircle(Offset(l + s * 0.38f, t + s * 0.36f), s * 0.16f, s * 0.06f, green)
        outlineCircle(Offset(l + s * 0.62f, t + s * 0.36f), s * 0.16f, s * 0.06f, green)
        outlineCircle(Offset(l + s * 0.36f, t + s * 0.60f), s * 0.16f, s * 0.06f, green)
        outlineCircle(Offset(l + s * 0.62f, t + s * 0.58f), s * 0.16f, s * 0.06f, green)
        drawLine(Color(0xFF2E7D32), Offset(l + s * 0.52f, t + s * 0.66f), Offset(l + s * 0.66f, t + s * 0.90f), strokeWidth = s * 0.06f)
    }

    fun DrawScope.parking(l: Float, t: Float, s: Float) {
        drawRoundRect(Color(0xFF2B2B33), Offset(l + s * 0.12f, t + s * 0.12f), Size(s * 0.76f, s * 0.76f), borderRadiusFix(s * 0.16f))
        drawRoundRect(Color(0xFF3B7FE0), Offset(l + s * 0.15f, t + s * 0.15f), Size(s * 0.70f, s * 0.70f), borderRadiusFix(s * 0.13f))
        // P
        val sw = s * 0.075f
        drawLine(Color.White, Offset(l + s * 0.34f, t + s * 0.72f), Offset(l + s * 0.34f, t + s * 0.28f), strokeWidth = sw)
        drawLine(Color.White, Offset(l + s * 0.34f, t + s * 0.28f), Offset(l + s * 0.58f, t + s * 0.28f), strokeWidth = sw)
        drawLine(Color.White, Offset(l + s * 0.58f, t + s * 0.28f), Offset(l + s * 0.58f, t + s * 0.48f), strokeWidth = sw)
        drawLine(Color.White, Offset(l + s * 0.58f, t + s * 0.48f), Offset(l + s * 0.34f, t + s * 0.48f), strokeWidth = sw)
    }

    fun DrawScope.mystery(l: Float, t: Float, s: Float) {
        outlineCircle(Offset(l + s * 0.5f, t + s * 0.5f), s * 0.38f, s * 0.08f, Color(0xFF9C6ADE))
        drawArc(Color.White, 195f, 160f, false, Offset(l + s * 0.34f, t + s * 0.22f), Size(s * 0.32f, s * 0.32f), style = Stroke(s * 0.085f))
        drawLine(Color.White, Offset(l + s * 0.5f, t + s * 0.52f), Offset(l + s * 0.5f, t + s * 0.62f), strokeWidth = s * 0.085f)
        drawCircle(Color.White, radius = s * 0.05f, center = Offset(l + s * 0.5f, t + s * 0.74f))
    }

    fun DrawScope.bolt(l: Float, t: Float, s: Float) {
        val b = Path().apply {
            moveTo(l + s * 0.56f, t + s * 0.10f)
            lineTo(l + s * 0.28f, t + s * 0.54f)
            lineTo(l + s * 0.46f, t + s * 0.56f)
            lineTo(l + s * 0.42f, t + s * 0.90f)
            lineTo(l + s * 0.72f, t + s * 0.44f)
            lineTo(l + s * 0.53f, t + s * 0.42f)
            close()
        }
        drawPath(b, Color(0xFF2B2B33))
        val inner = Path().apply {
            moveTo(l + s * 0.55f, t + s * 0.18f)
            lineTo(l + s * 0.36f, t + s * 0.50f)
            lineTo(l + s * 0.50f, t + s * 0.52f)
            lineTo(l + s * 0.47f, t + s * 0.80f)
            lineTo(l + s * 0.65f, t + s * 0.47f)
            lineTo(l + s * 0.52f, t + s * 0.45f)
            close()
        }
        drawPath(inner, Brush.verticalGradient(listOf(Color(0xFFFFF0A8), Color(0xFFFFB300)), startY = t + s * 0.2f, endY = t + s * 0.85f))
    }

    fun DrawScope.coinStack(l: Float, t: Float, s: Float) {
        val gold = Color(0xFFFFD32E)
        val deep = Color(0xFFB8860B)
        fun coin(cx: Float, cy: Float, r: Float) {
            drawCircle(deep, radius = r, center = Offset(cx, cy))
            drawCircle(gold, radius = r * 0.85f, center = Offset(cx, cy))
        }
        coin(l + s * 0.34f, t + s * 0.64f, s * 0.20f)
        coin(l + s * 0.62f, t + s * 0.66f, s * 0.20f)
        coin(l + s * 0.48f, t + s * 0.38f, s * 0.22f)
        star(l + s * 0.48f, t + s * 0.38f, s * 0.10f, deep)
    }

    /** Game controller — used for the Google Play Games sign-in prompt. */
    fun DrawScope.gamepad(l: Float, t: Float, s: Float) {
        val dark = Color(0xFF2B2B33)
        // grips
        drawRoundRect(dark, Offset(l + s * 0.10f, t + s * 0.40f), Size(s * 0.22f, s * 0.42f), borderRadiusFix(s * 0.10f))
        drawRoundRect(dark, Offset(l + s * 0.68f, t + s * 0.40f), Size(s * 0.22f, s * 0.42f), borderRadiusFix(s * 0.10f))
        // body outline + face
        drawRoundRect(dark, Offset(l + s * 0.08f, t + s * 0.24f), Size(s * 0.84f, s * 0.46f), borderRadiusFix(s * 0.20f))
        drawRoundRect(
            Brush.linearGradient(listOf(Color(0xFF9E7BFF), Color(0xFF6A45C8))),
            Offset(l + s * 0.13f, t + s * 0.29f), Size(s * 0.74f, s * 0.36f), borderRadiusFix(s * 0.15f),
        )
        // top sheen
        drawRoundRect(
            Color.White.copy(alpha = 0.35f),
            Offset(l + s * 0.20f, t + s * 0.33f), Size(s * 0.60f, s * 0.05f), borderRadiusFix(s * 0.025f),
        )
        // d-pad (left)
        val dp = Color(0xFF23262E)
        drawRoundRect(dp, Offset(l + s * 0.20f, t + s * 0.415f), Size(s * 0.15f, s * 0.05f), borderRadiusFix(s * 0.02f))
        drawRoundRect(dp, Offset(l + s * 0.25f, t + s * 0.365f), Size(s * 0.05f, s * 0.15f), borderRadiusFix(s * 0.02f))
        // action buttons (right)
        outlineCircle(Offset(l + s * 0.70f, t + s * 0.42f), s * 0.040f, s * 0.020f, Color(0xFF3DDC5F))
        outlineCircle(Offset(l + s * 0.79f, t + s * 0.50f), s * 0.040f, s * 0.020f, Color(0xFFFF5A5A))
        outlineCircle(Offset(l + s * 0.61f, t + s * 0.50f), s * 0.040f, s * 0.020f, Color(0xFF3B9BFF))
    }

    /** Ice-hammer booster: steel head with claw peen and a wooden handle. */
    fun DrawScope.hammer(l: Float, t: Float, s: Float) {
        val iron = Color(0xFF5B6B82)
        val ironLight = Color(0xFFB9C6DA)
        // claw peen (back of the head)
        val claw = Path().apply {
            moveTo(l + s * 0.20f, t + s * 0.24f)
            lineTo(l + s * 0.10f, t + s * 0.34f)
            lineTo(l + s * 0.20f, t + s * 0.40f)
            lineTo(l + s * 0.30f, t + s * 0.34f)
            close()
        }
        drawPath(claw, Color(0xFF2B2B33))
        // head
        drawRoundRect(Color(0xFF2B2B33), Offset(l + s * 0.18f, t + s * 0.22f), Size(s * 0.56f, s * 0.26f), borderRadiusFix(s * 0.07f))
        drawRoundRect(
            Brush.verticalGradient(listOf(ironLight, iron), startY = t + s * 0.24f, endY = t + s * 0.46f),
            Offset(l + s * 0.21f, t + s * 0.25f), Size(s * 0.50f, s * 0.20f), borderRadiusFix(s * 0.05f),
        )
        // striking face
        drawRoundRect(Color(0xFF2B2B33), Offset(l + s * 0.68f, t + s * 0.20f), Size(s * 0.14f, s * 0.30f), borderRadiusFix(s * 0.06f))
        drawRoundRect(Color(0xFFD7E1EE), Offset(l + s * 0.70f, t + s * 0.23f), Size(s * 0.10f, s * 0.24f), borderRadiusFix(s * 0.04f))
        // handle
        drawRoundRect(Color(0xFF2B2B33), Offset(l + s * 0.42f, t + s * 0.44f), Size(s * 0.15f, s * 0.44f), borderRadiusFix(s * 0.06f))
        drawRoundRect(
            Brush.verticalGradient(listOf(Color(0xFFE0A26A), Color(0xFFA86A2E)), startY = t + s * 0.46f, endY = t + s * 0.86f),
            Offset(l + s * 0.45f, t + s * 0.47f), Size(s * 0.09f, s * 0.38f), borderRadiusFix(s * 0.04f),
        )
        // ice chip sparkles
        outlineCircle(Offset(l + s * 0.16f, t + s * 0.62f), s * 0.055f, s * 0.022f, Color(0xFF9ED9F5))
        outlineCircle(Offset(l + s * 0.80f, t + s * 0.62f), s * 0.045f, s * 0.020f, Color(0xFF9ED9F5))
    }

    /** Queue-mix booster: two crossing arrows, heads on the far ends. */
    fun DrawScope.shuffle(l: Float, t: Float, s: Float) {
        val teal = Color(0xFF2ED3C6)
        val tealDark = Color(0xFF0E9E94)
        val w = s * 0.085f
        // arrow 1: top-left to bottom-right
        drawLine(Color(0xFF2B2B33), Offset(l + s * 0.18f, t + s * 0.32f), Offset(l + s * 0.74f, t + s * 0.66f), strokeWidth = w * 1.5f)
        drawLine(tealDark, Offset(l + s * 0.18f, t + s * 0.32f), Offset(l + s * 0.74f, t + s * 0.66f), strokeWidth = w)
        val h1 = Path().apply {
            moveTo(l + s * 0.86f, t + s * 0.72f)
            lineTo(l + s * 0.62f, t + s * 0.74f)
            lineTo(l + s * 0.76f, t + s * 0.50f)
            close()
        }
        drawPath(h1, Color(0xFF2B2B33))
        val h1i = Path().apply {
            moveTo(l + s * 0.82f, t + s * 0.70f)
            lineTo(l + s * 0.66f, t + s * 0.72f)
            lineTo(l + s * 0.75f, t + s * 0.56f)
            close()
        }
        drawPath(h1i, teal)
        // arrow 2: bottom-left to top-right
        drawLine(Color(0xFF2B2B33), Offset(l + s * 0.18f, t + s * 0.68f), Offset(l + s * 0.66f, t + s * 0.28f), strokeWidth = w * 1.5f)
        drawLine(tealDark, Offset(l + s * 0.18f, t + s * 0.68f), Offset(l + s * 0.66f, t + s * 0.28f), strokeWidth = w)
        val h2 = Path().apply {
            moveTo(l + s * 0.80f, t + s * 0.20f)
            lineTo(l + s * 0.60f, t + s * 0.20f)
            lineTo(l + s * 0.70f, t + s * 0.44f)
            close()
        }
        drawPath(h2, Color(0xFF2B2B33))
        val h2i = Path().apply {
            moveTo(l + s * 0.77f, t + s * 0.23f)
            lineTo(l + s * 0.64f, t + s * 0.23f)
            lineTo(l + s * 0.70f, t + s * 0.40f)
            close()
        }
        drawPath(h2i, tealDark)
    }

    /** Routes an icon kind to its painter. */
    fun DrawScope.icon(kind: GameIconKind, l: Float, t: Float, s: Float) {
        when (kind) {
            GameIconKind.CART -> cart(l, t, s)
            GameIconKind.GIFT -> gift(l, t, s)
            GameIconKind.CALENDAR -> calendar(l, t, s)
            GameIconKind.TROPHY -> trophy(l, t, s)
            GameIconKind.GRAD_CAP -> gradCap(l, t, s)
            GameIconKind.WIFI_OFF -> wifiOff(l, t, s)
            GameIconKind.SHIELD -> shield(l, t, s)
            GameIconKind.LOCK -> lock(l, t, s)
            GameIconKind.PLAY_AD -> playAd(l, t, s)
            GameIconKind.GEM -> gem(l, t, s)
            GameIconKind.MEDAL_1 -> medal(l, t, s, Color(0xFFFFD32E), Color(0xFFFF5A5A))
            GameIconKind.MEDAL_2 -> medal(l, t, s, Color(0xFFC7D2E0), Color(0xFF42A5F5))
            GameIconKind.MEDAL_3 -> medal(l, t, s, Color(0xFFE0A26A), Color(0xFFB678E8))
            GameIconKind.FIRE -> fire(l, t, s)
            GameIconKind.CLOVER -> clover(l, t, s)
            GameIconKind.PARKING -> parking(l, t, s)
            GameIconKind.MYSTERY -> mystery(l, t, s)
            GameIconKind.BOLT -> bolt(l, t, s)
            GameIconKind.COIN_STACK -> coinStack(l, t, s)
            GameIconKind.STAR -> star(l + s / 2f, t + s / 2f, s * 0.5f, Color(0xFFFFD32E))
            GameIconKind.GAMEPAD -> gamepad(l, t, s)
            GameIconKind.HAMMER -> hammer(l, t, s)
            GameIconKind.SHUFFLE -> shuffle(l, t, s)
        }
    }
}
