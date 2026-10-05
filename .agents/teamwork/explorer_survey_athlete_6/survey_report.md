# Survey Report — Athlete App (Athlete Pro Android)

**Target Codebase**: `F:\Projects\fitness-ecosystem-pro\athlete-app`  
**Explorer**: Athlete App Survey Explorer (`explorer_survey_athlete_6`)  
**Timestamp**: 2026-10-04T20:34:00Z  
**Scope**: Requirements R2 (Mobile Auth Parity, 1-Click TG Login, Data Sync) & R3 (Direct APK Background Update, Build Verification)

---

## Executive Summary
1. **1-Step 6-Digit Auth Dialog**: Currently, `AthleteAuthScreen.kt` implements a 2-step wizard requiring the user to type their `@username` first, request an OTP via `/api/auth/telegram/request-otp`, and then enter the 6-digit code. To satisfy R2 and backend R1, it must be simplified into a direct 1-step dialog entering the 6-digit code alone, calling `/api/auth/telegram/verify-otp` with `{ "code": "..." }`.
2. **1-Click Telegram Login Button**: Currently opens a regular `https://t.me/...` URL via `Intent.ACTION_VIEW`. It must trigger native URI `tg://resolve?domain=fitnessecosystemBOT&start=auth_<sessionId>` with fallback to `https://t.me/fitnessecosystemBOT?start=auth_<sessionId>` and background session polling (`pollTelegramSession`).
3. **Data Synchronization (6-Digit PIN, Photo, Name, Phone, Restrictions)**: `AthleteViewModel.kt` already synchronizes full name, phone, restrictions, and photo (base64 thumbnail < 15 KB saved to disk as `athlete_avatar.jpg`) via `/api/me` and `/api/user/profile`. Gap identified: `regeneratePairingPin()` generates a local PIN without notifying the backend (`/api/athlete/regenerate-pin`), causing potential desynchronization with the Web portal and Bot; also, auto-regeneration when the 5-minute timer expires needs to be hooked up.
4. **Direct Background APK Updater**: `AthleteUpdateService.kt` already streams the APK directly to `context.cacheDir/AthletePro_Update.apk` via `HttpURLConnection` and falls back to Android `DownloadManager` with `InstallReceiver`. There are 0 browser redirects.
5. **Build and Test Baseline**:
   - `testDebugUnitTest`: 100% PASS (BUILD SUCCESSFUL in 30s, 0 failures).
   - `assembleRelease`: 100% PASS (BUILD SUCCESSFUL in 13s, output: `app/build/outputs/apk/release/app-release.apk`).

---

## 1. AthleteAuthScreen.kt & 1-Step 6-Digit Code Auth Dialog

### Current Implementation
- **File**: `athlete-app/app/src/main/java/com/athleteapp/pro/ui/screens/AthleteAuthScreen.kt`
  - Lines 60–68: State variables for a 2-step dialog:
    ```kotlin
    var showTgCodeDialog by remember { mutableStateOf(false) }
    var tgCodeStep by remember { mutableIntStateOf(1) } // 1: username, 2: 6-digit otp
    var tgUsernameInput by remember { mutableStateOf("") }
    var tgCodeInput by remember { mutableStateOf("") }
    ```
  - Lines 228–244: Button "🔑 Войти по коду из Telegram бота" sets `tgCodeStep = 1` and opens `showTgCodeDialog`.
  - Lines 636–808: `AlertDialog` with step 1 ("Введите ваш Telegram @username") calling `viewModel.remoteAuthManager.requestTelegramOtp(clean)` and step 2 ("6-значный код из бота") calling `viewModel.remoteAuthManager.verifyTelegramOtp(cleanUser, tgCodeInput)`.
- **File**: `athlete-app/app/src/main/java/com/athleteapp/pro/data/auth/AthleteRemoteAuthManager.kt`
  - Lines 309–369: `verifyTelegramOtp(username: String, otp: String)` sends JSON `{ "username": cleanUser, "code": clean }` to `POST /api/auth/telegram/verify-otp`.

