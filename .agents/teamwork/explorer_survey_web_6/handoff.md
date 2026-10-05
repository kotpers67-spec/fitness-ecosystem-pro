# Handoff Report: Web Backend & Telegram Bot Survey

**Agent**: `explorer_survey_web_6`  
**Working Directory**: `F:\Projects\fitness-ecosystem-pro\.agents\teamwork\explorer_survey_web_6`  
**Handoff Type**: Hard (Task Complete)  
**Timestamp**: 2026-10-04T20:38:00Z  

---

## 1. Observation

1. **Test Execution Command & Failure**:
   - Command: `npm test` in `F:\Projects\fitness-ecosystem-pro\web`
   - Output from `tests/security.test.js`:
     ```text
     ▶ 1. Authentication & Token Security
       ...
       ✔ allows valid login and returns session token (2620.2895ms)
       ✔ rejects wrong password with 401 (31.9987ms)
       ✖ revokes session token on logout with 200 and blocks subsequent calls (3769.9615ms)
     ✖ 1. Authentication & Token Security (6667.5037ms)
     ...
     ✖ Fitness Ecosystem Pro - Security Test Suite (58199.6533ms)
     ```
   - Masked exit code: `tests/security.test.js`, lines 99–105:
     ```javascript
     after(async () => {
       await new Promise(res => server.close(res));
       if (db && typeof db.close === 'function') {
         db.close();
       }
       setTimeout(() => process.exit(0), 100);
     });
     ```
   - Test results for other test suites:
     - `node tests/pin_2fa.test.js`: 15 passed, 0 failed (8.28s).
     - `node --test tests/cloud_sync_anthropometry.test.js`: 6 passed, 0 failed (7.2ms).
     - `node --test tests/m1_hardening.test.js`: 5 passed, 0 failed (164.8ms).
     - `node --test tests/cloud_sync_profile_pin.test.js`: 4 passed, 0 failed (2.63s).

2. **Unsigned Token Call Sites**:
   - In `web/src/security.js:92-97`:
     ```javascript
     function generateToken(userId = 0, role = 'athlete') {
       if (userId) {
         return generateSignedToken(userId, role);
       }
       return crypto.randomBytes(32).toString('hex');
     }
     ```
   - Multiple endpoints call `generateToken()` without arguments:
     - `web/src/server.js:231`: `const token = generateToken();`
     - `web/src/server.js:695`: `const token = generateToken();`
     - `web/src/server.js:995`: `const token = generateToken();`
     - `web/src/server.js:1140`: `const token = generateToken();`
     - `web/src/bot.js:413`: `const token = generateToken();`

3. **Rehydration Zombie Bug on Logout**:
   - In `web/src/server.js:1348`:
     `if (token) db.deleteAuthToken(token);`
   - In `web/src/db.js:748-753`:
     ```javascript
     deleteAuthToken(token) {
       const stmt = this.db.prepare(`
         DELETE FROM auth_tokens WHERE token = ?
       `);
       stmt.run(token);
     }
     ```
   - In `web/src/db.js:733-743`:
     ```javascript
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
     ```

4. **Direct PIN Verification Endpoint Requirements**:
   - In `web/src/server.js:923-936`:
     ```javascript
     if (pathname === '/api/auth/telegram/verify-otp' && req.method === 'POST') {
       const body = await parseJsonBody(req);
       const { username, code } = body;
       const cleanUsername = String(username || '').replace(/^@/, '').trim().toLowerCase();
       const cleanCode = String(code || '').trim();

       if (!cleanUsername || !cleanCode) {
         return sendError(res, 400, 'Введите имя пользователя и 6-значный код');
       }
     ```

