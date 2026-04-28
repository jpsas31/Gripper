package com.whc06.trainer.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PresetDao {
    @Query("SELECT * FROM rep_presets ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<RepPresetEntity>>

    @Query("SELECT * FROM rep_presets ORDER BY createdAt DESC")
    suspend fun all(): List<RepPresetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(preset: RepPresetEntity)

    @Query("DELETE FROM rep_presets WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT COUNT(*) FROM rep_presets")
    suspend fun count(): Int
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY startedAtMs DESC LIMIT 100")
    fun observeRecent(): Flow<List<SessionEntity>>

    @Insert
    suspend fun insert(session: SessionEntity): Long

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM sessions")
    suspend fun deleteAll()

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun byId(id: Long): SessionEntity?
}

@Dao
interface MvcRecordDao {
    @Query("SELECT * FROM mvc_records ORDER BY savedAtMs ASC")
    fun observeAll(): Flow<List<MvcRecordEntity>>

    @Insert
    suspend fun insert(record: MvcRecordEntity)

    @Query("DELETE FROM mvc_records")
    suspend fun deleteAll()
}
