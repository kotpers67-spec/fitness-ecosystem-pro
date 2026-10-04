/**
 * Database Layer using Node.js Native SQLite (node:sqlite)
 * 100% Parameterized Queries — ZERO SQL Injection Vulnerabilities.
 */

const { DatabaseSync } = require('node:sqlite');
const path = require('node:path');
const { generateSecurePin } = require('./security');

class AppDatabase {
  constructor(dbPath = path.join(__dirname, '..', 'fitness.sqlite')) {
    this.db = new DatabaseSync(dbPath);
    this.initDb();
    this.initTables();
  }

  initDb() {
    try {
      this.db.exec("PRAGMA journal_mode = WAL; PRAGMA busy_timeout = 5000; PRAGMA synchronous = NORMAL; PRAGMA foreign_keys = ON;");
    } catch (_) {}
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
        avatar_base64 TEXT DEFAULT '',
        client_uuid TEXT DEFAULT '',
        coach_name TEXT DEFAULT '',
        coach_phone TEXT DEFAULT '',
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
        notes TEXT DEFAULT '',
        completed INTEGER DEFAULT 0,
        is_self_workout_allowed INTEGER DEFAULT 0,
        assigned_by_trainer_id INTEGER DEFAULT NULL,
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
        set_number INTEGER DEFAULT 1,
        target_weight_kg REAL DEFAULT 0,
        target_reps INTEGER DEFAULT 0,
        actual_weight_kg REAL DEFAULT 0,
        actual_reps INTEGER DEFAULT 0,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY(session_id) REFERENCES workout_sessions(id)
      );

      CREATE TABLE IF NOT EXISTS auth_tokens (
        token TEXT PRIMARY KEY,
        user_id INTEGER NOT NULL,
        expires_at INTEGER NOT NULL,
        FOREIGN KEY(user_id) REFERENCES users(id)
      );

      CREATE TABLE IF NOT EXISTS telegram_link_tokens (
        token TEXT PRIMARY KEY,
        user_id INTEGER NOT NULL,
        expires_at INTEGER NOT NULL,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY(user_id) REFERENCES users(id)
      );

      CREATE TABLE IF NOT EXISTS anthropometry (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        user_id INTEGER NOT NULL,
        date TEXT NOT NULL,
        weight_kg REAL NOT NULL,
        chest_cm REAL DEFAULT 0,
        waist_cm REAL DEFAULT 0,
        biceps_cm REAL DEFAULT 0,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY(user_id) REFERENCES users(id)
      );

