# Полный сквозной аудит приложения Athlete Pro (v1.0.4 -> v1.0.5)

**Дата аудита**: 2026-10-03  
**Проводящий**: Explorer 2 (Athlete Pro Codebase Explorer)  
**Репозиторий**: `F:\Projects\fitness-ecosystem-pro\athlete-app`  
**Целевая версия**: `versionCode = 5`, `versionName = "1.0.5"`

---

## Резюме аудита
Проведен детальный read-only аудит исходного кода, базы данных Room, компонентов Jetpack Compose, подсистемы облачной синхронизации, автообновлений и набора юнит-тестов приложения **Athlete Pro**.

Обнаружены **3 критических дефекта**, **1 сбой компиляции тестов** и **ряд нарушений стандартов UI/UX и безопасности**:
1. **Сбой компиляции юнит-тестов Gradle (`./gradlew testDebugUnitTest`)**: класс `FakeAthleteDao` в `AthleteSyncRemediationTest.kt:164` не реализует метод `getAllSets()`, добавленный в интерфейс `AthleteDao`. Сборка тестов падает с кодом 1.
2. **Инверсия фильтрации приватности в состязаниях (`LeaderboardScreen.kt:68`)**: условие `.filter { !isPrivate || it.isMe }` при включенной приватности скрывает всех реальных соперников и оставляет в таблице только самого пользователя ("ВЫ"), что прямо противоположно логике режима приватности.
3. **Утечка персональных данных тренера при отвязке (`GoogleDriveAthleteSyncManager.kt:212` и `AthleteViewModel.kt:356`)**: методы `unpairFromCoach()` и `regeneratePairingPin()` не очищают `pairedCoachPhone`, `pairedCoachPhotoUri` и `pairedCoachAvatarBase64`, сохраняя номер телефона и фото старого тренера в локальной базе.
4. **Передача открытого JSON без шифрования AES-256 при отвязке (`GoogleDriveAthleteSyncManager.kt:237`)**: `unpairFromCoach()` отправляет в облако сырой `gson.toJson(rootObj)` без вызова `CloudSecurityManager.encryptPayload()`, нарушая регламент защиты `ENC:`.
5. **Нежелательные упоминания сторонних провайдеров в UI (`Google Диск`, `GitHub`)**: в уведомлениях `_syncMessage`, статусах обновлений и резервных текстах открыто отображаются строки "Google Диск" и "GitHub", а также захардкожена устаревшая версия `v1.0.1`.
6. **Несоответствие версии в build.gradle.kts**: текущие значения `versionCode = 4`, `versionName = "1.0.4"` вместо требуемых `versionCode = 5`, `versionName = "1.0.5"`.

---

## 1. Фото и Аватарки (Синхронизация, Base64, Кэширование)

### 1.1. Кодирование и сохранение аватара атлета
- **Файл**: `app/src/main/java/com/athleteapp/pro/ui/AthleteViewModel.kt` (строки 318–350, функция `saveAvatar`)
- **Механизм**:
  1. URI выбранного изображения декодируется через `BitmapFactory.decodeStream(inputStream)`.
  2. Выполняется кадрирование до центрального квадрата (`minOf(width, height)`) и сжатие до 512x512 (`Bitmap.createScaledBitmap`).
  3. Сохраняется в приватное хранилище `context.filesDir/athlete_avatar.jpg` (JPEG, качество 85).
  4. Кодируется в Base64 (`android.util.Base64.encodeToString(bytes, NO_WRAP)`).
  5. В `AthleteProfileEntity` записываются `avatarPath`, `photoUri` (абсолютный путь к файлу) и `avatarBase64`.
  6. При синхронизации `GoogleDriveAthleteSyncManager` передает `avatarBase64` в облачный узел `clients[clientUuid].avatarBase64` в зашифрованном виде AES-256.

