/**
 * Telegram Bot module for Fitness Ecosystem Pro
 * Framework: grammY (v1.28.0)
 * Features: Deep Linking (/start link_<token>, /link <token>), 2FA OTP delivery,
 *           Owner Contact buttons (@SantiLA213, @Spirit5449), Trainer approval.
 */

const crypto = require('node:crypto');
const { Bot, InlineKeyboard, Keyboard } = require('grammy');
const { generateSecurePin, hashPassword, generateToken, escapeHtml } = require('./security');

const OWNER_LINKS = {
  santi: 'https://t.me/SantiLA213',
  spirit: 'https://t.me/Spirit5449'
};

/**
 * Creates persistent menu reply keyboard with main buttons
 */
function createMainMenuKeyboard() {
  return new Keyboard()
    .text('🔑 Код входа').text('🔄 Сменить роль').row()
    .text('🔗 Привязать аккаунт').text('💬 Связь с владельцами').row()
    .text('❓ Справка')
    .resized();
}

/**
 * Creates inline keyboard with links to project owners
 */
function createOwnersKeyboard() {
  return new InlineKeyboard()
    .url('💬 @SantiLA213 (Основатель)', OWNER_LINKS.santi)
    .row()
    .url('💬 @Spirit5449 (Разработчик)', OWNER_LINKS.spirit);
}

/**
 * Handles account linking by token (from /start link_<token> or /link <token>)
 */
async function handleLinkToken(ctx, token, db, userTgChatMap) {
  const cleanToken = String(token || '').replace(/^link_/, '').trim();
  if (!cleanToken) {
    await ctx.reply(
      'ℹ️ <b>Привязка аккаунта к Telegram</b>\n\n' +
      '🔑 <b>Что такое токен привязки:</b>\n' +
      'Это одноразовый 5-минутный код для связки вашего профиля (атлета или тренера) в приложении с Telegram-ботом.\n\n' +
      '📌 <b>Где взять токен:</b>\n' +
      '1. Зайдите на сайт или в мобильное приложение <b>Fitness Ecosystem Pro</b>.\n' +
      '2. Перейдите в раздел <b>Профиль</b> (или карточку тренера/атлета).\n' +
      '3. Нажмите кнопку <b>«Привязать Telegram»</b>.\n' +
      '4. Скопируйте 5-минутный токен или нажмите ссылку для перехода в Telegram.\n\n' +
      '💬 <b>Команда для отправки в бот:</b>\n' +
      '<code>/link &lt;ваш_токен&gt;</code>',
      { parse_mode: 'HTML', reply_markup: createMainMenuKeyboard() }
    );
    return { success: false, reason: 'missing_token' };
  }

  const linkRecord = db.findLinkToken(cleanToken);
  if (!linkRecord || Date.now() > linkRecord.expires_at) {
    await ctx.reply(
      '❌ Ссылка недействительна или срок действия (5 минут) истёк.\n' +
      'Запросите новую ссылку привязки в профиле приложения.',
      { parse_mode: 'HTML', reply_markup: createMainMenuKeyboard() }
    );
    return { success: false, reason: 'expired_or_invalid' };
  }

  const tgId = String(ctx.from?.id || '');
  const rawUsername = ctx.from?.username ? String(ctx.from.username).replace(/^@/, '').toLowerCase() : '';
  const fullName = [ctx.from?.first_name, ctx.from?.last_name].filter(Boolean).join(' ') || 'Пользователь';

  // Check if this Telegram ID is already bound to another account
  const existing = db.findUserByTelegramId(tgId);
  if (existing && existing.id !== linkRecord.user_id) {
    await ctx.reply(
      `⚠️ Этот Telegram уже привязан к аккаунту <b>${existing.username}</b>.\n` +
      `Сначала отвяжите его в настройках того профиля.`,
      { parse_mode: 'HTML', reply_markup: createMainMenuKeyboard() }
    );
    return { success: false, reason: 'already_linked_to_other' };
  }

  // Bind Telegram to user
  db.linkTelegram(linkRecord.user_id, tgId, rawUsername);
  db.consumeLinkToken(cleanToken);

  // Update memory chat maps
  if (userTgChatMap) {
    if (rawUsername) userTgChatMap.set(rawUsername, ctx.chat?.id || tgId);
    userTgChatMap.set(tgId, ctx.chat?.id || tgId);
  }

  const user = db.findUserById(linkRecord.user_id);
  const roleTitle = user?.role === 'trainer' ? 'Тренер' : 'Атлет';
  const displayName = user?.full_name || fullName;
  const usernameText = user?.username ? ` (@${user.username})` : '';

  await ctx.reply(
    `✅ <b>Telegram успешно привязан!</b>\n\n` +
    `👤 Аккаунт: <b>${displayName}</b>${usernameText}\n` +
    `🏷 Роль: <b>${roleTitle}</b>\n` +
    `🆔 Telegram ID: <code>${tgId}</code>\n\n` +
    `🔐 Теперь вы можете включить <b>2FA (двухфакторную аутентификацию)</b> в настройках профиля.\n` +
    `Все коды для входа будут приходить сюда в чат.`,
    {
      parse_mode: 'HTML',
      reply_markup: createMainMenuKeyboard()
    }
  );

  return {
    success: true,
    userId: linkRecord.user_id,
    telegramId: tgId,
    telegramUsername: rawUsername
  };
}

