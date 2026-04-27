package com.whc06.trainer.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

class BleScanner(context: Context) {

    private val tag = "BleScanner"
    private val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter: BluetoothAdapter? = manager.adapter

    private val _state = MutableStateFlow(State.IDLE)
    val state: StateFlow<State> = _state.asStateFlow()

    private val ch = Channel<WhC06Parser.Sample>(capacity = Channel.CONFLATED)
    val samples: Flow<WhC06Parser.Sample> = ch.receiveAsFlow()

    private var packetCount = 0L
    private var lastRateUpdateNs = 0L
    private val _packetsPerSec = MutableStateFlow(0.0)
    val packetsPerSec: StateFlow<Double> = _packetsPerSec.asStateFlow()

    enum class State { IDLE, SCANNING, NO_BLUETOOTH, BLUETOOTH_OFF }

    private val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val ts = SystemClock.elapsedRealtimeNanos()
            val mfr = result.scanRecord?.manufacturerSpecificData ?: return
            val bytes = mfr.get(WhC06Parser.MANUFACTURER_ID) ?: return
            val sample = WhC06Parser.parse(bytes, ts, result.rssi) ?: return
            ch.trySend(sample)

            packetCount++
            if (lastRateUpdateNs == 0L) lastRateUpdateNs = ts
            val deltaNs = ts - lastRateUpdateNs
            if (deltaNs > 1_000_000_000L) {
                _packetsPerSec.value = packetCount * 1e9 / deltaNs
                packetCount = 0
                lastRateUpdateNs = ts
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(tag, "scan failed: $errorCode")
            _state.value = State.IDLE
        }
    }

    @SuppressLint("MissingPermission")
    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_SCAN])
    fun start() {
        val a = adapter ?: run { _state.value = State.NO_BLUETOOTH; return }
        if (!a.isEnabled) { _state.value = State.BLUETOOTH_OFF; return }
        if (_state.value == State.SCANNING) return

        val filter = ScanFilter.Builder()
            .setManufacturerData(WhC06Parser.MANUFACTURER_ID, byteArrayOf())
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                    setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                    setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    setReportDelay(0)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    setLegacy(true)
                }
            }
            .build()

        a.bluetoothLeScanner?.startScan(listOf(filter), settings, callback) ?: run {
            _state.value = State.BLUETOOTH_OFF
            return
        }
        _state.value = State.SCANNING
    }

    @SuppressLint("MissingPermission")
    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_SCAN])
    fun stop() {
        adapter?.bluetoothLeScanner?.stopScan(callback)
        _state.value = State.IDLE
    }
}
