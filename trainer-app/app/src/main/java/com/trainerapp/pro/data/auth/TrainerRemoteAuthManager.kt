package com.trainerapp.pro.data.auth

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class TrainerRemoteUserInfo(
    val id: Long = 0,
    val username: String = "",
    val role: String = "trainer",
    val fullName: String = "",
    val phone: String = "",
    val telegramUsername: String = "",
    val isApproved: Boolean = false,
    val twoFactorEnabled: Boolean = false,
    val avatarBase64: String = "",
    val token: String = ""
)

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

sealed class TrainerRemoteAuthResult {
    data class Success(val token: String, val user: TrainerRemoteUserInfo) : TrainerRemoteAuthResult()
    data class Require2Fa(val userId: Long, val message: String, val expiresInSeconds: Int) : TrainerRemoteAuthResult()
    data class PendingApproval(val message: String) : TrainerRemoteAuthResult()
    data class InvalidCredentials(val message: String) : TrainerRemoteAuthResult()
    data class Error(val message: String) : TrainerRemoteAuthResult()
    object OfflineFallback : TrainerRemoteAuthResult()
}

sealed class TrainerTelegramSessionStatusResult {
    data class Authorized(val token: String, val user: TrainerRemoteUserInfo) : TrainerTelegramSessionStatusResult()
    data class Require2Fa(val userId: Long, val message: String, val expiresInSeconds: Int) : TrainerTelegramSessionStatusResult()
    data class Pending(val message: String = "Ожидание авторизации...") : TrainerTelegramSessionStatusResult()
    data class Expired(val message: String = "Сессия истекла") : TrainerTelegramSessionStatusResult()
    data class Error(val message: String) : TrainerTelegramSessionStatusResult()
}

/**
 * Dedicated auth manager handling remote login, 2FA OTP verification, 72h registration approval checks,
 * and 1-click Telegram session auth against backend endpoints with graceful offline fallback.
 */
