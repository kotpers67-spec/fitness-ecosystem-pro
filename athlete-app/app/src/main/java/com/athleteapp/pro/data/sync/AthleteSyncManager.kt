package com.athleteapp.pro.data.sync

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.athleteapp.pro.data.local.dao.AthleteDao
import com.athleteapp.pro.data.local.entities.*
import com.athleteapp.pro.data.sync.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64

class AthleteSyncManager(private val dao: AthleteDao) {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    /**
     * Executes bidirectional sync with Coach:
     * 1. Pulls latest coach workout assignments and merges into AthleteDatabase.
     * 2. Pushes athlete's completed workouts, weights, reps, RPE, and anthropometry back to GitHub.
     */
    suspend fun syncWithCoach(
        owner: String,
        repo: String,
        token: String,
        athleteId: Long
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (owner.isBlank() || repo.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("GitHub Owner и Repository должны быть указаны в настройках"))
            }

            var pulledCount = 0

            // 1. PULL: Download from GitHub
            val pullResult = pullCoachData(owner, repo, token, athleteId)
            if (pullResult.isSuccess) {
                pulledCount = pullResult.getOrDefault(0)
            }

            // 2. PUSH: If token is provided, upload athlete progress back to GitHub
            var pushMsg = ""
            if (token.isNotBlank()) {
                val pushResult = pushAthleteProgress(owner, repo, token, athleteId)
                if (pushResult.isSuccess) {
                    pushMsg = " Данные о выполненных сетах и антропометрии отправлены тренеру."
                } else {
                    pushMsg = " (Предупреждение отправки: ${pushResult.exceptionOrNull()?.message})"
                }
            }

