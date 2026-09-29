package com.seequid.app.overlay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.seequid.app.container
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Brings the water back after a reboot or app update if the user had it on. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return
        val pending = goAsync()
        val c = context.container
        c.appScope.launch {
            try {
                if (c.settings.settings.first().overlayEnabled && Settings.canDrawOverlays(context)) {
                    OverlayService.start(context)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
