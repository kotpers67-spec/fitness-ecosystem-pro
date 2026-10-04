package com.trainerapp.pro.data.auth

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

sealed class RemoteOtpResult {
    object Success : RemoteOtpResult()
    data class Rejected(val message: String) : RemoteOtpResult()
    object OfflineFallback : RemoteOtpResult()
}

sealed class RemoteApprovalResult {
    object Approved : RemoteApprovalResult()
    object Pending : RemoteApprovalResult()
    object Unreachable : RemoteApprovalResult()
}

/**
 * Dedicated auth manager handling remote 2FA OTP verification and 72h registration approval checks
 * against backend endpoints with graceful offline fallback.
 */
class TrainerRemoteAuthManager(
    var backendBaseUrl: String = "https://fitness-ecosystem-pro.onrender.com"
) {
    private val gson = Gson()

    fun cleanOtp(otp: String): String = otp.filter { it.isDigit() }

    fun isValidOtpFormat(otp: String): Boolean = cleanOtp(otp).length == 6

    suspend fun verifyOtp(username: String, otp: String): RemoteOtpResult = withContext(Dispatchers.IO) {
        val clean = cleanOtp(otp)
        if (clean.length != 6) {
            return@withContext RemoteOtpResult.Rejected("Код должен содержать ровно 6 цифр")
        }

        try {
            val url = URL("$backendBaseUrl/api/auth/telegram/verify-otp")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; utf-8")
            conn.doOutput = true
            conn.connectTimeout = 4000
            conn.readTimeout = 4000

            val payload = JsonObject().apply {
                addProperty("username", username.trim().removePrefix("@"))
                addProperty("code", clean)
            }
            conn.outputStream.use { os ->
                os.write(gson.toJson(payload).toByteArray(Charsets.UTF_8))
            }

            val code = conn.responseCode
            if (code in 200..299) {
                val body = conn.inputStream.bufferedReader().readText()
                val json = JsonParser.parseString(body).asJsonObject
                if (json.has("success") && json.get("success").asBoolean) {
                    return@withContext RemoteOtpResult.Success
                }
            } else if (code in 400..499) {
                val errorBody = conn.errorStream?.bufferedReader()?.readText() ?: ""
                val errJson = runCatching { JsonParser.parseString(errorBody).asJsonObject }.getOrNull()
                val errMsg = errJson?.get("error")?.asString ?: "Неверный код"
                return@withContext RemoteOtpResult.Rejected(errMsg)
            }
        } catch (_: Exception) {
            // Unreachable or offline -> fallback
            return@withContext RemoteOtpResult.OfflineFallback
        }

        return@withContext RemoteOtpResult.OfflineFallback
    }

    suspend fun checkApprovalStatus(username: String, password: String? = null): RemoteApprovalResult = withContext(Dispatchers.IO) {
        val cleanUser = username.trim()
        if (cleanUser.isBlank()) return@withContext RemoteApprovalResult.Unreachable

        // 1. Try GET /api/trainer/approval-status?username=...
        try {
            val enc = URLEncoder.encode(cleanUser, "UTF-8")
            val url = URL("$backendBaseUrl/api/trainer/approval-status?username=$enc")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            val code = conn.responseCode
            if (code in 200..299) {
                val text = conn.inputStream.bufferedReader().readText()
                val json = JsonParser.parseString(text).asJsonObject
                val isApproved = when {
                    json.has("isApproved") -> json.get("isApproved").asBoolean
                    json.has("approved") -> json.get("approved").asBoolean
                    json.has("status") -> json.get("status").asString.equals("APPROVED", ignoreCase = true)
                    else -> false
                }
                return@withContext if (isApproved) RemoteApprovalResult.Approved else RemoteApprovalResult.Pending
            }
        } catch (_: Exception) {}

        // 2. Try POST /api/trainer/approval-status with {"username": cleanUser}
        try {
            val url = URL("$backendBaseUrl/api/trainer/approval-status")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; utf-8")
            conn.doOutput = true
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            val payload = JsonObject().apply {
                addProperty("username", cleanUser)
            }
            conn.outputStream.use { os ->
                os.write(gson.toJson(payload).toByteArray(Charsets.UTF_8))
            }
            val code = conn.responseCode
            if (code in 200..299) {
                val text = conn.inputStream.bufferedReader().readText()
                val json = JsonParser.parseString(text).asJsonObject
                val isApproved = when {
                    json.has("isApproved") -> json.get("isApproved").asBoolean
                    json.has("approved") -> json.get("approved").asBoolean
                    json.has("status") -> json.get("status").asString.equals("APPROVED", ignoreCase = true)
                    else -> false
                }
                return@withContext if (isApproved) RemoteApprovalResult.Approved else RemoteApprovalResult.Pending
            }
        } catch (_: Exception) {}

        // 3. Fallback: Check against /api/login endpoint
        if (!password.isNullOrBlank()) {
            try {
                val url = URL("$backendBaseUrl/api/login")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; utf-8")
                conn.doOutput = true
                conn.connectTimeout = 4000
                conn.readTimeout = 4000
                val payload = JsonObject().apply {
                    addProperty("username", cleanUser)
                    addProperty("password", password)
                }
                conn.outputStream.use { os ->
                    os.write(gson.toJson(payload).toByteArray(Charsets.UTF_8))
                }
                val code = conn.responseCode
                if (code == 200) {
                    return@withContext RemoteApprovalResult.Approved
                } else if (code == 403) {
                    return@withContext RemoteApprovalResult.Pending
                }
            } catch (_: Exception) {}
        }

        return@withContext RemoteApprovalResult.Unreachable
    }
}
