package com.whc06.trainer.data

import android.content.Context
import com.whc06.trainer.training.GripType
import com.whc06.trainer.training.Metrics
import com.whc06.trainer.training.RepPreset
import com.whc06.trainer.training.SessionLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class Repository(private val ctx: Context) {
    private val db = AppDatabase.get(ctx)
    private val presetDao = db.presetDao()
    private val sessionDao = db.sessionDao()
    private val jsonCodec = Json { ignoreUnknownKeys = true }
    private val peaksSerializer = ListSerializer(Double.serializer())

    val presets: Flow<List<RepPreset>> = presetDao.observeAll().map { rows ->
        rows.map { it.toDomain() }
    }

    val sessions: Flow<List<SessionEntity>> = sessionDao.observeRecent()

    suspend fun seedPresetsIfEmpty() {
        if (presetDao.count() == 0) {
            RepPreset.SAMPLES.forEach { upsertPreset(it) }
        }
    }

    suspend fun upsertPreset(p: RepPreset) {
        presetDao.upsert(
            RepPresetEntity(
                id = p.id,
                name = p.name,
                workSec = p.workSec,
                restSec = p.restSec,
                repsPerSet = p.repsPerSet,
                sets = p.sets,
                restBetweenSetsSec = p.restBetweenSetsSec,
                targetPctMvc = p.targetPctMvc,
                gripType = p.gripType.name,
                notes = p.notes,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deletePreset(id: String) = presetDao.delete(id)

    suspend fun saveSession(log: SessionLog, gripType: GripType) {
        val peaks = log.reps.map { it.peakKg }
        val isCf = log.programId.contains("cf", ignoreCase = true)
        val cf = if (isCf) Metrics.criticalForceGiles(peaks) else null
        val avgPullSec = log.reps.firstOrNull()?.let { it.durationMs / 1000.0 } ?: 7.0
        val wPrime = if (isCf && cf != null) Metrics.wPrime(peaks, cf, avgPullSec) else null
        sessionDao.insert(
            SessionEntity(
                programId = log.programId,
                programName = log.programName,
                startedAtMs = log.startedAtMs,
                endedAtMs = log.endedAtMs,
                mvcAtStart = log.mvcAtStart,
                hand = log.hand.name,
                gripType = gripType.name,
                peakKgOverall = log.peakKgOverall,
                repPeaksJson = jsonCodec.encodeToString(peaksSerializer, peaks),
                criticalForceKg = cf,
                wPrimeKgSec = wPrime
            )
        )
    }

    suspend fun deleteAllSessions() = sessionDao.deleteAll()

    fun decodePeaks(s: String): List<Double> = try {
        jsonCodec.decodeFromString(peaksSerializer, s)
    } catch (_: Exception) { emptyList() }

    private fun RepPresetEntity.toDomain() = RepPreset(
        id = id, name = name, workSec = workSec, restSec = restSec,
        repsPerSet = repsPerSet, sets = sets, restBetweenSetsSec = restBetweenSetsSec,
        targetPctMvc = targetPctMvc,
        gripType = runCatching { GripType.valueOf(gripType) }.getOrElse { GripType.HALF_CRIMP },
        notes = notes
    )
}
