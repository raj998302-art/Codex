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
        drawGate(engine)
        drawStationSign(theme, engine.remainingPassengers())
        drawQueue(engine)

        // trails first (under the moving car)
        for (car in engine.cars) {
            if (car.phase == CarPhase.EXITING && car.trail.size > 1) drawTrail(car, ms)
        }

        // arena cars, painter's depth order
        val arenaCars = engine.cars.filter { it.phase == CarPhase.IN_ARENA }.sortedBy { it.spec.y }
        for (car in arenaCars) drawCarEntity(car, ms, chained = engine.isChainedActive(car))

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
        val lockedLeft = engine.lockedRemaining()
        centers.forEachIndexed { i, c ->
            if (lockedLeft > 0 && i >= centers.size - lockedLeft) {
                // locked More-Spot slot: dark box, padlock, green plus badge
                drawRoundRect(
                    color = Color(0x40000000),
                    topLeft = Offset(c.x - halfW, c.y - halfH),
                    size = Size(halfW * 2, halfH * 2),
                    cornerRadius = CornerRadius(26f),
                )
                drawRoundRect(
                    color = Color(0xFF8C6A3F).copy(alpha = 0.9f),
                    topLeft = Offset(c.x - halfW, c.y - halfH),
                    size = Size(halfW * 2, halfH * 2),
                    cornerRadius = CornerRadius(26f),
                    style = Stroke(width = 6f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 16f), phase)),
                )
                // padlock
                drawArc(
                    Color(0xFFDDE4EE), startAngle = 180f, sweepAngle = 180f, useCenter = false,
                    topLeft = Offset(c.x - 20f, c.y - 48f), size = Size(40f, 42f), style = Stroke(10f),
                )
                drawRoundRect(Color(0xFF2B2B33), topLeft = Offset(c.x - 27f, c.y - 22f), size = Size(54f, 44f), cornerRadius = CornerRadius(11f))
                drawRoundRect(
                    Brush.verticalGradient(listOf(Color(0xFFB9C6DA), Color(0xFF5B6B82)), startY = c.y - 20f, endY = c.y + 20f),
                    topLeft = Offset(c.x - 23f, c.y - 19f), size = Size(46f, 38f), cornerRadius = CornerRadius(9f),
                )
                drawCircle(Color(0xFF2B2B33), radius = 6f, center = Offset(c.x, c.y - 2f))
                drawRect(Color(0xFF2B2B33), topLeft = Offset(c.x - 3f, c.y - 2f), size = Size(6f, 12f))
                // green plus badge (tap to unlock)
                val bx = c.x + halfW - 6f
                val by = c.y - halfH + 4f
                drawCircle(Color(0xFF2B2B33), radius = 26f, center = Offset(bx, by))
                drawCircle(Color(0xFF3DDC5F), radius = 22f, center = Offset(bx, by))
                drawRect(Color.White, topLeft = Offset(bx - 12f, by - 4f), size = Size(24f, 8f))
                drawRect(Color.White, topLeft = Offset(bx - 4f, by - 12f), size = Size(8f, 24f))
            } else if (!occupied.contains(i)) {
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

    /**
     * Exit gate: a hazard-striped barrier across one arena side with a counter
     * badge showing how many arena exits still lift it. Fades to a ghost once
     * open so the freed side reads instantly.
     */
    private fun DrawScope.drawGate(engine: GameEngine) {
        val side = engine.spec.gateSide
        if (side < 0) return
        val open = engine.gateOpen()
        val l = Dim.ARENA_LEFT
        val t = Dim.ARENA_TOP
        val r = Dim.ARENA_RIGHT
        val b = Dim.ARENA_BOTTOM
        val thick = 22f
        val inset = 3f
        // bar rect along the gated side
        val tl: Offset
        val sz: Size
        val horizontal = side == 0 || side == 2
        when (side) {
            0 -> { tl = Offset(l + inset, t + inset); sz = Size(r - l - inset * 2, thick) }
            2 -> { tl = Offset(l + inset, b - inset - thick); sz = Size(r - l - inset * 2, thick) }
            1 -> { tl = Offset(r - inset - thick, t + inset); sz = Size(thick, b - t - inset * 2) }
            else -> { tl = Offset(l + inset, t + inset); sz = Size(thick, b - t - inset * 2) }
        }
        val alphaMul = if (open) 0.16f else 1f
        // hazard stripes clipped to the bar
        drawRect(Color(0xFF2F3540).copy(alpha = 0.95f * alphaMul), tl, sz)
        val stripe = 44f
        val yellow = Color(0xFFFFC93C)
        val across = (if (horizontal) sz.width else sz.height)
        var x0 = 0f
        var i = 0
        while (x0 < across) {
            if (i % 2 == 0) {
                val segLen = minOf(stripe, across - x0)
                if (horizontal) {
                    drawRect(yellow.copy(alpha = alphaMul), Offset(tl.x + x0, tl.y), Size(segLen, sz.height))
                } else {
                    drawRect(yellow.copy(alpha = alphaMul), Offset(tl.x, tl.y + x0), Size(sz.width, segLen))
                }
            }
            x0 += stripe
            i++
        }
        // frame lines
        if (horizontal) {
            drawRect(Color(0xFF1D222C).copy(alpha = alphaMul), tl, Size(sz.width, 5f))
            drawRect(Color(0xFF1D222C).copy(alpha = alphaMul), Offset(tl.x, tl.y + sz.height - 5f), Size(sz.width, 5f))
        } else {
            drawRect(Color(0xFF1D222C).copy(alpha = alphaMul), tl, Size(5f, sz.height))
            drawRect(Color(0xFF1D222C).copy(alpha = alphaMul), Offset(tl.x + sz.width - 5f, tl.y), Size(5f, sz.height))
        }
        // counter badge at the bar middle, tucked just inside the arena so it
        // never collides with the passenger queue band above
        val mid = Offset(tl.x + sz.width / 2f, tl.y + sz.height / 2f)
        val badgeC = when (side) {
            0 -> Offset(mid.x, tl.y + sz.height + 46f)
            2 -> Offset(mid.x, tl.y - 46f)
            1 -> Offset(tl.x - 46f, mid.y)
            else -> Offset(tl.x + sz.width + 46f, mid.y)
        }
        if (!open) {
            drawCircle(Color(0xFF2F3540), radius = 40f, center = badgeC)
            drawCircle(Color(0xFFFFC93C), radius = 34f, center = badgeC)
            drawCircle(Color(0xFF2F3540), radius = 27f, center = badgeC)
            outlinedText("${engine.gateRemaining()}", badgeC.x, badgeC.y, 38f, fill = Color.White, outline = Color(0xFF2B2B33))
        } else {
            drawCircle(Color(0xFF3DDC5F).copy(alpha = 0.9f), radius = 26f, center = badgeC)
            outlinedText("OK", badgeC.x, badgeC.y, 24f, fill = Color.White, outline = Color(0xFF1B7A33))
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
            else -> {} /*bisect-tolerant*/
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
                // train silhouette across the top — orange stripe + door seams (reference-look)
                drawRoundRect(Color(0xFFC4CEDD), topLeft = Offset(-40f, 190f), size = Size(Dim.VW + 80f, 118f), cornerRadius = CornerRadius(26f))
                drawRoundRect(Color(0xFF8E9DB5), topLeft = Offset(-40f, 276f), size = Size(Dim.VW + 80f, 32f), cornerRadius = CornerRadius(12f))
                var x = 40f
                while (x < Dim.VW - 60f) {
                    drawRoundRect(Color(0xFF44516A), topLeft = Offset(x, 212f), size = Size(120f, 52f), cornerRadius = CornerRadius(10f))
                    x += 190f
                }
                // signature orange cab stripe
                drawRect(Color(0xFFE8623D), topLeft = Offset(-40f, 252f), size = Size(Dim.VW + 80f, 13f))
                drawRect(Color(0xFFFFB08A), topLeft = Offset(-40f, 250f), size = Size(Dim.VW + 80f, 3f))
                // door seams
                var dx = 235f
                while (dx < Dim.VW - 60f) {
                    drawRoundRect(Color(0xFF8E9DB5).copy(alpha = 0.7f), topLeft = Offset(dx, 200f), size = Size(9f, 100f), cornerRadius = CornerRadius(4f))
                    dx += 380f
                }
                // platform railing behind the queue band
                drawRect(Color(0xFF39445A), topLeft = Offset(0f, 560f), size = Size(Dim.VW, 9f))
                var rx = 36f
                while (rx < Dim.VW) {
                    drawRect(Color(0xFF39445A), topLeft = Offset(rx, 560f), size = Size(7f, 42f))
                    rx += 92f
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

            Deco.WINTER -> {
                // snow mounds along the skyline
                drawOval(Color(0xFFF8FCFF), topLeft = Offset(-90f, 470f), size = Size(420f, 170f))
                drawOval(Color(0xFFF0F8FF), topLeft = Offset(700f, 500f), size = Size(460f, 140f))
                // pines with snowy caps + a snowman
                pineTree(120f, 330f, 95f)
                pineTree(950f, 300f, 115f)
                pineTree(1015f, 390f, 70f)
                snowman(310f, 520f)
                // falling snow
                for (i in 0 until 26) {
                    val fx = (i * 91f) % Dim.VW
                    val speed = 0.05f + (i % 3) * 0.022f
                    val fy = ((i * 137f + ms * speed) % 620f) - 10f
                    drawCircle(Color.White.copy(alpha = 0.45f + (i % 3) * 0.2f), radius = 3f + (i % 4) * 1.2f, center = Offset(fx, fy))
                }
            }

            Deco.BEACH -> {
                // spinning sun
                val sun = Offset(150f, 130f)
                withTransform({ rotate(ms * 0.012f, sun) }) {
                    for (i in 0 until 10) {
                        val a = i * Math.PI / 5
                        drawLine(
                            Color(0xFFFFE071).copy(alpha = 0.7f),
                            start = Offset(sun.x + cos(a).toFloat() * 62f, sun.y + sin(a).toFloat() * 62f),
                            end = Offset(sun.x + cos(a).toFloat() * 92f, sun.y + sin(a).toFloat() * 92f),
                            strokeWidth = 14f,
                        )
                    }
                }
                drawCircle(Color(0xFFFFD32E), radius = 52f, center = sun)
                drawCircle(Color(0xFFFFE071), radius = 38f, center = sun)
                palmTree(940f, 470f)
                umbrella(230f, 470f)
                // beach ball
                drawCircle(Color(0xFFFFFFFF), radius = 30f, center = Offset(500f, 560f))
                drawArc(Color(0xFFFF4757), 200f, 120f, useCenter = true, topLeft = Offset(470f, 530f), size = Size(60f, 60f))
                drawArc(Color(0xFF3B9BFF), 320f, 120f, useCenter = true, topLeft = Offset(470f, 530f), size = Size(60f, 60f))
                drawCircle(Color(0xFF2B2B33).copy(alpha = 0.2f), radius = 30f, center = Offset(500f, 560f), style = Stroke(5f))
            }

            Deco.FROZEN -> {
                // aurora ribbons shimmering across the arctic sky
                val auroraCols = listOf(Color(0xFF7CF7C4), Color(0xFF7FC8FF), Color(0xFFC39BFF))
                for (band in 0 until 3) {
                    val pth = Path()
                    var x = -30f
                    pth.moveTo(x, 150f)
                    while (x < Dim.VW + 30f) {
                        val y = 130f + band * 46f + sin(x * 0.011f + ms * 0.0012f + band * 2.1f) * 34f
                        pth.lineTo(x, y)
                        x += 58f
                    }
                    drawPath(pth, auroraCols[band].copy(alpha = 0.20f), style = Stroke(width = 30f))
                }
                // giant ice crystals + melt puddles
                iceCrystal(90f, 380f, 66f)
                iceCrystal(980f, 330f, 84f)
                iceCrystal(880f, 470f, 50f)
                drawOval(Color(0x33FFFFFF), topLeft = Offset(300f, 560f), size = Size(230f, 60f))
                drawOval(Color(0x334A78A0), topLeft = Offset(310f, 566f), size = Size(210f, 48f))
                // light drifting snow
                for (i in 0 until 16) {
                    val fx = (i * 131f) % Dim.VW
                    val fy = ((i * 167f + ms * 0.035f) % 600f) - 10f
                    drawCircle(Color.White.copy(alpha = 0.5f), radius = 2.5f + (i % 3), center = Offset(fx, fy))
                }
            }

            Deco.LAVA -> {
                // volcano with a glowing crater
                volcano(170f, 500f, 300f)
                // lava pool shimmering at the sky base
                drawRoundRect(Color(0xFF7E2210), topLeft = Offset(480f, 560f), size = Size(560f, 70f), cornerRadius = CornerRadius(24f))
                drawRoundRect(Color(0xFFFF7B2E), topLeft = Offset(495f, 572f), size = Size(530f, 46f), cornerRadius = CornerRadius(18f))
                drawRoundRect(Color(0xFFFFC93C).copy(alpha = 0.85f), topLeft = Offset(520f, 584f), size = Size(200f, 20f), cornerRadius = CornerRadius(10f))
                // drifting smoke
                for (i in 0 until 4) {
                    val sx = 150f + i * 70f + sin(ms * 0.0009f + i * 2.3f) * 26f
                    val sy = 330f - i * 34f
                    drawCircle(Color(0xFF9E8B90).copy(alpha = 0.30f - i * 0.05f), radius = 30f + i * 9f, center = Offset(sx, sy))
                }
                // rising embers
                for (i in 0 until 18) {
                    val ex = (i * 149f) % Dim.VW + sin(ms * 0.0013f + i * 1.7f) * 24f
                    val ey = 620f - ((i * 173f + ms * (0.05f + (i % 4) * 0.018f)) % 610f)
                    val fade = (ey / 620f).coerceIn(0.15f, 1f)
                    drawCircle(if (i % 2 == 0) Color(0xFFFFC93C) else Color(0xFFFF7B2E), radius = 3.5f + (i % 3) * 1.5f, center = Offset(ex, ey), alpha = fade)
                }
            }

            Deco.JUNGLE -> {
                // canopy leaves hanging in from the top
                leaf(120f, 60f, 220f, -28f, Color(0xFF2F7C2E), Color(0xFF3F9E3C))
                leaf(560f, 20f, 250f, 12f, Color(0xFF2A7030), Color(0xFF38923A))
                leaf(980f, 70f, 220f, 24f, Color(0xFF2F7C2E), Color(0xFF4AAE46))
                // a vine with side leaves
                drawLine(Color(0xFF2A5C1E), Offset(870f, 0f), Offset(870f, 260f), strokeWidth = 10f)
                leaf(845f, 150f, 95f, -40f, Color(0xFF2F7C2E), Color(0xFF3F9E3C))
                leaf(905f, 210f, 85f, 35f, Color(0xFF2A7030), Color(0xFF45A843))
                // wildflowers
                flower(90f, 540f, Color(0xFFFF7AD9))
                flower(190f, 585f, Color(0xFFFFD32E))
                flower(770f, 560f, Color(0xFFFF7AD9))
                // drifting fireflies
                for (i in 0 until 12) {
                    val fx = (i * 223f) % Dim.VW + sin(ms * 0.0011f + i * 2.9f) * 46f
                    val fy = 260f + (i * 71f) % 320 + cos(ms * 0.0014f + i * 1.3f) * 30f
                    val tw = 0.35f + 0.45f * (0.5f + 0.5f * sin(ms * 0.006f + i * 2.1f))
                    drawCircle(Color(0xFFFFF3A6), radius = 4f, center = Offset(fx, fy), alpha = tw)
                }
            }

            Deco.NIGHT -> {
                // twinkling stars + moon
                for (i in 0 until 30) {
                    val sx = (i * 197f) % Dim.VW
                    val sy = (i * 83f) % 300f + 20f
                    val tw = 0.3f + 0.55f * (0.5f + 0.5f * sin(ms * 0.004f + i * 1.9f))
                    drawCircle(Color(0xFFFFFBEA), radius = 2.2f + (i % 3) * 1.1f, center = Offset(sx, sy), alpha = tw)
                }
                drawCircle(Color(0xFFFFF4D6), radius = 54f, center = Offset(900f, 130f))
                drawCircle(Color(0xFFE9DFC0), radius = 11f, center = Offset(880f, 115f))
                drawCircle(Color(0xFFE9DFC0), radius = 7f, center = Offset(915f, 145f))
                // neon skyline with lit windows
                val buildingCols = listOf(Color(0xFF1C2244), Color(0xFF232B52), Color(0xFF1A2040), Color(0xFF262E58))
                var bx = -20f
                var bi = 0
                while (bx < Dim.VW) {
                    val bw = 150f + (bi % 3) * 50f
                    val bh = 190f + (bi % 4) * 55f
                    drawRoundRect(buildingCols[bi % 4], topLeft = Offset(bx, 630f - bh), size = Size(bw, bh), cornerRadius = CornerRadius(10f))
                    var wy = 630f - bh + 22f
                    var wi = 0
                    while (wy < 630f - 30f) {
                        var wx = bx + 18f
                        var wj = 0
                        while (wx < bx + bw - 22f) {
                            if ((wi + wj + bi) % 3 != 0) {
                                drawRect(Color(0xFFFFD93D).copy(alpha = 0.85f), topLeft = Offset(wx, wy), size = Size(14f, 16f))
                            }
                            wx += 32f
                            wj++
                        }
                        wy += 34f
                        wi++
                    }
                    // neon roof edge
                    drawRect(if (bi % 2 == 0) Color(0xFFFF5AD2) else Color(0xFF5AF2FF), topLeft = Offset(bx, 630f - bh), size = Size(bw, 6f))
                    bx += bw + 26f
                    bi++
                }
            }
        }
    }

    // ------------------------------------------------------------------ small deco helpers

    private fun DrawScope.pineTree(x: Float, y: Float, s: Float) {
        drawRect(Color(0xFF7E5A38), topLeft = Offset(x - s * 0.06f, y + s * 0.55f), size = Size(s * 0.12f, s * 0.32f))
        val greens = listOf(Color(0xFF2F7C4E), Color(0xFF37925C), Color(0xFF41A86C))
        for (i in 0 until 3) {
            val w = s * (1f - i * 0.24f)
            val ty = y + i * s * 0.26f
            val tri = Path().apply {
                moveTo(x - w / 2f, ty + s * 0.34f)
                lineTo(x + w / 2f, ty + s * 0.34f)
                lineTo(x, ty - s * 0.06f)
                close()
            }
            drawPath(tri, greens[i])
            val cap = Path().apply {
                moveTo(x - w * 0.18f, ty + s * 0.05f)
                lineTo(x + w * 0.18f, ty + s * 0.05f)
                lineTo(x, ty - s * 0.06f)
                close()
            }
            drawPath(cap, Color(0xFFF4FAFF))
        }
    }

    private fun DrawScope.snowman(x: Float, y: Float) {
        drawCircle(Color(0xFFF8FCFF), radius = 34f, center = Offset(x, y))
        drawCircle(Color(0xFFF8FCFF), radius = 24f, center = Offset(x, y - 46f))
        drawRect(Color(0xFF2B2B33), topLeft = Offset(x - 17f, y - 88f), size = Size(34f, 10f))
        drawRect(Color(0xFF2B2B33), topLeft = Offset(x - 11f, y - 110f), size = Size(22f, 24f))
        drawCircle(Color(0xFF2B2B33), radius = 3f, center = Offset(x - 8f, y - 52f))
        drawCircle(Color(0xFF2B2B33), radius = 3f, center = Offset(x + 8f, y - 52f))
        drawRect(Color(0xFFFF4757), topLeft = Offset(x - 20f, y - 34f), size = Size(40f, 9f))
        drawRect(Color(0xFFFF4757), topLeft = Offset(x + 8f, y - 28f), size = Size(9f, 22f))
    }

    private fun DrawScope.palmTree(x: Float, y: Float) {
        for (i in 0 until 4) {
            drawRoundRect(
                Color(0xFF9C6A38),
                topLeft = Offset(x + i * 12f - 10f, y - i * 46f - 42f),
                size = Size(24f, 52f),
                cornerRadius = CornerRadius(10f),
            )
        }
        val top = Offset(x + 44f, y - 195f)
        for (i in 0 until 5) {
            val a = -25f + i * 42f
            withTransform({ rotate(a, top) }) {
                drawOval(Color(0xFF3F9E3C), topLeft = Offset(top.x, top.y - 9f), size = Size(120f, 30f))
                drawOval(Color(0xFF4AAE46), topLeft = Offset(top.x + 8f, top.y - 4f), size = Size(104f, 18f))
            }
        }
        drawCircle(Color(0xFF7B5A2B), radius = 12f, center = Offset(top.x + 6f, top.y + 12f))
        drawCircle(Color(0xFF7B5A2B), radius = 10f, center = Offset(top.x + 26f, top.y + 8f))
    }

    private fun DrawScope.umbrella(x: Float, y: Float) {
        drawLine(Color(0xFF8A5A2B), Offset(x, y - 110f), Offset(x, y + 40f), strokeWidth = 10f)
        val cols = listOf(Color(0xFFFF4757), Color(0xFFFFF6E3), Color(0xFF3B9BFF), Color(0xFFFFF6E3))
        for (i in 0 until 4) {
            val p = Path().apply {
                moveTo(x + (i - 2) * 55f, y - 100f)
                lineTo(x + (i - 1) * 55f, y - 100f)
                lineTo(x, y - 165f)
                close()
            }
            drawPath(p, cols[i])
        }
    }

    private fun DrawScope.iceCrystal(x: Float, y: Float, s: Float) {
        val body = Path().apply {
            moveTo(x, y - s)
            lineTo(x + s * 0.55f, y)
            lineTo(x, y + s)
            lineTo(x - s * 0.55f, y)
            close()
        }
        drawPath(body, Color(0xFFBFE8FF).copy(alpha = 0.85f))
        drawLine(Color.White.copy(alpha = 0.8f), Offset(x, y - s * 0.7f), Offset(x + s * 0.35f, y), strokeWidth = 5f)
        drawLine(Color.White.copy(alpha = 0.5f), Offset(x, y - s * 0.7f), Offset(x - s * 0.35f, y), strokeWidth = 3f)
    }

    private fun DrawScope.volcano(x: Float, y: Float, w: Float) {
        val cone = Path().apply {
            moveTo(x - w / 2f, y)
            lineTo(x - w * 0.14f, y - w * 0.5f)
            lineTo(x + w * 0.14f, y - w * 0.5f)
            lineTo(x + w / 2f, y)
            close()
        }
        drawPath(cone, Color(0xFF5A3236))
        drawOval(Color(0xFFFF7B2E), topLeft = Offset(x - w * 0.16f, y - w * 0.53f), size = Size(w * 0.32f, w * 0.09f))
        drawOval(Color(0xFFFFC93C), topLeft = Offset(x - w * 0.09f, y - w * 0.51f), size = Size(w * 0.18f, w * 0.05f))
        val drip = Path().apply {
            moveTo(x - w * 0.10f, y - w * 0.44f)
            lineTo(x + w * 0.02f, y - w * 0.44f)
            lineTo(x - w * 0.02f, y - w * 0.16f)
            close()
        }
        drawPath(drip, Color(0xFFE8623D))
    }

    private fun DrawScope.leaf(cx: Float, cy: Float, len: Float, deg: Float, dark: Color, light: Color) {
        withTransform({ rotate(deg, Offset(cx, cy)) }) {
            drawOval(dark, topLeft = Offset(cx - len / 2f, cy - len * 0.17f), size = Size(len, len * 0.34f))
            drawOval(light, topLeft = Offset(cx - len / 2f + 14f, cy - len * 0.17f + 7f), size = Size(len - 28f, len * 0.34f - 14f))
            drawLine(light.copy(alpha = 0.7f), Offset(cx - len / 2f + 12f, cy), Offset(cx + len / 2f - 12f, cy), strokeWidth = 5f)
        }
    }

    private fun DrawScope.flower(x: Float, y: Float, petal: Color) {
        for (i in 0 until 5) {
            val a = i * Math.PI * 2 / 5
            drawCircle(petal, radius = 13f, center = Offset(x + cos(a).toFloat() * 16f, y + sin(a).toFloat() * 16f))
        }
        drawCircle(Color(0xFFFFD32E), radius = 10f, center = Offset(x, y))
    }

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

    private fun DrawScope.drawCarEntity(car: CarEnt, ms: Float, chained: Boolean = false) {
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
        if (car.frozenLeft > 0) {
            drawIce(car.x, car.y, angle, car.type, scale, cracked = car.spec.frozen - car.frozenLeft)
        }
        if (chained) {
            drawChains(car.x, car.y, angle, car.type, scale)
        }
    }

    /**
     * Iron chains strapped diagonally over a chain-locked car: two link runs
     * corner-to-corner plus a chunky padlock in the middle. Only drawn while
     * the key car still sits in the arena.
     */
    private fun DrawScope.drawChains(cx: Float, cy: Float, angleDeg: Float, type: CarType, scale: Float) {
        val hl = type.len / 2f
        val hw = type.wid / 2f
        val iron = Color(0xFF3C4250)
        val ironLight = Color(0xFF8A93A6)
        withTransform({
            translate(cx, cy)
            rotate(angleDeg, Offset.Zero)
            scale(scale, scale, Offset.Zero)
        }) {
            val diagonals = listOf(
                Offset(-hw * 0.96f, -hl * 0.96f) to Offset(hw * 0.96f, hl * 0.96f),
                Offset(hw * 0.96f, -hl * 0.96f) to Offset(-hw * 0.96f, hl * 0.96f),
            )
            for ((a, b) in diagonals) {
                val dx = b.x - a.x
                val dy = b.y - a.y
                val len = kotlin.math.sqrt(dx * dx + dy * dy)
                val linkDeg = Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
                val links = (len / 26f).toInt().coerceIn(5, 12)
                for (i in 0..links) {
                    val f = i / links.toFloat()
                    val px = a.x + dx * f
                    val py = a.y + dy * f
                    val tilt = linkDeg + (if (i % 2 == 0) 32f else -32f)
                    withTransform({
                        translate(px, py)
                        rotate(tilt, Offset.Zero)
                    }) {
                        drawRoundRect(iron, Offset(-13f, -8f), Size(26f, 16f), cornerRadius = CornerRadius(8f))
                        drawRoundRect(ironLight, Offset(-9f, -5f), Size(18f, 6f), cornerRadius = CornerRadius(3f))
                    }
                }
            }
            // padlock
            drawArc(
                iron, startAngle = 180f, sweepAngle = 180f, useCenter = false,
                topLeft = Offset(-16f, -34f), size = Size(32f, 34f), style = Stroke(9f),
            )
            drawRoundRect(iron, Offset(-22f, -16f), Size(44f, 36f), cornerRadius = CornerRadius(9f))
            drawRoundRect(
                Brush.verticalGradient(listOf(ironLight, iron), startY = -14f, endY = 18f),
                Offset(-19f, -13f), Size(38f, 30f), cornerRadius = CornerRadius(7f),
            )
            drawCircle(Color(0xFFFFD93D), radius = 5.5f, center = Offset(0f, 0f))
            drawRect(Color(0xFFFFD93D), Offset(-2.5f, 0f), Size(5f, 9f))
        }
    }

    /**
     * Translucent ice shell over a frozen car: frosted glass slab, white rim,
     * crack web that grows with each tap, and a corner frost sparkle. Drawn
     * translucent so the car's colour (match cue) still reads through.
     */
    private fun DrawScope.drawIce(
        cx: Float,
        cy: Float,
        angleDeg: Float,
        type: CarType,
        scale: Float,
        cracked: Int,
    ) {
        val hl = type.len / 2f
        val hw = type.wid / 2f
        val rr = hw * 0.55f
        withTransform({
            translate(cx, cy)
            rotate(angleDeg, Offset.Zero)
            scale(scale, scale, Offset.Zero)
        }) {
            // frozen slab with glossy frost gradient
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFE3F5FF).copy(alpha = 0.62f),
                        Color(0xFF9ED9F5).copy(alpha = 0.50f),
                        Color(0xFF6FBEE8).copy(alpha = 0.58f),
                    ),
                    start = Offset(-hw, -hl),
                    end = Offset(hw, hl),
                ),
                topLeft = Offset(-hw - 5f, -hl - 5f),
                size = Size(hw * 2 + 10f, hl * 2 + 10f),
                cornerRadius = CornerRadius(rr + 6f),
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.75f),
                topLeft = Offset(-hw - 5f, -hl - 5f),
                size = Size(hw * 2 + 10f, hl * 2 + 10f),
                cornerRadius = CornerRadius(rr + 6f),
                style = Stroke(4.5f),
            )
            // frost sheen band
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.5f), Color.Transparent),
                    startY = -hl,
                    endY = -hl * 0.2f,
                ),
                topLeft = Offset(-hw + 6f, -hl),
                size = Size(hw * 0.6f, hl * 0.8f),
                cornerRadius = CornerRadius(rr),
            )
            // crack webs — denser after each tap
            val crackCol = Color(0xFFF4FBFF).copy(alpha = 0.85f)
            val crackCount = 2 + cracked * 2
            for (i in 0 until crackCount) {
                val a = (i * 137f) % 360f
                val rad = Math.toRadians(a.toDouble())
                val cxn = (cos(rad) * hw * 0.4f).toFloat()
                val cyn = (sin(rad) * hl * 0.4f).toFloat()
                val ex = (cos(rad) * hw * 0.95f).toFloat()
                val ey = (sin(rad) * hl * 0.9f).toFloat()
                val mx = (cxn + ex) / 2f + (if (i % 2 == 0) 10f else -10f)
                val my = (cyn + ey) / 2f + (if (i % 2 == 0) -8f else 8f)
                drawLine(crackCol, Offset(cxn, cyn), Offset(mx, my), strokeWidth = 3f)
                drawLine(crackCol, Offset(mx, my), Offset(ex, ey), strokeWidth = 2.2f)
            }
            // sparkle at the top corner
            drawCircle(Color.White.copy(alpha = 0.9f), radius = 4f, center = Offset(-hw * 0.62f, -hl * 0.72f))
            drawCircle(Color.White.copy(alpha = 0.5f), radius = 8f, center = Offset(-hw * 0.62f, -hl * 0.72f))
        }
    }

    /**
     * AAA v3 toon car — rebuilt from the concept renders: layered contact shadow,
     * five-spoke alloy wheels, pearlescent paint with panel seams, a full glasshouse
     * with sky reflections, LED front/rear fascia, mirrors, handles and a license
     * plate, crowned by the iconic embossed white arrow. Local space points up (−Y);
     * the caller rotates. [variant] dresses up some sedans (taxi sign, lightbar).
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
        val glassDark = Color(0xFF16273F)
        val glassLight = Color(0xFF5F8CC4)
        val tireCol = Color(0xFF161A21)
        val isTaxi = type == CarType.SEDAN && variant % 6 == 1
        val isSport = type == CarType.SEDAN && variant % 6 == 2
        val isPolice = type == CarType.SEDAN && variant % 6 == 4
        val isAmbulance = type == CarType.VAN && variant % 3 == 1
        val isSchool = type == CarType.BUS && variant % 4 == 1

        withTransform({
            translate(cx, cy)
            rotate(angleDeg, Offset.Zero)
            scale(scale, scale, Offset.Zero)
        }) {
            // ---------- grounded shadow: broad ambient oval + tight contact blob
            drawOval(
                color = Color.Black.copy(alpha = 0.16f * alpha),
                topLeft = Offset(-hw - 10f, -hl + 12f),
                size = Size(hw * 2 + 20f, hl * 2 + 4f),
            )
            drawOval(
                color = Color.Black.copy(alpha = 0.28f * alpha),
                topLeft = Offset(-hw + 4f, -hl + 16f),
                size = Size(hw * 2 - 8f, hl * 2 - 14f),
            )

            // ---------- wheels: tire shell + five-spoke alloy + sheen
            val wy = hl * 0.55f
            for (sx in listOf(-1f, 1f)) {
                for (sy in listOf(-1f, 1f)) {
                    val wx = sx * (hw + 1f)
                    val yy = sy * wy
                    drawRoundRect(
                        color = tireCol.copy(alpha = alpha),
                        topLeft = Offset(wx - 8.5f, yy - 17.5f),
                        size = Size(17f, 35f),
                        cornerRadius = CornerRadius(8.5f),
                    )
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.10f * alpha),
                        topLeft = Offset(wx - 8.5f, yy - 17.5f),
                        size = Size(17f, 12f),
                        cornerRadius = CornerRadius(8.5f),
                    )
                    drawCircle(color = Color(0xFF3E4653).copy(alpha = alpha), radius = 6.9f, center = Offset(wx, yy))
                    drawCircle(color = Color(0xFFAFB9C6).copy(alpha = alpha), radius = 5.1f, center = Offset(wx, yy))
                    for (k in 0 until 5) {
                        withTransform({ rotate(k * 72f, Offset(wx, yy)) }) {
                            drawRoundRect(
                                color = Color(0xFFEDF1F6).copy(alpha = 0.9f * alpha),
                                topLeft = Offset(wx - 1.1f, yy - 4.6f),
                                size = Size(2.2f, 3.6f),
                                cornerRadius = CornerRadius(1.1f),
                            )
                        }
                    }
                    drawCircle(color = Color(0xFF565F6C).copy(alpha = alpha), radius = 1.6f, center = Offset(wx, yy))
                }
            }

            // ---------- body silhouette outline (deepest shade bleeding 3px out)
            drawRoundRect(
                color = color.deep.copy(alpha = alpha),
                topLeft = Offset(-hw - 3f, -hl - 3f),
                size = Size(hw * 2 + 6f, hl * 2 + 6f),
                cornerRadius = CornerRadius(rr + 3f),
            )
            // ---------- pearlescent paint: 4-stop diagonal gradient
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        mixWhite(color.body, 0.30f),
                        mixWhite(color.body, 0.08f),
                        color.body,
                        color.dark,
                    ),
                    start = Offset(-hw, -hl),
                    end = Offset(hw * 0.8f, hl),
                ),
                alpha = alpha,
                topLeft = Offset(-hw, -hl),
                size = Size(hw * 2, hl * 2),
                cornerRadius = CornerRadius(rr),
            )
            // side curvature: bright left edge, shaded right edge
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.16f)),
                    startX = hw * 0.25f,
                    endX = hw,
                ),
                alpha = alpha,
                topLeft = Offset(0f, -hl + 4f),
                size = Size(hw, hl * 2 - 8f),
                cornerRadius = CornerRadius(rr - 4f),
            )
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.14f), Color.Transparent),
                    startX = -hw + 3f,
                    endX = -hw * 0.3f,
                ),
                alpha = alpha,
                topLeft = Offset(-hw + 3f, -hl + 6f),
                size = Size(hw * 0.5f, hl * 2 - 12f),
                cornerRadius = CornerRadius(rr - 5f),
            )
            // rear ambient occlusion
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.18f * alpha)),
                    startY = hl * 0.42f,
                    endY = hl,
                ),
                topLeft = Offset(-hw, hl * 0.42f),
                size = Size(hw * 2, hl * 0.58f),
                cornerRadius = CornerRadius(rr),
            )
            // hood sheen band + inner rim light + roof light pool
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.34f * alpha), Color.Transparent),
                    startY = -hl,
                    endY = -hl * 0.30f,
                ),
                topLeft = Offset(-hw + 8f, -hl + 6f),
                size = Size(hw * 2 - 16f, hl * 0.72f),
                cornerRadius = CornerRadius(rr - 8f),
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.17f * alpha),
                topLeft = Offset(-hw, -hl),
                size = Size(hw * 2, hl * 2),
                cornerRadius = CornerRadius(rr),
                style = Stroke(3.5f),
            )
            drawOval(
                color = Color.White.copy(alpha = 0.11f * alpha),
                topLeft = Offset(-hw * 0.62f, -hl * 0.30f),
                size = Size(hw * 1.24f, hl * 0.66f),
            )
            // ---------- panel seams (hood shut line / trunk shut line)
            val seam = color.deep.copy(alpha = 0.20f * alpha)
            drawLine(seam, Offset(-hw * 0.78f, -hl * 0.86f), Offset(hw * 0.78f, -hl * 0.86f), strokeWidth = 2.5f)
            drawLine(seam, Offset(-hw * 0.78f, hl * 0.88f), Offset(hw * 0.78f, hl * 0.88f), strokeWidth = 2.5f)

            // ---------- glasshouse: windshield + side glass + rear window
            val wsTop = -hl * 0.80f
            val wsBot = -hl * 0.38f
            val wsPath = Path().apply {
                moveTo(-hw * 0.86f, wsBot)
                lineTo(-hw * 0.52f, wsTop)
                lineTo(hw * 0.52f, wsTop)
                lineTo(hw * 0.86f, wsBot)
                close()
            }
            drawPath(
                wsPath,
                Brush.linearGradient(
                    colors = listOf(
                        glassLight.copy(alpha = 0.95f * alpha),
                        glassDark.copy(alpha = 0.97f * alpha),
                    ),
                    start = Offset(-hw * 0.4f, wsTop),
                    end = Offset(hw * 0.3f, wsBot),
                ),
            )
            drawPath(wsPath, Color.White.copy(alpha = 0.26f * alpha), style = Stroke(2f))
            // sky reflection sweep across the windshield
            withTransform({ rotate(-26f, Offset(0f, (wsTop + wsBot) / 2f)) }) {
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.30f * alpha),
                    topLeft = Offset(-hw * 0.62f, wsTop - 14f),
                    size = Size(hw * 0.30f, wsBot - wsTop + 28f),
                    cornerRadius = CornerRadius(7f),
                )
            }
            // rear window
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(
                        glassLight.copy(alpha = 0.80f * alpha),
                        glassDark.copy(alpha = 0.93f * alpha),
                    ),
                    startY = hl * 0.44f,
                    endY = hl * 0.64f,
                ),
                topLeft = Offset(-hw * 0.66f, hl * 0.44f),
                size = Size(hw * 1.32f, hl * 0.20f),
                cornerRadius = CornerRadius(9f),
            )
            // side glass capsules with a thin sky bounce line
            for (sx in listOf(-1f, 1f)) {
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(
                            glassDark.copy(alpha = 0.38f * alpha),
                            glassDark.copy(alpha = 0.62f * alpha),
                        ),
                        startY = -hl * 0.34f,
                        endY = hl * 0.38f,
                    ),
                    topLeft = Offset(sx * (hw - 11f) - 4.5f, -hl * 0.34f),
                    size = Size(9f, hl * 0.72f),
                    cornerRadius = CornerRadius(4.5f),
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.22f * alpha),
                    start = Offset(sx * (hw - 11f), -hl * 0.28f),
                    end = Offset(sx * (hw - 11f), hl * 0.30f),
                    strokeWidth = 1.6f,
                )
            }

            // ---------- type-specific dressing
            when (type) {
                else -> {} /*bisect-tolerant*/
                CarType.BUS -> {
                    // amber route board at the front
                    drawRoundRect(
                        color = Color(0xFFFFC94D).copy(alpha = 0.95f * alpha),
                        topLeft = Offset(-hw * 0.45f, -hl + 5f),
                        size = Size(hw * 0.9f, 9f),
                        cornerRadius = CornerRadius(4f),
                    )
                    // three passenger windows per flank
                    for (i in 0 until 3) {
                        val y0 = -hl * 0.44f + i * (hl * 0.38f)
                        for (sx in listOf(-1f, 1f)) {
                            drawRoundRect(
                                color = color.deep.copy(alpha = 0.7f * alpha),
                                topLeft = Offset(sx * (hw - 15f) - 9f, y0),
                                size = Size(18f, 30f),
                                cornerRadius = CornerRadius(6f),
                            )
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    listOf(Color(0xFFB9D9F5), Color(0xFF6E9CC8)),
                                    startY = y0 + 3f,
                                    endY = y0 + 27f,
                                ),
                                alpha = alpha,
                                topLeft = Offset(sx * (hw - 15f) - 6f, y0 + 3f),
                                size = Size(12f, 24f),
                                cornerRadius = CornerRadius(4f),
                            )
                        }
                    }
                    // dual roof vents
                    for (vy in listOf(-hl * 0.02f, hl * 0.30f)) {
                        drawRoundRect(
                            color = color.deep.copy(alpha = 0.30f * alpha),
                            topLeft = Offset(-hw * 0.36f, vy),
                            size = Size(hw * 0.72f, 11f),
                            cornerRadius = CornerRadius(5.5f),
                        )
                        drawLine(
                            color = Color.White.copy(alpha = 0.25f * alpha),
                            start = Offset(-hw * 0.28f, vy + 5.5f),
                            end = Offset(hw * 0.28f, vy + 5.5f),
                            strokeWidth = 1.6f,
                        )
                    }
                    drawRoundRect(
                        color = color.deep.copy(alpha = 0.20f * alpha),
                        topLeft = Offset(-hw * 0.38f, hl - 84f),
                        size = Size(hw * 0.76f, 26f),
                        cornerRadius = CornerRadius(10f),
                    )
                    if (isSchool) {
                        // twin amber warning beacons on the front roof edge
                        for (sx in listOf(-1f, 1f)) {
                            drawCircle(Color(0xFFFFB300).copy(alpha = 0.25f * alpha), 9f, Offset(sx * hw * 0.28f, -hl * 0.90f))
                            drawCircle(Color(0xFFFFB300).copy(alpha = alpha), 5.5f, Offset(sx * hw * 0.28f, -hl * 0.90f))
                            drawCircle(Color(0xFFFFF3C4).copy(alpha = 0.85f * alpha), 2.5f, Offset(sx * hw * 0.28f, -hl * 0.90f))
                        }
                        // STOP badge on the flank
                        drawRoundRect(
                            color = Color(0xFFD63030).copy(alpha = alpha),
                            topLeft = Offset(-hw - 2f, hl * 0.20f),
                            size = Size(15f, 15f),
                            cornerRadius = CornerRadius(3.5f),
                        )
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.92f * alpha),
                            topLeft = Offset(-hw + 1.5f, hl * 0.20f + 5.5f),
                            size = Size(8f, 4f),
                            cornerRadius = CornerRadius(2f),
                        )
                        // black chevron band across the rear
                        for (i in 0 until 4) {
                            drawRect(
                                color = Color(0xFF23262E).copy(alpha = 0.85f * alpha),
                                topLeft = Offset(-hw * 0.62f + i * (hw * 0.31f), hl * 0.80f),
                                size = Size(hw * 0.155f, 7f),
                            )
                        }
                    }
                }

                CarType.VAN -> {
                    // sliding-door rails + roof rails
                    for (sx in listOf(-1f, 1f)) {
                        drawRoundRect(
                            color = color.deep.copy(alpha = 0.25f * alpha),
                            topLeft = Offset(sx * (hw - 8f) - 1.5f, -hl * 0.18f),
                            size = Size(3f, hl * 0.66f),
                            cornerRadius = CornerRadius(1.5f),
                        )
                        drawRoundRect(
                            color = color.deep.copy(alpha = 0.22f * alpha),
                            topLeft = Offset(sx * hw * 0.38f - 1.25f, -hl * 0.10f),
                            size = Size(2.5f, hl * 0.46f),
                            cornerRadius = CornerRadius(1.25f),
                        )
                    }
                    if (isAmbulance) {
                        // white-red emergency lightbar
                        drawRoundRect(
                            color = Color(0xFF22262E).copy(alpha = 0.95f * alpha),
                            topLeft = Offset(-15f, -hl * 0.36f),
                            size = Size(30f, 10f),
                            cornerRadius = CornerRadius(5f),
                        )
                        drawRoundRect(
                            color = Color(0xFFF4F6F8).copy(alpha = alpha),
                            topLeft = Offset(-12.5f, -hl * 0.36f + 1.8f),
                            size = Size(12f, 6.4f),
                            cornerRadius = CornerRadius(3f),
                        )
                        drawRoundRect(
                            color = Color(0xFFFF4B3E).copy(alpha = alpha),
                            topLeft = Offset(0.5f, -hl * 0.36f + 1.8f),
                            size = Size(12f, 6.4f),
                            cornerRadius = CornerRadius(3f),
                        )
                        drawCircle(Color(0xFFFF6B5E).copy(alpha = 0.3f * alpha), 7.5f, Offset(6.5f, -hl * 0.36f + 5f))
                        // medical cross under the arrow stem
                        val cyy = hl * 0.32f
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.92f * alpha),
                            topLeft = Offset(-7.5f, cyy - 7.5f),
                            size = Size(15f, 15f),
                            cornerRadius = CornerRadius(4f),
                        )
                        drawRoundRect(
                            color = Color(0xFFD63030).copy(alpha = alpha),
                            topLeft = Offset(-2.2f, cyy - 5.2f),
                            size = Size(4.4f, 10.4f),
                            cornerRadius = CornerRadius(1.6f),
                        )
                        drawRoundRect(
                            color = Color(0xFFD63030).copy(alpha = alpha),
                            topLeft = Offset(-5.2f, cyy - 2.2f),
                            size = Size(10.4f, 4.4f),
                            cornerRadius = CornerRadius(1.6f),
                        )
                    }
                }

                CarType.SEDAN -> {
                    if (isTaxi) {
                        // roof sign + checker band
                        drawRoundRect(
                            color = color.deep.copy(alpha = 0.8f * alpha),
                            topLeft = Offset(-15f, -hl * 0.17f),
                            size = Size(30f, 17f),
                            cornerRadius = CornerRadius(6f),
                        )
                        drawRoundRect(
                            color = Color(0xFFFFF3C4).copy(alpha = alpha),
                            topLeft = Offset(-12.5f, -hl * 0.17f + 2f),
                            size = Size(25f, 13f),
                            cornerRadius = CornerRadius(5f),
                        )
                        for (i in 0 until 6) {
                            val qx = -13.5f + i * 4.5f
                            drawRect(
                                color = if (i % 2 == 0) Color(0xFF23262E) else Color.White.copy(alpha = 0.9f),
                                topLeft = Offset(qx, -hl * 0.36f),
                                size = Size(4.5f, 4.5f),
                            )
                        }
                    }
                    if (isPolice) {
                        drawRoundRect(
                            color = Color(0xFF22262E).copy(alpha = 0.95f * alpha),
                            topLeft = Offset(-15f, -hl * 0.40f),
                            size = Size(30f, 10f),
                            cornerRadius = CornerRadius(5f),
                        )
                        drawRoundRect(
                            color = Color(0xFFFF4B3E).copy(alpha = alpha),
                            topLeft = Offset(-12.5f, -hl * 0.40f + 1.8f),
                            size = Size(11.5f, 6.4f),
                            cornerRadius = CornerRadius(3f),
                        )
                        drawRoundRect(
                            color = Color(0xFF3B9BFF).copy(alpha = alpha),
                            topLeft = Offset(1f, -hl * 0.40f + 1.8f),
                            size = Size(11.5f, 6.4f),
                            cornerRadius = CornerRadius(3f),
                        )
                        drawCircle(Color(0xFFFF6B5E).copy(alpha = 0.35f * alpha), 7f, Offset(-6.8f, -hl * 0.40f + 5f))
                        drawCircle(Color(0xFF6FB6FF).copy(alpha = 0.35f * alpha), 7f, Offset(6.8f, -hl * 0.40f + 5f))
                    }
                    if (isSport) {
                        // twin racing stripes over hood and trunk
                        for (sx in listOf(-1f, 1f)) {
                            drawRoundRect(
                                color = Color.White.copy(alpha = 0.85f * alpha),
                                topLeft = Offset(sx * 9f - 3.5f, -hl * 0.97f),
                                size = Size(7f, hl * 0.11f),
                                cornerRadius = CornerRadius(3.5f),
                            )
                            drawRoundRect(
                                color = Color.White.copy(alpha = 0.85f * alpha),
                                topLeft = Offset(sx * 9f - 3.5f, hl * 0.70f),
                                size = Size(7f, hl * 0.12f),
                                cornerRadius = CornerRadius(3.5f),
                            )
                        }
                        // rear spoiler: two mounts + glossy blade
                        for (sx in listOf(-1f, 1f)) {
                            drawRoundRect(
                                color = color.deep.copy(alpha = 0.9f * alpha),
                                topLeft = Offset(sx * hw * 0.42f - 2.5f, hl * 0.82f),
                                size = Size(5f, 7f),
                                cornerRadius = CornerRadius(2.5f),
                            )
                        }
                        drawRoundRect(
                            color = color.deep.copy(alpha = alpha),
                            topLeft = Offset(-hw * 0.62f, hl * 0.87f),
                            size = Size(hw * 1.24f, 8f),
                            cornerRadius = CornerRadius(4f),
                        )
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.3f * alpha),
                            topLeft = Offset(-hw * 0.62f, hl * 0.87f),
                            size = Size(hw * 1.24f, 3f),
                            cornerRadius = CornerRadius(1.5f),
                        )
                    }
                }
            }
            // door handles
            if (type != CarType.BUS) {
                for (sx in listOf(-1f, 1f)) {
                    for (hy in listOf(-hl * 0.02f, hl * 0.20f)) {
                        drawRoundRect(
                            color = color.deep.copy(alpha = 0.45f * alpha),
                            topLeft = Offset(sx * (hw - 1f) - 4f, hy),
                            size = Size(8f, 3f),
                            cornerRadius = CornerRadius(1.5f),
                        )
                    }
                }
            }
            // ---------- mirrors
            for (sx in listOf(-1f, 1f)) {
                val mx = sx * (hw + 1f)
                drawRoundRect(
                    color = color.deep.copy(alpha = alpha),
                    topLeft = Offset(mx - 4.5f, -hl * 0.62f - 8f),
                    size = Size(9f, 16f),
                    cornerRadius = CornerRadius(4.5f),
                )
                drawRoundRect(
                    color = color.body.copy(alpha = alpha),
                    topLeft = Offset(mx - 3f, -hl * 0.62f - 6.5f),
                    size = Size(6f, 13f),
                    cornerRadius = CornerRadius(3f),
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.7f * alpha),
                    radius = 1.6f,
                    center = Offset(mx, -hl * 0.62f - 4f),
                )
            }
            // ---------- front fascia: LED headlight pods + grille
            for (sx in listOf(-1f, 1f)) {
                val lx = sx * (hw - 17f)
                drawCircle(color = Color(0xFFFFF0B8).copy(alpha = 0.20f * alpha), radius = 9.5f, center = Offset(lx, -hl + 6f))
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFFFFFBE0), Color(0xFFFFD668)),
                        startY = -hl + 2f,
                        endY = -hl + 14f,
                    ),
                    alpha = alpha,
                    topLeft = Offset(lx - 9f, -hl + 2f),
                    size = Size(18f, 12f),
                    cornerRadius = CornerRadius(6f),
                )
                drawCircle(color = Color.White.copy(alpha = 0.9f * alpha), radius = 2.6f, center = Offset(lx - 2.5f, -hl + 5.5f))
            }
            if (type != CarType.BUS) {
                drawRoundRect(
                    color = Color(0xFF1F242C).copy(alpha = 0.85f * alpha),
                    topLeft = Offset(-17f, -hl + 3.5f),
                    size = Size(34f, 8.5f),
                    cornerRadius = CornerRadius(4f),
                )
                drawLine(Color(0xFF57606D).copy(alpha = 0.8f * alpha), Offset(-7f, -hl + 5.5f), Offset(-7f, -hl + 10.5f), strokeWidth = 1.8f)
                drawLine(Color(0xFF57606D).copy(alpha = 0.8f * alpha), Offset(0f, -hl + 5.5f), Offset(0f, -hl + 10.5f), strokeWidth = 1.8f)
                drawLine(Color(0xFF57606D).copy(alpha = 0.8f * alpha), Offset(7f, -hl + 5.5f), Offset(7f, -hl + 10.5f), strokeWidth = 1.8f)
            }
            // ---------- rear fascia: LED taillights + license plate
            for (sx in listOf(-1f, 1f)) {
                val tx = sx * (hw - 15f)
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFFFF7A6A), Color(0xFFC2271C)),
                        startY = hl - 13f,
                        endY = hl - 4f,
                    ),
                    alpha = alpha,
                    topLeft = Offset(tx - 8.5f, hl - 13f),
                    size = Size(17f, 9f),
                    cornerRadius = CornerRadius(4.5f),
                )
                drawCircle(color = Color.White.copy(alpha = 0.35f * alpha), radius = 1.8f, center = Offset(tx - 2f, hl - 10.5f))
            }
            if (type != CarType.BUS) {
                drawRoundRect(
                    color = Color(0xFFEDEFF4).copy(alpha = alpha),
                    topLeft = Offset(-12f, hl * 0.86f - 5f),
                    size = Size(24f, 10f),
                    cornerRadius = CornerRadius(3f),
                )
                drawRoundRect(
                    color = Color(0xFF1F242C).copy(alpha = 0.35f * alpha),
                    topLeft = Offset(-12f, hl * 0.86f - 5f),
                    size = Size(24f, 10f),
                    cornerRadius = CornerRadius(3f),
                    style = Stroke(1.5f),
                )
                for (dx in listOf(-6f, 0f, 6f)) {
                    drawCircle(color = Color(0xFF3A414B).copy(alpha = 0.8f * alpha), radius = 1.3f, center = Offset(dx, hl * 0.86f))
                }
            }
            // ---------- embossed roof arrow
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

    /**
     * Premium toy-pawn passenger — the gummy citizen from the key art: capsule body
     * with rim light, arms with hands, glossy head with a happy two-dot face and a
     * soft hair sheen. Colour carries the matching meaning, so it stays untouched.
     */
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
            // arms with hands
            for (sx in listOf(-1f, 1f)) {
                withTransform({ rotate(sx * 14f, Offset(sx * 16f, 4f)) }) {
                    drawRoundRect(
                        color = color.dark.copy(alpha = 0.95f),
                        topLeft = Offset(sx * 16f - 4f, -2f),
                        size = Size(9f, 17f),
                        cornerRadius = CornerRadius(5f),
                    )
                    drawCircle(
                        color = color.body,
                        radius = 4.4f,
                        center = Offset(sx * 16f + 0.5f, 15f),
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
            // hair cap (top arc in the deep shade) + sheen
            drawArc(
                color = color.deep.copy(alpha = 0.5f),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(-9.5f, -23.5f),
                size = Size(19f, 19f),
                style = Stroke(6f),
            )
            drawOval(
                color = Color.White.copy(alpha = 0.5f),
                topLeft = Offset(-8.5f, -25f),
                size = Size(4.5f, 6.5f),
            )
            // happy face — eyes with catch-lights, tiny smile, soft blush
            for (sx in listOf(-1f, 1f)) {
                drawOval(
                    color = color.deep.copy(alpha = 0.85f),
                    topLeft = Offset(sx * 6.5f - 1.9f, -19.5f),
                    size = Size(3.8f, 5f),
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.85f),
                    radius = 0.9f,
                    center = Offset(sx * 6.5f - 0.5f, -18.3f),
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.16f),
                    radius = 2.1f,
                    center = Offset(sx * 9.5f, -10.5f),
                )
            }
            drawArc(
                color = color.deep.copy(alpha = 0.65f),
                startAngle = 25f,
                sweepAngle = 130f,
                useCenter = false,
                topLeft = Offset(-3.2f, -13.2f),
                size = Size(6.4f, 5.2f),
                style = Stroke(1.7f),
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

    // ------------------------------------------------------------ avatar frames

    /**
     * v3.0 decorative ring drawn around a profile avatar.
     * frameId 0 = plain gold ring; others add themed deco (see AvatarFrames).
     */
    fun DrawScope.drawAvatarFrame(frameId: Int, cx: Float, cy: Float, r: Float) {
        val ctr = Offset(cx, cy)
        when (frameId) {
            1 -> { // Sprinter — racing-blue ring with checkered studs
                drawCircle(Color(0xFF2E5FBB), radius = r * 1.04f, center = ctr, style = Stroke(r * 0.11f))
                drawCircle(Color(0xFF9FC8FF), radius = r * 1.04f, center = ctr, style = Stroke(r * 0.035f))
                for (i in 0..7) {
                    val a = Math.toRadians((i * 45.0))
                    val bx = cx + kotlin.math.cos(a).toFloat() * r * 1.04f
                    val by = cy + kotlin.math.sin(a).toFloat() * r * 1.04f
                    drawCircle(if (i % 2 == 0) Color.White else Color(0xFF14335F), radius = r * 0.075f, center = Offset(bx, by))
                }
            }

            2 -> { // Champion — royal gold ring with a crown on top
                drawCircle(Color(0xFFE8A50C), radius = r * 1.05f, center = ctr, style = Stroke(r * 0.13f))
                drawCircle(Color(0xFFFFE27A), radius = r * 1.05f, center = ctr, style = Stroke(r * 0.04f))
                val crownY = cy - r * 1.28f
                val crown = Path().apply {
                    moveTo(cx - r * 0.38f, crownY + r * 0.18f)
                    lineTo(cx - r * 0.38f, crownY - r * 0.06f)
                    lineTo(cx - r * 0.19f, crownY + r * 0.05f)
                    lineTo(cx, crownY - r * 0.16f)
                    lineTo(cx + r * 0.19f, crownY + r * 0.05f)
                    lineTo(cx + r * 0.38f, crownY - r * 0.06f)
                    lineTo(cx + r * 0.38f, crownY + r * 0.18f)
                    close()
                }
                drawPath(crown, Color(0xFFFFC93C))
                drawPath(crown, Color(0xFFB8860B), style = Stroke(r * 0.03f))
            }

            3 -> { // Frost — icy ring with snow dots
                drawCircle(Color(0xFF9BD8F5), radius = r * 1.04f, center = ctr, style = Stroke(r * 0.12f))
                drawCircle(Color.White, radius = r * 1.04f, center = ctr, style = Stroke(r * 0.03f))
                for (i in 0..5) {
                    val a = Math.toRadians((i * 60.0) + 15.0)
                    val bx = cx + kotlin.math.cos(a).toFloat() * r * 1.04f
                    val by = cy + kotlin.math.sin(a).toFloat() * r * 1.04f
                    drawCircle(Color.White, radius = r * 0.08f, center = Offset(bx, by))
                    drawCircle(Color(0xFFC9ECFF), radius = r * 0.045f, center = Offset(bx, by))
                }
            }

            4 -> { // Inferno — ember ring with little flame tongues
                drawCircle(Color(0xFF8C2B12), radius = r * 1.05f, center = ctr, style = Stroke(r * 0.13f))
                drawCircle(Color(0xFFFF7A2F), radius = r * 1.05f, center = ctr, style = Stroke(r * 0.05f))
                for (i in 0..5) {
                    val a = Math.toRadians((i * 60.0) - 30.0)
                    val bx = cx + kotlin.math.cos(a).toFloat() * r * 1.05f
                    val by = cy + kotlin.math.sin(a).toFloat() * r * 1.05f
                    drawCircle(Color(0xFFFFB02E), radius = r * 0.07f, center = Offset(bx, by))
                }
            }

            5 -> { // Midnight — deep violet ring with moon & stars
                drawCircle(Color(0xFF2B2350), radius = r * 1.05f, center = ctr, style = Stroke(r * 0.13f))
                drawCircle(Color(0xFF7E6FD0), radius = r * 1.05f, center = ctr, style = Stroke(r * 0.04f))
                drawCircle(Color(0xFFFFF3B8), radius = r * 0.16f, center = Offset(cx + r * 0.62f, cy - r * 0.78f))
                drawCircle(Color(0xFF2B2350), radius = r * 0.13f, center = Offset(cx + r * 0.70f, cy - r * 0.82f))
                for (i in 0..3) {
                    val a = Math.toRadians((i * 90.0) + 40.0)
                    val bx = cx + kotlin.math.cos(a).toFloat() * r * 1.05f
                    val by = cy + kotlin.math.sin(a).toFloat() * r * 1.05f
                    star(bx, by, r * 0.09f, Color(0xFFFFE27A))
                }
            }

            else -> { // 0 Rookie — clean gold ring
                drawCircle(Color(0xFFE8A50C), radius = r * 1.03f, center = ctr, style = Stroke(r * 0.10f))
                drawCircle(Color(0xFFFFF0C2), radius = r * 1.03f, center = ctr, style = Stroke(r * 0.03f))
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
