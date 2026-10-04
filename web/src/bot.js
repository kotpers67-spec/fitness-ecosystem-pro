/**
 * Telegram Bot module for Fitness Ecosystem Pro
 * Framework: grammY (v1.28.0)
 * Features: Deep Linking (/start link_<token>, /link <token>), 2FA OTP delivery,
 *           Owner Contact buttons (@SantiLA213, @Spirit5449), Trainer approval.
 */

const { Bot, InlineKeyboard, Keyboard } = require('grammy');

const OWNER_LINKS = {
  santi: 'https://t.me/SantiLA213',
  spirit: 'https://t.me/Spirit5449'
};

/**
 * Creates persistent menu reply keyboard with main buttons
 */
function createMainMenuKeyboard() {
  return new Keyboard()
    .text('🔑 Код входа').text('🔗 Привязать аккаунт').row()
    .text('💬 Связь с владельцами').text('❓ Справка')
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
 * Setup command handlers and event listeners on grammY Bot instance
 */
function setupBotHandlers(bot, { db, telegramOtpStore, userTgChatMap }) {
  // /start handler with deep-linking support (/start link_<token>)
  bot.command('start', async (ctx) => {
    const username = ctx.from?.username ? ctx.from.username.replace(/^@/, '').toLowerCase() : null;
    const chatId = ctx.chat?.id;
    const tgId = String(ctx.from?.id || '');

    if (username && userTgChatMap) userTgChatMap.set(username, chatId);
    if (tgId && userTgChatMap) userTgChatMap.set(tgId, chatId);

    const payload = (ctx.match || '').trim();
    if (payload.startsWith('link_')) {
      const token = payload.replace(/^link_/, '').trim();
      await handleLinkToken(ctx, token, db, userTgChatMap);
      return;
    }

    // Default /start greeting with fresh 5-min login code
    const code = String(Math.floor(100000 + Math.random() * 900000));
    const expiresAt = Date.now() + 5 * 60 * 1000;

    if (telegramOtpStore) {
      if (username) telegramOtpStore.set(username, { code, expiresAt, attempts: 0 });
      telegramOtpStore.set(`id_${tgId}`, { code, expiresAt, attempts: 0 });
    }

    await ctx.reply(
      `👋 Привет, <b>${ctx.from?.first_name || 'атлет'}</b>!\n\n` +
      `Добро пожаловать в <b>Fitness Ecosystem Pro</b> — платформу для атлетов и персональных тренеров.\n\n` +
      `🔐 Ваш одноразовый код для входа на сайт:\n\n` +
      `👉 <code>${code}</code> 👈\n` +
      `<i>(нажмите на код, чтобы скопировать)</i>\n\n` +
      `⏱ Код действует <b>5 минут</b>.\n\n` +
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
    const code = String(Math.floor(100000 + Math.random() * 900000));
    const expiresAt = Date.now() + 5 * 60 * 1000;

    if (telegramOtpStore) {
      if (username) telegramOtpStore.set(username, { code, expiresAt, attempts: 0 });
      telegramOtpStore.set(`id_${tgId}`, { code, expiresAt, attempts: 0 });
    }

    await ctx.reply(
      `🔐 Ваш одноразовый код для входа на сайт:\n\n` +
      `👉 <code>${code}</code> 👈\n` +
      `<i>(нажмите на код, чтобы скопировать)</i>\n\n` +
      `⏱ Код действует <b>5 минут</b>.`,
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

  // Text messages fallback & approval handler
  bot.on('message:text', async (ctx) => {
    const username = ctx.from?.username ? ctx.from.username.replace(/^@/, '').toLowerCase() : null;
    const tgId = String(ctx.from?.id || '');
    if (username && userTgChatMap) userTgChatMap.set(username, ctx.chat?.id || tgId);
    if (tgId && userTgChatMap) userTgChatMap.set(tgId, ctx.chat?.id || tgId);

    const text = ctx.message.text.trim();

    // Trainer approval command (/approve_<id>)
    if (text.startsWith('/approve_')) {
      const targetId = parseInt(text.replace('/approve_', ''), 10);
      if (targetId) {
        db.approveTrainer(targetId);
        const approvedUser = db.findUserById(targetId);
        await ctx.reply(
          `✅ <b>АККАУНТ ТРЕНЕРА ПОДТВЕРЖДЕН!</b>\n\n` +
          `Тренер #${targetId} (<b>${approvedUser?.full_name || 'Тренер'}</b>) теперь имеет полный доступ к созданию планов и ведению подопечных на сайте.`,
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

    // Default text fallback: generate fresh 5-min code
    if (!text.startsWith('/start') && !text.startsWith('/code') && !text.startsWith('/login') && !text.startsWith('/contacts') && !text.startsWith('/help')) {
      const code = String(Math.floor(100000 + Math.random() * 900000));
      const expiresAt = Date.now() + 5 * 60 * 1000;
      if (telegramOtpStore) {
        if (username) telegramOtpStore.set(username, { code, expiresAt, attempts: 0 });
        telegramOtpStore.set(`id_${tgId}`, { code, expiresAt, attempts: 0 });
      }
      await ctx.reply(
        `🔐 Ваш код для входа на сайт:\n\n` +
        `👉 <code>${code}</code> 👈\n` +
        `<i>(нажмите на код, чтобы скопировать)</i>\n\n` +
        `⏱ Действует <b>5 минут</b>.`,
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
function initTelegramBot({ token, db, telegramOtpStore, userTgChatMap }) {
  if (!token) return null;
  try {
    const bot = new Bot(token);
    setupBotHandlers(bot, { db, telegramOtpStore, userTgChatMap });

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