### 1.2. Отображение, кэширование и устойчивость к ошибкам
- **Файл**: `app/src/main/java/com/athleteapp/pro/ui/components/CommonComponents.kt` (строки 33–81, компонент `AthleteAvatar`)
- **Логика**:
  ```kotlin
  val bitmap = remember(avatarPath) {
      if (!avatarPath.isNullOrBlank()) {
          val file = File(avatarPath)
          if (file.exists()) {
              BitmapFactory.decodeFile(avatarPath)?.asImageBitmap()
          } else {
              try {
                  val cleanB64 = if (avatarPath.contains(",")) avatarPath.substringAfter(",") else avatarPath
                  val bytes = android.util.Base64.decode(cleanB64, android.util.Base64.NO_WRAP)
                  BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
              } catch (_: Exception) {
                  null
              }
          }
      } else null
  }
  ```
- **Оценка отказоустойчивости**:
  - Пустые, null или битые строки Base64/URI перехватываются блоком `catch` и гарантированно отображают векторную заглушку `defaultResId` (`R.drawable.avatar_athlete` или `R.drawable.avatar_coach`).
  - **Замечание по производительности**: передача длинной строки Base64 (>50KB) в `File(avatarPath).exists()` вызывает лишнюю проверку файловой системы на строке с недопустимой длиной пути. Рекомендуется проверять `avatarPath.startsWith("/")` перед обращением к `File.exists()`.

---

## 2. Карточка тренера в Athlete Pro

### 2.1. Получение данных при синхронизации
- **Файл**: `app/src/main/java/com/athleteapp/pro/data/sync/GoogleDriveAthleteSyncManager.kt` (строки 50–80)
- **Логика**:
  - Из облачного узла `pairing[cleanPin]` считываются:
    `coachName` (по умолчанию "Алексей Романов"), `coachPhone` (по умолчанию "+7 (999) 123-45-67"), `coachAvatarBase64`.
  - Если `status == "PAIRED"`, в `AthleteProfileEntity` сохраняются флаг `isPairedWithCoach = true`, имя, телефон и Base64 аватара тренера.
  - Если `status == "UNPAIRED"` или `"PENDING"`, сбрасываются `isPairedWithCoach = false`, имя, телефон и аватар.

### 2.2. Отображение в UI
- **Файл**: `app/src/main/java/com/athleteapp/pro/ui/screens/AthleteSettingsScreen.kt` (строки 283–383)
- **Элементы**:
  - Статусный бейдж: "СВЯЗАН" (primary container) / "ОЖИДАНИЕ" (secondary container).
  - Аватар тренера через `AthleteAvatar(avatarPath = profile?.pairedCoachPhotoUri ?: profile?.pairedCoachAvatarBase64, size = 52.dp, defaultResId = R.drawable.avatar_coach)`.
  - Имя тренера и номер телефона.

### 2.3. Кнопка звонка тренеру (Intent)
- **Файл**: `app/src/main/java/com/athleteapp/pro/ui/screens/AthleteSettingsScreen.kt` (строки 370–379)
- **Код**:
  ```kotlin
  val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$coachPhone"))
  context.startActivity(intent)
  ```
- **Оценка**: реализовано безопасно через `ACTION_DIAL`. Не требует опасного runtime-разрешения `CALL_PHONE`, открывает системный диалер с предзаполненным номером.

### 2.4. Дефекты отвязки от тренера (Unlink Action)
1. **Неполный сброс состояния в БД (Утечка данных)**:
   - В `GoogleDriveAthleteSyncManager.kt:212` метод `unpairFromCoach()` выполняет:
     ```kotlin
     val updated = current.copy(
         isPairedWithCoach = false,
         pairedCoachName = "",
         pairingPin = newPin,
         clientUuid = newUuid
     )
     ```
     **Поля `pairedCoachPhone`, `pairedCoachPhotoUri`, `pairedCoachAvatarBase64` НЕ сбрасываются!** Телефон и аватар отвязанного тренера остаются сохраненными в профиле.
   - Аналогично в `AthleteViewModel.kt:356` (`regeneratePairingPin()`):
     ```kotlin
     val updated = current.copy(
         pairingPin = newPin,
         isPairedWithCoach = false,
         pairedCoachName = ""
     )
     ```
     Здесь также не сбрасываются телефон и аватар тренера.
