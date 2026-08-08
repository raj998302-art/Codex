package com.codex.carjam.game

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Deterministic, always-solvable level factory.
 *
 * Strategy: scatter cars for the requested layout style, prove solvability by
 * simulating greedy elimination with the same swept-path rule the game uses,
 * then paint cars along that elimination order and build the passenger queue
 * from matching colour runs. If an attempt fails it is reshaped with a new seed.
 */
object LevelGenerator {

    private class P(
        val id: Int,
        var x: Float,
        var y: Float,
        var angle: Float,
        var type: CarType,
    ) {
        var color: CarColor = CarColor.RED
        var mystery: Boolean = false
        val hl: Float get() = type.len / 2f
        val hw: Float get() = type.wid / 2f
        val center: Pt get() = Pt(x, y)

        fun corners(scale: Float = 1f) = obbCorners(center, hl * scale, hw * scale, angle)

        fun exitDist() = exitDistance(center, facingVec(angle), Dim.arenaRect, hl + hw)
    }

    private val placeL get() = Dim.ARENA_LEFT + Dim.ARENA_PAD
    private val placeT get() = Dim.ARENA_TOP + Dim.ARENA_PAD
    private val placeR get() = Dim.ARENA_RIGHT - Dim.ARENA_PAD
    private val placeB get() = Dim.ARENA_BOTTOM - Dim.ARENA_PAD

    fun generate(level: Int, mysteryBoost: Int = 0): LevelSpec {
        val themes = LevelTheme.entries
        val styles = LayoutStyle.entries
        val theme = themes[(level - 1) % themes.size]
        val style = styles[(level - 1) % styles.size]
        val slotCount = when {
            level <= 2 -> 4
            level <= 6 -> 5
            else -> 6
        }
        val styleCap = when (style) {
            LayoutStyle.GRID -> 56
            LayoutStyle.DISC -> 46
            LayoutStyle.SPIRAL -> 46
            LayoutStyle.DIAGONAL -> 34
            LayoutStyle.HEART -> 40
        }
        val baseCount = min((10 + level * 2), styleCap).coerceAtLeast(12)
        val baseSeed = level * 7919 + 17

        for (attempt in 0 until 30) {
            val count = (baseCount - attempt / 6).coerceAtLeast(12)
            val rng = Random(baseSeed + attempt * 977)
            val placed = tryPlace(style, count, rng) ?: continue
            val order = eliminationOrder(placed, rng) ?: continue

            // Paint cars in elimination order, queue = matching colour runs.
            val queue = ArrayList<CarColor>(count * 4)
            val palette = CarColor.playable
            var prev: CarColor? = null
            for (idx in order) {
                val car = placed[idx]
                var c = palette[rng.nextInt(palette.size)]
                var guard = 0
                while (c == prev && guard++ < 30) c = palette[rng.nextInt(palette.size)]
                prev = c
                car.color = c
                repeat(car.type.seats) { queue.add(c) }
            }

            // Mystery ("?") cars on later levels – but never among the first escapees.
            if (level >= 4 || mysteryBoost > 0) {
                val mysteryCount = min(1 + level / 3 + mysteryBoost, min(order.size / 4, 12))
                val pool = order.subList((order.size * 0.25f).toInt(), order.size).toMutableList()
                repeat(mysteryCount) {
                    if (pool.isEmpty()) return@repeat
                    val pick = pool.removeAt(rng.nextInt(pool.size))
                    placed[pick].mystery = true
                }
            }

            val cars = placed.map { p ->
                CarSpec(
                    id = p.id, x = p.x, y = p.y, angleDeg = p.angle,
                    type = p.type, color = p.color, mystery = p.mystery,
                )
            }
            return LevelSpec(level, theme, style, slotCount, cars, queue)
        }

        // Deterministic emergency fallback: sparse all-north grid, trivially solvable.
        val rng = Random(baseSeed)
        val fallback = ArrayList<P>()
        val cols = 4
        val cw = (placeR - placeL) / cols
        val ch = (placeB - placeT) / 3
        var id = 0
        for (r in 0 until 3) {
            for (c in 0 until cols) {
                val p = P(
                    id++,
                    placeL + cw * (c + 0.5f),
                    placeT + ch * (r + 0.5f),
                    0f,
                    if (rng.nextFloat() < 0.8f) CarType.SEDAN else CarType.VAN,
                )
                fallback.add(p)
            }
        }
        val order = fallback.indices.toList()
        val queue = ArrayList<CarColor>()
        var prev: CarColor? = null
        for (idx in order) {
            val car = fallback[idx]
            var c = CarColor.playable[rng.nextInt(CarColor.playable.size)]
            var guard = 0
            while (c == prev && guard++ < 30) c = CarColor.playable[rng.nextInt(CarColor.playable.size)]
            prev = c
            car.color = c
            repeat(car.type.seats) { queue.add(c) }
        }
        return LevelSpec(
            level, theme, style, slotCount,
            fallback.map { p -> CarSpec(p.id, p.x, p.y, p.angle, p.type, p.color, false) },
            queue,
        )
    }

