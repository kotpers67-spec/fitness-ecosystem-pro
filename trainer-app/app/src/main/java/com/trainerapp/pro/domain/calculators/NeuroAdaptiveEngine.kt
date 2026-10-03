package com.trainerapp.pro.domain.calculators

import com.trainerapp.pro.data.local.dao.SetHistoryItem
import com.trainerapp.pro.data.local.entities.WorkoutSetEntity
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.pow
import kotlin.math.roundToInt

enum class FatigueLevel(val title: String) {
    LOW("Низкая"),
    MODERATE("Оптимальная"),
    HIGH("Повышенная"),
    OVERREACHING("Критическая")
}

data class AdaptiveLoadRecommendation(
    val recommendedWeightKg: Double,
    val recommendedReps: Int,
    val targetRpe: Double,
    val estimated1RM: Double,
    val rationale: String,
    val adjustmentPercent: Double
)

data class SessionReadinessInfo(
    val score: Double, // 0..100
    val state: String,
    val recommendation: String
)

object NeuroAdaptiveEngine {

    /**
     * Вычисляет суммарный тоннаж тренировки (общий поднятый вес в кг).
     */
    fun calculateTonnage(sets: List<WorkoutSetEntity>): Double {
        val total = sets
            .filter { it.isCompleted && it.weightKg > 0 && it.reps > 0 }
            .sumOf { it.weightKg * it.reps }
        return (total * 10.0).roundToInt() / 10.0
    }

    /**
     * Вычисляет суммарное количество выполненных повторений.
     */
    fun calculateTotalReps(sets: List<WorkoutSetEntity>): Int {
        return sets
            .filter { it.isCompleted && it.reps > 0 }
            .sumOf { it.reps }
    }

    /**
     * Вычисляет среднюю интенсивность (средний вес на одно повторение).
     */
    fun calculateAverageIntensity(sets: List<WorkoutSetEntity>): Double {
        val completed = sets.filter { it.isCompleted && it.weightKg > 0 && it.reps > 0 }
        val reps = completed.sumOf { it.reps }
        if (reps == 0) return 0.0
        val tonnage = completed.sumOf { it.weightKg * it.reps }
        return ((tonnage / reps) * 10.0).roundToInt() / 10.0
    }

    /**
     * Вычисляет индекс накопленного нейромышечного утомления сессии (0.0 .. 100.0).
     * Использует взвешенное произведение тоннажа и RPE (Session RPE / Foster Method).
     */
    fun calculateFatigueIndex(sets: List<WorkoutSetEntity>): Double {
        val workingSets = sets.filter { it.isCompleted && it.weightKg > 0 && it.reps > 0 }
        if (workingSets.isEmpty()) return 0.0

        var rawStrain = 0.0
        for (set in workingSets) {
            val setVolume = set.weightKg * set.reps
            val rpe = (set.rpe ?: 7.5).coerceIn(1.0, 10.0)
            // Экспоненциальный фактор интенсивности для околопредельных сетов
            val intensityFactor = (rpe / 10.0).pow(1.6)
            rawStrain += setVolume * intensityFactor
        }

        // Нормализация к стандартной плотной тренировочной нагрузке (6000 кг*коэфф)
        val normalized = (rawStrain / 60.0).coerceIn(0.0, 100.0)
        val safeNormalized = if (normalized.isNaN()) 0.0 else normalized
        return (safeNormalized * 10.0).roundToInt() / 10.0
    }

    fun getFatigueLevel(fatigueIndex: Double): FatigueLevel {
        return when {
            fatigueIndex < 35.0 -> FatigueLevel.LOW
            fatigueIndex < 70.0 -> FatigueLevel.MODERATE
            fatigueIndex < 85.0 -> FatigueLevel.HIGH
            else -> FatigueLevel.OVERREACHING
        }
    }

