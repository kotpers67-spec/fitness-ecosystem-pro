package com.athleteapp.pro.data.auth

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class AthleteRemoteUserInfo(
    val id: Long = 0,
    val username: String = "",
    val role: String = "athlete",
    val fullName: String = "",
    val phone: String = "",
    val pairingCode: String = "",
    val telegramUsername: String = "",
    val twoFactorEnabled: Boolean = false,
    val avatarBase64: String = "",
    val token: String = ""
)

sealed class AthleteRemoteAuthResult {
    data class Success(val token: String, val user: AthleteRemoteUserInfo) : AthleteRemoteAuthResult()
    data class Require2Fa(val userId: Long, val message: String, val expiresInSeconds: Int) : AthleteRemoteAuthResult()
    data class InvalidCredentials(val message: String) : AthleteRemoteAuthResult()
    data class Error(val message: String) : AthleteRemoteAuthResult()
    object OfflineFallback : AthleteRemoteAuthResult()
}

sealed class TelegramSessionStatusResult {
    data class Authorized(val token: String, val user: AthleteRemoteUserInfo) : TelegramSessionStatusResult()
    data class Require2Fa(val userId: Long, val message: String, val expiresInSeconds: Int) : TelegramSessionStatusResult()
    data class Pending(val message: String = "Ожидание авторизации...") : TelegramSessionStatusResult()
    data class Expired(val message: String = "Сессия истекла") : TelegramSessionStatusResult()
    data class Error(val message: String) : TelegramSessionStatusResult()
}

