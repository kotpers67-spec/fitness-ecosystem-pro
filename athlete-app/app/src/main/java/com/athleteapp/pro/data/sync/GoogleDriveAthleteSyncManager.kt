package com.athleteapp.pro.data.sync

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.athleteapp.pro.data.local.dao.AthleteDao
import com.athleteapp.pro.data.local.entities.AthleteProfileEntity
import com.athleteapp.pro.data.sync.model.*
import com.athleteapp.pro.ui.screens.LeaderboardEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class GoogleDriveAthleteSyncManager(private val dao: AthleteDao) {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val athleteSyncManager = AthleteSyncManager(dao)
    private val _cloudAthletes = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val cloudAthletes: StateFlow<List<LeaderboardEntry>> = _cloudAthletes.asStateFlow()

    suspend fun syncWithCoach(
        athleteId: Long = 1L,
        clientUuidOverride: String? = null,
        pinCreatedAt: Long? = null
    ): Result<String> = withContext(Dispatchers.IO) {
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

                    // Check pairing node for unified PIN parity & status confirmation
                    if (parsed.has("pairing")) {
                        val pairingObj = parsed.getAsJsonObject("pairing")
                        var activePin = cleanPin
                        var matchedPinEntry: JsonObject? = if (cleanPin.isNotBlank() && pairingObj.has(cleanPin)) pairingObj.getAsJsonObject(cleanPin) else null

                        if (matchedPinEntry == null) {
                            for (entry in pairingObj.entrySet()) {
                                if (entry.value.isJsonObject) {
                                    val obj = entry.value.asJsonObject
                                    if (obj.has("clientUuid") && obj.get("clientUuid").asString == clientUuid) {
                                        activePin = entry.key
                                        matchedPinEntry = obj
                                        break
                                    }
                                }
                            }
                        }

                        if (matchedPinEntry != null) {
                            val status = if (matchedPinEntry.has("status")) matchedPinEntry.get("status").asString else ""
                            val coachName = if (matchedPinEntry.has("coachName") && matchedPinEntry.get("coachName").asString.isNotBlank()) matchedPinEntry.get("coachName").asString else "Тренер"
                            val coachPhone = if (matchedPinEntry.has("coachPhone")) matchedPinEntry.get("coachPhone").asString else ""
                            val coachB64 = if (matchedPinEntry.has("coachAvatarBase64")) matchedPinEntry.get("coachAvatarBase64").asString else null
                            val restrictionsFromCloud = if (matchedPinEntry.has("restrictions")) matchedPinEntry.get("restrictions").asString else ""
                            val phoneFromCloud = if (matchedPinEntry.has("phone")) matchedPinEntry.get("phone").asString else ""

                            val up = dao.getProfile().firstOrNull() ?: profile
                            if (status.equals("PAIRED", ignoreCase = true)) {
                                dao.saveProfile(up.copy(
                                    isPairedWithCoach = true,
                                    pairedCoachName = coachName,
                                    pairedCoachPhone = coachPhone,
                                    pairedCoachAvatarBase64 = coachB64,
                                    pairingPin = if (activePin.length == 6) activePin else up.pairingPin,
                                    restrictions = if (restrictionsFromCloud.isNotBlank()) restrictionsFromCloud else up.restrictions,
                                    phone = if (phoneFromCloud.isNotBlank()) phoneFromCloud else up.phone
                                ))
                            } else if (status.equals("UNPAIRED", ignoreCase = true) || status.equals("PENDING", ignoreCase = true)) {
                                dao.saveProfile(up.copy(
                                    isPairedWithCoach = false,
                                    pairedCoachName = "",
                                    pairedCoachPhone = "",
                                    pairedCoachAvatarBase64 = null,
                                    pairingPin = if (activePin.length == 6) activePin else up.pairingPin,
                                    restrictions = if (restrictionsFromCloud.isNotBlank()) restrictionsFromCloud else up.restrictions,
                                    phone = if (phoneFromCloud.isNotBlank()) phoneFromCloud else up.phone
                                ))
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

                        // Load real athletes from cloud sync for leaderboard
                        val otherAthletes = mutableListOf<LeaderboardEntry>()
                        for (entry in clientsObj.entrySet()) {
                            val otherUuid = entry.key
                            if (otherUuid != clientUuid && entry.value.isJsonObject) {
                                try {
                                    val otherPayload = gson.fromJson(entry.value, AthleteSyncPayload::class.java)
                                    if (otherPayload != null && otherPayload.clientName.isNotBlank() &&
                                        !otherPayload.clientName.contains("Смирнов", ignoreCase = true) &&
                                        !otherPayload.clientName.contains("Smirnov", ignoreCase = true) &&
                                        !otherPayload.clientName.contains("Спам", ignoreCase = true) &&
                                        !otherPayload.clientName.contains("Тест", ignoreCase = true)) {
                                        val workoutsCount = otherPayload.assignedWorkouts.count { it.completed }
                                        val weightGain = otherPayload.assignedWorkouts
                                            .flatMap { it.exercises }
                                            .flatMap { it.sets }
                                            .filter { it.isCompleted }
                                            .sumOf { if (it.actualWeightKg > 15.0) (it.actualWeightKg - 15.0) else 0.0 }
                                        val pts = workoutsCount * 10 + weightGain.toInt()
                                        otherAthletes.add(
                                            LeaderboardEntry(
                                                rank = 0,
                                                name = otherPayload.clientName,
                                                workoutsCount = workoutsCount,
                                                weightGainKg = weightGain,
                                                points = pts,
                                                avatarBase64 = otherPayload.avatarBase64,
                                                isMe = false
                                            )
                                        )
                                    }
                                } catch (_: Exception) {}
                            }
                        }
                        _cloudAthletes.value = otherAthletes
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
                pairingCode = cleanPin,
                clientName = currentProfile.fullName.ifBlank { "Атлет" },
                phone = currentProfile.phone,
                restrictions = currentProfile.restrictions,
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
                    addProperty("trainerVersion", "1.0.8")
                    addProperty("trainerUrl", "https://github.com/kotpers67-spec/fitness-ecosystem-pro/releases/download/v1.0.8/trainer-pro-v1.0.8.apk")
                    addProperty("athleteVersion", "1.0.8")
                    addProperty("athleteUrl", "https://github.com/kotpers67-spec/fitness-ecosystem-pro/releases/download/v1.0.8/athlete-pro-v1.0.8.apk")
                    addProperty("notes", "Версия 1.0.8: Прямая загрузка релизов, вход через @fitnessecosystemBOT и обновлённая синхронизация.")
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

                // Remove stale pairing entries for this clientUuid
                val staleKeys = mutableListOf<String>()
                for ((k, elem) in pairingObj.entrySet()) {
                    if (elem.isJsonObject) {
                        val entry = elem.asJsonObject
                        if (entry.get("clientUuid")?.asString == clientUuid && k != cleanPin) {
                            staleKeys.add(k)
                        }
                    }
                }
                staleKeys.forEach { pairingObj.remove(it) }

                val pairingNode = JsonObject().apply {
                    addProperty("pin", cleanPin)
                    addProperty("clientUuid", clientUuid)
                    addProperty("clientName", currentProfile.fullName)
                    addProperty("phone", currentProfile.phone)
                    addProperty("goal", currentProfile.goal)
                    addProperty("restrictions", currentProfile.restrictions)
                    addProperty("notes", currentProfile.notes)
                    if (!currentProfile.avatarBase64.isNullOrBlank()) {
                        addProperty("avatarBase64", currentProfile.avatarBase64)
                    }
                    addProperty("timestamp", if ((pinCreatedAt ?: 0L) > 0L) pinCreatedAt!! else System.currentTimeMillis())
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
                return@withContext Result.failure(Exception("Не удалось обновить данные в облаке"))
            }

            Result.success("Облако: данные синхронизированы! Актуализировано тренировок: $pulledWorkouts")
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
                pairedCoachPhone = "",
                pairedCoachPhotoUri = null,
                pairedCoachAvatarBase64 = null,
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
                    val decryptedJson = CloudSecurityManager.decryptPayload(cloudJson)
                    val rootObj = JsonParser.parseString(decryptedJson).asJsonObject
                    if (rootObj.has("pairing")) {
                        val pairingObj = rootObj.getAsJsonObject("pairing")
                        if (oldPin.isNotBlank() && pairingObj.has(oldPin)) {
                            pairingObj.remove(oldPin)
                        }
                    }
                    rootObj.addProperty("updatedAt", System.currentTimeMillis().toString())
                    val encryptedPayload = CloudSecurityManager.encryptPayload(gson.toJson(rootObj))
                    httpPost(requestUrl, encryptedPayload)
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
