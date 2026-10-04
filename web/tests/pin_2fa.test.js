const { describe, it, before, after } = require('node:test');
const assert = require('node:assert/strict');
const http = require('node:http');
const path = require('node:path');
const { server, db } = require('../src/server');

let baseUrl;
let athleteToken = null;
let athleteId = null;
let athletePin = null;

let trainerToken = null;
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

describe('PIN 5-Min TTL & Telegram 2FA Authentication Test Suite', () => {
  before(async () => {
    await new Promise(resolve => {
      server.listen(0, () => {
        baseUrl = `http://127.0.0.1:${server.address().port}`;
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

  after(() => {
    server.close();
    db.close();
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

    // Code is now consumed, second attempt must fail with 404/400
    const secondPair = await request('POST', '/api/trainer/pair', {
      Authorization: `Bearer ${trainerToken}`
    }, {
      code: athletePin
    });
    assert.notEqual(secondPair.statusCode, 200);
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
});
