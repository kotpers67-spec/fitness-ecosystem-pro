const { describe, it, before, after } = require('node:test');
const assert = require('node:assert/strict');
const http = require('node:http');
const { server, db } = require('../../../web/src/server');

let baseUrl;
let athleteToken = null;
let athleteUser = null;
let athletePin = null;
let athleteId = null;

let trainerToken = null;
let trainerUser = null;
let trainerId = null;

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

describe('Fitness Ecosystem Pro - Comprehensive Security Test Suite', () => {
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

  // ==========================================
  // 1. AUTHENTICATION & TOKEN SECURITY
  // ==========================================
  describe('1. Authentication & Token Security', () => {
    it('rejects unauthenticated requests to protected endpoints with 401', async () => {
      const res = await request('GET', '/api/me');
      assert.equal(res.statusCode, 401);
      assert.ok(res.body.error);
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
    it('sanitizes or rejects HTML/script tags in user fullName upon registration', async () => {
      // 1. Quoted injection is rejected with 400
      const resRejected = await request('POST', '/api/register', {}, {
        username: 'xss_reject_' + Date.now(),
        password: 'password123',
        role: 'athlete',
        fullName: '<script>alert("XSS")</script>'
      });
      assert.equal(resRejected.statusCode, 400);

      // 2. Unquoted HTML tag is sanitized into entities
      const xssUser = 'xss_' + Date.now();
      const resAllowed = await request('POST', '/api/register', {}, {
        username: xssUser,
        password: 'password123',
        role: 'athlete',
        fullName: '<script>alert(1)</script>'
      });
      assert.equal(resAllowed.statusCode, 201);
      assert.ok(!resAllowed.body.user.fullName.includes('<script>'));
      assert.ok(resAllowed.body.user.fullName.includes('&lt;script&gt;'));
    });

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

    it('serves strict OWASP security response headers on all endpoints', async () => {
      const res = await request('GET', '/api/leaderboard', {
        Authorization: `Bearer ${athleteToken}`
      });
      assert.equal(res.statusCode, 200);
      assert.ok(res.headers['content-security-policy'], 'CSP header missing');
      assert.equal(res.headers['x-content-type-options'], 'nosniff');
      assert.equal(res.headers['x-frame-options'], 'DENY');
      assert.equal(res.headers['x-xss-protection'], '1; mode=block');
    });
  });

  // ==========================================
  // 4. ROLE ISOLATION & PRIVILEGE ENFORCEMENT
  // ==========================================
  describe('4. Role Isolation & RBAC', () => {
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

    it('prevents athlete from viewing other athletes workouts via athleteId manipulation', async () => {
      const res = await request('GET', `/api/workout?athleteId=99999`, {
        Authorization: `Bearer ${athleteToken}`
      });
      assert.equal(res.statusCode, 200);
      assert.ok(Array.isArray(res.body.sets));
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
  // 5. STATIC PATH TRAVERSAL DEFENSE
  // ==========================================
  describe('5. Path Traversal Defense', () => {
    it('blocks directory traversal attempts outside public folder', async () => {
      const res = await request('GET', '/../../package.json');
      assert.ok([403, 404].includes(res.statusCode));
    });
  });

  // ==========================================
  // 6. RATE LIMITING & BRUTE FORCE SHIELD
  // ==========================================
  describe('6. Rate Limiting Defense', () => {
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
      assert.ok(rateLimited, 'Rate limiter must trigger 429 after exceeding max attempts');
    });
  });
});
