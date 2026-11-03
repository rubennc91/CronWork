package com.cronwork.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZonedDateTime

class DaySummaryTest {
    private val settings = Settings()

    private fun summary(
        date: LocalDate,
        sessions: List<Session> = emptyList(),
        adjustments: List<Adjustment> = emptyList(),
        special: SpecialDay? = null,
        s: Settings = settings,
        now: java.time.Instant = at(date.plusDays(1), 12),
    ) = WorkCalculator.daySummary(date, sessions, adjustments, special, s, now, MADRID)

    @Test fun `weekday target is 450 min`() {
        assertEquals(27000, summary(MON).targetSeconds)
        assertEquals(0, summary(MON.plusDays(5)).targetSeconds)
    }

    @Test fun `special day overrides target`() {
        val tue = MON.plusDays(1)
        val holiday = SpecialDay(tue, SpecialDayType.HOLIDAY, 0)
        assertEquals(0, summary(tue, special = holiday).targetSeconds)
    }

    @Test fun `closed and open sessions sum`() {
        val s = listOf(session(MON, 9, 0, 11), session(MON, 12, 0, null))
        assertEquals(9000, summary(MON, s, now = at(MON, 12, 30)).workedSeconds)
    }

    @Test fun `adjustments add and subtract`() {
        val r = summary(MON, listOf(session(MON, 9, 0, 10)), listOf(adj(MON, 15), adj(MON, -5)))
        assertEquals(4200, r.workedSeconds)
        assertFalse(r.negativeWorked)
    }

    @Test fun `negative adjustments can leave the day below zero`() {
        val r = summary(MON, listOf(session(MON, 9, 0, 10)), listOf(adj(MON, -90)))
        assertEquals(-1800, r.workedSeconds)
        assertEquals(27000 + 1800L, r.remainingSeconds)
        assertTrue(r.negativeWorked)
    }

    @Test fun `countdown keeps moving while adjustments exceed worked time`() {
        // App, widget and notification must agree: every running second lowers the remaining time.
        val open = Session(50, at(MON, 9), null)
        val a = summary(MON, listOf(open), listOf(adj(MON, -90)), now = at(MON, 9, 10))
        val b = summary(MON, listOf(open), listOf(adj(MON, -90)), now = at(MON, 9, 40))
        assertEquals(30 * 60L, a.remainingSeconds - b.remainingSeconds)
    }

    @Test fun `pause applies at threshold`() {
        val on = settings.copy(pauseEnabled = true)
        val six = summary(MON, listOf(session(MON, 8, 0, 14)), s = on)
        assertEquals(6 * 3600L + 1800, six.workedSeconds)
        assertTrue(six.pauseApplied)
        val almost = summary(MON, listOf(session(MON, 8, 0, 13, 59)), s = on)
        assertEquals(5 * 3600L + 59 * 60, almost.workedSeconds)
        assertFalse(almost.pauseApplied)
        val off = summary(MON, listOf(session(MON, 8, 0, 14)))
        assertEquals(6 * 3600L, off.workedSeconds)
        assertFalse(off.pauseApplied)
    }

    @Test fun `session crossing midnight belongs to start day`() {
        val s = Session(99, at(MON, 22), at(MON.plusDays(1), 2))
        val day1 = summary(MON, listOf(s))
        assertEquals(4 * 3600L, day1.workedSeconds)
        assertTrue(day1.crossesMidnight)
        val day2 = summary(MON.plusDays(1), listOf(s))
        assertEquals(0, day2.workedSeconds)
        assertFalse(day2.crossesMidnight)
    }

    @Test fun `open session past midnight is flagged`() {
        val s = Session(98, at(MON, 22), null)
        val r = summary(MON, listOf(s), now = at(MON.plusDays(1), 1))
        assertEquals(3 * 3600L, r.workedSeconds)
        assertTrue(r.crossesMidnight)
    }

    @Test fun `dst fall back day counts real duration`() {
        val day = LocalDate.of(2026, 10, 25)
        val start = ZonedDateTime.of(day, java.time.LocalTime.of(1, 0), MADRID).toInstant()
        val end = ZonedDateTime.of(day, java.time.LocalTime.of(4, 0), MADRID).toInstant()
        assertEquals(4 * 3600L, summary(day, listOf(Session(97, start, end))).workedSeconds)
    }
}
