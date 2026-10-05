# Project: Fitness Ecosystem Pro

## Architecture
- Module boundaries:
  - `web/`: Node.js/Express Fastify backend, SQLite DB (`web/src/db.js`), Security (`web/src/security.js`), Server endpoints (`web/src/server.js`), Telegram Bot (`web/src/bot.js`), Vitest/Jest suites (`web/tests/`).
  - `athlete-app/`: Android Kotlin Compose app for athletes (`AthleteAuthScreen.kt`, `AthleteRemoteAuthManager.kt`), background updater (`AthleteUpdateService.kt`), local Room DB & repositories.
  - `trainer-app/`: Android Kotlin Compose app for trainers (`TrainerAuthScreen.kt`, `TrainerRemoteAuthManager.kt`), background updater (`UpdateService.kt`), sync managers.
  - `releases/` & `web/releases/`: Standalone release APK distribution directory.

## Feature Inventory
| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| 1 | Stateless Session Persistence | Signed HMAC-SHA256 session tokens with DB re-hydration across cold boots and revocation blacklist | M1 | ORIGINAL_REQUEST §R1 |
| 2 | Direct PIN OTP Verification | `/api/auth/telegram/verify-otp` accepts code alone without username | M1 | ORIGINAL_REQUEST §R1 |
| 3 | Profile Parity API | `/api/me` returns camelCase and snake_case pairingCode, fullName, restrictions | M1 | ORIGINAL_REQUEST §R1, R2 |
| 4 | Athlete 1-Step 6-Digit Code Dialog | `AthleteAuthScreen.kt` 1-step direct PIN dialog without username | M2 | ORIGINAL_REQUEST §R2 |
| 5 | Athlete 1-Click TG Login | Native intent `tg://resolve?domain=fitnessecosystemBOT&start=auth_<sessionId>` with web fallback | M2 | ORIGINAL_REQUEST §R2 |
| 6 | Athlete PIN & Profile Sync | Sync 6-digit PIN with backend and auto-refresh on 5-min timer | M2 | ORIGINAL_REQUEST §R2 |
| 7 | Trainer 1-Step 6-Digit Code Dialog | `TrainerAuthScreen.kt` 1-step direct PIN dialog without username | M3 | ORIGINAL_REQUEST §R2 |
| 8 | Trainer 1-Click TG Login | Native intent `tg://resolve?domain=fitnessecosystemBOT&start=$sessionId` with web fallback | M3 | ORIGINAL_REQUEST §R2 |
| 9 | Trainer Data Sync | Ensure `restrictions` and `phone` are preserved across sync managers | M3 | ORIGINAL_REQUEST §R2 |
| 10 | Background Direct APK Updater | Verify in-app updater downloads directly without browser redirect | M2, M3 | ORIGINAL_REQUEST §R3 |
| 11 | Release Build & Deployment | `assembleRelease` for athlete & trainer apps, deploy to `releases/` & `web/releases/` | M4 | ORIGINAL_REQUEST §R3 |
| 12 | Automated Verification & Audit | `npm test` 100% pass, unit tests 100% pass, zero-mocks forensic audit CLEAN | M4 | ORIGINAL_REQUEST §AC |

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | Web & Bot Stateless Auth & Direct PIN | `web/src/db.js`, `web/src/server.js`, `web/src/bot.js`, `web/tests/` | none | IN_PROGRESS |
| M2 | Athlete App Auth Parity & Sync | `athlete-app/.../AthleteAuthScreen.kt`, `AthleteRemoteAuthManager.kt`, etc. | M1 contract | PLANNED |
| M3 | Trainer App Auth Parity & Sync | `trainer-app/.../TrainerAuthScreen.kt`, `TrainerRemoteAuthManager.kt`, etc. | M1 contract | PLANNED |
| M4 | Ecosystem Release Build & Audit | `assembleRelease`, deploy APKs, `npm test`, forensic audit | M1, M2, M3 | PLANNED |

## Interface Contracts
### Web ↔ Mobile & Bot Auth Contract
- `POST /api/auth/telegram/verify-otp`:
  - Request: `{ "code": "123456" }` or `{ "username": "user", "code": "123456" }`
  - Response: `{ "success": true, "token": "fit_...", "user": { "id": 1, "pairingCode": "123456", "fullName": "...", ... } }`
- `GET /api/me`:
  - Headers: `Authorization: Bearer <token>`
  - Response includes both `pairingCode` and `pairing_code`, `fullName` and `full_name`, `avatarBase64` and `avatar_base64`, `restrictions`.
- Token format: `fit_<base64url(userId:role:expiresAt:nonce)>_<signature>`

## Code Layout
- `web/`: Backend and bot code
- `athlete-app/`: Athlete Android Compose app
- `trainer-app/`: Trainer Android Compose app
- `releases/` & `web/releases/`: Standalone release APKs
