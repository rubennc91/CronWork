package com.cronwork.data

import com.cronwork.core.Adjustment
import com.cronwork.core.Session
import com.cronwork.core.SpecialDay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate

class WorkRepository(db: CronWorkDatabase) {
    private val dao = db.workDao()

    val sessions: Flow<List<Session>> = dao.observeSessions().map { l -> l.map { it.toCore() } }
    val adjustments: Flow<List<Adjustment>> = dao.observeAdjustments().map { l -> l.map { it.toCore() } }
    val specials: Flow<List<SpecialDay>> = dao.observeSpecialDays().map { l -> l.map { it.toCore() } }

    suspend fun allSessions(): List<Session> = dao.sessions().map { it.toCore() }
    suspend fun allAdjustments(): List<Adjustment> = dao.adjustments().map { it.toCore() }
    suspend fun allSpecials(): List<SpecialDay> = dao.specialDays().map { it.toCore() }

    suspend fun openSession(): Session? = dao.openSession()?.toCore()
    suspend fun startSession(at: Instant): Boolean = dao.startIfNoneOpen(at.toEpochMilli())
    suspend fun stopOpenSession(at: Instant): Boolean = dao.stopOpen(at.toEpochMilli())
    suspend fun upsertSession(s: Session) = dao.upsertSession(SessionEntity.from(s))
    suspend fun deleteSession(id: Long) = dao.deleteSession(id)

    suspend fun addAdjustment(date: LocalDate, minutes: Int, note: String?, now: Instant) {
        dao.insertAdjustment(AdjustmentEntity(epochDay = date.toEpochDay(), minutes = minutes, note = note, createdAtMillis = now.toEpochMilli()))
    }
    suspend fun deleteAdjustment(id: Long) = dao.deleteAdjustment(id)

    suspend fun upsertSpecialDay(d: SpecialDay) = dao.upsertSpecialDay(SpecialDayEntity.from(d))
    suspend fun deleteSpecialDay(date: LocalDate) = dao.deleteSpecialDay(date.toEpochDay())
}
