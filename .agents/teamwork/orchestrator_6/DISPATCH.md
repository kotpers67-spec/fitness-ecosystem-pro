# DISPATCH Log — orchestrator_6

## 2026-10-04T20:27:00Z
You are the Project Orchestrator (orchestrator_6) for Fitness Ecosystem Pro.
Your working directory is F:\Projects\fitness-ecosystem-pro\.agents\teamwork\orchestrator_6.
The project root is F:\Projects\fitness-ecosystem-pro.
Read the authoritative user request from F:\Projects\fitness-ecosystem-pro\.agents\teamwork\ORIGINAL_REQUEST.md (under timestamp 2026-10-04T20:25:27Z).

Mission:
Fix Telegram authentication, session persistence, pairing PIN code synchronization, and background APK updates across Web, Mobile (Athlete & Trainer Android apps), and Telegram bot in Fitness Ecosystem Pro.

Requirements:
### R1. Stateless Session Persistence & Direct PIN Auth (Web/Backend)
- Session tokens must persist across Render server restarts/cold boots using signed HMAC-SHA256 tokens with automatic re-hydration in database.
- `/api/auth/telegram/verify-otp` must accept verification by code alone (`{ code: "123456" }`) without requiring username entry, matching active OTPs and paired user PINs.

### R2. Mobile Auth Parity & 1-Click Telegram Login (Android Apps)
- Mobile auth screens (`AthleteAuthScreen.kt`, `TrainerAuthScreen.kt`) must support 1-step 6-digit code entry dialog without asking for username.
- 1-click Telegram button must trigger `tg://resolve?domain=fitnessecosystemBOT&start=auth_<sessionId>` with fallback and poll session token.
- Both mobile apps must synchronize the exact 6-digit pairing PIN, photo, full name, phone, and restrictions with the website and Telegram bot for the same account.

### R3. In-App Direct APK Background Updates & Build Verification
- APK update download must run directly in the background via download manager/service without opening or redirecting to external browser pages.
- Automated tests (`npm test` in `web/`) must pass 100%.
- Release APKs for Athlete and Trainer apps must build successfully (`gradlew.bat assembleRelease`) and be deployed to `releases/` and `web/releases/`.

Acceptance Criteria:
- Automated Verification:
  * `npm test` runs with 0 failures in `web/`.
  * `./gradlew.bat assembleRelease` passes for `athlete-app` and `trainer-app`.
  * `web/releases/athlete-latest.apk` and `web/releases/trainer-latest.apk` are generated and up-to-date.
- Functional Guardrails:
  * 6-digit PIN login works without providing a username.
  * Refreshing or restarting server does not invalidate active user sessions.
  * Pairing PIN displayed in mobile app matches web profile and bot for the same user.
  * Background APK updater downloads file directly without browser redirect.

Maintain progress.md and BRIEFING.md in your working directory. Report completion to the parent agent when all requirements and acceptance criteria are verified.
