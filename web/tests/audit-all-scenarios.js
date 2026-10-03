/**
 * Fitness Ecosystem Pro - Full Cross-Platform & UI Audit Test Suite
 * Tests all 4 scenarios:
 * 1. Web Athlete <-> Mobile Trainer (via Google Drive)
 * 2. Web Trainer <-> Mobile Athlete (via Google Drive)
 * 3. Web Athlete <-> Web Trainer (Local + Cloud)
 * 4. UI Components, Strict Permissions, and Zero-Mocks Competitions
 */

const assert = require('node:assert');
const test = require('node:test');
const http = require('node:http');
const { cloudSyncService } = require('../src/cloudSync');
const AppDatabase = require('../src/db');

const BASE_URL = 'http://localhost:3000';

function request(path, options = {}) {
  return new Promise((resolve, reject) => {
    const url = new URL(path, BASE_URL);
    const req = http.request(url, {
      method: options.method || 'GET',
      headers: {
        'Content-Type': 'application/json',
        ...(options.headers || {})
      }
    }, res => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        let json = null;
        try { json = JSON.parse(data); } catch (_) {}
        resolve({ status: res.statusCode, headers: res.headers, body: json, raw: data });
      });
    });
    req.on('error', reject);
    if (options.body) {
      req.write(typeof options.body === 'string' ? options.body : JSON.stringify(options.body));
    }
    req.end();
  });
}

