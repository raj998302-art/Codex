package com.codex.carjam.game

/**
 * v3.3 profile avatar frames — collectible rings drawn around the DP.
 * Source-tagged (see [source]) so the Info dialog groups them exactly the way
 * the reference badges shop does:
 *  u = free/starter   c = shop coins   g = gems   e = event   w = weekly winners   a = watch-ads
 */
data class AvatarFrame(
    val id: Int,
    val title: String,
    val coinPrice: Int = 0,
    val gemPrice: Int = 0,
    val unlockLevel: Int = 0,
    val adWatches: Int = 0,
    val source: String = "u",
)

object AvatarFrames {
    val ALL: List<AvatarFrame> = listOf(
        AvatarFrame(0, "ROOKIE", source = "u"),
        AvatarFrame(1, "SPRINTER", coinPrice = 150, source = "c"),
        AvatarFrame(2, "CHAMPION", coinPrice = 300, source = "c"),
        AvatarFrame(3, "FROST", unlockLevel = 9, source = "u"),
        AvatarFrame(4, "INFERNO", unlockLevel = 21, source = "u"),
        AvatarFrame(5, "MIDNIGHT", gemPrice = 45, source = "g"),
        // v3.3 special-source shelf (earned paths beyond plain prices)
        AvatarFrame(6, "SUMMER BEAT", source = "e"),          // event reward — special days
        AvatarFrame(7, "WEEKLY CROWN", source = "w"),         // weekly-winners podium prize
        AvatarFrame(8, "MEOW SQUAD", adWatches = 5, source = "a"),     // watch-ad strip
        AvatarFrame(9, "ROYAL VELVET", coinPrice = 400, source = "c"), // premium shop tile
    )

    fun byId(id: Int): AvatarFrame = ALL.firstOrNull { it.id == id } ?: ALL[0]

    fun framesOf(src: String): List<AvatarFrame> = ALL.filter { it.source == src }

    /** Can this frame be taken home right now? (owned check happens at call site) */
    fun requirementMet(f: AvatarFrame, prefs: Prefs): Boolean = when {
        f.unlockLevel > 0 -> prefs.maxLevel.intValue >= f.unlockLevel
        f.coinPrice > 0 -> prefs.coins.intValue >= f.coinPrice
        f.gemPrice > 0 -> prefs.gems.intValue >= f.gemPrice
        else -> false
    }

    /** Pay the price (no-op for free / level / earned-path frames). Returns false if unaffordable. */
    fun purchase(f: AvatarFrame, prefs: Prefs): Boolean = when {
        f.source == "e" || f.source == "w" || f.source == "a" -> false // earned, never bought
        f.unlockLevel > 0 -> prefs.maxLevel.intValue >= f.unlockLevel
        f.coinPrice > 0 -> {
            if (prefs.coins.intValue >= f.coinPrice) {
                prefs.addCoins(-f.coinPrice)
                true
            } else {
                false
            }
        }

        f.gemPrice > 0 -> {
            if (prefs.gems.intValue >= f.gemPrice) {
                prefs.addGems(-f.gemPrice)
                true
            } else {
                false
            }
        }

        else -> true
    }
}
