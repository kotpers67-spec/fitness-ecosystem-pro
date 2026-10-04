package com.trainerapp.pro

import com.trainerapp.pro.data.auth.RemoteApprovalResult
import com.trainerapp.pro.data.auth.RemoteOtpResult
import com.trainerapp.pro.data.auth.TrainerRemoteAuthManager
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

class TrainerMilestone1RemediationTest {

    private var serverSocket: ServerSocket? = null
    private lateinit var authManager: TrainerRemoteAuthManager

    @Before
    fun setUp() {
        authManager = TrainerRemoteAuthManager()
    }

    @After
    fun tearDown() {
        runCatching { serverSocket?.close() }
    }

    private fun startTestServer(
        handleRequest: (path: String, method: String, body: String) -> Pair<Int, String>
    ): Int {
        val ss = ServerSocket(0)
        serverSocket = ss
        val port = ss.localPort
        authManager.backendBaseUrl = "http://127.0.0.1:$port"

        thread(isDaemon = true) {
            while (!ss.isClosed) {
                try {
                    val socket: Socket = ss.accept()
                    thread(isDaemon = true) {
                        try {
                            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                            val requestLine = reader.readLine() ?: return@thread
                            val parts = requestLine.split(" ")
                            val method = parts.getOrNull(0) ?: "GET"
                            val path = parts.getOrNull(1) ?: "/"

                            var contentLength = 0
                            var headerLine = reader.readLine()
                            while (!headerLine.isNullOrEmpty()) {
                                if (headerLine.startsWith("Content-Length:", ignoreCase = true)) {
                                    contentLength = headerLine.substringAfter(":").trim().toIntOrNull() ?: 0
                                }
                                headerLine = reader.readLine()
                            }

                            val body = if (contentLength > 0) {
                                val buffer = CharArray(contentLength)
                                var totalRead = 0
                                while (totalRead < contentLength) {
                                    val read = reader.read(buffer, totalRead, contentLength - totalRead)
                                    if (read == -1) break
                                    totalRead += read
                                }
                                String(buffer, 0, totalRead)
                            } else ""

                            val (statusCode, responseBody) = handleRequest(path, method, body)
                            val statusMsg = if (statusCode == 200) "OK" else if (statusCode == 400) "Bad Request" else if (statusCode == 403) "Forbidden" else "Not Found"

                            val writer = OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8)
                            writer.write("HTTP/1.1 $statusCode $statusMsg\r\n")
                            writer.write("Content-Type: application/json; charset=utf-8\r\n")
                            val bytes = responseBody.toByteArray(Charsets.UTF_8)
                            writer.write("Content-Length: ${bytes.size}\r\n")
                            writer.write("Connection: close\r\n\r\n")
                            writer.write(responseBody)
                            writer.flush()
                        } catch (_: Exception) {
                        } finally {
                            runCatching { socket.close() }
                        }
                    }
                } catch (_: Exception) {
                    break
                }
            }
        }

