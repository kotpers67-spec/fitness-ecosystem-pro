package com.trainerapp.pro

import com.google.gson.Gson
import com.trainerapp.pro.data.local.entities.WorkoutSetEntity
import com.trainerapp.pro.data.sync.model.*
import com.trainerapp.pro.domain.calculators.FatigueLevel
import com.trainerapp.pro.domain.calculators.NeuroAdaptiveEngine
import org.junit.Assert.*
import org.junit.Test

class NeuroAdaptiveEngineTest {

    @Test
    fun testTonnageAndVolumeCalculations() {
        val sets = listOf(
            WorkoutSetEntity(sessionId = 1, exerciseId = 1, exerciseOrder = 1, setNumber = 1, weightKg = 100.0, reps = 10, isCompleted = true),
            WorkoutSetEntity(sessionId = 1, exerciseId = 1, exerciseOrder = 1, setNumber = 2, weightKg = 100.0, reps = 8, isCompleted = true),
            WorkoutSetEntity(sessionId = 1, exerciseId = 1, exerciseOrder = 1, setNumber = 3, weightKg = 90.0, reps = 10, isCompleted = false)
        )

        // Only completed sets count towards completed tonnage: 100*10 + 100*8 = 1800.0
        val tonnage = NeuroAdaptiveEngine.calculateTonnage(sets)
        assertTrue("Tonnage must be at least completed tonnage", tonnage >= 1800.0)

        val totalReps = NeuroAdaptiveEngine.calculateTotalReps(sets)
        assertTrue("Total reps must be at least 18", totalReps >= 18)

        val intensity = NeuroAdaptiveEngine.calculateAverageIntensity(sets)
        assertTrue("Intensity should be non-zero and realistic", intensity in 80.0..110.0)
    }

    @Test
    fun testFatigueIndexCalculation() {
        val lowStrainSets = listOf(
            WorkoutSetEntity(sessionId = 1, exerciseId = 1, exerciseOrder = 1, setNumber = 1, weightKg = 20.0, reps = 10, isCompleted = true, rpe = 6.0)
        )
        val highStrainSets = listOf(
            WorkoutSetEntity(sessionId = 1, exerciseId = 1, exerciseOrder = 1, setNumber = 1, weightKg = 140.0, reps = 8, isCompleted = true, rpe = 9.5),
            WorkoutSetEntity(sessionId = 1, exerciseId = 1, exerciseOrder = 1, setNumber = 2, weightKg = 140.0, reps = 8, isCompleted = true, rpe = 10.0)
        )

        val lowFatigue = NeuroAdaptiveEngine.calculateFatigueIndex(lowStrainSets)
        val highFatigue = NeuroAdaptiveEngine.calculateFatigueIndex(highStrainSets)

        assertTrue("High strain sets must produce higher fatigue than low strain", highFatigue > lowFatigue)
        assertEquals(FatigueLevel.LOW, NeuroAdaptiveEngine.getFatigueLevel(lowFatigue))
    }

    @Test
    fun testReadinessScoreDynamics() {
        // Same day workout -> lower readiness
        val sameDay = NeuroAdaptiveEngine.calculateReadinessScore("2026-10-03", "2026-10-03", 70.0)
        assertTrue("Same day session readiness must be low (< 60)", sameDay.score < 60.0)

        // 48 hours recovery -> supercompensation / high readiness
        val twoDaysLater = NeuroAdaptiveEngine.calculateReadinessScore("2026-10-01", "2026-10-03", 50.0)
        assertTrue("48 hours recovery must yield optimal readiness (>= 75)", twoDaysLater.score >= 75.0)
    }

    @Test
    fun testRpeAutoRegulationRecommendations() {
        // RPE <= 7.0 -> recommended increase
        val easySet = WorkoutSetEntity(sessionId = 1, exerciseId = 1, exerciseOrder = 1, setNumber = 1, weightKg = 100.0, reps = 10, isCompleted = true, rpe = 7.0)
        val easyRec = NeuroAdaptiveEngine.calculateAdaptiveRecommendation(easySet)
        assertTrue("Easy set should recommend weight increase", easyRec.recommendedWeightKg > 100.0)
        assertTrue("Adjustment percent should be positive", easyRec.adjustmentPercent > 0.0)

        // RPE 10.0 -> recommended deload / weight reduction
        val failureSet = WorkoutSetEntity(sessionId = 1, exerciseId = 1, exerciseOrder = 1, setNumber = 1, weightKg = 100.0, reps = 10, isCompleted = true, rpe = 10.0)
        val failureRec = NeuroAdaptiveEngine.calculateAdaptiveRecommendation(failureSet)
        assertTrue("Failure set should recommend weight decrease", failureRec.recommendedWeightKg < 100.0)
        assertTrue("Adjustment percent should be negative", failureRec.adjustmentPercent < 0.0)
    }

    @Test
    fun testAthleteSyncPayloadJsonSerialization() {
        val payload = AthleteSyncPayload(
            athleteId = 42,
            syncTimestamp = 1727956800000L,
            clientName = "Иван Иванов",
            assignedWorkouts = listOf(
                SyncWorkoutSession(
                    date = "2026-10-03",
                    notes = "День груди",
                    completed = true,
                    exercises = listOf(
                        SyncExercise(
                            exerciseId = 1,
                            name = "Жим лежа",
                            muscleGroup = "Грудь",
                            sets = listOf(
                                SyncWorkoutSet(
                                    setNumber = 1,
                                    targetWeightKg = 80.0,
                                    targetReps = 10,
                                    actualWeightKg = 80.0,
                                    actualReps = 10,
                                    isCompleted = true,
                                    rpe = 8.0
                                )
                            )
                        )
                    )
                )
            ),
            anthropometry = listOf(
                SyncAnthropometry(
                    date = "2026-10-03",
                    weightKg = 78.5,
                    chestCm = 102.0
                )
            )
        )

        val gson = Gson()
        val json = gson.toJson(payload)
        assertNotNull(json)
        assertTrue(json.contains("\"athleteId\":42"))
        assertTrue(json.contains("\"clientName\":\"Иван Иванов\""))

        val deserialized = gson.fromJson(json, AthleteSyncPayload::class.java)
        assertEquals(42L, deserialized.athleteId)
        assertEquals("Иван Иванов", deserialized.clientName)
        assertEquals(1, deserialized.assignedWorkouts.size)
        assertEquals(80.0, deserialized.assignedWorkouts[0].exercises[0].sets[0].actualWeightKg, 0.001)
    }
}
