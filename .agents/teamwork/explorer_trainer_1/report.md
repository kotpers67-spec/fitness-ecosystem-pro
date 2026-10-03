# Trainer Pro Codebase & Architecture Audit Report

## Executive Summary
Audit of Trainer Pro codebase (`F:\Projects\fitness-ecosystem-pro\trainer-app`) reveals a robust Room-based offline-first architecture with AES-256 cloud synchronization. However, critical defects were identified in:
1. **Previous Workout Stats UI**: In `WorkoutScreen.kt`, viewing exercise stats from the Add Exercise dialog is completely broken if no exercises are yet added today (`activeExerciseTriple == null`), and displays the wrong exercise name if an active exercise exists.
2. **Trainer Card Transmission**: `MainViewModel.pairClientByCode` omits the coach's configured name, phone, and avatar, defaulting to hardcoded fallback strings. Furthermore, `GoogleDriveSyncManager.syncClient` does not update the cloud `pairing` node during routine syncs.
3. **Avatar Display Consistency**: `HomeScreen.kt` hardcodes static drawable avatars instead of rendering the coach's and athlete's customized photos/avatars via `ClientAvatar`.
4. **Build & Version Bump**: `build.gradle.kts` is at `versionCode = 4`, `versionName = "1.0.4"`, requiring bump to `versionCode = 5`, `versionName = "1.0.5"`. Cloud update fallback strings also reference `1.0.4` and `1.0.2`.
5. **UI & Provider Branding**: Cloud sync status messages and update release notes leak internal provider mentions ("Google Диск").

---

## 1. Photo / Avatar Sync Audit

### 1.1 Implementation Details
- **Encoding & Storage**:
  - In `MainViewModel.kt` (`saveTrainerProfile`, lines 128–165):
    - Reads `photoUri`, decodes bitmap stream, center-crops to square, scales to 512x512.
    - Compresses to JPEG (85% quality) into internal storage: `context.filesDir/trainer_avatar.jpg`.
    - Encodes bytes to Base64 using `android.util.Base64.encodeToString(bytes, Base64.NO_WRAP)`.
    - Saves path in `trainer_photo_uri` and Base64 in `trainer_avatar_base64` in SharedPreferences (`trainer_settings`).
- **Decoding & Component**:
  - `CommonComponents.kt` (`ClientAvatar`, lines 242–290):
    - Prioritizes `photoUri` (checks `java.io.File(photoUri).exists()`).
    - If empty, checks `avatarBase64`: strips data URI prefix (`substringAfter(",")`), decodes `Base64.decode(cleanB64, Base64.NO_WRAP)`, and builds image bitmap via `BitmapFactory.decodeByteArray`.
    - Gracefully handles exceptions returning `null`.
    - Fallback: renders `defaultResId` (`R.drawable.avatar_athlete` or `R.drawable.avatar_coach`).
- **Database & Sync Integration**:
  - `ClientEntity` (`TrainerEntities.kt`, lines 22–23) has `photoUri: String?` and `avatarBase64: String?`.
  - Room Migration 3->4 (`TrainerDatabase.kt`, lines 44–49) properly adds `photoUri` and `avatarBase64` columns to `clients`.
  - `GitHubSyncManager.kt` (`buildAthletePayload`, line 85): includes `avatarBase64 = client.avatarBase64`.
  - `GitHubSyncManager.kt` (`applyAthletePayload`, lines 215–218): updates existing client in DB if athlete updated their `avatarBase64`.

### 1.2 Observed Defects & Recommendations
- **Defect 1.1**: `HomeScreen.kt` line 52 hardcodes `painter = painterResource(id = R.drawable.avatar_coach)` for the top bar coach avatar, ignoring `viewModel.trainerAvatarBase64` and `viewModel.trainerPhotoUri`.
  - *Recommendation*: Use `ClientAvatar(photoUri = viewModel.trainerPhotoUri, avatarBase64 = viewModel.trainerAvatarBase64, size = 38.dp, defaultResId = R.drawable.avatar_coach)`.
