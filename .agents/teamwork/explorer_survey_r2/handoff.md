# Handoff Report: Requirement R2 Investigation (Exercise Catalog, Sorting & Auto-Collapse)

## 1. Observation
1. **Web Exercise Catalog**:
   - In `web/src/public/app.js` lines 2836–2870, `EXERCISE_CATALOG` defines exactly 33 exercises across 8 muscle groups/categories.
   - In `web/src/public/app.js` lines 2873–2919, `renderTrainerSearchResults(query)` filters `EXERCISE_CATALOG` in real-time matching `name` and `category`.
   - In `web/src/public/index.html` lines 668–685, `#trainer-input-exercise` and `#trainer-exercise-search-results` provide the autocomplete dropdown.
2. **Web Sorting & 1-Hour Auto-Collapse**:
   - In `web/src/public/app.js` lines 712–741 (athlete) and lines 1642–1671 (trainer):
     `getEarliestSessionTimestamp(sets)` calculates `earliestMs` from `s.created_at`.
     `const isAfterOneHour = earliestMs ? (Date.now() - earliestMs >= 60 * 60 * 1000) : false;`
     `groups.sort((a, b) => (a.isCompleted !== b.isCompleted ? (a.isCompleted ? 1 : -1) : a.earliestId - b.earliestId));`
     `const shouldCollapse = isCompleted && isAfterOneHour;`
     `card.className = "exercise-group-card " + (isCompleted ? "completed-card " : "") + (shouldCollapse ? "collapsed" : "");`
   - In `web/src/public/app.js` lines 771–787 & 1698–1714: manual `<button class="collapse-toggle-btn">` toggles the `.collapsed` class on the card.
   - In `web/src/public/styles.css` line 545: `.exercise-group-card.collapsed .sets-table { display: none !important; }`.
3. **Android Trainer App (`trainer-app`) Catalog & Sorting**:
   - In `trainer-app/app/src/main/java/com/trainerapp/pro/data/local/TrainerDatabase.kt` lines 86–135, Room seeds 37 exercises into `ExerciseEntity` across 6 muscle groups: Грудь (6), Спина (7), Ноги (8), Плечи (6), Руки (6), Пресс/Кор (4).
   - In `trainer-app/app/src/main/java/com/trainerapp/pro/ui/screens/WorkoutScreen.kt` lines 686–799, `AddExerciseToSessionDialog` features an `OutlinedTextField` search bar, `LazyRow` filter chips for muscle groups, and real-time filtering in a `LazyColumn`.
   - In `trainer-app/app/src/main/java/com/trainerapp/pro/ui/screens/WorkoutScreen.kt` lines 59–63:
     ```kotlin
     list.sortedWith(
         compareBy<Triple<Int, ExerciseEntity, List<WorkoutSetEntity>>> { (_, _, sets) ->
             if (sets.isNotEmpty() && sets.all { it.isCompleted }) 1 else 0
         }.thenBy { it.first }
     )
     ```
     Sorts incomplete exercises first (0) and completed exercises last (1) in the horizontal `LazyRow`.
4. **Android Trainer App (`trainer-app`) Collapse Defect**:
   - In `trainer-app/app/src/main/java/com/trainerapp/pro/ui/screens/WorkoutScreen.kt` line 69:
     `var isSetsCollapsed by remember(activeExerciseTriple?.first) { mutableStateOf(false) }` (unconditionally `false`, no 1-hour calculation).
   - In `WorkoutScreen.kt` lines 372–395:
     ```kotlin
     if (!isSetsCollapsed) {
         Spacer(Modifier.height(10.dp))
         // Table Column Headers (ПОДХОД, ВЕС, ПОВТОРЫ, ГОТОВО)
         Row(...) { ... }
         Divider(...)
     }
     LazyColumn(modifier = Modifier.weight(1f), ...) {
         items(sets) { set -> SetRowItem(...) }
     }
     ```
     `isSetsCollapsed` only wraps the header labels. The `LazyColumn` containing the set rows is rendered outside `if (!isSetsCollapsed)` and remains visible when collapsed.
5. **Android Athlete App (`athlete-app`) Sorting & Collapse**:
   - In `athlete-app/app/src/main/java/com/athleteapp/pro/ui/screens/AthleteTodayScreen.kt` lines 60–67:
     `sortedExerciseGroups` sorts incomplete sets first (0) and completed sets last (1). Rendered in a vertical `LazyColumn`.
   - In `AthleteTodayScreen.kt` line 371:
     `var isCollapsed by remember(exerciseName, isAllCompleted) { mutableStateOf(false) }` (defaults to `false`, no 1-hour calculation).
   - In `AthleteTodayScreen.kt` lines 414–456:
     Clicking manual toggle toggles `isCollapsed`. `if (!isCollapsed)` properly hides sets.

