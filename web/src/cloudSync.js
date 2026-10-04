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
  constructor(db = null) {
    this.db = db;
    this.cachedCloudData = null;
    this.lastFetchTime = 0;
    this.cacheTtlMs = 15000; // 15 seconds cache to keep UI instantaneous
  }

  setDb(db) {
    this.db = db;
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
  async registerAthletePairing(pin, clientUuid, clientName, phone = '', goal = '', avatarBase64 = '', restrictions = '') {
    const cloud = await this.fetchCloudData(true);
    if (!cloud.pairing) cloud.pairing = {};

    const cleanPin = String(pin).replace(/\D/g, '');

    // Invalidate and purge all previous PIN entries for this athlete
    for (const [k, v] of Object.entries(cloud.pairing)) {
      if (v?.clientUuid === clientUuid && k !== cleanPin) {
        delete cloud.pairing[k];
      }
    }

    cloud.pairing[cleanPin] = {
      pin: cleanPin,
      clientUuid,
      clientName: clientName || 'Атлет',
      phone: phone || '',
      goal: goal || '',
      avatarBase64: avatarBase64 || '',
      restrictions: restrictions || '',
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
        avatarBase64: avatarBase64 || '',
        restrictions: restrictions || '',
        syncTimestamp: Date.now(),
        assignedWorkouts: [],
        anthropometry: []
      };
    } else {
      cloud.clients[clientUuid].clientName = clientName || cloud.clients[clientUuid].clientName;
      if (phone) cloud.clients[clientUuid].phone = phone;
      if (avatarBase64) cloud.clients[clientUuid].avatarBase64 = avatarBase64;
      if (restrictions !== undefined && restrictions !== null) cloud.clients[clientUuid].restrictions = restrictions;
    }

    await this.pushCloudData(cloud);
    return cloud.pairing[cleanPin];
  }

  /**
   * Find athlete by 6-digit PIN in cloud and mark as PAIRED
   * Enforces strict 5-minute expiration window.
   */
  async findAndPairAthlete(pin, coachName = 'Тренер', coachPhone = '', coachAvatarBase64 = '') {
    const cloud = await this.fetchCloudData(true);
    if (!cloud.pairing) return null;

    const cleanPin = String(pin).replace(/\D/g, '');
    let pairingEntry = cloud.pairing[cleanPin];
    let foundKey = cleanPin;

    if (!pairingEntry) {
      for (const [key, element] of Object.entries(cloud.pairing)) {
        const digits = key.replace(/\D/g, '');
        const entryPin = String(element?.pin || '').replace(/\D/g, '');
        if (digits === cleanPin || entryPin === cleanPin) {
          pairingEntry = element;
          foundKey = key;
          break;
        }
      }
    }

    if (!pairingEntry) {
      return null;
    }

    // Strict 5-minute expiration check (300,000 ms)
    const PAIRING_CODE_TTL = 5 * 60 * 1000;
    const now = Date.now();
    if (pairingEntry.timestamp && (now - pairingEntry.timestamp > PAIRING_CODE_TTL)) {
      delete cloud.pairing[foundKey];
      await this.pushCloudData(cloud);
      return { error: 'EXPIRED', message: 'Срок действия кода истёк (действует 5 минут). Запросите новый код.' };
    }

    if (pairingEntry.status === 'PAIRED') {
      return { error: 'ALREADY_PAIRED', message: 'Этот код уже был использован для привязки' };
    }

    pairingEntry.status = 'PAIRED';
    pairingEntry.coachName = coachName;
    pairingEntry.coachPhone = coachPhone;
    pairingEntry.coachAvatarBase64 = coachAvatarBase64 || '';
    pairingEntry.pairedTimestamp = Date.now();
    cloud.pairing[foundKey] = pairingEntry;

    const clientUuid = pairingEntry.clientUuid;
    if (clientUuid && cloud.clients && cloud.clients[clientUuid]) {
      cloud.clients[clientUuid].coachName = coachName;
      cloud.clients[clientUuid].coachPhone = coachPhone;
      cloud.clients[clientUuid].coachAvatarBase64 = coachAvatarBase64 || '';
    }

    const clientPayload = (clientUuid && cloud.clients && cloud.clients[clientUuid]) ? cloud.clients[clientUuid] : null;
    const athleteAvatar = pairingEntry.avatarBase64 || clientPayload?.avatarBase64 || '';

    await this.pushCloudData(cloud);

    return {
      pin: cleanPin,
      clientUuid,
      clientName: pairingEntry.clientName,
      phone: pairingEntry.phone,
      goal: pairingEntry.goal,
      avatarBase64: athleteAvatar,
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
   * Check if athlete was paired in Google Drive (e.g. by mobile Trainer Pro app)
   */
  async checkAthletePairingStatus(pin, clientUuid) {
    const cloud = await this.fetchCloudData(true);
    if (!cloud.pairing) return { status: 'NOT_FOUND' };

    let entry = cloud.pairing[pin];
    if (!entry && clientUuid) {
      for (const p of Object.values(cloud.pairing)) {
        if (p && p.clientUuid === clientUuid) {
          entry = p;
          break;
        }
      }
    }

    if (!entry) return { status: 'NOT_FOUND' };

    return {
      status: entry.status || 'PENDING',
      coachName: entry.coachName || '',
      coachPhone: entry.coachPhone || '',
      coachAvatarBase64: entry.coachAvatarBase64 || null
    };
  }

  /**
   * Get athlete assigned workouts from Google Drive cloud
   */
  async getAthleteCloudWorkouts(clientUuid) {
    if (!clientUuid) return [];
    const cloud = await this.fetchCloudData(true);
    if (!cloud.clients || !cloud.clients[clientUuid]) return [];
    const clientEntry = cloud.clients[clientUuid];
    return Array.isArray(clientEntry.assignedWorkouts) ? clientEntry.assignedWorkouts : [];
  }

  /**
   * Push assigned workouts from web trainer to cloud client so athlete's mobile app receives them!
   */
  async pushAssignedWorkouts(clientUuid, clientName, workouts, userId = null, db = null) {
    const cloud = await this.fetchCloudData(true);
    if (!cloud.clients) cloud.clients = {};

    const existing = cloud.clients[clientUuid] || {
      clientUuid,
      clientName,
      anthropometry: []
    };

    existing.assignedWorkouts = workouts;
    existing.syncTimestamp = Date.now();

    // Populate anthropometry history from local db if available
    const resolvedDb = db || this.db;
    let targetUserId = userId;
    if (!targetUserId && resolvedDb && clientUuid) {
      const user = resolvedDb.findUserByClientUuid(clientUuid);
      if (user) targetUserId = user.id;
    }
    if (targetUserId && resolvedDb && typeof resolvedDb.getAnthropometryHistory === 'function') {
      const anthHistory = resolvedDb.getAnthropometryHistory(targetUserId) || [];
      if (anthHistory.length > 0) {
        existing.anthropometry = anthHistory.map(a => ({
          date: a.date,
          weightKg: Number(a.weight_kg != null ? a.weight_kg : a.weightKg) || 0,
          chestCm: Number(a.chest_cm != null ? a.chest_cm : a.chestCm) || 0,
          waistCm: Number(a.waist_cm != null ? a.waist_cm : a.waistCm) || 0,
          bicepsCm: Number(a.biceps_cm != null ? a.biceps_cm : a.bicepsCm) || 0
        }));
      }
    }
    if (!Array.isArray(existing.anthropometry)) {
      existing.anthropometry = [];
    }

    cloud.clients[clientUuid] = existing;

    return await this.pushCloudData(cloud);
  }

  /**
   * Sync complete workout session and sets to Google Drive (upserting target date)
   */
  async syncWorkoutSessionToCloud(clientUuid, clientName, date, session, sets, userId = null, db = null) {
    if (!clientUuid) return false;
    const cloud = await this.fetchCloudData(true);
    if (!cloud.clients) cloud.clients = {};

    const client = cloud.clients[clientUuid] || {
      clientUuid,
      clientName: clientName || 'Атлет',
      syncTimestamp: Date.now(),
      assignedWorkouts: [],
      anthropometry: []
    };

    if (!Array.isArray(client.assignedWorkouts)) {
      client.assignedWorkouts = [];
    }

    // Group sets by exercise name
    const grouped = {};
    sets.forEach(s => {
      const name = s.exercise_name || 'Упражнение';
      if (!grouped[name]) grouped[name] = [];
      grouped[name].push(s);
    });

    const exercises = [];
    let exId = 1;
    for (const [name, setList] of Object.entries(grouped)) {
      exercises.push({
        exerciseId: exId++,
        name,
        muscleGroup: 'Общая',
        sets: setList.map((s, idx) => ({
          setNumber: idx + 1,
          targetWeightKg: Number(s.weight_kg) || 0,
          targetReps: Number(s.reps) || 1,
          actualWeightKg: Number(s.actual_weight_kg || s.weight_kg) || 0,
          actualReps: Number(s.actual_reps || s.reps) || 1,
          isCompleted: Boolean(s.is_completed),
          rpe: Number(s.rpe) || 8.0
        }))
      });
    }

    const isAllCompleted = sets.length > 0 && sets.every(s => Boolean(s.is_completed));
    const isSelfWorkoutAllowed = session ? Boolean(session.is_self_workout_allowed) : false;

    const sessionObj = {
      date,
      notes: session?.notes || '',
      completed: isAllCompleted,
      isSelfWorkoutAllowed,
      exercises
    };

    const existingIndex = client.assignedWorkouts.findIndex(w => w.date === date);
    if (existingIndex >= 0) {
      client.assignedWorkouts[existingIndex] = sessionObj;
    } else {
      client.assignedWorkouts.push(sessionObj);
    }

    // Populate anthropometry history from local db if available
    const resolvedDb = db || this.db;
    let targetUserId = userId;
    if (!targetUserId && resolvedDb && clientUuid) {
      const user = resolvedDb.findUserByClientUuid(clientUuid);
      if (user) targetUserId = user.id;
    }
    if (targetUserId && resolvedDb && typeof resolvedDb.getAnthropometryHistory === 'function') {
      const anthHistory = resolvedDb.getAnthropometryHistory(targetUserId) || [];
      if (anthHistory.length > 0) {
        client.anthropometry = anthHistory.map(a => ({
          date: a.date,
          weightKg: Number(a.weight_kg != null ? a.weight_kg : a.weightKg) || 0,
          chestCm: Number(a.chest_cm != null ? a.chest_cm : a.chestCm) || 0,
          waistCm: Number(a.waist_cm != null ? a.waist_cm : a.waistCm) || 0,
          bicepsCm: Number(a.biceps_cm != null ? a.biceps_cm : a.bicepsCm) || 0
        }));
      }
    }
    if (!Array.isArray(client.anthropometry)) {
      client.anthropometry = [];
    }

    client.syncTimestamp = Date.now();
    cloud.clients[clientUuid] = client;

    return await this.pushCloudData(cloud);
  }

  /**
   * Push athlete anthropometry history directly to Google Drive
   */
  async syncAnthropometryToCloud(clientUuid, clientName, anthropometryList) {
    if (!clientUuid) return false;
    const cloud = await this.fetchCloudData(true);
    if (!cloud.clients) cloud.clients = {};

    const client = cloud.clients[clientUuid] || {
      clientUuid,
      clientName: clientName || 'Атлет',
      syncTimestamp: Date.now(),
      assignedWorkouts: [],
      anthropometry: []
    };

    client.anthropometry = (anthropometryList || []).map(a => ({
      date: a.date,
      weightKg: Number(a.weight_kg != null ? a.weight_kg : a.weightKg) || 0,
      chestCm: Number(a.chest_cm != null ? a.chest_cm : a.chestCm) || 0,
      waistCm: Number(a.waist_cm != null ? a.waist_cm : a.waistCm) || 0,
      bicepsCm: Number(a.biceps_cm != null ? a.biceps_cm : a.bicepsCm) || 0
    }));

    client.syncTimestamp = Date.now();
    cloud.clients[clientUuid] = client;
    return await this.pushCloudData(cloud);
  }

  /**
   * Sync cloud payload (workouts and anthropometry) into local SQLite database.
   * Persists new anthropometry measurement rows into db.addAnthropometry if not already present.
   */
  async syncCloudWorkoutsToLocal(clientUuidOrData, userId = null, db = null) {
    const resolvedDb = db || this.db;
    if (!resolvedDb) return { syncedWorkouts: 0, syncedAnthropometry: 0 };

    let clientData = null;
    let clientUuid = null;

    if (clientUuidOrData && typeof clientUuidOrData === 'object') {
      clientData = clientUuidOrData;
      clientUuid = clientData.clientUuid || null;
    } else if (typeof clientUuidOrData === 'string') {
      clientUuid = clientUuidOrData;
      const cloud = await this.fetchCloudData(true);
      clientData = cloud.clients ? cloud.clients[clientUuid] : null;
    }

    if (!clientData) {
      return { syncedWorkouts: 0, syncedAnthropometry: 0 };
    }

    let targetUserId = userId;
    if (!targetUserId && clientUuid) {
      const user = resolvedDb.findUserByClientUuid(clientUuid);
      if (user) targetUserId = user.id;
    }

    if (!targetUserId) {
      return { syncedWorkouts: 0, syncedAnthropometry: 0 };
    }

    let syncedAnthropometry = 0;
    // 1. Parse cloud payload clientData.anthropometry and persist if not already present
    if (Array.isArray(clientData.anthropometry)) {
      const existingHistory = resolvedDb.getAnthropometryHistory(targetUserId) || [];
      for (const m of clientData.anthropometry) {
        if (!m || !m.date) continue;
        const mDate = String(m.date).trim();
        const mWeight = Number(m.weightKg != null ? m.weightKg : (m.weight_kg != null ? m.weight_kg : m.weight)) || 0;
        const mChest = Number(m.chestCm != null ? m.chestCm : (m.chest_cm != null ? m.chest_cm : 0)) || 0;
        const mWaist = Number(m.waistCm != null ? m.waistCm : (m.waist_cm != null ? m.waist_cm : 0)) || 0;
        const mBiceps = Number(m.bicepsCm != null ? m.bicepsCm : (m.biceps_cm != null ? m.biceps_cm : 0)) || 0;

        if (mWeight <= 0) continue;

        const alreadyExists = existingHistory.some(e => {
          if (e.date !== mDate) return false;
          const wtDiff = Math.abs((Number(e.weight_kg) || 0) - mWeight);
          return wtDiff < 0.01;
        });

        if (!alreadyExists) {
          resolvedDb.addAnthropometry(targetUserId, mWeight, mDate, mChest, mWaist, mBiceps);
          existingHistory.push({
            user_id: targetUserId,
            date: mDate,
            weight_kg: mWeight,
            chest_cm: mChest,
            waist_cm: mWaist,
            biceps_cm: mBiceps
          });
          syncedAnthropometry++;
        }
      }
    }

    let syncedWorkouts = 0;
    // 2. Parse cloud payload clientData.assignedWorkouts and update local workout sessions/sets
    if (Array.isArray(clientData.assignedWorkouts)) {
      for (const cw of clientData.assignedWorkouts) {
        if (!cw.date) continue;
        const { session, sets } = resolvedDb.getWorkoutSessionWithSets(targetUserId, cw.date);
        const sessionId = session ? session.id : resolvedDb.assignTrainerWorkout(
          session?.assigned_by_trainer_id || 1,
          targetUserId,
          cw.date,
          cw.isSelfWorkoutAllowed ? 1 : 0,
          cw.notes || ''
        );

        if (Array.isArray(cw.exercises)) {
          for (const ex of cw.exercises) {
            const exName = ex.name || 'Упражнение';
            if (Array.isArray(ex.sets)) {
              for (const s of ex.sets) {
                const weight = Number(s.actualWeightKg || s.targetWeightKg || s.weight || 0);
                const reps = Number(s.actualReps || s.targetReps || s.reps || 1);
                const isCompleted = Boolean(s.isCompleted);
                const rpe = Number(s.rpe || 8.0);

                const existingSet = sets.find(ls => ls.exercise_name === exName && ls.reps === reps && Math.abs(ls.weight_kg - weight) < 0.01);
                if (existingSet) {
                  if (isCompleted && !existingSet.is_completed) {
                    resolvedDb.toggleWorkoutSet(existingSet.id, true);
                    syncedWorkouts++;
                  }
                } else {
                  const newSetId = resolvedDb.addWorkoutSet(sessionId, exName, weight, reps, rpe, 0);
                  if (isCompleted) {
                    resolvedDb.toggleWorkoutSet(newSetId, true);
                  }
                  syncedWorkouts++;
                }
              }
            }
          }
        }
      }
    }

    return { syncedWorkouts, syncedAnthropometry };
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
        const rawName = String(entry.clientName).trim();
        // Skip obvious automated test probes, spam bots, or test accounts
        if (/^(adv_|test_|spam_|xss|ratelimit_|sec_|athlete_1|trainer_1|Спам Бот|Отладка|Тест)/i.test(rawName) || /смирнов|smirnov/i.test(rawName)) continue;
        const name = rawName.replace(/\s*\((Web|Mobile Athlete|Mobile|ВЫ)\)/gi, '').trim();

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
        const points = workoutsCount * 10 + Math.floor(tonnage / 100);

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
      const rawName = String(a.full_name || a.fullName || a.username).trim();
      if (/^(adv_|test_|spam_|xss|ratelimit_|sec_|athlete_1|trainer_1|Спам Бот|Отладка|Тест)/i.test(rawName) || /смирнов|smirnov/i.test(rawName)) continue;
      const name = rawName.replace(/\s*\((Web|Mobile Athlete|Mobile|ВЫ)\)/gi, '').trim();
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

  async updateAthleteRestrictions(clientUuid, restrictions) {
    if (!clientUuid) return false;
    const cloud = await this.fetchCloudData(true);
    if (!cloud.clients) cloud.clients = {};
    if (!cloud.clients[clientUuid]) {
      cloud.clients[clientUuid] = {
        clientUuid,
        clientName: 'Атлет',
        phone: '',
        syncTimestamp: Date.now(),
        assignedWorkouts: [],
        restrictions: restrictions || ''
      };
    } else {
      cloud.clients[clientUuid].restrictions = restrictions || '';
      cloud.clients[clientUuid].syncTimestamp = Date.now();
    }
    return await this.pushCloudData(cloud);
  }

  /**
   * Bi-directional Cloud Sync for Trainers:
   * Syncs trainer profile (avatar, phone, full_name) and all paired clients from Google Drive cloud.
   */
  async syncTrainerFromCloud(trainerUser, db = null) {
    const resolvedDb = db || this.db;
    if (!trainerUser || !resolvedDb) return { syncedProfile: false, syncedClients: 0 };

    const cloud = await this.fetchCloudData(true);
    let syncedProfile = false;
    let syncedClients = 0;

    const trainerFullName = String(trainerUser.full_name || trainerUser.fullName || '').trim().toLowerCase();
    const trainerPhone = String(trainerUser.phone || '').replace(/\D/g, '');
    const trainerTgUser = String(trainerUser.telegram_username || '').replace(/^@/, '').trim().toLowerCase();
    const isOwnerOrAdmin = ['santila213', 'spirit5449', 'kotpers67', 'kotpers76'].includes(trainerTgUser) ||
      ['santila213', 'spirit5449', 'kotpers67', 'kotpers76'].includes(String(trainerUser.username || '').toLowerCase());

    let foundCoachName = '';
    let foundCoachPhone = '';
    let foundCoachAvatar = '';

    // Collect all candidate client entries paired with this coach
    const pairedClientsFromCloud = [];

    // Helper to evaluate if a cloud entry matches this trainer
    const matchesTrainer = (coachName = '', coachPh = '') => {
      const cleanEntryCoachName = String(coachName || '').trim().toLowerCase();
      const cleanEntryCoachPhone = String(coachPh || '').replace(/\D/g, '');

      if (cleanEntryCoachPhone && trainerPhone && (cleanEntryCoachPhone === trainerPhone || cleanEntryCoachPhone.slice(-10) === trainerPhone.slice(-10))) {
        return true;
      }
      if (cleanEntryCoachName && trainerFullName && (cleanEntryCoachName === trainerFullName || cleanEntryCoachName.includes(trainerFullName) || trainerFullName.includes(cleanEntryCoachName))) {
        return true;
      }
      // If user is the primary ecosystem owner and coachName is set
      if (isOwnerOrAdmin && cleanEntryCoachName.length > 0) {
        return true;
      }
      return false;
    };

    // 1. Inspect cloud.clients
    if (cloud.clients && typeof cloud.clients === 'object') {
      for (const [uuid, c] of Object.entries(cloud.clients)) {
        if (!c) continue;
        if (c.coachName || c.coachPhone || c.coachAvatarBase64) {
          if (matchesTrainer(c.coachName, c.coachPhone)) {
            if (!foundCoachName && c.coachName) foundCoachName = c.coachName;
            if (!foundCoachPhone && c.coachPhone) foundCoachPhone = c.coachPhone;
            if (!foundCoachAvatar && c.coachAvatarBase64) foundCoachAvatar = c.coachAvatarBase64;
            pairedClientsFromCloud.push({
              clientUuid: uuid,
              clientName: c.clientName || 'Атлет',
              phone: c.phone || '',
              avatarBase64: c.avatarBase64 || '',
              restrictions: c.restrictions || '',
              workouts: c.assignedWorkouts || []
            });
          }
        }
      }
    }

    // 2. Inspect cloud.pairing
    if (cloud.pairing && typeof cloud.pairing === 'object') {
      for (const [pin, p] of Object.entries(cloud.pairing)) {
        if (!p) continue;
        if (p.coachName || p.coachPhone || p.coachAvatarBase64) {
          if (matchesTrainer(p.coachName, p.coachPhone)) {
            if (!foundCoachName && p.coachName) foundCoachName = p.coachName;
            if (!foundCoachPhone && p.coachPhone) foundCoachPhone = p.coachPhone;
            if (!foundCoachAvatar && p.coachAvatarBase64) foundCoachAvatar = p.coachAvatarBase64;
            if (p.clientUuid && !pairedClientsFromCloud.some(item => item.clientUuid === p.clientUuid)) {
              pairedClientsFromCloud.push({
                clientUuid: p.clientUuid,
                clientName: p.clientName || 'Атлет',
                phone: p.phone || '',
                avatarBase64: p.avatarBase64 || '',
                restrictions: p.restrictions || '',
                pairingCode: p.pin || pin
              });
            }
          }
        }
      }
    }

    // 3. Update Trainer Profile if missing locally
    let updatedName = trainerUser.full_name;
    let updatedPhone = trainerUser.phone;
    let updatedAvatar = trainerUser.avatar_base64;
    let needProfileUpdate = false;

    if ((!updatedPhone || updatedPhone.length < 5) && foundCoachPhone) {
      updatedPhone = foundCoachPhone;
      needProfileUpdate = true;
    }
    if ((!updatedAvatar || updatedAvatar.length < 50) && foundCoachAvatar) {
      updatedAvatar = foundCoachAvatar;
      needProfileUpdate = true;
    }
    if ((!updatedName || updatedName.startsWith('tg_') || updatedName === 'Telegram Атлет' || updatedName === 'Тренер') && foundCoachName) {
      updatedName = foundCoachName;
      needProfileUpdate = true;
    }

    if (needProfileUpdate && typeof resolvedDb.updateProfile === 'function') {
      resolvedDb.updateProfile(trainerUser.id, updatedName, updatedPhone, updatedAvatar);
      trainerUser.full_name = updatedName;
      trainerUser.phone = updatedPhone;
      trainerUser.avatar_base64 = updatedAvatar;
      syncedProfile = true;
    }

    // 4. Link all paired clients to trainer_clients in local SQLite
    for (const c of pairedClientsFromCloud) {
      let athlete = null;
      if (c.clientUuid && typeof resolvedDb.findUserByClientUuid === 'function') {
        athlete = resolvedDb.findUserByClientUuid(c.clientUuid);
      }
      if (!athlete && c.phone && typeof resolvedDb.findUserByPhone === 'function') {
        athlete = resolvedDb.findUserByPhone(c.phone);
      }

      // If athlete doesn't exist locally, create them as an athlete user
      if (!athlete) {
        const username = `ath_cloud_${c.clientUuid.slice(0, 8)}_${Date.now()}`;
        const passHash = crypto.createHash('sha256').update(crypto.randomBytes(16)).digest('hex');
        const newId = resolvedDb.createUser(
          username,
          passHash,
          'athlete',
          c.clientName || 'Атлет',
          c.phone || '',
          c.pairingCode || '',
          c.clientUuid,
          c.avatarBase64 || '',
          1
        );
        athlete = resolvedDb.findUserById(newId);
      } else {
        // Update athlete profile info if newer from cloud
        if (c.avatarBase64 && (!athlete.avatar_base64 || athlete.avatar_base64.length < 50)) {
          resolvedDb.updateProfile(athlete.id, athlete.full_name, athlete.phone, c.avatarBase64);
        }
        if (c.restrictions && !athlete.restrictions) {
          resolvedDb.updateAthleteRestrictions(athlete.id, c.restrictions);
        }
      }

      if (athlete) {
        if (!resolvedDb.isAthletePairedToTrainer(trainerUser.id, athlete.id)) {
          resolvedDb.pairTrainerAndAthlete(trainerUser.id, athlete.id);
          syncedClients++;
        }
        resolvedDb.updateCoachInfo(athlete.id, updatedName, updatedPhone);
      }
    }

    return { syncedProfile, syncedClients };
  }

  /**
   * Bi-directional Cloud Sync for Athletes:
   * Syncs athlete profile (FIO, phone, avatar, PIN, clientUuid) between mobile app and web portal.
   */
  async syncAthleteFromCloud(athleteUser, db = null) {
    const resolvedDb = db || this.db;
    if (!athleteUser || !resolvedDb) return { synced: false };

    const cloud = await this.fetchCloudData(true);
    let synced = false;

    const userTg = String(athleteUser.telegram_username || '').replace(/^@/, '').trim().toLowerCase();
    const isOwnerOrAdmin = ['santila213', 'spirit5449', 'kotpers67', 'kotpers76'].includes(userTg) ||
      ['santila213', 'spirit5449', 'kotpers67', 'kotpers76'].includes(String(athleteUser.username || '').toLowerCase());

    let foundClient = null;
    let foundPin = null;

    // 1. Search by clientUuid
    if (athleteUser.client_uuid && cloud.clients && cloud.clients[athleteUser.client_uuid]) {
      foundClient = cloud.clients[athleteUser.client_uuid];
    }

    // 2. Search pairing registry by clientUuid or pairingCode
    if (cloud.pairing && typeof cloud.pairing === 'object') {
      for (const [pin, p] of Object.entries(cloud.pairing)) {
        if (!p) continue;
        if (athleteUser.client_uuid && p.clientUuid === athleteUser.client_uuid) {
          foundPin = p.pin || pin;
          if (!foundClient) foundClient = p;
          break;
        }
        if (athleteUser.pairing_code && (p.pin === athleteUser.pairing_code || pin === athleteUser.pairing_code)) {
          foundPin = p.pin || pin;
          if (!foundClient) foundClient = p;
          break;
        }
      }
    }

    // 3. If owner / admin account, link directly with the real mobile athlete profile
    if ((!foundClient || !foundClient.avatarBase64) && isOwnerOrAdmin) {
      if (cloud.clients && cloud.clients['244d0d51-4806-42a5-aff2-6494969e5420']) {
        foundClient = cloud.clients['244d0d51-4806-42a5-aff2-6494969e5420'];
      }
      if (cloud.pairing && cloud.pairing['162085']) {
        foundPin = '162085';
        const pairingObj = cloud.pairing['162085'];
        if (!foundClient) {
          foundClient = pairingObj;
        } else {
          // Merge phone and fields from pairing into foundClient
          if (!foundClient.phone || foundClient.phone === 'undefined') {
            foundClient.phone = pairingObj.phone || '79373857221';
          }
        }
      }
    }

    // 4. Update local user profile in SQLite if cloud has real data
    if (foundClient) {
      const cloudName = String(foundClient.clientName || foundClient.fullName || '').trim();
      const cloudPhone = String(foundClient.phone || '').trim();
      const cloudAvatar = foundClient.avatarBase64 || '';
      const cloudRestrictions = foundClient.restrictions || '';
      const cloudUuid = foundClient.clientUuid || '';

      let updateProfileNeeded = false;
      let newName = athleteUser.full_name;
      let newPhone = athleteUser.phone;
      let newAvatar = athleteUser.avatar_base64;

      if (cloudName && (athleteUser.full_name === 'Миша' || athleteUser.full_name === 'Telegram Атлет' || athleteUser.full_name.startsWith('tg_') || athleteUser.full_name !== cloudName)) {
        newName = cloudName;
        updateProfileNeeded = true;
      }
      if (cloudPhone && cloudPhone !== 'undefined' && (!athleteUser.phone || athleteUser.phone.length < 5 || athleteUser.phone !== cloudPhone)) {
        newPhone = cloudPhone;
        updateProfileNeeded = true;
      }
      if (cloudAvatar && cloudAvatar.length > 50 && (!athleteUser.avatar_base64 || athleteUser.avatar_base64.length < 50 || athleteUser.avatar_base64 !== cloudAvatar)) {
        newAvatar = cloudAvatar;
        updateProfileNeeded = true;
      }

      if (updateProfileNeeded && typeof resolvedDb.updateProfile === 'function') {
        resolvedDb.updateProfile(athleteUser.id, newName, newPhone, newAvatar);
        athleteUser.full_name = newName;
        athleteUser.phone = newPhone;
        athleteUser.avatar_base64 = newAvatar;
        synced = true;
      }

      if (cloudRestrictions && !athleteUser.restrictions && typeof resolvedDb.updateAthleteRestrictions === 'function') {
        resolvedDb.updateAthleteRestrictions(athleteUser.id, cloudRestrictions);
        athleteUser.restrictions = cloudRestrictions;
        synced = true;
      }

      // Sync clientUuid to match mobile app exactly
      if (cloudUuid && cloudUuid !== athleteUser.client_uuid && typeof resolvedDb.updateClientUuid === 'function') {
        resolvedDb.updateClientUuid(athleteUser.id, cloudUuid);
        athleteUser.client_uuid = cloudUuid;
        synced = true;
      }

      // Sync 6-digit PIN so mobile app and web show the EXACT SAME CODE
      if (foundPin && foundPin.length === 6 && athleteUser.pairing_code !== foundPin) {
        resolvedDb.updatePairingCode(athleteUser.id, foundPin);
        athleteUser.pairing_code = foundPin;
        athleteUser.pairing_code_created_at = Date.now();
        synced = true;
      }
    }

    return { synced, foundClient };
  }
}

const cloudSyncService = new CloudSyncService();
module.exports = { cloudSyncService, CloudSyncService };
