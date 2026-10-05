# Survey Report: Web Backend & Telegram Bot Architecture Audit

**Author**: Web & Bot Survey Explorer (`explorer_survey_web_6`)  
**Target Codebase**: `F:\Projects\fitness-ecosystem-pro\web` & Mobile Integration  
**Date**: 2026-10-04  
**Audit Scope**: Requirement R1 (Stateless Session Persistence & Direct PIN Auth), Profile Synchronization, Baseline Test Analysis  

---

## 1. Executive Summary

This survey provides an end-to-end architectural investigation of the Web backend (`server.js`, `db.js`, `security.js`, `cloudSync.js`), Telegram bot (`bot.js`), and their mobile integration points (`AthleteRemoteAuthManager.kt`, `TrainerRemoteAuthManager.kt`).

### Key Survey Findings:
1. **Stateless HMAC-SHA256 Token Defect & Revocation Zombie Bug**:
   - `security.js` provides `generateSignedToken` and `verifySignedToken` using HMAC-SHA256.
   - However, multiple authentication endpoints (`/api/auth/telegram`, `/api/auth/telegram/verify-otp`, `/api/auth/telegram/complete-profile`, and bot 1-click `/start auth_<id>`) call `generateToken()` without arguments, emitting **random unsigned hex strings** (`crypto.randomBytes(32)`). These cannot be rehydrated upon server cold boot / container reset on Render.
   - In `db.js:734` (`getUserByToken`), the fallback blindly rehydrates *any* valid signed HMAC token that is missing from `auth_tokens`. When `/api/logout` deletes the token from `auth_tokens`, the subsequent request re-validates the HMAC signature and resurrects the token into `auth_tokens`. This caused `tests/security.test.js` to fail on logout revocation, which was masked by `process.exit(0)`.
2. **Direct PIN Verification Without Username Defect**:
   - `/api/auth/telegram/verify-otp` strictly enforces `cleanUsername` and `cleanCode` (`server.js:929-931`), returning HTTP 400 if `username` is omitted.
   - It only checks `telegramOtpStore.get(cleanUsername)`, ignoring direct matching of 6-digit codes against active OTP entries or athlete pairing PINs in SQLite.
3. **Profile Synchronization CamelCase Gap in `/api/me`**:
   - Mobile Android clients (`AthleteRemoteAuthManager.kt`) look for camelCase JSON fields (`pairingCode`, `fullName`, `avatarBase64`, `clientUuid`, `restrictions`).
   - `GET /api/me` returns `...freshUser` straight from SQLite, which provides only snake_case fields (`pairing_code`, `full_name`, `avatar_base64`, `client_uuid`).
   - Consequently, `fetchCurrentProfile()` in mobile apps receives empty strings for `pairingCode` and `fullName`, breaking automatic PIN and profile parity between Web and Mobile.
4. **Baseline Test Suite Status**:
   - `npm test` runs 5 test suites. 4 suites pass 100% (`pin_2fa.test.js`: 15/15 pass; `cloud_sync_anthropometry.test.js`: 6/6 pass; `m1_hardening.test.js`: 5/5 pass; `cloud_sync_profile_pin.test.js`: 4/4 pass).
   - `tests/security.test.js` has **1 failing test** (`revokes session token on logout with 200 and blocks subsequent calls`) caused by the HMAC re-hydration zombie bug.

---

## 2. Topic 1: Stateless Session Persistence & Cold Boot Rehydration

### 2.1 Current Implementation
- **Source Files**:
  - `web/src/security.js`: Lines 60–97
  - `web/src/db.js`: Lines 710–753
  - `web/src/server.js`: Lines 231, 332, 489, 572, 695, 995, 1140
  - `web/src/bot.js`: Line 413

```javascript
// security.js lines 60-90
const TOKEN_SECRET = process.env.SESSION_SECRET || 'fitness_ecosystem_pro_super_secret_signing_key_2026';

function generateSignedToken(userId, role = 'athlete', ttlMs = 86400000 * 365) {
  const expiresAt = Date.now() + ttlMs;
  const rawPayload = `${userId}:${role}:${expiresAt}:${crypto.randomBytes(8).toString('hex')}`;
  const payloadB64 = Buffer.from(rawPayload).toString('base64url');
  const signature = crypto.createHmac('sha256', TOKEN_SECRET).update(payloadB64).digest('hex').slice(0, 32);
  return `fit_${payloadB64}_${signature}`;
}

function verifySignedToken(token) {
  if (!token || typeof token !== 'string' || !token.startsWith('fit_')) return null;
  const parts = token.split('_');
  if (parts.length < 3) return null;
  const payloadB64 = parts[1];
  const signature = parts.slice(2).join('_');
  const expectedSig = crypto.createHmac('sha256', TOKEN_SECRET).update(payloadB64).digest('hex').slice(0, 32);
  if (signature !== expectedSig) return null;
  try {
    const rawPayload = Buffer.from(payloadB64, 'base64url').toString('utf8');
    const [userIdStr, role, expiresAtStr] = rawPayload.split(':');
    const userId = parseInt(userIdStr, 10);
    const expiresAt = parseInt(expiresAtStr, 10);
    if (!userId || !expiresAt || Date.now() > expiresAt) return null;
    return { userId, role, expiresAt };
  } catch (_) {
    return null;
  }
}
```

