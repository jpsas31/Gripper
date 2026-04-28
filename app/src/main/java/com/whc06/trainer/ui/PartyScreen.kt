package com.whc06.trainer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whc06.trainer.training.Hand

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartyScreen(vm: MainViewModel) {
    val players by vm.partyPlayers.collectAsState()
    val kg by vm.smoothedKg.collectAsState()
    val peak by vm.peakKg.collectAsState()
    val hand by vm.selectedHand.collectAsState()

    var showAdd by remember { mutableStateOf(false) }
    var pendingPlayerName by remember { mutableStateOf("") }
    var captureMenuFor by remember { mutableStateOf<String?>(null) }
    var confirmReset by remember { mutableStateOf(false) }

    val ranked = remember(players) {
        players.sortedByDescending { it.best() }
    }
    val maxBest = ranked.firstOrNull()?.best() ?: 0.0

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false; pendingPlayerName = "" },
            title = { Text("Add player") },
            text = {
                OutlinedTextField(
                    value = pendingPlayerName,
                    onValueChange = { pendingPlayerName = it.take(20) },
                    singleLine = true,
                    placeholder = { Text("Name") },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.partyAddPlayer(pendingPlayerName)
                    pendingPlayerName = ""
                    showAdd = false
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAdd = false; pendingPlayerName = "" }) { Text("Cancel") }
            }
        )
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Clear party?") },
            text = { Text("Removes all ${players.size} player(s).") },
            confirmButton = {
                TextButton(onClick = {
                    vm.partyResetAll()
                    confirmReset = false
                }) { Text("Clear", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } }
        )
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Party Mode", fontWeight = FontWeight.Bold, fontSize = 20.sp,
                modifier = Modifier.weight(1f))
            if (players.isNotEmpty()) {
                TextButton(onClick = { confirmReset = true }) {
                    Text("Reset", fontSize = 12.sp)
                }
            }
            FilledTonalButton(onClick = { showAdd = true }) {
                Icon(Icons.Outlined.PersonAdd, contentDescription = null,
                    modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add", fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(10.dp))

        // Capture controls
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "%.1f".format(kg),
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "kg",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Spacer(Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "PEAK",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.outline,
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "%.1f kg".format(peak),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                HandSegmentedSimple(hand) { vm.selectHand(it) }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { vm.resetPeak() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.RestartAlt, contentDescription = null,
                            modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Reset Peak")
                    }
                    Button(
                        onClick = { captureMenuFor = if (players.isNotEmpty()) "open" else null },
                        enabled = peak > 0 && players.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.Bolt, contentDescription = null,
                            modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Save Peak…")
                    }
                }
            }
        }

        captureMenuFor?.let {
            ModalBottomSheet(onDismissRequest = { captureMenuFor = null }) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()) {
                    Text(
                        "Save %.1f kg as %s for…".format(peak, hand.name.lowercase()),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    players.forEach { p ->
                        ListItem(
                            headlineContent = { Text(p.name) },
                            supportingContent = {
                                Text(
                                    "L %.1f · B %.1f · R %.1f".format(p.leftKg, p.bothKg, p.rightKg),
                                    fontSize = 11.sp
                                )
                            },
                            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        )
                        TextButton(
                            onClick = {
                                vm.partyCapturePeak(p.id, hand)
                                captureMenuFor = null
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) { Text("Save to ${p.name}") }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (players.isEmpty()) {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("No players yet", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Tap Add to enter friends. Then pick a hand, pull, and save the peak under any name.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Text(
                "LEADERBOARD · BEST PEAK",
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.outline,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(6.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(ranked) { i, p ->
                    PartyPlayerRow(
                        rank = i + 1,
                        player = p,
                        maxBest = maxBest,
                        onResetScores = { vm.partyResetPlayerScores(p.id) },
                        onRemove = { vm.partyRemovePlayer(p.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PartyPlayerRow(
    rank: Int,
    player: PartyPlayer,
    maxBest: Double,
    onResetScores: () -> Unit,
    onRemove: () -> Unit
) {
    val frac = if (maxBest > 0) (player.best() / maxBest).toFloat().coerceIn(0f, 1f) else 0f
    val rankColor = when (rank) {
        1 -> Color(0xFFFFB627)
        2 -> Color(0xFFB7BBC2)
        3 -> Color(0xFFCD7F32)
        else -> MaterialTheme.colorScheme.outline
    }
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(28.dp).clip(RoundedCornerShape(50))
                        .background(rankColor.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("$rank", fontWeight = FontWeight.Bold, fontSize = 13.sp,
                        color = rankColor)
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    player.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "%.1f kg".format(player.best()),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(6.dp))
            Box(
                Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Box(
                    Modifier.fillMaxWidth(frac).fillMaxHeight()
                        .background(rankColor)
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ScoreChip("L", player.leftKg, Color(0xFF34C759), Modifier.weight(1f))
                ScoreChip("B", player.bothKg, Color(0xFFFF8B57), Modifier.weight(1f))
                ScoreChip("R", player.rightKg, Color(0xFFFF3B30), Modifier.weight(1f))
            }
            Spacer(Modifier.height(4.dp))
            Row {
                TextButton(onClick = onResetScores) { Text("Reset scores", fontSize = 11.sp) }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onRemove) {
                    Icon(Icons.Outlined.Delete, contentDescription = null,
                        modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Remove", fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun ScoreChip(label: String, value: Double, tint: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = if (value > 0) tint.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            Modifier.padding(vertical = 6.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = tint)
            Spacer(Modifier.width(4.dp))
            Text(
                if (value > 0) "%.1f".format(value) else "—",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (value > 0) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.outline
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HandSegmentedSimple(selected: Hand, onSelect: (Hand) -> Unit) {
    val ordered = listOf(Hand.LEFT, Hand.BOTH, Hand.RIGHT)
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        ordered.forEachIndexed { i, h ->
            val tint = handTraceColor(h)
            SegmentedButton(
                selected = h == selected,
                onClick = { onSelect(h) },
                shape = SegmentedButtonDefaults.itemShape(index = i, count = ordered.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = tint.copy(alpha = 0.18f),
                    activeContentColor = tint,
                    activeBorderColor = tint
                ),
                label = {
                    Text(
                        when (h) { Hand.LEFT -> "Left"; Hand.RIGHT -> "Right"; Hand.BOTH -> "Both" },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            )
        }
    }
}