/**
 * Resolves the unified 6-digit code for a user across Web, Bot, and Mobile App.
 * If user is an athlete, uses and maintains the athlete's 5-minute pairing PIN.
 * Guarantees that the code displayed on website, mobile app, and bot is 100% IDENTICAL.
 */
function getUnifiedUserCode(tgId, username, db, telegramOtpStore, cloudSyncService) {
  let user = null;
  if (tgId) {
    user = db.findUserByTelegramId(String(tgId));
  }
  if (!user && username) {
    user = db.findUserByTelegramUsername(username);
  }

  const now = Date.now();
  const PAIRING_TTL = 5 * 60 * 1000;
  let code = null;
  let expiresAt = null;

  // 1. If athlete has an active pairing PIN in DB, reuse it
  if (user && user.role === 'athlete') {
    const isPinActive = user.pairing_code && (now - (user.pairing_code_created_at || 0) < PAIRING_TTL);
    if (isPinActive) {
      code = String(user.pairing_code);
      expiresAt = (user.pairing_code_created_at || now) + PAIRING_TTL;
    } else {
      // Regenerate athlete PIN and sync to cloud registry
      code = (db && typeof db.generateUniquePairingCode === 'function')
        ? db.generateUniquePairingCode(user.id)
        : generateSecurePin();
      expiresAt = now + PAIRING_TTL;
      db.updatePairingCode(user.id, code);
      user.pairing_code = code;
      user.pairing_code_created_at = now;
      if (cloudSyncService && typeof cloudSyncService.registerAthletePairing === 'function') {
        cloudSyncService.registerAthletePairing(code, user.client_uuid, user.full_name, user.phone, '').catch(() => {});
      }
    }
  }

  // 2. Check existing unexpired code in memory store
  if (!code && telegramOtpStore) {
    const existingById = tgId ? telegramOtpStore.get(`id_${tgId}`) : null;
    const existingByUsername = username ? telegramOtpStore.get(username) : null;
    const active = (existingById && existingById.expiresAt > now) ? existingById :
                   (existingByUsername && existingByUsername.expiresAt > now) ? existingByUsername : null;
    if (active) {
      code = active.code;
      expiresAt = active.expiresAt;
    }
  }

  // 3. Fallback: generate fresh code with collision retry loop
  if (!code) {
    let freshPin;
    for (let attempt = 0; attempt < 10; attempt++) {
      freshPin = generateSecurePin();
      let collides = false;
      if (telegramOtpStore) {
        for (const [k, v] of telegramOtpStore.entries()) {
          if (v && v.code === freshPin && v.expiresAt > now) {
            collides = true;
            break;
          }
        }
      }
      if (!collides) break;
    }
    code = freshPin;
    expiresAt = now + PAIRING_TTL;
  }

  // Sync to telegramOtpStore across all aliases
  if (telegramOtpStore) {
    if (username) telegramOtpStore.set(username, { code, expiresAt, attempts: 0 });
    if (tgId) telegramOtpStore.set(`id_${tgId}`, { code, expiresAt, attempts: 0 });
    if (user && user.username) telegramOtpStore.set(user.username, { code, expiresAt, attempts: 0 });
  }

  return { code, expiresAt, user };
}

