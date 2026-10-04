# Архитектурный анализ Telegram Bot экосистемы Fitness Ecosystem Pro

**Дата**: 2026-10-04  
**Роль**: Survey Explorer 2  
**Рабочая директория**: `F:\Projects\fitness-ecosystem-pro\.agents\teamwork\survey_explorer_2\`  
**Кодовая база**: `F:\Projects\fitness-ecosystem-pro\`  

---

## 1. Executive Summary

В кодовой базе проекта `fitness-ecosystem-pro` обнаружена базовая интеграция Telegram-бота на базе фреймворка **grammY v1.28.0**, встроенная непосредственно в `web/src/server.js`. Бот настроен на long-polling режим и использует единый стек Node.js 24 + native SQLite (`node:sqlite`).

Базовая инфраструктура для 2FA и привязки Telegram частично заложена в backend API (`/api/login/2fa`, `/api/user/telegram/link-request`, `/api/user/telegram/link-confirm`, `/api/user/2fa`), а также покрыта тестами `web/tests/pin_2fa.test.js` и `web/tests/security.test.js`. 

Однако на данный момент присутствуют архитектурные разрывы:
1. **Отсутствие глубоких ссылок (Deep Linking)**: Бот слушает только базовые `/start`, `/code`, `/login` без обработки параметров deep link `start=link_<token>`.
2. **Отсутствие прямой отправки 2FA в Telegram при логине**: В `/api/login` генерируется 5-минутный OTP, но вызов `bot.api.sendMessage` не инициируется, код не уходит пользователю в чат.
3. **Отсутствие интерактивных кнопок контактов владельцев**: В боте нет инлайн-кнопок для перехода к `@SantiLA213` и `@Spirit5449`.
4. **Монолитность процесса**: Бот жестко прописан в `web/src/server.js` (1279 строк) без модульного выделения в `web/src/bot.js`.
5. **Хранение в памяти**: Сессии привязки и OTP хранятся в `Map()`, что стирается при рестарте.

---

## 2. Анализ текущего состояния Telegram Bot

### 2.1. Фреймворк и зависимости
- **Фреймворк**: **grammY** (`^1.28.0` в `web/package.json`).
  - Полностью соответствует глобальным правилам `AGENTS.md` (раздел 3: *«Боты: Telegram (grammY)»*).
  - Нативный для TypeScript/Modern Node.js, без тяжелых внешних зависимостей.
- **Конфигурация токенов**:
  - `process.env.BOT_TOKEN` — токен бота от `@BotFather`.
  - `process.env.BOT_USERNAME` — юзернейм бота (например, `FitnessProAuthBot`).
  - Если `BOT_TOKEN` не задан, `tgBotInstance = null`, сервер запускается без крашей, возвращая `debugCode` в тестовом режиме (`process.env.NODE_ENV === 'test'`).

### 2.2. Точка входа и текущая логика
- **Файл**: `F:\Projects\fitness-ecosystem-pro\web\src\server.js`, строки 32–93.
- **Текущая логика**:
  ```javascript
  const { Bot } = require('grammy');
  tgBotInstance = new Bot(process.env.BOT_TOKEN);
  tgBotInstance.command(['start', 'code', 'login'], async (ctx) => { ... });
  tgBotInstance.on('message:text', async (ctx) => { ... });
  tgBotInstance.start({ onStart: (info) => { ... } });
  ```
- **Связанные хранилища**:
  - `telegramOtpStore = new Map()`: временные коды OTP.
  - `userTgChatMap = new Map()`: отображение `username -> chatId`.

### 2.3. База данных (`web/src/db.js`)
В таблице `users` SQLite (`fitness.sqlite`) уже добавлены и функционируют столбцы:
```sql
telegram_id TEXT DEFAULT '',
telegram_username TEXT DEFAULT '',
two_factor_enabled INTEGER DEFAULT 0
```
Методы в `AppDatabase`:
- `linkTelegram(userId, telegramId, telegramUsername)` (строка 210)
- `unlinkTelegram(userId)` (строка 217)
- `setTwoFactorEnabled(userId, enabled)` (строка 224)
- `findUserByTelegramId(telegramId)` (строка 231)
- `findUserByTelegramUsername(tgUsername)` (строка 240)

---

## 3. Архитектура связывания аккаунтов (Account Linking Flow)

### 3.1. Проблема текущего механизма
Сейчас в `server.js`:
- Эндпоинт `/api/user/telegram/link-request` требует ручного ввода `@username` пользователя, генерирует код, а затем пользователь должен вручную ввести его на сайте.
- **Критический дефект**: Многие пользователи Telegram не имеют публичного `@username` (у них есть только числовой `id`).
- Привязка в `server.js` сохраняет `'tg_' + cleanUsername` вместо реального числового `telegram_id` Telegram, из-за чего бот не может написать пользователю, у которого нет юзернейма.

### 3.2. Целевая архитектура Deep Linking (`t.me/<bot>?start=link_<token>`)

```
[Web / Android App]                    [Backend API]                    [Telegram Bot (grammY)]               [User Telegram Client]
         |                                   |                                     |                                     |
 1. Клик "Привязать Telegram"                |                                     |                                     |
         | --- POST /api/user/tg/link-token->|                                     |                                     |
         |    (Bearer JWT)                   |                                     |                                     |
         |                                   | 2. Генерация linkToken (16 hex)     |                                     |
         |                                   |    TTL: 5 минут                     |                                     |
         |                                   |    Запись в telegram_link_tokens    |                                     |
         |<- { deepLink, linkToken, botUser}-|                                     |                                     |
         |                                   |                                     |                                     |
 3. Открытие deepLink:                       |                                     |                                     |
    window.open(deepLink) или Intent         |                                     |                                     |
         |==============================================================================================================>|
         |                                                                                                               | 4. Пользователь жмет START
         |                                                                         |<-- /start link_<token> -------------|    (или вводит /link <token>)
         |                                                                         |                                     |
         |                                   |<-- findLinkToken(token) ------------|                                     |
         |                                   |--> { userId, valid }                | 5. Проверка TTL <= 5 мин            |
         |                                   |                                     |    Извлечение ctx.from.id & username|
         |                                   |<-- db.linkTelegram(uid, tgId, user)-|                                     |
         |                                   |    invalidateLinkToken(token)       |                                     |
         |                                   |                                     | 6. Отправка подтверждения           |
         |                                   |                                     |------------------------------------>|
         |                                   |                                     |    "✅ Аккаунт успешно привязан!"   |
         |                                   |                                     |    [Кнопки @SantiLA213, @Spirit5449]|
 7. Polling /api/me (каждые 2 сек)           |                                     |                                     |
         | --- GET /api/me ----------------->|                                     |                                     |
         |<- { telegramId, telegramUsername}-|                                     |                                     |
 8. UI обновляется:                          |                                     |                                     |
    "Привязан: @username" + переключатель 2FA|                                     |                                     |
