# BRIEFING — 2026-10-04T20:41:00Z

## Mission
Implement 1-step 6-digit code auth dialog without username, 1-click TG native intent with fallback and trainer role init, restrictions and phone sync in Google Drive and GitHub sync managers, and verify direct background APK updater and builds in trainer-app.

## 🔒 My Identity
- Archetype: worker_trainer_m3_6
- Roles: implementer, qa, specialist
- Working directory: F:\Projects\fitness-ecosystem-pro\.agents\teamwork\worker_trainer_m3_6
- Original parent: 7a8dbfe6-c2be-4283-bb75-fe454c56b1fa
- Milestone: milestone_3_remediation

## 🔒 Key Constraints
- Exclusive write ownership: `trainer-app/` source files only.
- MUST NOT edit any files in `web/` or `athlete-app/`.
- Zero-mocks mandate: all implementations must be genuine, maintain real state, produce real behavior.
- Run `.\gradlew.bat testDebugUnitTest` and `.\gradlew.bat assembleRelease` in `trainer-app/` to verify.

## Current Parent
- Conversation ID: 7a8dbfe6-c2be-4283-bb75-fe454c56b1fa
- Updated: 2026-10-04T20:41:00Z

## Task Summary
- **What to build**:
  1. 1-Step 6-digit PIN dialog without username in `TrainerAuthScreen.kt` + `verifyTelegramLogin(otp, username = "")` in `TrainerRemoteAuthManager.kt`.
  2. 1-Click TG login native intent `tg://resolve?domain=fitnessecosystemBOT&start=$sessionId` with web fallback, and `{"requestedRole":"trainer"}` in session-init.
  3. Data synchronization: ensure 6-digit PIN, photo, full name, phone, and restrictions are synced and preserved across `GoogleDriveSyncManager.kt` and `GitHubSyncManager.kt`.
  4. Verify direct background APK updater in `UpdateService.kt`.
  5. Run unit tests and release assemble, write changes.md and handoff.md.
- **Success criteria**: 100% test pass on `.\gradlew.bat testDebugUnitTest`, release APK build succeeds on `.\gradlew.bat assembleRelease`.
- **Interface contracts**: `F:\Projects\fitness-ecosystem-pro\.agents\teamwork\orchestrator_6\PROJECT.md`
- **Code layout**: `trainer-app/app/src/main/java/com/trainerapp/pro/...`

## Change Tracker
- **Files modified**: None yet
- **Build status**: Pending
- **Pending issues**: None

## Quality Status
- **Build/test result**: Pending
- **Lint status**: 0 violations known
- **Tests added/modified**: Pending

## Loaded Skills
- None specified in dispatch prompt.

## Key Decisions Made
- [TBD]

## Artifact Index
- `F:\Projects\fitness-ecosystem-pro\.agents\teamwork\worker_trainer_m3_6\changes.md` — Changes report
- `F:\Projects\fitness-ecosystem-pro\.agents\teamwork\worker_trainer_m3_6\handoff.md` — Final handoff report