### Gaps Against R2
- The dialog asks for Telegram username first before asking for the 6-digit OTP.
- Requirement R2 mandates: *"Mobile auth screens (`AthleteAuthScreen.kt`, `TrainerAuthScreen.kt`) must support 1-step 6-digit code entry dialog without asking for username."*
- Backend requirement R1 mandates: *"`/api/auth/telegram/verify-otp` must accept verification by code alone (`{ code: "123456" }`) without requiring username entry, matching active OTPs and paired user PINs."*

### Exact Proposed Changes
1. **In `AthleteRemoteAuthManager.kt`**:
   Update `verifyTelegramOtp` to allow code-only verification:
   ```kotlin
   suspend fun verifyTelegramOtp(otp: String, username: String = ""): AthleteRemoteAuthResult = withContext(Dispatchers.IO) {
       val clean = cleanOtp(otp)
       if (clean.length != 6) {
           return@withContext AthleteRemoteAuthResult.Error("Код должен содержать ровно 6 цифр")
       }
       try {
           val url = URL("$backendBaseUrl/api/auth/telegram/verify-otp")
           val conn = (url.openConnection() as HttpURLConnection).apply {
               requestMethod = "POST"
               setRequestProperty("Content-Type", "application/json; charset=utf-8")
               doOutput = true
               connectTimeout = 5000
               readTimeout = 5000
           }
           val payload = JsonObject().apply {
               addProperty("code", clean)
               val cleanUser = username.trim().removePrefix("@")
               if (cleanUser.isNotBlank()) {
                   addProperty("username", cleanUser)
               }
           }
           conn.outputStream.use { os ->
               os.write(gson.toJson(payload).toByteArray(Charsets.UTF_8))
           }
           val code = conn.responseCode
           val text = if (code in 200..299) conn.inputStream.bufferedReader().readText() else conn.errorStream?.bufferedReader()?.readText() ?: ""
           val json = runCatching { JsonParser.parseString(text).asJsonObject }.getOrNull()
           if (code == 200 && json != null) {
               val token = if (json.has("token")) json.get("token").asString else ""
               val userObj = json.getAsJsonObject("user")
               val user = if (userObj != null) parseRemoteUserInfo(userObj, token) else AthleteRemoteUserInfo(token = token)
               return@withContext AthleteRemoteAuthResult.Success(token, user)
           } else {
               val errMsg = json?.get("error")?.asString ?: "Неверный код"
               return@withContext AthleteRemoteAuthResult.Error(errMsg)
           }
       } catch (_: Exception) {
           return@withContext AthleteRemoteAuthResult.OfflineFallback
       }
   }
   ```
