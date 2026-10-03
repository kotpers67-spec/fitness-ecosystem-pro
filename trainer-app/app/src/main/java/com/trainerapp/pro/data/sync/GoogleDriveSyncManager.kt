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
    suspend fun syncClient(dao: TrainerDao, client: ClientEntity): Result<String> = withContext(Dispatchers.IO) {
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

            // Ключом в облаке строго является clientUuid для изоляции данных подопечных
            val clientStorageKey = if (clientUuid.isNotBlank()) clientUuid else clientIdStr
            rootObj.getAsJsonObject("clients").add(clientStorageKey, trainerPayloadElement)
            rootObj.addProperty("updatedAt", System.currentTimeMillis().toString())

            // 3. Отправляем зашифрованный JSON (AES-256) в Google Диск
            val updatedJson = gson.toJson(rootObj)
            val encryptedJson = CloudSecurityManager.encryptPayload(updatedJson)
            val postSuccess = httpPost(requestUrl, encryptedJson)

            if (!postSuccess) {
                return@withContext Result.failure(Exception("Не удалось сохранить данные на Google Диске (ошибка HTTP)"))
            }

            Result.success("Google Диск: план для ${client.fullName} отправлен в облако. $pullMsg")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Разрешает подопечного по 6-значному PIN-коду или QR-коду (JSON) из реестра pairing в Google Диске.
     * Создает или обновляет ClientEntity в локальной БД тренера и фиксирует статус PAIRED в облаке.
     */
    suspend fun findAndPairAthlete(dao: TrainerDao, inputCodeOrJson: String): Result<ClientEntity> = withContext(Dispatchers.IO) {
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

            val cleanPin = (pinFromQr ?: rawInput).filter { it.isDigit() }
            if (cleanPin.length != 6 && uuidFromQr.isNullOrBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Неверный формат кода. Введите 6 цифр (например, 739-102) или отсканируйте QR-код."))
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
                    if (rootObj.has("pairing")) {
                        val pairingObj = rootObj.getAsJsonObject("pairing")

                        // Ищем запись по точному cleanPin или по числовому совпадению ключа
                        for ((key, element) in pairingObj.entrySet()) {
                            val digitsInKey = key.filter { it.isDigit() }
                            val entryObj = element.asJsonObject
                            val entryPin = entryObj.get("pin")?.asString?.filter { it.isDigit() } ?: digitsInKey

                            if (digitsInKey == cleanPin || entryPin == cleanPin) {
                                foundPairingKey = key
                                if (athleteUuid.isNullOrBlank()) {
                                    athleteUuid = entryObj.get("clientUuid")?.asString
                                }
                                if (athleteName.isNullOrBlank()) {
                                    athleteName = entryObj.get("clientName")?.asString ?: entryObj.get("name")?.asString
                                }
                                if (athletePhone.isNullOrBlank()) {
                                    athletePhone = entryObj.get("phone")?.asString
                                }
                                if (athleteGoal.isNullOrBlank()) {
                                    athleteGoal = entryObj.get("goal")?.asString
                                }
                                if (athleteNotes.isNullOrBlank()) {
                                    athleteNotes = entryObj.get("notes")?.asString
                                }
                                break
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Ошибка чтения облака
                }
            }

            // Если в облаке нет записи и нет UUID из QR, код не найден
            if (athleteUuid.isNullOrBlank() && foundPairingKey == null) {
                val formattedPin = if (cleanPin.length == 6) "${cleanPin.substring(0, 3)}-${cleanPin.substring(3)}" else cleanPin
                return@withContext Result.failure(Exception("Подопечный с кодом $formattedPin не найден в облаке. Убедитесь, что Athlete Pro открыт на экране спаривания."))
            }

            val finalUuid = athleteUuid ?: UUID.randomUUID().toString()
            val finalName = athleteName?.takeIf { it.isNotBlank() } ?: "Подопечный ${cleanPin.take(3)}-${cleanPin.takeLast(3)}"
            val finalPhone = athletePhone ?: ""
            val finalGoal = athleteGoal ?: ""
            val finalNotes = athleteNotes ?: ""

            // 1. Проверяем локальную базу: есть ли уже клиент с таким UUID или PIN
            var client = dao.getClientByUuid(finalUuid)
            if (client == null && cleanPin.isNotEmpty()) {
                client = dao.getClientByPairingCode(cleanPin)
            }

            if (client != null) {
                // Обновляем данные существующего клиента
                val updated = client.copy(
                    fullName = if (client.fullName.isBlank() || client.fullName.startsWith("Подопечный ")) finalName else client.fullName,
                    phone = if (client.phone.isBlank()) finalPhone else client.phone,
                    goal = if (client.goal.isBlank()) finalGoal else client.goal,
                    notes = if (client.notes.isBlank()) finalNotes else client.notes,
                    clientUuid = finalUuid,
                    pairingCode = cleanPin,
                    membershipStatus = "Активен"
                )
                dao.updateClient(updated)
                client = updated
            } else {
                // Вставляем нового подопечного
                val newClient = ClientEntity(
                    fullName = finalName,
                    phone = finalPhone,
                    goal = finalGoal,
                    notes = finalNotes,
                    membershipStatus = "Активен",
                    clientUuid = finalUuid,
                    pairingCode = cleanPin
                )
                val newId = dao.insertClient(newClient)
                client = dao.getClientById(newId)
                    ?: return@withContext Result.failure(Exception("Ошибка сохранения клиента в локальной базе"))
            }

            // 2. Обновляем статус в облачном реестре спаривания на PAIRED
            try {
                if (!rootObj.has("pairing")) {
                    rootObj.add("pairing", JsonObject())
                }
                val pairingObj = rootObj.getAsJsonObject("pairing")
                val targetKey = foundPairingKey ?: cleanPin

                val pairingEntry = if (pairingObj.has(targetKey)) {
                    pairingObj.getAsJsonObject(targetKey)
                } else {
                    JsonObject().also { pairingObj.add(targetKey, it) }
                }

                pairingEntry.addProperty("pin", cleanPin)
                pairingEntry.addProperty("clientUuid", finalUuid)
                pairingEntry.addProperty("clientName", client.fullName)
                pairingEntry.addProperty("status", "PAIRED")
                pairingEntry.addProperty("pairedAt", System.currentTimeMillis().toString())

                rootObj.addProperty("updatedAt", System.currentTimeMillis().toString())
                val encPost = CloudSecurityManager.encryptPayload(gson.toJson(rootObj))
                httpPost(requestUrl, encPost)
            } catch (_: Exception) {
                // Ошибка обновления облачного статуса спаривания не должна ломать локальную привязку
            }

            // 3. Выполняем начальную синхронизацию с облаком
            try {
                syncClient(dao, client)
            } catch (_: Exception) {}

            Result.success(client)
        } catch (e: Exception) {
            Result.failure(e)
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
