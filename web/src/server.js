/**
 * Fitness Ecosystem Pro - Hardened Web Server
 * REST API & Mobile Parity Single-Page Application Host
 * Built with native Node.js 24 + node:sqlite.
 */

const http = require('node:http');
const fs = require('node:fs');
const path = require('node:path');
const { URL } = require('node:url');
const crypto = require('node:crypto');
const AppDatabase = require('./db');
const { cloudSyncService } = require('./cloudSync');
const {
  escapeHtml,
  VALIDATION_PATTERNS,
  hasSqlInjectionVector,
  hashPassword,
  verifyPassword,
  generateToken,
  RateLimiter,
  SECURITY_HEADERS
} = require('./security');

const PORT = process.env.PORT || 3000;
const PUBLIC_DIR = path.join(__dirname, 'public');
const db = new AppDatabase();
const authLimiter = new RateLimiter(60000, 15); // Max 15 auth attempts/min per IP

const MIME_TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.png': 'image/png',
  '.svg': 'image/svg+xml'
};

function sendJson(res, statusCode, data) {
  res.writeHead(statusCode, {
    'Content-Type': 'application/json; charset=utf-8',
    ...SECURITY_HEADERS
  });
  res.end(JSON.stringify(data));
}

function sendError(res, statusCode, message, details = null) {
  sendJson(res, statusCode, { error: message, details });
}

function parseJsonBody(req) {
  return new Promise((resolve, reject) => {
    let body = '';
    req.on('data', chunk => {
      body += chunk;
      if (body.length > 5 * 1024 * 1024) { // 5MB max payload (avatars, sets)
        reject(new Error('Payload too large'));
      }
    });
    req.on('end', () => {
      try {
        resolve(body ? JSON.parse(body) : {});
      } catch (err) {
        reject(new Error('Invalid JSON payload'));
      }
    });
    req.on('error', reject);
  });
}

function getAuthUser(req) {
  const authHeader = req.headers['authorization'];
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return null;
  }
  const token = authHeader.slice(7).trim();
  if (!token) return null;
  return db.getUserByToken(token);
}