```javascript
// db.js lines 721-746
  getUserByToken(token) {
    if (!token || typeof token !== 'string') return null;
    const now = Date.now();
    const stmt = this.db.prepare(`
      SELECT u.id, u.username, u.role, u.full_name, u.phone, u.avatar_base64, u.client_uuid, u.coach_name, u.coach_phone, u.pairing_code, u.pairing_code_created_at, u.is_private, u.telegram_id, u.telegram_username, u.two_factor_enabled, u.restrictions
      FROM auth_tokens t
      JOIN users u ON t.user_id = u.id
      WHERE t.token = ? AND t.expires_at > ?
    `);
    const found = stmt.get(token, now);
    if (found) return found;

    // Fallback: Verify stateless signed HMAC token if db restarted or token was evacuated
    const payload = verifySignedToken(token);
    if (payload && payload.userId && payload.expiresAt > now) {
      const user = this.findUserById(payload.userId);
      if (user) {
        try {
          this.createAuthToken(token, user.id, payload.expiresAt - now);
        } catch (_) {}
        return user;
      }
    }

    return null;
  }
```

### 2.2 Defects & Gaps Identified
1. **Unsigned Token Call Sites**:
   The function `generateToken(userId = 0, role = 'athlete')` requires `userId` to produce a signed token. Five critical auth endpoints omit the arguments:
   - `server.js:231`: `const token = generateToken();` (Admin/Owner quick overwrite)
   - `server.js:695`: `const token = generateToken();` (Telegram login widget)
   - `server.js:995`: `const token = generateToken();` (`/api/auth/telegram/verify-otp`)
   - `server.js:1140`: `const token = generateToken();` (`/api/auth/telegram/complete-profile`)
   - `bot.js:413`: `const token = generateToken();` (Bot 1-click `/start auth_<sessionId>`)
   *Impact*: These emit 64-char random hex strings. After Render cold boots, these sessions are permanently invalidated.
2. **Missing Revocation Blacklist / Zombie Token Bug**:
   - When a user logs out (`/api/logout`), `db.deleteAuthToken(token)` deletes the record from `auth_tokens`.
   - On the next API call, `db.getUserByToken` verifies the HMAC signature of `token`, finds the user, re-inserts it into `auth_tokens`, and returns HTTP 200.
   - *Impact*: Logout is ineffective for signed tokens; `tests/security.test.js` fails.

### 2.3 Proposed Fixes & Database Schema Changes
1. **Add `revoked_tokens` table** in `db.js`:
   ```sql
   CREATE TABLE IF NOT EXISTS revoked_tokens (
     token TEXT PRIMARY KEY,
     revoked_at INTEGER NOT NULL
   );
   CREATE INDEX IF NOT EXISTS idx_revoked_tokens ON revoked_tokens(token);
   ```
2. **Update `deleteAuthToken(token)`**:
   ```javascript
   deleteAuthToken(token) {
     if (!token) return;
     try {
       this.db.prepare('DELETE FROM auth_tokens WHERE token = ?').run(token);
       this.db.prepare('INSERT OR REPLACE INTO revoked_tokens (token, revoked_at) VALUES (?, ?)').run(token, Date.now());
     } catch (_) {}
   }
   ```
3. **Update `getUserByToken(token)`**:
   ```javascript
   getUserByToken(token) {
     if (!token || typeof token !== 'string') return null;
     const isRevoked = this.db.prepare('SELECT 1 FROM revoked_tokens WHERE token = ?').get(token);
     if (isRevoked) return null;
     // ... proceed to auth_tokens check and signed token fallback ...
   ```
4. **Pass `userId` and `role` to `generateToken`** across all endpoints:
   - `server.js:231`: `const token = generateToken(existing.id, existing.role);`
   - `server.js:695`: `const token = generateToken(user.id, user.role);`
   - `server.js:995`: `const token = generateToken(user.id, user.role);`
   - `server.js:1140`: `const token = generateToken(user.id, user.role);`
   - `bot.js:413`: `const token = generateToken(user.id, user.role);`

