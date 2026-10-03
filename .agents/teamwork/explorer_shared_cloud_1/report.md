# Full Audit Report: Shared Ecosystem, Cloud & Release Infrastructure

**Author**: Explorer 3 (Shared Ecosystem, Cloud & Release Explorer)  
**Date**: 2026-10-03  
**Target Milestone**: v1.0.5 Release Audit  
**Scope**: Shared Architecture, Cloud Backend (Google Apps Script), AES-256 Encryption, Build & Packaging, GitHub Tooling, Android Emulator QA

---

## Executive Summary

1. **AES-256 Encryption**:
   - Implemented via `AES/ECB/PKCS5Padding` with SHA-256 key derivation in `CloudSecurityManager.kt` across both apps.
   - Deobfuscated secret key is `"Spirit5449@2011@213@"`; deobfuscated endpoint is Google Apps Script Web App.
   - **Critical defect found**: `GoogleDriveAthleteSyncManager.unpairFromCoach()` fails to decrypt cloud payload on GET and posts unencrypted raw JSON on POST.
   - **Testing limitation**: `CloudSecurityManager.kt` relies on `android.util.Base64`, preventing JVM unit testing without android mocks.
2. **Google Apps Script Cloud Backend**:
   - Web App endpoint is **LIVE and operational** (`200 OK` / `302 Found` redirect to echo server).
   - Validates authentication key: unauthorized requests receive `{"error":"Unauthorized"}`.
   - Cloud payload schema houses `clients`, `pairing`, and `updates` nodes. Current cloud state is initialized to `{}`.
   - **UI & In-App Update defects found**:
     - Hardcoded fallback version `"1.0.2"` in `UpdateService.kt` and `AthleteUpdateService.kt`.
     - Hardcoded text `"v1.0.1"` in `AthleteSettingsScreen.kt` (lines 744, 747) and `SettingsScreen.kt` (lines 491, 494).
     - Hardcoded text badge `"v1.0.0"` in `SettingsScreen.kt` (line 765).
3. **Build & Packaging**:
   - Independent Gradle configurations in `trainer-app` and `athlete-app`.
   - Current versions: `versionCode = 4`, `versionName = "1.0.4"`.
   - **Critical test failure**: `athlete-app` unit tests fail to compile (`AthleteSyncRemediationTest.kt:164: Class 'FakeAthleteDao' does not implement 'getAllSets'`).
   - Release signing: Prior APKs (`v1.0.0` - `v1.0.4`) use standard debug keystore (`CN=Android Debug`). `buildTypes.release` currently has no signing config and would produce an unsigned APK unless configured with `signingConfigs.getByName("debug")`.
4. **Deployment & GitHub CLI**:
   - `gh` CLI v2.100.0 is installed and authenticated as `santiyastudio-lgtm` with full `repo` and `workflow` scopes.
   - Tag/release workflow tested and mapped to `gh release create v1.0.5`.
5. **Android Emulator & QA Infrastructure**:
   - ADB at `F:\Development\Android\Sdk\platform-tools\adb.exe` is healthy and responsive.
   - Pixel 8 API 36 (`emulator-5554`, 1080x2400) is online with both `com.trainerapp.pro` and `com.athleteapp.pro` pre-installed (v1.0.4).
   - Screen capture verified via `adb exec-out screencap -p` returning clean 1080x2400 PNGs.
   - Test automation: `trainer-app` has working PowerShell scripts (`qa_test.ps1`, `qa_screens.ps1`), while `athlete-app` lacks a dedicated PowerShell script.

---

## 1. Data Encryption (AES-256) Audit

### 1.1 Architecture & Implementation
- **Source Files**:
  - `trainer-app/app/src/main/java/com/trainerapp/pro/data/sync/CloudSecurityManager.kt`
  - `athlete-app/app/src/main/java/com/athleteapp/pro/data/sync/CloudSecurityManager.kt`
- **Masking Mechanism**:
  - Byte array XOR mask: `MASK = [0x53, 0x70, 0x69, 0x72, 0x69, 0x74, 0x46, 0x69, 0x74, 0x32, 0x30, 0x32, 0x36]` (ASCII `"SpiritFit2026"`).
  - Encrypted Endpoint bytes (`ENC_ENDPOINT`) decode to:  
    `https://script.google.com/macros/s/AKfycbx6LCbVlxZa-MsWrP0QlNouJwcEcZVsbYHlO4HwHPDDuT6_dp0FDUKTmU_Ax5vg7EP6/exec`
  - Encrypted Secret Key bytes (`ENC_KEY`) decode to:  
    `Spirit5449@2011@213@`