class AthleteRemoteAuthManager(
    var backendBaseUrl: String = "https://fitness-ecosystem-pro.onrender.com"
) {
    private val gson = Gson()

    fun cleanOtp(otp: String): String = otp.filter { it.isDigit() }
    fun isValidOtpFormat(otp: String): Boolean = cleanOtp(otp).length == 6

    /**
     * Remote login via POST /api/login
     */
    suspend fun login(username: String, pass: String): AthleteRemoteAuthResult = withContext(Dispatchers.IO) {
        val cleanUser = username.trim()
        if (cleanUser.isBlank() || pass.isBlank()) {
            return@withContext AthleteRemoteAuthResult.InvalidCredentials("Введите логин и пароль")
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
                    return@withContext AthleteRemoteAuthResult.Require2Fa(userId, msg, exp)
                }

                val token = if (json.has("token")) json.get("token").asString else ""
                val userObj = json.getAsJsonObject("user")
                val user = if (userObj != null) {
                    AthleteRemoteUserInfo(
                        id = if (userObj.has("id")) userObj.get("id").asLong else 0L,
                        username = if (userObj.has("username")) userObj.get("username").asString else cleanUser,
                        role = if (userObj.has("role")) userObj.get("role").asString else "athlete",
                        fullName = if (userObj.has("fullName") && !userObj.get("fullName").isJsonNull) userObj.get("fullName").asString else "",
                        phone = if (userObj.has("phone") && !userObj.get("phone").isJsonNull) userObj.get("phone").asString else "",
                        pairingCode = if (userObj.has("pairingCode") && !userObj.get("pairingCode").isJsonNull) userObj.get("pairingCode").asString else "",
                        telegramUsername = if (userObj.has("telegramUsername") && !userObj.get("telegramUsername").isJsonNull) userObj.get("telegramUsername").asString else "",
                        twoFactorEnabled = if (userObj.has("twoFactorEnabled") && !userObj.get("twoFactorEnabled").isJsonNull) userObj.get("twoFactorEnabled").asBoolean else false,
                        avatarBase64 = if (userObj.has("avatarBase64") && !userObj.get("avatarBase64").isJsonNull) userObj.get("avatarBase64").asString
                            else if (userObj.has("avatar_base64") && !userObj.get("avatar_base64").isJsonNull) userObj.get("avatar_base64").asString else "",
                        token = token
                    )
                } else {
                    AthleteRemoteUserInfo(username = cleanUser, token = token)
                }
                return@withContext AthleteRemoteAuthResult.Success(token, user)
            } else if (code == 401) {
                val errMsg = json?.get("error")?.asString ?: "Неверный логин или пароль"
                return@withContext AthleteRemoteAuthResult.InvalidCredentials(errMsg)
            } else if (code in 400..499) {
                val errMsg = json?.get("error")?.asString ?: "Ошибка авторизации ($code)"
                return@withContext AthleteRemoteAuthResult.Error(errMsg)
            }
        } catch (_: Exception) {
            return@withContext AthleteRemoteAuthResult.OfflineFallback
        }

        return@withContext AthleteRemoteAuthResult.OfflineFallback
    }

    /**
     * Verify 2FA OTP code via POST /api/login/2fa
     */
    suspend fun verify2FaOtp(userId: Long, otp: String): AthleteRemoteAuthResult = withContext(Dispatchers.IO) {
        val clean = cleanOtp(otp)
        if (clean.length != 6) {
            return@withContext AthleteRemoteAuthResult.Error("Код должен содержать ровно 6 цифр")
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
                    AthleteRemoteUserInfo(
                        id = if (userObj.has("id")) userObj.get("id").asLong else userId,
                        username = if (userObj.has("username")) userObj.get("username").asString else "",
                        role = if (userObj.has("role")) userObj.get("role").asString else "athlete",
                        fullName = if (userObj.has("fullName") && !userObj.get("fullName").isJsonNull) userObj.get("fullName").asString else "",
                        phone = if (userObj.has("phone") && !userObj.get("phone").isJsonNull) userObj.get("phone").asString else "",
                        pairingCode = if (userObj.has("pairingCode") && !userObj.get("pairingCode").isJsonNull) userObj.get("pairingCode").asString else "",
                        telegramUsername = if (userObj.has("telegramUsername") && !userObj.get("telegramUsername").isJsonNull) userObj.get("telegramUsername").asString else "",
                        twoFactorEnabled = true,
                        avatarBase64 = if (userObj.has("avatarBase64") && !userObj.get("avatarBase64").isJsonNull) userObj.get("avatarBase64").asString
                            else if (userObj.has("avatar_base64") && !userObj.get("avatar_base64").isJsonNull) userObj.get("avatar_base64").asString else "",
                        token = token
                    )
                } else {
                    AthleteRemoteUserInfo(id = userId, token = token)
                }
                return@withContext AthleteRemoteAuthResult.Success(token, user)
            } else {
                val errMsg = json?.get("error")?.asString ?: "Неверный код 2FA"
                return@withContext AthleteRemoteAuthResult.Error(errMsg)
            }
        } catch (_: Exception) {
            return@withContext AthleteRemoteAuthResult.OfflineFallback
        }
    }

    /**
     * Remote registration via POST /api/register
     */
    suspend fun register(fullName: String, username: String, pass: String, phone: String): AthleteRemoteAuthResult = withContext(Dispatchers.IO) {
        val cleanUser = username.trim()
        val cleanName = fullName.trim()
        val cleanPhone = phone.trim()

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
                addProperty("role", "athlete")
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
                    AthleteRemoteUserInfo(
                        id = if (userObj.has("id")) userObj.get("id").asLong else 0L,
                        username = if (userObj.has("username")) userObj.get("username").asString else cleanUser,
                        role = "athlete",
                        fullName = if (userObj.has("fullName") && !userObj.get("fullName").isJsonNull) userObj.get("fullName").asString else cleanName,
                        phone = if (userObj.has("phone") && !userObj.get("phone").isJsonNull) userObj.get("phone").asString else cleanPhone,
                        pairingCode = if (userObj.has("pairingCode") && !userObj.get("pairingCode").isJsonNull) userObj.get("pairingCode").asString else "",
                        telegramUsername = if (userObj.has("telegramUsername") && !userObj.get("telegramUsername").isJsonNull) userObj.get("telegramUsername").asString else "",
                        token = token
                    )
                } else {
                    AthleteRemoteUserInfo(username = cleanUser, fullName = cleanName, phone = cleanPhone, token = token)
                }
                return@withContext AthleteRemoteAuthResult.Success(token, user)
            } else {
                val errMsg = json?.get("error")?.asString ?: "Ошибка регистрации"
                return@withContext AthleteRemoteAuthResult.Error(errMsg)
            }
        } catch (_: Exception) {
            return@withContext AthleteRemoteAuthResult.OfflineFallback
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

    /**
     * Verify one-time 6-digit code: POST /api/auth/telegram/verify-otp
     */
    suspend fun verifyTelegramOtp(username: String, otp: String): AthleteRemoteAuthResult = withContext(Dispatchers.IO) {
        val cleanUser = username.trim().removePrefix("@")
        val clean = cleanOtp(otp)
        if (clean.length != 6) {
            return@withContext AthleteRemoteAuthResult.Error("Код должен содержать ровно 6 цифр")
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
                addProperty("username", cleanUser)
                addProperty("code", clean)
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
                    AthleteRemoteUserInfo(
                        id = if (userObj.has("id")) userObj.get("id").asLong else 0L,
                        username = if (userObj.has("username")) userObj.get("username").asString else cleanUser,
                        role = if (userObj.has("role")) userObj.get("role").asString else "athlete",
                        fullName = if (userObj.has("fullName") && !userObj.get("fullName").isJsonNull) userObj.get("fullName").asString else "",
                        phone = if (userObj.has("phone") && !userObj.get("phone").isJsonNull) userObj.get("phone").asString else "",
                        pairingCode = if (userObj.has("pairingCode") && !userObj.get("pairingCode").isJsonNull) userObj.get("pairingCode").asString else "",
                        telegramUsername = if (userObj.has("telegramUsername") && !userObj.get("telegramUsername").isJsonNull) userObj.get("telegramUsername").asString else cleanUser,
                        twoFactorEnabled = if (userObj.has("twoFactorEnabled") && !userObj.get("twoFactorEnabled").isJsonNull) userObj.get("twoFactorEnabled").asBoolean else false,
                        avatarBase64 = if (userObj.has("avatarBase64") && !userObj.get("avatarBase64").isJsonNull) userObj.get("avatarBase64").asString
                            else if (userObj.has("avatar_base64") && !userObj.get("avatar_base64").isJsonNull) userObj.get("avatar_base64").asString else "",
                        token = token
                    )
                } else {
                    AthleteRemoteUserInfo(username = cleanUser, telegramUsername = cleanUser, token = token)
                }
                return@withContext AthleteRemoteAuthResult.Success(token, user)
            } else {
                val errMsg = json?.get("error")?.asString ?: "Неверный код из Telegram"
                return@withContext AthleteRemoteAuthResult.Error(errMsg)
            }
        } catch (_: Exception) {
            return@withContext AthleteRemoteAuthResult.OfflineFallback
        }
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
            conn.outputStream.use { os -> os.write("{}".toByteArray(Charsets.UTF_8)) }

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
    suspend fun pollTelegramSession(sessionId: String): TelegramSessionStatusResult = withContext(Dispatchers.IO) {
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
                        AthleteRemoteUserInfo(
                            id = if (userObj.has("id")) userObj.get("id").asLong else 0L,
                            username = if (userObj.has("username")) userObj.get("username").asString else "",
                            role = if (userObj.has("role")) userObj.get("role").asString else "athlete",
                            fullName = if (userObj.has("fullName") && !userObj.get("fullName").isJsonNull) userObj.get("fullName").asString else "",
                            phone = if (userObj.has("phone") && !userObj.get("phone").isJsonNull) userObj.get("phone").asString else "",
                            pairingCode = if (userObj.has("pairingCode") && !userObj.get("pairingCode").isJsonNull) userObj.get("pairingCode").asString else "",
                            telegramUsername = if (userObj.has("telegramUsername") && !userObj.get("telegramUsername").isJsonNull) userObj.get("telegramUsername").asString
                                else if (userObj.has("telegram_username") && !userObj.get("telegram_username").isJsonNull) userObj.get("telegram_username").asString else "",
                            twoFactorEnabled = if (userObj.has("twoFactorEnabled") && !userObj.get("twoFactorEnabled").isJsonNull) userObj.get("twoFactorEnabled").asBoolean else false,
                            avatarBase64 = if (userObj.has("avatarBase64") && !userObj.get("avatarBase64").isJsonNull) userObj.get("avatarBase64").asString
                                else if (userObj.has("avatar_base64") && !userObj.get("avatar_base64").isJsonNull) userObj.get("avatar_base64").asString else "",
                            token = token
                        )
                    } else {
                        AthleteRemoteUserInfo(token = token)
                    }
                    return@withContext TelegramSessionStatusResult.Authorized(token, user)
                } else if (status == "REQUIRES_2FA") {
                    val userId = if (json.has("userId")) json.get("userId").asLong else 0L
                    val msg = if (json.has("message")) json.get("message").asString else "Включена 2FA: введите 6-значный код из Telegram"
                    val exp = if (json.has("expiresInSeconds")) json.get("expiresInSeconds").asInt else 300
                    return@withContext TelegramSessionStatusResult.Require2Fa(userId, msg, exp)
                } else if (status == "EXPIRED") {
                    return@withContext TelegramSessionStatusResult.Expired()
                } else {
                    return@withContext TelegramSessionStatusResult.Pending()
                }
            } else {
                return@withContext TelegramSessionStatusResult.Error("Ошибка проверки сессии")
            }
        } catch (e: Exception) {
            return@withContext TelegramSessionStatusResult.Error(e.message ?: "Сетевая ошибка")
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
