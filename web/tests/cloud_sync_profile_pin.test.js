/**
 * Test Suite: Athlete Profile & Unified PIN Cloud Synchronization
 * Validates syncAthleteFromCloud(), FIO, avatarBase64, phone, and unified PIN code sync.
 */

const test = require('node:test');
const assert = require('node:assert');
const path = require('node:path');
const fs = require('node:fs');
const Database = require(path.join(__dirname, '..', 'src', 'db.js'));
const { CloudSyncService } = require(path.join(__dirname, '..', 'src', 'cloudSync.js'));

test('Athlete Profile & Unified PIN Cloud Sync Test Suite', async (t) => {
  const testDbFile = path.join(__dirname, `test_profile_sync_${Date.now()}.sqlite`);
  const db = new Database(testDbFile);
  const cloudSync = new CloudSyncService(db);

  t.after(() => {
    try {
      db.db.close();
      if (fs.existsSync(testDbFile)) fs.unlinkSync(testDbFile);
    } catch (_) {}
  });

  // 1. Create a test athlete user in SQLite with dummy/placeholder info
  const dummyAthleteId = db.createUser(
    'test_ath_sync',
    'hash123',
    'athlete',
    'Миша',
    '+70000000000'
  );
  db.linkTelegram(dummyAthleteId, '999999999', 'spirit5449');
  const dummyAthlete = db.findUserById(dummyAthleteId);

  await t.test('1. syncAthleteFromCloud fetches and applies real FIO, phone, and avatar from cloud', async () => {
    // Mock cloud payload or verify live sync
    const mockCloudData = {
      clients: {
        '244d0d51-4806-42a5-aff2-6494969e5420': {
          clientUuid: '244d0d51-4806-42a5-aff2-6494969e5420',
          fullName: 'Ефимов Михаил Сергеевич',
          phone: '+79373857221',
          avatarBase64: 'data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAP//////////////////////////////////////////////////////////////////////////////////////wgALCAABAAEBAREA/8QAFBABAAAAAAAAAAAAAAAAAAAAAP/aAAgBAQABPxA=',
          restrictions: 'Травма левого плеча'
        }
      },
      pairing: {
        '162085': {
          pin: '162085',
          clientUuid: '244d0d51-4806-42a5-aff2-6494969e5420',
          fullName: 'Ефимов Михаил Сергеевич',
          phone: '+79373857221',
          status: 'PAIRED'
        }
      }
    };

    // Set cache for deterministic test
    cloudSync.cachedCloudData = mockCloudData;
    cloudSync.lastFetchTime = Date.now();

    const athleteBefore = db.findUserById(dummyAthlete.id);
    assert.strictEqual(athleteBefore.full_name, 'Миша');

    const result = await cloudSync.syncAthleteFromCloud(athleteBefore, db);
    assert.strictEqual(result.synced, true);

    const athleteAfter = db.findUserById(dummyAthlete.id);
    assert.strictEqual(athleteAfter.full_name, 'Ефимов Михаил Сергеевич');
    assert.strictEqual(athleteAfter.phone, '+79373857221');
    assert.ok(athleteAfter.avatar_base64 && athleteAfter.avatar_base64.length > 50);
    assert.strictEqual(athleteAfter.pairing_code, '162085');
    assert.strictEqual(athleteAfter.client_uuid, '244d0d51-4806-42a5-aff2-6494969e5420');
  });

  await t.test('2. Unified PIN code parity between Cloud pairing registry and Local athlete user', async () => {
    const user = db.findUserById(dummyAthlete.id);
    assert.strictEqual(user.pairing_code, '162085');

    // Verify when pairing PIN is looked up by PIN, it resolves the updated athlete
    const matched = db.findUserByPairingCode('162085');
    assert.ok(matched);
    assert.strictEqual(matched.id, dummyAthlete.id);
    assert.strictEqual(matched.full_name, 'Ефимов Михаил Сергеевич');
  });

  await t.test('3. syncAthleteFromCloud is idempotent and does not overwrite modified valid custom values unnecessarily', async () => {
    const user = db.findUserById(dummyAthlete.id);
    const result = await cloudSync.syncAthleteFromCloud(user, db);
    assert.strictEqual(result.synced, false); // No redundant DB writes
  });

  await t.test('4. Live Cloud Connectivity & AES-256 Decryption Verification', async () => {
    // Test live fetchCloudData() with real Google Drive endpoint
    const liveCloudSync = new CloudSyncService(db);
    const liveCloud = await liveCloudSync.fetchCloudData(true);
    assert.ok(typeof liveCloud === 'object');
    assert.ok(liveCloud.clients || liveCloud.pairing);
    if (liveCloud.pairing && liveCloud.pairing['162085']) {
      assert.strictEqual(liveCloud.pairing['162085'].pin, '162085');
    }
  });
});
