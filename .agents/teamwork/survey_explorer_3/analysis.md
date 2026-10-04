# Архитектурный аудит и технический анализ Android-приложений (Athlete Pro & Trainer Pro)

**Дата:** 2026-10-04  
**Исследователь:** Survey Explorer 3  
**Проект:** Fitness Ecosystem Pro  
**Репозиторий:** `F:\Projects\fitness-ecosystem-pro`  
**Исследуемые модули:** `athlete-app` (Athlete Pro) и `trainer-app` (Trainer Pro)

---

## 1. Резюме исследования (Executive Summary)

В ходе детального исследования Android-экосистемы выявлены текущие механизмы генерации кодов, проверки связывания, аутентификации и синхронизации:

1. **Athlete Pro (Код сопряжения и таймер):**
   - Экран: `AthleteSettingsScreen.kt` (строки 270–455). Код отображается в карточке «ПРИВЯЗКА К ТРЕНЕРУ» и дублируется в QR-виде (`QrCodeView`).
   - Текущая генерация: в `AthleteViewModel.kt` код генерируется единожды при регистрации или по ручной кнопке «Сгенерировать новый код» (`(100000..999999).random()`). Код хранится статично в таблице `athlete_profile` (колонка `pairingPin`).
   - **Дефект:** Отсутствует временная метка генерации (`pinCreatedAt`), нет 5-минутного лимита жизни, нет обратного отсчета (таймера 05:00) и автоматической перегенерации.
   - **Решение:** Добавление `pinCreatedAt: Long`, фоновый `StateFlow<Int>` таймер в `AthleteViewModel`, автоматическая ротация по истечении 300 секунд с мгновенной отправкой в облако и визуальный таймер 05:00 в UI.

2. **Trainer Pro (Валидация кода привязки):**
   - Экран: `HomeScreen.kt` (диалог `showPairingDialog`, строки 450–657). Позволяет сканировать QR, выбирать изображение или вводить 6 цифр вручную.
   - Текущая валидация: `GoogleDriveSyncManager.kt` (`findAndPairAthlete`, строки 154–360). 
   - **Критические дефекты:** 
     1) Время создания кода из облачной записи `pairing` не проверяется (принимаются коды любой давности).
     2) Статус `status == "PAIRED"` игнорируется (один и тот же код можно активировать повторно).
     3) Если код отсутствует в облаке, система **не возвращает ошибку**, а создает клиента-пустышку с именем `Подопечный 739-102`!
   - **Решение:** Внедрение строгой трехуровневой валидации: проверка наличия записи в облаке, проверка `now - timestamp <= 5 * 60 * 1000` (иначе ошибка «Срок действия кода истёк»), проверка `status != "PAIRED"` (иначе «Код уже использован»), удаление использованного кода из реестра.

3. **Профиль, Настройки, 2FA и Контакты:**
   - **Контакты владельцев (@SantiLA213, @Spirit5449):** Уже присутствуют в `AthleteSettingsScreen.kt` (строки 846–896) и `SettingsScreen.kt` (строки 820–870), но **полностью отсутствуют** на экранах входа `AthleteAuthScreen.kt` и `TrainerAuthScreen.kt`.
   - **Кнопка «Привязать Telegram»:** Отсутствует в обоих приложениях. Требуется добавить карточку со статусом привязки и запуском Telegram Deep Link (`tg://resolve?domain=...` / `https://t.me/...`).
   - **2FA (Тумблер и OTP диалог):** Отсутствует в обоих приложениях. Требуется переключатель 2FA в настройках и диалог ввода 6-значного OTP при входе.

4. **Механизм синхронизации:**
   - Android-приложения **не обращаются** к локальному веб-серверу (`http://localhost:3000`).
   - Синхронизация между Athlete Pro, Trainer Pro и Web Portal осуществляется **напрямую через общее облако Google Apps Script / Google Drive** (`CloudSecurityManager.kt`, шифрование AES-256).
   - Облачный узел `pairing[cleanPin]` является единым источником правды для 5-минутных динамических кодов и статуса PAIRED.

---

## 2. Анализ Athlete Pro: Генерация динамического PIN и таймер 05:00