- **Defect 1.2**: `HomeScreen.kt` line 191 hardcodes `painter = painterResource(id = R.drawable.avatar_athlete)` in the active client summary card, ignoring `client.photoUri` and `client.avatarBase64`.
  - *Recommendation*: Use `ClientAvatar(photoUri = client.photoUri, avatarBase64 = client.avatarBase64, size = 36.dp, defaultResId = R.drawable.avatar_athlete)`.

---

## 2. Trainer Card Audit

### 2.1 Implementation Details
- **Settings Screen Storage**:
  - In `SettingsScreen.kt` (lines 180–248): Trainer profile card allows entering First Name, Last Name, Phone, and Photo.
  - Calls `viewModel.saveTrainerProfile(...)` and saves to SharedPreferences.
- **Serialization & Cloud Sync**:
  - In `GoogleDriveSyncManager.kt` (`findAndPairAthlete`, lines 123–128):
    ```kotlin
    suspend fun findAndPairAthlete(
        dao: TrainerDao,
        inputCodeOrJson: String,
        coachName: String = "Алексей Романов",
        coachPhone: String = "+7 (999) 123-45-67",
        coachAvatarBase64: String? = null
    ): Result<ClientEntity>
    ```
    Populates `pairingEntry` in cloud JSON: `coachName`, `coachPhone`, and `coachAvatarBase64` (lines 306–310).

### 2.2 Observed Defects & Recommendations
- **Defect 2.1 (Critical)**: In `MainViewModel.kt` (`pairClientByCode`, line 477):
  ```kotlin
  val result = googleDriveSync.findAndPairAthlete(dao, codeOrJson)
  ```
  Does NOT pass `coachName`, `coachPhone`, or `coachAvatarBase64`! Always falls back to hardcoded defaults ("Алексей Романов", "+7 (999) 123-45-67", null).
  - *Recommendation*: Pass `coachName = "$trainerFirstName $trainerLastName".trim()`, `coachPhone = trainerPhone`, and `coachAvatarBase64 = trainerAvatarBase64`.
- **Defect 2.2 (Critical)**: In `GoogleDriveSyncManager.kt` (`syncClient`, lines 27–117):
  Routine periodic sync updates only `rootObj.getAsJsonObject("clients").add(clientStorageKey, trainerPayloadElement)`. It NEVER updates the `pairing[client.pairingCode]` node. Consequently, if the trainer updates their profile in Trainer Pro Settings after initial pairing, the athlete never receives the updated name, phone, or avatar.
  - *Recommendation*: In `GoogleDriveSyncManager.syncClient`, accept coach parameters and update `pairing[client.pairingCode]` with latest `coachName`, `coachPhone`, and `coachAvatarBase64` if the entry exists in cloud.

---

## 3. Previous Workout Stats Audit

### 3.1 Implementation Details
- **DAO Queries**:
  - `TrainerDao.kt` (`getLastExerciseSetsForClient`, lines 100–111): Queries all sets from the most recent session prior to `currentDate` where `exerciseId = :exerciseId`.
  - `TrainerDao.kt` (`getLastExerciseDateForClient`, lines 113–118): Queries `MAX(s.date)` prior to `currentDate`.
  - `MainViewModel.kt` (`getLastExerciseStats`, lines 518–527): Aggregates max weight, set count, and total reps into `LastExerciseStatsSummary`.
- **UI Invocation**:
  - `WorkoutScreen.kt` (`AddExerciseToSessionDialog`, lines 720–722):
    ```kotlin
    IconButton(onClick = { onViewStats(ex.id) }) {
        Icon(Icons.Default.BarChart, contentDescription = "Статистика", tint = MaterialTheme.colorScheme.primary)
    }
    ```
    Triggers `onViewStats = { exerciseId -> scope.launch { statsSummary = viewModel.getLastExerciseStats(exerciseId); showExerciseStatsDialog = true } }`.

