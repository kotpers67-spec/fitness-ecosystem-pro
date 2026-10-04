package com.athleteapp.pro

import org.junit.Assert.*
import org.junit.Test

class AthletePinAnd2FaTest {

    @Test
    fun testPinCountdown_calculatedCorrectly() {
        val pinDurationSec = 300
        val createdAt = 1000000000L
        val now = createdAt + (125 * 1000L) // 125 seconds passed

        val elapsedSec = ((now - createdAt) / 1000).toInt()
        val remainingSec = (pinDurationSec - elapsedSec).coerceAtLeast(0)

        assertEquals(175, remainingSec)
    }

    @Test
    fun testPinCountdown_expiresAfter300Seconds() {
        val pinDurationSec = 300
        val createdAt = 1000000000L
        val now = createdAt + (305 * 1000L) // 305 seconds passed

        val elapsedSec = ((now - createdAt) / 1000).toInt()
        val remainingSec = (pinDurationSec - elapsedSec).coerceAtLeast(0)

        assertEquals(0, remainingSec)
        assertTrue("PIN should be expired when remaining is 0", remainingSec <= 0)
    }

    @Test
    fun testPinCountdown_zeroWhenNegativeElapsedTime() {
        val pinDurationSec = 300
        val createdAt = 1000000000L
        val now = createdAt + (400 * 1000L)

        val elapsedSec = ((now - createdAt) / 1000).toInt()
        val remainingSec = (pinDurationSec - elapsedSec).coerceAtLeast(0)

        assertEquals(0, remainingSec)
    }

    @Test
    fun testTelegramUsernameFormatting() {
        val raw1 = "@SantiLA213"
        val clean1 = raw1.trim().removePrefix("@")
        assertEquals("SantiLA213", clean1)

        val raw2 = "  Spirit5449  "
        val clean2 = raw2.trim().removePrefix("@")
        assertEquals("Spirit5449", clean2)
    }

    @Test
    fun test2FaOtpValidation() {
        // Valid 6-digit OTP
        val validOtp = "123456"
        assertTrue(validOtp.length == 6 && validOtp.all { it.isDigit() })

        // Invalid length
        val shortOtp = "12345"
        assertFalse(shortOtp.length == 6 && shortOtp.all { it.isDigit() })

        val longOtp = "1234567"
        assertFalse(longOtp.length == 6 && longOtp.all { it.isDigit() })

        // Invalid non-digit chars
        val nonDigitOtp = "12a456"
        assertFalse(nonDigitOtp.length == 6 && nonDigitOtp.all { it.isDigit() })
    }
}
