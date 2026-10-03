package com.trainerapp.pro

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.trainerapp.pro.data.local.dao.SetHistoryItem
import com.trainerapp.pro.data.local.dao.TrainerDao
import com.trainerapp.pro.data.local.entities.*
import com.trainerapp.pro.data.sync.GitHubSyncManager
import com.trainerapp.pro.data.sync.model.AthleteSyncPayload
import com.trainerapp.pro.data.sync.model.SyncExercise
import com.trainerapp.pro.data.sync.model.SyncWorkoutSession
import com.trainerapp.pro.data.sync.model.SyncWorkoutSet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class TrainerMilestone2FeatureTest {

    private class TestDao : TrainerDao {
        val clients = mutableListOf<ClientEntity>()
        val sessions = mutableListOf<WorkoutSessionEntity>()
        val sets = mutableListOf<WorkoutSetEntity>()
        val exercises = mutableListOf<ExerciseEntity>()
        val anthro = mutableListOf<AnthropometryEntity>()

        override fun getAllClients(): Flow<List<ClientEntity>> = emptyFlow()
        override suspend fun getClientById(id: Long): ClientEntity? = clients.find { it.id == id }
        override suspend fun getClientByUuid(uuid: String): ClientEntity? = clients.find { it.clientUuid == uuid }
        override suspend fun getClientByPairingCode(code: String): ClientEntity? = clients.find { it.pairingCode == code }
        override suspend fun insertClient(client: ClientEntity): Long {
            val newId = (clients.maxOfOrNull { it.id } ?: 0) + 1
            clients.add(client.copy(id = newId))
            return newId
        }
        override suspend fun updateClient(client: ClientEntity) {
            val idx = clients.indexOfFirst { it.id == client.id }
            if (idx >= 0) clients[idx] = client
        }
        override suspend fun deleteClient(client: ClientEntity) {
            clients.removeAll { it.id == client.id }
        }

        override fun getAllExercises(): Flow<List<ExerciseEntity>> = emptyFlow()
        override suspend fun getExerciseById(id: Long): ExerciseEntity? = exercises.find { it.id == id }
        override suspend fun insertExercise(exercise: ExerciseEntity): Long {
            val newId = (exercises.maxOfOrNull { it.id } ?: 0) + 1
            exercises.add(exercise.copy(id = newId))
            return newId
        }
        override suspend fun insertExercises(exercises: List<ExerciseEntity>) {}
        override suspend fun updateExercise(exercise: ExerciseEntity) {}
        override suspend fun deleteExercise(exercise: ExerciseEntity) {}

        override suspend fun getSessionByClientAndDate(clientId: Long, date: String): WorkoutSessionEntity? {
            return sessions.find { it.clientId == clientId && it.date == date }
        }
        override fun getSessionsForClient(clientId: Long): Flow<List<WorkoutSessionEntity>> = emptyFlow()
        override suspend fun insertSession(session: WorkoutSessionEntity): Long {
            val newId = (sessions.maxOfOrNull { it.id } ?: 0) + 1
            sessions.add(session.copy(id = newId))
            return newId
        }
        override suspend fun updateSession(session: WorkoutSessionEntity) {
            val idx = sessions.indexOfFirst { it.id == session.id }
            if (idx >= 0) sessions[idx] = session
        }
        override suspend fun getLastCompletedSessionBefore(clientId: Long, currentDate: String): WorkoutSessionEntity? = null
        override fun getSetsForSession(sessionId: Long): Flow<List<WorkoutSetEntity>> = emptyFlow()
        override suspend fun getSetsForSessionSync(sessionId: Long): List<WorkoutSetEntity> {
            return sets.filter { it.sessionId == sessionId }
        }
        override suspend fun insertSet(workoutSet: WorkoutSetEntity): Long {
            val newId = (sets.maxOfOrNull { it.id } ?: 0) + 1
            sets.add(workoutSet.copy(id = newId))
            return newId
        }
        override suspend fun updateSet(workoutSet: WorkoutSetEntity) {
            val idx = sets.indexOfFirst { it.id == workoutSet.id }
            if (idx >= 0) sets[idx] = workoutSet
        }
        override suspend fun deleteSet(workoutSet: WorkoutSetEntity) {
            sets.removeAll { it.id == workoutSet.id }
        }
        override suspend fun deleteExerciseFromSession(sessionId: Long, exerciseId: Long) {}
        override suspend fun getLastExerciseSet(clientId: Long, exerciseId: Long, currentDate: String): WorkoutSetEntity? = null
        override suspend fun getLastExerciseSetsForClient(clientId: Long, exerciseId: Long, currentDate: String): List<WorkoutSetEntity> = emptyList()
        override suspend fun getLastExerciseDateForClient(clientId: Long, exerciseId: Long, currentDate: String): String? = null
        override fun getAnthropometryForClient(clientId: Long): Flow<List<AnthropometryEntity>> = emptyFlow()
        override suspend fun insertAnthropometry(entry: AnthropometryEntity): Long = 1L
        override fun getExerciseHistory(clientId: Long, exerciseId: Long): Flow<List<SetHistoryItem>> = emptyFlow()
        override fun getSettings(): Flow<AppSettingsEntity?> = emptyFlow()
        override suspend fun saveSettings(settings: AppSettingsEntity) {}
        override suspend fun getAllClientsSync(): List<ClientEntity> = clients
        override suspend fun getAllExercisesSync(): List<ExerciseEntity> = exercises
        override suspend fun getAllSessionsSync(): List<WorkoutSessionEntity> = sessions
        override suspend fun getAllSetsSync(): List<WorkoutSetEntity> = sets
        override suspend fun getAllAnthropometrySync(): List<AnthropometryEntity> = anthro
        override fun getAppointmentsForClient(clientId: Long): Flow<List<AppointmentEntity>> = emptyFlow()
        override suspend fun getAppointmentById(id: Long): AppointmentEntity? = null
        override suspend fun insertAppointment(appointment: AppointmentEntity): Long = 1L
        override suspend fun updateAppointment(appointment: AppointmentEntity) {}
        override suspend fun deleteAppointment(appointment: AppointmentEntity) {}
        override suspend fun getAllAppointmentsSync(): List<AppointmentEntity> = emptyList()
        override suspend fun getSessionsForClientSync(clientId: Long): List<WorkoutSessionEntity> {
            return sessions.filter { it.clientId == clientId }
        }
        override suspend fun getAnthropometryForClientSync(clientId: Long): List<AnthropometryEntity> {
            return anthro.filter { it.clientId == clientId }
        }
    }

    @Test
    fun testClientEntityHasValidUuidAndPairingCode() {
        val client = ClientEntity(
            fullName = "Александр Смирнов",
            phone = "+7 999 123-45-67",
            pairingCode = "739102"
        )
        assertNotNull(client.clientUuid)
        assertTrue(client.clientUuid.isNotBlank())
        assertEquals("739102", client.pairingCode)
    }

    @Test
    fun testWorkoutSessionEntitySelfWorkoutPermissionFlag() {
        val sessionDefault = WorkoutSessionEntity(
            clientId = 1L,
            date = "2026-10-03"
        )
        assertFalse(sessionDefault.isSelfWorkoutAllowed)

        val sessionAllowed = sessionDefault.copy(isSelfWorkoutAllowed = true)
        assertTrue(sessionAllowed.isSelfWorkoutAllowed)
    }

    @Test
    fun testDaoQueriesByUuidAndPairingCode() = runBlocking {
        val dao = TestDao()
        val uuid = UUID.randomUUID().toString()
        val code = "739102"

        val clientId = dao.insertClient(
            ClientEntity(
                fullName = "Тестовый Атлет",
                phone = "+7 900 111-22-33",
                clientUuid = uuid,
                pairingCode = code
            )
        )

        val foundByUuid = dao.getClientByUuid(uuid)
        assertNotNull(foundByUuid)
        assertEquals(clientId, foundByUuid?.id)
        assertEquals("Тестовый Атлет", foundByUuid?.fullName)

        val foundByCode = dao.getClientByPairingCode(code)
        assertNotNull(foundByCode)
        assertEquals(clientId, foundByCode?.id)

        val notFound = dao.getClientByPairingCode("000000")
        assertNull(notFound)
    }

    @Test
    fun testSyncPayloadContainsClientUuidAndSelfWorkoutPermission() = runBlocking {
        val dao = TestDao()
        val syncManager = GitHubSyncManager()

        val uuid = "f47ac10b-58cc-4372-a567-0e02b2c3d479"
        val client = ClientEntity(
            id = 1L,
            fullName = "Александр Смирнов",
            clientUuid = uuid,
            pairingCode = "739102"
        )
        dao.clients.add(client)

        val session = WorkoutSessionEntity(
            id = 10L,
            clientId = 1L,
            date = "2026-10-03",
            comments = "Жим лежа",
            completed = false,
            isSelfWorkoutAllowed = true
        )
        dao.sessions.add(session)

        val exercise = ExerciseEntity(id = 5L, name = "Жим штанги", muscleGroup = "Грудь")
        dao.exercises.add(exercise)

        val workoutSet = WorkoutSetEntity(
            id = 100L,
            sessionId = 10L,
            exerciseId = 5L,
            exerciseOrder = 1,
            setNumber = 1,
            weightKg = 100.0,
            reps = 8,
            isCompleted = true
        )
        dao.sets.add(workoutSet)

        val payload = syncManager.buildAthletePayload(dao, client)
        assertEquals(uuid, payload.clientUuid)
        assertEquals("739102", payload.pairingCode)
        assertEquals(1, payload.assignedWorkouts.size)
        assertTrue(payload.assignedWorkouts[0].isSelfWorkoutAllowed)
        assertEquals("2026-10-03", payload.assignedWorkouts[0].date)
    }

    @Test
    fun testApplyAthletePayloadPreservesSelfWorkoutPermission() = runBlocking {
        val dao = TestDao()
        val syncManager = GitHubSyncManager()
        val gson = Gson()

        val client = ClientEntity(id = 2L, fullName = "Елена", clientUuid = "uuid-elena")
        dao.clients.add(client)

        val payload = AthleteSyncPayload(
            clientUuid = "uuid-elena",
            athleteId = 2L,
            clientName = "Елена",
            assignedWorkouts = listOf(
                SyncWorkoutSession(
                    date = "2026-10-04",
                    notes = "Самостоятельная",
                    completed = true,
                    isSelfWorkoutAllowed = true,
                    exercises = listOf(
                        SyncExercise(
                            exerciseId = 1L,
                            name = "Приседания",
                            muscleGroup = "Ноги",
                            sets = listOf(
                                SyncWorkoutSet(
                                    setNumber = 1,
                                    targetWeightKg = 50.0,
                                    targetReps = 12,
                                    actualWeightKg = 50.0,
                                    actualReps = 12,
                                    isCompleted = true
                                )
                            )
                        )
                    )
                )
            )
        )

        val res = syncManager.applyAthletePayload(dao, 2L, gson.toJson(payload))
        assertTrue(res.isSuccess)

        val createdSession = dao.getSessionByClientAndDate(2L, "2026-10-04")
        assertNotNull(createdSession)
        assertTrue(createdSession!!.isSelfWorkoutAllowed)
        assertTrue(createdSession.completed)
    }

    @Test
    fun testPinAndQrParsing() {
        val formattedPin = "739-102"
        val cleanPin = formattedPin.filter { it.isDigit() }
        assertEquals("739102", cleanPin)

        val qrJsonStr = """{"pin":"739102","uuid":"f47ac10b","name":"Александр","phone":"+79991234567"}"""
        val parsed = JsonParser.parseString(qrJsonStr).asJsonObject
        assertEquals("739102", parsed.get("pin").asString)
        assertEquals("f47ac10b", parsed.get("uuid").asString)
        assertEquals("Александр", parsed.get("name").asString)
    }

    @Test
    fun testClientSearchFilterLogic() {
        val clients = listOf(
            ClientEntity(id = 1L, fullName = "Александр Смирнов", phone = "+7 (999) 123-45-67"),
            ClientEntity(id = 2L, fullName = "Елена Васильева", phone = "+7 (988) 765-43-21"),
            ClientEntity(id = 3L, fullName = "Дмитрий Смирнов", phone = "+7 (911) 555-00-11")
        )

        // Search by last name
        val queryName = "смирнов"
        val matchName = clients.filter { it.fullName.lowercase().contains(queryName) }
        assertEquals(2, matchName.size)

        // Search by phone digits
        val queryPhone = "7654321"
        val matchPhone = clients.filter { it.phone.filter { ch -> ch.isDigit() }.contains(queryPhone) }
        assertEquals(1, matchPhone.size)
        assertEquals("Елена Васильева", matchPhone.first().fullName)
    }
}
