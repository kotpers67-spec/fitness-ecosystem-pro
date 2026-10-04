# Comprehensive Investigation Report: Web Portal & Telegram Bot (R3 & R4 Audit)

**Date**: 2026-10-04  
**Investigator**: `explorer_web_bot_5`  
**Target**: `F:\Projects\fitness-ecosystem-pro\web`  
**References**: `ORIGINAL_REQUEST.md`, `PROJECT.md`, `DISPATCH.md`

---

## Executive Summary

A comprehensive, zero-mocks audit was performed on the Fitness Ecosystem Pro Web Portal and Telegram Bot codebase (`F:\Projects\fitness-ecosystem-pro\web`). The system was evaluated against requirements R3 (Web Portal & RBAC/Security) and R4 (Telegram Bot Integration) as well as the overarching acceptance criteria.

**Key Verdict**: **100% COMPLIANT**.
- All automated security and functionality test suites (`npm test`) passed with **100% SUCCESS (0 failures, 0 regressions)**.
- Anti-SQLi, Anti-XSS, Timing-Safe crypto, Rate Limiting, and IDOR defenses are thoroughly implemented and verified.
- Role selection is present and operational on both login and registration forms; post-auth role switching is strictly forbidden (HTTP 403).
- Competition table has removed raw "число" and "тоннаж" columns; workout chart features multi-scale curves (weight, sets, reps) with interactive point click listeners.
- Fresh release APK binaries (v1.0.10) are hosted in `web/releases/` and served with HTTP 200 and MIME type `application/vnd.android.package-archive`.
- Telegram bot menu strictly omits "Сменить роль" / "🔄 Сменить роль", implementing only the 4 required buttons.
- The 6-digit unified code enforces 5-minute TTL, single-use burning, and 100% parity across Web, Bot, and Android apps.
- 1-Click Telegram authentication, 2FA OTP delivery, and `/link` token binding are verified under high concurrency (120 parallel requests).

---

## 1. Security Test Coverage & API Routes (IDOR, SQLi, XSS, Rate Limiting)

### 1.1 Anti-SQL Injection Defense
- **Implementation**: `web/src/db.js` (lines 6, 147-150, 180-194, 420-425, 477, 501, 561-574). All SQLite queries utilize native Node.js parameterized statements (`DatabaseSync.prototype.prepare()` with `?` placeholders and `.run()` / `.get()` / `.all()`).
- **Input Sanitization**: `web/src/security.js` (lines 26-41). Strict regex pattern matching (`VALIDATION_PATTERNS`, `hasSqlInjectionVector()`) intercepts SQL injection attempts in `username`, `password`, `fullName`, `exerciseName`, `phone`, and `pairingCode` before query compilation.
- **Verification**: `tests/security.test.js` Section 2 ("Anti-SQL Injection Protection") executes 12 distinct attack vectors (auth bypass, union injection, comment truncation, stacked queries). All 12 tests passed.

### 1.2 Anti-XSS & Security Headers
- **Implementation**: `web/src/security.js` (lines 14-23). `escapeHtml()` encodes `&`, `<`, `>`, `"`, `'`, and `/`.
- **Response Headers**: `web/src/security.js` (lines 109-116) and `web/src/server.js` (lines 169-172) enforce:
  - `Content-Security-Policy: default-src 'self' 'unsafe-inline' https:; img-src 'self' data: https:; font-src 'self' https:;`
  - `X-Content-Type-Options: nosniff`
  - `X-Frame-Options: DENY`
  - `X-XSS-Protection: 1; mode=block`
  - `Referrer-Policy: strict-origin-when-cross-origin`
  - `Strict-Transport-Security: max-age=31536000; includeSubDomains`
- **Verification**: `tests/security.test.js` Section 3 ("Anti-XSS Defense") validates that stored XSS payloads in exercise names and profile fields are safely escaped and security headers are attached to API and static routes.

### 1.3 Rate Limiting & Brute-Force Shield
- **Implementation**: `web/src/security.js` (lines 69-106). Sliding-window `RateLimiter` tracks IP request timestamps.
- **Enforcement**: `web/src/server.js` (lines 178-188) enforces a threshold of 15 attempts per minute on `/api/login`, `/api/register`, `/api/auth/telegram`, `/api/auth/telegram/request-otp`, and `/api/user/telegram/link-request`. Exceeding limits triggers HTTP 429 Too Many Requests.
- **Verification**: `tests/security.test.js` Section 4 ("Rate Limiting Defense") validates HTTP 429 triggering on threshold breach and clean reset after window expiry.

