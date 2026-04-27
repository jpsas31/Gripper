package com.whc06.trainer.training

data class RepPreset(
    val id: String,
    val name: String,
    val workSec: Int,
    val restSec: Int,
    val repsPerSet: Int,
    val sets: Int,
    val restBetweenSetsSec: Int,
    val targetPctMvc: Int? = null,
    val gripType: GripType = GripType.HALF_CRIMP,
    val notes: String = ""
) {
    fun toProgram(category: Category = Category.TRAINING, audience: List<Audience> = listOf(Audience.CLIMBING)): Program {
        val phases = buildList {
            repeat(repsPerSet) { i ->
                add(Phase("Pull ${i + 1}/$repsPerSet", workSec * 1000L, targetPctMvc, isWork = true))
                if (i < repsPerSet - 1) add(Phase("Rest", restSec * 1000L, null, isWork = false))
            }
        }
        val trainingSets = List(sets) {
            TrainingSet(phases = phases, restAfterMs = restBetweenSetsSec * 1000L)
        }
        return Program(
            id = "preset_$id",
            name = name,
            category = category,
            description = "${sets}×${repsPerSet} reps · ${workSec}s on / ${restSec}s off · ${restBetweenSetsSec}s rest" +
                (targetPctMvc?.let { " · ${it}% MVC" } ?: ""),
            audience = audience,
            sets = trainingSets,
            gripDefault = gripType,
            notes = notes
        )
    }

    companion object {
        fun newId(): String = System.currentTimeMillis().toString(36)

        val SAMPLES = listOf(
            RepPreset(newId(), "Classic 7/3 Repeaters", 7, 3, 6, 4, 180, 80),
            RepPreset(newId(), "10/5 Endurance", 10, 5, 6, 3, 120, 60),
            RepPreset(newId(), "Density Holds", 30, 15, 3, 1, 0, 70),
            RepPreset(newId(), "5s × 5 Strength", 5, 60, 5, 4, 180, 90)
        )
    }
}
