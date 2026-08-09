package com.codex.carjam.game

/**
 * v3.3 card collection — SEVENTY ride cards to discover while climbing levels
 * (mirrors the "CARDS / CARS" collection wall from the reference game; locked
 * cards show a gray "???" silhouette, unlocked ones flip to the full-colour
 * ride). The back half of the book mixes the vehicle skins (Trail Boss, Grid
 * Racer…) with in-house hero machines standing in for the dream garage —
 * VULCAN / MATADOR SV / AERO 54 / ZEPHYR R / KAISER SL / BAVARIA M8 — more
 * cards than the opponent's 70-slot wall, all earned by playing.
 */
data class RideCard(
    val no: Int,
    val title: String,
    val type: CarType,
    val color: CarColor,
    /** Variant seed steering the paint job (taxi / sport / police / ambulance / school). */
    val variant: Int,
    val unlockLevel: Int,
    /** Optionally a [VehicleSkin] id — cards >= 25 mostly ride the skin looks. */
    val skin: Int = -1,
)

object RideCollection {
    val ALL: List<RideCard> = listOf(
        // ---- first set: everyday rides (v3.0 book, renumbered to match the wall)
        RideCard(1, "CITY SEDAN", CarType.SEDAN, CarColor.BLUE, 0, 1),
        RideCard(2, "SUNNY TAXI", CarType.SEDAN, CarColor.YELLOW, 1, 2),
        RideCard(3, "RED ROCKET", CarType.SEDAN, CarColor.RED, 2, 3),
        RideCard(4, "MINTY VAN", CarType.VAN, CarColor.GREEN, 0, 5),
        RideCard(5, "BEACH BUS", CarType.BUS, CarColor.ORANGE, 0, 7),
        RideCard(6, "SPEEDSTER", CarType.SEDAN, CarColor.CYAN, 2, 8),
        RideCard(7, "GRAPES VAN", CarType.VAN, CarColor.PURPLE, 0, 11),
        RideCard(8, "KINDIE BUS", CarType.BUS, CarColor.YELLOW, 1, 13),
        RideCard(9, "ROSE RIDE", CarType.SEDAN, CarColor.PINK, 0, 15),
        RideCard(10, "MEDIC VAN", CarType.VAN, CarColor.CYAN, 1, 17),
        RideCard(11, "METRO SEDAN", CarType.SEDAN, CarColor.GREEN, 0, 19),
        RideCard(12, "PATROL ONE", CarType.SEDAN, CarColor.BLUE, 4, 21),
        // ---- mid set: skin looks roll in
        RideCard(13, "TRAIL BOSS", CarType.VAN, CarColor.LIME, 0, 22, skin = 1),
        RideCard(14, "GRID RACER", CarType.SEDAN, CarColor.ORANGE, 2, 23, skin = 2),
        RideCard(15, "JUNGLE VAN", CarType.VAN, CarColor.GREEN, 2, 25),
        RideCard(16, "TOUR BUS", CarType.BUS, CarColor.CYAN, 0, 27),
        RideCard(17, "GRAND TOURER", CarType.SEDAN, CarColor.PURPLE, 2, 28, skin = 3),
        RideCard(18, "GLASS POD", CarType.VAN, CarColor.BLUE, 0, 29, skin = 4),
        RideCard(19, "CREAM VAN", CarType.VAN, CarColor.YELLOW, 2, 31),
        RideCard(20, "LAVA BUS", CarType.BUS, CarColor.RED, 2, 33),
        RideCard(21, "CARGO KING", CarType.BUS, CarColor.GRAY, 0, 34, skin = 5),
        RideCard(22, "APEX GT", CarType.SEDAN, CarColor.RED, 2, 35, skin = 6),
        RideCard(23, "FROST SEDAN", CarType.SEDAN, CarColor.CYAN, 1, 36),
        RideCard(24, "RADIO VAN", CarType.VAN, CarColor.ORANGE, 1, 37),
        // ---- the dream lane opens: in-house hero machines (the "more than
        // the opponent has" shelf — every one a playable look, never a logo)
        RideCard(25, "VULCAN", CarType.SEDAN, CarColor.ORANGE, 2, 38, skin = 6),
        RideCard(26, "MATADOR SV", CarType.SEDAN, CarColor.LIME, 2, 39, skin = 2),
        RideCard(27, "FIFTIES CRUISER", CarType.SEDAN, CarColor.PINK, 0, 40, skin = 7),
        RideCard(28, "AERO 54", CarType.SEDAN, CarColor.ORANGE, 2, 41, skin = 2),
        RideCard(29, "ZEPHYR R", CarType.SEDAN, CarColor.CYAN, 2, 42, skin = 6),
        RideCard(30, "KAISER SL", CarType.SEDAN, CarColor.GRAY, 2, 43, skin = 3),
        RideCard(31, "BAVARIA M8", CarType.SEDAN, CarColor.BLUE, 2, 44, skin = 6),
        RideCard(32, "GT FALCON", CarType.SEDAN, CarColor.RED, 2, 45, skin = 2),
        RideCard(33, "PHANTOM V12", CarType.TOURIST, CarColor.PURPLE, 0, 46, skin = 8),
        RideCard(34, "STALLION X", CarType.SEDAN, CarColor.RED, 2, 47, skin = 6),
        RideCard(35, "ROYAL BUS", CarType.BUS, CarColor.PURPLE, 1, 48),
        RideCard(36, "POCKET JET", CarType.SEDAN, CarColor.YELLOW, 0, 49, skin = 9),
        RideCard(37, "NIGHTFALL GT", CarType.SEDAN, CarColor.BLUE, 2, 51, skin = 3),
        RideCard(38, "SILVER VAN", CarType.VAN, CarColor.LIME, 0, 53),
        RideCard(39, "DESERT WOLF", CarType.VAN, CarColor.ORANGE, 0, 55, skin = 1),
        RideCard(40, "NEON BUS", CarType.BUS, CarColor.PINK, 2, 57),
        RideCard(41, "COMET R", CarType.SEDAN, CarColor.CYAN, 2, 59, skin = 2),
        RideCard(42, "TUNDRA RIG", CarType.VAN, CarColor.BLUE, 0, 61, skin = 1),
        RideCard(43, "MIDNIGHT GP", CarType.SEDAN, CarColor.PURPLE, 2, 63, skin = 6),
        RideCard(44, "SAFARI VAN", CarType.VAN, CarColor.LIME, 0, 65),
        RideCard(45, "BLADE S", CarType.SEDAN, CarColor.GRAY, 2, 67, skin = 2),
        RideCard(46, "MEGA LINER", CarType.TOURIST, CarColor.YELLOW, 1, 69),
        RideCard(47, "ROCKET VAN", CarType.VAN, CarColor.RED, 2, 71),
        RideCard(48, "TURBO FOX", CarType.SEDAN, CarColor.ORANGE, 2, 73, skin = 6),
        RideCard(49, "HARBOR BUS", CarType.BUS, CarColor.BLUE, 0, 75),
        RideCard(50, "VIPER RS", CarType.SEDAN, CarColor.LIME, 2, 77, skin = 2),
        RideCard(51, "CLOUD VAN", CarType.VAN, CarColor.CYAN, 0, 79),
        RideCard(52, "CRIMSON GT", CarType.SEDAN, CarColor.RED, 2, 81, skin = 3),
        RideCard(53, "TUNNEL KING", CarType.BUS, CarColor.GRAY, 2, 83, skin = 5),
        RideCard(54, "PIXEL DASH", CarType.SEDAN, CarColor.PINK, 2, 85, skin = 9),
        RideCard(55, "OLIVE DRAB", CarType.VAN, CarColor.LIME, 0, 87, skin = 1),
        RideCard(56, "CROWN CAB", CarType.SEDAN, CarColor.YELLOW, 1, 89),
        RideCard(57, "POLAR SWIFT", CarType.SEDAN, CarColor.CYAN, 2, 91, skin = 6),
        RideCard(58, "LAVA RUNNER", CarType.VAN, CarColor.ORANGE, 2, 93),
        RideCard(59, "STORM GT", CarType.SEDAN, CarColor.BLUE, 2, 95, skin = 2),
        RideCard(60, "QUARRY BUS", CarType.BUS, CarColor.GREEN, 0, 97, skin = 5),
        RideCard(61, "GOLD RUSH", CarType.SEDAN, CarColor.YELLOW, 3, 99),
        RideCard(62, "MISTRAL X", CarType.SEDAN, CarColor.PURPLE, 2, 101, skin = 6),
        RideCard(63, "DUNE RAM", CarType.VAN, CarColor.ORANGE, 0, 103, skin = 1),
        RideCard(64, "SKYBUS", CarType.TOURIST, CarColor.CYAN, 0, 105),
        RideCard(65, "EMBER S", CarType.SEDAN, CarColor.RED, 2, 107, skin = 2),
        RideCard(66, "HOPPER", CarType.SEDAN, CarColor.LIME, 0, 109, skin = 9),
        RideCard(67, "GRAND VAN", CarType.VAN, CarColor.PURPLE, 2, 111, skin = 4),
        RideCard(68, "RALLY BUS", CarType.BUS, CarColor.BLUE, 2, 113),
        RideCard(69, "ONYX GT", CarType.SEDAN, CarColor.GRAY, 2, 115, skin = 6),
        RideCard(70, "CENTURION", CarType.TOURIST, CarColor.YELLOW, 0, 120, skin = 8),
    )

    val TOTAL: Int get() = ALL.size

    fun unlockedCount(maxLevel: Int): Int = ALL.count { maxLevel >= it.unlockLevel }
}
