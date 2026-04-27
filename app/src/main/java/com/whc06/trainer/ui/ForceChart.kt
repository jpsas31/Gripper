package com.whc06.trainer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ForceChart(
    samples: List<Pair<Long, Double>>,
    targetKg: Double?,
    zoneTolerancePct: Int = 5,
    autoScale: Boolean = true,
    maxScaleKg: Double = 100.0,
    windowMs: Long = 30_000L,
    height: Dp = 140.dp,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    val targetColor = MaterialTheme.colorScheme.secondary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val zoneFill = Color(0x4040E0D0)

    Canvas(modifier.fillMaxWidth().height(height)) {
        val w = size.width
        val h = size.height
        if (samples.isEmpty()) return@Canvas

        val tMax = samples.last().first
        val tMin = tMax - windowMs

        val effectiveMax = if (autoScale) {
            val maxKg = samples.maxOf { it.second }
            val candidates = listOfNotNull(maxKg, targetKg).filter { it > 0 }
            val raw = (candidates.maxOrNull() ?: maxScaleKg) * 1.25
            maxOf(20.0, raw).coerceAtMost(300.0)
        } else maxScaleKg

        for (i in 1..3) {
            val y = h * i / 4f
            drawLine(grid, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
        }

        targetKg?.takeIf { it > 0 }?.let { tk ->
            val tol = tk * zoneTolerancePct / 100.0
            val yTop = h - ((tk + tol) / effectiveMax).coerceIn(0.0, 1.0).toFloat() * h
            val yBot = h - ((tk - tol) / effectiveMax).coerceIn(0.0, 1.0).toFloat() * h
            drawRect(
                color = zoneFill,
                topLeft = Offset(0f, yTop),
                size = Size(w, (yBot - yTop).coerceAtLeast(2f))
            )
            val yMid = h - (tk / effectiveMax).coerceIn(0.0, 1.0).toFloat() * h
            drawLine(targetColor, Offset(0f, yMid), Offset(w, yMid), strokeWidth = 2f)
        }

        val path = Path()
        var first = true
        for ((ts, kg) in samples) {
            if (ts < tMin) continue
            val x = ((ts - tMin).toFloat() / windowMs.toFloat()) * w
            val y = h - (kg / effectiveMax).coerceIn(0.0, 1.0).toFloat() * h
            if (first) {
                path.moveTo(x, y); first = false
            } else {
                path.lineTo(x, y)
            }
        }
        drawPath(path, accent, style = Stroke(width = 4f))
    }
}
