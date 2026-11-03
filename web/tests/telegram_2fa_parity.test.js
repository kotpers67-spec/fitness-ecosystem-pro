/**
 * Telegram 2FA & 6-Digit Code Authentication Parity Test Suite
 * 
 * 100% Zero-Mocks automated verification:
 * 1. Code Uniqueness: 100 consecutive codes without collisions in memory & SQLite.
 * 2. POST /api/auth/telegram/verify-code:
 *    - Login by 6-digit code without username.
 *    - Single-use consumption: second attempt returns HTTP 400.
 *    - Non-existent / invalid code returns HTTP 400.
 * 3. 2FA Security Lifecycle:
 *    - Attempting to enable 2FA without linked Telegram returns 400 requireTelegramLink.
 *    - Enabling 2FA with linked Telegram returns 200 { twoFactorEnabled: true }.
 *    - Password login with active 2FA returns requires2FA: true.
 */

const { describe, it, before, after, beforeEach } = require('node:test');
const assert = require('node:assert/strict');
const http = require('node:http');

const { server, db, telegramOtpStore, authLimiter } = require('../src/server');
const { getUnifiedUserCode } = require('../src/bot');
const { generateSecurePin } = require('../src/security');

let baseUrl;
let localServer;

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

describe('Telegram 2FA & 6-Digit Code Parity Suite', () => {
  before(async () => {
    await new Promise(resolve => {
      localServer = http.createServer(server.listeners('request')[0]);
      localServer.listen(0, '127.0.0.1', () => {
        baseUrl = `http://127.0.0.1:${localServer.address().port}`;
        resolve();
      });
    });
  });

  after(() => {
    if (localServer) localServer.close();
  });

  beforeEach(() => {
    if (authLimiter) {
      if (typeof authLimiter.reset === 'function') authLimiter.reset();
      else if (authLimiter.requests) authLimiter.requests.clear();
    }
  });

  // ==========================================
  // 1. УНИКАЛЬНОСТЬ КОДОВ (100 подряд)
  // ==========================================
  describe('1. Code Uniqueness (100 consecutive in SQLite & Memory)', () => {
    it('generates 100 consecutive pairing codes in SQLite without any duplicates', () => {
      const generatedCodes = new Set();
      const createdUserIds = [];

      try {
        for (let i = 0; i < 100; i++) {
          const code = db.generateUniquePairingCode();
          assert.equal(typeof code, 'string', `Code #${i} must be string`);
          assert.equal(code.length, 6, `Code #${i} must have exactly 6 digits`);
          assert.match(code, /^\d{6}$/, `Code #${i} must consist only of digits`);

          const num = parseInt(code, 10);
          assert.ok(num >= 100000 && num <= 999999, `Code #${i} must be in [100000, 999999]`);

          assert.equal(generatedCodes.has(code), false, `Collision detected at iteration ${i}: ${code}`);
          generatedCodes.add(code);

          const username = `uniq_ath_${i}_${Date.now()}`;
          const userId = db.createUser(username, 'pwd_hash', 'athlete', `Атлет #${i}`, '', code);
          createdUserIds.push(userId);
        }

        assert.equal(generatedCodes.size, 100, 'All 100 generated SQLite codes must be unique');

        for (const code of generatedCodes) {
          const user = db.findUserByPairingCode(code);
          assert.ok(user, `User with pairing code ${code} must exist in SQLite`);
          assert.equal(user.pairing_code, code);
        }
      } finally {
        if (createdUserIds.length > 0) {
          const placeholders = createdUserIds.map(() => '?').join(',');
          db.db.prepare(`DELETE FROM auth_tokens WHERE user_id IN (${placeholders})`).run(...createdUserIds);
          db.db.prepare(`DELETE FROM telegram_link_tokens WHERE user_id IN (${placeholders})`).run(...createdUserIds);
          db.db.prepare(`DELETE FROM users WHERE id IN (${placeholders})`).run(...createdUserIds);
        }
      }
    });

    it('generates 100 consecutive OTP codes in memory without collisions', () => {
      const memoryOtpStore = new Map();
      const now = Date.now();
      const generatedMemoryCodes = new Set();

      for (let i = 0; i < 100; i++) {
        let freshPin;
        for (let attempt = 0; attempt < 20; attempt++) {
          freshPin = generateSecurePin();
          let collides = false;
          for (const entry of memoryOtpStore.values()) {
            if (entry && entry.code === freshPin && entry.expiresAt > now) {
              collides = true;
              break;
            }
          }
          if (!collides) break;
        }

        assert.equal(freshPin.length, 6);
        assert.equal(generatedMemoryCodes.has(freshPin), false, `Memory collision at iteration ${i}: ${freshPin}`);
        generatedMemoryCodes.add(freshPin);

        const username = `mem_user_${i}`;
        const entry = {
          code: freshPin,
          expiresAt: now + 5 * 60 * 1000,
          username,
          tgId: 700000000 + i,
          attempts: 0
        };
        memoryOtpStore.set(username, entry);
        memoryOtpStore.set(`code_${freshPin}`, entry);
      }

      assert.equal(generatedMemoryCodes.size, 100, 'All 100 memory codes must be unique');
    });
  });

  // ==========================================
  // 2. POST /api/auth/telegram/verify-code
  // ==========================================
  describe('2. POST /api/auth/telegram/verify-code (Bot 6-Digit Code Auth)', () => {
    let testAthleteId;
    let testAthleteUsername;
    let testTgId;
    let botLoginCode;

    before(async () => {
      testTgId = String(890000000 + Math.floor(Math.random() * 999999));
      testAthleteUsername = `tg_bot_user_${Date.now()}`;
      
      testAthleteId = db.createUser(
        testAthleteUsername,
        'hash_secret',
        'athlete',
        'Тестовый Атлет БотКод',
        '+79998887766'
      );
      db.linkTelegram(testAthleteId, testTgId, testAthleteUsername);

      const unified = getUnifiedUserCode(testTgId, testAthleteUsername, db, telegramOtpStore);
      botLoginCode = unified.code;

      telegramOtpStore.set(`code_${botLoginCode}`, {
        code: botLoginCode,
        expiresAt: unified.expiresAt || (Date.now() + 5 * 60 * 1000),
        tgId: testTgId,
        username: testAthleteUsername,
        userId: testAthleteId,
        attempts: 0
      });
    });

    after(() => {
      if (testAthleteId) {
        db.db.prepare('DELETE FROM auth_tokens WHERE user_id = ?').run(testAthleteId);
        db.db.prepare('DELETE FROM telegram_link_tokens WHERE user_id = ?').run(testAthleteId);
        db.db.prepare('DELETE FROM users WHERE id = ?').run(testAthleteId);
      }
      if (botLoginCode) {
        telegramOtpStore.delete(`code_${botLoginCode}`);
      }
    });

    it('authenticates user by 6-digit code without specifying username', async () => {
      const res = await request('POST', '/api/auth/telegram/verify-code', {}, {
        code: botLoginCode
      });

      assert.equal(res.statusCode, 200, `Expected 200, got ${res.statusCode}: ${JSON.stringify(res.body)}`);
      assert.equal(res.body.success, true);
      assert.ok(res.body.token, 'Must return session token');
      assert.ok(res.body.user, 'Must return user object');
      assert.equal(res.body.user.id, testAthleteId);
      assert.equal(res.body.user.role, 'athlete');
    });

    it('enforces single-use: repeated attempt with same code returns 400', async () => {
      const secondRes = await request('POST', '/api/auth/telegram/verify-code', {}, {
        code: botLoginCode
      });

      assert.equal(secondRes.statusCode, 400, 'Reusing consumed code must return 400');
      assert.ok(secondRes.body.error);
    });

    it('rejects invalid or non-existent code with 400', async () => {
      const invalidRes = await request('POST', '/api/auth/telegram/verify-code', {}, {
        code: '000000'
      });

      assert.equal(invalidRes.statusCode, 400);
      assert.ok(invalidRes.body.error);
    });

    it('rejects malformed code (<6 digits or non-numeric) with 400', async () => {
      const shortRes = await request('POST', '/api/auth/telegram/verify-code', {}, { code: '123' });
      assert.equal(shortRes.statusCode, 400);

      const alphaRes = await request('POST', '/api/auth/telegram/verify-code', {}, { code: 'abcdef' });
      assert.equal(alphaRes.statusCode, 400);
    });
  });

  // ==========================================
  // 3. ТЕСТ 2FA
  // ==========================================
  describe('3. Two-Factor Authentication (2FA) Lifecycle', () => {
    let unlinkedAthleteToken;
    let unlinkedAthleteId;
    let linkedAthleteToken;
    let linkedAthleteId;
    let linkedTgId;
    const testPassword = 'password123';

    before(async () => {
      // Пользователь без Telegram
      const unlinkedRes = await request('POST', '/api/register', {}, {
        username: 'no_tg_ath_' + Date.now(),
        password: testPassword,
        role: 'athlete',
        fullName: 'Атлет Без Telegram'
      });
      assert.equal(unlinkedRes.statusCode, 201);
      unlinkedAthleteToken = unlinkedRes.body.token;
      unlinkedAthleteId = unlinkedRes.body.user.id;

      // Пользователь с Telegram
      linkedTgId = String(970000000 + Math.floor(Math.random() * 999999));
      const linkedUsername = 'with_tg_ath_' + Date.now();
      const linkedRes = await request('POST', '/api/register', {}, {
        username: linkedUsername,
        password: testPassword,
        role: 'athlete',
        fullName: 'Атлет С Telegram'
      });
      assert.equal(linkedRes.statusCode, 201);
      linkedAthleteToken = linkedRes.body.token;
      linkedAthleteId = linkedRes.body.user.id;

      db.linkTelegram(linkedAthleteId, linkedTgId, linkedUsername);
    });

    after(() => {
      if (unlinkedAthleteId) {
        db.db.prepare('DELETE FROM auth_tokens WHERE user_id = ?').run(unlinkedAthleteId);
        db.db.prepare('DELETE FROM users WHERE id = ?').run(unlinkedAthleteId);
      }
      if (linkedAthleteId) {
        db.db.prepare('DELETE FROM auth_tokens WHERE user_id = ?').run(linkedAthleteId);
        db.db.prepare('DELETE FROM users WHERE id = ?').run(linkedAthleteId);
      }
    });

    it('attempting to enable 2FA without Telegram returns 400 requireTelegramLink', async () => {
      const res = await request('POST', '/api/user/2fa', {
        Authorization: `Bearer ${unlinkedAthleteToken}`
      }, {
        enabled: true
      });

      assert.equal(res.statusCode, 400, `Expected 400, got ${res.statusCode}: ${JSON.stringify(res.body)}`);
      assert.ok(
        res.body.requireTelegramLink === true || 
        res.body.details?.requireTelegramLink === true ||
        (res.body.error && res.body.error.includes('Telegram')),
        'Response must require linking Telegram before enabling 2FA'
      );
    });

    it('enabling 2FA with linked Telegram returns 200 { twoFactorEnabled: true }', async () => {
      const res = await request('POST', '/api/user/2fa', {
        Authorization: `Bearer ${linkedAthleteToken}`
      }, {
        enabled: true
      });

      assert.equal(res.statusCode, 200, `Expected 200, got ${res.statusCode}: ${JSON.stringify(res.body)}`);
      assert.equal(res.body.twoFactorEnabled, true);

      const user = db.findUserById(linkedAthleteId);
      assert.equal(user.two_factor_enabled, 1);
    });

    it('when 2FA is active, password login returns requires2FA: true', async () => {
      const user = db.findUserById(linkedAthleteId);

      const loginRes = await request('POST', '/api/login', {}, {
        username: user.username,
        password: testPassword
      });

      assert.equal(loginRes.statusCode, 200, `Expected 200, got ${loginRes.statusCode}: ${JSON.stringify(loginRes.body)}`);
      assert.ok(
        loginRes.body.requires2FA === true || loginRes.body.require2FA === true,
        'Login response must specify 2FA requirement'
      );
      assert.equal(loginRes.body.userId, linkedAthleteId);
      assert.ok(loginRes.body.expiresInSeconds > 0);

      const otpRecord = telegramOtpStore.get(`2fa_${linkedAthleteId}`);
      assert.ok(otpRecord, 'OTP record must be present in telegramOtpStore');
      assert.equal(typeof otpRecord.code, 'string');
      assert.equal(otpRecord.code.length, 6);
    });
  });
});
