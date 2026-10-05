# Handoff Report — Athlete App Survey Explorer

**Agent**: `explorer_survey_athlete_6`  
**Timestamp**: 2026-10-04T20:35:00Z  
**Target Codebase**: `F:\Projects\fitness-ecosystem-pro\athlete-app`  
**Type**: Hard Handoff (Investigation & Survey Complete)

---

## 1. Observation

1. **AthleteAuthScreen.kt 2-Step Telegram Code Entry**:
   - In `athlete-app/app/src/main/java/com/athleteapp/pro/ui/screens/AthleteAuthScreen.kt`:
     - Lines 60–68:
       ```kotlin
       var showTgCodeDialog by remember { mutableStateOf(false) }
       var tgCodeStep by remember { mutableIntStateOf(1) } // 1: username, 2: 6-digit otp
       var tgUsernameInput by remember { mutableStateOf("") }
       var tgCodeInput by remember { mutableStateOf("") }
       ```
     - Lines 651–668: Step 1 asks for username:
       ```kotlin
       Text(text = "Введите ваш Telegram @username. Мы отправим 6-значный код в Telegram бота (действует 5 минут).")
       OutlinedTextField(value = tgUsernameInput, onValueChange = { ... }, label = { Text("Telegram логин (@username)") })
       ```
     - Lines 729–735: Button triggers `viewModel.remoteAuthManager.requestTelegramOtp(clean)` to proceed to Step 2.
     - Lines 763: In step 2, verifies code with `viewModel.remoteAuthManager.verifyTelegramOtp(cleanUser, tgCodeInput)`.
   - In `athlete-app/app/src/main/java/com/athleteapp/pro/data/auth/AthleteRemoteAuthManager.kt`:
     - Lines 309–331: `verifyTelegramOtp(username: String, otp: String)` sends `{ "username": cleanUser, "code": clean }` to `/api/auth/telegram/verify-otp`.

2. **1-Click Telegram Deep Link Handling**:
   - In `AthleteAuthScreen.kt` lines 140–145:
     ```kotlin
     val session = viewModel.remoteAuthManager.initTelegramSession()
     val targetUrl = if (session != null) session.second else "https://t.me/fitnessecosystemBOT?start=login"

     try {
         val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
         context.startActivity(intent)
     } catch (_: Exception) {}
     ```
     Only attempts generic web URL without testing native `tg://resolve?domain=fitnessecosystemBOT&start=auth_<sessionId>`.
     Session polling (lines 150–174) with `pollTelegramSession(sessionId)` is implemented and polls every 1.5s for up to 300s.

3. **Pairing PIN, Photo, Profile & Restrictions Sync**:
   - In `athlete-app/app/src/main/java/com/athleteapp/pro/ui/AthleteViewModel.kt`:
     - Lines 223–249 & 717–744: `completeRemoteLogin` and `autoSync()` fetch profile from `/api/me`, update Room database with `fullName`, `phone`, `restrictions`, `pairingPin`, and decode `avatarBase64` to `athlete_avatar.jpg`.
     - Lines 585–612 & 614–664: `updateProfile` and `saveAvatar` push updates to `/api/user/profile`.
     - Lines 666–685: `regeneratePairingPin()` generates a random local PIN:
       ```kotlin
       val newPin = String.format(Locale.US, "%06d", (100000..999999).random())
       ```
       It does not call the backend endpoint `POST /api/athlete/regenerate-pin`.
     - Lines 121–142: `startPinTicker()` decreases `_pinSecondsRemaining` to 0 but does not auto-regenerate the PIN when expired.

4. **Direct Background APK Updater**:
   - In `athlete-app/app/src/main/java/com/athleteapp/pro/data/update/AthleteUpdateService.kt`:
     - Lines 181–235: `downloadApkDirectly(downloadUrl)` downloads binary directly to `context.cacheDir/AthletePro_Update.apk` via `HttpURLConnection`, handles redirects, and verifies ZIP header bytes `0x50 0x4B 0x03 0x04`.
     - Lines 250–275: `downloadAndInstallApk(downloadUrl)` uses `downloadApkDirectly` or falls back to `DownloadManager` with notification, directly installing via `FileProvider` without any external browser redirects.
     - Lines 277–312: `launchApkInstallation(apkFile)` starts package installer via `Intent.ACTION_VIEW` with `application/vnd.android.package-archive`.
     - `InstallReceiver.kt` listens for `ACTION_DOWNLOAD_COMPLETE` and invokes installer.
     - `AndroidManifest.xml` line 7: `<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />`.

5. **Gradle Tests and Release Build Status**:
   - Command `.\gradlew.bat testDebugUnitTest` ran with output:
     `BUILD SUCCESSFUL in 30s` (25 actionable tasks: 12 executed, 13 up-to-date, 0 failures).
   - Command `.\gradlew.bat assembleRelease` ran with output:
     `BUILD SUCCESSFUL in 13s` (47 actionable tasks: 1 executed, 46 up-to-date, 0 failures).
   - Output binary produced at `athlete-app/app/build/outputs/apk/release/app-release.apk`.

