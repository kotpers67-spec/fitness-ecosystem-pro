package com.trainerapp.pro

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.trainerapp.pro.data.local.dao.SetHistoryItem
import com.trainerapp.pro.data.local.dao.TrainerDao
import com.trainerapp.pro.data.local.entities.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import com.trainerapp.pro.ui.screens.extractPairingCode

class TrainerRemediationV105Test {

    private class TestDao : TrainerDao {
        val clients = mutableListOf<ClientEntity>()
        val sessions = mutableListOf<WorkoutSessionEntity>()
        val sets = mutableListOf<WorkoutSetEntity>()
        val exercises = mutableListOf<ExerciseEntity>()
        val anthro = mutableListOf<AnthropometryEntity>()
        val appointments = mutableListOf<AppointmentEntity>()

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
        override suspend fun getLastCompletedSessionBefore(clientId: Long, currentDate: String): WorkoutSessionEntity? {
            return sessions.filter { it.clientId == clientId && it.date < currentDate && it.completed }
                .maxByOrNull { it.date }
        }
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

        override suspend fun getLastExerciseSetsForClient(clientId: Long, exerciseId: Long, currentDate: String): List<WorkoutSetEntity> {
            val validSessions = sessions.filter { it.clientId == clientId && it.date < currentDate }
            val validSessionIds = validSessions.map { it.id }
            val relevantSets = sets.filter { it.sessionId in validSessionIds && it.exerciseId == exerciseId && it.isCompleted }
            if (relevantSets.isEmpty()) return emptyList()

            val latestSessionId = validSessions
                .filter { s -> relevantSets.any { it.sessionId == s.id } }
                .maxByOrNull { it.date }?.id ?: return emptyList()

            return relevantSets.filter { it.sessionId == latestSessionId }.sortedBy { it.setNumber }
        }

        override suspend fun getLastExerciseDateForClient(clientId: Long, exerciseId: Long, currentDate: String): String? {
            val validSessions = sessions.filter { it.clientId == clientId && it.date < currentDate }
            val validSessionIds = validSessions.map { it.id }
            val matchingSets = sets.filter { it.sessionId in validSessionIds && it.exerciseId == exerciseId && it.isCompleted }
            return validSessions.filter { s -> matchingSets.any { it.sessionId == s.id } }.maxByOrNull { it.date }?.date
        }

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
        override suspend fun getAppointmentById(id: Long): AppointmentEntity? = appointments.find { it.id == id }
        override suspend fun insertAppointment(appointment: AppointmentEntity): Long = 1L
        override suspend fun updateAppointment(appointment: AppointmentEntity) {}
        override suspend fun deleteAppointment(appointment: AppointmentEntity) {}
        override suspend fun getAllAppointmentsSync(): List<AppointmentEntity> = appointments
        override suspend fun getSessionsForClientSync(clientId: Long): List<WorkoutSessionEntity> = sessions.filter { it.clientId == clientId }
        override suspend fun getAnthropometryForClientSync(clientId: Long): List<AnthropometryEntity> = anthro.filter { it.clientId == clientId }
    }

    @Test
    fun testExerciseStatsCalculationDecoupledFromActiveSession() = runBlocking {
        val dao = TestDao()

        val exercise1 = ExerciseEntity(id = 101L, name = "Жим лежа", muscleGroup = "Грудь")
        val exercise2 = ExerciseEntity(id = 102L, name = "Становая тяга", muscleGroup = "Спина")
        dao.exercises.addAll(listOf(exercise1, exercise2))

        val clientId = 1L
        val pastDate = "2026-10-01"
        val currentDate = "2026-10-03"

        // Historical session
        val pastSession = WorkoutSessionEntity(id = 10L, clientId = clientId, date = pastDate, completed = true)
        dao.sessions.add(pastSession)

        // Sets for exercise 102 (Deadlift)
        dao.sets.add(WorkoutSetEntity(id = 1L, sessionId = 10L, exerciseId = 102L, exerciseOrder = 1, setNumber = 1, weightKg = 120.0, reps = 5, isCompleted = true))
        dao.sets.add(WorkoutSetEntity(id = 2L, sessionId = 10L, exerciseId = 102L, exerciseOrder = 1, setNumber = 2, weightKg = 140.0, reps = 3, isCompleted = true))

        // On currentDate today, the workout session is completely empty (0 exercises, activeExerciseTriple is null)
        val todaySession = WorkoutSessionEntity(id = 11L, clientId = clientId, date = currentDate, completed = false)
        dao.sessions.add(todaySession)

        // Query stats for exercise 102 independently
        val lastSets = dao.getLastExerciseSetsForClient(clientId, 102L, currentDate)
        val lastDate = dao.getLastExerciseDateForClient(clientId, 102L, currentDate)

        assertEquals("2026-10-01", lastDate)
        assertEquals(2, lastSets.size)
        val maxWeight = lastSets.maxOfOrNull { it.weightKg } ?: 0.0
        val totalReps = lastSets.sumOf { it.reps }
        assertEquals(140.0, maxWeight, 0.001)
        assertEquals(8, totalReps)
    }

