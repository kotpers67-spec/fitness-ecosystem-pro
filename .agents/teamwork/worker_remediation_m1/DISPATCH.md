## 2026-10-05T04:19:09Z
You are worker_remediation_m1. Your working directory is F:\Projects\fitness-ecosystem-pro\.agents\teamwork\worker_remediation_m1.

MANDATORY FIRST STEP:
Read F:\Projects\fitness-ecosystem-pro\.agents\teamwork\ORIGINAL_REQUEST.md (specifically section "## 2026-10-05T04:06:54Z") and F:\Projects\fitness-ecosystem-pro\PROJECT.md. Also read F:\Projects\fitness-ecosystem-pro\.agents\teamwork\explorer_survey_r2\handoff.md.

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

TASK OBJECTIVE:
Implement the remediation fixes for Requirement R2 across Web, Trainer App, and Athlete App, and verify with tests.

SPECIFIC IMPLEMENTATION WORK:
1. Web (`web/src/public/app.js`):
   - In `EXERCISE_CATALOG` (around line 2836-2870), there are currently 33 exercises.
   - Requirement R2 states: "Trainer exercise addition must feature a searchable list/catalog (35+ exercises by muscle group)".
   - Add 4 legitimate exercises across relevant categories (e.g. "Отжимания на брусьях" (Грудь), "Тяга Т-грифа" (Спина), "Выпады с гантелями" (Ноги), "Подъем штанги на бицепс" (Руки)) so that the total count is >= 35 (e.g., 37 items).
   - Ensure the search and category filters in `renderTrainerSearchResults` handle them seamlessly.

2. Android Trainer App (`trainer-app/app/src/main/java/com/trainerapp/pro/ui/screens/WorkoutScreen.kt`):
   - Fix collapse UI bug: inspect lines 370–410. When `isSetsCollapsed` is true, the `LazyColumn` containing the set rows was rendered outside the `if (!isSetsCollapsed)` block, so set rows remained visible! Ensure the set table (including the `LazyColumn` with set rows) is properly hidden when `isSetsCollapsed` is true, with an indicator or clean card state.
   - Implement 1-hour auto-collapse calculation: for completed exercises (where all sets are checked), auto-collapse them if 1 hour has elapsed since session creation/first exercise. Ensure the manual toggle button (`isSetsCollapsed = !isSetsCollapsed`) can expand and collapse on demand.

3. Android Athlete App (`athlete-app/app/src/main/java/com/athleteapp/pro/ui/screens/AthleteTodayScreen.kt`):
   - Implement 1-hour auto-collapse calculation: in `ExerciseGroupCard` (around line 371), for completed exercises (`isAllCompleted`), initialize `isCollapsed` to true if 1 hour has elapsed since session/exercise creation. Ensure manual toggle continues to work cleanly.

4. Test Verification:
   - Run `npm test` in `F:\Projects\fitness-ecosystem-pro\web` and verify 0 failures.
   - Run `.\gradlew.bat testDebugUnitTest` in `F:\Projects\fitness-ecosystem-pro\athlete-app` and verify 0 failures.
   - Run `.\gradlew.bat testDebugUnitTest` in `F:\Projects\fitness-ecosystem-pro\trainer-app` and verify 0 failures.

DELIVERABLES:
Write a comprehensive report to F:\Projects\fitness-ecosystem-pro\.agents\teamwork\worker_remediation_m1\changes.md and F:\Projects\fitness-ecosystem-pro\.agents\teamwork\worker_remediation_m1\handoff.md documenting all modified files, diffs, test outputs, and send a completion message to parent.