/**
 * Setup command handlers and event listeners on grammY Bot instance
 */
function setupBotHandlers(bot, { db, telegramOtpStore, telegramSessionStore, userTgChatMap, cloudSyncService }) {
  if (typeof bot.catch === 'function') {
    bot.catch((err) => {
      console.error('grammY error boundary:', err);
    });
  }

  // /start handler with deep-linking support (/start auth_<session>, /start link_<token>, /start login)
  bot.command('start', async (ctx) => {
    const username = ctx.from?.username ? ctx.from.username.replace(/^@/, '').toLowerCase() : null;
    const chatId = ctx.chat?.id;
    const tgId = String(ctx.from?.id || '');

    if (username && userTgChatMap) userTgChatMap.set(username, chatId);
    if (tgId && userTgChatMap) userTgChatMap.set(tgId, chatId);

    const payload = (ctx.match || '').trim();

    // 1-Click Seamless Authorization: /start auth_<sessionId>
    if (payload.startsWith('auth_')) {
      const sessionId = payload.trim();
      const session = telegramSessionStore ? telegramSessionStore.get(sessionId) : null;
      const now = Date.now();
      if (!session || now > session.expiresAt) {
        await ctx.reply(
          '⚠️ <b>Срок действия сессии входа истёк (5 минут).</b>\n\n' +
          'Нажмите кнопку «Войти через Telegram» на сайте или в приложении заново.',
          { parse_mode: 'HTML', reply_markup: createMainMenuKeyboard() }
        );
        return;
      }

      const fullName = [ctx.from?.first_name, ctx.from?.last_name].filter(Boolean).join(' ') || username || 'Telegram Атлет';
      let user = null;
      if (tgId) user = db.findUserByTelegramId(tgId);
      if (!user && username) user = db.findUserByTelegramUsername(username);
      if (!user && username) user = db.findUserByUsername(username);
      if (!user && username) user = db.findUserByUsername(`tg_${username}`);

      if (!user) {
        const usernameKey = username ? `tg_${username}` : `tg_${tgId}`;
        const passwordHash = hashPassword(crypto.randomBytes(24).toString('hex'));
        const pairingCode = db.generateUniquePairingCode();
        const clientUuid = crypto.randomUUID();

        const userId = db.createUser(
          usernameKey,
          passwordHash,
          'athlete',
          escapeHtml(fullName),
          '',
          pairingCode,
          clientUuid,
          ''
        );
        db.linkTelegram(userId, tgId, username || '');
        if (cloudSyncService && typeof cloudSyncService.registerAthletePairing === 'function') {
          cloudSyncService.registerAthletePairing(pairingCode, clientUuid, escapeHtml(fullName), '', '').catch(() => {});
        }
        user = db.findUserById(userId);
      } else {
        if (!user.telegram_id && tgId) {
          db.linkTelegram(user.id, tgId, username || '');
          user = db.findUserById(user.id);
        }
      }

      // Security check: 2FA Enforcement
      // If 2FA is enabled on the account, 1-Click login MUST NOT bypass 2FA!
      if (user.two_factor_enabled === 1) {
        if (telegramOtpStore) {
          telegramOtpStore.delete(`2fa_${user.id}`);
        }
        const otp = generateSecurePin();
        const expiresAt = Date.now() + 5 * 60 * 1000;
        if (telegramOtpStore) {
          telegramOtpStore.set(`2fa_${user.id}`, {
            userId: user.id,
            code: otp,
            expiresAt,
            attempts: 0
          });
        }

        session.status = 'REQUIRES_2FA';
        session.userId = user.id;
        session.expiresInSeconds = 300;

        await ctx.reply(
          `🛡️ <b>Внимание: Для вашего аккаунта включена 2FA аутентификация!</b>\n\n` +
          `Вход в 1 клик заблокирован политикой безопасности.\n` +
          `Ваш 6-значный одноразовый код для подтверждения входа:\n\n` +
          `👉 <code>${otp}</code> 👈\n` +
          `<i>(нажмите на код, чтобы скопировать)</i>\n\n` +
          `⏱ Действует: <b>5 минут</b>.\n` +
          `Введите этот код в окне браузера или приложения для завершения авторизации.`,
          {
            parse_mode: 'HTML',
            reply_markup: createOwnersKeyboard()
          }
        );
        return;
      }

      const token = generateToken();
      db.createAuthToken(token, user.id);

      session.status = 'AUTHORIZED';
      session.token = token;
      session.user = {
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
      };

      const roleTitle = user.role === 'trainer' ? 'Тренер' : 'Атлет';
      const webAppKeyboard = new InlineKeyboard()
        .url('🌐 Открыть Fitness Ecosystem', 'https://fitness-ecosystem-pro.onrender.com')
        .row()
        .url('💬 Поддержка создателей', OWNER_LINKS.santi);

      await ctx.reply(
        `⚡ <b>Вход в Fitness Ecosystem Pro выполнен в 1 клик!</b>\n\n` +
        `👤 Профиль: <b>${user.full_name || fullName}</b>\n` +
        `🏷 Роль: <b>${roleTitle}</b>\n\n` +
        `🚀 В браузере или мобильном приложении вход произошёл автоматически!\n` +
        `Вы можете вернуться в открытое окно.`,
        {
          parse_mode: 'HTML',
          reply_markup: webAppKeyboard
        }
      );
      return;
    }

    if (payload.startsWith('link_')) {
      const token = payload.replace(/^link_/, '').trim();
      await handleLinkToken(ctx, token, db, userTgChatMap);
      return;
    }

    // Dedicated deep-link handler: /start login (?start=login)
    if (payload === 'login') {
      const { code } = getUnifiedUserCode(tgId, username, db, telegramOtpStore, cloudSyncService);
      await ctx.reply(
        `🔐 <b>Код авторизации в системе:</b>\n\n` +
        `👉 <code>${code}</code> 👈\n\n` +
        `⏱ Код действителен в течение <b>5 минут</b>.\n` +
        `Введите этот код в форме входа на сайте или в приложении.\n\n` +
        `⚠️ <b>Безопасность:</b> Никому не передавайте этот код!`,
        {
          parse_mode: 'HTML',
          reply_markup: createMainMenuKeyboard()
        }
      );
      return;
    }

    // Unified 5-minute code
    const { code, user } = getUnifiedUserCode(tgId, username, db, telegramOtpStore, cloudSyncService);

    const hint = (user && user.role === 'athlete')
      ? 'ℹ️ Этот же код отображается в вашем профиле на сайте и в мобильном приложении.\n\n'
      : '';

    await ctx.reply(
      `👋 Привет, <b>${ctx.from?.first_name || 'атлет'}</b>!\n\n` +
      `Добро пожаловать в <b>Fitness Ecosystem Pro</b> — платформу для атлетов и персональных тренеров.\n\n` +
      `🔐 Ваш единый код для входа на сайт и подключения тренера:\n\n` +
      `👉 <code>${code}</code> 👈\n` +
      `<i>(нажмите на код, чтобы скопировать)</i>\n\n` +
      `⏱ Код действует <b>5 минут</b>.\n\n` +
      hint +
      `Используйте кнопки меню ниже для быстрого управления:`,
      {
        parse_mode: 'HTML',
        reply_markup: createMainMenuKeyboard()
      }
    );
  });

  // /link <token> command for manual token entry
  bot.command('link', async (ctx) => {
    const rawToken = (ctx.match || '').trim();
    if (!rawToken) {
      return ctx.reply(
        '🔗 <b>Привязка аккаунта к Telegram</b>\n\n' +
        '🔑 <b>Что такое токен привязки:</b>\n' +
        'Это одноразовый 5-минутный код для связки вашего профиля (атлета или тренера) на сайте или в приложении с этим Telegram-ботом.\n\n' +
        '📌 <b>Откуда его брать:</b>\n' +
        '1. Откройте сайт или мобильное приложение <b>Fitness Ecosystem Pro</b>.\n' +
        '2. Перейдите в раздел <b>Профиль</b> (или карточку тренера/атлета).\n' +
        '3. Нажмите кнопку <b>«Привязать Telegram»</b> — система сгенерирует токен.\n' +
        '4. Скопируйте токен или нажмите прямую кнопку перехода в Telegram.\n\n' +
        '💬 <b>Формат команды:</b>\n' +
        '<code>/link &lt;токен&gt;</code>',
        {
          parse_mode: 'HTML',
          reply_markup: createMainMenuKeyboard()
        }
      );
    }
    await handleLinkToken(ctx, rawToken, db, userTgChatMap);
  });

  // Button handler: 🔑 Код входа
  bot.hears(['🔑 Код входа', '/code', '/login'], async (ctx) => {
    const username = ctx.from?.username ? ctx.from.username.replace(/^@/, '').toLowerCase() : null;
    const tgId = String(ctx.from?.id || '');
    const { code, user } = getUnifiedUserCode(tgId, username, db, telegramOtpStore, cloudSyncService);

    const hint = (user && user.role === 'athlete')
      ? '\n\nℹ️ Этот же код отображается в вашем профиле на сайте и в мобильном приложении.'
      : '';

    await ctx.reply(
      `🔐 Ваш единый код для входа на сайт и подключения тренера:\n\n` +
      `👉 <code>${code}</code> 👈\n` +
      `<i>(нажмите на код, чтобы скопировать)</i>\n\n` +
      `⏱ Код действует <b>5 минут</b>.` +
      hint,
      {
        parse_mode: 'HTML',
        reply_markup: createMainMenuKeyboard()
      }
    );
  });

  // Button handler: 🔗 Привязать аккаунт
  bot.hears(['🔗 Привязать аккаунт'], async (ctx) => {
    await ctx.reply(
      `🔗 <b>Привязка аккаунта к Telegram</b>\n\n` +
      `🔑 <b>Откуда брать токен привязки:</b>\n` +
      `1. Откройте сайт или мобильное приложение <b>Fitness Ecosystem Pro</b>.\n` +
      `2. Перейдите в раздел <b>Профиль</b> (или карточку тренера/атлета).\n` +
      `3. Нажмите <b>«Привязать Telegram»</b> для генерации 5-минутного токена.\n` +
      `4. Отправьте в этот чат команду:\n\n` +
      `<code>/link &lt;токен&gt;</code>`,
      {
        parse_mode: 'HTML',
        reply_markup: createMainMenuKeyboard()
      }
    );
  });

  // Button handler: 💬 Связь с владельцами
  bot.hears(['💬 Связь с владельцами', '/contacts'], async (ctx) => {
    await ctx.reply(
      `🤝 <b>Связь с создателями Fitness Ecosystem Pro:</b>\n\n` +
      `• <b>@SantiLA213</b> — Архитектура, продукты и партнерства\n` +
      `• <b>@Spirit5449</b> — Backend, безопасность и мобильная разработка\n\n` +
      `Нажмите на кнопку ниже, чтобы начать диалог в Telegram:`,
      {
        parse_mode: 'HTML',
        reply_markup: createOwnersKeyboard()
      }
    );
  });

  // Button handler: ❓ Справка
  bot.hears(['❓ Справка', '/help'], async (ctx) => {
    await ctx.reply(
      `📖 <b>Справка Fitness Ecosystem Pro:</b>\n\n` +
      `• <b>🔑 Код входа</b> — Получить свежий 6-значный одноразовый код\n` +
      `• <b>🔗 Привязать аккаунт</b> — Инструкция по привязке Telegram\n` +
      `• <b>💬 Связь с владельцами</b> — Контакты основателей проекта\n\n` +
      `🔐 <b>Безопасность (Zero-Trust):</b>\n` +
      `Все одноразовые пароли и коды привязки действуют строго <b>5 минут (300 секунд)</b>.`,
      {
        parse_mode: 'HTML',
        reply_markup: createMainMenuKeyboard()
      }
    );
  });

  // Inline button callback query handler for trainer approval
  if (typeof bot.callbackQuery === 'function') {
    bot.callbackQuery(/^approve_(\d+)$/, async (ctx) => {
      const callerUsername = (ctx.from?.username || '').replace(/^@/, '');
      const callerTgId = String(ctx.from?.id || '');
      const adminChatId = String(process.env.ADMIN_CHAT_ID || '').trim();
      const isAuthorized =
        ['SantiLA213', 'Spirit5449'].some(u => u.toLowerCase() === callerUsername.toLowerCase()) ||
        (adminChatId && callerTgId === adminChatId);

      if (!isAuthorized) {
        await ctx.answerCallbackQuery({ text: '⛔ У вас нет прав администратора', show_alert: true });
        return;
      }

      const targetId = parseInt(ctx.match[1], 10);
      if (targetId) {
        db.approveTrainer(targetId);
        const approvedUser = db.findUserById(targetId);
        await ctx.answerCallbackQuery({ text: `✅ Тренер #${targetId} успешно одобрен!` });
        await ctx.editMessageReplyMarkup({ reply_markup: { inline_keyboard: [] } }).catch(() => {});
        await ctx.reply(
          `✅ <b>АККАУНТ ТРЕНЕРА ОДОБРЕН КНОПКОЙ!</b>\n\n` +
          `Тренер #${targetId} (<b>${approvedUser?.full_name || 'Тренер'}</b>) получил доступ на сайте.`,
          { parse_mode: 'HTML' }
        );
      }
    });
  }

  // Text messages fallback & approval handler
  bot.on('message:text', async (ctx) => {
    const username = ctx.from?.username ? ctx.from.username.replace(/^@/, '').toLowerCase() : null;
    const tgId = String(ctx.from?.id || '');
    if (username && userTgChatMap) userTgChatMap.set(username, ctx.chat?.id || tgId);
    if (tgId && userTgChatMap) userTgChatMap.set(tgId, ctx.chat?.id || tgId);

    const text = ctx.message.text.trim();

    // Trainer approval command (/approve_<id> or /approve <username/id>)
    if (text.startsWith('/approve')) {
      const callerUsername = (ctx.from?.username || '').replace(/^@/, '');
      const callerTgId = String(ctx.from?.id || '');
      const adminChatId = String(process.env.ADMIN_CHAT_ID || '').trim();
      const isAuthorized =
        ['SantiLA213', 'Spirit5449'].some(u => u.toLowerCase() === callerUsername.toLowerCase()) ||
        (adminChatId && callerTgId === adminChatId);

      if (!isAuthorized) {
        await ctx.reply('У вас нет прав администратора');
        return;
      }

      const rawTarget = text.replace(/^\/approve_?/, '').trim();
      if (rawTarget) {
        db.approveTrainer(rawTarget);
        const approvedUser = db.findUserById(parseInt(rawTarget, 10)) || db.findUserByUsername(rawTarget);
        await ctx.reply(
          `✅ <b>АККАУНТ ТРЕНЕРА ПОДТВЕРЖДЕН!</b>\n\n` +
          `Тренер <b>${approvedUser?.full_name || 'Тренер'}</b> (@${approvedUser?.username || rawTarget}) теперь имеет полный доступ к созданию планов и ведению подопечных на сайте.`,
          { parse_mode: 'HTML' }
        );
        return;
      }
    }

    // Direct /link message if not recognized command
    if (text.startsWith('/link')) {
      const parts = text.split(/\s+/);
      if (parts.length > 1) {
        await handleLinkToken(ctx, parts[1], db, userTgChatMap);
        return;
      }
    }

    // Default text fallback: generate or return unified 5-min code
    if (!text.startsWith('/start') && !text.startsWith('/code') && !text.startsWith('/login') && !text.startsWith('/contacts') && !text.startsWith('/help')) {
      const { code, user } = getUnifiedUserCode(tgId, username, db, telegramOtpStore, cloudSyncService);
      const hint = (user && user.role === 'athlete')
        ? '\n\nℹ️ Этот же код отображается в вашем профиле на сайте и в мобильном приложении.'
        : '';
      await ctx.reply(
        `🔐 Ваш единый код для входа на сайт и подключения тренера:\n\n` +
        `👉 <code>${code}</code> 👈\n` +
        `<i>(нажмите на код, чтобы скопировать)</i>\n\n` +
        `⏱ Действует <b>5 минут</b>.` +
        hint,
        {
          parse_mode: 'HTML',
          reply_markup: createOwnersKeyboard()
        }
      );
    }
  });
}

