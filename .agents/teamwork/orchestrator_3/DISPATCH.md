## 2026-10-04T07:32:46Z
You are the Project Orchestrator for the fitness ecosystem project.

## Your Identity and Working Directory
- Identity: Project Orchestrator (orchestrator_3)
- Working Directory: F:\Projects\fitness-ecosystem-pro\.agents\teamwork\orchestrator_3\
- Project Root: F:\Projects\fitness-ecosystem-pro
- Task Reference: Read F:\Projects\fitness-ecosystem-pro\.agents\teamwork\ORIGINAL_REQUEST.md (specifically Follow-up — 2026-10-04T07:30:38Z).

## Requirements to Implement and Verify

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
- [ ] При попытке тренера ввести истекший (>5 мин) или уже использованный код возвращается ошибка 400.
- [ ] Привязка Telegram к существующему аккаунту сохраняет telegram_id и telegram_username в базе данных.
- [ ] Вход с включенным 2FA блокируется без подтверждения актуальным 6-значным OTP из Telegram.
- [ ] Таймер 5 минут отображается и обновляет код привязки.
- [ ] Кнопки связи с владельцами корректно открывают @SantiLA213 и @Spirit5449.

## Execution Rules
- Zero-Mocks: 100% реальный, рабочий и протестированный функционал.
- Decompose and orchestrate via specialists (web-dev, winui/android specialists, reviewer, etc.) under .agents/teamwork/.
- Maintain progress.md and BRIEFING.md regularly in your working directory.
- Verify all implementations with automated tests and builds.
- When finished, compile full results and handoff report handoff.md, then notify Sentinel.
