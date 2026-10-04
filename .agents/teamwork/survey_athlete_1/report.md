# Полный отчёт аудита Athlete Pro (Android App)

**Дата проведения**: 2026-10-04  
**Аудитор**: Survey Explorer 1 (`survey_athlete_1`)  
**Объект аудита**: `F:\Projects\fitness-ecosystem-pro\athlete-app`  
**Версия приложения**: `versionCode = 8`, `versionName = "1.0.8"` (ранее "1.0.5")  
**Результат unit-тестов**: 39 из 39 PASS (100% успех, 0.854s)  
**Результат сборки APK**: `assembleRelease` SUCCESS (APK: `13 042 913` байт, ~12.4 МБ)

---

## 1. Архитектура приложения и стек технологий

### 1.1 Архитектурный паттерн
- **UI Toolkit**: Jetpack Compose (BOM 2024.06.00, Material 3 1.2.x, Material Icons Extended).
- **Паттерн**: MVVM (Model-View-ViewModel) на Kotlin Coroutines & StateFlow.
- **Навигация**: `androidx.navigation.compose:navigation-compose:2.7.7` (`NavHost` с 4 основными маршрутами в нижней панели + экран авторизации `AthleteAuthScreen`).
- **Локальная БД**: Room SQLite (`androidx.room:room-runtime:2.6.1` с KSP `2.0.0-1.0.24`, схема v4).
- **Безопасность и шифрование**: AES-256 (SHA-256 key hashing + PKCS5Padding) через `CloudSecurityManager.kt`.
- **Сеть и синхронизация**: `HttpURLConnection` со строгой обработкой HTTP 302/307 редиректов и Google Apps Script API.
- **Генерация QR-кодов**: `com.google.zxing:core:3.5.3`.
- **Фоновые обновления**: `AthleteUpdateService.kt` + `InstallReceiver.kt` (`DownloadManager` + `FileProvider` + `REQUEST_INSTALL_PACKAGES`).

---

## 2. Структура экранов и пользовательский опыт (UI/UX)

### 2.1 Навигация (`MainActivity.kt`)
1. **Неавторизованное состояние**: `AthleteAuthScreen` (Вход / Регистрация, fast-login через `@fitnessecosystemBOT`, валидация 6-значного 2FA OTP, ссылки на владельцев `@SantiLA213` и `@Spirit5449`).
2. **Авторизованное состояние** (`Scaffold` с `NavigationBar`):
   - `today` → `AthleteTodayScreen`: Дневник тренировок на сегодня / выбранную дату.
   - `history` → `AthleteHistoryScreen`: Динамика веса тела и силовая кривая упражнений.
   - `leaderboard` → `LeaderboardScreen`: Состязания и таблица лидеров (Zero-Mocks).
   - `settings` → `AthleteSettingsScreen`: Профиль, 6-значный динамический PIN, QR-код, карточка тренера, 2FA, тема, язык, бэкапы, контакты владельцев.

### 2.2 Дневник тренировок (`AthleteTodayScreen.kt`)
- **Переключатель даты**: кнопки «Вчера» / «Завтра» и диалог даты.
- **Баннер режима доступа**:
  - `isSelfWorkoutAllowed == true`: «Самостоятельная тренировка разрешена» — атлет может вводить фактический вес, повторения и отмечать сеты.
  - `isSelfWorkoutAllowed == false`: «Тренировка с тренером (Только чтение)» — поля ввода заблокированы для предотвращения расхождения с тренером в зале.
- **Карточки упражнений**: отображение мышечной группы, плановых (target) и фактических (actual) весов/повторений, чекбоксы завершения, ввод RPE (усилия).
- **Плавающий таймер отдыха**: `RestTimerFloatingBanner` — таймер обратного отсчета с кнопками +30с, сброса, звуковыми бипами (`ToneGenerator`) и тактильной вибрацией (`VibratorManager`).

