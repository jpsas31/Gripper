package com.whc06.trainer.training

data class RepRecord(
    val phaseLabel: String,
    val durationMs: Long,
    val peakKg: Double,
    val avgKg: Double,
    val targetPctMvc: Int?,
    val samples: List<Pair<Long, Double>>
)

data class SessionLog(
    val programId: String,
    val programName: String,
    val startedAtMs: Long,
    val endedAtMs: Long,
    val mvcAtStart: Double,
    val hand: Hand,
    val reps: List<RepRecord>
) {
    val durationMs: Long get() = endedAtMs - startedAtMs
    val peakKgOverall: Double get() = reps.maxOfOrNull { it.peakKg } ?: 0.0
}
