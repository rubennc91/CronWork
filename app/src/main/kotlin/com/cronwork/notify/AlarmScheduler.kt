package com.cronwork.notify

import android.app.AlarmManager
import android.content.Context
import android.os.Build
import com.cronwork.core.Settings
import com.cronwork.core.TimerState
import com.cronwork.timer.TimerSideEffects
import java.time.ZoneId
import java.time.ZonedDateTime

/** Schedules end-of-day, "forgot to stop?" and midnight refresh alarms from the timer state. */
class AlarmScheduler(private val context: Context) : TimerSideEffects {
    private val am = context.getSystemService(AlarmManager::class.java)
    private val prefs = context.getSharedPreferences("alarms", Context.MODE_PRIVATE)

    private fun endOfDayIntent() = Actions.broadcast(context, Actions.END_OF_DAY, 10)
    private fun forgotIntent() = Actions.broadcast(context, Actions.FORGOT_CHECK, 11)
    private fun dayChangedIntent() = Actions.broadcast(context, Actions.DAY_CHANGED, 12)

    fun canExact(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()

    override suspend fun sync(state: TimerState, settings: Settings) {
        val now = System.currentTimeMillis()
        val remaining = state.today.remainingSeconds
        if (state.running && !state.openSessionFromEarlierDay && remaining > 0) {
            set(now + remaining * 1000, endOfDayIntent())
        } else {
            am.cancel(endOfDayIntent())
        }

        val since = state.runningSince
        if (since != null && settings.forgotReminderHours > 0) {
            val base = since.toEpochMilli() + settings.forgotReminderHours * 3_600_000L
            val snoozed = if (prefs.getLong("snooze_session", -1) == since.toEpochMilli()) prefs.getLong("snooze_until", 0) else 0
            val at = maxOf(base, snoozed)
            if (at > now) set(at, forgotIntent()) else am.cancel(forgotIntent())
        } else {
            am.cancel(forgotIntent())
        }

        // Refresh shortly after local midnight so widget and notification move to the new day.
        val midnight = ZonedDateTime.now(ZoneId.systemDefault()).toLocalDate().plusDays(1)
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() + 5_000
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, midnight, dayChangedIntent())
    }

    /** "Still working": ask again after another [hours] for the session started at [sessionStartMillis]. */
    fun snoozeForgot(sessionStartMillis: Long, hours: Int) {
        prefs.edit()
            .putLong("snooze_session", sessionStartMillis)
            .putLong("snooze_until", System.currentTimeMillis() + hours * 3_600_000L)
            .apply()
    }

    private fun set(atMillis: Long, pi: android.app.PendingIntent) {
        if (canExact()) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
        }
    }
}