### 2.3 Динамика веса и замеры (`AthleteHistoryScreen.kt`)
- **Замеры веса**: отображение текущего и стартового веса. Диалог внесения замера (`showWeightDialog`): вес (кг), обхват груди, талии, бёдер, бицепса.
- **Canvas-графики**:
  - `AthleteWeightLineChart`: гладкая векторная кривая веса тела с узловыми точками и значениями. При < 2 замеров отображает информативное пустое состояние («Добавьте минимум 2 замера для графика»).
  - `AthleteExerciseLineChart`: прогресс максимального рабочего веса по выбранному упражнению (горизонтальные чипсы упражнений).

### 2.4 Состязания и таблица лидеров (`LeaderboardScreen.kt`)
- **Полное соответствие Zero-Mocks**:
  - Все заглушечные имена («Максим Громов», «Елена Соколова», «Дмитрий Воронов», «Ольга Морозова») полностью удалены из кода.
  - Участники формируются строго из:
    1. Самого атлета (`myEntry`), если у него есть хотя бы 1 выполненная тренировка или тоннаж > 0, и приватность выключена.
    2. Реальных подопечных из облака (`_cloudAthletes` из узла `clients` Google Drive), у которых `workoutsCount > 0` или `tonnageKg > 0`.
- **Подсчёт очков**:
  $$\text{Points} = \text{WorkoutsCount} \times 10 + \lfloor\frac{\text{TonnageKg}}{100}\rfloor$$
- **Тумблер приватности**: при `isPrivateLeaderboard == true` атлет скрывается из рейтинга, выводится специальный баннер и Empty State.
- **Пустое состояние**: аккуратный Bento-блок с иконкой и поясняющим текстом.

### 2.5 Настройки, профиль, PIN и QR (`AthleteSettingsScreen.kt`)
- **Сжатие фото (CursorWindow Fix)**: выбор фото из галереи (`photoPickerLauncher`), автоматическое кадрирование в квадрат, масштабирование до 128x128, сжатие JPEG 75% с циклом уменьшения качества до достижения размера < 15 КБ. Исключает `SQLiteBlobTooBigException`.
- **Динамический 6-значный PIN**: крупный моноширинный шрифт с межбуквенным интервалом (`letterSpacing = 6.sp`), без дефисов.
- **Живой таймер 05:00**: моноширинный счетчик времени с `LinearProgressIndicator`, смена цвета на акцентный красный при < 60 секунд.
- **Кнопки передачи кода**:
  - «Скопировать»: копирует `https://fitnessapp.pro/pair?code=$cleanPin` в буфер обмена с Toast.
  - «Отправить»: вызывает системный `Intent.ACTION_SEND` с текстом и ссылкой.
  - «Сгенерировать новый код»: принудительный сброс и генерация нового PIN с синхронизацией в облако.
- **QR-код**: генерируется на лету через `QrCodeView` (ZXing, 512x512, контрастная белая подложка).
- **Карточка тренера**: при статусе «СВЯЗАН» отображает имя, телефон, фото/аватар тренера, кнопку прямого звонка (`Intent.ACTION_DIAL`) и кнопку «Отвязаться от тренера».
- **Telegram & 2FA**:
  - Привязка Telegram через бота `@FitnessEcosystemBot?start=link_$clientUuid`.
  - Тумблер 2FA: требует подтверждения кодом из Telegram.
- **Прямая связь с владельцами**: кнопки с прямыми ссылками на `https://t.me/SantiLA213` и `https://t.me/Spirit5449`.

---

## 3. База данных Room и структуры данных

### 3.1 Схема таблиц (`AthleteDatabase.kt`, версия 4)
1. `athlete_profile` (`AthleteProfileEntity`):
   - `id: Long = 1`
   - `clientUuid: String` (UUID v4, изоляция в облаке)
   - `athleteIdInCoachBase: Long`
   - `fullName`, `phone`, `goal`, `notes`, `restrictions`
   - `avatarPath`, `photoUri`, `avatarBase64` (размер < 15 КБ)
   - `pairingPin: String` (6 цифр)
   - `isPairedWithCoach: Boolean`
   - `pairedCoachName`, `pairedCoachPhone`, `pairedCoachPhotoUri`, `pairedCoachAvatarBase64`
   - `isPrivateLeaderboard: Boolean`
