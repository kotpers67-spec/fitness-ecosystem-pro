package com.athleteapp.pro.domain.calculators

import com.athleteapp.pro.data.local.entities.AssignedExerciseEntity
import com.athleteapp.pro.data.local.entities.MyWorkoutSetEntity
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class NeuroAdaptiveStressTest {

    private lateinit var engine: NeuroAdaptiveEngine

    @Before
    fun setUp() {
        engine = NeuroAdaptiveEngine()
    }

    // ==========================================
    // 1. Boundary: 0 sets / Empty workout history
    // ==========================================

    @Test
    fun testEmptySets_tonnageAndReadiness() {
        val emptyList = emptyList<MyWorkoutSetEntity>()
        assertEquals(0.0, engine.calculateTonnage(emptyList), 0.0)

        val readiness = engine.calculateSessionReadiness(emptyList)
        assertEquals(100, readiness.readinessPercent)
        assertEquals(0, readiness.fatiguePercent)
        assertEquals(FatigueLevel.FRESH, readiness.fatigueLevel)
        assertEquals(0.0, readiness.totalTonnageKg, 0.0)
        assertEquals(0, readiness.completedSetsCount)
        assertEquals(0.0, readiness.averageRpe, 0.0)
    }

    @Test
    fun testEmptySets_recommendationFallback() {
        val emptyList = emptyList<MyWorkoutSetEntity>()
        val rec = engine.calculateSetRecommendation(emptyList)
        assertEquals(40.0, rec.recommendedWeightKg, 0.01)
        assertEquals(10, rec.recommendedReps)
        assertEquals(7.5, rec.targetRpe, 0.01)
        assertFalse("1RM must not be NaN", rec.estimated1RM.isNaN())
        assertTrue("1RM must be positive", rec.estimated1RM > 0.0)
    }

    // ==========================================
    // 2. Boundary: Extreme high reps & extreme heavy weights
    // ==========================================

    @Test
    fun testExtremeValues_highRepsAndHeavyWeights() {
        // 500 kg x 100 reps
        val e1rm500 = engine.calculateEstimated1RM(500.0, 100, 10.0)
        assertTrue("e1RM must be positive and finite", e1rm500 > 500.0 && !e1rm500.isInfinite() && !e1rm500.isNaN())

        val extremeSet = listOf(
            MyWorkoutSetEntity(
                id = 1, sessionId = 1, exerciseId = 1, exerciseName = "Deadlift",
                muscleGroup = "Back", exerciseOrder = 1, setNumber = 1,
                targetWeightKg = 500.0, targetReps = 100,
                actualWeightKg = 500.0, actualReps = 100, isCompleted = true, rpe = 10.0
            )
        )
        val tonnage = engine.calculateTonnage(extremeSet)
        assertEquals(50000.0, tonnage, 0.1)

        val readiness = engine.calculateSessionReadiness(extremeSet)
        assertEquals("Readiness must not be negative", 0, readiness.readinessPercent)
        assertEquals("Fatigue must cap at 100", 100, readiness.fatiguePercent)
        assertEquals(FatigueLevel.EXHAUSTED, readiness.fatigueLevel)
    }

    @Test
    fun testSuperExtremeValues_1000kgAnd1000reps() {
        val superE1rm = engine.calculateEstimated1RM(1000.0, 1000, 10.0)
        assertTrue("Super extreme 1RM must remain finite", !superE1rm.isInfinite() && !superE1rm.isNaN())

        val superSet = listOf(
            MyWorkoutSetEntity(
                id = 1, sessionId = 1, exerciseId = 1, exerciseName = "Squat",
                muscleGroup = "Legs", exerciseOrder = 1, setNumber = 1,
                targetWeightKg = 1000.0, targetReps = 1000,
                actualWeightKg = 1000.0, actualReps = 1000, isCompleted = true, rpe = 10.0
            )
        )
        val superTonnage = engine.calculateTonnage(superSet)
        assertEquals(1000000.0, superTonnage, 0.1)

        val readiness = engine.calculateSessionReadiness(superSet)
        assertEquals(100, readiness.fatiguePercent)
        assertEquals(0, readiness.readinessPercent)
    }

    // ==========================================
    // 3. Boundary: RPE Edge Cases (0, 10, null, negative, overflow)
    // ==========================================

    @Test
    fun testRpeBoundaries_zeroTenNullNegative() {
        // RPE = 0
        val e1rmRpe0 = engine.calculateEstimated1RM(100.0, 10, 0.0)
        assertTrue("e1RM with RPE 0 must be finite and positive", e1rmRpe0 > 0.0 && !e1rmRpe0.isNaN())

        // RPE = null
        val e1rmNull = engine.calculateEstimated1RM(100.0, 10, null)
        assertTrue("e1RM with null RPE must be finite and positive", e1rmNull > 0.0 && !e1rmNull.isNaN())

        // RPE = -5.0
        val e1rmNeg = engine.calculateEstimated1RM(100.0, 10, -5.0)
        assertTrue("e1RM with negative RPE must clamp RIR to 5.0 safely", e1rmNeg > 0.0 && !e1rmNeg.isNaN())

        // RPE = 20.0
        val e1rmOver = engine.calculateEstimated1RM(100.0, 10, 20.0)
        assertTrue("e1RM with RPE > 10 must clamp RIR to 0.0 safely", e1rmOver > 0.0 && !e1rmOver.isNaN())
    }

    @Test
    fun testSetRecommendation_zeroTargetReps_safeLowerBoundEnforced() {
        // Safe lower bound test: When targetReps = 0, engine enforces recReps >= 1 and positive e1RM
        val setWithZeroTargetReps = listOf(
            MyWorkoutSetEntity(
                id = 1, sessionId = 1, exerciseId = 1, exerciseName = "Press",
                muscleGroup = "Shoulders", exerciseOrder = 1, setNumber = 1,
                targetWeightKg = 60.0, targetReps = 0, // Target reps is 0!
                actualWeightKg = 60.0, actualReps = 10, isCompleted = true, rpe = 6.5
            )
        )
        val rec = engine.calculateSetRecommendation(setWithZeroTargetReps)
        assertEquals(1, rec.recommendedReps)
        assertTrue("e1RM must be positive when safe lower bound is enforced", rec.estimated1RM > 0.0)
    }

    @Test
    fun testSetRecommendation_unratedRpeMaintainsWeightWithoutOverload() {
        val baseSet = MyWorkoutSetEntity(
            id = 1, sessionId = 1, exerciseId = 1, exerciseName = "Squat",
            muscleGroup = "Legs", exerciseOrder = 1, setNumber = 1,
            targetWeightKg = 100.0, targetReps = 5,
            actualWeightKg = 100.0, actualReps = 5, isCompleted = true, rpe = null // Unrated
        )

        // RPE = null
        val recNull = engine.calculateSetRecommendation(listOf(baseSet))
        assertEquals(100.0, recNull.recommendedWeightKg, 0.0)
        assertEquals(5, recNull.recommendedReps)
        assertTrue(recNull.recommendationReason.contains("не указана"))

        // RPE = 0.0
        val recZero = engine.calculateSetRecommendation(listOf(baseSet.copy(rpe = 0.0)))
        assertEquals(100.0, recZero.recommendedWeightKg, 0.0)
        assertEquals(5, recZero.recommendedReps)

        // RPE = -1.0
        val recNeg = engine.calculateSetRecommendation(listOf(baseSet.copy(rpe = -1.0)))
        assertEquals(100.0, recNeg.recommendedWeightKg, 0.0)
        assertEquals(5, recNeg.recommendedReps)

        // RPE = 6.0 (valid in 1.0..7.0) -> MUST overload
        val recOverload = engine.calculateSetRecommendation(listOf(baseSet.copy(rpe = 6.0)))
        assertTrue("RPE in 1.0..7.0 must apply progressive overload", recOverload.recommendedWeightKg > 100.0)
    }

    @Test
    fun testSetRecommendation_zeroTargetRepsAcrossAllCases() {
        fun makeSet(rpe: Double?, actualReps: Int) = listOf(
            MyWorkoutSetEntity(
                id = 1, sessionId = 1, exerciseId = 1, exerciseName = "Deadlift",
                muscleGroup = "Back", exerciseOrder = 1, setNumber = 1,
                targetWeightKg = 120.0, targetReps = 0,
                actualWeightKg = 120.0, actualReps = actualReps, isCompleted = true, rpe = rpe
            )
        )

        // Unrated case
        val recUnrated = engine.calculateSetRecommendation(makeSet(null, 5))
        assertEquals(1, recUnrated.recommendedReps)

        // Case 1: RPE 6.5
        val recCase1 = engine.calculateSetRecommendation(makeSet(6.5, 5))
        assertEquals(1, recCase1.recommendedReps)

        // Case 2: RPE 8.0
        val recCase2 = engine.calculateSetRecommendation(makeSet(8.0, 5))
        assertEquals(1, recCase2.recommendedReps)

        // Case 3: RPE 9.0
        val recCase3 = engine.calculateSetRecommendation(makeSet(9.0, 5))
        assertEquals(1, recCase3.recommendedReps)

        // Case 4: RPE 10.0
        val recCase4 = engine.calculateSetRecommendation(makeSet(10.0, 3))
        assertEquals(1, recCase4.recommendedReps)
    }

    @Test
    fun testSetRecommendation_deloadOnFailure() {
        val failureSet = listOf(
            MyWorkoutSetEntity(
                id = 1, sessionId = 1, exerciseId = 1, exerciseName = "Bench Press",
                muscleGroup = "Chest", exerciseOrder = 1, setNumber = 1,
                targetWeightKg = 100.0, targetReps = 10,
                actualWeightKg = 100.0, actualReps = 10, isCompleted = true, rpe = 10.0
            )
        )
        val rec = engine.calculateSetRecommendation(failureSet, sessionFatigue = 50)
        assertTrue("Weight should deload on RPE 10.0", rec.recommendedWeightKg < 100.0)
        assertEquals(7.5, rec.targetRpe, 0.01)
    }

    // ==========================================
    // 4. Mathematical Soundness & Runaway Simulation
    // ==========================================

    @Test
    fun testMathematicalSoundness_zeroWeightAndReps() {
        assertEquals(0.0, engine.calculateEstimated1RM(0.0, 10, 8.0), 0.0)
        assertEquals(0.0, engine.calculateEstimated1RM(100.0, 0, 8.0), 0.0)
        assertEquals(0.0, engine.calculateEstimated1RM(0.0, 0, 8.0), 0.0)
    }

    @Test
    fun testProgressionRunawaySimulation() {
        var currentWeight = 80.0
        val targetReps = 8

        for (i in 1..10) {
            val sets = listOf(
                MyWorkoutSetEntity(
                    id = i.toLong(), sessionId = 1, exerciseId = 1, exerciseName = "Squat",
                    muscleGroup = "Legs", exerciseOrder = 1, setNumber = i,
                    targetWeightKg = currentWeight, targetReps = targetReps,
                    actualWeightKg = currentWeight, actualReps = targetReps,
                    isCompleted = true, rpe = 6.0
                )
            )
            val rec = engine.calculateSetRecommendation(sets, sessionFatigue = 20)
            assertTrue("Weight should progress", rec.recommendedWeightKg >= currentWeight)
            assertFalse("Weight must be finite", rec.recommendedWeightKg.isInfinite() || rec.recommendedWeightKg.isNaN())
            currentWeight = rec.recommendedWeightKg
        }
        assertTrue("Compounded weight should be realistic", currentWeight in 90.0..130.0)
    }

    @Test
    fun testUncompletedSetsCountedInTonnageSemantics() {
        val sets = listOf(
            MyWorkoutSetEntity(
                id = 1, sessionId = 1, exerciseId = 1, exerciseName = "Squat",
                muscleGroup = "Legs", exerciseOrder = 1, setNumber = 1,
                targetWeightKg = 100.0, targetReps = 10,
                actualWeightKg = 100.0, actualReps = 10,
                isCompleted = false // NOT completed!
            )
        )
        val tonnage = engine.calculateTonnage(sets)
        assertEquals("Athlete Pro tonnage must strictly be 0.0 for isCompleted=false", 0.0, tonnage, 0.0)
    }
}
