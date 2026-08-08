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
        val centers = Dim.slotCenters(engine.spec.slotCount)
        val phase = -(engine.ms / 40f)
        centers.forEachIndexed { i, c ->
            if (!occupied.contains(i)) {
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.10f),
                    topLeft = Offset(c.x - 66f, c.y - 108f),
                    size = Size(132f, 216f),
                    cornerRadius = CornerRadius(26f),
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.72f),
                    topLeft = Offset(c.x - 66f, c.y - 108f),
                    size = Size(132f, 216f),
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
        drawCar(car.x, car.y, angle, car.type, car.color, scale, mystery = !car.revealed)
    }

    /**
     * The toon car itself: soft shadow, wheels, glossy capsule body, windshield,
     * roof panel with the iconic white arrow, bumper lights — like the Play Store art.
     * Local space points up (−Y); caller rotates by facing angle.
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
    ) {
        val hl = type.len / 2f
        val hw = type.wid / 2f
        val rr = hw * 0.55f

        withTransform({
            translate(cx, cy)
            rotate(angleDeg, Offset.Zero)
            scale(scale, scale, Offset.Zero)
        }) {
            // drop shadow
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.28f * alpha),
                topLeft = Offset(-hw - 3f, -hl + 4f),
                size = Size(hw * 2 + 10f, hl * 2 + 8f),
                cornerRadius = CornerRadius(rr),
            )
            // wheels
            val wy = hl * 0.55f
            for (wx in listOf(-hw - 3f, hw - 9f)) {
                for (yy in listOf(-wy, wy)) {
                    drawRoundRect(
                        color = Color(0xFF23272E).copy(alpha = alpha),
                        topLeft = Offset(wx, yy - 15f),
                        size = Size(12f, 30f),
                        cornerRadius = CornerRadius(6f),
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
            // body gradient
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(color.body, color.dark),
                    start = Offset(-hw, -hl),
                    end = Offset(hw * 0.8f, hl),
                ),
                alpha = alpha,
                topLeft = Offset(-hw, -hl),
                size = Size(hw * 2, hl * 2),
                cornerRadius = CornerRadius(rr),
            )
            // glossy top sheen
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.34f * alpha), Color.Transparent),
                    startY = -hl,
                    endY = -hl * 0.2f,
                ),
                topLeft = Offset(-hw + 7f, -hl + 5f),
                size = Size(hw * 2 - 14f, hl * 0.85f),
                cornerRadius = CornerRadius(rr - 8f),
            )
            // windshield (front = up)
            val wsW = hw * 0.78f
            drawRoundRect(
                color = Color(0xFF24344E).copy(alpha = 0.95f * alpha),
                topLeft = Offset(-wsW / 2f, -hl + 20f),
                size = Size(wsW, 34f),
                cornerRadius = CornerRadius(12f),
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.22f * alpha),
                topLeft = Offset(-wsW / 2f + 6f, -hl + 24f),
                size = Size(wsW * 0.38f, 26f),
                cornerRadius = CornerRadius(8f),
            )
            // rear window
            drawRoundRect(
                color = Color(0xFF24344E).copy(alpha = 0.85f * alpha),
                topLeft = Offset(-wsW * 0.42f, hl - 46f),
                size = Size(wsW * 0.84f, 26f),
                cornerRadius = CornerRadius(10f),
            )
            // bus side windows + stripe
            if (type == CarType.BUS) {
                for (i in 0 until 3) {
                    val y0 = -hl + 66f + i * 44f
                    for (sx in listOf(-1f, 1f)) {
                        drawRoundRect(
                            color = Color(0xFF24344E).copy(alpha = 0.55f * alpha),
                            topLeft = Offset(sx * (hw - 13f) - 7f, y0),
                            size = Size(14f, 30f),
                            cornerRadius = CornerRadius(6f),
                        )
                    }
                }
            }
            // roof panel
            drawRoundRect(
                color = Color.White.copy(alpha = 0.10f * alpha),
                topLeft = Offset(-hw * 0.56f, -hl * 0.40f),
                size = Size(hw * 1.12f, hl * 0.94f),
                cornerRadius = CornerRadius(18f),
            )
            // headlights + taillights
            for (sx in listOf(-1f, 1f)) {
                drawRoundRect(
                    color = Color(0xFFFFF3C4).copy(alpha = alpha),
                    topLeft = Offset(sx * (hw - 20f) - 9f, -hl + 4f),
                    size = Size(18f, 10f),
                    cornerRadius = CornerRadius(5f),
                )
                drawRoundRect(
                    color = Color(0xFFFF6B57).copy(alpha = alpha),
                    topLeft = Offset(sx * (hw - 20f) - 9f, hl - 14f),
                    size = Size(18f, 9f),
                    cornerRadius = CornerRadius(4f),
                )
            }
            // roof arrow
            if (arrowVisible) {
                val aCol = if (mystery) Color(0xFFE9EDF1) else Color.White
                val arrow = Path().apply {
                    // tip at front
                    moveTo(0f, -hl * 0.58f)
                    lineTo(20f, -hl * 0.58f + 26f)
                    lineTo(8f, -hl * 0.58f + 26f)
                    lineTo(8f, hl * 0.20f)
                    lineTo(-8f, hl * 0.20f)
                    lineTo(-8f, -hl * 0.58f + 26f)
                    lineTo(-20f, -hl * 0.58f + 26f)
                    close()
                }
                withTransform({ translate(0f, 5f) }) {
                    drawPath(arrow, color.deep.copy(alpha = 0.55f * alpha))
                }
                drawPath(arrow, aCol.copy(alpha = 0.97f * alpha))
            }
            if (mystery) {
                // dark veil + "?"
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

    /** Toy "pawn" passenger, top-down: capsule body + glossy head. */
    fun DrawScope.drawPassenger(x: Float, y: Float, color: CarColor, scale: Float = 1f) {
        withTransform({
            translate(x, y)
            scale(scale, scale, Offset.Zero)
        }) {
            drawOval(
                color = Color.Black.copy(alpha = 0.22f),
                topLeft = Offset(-15f, 14f),
                size = Size(30f, 14f),
            )
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(color.body, color.dark), startY = -14f, endY = 22f),
                topLeft = Offset(-14f, -14f),
                size = Size(28f, 38f),
                cornerRadius = CornerRadius(14f),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(mixWhite(color.body, 0.5f), color.body),
                    center = Offset(-3f, -14f),
                    radius = 26f,
                ),
                radius = 13f,
                center = Offset(0f, -14f),
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.75f),
                radius = 3.6f,
                center = Offset(-4f, -18f),
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
