package com.whc06.trainer.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [RepPresetEntity::class, SessionEntity::class, MvcRecordEntity::class],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun presetDao(): PresetDao
    abstract fun sessionDao(): SessionDao
    abstract fun mvcRecordDao(): MvcRecordDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(ctx: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                ctx.applicationContext,
                AppDatabase::class.java,
                "whc06.db"
            ).fallbackToDestructiveMigration().build().also { instance = it }
        }
    }
}
