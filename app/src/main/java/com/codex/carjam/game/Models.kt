package com.codex.carjam.game

import androidx.compose.ui.graphics.Color

/** Design canvas size. Everything is laid out in these units and letterboxed to the device. */
object Dim {
    const val VW = 1080f
    const val VH = 2280f

    // Vertical zones (design units, y grows downwards)
    const val HUD_H = 190f
    const val STATION_TOP = 0f
    const val STATION_BOTTOM = 640f
    const val SLOT_TOP = 640f
    const val SLOT_BOTTOM = 900f
    const val ROAD_TOP = 900f
    const val ROAD_BOTTOM = 1010f
    const val ARENA_LEFT = 14f
    const val ARENA_TOP = 1016f
    const val ARENA_RIGHT = 1066f
    const val ARENA_BOTTOM = 2262f

    const val ARENA_PAD = 34f // keep-away margin when placing cars

    // Queue of waiting passengers in the station zone
    const val QUEUE_Y = 556f
    const val QUEUE_X0 = 168f
    const val QUEUE_GAP = 47f
    const val MAX_VISIBLE_QUEUE = 19
    const val QUEUE_SPAWN_X = 1090f

    // Station sign
    const val SIGN_X = 262f
    const val SIGN_Y = 342f

    // HUD anchor points (used for coin-flight targets)
    const val COIN_X = 930f
    const val COIN_Y = 96f

    val arenaRect = Pt(ARENA_LEFT, ARENA_TOP) to Pt(ARENA_RIGHT, ARENA_BOTTOM)

    fun slotCenters(count: Int): List<Pt> {
        val margin = 44f
        val usable = VW - margin * 2
        val gap = usable / count
        val cy = (SLOT_TOP + SLOT_BOTTOM) / 2f + 6f
        return List(count) { i -> Pt(margin + gap * (i + 0.5f), cy) }
    }
}

/** Vehicle palette, sampled from the reference game's saturated toy colours. */
enum class CarColor(val body: Color, val dark: Color, val deep: Color) {
    RED(Color(0xFFFF4757), Color(0xFFD63030), Color(0xFF9E1E1E)),
    ORANGE(Color(0xFFFF9F2E), Color(0xFFE57A00), Color(0xFFB25E00)),
    YELLOW(Color(0xFFFFD32E), Color(0xFFEBAC00), Color(0xFFB88600)),
    LIME(Color(0xFFA8E63B), Color(0xFF80BE1E), Color(0xFF5E9312)),
    GREEN(Color(0xFF2ED573), Color(0xFF1FA94F), Color(0xFF16793A)),
    CYAN(Color(0xFF35D0DC), Color(0xFF17A8B4), Color(0xFF0E7F88)),
    BLUE(Color(0xFF3B9BFF), Color(0xFF2273DB), Color(0xFF1857A6)),
    PURPLE(Color(0xFFBF6CF2), Color(0xFF9A45D1), Color(0xFF7330A0)),
    PINK(Color(0xFFFF7AD9), Color(0xFFE851BB), Color(0xFFB3388F)),
    GRAY(Color(0xFFB9C1C8), Color(0xFF8D959C), Color(0xFF6B7379));

    companion object {
        /** Colours that can appear in the passenger queue / on cars (no GRAY – reserved for mystery). */
        val playable = entries.filter { it != GRAY }
    }
}

enum class CarType(val seats: Int, val len: Float, val wid: Float) {
    SEDAN(seats = 4, len = 150f, wid = 84f),
    VAN(seats = 5, len = 168f, wid = 88f),
    BUS(seats = 6, len = 214f, wid = 90f),
}

enum class LayoutStyle { GRID, DIAGONAL, SPIRAL, DISC, HEART }

enum class Deco { SEA, ZOO, FUNFAIR, METRO, DESERT }

/** Full colour script for a level, mirroring the themed boards in the screenshots. */
enum class LevelTheme(
    val skyTop: Color,
    val skyBottom: Color,
    val slotBandTop: Color,
    val slotBandBottom: Color,
    val road: Color,
    val arenaBg: Color,
    val arenaEdge: Color,
    val signBoard: Color,
    val signPost: Color,
    val deco: Deco,
) {
    SEA(
        skyTop = Color(0xFF7FD8E6), skyBottom = Color(0xFF3FA6C8),
        slotBandTop = Color(0xFF1B7295), slotBandBottom = Color(0xFF125D7E),
        road = Color(0xFF5E7183),
        arenaBg = Color(0xFF4FB3D9), arenaEdge = Color(0xFF2B88B0),
        signBoard = Color(0xFF155E76), signPost = Color(0xFF0D4658),
        deco = Deco.SEA,
    ),
    ZOO(
        skyTop = Color(0xFFA8D479), skyBottom = Color(0xFF7CB24C),
        slotBandTop = Color(0xFF6FA03C), slotBandBottom = Color(0xFF5C8C30),
        road = Color(0xFF9AA38E),
        arenaBg = Color(0xFFE9EFC9), arenaEdge = Color(0xFFB9C78B),
        signBoard = Color(0xFF8A5A2B), signPost = Color(0xFF6B411C),
        deco = Deco.ZOO,
    ),
    FUNFAIR(
        skyTop = Color(0xFFF7C6DE), skyBottom = Color(0xFFEC9CC4),
        slotBandTop = Color(0xFFE39BC2), slotBandBottom = Color(0xFFD986B3),
        road = Color(0xFF8E6E80),
        arenaBg = Color(0xFFF9DCE9), arenaEdge = Color(0xFFDE9DBE),
        signBoard = Color(0xFFC94F7C), signPost = Color(0xFF8E2E53),
        deco = Deco.FUNFAIR,
    ),
    METRO(
        skyTop = Color(0xFFAAB8CC), skyBottom = Color(0xFF7E8FA8),
        slotBandTop = Color(0xFF65748C), slotBandBottom = Color(0xFF56647B),
        road = Color(0xFF5F6B7A),
        arenaBg = Color(0xFFD8DFE9), arenaEdge = Color(0xFF9FABBE),
        signBoard = Color(0xFF3E74B5), signPost = Color(0xFF2C578C),
        deco = Deco.METRO,
    ),
    DESERT(
        skyTop = Color(0xFFF6DCA8), skyBottom = Color(0xFFEABE7C),
        slotBandTop = Color(0xFFE4B573), slotBandBottom = Color(0xFFD59F5A),
        road = Color(0xFF9B8465),
        arenaBg = Color(0xFFFCF1D2), arenaEdge = Color(0xFFE0BB7E),
        signBoard = Color(0xFF9C6230), signPost = Color(0xFF73451F),
        deco = Deco.DESERT,
    ),
}

data class LevelSpec(
    val level: Int,
    val theme: LevelTheme,
    val layout: LayoutStyle,
    val slotCount: Int,
    val cars: List<CarSpec>,
    val queue: List<CarColor>,
    /** Arena side barred by the exit gate (0=N 1=E 2=S 3=W), -1 = no gate. */
    val gateSide: Int = -1,
    /** Arena exits required to lift the gate. */
    val gateNeed: Int = 0,
) {
    val totalPassengers: Int get() = queue.size
}

data class CarSpec(
    val id: Int,
    val x: Float,
    val y: Float,
    val angleDeg: Float,
    val type: CarType,
    val color: CarColor,
    val mystery: Boolean,
    /** >0 ⇒ frozen in ice: that many taps to crack before the car can move. */
    val frozen: Int = 0,
    /** >=0 ⇒ chain-locked: immobile until the car with this id leaves the arena. */
    val chainKey: Int = -1,
)
