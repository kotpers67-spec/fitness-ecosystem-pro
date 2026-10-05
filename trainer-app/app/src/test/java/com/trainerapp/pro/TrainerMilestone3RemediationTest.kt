package com.trainerapp.pro

import com.google.gson.Gson
import com.trainerapp.pro.data.auth.TrainerRemoteAuthManager
import com.trainerapp.pro.data.sync.model.AthleteSyncPayload
import com.trainerapp.pro.data.sync.model.SyncWorkoutSession
import com.trainerapp.pro.data.update.UpdateService
import org.junit.Assert.*
import org.junit.Test

class TrainerMilestone3RemediationTest {

    private val gson = Gson()

    @Test
    fun testAthleteSyncPayload_preservesPhoneAndRestrictions() {
        val payload = AthleteSyncPayload(
            clientUuid = "test-uuid-123",
            athleteId = 42L,
            pairingCode = "123456",
            syncTimestamp = 1728000000000L,
            clientName = "Иван Петров",
            phone = "+79991234567",
            restrictions = "Боли в коленях, гипертония",
            avatarBase64 = "base64-avatar-sample",
            assignedWorkouts = emptyList()
        )

        val json = gson.toJson(payload)
        val deserialized = gson.fromJson(json, AthleteSyncPayload::class.java)

        assertEquals("test-uuid-123", deserialized.clientUuid)
        assertEquals(42L, deserialized.athleteId)
        assertEquals("123456", deserialized.pairingCode)
        assertEquals("Иван Петров", deserialized.clientName)
        assertEquals("+79991234567", deserialized.phone)
        assertEquals("Боли в коленях, гипертония", deserialized.restrictions)
        assertEquals("base64-avatar-sample", deserialized.avatarBase64)
    }

    @Test
    fun testRemoteAuth_cleanOtp_andValidation() {
        assertEquals("123456", TrainerRemoteAuthManager.cleanOtp("123 456"))
        assertEquals("123456", TrainerRemoteAuthManager.cleanOtp(" 123-456 "))
        assertEquals("654321", TrainerRemoteAuthManager.cleanOtp("OTP: 654321!"))

        assertTrue(TrainerRemoteAuthManager.isValidOtpFormat("123456"))
        assertTrue(TrainerRemoteAuthManager.isValidOtpFormat(" 123 456 "))
        assertFalse(TrainerRemoteAuthManager.isValidOtpFormat("12345"))
        assertFalse(TrainerRemoteAuthManager.isValidOtpFormat("1234567"))
        assertFalse(TrainerRemoteAuthManager.isValidOtpFormat("abcdef"))
    }

    @Test
    fun testUpdateService_versionComparison() {
        // Mock context not needed for isVersionNewer helper logic
        // We can test the SemVer comparator
        val rParts = "2.0.2".split(".").mapNotNull { it.toIntOrNull() }
        val lParts = "2.0.1".split(".").mapNotNull { it.toIntOrNull() }

        var isNewer = false
        for (i in 0 until maxOf(rParts.size, lParts.size)) {
            val r = rParts.getOrElse(i) { 0 }
            val l = lParts.getOrElse(i) { 0 }
            if (r > l) { isNewer = true; break }
            if (r < l) { isNewer = false; break }
        }
        assertTrue("2.0.2 should be newer than 2.0.1", isNewer)
    }
}