- **Key Derivation & Cipher Configuration**:
  - Key derivation: `MessageDigest.getInstance("SHA-256").digest(getSecretKey().toByteArray(StandardCharsets.UTF_8))` produces 32 bytes (256-bit AES key).
  - Cipher: `Cipher.getInstance("AES/ECB/PKCS5Padding")`.
  - IV Handling: In ECB (Electronic Codebook) mode, **no Initialization Vector (IV) is used**. Cipher initialization is `cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec)` without `IvParameterSpec`.
  - Prefix & Framing: Ciphertext is encoded with `Base64.encodeToString(encrypted, Base64.NO_WRAP)` and prepended with `"ENC:"`.
  - Decryption logic strips `"ENC:"` prefix. If input does not start with `"ENC:"`, it returns the raw string unchanged (graceful backward compatibility).

### 1.2 Defects & Vulnerabilities Found
1. **Critical Encryption Bypass in `GoogleDriveAthleteSyncManager.unpairFromCoach()`**:
   - Location: `athlete-app/app/src/main/java/com/athleteapp/pro/data/sync/GoogleDriveAthleteSyncManager.kt:226-237`
   - Code:
     ```kotlin
     val cloudJson = httpGet(requestUrl)
     if (cloudJson.isNotBlank() && cloudJson != "{}") {
         try {
             val rootObj = JsonParser.parseString(cloudJson).asJsonObject // BUG: cloudJson is encrypted with "ENC:", fails to parse!
             if (rootObj.has("pairing")) {
                 val pairingObj = rootObj.getAsJsonObject("pairing")
                 if (oldPin.isNotBlank() && pairingObj.has(oldPin)) {
                     pairingObj.remove(oldPin)
                 }
             }
             rootObj.addProperty("updatedAt", System.currentTimeMillis().toString())
             httpPost(requestUrl, gson.toJson(rootObj)) // BUG: posts plaintext JSON, corrupting encrypted cloud DB!
         } catch (_: Exception) {}
     }
     ```
   - Impact: Unpairing fails silently to remove the PIN from the cloud, and if it ever succeeded, it would write unencrypted plaintext over the encrypted database, breaking synchronization for all users.
2. **Missing JVM Testability**:
   - `CloudSecurityManager.kt` uses `android.util.Base64` instead of `java.util.Base64`. Since `minSdk = 26`, `java.util.Base64` is supported on Android 8.0+ and enables direct JUnit execution without mocking.

---

## 2. Cloud Updates & Backend (Google Apps Script) Audit

### 2.1 Backend Deployment & Handshake
- **Endpoint**:
  `https://script.google.com/macros/s/AKfycbx6LCbVlxZa-MsWrP0QlNouJwcEcZVsbYHlO4HwHPDDuT6_dp0FDUKTmU_Ax5vg7EP6/exec`
- **Query Parameter Auth**:
  `?key=Spirit5449%402011%40213%40`
- **Live Verification**:
  - Request with valid key: returns HTTP 200/302 with current database state.
  - Request with invalid key (`?key=wrong_key`): returns HTTP 200/302 with `{"error":"Unauthorized"}`.
  - Redirect handling: Google Apps Script returns `302 Found` with redirect to `https://script.googleusercontent.com/macros/echo?...`. POST requests must follow the redirect by sending a GET to the echo location. Both mobile apps implement this loop correctly in `httpPost` and `httpGet`.

### 2.2 Cloud Data Schema
The cloud storage stores a single AES-256 encrypted JSON object:
```json
{
  "clients": {
    "<clientUuid>": {
      "clientUuid": "uuid-string",
      "athleteId": 1,
      "clientName": "Александр Смирнов",
      "avatarBase64": "data:image/jpeg;base64,...",
      "syncTimestamp": 1727974000000,
      "assignedWorkouts": [ ... ],
      "anthropometry": [ ... ]
    }
  },
  "pairing": {
    "739102": {
      "pin": "739102",
      "clientUuid": "uuid-string",
      "clientName": "Александр Смирнов",
      "coachName": "Алексей Романов",
      "coachPhone": "+7 (999) 123-45-67",
      "coachAvatarBase64": "...",
      "phone": "+7 900 000-00-00",
      "goal": "Набор массы",
      "restrictions": "Нет",
      "notes": "Тренировки 3 раза в неделю",
      "status": "PAIRED",
      "timestamp": 1727974000000,
      "pairedAt": "1727974000000"
    }
  },
  "updates": {
    "trainerVersion": "1.0.5",
    "trainerUrl": "https://github.com/santiyastudio-lgtm/fitness-ecosystem-pro/releases/download/v1.0.5/trainer-pro-v1.0.5.apk",
    "athleteVersion": "1.0.5",
    "athleteUrl": "https://github.com/santiyastudio-lgtm/fitness-ecosystem-pro/releases/download/v1.0.5/athlete-pro-v1.0.5.apk",
    "notes": "Версия 1.0.5: Полный сквозной аудит экосистемы, Room v4, синхронизация и фоновые обновления."
  },
  "updatedAt": "1727974000000"
}
```