    /**
     * Рассчитывает готовность ЦНС и мышц к нагрузке (0.0 .. 100.0)
     * на основе прошедших дней с прошлой сессии и утомления.
     */
    fun calculateReadinessScore(
        lastSessionDate: String?,
        currentDate: String,
        previousFatigueIndex: Double = 50.0
    ): SessionReadinessInfo {
        if (lastSessionDate == null) {
            return SessionReadinessInfo(
                score = 90.0,
                state = "Оптимальная готовность",
                recommendation = "Предыдущие тренировки не зафиксированы. Начните с базовой разминки."
            )
        }

        val daysBetween = try {
            val d1 = LocalDate.parse(lastSessionDate)
            val d2 = LocalDate.parse(currentDate)
            ChronoUnit.DAYS.between(d1, d2).coerceAtLeast(0)
        } catch (_: Exception) {
            2L
        }

        val safeFatigueIndex = if (previousFatigueIndex.isNaN()) 50.0 else previousFatigueIndex.coerceIn(0.0, 100.0)
        val fatigueDampener = (safeFatigueIndex / 100.0).coerceIn(0.1, 1.0)

        val score = when {
            daysBetween == 0L -> {
                // Вторая сессия за тот же день
                (45.0 - fatigueDampener * 15.0).coerceIn(20.0, 50.0)
            }
            daysBetween == 1L -> {
                // На следующий день
                (70.0 - fatigueDampener * 15.0).coerceIn(45.0, 75.0)
            }
            daysBetween == 2L -> {
                // 48 часов - окно суперкомпенсации
                (95.0 - fatigueDampener * 10.0).coerceIn(75.0, 98.0)
            }
            daysBetween == 3L -> {
                // 72 часа - отличная готовность
                (98.0 - fatigueDampener * 5.0).coerceIn(85.0, 100.0)
            }
            daysBetween in 4L..6L -> {
                // 4-6 дней - сохранение суперкомпенсации
                88.0
            }
            else -> {
                // Более недели отдыха - спад тонуса
                80.0
            }
        }

        val safeScore = if (score.isNaN()) 50.0 else score
        val roundedScore = (safeScore * 10.0).roundToInt() / 10.0

        val (state, rec) = when {
            roundedScore >= 85.0 -> Pair(
                "Пиковая готовность (Суперкомпенсация)",
                "ЦНС полностью восстановилась. Благоприятный день для прогрессии весов и личных рекордов."
            )
            roundedScore >= 70.0 -> Pair(
                "Рабочая готовность",
                "Оптимальный баланс восстановления. Работайте в запланированном рабочем диапазоне RPE 7.5-8.5."
            )
            roundedScore >= 50.0 -> Pair(
                "Умеренное утомление",
                "Неполное восстановление. Рекомендуется удержание весов и контроль техники без отказных сетов."
            )
            else -> Pair(
                "Высокое кумулятивное утомление",
                "Высокий риск перетренированности. Рекомендуется разгрузочная тренировка (Deload) или легкое кардио."
            )
        }

        return SessionReadinessInfo(roundedScore, state, rec)
    }

