package com.athleteapp.pro.data.sync

import com.athleteapp.pro.data.local.dao.AthleteDao
import com.athleteapp.pro.data.local.dao.AthleteSetHistory
import com.athleteapp.pro.data.local.entities.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AthleteSyncRemediationTest {

    private lateinit var fakeDao: FakeAthleteDao
    private lateinit var syncManager: AthleteSyncManager

    @Before
    fun setUp() {
        fakeDao = FakeAthleteDao()
        syncManager = AthleteSyncManager(fakeDao)
    }

    @Test
    fun testCorruptedJson_returnsFailureExplicitly() = runBlocking {
        val corruptedJson = "<html><body>502 Bad Gateway</body></html>"
        val result = syncManager.applyPayloadJson(corruptedJson)

        assertTrue("Corrupted JSON must return Result.failure", result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertEquals("Не удалось разобрать план тренировок", exception?.message)
    }

    @Test
    fun testMalformedJson_returnsFailureExplicitly() = runBlocking {
        val malformedJson = "{ unclosed json "
        val result = syncManager.applyPayloadJson(malformedJson)

        assertTrue("Malformed JSON must return Result.failure", result.isFailure)
        assertEquals("Не удалось разобрать план тренировок", result.exceptionOrNull()?.message)
    }

    @Test
    fun testAnthropometryAndNameImported_whenAssignedWorkoutsEmpty() = runBlocking {
        val json = """
            {
              "athleteId": 42,
              "syncTimestamp": 1727956800000,
              "clientName": "Алексей Смирнов",
              "assignedWorkouts": [],
              "anthropometry": [
                {
                  "date": "2026-10-03",
                  "weightKg": 82.5,
                  "chestCm": 105.0,
                  "waistCm": 84.0,
                  "hipsCm": 98.0,
                  "bicepsCm": 39.5
                }
              ]
            }
        """.trimIndent()

        val result = syncManager.applyPayloadJson(json)
        assertTrue("Sync must succeed even with empty workouts", result.isSuccess)
        assertEquals(0, result.getOrNull())

        // Verify profile name updated
        assertEquals("Алексей Смирнов", fakeDao.savedProfile?.fullName)
        assertEquals(42L, fakeDao.savedProfile?.athleteIdInCoachBase)

        // Verify anthropometry imported
        assertEquals(1, fakeDao.anthropometryList.size)
        val anth = fakeDao.anthropometryList.first()
        assertEquals("2026-10-03", anth.date)
        assertEquals(82.5, anth.weightKg, 0.01)
        assertEquals(105.0, anth.chestCm!!, 0.01)
    }

    @Test
    fun testExistingSessionUpdate_usesUpdateSessionToPreventCascadeDeletion() = runBlocking {
        // Pre-populate an existing session with notes = "" and 3 child sets
        val existingSession = MyWorkoutSessionEntity(
            id = 100L,
            date = "2026-10-03",
            notes = "",
            completed = false
        )
        fakeDao.sessions[existingSession.date] = existingSession
        val existingSet = MyWorkoutSetEntity(
            id = 1,
            sessionId = 100L,
            exerciseId = 1,
            exerciseName = "Жим лежа",
            muscleGroup = "Грудь",
            exerciseOrder = 1,
            setNumber = 1,
            targetWeightKg = 80.0,
            targetReps = 10,
            actualWeightKg = 80.0,
            actualReps = 10,
            isCompleted = true,
            rpe = 7.5
        )
        fakeDao.setsForSession.getOrPut(100L) { mutableListOf() }.add(existingSet)

        val json = """
            {
              "athleteId": 1,
              "syncTimestamp": 1727956800000,
              "clientName": "Тест",
              "assignedWorkouts": [
                {
                  "date": "2026-10-03",
                  "notes": "Увеличить рабочий вес на 2.5 кг",
                  "completed": false,
                  "exercises": [
                    {
                      "exerciseId": 1,
                      "name": "Жим лежа",
                      "muscleGroup": "Грудь",
                      "sets": [
                        {
                          "setNumber": 1,
                          "targetWeightKg": 82.5,
                          "targetReps": 10,
                          "actualWeightKg": 0.0,
                          "actualReps": 0,
                          "isCompleted": false,
                          "rpe": null
                        }
                      ]
                    }
                  ]
                }
              ],
              "anthropometry": []
            }
        """.trimIndent()

        val result = syncManager.applyPayloadJson(json)
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull())

        // Verify updateSession was called, NOT insertSession
        assertEquals(1, fakeDao.updateSessionCallCount)
        assertEquals(0, fakeDao.insertSessionCallCount)
        assertEquals("Увеличить рабочий вес на 2.5 кг", fakeDao.sessions["2026-10-03"]?.notes)

        // Verify child sets were NOT cascade deleted and retained completed actuals
        val sets = fakeDao.getSetsForSessionSync(100L)
        assertEquals(1, sets.size)
        assertEquals(80.0, sets[0].actualWeightKg, 0.01)
        assertEquals(10, sets[0].actualReps)
        assertTrue(sets[0].isCompleted)
        assertEquals(82.5, sets[0].targetWeightKg, 0.01) // Target updated
    }
}

