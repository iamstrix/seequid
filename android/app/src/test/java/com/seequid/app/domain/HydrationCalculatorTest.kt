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
    fun scheduleOffPacesAcrossTheWholeDay() {
        val allDay = settings.copy(scheduleEnabled = false)
        val s = HydrationCalculator.state(allDay, 0, 3 * 60.0, loggedMl = 0)
        assertTrue("no quiet hours", !s.quiet)
        assertEquals(300, s.expectedMl) // 3 of 24 hours of 2400 ml
    }

    @Test
    fun bedtimeAfterMidnightKeepsTheDayGoing() {
        val nightOwl = settings.copy(wakeMinute = 10 * 60, sleepMinute = 26 * 60) // 10 AM to 2 AM
        assertEquals(2 * 60, HydrationCalculator.dayBoundaryMinute(nightOwl))
        // 1 AM is the last waking hour of the previous day: nearly the whole goal is due, not zero.
        val oneAm = HydrationCalculator.state(nightOwl, 0, 1 * 60.0, loggedMl = 2200)
        assertTrue(!oneAm.quiet)
        assertEquals(2250, oneAm.expectedMl)
        // 3 AM is asleep.
        assertTrue(HydrationCalculator.state(nightOwl, 0, 3 * 60.0, loggedMl = 0).quiet)
        // Normal bedtimes keep a midnight boundary.
        assertEquals(0, HydrationCalculator.dayBoundaryMinute(settings))
    }

    @Test
    fun fullScreenScalesWithTheGoal() {
        val defaults = HydrationSettings(dailyGoalMl = 3000)
        assertEquals(1200, HydrationCalculator.fullScreenDeficit(defaults, 3000))
        // Small goals keep a floor so a sip doesn't fill the whole screen.
        assertEquals(500, HydrationCalculator.fullScreenDeficit(defaults, 1000))
        // Every 250 ml drink moves the water: 1100 ml behind isn't pinned at 100% any more.
        val before = HydrationCalculator.level(expectedMl = 2550, loggedMl = 1450, fullScreenDeficitMl = 1200)
        val after = HydrationCalculator.level(expectedMl = 2550, loggedMl = 1700, fullScreenDeficitMl = 1200)
        assertTrue(before < 1f && after < before - 0.15f)
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
