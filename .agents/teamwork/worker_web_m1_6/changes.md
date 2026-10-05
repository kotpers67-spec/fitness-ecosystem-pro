# Changes Report — worker_web_m1_6

## Overview
Implemented stateless session persistence & cold boot rehydration, direct PIN verification endpoint, and profile synchronization parity for Web backend & Telegram bot in Fitness Ecosystem Pro.

---

## 1. `web/src/db.js`
- **Revocation Tracking Table**:
  - Added `CREATE TABLE IF NOT EXISTS revoked_tokens (token TEXT PRIMARY KEY, revoked_at INTEGER NOT NULL)` and index `idx_revoked_tokens`.
- **Token Invalidation**:
  - In `deleteAuthToken(token)`: Deletes from `auth_tokens` and inserts into `revoked_tokens` with timestamp `Date.now()`.
- **Cold Boot Rehydration & Revocation Check**:
  - In `getUserByToken(token)`: Immediately checks `revoked_tokens`; if found, returns `null` so revoked tokens can never rehydrate.
  - If token not found in `auth_tokens`, falls back to HMAC-SHA256 signature verification via `verifySignedToken(token)`. Valid unexpired tokens are automatically re-inserted into `auth_tokens` and returned.
- **Pairing Code User Lookup**:
  - Updated `findUserByPairingCode(code)` to query all essential user columns (`restrictions`, `two_factor_enabled`, `telegram_id`, etc.) for seamless deserialization.

---

## 2. `web/src/server.js`
- **Stateless HMAC-SHA256 Token Generation**:
  - Updated call sites to supply `userId` and `role` to `generateToken(userId, role)` so signed tokens (`fit_<payload>_<signature>`) are issued consistently:
    - Line 231: `generateToken(existing.id, existing.role)` (Admin/Owner quick overwrite)
    - Line 695: `generateToken(user.id, user.role)` (Telegram login widget)
    - Line 1024: `generateToken(user.id, user.role)` (`/api/auth/telegram/verify-otp`)
    - Line 1194: `generateToken(user.id, user.role)` (`/api/auth/telegram/complete-profile`)
- **Direct PIN Verification Endpoint (`/api/auth/telegram/verify-otp`)**:
  - Permits `{ code: "123456" }` without requiring `username`.
  - Normalizes and validates 6-digit digits.
  - If `username` provided, checks direct OTP entry and enforces 5-attempt rate-limit / lockout.
  - If `username` not provided or not matched, searches unexpired active entries in `telegramOtpStore`.
  - If not matched in `telegramOtpStore`, queries active athlete pairing PIN in SQLite via `db.findUserByPairingCode(cleanCode)` within 5-minute TTL (300,000 ms).
  - Returns authenticated user with signed HMAC token, full user profile, and single-use consumption.
- **Profile Synchronization Parity (`GET /api/me`)**:
  - Added camelCase property aliases to `normalizedUser` response: `fullName`, `pairingCode`, `avatarBase64`, `clientUuid`, `restrictions`, `coachName`, `coachPhone`, `twoFactorEnabled`, `telegramId`, `telegramUsername` alongside snake_case properties to ensure 100% Android app deserialization compatibility.

---

## 3. `web/src/bot.js`
- **Signed Token on 1-Click Telegram Login**:
  - Line 413: Updated `generateToken()` to `generateToken(user.id, user.role)` so tokens issued through Telegram 1-click login are HMAC-SHA256 signed and persistent across cold boots.

---

## 4. `web/tests/security.test.js`
- **Removed Process Exit Masking**:
  - Removed `setTimeout(() => process.exit(0), 100)` in `after()` hook so real test runner exit codes propagate accurately.
- **Verification Tests Added**:
  - Cold boot rehydration test: Clears `auth_tokens` table in SQLite, verifies valid HMAC token rehydrates and returns HTTP 200 with full user data.
  - Direct PIN verification test: Verifies `{ code: pairingCode }` alone logs in user with HTTP 200 and signed token without requiring username.
  - Verified logout revocation test passes cleanly with HTTP 401 on subsequent requests.

---

## Verification Results
- Ran `npm test` in `web/`:
  - `tests/security.test.js`: 57/57 tests PASS (0 fail)
  - `tests/pin_2fa.test.js`: 15/15 tests PASS (0 fail)
  - `tests/cloud_sync_anthropometry.test.js`: 6/6 tests PASS (0 fail)
  - `tests/m1_hardening.test.js`: 6/6 tests PASS (0 fail)
  - `tests/cloud_sync_profile_pin.test.js`: 5/5 tests PASS (0 fail)
  - Overall: 89/89 tests passed (100% pass rate, 0 failures).
