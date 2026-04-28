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
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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

enum class Tab(val label: String, val icon: ImageVector) {
    LIVE("Live", Icons.Outlined.Bolt),
    PROGRAMS("Train", Icons.Outlined.FitnessCenter),
    PARTY("Party", Icons.Outlined.Groups),
    HISTORY("History", Icons.Outlined.History),
    SETTINGS("Settings", Icons.Outlined.Settings)
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
    onRequestBluetoothEnable: () -> Unit = {},
    onOpenAppSettings: () -> Unit = {}
) {
    var tab by remember { mutableStateOf(Tab.LIVE) }
    var runningProgram by remember { mutableStateOf<Program?>(null) }
    var detailSession by remember { mutableStateOf<SessionEntity?>(null) }
    var pendingCfProgram by remember { mutableStateOf<Program?>(null) }
    val scanState by vm.scanState.collectAsState()
    val sessionState by vm.sessionState.collectAsState()
    val lastLog by vm.lastLog.collectAsState()
    val cfTutorialSeen by vm.cfTutorialSeen.collectAsState()
    val permsDenied by vm.permsDenied.collectAsState()
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
                        icon = {
                            Icon(
                                t.icon,
                                contentDescription = t.label,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                t.label,
                                fontSize = 11.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    )
                }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (permsDenied.isNotEmpty()) {
                PermissionBanner(
                    denied = permsDenied,
                    onOpenSettings = onOpenAppSettings,
                    onRetry = onPermissionsNeeded
                )
            }
            ConnectionStrip(scanState, vm.packetsPerSec.collectAsState().value) {
                when (scanState) {
                    BleScanner.State.SCANNING -> vm.stopScan()
                    BleScanner.State.BLUETOOTH_OFF -> onRequestBluetoothEnable()
                    else -> vm.startScan()
                }
            }
            when (tab) {
                Tab.LIVE -> LiveScreen(vm)
                Tab.PROGRAMS -> ProgramsScreen(
                    vm = vm,
                    onRun = { p ->
                        val isCf = p.id.contains("cf", ignoreCase = true)
                        if (isCf && !cfTutorialSeen) {
                            pendingCfProgram = p
                        } else {
                            vm.selectProgram(p)
                            runningProgram = p
                        }
                    },
                    onRunPreset = { preset ->
                        val program = preset.toProgram()
                        vm.selectProgram(program)
                        runningProgram = program
                    }
                )
                Tab.PARTY -> PartyScreen(vm)
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
    val collapsed = state == BleScanner.State.SCANNING && hz > 1.0
    if (collapsed) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(8.dp).background(color, RoundedCornerShape(50)))
            Spacer(Modifier.width(6.dp))
            Text(
                "%.1f Hz".format(hz),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onToggle, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                Text("Stop", fontSize = 11.sp)
            }
        }
    } else {
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
}

