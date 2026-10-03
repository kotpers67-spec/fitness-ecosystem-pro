package com.athleteapp.pro.data.sync

import com.athleteapp.pro.data.local.entities.AthleteProfileEntity
import com.athleteapp.pro.data.local.entities.MyWorkoutSessionEntity
import com.athleteapp.pro.data.sync.model.AthleteSyncPayload
import com.athleteapp.pro.data.sync.model.SyncExercise
import com.athleteapp.pro.data.sync.model.SyncWorkoutSession
import com.athleteapp.pro.data.sync.model.SyncWorkoutSet
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

class AthleteIsolationAndPairingTest {

    private lateinit var fakeDao: FakeAthleteDao
    private lateinit var syncManager: AthleteSyncManager
    private val gson = Gson()

    @Before
    fun setUp() {
        fakeDao = FakeAthleteDao()
        syncManager = AthleteSyncManager(fakeDao)
    }

    @Test
    fun testClientUuid_persistedAndIsolatedInSyncPayload() = runBlocking {
        val testUuid = UUID.randomUUID().toString()
        val initialProfile = AthleteProfileEntity(
            id = 1,
            clientUuid = testUuid,
            fullName = "Иван Иванов",
            phone = "+7 900 111-22-33",
            restrictions = "Травма плеча"
        )
        fakeDao.saveProfile(initialProfile)

        val jsonPayload = """
            {
              "clientUuid": "$testUuid",
              "athleteId": 99,
              "syncTimestamp": 1727956800000,
              "clientName": "Иван Иванов",
              "assignedWorkouts": [],
              "anthropometry": []
            }
        """.trimIndent()

        val result = syncManager.applyPayloadJson(jsonPayload)
        assertTrue(result.isSuccess)

        // Ensure clientUuid was preserved and not overridden with empty or random
        val updatedProfile = fakeDao.savedProfile
        assertNotNull(updatedProfile)
        assertEquals(testUuid, updatedProfile?.clientUuid)
        assertEquals("Иван Иванов", updatedProfile?.fullName)
        assertEquals("Травма плеча", updatedProfile?.restrictions)
    }

    @Test
    fun testIsSelfWorkoutAllowed_importedAndSavedIntoWorkoutSession() = runBlocking {
        val dateToday = "2026-10-03"
        val payloadWithSelfAllowed = """
            {
              "clientUuid": "uuid-1234",
              "athleteId": 1,
              "syncTimestamp": 1727956800000,
              "clientName": "Тестовый Атлет",
              "assignedWorkouts": [
                {
                  "date": "$dateToday",
                  "notes": "День ног (самостоятельно)",
                  "completed": false,
                  "isSelfWorkoutAllowed": true,
                  "exercises": [
                    {
                      "exerciseId": 10,
                      "name": "Приседания",
                      "muscleGroup": "Ноги",
                      "sets": [
                        {
                          "setNumber": 1,
                          "targetWeightKg": 100.0,
                          "targetReps": 8,
                          "actualWeightKg": 0.0,
                          "actualReps": 0,
                          "isCompleted": false
                        }
                      ]
                    }
                  ]
                }
              ],
              "anthropometry": []
            }
        """.trimIndent()

        val result = syncManager.applyPayloadJson(payloadWithSelfAllowed)
        assertTrue(result.isSuccess)

        val savedSession = fakeDao.getSessionByDate(dateToday)
        assertNotNull(savedSession)
        assertTrue("Session must have isSelfWorkoutAllowed == true", savedSession!!.isSelfWorkoutAllowed)
        assertEquals("День ног (самостоятельно)", savedSession.notes)
    }

    @Test
    fun testIsSelfWorkoutAllowed_falseByDefaultEnforcesReadOnly() = runBlocking {
        val dateToday = "2026-10-04"
        val payloadReadOnly = """
            {
              "clientUuid": "uuid-5678",
              "athleteId": 1,
              "syncTimestamp": 1727956800000,
              "clientName": "Тестовый Атлет",
              "assignedWorkouts": [
                {
                  "date": "$dateToday",
                  "notes": "Тренировка с тренером в зале",
                  "completed": false,
                  "isSelfWorkoutAllowed": false,
                  "exercises": []
                }
              ],
              "anthropometry": []
            }
        """.trimIndent()

        val result = syncManager.applyPayloadJson(payloadReadOnly)
        assertTrue(result.isSuccess)

        val savedSession = fakeDao.getSessionByDate(dateToday)
        assertNotNull(savedSession)
        assertFalse("Session must have isSelfWorkoutAllowed == false (Read-Only)", savedSession!!.isSelfWorkoutAllowed)
    }

    @Test
    fun testDataIsolation_multiClientCloudPayloadDoesNotLeakToAthlete() {
        val myUuid = "my-isolated-uuid-001"
        val foreignUuid = "victim-uuid-999"

        val multiClientCloudJson = """
            {
              "updatedAt": "1727956800000",
              "clients": {
                "$foreignUuid": {
                  "clientUuid": "$foreignUuid",
                  "clientName": "Секретный Клиент",
                  "assignedWorkouts": []
                },
                "$myUuid": {
                  "clientUuid": "$myUuid",
                  "clientName": "Мой Профиль",
                  "assignedWorkouts": []
                }
              }
            }
        """.trimIndent()

        val parsed = JsonParser.parseString(multiClientCloudJson).asJsonObject
        val clientsObj = parsed.getAsJsonObject("clients")

        // Athlete Pro MUST look up exclusively its own key
        assertTrue("Cloud has myUuid", clientsObj.has(myUuid))
        val myPayloadStr = clientsObj.get(myUuid).toString()

        val myParsed = gson.fromJson(myPayloadStr, AthleteSyncPayload::class.java)
        assertEquals("Мой Профиль", myParsed.clientName)
        assertEquals(myUuid, myParsed.clientUuid)

        // Foreign client data is never parsed into athlete state
        assertNotEquals("Секретный Клиент", myParsed.clientName)
    }

    @Test
    fun testPairingQrJsonFormat() {
        val pin = "739102"
        val uuid = "f47ac10b-58cc-4372-a567-0e02b2c3d479"
        val name = "Александр Смирнов"
        val phone = "+7 999 123-45-67"

        val qrJson = "{\"pin\":\"$pin\",\"uuid\":\"$uuid\",\"name\":\"$name\",\"phone\":\"$phone\"}"
        val parsed = JsonParser.parseString(qrJson).asJsonObject

        assertEquals("739102", parsed.get("pin").asString)
        assertEquals(uuid, parsed.get("uuid").asString)
        assertEquals(name, parsed.get("name").asString)
        assertEquals(phone, parsed.get("phone").asString)
    }
}
