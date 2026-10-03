package com.athleteapp.pro.data.backup

import com.athleteapp.pro.data.local.dao.AthleteDao
import com.athleteapp.pro.data.local.entities.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AthleteFullBackupData(
    val version: Int = 1,
    val exportDate: String = System.currentTimeMillis().toString(),
    val exercises: List<AssignedExerciseEntity>,
    val sessions: List<MyWorkoutSessionEntity>,
    val sets: List<MyWorkoutSetEntity>,
    val anthropometry: List<MyAnthropometryEntity>
)

class AthleteBackupManager(private val dao: AthleteDao) {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    suspend fun exportToJson(): String = withContext(Dispatchers.IO) {
        val backup = AthleteFullBackupData(
            exercises = dao.getAllExercisesSync(),
            sessions = dao.getAllSessionsSync(),
            sets = dao.getAllSetsSync(),
            anthropometry = dao.getAllAnthropometrySync()
        )
        gson.toJson(backup)
    }

    suspend fun importFromJson(jsonString: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val backup = gson.fromJson(jsonString, AthleteFullBackupData::class.java)
                ?: return@withContext Result.failure(IllegalArgumentException("Неверный формат JSON файла"))

            if (backup.exercises.isNotEmpty()) {
                dao.insertExercises(backup.exercises)
            }
            for (session in backup.sessions) {
                dao.insertSession(session)
            }
            for (set in backup.sets) {
                dao.insertSet(set)
            }
            for (anthro in backup.anthropometry) {
                dao.insertAnthropometry(anthro)
            }

            val totalImported = backup.exercises.size + backup.sessions.size + backup.sets.size + backup.anthropometry.size
            Result.success(totalImported)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
