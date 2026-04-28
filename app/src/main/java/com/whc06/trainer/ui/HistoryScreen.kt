package com.whc06.trainer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whc06.trainer.data.SessionEntity
import java.text.DateFormat
import java.util.Date

@Composable
fun HistoryScreen(vm: MainViewModel, onSessionClick: (SessionEntity) -> Unit) {
    val sessions by vm.recentSessions.collectAsState()
    val prs = remember(sessions) { computePRs(sessions) }
    val byProgram = remember(sessions) { sessions.groupBy { it.programName } }

    var confirmClear by remember { mutableStateOf(false) }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear all history?") },
            text = { Text("Deletes all ${sessions.size} saved session(s). Cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.clearHistory()
                    confirmClear = false
                }) { Text("Clear", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Cancel") }
            }
        )
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "History",
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                modifier = Modifier.weight(1f)
            )
            if (sessions.isNotEmpty()) {
                TextButton(onClick = { confirmClear = true }) {
                    Icon(
                        Icons.Outlined.DeleteSweep,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Clear", fontSize = 13.sp)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        if (sessions.isEmpty()) {
            EmptyHistoryCard()
            return@Column
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item(key = "hand_compare") { HandComparisonCard(sessions) }
            byProgram.forEach { (programName, sList) ->
                item(key = "trend_$programName") {
                    TrendCard(programName, sList)
                }
                items(sList, key = { "session_${it.id}" }) { s ->
                    SessionCard(s, isPr = s.id in prs) { onSessionClick(s) }
                }
            }
        }
    }
}

@Composable
private fun HandComparisonCard(sessions: List<SessionEntity>) {
    val left = sessions.filter { it.hand.equals("LEFT", true) }
    val right = sessions.filter { it.hand.equals("RIGHT", true) }
    val both = sessions.filter { it.hand.equals("BOTH", true) }
    val bestL = left.maxOfOrNull { it.peakKgOverall } ?: 0.0
    val bestR = right.maxOfOrNull { it.peakKgOverall } ?: 0.0
    val bestB = both.maxOfOrNull { it.peakKgOverall } ?: 0.0
    val asymPct = if (bestL > 0 && bestR > 0)
        kotlin.math.abs(bestL - bestR) / kotlin.math.max(bestL, bestR) * 100.0
    else null
    val stronger = when {
        bestL <= 0 || bestR <= 0 -> null
        bestL > bestR -> "Left"
        bestR > bestL -> "Right"
        else -> null
    }
    val anyData = bestL > 0 || bestR > 0 || bestB > 0

    if (!anyData) return

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "BEST PEAKS BY HAND",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.outline,
                    letterSpacing = 1.sp,
                    modifier = Modifier.weight(1f)
                )
                asymPct?.let { p ->
                    val tag = stronger?.let { "$it +%.0f%%".format(p) } ?: "%.0f%%".format(p)
                    Text(
                        "Asym $tag",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (p >= 10.0) Color(0xFFFF3B30)
                                else if (p >= 5.0) Color(0xFFFFB627)
                                else Color(0xFF34C759)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                HandPeakCell("LEFT", bestL, left.size, Color(0xFF34C759), Modifier.weight(1f))
                HandPeakCell("BOTH", bestB, both.size, Color(0xFFFF8B57), Modifier.weight(1f))
                HandPeakCell("RIGHT", bestR, right.size, Color(0xFFFF3B30), Modifier.weight(1f))
            }
            if (bestL > 0 && bestR > 0) {
                Spacer(Modifier.height(8.dp))
                AsymmetryBar(bestL, bestR)
            }
        }
    }
}

