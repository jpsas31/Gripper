package com.whc06.trainer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whc06.trainer.training.GripType
import com.whc06.trainer.training.RepPreset

@Composable
fun PresetsScreen(vm: MainViewModel, onRunPreset: (RepPreset) -> Unit) {
    val presets by vm.presets.collectAsState()
    var editing by remember { mutableStateOf<RepPreset?>(null) }
    var showNew by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Rep Presets", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(Modifier.weight(1f))
            FilledTonalButton(onClick = { showNew = true }) { Text("New") }
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(presets, key = { it.id }) { p ->
                PresetCard(
                    p = p,
                    onRun = { onRunPreset(p) },
                    onEdit = { editing = p },
                    onDelete = { vm.deletePreset(p.id) }
                )
            }
        }
    }

    if (showNew) {
        PresetEditorDialog(
            initial = null,
            onDismiss = { showNew = false },
            onSave = {
                vm.savePreset(it)
                showNew = false
            }
        )
    }
    editing?.let { p ->
        PresetEditorDialog(
            initial = p,
            onDismiss = { editing = null },
            onSave = {
                vm.savePreset(it)
                editing = null
            }
        )
    }
}

@Composable
private fun PresetCard(
    p: RepPreset,
    onRun: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(onClick = onRun, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(p.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${p.sets}×${p.repsPerSet} · ${p.workSec}s/${p.restSec}s · rest ${p.restBetweenSetsSec}s" +
                        (p.targetPctMvc?.let { " · ${it}% MVC" } ?: ""),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "grip: ${p.gripType.display}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Edit") }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
        }
    }
}

@Composable
private fun PresetEditorDialog(
    initial: RepPreset?,
    onDismiss: () -> Unit,
    onSave: (RepPreset) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "My Preset") }
    var workSec by remember { mutableStateOf(initial?.workSec?.toString() ?: "7") }
    var restSec by remember { mutableStateOf(initial?.restSec?.toString() ?: "3") }
    var reps by remember { mutableStateOf(initial?.repsPerSet?.toString() ?: "6") }
    var sets by remember { mutableStateOf(initial?.sets?.toString() ?: "4") }
    var setRest by remember { mutableStateOf(initial?.restBetweenSetsSec?.toString() ?: "180") }
    var targetPct by remember { mutableStateOf(initial?.targetPctMvc?.toString() ?: "80") }
    var grip by remember { mutableStateOf(initial?.gripType ?: GripType.HALF_CRIMP) }
    var gripExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    RepPreset(
                        id = initial?.id ?: RepPreset.newId(),
                        name = name.ifBlank { "Preset" },
                        workSec = workSec.toIntOrNull() ?: 7,
                        restSec = restSec.toIntOrNull() ?: 3,
                        repsPerSet = reps.toIntOrNull() ?: 6,
                        sets = sets.toIntOrNull() ?: 4,
                        restBetweenSetsSec = setRest.toIntOrNull() ?: 180,
                        targetPctMvc = targetPct.toIntOrNull(),
                        gripType = grip,
                        notes = initial?.notes ?: ""
                    )
                )
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text(if (initial == null) "New Preset" else "Edit Preset") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = workSec, onValueChange = { workSec = it.filter { c -> c.isDigit() } }, label = { Text("Work s") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = restSec, onValueChange = { restSec = it.filter { c -> c.isDigit() } }, label = { Text("Rest s") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = reps, onValueChange = { reps = it.filter { c -> c.isDigit() } }, label = { Text("Reps/set") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = sets, onValueChange = { sets = it.filter { c -> c.isDigit() } }, label = { Text("Sets") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = setRest, onValueChange = { setRest = it.filter { c -> c.isDigit() } }, label = { Text("Set rest s") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = targetPct, onValueChange = { targetPct = it.filter { c -> c.isDigit() } }, label = { Text("% MVC") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Box {
                    OutlinedButton(onClick = { gripExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Grip: ${grip.display}")
                    }
                    DropdownMenu(expanded = gripExpanded, onDismissRequest = { gripExpanded = false }) {
                        GripType.entries.forEach { g ->
                            DropdownMenuItem(
                                text = { Text(g.display) },
                                onClick = { grip = g; gripExpanded = false }
                            )
                        }
                    }
                }
            }
        }
    )
}