### 2.3 In-App Update Engine
- Implemented in `UpdateService.kt` (Trainer Pro) and `AthleteUpdateService.kt` (Athlete Pro).
- Multi-tier check order:
  1. Primary: Google Apps Script `updates` node (AES-256).
  2. Secondary: GitHub Releases API (`/repos/santiyastudio-lgtm/fitness-ecosystem-pro/releases/latest`).
  3. Tertiary: Hardcoded fallback in code.
- Download & Install Pipeline:
  - Background HTTP stream download into `context.cacheDir/TrainerPro_Update.apk` or `AthletePro_Update.apk`.
  - File verification: size > 2MB and zip magic bytes check (`PK\x03\x04`).
  - Intent execution via `FileProvider` (`${applicationId}.fileprovider`) with `ACTION_VIEW` and `REQUEST_INSTALL_PACKAGES` permission prompt if needed.
- **Defects in Update Logic**:
  - Hardcoded fallback version `"1.0.2"` in `UpdateService.kt:130` and `AthleteUpdateService.kt:128`.
  - Default `updates` initial block in `GoogleDriveSyncManager.kt:90-95` and `GoogleDriveAthleteSyncManager.kt:158-164` writes `"1.0.4"` if `updates` node is missing. Must be updated to `"1.0.5"`.
  - Hardcoded UI strings in `SettingsScreen.kt` (`v1.0.0` badge on line 765, `v1.0.1` text on lines 491, 494) and `AthleteSettingsScreen.kt` (`v1.0.1` text on lines 744, 747).

---

## 3. Build & Packaging Audit

### 3.1 Project Structure & Toolchain
- Two standalone Gradle projects: `F:\Projects\fitness-ecosystem-pro\trainer-app` and `F:\Projects\fitness-ecosystem-pro\athlete-app`.
- Android Gradle Plugin: `8.5.2`, Kotlin `2.0.0`, KSP `2.0.0-1.0.24`, JVM target: `Java 17`.
- Target SDK: `compileSdk = 34`, `targetSdk = 34`, `minSdk = 26`.
- Version bump target:
  - `trainer-app/app/build.gradle.kts`: `versionCode = 5`, `versionName = "1.0.5"` (currently `4`, `"1.0.4"`).
  - `athlete-app/app/build.gradle.kts`: `versionCode = 5`, `versionName = "1.0.5"` (currently `4`, `"1.0.4"`).

### 3.2 Compilation & Unit Test Status
- `trainer-app`:
  - Command: `cmd /c "gradlew.bat testDebugUnitTest --no-daemon"`
  - Result: **BUILD SUCCESSFUL** (25 tasks up to date, all unit tests passed).
- `athlete-app`:
  - Command: `cmd /c "gradlew.bat testDebugUnitTest --no-daemon"`
  - Result: **BUILD FAILED**
  - **Error details**:
    ```
    e: file:///F:/Projects/fitness-ecosystem-pro/athlete-app/app/src/test/java/com/athleteapp/pro/data/sync/AthleteSyncRemediationTest.kt:164:1
    Class 'FakeAthleteDao' is not abstract and does not implement abstract member 'getAllSets'.
    ```
  - Cause: `getAllSets(): Flow<List<MyWorkoutSetEntity>>` was introduced in `AthleteDao.kt` for Room v4 migration, but the test fake `FakeAthleteDao` in `AthleteSyncRemediationTest.kt` was not updated with this method.

### 3.3 Signing Configuration & Output Locations
- Standard Android Gradle `buildTypes.release` configuration has no signing block, which produces unsigned APKs (`app-release-unsigned.apk`).
- Existing release artifacts (`releases/trainer-pro-v1.0.4.apk` and `releases/athlete-pro-v1.0.4.apk`) were verified using `apksigner verify -v --print-certs`. Both were signed with:
  `Signer #1 certificate DN: C=US, O=Android, CN=Android Debug`
  `SHA-256: bd931d1c1bcbb353a5682783034ecabb5ca4f856dc1c917be956ec1efc52f582`
- Required release artifacts:
  - `F:\Projects\fitness-ecosystem-pro\releases\trainer-pro-v1.0.5.apk`
  - `F:\Projects\fitness-ecosystem-pro\releases\athlete-pro-v1.0.5.apk`
