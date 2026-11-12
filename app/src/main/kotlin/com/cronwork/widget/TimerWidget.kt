package com.cronwork.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import com.cronwork.R
import com.cronwork.container
import com.cronwork.core.Settings
import com.cronwork.core.TimerState
import com.cronwork.core.formatHm
import com.cronwork.notify.Actions
import com.cronwork.timer.TimerSideEffects
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TimerWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
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

/** Pushes the timer state to every home screen widget. */
class WidgetUpdater(private val context: Context) : TimerSideEffects {
    override suspend fun sync(state: TimerState, settings: Settings) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, TimerWidgetProvider::class.java))
        if (ids.isEmpty()) return
        manager.updateAppWidget(ids, build(state))
    }

    private fun build(state: TimerState): RemoteViews {
        val v = RemoteViews(context.packageName, R.layout.widget_timer)
        val remaining = state.secondsToTarget
        val fromEarlierDay = state.openSessionFromEarlierDay
        v.setTextViewText(
            R.id.widget_label,
            when {
                state.running && fromEarlierDay -> "En marcha desde otro día"
                remaining > 0 -> "Quedan hoy"
                else -> "Extra hoy"
            },
        )
        if (state.running) {
            // Chronometer base is in elapsedRealtime: the moment the day's target is (or was) reached,
            // or the session start when it was left running from an earlier day.
            val base = if (fromEarlierDay) {
                SystemClock.elapsedRealtime() - (System.currentTimeMillis() - state.runningSince!!.toEpochMilli())
            } else {
                SystemClock.elapsedRealtime() + remaining * 1000
            }
            val countDown = !fromEarlierDay && remaining > 0
            v.setChronometer(R.id.widget_chrono, base, if (countDown || fromEarlierDay) null else "+%s", true)
            v.setChronometerCountDown(R.id.widget_chrono, countDown)
            v.setViewVisibility(R.id.widget_chrono, View.VISIBLE)
            v.setViewVisibility(R.id.widget_static, View.GONE)
            v.setImageViewResource(R.id.widget_toggle, R.drawable.ic_stop)
        } else {
            v.setChronometer(R.id.widget_chrono, SystemClock.elapsedRealtime(), null, false)
            val shown = state.today.remainingSeconds
            v.setTextViewText(R.id.widget_static, (if (shown < 0) "+" else "") + formatHm(shown))
            v.setViewVisibility(R.id.widget_chrono, View.GONE)
            v.setViewVisibility(R.id.widget_static, View.VISIBLE)
            v.setImageViewResource(R.id.widget_toggle, R.drawable.ic_play)
        }
        // Explicit start/stop matching what the widget shows, so a stale tap never does the opposite.
        v.setOnClickPendingIntent(
            R.id.widget_toggle,
            if (state.running) Actions.broadcast(context, Actions.STOP, 21) else Actions.broadcast(context, Actions.START, 22),
        )
        v.setOnClickPendingIntent(R.id.widget_root, Actions.openApp(context))
        return v
    }
}
