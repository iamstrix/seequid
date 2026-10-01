package com.seequid.app.domain

/**
 * User-tunable pacing. Times are minutes after local midnight; [sleepMinute] may run
 * past 24:00 (up to [HydrationCalculator.LATEST_SLEEP_MINUTE]) for people who go to bed after midnight.
 * The water level measures how far behind the user's intake is compared with
 * where it should be by now, spread evenly across their waking hours.
 */
data class HydrationSettings(
    val dailyGoalMl: Int = 2500,
    /** Off: no quiet hours, and the goal is paced across all 24 hours. */
    val scheduleEnabled: Boolean = true,
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
    /** Bedtime can be as late as 4 AM the next day. */
    const val LATEST_SLEEP_MINUTE = 28 * 60
    private const val DAY_MINUTES = 24 * 60

    fun clampGoal(ml: Int): Int = ml.coerceIn(MIN_GOAL_ML, MAX_GOAL_ML)

    /**
     * Wall-clock minute at which one hydration day hands over to the next. Midnight, unless
     * bedtime is after midnight: then the day lasts until bedtime, so a night owl's 1 AM drinks
     * still count towards the day they belong to.
     */
    fun dayBoundaryMinute(settings: HydrationSettings): Int =
        if (settings.scheduleEnabled) (settings.sleepMinute - DAY_MINUTES).coerceAtLeast(0) else 0

    /** [minuteOfDay] measured from the start of the current hydration day's calendar date (can exceed 24:00). */
    fun pacingMinute(settings: HydrationSettings, minuteOfDay: Double): Double =
        if (minuteOfDay < dayBoundaryMinute(settings)) minuteOfDay + DAY_MINUTES else minuteOfDay

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
     * @param loggedMl intake counted for the current period: the hydration day (see [dayBoundaryMinute]),
     *   or since demo start.
     */
    fun state(settings: HydrationSettings, now: Long, minuteOfDay: Double, loggedMl: Int): HydrationState {
        val goal = clampGoal(settings.dailyGoalMl)
        val quiet: Boolean
        val fraction: Double
        if (settings.demoMode) {
            quiet = false
            fraction = ((now - settings.demoStartedAt).toDouble() / DEMO_DAY_MS).coerceIn(0.0, 1.0)
        } else if (!settings.scheduleEnabled) {
            quiet = false
            fraction = dueFraction(minuteOfDay, 0, DAY_MINUTES)
        } else {
            val minute = pacingMinute(settings, minuteOfDay)
            quiet = isQuiet(minute.toInt(), settings.wakeMinute, settings.sleepMinute)
            fraction = dueFraction(minute, settings.wakeMinute, settings.sleepMinute)
        }
        val expected = (goal * fraction).toInt()
        val level = if (quiet) 0f else level(expected, loggedMl, settings.fullScreenDeficitMl)
        return HydrationState(level, loggedMl, expected, goal, quiet)
    }
}
