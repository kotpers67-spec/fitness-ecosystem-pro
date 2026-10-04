process.env.NODE_ENV = 'test';
const { describe, it, before, after } = require('node:test');
const assert = require('node:assert/strict');
const http = require('node:http');
const path = require('node:path');
const { server, db, telegramOtpStore, telegramSessionStore, userTgChatMap } = require('../src/server');
const { handleLinkToken, send2FAOtp, createOwnersKeyboard, setupBotHandlers, OWNER_LINKS } = require('../src/bot');

let baseUrl;
let athleteToken = null;
let athleteId = null;
let athletePin = null;

let trainerToken = null;
let trainerId = null;
let linkedAthleteTgId = 980000000 + Math.floor(Math.random() * 999999);

function request(method, endpoint, headers = {}, body = null) {
  return new Promise((resolve, reject) => {
    const url = new URL(endpoint, baseUrl);
    const req = http.request({
      hostname: url.hostname,
      port: url.port,
      path: url.pathname + url.search,
      method,
      headers: {
        'Content-Type': 'application/json',
        ...headers
      }
    }, res => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        let json = null;
        try {
          if (data) json = JSON.parse(data);
        } catch (_) {}
        resolve({
          statusCode: res.statusCode,
          headers: res.headers,
          body: json,
          rawBody: data
        });
      });
    });
    req.on('error', reject);
    if (body) {
      req.write(typeof body === 'string' ? body : JSON.stringify(body));
    }
    req.end();
  });
}

let localServer;