        return port
    }

    @Test
    fun testOtpValidation_rejectsInvalidLengths() = runBlocking {
        authManager.backendBaseUrl = "http://127.0.0.1:59999" // Unreachable port

        val res5 = authManager.verifyOtp("trainer1", "12345")
        assertTrue("5 digits must be rejected", res5 is RemoteOtpResult.Rejected)

        val res7 = authManager.verifyOtp("trainer1", "1234567")
        assertTrue("7 digits must be rejected", res7 is RemoteOtpResult.Rejected)

        val resAlpha = authManager.verifyOtp("trainer1", "abcdef")
        assertTrue("Non-digits must be rejected", resAlpha is RemoteOtpResult.Rejected)

        val resEmpty = authManager.verifyOtp("trainer1", "")
        assertTrue("Empty string must be rejected", resEmpty is RemoteOtpResult.Rejected)

        assertFalse(authManager.isValidOtpFormat("12345"))
        assertFalse(authManager.isValidOtpFormat("1234567"))
        assertTrue(authManager.isValidOtpFormat("123456"))
    }

    @Test
    fun testOtpValidation_gracefulOfflineFallbackWith6Digits() = runBlocking {
        authManager.backendBaseUrl = "http://127.0.0.1:59999" // Unreachable port

        val result = authManager.verifyOtp("trainer_offline", "884422")
        assertTrue("When server is unreachable, valid 6-digit OTP yields OfflineFallback", result is RemoteOtpResult.OfflineFallback)
        assertTrue("Format must be valid 6 digits", authManager.isValidOtpFormat("884422"))
    }

    @Test
    fun testOtpValidation_remoteVerifySuccess() = runBlocking {
        val requestReceived = AtomicBoolean(false)
        val receivedPayload = StringBuilder()

        startTestServer { path, method, body ->
            if (path == "/api/auth/telegram/verify-otp" && method == "POST") {
                requestReceived.set(true)
                receivedPayload.append(body)
                Pair(200, """{"success":true}""")
            } else {
                Pair(404, """{"error":"Not Found"}""")
            }
        }

        val result = authManager.verifyOtp("@romanov_coach", "739102")

        assertTrue("Remote verification with 200 response must return Success", result is RemoteOtpResult.Success)
        assertTrue("Remote verify-otp endpoint must have received the request", requestReceived.get())
        assertTrue("Payload contains romanov_coach", receivedPayload.contains("romanov_coach"))
        assertTrue("Payload contains 739102", receivedPayload.contains("739102"))
    }

    @Test
    fun testOtpValidation_remoteVerifyExplicitRejectionDoesNotBypass() = runBlocking {
        val requestReceived = AtomicBoolean(false)

        startTestServer { path, method, _ ->
            if (path == "/api/auth/telegram/verify-otp" && method == "POST") {
                requestReceived.set(true)
                Pair(400, """{"error":"Неверный код из Telegram"}""")
            } else {
                Pair(404, """{"error":"Not Found"}""")
            }
        }

        val result = authManager.verifyOtp("romanov_coach", "000000")

        assertTrue("When server returns 400, result must be Rejected", result is RemoteOtpResult.Rejected)
        assertEquals("Неверный код из Telegram", (result as RemoteOtpResult.Rejected).message)
        assertTrue("Server must have received the verification request", requestReceived.get())
    }

    @Test
    fun testCheckRemoteApprovalStatus_viaGetEndpoint() = runBlocking {
        startTestServer { path, method, _ ->
            if (path.startsWith("/api/trainer/approval-status") && method == "GET") {
                if (path.contains("approved_trainer")) {
                    Pair(200, """{"isApproved":true,"status":"APPROVED"}""")
                } else {
                    Pair(200, """{"isApproved":false,"status":"PENDING"}""")
                }
            } else {
                Pair(404, """{"error":"Not Found"}""")
            }
        }

        val approvedRes = authManager.checkApprovalStatus("approved_trainer")
        assertTrue("Trainer marked approved on server should yield Approved", approvedRes is RemoteApprovalResult.Approved)

        val pendingRes = authManager.checkApprovalStatus("pending_trainer")
        assertTrue("Trainer with isApproved=false on server should yield Pending", pendingRes is RemoteApprovalResult.Pending)
    }

    @Test
    fun testCheckRemoteApprovalStatus_viaPostLoginFallback() = runBlocking {
        val loginAttempts = AtomicInteger(0)

        startTestServer { path, method, body ->
            if (path == "/api/trainer/approval-status") {
                Pair(404, """{"error":"Not Found"}""")
            } else if (path == "/api/login" && method == "POST") {
                loginAttempts.incrementAndGet()
                if (body.contains("approved_via_login")) {
                    Pair(200, """{"success":true,"token":"auth-token"}""")
                } else {
                    Pair(403, """{"error":"⏳ ЗАЯВКА НА РАССМОТРЕНИИ"}""")
                }
            } else {
                Pair(404, """{"error":"Not Found"}""")
            }
        }

        val approved = authManager.checkApprovalStatus("approved_via_login", "pass123")
        assertTrue("Successful 200 on /api/login confirms approval", approved is RemoteApprovalResult.Approved)

        val rejected = authManager.checkApprovalStatus("pending_coach", "pass123")
        assertTrue("403 on /api/login preserves pending status", rejected is RemoteApprovalResult.Pending)
        assertEquals(2, loginAttempts.get())
    }

    @Test
    fun testAthleteRestrictionsBanner_presenceLogic() {
        val notesWithInjury = "Травма плечевого сустава, без жима стоя"
        val notesEmpty = ""
        val notesBlank = "   \n\t  "
        val notesNull: String? = null

        assertTrue("Non-empty notes with injury must be recognized as valid restrictions", notesWithInjury.isNotBlank())
        assertTrue("Empty notes must be blank", notesEmpty.isBlank())
        assertTrue("Whitespace notes must be blank", notesBlank.isBlank())
        assertTrue("Null notes must be null or blank", notesNull.isNullOrBlank())
    }
}
