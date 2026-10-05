# DISPATCH — explorer_survey_athlete_6

You are the Athlete App Survey Explorer.
Your working directory is: F:\Projects\fitness-ecosystem-pro\.agents\teamwork\explorer_survey_athlete_6
Original request path: F:\Projects\fitness-ecosystem-pro\.agents\teamwork\ORIGINAL_REQUEST.md (read section under timestamp 2026-10-04T20:25:27Z)
Target codebase: F:\Projects\fitness-ecosystem-pro\athlete-app

Mission:
Survey the Athlete Android app codebase to identify current state and exact changes required for:
1. 1-Step 6-Digit Code Auth Dialog (R2):
   - Current implementation in `AthleteAuthScreen.kt` and associated ViewModels/Repositories.
   - Enabling 1-step 6-digit code entry dialog without asking for username.
2. 1-Click Telegram Login (R2):
   - 1-click button triggering `tg://resolve?domain=fitnessecosystemBOT&start=auth_<sessionId>` with web fallback and polling session token.
3. Data Synchronization (R2):
   - Exact 6-digit pairing PIN, photo, full name, phone, restrictions synchronization with backend.
   - Profile screen and Room database persistence.
4. Direct Background APK Updater (R3):
   - Current updater implementation in `athlete-app` (`UpdateService.kt`, update dialogs, etc.).
   - Background download via DownloadManager / foreground service directly to disk without browser redirects.
5. Existing build and test state:
   - Run unit tests and check assembleRelease status in `athlete-app`.

Output:
Write a comprehensive report to F:\Projects\fitness-ecosystem-pro\.agents\teamwork\explorer_survey_athlete_6\survey_report.md
Include: Current implementation files, gaps against requirements R2 and R3, exact proposed file edits/additions, baseline build/test results.
Send completion message when done.


## 2026-10-04T20:29:19Z
You are the Athlete App Survey Explorer.
Your working directory is F:\Projects\fitness-ecosystem-pro\.agents\teamwork\explorer_survey_athlete_6.
Read your dispatch instructions in F:\Projects\fitness-ecosystem-pro\.agents\teamwork\explorer_survey_athlete_6\DISPATCH.md.
Read the user request in F:\Projects\fitness-ecosystem-pro\.agents\teamwork\ORIGINAL_REQUEST.md (timestamp 2026-10-04T20:25:27Z).
Investigate F:\Projects\fitness-ecosystem-pro\athlete-app.
Focus on:
1. AthleteAuthScreen.kt: 1-step 6-digit code entry dialog without asking for username.
2. 1-click Telegram button with tg://resolve?domain=fitnessecosystemBOT&start=auth_<sessionId>, fallback, polling.
3. Exact 6-digit pairing PIN, photo, full name, phone, restrictions sync.
4. Background APK updater downloading directly without browser redirects.
5. Baseline test and assembleRelease build status.
Produce F:\Projects\fitness-ecosystem-pro\.agents\teamwork\explorer_survey_athlete_6\survey_report.md and handoff.md.
Send message back to parent when complete.