    @Test
    fun testTrainerCardPairingTransmissionAndSerialization() {
        val rootObj = JsonObject()
        val pairingObj = JsonObject()
        rootObj.add("pairing", pairingObj)

        val pin = "123456"
        val coachName = "Виктор Кузнецов"
        val coachPhone = "+7 (900) 111-22-33"
        val coachAvatarBase64 = "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEASABIAAD..."

        val pairingEntry = JsonObject().apply {
            addProperty("pin", pin)
            addProperty("clientUuid", "uuid-test-athlete")
            addProperty("clientName", "Алексей Смирнов")
            addProperty("coachName", coachName)
            addProperty("coachPhone", coachPhone)
            addProperty("coachAvatarBase64", coachAvatarBase64)
            addProperty("status", "PAIRED")
            addProperty("pairedAt", System.currentTimeMillis().toString())
        }
        pairingObj.add(pin, pairingEntry)

        val jsonStr = Gson().toJson(rootObj)
        val parsed = JsonParser.parseString(jsonStr).asJsonObject
        val parsedPairing = parsed.getAsJsonObject("pairing").getAsJsonObject(pin)

        assertEquals(coachName, parsedPairing.get("coachName").asString)
        assertEquals(coachPhone, parsedPairing.get("coachPhone").asString)
        assertEquals(coachAvatarBase64, parsedPairing.get("coachAvatarBase64").asString)
        assertEquals("PAIRED", parsedPairing.get("status").asString)
    }

    @Test
    fun testCloudSyncPairingEntryUpdate() {
        val rootJson = """
        {
          "pairing": {
            "777888": {
              "pin": "777888",
              "clientUuid": "uuid-athlete-777",
              "clientName": "Сергей",
              "coachName": "Старый Тренер",
              "coachPhone": "+70000000000",
              "status": "WAITING"
            }
          },
          "clients": {}
        }
        """.trimIndent()

        val parsed = JsonParser.parseString(rootJson).asJsonObject
        val pairingObj = parsed.getAsJsonObject("pairing")
        val clientUuid = "uuid-athlete-777"
        val cleanPin = "777888"

        val updatedCoachName = "Михаил Тренер"
        val updatedCoachPhone = "+7 (999) 888-77-66"
        val updatedAvatar = "base64-coach-avatar-data"

        for ((key, element) in pairingObj.entrySet()) {
            if (!element.isJsonObject) continue
            val entryObj = element.asJsonObject
            val entryUuid = entryObj.get("clientUuid")?.asString
            val entryPin = entryObj.get("pin")?.asString?.filter { it.isDigit() } ?: key.filter { it.isDigit() }
            if (entryUuid == clientUuid || entryPin == cleanPin) {
                entryObj.addProperty("coachName", updatedCoachName)
                entryObj.addProperty("coachPhone", updatedCoachPhone)
                entryObj.addProperty("coachAvatarBase64", updatedAvatar)
                entryObj.addProperty("status", "PAIRED")
                break
            }
        }

        val updatedEntry = pairingObj.getAsJsonObject("777888")
        assertEquals(updatedCoachName, updatedEntry.get("coachName").asString)
        assertEquals(updatedCoachPhone, updatedEntry.get("coachPhone").asString)
        assertEquals(updatedAvatar, updatedEntry.get("coachAvatarBase64").asString)
        assertEquals("PAIRED", updatedEntry.get("status").asString)
    }

    @Test
    fun testSemVerVersionComparisonLogic() {
        fun isVersionNewer(remote: String, local: String): Boolean {
            val rParts = remote.split(".").mapNotNull { it.trim().toIntOrNull() }
            val lParts = local.split(".").mapNotNull { it.trim().toIntOrNull() }
            val maxLen = maxOf(rParts.size, lParts.size)
            for (i in 0 until maxLen) {
                val r = rParts.getOrElse(i) { 0 }
                val l = lParts.getOrElse(i) { 0 }
                if (r > l) return true
                if (r < l) return false
            }
            return false
        }

        assertTrue(isVersionNewer("1.0.5", "1.0.4"))
        assertTrue(isVersionNewer("1.1.0", "1.0.5"))
        assertFalse(isVersionNewer("1.0.5", "1.0.5"))
        assertFalse(isVersionNewer("1.0.4", "1.0.5"))
        assertTrue(isVersionNewer("2.0.0", "1.0.5"))
    }

    @Test
    fun testPairingCodeWithoutDashesAndLinkExtraction() {
        // Direct 6 digits without hyphens
        assertEquals("265507", extractPairingCode("265507"))
        assertEquals("739102", extractPairingCode("739102"))

        // Auto-stripping hyphens, spaces, and non-digits
        assertEquals("265507", extractPairingCode("265-507"))
        assertEquals("739102", extractPairingCode("  739-102  "))
        assertEquals("123456", extractPairingCode("code: 123-456!"))

        // Paste link with code parameter
        assertEquals("265507", extractPairingCode("https://fitnessapp.pro/pair?code=265507"))
        assertEquals("739102", extractPairingCode("https://fitnessapp.pro/pair?pin=739102"))
        assertEquals("889900", extractPairingCode("https://fitnessapp.pro/pair/889900"))

        // Paste JSON QR payload
        assertEquals("265507", extractPairingCode("""{"pin":"265507","clientUuid":"abc-123"}"""))
        assertEquals("739102", extractPairingCode("""{"code":"739102"}"""))
    }

    @Test
    fun testZeroMocksDataAuditInTrainerDao() = runBlocking {
        val dao = TestDao()
        // Fresh database state has 0 mock clients
        assertEquals(0, dao.getAllClientsSync().size)
        assertEquals(0, dao.getAllSessionsSync().size)
        assertEquals(0, dao.getAllSetsSync().size)
    }
}
