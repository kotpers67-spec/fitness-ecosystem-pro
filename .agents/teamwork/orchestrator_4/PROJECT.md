# Project: Fitness Ecosystem Pro — Comprehensive Ecosystem Audit & Hardening

## Architecture
- **Mobile Android Clients**:
  - `athlete-app`: Kotlin / Jetpack Compose, Room SQLite v4, 5-min dynamic 6-digit PIN, QR generator, Google Drive AES-256 cloud sync, CursorWindow-safe 128x128 <15KB avatars.
  - `trainer-app`: Kotlin / Jetpack Compose, Room SQLite v3, 72h registration approval dialog with @SantiLA213 and @Spirit5449 owner contacts, 6-digit PIN/QR camera scanner with OOM protection, strict cloud pairing validation, Google Drive AES-256 cloud sync.
- **Web Portal & Backend**:
  - `web`: Native Node.js 24 + SQLite (`node:sqlite`) in WAL mode (`PRAGMA busy_timeout = 5000;`), Swiss Clean UI dark theme (#0d0d0d), Anti-Overlap Guard (`min-width: 0`), client-side vector SVG QR generator, dual session persistence (localStorage + cookie Max-Age 1 yr) with anti-false-logout protection.
  - `bot`: grammY v1.28.0 Telegram Bot with deep linking `/start link_<token>`, OTP delivery, `/approve_<id>`, and owner contacts.
- **Cloud Protocol Parity**:
  - AES-256-ECB with SHA-256 key `Spirit5449@2011@213@` and `ENC:` prefix across Android (`CloudSecurityManager.kt`) and Web (`cloudSync.js`).

## Feature Inventory
| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| 1 | Athlete Dynamic 5-min PIN & QR | 6-digit PIN, 300s ticker, auto-refresh, QR vector/image generator | M3 | survey_athlete_1 |
| 2 | Athlete Training Diary & Weight Progress | Room entities, workout sessions, body weight history, 0-mocks leaderboard | M3 | survey_athlete_1 |
| 3 | Athlete Cloud Sync & CursorWindow Guard | AES-256 Google Drive sync, 128x128 <15KB avatar compression | M3 | survey_athlete_1 |
| 4 | Trainer 72h Owner Approval & Links | Modal dialog with 72h notice, links to @SantiLA213 and @Spirit5449, remote approval sync | M1 | survey_trainer_1 |
| 5 | Trainer 6-Digit PIN/QR Linking | Strict validation (5-min TTL, single-use, no fake clients), camera QR scanner with OOM guard | M1 | survey_trainer_1 |
| 6 | Trainer Workout Assignment & Restrictions Row | Exercise programming, sets/reps, athlete injuries/restrictions banner on WorkoutScreen | M1 | survey_trainer_1 |
| 7 | Trainer 2FA Remote Verification | Remote verification against `/api/auth/telegram/verify-otp` with graceful fallback | M1 | survey_trainer_1 |
| 8 | Web OWASP Hardening & Session Resilience | 0 SQLi/XSS/IDOR, RateLimiting, dual storage (localStorage + cookie), anti-false-logout | M2 | survey_web_cloud_1 |
| 9 | Web Telegram Bot & 2FA OTP | grammY deep linking, 6-digit OTP delivery, 5-min TTL, owner contacts | M2 | survey_web_cloud_1 |
| 10 | Cloud Protocol Parity (AES-256 & Avatars) | 100% cryptographic parity, 128x128 <15KB avatars, bidirectional anthropometry sync | M2 | survey_web_cloud_1 |
| 11 | Leaderboard Scoring Scale Parity | Standardize scoring formula across mobile and web platforms | M2 | survey_web_cloud_1 |
| 12 | Full Automated Test Suites Execution | 100% pass: `npm test` (55+13), `trainer-app testDebugUnitTest`, `athlete-app testDebugUnitTest` | M4 | All surveys |
| 13 | Release APK Packaging & Signature Verification | `assembleRelease` produces valid v2 signed release APKs (v1.0.8, code 8) for athlete and trainer | M4 | All surveys |
| 14 | Forensic Zero-Mocks & Integrity Certification | Zero banned mock athletes in code/DB/APK, 100% authentic implementations | M4 | All surveys |

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | Trainer Pro Remediation & Hardening | Athlete restrictions banner on `WorkoutScreen.kt`, 2FA OTP remote verify, remote approval check | none | DONE |
| M2 | Web & Cloud Parity Harmonization | Anthropometry sync in `cloudSync.js`, Leaderboard points alignment, test suite verification | none | DONE |
| M3 | Athlete Pro Polish | Version string update, cleanup fallback strings, unit tests and release build verification | none | DONE |
| M4 | Final E2E Dual-Track & Forensic Audit | Full ecosystem test execution, release APK checks, forensic zero-mocks certification | M1, M2, M3 | PLANNED |

## Interface Contracts
### Mobile ↔ Cloud ↔ Web Protocol Parity
- **Payload Format**: `ENC:` + Base64(AES-256-ECB(JSON, SHA-256("Spirit5449@2011@213@")))
- **Pairing Entry**:
  ```json
  {
    "clientUuid": "uuid-v4",
    "pin": "123456",
    "timestamp": 1728038400000,
    "status": "PENDING" | "PAIRED" | "USED",
    "coachName": "string",
    "coachPhone": "string",
    "coachAvatarBase64": "string"
  }
  ```
- **Athlete Sync Payload**: Includes `clientUuid`, `assignedWorkouts`, `anthropometry`, `avatarBase64` (<15KB).
- **Leaderboard Points Formula**: Standardized as `workoutsCount * 10 + floor(tonnage / 100)`.

## Code Layout
- `athlete-app/app/src/main/java/com/athleteapp/pro/`:
  - `ui/screens/`: `AthleteSettingsScreen.kt`, `AthleteAuthScreen.kt`, `LeaderboardScreen.kt`, `WorkoutScreen.kt`
  - `ui/AthleteViewModel.kt`
  - `data/sync/GoogleDriveAthleteSyncManager.kt`, `CloudSecurityManager.kt`
- `trainer-app/app/src/main/java/com/trainerapp/pro/`:
  - `ui/screens/`: `HomeScreen.kt`, `SettingsScreen.kt`, `TrainerAuthScreen.kt`, `WorkoutScreen.kt`
  - `ui/MainViewModel.kt`
  - `data/sync/GoogleDriveSyncManager.kt`, `CloudSecurityManager.kt`
- `web/`:
  - `src/server.js`, `src/db.js`, `src/bot.js`, `src/security.js`, `src/cloudSync.js`
  - `src/public/`: `index.html`, `app.js`, `styles.css`, `qr.js`
  - `tests/`: `security.test.js`, `pin_2fa.test.js`, `verification_otp_stress.test.js`
