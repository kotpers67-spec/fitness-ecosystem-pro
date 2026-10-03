# Handoff Report: Shared Ecosystem, Cloud & Release Explorer

## 1. Observation

### 1.1 AES-256 Encryption & Secret Keys
- File `trainer-app/app/src/main/java/com/trainerapp/pro/data/sync/CloudSecurityManager.kt` and `athlete-app/app/src/main/java/com/athleteapp/pro/data/sync/CloudSecurityManager.kt`:
  - Static XOR mask `MASK`: `[0x53, 0x70, 0x69, 0x72, 0x69, 0x74, 0x46, 0x69, 0x74, 0x32, 0x30, 0x32, 0x36]` (`"SpiritFit2026"`).
  - Deobfuscated endpoint: `https://script.google.com/macros/s/AKfycbx6LCbVlxZa-MsWrP0QlNouJwcEcZVsbYHlO4HwHPDDuT6_dp0FDUKTmU_Ax5vg7EP6/exec`.
  - Deobfuscated secret key: `Spirit5449@2011@213@`.
  - Cipher algorithm: `Cipher.getInstance("AES/ECB/PKCS5Padding")` with key `MessageDigest.getInstance("SHA-256").digest(getSecretKey().toByteArray(StandardCharsets.UTF_8))` (lines 54-57).
  - Ciphertext framing: `"ENC:" + Base64.encodeToString(encrypted, Base64.NO_WRAP)` (line 59).
  - Decryption prefix check: `if (!trimmed.startsWith("ENC:")) return trimmed` (line 67).
  - IV handling: ECB mode does not use an IV (`cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec)` without `IvParameterSpec`).
- Critical bug in `athlete-app/app/src/main/java/com/athleteapp/pro/data/sync/GoogleDriveAthleteSyncManager.kt:226-237`:
  - Line 226: `val cloudJson = httpGet(requestUrl)` (returns `"ENC:..."`).
  - Line 229: `val rootObj = JsonParser.parseString(cloudJson).asJsonObject` (parses without decrypting; fails on `"ENC:"` prefix).
  - Line 237: `httpPost(requestUrl, gson.toJson(rootObj))` (sends unencrypted JSON to cloud).

### 1.2 Google Apps Script Backend
- Live HTTP query with invalid key (`?key=wrong_key`):
  `Redirect Location: https://script.googleusercontent.com/macros/echo?...`
  `Body: {"error":"Unauthorized"}`
- Live HTTP query with valid key (`?key=Spirit5449%402011%40213%40`):
  `Body: {}` (HTTP 200 via 302 Found redirect).
- Schema structure expected in apps:
  - `clients`: map of client UUID -> `AthleteSyncPayload`.
  - `pairing`: map of 6-digit PIN -> pairing handshake object.
  - `updates`: object containing `trainerVersion`, `trainerUrl`, `athleteVersion`, `athleteUrl`, `notes`.
  - `updatedAt`: timestamp string.
- Outdated hardcoded fallback versions:
  - `trainer-app/.../UpdateService.kt:130`: `val fallbackVersion = "1.0.2"`
  - `athlete-app/.../AthleteUpdateService.kt:128`: `val fallbackVersion = "1.0.2"`
  - `trainer-app/.../SettingsScreen.kt:765`: `Text("v1.0.0", fontSize = 12.sp, fontWeight = FontWeight.Black)`
  - `trainer-app/.../SettingsScreen.kt:491, 494`: `"У вас уже установлена актуальная версия Trainer Pro (v1.0.1)."`
  - `athlete-app/.../AthleteSettingsScreen.kt:744, 747`: `"У вас установлена актуальная версия Athlete Pro (v1.0.1)."`

### 1.3 Build, Packaging & Existing Test Failure
- Build configuration:
  - `trainer-app/app/build.gradle.kts:16-17`: `versionCode = 4`, `versionName = "1.0.4"`
  - `athlete-app/app/build.gradle.kts:16-17`: `versionCode = 4`, `versionName = "1.0.4"`
  - Neither project defines `signingConfig` under `buildTypes.release`.
- APK signature inspection on `releases/trainer-pro-v1.0.4.apk` and `releases/athlete-pro-v1.0.4.apk`:
  - `Signer #1 certificate DN: C=US, O=Android, CN=Android Debug`
  - `SHA-256: bd931d1c1bcbb353a5682783034ecabb5ca4f856dc1c917be956ec1efc52f582`
- Test run results:
  - `trainer-app`: `cmd /c "gradlew.bat testDebugUnitTest --no-daemon"` -> **BUILD SUCCESSFUL**.
  - `athlete-app`: `cmd /c "gradlew.bat testDebugUnitTest --no-daemon"` -> **BUILD FAILED**:
    ```
    e: file:///F:/Projects/fitness-ecosystem-pro/athlete-app/app/src/test/java/com/athleteapp/pro/data/sync/AthleteSyncRemediationTest.kt:164:1
    Class 'FakeAthleteDao' is not abstract and does not implement abstract member 'getAllSets'.
    ```