### 3.2 Observed Defects & Recommendations
- **Defect 3.1 (Fatal UI Glitch)**: In `WorkoutScreen.kt` line 459:
  ```kotlin
  // Exercise History / Statistics Dialog
  if (showExerciseStatsDialog && activeExerciseTriple != null) {
      val exercise = activeExerciseTriple.second
      val summary = statsSummary
  ```
  1. When starting a fresh workout with 0 exercises today, `activeExerciseTriple` is `null`. Clicking the stats button next to ANY exercise in `AddExerciseToSessionDialog` fails silently — the dialog is never rendered!
  2. When the workout already contains exercises, `activeExerciseTriple.second` represents the currently selected workout tab (e.g. "Жим лежа"), NOT the exercise whose stats icon was clicked (e.g. "Становая тяга"). The dialog header displays "Статистика: Жим лежа" while rendering stats for "Становая тяга".
  - *Recommendation*:
    1. Introduce `var selectedStatsExercise by remember { mutableStateOf<ExerciseEntity?>(null) }`.
    2. In `AddExerciseToSessionDialog.onViewStats`:
       `selectedStatsExercise = exercises.find { it.id == exerciseId }`
       `statsSummary = viewModel.getLastExerciseStats(exerciseId)`
       `showExerciseStatsDialog = true`
    3. In active exercise card stats button:
       `selectedStatsExercise = exercise`
       `statsSummary = viewModel.getLastExerciseStats(exercise.id)`
       `showExerciseStatsDialog = true`
    4. Render dialog condition: `if (showExerciseStatsDialog && selectedStatsExercise != null)`.

---

## 4. Auto-update & Version Checking Audit

### 4.1 Implementation Details
- **Update Checking Loop**:
  - `MainViewModel.kt` (lines 185–205): Coroutine runs every 45–60s on `Dispatchers.IO`. If `isAutoInstallUpdatesEnabled` is true and a newer version is found, triggers `updateService.downloadAndInstallApk`.
  - Lifecycle: Runs only while app is in memory; no WorkManager or persistent Alarm/JobScheduler is registered.
- **Update Sources & Verification**:
  - `UpdateService.kt` (`checkForUpdates`, lines 58–144):
    1. Priority 1: Google Drive cloud JSON endpoint (`updates.trainerVersion`).
    2. Priority 2: GitHub Releases API (`api.github.com/repos/santiyastudio-lgtm/fitness-ecosystem-pro/releases/latest`).
    3. Priority 3: Hardcoded fallback to `1.0.2`.
  - `UpdateService.kt` (`downloadApkDirectly`, lines 174–217):
    - Downloads directly with redirect following (up to 8 redirects).
    - Validates file size (> 2MB) and ZIP signature (`PK\x03\x04`).
  - `UpdateService.kt` (`launchApkInstallation`, lines 252–287):
    - Verifies `canRequestPackageInstalls()` on API 26+; launches `ACTION_MANAGE_UNKNOWN_APP_SOURCES` if needed.
    - Launches `ACTION_VIEW` intent with `FileProvider` URI and `FLAG_GRANT_READ_URI_PERMISSION`.
- **Version Comparison**:
  - `UpdateService.kt` (`isVersionNewer`, lines 289–300):
    - Parses dot-delimited integer tokens (`rParts` vs `lParts`).
    - Works correctly for SemVer (e.g. "1.0.5" > "1.0.4").

### 4.2 Observed Defects & Recommendations
- **Defect 4.1**: Outdated fallback version strings in `UpdateService.kt`:
  - Line 78: download URL points to `v1.0.2`.
  - Line 130: `val fallbackVersion = "1.0.2"`.
  - Line 138: fallback URL points to `v1.0.2`.
  - *Recommendation*: Update fallback version references to `1.0.5` and release URLs to `v1.0.5`.
