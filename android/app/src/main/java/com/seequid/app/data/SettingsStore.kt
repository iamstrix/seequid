package com.seequid.app.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.seequid.app.domain.HydrationCalculator
import com.seequid.app.domain.HydrationSettings
import com.seequid.app.overlay.LiquidSkin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

data class AppSettings(
    val hydration: HydrationSettings = HydrationSettings(),
    /** Window alpha of the water. Kept under Android's 0.8 touch-occlusion limit. */
    val opacity: Float = DEFAULT_OPACITY,
    /** Level at which the tappable drop handle appears. */
    val handleThreshold: Float = 0.4f,
    val skin: LiquidSkin = LiquidSkin.WATER,
    val overlayEnabled: Boolean = false,
    val onboardingDone: Boolean = false,
    val paywallSeen: Boolean = false,
    /** The first time water shows over other apps, Seequid explains what it is. */
    val waterIntroShown: Boolean = false,
) {
    companion object {
        const val DEFAULT_OPACITY = 0.55f
        const val MIN_OPACITY = 0.2f
        const val MAX_OPACITY = 0.75f
    }
}

class SettingsStore(private val context: Context) {

    private object Keys {
        val goal = intPreferencesKey("goal_ml")
        val wake = intPreferencesKey("wake_minute")
        val sleep = intPreferencesKey("sleep_minute")
        val schedule = booleanPreferencesKey("schedule_enabled")
        val demo = booleanPreferencesKey("demo_mode")
        val demoStart = longPreferencesKey("demo_started_at")
        val opacity = floatPreferencesKey("opacity")
        val threshold = floatPreferencesKey("handle_threshold")
        val skin = stringPreferencesKey("skin")
        val overlay = booleanPreferencesKey("overlay_enabled")
        val onboarding = booleanPreferencesKey("onboarding_done")
        val paywallSeen = booleanPreferencesKey("paywall_seen")
        val waterIntro = booleanPreferencesKey("water_intro_shown")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { it.toSettings() }

    private fun Preferences.toSettings(): AppSettings {
        val d = AppSettings()
        return AppSettings(
            hydration = HydrationSettings(
                dailyGoalMl = this[Keys.goal] ?: d.hydration.dailyGoalMl,
                scheduleEnabled = this[Keys.schedule] ?: d.hydration.scheduleEnabled,
                wakeMinute = this[Keys.wake] ?: d.hydration.wakeMinute,
                sleepMinute = this[Keys.sleep] ?: d.hydration.sleepMinute,
                demoMode = this[Keys.demo] ?: false,
                demoStartedAt = this[Keys.demoStart] ?: 0L,
            ),
            opacity = this[Keys.opacity] ?: d.opacity,
            handleThreshold = this[Keys.threshold] ?: d.handleThreshold,
            skin = LiquidSkin.fromName(this[Keys.skin]),
            overlayEnabled = this[Keys.overlay] ?: false,
            onboardingDone = this[Keys.onboarding] ?: false,
            paywallSeen = this[Keys.paywallSeen] ?: false,
            waterIntroShown = this[Keys.waterIntro] ?: false,
        )
    }

    suspend fun setGoal(ml: Int) = edit { it[Keys.goal] = HydrationCalculator.clampGoal(ml) }

    suspend fun setWakeHours(wakeMinute: Int, sleepMinute: Int) = edit {
        it[Keys.wake] = wakeMinute
        it[Keys.sleep] = sleepMinute.coerceIn(wakeMinute + 60, HydrationCalculator.LATEST_SLEEP_MINUTE)
    }

    suspend fun setScheduleEnabled(enabled: Boolean) = edit { it[Keys.schedule] = enabled }

    /** Turning demo on restarts its ten-minute clock so the water starts from empty. */
    suspend fun setDemoMode(enabled: Boolean) = edit {
        it[Keys.demo] = enabled
        if (enabled) it[Keys.demoStart] = System.currentTimeMillis()
    }

    suspend fun setOpacity(value: Float) = edit {
        it[Keys.opacity] = value.coerceIn(AppSettings.MIN_OPACITY, AppSettings.MAX_OPACITY)
    }

    suspend fun setHandleThreshold(value: Float) = edit { it[Keys.threshold] = value.coerceIn(0.1f, 0.9f) }
    suspend fun setSkin(skin: LiquidSkin) = edit { it[Keys.skin] = skin.name }
    suspend fun setOverlayEnabled(enabled: Boolean) = edit { it[Keys.overlay] = enabled }
    suspend fun setOnboardingDone() = edit { it[Keys.onboarding] = true }
    suspend fun setPaywallSeen() = edit { it[Keys.paywallSeen] = true }
    suspend fun setWaterIntroShown() = edit { it[Keys.waterIntro] = true }

    private suspend fun edit(block: (MutablePreferences) -> Unit) {
        context.dataStore.edit { block(it) }
    }
}
