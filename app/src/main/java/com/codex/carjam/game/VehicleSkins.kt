package com.codex.carjam.game

/**
 * v3.3 vehicle skin catalog — collectible ride looks (all in-house names; the
 * hero-read super-lane nods to the real hypercar world with originals like
 * "VULCAN", "APEX GT", "PHANTOM V12" standing in for Bugatti / Lamborghini
 * / McLaren / Pagani / Ferrari energy without copying any brand).
 *
 * Skins ride on top of the base [CarType] shapes: OFFROAD/SUV/GT lean VAN,
 * the super lane leans SEDAN, mini/vintage get their own silhouettes.
 * `minLevel` gates both the shop tiles and the in-level skin draw
 * ([selectFor] never hands out a skin above the player's level).
 */
enum class VehicleSkin(
    val id: Int,
    val title: String,
    /** Preferred body for the shop tile preview when the catalog pins one. */
    val prem: CarType?,
    val minLevel: Int,
    val node: String,
) {
    OFFROAD(1, "TRAIL BOSS", CarType.VAN, 4, "roof rack + spare tire"),
    FORMULA(2, "GRID RACER", CarType.SEDAN, 6, "rear wing + nose cone"),
    GT(3, "GRAND TOURER", CarType.SEDAN, 10, "roof rails + stripe"),
    POD(4, "GLASS POD", CarType.VAN, 12, "black dome glass"),
    TRUCK(5, "CARGO KING", CarType.BUS, 14, "bolt-on container"),
    TURBO(6, "APEX GT", CarType.SEDAN, 16, "twin red go-stripes"),
    CLASSIC(7, "FIFTIES CRUISER", CarType.SEDAN, 18, "chrome + whitewalls"),
    MEGA(8, "PHANTOM V12", CarType.TOURIST, 20, "gold coach trim"),
    MINI(9, "POCKET JET", CarType.SEDAN, 26, "roof flag"),
    ;

    companion object {
        val catalog: List<VehicleSkin> get() = entries

        fun byId(id: Int): VehicleSkin? = entries.firstOrNull { it.id == id }

        /** Skins a level may drip onto its cars (style-aware spread, see generator). */
        fun poolFor(level: Int): List<VehicleSkin> = entries.filter { level >= it.minLevel }

        /**
         * Deterministic in-level skin for a car: levels below 10 keep the stock
         * look; after that every car rolls the level-scaled pool. Same level +
         * same car id = same skin, so the shop tile, the home road preview and
         * the jam board never disagree.
         */
        fun selectFor(level: Int, carId: Int): VehicleSkin? {
            val pool = poolFor(level)
            if (pool.isEmpty()) return null
            val i = ((level * 31 + carId * 7) % pool.size + pool.size) % pool.size
            return pool[i]
        }

        /** Home rowd banner shows the freshest unlockable skin at this level. */
        fun teaserFor(level: Int): VehicleSkin? = poolFor(level).maxByOrNull { it.minLevel }
    }
}

/** Where a skin tile sits in the Skin Shop. */
enum class SkinSource { LEVELS, EVENT, PACKS }

/** One tile in the Skin Shop grid (vehicle = ride look, scene = board/map swap). */
data class SkinTile(
    val id: String,
    val tab: SkinTab,
    val source: SkinSource,
    val title: String,
    /** For LEVELS/EVENT tiles: grid label like "2/10" -> numbers. For PACKS: 0. */
    val unlockLevel: Int = 0,
    val priceCoins: Int = 0,
    /** Vehicle tiles carry the skin id; scene tiles carry the LevelTheme name. */
    val ref: String,
)

enum class SkinTab { VEHICLE, SCENE }

object SkinShopCatalog {

