package com.whc06.trainer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whc06.trainer.ble.BleScanner
import com.whc06.trainer.data.SessionEntity
import com.whc06.trainer.training.Category
import com.whc06.trainer.training.GripType
import com.whc06.trainer.training.Hand
import com.whc06.trainer.training.Program
import com.whc06.trainer.training.RepPreset

enum class Tab(val label: String) {
    LIVE("Live"), PROGRAMS("Programs"), PRESETS("Presets"), HISTORY("History"), SETTINGS("Settings")
}

private val Category.icon
    get() = when (this) {
        Category.ASSESSMENT -> Icons.Outlined.Speed
        Category.WARMUP -> Icons.Outlined.LocalFireDepartment
        Category.TRAINING -> Icons.Outlined.FitnessCenter
        Category.RECOVERY -> Icons.Outlined.Spa
    }

@Composable
fun AppRoot(
    vm: MainViewModel,
    onPermissionsNeeded: () -> Unit,
    onRequestBluetoothEnable: () -> Unit = {}
) {
    var tab by remember { mutableStateOf(Tab.LIVE) }
    var runningProgram by remember { mutableStateOf<Program?>(null) }
    var detailSession by remember { mutableStateOf<SessionEntity?>(null) }
    var pendingCfProgram by remember { mutableStateOf<Program?>(null) }
    val scanState by vm.scanState.collectAsState()
    val sessionState by vm.sessionState.collectAsState()
    val lastLog by vm.lastLog.collectAsState()
    val cfTutorialSeen by vm.cfTutorialSeen.collectAsState()
    var showCfResult by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { onPermissionsNeeded() }

    LaunchedEffect(sessionState.finished, lastLog) {
        if (sessionState.finished && lastLog != null && lastLog!!.programId.contains("cf", ignoreCase = true)) {
            showCfResult = true
        }
    }

    if (showCfResult && lastLog != null) {
        CriticalForceResultScreen(log = lastLog!!, onClose = {
            showCfResult = false
            runningProgram = null
        })
        return
    }

    runningProgram?.let { p ->
        SessionScreen(vm = vm, program = p, onClose = { runningProgram = null })
        return
    }

    detailSession?.let { s ->
        SessionDetailScreen(vm = vm, session = s, onClose = { detailSession = null })
        return
    }

    pendingCfProgram?.let { p ->
        CfTutorialDialog(
            onProceed = {
                vm.markCfTutorialSeen()
                vm.selectProgram(p)
                runningProgram = p
                pendingCfProgram = null
            },
            onSkip = { pendingCfProgram = null }
        )
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = {},
                        label = { Text(t.label, fontSize = 11.sp) }
                    )
                }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            ConnectionStrip(scanState, vm.packetsPerSec.collectAsState().value) {
                when (scanState) {
                    BleScanner.State.SCANNING -> vm.stopScan()
                    BleScanner.State.BLUETOOTH_OFF -> onRequestBluetoothEnable()
                    else -> vm.startScan()
                }
            }
            when (tab) {
                Tab.LIVE -> LiveScreen(vm)
                Tab.PROGRAMS -> ProgramsScreen(vm) { p ->
                    val isCf = p.id.contains("cf", ignoreCase = true)
                    if (isCf && !cfTutorialSeen) {
                        pendingCfProgram = p
                    } else {
                        vm.selectProgram(p)
                        runningProgram = p
                    }
                }
                Tab.PRESETS -> PresetsScreen(vm) { preset ->
                    val program = preset.toProgram()
                    vm.selectProgram(program)
                    runningProgram = program
                }
                Tab.HISTORY -> HistoryScreen(vm) { s -> detailSession = s }
                Tab.SETTINGS -> SettingsScreen(vm)
            }
        }
    }
}

@Composable
private fun ConnectionStrip(state: BleScanner.State, hz: Double, onToggle: () -> Unit) {
    val (label, color) = when (state) {
        BleScanner.State.SCANNING -> "Scanning · %.1f Hz".format(hz) to MaterialTheme.colorScheme.primary
        BleScanner.State.IDLE -> "Idle" to MaterialTheme.colorScheme.outline
        BleScanner.State.NO_BLUETOOTH -> "No BLE" to MaterialTheme.colorScheme.error
        BleScanner.State.BLUETOOTH_OFF -> "Tap to enable Bluetooth" to MaterialTheme.colorScheme.error
    }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(10.dp).background(color, RoundedCornerShape(50)))
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.weight(1f))
        FilledTonalButton(onClick = onToggle) {
            Text(when (state) {
                BleScanner.State.SCANNING -> "Stop"
                BleScanner.State.BLUETOOTH_OFF -> "Enable"
                else -> "Scan"
            })
        }
    }
}

