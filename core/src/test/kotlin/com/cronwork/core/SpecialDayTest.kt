package com.cronwork.core

import org.junit.Assert.assertEquals
import org.junit.Test

class SpecialDayTest {
    @Test fun `label without reason is the type`() {
        assertEquals("Festivo", SpecialDay(MON, SpecialDayType.HOLIDAY, 0).displayName())
        assertEquals("Vacaciones", SpecialDay(MON, SpecialDayType.VACATION, 0).displayName())
        assertEquals("Otro", SpecialDay(MON, SpecialDayType.OTHER, 0).displayName())
    }

    @Test fun `reason is added to the type`() {
        assertEquals("Festivo · San Isidro", SpecialDay(MON, SpecialDayType.HOLIDAY, 0, "San Isidro").displayName())
    }

    @Test fun `other with reason shows only the reason`() {
        assertEquals("Médico", SpecialDay(MON, SpecialDayType.OTHER, 240, "Médico").displayName())
    }

    @Test fun `blank reason is ignored`() {
        assertEquals("Festivo", SpecialDay(MON, SpecialDayType.HOLIDAY, 0, "  ").displayName())
    }

    @Test fun `csv shows the reason`() {
        val csv = CsvExporter.export(
            listOf(Session(1, at(MON, 8), at(MON, 9))), emptyList(),
            listOf(SpecialDay(MON, SpecialDayType.OTHER, 240, "Médico")), Settings(), at(MON, 20), MADRID,
        )
        assertEquals(true, csv.contains(";Médico\r\n"))
    }
}