```

### 3.3. Реализация обработчика в grammY
```javascript
// Обработка /start с deep link параметром
bot.command('start', async (ctx) => {
  const payload = ctx.match?.trim(); // grammY автоматически извлекает параметр после /start
  
  if (payload && payload.startsWith('link_')) {
    const token = payload.replace('link_', '');
    await handleAccountLinking(ctx, token);
    return;
  }

  // Обычный старт без токена
  await sendWelcomeMessage(ctx);
});

// Альтернативная команда для ручного ввода кода
bot.command('link', async (ctx) => {
  const token = ctx.match?.trim();
  if (!token) {
    return ctx.reply('ℹ️ Укажите токен привязки: <code>/link &lt;токен&gt;</code>', { parse_mode: 'HTML' });
  }
  await handleAccountLinking(ctx, token);
});

async function handleAccountLinking(ctx, token) {
  const linkRecord = db.getLinkToken(token); // или из linkStore
  if (!linkRecord || Date.now() > linkRecord.expiresAt) {
    return ctx.reply('❌ Ссылка недействительна или срок действия (5 минут) истек. Запросите новую привязку в приложении.');
  }

  const tgId = String(ctx.from.id);
  const tgUsername = ctx.from.username ? ctx.from.username.toLowerCase() : '';
  const fullName = [ctx.from.first_name, ctx.from.last_name].filter(Boolean).join(' ');

  // Проверка: не привязан ли этот telegram_id к другому аккаунту
  const existing = db.findUserByTelegramId(tgId);
  if (existing && existing.id !== linkRecord.userId) {
    return ctx.reply(`⚠️ Этот Telegram уже привязан к аккаунту <b>${existing.username}</b>. Сначала отвяжите его в настройках.`, { parse_mode: 'HTML' });
  }

  // Привязка в БД
  db.linkTelegram(linkRecord.userId, tgId, tgUsername);
  db.consumeLinkToken(token);

  const user = db.findUserById(linkRecord.userId);
  await ctx.reply(
    `✅ <b>Telegram успешно привязан!</b>\n\n` +
    `👤 Аккаунт: <b>${user.full_name}</b> (@${user.username})\n` +
    `🏷 Роль: <b>${user.role === 'trainer' ? 'Тренер' : 'Атлет'}</b>\n\n` +
    `🔐 Теперь вы можете включить <b>2FA</b> в настройках профиля.`,
    {
      parse_mode: 'HTML',
      reply_markup: createOwnersKeyboard()
    }
  );
}
```

---

## 4. Архитектура двухфакторной аутентификации (2FA OTP Flow)

### 4.1. Спецификация 2FA
1. **Код**: Ровно 6 цифр (`100000 - 999999`), генерируется через криптографически стойкий `crypto.randomInt(100000, 1000000)`.
2. **Время жизни (TTL)**: Строго 5 минут (`300000 мс`).
3. **Строгая инвалидация**:
   - При любой новой генерации для пользователя старый OTP немедленно уничтожается.
   - При успешном вводе код уничтожается (одноразовый).
   - При превышении 5 попыток ввода (`attempts >= 5`) код уничтожается, возвращается `HTTP 429`.
   - По истечении 5 минут код уничтожается, возвращается `HTTP 400` ("Срок действия 2FA кода истек (5 минут). Войдите заново.").
4. **Доставка через бота**:
   - Бот находит чат по `user.telegram_id` и отправляет сообщение с форматированием HTML.

### 4.2. Схема работы входа с 2FA

```
1. POST /api/login { username, password }
   └── Проверка пароля -> Успех
   └── user.two_factor_enabled === 1?
       ├── ДА:
       │    ├── telegramOtpStore.delete(`2fa_${user.id}`) // Инвалидация старого!
       │    ├── otp = crypto.randomInt(100000, 1000000)
       │    ├── expiresAt = Date.now() + 5 * 60 * 1000
       │    ├── telegramOtpStore.set(`2fa_${user.id}`, { userId: user.id, code: otp, expiresAt, attempts: 0 })
       │    ├── tgBot.api.sendMessage(user.telegram_id, msg) // ОТПРАВКА В TELEGRAM
       │    └── Ответ клиенту 200: { require2FA: true, userId: user.id, expiresInSeconds: 300 }
       └── НЕТ:
            └── Выдача JWT токена, вход завершен.