- **Defect 4.2**: Offline behavior in `checkForUpdates`:
  When completely offline, catching network errors falls through to returning `Result.success(UpdateCheckResult(..., latestVersion = fallbackVersion))` instead of returning `Result.failure(Exception("Нет подключения к сети"))`.
  - *Recommendation*: Return `Result.failure` when all network update checks fail so the UI can report network status accurately.

---

## 5. Code Quality, Lifecycle, UX & Anti-Overlap Audit

### 5.1 Architecture & Lifecycles
- `RestTimerManager`: Properly encapsulates ToneGenerators and Vibrator. Cleans up in `release()` on `MainViewModel.onCleared()`.
- `SyncEngine`: Properly calls `scanner.stopScan(scanCallback)` and cleans up on `MainViewModel.onCleared()`.
- Edge-to-Edge: `MainActivity.kt` uses `enableEdgeToEdge()` and passes `innerPadding.calculateBottomPadding()` to account for `FloatingRestTimerOverlay`. Screen `TopAppBar`s handle system status bar insets cleanly.

### 5.2 Provider Branding & UI Terminology
- In `GoogleDriveSyncManager.kt`:
  - Line 110: `"Не удалось сохранить данные на Google Диске (ошибка HTTP)"`
  - Line 113: `"Google Диск: план для ${client.fullName} отправлен в облако. $pullMsg"`
- In `UpdateService.kt`:
  - Line 79: `"Новое обновление Trainer Pro 1.0.2 доступно на Google Диске"`
  - Line 137: `"Версия $fallbackVersion доступна на Google Диске"`
- *Recommendation*: Replace mentions of "Google Диск" with generic "Облако" / "Облачная синхронизация" to satisfy Acceptance Criteria: *"В UI отсутствуют любые артефакты верстки и нежелательные упоминания провайдеров."*

---

## 6. Build Configuration Audit

### Current vs Required
| Parameter | Current (`trainer-app/app/build.gradle.kts`) | Required (v1.0.5 Release) |
|---|---|---|
| `versionCode` | `4` (line 16) | `5` |
| `versionName` | `"1.0.4"` (line 17) | `"1.0.5"` |
| `GoogleDriveSyncManager.kt` default updates | `"1.0.4"` (line 90, 92) | `"1.0.5"` |

---

## 7. Tests Audit

### 7.1 Existing Unit Test Suites
1. `NeuroAdaptiveEngineTest.kt` (4 tests): Verifies tonnage calculation, fatigue levels, readiness dynamics, 1RM calculator.
2. `NeuroAdaptiveStressTest.kt` (3 tests): Verifies 10,000 synthetic sets, edge cases (0 reps, extreme weights), thread safety.
3. `SyncAndReadinessRemediationTest.kt` (5 tests): Verifies prevention of cascade deletion on session completion, exercise ID mapping by name, safe RPE auto-regulation, tonnage filter, readiness rest date comparison.
4. `TrainerMilestone2FeatureTest.kt` (7 tests): Verifies client entity UUID/pairing code, self-workout permission flag, DAO queries, payload serialization, search filter logic.

### 7.2 Verification Execution
- Test command: `.\gradlew.bat testDebugUnitTest`
- Execution Result: **BUILD SUCCESSFUL in 31s**. All 19 existing unit tests pass without failure.
- Recommended New Tests:
  1. `testExerciseStatsDialogSelection`: Unit test ensuring `getLastExerciseStats` correctly fetches prior workout sets for arbitrary exercise IDs.
  2. `testTrainerCardPairingTransmission`: Unit test verifying `findAndPairAthlete` and `syncClient` properly serialize custom `coachName`, `coachPhone`, and `coachAvatarBase64`.
  3. `testUpdateVersionComparison`: Unit test verifying `isVersionNewer` with "1.0.5" > "1.0.4", same version equality, and malformed tags.
