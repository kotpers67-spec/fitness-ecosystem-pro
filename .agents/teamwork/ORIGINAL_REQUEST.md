# Original User Request

## Initial Request — 2026-10-03T17:27:20Z

Выполнить полный сквозной аудит кода экосистемы фитнес-приложений (Trainer Pro и Athlete Pro), устранить все найденные дефекты, провести строгую верификацию по стандартам Zero-Mocks на эмуляторе Pixel 8 и выпустить официальный релиз v1.0.5 на GitHub и в облако.

Working directory: F:\Projects\fitness-ecosystem-pro
Integrity mode: development

## Requirements

### R1. Полный сквозной аудит функционала и кода
- Проверить целостность и отказоустойчивость:
  1. Синхронизация фото/аватарок (Base64 кодирование/декодирование, кэширование, обработка пустых значений).
  2. Карточка тренера: сохранение в Trainer Pro Settings, передача при синхронизации и отображение в Athlete Pro с кнопкой вызова и отвязкой.
  3. Раздел «Состязания»: корректный подсчет очков за количество тренировок и тоннаж, лидерборд, фильтрация по тумблеру приватности.
  4. Просмотр статистики предыдущей тренировки при добавлении упражнения в Trainer Pro.
  5. Фоновая автоустановка обновлений и проверка версий в обоих приложениях.
  6. Отсутствие утечек памяти, соблюдение Anti-Overlap Guard и корректная обработка отсутствия интернет-соединения.

### R2. Исправление дефектов и бамп версии до v1.0.5
- Устранить все обнаруженные ошибки, предупреждения компилятора и линтера.
- В `build.gradle.kts` обоих приложений повысить версию: `versionCode = 5`, `versionName = "1.0.5"`.
- Обновить версию в UI и в облачном манифесте обновлений.

### R3. Строгая верификация Zero-Mocks на эмуляторе
- Запустить unit-тесты Gradle (`./gradlew testDebugUnitTest`) для обоих проектов.
- Установить свежие APK на эмулятор Pixel 8 API 36 (`emulator-5554`).
- Выполнить сквозной прогон сценариев с сохранением подтверждающих скриншотов всех экранов.

### R4. Деплой релиза v1.0.5
- Собрать релизные APK: `releases/trainer-pro-v1.0.5.apk` и `releases/athlete-pro-v1.0.5.apk`.
- Создать GitHub Release `v1.0.5` с загрузкой бинарников через `gh release create`.
- Обновить зашифрованный узел `updates` в Google Apps Script облаке для доставки автообновления по воздуху.

## Acceptance Criteria

### Качество кода и функциональность
- [ ] Все 5 ключевых функций проверены и работают без багов и крашей.
- [ ] Данные передаются в зашифрованном виде AES-256 (`ENC:`).
- [ ] В UI отсутствуют любые артефакты верстки и нежелательные упоминания провайдеров.

### Верификация и Сборка
- [ ] Все юнит-тесты обоих проектов успешно проходят.
- [ ] Запуск на эмуляторе подтвержден скриншотами без системных ошибок.

### Релиз
- [ ] GitHub Release v1.0.5 создан и содержит оба APK.
- [ ] Облачный манифест v1.0.5 обновлен.


## Follow-up — 2026-10-03T18:00:17Z

ВНИМАНИЕ ОТ ПОЛЬЗОВАТЕЛЯ (Критическое требование к релизу v1.0.5):
"только реальные данные в приложение и состязания всех тестовых убери из релизной версии"

1. Полностью удалить всех тестовых/заглушечных участников в LeaderboardScreen.kt ("Максим Громов", "Елена Соколова", "Дмитрий Воронов", "Ольга Морозова"). В состязаниях должны быть ТОЛЬКО реальные пользователи (текущий атлет по его реальным выполненным сессиям и тоннажу, а также реальные участники из облака, если они есть).
2. Если других участников пока нет — показывать только реального пользователя, либо красивое пустое состояние (Empty State), если включена приватность.
3. Убедиться, что в базах данных (Room), сидах и дефолтных значениях нет тестовых пользователей-заглушек. Все данные — только реальные (Zero-Mocks).
4. Пересобрать APK v1.0.5, проверить на эмуляторе и выпустить релиз.


## Follow-up — 2026-10-03T18:01:19Z

ДОПОЛНИТЕЛЬНОЕ УКАЗАНИЕ ПОЛЬЗОВАТЕЛЯ:
"и кюар код не работает раз у тебя не получается можешь зделать место него сылку и сделай чтобы тире не нада было вводить при вводе кода"