@Composable
fun LiveScreen(vm: MainViewModel) {
    val kg by vm.smoothedKg.collectAsState()
    val peak by vm.peakKg.collectAsState()
    val mvc by vm.effectiveMvcKg.collectAsState()
    val stable by vm.stable.collectAsState()
    val hand by vm.selectedHand.collectAsState()
    val grip by vm.gripType.collectAsState()
    val targetKg by vm.targetKg.collectAsState()
    val zoneTol by vm.zoneTolerancePct.collectAsState()
    val tiz by vm.timeInZoneMs.collectAsState()
    val scanState by vm.scanState.collectAsState()

    Column(
        Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HandSelector(hand) { vm.selectHand(it) }
            GripSelector(grip) { vm.selectGrip(it) }
            Spacer(Modifier.weight(1f))
            StableIndicator(stable)
        }

        if (scanState == BleScanner.State.SCANNING && mvc <= 0) {
            EmptyStateCard(
                title = "Set your MVC first",
                body = "Pull max effort → tap Save MVC."
            )
        }

        Box(
            Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            ForceGauge(
                kg = kg, peakKg = peak,
                targetKg = if (targetKg > 0) targetKg else null,
                zoneTolerancePct = zoneTol,
                height = 280.dp
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.offset(y = 20.dp)
            ) {
                Text(
                    "%.1f".format(kg),
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text("kg", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (mvc <= 0) {
            Button(
                onClick = { vm.saveCurrentPeakAsMvc() },
                enabled = peak > 0,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (peak > 0) "Save MVC (%.1f kg)".format(peak) else "Pull max effort to save MVC")
            }
        } else {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Peak %.1f kg".format(peak),
                            fontSize = 22.sp, fontWeight = FontWeight.Bold
                        )
                        Text(
                            "MVC (${hand.name.lowercase()}): %.1f kg".format(mvc),
                            fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary
                        )
                        if (targetKg > 0) {
                            Text(
                                "Zone %.1fs · target %.1f kg".format(tiz / 1000.0, targetKg),
                                fontSize = 11.sp, color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    FilledTonalButton(
                        onClick = { vm.saveCurrentPeakAsMvc() },
                        enabled = peak > 0
                    ) { Text("Update MVC") }
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { vm.tare() }, modifier = Modifier.weight(1f)) {
                Icon(
                    Icons.Outlined.RestartAlt,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Tare")
            }
            OutlinedButton(onClick = { vm.resetPeak() }, modifier = Modifier.weight(1f)) {
                Icon(
                    Icons.Outlined.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Reset Peak")
            }
        }
    }
}

@Composable
private fun StableIndicator(stable: Boolean) {
    val color = if (stable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (stable) "●" else "○",
            color = color,
            fontSize = 14.sp
        )
        Spacer(Modifier.width(4.dp))
        Text(
            if (stable) "Stable" else "Settling",
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun EmptyStateCard(title: String, body: String) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(body, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun HandSelector(selected: Hand, onSelect: (Hand) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val label = selected.name.lowercase().replaceFirstChar { it.uppercase() }
    Box {
        AssistChip(
            onClick = { expanded = true },
            label = { Text(label, fontSize = 12.sp) },
            trailingIcon = {
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Hand.entries.forEach { h ->
                DropdownMenuItem(
                    text = { Text(h.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    onClick = { onSelect(h); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun GripSelector(selected: GripType, onSelect: (GripType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        AssistChip(
            onClick = { expanded = true },
            label = { Text(selected.display, fontSize = 12.sp) },
            trailingIcon = {
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            GripType.entries.forEach { g ->
                DropdownMenuItem(
                    text = { Text(g.display) },
                    onClick = { onSelect(g); expanded = false }
                )
            }
        }
    }
}

@Composable
fun ProgramsScreen(vm: MainViewModel, onRun: (Program) -> Unit) {
    var filter by remember { mutableStateOf<Category?>(null) }
    val all = vm.programs
    val visible = filter?.let { f -> all.filter { it.category == f } } ?: all

    val tabs = buildList {
        add(null)
        addAll(Category.entries)
    }
    val selectedIndex = tabs.indexOf(filter).coerceAtLeast(0)

    Column(Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = selectedIndex,
            edgePadding = 8.dp
        ) {
            tabs.forEachIndexed { idx, c ->
                androidx.compose.material3.Tab(
                    selected = selectedIndex == idx,
                    onClick = { filter = c },
                    text = {
                        Text(
                            c?.display ?: "All",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    icon = c?.let {
                        {
                            Icon(
                                imageVector = it.icon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Color(it.tagColor)
                            )
                        }
                    }
                )
            }
        }
        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp)
        ) {
            items(visible, key = { it.id }) { p -> ProgramCard(p) { onRun(p) } }
        }
    }
}

@Composable
private fun ProgramCard(p: Program, onStart: () -> Unit) {
    val workReps = p.sets.sumOf { it.phases.count { ph -> ph.isWork } }
    val firstWork = p.sets.firstOrNull()?.phases?.firstOrNull { it.isWork }
    val firstRest = p.sets.firstOrNull()?.phases?.firstOrNull { !it.isWork && it.label.contains("Rest", true) }
    val workSec = firstWork?.let { it.durationMs / 1000 } ?: 0
    val restSec = firstRest?.let { it.durationMs / 1000 } ?: 0
    val targetPct = firstWork?.targetPctMvc
    val tagColor = Color(p.category.tagColor)

    ElevatedCard(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                Modifier.width(6.dp).fillMaxHeight().background(tagColor)
            )
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = p.category.icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = tagColor
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(p.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
                        modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(4.dp))
                Text(p.description, fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                val phaseSummary = buildString {
                    append("${p.sets.size}×$workReps reps")
                    if (workSec > 0) append(" · ${workSec}s")
                    if (restSec > 0) append("/${restSec}s")
                    targetPct?.let { append(" · ${it}% MVC") }
                    append(" · ~%dm".format(p.totalDurationMs / 60_000L))
                }
                Text(phaseSummary, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
fun SettingsScreen(vm: MainViewModel) {
    val mvc by vm.commonMvc.collectAsState()
    val stableOnly by vm.stableOnly.collectAsState()
    val tts by vm.ttsEnabled.collectAsState()
    val haptic by vm.hapticEnabled.collectAsState()
    val zoneTol by vm.zoneTolerancePct.collectAsState()
    val targetPct by vm.targetPct.collectAsState()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingsCard("MVC Reference") {
            ManualMvcRow("Bilateral", mvc.bilateralKg) { vm.setManualMvc(Hand.BOTH, it) }
            ManualMvcRow("Left", mvc.leftKg) { vm.setManualMvc(Hand.LEFT, it) }
            ManualMvcRow("Right", mvc.rightKg) { vm.setManualMvc(Hand.RIGHT, it) }
            Spacer(Modifier.height(4.dp))
            Text("source: ${mvc.source.name}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
        }

        SettingsCard("Coaching") {
            SwitchRow("Voice cues (TTS)", tts) { vm.setTtsEnabled(it) }
            SwitchRow("Haptic feedback", haptic) { vm.setHapticEnabled(it) }
        }

        SettingsCard("Live Target") {
            Text("Target: $targetPct% MVC", fontSize = 13.sp)
            Slider(
                value = targetPct.toFloat(),
                onValueChange = { vm.setTargetPct(it.toInt()) },
                valueRange = 10f..120f
            )
            Spacer(Modifier.height(4.dp))
            Text("Zone tolerance: ±$zoneTol%", fontSize = 13.sp)
            Slider(
                value = zoneTol.toFloat(),
                onValueChange = { vm.setZoneTolerance(it.toInt()) },
                valueRange = 1f..20f
            )
        }

        SettingsCard("Signal") {
            SwitchRow("Stable readings only", stableOnly) { vm.setStableOnly(it) }
        }

        SettingsCard("About") {
            Text("Gripper · POC", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text("Sample rate ~5-10 Hz limited by BLE adv interval. RFD measurements unreliable.",
                fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun ManualMvcRow(label: String, current: Double, onSet: (Double) -> Unit) {
    var editing by remember(current) { mutableStateOf("%.1f".format(current)) }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.width(80.dp), fontSize = 14.sp)
        OutlinedTextField(
            value = editing,
            onValueChange = { editing = it.filter { c -> c.isDigit() || c == '.' } },
            singleLine = true,
            modifier = Modifier.weight(1f),
            suffix = { Text("kg", fontSize = 12.sp) },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
            )
        )
        Spacer(Modifier.width(8.dp))
        TextButton(onClick = {
            editing.toDoubleOrNull()?.let { onSet(it) }
        }) { Text("Set") }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f), fontSize = 14.sp)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
