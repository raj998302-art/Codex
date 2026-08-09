package com.codex.carjam.game

/**
 * v3.0 profile avatar frames — collectible rings drawn around the DP
 * (see Painters.drawAvatarFrame). Unlock with coins/gems or by reaching
 * a level, then select one in Profile.
 */
data class AvatarFrame(
    val id: Int,
    val title: String,
    val coinPrice: Int = 0,
    val gemPrice: Int = 0,
    val unlockLevel: Int = 0,
)

object AvatarFrames {
    val ALL: List<AvatarFrame> = listOf(
        AvatarFrame(0, "ROOKIE"),
        AvatarFrame(1, "SPRINTER", coinPrice = 150),
        AvatarFrame(2, "CHAMPION", coinPrice = 300),
        AvatarFrame(3, "FROST", unlockLevel = 9),
        AvatarFrame(4, "INFERNO", unlockLevel = 21),
        AvatarFrame(5, "MIDNIGHT", gemPrice = 45),
    )

    fun byId(id: Int): AvatarFrame = ALL.firstOrNull { it.id == id } ?: ALL[0]

    /** Can this frame be taken home right now? (owned check happens at call site) */
    fun requirementMet(f: AvatarFrame, prefs: Prefs): Boolean = when {
        f.unlockLevel > 0 -> prefs.maxLevel.intValue >= f.unlockLevel
        f.coinPrice > 0 -> prefs.coins.intValue >= f.coinPrice
        f.gemPrice > 0 -> prefs.gems.intValue >= f.gemPrice
        else -> true
    }

    /** Pay the price (no-op for free / level frames). Returns false if unaffordable. */
    fun purchase(f: AvatarFrame, prefs: Prefs): Boolean = when {
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
