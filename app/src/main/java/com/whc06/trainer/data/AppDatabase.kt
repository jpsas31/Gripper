package com.whc06.trainer.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS mvc_records (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        hand TEXT NOT NULL,
                        kg REAL NOT NULL,
                        savedAtMs INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun get(ctx: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                ctx.applicationContext,
                AppDatabase::class.java,
                "whc06.db"
            )
                .addMigrations(MIGRATION_2_3)
                .fallbackToDestructiveMigration()
                .build()
                .also { instance = it }
        }
    }
}
