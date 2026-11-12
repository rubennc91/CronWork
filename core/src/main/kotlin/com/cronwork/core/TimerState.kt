package com.cronwork.core

import java.time.Instant
import java.time.ZoneId

/** Everything the UI, notification and widget need to show about the timer right now. */
data class TimerState(
    val running: Boolean,
    val runningSince: Instant?,
    val today: DaySummary,
    val weekBalanceSeconds: Long,
    val totalBalanceSeconds: Long,
    /** The open session started on an earlier day (forgot to stop?). Its time counts for that day. */
    val openSessionFromEarlierDay: Boolean,
    /** Real working seconds until today's target is reached, paid pause included; negative once exceeded. */
    val secondsToTarget: Long,
) {
    companion object {
        fun compute(
            sessions: List<Session>,
            adjustments: List<Adjustment>,
            specials: List<SpecialDay>,
            settings: Settings,
            now: Instant,
            zone: ZoneId,
        ): TimerState {
            val today = now.atZone(zone).toLocalDate()
            val open = sessions.firstOrNull { it.end == null }
            val week = WorkCalculator.weekSummary(
                WorkCalculator.weekStart(today), sessions, adjustments, specials, settings, now, zone,
            )
            val todaySummary = week.days.first { it.date == today }
            return TimerState(
                running = open != null,
                runningSince = open?.start,
                today = todaySummary,
                weekBalanceSeconds = week.balanceSeconds,
                totalBalanceSeconds = WorkCalculator.totalBalanceSeconds(sessions, adjustments, specials, settings, now, zone),
                openSessionFromEarlierDay = open != null && open.start.atZone(zone).toLocalDate() != today,
                secondsToTarget = WorkCalculator.secondsToTarget(todaySummary, settings),
            )
        }
    }
}
