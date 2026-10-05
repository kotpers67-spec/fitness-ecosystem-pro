package com.trainerapp.pro

import com.google.gson.JsonObject
import com.trainerapp.pro.data.local.entities.ClientEntity
import com.trainerapp.pro.data.sync.GoogleDriveSyncManager
import org.junit.Assert.*
import org.junit.Test

class TrainerStrictPairingTest {

    @Test
    fun testPairing_missingCode_throwsIllegalArgumentException() {
        val rootObj = JsonObject().apply {
            add("pairing", JsonObject().apply {
                add("111222", JsonObject().apply {
                    addProperty("pin", "111222")
                    addProperty("clientUuid", "uuid-1")
                    addProperty("timestamp", System.currentTimeMillis())
                    addProperty("status", "PENDING")
                })
            })
        }

        val result = GoogleDriveSyncManager.validateAndProcessPairingData(
            rootObj = rootObj,
            cleanPin = "999888"
        )

        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull()
        assertTrue("Expected IllegalArgumentException but was $ex", ex is IllegalArgumentException)
        assertTrue(ex?.message?.contains("Код не найден") == true)
    }

    @Test
    fun testPairing_expiredCode_throwsIllegalStateException_andPurgesFromCloud() {
        val now = 1728000000000L
        val expiredTimestamp = now - (6 * 60 * 1000L) // 6 minutes old (> 5 min)

        val rootObj = JsonObject().apply {
            add("pairing", JsonObject().apply {
                add("123456", JsonObject().apply {
                    addProperty("pin", "123456")
                    addProperty("clientUuid", "uuid-expired")
                    addProperty("clientName", "Иван Тестов")
                    addProperty("timestamp", expiredTimestamp)
                    addProperty("status", "PENDING")
                })
            })
        }

        val result = GoogleDriveSyncManager.validateAndProcessPairingData(
            rootObj = rootObj,
            cleanPin = "123456",
            now = now
        )

        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull()
        assertTrue("Expected IllegalStateException but was $ex", ex is IllegalStateException)
        assertTrue(ex?.message?.contains("Срок действия кода истёк") == true)

        // Verify the expired entry was purged from the cloud rootObj
        val pairingObj = rootObj.getAsJsonObject("pairing")
        assertFalse("Expired key 123456 should be purged from cloud JSON", pairingObj.has("123456"))
    }

    @Test
    fun testPairing_reusedCode_statusPaired_throwsIllegalStateException() {
        val now = 1728000000000L
        val validTimestamp = now - (2 * 60 * 1000L) // 2 minutes old

        val rootObj = JsonObject().apply {
            add("pairing", JsonObject().apply {
                add("123456", JsonObject().apply {
                    addProperty("pin", "123456")
                    addProperty("clientUuid", "uuid-already-paired")
                    addProperty("clientName", "Иван Тестов")
                    addProperty("timestamp", validTimestamp)
                    addProperty("status", "PAIRED")
                })
            })
        }

        val result = GoogleDriveSyncManager.validateAndProcessPairingData(
            rootObj = rootObj,
            cleanPin = "123456",
            now = now
        )

        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull()
        assertTrue("Expected IllegalStateException but was $ex", ex is IllegalStateException)
        assertTrue(ex?.message?.contains("Этот код уже был использован") == true)
    }

    @Test
    fun testPairing_reusedCode_statusUsed_throwsIllegalStateException() {
        val now = 1728000000000L
        val validTimestamp = now - (2 * 60 * 1000L)

        val rootObj = JsonObject().apply {
            add("pairing", JsonObject().apply {
                add("123456", JsonObject().apply {
                    addProperty("pin", "123456")
                    addProperty("clientUuid", "uuid-already-used")
                    addProperty("clientName", "Иван Тестов")
                    addProperty("timestamp", validTimestamp)
                    addProperty("status", "USED")
                })
            })
        }

        val result = GoogleDriveSyncManager.validateAndProcessPairingData(
            rootObj = rootObj,
            cleanPin = "123456",
            now = now
        )

        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull()
        assertTrue(ex is IllegalStateException)
        assertTrue(ex?.message?.contains("Этот код уже был использован") == true)
    }

