# Архитектурный отчет обследования веб-портала и бэкенд-сервера
**Дата обследования:** 2026-10-04  
**Исследователь:** Survey Explorer 1  
**Область:** Web Portal & Backend Architecture (`F:\Projects\fitness-ecosystem-pro\web`)  
**Статус:** Исследование завершено, 100% верифицировано тестами (61/61 PASS, 0 fail).

---

## 1. Архитектура веб-сервера, маршрутизация и базы данных

### 1.1 Точка входа и рантайм
- **Файл точки входа:** `F:\Projects\fitness-ecosystem-pro\web\src\server.js` (строки 25–93, 1257–1264).
- **Стек:** Native Node.js 24 (`node:http`, `node:crypto`, `node:sqlite`). Внешние зависимости минимизированы (`grammy` для Telegram-бота).
- **Запуск:**
  - `start.bat` и `start.ps1` в папке `web/`.
  - Порт по умолчанию: `3000` (переменная окружения `PORT`).
  - Экспортирует `{ server, db, authLimiter }` для прямого подключения в тестах (`server.listen(0)`).

### 1.2 Маршрутизация и диспетчеризация
1. **API Маршруты (`pathname.startsWith('/api/')`):**
   - Строка 94 `server.js`.
   - Защита от Brute-Force через скользящий RateLimiter: `/api/login`, `/api/register`, `/api/auth/telegram` (макс 15 попыток/мин на IP).
   - Авторизация через заголовок `Authorization: Bearer <token>`.
2. **Статические файлы и SPA Fallback:**
   - Строки 1193–1255 `server.js`.
   - Защита от Path Traversal: блокировка `..`, `\0`, прямого доступа к файлам `.sqlite` и `.db` (HTTP 403).
   - Раздача статики из `web/src/public/` (`index.html`, `styles.css`, `app.js`, `qr.js`).
   - SPA Fallback: все браузерные маршруты перенаправляются на `index.html`.

### 1.3 Схема базы данных SQLite (`web/src/db.js`)
Используется `DatabaseSync` из `node:sqlite` с WAL-режимом (`PRAGMA journal_mode = WAL; PRAGMA busy_timeout = 5000;`).

#### Таблицы и поля:
1. `users` (строки 20–34, 89–97):
   - `id` (INTEGER PRIMARY KEY)
   - `username` (TEXT UNIQUE)
   - `password_hash` (TEXT, scrypt)
   - `role` (TEXT CHECK ('athlete', 'trainer'))
   - `full_name` (TEXT)
   - `phone` (TEXT)
   - `avatar_base64` (TEXT)
   - `client_uuid` (TEXT) — UUID для сквозной синхронизации с Android (Athlete Pro)
   - `coach_name`, `coach_phone` (TEXT)
   - `pairing_code` (TEXT) — 6-значный цифровой код подопечного
   - `pairing_code_created_at` (INTEGER) — таймстемп создания PIN (для 5-минутного TTL)
   - `is_private` (INTEGER) — тумблер приватности в состязаниях
   - `telegram_id` (TEXT) — идентификатор пользователя в Telegram
   - `telegram_username` (TEXT) — никнейм без `@`
   - `two_factor_enabled` (INTEGER) — флаг 2FA (1 = включено, 0 = выключено)
   - `created_at` (DATETIME)
2. `trainer_clients` (строки 36–44):
   - `id`, `trainer_id`, `athlete_id`, `paired_at` (UNIQUE(trainer_id, athlete_id)).
3. `workout_sessions` (строки 46–57):
   - `id`, `athlete_id`, `date`, `notes`, `completed`, `is_self_workout_allowed`, `assigned_by_trainer_id`.
4. `workout_sets` (строки 59–74):
   - `id`, `session_id`, `exercise_name`, `weight_kg`, `reps`, `rpe`, `is_completed`, `target_weight_kg`, `target_reps`.
5. `auth_tokens` (строки 76–81):
   - `token` (TEXT PRIMARY KEY, 32 байта hex), `user_id`, `expires_at` (TTL 7 дней).

---

## 2. Логика 5-минутного динамического PIN привязки (R1)

### 2.1 Генерация и хранение
- **Генерация:** строго 6 случайных цифр (`String(Math.floor(100000 + Math.random() * 900000))`).
- При регистрации атлета (`server.js:136, 199`) и при запросе перегенерации (`server.js:691`).
- Таймстемп создания: `db.createUser` и `db.regeneratePairingCode` фиксируют `pairing_code_created_at = Date.now()` (`db.js:113, 196`).
- Облачная регистрация: через `cloudSyncService.registerAthletePairing(pin, clientUuid, ...)` передается в Google Apps Script облако для сквозного взаимодействия с Android Trainer Pro.