@Composable
private fun HandPeakCell(label: String, peak: Double, count: Int, tint: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = if (peak > 0) tint.copy(alpha = 0.14f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                label,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = tint,
                letterSpacing = 0.6.sp
            )
            Spacer(Modifier.height(2.dp))
            if (peak > 0) {
                Text(
                    "%.1f".format(peak),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text("kg · $count", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text("—", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline)
                Text("no data", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
private fun AsymmetryBar(left: Double, right: Double) {
    val total = left + right
    val leftFrac = if (total > 0) (left / total).toFloat() else 0.5f
    val leftColor = Color(0xFF34C759)
    val rightColor = Color(0xFFFF3B30)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "%.1f".format(left),
            fontSize = 10.sp,
            color = leftColor,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(36.dp)
        )
        Box(
            Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp))
        ) {
            Row(Modifier.fillMaxSize()) {
                Box(Modifier.weight(leftFrac.coerceAtLeast(0.001f)).fillMaxHeight().background(leftColor))
                Box(
                    Modifier.weight((1f - leftFrac).coerceAtLeast(0.001f))
                        .fillMaxHeight()
                        .background(rightColor)
                )
            }
        }
        Spacer(Modifier.width(6.dp))
        Text(
            "%.1f".format(right),
            fontSize = 10.sp,
            color = rightColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun EmptyHistoryCard() {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("No sessions yet", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(Modifier.height(4.dp))
            Text("Run any program from the Programs tab to record your first session.",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TrendCard(programName: String, sessions: List<SessionEntity>) {
    val sorted = sessions.sortedBy { it.startedAtMs }
    val peaks = sorted.map { it.peakKgOverall }
    val accent = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outlineVariant

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(programName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                    modifier = Modifier.weight(1f))
                Text("${sessions.size} sessions", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline)
            }
            Spacer(Modifier.height(8.dp))
            if (peaks.size >= 2) {
                Canvas(Modifier.fillMaxWidth().height(48.dp)) {
                    val w = size.width; val h = size.height
                    val maxV = peaks.max(); val minV = peaks.min()
                    val range = (maxV - minV).coerceAtLeast(0.1)
                    val path = Path()
                    peaks.forEachIndexed { i, v ->
                        val x = if (peaks.size == 1) w / 2f else i.toFloat() / (peaks.size - 1) * w
                        val y = h - ((v - minV) / range).toFloat() * h * 0.8f - h * 0.1f
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(path, accent, style = Stroke(width = 3f))
                    val lastX = w
                    val lastY = h - ((peaks.last() - minV) / range).toFloat() * h * 0.8f - h * 0.1f
                    drawCircle(accent, radius = 5f, center = Offset(lastX - 2f, lastY))
                }
            } else {
                Box(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(4.dp))
                    .background(outline.copy(alpha = 0.15f)))
            }
            Spacer(Modifier.height(4.dp))
            Row {
                Text("min %.1f".format(peaks.min()), fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.weight(1f))
                Text("max %.1f kg".format(peaks.max()), fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SessionCard(s: SessionEntity, isPr: Boolean, onClick: () -> Unit) {
    val dt = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(s.startedAtMs))
    val dur = (s.endedAtMs - s.startedAtMs) / 1000
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(s.programName, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                    modifier = Modifier.weight(1f))
                if (isPr) {
                    AssistChip(
                        onClick = {},
                        label = { Text("PR", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color(0xFFFFB627),
                            labelColor = Color.Black
                        )
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text("%.1f kg".format(s.peakKgOverall),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(4.dp))
            Text("$dt · ${dur}s · ${s.hand} · ${s.gripType}",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (s.criticalForceKg != null) {
                Spacer(Modifier.height(4.dp))
                Text("CF %.1f kg · W' %.0f kg·s".format(s.criticalForceKg, s.wPrimeKgSec ?: 0.0),
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

private fun computePRs(sessions: List<SessionEntity>): Set<Long> {
    val pr = mutableSetOf<Long>()
    sessions.groupBy { it.programName }.forEach { (_, list) ->
        val sorted = list.sortedBy { it.startedAtMs }
        var maxSoFar = 0.0
        sorted.forEach { s ->
            if (s.peakKgOverall > maxSoFar) {
                pr.add(s.id)
                maxSoFar = s.peakKgOverall
            }
        }
    }
    return pr
}

@Composable
fun SessionDetailScreen(vm: MainViewModel, session: SessionEntity, onClose: () -> Unit) {
    val peaks = vm.decodePeaks(session.repPeaksJson)
    val maxPeak = peaks.maxOrNull() ?: 1.0
    val cf = session.criticalForceKg ?: 0.0
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(16.dp)
        ) {
            Text(session.programName, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            val dt = DateFormat.getDateTimeInstance().format(Date(session.startedAtMs))
            Text(dt, color = MaterialTheme.colorScheme.outline, fontSize = 12.sp)

            Spacer(Modifier.height(20.dp))
            HRow("Hand", session.hand)
            HRow("Grip", session.gripType)
            HRow("MVC at start", "%.2f kg".format(session.mvcAtStart))
            HRow("Peak overall", "%.2f kg".format(session.peakKgOverall))
            HRow("Reps", peaks.size.toString())
            HRow("Duration", "%.1fs".format((session.endedAtMs - session.startedAtMs) / 1000.0))
            session.criticalForceKg?.let { HRow("Critical Force", "%.2f kg".format(it)) }
            session.wPrimeKgSec?.let { HRow("W'", "%.1f kg·s".format(it)) }

            if (peaks.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Text("Per-rep peaks", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                RepBars(peaks = peaks, cf = cf, maxPeak = maxPeak)
            }

            Spacer(Modifier.weight(1f))
            Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Close") }
        }
    }
}

@Composable
private fun HRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, color = MaterialTheme.colorScheme.outline, fontSize = 13.sp)
        Spacer(Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
private fun RepBars(peaks: List<Double>, cf: Double, maxPeak: Double) {
    val accent = MaterialTheme.colorScheme.primary
    val cfColor = MaterialTheme.colorScheme.secondary
    Canvas(Modifier.fillMaxWidth().height(160.dp)) {
        val w = size.width; val h = size.height
        if (peaks.isEmpty() || maxPeak <= 0) return@Canvas
        val barW = w / peaks.size
        peaks.forEachIndexed { i, v ->
            val barH = (v / maxPeak).toFloat() * h
            drawRect(
                color = accent,
                topLeft = androidx.compose.ui.geometry.Offset(i * barW, h - barH),
                size = androidx.compose.ui.geometry.Size(barW * 0.85f, barH.coerceAtLeast(2f))
            )
        }
        if (cf > 0) {
            val cfY = h - (cf / maxPeak).toFloat() * h
            drawLine(cfColor,
                Offset(0f, cfY), Offset(w, cfY), strokeWidth = 3f)
        }
    }
}
