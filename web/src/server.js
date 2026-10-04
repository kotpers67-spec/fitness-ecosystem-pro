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
const telegramOtpStore = new Map(); // key: username -> { code, expiresAt, attempts }
const userTgChatMap = new Map(); // key: username -> chatId

// Real Telegram Bot Instance (grammY)
let activeBotUsername = process.env.BOT_USERNAME || '';
const { initTelegramBot, send2FAOtp, createOwnersKeyboard, OWNER_LINKS } = require('./bot');

let tgBotInstance = initTelegramBot({
  token: process.env.BOT_TOKEN,
  db,
  telegramOtpStore,
  userTgChatMap
});

const RELEASES_DIR = path.resolve(__dirname, '../../releases');

const MIME_TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.png': 'image/png',
  '.svg': 'image/svg+xml',
  '.apk': 'application/vnd.android.package-archive',
  '.webmanifest': 'application/manifest+json'
};

function sendJson(res, statusCode, data) {
  const headers = {
    'Content-Type': 'application/json; charset=utf-8',
    ...SECURITY_HEADERS
  };
  if (data && data.token) {
    headers['Set-Cookie'] = `fit_token=${data.token}; Path=/; Max-Age=31536000; SameSite=Lax`;
  }
  res.writeHead(statusCode, headers);
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
  let token = null;
  const authHeader = req.headers['authorization'];
  if (authHeader && authHeader.startsWith('Bearer ')) {
    token = authHeader.slice(7).trim();
  } else if (req.headers['cookie']) {
    const match = req.headers['cookie'].match(/(?:^|;\s*)fit_token=([^;]+)/);
    if (match) token = match[1];
  }
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
      // Rate Limit Auth Endpoints (OWASP Brute-Force & Credential Stuffing Defense)
      if (pathname === '/api/login' || pathname === '/api/register' || pathname === '/api/auth/telegram') {
        if (authLimiter.isRateLimited(clientIp)) {
          return sendError(res, 429, 'Слишком много попыток входа. Попробуйте через минуту.');
        }
      }

      // REGISTER
      if (pathname === '/api/register' && req.method === 'POST') {
        const body = await parseJsonBody(req);
        const { username, password, role, fullName, phone, avatarBase64, telegram } = body;
        const cleanUsername = String(username || '').trim();
        const cleanFullName = String(fullName || '').trim();
        const cleanPhone = phone ? String(phone).trim() : '';
        const cleanTelegram = telegram ? String(telegram).trim() : '';

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

        const requireTrainerApproval = process.env.REQUIRE_TRAINER_APPROVAL === 'true';
        const isApproved = (role === 'trainer' && requireTrainerApproval) ? 0 : 1;
        const userId = db.createUser(cleanUsername, passwordHash, role, escapedFullName, escapedPhone, pairingCode, clientUuid, avatarBase64 || '', isApproved);

        if (role === 'trainer') {
          if (cleanTelegram) {
            const tgHandle = cleanTelegram.replace(/^@/, '').toLowerCase();
            db.linkTelegram(userId, 'tg_' + tgHandle, tgHandle);
          }

          const createdUser = db.findUserById(userId);
          const internalUsername = createdUser ? createdUser.username : cleanUsername;

          // Send notification to owner/admin via Telegram
          if (tgBotInstance) {
            for (const [uName, cId] of userTgChatMap.entries()) {
              try {
                await tgBotInstance.api.sendMessage(
                  cId,
                  `🔔 <b>НОВАЯ ЗАЯВКА НА АККАУНТ ТРЕНЕРА!</b>\n\n` +
                  `👤 <b>ФИО:</b> ${escapedFullName}\n` +
                  `🏷 <b>Логин:</b> ${internalUsername}\n` +
                  `📞 <b>Телефон:</b> ${escapedPhone || 'Не указан'}\n` +
                  `✈ <b>Telegram:</b> ${cleanTelegram || 'Не указан'}\n\n` +
                  `Для подтверждения отправьте команду:\n<code>/approve_${userId}</code>`,
                  { parse_mode: 'HTML' }
                );
              } catch (_) {}
            }
          }

          if (requireTrainerApproval) {
            return sendJson(res, 201, {
              success: true,
              pendingApproval: true,
              message: '⏳ ЗАЯВКА НА РАССМОТРЕНИИ\nВаша заявка на создание аккаунта тренера принята!\n\nВ течение 72 часов ваша заявка будет обработана, мы свяжемся если будет необходима дополнительная информация.'
            });
          }
        }

        const token = generateToken();
        db.createAuthToken(token, userId);

        // Sync athlete pairing code to Google Drive cloud
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

        if (user && user.role === 'trainer' && user.is_approved === 0) {
          return sendError(res, 403, '⏳ ЗАЯВКА НА РАССМОТРЕНИИ\nВаша заявка на создание аккаунта тренера принята!\n\nВ течение 72 часов ваша заявка будет обработана, мы свяжемся если будет необходима дополнительная информация.');
        }

        if (!user || !verifyPassword(password, user.password_hash)) {
          return sendError(res, 401, 'Неверный логин или пароль');
        }

        // 2FA Authentication Check
        if (user.two_factor_enabled === 1) {
          telegramOtpStore.delete(`2fa_${user.id}`); // Invalidate any prior 2FA code
          const otp = String(Math.floor(100000 + Math.random() * 900000));
          const expiresAt = Date.now() + 5 * 60 * 1000;
          telegramOtpStore.set(`2fa_${user.id}`, {
            userId: user.id,
            code: otp,
            expiresAt,
            attempts: 0
          });
          console.log(`[2FA Security] Generated 5-minute OTP for user #${user.id} (${user.username}): ${otp}`);

          // Deliver 6-digit OTP code to user's Telegram via bot.api.sendMessage
          if (tgBotInstance && user.telegram_id) {
            send2FAOtp(tgBotInstance, user.telegram_id, otp).catch(err => {
              console.warn('[2FA] Telegram OTP delivery warning:', err.message);
            });
          }

          return sendJson(res, 200, {
            success: true,
            require2FA: true,
            userId: user.id,
            message: 'Требуется ввод 6-значного кода 2FA из Telegram (действует 5 минут)',
            expiresInSeconds: 300,
            debugCode: (process.env.NODE_ENV === 'test' || !process.env.BOT_TOKEN) ? otp : undefined
          });
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
            pairingCodeCreatedAt: user.pairing_code_created_at || 0,
            clientUuid: user.client_uuid || '',
            coachName: user.coach_name || '',
            coachPhone: user.coach_phone || '',
            isPrivate: Boolean(user.is_private),
            telegramId: user.telegram_id || '',
            telegramUsername: user.telegram_username || '',
            twoFactorEnabled: Boolean(user.two_factor_enabled)
          }
        });
      }

      // 2FA LOGIN VERIFICATION (Verify 6-digit OTP within 5 minutes)
      if (pathname === '/api/login/2fa' && req.method === 'POST') {
        const body = await parseJsonBody(req);
        const { userId, code } = body;
        const uid = Number(userId);
        const cleanCode = String(code || '').trim();

        if (!uid || !cleanCode) {
          return sendError(res, 400, 'Укажите userId и 6-значный код');
        }

        const record = telegramOtpStore.get(`2fa_${uid}`);
        if (!record) {
          return sendError(res, 400, 'Код 2FA не запрашивался или срок действия (5 минут) истек');
        }

        if (Date.now() > record.expiresAt) {
          telegramOtpStore.delete(`2fa_${uid}`);
          return sendError(res, 400, 'Срок действия 2FA кода истек (5 минут). Войдите заново.');
        }

        if (record.attempts >= 5) {
          telegramOtpStore.delete(`2fa_${uid}`);
          return sendError(res, 429, 'Превышено количество попыток. Войдите заново.');
        }

        if (record.code !== cleanCode) {
          record.attempts++;
          return sendError(res, 400, 'Неверный код 2FA из Telegram');
        }

        telegramOtpStore.delete(`2fa_${uid}`);
        const user = db.findUserById(uid);
        if (!user) {
          return sendError(res, 404, 'Пользователь не найден');
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
            pairingCodeCreatedAt: user.pairing_code_created_at || 0,
            clientUuid: user.client_uuid || '',
            coachName: user.coach_name || '',
            coachPhone: user.coach_phone || '',
            isPrivate: Boolean(user.is_private),
            telegramId: user.telegram_id || '',
            telegramUsername: user.telegram_username || '',
            twoFactorEnabled: Boolean(user.two_factor_enabled)
          }
        });
      }

      // TELEGRAM AUTHENTICATION (Mini App initData, Widget or Telegram Username)
      if (pathname === '/api/auth/telegram' && req.method === 'POST') {
        const body = await parseJsonBody(req);
        const { initData, telegramUser, requestedRole } = body;
        const botToken = process.env.BOT_TOKEN || '';

        let tgId = null;
        let tgUsername = '';
        let tgFirstName = '';
        let tgLastName = '';
        let tgPhotoUrl = '';

        if (initData) {
          const params = new URLSearchParams(initData);
          const hash = params.get('hash');
          if (botToken && hash) {
            params.delete('hash');
            const dataCheckString = Array.from(params.entries())
              .map(([k, v]) => `${k}=${v}`)
              .sort()
              .join('\n');
            const secretKey = crypto.createHmac('sha256', 'WebAppData').update(botToken).digest();
            const calculatedHash = crypto.createHmac('sha256', secretKey).update(dataCheckString).digest('hex');
            if (calculatedHash !== hash) {
              return sendError(res, 401, 'Недействительная подпись Telegram HMAC');
            }
          }
          const userJson = params.get('user');
          if (userJson) {
            try {
              const u = JSON.parse(userJson);
              tgId = u.id;
              tgUsername = u.username || '';
              tgFirstName = u.first_name || '';
              tgLastName = u.last_name || '';
              tgPhotoUrl = u.photo_url || '';
            } catch (_) {}
          }
        } else if (telegramUser) {
          if (botToken && telegramUser.hash) {
            const { hash, ...data } = telegramUser;
            const dataCheckString = Object.keys(data)
              .sort()
              .map(k => `${k}=${data[k]}`)
              .join('\n');
            const secretKey = crypto.createHash('sha256').update(botToken).digest();
            const calculatedHash = crypto.createHmac('sha256', secretKey).update(dataCheckString).digest('hex');
            if (calculatedHash !== hash) {
              return sendError(res, 401, 'Недействительная подпись виджета Telegram');
            }
          }
          tgId = telegramUser.id || ('tg_' + Date.now());
          tgUsername = telegramUser.username || '';
          tgFirstName = telegramUser.first_name || '';
          tgLastName = telegramUser.last_name || '';
          tgPhotoUrl = telegramUser.photo_url || '';
        } else if (body.username) {
          const clean = String(body.username).replace(/^@/, '').trim();
          if (!clean || hasSqlInjectionVector(clean)) {
            return sendError(res, 400, 'Некорректный логин Telegram');
          }
          tgUsername = clean;
          tgFirstName = clean;
          tgId = 'usr_' + clean.toLowerCase();
        } else {
          return sendError(res, 400, 'Не переданы данные для входа через Telegram');
        }

        const usernameKey = tgUsername ? `tg_${tgUsername.toLowerCase()}` : `tg_${tgId}`;
        let user = db.findUserByUsername(usernameKey);
        const role = requestedRole === 'trainer' ? 'trainer' : 'athlete';

        if (!user) {
          const passwordHash = hashPassword(crypto.randomBytes(24).toString('hex'));
          const fullName = (tgFirstName + (tgLastName ? ' ' + tgLastName : '')).trim() || tgUsername || 'Telegram Атлет';
          const pairingCode = role === 'athlete' ? String(Math.floor(100000 + Math.random() * 900000)) : '';
          const clientUuid = crypto.randomUUID();

          const userId = db.createUser(
            usernameKey,
            passwordHash,
            role,
            escapeHtml(fullName),
            '', // phone
            pairingCode,
            clientUuid,
            tgPhotoUrl
          );

          if (role === 'athlete') {
            cloudSyncService.registerAthletePairing(pairingCode, clientUuid, escapeHtml(fullName), '').catch(() => {});
          }

          user = db.findUserById(userId);
        }

        const token = generateToken();
        db.createAuthToken(token, user.id);

        return sendJson(res, 200, {
          success: true,
          authProvider: 'telegram',
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

      // TELEGRAM OTP: 1. Request One-Time 6-digit Code (Valid 5 minutes)
      if (pathname === '/api/auth/telegram/request-otp' && req.method === 'POST') {
        const body = await parseJsonBody(req);
        const { username } = body;
        const cleanUsername = String(username || '').replace(/^@/, '').trim().toLowerCase();

        if (!cleanUsername || hasSqlInjectionVector(cleanUsername)) {
          return sendError(res, 400, 'Укажите корректный Telegram логин');
        }

        // Generate 6-digit random code
        const code = String(Math.floor(100000 + Math.random() * 900000));
        const expiresAt = Date.now() + 5 * 60 * 1000; // Strictly 5 minutes

        telegramOtpStore.set(cleanUsername, {
          code,
          expiresAt,
          attempts: 0
        });

        let delivered = false;
        const targetChatId = userTgChatMap.get(cleanUsername);

        if (tgBotInstance && targetChatId) {
          try {
            await tgBotInstance.api.sendMessage(
              targetChatId,
              `🔐 Ваш 6-значный код для входа в <b>Fitness Ecosystem Pro</b>:\n\n<code>${code}</code>\n\n⏱ Действует ровно 5 минут. Введите его на сайте.`,
              { parse_mode: 'HTML' }
            );
            delivered = true;
          } catch (sendErr) {
            console.error('[Telegram Bot] Ошибка отправки:', sendErr.message);
          }
        }

        return sendJson(res, 200, {
          success: true,
          message: delivered 
            ? 'Код отправлен вам в бота Telegram!' 
            : 'Код сгенерирован. Откройте бота в Telegram для получения кода.',
          expiresInSeconds: 300,
          telegramUsername: cleanUsername,
          botUsername: activeBotUsername || process.env.BOT_USERNAME || '',
          delivered,
          needStartBot: !delivered
        });
      }

      // TELEGRAM OTP: 2. Verify 6-digit Code
      if (pathname === '/api/auth/telegram/verify-otp' && req.method === 'POST') {
        const body = await parseJsonBody(req);
        const { username, code } = body;
        const cleanUsername = String(username || '').replace(/^@/, '').trim().toLowerCase();
        const cleanCode = String(code || '').trim();

        if (!cleanUsername || !cleanCode) {
          return sendError(res, 400, 'Введите имя пользователя и 6-значный код');
        }

        const record = telegramOtpStore.get(cleanUsername);
        if (!record) {
          return sendError(res, 400, 'Код не запрашивался или срок действия (5 минут) истек');
        }

        if (Date.now() > record.expiresAt) {
          telegramOtpStore.delete(cleanUsername);
          return sendError(res, 400, 'Срок действия кода истек (5 минут). Запросите новый код.');
        }

        if (record.attempts >= 5) {
          telegramOtpStore.delete(cleanUsername);
          return sendError(res, 429, 'Превышено количество попыток. Запросите код заново.');
        }

        if (record.code !== cleanCode) {
          record.attempts++;
          return sendError(res, 400, 'Неверный код из Telegram');
        }

        // Code is verified and consumed (single-use guaranteed)
        telegramOtpStore.delete(cleanUsername);

        const usernameKey = `tg_${cleanUsername}`;
        const user = db.findUserByUsername(usernameKey);

        if (user) {
          const token = generateToken();
          db.createAuthToken(token, user.id);
          return sendJson(res, 200, {
            success: true,
            isNewUser: false,
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

        return sendJson(res, 200, {
          success: true,
          isNewUser: true,
          telegramUsername: cleanUsername
        });
      }

      // TELEGRAM OTP: 3. Complete Profile (New User: Name, Phone, Role)
      if (pathname === '/api/auth/telegram/complete-profile' && req.method === 'POST') {
        const body = await parseJsonBody(req);
        const { username, role, fullName, phone, avatarBase64 } = body;
        const cleanUsername = String(username || '').replace(/^@/, '').trim().toLowerCase();
        const cleanFullName = String(fullName || '').trim();
        const cleanPhone = phone ? String(phone).trim() : '';

        if (!cleanUsername || !cleanFullName || !role) {
          return sendError(res, 400, 'Заполните обязательные поля профиля');
        }

        if (hasSqlInjectionVector(cleanUsername) || hasSqlInjectionVector(cleanFullName)) {
          return sendError(res, 400, 'Недопустимые символы в профиле');
        }

        if (role !== 'athlete' && role !== 'trainer') {
          return sendError(res, 400, 'Роль должна быть athlete или trainer');
        }

        const usernameKey = `tg_${cleanUsername}`;
        let user = db.findUserByUsername(usernameKey);

        if (!user) {
          const passwordHash = hashPassword(crypto.randomBytes(24).toString('hex'));
          const escapedFullName = escapeHtml(cleanFullName);
          const escapedPhone = cleanPhone ? escapeHtml(cleanPhone) : '';
          const pairingCode = role === 'athlete' ? String(Math.floor(100000 + Math.random() * 900000)) : '';
          const clientUuid = crypto.randomUUID();

          const userId = db.createUser(
            usernameKey,
            passwordHash,
            role,
            escapedFullName,
            escapedPhone,
            pairingCode,
            clientUuid,
            avatarBase64 || ''
          );

          if (role === 'athlete') {
            cloudSyncService.registerAthletePairing(pairingCode, clientUuid, escapedFullName, escapedPhone).catch(() => {});
          }

          user = db.findUserById(userId);
        }

        const token = generateToken();
        db.createAuthToken(token, user.id);

        return sendJson(res, 201, {
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
          const PAIRING_TTL = 5 * 60 * 1000;
          if (!user.pairing_code || (Date.now() - (user.pairing_code_created_at || 0) > PAIRING_TTL)) {
            const newPin = String(Math.floor(100000 + Math.random() * 900000));
            db.regeneratePairingCode(user.id, newPin);
            cloudSyncService.registerAthletePairing(newPin, user.client_uuid, user.full_name, user.phone).catch(() => {});
            user.pairing_code = newPin;
            user.pairing_code_created_at = Date.now();
          }
          pairedCoach = db.getAthleteCoach(user.id);
          // Bi-directional Cloud Check: If not locally paired or user has pairingCode, check Google Drive
          if (!pairedCoach && user.pairing_code) {
            try {
              const cloudStatus = await cloudSyncService.checkAthletePairingStatus(user.pairing_code, user.client_uuid);
              if (cloudStatus && cloudStatus.status === 'PAIRED' && cloudStatus.coachName) {
                db.updateCoachInfo(user.id, cloudStatus.coachName, cloudStatus.coachPhone || '');
                pairedCoach = {
                  full_name: cloudStatus.coachName,
                  phone: cloudStatus.coachPhone || '',
                  avatar_base64: cloudStatus.coachAvatarBase64 || ''
                };
              } else if (cloudStatus && (cloudStatus.status === 'UNPAIRED' || cloudStatus.status === 'PENDING')) {
                if (user.coach_name) {
                  db.updateCoachInfo(user.id, '', '');
                }
                pairedCoach = null;
              }
            } catch (err) {
              console.warn('[Server] Cloud coach status check notice:', err.message);
            }
          }
          if (!pairedCoach && user.coach_name) {
            pairedCoach = {
              full_name: user.coach_name,
              phone: user.coach_phone || '',
              avatar_base64: ''
            };
          }
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

      // REGENERATE PIN (Athlete - Valid strictly 5 minutes)
      if (pathname === '/api/athlete/regenerate-pin' && req.method === 'POST') {
        if (user.role !== 'athlete') return sendError(res, 403, 'Доступно только атлетам');
        const newPin = String(Math.floor(100000 + Math.random() * 900000));
        db.regeneratePairingCode(user.id, newPin);

        cloudSyncService.registerAthletePairing(newPin, user.client_uuid, user.full_name, user.phone).catch(() => {});

        return sendJson(res, 200, {
          success: true,
          pairingCode: newPin,
          pairingCodeCreatedAt: Date.now(),
          expiresInSeconds: 300
        });
      }

      // TELEGRAM LINKING: Generate Deep Link Token (5-minute TTL)
      if (pathname === '/api/user/telegram/link-token' && req.method === 'POST') {
        const rawToken = crypto.randomBytes(16).toString('hex');
        const tokenRecord = db.createLinkToken(user.id, rawToken, 5 * 60 * 1000);
        const botName = activeBotUsername || process.env.BOT_USERNAME || '';
        const deepLink = botName ? `https://t.me/${botName}?start=link_${rawToken}` : '';

        return sendJson(res, 200, {
          success: true,
          token: rawToken,
          expiresInSeconds: 300,
          expiresAt: tokenRecord.expiresAt,
          botUsername: botName,
          deepLink: deepLink || null
        });
      }

      // TELEGRAM LINKING: Request OTP
      if (pathname === '/api/user/telegram/link-request' && req.method === 'POST') {
        const body = await parseJsonBody(req);
        const cleanUsername = String(body.username || '').replace(/^@/, '').trim().toLowerCase();
        if (!cleanUsername || hasSqlInjectionVector(cleanUsername)) {
          return sendError(res, 400, 'Укажите корректный Telegram @username');
        }

        const otp = String(Math.floor(100000 + Math.random() * 900000));
        const expiresAt = Date.now() + 5 * 60 * 1000;
        telegramOtpStore.set(`link_${cleanUsername}`, {
          userId: user.id,
          username: cleanUsername,
          code: otp,
          expiresAt,
          attempts: 0
        });

        console.log(`[Telegram Link] OTP for linking @${cleanUsername} to user #${user.id}: ${otp}`);

        return sendJson(res, 200, {
          success: true,
          message: 'Код подтверждения отправлен в Telegram бота. Действует 5 минут.',
          expiresInSeconds: 300,
          debugCode: (process.env.NODE_ENV === 'test' || !process.env.BOT_TOKEN) ? otp : undefined
        });
      }

      // TELEGRAM LINKING: Confirm OTP & Link
      if (pathname === '/api/user/telegram/link-confirm' && req.method === 'POST') {
        const body = await parseJsonBody(req);
        const cleanUsername = String(body.username || '').replace(/^@/, '').trim().toLowerCase();
        const cleanCode = String(body.code || '').trim();

        const record = telegramOtpStore.get(`link_${cleanUsername}`);
        if (!record || record.userId !== user.id) {
          return sendError(res, 400, 'Запрос привязки не найден или срок действия (5 минут) истек');
        }

        if (Date.now() > record.expiresAt) {
          telegramOtpStore.delete(`link_${cleanUsername}`);
          return sendError(res, 400, 'Срок действия кода истек (5 минут). Запросите заново.');
        }

        if (record.attempts >= 5) {
          telegramOtpStore.delete(`link_${cleanUsername}`);
          return sendError(res, 429, 'Превышено количество попыток. Запросите заново.');
        }

        if (record.code !== cleanCode) {
          record.attempts++;
          return sendError(res, 400, 'Неверный код из Telegram');
        }

        telegramOtpStore.delete(`link_${cleanUsername}`);
        db.linkTelegram(user.id, 'tg_' + cleanUsername, cleanUsername);

        return sendJson(res, 200, {
          success: true,
          message: `Telegram @${cleanUsername} успешно привязан`,
          telegramUsername: cleanUsername
        });
      }

      // TELEGRAM: Unlink
      if (pathname === '/api/user/telegram/unlink' && req.method === 'POST') {
        db.unlinkTelegram(user.id);
        db.setTwoFactorEnabled(user.id, 0);
        return sendJson(res, 200, { success: true, message: 'Telegram отвязан' });
      }

      // 2FA: Toggle Two-Factor Authentication
      if (pathname === '/api/user/2fa' && req.method === 'POST') {
        const body = await parseJsonBody(req);
        const isEnable = Boolean(body.enabled);

        if (isEnable) {
          const freshUser = db.findUserById(user.id);
          const hasTg = Boolean(freshUser.telegram_id || freshUser.telegram_username || freshUser.username.startsWith('tg_'));
          if (!hasTg) {
            return sendError(res, 400, 'Сначала привяжите Telegram аккаунт для включения 2FA');
          }
        }

        db.setTwoFactorEnabled(user.id, isEnable ? 1 : 0);
        return sendJson(res, 200, { success: true, twoFactorEnabled: isEnable });
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

        // Strict 5-minute TTL check if found in local SQLite
        if (athlete) {
          const PAIRING_TTL = 5 * 60 * 1000;
          if (athlete.pairing_code_created_at && (Date.now() - athlete.pairing_code_created_at > PAIRING_TTL)) {
            return sendError(res, 400, 'Срок действия кода истёк (действует 5 минут). Запросите у подопечного новый код.');
          }
        } else {
          // If not in local SQLite, query Google Drive Cloud
          try {
            const cloudAthlete = await cloudSyncService.findAndPairAthlete(rawCode, user.full_name, user.phone);
            if (cloudAthlete) {
              if (cloudAthlete.error) {
                return sendError(res, 400, cloudAthlete.message || 'Ошибка кода привязки');
              }
              let localUser = db.findUserByClientUuid(cloudAthlete.clientUuid);
              if (!localUser) {
                const uniqueUsername = 'ath_' + crypto.randomBytes(6).toString('hex');
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
          return sendError(res, 400, 'Код привязки не найден или уже был использован. Запросите у подопечного новый код.');
        }

        db.pairTrainerAndAthlete(user.id, athlete.id);
        db.updateCoachInfo(athlete.id, user.full_name, user.phone);
        // Strict single-use consumption: code cannot be reused!
        db.consumePairingCode(athlete.id);

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

      // TRAINER: GET ATHLETE RESTRICTIONS ("Строчки травмы")
      if (pathname === '/api/trainer/athlete-restrictions' && req.method === 'GET') {
        if (user.role !== 'trainer') return sendError(res, 403, 'Доступно только тренерам');
        const athleteId = Number(reqUrl.searchParams.get('athleteId'));
        if (!athleteId) return sendError(res, 400, 'Укажите athleteId');
        const athlete = db.findUserById(athleteId);
        if (!athlete) return sendError(res, 404, 'Атлет не найден');
        return sendJson(res, 200, { success: true, athleteId, restrictions: athlete.restrictions || '' });
      }

      // TRAINER: UPDATE ATHLETE RESTRICTIONS ("Строчки травмы")
      if (pathname === '/api/trainer/athlete-restrictions' && req.method === 'POST') {
        if (user.role !== 'trainer') return sendError(res, 403, 'Доступно только тренерам');
        const body = await parseJsonBody(req);
        const athleteId = Number(body.athleteId);
        const restrictions = String(body.restrictions || '').trim();
        if (!athleteId) return sendError(res, 400, 'Укажите athleteId');

        db.updateAthleteRestrictions(athleteId, restrictions);

        const athlete = db.findUserById(athleteId);
        if (athlete && athlete.client_uuid) {
          cloudSyncService.updateAthleteRestrictions(athlete.client_uuid, restrictions).catch(() => {});
        }

        return sendJson(res, 200, { success: true, restrictions });
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
          const { session, sets } = db.getWorkoutSessionWithSets(athlete.id, date);
          try {
            await cloudSyncService.syncWorkoutSessionToCloud(athlete.client_uuid, athlete.full_name, date, session, sets);
          } catch (err) {
            console.warn('[Server] Cloud sync assign error:', err.message);
          }
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

        const athlete = db.findUserById(targetAthleteId);

        // Bi-directional Google Drive Cloud Workout Sync
        if (athlete && athlete.client_uuid) {
          try {
            const cloudWorkouts = await cloudSyncService.getAthleteCloudWorkouts(athlete.client_uuid);
            const targetCloudSession = cloudWorkouts.find(w => w.date === date);
            if (targetCloudSession) {
              const { session, sets } = db.getWorkoutSessionWithSets(targetAthleteId, date);
              let sessionId = session ? session.id : null;

              if (!session) {
                sessionId = db.assignTrainerWorkout(
                  session?.assigned_by_trainer_id || 1,
                  targetAthleteId,
                  date,
                  targetCloudSession.isSelfWorkoutAllowed ? 1 : 0,
                  targetCloudSession.notes || ''
                );
              } else if (targetCloudSession.isSelfWorkoutAllowed && !session.is_self_workout_allowed) {
                db.assignTrainerWorkout(
                  session.assigned_by_trainer_id || 1,
                  targetAthleteId,
                  date,
                  1,
                  session.notes || targetCloudSession.notes || ''
                );
              }

              if (Array.isArray(targetCloudSession.exercises)) {
                for (const ex of targetCloudSession.exercises) {
                  const exName = escapeHtml(ex.name || 'Упражнение');
                  if (Array.isArray(ex.sets)) {
                    for (const s of ex.sets) {
                      const weight = Number(s.actualWeightKg || s.targetWeightKg || s.weight || 0);
                      const reps = Number(s.actualReps || s.targetReps || s.reps || 1);
                      const isCompleted = Boolean(s.isCompleted);
                      const rpe = Number(s.rpe || 8.0);

                      const existingSet = sets.find(ls => ls.exercise_name === exName && ls.reps === reps && Math.abs(ls.weight_kg - weight) < 0.01);
                      if (existingSet) {
                        if (isCompleted && !existingSet.is_completed) {
                          db.toggleWorkoutSet(existingSet.id, true);
                        }
                      } else {
                        const newSetId = db.addWorkoutSet(sessionId, exName, weight, reps, rpe, 0);
                        if (isCompleted) {
                          db.toggleWorkoutSet(newSetId, true);
                        }
                      }
                    }
                  }
                }
              }
            }
          } catch (err) {
            console.warn('[Server] Cloud workout sync notice:', err.message);
          }
        }

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
        const hasTrainer = Boolean(session.assigned_by_trainer_id !== null || user.coach_name);
        if (user.role === 'athlete' && hasTrainer && !session.is_self_workout_allowed) {
          return sendError(res, 403, 'Добавление упражнений заблокировано тренером. Атлет может только отмечать выполнение подходов.');
        }

        const cleanExercise = escapeHtml(String(exerciseName).trim());
        const weight = Math.max(0, Number(weightKg) || 0);
        const repCount = Math.max(1, Number(reps) || 1);
        const rpeVal = Math.min(10, Math.max(1, Number(rpe) || 8.0));

        const setId = db.addWorkoutSet(session.id, cleanExercise, weight, repCount, rpeVal, 1);

        // Sync to Google Drive cloud
        const athlete = db.findUserById(targetAthleteId);
        if (athlete && athlete.client_uuid) {
          const { session: updatedSession, sets: updatedSets } = db.getWorkoutSessionWithSets(athlete.id, workoutDate);
          try {
            await cloudSyncService.syncWorkoutSessionToCloud(athlete.client_uuid, athlete.full_name, workoutDate, updatedSession, updatedSets);
          } catch (err) {
            console.warn('[Server] Cloud sync add set error:', err.message);
          }
        }

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

        // Sync to Google Drive cloud
        const athlete = db.findUserById(targetSet.athlete_id);
        if (athlete && athlete.client_uuid) {
          const targetDate = targetSet.workout_date || new Date().toISOString().slice(0, 10);
          const { session, sets } = db.getWorkoutSessionWithSets(athlete.id, targetDate);
          try {
            await cloudSyncService.syncWorkoutSessionToCloud(athlete.client_uuid, athlete.full_name, targetDate, session, sets);
          } catch (err) {
            console.warn('[Server] Cloud sync toggle set error:', err.message);
          }
        }

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

        // Sync to Google Drive cloud
        const athlete = db.findUserById(targetSet.athlete_id);
        if (athlete && athlete.client_uuid) {
          const targetDate = targetSet.workout_date || new Date().toISOString().slice(0, 10);
          const { session: updatedSession, sets: updatedSets } = db.getWorkoutSessionWithSets(athlete.id, targetDate);
          try {
            await cloudSyncService.syncWorkoutSessionToCloud(athlete.client_uuid, athlete.full_name, targetDate, updatedSession, updatedSets);
          } catch (err) {
            console.warn('[Server] Cloud sync delete set error:', err.message);
          }
        }

        return sendJson(res, 200, { success: true, setId });
      }

      // PROGRESS & CHARTS: Get list of all exercises for athlete
      if (pathname === '/api/progress/exercises' && req.method === 'GET') {
        const targetAthleteId = user.role === 'athlete'
          ? user.id
          : Number(reqUrl.searchParams.get('athleteId') || user.id);
        const exercises = db.getAthleteExercises(targetAthleteId);
        return sendJson(res, 200, { exercises });
      }

      // PROGRESS & CHARTS: Get exercise history timeline (Weight, Reps, Sets, RPE)
      if (pathname === '/api/progress/exercise' && req.method === 'GET') {
        const targetAthleteId = user.role === 'athlete'
          ? user.id
          : Number(reqUrl.searchParams.get('athleteId') || user.id);
        const exerciseName = reqUrl.searchParams.get('exercise') || '';
        if (!exerciseName) {
          return sendError(res, 400, 'Укажите название упражнения');
        }
        const timeline = db.getExerciseProgressTimeline(targetAthleteId, exerciseName);
        return sendJson(res, 200, { exerciseName, timeline });
      }

      // PROGRESS & CHARTS: Get body weight / anthropometry history
      if (pathname === '/api/progress/anthropometry' && req.method === 'GET') {
        const targetAthleteId = user.role === 'athlete'
          ? user.id
          : Number(reqUrl.searchParams.get('athleteId') || user.id);
        const history = db.getAnthropometryHistory(targetAthleteId);
        return sendJson(res, 200, { history });
      }

      // PROGRESS & CHARTS: Add new body weight / anthropometry record
      if (pathname === '/api/progress/anthropometry' && req.method === 'POST') {
        const body = await parseJsonBody(req);
        const targetAthleteId = user.role === 'athlete'
          ? user.id
          : Number(body.athleteId || user.id);
        const { weightKg, date, chestCm, waistCm, bicepsCm } = body;
        if (!weightKg || Number(weightKg) <= 0) {
          return sendError(res, 400, 'Укажите корректный вес тела в кг');
        }
        const id = db.addAnthropometry(targetAthleteId, Number(weightKg), date, chestCm, waistCm, bicepsCm);
        const history = db.getAnthropometryHistory(targetAthleteId);
        return sendJson(res, 201, { success: true, id, history });
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

    // Serve official Android APK downloads from releases directory
    if (safePathname.startsWith('/releases/') || safePathname.startsWith('releases/')) {
      const fileName = path.basename(safePathname);
      if (fileName.endsWith('.apk')) {
        const apkPath = path.resolve(RELEASES_DIR, fileName);
        if (apkPath.startsWith(RELEASES_DIR) && fs.existsSync(apkPath) && fs.statSync(apkPath).isFile()) {
          res.writeHead(200, {
            'Content-Type': 'application/vnd.android.package-archive',
            'Content-Disposition': `attachment; filename="${fileName}"`,
            'Content-Length': fs.statSync(apkPath).size
          });
          return fs.createReadStream(apkPath).pipe(res);
        }
      }
      res.writeHead(404);
      return res.end('Release APK Not Found');
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

module.exports = {
  server,
  db,
  authLimiter,
  telegramOtpStore,
  userTgChatMap,
  getTgBotInstance: () => tgBotInstance,
  setTgBotInstance: (b) => { tgBotInstance = b; }
};