### 2.1. Где сейчас отображается код
- **Файл:** `athlete-app/app/src/main/java/com/athleteapp/pro/ui/screens/AthleteSettingsScreen.kt`
- **Координаты:** Строки 270–455:
  ```kotlin
  val cleanPin = (profile?.pairingPin ?: "").filter { it.isDigit() }
  ...
  Surface(...) {
      Text(text = "КОД ПОДКЛЮЧЕНИЯ (6 ЦИФР)")
      Text(text = cleanPin, style = MaterialTheme.typography.headlineLarge...)
      Text(text = "Вводится тренером слитно, без дефиса")
  }
  QrCodeView(content = cleanPin, ...)
  OutlinedButton(onClick = { viewModel.regeneratePairingPin() }) {
      Text("Сгенерировать новый код")
  }
  ```

### 2.2. Как сейчас устроен жизненный цикл кода
1. В `AthleteViewModel.kt` (строка 128) при регистрации:
   ```kotlin
   val pin = if (current.pairingPin.length == 6) current.pairingPin 
             else String.format(Locale.US, "%06d", Random().nextInt(1000000))
   ```
2. В `AthleteViewModel.kt` (строка 411) при ручной перегенерации:
   ```kotlin
   fun regeneratePairingPin() {
       val newPin = String.format("%06d", (100000..999999).random())
       dao.saveProfile(current.copy(pairingPin = newPin, isPairedWithCoach = false...))
       googleDriveSync.syncWithCoach(clientUuidOverride = updated.clientUuid)
   }
   ```
3. В `GoogleDriveAthleteSyncManager.kt` (строки 213–228):
   ```kotlin
   val pairingNode = JsonObject().apply {
       addProperty("pin", cleanPin)
       addProperty("clientUuid", clientUuid)
       addProperty("clientName", currentProfile.fullName)
       addProperty("timestamp", System.currentTimeMillis())
       addProperty("status", if (currentProfile.isPairedWithCoach) "PAIRED" else "PENDING")
   }
   pairingObj.add(cleanPin, pairingNode)
   ```
4. **Проблема:**
   - Поле `pairingPin` в `AthleteProfileEntity` является постоянным текстовым полем без срока годности.
   - Нет фонового процесса, который отсчитывал бы 300 секунд (5 минут).
   - Нет автообновления: код висит неделями, пока пользователь не нажмет кнопку обновления.

### 2.3. Архитектура динамического PIN (5 минут) и таймера обратного отсчета

#### А. Модель данных и сохранение состояния
Для сохранения времени генерации PIN используем `SharedPreferences` (`athlete_auth` или `athlete_settings`) либо поле `pinCreatedAt: Long = 0L` в сущности профиля. 
Хранение в `SharedPreferences` предпочтительно, так как не требует деструктивной миграции Room-схемы:
```kotlin
private val authPrefs = application.getSharedPreferences("athlete_auth", Context.MODE_PRIVATE)
var pinCreatedAt: Long
    get() = authPrefs.getLong("pin_created_at", 0L)
    set(value) = authPrefs.edit().putLong("pin_created_at", value).apply()
```

#### Б. ViewModel: Таймер и авто-перегенерация (`AthleteViewModel.kt`)
Добавляем реактивный поток оставшихся секунд:
```kotlin
private val _pinSecondsRemaining = MutableStateFlow(300)
val pinSecondsRemaining: StateFlow<Int> = _pinSecondsRemaining.asStateFlow()

private var pinCountdownJob: Job? = null

fun startPinCountdown() {
    pinCountdownJob?.cancel()
    pinCountdownJob = viewModelScope.launch(Dispatchers.Default) {
        while (isActive) {
            val prof = profile.value ?: AthleteProfileEntity()
            if (!prof.isPairedWithCoach && prof.pairingPin.isNotBlank()) {
                val now = System.currentTimeMillis()
                val elapsedMs = now - pinCreatedAt
                val remainingSec = ((5 * 60 * 1000L - elapsedMs) / 1000).toInt()
                
                if (remainingSec <= 0) {
                    _pinSecondsRemaining.value = 0
                    // Автоматическая генерация нового кода при истечении
                    regeneratePairingPin()
                } else {
                    _pinSecondsRemaining.value = remainingSec
                }
            } else {
                _pinSecondsRemaining.value = 300
            }
            delay(1000L)
        }
    }
}
```

