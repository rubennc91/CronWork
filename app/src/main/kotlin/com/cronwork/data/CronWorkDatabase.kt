package com.cronwork.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [SessionEntity::class, AdjustmentEntity::class, SpecialDayEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class CronWorkDatabase : RoomDatabase() {
    abstract fun workDao(): WorkDao

    companion object {
        fun create(context: Context): CronWorkDatabase =
            Room.databaseBuilder(context, CronWorkDatabase::class.java, "cronwork.db")
                .addMigrations(MIGRATION_1_2)
                .build()

        /** v2: special days get a free-text reason. Existing rows keep everything and get no reason. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `special_days` ADD COLUMN `note` TEXT")
            }
        }
    }
}
