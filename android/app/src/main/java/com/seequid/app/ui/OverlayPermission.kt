package com.seequid.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Opens Android's "Display over other apps" screen and brings the user straight back to Seequid
 * once they switch it on, so nobody gets stranded in system settings.
 */
fun Context.requestOverlayPermission(scope: CoroutineScope) {
    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
    val app = applicationContext
    scope.launch {
        // Poll for up to two minutes. Holding the permission exempts us from Android's
        // background activity-start limits, so we're allowed to reopen ourselves.
        repeat(240) {
            delay(500)
            if (Settings.canDrawOverlays(app)) {
                app.startActivity(
                    Intent(app, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                )
                return@launch
            }
        }
    }
}
