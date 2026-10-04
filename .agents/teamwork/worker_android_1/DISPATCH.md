## 2026-10-04T07:42:39Z
You are Worker 2 (Android Apps Specialist) for the fitness ecosystem project.
Working Directory: F:\Projects\fitness-ecosystem-pro\.agents\teamwork\worker_android_1\
Project Root: F:\Projects\fitness-ecosystem-pro\

MANDATORY FIRST STEPS:
1. Initialize your workspace: create BRIEFING.md and progress.md in your working directory.
2. Read F:\Projects\fitness-ecosystem-pro\.agents\teamwork\ORIGINAL_REQUEST.md (specifically Follow-up — 2026-10-04T07:30:38Z).
3. Read F:\Projects\fitness-ecosystem-pro\PROJECT.md.
4. Read explorer findings:
   - F:\Projects\fitness-ecosystem-pro\.agents\teamwork\survey_explorer_3\analysis.md

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

EXCLUSIVE WRITE OWNERSHIP:
You own F:\Projects\fitness-ecosystem-pro\athlete-app\ and F:\Projects\fitness-ecosystem-pro\trainer-app\ exclusively.
DO NOT touch the web/ directory.

TASK:
1. Athlete Pro (athlete-app):
   - In `AthleteViewModel.kt`:
     - Track `pinCreatedAt: Long` (in persistent preferences or entity).
     - Expose `pinSecondsRemaining: StateFlow<Int>` with a 1-second coroutine ticker (300 seconds down to 0).
     - When `pinSecondsRemaining` reaches 0, automatically call `regeneratePairingPin()`.
   - In `AthleteSettingsScreen.kt`:
     - Display a live countdown timer (`05:00` / `MM:SS`) next to or under the 6-digit PIN with visual indicator.
     - Add «Привязать Telegram» button that opens Telegram deep link or shows linking instruction.
     - Add 2FA toggle switch in profile settings.
   - In `AthleteAuthScreen.kt`:
     - Add project owner contact buttons (Telegram links https://t.me/SantiLA213 and https://t.me/Spirit5449).
     - If 2FA is active, show dialog requesting 6-digit OTP from Telegram.

2. Trainer Pro (trainer-app):
   - In `GoogleDriveSyncManager.kt` (`findAndPairAthlete`):
     - STRICT ERROR if code is not found in cloud (`Result.failure(IllegalArgumentException("Код не найден"))`). Completely remove the dummy client fallback!
     - STRICT ERROR if `System.currentTimeMillis() - timestamp > 5 * 60 * 1000L` -> remove expired entry from cloud and return `Result.failure(IllegalStateException("Срок действия кода истёк (действует 5 минут)"))`.
     - STRICT ERROR if `status == "PAIRED"` or `"USED"` -> return `Result.failure(IllegalStateException("Этот код уже был использован"))`.
     - On successful pairing: set `status = "PAIRED"`, update cloud registry so code cannot be reused.
   - In `SettingsScreen.kt`:
     - Add «Привязать Telegram» button and 2FA toggle switch.
   - In `TrainerAuthScreen.kt`:
     - Add project owner contact buttons (Telegram links https://t.me/SantiLA213 and https://t.me/Spirit5449).
     - If 2FA is active, show dialog requesting 6-digit OTP from Telegram.

3. Testing & Verification:
   - Run unit tests for both apps:
     `cmd /c "gradlew.bat testDebugUnitTest"` in `athlete-app`
     `cmd /c "gradlew.bat testDebugUnitTest"` in `trainer-app`
   - Verify `BUILD SUCCESSFUL` with 0 errors.

When finished, write a comprehensive handoff report to F:\Projects\fitness-ecosystem-pro\.agents\teamwork\worker_android_1\handoff.md documenting all modified files, test outputs, and verification results. Then send a completion message to parent.