Обновленный метод `regeneratePairingPin()`:
```kotlin
fun regeneratePairingPin() {
    viewModelScope.launch {
        val current = profile.value ?: AthleteProfileEntity()
        val newPin = String.format(Locale.US, "%06d", (100000..999999).random())
        pinCreatedAt = System.currentTimeMillis()
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
        _syncMessage.value = "Сгенерирован новый PIN: $newPin"
        googleDriveSync.syncWithCoach(clientUuidOverride = updated.clientUuid)
    }
}
```

#### В. Обновление облачной очистки (`GoogleDriveAthleteSyncManager.kt`)
При публикации нового кода в облако необходимо очищать предыдущие записи для данного `clientUuid`:
```kotlin
if (cleanPin.isNotBlank()) {
    if (!rootObj.has("pairing")) {
        rootObj.add("pairing", JsonObject())
    }
    val pairingObj = rootObj.getAsJsonObject("pairing")
    // Удаляем все старые/истекшие ключи этого атлета
    val keysToRemove = mutableListOf<String>()
    for ((key, elem) in pairingObj.entrySet()) {
        val entry = elem.asJsonObject
        if (entry.get("clientUuid")?.asString == clientUuid && key != cleanPin) {
            keysToRemove.add(key)
        }
    }
    keysToRemove.forEach { pairingObj.remove(it) }

    val pairingNode = JsonObject().apply {
        addProperty("pin", cleanPin)
        addProperty("clientUuid", clientUuid)
        addProperty("clientName", currentProfile.fullName)
        addProperty("phone", currentProfile.phone)
        addProperty("timestamp", pinCreatedAt)
        addProperty("status", if (currentProfile.isPairedWithCoach) "PAIRED" else "PENDING")
    }
    pairingObj.add(cleanPin, pairingNode)
}
```

#### Г. UI в `AthleteSettingsScreen.kt`
Вместо статичной плашки добавляется таймер и индикатор:
```kotlin
val secondsLeft by viewModel.pinSecondsRemaining.collectAsState()
val timerFormatted = String.format(Locale.US, "%02d:%02d", secondsLeft / 60, secondsLeft % 60)
val isExpiringSoon = secondsLeft < 60

Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp)
) {
    Icon(
        imageVector = Icons.Default.Timer,
        contentDescription = null,
        tint = if (isExpiringSoon) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(16.dp)
    )
    Text(
        text = "Код действителен: $timerFormatted",
        style = MaterialTheme.typography.labelMedium.copy(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        ),
        color = if (isExpiringSoon) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    )
}
LinearProgressIndicator(
    progress = { secondsLeft / 300f },
    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
    color = if (isExpiringSoon) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
)
```

---

## 3. Анализ Trainer Pro: Строгая валидация кода привязки

### 3.1. Где находится экран сопряжения
- **Файл:** `trainer-app/app/src/main/java/com/trainerapp/pro/ui/screens/HomeScreen.kt`
- **Координаты:** Диалог `showPairingDialog`, строки 450–657.
- **Ввод кода:** Текстовое поле `OutlinedTextField` (строки 577–592) с плейсхолдером `"739102 (без тире)"`.
- **Функция очистки:** `extractPairingCode(input)` (строки 659–673) — автоматически фильтрует нецифровые символы и достает 6 цифр из URL или JSON.
- **Вызов:** `viewModel.pairClientByCode(codeToPair) { success, msg -> ... }` (строка 632).

### 3.2. Как сейчас работает проверка (`GoogleDriveSyncManager.kt`)
- **Координаты:** Строки 154–360 (`findAndPairAthlete`).
- **Текущий алгоритм:**
  1. Скачивает и расшифровывает AES-256 JSON облака.
  2. Ищет `cleanPin` в объекте `rootObj.getAsJsonObject("pairing")`.
  3. **ОШИБКА 1:** Не проверяет `entryObj.get("timestamp")`. Если запись сделана 5 дней назад, она все равно находится.
  4. **ОШИБКА 2:** Не проверяет `entryObj.get("status")`. Если `status == "PAIRED"`, привязка повторяется.
  5. **ОШИБКА 3 (КРИТИЧЕСКАЯ):** Строка 290–328:
     ```kotlin
     val finalUuid = athleteUuid ?: UUID.randomUUID().toString()
     val finalName = athleteName?.takeIf { it.isNotBlank() } ?: "Подопечный ${cleanPin.take(3)}-${cleanPin.takeLast(3)}"
     ...
     val newClient = ClientEntity(...)
     dao.insertClient(newClient)
     ```
     Если в облаке вообще нет записи с таким кодом, менеджер **не возвращает ошибку**, а молча создает клиента с именем `Подопечный 739-102` и возвращает успех!

