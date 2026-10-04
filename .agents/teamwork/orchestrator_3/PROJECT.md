# Project: Fitness Ecosystem Pro (Dynamic PIN, Telegram 2FA, Owner Contacts)

## Architecture
- **Web Portal & Backend**: Node.js 24 + SQLite (`node:sqlite`) + grammY Telegram Bot (long polling in unified host).
- **Mobile Android**: Jetpack Compose, Room SQLite, Coroutines/Flow, AES-256 Cloud Sync (`CloudSecurityManager.kt`).
- **Data Flow**:
  - Athlete creates dynamic 6-digit PIN with 5-minute TTL. PIN synced to DB (Web) and Cloud/Room (Android).
  - Trainer enters 6-digit PIN. Strict check: not found -> error, >5 min -> 400 error, already used -> 400 error. On success: single-use code consumed.
  - User binds Telegram via `/start link_<token>`. Bot captures numeric `telegram_id` and `@username`.
  - On login with 2FA enabled, backend generates 6-digit OTP (5-min TTL, invalidates prior codes) and bot delivers it to `telegram_id`. Login succeeds only after valid OTP verification.
  - Support/Owner buttons to `@SantiLA213` and `@Spirit5449` displayed across Web (login, athlete, trainer), Android (auth, settings), and Bot (/start, /contacts).

## Feature Inventory
| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| 1 | Dynamic 5-min PIN (Web) | 6-digit PIN, 5-min TTL, single-use consume, live 05:00 timer, auto-refresh | M1 | R1 |
| 2 | Telegram Link & 2FA API (Web) | Deep link token, numeric telegram_id bind, 2FA toggle, login OTP challenge | M1 | R2 |
| 3 | Telegram Bot Integration | grammY bot, deep link handling, OTP delivery via sendMessage, owner contact buttons | M2 | R2, R3 |
| 4 | Athlete Pro Dynamic PIN (Android) | 5-min countdown StateFlow, 05:00 UI ticker, auto-regeneration, cloud sync | M3 | R1 |
| 5 | Trainer Pro Strict Validation (Android) | Strict rejection for missing, >5 min expired, or PAIRED/USED codes (no fake client) | M3 | R1 |
| 6 | Android Telegram Link & 2FA & Contacts | Telegram link button, 2FA toggle & OTP dialog on login, owner contact links | M3 | R2, R3 |
| 7 | Owner Contact Buttons (All) | Direct Telegram links to @SantiLA213 and @Spirit5449 on all entry & profile screens | M1, M2, M3 | R3 |
| 8 | Comprehensive E2E & Security Tests | Security test suite (SQLi, XSS, rate limit) + PIN/2FA test suite (TTL, reuse, OTP) + Android tests | M4 | R1, R2, R3 |

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | Web & Backend Integration | Dynamic PIN API, Telegram linking API, 2FA OTP auth, Web UI timers & owner links | none | IN_PROGRESS |
| M2 | Telegram Bot Enhancement | grammY bot deep linking `/start link_<token>`, OTP delivery, owner buttons | M1 | PLANNED |
| M3 | Android Mobile Apps | Athlete Pro 5-min timer & auto-regen, Trainer Pro strict validation, Auth/Settings UI | none | PLANNED |
| M4 | E2E & Dual-Track Verification | Comprehensive automated test runs (Web + Android unit tests + 0-mock validation) | M1, M2, M3 | PLANNED |

## Interface Contracts
### Web Backend ↔ Telegram Bot
- `db.linkTelegram(userId, telegramId, telegramUsername)`: persists numeric Telegram ID.
- `telegramLinkTokens`: Map / DB storing `{ token, userId, expiresAt }`.
- `bot.api.sendMessage(telegramId, text)`: delivers OTP during `POST /api/login`.
- Owner Links: `https://t.me/SantiLA213`, `https://t.me/Spirit5449`.

### Athlete ↔ Trainer Pairing Contract
- PIN: Exactly 6 numeric digits (`^\d{6}$`).
- Expiry: Strictly 300,000 ms (5 minutes) from creation.
- State: PENDING -> PAIRED -> CONSUMED.
- Errors: Missing -> 400 "Код не найден", Expired -> 400 "Срок действия кода истёк (действует 5 минут)", Used -> 400 "Этот код уже был использован".

## Code Layout
- `web/src/server.js`: Web server, auth routes, pairing routes, 2FA endpoints.
- `web/src/db.js`: SQLite schema, user fields, linkTelegram, consumePairingCode.
- `web/src/bot.js` (or inline grammY): Telegram bot handler.
- `web/src/public/index.html` & `app.js`: Web UI SPA, countdown timer, dialogs.
- `web/tests/`: `security.test.js`, `pin_2fa.test.js`.
- `athlete-app/app/src/main/java/com/athleteapp/pro/`:
  - `ui/screens/AthleteSettingsScreen.kt`, `AthleteAuthScreen.kt`
  - `ui/AthleteViewModel.kt`
  - `data/sync/GoogleDriveAthleteSyncManager.kt`
- `trainer-app/app/src/main/java/com/trainerapp/pro/`:
  - `ui/screens/HomeScreen.kt`, `SettingsScreen.kt`, `TrainerAuthScreen.kt`
  - `ui/MainViewModel.kt`
  - `data/sync/GoogleDriveSyncManager.kt`
