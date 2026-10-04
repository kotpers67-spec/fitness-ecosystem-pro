# Plan — Full Ecosystem Audit & Verification (orchestrator_5)

## Objective
Comprehensive audit and verification of Fitness Ecosystem Pro across:
- Athlete Pro Android App (`athlete-app`)
- Trainer Pro Android App (`trainer-app`)
- Web Portal & Telegram Bot (`web`)

## Requirements Matrix
- **R1: Athlete Pro**
  - Telegram 1-click & 6-digit OTP auth
  - 2FA block on 1-click (enforce 6-digit OTP when 2FA enabled)
  - Workout diary: rename "Подходы", delete sets, add custom exercises (`+ Добавить упражнение`)
  - Profile photo persistence: Base64 to disk & Room SQLite (CursorWindow safe < 15KB)
  - Progress chart: weight/date on points & click details
  - Pull-to-refresh sync
  - Competition table: verify clean layout (no unwanted columns)
- **R2: Trainer Pro**
  - Telegram auth & account link
  - Trainer profile photo persistence
  - 2FA block on 1-click
  - Workout assignment to athletes
  - Sets terminology ("Подходы")
  - Pull-to-refresh sync
  - Zero outdated stubs, assembleRelease APK build
- **R3: Web Portal & Security**
  - IDOR, SQLi, XSS, and Rate Limiting on all API endpoints
  - Role selection at login and registration
  - Competition table & interactive workout chart (point clicks)
  - Fresh APK download links
- **R4: Telegram Bot**
  - Main menu verification: NO 'Сменить роль' button
  - Unified 6-digit 5-minute code (`🔑 Код входа`)
  - 1-click auth & 2FA OTP delivery
  - Account linking (`/link`)
- **Acceptance Criteria**:
  - `gradlew testDebugUnitTest` in `athlete-app` (100% PASS)
  - `gradlew testDebugUnitTest` in `trainer-app` (100% PASS)
  - `assembleRelease` produces valid release APKs for both apps
  - `npm test` in `web` passes 100% (security & 2FA suites)
  - Telegram bot menu has NO 'Сменить роль'
  - Web site serves fresh built APKs

## Execution Steps
1. **Phase 0: Survey**
   - Dispatch `explorer_athlete_5` (`teamwork_preview_explorer`): examine athlete-app source, tests, and build files.
   - Dispatch `explorer_trainer_5` (`teamwork_preview_explorer`): examine trainer-app source, tests, and build files.
   - Dispatch `explorer_web_bot_5` (`teamwork_preview_explorer`): examine web portal, bot scripts, tests, and APK serving.
2. **Phase 1: Remediation (if required)**
   - Dispatch dedicated `teamwork_preview_worker` instances to fix any gap found during survey.
3. **Phase 2: Full Verification Gate**
   - Dispatch `reviewer_mobile_5` (`teamwork_preview_reviewer`): run gradle tests, build APKs, inspect binaries.
   - Dispatch `reviewer_web_5` (`teamwork_preview_reviewer`): run npm test suites, verify bot and web UI.
   - Dispatch `challenger_ecosystem_5` (`teamwork_preview_challenger`): adversarial attack vectors and stress testing.
   - Dispatch `auditor_forensic_5` (`teamwork_preview_auditor`): forensic zero-mocks, no stubs, real implementation validation.
4. **Phase 3: Synthesis & Sentinel Notification**
   - Aggregate all verdicts into `GATE_STATUS.md`.
   - Update `PROJECT.md` and write `handoff.md`.
   - Notify Sentinel (`eee718a4-bf28-4d58-821b-1bf853250bf6`) with final victory evidence.
