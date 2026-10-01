package com.seequid.app

import android.app.Application
import android.content.Context
import com.seequid.app.billing.BillingRepository
import com.seequid.app.data.HydrationRepository
import com.seequid.app.data.SeequidDatabase
import com.seequid.app.data.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/** Manual dependency wiring; the app is small enough not to need a DI framework. */
class AppContainer(context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val settings = SettingsStore(context)
    val hydration = HydrationRepository(SeequidDatabase.create(context).drinks(), settings)
    val billing = BillingRepository(context)
    /** True while one of Seequid's own screens is visible: the overlay steps aside for them. */
    val appInForeground = MutableStateFlow(false)
    private var visibleScreens = 0

    /** Called from each activity's onStart/onStop; the main app and the quick-log sheet can overlap. */
    fun screenVisible(visible: Boolean) {
        visibleScreens = (visibleScreens + if (visible) 1 else -1).coerceAtLeast(0)
        appInForeground.value = visibleScreens > 0
    }
}

class SeequidApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.appScope.launch { container.billing.refresh() }
    }
}

val Context.container: AppContainer
    get() = (applicationContext as SeequidApp).container