### 2.2 Валидация и проверка 5-минутного срока действия
- Эндпоинт привязки: `POST /api/trainer/pair` (`server.js:811–870`).
- Ввод очищается от любых нецифровых символов: `rawCode = String(body.code || '').replace(/\D/g, '')`.
- Валидация по регулярному выражению `/^\d{6}$/`.
- **Строгая проверка TTL (5 минут):**
  ```javascript
  const PAIRING_TTL = 5 * 60 * 1000;
  if (athlete.pairing_code_created_at && (Date.now() - athlete.pairing_code_created_at > PAIRING_TTL)) {
    return sendError(res, 400, 'Срок действия кода истёк (действует 5 минут). Запросите у подопечного новый код.');
  }
  ```
- В облачном сервисе `cloudSync.js:188–194` также выполняется проверка: если `now - pairingEntry.timestamp > 300000`, запись удаляется и возвращается ошибка 400 `'EXPIRED'`.

### 2.3 Одноразовое использование (Strict Single-Use Consumption)
- После успешной привязки тренера к атлету вызывается:
  ```javascript
  db.consumePairingCode(athlete.id);
  ```
- Метод `db.consumePairingCode` (`db.js:203–207`) сбрасывает `pairing_code = ''` и `pairing_code_created_at = 0`.
- Повторный ввод этого же кода немедленно возвращает 404/400.

### 2.4 Клиентский UI: таймер обратного отсчета и автообновление
- **Отображение:** в профиле атлета (`index.html:290–297`):
  - Крупный 6-значный код: `#athlete-pairing-pin` (с `tabular-nums`).
  - Бейдж таймера: `#pin-timer-badge` с `#pin-countdown-text` (формат `05:00`).
- **Скрипт таймера (`app.js:767–795`):**
  - Функция `startPinCountdown()` вычисляет `remaining = Math.max(0, 300 - elapsed)`.
  - Каждую секунду обновляет текст `MM:SS`.
  - При `remaining <= 0` автоматически вызывает `autoRegenerateAthletePin()` (`app.js:797–807`).
  - Метод отправляет `POST /api/athlete/regenerate-pin`, получает свежий 6-значный PIN, обновляет QR-код и сбрасывает таймер на `05:00`.
- **QR-код:** отображается чистый векторный SVG через `qr.js` (без ссылок).

---

## 3. Аутентификация, 2FA через Telegram и сессии (R2)

### 3.1 Модель паролей и токенов
- Хеширование паролей: `crypto.scryptSync(password, salt, 64)` с криптостойкой солью (`security.js:44–57`).
- Проверка через `crypto.timingSafeEqual` против Timing-атак.
- Сессии: токены 64-символьного hex (`crypto.randomBytes(32)`), сохраняются в таблице `auth_tokens`.

### 3.2 Точки входа и механизм 2FA
1. **Тумблер 2FA в настройках профиля:**
   - Эндпоинт: `POST /api/user/2fa` (`server.js:778–792`).
   - Перед включением проверяет наличие привязанного Telegram (`telegram_id` или `telegram_username`). Если не привязан — возвращает HTTP 400 (`Сначала привяжите Telegram аккаунт для включения 2FA`).
   - Сохраняет флаг `two_factor_enabled` в SQLite.
2. **Перехват при входе (`POST /api/login`):**
   - Строки 180–198 `server.js`:
   - Если `user.two_factor_enabled === 1`:
     - Генерируется 6-значный OTP (`String(Math.floor(100000 + Math.random() * 900000))`).
     - Записывается в `telegramOtpStore.set('2fa_' + user.id, { userId, code, expiresAt, attempts: 0 })` со сроком 5 минут.
     - Если бот активен и известен `chatId` пользователя — отправляется сообщение через Telegram API.
     - Ответ клиенту: `{ success: true, require2FA: true, userId: user.id, expiresInSeconds: 300 }`.
3. **Подтверждение 2FA (`POST /api/login/2fa`):**
   - Строки 227–287 `server.js`.
   - Проверяет срок действия (5 минут), лимит попыток (макс. 5), совпадение кода.
   - При успехе код удаляется из хранилища (`telegramOtpStore.delete`), выдается токен авторизации.