---

## 3. Topic 2: Direct PIN Verification Endpoint (`/api/auth/telegram/verify-otp`)

### 3.1 Current Implementation
- **Source File**: `web/src/server.js`, Lines 922–1030

```javascript
      // TELEGRAM OTP: 2. Verify 6-digit Code
      if (pathname === '/api/auth/telegram/verify-otp' && req.method === 'POST') {
        const body = await parseJsonBody(req);
        const { username, code } = body;
        const cleanUsername = String(username || '').replace(/^@/, '').trim().toLowerCase();
        const cleanCode = String(code || '').trim();

        if (!cleanUsername || !cleanCode) {
          return sendError(res, 400, 'Введите имя пользователя и 6-значный код');
        }

        const record = telegramOtpStore.get(cleanUsername);
        if (!record) {
          return sendError(res, 400, 'Код не запрашивался или срок действия (5 минут) истек');
        }
        // ...
```

### 3.2 Gaps Against Requirement R1
1. **Requires `cleanUsername`**:
   Requests with `{ code: "123456" }` alone immediately hit `if (!cleanUsername || !cleanCode)` and return HTTP 400.
2. **Missing Search Across Active OTPs**:
   `telegramOtpStore` contains entries created by bot (`bot.js:195`) and `/request-otp`. The entries store `{ code, expiresAt, tgId, username, attempts, userId }`. A standalone code should search across all unexpired entries in `telegramOtpStore`.
3. **Missing Match Against Athlete Pairing PINs**:
   In `bot.js:126`, an athlete's 6-digit PIN in the database (`users.pairing_code`) is the primary login PIN. A standalone 6-digit code must be checked against `users.pairing_code` with `pairing_code_created_at` within 5 minutes (`300000 ms`).

### 3.3 Proposed Endpoint Logic
```javascript
      if (pathname === '/api/auth/telegram/verify-otp' && req.method === 'POST') {
        const body = await parseJsonBody(req);
        const { username, code } = body;
        const cleanUsername = String(username || '').replace(/^@/, '').trim().toLowerCase();
        const cleanCode = String(code || '').replace(/\D/g, '').trim();

        if (!cleanCode || cleanCode.length !== 6) {
          return sendError(res, 400, 'Введите корректный 6-значный код');
        }

        const now = Date.now();
        let matchedUser = null;
        let matchedOtpRecord = null;
        let matchedOtpKey = null;

        // Path A: Username provided -> check direct OTP entry
        if (cleanUsername) {
          const record = telegramOtpStore.get(cleanUsername) || telegramOtpStore.get(`id_${cleanUsername}`);
          if (record && record.expiresAt > now && record.code === cleanCode) {
            matchedOtpRecord = record;
            matchedOtpKey = cleanUsername;
          }
        }

        // Path B: Direct PIN lookup (no username or username not found in store)
        if (!matchedOtpRecord) {
          // 1. Check telegramOtpStore for any unexpired matching code
          for (const [key, entry] of telegramOtpStore.entries()) {
            if (entry && entry.code === cleanCode && entry.expiresAt > now) {
              if (entry.attempts >= 5) continue;
              matchedOtpRecord = entry;
              matchedOtpKey = key;
              break;
            }
          }
        }

        // 2. Resolve user from OTP record if matched
        if (matchedOtpRecord) {
          telegramOtpStore.delete(matchedOtpKey);
          if (matchedOtpRecord.userId) {
            matchedUser = db.findUserById(matchedOtpRecord.userId);
          }
          if (!matchedUser && matchedOtpRecord.tgId) {
            matchedUser = db.findUserByTelegramId(String(matchedOtpRecord.tgId));
          }
          if (!matchedUser && matchedOtpRecord.username) {
            matchedUser = db.findUserByTelegramUsername(matchedOtpRecord.username) ||
                          db.findUserByUsername(matchedOtpRecord.username) ||
                          db.findUserByUsername(`tg_${matchedOtpRecord.username}`);
          }
        }

        // Path C: Match against active athlete pairing PIN in SQLite users
        if (!matchedUser) {
          const PAIRING_TTL = 5 * 60 * 1000;
          const candidate = db.findUserByPairingCode(cleanCode);
          if (candidate && candidate.pairing_code_created_at && (now - candidate.pairing_code_created_at < PAIRING_TTL)) {
            matchedUser = candidate;
          }
        }

        if (!matchedUser && !matchedOtpRecord) {
          return sendError(res, 400, 'Неверный или истекший 6-значный код');
        }

        // If OTP matched for a brand-new user without account in DB yet
        if (!matchedUser && matchedOtpRecord) {
          return sendJson(res, 200, {
            success: true,
            isNewUser: true,
            telegramUsername: matchedOtpRecord.username || ''
          });
        }

        // Generate signed HMAC token for authenticated user
        const token = generateToken(matchedUser.id, matchedUser.role);
        db.createAuthToken(token, matchedUser.id);

        return sendJson(res, 200, {
          success: true,
          isNewUser: false,
          token,
          user: {
            id: matchedUser.id,
            username: matchedUser.username,
            role: matchedUser.role,
            fullName: matchedUser.full_name,
            phone: matchedUser.phone,
            avatarBase64: matchedUser.avatar_base64 || '',
            pairingCode: matchedUser.pairing_code,
            clientUuid: matchedUser.client_uuid || '',
            restrictions: matchedUser.restrictions || '',
            coachName: matchedUser.coach_name || '',
            coachPhone: matchedUser.coach_phone || '',
            isPrivate: Boolean(matchedUser.is_private),
            telegramId: String(matchedUser.telegram_id || ''),
            telegramUsername: String(matchedUser.telegram_username || ''),
            twoFactorEnabled: Boolean(matchedUser.two_factor_enabled)
          }
        });
      }
```

