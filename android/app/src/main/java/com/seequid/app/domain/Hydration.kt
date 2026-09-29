package com.seequid.app.domain

/**
 * User-tunable pacing. Times are minutes after local midnight.
 * The water level measures how far behind the user's intake is compared with
 * where it should be by now, spread evenly across their waking hours.
 */
data class HydrationSettings(
    val dailyGoalMl: Int = 2500,
    val wakeMinute: Int = 7 * 60,
    val sleepMinute: Int = 23 * 60,
    /** Deficit (ml) at which the water reaches the top of the screen. */
    val fullScreenDeficitMl: Int = 500,
    val demoMode: Boolean = false,
    val demoStartedAt: Long = 0L,
)

data class HydrationState(
    /** 0 = on pace, 1 = screen full. */
    val level: Float,
    val loggedMl: Int,
    val expectedMl: Int,
    val goalMl: Int,
    /** Outside waking hours: the overlay stays hidden and nothing accrues. */
    val quiet: Boolean,
)

object HydrationCalculator {
    const val MIN_GOAL_ML = 1000
    /** Upper bound on purpose: pushing people past ~4 L/day risks hyponatremia. */
    const val MAX_GOAL_ML = 4000
    /** In demo mode a whole day's goal comes due over this span. */
    const val DEMO_DAY_MS = 10 * 60_000L

    fun clampGoal(ml: Int): Int = ml.coerceIn(MIN_GOAL_ML, MAX_GOAL_ML)

    fun isQuiet(minuteOfDay: Int, wakeMinute: Int, sleepMinute: Int): Boolean =
        minuteOfDay < wakeMinute || minuteOfDay >= sleepMinute

    /** Share of the day's goal that should have been drunk by [minuteOfDay]. */
    fun dueFraction(minuteOfDay: Double, wakeMinute: Int, sleepMinute: Int): Double {
        val span = (sleepMinute - wakeMinute).coerceAtLeast(1)
        return ((minuteOfDay - wakeMinute) / span).coerceIn(0.0, 1.0)
    }

    fun level(expectedMl: Int, loggedMl: Int, fullScreenDeficitMl: Int): Float {
        val deficit = expectedMl - loggedMl
        return (deficit.toFloat() / fullScreenDeficitMl.coerceAtLeast(1)).coerceIn(0f, 1f)
    }

    /**
     * @param minuteOfDay local wall-clock minute (fractional) for the normal pace.
     * @param loggedMl intake counted for the current period (today, or since demo start).
     */
    fun state(settings: HydrationSettings, now: Long, minuteOfDay: Double, loggedMl: Int): HydrationState {
        val goal = clampGoal(settings.dailyGoalMl)
        val quiet: Boolean
        val fraction: Double
        if (settings.demoMode) {
            quiet = false
            fraction = ((now - settings.demoStartedAt).toDouble() / DEMO_DAY_MS).coerceIn(0.0, 1.0)
        } else {
            quiet = isQuiet(minuteOfDay.toInt(), settings.wakeMinute, settings.sleepMinute)
            fraction = dueFraction(minuteOfDay, settings.wakeMinute, settings.sleepMinute)
        }
        val expected = (goal * fraction).toInt()
        val level = if (quiet) 0f else level(expected, loggedMl, settings.fullScreenDeficitMl)
        return HydrationState(level, loggedMl, expected, goal, quiet)
    }
}
