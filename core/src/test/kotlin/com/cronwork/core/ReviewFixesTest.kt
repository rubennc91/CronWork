package com.cronwork.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class ReviewFixesTest {
    @Test fun `wall clock time on dst fall back day`() {
        val day = LocalDate.of(2026, 10, 25)
        val i = WorkCalculator.instantAt(day, 10 * 60, MADRID)
        assertEquals(LocalDateTime.of(2026, 10, 25, 10, 0), LocalDateTime.ofInstant(i, MADRID))
    }

    @Test fun `wall clock time on dst spring forward day`() {
        val day = LocalDate.of(2026, 3, 29)
        val i = WorkCalculator.instantAt(day, 10 * 60, MADRID)
        assertEquals(LocalDateTime.of(2026, 3, 29, 10, 0), LocalDateTime.ofInstant(i, MADRID))
    }

    private val pause = Settings(pauseEnabled = true) // target 7:30, pause 30 min after 6:00

    private fun st(workedMinutes: Long, s: Settings): TimerState {
        val sessions = listOf(Session(1, at(MON, 8), null))
        return TimerState.compute(sessions, emptyList(), emptyList(), s, at(MON, 8).plusSeconds(workedMinutes * 60), MADRID)
    }

    @Test fun `seconds to target without pause equals remaining`() {
        assertEquals(30 * 60L, st(420, Settings()).secondsToTarget)
    }

    @Test fun `seconds to target accounts for upcoming pause`() {
        // 5:00 worked: threshold at 6:00 adds 30 min, so 7:30 is reached at 7:00 of real work.
        assertEquals(120 * 60L, st(300, pause).secondsToTarget)
    }

    @Test fun `seconds to target after pause applied`() {
        // 6:30 worked + 30 pause = 7:00 → 30 min left.
        assertEquals(30 * 60L, st(390, pause).secondsToTarget)
    }

    @Test fun `pause threshold above target is ignored`() {
        val s = pause.copy(pauseThresholdMinutes = 480)
        assertEquals(150 * 60L, st(300, s).secondsToTarget)
    }

    @Test fun `seconds to target negative when exceeded`() {
        assertEquals(-10 * 60L, st(460, Settings()).secondsToTarget)
    }
}

class SessionTimesTest {
    @Test fun `end after start stays on the same day`() {
        val (start, end) = WorkCalculator.sessionInstants(MON, 9 * 60, 17 * 60, MADRID)
        assertEquals(at(MON, 9), start)
        assertEquals(at(MON, 17), end)
    }

    @Test fun `end before start finishes the next day`() {
        val (start, end) = WorkCalculator.sessionInstants(MON, 22 * 60, 60, MADRID)
        assertEquals(at(MON, 22), start)
        assertEquals(at(MON.plusDays(1), 1), end)
    }

    @Test fun `open session has no end`() {
        val (_, end) = WorkCalculator.sessionInstants(MON, 22 * 60, null, MADRID)
        assertEquals(null, end)
    }

    @Test fun `next day end is detected`() {
        assertEquals(true, WorkCalculator.endsNextDay(22 * 60, 60))
        assertEquals(false, WorkCalculator.endsNextDay(9 * 60, 17 * 60))
        assertEquals(false, WorkCalculator.endsNextDay(9 * 60, null))
    }
}
