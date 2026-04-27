package com.whc06.trainer.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whc06.trainer.training.Program
import kotlinx.coroutines.flow.collectLatest

@Composable
fun SessionScreen(vm: MainViewModel, program: Program, onClose: () -> Unit) {
    val st by vm.sessionState.collectAsState()
    val kg by vm.smoothedKg.collectAsState()
    val mvc by vm.effectiveMvcKg.collectAsState()
    val targetPctMvc = st.targetPctMvc
    val targetKg = if (targetPctMvc != null && mvc > 0) mvc * targetPctMvc / 100.0 else null
    val zoneTol = vm.zoneTolerancePct.collectAsState().value

    Surface(modifier = Modifier.fillMaxSize()) {
        when {
            st.finished -> CelebrationLayer(peak = st.peakKgOverall, onClose = onClose)
            st.isPreRoll -> PreRollLayer(st = st)
            else -> ActiveLayer(
                vm = vm, st = st, program = program, kg = kg,
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
            color = MaterialTheme.colorScheme.outline,
            letterSpacing = 4.sp
        )
        Spacer(Modifier.height(24.dp))
        Box(
            Modifier
                .size(220.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "$secondsLeft",
                fontSize = 160.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
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
    targetKg: Double?,
    targetPctMvc: Int?,
    zoneTol: Int,
    onClose: () -> Unit
) {
    val snackbarHost = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        vm.repCompleteEvents.collectLatest { (idx, peak) ->
            snackbarHost.showSnackbar("Rep ${idx + 1}: %.1f kg".format(peak))
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHost) }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().padding(16.dp),
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
                        color = if (st.isWork) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline
                    )
                }
            }

            if (st.running) {
                val progress = if (st.phaseTotalMs > 0) (st.phaseElapsedMs.toFloat() / st.phaseTotalMs) else 0f
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50))
                )
            }

            ForceGauge(
                kg = kg,
                peakKg = st.peakKgInPhase,
                targetKg = targetKg,
                zoneTolerancePct = zoneTol,
                height = 240.dp
            )

            ForceChart(
                samples = vm.recentSamples.toList(),
                targetKg = targetKg,
                zoneTolerancePct = zoneTol,
                windowMs = (program.totalDurationMs.coerceIn(20_000L, 120_000L)),
                height = 90.dp
            )

            Spacer(Modifier.weight(1f))

            // Action bar — sticky bottom
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!st.running) {
                    Button(onClick = { vm.startSession() }, modifier = Modifier.weight(1f)) {
                        Text("Start")
                    }
                    OutlinedButton(onClick = onClose, modifier = Modifier.weight(1f)) {
                        Text("Close")
                    }
                } else {
                    if (st.paused) {
                        Button(onClick = { vm.resumeSession() }, modifier = Modifier.weight(1f)) {
                            Text("Resume")
                        }
                    } else {
                        OutlinedButton(onClick = { vm.pauseSession() }, modifier = Modifier.weight(1f)) {
                            Text("Pause")
                        }
                    }
                    OutlinedButton(onClick = { vm.skipPhase() }, modifier = Modifier.weight(1f)) {
                        Text("Skip")
                    }
                    Button(
                        onClick = { vm.stopSession() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Stop") }
                }
            }
        }
    }
}

@Composable
private fun CelebrationLayer(peak: Double, onClose: () -> Unit) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(600),
        label = "done"
    )
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedVisibility(visible = true, enter = fadeIn(tween(400)), exit = fadeOut()) {
            Icon(
                imageVector = Icons.Outlined.EmojiEvents,
                contentDescription = null,
                modifier = Modifier.size(96.dp).scale(scale),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(16.dp))
        Text("Session Complete", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            "Peak %.1f kg".format(peak),
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(40.dp))
        Button(onClick = onClose) { Text("Close") }
    }
}
