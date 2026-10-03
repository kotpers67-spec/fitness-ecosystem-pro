# Project: fitness-ecosystem-pro

## Architecture
- **Mobile Track**: Android Kotlin (Jetpack Compose, Room SQLite, Coroutines/Flow, Material 3, ZXing).
  - Trainer Pro (`trainer-app`): Coach management, client pairing by 6 digits or QR, workouts, synchronization.
  - Athlete Pro (`athlete-app`): Workout execution, set logging, 6-digit PIN and share link generation, competitions.
- **Web Track**: Node.js 24 native (`node:http`, `node:sqlite`, `node:crypto`, `node:test`).
  - Backend API: REST endpoints with parameterized queries, scrypt auth, sliding-window rate limiting.
  - Frontend SPA: Zero-dependency Swiss Style SPA (`#0d0d0d`, Bento Grid, Anti-Overlap Guard, pure SVG QR generator, responsive).
  - Leaderboard: 100% real database records (Zero-Mocks).
- **Security Track**: OWASP-hardened endpoints with comprehensive test suite (`web/tests/security.test.js`) verifying SQLi, XSS, rate limiting, role boundaries, and path traversal immunity.

## Code Layout
- `trainer-app/app/src/main/java/com/trainerapp/pro/ui/screens/HomeScreen.kt`: Camera permission & QR scan dialog.
- `trainer-app/app/src/main/java/com/trainerapp/pro/util/QrCodeScannerHelper.kt`: QR decode, image downscale, Throwable catch.
- `trainer-app/app/src/main/java/com/trainerapp/pro/ui/MainViewModel.kt`: Avatar compression (<15KB), safe Flow init on empty DB.
- `athlete-app/app/src/main/java/com/athleteapp/pro/ui/AthleteViewModel.kt`: Avatar compression (<15KB).
- `trainer-app/app/src/main/java/com/trainerapp/pro/ui/components/CommonComponents.kt`: Safe Base64 avatar decoding.
- `athlete-app/app/src/main/java/com/athleteapp/pro/ui/components/CommonComponents.kt`: Safe Base64 avatar decoding.
- `web/src/server.js`: Web API routing, static file hosting, rate limiter, security headers.
- `web/src/db.js`: SQLite schema, parameterized queries, user/workout/pairing management.
- `web/src/security.js`: Password hashing, input sanitization, rate limiter.
- `web/src/public/`: Static SPA assets (`index.html`, `styles.css`, `app.js`, `qr.js`).
- `web/tests/security.test.js`: Security penetration test suite.

## Feature Inventory
| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| 1 | Camera Runtime Permission | Runtime `CAMERA` permission request in Trainer Pro pairing dialog before launching camera | M1 | survey |
| 2 | QrCodeScannerHelper OOM Fix | Downscale images to max 800px, catch `Throwable` (OOM and Error) during QR decoding | M1 | survey |
| 3 | Avatar CursorWindow Compression | Downscale avatars to 128x128, JPEG 75%, size < 15 KB in both apps to prevent `SQLiteBlobTooBigException` | M1 | survey |
| 4 | Safe Flow Init on Empty DB | Replace blocking `clients.filter { it.isNotEmpty() }.first()` in MainViewModel with non-blocking initial read | M1 | survey |
| 5 | Mobile Release Assembly | Ensure both Athlete Pro and Trainer Pro build clean signed APKs via `assembleRelease` | M1 | survey |
| 6 | Web Backend Enhancements | Add toggle set completion, delete set, user role switching, unpair endpoints, and export authLimiter | M2 | survey |
| 7 | Swiss Dark SPA Portal | Complete responsive SPA on `http://localhost:3000` (#0d0d0d, Anti-Overlap, SVG QR, 6-digit pairing, real leaderboard) | M2 | survey |
| 8 | Security Penetration Test Suite | `web/tests/security.test.js` with 100% PASS for SQLi, XSS, rate limiting, and role isolation | M3 | survey |
| 9 | End-to-End Certification | Full system verification (zero mocks, clean builds, active portal, 0 security vulnerabilities) | M3 | survey |

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | Mobile App Crash Fixes & Stability | Runtime camera check, QrCodeScannerHelper OOM fix, Avatar compression (<15KB), MainViewModel safe init, `assembleRelease` | none | DONE |
| M2 | Local Web Portal (Frontend & API) | Backend endpoint extensions + complete Swiss Dark SPA in `web/src/public` with 6-digit PIN, QR, and real leaderboard | none | DONE |
| M3 | Security Test Suite & Hardening | `web/tests/security.test.js` with 100% PASS (26/26 tests, 0 vulnerabilities), path traversal hardening | M2 | DONE |

## Interface Contracts
### Web API ↔ SPA Frontend
- `POST /api/register` -> `{ username, password, role: 'athlete'|'trainer', fullName, phone }` => `{ token, user }`
- `POST /api/login` -> `{ username, password }` => `{ token, user }`
- `GET /api/me` -> Headers: `Authorization: Bearer <token>` => `{ user }`
- `POST /api/user/role` -> `{ role: 'athlete'|'trainer' }` => `{ user }`
- `POST /api/trainer/pair` -> `{ code: '123456' }` => `{ success: true, athlete }`
- `POST /api/trainer/unpair` -> `{ athleteId }` => `{ success: true }`
- `POST /api/athlete/unpair` => `{ success: true }`
- `GET /api/workout?date=YYYY-MM-DD` => `{ session, sets: [...] }`
- `POST /api/workout/set` -> `{ exerciseName, weightKg, reps, rpe }` => `{ set }`
- `POST /api/workout/set/toggle` -> `{ setId, isCompleted }` => `{ success: true }`
- `DELETE /api/workout/set` -> `{ setId }` => `{ success: true }`
- `GET /api/leaderboard` => `{ leaderboard: [...] }` (100% real data, empty if no public sessions)

### Mobile Data Contract
- Avatar JPEG: max 128x128 px, quality 75%, byte size < 15360 bytes (<15 KB).
- Pairing Code: 6 digits (`\d{6}`), no dashes.