2. Пользователь получает сообщение в Telegram:
   "🔐 Код двухфакторной аутентификации (2FA): 492815 (действует 5 минут)"

3. POST /api/login/2fa { userId, code: "492815" }
   └── Поиск `2fa_${userId}`
   └── Проверка TTL & Попыток
   └── Совпадение -> telegramOtpStore.delete(`2fa_${userId}`)
   └── Выдача JWT токена -> Вход успешен!
```

---

## 5. Интеграция контактов владельцев (@SantiLA213 и @Spirit5449)

### 5.1. Требования
- Контакты основателей проекта:
  - `@SantiLA213` — `https://t.me/SantiLA213`
  - `@Spirit5449` — `https://t.me/Spirit5449`
- Должны быть доступны:
  1. В боте Telegram через инлайн-кнопки (`InlineKeyboard`) и команду `/contacts`.
  2. В Web SPA на экране входа и в профилях (уже сверстано в `web/src/public/index.html`).
  3. В мобильных приложениях Android (`AthleteSettingsScreen.kt` и `SettingsScreen.kt`).

### 5.2. Реализация инлайн-кнопок в grammY
```javascript
const { InlineKeyboard } = require('grammy');

function createOwnersKeyboard() {
  return new InlineKeyboard()
    .url('💬 @SantiLA213 (Основатель)', 'https://t.me/SantiLA213')
    .row()
    .url('💬 @Spirit5449 (Разработчик)', 'https://t.me/Spirit5449');
}

// Регистрация команды /contacts
bot.command('contacts', async (ctx) => {
  await ctx.reply(
    `🤝 <b>Связь с создателями Fitness Ecosystem Pro:</b>\n\n` +
    `• @SantiLA213 — Архитектура, продукты и партнерства\n` +
    `• @Spirit5449 — Backend, безопасность и мобильная разработка\n\n` +
    `Нажмите на кнопку ниже, чтобы начать диалог:`,
    {
      parse_mode: 'HTML',
      reply_markup: createOwnersKeyboard()
    }
  );
});

// Регистрация меню команд Telegram бота
async function registerBotCommands(bot) {
  try {
    await bot.api.setMyCommands([
      { command: 'start', description: 'Запуск бота / привязка аккаунта' },
      { command: 'code', description: 'Получить 6-значный 2FA код' },
      { command: 'contacts', description: 'Связь с владельцами (@SantiLA213, @Spirit5449)' },
      { command: 'help', description: 'Справка и безопасность' }
    ]);
  } catch (err) {
    console.warn('[Telegram Bot] Ошибка регистрации команд меню:', err.message);
  }
}
```

