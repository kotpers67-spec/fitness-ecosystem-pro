/**
 * Fitness Ecosystem Pro - Cloud Sync Service
 * Seamless bi-directional sync between Web Portal and Android Apps via Google Drive / Google Apps Script.
 * 100% format parity with CloudSecurityManager.kt and GoogleDriveSyncManager.kt (AES-256).
 */

const crypto = require('node:crypto');

const ENDPOINT_URL = 'https://script.google.com/macros/s/AKfycbx6LCbVlxZa-MsWrP0QlNouJwcEcZVsbYHlO4HwHPDDuT6_dp0FDUKTmU_Ax5vg7EP6/exec';
const SECRET_KEY = 'Spirit5449@2011@213@';
const KEY_HASH = crypto.createHash('sha256').update(SECRET_KEY, 'utf8').digest();

function encryptPayload(plainText) {
  const cipher = crypto.createCipheriv('aes-256-ecb', KEY_HASH, null);
  cipher.setAutoPadding(true);
  const encrypted = Buffer.concat([
    cipher.update(Buffer.from(plainText, 'utf8')),
    cipher.final()
  ]);
  return 'ENC:' + encrypted.toString('base64');
}

function decryptPayload(rawText) {
  if (!rawText || !rawText.startsWith('ENC:')) {
    return rawText || '{}';
  }
  try {
    const cipherText = Buffer.from(rawText.slice(4), 'base64');
    const decipher = crypto.createDecipheriv('aes-256-ecb', KEY_HASH, null);
    decipher.setAutoPadding(true);
    const decrypted = Buffer.concat([
      decipher.update(cipherText),
      decipher.final()
    ]);
    return decrypted.toString('utf8');
  } catch (err) {
    console.error('[CloudSync] Decryption error:', err.message);
    return '{}';
  }
}

class CloudSyncService {
  constructor() {
    this.cachedCloudData = null;
    this.lastFetchTime = 0;
    this.cacheTtlMs = 15000; // 15 seconds cache to keep UI instantaneous
  }

  getCloudUrl() {
    return `${ENDPOINT_URL}?key=${encodeURIComponent(SECRET_KEY)}`;
  }

  async fetchCloudData(force = false) {
    const now = Date.now();
    if (!force && this.cachedCloudData && (now - this.lastFetchTime < this.cacheTtlMs)) {
      return this.cachedCloudData;
    }

    try {
      const ac = new AbortController();
      const timeoutId = setTimeout(() => ac.abort(), 20000);
      const res = await fetch(this.getCloudUrl(), {
        signal: ac.signal,
        headers: { 'User-Agent': 'FitnessEcosystemWeb/1.0.5' }
      });
      clearTimeout(timeoutId);

      if (res.ok) {
        const text = await res.text();
        const decrypted = decryptPayload(text);
        const parsed = JSON.parse(decrypted || '{}');
        this.cachedCloudData = parsed;
        this.lastFetchTime = now;
        return parsed;
      }
    } catch (err) {
      console.warn('[CloudSync] Fetch failed, using fallback or cache:', err.message);
      if (this.cachedCloudData) return this.cachedCloudData;
    }

    return { clients: {}, pairing: {}, updates: {} };
  }

