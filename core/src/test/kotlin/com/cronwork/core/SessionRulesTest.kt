package com.cronwork.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionRulesTest {
    private val now = at(MON, 20)

    @Test fun `end before start rejected`() {
        val s = Session(1, at(MON, 11), at(MON, 10))
        assertEquals(EditError.EndBeforeStart, SessionRules.validate(s, emptyList(), now))
    }

    @Test fun `overlap rejected`() {
        val other = Session(2, at(MON, 9), at(MON, 11))
        val s = Session(1, at(MON, 10), at(MON, 12))
        assertEquals(EditError.Overlaps, SessionRules.validate(s, listOf(other), now))
    }

    @Test fun `overlap with open session rejected`() {
        val open = Session(2, at(MON, 15), null)
        val s = Session(1, at(MON, 16), at(MON, 17))
        assertEquals(EditError.Overlaps, SessionRules.validate(s, listOf(open), now))
    }

    @Test fun `touching sessions allowed`() {
        val other = Session(2, at(MON, 9), at(MON, 10))
        val s = Session(1, at(MON, 10), at(MON, 11))
        assertNull(SessionRules.validate(s, listOf(other), now))
    }

    @Test fun `editing a session ignores its own previous version`() {
        val old = Session(1, at(MON, 9), at(MON, 10))
        val s = old.copy(end = at(MON, 10, 30))
        assertNull(SessionRules.validate(s, listOf(old), now))
    }

    @Test fun `future end rejected`() {
        val s = Session(1, at(MON, 19), at(MON, 21))
        assertEquals(EditError.InFuture, SessionRules.validate(s, emptyList(), now))
    }

    @Test fun `cannot start with open session`() {
        assertFalse(SessionRules.canStart(listOf(Session(1, at(MON, 9), null))))
        assertTrue(SessionRules.canStart(listOf(Session(1, at(MON, 9), at(MON, 10)))))
    }

    @Test fun `keeping a session open is rejected if it was stopped meanwhile`() {
        // Dialog opened while running; meanwhile stopped from the notification at 12:00.
        val stored = Session(1, at(MON, 9), at(MON, 12))
        val stillOpen = Session(1, at(MON, 9), null)
        assertEquals(EditError.AlreadyStopped, SessionRules.validate(stillOpen, listOf(stored), now))
    }

    @Test fun `editing the start of a running session is allowed`() {
        val stored = Session(1, at(MON, 9), null)
        assertNull(SessionRules.validate(Session(1, at(MON, 8, 30), null), listOf(stored), now))
    }
}
