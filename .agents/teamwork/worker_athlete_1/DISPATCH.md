## 2026-10-03T17:41:00Z
You are Worker 2 (Athlete Pro Remediation Specialist).
Your working directory is: F:\Projects\fitness-ecosystem-pro\.agents\teamwork\worker_athlete_1
Original user request file: F:\Projects\fitness-ecosystem-pro\.agents\teamwork\ORIGINAL_REQUEST.md
Project plan: F:\Projects\fitness-ecosystem-pro\PROJECT.md
Explorer reports:
- F:\Projects\fitness-ecosystem-pro\.agents\teamwork\explorer_athlete_1\report.md
- F:\Projects\fitness-ecosystem-pro\.agents\teamwork\explorer_athlete_1\handoff.md

WRITE OWNERSHIP: You exclusively own files in F:\Projects\fitness-ecosystem-pro\athlete-app/**. DO NOT edit files outside this directory.

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A forensic auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

Your tasks:
1. Fix Unit Test Compilation (AthleteSyncRemediationTest.kt):
   - In `FakeAthleteDao`, implement `override fun getAllSets(): Flow<List<MyWorkoutSetEntity>> = flowOf(setsForSession.values.flatten())`.
2. Fix Inverted Privacy Filter (LeaderboardScreen.kt):
   - Lines 65-70: Fix `.filter { !isPrivate || !it.isMe }` so when `isPrivate == true`, other athletes remain on the leaderboard and only the user themselves is omitted. Correctly rank athletes after filtering.
3. Fix Coach Unlinking & Encryption (GoogleDriveAthleteSyncManager.kt & AthleteViewModel.kt):
   - In `unpairFromCoach()` and `regeneratePairingPin()`: clear `pairedCoachPhone = ""`, `pairedCoachPhotoUri = null`, `pairedCoachAvatarBase64 = null`.
   - In `unpairFromCoach()`: decrypt cloud response with `CloudSecurityManager.decryptPayload(cloudJson)` before parsing.
   - When posting updated cloud state in `unpairFromCoach()`, encrypt payload with `CloudSecurityManager.encryptPayload(...)` before `httpPost`.
4. Sanitize Branding & Version Strings:
   - Sanitize "Google Диск" and "GitHub" strings in UI/messages to neutral "Облако" / "Сервер обновлений".
   - Update fallback versions in `AthleteUpdateService.kt` to "1.0.5".
   - Update version labels in `AthleteSettingsScreen.kt` to "v1.0.5".
5. Version Bump & Release Signing:
   - In `athlete-app/app/build.gradle.kts`: bump `versionCode = 5`, `versionName = "1.0.5"`.
   - In `buildTypes.release`: set `signingConfig = signingConfigs.getByName("debug")` so release builds are signed.
6. Build and Test:
   - Run `cmd /c "cd athlete-app && gradlew.bat testDebugUnitTest --no-daemon"`.
   - Run `cmd /c "cd athlete-app && gradlew.bat assembleRelease --no-daemon"`.
   - Copy the generated release APK (`app/build/outputs/apk/release/app-release.apk`) to `F:\Projects\fitness-ecosystem-pro\releases\athlete-pro-v1.0.5.apk` (create `releases/` directory if needed).

Write your completion report to `F:\Projects\fitness-ecosystem-pro\.agents\teamwork\worker_athlete_1\report.md` and handoff to `F:\Projects\fitness-ecosystem-pro\.agents\teamwork\worker_athlete_1\handoff.md`.
Then send a message to parent when finished.
