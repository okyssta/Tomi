package com.tomi.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.Dispatchers

@Database(
    entities = [
        TaskEntity::class,
        JournalEntity::class,
        MovementEntity::class,
        DailyCheckInEntity::class,
        WaterEntity::class,
        StepCacheEntity::class,
        PrayerLogEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class TomiDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun journalDao(): JournalDao
    abstract fun movementDao(): MovementDao
    abstract fun checkInDao(): CheckInDao
    abstract fun waterDao(): WaterDao
    abstract fun stepCacheDao(): StepCacheDao
    abstract fun prayerLogDao(): PrayerLogDao

    companion object {
        @Volatile
        private var instance: TomiDatabase? = null

        fun get(context: Context): TomiDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    TomiDatabase::class.java,
                    context.applicationContext.getDatabasePath("tomi.db").absolutePath,
                )
                    .addMigrations(MIGRATION_1_2)
                    .setQueryCoroutineContext(Dispatchers.IO)
                    .build()
                    .also { instance = it }
            }

        internal val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `prayer_logs` " +
                        "(`date` TEXT NOT NULL, `prayer` TEXT NOT NULL, `offered` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL, PRIMARY KEY(`date`, `prayer`))",
                )
            }
        }
    }
}
