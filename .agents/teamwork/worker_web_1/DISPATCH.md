## 2026-10-04T07:42:39Z
You are Worker 1 (Web & Telegram Bot Specialist) for the fitness ecosystem project.
Working Directory: F:\Projects\fitness-ecosystem-pro\.agents\teamwork\worker_web_1\
Project Root: F:\Projects\fitness-ecosystem-pro\

MANDATORY FIRST STEPS:
1. Initialize your workspace: create BRIEFING.md and progress.md in your working directory.
2. Read F:\Projects\fitness-ecosystem-pro\.agents\teamwork\ORIGINAL_REQUEST.md (specifically Follow-up — 2026-10-04T07:30:38Z).
3. Read F:\Projects\fitness-ecosystem-pro\PROJECT.md.
4. Read explorer findings:
   - F:\Projects\fitness-ecosystem-pro\.agents\teamwork\survey_explorer_1\analysis.md
   - F:\Projects\fitness-ecosystem-pro\.agents\teamwork\survey_explorer_2\analysis.md

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

EXCLUSIVE WRITE OWNERSHIP:
You own F:\Projects\fitness-ecosystem-pro\web\ and all its files exclusively.
DO NOT touch athlete-app or trainer-app directories.

TASK:
1. Telegram Bot (grammY in web/src/server.js or web/src/bot.js):
   - Support deep linking `/start link_<token>` and command `/link <token>`:
     Parse token, look up corresponding user, bind real numeric `ctx.from.id` (as telegram_id) and `ctx.from.username` into SQLite via `db.linkTelegram(userId, telegramId, telegramUsername)`.
   - In bot greetings and command handlers (`/start`, `/contacts`, `/help`):
     Include prominent inline contact buttons or links to project owners:
     https://t.me/SantiLA213 and https://t.me/Spirit5449
   - In 2FA OTP flow:
     When `POST /api/login` generates a 5-minute OTP for a user with `two_factor_enabled === 1`, if the bot instance is active and user has a `telegram_id`, deliver the 6-digit OTP code to the user's Telegram via `bot.api.sendMessage`.
2. Dynamic 5-min PIN and strict single-use validation (web/src/server.js & web/src/db.js):
   - Verify that athlete PIN is 6 digits, expires in strictly 300 seconds (5 min).
   - If trainer submits an expired code (>5 min) or an already used / consumed code, return strict HTTP 400 error.
   - Verify countdown timer (05:00) and automatic regeneration in web/src/public/app.js.
3. Owner contact links in Web UI (web/src/public/index.html):
   - Ensure links to https://t.me/SantiLA213 and https://t.me/Spirit5449 exist on Login screen, Athlete profile, and Trainer settings.
4. Testing:
   - Update `web/package.json` test script: `"test": "node --test tests/security.test.js tests/pin_2fa.test.js"`.
   - Run tests: `cd F:\Projects\fitness-ecosystem-pro\web && npm test` (or `node --test tests/security.test.js tests/pin_2fa.test.js`).
   - Ensure 100% PASS with 0 failures.

When finished, write a comprehensive handoff report to F:\Projects\fitness-ecosystem-pro\.agents\teamwork\worker_web_1\handoff.md documenting all modified files, test outputs, and verification results. Then send a completion message to parent.
