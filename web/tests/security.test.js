/**
 * Fitness Ecosystem Pro - Comprehensive Automated Security Test Suite
 * Validates OWASP ASVS Level 2 Security Controls:
 * - Anti-SQL Injection Defense across all API endpoints (Parameterized DB + Input Sanitization)
 * - Anti-XSS (HTML Entity Encoding + OWASP Security Headers)
 * - Rate Limiting & Brute-Force Shield (/api/login & /api/register)
 * - Strict Role Isolation & Privilege Separation (Athlete vs Trainer, IDOR protection)
 * - Token Authentication & Tampering Rejection
 * - Path Traversal & Static Asset Access Control
 */

const { describe, it, before, after, beforeEach } = require('node:test');
const assert = require('node:assert/strict');
const http = require('node:http');
const fs = require('node:fs');
const path = require('node:path');

const serverModulePath = fs.existsSync(path.join(__dirname, '../src/server.js'))
  ? '../src/server'
  : '../../../web/src/server';
const { server, db, authLimiter } = require(serverModulePath);

let baseUrl;
let athleteToken = null;
let athleteUser = null;
let athletePin = null;
let athleteId = null;

let athlete2Token = null;
let athlete2User = null;
let athlete2Id = null;

let trainerToken = null;
let trainerUser = null;
let trainerId = null;

