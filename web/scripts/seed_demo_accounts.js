/**
 * Fitness Ecosystem Pro - Demo Accounts Seeder
 * Создает demo_athlete и demo_trainer с тренировкой (5 подходов), замерами и привязкой.
 */
const path = require('node:path');
const fs = require('node:fs');
const AppDatabase = require('../src/db');
const { hashPassword } = require('../src/security');

function seedDemoAccounts(dbInstance) {
  const db = dbInstance.db;

  const athleteHash = hashPassword('athlete123');
  const trainerHash = hashPassword('trainer123');

  // 1. Демо-атлет: Алексей Смирнов
  let athlete = dbInstance.findUserByUsername('demo_athlete');
  let athleteId;
  if (!athlete) {
    athleteId = dbInstance.createUser(
      'demo_athlete',
      athleteHash,
      'athlete',
      'Алексей Смирнов',
      '+79991234567',
      '123456',
      'uuid-demo-athlete-001',
      '',
      1 // is_approved = 1
    );
  } else {
    athleteId = athlete.id;
    db.prepare(`
      UPDATE users 
      SET password_hash = ?, full_name = ?, phone = ?, is_approved = 1, pairing_code = '123456', client_uuid = 'uuid-demo-athlete-001'
      WHERE id = ?
    `).run(athleteHash, 'Алексей Смирнов', '+79991234567', athleteId);
  }

  // 2. Демо-тренер: Михаил Тренер (is_approved = 1)
  let trainer = dbInstance.findUserByUsername('demo_trainer');
  let trainerId;
  if (!trainer) {
    trainerId = dbInstance.createUser(
      'demo_trainer',
      trainerHash,
      'trainer',
      'Михаил Тренер',
      '+79997654321',
      '',
      'uuid-demo-trainer-001',
      '',
      1 // is_approved = 1
    );
  } else {
    trainerId = trainer.id;
    db.prepare(`
      UPDATE users 
      SET password_hash = ?, full_name = ?, phone = ?, is_approved = 1, client_uuid = 'uuid-demo-trainer-001'
      WHERE id = ?
    `).run(trainerHash, 'Михаил Тренер', '+79997654321', trainerId);
  }

  // 3. Привязка тренера к атлету
  dbInstance.pairTrainerAndAthlete(trainerId, athleteId, true);
  dbInstance.updateCoachInfo(athleteId, 'Михаил Тренер', '+79997654321');

  // 4. Сегодняшняя тренировка с 5 подходами
  const today = new Date().toISOString().slice(0, 10);
  const session = dbInstance.getOrCreateSession(athleteId, today);
  const sessionId = session.id;

  db.prepare(`
    UPDATE workout_sessions
    SET is_self_workout_allowed = 1, completed = 1, assigned_by_trainer_id = ?
    WHERE id = ?
  `).run(trainerId, sessionId);

  // Очистка и вставка 5 подходов
  db.prepare(`DELETE FROM workout_sets WHERE session_id = ?`).run(sessionId);

  const demoSets = [
    { ex: 'Жим штанги лежа', weight: 80, reps: 10, rpe: 8.0, num: 1 },
    { ex: 'Жим штанги лежа', weight: 85, reps: 8, rpe: 8.5, num: 2 },
    { ex: 'Жим штанги лежа', weight: 90, reps: 6, rpe: 9.0, num: 3 },
    { ex: 'Приседания со штангой', weight: 100, reps: 8, rpe: 8.0, num: 4 },
    { ex: 'Приседания со штангой', weight: 110, reps: 6, rpe: 8.5, num: 5 }
  ];

  for (const s of demoSets) {
    db.prepare(`
      INSERT INTO workout_sets (
        session_id, exercise_name, weight_kg, reps, rpe, is_completed, 
        set_number, target_weight_kg, target_reps, actual_weight_kg, actual_reps
      ) VALUES (?, ?, ?, ?, ?, 1, ?, ?, ?, ?, ?)
    `).run(sessionId, s.ex, s.weight, s.reps, s.rpe, s.num, s.weight, s.reps, s.weight, s.reps);
  }

  // 5. Антропометрические замеры за сегодня (addAnthropometry: userId, date, weight, chest, waist, biceps)
  db.prepare(`DELETE FROM anthropometry WHERE user_id = ? AND date = ?`).run(athleteId, today);
  dbInstance.addAnthropometry(athleteId, today, 78.5, 104, 82, 38.5);

  console.log(`✓ Успешно: demo_athlete (ID ${athleteId}) и demo_trainer (ID ${trainerId}) настроены в БД.`);
}

// Запуск для основной базы fitness.sqlite
const db = new AppDatabase();
seedDemoAccounts(db);

// Если в корне проекта также присутствует fitness.sqlite — обновляем и её
const rootDbPath = path.join(__dirname, '..', '..', 'fitness.sqlite');
if (fs.existsSync(rootDbPath)) {
  try {
    const rootDb = new AppDatabase(rootDbPath);
    seedDemoAccounts(rootDb);
  } catch (_) {}
}
