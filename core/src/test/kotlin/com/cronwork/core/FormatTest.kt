package com.cronwork.core

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {
    @Test fun `remaining time counts down`() {
        assertEquals("7:30:00", formatRemaining(27000))
        assertEquals("0:00:00", formatRemaining(0))
        assertEquals("0:00:59", formatRemaining(59))
    }

    @Test fun `negative remaining shows extra with plus`() {
        assertEquals("+0:05:00", formatRemaining(-300))
    }

    @Test fun `signed balance`() {
        assertEquals("+1:15", formatSigned(4500))
        assertEquals("−0:30", formatSigned(-1800))
        assertEquals("0:00", formatSigned(0))
        assertEquals("+12:05", formatSigned(12 * 3600 + 300))
    }

    @Test fun `duration without sign`() {
        assertEquals("7:30", formatHm(27000))
        assertEquals("0:00", formatHm(59))
    }
}

class ParseTest {
    @Test fun `parses hours and minutes`() {
        assertEquals(450, parseHm("7:30"))
        assertEquals(450, parseHm(" 07:30 "))
        assertEquals(0, parseHm("0:00"))
        assertEquals(600, parseHm("10"))
    }

    @Test fun `rejects invalid input`() {
        assertEquals(null, parseHm("7:75"))
        assertEquals(null, parseHm("abc"))
        assertEquals(null, parseHm(""))
        assertEquals(null, parseHm("-1:00"))
    }
}
