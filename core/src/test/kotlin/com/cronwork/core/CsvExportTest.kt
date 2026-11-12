package com.cronwork.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvExportTest {
    private val tue = MON.plusDays(1)

    private fun export(
        sessions: List<Session>,
        adjustments: List<Adjustment> = emptyList(),
        specials: List<SpecialDay> = emptyList(),
        now: java.time.Instant = at(tue, 20),
    ) = CsvExporter.export(sessions, adjustments, specials, Settings(), now, MADRID)

    private fun lines(csv: String) = csv.removePrefix("﻿").trimEnd().split("\r\n")

    @Test fun `starts with BOM and Spanish header`() {
        val csv = export(listOf(Session(1, at(MON, 8), at(MON, 15, 30))))
        assertTrue(csv.startsWith("﻿"))
        assertEquals("Fecha;Día;Trabajado;Trabajado (horas);Objetivo;Saldo;Tramos;Ajustes;Día especial", lines(csv)[0])
    }

    @Test fun `one row per day from first record to today`() {
        val csv = export(listOf(Session(1, at(MON, 8), at(MON, 15, 30))))
        val l = lines(csv)
        assertEquals("2026-11-02;lunes;7:30;7,50;7:30;0:00;08:00-15:30;;", l[1])
        assertEquals("2026-11-03;martes;0:00;0,00;7:30;-7:30;;;", l[2])
        assertEquals("Bolsa total;;;;;-7:30;;;", l[3])
        assertEquals(4, l.size)
    }

    @Test fun `adjustments and special days are listed`() {
        val csv = export(
            listOf(Session(1, at(MON, 8), at(MON, 15, 30))),
            adjustments = listOf(Adjustment(1, MON, 15, "reunión"), Adjustment(2, MON, -5, null)),
            specials = listOf(SpecialDay(tue, SpecialDayType.HOLIDAY, 0)),
        )
        val l = lines(csv)
        assertEquals("2026-11-02;lunes;7:40;7,67;7:30;+0:10;08:00-15:30;+0:15 reunión, -0:05;", l[1])
        assertEquals("2026-11-03;martes;0:00;0,00;0:00;0:00;;;Festivo", l[2])
    }

    @Test fun `session past midnight is marked`() {
        val csv = export(listOf(Session(1, at(MON, 22), at(tue, 1))))
        assertEquals("2026-11-02;lunes;3:00;3,00;7:30;-4:30;22:00-01:00 (+1);;", lines(csv)[1])
    }

    @Test fun `running session is marked`() {
        val csv = export(listOf(Session(1, at(tue, 18), null)))
        assertEquals("2026-11-03;martes;2:00;2,00;7:30;-5:30;18:00-en marcha;;", lines(csv)[1])
    }

    @Test fun `fields with separators are quoted`() {
        val csv = export(
            listOf(Session(1, at(MON, 8), at(MON, 15, 30))),
            adjustments = listOf(Adjustment(1, MON, 5, "a;b \"c\"")),
        )
        assertEquals("2026-11-02;lunes;7:35;7,58;7:30;+0:05;08:00-15:30;\"+0:05 a;b \"\"c\"\"\";", lines(csv)[1])
    }

    @Test fun `no data gives only the header`() {
        assertEquals(1, lines(export(emptyList())).size)
    }
}
