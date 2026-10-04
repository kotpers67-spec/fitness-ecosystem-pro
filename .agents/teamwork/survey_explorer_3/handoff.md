# Handoff Report — Survey Explorer 3 (Android Apps Architecture)

**Date:** 2026-10-04  
**Agent:** Survey Explorer 3  
**Working Directory:** `F:\Projects\fitness-ecosystem-pro\.agents\teamwork\survey_explorer_3\`  
**Scope:** Android Apps (`athlete-app` & `trainer-app`) Pairing PIN, Validation, Telegram 2FA, Owner Contacts, and Cloud Synchronization Architecture.

---

## 1. Observation

1. **Athlete Pro Pairing Screen & Code Generation:**
   - **File:** `athlete-app/app/src/main/java/com/athleteapp/pro/ui/screens/AthleteSettingsScreen.kt` (lines 270–455).
   - Component: Card `ПРИВЯЗКА К ТРЕНЕРУ`. PIN displayed as `cleanPin` (line 406: `text = cleanPin`) in Monospace font, and `QrCodeView(content = cleanPin)`.
   - Action button: Line 444: `onClick = { viewModel.regeneratePairingPin() }`.
   - **File:** `athlete-app/app/src/main/java/com/athleteapp/pro/ui/AthleteViewModel.kt` (lines 128, 411–427, 451–455).
     - Line 128: `val pin = if (current.pairingPin.length == 6) current.pairingPin else String.format(Locale.US, "%06d", Random().nextInt(1000000))`
     - Lines 414–416: `val newPin = String.format("%06d", (100000..999999).random())`, updates `pairingPin` in `AthleteProfileEntity`.
     - **Observed fact:** No timestamp (`pinCreatedAt` or `pinExpiresAt`) exists in `AthleteProfileEntity` or `AthleteViewModel`. There is no countdown timer (05:00) and no automatic regeneration when 5 minutes expire.
   - **File:** `athlete-app/app/src/main/java/com/athleteapp/pro/data/sync/GoogleDriveAthleteSyncManager.kt` (lines 208–228):
     - Publishes `pairingNode` under `pairing[cleanPin]` with `timestamp = System.currentTimeMillis()`, `status = "PENDING"`.

2. **Trainer Pro Pairing Screen & Validation:**
   - **File:** `trainer-app/app/src/main/java/com/trainerapp/pro/ui/screens/HomeScreen.kt` (lines 450–673).
     - Dialog `showPairingDialog` has camera QR scanning (`cameraLauncher`), gallery picker, and manual 6-digit text input (`pairingCodeInput`).
     - Line 632: calls `viewModel.pairClientByCode(codeToPair) { success, msg -> ... }`.
   - **File:** `trainer-app/app/src/main/java/com/trainerapp/pro/ui/MainViewModel.kt` (lines 539–565):
     - `pairClientByCode` delegates directly to `googleDriveSync.findAndPairAthlete(...)`.
   - **File:** `trainer-app/app/src/main/java/com/trainerapp/pro/data/sync/GoogleDriveSyncManager.kt` (lines 154–360):
     - Lines 259–283: Loops through `rootObj.getAsJsonObject("pairing")` matching `cleanPin`.
     - **Observed defect 1:** Line 264 matches solely by digits (`digitsInKey == cleanPin || entryPin == cleanPin`). Does NOT check `timestamp` against a 5-minute TTL!
     - **Observed defect 2:** Does NOT check `status`. Re-pairs even if `status == "PAIRED"`!
     - **Observed defect 3 (Critical):** Lines 290–328: If code is not found in cloud, it falls back to:
       `val finalUuid = athleteUuid ?: UUID.randomUUID().toString()`
       `val finalName = athleteName?.takeIf { it.isNotBlank() } ?: "Подопечный ${cleanPin.take(3)}-${cleanPin.takeLast(3)}"`
       and inserts a dummy client in local DB, returning SUCCESS instead of returning an error!

3. **Contacts, Telegram Linking, and 2FA:**
   - **File:** `athlete-app/app/src/main/java/com/athleteapp/pro/ui/screens/AthleteSettingsScreen.kt` (lines 846–896) and `trainer-app/app/src/main/java/com/trainerapp/pro/ui/screens/SettingsScreen.kt` (lines 820–870):
     - Direct contact buttons to `@SantiLA213` and `@Spirit5449` (`https://t.me/SantiLA213`, `https://t.me/Spirit5449`) are already present in settings.
   - **File:** `AthleteAuthScreen.kt` (lines 1–244) and `TrainerAuthScreen.kt` (lines 1–244):
     - Contact buttons to owners are completely absent on login/register screens.
     - No 2FA OTP dialog or verification step exists upon login.
   - **Settings in both apps:**
     - "Привязать Telegram" button does not exist.
     - 2FA toggle does not exist.

4. **Synchronization Topology:**
   - Both Android apps communicate strictly with Google Apps Script Cloud via `CloudSecurityManager.kt` (AES-256 encrypted payload, endpoint `https://script.google.com/macros/s/AKfycbx6LCbVlxZa-MsWrP0QlNouJwcEcZVsbYHlO4HwHPDDuT6_dp0FDUKTmU_Ax5vg7EP6/exec`).
   - Neither Android app communicates directly via HTTP REST with `http://localhost:3000`.
   - The Web portal (`web/src/cloudSync.js`) connects to this exact same Google Apps Script cloud.

---

## 2. Logic Chain