2. **Передача незашифрованных данных в облако**:
   - В `GoogleDriveAthleteSyncManager.kt:237`:
     ```kotlin
     httpPost(requestUrl, gson.toJson(rootObj))
     ```
     Вместо `val encryptedJson = CloudSecurityManager.encryptPayload(gson.toJson(rootObj))` отправляется открытый текст, что нарушает стандарт `ENC:`.

---

## 3. Состязания (Состязания / Leaderboard)

### 3.1. Подсчет очков и тоннажа
- **Файл**: `app/src/main/java/com/athleteapp/pro/ui/screens/LeaderboardScreen.kt` (строки 45–50)
- **Формула**:
  ```kotlin
  val completedWorkouts = sessions.count { it.completed }
  val myTonnage: Double = sets.filter { it.isCompleted }.fold(0.0) { acc, s -> acc + (s.actualWeightKg * s.actualReps) }
  val myPoints: Int = completedWorkouts * 10 + (myTonnage / 100.0).toInt()
  ```
- **Оценка**: подсчет корректен. 1 выполненная тренировка = 10 очков, 100 кг выполненного тоннажа = 1 очко. Незавершенные подходы и тренировки отфильтрованы.

### 3.2. Рейтинг и сортировка
- Список участников формируется с учетом прогресса атлета и сортируется по убыванию очков: `.sortedByDescending { it.points }`.
- Текущий пользователь визуально выделен бейджем "ВЫ" и акцентной рамкой `primary`.

### 3.3. Критический дефект: тумблер приватности (Privacy Toggle)
- **Файл**: `app/src/main/java/com/athleteapp/pro/ui/screens/LeaderboardScreen.kt` (строки 65–69)
- **Код**:
  ```kotlin
  val entries = remember(rawEntries, isPrivate) {
      rawEntries.mapIndexed { index, entry ->
          entry.copy(rank = index + 1)
      }.filter { !isPrivate || it.isMe }
  }
  ```
- **Суть бага**:
  - При `isPrivate == false` условие `!isPrivate` истинно — отображаются все участники.
  - При `isPrivate == true` условие `!isPrivate` ложно, остается только `it.isMe`!
  - **В результате**: когда пользователь включает тумблер «Скрыть мой профиль из состязаний», из таблицы удаляются ВСЕ ДРУГИЕ УЧАСТНИКИ, и пользователь видит в таблице только одного себя на первом месте!
  - При этом на экране отображается баннер: *"Вы включили приватный режим в настройках. Ваш профиль скрыт от других участников состязания"*.
- **Требуемое исправление**:
  Если пользователь скрыт из таблицы лидеров, таблица лидеров должна отображать остальных участников без него (`!isPrivate || !it.isMe`), либо если пользователь не участвует в рейтинге, пересчитывать ранги после фильтрации:
  ```kotlin
  val entries = remember(rawEntries, isPrivate) {
      rawEntries
          .filter { !isPrivate || !it.isMe }
          .mapIndexed { index, entry -> entry.copy(rank = index + 1) }
  }
  ```

---

## 4. Автообновление и Проверка версий

### 4.1. Фоновая проверка и служба обновлений
- **Файл**: `app/src/main/java/com/athleteapp/pro/data/update/AthleteUpdateService.kt`
- **Источники обновления**:
  1. Приоритетный: облачный зашифрованный узел `updates` на Google Диске (`athleteVersion`, `athleteUrl`, `notes`).
  2. Резервный: GitHub Releases API (`/releases/latest`).
  3. Фолбэк: захардкоженная версия `fallbackVersion = "1.0.2"` (строка 128). Требуется обновить до `1.0.5`.
- **Алгоритм сравнения версий** (`isVersionNewer`, строки 285–296):
  Поразрядное сравнение компонентов SemVer (`major.minor.patch`). Корректно определяет, что `1.0.5` новее `1.0.4`.

