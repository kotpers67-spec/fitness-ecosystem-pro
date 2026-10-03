package com.trainerapp.pro.data.sync

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.trainerapp.pro.data.local.dao.TrainerDao
import com.trainerapp.pro.data.local.entities.*
import com.trainerapp.pro.data.sync.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64

data class GitHubSyncConfig(
    val owner: String = "kotpe",
    val repo: String = "trainer-pro-sync",
    val token: String = "",
    val branch: String = "main"
)

class GitHubSyncManager {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    /**
     * Создает строго изолированный срез данных (AthleteSyncPayload)
     * исключительно для указанного подопечного (без утечки данных других клиентов).
     */
    suspend fun buildAthletePayload(dao: TrainerDao, client: ClientEntity): AthleteSyncPayload = withContext(Dispatchers.IO) {
        val sessions = dao.getSessionsForClientSync(client.id)
        val anthropometry = dao.getAnthropometryForClientSync(client.id)

        val assignedWorkouts = sessions.map { session ->
            val sets = dao.getSetsForSessionSync(session.id)
            val exerciseIds = sets.map { it.exerciseId }.distinct()

            val exercises = exerciseIds.mapNotNull { exerciseId ->
                val exerciseEntity = dao.getExerciseById(exerciseId) ?: return@mapNotNull null
                val exerciseSets = sets.filter { it.exerciseId == exerciseId }.map { set ->
                    SyncWorkoutSet(
                        setNumber = set.setNumber,
                        targetWeightKg = set.weightKg,
                        targetReps = set.reps,
                        actualWeightKg = if (set.isCompleted) set.weightKg else 0.0,
                        actualReps = if (set.isCompleted) set.reps else 0,
                        isCompleted = set.isCompleted,
                        rpe = set.rpe
                    )
                }
                SyncExercise(
                    exerciseId = exerciseId,
                    name = exerciseEntity.name,
                    muscleGroup = exerciseEntity.muscleGroup,
                    sets = exerciseSets
                )
            }

            SyncWorkoutSession(
                date = session.date,
                notes = session.comments,
                completed = session.completed,
                isSelfWorkoutAllowed = session.isSelfWorkoutAllowed,
                exercises = exercises
            )
        }

        val syncAnthropometry = anthropometry.map {
            SyncAnthropometry(
                date = it.date,
                weightKg = it.weightKg,
                chestCm = it.chestCm,
                waistCm = it.waistCm,
                hipsCm = it.hipsCm,
                bicepsCm = it.bicepsCm
            )
        }

        AthleteSyncPayload(
            clientUuid = client.clientUuid,
            athleteId = client.id,
            pairingCode = client.pairingCode,
            syncTimestamp = System.currentTimeMillis(),
            clientName = client.fullName,
            avatarBase64 = client.avatarBase64,
            assignedWorkouts = assignedWorkouts,
            anthropometry = syncAnthropometry
        )
    }

    suspend fun exportAthletePayloadJson(dao: TrainerDao, client: ClientEntity): String = withContext(Dispatchers.IO) {
        val payload = buildAthletePayload(dao, client)
        gson.toJson(payload)
    }

