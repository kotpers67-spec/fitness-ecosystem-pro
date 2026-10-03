# BRIEFING — 2026-10-03T19:18:00Z

## Mission
Investigate Trainer Pro & Athlete Pro Android codebases for camera/QR crash prevention, CursorWindow/avatar optimization, MainViewModel flow init safety, and release build readiness.

## 🔒 My Identity
- Archetype: Explorer
- Roles: Mobile Survey Explorer, Investigator, Synthesizer
- Working directory: F:\Projects\fitness-ecosystem-pro\.agents\teamwork\explorer_survey_mobile_1
- Original parent: f19f8947-a22d-4cff-98b7-961f56b45b31
- Milestone: Mobile Crash & Release Survey

## 🔒 Key Constraints
- Read-only investigation — do NOT implement
- Write only to own directory (.agents/teamwork/explorer_survey_mobile_1)
- Never modify source code directly
- Document exact file paths, line numbers, current logic, and proposed implementation details in handoff.md

## Current Parent
- Conversation ID: f19f8947-a22d-4cff-98b7-961f56b45b31
- Updated: 2026-10-03T19:10:39Z

## Investigation State
- **Explored paths**:
  - `trainer-app/app/src/main/java/com/trainerapp/pro/ui/screens/HomeScreen.kt` (lines 447-548)
  - `trainer-app/app/src/main/java/com/trainerapp/pro/util/QrCodeScannerHelper.kt` (lines 1-38)
  - `trainer-app/app/src/main/java/com/trainerapp/pro/ui/MainViewModel.kt` (lines 128-208)
  - `trainer-app/app/src/main/java/com/trainerapp/pro/data/sync/GitHubSyncManager.kt` (lines 205-225)
  - `trainer-app/app/src/main/java/com/trainerapp/pro/data/local/entities/TrainerEntities.kt` (lines 1-50)
  - `trainer-app/app/src/main/java/com/trainerapp/pro/ui/components/CommonComponents.kt` (lines 240-285)
  - `athlete-app/app/src/main/java/com/athleteapp/pro/ui/AthleteViewModel.kt` (lines 100-140, 320-355)
  - `athlete-app/app/src/main/java/com/athleteapp/pro/data/local/entities/AthleteEntities.kt` (lines 1-60)
  - `athlete-app/app/src/main/java/com/athleteapp/pro/data/sync/GoogleDriveAthleteSyncManager.kt` (lines 60-95)
  - `athlete-app/app/src/main/java/com/athleteapp/pro/ui/components/CommonComponents.kt` (lines 25-60)
  - `trainer-app/app/build.gradle.kts` & `athlete-app/app/build.gradle.kts`
- **Key findings**:
  1. `HomeScreen.kt` lacks `CAMERA` runtime permission launcher before `cameraLauncher.launch(null)`.
  2. `QrCodeScannerHelper.kt` catches `Exception` instead of `Throwable` (ignoring `OutOfMemoryError`) and does not downscale input images.
  3. Avatars in both apps currently scale to 512x512 JPEG 85% (~50-100KB Base64), risking `SQLiteBlobTooBigException` in Room; need max 128x128, JPEG 75%, size < 15KB with `catch (Throwable)` in avatar renderers.
  4. `MainViewModel.kt` line 177 uses `clients.filter { it.isNotEmpty() }.first()`, blocking the background sync and update checking loop permanently on an empty database.
  5. Both apps configure `signingConfig = signingConfigs.getByName("debug")` under `buildTypes.release` and build cleanly via `./gradlew assembleRelease` generating 13MB release APKs.
- **Unexplored areas**: None within the survey scope.

## Key Decisions Made
- Fully documented all 4 investigation areas with verbatim code, line numbers, analysis, and proposed drop-in implementations in `handoff.md`.

## Artifact Index
- DISPATCH.md — incoming task dispatches
- BRIEFING.md — persistent working memory
- progress.md — liveness heartbeat
- handoff.md — structured 5-component handoff report
