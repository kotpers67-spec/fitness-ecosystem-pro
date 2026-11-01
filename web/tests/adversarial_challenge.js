/**
 * Fitness Ecosystem Pro - Empirical Adversarial Stress Test Harness
 * Author: Challenger 2 (Web & Security Focus)
 * Target: http://localhost:3000
 */

const http = require('node:http');
const assert = require('node:assert/strict');
const path = require('node:path');
const { DatabaseSync } = require('node:sqlite');

const BASE_URL = process.env.BASE_URL || 'http://localhost:3000';
const parsedUrl = new URL(BASE_URL);
const HOST = parsedUrl.hostname;
const PORT = parseInt(parsedUrl.port || '3000', 10);

function sendRequest(options, body = null) {
  return new Promise((resolve, reject) => {
    const reqOpts = {
      hostname: HOST,
      port: PORT,
      path: options.path,
      method: options.method || 'GET',
      headers: Object.assign({}, options.headers || {})
    };

    let postData = null;
    if (body !== null && body !== undefined) {
      postData = typeof body === 'string' ? body : JSON.stringify(body);
      if (!reqOpts.headers['Content-Type']) {
        reqOpts.headers['Content-Type'] = 'application/json';
      }
      reqOpts.headers['Content-Length'] = Buffer.byteLength(postData);
    }

    const req = http.request(reqOpts, (res) => {
      let rawData = '';
      res.setEncoding('utf8');
      res.on('data', chunk => rawData += chunk);
      res.on('end', () => {
        let json = null;
        try {
          json = JSON.parse(rawData);
        } catch (_) {}
        resolve({
          status: res.statusCode,
          headers: res.headers,
          body: rawData,
          json
        });
      });
    });

    req.on('error', reject);

    if (postData) {
      req.write(postData);
    }
    req.end();
  });
}

function sendRawPathRequest(rawPath) {
  return new Promise((resolve, reject) => {
    const req = http.request({
      hostname: HOST,
      port: PORT,
      path: rawPath,
      method: 'GET'
    }, (res) => {
      let rawData = '';
      res.setEncoding('utf8');
      res.on('data', chunk => rawData += chunk);
      res.on('end', () => {
        resolve({
          status: res.statusCode,
          headers: res.headers,
          body: rawData
        });
      });
    });
    req.on('error', reject);
    req.end();
  });
}