2. `assigned_exercises` (`AssignedExerciseEntity`):
   - `id`, `name`, `muscleGroup`, `defaultRestSeconds`, `description`
3. `my_workout_sessions` (`MyWorkoutSessionEntity`):
   - `id`, `date` (YYYY-MM-DD), `notes`, `completed`, `isSelfWorkoutAllowed: Boolean`
4. `my_workout_sets` (`MyWorkoutSetEntity`):
   - `id`, `sessionId` (FK cascade), `exerciseId` (FK cascade), `exerciseName`, `muscleGroup`
   - `exerciseOrder`, `setNumber`, `targetWeightKg`, `targetReps`, `actualWeightKg`, `actualReps`, `isCompleted`, `rpe: Double?`
5. `my_anthropometry` (`MyAnthropometryEntity`):
   - `id`, `date`, `weightKg`, `chestCm`, `waistCm`, `hipsCm`, `bicepsCm`
6. `athlete_app_settings` (`AthleteAppSettingsEntity`):
   - `id = 1`, `currentThemeName`, `language`, `githubToken`, `githubRepo`, `coachGitHubOwner`, `athleteId`, `autoStartTimer`, `defaultRestTimeSeconds`

### 3.2 Миграции базы данных
- `MIGRATION_1_2`: пересоздание `my_workout_sets` с внешними ключами и индексами.
- `MIGRATION_2_3`: добавление `clientUuid`, `restrictions`, `pairingPin`, `isPairedWithCoach`, `isSelfWorkoutAllowed`.
- `MIGRATION_3_4`: добавление `avatarBase64`, контактов тренера (`pairedCoachPhone`, `pairedCoachAvatarBase64`) и `isPrivateLeaderboard`.
- При `onCreate` база сидируется пустым профилем с уникальным UUID, рандомным 6-значным PIN и 5 базовыми упражнениями (никаких фейковых пользователей).

---

## 4. Динамический 5-минутный PIN и синхронизация

### 4.1 Логика таймера и авто-ротации (`AthleteViewModel.kt`)
- `pinCreatedAt`: хранится в `SharedPreferences` ("pin_created_at").
- `startPinTicker()`: корутина запускается в `init`, выполняет тик раз в секунду:
  ```kotlin
  val elapsedSec = ((now - created).coerceAtLeast(0L) / 1000L).toInt()
  val remaining = (300 - elapsedSec).coerceAtLeast(0)
  _pinSecondsRemaining.value = remaining
  if (remaining <= 0) {
      regeneratePairingPin()
  }
  ```
- При истечении 300 секунд (или нажатии кнопки «Сгенерировать новый код»):
  1. Генерируется случайный 6-значный код: `(100000..999999).random()`.
  2. Обновляется метка времени `pinCreatedAt = now`.
  3. Сбрасывается статус привязки к тренеру.
  4. Сохраняется в Room DB.
  5. Немедленно вызывается фоновый `googleDriveSync.syncWithCoach(...)` с передачей `pinCreatedAt`.

### 4.2 Протокол облачной синхронизации (`GoogleDriveAthleteSyncManager.kt`)
- **Шифрование AES-256**:
  - `CloudSecurityManager.encryptPayload()`: хэширование секретного ключа через SHA-256 → AES/ECB/PKCS5Padding → префикс `"ENC:"` + Base64.
  - `CloudSecurityManager.decryptPayload()`: проверка префикса `"ENC:"`, Base64-декодирование, AES-дешифрование.
- **Хэндшейк привязки (`pairing`)**:
  - Публикуется узел `pairing[cleanPin]`:
    - `pin`, `clientUuid`, `clientName`, `phone`, `goal`, `restrictions`, `notes`, `avatarBase64`, `timestamp`, `status` ("PENDING" / "PAIRED").
  - Автоматическая очистка: удаление устаревших записей для того же `clientUuid` с другим PIN.
