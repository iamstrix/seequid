package com.seequid.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HydrationCalculatorTest {

    private val settings = HydrationSettings(
        dailyGoalMl = 2400,
        wakeMinute = 8 * 60,
        sleepMinute = 20 * 60,
        fullScreenDeficitMl = 600,
    )

    @Test
    fun onPaceMeansEmptyScreen() {
        // Halfway through the waking day, half the goal drunk.
        val s = HydrationCalculator.state(settings, now = 0, minuteOfDay = 14 * 60.0, loggedMl = 1200)
        assertEquals(1200, s.expectedMl)
        assertEquals(0f, s.level, 0.0001f)
    }

    @Test
    fun deficitRaisesWaterProportionally() {
        val s = HydrationCalculator.state(settings, now = 0, minuteOfDay = 14 * 60.0, loggedMl = 900)
        assertEquals(0.5f, s.level, 0.0001f)
    }

    @Test
    fun drinkSizeMatters() {
        val small = HydrationCalculator.state(settings, 0, 14 * 60.0, loggedMl = 100)
        val large = HydrationCalculator.state(settings, 0, 14 * 60.0, loggedMl = 1000)
        assertTrue(large.level < small.level)
    }

    @Test
    fun levelIsClampedAtFull() {
        val s = HydrationCalculator.state(settings, 0, 19 * 60.0, loggedMl = 0)
        assertEquals(1f, s.level, 0.0001f)
    }

    @Test
    fun quietHoursHideTheWater() {
        val night = HydrationCalculator.state(settings, 0, 23 * 60.0, loggedMl = 0)
        assertTrue(night.quiet)
        assertEquals(0f, night.level, 0.0001f)
        val early = HydrationCalculator.state(settings, 0, 6 * 60.0, loggedMl = 0)
        assertTrue(early.quiet)
    }

    @Test
    fun goalIsCappedForSafety() {
        assertEquals(HydrationCalculator.MAX_GOAL_ML, HydrationCalculator.clampGoal(9000))
        assertEquals(HydrationCalculator.MIN_GOAL_ML, HydrationCalculator.clampGoal(100))
    }

    @Test
    fun demoModeFillsOverTenMinutes() {
        val demo = settings.copy(demoMode = true, demoStartedAt = 1_000L)
        val s = HydrationCalculator.state(demo, now = 1_000L + HydrationCalculator.DEMO_DAY_MS / 2, minuteOfDay = 3 * 60.0, loggedMl = 0)
        assertTrue("demo ignores quiet hours", !s.quiet)
        assertEquals(1200, s.expectedMl)
        assertEquals(1f, s.level, 0.0001f)
    }
}
