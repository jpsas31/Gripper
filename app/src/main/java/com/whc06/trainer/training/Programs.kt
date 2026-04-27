package com.whc06.trainer.training

enum class GripType(val display: String) {
    HALF_CRIMP("Half Crimp"),
    OPEN_HAND("Open Hand"),
    THREE_FINGER_DRAG("Three Finger Drag"),
    PINCH("Pinch"),
    DEADLIFT("Deadlift"),
    ROLLING_THUNDER("Rolling Thunder"),
    LEG_EXTENSION("Leg Extension"),
    OTHER("Other")
}

enum class ContractionType { ACTIVE, PASSIVE }
enum class Hand { LEFT, RIGHT, BOTH }

enum class Audience { CLIMBING, ARM_WRESTLING, WEIGHT_TRAINING, MEASURE_STRENGTH }

enum class Category(val tagColor: Long, val display: String) {
    ASSESSMENT(0xFF5BC0EB, "Tests"),
    WARMUP(0xFFFFB627, "Warmup"),
    TRAINING(0xFFFF6B35, "Strength"),
    RECOVERY(0xFF40E0D0, "Recovery")
}

data class Phase(
    val label: String,
    val durationMs: Long,
    val targetPctMvc: Int? = null,
    val isWork: Boolean = true
)

data class TrainingSet(
    val phases: List<Phase>,
    val restAfterMs: Long
)

data class Program(
    val id: String,
    val name: String,
    val category: Category,
    val description: String,
    val audience: List<Audience>,
    val sets: List<TrainingSet>,
    val gripDefault: GripType = GripType.HALF_CRIMP,
    val contraction: ContractionType = ContractionType.ACTIVE,
    val notes: String = ""
) {
    val totalDurationMs: Long
        get() = sets.sumOf { s ->
            s.phases.sumOf { it.durationMs } + s.restAfterMs
        }
}

object ProgramLibrary {

    private fun work(label: String, secs: Int, pct: Int? = null) =
        Phase(label, secs * 1000L, pct, isWork = true)

    private fun rest(secs: Int) =
        Phase("Rest", secs * 1000L, null, isWork = false)

    val MVC_ASSESSMENT = Program(
        id = "asmt_mvc",
        name = "MVC Assessment",
        category = Category.ASSESSMENT,
        description = "Max voluntary contraction. 5s all-out pull, best of 3.",
        audience = Audience.entries.toList(),
        sets = List(3) { idx ->
            TrainingSet(
                phases = listOf(
                    work("Pull #${idx + 1}", 5, 100)
                ),
                restAfterMs = 120_000L
            )
        }
    )

    val PEAK_ASSESSMENT = Program(
        id = "asmt_peak",
        name = "Peak Force",
        category = Category.ASSESSMENT,
        description = "Single max-effort pull. 8s window, peak captured.",
        audience = Audience.entries.toList(),
        sets = listOf(
            TrainingSet(
                phases = listOf(
                    work("Pull", 8, 100)
                ),
                restAfterMs = 0L
            )
        )
    )

    val CRITICAL_FORCE = Program(
        id = "asmt_cf",
        name = "Critical Force (Giles)",
        category = Category.ASSESSMENT,
        description = "All-out test per Giles et al. (2010). 7s on / 3s off × 24 reps. CF = mean of last 6 pulls (outliers >1 SD excluded). W' = work above CF.",
        audience = listOf(Audience.CLIMBING, Audience.MEASURE_STRENGTH),
        sets = listOf(
            TrainingSet(
                phases = buildList {
                    repeat(24) {
                        add(work("Pull ${it + 1}/24", 7, 100))
                        if (it < 23) add(rest(3))
                    }
                },
                restAfterMs = 0L
            )
        ),
        notes = "Tindeq protocol. Plateau-based: pull all-out each rep until force flattens."
    )

    val AVG_PEAKS_MVC = Program(
        id = "asmt_avg_peaks",
        name = "Avg of Peaks MVC",
        category = Category.ASSESSMENT,
        description = "5 reps × 5s max pull, 3min rest. Score = average of peak forces. More reliable than single max.",
        audience = listOf(Audience.CLIMBING, Audience.MEASURE_STRENGTH),
        sets = List(5) { idx ->
            TrainingSet(
                phases = listOf(
                    work("Get Ready", 5),
                    work("Pull #${idx + 1}/5", 5, 100)
                ),
                restAfterMs = 180_000L
            )
        },
        notes = "Tindeq pattern. Average smooths single-rep variance from form/grip."
    )