---

## 2. Logic Chain
1. Requirement R2 states: "Trainer exercise addition must feature a searchable list/catalog (35+ exercises by muscle group) without carousel limitations or daily exercise caps."
   - Observation 1 shows Web has real-time search without carousel or daily caps, but its catalog contains 33 items. Because 33 < 35, Web catalog requires 2+ additional exercises.
   - Observation 3 shows Android Trainer App pre-populates 37 exercises across 6 muscle groups and provides real-time search with muscle group chips. 37 >= 35, satisfying this clause.
2. Requirement R2 states: "Completed exercises (where all sets are checked) must sort to the bottom of the list with pending exercises at the top."
   - Observation 2 shows Web implements this via `groups.sort()` placing `isCompleted` items at the bottom.
   - Observation 3 shows Trainer App sorts `sessionExercises` with incomplete at key 0 and completed at key 1.
   - Observation 5 shows Athlete App sorts `sortedExerciseGroups` with incomplete at key 0 and completed at key 1.
   - Conclusion: Sorting is implemented across all 3 platforms.
3. Requirement R2 states: "Completed exercises must auto-collapse 1 hour after the first exercise of the session was created, with a manual expand/collapse toggle."
   - Observation 2 shows Web computes `Date.now() - earliestMs >= 3600000` and sets `.collapsed` with a working toggle button.
   - Observations 4 and 5 show Android Trainer App and Athlete App initialize collapse state to `mutableStateOf(false)` unconditionally, lacking any 1-hour elapsed check.
   - Observation 4 shows Trainer App has a critical UI bug where clicking the collapse button hides only the table header, leaving set rows rendered.

---

## 3. Caveats
- Android Room entities (`WorkoutSessionEntity`, `MyWorkoutSessionEntity`) store date as "YYYY-MM-DD" and do not currently have a timestamp column (`createdAt`). Auto-collapse for past dates (`date < today`) can be determined from the date string, but for today's sessions, tracking elapsed time requires either a Room column or in-memory session start time.
- Athlete self-workout exercise addition in `athlete-app` uses text input rather than a 35+ exercise picker, but R2 specifically states "Trainer exercise addition must feature a searchable list/catalog".

---

## 4. Conclusion
1. **Web Portal**:
   - Sorting and 1-hour auto-collapse + manual toggle are fully functional and compliant.
   - Minor defect: `EXERCISE_CATALOG` has 33 exercises, requiring 2+ more to reach 35+.
2. **Android Trainer App**:
   - Catalog has 37 exercises with real-time search and muscle filter chips (compliant).
   - Sorting completed exercises to the tail is implemented.
   - Critical Defect: 1-hour auto-collapse is missing.
   - Critical Defect: Manual toggle button does not hide set rows (only hides table headers).
3. **Android Athlete App**:
   - Sorting completed exercises to the bottom is implemented.
   - Manual toggle hides sets correctly.
   - Defect: 1-hour auto-collapse is missing.

---

## 5. Verification Method
1. **Web Catalog Count Verification**:
   - Command:
     ```powershell
     node -e "const fs = require('fs'); const content = fs.readFileSync('web/src/public/app.js', 'utf8'); const match = content.match(/const EXERCISE_CATALOG = \[([\s\S]*?)\];/); const items = match[1].split('{ name:').length - 1; console.log('Catalog Count:', items);"
     ```
   - Current output: 33. Must be >= 35.
2. **Web Tests Verification**:
   - Command in `F:\Projects\fitness-ecosystem-pro\web`: `npm test`
3. **Android Trainer App UI Collapse Inspection**:
   - Inspect `trainer-app/app/src/main/java/com/trainerapp/pro/ui/screens/WorkoutScreen.kt` lines 372–402. Confirm whether `LazyColumn` is enclosed within `if (!isSetsCollapsed)`.
4. **Android Compilation & Unit Test Verification**:
   - Command in `F:\Projects\fitness-ecosystem-pro\trainer-app`: `./gradlew.bat testDebugUnitTest`
   - Command in `F:\Projects\fitness-ecosystem-pro\athlete-app`: `./gradlew.bat testDebugUnitTest`