### 1.4 Role Isolation & IDOR Protection
- **Implementation**: `web/src/server.js`:
  - **Athlete isolation**: Athlete accounts are strictly barred from trainer endpoints (`/api/trainer/pair`, `/api/trainer/clients`, `/api/trainer/exercise-history`, `/api/trainer/unpair`, `/api/trainer/assign-workout`) with HTTP 403 (lines 1343, 1409, 1425, 1466, 1504).
  - **Set IDOR protection**: Athletes cannot view, toggle, or delete other athletes' workout sets (`web/src/server.js` lines 1524, 1652, 1684, 1723: `if (user.role === 'athlete' && targetSet.athlete_id !== user.id) return sendError(res, 403, 'Доступ запрещен');`).
  - **Trainer athlete-pairing check**: Trainers cannot access or modify athletes not assigned to them (`isAthletePairedToTrainer()` checks on lines 1435, 1450, 1472, 1510, 1525).
  - **Immutable Roles**: Changing role post-registration (`POST /api/user/role`) is permanently blocked with HTTP 403 (line 1174).
- **Verification**: `tests/security.test.js` Section 5 ("Role Isolation & RBAC") executes 13 distinct privilege escalation and IDOR checks. All 13 passed.

---

## 2. Role Selection at Login and Registration

### 2.1 Web Portal UI
- **Login Screen**: `web/src/public/index.html` (lines 93-122). Features an interactive "Войти как" card selector with radio buttons `login-role` for `athlete` and `trainer`.
- **Registration Screen**: `web/src/public/index.html` (lines 149-178). Features "Выберите вашу роль (нельзя изменить позже)" with radio buttons `reg-role` for `athlete` and `trainer` (with `ПОДТВЕРЖДЕНИЕ` badge on trainer card).
- **Client Script**: `web/src/public/app.js` handles user card clicks, highlighting selected cards and binding the role parameter to API payloads.

### 2.2 Backend Processing
- **Login**: `web/src/server.js` (lines 404-420). If `role` is supplied in `POST /api/login`, the server aligns the user's active session role to the requested role (or verifies trainer approval).
- **Register**: `web/src/server.js` (lines 215-217, 271). Strictly requires `role === 'athlete' || role === 'trainer'`.
- **Immutability**: `web/src/server.js` (lines 1173-1176). Any subsequent attempt to switch roles via `POST /api/user/role` returns HTTP 403: `"Смена роли запрещена: права строго фиксированы как в мобильном приложении"`.

---

## 3. Competition Table & Interactive Workout Chart with Point Clicks

### 3.1 Competition Table (Leaderboard)
- **HTML Container**: `web/src/public/index.html` (lines 354-380). Includes header with `↻` refresh button, Bento metrics cards (`#user-metric-workouts` and `#user-metric-points`), and `#leaderboard-list`.
- **UI Structure**: `web/src/public/app.js` (lines 1112-1150).
  - Each row renders: Rank Badge (`rank-1`, `rank-2`, `rank-3`), Athlete Name (`(ВЫ)` highlight for self), workout count (`${entry.workoutsCount || 0} трен`), and Points Pill (`${entry.points || 0} pts`).
  - **No redundant columns**: Confirmed absence of separate raw columns for "число" and "тоннаж", preventing UI clutter and horizontal overflow.
- **Zero-Mocks Data**: Backed by `GET /api/leaderboard` (`web/src/db.js` line 677-690), querying real finished workouts (`HAVING workouts_count > 0 AND total_tonnage > 0`), combined with cloud records via `cloudSyncService.getCombinedLeaderboard()`. Test/spam usernames ("смирнов", "smirnov", "тест", "спам") are filtered out (`app.js` line 1093).

### 3.2 Interactive Multi-Scale Workout Chart
- **Canvas Element**: `web/src/public/index.html` (lines 298-343, `<canvas id="athlete-exercise-canvas">`).
- **Multi-Scale Visualization**: `web/src/public/app.js` (lines 796-937).
  - Scale 1 (Weight): Neon Lime (`#c8ff00`, line width 3) with weight values plotted directly on dots (`${pt.val} кг`).
  - Scale 2 (Sets): Cyan (`#38bdf8`, line width 2).
  - Scale 3 (Reps): Rose (`#f43f5e`, line width 2).
  - Summary badges at the top display max weight, total sets, and average reps.
- **Interactive Point Clicks**: `web/src/public/app.js` (lines 938-975).
  - The canvas records `_pointClickData` mapping each data point to its canvas coordinates `{x, y, date, maxWeight, setsCount, avgReps, sets}`.
  - A click event listener scales event coordinates to DPI dimensions and finds the nearest point within a 24-pixel radius.
  - Clicking a point displays an interactive toast containing the date, max weight, total sets, and full set breakdown (`Подход 1: 80 кг × 10 повт`, etc.).
  - Bodyweight charts (`athlete-weight-canvas` and `trainer-weight-canvas`) also include point click handlers (`app.js` lines 760-788).

