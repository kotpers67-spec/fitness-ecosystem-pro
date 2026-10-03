package com.trainerapp.pro.domain.calculators

import kotlin.math.pow
import kotlin.math.roundToInt

object FitnessCalculators {

    data class OneRepMaxResult(
        val epley: Double,
        val brzycki: Double,
        val lombardi: Double,
        val average: Double,
        val percentages: Map<Int, Double>
    )

    fun calculate1RM(weightKg: Double, reps: Int): OneRepMaxResult {
        if (reps <= 0 || weightKg <= 0) {
            return OneRepMaxResult(0.0, 0.0, 0.0, 0.0, emptyMap())
        }
        if (reps == 1) {
            val percentages = mapOf(
                100 to weightKg, 95 to weightKg * 0.95, 90 to weightKg * 0.90,
                85 to weightKg * 0.85, 80 to weightKg * 0.80, 75 to weightKg * 0.75,
                70 to weightKg * 0.70, 65 to weightKg * 0.65, 60 to weightKg * 0.60
            )
            return OneRepMaxResult(weightKg, weightKg, weightKg, weightKg, percentages)
        }

        val epley = weightKg * (1.0 + reps / 30.0)
        val brzycki = if (reps < 37) weightKg * (36.0 / (37.0 - reps)) else epley
        val lombardi = weightKg * (reps.toDouble().pow(0.1))

        val avg = (epley + brzycki + lombardi) / 3.0

        val pctMap = linkedMapOf(
            100 to (avg * 10).roundToInt() / 10.0,
            95 to (avg * 0.95 * 10).roundToInt() / 10.0,
            90 to (avg * 0.90 * 10).roundToInt() / 10.0,
            85 to (avg * 0.85 * 10).roundToInt() / 10.0,
            80 to (avg * 0.80 * 10).roundToInt() / 10.0,
            75 to (avg * 0.75 * 10).roundToInt() / 10.0,
            70 to (avg * 0.70 * 10).roundToInt() / 10.0,
            65 to (avg * 0.65 * 10).roundToInt() / 10.0,
            60 to (avg * 0.60 * 10).roundToInt() / 10.0
        )

        return OneRepMaxResult(
            epley = (epley * 10).roundToInt() / 10.0,
            brzycki = (brzycki * 10).roundToInt() / 10.0,
            lombardi = (lombardi * 10).roundToInt() / 10.0,
            average = (avg * 10).roundToInt() / 10.0,
            percentages = pctMap
        )
    }

    data class PlateBreakdown(
        val targetWeight: Double,
        val barWeight: Double,
        val weightPerSide: Double,
        val platesPerSide: Map<Double, Int>,
        val actualTotalWeight: Double,
        val remainder: Double
    )

    fun calculatePlates(
        targetWeightKg: Double,
        barWeightKg: Double = 20.0,
        availablePlates: List<Double> = listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25)
    ): PlateBreakdown {
        if (targetWeightKg < barWeightKg) {
            return PlateBreakdown(targetWeightKg, barWeightKg, 0.0, emptyMap(), barWeightKg, 0.0)
        }

        var neededPerSide = (targetWeightKg - barWeightKg) / 2.0
        val platesMap = mutableMapOf<Double, Int>()

        for (plate in availablePlates.sortedDescending()) {
            val count = (neededPerSide / plate).toInt()
            if (count > 0) {
                platesMap[plate] = count
                neededPerSide -= count * plate
            }
        }

        val actualWeightPerSide = platesMap.entries.sumOf { it.key * it.value }
        val actualTotal = barWeightKg + (actualWeightPerSide * 2.0)

        return PlateBreakdown(
            targetWeight = targetWeightKg,
            barWeight = barWeightKg,
            weightPerSide = actualWeightPerSide,
            platesPerSide = platesMap,
            actualTotalWeight = actualTotal,
            remainder = targetWeightKg - actualTotal
        )
    }

    // Neuro-Adaptive Engine integration
    val neuroAdaptive: NeuroAdaptiveEngine = NeuroAdaptiveEngine

    fun calculateTonnage(sets: List<com.trainerapp.pro.data.local.entities.WorkoutSetEntity>): Double =
        NeuroAdaptiveEngine.calculateTonnage(sets)

    fun calculateFatigueIndex(sets: List<com.trainerapp.pro.data.local.entities.WorkoutSetEntity>): Double =
        NeuroAdaptiveEngine.calculateFatigueIndex(sets)

    fun calculateReadinessScore(
        lastSessionDate: String?,
        currentDate: String,
        previousFatigueIndex: Double = 50.0
    ): SessionReadinessInfo =
        NeuroAdaptiveEngine.calculateReadinessScore(lastSessionDate, currentDate, previousFatigueIndex)

    fun calculateAdaptiveRecommendation(
        lastSet: com.trainerapp.pro.data.local.entities.WorkoutSetEntity?,
        history: List<com.trainerapp.pro.data.local.dao.SetHistoryItem> = emptyList(),
        defaultWeight: Double = 20.0,
        defaultReps: Int = 10
    ): AdaptiveLoadRecommendation =
        NeuroAdaptiveEngine.calculateAdaptiveRecommendation(lastSet, history, defaultWeight, defaultReps)
}
