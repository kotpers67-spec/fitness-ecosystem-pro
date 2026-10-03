const http = require('node:http');
const { server, db } = require('../../../web/src/server');

function makeRequest(port, method, path, headers = {}, body = null) {
  return new Promise((resolve, reject) => {
    const req = http.request({
      hostname: '127.0.0.1',
      port,
      path,
      method,
      headers: {
        'Content-Type': 'application/json',
        ...headers
      }
    }, res => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        try {
          resolve({ status: res.statusCode, headers: res.headers, body: data ? JSON.parse(data) : null });
        } catch (e) {
          resolve({ status: res.statusCode, headers: res.headers, rawBody: data });
        }
      });
    });
    req.on('error', reject);
    if (body) req.write(typeof body === 'string' ? body : JSON.stringify(body));
    req.end();
  });
}

async function run() {
  server.listen(0, async () => {
    const port = server.address().port;
    console.log(`Test server running on port ${port}`);

    try {
      // 1. Unauthenticated /api/me
      const r1 = await makeRequest(port, 'GET', '/api/me');
      console.log('Unauth /api/me:', r1.status === 401 ? 'PASS (401)' : `FAIL (${r1.status})`);

      // 2. Register athlete with SQLi in username
      const r2 = await makeRequest(port, 'POST', '/api/register', {}, {
        username: "admin' OR '1'='1",
        password: "password123",
        role: "athlete",
        fullName: "Test Athlete"
      });
      console.log('Register SQLi username:', r2.status === 400 ? 'PASS (400)' : `FAIL (${r2.status})`);

      // 3. Register valid athlete
      const athleteUser = 'athlete_' + Date.now();
      const r3 = await makeRequest(port, 'POST', '/api/register', {}, {
        username: athleteUser,
        password: "password123",
        role: "athlete",
        fullName: "Иван Иванов"
      });
      console.log('Register valid athlete:', r3.status === 201 ? 'PASS (201)' : `FAIL (${r3.status})`);
      const athleteToken = r3.body?.token;
      const athletePin = r3.body?.user?.pairingCode;

      // 4. Register valid trainer
      const trainerUser = 'trainer_' + Date.now();
      const r4 = await makeRequest(port, 'POST', '/api/register', {}, {
        username: trainerUser,
        password: "password123",
        role: "trainer",
        fullName: "Тренер Сидоров"
      });
      console.log('Register valid trainer:', r4.status === 201 ? 'PASS (201)' : `FAIL (${r4.status})`);
      const trainerToken = r4.body?.token;

      // 5. Role isolation: Athlete accessing trainer endpoints
      const r5 = await makeRequest(port, 'GET', '/api/trainer/clients', {
        Authorization: `Bearer ${athleteToken}`
      });
      console.log('Athlete accessing trainer clients:', r5.status === 403 ? 'PASS (403)' : `FAIL (${r5.status})`);

      // 6. Role isolation: Trainer accessing athlete endpoints
      const r6 = await makeRequest(port, 'POST', '/api/athlete/regenerate-pin', {
        Authorization: `Bearer ${trainerToken}`
      });
      console.log('Trainer regenerating athlete pin:', r6.status === 403 ? 'PASS (403)' : `FAIL (${r6.status})`);

      // 7. Trainer pairing athlete
      const r7 = await makeRequest(port, 'POST', '/api/trainer/pair', {
        Authorization: `Bearer ${trainerToken}`
      }, { code: athletePin });
      console.log('Trainer pairing athlete:', r7.status === 200 ? 'PASS (200)' : `FAIL (${r7.status})`);

      // 8. XSS check: add workout with script tags
      const r8 = await makeRequest(port, 'POST', '/api/workout/set', {
        Authorization: `Bearer ${athleteToken}`
      }, {
        exerciseName: "<script>alert(1)</script>",
        weightKg: 80,
        reps: 10
      });
      console.log('XSS exercise set submission:', r8.status, r8.body);

      // Fetch workout
      const r9 = await makeRequest(port, 'GET', '/api/workout', {
        Authorization: `Bearer ${athleteToken}`
      });
      console.log('Fetched workout sets:', r9.body?.sets);

      // Check headers
      console.log('CSP header:', r9.headers['content-security-policy'] ? 'PRESENT' : 'MISSING');
      console.log('X-Content-Type-Options:', r9.headers['x-content-type-options'] ? 'PRESENT' : 'MISSING');
      console.log('X-Frame-Options:', r9.headers['x-frame-options'] ? 'PRESENT' : 'MISSING');

    } catch (err) {
      console.error('Error during test execution:', err);
    } finally {
      server.close();
      db.close();
      process.exit(0);
    }
  });
}

run();
