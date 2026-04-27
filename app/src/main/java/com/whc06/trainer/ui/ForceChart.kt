package com.whc06.trainer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ForceChart(
    samples: List<Pair<Long, Double>>,
    modifier: Modifier = Modifier,
    targetKg: Double? = null,
    targetPctMvc: Int? = null,
    zoneTolerancePct: Int = 5,
    autoScale: Boolean = true,
    maxScaleKg: Double = 100.0,
    windowMs: Long = 30_000L,
    lineColor: Color? = null,
    showAxisLabels: Boolean = true
) {
    val accent = lineColor ?: MaterialTheme.colorScheme.primary
    val targetColor = MaterialTheme.colorScheme.secondary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val axisText = MaterialTheme.colorScheme.onSurfaceVariant
    val zoneFill = Color(0x4040E0D0)
    val measurer: TextMeasurer = rememberTextMeasurer()
    val rightInset = if (showAxisLabels) 36f else 0f
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)

    Canvas(modifier.fillMaxWidth().defaultMinSize(minHeight = 140.dp)) {
        val w = size.width
        val h = size.height
        val plotW = (w - rightInset).coerceAtLeast(1f)
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
            drawLine(grid, Offset(0f, y), Offset(plotW, y), strokeWidth = 1f)
        }

        if (showAxisLabels) {
            val ticks = listOf(0f, 0.25f, 0.5f, 0.75f, 1f)
            for (t in ticks) {
                val kgVal = effectiveMax * (1f - t)
                val y = h * t
                val txt = "%.0f".format(kgVal)
                val layout = measurer.measure(
                    txt,
                    style = TextStyle(fontSize = 9.sp, color = axisText)
                )
                drawText(
                    layout,
                    topLeft = Offset(plotW + 4f, (y - layout.size.height / 2f).coerceIn(0f, h - layout.size.height.toFloat()))
                )
            }
        }

        targetKg?.takeIf { it > 0 }?.let { tk ->
            val tol = tk * zoneTolerancePct / 100.0
            val yTop = h - ((tk + tol) / effectiveMax).coerceIn(0.0, 1.0).toFloat() * h
            val yBot = h - ((tk - tol) / effectiveMax).coerceIn(0.0, 1.0).toFloat() * h
            drawRect(
                color = zoneFill,
                topLeft = Offset(0f, yTop),
                size = Size(plotW, (yBot - yTop).coerceAtLeast(2f))
            )
            val yMid = h - (tk / effectiveMax).coerceIn(0.0, 1.0).toFloat() * h
            val path = Path().apply {
                moveTo(0f, yMid)
                lineTo(plotW, yMid)
            }
            drawPath(path, color = targetColor, style = Stroke(width = 2f, pathEffect = dashEffect))

            targetPctMvc?.let { pct ->
                val labelTxt = "${pct}%"
                val layout = measurer.measure(
                    labelTxt,
                    style = TextStyle(fontSize = 9.sp, color = targetColor)
                )
                drawText(
                    layout,
                    topLeft = Offset(plotW - layout.size.width - 4f, (yMid - layout.size.height - 2f).coerceAtLeast(0f))
                )
            }
        }

        val path = Path()
        var first = true
        for ((ts, kg) in samples) {
            if (ts < tMin) continue
            val x = ((ts - tMin).toFloat() / windowMs.toFloat()) * plotW
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
