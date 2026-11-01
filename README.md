# Fitness Ecosystem Pro

Единый монорепозиторий экосистемы Fitness Pro: веб-портал (PWA), Telegram-бот авторизации и мобильные приложения (Android).

## 📁 Структура проекта
- `web/` — Веб-приложение (PWA) и Telegram-бот на Node.js (grammY + SQLite).
  - Личные кабинеты атлета и тренера, графики тренировок, замеры тела.
  - Двухфакторная авторизация (2FA) по одноразовым OTP-кодам в Telegram.
  - Динамическая ротация PIN-кодов (5 минут) связки тренера и атлета.
  - Полный OWASP ASVS Level 2 тестовый набор (`tests/`).
- `athlete-app/` — Исходный код мобильного приложения атлета (Android).
- `trainer-app/` — Исходный код мобильного приложения тренера (Android).
- `releases/` — Релизные сборки и архивные APK.
- `render.yaml` — Конфигурация для мгновенного развертывания веб-сервиса на Render.com (rootDir: `web`).

## ⚙️ Развертывание на Render.com
1. Создайте Web Service на [Render.com](https://render.com).
2. Подключите данный репозиторий `kotpers67-spec/fitness-ecosystem-pro`.
3. Укажите параметры (или примените `render.yaml`):
   - **Root Directory**: `web`
   - **Environment**: `Node`
   - **Build Command**: `npm install`
   - **Start Command**: `npm start`
4. Переменные окружения (Environment Variables):
   - `BOT_TOKEN`: токен Telegram-бота от `@BotFather`
   - `PORT`: `10000`

## 🛠️ Локальный запуск веб-портала
```bash
cd web
npm install
npm start
```
Сервер будет доступен по адресу `http://localhost:3000`.

## 🧪 Тестирование безопасности
```bash
cd web
npm test
```
