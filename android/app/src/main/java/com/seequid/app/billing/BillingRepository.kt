package com.seequid.app.billing

import android.content.Context
import android.util.Log
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.awaitRestore
import com.seequid.app.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single source of truth for the `pro` entitlement. Everything Pro-gated —
 * skins in the live overlay, history, the paywall triggers — reads [isPro].
 */
class BillingRepository(context: Context) {

    /** False when no RevenueCat key is set in local.properties; the app still runs free-tier. */
    val isConfigured: Boolean = BuildConfig.REVENUECAT_API_KEY.isNotBlank()

    private val _isPro = MutableStateFlow(false)
    val isPro: StateFlow<Boolean> = _isPro.asStateFlow()

    init {
        if (isConfigured) {
            if (BuildConfig.DEBUG) Purchases.logLevel = LogLevel.DEBUG
            Purchases.configure(
                PurchasesConfiguration.Builder(context.applicationContext, BuildConfig.REVENUECAT_API_KEY).build()
            )
            Purchases.sharedInstance.updatedCustomerInfoListener = UpdatedCustomerInfoListener { info -> apply(info) }
        } else {
            Log.w(TAG, "revenuecat.apiKey missing from local.properties; purchases disabled")
        }
    }

    suspend fun refresh() {
        if (!isConfigured) return
        runCatching { Purchases.sharedInstance.awaitCustomerInfo() }
            .onSuccess(::apply)
            .onFailure { Log.w(TAG, "customer info fetch failed", it) }
    }

    /** @return true when a restore found an active `pro` entitlement. */
    suspend fun restore(): Result<Boolean> {
        if (!isConfigured) return Result.failure(IllegalStateException("Purchases not configured"))
        return runCatching { Purchases.sharedInstance.awaitRestore() }
            .onSuccess(::apply)
            .map { it.hasPro() }
    }

    private fun apply(info: CustomerInfo) {
        _isPro.value = info.hasPro()
    }

    private fun CustomerInfo.hasPro(): Boolean = entitlements[ENTITLEMENT_PRO]?.isActive == true

    companion object {
        const val ENTITLEMENT_PRO = "pro"
        private const val TAG = "SeequidBilling"
    }
}
