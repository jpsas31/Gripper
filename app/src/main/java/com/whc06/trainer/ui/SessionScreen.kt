package com.whc06.trainer.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whc06.trainer.training.Program
import com.whc06.trainer.training.RepRecord
import com.whc06.trainer.training.SessionLog

private val PhaseWork = Color(0xFFFF6B35)
private val PhaseRest = Color(0xFF34C759)
private val PhasePreRoll = Color(0xFF5BC0EB)

@Composable
fun SessionScreen(vm: MainViewModel, program: Program, onClose: () -> Unit) {
    val st by vm.sessionState.collectAsState()
    val kg by vm.smoothedKg.collectAsState()
    val mvc by vm.effectiveMvcKg.collectAsState()
    val hand by vm.selectedHand.collectAsState()
    val targetPctMvc = st.targetPctMvc
    val targetKg = if (targetPctMvc != null && mvc > 0) mvc * targetPctMvc / 100.0 else null
    val zoneTol = vm.zoneTolerancePct.collectAsState().value

    Surface(modifier = Modifier.fillMaxSize()) {
        when {
            st.finished -> CelebrationLayer(vm = vm, onClose = onClose)
            st.isPreRoll -> PreRollLayer(st = st)
            else -> ActiveLayer(
                vm = vm, st = st, program = program, kg = kg,
                hand = hand, mvc = mvc,
                targetKg = targetKg, targetPctMvc = targetPctMvc,
                zoneTol = zoneTol, onClose = onClose
            )
        }
    }
}

