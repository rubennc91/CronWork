package com.cronwork.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerStateTest {
    @Test fun `running state reflects open session`() {
        val s = listOf(Session(1, at(MON, 8), at(MON, 10)), Session(2, at(MON, 11), null))
        val st = TimerState.compute(s, emptyList(), emptyList(), Settings(), at(MON, 12), MADRID)
        assertTrue(st.running)
        assertEquals(at(MON, 11), st.runningSince)
        assertEquals(3 * 3600L, st.today.workedSeconds)
        assertEquals(3 * 3600L - 27000, st.weekBalanceSeconds)
        assertEquals(3 * 3600L - 27000, st.totalBalanceSeconds)
    }

    @Test fun `open session from yesterday keeps running into today`() {
        val s = listOf(Session(1, at(MON, 22), null))
        val st = TimerState.compute(s, emptyList(), emptyList(), Settings(), at(MON.plusDays(1), 1), MADRID)
        assertTrue(st.running)
        assertEquals(MON.plusDays(1), st.today.date)
        assertEquals(0, st.today.workedSeconds)
        assertTrue(st.openSessionFromEarlierDay)
    }

    @Test fun `stopped state`() {
        val st = TimerState.compute(emptyList(), emptyList(), emptyList(), Settings(), at(MON, 12), MADRID)
        assertFalse(st.running)
        assertNull(st.runningSince)
        assertEquals(27000, st.today.remainingSeconds)
    }
}
