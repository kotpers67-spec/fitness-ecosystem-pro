# Handoff Report: Trainer Android App Survey

**Agent**: `explorer_survey_trainer_6` (Trainer App Survey Explorer)  
**Parent Agent**: `7a8dbfe6-c2be-4283-bb75-fe454c56b1fa`  
**Working Directory**: `F:\Projects\fitness-ecosystem-pro\.agents\teamwork\explorer_survey_trainer_6`  
**Date**: 2026-10-04T20:36:00Z  
**Handoff Type**: Hard (Task Complete)

---

## 1. Observation

1. **`TrainerAuthScreen.kt` 2-Step Code Dialog**:
   - `TrainerAuthScreen.kt` lines 66–74:
     `var showTgCodeDialog by remember { mutableStateOf(false) }`
     `var tgCodeStep by remember { mutableIntStateOf(1) } // 1: username, 2: 6-digit otp`
     `var tgUsernameInput by remember { mutableStateOf("") }`
     `var tgCodeInput by remember { mutableStateOf("") }`
   - Step 1 (lines 676–693): Asks for `tgUsernameInput` with label "Telegram логин (@username)".
   - Step 1 Button (lines 743–764): Calls `viewModel.remoteAuthManager.requestTelegramOtp(clean)` before transitioning to `tgCodeStep = 2`.
   - `TrainerRemoteAuthManager.kt` lines 374–395:
     `suspend fun verifyTelegramLogin(username: String, otp: String): TrainerRemoteAuthResult` requires `username` parameter and sends `{ "username": cleanUser, "code": clean }`.

2. **`TrainerAuthScreen.kt` 1-Click Telegram Button Intent**:
   - Lines 145–151:
     ```kotlin
     val session = viewModel.remoteAuthManager.initTelegramSession()
     val targetUrl = if (session != null) session.second else "https://t.me/fitnessecosystemBOT?start=login"
     try {
         val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
         context.startActivity(intent)
     } catch (_: Exception) {}
     ```
   - Target URL uses `https://t.me/...` HTTP URI directly instead of trying `tg://resolve?domain=fitnessecosystemBOT&start=$sessionId` first.
   - `TrainerRemoteAuthManager.kt` line 531:
     `conn.outputStream.use { os -> os.write("{}".toByteArray(Charsets.UTF_8)) }`
     Backend `web/src/server.js` line 746 defaults `requestedRole` to `'athlete'` when body is empty.

3. **Data Synchronization (PIN, Avatar, Phone, Full Name, Restrictions)**:
   - `GoogleDriveSyncManager.kt` line 396:
     `val athleteNotes = foundPairingEntry.get("notes")?.asString ?: ""`
     Does not check `foundPairingEntry.get("restrictions")`, even though Web Portal and Telegram Bot store restrictions under property `"restrictions"`.
   - `GitHubSyncManager.kt` lines 79–88 (`buildAthletePayload`):
     Constructs `AthleteSyncPayload` without passing `client.phone` and `client.notes` (restrictions).
   - `GitHubSyncManager.kt` lines 215–219 (`applyAthletePayload`):
     Only updates `existingClient.avatarBase64`, ignoring `payload.clientName`, `payload.phone`, and `payload.restrictions`.

4. **Background APK Updater (`UpdateService.kt`)**:
   - Lines 178–232 (`downloadApkDirectly`): Directly streams APK via `HttpURLConnection` into `File(context.cacheDir, "TrainerPro_Update.apk")`.
   - Line 240: Validates APK zip header `PK\x03\x04` (`0x50, 0x4B, 0x03, 0x04`).
   - Lines 274–299 (`launchApkInstallation`): Launches package installer via `FileProvider.getUriForFile`.
   - Lines 255–270: Fallback uses Android `DownloadManager`. Zero browser redirects are invoked.
   - `AndroidManifest.xml` line 7: `<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />`.

5. **Gradle Build & Unit Tests**:
   - `.\gradlew.bat testDebugUnitTest` in `F:\Projects\fitness-ecosystem-pro\trainer-app`:
     Exited with code 0 (`BUILD SUCCESSFUL in 50s`, 25 tasks, 0 failures).
   - `.\gradlew.bat assembleRelease` in `F:\Projects\fitness-ecosystem-pro\trainer-app`:
     Exited with code 0 (`BUILD SUCCESSFUL in 11s`, 47 tasks, 0 failures).
     Binary generated: `trainer-app/app/build/outputs/apk/release/app-release.apk`.

---

## 2. Logic Chain