@Composable
private fun PreRollLayer(st: com.whc06.trainer.training.SessionState) {
    val secondsLeft = ((st.phaseTotalMs - st.phaseElapsedMs) / 1000.0).coerceAtLeast(0.0).let {
        kotlin.math.ceil(it).toInt().coerceAtLeast(1)
    }
    val scale by animateFloatAsState(
        targetValue = secondsLeft.toFloat(),
        animationSpec = tween(200),
        label = "preroll-scale"
    )
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "GET READY",
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            color = PhasePreRoll,
            letterSpacing = 4.sp
        )
        Spacer(Modifier.height(24.dp))
        Box(
            Modifier
                .size(220.dp)
                .clip(RoundedCornerShape(50))
                .background(PhasePreRoll.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "$secondsLeft",
                fontSize = 160.sp,
                fontWeight = FontWeight.Bold,
                color = PhasePreRoll,
                modifier = Modifier.scale(0.9f + (scale % 1f) * 0.2f)
            )
        }
        Spacer(Modifier.height(32.dp))
        Text(
            "Set ${st.setIndex + 1}/${st.totalSets} · rep ${st.workRepsCompleted + 1}/${st.totalWorkReps}",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        st.targetPctMvc?.let {
            Text("Target ${it}% MVC", fontSize = 14.sp,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ActiveLayer(
    vm: MainViewModel,
    st: com.whc06.trainer.training.SessionState,
    program: Program,
    kg: Double,
    hand: com.whc06.trainer.training.Hand,
    mvc: Double,
    targetKg: Double?,
    targetPctMvc: Int?,
    zoneTol: Int,
    onClose: () -> Unit
) {
    val phaseColor = if (!st.running) MaterialTheme.colorScheme.outline
        else if (st.isWork) PhaseWork else PhaseRest

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Phase header — big rep counter, small subline
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                val bigLabel = when {
                    !st.running -> program.name
                    st.isWork && st.totalWorkReps > 0 ->
                        "Rep ${st.workRepsCompleted + 1}/${st.totalWorkReps}"
                    st.phaseLabel.isNotEmpty() -> st.phaseLabel
                    else -> program.name
                }
                Text(
                    bigLabel,
                    fontSize = if (!st.running) 22.sp else 30.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                val parts = buildList {
                    add("Set ${st.setIndex + 1}/${st.totalSets}")
                    targetPctMvc?.let { add("${it}% MVC") }
                    if (st.running && !st.isWork && st.phaseLabel.isNotEmpty()) add(st.phaseLabel)
                }
                Text(
                    parts.joinToString(" · "),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (st.running) {
                val remainingSec = ((st.phaseTotalMs - st.phaseElapsedMs) / 1000.0).coerceAtLeast(0.0)
                Text(
                    "%.1fs".format(remainingSec),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = phaseColor
                )
            }
        }

        if (st.running) {
            val progress = if (st.phaseTotalMs > 0) (st.phaseElapsedMs.toFloat() / st.phaseTotalMs) else 0f
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                color = phaseColor,
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50))
            )
        }

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    "%.1f".format(kg),
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "kg",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                Spacer(Modifier.weight(1f))
                if (st.peakKgInPhase > 0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "PEAK",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.outline,
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "%.1f kg".format(st.peakKgInPhase),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }

        ForceChart(
            samples = vm.recentSamples.collectAsState().value,
            targetKg = targetKg,
            targetPctMvc = targetPctMvc,
            zoneTolerancePct = zoneTol,
            windowMs = (program.totalDurationMs.coerceIn(20_000L, 120_000L)),
            lineColor = handTraceColor(hand),
            modifier = Modifier.weight(1f).fillMaxWidth()
        )

        // Action bar — sticky bottom
        if (!st.running) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = { vm.startSession() }, modifier = Modifier.weight(1f)) {
                    Text("Start")
                }
                OutlinedButton(onClick = onClose, modifier = Modifier.weight(1f)) {
                    Text("Close")
                }
            }
        } else {
            if (st.paused) {
                Button(
                    onClick = { vm.resumeSession() },
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) { Text("Resume", fontWeight = FontWeight.SemiBold) }
            } else {
                OutlinedButton(
                    onClick = { vm.pauseSession() },
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) { Text("Pause", fontWeight = FontWeight.SemiBold) }
            }
            Spacer(Modifier.height(6.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { vm.skipPhase() },
                    modifier = Modifier.weight(1f).height(40.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text("Skip phase", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = { vm.stopSession() },
                    modifier = Modifier.weight(1f).height(40.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Stop", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun CelebrationLayer(vm: MainViewModel, onClose: () -> Unit) {
    val log by vm.lastLog.collectAsState()
    val st by vm.sessionState.collectAsState()
    val peak = log?.peakKgOverall ?: st.peakKgOverall
    val mvc = log?.mvcAtStart ?: 0.0

    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(600),
        label = "done"
    )
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(Modifier.height(24.dp))
        AnimatedVisibility(visible = true, enter = fadeIn(tween(400)), exit = fadeOut()) {
            Icon(
                imageVector = Icons.Outlined.EmojiEvents,
                contentDescription = null,
                modifier = Modifier.size(72.dp).scale(scale),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(8.dp))
        Text("Session Complete", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ResultStat("PEAK", "%.1f".format(peak), "kg", Modifier.weight(1f))
            if (mvc > 0) {
                val pct = (peak / mvc) * 100
                ResultStat("OF MVC", "%.0f".format(pct), "%", Modifier.weight(1f))
                ResultStat("MVC", "%.1f".format(mvc), "kg", Modifier.weight(1f))
            }
        }

        log?.takeIf { it.reps.isNotEmpty() }?.let { l ->
            Spacer(Modifier.height(20.dp))
            Text(
                "REP PEAKS",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.outline,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(Modifier.height(4.dp))
            RepHistogram(
                reps = l.reps,
                modifier = Modifier.fillMaxWidth().height(120.dp)
            )
        }

        Spacer(Modifier.weight(1f))
        Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Close") }
    }
}

@Composable
private fun ResultStat(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier) {
        Column(Modifier.padding(vertical = 12.dp, horizontal = 14.dp)) {
            Text(
                label,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.outline,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    value,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    unit,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun RepHistogram(reps: List<RepRecord>, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val maxKg = reps.maxOf { it.peakKg }.takeIf { it > 0 } ?: 1.0
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val n = reps.size
        if (n == 0) return@Canvas
        val gap = 2f
        val barW = ((w - gap * (n - 1)) / n).coerceAtLeast(1f)
        for (i in 0..3) {
            val y = h * i / 4f
            drawLine(grid, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
        }
        reps.forEachIndexed { i, rep ->
            val frac = (rep.peakKg / maxKg).coerceIn(0.0, 1.0).toFloat()
            val barH = h * frac
            val x = i * (barW + gap)
            drawRect(
                color = accent,
                topLeft = Offset(x, h - barH),
                size = Size(barW, barH)
            )
        }
    }
}