### 4.2. Фоновый цикл проверки
- **Файл**: `app/src/main/java/com/athleteapp/pro/ui/AthleteViewModel.kt` (строки 114–134)
- **Периодичность**: синхронизация каждые 45 с, проверка обновлений каждые 60 с.
- **Автоустановка**: если `isAutoInstallUpdatesEnabled == true`, скачивает APK через `downloadAndInstallApk`.
- **Дефект**: бесконечный цикл `while (true)` вместо `while (isActive)`.

### 4.3. Мертвый код и дефект безопасности: `InstallReceiver`
- **Файл**: `app/src/main/java/com/athleteapp/pro/data/update/InstallReceiver.kt` и `AndroidManifest.xml:28-34`
- **Проблема**:
  `InstallReceiver` зарегистрирован на широковещательное событие `DownloadManager.ACTION_DOWNLOAD_COMPLETE`. Однако `AthleteUpdateService.downloadAndInstallApk` **НЕ использует `DownloadManager`**, а загружает APK напрямую через `HttpURLConnection` в `context.cacheDir` и запускает установку вручную через `FileProvider`!
  В результате `InstallReceiver` никогда не вызывается для обновлений приложения, но слушает любые сторонние загрузки системы (`exported = true`), что является потенциальным дефектом безопасности.

---

## 5. Качество кода, UX и Стандарты

### 5.1. Anti-Overlap Guard
- Все строковые элементы в `Row` имеют `Modifier.weight(1f)` и `maxLines = 1, overflow = TextOverflow.Ellipsis` (`AthleteExerciseCard`, `AthleteTodayScreen`, `AthleteHistoryScreen`).
- Минимальный размер интерактивных зон для кликов соблюден (48x48 dp для `IconButton` и `FilledIconToggleButton`).
- Отсутствуют жесткие ограничения высоты контейнеров карточек.

### 5.2. Нежелательные упоминания сторонних провайдеров (Нарушение AC R1/R4)
В коде обнаружены открытые упоминания брендов провайдеров в текстах для пользователя:
1. `GoogleDriveAthleteSyncManager.kt:197`: `"Не удалось обновить данные на Google Диске"` -> заменить на `"Не удалось обновить данные в облаке"`.
2. `GoogleDriveAthleteSyncManager.kt:200`: `"Google Диск: данные синхронизированы!..."` -> заменить на `"Облако: данные синхронизированы!..."`.
3. `AthleteUpdateService.kt:77`: `"Новое обновление Athlete Pro 1.0.2 доступно на Google Диске"` -> заменить на `"Обновление доступно в облаке"`.
4. `AthleteUpdateService.kt:135`: `"Версия $fallbackVersion доступна на Google Диске"` -> заменить на `"Версия $fallbackVersion доступна в облаке"`.
5. `AthleteSettingsScreen.kt:734`: `"Проверка релизов на GitHub..."` -> заменить на `"Проверка обновлений..."`.
6. `AthleteSettingsScreen.kt:744, 747`: `"У вас установлена актуальная версия Athlete Pro (v1.0.1)."` -> захардкожена старая версия `v1.0.1` вместо динамической `$currentVersionName`.

### 5.3. Затенение (shadowing) переменных состояния в Compose
В `AthleteSettingsScreen.kt:125-127`:
```kotlin
var updateStatus by remember { mutableStateOf<String?>(null) }
var isCheckingUpdate by remember { mutableStateOf(false) }
var availableUpdate by remember { mutableStateOf<com.athleteapp.pro.data.update.AthleteUpdateCheckResult?>(null) }
```
Эти переменные объявлены на уровне экрана, но в блоке карточки обновлений (строки 666–668) объявлены локальные дубликаты с аналогичными именами, из-за чего внешние переменные являются мертвым кодом.

---

## 6. Конфигурация сборки (Build Configuration)

