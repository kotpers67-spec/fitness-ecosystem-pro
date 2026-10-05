# DISPATCH — worker_trainer_m3_6

You are the Trainer App Implementation Worker.
Your working directory is: F:\Projects\fitness-ecosystem-pro\.agents\teamwork\worker_trainer_m3_6
Original request path: F:\Projects\fitness-ecosystem-pro\.agents\teamwork\ORIGINAL_REQUEST.md (timestamp 2026-10-04T20:25:27Z)
Survey findings: F:\Projects\fitness-ecosystem-pro\.agents\teamwork\explorer_survey_trainer_6\survey_report.md
Project plan: F:\Projects\fitness-ecosystem-pro\.agents\teamwork\orchestrator_6\PROJECT.md

Exclusive Write Ownership:
`trainer-app/` source files only.
You MUST NOT edit any files in `web/` or `athlete-app/`.

Tasks:
1. 1-Step 6-Digit Code Auth Dialog (R2):
   - In `trainer-app/.../TrainerAuthScreen.kt`:
     * Update 6-digit code entry dialog to be 1-step direct numeric input with countdown timer calling `verifyTelegramLogin(otp = code)` without demanding username first.
2. 1-Click Telegram Login (R2):
   - In `trainer-app/.../TrainerAuthScreen.kt`:
     * Launch native intent `tg://resolve?domain=fitnessecosystemBOT&start=$sessionId` with web fallback (`https://t.me/...`).
     * Ensure `session-init` payload sends `{"requestedRole": "trainer"}`.
     * Keep existing session polling mechanism until confirmed or expired.
3. Data Synchronization (R2):
   - In `trainer-app`:
     * Ensure exact 6-digit pairing PIN, photo, full name, phone, and restrictions are properly synced with backend.
     * In `GoogleDriveSyncManager.kt` and `GitHubSyncManager.kt`: ensure `restrictions` and `phone` are preserved and not dropped.
4. Background APK Updater Verification (R3):
   - In `trainer-app` (`UpdateService.kt`):
     * Verify APK update download runs directly in the background via download manager/service without opening or redirecting to external browser pages.
5. Verification:
   - Run `.\gradlew.bat testDebugUnitTest` in `trainer-app/` and ensure 100% pass.
   - Run `.\gradlew.bat assembleRelease` in `trainer-app/` to ensure release build succeeds.

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

Output:
Write changes report to `F:\Projects\fitness-ecosystem-pro\.agents\teamwork\worker_trainer_m3_6\changes.md` and handoff report to `handoff.md`.
Send message to parent when complete.


## 2026-10-04T20:40:49Z
[Message] timestamp=2026-10-04T20:40:49Z sender=7a8dbfe6-c2be-4283-bb75-fe454c56b1fa priority=MESSAGE_PRIORITY_HIGH content=You are the Trainer App Worker (worker_trainer_m3_6).
Your working directory is F:\Projects\fitness-ecosystem-pro\.agents\teamwork\worker_trainer_m3_6.
Read your dispatch instructions in F:\Projects\fitness-ecosystem-pro\.agents\teamwork\worker_trainer_m3_6\DISPATCH.md.
Read the survey report in F:\Projects\fitness-ecosystem-pro\.agents\teamwork\explorer_survey_trainer_6\survey_report.md.
Read the original request in F:\Projects\fitness-ecosystem-pro\.agents\teamwork\ORIGINAL_REQUEST.md (timestamp 2026-10-04T20:25:27Z).
Implement all tasks in trainer-app (1-step 6-digit PIN dialog with countdown without username, 1-click TG native intent with fallback, restrictions/phone sync, direct background APK updater).
Run .\gradlew.bat testDebugUnitTest in trainer-app and verify 100% pass.
Run .\gradlew.bat assembleRelease in trainer-app and verify release build succeeds.
Produce changes.md and handoff.md in your working directory.
Send message to parent when complete.