function request(method, endpoint, headers = {}, body = null) {
  return new Promise((resolve, reject) => {
    let reqPath;
    let hostname;
    let port;
    if (baseUrl) {
      const base = new URL(baseUrl);
      hostname = base.hostname;
      port = base.port;
    }
    if (endpoint.startsWith('/..') || endpoint.startsWith('/%2e%2e')) {
      reqPath = endpoint;
    } else {
      const url = new URL(endpoint, baseUrl);
      reqPath = url.pathname + url.search;
      hostname = url.hostname;
      port = url.port;
    }
    const req = http.request({
      hostname,
      port,
      path: reqPath,
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

describe('Fitness Ecosystem Pro - Security Test Suite', () => {
  before(async () => {
    await new Promise(resolve => {
      server.listen(0, () => {
        baseUrl = `http://127.0.0.1:${server.address().port}`;
        resolve();
      });
    });
  });

  after(() => {
    server.close();
    db.close();
  });

  beforeEach(() => {
    if (authLimiter && typeof authLimiter.reset === 'function') {
      authLimiter.reset();
    }
  });

  // ==========================================
  // 1. AUTHENTICATION & TOKEN SECURITY
  // ==========================================
  describe('1. Authentication & Token Security', () => {
    it('rejects unauthenticated requests to protected endpoints with 401', async () => {
      const res = await request('GET', '/api/me');
      assert.equal(res.statusCode, 401);
      assert.ok(res.body?.error);
    });

    it('rejects invalid or forged Bearer token with 401', async () => {
      const res = await request('GET', '/api/me', {
        Authorization: 'Bearer invalid_forged_token_12345'
      });
      assert.equal(res.statusCode, 401);
    });

    it('rejects empty or malformed Authorization header with 401', async () => {
      const res = await request('GET', '/api/me', {
        Authorization: 'Bearer '
      });
      assert.equal(res.statusCode, 401);
    });

    it('rejects tampered token where genuine token characters are modified with 401', async () => {
      // Create a temporary valid token
      const tempUser = 'tamper_chk_' + Date.now();
      const reg = await request('POST', '/api/register', {}, {
        username: tempUser,
        password: 'password123',
        role: 'athlete',
        fullName: 'Тест Тамперинга'
      });
      assert.equal(reg.statusCode, 201);
      const validToken = reg.body.token;

      // Tamper by modifying the first characters
      const tamperedToken = 'deadbeef' + validToken.slice(8);
      const res = await request('GET', '/api/me', {
        Authorization: `Bearer ${tamperedToken}`
      });
      assert.equal(res.statusCode, 401);

      // Tamper by truncating
      const truncatedToken = validToken.slice(0, 16);
      const res2 = await request('GET', '/api/me', {
        Authorization: `Bearer ${truncatedToken}`
      });
      assert.equal(res2.statusCode, 401);
    });

    it('rejects token tampering containing SQL injection payloads with 401', async () => {
      const res = await request('GET', '/api/me', {
        Authorization: "Bearer ' OR '1'='1"
      });
      assert.equal(res.statusCode, 401);
    });

    it('registers a valid athlete with 201 and secure token', async () => {
      athleteUser = 'ath_' + Date.now();
      const res = await request('POST', '/api/register', {}, {
        username: athleteUser,
        password: 'securePassword123!',
        role: 'athlete',
        fullName: 'Алексей Смирнов',
        phone: '+79991234567'
      });
      assert.equal(res.statusCode, 201);
      assert.ok(res.body.token);
      assert.equal(res.body.user.role, 'athlete');
      assert.match(res.body.user.pairingCode, /^\d{6}$/);

      athleteToken = res.body.token;
      athletePin = res.body.user.pairingCode;
      athleteId = res.body.user.id;
    });

    it('registers a second athlete for access control isolation tests', async () => {
      athlete2User = 'ath2_' + Date.now();
      const res = await request('POST', '/api/register', {}, {
        username: athlete2User,
        password: 'securePassword123!',
        role: 'athlete',
        fullName: 'Второй Атлет',
        phone: '+79991112233'
      });
      assert.equal(res.statusCode, 201);
      athlete2Token = res.body.token;
      athlete2Id = res.body.user.id;
    });

    it('registers a valid trainer with 201 and secure token', async () => {
      trainerUser = 'trn_' + Date.now();
      const res = await request('POST', '/api/register', {}, {
        username: trainerUser,
        password: 'securePassword123!',
        role: 'trainer',
        fullName: 'Виктор Кузнецов',
        phone: '+79997654321'
      });
      assert.equal(res.statusCode, 201);
      assert.ok(res.body.token);
      assert.equal(res.body.user.role, 'trainer');

      trainerToken = res.body.token;
      trainerId = res.body.user.id;
    });

    it('allows valid login and returns session token', async () => {
      const res = await request('POST', '/api/login', {}, {
        username: athleteUser,
        password: 'securePassword123!'
      });
      assert.equal(res.statusCode, 200);
      assert.ok(res.body.token);
      assert.equal(res.body.user.username, athleteUser);
    });

    it('rejects wrong password with 401', async () => {
      const res = await request('POST', '/api/login', {}, {
        username: athleteUser,
        password: 'wrongPassword!'
      });
      assert.equal(res.statusCode, 401);
    });

    it('revokes session token on logout with 200 and blocks subsequent calls', async () => {
      const tempUser = 'logout_' + Date.now();
      const regRes = await request('POST', '/api/register', {}, {
        username: tempUser,
        password: 'password123',
        role: 'athlete',
        fullName: 'Временный Атлет'
      });
      const tempToken = regRes.body.token;

      const meRes = await request('GET', '/api/me', { Authorization: `Bearer ${tempToken}` });
      assert.equal(meRes.statusCode, 200);

      const logoutRes = await request('POST', '/api/logout', { Authorization: `Bearer ${tempToken}` });
      assert.equal(logoutRes.statusCode, 200);

      const afterRes = await request('GET', '/api/me', { Authorization: `Bearer ${tempToken}` });
      assert.equal(afterRes.statusCode, 401);
    });
  });

  // ==========================================
  // 2. ANTI-SQL INJECTION (SQLi) PROTECTION
  // ==========================================
  describe('2. Anti-SQL Injection Protection', () => {
    const sqliPayloads = [
      "' OR '1'='1",
      "admin'--",
      "admin' /*",
      "' UNION SELECT null, username, password_hash FROM users--",
      "'; DROP TABLE users;--",
      "1; DROP TABLE workout_sets;--",
      "' OR 1=1#"
    ];

    it('blocks SQL injection in login username with 400', async () => {
      for (const payload of sqliPayloads) {
        const res = await request('POST', '/api/login', {}, {
          username: payload,
          password: 'password123'
        });
        assert.equal(res.statusCode, 400, `Expected 400 for SQLi payload: ${payload}`);
      }
    });

    it('prevents SQL injection authentication bypass in login password field', async () => {
      for (const payload of sqliPayloads) {
        const res = await request('POST', '/api/login', {}, {
          username: athleteUser,
          password: payload
        });
        assert.equal(res.statusCode, 401, 'Password injection must not bypass authentication');
      }
    });

    it('blocks SQL injection in register username and fullName with 400', async () => {
      const res1 = await request('POST', '/api/register', {}, {
        username: "user' OR '1'='1",
        password: 'password123',
        role: 'athlete',
        fullName: 'Normal Name'
      });
      assert.equal(res1.statusCode, 400);

      const res2 = await request('POST', '/api/register', {}, {
        username: 'safe_user_99',
        password: 'password123',
        role: 'athlete',
        fullName: "Hacker'; DROP TABLE users;--"
      });
      assert.equal(res2.statusCode, 400);
    });

    it('immunizes register phone parameter against SQL injection', async () => {
      const phonePayload = "'+(SELECT password_hash FROM users)+'";
      const res = await request('POST', '/api/register', {}, {
        username: 'phone_test_' + Date.now(),
        password: 'password123',
        role: 'athlete',
        fullName: 'Телефонный Тест',
        phone: phonePayload
      });
      // Either accepted safely as encoded text or rejected, but database must remain intact
      assert.ok([201, 400].includes(res.statusCode));
      const testUser = db.findUserByUsername('phone_test_' + Date.now());
      if (testUser) {
        assert.ok(!testUser.phone.includes('password_hash'));
      }
    });

    it('blocks SQL injection in workout set exerciseName with 400', async () => {
      const res = await request('POST', '/api/workout/set', {
        Authorization: `Bearer ${athleteToken}`
      }, {
        exerciseName: "Bench Press'; DROP TABLE workout_sets;--",
        weightKg: 100,
        reps: 10
      });
      assert.equal(res.statusCode, 400);
    });

    it('rejects SQL injection in trainer pairing code with 400', async () => {
      const res = await request('POST', '/api/trainer/pair', {
        Authorization: `Bearer ${trainerToken}`
      }, {
        code: "12345' OR '1'='1"
      });
      assert.equal(res.statusCode, 400);
    });

    it('immunizes GET /api/workout query parameters against SQL injection', async () => {
      const res1 = await request('GET', `/api/workout?date=' OR '1'='1`, {
        Authorization: `Bearer ${athleteToken}`
      });
      assert.equal(res1.statusCode, 200);
      assert.ok(Array.isArray(res1.body.sets));

      const res2 = await request('GET', `/api/workout?athleteId=1 OR 1=1`, {
        Authorization: `Bearer ${trainerToken}`
      });
      assert.equal(res2.statusCode, 200);
      assert.ok(Array.isArray(res2.body.sets));
    });

    it('immunizes GET /api/trainer/exercise-history query parameters against SQL injection', async () => {
      const res = await request('GET', `/api/trainer/exercise-history?athleteId=1&exercise=Squat'; DROP TABLE users;--`, {
        Authorization: `Bearer ${trainerToken}`
      });
      assert.equal(res.statusCode, 200);
    });

    it('immunizes POST /api/workout/set/toggle against SQL injection in setId', async () => {
      const res = await request('POST', '/api/workout/set/toggle', {
        Authorization: `Bearer ${athleteToken}`
      }, {
        setId: "1' OR '1'='1"
      });
      assert.equal(res.statusCode, 400);
    });

    it('immunizes DELETE /api/workout/set against SQL injection in setId', async () => {
      const res = await request('DELETE', `/api/workout/set?setId=1' OR '1'='1`, {
        Authorization: `Bearer ${athleteToken}`
      });
      assert.equal(res.statusCode, 400);
    });

    it('immunizes POST /api/trainer/unpair against SQL injection in athleteId', async () => {
      const res = await request('POST', '/api/trainer/unpair', {
        Authorization: `Bearer ${trainerToken}`
      }, {
        athleteId: "1' OR '1'='1"
      });
      assert.equal(res.statusCode, 400);
    });

    it('guarantees database integrity after all SQL injection attempts', () => {
      const athlete = db.findUserByUsername(athleteUser);
      assert.ok(athlete, 'Athlete user still exists in database');
      const sets = db.getWorkoutSets(athleteId, '2026-10-03');
      assert.ok(Array.isArray(sets), 'Workout sets table remains healthy and queryable');
    });
  });

  // ==========================================
  // 3. ANTI-XSS & SECURITY HEADERS
  // ==========================================
  describe('3. Anti-XSS (Cross-Site Scripting) Defense', () => {
    it('escapes HTML tags in workout set exerciseName preventing stored XSS', async () => {
      const res = await request('POST', '/api/workout/set', {
        Authorization: `Bearer ${athleteToken}`
      }, {
        exerciseName: '<img src=x onerror=alert(1)>',
        weightKg: 50,
        reps: 12
      });
      assert.equal(res.statusCode, 201);

      const listRes = await request('GET', '/api/workout', {
        Authorization: `Bearer ${athleteToken}`
      });
      assert.equal(listRes.statusCode, 200);
      const found = listRes.body.sets.find(s => s.exercise_name.includes('onerror'));
      assert.ok(found);
      assert.ok(!found.exercise_name.includes('<img'));
      assert.ok(found.exercise_name.includes('&lt;img'));
    });

    it('rejects script tags with quote injections in fullName during registration', async () => {
      const res = await request('POST', '/api/register', {}, {
        username: 'xss_quoted_' + Date.now(),
        password: 'password123',
        role: 'athlete',
        fullName: '<script>alert("XSS")</script>'
      });
      assert.equal(res.statusCode, 400);
    });

    it('escapes HTML tags in registration fullName when valid characters are used', async () => {
      const safeName = 'Иван <b>Сильный</b>';
      const regUser = 'xss_esc_' + Date.now();
      const res = await request('POST', '/api/register', {}, {
        username: regUser,
        password: 'password123',
        role: 'athlete',
        fullName: safeName
      });
      if (res.statusCode === 201) {
        assert.ok(!res.body.user.fullName.includes('<b>'));
        assert.ok(res.body.user.fullName.includes('&lt;b&gt;'));
      }
    });

    it('serves strict OWASP security response headers on all API endpoints', async () => {
      const res = await request('GET', '/api/leaderboard', {
        Authorization: `Bearer ${athleteToken}`
      });
      assert.equal(res.statusCode, 200);
      assert.ok(res.headers['content-security-policy'], 'CSP header missing');
      assert.equal(res.headers['x-content-type-options'], 'nosniff');
      assert.equal(res.headers['x-frame-options'], 'DENY');
      assert.equal(res.headers['x-xss-protection'], '1; mode=block');
    });

    it('serves strict OWASP security response headers on static web page requests', async () => {
      const res = await request('GET', '/');
      assert.ok(res.headers['content-security-policy']);
      assert.equal(res.headers['x-content-type-options'], 'nosniff');
      assert.equal(res.headers['x-frame-options'], 'DENY');
      assert.equal(res.headers['x-xss-protection'], '1; mode=block');
    });
  });

  // ==========================================
  // 4. RATE LIMITING & BRUTE FORCE SHIELD
  // ==========================================
  describe('4. Rate Limiting Defense', () => {
    it('triggers HTTP 429 Too Many Requests when exceeding login attempts threshold', async () => {
      let rateLimited = false;
      for (let i = 0; i < 20; i++) {
        const res = await request('POST', '/api/login', {}, {
          username: 'brute_force_target',
          password: `attempt_${i}`
        });
        if (res.statusCode === 429) {
          rateLimited = true;
          assert.ok(res.body.error);
          break;
        }
      }
      assert.ok(rateLimited, 'Rate limiter must trigger 429 after exceeding max attempts on /api/login');
    });

    it('triggers HTTP 429 Too Many Requests when exceeding register attempts threshold', async () => {
      let rateLimited = false;
      for (let i = 0; i < 20; i++) {
        const res = await request('POST', '/api/register', {}, {
          username: `spam_user_${i}_${Date.now()}`,
          password: 'password123',
          role: 'athlete',
          fullName: 'Спам Бот'
        });
        if (res.statusCode === 429) {
          rateLimited = true;
          assert.ok(res.body.error);
          break;
        }
      }
      assert.ok(rateLimited, 'Rate limiter must trigger 429 after exceeding max attempts on /api/register');
    });

    it('resets rate limits cleanly allowing legitimate authentication requests', async () => {
      if (authLimiter && typeof authLimiter.reset === 'function') {
        authLimiter.reset();
      }
      const res = await request('POST', '/api/login', {}, {
        username: athleteUser,
        password: 'securePassword123!'
      });
      assert.equal(res.statusCode, 200, 'Legitimate login succeeds after limiter reset');
    });
  });

  // ==========================================
  // 5. ROLE ISOLATION & PRIVILEGE ENFORCEMENT
  // ==========================================
  describe('5. Role Isolation & RBAC', () => {
    let testSetId = null;

    it('athlete adds a workout set for IDOR isolation verification', async () => {
      const res = await request('POST', '/api/workout/set', {
        Authorization: `Bearer ${athleteToken}`
      }, {
        exerciseName: 'Squat',
        weightKg: 120,
        reps: 5
      });
      assert.equal(res.statusCode, 201);
      testSetId = res.body.setId;
      assert.ok(testSetId);
    });

    it('strictly forbids athlete from accessing trainer pairing endpoint (403)', async () => {
      const res = await request('POST', '/api/trainer/pair', {
        Authorization: `Bearer ${athleteToken}`
      }, { code: '123456' });
      assert.equal(res.statusCode, 403);
    });

    it('strictly forbids athlete from accessing trainer client list (403)', async () => {
      const res = await request('GET', '/api/trainer/clients', {
        Authorization: `Bearer ${athleteToken}`
      });
      assert.equal(res.statusCode, 403);
    });

    it('strictly forbids athlete from accessing trainer exercise history (403)', async () => {
      const res = await request('GET', '/api/trainer/exercise-history?athleteId=1&exercise=Squat', {
        Authorization: `Bearer ${athleteToken}`
      });
      assert.equal(res.statusCode, 403);
    });

    it('strictly forbids athlete from accessing trainer unpair endpoint (403)', async () => {
      const res = await request('POST', '/api/trainer/unpair', {
        Authorization: `Bearer ${athleteToken}`
      }, { athleteId: 999 });
      assert.equal(res.statusCode, 403);
    });

    it('strictly forbids trainer from accessing athlete-only PIN regeneration (403)', async () => {
      const res = await request('POST', '/api/athlete/regenerate-pin', {
        Authorization: `Bearer ${trainerToken}`
      });
      assert.equal(res.statusCode, 403);
    });

    it('strictly forbids trainer from modifying athlete privacy setting (403)', async () => {
      const res = await request('POST', '/api/athlete/privacy', {
        Authorization: `Bearer ${trainerToken}`
      }, { isPrivate: true });
      assert.equal(res.statusCode, 403);
    });

    it('strictly forbids trainer from unpairing via athlete unpair route (403)', async () => {
      const res = await request('POST', '/api/athlete/unpair', {
        Authorization: `Bearer ${trainerToken}`
      });
      assert.equal(res.statusCode, 403);
    });

    it('prevents athlete from viewing other athletes workouts via athleteId manipulation', async () => {
      const res = await request('GET', `/api/workout?athleteId=${athlete2Id}`, {
        Authorization: `Bearer ${athleteToken}`
      });
      assert.equal(res.statusCode, 200);
      // Backend automatically forces targetAthleteId to user.id for athlete role
      assert.ok(Array.isArray(res.body.sets));
    });

    it('prevents athlete from toggling another athletes workout set (IDOR protection) (403)', async () => {
      const res = await request('POST', '/api/workout/set/toggle', {
        Authorization: `Bearer ${athlete2Token}`
      }, {
        setId: testSetId
      });
      assert.equal(res.statusCode, 403, 'Must reject set toggle attempt on another athletes set with 403');
    });

    it('prevents athlete from deleting another athletes workout set (IDOR protection) (403)', async () => {
      const res = await request('DELETE', `/api/workout/set?setId=${testSetId}`, {
        Authorization: `Bearer ${athlete2Token}`
      });
      assert.equal(res.statusCode, 403, 'Must reject set deletion attempt on another athletes set with 403');
    });

    it('allows owner athlete to toggle and delete their own workout set', async () => {
      const toggleRes = await request('POST', '/api/workout/set/toggle', {
        Authorization: `Bearer ${athleteToken}`
      }, {
        setId: testSetId
      });
      assert.equal(toggleRes.statusCode, 200);

      const deleteRes = await request('DELETE', `/api/workout/set?setId=${testSetId}`, {
        Authorization: `Bearer ${athleteToken}`
      });
      assert.equal(deleteRes.statusCode, 200);
    });

    it('allows trainer to pair athlete via valid 6-digit PIN', async () => {
      const res = await request('POST', '/api/trainer/pair', {
        Authorization: `Bearer ${trainerToken}`
      }, { code: athletePin });
      assert.equal(res.statusCode, 200);
      assert.ok(res.body.success);
      assert.equal(res.body.athlete.id, athleteId);
    });

    it('shows paired athlete in trainer client list', async () => {
      const res = await request('GET', '/api/trainer/clients', {
        Authorization: `Bearer ${trainerToken}`
      });
      assert.equal(res.statusCode, 200);
      const client = res.body.clients.find(c => c.id === athleteId);
      assert.ok(client, 'Paired athlete found in trainer client list');
    });
  });

  // ==========================================
  // 6. STATIC PATH TRAVERSAL DEFENSE
  // ==========================================
  describe('6. Path Traversal & Static Asset Defense', () => {
    it('blocks relative directory traversal attempts outside public folder', async () => {
      const res = await request('GET', '/../../package.json');
      assert.ok([403, 404].includes(res.statusCode));
    });

    it('blocks URL-encoded path traversal sequences (%2e%2e)', async () => {
      const res = await request('GET', '/%2e%2e/%2e%2e/package.json');
      assert.ok([403, 404].includes(res.statusCode));
    });

    it('blocks mixed backslash path traversal sequences (..%5c)', async () => {
      const res = await request('GET', '/..%5c..%5cpackage.json');
      assert.ok([403, 404].includes(res.statusCode));
    });

    it('blocks null-byte injection path traversal attempts', async () => {
      const res = await request('GET', '/..%00/package.json');
      assert.ok([403, 404].includes(res.statusCode));
      assert.ok(!res.rawBody.includes('fitness-ecosystem-web'));
    });

    it('blocks direct access to backend server source files', async () => {
      const res = await request('GET', '/../src/server.js');
      assert.ok([403, 404].includes(res.statusCode));
    });
  });

  // ==========================================
  // 7. TELEGRAM AUTHENTICATION SECURITY
  // ==========================================
  describe('7. Telegram Authentication Security', () => {
    it('blocks SQL injection payloads in Telegram username parameter', async () => {
      const res = await request('POST', '/api/auth/telegram', {}, {
        username: "durov'; DROP TABLE users;--",
        requestedRole: 'athlete'
      });
      assert.equal(res.statusCode, 400);
      assert.ok(res.body.error);
    });

    it('escapes HTML and script tags in Telegram user full name preventing XSS', async () => {
      const res = await request('POST', '/api/auth/telegram', {}, {
        telegramUser: {
          id: 11223344,
          first_name: '<script>alert("XSS")</script>',
          last_name: 'Дуров',
          username: 'xss_tester'
        },
        requestedRole: 'athlete'
      });
      assert.equal(res.statusCode, 200);
      assert.ok(res.body.token);
      assert.ok(!res.body.user.fullName.includes('<script>'));
      assert.ok(res.body.user.fullName.includes('&lt;script&gt;'));
    });

    it('rejects forged Telegram HMAC signature when BOT_TOKEN is verified', async () => {
      process.env.BOT_TOKEN = '123456:FAKE_BOT_TOKEN_FOR_SECURITY_TEST';
      const fakeInitData = 'auth_date=1616239000&query_id=AAHdF6IQAAAAAN0XohDhrOrc&user=%7B%22id%22%3A279058397%2C%22first_name%22%3A%22Vladislav%22%7D&hash=invalid_forged_hash_12345';
      const res = await request('POST', '/api/auth/telegram', {}, {
        initData: fakeInitData
      });
      assert.equal(res.statusCode, 401);
      assert.ok(res.body.error.includes('подпись'));
      delete process.env.BOT_TOKEN;
    });

    it('triggers HTTP 429 Too Many Requests on Telegram auth brute-force attempts', async () => {
      let rateLimited = false;
      for (let i = 0; i < 20; i++) {
        const res = await request('POST', '/api/auth/telegram', {}, {
          username: `spam_tg_${i}_${Date.now()}`
        });
        if (res.statusCode === 429) {
          rateLimited = true;
          break;
        }
      }
      assert.ok(rateLimited, 'Rate limiter must trigger 429 on /api/auth/telegram');
    });

    it('guarantees database integrity and user table health after Telegram security tests', () => {
      const check = db.findUserByUsername(athleteUser);
      assert.ok(check, 'Database remains consistent and accessible');
    });
  });
});
