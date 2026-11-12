package com.cronwork.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class WorkDao {
    @Query("SELECT * FROM sessions ORDER BY startMillis")
    abstract fun observeSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions ORDER BY startMillis")
    abstract suspend fun sessions(): List<SessionEntity>

    @Query("SELECT * FROM sessions WHERE endMillis IS NULL LIMIT 1")
    abstract suspend fun openSession(): SessionEntity?

    @Insert
    abstract suspend fun insertSession(s: SessionEntity): Long

    @Upsert
    abstract suspend fun upsertSession(s: SessionEntity)

    @Query("UPDATE sessions SET endMillis = :endMillis WHERE endMillis IS NULL")
    abstract suspend fun closeOpenSessions(endMillis: Long): Int

    @Query("DELETE FROM sessions WHERE id = :id")
    abstract suspend fun deleteSession(id: Long)

    /** Starts a session unless one is already open. Atomic, so double taps cannot create two. */
    @Transaction
    open suspend fun startIfNoneOpen(startMillis: Long): Boolean {
        if (openSession() != null) return false
        insertSession(SessionEntity(startMillis = startMillis, endMillis = null))
        return true
    }

    /** Closes the open session at [endMillis] (never before its start). Returns false if none was open. */
    @Transaction
    open suspend fun stopOpen(endMillis: Long): Boolean {
        val open = openSession() ?: return false
        closeOpenSessions(maxOf(endMillis, open.startMillis))
        return true
    }

    @Query("SELECT * FROM adjustments ORDER BY epochDay, createdAtMillis")
    abstract fun observeAdjustments(): Flow<List<AdjustmentEntity>>

    @Query("SELECT * FROM adjustments ORDER BY epochDay, createdAtMillis")
    abstract suspend fun adjustments(): List<AdjustmentEntity>

    @Insert
    abstract suspend fun insertAdjustment(a: AdjustmentEntity): Long

    @Query("DELETE FROM adjustments WHERE id = :id")
    abstract suspend fun deleteAdjustment(id: Long)

    @Query("SELECT * FROM special_days ORDER BY epochDay")
    abstract fun observeSpecialDays(): Flow<List<SpecialDayEntity>>

    @Query("SELECT * FROM special_days ORDER BY epochDay")
    abstract suspend fun specialDays(): List<SpecialDayEntity>

    @Upsert
    abstract suspend fun upsertSpecialDay(d: SpecialDayEntity)

    @Query("DELETE FROM special_days WHERE epochDay = :epochDay")
    abstract suspend fun deleteSpecialDay(epochDay: Long)
}
