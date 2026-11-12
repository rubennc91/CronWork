package com.cronwork.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SessionEntity::class, AdjustmentEntity::class, SpecialDayEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class CronWorkDatabase : RoomDatabase() {
    abstract fun workDao(): WorkDao

    companion object {
        fun create(context: Context): CronWorkDatabase =
            Room.databaseBuilder(context, CronWorkDatabase::class.java, "cronwork.db").build()
    }
}