- **Строгая изоляция клиентов (`clients`)**:
  - Атлет считывает и перезаписывает строго свой ключ: `rootObj["clients"][clientUuid]`.
  - Чужие данные не сохраняются в локальные таблицы и не перезаписывают данные атлета.
- **Отвязка (`unpairFromCoach`)**:
  - Генерирует новый UUID и PIN, удаляет старый PIN из облака, отправляет состояние "PENDING".

---

## 5. Результаты выполнения тестов и верификации

### 5.1 Gradle Unit Tests (`testDebugUnitTest`)
Команда: `.\gradlew.bat testDebugUnitTest --no-daemon`
- **Всего тестов**: 39
- **Провалено**: 0
- **Пропущено**: 0
- **Успешность**: 100% (время выполнения: 0.854 с)

Детализация по тест-сьютам:
1. `com.athleteapp.pro.AthletePinAnd2FaTest` (5 тестов):
   - `testPinCountdown_calculatedCorrectly` — PASS
   - `testPinCountdown_expiresAfter300Seconds` — PASS
   - `testPinCountdown_zeroWhenNegativeElapsedTime` — PASS
   - `testTelegramUsernameFormatting` — PASS
   - `test2FaOtpValidation` — PASS
2. `com.athleteapp.pro.data.sync.AthleteIsolationAndPairingTest` (6 тестов):
   - `testClientUuid_persistedAndIsolatedInSyncPayload` — PASS
   - `testIsSelfWorkoutAllowed_importedAndSavedIntoWorkoutSession` — PASS
   - `testIsSelfWorkoutAllowed_falseByDefaultEnforcesReadOnly` — PASS
   - `testDataIsolation_multiClientCloudPayloadDoesNotLeakToAthlete` — PASS
   - `testPairingQrJsonFormat` — PASS
   - `testUnpairResetProfile_clearsCoachPhoneAndPhotos` — PASS
3. `com.athleteapp.pro.data.sync.AthleteSyncRemediationTest` (10 тестов):
   - `testCorruptedJson_returnsFailureExplicitly` — PASS
   - `testMalformedJson_returnsFailureExplicitly` — PASS
   - `testAnthropometryAndNameImported_whenAssignedWorkoutsEmpty` — PASS
   - `testExistingSessionUpdate_usesUpdateSessionToPreventCascadeDeletion` — PASS
   - `testLeaderboardPrivacyFilter_omitsCurrentUserWhenPrivate` — PASS
   - `testLeaderboardPrivacyFilter_retainsAllWhenNotPrivate` — PASS
   - `testFakeAthleteDao_getAllSets_returnsAllSetsAcrossSessions` — PASS
   - `testLeaderboard_showsOnlyCurrentUserWhenNoCompetitors` — PASS
   - `testLeaderboard_emptyStateWhenPrivateAndNoCompetitors` — PASS
   - `testPairingLinkFormat_andShareText` — PASS
4. `com.athleteapp.pro.domain.calculators.NeuroAdaptiveEngineTest` (6 тестов):
   - `testEstimated1RM_validCalculations` — PASS
   - `testTonnageCalculation` — PASS
   - `testSessionReadiness_emptyIsFresh` — PASS
   - `testSessionReadiness_accumulatedFatigue` — PASS
   - `testSetRecommendation_progressiveOverloadOnEasySet` — PASS
   - `testSetRecommendation_deloadOnExtremeFatigue` — PASS
5. `com.athleteapp.pro.domain.calculators.NeuroAdaptiveStressTest` (12 тестов):
   - `testEmptySets_tonnageAndReadiness` — PASS
   - `testEmptySets_recommendationFallback` — PASS
   - `testExtremeValues_highRepsAndHeavyWeights` — PASS
   - `testSuperExtremeValues_1000kgAnd1000reps` — PASS
   - `testRpeBoundaries_zeroTenNullNegative` — PASS
   - `testSetRecommendation_zeroTargetReps_safeLowerBoundEnforced` — PASS
   - `testSetRecommendation_unratedRpeMaintainsWeightWithoutOverload` — PASS
   - `testSetRecommendation_zeroTargetRepsAcrossAllCases` — PASS
   - `testSetRecommendation_deloadOnFailure` — PASS
   - `testMathematicalSoundness_zeroWeightAndReps` — PASS
   - `testProgressionRunawaySimulation` — PASS
   - `testUncompletedSetsCountedInTonnageSemantics` — PASS