1. В AthleteSettingsScreen.kt:
- Заменить неработающий QR-код на ссылку и кнопки: «Скопировать ссылку для тренера» (копирует https://fitnessapp.pro/pair?code=$cleanPin в буфер с Toast) и «Отправить тренеру» (Android Share Intent с текстом и ссылкой).
- Убрать компонент QrCodeView, чтобы не загромождать интерфейс.

2. При вводе кода привязки в Trainer Pro (HomeScreen.kt и GoogleDriveSyncManager.kt):
- Сделать так, чтобы ТИРЕ НЕ НАДО БЫЛО ВВОДИТЬ!
- Тренер вводит просто 6 цифр подряд (например, 265507), без дефиса.
- В парсере findAndPairAthlete и поле ввода: автоматическая очистка от любых нецифровых знаков (`filter { it.isDigit() }`), авто-извлечение 6-значного кода из ссылок/текста/JSON.
- Обновить плейсхолдер: "739102 (без тире)".

Внедри эти изменения в код, пересобери APK v1.0.5 и протестируй.


## Follow-up — 2026-10-03T19:07:57Z

Экосистема фитнес-приложений (Trainer Pro, Athlete Pro) и локальный веб-портал: устранение крашей при запуске и сканировании на смартфонах, оптимизация кэша/аватарок (защита от переполнения CursorWindow), реализация локального защищенного веб-портала и полный комплект тестов безопасности.

Working directory: F:\Projects\fitness-ecosystem-pro
Integrity mode: development

## Requirements

### R1. Устранение крашей мобильных приложений (Trainer Pro & Athlete Pro)
1. **Камера и QR-сканер**: Добавить обязательный рантайм-запрос разрешения `CAMERA` перед вызовом системной камеры в диалоге привязки Trainer Pro. В `QrCodeScannerHelper` перехватывать `Throwable` (включая `OutOfMemoryError`) и масштабировать входящие изображения.
2. **Защита от сбоев кэша и базы данных (CursorWindow Fix)**: Сжатие аватарок до компактных миниатюр (максимум 128x128, JPEG 75%, размер < 15 КБ), безопасное чтение из Room без риска `SQLiteBlobTooBigException`. 
3. **Безопасная инициализация ViewModel**: Устранить блокирующие ожидания Flow (`clients.filter { it.isNotEmpty() }.first()`) в `MainViewModel`, гарантировать корректный запуск даже при чистой базе данных без клиентов.

### R2. Локальный веб-портал (Trainer & Athlete Web)
1. **Фронтенд SPA**: Полноценный интерфейс для тренера и атлета в темной теме (`#0d0d0d`, швейцарская типографика, адаптивность для ПК и мобильных, Anti-Overlap Guard).
2. **Функционал**: Регистрация/вход, переключение ролей, просмотр и отметка подходов, отображение чистого QR-кода и 6-значного цифрового PIN подопечного, мгновенная привязка тренером по 6 цифрам, лидерборд на 100% реальных данных (Zero-Mocks).
3. **Локальный режим**: Запуск на `http://localhost:3000` без внешней выгрузки/деплоя в интернет.

### R3. Комплекс тестов безопасности (Security Test Suite)
1. Тестирование защиты от SQL Injection: атаки на все эндпоинты через логин, пароль, имя, названия упражнений и параметры запросов.
2. Тестирование защиты от XSS: экранирование HTML-тегов и скриптов в полях ввода.
3. Тестирование Rate Limiting: блокировка перебора паролей (Brute-force) на `/api/login` и `/api/register`.
4. Тестирование изоляции доступа: атлеты не имеют доступа к эндпоинтам тренера, проверка валидности токенов.

## Acceptance Criteria

### Мобильная стабильность
- [ ] Приложение Trainer Pro запрашивает разрешение на камеру перед сканированием QR и не падает ни при каких условиях.
- [ ] Размер сохраняемых аватарок ограничен, исключая переполнение CursorWindow Room SQLite.
- [ ] Обе сборки (Athlete Pro и Trainer Pro) успешно собираются (`assembleRelease`) и запускаются с чистым логом.

### Веб-портал и безопасность
- [ ] Локальный веб-сервер запускается на порту 3000 и корректно отдает SPA и API.
- [ ] Все тесты безопасности в `web/tests/security.test.js` завершаются с результатом 100% PASS (0 уязвимостей).
- [ ] В лидерборде и состязаниях используются строго реальные данные пользователей.


## Follow-up — 2026-10-04T07:30:38Z

Экосистема фитнес-приложений (Trainer Pro, Athlete Pro, Web Portal, Telegram Bot): 5-минутные динамические коды со строгой инвалидацией, 2FA аутентификация через Telegram-бота, привязка Telegram к аккаунтам, контакты владельцев проекта.

Working directory: F:\Projects\fitness-ecosystem-pro
Integrity mode: development

## Requirements

### R1. 5-минутный динамический PIN привязки атлета к тренеру
1. PIN атлета (6 цифр) строго меняется каждые 5 минут и сбрасывается при использовании (одноразовый).
2. Тренер не может привязать подопечного по старому, сменившемуся или истекшему коду (строгая ошибка 400).
3. В профиле атлета (Web и Android) отображается живой таймер обратного отсчета (05:00) с автоматической перегенерацией кода при истечении.

### R2. Telegram-бот как полноценный ключ и 2FA аутентификатор
1. Привязка существующих аккаунтов: кнопка «Привязать Telegram» в профиле (Web/Android) открывает бота с командой привязки (логин/пароль или токен).
2. Двухфакторная аутентификация (2FA): переключатель в настройках профиля. При включенном 2FA любой вход требует 6-значный код из Telegram бота.
3. Коды авторизации меняются каждые 5 минут; все старые коды мгновенно становятся недействительными.

### R3. Связь с владельцами проекта (@SantiLA213, @Spirit5449)
1. Прямые кнопки связи с владельцами проекта (Telegram-ссылки https://t.me/SantiLA213 и https://t.me/Spirit5449) в интерфейсе (экран входа, профиль атлета/тренера) и в боте.

## Acceptance Criteria

### Динамические коды и 2FA
- [ ] При попытке тренера ввести истекший (>5 мин) или уже использованный код возвращается ошибка 400.
- [ ] Привязка Telegram к существующему аккаунту сохраняет telegram_id и telegram_username в базе данных.
- [ ] Вход с включенным 2FA блокируется без подтверждения актуальным 6-значным OTP из Telegram.

### Интерфейс и контакты
- [ ] Таймер 5 минут отображается и обновляет код привязки.
- [ ] Кнопки связи с владельцами корректно открывают @SantiLA213 и @Spirit5449.


## 2026-10-04T15:46:58Z

# Teamwork Project Prompt — Full Ecosystem Audit

Комплексный полный аудит всей экосистемы Fitness Ecosystem Pro: мобильные приложения на Android (`F:\Projects\fitness-ecosystem-pro\athlete-app`, `F:\Projects\fitness-ecosystem-pro\trainer-app`), веб-портал и Telegram-бот (`F:\Projects\fitness-ecosystem-pro\web`).

Working directory: `F:\Projects\fitness-ecosystem-pro`
Integrity mode: development

## Requirements

### R1. Мобильное приложение Атлета (Athlete Pro Android App)
- Проверить авторизацию по Telegram (мгновенный вход в 1 клик и 6-значный OTP код).
- Проверить блокировку входа в 1 клик при включённой 2FA (требование 6-значного кода).
- Проверить дневник тренировок: переименование "Подходы", удаление подходов, самостоятельное добавление упражнений (`+ Добавить упражнение`).
- Проверить сохранность фото профиля при перезаходе (Base64 на диск и Room).
- Проверить график прогресса: отображение веса/даты на точках и детализация по клику.
- Проверить pull-to-refresh синхронизацию в профиле.
- Проверить таблицу состязаний (отсутствие колонок "число" и "тоннаж").

### R2. Мобильное приложение Тренера (Trainer Pro Android App)
- Проверить авторизацию и привязку Telegram, сохранение фото профиля тренера.
- Проверить блокировку входа в 1 клик при включённой 2FA.
- Проверить назначение тренировок, терминологию подходов, pull-to-refresh синхронизацию.
- Проверить сборку APK и отсутствие устаревших заглушек.

### R3. Веб-портал и Безопасность (Web Portal & RBAC)
- Проверить все эндпоинты API на IDOR, SQL-инъекции, XSS и Rate Limiting.
- Проверить выбор роли при входе/регистрации на сайте.
- Проверить таблицу состязаний и интерактивный график тренировок с кликом по точкам.
- Проверить актуальность ссылок для скачивания APK тренера и атлета.

### R4. Telegram-бот (Telegram Bot Integration)
- Проверить главное меню: подтвердить удаление кнопки "Сменить роль".
- Проверить генерацию единого 6-значного 5-минутного кода (`🔑 Код входа`).
- Проверить 1-клик авторизацию и работу 2FA OTP.
- Проверить привязку аккаунтов (`/link`).

## Acceptance Criteria

### Mobile Verification
- [ ] `gradlew testDebugUnitTest` в `athlete-app` проходит со 100% успехом
- [ ] `gradlew testDebugUnitTest` в `trainer-app` проходит со 100% успехом
- [ ] `assembleRelease` успешно собирает оба APK без ошибок

### Web & Bot Verification
- [ ] `npm test` в `web` проходит со 100% успехом (все сьюты безопасности и 2FA)
- [ ] В меню Telegram-бота отсутствует кнопка "🔄 Сменить роль"
- [ ] Ссылки на сайте отдают свежие собранные APK


## 2026-10-04T20:25:27Z

Fix Telegram authentication, session persistence, pairing PIN code synchronization, and background APK updates across Web, Mobile (Athlete & Trainer Android apps), and Telegram bot in Fitness Ecosystem Pro.

Working directory: F:\Projects\fitness-ecosystem-pro
Integrity mode: development

## Requirements

### R1. Stateless Session Persistence & Direct PIN Auth (Web/Backend)
- Session tokens must persist across Render server restarts/cold boots using signed HMAC-SHA256 tokens with automatic re-hydration in database.
- `/api/auth/telegram/verify-otp` must accept verification by code alone (`{ code: "123456" }`) without requiring username entry, matching active OTPs and paired user PINs.

### R2. Mobile Auth Parity & 1-Click Telegram Login (Android Apps)
- Mobile auth screens (`AthleteAuthScreen.kt`, `TrainerAuthScreen.kt`) must support 1-step 6-digit code entry dialog without asking for username.
- 1-click Telegram button must trigger `tg://resolve?domain=fitnessecosystemBOT&start=auth_<sessionId>` with fallback and poll session token.
- Both mobile apps must synchronize the exact 6-digit pairing PIN, photo, full name, phone, and restrictions with the website and Telegram bot for the same account.

### R3. In-App Direct APK Background Updates & Build Verification
- APK update download must run directly in the background via download manager/service without opening or redirecting to external browser pages.
- Automated tests (`npm test` in `web/`) must pass 100%.
- Release APKs for Athlete and Trainer apps must build successfully (`gradlew.bat assembleRelease`) and be deployed to `releases/` and `web/releases/`.

## Acceptance Criteria

### Automated Verification
- [ ] `npm test` runs with 0 failures in `web/`.
- [ ] `./gradlew.bat assembleRelease` passes for `athlete-app` and `trainer-app`.
- [ ] `web/releases/athlete-latest.apk` and `web/releases/trainer-latest.apk` are generated and up-to-date.

### Functional Guardrails
- [ ] 6-digit PIN login works without providing a username.
- [ ] Refreshing or restarting server does not invalidate active user sessions.
- [ ] Pairing PIN displayed in mobile app matches web profile and bot for the same user.
- [ ] Background APK updater downloads file directly without browser redirect.


## 2026-10-05T04:06:54Z

Comprehensive verification, release validation, and forensic audit of Telegram authentication, persistent sessions, exercise catalog search, sorting and auto-collapse, and background APK updates across Web, Mobile (Athlete & Trainer Android apps), and Telegram bot in Fitness Ecosystem Pro.

Working directory: F:\Projects\fitness-ecosystem-pro
Integrity mode: development

## Requirements

### R1. Stateless Session Persistence & Direct PIN Auth (Web/Backend)
- Session tokens must persist across Render server restarts/cold boots using signed HMAC-SHA256 tokens with automatic re-hydration in database.
- `/api/auth/telegram/verify-otp` must accept verification by code alone (`{ code: "123456" }`) without requiring username entry.

### R2. Exercise Catalog, Sorting & Auto-Collapse (Web & Android)
- Trainer exercise addition must feature a searchable list/catalog (35+ exercises by muscle group) without carousel limitations or daily exercise caps.
- Completed exercises (where all sets are checked) must sort to the bottom of the list with pending exercises at the top.
- Completed exercises must auto-collapse 1 hour after the first exercise of the session was created, with a manual expand/collapse toggle.

### R3. Mobile Auth Parity & In-App APK Updates (Android Apps)
- Mobile auth screens (`AthleteAuthScreen.kt`, `TrainerAuthScreen.kt`) must support 1-step 6-digit code entry dialog without asking for username.
- 1-click Telegram button must trigger `tg://resolve?domain=fitnessecosystemBOT&start=auth_<sessionId>` with fallback and poll session token.
- Background APK updater must download APK directly via download service without browser redirects.

## Acceptance Criteria

### Automated Verification
- [ ] `npm test` runs with 0 failures in `web/` (all 57+ security and sync tests pass).
- [ ] `./gradlew.bat test` passes with 0 failures for `athlete-app` and `trainer-app`.
- [ ] `./gradlew.bat assembleRelease` passes for `athlete-app` and `trainer-app`.
- [ ] `releases/athlete-latest.apk` and `releases/trainer-latest.apk` are generated and up-to-date.

### Functional Guardrails
- [ ] 6-digit PIN login works without providing a username.
- [ ] Refreshing or restarting server does not invalidate active user sessions.
- [ ] Pairing PIN displayed in mobile app matches web profile and bot for the same user.
- [ ] Searchable exercise catalog filters in real-time.
- [ ] Completed exercises sort to bottom and auto-collapse after 1 hour.
- [ ] Background APK updater downloads file directly without browser redirect.
