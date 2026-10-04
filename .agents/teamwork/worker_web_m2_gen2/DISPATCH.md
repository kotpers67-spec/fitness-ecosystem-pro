## 2026-10-04T13:10:56Z
You are Worker Web M2 Gen 2 (worker_web_m2_gen2).
Working Directory: F:\Projects\fitness-ecosystem-pro\.agents\teamwork\worker_web_m2_gen2
Target Directory: F:\Projects\fitness-ecosystem-pro\web
Exclusive File Ownership: You own files strictly inside `F:\Projects\fitness-ecosystem-pro\web/**`. Do NOT touch any other directory.

MANDATORY FIRST STEP:
Read F:\Projects\fitness-ecosystem-pro\.agents\teamwork\ORIGINAL_REQUEST.md completely.
Also read:
- F:\Projects\fitness-ecosystem-pro\.agents\teamwork\orchestrator_4\PROJECT.md
- F:\Projects\fitness-ecosystem-pro\.agents\teamwork\survey_web_cloud_1\report.md

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

Tasks for Milestone 2:
1. In `web/src/cloudSync.js`:
   - Implement bidirectional `anthropometry` synchronization with Google Drive:
     - In `pushAssignedWorkouts` / `syncWorkoutSessionToCloud`: fetch anthropometry history for the user from `db.getAnthropometryHistory(userId)` and populate `AthleteSyncPayload.anthropometry` (`[{ date, weightKg, chestCm, waistCm, bicepsCm }]`) instead of empty array.
     - In `syncCloudWorkoutsToLocal`: when parsing cloud payload `clientData.anthropometry`, persist new measurement rows into `db.addAnthropometry` if not already present.
2. In `web/src/cloudSync.js` & `web/src/db.js`:
   - Align Leaderboard points calculation to match mobile:
     Points formula: `workoutsCount * 10 + Math.floor(tonnage / 100)` (standardized cross-platform contract).
3. Run Automated Tests:
   In `F:\Projects\fitness-ecosystem-pro\web`:
   Run `npm test` (or `node --test tests/security.test.js` and `node --test tests/pin_2fa.test.js`). 100% of tests must pass (55/55 security tests, 13/13 PIN/2FA tests).
   Run `node tests/verification_otp_stress.test.js` — verify 0 locking errors.
4. Verify HTTP and PWA static assets:
   Confirm all endpoints, modals, and APK download links (v1.0.8) remain intact.
5. Write your detailed report to `F:\Projects\fitness-ecosystem-pro\.agents\teamwork\worker_web_m2_gen2\report.md` and handoff to `handoff.md`.
6. Send your completion message back to the orchestrator via `send_message`.
