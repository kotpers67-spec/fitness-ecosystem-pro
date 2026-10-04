## 2026-10-04T10:31:21Z
You are Worker Trainer M1 (worker_trainer_m1).
Working Directory: F:\Projects\fitness-ecosystem-pro\.agents\teamwork\worker_trainer_m1
Target Directory: F:\Projects\fitness-ecosystem-pro\trainer-app
Exclusive File Ownership: You own files strictly inside `F:\Projects\fitness-ecosystem-pro\trainer-app/**`. Do NOT touch any other directory.

MANDATORY FIRST STEP:
Read F:\Projects\fitness-ecosystem-pro\.agents\teamwork\ORIGINAL_REQUEST.md completely.
Also read:
- F:\Projects\fitness-ecosystem-pro\.agents\teamwork\orchestrator_4\PROJECT.md
- F:\Projects\fitness-ecosystem-pro\.agents\teamwork\survey_trainer_1\report.md

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

Tasks for Milestone 1:
1. In `WorkoutScreen.kt` (`trainer-app/app/src/main/java/com/trainerapp/pro/ui/screens/WorkoutScreen.kt`):
   - Add an Athlete Restrictions / Injuries banner/card (`activeClient?.notes` or restrictions). When an active client has restrictions or notes (e.g. "травма плеча", "ограничения"), display a clearly visible, beautifully styled warning/card with Swiss styling (warning accent, rounded card, icon/tag) so the coach sees it while programming workout exercises and sets. If no restrictions, display clean state or hide gracefully.
2. In `MainViewModel.kt` (`trainer-app/app/src/main/java/com/trainerapp/pro/ui/MainViewModel.kt`):
   - Enhance `verify2FaOtp` to support remote verification against backend `/api/auth/telegram/verify-otp` (or HTTP call) with graceful fallback to local validation if offline or server is unreachable.
3. In `MainViewModel.kt` & `TrainerAuthScreen.kt`:
   - Support remote approval check for 72h registration verification against server `/api/trainer/approval-status` or `/api/me`, preserving the 72h dialog and owner links to @SantiLA213 and @Spirit5449.
4. Run Unit Tests:
   Execute `.\gradlew.bat testDebugUnitTest` in `F:\Projects\fitness-ecosystem-pro\trainer-app`. All tests MUST PASS (100% PASS, 0 failures). If any fail, fix them cleanly.
5. Run Release Build:
   Execute `.\gradlew.bat assembleRelease` in `F:\Projects\fitness-ecosystem-pro\trainer-app`. Must compile successfully into release APK.
6. Write your detailed report to `F:\Projects\fitness-ecosystem-pro\.agents\teamwork\worker_trainer_m1\report.md` and handoff to `handoff.md`.
7. Send your completion message back to the orchestrator via `send_message`.
