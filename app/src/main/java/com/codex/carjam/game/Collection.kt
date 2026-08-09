package com.codex.carjam.game

/**
 * v3.0 card collection — 24 ride cards to discover while climbing levels
 * (mirrors the "CARDS / CARS" collection screen from the reference game).
 * A card flips from a gray "???" silhouette to a full-colour ride once the
 * player reaches its unlock level. Purely collectible — no buffs attached.
 */
data class RideCard(
    val no: Int,
    val title: String,
    val type: CarType,
    val color: CarColor,
    /** Variant seed steering the paint job (taxi / sport / police / ambulance / school). */
    val variant: Int,
    val unlockLevel: Int,
)

object RideCollection {
    val ALL: List<RideCard> = listOf(
        RideCard(1, "CITY SEDAN", CarType.SEDAN, CarColor.BLUE, 0, 1),
        RideCard(2, "SUNNY TAXI", CarType.SEDAN, CarColor.YELLOW, 1, 2),
        RideCard(3, "RED ROCKET", CarType.SEDAN, CarColor.RED, 2, 3),
        RideCard(4, "MINTY VAN", CarType.VAN, CarColor.GREEN, 0, 5),
        RideCard(5, "BEACH BUS", CarType.BUS, CarColor.ORANGE, 0, 7),
        RideCard(6, "SPEEDSTER", CarType.SEDAN, CarColor.CYAN, 2, 9),
        RideCard(7, "GRAPES VAN", CarType.VAN, CarColor.PURPLE, 0, 11),
        RideCard(8, "KINDIE BUS", CarType.BUS, CarColor.YELLOW, 1, 13),
        RideCard(9, "ROSE RIDE", CarType.SEDAN, CarColor.PINK, 0, 16),
        RideCard(10, "MEDIC VAN", CarType.VAN, CarColor.CYAN, 1, 19),
        RideCard(11, "METRO SEDAN", CarType.SEDAN, CarColor.GREEN, 0, 22),
        RideCard(12, "PATROL ONE", CarType.SEDAN, CarColor.BLUE, 4, 25),
        RideCard(13, "JUNGLE VAN", CarType.VAN, CarColor.GREEN, 2, 28),
        RideCard(14, "TOUR BUS", CarType.BUS, CarColor.CYAN, 0, 31),
        RideCard(15, "MIDNIGHT SPORT", CarType.SEDAN, CarColor.PURPLE, 2, 34),
        RideCard(16, "CREAM VAN", CarType.VAN, CarColor.YELLOW, 2, 37),
        RideCard(17, "LAVA BUS", CarType.BUS, CarColor.RED, 2, 40),
        RideCard(18, "FROST SEDAN", CarType.SEDAN, CarColor.CYAN, 1, 44),
        RideCard(19, "RADIO VAN", CarType.VAN, CarColor.ORANGE, 1, 48),
        RideCard(20, "ROYAL BUS", CarType.BUS, CarColor.PURPLE, 1, 52),
        RideCard(21, "NITRO SPORT", CarType.SEDAN, CarColor.RED, 2, 57),
        RideCard(22, "SILVER VAN", CarType.VAN, CarColor.LIME, 0, 63),
        RideCard(23, "NEON BUS", CarType.BUS, CarColor.PINK, 2, 70),
        RideCard(24, "GOLD RUSH", CarType.SEDAN, CarColor.YELLOW, 3, 80),
    )

    val TOTAL: Int get() = ALL.size

    fun unlockedCount(maxLevel: Int): Int = ALL.count { maxLevel >= it.unlockLevel }
}