- **Файл**: `app/build.gradle.kts`
  - Текущее состояние:
    ```kotlin
    versionCode = 4
    versionName = "1.0.4"
    ```
  - Требование R2:
    ```kotlin
    versionCode = 5
    versionName = "1.0.5"
    ```
- **Файл**: `app/src/main/java/com/athleteapp/pro/data/sync/GoogleDriveAthleteSyncManager.kt:158-161`:
  Узел `defaultUpdates` содержит `1.0.4`, требуется обновить до `1.0.5`.

---

## 7. Юнит-тесты и Верификация сборки

### 7.1. Запуск команды `./gradlew testDebugUnitTest`
Выполнено: `cmd /c "gradlew.bat testDebugUnitTest"` из директории `F:\Projects\fitness-ecosystem-pro\athlete-app`.

### 7.2. Результат выполнения
**BUILD FAILED (Exit Code 1)**
```
> Task :app:compileDebugUnitTestKotlin
e: file:///F:/Projects/fitness-ecosystem-pro/athlete-app/app/src/test/java/com/athleteapp/pro/data/sync/AthleteSyncRemediationTest.kt:164:1 Class 'FakeAthleteDao' is not abstract and does not implement abstract member 'getAllSets'.
```

### 7.3. Причина и исправление
В интерфейс `AthleteDao` (строка 75) добавлен метод:
```kotlin
@Query("SELECT * FROM my_workout_sets ORDER BY sessionId ASC, exerciseOrder ASC, setNumber ASC")
fun getAllSets(): Flow<List<MyWorkoutSetEntity>>
```
Однако в тестовом двойнике `FakeAthleteDao` (`AthleteSyncRemediationTest.kt:164`) этот метод не был реализован.
Для успешного прохождения всех тестов в `FakeAthleteDao` необходимо добавить:
```kotlin
override fun getAllSets(): Flow<List<MyWorkoutSetEntity>> = flowOf(setsForSession.values.flatten())
```

---

## Чек-лист рекомендаций для реализации (Fix Plan)

| № | Файл | Необходимое изменение |
|---|------|------------------------|
| 1 | `app/build.gradle.kts` | Повысить версию: `versionCode = 5`, `versionName = "1.0.5"`. |
| 2 | `app/src/test/.../AthleteSyncRemediationTest.kt` | Добавить `override fun getAllSets(): Flow<List<MyWorkoutSetEntity>> = flowOf(setsForSession.values.flatten())` в `FakeAthleteDao`. |
| 3 | `app/src/main/.../LeaderboardScreen.kt` | Исправить условие фильтрации приватности: `rawEntries.filter { !isPrivate \|\| !it.isMe }.mapIndexed { index, entry -> entry.copy(rank = index + 1) }`. |
| 4 | `app/src/main/.../GoogleDriveAthleteSyncManager.kt` | В `unpairFromCoach()` очищать `pairedCoachPhone = ""`, `pairedCoachPhotoUri = null`, `pairedCoachAvatarBase64 = null`. Зашифровать JSON через `CloudSecurityManager.encryptPayload()` перед `httpPost`. Заменить упоминания "Google Диск" на "облако". Обновить `defaultUpdates` до `1.0.5`. |
| 5 | `app/src/main/.../AthleteViewModel.kt` | В `regeneratePairingPin()` очищать `pairedCoachPhone = ""`, `pairedCoachPhotoUri = null`, `pairedCoachAvatarBase64 = null`. В фоновом цикле заменить `while (true)` на `while (isActive)`. |
| 6 | `app/src/main/.../AthleteSettingsScreen.kt` | Заменить хардкод `v1.0.1` на `$currentVersionName`. Заменить упоминание "GitHub" на нейтральное "сервер обновлений". Удалить неиспользуемые переменные на строках 125–127. |
| 7 | `app/src/main/.../AthleteUpdateService.kt` | Заменить хардкод `fallbackVersion = "1.0.2"` на `"1.0.5"`, ссылки на релизы обновить до `v1.0.5`. Удалить упоминания "Google Диске". |
