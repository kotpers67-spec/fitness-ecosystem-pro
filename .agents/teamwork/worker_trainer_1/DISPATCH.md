## 2026-10-03T17:41:00Z
You are Worker 1 (Trainer Pro Remediation Specialist).
Your working directory is: F:\Projects\fitness-ecosystem-pro\.agents\teamwork\worker_trainer_1
Original user request file: F:\Projects\fitness-ecosystem-pro\.agents\teamwork\ORIGINAL_REQUEST.md
Project plan: F:\Projects\fitness-ecosystem-pro\PROJECT.md
Explorer reports:
- F:\Projects\fitness-ecosystem-pro\.agents\teamwork\explorer_trainer_1\report.md
- F:\Projects\fitness-ecosystem-pro\.agents\teamwork\explorer_trainer_1\handoff.md

WRITE OWNERSHIP: You exclusively own files in F:\Projects\fitness-ecosystem-pro\trainer-app/**. DO NOT edit files outside this directory.

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A forensic auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

Your tasks:
1. Exercise Stats Dialog (WorkoutScreen.kt):
   - Decouple stats dialog from `activeExerciseTriple`. Store `selectedStatsExercise: ExerciseEntity?`.
   - In `onViewStats`, resolve the exercise entity and store it in `selectedStatsExercise`.
   - Condition `if (showExerciseStatsDialog && selectedStatsExercise != null)` so it displays even when today's workout has no exercises yet.
   - Show `selectedStatsExercise.name` in the dialog title.
2. Trainer Card Sync (MainViewModel.kt & GoogleDriveSyncManager.kt):
   - In `MainViewModel.pairClientByCode`: fetch the coach's actual profile (name, phone, avatar) from Room DB/state instead of fallback defaults, and pass to `findAndPairAthlete`.
   - In `GoogleDriveSyncManager.syncClient`: update the cloud pairing entry so the athlete receives updated coach details during sync.
3. Avatar Display (HomeScreen.kt):
   - Replace static drawables with `ClientAvatar` in the top bar (coach avatar) and client card, utilizing Base64/photoUri with fallback.
4. Sanitize Branding & Version Strings:
   - Sanitize "Google Диск" strings in UI/messages to neutral "Облако".
   - Update fallback versions in `UpdateService.kt` to "1.0.5".
   - Update version labels in `SettingsScreen.kt` to "v1.0.5".
5. Version Bump & Release Signing:
   - In `trainer-app/app/build.gradle.kts`: bump `versionCode = 5`, `versionName = "1.0.5"`.
   - In `buildTypes.release`: set `signingConfig = signingConfigs.getByName("debug")` so release builds are signed.
6. Build and Test:
   - Run `cmd /c "cd trainer-app && gradlew.bat testDebugUnitTest --no-daemon"`.
   - Run `cmd /c "cd trainer-app && gradlew.bat assembleRelease --no-daemon"`.
   - Copy the generated release APK (`app/build/outputs/apk/release/app-release.apk`) to `F:\Projects\fitness-ecosystem-pro\releases\trainer-pro-v1.0.5.apk` (create `releases/` directory if needed).

Write your completion report to `F:\Projects\fitness-ecosystem-pro\.agents\teamwork\worker_trainer_1\report.md` and handoff to `F:\Projects\fitness-ecosystem-pro\.agents\teamwork\worker_trainer_1\handoff.md`.
Then send a message to parent when finished.
