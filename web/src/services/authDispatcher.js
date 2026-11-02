/**
 * Multi-Scheme Authentication Dispatcher Service
 * Supports:
 * - scheme: 'basic' (credentials or username:password / Base64 secret, ruToEn layout auto-fix, 3-30 len policy, >=6 char pwd)
 * - scheme: 'token' (silent fast re-auth via sessionId/token, last_seen_at bump, token expiration extension)
 * - scheme: 'anonymous' / 'anon' (clean guest profile without "(гость)", instant session token & cookie)
 * - Session lifetime policy (persist === true: 30 days cookie Max-Age; persist === false: browser session cookie)
 */

const crypto = require('node:crypto');
const { verifyPassword, generateToken, hasSqlInjectionVector, escapeHtml } = require('../security');

const RU_TO_EN_MAP = {
  'й': 'q', 'ц': 'w', 'у': 'e', 'к': 'r', 'е': 't', 'н': 'y', 'г': 'u', 'ш': 'i', 'щ': 'o', 'з': 'p', 'х': '[', 'ъ': ']',
  'ф': 'a', 'ы': 's', 'в': 'd', 'а': 'f', 'п': 'g', 'р': 'h', 'о': 'j', 'л': 'k', 'д': 'l', 'ж': ';', 'э': '\'',
  'я': 'z', 'ч': 'x', 'с': 'c', 'м': 'v', 'и': 'b', 'т': 'n', 'ь': 'm', 'б': ',', 'ю': '.',
  'Й': 'Q', 'Ц': 'W', 'У': 'E', 'К': 'R', 'Е': 'T', 'Н': 'Y', 'Г': 'U', 'Ш': 'I', 'Щ': 'O', 'З': 'P', 'Х': '{', 'Ъ': '}',
  'Ф': 'A', 'Ы': 'S', 'В': 'D', 'А': 'F', 'П': 'G', 'Р': 'H', 'О': 'J', 'Л': 'K', 'Д': 'L', 'Ж': ':', 'Э': '"',
  'Я': 'Z', 'Ч': 'X', 'С': 'C', 'М': 'V', 'И': 'B', 'Т': 'N', 'Ь': 'M', 'Б': '<', 'Ю': '>',
  'ё': '`', 'Ё': '~'
};

function ruToEn(str) {
  if (typeof str !== 'string') return '';
  return str.split('').map(ch => RU_TO_EN_MAP[ch] || ch).join('');
}

function validateLoginPolicy(username) {
  if (!username || typeof username !== 'string') {
    return { valid: false, error: 'Введите логин и пароль' };
  }
  const clean = username.trim();
  if (!clean) {
    return { valid: false, error: 'Введите логин и пароль' };
  }
  if (hasSqlInjectionVector(clean)) {
    return { valid: false, error: 'Некорректный логин' };
  }
  if (clean.length < 3 || clean.length > 30) {
    return { valid: false, error: 'Логин должен содержать от 3 до 30 символов' };
  }
  const converted = ruToEn(clean);
  return { valid: true, sanitized: clean, converted };
}

function validatePasswordPolicy(password) {
  if (!password || typeof password !== 'string') {
    return { valid: false, error: 'Введите логин и пароль' };
  }
  if (password.length < 6) {
    return { valid: false, error: 'Пароль должен содержать минимум 6 символов' };
  }
  return { valid: true };
}

function parseSecret(secret) {
  if (!secret || typeof secret !== 'string') return null;
  let raw = secret.trim();
  // Check if secret is Base64 encoded (standard HTTP Basic / Tinode auth format)
  if (!raw.includes(':')) {
    try {
      const decoded = Buffer.from(raw, 'base64').toString('utf8');
      if (decoded.includes(':')) {
        raw = decoded;
      }
    } catch (_) {}
  }
  const colonIndex = raw.indexOf(':');
  if (colonIndex !== -1) {
    const parsedUsername = raw.slice(0, colonIndex).trim();
    const parsedPassword = raw.slice(colonIndex + 1);
    return { username: parsedUsername, password: parsedPassword };
  }
  return null;
}

function setAuthCookie(res, token, persist = false) {
  if (!res || typeof res.setHeader !== 'function' || !token) return;
  let cookieHeader = `fit_token=${token}; Path=/; HttpOnly; SameSite=Lax`;
  if (persist) {
    const THIRTY_DAYS_SECONDS = 30 * 24 * 60 * 60; // 2592000
    const expiresDate = new Date(Date.now() + THIRTY_DAYS_SECONDS * 1000).toUTCString();
    cookieHeader += `; Max-Age=${THIRTY_DAYS_SECONDS}; Expires=${expiresDate}`;
  }
  res.setHeader('Set-Cookie', cookieHeader);
}

function clearAuthCookie(res) {
  if (!res || typeof res.setHeader !== 'function') return;
  res.setHeader('Set-Cookie', 'fit_token=; Path=/; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT; HttpOnly; SameSite=Lax');
}

