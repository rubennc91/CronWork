package com.cronwork.core

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

/** A stretch of work. [end] is null while the timer is running. */
data class Session(val id: Long, val start: Instant, val end: Instant?)

/** Manual correction of a day's worked time, in minutes (may be negative). */
data class Adjustment(val id: Long, val date: LocalDate, val minutes: Int, val note: String?)

enum class SpecialDayType { HOLIDAY, VACATION, OTHER }

fun SpecialDayType.label(): String = when (this) {
    SpecialDayType.HOLIDAY -> "Festivo"
    SpecialDayType.VACATION -> "Vacaciones"
    SpecialDayType.OTHER -> "Otro"
}

/** A day whose target differs from the normal one (holiday, vacation...), with an optional free-text reason. */
data class SpecialDay(val date: LocalDate, val type: SpecialDayType, val targetMinutes: Int, val note: String? = null) {
    /** "Festivo", "Festivo · San Isidro", or just the reason for "Otro" ("Médico"). */
    fun displayName(): String {
        val reason = note?.trim()?.takeIf { it.isNotEmpty() } ?: return type.label()
        return if (type == SpecialDayType.OTHER) reason else "${type.label()} · $reason"
    }
}

data class Settings(
    val dailyTargetMinutes: Int = 450,
    val workDays: Set<DayOfWeek> = setOf(
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
    ),
    val pauseEnabled: Boolean = false,
    val pauseMinutes: Int = 30,
    val pauseThresholdMinutes: Int = 360,
    val forgotReminderHours: Int = 4,
)

data class DaySummary(
    val date: LocalDate,
    val workedSeconds: Long,
    val targetSeconds: Long,
    val pauseApplied: Boolean,
    /** Negative adjustments exceed the time worked: the day is below zero. */
    val negativeWorked: Boolean,
    val crossesMidnight: Boolean,
) {
    val balanceSeconds: Long get() = workedSeconds - targetSeconds
    val remainingSeconds: Long get() = targetSeconds - workedSeconds
}

data class WeekSummary(
    val weekStart: LocalDate,
    /** Seven days, Monday to Sunday. */
    val days: List<DaySummary>,
    val workedSeconds: Long,
    val targetSeconds: Long,
    val balanceSeconds: Long,
)