### 3.3. Архитектура строгой валидации (HTTP 400 / Validation Failure)

В `GoogleDriveSyncManager.findAndPairAthlete()` внедряются строгие условия:

```kotlin
// 1. Поиск записи в облаке
var foundEntry: JsonObject? = null
var foundKey: String? = null

if (rootObj.has("pairing")) {
    val pairingObj = rootObj.getAsJsonObject("pairing")
    for ((key, elem) in pairingObj.entrySet()) {
        val digitsKey = key.filter { it.isDigit() }
        val entry = elem.asJsonObject
        val entryPin = entry.get("pin")?.asString?.filter { it.isDigit() } ?: digitsKey
        if (digitsKey == cleanPin || entryPin == cleanPin) {
            foundEntry = entry
            foundKey = key
            break
        }
    }
}

// ПРОВЕРКА 1: Код отсутствует в облаке
if (foundEntry == null) {
    return@withContext Result.failure(
        IllegalArgumentException("Код «$cleanPin» не найден в облаке. Проверьте правильность кода у подопечного.")
    )
}

// ПРОВЕРКА 2: Проверка 5-минутного окна (300 000 мс)
val timestamp = foundEntry.get("timestamp")?.asLong ?: 0L
val now = System.currentTimeMillis()
val TTL_MS = 5 * 60 * 1000L

if (timestamp > 0L && (now - timestamp > TTL_MS)) {
    // Удаляем просроченную запись из облака
    if (foundKey != null && rootObj.has("pairing")) {
        rootObj.getAsJsonObject("pairing").remove(foundKey)
        val encPost = CloudSecurityManager.encryptPayload(gson.toJson(rootObj))
        httpPost(requestUrl, encPost)
    }
    return@withContext Result.failure(
        IllegalStateException("Срок действия кода «$cleanPin» истёк (действует строго 5 минут). Попросите подопечного сгенерировать новый код.")
    )
}

// ПРОВЕРКА 3: Одноразовость — код уже использован
val currentStatus = foundEntry.get("status")?.asString ?: "PENDING"
if (currentStatus.equals("PAIRED", ignoreCase = true) || currentStatus.equals("USED", ignoreCase = true)) {
    return@withContext Result.failure(
        IllegalStateException("Код «$cleanPin» уже был использован для привязки. Для повторного подключения подопечный должен сгенерировать новый код.")
    )
}

// 2. Если все проверки пройдены:
val athleteUuid = foundEntry.get("clientUuid")?.asString 
    ?: return@withContext Result.failure(IllegalStateException("Некорректные данные профиля подопечного в облаке"))
val athleteName = foundEntry.get("clientName")?.asString?.takeIf { it.isNotBlank() } ?: "Атлет"
val athletePhone = foundEntry.get("phone")?.asString ?: ""
val athleteGoal = foundEntry.get("goal")?.asString ?: ""
val athleteNotes = foundEntry.get("notes")?.asString ?: ""

// 3. Сохранение/обновление в локальной БД тренера
...

// 4. Фиксация статуса PAIRED и потребление кода
foundEntry.addProperty("status", "PAIRED")
foundEntry.addProperty("pairedAt", now.toString())
foundEntry.addProperty("coachName", coachName)
foundEntry.addProperty("coachPhone", coachPhone)
if (!coachAvatarBase64.isNullOrBlank()) {
    foundEntry.addProperty("coachAvatarBase64", coachAvatarBase64)
}

rootObj.addProperty("updatedAt", now.toString())
val encPost = CloudSecurityManager.encryptPayload(gson.toJson(rootObj))
val postSuccess = httpPost(requestUrl, encPost)
if (!postSuccess) {
    return@withContext Result.failure(IllegalStateException("Ошибка записи статуса привязки в облако"))
}
```

