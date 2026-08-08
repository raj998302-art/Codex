package com.codex.carjam.game.render

import android.graphics.Typeface
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.codex.carjam.game.BoardAnim
import com.codex.carjam.game.CarColor
import com.codex.carjam.game.CarEnt
import com.codex.carjam.game.CarPhase
import com.codex.carjam.game.CarType
import com.codex.carjam.game.Deco
import com.codex.carjam.game.Dim
import com.codex.carjam.game.GameEngine
import com.codex.carjam.game.GameResult
import com.codex.carjam.game.LevelTheme
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

/**
 * Hand-tuned Canvas painters. Everything is drawn in design units (see [Dim]);
 * the caller wraps scene() in the letterbox transform.
 */
object Painters {

    private val fillPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = android.graphics.Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }

    private val strokePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = android.graphics.Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        style = android.graphics.Paint.Style.STROKE
        strokeJoin = android.graphics.Paint.Join.ROUND
        strokeCap = android.graphics.Paint.Cap.ROUND
    }

    // ------------------------------------------------------------------ text

    /** Chunky game text: dark outline pass + coloured fill pass, like the reference UI. */
    fun DrawScope.outlinedText(
        text: String,
        cx: Float,
        cy: Float,
        size: Float,
        fill: Color = Color.White,
        outline: Color = Color(0xFF2B2B33),
        strokeMul: Float = 0.16f,
    ) {
        fillPaint.textSize = size
        fillPaint.color = fill.toArgb()
        strokePaint.textSize = size
        strokePaint.color = outline.toArgb()
        strokePaint.strokeWidth = size * strokeMul
        val baseline = cy + size * 0.36f
        val canvas = drawContext.canvas.nativeCanvas
        canvas.drawText(text, cx, baseline, strokePaint)
        canvas.drawText(text, cx, baseline, fillPaint)
    }

    // ------------------------------------------------------------------ scene

    fun DrawScope.scene(engine: GameEngine) {
        val theme = engine.spec.theme
        val ms = engine.ms
        drawSky(theme)
        drawThemeDeco(theme, ms)
        drawSlotBand(theme, engine)
        drawRoad(theme)
        drawArena(theme)
        drawStationSign(theme, engine.remainingPassengers())
        drawQueue(engine)

        // trails first (under the moving car)
        for (car in engine.cars) {
            if (car.phase == CarPhase.EXITING && car.trail.size > 1) drawTrail(car, ms)
        }

        // arena cars, painter's depth order
        val arenaCars = engine.cars.filter { it.phase == CarPhase.IN_ARENA }.sortedBy { it.spec.y }
        for (car in arenaCars) drawCarEntity(car, ms)

        // parked + moving cars with their seated passengers
        for (car in engine.cars) {
            when (car.phase) {
                CarPhase.PARKED, CarPhase.EXITING, CarPhase.DEPARTING -> {
                    drawCarEntity(car, ms)
                    for (i in 0 until car.seatsFilled) {
                        val seat = engine.seatPos(car, i)
                        val pop = car.seatPops[i]
                        var s = 0.62f
                        if (pop != null && ms - pop < 400f) {
                            s *= 1f + 0.45f * exp(-(ms - pop) / 130f) * sin((ms - pop) / 32f)
                        }
                        drawPassenger(seat.x, seat.y, car.color, s)
                    }
                }

                else -> Unit
            }
        }

        // passengers in flight
        for (b in engine.boardAnims) drawBoarding(b)

        // coins
        for (c in engine.coins) {
            if (c.landed || engine.ms < c.birthMs + c.delay) continue
            val p = c.pos()
            drawCoin(p.x, p.y, 24f)
        }

        if (engine.result == GameResult.WON) {
            for (c in engine.confetti) {
                withTransform({ translate(c.x, c.y); rotate(c.rot, Offset.Zero) }) {
                    drawRect(
                        color = c.color,
                        topLeft = Offset(-c.w / 2, -c.h / 2),
                        size = Size(c.w, c.h),
                    )
                }
            }
        }
    }

    // ------------------------------------------------------------------ zones

    private fun DrawScope.drawSky(theme: LevelTheme) {
        drawRect(
            brush = Brush.verticalGradient(listOf(theme.skyTop, theme.skyBottom), startY = 0f, endY = Dim.STATION_BOTTOM),
            topLeft = Offset.Zero,
            size = Size(Dim.VW, Dim.STATION_BOTTOM),
        )
    }

    private fun DrawScope.drawSlotBand(theme: LevelTheme, engine: GameEngine) {
        drawRect(
            brush = Brush.verticalGradient(
                listOf(theme.slotBandTop, theme.slotBandBottom),
                startY = Dim.SLOT_TOP,
                endY = Dim.SLOT_BOTTOM,
            ),
            topLeft = Offset(0f, Dim.SLOT_TOP),
            size = Size(Dim.VW, Dim.SLOT_BOTTOM - Dim.SLOT_TOP),
        )
        // soft shadow where band meets road
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color.Black.copy(alpha = 0.22f), Color.Transparent),
                startY = Dim.SLOT_BOTTOM - 6f,
                endY = Dim.SLOT_BOTTOM + 30f,
            ),
            topLeft = Offset(0f, Dim.SLOT_BOTTOM - 6f),
            size = Size(Dim.VW, 36f),
        )

        val occupied = HashSet<Int>()
        for (c in engine.cars) if (c.slotIdx >= 0 && c.phase != CarPhase.GONE) occupied.add(c.slotIdx)
        val centers = engine.slotCenters()
        val spacing = if (centers.size > 1) centers[1].x - centers[0].x else 200f
        val halfW = min(66f, spacing * 0.44f)
        val halfH = 108f
        val phase = -(engine.ms / 40f)
        centers.forEachIndexed { i, c ->
            if (!occupied.contains(i)) {
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.10f),
                    topLeft = Offset(c.x - halfW, c.y - halfH),
                    size = Size(halfW * 2, halfH * 2),
                    cornerRadius = CornerRadius(26f),
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.72f),
                    topLeft = Offset(c.x - halfW, c.y - halfH),
                    size = Size(halfW * 2, halfH * 2),
                    cornerRadius = CornerRadius(26f),
                    style = Stroke(width = 7f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(24f, 20f), phase)),
                )
            }
        }
    }

    private fun DrawScope.drawRoad(theme: LevelTheme) {
        drawRect(
            color = theme.road,
            topLeft = Offset(0f, Dim.ROAD_TOP),
            size = Size(Dim.VW, Dim.ROAD_BOTTOM - Dim.ROAD_TOP),
        )
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color.Black.copy(alpha = 0.20f), Color.Transparent),
                startY = Dim.ROAD_TOP,
                endY = Dim.ROAD_TOP + 26f,
            ),
            topLeft = Offset(0f, Dim.ROAD_TOP),
            size = Size(Dim.VW, 26f),
        )
        // centre guide dashes
        val y = (Dim.ROAD_TOP + Dim.ROAD_BOTTOM) / 2f
        var x = 20f
        while (x < Dim.VW - 20f) {
            drawRoundRect(
                color = Color.White.copy(alpha = 0.5f),
                topLeft = Offset(x, y - 5f),
                size = Size(56f, 10f),
                cornerRadius = CornerRadius(5f),
            )
            x += 110f
        }
    }

    private fun DrawScope.drawArena(theme: LevelTheme) {
        drawRoundRect(
            color = theme.arenaEdge,
            topLeft = Offset(Dim.ARENA_LEFT, Dim.ARENA_TOP),
            size = Size(Dim.ARENA_RIGHT - Dim.ARENA_LEFT, Dim.ARENA_BOTTOM - Dim.ARENA_TOP),
            cornerRadius = CornerRadius(30f),
        )
        drawRoundRect(
            color = theme.arenaBg,
            topLeft = Offset(Dim.ARENA_LEFT + 10f, Dim.ARENA_TOP + 10f),
            size = Size(Dim.ARENA_RIGHT - Dim.ARENA_LEFT - 20f, Dim.ARENA_BOTTOM - Dim.ARENA_TOP - 20f),
            cornerRadius = CornerRadius(22f),
        )
        // inner top shadow
        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(Color.Black.copy(alpha = 0.10f), Color.Transparent),
                startY = Dim.ARENA_TOP + 10f,
                endY = Dim.ARENA_TOP + 70f,
            ),
            topLeft = Offset(Dim.ARENA_LEFT + 10f, Dim.ARENA_TOP + 10f),
            size = Size(Dim.ARENA_RIGHT - Dim.ARENA_LEFT - 20f, 60f),
            cornerRadius = CornerRadius(22f),
        )
    }

    private fun DrawScope.drawStationSign(theme: LevelTheme, remaining: Int) {
        // post
        drawRoundRect(
            color = theme.signPost,
            topLeft = Offset(Dim.SIGN_X - 11f, Dim.SIGN_Y + 60f),
            size = Size(22f, 150f),
            cornerRadius = CornerRadius(8f),
        )
        // board
        drawRoundRect(
            color = theme.signPost,
            topLeft = Offset(Dim.SIGN_X - 132f, Dim.SIGN_Y - 96f),
            size = Size(264f, 176f),
            cornerRadius = CornerRadius(28f),
        )
        drawRoundRect(
            color = theme.signBoard,
            topLeft = Offset(Dim.SIGN_X - 121f, Dim.SIGN_Y - 85f),
            size = Size(242f, 154f),
            cornerRadius = CornerRadius(20f),
        )
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.14f), Color.Transparent)),
            topLeft = Offset(Dim.SIGN_X - 121f, Dim.SIGN_Y - 85f),
            size = Size(242f, 60f),
            cornerRadius = CornerRadius(20f),
        )
        outlinedText("$remaining", Dim.SIGN_X, Dim.SIGN_Y - 18f, 64f)
        outlinedText("Queue", Dim.SIGN_X, Dim.SIGN_Y + 44f, 40f, fill = Color(0xFFFFE071))
    }

    private fun DrawScope.drawQueue(engine: GameEngine) {
        // tunnel at the right edge where passengers stream in
        drawCircle(color = engine.spec.theme.signPost, radius = 66f, center = Offset(Dim.VW - 8f, Dim.QUEUE_Y + 6f))
        drawCircle(color = Color.Black.copy(alpha = 0.55f), radius = 46f, center = Offset(Dim.VW - 8f, Dim.QUEUE_Y + 6f))
        drawCircle(
            color = Color.White.copy(alpha = 0.35f),
            radius = 58f,
            center = Offset(Dim.VW - 8f, Dim.QUEUE_Y + 6f),
            style = Stroke(8f),
        )

        for (p in engine.waiting) {
            var scale = 1f
            val age = engine.ms - p.popStart
            if (age >= 0f && age < 400f) scale = 1f + 0.35f * exp(-age / 140f) * sin(age / 34f)
            if (p.popStart < -1f) scale = 1f
            drawPassenger(p.x, p.y, p.color, scale)
        }

        // guard rail in front of the queue
        val rail = engine.spec.theme.signPost
        drawRoundRect(
            color = rail,
            topLeft = Offset(120f, Dim.QUEUE_Y + 38f),
            size = Size(Dim.VW - 240f, 9f),
            cornerRadius = CornerRadius(5f),
        )
        var x = 150f
        while (x < Dim.VW - 150f) {
            drawRoundRect(
                color = rail,
                topLeft = Offset(x, Dim.QUEUE_Y + 38f),
                size = Size(10f, 30f),
                cornerRadius = CornerRadius(4f),
            )
            x += 168f
        }
    }

    // ------------------------------------------------------------------ decorations

    private fun DrawScope.drawThemeDeco(theme: LevelTheme, ms: Float) {
        when (theme.deco) {
            Deco.SEA -> {
                // light rays
                withTransform({ rotate(-18f, Offset(220f, 0f)) }) {
                    drawRect(Color.White.copy(alpha = 0.10f), topLeft = Offset(160f, -60f), size = Size(90f, 700f))
                }
                withTransform({ rotate(-18f, Offset(430f, 0f)) }) {
                    drawRect(Color.White.copy(alpha = 0.07f), topLeft = Offset(390f, -60f), size = Size(60f, 700f))
                }
                // corals + seaweed bottom-left
                coralCone(52f, 618f, Color(0xFFFF8FB1))
                coralCone(104f, 624f, Color(0xFFB978E8))
                seaweed(150f, 630f, ms)
                // starfish right
                star(950f, 250f, 26f, Color(0xFFFFD32E))
                star(1010f, 330f, 16f, Color(0xFFFF9F2E))
            }

            Deco.ZOO -> {
                // wooden fence along the bottom of the sky zone
                var x = 0f
                while (x < Dim.VW) {
                    drawRoundRect(Color(0xFF8A5A2B), topLeft = Offset(x + 8f, 566f), size = Size(16f, 74f), cornerRadius = CornerRadius(6f))
                    x += 92f
                }
                drawRect(Color(0xFF9C6A38), topLeft = Offset(0f, 582f), size = Size(Dim.VW, 10f))
                drawRect(Color(0xFF9C6A38), topLeft = Offset(0f, 616f), size = Size(Dim.VW, 10f))
                // paw prints
                paw(720f, 250f)
                paw(60f, 470f)
                // giraffe-ish bushes
                drawCircle(Color(0xFF69A63C), radius = 46f, center = Offset(52f, 250f))
                drawCircle(Color(0xFF7CB24C), radius = 60f, center = Offset(1030f, 470f))
            }

            Deco.FUNFAIR -> {
                // ferris wheel, right
                val c = Offset(900f, 150f)
                drawCircle(Color(0xFFA05BC8), radius = 118f, center = c, style = Stroke(9f))
                for (i in 0 until 8) {
                    val a = i * Math.PI / 4
                    val e = Offset(c.x + cos(a).toFloat() * 118f, c.y + sin(a).toFloat() * 118f)
                    drawLine(Color(0xFFA05BC8), start = c, end = e, strokeWidth = 6f)
                    drawRoundRect(
                        Color(0xFFFF7AD9),
                        topLeft = Offset(e.x - 14f, e.y - 10f),
                        size = Size(28f, 20f),
                        cornerRadius = CornerRadius(6f),
                    )
                }
                // bunting across the top
                var x = 30f
                val cols = listOf(Color(0xFFFF4757), Color(0xFFFFD32E), Color(0xFF3B9BFF), Color(0xFF2ED573))
                var i = 0
                while (x < Dim.VW - 100f) {
                    val path = Path().apply {
                        moveTo(x, 6f)
                        lineTo(x + 44f, 6f)
                        lineTo(x + 22f, 52f)
                        close()
                    }
                    drawPath(path, cols[i++ % cols.size])
                    x += 58f
                }
                // balloons left
                drawCircle(Color(0xFFFF6FA5), radius = 26f, center = Offset(70f, 220f))
                drawCircle(Color(0xFF6FB6FF), radius = 20f, center = Offset(112f, 240f))
                drawCircle(Color(0xFFFFE071), radius = 17f, center = Offset(46f, 258f))
            }

            Deco.METRO -> {
                // train silhouette across the top
                drawRoundRect(Color(0xFFC4CEDD), topLeft = Offset(-40f, 190f), size = Size(Dim.VW + 80f, 118f), cornerRadius = CornerRadius(26f))
                drawRoundRect(Color(0xFF8E9DB5), topLeft = Offset(-40f, 276f), size = Size(Dim.VW + 80f, 32f), cornerRadius = CornerRadius(12f))
                var x = 40f
                while (x < Dim.VW - 60f) {
                    drawRoundRect(Color(0xFF44516A), topLeft = Offset(x, 212f), size = Size(120f, 52f), cornerRadius = CornerRadius(10f))
                    x += 190f
                }
                // lamp post left
                drawRoundRect(Color(0xFF39445A), topLeft = Offset(60f, 330f), size = Size(12f, 250f), cornerRadius = CornerRadius(5f))
                drawCircle(Color(0xFFFFF2B8), radius = 22f, center = Offset(66f, 322f))
                drawCircle(Color(0x66FFF2B8), radius = 38f, center = Offset(66f, 322f))
                // benches
                drawRoundRect(Color(0xFF6B5CA8), topLeft = Offset(640f, 440f), size = Size(200f, 18f), cornerRadius = CornerRadius(8f))
                drawRoundRect(Color(0xFF6B5CA8), topLeft = Offset(650f, 458f), size = Size(16f, 34f), cornerRadius = CornerRadius(5f))
                drawRoundRect(Color(0xFF6B5CA8), topLeft = Offset(812f, 458f), size = Size(16f, 34f), cornerRadius = CornerRadius(5f))
            }

            Deco.DESERT -> {
                // sun
                drawCircle(Color(0xFFFFE9A8), radius = 52f, center = Offset(920f, 120f))
                drawCircle(Color(0x66FFE9A8), radius = 78f, center = Offset(920f, 120f))
                // dunes
                drawOval(Color(0xFFF3D9A4), topLeft = Offset(-80f, 480f), size = Size(360f, 130f))
                drawOval(Color(0xFFF3D9A4), topLeft = Offset(760f, 520f), size = Size(420f, 110f))
                // cactus
                drawRoundRect(Color(0xFF7CB24C), topLeft = Offset(56f, 320f), size = Size(34f, 150f), cornerRadius = CornerRadius(18f))
                drawRoundRect(Color(0xFF7CB24C), topLeft = Offset(28f, 360f), size = Size(34f, 60f), cornerRadius = CornerRadius(16f))
                drawRoundRect(Color(0xFF7CB24C), topLeft = Offset(88f, 380f), size = Size(34f, 56f), cornerRadius = CornerRadius(16f))
                // awning stripes top-right
                var x = 640f
                var i = 0
                while (x < Dim.VW - 40f) {
                    drawRoundRect(
                        color = if (i++ % 2 == 0) Color(0xFFFFF6E3) else Color(0xFFE8623D),
                        topLeft = Offset(x, 170f),
                        size = Size(48f, 70f),
                        cornerRadius = CornerRadius(8f),
                    )
                    x += 50f
                }
            }
        }
    }

    // ------------------------------------------------------------------ small deco helpers

    private fun DrawScope.coralCone(x: Float, y: Float, color: Color) {
        val path = Path().apply {
            moveTo(x - 26f, y)
            lineTo(x, y - 66f)
            lineTo(x + 26f, y)
            close()
        }
        drawPath(path, color)
        drawCircle(color.copy(alpha = 0.75f), radius = 10f, center = Offset(x, y - 66f))
    }

    private fun DrawScope.seaweed(x: Float, baseY: Float, ms: Float) {
        val sway = sin(ms / 700f) * 12f
        val path = Path().apply {
            moveTo(x, baseY)
            cubicTo(x - 26f, baseY - 60f, x + 26f + sway, baseY - 100f, x + sway, baseY - 150f)
        }
        drawPath(path, Color(0xFF2FA36B), style = Stroke(10f))
    }

    private fun DrawScope.star(x: Float, y: Float, r: Float, color: Color) {
        val path = Path()
        for (i in 0 until 10) {
            val rad = if (i % 2 == 0) r else r * 0.45f
            val a = -Math.PI / 2 + i * Math.PI / 5
            val px = x + cos(a).toFloat() * rad
            val py = y + sin(a).toFloat() * rad
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        drawPath(path, color)
    }

    private fun DrawScope.paw(x: Float, y: Float) {
        val c = Color(0x557A4A22)
        drawCircle(c, radius = 18f, center = Offset(x, y))
        drawCircle(c, radius = 7f, center = Offset(x - 18f, y - 14f))
        drawCircle(c, radius = 7f, center = Offset(x, y - 20f))
        drawCircle(c, radius = 7f, center = Offset(x + 18f, y - 14f))
    }

    // ------------------------------------------------------------------ entities

    private fun DrawScope.drawCarEntity(car: CarEnt, ms: Float) {
        var angle = car.angle
        var scale = 1f
        val wobbleAge = ms - car.wobbleStart
        if (wobbleAge in 0f..600f) {
            angle += sin(wobbleAge / 26f) * 7.5f * exp(-wobbleAge / 260f)
        }
        val popAge = ms - car.popStart
        if (popAge in 0f..500f) {
            scale = 1f + 0.30f * exp(-popAge / 140f) * sin(popAge / 36f)
        }
        drawCar(car.x, car.y, angle, car.type, car.color, scale, mystery = !car.revealed, variant = car.spec.id)
    }

    /**
     * The premium toon car: oval shadow, chrome wheels, glossy capsule body with rim
     * light + roof dome, trapezoid glass with shine sweep, mirrors, bumpers, and the
     * iconic embossed white arrow. Local space points up (−Y); caller rotates.
     * [variant] deterministically dresses up some sedans (taxi sign, etc.).
     */
    fun DrawScope.drawCar(
        cx: Float,
        cy: Float,
        angleDeg: Float,
        type: CarType,
        color: CarColor,
        scale: Float = 1f,
        alpha: Float = 1f,
        mystery: Boolean = false,
        arrowVisible: Boolean = true,
        variant: Int = 0,
    ) {
        val hl = type.len / 2f
        val hw = type.wid / 2f
        val rr = hw * 0.55f
        val glassDark = Color(0xFF1B2C45)
        val glassLight = Color(0xFF4A6E9E)
        val isTaxi = type == CarType.SEDAN && variant % 6 == 1

        withTransform({
            translate(cx, cy)
            rotate(angleDeg, Offset.Zero)
            scale(scale, scale, Offset.Zero)
        }) {
            // soft oval drop shadow
            drawOval(
                color = Color.Black.copy(alpha = 0.30f * alpha),
                topLeft = Offset(-hw - 6f, -hl + 6f),
                size = Size(hw * 2 + 12f, hl * 2 + 2f),
            )
            // wheels with hubcaps
            val wy = hl * 0.55f
            for (wx in listOf(-hw - 3f, hw - 9f)) {
                for (yy in listOf(-wy, wy)) {
                    drawRoundRect(
                        color = Color(0xFF1C2027).copy(alpha = alpha),
                        topLeft = Offset(wx, yy - 16f),
                        size = Size(12f, 32f),
                        cornerRadius = CornerRadius(6f),
                    )
                    drawCircle(
                        color = Color(0xFF6E7680).copy(alpha = alpha),
                        radius = 5.5f,
                        center = Offset(wx + 6f, yy),
                    )
                    drawCircle(
                        color = Color(0xFFAEB6C0).copy(alpha = alpha),
                        radius = 2f,
                        center = Offset(wx + 6f, yy),
                    )
                }
            }
            // body outline pass
            drawRoundRect(
                color = color.deep.copy(alpha = alpha),
                topLeft = Offset(-hw - 2f, -hl - 2f),
                size = Size(hw * 2 + 4f, hl * 2 + 4f),
                cornerRadius = CornerRadius(rr + 2f),
            )
            // body diagonal gradient
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(mixWhite(color.body, 0.18f), color.body, color.dark),
                    start = Offset(-hw, -hl),
                    end = Offset(hw * 0.9f, hl),
                ),
                alpha = alpha,
                topLeft = Offset(-hw, -hl),
                size = Size(hw * 2, hl * 2),
                cornerRadius = CornerRadius(rr),
            )
            // ambient occlusion at the rear
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.20f * alpha)),
                    startY = hl * 0.35f,
                    endY = hl,
                ),
                topLeft = Offset(-hw, hl * 0.35f),
                size = Size(hw * 2, hl * 0.65f),
                cornerRadius = CornerRadius(rr),
            )
            // top sheen + rim light
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.38f * alpha), Color.Transparent),
                    startY = -hl,
                    endY = -hl * 0.25f,
                ),
                topLeft = Offset(-hw + 7f, -hl + 5f),
                size = Size(hw * 2 - 14f, hl * 0.8f),
                cornerRadius = CornerRadius(rr - 8f),
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.20f * alpha),
                topLeft = Offset(-hw, -hl),
                size = Size(hw * 2, hl * 2),
                cornerRadius = CornerRadius(rr),
                style = Stroke(4f),
            )
            // roof dome highlight
            drawOval(
                color = Color.White.copy(alpha = 0.13f * alpha),
                topLeft = Offset(-hw * 0.66f, -hl * 0.52f),
                size = Size(hw * 1.32f, hl * 0.72f),
            )
            // bumpers
            drawRoundRect(
                color = color.deep.copy(alpha = 0.26f * alpha),
                topLeft = Offset(-hw + 3f, -hl + 1f),
                size = Size(hw * 2 - 6f, 9f),
                cornerRadius = CornerRadius(5f),
            )
            drawRoundRect(
                color = color.deep.copy(alpha = 0.26f * alpha),
                topLeft = Offset(-hw + 3f, hl - 10f),
                size = Size(hw * 2 - 6f, 9f),
                cornerRadius = CornerRadius(5f),
            )
            // windshield trapezoid with gradient + shine sweep
            val wsW = hw * 0.80f
            val wsTop = -hl + 15f
            val wsPath = Path().apply {
                moveTo(-wsW / 2f, wsTop + 32f)
                lineTo(-wsW * 0.36f, wsTop)
                lineTo(wsW * 0.36f, wsTop)
                lineTo(wsW / 2f, wsTop + 32f)
                close()
            }
            drawPath(
                wsPath,
                Brush.verticalGradient(
                    listOf(glassLight.copy(alpha = 0.9f * alpha), glassDark.copy(alpha = 0.97f * alpha)),
                    startY = wsTop,
                    endY = wsTop + 32f,
                ),
            )
            withTransform({ rotate(-24f, Offset(0f, wsTop + 16f)) }) {
                drawRect(
                    color = Color.White.copy(alpha = 0.26f * alpha),
                    topLeft = Offset(-9f, wsTop - 8f),
                    size = Size(16f, 46f),
                )
            }
            // rear window
            val rwW = wsW * 0.86f
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(glassLight.copy(alpha = 0.7f * alpha), glassDark.copy(alpha = 0.92f * alpha)),
                    startY = hl - 48f,
                    endY = hl - 20f,
                ),
                topLeft = Offset(-rwW / 2f, hl - 48f),
                size = Size(rwW, 28f),
                cornerRadius = CornerRadius(10f),
            )
            // side glass strips
            for (sx in listOf(-1f, 1f)) {
                drawRoundRect(
                    color = glassDark.copy(alpha = 0.5f * alpha),
                    topLeft = Offset(sx * (hw - 12f) - 5f, -hl + 52f),
                    size = Size(10f, hl * 0.42f),
                    cornerRadius = CornerRadius(5f),
                )
            }
            // type-specific dressing
            when (type) {
                CarType.BUS -> {
                    drawRoundRect(
                        color = Color(0xFFFFC94D).copy(alpha = 0.95f * alpha),
                        topLeft = Offset(-wsW * 0.53f, -hl + 5f),
                        size = Size(wsW * 1.06f, 9f),
                        cornerRadius = CornerRadius(4f),
                    )
                    for (i in 0 until 3) {
                        val y0 = -hl + 60f + i * 42f
                        for (sx in listOf(-1f, 1f)) {
                            drawRoundRect(
                                color = color.deep.copy(alpha = 0.7f * alpha),
                                topLeft = Offset(sx * (hw - 15f) - 9f, y0),
                                size = Size(18f, 30f),
                                cornerRadius = CornerRadius(6f),
                            )
                            drawRoundRect(
                                color = Color(0xFF9FC3E8).copy(alpha = 0.9f * alpha),
                                topLeft = Offset(sx * (hw - 15f) - 6f, y0 + 3f),
                                size = Size(12f, 24f),
                                cornerRadius = CornerRadius(4f),
                            )
                        }
                    }
                    drawRoundRect(
                        color = color.deep.copy(alpha = 0.20f * alpha),
                        topLeft = Offset(-hw * 0.38f, hl - 84f),
                        size = Size(hw * 0.76f, 26f),
                        cornerRadius = CornerRadius(10f),
                    )
                }

                CarType.VAN -> {
                    for (sx in listOf(-1f, 1f)) {
                        drawRoundRect(
                            color = color.deep.copy(alpha = 0.25f * alpha),
                            topLeft = Offset(sx * (hw - 8f) - 1.5f, -hl * 0.18f),
                            size = Size(3f, hl * 0.66f),
                            cornerRadius = CornerRadius(1.5f),
                        )
                    }
                }

                CarType.SEDAN -> {
                    if (isTaxi) {
                        drawRoundRect(
                            color = color.deep.copy(alpha = 0.8f * alpha),
                            topLeft = Offset(-15f, -hl + 62f),
                            size = Size(30f, 17f),
                            cornerRadius = CornerRadius(6f),
                        )
                        drawRoundRect(
                            color = Color(0xFFFFF3C4).copy(alpha = alpha),
                            topLeft = Offset(-12.5f, -hl + 64f),
                            size = Size(25f, 13f),
                            cornerRadius = CornerRadius(5f),
                        )
                    }
                }
            }
            // mirrors
            for (sx in listOf(-1f, 1f)) {
                drawRoundRect(
                    color = color.deep.copy(alpha = alpha),
                    topLeft = Offset(sx * (hw + 1f) - 4f, -hl + 28f),
                    size = Size(8f, 15f),
                    cornerRadius = CornerRadius(4f),
                )
            }
            // headlights + taillights
            for (sx in listOf(-1f, 1f)) {
                drawRoundRect(
                    color = Color(0xFFFFF3C4).copy(alpha = alpha),
                    topLeft = Offset(sx * (hw - 20f) - 9f, -hl + 4f),
                    size = Size(18f, 10f),
                    cornerRadius = CornerRadius(5f),
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.8f * alpha),
                    radius = 2.5f,
                    center = Offset(sx * (hw - 20f) - 3f, -hl + 7f),
                )
                drawRoundRect(
                    color = Color(0xFFFF6B57).copy(alpha = alpha),
                    topLeft = Offset(sx * (hw - 20f) - 9f, hl - 14f),
                    size = Size(18f, 9f),
                    cornerRadius = CornerRadius(4f),
                )
            }
            // embossed roof arrow
            if (arrowVisible) {
                val aCol = if (mystery) Color(0xFFE9EDF1) else Color.White
                val arrow = Path().apply {
                    moveTo(0f, -hl * 0.58f)
                    lineTo(20f, -hl * 0.58f + 26f)
                    lineTo(8f, -hl * 0.58f + 26f)
                    lineTo(8f, hl * 0.20f)
                    lineTo(-8f, hl * 0.20f)
                    lineTo(-8f, -hl * 0.58f + 26f)
                    lineTo(-20f, -hl * 0.58f + 26f)
                    close()
                }
                withTransform({ translate(0f, 6f) }) {
                    drawPath(arrow, color.deep.copy(alpha = 0.55f * alpha))
                }
                drawPath(arrow, Color(0xFF1B1F26).copy(alpha = 0.35f * alpha), style = Stroke(6f))
                drawPath(arrow, aCol.copy(alpha = 0.97f * alpha))
                withTransform({ translate(0f, -3f) }) {
                    drawPath(arrow, Color.White.copy(alpha = 0.55f * alpha), style = Stroke(2.5f))
                }
            }
            if (mystery) {
                drawRoundRect(
                    color = Color(0xFF3C434B).copy(alpha = 0.30f * alpha),
                    topLeft = Offset(-hw, -hl),
                    size = Size(hw * 2, hl * 2),
                    cornerRadius = CornerRadius(rr),
                )
                outlinedText("?", 0f, 0f, 64f, fill = Color(0xFFF2F4F7), outline = Color(0xFF4A5058))
            }
        }
    }

    /** Premium toy "pawn" passenger: feet, capsule body with rim light, arms, glossy head + hair cap. */
    fun DrawScope.drawPassenger(x: Float, y: Float, color: CarColor, scale: Float = 1f) {
        withTransform({
            translate(x, y)
            scale(scale, scale, Offset.Zero)
        }) {
            // ground shadow
            drawOval(
                color = Color.Black.copy(alpha = 0.24f),
                topLeft = Offset(-16f, 14f),
                size = Size(32f, 15f),
            )
            // feet
            for (fx in listOf(-7f, 5f)) {
                drawOval(
                    color = color.deep.copy(alpha = 0.95f),
                    topLeft = Offset(fx, 18f),
                    size = Size(9f, 8f),
                )
            }
            // body capsule
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(mixWhite(color.body, 0.22f), color.body, color.dark),
                    startY = -14f,
                    endY = 24f,
                ),
                topLeft = Offset(-14f, -14f),
                size = Size(28f, 39f),
                cornerRadius = CornerRadius(14f),
            )
            // right-side shade + left rim light
            drawRoundRect(
                color = color.deep.copy(alpha = 0.28f),
                topLeft = Offset(2f, -14f),
                size = Size(12f, 39f),
                cornerRadius = CornerRadius(14f),
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.22f),
                topLeft = Offset(-14f, -12f),
                size = Size(8f, 34f),
                cornerRadius = CornerRadius(6f),
            )
            // arms
            for (sx in listOf(-1f, 1f)) {
                withTransform({ rotate(sx * 14f, Offset(sx * 16f, 4f)) }) {
                    drawRoundRect(
                        color = color.dark.copy(alpha = 0.95f),
                        topLeft = Offset(sx * 16f - 4f, -2f),
                        size = Size(9f, 17f),
                        cornerRadius = CornerRadius(5f),
                    )
                }
            }
            // head with glossy dome
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(mixWhite(color.body, 0.55f), color.body, color.dark),
                    center = Offset(-4f, -18f),
                    radius = 30f,
                ),
                radius = 13.5f,
                center = Offset(0f, -14f),
            )
            // hair cap (top arc in the deep shade)
            drawArc(
                color = color.deep.copy(alpha = 0.55f),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(-9.5f, -23.5f),
                size = Size(19f, 19f),
                style = Stroke(6f),
            )
            // sparkle on the head
            drawCircle(
                color = Color.White.copy(alpha = 0.8f),
                radius = 3.6f,
                center = Offset(-4f, -18f),
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.5f),
                radius = 1.6f,
                center = Offset(3f, -12f),
            )
        }
    }

    private fun DrawScope.drawBoarding(b: BoardAnim) {
        val p = b.pos()
        val t = b.t.coerceIn(0f, 1f)
        val arc = sin(t * Math.PI.toFloat())
        drawPassenger(p.x, p.y - arc * 10f, b.color, 0.9f + 0.15f * arc)
    }

    private fun DrawScope.drawTrail(car: CarEnt, ms: Float) {
        val pts = car.trail
        var i = 0
        while (i < pts.size) {
            val age = ms - pts[i].second
            val a = (1f - age / 520f).coerceIn(0f, 1f) * 0.8f
            drawCircle(
                color = car.color.body.copy(alpha = a),
                radius = 10f,
                center = Offset(pts[i].first.x, pts[i].first.y),
            )
            i += 2
        }
    }

    fun DrawScope.drawCoin(x: Float, y: Float, r: Float) {
        drawCircle(color = Color(0xFFB8860B), radius = r, center = Offset(x, y))
        drawCircle(color = Color(0xFFFFD32E), radius = r * 0.84f, center = Offset(x, y))
        drawCircle(color = Color(0xFFFFF0A8), radius = r * 0.42f, center = Offset(x - r * 0.18f, y - r * 0.18f))
        drawCircle(color = Color(0xFFB8860B).copy(alpha = 0.85f), radius = r * 0.62f, center = Offset(x, y), style = Stroke(r * 0.14f))
    }

    private const val AV = 8
    private val AV_BG = listOf(Color(0xFFFF6B6B), Color(0xFF4D96FF), Color(0xFF555555), Color(0xFF6BCB77), Color(0xFFFFD93D), Color(0xFFFF9F45), Color(0xFF4DD0E1), Color(0xFFFF6FB5))
    private val AV_SKIN = listOf(Color(0xFFFFDDB8), Color(0xFFF1C27D), Color(0xFFDB9E68), Color(0xFFB97F53), Color(0xFFFFE3C4), Color(0xFFE0AC69), Color(0xFFC68642), Color(0xFFFFD1A1))
    private val AV_HAIR = listOf(Color(0xFF5B3A29), Color(0xFF3A2E39), Color(0xFF2B2B33), Color(0xFF6B4226), Color(0xFF3D3D3D), Color(0xFF4A2C2A), Color(0xFF22252D), Color(0xFF7A4A12))

    /**
     * Outlined profile-avatar head on a coloured plate; [id] deterministically
     * picks one of 8 dressed-up styles (cap / helmet / shades / headband /
     * crown / headphones / mohawk / beanie).
     */
    fun DrawScope.drawAvatar(id: Int, cx: Float, cy: Float, r: Float) {
        val v = ((id % AV) + AV) % AV
        val bg = AV_BG[v]
        val skin = AV_SKIN[v]
        val hair = AV_HAIR[v]
        val hy = cy + r * 0.14f
        val hr = r * 0.55f
        val ey = hy - hr * 0.10f
        val ex = hr * 0.36f

        // plate
        drawCircle(bg, radius = r, center = Offset(cx, cy))
        drawCircle(mixWhite(bg, 0.30f), radius = r * 0.80f, center = Offset(cx, cy))
        // soft shadow + ears + head
        drawCircle(Color.Black.copy(alpha = 0.18f), radius = hr + r * 0.04f, center = Offset(cx, hy + r * 0.05f))
        for (sx in listOf(-1f, 1f)) drawCircle(skin, radius = hr * 0.20f, center = Offset(cx + sx * hr * 0.92f, hy + hr * 0.05f))
        drawCircle(skin, radius = hr, center = Offset(cx, hy))
        // eyes (variants 1 & 2 wear visor/shades instead)
        if (v != 1 && v != 2) {
            for (sx in listOf(-1f, 1f)) {
                drawCircle(Color.White, radius = hr * 0.17f, center = Offset(cx + sx * ex, ey))
                drawCircle(Color(0xFF2B2B33), radius = hr * 0.085f, center = Offset(cx + sx * ex, ey + hr * 0.03f))
            }
        }
        // smile
        drawArc(
            color = Color(0xFF7C4A21),
            startAngle = 25f,
            sweepAngle = 130f,
            useCenter = false,
            topLeft = Offset(cx - hr * 0.30f, hy + hr * 0.16f),
            size = Size(hr * 0.60f, hr * 0.42f),
            style = Stroke(width = r * 0.055f),
        )
        // dressing
        when (v) {
            0 -> { // red speed cap
                drawArc(Color(0xFFE63946), 180f, 180f, true, Offset(cx - hr, hy - hr * 1.12f), Size(hr * 2f, hr * 1.35f))
                drawRoundRect(Color(0xFFC1121F), Offset(cx - hr * 0.85f, hy - hr * 0.34f), Size(hr * 1.7f, hr * 0.20f), CornerRadius(hr * 0.10f))
                drawCircle(Color(0xFFFFD32E), radius = hr * 0.14f, center = Offset(cx, hy - hr * 0.72f))
            }

            1 -> { // racer helmet + visor
                drawArc(Color(0xFF2D6A9F), 180f, 180f, true, Offset(cx - hr * 1.02f, hy - hr * 1.14f), Size(hr * 2.04f, hr * 1.75f))
                drawRoundRect(Color(0xFF9FD8FF), Offset(cx - hr * 0.80f, ey - hr * 0.22f), Size(hr * 1.6f, hr * 0.52f), CornerRadius(hr * 0.22f))
                drawRoundRect(Color.White.copy(alpha = 0.75f), Offset(cx - hr * 0.62f, ey - hr * 0.14f), Size(hr * 0.55f, hr * 0.16f), CornerRadius(hr * 0.08f))
            }

            2 -> { // mop hair + cool shades
                drawArc(hair, 180f, 180f, true, Offset(cx - hr, hy - hr * 1.10f), Size(hr * 2f, hr * 1.30f))
                drawRoundRect(Color(0xFF22252D), Offset(cx - hr * 0.74f, ey - hr * 0.16f), Size(hr * 1.48f, hr * 0.34f), CornerRadius(hr * 0.13f))
                drawRoundRect(Color(0xFF9FC3E8).copy(alpha = 0.8f), Offset(cx - hr * 0.64f, ey - hr * 0.10f), Size(hr * 0.52f, hr * 0.20f), CornerRadius(hr * 0.08f))
            }

            3 -> { // hair + golden headband
                drawArc(hair, 180f, 180f, true, Offset(cx - hr, hy - hr * 1.08f), Size(hr * 2f, hr * 1.25f))
                drawRoundRect(Color(0xFFFFD32E), Offset(cx - hr, hy - hr * 0.62f), Size(hr * 2f, hr * 0.24f), CornerRadius(hr * 0.12f))
                drawCircle(Color(0xFFFFF0A8), radius = hr * 0.10f, center = Offset(cx, hy - hr * 0.50f))
            }

            4 -> { // champion crown
                drawArc(hair, 180f, 180f, true, Offset(cx - hr, hy - hr * 1.05f), Size(hr * 2f, hr * 1.2f))
                val base = hy - hr * 1.02f
                val crown = Path().apply {
                    moveTo(cx - hr * 0.55f, base)
                    lineTo(cx - hr * 0.55f, base - hr * 0.42f)
                    lineTo(cx - hr * 0.28f, base - hr * 0.18f)
                    lineTo(cx, base - hr * 0.52f)
                    lineTo(cx + hr * 0.28f, base - hr * 0.18f)
                    lineTo(cx + hr * 0.55f, base - hr * 0.42f)
                    lineTo(cx + hr * 0.55f, base)
                    close()
                }
                drawPath(crown, Color(0xFFFFC93C))
                drawPath(crown, Color(0xFFB8860B), style = Stroke(width = r * 0.03f))
                drawCircle(Color(0xFFFF6B6B), radius = hr * 0.07f, center = Offset(cx, base - hr * 0.26f))
            }

            5 -> { // headphones
                drawArc(hair, 180f, 180f, true, Offset(cx - hr, hy - hr * 1.06f), Size(hr * 2f, hr * 1.25f))
                drawArc(Color(0xFF39424E), 180f, 180f, false, Offset(cx - hr * 1.02f, hy - hr * 1.02f), Size(hr * 2.04f, hr * 1.6f), style = Stroke(width = r * 0.09f))
                for (sx in listOf(-1f, 1f)) {
                    drawRoundRect(Color(0xFF39424E), Offset(cx + sx * hr * 1.0f - r * 0.11f, hy - hr * 0.25f), Size(r * 0.22f, hr * 0.75f), CornerRadius(r * 0.10f))
                    drawRoundRect(Color(0xFF9AA5B1), Offset(cx + sx * hr * 1.0f - r * 0.055f, hy - hr * 0.10f), Size(r * 0.11f, hr * 0.45f), CornerRadius(r * 0.05f))
                }
            }

            6 -> { // pink mohawk
                drawArc(hair, 180f, 180f, true, Offset(cx - hr, hy - hr * 1.00f), Size(hr * 2f, hr * 1.05f))
                val mohawk = Path().apply {
                    moveTo(cx - hr * 0.22f, hy - hr * 0.78f)
                    lineTo(cx - hr * 0.10f, hy - hr * 1.55f)
                    lineTo(cx + hr * 0.06f, hy - hr * 1.05f)
                    lineTo(cx + hr * 0.16f, hy - hr * 1.70f)
                    lineTo(cx + hr * 0.26f, hy - hr * 0.80f)
                    close()
                }
                drawPath(mohawk, Color(0xFFFF3EA5))
            }

            else -> { // propeller beanie
                drawArc(Color(0xFF4D96FF), 180f, 180f, true, Offset(cx - hr, hy - hr * 1.10f), Size(hr * 2f, hr * 1.30f))
                drawRoundRect(Color(0xFFFFD32E), Offset(cx - r * 0.04f, hy - hr * 1.42f), Size(r * 0.08f, hr * 0.30f), CornerRadius(r * 0.03f))
                for (ang in listOf(30f, 210f)) {
                    withTransform({ rotate(ang, Offset(cx, hy - hr * 1.42f)) }) {
                        drawOval(Color(0xFFFF6B6B), Offset(cx + r * 0.02f, hy - hr * 1.55f), Size(hr * 0.75f, r * 0.16f))
                    }
                }
                drawCircle(Color(0xFFFFD32E), radius = r * 0.10f, center = Offset(cx, hy - hr * 1.42f))
            }
        }
    }

    private fun mixWhite(c: Color, f: Float): Color = Color(
        red = c.red + (1f - c.red) * f,
        green = c.green + (1f - c.green) * f,
        blue = c.blue + (1f - c.blue) * f,
        alpha = 1f,
    )
}

/** Convenience re-export so screens can draw the mini-showcase outside Painters' scope. */
fun DrawScope.showcaseCar(x: Float, y: Float, angle: Float, type: CarType, color: CarColor, scale: Float) {
    with(Painters) {
        drawCar(x, y, angle, type, color, scale)
    }
}
