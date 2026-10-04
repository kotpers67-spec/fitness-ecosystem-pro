package com.trainerapp.pro.data.sync

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.trainerapp.pro.data.local.dao.TrainerDao
import com.trainerapp.pro.data.local.entities.ClientEntity
import com.trainerapp.pro.data.sync.model.AthleteSyncPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID

class GoogleDriveSyncManager {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val gitHubSyncManager = GitHubSyncManager()

    /**
     * Выполняет полный цикл двусторонней синхронизации через Google Диск:
     * 1. Скачивает и объединяет выполненные подходы подопечного по clientUuid.
     * 2. Загружает актуальные назначенные тренировки тренера под изолированным ключом clientUuid.
     */
    suspend fun syncClient(
        dao: TrainerDao,
        client: ClientEntity,
        coachName: String? = null,
        coachPhone: String? = null,
        coachAvatarBase64: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val endpoint = CloudSecurityManager.getEndpointUrl()
            val secretKey = CloudSecurityManager.getSecretKey()

            val encodedKey = URLEncoder.encode(secretKey, "UTF-8")
            val requestUrl = "$endpoint?key=$encodedKey"

            // 1. Читаем текущее состояние с Google Диска (с автоматической расшифровкой AES-256)
            val rawCloudData = httpGet(requestUrl)
            val currentCloudJson = CloudSecurityManager.decryptPayload(rawCloudData)
            var pullMsg = "Данных подопечного в облаке пока нет."

            val clientUuid = client.clientUuid
            val clientIdStr = client.id.toString()
            var rootObj = JsonObject()

            if (currentCloudJson.isNotBlank() && currentCloudJson != "{}") {
                try {
                    val parsed = JsonParser.parseString(currentCloudJson).asJsonObject
                    if (parsed.has("clients")) {
                        rootObj = parsed
                        val clientsObj = parsed.getAsJsonObject("clients")

                        // Приоритетно ищем по clientUuid, затем по legacy id
                        val clientPayloadJson = when {
                            clientUuid.isNotBlank() && clientsObj.has(clientUuid) -> {
                                clientsObj.get(clientUuid).toString()
                            }
                            clientsObj.has(clientIdStr) -> {
                                clientsObj.get(clientIdStr).toString()
                            }
                            else -> null
                        }

                        if (clientPayloadJson != null) {
                            val mergeRes = gitHubSyncManager.applyAthletePayload(dao, client.id, clientPayloadJson)
                            if (mergeRes.isSuccess) {
                                pullMsg = mergeRes.getOrNull() ?: "Прогресс подопечного объединен."
                            }
                        }
                    } else if (parsed.has("athleteId") || parsed.has("clientUuid")) {
                        // Прямой одиночный payload
                        val mergeRes = gitHubSyncManager.applyAthletePayload(dao, client.id, currentCloudJson)
                        if (mergeRes.isSuccess) {
                            pullMsg = mergeRes.getOrNull() ?: "Прогресс подопечного объединен."
                        }
                    }
                } catch (e: Exception) {
                    // Игнорируем ошибки парсинга старого формата
                }
            }

            // 2. Формируем актуальный payload тренера для подопечного
            val trainerPayload = gitHubSyncManager.buildAthletePayload(dao, client)
            val trainerPayloadElement = JsonParser.parseString(gson.toJson(trainerPayload))

            if (!rootObj.has("clients")) {
                rootObj.add("clients", JsonObject())
            }

            // Обновляем запись сопряжения в облачном реестре pairing, если она существует
            if (rootObj.has("pairing")) {
                val pairingObj = rootObj.getAsJsonObject("pairing")
                val cleanPin = client.pairingCode?.filter { it.isDigit() } ?: ""
                val cUuid = client.clientUuid
                for ((key, element) in pairingObj.entrySet()) {
                    if (!element.isJsonObject) continue
                    val entryObj = element.asJsonObject
                    val entryUuid = entryObj.get("clientUuid")?.asString
                    val entryPin = entryObj.get("pin")?.asString?.filter { it.isDigit() } ?: key.filter { it.isDigit() }
                    val matchesUuid = cUuid.isNotBlank() && entryUuid == cUuid
                    val matchesPin = cleanPin.isNotBlank() && entryPin == cleanPin
                    if (matchesUuid || matchesPin) {
                        if (!coachName.isNullOrBlank()) entryObj.addProperty("coachName", coachName)
                        if (!coachPhone.isNullOrBlank()) entryObj.addProperty("coachPhone", coachPhone)
                        if (!coachAvatarBase64.isNullOrBlank()) {
                            entryObj.addProperty("coachAvatarBase64", coachAvatarBase64)
                        }
                        entryObj.addProperty("status", "PAIRED")
                        entryObj.addProperty("lastSyncAt", System.currentTimeMillis().toString())
                        break
                    }
                }
            }

            if (!rootObj.has("updates")) {
                val defaultUpdates = JsonObject().apply {
                    addProperty("trainerVersion", "1.0.5")
                    addProperty("trainerUrl", "https://github.com/santiyastudio-lgtm/fitness-ecosystem-pro/releases/download/v1.0.5/trainer-pro-v1.0.5.apk")
                    addProperty("athleteVersion", "1.0.5")
                    addProperty("athleteUrl", "https://github.com/santiyastudio-lgtm/fitness-ecosystem-pro/releases/download/v1.0.5/athlete-pro-v1.0.5.apk")
                    addProperty("notes", "Версия 1.0.5: Карточка тренера, синхронизация фото, состязания и статистика упражнений.")
                }
                rootObj.add("updates", defaultUpdates)
            }

            // Ключом в облаке строго является clientUuid для изоляции данных подопечных
            val clientStorageKey = if (clientUuid.isNotBlank()) clientUuid else clientIdStr
            rootObj.getAsJsonObject("clients").add(clientStorageKey, trainerPayloadElement)
            rootObj.addProperty("updatedAt", System.currentTimeMillis().toString())

            // 3. Отправляем зашифрованный JSON (AES-256) в облако
            val updatedJson = gson.toJson(rootObj)
            val encryptedJson = CloudSecurityManager.encryptPayload(updatedJson)
            val postSuccess = httpPost(requestUrl, encryptedJson)

            if (!postSuccess) {
                return@withContext Result.failure(Exception("Не удалось сохранить данные в облаке (ошибка HTTP)"))
            }

            Result.success("Облако: план для ${client.fullName} отправлен. $pullMsg")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Разрешает подопечного по 6-значному PIN-коду или QR-коду (JSON) из реестра pairing в Google Диске.
     * Создает или обновляет ClientEntity в локальной БД тренера и фиксирует статус PAIRED в облаке.
     */
    suspend fun findAndPairAthlete(
        dao: TrainerDao,
        inputCodeOrJson: String,
        coachName: String = "Алексей Романов",
        coachPhone: String = "+7 (999) 123-45-67",
        coachAvatarBase64: String? = null
    ): Result<ClientEntity> = withContext(Dispatchers.IO) {
        try {
            val trimmedInput = inputCodeOrJson.trim()
            if (trimmedInput.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Код или данные QR не могут быть пустыми"))
            }

            var pinFromQr: String? = null
            var uuidFromQr: String? = null
            var nameFromQr: String? = null
            var phoneFromQr: String? = null
            var goalFromQr: String? = null
            var notesFromQr: String? = null

            // Clean input string from surrounding quotes or markdown formatting
            var rawInput = trimmedInput
            if (rawInput.startsWith("```json")) {
                rawInput = rawInput.removePrefix("```json").removeSuffix("```").trim()
            } else if (rawInput.startsWith("```")) {
                rawInput = rawInput.removePrefix("```").removeSuffix("```").trim()
            }
            if (rawInput.startsWith("\"") && rawInput.endsWith("\"") && rawInput.length > 2) {
                rawInput = rawInput.substring(1, rawInput.length - 1).replace("\\\"", "\"").replace("\\\\", "\\")
            }

            // Extract JSON object if present
            var jsonCandidate: String? = null
            if (rawInput.startsWith("{") && rawInput.endsWith("}")) {
                jsonCandidate = rawInput
            } else if (rawInput.contains("{") && rawInput.contains("}")) {
                val start = rawInput.indexOf("{")
                val end = rawInput.lastIndexOf("}")
                if (start < end) {
                    jsonCandidate = rawInput.substring(start, end + 1)
                }
            }

            if (jsonCandidate != null) {
                try {
                    val element = JsonParser.parseString(jsonCandidate)
                    val qrJson = if (element.isJsonObject) element.asJsonObject else JsonParser.parseString(element.asString).asJsonObject
                    pinFromQr = qrJson.get("pin")?.asString
                    uuidFromQr = qrJson.get("uuid")?.asString ?: qrJson.get("clientUuid")?.asString
                    nameFromQr = qrJson.get("name")?.asString ?: qrJson.get("clientName")?.asString
                    phoneFromQr = qrJson.get("phone")?.asString
                    goalFromQr = qrJson.get("goal")?.asString
                    notesFromQr = qrJson.get("notes")?.asString
                } catch (_: Exception) {
                    // Fallback to Regex extraction if JSON parsing fails due to escaping/quotes
                    Regex("\"pin\"\\s*:\\s*\"([^\"]+)\"").find(jsonCandidate)?.let { pinFromQr = it.groupValues[1] }
                    Regex("\"uuid\"\\s*:\\s*\"([^\"]+)\"").find(jsonCandidate)?.let { uuidFromQr = it.groupValues[1] }
                    Regex("\"clientUuid\"\\s*:\\s*\"([^\"]+)\"").find(jsonCandidate)?.let { uuidFromQr = it.groupValues[1] }
                    Regex("\"name\"\\s*:\\s*\"([^\"]+)\"").find(jsonCandidate)?.let { nameFromQr = it.groupValues[1] }
                    Regex("\"clientName\"\\s*:\\s*\"([^\"]+)\"").find(jsonCandidate)?.let { nameFromQr = it.groupValues[1] }
                    Regex("\"phone\"\\s*:\\s*\"([^\"]+)\"").find(jsonCandidate)?.let { phoneFromQr = it.groupValues[1] }
                    Regex("\"goal\"\\s*:\\s*\"([^\"]+)\"").find(jsonCandidate)?.let { goalFromQr = it.groupValues[1] }
                    Regex("\"notes\"\\s*:\\s*\"([^\"]+)\"").find(jsonCandidate)?.let { notesFromQr = it.groupValues[1] }
                }
            }

            var extractedCode: String? = pinFromQr
            if (extractedCode == null) {
                // If user pasted a link (e.g. https://fitnessapp.pro/pair?code=265507 or ?pin=265507)
                val urlMatch = Regex("""[?&](?:code|pin)=(\d{6})""").find(rawInput)
                    ?: Regex("""/pair/(\d{6})""").find(rawInput)
                if (urlMatch != null) {
                    extractedCode = urlMatch.groupValues[1]
                }
            }

            val cleanCode = (extractedCode ?: rawInput).filter { it.isDigit() }
            val cleanPin = if (cleanCode.length >= 6) cleanCode.take(6) else cleanCode
            if (cleanPin.length != 6 && uuidFromQr.isNullOrBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Неверный формат кода. Введите 6 цифр (например, 739102 без тире) или ссылку подопечного."))
            }

            val endpoint = CloudSecurityManager.getEndpointUrl()
            val secretKey = CloudSecurityManager.getSecretKey()
            val encodedKey = URLEncoder.encode(secretKey, "UTF-8")
            val requestUrl = "$endpoint?key=$encodedKey"

            val rawCloudData = httpGet(requestUrl)
            val currentCloudJson = CloudSecurityManager.decryptPayload(rawCloudData)
            var rootObj = JsonObject()

            var athleteUuid: String? = uuidFromQr
            var athleteName: String? = nameFromQr
            var athletePhone: String? = phoneFromQr
            var athleteGoal: String? = goalFromQr
            var athleteNotes: String? = notesFromQr
            var foundPairingKey: String? = null

            if (currentCloudJson.isNotBlank() && currentCloudJson != "{}") {
                try {
                    rootObj = JsonParser.parseString(currentCloudJson).asJsonObject
                } catch (e: Exception) {
                    return@withContext Result.failure(IllegalStateException("Ошибка разбора данных облака: ${e.message}"))
                }
            }

            val validationResult = validateAndProcessPairingData(
                rootObj = rootObj,
                cleanPin = cleanPin,
                coachName = coachName,
                coachPhone = coachPhone,
                coachAvatarBase64 = coachAvatarBase64,
                now = System.currentTimeMillis()
            )

            if (validationResult.isFailure) {
                val exception = validationResult.exceptionOrNull()
                // If code expired, write back rootObj with the expired entry removed
                if (exception is IllegalStateException && exception.message?.contains("истёк") == true) {
                    try {
                        val encPost = CloudSecurityManager.encryptPayload(gson.toJson(rootObj))
                        httpPost(requestUrl, encPost)
                    } catch (_: Exception) {}
                }
                return@withContext Result.failure(exception ?: Exception("Ошибка привязки подопечного"))
            }

            val (clientData, updatedRootObj) = validationResult.getOrThrow()

            // 1. Проверяем локальную базу: есть ли уже клиент с таким UUID или PIN
            var client = dao.getClientByUuid(clientData.clientUuid)
            if (client == null && cleanPin.isNotEmpty()) {
                client = dao.getClientByPairingCode(cleanPin)
            }

            if (client != null) {
                // Обновляем данные существующего клиента
                val updated = client.copy(
                    fullName = if (client.fullName.isBlank() || client.fullName.startsWith("Подопечный ")) clientData.fullName else client.fullName,
                    phone = if (client.phone.isBlank()) clientData.phone else client.phone,
                    goal = if (client.goal.isBlank()) clientData.goal else client.goal,
                    notes = if (client.notes.isBlank()) clientData.notes else client.notes,
                    clientUuid = clientData.clientUuid,
                    pairingCode = cleanPin,
                    membershipStatus = "Активен"
                )
                dao.updateClient(updated)
                client = updated
            } else {
                // Вставляем нового подопечного
                val newId = dao.insertClient(clientData)
                client = dao.getClientById(newId)
                    ?: return@withContext Result.failure(Exception("Ошибка сохранения клиента в локальной базе"))
            }

            // 2. Обновляем статус в облачном реестре спаривания на PAIRED
            val encPost = CloudSecurityManager.encryptPayload(gson.toJson(updatedRootObj))
            val postSuccess = httpPost(requestUrl, encPost)
            if (!postSuccess) {
                return@withContext Result.failure(IllegalStateException("Ошибка записи статуса привязки в облако"))
            }

            // 3. Выполняем начальную синхронизацию с облаком
            try {
                syncClient(dao, client, coachName, coachPhone, coachAvatarBase64)
            } catch (_: Exception) {}

            Result.success(client)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        /**
         * Валидация кода сопряжения в облачном реестре:
         * - Ошибка IllegalArgumentException("Код не найден"), если код отсутствует
         * - Ошибка IllegalStateException("Срок действия кода истёк (действует 5 минут)"), если возраст > 5 мин (удаляет из rootObj)
         * - Ошибка IllegalStateException("Этот код уже был использован"), если статус PAIRED или USED
         * - Успех: переводит статус в PAIRED и возвращает проверенного ClientEntity
         */
        fun validateAndProcessPairingData(
            rootObj: JsonObject,
            cleanPin: String,
            coachName: String = "Алексей Романов",
            coachPhone: String = "+7 (999) 123-45-67",
            coachAvatarBase64: String? = null,
            now: Long = System.currentTimeMillis()
        ): Result<Pair<ClientEntity, JsonObject>> {
            var foundPairingKey: String? = null
            var foundPairingEntry: JsonObject? = null

            if (rootObj.has("pairing")) {
                val pairingObj = rootObj.getAsJsonObject("pairing")
                for ((key, element) in pairingObj.entrySet()) {
                    if (!element.isJsonObject) continue
                    val digitsInKey = key.filter { it.isDigit() }
                    val entryObj = element.asJsonObject
                    val entryPin = entryObj.get("pin")?.asString?.filter { it.isDigit() } ?: digitsInKey

                    if (digitsInKey == cleanPin || entryPin == cleanPin) {
                        foundPairingKey = key
                        foundPairingEntry = entryObj
                        break
                    }
                }
            }

            if (foundPairingKey == null || foundPairingEntry == null) {
                return Result.failure(IllegalArgumentException("Код не найден"))
            }

            val timestamp = foundPairingEntry.get("timestamp")?.asLong ?: 0L
            val ttlMs = 5 * 60 * 1000L

            if (timestamp > 0L && (now - timestamp > ttlMs)) {
                if (rootObj.has("pairing")) {
                    rootObj.getAsJsonObject("pairing").remove(foundPairingKey)
                    rootObj.addProperty("updatedAt", now.toString())
                }
                return Result.failure(IllegalStateException("Срок действия кода истёк (действует 5 минут)"))
            }

            val currentStatus = foundPairingEntry.get("status")?.asString ?: "PENDING"
            if (currentStatus.equals("PAIRED", ignoreCase = true) || currentStatus.equals("USED", ignoreCase = true)) {
                return Result.failure(IllegalStateException("Этот код уже был использован"))
            }

            val athleteUuid = foundPairingEntry.get("clientUuid")?.asString?.takeIf { it.isNotBlank() }
                ?: return Result.failure(IllegalStateException("Некорректные данные профиля подопечного в облаке"))
            val athleteName = foundPairingEntry.get("clientName")?.asString?.takeIf { it.isNotBlank() }
                ?: foundPairingEntry.get("name")?.asString?.takeIf { it.isNotBlank() }
                ?: "Подопечный"
            val athletePhone = foundPairingEntry.get("phone")?.asString ?: ""
            val athleteGoal = foundPairingEntry.get("goal")?.asString ?: ""
            val athleteNotes = foundPairingEntry.get("notes")?.asString ?: ""

            foundPairingEntry.addProperty("pin", cleanPin)
            foundPairingEntry.addProperty("clientUuid", athleteUuid)
            foundPairingEntry.addProperty("clientName", athleteName)
            foundPairingEntry.addProperty("coachName", coachName)
            foundPairingEntry.addProperty("coachPhone", coachPhone)
            if (!coachAvatarBase64.isNullOrBlank()) {
                foundPairingEntry.addProperty("coachAvatarBase64", coachAvatarBase64)
            }
            foundPairingEntry.addProperty("status", "PAIRED")
            foundPairingEntry.addProperty("pairedAt", now.toString())

            rootObj.addProperty("updatedAt", now.toString())

            val client = ClientEntity(
                fullName = athleteName,
                phone = athletePhone,
                goal = athleteGoal,
                notes = athleteNotes,
                membershipStatus = "Активен",
                clientUuid = athleteUuid,
                pairingCode = cleanPin
            )
            return Result.success(Pair(client, rootObj))
        }
    }

    private fun httpGet(urlStr: String): String {
        var currentUrl = urlStr
        var redirects = 0
        while (redirects < 5) {
            val conn = URL(currentUrl).openConnection() as HttpURLConnection
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.instanceFollowRedirects = true
            conn.requestMethod = "GET"

            val code = conn.responseCode
            if (code in 300..308) {
                val loc = conn.getHeaderField("Location") ?: break
                currentUrl = loc
                redirects++
                continue
            }

            return if (code in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                ""
            }
        }
        return ""
    }

    private fun httpPost(urlStr: String, body: String): Boolean {
        var currentUrl = urlStr
        var redirects = 0

        while (redirects < 5) {
            val conn = URL(currentUrl).openConnection() as HttpURLConnection
            conn.connectTimeout = 20000
            conn.readTimeout = 20000
            conn.instanceFollowRedirects = false
            conn.requestMethod = if (redirects == 0) "POST" else "GET"
            conn.setRequestProperty("Content-Type", "application/json")

            if (redirects == 0) {
                conn.doOutput = true
                conn.outputStream.use { os ->
                    os.write(body.toByteArray(Charsets.UTF_8))
                }
            }

            val code = conn.responseCode
            if (code in 300..308) {
                val loc = conn.getHeaderField("Location") ?: return false
                currentUrl = loc
                redirects++
                continue
            }

            return code in 200..299
        }
        return false
    }
}