/**
 * In-memory test double for AthleteDao.
 */
class FakeAthleteDao : AthleteDao {
    var savedProfile: AthleteProfileEntity? = null
    val exercises = mutableListOf<AssignedExerciseEntity>()
    val sessions = mutableMapOf<String, MyWorkoutSessionEntity>()
    val setsForSession = mutableMapOf<Long, MutableList<MyWorkoutSetEntity>>()
    val anthropometryList = mutableListOf<MyAnthropometryEntity>()
    var insertSessionCallCount = 0
    var updateSessionCallCount = 0

    override fun getProfile(): Flow<AthleteProfileEntity?> = flowOf(savedProfile)

    override suspend fun saveProfile(profile: AthleteProfileEntity) {
        savedProfile = profile
    }

    override fun getAllExercises(): Flow<List<AssignedExerciseEntity>> = flowOf(exercises)

    override suspend fun insertExercise(exercise: AssignedExerciseEntity): Long {
        val id = (exercises.size + 1).toLong()
        val item = exercise.copy(id = id)
        exercises.add(item)
        return id
    }

    override suspend fun insertExercises(exercises: List<AssignedExerciseEntity>): List<Long> {
        return exercises.map { insertExercise(it) }
    }

    override suspend fun getSessionByDate(date: String): MyWorkoutSessionEntity? {
        return sessions[date]
    }

    override fun getAllSessions(): Flow<List<MyWorkoutSessionEntity>> = flowOf(sessions.values.toList())

    override suspend fun insertSession(session: MyWorkoutSessionEntity): Long {
        insertSessionCallCount++
        val id = if (session.id > 0) session.id else (sessions.size + 1).toLong()
        val s = session.copy(id = id)
        sessions[session.date] = s
        return id
    }

    override suspend fun updateSession(session: MyWorkoutSessionEntity) {
        updateSessionCallCount++
        sessions[session.date] = session
    }

    override fun getSetsForSession(sessionId: Long): Flow<List<MyWorkoutSetEntity>> {
        return flowOf(setsForSession[sessionId] ?: emptyList())
    }

    override suspend fun getSetsForSessionSync(sessionId: Long): List<MyWorkoutSetEntity> {
        return setsForSession[sessionId] ?: emptyList()
    }

    override suspend fun insertSet(workoutSet: MyWorkoutSetEntity): Long {
        val list = setsForSession.getOrPut(workoutSet.sessionId) { mutableListOf() }
        val id = (list.size + 1).toLong()
        val item = workoutSet.copy(id = id)
        list.add(item)
        return id
    }

    override suspend fun insertSets(sets: List<MyWorkoutSetEntity>) {
        sets.forEach { insertSet(it) }
    }

    override suspend fun updateSet(workoutSet: MyWorkoutSetEntity) {
        val list = setsForSession[workoutSet.sessionId] ?: return
        val idx = list.indexOfFirst { it.id == workoutSet.id || (it.exerciseId == workoutSet.exerciseId && it.setNumber == workoutSet.setNumber) }
        if (idx >= 0) {
            list[idx] = workoutSet
        }
    }

    override suspend fun deleteSet(workoutSet: MyWorkoutSetEntity) {
        setsForSession[workoutSet.sessionId]?.removeAll { it.id == workoutSet.id }
    }

    override suspend fun deleteSetsForSession(sessionId: Long) {
        setsForSession.remove(sessionId)
    }

    override suspend fun getRecentCompletedSets(): List<MyWorkoutSetEntity> = emptyList()

    override suspend fun getCompletedSetsForExercise(exerciseId: Long): List<MyWorkoutSetEntity> = emptyList()

    override suspend fun getAllSetsSync(): List<MyWorkoutSetEntity> = setsForSession.values.flatten()

    override suspend fun getAllSessionsSync(): List<MyWorkoutSessionEntity> = sessions.values.toList()

    override suspend fun getAllAnthropometrySync(): List<MyAnthropometryEntity> = anthropometryList.toList()

    override suspend fun getAllExercisesSync(): List<AssignedExerciseEntity> = exercises.toList()

    override fun getAllAnthropometry(): Flow<List<MyAnthropometryEntity>> = flowOf(anthropometryList)

    override suspend fun insertAnthropometry(entry: MyAnthropometryEntity): Long {
        val id = (anthropometryList.size + 1).toLong()
        val item = entry.copy(id = id)
        anthropometryList.add(item)
        return id
    }

    override fun getExerciseHistory(exerciseId: Long): Flow<List<AthleteSetHistory>> = flowOf(emptyList())

    override fun getSettings(): Flow<AthleteAppSettingsEntity?> = flowOf(null)

    override suspend fun saveSettings(settings: AthleteAppSettingsEntity) {}
}