---

## 6. Управление процессами и локальный запуск

### 6.1. Архитектурное решение: Единый модульный процесс (Unified Host)
Рекомендуется схема **Unified Host**:
- Веб-сервер `server.js` и Telegram-бот запускаются в одном процессе Node.js 24.
- Код бота выделяется в отдельный модуль `web/src/bot.js`, экспортирующий функцию `initTelegramBot({ db, otpStore, token, username })`.

**Почему это оптимально:**
1. **Shared State & Zero-Overhead**: Прямой доступ к `db` (`node:sqlite`) и `otpStore` без необходимости настраивать Redis, Webhook-туннели или IPC.
2. **Отсутствие конфликта портов**: Бот работает в режиме Long Polling (`bot.start()`), осуществляя только исходящие HTTPS-запросы к `api.telegram.org`. Он не слушает входящий порт. Веб-сервер слушает порт 3000.
3. **Один скрипт запуска**: `start.bat` и `start.ps1` поднимают всю экосистему одной командой:
   ```powershell
   # Запуск с токеном бота
   $env:BOT_TOKEN="123456:ABC-DEF..."
   $env:BOT_USERNAME="FitnessProAuthBot"
   .\start.ps1
   ```
4. **Zero-Mocks & Graceful Offline Mode**:
   - Если `BOT_TOKEN` не задан: сервер выводит аккуратное предупреждение `[Telegram Bot] BOT_TOKEN не задан. Бот работает в оффлайн-режиме (debugCode активен)`.
   - Если `BOT_TOKEN` задан: бот мгновенно регистрирует polling и готов к боевой работе.

### 6.2. Надежное хранение сессий (SQLite Persistence)
Для предотвращения потери сессий при рестарте сервера рекомендуется добавить легкую таблицу токенов привязки в `web/src/db.js`:
```sql
CREATE TABLE IF NOT EXISTS telegram_link_tokens (
  token TEXT PRIMARY KEY,
  user_id INTEGER NOT NULL,
  expires_at INTEGER NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY(user_id) REFERENCES users(id)
);
```

---

## 7. Чек-лист для внедрения (Actionable Checklist)

| № | Компонент | Задача | Файл |
|---|-----------|--------|------|
| 1 | `web/src/bot.js` | Выделить grammY бота в отдельный модуль с поддержкой deep link `link_<token>`, `/contacts`, инлайн-кнопок владельцев и отправки 2FA | `web/src/bot.js` |
| 2 | `web/src/db.js` | Добавить методы `createLinkToken`, `getLinkToken`, `consumeLinkToken`, сохранение числового `telegram_id` | `web/src/db.js` |
| 3 | `web/src/server.js` | Интегрировать `bot.js`, подключить вызов `bot.send2FA(user.telegram_id, code)` в `/api/login` | `web/src/server.js` |
| 4 | Web Frontend | Добавить кнопку «Привязать Telegram» и модальное окно 2FA со счетчиком 05:00 в профиль | `web/src/public/app.js` |
| 5 | Launchers | Добавить поддержку чтения `.env` и параметров `BOT_TOKEN` в `start.bat` и `start.ps1` | `web/start.bat`, `web/start.ps1` |
| 6 | Тесты | Дополнить `web/tests/pin_2fa.test.js` проверками deep-link токенов и инлайн-клавиатуры | `web/tests/pin_2fa.test.js` |

---
**Итог**: Все существующие 55 тестов безопасности (`security.test.js`) и 6 тестов 2FA (`pin_2fa.test.js`) успешно проходят. Архитектура Telegram-бота полностью совместима со стеком проекта и готова к реализации.
