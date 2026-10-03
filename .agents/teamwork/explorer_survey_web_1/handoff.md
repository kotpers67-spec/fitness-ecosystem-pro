# Handoff Report: Web Portal Codebase Survey & Architecture

**Agent**: `explorer_survey_web_1`  
**Working Directory**: `F:\Projects\fitness-ecosystem-pro\.agents\teamwork\explorer_survey_web_1`  
**Target Project**: `F:\Projects\fitness-ecosystem-pro\web`  
**Date**: 2026-10-03  
**Status**: Complete (Hard Handoff)  

---

## 1. Observation

### 1.1 Filesystem & Existing Codebase
Direct inspection of `F:\Projects\fitness-ecosystem-pro\web` revealed the following directory structure:
```
F:\Projects\fitness-ecosystem-pro\web
├── package.json         (12 lines, 398 bytes)
├── fitness.sqlite       (created upon DB init, 45,056 bytes, 0 rows)
├── src/
│   ├── server.js        (342 lines, native node:http server)
│   ├── db.js            (271 lines, native node:sqlite DatabaseSync)
│   ├── security.js      (112 lines, scrypt auth, sanitization, rate limiter)
│   └── public/          (EMPTY DIRECTORY)
└── tests/               (EMPTY DIRECTORY)
```

### 1.2 Runtime Environment & Port
- **Node.js Binary**: A standard `node -v` in PowerShell failed with `CommandNotFoundException` due to an invalid path in `$env:PATH`.
- **Installed Runtime Located**: Node.js 24 (`v24.21.0`) was identified at:
  `C:\Users\kotpe\AppData\Local\OpenAI\Codex\runtimes\cua_node\45309f9050f7314b\bin\node.exe`
- **Native Modules Verified**:
  - `node:sqlite`: Successfully executed `new DatabaseSync(':memory:')`.
  - `node:test`: Successfully executed native unit test runner with `assert.strictEqual(1, 1)`.
  - `node:http`, `node:crypto`, `node:fs`, `node:path`: Fully functional.
  - **Zero npm dependencies**: The entire web server runs natively with zero external dependencies in `node_modules`.
- **Port 3000 Availability**:
  - `Get-NetTCPConnection -LocalPort 3000` returned no active connections. Port 3000 is completely free.
  - `src/server.js:23`: `const PORT = process.env.PORT || 3000;`.

### 1.3 Backend Implementation (`server.js`, `db.js`, `security.js`)
- **Database Schema**: Initialized in `src/db.js:16-67`:
  - `users`: `id`, `username UNIQUE`, `password_hash`, `role` (`athlete`/`trainer`), `full_name`, `phone`, `pairing_code`, `is_private`, `created_at`.
  - `trainer_clients`: `id`, `trainer_id`, `athlete_id`, `paired_at`, `UNIQUE(trainer_id, athlete_id)`.
  - `workout_sessions`: `id`, `athlete_id`, `date`, `UNIQUE(athlete_id, date)`.
  - `workout_sets`: `id`, `session_id`, `exercise_name`, `weight_kg`, `reps`, `rpe`, `is_completed`, `created_at`.
  - `auth_tokens`: `token PRIMARY KEY`, `user_id`, `expires_at`.
- **Zero-Mocks Status**:
  - Direct query `SELECT count(*) as count FROM users` returned `0`.
  - No seeded dummy users ("Максим Громов", "Елена Соколова" etc.) exist in the web database.
  - Leaderboard query in `src/db.js:221-234` queries only real users (`WHERE u.role = 'athlete' AND u.is_private = 0`).
- **Security Defenses**:
  - `src/security.js` provides `hasSqlInjectionVector(input)`, `escapeHtml(str)`, `RateLimiter(60000, 15)`, `hashPassword(password)` via `crypto.scryptSync`, and strict `SECURITY_HEADERS` (CSP, X-Content-Type-Options: nosniff, X-Frame-Options: DENY).
  - All DB queries in `src/db.js` use prepared statements with parameterized positional parameters (`?`).

### 1.4 Missing Components & Gaps Directly Observed
1. **Frontend Assets Missing**:
   - `web/src/public` is empty. There is no `index.html`, `styles.css`, or client JavaScript.
2. **Security Test Suite Missing**:
   - `web/tests` is empty. `web/tests/security.test.js` required by R3 and `package.json` does not exist yet.