            Result.success("Синхронизация завершена! Обновлено тренировок: $pulledCount.$pushMsg")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Pulls workout plan from GitHub and updates Room database.
     */
    /**
     * Applies coach payload JSON to Room database.
     */
    internal suspend fun applyPayloadJson(jsonContent: String): Result<Int> {
        return try {
            val payload = try {
                gson.fromJson(jsonContent, AthleteSyncPayload::class.java)
            } catch (_: Exception) {
                null
            }

            if (payload == null) {
                return Result.failure(Exception("Не удалось разобрать план тренировок"))
            }

            var importedWorkoutsCount = 0

            // Update profile name if available
            if (payload.clientName.isNotBlank()) {
                val currentProfile = dao.getProfile().firstOrNull() ?: AthleteProfileEntity()
                val updatedUuid = if (payload.clientUuid.isNotBlank()) payload.clientUuid else currentProfile.clientUuid
                dao.saveProfile(
                    currentProfile.copy(
                        fullName = payload.clientName,
                        athleteIdInCoachBase = payload.athleteId,
                        clientUuid = updatedUuid
                    )
                )
            }

            // Import anthropometry if present
            if (payload.anthropometry.isNotEmpty()) {
                val existingAnth = dao.getAllAnthropometrySync()
                for (anth in payload.anthropometry) {
                    if (existingAnth.none { it.date == anth.date }) {
                        dao.insertAnthropometry(
                            MyAnthropometryEntity(
                                date = anth.date,
                                weightKg = anth.weightKg,
                                chestCm = anth.chestCm,
                                waistCm = anth.waistCm,
                                hipsCm = anth.hipsCm,
                                bicepsCm = anth.bicepsCm
                            )
                        )
                    }
                }
            }

            if (payload.assignedWorkouts.isNotEmpty()) {
                // Cache all current exercises in DB
                val allExercises = dao.getAllExercisesSync().toMutableList()

                for (workout in payload.assignedWorkouts) {
                    var session = dao.getSessionByDate(workout.date)
                    val sessionId = if (session == null) {
                        dao.insertSession(
                            MyWorkoutSessionEntity(
                                date = workout.date,
                                notes = workout.notes,
                                completed = workout.completed,
                                isSelfWorkoutAllowed = workout.isSelfWorkoutAllowed
                            )
                        )
                    } else {
                        val updatedNotes = if (workout.notes.isNotBlank() && session.notes.isBlank()) workout.notes else session.notes
                        dao.updateSession(
                            session.copy(
                                notes = updatedNotes,
                                isSelfWorkoutAllowed = workout.isSelfWorkoutAllowed
                            )
                        )
                        session.id
                    }

                    val existingSets = dao.getSetsForSessionSync(sessionId)

                    for ((exerciseOrderIndex, syncEx) in workout.exercises.withIndex()) {
                        // Find or create assigned exercise
                        var localEx = allExercises.find { it.name.equals(syncEx.name, ignoreCase = true) }
                        if (localEx == null) {
                            val newEx = AssignedExerciseEntity(
                                name = syncEx.name,
                                muscleGroup = syncEx.muscleGroup,
                                defaultRestSeconds = 90
                            )
                            val insertedId = dao.insertExercise(newEx)
                            localEx = newEx.copy(id = if (insertedId > 0) insertedId else syncEx.exerciseId)
                            allExercises.add(localEx)
                        }

                        val resolvedExId = localEx.id

                        for (syncSet in syncEx.sets) {
                            val existingSet = existingSets.find {
                                (it.exerciseId == resolvedExId || it.exerciseName.equals(syncEx.name, ignoreCase = true)) &&
                                        it.setNumber == syncSet.setNumber
                            }

                            if (existingSet == null) {
                                // New set assigned by coach
                                val newSet = MyWorkoutSetEntity(
                                    sessionId = sessionId,
                                    exerciseId = resolvedExId,
                                    exerciseName = syncEx.name,
                                    muscleGroup = syncEx.muscleGroup,
                                    exerciseOrder = exerciseOrderIndex + 1,
                                    setNumber = syncSet.setNumber,
                                    targetWeightKg = syncSet.targetWeightKg,
                                    targetReps = syncSet.targetReps,
                                    actualWeightKg = syncSet.actualWeightKg,
                                    actualReps = syncSet.actualReps,
                                    isCompleted = syncSet.isCompleted,
                                    rpe = syncSet.rpe
                                )
                                dao.insertSet(newSet)
                            } else {
                                // If already exists locally, update target without overwriting athlete's completed actuals
                                val merged = existingSet.copy(
                                    targetWeightKg = syncSet.targetWeightKg,
                                    targetReps = syncSet.targetReps,
                                    actualWeightKg = if (existingSet.isCompleted) existingSet.actualWeightKg else syncSet.actualWeightKg,
                                    actualReps = if (existingSet.isCompleted) existingSet.actualReps else syncSet.actualReps,
                                    isCompleted = existingSet.isCompleted || syncSet.isCompleted,
                                    rpe = existingSet.rpe ?: syncSet.rpe
                                )
                                dao.updateSet(merged)
                            }
                        }
                    }
                    importedWorkoutsCount++
                }
            }

            Result.success(importedWorkoutsCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Pulls workout plan from GitHub and updates Room database.
     */
    internal suspend fun pullCoachData(
        owner: String,
        repo: String,
        token: String,
        athleteId: Long
    ): Result<Int> {
        return try {
            val path = "athletes/athlete_${athleteId}.json"
            val jsonContent = fetchGitHubFileContent(owner, repo, token, path)
                ?: return Result.failure(Exception("Файл плана не найден в репозитории тренера"))

            applyPayloadJson(jsonContent)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Pushes athlete's local workout sessions, completed sets, actuals, RPE and anthropometry to GitHub.
     */
    suspend fun pushAthleteProgress(
        owner: String,
        repo: String,
        token: String,
        athleteId: Long
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val profile = dao.getProfile().firstOrNull() ?: AthleteProfileEntity()
            val sessions = dao.getAllSessionsSync()
            val allSets = dao.getAllSetsSync()
            val allAnth = dao.getAllAnthropometrySync()

            // Group sets by session
            val setsBySessionId = allSets.groupBy { it.sessionId }

            val syncSessions = sessions.map { session ->
                val sessionSets = setsBySessionId[session.id] ?: emptyList()
                val exercisesInSession = sessionSets.groupBy { it.exerciseId }.map { (exId, setsList) ->
                    val first = setsList.first()
                    SyncExercise(
                        exerciseId = exId,
                        name = first.exerciseName,
                        muscleGroup = first.muscleGroup,
                        sets = setsList.map { set ->
                            SyncWorkoutSet(
                                setNumber = set.setNumber,
                                targetWeightKg = set.targetWeightKg,
                                targetReps = set.targetReps,
                                actualWeightKg = set.actualWeightKg,
                                actualReps = set.actualReps,
                                isCompleted = set.isCompleted,
                                rpe = set.rpe
                            )
                        }
                    )
                }

                SyncWorkoutSession(
                    date = session.date,
                    notes = session.notes,
                    completed = session.completed,
                    isSelfWorkoutAllowed = session.isSelfWorkoutAllowed,
                    exercises = exercisesInSession
                )
            }

            val syncAnth = allAnth.map {
                SyncAnthropometry(
                    date = it.date,
                    weightKg = it.weightKg,
                    chestCm = it.chestCm,
                    waistCm = it.waistCm,
                    hipsCm = it.hipsCm,
                    bicepsCm = it.bicepsCm
                )
            }

            val payload = AthleteSyncPayload(
                clientUuid = profile.clientUuid,
                athleteId = athleteId,
                syncTimestamp = System.currentTimeMillis(),
                clientName = profile.fullName,
                assignedWorkouts = syncSessions,
                anthropometry = syncAnth
            )

            val jsonPayload = gson.toJson(payload)

            // Upload both to athlete progress and main athlete file
            uploadFileToGitHub(
                owner = owner,
                repo = repo,
                token = token,
                path = "athletes/athlete_${athleteId}.json",
                contentString = jsonPayload,
                commitMessage = "Athlete #${athleteId} (${profile.fullName}) workout data update"
            )

            uploadFileToGitHub(
                owner = owner,
                repo = repo,
                token = token,
                path = "athletes/athlete_${athleteId}_progress.json",
                contentString = jsonPayload,
                commitMessage = "Athlete #${athleteId} progress record"
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun fetchGitHubFileContent(owner: String, repo: String, token: String, path: String): String? {
        // First try GitHub API with token
        if (token.isNotBlank()) {
            try {
                val apiUrl = "https://api.github.com/repos/$owner/$repo/contents/$path"
                val conn = URL(apiUrl).openConnection() as HttpURLConnection
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.setRequestProperty("Accept", "application/vnd.github.raw+json")
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                if (conn.responseCode == 200) {
                    return conn.inputStream.bufferedReader().readText()
                }
            } catch (_: Exception) {}
        }

        // Fallback to raw.githubusercontent.com
        return try {
            val rawUrl = "https://raw.githubusercontent.com/$owner/$repo/main/$path"
            val conn = URL(rawUrl).openConnection() as HttpURLConnection
            if (token.isNotBlank()) {
                conn.setRequestProperty("Authorization", "token $token")
            }
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            if (conn.responseCode == 200) {
                conn.inputStream.bufferedReader().readText()
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun uploadFileToGitHub(
        owner: String,
        repo: String,
        token: String,
        path: String,
        contentString: String,
        commitMessage: String
    ) {
        val apiUrl = "https://api.github.com/repos/$owner/$repo/contents/$path"
        val existingSha = getFileSha(apiUrl, token)

        val base64Content = Base64.getEncoder().encodeToString(contentString.toByteArray(Charsets.UTF_8))
        val bodyMap = mutableMapOf<String, Any>(
            "message" to "$commitMessage [ts: ${System.currentTimeMillis()}]",
            "content" to base64Content,
            "branch" to "main"
        )
        if (existingSha != null) {
            bodyMap["sha"] = existingSha
        }

        val url = URL(apiUrl)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "PUT"
        conn.setRequestProperty("Authorization", "Bearer $token")
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true
        conn.connectTimeout = 10000
        conn.readTimeout = 10000

        OutputStreamWriter(conn.outputStream).use { it.write(gson.toJson(bodyMap)) }
        val code = conn.responseCode
        if (code !in 200..299) {
            val err = conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $code"
            throw Exception("GitHub API error ($code): $err")
        }
    }

    private fun getFileSha(apiUrl: String, token: String): String? {
        return try {
            val url = URL(apiUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
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