1. **Premise 1**: Requirement R2 mandates a 1-step 6-digit code entry dialog without asking for username.
   - *From Observation 1*: `TrainerAuthScreen.kt` currently splits login into a 2-step process requiring username first, and `TrainerRemoteAuthManager.kt` enforces a mandatory `username` parameter.
   - *Inference*: `TrainerAuthScreen.kt` must be modified to remove Step 1 and the username prompt, and `TrainerRemoteAuthManager.kt` must accept `{ "code": clean }` without username.

2. **Premise 2**: Requirement R2 mandates the 1-click Telegram button to trigger `tg://resolve?domain=fitnessecosystemBOT&start=auth_<sessionId>` with web fallback and polling.
   - *From Observation 2*: Currently it directly attempts `https://t.me/...` and passes `{}` in `initTelegramSession()`.
   - *Inference*: Must invoke `tg://resolve?domain=fitnessecosystemBOT&start=$sessionId` in an Intent, catch `ActivityNotFoundException` to fall back to `session.second`, and pass `{"requestedRole":"trainer"}` during initialization.

3. **Premise 3**: Requirement R2 mandates exact synchronization of 6-digit pairing PIN, photo, full name, phone, and restrictions.
   - *From Observation 3*: `GoogleDriveSyncManager` omits reading `"restrictions"` from pairing entries, and `GitHubSyncManager` omits `phone` and `restrictions` during payload construction and ingestion.
   - *Inference*: Synchronizing restrictions to/from `client.notes` in `GoogleDriveSyncManager.kt` and `GitHubSyncManager.kt` closes this gap completely.

4. **Premise 4**: Requirement R3 mandates direct background APK updating without browser redirects.
   - *From Observation 4*: `UpdateService.kt` already fulfills this requirement via streaming `HttpURLConnection` and `DownloadManager`.

5. **Premise 5**: Requirement R3 mandates unit test and `assembleRelease` verification.
   - *From Observation 5*: Both `./gradlew.bat testDebugUnitTest` and `./gradlew.bat assembleRelease` pass with 100% success on the existing codebase.

---

## 3. Caveats

- Backend dependency: Code-only OTP verification (`POST /api/auth/telegram/verify-otp` with `{ code: "123456" }`) requires the backend server (`web/src/server.js`) to support code matching without username. The mobile changes must be coordinated with the backend update.
- Emulator runtime intent handling: If the Telegram Android client is not installed on the emulator or device, the `tg://resolve` intent will throw `ActivityNotFoundException`; the proposed fallback to `https://t.me/` handles this gracefully.

---

## 4. Conclusion

The `trainer-app` codebase is in excellent operational health (unit tests and release builds pass 100%).
Four specific targeted modifications are required:
1. Simplify `showTgCodeDialog` in `TrainerAuthScreen.kt` to a 1-step 6-digit numeric input dialog without username.
2. In `TrainerRemoteAuthManager.kt`, support verifying code alone without username, and pass `{"requestedRole":"trainer"}` in `session-init`.
3. In `TrainerAuthScreen.kt`, trigger `tg://resolve?domain=fitnessecosystemBOT&start=$sessionId` first before falling back to `https://t.me/`.
4. In `GoogleDriveSyncManager.kt` and `GitHubSyncManager.kt`, map `"restrictions"` from/to `client.notes` and include `phone` and `fullName` in synchronization payloads.

---

## 5. Verification Method

1. **Gradle Unit Tests**:
   ```powershell
   cd F:\Projects\fitness-ecosystem-pro\trainer-app
   .\gradlew.bat testDebugUnitTest
   ```
   *Expected*: BUILD SUCCESSFUL, 0 test failures.

2. **Gradle Release Build**:
   ```powershell
   cd F:\Projects\fitness-ecosystem-pro\trainer-app
   .\gradlew.bat assembleRelease
   ```
   *Expected*: BUILD SUCCESSFUL, binary at `app\build\outputs\apk\release\app-release.apk`.

3. **Files to Inspect**:
   - `trainer-app/app/src/main/java/com/trainerapp/pro/ui/screens/TrainerAuthScreen.kt`
   - `trainer-app/app/src/main/java/com/trainerapp/pro/data/auth/TrainerRemoteAuthManager.kt`
   - `trainer-app/app/src/main/java/com/trainerapp/pro/data/sync/GoogleDriveSyncManager.kt`
   - `trainer-app/app/src/main/java/com/trainerapp/pro/data/sync/GitHubSyncManager.kt`
   - Report: `F:\Projects\fitness-ecosystem-pro\.agents\teamwork\explorer_survey_trainer_6\survey_report.md`
