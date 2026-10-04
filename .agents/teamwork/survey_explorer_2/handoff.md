# Handoff Report — Survey Explorer 2 (Telegram Bot Architecture)

## 1. Observation

1. **Framework & Dependencies**:
   - `F:\Projects\fitness-ecosystem-pro\web\package.json`, строки 11–13:
     `"dependencies": { "grammy": "^1.28.0" }`.
   - Полное соответствие правилу `AGENTS.md`: *«Боты: Telegram (grammY)»*.

2. **Existing Implementation**:
   - `F:\Projects\fitness-ecosystem-pro\web\src\server.js`, строки 32–93:
     ```javascript
     let tgBotInstance = null;
     let activeBotUsername = process.env.BOT_USERNAME || '';
     if (process.env.BOT_TOKEN) {
       const { Bot } = require('grammy');
       tgBotInstance = new Bot(process.env.BOT_TOKEN);
       tgBotInstance.command(['start', 'code', 'login'], async (ctx) => { ... });
       tgBotInstance.on('message:text', async (ctx) => { ... });
       tgBotInstance.start({ ... });
     }
     ```
   - Запуск осуществляется методом long polling (`tgBotInstance.start()`), исходящие соединения без выделенного HTTP-порта.
   - В `/api/login` (строки 244–262) генерируется 5-минутный OTP для пользователей с `two_factor_enabled === 1`, но отсутствует вызов `tgBotInstance.api.sendMessage` для отправки этого кода в Telegram.
   - В `/api/user/telegram/link-confirm` (строка 840) сохраняется `'tg_' + cleanUsername` вместо реального числового `telegram_id` Telegram.
   - Бот не парсит payload deep link (`/start link_<token>`).
   - Отсутствуют инлайн-кнопки для контактов владельцев (`@SantiLA213`, `@Spirit5449`).

3. **Database Schema & Columns**:
   - `F:\Projects\fitness-ecosystem-pro\web\src\db.js`, строки 94–96:
     ```javascript
     safeAddColumn('users', "telegram_id TEXT DEFAULT ''");
     safeAddColumn('users', "telegram_username TEXT DEFAULT ''");
     safeAddColumn('users', "two_factor_enabled INTEGER DEFAULT 0");
     ```
   - Методы `linkTelegram`, `unlinkTelegram`, `setTwoFactorEnabled`, `findUserByTelegramId`, `findUserByTelegramUsername` уже реализованы (строки 210–248).

4. **Owner Contacts in Ecosystem**:
   - Web Portal: `F:\Projects\fitness-ecosystem-pro\web\src\public\index.html`, строки 123–128, 372–377, 596–601 содержат рабочие ссылки `https://t.me/SantiLA213` и `https://t.me/Spirit5449`.
   - Android Athlete Pro: `F:\Projects\fitness-ecosystem-pro\athlete-app\app\src\main\java\com\athleteapp\pro\ui\screens\AthleteSettingsScreen.kt`, строки 874, 880.
   - Android Trainer Pro: `F:\Projects\fitness-ecosystem-pro\trainer-app\app\src\main\java\com\trainerapp\pro\ui\screens\SettingsScreen.kt`, строки 848, 854.

5. **Test Suite Verification**:
   - `node --test tests/pin_2fa.test.js`: 6 тестов (100% PASS, 0 fail).
   - `node --test tests/security.test.js`: 55 тестов (100% PASS, 0 fail).

---

## 2. Logic Chain

1. **Базовый фреймворк и совместимость** (на основе Observation 1):
   `grammY` уже подключен в `web/package.json`. Никаких дополнительных npm-пакетов или сторонних библиотек не требуется.
2. **Точка интеграции** (на основе Observation 2 & 3):
   Поскольку база данных SQLite работает через нативный `node:sqlite` синхронно в рамках Node.js процесса, а grammY использует асинхронный event loop, бот и веб-сервер идеально уживаются в одном процессе без оверхеда IPC или Redis.
3. **Устранение дефекта привязки аккаунтов** (на основе Observation 2):
   Привязка по `@username` ненадежна, так как у многих пользователей Telegram нет username. Решение — генерация одноразового 5-минутного `linkToken` на бэкенде и открытие deep link `https://t.me/<bot>?start=link_<token>`. Бот через grammY `ctx.match` читает `token`, берет истинный `ctx.from.id` (числовой Telegram ID) и `ctx.from.username`, сохраняет их в `db.linkTelegram(userId, telegramId, telegramUsername)` и удаляет токен.
