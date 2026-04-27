package com.whc06.trainer.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ForceGauge(
    kg: Double,
    peakKg: Double,
    targetKg: Double?,
    zoneTolerancePct: Int = 5,
    autoScale: Boolean = true,
    minScaleKg: Double = 30.0,
    maxScaleKg: Double = 100.0,
    height: Dp = 320.dp,
    modifier: Modifier = Modifier
) {
    val effectiveMax = if (autoScale) {
        val candidates = listOfNotNull(peakKg, targetKg, kg).filter { it > 0 }
        val raw = (candidates.maxOrNull() ?: 0.0) * 1.25
        maxOf(minScaleKg, raw).coerceAtMost(300.0)
    } else maxScaleKg

    val anim = remember { Animatable(0f) }
    val target = (kg / effectiveMax).coerceIn(0.0, 1.0).toFloat()
    LaunchedEffect(target) {
        val rising = target > anim.value
        anim.animateTo(
            target,
            animationSpec = if (rising) {
                spring(stiffness = 1400f, dampingRatio = 0.7f)
            } else {
                spring(stiffness = 200f, dampingRatio = 0.95f)
            }
        )
    }
    val animatedFrac = anim.value
    val peakFrac = (peakKg / effectiveMax).coerceIn(0.0, 1.0).toFloat()
    val targetFrac = targetKg?.let { (it / effectiveMax).coerceIn(0.0, 1.0).toFloat() }

    val accent = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val secondary = MaterialTheme.colorScheme.secondary

    Box(modifier = modifier.fillMaxWidth().height(height), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxWidth().height(height)) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h * 0.95f
            val radius = minOf(w / 2.2f, h * 0.95f)

            val startAngle = 180f
            val sweepRange = 180f

            drawArc(
                color = track,
                startAngle = startAngle,
                sweepAngle = sweepRange,
                useCenter = false,
                topLeft = Offset(cx - radius, cy - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = 36f)
            )

            val fillColor: Color? = targetKg?.let { tk ->
                val tol = tk * zoneTolerancePct / 100.0
                when {
                    kg in (tk - tol)..(tk + tol) -> Color(0xFF34C759)
                    kg in (tk - tol * 1.5)..(tk + tol * 1.5) -> Color(0xFFFFB627)
                    kg > tk + tol * 1.5 -> Color(0xFFFF3B30)
                    else -> null
                }
            }
            if (fillColor != null) {
                drawArc(
                    color = fillColor,
                    startAngle = startAngle,
                    sweepAngle = sweepRange * animatedFrac,
                    useCenter = false,
                    topLeft = Offset(cx - radius, cy - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = 36f)
                )
            } else {
                drawArc(
                    brush = Brush.sweepGradient(
                        0f to Color(0xFF40E0D0),
                        0.5f to accent,
                        1f to Color(0xFFFF3B30)
                    ),
                    startAngle = startAngle,
                    sweepAngle = sweepRange * animatedFrac,
                    useCenter = false,
                    topLeft = Offset(cx - radius, cy - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = 36f)
                )
            }

            if (peakFrac > 0f) {
                val a = (startAngle + sweepRange * peakFrac) * (Math.PI / 180.0)
                val px = cx + radius * kotlin.math.cos(a).toFloat()
                val py = cy + radius * kotlin.math.sin(a).toFloat()
                drawCircle(color = onSurface, radius = 9f, center = Offset(px, py))
            }

            targetFrac?.let { tf ->
                val a = (startAngle + sweepRange * tf) * (Math.PI / 180.0)
                val tx = cx + radius * kotlin.math.cos(a).toFloat()
                val ty = cy + radius * kotlin.math.sin(a).toFloat()
                drawCircle(color = secondary, radius = 14f, center = Offset(tx, ty), style = Stroke(width = 4f))
            }
        }

        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "%.1f".format(kg),
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                color = onSurface
            )
            Text(text = "kg", fontSize = 18.sp, color = onSurface.copy(alpha = 0.6f))
            if (peakKg > 0) {
                Text(
                    text = "peak %.1f kg".format(peakKg),
                    fontSize = 14.sp,
                    color = onSurface.copy(alpha = 0.7f)
                )
            }
            if (autoScale) {
                Text(
                    text = "scale %.0f".format(effectiveMax),
                    fontSize = 10.sp,
                    color = onSurface.copy(alpha = 0.4f)
                )
            }
        }
    }
}
