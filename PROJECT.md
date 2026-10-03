# Project: Fitness Ecosystem Pro v1.0.5

## Architecture
- **Trainer Pro** (`trainer-app`): Jetpack Compose, Room DB, BLE heart rate monitor, AES-256 cloud sync, NeuroAdaptive Engine.
- **Athlete Pro** (`athlete-app`): Jetpack Compose, Room DB, Gamification & Leaderboard, AES-256 cloud sync, In-App Update receiver.
- **Cloud Backend**: Google Apps Script endpoint (`https://script.google.com/macros/s/AKfycbx6LCbVlxZa-MsWrP0QlNouJwcEcZVsbYHlO4HwHPDDuT6_dp0FDUKTmU_Ax5vg7EP6/exec`), AES-256 ECB PKCS5Padding with key `Spirit5449@2011@213@`.
- **Target Verification Environment**: Android Emulator Pixel 8 API 36 (`emulator-5554`), 1080x2400.

## Feature Inventory
| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| 1 | Photo / Avatar Sync | Base64 encoding/decoding, caching, null/empty fallback, dynamic rendering in `HomeScreen.kt` and `ClientAvatar` | M1, M2 | survey |
| 2 | Trainer Card Transmission & Display | Settings profile saving in Trainer Pro, sync to cloud `pairing` node, reception and display in Athlete Pro, call button Intent, clean unlinking with full cleanup | M1, M2 | survey |
| 3 | Competitions & Leaderboard | Score calculation for workout count and tonnage, leaderboard ranking, privacy filter (`!isPrivate \|\| !it.isMe`) hiding user profile while showing competitors | M2 | survey |
| 4 | Previous Workout Stats Dialog | View previous exercise stats when adding an exercise in Trainer Pro, decoupled from `activeExerciseTriple` | M1 | survey |
| 5 | Background Auto-Update & Versioning | Background update checking, encrypted `updates` cloud node, bump `versionCode=5`, `versionName="1.0.5"`, sanitize provider names ("Google Диск", "GitHub") | M1, M2, M4 | survey |
| 6 | Unit Test & Build Integrity | Fix `FakeAthleteDao.getAllSets()` in Athlete Pro, ensure all `./gradlew testDebugUnitTest` pass, configure release signing | M1, M2 | survey |
| 7 | Zero-Mocks Pixel 8 Verification | Real APK install on emulator-5554, end-to-end execution of all user flows, screenshot capture and visual verification | M3 | survey |
| 8 | GitHub Release & Cloud Deploy | Standalone release APKs in `releases/`, `gh release create v1.0.5`, update encrypted `updates` node on GAS backend | M4 | survey |

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | Trainer Pro Remediation | Fix stats dialog, trainer card sync, HomeScreen avatars, sanitize branding, bump v1.0.5, test | none | IN_PROGRESS |
| M2 | Athlete Pro Remediation | Fix test compilation, privacy filter, unpair AES-256 encryption & cleanup, sanitize branding, bump v1.0.5, test | none | IN_PROGRESS |
| M3 | Zero-Mocks Pixel 8 Verification | Install both APKs on emulator-5554, run full E2E scenarios, capture screenshots of all screens, Anti-Overlap verification | M1, M2 | PLANNED |
| M4 | Cloud Deploy & GitHub Release | Generate releases/*.apk, `gh release create v1.0.5`, post encrypted v1.0.5 update to Google Apps Script | M3 | PLANNED |

## Interface Contracts
### Trainer Pro ↔ Athlete Pro Cloud Sync
- **Encryption**: AES-256 ECB PKCS5Padding with key derived from SHA-256 of `Spirit5449@2011@213@`. Ciphertext prefixed with `ENC:`.
- **Payload Node `pairing[pin]`**:
  - `pin`: String (6 digits)
  - `coachName`: String
  - `coachPhone`: String
  - `coachAvatarBase64`: String?
  - `pairedAt`: Long
- **Payload Node `updates`**:
  - `trainerVersion`: "1.0.5"
  - `trainerUrl`: String (GitHub release direct APK link)
  - `athleteVersion`: "1.0.5"
  - `athleteUrl`: String (GitHub release direct APK link)
  - `notes`: String
- **Payload Node `clients[uuid]`**:
  - `AthleteSyncPayload`: sets, workouts, tonnage, avatarBase64, privacy flag.

## Code Layout
- `trainer-app/`: Trainer Pro Android project (Gradle, Kotlin, Jetpack Compose, Room)
- `athlete-app/`: Athlete Pro Android project (Gradle, Kotlin, Jetpack Compose, Room)
- `releases/`: Output directory for release APKs (`trainer-pro-v1.0.5.apk`, `athlete-pro-v1.0.5.apk`)
