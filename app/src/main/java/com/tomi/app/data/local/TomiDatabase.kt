package com.tomi.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

@Database(
    entities = [
        TaskEntity::class,
        JournalEntity::class,
        MovementEntity::class,
        DailyCheckInEntity::class,
        WaterEntity::class,
        StepCacheEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class TomiDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun journalDao(): JournalDao
    abstract fun movementDao(): MovementDao
    abstract fun checkInDao(): CheckInDao
    abstract fun waterDao(): WaterDao
    abstract fun stepCacheDao(): StepCacheDao

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
                    .setDriver(BundledSQLiteDriver())
                    .setQueryCoroutineContext(Dispatchers.IO)
                    .build()
                    .also { instance = it }
            }
    }
}