3. **Backend API Functional Gaps**:
   - **Mark/Toggle Set Completed**: `workout_sets.is_completed` exists in the database schema (`src/db.js:55`), but neither `db.js` nor `server.js` contains a method or endpoint to toggle set completion (`POST /api/workout/set/toggle`).
   - **Role Switching**: R2 requires "переключение ролей (trainer/athlete)". There is currently no endpoint (`POST /api/user/role`) or DB method to switch active roles.
   - **Unpair Endpoint**: `db.unpairAthlete(athleteId)` exists in `src/db.js:161`, but no corresponding API route exists in `src/server.js`.
   - **Delete Workout Set**: Athletes/trainers cannot remove mistakenly entered sets.

---

## 2. Logic Chain

1. **Premise 1**: The user requirement R2 mandates a local single-page application (SPA) on `http://localhost:3000` with dark theme (`#0d0d0d`), Swiss typography, Anti-Overlap Guard, workout logging with set marking, clean QR and 6-digit PIN display, instant 6-digit pairing without dashes, role switching, and 100% real data leaderboard.
2. **Premise 2**: Requirement R3 mandates that `web/tests/security.test.js` passes 100% with 0 vulnerabilities, covering SQL injection attacks, XSS attacks, rate limiting, and access isolation.
3. **Observation Reference**: `src/server.js` already provides an HTTP server with routing and static file fallback to `src/public/index.html` (lines 319-328), but `src/public/` and `tests/` contain no files.
4. **Deduction 1 (Backend enhancements)**: Before the frontend and test suite can function completely, `src/db.js` and `src/server.js` require minor additions:
   - Add `toggleWorkoutSet(setId, isCompleted)` and `deleteWorkoutSet(setId)` in `db.js`.
   - Add `updateUserRole(userId, newRole)` in `db.js`.
   - Add `POST /api/workout/set/toggle` and `DELETE /api/workout/set` in `server.js`.
   - Add `POST /api/user/role` for instant role switching in `server.js`.
   - Add `POST /api/trainer/unpair` and `POST /api/athlete/unpair` in `server.js`.
5. **Deduction 2 (Frontend SPA design)**: A high-performance, dependency-free vanilla JS SPA in `src/public/` (`index.html`, `styles.css`, `app.js`, `qr.js`) is the optimal architecture:
   - Eliminates build steps, bundlers, and npm install overhead.
   - Strictly enforces `#0d0d0d` background, `border-white/10`, Framer-grade Bento Grid, Swiss typographic hierarchy, and Anti-Overlap Guard (`min-w-0`, `truncate`+title, `tabular-nums`).
   - Provides pure client-side SVG QR code generation without external network CDNs.
   - Automatically sanitizes 6-digit PIN inputs (`replace(/\D/g, '').slice(0, 6)`) eliminating any dash input requirement.
6. **Deduction 3 (Security test suite)**: `web/tests/security.test.js` using Node's native `node:test`, `node:assert`, and native `fetch` can spin up an ephemeral test server on port 0, execute exhaustive penetration tests across all 4 OWASP ASVS categories, and assert 100% PASS cleanly.

---

## 3. Caveats

1. **Node.js Environment Variable**: The developer environment's global PATH does not point to `node.exe`. The runtime must be invoked via `C:\Users\kotpe\AppData\Local\OpenAI\Codex\runtimes\cua_node\45309f9050f7314b\bin\node.exe` or wrapped in a batch/PowerShell launcher (`start.bat` / `start.ps1`).
2. **Dual-Role Workflow**: In `users` table, each user has a single `role` column. Role switching toggles this column and generates a pairing code if transitioning to athlete. Historical paired data remains linked by user ID.
3. **Local-Only Scope**: The web portal runs strictly on `localhost:3000` with no internet access or external API calls required. All SVG QR generation and crypto hashing are executed locally.

---

## 4. Conclusion & Proposed Architecture

### 4.1 Backend Architecture Enhancements (`server.js` & `db.js`)

#### A. Database Layer (`web/src/db.js`) Additions:
```javascript
// Toggle set completion status
toggleWorkoutSet(setId, isCompleted) {
  const stmt = this.db.prepare('UPDATE workout_sets SET is_completed = ? WHERE id = ?');
  return stmt.run(isCompleted ? 1 : 0, setId);
}

// Delete workout set
deleteWorkoutSet(setId) {
  const stmt = this.db.prepare('DELETE FROM workout_sets WHERE id = ?');
  return stmt.run(setId);
}

// Update user role
updateUserRole(userId, newRole) {
  if (newRole !== 'athlete' && newRole !== 'trainer') return false;
  const stmt = this.db.prepare('UPDATE users SET role = ? WHERE id = ?');
  stmt.run(newRole, userId);
  return true;
}
```