/**
 * Deliver 2FA OTP code to user's Telegram chat
 */
async function send2FAOtp(bot, telegramId, otp) {
  if (!bot || !bot.api || !telegramId) return false;
  try {
    await bot.api.sendMessage(
      telegramId,
      `🔐 <b>Код двухфакторной аутентификации (2FA):</b>\n\n` +
      `👉 <code>${otp}</code> 👈\n` +
      `<i>(нажмите на код, чтобы скопировать)</i>\n\n` +
      `⏱ Действует: <b>5 минут</b>.\n` +
      `Введите этот код в форму на сайте для подтверждения входа.\n` +
      `Никому не сообщайте этот код!`,
      {
        parse_mode: 'HTML',
        reply_markup: createOwnersKeyboard()
      }
    );
    return true;
  } catch (err) {
    console.warn(`[Telegram 2FA] Не удалось отправить OTP в чат ${telegramId}:`, err.message);
    return false;
  }
}

/**
 * Initialize grammY Bot
 */
function initTelegramBot({ token, db, telegramOtpStore, telegramSessionStore, userTgChatMap, cloudSyncService }) {
  if (!token) return null;
  try {
    const bot = new Bot(token);
    bot.catch((err) => {
      console.error('grammY error boundary:', err);
    });
    setupBotHandlers(bot, { db, telegramOtpStore, telegramSessionStore, userTgChatMap, cloudSyncService });

    // Set menu commands
    bot.api.setMyCommands([
      { command: 'start', description: 'Запуск бота / привязка аккаунта' },
      { command: 'code', description: 'Получить 6-значный 2FA код' },
      { command: 'link', description: 'Привязать аккаунт: /link <токен>' },
      { command: 'contacts', description: 'Связь с создателями (@SantiLA213, @Spirit5449)' },
      { command: 'help', description: 'Справка и безопасность' }
    ]).catch(err => {
      console.warn('[Telegram Bot] Предупреждение setMyCommands:', err.message);
    });

    bot.start({
      onStart: (info) => {
        console.log(`[Telegram Bot] 🚀 @${info.username} успешно запущен в режиме Long Polling!`);
      }
    }).catch(err => {
      console.warn('[Telegram Bot] Ошибка polling:', err.message);
    });

    return bot;
  } catch (err) {
    console.warn('[Telegram Bot] Ошибка инициализации:', err.message);
    return null;
  }
}

module.exports = {
  OWNER_LINKS,
  createOwnersKeyboard,
  handleLinkToken,
  setupBotHandlers,
  send2FAOtp,
  initTelegramBot
};