    // ------------------------------------------------------------------ placement

    private fun pickType(rng: Random, allowBig: Boolean): CarType {
        val r = rng.nextFloat()
        return when {
            allowBig && r < 0.12f -> CarType.BUS
            r < 0.32f -> CarType.VAN
            else -> CarType.SEDAN
        }
    }

    private fun insideArena(p: P): Boolean {
        for (corner in p.corners()) {
            if (corner.x < placeL || corner.x > placeR || corner.y < placeT || corner.y > placeB) return false
        }
        return true
    }

    private fun tryPlace(style: LayoutStyle, count: Int, rng: Random): List<P>? {
        val cells: List<Pt>? = when (style) {
            LayoutStyle.GRID -> gridCells(count, diagonal = false)
            LayoutStyle.DIAGONAL -> gridCells(count, diagonal = true)
            LayoutStyle.SPIRAL -> spiralPoints(count)
            LayoutStyle.HEART -> heartPoints(count)
            LayoutStyle.DISC -> null
        }
        val allowBig = when (style) {
            LayoutStyle.GRID -> {
                // only allow buses when a grid axis can swallow them
                val cols = gridCols(count)
                val rows = ceil(count.toDouble() / cols).toInt()
                val cw = (placeR - placeL) / cols
                val chh = (placeB - placeT) / rows
                max(cw, chh) >= CarType.BUS.len + 12f
            }

            LayoutStyle.DIAGONAL -> false
            else -> true
        }

        val types = List(count) { pickType(rng, allowBig) }
        val orderIdx = types.indices.sortedByDescending { types[it].len }
        val cars = arrayOfNulls<P>(count)
        val placedPolygons = ArrayList<List<Pt>>(count)

        for (idx in orderIdx) {
            val type = types[idx]
            var done = false
            var tries = 0
            while (!done && tries < 120) {
                tries++
                val cand = candidate(style, cells, idx, type, rng) ?: break
                val p = P(idx, cand.x, cand.y, cand.z, type)
                if (!insideArena(p)) continue
                val poly = p.corners(1.07f)
                var bad = false
                for (o in placedPolygons) {
                    if (polysOverlap(poly, o)) {
                        bad = true
                        break
                    }
                }
                if (bad) continue
                cars[idx] = p
                placedPolygons.add(p.corners(1.04f))
                done = true
            }
            if (!done) return null
        }
        return cars.map { it!! }
    }

    private data class Vec3(val x: Float, val y: Float, val z: Float)

    private fun candidate(
        style: LayoutStyle,
        cells: List<Pt>?,
        idx: Int,
        type: CarType,
        rng: Random,
    ): Vec3? {
        when (style) {
            LayoutStyle.GRID, LayoutStyle.DIAGONAL -> {
                val cell = cells!![idx % cells.size]
                val jx = (rng.nextFloat() - 0.5f) * 14f
                val jy = (rng.nextFloat() - 0.5f) * 14f
                val angles = if (style == LayoutStyle.GRID) {
                    listOf(0f, 90f, 180f, 270f)
                } else {
                    listOf(45f, 135f, 225f, 315f)
                }
                val fits = angles.filter { a ->
                    val r = Math.toRadians(a.toDouble())
                    val s = abs(sin(r)).toFloat()
                    val c = abs(cos(r)).toFloat()
                    val bw = s * type.len + c * type.wid + 8f
                    val bh = c * type.len + s * type.wid + 8f
                    bw <= gridCellW && bh <= gridCellH
                }
                if (fits.isEmpty()) return null
                val angle = fits[rng.nextInt(fits.size)] + (rng.nextFloat() - 0.5f) * 7f
                return Vec3(cell.x + jx, cell.y + jy, angle)
            }

            LayoutStyle.SPIRAL, LayoutStyle.HEART -> {
                // ring positions carry their tangent angle; when we run out, scatter
                if (cells != null && idx < cells.size) {
                    val p = cells[idx]
                    val tangent = if (style == LayoutStyle.SPIRAL) {
                        val cx = (placeL + placeR) / 2f
                        val cy = (placeT + placeB) / 2f
                        val th = Math.atan2((p.y - cy).toDouble(), (p.x - cx).toDouble())
                        var a = Math.toDegrees(th).toFloat() + 90f
                        if (rng.nextFloat() < 0.5f) a += 180f
                        if (rng.nextFloat() < 0.18f) a += 10f
                        a
                    } else {
                        // heart: tangential too but computed from neighbours – approximate radial
                        val cx = (placeL + placeR) / 2f
                        val cy = (placeT + placeB) / 2f + 60f
                        val th = Math.atan2((p.y - cy).toDouble(), (p.x - cx).toDouble())
                        Math.toDegrees(th).toFloat() + 90f + (rng.nextFloat() - 0.5f) * 24f
                    }
                    var angle = tangent % 360f
                    if (angle < 0) angle += 360f
                    return Vec3(p.x, p.y, angle)
                }
                return scatterCandidate(type, rng)
            }

            LayoutStyle.DISC -> return scatterCandidate(type, rng)
        }
    }

