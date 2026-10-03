package com.athleteapp.pro.data.sync.model

data class AthleteSyncPayload(
    val clientUuid: String = "",
    val athleteId: Long = 1,
    val syncTimestamp: Long = System.currentTimeMillis(),
    val clientName: String = "",
    val assignedWorkouts: List<SyncWorkoutSession> = emptyList(),
    val anthropometry: List<SyncAnthropometry> = emptyList()
)

data class SyncWorkoutSession(
    val date: String,
    val notes: String = "",
    val completed: Boolean = false,
    val isSelfWorkoutAllowed: Boolean = false,
    val exercises: List<SyncExercise> = emptyList()
)

data class SyncExercise(
    val exerciseId: Long,
    val name: String,
    val muscleGroup: String,
    val sets: List<SyncWorkoutSet> = emptyList()
)

data class SyncWorkoutSet(
    val setNumber: Int,
    val targetWeightKg: Double = 0.0,
    val targetReps: Int = 10,
    val actualWeightKg: Double = 0.0,
    val actualReps: Int = 10,
    val isCompleted: Boolean = false,
    val rpe: Double? = null
)

data class SyncAnthropometry(
    val date: String,
    val weightKg: Double,
    val chestCm: Double? = null,
    val waistCm: Double? = null,
    val hipsCm: Double? = null,
    val bicepsCm: Double? = null
)