1. **Step 1 (Athlete Pro PIN TTL):**
   - Observation 1 proves that `pairingPin` is stored without timestamp in Room DB, and regeneration is solely manual.
   - Therefore, to support a 5-minute dynamic PIN with countdown and auto-regeneration, `AthleteViewModel` must track `pinCreatedAt` in persistent preferences, expose `pinSecondsRemaining: StateFlow<Int>`, run a coroutine ticker loop, automatically invoke `regeneratePairingPin()` when `remaining <= 0`, and display `05:00` in Compose with `LinearProgressIndicator`.
   - Observation 1 also shows that old pairing keys in the cloud `pairing` map must be removed during regeneration so obsolete codes cannot linger in cloud storage.

2. **Step 2 (Trainer Pro Strict Validation):**
   - Observation 2 proves that `GoogleDriveSyncManager.findAndPairAthlete` currently accepts any code regardless of age, accepts previously used codes, and creates phantom dummy clients if the code is missing from the cloud entirely.
   - Therefore, three strict rejection conditions must be inserted:
     1) Not found in cloud -> `Result.failure(IllegalArgumentException("Код не найден"))`.
     2) `now - timestamp > 5 * 60 * 1000L` -> remove expired entry and `Result.failure(IllegalStateException("Срок действия кода истёк (действует 5 минут)"))`.
     3) `status == "PAIRED"` or `"USED"` -> `Result.failure(IllegalStateException("Этот код уже был использован"))`.
     4) On success: set `status = "PAIRED"`, consume code, and update cloud JSON.

3. **Step 3 (UI Additions):**
   - Observation 3 proves that owner contact buttons exist only in settings, not on auth screens; and Telegram linking / 2FA are entirely missing.
   - Therefore, adding owner contacts to `AthleteAuthScreen.kt` and `TrainerAuthScreen.kt` satisfies R3.
   - Adding a Telegram Linking button (deep link `tg://resolve?domain=...` / `https://t.me/...`) and 2FA switch to settings, plus a 6-digit OTP dialog upon login when 2FA is active, satisfies R2.

4. **Step 4 (Sync Architecture):**
   - Observation 4 confirms that Google Apps Script cloud acts as the decentralized shared cloud hub for both Android apps and the Web portal.
   - Therefore, no local REST API client needs to be introduced in the Android apps; the shared contract in `pairing[cleanPin]` (with `timestamp` and `status: "PENDING" | "PAIRED"`) enables seamless cross-platform pairing between Web and Android.

---

## 3. Caveats

- **Room DB Migration vs SharedPreferences:** Modifying `AthleteProfileEntity` directly requires a Room database migration or `fallbackToDestructiveMigration`. To prevent any user data loss on production devices, storing `pinCreatedAt`, `telegramUsername`, and `twoFactorEnabled` in `SharedPreferences` (or using safe Room migration) is recommended.
- **Offline Mode:** If an athlete is offline when 5 minutes expire, local regeneration will occur, but cloud sync will succeed on the next network reconnection. Trainer pairing inherently requires internet access to query the cloud registry.
- **Telegram Bot Webhook / Long Polling:** If the user launches Telegram via deep link on mobile, the Telegram bot server (running locally or in cloud) will issue the OTP; on Android, the user enters this OTP in the Compose dialog.

---

## 4. Conclusion

The Android apps architecture for Athlete Pro and Trainer Pro has been thoroughly audited and documented:
1. Athlete Pro pairing PIN can be rendered dynamic with a 5-minute lifecycle, live 05:00 countdown timer, and automatic expiration regeneration by adding `pinCreatedAt` tracking and a coroutine loop in `AthleteViewModel`.
2. Trainer Pro pairing validation must eliminate the dummy client fallback and enforce strict rejection for missing codes, codes older than 300 seconds, and already used codes.
3. Profile and login screens require owner contact links on auth screens, a Telegram link button, a 2FA toggle in settings, and a 2FA OTP verification dialog on login.
4. The ecosystem sync mechanism uses AES-256 encrypted Google Apps Script cloud as a unified bus, ensuring full parity between Web Portal, Trainer Pro, and Athlete Pro.

A comprehensive analysis report has been written to `F:\Projects\fitness-ecosystem-pro\.agents\teamwork\survey_explorer_3\analysis.md`.

---

## 5. Verification Method

To verify the findings and proposed implementation:
1. **Athlete Pro Unit Tests:**
   Command: `cmd /c "gradlew.bat testDebugUnitTest"` in `F:\Projects\fitness-ecosystem-pro\athlete-app`
   Expected Result: `BUILD SUCCESSFUL` (all unit tests pass).
2. **Trainer Pro Unit Tests:**
   Command: `cmd /c "gradlew.bat testDebugUnitTest"` in `F:\Projects\fitness-ecosystem-pro\trainer-app`
   Expected Result: `BUILD SUCCESSFUL` (all unit tests pass).
3. **Inspect Implementation Files:**
   - `athlete-app/app/src/main/java/com/athleteapp/pro/ui/screens/AthleteSettingsScreen.kt` (lines 270–455)
   - `athlete-app/app/src/main/java/com/athleteapp/pro/ui/AthleteViewModel.kt` (lines 411–427)
   - `trainer-app/app/src/main/java/com/trainerapp/pro/ui/screens/HomeScreen.kt` (lines 450–673)
   - `trainer-app/app/src/main/java/com/trainerapp/pro/data/sync/GoogleDriveSyncManager.kt` (lines 154–360)
   - `trainer-app/app/src/main/java/com/trainerapp/pro/ui/screens/TrainerAuthScreen.kt` (lines 1–244)
   - `athlete-app/app/src/main/java/com/athleteapp/pro/ui/screens/AthleteAuthScreen.kt` (lines 1–244)
