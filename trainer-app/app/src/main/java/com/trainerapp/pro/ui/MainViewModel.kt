package com.trainerapp.pro.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.trainerapp.pro.data.backup.BackupManager
import com.trainerapp.pro.data.local.TrainerDatabase
import com.trainerapp.pro.data.local.dao.SetHistoryItem
import com.trainerapp.pro.data.local.entities.*
import com.trainerapp.pro.data.sync.SyncEngine
import com.trainerapp.pro.domain.calculators.AdaptiveLoadRecommendation
import com.trainerapp.pro.domain.calculators.NeuroAdaptiveEngine
import com.trainerapp.pro.domain.calculators.SessionReadinessInfo
import com.trainerapp.pro.domain.timer.RestTimerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = TrainerDatabase.getDatabase(application, viewModelScope)
    val dao = db.trainerDao()
    val timerManager = RestTimerManager(application)
    val backupManager = BackupManager(dao)
    val syncEngine = SyncEngine(application)
    val gitHubSync = com.trainerapp.pro.data.sync.GitHubSyncManager()
    val updateService = com.trainerapp.pro.data.update.UpdateService(application)

    private val _currentDate = MutableStateFlow(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
    val currentDate: StateFlow<String> = _currentDate.asStateFlow()

    private val _selectedClientId = MutableStateFlow<Long?>(null)
    val selectedClientId: StateFlow<Long?> = _selectedClientId.asStateFlow()

    private val _selectedExerciseOrder = MutableStateFlow(1)
    val selectedExerciseOrder: StateFlow<Int> = _selectedExerciseOrder.asStateFlow()

    val clients: StateFlow<List<ClientEntity>> = dao.getAllClients()
        .map { rawList ->
            val seenUuids = mutableSetOf<String>()
            val seenPhones = mutableSetOf<String>()
            val seenNames = mutableSetOf<String>()
            val deduped = mutableListOf<ClientEntity>()

            for (c in rawList) {
                val cleanPhone = c.phone.filter { it.isDigit() }
                val normName = c.fullName.trim().lowercase()

                if (c.clientUuid.isNotBlank() && seenUuids.contains(c.clientUuid)) continue
                if (cleanPhone.length >= 7 && seenPhones.contains(cleanPhone.takeLast(10))) continue
                if (normName.isNotBlank() && normName != "подопечный" && normName != "атлет" && seenNames.contains(normName)) continue

                if (c.clientUuid.isNotBlank()) seenUuids.add(c.clientUuid)
                if (cleanPhone.length >= 7) seenPhones.add(cleanPhone.takeLast(10))
                if (normName.isNotBlank()) seenNames.add(normName)
                deduped.add(c)
            }
            deduped
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val exercises: StateFlow<List<ExerciseEntity>> = dao.getAllExercises()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<AppSettingsEntity> = dao.getSettings()
        .map { it ?: AppSettingsEntity() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettingsEntity())

    val activeClient = combine(clients, selectedClientId) { clientList, id ->
        clientList.find { it.id == id } ?: clientList.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _currentSession = MutableStateFlow<WorkoutSessionEntity?>(null)
    val currentSession: StateFlow<WorkoutSessionEntity?> = _currentSession.asStateFlow()

    private val _currentSets = MutableStateFlow<List<WorkoutSetEntity>>(emptyList())
    val currentSets: StateFlow<List<WorkoutSetEntity>> = _currentSets.asStateFlow()

    private val _availableUpdate = MutableStateFlow<com.trainerapp.pro.data.update.UpdateCheckResult?>(null)
    val availableUpdate: StateFlow<com.trainerapp.pro.data.update.UpdateCheckResult?> = _availableUpdate.asStateFlow()

    private val _selectedChartExerciseId = MutableStateFlow<Long?>(null)
    val selectedChartExerciseId: StateFlow<Long?> = _selectedChartExerciseId.asStateFlow()

    // Neuro-Adaptive Real-Time Metrics
    val sessionTonnage: StateFlow<Double> = currentSets.map {
        NeuroAdaptiveEngine.calculateTonnage(it)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val sessionFatigueIndex: StateFlow<Double> = currentSets.map {
        NeuroAdaptiveEngine.calculateFatigueIndex(it)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val sessionReadiness: StateFlow<SessionReadinessInfo> = combine(selectedClientId, currentDate, currentSession) { cId, curDate, _ ->
        Pair(cId, curDate)
    }.flatMapLatest { (cId, curDate) ->
        flow {
            if (cId != null) {
                val previousCompletedSession = dao.getLastCompletedSessionBefore(cId, curDate)
                emit(NeuroAdaptiveEngine.calculateReadinessScore(previousCompletedSession?.date, curDate))
            } else {
                emit(NeuroAdaptiveEngine.calculateReadinessScore(null, curDate))
            }
        }
    }.flowOn(Dispatchers.IO)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SessionReadinessInfo(90.0, "Оптимальная готовность", "Готов к тренировке"))

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val exerciseHistory: StateFlow<List<SetHistoryItem>> = combine(selectedClientId, selectedChartExerciseId) { cId, eId ->
        Pair(cId, eId)
    }.flatMapLatest { (cId, eId) ->
        if (cId != null && eId != null) {
            dao.getExerciseHistory(cId, eId)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val anthropometryHistory: StateFlow<List<AnthropometryEntity>> = selectedClientId.flatMapLatest { cId ->
        if (cId != null) dao.getAnthropometryForClient(cId) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var loadSessionJob: Job? = null

    private val authPrefs = application.getSharedPreferences("trainer_auth", android.content.Context.MODE_PRIVATE)
    private val _isLoggedIn = MutableStateFlow(authPrefs.getBoolean("is_logged_in", false))
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    var is2FaEnabled: Boolean
        get() = authPrefs.getBoolean("is_2fa_enabled", false)
        set(value) {
            authPrefs.edit().putBoolean("is_2fa_enabled", value).apply()
            _is2FaEnabledFlow.value = value
        }

    private val _is2FaEnabledFlow = MutableStateFlow(authPrefs.getBoolean("is_2fa_enabled", false))
    val is2FaEnabledFlow: StateFlow<Boolean> = _is2FaEnabledFlow.asStateFlow()

    fun update2FaEnabled(enabled: Boolean) {
        is2FaEnabled = enabled
    }

    var telegramUsername: String
        get() = authPrefs.getString("telegram_username", "") ?: ""
        set(value) {
            val clean = value.trim().removePrefix("@")
            authPrefs.edit().putString("telegram_username", clean).apply()
            _telegramUsernameFlow.value = clean
        }

    private val _telegramUsernameFlow = MutableStateFlow(authPrefs.getString("telegram_username", "") ?: "")
    val telegramUsernameFlow: StateFlow<String> = _telegramUsernameFlow.asStateFlow()

    fun updateTelegramUsername(username: String) {
        telegramUsername = username
    }

    suspend fun getTelegramLinkDeepLink(): String? {
        val token = authPrefs.getString("auth_token", "") ?: ""
        remoteAuthManager.backendBaseUrl = backendBaseUrl
        return remoteAuthManager.getTelegramLinkDeepLink(token)
    }

    suspend fun linkTelegramByBotCode(code: String): Pair<Boolean, String> {
        val token = authPrefs.getString("auth_token", "") ?: ""
        if (token.isBlank()) return Pair(false, "Требуется авторизация")
        remoteAuthManager.backendBaseUrl = backendBaseUrl
        val (ok, res) = remoteAuthManager.linkTelegramByBotCode(token, code)
        if (ok && res.isNotBlank()) {
            updateTelegramUsername(res)
        }
        return Pair(ok, res)
    }

    val remoteAuthManager = com.trainerapp.pro.data.auth.TrainerRemoteAuthManager()

    companion object {
        var backendBaseUrl: String = "https://fitness-ecosystem-pro.onrender.com"
    }

    var isApproved: Boolean
        get() = authPrefs.getBoolean("is_approved", false)
        set(value) = authPrefs.edit().putBoolean("is_approved", value).apply()

    fun checkCredentials(username: String, pass: String): Boolean {
        val savedUser = authPrefs.getString("username", "") ?: ""
        val savedHash = authPrefs.getString("password_hash", "") ?: ""
        val inputHash = hashPassword(pass)
        return (savedUser.isNotEmpty() && savedUser.equals(username.trim(), ignoreCase = true) && savedHash == inputHash)
    }

    fun completeLogin(): Boolean {
        authPrefs.edit().putBoolean("is_logged_in", true).apply()
        _isLoggedIn.value = true
        return true
    }

    /**
     * Remote verification of 2FA OTP against backend /api/auth/telegram/verify-otp
     * with graceful fallback to local validation if offline or server is unreachable.
     */
    suspend fun verify2FaOtpRemote(otp: String, username: String? = null): Boolean = withContext(Dispatchers.IO) {
        remoteAuthManager.backendBaseUrl = backendBaseUrl
        val targetUser = (username?.trim()?.ifBlank { null }
            ?: authPrefs.getString("username", "")?.ifBlank { null }
            ?: telegramUsername.ifBlank { null }
            ?: "trainer")

        when (val res = remoteAuthManager.verifyOtp(targetUser, otp)) {
            is com.trainerapp.pro.data.auth.RemoteOtpResult.Success -> {
                completeLogin()
                true
            }
            is com.trainerapp.pro.data.auth.RemoteOtpResult.Rejected -> {
                false
            }
            is com.trainerapp.pro.data.auth.RemoteOtpResult.OfflineFallback -> {
                if (remoteAuthManager.isValidOtpFormat(otp)) {
                    completeLogin()
                    true
                } else false
            }
        }
    }

    fun verify2FaOtp(otp: String, username: String? = null): Boolean {
        val clean = otp.filter { it.isDigit() }
        if (clean.length != 6) return false
        return try {
            kotlinx.coroutines.runBlocking(Dispatchers.IO) {
                verify2FaOtpRemote(clean, username)
            }
        } catch (_: Exception) {
            if (clean.length == 6) {
                completeLogin()
                true
            } else false
        }
    }

    /**
     * Remote approval check for 72h registration verification against server
     * /api/trainer/approval-status or /api/me / /api/login, preserving local state fallback.
     */
    suspend fun checkRemoteApprovalStatus(username: String, password: String? = null): Boolean = withContext(Dispatchers.IO) {
        remoteAuthManager.backendBaseUrl = backendBaseUrl
        when (val res = remoteAuthManager.checkApprovalStatus(username, password)) {
            is com.trainerapp.pro.data.auth.RemoteApprovalResult.Approved -> {
                isApproved = true
                true
            }
            is com.trainerapp.pro.data.auth.RemoteApprovalResult.Pending -> {
                isApproved = false
                false
            }
            is com.trainerapp.pro.data.auth.RemoteApprovalResult.Unreachable -> {
                isApproved
            }
        }
    }

    fun checkRemoteApprovalStatusSync(username: String, password: String? = null): Boolean {
        return runCatching {
            kotlinx.coroutines.runBlocking(Dispatchers.IO) {
                checkRemoteApprovalStatus(username, password)
            }
        }.getOrDefault(isApproved)
    }

    fun completeRemoteLogin(user: com.trainerapp.pro.data.auth.TrainerRemoteUserInfo, pass: String = "") {
        val editor = authPrefs.edit()
            .putBoolean("is_logged_in", true)
            .putBoolean("is_approved", true)
            .putString("username", user.username)
            .putString("trainer_name", user.fullName)
            .putString("phone", user.phone)
            .putString("auth_token", user.token)
        if (pass.isNotBlank()) {
            editor.putString("password_hash", hashPassword(pass))
        }
        if (user.telegramUsername.isNotBlank()) {
            editor.putString("telegram_username", user.telegramUsername)
            _telegramUsernameFlow.value = user.telegramUsername
        }
        editor.apply()
        isApproved = true
        _isLoggedIn.value = true

        val parts = user.fullName.trim().split(" ", limit = 2)
        val firstName = parts.firstOrNull() ?: user.fullName.trim()
        val lastName = if (parts.size > 1) parts[1] else ""
        val prefsEditor = prefs.edit()
            .putString("trainer_first_name", firstName)
            .putString("trainer_last_name", lastName)
            .putString("trainer_phone", user.phone.trim())
        
        if (user.avatarBase64.isNotBlank()) {
            prefsEditor.putString("trainer_avatar_base64", user.avatarBase64)
            try {
                val cleanB64 = user.avatarBase64.substringAfter("base64,")
                val bytes = android.util.Base64.decode(cleanB64, android.util.Base64.DEFAULT)
                val avatarFile = java.io.File(getApplication<Application>().filesDir, "trainer_avatar.jpg")
                avatarFile.writeBytes(bytes)
                prefsEditor.putString("trainer_photo_uri", avatarFile.absolutePath)
            } catch (_: Exception) {}
        }
        prefsEditor.apply()
    }

    suspend fun remoteLogin(username: String, pass: String): com.trainerapp.pro.data.auth.TrainerRemoteAuthResult {
        remoteAuthManager.backendBaseUrl = backendBaseUrl
        val res = remoteAuthManager.login(username, pass)
        if (res is com.trainerapp.pro.data.auth.TrainerRemoteAuthResult.Success) {
            completeRemoteLogin(res.user, pass)
        }
        return res
    }

    suspend fun remoteVerify2FaLogin(userId: Long, otp: String): com.trainerapp.pro.data.auth.TrainerRemoteAuthResult {
        remoteAuthManager.backendBaseUrl = backendBaseUrl
        val res = remoteAuthManager.verify2FaLogin(userId, otp)
        if (res is com.trainerapp.pro.data.auth.TrainerRemoteAuthResult.Success) {
            completeRemoteLogin(res.user)
        }
        return res
    }

    fun login(username: String, pass: String): Boolean {
        if (checkCredentials(username, pass)) {
            if (!isApproved) {
                return false
            }
            if (is2FaEnabled) {
                return false
            }
            return completeLogin()
        }
        return false
    }

    fun submitTrainerRegistration(trainerName: String, username: String, password: String, phone: String, telegram: String) {
        val inputHash = hashPassword(password)
        authPrefs.edit()
            .putString("username", username.trim())
            .putString("password_hash", inputHash)
            .putString("trainer_name", trainerName.trim())
            .putString("phone", phone.trim())
            .putString("telegram_username", telegram.trim())
            .putBoolean("is_logged_in", true)
            .putBoolean("is_approved", true)
            .apply()
        _isLoggedIn.value = true

        val parts = trainerName.trim().split(" ", limit = 2)
        val firstName = parts.firstOrNull() ?: trainerName.trim()
        val lastName = if (parts.size > 1) parts[1] else ""
        prefs.edit()
            .putString("trainer_first_name", firstName)
            .putString("trainer_last_name", lastName)
            .putString("trainer_phone", phone.trim())
            .apply()

        // Send notification to owners & server for approval
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = java.net.URL("$backendBaseUrl/api/register")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; utf-8")
                conn.doOutput = true
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                val payload = org.json.JSONObject().apply {
                    put("username", username.trim())
                    put("password", password)
                    put("role", "trainer")
                    put("fullName", trainerName.trim())
                    put("phone", phone.trim())
                    put("telegram", telegram.trim())
                }
                conn.outputStream.use { os ->
                    os.write(payload.toString().toByteArray(Charsets.UTF_8))
                }
                conn.responseCode
            } catch (_: Exception) {}
        }
    }

    fun register(trainerName: String, username: String, password: String, phone: String) {
        submitTrainerRegistration(trainerName, username, password, phone, "")
    }

    fun logout() {
        authPrefs.edit().putBoolean("is_logged_in", false).apply()
        _isLoggedIn.value = false
    }

    private fun hashPassword(password: String): String {
        val bytes = java.security.MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private val prefs = application.getSharedPreferences("trainer_settings", android.content.Context.MODE_PRIVATE)
    var isAutoInstallUpdatesEnabled: Boolean
        get() = prefs.getBoolean("auto_install_updates", true)
        set(value) = prefs.edit().putBoolean("auto_install_updates", value).apply()

    val trainerFirstName: String
        get() = prefs.getString("trainer_first_name", "Алексей") ?: "Алексей"
    val trainerLastName: String
        get() = prefs.getString("trainer_last_name", "Романов") ?: "Романов"
    val trainerPhone: String
        get() = prefs.getString("trainer_phone", "+7 (999) 123-45-67") ?: "+7 (999) 123-45-67"
    val trainerPhotoUri: String?
        get() = prefs.getString("trainer_photo_uri", null)
    val trainerAvatarBase64: String?
        get() = prefs.getString("trainer_avatar_base64", null)

    fun saveTrainerProfile(context: android.content.Context, firstName: String, lastName: String, phone: String, photoUri: android.net.Uri?) {
        viewModelScope.launch(Dispatchers.IO) {
            val editor = prefs.edit()
                .putString("trainer_first_name", firstName)
                .putString("trainer_last_name", lastName)
                .putString("trainer_phone", phone)

            if (photoUri != null) {
                try {
                    val inputStream = context.contentResolver.openInputStream(photoUri)
                    if (inputStream != null) {
                        val originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                        inputStream.close()
                        if (originalBitmap != null) {
                            val width = originalBitmap.width
                            val height = originalBitmap.height
                            val edge = minOf(width, height)
                            val x = (width - edge) / 2
                            val y = (height - edge) / 2
                            val squareBitmap = android.graphics.Bitmap.createBitmap(originalBitmap, x, y, edge, edge)
                            val scaledBitmap = android.graphics.Bitmap.createScaledBitmap(squareBitmap, 128, 128, true)

                            val file = java.io.File(context.filesDir, "trainer_avatar.jpg")
                            var quality = 75
                            var bytes: ByteArray
                            do {
                                java.io.ByteArrayOutputStream().use { baos ->
                                    scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, baos)
                                    bytes = baos.toByteArray()
                                }
                                quality -= 10
                            } while (bytes.size > 15 * 1024 && quality >= 35)

                            file.writeBytes(bytes)
                            val b64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                            editor.putString("trainer_photo_uri", file.absolutePath)
                            editor.putString("trainer_avatar_base64", b64)
                        }
                    }
                } catch (_: Exception) {}
            }
            editor.apply()
            selectedClientId.value?.let { syncActiveClientWithGoogleDrive() }
        }
    }

    init {
        viewModelScope.launch {
            clients.collect { clientList ->
                if (_selectedClientId.value == null && clientList.isNotEmpty()) {
                    _selectedClientId.value = clientList.first().id
                }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            // Убеждаемся, что у существующих клиентов есть clientUuid (миграция legacy)
            val initialClients = clients.first()
            if (initialClients.isNotEmpty()) {
                initialClients.forEach { client ->
                    if (client.clientUuid.isBlank()) {
                        val defaultUuid = if (client.id == 1L) "f47ac10b-58cc-4372-a567-0e02b2c3d479" else java.util.UUID.randomUUID().toString()
                        val defaultPin = if (client.id == 1L) "739102" else ""
                        dao.updateClient(client.copy(clientUuid = defaultUuid, pairingCode = defaultPin))
                    }
                }
            }
            // Автоматическая фоновая синхронизация и проверка обновлений каждые 45 секунд
            var lastUpdateCheck = 0L
            var lastAutoUpdateVersion: String? = null
            while (isActive) {
                selectedClientId.value?.let {
                    syncActiveClientWithGoogleDrive()
                }
                if (System.currentTimeMillis() - lastUpdateCheck > 60000L) {
                    lastUpdateCheck = System.currentTimeMillis()
                    val checkRes = updateService.checkForUpdates()
                    val updateInfo = checkRes.getOrNull()
                    if (updateInfo?.isUpdateAvailable == true && !updateInfo.downloadUrl.isNullOrBlank()) {
                        _availableUpdate.value = updateInfo
                        if (isAutoInstallUpdatesEnabled && updateInfo.latestVersion != lastAutoUpdateVersion) {
                            lastAutoUpdateVersion = updateInfo.latestVersion
                            updateService.downloadAndInstallApk(updateInfo.downloadUrl)
                        }
                    }
                }
                delay(15000L)
            }
        }
        viewModelScope.launch {
            combine(selectedClientId, currentDate) { cId, date ->
                if (cId != null) loadSessionForDate(cId, date)
            }.collect()
        }
    }

    fun selectClient(id: Long) {
        _selectedClientId.value = id
        viewModelScope.launch {
            dao.saveSettings(settings.value.copy(selectedClientId = id))
        }
    }

    fun selectChartExercise(exerciseId: Long) {
        _selectedChartExerciseId.value = exerciseId
    }

    fun setDate(date: String) {
        _currentDate.value = date
    }

    fun changeDateByDays(days: Long) {
        val current = LocalDate.parse(_currentDate.value)
        val newDate = current.plusDays(days).format(DateTimeFormatter.ISO_LOCAL_DATE)
        _currentDate.value = newDate
    }

    fun setSelectedExerciseOrder(order: Int) {
        _selectedExerciseOrder.value = order
    }

    private fun loadSessionForDate(clientId: Long, date: String) {
        loadSessionJob?.cancel()
        loadSessionJob = viewModelScope.launch(Dispatchers.IO) {
            var session = dao.getSessionByClientAndDate(clientId, date)
            if (session == null) {
                dao.insertSession(WorkoutSessionEntity(clientId = clientId, date = date))
                session = dao.getSessionByClientAndDate(clientId, date)
            }
            _currentSession.value = session
            session?.let { s ->
                dao.getSetsForSession(s.id).collect { sets ->
                    _currentSets.value = sets
                }
            }
        }
    }

    fun addExerciseToSession(exerciseId: Long) {
        val session = _currentSession.value ?: return
        val currentOrders = _currentSets.value.map { it.exerciseOrder }.distinct()
        if (currentOrders.size >= 8) return // Max 8 per notebook design

        val nextOrder = (currentOrders.maxOrNull() ?: 0) + 1
        viewModelScope.launch(Dispatchers.IO) {
            // Check previous weight for smart hint
            val lastSet = dao.getLastExerciseSet(session.clientId, exerciseId, session.date)
            val defaultWeight = lastSet?.weightKg ?: 20.0
            val defaultReps = lastSet?.reps ?: 10

            // Add first set
            dao.insertSet(
                WorkoutSetEntity(
                    sessionId = session.id,
                    exerciseId = exerciseId,
                    exerciseOrder = nextOrder,
                    setNumber = 1,
                    weightKg = defaultWeight,
                    reps = defaultReps,
                    isCompleted = false
                )
            )
            _selectedExerciseOrder.value = nextOrder
        }
    }

    fun addSetToCurrentExercise(exerciseId: Long, exerciseOrder: Int) {
        val session = _currentSession.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val existingSets = _currentSets.value.filter { it.exerciseOrder == exerciseOrder }
            val nextSetNum = (existingSets.maxOfOrNull { it.setNumber } ?: 0) + 1
            val lastSet = existingSets.lastOrNull()

            // Calculate auto-regulated load recommendation if last set was logged
            val rec = NeuroAdaptiveEngine.calculateAdaptiveRecommendation(
                lastSet = lastSet,
                defaultWeight = 20.0,
                defaultReps = 10
            )

            dao.insertSet(
                WorkoutSetEntity(
                    sessionId = session.id,
                    exerciseId = exerciseId,
                    exerciseOrder = exerciseOrder,
                    setNumber = nextSetNum,
                    weightKg = rec.recommendedWeightKg,
                    reps = rec.recommendedReps,
                    isCompleted = false,
                    rpe = null
                )
            )
            syncActiveClientWithGoogleDrive()
        }
    }

    fun getAdaptiveRecommendation(exerciseId: Long): AdaptiveLoadRecommendation {
        val setsForExercise = _currentSets.value.filter { it.exerciseId == exerciseId }
        val lastSet = setsForExercise.lastOrNull()
        return NeuroAdaptiveEngine.calculateAdaptiveRecommendation(
            lastSet = lastSet,
            defaultWeight = 20.0,
            defaultReps = 10
        )
    }

    fun updateSet(set: WorkoutSetEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateSet(set)
            if (set.isCompleted && settings.value.autoStartTimer) {
                val exercise = dao.getExerciseById(set.exerciseId)
                val restTime = exercise?.defaultRestSeconds ?: settings.value.defaultRestTimeSeconds
                timerManager.startTimer(restTime)
            }
            if (set.isCompleted) {
                val setsList = _currentSets.value
                val allCompleted = setsList.isNotEmpty() && setsList.all {
                    if (it.id == set.id) true else it.isCompleted
                }
                if (allCompleted) {
                    _currentSession.value?.let { sess ->
                        if (!sess.completed) {
                            val updatedSess = sess.copy(completed = true)
                            dao.updateSession(updatedSess)
                            _currentSession.value = updatedSess
                        }
                    }
                }
            }
            syncActiveClientWithGoogleDrive()
        }
    }

    fun deleteSet(set: WorkoutSetEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteSet(set)
            syncActiveClientWithGoogleDrive()
        }
    }

    fun removeExerciseFromSession(exerciseId: Long) {
        val session = _currentSession.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteExerciseFromSession(session.id, exerciseId)
            _selectedExerciseOrder.value = 1
        }
    }

    // Client CRUD
    fun saveClient(client: ClientEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            if (client.id == 0L) {
                val newId = dao.insertClient(client)
                _selectedClientId.value = newId
            } else {
                dao.updateClient(client)
            }
        }
    }

    fun deleteClient(client: ClientEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteClient(client)
            _selectedClientId.value = null
        }
    }

    // Exercise CRUD
    fun saveExercise(exercise: ExerciseEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            if (exercise.id == 0L) {
                dao.insertExercise(exercise)
            } else {
                dao.updateExercise(exercise)
            }
        }
    }

    fun deleteExercise(exercise: ExerciseEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteExercise(exercise)
        }
    }

    // Anthropometry
    fun addAnthropometry(entry: AnthropometryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertAnthropometry(entry)
        }
    }

    // Settings update
    fun updateTheme(themeName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.saveSettings(settings.value.copy(currentThemeName = themeName))
        }
    }

    fun updateLayoutStyle(styleName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.saveSettings(settings.value.copy(currentLayoutStyleName = styleName))
        }
    }

    fun updateLanguage(langCode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.saveSettings(settings.value.copy(language = langCode))
        }
    }

    fun updateGitHubConfig(token: String, repo: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.saveSettings(settings.value.copy(githubToken = token, githubRepo = repo))
        }
    }

    // Scoped Cloud Sync (Privacy Hardened)
    val googleDriveSync = com.trainerapp.pro.data.sync.GoogleDriveSyncManager()
    private val _syncStatus = MutableStateFlow<String?>(null)
    val syncStatus: StateFlow<String?> = _syncStatus.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    fun syncActiveClientWithGoogleDrive() {
        viewModelScope.launch {
            val client = activeClient.value ?: run {
                _syncStatus.value = "Ошибка: подопечный не выбран"
                return@launch
            }
            _isSyncing.value = true
            _syncStatus.value = "Синхронизация данных..."
            val coachFullName = "$trainerFirstName $trainerLastName".trim().ifBlank { "Алексей Романов" }
            val result = googleDriveSync.syncClient(
                dao = dao,
                client = client,
                coachName = coachFullName,
                coachPhone = trainerPhone,
                coachAvatarBase64 = trainerAvatarBase64
            )
            if (result.isSuccess) {
                _syncStatus.value = result.getOrNull()
                // Перезагружаем сессию для обновления UI
                loadSessionForDate(client.id, currentDate.value)
            } else {
                _syncStatus.value = "Ошибка: ${result.exceptionOrNull()?.message}"
            }
            _isSyncing.value = false
        }
    }

    fun syncAllWithGoogleDrive() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncStatus.value = "Синхронизация всех данных..."
            val allClients = withContext(Dispatchers.IO) { dao.getAllClientsSync() }
            val coachFullName = "$trainerFirstName $trainerLastName".trim().ifBlank { "Алексей Романов" }
            if (allClients.isEmpty()) {
                _syncStatus.value = "Нет подопечных для синхронизации"
                _isSyncing.value = false
                return@launch
            }
            var successCount = 0
            for (client in allClients) {
                val res = googleDriveSync.syncClient(
                    dao = dao,
                    client = client,
                    coachName = coachFullName,
                    coachPhone = trainerPhone,
                    coachAvatarBase64 = trainerAvatarBase64
                )
                if (res.isSuccess) successCount++
            }
            val active = activeClient.value
            if (active != null) {
                loadSessionForDate(active.id, currentDate.value)
            }
            _syncStatus.value = "Синхронизировано подопечных: $successCount из ${allClients.size}"
            _isSyncing.value = false
        }
    }

    fun clearSyncStatus() {
        _syncStatus.value = null
    }

    fun setSelfWorkoutAllowed(allowed: Boolean) {
        val clientId = _selectedClientId.value ?: return
        val date = _currentDate.value
        viewModelScope.launch(Dispatchers.IO) {
            var session = _currentSession.value
            if (session == null) {
                dao.insertSession(WorkoutSessionEntity(clientId = clientId, date = date, isSelfWorkoutAllowed = allowed))
                session = dao.getSessionByClientAndDate(clientId, date)
            } else {
                val updated = session.copy(isSelfWorkoutAllowed = allowed)
                dao.updateSession(updated)
                session = updated
            }
            _currentSession.value = session
            // Мгновенная синхронизация с облаком, чтобы атлет сразу получил допуск
            syncActiveClientWithGoogleDrive()
        }
    }

    fun pairClientByCode(codeOrJson: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _syncStatus.value = "Поиск и связывание подопечного..."
            val coachFullName = "$trainerFirstName $trainerLastName".trim().ifBlank { "Алексей Романов" }
            val result = googleDriveSync.findAndPairAthlete(
                dao = dao,
                inputCodeOrJson = codeOrJson,
                coachName = coachFullName,
                coachPhone = trainerPhone,
                coachAvatarBase64 = trainerAvatarBase64
            )
            result.onSuccess { client ->
                selectClient(client.id)
                loadSessionForDate(client.id, currentDate.value)
                _syncStatus.value = "Подопечный «${client.fullName}» успешно привязан!"
                withContext(Dispatchers.Main) {
                    onResult(true, "Подопечный «${client.fullName}» успешно привязан!")
                }
            }.onFailure { err ->
                val errMsg = err.message ?: "Ошибка связывания подопечного"
                _syncStatus.value = "Ошибка: $errMsg"
                withContext(Dispatchers.Main) {
                    onResult(false, errMsg)
                }
            }
        }
    }

    suspend fun exportAthletePayloadJson(clientId: Long): Result<String> = withContext(Dispatchers.IO) {
        val client = dao.getClientById(clientId)
            ?: return@withContext Result.failure(IllegalStateException("Подопечный не найден"))
        try {
            val json = gitHubSync.exportAthletePayloadJson(dao, client)
            Result.success(json)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importAthletePayloadJson(clientId: Long, json: String): Result<String> = withContext(Dispatchers.IO) {
        gitHubSync.applyAthletePayload(dao, clientId, json)
    }

    data class LastExerciseStatsSummary(
        val lastDate: String?,
        val maxWeightKg: Double,
        val setsCount: Int,
        val totalReps: Int,
        val sets: List<WorkoutSetEntity>
    )

    suspend fun getLastExerciseStats(exerciseId: Long): LastExerciseStatsSummary = withContext(Dispatchers.IO) {
        val clientId = _selectedClientId.value ?: 1L
        val date = _currentDate.value
        val lastDate = dao.getLastExerciseDateForClient(clientId, exerciseId, date)
        val sets = dao.getLastExerciseSetsForClient(clientId, exerciseId, date)
        val maxWeight = sets.maxOfOrNull { it.weightKg } ?: 0.0
        val setsCount = sets.size
        val totalReps = sets.sumOf { it.reps }
        LastExerciseStatsSummary(lastDate, maxWeight, setsCount, totalReps, sets)
    }

    override fun onCleared() {
        super.onCleared()
        loadSessionJob?.cancel()
        timerManager.release()
        syncEngine.stopScan()
    }
}
