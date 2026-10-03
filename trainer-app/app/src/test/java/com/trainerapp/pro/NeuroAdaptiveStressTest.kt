package com.trainerapp.pro

import com.trainerapp.pro.data.local.entities.WorkoutSetEntity
import com.trainerapp.pro.domain.calculators.FatigueLevel
import com.trainerapp.pro.domain.calculators.NeuroAdaptiveEngine
import org.junit.Assert.*
import org.junit.Test

class NeuroAdaptiveStressTest {

    // ==========================================
    // 1. Boundary: 0 sets / Empty history
    // ==========================================

    @Test
    fun testEmptySets_tonnageAndVolume() {
        val emptyList = emptyList<WorkoutSetEntity>()
        assertEquals(0.0, NeuroAdaptiveEngine.calculateTonnage(emptyList), 0.0)
        assertEquals(0, NeuroAdaptiveEngine.calculateTotalReps(emptyList))
        assertEquals(0.0, NeuroAdaptiveEngine.calculateAverageIntensity(emptyList), 0.0)
        assertEquals(0.0, NeuroAdaptiveEngine.calculateFatigueIndex(emptyList), 0.0)
        assertEquals(FatigueLevel.LOW, NeuroAdaptiveEngine.getFatigueLevel(0.0))
    }

    @Test
    fun testEmptyHistory_readinessAndRecommendation() {
        val readiness = NeuroAdaptiveEngine.calculateReadinessScore(null, "2026-10-03")
        assertTrue("Null last session should yield high default readiness", readiness.score >= 85.0)
        assertFalse("Score should not be NaN", readiness.score.isNaN())

        val rec = NeuroAdaptiveEngine.calculateAdaptiveRecommendation(null, emptyList())
        assertEquals(20.0, rec.recommendedWeightKg, 0.01)
        assertEquals(10, rec.recommendedReps)
        assertFalse("1RM should not be NaN", rec.estimated1RM.isNaN())
        assertTrue("1RM should be positive", rec.estimated1RM > 0.0)
    }

    // ==========================================
    // 2. Boundary: Extreme high reps & extreme heavy weights
    // ==========================================

    @Test
    fun testExtremeValues_highRepsAndHeavyWeights() {
        // 500 kg x 100 reps
        val extremeSets = listOf(
            WorkoutSetEntity(
                sessionId = 1, exerciseId = 1, exerciseOrder = 1, setNumber = 1,
                weightKg = 500.0, reps = 100, isCompleted = true, rpe = 10.0
            )
        )
        val tonnage = NeuroAdaptiveEngine.calculateTonnage(extremeSets)
        assertEquals(50000.0, tonnage, 0.1)

        val fatigue = NeuroAdaptiveEngine.calculateFatigueIndex(extremeSets)
        assertEquals("Fatigue index must be capped at 100.0 under extreme strain", 100.0, fatigue, 0.01)
        assertEquals(FatigueLevel.OVERREACHING, NeuroAdaptiveEngine.getFatigueLevel(fatigue))

        val e1rm = NeuroAdaptiveEngine.calculateRpe1RM(500.0, 100, 10.0)
        assertTrue("1RM must be positive and finite", e1rm > 500.0 && !e1rm.isInfinite() && !e1rm.isNaN())

        // 1000 kg x 1000 reps
        val superExtremeSets = listOf(
            WorkoutSetEntity(
                sessionId = 1, exerciseId = 1, exerciseOrder = 1, setNumber = 1,
                weightKg = 1000.0, reps = 1000, isCompleted = true, rpe = 10.0
            )
        )
        val superTonnage = NeuroAdaptiveEngine.calculateTonnage(superExtremeSets)
        assertEquals(1000000.0, superTonnage, 0.1)

        val superFatigue = NeuroAdaptiveEngine.calculateFatigueIndex(superExtremeSets)
        assertEquals(100.0, superFatigue, 0.01)
    }

    // ==========================================
    // 3. Boundary: RPE Edge Cases (0, 10, null, negative, overflow)
    // ==========================================

