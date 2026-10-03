/**
 * Fitness Ecosystem Pro - Web Server
 * Hardened REST API & Static Single-Page Application Host
 * Built with native Node.js 24 + node:sqlite.
 */

const http = require('node:http');
const fs = require('node:fs');
const path = require('node:path');
const { URL } = require('node:url');
const AppDatabase = require('./db');
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
      if (body.length > 1024 * 1024) {
        reject(new Error('Payload too large'));
      }
    });
    req.on('end', () => {
      try {
        resolve(body ? JSON.parse(body) : {});
      } catch (e) {
        reject(new Error('Invalid JSON format'));
      }
    });
    req.on('error', reject);
  });
}

function getAuthUser(req) {
  const authHeader = req.headers['authorization'] || '';
  const token = authHeader.replace(/^Bearer\s+/i, '').trim();
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
      // Rate Limit Auth Endpoints
      if (pathname === '/api/login' || pathname === '/api/register') {
        if (authLimiter.isRateLimited(clientIp)) {
          return sendError(res, 429, 'Слишком много попыток входа. Попробуйте через минуту.');
        }
      }

      // REGISTER
      if (pathname === '/api/register' && req.method === 'POST') {
        const body = await parseJsonBody(req);
        const { username, password, role, fullName, phone } = body;

        // Anti-Injection & Strict Validation
        if (!username || !password || !role || !fullName) {
          return sendError(res, 400, 'Заполните все обязательные поля');
        }
        if (hasSqlInjectionVector(username) || hasSqlInjectionVector(fullName)) {
          return sendError(res, 400, 'Обнаружены недопустимые символы или попытка инъекции.');
        }
        if (!VALIDATION_PATTERNS.username.test(username)) {
          return sendError(res, 400, 'Логин должен содержать от 3 до 30 латинских букв или цифр');
        }
        if (typeof password !== 'string' || password.length < 6) {
          return sendError(res, 400, 'Пароль должен быть не короче 6 символов');
        }
        if (role !== 'athlete' && role !== 'trainer') {
          return sendError(res, 400, 'Роль должна быть athlete или trainer');
        }

        const existing = db.findUserByUsername(username);
        if (existing) {
          return sendError(res, 409, 'Пользователь с таким логином уже существует');
        }

        // Clean PIN for athlete (6 digits)
        let pairingCode = '';
        if (role === 'athlete') {
          pairingCode = String(Math.floor(100000 + Math.random() * 900000));
        }

        const passwordHash = hashPassword(password);
        const cleanFullName = escapeHtml(fullName.trim());
        const cleanPhone = phone ? escapeHtml(phone.trim()) : '';

        const userId = db.createUser(username, passwordHash, role, cleanFullName, cleanPhone, pairingCode);
        const token = generateToken();
        db.createAuthToken(token, userId);

        return sendJson(res, 201, {
          success: true,
          token,
          user: { id: userId, username, role, fullName: cleanFullName, phone: cleanPhone, pairingCode }
        });
      }

      // LOGIN
      if (pathname === '/api/login' && req.method === 'POST') {
        const body = await parseJsonBody(req);
        const { username, password } = body;

        if (!username || !password) {
          return sendError(res, 400, 'Введите логин и пароль');
        }
        if (hasSqlInjectionVector(username)) {
          return sendError(res, 400, 'Некорректный логин');
        }

        const user = db.findUserByUsername(username);
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
            pairingCode: user.pairing_code,
            isPrivate: Boolean(user.is_private)
          }
        });
      }

      // LEADERBOARD (Public Competitions - Zero Mocks)
      if (pathname === '/api/leaderboard' && req.method === 'GET') {
        const leaderboard = db.getLeaderboard();
        return sendJson(res, 200, { leaderboard });
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
          pairedCoach = db.getPairedTrainer(user.id);
        }
        return sendJson(res, 200, { user, pairedCoach });
      }

      // LOGOUT
      if (pathname === '/api/logout' && req.method === 'POST') {
        const token = (req.headers['authorization'] || '').replace(/^Bearer\s+/i, '').trim();
        if (token) db.deleteAuthToken(token);
        return sendJson(res, 200, { success: true });
      }

      // SWITCH USER ROLE
      if (pathname === '/api/user/role' && req.method === 'POST') {
        const body = await parseJsonBody(req);
        const { role: newRole } = body;
        if (newRole !== 'athlete' && newRole !== 'trainer') {
          return sendError(res, 400, 'Роль должна быть athlete или trainer');
        }
        db.updateUserRole(user.id, newRole);
        let pairingCode = user.pairing_code;
        if (newRole === 'athlete' && (!pairingCode || pairingCode.length !== 6)) {
          pairingCode = String(Math.floor(100000 + Math.random() * 900000));
          db.regeneratePairingCode(user.id, pairingCode);
        }
        const updatedUser = db.findUserById(user.id);
        return sendJson(res, 200, {
          success: true,
          user: {
            id: updatedUser.id,
            username: updatedUser.username,
            role: updatedUser.role,
            fullName: updatedUser.full_name,
            phone: updatedUser.phone,
            pairingCode: updatedUser.pairing_code,
            isPrivate: Boolean(updatedUser.is_private)
          }
        });
      }

      // REGENERATE PIN (Athlete)
      if (pathname === '/api/athlete/regenerate-pin' && req.method === 'POST') {
        if (user.role !== 'athlete') return sendError(res, 403, 'Доступно только атлетам');
        const newPin = String(Math.floor(100000 + Math.random() * 900000));
        db.regeneratePairingCode(user.id, newPin);
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
        db.unpairAthlete(user.id);
        return sendJson(res, 200, { success: true, message: 'Связь с тренером разорвана' });
      }

      // TRAINER: PAIR ATHLETE BY 6-DIGIT CODE
      if (pathname === '/api/trainer/pair' && req.method === 'POST') {
        if (user.role !== 'trainer') return sendError(res, 403, 'Доступно только тренерам');
        const body = await parseJsonBody(req);
        const rawCode = String(body.code || '').replace(/\D/g, ''); // Extract only 6 digits
        if (!VALIDATION_PATTERNS.pairingCode.test(rawCode)) {
          return sendError(res, 400, 'Код должен содержать ровно 6 цифр');
        }

        const athlete = db.findUserByPairingCode(rawCode);
        if (!athlete) {
          return sendError(res, 404, 'Подопечный с таким кодом не найден');
        }

        db.pairTrainerAndAthlete(user.id, athlete.id);
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
        db.unpairTrainerClient(user.id, athleteId);
        return sendJson(res, 200, { success: true, message: 'Связь с атлетом разорвана' });
      }

      // TRAINER: GET CLIENTS
      if (pathname === '/api/trainer/clients' && req.method === 'GET') {
        if (user.role !== 'trainer') return sendError(res, 403, 'Доступно только тренерам');
        const clients = db.getTrainerClients(user.id);
        return sendJson(res, 200, { clients });
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
        
        const sets = db.getWorkoutSets(targetAthleteId, date);
        return sendJson(res, 200, { sets, date });
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

        const cleanExercise = escapeHtml(String(exerciseName).trim());
        const weight = Math.max(0, Number(weightKg) || 0);
        const repCount = Math.max(1, Number(reps) || 1);
        const rpeVal = Math.min(10, Math.max(1, Number(rpe) || 8.0));
        const workoutDate = date || new Date().toISOString().slice(0, 10);

        const setId = db.addWorkoutSet(targetAthleteId, workoutDate, cleanExercise, weight, repCount, rpeVal);
        return sendJson(res, 201, { success: true, setId });
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
    } catch {
      safePathname = pathname;
    }
    safePathname = safePathname.replace(/\0/g, '');

    if (safePathname.includes('..')) {
      res.writeHead(403);
      return res.end('Access Denied');
    }

    let filePath = path.join(PUBLIC_DIR, safePathname === '/' ? 'index.html' : safePathname);
    
    // Path Traversal Defense
    const normalized = path.normalize(filePath);
    if (!normalized.startsWith(PUBLIC_DIR)) {
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