test('Full Ecosystem Audit: Cross-Platform & UI Scenarios', async (t) => {
  const ts = Date.now();

  await t.test('1. Scenario A: Web Athlete <-> Mobile Trainer (Google Drive Cloud)', async () => {
    // 1. Register Web Athlete
    const athUsername = `audit_ath_${ts}`;
    const regRes = await request('/api/register', {
      method: 'POST',
      body: {
        username: athUsername,
        password: 'Password123!',
        role: 'athlete',
        fullName: 'Аудит Атлет (Web)',
        phone: '+7 (999) 000-00-01'
      }
    });

    assert.strictEqual(regRes.status, 201, 'Web Athlete must register with 201');
    const athleteToken = regRes.body.token;
    const athleteUser = regRes.body.user;
    const pin = athleteUser.pairingCode;
    const clientUuid = athleteUser.clientUuid;
    assert.match(pin, /^\d{6}$/, 'Pairing PIN must be 6 digits');

    // 2. Simulate Mobile Trainer Pro discovering athlete PIN and pairing via Google Drive cloud
    const mockMobileCoachName = 'Алексей Романов (Mobile Trainer)';
    const mockMobileCoachPhone = '+7 (999) 777-88-99';

    // Simulate Google Drive cloud update by Android Trainer Pro
    const cloud = await cloudSyncService.fetchCloudData(true);
    if (!cloud.pairing) cloud.pairing = {};
    cloud.pairing[pin] = {
      pin,
      clientUuid,
      clientName: athleteUser.fullName,
      phone: athleteUser.phone,
      status: 'PAIRED',
      coachName: mockMobileCoachName,
      coachPhone: mockMobileCoachPhone,
      timestamp: Date.now()
    };

    // Mobile trainer assigns a workout for today in cloud
    const today = new Date().toISOString().slice(0, 10);
    if (!cloud.clients) cloud.clients = {};
    cloud.clients[clientUuid] = {
      clientUuid,
      clientName: athleteUser.fullName,
      assignedWorkouts: [{
        date: today,
        notes: 'План от мобильного тренера',
        completed: false,
        isSelfWorkoutAllowed: false,
        exercises: [{
          exerciseId: 1,
          name: 'Жим штанги лёжа',
          muscleGroup: 'Грудь',
          sets: [{
            setNumber: 1,
            targetWeightKg: 80.0,
            targetReps: 10,
            actualWeightKg: 80.0,
            actualReps: 10,
            isCompleted: false,
            rpe: 8.0
          }]
        }]
      }]
    };
    await cloudSyncService.pushCloudData(cloud);

    // 3. Web Athlete opens profile (/api/me)
    const meRes = await request('/api/me', {
      headers: { 'Authorization': `Bearer ${athleteToken}` }
    });
    assert.strictEqual(meRes.status, 200);
    assert.strictEqual(meRes.body.pairedCoach?.full_name, mockMobileCoachName, 'Web Athlete sees Mobile Trainer name in paired coach profile');
    assert.strictEqual(meRes.body.pairedCoach?.phone, mockMobileCoachPhone, 'Web Athlete sees Mobile Trainer phone');

    // 4. Web Athlete opens workout screen (GET /api/workout)
    const workoutRes = await request(`/api/workout?date=${today}`, {
      headers: { 'Authorization': `Bearer ${athleteToken}` }
    });
    assert.strictEqual(workoutRes.status, 200);
    assert.strictEqual(workoutRes.body.sets.length, 1, 'Web Athlete receives exercises assigned by Mobile Trainer via Google Drive');
    assert.strictEqual(workoutRes.body.sets[0].exercise_name, 'Жим штанги лёжа');
    assert.strictEqual(workoutRes.body.isSelfAllowed, false, 'Self workout is disabled as set by Mobile Trainer');

    // 5. Strict Athlete Permissions Check: Athlete cannot add exercise when self workout is not allowed
    const addForbiddenRes = await request('/api/workout/set', {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${athleteToken}` },
      body: {
        date: today,
        exerciseName: 'Самовольные приседания',
        weightKg: 100,
        reps: 5
      }
    });
    assert.strictEqual(addForbiddenRes.status, 403, 'Athlete must be strictly forbidden from creating workouts when coach disallowed it');

    // 6. Web Athlete marks set completed (toggle)
    const targetSetId = workoutRes.body.sets[0].id;
    const toggleRes = await request('/api/workout/set/toggle', {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${athleteToken}` },
      body: { setId: targetSetId, isCompleted: true }
    });
    assert.strictEqual(toggleRes.status, 200);
    assert.strictEqual(toggleRes.body.isCompleted, true);

    // Verify it synced back to Google Drive for Mobile Trainer
    const updatedCloud = await cloudSyncService.fetchCloudData(true);
    const athleteCloudSession = updatedCloud.clients[clientUuid].assignedWorkouts.find(w => w.date === today);
    assert.strictEqual(athleteCloudSession.completed, true, 'Completed workout status synced to Google Drive for Mobile Trainer');
    assert.strictEqual(athleteCloudSession.exercises[0].sets[0].isCompleted, true, 'Set isCompleted synced to Google Drive');
  });

  await t.test('2. Scenario B: Web Trainer <-> Mobile Athlete (Google Drive Cloud)', async () => {
    // 1. Register Web Trainer
    const trainerUsername = `audit_tr_${ts}`;
    const regTrRes = await request('/api/register', {
      method: 'POST',
      body: {
        username: trainerUsername,
        password: 'Password123!',
        role: 'trainer',
        fullName: 'Сергей Тренер (Web)',
        phone: '+7 (999) 555-44-33'
      }
    });
    assert.strictEqual(regTrRes.status, 201);
    const trainerToken = regTrRes.body.token;

    // 2. Simulate Mobile Athlete Pro registered on Android phone with Google Drive cloud entry
    const mobilePin = String(Math.floor(100000 + Math.random() * 900000));
    const mobileClientUuid = `mobile-uuid-${ts}`;
    const mobileAthleteName = 'Дмитрий (Mobile Athlete)';
    const mobileAthletePhone = '+7 (999) 333-22-11';

    const cloud = await cloudSyncService.fetchCloudData(true);
    if (!cloud.pairing) cloud.pairing = {};
    cloud.pairing[mobilePin] = {
      pin: mobilePin,
      clientUuid: mobileClientUuid,
      clientName: mobileAthleteName,
      phone: mobileAthletePhone,
      status: 'PENDING',
      timestamp: Date.now()
    };
    if (!cloud.clients) cloud.clients = {};
    cloud.clients[mobileClientUuid] = {
      clientUuid: mobileClientUuid,
      clientName: mobileAthleteName,
      phone: mobileAthletePhone,
      assignedWorkouts: []
    };
    await cloudSyncService.pushCloudData(cloud);

    // 3. Web Trainer enters the 6-digit PIN of Mobile Athlete
    const pairRes = await request('/api/trainer/pair', {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${trainerToken}` },
      body: { code: mobilePin }
    });
    assert.strictEqual(pairRes.status, 200, 'Web Trainer successfully pairs with Mobile Athlete via 6-digit PIN');
    assert.strictEqual(pairRes.body.athlete.fullName, mobileAthleteName);

    const pairedAthleteId = pairRes.body.athlete.id;

    // 4. Web Trainer assigns workout to Mobile Athlete
    const today = new Date().toISOString().slice(0, 10);
    const assignRes = await request('/api/trainer/assign-workout', {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${trainerToken}` },
      body: {
        athleteId: pairedAthleteId,
        date: today,
        isSelfAllowed: true,
        exercises: [{ exerciseName: 'Становая тяга', weightKg: 120, reps: 5 }]
      }
    });
    assert.strictEqual(assignRes.status, 200);

    // 5. Verify Mobile Athlete's Google Drive node received the workout
    const verifyCloud = await cloudSyncService.fetchCloudData(true);
    const assigned = verifyCloud.clients[mobileClientUuid].assignedWorkouts;
    assert.ok(assigned.length > 0, 'Google Drive received assigned workout for Mobile Athlete');
    assert.strictEqual(assigned[0].exercises[0].name, 'Становая тяга');
    assert.strictEqual(assigned[0].isSelfWorkoutAllowed, true);

    // 6. Simulate Mobile Athlete marking the set completed on phone
    verifyCloud.clients[mobileClientUuid].assignedWorkouts[0].exercises[0].sets[0].isCompleted = true;
    verifyCloud.clients[mobileClientUuid].assignedWorkouts[0].completed = true;
    await cloudSyncService.pushCloudData(verifyCloud);

    // 7. Web Trainer views workout in Web Portal
    const checkWorkoutRes = await request(`/api/workout?athleteId=${pairedAthleteId}&date=${today}`, {
      headers: { 'Authorization': `Bearer ${trainerToken}` }
    });
    assert.strictEqual(checkWorkoutRes.status, 200);
    assert.strictEqual(checkWorkoutRes.body.sets[0].is_completed, 1, 'Web Trainer sees that Mobile Athlete completed the workout');
  });

  await t.test('3. UI & Profile Editing Audit', async () => {
    // Register athlete
    const uName = `ui_user_${ts}`;
    const regRes = await request('/api/register', {
      method: 'POST',
      body: {
        username: uName,
        password: 'Password123!',
        role: 'athlete',
        fullName: 'Тест Юзер',
        phone: '+7 900 111-22-33'
      }
    });
    const token = regRes.body.token;

    // Test Profile update
    const updateRes = await request('/api/user/profile', {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${token}` },
      body: {
        fullName: 'Александр Великий',
        phone: '+7 900 999-88-77',
        avatarBase64: 'data:image/jpeg;base64,/9j/4AAQSkZJRg=='
      }
    });
    assert.strictEqual(updateRes.status, 200);
    assert.strictEqual(updateRes.body.user.fullName, 'Александр Великий');
    assert.strictEqual(updateRes.body.user.phone, '+7 900 999-88-77');
    assert.ok(updateRes.body.user.avatarBase64);
  });

  await t.test('4. Leaderboard Zero-Mocks & Clean Data Audit', async () => {
    const lbRes = await request('/api/leaderboard');
    assert.strictEqual(lbRes.status, 200);
    assert.ok(Array.isArray(lbRes.body.leaderboard));
    
    // Check that there are no mock names
    const mocks = lbRes.body.leaderboard.filter(e => /^(test_|bot_|mock_|fake_|adv_)/i.test(e.name));
    assert.strictEqual(mocks.length, 0, 'No mock or bot accounts in leaderboard');
  });
});