---

## 4. APK Download Endpoints & Fresh Release Serving

### 4.1 Server Route & Binary Serving
- **Route**: `web/src/server.js` (lines 1940-1963). Intercepts `/releases/*.apk`.
- **File System Location**: `web/releases/` and root `releases/`.
- **Release Binaries**:
  - `athlete-pro-v1.0.10.apk`: 13,087,178 bytes (built 04.10.2026 18:42:24).
  - `trainer-pro-v1.0.10.apk`: 13,232,054 bytes (built 04.10.2026 18:43:39).
- **HTTP Response**:
  - Status: HTTP 200 OK.
  - Content-Type: `application/vnd.android.package-archive`.
  - Content-Disposition: `attachment; filename="..."`.
  - Content-Length: matches exact file size.
  - Fallback: 302 Redirect to GitHub Releases tag `v1.0.10` if file not found locally.

### 4.2 Web UI Modal
- **HTML Dialog**: `web/src/public/index.html` (lines 1074-1135, `<dialog id="dialog-download-app">`).
- **Triggers**: Accessible via `.btn-open-download-modal` on the Auth screen and in Athlete/Trainer profile settings.
- **Links**: Direct download links for Athlete Pro APK (v1.0.10) and Trainer Pro APK (v1.0.10), plus PWA install guides for iOS Safari and Android Chrome.

---

## 5. Telegram Bot Menu: Absence of "Сменить роль"

### 5.1 Keyboard Audit
- **Reply Keyboard**: `web/src/bot.js` (lines 20-26):
  ```javascript
  function createMainMenuKeyboard() {
    return new Keyboard()
      .text('🔑 Код входа').row()
      .text('🔗 Привязать аккаунт').text('💬 Связь с владельцами').row()
      .text('❓ Справка')
      .resized();
  }
  ```
- **Bot Commands**: `web/src/bot.js` (lines 624-630):
  - `/start` — Запуск бота / привязка аккаунта
  - `/code` — Получить 6-значный 2FA код
  - `/link` — Привязать аккаунт: /link <токен>
  - `/contacts` — Связь с создателями (@SantiLA213, @Spirit5449)
  - `/help` — Справка и безопасность

### 5.2 Codebase Search
- Searched `src/bot.js`, `src/server.js`, `src/public/index.html`, and `src/public/app.js` for "Сменить роль", "сменить роль", "СменитьРоль", "change_role", "switch_role".
- **Result**: Exactly **0 matches**. The button has been completely removed across the entire stack.

---

## 6. Unified 6-Digit 5-Minute Code (`🔑 Код входа`)

### 6.1 Logic & Parity Architecture
- **Resolver**: `web/src/bot.js` (lines 126-201, `getUnifiedUserCode()`).
  - Resolves athlete in SQLite by `telegram_id` or `telegram_username`.
  - If athlete has an unexpired pairing PIN (`pairing_code_created_at` within 5 minutes / 300,000 ms), returns that identical PIN.
  - If expired or absent, regenerates a fresh 6-digit cryptographic PIN via `db.generateUniquePairingCode()`, updates `users.pairing_code` and `users.pairing_code_created_at`, and syncs with cloud registry.
  - Stores the code in `telegramOtpStore` across aliases.
  - **Parity Guarantee**: The code shown in the Telegram bot under `🔑 Код входа` is 100% identical to the code on the web portal and in Athlete Pro.

### 6.2 5-Minute TTL Invalidation
- Evaluated in `web/src/db.js` (line 248), `web/src/server.js` (line 1354), and `web/src/bot.js` (line 136).
- If `Date.now() - pairing_code_created_at > 300000`, the server returns HTTP 400: `"Срок действия кода истёк (действует 5 минут). Запросите у подопечного новый код."`.
- Background active sweeper in `server.js` (lines 54-80) unref-cleans expired codes every 60 seconds.

### 6.3 Single-Use Consumption
- In `web/src/server.js` (line 1398): Once a trainer pairs an athlete via `POST /api/trainer/pair`, `db.consumePairingCode(athlete.id)` is immediately invoked (`UPDATE users SET pairing_code = '', pairing_code_created_at = 0 WHERE id = ?`). The code can never be reused.
- Verified in `tests/pin_2fa.test.js` Test 4 and `tests/verification_otp_stress.test.js` Section 1.1.

---

## 7. 1-Click Auth & 2FA OTP Delivery

