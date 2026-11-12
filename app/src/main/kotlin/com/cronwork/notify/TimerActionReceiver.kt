package com.cronwork.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.cronwork.container
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Duration

/** Handles notification/widget buttons and alarms. */
class TimerActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val c = context.container
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    Actions.STOP -> {
                        c.timer.stop()
                        c.notifier.cancelForgot()
                        c.notifier.cancelEndOfDay()
                    }
                    Actions.TOGGLE -> c.timer.toggle()
                    Actions.START -> c.timer.start()
                    Actions.STILL_WORKING -> {
                        val st = c.timer.compute(c.timer.snapshot())
                        st.runningSince?.let { since ->
                            c.alarms.snoozeForgot(since.toEpochMilli(), c.settingsRepository.current().forgotReminderHours)
                        }
                        c.notifier.cancelForgot()
                        c.timer.refresh()
                    }
                    Actions.END_OF_DAY -> {
                        val st = c.timer.compute(c.timer.snapshot())
                        if (st.running && st.secondsToTarget <= 60) c.notifier.showEndOfDay()
                        c.timer.refresh()
                    }
                    Actions.FORGOT_CHECK -> {
                        val st = c.timer.compute(c.timer.snapshot())
                        val since = st.runningSince
                        if (since != null) {
                            c.notifier.showForgot(Duration.between(since, c.timer.now()).toHours())
                        }
                    }
                    else -> c.timer.refresh()
                }
            } finally {
                pending.finish()
            }
        }
    }
}