async function runAdversarialAudit() {
  const results = {
    total: 0,
    passed: 0,
    failed: 0,
    failures: []
  };

  function test(name, fn) {
    results.total++;
    try {
      fn();
      results.passed++;
      console.log(`  ✔ [PASS] ${name}`);
    } catch (err) {
      results.failed++;
      results.failures.push({ name, error: err.message });
      console.error(`  ✖ [FAIL] ${name}: ${err.message}`);
    }
  }

  async function testAsync(name, fn) {
    results.total++;
    try {
      await fn();
      results.passed++;
      console.log(`  ✔ [PASS] ${name}`);
    } catch (err) {
      results.failed++;
      results.failures.push({ name, error: err.message });
      console.error(`  ✖ [FAIL] ${name}: ${err.message}`);
    }
  }

  console.log(`\n======================================================`);
  console.log(`ADVERSARIAL STRESS TEST SUITE: FITNESS ECOSYSTEM PRO`);
  console.log(`Target: ${BASE_URL}`);
  console.log(`Timestamp: ${new Date().toISOString()}`);
  console.log(`======================================================\n`);

  // SECTION 1: Static SPA Delivery & Design Specs
  console.log('--- 1. Static SPA Delivery & Swiss Clean UI Specifications ---');

  await testAsync('SPA root serves index.html (200 OK)', async () => {
    const res = await sendRequest({ path: '/' });
    assert.equal(res.status, 200);
    assert.match(res.headers['content-type'], /text\/html/);
    assert.ok(res.body.includes('Fitness Ecosystem Pro'));
    assert.ok(res.body.includes('bento-grid'));
  });

  await testAsync('styles.css enforces #0d0d0d and Anti-Overlap Guard (200 OK)', async () => {
    const res = await sendRequest({ path: '/styles.css' });
    assert.equal(res.status, 200);
    assert.match(res.headers['content-type'], /text\/css/);
    assert.ok(res.body.includes('--bg-primary: #0d0d0d;'), 'Missing #0d0d0d background');
    assert.ok(res.body.includes('min-width: 0;'), 'Missing Anti-Overlap min-width: 0');
    assert.ok(res.body.includes('tabular-nums'), 'Missing tabular-nums rule');
  });

  await testAsync('app.js serves clean SPA logic (200 OK)', async () => {
    const res = await sendRequest({ path: '/app.js' });
    assert.equal(res.status, 200);
    assert.match(res.headers['content-type'], /javascript/);
    assert.ok(res.body.includes('/api/leaderboard'));
  });

  await testAsync('qr.js serves vector QR generator (200 OK)', async () => {
    const res = await sendRequest({ path: '/qr.js' });
    assert.equal(res.status, 200);
    assert.match(res.headers['content-type'], /javascript/);
    assert.ok(res.body.includes('generateQrSvg'));
  });

  test('qr.js produces valid vector SVG with correct attributes', () => {
    const SwissQr = require(path.join(__dirname, '../src/public/qr.js'));
    const svg = SwissQr.generateQrSvg('654321', { size: 256, color: '#ffffff' });
    assert.ok(svg.startsWith('<svg xmlns="http://www.w3.org/2000/svg"'));
    assert.ok(svg.includes('width="256"'));
    assert.ok(svg.includes('height="256"'));
    assert.ok(svg.includes('shape-rendering="crispEdges"'));
    assert.ok(svg.includes('fill="#ffffff"'));
    assert.ok(svg.includes('path d='));
  });

  await testAsync('Non-existent static asset returns 404 Not Found', async () => {
    const res = await sendRequest({ path: '/ghost-file-404.css' });
    assert.equal(res.status, 404);
  });

  // SECTION 2: Path Traversal & Directory Traversal Attacks
  console.log('\n--- 2. Path Traversal & File Disclosure Attacks ---');

  await testAsync('Raw path traversal /../../package.json is rejected (403/404)', async () => {
    const res = await sendRawPathRequest('/../../package.json');
    assert.ok([400, 403, 404].includes(res.status));
    assert.ok(!res.body.includes('"name": "fitness-ecosystem-pro"'));
  });

  await testAsync('URL-encoded path traversal /%2e%2e/%2e%2e/src/server.js is rejected (403/404)', async () => {
    const res = await sendRawPathRequest('/%2e%2e/%2e%2e/src/server.js');
    assert.ok([400, 403, 404].includes(res.status));
    assert.ok(!res.body.includes('Fitness Ecosystem Pro - Web Server'));
  });

  await testAsync('Encoded slash traversal /..%2f..%2fsrc/db.js is rejected (403/404)', async () => {
    const res = await sendRawPathRequest('/..%2f..%2fsrc/db.js');
    assert.ok([400, 403, 404].includes(res.status));
    assert.ok(!res.body.includes('class AppDatabase'));
  });

  await testAsync('Windows backslash traversal /..%5c..%5csrc/server.js is rejected (403/404)', async () => {
    const res = await sendRawPathRequest('/..%5c..%5csrc/server.js');
    assert.ok([400, 403, 404].includes(res.status));
    assert.ok(!res.body.includes('require('));
  });

  await testAsync('Null byte injection /..%00/package.json is rejected (403/404)', async () => {
    const res = await sendRawPathRequest('/..%00/package.json');
    assert.ok([400, 403, 404].includes(res.status));
    assert.ok(!res.body.includes('"name": "fitness-ecosystem-pro"'));
  });

  await testAsync('Direct database file access /fitness.sqlite is blocked', async () => {
    const res = await sendRawPathRequest('/fitness.sqlite');
    assert.ok([400, 403, 404].includes(res.status));
    assert.ok(!res.body.startsWith('SQLite format 3'));
  });

  // SECTION 3: SQL Injection Vectors
  console.log('\n--- 3. SQL Injection Resilience Across Endpoints ---');

  const sqliPayloads = [
    "' OR 1=1--",
    "admin'--",
    "' UNION SELECT 1, 'admin', 'password'--",
    "'; DROP TABLE users;--",
    "1' OR '1'='1"
  ];

  for (const payload of sqliPayloads) {
    await testAsync(`SQLi in /api/login username [${payload.substring(0, 15)}] is blocked`, async () => {
      const res = await sendRequest({
        path: '/api/login',
        method: 'POST'
      }, { username: payload, password: 'password123' });
      assert.ok([400, 401].includes(res.status), `Unexpected status ${res.status}`);
      assert.ok(!res.json?.token, 'SQLi must not generate a session token');
    });
  }

  await testAsync('SQLi in /api/login password does not bypass auth', async () => {
    const res = await sendRequest({
      path: '/api/login',
      method: 'POST'
    }, { username: 'testuser', password: "' OR 1=1--" });
    assert.equal(res.status, 401);
  });

  await testAsync('SQLi in /api/register username is rejected (400 Bad Request)', async () => {
    const res = await sendRequest({
      path: '/api/register',
      method: 'POST'
    }, {
      username: "hack'; DROP TABLE users;--",
      password: "Password123!",
      role: "athlete",
      fullName: "Hacker User"
    });
    assert.equal(res.status, 400);
  });

  await testAsync('SQLi in /api/register fullName is rejected (400 Bad Request)', async () => {
    const res = await sendRequest({
      path: '/api/register',
      method: 'POST'
    }, {
      username: `safeuser_${Date.now()}`,
      password: "Password123!",
      role: "athlete",
      fullName: "User' UNION SELECT 1,2,3--"
    });
    assert.equal(res.status, 400);
  });

  await testAsync('SQLi in /api/trainer/pair pairing code is rejected (400 Bad Request)', async () => {
    const res = await sendRequest({
      path: '/api/trainer/pair',
      method: 'POST'
    }, { code: "' OR 1=1--" });
    assert.ok([400, 401].includes(res.status));
  });

  // SECTION 4: Cross-Site Scripting (XSS) & OWASP Security Headers
  console.log('\n--- 4. Cross-Site Scripting (XSS) & Security Headers ---');

  await testAsync('XSS <script> payload in registration fullName is rejected (400)', async () => {
    const res = await sendRequest({
      path: '/api/register',
      method: 'POST'
    }, {
      username: `xssuser_${Date.now()}`,
      password: "Password123!",
      role: "athlete",
      fullName: '<script>alert("XSS")</script>'
    });
    assert.equal(res.status, 400);
  });

  await testAsync('HTML tags in registration fullName are safely encoded / sanitized', async () => {
    const res = await sendRequest({
      path: '/api/register',
      method: 'POST'
    }, {
      username: `xssimg_${Date.now()}`,
      password: "Password123!",
      role: "athlete",
      fullName: 'Иван <b>Сильный</b>'
    });
    assert.ok([201, 400].includes(res.status));
    if (res.status === 201) {
      assert.ok(!res.json.user.fullName.includes('<b>'));
      assert.ok(res.json.user.fullName.includes('&lt;b&gt;'));
    }
  });

  await testAsync('API responses include strict OWASP security headers', async () => {
    const res = await sendRequest({ path: '/api/leaderboard' });
    assert.equal(res.headers['x-content-type-options'], 'nosniff');
    assert.equal(res.headers['x-frame-options'], 'DENY');
    assert.ok(res.headers['content-security-policy'], 'Missing CSP header');
  });

  // SECTION 5: IDOR (Insecure Direct Object Reference)
  console.log('\n--- 5. IDOR (Insecure Direct Object Reference) Protection ---');

  const ts = Date.now();
  let athAToken, athBToken, workoutSetId;

  await testAsync('Setup: Register Athlete Alpha and Athlete Beta', async () => {
    const resA = await sendRequest({
      path: '/api/register',
      method: 'POST'
    }, {
      username: `adv_ath_a_${ts}`,
      password: 'Password123!',
      role: 'athlete',
      fullName: 'Athlete Alpha'
    });
    assert.equal(resA.status, 201);
    athAToken = resA.json.token;

    const resB = await sendRequest({
      path: '/api/register',
      method: 'POST'
    }, {
      username: `adv_ath_b_${ts}`,
      password: 'Password123!',
      role: 'athlete',
      fullName: 'Athlete Beta'
    });
    assert.equal(resB.status, 201);
    athBToken = resB.json.token;
  });

  await testAsync('Athlete Alpha logs a workout set', async () => {
    const res = await sendRequest({
      path: '/api/workout/set',
      method: 'POST',
      headers: { 'Authorization': `Bearer ${athAToken}` }
    }, {
      exerciseName: 'Bench Press',
      weightKg: 100,
      reps: 5,
      rpe: 8
    });
    assert.equal(res.status, 201);
    workoutSetId = res.json.setId;
    assert.ok(workoutSetId > 0, `Expected positive setId, got ${workoutSetId}`);
  });

  await testAsync('Athlete Beta attempts IDOR toggle on Alpha set -> 403 Forbidden', async () => {
    const res = await sendRequest({
      path: '/api/workout/set/toggle',
      method: 'POST',
      headers: { 'Authorization': `Bearer ${athBToken}` }
    }, {
      setId: workoutSetId,
      isCompleted: 1
    });
    assert.equal(res.status, 403);
  });

  await testAsync('Athlete Beta attempts IDOR deletion of Alpha set -> 403 Forbidden', async () => {
    const res = await sendRequest({
      path: '/api/workout/set',
      method: 'DELETE',
      headers: { 'Authorization': `Bearer ${athBToken}` }
    }, {
      setId: workoutSetId
    });
    assert.equal(res.status, 403);
  });

  await testAsync('Alpha workout set remains unmutated after hostile IDOR attempts', async () => {
    const today = new Date().toISOString().split('T')[0];
    const res = await sendRequest({
      path: `/api/workout?date=${today}`,
      method: 'GET',
      headers: { 'Authorization': `Bearer ${athAToken}` }
    });
    assert.equal(res.status, 200);
    const set = res.json.sets.find(s => s.id === workoutSetId);
    assert.ok(set, 'Workout set was erroneously deleted by IDOR');
    assert.equal(set.is_completed, 1, 'Workout set status was tampered with');
  });

  await testAsync('Owner Athlete Alpha can toggle their own workout set (200 OK)', async () => {
    const res = await sendRequest({
      path: '/api/workout/set/toggle',
      method: 'POST',
      headers: { 'Authorization': `Bearer ${athAToken}` }
    }, {
      setId: workoutSetId,
      isCompleted: 0
    });
    assert.equal(res.status, 200);
  });

  // SECTION 6: Role Boundaries & Privilege Separation
  console.log('\n--- 6. Role Boundaries & Privilege Separation ---');

  let trainerToken;
  await testAsync('Setup: Register Trainer Gamma', async () => {
    const res = await sendRequest({
      path: '/api/register',
      method: 'POST'
    }, {
      username: `adv_trn_g_${ts}`,
      password: 'Password123!',
      role: 'trainer',
      fullName: 'Trainer Gamma'
    });
    assert.equal(res.status, 201);
    trainerToken = res.json.token;
  });

  await testAsync('Athlete calling /api/trainer/pair is rejected with 403', async () => {
    const res = await sendRequest({
      path: '/api/trainer/pair',
      method: 'POST',
      headers: { 'Authorization': `Bearer ${athAToken}` }
    }, { code: '123456' });
    assert.equal(res.status, 403);
  });

  await testAsync('Athlete calling /api/trainer/clients is rejected with 403', async () => {
    const res = await sendRequest({
      path: '/api/trainer/clients',
      method: 'GET',
      headers: { 'Authorization': `Bearer ${athAToken}` }
    });
    assert.equal(res.status, 403);
  });

  await testAsync('Athlete calling /api/trainer/unpair is rejected with 403', async () => {
    const res = await sendRequest({
      path: '/api/trainer/unpair',
      method: 'POST',
      headers: { 'Authorization': `Bearer ${athAToken}` }
    }, { athleteId: 999 });
    assert.equal(res.status, 403);
  });

  await testAsync('Trainer calling /api/athlete/regenerate-pin is rejected with 403', async () => {
    const res = await sendRequest({
      path: '/api/athlete/regenerate-pin',
      method: 'POST',
      headers: { 'Authorization': `Bearer ${trainerToken}` }
    });
    assert.equal(res.status, 403);
  });

  await testAsync('Trainer calling /api/athlete/unpair is rejected with 403', async () => {
    const res = await sendRequest({
      path: '/api/athlete/unpair',
      method: 'POST',
      headers: { 'Authorization': `Bearer ${trainerToken}` }
    });
    assert.equal(res.status, 403);
  });

  await testAsync('Trainer calling /api/athlete/privacy is rejected with 403', async () => {
    const res = await sendRequest({
      path: '/api/athlete/privacy',
      method: 'POST',
      headers: { 'Authorization': `Bearer ${trainerToken}` }
    }, { isPrivate: true });
    assert.equal(res.status, 403);
  });

  await testAsync('Unauthenticated access to /api/me is rejected with 401', async () => {
    const res = await sendRequest({ path: '/api/me' });
    assert.equal(res.status, 401);
  });

  // SECTION 7: Concurrency Stress Test
  console.log('\n--- 7. High Concurrency Stress Test (100 parallel requests) ---');

  await testAsync('50 parallel requests to /api/leaderboard complete successfully', async () => {
    const t0 = Date.now();
    const promises = [];
    for (let i = 0; i < 50; i++) {
      promises.push(sendRequest({ path: '/api/leaderboard' }));
    }
    const responses = await Promise.all(promises);
    const duration = Date.now() - t0;
    const okCount = responses.filter(r => r.status === 200).length;
    assert.equal(okCount, 50, `Expected 50 OK responses, got ${okCount}`);
    console.log(`    ↳ 50 requests handled in ${duration}ms (avg ${(duration / 50).toFixed(1)}ms/req)`);
  });

  await testAsync('50 parallel requests to static / complete successfully', async () => {
    const t0 = Date.now();
    const promises = [];
    for (let i = 0; i < 50; i++) {
      promises.push(sendRequest({ path: '/' }));
    }
    const responses = await Promise.all(promises);
    const duration = Date.now() - t0;
    const okCount = responses.filter(r => r.status === 200).length;
    assert.equal(okCount, 50, `Expected 50 OK responses, got ${okCount}`);
    console.log(`    ↳ 50 static requests handled in ${duration}ms (avg ${(duration / 50).toFixed(1)}ms/req)`);
  });

  // SECTION 8: Zero-Mocks Database Verification
  console.log('\n--- 8. Zero-Mocks SQLite Integrity Verification ---');

  test('fitness.sqlite has 0 hardcoded mock users', () => {
    const db = new DatabaseSync(path.join(__dirname, '../fitness.sqlite'));
    const mockNames = [
      'Максим Громов',
      'Елена Соколова',
      'Дмитрий Воронов',
      'Ольга Морозова',
      'Mock Athlete',
      'Dummy Trainer',
      'John Doe'
    ];
    for (const name of mockNames) {
      const row = db.prepare('SELECT COUNT(*) as c FROM users WHERE full_name LIKE ?').get(`%${name}%`);
      assert.equal(row.c, 0, `Found mock user: ${name}`);
    }
  });

  test('Database schema integrity: all tables exist and intact', () => {
    const db = new DatabaseSync(path.join(__dirname, '../fitness.sqlite'));
    const tables = db.prepare("SELECT name FROM sqlite_master WHERE type='table'").all().map(r => r.name);
    assert.ok(tables.includes('users'), 'Table users missing');
    assert.ok(tables.includes('auth_tokens'), 'Table auth_tokens missing');
    assert.ok(tables.includes('trainer_clients'), 'Table trainer_clients missing');
    assert.ok(tables.includes('workout_sessions'), 'Table workout_sessions missing');
    assert.ok(tables.includes('workout_sets'), 'Table workout_sets missing');
  });

  // SECTION 9: Rate Limiting Boundary (HTTP 429) (Executed Last)
  console.log('\n--- 9. Rate Limiting Boundary (HTTP 429) ---');

  await testAsync('Rapid auth attempts trigger HTTP 429 Too Many Requests', async () => {
    let triggered429 = false;
    for (let i = 0; i < 20; i++) {
      const res = await sendRequest({
        path: '/api/login',
        method: 'POST'
      }, {
        username: `ratelimit_probe_${Date.now()}_${i}`,
        password: 'wrong_password'
      });
      if (res.status === 429) {
        triggered429 = true;
        assert.ok(res.body.includes('Слишком много попыток входа'), 'Expected rate limiter message');
        break;
      }
    }
    assert.ok(triggered429, 'Rate limiter did not trigger HTTP 429 within 20 attempts');
  });

  console.log(`\n======================================================`);
  console.log(`ADVERSARIAL STRESS TEST SUMMARY`);
  console.log(`Total: ${results.total}`);
  console.log(`Passed: ${results.passed}`);
  console.log(`Failed: ${results.failed}`);
  console.log(`======================================================\n`);

  if (results.failed > 0) {
    console.error('FAILED TESTS:');
    for (const f of results.failures) {
      console.error(`- ${f.name}: ${f.error}`);
    }
    process.exit(1);
  } else {
    console.log('ALL ADVERSARIAL STRESS CHALLENGES PASSED (100%)!');
    process.exit(0);
  }
}

runAdversarialAudit().catch(err => {
  console.error('Fatal test runner error:', err);
  process.exit(1);
});