    @Test
    fun testRpeBoundaries_zeroTenNull() {
        val setRpe0 = WorkoutSetEntity(
            sessionId = 1, exerciseId = 1, exerciseOrder = 1, setNumber = 1,
            weightKg = 100.0, reps = 10, isCompleted = true, rpe = 0.0
        )
        val fatigueRpe0 = NeuroAdaptiveEngine.calculateFatigueIndex(listOf(setRpe0))
        assertFalse("Fatigue must not be NaN", fatigueRpe0.isNaN())
        assertTrue("Fatigue must be non-negative", fatigueRpe0 >= 0.0)

        val recRpe0 = NeuroAdaptiveEngine.calculateAdaptiveRecommendation(setRpe0)
        // Safe RPE auto-regulation: RPE 0 is unrated/zero, maintains weight without progressive overload
        assertEquals("RPE 0 maintains current weight without overload", 100.0, recRpe0.recommendedWeightKg, 0.01)
        assertEquals("Adjustment percent for unrated RPE must be 0", 0.0, recRpe0.adjustmentPercent, 0.01)

        val setRpe10 = WorkoutSetEntity(
            sessionId = 1, exerciseId = 1, exerciseOrder = 1, setNumber = 1,
            weightKg = 100.0, reps = 10, isCompleted = true, rpe = 10.0
        )
        val fatigueRpe10 = NeuroAdaptiveEngine.calculateFatigueIndex(listOf(setRpe10))
        assertTrue("RPE 10 strain should be substantial", fatigueRpe10 > 10.0)

        val recRpe10 = NeuroAdaptiveEngine.calculateAdaptiveRecommendation(setRpe10)
        assertTrue("RPE 10 should trigger deload reduction", recRpe10.recommendedWeightKg < 100.0)
        assertEquals(-7.5, recRpe10.adjustmentPercent, 0.01)

        val setNullRpe = WorkoutSetEntity(
            sessionId = 1, exerciseId = 1, exerciseOrder = 1, setNumber = 1,
            weightKg = 100.0, reps = 10, isCompleted = true, rpe = null
        )
        val fatigueNull = NeuroAdaptiveEngine.calculateFatigueIndex(listOf(setNullRpe))
        assertTrue("Null RPE should default to 7.5 strain", fatigueNull > 0.0)
    }

    @Test
    fun testNegativeRpe_fatigueCalculationSafe() {
        val negativeRpeSet = WorkoutSetEntity(
            sessionId = 1, exerciseId = 1, exerciseOrder = 1, setNumber = 1,
            weightKg = 100.0, reps = 10, isCompleted = true, rpe = -1.0
        )
        // Sanitized RPE ensures (-1.0) is coerced to 1.0, preventing NaN and crash
        val fatigue = NeuroAdaptiveEngine.calculateFatigueIndex(listOf(negativeRpeSet))
        assertFalse("Fatigue must not be NaN", fatigue.isNaN())
        assertTrue("Fatigue must be non-negative", fatigue >= 0.0)
    }

    @Test
    fun testNanFatigueDampener_readinessScoreSafe() {
        // Clamping against NaN ensures readiness calculation does not crash
        val readiness = NeuroAdaptiveEngine.calculateReadinessScore("2026-10-01", "2026-10-03", Double.NaN)
        assertFalse("Readiness score must not be NaN", readiness.score.isNaN())
        assertTrue("Readiness score must be in valid range", readiness.score in 0.0..100.0)
    }

    @Test
    fun testExtremeRpe_overTen() {
        val extremeRpeSet = WorkoutSetEntity(
            sessionId = 1, exerciseId = 1, exerciseOrder = 1, setNumber = 1,
            weightKg = 100.0, reps = 10, isCompleted = true, rpe = 20.0
        )
        val fatigue = NeuroAdaptiveEngine.calculateFatigueIndex(listOf(extremeRpeSet))
        assertTrue("Fatigue must stay within 0..100 even with RPE 20", fatigue in 0.0..100.0)

        val e1rm = NeuroAdaptiveEngine.calculateRpe1RM(100.0, 10, 20.0)
        assertTrue("e1RM must remain non-negative and finite", e1rm >= 100.0 && !e1rm.isNaN())
    }

    // ==========================================
    // 4. Boundary: Rest intervals (short, optimal, long, malformed)
    // ==========================================

