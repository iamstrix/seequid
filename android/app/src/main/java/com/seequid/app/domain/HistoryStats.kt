package com.seequid.app.domain

/** Retention numbers from daily totals. */
data class HistoryStats(
    /** Days in a row the goal was met, ending today (or yesterday, while today is still in progress). */
    val currentStreak: Int,
    val bestStreak: Int,
    /** Average per day since the first day anything was logged; 0 with no history. */
    val averageMl: Int,
)

object History {
    /** @param dailyTotals oldest first, today last. */
    fun stats(dailyTotals: List<Int>, goalMl: Int): HistoryStats {
        if (dailyTotals.isEmpty()) return HistoryStats(0, 0, 0)
        val met = dailyTotals.map { it >= goalMl }

        // Today doesn't break the streak until it's over: count back from yesterday if today isn't met yet.
        var current = 0
        var i = if (met.last()) met.lastIndex else met.lastIndex - 1
        while (i >= 0 && met[i]) { current++; i-- }

        var best = 0
        var run = 0
        met.forEach { if (it) { run++; best = maxOf(best, run) } else run = 0 }

        val first = dailyTotals.indexOfFirst { it > 0 }
        val average = if (first < 0) 0 else dailyTotals.drop(first).average().toInt()
        return HistoryStats(current, best, average)
    }
}
