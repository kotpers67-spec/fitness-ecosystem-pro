package com.trainerapp.pro

import com.google.gson.Gson
import com.trainerapp.pro.data.local.dao.SetHistoryItem
import com.trainerapp.pro.data.local.dao.TrainerDao
import com.trainerapp.pro.data.local.entities.*
import com.trainerapp.pro.data.sync.GitHubSyncManager
import com.trainerapp.pro.data.sync.model.*
import com.trainerapp.pro.domain.calculators.NeuroAdaptiveEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class SyncAndReadinessRemediationTest {

    private class FakeTrainerDao : TrainerDao {
        val sessions = mutableListOf<WorkoutSessionEntity>()
        val sets = mutableListOf<WorkoutSetEntity>()
        val exercises = mutableListOf<ExerciseEntity>()
        val clients = mutableListOf<ClientEntity>()
        val anthropometry = mutableListOf<AnthropometryEntity>()

        var updateSessionCalled = false
        var insertSessionCalled = false

        override fun getAllClients(): Flow<List<ClientEntity>> = emptyFlow()
        override suspend fun getClientById(id: Long): ClientEntity? = clients.find { it.id == id }
        override suspend fun getClientByUuid(uuid: String): ClientEntity? = clients.find { it.clientUuid == uuid }
        override suspend fun getClientByPairingCode(code: String): ClientEntity? = clients.find { it.pairingCode == code }
        override suspend fun getClientByPhone(phone: String): ClientEntity? = clients.find { it.phone == phone || (it.phone.isNotBlank() && it.phone.endsWith(phone)) }
        override suspend fun getClientByFullName(fullName: String): ClientEntity? = clients.find { it.fullName.equals(fullName, ignoreCase = true) }
        override suspend fun insertClient(client: ClientEntity): Long {
            val id = (clients.maxOfOrNull { it.id } ?: 0) + 1
            clients.add(client.copy(id = id))
            return id
        }
        override suspend fun updateClient(client: ClientEntity) {}
        override suspend fun deleteClient(client: ClientEntity) {}

        override fun getAllExercises(): Flow<List<ExerciseEntity>> = emptyFlow()
        override suspend fun getExerciseById(id: Long): ExerciseEntity? = exercises.find { it.id == id }
        override suspend fun insertExercise(exercise: ExerciseEntity): Long {
            val id = (exercises.maxOfOrNull { it.id } ?: 0) + 1
            exercises.add(exercise.copy(id = id))
            return id
        }
        override suspend fun insertExercises(exercises: List<ExerciseEntity>) {}
        override suspend fun updateExercise(exercise: ExerciseEntity) {}
        override suspend fun deleteExercise(exercise: ExerciseEntity) {}

        override suspend fun getSessionByClientAndDate(clientId: Long, date: String): WorkoutSessionEntity? {
            return sessions.find { it.clientId == clientId && it.date == date }
        }
        override fun getSessionsForClient(clientId: Long): Flow<List<WorkoutSessionEntity>> = emptyFlow()
        override suspend fun insertSession(session: WorkoutSessionEntity): Long {
            insertSessionCalled = true
            // If exists, replace (simulate SQLite REPLACE with CASCADE)
            val existing = sessions.indexOfFirst { it.id == session.id && session.id > 0 }
            val id = if (session.id > 0) session.id else ((sessions.maxOfOrNull { it.id } ?: 0) + 1)
            val toSave = session.copy(id = id)
            if (existing >= 0) {
                // CASCADE DELETE sets
                sets.removeAll { it.sessionId == id }
                sessions[existing] = toSave
            } else {
                sessions.add(toSave)
            }
            return id
        }
        override suspend fun updateSession(session: WorkoutSessionEntity) {
            updateSessionCalled = true
            val idx = sessions.indexOfFirst { it.id == session.id }
            if (idx >= 0) {
                sessions[idx] = session
            }
        }
        override suspend fun getLastCompletedSessionBefore(clientId: Long, currentDate: String): WorkoutSessionEntity? {
            return sessions.filter { it.clientId == clientId && it.date < currentDate && it.completed }
                .maxByOrNull { it.date }
        }
        override fun getSetsForSession(sessionId: Long): Flow<List<WorkoutSetEntity>> = emptyFlow()
        override suspend fun getSetsForSessionSync(sessionId: Long): List<WorkoutSetEntity> {
            return sets.filter { it.sessionId == sessionId }
        }
        override suspend fun insertSet(workoutSet: WorkoutSetEntity): Long {
            val id = (sets.maxOfOrNull { it.id } ?: 0) + 1
            sets.add(workoutSet.copy(id = id))
            return id
        }
        override suspend fun updateSet(workoutSet: WorkoutSetEntity) {
            val idx = sets.indexOfFirst { it.id == workoutSet.id }
            if (idx >= 0) {
                sets[idx] = workoutSet
            }
        }
        override suspend fun deleteSet(workoutSet: WorkoutSetEntity) {
            sets.removeAll { it.id == workoutSet.id }
        }
        override suspend fun deleteExerciseFromSession(sessionId: Long, exerciseId: Long) {
            sets.removeAll { it.sessionId == sessionId && it.exerciseId == exerciseId }
        }
        override suspend fun getLastExerciseSet(clientId: Long, exerciseId: Long, currentDate: String): WorkoutSetEntity? = null
        override suspend fun getLastExerciseSetsForClient(clientId: Long, exerciseId: Long, currentDate: String): List<WorkoutSetEntity> = emptyList()
        override suspend fun getLastExerciseDateForClient(clientId: Long, exerciseId: Long, currentDate: String): String? = null
        override fun getAnthropometryForClient(clientId: Long): Flow<List<AnthropometryEntity>> = emptyFlow()
        override suspend fun insertAnthropometry(entry: AnthropometryEntity): Long {
            val id = (anthropometry.maxOfOrNull { it.id } ?: 0) + 1
            anthropometry.add(entry.copy(id = id))
            return id
        }
        override fun getExerciseHistory(clientId: Long, exerciseId: Long): Flow<List<SetHistoryItem>> = emptyFlow()
        override fun getSettings(): Flow<AppSettingsEntity?> = emptyFlow()
        override suspend fun saveSettings(settings: AppSettingsEntity) {}
        override suspend fun getAllClientsSync(): List<ClientEntity> = clients
        override suspend fun getAllExercisesSync(): List<ExerciseEntity> = exercises
        override suspend fun getAllSessionsSync(): List<WorkoutSessionEntity> = sessions
        override suspend fun getAllSetsSync(): List<WorkoutSetEntity> = sets
        override suspend fun getAllAnthropometrySync(): List<AnthropometryEntity> = anthropometry
        override fun getAppointmentsForClient(clientId: Long): Flow<List<AppointmentEntity>> = emptyFlow()
        override suspend fun getAppointmentById(id: Long): AppointmentEntity? = null
        override suspend fun insertAppointment(appointment: AppointmentEntity): Long = 1L
        override suspend fun updateAppointment(appointment: AppointmentEntity) {}
        override suspend fun deleteAppointment(appointment: AppointmentEntity) {}
        override suspend fun getAllAppointmentsSync(): List<AppointmentEntity> = emptyList()
        override suspend fun getSessionsForClientSync(clientId: Long): List<WorkoutSessionEntity> = sessions.filter { it.clientId == clientId }
        override suspend fun getAnthropometryForClientSync(clientId: Long): List<AnthropometryEntity> = anthropometry.filter { it.clientId == clientId }
    }

    @Test
    fun testPreventCascadeDeletionOnSessionCompletion() = runBlocking {
        val fakeDao = FakeTrainerDao()
        val syncManager = GitHubSyncManager()

        // 1. Setup existing session with 3 child sets
        val session = WorkoutSessionEntity(id = 1, clientId = 10, date = "2026-10-03", completed = false)
        fakeDao.sessions.add(session)
        val exercise = ExerciseEntity(id = 1, name = "Приседания", muscleGroup = "Ноги")
        fakeDao.exercises.add(exercise)
        fakeDao.sets.add(WorkoutSetEntity(id = 1, sessionId = 1, exerciseId = 1, exerciseOrder = 1, setNumber = 1, weightKg = 100.0, reps = 5, isCompleted = false))
        fakeDao.sets.add(WorkoutSetEntity(id = 2, sessionId = 1, exerciseId = 1, exerciseOrder = 1, setNumber = 2, weightKg = 100.0, reps = 5, isCompleted = false))
        fakeDao.sets.add(WorkoutSetEntity(id = 3, sessionId = 1, exerciseId = 1, exerciseOrder = 1, setNumber = 3, weightKg = 100.0, reps = 5, isCompleted = false))

        // 2. Incoming athlete payload marks workout as completed
        val payload = AthleteSyncPayload(
            athleteId = 10,
            syncTimestamp = System.currentTimeMillis(),
            clientName = "Иван",
            assignedWorkouts = listOf(
                SyncWorkoutSession(
                    date = "2026-10-03",
                    notes = "",
                    completed = true,
                    exercises = listOf(
                        SyncExercise(
                            exerciseId = 1,
                            name = "Приседания",
                            muscleGroup = "Ноги",
                            sets = listOf(
                                SyncWorkoutSet(setNumber = 1, targetWeightKg = 100.0, targetReps = 5, actualWeightKg = 100.0, actualReps = 5, isCompleted = true, rpe = 8.0),
                                SyncWorkoutSet(setNumber = 2, targetWeightKg = 100.0, targetReps = 5, actualWeightKg = 100.0, actualReps = 5, isCompleted = true, rpe = 8.5),
                                SyncWorkoutSet(setNumber = 3, targetWeightKg = 100.0, targetReps = 5, actualWeightKg = 100.0, actualReps = 5, isCompleted = true, rpe = 9.0)
                            )
                        )
                    )
                )
            ),
            anthropometry = emptyList()
        )

        fakeDao.insertSessionCalled = false
        val result = syncManager.applyAthletePayload(fakeDao, 10, Gson().toJson(payload))

        assertTrue(result.isSuccess)
        assertTrue("updateSession must be called instead of insertSession", fakeDao.updateSessionCalled)
        assertFalse("insertSession must NOT be called for existing session completion update", fakeDao.insertSessionCalled)
        assertEquals("All 3 child sets must survive without cascade deletion", 3, fakeDao.sets.size)
        assertTrue("Session must be marked completed", fakeDao.sessions.first().completed)
        assertTrue("All sets must be marked completed", fakeDao.sets.all { it.isCompleted })
    }

    @Test
    fun testExerciseIdDesynchronizationByName() = runBlocking {
        val fakeDao = FakeTrainerDao()
        val syncManager = GitHubSyncManager()

        // Coach DB: 'Становая тяга' has ID 15, while ID 1 is 'Жим штанги лежа'
        val benchPress = ExerciseEntity(id = 1, name = "Жим штанги лежа", muscleGroup = "Грудь")
        val deadlift = ExerciseEntity(id = 15, name = "Становая тяга", muscleGroup = "Спина")
        fakeDao.exercises.addAll(listOf(benchPress, deadlift))

        val session = WorkoutSessionEntity(id = 1, clientId = 5, date = "2026-10-03", completed = false)
        fakeDao.sessions.add(session)
        // Coach set for deadlift (ID 15)
        fakeDao.sets.add(WorkoutSetEntity(id = 1, sessionId = 1, exerciseId = 15, exerciseOrder = 1, setNumber = 1, weightKg = 140.0, reps = 5, isCompleted = false))

        // Athlete sends payload where Athlete's local Room ID for 'Становая тяга' was 1
        val payload = AthleteSyncPayload(
            athleteId = 5,
            syncTimestamp = System.currentTimeMillis(),
            clientName = "Алексей",
            assignedWorkouts = listOf(
                SyncWorkoutSession(
                    date = "2026-10-03",
                    completed = true,
                    exercises = listOf(
                        SyncExercise(
                            exerciseId = 1, // Athlete's local ID was 1
                            name = "Становая тяга", // But name matches deadlift!
                            muscleGroup = "Спина",
                            sets = listOf(
                                SyncWorkoutSet(setNumber = 1, targetWeightKg = 140.0, targetReps = 5, actualWeightKg = 145.0, actualReps = 5, isCompleted = true, rpe = 8.0)
                            )
                        )
                    )
                )
            ),
            anthropometry = emptyList()
        )

        val result = syncManager.applyAthletePayload(fakeDao, 5, Gson().toJson(payload))
        assertTrue(result.isSuccess)

        // Verify the updated set is attributed to Deadlift (ID 15), NOT Bench Press (ID 1)
        val updatedSet = fakeDao.sets.find { it.sessionId == 1L && it.setNumber == 1 }
        assertNotNull(updatedSet)
        assertEquals("Exercise must be mapped to Deadlift (ID 15) by name lookup", 15L, updatedSet!!.exerciseId)
        assertEquals(145.0, updatedSet.weightKg, 0.01)
        assertTrue(updatedSet.isCompleted)
    }

    @Test
    fun testSafeRpeAutoRegulationNoOverloadOnZeroOrUnrated() {
        // RPE 0.0 -> Maintain weight without overload
        val setRpe0 = WorkoutSetEntity(sessionId = 1, exerciseId = 1, exerciseOrder = 1, setNumber = 1, weightKg = 100.0, reps = 8, isCompleted = true, rpe = 0.0)
        val rec0 = NeuroAdaptiveEngine.calculateAdaptiveRecommendation(setRpe0)
        assertEquals("RPE 0.0 must maintain weight without overload", 100.0, rec0.recommendedWeightKg, 0.01)
        assertEquals("RPE 0.0 must have 0% adjustment", 0.0, rec0.adjustmentPercent, 0.01)

        // RPE null -> Maintain weight without overload
        val setRpeNull = WorkoutSetEntity(sessionId = 1, exerciseId = 1, exerciseOrder = 1, setNumber = 1, weightKg = 100.0, reps = 8, isCompleted = true, rpe = null)
        val recNull = NeuroAdaptiveEngine.calculateAdaptiveRecommendation(setRpeNull)
        assertEquals("Null RPE must maintain weight without overload", 100.0, recNull.recommendedWeightKg, 0.01)
        assertEquals("Null RPE must have 0% adjustment", 0.0, recNull.adjustmentPercent, 0.01)

        // RPE in 1.0..7.0 -> Progressive overload
        val setRpe6 = WorkoutSetEntity(sessionId = 1, exerciseId = 1, exerciseOrder = 1, setNumber = 1, weightKg = 100.0, reps = 8, isCompleted = true, rpe = 6.0)
        val rec6 = NeuroAdaptiveEngine.calculateAdaptiveRecommendation(setRpe6)
        assertTrue("RPE 6.0 must apply progressive overload", rec6.recommendedWeightKg > 100.0)
        assertEquals(5.0, rec6.adjustmentPercent, 0.01)
    }

    @Test
    fun testTonnageFilterExcludesUncompletedSets() {
        val sets = listOf(
            WorkoutSetEntity(sessionId = 1, exerciseId = 1, exerciseOrder = 1, setNumber = 1, weightKg = 100.0, reps = 10, isCompleted = true),
            WorkoutSetEntity(sessionId = 1, exerciseId = 1, exerciseOrder = 1, setNumber = 2, weightKg = 120.0, reps = 5, isCompleted = false) // Uncompleted
        )
        val tonnage = NeuroAdaptiveEngine.calculateTonnage(sets)
        assertEquals("Only completed sets count towards tonnage", 1000.0, tonnage, 0.01)

        val totalReps = NeuroAdaptiveEngine.calculateTotalReps(sets)
        assertEquals("Only completed sets count towards total reps", 10, totalReps)

        val intensity = NeuroAdaptiveEngine.calculateAverageIntensity(sets)
        assertEquals("Intensity calculated strictly from completed sets", 100.0, intensity, 0.01)
    }

    @Test
    fun testReadinessScorePreviousSessionDateComparison() {
        // Today is 2026-10-03.
        // If prior completed session was 2 days ago (2026-10-01), score reflects supercompensation:
        val readiness2Days = NeuroAdaptiveEngine.calculateReadinessScore("2026-10-01", "2026-10-03")
        assertTrue("2 days rest yields supercompensation (>= 85.0)", readiness2Days.score >= 85.0)

        // If today is compared against today (the old bug), score is deload (< 60.0):
        val sameDay = NeuroAdaptiveEngine.calculateReadinessScore("2026-10-03", "2026-10-03")
        assertTrue("Same day yields deload warning (<= 50.0)", sameDay.score <= 50.0)
    }
}
