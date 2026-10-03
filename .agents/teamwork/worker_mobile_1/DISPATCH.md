# Task Assignment: Mobile Worker (Milestone 1)

**Working Directory**: F:\Projects\fitness-ecosystem-pro\.agents\teamwork\worker_mobile_1
**Original Request**: F:\Projects\fitness-ecosystem-pro\.agents\teamwork\ORIGINAL_REQUEST.md
**Project Plan**: F:\Projects\fitness-ecosystem-pro\PROJECT.md
**Mobile Explorer Report**: F:\Projects\fitness-ecosystem-pro\.agents\teamwork\explorer_survey_mobile_1\handoff.md

## Exclusive File Ownership
You exclusively own and may edit:
- `trainer-app/app/src/main/java/com/trainerapp/pro/ui/screens/HomeScreen.kt`
- `trainer-app/app/src/main/java/com/trainerapp/pro/util/QrCodeScannerHelper.kt`
- `trainer-app/app/src/main/java/com/trainerapp/pro/ui/MainViewModel.kt`
- `athlete-app/app/src/main/java/com/athleteapp/pro/ui/AthleteViewModel.kt`
- `trainer-app/app/src/main/java/com/trainerapp/pro/ui/components/CommonComponents.kt`
- `athlete-app/app/src/main/java/com/athleteapp/pro/ui/components/CommonComponents.kt`

Do NOT touch `web/**` or any other files outside mobile apps.

## Mandatory Integrity Warning
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

## Objective & Implementation Steps
Read `F:\Projects\fitness-ecosystem-pro\.agents\teamwork\explorer_survey_mobile_1\handoff.md` and implement the 4 verified fixes:
1. **Camera Permission in Trainer Pro**:
   In `HomeScreen.kt`, add `rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())`. Check `ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)` before launching `cameraLauncher.launch(null)`. If permission not granted, launch permission request.
2. **QrCodeScannerHelper OOM Prevention & Downscaling**:
   In `QrCodeScannerHelper.kt`, downscale bitmaps to max 800px before allocating `IntArray(width * height)`. In `decodeFromUri`, use `BitmapFactory.Options` with `inJustDecodeBounds` and `inSampleSize` to prevent reading full 12+ MP bitmaps. Catch `Throwable` (to catch `OutOfMemoryError` and all errors).
3. **CursorWindow Avatar Optimization**:
   In `AthleteViewModel.kt` and `MainViewModel.kt`, scale avatars to 128x128, compress at JPEG 75% in a loop ensuring byte size < 15 KB (15 * 1024 bytes). In `CommonComponents.kt` for both apps, catch `Throwable` during Base64 decoding / Bitmap decoding.
4. **MainViewModel Safe Flow Init**:
   In `MainViewModel.kt`, replace `clients.filter { it.isNotEmpty() }.first()` with `val initialClients = clients.first()`. Verify loop runs safely on empty database without clients.

## Verification
You MUST run the following commands and report exact output:
1. `cd F:\Projects\fitness-ecosystem-pro\trainer-app; .\gradlew.bat testDebugUnitTest`
2. `cd F:\Projects\fitness-ecosystem-pro\athlete-app; .\gradlew.bat testDebugUnitTest`
3. `cd F:\Projects\fitness-ecosystem-pro\trainer-app; .\gradlew.bat assembleRelease`
4. `cd F:\Projects\fitness-ecosystem-pro\athlete-app; .\gradlew.bat assembleRelease`

Record full details and verification results in `F:\Projects\fitness-ecosystem-pro\.agents\teamwork\worker_mobile_1\handoff.md` and send a message when done.


## 2026-10-03T19:20:41Z
You are the Mobile Worker for Milestone 1.
Working Directory: F:\Projects\fitness-ecosystem-pro\.agents\teamwork\worker_mobile_1
Original Request: F:\Projects\fitness-ecosystem-pro\.agents\teamwork\ORIGINAL_REQUEST.md
Project Plan: F:\Projects\fitness-ecosystem-pro\PROJECT.md
Dispatch Instructions: F:\Projects\fitness-ecosystem-pro\.agents\teamwork\worker_mobile_1\DISPATCH.md
Explorer Report: F:\Projects\fitness-ecosystem-pro\.agents\teamwork\explorer_survey_mobile_1\handoff.md

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

Follow the instructions in DISPATCH.md and the drop-in solutions in explorer_survey_mobile_1/handoff.md.
Modify only the mobile files you own:
- HomeScreen.kt: add runtime CAMERA permission request before launching camera.
- QrCodeScannerHelper.kt: downscale images to max 800px, catch Throwable/OOM.
- AthleteViewModel.kt & MainViewModel.kt: scale avatars to 128x128, JPEG 75%, byte size < 15 KB (15360 bytes).
- CommonComponents.kt (both apps): catch Throwable on avatar decoding.
- MainViewModel.kt: replace blocking Flow filter with `val initialClients = clients.first()` for safe launch on empty DB.

Run the unit tests and assembleRelease for both apps using gradlew.bat:
1. cd F:\Projects\fitness-ecosystem-pro\trainer-app; .\gradlew.bat testDebugUnitTest
2. cd F:\Projects\fitness-ecosystem-pro\athlete-app; .\gradlew.bat testDebugUnitTest
3. cd F:\Projects\fitness-ecosystem-pro\trainer-app; .\gradlew.bat assembleRelease
4. cd F:\Projects\fitness-ecosystem-pro\athlete-app; .\gradlew.bat assembleRelease

Write your full handoff report to F:\Projects\fitness-ecosystem-pro\.agents\teamwork\worker_mobile_1\handoff.md with all changes and test outputs. Send a completion message when done.
