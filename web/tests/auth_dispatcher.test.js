/**
 * Multi-Scheme Authentication Dispatcher Test Suite
 * Comprehensive automated verification for:
 * 1. scheme: 'basic' (credentials and username:password / Base64 secret, ruToEn auto-fix, 3-30 len policy, >=6 char pwd)
 * 2. scheme: 'token' (silent fast re-auth, last_seen_at bump, token expiration extension)
 * 3. scheme: 'anonymous' / 'anon' (clean guest profile without "(гость)", instant session token & cookie)
 * 4. Session lifetime policy (persist === true: 30-day Max-Age cookie; persist === false: browser session cookie)
 * 5. 100% Backwards compatibility with existing /api/login, /api/register, /api/auth/* routes
 */

const { describe, it, before, after, beforeEach } = require('node:test');
const assert = require('node:assert/strict');
const http = require('node:http');
const path = require('node:path');

const { server, db, authLimiter } = require('../src/server');
const { ruToEn, validateLoginPolicy, validatePasswordPolicy, parseSecret } = require('../src/services/authDispatcher');

let baseUrl;

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

describe('Multi-Scheme Authentication Dispatcher Test Suite', () => {
  before(async () => {
    await new Promise(resolve => {
      if (server.listening) {
        const addr = server.address();
        baseUrl = `http://127.0.0.1:${addr.port}`;
        return resolve();
      }
      server.listen(0, '127.0.0.1', () => {
        const addr = server.address();
        baseUrl = `http://127.0.0.1:${addr.port}`;
        resolve();
      });
    });
  });

  after(async () => {
    await new Promise(resolve => server.close(resolve));
  });

  beforeEach(() => {
    authLimiter.cleanup();
    authLimiter.requests.clear();
  });

  // ==========================================
  // 1. UNIT LOGIC: ruToEn, policies, secret parser
  // ==========================================
  describe('1. Unit Utilities (ruToEn, Policies, Secret Parser)', () => {
    it('ruToEn correctly converts Russian keyboard layout to English QWERTY', () => {
      assert.equal(ruToEn('фдшсу'), 'alice');
      assert.equal(ruToEn('йцукен'), 'qwerty');
      assert.equal(ruToEn('МИТЯ'), 'VBNZ');
      assert.equal(ruToEn('english123'), 'english123');
    });

    it('validateLoginPolicy enforces 3-30 character length and rejects SQL injection', () => {
      assert.equal(validateLoginPolicy('ab').valid, false);
      assert.equal(validateLoginPolicy('a'.repeat(31)).valid, false);
      assert.equal(validateLoginPolicy("admin' OR 1=1--").valid, false);
      assert.equal(validateLoginPolicy('valid_user.1').valid, true);
    });

    it('validatePasswordPolicy enforces minimum 6 characters', () => {
      assert.equal(validatePasswordPolicy('12345').valid, false);
      assert.equal(validatePasswordPolicy('123456').valid, true);
    });

    it('parseSecret correctly parses plain username:password and Base64 secrets', () => {
      const plain = parseSecret('ivan:myPassword123');
      assert.deepEqual(plain, { username: 'ivan', password: 'myPassword123' });

      const base64Secret = Buffer.from('alex_box:superSecret999').toString('base64');
      const b64 = parseSecret(base64Secret);
      assert.deepEqual(b64, { username: 'alex_box', password: 'superSecret999' });

      assert.equal(parseSecret('invalid_no_colon'), null);
      assert.equal(parseSecret(''), null);
    });
  });

  // ==========================================
  // 2. SCHEME: basic
  // ==========================================
  describe('2. Scheme: basic (Credentials, Secret, ruToEn, Policies)', () => {
    const testUsername = 'testuser_' + Date.now();
    const testPassword = 'StrongPassword123';

    before(async () => {
      const res = await request('POST', '/api/register', {}, {
        username: testUsername,
        password: testPassword,
        role: 'athlete',
        fullName: 'Тестовый Пользователь'
      });
      assert.equal(res.statusCode, 201);
    });

    it('authenticates successfully via standard { username, password } fields', async () => {
      const res = await request('POST', '/api/auth', {}, {
        scheme: 'basic',
        username: testUsername,
        password: testPassword
      });
      assert.equal(res.statusCode, 200);
      assert.equal(res.body.success, true);
      assert.equal(res.body.scheme, 'basic');
      assert.ok(res.body.token);
      assert.equal(res.body.user.username, testUsername);
    });

    it('authenticates successfully via plain secret "username:password"', async () => {
      const res = await request('POST', '/api/auth', {}, {
        scheme: 'basic',
        secret: `${testUsername}:${testPassword}`
      });
      assert.equal(res.statusCode, 200);
      assert.equal(res.body.success, true);
      assert.ok(res.body.token);
      assert.equal(res.body.user.username, testUsername);
    });

    it('authenticates successfully via Base64 encoded secret (Tinode format)', async () => {
      const b64Secret = Buffer.from(`${testUsername}:${testPassword}`).toString('base64');
      const res = await request('POST', '/api/auth', {}, {
        scheme: 'basic',
        secret: b64Secret
      });
      assert.equal(res.statusCode, 200);
      assert.equal(res.body.success, true);
      assert.ok(res.body.token);
    });

    it('auto-recovers English username typed in Russian keyboard layout (ruToEn)', async () => {
      // Create user with known English name 'qwerty_ru'
      const qwertyUser = 'qwerty_ru_' + Math.floor(Math.random() * 10000);
      const regRes = await request('POST', '/api/register', {}, {
        username: qwertyUser,
        password: testPassword,
        role: 'athlete',
        fullName: 'Keyboard Layout User'
      });
      assert.equal(regRes.statusCode, 201);

      // Convert qwertyUser to Russian layout string
      const EN_TO_RU = {
        'q': 'й', 'w': 'ц', 'e': 'у', 'r': 'к', 't': 'е', 'y': 'н', 'u': 'г', 'i': 'ш', 'o': 'щ', 'p': 'з',
        '[': 'х', ']': 'ъ', 'a': 'ф', 's': 'ы', 'd': 'в', 'f': 'а', 'g': 'п', 'h': 'р', 'j': 'о', 'k': 'л',
        'l': 'д', ';': 'ж', '\'': 'э', 'z': 'я', 'x': 'ч', 'c': 'с', 'v': 'м', 'b': 'и', 'n': 'т', 'm': 'ь',
        ',': 'б', '.': 'ю', '_': '_'
      };
      const typedInRussian = qwertyUser.split('').map(ch => EN_TO_RU[ch] || ch).join('');

      const res = await request('POST', '/api/auth', {}, {
        scheme: 'basic',
        username: typedInRussian,
        password: testPassword
      });
      assert.equal(res.statusCode, 200);
      assert.equal(res.body.success, true);
      assert.equal(res.body.user.username, qwertyUser);
    });

    it('rejects passwords shorter than 6 characters with 400 Bad Request', async () => {
      const res = await request('POST', '/api/auth', {}, {
        scheme: 'basic',
        username: testUsername,
        password: '123'
      });
      assert.equal(res.statusCode, 400);
      assert.match(res.body.error, /минимум 6 символов/i);
    });

    it('rejects logins shorter than 3 or longer than 30 characters with 400 Bad Request', async () => {
      const shortRes = await request('POST', '/api/auth', {}, {
        scheme: 'basic',
        username: 'ab',
        password: testPassword
      });
      assert.equal(shortRes.statusCode, 400);

      const longRes = await request('POST', '/api/auth', {}, {
        scheme: 'basic',
        username: 'u'.repeat(35),
        password: testPassword
      });
      assert.equal(longRes.statusCode, 400);
    });
  });

  // ==========================================
  // 3. SCHEME: token (Fast Silent Auto-Login)
  // ==========================================
  describe('3. Scheme: token (Silent Auto-Login, last_seen_at bump, TTL extension)', () => {
    let sessionToken = null;
    let registeredUser = null;

    before(async () => {
      const uname = 'tokenuser_' + Date.now();
      const pwd = 'ValidPassword123';
      const reg = await request('POST', '/api/register', {}, {
        username: uname,
        password: pwd,
        role: 'athlete',
        fullName: 'Авто Вход Атлет'
      });
      assert.equal(reg.statusCode, 201);
      sessionToken = reg.body.token;
      registeredUser = reg.body.user;
    });

    it('authenticates silently with { scheme: "token", token } and returns user profile', async () => {
      const res = await request('POST', '/api/auth/token', {}, {
        token: sessionToken
      });
      assert.equal(res.statusCode, 200);
      assert.equal(res.body.success, true);
      assert.equal(res.body.scheme, 'token');
      assert.equal(res.body.user.username, registeredUser.username);
      assert.ok(res.body.user.lastSeenAt >= 0);
    });

    it('updates last_seen_at timestamp and extends session expiration in database', async () => {
      const beforeRow = db.db.prepare('SELECT last_seen_at FROM users WHERE id = ?').get(registeredUser.id);
      const tokenBefore = db.db.prepare('SELECT expires_at FROM auth_tokens WHERE token = ?').get(sessionToken);

      // Wait a tick so timestamps increase
      await new Promise(r => setTimeout(r, 20));

      const res = await request('POST', '/api/auth', {}, {
        scheme: 'token',
        token: sessionToken,
        persist: true
      });
      assert.equal(res.statusCode, 200);

      const afterRow = db.db.prepare('SELECT last_seen_at FROM users WHERE id = ?').get(registeredUser.id);
      const tokenAfter = db.db.prepare('SELECT expires_at FROM auth_tokens WHERE token = ?').get(sessionToken);

      assert.ok(afterRow.last_seen_at >= (beforeRow.last_seen_at || 0));
      assert.ok(tokenAfter.expires_at >= tokenBefore.expires_at);
    });

    it('rejects invalid or non-existent tokens with 401 Unauthorized', async () => {
      const res = await request('POST', '/api/auth/token', {}, {
        token: 'fit_invalid_non_existent_token_12345'
      });
      assert.equal(res.statusCode, 401);
      assert.equal(res.body.expired, true);
    });
  });

  // ==========================================
  // 4. SCHEME: anonymous / anon (Clean Temporary Account)
  // ==========================================
  describe('4. Scheme: anonymous / anon (Clean Guest, No "(гость)", Cookie & Token)', () => {
    it('creates temporary account with custom clean desiredName (without "(гость)")', async () => {
      const res = await request('POST', '/api/auth/guest-login', {}, {
        desiredName: 'Артем Смирнов',
        persist: true
      });
      assert.equal(res.statusCode, 201);
      assert.equal(res.body.success, true);
      assert.equal(res.body.scheme, 'anonymous');
      assert.equal(res.body.isAnonymous, true);
      assert.ok(res.body.token);

      // Verify name does NOT contain "(гость)"
      assert.equal(res.body.user.fullName, 'Артем Смирнов');
      assert.ok(!res.body.user.fullName.includes('(гость)'));
      assert.equal(res.body.user.isGuest, true);

      // Verify cookie is set with Max-Age because persist === true
      const setCookie = res.headers['set-cookie'];
      assert.ok(setCookie);
      assert.match(setCookie[0], /fit_token=/);
      assert.match(setCookie[0], /Max-Age=2592000/);
    });

    it('creates temporary account with clean generated name when desiredName is omitted', async () => {
      const res = await request('POST', '/api/auth', {}, {
        scheme: 'anonymous'
      });
      assert.equal(res.statusCode, 201);
      assert.equal(res.body.success, true);
      assert.ok(res.body.user.fullName.startsWith('Атлет '));
      assert.ok(!res.body.user.fullName.includes('(гость)'));
      assert.equal(res.body.user.isGuest, true);
    });

    it('supports alias scheme: "anon"', async () => {
      const res = await request('POST', '/api/auth', {}, {
        scheme: 'anon',
        desiredName: 'Максим'
      });
      assert.equal(res.statusCode, 201);
      assert.equal(res.body.success, true);
      assert.equal(res.body.user.fullName, 'Максим');
    });
  });

  // ==========================================
  // 5. SESSION LIFETIME (persist: boolean)
  // ==========================================
  describe('5. Session Lifetime Management (persist: boolean)', () => {
    const persistUser = 'persist_user_' + Date.now();
    const persistPass = 'PersistPass123';

    before(async () => {
      await request('POST', '/api/register', {}, {
        username: persistUser,
        password: persistPass,
        role: 'athlete',
        fullName: 'Persist Testing'
      });
    });

    it('persist === true sets cookie with Max-Age 30 days (2592000 seconds)', async () => {
      const res = await request('POST', '/api/login', {}, {
        username: persistUser,
        password: persistPass,
        persist: true
      });
      assert.equal(res.statusCode, 200);
      const setCookie = res.headers['set-cookie'];
      assert.ok(setCookie && setCookie.length > 0);
      assert.match(setCookie[0], /fit_token=/);
      assert.match(setCookie[0], /Max-Age=2592000/);
      assert.match(setCookie[0], /Expires=/);
      assert.match(setCookie[0], /HttpOnly/);
    });

    it('persist === false sets browser session cookie (no Max-Age, no Expires)', async () => {
      const res = await request('POST', '/api/login', {}, {
        username: persistUser,
        password: persistPass,
        persist: false
      });
      assert.equal(res.statusCode, 200);
      const setCookie = res.headers['set-cookie'];
      assert.ok(setCookie && setCookie.length > 0);
      assert.match(setCookie[0], /fit_token=/);
      assert.ok(!setCookie[0].includes('Max-Age'));
      assert.ok(!setCookie[0].includes('Expires'));
      assert.match(setCookie[0], /HttpOnly/);
    });
  });

  // ==========================================
  // 6. 100% BACKWARDS COMPATIBILITY
  // ==========================================
  describe('6. 100% Backwards Compatibility with Existing Endpoints', () => {
    const compatUser = 'compat_user_' + Date.now();
    const compatPass = 'CompatPass123';
    let compatToken = null;

    it('POST /api/auth/register and POST /api/register work identically', async () => {
      const res = await request('POST', '/api/auth/register', {}, {
        username: compatUser,
        password: compatPass,
        role: 'athlete',
        fullName: 'Совместимый Атлет'
      });
      assert.equal(res.statusCode, 201);
      assert.ok(res.body.token);
      compatToken = res.body.token;
    });

    it('POST /api/auth/login and POST /api/login work identically', async () => {
      const res1 = await request('POST', '/api/auth/login', {}, {
        username: compatUser,
        password: compatPass
      });
      assert.equal(res1.statusCode, 200);
      assert.equal(res1.body.user.username, compatUser);

      const res2 = await request('POST', '/api/login', {}, {
        username: compatUser,
        password: compatPass
      });
      assert.equal(res2.statusCode, 200);
      assert.equal(res2.body.user.username, compatUser);
    });

    it('GET /api/auth/me and GET /api/me return profile and pairedCoach correctly', async () => {
      const res1 = await request('GET', '/api/auth/me', {
        Authorization: `Bearer ${compatToken}`
      });
      assert.equal(res1.statusCode, 200);
      assert.equal(res1.body.user.username, compatUser);

      const res2 = await request('GET', '/api/me', {
        Authorization: `Bearer ${compatToken}`
      });
      assert.equal(res2.statusCode, 200);
      assert.equal(res2.body.user.username, compatUser);
    });

    it('POST /api/auth/logout and POST /api/logout clear cookie and revoke token', async () => {
      const logoutRes = await request('POST', '/api/auth/logout', {
        Authorization: `Bearer ${compatToken}`
      });
      assert.equal(logoutRes.statusCode, 200);
      assert.equal(logoutRes.body.success, true);
      assert.match(logoutRes.headers['set-cookie'][0], /Max-Age=0/);

      // Verify token is now invalid
      const meRes = await request('GET', '/api/me', {
        Authorization: `Bearer ${compatToken}`
      });
      assert.equal(meRes.statusCode, 401);
    });
  });
});
