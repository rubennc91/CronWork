package com.cronwork.core

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

val MADRID: ZoneId = ZoneId.of("Europe/Madrid")

fun at(date: LocalDate, hour: Int, minute: Int = 0): Instant =
    LocalDateTime.of(date, java.time.LocalTime.of(hour, minute)).atZone(MADRID).toInstant()

private var nextId = 1L

fun session(date: LocalDate, fromH: Int, fromM: Int, toH: Int?, toM: Int = 0): Session =
    Session(nextId++, at(date, fromH, fromM), toH?.let { at(date, it, toM) })

fun adj(date: LocalDate, minutes: Int) = Adjustment(nextId++, date, minutes, null)

// 2026-11-02 is a Monday.
val MON: LocalDate = LocalDate.of(2026, 11, 2)