    val RFD_TEST_2080 = Program(
        id = "asmt_rfd",
        name = "RFD Test (20-80%)",
        category = Category.ASSESSMENT,
        description = "Rate of Force Development. Pull as fast as possible to peak. 3 reps, 2min rest. RFD = slope from 20% to 80% peak.",
        audience = listOf(Audience.CLIMBING, Audience.MEASURE_STRENGTH),
        sets = List(3) { idx ->
            TrainingSet(
                phases = listOf(
                    work("Explosive Pull #${idx + 1}", 5, 100)
                ),
                restAfterMs = 120_000L
            )
        },
        notes = "WH-C06 sample rate ~5-10 Hz makes RFD unreliable. Use Tindeq for accurate RFD."
    )

    val ENDURANCE_TARGET_ZONE = Program(
        id = "asmt_endurance_zone",
        name = "Endurance — Target Zone",
        category = Category.ASSESSMENT,
        description = "Hold force in target zone (60% MVC ± 5%) until you exit zone for 3s. Time-in-zone tracked.",
        audience = listOf(Audience.CLIMBING, Audience.WEIGHT_TRAINING),
        sets = listOf(
            TrainingSet(
                phases = listOf(
                    work("Hold in Zone", 600, 60)
                ),
                restAfterMs = 0L
            )
        ),
        notes = "Tindeq endurance pattern. Zone tolerance configurable."
    )

    val COMPETITION_PEAK_LOAD = Program(
        id = "asmt_competition",
        name = "Competition Peak Load",
        category = Category.ASSESSMENT,
        description = "Formal MVC scoring. 3 attempts, best-of-3 wins. 3min rest. No partial credit — must hold ≥1s.",
        audience = listOf(Audience.CLIMBING, Audience.ARM_WRESTLING, Audience.MEASURE_STRENGTH),
        sets = List(3) { idx ->
            TrainingSet(
                phases = listOf(
                    work("Attempt ${idx + 1}/3", 6, 100)
                ),
                restAfterMs = 180_000L
            )
        },
        notes = "Tindeq Competition mode. For comparison/leaderboards."
    )

    val ENDURANCE_ASSESSMENT = Program(
        id = "asmt_endurance",
        name = "Endurance — Time to Failure",
        category = Category.ASSESSMENT,
        description = "Hold target % MVC until failure. Time-to-failure tracked.",
        audience = listOf(Audience.CLIMBING, Audience.WEIGHT_TRAINING),
        sets = listOf(
            TrainingSet(
                phases = listOf(
                    work("Hold to Failure", 600, 60)
                ),
                restAfterMs = 0L
            )
        )
    )

    val WARMUP = Program(
        id = "prg_warmup",
        name = "Progressive Warmup",
        category = Category.WARMUP,
        description = "5 progressive pulls at 40/50/60/70/80% MVC. 5s on, 30s rest.",
        audience = Audience.entries.toList(),
        sets = listOf(40, 50, 60, 70, 80).map { pct ->
            TrainingSet(
                phases = listOf(work("Pull @${pct}%", 5, pct)),
                restAfterMs = 30_000L
            )
        }
    )

    val WARMUP_PULSE_ACTIVATION = Program(
        id = "prg_warmup_pulse",
        name = "Pulse Activation",
        category = Category.WARMUP,
        description = "3 sets × 4 short pulses @ 50% MVC. 5s on / 5s off, 45s between sets. Quick blood-flow primer.",
        audience = Audience.entries.toList(),
        sets = List(3) {
            TrainingSet(
                phases = buildList {
                    repeat(4) { i ->
                        add(work("Pulse ${i + 1}/4", 5, 50))
                        if (i < 3) add(rest(5))
                    }
                },
                restAfterMs = 45_000L
            )
        },
        notes = "Power Company / standard crag warmup. Open-hand grip recommended."
    )