#### B. API Endpoints (`web/src/server.js`) Additions:
1. `POST /api/workout/set/toggle`: Accepts `{ setId, isCompleted }`. Toggles completion checkbox.
2. `DELETE /api/workout/set`: Accepts query or body `{ setId }`. Removes set.
3. `POST /api/user/role`: Accepts `{ role: 'athlete' | 'trainer' }`. Updates active role, creates pairing PIN if new athlete, returns updated profile.
4. `POST /api/trainer/unpair`: Accepts `{ athleteId }`. Removes pairing row from `trainer_clients`.
5. `POST /api/athlete/unpair`: Removes pairing row for caller athlete.

---

### 4.2 Frontend SPA Architecture (`web/src/public`)

#### File Breakdown:
1. `index.html`:
   - Single-Page Application container with semantic `<header>`, `<main>`, `<nav>`, `<aside>`.
   - Meta viewport: `width=device-width, initial-scale=1.0, maximum-scale=5.0`.
   - Theme meta: `<meta name="theme-color" content="#0d0d0d">`.
2. `styles.css`:
   - **Colors**:
     - Background: `#0d0d0d`
     - Surface Cards: `#141414` (Bento border: `1px solid rgba(255, 255, 255, 0.08)`)
     - Elevated Surface: `#1c1c1c`
     - Accent Primary: `#10b981` (Emerald / Workout Active)
     - Accent Trainer: `#6366f1` (Indigo / Coach Accent)
     - Accent Danger: `#f43f5e` (Rose / Unpair, Delete)
     - Text Primary: `#ffffff`, Text Secondary: `#a1a1aa`, Text Muted: `#71717a`
   - **Swiss Typography**:
     - Font family: `system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif`
     - Micro-labels: `font-size: 11px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.08em; color: #a1a1aa;`
     - Number metrics: `font-variant-numeric: tabular-nums; font-weight: 700;`
     - Asymmetric grid layouts with proportional negative space.
   - **Anti-Overlap Guard**:
     - `min-width: 0;` applied to all flex items and CSS grid cells.
     - `.truncate`: `overflow: hidden; text-overflow: ellipsis; white-space: nowrap;`
     - Tooltip attribute `data-tooltip` for truncated text on hover/focus.
     - Responsive Breakpoints:
       - Mobile (`< 768px`): 1-column stack, bottom navigation bar, touch targets >= 44px.
       - Desktop (`>= 768px`): Multi-column Bento Grid (2 to 4 columns), top navigation bar.
3. `qr.js`:
   - Lightweight, standalone QR Code SVG Generator (pure JS, zero external dependencies).
   - Generates vector `<svg>` markup directly into `#athlete-qr-container`.
4. `app.js`:
   - State management: `state.user`, `state.token`, `state.activeRole`, `state.currentDate`.
   - Views:
     - **Auth Modal / Screen**: Tabs for Login & Registration with instant role selection.
     - **Athlete View**:
       - *Pairing Card*: Displays clean SVG QR code, 6-digit PIN in large tabular font (`text-3xl`), "Скопировать PIN", "Скопировать ссылку для тренера", "Перегенерировать PIN".
       - *Trainer Card*: Connected trainer info, "Позвонить" button, "Отвязать" button.
       - *Workout Logger*: Date selector, exercise quick chips, previous workout stats lookup, weight/reps/RPE inputs, "Добавить подход" button.
       - *Sets List*: Set rows with completion checkbox (`is_completed`), weight x reps, RPE tag, delete button.
       - *Privacy Toggle*: Switch for public leaderboard participation.
       - *Role Switcher*: Button in top bar to switch into Trainer mode.
     - **Trainer View**:
       - *Instant Pairing Card*: Input for 6 digits (auto-strips non-digits, no dash needed), "Привязать атлета" button.
       - *Clients Bento Grid*: Athlete cards with full name, phone, sessions count, total tonnage, and exercise history inspection.
     - **Competitions & Leaderboard (Zero-Mocks)**:
       - Table with Rank (🥇, 🥈, 🥉, 4+), Athlete Name, Workouts Count, Total Tonnage, Total Points.
       - 100% real data from SQLite. Empty state message when no public workouts are logged.

---

### 4.3 Security Test Suite Architecture (`web/tests/security.test.js`)

The test suite must be built with native `node:test` and `node:assert`, importing `src/server.js` and running tests against an ephemeral server port (`server.listen(0)`).