@Composable
private fun PermissionBanner(
    denied: Set<String>,
    onOpenSettings: () -> Unit,
    onRetry: () -> Unit
) {
    val labels = denied.joinToString(", ") { it.substringAfterLast('.') }
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.WarningAmber,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Permissions denied",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    labels,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            TextButton(onClick = onRetry) {
                Text("Retry", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer)
            }
            TextButton(onClick = onOpenSettings) {
                Text("Settings", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
}

@Composable
private fun FirstRunCoachMark() {
    val accent = MaterialTheme.colorScheme.primary
    Surface(
        color = accent.copy(alpha = 0.12f),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.Bolt,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "First time? Set your MVC",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = accent
                )
                Text(
                    "Pull as hard as you can for ~5s, then tap Save MVC below.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
    val hz by vm.packetsPerSec.collectAsState()

    val samples by vm.recentSamples.collectAsState()
    val avgKg10s = remember(samples) {
        if (samples.isEmpty()) 0.0
        else {
            val tMax = samples.last().first
            val window = samples.filter { it.first >= tMax - 10_000L }
            if (window.isEmpty()) 0.0 else window.sumOf { it.second } / window.size
        }
    }
    val targetPctMvc = remember(targetKg, mvc) {
        if (mvc > 0 && targetKg > 0) ((targetKg / mvc) * 100).toInt() else null
    }

    val units by vm.unitsKg.collectAsState()
    val unitLabel = if (units) "kg" else "lb"
    fun fmt(v: Double): String = "%.1f".format(if (units) v else v * 2.20462)
    val targetActive = targetKg > 0

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GripSelector(grip) { vm.selectGrip(it) }
            Spacer(Modifier.weight(1f))
            StableIndicator(stable, hz)
        }

        if (mvc <= 0) {
            FirstRunCoachMark()
        }

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        fmt(kg),
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.semantics {
                            contentDescription = "Current force, ${"%.1f".format(kg)} kilograms"
                        }
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        unitLabel,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
                Spacer(Modifier.height(4.dp))
                ForceChart(
                    samples = samples,
                    targetKg = if (targetActive) targetKg else null,
                    targetPctMvc = targetPctMvc,
                    zoneTolerancePct = zoneTol,
                    windowMs = 30_000L,
                    lineColor = handTraceColor(hand),
                    modifier = Modifier.fillMaxWidth().height(160.dp)
                )
            }
        }

        StatsGrid(
            peak = peak,
            avg = avgKg10s,
            tizSec = tiz / 1000.0,
            showZone = targetActive,
            unitLabel = unitLabel,
            convert = { v -> if (units) v else v * 2.20462 }
        )

        val cmvc by vm.commonMvc.collectAsState()
        MvcComparisonCard(
            mvc = cmvc,
            selectedHand = hand,
            currentPeak = peak,
            targetKg = if (targetActive) targetKg else null,
            grip = grip,
            unitLabel = unitLabel,
            convert = { v -> if (units) v else v * 2.20462 },
            onSelectHand = { vm.selectHand(it) },
            onSavePeak = { vm.saveCurrentPeakAsMvc() }
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { vm.tare() }, modifier = Modifier.weight(1f)) {
                Icon(
                    Icons.Outlined.RestartAlt,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Tare")
            }
            OutlinedIconButton(
                onClick = { vm.resetPeak() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Outlined.Refresh,
                    contentDescription = "Reset peak",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun StableIndicator(stable: Boolean, hz: Double) {
    val color = if (stable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    val label = when {
        hz <= 0.0 -> if (stable) "stable" else "settling"
        else -> "%.1f Hz · ".format(hz) + if (stable) "stable" else "noisy"
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(color, RoundedCornerShape(50)))
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun StatsGrid(
    peak: Double,
    avg: Double,
    tizSec: Double,
    showZone: Boolean = true,
    unitLabel: String = "kg",
    convert: (Double) -> Double = { it }
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCell("PEAK", "%.1f".format(convert(peak)), unitLabel, Modifier.weight(1f))
        StatCell("AVG (10s)", "%.1f".format(convert(avg)), unitLabel, Modifier.weight(1f))
        if (showZone) {
            StatCell("IN ZONE", "%.1f".format(tizSec), "s", Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatCell(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier) {
        Column(
            Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.Start
        ) {
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
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
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

internal fun handTraceColor(h: Hand): Color = when (h) {
    Hand.LEFT -> Color(0xFF34C759)
    Hand.RIGHT -> Color(0xFFFF3B30)
    Hand.BOTH -> Color(0xFFFF8B57)
}

@Composable
private fun MvcComparisonCard(
    mvc: com.whc06.trainer.training.CommonMvc,
    selectedHand: Hand,
    currentPeak: Double,
    targetKg: Double?,
    grip: GripType,
    unitLabel: String = "kg",
    convert: (Double) -> Double = { it },
    onSelectHand: (Hand) -> Unit,
    onSavePeak: () -> Unit
) {
    val left = mvc.leftKg
    val right = mvc.rightKg
    val both = mvc.bilateralKg
    val asymPct = if (left > 0 && right > 0) {
        kotlin.math.abs(left - right) / kotlin.math.max(left, right) * 100.0
    } else null
    val stronger = when {
        left <= 0 || right <= 0 -> null
        left > right -> Hand.LEFT
        right > left -> Hand.RIGHT
        else -> null
    }
    val anyMvc = (left > 0) || (right > 0) || (both > 0)
    val asymIcon = asymPct?.let { p ->
        when {
            p >= 10.0 -> "⚠⚠"
            p >= 5.0 -> "⚠"
            else -> "✓"
        }
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "MVC PER HAND",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.outline,
                        letterSpacing = 1.sp
                    )
                    Text(
                        "grip: ${grip.display}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                asymPct?.let { p ->
                    val tag = stronger?.let { "${it.name.lowercase().replaceFirstChar { c -> c.uppercase() }} +%.0f%%".format(p) } ?: "%.0f%%".format(p)
                    Text(
                        "$asymIcon Asym $tag",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (p >= 10.0) Color(0xFFFF3B30)
                                else if (p >= 5.0) Color(0xFFFFB627)
                                else Color(0xFF34C759)
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MvcCell("LEFT", left, Hand.LEFT, selectedHand, onSelectHand, unitLabel, convert, Modifier.weight(1f))
                MvcCell("BOTH", both, Hand.BOTH, selectedHand, onSelectHand, unitLabel, convert, Modifier.weight(1f))
                MvcCell("RIGHT", right, Hand.RIGHT, selectedHand, onSelectHand, unitLabel, convert, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    if (targetKg != null) {
                        Text(
                            "Target %.1f $unitLabel".format(convert(targetKg)),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    } else if (!anyMvc) {
                        Text(
                            "Pull max effort to set MVC ↓",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                FilledTonalButton(
                    onClick = onSavePeak,
                    enabled = currentPeak > 0
                ) {
                    Text(
                        if (anyMvc) "Update ${selectedHand.name.lowercase()}" else "Save MVC",
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MvcCell(
    label: String,
    value: Double,
    hand: Hand,
    selectedHand: Hand,
    onSelect: (Hand) -> Unit,
    unitLabel: String = "kg",
    convert: (Double) -> Double = { it },
    modifier: Modifier = Modifier
) {
    val tint = handTraceColor(hand)
    val isSelected = hand == selectedHand
    Surface(
        modifier = modifier.heightIn(min = 48.dp),
        onClick = { onSelect(hand) },
        color = if (isSelected) tint.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, tint) else null
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
            if (value > 0) {
                Text(
                    "%.1f".format(convert(value)),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    unitLabel,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    "—",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
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

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun HandSegmented(selected: Hand, modifier: Modifier = Modifier, onSelect: (Hand) -> Unit) {
    val ordered = listOf(Hand.LEFT, Hand.BOTH, Hand.RIGHT)
    SingleChoiceSegmentedButtonRow(modifier) {
        ordered.forEachIndexed { i, h ->
            val handColor = handTraceColor(h)
            SegmentedButton(
                selected = h == selected,
                onClick = { onSelect(h) },
                shape = SegmentedButtonDefaults.itemShape(index = i, count = ordered.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = handColor.copy(alpha = 0.18f),
                    activeContentColor = handColor,
                    activeBorderColor = handColor
                ),
                label = {
                    Text(
                        when (h) { Hand.LEFT -> "L"; Hand.RIGHT -> "R"; Hand.BOTH -> "B" },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            )
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

private enum class TrainSource { BUILT_IN, CUSTOM }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramsScreen(
    vm: MainViewModel,
    onRun: (Program) -> Unit,
    onRunPreset: (RepPreset) -> Unit
) {
    var source by remember { mutableStateOf(TrainSource.BUILT_IN) }

    Column(Modifier.fillMaxSize()) {
        SingleChoiceSegmentedButtonRow(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            SegmentedButton(
                selected = source == TrainSource.BUILT_IN,
                onClick = { source = TrainSource.BUILT_IN },
                shape = SegmentedButtonDefaults.itemShape(0, 2),
                label = { Text("Built-in", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
            )
            SegmentedButton(
                selected = source == TrainSource.CUSTOM,
                onClick = { source = TrainSource.CUSTOM },
                shape = SegmentedButtonDefaults.itemShape(1, 2),
                label = { Text("Custom", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
            )
        }
        when (source) {
            TrainSource.BUILT_IN -> BuiltInProgramsList(vm, onRun)
            TrainSource.CUSTOM -> CustomPresetsList(vm, onRunPreset)
        }
    }
}

@Composable
private fun CustomPresetsList(vm: MainViewModel, onRunPreset: (RepPreset) -> Unit) {
    PresetsScreen(vm = vm, onRunPreset = onRunPreset)
}

@Composable
private fun BuiltInProgramsList(vm: MainViewModel, onRun: (Program) -> Unit) {
    var filter by remember { mutableStateOf<Category?>(null) }
    val all = vm.programs
    val visible = filter?.let { f -> all.filter { it.category == f } } ?: all
    val sessions by vm.recentSessions.collectAsState()

    val quickStart = remember(all, sessions) {
        val byId = all.associateBy { it.id }
        val lastRun = sessions.firstOrNull()?.let { byId[it.programId] }
        listOfNotNull(
            byId["asmt_mvc"],
            byId["prg_warmup"],
            lastRun
        ).distinctBy { it.id }
    }

    val tabs = buildList {
        add(null)
        addAll(Category.entries)
    }
    val selectedIndex = tabs.indexOf(filter).coerceAtLeast(0)

    Column(Modifier.fillMaxSize()) {
        if (quickStart.isNotEmpty()) {
            Text(
                "QUICK START",
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.outline,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
            )
            androidx.compose.foundation.lazy.LazyRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
            ) {
                items(quickStart, key = { "qs_${it.id}" }) { p ->
                    QuickStartCard(p) { onRun(p) }
                }
            }
        }
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
private fun QuickStartCard(p: Program, onStart: () -> Unit) {
    val tagColor = Color(p.category.tagColor)
    ElevatedCard(
        onClick = onStart,
        modifier = Modifier.width(180.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = p.category.icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = tagColor
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    p.category.display.uppercase(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = tagColor,
                    letterSpacing = 1.sp
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                p.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                maxLines = 2
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "~%dm".format(p.totalDurationMs / 60_000L),
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.outline
            )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel) {
    val mvc by vm.commonMvc.collectAsState()
    val stableOnly by vm.stableOnly.collectAsState()
    val tts by vm.ttsEnabled.collectAsState()
    val haptic by vm.hapticEnabled.collectAsState()
    val zoneTol by vm.zoneTolerancePct.collectAsState()
    val targetPct by vm.targetPct.collectAsState()
    val unitsKg by vm.unitsKg.collectAsState()
    val themeMode by vm.themeMode.collectAsState()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingsCard("Display") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Units", modifier = Modifier.weight(1f), fontSize = 14.sp)
                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        selected = unitsKg,
                        onClick = { vm.setUnitsKg(true) },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                        label = { Text("kg", fontSize = 12.sp) }
                    )
                    SegmentedButton(
                        selected = !unitsKg,
                        onClick = { vm.setUnitsKg(false) },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                        label = { Text("lb", fontSize = 12.sp) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("Theme", fontSize = 14.sp)
            Spacer(Modifier.height(4.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                com.whc06.trainer.data.ThemeMode.entries.forEachIndexed { i, m ->
                    SegmentedButton(
                        selected = themeMode == m,
                        onClick = { vm.setThemeMode(m) },
                        shape = SegmentedButtonDefaults.itemShape(i, com.whc06.trainer.data.ThemeMode.entries.size),
                        label = { Text(m.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 12.sp) }
                    )
                }
            }
        }

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
                valueRange = 10f..120f,
                steps = 21
            )
            Spacer(Modifier.height(4.dp))
            Text("Zone tolerance: ±$zoneTol%", fontSize = 13.sp)
            Slider(
                value = zoneTol.toFloat(),
                onValueChange = { vm.setZoneTolerance(it.toInt()) },
                valueRange = 1f..20f,
                steps = 18
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