class TrainerRemoteAuthManager(
    var backendBaseUrl: String = "https://fitness-ecosystem-pro.onrender.com"
) {
    private val gson = Gson()

    fun cleanOtp(otp: String): String = Companion.cleanOtp(otp)
    fun isValidOtpFormat(otp: String): Boolean = Companion.isValidOtpFormat(otp)

    companion object {
        fun cleanOtp(otp: String): String = otp.filter { it.isDigit() }
        fun isValidOtpFormat(otp: String): Boolean = cleanOtp(otp).length == 6
    }

    /**
     * Remote login via POST /api/login
     */
    suspend fun login(username: String, pass: String): TrainerRemoteAuthResult = withContext(Dispatchers.IO) {
        val cleanUser = username.trim()
        if (cleanUser.isBlank() || pass.isBlank()) {
            return@withContext TrainerRemoteAuthResult.InvalidCredentials("Введите логин и пароль")
        }

        try {
            val url = URL("$backendBaseUrl/api/login")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                doOutput = true
                connectTimeout = 6000
                readTimeout = 6000
            }

            val payload = JsonObject().apply {
                addProperty("username", cleanUser)
                addProperty("password", pass)
            }

            conn.outputStream.use { os ->
                os.write(gson.toJson(payload).toByteArray(Charsets.UTF_8))
            }

            val code = conn.responseCode
            val responseText = if (code in 200..299) {
                conn.inputStream.bufferedReader().readText()
            } else {
                conn.errorStream?.bufferedReader()?.readText() ?: ""
            }

            val json = runCatching { JsonParser.parseString(responseText).asJsonObject }.getOrNull()

            if (code == 200 && json != null) {
                if (json.has("require2FA") && json.get("require2FA").asBoolean) {
                    val userId = if (json.has("userId")) json.get("userId").asLong else 0L
                    val msg = if (json.has("message")) json.get("message").asString else "Требуется код 2FA"
                    val exp = if (json.has("expiresInSeconds")) json.get("expiresInSeconds").asInt else 300
                    return@withContext TrainerRemoteAuthResult.Require2Fa(userId, msg, exp)
                }

                val token = if (json.has("token")) json.get("token").asString else ""
                val userObj = json.getAsJsonObject("user")
                val user = if (userObj != null) {
                    TrainerRemoteUserInfo(
                        id = if (userObj.has("id")) userObj.get("id").asLong else 0L,
                        username = if (userObj.has("username")) userObj.get("username").asString else cleanUser,
                        role = if (userObj.has("role")) userObj.get("role").asString else "trainer",
                        fullName = if (userObj.has("fullName") && !userObj.get("fullName").isJsonNull) userObj.get("fullName").asString else "",
                        phone = if (userObj.has("phone") && !userObj.get("phone").isJsonNull) userObj.get("phone").asString else "",
                        telegramUsername = if (userObj.has("telegramUsername") && !userObj.get("telegramUsername").isJsonNull) userObj.get("telegramUsername").asString else "",
                        isApproved = true,
                        twoFactorEnabled = if (userObj.has("twoFactorEnabled") && !userObj.get("twoFactorEnabled").isJsonNull) userObj.get("twoFactorEnabled").asBoolean else false,
                        avatarBase64 = if (userObj.has("avatarBase64") && !userObj.get("avatarBase64").isJsonNull) userObj.get("avatarBase64").asString
                            else if (userObj.has("avatar_base64") && !userObj.get("avatar_base64").isJsonNull) userObj.get("avatar_base64").asString else "",
                        token = token
                    )
                } else {
                    TrainerRemoteUserInfo(username = cleanUser, isApproved = true, token = token)
                }
                return@withContext TrainerRemoteAuthResult.Success(token, user)
            } else if (code == 403) {
                val errMsg = json?.get("error")?.asString ?: "⏳ Аккаунт тренера находится на рассмотрении (до 72 часов)"
                return@withContext TrainerRemoteAuthResult.PendingApproval(errMsg)
            } else if (code == 401) {
                val errMsg = json?.get("error")?.asString ?: "Неверный логин или пароль"
                return@withContext TrainerRemoteAuthResult.InvalidCredentials(errMsg)
            } else if (code in 400..499) {
                val errMsg = json?.get("error")?.asString ?: "Ошибка авторизации ($code)"
                return@withContext TrainerRemoteAuthResult.Error(errMsg)
            }
        } catch (_: Exception) {
            return@withContext TrainerRemoteAuthResult.OfflineFallback
        }

        return@withContext TrainerRemoteAuthResult.OfflineFallback
    }

    /**
     * Verify 2FA OTP code via POST /api/login/2fa
     */
    suspend fun verify2FaLogin(userId: Long, otp: String): TrainerRemoteAuthResult = withContext(Dispatchers.IO) {
        val clean = cleanOtp(otp)
        if (clean.length != 6) {
            return@withContext TrainerRemoteAuthResult.Error("Код должен содержать ровно 6 цифр")
        }

        try {
            val url = URL("$backendBaseUrl/api/login/2fa")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                doOutput = true
                connectTimeout = 5000
                readTimeout = 5000
            }

            val payload = JsonObject().apply {
                addProperty("userId", userId)
                addProperty("code", clean)
            }

            conn.outputStream.use { os ->
                os.write(gson.toJson(payload).toByteArray(Charsets.UTF_8))
            }

            val code = conn.responseCode
            val responseText = if (code in 200..299) {
                conn.inputStream.bufferedReader().readText()
            } else {
                conn.errorStream?.bufferedReader()?.readText() ?: ""
            }

            val json = runCatching { JsonParser.parseString(responseText).asJsonObject }.getOrNull()

            if (code == 200 && json != null) {
                val token = if (json.has("token")) json.get("token").asString else ""
                val userObj = json.getAsJsonObject("user")
                val user = if (userObj != null) {
                    TrainerRemoteUserInfo(
                        id = if (userObj.has("id")) userObj.get("id").asLong else userId,
                        username = if (userObj.has("username")) userObj.get("username").asString else "",
                        role = if (userObj.has("role")) userObj.get("role").asString else "trainer",
                        fullName = if (userObj.has("fullName") && !userObj.get("fullName").isJsonNull) userObj.get("fullName").asString else "",
                        phone = if (userObj.has("phone") && !userObj.get("phone").isJsonNull) userObj.get("phone").asString else "",
                        telegramUsername = if (userObj.has("telegramUsername") && !userObj.get("telegramUsername").isJsonNull) userObj.get("telegramUsername").asString else "",
                        isApproved = true,
                        twoFactorEnabled = true,
                        token = token
                    )
                } else {
                    TrainerRemoteUserInfo(id = userId, isApproved = true, token = token)
                }
                return@withContext TrainerRemoteAuthResult.Success(token, user)
            } else {
                val errMsg = json?.get("error")?.asString ?: "Неверный код 2FA"
                return@withContext TrainerRemoteAuthResult.Error(errMsg)
            }
        } catch (_: Exception) {
            return@withContext TrainerRemoteAuthResult.OfflineFallback
        }
    }

    /**
     * Submit trainer registration via POST /api/register
     */
    suspend fun registerTrainer(
        trainerName: String,
        username: String,
        pass: String,
        phone: String,
        telegram: String
    ): TrainerRemoteAuthResult = withContext(Dispatchers.IO) {
        val cleanUser = username.trim()
        val cleanName = trainerName.trim()
        val cleanPhone = phone.trim()
        val cleanTg = telegram.trim()

        try {
            val url = URL("$backendBaseUrl/api/register")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                doOutput = true
                connectTimeout = 6000
                readTimeout = 6000
            }

            val payload = JsonObject().apply {
                addProperty("username", cleanUser)
                addProperty("password", pass)
                addProperty("fullName", cleanName)
                addProperty("phone", cleanPhone)
                addProperty("telegram", cleanTg)
                addProperty("role", "trainer")
            }

            conn.outputStream.use { os ->
                os.write(gson.toJson(payload).toByteArray(Charsets.UTF_8))
            }

            val code = conn.responseCode
            val responseText = if (code in 200..299) {
                conn.inputStream.bufferedReader().readText()
            } else {
                conn.errorStream?.bufferedReader()?.readText() ?: ""
            }

            val json = runCatching { JsonParser.parseString(responseText).asJsonObject }.getOrNull()

            if (code == 200 && json != null) {
                val token = if (json.has("token")) json.get("token").asString else ""
                val isApproved = if (json.has("isApproved")) json.get("isApproved").asBoolean else false
                val user = TrainerRemoteUserInfo(
                    username = cleanUser,
                    fullName = cleanName,
                    phone = cleanPhone,
                    telegramUsername = cleanTg,
                    isApproved = isApproved,
                    token = token
                )
                if (isApproved) {
                    return@withContext TrainerRemoteAuthResult.Success(token, user)
                } else {
                    val msg = if (json.has("message")) json.get("message").asString else "Заявка на рассмотрении (до 72 часов)"
                    return@withContext TrainerRemoteAuthResult.PendingApproval(msg)
                }
            } else {
                val errMsg = json?.get("error")?.asString ?: "Ошибка регистрации"
                return@withContext TrainerRemoteAuthResult.Error(errMsg)
            }
        } catch (_: Exception) {
            return@withContext TrainerRemoteAuthResult.OfflineFallback
        }
    }

    /**
     * Request one-time 6-digit code via Telegram bot: POST /api/auth/telegram/request-otp
     */
    suspend fun requestTelegramOtp(username: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val cleanUser = username.trim().removePrefix("@")
        if (cleanUser.isBlank()) return@withContext Pair(false, "Укажите Telegram логин")

        try {
            val url = URL("$backendBaseUrl/api/auth/telegram/request-otp")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                doOutput = true
                connectTimeout = 5000
                readTimeout = 5000
            }

            val payload = JsonObject().apply {
                addProperty("username", cleanUser)
            }
            conn.outputStream.use { os ->
                os.write(gson.toJson(payload).toByteArray(Charsets.UTF_8))
            }

            val code = conn.responseCode
            val text = if (code in 200..299) conn.inputStream.bufferedReader().readText() else conn.errorStream?.bufferedReader()?.readText() ?: ""
            val json = runCatching { JsonParser.parseString(text).asJsonObject }.getOrNull()

            if (code == 200 && json != null && json.has("success") && json.get("success").asBoolean) {
                val msg = if (json.has("message")) json.get("message").asString else "Код отправлен в Telegram"
                return@withContext Pair(true, msg)
            } else {
                val errMsg = json?.get("error")?.asString ?: "Не удалось отправить код"
                return@withContext Pair(false, errMsg)
            }
        } catch (e: Exception) {
            return@withContext Pair(false, e.message ?: "Сетевая ошибка при запросе кода")
        }
    }

    suspend fun verifyOtp(username: String, otp: String): RemoteOtpResult = withContext(Dispatchers.IO) {
        val clean = cleanOtp(otp)
        if (clean.length != 6) {
            return@withContext RemoteOtpResult.Rejected("Код должен содержать ровно 6 цифр")
        }

        try {
            val url = URL("$backendBaseUrl/api/auth/telegram/verify-otp")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                doOutput = true
                connectTimeout = 4000
                readTimeout = 4000
            }

            val payload = JsonObject().apply {
                val cleanUser = username.trim().removePrefix("@")
                if (cleanUser.isNotBlank()) {
                    addProperty("username", cleanUser)
                }
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
            return@withContext RemoteOtpResult.OfflineFallback
        }

        return@withContext RemoteOtpResult.OfflineFallback
    }

    /**
     * Verify one-time 6-digit code for Trainer Login returning TrainerRemoteAuthResult.
     * Supports direct 1-step verification by OTP alone or optional username.
     */
    suspend fun verifyTelegramLogin(otp: String, username: String = ""): TrainerRemoteAuthResult = withContext(Dispatchers.IO) {
        var codeClean = cleanOtp(otp)
        var userClean = username.trim().removePrefix("@")
        // Gracefully swap if called positionally as (username, otp)
        if (codeClean.length != 6 && cleanOtp(userClean).length == 6) {
            val temp = codeClean
            codeClean = cleanOtp(userClean)
            userClean = temp
        }

        if (codeClean.length != 6) {
            return@withContext TrainerRemoteAuthResult.Error("Код должен содержать ровно 6 цифр")
        }

        try {
            val url = URL("$backendBaseUrl/api/auth/telegram/verify-otp")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                doOutput = true
                connectTimeout = 5000
                readTimeout = 5000
            }

            val payload = JsonObject().apply {
                if (userClean.isNotBlank()) {
                    addProperty("username", userClean)
                }
                addProperty("code", codeClean)
            }
            conn.outputStream.use { os ->
                os.write(gson.toJson(payload).toByteArray(Charsets.UTF_8))
            }

            val code = conn.responseCode
            val text = if (code in 200..299) conn.inputStream.bufferedReader().readText() else conn.errorStream?.bufferedReader()?.readText() ?: ""
            val json = runCatching { JsonParser.parseString(text).asJsonObject }.getOrNull()

            if (code == 200 && json != null) {
                val token = if (json.has("token")) json.get("token").asString else ""
                val userObj = json.getAsJsonObject("user")
                val user = if (userObj != null) {
                    TrainerRemoteUserInfo(
                        id = if (userObj.has("id")) userObj.get("id").asLong else 0L,
                        username = if (userObj.has("username")) userObj.get("username").asString else if (userClean.isNotBlank()) userClean else "trainer",
                        role = if (userObj.has("role")) userObj.get("role").asString else "trainer",
                        fullName = if (userObj.has("fullName") && !userObj.get("fullName").isJsonNull) userObj.get("fullName").asString else "",
                        phone = if (userObj.has("phone") && !userObj.get("phone").isJsonNull) userObj.get("phone").asString else "",
                        telegramUsername = if (userObj.has("telegramUsername") && !userObj.get("telegramUsername").isJsonNull) userObj.get("telegramUsername").asString
                            else if (userObj.has("telegram_username") && !userObj.get("telegram_username").isJsonNull) userObj.get("telegram_username").asString
                            else userClean,
                        isApproved = true,
                        twoFactorEnabled = if (userObj.has("twoFactorEnabled") && !userObj.get("twoFactorEnabled").isJsonNull) userObj.get("twoFactorEnabled").asBoolean else false,
                        token = token
                    )
                } else {
                    TrainerRemoteUserInfo(username = if (userClean.isNotBlank()) userClean else "trainer", telegramUsername = userClean, isApproved = true, token = token)
                }
                return@withContext TrainerRemoteAuthResult.Success(token, user)
            } else {
                val errMsg = json?.get("error")?.asString ?: "Неверный код из Telegram"
                return@withContext TrainerRemoteAuthResult.Error(errMsg)
            }
        } catch (_: Exception) {
            return@withContext TrainerRemoteAuthResult.OfflineFallback
        }
    }

    suspend fun checkApprovalStatus(username: String, password: String? = null): RemoteApprovalResult = withContext(Dispatchers.IO) {
        val cleanUser = username.trim()
        if (cleanUser.isBlank()) return@withContext RemoteApprovalResult.Unreachable

        // 1. Try GET /api/trainer/approval-status?username=...
        try {
            val enc = URLEncoder.encode(cleanUser, "UTF-8")
            val url = URL("$backendBaseUrl/api/trainer/approval-status?username=$enc")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 4000
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

        // 2. Try POST /api/trainer/approval-status with {"username": cleanUser}
        try {
            val url = URL("$backendBaseUrl/api/trainer/approval-status")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                doOutput = true
                connectTimeout = 4000
                readTimeout = 4000
            }
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
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    doOutput = true
                    connectTimeout = 4000
                    readTimeout = 4000
                }
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

    /**
     * Initialize Telegram 1-Click login session via POST /api/auth/telegram/session-init
     */
    suspend fun initTelegramSession(): Pair<String, String>? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$backendBaseUrl/api/auth/telegram/session-init")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                doOutput = true
                connectTimeout = 5000
                readTimeout = 5000
            }
            val payload = JsonObject().apply {
                addProperty("requestedRole", "trainer")
            }
            conn.outputStream.use { os -> os.write(gson.toJson(payload).toByteArray(Charsets.UTF_8)) }

            if (conn.responseCode == 200) {
                val text = conn.inputStream.bufferedReader().readText()
                val json = JsonParser.parseString(text).asJsonObject
                if (json.has("sessionId") && json.has("botUrl")) {
                    return@withContext Pair(json.get("sessionId").asString, json.get("botUrl").asString)
                }
            }
        } catch (_: Exception) {}
        null
    }

    /**
     * Poll Telegram 1-Click login session status via GET /api/auth/telegram/session-status?sessionId=...
     */
    suspend fun pollTelegramSession(sessionId: String): TrainerTelegramSessionStatusResult = withContext(Dispatchers.IO) {
        try {
            val enc = URLEncoder.encode(sessionId, "UTF-8")
            val url = URL("$backendBaseUrl/api/auth/telegram/session-status?sessionId=$enc")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 4000
            }

            val code = conn.responseCode
            val text = if (code in 200..299) conn.inputStream.bufferedReader().readText() else conn.errorStream?.bufferedReader()?.readText() ?: ""
            val json = runCatching { JsonParser.parseString(text).asJsonObject }.getOrNull()

            if (code == 200 && json != null) {
                val status = if (json.has("status")) json.get("status").asString else ""
                if (status == "AUTHORIZED") {
                    val token = if (json.has("token")) json.get("token").asString else ""
                    val userObj = json.getAsJsonObject("user")
                    val user = if (userObj != null) {
                        TrainerRemoteUserInfo(
                            id = if (userObj.has("id")) userObj.get("id").asLong else 0L,
                            username = if (userObj.has("username")) userObj.get("username").asString else "",
                            role = if (userObj.has("role")) userObj.get("role").asString else "trainer",
                            fullName = if (userObj.has("fullName") && !userObj.get("fullName").isJsonNull) userObj.get("fullName").asString else "",
                            phone = if (userObj.has("phone") && !userObj.get("phone").isJsonNull) userObj.get("phone").asString else "",
                            telegramUsername = if (userObj.has("telegramUsername") && !userObj.get("telegramUsername").isJsonNull) userObj.get("telegramUsername").asString
                                else if (userObj.has("telegram_username") && !userObj.get("telegram_username").isJsonNull) userObj.get("telegram_username").asString else "",
                            isApproved = true,
                            twoFactorEnabled = if (userObj.has("twoFactorEnabled") && !userObj.get("twoFactorEnabled").isJsonNull) userObj.get("twoFactorEnabled").asBoolean else false,
                            avatarBase64 = if (userObj.has("avatarBase64") && !userObj.get("avatarBase64").isJsonNull) userObj.get("avatarBase64").asString
                                else if (userObj.has("avatar_base64") && !userObj.get("avatar_base64").isJsonNull) userObj.get("avatar_base64").asString else "",
                            token = token
                        )
                    } else {
                        TrainerRemoteUserInfo(isApproved = true, token = token)
                    }
                    return@withContext TrainerTelegramSessionStatusResult.Authorized(token, user)
                } else if (status == "REQUIRES_2FA") {
                    val userId = if (json.has("userId")) json.get("userId").asLong else 0L
                    val msg = if (json.has("message")) json.get("message").asString else "Включена 2FA: введите 6-значный код из Telegram"
                    val exp = if (json.has("expiresInSeconds")) json.get("expiresInSeconds").asInt else 300
                    return@withContext TrainerTelegramSessionStatusResult.Require2Fa(userId, msg, exp)
                } else if (status == "EXPIRED") {
                    return@withContext TrainerTelegramSessionStatusResult.Expired()
                } else {
                    return@withContext TrainerTelegramSessionStatusResult.Pending()
                }
            } else {
                return@withContext TrainerTelegramSessionStatusResult.Error("Ошибка проверки сессии")
            }
        } catch (e: Exception) {
            return@withContext TrainerTelegramSessionStatusResult.Error(e.message ?: "Сетевая ошибка")
        }
    }

    /**
     * Generate 5-minute deep link for 1-Click Telegram Linking
     */
    suspend fun getTelegramLinkDeepLink(authToken: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$backendBaseUrl/api/user/telegram/link-token")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Authorization", "Bearer $authToken")
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                doOutput = true
                connectTimeout = 5000
                readTimeout = 5000
            }
            conn.outputStream.use { it.write("{}".toByteArray(Charsets.UTF_8)) }
            if (conn.responseCode == 200) {
                val text = conn.inputStream.bufferedReader().readText()
                val json = JsonParser.parseString(text).asJsonObject
                if (json.has("deepLink") && !json.get("deepLink").isJsonNull) {
                    return@withContext json.get("deepLink").asString
                }
            }
        } catch (_: Exception) {}
        null
    }

    /**
     * Link Telegram via 6-digit code obtained from the bot
     */
    suspend fun linkTelegramByBotCode(authToken: String, code: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val clean = cleanOtp(code)
        if (clean.length != 6) {
            return@withContext Pair(false, "Код должен содержать ровно 6 цифр")
        }
        try {
            val url = URL("$backendBaseUrl/api/user/telegram/link-by-bot-code")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Authorization", "Bearer $authToken")
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                doOutput = true
                connectTimeout = 5000
                readTimeout = 5000
            }
            val payload = JsonObject().apply { addProperty("code", clean) }
            conn.outputStream.use { it.write(gson.toJson(payload).toByteArray(Charsets.UTF_8)) }
            val respCode = conn.responseCode
            val text = if (respCode in 200..299) conn.inputStream.bufferedReader().readText() else conn.errorStream?.bufferedReader()?.readText() ?: ""
            val json = runCatching { JsonParser.parseString(text).asJsonObject }.getOrNull()
            if (respCode == 200 && json != null && json.has("success") && json.get("success").asBoolean) {
                val uname = if (json.has("telegramUsername") && !json.get("telegramUsername").isJsonNull) json.get("telegramUsername").asString else ""
                return@withContext Pair(true, uname)
            } else {
                val err = json?.get("error")?.asString ?: "Не удалось привязать Telegram"
                return@withContext Pair(false, err)
            }
        } catch (e: Exception) {
            return@withContext Pair(false, e.message ?: "Сетевая ошибка")
        }
    }
}