  async pushCloudData(rootObj) {
    try {
      rootObj.updatedAt = String(Date.now());
      const json = JSON.stringify(rootObj);
      const encrypted = encryptPayload(json);

      const ac = new AbortController();
      const timeoutId = setTimeout(() => ac.abort(), 25000);
      const res = await fetch(this.getCloudUrl(), {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'User-Agent': 'FitnessEcosystemWeb/1.0.5'
        },
        body: encrypted,
        signal: ac.signal
      });
      clearTimeout(timeoutId);

      this.cachedCloudData = rootObj;
      this.lastFetchTime = Date.now();
      return res.ok;
    } catch (err) {
      console.error('[CloudSync] Push failed:', err.message);
      return false;
    }
  }

  /**
   * Register or update athlete in cloud pairing registry
   */
  async registerAthletePairing(pin, clientUuid, clientName, phone = '', goal = '') {
    const cloud = await this.fetchCloudData(true);
    if (!cloud.pairing) cloud.pairing = {};

    cloud.pairing[pin] = {
      pin,
      clientUuid,
      clientName: clientName || 'Атлет',
      phone: phone || '',
      goal: goal || '',
      restrictions: '',
      notes: '',
      timestamp: Date.now(),
      status: 'PENDING'
    };

    if (!cloud.clients) cloud.clients = {};
    if (!cloud.clients[clientUuid]) {
      cloud.clients[clientUuid] = {
        clientUuid,
        clientName: clientName || 'Атлет',
        phone: phone || '',
        syncTimestamp: Date.now(),
        assignedWorkouts: [],
        anthropometry: []
      };
    }

    await this.pushCloudData(cloud);
    return cloud.pairing[pin];
  }

  /**
   * Find athlete by 6-digit PIN in cloud and mark as PAIRED
   */
  async findAndPairAthlete(pin, coachName = 'Тренер', coachPhone = '') {
    const cloud = await this.fetchCloudData(true);
    if (!cloud.pairing || !cloud.pairing[pin]) {
      return null;
    }

    const pairingEntry = cloud.pairing[pin];
    pairingEntry.status = 'PAIRED';
    pairingEntry.coachName = coachName;
    pairingEntry.coachPhone = coachPhone;
    pairingEntry.pairedTimestamp = Date.now();

    const clientUuid = pairingEntry.clientUuid;
    const clientPayload = (cloud.clients && cloud.clients[clientUuid]) ? cloud.clients[clientUuid] : null;

    await this.pushCloudData(cloud);

    return {
      pin,
      clientUuid,
      clientName: pairingEntry.clientName,
      phone: pairingEntry.phone,
      goal: pairingEntry.goal,
      clientPayload
    };
  }

  /**
   * Unpair athlete from cloud
   */
  async unpairAthlete(pin, clientUuid) {
    const cloud = await this.fetchCloudData(true);
    if (cloud.pairing && cloud.pairing[pin]) {
      cloud.pairing[pin].status = 'UNPAIRED';
      delete cloud.pairing[pin].coachName;
      delete cloud.pairing[pin].coachPhone;
    }
    await this.pushCloudData(cloud);
    return true;
  }

  /**
   * Push assigned workouts from web trainer to cloud client so athlete's mobile app receives them!
   */
  async pushAssignedWorkouts(clientUuid, clientName, workouts) {
    const cloud = await this.fetchCloudData(true);
    if (!cloud.clients) cloud.clients = {};

    const existing = cloud.clients[clientUuid] || {
      clientUuid,
      clientName,
      anthropometry: []
    };

    existing.assignedWorkouts = workouts;
    existing.syncTimestamp = Date.now();
    cloud.clients[clientUuid] = existing;

    return await this.pushCloudData(cloud);
  }

  /**
   * Get Leaderboard containing strictly real athletes from mobile apps + real registered users
   */
  async getCombinedLeaderboard(localAthletes = []) {
    const cloud = await this.fetchCloudData();
    const map = new Map();

    // 1. Add real cloud athletes from mobile apps (Zero-Mocks: real users like Михаил, Александр)
    if (cloud.clients && typeof cloud.clients === 'object') {
      for (const [uuid, entry] of Object.entries(cloud.clients)) {
        if (!entry || !entry.clientName) continue;
        const name = String(entry.clientName).trim();
        // Skip obvious automated test probes
        if (/^(adv_|test_|spam_|xss|ratelimit_|sec_|athlete_1|trainer_1)/i.test(name)) continue;

        const workouts = Array.isArray(entry.assignedWorkouts) ? entry.assignedWorkouts : [];
        const completedWorkouts = workouts.filter(w => w.completed || (w.exercises && w.exercises.some(e => e.sets && e.sets.some(s => s.isCompleted))));
        
        let tonnage = 0;
        for (const w of workouts) {
          if (!w.exercises) continue;
          for (const ex of w.exercises) {
            if (!ex.sets) continue;
            for (const s of ex.sets) {
              if (s.isCompleted) {
                const wt = Number(s.actualWeightKg || s.targetWeightKg || s.weight || 0);
                const reps = Number(s.actualReps || s.targetReps || s.reps || 0);
                tonnage += (wt * reps);
              }
            }
          }
        }

        const workoutsCount = completedWorkouts.length;
        const points = Math.round(workoutsCount * 100 + (tonnage * 0.1));

        map.set(name.toLowerCase(), {
          name,
          workoutsCount,
          totalTonnage: Math.round(tonnage),
          points,
          avatarBase64: entry.avatarBase64 || '',
          source: 'cloud'
        });
      }
    }

    // 2. Add local real athletes
    for (const a of localAthletes) {
      const name = String(a.full_name || a.fullName || a.username).trim();
      if (/^(adv_|test_|spam_|xss|ratelimit_|sec_|athlete_1|trainer_1)/i.test(name)) continue;
      const key = name.toLowerCase();

      const localWorkouts = Number(a.workouts_count || 0);
      const localTonnage = Math.round(Number(a.total_tonnage || 0));
      const localPoints = Number(a.points || 0);

      if (map.has(key)) {
        const item = map.get(key);
        item.workoutsCount = Math.max(item.workoutsCount, localWorkouts);
        item.totalTonnage = Math.max(item.totalTonnage, localTonnage);
        item.points = Math.max(item.points, localPoints);
      } else {
        map.set(key, {
          name,
          workoutsCount: localWorkouts,
          totalTonnage: localTonnage,
          points: localPoints,
          avatarBase64: a.avatar_base64 || '',
          source: 'local'
        });
      }
    }

    const leaderboard = Array.from(map.values());
    leaderboard.sort((a, b) => b.points - a.points || b.totalTonnage - a.totalTonnage);

    return leaderboard.map((item, index) => ({
      rank: index + 1,
      name: item.name,
      workoutsCount: item.workoutsCount,
      totalTonnage: item.totalTonnage,
      points: item.points,
      avatarBase64: item.avatarBase64
    }));
  }
}

const cloudSyncService = new CloudSyncService();
module.exports = { cloudSyncService, CloudSyncService };
