/**
 * Test Suite: Bidirectional Anthropometry Cloud Sync & Leaderboard Formula Parity
 * Verifies Milestone 2 CloudSync and Database improvements.
 */

const { describe, it, before, after } = require('node:test');
const assert = require('node:assert/strict');
const AppDatabase = require('../src/db');
const { CloudSyncService } = require('../src/cloudSync');

describe('Milestone 2 - Anthropometry Sync & Leaderboard Parity', () => {
  let db;
  let testUserId;
  let testClientUuid = 'test-athlete-uuid-' + Date.now();
  let syncService;

  before(() => {
    // Isolated in-memory database
    db = new AppDatabase(':memory:');
    syncService = new CloudSyncService(db);

    // Create test athlete
    testUserId = db.createUser(
      'athlete_sync_test_' + Date.now(),
      'hash123',
      'athlete',
      'Тестовый Атлет',
      '+79991112233'
    );

    // Set client UUID
    db.db.prepare('UPDATE users SET client_uuid = ? WHERE id = ?').run(testClientUuid, testUserId);
  });

  after(() => {
    db.close();
  });

  it('1. Persists anthropometry into local database and retrieves history', () => {
    const id1 = db.addAnthropometry(testUserId, 80.5, '2026-10-01', 105, 85, 40);
    const id2 = db.addAnthropometry(testUserId, 80.0, '2026-10-02', 105, 84, 40);
    assert.ok(id1 > 0);
    assert.ok(id2 > 0);

    const history = db.getAnthropometryHistory(testUserId);
    assert.equal(history.length, 2);
    assert.equal(history[0].date, '2026-10-01');
    assert.equal(Number(history[0].weight_kg), 80.5);
    assert.equal(Number(history[0].chest_cm), 105);
    assert.equal(Number(history[0].waist_cm), 85);
    assert.equal(Number(history[0].biceps_cm), 40);
  });

  it('2. pushAssignedWorkouts populates AthleteSyncPayload.anthropometry from db', async () => {
    let capturedRoot = null;
    syncService.pushCloudData = async (root) => {
      capturedRoot = root;
      return true;
    };
    syncService.fetchCloudData = async () => ({ clients: {}, pairing: {}, updates: {} });

    await syncService.pushAssignedWorkouts(
      testClientUuid,
      'Тестовый Атлет',
      [{ date: '2026-10-04', notes: 'Leg day', completed: false, isSelfWorkoutAllowed: true, exercises: [] }],
      testUserId,
      db
    );

    assert.ok(capturedRoot);
    const client = capturedRoot.clients[testClientUuid];
    assert.ok(client);
    assert.ok(Array.isArray(client.anthropometry));
    assert.equal(client.anthropometry.length, 2);
    assert.equal(client.anthropometry[0].date, '2026-10-01');
    assert.equal(client.anthropometry[0].weightKg, 80.5);
    assert.equal(client.anthropometry[0].chestCm, 105);
    assert.equal(client.anthropometry[0].waistCm, 85);
    assert.equal(client.anthropometry[0].bicepsCm, 40);
  });

  it('3. syncWorkoutSessionToCloud populates AthleteSyncPayload.anthropometry from db', async () => {
    let capturedRoot = null;
    syncService.pushCloudData = async (root) => {
      capturedRoot = root;
      return true;
    };
    syncService.fetchCloudData = async () => ({ clients: {}, pairing: {}, updates: {} });

    await syncService.syncWorkoutSessionToCloud(
      testClientUuid,
      'Тестовый Атлет',
      '2026-10-04',
      { notes: 'Chest day', is_self_workout_allowed: 1 },
      [{ exercise_name: 'Жим лежа', weight_kg: 90, reps: 10, is_completed: 1, rpe: 8.5 }],
      testUserId,
      db
    );

    assert.ok(capturedRoot);
    const client = capturedRoot.clients[testClientUuid];
    assert.ok(client);
    assert.ok(Array.isArray(client.anthropometry));
    assert.equal(client.anthropometry.length, 2);
    assert.equal(client.anthropometry[1].date, '2026-10-02');
    assert.equal(client.anthropometry[1].weightKg, 80.0);
  });

  it('4. syncCloudWorkoutsToLocal parses clientData.anthropometry and inserts new rows without duplicates', async () => {
    const cloudPayload = {
      clientUuid: testClientUuid,
      clientName: 'Тестовый Атлет',
      assignedWorkouts: [],
      anthropometry: [
        { date: '2026-10-01', weightKg: 80.5, chestCm: 105, waistCm: 85, bicepsCm: 40 }, // Already exists
        { date: '2026-10-03', weightKg: 79.5, chestCm: 104, waistCm: 83, bicepsCm: 39.5 } // New measurement
      ]
    };

    const res1 = await syncService.syncCloudWorkoutsToLocal(cloudPayload, testUserId, db);
    assert.equal(res1.syncedAnthropometry, 1, 'Should have inserted exactly 1 new row');

    const history = db.getAnthropometryHistory(testUserId);
    assert.equal(history.length, 3, 'Total history should now be 3 rows');
    assert.equal(history[2].date, '2026-10-03');
    assert.equal(Number(history[2].weight_kg), 79.5);

    // Run again with identical payload: must be idempotent (0 new rows)
    const res2 = await syncService.syncCloudWorkoutsToLocal(cloudPayload, testUserId, db);
    assert.equal(res2.syncedAnthropometry, 0, 'Must not duplicate existing measurements');
    assert.equal(db.getAnthropometryHistory(testUserId).length, 3);
  });

  it('5. Aligns Leaderboard points formula in CloudSync to workoutsCount * 10 + floor(tonnage / 100)', async () => {
    syncService.fetchCloudData = async () => ({
      clients: {
        'cloud-athlete-1': {
          clientName: 'Александр Облако',
          assignedWorkouts: [
            {
              completed: true,
              exercises: [
                {
                  sets: [
                    { isCompleted: true, actualWeightKg: 100, actualReps: 10 }, // 1000 kg
                    { isCompleted: true, actualWeightKg: 120, actualReps: 5 }   // 600 kg
                  ]
                }
              ]
            },
            {
              completed: true,
              exercises: [
                {
                  sets: [
                    { isCompleted: true, actualWeightKg: 95, actualReps: 10 }  // 950 kg
                  ]
                }
              ]
            }
          ]
        }
      }
    });

    // 2 workouts, tonnage = 1000 + 600 + 950 = 2550 kg
    // Points = 2 * 10 + Math.floor(2550 / 100) = 20 + 25 = 45 points
    const leaderboard = await syncService.getCombinedLeaderboard([]);
    assert.equal(leaderboard.length, 1);
    const athlete = leaderboard[0];
    assert.equal(athlete.workoutsCount, 2);
    assert.equal(athlete.totalTonnage, 2550);
    assert.equal(athlete.points, 45, 'Points formula must be workoutsCount * 10 + Math.floor(tonnage / 100)');
  });

  it('6. Aligns Leaderboard points formula in db.js SQL query', () => {
    // Create an athlete and sessions/sets
    const session1 = db.getOrCreateSession(testUserId, '2026-10-01');
    db.addWorkoutSet(session1.id, 'Приседания', 100, 10, 8.0, 1); // 1000 kg
    db.addWorkoutSet(session1.id, 'Жим ногами', 150, 10, 8.0, 1); // 1500 kg

    const session2 = db.getOrCreateSession(testUserId, '2026-10-02');
    db.addWorkoutSet(session2.id, 'Становая тяга', 120, 8, 8.0, 1); // 960 kg

    // Total workouts: 2, Total tonnage: 1000 + 1500 + 960 = 3460 kg
    // Expected points: 2 * 10 + Math.floor(3460 / 100) = 20 + 34 = 54 points
    const localLeaderboard = db.getLeaderboard();
    const entry = localLeaderboard.find(e => e.id === testUserId);
    assert.ok(entry, 'Athlete must be in leaderboard');
    assert.equal(entry.workouts_count, 2);
    assert.equal(entry.total_tonnage, 3460);
    assert.equal(entry.points, 54, 'Points in SQLite query must match workouts * 10 + floor(tonnage / 100)');
  });
});
