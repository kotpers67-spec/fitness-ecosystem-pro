package com.athleteapp.pro.domain.calculators

import com.athleteapp.pro.data.local.entities.AssignedExerciseEntity
import com.athleteapp.pro.data.local.entities.MyWorkoutSetEntity
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class NeuroAdaptiveEngineTest {

    private lateinit var engine: NeuroAdaptiveEngine

    @Before
    fun setUp() {
        engine = NeuroAdaptiveEngine()
    }

    @Test
    fun testEstimated1RM_validCalculations() {
        // 100kg x 10 reps @ RPE 10 (0 RIR) -> 100 * (1 + 10/30) = 133.3 kg
        val oneRmMax = engine.calculateEstimated1RM(100.0, 10, 10.0)
        assertEquals(133.3, oneRmMax, 0.1)

        // 100kg x 10 reps @ RPE 8 (2 RIR) -> Effective reps = 12 -> 100 * (1 + 12/30) = 140.0 kg
        val oneRmRpe8 = engine.calculateEstimated1RM(100.0, 10, 8.0)
        assertEquals(140.0, oneRmRpe8, 0.1)

        // Zero / negative edge cases
        assertEquals(0.0, engine.calculateEstimated1RM(0.0, 10, 8.0), 0.01)
        assertEquals(0.0, engine.calculateEstimated1RM(100.0, 0, 8.0), 0.01)
    }

    @Test
    fun testTonnageCalculation() {
        val sets = listOf(
            MyWorkoutSetEntity(
                id = 1, sessionId = 1, exerciseId = 1, exerciseName = "Жим",
                muscleGroup = "Грудь", exerciseOrder = 1, setNumber = 1,
                actualWeightKg = 80.0, actualReps = 10, isCompleted = true
            ),
            MyWorkoutSetEntity(
                id = 2, sessionId = 1, exerciseId = 1, exerciseName = "Жим",
                muscleGroup = "Грудь", exerciseOrder = 1, setNumber = 2,
                actualWeightKg = 85.0, actualReps = 8, isCompleted = true
            ),
            MyWorkoutSetEntity(
                id = 3, sessionId = 1, exerciseId = 1, exerciseName = "Жим",
                muscleGroup = "Грудь", exerciseOrder = 1, setNumber = 3,
                actualWeightKg = 90.0, actualReps = 6, isCompleted = false // Should not be counted
            )
        )

        // Tonnage = (80 * 10) + (85 * 8) = 800 + 680 = 1480.0 kg
        val tonnage = engine.calculateTonnage(sets)
        assertEquals(1480.0, tonnage, 0.1)
    }

    @Test
    fun testSessionReadiness_emptyIsFresh() {
        val readiness = engine.calculateSessionReadiness(emptyList())
        assertEquals(100, readiness.readinessPercent)
        assertEquals(0, readiness.fatiguePercent)
        assertEquals(FatigueLevel.FRESH, readiness.fatigueLevel)
        assertEquals(0.0, readiness.totalTonnageKg, 0.01)
    }

    @Test
    fun testSessionReadiness_accumulatedFatigue() {
        val completedSets = (1..6).map { i ->
            MyWorkoutSetEntity(
                id = i.toLong(), sessionId = 1, exerciseId = 1, exerciseName = "Приседания",
                muscleGroup = "Ноги", exerciseOrder = 1, setNumber = i,
                actualWeightKg = 120.0, actualReps = 8, isCompleted = true, rpe = 9.5
            )
        }

        val readiness = engine.calculateSessionReadiness(completedSets)
        assertTrue("Fatigue should be elevated after 6 heavy sets", readiness.fatiguePercent > 50)
        assertTrue("Readiness should drop", readiness.readinessPercent < 50)
        assertTrue(readiness.fatigueLevel == FatigueLevel.ELEVATED || readiness.fatigueLevel == FatigueLevel.EXHAUSTED)
    }

    @Test
    fun testSetRecommendation_progressiveOverloadOnEasySet() {
        val exercise = AssignedExerciseEntity(id = 1, name = "Жим штанги лежа", muscleGroup = "Грудь")
        val sets = listOf(
            MyWorkoutSetEntity(
                id = 1, sessionId = 1, exerciseId = 1, exerciseName = "Жим",
                muscleGroup = "Грудь", exerciseOrder = 1, setNumber = 1,
                targetWeightKg = 80.0, targetReps = 10,
                actualWeightKg = 80.0, actualReps = 10, isCompleted = true, rpe = 6.5
            )
        )

        val rec = engine.calculateSetRecommendation(sets, exercise, sessionFatigue = 15)
        assertTrue("Next weight should increase for easy set (RPE 6.5)", rec.recommendedWeightKg > 80.0)
        assertEquals(10, rec.recommendedReps)
        assertEquals(8.0, rec.targetRpe, 0.01)
    }

    @Test
    fun testSetRecommendation_deloadOnExtremeFatigue() {
        val exercise = AssignedExerciseEntity(id = 1, name = "Становая тяга", muscleGroup = "Спина")
        val sets = listOf(
            MyWorkoutSetEntity(
                id = 1, sessionId = 1, exerciseId = 1, exerciseName = "Становая тяга",
                muscleGroup = "Спина", exerciseOrder = 1, setNumber = 1,
                targetWeightKg = 150.0, targetReps = 5,
                actualWeightKg = 150.0, actualReps = 4, isCompleted = true, rpe = 10.0
            )
        )

        val rec = engine.calculateSetRecommendation(sets, exercise, sessionFatigue = 60)
        assertTrue("Next weight should deload when RPE is 10.0 and reps missed", rec.recommendedWeightKg < 150.0)
        assertTrue(rec.recommendationReason.contains("Предельное утомление") || rec.recommendationReason.contains("сброс"))
    }
}
