/**
 * Database Layer using Node.js Native SQLite (node:sqlite)
 * 100% Parameterized Queries — ZERO SQL Injection Vulnerabilities.
 */

const { DatabaseSync } = require('node:sqlite');
const path = require('node:path');

class AppDatabase {
  constructor(dbPath = path.join(__dirname, '..', 'fitness.sqlite')) {
    this.db = new DatabaseSync(dbPath);
    this.initTables();
  }

  initTables() {
    this.db.exec(`
      CREATE TABLE IF NOT EXISTS users (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        username TEXT UNIQUE NOT NULL,
        password_hash TEXT NOT NULL,
        role TEXT NOT NULL CHECK(role IN ('athlete', 'trainer')),
        full_name TEXT NOT NULL,
        phone TEXT DEFAULT '',
        pairing_code TEXT DEFAULT '',
        is_private INTEGER DEFAULT 0,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP
      );

      CREATE TABLE IF NOT EXISTS trainer_clients (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        trainer_id INTEGER NOT NULL,
        athlete_id INTEGER NOT NULL,
        paired_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        UNIQUE(trainer_id, athlete_id),
        FOREIGN KEY(trainer_id) REFERENCES users(id),
        FOREIGN KEY(athlete_id) REFERENCES users(id)
      );

      CREATE TABLE IF NOT EXISTS workout_sessions (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        athlete_id INTEGER NOT NULL,
        date TEXT NOT NULL,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        UNIQUE(athlete_id, date),
        FOREIGN KEY(athlete_id) REFERENCES users(id)
      );

      CREATE TABLE IF NOT EXISTS workout_sets (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        session_id INTEGER NOT NULL,
        exercise_name TEXT NOT NULL,
        weight_kg REAL NOT NULL,
        reps INTEGER NOT NULL,
        rpe REAL DEFAULT 8.0,
        is_completed INTEGER DEFAULT 1,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY(session_id) REFERENCES workout_sessions(id)
      );

      CREATE TABLE IF NOT EXISTS auth_tokens (
        token TEXT PRIMARY KEY,
        user_id INTEGER NOT NULL,
        expires_at INTEGER NOT NULL,
        FOREIGN KEY(user_id) REFERENCES users(id)
      );
    `);
  }

  // --- User Operations (Strictly Parameterized) ---

  createUser(username, passwordHash, role, fullName, phone = '', pairingCode = '') {
    const stmt = this.db.prepare(`
      INSERT INTO users (username, password_hash, role, full_name, phone, pairing_code, is_private)
      VALUES (?, ?, ?, ?, ?, ?, 0)
    `);
    const result = stmt.run(username, passwordHash, role, fullName, phone, pairingCode);
    return Number(result.lastInsertRowid);
  }

  findUserByUsername(username) {
    const stmt = this.db.prepare(`
      SELECT id, username, password_hash, role, full_name, phone, pairing_code, is_private, created_at
      FROM users WHERE username = ?
    `);
    return stmt.get(username) || null;
  }

  findUserById(id) {
    const stmt = this.db.prepare(`
      SELECT id, username, role, full_name, phone, pairing_code, is_private, created_at
      FROM users WHERE id = ?
    `);
    return stmt.get(id) || null;
  }

  findUserByPairingCode(code) {
    const stmt = this.db.prepare(`
      SELECT id, username, role, full_name, phone, pairing_code, is_private
      FROM users WHERE pairing_code = ? AND role = 'athlete'
    `);
    return stmt.get(code) || null;
  }

  updateProfile(userId, fullName, phone) {
    const stmt = this.db.prepare(`
      UPDATE users SET full_name = ?, phone = ? WHERE id = ?
    `);
    return stmt.run(fullName, phone, userId);
  }

  updateAthletePrivacy(athleteId, isPrivate) {
    const stmt = this.db.prepare(`
      UPDATE users SET is_private = ? WHERE id = ? AND role = 'athlete'
    `);
    return stmt.run(isPrivate ? 1 : 0, athleteId);
  }

  regeneratePairingCode(athleteId, newCode) {
    const stmt = this.db.prepare(`
      UPDATE users SET pairing_code = ? WHERE id = ? AND role = 'athlete'
    `);
    return stmt.run(newCode, athleteId);
  }

  updateUserRole(userId, newRole) {
    if (newRole !== 'athlete' && newRole !== 'trainer') return false;
    const stmt = this.db.prepare(`
      UPDATE users SET role = ? WHERE id = ?
    `);
    stmt.run(newRole, userId);
    return true;
  }

  // --- Pairing Operations ---

  pairTrainerAndAthlete(trainerId, athleteId) {
    const stmt = this.db.prepare(`
      INSERT OR IGNORE INTO trainer_clients (trainer_id, athlete_id)
      VALUES (?, ?)
    `);
    stmt.run(trainerId, athleteId);
    return true;
  }

  getTrainerClients(trainerId) {
    const stmt = this.db.prepare(`
      SELECT u.id, u.username, u.full_name, u.phone, u.pairing_code, tc.paired_at,
             (SELECT COUNT(*) FROM workout_sessions ws WHERE ws.athlete_id = u.id) as sessions_count,
             (SELECT COALESCE(SUM(s.weight_kg * s.reps), 0) FROM workout_sets s 
              JOIN workout_sessions ws ON s.session_id = ws.id WHERE ws.athlete_id = u.id) as total_tonnage
      FROM trainer_clients tc
      JOIN users u ON tc.athlete_id = u.id
      WHERE tc.trainer_id = ?
      ORDER BY tc.paired_at DESC
    `);
    return stmt.all(trainerId);
  }