5. **Profile Serialization Contract Mismatch in `/api/me`**:
   - In `athlete-app/app/src/main/java/com/athleteapp/pro/data/auth/AthleteRemoteAuthManager.kt:103-112`:
     ```kotlin
     fullName = if (userObj.has("fullName") && !userObj.get("fullName").isJsonNull) userObj.get("fullName").asString else "",
     phone = if (userObj.has("phone") && !userObj.get("phone").isJsonNull) userObj.get("phone").asString else "",
     pairingCode = if (userObj.has("pairingCode") && !userObj.get("pairingCode").isJsonNull) userObj.get("pairingCode").asString else "",
     avatarBase64 = if (userObj.has("avatarBase64") && !userObj.get("avatarBase64").isJsonNull) userObj.get("avatarBase64").asString
         else if (userObj.has("avatar_base64") && !userObj.get("avatar_base64").isJsonNull) userObj.get("avatar_base64").asString else "",
     clientUuid = if (userObj.has("clientUuid") && !userObj.get("clientUuid").isJsonNull) userObj.get("clientUuid").asString
         else if (userObj.has("client_uuid") && !userObj.get("client_uuid").isJsonNull) userObj.get("client_uuid").asString else "",
     ```
   - In `web/src/server.js:1270-1279`:
     ```javascript
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
     `freshUser` provides `full_name`, `pairing_code`, `client_uuid` from SQLite; camelCase aliases are missing.

---

## 2. Logic Chain

1. **Logic Step 1 (Stateless Rehydration vs Revocation)**:
   - *From Observation 3*: `getUserByToken` verifies the HMAC signature of any unrecorded token. If valid, it recreates the token row in `auth_tokens` and returns the user.
   - *From Observation 3*: `deleteAuthToken` only deletes from `auth_tokens`.
   - *Inference*: Any signed token that is deleted upon logout will immediately be rehydrated and accepted by `getUserByToken`.
   - *From Observation 1*: This is the exact cause of test failure `revokes session token on logout with 200 and blocks subsequent calls`.
   - *Conclusion*: A `revoked_tokens` table is mandatory so that `getUserByToken` refuses to rehydrate revoked tokens.

2. **Logic Step 2 (Unsigned Token Vulnerability across Restarts)**:
   - *From Observation 2*: When `generateToken()` is called without `userId`, it produces an unsigned random hex string.
   - *From Observation 2*: Lines `server.js:695, 995, 1140` and `bot.js:413` generate tokens without `userId`.
   - *Inference*: Any user authenticating via Telegram widget, Telegram OTP, profile completion, or bot 1-click receives an unsigned token.
   - *Conclusion*: On Render server restart or database re-initialization, these tokens cannot be rehydrated statelessly. Passing `user.id, user.role` to all `generateToken` calls is required.

3. **Logic Step 3 (Direct PIN Verification)**:
   - *From Observation 4*: `/api/auth/telegram/verify-otp` returns 400 when `username` is empty.
   - *Inference*: Mobile app and web requests sending `{ code: "123456" }` alone will fail.
   - *Conclusion*: The endpoint must be updated to accept code alone, search `telegramOtpStore` across unexpired entries, match active athlete `pairing_code` in SQLite `users`, and emit signed HMAC tokens.

4. **Logic Step 4 (Mobile-Web Profile Sync Parity)**:
   - *From Observation 5*: `AthleteRemoteAuthManager.kt` checks `userObj.has("pairingCode")` and `userObj.has("fullName")`.
   - *From Observation 5*: `server.js:1270` only outputs snake_case from SQLite.
   - *Inference*: `AthleteViewModel` receives empty `remoteProfile.pairingCode`, preventing mobile app from synchronizing the backend PIN.
   - *Conclusion*: `server.js:1270` must include `fullName`, `pairingCode`, `avatarBase64`, `clientUuid`, and `restrictions` in `normalizedUser`.

---

## 3. Caveats

1. **Android App Source Modification**: Android Kotlin files were inspected for contract parity and API expectations; no Android code was modified (read-only investigation).
2. **Render Environment Specifics**: On Render Free tier, filesystem persistence is ephemeral. True stateless rehydration across cold boots relies on deterministic `SESSION_SECRET` and user records in SQLite/CloudSync.
3. **Admin / Pending Trainer Approval**: Trainers requiring admin approval (`is_approved = 0`) must not receive active login tokens until approved.

---

## 4. Conclusion

The Web backend requires targeted modifications across 3 files (`web/src/db.js`, `web/src/server.js`, `web/src/bot.js`) and 1 test file (`web/tests/security.test.js`):
1. Add `revoked_tokens` table and check in `db.js:getUserByToken` to eliminate the rehydration zombie bug and pass `security.test.js`.
2. Update `/api/auth/telegram/verify-otp` in `server.js` to accept `{ code: "123456" }` alone, matching `telegramOtpStore` and active user PINs.
3. Pass `userId, role` to all `generateToken()` calls so 100% of issued tokens are signed HMAC tokens.
4. Add camelCase aliases (`pairingCode`, `fullName`, `avatarBase64`, `clientUuid`, `restrictions`) in `server.js:1270` (`/api/me`) to achieve profile sync parity with Android apps.

---

## 5. Verification Method

1. **Baseline Test Suite Command**:
   ```powershell
   cd F:\Projects\fitness-ecosystem-pro\web
   npm test
   ```
2. **Individual Suite Verification**:
   ```powershell
   node tests/security.test.js
   node tests/pin_2fa.test.js
   node --test tests/cloud_sync_anthropometry.test.js
   node --test tests/m1_hardening.test.js
   node --test tests/cloud_sync_profile_pin.test.js
   ```
3. **Invalidation Conditions**:
   - Any test failure in `tests/security.test.js` (especially logout token revocation).
   - Rejection of POST `/api/auth/telegram/verify-otp` with `{ code: "123456" }`.
   - Inability of signed token to survive simulated deletion from `auth_tokens` (rehydration check).
