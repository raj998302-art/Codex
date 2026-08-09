package com.codex.carjam.game

import androidx.compose.ui.graphics.Color
import java.util.Calendar

enum class EventId { COIN_RUSH, MYSTERY_MAYHEM, SLOT_SALE, LUCKY_LANE, DOUBLE_FRIDAY, WEEKEND_FEVER }

data class GameEvent(
    val id: EventId,
    val title: String,
    val subtitle: String,
    val coinMult: Float,
    val bonusSlots: Int,
    val mysteryBoost: Int,
    val accent: Color,
) {
    /** v3.3: rare boost days — the only days the EVENT hero theme may leak into the board pool. */
    fun isSpecial(): Boolean = coinMult >= 2f && bonusSlots >= 1
}

/**
 * Offline daily-rotating events. One event is live at any moment, chosen from the
 * weekday, so every day the game plays a little differently — classic live-ops hook.
 */
object Events {

    fun today(): GameEvent = forDay(Calendar.getInstance())

    fun forDay(cal: Calendar): GameEvent = when (cal.get(Calendar.DAY_OF_WEEK)) {
        Calendar.SATURDAY, Calendar.SUNDAY -> GameEvent(
            id = EventId.WEEKEND_FEVER,
            title = "WEEKEND FEVER",
            subtitle = "2× coins on every level!",
            coinMult = 2f,
            bonusSlots = 0,
            mysteryBoost = 0,
            accent = Color(0xFFFF7043),
        )

        Calendar.MONDAY -> GameEvent(
            id = EventId.MYSTERY_MAYHEM,
            title = "MYSTERY MAYHEM",
            subtitle = "Lots of hidden '?' cars today",
            coinMult = 1f,
            bonusSlots = 0,
            mysteryBoost = 5,
            accent = Color(0xFF9C6ADE),
        )

        Calendar.TUESDAY -> GameEvent(
            id = EventId.SLOT_SALE,
            title = "SLOT SALE",
            subtitle = "+1 bonus parking slot every level",
            coinMult = 1f,
            bonusSlots = 1,
            mysteryBoost = 0,
            accent = Color(0xFF42A5F5),
        )

        Calendar.WEDNESDAY -> GameEvent(
            id = EventId.LUCKY_LANE,
            title = "LUCKY LANE",
            subtitle = "1.5× coins today",
            coinMult = 1.5f,
            bonusSlots = 0,
            mysteryBoost = 0,
            accent = Color(0xFF4CAF50),
        )

        Calendar.THURSDAY -> GameEvent(
            id = EventId.SLOT_SALE,
            title = "PARKING PARTY",
            subtitle = "+1 bonus parking slot every level",
            coinMult = 1f,
            bonusSlots = 1,
            mysteryBoost = 0,
            accent = Color(0xFF26C6DA),
        )

        else -> GameEvent(
            id = EventId.DOUBLE_FRIDAY,
            title = "DOUBLE FRIDAY",
            subtitle = "2× coins on every level!",
            coinMult = 2f,
            bonusSlots = 0,
            mysteryBoost = 0,
            accent = Color(0xFFFFB300),
        )
    }

    fun msUntilMidnight(): Long {
        val now = Calendar.getInstance()
        val next = (now.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return next.timeInMillis - now.timeInMillis
    }

    /** Identifier of the current week, drives the leaderboard rotation. */
    fun weekId(): Int {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.YEAR) * 100 + cal.get(Calendar.WEEK_OF_YEAR)
    }

    fun msUntilNextWeek(): Long {
        val now = Calendar.getInstance()
        val next = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            var dow = get(Calendar.DAY_OF_WEEK)
            while (dow != Calendar.MONDAY) {
                add(Calendar.DAY_OF_YEAR, 1)
                dow = get(Calendar.DAY_OF_WEEK)
            }
        }
        return next.timeInMillis - now.timeInMillis
    }
}