### 5.2 Сборка релиза (`assembleRelease`)
Команда: `.\gradlew.bat assembleRelease --no-daemon`
- **Результат**: BUILD SUCCESSFUL in 11s (47 задач).
- **Сформированный артефакт**: `athlete-app/app/build/outputs/apk/release/app-release.apk`
- **Размер**: 13,042,913 байт (~12.4 МБ).
- **Подпись**: подписан релизным конфигом (keystore).
- **Метаданные**: `versionCode = 8`, `versionName = "1.0.8"`.

---

## 6. Обнаруженные дефекты, несоответствия и пограничные случаи

### 6.1 Несоответствие версий в интерфейсе обновления
- **Локация**: `AthleteSettingsScreen.kt`, строки 966, 969.
- **Проблема**: В тексте статуса обновления захардкожена версия `v1.0.5`:
  `"У вас установлена актуальная версия Athlete Pro (v1.0.5)."`
  При этом в `build.gradle.kts` приложение уже имеет версию `1.0.8`.
- **Рекомендация**: Заменить строку на динамическую интерполяцию текущей версии:
  `"У вас установлена актуальная версия Athlete Pro (v$currentVersionName)."`

### 6.2 Дефолтная версия в `AthleteUpdateService.kt`
- **Локация**: `AthleteUpdateService.kt`, строка 50.
- **Проблема**: В `getCurrentVersionName()` fallback при ошибке возвращает `"1.0.6"`.
- **Рекомендация**: Обновить дефолт на `"1.0.8"`.

### 6.3 Моковые имена в fallback облачной синхронизации
- **Локация**: `GoogleDriveAthleteSyncManager.kt`:
  - Строка 66: `val coachName = if (pinEntry.has("coachName")) pinEntry.get("coachName").asString else "Алексей Романов"`
  - Строка 67: `val coachPhone = if (pinEntry.has("coachPhone")) pinEntry.get("coachPhone").asString else "+7 (999) 123-45-67"`
  - Строка 188: `clientName = currentProfile.fullName.ifBlank { "Александр Смирнов" }`
- **Проблема**: Если тренер не передал имя или атлет не указал имя в профиле, подставляются старые тестовые имена («Алексей Романов», «Александр Смирнов»), что нарушает строгий принцип Zero-Mocks.
- **Рекомендация**: Заменить fallback на нейтральные значения: `"Тренер"`, `""`, `"Атлет"`.

---

## 7. Сводная оценка готовности к релизу

| Компонент / Требование | Статус | Комментарий |
|---|---|---|
| Дневник тренировок & Room DB | 100% ГОТОВ | Таблицы, миграции, каскады, права Read-Only / Self |
| Замеры веса и графики | 100% ГОТОВ | Сохранение антропометрии, Canvas-графики, пустые состояния |
| 5-минутный PIN & QR-код | 100% ГОТОВ | Таймер 05:00, авто-ротация, ZXing QR, кнопки Copy/Share |
| Google Drive AES-256 | 100% ГОТОВ | Шифрование с префиксом `ENC:`, PULL/PUSH, изоляция по UUID |
| Защита CursorWindow (фото) | 100% ГОТОВ | 128x128, JPEG 75%, < 15 КБ |
| Leaderboard Zero-Mocks | 100% ГОТОВ | Заглушки удалены, фильтрация по приватности |
| Telegram 2FA & Контакты | 100% ГОТОВ | Вход по OTP, бот, кнопки @SantiLA213 и @Spirit5449 |
| Unit-тесты | 100% PASS | 39 из 39 тестов успешно проходят |
| Релизная сборка APK | 100% ГОТОВ | `assembleRelease` собирает валидный APK (12.4 МБ) |
