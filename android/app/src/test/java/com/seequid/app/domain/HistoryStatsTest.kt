package com.seequid.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryStatsTest {

    @Test
    fun streakCountsBackFromToday() {
        val s = History.stats(listOf(0, 2000, 2600, 2500, 3000), goalMl = 2500)
        assertEquals(3, s.currentStreak)
        assertEquals(3, s.bestStreak)
    }

    @Test
    fun unfinishedTodayDoesNotBreakTheStreak() {
        val s = History.stats(listOf(2500, 2500, 400), goalMl = 2500)
        assertEquals(2, s.currentStreak)
    }

    @Test
    fun aMissedDayEndsTheStreakButBestIsKept() {
        val s = History.stats(listOf(2500, 2500, 2500, 1000, 2500), goalMl = 2500)
        assertEquals(1, s.currentStreak)
        assertEquals(3, s.bestStreak)
    }

    @Test
    fun averageStartsFromTheFirstLoggedDay() {
        val s = History.stats(listOf(0, 0, 2000, 3000), goalMl = 2500)
        assertEquals(2500, s.averageMl)
        assertEquals(HistoryStats(0, 0, 0), History.stats(listOf(0, 0), goalMl = 2500))
    }
}