---

## 4. Профиль, Настройки, Кнопка Telegram, 2FA и Контакты

### 4.1. Контакты владельцев проекта (@SantiLA213, @Spirit5449)
- **Текущее состояние:**
  - В `AthleteSettingsScreen.kt` (строки 846–896) и `SettingsScreen.kt` (строки 820–870) блок «СВЯЗЬ С ВЛАДЕЛЬЦАМИ» с рабочими ссылками `https://t.me/SantiLA213` и `https://t.me/Spirit5449` **уже реализован**.
  - На экранах авторизации `AthleteAuthScreen.kt` и `TrainerAuthScreen.kt` контакты **отсутствуют**.
- **Решение:** Внедрить аналогичный блок внизу карточки авторизации в обоих приложениях:
  ```kotlin
  Spacer(modifier = Modifier.height(20.dp))
  Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically
  ) {
      Text(
          text = "Поддержка: ",
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Text(
          text = "@SantiLA213",
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.clickable {
              val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/SantiLA213"))
              context.startActivity(intent)
          }
      )
      Text(text = " • ", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Text(
          text = "@Spirit5449",
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.secondary,
          modifier = Modifier.clickable {
              val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/Spirit5449"))
              context.startActivity(intent)
          }
      )
  }
  ```

### 4.2. Кнопка «Привязать Telegram» в профиле
- **Размещение:** В карточке настроек профиля (как в `AthleteSettingsScreen.kt`, так и в `SettingsScreen.kt`).
- **Поведение:**
  1. Показывает текущий статус: `Не привязан` или `@username (Привязан)`.
  2. При нажатии на кнопку открывает диалог связывания с двумя опциями:
     - Опция А (Прямой Deep Link): запуск бота в Telegram:
       `val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/FitnessEcosystemBot?start=link_${userUuid}"))`
       `context.startActivity(intent)`
     - Опция Б: Ввод Telegram `@username` и подтверждение 6-значным OTP-кодом из бота (ввод в диалоге).
  3. Сохраняет `telegram_username` в локальном профиле и в облаке.

### 4.3. Тумблер 2FA и Диалог ввода OTP при входе
- **В Настройках (`AthleteSettingsScreen.kt` / `SettingsScreen.kt`):**
  - Переключатель «Двухфакторная аутентификация (2FA)»:
    - Если Telegram не привязан: выдает предупреждение «Для включения 2FA сначала привяжите Telegram аккаунт».
    - Если привязан: активирует флаг `is2FAEnabled = true` в `SharedPreferences` и профиле.
- **На Экране Входа (`AthleteAuthScreen.kt` / `TrainerAuthScreen.kt`):**
  - При вводе правильного логина и пароля, если для аккаунта включен 2FA:
    - Приложение **не выполняет вход сразу**, а открывает модальный диалог:
      ```kotlin
      if (show2FaDialog) {
          AlertDialog(
              onDismissRequest = { show2FaDialog = false },
              title = { Text("Двухфакторная аутентификация (2FA)") },
              text = {
                  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                      Text("Введите 6-значный код подтверждения из Telegram бота (действует 5 минут):")
                      OutlinedTextField(
                          value = otpInput,
                          onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) otpInput = it },
                          label = { Text("Код 2FA (6 цифр)") },
                          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                          singleLine = true
                      )
                      Text("Код действует: $timer2Fa", color = MaterialTheme.colorScheme.primary)
                  }
              },
              confirmButton = {
                  Button(onClick = { viewModel.verify2FaAndLogin(otpInput) }) {
                      Text("Подтвердить")
                  }
              },
              dismissButton = {
                  TextButton(onClick = { show2FaDialog = false }) { Text("Отмена") }
              }
          )
      }
      ```

---

## 5. Механизм синхронизации: Архитектура и сетевая топология

### 5.1. Анализ сетевого взаимодействия
В Android-приложениях проведена проверка сетевых вызовов:
1. **Отсутствие вызовов Web/API бэкенда (`http://localhost:3000`):**
   - В исходном коде `athlete-app` и `trainer-app` полностью отсутствуют Retrofit/OkHttp/Ktor клиенты к локальному веб-серверу портала.
   - Никаких эндпоинтов вроде `POST /api/trainer/pair` или `POST /api/login/2fa` в Android-коде сейчас нет.
