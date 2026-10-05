# Changes Report — Athlete Pro (worker_athlete_m2_6)

## 1. 1-Step 6-Digit Code Auth Dialog Without Username
- **`AthleteRemoteAuthManager.kt`**:
  - Updated `verifyTelegramOtp(otp: String, username: String = "")` to accept code-only authentication.
  - Sends `{ "code": clean }` directly to `POST /api/auth/telegram/verify-otp`, including `"username"` only if non-blank.
  - Correctly extracts remote profile username, telegram username, avatar, and token upon success.
- **`AthleteAuthScreen.kt`**:
  - Replaced the 2-step OTP wizard (`tgCodeStep`, `tgUsernameInput`, `requestTelegramOtp`) with a direct 1-step dialog.
  - Dialog prompts for 6 numeric digits with `KeyboardType.NumberPassword` and 5-minute countdown chip.
  - Calls `remoteAuthManager.verifyTelegramOtp(tgCodeInput)` directly with fallback to local 2FA check.

## 2. 1-Click Telegram Login Button with Native Intent & Web Fallback
- **`AthleteAuthScreen.kt`**:
  - Updated 1-click Telegram login button to resolve native Android intent:
    `tg://resolve?domain=fitnessecosystemBOT&start=auth_<sessionId>`
  - Added seamless fallback to `https://t.me/fitnessecosystemBOT?start=auth_<sessionId>` if Telegram app is not installed.
  - Sets `FLAG_ACTIVITY_NEW_TASK` on intents.
  - If 2FA is required by the server during polling, cleanly switches to the 1-step 6-digit dialog.

## 3. Pairing PIN & Profile Synchronization
- **`AthleteRemoteAuthManager.kt`**:
  - Implemented `regeneratePairingPin(authToken: String): String?` calling backend `POST /api/athlete/regenerate-pin`.
- **`AthleteViewModel.kt`**:
  - Updated `regeneratePairingPin()` to fetch the new 5-minute dynamic PIN from the server when logged in, with offline random 6-digit fallback.
  - Added re-entrancy lock (`isRegeneratingPin`) to prevent multiple concurrent network calls.
  - Updated `startPinTicker()` to automatically invoke `regeneratePairingPin()` when the 5-minute timer expires (`remaining == 0`).
  - Preserved continuous two-way profile synchronization (`fullName`, `phone`, `restrictions`, `pairingPin`, `avatarBase64`) in `autoSync()` and `updateProfile()`.

## 4. Background In-App APK Updater Verification
- **`AthleteUpdateService.kt`**:
  - Verified background APK updater: streams directly via `HttpURLConnection` to cache and falls back to Android `DownloadManager` with `InstallReceiver`.
  - Zero browser redirects or external browser launches.

## 5. Test Suite & Verification
- **`AthletePinAnd2FaTest.kt`**:
  - Added `test1StepCodeAuthWithoutUsername`: asserts 6-digit code validation without username requirement.
  - Added `testTelegramNativeDeepLinkAndFallbackFormat`: asserts proper construction of `tg://` and `https://` URIs with `auth_<sessionId>`.
  - Added `testPinAutoRegenerationTriggerWhenExpired`: asserts auto-regeneration trigger when timer hits 0.
- Gradle Unit Tests: `.\gradlew.bat testDebugUnitTest` -> 42/42 tests PASSED (100%).
- Release APK Build: `.\gradlew.bat assembleRelease` -> BUILD SUCCESSFUL in 1m 20s.
  - Output: `athlete-app/app/build/outputs/apk/release/app-release.apk` (13,100,720 bytes).
