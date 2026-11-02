package com.cronwork.core

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object WorkCalculator {

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
        val sessionSeconds = daySessions.sumOf { s ->
            Duration.between(s.start, s.end ?: now).seconds.coerceAtLeast(0)
        }
        val crossesMidnight = daySessions.any { s ->
            (s.end ?: now).atZone(zone).toLocalDate() != date
        }
        val adjustmentSeconds = adjustments.filter { it.date == date }.sumOf { it.minutes * 60L }
        val raw = sessionSeconds + adjustmentSeconds
        val clamped = raw < 0
        var worked = raw.coerceAtLeast(0)
        val pauseApplied = settings.pauseEnabled && worked >= settings.pauseThresholdMinutes * 60L
        if (pauseApplied) worked += settings.pauseMinutes * 60L
        return DaySummary(
            date = date,
            workedSeconds = worked,
            targetSeconds = targetMinutes(date, special, settings) * 60L,
            pauseApplied = pauseApplied,
            adjustmentClamped = clamped,
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
        val days = (0L until 7L).map { offset ->
            val date = weekStart.plusDays(offset)
            daySummary(date, sessions, adjustments, specialsByDate[date], settings, now, zone)
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
        val first = (sessions.map { it.start.atZone(zone).toLocalDate() } + adjustments.map { it.date })
            .minOrNull() ?: return 0
        val specialsByDate = specials.associateBy { it.date }
        var total = 0L
        var date = first
        while (!date.isAfter(today)) {
            total += daySummary(date, sessions, adjustments, specialsByDate[date], settings, now, zone).balanceSeconds
            date = date.plusDays(1)
        }
        return total
    }
}