2. **In `AthleteAuthScreen.kt`**:
   - Remove `tgCodeStep`, `tgUsernameInput`, and `isRequestingTgOtp`.
   - On clicking "🔑 Войти по коду из Telegram бота", open a direct 1-step dialog:
     ```kotlin
     if (showTgCodeDialog) {
         AlertDialog(
             onDismissRequest = { if (!isVerifyingTgOtp) showTgCodeDialog = false },
             title = { Text("Вход по 6-значному коду", fontWeight = FontWeight.Bold) },
             text = {
                 Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                     Text(
                         text = "Введите 6-значный код из Telegram бота (@fitnessecosystemBOT) или код подключения:",
                         style = MaterialTheme.typography.bodySmall,
                         color = MaterialTheme.colorScheme.onSurfaceVariant
                     )
                     OutlinedTextField(
                         value = tgCodeInput,
                         onValueChange = {
                             if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                                 tgCodeInput = it
                                 tgCodeError = null
                             }
                         },
                         label = { Text("6-значный код") },
                         placeholder = { Text("123456") },
                         singleLine = true,
                         keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                         modifier = Modifier.fillMaxWidth()
                     )
                     if (tgCodeError != null) {
                         Text(
                             text = tgCodeError ?: "",
                             color = MaterialTheme.colorScheme.error,
                             style = MaterialTheme.typography.bodySmall,
                             fontWeight = FontWeight.Bold
                         )
                     }
                 }
             },
             confirmButton = {
                 Button(
                     enabled = !isVerifyingTgOtp && tgCodeInput.length == 6,
                     onClick = {
                         scope.launch {
                             isVerifyingTgOtp = true
                             tgCodeError = null
                             val res = viewModel.remoteAuthManager.verifyTelegramOtp(tgCodeInput)
                             if (res is AthleteRemoteAuthResult.Success) {
                                 viewModel.completeRemoteLogin(res.user)
                                 showTgCodeDialog = false
                             } else if (res is AthleteRemoteAuthResult.Error) {
                                 tgCodeError = res.message
                             } else {
                                 // Local fallback
                                 if (viewModel.verify2FaOtp(tgCodeInput)) {
                                     showTgCodeDialog = false
                                 } else {
                                     tgCodeError = "Неверный код авторизации"
                                 }
                             }
                             isVerifyingTgOtp = false
                         }
                     }
                 ) {
                     if (isVerifyingTgOtp) {
                         CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                     } else {
                         Text("Войти")
                     }
                 }
             },
             dismissButton = {
                 TextButton(onClick = { showTgCodeDialog = false }) { Text("Отмена") }
             }
         )
     }
     ```

---

## 2. 1-Click Telegram Login Button with Native URI & Polling

### Current Implementation
- **File**: `AthleteAuthScreen.kt` lines 132–179:
  ```kotlin
  val session = viewModel.remoteAuthManager.initTelegramSession()
  val targetUrl = if (session != null) session.second else "https://t.me/fitnessecosystemBOT?start=login"
  try {
      val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
      context.startActivity(intent)
  } catch (_: Exception) {}
  ```
  Then polls `pollTelegramSession(sessionId)` every 1.5 seconds for up to 300 seconds.

### Gaps Against R2
- The app only creates a web link `Intent.ACTION_VIEW` (`https://t.me/...`), which prompts Android to open Chrome or an in-app browser instead of directly opening the Telegram application.
- Requirement R2 specifies: *"1-click Telegram button must trigger `tg://resolve?domain=fitnessecosystemBOT&start=auth_<sessionId>` with fallback and poll session token."*

### Exact Proposed Changes
In `AthleteAuthScreen.kt` lines 139–147:
```kotlin
val session = viewModel.remoteAuthManager.initTelegramSession()
if (session != null) {
    val sessionId = session.first
    val authParam = if (sessionId.startsWith("auth_")) sessionId else "auth_$sessionId"
    val tgScheme = "tg://resolve?domain=fitnessecosystemBOT&start=$authParam"
    val webFallback = "https://t.me/fitnessecosystemBOT?start=$authParam"

    try {
        val tgIntent = Intent(Intent.ACTION_VIEW, Uri.parse(tgScheme)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(tgIntent)
    } catch (_: Exception) {
        try {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webFallback)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        } catch (_: Exception) {}
    }
}
```

---

## 3. Data Synchronization: Exact 6-Digit PIN, Photo, Full Name, Phone, Restrictions

### Current Implementation
- **Entities & Database**:
  - `AthleteProfileEntity` in `athlete-app/app/src/main/java/com/athleteapp/pro/data/local/entities/AthleteEntities.kt` contains:
    `clientUuid`, `fullName`, `phone`, `goal`, `restrictions`, `pairingPin`, `avatarPath`, `avatarBase64`, `photoUri`, `isPairedWithCoach`, `pairedCoachName`, `pairedCoachPhone`, `pairedCoachPhotoUri`, `pairedCoachAvatarBase64`.