describe('PIN 5-Min TTL & Telegram 2FA Authentication Test Suite', () => {
  before(async () => {
    await new Promise(resolve => {
      localServer = http.createServer(server.listeners('request')[0]);
      localServer.listen(0, () => {
        baseUrl = `http://127.0.0.1:${localServer.address().port}`;
        resolve();
      });
    });

    // Create athlete
    const athRes = await request('POST', '/api/register', {}, {
      username: 'pin_ath_' + Date.now(),
      password: 'password123',
      role: 'athlete',
      fullName: 'Тестовый Атлет PIN'
    });
    assert.equal(athRes.statusCode, 201);
    athleteToken = athRes.body.token;
    athleteId = athRes.body.user.id;
    athletePin = athRes.body.user.pairingCode;

    // Create trainer
    const trRes = await request('POST', '/api/register', {}, {
      username: 'pin_tr_' + Date.now(),
      password: 'password123',
      role: 'trainer',
      fullName: 'Тестовый Тренер PIN'
    });
    assert.equal(trRes.statusCode, 201);
    trainerToken = trRes.body.token;
    trainerId = trRes.body.user.id;
  });

  after(async () => {
    if (localServer) {
      await new Promise(res => localServer.close(res));
    }
    if (db && typeof db.close === 'function') {
      db.close();
    }
    setTimeout(() => process.exit(0), 100);
  });

  it('1. Pairing PIN has valid 5-min creation timestamp', async () => {
    const meRes = await request('GET', '/api/me', {
      Authorization: `Bearer ${athleteToken}`
    });
    assert.equal(meRes.statusCode, 200);
    assert.ok(meRes.body.user.pairing_code_created_at > 0);
  });

  it('2. Rejects pairing if 5-minute window has expired', async () => {
    // Manually expire PIN in DB to 6 minutes ago
    const sixMinutesAgo = Date.now() - 6 * 60 * 1000;
    db.db.prepare('UPDATE users SET pairing_code_created_at = ? WHERE id = ?').run(sixMinutesAgo, athleteId);

    const pairRes = await request('POST', '/api/trainer/pair', {
      Authorization: `Bearer ${trainerToken}`
    }, {
      code: athletePin
    });

    assert.equal(pairRes.statusCode, 400);
    assert.ok(pairRes.body.error.includes('истёк'));
  });

  it('3. Regenerates new PIN with fresh 5-minute timer', async () => {
    const regenRes = await request('POST', '/api/athlete/regenerate-pin', {
      Authorization: `Bearer ${athleteToken}`
    });
    assert.equal(regenRes.statusCode, 200);
    assert.equal(regenRes.body.expiresInSeconds, 300);
    assert.notEqual(regenRes.body.pairingCode, athletePin);
    athletePin = regenRes.body.pairingCode;
  });

  it('4. Successfully pairs athlete with active PIN and consumes the code', async () => {
    const pairRes = await request('POST', '/api/trainer/pair', {
      Authorization: `Bearer ${trainerToken}`
    }, {
      code: athletePin
    });

    assert.equal(pairRes.statusCode, 200);
    assert.equal(pairRes.body.success, true);

    // Code is now consumed, second attempt must fail with strict HTTP 400
    const secondPair = await request('POST', '/api/trainer/pair', {
      Authorization: `Bearer ${trainerToken}`
    }, {
      code: athletePin
    });
    assert.equal(secondPair.statusCode, 400);
    assert.ok(secondPair.body.error.includes('не найден') || secondPair.body.error.includes('использован'));
  });

  it('5. Links Telegram account via OTP verification', async () => {
    const tgUsername = 'test_owner_tg_' + Date.now();
    const reqRes = await request('POST', '/api/user/telegram/link-request', {
      Authorization: `Bearer ${athleteToken}`
    }, {
      username: tgUsername
    });
    assert.equal(reqRes.statusCode, 200);
    const code = reqRes.body.debugCode;
    assert.ok(code);

    const confRes = await request('POST', '/api/user/telegram/link-confirm', {
      Authorization: `Bearer ${athleteToken}`
    }, {
      username: tgUsername,
      code
    });
    assert.equal(confRes.statusCode, 200);
    assert.equal(confRes.body.telegramUsername, tgUsername.toLowerCase());
  });

  it('6. Enables 2FA and enforces 6-digit OTP on login', async () => {
    const tfaToggle = await request('POST', '/api/user/2fa', {
      Authorization: `Bearer ${athleteToken}`
    }, {
      enabled: true
    });
    assert.equal(tfaToggle.statusCode, 200);
    assert.equal(tfaToggle.body.twoFactorEnabled, true);

    // Login now requires 2FA
    const athUser = db.findUserById(athleteId);
    const loginRes = await request('POST', '/api/login', {}, {
      username: athUser.username,
      password: 'password123'
    });
    assert.equal(loginRes.statusCode, 200);
    assert.equal(loginRes.body.require2FA, true);
    assert.equal(loginRes.body.userId, athleteId);
    const otp = loginRes.body.debugCode;
    assert.ok(otp);

    // Complete login with 2FA code
    const verifyRes = await request('POST', '/api/login/2fa', {}, {
      userId: athleteId,
      code: otp
    });
    assert.equal(verifyRes.statusCode, 200);
    assert.ok(verifyRes.body.token);
    assert.equal(verifyRes.body.user.id, athleteId);
  });

  it('7. Generates 5-minute deep link token for Telegram bot linking (POST /api/user/telegram/link-token)', async () => {
    const tokenRes = await request('POST', '/api/user/telegram/link-token', {
      Authorization: `Bearer ${athleteToken}`
    });
    assert.equal(tokenRes.statusCode, 200);
    assert.ok(tokenRes.body.token);
    assert.equal(tokenRes.body.expiresInSeconds, 300);

    const stored = db.findLinkToken(tokenRes.body.token);
    assert.ok(stored);
    assert.equal(stored.user_id, athleteId);
  });

  let boundAthleteTgId = null;

  it('8. Telegram Bot deep link (/start link_<token>) binds numeric telegram_id and username', async () => {
    // Generate fresh link token
    const tokenRes = await request('POST', '/api/user/telegram/link-token', {
      Authorization: `Bearer ${athleteToken}`
    });
    assert.equal(tokenRes.statusCode, 200);
    const token = tokenRes.body.token;

    // Simulate grammY context for /start link_<token> with fresh unique TG ID
    boundAthleteTgId = 980000000 + Math.floor(Math.random() * 999999);
    let repliedMsg = '';
    let replyOptions = null;
    const mockCtx = {
      from: {
        id: boundAthleteTgId,
        username: 'ProAthleteBotUser',
        first_name: 'Иван',
        last_name: 'Иванов'
      },
      chat: { id: boundAthleteTgId },
      match: `link_${token}`,
      reply: async (msg, opts) => {
        repliedMsg = msg;
        replyOptions = opts;
      }
    };

    const linkResult = await handleLinkToken(mockCtx, token, db, userTgChatMap);
    assert.equal(linkResult.success, true);
    assert.equal(linkResult.userId, athleteId);
    assert.equal(linkResult.telegramId, String(boundAthleteTgId));
    assert.equal(linkResult.telegramUsername, 'proathletebotuser');

    // Verify persisted in SQLite
    const updatedUser = db.findUserById(athleteId);
    assert.equal(updatedUser.telegram_id, String(boundAthleteTgId));
    assert.equal(updatedUser.telegram_username, 'proathletebotuser');

    // Verify token consumed
    assert.equal(db.findLinkToken(token), null);

    // Verify bot replied with confirmation and owner buttons
    assert.ok(repliedMsg.includes('Telegram успешно привязан'));
    assert.ok(replyOptions?.reply_markup);
  });

  it('9. Command /link <token> binds account and single-use token cannot be reused', async () => {
    // Generate fresh link token for trainer
    const tokenRes = await request('POST', '/api/user/telegram/link-token', {
      Authorization: `Bearer ${trainerToken}`
    });
    assert.equal(tokenRes.statusCode, 200);
    const token = tokenRes.body.token;

    const testTrainerTgId = 880000000 + Math.floor(Math.random() * 999999);
    let repliedMsg = '';
    const mockCtx = {
      from: {
        id: testTrainerTgId,
        username: 'ProTrainerUser',
        first_name: 'Виктор'
      },
      chat: { id: testTrainerTgId },
      match: token,
      reply: async (msg) => {
        repliedMsg = msg;
      }
    };

    // First use: must succeed
    const firstAttempt = await handleLinkToken(mockCtx, token, db, userTgChatMap);
    assert.equal(firstAttempt.success, true);
    assert.equal(firstAttempt.userId, trainerId);

    const updatedTrainer = db.findUserById(trainerId);
    assert.equal(updatedTrainer.telegram_id, String(testTrainerTgId));

    // Second use with same token: must fail (single-use consumed)
    const secondAttempt = await handleLinkToken(mockCtx, token, db, userTgChatMap);
    assert.equal(secondAttempt.success, false);
    assert.equal(secondAttempt.reason, 'expired_or_invalid');
    assert.ok(repliedMsg.includes('недействительна') || repliedMsg.includes('истёк'));
  });

  it('10. Rejects expired deep link token (>5 min)', async () => {
    const tokenRes = await request('POST', '/api/user/telegram/link-token', {
      Authorization: `Bearer ${athleteToken}`
    });
    assert.equal(tokenRes.statusCode, 200);
    const token = tokenRes.body.token;

    // Manually expire token in DB to 6 minutes ago
    db.db.prepare('UPDATE telegram_link_tokens SET expires_at = ? WHERE token = ?').run(Date.now() - 6 * 60 * 1000, token);

    let repliedMsg = '';
    const mockCtx = {
      from: { id: 111222333, username: 'LateUser' },
      chat: { id: 111222333 },
      match: token,
      reply: async (msg) => { repliedMsg = msg; }
    };

    const attempt = await handleLinkToken(mockCtx, token, db, userTgChatMap);
    assert.equal(attempt.success, false);
    assert.equal(attempt.reason, 'expired_or_invalid');
    assert.ok(repliedMsg.includes('истёк') || repliedMsg.includes('недействительна'));
  });

  it('11. Delivers 2FA OTP to Telegram chat with owner support buttons', async () => {
    let sentToChat = null;
    let sentMessage = '';
    let sentOptions = null;

    const mockBot = {
      api: {
        sendMessage: async (chatId, text, opts) => {
          sentToChat = chatId;
          sentMessage = text;
          sentOptions = opts;
          return { message_id: 12345 };
        }
      }
    };

    const otpCode = '742918';
    const delivered = await send2FAOtp(mockBot, String(boundAthleteTgId || '987654321'), otpCode);
    assert.equal(delivered, true);
    assert.equal(sentToChat, String(boundAthleteTgId || '987654321'));
    assert.ok(sentMessage.includes('742918'));
    assert.ok(sentMessage.includes('5 минут'));
    assert.ok(sentOptions?.reply_markup);
  });

  it('12. Telegram Bot provides verified owner contact links (@SantiLA213 and @Spirit5449)', () => {
    assert.equal(OWNER_LINKS.santi, 'https://t.me/SantiLA213');
    assert.equal(OWNER_LINKS.spirit, 'https://t.me/Spirit5449');

    const keyboard = createOwnersKeyboard();
    assert.ok(keyboard);
    // Serialize keyboard inline rows
    const rows = keyboard.inline_keyboard;
    assert.ok(Array.isArray(rows));
    const allButtons = rows.flat();
    const hasSanti = allButtons.some(b => b.url === 'https://t.me/SantiLA213' && b.text.includes('@SantiLA213'));
    const hasSpirit = allButtons.some(b => b.url === 'https://t.me/Spirit5449' && b.text.includes('@Spirit5449'));
    assert.ok(hasSanti, 'Inline keyboard must contain @SantiLA213');
    assert.ok(hasSpirit, 'Inline keyboard must contain @Spirit5449');
  });

  it('13. Strictly prevents binding same Telegram ID to multiple accounts', async () => {
    // Register a new distinct athlete
    const newAthRes = await request('POST', '/api/register', {}, {
      username: 'ath_conflict_' + Date.now(),
      password: 'password123',
      role: 'athlete',
      fullName: 'Конфликтный Атлет'
    });
    assert.equal(newAthRes.statusCode, 201);
    const newTokenRes = await request('POST', '/api/user/telegram/link-token', {
      Authorization: `Bearer ${newAthRes.body.token}`
    });
    assert.equal(newTokenRes.statusCode, 200);

    let conflictMsg = '';
    const mockCtx = {
      from: {
        id: boundAthleteTgId, // Bound to athleteId in test 8
        username: 'OtherUser'
      },
      chat: { id: boundAthleteTgId },
      match: newTokenRes.body.token,
      reply: async (msg) => { conflictMsg = msg; }
    };

    const conflictAttempt = await handleLinkToken(mockCtx, newTokenRes.body.token, db, userTgChatMap);
    assert.equal(conflictAttempt.success, false);
    assert.equal(conflictAttempt.reason, 'already_linked_to_other');
    assert.ok(conflictMsg.includes('уже привязан к аккаунту'));
  });

  it('14. Seamless 1-Click Telegram Login: session-init and bot auth flow', async () => {
    // 1. Initialize 1-click session
    const initRes = await request('POST', '/api/auth/telegram/session-init');
    assert.equal(initRes.statusCode, 200);
    assert.ok(initRes.body.sessionId);
    assert.ok(initRes.body.sessionId.startsWith('auth_'));
    assert.ok(initRes.body.botUrl.includes(initRes.body.sessionId));

    // 2. Poll initial status -> PENDING
    const pollPending = await request('GET', `/api/auth/telegram/session-status?sessionId=${initRes.body.sessionId}`);
    assert.equal(pollPending.statusCode, 200);
    assert.equal(pollPending.body.status, 'PENDING');

    // 3. Simulate bot receiving /start auth_<sessionId>
    let botReplyText = '';
    const commands = new Map();
    const mockBot = {
      command: (cmd, handler) => { commands.set(cmd, handler); },
      hears: () => {},
      on: () => {},
      catch: () => {}
    };
    setupBotHandlers(mockBot, { db, telegramOtpStore: new Map(), telegramSessionStore, userTgChatMap, cloudSyncService: null });

    const oneClickTgId = 991000000 + Math.floor(Math.random() * 99999);
    const mockStartCtx = {
      from: {
        id: oneClickTgId,
        username: 'oneclick_hero_' + Date.now(),
        first_name: 'ОдинКлик',
        last_name: 'Атлет'
      },
      chat: { id: oneClickTgId },
      match: initRes.body.sessionId,
      reply: async (msg) => { botReplyText = msg; }
    };

    await commands.get('start')(mockStartCtx);
    assert.ok(botReplyText.includes('Вход в Fitness Ecosystem Pro выполнен в 1 клик'));

    // 4. Poll status -> AUTHORIZED
    const pollAuth = await request('GET', `/api/auth/telegram/session-status?sessionId=${initRes.body.sessionId}`);
    assert.equal(pollAuth.statusCode, 200);
    assert.equal(pollAuth.body.status, 'AUTHORIZED');
    assert.ok(pollAuth.body.token);
    assert.equal(pollAuth.body.user.fullName, 'ОдинКлик Атлет');

    // 5. Subsequent poll -> EXPIRED (single-use consumption)
    const pollConsumed = await request('GET', `/api/auth/telegram/session-status?sessionId=${initRes.body.sessionId}`);
    assert.equal(pollConsumed.statusCode, 200);
    assert.equal(pollConsumed.body.status, 'EXPIRED');
  });

  it('15. Enforces 2FA security during 1-Click Telegram Login if 2FA is enabled', async () => {
    // 1. Register a user
    const userWith2Fa = await request('POST', '/api/register', {}, {
      username: 'ath_2fa_1click_' + Date.now(),
      password: 'password123',
      role: 'athlete',
      fullName: 'Защищённый Атлет'
    });
    assert.equal(userWith2Fa.statusCode, 201);
    const userId = userWith2Fa.body.user.id;
    const token = userWith2Fa.body.token;

    // Link Telegram to this user FIRST (prerequisite for 2FA)
    const linkTokenRes = await request('POST', '/api/user/telegram/link-token', {
      Authorization: `Bearer ${token}`
    });
    const linkToken = linkTokenRes.body.token;
    const tgId = 882000000 + Math.floor(Math.random() * 99999);
    const mockCtx = {
      from: { id: tgId, username: 'protected_user_tg' },
      chat: { id: tgId },
      match: linkToken,
      reply: async () => {}
    };
    await handleLinkToken(mockCtx, linkToken, db, userTgChatMap);

    // Now enable 2FA on this user
    const enable2FaRes = await request('POST', '/api/user/2fa', {
      Authorization: `Bearer ${token}`
    }, { enabled: true });
    assert.equal(enable2FaRes.statusCode, 200);
    assert.equal(enable2FaRes.body.twoFactorEnabled, true);

    // 2. Initialize 1-click session
    const initRes = await request('POST', '/api/auth/telegram/session-init');
    assert.equal(initRes.statusCode, 200);
    const sessionId = initRes.body.sessionId;

    // 3. Simulate bot receiving /start auth_<sessionId> for the 2FA user
    let botReplyText = '';
    const commands = new Map();
    const mockBot = {
      command: (cmd, handler) => { commands.set(cmd, handler); },
      hears: () => {},
      on: () => {},
      catch: () => {}
    };
    setupBotHandlers(mockBot, { db, telegramOtpStore, telegramSessionStore, userTgChatMap, cloudSyncService: null });

    const mockStartCtx = {
      from: {
        id: tgId,
        username: 'protected_user_tg',
        first_name: 'Защищённый',
        last_name: 'Атлет'
      },
      chat: { id: tgId },
      match: sessionId,
      reply: async (msg) => { botReplyText = msg; }
    };

    await commands.get('start')(mockStartCtx);

    // Bot MUST notify that 2FA is required and provide the 6-digit code
    assert.ok(botReplyText.includes('включена 2FA аутентификация'));
    assert.ok(botReplyText.includes('Вход в 1 клик заблокирован'));

    // Extract OTP code from bot reply
    const otpMatch = botReplyText.match(/<code>(\d{6})<\/code>/);
    assert.ok(otpMatch, 'Bot response must contain 6-digit OTP code');
    const otpCode = otpMatch[1];

    // 4. Poll status -> MUST return REQUIRES_2FA, NOT AUTHORIZED!
    const pollStatus = await request('GET', `/api/auth/telegram/session-status?sessionId=${sessionId}`);
    assert.equal(pollStatus.statusCode, 200);
    assert.equal(pollStatus.body.status, 'REQUIRES_2FA');
    assert.equal(pollStatus.body.userId, userId);
    assert.ok(pollStatus.body.expiresInSeconds > 0);

    // 5. Complete login via 2FA verification endpoint
    const verify2FaRes = await request('POST', '/api/login/2fa', {}, {
      userId,
      code: otpCode
    });
    assert.equal(verify2FaRes.statusCode, 200);
    assert.ok(verify2FaRes.body.token);
    assert.equal(verify2FaRes.body.user.id, userId);
  });
});

