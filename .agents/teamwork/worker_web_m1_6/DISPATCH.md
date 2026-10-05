# DISPATCH — worker_web_m1_6

You are the Web & Bot Backend Implementation Worker.
Your working directory is: F:\Projects\fitness-ecosystem-pro\.agents\teamwork\worker_web_m1_6
Original request path: F:\Projects\fitness-ecosystem-pro\.agents\teamwork\ORIGINAL_REQUEST.md (timestamp 2026-10-04T20:25:27Z)
Survey findings: F:\Projects\fitness-ecosystem-pro\.agents\teamwork\explorer_survey_web_6\survey_report.md
Project plan: F:\Projects\fitness-ecosystem-pro\.agents\teamwork\orchestrator_6\PROJECT.md

Exclusive Write Ownership:
`web/src/db.js`, `web/src/server.js`, `web/src/bot.js`, `web/tests/security.test.js`, and any new unit test files in `web/tests/`.
You MUST NOT edit any files in `athlete-app` or `trainer-app`.

Tasks:
1. Stateless Session Persistence & Cold Boot Rehydration (R1):
   - In `web/src/db.js`:
     * Create `revoked_tokens (token TEXT PRIMARY KEY, revoked_at INTEGER NOT NULL)` table and index if not exists.
     * In `deleteAuthToken(token)`: delete from `auth_tokens` AND insert into `revoked_tokens`.
     * In `getUserByToken(token)`: check `revoked_tokens` first — if found, return null immediately.
     * Ensure HMAC-SHA256 fallback rehydrates valid unrevoked signed tokens into `auth_tokens`.
   - In `web/src/server.js` and `web/src/bot.js`:
     * Pass `userId` and `role` to `generateToken(userId, role)` at all call sites:
       - `server.js:231`: `generateToken(existing.id, existing.role)`
       - `server.js:695`: `generateToken(user.id, user.role)`
       - `server.js:995`: `generateToken(user.id, user.role)`
       - `server.js:1140`: `generateToken(user.id, user.role)`
       - `bot.js:413`: `generateToken(user.id, user.role)`
2. Direct PIN Verification Endpoint (R1):
   - In `web/src/server.js` (`/api/auth/telegram/verify-otp`):
     * Accept `{ code: "123456" }` without requiring `username`.
     * Clean and validate `cleanCode` (6 digits).
     * If username provided, check direct OTP entry.
     * If username NOT provided or not matched, search unexpired entries in `telegramOtpStore`.
     * If not matched in `telegramOtpStore`, match against athlete pairing PIN in SQLite (`db.findUserByPairingCode(cleanCode)`) if `pairing_code_created_at` is within 5 minutes (300,000 ms).
     * Return authenticated user with signed HMAC token and user profile fields.
3. Profile Synchronization Parity (R1, R2):
   - In `web/src/server.js` (`GET /api/me`):
     * Return both snake_case and camelCase fields: `pairingCode`, `fullName`, `avatarBase64`, `clientUuid`, `restrictions`, `twoFactorEnabled`, etc., so Android apps can deserialize seamlessly.
4. Test Suite Verification:
   - In `web/tests/security.test.js`:
     * Verify logout revocation test passes cleanly (with the revocation blacklist in `db.js`, revoked tokens will not rehydrate).
     * Remove the legacy `process.exit(0)` masking line if present.
   - Run `npm test` in `web/` and verify 100% pass across all test suites with 0 failures.

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

Output:
Write changes report to `F:\Projects\fitness-ecosystem-pro\.agents\teamwork\worker_web_m1_6\changes.md` and handoff report to `handoff.md`.
Send message to parent when complete.