function normalizeUserProfile(user) {
  if (!user) return null;
  return {
    id: user.id,
    username: user.username,
    role: user.role,
    fullName: user.full_name || '',
    phone: user.phone || '',
    avatarBase64: user.avatar_base64 || '',
    pairingCode: user.pairing_code || '',
    pairingCodeCreatedAt: user.pairing_code_created_at || 0,
    clientUuid: user.client_uuid || '',
    restrictions: user.restrictions || '',
    coachName: user.coach_name || '',
    coachPhone: user.coach_phone || '',
    isPrivate: Boolean(user.is_private),
    telegram_id: String(user.telegram_id || ''),
    telegramId: String(user.telegram_id || ''),
    telegram_username: String(user.telegram_username || ''),
    telegramUsername: String(user.telegram_username || ''),
    two_factor_enabled: Number(user.two_factor_enabled) || 0,
    twoFactorEnabled: Boolean(user.two_factor_enabled),
    last_seen_at: Number(user.last_seen_at) || 0,
    lastSeenAt: Number(user.last_seen_at) || 0,
    is_guest: Number(user.is_guest) || 0,
    isGuest: Boolean(user.is_guest)
  };
}

/**
 * Main Dispatcher: executes multi-scheme authentication logic
 */
async function dispatchAuth(params, context) {
  const { db, cloudSyncService, tgBotInstance, isTrainerApprovalRequired, send2FAOtp, generateSecureOtpWithRetry, telegramOtpStore } = context;
  const rawScheme = String(params.scheme || 'basic').toLowerCase().trim();
  const persist = Boolean(params.persist);

  // ==========================================
  // SCHEME: basic
  // ==========================================
  if (rawScheme === 'basic') {
    let username = params.username;
    let password = params.password;

    // Handle Tinode-style or basic auth secret: "username:password" or Base64
    if (params.secret) {
      const parsed = parseSecret(params.secret);
      if (parsed) {
        username = parsed.username;
        password = parsed.password;
      }
    }

    const loginValidation = validateLoginPolicy(username);
    if (!loginValidation.valid) {
      return { status: 400, data: { error: loginValidation.error } };
    }

    const passwordValidation = validatePasswordPolicy(password);
    if (!passwordValidation.valid) {
      return { status: 400, data: { error: passwordValidation.error } };
    }

    const cleanUsername = loginValidation.sanitized;
    const convertedUsername = loginValidation.converted;

    // Try finding user by clean username, then by ruToEn converted username
    let user = db.findUserByUsername(cleanUsername);
    if (!user && convertedUsername && convertedUsername !== cleanUsername) {
      user = db.findUserByUsername(convertedUsername);
    }

    if (!user) {
      return {
        status: 401,
        data: {
          error: 'Пользователь не найден. Зарегистрируйтесь или проверьте правильность логина.',
          userNotFound: true,
          suggestRegister: true,
          username: cleanUsername
        }
      };
    }

    if (!verifyPassword(password, user.password_hash)) {
      return {
        status: 401,
        data: {
          error: 'Неверный пароль. Проверьте правильность введенного пароля.',
          userNotFound: false,
          invalidPassword: true,
          suggestTelegram: true,
          username: cleanUsername
        }
      };
    }

    // Role override if requested
    const requestedRole = params.role;
    if (requestedRole && (requestedRole === 'athlete' || requestedRole === 'trainer')) {
      if (user.role !== requestedRole) {
        db.updateUserRole(user.id, requestedRole);
        user.role = requestedRole;
        const isOwnerOrAdmin = ['santila213', 'spirit5449', 'kotpers67', 'kotpers76'].includes(user.username.toLowerCase()) ||
          ['santila213', 'spirit5449', 'kotpers67', 'kotpers76'].includes((user.telegram_username || '').replace(/^@/, '').toLowerCase());
        if (requestedRole === 'trainer') {
          if (isOwnerOrAdmin || (typeof isTrainerApprovalRequired === 'function' && !isTrainerApprovalRequired())) {
            db.approveTrainer(user.id);
            user.is_approved = 1;
          } else {
            user.is_approved = 0;
          }
        }
      }
    }

    if (user.role === 'trainer' && user.is_approved === 0) {
      const isOwnerOrAdmin = ['santila213', 'spirit5449', 'kotpers67', 'kotpers76'].includes(user.username.toLowerCase()) ||
        ['santila213', 'spirit5449', 'kotpers67', 'kotpers76'].includes((user.telegram_username || '').replace(/^@/, '').toLowerCase());
      if (isOwnerOrAdmin || (typeof isTrainerApprovalRequired === 'function' && !isTrainerApprovalRequired())) {
        db.approveTrainer(user.id);
        user.is_approved = 1;
      } else {
        return {
          status: 403,
          data: {
            error: '⏳ ЗАЯВКА НА РАССМОТРЕНИИ\nВаша заявка на создание аккаунта тренера принята!\n\nВ течение 72 часов ваша заявка будет обработана.'
          }
        };
      }
    }

    // 2FA Security Check
    if (user.two_factor_enabled === 1 && telegramOtpStore && typeof generateSecureOtpWithRetry === 'function') {
      telegramOtpStore.delete(`2fa_${user.id}`);
      const otp = generateSecureOtpWithRetry(telegramOtpStore);
      const expiresAt = Date.now() + 5 * 60 * 1000;
      telegramOtpStore.set(`2fa_${user.id}`, {
        userId: user.id,
        code: otp,
        expiresAt,
        attempts: 0
      });
      if (tgBotInstance && user.telegram_id && typeof send2FAOtp === 'function') {
        send2FAOtp(tgBotInstance, user.telegram_id, otp).catch(() => {});
      }
      return {
        status: 200,
        data: {
          success: true,
          require2FA: true,
          userId: user.id,
          message: 'Требуется ввод 6-значного кода 2FA из Telegram (действует 5 минут)',
          expiresInSeconds: 300,
          debugCode: (process.env.NODE_ENV === 'test' || !process.env.BOT_TOKEN) ? otp : undefined
        }
      };
    }

    // Sync cloud info if needed
    if (user.role === 'athlete' && cloudSyncService) {
      try {
        await cloudSyncService.syncAthleteFromCloud(user, db);
        const refetched = db.findUserById(user.id);
        if (refetched) user = refetched;
      } catch (_) {}
    } else if (user.role === 'trainer' && cloudSyncService) {
      try {
        await cloudSyncService.syncTrainerFromCloud(user, db);
        const refetched = db.findUserById(user.id);
        if (refetched) user = refetched;
      } catch (_) {}
    }

    const ttlMs = persist ? (30 * 86400 * 1000) : (7 * 86400 * 1000);
    const token = generateToken(user.id, user.role);
    db.createAuthToken(token, user.id, ttlMs);
    db.updateUserLastSeen(user.id, Date.now());

    return {
      status: 200,
      token,
      persist,
      data: {
        success: true,
        scheme: 'basic',
        token,
        user: normalizeUserProfile(user)
      }
    };
  }

  // ==========================================
  // SCHEME: token (Fast quiet auto-login)
  // ==========================================
  if (rawScheme === 'token') {
    const token = params.token;
    if (!token || typeof token !== 'string') {
      return { status: 400, data: { error: 'Токен сессии обязателен для схемы token' } };
    }

    const user = db.getUserByToken(token);
    if (!user) {
      return { status: 401, data: { error: 'Недействительный или истекший сессионный токен', expired: true } };
    }

    // Update activity and extend token TTL
    db.updateUserLastSeen(user.id, Date.now());
    const additionalTtl = persist ? (30 * 86400 * 1000) : (7 * 86400 * 1000);
    db.extendAuthToken(token, additionalTtl);

    return {
      status: 200,
      token,
      persist,
      data: {
        success: true,
        scheme: 'token',
        token,
        user: normalizeUserProfile(user)
      }
    };
  }

  // ==========================================
  // SCHEME: anonymous / anon (Temporary clean account)
  // ==========================================
  if (rawScheme === 'anonymous' || rawScheme === 'anon') {
    let cleanDesiredName = '';
    if (params.desiredName && typeof params.desiredName === 'string') {
      cleanDesiredName = escapeHtml(params.desiredName.trim().slice(0, 50));
    }

    // Generate clean temporary name (without "(гость)")
    const randomSuffix = crypto.randomInt(1000, 9999);
    const fullName = cleanDesiredName || `Атлет ${randomSuffix}`;
    const guestUsername = `anon_${Date.now()}_${crypto.randomBytes(3).toString('hex')}`;

    const guestId = db.createGuestUser(guestUsername, fullName);
    const guestUser = db.findUserById(guestId);

    const ttlMs = persist ? (30 * 86400 * 1000) : (7 * 86400 * 1000);
    const token = generateToken(guestId, 'athlete');
    db.createAuthToken(token, guestId, ttlMs);
    db.updateUserLastSeen(guestId, Date.now());

    return {
      status: 201,
      token,
      persist,
      data: {
        success: true,
        scheme: 'anonymous',
        token,
        isAnonymous: true,
        user: normalizeUserProfile(guestUser)
      }
    };
  }

  return {
    status: 400,
    data: {
      error: `Неподдерживаемая схема аутентификации: '${rawScheme}'. Допустимы: 'basic', 'token', 'anonymous'`
    }
  };
}

module.exports = {
  ruToEn,
  validateLoginPolicy,
  validatePasswordPolicy,
  parseSecret,
  setAuthCookie,
  clearAuthCookie,
  normalizeUserProfile,
  dispatchAuth
};
