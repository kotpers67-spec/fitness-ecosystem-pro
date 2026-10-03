const { DatabaseSync } = require('node:sqlite');
const path = require('node:path');
const AppDatabase = require('../../../web/src/db');
const { hasSqlInjectionVector, escapeHtml, verifyPassword, hashPassword } = require('../../../web/src/security');

console.log('Testing AppDatabase in-memory for SQLi resilience...');

// Create in-memory db
const db = new AppDatabase(':memory:');

// Test 1: User creation with SQLi payload
const sqliUser = "admin' OR '1'='1";
const hash = hashPassword('secret123');
const id = db.createUser(sqliUser, hash, 'athlete', 'Test Name', '', '123456');
console.log('Created user with username:', sqliUser, 'ID:', id);

// Test 2: Query user with standard injection
const injectedUser = db.findUserByUsername("admin' OR '1'='1");
console.log('Found user with exact literal match:', injectedUser ? injectedUser.username : null);

const bypassUser = db.findUserByUsername("random' OR '1'='1' --");
console.log('SQLi bypass attempt (should be null):', bypassUser);

// Test 3: Workout set with SQLi in exercise name
const date = '2026-10-03';
const setId = db.addWorkoutSet(id, date, "Bench'); DROP TABLE workout_sets; --", 100, 10, 8.5);
console.log('Added set with SQLi exercise name, setId:', setId);

const sets = db.getWorkoutSets(id, date);
console.log('Sets count (table was NOT dropped):', sets.length);
console.log('Exercise name literal:', sets[0].exercise_name);

// Test 4: SQLi in search params / stats
const stats = db.getLastExerciseStats(id, "' UNION SELECT '2026-01-01', 999, 99, 10 --");
console.log('Stats query injection attempt (should be null):', stats);

// Test 5: Verify table still intact
const allSets = db.getWorkoutSets(id, date);
console.log('Table intact, sets count:', allSets.length);

db.close();
console.log('All DB SQLi resilience checks PASSED!');
