# BRIEFING — 2026-10-04T20:35:00Z

## Mission
Survey athlete-app codebase against requirements R2 and R3 (6-digit PIN auth, 1-click TG auth, profile sync, direct APK updater, build/test baseline).

## 🔒 My Identity
- Archetype: explorer
- Roles: Athlete App Survey Explorer
- Working directory: F:\Projects\fitness-ecosystem-pro\.agents\teamwork\explorer_survey_athlete_6
- Original parent: 7a8dbfe6-c2be-4283-bb75-fe454c56b1fa
- Milestone: survey

## 🔒 Key Constraints
- Read-only investigation — do NOT implement
- Focus on athlete-app: AthleteAuthScreen.kt 1-step 6-digit code dialog without username, 1-click TG login with session polling, 6-digit pairing PIN & photo/name/phone/restrictions sync, background direct APK updater, baseline test and assembleRelease build status.

## Current Parent
- Conversation ID: 7a8dbfe6-c2be-4283-bb75-fe454c56b1fa
- Updated: not yet

## Investigation State
- **Explored paths**:
  - `athlete-app/app/src/main/java/com/athleteapp/pro/ui/screens/AthleteAuthScreen.kt`
  - `athlete-app/app/src/main/java/com/athleteapp/pro/ui/screens/AthleteSettingsScreen.kt`
  - `athlete-app/app/src/main/java/com/athleteapp/pro/ui/AthleteViewModel.kt`
  - `athlete-app/app/src/main/java/com/athleteapp/pro/data/auth/AthleteRemoteAuthManager.kt`
  - `athlete-app/app/src/main/java/com/athleteapp/pro/data/update/AthleteUpdateService.kt`
  - `athlete-app/app/src/main/java/com/athleteapp/pro/data/update/InstallReceiver.kt`
  - `athlete-app/app/src/main/AndroidManifest.xml`
  - `athlete-app/app/src/test/...`
- **Key findings**:
  1. `AthleteAuthScreen.kt` currently uses a 2-step dialog prompting for Telegram username before 6-digit code. Needs simplification into a 1-step direct 6-digit code dialog calling `/api/auth/telegram/verify-otp` with `{ code }`.
  2. 1-click Telegram button only launches web `https://t.me/...` URL; must trigger native `tg://resolve?domain=fitnessecosystemBOT&start=auth_<sessionId>` with web fallback.
  3. `regeneratePairingPin()` in `AthleteViewModel.kt` generates a local PIN without syncing with `/api/athlete/regenerate-pin`; timer doesn't auto-regenerate on 00:00. Profile, photo (<15KB JPEG), name, phone, restrictions are already synchronized.
  4. `AthleteUpdateService.kt` already downloads APK directly via HTTP streaming to cache and `DownloadManager` with 0 browser redirects.
  5. Baseline unit tests (`testDebugUnitTest`) pass 100% in 30s; `assembleRelease` builds successfully in 13s.
- **Unexplored areas**: None, survey complete.

## Key Decisions Made
- Fully documented all 5 areas in `survey_report.md` and `handoff.md`.
- Formulated exact proposed code edits and verification commands for the implementation agent.

## Artifact Index
- DISPATCH.md — Task assignment and message history
- BRIEFING.md — Persistent situational awareness
- survey_report.md — Comprehensive survey report
- handoff.md — 5-component hard handoff report
