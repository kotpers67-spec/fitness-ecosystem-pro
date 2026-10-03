package com.trainerapp.pro.data.backup

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.trainerapp.pro.data.local.dao.TrainerDao
import com.trainerapp.pro.data.local.entities.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class FullBackupData(
    val version: Int = 1,
    val exportDate: String = System.currentTimeMillis().toString(),
    val clients: List<ClientEntity>,
    val exercises: List<ExerciseEntity>,
    val sessions: List<WorkoutSessionEntity>,
    val sets: List<WorkoutSetEntity>,
    val anthropometry: List<AnthropometryEntity>
)

class BackupManager(private val dao: TrainerDao) {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    suspend fun exportToJson(): String = withContext(Dispatchers.IO) {
        val backup = FullBackupData(
            clients = dao.getAllClientsSync(),
            exercises = dao.getAllExercisesSync(),
            sessions = dao.getAllSessionsSync(),
            sets = dao.getAllSetsSync(),
            anthropometry = dao.getAllAnthropometrySync()
        )
        gson.toJson(backup)
    }

    suspend fun importFromJson(jsonString: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val backup = gson.fromJson(jsonString, FullBackupData::class.java)
                ?: return@withContext Result.failure(IllegalArgumentException("Invalid JSON format"))

            for (client in backup.clients) {
                dao.insertClient(client)
            }
            dao.insertExercises(backup.exercises)
            for (session in backup.sessions) {
                dao.insertSession(session)
            }
            for (set in backup.sets) {
                dao.insertSet(set)
            }
            for (anthro in backup.anthropometry) {
                dao.insertAnthropometry(anthro)
            }

            Result.success(backup.clients.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
