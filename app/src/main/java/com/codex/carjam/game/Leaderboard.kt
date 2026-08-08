package com.codex.carjam.game

import kotlin.random.Random

/**
 * Weekly leaderboard. Fully offline: a fresh field of 49 rivals is generated every
 * week (seeded by the week id) and the player climbs it with their [Prefs.rating].
 * Swap [weeklyBoard] for Play Games Services calls later without touching the UI.
 */
object Leaderboard {

    private val names = listOf(
        "Aarav", "Vivaan", "Diya", "Arjun", "Ananya", "Kabir", "Ishaan", "Myra",
        "Sai", "Rudra", "Navya", "Aryan", "Kiara", "Reyansh", "Anika", "Advait",
        "Sara", "Dhruv", "Prisha", "Yash", "Meera", "Rohan", "Zara", "Veer",
        "Ira", "Ayaan", "Nora", "Dev", "Tara", "Arnav", "Ivy", "Ria", "Om",
        "Aisha", "Parth", "Mahi", "Laksh", "Nyra", "Zian", "Freya", "Ritvik",
        "Aadhya", "Krish", "Eva", "Shaurya", "Jiya", "Manav", "Saanvi", "Tejas",
        "Ahana", "Viaan", "Pari", "Neel", "Rhea",
    )

    data class Entry(val rank: Int, val name: String, val score: Int, val isYou: Boolean)

    /** Sorted board + the player's 1-based rank. */
    fun weeklyBoard(weekId: Int, youScore: Int, youName: String = "You"): Pair<List<Entry>, Int> {
        val rng = Random(weekId * 2654435761L + 17)
        // Rival scores spread around a wide band so any player level feels competitive.
        val cap = maxOf(900, (youScore * 1.35f).toInt() + 700)
        val rivals = names.shuffled(rng).take(49).map { n ->
            n to (rng.nextInt(150, cap) + rng.nextInt(0, 300))
        }.toMutableList()
        rivals.add(youName to youScore)
        val sorted = rivals.sortedByDescending { it.second }
        val board = sorted.mapIndexed { i, e ->
            Entry(rank = i + 1, name = e.first, score = e.second, isYou = e.first == youName)
        }
        val yourRank = board.first { it.isYou }.rank
        return board to yourRank
    }

    /** Coin prize by final weekly rank, paid out when the week flips (flavour for now). */
    fun prizeForRank(rank: Int): Int = when {
        rank == 1 -> 500
        rank <= 3 -> 250
        rank <= 10 -> 100
        rank <= 25 -> 40
        else -> 10
    }
}