    @Test
    fun testPairing_validCode_success_setsPairedStatus_andReturnsClientEntity() {
        val now = 1728000000000L
        val validTimestamp = now - (60 * 1000L) // 1 minute old

        val rootObj = JsonObject().apply {
            add("pairing", JsonObject().apply {
                add("654321", JsonObject().apply {
                    addProperty("pin", "654321")
                    addProperty("clientUuid", "uuid-valid-athlete")
                    addProperty("clientName", "Атлет Чемпион")
                    addProperty("phone", "+79998887766")
                    addProperty("goal", "Набор массы")
                    addProperty("notes", "Без травм")
                    addProperty("avatarBase64", "base64-avatar-athlete-data")
                    addProperty("timestamp", validTimestamp)
                    addProperty("status", "PENDING")
                })
            })
        }

        val result = GoogleDriveSyncManager.validateAndProcessPairingData(
            rootObj = rootObj,
            cleanPin = "654321",
            coachName = "Тренер Тест",
            coachPhone = "+70001112233",
            now = now
        )

        assertTrue(result.isSuccess)
        val pair = result.getOrThrow()
        val client: ClientEntity = pair.first
        val updatedRoot: JsonObject = pair.second

        // Verify client entity fields
        assertEquals("uuid-valid-athlete", client.clientUuid)
        assertEquals("Атлет Чемпион", client.fullName)
        assertEquals("+79998887766", client.phone)
        assertEquals("Набор массы", client.goal)
        assertEquals("654321", client.pairingCode)
        assertEquals("base64-avatar-athlete-data", client.avatarBase64)

        // Verify updated root object
        val entry = updatedRoot.getAsJsonObject("pairing").getAsJsonObject("654321")
        assertEquals("PAIRED", entry.get("status").asString)
        assertEquals("Тренер Тест", entry.get("coachName").asString)
        assertEquals("+70001112233", entry.get("coachPhone").asString)
    }

    @Test
    fun testPairing_withRestrictionsInPairingEntry_extractsRestrictionsIntoNotes() {
        val now = 1728000000000L
        val validTimestamp = now - (60 * 1000L)

        val rootObj = JsonObject().apply {
            add("pairing", JsonObject().apply {
                add("333444", JsonObject().apply {
                    addProperty("pin", "333444")
                    addProperty("clientUuid", "uuid-restricted-athlete")
                    addProperty("clientName", "Травмированный Атлет")
                    addProperty("phone", "+79991112233")
                    addProperty("goal", "Реабилитация")
                    addProperty("restrictions", "Травма колена, избегать приседаний")
                    addProperty("timestamp", validTimestamp)
                    addProperty("status", "PENDING")
                })
            })
        }

        val result = GoogleDriveSyncManager.validateAndProcessPairingData(
            rootObj = rootObj,
            cleanPin = "333444",
            coachName = "Тренер Тест",
            coachPhone = "+70001112233",
            now = now
        )

        assertTrue(result.isSuccess)
        val client: ClientEntity = result.getOrThrow().first
        assertEquals("Травма колена, избегать приседаний", client.notes)
        assertEquals("+79991112233", client.phone)
        assertEquals("Травмированный Атлет", client.fullName)
    }

    @Test
    fun testPairing_withRestrictionsInClientsMap_extractsRestrictionsIntoNotes() {
        val now = 1728000000000L
        val validTimestamp = now - (60 * 1000L)

        val rootObj = JsonObject().apply {
            add("pairing", JsonObject().apply {
                add("555666", JsonObject().apply {
                    addProperty("pin", "555666")
                    addProperty("clientUuid", "uuid-athlete-clients-map")
                    addProperty("clientName", "Атлет из реестра")
                    addProperty("timestamp", validTimestamp)
                    addProperty("status", "PENDING")
                })
            })
            add("clients", JsonObject().apply {
                add("uuid-athlete-clients-map", JsonObject().apply {
                    addProperty("phone", "+79994445566")
                    addProperty("goal", "Выносливость")
                    addProperty("restrictions", "Грыжа поясничного отдела")
                    addProperty("avatarBase64", "client-avatar-data")
                })
            })
        }

        val result = GoogleDriveSyncManager.validateAndProcessPairingData(
            rootObj = rootObj,
            cleanPin = "555666",
            coachName = "Тренер Тест",
            coachPhone = "+70001112233",
            now = now
        )

        assertTrue(result.isSuccess)
        val client: ClientEntity = result.getOrThrow().first
        assertEquals("Грыжа поясничного отдела", client.notes)
        assertEquals("+79994445566", client.phone)
        assertEquals("Выносливость", client.goal)
        assertEquals("client-avatar-data", client.avatarBase64)
    }
}
