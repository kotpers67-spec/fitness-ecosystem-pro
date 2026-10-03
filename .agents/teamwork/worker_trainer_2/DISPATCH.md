## 2026-10-03T18:04:10Z
You are Worker Trainer 2 (Trainer Pro User Directives & Refinement).
Your working directory is: F:\Projects\fitness-ecosystem-pro\.agents\teamwork\worker_trainer_2
Original user request file: F:\Projects\fitness-ecosystem-pro\.agents\teamwork\ORIGINAL_REQUEST.md
Project plan: F:\Projects\fitness-ecosystem-pro\PROJECT.md

WRITE OWNERSHIP: You exclusively own files in F:\Projects\fitness-ecosystem-pro\trainer-app/**. DO NOT edit files outside this directory.

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A forensic auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

CRITICAL USER DIRECTIVE TO IMPLEMENT:
1. Pairing code input without dashes:
   - In `HomeScreen.kt` (pairing dialog) and `GoogleDriveSyncManager.kt`:
     - Allow entering 6 digits directly without hyphens (e.g. 265507).
     - Auto-strip any non-digit characters (`val cleanCode = input.filter { it.isDigit() }`).
     - If user pastes a link (e.g. `https://fitnessapp.pro/pair?code=265507`), automatically extract the 6-digit code.
     - Update UI placeholder text: "739102 (без тире)".
2. Sanitize any remaining provider strings:
   - In `SettingsScreen.kt:481`: replace "Запрос к GitHub Releases..." with "Проверка обновлений..."
3. Zero-Mocks Data Audit:
   - Verify that Trainer Pro's Room database, initial migrations, and DAOs do not contain any hardcoded fake/mock clients or test data.
4. Build & Test:
   - Run `cmd /c "cd trainer-app && gradlew.bat testDebugUnitTest --no-daemon"`.
   - Run `cmd /c "cd trainer-app && gradlew.bat assembleRelease --no-daemon"`.
   - Overwrite `F:\Projects\fitness-ecosystem-pro\releases\trainer-pro-v1.0.5.apk` with the newly built signed release APK.

Write your report to `F:\Projects\fitness-ecosystem-pro\.agents\teamwork\worker_trainer_2\report.md` and handoff to `F:\Projects\fitness-ecosystem-pro\.agents\teamwork\worker_trainer_2\handoff.md`.
Then send a message to parent.
