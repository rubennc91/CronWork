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
}
