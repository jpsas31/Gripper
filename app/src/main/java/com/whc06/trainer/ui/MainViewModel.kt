package com.whc06.trainer.ui

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.whc06.trainer.ble.BleScanner
import com.whc06.trainer.ble.ExponentialSmoother
import com.whc06.trainer.ble.PeakTracker
import com.whc06.trainer.ble.WhC06Parser
import com.whc06.trainer.data.MvcRecordEntity
import com.whc06.trainer.data.Prefs
import com.whc06.trainer.data.Repository
import com.whc06.trainer.data.SessionEntity
import com.whc06.trainer.service.ScannerService
import com.whc06.trainer.training.CommonMvc
import com.whc06.trainer.training.GripType
import com.whc06.trainer.training.Hand
import com.whc06.trainer.training.Program
import com.whc06.trainer.training.ProgramLibrary
import com.whc06.trainer.training.RepPreset
import com.whc06.trainer.training.SessionEngine
import com.whc06.trainer.training.SessionListener
import com.whc06.trainer.training.SessionLog
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repository(app)
    private val scanner = BleScanner(app)
    private val coach = Coach(app)
    private val smoother = ExponentialSmoother(alpha = 0.4)
    private val peakTracker = PeakTracker(windowMs = 300L)

    val scanState: StateFlow<BleScanner.State> = scanner.state
    val packetsPerSec: StateFlow<Double> = scanner.packetsPerSec

    private val _liveKg = MutableStateFlow(0.0)
    val liveKg: StateFlow<Double> = _liveKg.asStateFlow()

    private val _smoothedKg = MutableStateFlow(0.0)
    val smoothedKg: StateFlow<Double> = _smoothedKg.asStateFlow()

    private val _peakKg = MutableStateFlow(0.0)
    val peakKg: StateFlow<Double> = _peakKg.asStateFlow()

    private val _stable = MutableStateFlow(false)
    val stable: StateFlow<Boolean> = _stable.asStateFlow()

    private val _tareKg = MutableStateFlow(0.0)
    val tareKg: StateFlow<Double> = _tareKg.asStateFlow()

    private val _commonMvc = MutableStateFlow(CommonMvc())
    val commonMvc: StateFlow<CommonMvc> = _commonMvc.asStateFlow()

    private val _selectedHand = MutableStateFlow(Hand.BOTH)
    val selectedHand: StateFlow<Hand> = _selectedHand.asStateFlow()

    private val _gripType = MutableStateFlow(GripType.HALF_CRIMP)
    val gripType: StateFlow<GripType> = _gripType.asStateFlow()

    val effectiveMvcKg: StateFlow<Double> =
        combine(_commonMvc, _selectedHand) { mvc, hand -> mvc.forHand(hand) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    private val _targetPct = MutableStateFlow(70)
    val targetPct: StateFlow<Int> = _targetPct.asStateFlow()

    private val _zoneTolerancePct = MutableStateFlow(5)
    val zoneTolerancePct: StateFlow<Int> = _zoneTolerancePct.asStateFlow()

    private val _stableOnly = MutableStateFlow(false)
    val stableOnly: StateFlow<Boolean> = _stableOnly.asStateFlow()

    private val _ttsEnabled = MutableStateFlow(true)
    val ttsEnabled: StateFlow<Boolean> = _ttsEnabled.asStateFlow()

    private val _hapticEnabled = MutableStateFlow(true)
    val hapticEnabled: StateFlow<Boolean> = _hapticEnabled.asStateFlow()

    private val _cfTutorialSeen = MutableStateFlow(false)
    val cfTutorialSeen: StateFlow<Boolean> = _cfTutorialSeen.asStateFlow()

    val targetKg: StateFlow<Double> =
        combine(effectiveMvcKg, _targetPct) { mvc, pct -> if (mvc > 0) mvc * pct / 100.0 else 0.0 }
            .stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    private val _timeInZoneMs = MutableStateFlow(0L)
    val timeInZoneMs: StateFlow<Long> = _timeInZoneMs.asStateFlow()
    private var lastSampleTsMs: Long = 0L

    private val _selectedProgram = MutableStateFlow<Program?>(null)
    val selectedProgram: StateFlow<Program?> = _selectedProgram.asStateFlow()

    private var session: SessionEngine? = null
    private var sessionCollectJob: Job? = null
    private val _sessionState = MutableStateFlow(com.whc06.trainer.training.SessionState())
    val sessionState: StateFlow<com.whc06.trainer.training.SessionState> = _sessionState.asStateFlow()

    private val _lastLog = MutableStateFlow<SessionLog?>(null)
    val lastLog: StateFlow<SessionLog?> = _lastLog.asStateFlow()

    private val _repCompleteEvents = MutableSharedFlow<Pair<Int, Double>>(extraBufferCapacity = 8)
    val repCompleteEvents: SharedFlow<Pair<Int, Double>> = _repCompleteEvents.asSharedFlow()

    private val samplesDeque = ArrayDeque<Pair<Long, Double>>()
    private val _recentSamples = MutableStateFlow<List<Pair<Long, Double>>>(emptyList())
    val recentSamples: StateFlow<List<Pair<Long, Double>>> = _recentSamples.asStateFlow()
    private val maxHistoryMs = 30_000L

    private val _presets = MutableStateFlow<List<RepPreset>>(emptyList())
    val presets: StateFlow<List<RepPreset>> = _presets.asStateFlow()

    val recentSessions: StateFlow<List<SessionEntity>> =
        repo.sessions.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val mvcRecords: StateFlow<List<MvcRecordEntity>> =
        repo.mvcRecords.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _partyPlayers = MutableStateFlow<List<PartyPlayer>>(emptyList())
    val partyPlayers: StateFlow<List<PartyPlayer>> = _partyPlayers.asStateFlow()

    fun partyAddPlayer(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        if (_partyPlayers.value.any { it.name.equals(trimmed, ignoreCase = true) }) return
        _partyPlayers.value = _partyPlayers.value +
            PartyPlayer(id = java.util.UUID.randomUUID().toString(), name = trimmed)
    }

    fun partyRemovePlayer(id: String) {
        _partyPlayers.value = _partyPlayers.value.filterNot { it.id == id }
    }

    fun partyResetAll() {
        _partyPlayers.value = emptyList()
    }

    fun partyResetPlayerScores(id: String) {
        _partyPlayers.value = _partyPlayers.value.map {
            if (it.id == id) it.copy(leftKg = 0.0, rightKg = 0.0, bothKg = 0.0) else it
        }
    }

    fun partyCapturePeak(playerId: String): Pair<Hand, Double>? {
        val peak = _peakKg.value
        val hand = _selectedHand.value
        if (peak <= 0) return null
        _partyPlayers.value = _partyPlayers.value.map {
            if (it.id == playerId) it.withCapture(hand, peak) else it
        }
        return hand to peak
    }

    val programs: List<Program> get() = ProgramLibrary.all + _presets.value.map { it.toProgram() }

    init {
        viewModelScope.launch { repo.seedPresetsIfEmpty() }
        viewModelScope.launch { scanner.samples.collect { onSample(it) } }
        viewModelScope.launch { repo.presets.collect { _presets.value = it } }
        viewModelScope.launch {
            var lastHand: Hand? = null
            Prefs.observe(app).collect { st ->
                _commonMvc.value = st.mvc
                if (lastHand != null && lastHand != st.hand) {
                    _peakKg.value = 0.0
                    peakTracker.reset()
                }
                lastHand = st.hand
                _selectedHand.value = st.hand
                _tareKg.value = st.tareKg
                _targetPct.value = st.targetPct
                _zoneTolerancePct.value = st.zoneTolPct
                _stableOnly.value = st.stableOnly
                _ttsEnabled.value = st.ttsEnabled
                _hapticEnabled.value = st.hapticEnabled
                _gripType.value = st.gripType
                _cfTutorialSeen.value = st.cfTutorialSeen
                coach.enabled = st.ttsEnabled
                coach.hapticEnabled = st.hapticEnabled
            }
        }
    }

    override fun onCleared() {
        coach.shutdown()
        super.onCleared()
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        ScannerService.start(getApplication())
        scanner.start()
    }
    @SuppressLint("MissingPermission")
    fun stopScan() {
        scanner.stop()
        ScannerService.stop(getApplication())
    }

    fun tare() {
        val v = _smoothedKg.value
        viewModelScope.launch { Prefs.setTare(getApplication(), v) }
    }
    fun untare() = viewModelScope.launch { Prefs.setTare(getApplication(), 0.0) }

    fun selectHand(h: Hand): Job? {
        if (_sessionState.value.running) return null
        return viewModelScope.launch { Prefs.setHand(getApplication(), h) }
    }

    fun selectGrip(g: GripType) =
        viewModelScope.launch { Prefs.setGripType(getApplication(), g) }

    fun setTargetPct(pct: Int) =
        viewModelScope.launch { Prefs.setTargetPct(getApplication(), pct.coerceIn(0, 200)) }

    fun setZoneTolerance(pct: Int) =
        viewModelScope.launch { Prefs.setZoneTolerance(getApplication(), pct.coerceIn(1, 50)) }

    fun setStableOnly(on: Boolean) =
        viewModelScope.launch { Prefs.setStableOnly(getApplication(), on) }

    fun setTtsEnabled(on: Boolean) =
        viewModelScope.launch { Prefs.setTtsEnabled(getApplication(), on) }

    fun setHapticEnabled(on: Boolean) =
        viewModelScope.launch { Prefs.setHapticEnabled(getApplication(), on) }

    fun markCfTutorialSeen() =
        viewModelScope.launch { Prefs.setCfTutorialSeen(getApplication(), true) }

    fun resetTimeInZone() { _timeInZoneMs.value = 0L }

    fun saveCurrentPeakAsMvc() {
        val peak = _peakKg.value
        setMvcForCurrentHand(peak, CommonMvc.Source.MVC_ASSESSMENT)
    }

    fun setManualMvc(hand: Hand, kg: Double) {
        val curr = _commonMvc.value
        val updated = when (hand) {
            Hand.BOTH -> curr.copy(bilateralKg = kg, source = CommonMvc.Source.MANUAL, updatedAt = System.currentTimeMillis())
            Hand.LEFT -> curr.copy(leftKg = kg, source = CommonMvc.Source.MANUAL, updatedAt = System.currentTimeMillis())
            Hand.RIGHT -> curr.copy(rightKg = kg, source = CommonMvc.Source.MANUAL, updatedAt = System.currentTimeMillis())
        }
        viewModelScope.launch { Prefs.setMvc(getApplication(), updated) }
    }

    private fun setMvcForCurrentHand(kg: Double, source: CommonMvc.Source) {
        val curr = _commonMvc.value
        val hand = _selectedHand.value
        val updated = when (hand) {
            Hand.BOTH -> curr.copy(bilateralKg = kg, source = source, updatedAt = System.currentTimeMillis())
            Hand.LEFT -> curr.copy(leftKg = kg, source = source, updatedAt = System.currentTimeMillis())
            Hand.RIGHT -> curr.copy(rightKg = kg, source = source, updatedAt = System.currentTimeMillis())
        }
        viewModelScope.launch {
            Prefs.setMvc(getApplication(), updated)
            if (kg > 0) repo.saveMvcRecord(hand, kg)
        }
    }

    fun resetPeak() {
        _peakKg.value = 0.0
        peakTracker.reset()
    }

    fun selectProgram(program: Program) {
        _selectedProgram.value = program
        session?.stop()
        sessionCollectJob?.cancel()
        val s = SessionEngine(
            program = program,
            scope = viewModelScope,
            mvcKg = effectiveMvcKg.value,
            hand = _selectedHand.value,
            preRollSec = 3,
            listener = object : SessionListener {
                override fun onPreRoll(secondsLeft: Int) { coach.countdown(secondsLeft) }
                override fun onWorkStart(label: String) { coach.startWork("Pull") }
                override fun onRestStart() { coach.startRest() }
                override fun onPhaseEnd(repIndex: Int, peakKg: Double) {
                    coach.phaseEnd()
                    _repCompleteEvents.tryEmit(repIndex to peakKg)
                }
                override fun onSessionEnd() { coach.say("Session complete", urgent = true) }
            }
        )
        session = s
        sessionCollectJob = viewModelScope.launch {
            s.state.collect { st ->
                _sessionState.value = st
                if (st.finished && st.log != null) {
                    _lastLog.value = st.log
                    repo.saveSession(st.log!!, _gripType.value)
                }
            }
        }
    }

    fun startSession() {
        val s = session ?: return
        smoother.reset()
        peakTracker.reset()
        _peakKg.value = 0.0
        _lastLog.value = null
        s.start()
    }

    fun pauseSession() = session?.pause()
    fun resumeSession() = session?.resume()
    fun skipPhase() = session?.skip()
    fun stopSession() = session?.stop()

    fun savePreset(preset: RepPreset) {
        viewModelScope.launch { repo.upsertPreset(preset) }
    }

    fun deletePreset(id: String) {
        viewModelScope.launch { repo.deletePreset(id) }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repo.deleteAllSessions()
            repo.deleteAllMvcRecords()
        }
    }

    fun clearChartHistory() {
        synchronized(samplesDeque) {
            samplesDeque.clear()
            _recentSamples.value = emptyList()
        }
    }

    fun decodePeaks(json: String) = repo.decodePeaks(json)

    private fun onSample(s: WhC06Parser.Sample) {
        if (_stableOnly.value && !s.stable) return
        val raw = s.kg - _tareKg.value
        _liveKg.value = raw
        val smooth = smoother.next(raw)
        _smoothedKg.value = smooth
        _stable.value = s.stable

        val p = peakTracker.update(smooth, s.timestampNanos)
        if (p > _peakKg.value) _peakKg.value = p

        val nowMs = s.timestampNanos / 1_000_000L
        val snapshot: List<Pair<Long, Double>>
        synchronized(samplesDeque) {
            samplesDeque.addLast(nowMs to smooth)
            while (samplesDeque.isNotEmpty() && nowMs - samplesDeque.first().first > maxHistoryMs) {
                samplesDeque.removeFirst()
            }
            snapshot = samplesDeque.toList()
        }
        _recentSamples.value = snapshot

        val tk = targetKg.value
        if (tk > 0) {
            val tol = tk * _zoneTolerancePct.value / 100.0
            val inZone = smooth in (tk - tol)..(tk + tol)
            if (inZone && lastSampleTsMs > 0) {
                _timeInZoneMs.value += (nowMs - lastSampleTsMs).coerceAtMost(500L)
            }
        }
        lastSampleTsMs = nowMs

        session?.reportSample(smooth)
    }
}

data class PartyPlayer(
    val id: String,
    val name: String,
    val leftKg: Double = 0.0,
    val rightKg: Double = 0.0,
    val bothKg: Double = 0.0
) {
    fun best(): Double = maxOf(leftKg, rightKg, bothKg)
    fun forHand(h: Hand): Double = when (h) {
        Hand.LEFT -> leftKg
        Hand.RIGHT -> rightKg
        Hand.BOTH -> bothKg
    }
    fun withCapture(h: Hand, kg: Double): PartyPlayer = when (h) {
        Hand.LEFT -> copy(leftKg = maxOf(leftKg, kg))
        Hand.RIGHT -> copy(rightKg = maxOf(rightKg, kg))
        Hand.BOTH -> copy(bothKg = maxOf(bothKg, kg))
    }
}