  getPairedTrainer(athleteId) {
    const stmt = this.db.prepare(`
      SELECT u.id, u.username, u.full_name, u.phone
      FROM trainer_clients tc
      JOIN users u ON tc.trainer_id = u.id
      WHERE tc.athlete_id = ?
      LIMIT 1
    `);
    return stmt.get(athleteId) || null;
  }

  unpairAthlete(athleteId) {
    const stmt = this.db.prepare(`
      DELETE FROM trainer_clients WHERE athlete_id = ?
    `);
    return stmt.run(athleteId);
  }

  unpairTrainerClient(trainerId, athleteId) {
    const stmt = this.db.prepare(`
      DELETE FROM trainer_clients WHERE trainer_id = ? AND athlete_id = ?
    `);
    return stmt.run(trainerId, athleteId);
  }

  // --- Workout Operations ---

  getOrCreateSession(athleteId, date) {
    const selectStmt = this.db.prepare(`
      SELECT id FROM workout_sessions WHERE athlete_id = ? AND date = ?
    `);
    let session = selectStmt.get(athleteId, date);
    if (!session) {
      const insertStmt = this.db.prepare(`
        INSERT INTO workout_sessions (athlete_id, date) VALUES (?, ?)
      `);
      const res = insertStmt.run(athleteId, date);
      session = { id: Number(res.lastInsertRowid) };
    }
    return session.id;
  }

  addWorkoutSet(athleteId, date, exerciseName, weightKg, reps, rpe = 8.0) {
    const sessionId = this.getOrCreateSession(athleteId, date);
    const stmt = this.db.prepare(`
      INSERT INTO workout_sets (session_id, exercise_name, weight_kg, reps, rpe, is_completed)
      VALUES (?, ?, ?, ?, ?, 1)
    `);
    const res = stmt.run(sessionId, exerciseName, weightKg, reps, rpe);
    return Number(res.lastInsertRowid);
  }

  getWorkoutSets(athleteId, date) {
    const stmt = this.db.prepare(`
      SELECT s.id, s.exercise_name, s.weight_kg, s.reps, s.rpe, s.is_completed, s.created_at
      FROM workout_sets s
      JOIN workout_sessions ws ON s.session_id = ws.id
      WHERE ws.athlete_id = ? AND ws.date = ?
      ORDER BY s.id ASC
    `);
    return stmt.all(athleteId, date);
  }

  toggleWorkoutSet(setId, isCompleted) {
    const stmt = this.db.prepare(`
      UPDATE workout_sets SET is_completed = ? WHERE id = ?
    `);
    return stmt.run(isCompleted ? 1 : 0, setId);
  }

  deleteWorkoutSet(setId) {
    const stmt = this.db.prepare(`
      DELETE FROM workout_sets WHERE id = ?
    `);
    return stmt.run(setId);
  }

  getWorkoutSetById(setId) {
    const stmt = this.db.prepare(`
      SELECT s.id, s.session_id, s.exercise_name, s.weight_kg, s.reps, s.rpe, s.is_completed, s.created_at, ws.athlete_id
      FROM workout_sets s
      JOIN workout_sessions ws ON s.session_id = ws.id
      WHERE s.id = ?
    `);
    return stmt.get(setId) || null;
  }

  getLastExerciseStats(athleteId, exerciseName) {
    const stmt = this.db.prepare(`
      SELECT ws.date, s.weight_kg, s.reps, s.rpe
      FROM workout_sets s
      JOIN workout_sessions ws ON s.session_id = ws.id
      WHERE ws.athlete_id = ? AND s.exercise_name = ?
      ORDER BY s.id DESC
      LIMIT 1
    `);
    return stmt.get(athleteId, exerciseName) || null;
  }

  // --- Leaderboard / Competitions (Zero Mocks) ---

  getLeaderboard() {
    const stmt = this.db.prepare(`
      SELECT u.id, u.full_name,
             COUNT(DISTINCT ws.id) as workouts_count,
             COALESCE(SUM(s.weight_kg * s.reps), 0) as total_tonnage,
             ROUND(COUNT(DISTINCT ws.id) * 100 + COALESCE(SUM(s.weight_kg * s.reps), 0) * 0.1) as points
      FROM users u
      LEFT JOIN workout_sessions ws ON u.id = ws.athlete_id
      LEFT JOIN workout_sets s ON ws.id = s.session_id
      WHERE u.role = 'athlete' AND u.is_private = 0
      GROUP BY u.id, u.full_name
      ORDER BY points DESC, total_tonnage DESC
    `);
    return stmt.all();
  }

  // --- Auth Token Operations ---

  createAuthToken(token, userId, ttlMs = 86400000 * 7) {
    const expiresAt = Date.now() + ttlMs;
    const stmt = this.db.prepare(`
      INSERT OR REPLACE INTO auth_tokens (token, user_id, expires_at)
      VALUES (?, ?, ?)
    `);
    stmt.run(token, userId, expiresAt);
  }

  getUserByToken(token) {
    const now = Date.now();
    const stmt = this.db.prepare(`
      SELECT u.id, u.username, u.role, u.full_name, u.phone, u.pairing_code, u.is_private
      FROM auth_tokens t
      JOIN users u ON t.user_id = u.id
      WHERE t.token = ? AND t.expires_at > ?
    `);
    return stmt.get(token, now) || null;
  }

  deleteAuthToken(token) {
    const stmt = this.db.prepare(`
      DELETE FROM auth_tokens WHERE token = ?
    `);
    return stmt.run(token);
  }

  close() {
    this.db.close();
  }
}

module.exports = AppDatabase;
