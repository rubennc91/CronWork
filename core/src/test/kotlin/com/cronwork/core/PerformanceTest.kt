package com.cronwork.core

import org.junit.Assert.assertEquals
import org.junit.Test

class PerformanceTest {
    // Three years of history, four sessions and one adjustment per working day.
    private val start = MON.minusYears(3)
    private val days = generateSequence(start) { it.plusDays(1) }.takeWhile { !it.isAfter(MON) }.toList()
    private val sessions = days.filter { it.dayOfWeek.value <= 5 }.flatMapIndexed { i, d ->
        listOf(8, 10, 12, 15).mapIndexed { j, h -> Session(i * 4L + j, at(d, h), at(d, h + 1, 50)) }
    }
    private val adjustments = days.filter { it.dayOfWeek.value <= 5 }.mapIndexed { i, d -> Adjustment(i.toLong(), d, -10, null) }
    private val now = at(MON, 18)

    @Test(timeout = 2000)
    fun `sixty recomputations over three years stay fast`() {
        // The UI recomputes the state every second; a minute of that must take well under a second each.
        repeat(60) { TimerState.compute(sessions, adjustments, emptyList(), Settings(), now, MADRID) }
    }

    @Test fun `total over three years matches day by day sum`() {
        val expected = days.sumOf { d ->
            WorkCalculator.daySummary(d, sessions, adjustments, null, Settings(), now, MADRID).balanceSeconds
        }
        assertEquals(expected, WorkCalculator.totalBalanceSeconds(sessions, adjustments, emptyList(), Settings(), now, MADRID))
    }
}
