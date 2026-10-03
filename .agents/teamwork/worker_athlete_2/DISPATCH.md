## 2026-10-03T18:04:10Z
You are Worker Athlete 2 (Athlete Pro User Directives & Zero-Mocks Refinement).
Your working directory is: F:\Projects\fitness-ecosystem-pro\.agents\teamwork\worker_athlete_2
Original user request file: F:\Projects\fitness-ecosystem-pro\.agents\teamwork\ORIGINAL_REQUEST.md
Project plan: F:\Projects\fitness-ecosystem-pro\PROJECT.md

WRITE OWNERSHIP: You exclusively own files in F:\Projects\fitness-ecosystem-pro\athlete-app/**. DO NOT edit files outside this directory.

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A forensic auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

CRITICAL USER DIRECTIVES TO IMPLEMENT:
1. Completely remove all mock participants in LeaderboardScreen.kt:
   - Remove "Максим Громов", "Елена Соколова", "Дмитрий Воронов", "Ольга Морозова".
   - In competitions, leaderboards must display ONLY real data: the current athlete (from their actual workout sessions and tonnage), plus any real athletes loaded from cloud sync.
   - If no other athletes exist, show only the current user.
   - If user enabled private mode and there are no other participants, show an elegant Empty State ("В состязаниях пока нет участников" / "Ваш профиль скрыт от других участников").
2. Replace QR code in AthleteSettingsScreen.kt with link & share buttons:
   - Remove the non-working `QrCodeView` component.
   - Add a clean card with the pairing link `https://fitnessapp.pro/pair?code=$cleanPin` and PIN display.
   - Add button "Скопировать ссылку для тренера": copies the link to clipboard with Toast ("Ссылка скопирована в буфер").
   - Add button "Отправить тренеру": launches Android Share Intent (`Intent.ACTION_SEND` with text e.g. "Код для привязки к тренеру: $cleanPin\nhttps://fitnessapp.pro/pair?code=$cleanPin").
3. Zero-Mocks Data Audit:
   - Verify that Athlete Pro Room database, migrations, and seeds contain no mock/test users or dummy records.
4. Build & Test:
   - Run `cmd /c "cd athlete-app && gradlew.bat testDebugUnitTest --no-daemon"`.
   - Run `cmd /c "cd athlete-app && gradlew.bat assembleRelease --no-daemon"`.
   - Overwrite `F:\Projects\fitness-ecosystem-pro\releases\athlete-pro-v1.0.5.apk` with the newly built signed release APK.

Write your report to `F:\Projects\fitness-ecosystem-pro\.agents\teamwork\worker_athlete_2\report.md` and handoff to `F:\Projects\fitness-ecosystem-pro\.agents\teamwork\worker_athlete_2\handoff.md`.
Then send a message to parent.