---

## 4. Topic 3: Profile Data Synchronization (PIN, Photo, Full Name, Phone, Restrictions)

### 4.1 Field Contract Across Surfaces

| Field | SQLite Column (`users`) | Web JSON API | Mobile Kotlin Model (`AthleteRemoteUserInfo`) | Bot (`bot.js`) | CloudSync (`cloud.clients`) |
|---|---|---|---|---|---|
| **Pairing PIN** | `pairing_code` (6-digit) | `pairingCode` / `pairing_code` | `pairingCode: String` | `code: String` via `getUnifiedUserCode` | `pin` (AES-256) |
| **Photo / Avatar** | `avatar_base64` (< 30 KB) | `avatarBase64` / `avatar_base64` | `avatarBase64: String` | `avatarBase64` | `avatarBase64` |
| **Full Name** | `full_name` (min 2 chars) | `fullName` / `full_name` | `fullName: String` | `fullName` | `name` |
| **Phone** | `phone` | `phone` | `phone: String` | `phone` | `phone` |
| **Restrictions** | `restrictions` (TEXT) | `restrictions` | `restrictions: String` | `restrictions` | `restrictions` |

### 4.2 Endpoints Used by Mobile Apps and Bot
1. **Authentication & Session**:
   - `POST /api/login`: Standard username/password login.
   - `POST /api/login/2fa`: 2FA OTP verification.
   - `POST /api/auth/telegram/session-init`: Generates `auth_<sessionId>` and QR/link for 1-click bot login.
   - `GET /api/auth/telegram/session-status?sessionId=...`: Polls session authorization state.
   - `POST /api/auth/telegram/request-otp`: Sends 6-digit code to Telegram bot.
   - `POST /api/auth/telegram/verify-otp`: Verifies 6-digit OTP / PIN.
2. **Profile & Synchronization**:
   - `GET /api/me`: Returns current user info and paired trainer info.
   - `POST /api/profile` (or `PUT /api/user/profile`): Updates `fullName`, `phone`, `avatarBase64`, `restrictions`, `clientUuid`.
   - `POST /api/athlete/regenerate-pin`: Regenerates 5-minute athlete pairing PIN.
   - `POST /api/user/telegram/link-token`: Creates 5-minute deep link token for Telegram bot account linking.
   - `POST /api/user/telegram/link-by-bot-code`: Binds account via 6-digit OTP from bot.
   - `GET /api/trainer/athlete-restrictions`: Trainer reads athlete restrictions.
   - `POST /api/trainer/athlete-restrictions`: Trainer updates athlete restrictions.

### 4.3 The `/api/me` Serialization Defect & Fix
In `server.js:1270`:
```javascript
// BEFORE (Buggy):
const normalizedUser = {
  ...freshUser,
  telegram_id: String(freshUser.telegram_id || ''),
  telegramId: String(freshUser.telegram_id || ''),
  telegram_username: String(freshUser.telegram_username || ''),
  telegramUsername: String(freshUser.telegram_username || ''),
  two_factor_enabled: Number(freshUser.two_factor_enabled) || 0,
  twoFactorEnabled: Boolean(freshUser.two_factor_enabled)
};
```
Because `freshUser` contains SQLite row properties (`full_name`, `pairing_code`, `avatar_base64`, `client_uuid`), it lacks camelCase properties expected by Kotlin.