- **Sync Mechanisms in `AthleteViewModel.kt`**:
  - `autoSync()` (lines 704–751): Calls `remoteAuthManager.fetchCurrentProfile(token)` via `/api/me`. If remote data is found, updates `fullName`, `phone`, `restrictions`, `pairingPin`, and `avatarBase64`.
  - Photo handling (lines 721–729, 614–664): Decodes Base64 to `athlete_avatar.jpg` in `context.filesDir`. In `saveAvatar()`, resizes image to 128x128, compresses to JPEG <= 15 KB (CursorWindow-safe), generates Base64, saves locally and calls `remoteAuthManager.updateProfile()`.
  - Profile update (lines 585–612): Updates Room and pushes to `/api/user/profile` via `remoteAuthManager.updateProfile()`.

### Gaps Against R2
1. **Pairing PIN Desynchronization**:
   - `regeneratePairingPin()` in `AthleteViewModel.kt` (lines 666–685) generates a random local number:
     ```kotlin
     val newPin = String.format(Locale.US, "%06d", (100000..999999).random())
     ```
     It does not notify the backend server. The Web portal and Telegram Bot fetch the PIN from `/api/athlete/regenerate-pin` or the database `users.pairing_code`. If the user regenerates the PIN in the Android app, the backend remains out-of-sync!
2. **Auto-Regeneration on Timer Expiration**:
   - `startPinTicker()` (lines 121–142): Decrements `_pinSecondsRemaining` to 0. When `remaining == 0`, it does not trigger automatic regeneration. Requirement R1 specifies: *"В профиле атлета (Web и Android) отображается живой таймер обратного отсчета (05:00) с автоматической перегенерацией кода при истечении."*

### Exact Proposed Changes
1. **In `AthleteRemoteAuthManager.kt`**:
   Add `regeneratePairingPin`:
   ```kotlin
   suspend fun regeneratePairingPin(authToken: String): String? = withContext(Dispatchers.IO) {
       if (authToken.isBlank()) return@withContext null
       try {
           val url = URL("$backendBaseUrl/api/athlete/regenerate-pin")
           val conn = (url.openConnection() as HttpURLConnection).apply {
               requestMethod = "POST"
               setRequestProperty("Authorization", "Bearer $authToken")
               setRequestProperty("Content-Type", "application/json; charset=utf-8")
               doOutput = true
               connectTimeout = 5000
               readTimeout = 5000
           }
           conn.outputStream.use { it.write("{}".toByteArray(Charsets.UTF_8)) }
           if (conn.responseCode == 200) {
               val text = conn.inputStream.bufferedReader().readText()
               val json = JsonParser.parseString(text).asJsonObject
               if (json.has("pairingCode")) {
                   return@withContext json.get("pairingCode").asString
               }
           }
       } catch (_: Exception) {}
       null
   }
   ```
2. **In `AthleteViewModel.kt`**:
   - In `regeneratePairingPin()`:
     If `auth_token` is present, call `remoteAuthManager.regeneratePairingPin(token)`. If the remote call returns a 6-digit PIN, use it; otherwise, fallback to generating a local random 6-digit PIN.
   - In `startPinTicker()`:
     ```kotlin
     if (remaining == 0 && !isPaired) {
         regeneratePairingPin()
     }
     ```

---

## 4. Direct Background APK Updater Without Browser Redirects

### Current Implementation
- **File**: `athlete-app/app/src/main/java/com/athleteapp/pro/data/update/AthleteUpdateService.kt`
  - `checkForUpdates()` checks `/api/version` and GitHub Releases API.
  - `downloadApkDirectly(downloadUrl)` connects via `HttpURLConnection`, handles redirects (up to 10), and streams bytes directly into `context.cacheDir/AthletePro_Update.apk`.
  - Validates ZIP header (`0x50, 0x4B, 0x03, 0x04`) and file size > 2 MB.
  - `downloadAndInstallApk(downloadUrl)`:
    - First attempts `downloadApkDirectly(downloadUrl)`. On success, launches `launchApkInstallation(apkFile)`.
    - Fallback: Enqueues a direct download via `android.app.DownloadManager.Request(Uri.parse(downloadUrl))` without opening any browser.
  - `launchApkInstallation(apkFile)` launches an `Intent(Intent.ACTION_VIEW)` with `FileProvider` and MIME type `application/vnd.android.package-archive`.
  - `InstallReceiver.kt` catches `DownloadManager.ACTION_DOWNLOAD_COMPLETE` and launches the system package installer.
  - `AndroidManifest.xml` includes `REQUEST_INSTALL_PACKAGES` permission and FileProvider configured in `@xml/file_paths`.