4. **Устранение дефекта 2FA OTP доставки** (на основе Observation 2):
   В `/api/login` при `two_factor_enabled === 1` бэкенд находит `user.telegram_id` и вызывает `tgBotInstance.api.sendMessage(user.telegram_id, msg)`. Предыдущий код для данного пользователя удаляется из `telegramOtpStore`. Код действует строго 5 минут и сгорает при вводе или 5 неудачных попытках.
5. **Внедрение кнопок владельцев в бот** (на основе Observation 4):
   В grammY через `new InlineKeyboard().url('💬 @SantiLA213', 'https://t.me/SantiLA213').url('💬 @Spirit5449', 'https://t.me/Spirit5449')` кнопки добавляются в приветственное сообщение `/start`, команду `/contacts` и подтверждение привязки аккаунта.
6. **Выделение процесса и модульность**:
   Вынос логики бота из `server.js` в `web/src/bot.js` сделает архитектуру чистой, позволит запускать бота как совместно с веб-сервером (по умолчанию), так и изолированно при необходимости.

---

## 3. Caveats

1. **Внешний токен Telegram**: В тестовом окружении без переменной окружения `BOT_TOKEN` бот не может подключиться к серверам Telegram. Для этого в кодовой базе сохранен режим эмуляции (`debugCode`), позволяющий проходить юнит-тесты без внешнего интернета.
2. **Лимиты Telegram API**: Telegram Bot API имеет ограничение до 30 сообщений в секунду суммарно и до 1 сообщения в секунду одному пользователю. Для масштабирования в highload рекомендуется плагин `@grammyjs/transformer-throttler`, однако для текущей фитнес-экосистемы встроенных возможностей grammY более чем достаточно.
3. **Хранение в памяти vs SQLite**: При внезапном падении сервера текущий `Map()` сбрасывается. Рекомендуется персистить `telegram_link_tokens` в SQLite.

---

## 4. Conclusion

Архитектура Telegram-бота в проекте `fitness-ecosystem-pro` полностью спроектирована и детализирована в отчете `F:\Projects\fitness-ecosystem-pro\.agents\teamwork\survey_explorer_2\analysis.md`.
- **Фреймворк**: grammY 1.28.0 (согласно правилам `AGENTS.md`).
- **Схема работы**: Unified Host (единый процесс Node.js с модулем `web/src/bot.js`, long polling, без конфликта портов).
- **Привязка аккаунтов**: Deep link `t.me/<bot>?start=link_<token>` + резервная команда `/link <token>` с привязкой числового `telegram_id`.
- **2FA**: Строгий 5-минутный 6-значный OTP, немедленная инвалидация старых кодов, прямая доставка через `bot.api.sendMessage`, лимит 5 попыток.
- **Контакты**: Инлайн-кнопки `@SantiLA213` и `@Spirit5449` в командах `/start`, `/contacts`, `/help` и меню бота.

---

## 5. Verification Method

1. **Проверка существующих тестов**:
   ```powershell
   cd F:\Projects\fitness-ecosystem-pro\web
   node --test tests/pin_2fa.test.js
   node --test tests/security.test.js
   ```
   *Критерий успеха*: Все 6 тестов `pin_2fa.test.js` и 55 тестов `security.test.js` завершаются со статусом 100% PASS.

2. **Проверка схемы БД и структуры файлов**:
   - `F:\Projects\fitness-ecosystem-pro\web\src\db.js` (проверить наличие полей `telegram_id`, `telegram_username`, `two_factor_enabled`).
   - `F:\Projects\fitness-ecosystem-pro\web\package.json` (проверить зависимость `"grammy": "^1.28.0"`).
   - `F:\Projects\fitness-ecosystem-pro\.agents\teamwork\survey_explorer_2\analysis.md` (ознакомиться с полным отчетом).

3. **Условия признания недействительным (Invalidation conditions)**:
   - Изменение основного стека бота с grammY на иной фреймворк.
   - Изменение времени жизни кодов (TTL) с 5 минут на другое значение.