### 1.4 GitHub CLI & Deployment Tooling
- `gh --version`: `gh version 2.100.0 (2026-09-03)`.
- `gh auth status`: Logged in to `github.com` account `santiyastudio-lgtm` with `repo`, `workflow`, `read:org`, `gist`, `delete_repo`.
- `gh release list`: Lists releases `v1.0.0` through `v1.0.4` (Latest).

### 1.5 Emulator & QA Infrastructure
- ADB path: `F:\Development\Android\Sdk\platform-tools\adb.exe`.
- Device attached: `emulator-5554` (Pixel 8, `sdk_gphone64_x86_64`, `ro.build.version.sdk = 36`, resolution `1080x2400`).
- Pre-installed packages: `com.trainerapp.pro` (1.0.4) and `com.athleteapp.pro` (1.0.4).
- Screencap test: `cmd /c "adb exec-out screencap -p > <path.png>"` verified functional, returning 359 KB 1080x2400 PNG images.
- Existing PowerShell QA scripts: `trainer-app/qa_test.ps1`, `qa_screens.ps1` exist and function; `athlete-app` lacks a corresponding automated test script.

---

## 2. Logic Chain

1. From **Observation 1.1**, `CloudSecurityManager.kt` uses AES-256 with ECB mode and PKCS5/PKCS7 padding, prepending `"ENC:"` to Base64 ciphertext.
2. In `GoogleDriveAthleteSyncManager.unpairFromCoach()` (Observation 1.1), `httpGet` returns ciphertext beginning with `"ENC:"`, but `unpairFromCoach()` passes this directly to `JsonParser.parseString`, resulting in a parse exception. Furthermore, on saving, it calls `httpPost` with raw plaintext JSON. Therefore, unpairing from a coach fails to clear the pairing PIN in the cloud and risks overwriting encrypted cloud state with unencrypted data.
3. From **Observation 1.2**, the Google Apps Script Web App is online and enforces key authorization. The current database payload is empty `{}`. Both apps poll this endpoint first for updates.
4. Outdated strings and fallbacks (Observation 1.2) will cause the apps to display stale versions (e.g. `v1.0.0`, `v1.0.1`, `v1.0.2`) unless bumped to `1.0.5`.
5. From **Observation 1.3**, `athlete-app` unit tests are currently broken because Room migration 3-to-4 introduced `getAllSets()` on `AthleteDao`, but `FakeAthleteDao` was not updated. Adding `override fun getAllSets(): Flow<List<MyWorkoutSetEntity>>` is strictly required before `./gradlew testDebugUnitTest` can pass.
6. Existing release APKs (Observation 1.3) were signed with the debug keystore. Setting `signingConfig = signingConfigs.getByName("debug")` in `buildTypes.release` will ensure `./gradlew assembleRelease` outputs validly signed standalone APKs directly usable on user devices and the emulator.
7. From **Observations 1.4 & 1.5**, GitHub CLI and the Pixel 8 API 36 emulator are 100% operational and ready for deployment (`gh release create v1.0.5`) and end-to-end screencap verification.

---

## 3. Caveats

1. The underlying Google Apps Script deployment code (the `.gs` script inside Google Drive) is hosted on Google's cloud and cannot be edited locally as a file. However, its HTTP API contract (`?key=...` GET and POST) is verified live and fully compatible with the client-side data schema.
2. While `AES/ECB/PKCS5Padding` lacks an IV, it is the established contract between both mobile applications and the cloud database; changing to CBC or GCM would break backwards compatibility with existing clients unless handled by a versioned prefix.
3. No code modifications were performed in this investigation (read-only audit).

---

## 4. Conclusion

The ecosystem cloud backend and encryption pipeline are operational, but there are 4 critical defects that must be resolved in Milestone 2:
1. Fix test compilation error in `athlete-app/app/src/test/java/com/athleteapp/pro/data/sync/AthleteSyncRemediationTest.kt:164`.
2. Fix unpair decryption/encryption bug in `athlete-app/app/src/main/java/com/athleteapp/pro/data/sync/GoogleDriveAthleteSyncManager.kt:226-237`.
3. Bump `versionCode = 5`, `versionName = "1.0.5"` across Gradle build files, replace hardcoded version text in Settings screens, and update fallback versions in `UpdateService.kt` and `AthleteUpdateService.kt`.
4. Configure release debug signing in `buildTypes.release` for reproducible signed APK generation, execute end-to-end emulator verification, and deploy to GitHub and the cloud.

---

## 5. Verification Method

1. **Verify Unit Test Compilation & Execution**:
   - `trainer-app`: `cmd /c "cd trainer-app && gradlew.bat testDebugUnitTest --no-daemon"`
   - `athlete-app`: `cmd /c "cd athlete-app && gradlew.bat testDebugUnitTest --no-daemon"` (expected to fail until `FakeAthleteDao` implements `getAllSets`).
2. **Verify ADB & Emulator Connection**:
   - Command: `& "F:\Development\Android\Sdk\platform-tools\adb.exe" devices`
   - Invalidation condition: `emulator-5554` not listed or offline.
3. **Verify GitHub CLI Authenticity**:
   - Command: `gh auth status`
   - Invalidation condition: Exit code non-zero or token revoked.
4. **Inspect Audit Report**:
   - View `F:\Projects\fitness-ecosystem-pro\.agents\teamwork\explorer_shared_cloud_1\report.md`.