    @Test
    fun testRestIntervalDynamics() {
        // Same day (0 days)
        val sameDay = NeuroAdaptiveEngine.calculateReadinessScore("2026-10-03", "2026-10-03", 50.0)
        assertTrue("Same day should have low readiness score (<= 50)", sameDay.score <= 50.0)

        // 24 hours (1 day)
        val oneDay = NeuroAdaptiveEngine.calculateReadinessScore("2026-10-02", "2026-10-03", 50.0)
        assertTrue("1 day recovery should be moderate (45..75)", oneDay.score in 45.0..75.0)

        // 48 hours (2 days)
        val twoDays = NeuroAdaptiveEngine.calculateReadinessScore("2026-10-01", "2026-10-03", 50.0)
        assertTrue("48 hours should be supercompensation (>= 75)", twoDays.score >= 75.0)

        // 72 hours (3 days)
        val threeDays = NeuroAdaptiveEngine.calculateReadinessScore("2026-09-30", "2026-10-03", 50.0)
        assertTrue("72 hours should be high readiness (>= 85)", threeDays.score >= 85.0)

        // > 7 days (long rest)
        val thirtyDays = NeuroAdaptiveEngine.calculateReadinessScore("2026-09-01", "2026-10-03", 50.0)
        assertEquals(80.0, thirtyDays.score, 0.1)

        // Future session date (clock skew: last session is in future)
        val futureDate = NeuroAdaptiveEngine.calculateReadinessScore("2026-10-10", "2026-10-03", 50.0)
        assertFalse("Future date must not produce NaN", futureDate.score.isNaN())
        assertTrue("Future date clamped between 20..50", futureDate.score in 20.0..50.0)

        // Corrupted date string
        val corruptDate = NeuroAdaptiveEngine.calculateReadinessScore("invalid-date", "2026-10-03", 50.0)
        assertFalse("Corrupted date should fallback safely without NaN", corruptDate.score.isNaN())
    }

    // ==========================================
    // 5. Mathematical Soundness: Div by zero, NaN, Negative inputs
    // ==========================================

    @Test
    fun testMathematicalSoundness_zeroWeightAndReps() {
        val zeroSet = WorkoutSetEntity(
            sessionId = 1, exerciseId = 1, exerciseOrder = 1, setNumber = 1,
            weightKg = 0.0, reps = 0, isCompleted = true, rpe = 8.0
        )
        val intensity = NeuroAdaptiveEngine.calculateAverageIntensity(listOf(zeroSet))
        assertEquals("Intensity must be 0.0 when reps are 0", 0.0, intensity, 0.0)

        val e1rm = NeuroAdaptiveEngine.calculateRpe1RM(0.0, 0, 8.0)
        assertEquals("1RM must be 0.0 when weight/reps are 0", 0.0, e1rm, 0.0)

        val rec = NeuroAdaptiveEngine.calculateAdaptiveRecommendation(zeroSet, defaultWeight = 20.0, defaultReps = 10)
        assertTrue("Recommendation must fall back to default weight", rec.recommendedWeightKg > 0.0)
        assertTrue("Recommendation reps must fall back to default reps", rec.recommendedReps > 0)
    }

    @Test
    fun testProgressionRunawaySimulation() {
        var currentWeight = 100.0
        val reps = 10

        // Simulate 10 consecutive easy sets at RPE 6.5
        for (i in 1..10) {
            val set = WorkoutSetEntity(
                sessionId = 1, exerciseId = 1, exerciseOrder = 1, setNumber = i,
                weightKg = currentWeight, reps = reps, isCompleted = true, rpe = 6.5
            )
            val rec = NeuroAdaptiveEngine.calculateAdaptiveRecommendation(set)
            assertTrue("Each easy step should recommend >= weight", rec.recommendedWeightKg >= currentWeight)
            assertFalse("Weight must be finite", rec.recommendedWeightKg.isInfinite() || rec.recommendedWeightKg.isNaN())
            currentWeight = rec.recommendedWeightKg
        }
        // Starting at 100kg, after 10 * +5%, weight is approx 162.5kg
        assertTrue("10 progressive sets should yield realistic compounded weight", currentWeight in 150.0..180.0)
    }

    @Test
    fun testUncompletedSetsCountedInTonnageSemantics() {
        val sets = listOf(
            WorkoutSetEntity(
                sessionId = 1, exerciseId = 1, exerciseOrder = 1, setNumber = 1,
                weightKg = 100.0, reps = 10, isCompleted = false // NOT completed!
            )
        )
        val tonnage = NeuroAdaptiveEngine.calculateTonnage(sets)
        // Uncompleted sets must not be counted as completed volume
        assertEquals(0.0, tonnage, 0.01)
    }
}
