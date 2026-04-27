package com.whc06.trainer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whc06.trainer.training.Metrics
import com.whc06.trainer.training.SessionLog

@Composable
fun CriticalForceResultScreen(log: SessionLog, onClose: () -> Unit) {
    val peaks = log.reps.map { it.peakKg }
    val cf = Metrics.criticalForceGiles(peaks)
    val avgPullSec = log.reps.firstOrNull()?.let { it.durationMs / 1000.0 } ?: 7.0
    val wPrime = Metrics.wPrime(peaks, cf, avgPullSec)
    val maxPeak = peaks.maxOrNull() ?: 1.0

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .windowInsetsPadding(WindowInsets.systemBars)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Critical Force Result", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(log.programName, color = MaterialTheme.colorScheme.outline, fontSize = 13.sp)

            Spacer(Modifier.height(20.dp))

            ResultRow("Critical Force (CF)", "%.2f kg".format(cf))
            ResultRow("W' (work above CF)", "%.1f kg·s".format(wPrime))
            ResultRow("Reps completed", peaks.size.toString())
            ResultRow("Best rep", "%.2f kg".format(maxPeak))
            ResultRow("MVC at start", "%.2f kg".format(log.mvcAtStart))
            if (log.mvcAtStart > 0) ResultRow("CF / MVC", "%.0f%%".format(cf / log.mvcAtStart * 100))
            ResultRow("Hand", log.hand.name)
            ResultRow("Duration", "%.1fs".format(log.durationMs / 1000.0))

            Spacer(Modifier.height(20.dp))
            Text("Per-rep peak force", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            RepBarChart(peaks = peaks, cf = cf, maxPeak = maxPeak)

            Spacer(Modifier.height(8.dp))
            Text(
                "Bar = peak per rep. Solid line = CF (mean of last 6, ±1 SD outliers excluded). Shaded = W' impulse.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(Modifier.height(24.dp))
            Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                Text("Close")
            }
        }
    }
}

@Composable
private fun ResultRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = MaterialTheme.colorScheme.outline, fontSize = 13.sp)
        Spacer(Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
private fun RepBarChart(peaks: List<Double>, cf: Double, maxPeak: Double) {
    val accent = MaterialTheme.colorScheme.primary
    val cfColor = MaterialTheme.colorScheme.secondary
    val cfFill = Color(0x40FF6B35)
    Canvas(Modifier.fillMaxWidth().height(180.dp)) {
        val w = size.width; val h = size.height
        if (peaks.isEmpty() || maxPeak <= 0) return@Canvas
        val barW = w / peaks.size
        val cfY = h - (cf / maxPeak).toFloat() * h

        peaks.forEachIndexed { i, v ->
            val barH = (v / maxPeak).toFloat() * h
            val x = i * barW
            if (v > cf) {
                drawRect(
                    color = cfFill,
                    topLeft = Offset(x, cfY),
                    size = Size(barW * 0.85f, (h - cfY) - (h - barH))
                )
            }
            drawRect(
                color = accent,
                topLeft = Offset(x, h - barH),
                size = Size(barW * 0.85f, barH.coerceAtLeast(2f))
            )
        }
        if (cf > 0) {
            drawLine(cfColor, Offset(0f, cfY), Offset(w, cfY), strokeWidth = 3f)
        }
    }
}