    /** Vehicle tiles: LEVELS tiles (1st through 20th = starter + auto-switch),
     *  coin tiles, then pack teasers (? lock). */
    val vehicles: List<SkinTile> = listOf(
        SkinTile("veh_starter", SkinTab.VEHICLE, SkinSource.LEVELS, "CLASSIC DUO", unlockLevel = 1, ref = "starter"),
        SkinTile("veh_suv", SkinTab.VEHICLE, SkinSource.LEVELS, "TRAIL BOSS", unlockLevel = 4, ref = "1"),
        SkinTile("veh_racer", SkinTab.VEHICLE, SkinSource.LEVELS, "GRID RACER", unlockLevel = 6, ref = "2"),
        SkinTile("veh_gt", SkinTab.VEHICLE, SkinSource.LEVELS, "GRAND TOURER", unlockLevel = 10, ref = "3"),
        SkinTile("veh_pod", SkinTab.VEHICLE, SkinSource.LEVELS, "GLASS POD", unlockLevel = 12, ref = "4"),
        SkinTile("veh_cargo", SkinTab.VEHICLE, SkinSource.LEVELS, "CARGO KING", unlockLevel = 14, ref = "5"),
        SkinTile("veh_apex", SkinTab.VEHICLE, SkinSource.LEVELS, "APEX GT", unlockLevel = 16, ref = "6"),
        SkinTile("veh_fifties", SkinTab.VEHICLE, SkinSource.LEVELS, "FIFTIES CRUISER", unlockLevel = 18, ref = "7"),
        SkinTile("veh_phantom", SkinTab.VEHICLE, SkinSource.LEVELS, "PHANTOM V12", unlockLevel = 20, ref = "8"),
        SkinTile("veh_jet", SkinTab.VEHICLE, SkinSource.LEVELS, "POCKET JET", unlockLevel = 26, ref = "9"),
        // coin lane (screenshot: 300 / 500 coin tiles lol)
        SkinTile("veh_c300", SkinTab.VEHICLE, SkinSource.PACKS, "STREET PACK", priceCoins = 300, ref = "c300"),
        SkinTile("veh_c500a", SkinTab.VEHICLE, SkinSource.PACKS, "FLEET PACK", priceCoins = 500, ref = "c500a"),
        SkinTile("veh_c500b", SkinTab.VEHICLE, SkinSource.PACKS, "CITY PACK", priceCoins = 500, ref = "c500b"),
        // event teasers — padlock until the matching day rolls around
        SkinTile("veh_ev_a", SkinTab.VEHICLE, SkinSource.EVENT, "ARCTIC CONVOY", ref = "ev_a"),
        SkinTile("veh_ev_b", SkinTab.VEHICLE, SkinSource.EVENT, "SAFARI RIG", ref = "ev_b"),
        SkinTile("veh_ev_c", SkinTab.VEHICLE, SkinSource.EVENT, "MIDNIGHT GP", ref = "ev_c"),
    )

    /** Scene tiles: one per LevelTheme (matching its unlock level) + specials. */
    val scenes: List<SkinTile> = listOf(
        SkinTile("sc_auto", SkinTab.SCENE, SkinSource.LEVELS, "AUTO SWITCH", unlockLevel = 1, ref = "auto"),
        SkinTile("sc_sea", SkinTab.SCENE, SkinSource.LEVELS, "HARBOR", unlockLevel = 1, ref = "SEA"),
        SkinTile("sc_zoo", SkinTab.SCENE, SkinSource.LEVELS, "SAFARI PARK", unlockLevel = 1, ref = "ZOO"),
        SkinTile("sc_fair", SkinTab.SCENE, SkinSource.LEVELS, "FUNFAIR", unlockLevel = 1, ref = "FUNFAIR"),
        SkinTile("sc_metro", SkinTab.SCENE, SkinSource.LEVELS, "METRO HUB", unlockLevel = 1, ref = "METRO"),
        SkinTile("sc_desert", SkinTab.SCENE, SkinSource.LEVELS, "DUNE RALLY", unlockLevel = 1, ref = "DESERT"),
        SkinTile("sc_beach", SkinTab.SCENE, SkinSource.LEVELS, "PALM SHORE", unlockLevel = 6, ref = "BEACH"),
        SkinTile("sc_winter", SkinTab.SCENE, SkinSource.LEVELS, "SNOW PASS", unlockLevel = 9, ref = "WINTER"),
        SkinTile("sc_jungle", SkinTab.SCENE, SkinSource.LEVELS, "CANOPY", unlockLevel = 13, ref = "JUNGLE"),
        SkinTile("sc_frozen", SkinTab.SCENE, SkinSource.LEVELS, "GLACIER", unlockLevel = 16, ref = "FROZEN"),
        SkinTile("sc_lava", SkinTab.SCENE, SkinSource.LEVELS, "MAGMA BAY", unlockLevel = 21, ref = "LAVA"),
        SkinTile("sc_night", SkinTab.SCENE, SkinSource.LEVELS, "NEON CITY", unlockLevel = 26, ref = "NIGHT"),
        SkinTile("sc_event", SkinTab.SCENE, SkinSource.LEVELS, "HERO STRIP", unlockLevel = 30, ref = "EVENT"),
        SkinTile("sc_packs", SkinTab.SCENE, SkinSource.PACKS, "SKYLINE PACK", priceCoins = 500, ref = "spe"),
    )

    fun isEventDay(event: Events2): Boolean = event.isSpecial()
}

/** Tiny indirection so the catalog doesn't import Events itself. */
typealias Events2 = GameEvent