    private fun scatterCandidate(type: CarType, rng: Random): Vec3 {
        val cx = (placeL + placeR) / 2f
        val cy = (placeT + placeB) / 2f
        val rx = (placeR - placeL) / 2f - 30f
        val ry = (placeB - placeT) / 2f - 30f
        val a = rng.nextFloat() * (2f * Math.PI.toFloat())
        val rad = kotlin.math.sqrt(rng.nextFloat())
        val x = cx + cos(a) * rx * rad + (rng.nextFloat() - 0.5f) * 10f
        val y = cy + sin(a) * ry * rad + (rng.nextFloat() - 0.5f) * 10f
        val angle = (floor(rng.nextFloat() * 8f) * 45f) + (rng.nextFloat() - 0.5f) * 8f
        return Vec3(x, y, angle)
    }

    private var gridCellW = 150f
    private var gridCellH = 168f

    private fun gridCols(count: Int): Int = when {
        count <= 18 -> 5
        count <= 30 -> 6
        count <= 44 -> 7
        else -> 8
    }

    /** Cell centres, pre-shuffled, sized to the placement rect. */
    private fun gridCells(count: Int, diagonal: Boolean): List<Pt> {
        val cols = if (diagonal) {
            when {
                count <= 16 -> 4
                count <= 28 -> 5
                else -> 6
            }
        } else {
            gridCols(count)
        }
        val rows = ceil(count.toDouble() / cols).toInt()
        gridCellW = (placeR - placeL) / cols
        gridCellH = (placeB - placeT) / rows
        val cells = ArrayList<Pt>(cols * rows)
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                cells.add(Pt(placeL + gridCellW * (c + 0.5f), placeT + gridCellH * (r + 0.5f)))
            }
        }
        return cells.shuffled(Random(count * 31 + if (diagonal) 5 else 0))
    }

    private fun spiralPoints(count: Int): List<Pt> {
        val cx = (placeL + placeR) / 2f
        val cy = (placeT + placeB) / 2f
        val pts = ArrayList<Pt>()
        var r = min((placeR - placeL), (placeB - placeT)) / 2f - 60f
        while (r > 50f && pts.size < count) {
            val slots = max(3, floor((2f * Math.PI * r / 168f)).toInt())
            val off = r * 0.37f
            for (i in 0 until slots) {
                val th = off + i * (2f * Math.PI.toFloat()) / slots
                pts.add(Pt(cx + cos(th) * r, cy + sin(th) * r))
                if (pts.size >= count) break
            }
            r -= 118f
        }
        return pts
    }

    private fun heartPoints(count: Int): List<Pt> {
        val cx = (placeL + placeR) / 2f
        val cy = (placeT + placeB) / 2f + 30f
        val kx = (placeR - placeL) / 2f / 18.5f
        val ky = (placeB - placeT) / 2f / 15.5f
        val pts = ArrayList<Pt>()
        for (scale in listOf(1f, 0.64f, 0.3f)) {
            val per = max(6, floor((2f * Math.PI * scale * 30f)).toInt())
            for (i in 0 until per) {
                val t = i * (2f * Math.PI) / per
                val st = sin(t).toFloat()
                val x = 16f * st * st * st
                val y = 13f * cos(t).toFloat() - 5f * cos(2 * t).toFloat() - 2f * cos(3 * t).toFloat() - cos(4 * t).toFloat()
                pts.add(Pt(cx + x * kx * scale, cy - y * ky * scale * 0.9f))
                if (pts.size >= count) return pts
            }
        }
        return pts
    }

    // ------------------------------------------------------------------ solvability

    /** Greedy elimination proof. Returns an elimination order, or null when the jam locks. */
    private fun eliminationOrder(cars: List<P>, rng: Random): List<Int>? {
        data class Node(val id: Int, val corners: List<Pt>, val dir: Pt, val dist: Float)

        val remaining = cars.map { c ->
            Node(c.id, c.corners(), facingVec(c.angle), c.exitDist())
        }.toMutableList()
        val order = ArrayList<Int>(cars.size)
        var guard = 0
        while (remaining.isNotEmpty() && guard++ < cars.size * 4) {
            val clearIdx = ArrayList<Int>()
            for (i in remaining.indices) {
                val n = remaining[i]
                val others = ArrayList<List<Pt>>(remaining.size - 1)
                for (j in remaining.indices) if (j != i) others.add(remaining[j].corners)
                if (sweptClear(shrink(n.corners, 0.97f), n.dir, n.dist, others)) clearIdx.add(i)
            }
            if (clearIdx.isEmpty()) return null
            val pick = clearIdx[rng.nextInt(clearIdx.size)]
            order.add(remaining[pick].id)
            remaining.removeAt(pick)
        }
        return if (remaining.isEmpty()) order else null
    }

    private fun shrink(poly: List<Pt>, f: Float): List<Pt> {
        val c = centroid(poly)
        return poly.map { c + (it - c) * f }
    }
}