      CREATE INDEX IF NOT EXISTS idx_users_pairing_code ON users(pairing_code);
      CREATE INDEX IF NOT EXISTS idx_workout_sets_session ON workout_sets(session_id);
    `);

    // Ensure backwards-compatible columns exist in existing tables
    const safeAddColumn = (table, colDef) => {
      try { this.db.exec(`ALTER TABLE ${table} ADD COLUMN ${colDef}`); } catch (_) {}
    };

    safeAddColumn('users', "avatar_base64 TEXT DEFAULT ''");
    safeAddColumn('users', "client_uuid TEXT DEFAULT ''");
    safeAddColumn('users', "coach_name TEXT DEFAULT ''");
    safeAddColumn('users', "coach_phone TEXT DEFAULT ''");
    safeAddColumn('users', "pairing_code_created_at INTEGER DEFAULT 0");
    safeAddColumn('users', "telegram_id TEXT DEFAULT ''");
    safeAddColumn('users', "telegram_username TEXT DEFAULT ''");
    safeAddColumn('users', "two_factor_enabled INTEGER DEFAULT 0");
    safeAddColumn('users', "is_approved INTEGER DEFAULT 1");
    safeAddColumn('users', "restrictions TEXT DEFAULT ''");

    safeAddColumn('workout_sessions', "is_self_workout_allowed INTEGER DEFAULT 0");
    safeAddColumn('workout_sessions', "assigned_by_trainer_id INTEGER DEFAULT NULL");
    safeAddColumn('workout_sessions', "completed INTEGER DEFAULT 0");
    safeAddColumn('workout_sessions', "notes TEXT DEFAULT ''");

    safeAddColumn('workout_sets', "set_number INTEGER DEFAULT 1");
    safeAddColumn('workout_sets', "target_weight_kg REAL DEFAULT 0");
    safeAddColumn('workout_sets', "target_reps INTEGER DEFAULT 0");
    safeAddColumn('workout_sets', "actual_weight_kg REAL DEFAULT 0");
    safeAddColumn('workout_sets', "actual_reps INTEGER DEFAULT 0");
  }

  // --- User Operations (Strictly Parameterized) ---

  createUser(username, passwordHash, role, fullName, phone = '', pairingCode = '', clientUuid = '', avatarBase64 = '', isApproved = null) {
    const pairingCreatedAt = pairingCode ? Date.now() : 0;
    const approvedVal = isApproved !== null ? (isApproved ? 1 : 0) : (role === 'trainer' ? 0 : 1);
    const initialUsername = String(username || '').trim() || `${role}_temp_${Date.now()}`;
    const stmt = this.db.prepare(`
      INSERT INTO users (username, password_hash, role, full_name, phone, pairing_code, client_uuid, avatar_base64, is_private, pairing_code_created_at, is_approved)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0, ?, ?)
    `);
    const result = stmt.run(initialUsername, passwordHash, role, fullName, phone, pairingCode, clientUuid, avatarBase64, pairingCreatedAt, approvedVal);
    const userId = Number(result.lastInsertRowid);
    if (!username) {
      const formattedUsername = `${role}_${userId}`;
      this.db.prepare(`UPDATE users SET username = ? WHERE id = ?`).run(formattedUsername, userId);
    }
    return userId;
  }

  approveTrainer(identifier) {
    const raw = String(identifier || '').replace(/^@/, '').trim();
    const id = parseInt(raw, 10);
    const cleanLower = raw.toLowerCase();
    const stmt = this.db.prepare(`
      UPDATE users SET is_approved = 1 
      WHERE (id = ? OR LOWER(username) = ? OR LOWER(telegram_username) = ?) AND role = 'trainer'
    `);
    return stmt.run(id || 0, cleanLower, cleanLower);
  }

  updateAthleteRestrictions(athleteId, restrictions) {
    const stmt = this.db.prepare(`UPDATE users SET restrictions = ? WHERE id = ? AND role = 'athlete'`);
    return stmt.run(restrictions, athleteId);
  }

  findUserByUsername(username) {
    const clean = String(username || '').trim();
    if (!clean) return null;
    const cleanNoAt = clean.replace(/^@/, '');
    const cleanDigits = clean.replace(/\D/g, '');
    const stmt = this.db.prepare(`
      SELECT id, username, password_hash, role, full_name, phone, avatar_base64, client_uuid, coach_name, coach_phone, pairing_code, pairing_code_created_at, is_private, telegram_id, telegram_username, two_factor_enabled, created_at, is_approved, restrictions
      FROM users 
      WHERE username = ? 
         OR LOWER(username) = LOWER(?) 
         OR LOWER(full_name) = LOWER(?) 
         OR LOWER(telegram_username) = LOWER(?)
         OR telegram_username = ?
         OR (phone != '' AND phone = ?)
    `);
    let user = stmt.get(clean, clean, clean, cleanNoAt, cleanNoAt, cleanDigits || clean);
    if (!user && cleanDigits && cleanDigits.length >= 7) {
      user = this.findUserByPhone(cleanDigits);
    }
    return user || null;
  }

  findUserByPhone(phone) {
    const raw = String(phone || '').trim();
    const digits = raw.replace(/\D/g, '');
    if (!digits || digits.length < 7) return null;
    const last10 = digits.slice(-10);
    const stmt = this.db.prepare(`
      SELECT id, username, password_hash, role, full_name, phone, avatar_base64, client_uuid, coach_name, coach_phone, pairing_code, pairing_code_created_at, is_private, telegram_id, telegram_username, two_factor_enabled, created_at, is_approved, restrictions
      FROM users
      WHERE phone = ? OR phone LIKE ?
    `);
    return stmt.get(digits, `%${last10}`) || null;
  }

  findUserByFullName(fullName) {
    const clean = String(fullName || '').trim().toLowerCase();
    if (!clean) return null;
    const stmt = this.db.prepare(`
      SELECT id, username, password_hash, role, full_name, phone, avatar_base64, client_uuid, coach_name, coach_phone, pairing_code, pairing_code_created_at, is_private, telegram_id, telegram_username, two_factor_enabled, created_at, is_approved, restrictions
      FROM users
      WHERE LOWER(full_name) = ?
    `);
    return stmt.get(clean) || null;
  }

  findUserById(id) {
    const stmt = this.db.prepare(`
      SELECT id, username, password_hash, role, full_name, phone, avatar_base64, client_uuid, coach_name, coach_phone, pairing_code, pairing_code_created_at, is_private, telegram_id, telegram_username, two_factor_enabled, created_at, is_approved, restrictions
      FROM users WHERE id = ?
    `);
    return stmt.get(id) || null;
  }

  findUserByClientUuid(clientUuid) {
    const stmt = this.db.prepare(`
      SELECT id, username, role, full_name, phone, avatar_base64, client_uuid, coach_name, coach_phone, pairing_code, is_private
      FROM users WHERE client_uuid = ?
    `);
    return stmt.get(clientUuid) || null;
  }

  findUserByPairingCode(code) {
    const cleanCode = String(code || '').trim();
    if (!cleanCode) return null;
    const stmt = this.db.prepare(`
      SELECT id, username, role, full_name, phone, avatar_base64, client_uuid, coach_name, coach_phone, pairing_code, pairing_code_created_at, is_private
      FROM users WHERE pairing_code = ? AND role = 'athlete'
    `);
    return stmt.get(cleanCode) || null;
  }

  generateUniquePairingCode(currentUserId = null) {
    const PAIRING_TTL = 5 * 60 * 1000;
    const now = Date.now();
    for (let attempt = 0; attempt < 20; attempt++) {
      const pin = generateSecurePin();
      const existing = this.findUserByPairingCode(pin);
      if (!existing) return pin;
      if (currentUserId && existing.id === currentUserId) return pin;
      if (now - (existing.pairing_code_created_at || 0) >= PAIRING_TTL) return pin;
    }
    return generateSecurePin();
  }

  updateProfile(userId, fullName, phone, avatarBase64 = null, restrictions = null, clientUuid = null) {
    const existing = this.findUserById(userId);
    if (!existing) return null;
    const finalAvatar = (avatarBase64 !== null && avatarBase64 !== undefined) ? avatarBase64 : existing.avatar_base64;
    const finalRestrictions = (restrictions !== null && restrictions !== undefined) ? restrictions : (existing.restrictions || '');
    const finalClientUuid = (clientUuid && String(clientUuid).trim()) ? String(clientUuid).trim() : existing.client_uuid;

    const stmt = this.db.prepare(`
      UPDATE users 
      SET full_name = ?, phone = ?, avatar_base64 = ?, restrictions = ?, client_uuid = ? 
      WHERE id = ?
    `);
    return stmt.run(fullName, phone, finalAvatar, finalRestrictions, finalClientUuid, userId);
  }

  updateCoachInfo(athleteId, coachName, coachPhone) {
    const stmt = this.db.prepare(`
      UPDATE users SET coach_name = ?, coach_phone = ? WHERE id = ?
    `);
    return stmt.run(coachName, coachPhone, athleteId);
  }

  updateClientUuid(athleteId, clientUuid) {
    const cleanUuid = String(clientUuid || '').trim();
    const stmt = this.db.prepare(`UPDATE users SET client_uuid = ? WHERE id = ?`);
    return stmt.run(cleanUuid, athleteId);
  }

  updateAthletePrivacy(athleteId, isPrivate) {
    const stmt = this.db.prepare(`
      UPDATE users SET is_private = ? WHERE id = ? AND role = 'athlete'
    `);
    return stmt.run(isPrivate ? 1 : 0, athleteId);
  }

  regeneratePairingCode(athleteId, newCode) {
    const now = Date.now();
    const stmt = this.db.prepare(`
      UPDATE users SET pairing_code = ?, pairing_code_created_at = ? WHERE id = ? AND role = 'athlete'
    `);
    return stmt.run(newCode, now, athleteId);
  }

  updatePairingCode(athleteId, newCode) {
    return this.regeneratePairingCode(athleteId, newCode);
  }

  consumePairingCode(athleteId) {
    const stmt = this.db.prepare(`
      UPDATE users SET pairing_code = '', pairing_code_created_at = 0 WHERE id = ? AND role = 'athlete'
    `);
    return stmt.run(athleteId);
  }

  linkTelegram(userId, telegramId, telegramUsername) {
    const cleanUsername = String(telegramUsername || '').replace(/^@/, '').trim().toLowerCase();
    const cleanId = String(telegramId || '').trim();
    const stmt = this.db.prepare(`
      UPDATE users SET 
        telegram_id = COALESCE(NULLIF(?, ''), telegram_id), 
        telegram_username = COALESCE(NULLIF(?, ''), telegram_username) 
      WHERE id = ?
    `);
    return stmt.run(cleanId, cleanUsername, userId);
  }

  createLinkToken(userId, token, ttlMs = 300000) {
    const cleanToken = String(token || '').trim();
    const expiresAt = Date.now() + ttlMs;
    const stmt = this.db.prepare(`
      INSERT OR REPLACE INTO telegram_link_tokens (token, user_id, expires_at)
      VALUES (?, ?, ?)
    `);
    stmt.run(cleanToken, userId, expiresAt);
    return { token: cleanToken, userId, expiresAt };
  }

  findLinkToken(token) {
    const cleanToken = String(token || '').trim();
    if (!cleanToken) return null;
    const stmt = this.db.prepare(`
      SELECT token, user_id, expires_at FROM telegram_link_tokens WHERE token = ?
    `);
    const row = stmt.get(cleanToken);
    if (!row) return null;
    if (Date.now() > row.expires_at) {
      this.consumeLinkToken(cleanToken);
      return null;
    }
    return row;
  }

  consumeLinkToken(token) {
    const cleanToken = String(token || '').trim();
    if (!cleanToken) return;
    const stmt = this.db.prepare(`DELETE FROM telegram_link_tokens WHERE token = ?`);
    stmt.run(cleanToken);
  }

  unlinkTelegram(userId) {
    const stmt = this.db.prepare(`
      UPDATE users SET telegram_id = '', telegram_username = '', two_factor_enabled = 0 WHERE id = ?
    `);
    return stmt.run(userId);
  }

  setTwoFactorEnabled(userId, enabled) {
    const stmt = this.db.prepare(`
      UPDATE users SET two_factor_enabled = ? WHERE id = ?
    `);
    return stmt.run(enabled ? 1 : 0, userId);
  }

  findUserByTelegramId(telegramId, role = null) {
    if (!telegramId) return null;
    if (role) {
      const stmt = this.db.prepare(`
        SELECT id, username, password_hash, role, full_name, phone, avatar_base64, client_uuid, coach_name, coach_phone, pairing_code, pairing_code_created_at, is_private, telegram_id, telegram_username, two_factor_enabled
        FROM users WHERE telegram_id = ? AND telegram_id != '' AND role = ?
      `);
      const user = stmt.get(String(telegramId), role);
      if (user) return user;
    }
    const stmt = this.db.prepare(`
      SELECT id, username, password_hash, role, full_name, phone, avatar_base64, client_uuid, coach_name, coach_phone, pairing_code, pairing_code_created_at, is_private, telegram_id, telegram_username, two_factor_enabled
      FROM users WHERE telegram_id = ? AND telegram_id != ''
    `);
    return stmt.get(String(telegramId)) || null;
  }

  findUserByTelegramUsername(tgUsername, role = null) {
    const clean = String(tgUsername || '').replace(/^@/, '').trim().toLowerCase();
    if (!clean) return null;
    if (role) {
      const stmt = this.db.prepare(`
        SELECT id, username, password_hash, role, full_name, phone, avatar_base64, client_uuid, coach_name, coach_phone, pairing_code, pairing_code_created_at, is_private, telegram_id, telegram_username, two_factor_enabled
        FROM users WHERE (LOWER(telegram_username) = ? OR LOWER(username) = ?) AND role = ?
      `);
      const user = stmt.get(clean, `tg_${clean}`, role);
      if (user) return user;
    }
    const stmt = this.db.prepare(`
      SELECT id, username, password_hash, role, full_name, phone, avatar_base64, client_uuid, coach_name, coach_phone, pairing_code, pairing_code_created_at, is_private, telegram_id, telegram_username, two_factor_enabled
      FROM users WHERE LOWER(telegram_username) = ? OR LOWER(username) = ?
    `);
    return stmt.get(clean, `tg_${clean}`) || null;
  }

  updateUserPassword(userId, newPasswordHash) {
    const stmt = this.db.prepare(`
      UPDATE users SET password_hash = ? WHERE id = ?
    `);
    return stmt.run(newPasswordHash, userId);
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

  isAthletePairedToTrainer(trainerId, athleteId) {
    const tId = Number(trainerId);
    const aId = Number(athleteId);
    if (!tId || !aId) return false;
    const stmt = this.db.prepare(`
      SELECT 1 FROM trainer_clients WHERE trainer_id = ? AND athlete_id = ?
    `);
    const row = stmt.get(tId, aId);
    return Boolean(row);
  }

  unpairTrainerAndAthlete(trainerId, athleteId) {
    const stmt = this.db.prepare(`
      DELETE FROM trainer_clients WHERE trainer_id = ? AND athlete_id = ?
    `);
    stmt.run(trainerId, athleteId);

    const coachClearStmt = this.db.prepare(`
      UPDATE users SET coach_name = '', coach_phone = '' WHERE id = ?
    `);
    coachClearStmt.run(athleteId);
    return true;
  }

  unpairAthleteBySelf(athleteId) {
    const stmt = this.db.prepare(`
      DELETE FROM trainer_clients WHERE athlete_id = ?
    `);
    stmt.run(athleteId);

    const coachClearStmt = this.db.prepare(`
      UPDATE users SET coach_name = '', coach_phone = '' WHERE id = ?
    `);
    coachClearStmt.run(athleteId);
    return true;
  }

  getTrainerClients(trainerId) {
    const stmt = this.db.prepare(`
      SELECT u.id, u.username, u.full_name, u.phone, u.avatar_base64, u.client_uuid, u.pairing_code, u.restrictions, tc.paired_at,
             (SELECT COUNT(*) FROM workout_sessions ws WHERE ws.athlete_id = u.id) as sessions_count,
             (SELECT COALESCE(SUM(s.weight_kg * s.reps), 0) FROM workout_sets s 
              JOIN workout_sessions ws ON s.session_id = ws.id 
              WHERE ws.athlete_id = u.id AND s.is_completed = 1) as total_tonnage
      FROM users u
      JOIN trainer_clients tc ON u.id = tc.athlete_id
      WHERE tc.trainer_id = ?
      ORDER BY tc.paired_at DESC
    `);
    return stmt.all(trainerId);
  }

  getAthleteCoach(athleteId) {
    const stmt = this.db.prepare(`
      SELECT u.id, u.username, u.full_name, u.phone, u.avatar_base64, tc.paired_at
      FROM users u
      JOIN trainer_clients tc ON u.id = tc.trainer_id
      WHERE tc.athlete_id = ?
      LIMIT 1
    `);
    return stmt.get(athleteId) || null;
  }

  // --- Workout Operations ---

  getOrCreateSession(athleteId, date) {
    const selectStmt = this.db.prepare(`
      SELECT id, athlete_id, date, notes, completed, is_self_workout_allowed, assigned_by_trainer_id
      FROM workout_sessions WHERE athlete_id = ? AND date = ?
    `);
    let session = selectStmt.get(athleteId, date);
    if (!session) {
      const insertStmt = this.db.prepare(`
        INSERT INTO workout_sessions (athlete_id, date, is_self_workout_allowed, completed)
        VALUES (?, ?, 0, 0)
      `);
      const result = insertStmt.run(athleteId, date);
      session = {
        id: Number(result.lastInsertRowid),
        athlete_id: athleteId,
        date,
        notes: '',
        completed: 0,
        is_self_workout_allowed: 0,
        assigned_by_trainer_id: null
      };
    }
    return session;
  }

  assignTrainerWorkout(trainerId, athleteId, date, isSelfAllowed = 0, notes = '') {
    const selectStmt = this.db.prepare(`
      SELECT id FROM workout_sessions WHERE athlete_id = ? AND date = ?
    `);
    let session = selectStmt.get(athleteId, date);
    if (!session) {
      const insertStmt = this.db.prepare(`
        INSERT INTO workout_sessions (athlete_id, date, is_self_workout_allowed, assigned_by_trainer_id, notes)
        VALUES (?, ?, ?, ?, ?)
      `);
      const result = insertStmt.run(athleteId, date, isSelfAllowed ? 1 : 0, trainerId, notes);
      return Number(result.lastInsertRowid);
    } else {
      const updateStmt = this.db.prepare(`
        UPDATE workout_sessions
        SET is_self_workout_allowed = ?, assigned_by_trainer_id = ?, notes = ?
        WHERE id = ?
      `);
      updateStmt.run(isSelfAllowed ? 1 : 0, trainerId, notes, session.id);
      return session.id;
    }
  }

  getWorkoutSessionWithSets(athleteId, date) {
    const sessionStmt = this.db.prepare(`
      SELECT id, athlete_id, date, notes, completed, is_self_workout_allowed, assigned_by_trainer_id
      FROM workout_sessions WHERE athlete_id = ? AND date = ?
    `);
    const session = sessionStmt.get(athleteId, date);
    if (!session) {
      return { session: null, sets: [] };
    }

    const setsStmt = this.db.prepare(`
      SELECT id, session_id, exercise_name, weight_kg, reps, rpe, is_completed,
             set_number, target_weight_kg, target_reps, actual_weight_kg, actual_reps, created_at
      FROM workout_sets WHERE session_id = ? ORDER BY id ASC
    `);
    const sets = setsStmt.all(session.id);
    return { session, sets };
  }

  getAthleteHistory(athleteId, exerciseName = null) {
    let sql = `
      SELECT s.id, ws.date, s.exercise_name, s.weight_kg, s.reps, s.rpe, s.is_completed, s.created_at
      FROM workout_sets s
      JOIN workout_sessions ws ON s.session_id = ws.id
      WHERE ws.athlete_id = ?
    `;
    const params = [athleteId];
    if (exerciseName) {
      sql += ' AND s.exercise_name = ?';
      params.push(exerciseName);
    }
    sql += ' ORDER BY ws.date DESC, s.id DESC';
    const stmt = this.db.prepare(sql);
    return stmt.all(...params);
  }

  getAthleteExercises(athleteId) {
    const stmt = this.db.prepare(`
      SELECT DISTINCT s.exercise_name
      FROM workout_sets s
      JOIN workout_sessions ws ON s.session_id = ws.id
      WHERE ws.athlete_id = ?
      ORDER BY s.exercise_name ASC
    `);
    const rows = stmt.all(athleteId);
    return rows.map(r => r.exercise_name);
  }

  getExerciseProgressTimeline(athleteId, exerciseName) {
    const stmt = this.db.prepare(`
      SELECT ws.date, s.weight_kg, s.reps, s.rpe, s.is_completed, s.id
      FROM workout_sets s
      JOIN workout_sessions ws ON s.session_id = ws.id
      WHERE ws.athlete_id = ? AND s.exercise_name = ?
      ORDER BY ws.date ASC, s.id ASC
    `);
    return stmt.all(athleteId, exerciseName);
  }

  addAnthropometry(userId, weightKg, date = null, chestCm = 0, waistCm = 0, bicepsCm = 0) {
    const entryDate = date || new Date().toISOString().slice(0, 10);
    const stmt = this.db.prepare(`
      INSERT INTO anthropometry (user_id, date, weight_kg, chest_cm, waist_cm, biceps_cm)
      VALUES (?, ?, ?, ?, ?, ?)
    `);
    const res = stmt.run(userId, entryDate, Number(weightKg) || 0, Number(chestCm) || 0, Number(waistCm) || 0, Number(bicepsCm) || 0);
    return Number(res.lastInsertRowid);
  }

  getAnthropometryHistory(userId) {
    const stmt = this.db.prepare(`
      SELECT id, user_id, date, weight_kg, chest_cm, waist_cm, biceps_cm, created_at
      FROM anthropometry
      WHERE user_id = ?
      ORDER BY date ASC, id ASC
    `);
    return stmt.all(userId);
  }

  addWorkoutSet(sessionId, exerciseName, weightKg, reps, rpe = 8.0, isCompleted = 1) {
    const stmt = this.db.prepare(`
      INSERT INTO workout_sets (session_id, exercise_name, weight_kg, reps, rpe, is_completed, actual_weight_kg, actual_reps)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?)
    `);
    const res = stmt.run(sessionId, exerciseName, weightKg, reps, rpe, isCompleted ? 1 : 0, weightKg, reps);
    return Number(res.lastInsertRowid);
  }

  toggleWorkoutSet(setId, isCompleted = null) {
    let stmt;
    if (isCompleted === null) {
      stmt = this.db.prepare(`
        UPDATE workout_sets SET is_completed = CASE WHEN is_completed = 1 THEN 0 ELSE 1 END WHERE id = ?
      `);
      stmt.run(setId);
    } else {
      stmt = this.db.prepare(`
        UPDATE workout_sets SET is_completed = ? WHERE id = ?
      `);
      stmt.run(isCompleted ? 1 : 0, setId);
    }
    return this.getWorkoutSetById(setId);
  }

  updateWorkoutSet(setId, weightKg, reps, rpe = 8.0) {
    const stmt = this.db.prepare(`
      UPDATE workout_sets 
      SET weight_kg = ?, reps = ?, rpe = ?, actual_weight_kg = ?, actual_reps = ?
      WHERE id = ?
    `);
    stmt.run(weightKg, reps, rpe, weightKg, reps, setId);
    return this.getWorkoutSetById(setId);
  }

  deleteWorkoutSet(setId) {
    const stmt = this.db.prepare(`
      DELETE FROM workout_sets WHERE id = ?
    `);
    return stmt.run(setId);
  }

  getWorkoutSetById(setId) {
    const stmt = this.db.prepare(`
      SELECT s.id, s.session_id, s.exercise_name, s.weight_kg, s.reps, s.rpe, s.is_completed, s.created_at, ws.athlete_id, ws.date AS workout_date
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

  // --- Leaderboard / Competitions (Strict Zero-Mocks, Real Finished Workouts Only) ---

  getLeaderboard() {
    const stmt = this.db.prepare(`
      SELECT u.id, u.full_name, u.avatar_base64,
             COUNT(DISTINCT ws.id) as workouts_count,
             COALESCE(SUM(CASE WHEN s.weight_kg > 15 THEN s.weight_kg - 15 ELSE 0 END), 0) as weight_gain,
             CAST(COUNT(DISTINCT ws.id) * 10 + COALESCE(SUM(CASE WHEN s.weight_kg > 15 THEN s.weight_kg - 15 ELSE 0 END), 0) AS INTEGER) as points
      FROM users u
      JOIN workout_sessions ws ON u.id = ws.athlete_id
      JOIN workout_sets s ON ws.id = s.session_id AND s.is_completed = 1
      WHERE u.role = 'athlete' AND u.is_private = 0
      GROUP BY u.id, u.full_name, u.avatar_base64
      HAVING workouts_count > 0
      ORDER BY points DESC, weight_gain DESC
    `);
    return stmt.all();
  }

  // --- Auth Token Operations ---

  createAuthToken(token, userId, ttlMs = 86400000 * 365) {
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
      SELECT u.id, u.username, u.role, u.full_name, u.phone, u.avatar_base64, u.client_uuid, u.coach_name, u.coach_phone, u.pairing_code, u.pairing_code_created_at, u.is_private, u.telegram_id, u.telegram_username, u.two_factor_enabled
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
    stmt.run(token);
  }

  getWorkoutSets(athleteId, date) {
    const session = this.getWorkoutSessionWithSets(athleteId, date);
    return session ? session.sets : [];
  }

  close() {
    try {
      this.db.close();
    } catch (_) {}
  }
}

module.exports = AppDatabase;
