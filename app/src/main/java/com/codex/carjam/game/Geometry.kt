package com.codex.carjam.game

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

data class Pt(val x: Float, val y: Float) {
    operator fun plus(o: Pt) = Pt(x + o.x, y + o.y)
    operator fun minus(o: Pt) = Pt(x - o.x, y - o.y)
    operator fun times(s: Float) = Pt(x * s, y * s)
    fun dot(o: Pt) = x * o.x + y * o.y
    fun cross(o: Pt) = x * o.y - y * o.x
    fun len() = sqrt(x * x + y * y)
    fun norm(): Pt {
        val l = len()
        return if (l < 1e-6f) Pt(0f, -1f) else Pt(x / l, y / l)
    }
    fun rotate(deg: Float): Pt {
        val r = Math.toRadians(deg.toDouble())
        val c = cos(r).toFloat()
        val s = sin(r).toFloat()
        return Pt(x * c - y * s, x * s + y * c)
    }
}

fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
fun lerp(a: Pt, b: Pt, t: Float) = Pt(lerp(a.x, b.x, t), lerp(a.y, b.y, t))

/** Shortest-arc angle interpolation in degrees. */
fun lerpAngle(a: Float, b: Float, t: Float): Float {
    var d = (b - a) % 360f
    if (d > 180f) d -= 360f
    if (d < -180f) d += 360f
    return a + d * t
}

fun easeOutCubic(t: Float): Float {
    val u = 1f - t
    return 1f - u * u * u
}

fun easeInOutCubic(t: Float): Float =
    if (t < 0.5f) 4f * t * t * t else 1f - (-2f * t + 2f).let { it * it * it } / 2f

fun easeInCubic(t: Float) = t * t * t

fun quadBezier(p0: Pt, c: Pt, p1: Pt, t: Float): Pt {
    val u = 1f - t
    return p0 * (u * u) + c * (2f * u * t) + p1 * (t * t)
}

/** World-space facing vector for a car: angle 0 = up / north, 90 = right / east. */
fun facingVec(angleDeg: Float): Pt {
    val r = Math.toRadians(angleDeg.toDouble())
    return Pt(sin(r).toFloat(), -cos(r).toFloat())
}

/** The four corners of an oriented box centred at [c], half-length [hl] along its facing axis. */
fun obbCorners(c: Pt, halfLen: Float, halfWid: Float, angleDeg: Float): List<Pt> {
    val f = facingVec(angleDeg)
    val r = Pt(-f.y, f.x) // right side of the car
    return listOf(
        c + f * halfLen + r * halfWid,
        c + f * halfLen - r * halfWid,
        c - f * halfLen - r * halfWid,
        c - f * halfLen + r * halfWid,
    )
}

fun centroid(poly: List<Pt>): Pt {
    var sx = 0f
    var sy = 0f
    for (p in poly) {
        sx += p.x
        sy += p.y
    }
    return Pt(sx / poly.size, sy / poly.size)
}

private fun projectOnto(poly: List<Pt>, axis: Pt): Pair<Float, Float> {
    var lo = poly[0].dot(axis)
    var hi = lo
    for (i in 1 until poly.size) {
        val d = poly[i].dot(axis)
        if (d < lo) lo = d
        if (d > hi) hi = d
    }
    return lo to hi
}

/** Convex-polygon overlap using the separating axis theorem. */
fun polysOverlap(a: List<Pt>, b: List<Pt>): Boolean {
    // Cheap circle reject first.
    val ca = centroid(a)
    val cb = centroid(b)
    var ra = 0f
    for (p in a) ra = max(ra, (p - ca).len())
    var rb = 0f
    for (p in b) rb = max(rb, (p - cb).len())
    if ((ca - cb).len() > ra + rb) return false

    for (poly in listOf(a, b)) {
        for (i in poly.indices) {
            val e = poly[(i + 1) % poly.size] - poly[i]
            val axis = Pt(-e.y, e.x)
            if (axis.len() < 1e-6f) continue
            val (aLo, aHi) = projectOnto(a, axis)
            val (bLo, bHi) = projectOnto(b, axis)
            if (aHi < bLo || bHi < aLo) return false
        }
    }
    return true
}

/** Point containment for convex polygons (consistent winding not required). */
fun pointInPoly(p: Pt, poly: List<Pt>): Boolean {
    var sign = 0
    for (i in poly.indices) {
        val a = poly[i]
        val b = poly[(i + 1) % poly.size]
        val cross = (b - a).cross(p - a)
        if (abs(cross) < 1e-6f) continue
        val s = if (cross > 0f) 1 else -1
        if (sign == 0) sign = s else if (sign != s) return false
    }
    return true
}

/** Andrew's monotone chain convex hull. */
fun convexHull(points: List<Pt>): List<Pt> {
    if (points.size < 3) return points
    val pts = points.sortedWith(Comparator { p, q -> if (p.x != q.x) p.x.compareTo(q.x) else p.y.compareTo(q.y) })
    val lower = ArrayList<Pt>()
    for (p in pts) {
        while (lower.size >= 2 && ((lower[lower.size - 1] - lower[lower.size - 2]).cross(p - lower[lower.size - 1]) <= 0f)) {
            lower.removeAt(lower.size - 1)
        }
        lower.add(p)
    }
    val upper = ArrayList<Pt>()
    for (i in pts.size - 1 downTo 0) {
        val p = pts[i]
        while (upper.size >= 2 && ((upper[upper.size - 1] - upper[upper.size - 2]).cross(p - upper[upper.size - 1]) <= 0f)) {
            upper.removeAt(upper.size - 1)
        }
        upper.add(p)
    }
    lower.removeAt(lower.size - 1)
    upper.removeAt(upper.size - 1)
    return lower + upper
}

/**
 * True when [mover], swept along [dir] for [dist], never touches any of [others].
 * This is the core "is the car free to leave?" test – identical in spirit to the
 * reference game, whose cars slide out in their arrow direction only.
 */
fun sweptClear(mover: List<Pt>, dir: Pt, dist: Float, others: List<List<Pt>>): Boolean {
    if (others.isEmpty()) return true
    val shifted = mover.map { it + dir * dist }
    val swept = convexHull(mover + shifted)
    for (o in others) {
        if (polysOverlap(swept, o)) return false
    }
    return true
}

/** Distance from [c] along [dir] until a box of radius [pad] fully leaves [rect]. */
fun exitDistance(c: Pt, dir: Pt, rect: Pair<Pt, Pt>, pad: Float): Float {
    val (tl, br) = rect
    var t = Float.MAX_VALUE
    if (dir.x > 1e-6f) t = min(t, (br.x - c.x) / dir.x)
    if (dir.x < -1e-6f) t = min(t, (tl.x - c.x) / dir.x)
    if (dir.y > 1e-6f) t = min(t, (br.y - c.y) / dir.y)
    if (dir.y < -1e-6f) t = min(t, (tl.y - c.y) / dir.y)
    if (t == Float.MAX_VALUE || t < 0f) t = 600f
    return t + pad + 40f
}