### 3.3 Привязка существующего Telegram к аккаунту
- Кнопка «Привязать Telegram» в профиле атлета (`#btn-link-tg-athlete`) и тренера (`#btn-link-tg-trainer`).
- Диалог `#dialog-link-telegram` (`index.html:703–735`, `app.js:1237–1320`):
  1. Шаг 1: ввод `@username` -> запрос на `POST /api/user/telegram/link-request`.
  2. В Telegram отправляется 6-значный проверочный код.
  3. Шаг 2: ввод кода с живым таймером 5 минут -> подтверждение через `POST /api/user/telegram/link-confirm`.
  4. Сохраняются `telegram_id` и `telegram_username` в базе данных.

### 3.4 Встроенный Telegram-бот (grammY)
- В `server.js:32–93`:
  - При наличии `process.env.BOT_TOKEN` инициализируется `Bot` из библиотеки `grammy`.
  - Обработчики команд: `/start`, `/code`, `/login`.
  - Бот автоматически генерирует и высылает 6-значный одноразовый код на 5 минут.

---

## 4. Контакты владельцев проекта (@SantiLA213, @Spirit5449) (R3)

Прямые кнопки связи интегрированы во всех ключевых экранах веб-портала:
1. **Экран входа / регистрации (`index.html:120–130`):**
   - Блок `.auth-owners-box` с кнопками:
     - `https://t.me/SantiLA213` (`💬 @SantiLA213`)
     - `https://t.me/Spirit5449` (`💬 @Spirit5449`)
2. **Профиль атлета (`index.html:368–379`):**
   - Карточка «СВЯЗЬ С ВЛАДЕЛЬЦАМИ» с прямыми ссылками на обоих создателей.
3. **Настройки тренера (`index.html:592–603`):**
   - Карточка «СВЯЗЬ С ВЛАДЕЛЬЦАМИ» с прямыми ссылками.
4. **Стилистика:**
   - Компонент `.owner-chip` в `styles.css:1275–1295`: темный фон `#161616`, акцентная подсветка `#29b6f6` при наведении, адаптивная верстка по стандарту Anti-Overlap.

---

## 5. Тестовый набор и верификация (R1, R2, R3)

### 5.1 Текущее состояние тестов
В проекте реализованы 2 ключевых тестовых набора:
1. `web/tests/security.test.js` (735 строк, 55 тестов):
   - Разделы: Token Security, Anti-SQL Injection, Anti-XSS, Rate Limiting, Role Isolation (RBAC & IDOR), Path Traversal, Telegram Auth Security.
2. `web/tests/pin_2fa.test.js` (193 строки, 6 тестов):
   - Тест 1: Проверка наличия и корректности `pairing_code_created_at`.
   - Тест 2: Отклонение просроченного PIN (>5 минут) с HTTP 400.
   - Тест 3: Перегенерация PIN с новым таймером 300 секунд.
   - Тест 4: Успешная привязка и сгорание одноразового кода (защита от повторного использования).
   - Тест 5: Привязка Telegram через OTP и сохранение username в базе данных.
   - Тест 6: Включение 2FA, требование OTP при входе и вход по 6-значному коду.

### 5.2 Результаты запуска тестов
- Команда запуска: `node --test tests/security.test.js tests/pin_2fa.test.js`
- **Результат:**
  - Тестов запущено: **61**
  - Пройдено (PASS): **61 (100%)**
  - Ошибок (FAIL): **0**
  - Время выполнения: ~114 с (с учетом сетевых таймаутов обращений к Google Apps Script).

---

## 6. Рекомендации для разработчиков

1. **Telegram Bot Welcome Message:**
   - В `server.js:57–65` добавить в приветственное сообщение бота inline-кнопки или текст со ссылками на владельцев: `https://t.me/SantiLA213` и `https://t.me/Spirit5449` для 100% покрытия требований R3 в боте.
2. **Скрипт запуска тестов в `package.json`:**
   - Обновить секцию `scripts.test` в `web/package.json`:
     ```json
     "test": "node --test tests/security.test.js tests/pin_2fa.test.js"
     ```
3. **Персистентность OTP при перезапуске сервера (опционально):**
   - На текущий момент `telegramOtpStore` размещен в памяти `Map`. Это обеспечивает скорость и отсутствие нагрузки на SSD, но при рестарте сервера активные 5-минутные коды сбрасываются (пользователь просто запрашивает новый). При необходимости можно добавить таблицу `otp_verifications` в SQLite.
