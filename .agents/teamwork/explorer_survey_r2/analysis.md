# Survey & Gap Analysis: Requirement R2 (Exercise Catalog, Sorting & Auto-Collapse)

**Survey Date**: 2026-10-05  
**Investigator**: `explorer_survey_r2`  
**Target Codebases**:
1. Web Portal (`web/src/public/`, `web/src/`)
2. Android Trainer Pro (`trainer-app/`)
3. Android Athlete Pro (`athlete-app/`)

---

## 1. Executive Summary

Requirement R2 mandates three core capabilities across the ecosystem:
1. **Searchable Exercise Catalog (35+ exercises by muscle group)** without carousel limitations or daily caps for Trainer exercise addition.
2. **Sorting of Completed Exercises** (where all sets are checked) to the bottom of the list with pending exercises at the top.
3. **1-Hour Auto-Collapse of Completed Exercises** (1 hour after the session's first exercise was created), accompanied by a manual expand/collapse toggle.

### Compliance Scorecard

| Requirement Item | Web Portal | Android Trainer Pro | Android Athlete Pro |
|---|---|---|---|
| **Catalog Count (35+)** | ⚠️ **33 exercises** (needs +2) | ✅ **37 exercises** | N/A (Manual name entry) |
| **Catalog Search & Filter** | ✅ Real-time name + muscle | ✅ Real-time name + chip filter | N/A |
| **No Daily Caps / Carousel Limits** | ✅ Unlimited, dropdown list | ✅ Unlimited; vertical dialog | N/A |
| **Sorting Completed to Bottom** | ✅ `groups.sort()` fully working | ✅ Incomplete left, completed right | ✅ Incomplete top, completed bottom |
| **1-Hour Auto-Collapse** | ✅ Calculated via `created_at` | ❌ **Missing** (defaults `false`) | ❌ **Missing** (defaults `false`) |
| **Manual Expand/Collapse Toggle** | ✅ `collapse-toggle-btn` works | ⚠️ **Broken UI** (only hides headers) | ✅ Hides sets properly |

---

## 2. Deep Dive: Exercise Catalog & Search

### 2.1 Web Implementation
- **Source Files**:
  - `web/src/public/app.js`: lines 2835–2919
  - `web/src/public/index.html`: lines 668–685
  - `web/src/server.js`: lines 1939–1980
- **Catalog Definition**:
  `EXERCISE_CATALOG` in `app.js` (lines 2836–2870) contains **33 exercises**:
  - Грудь (5): Жим штанги лёжа, Жим гантелей на наклонной скамье, Жим гантелей лёжа, Разведение гантелей лёжа, Сведение рук в кроссовере.
  - Грудь / Трицепс (1): Отжимания на брусьях.
  - Ноги (5): Приседания со штангой, Жим ногами в тренажёре, Выпады с гантелями, Сгибания ног в тренажёре, Разгибания ног в тренажёре.
  - Ноги / Спина (1): Румынская тяга со штангой.
  - Икры (1): Подъёмы на носки стоя.
  - Спина (7): Становая тяга, Подтягивания широким хватом, Тяга верхнего блока к груди, Тяга штанги в наклоне, Тяга горизонтального блока, Тяга гантели в наклоне, Гиперэкстензия.
  - Плечи (4): Армейский жим стоя, Жим гантелей сидя, Махи гантелями через стороны, Махи в наклоне на заднюю дельту.
  - Руки (2): Подъём штанги на бицепс, Молотковые сгибания с гантелями.
  - Трицепс (2): Французский жим со штангой, Разгибания рук на блоке.
  - Пресс (3): Скручивания на пресс, Подъём ног в висе, Планка.
  - Кардио (2): Кардио: Беговая дорожка, Кардио: Велотренажёр.
  *Gap*: 33 items < 35 required items. Needs at least 2 additional exercises (e.g., "Жим узким хватом" and "Тяга к подбородку") to meet 35+.
- **Search Interaction**:
  - Input: `<input id="trainer-input-exercise" placeholder="🔍 Название или поиск упражнения...">`
  - Dropdown: `<div id="trainer-exercise-search-results">`
  - Real-time event listeners on `focus` and `input` (`app.js` lines 2906–2913).
  - Matches filter by query matching item name or muscle category.
  - Selecting an item fills the input and immediately focuses `#trainer-input-weight`.
- **Limits / Caps**:
  - No carousel limitations.
  - No daily exercise cap in client or server.

### 2.2 Android Trainer App (`trainer-app`)
- **Source Files**:
  - `trainer-app/app/src/main/java/com/trainerapp/pro/data/local/TrainerDatabase.kt`: lines 86–135
  - `trainer-app/app/src/main/java/com/trainerapp/pro/ui/screens/WorkoutScreen.kt`: lines 686–799
  - `trainer-app/app/src/main/java/com/trainerapp/pro/ui/MainViewModel.kt`: lines 564–588
- **Catalog Definition**:
  - Room SQLite `exercises` table pre-seeded with **37 exercises** across 6 muscle groups:
    - Грудь: 6
    - Спина: 7
    - Ноги: 8
    - Плечи: 6
    - Руки: 6
    - Пресс/Кор: 4
  - Meets requirement (37 >= 35).
- **Search & Filter UI**:
  - Dialog `AddExerciseToSessionDialog` (lines 686–799 in `WorkoutScreen.kt`):
    - `OutlinedTextField` search input with search icon.
    - `LazyRow` filter chips: `listOf("Все", "Грудь", "Спина", "Ноги", "Плечи", "Руки", "Пресс/Кор")`.
    - `LazyColumn` of filtered exercises with live filter expression:
      ```kotlin
      val filtered = exercises.filter {
          (selectedMuscleGroup == "Все" || it.muscleGroup == selectedMuscleGroup) &&
          (it.name.contains(searchQuery, ignoreCase = true))
      }
      ```
    - Each item has exercise name, muscle group, historical stats button, and add button.
- **Limits / Caps**:
  - `addExerciseToSession(exerciseId)` dynamically computes `nextOrder = (currentOrders.maxOrNull() ?: 0) + 1`. No daily cap.
  - Dialog is a vertical list, not a carousel. (Note: in the main workout screen, session exercises are placed in a horizontal `LazyRow` of cards).

### 2.3 Android Athlete App (`athlete-app`)
- **Source Files**:
  - `athlete-app/app/src/main/java/com/athleteapp/pro/ui/screens/AthleteTodayScreen.kt`: lines 283–356
  - `athlete-app/app/src/main/java/com/athleteapp/pro/data/local/AthleteDatabase.kt`: lines 145–150
- **Catalog State**:
  - Athlete DB only pre-seeds 5 basic exercises.
  - Adding self-exercise (`+ Добавить упражнение`) uses manual text entry (`newExName`), muscle group chip selection, and default weight/reps.

---

## 3. Deep Dive: Sorting Completed Exercises to Bottom

### 3.1 Web Implementation
- **Source File**: `web/src/public/app.js`
- **Athlete Matrix (`renderAthleteWorkoutMatrix`, lines 723–735)**:
  ```javascript
  const groups = Object.entries(grouped).map(([exName, sets]) => {
    const isCompleted = sets.length > 0 && sets.every(s => Boolean(s.is_completed));
    const earliestId = Math.min(...sets.map(s => s.id || 0));
    return { exName, sets, isCompleted, earliestId };
  });

  // Incomplete exercises go to TOP, completed go to BOTTOM
  groups.sort((a, b) => {
    if (a.isCompleted !== b.isCompleted) {
      return a.isCompleted ? 1 : -1;
    }
    return a.earliestId - b.earliestId;
  });
  ```
- **Trainer Matrix (`renderTrainerWorkoutMatrix`, lines 1652–1664)**:
  Identical logic sorting `state.trainerSets`.
- **Verdict**: Fully verified and working as expected.

### 3.2 Android Trainer App (`trainer-app`)
- **Source File**: `trainer-app/app/src/main/java/com/trainerapp/pro/ui/screens/WorkoutScreen.kt` (lines 53–64):
  ```kotlin
  val sessionExercises = remember(currentSets, exercises) {
      val list = currentSets.groupBy { it.exerciseOrder }.toSortedMap().mapNotNull { (order, sets) ->
          val exId = sets.firstOrNull()?.exerciseId ?: return@mapNotNull null
          val exercise = exercises.find { it.id == exId } ?: return@mapNotNull null
          Triple(order, exercise, sets)
      }
      list.sortedWith(
          compareBy<Triple<Int, ExerciseEntity, List<WorkoutSetEntity>>> { (_, _, sets) ->
              if (sets.isNotEmpty() && sets.all { it.isCompleted }) 1 else 0
          }.thenBy { it.first }
      )
  }
  ```
- **Verdict**: Incomplete exercises have sort key `0` and completed have key `1`. In the horizontal `LazyRow` (line 218), incomplete exercises appear on the left (first) and completed exercises on the right (last).

### 3.3 Android Athlete App (`athlete-app`)
- **Source File**: `athlete-app/app/src/main/java/com/athleteapp/pro/ui/screens/AthleteTodayScreen.kt` (lines 60–67):
  ```kotlin
  val sortedExerciseGroups = remember(sets) {
      sets.groupBy { it.exerciseId }.entries.sortedWith(
          compareBy<Map.Entry<Long, List<MyWorkoutSetEntity>>> { (_, exerciseSets) ->
              if (exerciseSets.isNotEmpty() && exerciseSets.all { it.isCompleted }) 1 else 0
          }.thenBy { it.value.firstOrNull()?.setNumber ?: 0 }
      )
  }
  ```
- Rendered in a vertical `LazyColumn` (line 258).
- **Verdict**: Fully verified. Pending exercises at top, completed exercises at bottom.

---

## 4. Deep Dive: 1-Hour Auto-Collapse & Manual Toggle

### 4.1 Web Implementation
- **Source File**: `web/src/public/app.js` (lines 685–787, 1642–1715) and `styles.css` (lines 545–565).
- **Elapsed Time Calculation**:
  ```javascript
  function getEarliestSessionTimestamp(sets) {
    let earliestMs = null;
    sets.forEach(s => {
      if (s.created_at) {
        let dStr = s.created_at;
        if (!dStr.includes('T')) dStr = dStr.replace(' ', 'T') + 'Z';
        const t = new Date(dStr).getTime();
        if (!isNaN(t) && (earliestMs === null || t < earliestMs)) {
          earliestMs = t;
        }
      }
    });
    return earliestMs;
  }
  const earliestMs = getEarliestSessionTimestamp(state.athleteSets);
  const isAfterOneHour = earliestMs ? (Date.now() - earliestMs >= 60 * 60 * 1000) : false;
  const shouldCollapse = isCompleted && isAfterOneHour;
  ```
- **CSS Collapse Rule**:
  ```css
  .exercise-group-card.collapsed .sets-table {
    display: none !important;
  }
  ```
- **Manual Toggle**:
  `<button type="button" class="collapse-toggle-btn">` toggles the `.collapsed` class on the card.
- **Verdict**: Fully compliant on Web.

### 4.2 Android Trainer App (`trainer-app`)
- **Source File**: `trainer-app/app/src/main/java/com/trainerapp/pro/ui/screens/WorkoutScreen.kt` (lines 69, 354–402)
- **Defects Identified**:
  1. **UI Bug — Collapse button does NOT collapse sets**:
     - At line 372:
       ```kotlin
       if (!isSetsCollapsed) {
           // Only column headers (ПОДХОД, ВЕС, ПОВТОРЫ) are hidden!
       }
       LazyColumn(...) { // Renders items(sets) regardless of isSetsCollapsed!
           items(sets) { set -> SetRowItem(...) }
       }
       ```
     - Result: Clicking "▼ Свернуть" hides only column labels; set rows remain fully visible.
  2. **Missing 1-Hour Auto-Collapse**:
     - `isSetsCollapsed` defaults to `remember(...) { mutableStateOf(false) }`.
     - Zero code checks whether 1 hour has passed since the first exercise was created.
  3. **Data Model Gap**:
     - `WorkoutSessionEntity` and `WorkoutSetEntity` only store `date: String` ("YYYY-MM-DD"). No `createdAt` timestamp is persisted.

### 4.3 Android Athlete App (`athlete-app`)
- **Source File**: `athlete-app/app/src/main/java/com/athleteapp/pro/ui/screens/AthleteTodayScreen.kt` (lines 370–456)
- **Manual Toggle**:
  - `if (!isCollapsed)` correctly wraps set rendering (`sets.forEach { set -> AthleteSetRow(...) }`).
- **Defects Identified**:
  1. **Missing 1-Hour Auto-Collapse**:
     - `isCollapsed` defaults to `mutableStateOf(false)`.
     - No logic checking if 1 hour has elapsed since session creation.
  2. **Data Model Gap**:
     - `MyWorkoutSessionEntity` only has `date: String`, with no creation timestamp.

---

## 5. Precise Actionable Remediation Plan

### Remediation Item 1: Web Catalog Extension (33 → 37 exercises)
In `web/src/public/app.js` (lines 2836–2870), add 4 exercises to `EXERCISE_CATALOG`:
- `{ name: 'Жим узким хватом', category: 'Трицепс / Грудь' }`
- `{ name: 'Тяга штанги к подбородку', category: 'Плечи' }`
- `{ name: 'Сгибания на скамье Скотта', category: 'Руки' }`
- `{ name: 'Ягодичный мостик', category: 'Ноги' }`
This brings the catalog to 37 exercises across all muscle groups.

### Remediation Item 2: Fix Trainer App Sets Collapse UI Bug
In `trainer-app/app/src/main/java/com/trainerapp/pro/ui/screens/WorkoutScreen.kt`:
Wrap the entire sets table and rows (`LazyColumn` or `items(sets)`) inside `if (!isSetsCollapsed)`.

### Remediation Item 3: Implement 1-Hour Auto-Collapse on Android
For both `trainer-app` and `athlete-app`:
1. Use session date comparison and/or session start time:
   - For past dates (`sessionDate < currentDate`): auto-collapse completed exercises immediately (`isOlderThanOneHour = true`).
   - For today's date: track session start time in memory or in database.
   - Initial state:
     ```kotlin
     val shouldAutoCollapse = isAllCompleted && isOlderThanOneHour
     var isCollapsed by remember(exerciseId, isAllCompleted, shouldAutoCollapse) {
         mutableStateOf(shouldAutoCollapse)
     }
     ```
2. Ensure manual toggle (`▶ Развернуть` / `▼ Свернуть`) remains functional regardless of initial auto-collapse state.