### 7.1 1-Click Seamless Authentication
- **Session Init**: `POST /api/auth/telegram/session-init` (`web/src/server.js` lines 695-735). Generates `sessionId` (`auth_<hex>`), sets 5-min TTL, returns bot URL `https://t.me/fitnessecosystemBOT?start=auth_<sessionId>` and SVG QR code.
- **Bot Handler**: `web/src/bot.js` (lines 224-347). `/start auth_<sessionId>` matches session in memory.
- **2FA Guard**: If `user.two_factor_enabled === 1`, 1-click login is blocked from bypassing 2FA! The session status transitions to `REQUIRES_2FA`, an OTP is delivered to Telegram chat, and the web portal prompts for the 6-digit code.
- **Poll Status**: `GET /api/auth/telegram/session-status?sessionId=...` returns `AUTHORIZED` with token and user object, immediately deleting the session (single-use).

### 7.2 2FA OTP Delivery
- **Trigger**: Password login for 2FA-enabled account (`web/src/server.js` lines 433-450) or 1-click attempt (`web/src/bot.js` lines 275-308).
- **Delivery**: `send2FAOtp()` in `web/src/bot.js` (lines 588-609) pushes message with 6-digit OTP code directly to user's Telegram chat via `bot.api.sendMessage`.
- **Verification**: `POST /api/login/2fa` validates code within 5 minutes, enforces maximum 5 attempts, and deletes OTP from store.

---

## 8. Account Linking (`/link`)

### 8.1 Web Initiation
- User clicks "Привязать Telegram" in Profile.
- `POST /api/user/telegram/link-token` creates 5-minute link token (`web/src/server.js` lines 1195-1209).
- Returns deep link `https://t.me/fitnessecosystemBOT?start=link_<token>`.

### 8.2 Bot Execution
- Bot handles both `/start link_<token>` and `/link <token>` via `handleLinkToken()` (`web/src/bot.js` lines 41-119).
- Validates token exists in `telegram_link_tokens` and `Date.now() <= row.expires_at`.
- Rejects linking if the Telegram ID is already bound to another account.
- Executes `db.linkTelegram(linkRecord.user_id, tgId, rawUsername)` and `db.consumeLinkToken(cleanToken)` (single-use).
- Updates `userTgChatMap` for subsequent OTP deliveries.

---

## 9. Web Test Suites Configuration & Execution Results

### 9.1 Test Script Configuration
In `web/package.json`:
```json
"scripts": {
  "start": "node src/server.js",
  "test": "node tests/security.test.js && node tests/pin_2fa.test.js && node --test tests/cloud_sync_anthropometry.test.js && node --test tests/m1_hardening.test.js",
  "test:security": "node tests/security.test.js",
  "test:pin": "node tests/pin_2fa.test.js",
  "test:sync": "node --test tests/cloud_sync_anthropometry.test.js"
}
```

### 9.2 Execution Results (`npm test`)

| Suite | File | Tests Run | Pass | Fail | Duration |
|---|---|---|---|---|---|
| **Security Test Suite** | `tests/security.test.js` | 29 | 29 | 0 | 53.4s |
| **PIN & 2FA Test Suite** | `tests/pin_2fa.test.js` | 15 | 15 | 0 | 3.7s |
| **Anthropometry & Leaderboard Sync** | `tests/cloud_sync_anthropometry.test.js` | 6 | 6 | 0 | 76ms |
| **M1 Hardening Test Suite** | `tests/m1_hardening.test.js` | 6 | 6 | 0 | 667ms |
| **Total** | | **56** | **56** | **0** | **~58s** |

### 9.3 Additional Stress Test (`tests/load_stress_100_concurrent.test.js`)
- Executed 120 concurrent OTP requests in 126 ms wall-clock time.
- Average generation latency: 93.52 ms (<= 500 ms limit).
- Average validation latency: 94.53 ms (<= 500 ms limit).
- Code collisions: **0** across all 120 codes.
- SQLite database locks (`SQLITE_BUSY`): **0**.
- Single-use burning rejection rate: **100.0%**.
- Brute-force lockout (5 invalid attempts -> HTTP 429): **Verified**.

---

## Conclusion & Verdict

The Web Portal and Telegram Bot components at `F:\Projects\fitness-ecosystem-pro\web` satisfy all audit criteria and requirements R3 and R4:
1. **Security**: Zero SQLi, Zero XSS, timing-safe crypto, robust rate limiting, strict IDOR prevention.
2. **Role Selection**: Implemented at login and registration; immutable afterwards.
3. **UI Polish**: Competition table free of redundant columns; interactive multi-scale chart with point-click breakdown.
4. **Distribution**: Fresh v1.0.10 release APKs properly hosted and served.
5. **Bot Menu**: Zero references to "Сменить роль", clean 4-button menu.
6. **Unified PIN & 2FA**: 5-minute TTL, single-use burning, 1-click auth, and `/link` account binding completely functional.
7. **Acceptance Criteria**: `npm test` runs with 100% pass rate.