#### Test Categories to Cover:
1. **SQL Injection Defense (100% Immunity)**:
   - `POST /api/login`: Attempt auth bypass with `' OR '1'='1`, `admin'--`, `' UNION SELECT ...`. Expect 400 or 401, zero data leak.
   - `POST /api/register`: Attempt injection in `username`, `fullName`, `phone`. Expect 400 rejection via `hasSqlInjectionVector` or regex validation.
   - `POST /api/workout/set`: Inject `Bench'); DROP TABLE workout_sets;--` into `exerciseName`. Verify sanitized insertion, table integrity maintained.
   - `POST /api/trainer/pair`: Inject `' OR 1=1` into `code`. Expect 400 rejection.
   - `GET /api/workout?date=...` and `GET /api/trainer/exercise-history`: Inject SQL into query parameters. Verify parameterized query blocks any execution.
2. **Cross-Site Scripting (XSS) Sanitization**:
   - `fullName` containing `<script>alert('xss')</script>`. Verify sanitized to `&lt;script&gt;alert(&#x27;xss&#x27;)&lt;&#x2F;script&gt;`.
   - `exerciseName` containing `<img src=x onerror=alert(1)>`. Verify HTML escaping.
   - HTTP Response Headers: Assert presence of `Content-Security-Policy`, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `X-XSS-Protection: 1; mode=block`.
3. **Rate Limiting & Brute Force Defense**:
   - Send 16 rapid sequential POST requests to `/api/login` and `/api/register` from same IP.
   - Assert requests 1-15 return standard auth response; request 16 returns HTTP `429 Too Many Requests`.
4. **Access Isolation & Authorization Boundaries**:
   - Unauthenticated access to `/api/me`, `/api/workout`, `/api/trainer/clients` returns `401 Unauthorized`.
   - Athlete token requesting `/api/trainer/clients` returns `403 Forbidden`.
   - Athlete token requesting `/api/trainer/pair` returns `403 Forbidden`.
   - Trainer token requesting `/api/athlete/regenerate-pin` returns `403 Forbidden`.
   - Invalid or tampered Bearer token returns `401 Unauthorized`.
5. **Path Traversal Defense**:
   - `GET /../../etc/passwd` or `GET /..\..\package.json` returns `403 Access Denied`.

---

### 4.4 Startup Scripts & Developer Ergonomics

Create `web/start.bat` and `web/start.ps1`:
```bat
@echo off
setlocal
cd /d "%~dp0"
set "NODE_EXE=node"
where node >nul 2>nul
if %errorlevel% neq 0 (
  if exist "C:\Users\kotpe\AppData\Local\OpenAI\Codex\runtimes\cua_node\45309f9050f7314b\bin\node.exe" (
    set "NODE_EXE=C:\Users\kotpe\AppData\Local\OpenAI\Codex\runtimes\cua_node\45309f9050f7314b\bin\node.exe"
  )
)
echo [Fitness Ecosystem Web] Starting server on http://localhost:3000...
"%NODE_EXE%" src/server.js
```

---

## 5. Verification Method

### 5.1 Verification Commands
To independently verify the web portal and security test suite:

1. **Security Test Suite Verification**:
   ```powershell
   & "C:\Users\kotpe\AppData\Local\OpenAI\Codex\runtimes\cua_node\45309f9050f7314b\bin\node.exe" --test F:\Projects\fitness-ecosystem-pro\web\tests\security.test.js
   ```
   *Expected Result*: All test suites pass with 0 failures, 100% PASS.

2. **Web Server Startup & Port Verification**:
   ```powershell
   & "C:\Users\kotpe\AppData\Local\OpenAI\Codex\runtimes\cua_node\45309f9050f7314b\bin\node.exe" F:\Projects\fitness-ecosystem-pro\web\src\server.js
   ```
   *Expected Result*: Logs `[Fitness Ecosystem Web] Server running at http://localhost:3000`.

3. **HTTP Endpoint & Static File Verification**:
   ```powershell
   Invoke-RestMethod -Uri "http://localhost:3000/api/leaderboard" -Method GET
   Invoke-WebRequest -Uri "http://localhost:3000/" -Method GET
   ```
   *Expected Result*: Returns `200 OK`, HTML content with dark theme `#0d0d0d` and empty real-data leaderboard array `{"leaderboard":[]}`.

4. **Zero-Mocks Database Invalidation Condition**:
   ```powershell
   & "C:\Users\kotpe\AppData\Local\OpenAI\Codex\runtimes\cua_node\45309f9050f7314b\bin\node.exe" -e "const { DatabaseSync } = require('node:sqlite'); const db = new DatabaseSync('F:/Projects/fitness-ecosystem-pro/web/fitness.sqlite'); const c = db.prepare('SELECT count(*) as count FROM users').get().count; console.log('Users:', c); db.close();"
   ```
   *Invalidation Condition*: Any presence of pre-seeded/mock users ("Максим", "Елена", etc.) invalidates compliance.
