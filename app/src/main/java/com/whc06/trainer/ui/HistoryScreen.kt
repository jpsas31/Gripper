package com.whc06.trainer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("History", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        Spacer(Modifier.height(12.dp))
        if (sessions.isEmpty()) {
            EmptyHistoryCard()
            return@Column
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