    /**
     * Загружает персональный срез тренировок подопечного в GitHub.
     */
    suspend fun uploadAthleteData(
        config: GitHubSyncConfig,
        athleteId: Long,
        jsonData: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (config.token.isBlank()) {
            return@withContext Result.failure(IllegalStateException("GitHub Token not configured"))
        }

        try {
            val path = "athletes/athlete_${athleteId}.json"
            val apiUrl = "https://api.github.com/repos/${config.owner}/${config.repo}/contents/$path"

            // 1. Check if file already exists to get SHA
            val existingSha = getFileSha(apiUrl, config.token)

            // 2. Put file to GitHub
            val url = URL(apiUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.requestMethod = "PUT"
            conn.setRequestProperty("Authorization", "Bearer ${config.token}")
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val base64Content = Base64.getEncoder().encodeToString(jsonData.toByteArray(Charsets.UTF_8))
            val payload = mutableMapOf<String, Any>(
                "message" to "Sync workout data for athlete #$athleteId [timestamp: ${System.currentTimeMillis()}]",
                "content" to base64Content,
                "branch" to config.branch
            )
            if (existingSha != null) {
                payload["sha"] = existingSha
            }

            val jsonPayload = gson.toJson(payload)
            OutputStreamWriter(conn.outputStream).use { it.write(jsonPayload) }

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                Result.success("Данные подопечного успешно загружены в облако (HTTP $responseCode)")
            } else {
                val errorMsg = conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $responseCode"
                Result.failure(Exception("GitHub API Error: $errorMsg"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Скачивает данные подопечного из GitHub через Contents API или Raw fallback.
     */
    suspend fun downloadAthleteData(
        config: GitHubSyncConfig,
        athleteId: Long
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val path = "athletes/athlete_${athleteId}.json"
            val apiUrl = "https://api.github.com/repos/${config.owner}/${config.repo}/contents/$path"

            val url = URL(apiUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.requestMethod = "GET"
            if (config.token.isNotBlank()) {
                conn.setRequestProperty("Authorization", "Bearer ${config.token}")
            }
            conn.setRequestProperty("Accept", "application/vnd.github.raw+json")
            conn.connect()

            if (conn.responseCode == 200) {
                val content = conn.inputStream.bufferedReader().readText()
                Result.success(content)
            } else {
                // Fallback: try raw.githubusercontent.com
                val rawUrl = "https://raw.githubusercontent.com/${config.owner}/${config.repo}/${config.branch}/$path"
                val rawConn = URL(rawUrl).openConnection() as HttpURLConnection
                rawConn.connectTimeout = 10000
                rawConn.readTimeout = 10000
                if (config.token.isNotBlank()) {
                    rawConn.setRequestProperty("Authorization", "token ${config.token}")
                }
                rawConn.connect()

                if (rawConn.responseCode == 200) {
                    val content = rawConn.inputStream.bufferedReader().readText()
                    Result.success(content)
                } else {
                    Result.failure(Exception("Данные подопечного не найдены в репозитории (HTTP ${rawConn.responseCode})"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Интегрирует полученные от подопечного данные (выполненные подходы, фактические веса,
     * повторения, RPE и замеры тела) обратно в локальную базу данных тренера.
     */
    suspend fun applyAthletePayload(
        dao: TrainerDao,
        clientId: Long,
        payloadJson: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val payload = gson.fromJson(payloadJson, AthleteSyncPayload::class.java)
                ?: return@withContext Result.failure(IllegalArgumentException("Неверный формат JSON"))

            var updatedSetsCount = 0
            var addedAnthroCount = 0

            val existingClient = dao.getClientById(clientId)
            if (existingClient != null && !payload.avatarBase64.isNullOrBlank() && existingClient.avatarBase64 != payload.avatarBase64) {
                dao.updateClient(existingClient.copy(avatarBase64 = payload.avatarBase64))
            }

            // 1. Обновляем сессии и выполненные подходы
            for (syncWorkout in payload.assignedWorkouts) {
                var session = dao.getSessionByClientAndDate(clientId, syncWorkout.date)
                val sessionId = if (session == null) {
                    val newSession = WorkoutSessionEntity(
                        clientId = clientId,
                        date = syncWorkout.date,
                        comments = syncWorkout.notes,
                        completed = syncWorkout.completed,
                        isSelfWorkoutAllowed = syncWorkout.isSelfWorkoutAllowed
                    )
                    dao.insertSession(newSession)
                } else {
                    if (syncWorkout.completed && !session.completed) {
                        dao.updateSession(session.copy(completed = true))
                    }
                    session.id
                }

                val existingSets = dao.getSetsForSessionSync(sessionId)

                for (syncExercise in syncWorkout.exercises) {
                    // Убеждаемся, что упражнение существует
                    val exercise = dao.getAllExercisesSync().find { it.name.equals(syncExercise.name, ignoreCase = true) }
                        ?: dao.getExerciseById(syncExercise.exerciseId)
                    val targetExerciseId = exercise?.id ?: syncExercise.exerciseId

                    for (syncSet in syncExercise.sets) {
                        val matchingSet = existingSets.find {
                            it.exerciseId == targetExerciseId && it.setNumber == syncSet.setNumber
                        }

                        if (matchingSet != null) {
                            // Обновляем фактические данные от подопечного
                            val updated = matchingSet.copy(
                                weightKg = if (syncSet.actualWeightKg > 0) syncSet.actualWeightKg else matchingSet.weightKg,
                                reps = if (syncSet.actualReps > 0) syncSet.actualReps else matchingSet.reps,
                                isCompleted = syncSet.isCompleted || matchingSet.isCompleted,
                                rpe = syncSet.rpe ?: matchingSet.rpe
                            )
                            dao.updateSet(updated)
                            updatedSetsCount++
                        } else {
                            // Новый подход, выполненный подопечным
                            val order = (existingSets.filter { it.exerciseId == targetExerciseId }.maxOfOrNull { it.exerciseOrder } ?: 1)
                            dao.insertSet(
                                WorkoutSetEntity(
                                    sessionId = sessionId,
                                    exerciseId = targetExerciseId,
                                    exerciseOrder = order,
                                    setNumber = syncSet.setNumber,
                                    weightKg = if (syncSet.actualWeightKg > 0) syncSet.actualWeightKg else syncSet.targetWeightKg,
                                    reps = if (syncSet.actualReps > 0) syncSet.actualReps else syncSet.targetReps,
                                    isCompleted = syncSet.isCompleted,
                                    rpe = syncSet.rpe
                                )
                            )
                            updatedSetsCount++
                        }
                    }
                }
            }

            // 2. Интегрируем антропометрию подопечного
            val existingAnthro = dao.getAnthropometryForClientSync(clientId)
            for (anthro in payload.anthropometry) {
                val exists = existingAnthro.any { it.date == anthro.date }
                if (!exists && anthro.weightKg > 0) {
                    dao.insertAnthropometry(
                        AnthropometryEntity(
                            clientId = clientId,
                            date = anthro.date,
                            weightKg = anthro.weightKg,
                            chestCm = anthro.chestCm,
                            waistCm = anthro.waistCm,
                            hipsCm = anthro.hipsCm,
                            bicepsCm = anthro.bicepsCm
                        )
                    )
                    addedAnthroCount++
                }
            }

            Result.success("Синхронизировано: обновлено $updatedSetsCount подходов, добавлено $addedAnthroCount замеров")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getFileSha(apiUrl: String, token: String): String? {
        return try {
            val url = URL(apiUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().readText()
                val map = gson.fromJson(response, Map::class.java)
                map["sha"] as? String
            } else null
        } catch (_: Exception) {
            null
        }
    }
}
