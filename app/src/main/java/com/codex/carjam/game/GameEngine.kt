package com.codex.carjam.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.min
import kotlin.random.Random

enum class Fx { TAP, BLOCKED, WHOOSH, BOARD, COIN, DEPART, REVEAL, WIN, LOSE, CRACK, HAMMER, SHUFFLE, CHAINED, CHAINBREAK, UNLOCK, ELIMINATE }

enum class GameResult { PLAYING, WON, LOST }

enum class CarPhase { IN_ARENA, EXITING, PARKED, DEPARTING, GONE }

class MoveSeg(
    val p0: Pt,
    val ctrl: Pt?,
    val p1: Pt,
    val dur: Float,
    val ease: Int, // 0 linear 1 in-out 2 in
    val a0: Float,
    val a1: Float,
)

class CarEnt(val spec: CarSpec) {
    var x = spec.x
    var y = spec.y
    var angle = spec.angleDeg
    var phase = CarPhase.IN_ARENA
    var revealed = !spec.mystery
    var frozenLeft = spec.frozen
    var seatsFilled = 0
    var slotIdx = -1
    var wobbleStart = -1f
    var popStart = -1f
    var departAt = -1f
    var onSeg1End: (() -> Unit)? = null

    var segs: List<MoveSeg>? = null
    var segIdx = 0
    var segT = 0f
    var onAnimEnd: (() -> Unit)? = null

    val trail = ArrayList<Pair<Pt, Float>>()
    val seatPops = HashMap<Int, Float>()

    val color: CarColor get() = if (revealed) spec.color else CarColor.GRAY
    val type: CarType get() = spec.type
    val hl: Float get() = type.len / 2f
    val hw: Float get() = type.wid / 2f
    val pos: Pt get() = Pt(x, y)
    val inArena: Boolean get() = phase == CarPhase.IN_ARENA

    fun corners(scale: Float = 1f) = obbCorners(pos, hl * scale, hw * scale, angle)
}

class PassengerEnt(val color: CarColor) {
    var x = Dim.QUEUE_SPAWN_X
    var tx = Dim.QUEUE_SPAWN_X
    var y = Dim.QUEUE_Y
    var popStart = -1f
}

class BoardAnim(val car: CarEnt, val color: CarColor, val from: Pt, val to: Pt, val seatIdx: Int) {
    val ctrl: Pt = Pt((from.x + to.x) / 2f, min(from.y, to.y) - 190f)
    var t = 0f
    val dur = 240f

    fun pos(): Pt = quadBezier(from, ctrl, to, easeOutCubic(t))
}

class CoinFly(val from: Pt, val value: Int, val delay: Float) {
    val to: Pt = Pt(Dim.COIN_X, Dim.COIN_Y)
    val ctrl: Pt = Pt((from.x + to.x) / 2f + 60f, min(from.y, to.y) - 260f)
    var t = 0f
    val dur = 480f
    var landed = false
    var birthMs = -1f

    fun pos(): Pt = quadBezier(from, ctrl, to, easeInOutCubic(t))
}

class Confetto(val rng: Random) {
    var x = rng.nextFloat() * Dim.VW
    var y = -30f - rng.nextFloat() * 500f
    val vy = 0.45f + rng.nextFloat() * 0.5f
    val vx = (rng.nextFloat() - 0.5f) * 0.25f
    var rot = rng.nextFloat() * 360f
    val vr = (rng.nextFloat() - 0.5f) * 0.6f
    val color = CarColor.playable[rng.nextInt(CarColor.playable.size)].body
    val w = 14f + rng.nextFloat() * 12f
    val h = 8f + rng.nextFloat() * 8f

    fun step(dt: Float) {
        x += vx * dt
        y += vy * dt
        rot += vr * dt
    }
}

/**
 * Runtime level state + physics. The Compose layer calls [tick] every frame and
 * [onTap] for arena taps; everything redraws from [frame].
 */
