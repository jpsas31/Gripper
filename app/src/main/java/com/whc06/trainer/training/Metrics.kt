package com.whc06.trainer.training

import kotlin.math.sqrt

object Metrics {

    fun avgOfPeaks(repPeaks: List<Double>): Double =
        if (repPeaks.isEmpty()) 0.0 else repPeaks.average()

    /**
     * Critical Force per Giles et al. (2010): mean of last 6 contractions,
     * excluding outliers beyond 1 SD from that mean.
     */
    fun criticalForceGiles(allRepPeaks: List<Double>): Double {
        if (allRepPeaks.size < 6) return 0.0
        val tail = allRepPeaks.takeLast(6)
        val mean = tail.average()
        val sd = stddev(tail)
        val filtered = tail.filter { kotlin.math.abs(it - mean) <= sd }
        return if (filtered.isEmpty()) mean else filtered.average()
    }

    /**
     * W' (W-prime): impulse above critical force during the test.
     * Sum over each rep of max(0, peak - cf) * pullDurationSeconds.
     * Returns kg-seconds (not joules — load-cell limitation).
     */
    fun wPrime(repPeaks: List<Double>, cf: Double, pullDurationSec: Double): Double =
        repPeaks.sumOf { maxOf(0.0, it - cf) } * pullDurationSec

    /**
     * RFD 20-80% protocol per Tindeq:
     * slope of line from intersection of force curve with 20% peak
     * to intersection with 80% peak, in kg/s.
     *
     * NOTE: WH-C06 sample rate ~5-10 Hz makes this unreliable.
     * Use only as rough estimate.
     */
    fun rfd2080(samples: List<Pair<Long, Double>>): Double? {
        if (samples.size < 4) return null
        val peak = samples.maxOf { it.second }
        if (peak <= 0.0) return null
        val t20 = samples.firstOrNull { it.second >= peak * 0.2 } ?: return null
        val t80 = samples.firstOrNull { it.second >= peak * 0.8 } ?: return null
        val dt = (t80.first - t20.first).toDouble() / 1000.0
        if (dt <= 0.0) return null
        return (t80.second - t20.second) / dt
    }

    /**
     * RFD interval: average rate of force development across full pull
     * over a fixed window, default 250ms.
     */
    fun rfdInterval(samples: List<Pair<Long, Double>>, windowMs: Long = 250L): Double? {
        if (samples.size < 2) return null
        val first = samples.first()
        val end = samples.firstOrNull { it.first - first.first >= windowMs } ?: return null
        val dt = (end.first - first.first).toDouble() / 1000.0
        if (dt <= 0.0) return null
        return (end.second - first.second) / dt
    }

    private fun stddev(xs: List<Double>): Double {
        if (xs.size < 2) return 0.0
        val m = xs.average()
        return sqrt(xs.sumOf { (it - m) * (it - m) } / (xs.size - 1))
    }
}

/**
 * Common MVC reference shared across all tests (Tindeq pattern).
 */
data class CommonMvc(
    val bilateralKg: Double = 0.0,
    val leftKg: Double = 0.0,
    val rightKg: Double = 0.0,
    val source: Source = Source.MANUAL,
    val updatedAt: Long = 0L
) {
    enum class Source { MANUAL, PEAK_LOAD_TEST, COMPETITION_PEAK_LOAD, MVC_ASSESSMENT }

    fun forHand(hand: Hand): Double = when (hand) {
        Hand.BOTH -> bilateralKg
        Hand.LEFT -> leftKg
        Hand.RIGHT -> rightKg
    }
}
