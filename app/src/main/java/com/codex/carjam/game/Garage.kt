package com.codex.carjam.game

/**
 * The car collection. Each ride is a body+variant combo the painter already
 * knows how to draw (variants double as in-level dressing, so unlocked rides
 * also show up across the jam boards). Unlocks are one-time wallet spends;
 * the selected ride stars in the garage showroom and the profile.
 */
object Garage {

    data class Ride(
        val id: String,
        val title: String,
        val type: CarType,
        val variant: Int,
        val priceCoins: Int,
        val priceGems: Int,
        val blurb: String,
    )

    val rides = listOf(
        Ride("sedan", "CITY SEDAN", CarType.SEDAN, 0, 0, 0, "The everyday hero — free forever."),
        Ride("taxi", "TAXI CAB", CarType.SEDAN, 1, 500, 0, "Checker band, rooftop sign, pure attitude."),
        Ride("sport", "STREET SPORT", CarType.SEDAN, 2, 1500, 0, "Racing stripes and a proper spoiler."),
        Ride("police", "PATROL CAR", CarType.SEDAN, 4, 0, 20, "Red-blue lightbar, always on duty."),
        Ride("van", "FAMILY VAN", CarType.VAN, 0, 800, 0, "Sliding doors, five seats, zero drama."),
        Ride("ambulance", "RESCUE VAN", CarType.VAN, 1, 0, 30, "Flashing bar and a life-saving cross."),
        Ride("bus", "CITY BUS", CarType.BUS, 0, 2000, 0, "Six seats of pure jam power."),
        Ride("school", "SCHOOL BUS", CarType.BUS, 1, 0, 40, "STOP sign deployed — kids first!"),
    )

    fun rideOf(id: String): Ride = rides.firstOrNull { it.id == id } ?: rides.first()

    fun selected(prefs: Prefs): Ride = rideOf(prefs.selectedRide.value)

    fun owned(prefs: Prefs, ride: Ride): Boolean =
        ride.id == "sedan" || prefs.ownedRides.value.contains(ride.id)

    fun canAfford(prefs: Prefs, ride: Ride): Boolean =
        prefs.coins.intValue >= ride.priceCoins && prefs.gems.intValue >= ride.priceGems

    /** Spends the wallet and unlocks; returns false when unaffordable. */
    fun unlock(prefs: Prefs, ride: Ride): Boolean {
        if (owned(prefs, ride)) return true
        if (!canAfford(prefs, ride)) return false
        if (ride.priceGems > 0 && !prefs.spendGems(ride.priceGems)) return false
        if (ride.priceCoins > 0) prefs.addCoins(-ride.priceCoins)
        prefs.unlockRide(ride.id)
        return true
    }
}