class GameEngine(
    val spec: LevelSpec,
    private val onFx: (Fx) -> Unit,
    private val onCoinLanded: (Int) -> Unit,
    private val onWin: () -> Unit,
) {
    private val rng = Random(spec.level * 131 + 7)

    var frame = mutableLongStateOf(0L)
        private set
    var result by mutableStateOf(GameResult.PLAYING)
        private set
    var coinsEarned by mutableIntStateOf(0)
        private set
    var ms = 0f
        private set
    var loseReason = ""
        private set

    // Live-ops modifiers (events / revive)
    var bonusSlots = 0
        private set
    var coinMult = 1f

    /** Locked spare spots the player paid to open (this attempt only). */
    var unlockedExtra = 0
        private set

    val cars: List<CarEnt> = spec.cars.map { CarEnt(it) }
    val waiting = ArrayList<PassengerEnt>()
    private val backlog = ArrayList<CarColor>()
    val boardAnims = ArrayList<BoardAnim>()
    val coins = ArrayList<CoinFly>()
    val confetti = ArrayList<Confetto>()

    var seatedCount = 0
        private set
    /** Cars that have left the arena — feeds the exit-gate counter. */
    var arenaExits = 0
        private set
    private var lastActionMs = 0f
    private var resultAtMs = 0f

    init {
        val first = spec.queue.take(Dim.MAX_VISIBLE_QUEUE)
        first.forEachIndexed { i, c ->
            waiting.add(PassengerEnt(c).apply {
                x = Dim.QUEUE_X0 + i * Dim.QUEUE_GAP
                tx = x
                popStart = -10000f
            })
        }
        backlog.addAll(spec.queue.drop(Dim.MAX_VISIBLE_QUEUE))
    }

    // ------------------------------------------------------------ queries

    fun remainingPassengers(): Int = waiting.size + backlog.size + boardAnims.size

    /** Usable parking slots right now (paid extras + revive bonuses included). */
    fun effectiveSlots(): Int = spec.slotCount + unlockedExtra + bonusSlots

    /** Every slot shown in the band, including the still-locked spares. */
    fun slotsTotalShown(): Int = spec.slotCount + spec.lockedSlots + bonusSlots

    fun slotCenters(): List<Pt> = Dim.slotCenters(slotsTotalShown())

    fun lockedRemaining(): Int = (spec.lockedSlots - unlockedExtra).coerceAtLeast(0)

    /** Centres of the still-locked spare slots (the tappable, padlock ones). */
    fun lockedSlotCenters(): List<Pt> {
        val n = lockedRemaining()
        if (n <= 0) return emptyList()
        return slotCenters().takeLast(n)
    }

    /** More Spot purchase: open one locked spare. False when none remain. */
    fun unlockSlot(): Boolean {
        if (result != GameResult.PLAYING || lockedRemaining() <= 0) return false
        unlockedExtra++
        lastActionMs = ms
        onFx(Fx.UNLOCK)
        return true
    }

    fun grantBonusSlots(n: Int) {
        bonusSlots += n
    }

    /** Second-chance hook (rewarded ad): more slots, straight back to play. */
    fun revive(extraSlots: Int = 2) {
        if (result != GameResult.LOST) return
        bonusSlots += extraSlots
        result = GameResult.PLAYING
        lastActionMs = ms
        onFx(Fx.REVEAL)
    }

    private fun freeSlotIndex(): Int? {
        val used = HashSet<Int>()
        for (c in cars) if (c.slotIdx >= 0 && c.phase != CarPhase.GONE) used.add(c.slotIdx)
        for (i in 0 until effectiveSlots()) if (!used.contains(i)) return i
        return null
    }

    private fun isClear(car: CarEnt): Boolean {
        val others = ArrayList<List<Pt>>()
        for (o in cars) {
            if (o !== car && o.inArena) others.add(o.corners())
        }
        val dir = facingVec(car.angle)
        val dist = exitDistance(car.pos, dir, Dim.arenaRect, car.hl + car.hw)
        if (!sweptClear(car.corners(0.96f), dir, dist, others)) return false
        // The exit gate bars one side until enough arena exits lift it.
        if (spec.gateSide >= 0 && !gateOpen() && exitSide(dir) == spec.gateSide) return false
        return true
    }

    // ------------------------------------------------------------ gate + chains

    fun gateOpen(): Boolean = spec.gateSide < 0 || arenaExits >= spec.gateNeed

    fun gateRemaining(): Int = (spec.gateNeed - arenaExits).coerceAtLeast(0)

    /** True while this car's chain key car is still sitting in the arena. */
    fun isChainedActive(car: CarEnt): Boolean {
        val keyId = car.spec.chainKey
        if (keyId < 0) return false
        val key = cars.firstOrNull { it.spec.id == keyId } ?: return false
        return key.phase == CarPhase.IN_ARENA
    }

    private fun busy(): Boolean {
        for (c in cars) if (c.phase == CarPhase.EXITING || c.phase == CarPhase.DEPARTING) return true
        return boardAnims.isNotEmpty()
    }

    // ------------------------------------------------------------ input

    /** Topmost arena car under a design-space point (booster targeting). */
    fun hitCarAt(x: Float, y: Float): CarEnt? {
        val p = Pt(x, y)
        for (c in cars.asReversed()) {
            if (c.inArena && pointInPoly(p, c.corners(1.06f))) return c
        }
        return null
    }

    fun onTap(x: Float, y: Float) {
        if (result != GameResult.PLAYING) return
        val car = hitCarAt(x, y) ?: return
        lastActionMs = ms

        // Ice-locked: every tap cracks the shell first; the car can't move yet.
        if (car.frozenLeft > 0) {
            car.frozenLeft--
            car.wobbleStart = ms
            onFx(Fx.CRACK)
            return
        }

        // Chain-locked: shackled until its key car leaves the arena.
        if (isChainedActive(car)) {
            car.wobbleStart = ms
            onFx(Fx.CHAINED)
            return
        }

        if (!car.revealed || !isClear(car) || freeSlotIndex() == null) {
            car.wobbleStart = ms
            onFx(Fx.BLOCKED)
            return
        }
        startExit(car)
    }

    // ------------------------------------------------------------ boosters

    /** Ice Hammer: shatter every ice layer on [car] in one blow. */
    fun smashIce(car: CarEnt): Boolean {
        if (result != GameResult.PLAYING) return false
        if (!car.inArena || car.frozenLeft <= 0) return false
        car.frozenLeft = 0
        car.wobbleStart = ms
        car.popStart = ms
        lastActionMs = ms
        onFx(Fx.HAMMER)
        return true
    }

    /**
     * Eliminate VIP: drive [car] straight out regardless of ice, chains, the
     * gate or traffic — needs one free parking slot. Mystery cars step out
     * revealed (hidden colour could never board afterwards otherwise). Still
     * counts as a normal exit: gate counter ticks and shackled chains snap.
     */
    fun eliminateCar(car: CarEnt): Boolean {
        if (result != GameResult.PLAYING) return false
        if (!car.inArena) return false
        if (freeSlotIndex() == null) return false
        car.frozenLeft = 0
        car.revealed = true
        onFx(Fx.ELIMINATE)
        startExit(car)
        return true
    }

    /**
     * Queue Mix: rebuild the waiting line so its front passenger is useful right
     * now (a parked car hungry for that colour, else a colour that can move),
     * the rest shuffled. Passenger counts are never touched, so the level stays
     * exactly as solvable as before.
     */
    fun shuffleQueue(): Boolean {
        if (result != GameResult.PLAYING) return false
        if (waiting.size + backlog.size < 2) return false
        val hungry = LinkedHashSet<CarColor>()
        for (c in cars) {
            if (c.phase == CarPhase.PARKED && c.revealed && c.seatsFilled < c.type.seats && boardAnims.none { it.car === c }) {
                hungry.add(c.color)
            }
        }
        val movable = LinkedHashSet<CarColor>()
        for (c in cars) {
            if (c.inArena && c.revealed && c.frozenLeft <= 0 && !isChainedActive(c) && isClear(c)) {
                movable.add(c.color)
            }
        }
        // find a "hero" passenger: prefer someone a parked car can take, from the
        // visible line first, else fish it out of the hidden backlog
        var hero: PassengerEnt? = null
        var heroFromBacklog = false
        for (pool in listOf(hungry, movable)) {
            if (hero != null) break
            if (pool.isEmpty()) continue
            for (p in waiting) {
                if (pool.contains(p.color)) {
                    hero = p
                    break
                }
            }
            if (hero == null) {
                var i = 0
                while (i < backlog.size) {
                    if (pool.contains(backlog[i])) {
                        val col = backlog.removeAt(i)
                        hero = PassengerEnt(col).apply {
                            x = Dim.QUEUE_X0
                            y = Dim.QUEUE_Y
                        }
                        heroFromBacklog = true
                        break
                    }
                    i++
                }
            }
        }
        val rest = waiting.filter { it !== hero }.toMutableList()
        rest.shuffle(rng)
        waiting.clear()
        hero?.let { waiting.add(it) }
        waiting.addAll(rest)
        if (heroFromBacklog && waiting.size > Dim.MAX_VISIBLE_QUEUE) {
            val dropped = waiting.removeAt(waiting.size - 1)
            backlog.add(0, dropped.color)
        }
        waiting.forEachIndexed { i, p ->
            p.tx = Dim.QUEUE_X0 + i * Dim.QUEUE_GAP
            p.popStart = ms
        }
        lastActionMs = ms
        onFx(Fx.SHUFFLE)
        return true
    }

    // ------------------------------------------------------------ flow

    private fun startExit(car: CarEnt) {
        val slot = freeSlotIndex() ?: return
        car.slotIdx = slot
        car.phase = CarPhase.EXITING
        arenaExits++
        // chains shackled to this key car snap the moment it rolls out
        var brokeAny = false
        for (c in cars) {
            if (c.spec.chainKey == car.spec.id && c.inArena) {
                c.popStart = ms
                brokeAny = true
            }
        }
        if (brokeAny) onFx(Fx.CHAINBREAK)
        val slotCenter = slotCenters()[slot]
        val dir = facingVec(car.angle)
        val dist = exitDistance(car.pos, dir, Dim.arenaRect, car.hl + car.hw)
        val p0 = car.pos
        val p1 = p0 + dir * dist
        val d1 = (dist / 2.6f).coerceIn(160f, 480f)
        val ctrl = Pt((p1.x + slotCenter.x) / 2f, min(p1.y, slotCenter.y) - 170f)
        car.segs = listOf(
            MoveSeg(p0, null, p1, d1, 0, car.angle, car.angle),
            MoveSeg(p1, ctrl, slotCenter, 430f, 1, car.angle, 0f),
        )
        car.segIdx = 0
        car.segT = 0f
        car.onSeg1End = { revealScan() }
        car.onAnimEnd = {
            car.phase = CarPhase.PARKED
            car.popStart = ms
            car.angle = 0f
        }
        onFx(Fx.TAP)
        onFx(Fx.WHOOSH)
        lastActionMs = ms
    }

    private fun startDepart(car: CarEnt) {
        if (car.phase != CarPhase.PARKED) return
        car.phase = CarPhase.DEPARTING
        val p0 = car.pos
        val p1 = Pt(Dim.VW + 320f, p0.y - 30f)
        val ctrl = Pt(p0.x + 240f, p0.y - 130f)
        car.segs = listOf(MoveSeg(p0, ctrl, p1, 520f, 2, car.angle, 90f))
        car.segIdx = 0
        car.segT = 0f
        car.onAnimEnd = {
            car.phase = CarPhase.GONE
            car.slotIdx = -1
            spawnCoins(car.pos, 5, maxOf(1, (2f * coinMult).toInt()))
        }
        onFx(Fx.DEPART)
        lastActionMs = ms
    }

    private fun revealScan() {
        var revealedAny = false
        for (c in cars) {
            if (c.inArena && !c.revealed && !revealedAny && isClear(c)) {
                c.revealed = true
                c.popStart = ms
                revealedAny = true
                onFx(Fx.REVEAL)
            }
        }
    }

    private fun spawnCoins(at: Pt, n: Int, value: Int) {
        for (i in 0 until n) {
            coins.add(
                CoinFly(
                    at + Pt((rng.nextFloat() - 0.5f) * 70f, (rng.nextFloat() - 0.5f) * 40f),
                    value,
                    i * 70f,
                ).apply { birthMs = ms },
            )
        }
    }

    // ------------------------------------------------------------ tick

    fun tick(dtMs: Float) {
        val dt = dtMs.coerceIn(0f, 60f)
        ms += dt

        // Car movement
        for (car in cars) {
            val segs = car.segs
            if (segs != null && (car.phase == CarPhase.EXITING || car.phase == CarPhase.DEPARTING)) {
                car.segT += dt
                val seg = segs[car.segIdx]
                var t = (car.segT / seg.dur).coerceIn(0f, 1f)
                val et = when (seg.ease) {
                    1 -> easeInOutCubic(t)
                    2 -> easeInCubic(t)
                    else -> t
                }
                val pos = if (seg.ctrl != null) quadBezier(seg.p0, seg.ctrl, seg.p1, et) else lerp(seg.p0, seg.p1, et)
                car.x = pos.x
                car.y = pos.y
                car.angle = lerpAngle(seg.a0, seg.a1, et)
                car.trail.add(pos to ms)
                if (t >= 1f) {
                    if (car.segIdx == 0 && segs.size > 1) {
                        car.onSeg1End?.invoke()
                        car.onSeg1End = null
                        car.segIdx = 1
                        car.segT = 0f
                    } else {
                        t = 1f
                        car.segs = null
                        car.onAnimEnd?.invoke()
                        car.onAnimEnd = null
                    }
                }
            } else {
                car.trail.clear()
            }
            // trim trail
            while (car.trail.isNotEmpty() && ms - car.trail.first().second > 520f) {
                car.trail.removeAt(0)
            }
            if (car.departAt in 0f..ms) {
                car.departAt = -1f
                startDepart(car)
            }
        }

        // Boarding: keep feeding parked cars whose colour matches the queue front
        run {
            val front = waiting.firstOrNull()?.color
            if (front != null) {
                for (car in cars) {
                    if (car.phase != CarPhase.PARKED) continue
                    if (car.seatsFilled >= car.type.seats) continue
                    if (!car.revealed) continue
                    if (car.color != front) continue
                    if (boardAnims.any { it.car === car }) continue
                    val p = waiting.removeAt(0)
                    val seatIdx = car.seatsFilled
                    val target = seatPos(car, seatIdx)
                    boardAnims.add(BoardAnim(car, p.color, Pt(p.x, p.y), target, seatIdx))
                    onFx(Fx.BOARD)
                    retargetQueue()
                    lastActionMs = ms
                    break
                }
            }
        }

        // Advance boarding flights
        val it = boardAnims.iterator()
        while (it.hasNext()) {
            val b = it.next()
            b.t += dt / b.dur
            if (b.t >= 1f) {
                it.remove()
                b.car.seatsFilled++
                b.car.seatPops[b.seatIdx] = ms
                seatedCount++
                if (b.car.seatsFilled >= b.car.type.seats) {
                    b.car.departAt = ms + 150f
                }
            }
        }

        // Queue spacing + refill from backlog
        for (p in waiting) {
            p.x += (p.tx - p.x) * min(1f, dt / 90f)
        }
        val doneCoins = ArrayList<CoinFly>()
        for (c in coins) {
            if (c.landed) continue
            if (ms < c.birthMs + c.delay) continue
            c.t += dt / c.dur
            if (c.t >= 1f) {
                c.landed = true
                doneCoins.add(c)
                coinsEarned += c.value
                onCoinLanded(c.value)
                onFx(Fx.COIN)
            }
        }
        coins.removeAll(doneCoins)

        if (result == GameResult.WON) {
            for (c in confetti) c.step(dt)
        }

        // Win / lose evaluation (only once the board settles)
        if (result == GameResult.PLAYING && !busy() && ms - lastActionMs > 450f) {
            revealScan()
            val remaining = remainingPassengers()
            if (remaining == 0 && cars.none { it.phase == CarPhase.PARKED || it.phase == CarPhase.EXITING || it.phase == CarPhase.DEPARTING }) {
                result = GameResult.WON
                resultAtMs = ms
                confetti.clear()
                repeat(70) { confetti.add(Confetto(rng)) }
                onFx(Fx.WIN)
                onWin()
            } else if (remaining > 0) {
                val freeSlot = freeSlotIndex()
                if (freeSlot == null) {
                    lose("No free parking slots!")
                } else {
                    val anyClear = cars.any { it.inArena && it.revealed && !isChainedActive(it) && isClear(it) }
                    if (!anyClear && cars.any { it.inArena }) {
                        lose("No car can move!")
                    }
                }
            }
        }

        frame.longValue = ms.toLong()
    }

    fun resultAge(): Float = ms - resultAtMs

    private fun lose(reason: String) {
        if (result != GameResult.PLAYING) return
        result = GameResult.LOST
        loseReason = reason
        resultAtMs = ms
        onFx(Fx.LOSE)
    }

    private fun retargetQueue() {
        while (waiting.size < Dim.MAX_VISIBLE_QUEUE && backlog.isNotEmpty()) {
            val c = backlog.removeAt(0)
            waiting.add(PassengerEnt(c).apply {
                popStart = ms
            })
        }
        waiting.forEachIndexed { i, p -> p.tx = Dim.QUEUE_X0 + i * Dim.QUEUE_GAP }
    }

    // ------------------------------------------------------------ seats

    fun seatPos(car: CarEnt, seatIdx: Int): Pt {
        val col = seatIdx % 2
        val row = seatIdx / 2
        val lx = if (col == 0) -17f else 17f
        val ly = car.hl * 0.60f - row * 38f
        return car.pos + Pt(lx, ly).rotate(car.angle)
    }
}
