## 2026-10-05T04:07:49Z
[Message] timestamp=2026-10-05T04:07:49Z sender=386a8d4c-d330-48c4-9a3b-18237f181d54 priority=MESSAGE_PRIORITY_HIGH content=You are the Project Orchestrator (orchestrator_7) for the task defined in F:\Projects\fitness-ecosystem-pro\.agents\teamwork\ORIGINAL_REQUEST.md under section "## 2026-10-05T04:06:54Z".

Working directory: F:\Projects\fitness-ecosystem-pro\.agents\teamwork\orchestrator_7
Project root: F:\Projects\fitness-ecosystem-pro
Integrity mode: development

Task Objective:
Comprehensive verification, release validation, and forensic audit of Telegram authentication, persistent sessions, exercise catalog search, sorting and auto-collapse, and background APK updates across Web, Mobile (Athlete & Trainer Android apps), and Telegram bot in Fitness Ecosystem Pro.

Requirements:
### R1. Stateless Session Persistence & Direct PIN Auth (Web/Backend)
- Session tokens must persist across Render server restarts/cold boots using signed HMAC-SHA256 tokens with automatic re-hydration in database.
- `/api/auth/telegram/verify-otp` must accept verification by code alone (`{ code: "123456" }`) without requiring username entry.

### R2. Exercise Catalog, Sorting & Auto-Collapse (Web & Android)
- Trainer exercise addition must feature a searchable list/catalog (35+ exercises by muscle group) without carousel limitations or daily exercise caps.
- Completed exercises (where all sets are checked) must sort to the bottom of the list with pending exercises at the top.
- Completed exercises must auto-collapse 1 hour after the first exercise of the session was created, with a manual expand/collapse toggle.

### R3. Mobile Auth Parity & In-App APK Updates (Android Apps)
- Mobile auth screens (`AthleteAuthScreen.kt`, `TrainerAuthScreen.kt`) must support 1-step 6-digit code entry dialog without asking for username.
- 1-click Telegram button must trigger `tg://resolve?domain=fitnessecosystemBOT&start=auth_<sessionId>` with fallback and poll session token.
- Background APK updater must download APK directly via download service without browser redirects.

Acceptance Criteria:
- Automated Verification:
  - `npm test` runs with 0 failures in `web/` (all 57+ security and sync tests pass).
  - `./gradlew.bat test` passes with 0 failures for `athlete-app` and `trainer-app`.
  - `./gradlew.bat assembleRelease` passes for `athlete-app` and `trainer-app`.
  - `releases/athlete-latest.apk` and `releases/trainer-latest.apk` are generated and up-to-date.
- Functional Guardrails:
  - 6-digit PIN login works without providing a username.
  - Refreshing or restarting server does not invalidate active user sessions.
  - Pairing PIN displayed in mobile app matches web profile and bot for the same user.
  - Searchable exercise catalog filters in real-time.
  - Completed exercises sort to bottom and auto-collapse after 1 hour.
  - Background APK updater downloads file directly without browser redirect.
