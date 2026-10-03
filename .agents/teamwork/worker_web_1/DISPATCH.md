# Task Assignment: Web Worker (Milestone 2)

**Working Directory**: F:\Projects\fitness-ecosystem-pro\.agents\teamwork\worker_web_1
**Original Request**: F:\Projects\fitness-ecosystem-pro\.agents\teamwork\ORIGINAL_REQUEST.md
**Project Plan**: F:\Projects\fitness-ecosystem-pro\PROJECT.md
**Web Explorer Report**: F:\Projects\fitness-ecosystem-pro\.agents\teamwork\explorer_survey_web_1\handoff.md
**Security Explorer Report**: F:\Projects\fitness-ecosystem-pro\.agents\teamwork\explorer_survey_security_1\handoff.md

## Exclusive File Ownership
You exclusively own and may edit:
- `web/src/db.js`
- `web/src/server.js`
- `web/src/public/index.html`
- `web/src/public/styles.css`
- `web/src/public/app.js`
- `web/src/public/qr.js`
- `web/start.bat`
- `web/start.ps1`

Do NOT touch `trainer-app/**`, `athlete-app/**`, or `web/tests/**`.

## Mandatory Integrity Warning
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

## Objective & Implementation Steps
Read `F:\Projects\fitness-ecosystem-pro\.agents\teamwork\explorer_survey_web_1\handoff.md` and implement the complete Web Portal:
1. **Backend Additions in `web/src/db.js`**:
   - `toggleWorkoutSet(setId, isCompleted)`: UPDATE workout_sets SET is_completed = ? WHERE id = ?
   - `deleteWorkoutSet(setId)`: DELETE FROM workout_sets WHERE id = ?
   - `updateUserRole(userId, newRole)`: UPDATE users SET role = ? WHERE id = ?
   - Ensure parameterized queries throughout.
2. **Backend Additions in `web/src/server.js`**:
   - `POST /api/workout/set/toggle`: accepts `{ setId, isCompleted }`
   - `DELETE /api/workout/set`: accepts `{ setId }`
   - `POST /api/user/role`: accepts `{ role: 'athlete' | 'trainer' }`, updates role, regenerates 6-digit PIN if transitioning to athlete
   - `POST /api/trainer/unpair`: accepts `{ athleteId }`, removes row from `trainer_clients`
   - `POST /api/athlete/unpair`: removes pairing for requesting athlete
   - Export `authLimiter` in `module.exports = { server, db, authLimiter };`
   - Path traversal hardening: use `decodeURIComponent(parsedUrl.pathname)` before path normalization
3. **Frontend SPA in `web/src/public/`**:
   - Swiss-Style Dark Theme (`#0d0d0d`, Bento Grid `border-white/10`, Swiss typographic hierarchy, Anti-Overlap Guard with `min-w-0`, `truncate`, responsive desktop/mobile).
   - Zero external npm / CDN dependencies.
   - `qr.js`: pure client-side SVG QR code generator (renders clean vector QR into athlete's pairing card).
   - Athlete View:
     - Clear SVG QR code display and large 6-digit PIN (tabular numbers, e.g. "739102").
     - Buttons: "Скопировать PIN", "Скопировать ссылку для тренера" (`https://fitnessapp.pro/pair?code=...`), "Перегенерировать PIN".
     - Connected Trainer info with "Позвонить" and "Отвязать".
     - Workout Logger: date picker, exercise chips, previous workout stats, weight, reps, RPE, and set logging.
     - Sets List: set rows with completion checkbox (`is_completed`), weight x reps, RPE tag, delete button.
     - Privacy Toggle for public leaderboard.
     - Role Switcher button.
   - Trainer View:
     - Instant 6-digit PIN pairing input (auto-cleans any non-digits, no dash needed!).
     - Clients Bento Grid: client cards with workouts count, total tonnage, exercise history viewer, and unpair option.
   - Leaderboard View (Zero-Mocks):
     - Displays real ranked athletes from SQLite.
     - Clean empty state when no public workouts exist.
4. **Startup Scripts**:
   - `web/start.bat` and `web/start.ps1` using the detected Node runtime.

## Verification
You MUST test the web server:
- Launch server and verify HTTP 200 responses on `/`, `/api/leaderboard`, and static assets.
- Verify Zero-Mocks: database has 0 fake users.
- Verify node test execution if applicable.

Document all created files, changes, and test results in `F:\Projects\fitness-ecosystem-pro\.agents\teamwork\worker_web_1\handoff.md` and send a message when done.


## 2026-10-03T19:20:41Z
Received dispatch from parent (f19f8947-a22d-4cff-98b7-961f56b45b31):
You are the Web Worker for Milestone 2.
Working Directory: F:\Projects\fitness-ecosystem-pro\.agents\teamwork\worker_web_1
Original Request: F:\Projects\fitness-ecosystem-pro\.agents\teamwork\ORIGINAL_REQUEST.md
Project Plan: F:\Projects\fitness-ecosystem-pro\PROJECT.md
Dispatch Instructions: F:\Projects\fitness-ecosystem-pro\.agents\teamwork\worker_web_1\DISPATCH.md
Web Explorer Report: F:\Projects\fitness-ecosystem-pro\.agents\teamwork\explorer_survey_web_1\handoff.md
Security Explorer Report: F:\Projects\fitness-ecosystem-pro\.agents\teamwork\explorer_survey_security_1\handoff.md