2. **Прямая синхронизация через Google Apps Script Cloud:**
   - Оба приложения используют `CloudSecurityManager.kt` с URL Google Apps Script:
     `https://script.google.com/macros/s/AKfycbx6LCbVlxZa-MsWrP0QlNouJwcEcZVsbYHlO4HwHPDDuT6_dp0FDUKTmU_Ax5vg7EP6/exec?key=...`
   - Обмен идет нативными `HttpURLConnection` GET/POST запросами с шифрованием тела алгоритмом AES-256 (префикс `ENC:`).
3. **Роль Web Portal (`web/src/cloudSync.js`):**
   - Локальный веб-сервер Node.js работает в режиме зеркала: он опрашивает тот же самый Google Apps Script endpoint и расшифровывает AES-256 полезную нагрузку (`crypto.createDecipheriv('aes-256-ecb')`).
   - Таким образом, Web Portal и мобильные приложения уже синхронизируются через единое децентрализованное облачное хранилище!

### 5.2. Схема интеграции 5-минутного PIN и Telegram 2FA в экосистему

```
       ┌────────────────────────┐
       │   Telegram Bot         │
       │   (Генерация 5-мин OTP)│
       └───────────┬────────────┘
                   │
                   ▼ (Регистрация OTP / Привязка)
 ┌────────────────────────────────────────────────────────┐
 │       Google Apps Script Cloud (Encrypted JSON)        │
 │                                                        │
 │ 1. "pairing": {                                        │
 │      "739102": {                                       │
 │         "pin": "739102",                               │
 │         "clientUuid": "...",                           │
 │         "timestamp": 1728030000000,                    │
 │         "status": "PENDING" | "PAIRED"                 │
 │      }                                                 │
 │    }                                                   │
 │ 2. "tg_auth": {                                        │
 │      "username": { "otp": "481920", "expiresAt": ... } │
 │    }                                                   │
 └─────────────▲──────────────────────────▲───────────────┘
               │                          │
       AES-256 │                  AES-256 │
               ▼                          ▼
 ┌───────────────────────────┐    ┌───────────────────────────┐
 │   Athlete Pro (Android)   │    │   Trainer Pro (Android)   │
 │ • Авто-ротация PIN (5 мин)│    │ • Валидация TTL <= 5 мин  │
 │ • Таймер UI (05:00)       │    │ • Запрет повторных кодов  │
 │ • Тумблер 2FA + Telegram  │    │ • Ошибка при невалидности │
 └───────────────────────────┘    └───────────────────────────┘
```

---

## 6. Таблица соответствия требованиям задачи

| Требование задачи | Текущее состояние в Android | План реализации / Решение | Статус |
|---|---|---|---|
| **1. Athlete Pro PIN (5 мин, таймер, авто-реген)** | Статичный PIN в Room, ручная кнопка, нет таймера | `pinCreatedAt` в prefs, фоновый `StateFlow` таймер 300с, авто-ротация, Compose-таймер `05:00` | Спроектировано |
| **2. Trainer Pro Валидация (Строго 400 при >5 мин или reuse)** | Принимает любые старые коды, принимает PAIRED коды, создает фейкового клиента | Проверка наличия, `timestamp <= 5 мин`, `status != PAIRED`, исключение фейковых клиентов | Спроектировано |
| **3. Кнопка «Привязать Telegram»** | Отсутствует | Добавить карточку в профиле с deep link на бота и сохранением `@username` | Спроектировано |
| **4. Тумблер 2FA и OTP диалог** | Отсутствует | Switch 2FA в настройках + диалог ввода 6-значного OTP при входе | Спроектировано |
| **5. Контакты (@SantiLA213, @Spirit5449)** | Есть в настройках, нет на экранах входа | Добавить блок поддержки на `AthleteAuthScreen.kt` и `TrainerAuthScreen.kt` | Спроектировано |
| **6. Синхронизация** | Прямой Google Apps Script (AES-256), без прямого REST к локальному веб | Единый узел `pairing` и `tg_auth` в облачном JSON для Android, Web и бота | Спроектировано |
