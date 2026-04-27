package com.whc06.trainer.ble

class ExponentialSmoother(private val alpha: Double = 0.4) {
    private var ema = Double.NaN

    fun reset() { ema = Double.NaN }

    fun next(x: Double): Double {
        ema = if (ema.isNaN()) x else alpha * x + (1 - alpha) * ema
        return ema
    }
}

class PeakTracker(private val windowMs: Long = 300L) {
    private data class Entry(val ts: Long, val v: Double)
    private val buf = ArrayDeque<Entry>()
    var peak: Double = 0.0; private set

    fun reset() { buf.clear(); peak = 0.0 }

    fun update(value: Double, tsNanos: Long): Double {
        val tsMs = tsNanos / 1_000_000L
        buf.addLast(Entry(tsMs, value))
        while (buf.isNotEmpty() && tsMs - buf.first().ts > windowMs) buf.removeFirst()
        peak = buf.maxOf { it.v }
        return peak
    }
}
