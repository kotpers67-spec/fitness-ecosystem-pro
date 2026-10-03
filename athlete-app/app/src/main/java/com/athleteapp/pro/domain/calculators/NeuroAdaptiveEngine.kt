package com.athleteapp.pro.domain.calculators

import com.athleteapp.pro.data.local.entities.AssignedExerciseEntity
import com.athleteapp.pro.data.local.entities.MyWorkoutSetEntity
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class FatigueLevel(val titleRu: String, val titleEn: String) {
    FRESH("Минимальная (Свежесть)", "Fresh"),
    OPTIMAL("Оптимальная (Рабочая зона)", "Optimal"),
    ELEVATED("Повышенная (Усталость)", "Elevated"),
    EXHAUSTED("Предельная (Истощение)", "Exhausted")
}

data class NeuroRecommendation(
    val recommendedWeightKg: Double,
    val recommendedReps: Int,
    val targetRpe: Double,
    val fatigueLevel: FatigueLevel,
    val fatiguePercent: Int, // 0..100
    val readinessScore: Int, // 0..100
    val recommendationReason: String,
    val estimated1RM: Double = 0.0
)

data class SessionReadiness(
    val readinessPercent: Int, // 0..100
    val fatiguePercent: Int,   // 0..100
    val fatigueLevel: FatigueLevel,
    val totalTonnageKg: Double,
    val completedSetsCount: Int,
    val averageRpe: Double
)

class NeuroAdaptiveEngine {

    /**
     * Estimates 1-Repetition Maximum (1RM) using Epley formula adjusted for RPE (Reps in Reserve).
     * Effective reps = reps + (10.0 - RPE)
     */
    fun calculateEstimated1RM(weightKg: Double, reps: Int, rpe: Double? = null): Double {
        if (weightKg <= 0.0 || reps <= 0) return 0.0
        val effectiveRpe = rpe ?: 8.0
        val rir = (10.0 - effectiveRpe).coerceIn(0.0, 5.0)
        val effectiveReps = reps.toDouble() + rir
        val raw1RM = weightKg * (1.0 + (effectiveReps / 30.0))
        return ((raw1RM * 10.0).roundToInt() / 10.0)
    }

    /**
     * Calculates total session volume/tonnage in kilograms.
     */
    fun calculateTonnage(sets: List<MyWorkoutSetEntity>): Double {
        val total = sets.filter { it.isCompleted }.sumOf { it.actualWeightKg * it.actualReps }
        return ((total * 10.0).roundToInt() / 10.0)
    }

    /**
     * Evaluates current workout session readiness and neuromuscular fatigue.
     */
    fun calculateSessionReadiness(allSets: List<MyWorkoutSetEntity>): SessionReadiness {
        val completed = allSets.filter { it.isCompleted }
        if (completed.isEmpty()) {
            return SessionReadiness(
                readinessPercent = 100,
                fatiguePercent = 0,
                fatigueLevel = FatigueLevel.FRESH,
                totalTonnageKg = 0.0,
                completedSetsCount = 0,
                averageRpe = 0.0
            )
        }

        val totalTonnage = calculateTonnage(completed)
        val avgRpe = completed.map { it.rpe ?: 8.0 }.average()

        // Base fatigue accumulates ~5% per completed work set
        val setCountFatigue = completed.size * 5.2

        // High intensity / RPE penalty
        val rpePenalty = ((avgRpe - 6.5).coerceAtLeast(0.0) / 3.5) * 45.0

        // Tonnage scaling factor (every 1000kg adds slight systemic load)
        val tonnageLoad = (totalTonnage / 1500.0) * 10.0

        val computedFatigue = (setCountFatigue + rpePenalty + tonnageLoad).coerceIn(5.0, 100.0).roundToInt()
        val computedReadiness = (100 - computedFatigue).coerceIn(0, 100)

        val level = when {
            computedFatigue < 25 -> FatigueLevel.FRESH
            computedFatigue < 55 -> FatigueLevel.OPTIMAL
            computedFatigue < 80 -> FatigueLevel.ELEVATED
            else -> FatigueLevel.EXHAUSTED
        }

        return SessionReadiness(
            readinessPercent = computedReadiness,
            fatiguePercent = computedFatigue,
            fatigueLevel = level,
            totalTonnageKg = totalTonnage,
            completedSetsCount = completed.size,
            averageRpe = ((avgRpe * 10.0).roundToInt() / 10.0)
        )
    }

