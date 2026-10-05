# Project: Fitness Ecosystem Pro (Auth, Session Persistence, Exercise Catalog & Release Validation)

## Architecture
- **Web Portal & Backend**: Node.js 24 + SQLite (`node:sqlite`) + grammY Telegram Bot + HMAC-SHA256 signed stateless session tokens with DB re-hydration.
- **Mobile Android**: Jetpack Compose, Room SQLite, Coroutines/Flow, standalone Gradle builds for `athlete-app` and `trainer-app`.
- **Data Flow**:
  - Athlete/Trainer 6-digit PIN & 1-click Telegram deep link (`tg://resolve?domain=fitnessecosystemBOT&start=auth_<sessionId>`) with 2FA OTP gate.
  - Verification on `/api/auth/telegram/verify-otp` by code alone (`{ code: "123456" }`) without username requirement.
  - Exercise Catalog: 37+ exercises categorised by muscle group, searchable in real-time without carousel or daily caps.
  - Sorting: Completed exercises sort to the bottom; pending exercises at top.
  - Auto-collapse: Completed exercises auto-collapse 1 hour after first exercise of session, with manual toggle.
  - Background APK updater: Direct socket download to cache and `FileProvider` intent without browser redirect.

## Feature Inventory
| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| 1 | Stateless Session Persistence (Web) | HMAC-SHA256 signed tokens (`fit_<payloadB64>_<sig>`) with DB re-hydration on Render restarts | M1 | R1 |
| 2 | Direct PIN OTP Auth (Web) | `/api/auth/telegram/verify-otp` accepts `{ code: "123456" }` without username | M1 | R1 |
| 3 | Exercise Catalog Expansion (Web) | Expand `EXERCISE_CATALOG` in `web/src/public/app.js` to 37 exercises (>=35) across muscle groups | M1 | R2 |
| 4 | Exercise Sorting & 1-Hr Auto-Collapse (Web) | Completed sort to bottom, auto-collapse after 1 hour with toggle button | M1 | R2 |
| 5 | Exercise Catalog Search & Sorting (Android Trainer) | 37 exercises by muscle group, real-time search, completed sort to bottom | M1 | R2 |
| 6 | Exercise 1-Hr Auto-Collapse & Toggle Fix (Android Trainer) | Fix toggle UI bug to collapse set rows, implement 1-hr auto-collapse | M1 | R2 |
| 7 | Exercise Sorting & 1-Hr Auto-Collapse (Android Athlete) | Completed sort to bottom, implement 1-hr auto-collapse with toggle | M1 | R2 |
| 8 | Mobile Auth Parity (Android Both) | 1-step 6-digit code entry dialog without username, 1-click Telegram deep linking | M1 | R3 |
| 9 | In-App Direct APK Updater (Android Both) | Direct background download via `HttpURLConnection` and `FileProvider` | M1 | R3 |
| 10 | Automated Test Suites & Build Assembly | `npm test` (57+ security/sync tests), `./gradlew.bat testDebugUnitTest`, `./gradlew.bat assembleRelease` | M2 | R1, R2, R3 |
| 11 | Independent Review & Forensic Audit | Zero-Mocks verification, reviewer approvals, challenger stress tests, forensic audit clean | M3 | R1, R2, R3 |

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | Remediation & Parity Fixes | Web catalog expansion (37 items), Trainer App collapse UI fix & 1-hr collapse, Athlete App 1-hr collapse | none | IN_PROGRESS |
| M2 | Test Suites & Release Builds | `npm test`, `./gradlew.bat testDebugUnitTest` for both apps, `assembleRelease` and publish to `releases/` | M1 | PLANNED |
| M3 | Gate Reviews & Forensic Audit | 2 Reviewers, 2 Challengers, 1 Forensic Auditor (Zero-Mocks, integrity verification) | M2 | PLANNED |

## Interface Contracts
### Auth & Session Contract
- Signed Token Format: `fit_<base64UrlPayload>_<32charHmacSha256>`
- Verify OTP Request: `POST /api/auth/telegram/verify-otp` with `{ code: "123456" }` (username optional)
- Telegram Deep Link: `tg://resolve?domain=fitnessecosystemBOT&start=auth_<sessionId>`

### Exercise & Workout Contract
- Exercise Catalog: >=35 exercises grouped by muscle groups (Грудь, Спина, Ноги, Плечи, Руки, Пресс/Кор).
- Sorting: `compareBy { isCompleted }.thenBy { order/id }` (0 for incomplete, 1 for complete).
- Collapse Rule: `isCompleted && (now - sessionStart >= 3600000)`. Manual toggle overrides default state.

## Code Layout
- `web/src/public/app.js`: Web client, `EXERCISE_CATALOG`, sorting and collapse logic.
- `web/src/server.js`: Web API server, OTP verification route.
- `web/src/security.js`: HMAC token generation and verification.
- `web/src/db.js`: SQLite storage, token re-hydration and revocation.
- `trainer-app/app/src/main/java/com/trainerapp/pro/`:
  - `ui/screens/WorkoutScreen.kt`: Trainer workout screen, exercise search, sorting, collapse.
  - `data/local/TrainerDatabase.kt`: Pre-seeded 37 exercises.
- `athlete-app/app/src/main/java/com/athleteapp/pro/`:
  - `ui/screens/AthleteTodayScreen.kt`: Athlete workout screen, sorting, collapse.
- `releases/`:
  - `athlete-latest.apk`, `trainer-latest.apk`.
