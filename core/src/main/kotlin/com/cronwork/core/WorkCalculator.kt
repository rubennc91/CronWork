package com.cronwork.core

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object WorkCalculator {

    /** Wall-clock time ([minutes] after midnight) on [date], correct on DST change days. */
    fun instantAt(date: LocalDate, minutes: Int, zone: ZoneId): Instant =
        date.atTime(minutes / 60 % 24, minutes % 60).atZone(zone).toInstant()
            .let { if (minutes >= 24 * 60) date.plusDays(1).atStartOfDay(zone).toInstant() else it }

    /**
     * Real working seconds until the day's target is reached (negative once exceeded).
     * Unlike [DaySummary.remainingSeconds] it accounts for a paid pause that will be added
     * when the threshold is crossed.
     */
    fun secondsToTarget(summary: DaySummary, settings: Settings): Long {
        if (summary.remainingSeconds <= 0 || summary.pauseApplied || !settings.pauseEnabled) return summary.remainingSeconds
        val raw = summary.workedSeconds
        val goal = summary.targetSeconds
        val threshold = settings.pauseThresholdMinutes * 60L
        val neededRaw = if (goal <= threshold) goal else maxOf(threshold, goal - settings.pauseMinutes * 60L)
        return neededRaw - raw
    }

    fun targetMinutes(date: LocalDate, special: SpecialDay?, settings: Settings): Int = when {
        special != null -> special.targetMinutes
        date.dayOfWeek in settings.workDays -> settings.dailyTargetMinutes
        else -> 0
    }

    /** Sessions belong entirely to the day they started on; an open session counts up to [now]. */
    fun daySummary(
        date: LocalDate,
        sessions: List<Session>,
        adjustments: List<Adjustment>,
        special: SpecialDay?,
        settings: Settings,
        now: Instant,
        zone: ZoneId,
    ): DaySummary {
        val daySessions = sessions.filter { it.start.atZone(zone).toLocalDate() == date }
        val adjustmentSeconds = adjustments.filter { it.date == date }.sumOf { it.minutes * 60L }
        return summarize(date, daySessions, adjustmentSeconds, special, settings, now, zone)
    }

    /** Sessions and adjustment totals grouped by day once, so long histories stay fast. */
    private class DayIndex(sessions: List<Session>, adjustments: List<Adjustment>, zone: ZoneId) {
        val sessionsByDay: Map<LocalDate, List<Session>> = sessions.groupBy { it.start.atZone(zone).toLocalDate() }
        val adjustmentSecondsByDay: Map<LocalDate, Long> =
            adjustments.groupBy { it.date }.mapValues { (_, l) -> l.sumOf { it.minutes * 60L } }

        fun firstDay(): LocalDate? = (sessionsByDay.keys + adjustmentSecondsByDay.keys).minOrNull()
    }

    private fun summarize(
        date: LocalDate,
        index: DayIndex,
        special: SpecialDay?,
        settings: Settings,
        now: Instant,
        zone: ZoneId,
    ) = summarize(
        date, index.sessionsByDay[date].orEmpty(), index.adjustmentSecondsByDay[date] ?: 0L,
        special, settings, now, zone,
    )

    private fun summarize(
        date: LocalDate,
        daySessions: List<Session>,
        adjustmentSeconds: Long,
        special: SpecialDay?,
        settings: Settings,
        now: Instant,
        zone: ZoneId,
    ): DaySummary {
        val sessionSeconds = daySessions.sumOf { s ->
            Duration.between(s.start, s.end ?: now).seconds.coerceAtLeast(0)
        }
        val crossesMidnight = daySessions.any { s ->
            (s.end ?: now).atZone(zone).toLocalDate() != date
        }
        // Not clamped at 0: a linear total keeps the app, notification and widget countdowns in step.
        var worked = sessionSeconds + adjustmentSeconds
        val pauseApplied = settings.pauseEnabled && worked >= settings.pauseThresholdMinutes * 60L
        if (pauseApplied) worked += settings.pauseMinutes * 60L
        return DaySummary(
            date = date,
            workedSeconds = worked,
            targetSeconds = targetMinutes(date, special, settings) * 60L,
            pauseApplied = pauseApplied,
            negativeWorked = worked < 0,
            crossesMidnight = crossesMidnight,
        )
    }

    fun weekStart(date: LocalDate): LocalDate = date.minusDays((date.dayOfWeek.value - 1).toLong())

    /** Week from Monday to Sunday. Days after today do not count towards the balance. */
    fun weekSummary(
        weekStart: LocalDate,
        sessions: List<Session>,
        adjustments: List<Adjustment>,
        specials: List<SpecialDay>,
        settings: Settings,
        now: Instant,
        zone: ZoneId,
    ): WeekSummary {
        val today = now.atZone(zone).toLocalDate()
        val specialsByDate = specials.associateBy { it.date }
        val index = DayIndex(sessions, adjustments, zone)
        val days = (0L until 7L).map { offset ->
            val date = weekStart.plusDays(offset)
            summarize(date, index, specialsByDate[date], settings, now, zone)
        }
        return WeekSummary(
            weekStart = weekStart,
            days = days,
            workedSeconds = days.sumOf { it.workedSeconds },
            targetSeconds = days.sumOf { it.targetSeconds },
            balanceSeconds = days.filter { !it.date.isAfter(today) }.sumOf { it.balanceSeconds },
        )
    }

    /** Hour bank: sum of daily balances from the first day with data up to today. */
    fun totalBalanceSeconds(
        sessions: List<Session>,
        adjustments: List<Adjustment>,
        specials: List<SpecialDay>,
        settings: Settings,
        now: Instant,
        zone: ZoneId,
    ): Long {
        val today = now.atZone(zone).toLocalDate()
        val index = DayIndex(sessions, adjustments, zone)
        val first = index.firstDay() ?: return 0
        val specialsByDate = specials.associateBy { it.date }
        var total = 0L
        var date = first
        while (!date.isAfter(today)) {
            total += summarize(date, index, specialsByDate[date], settings, now, zone).balanceSeconds
            date = date.plusDays(1)
        }
        return total
    }
}
