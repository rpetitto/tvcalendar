package com.rpetitto.tvcalendar.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Re-establishes the periodic sync schedule after a device reboot. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            SyncScheduler.schedulePeriodicSync(context.applicationContext)
        }
    }
}
