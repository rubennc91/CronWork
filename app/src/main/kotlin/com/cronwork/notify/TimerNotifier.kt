package com.cronwork.notify

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.cronwork.R
import com.cronwork.core.Settings
import com.cronwork.core.TimerState
import com.cronwork.core.formatSigned
import com.cronwork.timer.TimerSideEffects

/** Ongoing countdown notification plus the end-of-day and "forgot to stop?" alerts. */
class TimerNotifier(private val context: Context) : TimerSideEffects {

    companion object {
        const val CHANNEL_TIMER = "timer"
        const val CHANNEL_ALERTS = "alerts"
        const val ID_TIMER = 1
        const val ID_END_OF_DAY = 2
        const val ID_FORGOT = 3
    }

    private val nm = NotificationManagerCompat.from(context)

    fun createChannels() {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_TIMER, "Contador en marcha", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Cuenta atrás de la jornada mientras el contador está en marcha"
                setShowBadge(false)
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ALERTS, "Avisos", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Fin de jornada y avisos de contador olvidado"
            },
        )
    }

    private fun canNotify(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    override suspend fun sync(state: TimerState, settings: Settings) {
        if (!state.running) {
            // Stopped from anywhere (app, widget, notification, tile): clear every timer notification.
            nm.cancel(ID_TIMER)
            nm.cancel(ID_END_OF_DAY)
            nm.cancel(ID_FORGOT)
            return
        }
        if (!canNotify()) return
        val remaining = state.secondsToTarget
        val fromEarlierDay = state.openSessionFromEarlierDay
        // "when" is the moment the day's target is reached; the system chronometer counts towards/away from it.
        // A session left running from an earlier day adds nothing to today, so it just counts up from its start.
        val whenMillis = if (fromEarlierDay) state.runningSince!!.toEpochMilli() else System.currentTimeMillis() + remaining * 1000
        val countDown = !fromEarlierDay && remaining > 0
        val builder = NotificationCompat.Builder(context, CHANNEL_TIMER)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setContentIntent(Actions.openApp(context))
            .addAction(0, "Parar", Actions.broadcast(context, Actions.STOP, 1))
            .setShowWhen(true)
            .setWhen(whenMillis)
            .setUsesChronometer(true)
            .setChronometerCountDown(countDown)
            .setContentTitle(
                when {
                    fromEarlierDay -> "En marcha desde otro día"
                    remaining > 0 -> "Trabajando · queda para la jornada"
                    else -> "Jornada cumplida · tiempo extra"
                },
            )
            .setContentText(
                if (fromEarlierDay) "¿Olvidaste pararlo? Toca para corregirlo."
                else "Semana ${formatSigned(state.weekBalanceSeconds)} · Bolsa ${formatSigned(state.totalBalanceSeconds)}",
            )
        nm.notify(ID_TIMER, builder.build())
    }

    @SuppressLint("MissingPermission")
    fun showEndOfDay() {
        if (!canNotify()) return
        nm.notify(
            ID_END_OF_DAY,
            NotificationCompat.Builder(context, CHANNEL_ALERTS)
                .setSmallIcon(R.drawable.ic_stat_timer)
                .setContentTitle("¡Jornada completada!")
                .setContentText("Has llegado a las horas de hoy. El contador sigue en marcha.")
                .setContentIntent(Actions.openApp(context))
                .addAction(0, "Parar", Actions.broadcast(context, Actions.STOP, 2))
                .setAutoCancel(true)
                .build(),
        )
    }

    @SuppressLint("MissingPermission")
    fun showForgot(hoursRunning: Long) {
        if (!canNotify()) return
        nm.notify(
            ID_FORGOT,
            NotificationCompat.Builder(context, CHANNEL_ALERTS)
                .setSmallIcon(R.drawable.ic_stat_timer)
                .setContentTitle("¿Sigues trabajando?")
                .setContentText("El contador lleva $hoursRunning h seguidas en marcha.")
                .setContentIntent(Actions.openApp(context, editOpenSession = true))
                .addAction(0, "Sigo trabajando", Actions.broadcast(context, Actions.STILL_WORKING, 3))
                .addAction(0, "Paré hace…", Actions.openApp(context, editOpenSession = true))
                .setAutoCancel(true)
                .build(),
        )
    }

    fun cancelForgot() = nm.cancel(ID_FORGOT)
    fun cancelEndOfDay() = nm.cancel(ID_END_OF_DAY)
}