    /**
     * Produces smart neuro-adaptive target recommendations for the next set
     * based on previous completed sets, RPE auto-regulation, and session fatigue.
     */
    fun calculateSetRecommendation(
        exerciseSets: List<MyWorkoutSetEntity>,
        exercise: AssignedExerciseEntity? = null,
        sessionFatigue: Int = 0
    ): NeuroRecommendation {
        val completed = exerciseSets.filter { it.isCompleted }

        // Initial set of the exercise
        if (completed.isEmpty()) {
            val defaultTargetWeight = exerciseSets.firstOrNull()?.targetWeightKg?.takeIf { it > 0.0 } ?: 40.0
            val defaultTargetReps = maxOf(1, exerciseSets.firstOrNull()?.targetReps ?: 10)
            val est1RM = calculateEstimated1RM(defaultTargetWeight, defaultTargetReps, 7.5)

            return NeuroRecommendation(
                recommendedWeightKg = defaultTargetWeight,
                recommendedReps = defaultTargetReps,
                targetRpe = 7.5,
                fatigueLevel = if (sessionFatigue > 60) FatigueLevel.ELEVATED else FatigueLevel.FRESH,
                fatiguePercent = sessionFatigue,
                readinessScore = (100 - sessionFatigue).coerceIn(0, 100),
                recommendationReason = "Первый подход упражнения. Рекомендуется целевой вес $defaultTargetWeight кг (RPE 7.5).",
                estimated1RM = est1RM
            )
        }

        val lastSet = completed.last()
        val lastWeight = lastSet.actualWeightKg.takeIf { it > 0.0 } ?: lastSet.targetWeightKg.takeIf { it > 0.0 } ?: 40.0
        val lastReps = lastSet.actualReps.takeIf { it > 0 } ?: lastSet.targetReps
        val lastRpe = lastSet.rpe
        val targetReps = maxOf(1, lastSet.targetReps)

        val recWeight: Double
        val recReps: Int
        val targetRpe: Double
        val reason: String

        when {
            // Unrated / RPE < 1.0 / null -> Maintain weight without overload
            lastRpe == null || lastRpe < 1.0 -> {
                recWeight = lastWeight
                recReps = maxOf(1, targetReps)
                targetRpe = 8.0
                reason = "Оценка усилия (RPE) не указана. Сохраняйте текущий рабочий вес $recWeight кг без перегрузки."
            }

            // Case 1: Easy set (RPE in 1.0..7.0, RIR >= 3 reps) -> Progressive overload
            lastRpe in 1.0..7.0 -> {
                val step = roundToBarbellIncrement(max(1.25, lastWeight * 0.03))
                recWeight = lastWeight + step
                recReps = maxOf(1, targetReps)
                targetRpe = 8.0
                reason = "Высокий запас сил (RPE $lastRpe). Прогрессивная перегрузка: +$step кг к следующему сету."
            }

            // Case 2: Optimal hypertrophic zone (RPE 7.1..8.5, RIR 1.5..3)
            lastRpe <= 8.5 -> {
                if (lastReps >= targetReps && sessionFatigue < 60) {
                    val step = roundToBarbellIncrement(max(1.25, lastWeight * 0.015))
                    recWeight = lastWeight + step
                    recReps = maxOf(1, targetReps)
                    targetRpe = 8.0
                    reason = "Оптимальная рабочая зона (RPE $lastRpe). Небольшая прибавка +$step кг для максимизации стимула."
                } else {
                    recWeight = lastWeight
                    recReps = maxOf(1, targetReps)
                    targetRpe = 8.0
                    reason = "Идеальный рабочий стимул (RPE $lastRpe). Сохраняйте вес $recWeight кг."
                }
            }

            // Case 3: High fatigue / near failure (RPE 8.6..9.5, RIR 0.5..1)
            lastRpe <= 9.5 -> {
                recWeight = lastWeight
                recReps = maxOf(1, targetReps)
                targetRpe = 8.5
                reason = "Субмаксимальное усилие (RPE $lastRpe). Сохраните вес $recWeight кг, увеличьте время отдыха между сетами."
            }

            // Case 4: Complete failure or form breakdown (RPE >= 9.6 or missed reps by >= 2)
            else -> {
                val deload = roundToBarbellIncrement(max(2.5, lastWeight * 0.06))
                recWeight = max(15.0, lastWeight - deload)
                recReps = maxOf(1, targetReps)
                targetRpe = 7.5
                reason = "Предельное утомление (RPE $lastRpe). Авто-регулируемый сброс веса (-$deload кг) для чистой техники."
            }
        }

        // Clamp by global session fatigue if exhausted
        val finalWeight = if (sessionFatigue >= 75 && recWeight > lastWeight) {
            lastWeight
        } else {
            recWeight
        }

        val readiness = (100 - sessionFatigue).coerceIn(0, 100)
        val level = when {
            sessionFatigue < 25 -> FatigueLevel.FRESH
            sessionFatigue < 55 -> FatigueLevel.OPTIMAL
            sessionFatigue < 80 -> FatigueLevel.ELEVATED
            else -> FatigueLevel.EXHAUSTED
        }

        val est1RM = calculateEstimated1RM(finalWeight, recReps, targetRpe)

        return NeuroRecommendation(
            recommendedWeightKg = finalWeight,
            recommendedReps = recReps,
            targetRpe = targetRpe,
            fatigueLevel = level,
            fatiguePercent = sessionFatigue,
            readinessScore = readiness,
            recommendationReason = reason,
            estimated1RM = est1RM
        )
    }

    /**
     * Rounds weight to nearest 0.5 kg or 1.25 kg for realistic gym plate increments.
     */
    private fun roundToBarbellIncrement(weight: Double): Double {
        val stepped = (weight / 0.5).roundToInt() * 0.5
        return ((stepped * 10.0).roundToInt() / 10.0)
    }
}
