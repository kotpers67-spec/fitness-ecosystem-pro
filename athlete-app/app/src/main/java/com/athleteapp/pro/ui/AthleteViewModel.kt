package com.athleteapp.pro.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.athleteapp.pro.data.local.AthleteDatabase
import com.athleteapp.pro.data.local.dao.AthleteSetHistory
import com.athleteapp.pro.data.local.entities.*
import com.athleteapp.pro.data.sync.AthleteSyncManager
import com.athleteapp.pro.data.update.AthleteUpdateService
import com.athleteapp.pro.domain.calculators.FatigueLevel
import com.athleteapp.pro.domain.calculators.NeuroAdaptiveEngine
import com.athleteapp.pro.domain.calculators.NeuroRecommendation
import com.athleteapp.pro.domain.calculators.SessionReadiness
import com.athleteapp.pro.domain.timer.RestTimerManager
import com.athleteapp.pro.ui.screens.LeaderboardEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class AthleteViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AthleteDatabase.getDatabase(application)
    private val dao = database.athleteDao()
    val syncManager = AthleteSyncManager(dao)
    val updateService = AthleteUpdateService(application)
    val timerManager = RestTimerManager(application)
    val neuroEngine = NeuroAdaptiveEngine()
    val backupManager = com.athleteapp.pro.data.backup.AthleteBackupManager(dao)

    // Current Date
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val _selectedDate = MutableStateFlow(dateFormat.format(Date()))
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    // State Flows from Room
    val profile: StateFlow<AthleteProfileEntity?> = dao.getProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val settings: StateFlow<AthleteAppSettingsEntity?> = dao.getSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AthleteAppSettingsEntity())

    val exercises: StateFlow<List<AssignedExerciseEntity>> = dao.getAllExercises()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val anthropometry: StateFlow<List<MyAnthropometryEntity>> = dao.getAllAnthropometry()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSessions: StateFlow<List<MyWorkoutSessionEntity>> = dao.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSets: StateFlow<List<MyWorkoutSetEntity>> = dao.getAllSets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Current Session & Sets
    private val _currentSession = MutableStateFlow<MyWorkoutSessionEntity?>(null)
    val currentSession: StateFlow<MyWorkoutSessionEntity?> = _currentSession.asStateFlow()

    private val _currentSets = MutableStateFlow<List<MyWorkoutSetEntity>>(emptyList())
    val currentSets: StateFlow<List<MyWorkoutSetEntity>> = _currentSets.asStateFlow()

    // Neuro-Adaptive Session Readiness & Fatigue
    val sessionReadiness: StateFlow<SessionReadiness> = _currentSets
        .map { sets -> neuroEngine.calculateSessionReadiness(sets) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            SessionReadiness(100, 0, FatigueLevel.FRESH, 0.0, 0, 0.0)
        )

    // Selected Exercise for history charts
    private val _selectedExerciseId = MutableStateFlow<Long?>(null)
    val selectedExerciseId: StateFlow<Long?> = _selectedExerciseId.asStateFlow()

    private val _exerciseHistory = MutableStateFlow<List<AthleteSetHistory>>(emptyList())
    val exerciseHistory: StateFlow<List<AthleteSetHistory>> = _exerciseHistory.asStateFlow()

    // UI Status / Messages
    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _availableUpdate = MutableStateFlow<com.athleteapp.pro.data.update.AthleteUpdateCheckResult?>(null)
    val availableUpdate: StateFlow<com.athleteapp.pro.data.update.AthleteUpdateCheckResult?> = _availableUpdate.asStateFlow()

    // Safe coroutine job holders to prevent Flow leaks
    private var sessionJob: Job? = null
    private var historyJob: Job? = null

    private val authPrefs = application.getSharedPreferences("athlete_auth", Context.MODE_PRIVATE)
    private val _isLoggedIn = MutableStateFlow(authPrefs.getBoolean("is_logged_in", false))
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    val remoteAuthManager = com.athleteapp.pro.data.auth.AthleteRemoteAuthManager()

    companion object {
        var backendBaseUrl: String = "https://fitness-ecosystem-pro.onrender.com"
    }

    var pinCreatedAt: Long
        get() = authPrefs.getLong("pin_created_at", 0L)
        set(value) = authPrefs.edit().putLong("pin_created_at", value).apply()

    private val _pinSecondsRemaining = MutableStateFlow(300)
    val pinSecondsRemaining: StateFlow<Int> = _pinSecondsRemaining.asStateFlow()

    private var pinTickerJob: Job? = null

    fun startPinTicker() {
        pinTickerJob?.cancel()
        pinTickerJob = viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                val prof = profile.value
                val isPaired = prof?.isPairedWithCoach == true
                if (isPaired) {
                    _pinSecondsRemaining.value = 300
                } else {
                    val now = System.currentTimeMillis()
                    var created = pinCreatedAt
                    if (created <= 0L) {
                        created = now
                        pinCreatedAt = now
                    }
                    val elapsedSec = ((now - created).coerceAtLeast(0L) / 1000L).toInt()
                    val remaining = (300 - elapsedSec).coerceAtLeast(0)
                    _pinSecondsRemaining.value = remaining
                }
                delay(1000L)
            }
        }
    }

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

    fun completeRemoteLogin(user: com.athleteapp.pro.data.auth.AthleteRemoteUserInfo, pass: String = "") {
        val editor = authPrefs.edit()
            .putBoolean("is_logged_in", true)
            .putString("username", user.username)
            .putString("full_name", user.fullName)
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
        _isLoggedIn.value = true

        viewModelScope.launch {
            val current = profile.value ?: AthleteProfileEntity()
            val pin = if (user.pairingCode.isNotBlank()) user.pairingCode else if (current.pairingPin.length == 6) current.pairingPin else String.format(Locale.US, "%06d", Random().nextInt(1000000))
            
            var savedPath = current.avatarPath
            val b64 = if (user.avatarBase64.isNotBlank()) user.avatarBase64 else current.avatarBase64
            if (user.avatarBase64.isNotBlank()) {
                try {
                    val cleanB64 = user.avatarBase64.substringAfter("base64,")
                    val bytes = android.util.Base64.decode(cleanB64, android.util.Base64.DEFAULT)
                    val avatarFile = java.io.File(getApplication<Application>().filesDir, "athlete_avatar.jpg")
                    avatarFile.writeBytes(bytes)
                    savedPath = avatarFile.absolutePath
                } catch (_: Exception) {}
            }

            val clientUuidToUse = user.clientUuid.ifBlank { current.clientUuid.ifBlank { "ath_${user.id}" } }
            dao.saveProfile(
                current.copy(
                    clientUuid = clientUuidToUse,
                    fullName = user.fullName.ifBlank { current.fullName },
                    phone = user.phone.ifBlank { current.phone },
                    restrictions = user.restrictions.ifBlank { current.restrictions },
                    pairingPin = pin,
                    avatarBase64 = b64,
                    avatarPath = savedPath,
                    photoUri = savedPath
                )
            )
            autoSync()
        }
    }

    suspend fun remoteLogin(username: String, pass: String): com.athleteapp.pro.data.auth.AthleteRemoteAuthResult {
        remoteAuthManager.backendBaseUrl = backendBaseUrl
        val res = remoteAuthManager.login(username, pass)
        if (res is com.athleteapp.pro.data.auth.AthleteRemoteAuthResult.Success) {
            completeRemoteLogin(res.user, pass)
        }
        return res
    }

    suspend fun remoteVerify2Fa(userId: Long, otp: String): com.athleteapp.pro.data.auth.AthleteRemoteAuthResult {
        remoteAuthManager.backendBaseUrl = backendBaseUrl
        val res = remoteAuthManager.verify2FaOtp(userId, otp)
        if (res is com.athleteapp.pro.data.auth.AthleteRemoteAuthResult.Success) {
            completeRemoteLogin(res.user)
        }
        return res
    }

    fun verify2FaOtp(otp: String): Boolean {
        val clean = otp.filter { it.isDigit() }
        if (clean.length == 6) {
            completeLogin()
            return true
        }
        return false
    }

    fun login(username: String, pass: String): Boolean {
        if (checkCredentials(username, pass)) {
            if (is2FaEnabled) {
                // Requires 2FA verification step in UI
                return false
            }
            return completeLogin()
        }
        return false
    }

    suspend fun remoteRegister(fullName: String, username: String, password: String, phone: String): com.athleteapp.pro.data.auth.AthleteRemoteAuthResult {
        remoteAuthManager.backendBaseUrl = backendBaseUrl
        val res = remoteAuthManager.register(fullName, username, password, phone)
        if (res is com.athleteapp.pro.data.auth.AthleteRemoteAuthResult.Success) {
            completeRemoteLogin(res.user, password)
        } else if (res is com.athleteapp.pro.data.auth.AthleteRemoteAuthResult.OfflineFallback) {
            register(fullName, username, password, phone)
        }
        return res
    }

    fun register(fullName: String, username: String, password: String, phone: String) {
        val inputHash = hashPassword(password)
        val now = System.currentTimeMillis()
        pinCreatedAt = now
        authPrefs.edit()
            .putString("username", username.trim())
            .putString("password_hash", inputHash)
            .putString("full_name", fullName.trim())
            .putString("phone", phone.trim())
            .putBoolean("is_logged_in", true)
            .apply()
        _isLoggedIn.value = true

        viewModelScope.launch {
            val current = profile.value ?: AthleteProfileEntity()
            val pin = if (current.pairingPin.length == 6) current.pairingPin else String.format(Locale.US, "%06d", Random().nextInt(1000000))
            dao.saveProfile(
                current.copy(
                    fullName = fullName.trim(),
                    phone = phone.trim(),
                    pairingPin = pin
                )
            )
            autoSync()
        }
    }

    fun logout() {
        authPrefs.edit().putBoolean("is_logged_in", false).apply()
        _isLoggedIn.value = false
    }

    private fun hashPassword(password: String): String {
        val bytes = java.security.MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private val prefs = application.getSharedPreferences("athlete_settings", Context.MODE_PRIVATE)
    var isAutoInstallUpdatesEnabled: Boolean
        get() = prefs.getBoolean("auto_install_updates", true)
        set(value) = prefs.edit().putBoolean("auto_install_updates", value).apply()

    init {
        if (pinCreatedAt <= 0L) {
            pinCreatedAt = System.currentTimeMillis()
        }
        startPinTicker()
        loadSessionForDate(_selectedDate.value)

        viewModelScope.launch {
            exercises.collect { list ->
                if (_selectedExerciseId.value == null && list.isNotEmpty()) {
                    selectExerciseForHistory(list.first().id)
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            var lastUpdateCheck = 0L
            var lastAutoUpdateVersion: String? = null
            while (isActive) {
                autoSync()
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
    }

    fun changeDate(offsetDays: Int) {
        val cal = Calendar.getInstance()
        try {
            cal.time = dateFormat.parse(_selectedDate.value) ?: Date()
        } catch (e: Exception) {
            cal.time = Date()
        }
        cal.add(Calendar.DAY_OF_YEAR, offsetDays)
        val newDate = dateFormat.format(cal.time)
        _selectedDate.value = newDate
        loadSessionForDate(newDate)
    }

    fun setDate(date: String) {
        _selectedDate.value = date
        loadSessionForDate(date)
    }

    private fun loadSessionForDate(date: String) {
        // Cancel existing collector to avoid Flow leak on date change
        sessionJob?.cancel()
        sessionJob = viewModelScope.launch {
            var session = dao.getSessionByDate(date)
            if (session == null) {
                val newSession = MyWorkoutSessionEntity(date = date, notes = "")
                val id = dao.insertSession(newSession)
                session = newSession.copy(id = id)
            }
            _currentSession.value = session

            dao.getSetsForSession(session.id).collect { sets ->
                _currentSets.value = sets
            }
        }
    }

    fun toggleSetCompletion(set: MyWorkoutSetEntity) {
        viewModelScope.launch {
            val updated = set.copy(isCompleted = !set.isCompleted)
            dao.updateSet(updated)
            if (updated.isCompleted) {
                val s = settings.value
                val duration = s?.defaultRestTimeSeconds ?: 90
                timerManager.startTimer(duration)
            }

            // Check if all sets for today's workout session are now completed
            val session = _currentSession.value
            if (session != null) {
                val allSets = dao.getSetsForSessionSync(session.id)
                val allCompleted = allSets.isNotEmpty() && allSets.all { it.isCompleted }
                if (allCompleted && !session.completed) {
                    val completedSession = session.copy(completed = true)
                    dao.updateSession(completedSession)
                    _currentSession.value = completedSession
                    if (completedSession.isSelfWorkoutAllowed) {
                        autoSync()
                    }
                }
            }
        }
    }

    fun updateSetValues(set: MyWorkoutSetEntity, weight: Double, reps: Int) {
        viewModelScope.launch {
            val updated = set.copy(actualWeightKg = weight, actualReps = reps)
            dao.updateSet(updated)
        }
    }

    fun updateSetRpe(set: MyWorkoutSetEntity, rpe: Double) {
        viewModelScope.launch {
            val updated = set.copy(rpe = rpe)
            dao.updateSet(updated)
        }
    }

    fun getRecommendationForExercise(exerciseId: Long): NeuroRecommendation {
        val exerciseSets = _currentSets.value.filter { it.exerciseId == exerciseId }
        val matchedEx = exercises.value.find { it.id == exerciseId }
        val currentFatigue = sessionReadiness.value.fatiguePercent
        return neuroEngine.calculateSetRecommendation(exerciseSets, matchedEx, currentFatigue)
    }

    fun addSetToExercise(exercise: AssignedExerciseEntity) {
        viewModelScope.launch {
            val session = _currentSession.value ?: return@launch
            val existingSets = _currentSets.value.filter { it.exerciseId == exercise.id }
            val nextSetNum = existingSets.size + 1
            val rec = neuroEngine.calculateSetRecommendation(
                existingSets,
                exercise,
                sessionReadiness.value.fatiguePercent
            )

            val newSet = MyWorkoutSetEntity(
                sessionId = session.id,
                exerciseId = exercise.id,
                exerciseName = exercise.name,
                muscleGroup = exercise.muscleGroup,
                exerciseOrder = (existingSets.firstOrNull()?.exerciseOrder ?: (_currentSets.value.map { it.exerciseOrder }.maxOrNull() ?: 0) + 1),
                setNumber = nextSetNum,
                targetWeightKg = rec.recommendedWeightKg,
                targetReps = rec.recommendedReps,
                actualWeightKg = rec.recommendedWeightKg,
                actualReps = rec.recommendedReps,
                isCompleted = false,
                rpe = rec.targetRpe
            )
            dao.insertSet(newSet)
        }
    }

    fun createSelfExercise(name: String, muscleGroup: String, initialWeight: Double, initialReps: Int) {
        viewModelScope.launch {
            val session = _currentSession.value ?: return@launch
            val cleanName = name.trim().ifBlank { "Упражнение" }
            val group = muscleGroup.trim().ifBlank { "Общая" }
            
            // Check if exercise entity already exists or insert new
            val existingEx = exercises.value.find { it.name.equals(cleanName, ignoreCase = true) }
            val exId = existingEx?.id ?: dao.insertExercise(
                AssignedExerciseEntity(
                    name = cleanName,
                    muscleGroup = group,
                    defaultRestSeconds = 90
                )
            )

            val maxOrder = _currentSets.value.map { it.exerciseOrder }.maxOrNull() ?: 0
            val newSet = MyWorkoutSetEntity(
                sessionId = session.id,
                exerciseId = exId,
                exerciseName = cleanName,
                muscleGroup = group,
                exerciseOrder = maxOrder + 1,
                setNumber = 1,
                targetWeightKg = initialWeight.coerceAtLeast(0.0),
                targetReps = initialReps.coerceAtLeast(1),
                actualWeightKg = initialWeight.coerceAtLeast(0.0),
                actualReps = initialReps.coerceAtLeast(1),
                isCompleted = false,
                rpe = 8.0
            )
            dao.insertSet(newSet)
            autoSync()
        }
    }

    fun deleteSet(set: MyWorkoutSetEntity) {
        viewModelScope.launch {
            dao.deleteSet(set)
        }
    }

    fun selectExerciseForHistory(exerciseId: Long) {
        _selectedExerciseId.value = exerciseId
        historyJob?.cancel()
        historyJob = viewModelScope.launch {
            dao.getExerciseHistory(exerciseId).collect { history ->
                _exerciseHistory.value = history
            }
        }
    }

    fun saveAnthropometry(weight: Double) {
        viewModelScope.launch {
            val today = dateFormat.format(Date())
            dao.insertAnthropometry(MyAnthropometryEntity(date = today, weightKg = weight))
        }
    }

    fun updateLanguage(lang: String) {
        viewModelScope.launch {
            val current = settings.value ?: AthleteAppSettingsEntity()
            dao.saveSettings(current.copy(language = lang))
        }
    }

    fun updateTheme(themeName: String) {
        viewModelScope.launch {
            val current = settings.value ?: AthleteAppSettingsEntity()
            dao.saveSettings(current.copy(currentThemeName = themeName))
        }
    }

    fun updateGitHubConfig(token: String, repo: String, owner: String, athleteId: Long) {
        viewModelScope.launch {
            val current = settings.value ?: AthleteAppSettingsEntity()
            dao.saveSettings(
                current.copy(
                    githubToken = token,
                    githubRepo = repo,
                    coachGitHubOwner = owner,
                    athleteId = athleteId
                )
            )
        }
    }

    val googleDriveSync = com.athleteapp.pro.data.sync.GoogleDriveAthleteSyncManager(dao)
    val cloudAthletes: StateFlow<List<LeaderboardEntry>> = googleDriveSync.cloudAthletes

    fun updateProfile(fullName: String, phone: String, goal: String, restrictions: String) {
        viewModelScope.launch {
            val current = profile.value ?: AthleteProfileEntity()
            val updated = current.copy(
                fullName = fullName,
                phone = phone,
                goal = goal,
                restrictions = restrictions
            )
            dao.saveProfile(updated)
            _syncMessage.value = "Профиль успешно сохранен"
            // Push profile to web server if auth token is present
            val token = authPrefs.getString("auth_token", "") ?: ""
            if (token.isNotBlank()) {
                remoteAuthManager.backendBaseUrl = backendBaseUrl
                remoteAuthManager.updateProfile(
                    authToken = token,
                    fullName = fullName,
                    phone = phone,
                    avatarBase64 = updated.avatarBase64,
                    restrictions = restrictions,
                    clientUuid = updated.clientUuid
                )
            }
            // Quietly update cloud pairing / profile data
            autoSync()
        }
    }

    fun saveAvatar(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@launch
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                if (originalBitmap == null) return@launch

                val width = originalBitmap.width
                val height = originalBitmap.height
                val edge = minOf(width, height)
                val x = (width - edge) / 2
                val y = (height - edge) / 2
                val squareBitmap = Bitmap.createBitmap(originalBitmap, x, y, edge, edge)
                val scaledBitmap = Bitmap.createScaledBitmap(squareBitmap, 128, 128, true)

                val avatarFile = File(context.filesDir, "athlete_avatar.jpg")
                var quality = 75
                var bytes: ByteArray
                do {
                    java.io.ByteArrayOutputStream().use { baos ->
                        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
                        bytes = baos.toByteArray()
                    }
                    quality -= 10
                } while (bytes.size > 15 * 1024 && quality >= 35)

                avatarFile.writeBytes(bytes)
                val base64Str = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                val savedPath = avatarFile.absolutePath
                val current = profile.value ?: AthleteProfileEntity()
                dao.saveProfile(current.copy(avatarPath = savedPath, photoUri = savedPath, avatarBase64 = base64Str))
                _syncMessage.value = "Фото профиля обновлено"
                val token = authPrefs.getString("auth_token", "") ?: ""
                if (token.isNotBlank()) {
                    remoteAuthManager.backendBaseUrl = backendBaseUrl
                    remoteAuthManager.updateProfile(
                        authToken = token,
                        fullName = current.fullName,
                        phone = current.phone,
                        avatarBase64 = base64Str,
                        restrictions = current.restrictions,
                        clientUuid = current.clientUuid
                    )
                }
                autoSync()
            } catch (e: Exception) {
                _syncMessage.value = "Ошибка сохранения фото: ${e.message}"
            }
        }
    }

    fun regeneratePairingPin() {
        viewModelScope.launch {
            val current = profile.value ?: AthleteProfileEntity()
            val newPin = String.format(Locale.US, "%06d", (100000..999999).random())
            val now = System.currentTimeMillis()
            pinCreatedAt = now
            _pinSecondsRemaining.value = 300
            val updated = current.copy(
                pairingPin = newPin,
                isPairedWithCoach = false,
                pairedCoachName = "",
                pairedCoachPhone = "",
                pairedCoachPhotoUri = null,
                pairedCoachAvatarBase64 = null
            )
            dao.saveProfile(updated)
            _syncMessage.value = "Сгенерирован новый PIN: ${newPin.substring(0, 3)}-${newPin.substring(3)}"
            googleDriveSync.syncWithCoach(clientUuidOverride = updated.clientUuid, pinCreatedAt = now)
        }
    }

    fun unpairFromCoach() {
        viewModelScope.launch {
            _syncMessage.value = "Отключение от тренера..."
            val result = googleDriveSync.unpairFromCoach()
            _syncMessage.value = if (result.isSuccess) result.getOrNull() else "Ошибка: ${result.exceptionOrNull()?.message}"
        }
    }

    fun setPrivateLeaderboard(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = profile.value ?: AthleteProfileEntity()
            dao.saveProfile(current.copy(isPrivateLeaderboard = enabled))
            _syncMessage.value = if (enabled) "Приватность включена: Вы скрыты в состязаниях" else "Приватность отключена: Вы участвуете в состязаниях"
            autoSync()
        }
    }

    fun autoSync() {
        viewModelScope.launch(Dispatchers.IO) {
            val current = settings.value ?: AthleteAppSettingsEntity()
            val athleteId = if (current.athleteId > 0) current.athleteId else 1L
            val prof = dao.getProfile().firstOrNull() ?: AthleteProfileEntity()
            if (prof.pairingPin.isBlank()) {
                val initialPin = String.format("%06d", (100000..999999).random())
                dao.saveProfile(prof.copy(pairingPin = initialPin))
            }
            // Pull latest remote profile from web server if auth token is active
            val token = authPrefs.getString("auth_token", "") ?: ""
            if (token.isNotBlank()) {
                remoteAuthManager.backendBaseUrl = backendBaseUrl
                val remoteProfile = remoteAuthManager.fetchCurrentProfile(token)
                if (remoteProfile != null) {
                    val freshProf = dao.getProfile().firstOrNull() ?: prof
                    var savedPath = freshProf.avatarPath
                    if (remoteProfile.avatarBase64.isNotBlank() && remoteProfile.avatarBase64 != freshProf.avatarBase64) {
                        try {
                            val cleanB64 = remoteProfile.avatarBase64.substringAfter("base64,")
                            val bytes = android.util.Base64.decode(cleanB64, android.util.Base64.DEFAULT)
                            val avatarFile = java.io.File(getApplication<Application>().filesDir, "athlete_avatar.jpg")
                            avatarFile.writeBytes(bytes)
                            savedPath = avatarFile.absolutePath
                        } catch (_: Exception) {}
                    }
                    val updatedProf = freshProf.copy(
                        clientUuid = remoteProfile.clientUuid.ifBlank { freshProf.clientUuid },
                        fullName = remoteProfile.fullName.ifBlank { freshProf.fullName },
                        phone = remoteProfile.phone.ifBlank { freshProf.phone },
                        restrictions = remoteProfile.restrictions.ifBlank { freshProf.restrictions },
                        pairingPin = if (remoteProfile.pairingCode.length == 6) remoteProfile.pairingCode else freshProf.pairingPin,
                        avatarBase64 = if (remoteProfile.avatarBase64.isNotBlank()) remoteProfile.avatarBase64 else freshProf.avatarBase64,
                        avatarPath = savedPath,
                        photoUri = savedPath
                    )
                    if (updatedProf != freshProf) {
                        dao.saveProfile(updatedProf)
                    }
                }
            }

            val result = googleDriveSync.syncWithCoach(athleteId)
            if (result.isSuccess) {
                loadSessionForDate(_selectedDate.value)
            }
        }
    }

    fun syncWithCoachGoogleDrive() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncMessage.value = "Синхронизация данных..."
            val current = settings.value ?: AthleteAppSettingsEntity()
            val athleteId = if (current.athleteId > 0) current.athleteId else 1L
            val result = googleDriveSync.syncWithCoach(athleteId)
            if (result.isSuccess) {
                loadSessionForDate(_selectedDate.value)
                _syncMessage.value = result.getOrNull()
            } else {
                _syncMessage.value = "Ошибка: ${result.exceptionOrNull()?.message}"
            }
            _isSyncing.value = false
        }
    }

    fun syncWithCoach() {
        viewModelScope.launch {
            _syncMessage.value = "Синхронизация с тренером..."
            val current = settings.value ?: AthleteAppSettingsEntity()
            val result = syncManager.syncWithCoach(
                owner = current.coachGitHubOwner,
                repo = current.githubRepo,
                token = current.githubToken,
                athleteId = current.athleteId
            )
            _syncMessage.value = if (result.isSuccess) {
                // Refresh local session display
                loadSessionForDate(_selectedDate.value)
                result.getOrNull() ?: "Успешно синхронизировано!"
            } else {
                "Ошибка: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        pinTickerJob?.cancel()
        sessionJob?.cancel()
        historyJob?.cancel()
        timerManager.release()
    }
}
