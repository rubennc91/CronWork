package com.cronwork.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.cronwork.container
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Reboot, app update and clock/time zone changes: restore notification, alarms and widget. */
class SystemEventsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                context.container.timer.refresh()
            } finally {
                pending.finish()
            }
        }
    }
}