### Compliance Assessment
- **Status**: **100% COMPLIANT**.
- There are NO browser redirects or external browser launches anywhere in the update pipeline.
- Both manual update checking in `AthleteSettingsScreen.kt` and auto-checking in `AthleteViewModel.kt` invoke `downloadAndInstallApk`, which runs entirely in the background.

---

## 5. Build and Test Verification

### Unit Tests
- **Command**: `.\gradlew.bat testDebugUnitTest` in `athlete-app`
- **Result**: `BUILD SUCCESSFUL in 30s` (25 actionable tasks: 12 executed, 13 up-to-date)
- **Suite Breakdown**:
  - `AthletePinAnd2FaTest`: 5/5 PASSED (PIN countdown, 300s expiry, Telegram username format, 2FA OTP length/digit check).
  - `AthleteIsolationAndPairingTest`: 6/6 PASSED (client UUID isolation, self workout toggle, multi-client cloud isolation, pairing QR/PIN format, coach unpair cleanup).
  - `AthleteSyncRemediationTest`: 9/9 PASSED (malformed JSON resilience, empty workouts import, non-cascade update, leaderboard privacy filtering, share link formatting).
  - `NeuroAdaptiveEngineTest`: 6/6 PASSED (1RM estimation, tonnage calculation, readiness/fatigue levels, progressive overload & deload logic).
  - `NeuroAdaptiveStressTest`: PASSED.
- **Failures / Errors**: 0

### Release Build
- **Command**: `.\gradlew.bat assembleRelease` in `athlete-app`
- **Result**: `BUILD SUCCESSFUL in 13s` (47 actionable tasks: 1 executed, 46 up-to-date)
- **Output Artifact**:
  - `athlete-app/app/build/outputs/apk/release/app-release.apk`
- **Deployment Targets**:
  - `releases/athlete-latest.apk` / `releases/athlete-pro-latest.apk`
  - `web/releases/athlete-latest.apk` / `web/releases/athlete-pro-latest.apk`

---

## Summary Matrix of Required Actions

| Component | Current State | Required State | Action Needed |
|---|---|---|---|
| **Auth Screen Code Entry** | 2-step dialog (asks username first, then code) | 1-step dialog entering 6-digit code alone | Modify `AthleteAuthScreen.kt` and `AthleteRemoteAuthManager.kt` to send `{ code }` directly |
| **1-Click Telegram Login** | Opens `https://t.me/...` | Calls `tg://resolve?domain=fitnessecosystemBOT&start=auth_<sessionId>` with web fallback | Update `AthleteAuthScreen.kt` intent dispatch to try `tg://` first |
| **Pairing PIN Sync** | Generated locally on device | Synchronized with Web backend `/api/athlete/regenerate-pin` | Add `regeneratePairingPin` remote API call in `AthleteRemoteAuthManager` and `AthleteViewModel` |
| **5-Min Timer Auto-Regen** | Timer stops at 00:00 | Auto-regenerates new PIN when expired | Add auto-call to `regeneratePairingPin()` when `remaining == 0` |
| **Background APK Updater** | Direct background download & installer | Direct background download & installer | No code change needed; already compliant |
| **Release Build & Tests** | Passing | Passing | Deploy compiled release APK to `releases/` and `web/releases/` |
