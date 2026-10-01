package com.seequid.app.data

import com.seequid.app.domain.HydrationCalculator
import com.seequid.app.domain.HydrationSettings
import com.seequid.app.domain.HydrationState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class DayTotal(val date: LocalDate, val totalMl: Int)

/**
 * The level is never stored: it is recomputed from the clock and the drink log,
 * so it stays correct after process death, a reboot or a timezone change.
 */
class HydrationRepository(
    private val dao: DrinkDao,
    private val settingsStore: SettingsStore,
) {
    // A month of history plus today; over-inclusive is fine, the list is small.
    private val recentDrinks: Flow<List<Drink>> =
        dao.observeSince(System.currentTimeMillis() - (HISTORY_DAYS + 1) * DAY_MS)

    private fun ticker(periodMs: Long): Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(periodMs)
        }
    }

    val state: Flow<HydrationState> =
        combine(settingsStore.settings, recentDrinks, ticker(1_000)) { settings, drinks, now ->
            val h = settings.hydration
            val zone = ZoneId.systemDefault()
            val local = Instant.ofEpochMilli(now).atZone(zone)
            val minuteOfDay = local.hour * 60 + local.minute + local.second / 60.0
            val since = periodStart(h, now)
            val logged = drinks.filter { it.timestamp >= since }.sumOf { it.amountMl }
            HydrationCalculator.state(h, now, minuteOfDay, logged)
        }.distinctUntilChanged()

    /** Totals for the last [HISTORY_DAYS] local days, oldest first, today included. */
    val history: Flow<List<DayTotal>> = recentDrinks.map { drinks ->
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val byDay = drinks.groupBy { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() }
        (HISTORY_DAYS - 1 downTo 0).map { back ->
            val day = today.minusDays(back.toLong())
            DayTotal(day, byDay[day].orEmpty().sumOf { it.amountMl })
        }
    }

    suspend fun log(amountMl: Int) =
        dao.insert(Drink(timestamp = System.currentTimeMillis(), amountMl = amountMl))

    /**
     * Removes the latest drink of the current day (never one from an earlier day).
     * @return the amount removed, or null when there was nothing to undo today.
     */
    suspend fun undoLast(): Int? {
        val h = settingsStore.settings.first().hydration
        val drink = dao.latestSince(periodStart(h, System.currentTimeMillis())) ?: return null
        dao.delete(drink.id)
        return drink.amountMl
    }

    /** Start of the period whose drinks count: the demo start, or the hydration day (midnight or a late bedtime). */
    private fun periodStart(h: HydrationSettings, now: Long): Long {
        if (h.demoMode) return h.demoStartedAt
        val zone = ZoneId.systemDefault()
        val local = Instant.ofEpochMilli(now).atZone(zone)
        val minuteOfDay = local.hour * 60 + local.minute
        val boundary = HydrationCalculator.dayBoundaryMinute(h)
        val date = if (minuteOfDay < boundary) local.toLocalDate().minusDays(1) else local.toLocalDate()
        return date.atStartOfDay(zone).plusMinutes(boundary.toLong()).toInstant().toEpochMilli()
    }

    companion object {
        const val HISTORY_DAYS = 30
        private const val DAY_MS = 24 * 60 * 60 * 1000L
    }
}
