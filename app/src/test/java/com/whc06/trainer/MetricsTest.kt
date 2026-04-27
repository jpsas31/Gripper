package com.whc06.trainer

import com.whc06.trainer.training.Metrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MetricsTest {

    @Test fun avgOfPeaks_basic() {
        assertEquals(20.0, Metrics.avgOfPeaks(listOf(10.0, 20.0, 30.0)), 1e-6)
    }

    @Test fun avgOfPeaks_empty() {
        assertEquals(0.0, Metrics.avgOfPeaks(emptyList()), 1e-6)
    }

    @Test fun cf_returnsZeroWhenInsufficientReps() {
        assertEquals(0.0, Metrics.criticalForceGiles(listOf(30.0, 28.0, 26.0)), 1e-6)
    }

    @Test fun cf_basicLastSix() {
        // 24 reps, last 6 = 20,21,19,20,22,18 → mean=20, sd≈1.41, all within 1 SD → mean=20
        val reps = (1..18).map { 30.0 } + listOf(20.0, 21.0, 19.0, 20.0, 22.0, 18.0)
        val cf = Metrics.criticalForceGiles(reps)
        assertEquals(20.0, cf, 1e-3)
    }

    @Test fun cf_excludesOutliersAbove1SD() {
        // Last 6: 20,20,20,20,20,40 → mean≈23.33, sd≈8.16
        // 40 - 23.33 = 16.67 > sd → 40 excluded
        // remaining mean = 20
        val reps = (1..18).map { 30.0 } + listOf(20.0, 20.0, 20.0, 20.0, 20.0, 40.0)
        val cf = Metrics.criticalForceGiles(reps)
        assertEquals(20.0, cf, 1e-3)
    }

    @Test fun wPrime_sumsAboveCf() {
        // peaks 10, 5, 8 ; cf=6 ; pull=1s → (4 + 0 + 2)*1 = 6
        val w = Metrics.wPrime(listOf(10.0, 5.0, 8.0), 6.0, 1.0)
        assertEquals(6.0, w, 1e-6)
    }

    @Test fun rfd2080_returnsNullForTooFewSamples() {
        assertNull(Metrics.rfd2080(listOf(0L to 1.0, 100L to 5.0)))
    }

    @Test fun rfd2080_basicSlope() {
        // Linear ramp 0→10kg over 1000ms. 20%=2kg at 200ms. 80%=8kg at 800ms.
        // slope = (8-2) / 0.6s = 10 kg/s
        val samples = (0L..1000L step 50L).map { it to (it / 100.0) }
        val rfd = Metrics.rfd2080(samples) ?: error("expected non-null")
        assertEquals(10.0, rfd, 0.5)
    }

    @Test fun rfdInterval_basicSlope() {
        val samples = (0L..500L step 50L).map { it to (it / 100.0) }
        val rfd = Metrics.rfdInterval(samples, windowMs = 250L) ?: error("expected non-null")
        // 0kg → 2.5kg over 250ms = 10 kg/s
        assertTrue("rfd should be ~10, got $rfd", kotlin.math.abs(rfd - 10.0) < 0.5)
    }
}