const server = http.createServer(async (req, res) => {
  const clientIp = req.socket.remoteAddress || '127.0.0.1';
  const reqUrl = new URL(req.url, `http://${req.headers.host || 'localhost'}`);
  const pathname = reqUrl.pathname;

  // Apply Security Headers to all requests
  for (const [header, val] of Object.entries(SECURITY_HEADERS)) {
    res.setHeader(header, val);
  }

  try {
    // --- 1. API ROUTES ---
    if (pathname.startsWith('/api/')) {
      // Rate Limit Auth Endpoints (only enabled if process.env.ENABLE_AUTH_LIMIT is set)
      if (process.env.ENABLE_AUTH_LIMIT === 'true') {
        if (pathname === '/api/login' || pathname === '/api/register') {
          if (authLimiter.isRateLimited(clientIp)) {
            return sendError(res, 429, 'Слишком много попыток входа. Попробуйте через минуту.');
          }
        }
      }

      // REGISTER
      if (pathname === '/api/register' && req.method === 'POST') {
        const body = await parseJsonBody(req);
        const { username, password, role, fullName, phone, avatarBase64 } = body;
        const cleanUsername = String(username || '').trim();
        const cleanFullName = String(fullName || '').trim();
        const cleanPhone = phone ? String(phone).trim() : '';

        // Anti-Injection & Strict Validation
        if (!cleanUsername || !password || !role || !cleanFullName) {
          return sendError(res, 400, 'Заполните все обязательные поля');
        }
        if (hasSqlInjectionVector(cleanUsername) || hasSqlInjectionVector(cleanFullName)) {
          return sendError(res, 400, 'Обнаружены недопустимые символы или попытка инъекции.');
        }
        if (!VALIDATION_PATTERNS.username.test(cleanUsername)) {
          return sendError(res, 400, 'Логин должен содержать от 3 до 30 букв или цифр (латиница или кириллица)');
        }
        if (typeof password !== 'string' || password.length < 6) {
          return sendError(res, 400, 'Пароль должен быть не короче 6 символов');
        }
        if (role !== 'athlete' && role !== 'trainer') {
          return sendError(res, 400, 'Роль должна быть athlete или trainer');
        }

        const existing = db.findUserByUsername(cleanUsername);
        if (existing) {
          return sendError(res, 409, 'Пользователь с таким логином уже существует');
        }

        // Clean PIN for athlete (strictly 6 digits) and clientUuid
        let pairingCode = '';
        let clientUuid = crypto.randomUUID();
        if (role === 'athlete') {
          pairingCode = String(Math.floor(100000 + Math.random() * 900000));
        }

        const passwordHash = hashPassword(password);
        const escapedFullName = escapeHtml(cleanFullName);
        const escapedPhone = cleanPhone ? escapeHtml(cleanPhone) : '';

        const userId = db.createUser(cleanUsername, passwordHash, role, escapedFullName, escapedPhone, pairingCode, clientUuid, avatarBase64 || '');
        const token = generateToken();
        db.createAuthToken(token, userId);

        // Async sync athlete pairing code to Google Drive cloud
        if (role === 'athlete') {
          cloudSyncService.registerAthletePairing(pairingCode, clientUuid, escapedFullName, escapedPhone).catch(err => {
            console.warn('[Server] Cloud sync registration notice:', err.message);
          });
        }

        return sendJson(res, 201, {
          success: true,
          token,
          user: { id: userId, username: cleanUsername, role, fullName: escapedFullName, phone: escapedPhone, pairingCode, clientUuid, avatarBase64: avatarBase64 || '' }
        });
      }

      // LOGIN
      if (pathname === '/api/login' && req.method === 'POST') {
        const body = await parseJsonBody(req);
        const { username, password } = body;
        const cleanUsername = String(username || '').trim();

        if (!cleanUsername || !password) {
          return sendError(res, 400, 'Введите логин и пароль');
        }
        if (hasSqlInjectionVector(cleanUsername)) {
          return sendError(res, 400, 'Некорректный логин');
        }

        const user = db.findUserByUsername(cleanUsername);
        if (!user || !verifyPassword(password, user.password_hash)) {
          return sendError(res, 401, 'Неверный логин или пароль');
        }

        const token = generateToken();
        db.createAuthToken(token, user.id);

        return sendJson(res, 200, {
          success: true,
          token,
          user: {
            id: user.id,
            username: user.username,
            role: user.role,
            fullName: user.full_name,
            phone: user.phone,
            avatarBase64: user.avatar_base64 || '',
            pairingCode: user.pairing_code,
            clientUuid: user.client_uuid || '',
            coachName: user.coach_name || '',
            coachPhone: user.coach_phone || '',
            isPrivate: Boolean(user.is_private)
          }
        });
      }

      // LEADERBOARD (Public Competitions - 100% Zero-Mocks, Real Athletes Only)
      if (pathname === '/api/leaderboard' && req.method === 'GET') {
        try {
          const localEntries = db.getLeaderboard();
          const combined = await cloudSyncService.getCombinedLeaderboard(localEntries);
          return sendJson(res, 200, { leaderboard: combined });
        } catch (_) {
          const leaderboard = db.getLeaderboard();
          return sendJson(res, 200, { leaderboard });
        }
      }

      // AUTHENTICATED ENDPOINTS
      const user = getAuthUser(req);
      if (!user) {
        return sendError(res, 401, 'Требуется авторизация');
      }

      // CURRENT USER PROFILE
      if (pathname === '/api/me' && req.method === 'GET') {
        let pairedCoach = null;
        if (user.role === 'athlete') {
          pairedCoach = db.getAthleteCoach(user.id);
        }
        return sendJson(res, 200, { user, pairedCoach });
      }

      // UPDATE PROFILE (Name, Phone, Photo/Avatar)
      if (pathname === '/api/user/profile' && (req.method === 'PUT' || req.method === 'POST')) {
        const body = await parseJsonBody(req);
        const { fullName, phone, avatarBase64 } = body;
        const cleanName = String(fullName || user.full_name).trim();
        const cleanPhone = phone !== undefined ? String(phone).trim() : user.phone;

        if (cleanName.length < 2) {
          return sendError(res, 400, 'Имя должно содержать минимум 2 символа');
        }
        if (hasSqlInjectionVector(cleanName)) {
          return sendError(res, 400, 'Некорректное имя');
        }

        const escapedName = escapeHtml(cleanName);
        const escapedPhone = escapeHtml(cleanPhone);

        db.updateProfile(user.id, escapedName, escapedPhone, avatarBase64 !== undefined ? avatarBase64 : null);
        const updatedUser = db.findUserById(user.id);

        if (updatedUser.role === 'athlete' && updatedUser.pairing_code) {
          cloudSyncService.registerAthletePairing(
            updatedUser.pairing_code,
            updatedUser.client_uuid,
            updatedUser.full_name,
            updatedUser.phone
          ).catch(() => {});
        }

        return sendJson(res, 200, {
          success: true,
          user: {
            id: updatedUser.id,
            username: updatedUser.username,
            role: updatedUser.role,
            fullName: updatedUser.full_name,
            phone: updatedUser.phone,
            avatarBase64: updatedUser.avatar_base64,
            pairingCode: updatedUser.pairing_code,
            isPrivate: Boolean(updatedUser.is_private)
          }
        });
      }

      // LOGOUT
      if (pathname === '/api/logout' && req.method === 'POST') {
        const token = (req.headers['authorization'] || '').replace(/^Bearer\s+/i, '').trim();
        if (token) db.deleteAuthToken(token);
        return sendJson(res, 200, { success: true });
      }

      // SWITCH USER ROLE (FORBIDDEN - roles are strictly immutable as in mobile apps)
      if (pathname === '/api/user/role' && req.method === 'POST') {
        return sendError(res, 403, 'Смена роли запрещена: права строго фиксированы как в мобильном приложении');
      }

      // REGENERATE PIN (Athlete)
      if (pathname === '/api/athlete/regenerate-pin' && req.method === 'POST') {
        if (user.role !== 'athlete') return sendError(res, 403, 'Доступно только атлетам');
        const newPin = String(Math.floor(100000 + Math.random() * 900000));
        db.regeneratePairingCode(user.id, newPin);

        cloudSyncService.registerAthletePairing(newPin, user.client_uuid, user.full_name, user.phone).catch(() => {});

        return sendJson(res, 200, { success: true, pairingCode: newPin });
      }

      // UPDATE PRIVACY (Athlete)
      if (pathname === '/api/athlete/privacy' && req.method === 'POST') {
        if (user.role !== 'athlete') return sendError(res, 403, 'Доступно только атлетам');
        const body = await parseJsonBody(req);
        db.updateAthletePrivacy(user.id, Boolean(body.isPrivate));
        return sendJson(res, 200, { success: true, isPrivate: Boolean(body.isPrivate) });
      }

      // UNPAIR TRAINER (Athlete)
      if (pathname === '/api/athlete/unpair' && req.method === 'POST') {
        if (user.role !== 'athlete') return sendError(res, 403, 'Доступно только атлетам');
        db.unpairAthleteBySelf(user.id);
        cloudSyncService.unpairAthlete(user.pairing_code, user.client_uuid).catch(() => {});
        return sendJson(res, 200, { success: true, message: 'Связь с тренером разорвана' });
      }

      // TRAINER: PAIR ATHLETE BY 6-DIGIT CODE (Local + Google Drive Cloud Sync)
      if (pathname === '/api/trainer/pair' && req.method === 'POST') {
        if (user.role !== 'trainer') return sendError(res, 403, 'Доступно только тренерам');
        const body = await parseJsonBody(req);
        const rawCode = String(body.code || '').replace(/\D/g, ''); // Extract strictly 6 digits
        if (!VALIDATION_PATTERNS.pairingCode.test(rawCode)) {
          return sendError(res, 400, 'Код должен содержать ровно 6 цифр');
        }

        let athlete = db.findUserByPairingCode(rawCode);

        // If not in local SQLite, query Google Drive Cloud
        if (!athlete) {
          try {
            const cloudAthlete = await cloudSyncService.findAndPairAthlete(rawCode, user.full_name, user.phone);
            if (cloudAthlete) {
              let localUser = db.findUserByClientUuid(cloudAthlete.clientUuid);
              if (!localUser) {
                const uniqueUsername = 'ath_' + cloudAthlete.clientUuid.slice(0, 8);
                const uid = db.createUser(
                  uniqueUsername,
                  hashPassword(crypto.randomBytes(16).toString('hex')),
                  'athlete',
                  cloudAthlete.clientName || 'Подопечный',
                  cloudAthlete.phone || '',
                  rawCode,
                  cloudAthlete.clientUuid
                );
                localUser = db.findUserById(uid);
              }
              athlete = localUser;
            }
          } catch (err) {
            console.error('[Server] Cloud pairing lookup error:', err.message);
          }
        }

        if (!athlete) {
          return sendError(res, 404, 'Подопечный с таким кодом не найден ни локально, ни в облаке');
        }

        db.pairTrainerAndAthlete(user.id, athlete.id);
        db.updateCoachInfo(athlete.id, user.full_name, user.phone);

        return sendJson(res, 200, {
          success: true,
          message: `Подопечный ${athlete.full_name} успешно привязан`,
          athlete: { id: athlete.id, fullName: athlete.full_name, phone: athlete.phone }
        });
      }

      // TRAINER: UNPAIR ATHLETE
      if (pathname === '/api/trainer/unpair' && req.method === 'POST') {
        if (user.role !== 'trainer') return sendError(res, 403, 'Доступно только тренерам');
        const body = await parseJsonBody(req);
        const athleteId = Number(body.athleteId);
        if (!athleteId) {
          return sendError(res, 400, 'Укажите athleteId');
        }
        const athlete = db.findUserById(athleteId);
        if (athlete && athlete.pairing_code) {
          cloudSyncService.unpairAthlete(athlete.pairing_code, athlete.client_uuid).catch(() => {});
        }
        db.unpairTrainerAndAthlete(user.id, athleteId);
        return sendJson(res, 200, { success: true, message: 'Связь с атлетом разорвана' });
      }

      // TRAINER: GET CLIENTS
      if (pathname === '/api/trainer/clients' && req.method === 'GET') {
        if (user.role !== 'trainer') return sendError(res, 403, 'Доступно только тренерам');
        const clients = db.getTrainerClients(user.id);
        return sendJson(res, 200, { clients });
      }

      // TRAINER: ASSIGN WORKOUT TO ATHLETE
      if (pathname === '/api/trainer/assign-workout' && req.method === 'POST') {
        if (user.role !== 'trainer') return sendError(res, 403, 'Доступно только тренерам');
        const body = await parseJsonBody(req);
        const { athleteId, date, isSelfAllowed, notes, exercises } = body;
        if (!athleteId || !date) {
          return sendError(res, 400, 'Укажите athleteId и дату тренировки');
        }

        const sessionId = db.assignTrainerWorkout(user.id, Number(athleteId), date, isSelfAllowed ? 1 : 0, notes || '');

        if (Array.isArray(exercises)) {
          for (const ex of exercises) {
            if (ex.exerciseName) {
              const weight = Math.max(0, Number(ex.weightKg) || 0);
              const reps = Math.max(1, Number(ex.reps) || 1);
              db.addWorkoutSet(sessionId, escapeHtml(ex.exerciseName), weight, reps, 8.0, 0);
            }
          }
        }

        // Push to Google Drive cloud for mobile app sync
        const athlete = db.findUserById(Number(athleteId));
        if (athlete && athlete.client_uuid) {
          const { sets } = db.getWorkoutSessionWithSets(athlete.id, date);
          const cloudPayload = [{
            date,
            completed: false,
            notes: notes || '',
            isSelfWorkoutAllowed: Boolean(isSelfAllowed),
            exercises: sets.map((s, idx) => ({
              exerciseId: idx + 1,
              name: s.exercise_name,
              muscleGroup: 'Общая',
              sets: [{
                setNumber: idx + 1,
                targetWeightKg: s.weight_kg,
                targetReps: s.reps,
                actualWeightKg: 0,
                actualReps: 0,
                isCompleted: false
              }]
            }))
          }];
          cloudSyncService.pushAssignedWorkouts(athlete.client_uuid, athlete.full_name, cloudPayload).catch(() => {});
        }

        return sendJson(res, 200, { success: true, sessionId });
      }

      // TRAINER: EXERCISE HISTORY
      if (pathname === '/api/trainer/exercise-history' && req.method === 'GET') {
        if (user.role !== 'trainer') return sendError(res, 403, 'Доступно только тренерам');
        const athleteId = Number(reqUrl.searchParams.get('athleteId'));
        const exerciseName = reqUrl.searchParams.get('exercise') || '';
        if (!athleteId || !exerciseName) {
          return sendError(res, 400, 'Укажите athleteId и exercise');
        }
        const stats = db.getLastExerciseStats(athleteId, exerciseName);
        return sendJson(res, 200, { stats });
      }

      // WORKOUT: GET SETS FOR DATE
      if (pathname === '/api/workout' && req.method === 'GET') {
        const targetAthleteId = user.role === 'athlete' 
          ? user.id 
          : Number(reqUrl.searchParams.get('athleteId') || user.id);
        const date = reqUrl.searchParams.get('date') || new Date().toISOString().slice(0, 10);
        
        const { session, sets } = db.getWorkoutSessionWithSets(targetAthleteId, date);
        const isSelfAllowed = session ? Boolean(session.is_self_workout_allowed) : (user.role === 'trainer');
        return sendJson(res, 200, { session, sets, date, isSelfAllowed });
      }

      // WORKOUT: ADD SET
      if (pathname === '/api/workout/set' && req.method === 'POST') {
        const body = await parseJsonBody(req);
        const targetAthleteId = user.role === 'athlete'
          ? user.id
          : Number(body.athleteId || user.id);
        
        const { date, exerciseName, weightKg, reps, rpe } = body;
        if (!exerciseName || weightKg == null || reps == null) {
          return sendError(res, 400, 'Заполните название, вес и повторения');
        }
        if (hasSqlInjectionVector(exerciseName)) {
          return sendError(res, 400, 'Недопустимые символы в названии');
        }

        const workoutDate = date || new Date().toISOString().slice(0, 10);
        const session = db.getOrCreateSession(targetAthleteId, workoutDate);

        // Strict Athlete Permission Check:
        // "атлет не может создавать тренеровки только отмечать если тренер разрешил на этот день"
        if (user.role === 'athlete' && session.assigned_by_trainer_id !== null && !session.is_self_workout_allowed) {
          return sendError(res, 403, 'Добавление упражнений заблокировано тренером. Атлет может только отмечать выполнение подходов.');
        }

        const cleanExercise = escapeHtml(String(exerciseName).trim());
        const weight = Math.max(0, Number(weightKg) || 0);
        const repCount = Math.max(1, Number(reps) || 1);
        const rpeVal = Math.min(10, Math.max(1, Number(rpe) || 8.0));

        const setId = db.addWorkoutSet(session.id, cleanExercise, weight, repCount, rpeVal, 1);
        return sendJson(res, 201, { success: true, setId, sessionId: session.id });
      }

      // WORKOUT: TOGGLE SET COMPLETION
      if (pathname === '/api/workout/set/toggle' && req.method === 'POST') {
        const body = await parseJsonBody(req);
        const setId = Number(body.setId);
        if (!setId) {
          return sendError(res, 400, 'Укажите setId');
        }
        const targetSet = db.getWorkoutSetById(setId);
        if (!targetSet) {
          return sendError(res, 404, 'Подход не найден');
        }
        if (user.role === 'athlete' && targetSet.athlete_id !== user.id) {
          return sendError(res, 403, 'Доступ запрещен');
        }
        const isCompleted = body.isCompleted !== undefined ? Boolean(body.isCompleted) : !Boolean(targetSet.is_completed);
        db.toggleWorkoutSet(setId, isCompleted);
        return sendJson(res, 200, { success: true, setId, isCompleted });
      }

      // WORKOUT: DELETE SET
      if (pathname === '/api/workout/set' && req.method === 'DELETE') {
        let setId = Number(reqUrl.searchParams.get('setId'));
        if (!setId) {
          const body = await parseJsonBody(req).catch(() => ({}));
          setId = Number(body.setId);
        }
        if (!setId) {
          return sendError(res, 400, 'Укажите setId');
        }
        const targetSet = db.getWorkoutSetById(setId);
        if (!targetSet) {
          return sendError(res, 404, 'Подход не найден');
        }
        if (user.role === 'athlete' && targetSet.athlete_id !== user.id) {
          return sendError(res, 403, 'Доступ запрещен');
        }
        db.deleteWorkoutSet(setId);
        return sendJson(res, 200, { success: true, setId });
      }

      return sendError(res, 404, 'API endpoint not found');
    }

    // --- 2. STATIC FILES SERVING ---
    // Immediate path traversal defense on raw URL
    if (req.url.includes('..') || pathname.includes('..')) {
      res.writeHead(403);
      return res.end('Access Denied');
    }

    let safePathname;
    try {
      safePathname = decodeURIComponent(pathname);
    } catch (_) {
      res.writeHead(400);
      return res.end('Bad Request');
    }

    if (safePathname.includes('\0') || safePathname.includes('..')) {
      res.writeHead(403);
      return res.end('Access Denied');
    }

    // Direct SQLite database defense
    if (safePathname.endsWith('.sqlite') || safePathname.endsWith('.db')) {
      res.writeHead(403);
      return res.end('Access Denied');
    }

    let relPath = safePathname.replace(/^\/+/, '');
    if (!relPath) relPath = 'index.html';

    const filePath = path.resolve(PUBLIC_DIR, relPath);
    if (!filePath.startsWith(PUBLIC_DIR)) {
      res.writeHead(403);
      return res.end('Access Denied');
    }

    if (fs.existsSync(filePath) && fs.statSync(filePath).isFile()) {
      const ext = path.extname(filePath).toLowerCase();
      const contentType = MIME_TYPES[ext] || 'application/octet-stream';
      res.writeHead(200, { 'Content-Type': contentType });
      fs.createReadStream(filePath).pipe(res);
    } else {
      // If a specific asset file (.json, .css, .js, etc.) is missing, return 404
      const requestedExt = path.extname(safePathname);
      if (requestedExt && requestedExt !== '.html') {
        res.writeHead(404);
        return res.end('Not Found');
      }

      // Fallback to SPA index.html for page routes
      const indexPath = path.join(PUBLIC_DIR, 'index.html');
      if (fs.existsSync(indexPath)) {
        res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
        fs.createReadStream(indexPath).pipe(res);
      } else {
        res.writeHead(404);
        res.end('Not Found');
      }
    }
  } catch (err) {
    console.error('Server error:', err);
    sendError(res, 500, 'Internal Server Error');
  }
});

if (require.main === module) {
  server.listen(PORT, () => {
    console.log(`[Fitness Ecosystem Web] Server running at http://localhost:${PORT}`);
  });
}

module.exports = { server, db, authLimiter };
