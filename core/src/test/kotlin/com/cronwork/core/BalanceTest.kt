package com.cronwork.core

import org.junit.Assert.assertEquals
import org.junit.Test

class BalanceTest {
    private val settings = Settings()

    private fun fullDay(day: java.time.LocalDate) = session(day, 8, 0, 15, 30)

    @Test fun `week start is monday`() {
        assertEquals(MON, WorkCalculator.weekStart(MON.plusDays(6)))
        assertEquals(MON, WorkCalculator.weekStart(MON))
    }

    @Test fun `full week of 7h30 gives zero balance`() {
        val s = (0L..4L).map { fullDay(MON.plusDays(it)) }
        val w = WorkCalculator.weekSummary(MON, s, emptyList(), emptyList(), settings, at(MON.plusDays(7), 9), MADRID)
        assertEquals(0, w.balanceSeconds)
        assertEquals(135000, w.targetSeconds)
        assertEquals(135000, w.workedSeconds)
        assertEquals(7, w.days.size)
    }

    @Test fun `future days do not subtract`() {
        val s = (0L..2L).map { fullDay(MON.plusDays(it)) }
        val w = WorkCalculator.weekSummary(MON, s, emptyList(), emptyList(), settings, at(MON.plusDays(2), 18), MADRID)
        assertEquals(0, w.balanceSeconds)
    }

    @Test fun `weekend work adds to balance`() {
        val sat = MON.plusDays(5)
        val s = (0L..4L).map { fullDay(MON.plusDays(it)) } + session(sat, 10, 0, 12)
        val w = WorkCalculator.weekSummary(MON, s, emptyList(), emptyList(), settings, at(MON.plusDays(7), 9), MADRID)
        assertEquals(7200, w.balanceSeconds)
    }

    @Test fun `holiday reduces week target`() {
        val holiday = SpecialDay(MON.plusDays(3), SpecialDayType.HOLIDAY, 0)
        val w = WorkCalculator.weekSummary(MON, emptyList(), emptyList(), listOf(holiday), settings, at(MON, 9), MADRID)
        assertEquals(108000, w.targetSeconds)
    }

    @Test fun `total balance spans weeks`() {
        val prevMon = MON.minusDays(7)
        // week 1: Mon-Fri full + 1h extra on Friday; week 2 (until Mon): Mon 7h00
        val s = (0L..4L).map { fullDay(prevMon.plusDays(it)) } +
            session(prevMon.plusDays(4), 16, 0, 17) +
            session(MON, 8, 0, 15)
        val total = WorkCalculator.totalBalanceSeconds(s, emptyList(), emptyList(), settings, at(MON, 20), MADRID)
        assertEquals(1800, total)
    }

    @Test fun `no data gives zero total`() {
        assertEquals(0, WorkCalculator.totalBalanceSeconds(emptyList(), emptyList(), emptyList(), settings, at(MON, 9), MADRID))
    }
}
