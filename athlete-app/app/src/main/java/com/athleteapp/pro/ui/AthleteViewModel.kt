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

    private val _availableUpdate = MutableStateFlow<com.athleteapp.pro.data.update.AthleteUpdateCheckResult?>(null)
    val availableUpdate: StateFlow<com.athleteapp.pro.data.update.AthleteUpdateCheckResult?> = _availableUpdate.asStateFlow()

    // Safe coroutine job holders to prevent Flow leaks
    private var sessionJob: Job? = null
    private var historyJob: Job? = null

    private val prefs = application.getSharedPreferences("athlete_settings", Context.MODE_PRIVATE)
    var isAutoInstallUpdatesEnabled: Boolean
        get() = prefs.getBoolean("auto_install_updates", true)
        set(value) = prefs.edit().putBoolean("auto_install_updates", value).apply()

    init {
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
            while (true) {
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
                delay(45000L)
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
                val scaledBitmap = Bitmap.createScaledBitmap(squareBitmap, 512, 512, true)

                val avatarFile = File(context.filesDir, "athlete_avatar.jpg")
                FileOutputStream(avatarFile).use { out ->
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }

                val bytes = avatarFile.readBytes()
                val base64Str = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                val savedPath = avatarFile.absolutePath
                val current = profile.value ?: AthleteProfileEntity()
                dao.saveProfile(current.copy(avatarPath = savedPath, photoUri = savedPath, avatarBase64 = base64Str))
                _syncMessage.value = "Фото профиля обновлено"
                autoSync()
            } catch (e: Exception) {
                _syncMessage.value = "Ошибка сохранения фото: ${e.message}"
            }
        }
    }

    fun regeneratePairingPin() {
        viewModelScope.launch {
            val current = profile.value ?: AthleteProfileEntity()
            val newPin = String.format("%06d", (100000..999999).random())
            val updated = current.copy(
                pairingPin = newPin,
                isPairedWithCoach = false,
                pairedCoachName = ""
            )
            dao.saveProfile(updated)
            _syncMessage.value = "Сгенерирован новый PIN: ${newPin.substring(0, 3)}-${newPin.substring(3)}"
            googleDriveSync.syncWithCoach(clientUuidOverride = updated.clientUuid)
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
            val result = googleDriveSync.syncWithCoach(athleteId)
            if (result.isSuccess) {
                loadSessionForDate(_selectedDate.value)
            }
        }
    }

    fun syncWithCoachGoogleDrive() {
        viewModelScope.launch {
            _syncMessage.value = "Синхронизация через Google Диск..."
            val current = settings.value ?: AthleteAppSettingsEntity()
            val athleteId = if (current.athleteId > 0) current.athleteId else 1L
            val result = googleDriveSync.syncWithCoach(athleteId)
            if (result.isSuccess) {
                loadSessionForDate(_selectedDate.value)
                _syncMessage.value = result.getOrNull()
            } else {
                _syncMessage.value = "Ошибка: ${result.exceptionOrNull()?.message}"
            }
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
        sessionJob?.cancel()
        historyJob?.cancel()
        timerManager.release()
    }
}
