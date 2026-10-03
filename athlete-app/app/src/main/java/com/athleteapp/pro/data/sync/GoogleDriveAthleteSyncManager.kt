package com.athleteapp.pro.data.sync

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.athleteapp.pro.data.local.dao.AthleteDao
import com.athleteapp.pro.data.local.entities.AthleteProfileEntity
import com.athleteapp.pro.data.sync.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class GoogleDriveAthleteSyncManager(private val dao: AthleteDao) {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val athleteSyncManager = AthleteSyncManager(dao)

    suspend fun syncWithCoach(athleteId: Long = 1L, clientUuidOverride: String? = null): Result<String> = withContext(Dispatchers.IO) {
        try {
            val endpoint = CloudSecurityManager.getEndpointUrl()
            val secretKey = CloudSecurityManager.getSecretKey()
            val encodedKey = URLEncoder.encode(secretKey, "UTF-8")
            val requestUrl = "$endpoint?key=$encodedKey"

            val profile = dao.getProfile().firstOrNull() ?: AthleteProfileEntity()
            val clientUuid = clientUuidOverride ?: profile.clientUuid.ifBlank {
                val freshUuid = java.util.UUID.randomUUID().toString()
                dao.saveProfile(profile.copy(clientUuid = freshUuid))
                freshUuid
            }
            val cleanPin = profile.pairingPin.filter { it.isDigit() }

            // 1. PULL: Читаем с Google Диска (с автоматической расшифровкой AES-256)
            val rawCloudData = httpGet(requestUrl)
            val cloudJson = CloudSecurityManager.decryptPayload(rawCloudData)
            var pulledWorkouts = 0

            val clientIdStr = athleteId.toString()
            var rootObj = JsonObject()

            if (cloudJson.isNotBlank() && cloudJson != "{}") {
                try {
                    val parsed = JsonParser.parseString(cloudJson).asJsonObject
                    rootObj = parsed

                    // Check pairing node for status confirmation
                    if (parsed.has("pairing") && cleanPin.isNotBlank()) {
                        val pairingObj = parsed.getAsJsonObject("pairing")
                        if (pairingObj.has(cleanPin)) {
                            val pinEntry = pairingObj.getAsJsonObject(cleanPin)
                            val status = if (pinEntry.has("status")) pinEntry.get("status").asString else ""
                            val coachName = if (pinEntry.has("coachName")) pinEntry.get("coachName").asString else "Тренер"
                            if (status.equals("PAIRED", ignoreCase = true)) {
                                if (!profile.isPairedWithCoach || profile.pairedCoachName != coachName) {
                                    val up = dao.getProfile().firstOrNull() ?: profile
                                    dao.saveProfile(up.copy(isPairedWithCoach = true, pairedCoachName = coachName))
                                }
                            } else if (status.equals("UNPAIRED", ignoreCase = true) || status.equals("PENDING", ignoreCase = true)) {
                                if (profile.isPairedWithCoach) {
                                    val up = dao.getProfile().firstOrNull() ?: profile
                                    dao.saveProfile(up.copy(isPairedWithCoach = false, pairedCoachName = ""))
                                }
                            }
                        }
                    }

                    // Strict Isolation: lookup strictly by clientUuid
                    if (parsed.has("clients")) {
                        val clientsObj = parsed.getAsJsonObject("clients")
                        if (clientsObj.has(clientUuid)) {
                            val clientPayload = clientsObj.get(clientUuid).toString()
                            val applyRes = athleteSyncManager.applyPayloadJson(clientPayload)
                            pulledWorkouts = applyRes.getOrDefault(0)
                        }
                    }
                } catch (_: Exception) { }
            }

            // 2. PUSH: Отправляем прогресс подопечного (выполненные сеты и замеры) strictly under clients[clientUuid]
            val currentProfile = dao.getProfile().firstOrNull() ?: profile
            val sessions = dao.getAllSessionsSync()
            val allSets = dao.getAllSetsSync()
            val allAnth = dao.getAllAnthropometrySync()
            val setsBySessionId = allSets.groupBy { it.sessionId }

            val syncSessions = sessions.map { session ->
                val sessionSets = setsBySessionId[session.id] ?: emptyList()
                val exercisesInSession = sessionSets.groupBy { it.exerciseId }.map { (exId, setsList) ->
                    val first = setsList.first()
                    SyncExercise(
                        exerciseId = exId,
                        name = first.exerciseName,
                        muscleGroup = first.muscleGroup,
                        sets = setsList.map { set ->
                            SyncWorkoutSet(
                                setNumber = set.setNumber,
                                targetWeightKg = set.targetWeightKg,
                                targetReps = set.targetReps,
                                actualWeightKg = set.actualWeightKg,
                                actualReps = set.actualReps,
                                isCompleted = set.isCompleted,
                                rpe = set.rpe
                            )
                        }
                    )
                }

                SyncWorkoutSession(
                    date = session.date,
                    notes = session.notes,
                    completed = session.completed,
                    isSelfWorkoutAllowed = session.isSelfWorkoutAllowed,
                    exercises = exercisesInSession
                )
            }

            val syncAnth = allAnth.map {
                SyncAnthropometry(
                    date = it.date,
                    weightKg = it.weightKg,
                    chestCm = it.chestCm,
                    waistCm = it.waistCm,
                    hipsCm = it.hipsCm,
                    bicepsCm = it.bicepsCm
                )
            }

            val myPayload = AthleteSyncPayload(
                clientUuid = clientUuid,
                athleteId = athleteId,
                clientName = currentProfile.fullName.ifBlank { "Александр Смирнов" },
                avatarBase64 = currentProfile.avatarBase64,
                syncTimestamp = System.currentTimeMillis(),
                assignedWorkouts = syncSessions,
                anthropometry = syncAnth
            )

            if (!rootObj.has("clients")) {
                rootObj.add("clients", JsonObject())
            }
            if (!rootObj.has("updates")) {
                val defaultUpdates = JsonObject().apply {
                    addProperty("trainerVersion", "1.0.3")
                    addProperty("trainerUrl", "https://github.com/santiyastudio-lgtm/fitness-ecosystem-pro/releases/download/v1.0.3/trainer-pro-v1.0.3.apk")
                    addProperty("athleteVersion", "1.0.3")
                    addProperty("athleteUrl", "https://github.com/santiyastudio-lgtm/fitness-ecosystem-pro/releases/download/v1.0.3/athlete-pro-v1.0.3.apk")
                    addProperty("notes", "Версия 1.0.3: Автофоновые обновления, экспорт в мессенджеры и импорт из файлов.")
                }
                rootObj.add("updates", defaultUpdates)
            }
            // Strict Isolation: Write strictly to our clientUuid key
            rootObj.getAsJsonObject("clients").add(clientUuid, JsonParser.parseString(gson.toJson(myPayload)))

            // Publish or update pairing handshake node
            if (cleanPin.isNotBlank()) {
                if (!rootObj.has("pairing")) {
                    rootObj.add("pairing", JsonObject())
                }
                val pairingObj = rootObj.getAsJsonObject("pairing")
                val pairingNode = JsonObject().apply {
                    addProperty("pin", cleanPin)
                    addProperty("clientUuid", clientUuid)
                    addProperty("clientName", currentProfile.fullName)
                    addProperty("phone", currentProfile.phone)
                    addProperty("goal", currentProfile.goal)
                    addProperty("restrictions", currentProfile.restrictions)
                    addProperty("notes", currentProfile.notes)
                    addProperty("timestamp", System.currentTimeMillis())
                    addProperty("status", if (currentProfile.isPairedWithCoach) "PAIRED" else "PENDING")
                    if (currentProfile.pairedCoachName.isNotBlank()) {
                        addProperty("coachName", currentProfile.pairedCoachName)
                    }
                }
                pairingObj.add(cleanPin, pairingNode)
            }

            rootObj.addProperty("updatedAt", System.currentTimeMillis().toString())

            val encryptedJson = CloudSecurityManager.encryptPayload(gson.toJson(rootObj))
            val postSuccess = httpPost(requestUrl, encryptedJson)
            if (!postSuccess) {
                return@withContext Result.failure(Exception("Не удалось обновить данные на Google Диске"))
            }

            Result.success("Google Диск: данные синхронизированы! Актуализировано тренировок: $pulledWorkouts")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unpairFromCoach(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val current = dao.getProfile().firstOrNull() ?: AthleteProfileEntity()
            val oldPin = current.pairingPin.filter { it.isDigit() }
            val newPin = String.format("%06d", (100000..999999).random())
            val newUuid = java.util.UUID.randomUUID().toString()
            val updated = current.copy(
                isPairedWithCoach = false,
                pairedCoachName = "",
                pairingPin = newPin,
                clientUuid = newUuid
            )
            dao.saveProfile(updated)

            // 1. Remove or unpair old PIN node from cloud
            val endpoint = CloudSecurityManager.getEndpointUrl()
            val secretKey = CloudSecurityManager.getSecretKey()
            val encodedKey = URLEncoder.encode(secretKey, "UTF-8")
            val requestUrl = "$endpoint?key=$encodedKey"

            val cloudJson = httpGet(requestUrl)
            if (cloudJson.isNotBlank() && cloudJson != "{}") {
                try {
                    val rootObj = JsonParser.parseString(cloudJson).asJsonObject
                    if (rootObj.has("pairing")) {
                        val pairingObj = rootObj.getAsJsonObject("pairing")
                        if (oldPin.isNotBlank() && pairingObj.has(oldPin)) {
                            pairingObj.remove(oldPin)
                        }
                    }
                    rootObj.addProperty("updatedAt", System.currentTimeMillis().toString())
                    httpPost(requestUrl, gson.toJson(rootObj))
                } catch (_: Exception) {}
            }

            // 2. Push new unassigned PENDING state with new UUID and PIN
            syncWithCoach(clientUuidOverride = newUuid)
            Result.success("Вы успешно отвязались от тренера. Новый код: ${newPin.substring(0, 3)}-${newPin.substring(3)}")
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
            } else ""
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
