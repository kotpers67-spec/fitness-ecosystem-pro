const test = require('node:test');
const assert = require('node:assert');
const { generateSecurePin, RateLimiter } = require('../src/security');
const AppDatabase = require('../src/db');
const { setupBotHandlers } = require('../src/bot');

test('M1 Hardening Test Suite', async (t) => {
  await t.test('1. generateSecurePin produces 6-digit cryptographic PINs', () => {
    for (let i = 0; i < 500; i++) {
      const pin = generateSecurePin();
      assert.strictEqual(typeof pin, 'string');
      assert.strictEqual(pin.length, 6);
      const num = parseInt(pin, 10);
      assert.ok(num >= 100000 && num <= 999999, `PIN ${pin} must be in [100000, 999999]`);
    }
  });

  await t.test('2. RateLimiter.cleanup() purges expired IP records from memory', () => {
    const limiter = new RateLimiter(50, 5); // 50ms window
    limiter.isRateLimited('1.1.1.1');
    limiter.isRateLimited('2.2.2.2');
    assert.strictEqual(limiter.requests.size, 2);

    // After waiting > 60ms, cleanup should evict them
    return new Promise((resolve) => {
      setTimeout(() => {
        limiter.cleanup();
        assert.strictEqual(limiter.requests.size, 0, 'Expired IPs must be removed by cleanup()');
        resolve();
      }, 70);
    });
  });

  await t.test('3. Database pragmas, indexes, and case-insensitive lookup', () => {
    const db = new AppDatabase();
    
    // Check indexes exist
    const indexes = db.db.prepare(`SELECT name FROM sqlite_master WHERE type='index'`).all();
    const indexNames = indexes.map(idx => idx.name);
    assert.ok(indexNames.includes('idx_users_pairing_code'), 'Index idx_users_pairing_code must exist');
    assert.ok(indexNames.includes('idx_workout_sets_session'), 'Index idx_workout_sets_session must exist');

    // Check case-insensitive username lookup
    const testUsername = 'testCase_M1_' + Date.now();
    const userId = db.createUser(testUsername, 'hash123', 'athlete', 'Test Case M1', '+79990001122');
    
    const lowerUser = db.findUserByUsername(testUsername.toLowerCase());
    assert.ok(lowerUser, 'Must find user with lowercase query');
    assert.strictEqual(lowerUser.id, userId);

    const upperUser = db.findUserByUsername(testUsername.toUpperCase());
    assert.ok(upperUser, 'Must find user with uppercase query');
    assert.strictEqual(upperUser.id, userId);

    // Clean up
    db.db.prepare(`DELETE FROM users WHERE id = ?`).run(userId);
  });

  await t.test('4. Bot /approve_<id> authorization check', async () => {
    const db = new AppDatabase();
    const telegramOtpStore = new Map();
    const userTgChatMap = new Map();

    const trainerUser = 'pending_trainer_' + Date.now();
    const trainerId = db.createUser(trainerUser, 'hash', 'trainer', 'Тренер Кандидат', '', '', '', '', 0);
    assert.strictEqual(db.findUserById(trainerId).is_approved, 0);

    let capturedMessages = [];
    const mockBot = {
      command: () => {},
      hears: () => {},
      on: (event, handler) => {
        if (event === 'message:text') {
          mockBot.textHandler = handler;
        }
      },
      catch: () => {}
    };

    setupBotHandlers(mockBot, { db, telegramOtpStore, userTgChatMap });

    // Unauthorized user attempt
    const unauthorizedCtx = {
      from: { id: 999999, username: 'random_hacker' },
      message: { text: `/approve_${trainerId}` },
      reply: async (msg) => { capturedMessages.push(msg); }
    };
    await mockBot.textHandler(unauthorizedCtx);

    assert.ok(capturedMessages.includes('У вас нет прав администратора'), 'Unauthorized user must be rejected');
    assert.strictEqual(db.findUserById(trainerId).is_approved, 0, 'Trainer must remain unapproved');

    // Authorized user attempt (SantiLA213)
    capturedMessages = [];
    const authorizedCtx = {
      from: { id: 111111, username: 'SantiLA213' },
      message: { text: `/approve_${trainerId}` },
      reply: async (msg) => { capturedMessages.push(msg); }
    };
    await mockBot.textHandler(authorizedCtx);

    assert.strictEqual(db.findUserById(trainerId).is_approved, 1, 'Trainer must be approved by SantiLA213');
    assert.ok(capturedMessages.some(m => m.includes('АККАУНТ ТРЕНЕРА ПОДТВЕРЖДЕН')), 'Confirmation message sent');

    // Clean up
    db.db.prepare(`DELETE FROM users WHERE id = ?`).run(trainerId);
  });

  await t.test('5. Bot dedicated /start login handler', async () => {
    const db = new AppDatabase();
    const telegramOtpStore = new Map();
    const userTgChatMap = new Map();

    let startHandler;
    const mockBot = {
      command: (cmd, handler) => {
        if (cmd === 'start') startHandler = handler;
      },
      hears: () => {},
      on: () => {},
      catch: () => {}
    };

    setupBotHandlers(mockBot, { db, telegramOtpStore, userTgChatMap });

    let sentMessage = null;
    let sentOptions = null;
    const loginCtx = {
      from: { id: 123456, username: 'login_tester' },
      chat: { id: 123456 },
      match: 'login',
      reply: async (msg, opts) => {
        sentMessage = msg;
        sentOptions = opts;
      }
    };

    await startHandler(loginCtx);
    assert.ok(sentMessage.includes('Код авторизации в системе:'), 'Must include header');
    assert.ok(/<code>\d{6}<\/code>/.test(sentMessage), 'Must include 6-digit monospaced code');
    assert.ok(sentMessage.includes('5 минут'), 'Must state 5-minute validity');
    assert.ok(sentMessage.includes('Никому не передавайте этот код'), 'Must include security warning');
  });
});