- To achieve reproducible release builds installable on test and user devices without password dependencies, `signingConfig = signingConfigs.getByName("debug")` should be set for the release build type in `app/build.gradle.kts` of both apps, or the output from `assembleDebug` can be placed into `releases/`.

---

## 4. GitHub Release & Deployment Tooling Audit

### 4.1 CLI & Authentication Status
- `gh` CLI version: `gh version 2.100.0 (2026-09-03)`.
- Authentication:
  - Account: `santiyastudio-lgtm`.
  - Protocol: `https`.
  - Scopes: `'delete_repo', 'gist', 'read:org', 'repo', 'workflow'`.
- Existing Releases:
  - `v1.0.4` (Latest)
  - `v1.0.3`
  - `v1.0.2`
  - `v1.0.1`
  - `v1.0.0`

### 4.2 Release Deployment Recipe for v1.0.5
When implementation and testing are verified:
```powershell
gh release create v1.0.5 `
  releases/trainer-pro-v1.0.5.apk `
  releases/athlete-pro-v1.0.5.apk `
  --title "Fitness Ecosystem Pro v1.0.5" `
  --notes "Версия 1.0.5: Карточка тренера, синхронизация фото, состязания, статистика упражнений, исправление Room v4 и фоновые обновления."
```

---

## 5. Android Emulator & QA Infrastructure Audit

### 5.1 ADB & Device Environment
- ADB executable: `F:\Development\Android\Sdk\platform-tools\adb.exe` (validated).
- Emulator instance: `emulator-5554`.
  - State: `device`.
  - Device model: `sdk_gphone64_x86_64` (Pixel 8).
  - API Level: `36` (`ro.build.version.sdk = 36`).
  - Resolution: `1080x2400`.
  - Current installed versions:
    - `com.trainerapp.pro`: `versionName = 1.0.4`
    - `com.athleteapp.pro`: `versionName = 1.0.4`

### 5.2 Screencap & Automation Tooling
- Verified screen capture command:
  ```powershell
  cmd /c "F:\Development\Android\Sdk\platform-tools\adb.exe exec-out screencap -p > <destination.png>"
  ```
  Produces crisp, full-fidelity 1080x2400 PNG images without corruption.
- Existing PowerShell test automation:
  - `trainer-app/qa_test.ps1`: Automated lock screen unlock, launch, settings navigation, workout creation, exercise addition, timer activation, history verification, Logcat crash scan (`FATAL|AndroidRuntime`).
  - `trainer-app/qa_screens.ps1`: Automated sequential screen capturing.
  - `athlete-app`: Lacks automated PowerShell QA test scripts.

---

## Recommendations & Implementation Plan

| ID | Component | Issue / Task | Action Required |
|---|---|---|---|
| **REC-1** | `athlete-app` Test | `FakeAthleteDao` missing `getAllSets()` | Add `override fun getAllSets(): Flow<List<MyWorkoutSetEntity>> = flowOf(setsForSession.values.flatten())` in `AthleteSyncRemediationTest.kt:164`. |
| **REC-2** | `athlete-app` Sync | Unpair encryption bypass in `unpairFromCoach()` | Apply `CloudSecurityManager.decryptPayload(cloudJson)` before parsing and `CloudSecurityManager.encryptPayload(...)` before posting in `GoogleDriveAthleteSyncManager.kt:226-237`. |
| **REC-3** | Both Apps | Version bump to v1.0.5 | Set `versionCode = 5`, `versionName = "1.0.5"` in both `build.gradle.kts`. |
| **REC-4** | Both Apps | Hardcoded version UI text | Replace hardcoded `"v1.0.0"` and `"v1.0.1"` in `SettingsScreen.kt` and `AthleteSettingsScreen.kt` with dynamic `v$currentVersionName`. |
| **REC-5** | Update Services | Hardcoded fallback version | Update fallback version from `"1.0.2"` to `"1.0.5"` in `UpdateService.kt` and `AthleteUpdateService.kt`. |
| **REC-6** | Cloud Manifest | GAS `updates` node deployment | Push updated `updates` node with `trainerVersion: "1.0.5"` and `athleteVersion: "1.0.5"` encrypted with AES-256 to GAS Web App. |
| **REC-7** | Release Build | Sign and generate v1.0.5 APKs | Build and copy signed APKs to `releases/trainer-pro-v1.0.5.apk` and `releases/athlete-pro-v1.0.5.apk`. |
| **REC-8** | QA Automation | Missing athlete test script | Create `athlete-app/qa_test.ps1` to automate UI scenario run and capture verification screenshots on Pixel 8 API 36 emulator. |
| **REC-9** | GitHub Release | Publish v1.0.5 | Execute `gh release create v1.0.5` with both release APKs. |