    val WARMUP_RECRUITMENT_RAMP = Program(
        id = "prg_warmup_recruit",
        name = "Recruitment Ramp",
        category = Category.WARMUP,
        description = "4 single pulls at 70/75/80/85% MVC. 5s on, 90s rest. CNS prep before max-effort work.",
        audience = listOf(Audience.CLIMBING, Audience.ARM_WRESTLING, Audience.WEIGHT_TRAINING),
        sets = listOf(70, 75, 80, 85).map { pct ->
            TrainingSet(
                phases = listOf(work("Pull @${pct}%", 5, pct)),
                restAfterMs = 90_000L
            )
        },
        notes = "Hörst / Tyler Nelson recruitment hangs. Use as final ramp before MaxHangs or limit work."
    )

    val WARMUP_REPEATER_LITE = Program(
        id = "prg_warmup_rep_lite",
        name = "Repeater Lite",
        category = Category.WARMUP,
        description = "2 sets × 6 reps. 7s on / 3s off @ 60% MVC. 2min rest between sets.",
        audience = listOf(Audience.CLIMBING),
        sets = List(2) {
            TrainingSet(
                phases = buildList {
                    repeat(6) { i ->
                        add(work("Pull ${i + 1}/6", 7, 60))
                        if (i < 5) add(rest(3))
                    }
                },
                restAfterMs = 120_000L
            )
        },
        notes = "Sub-max repeater pattern to prime forearm endurance system before main repeaters."
    )

    val WARMUP_SHORT_MAX_PULSES = Program(
        id = "prg_warmup_short_max",
        name = "Short Max Pulses",
        category = Category.WARMUP,
        description = "5 reps × 3s @ 75% MVC. 30s rest. Wakes up high-threshold motor units.",
        audience = listOf(Audience.CLIMBING, Audience.ARM_WRESTLING, Audience.MEASURE_STRENGTH),
        sets = List(5) { idx ->
            TrainingSet(
                phases = listOf(work("Pulse ${idx + 1}/5", 3, 75)),
                restAfterMs = 30_000L
            )
        },
        notes = "Short duration keeps fatigue minimal. Use right before max-effort assessments."
    )

    val WARMUP_TENDON_PRIMER = Program(
        id = "prg_warmup_tendon",
        name = "Tendon Primer",
        category = Category.WARMUP,
        description = "2 × 30s low-intensity holds @ 45% MVC. 60s rest. Long isometric tissue prep.",
        audience = listOf(Audience.CLIMBING),
        sets = List(2) { idx ->
            TrainingSet(
                phases = listOf(work("Hold ${idx + 1}/2", 30, 45)),
                restAfterMs = 60_000L
            )
        },
        notes = "Open-hand grip. Submax sustained loading to prep collagen / pulleys."
    )

    val MAX_FORCE = Program(
        id = "prg_max_force",
        name = "Max Force",
        category = Category.TRAINING,
        description = "5s × 5 reps × 4 sets at 90% MVC. Strength focus.",
        audience = listOf(Audience.CLIMBING, Audience.WEIGHT_TRAINING, Audience.ARM_WRESTLING),
        sets = List(4) {
            TrainingSet(
                phases = buildList {
                    repeat(5) { i ->
                        add(work("Pull ${i + 1}/5", 5, 90))
                        if (i < 4) add(rest(15))
                    }
                },
                restAfterMs = 180_000L
            )
        }
    )

    val REPEATERS = Program(
        id = "prg_repeaters",
        name = "Repeaters",
        category = Category.TRAINING,
        description = "7s on / 3s off × 6 reps × 4 sets at 80% MVC. Strength endurance.",
        audience = listOf(Audience.CLIMBING),
        sets = List(4) {
            TrainingSet(
                phases = buildList {
                    repeat(6) { i ->
                        add(work("Pull ${i + 1}/6", 7, 80))
                        if (i < 5) add(rest(3))
                    }
                },
                restAfterMs = 180_000L
            )
        }
    )

    val ENDURANCE_TRAINING = Program(
        id = "prg_endurance",
        name = "Endurance Intervals",
        category = Category.TRAINING,
        description = "10s on / 5s off × 6 reps × 3 sets at 60% MVC.",
        audience = listOf(Audience.CLIMBING, Audience.WEIGHT_TRAINING),
        sets = List(3) {
            TrainingSet(
                phases = buildList {
                    repeat(6) { i ->
                        add(work("Pull ${i + 1}/6", 10, 60))
                        if (i < 5) add(rest(5))
                    }
                },
                restAfterMs = 120_000L
            )
        }
    )

