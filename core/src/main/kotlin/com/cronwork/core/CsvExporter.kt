package com.cronwork.core

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

/**
 * One row per day, from the first record to today, plus the hour bank total.
 * Formatted for Spanish Excel: ";" separator, decimal comma, UTF-8 with BOM, CRLF line ends.
 */
object CsvExporter {
    private val spanish = Locale.forLanguageTag("es-ES")
    private val time = DateTimeFormatter.ofPattern("HH:mm")
    private const val HEADER = "Fecha;Día;Trabajado;Trabajado (horas);Objetivo;Saldo;Tramos;Ajustes;Día especial"

    fun export(
        sessions: List<Session>,
        adjustments: List<Adjustment>,
        specials: List<SpecialDay>,
        settings: Settings,
        now: Instant,
        zone: ZoneId,
    ): String {
        val rows = mutableListOf(HEADER)
        val first = WorkCalculator.firstDay(sessions, adjustments, zone)
        if (first != null) {
            val today = now.atZone(zone).toLocalDate()
            val sessionsByDay = sessions.groupBy { it.start.atZone(zone).toLocalDate() }
            val adjustmentsByDay = adjustments.groupBy { it.date }
            val specialsByDay = specials.associateBy { it.date }
            WorkCalculator.daySummaries(first, today, sessions, adjustments, specials, settings, now, zone).forEach { d ->
                val tramos = sessionsByDay[d.date].orEmpty().sortedBy { it.start }.joinToString(", ") { s ->
                    val start = s.start.atZone(zone)
                    val end = s.end?.atZone(zone)
                    when {
                        end == null -> "${start.format(time)}-en marcha"
                        end.toLocalDate() != d.date -> "${start.format(time)}-${end.format(time)} (+1)"
                        else -> "${start.format(time)}-${end.format(time)}"
                    }
                }
                val ajustes = adjustmentsByDay[d.date].orEmpty().joinToString(", ") { a ->
                    signed(a.minutes * 60L) + (a.note?.let { " $it" } ?: "")
                }
                rows += listOf(
                    d.date.toString(),
                    d.date.dayOfWeek.getDisplayName(TextStyle.FULL, spanish),
                    worked(d.workedSeconds),
                    hours(d.workedSeconds),
                    formatHm(d.targetSeconds),
                    signed(d.balanceSeconds),
                    tramos,
                    ajustes,
                    specialsByDay[d.date]?.displayName() ?: "",
                ).joinToString(";") { quote(it) }
            }
            val total = WorkCalculator.totalBalanceSeconds(sessions, adjustments, specials, settings, now, zone)
            rows += listOf("Bolsa total", "", "", "", "", signed(total), "", "", "").joinToString(";")
        }
        return "﻿" + rows.joinToString("\r\n") + "\r\n"
    }

    private fun worked(seconds: Long) = (if (seconds / 60 < 0) "-" else "") + formatHm(seconds)

    private fun signed(seconds: Long) = when {
        seconds / 60 == 0L -> "0:00"
        seconds > 0 -> "+" + formatHm(seconds)
        else -> "-" + formatHm(seconds)
    }

    /** Decimal hours with a comma, e.g. 27600 s → "7,67". */
    private fun hours(seconds: Long): String {
        val minutes = BigDecimal.valueOf(abs(seconds) / 60)
        val value = minutes.divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP).toPlainString().replace('.', ',')
        return if (seconds / 60 < 0) "-$value" else value
    }

    private fun quote(field: String): String =
        if (field.any { it == ';' || it == '"' || it == '\n' || it == '\r' }) "\"" + field.replace("\"", "\"\"") + "\"" else field
}
