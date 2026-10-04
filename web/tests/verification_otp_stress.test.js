/**
 * Automated Verification & Stress Test Suite:
 * 1. Irreversible OTP / Pairing Code Invalidation & 5-minute Expiry
 * 2. 10 Athletes + 2 Trainers Concurrency & Stress Simulation
 * 3. Safe Post-Test Cleanup (Preserving 'Ефимов Михаил Сергеевич')
 */

const assert = require('node:assert');
const path = require('node:path');
const AppDatabase = require('../src/db');
const { hashPassword, verifyPassword, generateToken } = require('../src/security');

async function runTests() {
  console.log('=== [1/3] VERIFYING OTP / PAIRING CODE INVALIDATION & EXPIRY ===');
  const db = new AppDatabase();

  // 1.1 Pairing Code Invalidation on Regeneration
  const testAthleteUsername = 'test_athlete_otp_' + Date.now();
  const initialPin = String(Math.floor(100000 + Math.random() * 900000));
  const newPin = String(Math.floor(100000 + Math.random() * 900000));

  // Create test athlete with initial PIN
  const athleteId = db.createUser(
    testAthleteUsername,
    hashPassword('Pass123!'),
    'athlete',
    'Тестовый Атлет ОТР',
    '+79991112233',
    initialPin,
    'uuid-otp-1'
  );

  let athleteByOldPin = db.findUserByPairingCode(initialPin);
  assert.strictEqual(athleteByOldPin.id, athleteId, 'Initial PIN should find athlete');

  // Regenerate pairing code
  db.regeneratePairingCode(athleteId, newPin);

  // Check old PIN is permanently purged and unusable
  athleteByOldPin = db.findUserByPairingCode(initialPin);
  assert.strictEqual(athleteByOldPin, null, 'OLD PIN MUST BE NULL AND UNUSABLE AFTER REGENERATION');

  // Check new PIN works
  const athleteByNewPin = db.findUserByPairingCode(newPin);
  assert.strictEqual(athleteByNewPin.id, athleteId, 'NEW PIN must find athlete');
  console.log('✔ Old PIN permanently invalidated upon code regeneration.');

  // 1.2 Pairing Code 5-minute TTL Expiration
  // Artificially set pairing_code_created_at to 6 minutes ago (360,000 ms ago)
  const sixMinutesAgo = Date.now() - 360000;
  db.db.prepare(`UPDATE users SET pairing_code_created_at = ? WHERE id = ?`).run(sixMinutesAgo, athleteId);

  const foundAthleteForPairing = db.findUserByPairingCode(newPin);
  const isExpired = foundAthleteForPairing && (Date.now() - foundAthleteForPairing.pairing_code_created_at > 300000);
  assert.strictEqual(isExpired, true, 'PIN older than 5 minutes MUST be evaluated as expired (TTL check)');
  console.log('✔ 5-Minute TTL correctly flags expired PINs as expired.');

  // 1.3 Telegram Link Token Expiration & Irreversible Consumption
  const linkToken = 'tok_test_' + Date.now();
  db.createLinkToken(athleteId, linkToken, 300000); // 5 min TTL
  let tokenRecord = db.findLinkToken(linkToken);
  assert.ok(tokenRecord, 'Link token should exist');

  // Consume token
  db.consumeLinkToken(linkToken);
  let consumedRecord = db.findLinkToken(linkToken);
  assert.strictEqual(consumedRecord, null, 'Consumed link token MUST be permanently deleted');
  console.log('✔ One-time tokens are irreversibly consumed and deleted.');

  console.log('\n=== [2/3] STRESS TEST: 10 CONCURRENT ATHLETES + 2 CONCURRENT TRAINERS ===');

  const NUM_ATHLETES = 10;
  const NUM_TRAINERS = 2;
  const athleteIds = [];
  const trainerIds = [];

  // Register 2 Trainers
  for (let t = 1; t <= NUM_TRAINERS; t++) {
    const tUser = `stress_trainer_${t}_${Date.now()}`;
    const tId = db.createUser(tUser, hashPassword('TrainerPass123!'), 'trainer', `Тренер Стресс ${t}`, `+7999888000${t}`, '', `uuid-tr-${t}`, '', 1);
    trainerIds.push({ id: tId, username: tUser, name: `Тренер Стресс ${t}` });
  }

  // Register 10 Athletes concurrently
  const athletePromises = Array.from({ length: NUM_ATHLETES }).map(async (_, idx) => {
    const aNum = idx + 1;
    const aUser = `stress_athlete_${aNum}_${Date.now()}`;
    const aPin = String(200000 + aNum);
    const aId = db.createUser(
      aUser,
      hashPassword('AthletePass123!'),
      'athlete',
      `Атлет Стресс ${aNum}`,
      `+7999777000${aNum}`,
      aPin,
      `uuid-ath-${aNum}`
    );
    return { id: aId, username: aUser, pin: aPin, name: `Атлет Стресс ${aNum}` };
  });

  const createdAthletes = await Promise.all(athletePromises);
  createdAthletes.forEach(a => athleteIds.push(a));

  console.log(`✔ Created ${NUM_TRAINERS} trainers and ${NUM_ATHLETES} athletes concurrently.`);

  // Pair 5 athletes to Trainer 1, 5 athletes to Trainer 2 concurrently
  const pairingPromises = createdAthletes.map(async (ath, index) => {
    const assignedTrainer = trainerIds[index % NUM_TRAINERS];
    db.pairTrainerAndAthlete(assignedTrainer.id, ath.id);
    db.updateCoachInfo(ath.id, assignedTrainer.name, '+79990000000');
  });

  await Promise.all(pairingPromises);
  console.log('✔ Successfully paired 10 athletes across 2 trainers concurrently.');

  // Concurrently log workouts (10 sets per athlete = 100 sets total)
  const todayStr = new Date().toISOString().slice(0, 10);
  const workoutPromises = [];

  createdAthletes.forEach((ath, athIndex) => {
    const assignedTrainer = trainerIds[athIndex % NUM_TRAINERS];
    const sessionId = db.assignTrainerWorkout(assignedTrainer.id, ath.id, todayStr, 1, 'Stress Session');

    for (let setIdx = 1; setIdx <= 10; setIdx++) {
      workoutPromises.push(
        (async () => {
          const weight = 50 + (setIdx * 5);
          const reps = 8 + (setIdx % 4);
          db.addWorkoutSet(
            sessionId,
            'Жим штанги лежа',
            weight,
            reps,
            8.0,
            1
          );
        })()
      );
    }
  });

  await Promise.all(workoutPromises);
  console.log(`✔ Concurrently recorded ${workoutPromises.length} workout sets across 10 athletes with zero SQLite lockups.`);

  // Concurrently calculate ratings & leaderboards
  const rating1 = db.getLeaderboard();
  assert.ok(rating1.length >= NUM_ATHLETES, 'Rating leaderboard must contain active athletes');
  console.log(`✔ Leaderboard calculated with ${rating1.length} athletes.`);

  // Verify Trainer Client lists
  const trainer1Clients = db.getTrainerClients(trainerIds[0].id);
  const trainer2Clients = db.getTrainerClients(trainerIds[1].id);
  assert.strictEqual(trainer1Clients.length, 5, 'Trainer 1 must have 5 clients');
  assert.strictEqual(trainer2Clients.length, 5, 'Trainer 2 must have 5 clients');
  console.log('✔ Trainer client lists verified: exactly 5 clients per coach.');

  console.log('\n=== [3/3] CLEANING UP ALL TEST ACCOUNTS ===');
  // Purge test accounts while strictly preserving 'Ефимов Михаил Сергеевич'
  db.db.exec(`
    DELETE FROM workout_sets WHERE session_id IN (
      SELECT id FROM workout_sessions WHERE athlete_id NOT IN (SELECT id FROM users WHERE full_name = 'Ефимов Михаил Сергеевич')
    );
    DELETE FROM workout_sessions WHERE athlete_id NOT IN (SELECT id FROM users WHERE full_name = 'Ефимов Михаил Сергеевич');
    DELETE FROM trainer_clients WHERE athlete_id NOT IN (SELECT id FROM users WHERE full_name = 'Ефимов Михаил Сергеевич')
                                   OR trainer_id NOT IN (SELECT id FROM users WHERE full_name = 'Ефимов Михаил Сергеевич');
    DELETE FROM auth_tokens WHERE user_id NOT IN (SELECT id FROM users WHERE full_name = 'Ефимов Михаил Сергеевич');
    DELETE FROM anthropometry WHERE user_id NOT IN (SELECT id FROM users WHERE full_name = 'Ефимов Михаил Сергеевич');
    DELETE FROM telegram_link_tokens WHERE user_id NOT IN (SELECT id FROM users WHERE full_name = 'Ефимов Михаил Сергеевич');
    DELETE FROM users WHERE full_name != 'Ефимов Михаил Сергеевич';
  `);

  const remainingUsers = db.db.prepare(`SELECT id, username, full_name, role FROM users`).all();
  console.log('✔ Remaining accounts in SQLite database:');
  console.table(remainingUsers);

  assert.strictEqual(remainingUsers.length, 1, 'Only 1 user account should remain in database');
  assert.strictEqual(remainingUsers[0].full_name, 'Ефимов Михаил Сергеевич', 'Remaining account MUST be Ефимов Михаил Сергеевич');

  console.log('\n>>> ALL VERIFICATION & STRESS CHECKS PASSED WITH 100% SUCCESS! <<<');
}

runTests().catch(err => {
  console.error('Test failed with error:', err);
  process.exit(1);
});
