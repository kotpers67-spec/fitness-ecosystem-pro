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
                            val scaledBitmap = android.graphics.Bitmap.createScaledBitmap(squareBitmap, 512, 512, true)

                            val file = java.io.File(context.filesDir, "trainer_avatar.jpg")
                            java.io.FileOutputStream(file).use { out ->
                                scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, out)
                            }
                            val bytes = file.readBytes()
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
            val clientList = clients.filter { it.isNotEmpty() }.first()
            clientList.forEach { client ->
                if (client.clientUuid.isBlank()) {
                    val defaultUuid = if (client.id == 1L) "f47ac10b-58cc-4372-a567-0e02b2c3d479" else java.util.UUID.randomUUID().toString()
                    val defaultPin = if (client.id == 1L) "739102" else ""
                    dao.updateClient(client.copy(clientUuid = defaultUuid, pairingCode = defaultPin))
                }
            }
            // Автоматическая фоновая синхронизация и проверка обновлений каждые 45 секунд
            var lastUpdateCheck = 0L
            var lastAutoUpdateVersion: String? = null
            while (true) {
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
                delay(45000L)
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
                    syncActiveClientWithGoogleDrive()
                }
            }
        }
    }

    fun deleteSet(set: WorkoutSetEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteSet(set)
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

    fun syncActiveClientWithGoogleDrive() {
        viewModelScope.launch {
            val client = activeClient.value ?: run {
                _syncStatus.value = "Ошибка: подопечный не выбран"
                return@launch
            }
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
        }
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