    val ACTIVE_RECOVERY = Program(
        id = "prg_recovery",
        name = "Active Recovery",
        category = Category.RECOVERY,
        description = "30s holds × 3 reps at 30% MVC. Density / blood flow.",
        audience = Audience.entries.toList(),
        sets = listOf(
            TrainingSet(
                phases = buildList {
                    repeat(3) { i ->
                        add(work("Hold ${i + 1}/3", 30, 30))
                        if (i < 2) add(rest(60))
                    }
                },
                restAfterMs = 0L
            )
        )
    )

    val NO_HANG_MAX = Program(
        id = "prg_no_hang",
        name = "No-Hang Max",
        category = Category.TRAINING,
        description = "10s pull × 4 reps at 95% MVC. Tyler Nelson style.",
        audience = listOf(Audience.CLIMBING),
        sets = List(4) {
            TrainingSet(
                phases = listOf(work("Pull", 10, 95)),
                restAfterMs = 180_000L
            )
        }
    )

    val HORST_7_53 = Program(
        id = "prg_horst_7_53",
        name = "Hörst 7/53",
        category = Category.TRAINING,
        description = "3×7s hangs with 53s rest = 1 set. 4 sets total, 3min between sets. ~80-85% MVC.",
        audience = listOf(Audience.CLIMBING),
        sets = List(4) {
            TrainingSet(
                phases = buildList {
                    add(work("Pull 1/3", 7, 85))
                    add(rest(53))
                    add(work("Pull 2/3", 7, 85))
                    add(rest(53))
                    add(work("Pull 3/3", 7, 85))
                },
                restAfterMs = 180_000L
            )
        },
        notes = "Hörst protocol. Train weight = max 10s single hang. PCr resynthesis driven."
    )

    val LOPEZ_MAXHANGS_MAW = Program(
        id = "prg_lopez_maw",
        name = "Eva López MaxHangs (MAW)",
        category = Category.TRAINING,
        description = "10s max hang × 5 reps. 3min rest. ~100% MVC. Use 8-22mm edge.",
        audience = listOf(Audience.CLIMBING, Audience.MEASURE_STRENGTH),
        sets = List(5) {
            TrainingSet(
                phases = listOf(work("Max Hang", 10, 100)),
                restAfterMs = 180_000L
            )
        },
        notes = "Eva López MAW. Margin >3s → add load. Margin near 0 → drop load."
    )

    val LOPEZ_MAXHANGS_MED = Program(
        id = "prg_lopez_med",
        name = "Eva López MinEdge (MED)",
        category = Category.TRAINING,
        description = "10s hang on smallest edge you can hold ~12s. 5 reps. 3-5min rest.",
        audience = listOf(Audience.CLIMBING),
        sets = List(5) {
            TrainingSet(
                phases = listOf(work("Min Edge Hang", 10, 100)),
                restAfterMs = 240_000L
            )
        },
        notes = "Eva López MED. No added weight — vary edge size, not load."
    )

    val ANDERSON_REPEATERS = Program(
        id = "prg_anderson_rpm",
        name = "Anderson Repeaters (RPM)",
        category = Category.TRAINING,
        description = "7s on / 3s off × 7 reps × 4 sets. 3min rest between sets. 80% MVC. Rock Prodigy.",
        audience = listOf(Audience.CLIMBING),
        sets = List(4) {
            TrainingSet(
                phases = buildList {
                    repeat(7) { i ->
                        add(work("Pull ${i + 1}/7", 7, 80))
                        if (i < 6) add(rest(3))
                    }
                },
                restAfterMs = 180_000L
            )
        },
        notes = "Anderson Brothers RPM. Multi-grip recommended (cycle grips per session)."
    )

    val BECHTEL_3_6_9 = Program(
        id = "prg_bechtel_369",
        name = "Bechtel 3-6-9 Ladders",
        category = Category.TRAINING,
        description = "3s + 6s + 9s hangs (rest 10-60s between). 3 sets per grip. 92% MVC.",
        audience = listOf(Audience.CLIMBING),
        sets = List(3) {
            TrainingSet(
                phases = listOf(
                    work("Hang 3s", 3, 92),
                    rest(30),
                    work("Hang 6s", 6, 92),
                    rest(30),
                    work("Hang 9s", 9, 92)
                ),
                restAfterMs = 180_000L
            )
        },
        notes = "Steve Bechtel. Load = max 12s hang. 4-week cycle, 2 sessions/week."
    )

