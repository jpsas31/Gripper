package com.whc06.trainer.training

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class SessionState(
    val running: Boolean = false,
    val paused: Boolean = false,
    val finished: Boolean = false,
    val setIndex: Int = 0,
    val phaseIndex: Int = 0,
    val phaseLabel: String = "",
    val phaseTotalMs: Long = 0,
    val phaseElapsedMs: Long = 0,
    val isWork: Boolean = false,
    val isPreRoll: Boolean = false,
    val targetPctMvc: Int? = null,
    val totalSets: Int = 0,
    val workRepsCompleted: Int = 0,
    val totalWorkReps: Int = 0,
    val peakKgInPhase: Double = 0.0,
    val peakKgOverall: Double = 0.0,
    val log: SessionLog? = null
)

interface SessionListener {
    fun onPreRoll(secondsLeft: Int) {}
    fun onWorkStart(label: String) {}
    fun onRestStart() {}
    fun onPhaseEnd(repIndex: Int, peakKg: Double) {}
    fun onSessionEnd() {}
}

class SessionEngine(
    private val program: Program,
    private val scope: CoroutineScope,
    val mvcKg: Double = 0.0,
    private val hand: Hand = Hand.BOTH,
    private val preRollSec: Int = 3,
    private val listener: SessionListener? = null
) {
    private val totalWorkReps = program.sets.sumOf { it.phases.count { p -> p.isWork } }
    private val _state = MutableStateFlow(
        SessionState(totalSets = program.sets.size, totalWorkReps = totalWorkReps)
    )
    val state: StateFlow<SessionState> = _state.asStateFlow()

    private var job: Job? = null
    private val reps = mutableListOf<RepRecord>()
    private val phaseSamples = mutableListOf<Pair<Long, Double>>()
    private var phaseSum = 0.0
    private var phaseCount = 0
    private var startedAt = 0L
    @Volatile private var paused = false
    @Volatile private var skipRequested = false

    fun targetKgFor(pct: Int?): Double? =
        if (pct == null || mvcKg <= 0.0) null else mvcKg * pct / 100.0

    fun reportSample(kg: Double) {
        val s = _state.value
        if (!s.running || !s.isWork || s.isPreRoll || paused) return
        val now = System.currentTimeMillis()
        phaseSamples.add(now to kg)
        phaseSum += kg
        phaseCount++
        _state.value = s.copy(
            peakKgInPhase = maxOf(s.peakKgInPhase, kg),
            peakKgOverall = maxOf(s.peakKgOverall, kg)
        )
    }

    fun start() {
        if (job?.isActive == true) return
        startedAt = System.currentTimeMillis()
        reps.clear()
        paused = false
        skipRequested = false
        _state.value = SessionState(
            running = true, totalSets = program.sets.size, totalWorkReps = totalWorkReps
        )
        job = scope.launch { run() }
    }

    fun pause() { paused = true; _state.value = _state.value.copy(paused = true) }
    fun resume() { paused = false; _state.value = _state.value.copy(paused = false) }
    fun skip() { skipRequested = true }

    fun stop() {
        paused = false
        skipRequested = false
        job?.cancel()
        emitLog(finished = false)
    }

    private suspend fun run() {
        var workCompleted = 0
        program.sets.forEachIndexed { setIdx, set ->
            set.phases.forEachIndexed { phaseIdx, phase ->
                phaseSamples.clear()
                phaseSum = 0.0
                phaseCount = 0

                if (phase.isWork && preRollSec > 0) {
                    runPreRoll(setIdx, phaseIdx, phase)
                }

                _state.value = _state.value.copy(
                    setIndex = setIdx,
                    phaseIndex = phaseIdx,
                    phaseLabel = phase.label,
                    phaseTotalMs = phase.durationMs,
                    phaseElapsedMs = 0L,
                    isWork = phase.isWork,
                    isPreRoll = false,
                    targetPctMvc = phase.targetPctMvc,
                    peakKgInPhase = 0.0
                )
                if (phase.isWork) listener?.onWorkStart(phase.label)
                else if (phase.label.contains("Rest", ignoreCase = true)) listener?.onRestStart()

                tickPhase(phase.durationMs)

                if (phase.isWork) {
                    workCompleted++
                    val peak = _state.value.peakKgInPhase
                    listener?.onPhaseEnd(workCompleted - 1, peak)
                    reps.add(
                        RepRecord(
                            phaseLabel = phase.label,
                            durationMs = phase.durationMs,
                            peakKg = peak,
                            avgKg = if (phaseCount > 0) phaseSum / phaseCount else 0.0,
                            targetPctMvc = phase.targetPctMvc,
                            samples = phaseSamples.toList()
                        )
                    )
                    _state.value = _state.value.copy(workRepsCompleted = workCompleted)
                }
            }
            if (set.restAfterMs > 0 && setIdx < program.sets.lastIndex) {
                _state.value = _state.value.copy(
                    phaseLabel = "Set Rest",
                    phaseTotalMs = set.restAfterMs,
                    phaseElapsedMs = 0L,
                    isWork = false,
                    targetPctMvc = null
                )
                listener?.onRestStart()
                tickPhase(set.restAfterMs)
            }
        }
        listener?.onSessionEnd()
        emitLog(finished = true)
    }

    private suspend fun runPreRoll(setIdx: Int, phaseIdx: Int, phase: Phase) {
        for (sec in preRollSec downTo 1) {
            _state.value = _state.value.copy(
                setIndex = setIdx,
                phaseIndex = phaseIdx,
                phaseLabel = "Get Ready",
                phaseTotalMs = preRollSec * 1000L,
                phaseElapsedMs = ((preRollSec - sec) * 1000L),
                isWork = false,
                isPreRoll = true,
                targetPctMvc = phase.targetPctMvc,
                peakKgInPhase = 0.0
            )
            listener?.onPreRoll(sec)
            val tickStart = System.currentTimeMillis()
            while (System.currentTimeMillis() - tickStart < 1000L) {
                if (skipRequested) { skipRequested = false; return }
                while (paused) delay(50L)
                delay(20L)
            }
        }
    }

    private fun emitLog(finished: Boolean) {
        val log = SessionLog(
            programId = program.id,
            programName = program.name,
            startedAtMs = startedAt,
            endedAtMs = System.currentTimeMillis(),
            mvcAtStart = mvcKg,
            hand = hand,
            reps = reps.toList()
        )
        _state.value = _state.value.copy(running = false, finished = finished, log = log)
    }

    private suspend fun tickPhase(totalMs: Long) {
        val tickMs = 16L
        var elapsed = 0L
        var lastWall = System.currentTimeMillis()
        while (kotlinx.coroutines.coroutineScope { isActive }) {
            if (skipRequested) { skipRequested = false; _state.value = _state.value.copy(phaseElapsedMs = totalMs); return }
            if (paused) {
                lastWall = System.currentTimeMillis()
                delay(50L)
                continue
            }
            val now = System.currentTimeMillis()
            elapsed += (now - lastWall)
            lastWall = now
            if (elapsed >= totalMs) {
                _state.value = _state.value.copy(phaseElapsedMs = totalMs)
                return
            }
            _state.value = _state.value.copy(phaseElapsedMs = elapsed)
            delay(tickMs)
        }
    }
}
