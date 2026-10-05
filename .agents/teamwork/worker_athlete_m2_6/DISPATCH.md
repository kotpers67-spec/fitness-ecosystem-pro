# DISPATCH — worker_athlete_m2_6

You are the Athlete App Implementation Worker.
Your working directory is: F:\Projects\fitness-ecosystem-pro\.agents\teamwork\worker_athlete_m2_6
Original request path: F:\Projects\fitness-ecosystem-pro\.agents\teamwork\ORIGINAL_REQUEST.md (timestamp 2026-10-04T20:25:27Z)
Survey findings: F:\Projects\fitness-ecosystem-pro\.agents\teamwork\explorer_survey_athlete_6\survey_report.md
Project plan: F:\Projects\fitness-ecosystem-pro\.agents\teamwork\orchestrator_6\PROJECT.md

Exclusive Write Ownership:
`athlete-app/` source files only.
You MUST NOT edit any files in `web/` or `trainer-app/`.

Tasks:
1. 1-Step 6-Digit Code Auth Dialog (R2):
   - In `athlete-app/.../AthleteAuthScreen.kt`:
     * Update the 6-digit code entry dialog to be 1-step direct numeric input without asking for username.
     * Call the auth verification endpoint with `{ "code": "..." }` alone (no username requirement).
2. 1-Click Telegram Login (R2):
   - In `athlete-app/.../AthleteAuthScreen.kt`:
     * Trigger native intent `tg://resolve?domain=fitnessecosystemBOT&start=auth_<sessionId>` with web fallback to `https://t.me/...` if Telegram app is not installed.
     * Keep existing session polling mechanism until confirmed or expired.
3. Pairing PIN & Profile Synchronization (R2):
   - In `athlete-app`:
     * Synchronize the exact 6-digit pairing PIN, photo, full name, phone, and restrictions with the website and Telegram bot for the same account.
     * When regenerating PIN or when timer expires (5 minutes), call backend endpoint (e.g. `/api/pairing/regenerate` or `/api/athlete/regenerate-pin`) to keep PIN strictly synchronized with the Web and Bot, rather than purely local generation.
4. Background APK Updater Verification (R3):
   - In `athlete-app` (`AthleteUpdateService.kt`):
     * Confirm direct background downloading via DownloadManager or HTTP download without opening/redirecting to external browser pages.
5. Verification:
   - Run `.\gradlew.bat testDebugUnitTest` in `athlete-app/` and ensure 100% pass.
   - Run `.\gradlew.bat assembleRelease` in `athlete-app/` to ensure release build succeeds.

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

Output:
Write changes report to `F:\Projects\fitness-ecosystem-pro\.agents\teamwork\worker_athlete_m2_6\changes.md` and handoff report to `handoff.md`.
Send message to parent when complete.

## 2026-10-04T20:40:49Z
[Message] sender=7a8dbfe6-c2be-4283-bb75-fe454c56b1fa
You are the Athlete App Worker (worker_athlete_m2_6).
Your working directory is F:\Projects\fitness-ecosystem-pro\.agents\teamwork\worker_athlete_m2_6.
Read your dispatch instructions in F:\Projects\fitness-ecosystem-pro\.agents\teamwork\worker_athlete_m2_6\DISPATCH.md.
Read the survey report in F:\Projects\fitness-ecosystem-pro\.agents\teamwork\explorer_survey_athlete_6\survey_report.md.
Read the original request in F:\Projects\fitness-ecosystem-pro\.agents\teamwork\ORIGINAL_REQUEST.md (timestamp 2026-10-04T20:25:27Z).
Implement all tasks in athlete-app (1-step 6-digit PIN dialog without username, 1-click TG native intent with fallback, PIN & profile sync, direct background APK updater).
Run .\gradlew.bat testDebugUnitTest in athlete-app and verify 100% pass.
Run .\gradlew.bat assembleRelease in athlete-app and verify release build succeeds.
Produce changes.md and handoff.md in your working directory.
Send message to parent when complete.