```javascript
// AFTER (Fixed Contract Parity):
const normalizedUser = {
  ...freshUser,
  fullName: freshUser.full_name,
  pairingCode: freshUser.pairing_code,
  avatarBase64: freshUser.avatar_base64 || '',
  clientUuid: freshUser.client_uuid || '',
  restrictions: freshUser.restrictions || '',
  coachName: freshUser.coach_name || '',
  coachPhone: freshUser.coach_phone || '',
  telegram_id: String(freshUser.telegram_id || ''),
  telegramId: String(freshUser.telegram_id || ''),
  telegram_username: String(freshUser.telegram_username || ''),
  telegramUsername: String(freshUser.telegram_username || ''),
  two_factor_enabled: Number(freshUser.two_factor_enabled) || 0,
  twoFactorEnabled: Boolean(freshUser.two_factor_enabled)
};
```

---

## 5. Topic 4: Baseline Test Suite Audit

### 5.1 Command & Execution
```powershell
npm test
# Equivalent to:
node tests/security.test.js && node tests/pin_2fa.test.js && node --test tests/cloud_sync_anthropometry.test.js && node --test tests/m1_hardening.test.js && node --test tests/cloud_sync_profile_pin.test.js
```

### 5.2 Results Matrix

| Test Suite File | Test Count | Pass | Fail | Duration | Status | Notes |
|---|---|---|---|---|---|---|
| `tests/security.test.js` | 43 | 42 | **1** | ~58.2s | **FAIL** (Masked) | Fails on token logout revocation test; masked by `process.exit(0)` |
| `tests/pin_2fa.test.js` | 15 | 15 | 0 | ~8.3s | **PASS** | 100% pass across 5-min TTL, 2FA, deep linking, 1-click bot login |
| `tests/cloud_sync_anthropometry.test.js` | 6 | 6 | 0 | ~7ms | **PASS** | 100% pass for anthropometry sync and leaderboard formula |
| `tests/m1_hardening.test.js` | 5 | 5 | 0 | ~165ms | **PASS** | 100% pass for secure PIN generation, rate limiter cleanup, bot handlers |
| `tests/cloud_sync_profile_pin.test.js` | 4 | 4 | 0 | ~2.6s | **PASS** | 100% pass for cloud sync, live AES-256 decryption, PIN parity |

### 5.3 Detailed Breakdown of the Single Failing Test
- **Location**: `tests/security.test.js`, lines 239–257
- **Test Title**: `revokes session token on logout with 200 and blocks subsequent calls`
- **Error**: Expected HTTP 401 on `GET /api/me` after `POST /api/logout`, but received HTTP 200.
- **Root Cause**: `getUserByToken` rehydrated the deleted HMAC-signed token from memory/signature without checking if it was revoked.
- **Test Runner Defect**: Line 104 has `setTimeout(() => process.exit(0), 100);` which unconditionally exited 0, hiding this test failure in CI/scripts.

---

## 6. Concrete File Modification Roadmap for Implementation

1. **`web/src/db.js`**:
   - Create `revoked_tokens` table in `initTables()`.
   - Update `deleteAuthToken(token)` to record `token` in `revoked_tokens`.
   - Update `getUserByToken(token)` to check `revoked_tokens` before querying `auth_tokens` and before attempting HMAC signature fallback.
   - Add `findUserByPairingCode(pin)` helper if not already present.
2. **`web/src/server.js`**:
   - Update `/api/auth/telegram/verify-otp` to accept `{ code: "123456" }` without `username`, search `telegramOtpStore` and `users.pairing_code`, and issue signed HMAC tokens.
   - Pass `user.id, user.role` to all `generateToken` calls (`lines 231, 695, 995, 1140`).
   - In `GET /api/me`, include `fullName`, `pairingCode`, `avatarBase64`, `clientUuid`, `restrictions` in `normalizedUser`.
3. **`web/src/bot.js`**:
   - Line 413: Pass `user.id, user.role` to `generateToken(user.id, user.role)` for 1-click authorization.
4. **`web/tests/security.test.js`**:
   - Line 104: Remove unconditional `process.exit(0)` to allow proper test runner exit code propagation.
5. **Add Targeted Verification Tests**:
   - Direct PIN verification test (`{ code: "123456" }` without username).
   - Cold boot simulation test (verifying signed token rehydration after wiping `auth_tokens` table while preserving users).
   - Logout revocation verification (ensuring revoked signed tokens return 401).
