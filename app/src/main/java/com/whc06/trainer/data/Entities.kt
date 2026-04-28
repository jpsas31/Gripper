package com.whc06.trainer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rep_presets")
data class RepPresetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val workSec: Int,
    val restSec: Int,
    val repsPerSet: Int,
    val sets: Int,
    val restBetweenSetsSec: Int,
    val targetPctMvc: Int?,
    val gripType: String,
    val notes: String,
    val createdAt: Long
)

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val programId: String,
    val programName: String,
    val startedAtMs: Long,
    val endedAtMs: Long,
    val mvcAtStart: Double,
    val hand: String,
    val gripType: String,
    val peakKgOverall: Double,
    val repPeaksJson: String,
    val criticalForceKg: Double?,
    val wPrimeKgSec: Double?
)

@Entity(tableName = "mvc_records")
data class MvcRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hand: String,
    val kg: Double,
    val savedAtMs: Long
)