    /**
     * Нейро-адаптивная авторегуляция нагрузки:
     * рассчитывает рекомендуемый вес и повторения на следующий подход
     * на основе предшествующих подходов и RPE (Rating of Perceived Exertion).
     */
    fun calculateAdaptiveRecommendation(
        lastSet: WorkoutSetEntity?,
        history: List<SetHistoryItem> = emptyList(),
        defaultWeight: Double = 20.0,
        defaultReps: Int = 10
    ): AdaptiveLoadRecommendation {
        if (lastSet == null) {
            val recentHistorical = history.lastOrNull { it.isCompleted }
            val weight = recentHistorical?.weightKg ?: defaultWeight
            val reps = recentHistorical?.reps ?: defaultReps
            val e1rm = calculateRpe1RM(weight, reps, 8.0)

            return AdaptiveLoadRecommendation(
                recommendedWeightKg = weight,
                recommendedReps = reps,
                targetRpe = 7.5,
                estimated1RM = e1rm,
                rationale = "Первый подход сессии. Разминочно-активационный вес с запасом в 2-3 повтора.",
                adjustmentPercent = 0.0
            )
        }

        val actualWeight = if (lastSet.weightKg > 0) lastSet.weightKg else defaultWeight
        val actualReps = if (lastSet.reps > 0) lastSet.reps else defaultReps
        val rpe = lastSet.rpe

        // Safe RPE auto-regulation: If RPE is 0.0, null, or unrated (< 1.0), maintain weight without overload
        if (rpe == null || rpe < 1.0) {
            val e1rm = calculateRpe1RM(actualWeight, actualReps, 8.0)
            return AdaptiveLoadRecommendation(
                recommendedWeightKg = actualWeight,
                recommendedReps = actualReps,
                targetRpe = 8.0,
                estimated1RM = e1rm,
                rationale = "RPE не указан или не оценен. Сохраняем текущий рабочий вес без перегрузки.",
                adjustmentPercent = 0.0
            )
        }

        val e1rm = calculateRpe1RM(actualWeight, actualReps, rpe)

        return when {
            rpe in 1.0..7.0 -> {
                // RIR >= 3 (Легкий запас) -> прогрессия веса +2.5% .. +5.0%
                val adjustment = 0.05
                val newWeight = roundToStandardPlates(actualWeight * (1.0 + adjustment))
                AdaptiveLoadRecommendation(
                    recommendedWeightKg = newWeight,
                    recommendedReps = actualReps,
                    targetRpe = 8.0,
                    estimated1RM = e1rm,
                    rationale = "Высокий запас сил в предыдущем подходе (RPE $rpe, RIR ≥ 3). Рекомендуется повышение на +${(adjustment * 100).toInt()}% (+${(newWeight - actualWeight)} кг).",
                    adjustmentPercent = adjustment * 100
                )
            }
            rpe in 7.0..8.5 -> {
                // Оптимальный стимул (RIR 1-2) -> удержание веса или минимальный шаг +1.25..2.5 кг
                val newWeight = roundToStandardPlates(actualWeight + 1.25)
                AdaptiveLoadRecommendation(
                    recommendedWeightKg = newWeight,
                    recommendedReps = actualReps,
                    targetRpe = 8.5,
                    estimated1RM = e1rm,
                    rationale = "Целевая гипертрофическая зона (RPE $rpe, RIR 1-2). Оптимальный рабочий стимул, сохраняем вес или добавляем микро-шаг.",
                    adjustmentPercent = 2.0
                )
            }
            rpe in 8.5..9.5 -> {
                // Околоотказное утомление (RIR 0-1) -> сохраняем вес, уменьшаем повторы на 1
                AdaptiveLoadRecommendation(
                    recommendedWeightKg = actualWeight,
                    recommendedReps = (actualReps - 1).coerceAtLeast(1),
                    targetRpe = 9.0,
                    estimated1RM = e1rm,
                    rationale = "Высокое утомление ЦНС (RPE $rpe, RIR ≤ 1). Удерживайте вес, снизив повторения на 1 во избежание срыва техники.",
                    adjustmentPercent = 0.0
                )
            }
            else -> {
                // Мышечный отказ RPE 10.0 -> сброс веса на 5-10% (Drop/Deload)
                val adjustment = -0.075
                val newWeight = roundToStandardPlates(actualWeight * (1.0 + adjustment))
                AdaptiveLoadRecommendation(
                    recommendedWeightKg = newWeight,
                    recommendedReps = actualReps,
                    targetRpe = 8.0,
                    estimated1RM = e1rm,
                    rationale = "Достигнут предельный мышечный отказ (RPE $rpe). Рекомендуется снижение веса на 7.5% (-${actualWeight - newWeight} кг) для безопасного объема.",
                    adjustmentPercent = adjustment * 100
                )
            }
        }
    }

    /**
     * Оценка 1ПМ с учетом RPE (Mike Tuchscherer / Reactive Training Systems)
     */
    fun calculateRpe1RM(weightKg: Double, reps: Int, rpe: Double): Double {
        if (weightKg <= 0 || reps <= 0) return 0.0
        val rir = (10.0 - rpe).coerceAtLeast(0.0)
        val totalRepsToFailure = reps + rir
        val e1rm = weightKg * (1.0 + totalRepsToFailure / 30.0)
        return (e1rm * 10.0).roundToInt() / 10.0
    }

    /**
     * Округление веса до шага 0.5 кг или стандартных дисков
     */
    private fun roundToStandardPlates(weight: Double): Double {
        return (Math.round(weight * 2.0) / 2.0).coerceAtLeast(1.0)
    }
}