    val BECHTEL_3_6_9_12 = Program(
        id = "prg_bechtel_36912",
        name = "Bechtel 3-6-9-12 (Week 4)",
        category = Category.TRAINING,
        description = "Adds 12s hang. 3 sets per grip. 92% MVC. Used in week 4 of cycle.",
        audience = listOf(Audience.CLIMBING),
        sets = List(3) {
            TrainingSet(
                phases = listOf(
                    work("Hang 3s", 3, 92),
                    rest(30),
                    work("Hang 6s", 6, 92),
                    rest(30),
                    work("Hang 9s", 9, 92),
                    rest(45),
                    work("Hang 12s", 12, 92)
                ),
                restAfterMs = 180_000L
            )
        },
        notes = "Steve Bechtel — Week 4 progression."
    )

    val NELSON_DENSITY = Program(
        id = "prg_nelson_density",
        name = "Tyler Nelson Density",
        category = Category.TRAINING,
        description = "30s hang × 3 reps. 15s rest (2:1 work:rest). 95% RPE, near-failure.",
        audience = listOf(Audience.CLIMBING, Audience.WEIGHT_TRAINING),
        sets = listOf(
            TrainingSet(
                phases = buildList {
                    repeat(3) { i ->
                        add(work("Hold ${i + 1}/3", 30, 70))
                        if (i < 2) add(rest(15))
                    }
                },
                restAfterMs = 0L
            )
        ),
        notes = "Tyler Nelson. Lower-intensity, longer-duration. Tendon health focus."
    )

    val ABRAHAMSSON_NO_HANG = Program(
        id = "prg_abrahamsson",
        name = "Abrahamsson No-Hang (Daily)",
        category = Category.RECOVERY,
        description = "10s on / 50s off. 6 grip exercises. ~10min total. 2×/day, 6h apart.",
        audience = listOf(Audience.CLIMBING),
        sets = listOf(
            TrainingSet(
                phases = buildList {
                    listOf(
                        "4-finger crimp 14mm" to 75,
                        "4-finger crimp 14mm" to 75,
                        "4-finger crimp 14mm" to 75,
                        "3-finger drag" to 75,
                        "3-finger drag" to 75,
                        "3-finger drag" to 75,
                        "Mid 2-finger pocket" to 55,
                        "Front 2-finger pocket" to 55,
                        "Mid 2-finger crimp" to 35,
                        "Front 2-finger crimp" to 35
                    ).forEachIndexed { i, (label, pct) ->
                        add(work(label, 10, pct))
                        if (i < 9) add(rest(50))
                    }
                },
                restAfterMs = 0L
            )
        ),
        notes = "Emil Abrahamsson. Submax daily. Tendon adaptation via collagen synthesis."
    )

    val all: List<Program> = listOf(
        // Assessments
        MVC_ASSESSMENT,
        PEAK_ASSESSMENT,
        AVG_PEAKS_MVC,
        COMPETITION_PEAK_LOAD,
        CRITICAL_FORCE,
        RFD_TEST_2080,
        ENDURANCE_ASSESSMENT,
        ENDURANCE_TARGET_ZONE,
        // Warmup
        WARMUP,
        WARMUP_PULSE_ACTIVATION,
        WARMUP_RECRUITMENT_RAMP,
        WARMUP_REPEATER_LITE,
        WARMUP_SHORT_MAX_PULSES,
        WARMUP_TENDON_PRIMER,
        // Strength training
        MAX_FORCE,
        HORST_7_53,
        LOPEZ_MAXHANGS_MAW,
        LOPEZ_MAXHANGS_MED,
        BECHTEL_3_6_9,
        BECHTEL_3_6_9_12,
        NO_HANG_MAX,
        // Strength endurance
        REPEATERS,
        ANDERSON_REPEATERS,
        ENDURANCE_TRAINING,
        // Density / volume
        NELSON_DENSITY,
        // Recovery / daily
        ABRAHAMSSON_NO_HANG,
        ACTIVE_RECOVERY
    )

    fun byCategory(c: Category) = all.filter { it.category == c }
    fun byId(id: String) = all.firstOrNull { it.id == id }
}
