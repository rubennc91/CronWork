package com.cronwork.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cronwork.core.Adjustment
import com.cronwork.core.Session
import com.cronwork.core.SpecialDay
import com.cronwork.core.SpecialDayType
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startMillis: Long,
    val endMillis: Long?,
) {
    fun toCore() = Session(id, Instant.ofEpochMilli(startMillis), endMillis?.let(Instant::ofEpochMilli))

    companion object {
        fun from(s: Session) = SessionEntity(s.id, s.start.toEpochMilli(), s.end?.toEpochMilli())
    }
}

@Entity(tableName = "adjustments")
data class AdjustmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val minutes: Int,
    val note: String?,
    val createdAtMillis: Long,
) {
    fun toCore() = Adjustment(id, LocalDate.ofEpochDay(epochDay), minutes, note)
}

@Entity(tableName = "special_days")
data class SpecialDayEntity(
    @PrimaryKey val epochDay: Long,
    val type: String,
    val targetMinutes: Int,
    /** Free-text reason ("Médico", "San Isidro"...). Added in database version 2. */
    val note: String? = null,
) {
    fun toCore() = SpecialDay(
        LocalDate.ofEpochDay(epochDay),
        runCatching { SpecialDayType.valueOf(type) }.getOrDefault(SpecialDayType.OTHER),
        targetMinutes,
        note,
    )

    companion object {
        fun from(d: SpecialDay) = SpecialDayEntity(d.date.toEpochDay(), d.type.name, d.targetMinutes, d.note?.trim()?.ifEmpty { null })
    }
}