---

## 2. Logic Chain

1. **1-Step PIN Auth**:
   - *Observation 1* shows that the dialog requires the user to input their username before requesting an OTP.
   - *Requirement R2* specifies: "Mobile auth screens (`AthleteAuthScreen.kt`, `TrainerAuthScreen.kt`) must support 1-step 6-digit code entry dialog without asking for username."
   - *Requirement R1* specifies that the backend `/api/auth/telegram/verify-otp` accepts `{ code: "123456" }` alone.
   - *Inference*: `AthleteAuthScreen.kt` must be modified to present an immediate single-step dialog prompting only for the 6-digit code, calling `verifyTelegramOtp(code)` directly.

2. **1-Click Telegram Deep Link**:
   - *Observation 2* shows that `AthleteAuthScreen.kt` only uses web URLs (`https://t.me/...`).
   - *Requirement R2* specifies: "1-click Telegram button must trigger `tg://resolve?domain=fitnessecosystemBOT&start=auth_<sessionId>` with fallback and poll session token."
   - *Inference*: The intent handler must attempt opening `tg://resolve?domain=fitnessecosystemBOT&start=auth_<sessionId>` first; if an exception occurs (no native Telegram app), fall back to `https://t.me/fitnessecosystemBOT?start=auth_<sessionId>`, while keeping session polling active.

3. **Data Synchronization**:
   - *Observation 3* shows that while profile data and avatar are synchronized with `/api/me` and `/api/user/profile`, `regeneratePairingPin()` generates a disconnected local random number without calling `/api/athlete/regenerate-pin`, and does not auto-regenerate when the 5-minute timer expires.
   - *Requirement R2* specifies: "Both mobile apps must synchronize the exact 6-digit pairing PIN, photo, full name, phone, and restrictions with the website and Telegram bot for the same account."
   - *Inference*: `AthleteRemoteAuthManager.kt` needs `regeneratePairingPin(authToken)` calling `POST /api/athlete/regenerate-pin`. When triggered, `AthleteViewModel` updates the Room entity with the server PIN. Additionally, `startPinTicker()` must auto-trigger regeneration when remaining time reaches 0.

4. **Background APK Updater**:
   - *Observation 4* confirms that direct binary streaming and `DownloadManager` are fully implemented without browser redirects.
   - *Inference*: Requirement R3 is already satisfied in the athlete codebase.

5. **Build and Test Verification**:
   - *Observation 5* proves all unit tests pass and `assembleRelease` compiles successfully.

---

## 3. Caveats

1. The backend server endpoint `/api/auth/telegram/verify-otp` must support verifying by code alone (`{ code: "123456" }`) without requiring a `username` field as outlined in R1. If the implementer updates `athlete-app`, backend changes from the web agent must be present for end-to-end integration.
2. In release deployment, the release APK from `athlete-app/app/build/outputs/apk/release/app-release.apk` must be copied to both `releases/athlete-latest.apk` and `web/releases/athlete-latest.apk` (and `athlete-pro-latest.apk`).

---

## 4. Conclusion

The Athlete Pro Android app codebase is healthy, passes all existing unit tests (100% PASS), and compiles release APKs without errors.
Four specific modifications are required for full compliance with requirements R2 and R3:
1. `AthleteAuthScreen.kt`: Replace 2-step username+OTP wizard with a 1-step direct 6-digit code entry dialog.
2. `AthleteRemoteAuthManager.kt`: Update `verifyTelegramOtp` to accept `code` without requiring `username`.
3. `AthleteAuthScreen.kt`: Update 1-click Telegram button to trigger native `tg://resolve?domain=fitnessecosystemBOT&start=auth_<sessionId>` before web fallback.
4. `AthleteViewModel.kt` & `AthleteRemoteAuthManager.kt`: Synchronize `regeneratePairingPin` with `POST /api/athlete/regenerate-pin` and auto-regenerate on 5-minute timer expiry.

---

## 5. Verification Method

1. **Run Unit Tests**:
   ```powershell
   cd F:\Projects\fitness-ecosystem-pro\athlete-app
   .\gradlew.bat testDebugUnitTest
   ```
   *Expected*: `BUILD SUCCESSFUL`, 0 failed tests.
2. **Run Release Build**:
   ```powershell
   cd F:\Projects\fitness-ecosystem-pro\athlete-app
   .\gradlew.bat assembleRelease
   ```
   *Expected*: `BUILD SUCCESSFUL`, outputs `app/build/outputs/apk/release/app-release.apk`.
3. **Inspect Code Files**:
   - `athlete-app/app/src/main/java/com/athleteapp/pro/ui/screens/AthleteAuthScreen.kt` for 1-step dialog and `tg://` scheme.
   - `athlete-app/app/src/main/java/com/athleteapp/pro/data/auth/AthleteRemoteAuthManager.kt` for `verifyTelegramOtp(otp)` and `regeneratePairingPin(token)`.
   - `athlete-app/app/src/main/java/com/athleteapp/pro/ui/AthleteViewModel.kt` for PIN sync and ticker expiry auto-regeneration.
