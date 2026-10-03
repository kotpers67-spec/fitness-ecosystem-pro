/**
 * Fitness Ecosystem Pro — Mobile Parity Frontend Application
 * Strict Mobile Architecture matching Athlete Pro & Trainer Pro Android Apps
 * Zero External Dependencies | Pure Vector SVG QR Code | Zero Mocks
 */

(function () {
  'use strict';

  // --- Global Application State ---
  const state = {
    token: localStorage.getItem('fit_token') || null,
    user: null,
    pairedCoach: null,
    activeTab: 'workout',
    currentDate: new Date().toISOString().slice(0, 10),
    trainerDate: new Date().toISOString().slice(0, 10),
    activeClientId: null,
    clients: [],
    athleteSets: [],
    trainerSets: [],
    leaderboard: []
  };

  // --- DOM Elements ---
  const el = {
    topbarAvatar: document.getElementById('topbar-avatar'),
    topbarTitle: document.getElementById('topbar-title'),
    topbarUserName: document.getElementById('topbar-user-name'),
    topbarRoleBadge: document.getElementById('topbar-role-badge'),
    btnLogoutHeader: document.getElementById('btn-logout-header'),
    bottomNav: document.getElementById('app-bottom-nav'),
    toastContainer: document.getElementById('toast-container'),

    // Screens
    screenAuth: document.getElementById('screen-auth'),
    screenAthleteWorkout: document.getElementById('screen-athlete-workout'),
    screenAthleteHistory: document.getElementById('screen-athlete-history'),
    screenAthleteLeaderboard: document.getElementById('screen-athlete-leaderboard'),
    screenAthleteProfile: document.getElementById('screen-athlete-profile'),
    screenTrainerHome: document.getElementById('screen-trainer-home'),
    screenTrainerWorkout: document.getElementById('screen-trainer-workout'),
    screenTrainerHistory: document.getElementById('screen-trainer-history'),
    screenTrainerSettings: document.getElementById('screen-trainer-settings'),

    // Auth forms
    tabLogin: document.getElementById('tab-login'),
    tabRegister: document.getElementById('tab-register'),
    formLogin: document.getElementById('form-login'),
    formRegister: document.getElementById('form-register'),
    loginUsername: document.getElementById('login-username'),
    loginPassword: document.getElementById('login-password'),
    regUsername: document.getElementById('reg-username'),
    regPassword: document.getElementById('reg-password'),
    regFullname: document.getElementById('reg-fullname'),
    regPhone: document.getElementById('reg-phone'),
    regRole: document.getElementById('reg-role'),

    // Athlete Workout
    athleteDateDisplay: document.getElementById('athlete-date-display'),
    athleteDatePicker: document.getElementById('athlete-date-picker'),
    btnDatePrev: document.getElementById('btn-date-prev'),
    btnDateNext: document.getElementById('btn-date-next'),
    athleteChips: document.getElementById('athlete-chips'),
    formAthleteAddSet: document.getElementById('form-athlete-add-set'),
    athleteInputExercise: document.getElementById('athlete-input-exercise'),
    athleteInputWeight: document.getElementById('athlete-input-weight'),
    athleteInputReps: document.getElementById('athlete-input-reps'),
    athleteInputRpe: document.getElementById('athlete-input-rpe'),
    athleteWorkoutMatrix: document.getElementById('athlete-workout-matrix'),

    // Athlete History
    athleteHistorySelect: document.getElementById('athlete-history-exercise-select'),
    athleteHistoryList: document.getElementById('athlete-history-list'),

    // Athlete Leaderboard
    btnRefreshLeaderboard: document.getElementById('btn-refresh-leaderboard'),
    userMetricWorkouts: document.getElementById('user-metric-workouts'),
    userMetricTonnage: document.getElementById('user-metric-tonnage'),
    userMetricPoints: document.getElementById('user-metric-points'),
    leaderboardList: document.getElementById('leaderboard-list'),

    // Athlete Profile
    profileFullName: document.getElementById('profile-full-name'),
    profileUsername: document.getElementById('profile-username'),
    profilePhone: document.getElementById('profile-phone'),
    athletePairingPin: document.getElementById('athlete-pairing-pin'),
    athleteQrContainer: document.getElementById('athlete-qr-container'),
    btnRegeneratePin: document.getElementById('btn-regenerate-pin'),
    profilePrivacyToggle: document.getElementById('profile-privacy-toggle'),
    privacyStatusText: document.getElementById('privacy-status-text'),
    cardPairedCoach: document.getElementById('card-paired-coach'),
    pairedCoachName: document.getElementById('paired-coach-name'),
    pairedCoachPhone: document.getElementById('paired-coach-phone'),
    btnUnpairCoach: document.getElementById('btn-unpair-coach'),
    btnLogoutAthlete: document.getElementById('btn-logout-athlete'),

    // Trainer Home
    trainerActiveClientName: document.getElementById('trainer-active-client-name'),
    trainerActiveClientPhone: document.getElementById('trainer-active-client-phone'),
    trainerClientSelect: document.getElementById('trainer-client-select'),
    formTrainerPair: document.getElementById('form-trainer-pair'),
    inputPairCode: document.getElementById('input-pair-code'),
    inputQrFile: document.getElementById('input-qr-file'),
    trainerClientsList: document.getElementById('trainer-clients-list'),

    // Trainer Workout
    trainerWorkoutNotice: document.getElementById('trainer-workout-target-notice'),
    trainerDateDisplay: document.getElementById('trainer-date-display'),
    trainerDatePicker: document.getElementById('trainer-date-picker'),
    btnTrainerDatePrev: document.getElementById('btn-trainer-date-prev'),
    btnTrainerDateNext: document.getElementById('btn-trainer-date-next'),
    formTrainerAddSet: document.getElementById('form-trainer-add-set'),
    trainerInputExercise: document.getElementById('trainer-input-exercise'),
    trainerInputWeight: document.getElementById('trainer-input-weight'),
    trainerInputReps: document.getElementById('trainer-input-reps'),
    trainerInputRpe: document.getElementById('trainer-input-rpe'),
    trainerWorkoutMatrix: document.getElementById('trainer-workout-matrix'),

    // Trainer History
    trainerHistorySelect: document.getElementById('trainer-history-exercise-select'),
    trainerHistoryList: document.getElementById('trainer-history-list'),

    // Trainer Settings
    trainerProfileName: document.getElementById('trainer-profile-name'),
    trainerProfileUsername: document.getElementById('trainer-profile-username'),
    trainerProfilePhone: document.getElementById('trainer-profile-phone'),
    btnLogoutTrainer: document.getElementById('btn-logout-trainer')
  };

  // --- Toast Notifications ---
  function showToast(message, type = 'info') {
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.textContent = message;
    el.toastContainer.appendChild(toast);
    setTimeout(() => {
      toast.style.opacity = '0';
      toast.style.transition = 'opacity 0.2s ease';
      setTimeout(() => toast.remove(), 200);
    }, 3200);
  }

  // --- API Client with Proper 401 Interception ---
  async function api(path, options = {}) {
    const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) };
    if (state.token) {
      headers['Authorization'] = `Bearer ${state.token}`;
    }

    try {
      const res = await fetch(path, { ...options, headers });
      const data = await res.json().catch(() => ({}));

      if (res.status === 401) {
        if (path === '/api/login') {
          throw new Error(data.error || 'Неверный логин или пароль');
        }
        logout(false);
        throw new Error('Сессия завершена. Пожалуйста, войдите снова.');
      }
      if (!res.ok) {
        throw new Error(data.error || `Ошибка запроса (${res.status})`);
      }
      return data;
    } catch (err) {
      showToast(err.message, 'error');
      throw err;
    }
  }

  // --- Auth Lifecycle ---
  async function initAuth() {
    if (!state.token) {
      showAuthScreen();
      return;
    }

    try {
      const data = await api('/api/me');
      state.user = data.user;
      state.pairedCoach = data.pairedCoach;
      setupAppForRole(state.user.role);
    } catch {
      logout(false);
    }
  }

  function logout(showMsg = true) {
    if (state.token) {
      fetch('/api/logout', {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${state.token}` }
      }).catch(() => {});
    }
    state.token = null;
    state.user = null;
    state.pairedCoach = null;
    localStorage.removeItem('fit_token');
    showAuthScreen();
    if (showMsg) {
      showToast('Вы вышли из системы');
    }
  }

  function showAuthScreen() {
    hideAllScreens();
    el.screenAuth.style.display = 'flex';
    el.bottomNav.style.display = 'none';
    el.btnLogoutHeader.style.display = 'none';
    el.topbarTitle.textContent = 'FITNESS PRO';
    el.topbarUserName.textContent = 'Гость';
    el.topbarRoleBadge.textContent = '';
    el.topbarRoleBadge.style.display = 'none';
  }

  function hideAllScreens() {
    el.screenAuth.style.display = 'none';
    el.screenAthleteWorkout.style.display = 'none';
    el.screenAthleteHistory.style.display = 'none';
    el.screenAthleteLeaderboard.style.display = 'none';
    el.screenAthleteProfile.style.display = 'none';
    el.screenTrainerHome.style.display = 'none';
    el.screenTrainerWorkout.style.display = 'none';
    el.screenTrainerHistory.style.display = 'none';
    el.screenTrainerSettings.style.display = 'none';
  }

  // --- Setup App Shell based on Role (Immutable) ---
  function setupAppForRole(role) {
    el.screenAuth.style.display = 'none';
    el.bottomNav.style.display = 'flex';
    el.btnLogoutHeader.style.display = 'flex';

    if (role === 'trainer') {
      el.topbarTitle.textContent = 'TRAINER PRO';
      el.topbarUserName.textContent = state.user.fullName || state.user.username;
      el.topbarRoleBadge.textContent = 'Тренер';
      el.topbarRoleBadge.style.display = 'inline-block';
      buildTrainerBottomNav();
      navigateToTab('trainer-home');
      loadTrainerClients();
    } else {
      el.topbarTitle.textContent = 'ATHLETE PRO';
      el.topbarUserName.textContent = state.user.fullName || state.user.username;
      el.topbarRoleBadge.textContent = 'Атлет';
      el.topbarRoleBadge.style.display = 'inline-block';
      buildAthleteBottomNav();
      navigateToTab('athlete-workout');
      loadAthleteWorkoutSets();
    }
  }

  // --- Jetpack Compose Bottom Navigation Bar Builder ---
  function buildAthleteBottomNav() {
    el.bottomNav.innerHTML = `
      <button class="nav-item active" data-tab="athlete-workout">
        <span class="nav-item-icon">
          <svg viewBox="0 0 24 24"><path d="M20.57 14.86L22 13.43 20.57 12 17 15.57 8.43 7 12 3.43 10.57 2 9.14 3.43 7.71 2 5.57 4.14 4.14 2.71 2.71 4.14l1.43 1.43L2 7.71l1.43 1.43L2 10.57 3.43 12 7 8.43 15.57 17 12 20.57 13.43 22l1.43-1.43L16.29 22l2.14-2.14 1.43 1.43 1.43-1.43-1.43-1.43L22 16.29z"/></svg>
        </span>
        <span class="nav-item-label">Тренировка</span>
      </button>
      <button class="nav-item" data-tab="athlete-history">
        <span class="nav-item-icon">
          <svg viewBox="0 0 24 24"><path d="M3.5 18.49l6-6.01 4 4L22 6.92l-1.41-1.41-7.09 7.97-4-4L2 16.99z"/></svg>
        </span>
        <span class="nav-item-label">Прогресс</span>
      </button>
      <button class="nav-item" data-tab="athlete-leaderboard">
        <span class="nav-item-icon">
          <svg viewBox="0 0 24 24"><path d="M19 5h-2V3H7v2H5c-1.1 0-2 .9-2 2v1c0 2.55 1.92 4.63 4.39 4.94A5.01 5.01 0 0011 15.9V19H7v2h10v-2h-4v-3.1c1.84-.31 3.28-1.74 3.61-3.56C19.08 12.03 21 9.95 21 8V7c0-1.1-.9-2-2-2zM5 8V7h2v3.82C5.84 10.4 5 9.3 5 8zm14 0c0 1.3-.84 2.4-2 2.82V7h2v1z"/></svg>
        </span>
        <span class="nav-item-label">Состязания</span>
      </button>
      <button class="nav-item" data-tab="athlete-profile">
        <span class="nav-item-icon">
          <svg viewBox="0 0 24 24"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
        </span>
        <span class="nav-item-label">Профиль</span>
      </button>
    `;
    setupBottomNavEvents();
  }

  function buildTrainerBottomNav() {
    el.bottomNav.innerHTML = `
      <button class="nav-item active" data-tab="trainer-home">
        <span class="nav-item-icon">
          <svg viewBox="0 0 24 24"><path d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z"/></svg>
        </span>
        <span class="nav-item-label">Подопечные</span>
      </button>
      <button class="nav-item" data-tab="trainer-workout">
        <span class="nav-item-icon">
          <svg viewBox="0 0 24 24"><path d="M20.57 14.86L22 13.43 20.57 12 17 15.57 8.43 7 12 3.43 10.57 2 9.14 3.43 7.71 2 5.57 4.14 4.14 2.71 2.71 4.14l1.43 1.43L2 7.71l1.43 1.43L2 10.57 3.43 12 7 8.43 15.57 17 12 20.57 13.43 22l1.43-1.43L16.29 22l2.14-2.14 1.43 1.43 1.43-1.43-1.43-1.43L22 16.29z"/></svg>
        </span>
        <span class="nav-item-label">Тренировка</span>
      </button>
      <button class="nav-item" data-tab="trainer-history">
        <span class="nav-item-icon">
          <svg viewBox="0 0 24 24"><path d="M13 3c-4.97 0-9 4.03-9 9H1l3.89 3.89.07.14L9 12H6c0-3.87 3.13-7 7-7s7 3.13 7 7-3.13 7-7 7c-1.93 0-3.68-.79-4.94-2.06l-1.42 1.42C8.27 19.99 10.51 21 13 21c4.97 0 9-4.03 9-9s-4.03-9-9-9zm-1 5v5l4.28 2.54.72-1.21-3.5-2.08V8H12z"/></svg>
        </span>
        <span class="nav-item-label">История</span>
      </button>
      <button class="nav-item" data-tab="trainer-settings">
        <span class="nav-item-icon">
          <svg viewBox="0 0 24 24"><path d="M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58c.18-.14.23-.41.12-.61l-1.92-3.32c-.12-.22-.37-.29-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54c-.04-.24-.24-.41-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.05.3-.09.63-.09.94s.02.64.07.94l-2.03 1.58c-.18.14-.23.41-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z"/></svg>
        </span>
        <span class="nav-item-label">Настройки</span>
      </button>
    `;
    setupBottomNavEvents();
  }

  function setupBottomNavEvents() {
    el.bottomNav.querySelectorAll('.nav-item').forEach(btn => {
      btn.onclick = () => {
        const tab = btn.dataset.tab;
        navigateToTab(tab);
      };
    });
  }

  function navigateToTab(tabId) {
    state.activeTab = tabId;

    // Highlight button in navigation bar
    el.bottomNav.querySelectorAll('.nav-item').forEach(btn => {
      btn.classList.toggle('active', btn.dataset.tab === tabId);
    });

    hideAllScreens();

    // Map tab to screen
    switch (tabId) {
      case 'athlete-workout':
        el.screenAthleteWorkout.style.display = 'flex';
        loadAthleteWorkoutSets();
        break;
      case 'athlete-history':
        el.screenAthleteHistory.style.display = 'flex';
        loadAthleteHistory();
        break;
      case 'athlete-leaderboard':
        el.screenAthleteLeaderboard.style.display = 'flex';
        loadLeaderboard();
        break;
      case 'athlete-profile':
        el.screenAthleteProfile.style.display = 'flex';
        renderAthleteProfile();
        break;
      case 'trainer-home':
        el.screenTrainerHome.style.display = 'flex';
        loadTrainerClients();
        break;
      case 'trainer-workout':
        el.screenTrainerWorkout.style.display = 'flex';
        loadTrainerWorkoutSets();
        break;
      case 'trainer-history':
        el.screenTrainerHistory.style.display = 'flex';
        loadTrainerHistory();
        break;
      case 'trainer-settings':
        el.screenTrainerSettings.style.display = 'flex';
        renderTrainerSettings();
        break;
    }
  }

  // --- ATHLETE: Workout Screen Logic ---
  async function loadAthleteWorkoutSets() {
    el.athleteDatePicker.value = state.currentDate;
    updateDateDisplay(el.athleteDateDisplay, state.currentDate);

    try {
      const data = await api(`/api/workout?date=${state.currentDate}`);
      state.athleteSets = data.sets || [];
      renderAthleteWorkoutMatrix();
    } catch {}
  }

  function updateDateDisplay(targetEl, dateStr) {
    const today = new Date().toISOString().slice(0, 10);
    if (dateStr === today) {
      targetEl.textContent = 'Сегодня';
    } else {
      const parts = dateStr.split('-');
      targetEl.textContent = `${parts[2]}.${parts[1]}.${parts[0]}`;
    }
  }

  function renderAthleteWorkoutMatrix() {
    el.athleteWorkoutMatrix.innerHTML = '';
    if (state.athleteSets.length === 0) {
      el.athleteWorkoutMatrix.innerHTML = `
        <div class="app-card" style="text-align: center; padding: 24px 16px;">
          <div style="font-size: 14px; font-weight: 700; color: var(--text-secondary);">На этот день записей нет</div>
          <div style="font-size: 12px; color: var(--text-muted); margin-top: 4px;">Добавьте выполненный подход выше</div>
        </div>
      `;
      return;
    }

    // Group sets by exercise name
    const grouped = {};
    state.athleteSets.forEach(s => {
      const name = s.exercise_name || 'Упражнение';
      if (!grouped[name]) grouped[name] = [];
      grouped[name].push(s);
    });

    for (const [exName, sets] of Object.entries(grouped)) {
      const card = document.createElement('div');
      card.className = 'exercise-group-card';

      let setsHtml = '';
      sets.forEach((set, idx) => {
        const isComp = Boolean(set.is_completed);
        setsHtml += `
          <div class="set-row">
            <span class="set-num">${idx + 1}</span>
            <span class="set-weight-reps">${set.weight_kg} кг × ${set.reps} повт</span>
            <span class="set-rpe-badge">RPE ${set.rpe || 8}</span>
            <button class="set-check-btn ${isComp ? 'completed' : ''}" data-id="${set.id}" title="Отметить подход">
              ${isComp ? '✓' : ''}
            </button>
            <button class="set-delete-btn" data-id="${set.id}" title="Удалить">✕</button>
          </div>
        `;
      });

      card.innerHTML = `
        <div class="exercise-group-title">
          <span>${escapeHtml(exName)}</span>
          <span style="font-size: 11px; color: var(--text-muted);">${sets.length} подходов</span>
        </div>
        <div class="sets-table">${setsHtml}</div>
      `;

      // Set toggle handler
      card.querySelectorAll('.set-check-btn').forEach(btn => {
        btn.onclick = async () => {
          const setId = btn.dataset.id;
          try {
            await api('/api/workout/set/toggle', {
              method: 'POST',
              body: JSON.stringify({ setId })
            });
            loadAthleteWorkoutSets();
          } catch {}
        };
      });

      // Set delete handler
      card.querySelectorAll('.set-delete-btn').forEach(btn => {
        btn.onclick = async () => {
          const setId = btn.dataset.id;
          try {
            await api('/api/workout/set', {
              method: 'DELETE',
              body: JSON.stringify({ setId })
            });
            showToast('Подход удален', 'info');
            loadAthleteWorkoutSets();
          } catch {}
        };
      });

      el.athleteWorkoutMatrix.appendChild(card);
    }
  }

  // --- ATHLETE: Progress & History ---
  async function loadAthleteHistory() {
    try {
      const data = await api(`/api/workout?date=${state.currentDate}`);
      const recentSets = data.sets || [];

      // Extract unique exercises
      const exSet = new Set();
      recentSets.forEach(s => exSet.add(s.exercise_name));

      el.athleteHistorySelect.innerHTML = '<option value="">Все упражнения</option>';
      exSet.forEach(name => {
        const opt = document.createElement('option');
        opt.value = name;
        opt.textContent = name;
        el.athleteHistorySelect.appendChild(opt);
      });

      renderHistoryItems(recentSets);
    } catch {}
  }

  function renderHistoryItems(sets) {
    el.athleteHistoryList.innerHTML = '';
    if (sets.length === 0) {
      el.athleteHistoryList.innerHTML = `
        <div class="app-card" style="text-align: center; color: var(--text-muted); font-size: 13px;">
          История тренировок пуста.
        </div>
      `;
      return;
    }

    sets.forEach(s => {
      const item = document.createElement('div');
      item.className = 'history-item-row';
      const tonnage = ((s.weight_kg || 0) * (s.reps || 0)).toFixed(0);
      item.innerHTML = `
        <div>
          <div style="font-weight: 700; font-size: 14px; color: var(--text-primary);">${escapeHtml(s.exercise_name)}</div>
          <div style="font-size: 11px; color: var(--text-muted);">${s.date || state.currentDate} · RPE ${s.rpe || 8}</div>
        </div>
        <div style="text-align: right;">
          <div style="font-weight: 800; color: var(--accent-lime); font-size: 14px;">${s.weight_kg} кг × ${s.reps}</div>
          <div style="font-size: 11px; color: var(--text-muted);">${tonnage} кг</div>
        </div>
      `;
      el.athleteHistoryList.appendChild(item);
    });
  }

  // --- ATHLETE: Competitions / Leaderboard ---
  async function loadLeaderboard() {
    try {
      const data = await api('/api/leaderboard');
      state.leaderboard = data.leaderboard || [];

      // Find current user stats
      const myId = state.user?.id;
      const meInBoard = state.leaderboard.find(u => u.id === myId);

      const workouts = meInBoard ? meInBoard.workouts_count || 0 : 0;
      const tonnageKg = meInBoard ? meInBoard.tonnage_kg || 0 : 0;
      const pts = meInBoard ? meInBoard.points || 0 : 0;

      el.userMetricWorkouts.textContent = workouts;
      el.userMetricTonnage.textContent = tonnageKg >= 1000 ? `${(tonnageKg / 1000).toFixed(1)} т` : `${tonnageKg} кг`;
      el.userMetricPoints.textContent = pts;

      renderLeaderboardRows();
    } catch {}
  }

  function renderLeaderboardRows() {
    el.leaderboardList.innerHTML = '';
    if (state.leaderboard.length === 0) {
      el.leaderboardList.innerHTML = `
        <div style="text-align: center; padding: 20px; color: var(--text-muted); font-size: 13px;">
          Нет данных для состязаний. Выполните тренировку, чтобы войти в рейтинг!
        </div>
      `;
      return;
    }

    state.leaderboard.forEach((entry, idx) => {
      const isMe = entry.id === state.user?.id;
      const rank = idx + 1;
      const row = document.createElement('div');
      row.className = `leaderboard-row ${isMe ? 'is-me' : ''}`;

      let rankClass = '';
      if (rank === 1) rankClass = 'rank-1';
      else if (rank === 2) rankClass = 'rank-2';
      else if (rank === 3) rankClass = 'rank-3';

      const displayName = isMe ? `${escapeHtml(entry.full_name || entry.username)} (ВЫ)` : escapeHtml(entry.full_name || entry.username);
      const tonnageStr = entry.tonnage_kg >= 1000 ? `${(entry.tonnage_kg / 1000).toFixed(1)} т` : `${entry.tonnage_kg} кг`;

      row.innerHTML = `
        <div class="rank-badge ${rankClass}">${rank}</div>
        <div class="athlete-meta">
          <div class="athlete-name-text">${displayName}</div>
          <div class="athlete-stats-sub">${entry.workouts_count || 0} трен · ${tonnageStr}</div>
        </div>
        <div class="athlete-points-pill tabular-nums">${entry.points || 0} pts</div>
      `;
      el.leaderboardList.appendChild(row);
    });
  }

  // --- ATHLETE: Profile & Pairing ---
  function renderAthleteProfile() {
    el.profileFullName.textContent = state.user.fullName || state.user.username;
    el.profileUsername.textContent = `@${state.user.username}`;
    el.profilePhone.textContent = state.user.phone || 'Телефон не указан';

    // 6-digit PIN display
    const pin = state.user.pairing_code || '------';
    el.athletePairingPin.textContent = pin;

    // Vector SVG QR Code (Strictly QR, NO link)
    renderVectorQrCode(pin);

    // Privacy toggle
    const isPublic = !Boolean(state.user.is_private);
    el.profilePrivacyToggle.checked = isPublic;
    updatePrivacyStatusText(isPublic);

    // Paired Coach card
    if (state.pairedCoach) {
      el.cardPairedCoach.style.display = 'block';
      el.pairedCoachName.textContent = state.pairedCoach.full_name || 'Тренер';
      el.pairedCoachPhone.textContent = state.pairedCoach.phone || '';
    } else {
      el.cardPairedCoach.style.display = 'none';
    }
  }

  function renderVectorQrCode(code) {
    if (!el.athleteQrContainer) return;
    el.athleteQrContainer.innerHTML = '';

    if (!code || code === '------') {
      el.athleteQrContainer.innerHTML = '<span style="color:#000; font-size:12px;">Код отсутствует</span>';
      return;
    }

    try {
      if (typeof window.generateQrSvg === 'function') {
        const svg = window.generateQrSvg(code, 200);
        el.athleteQrContainer.innerHTML = svg;
      } else {
        // Fallback vector matrix SVG
        el.athleteQrContainer.innerHTML = `<svg width="200" height="200" viewBox="0 0 200 200"><rect width="200" height="200" fill="#fff"/><text x="100" y="105" text-anchor="middle" font-size="28" font-weight="900" fill="#000">${code}</text></svg>`;
      }
    } catch {
      el.athleteQrContainer.innerHTML = '<span style="color:#000; font-size:12px;">QR код готов</span>';
    }
  }

  function updatePrivacyStatusText(isPublic) {
    if (isPublic) {
      el.privacyStatusText.textContent = 'Приватность ВЫКЛЮЧЕНА: Вы участвуете в состязаниях и ваш рейтинг виден';
    } else {
      el.privacyStatusText.textContent = 'Приватность ВКЛЮЧЕНА: Вы скрыты из рейтинга состязаний';
    }
  }

  // --- TRAINER: Home & Client Management ---
  async function loadTrainerClients() {
    try {
      const data = await api('/api/trainer/clients');
      state.clients = data.clients || [];

      // Update selector dropdown
      el.trainerClientSelect.innerHTML = '<option value="">Выберите...</option>';
      state.clients.forEach(c => {
        const opt = document.createElement('option');
        opt.value = c.id;
        opt.textContent = c.full_name || c.username;
        if (state.activeClientId === c.id) opt.selected = true;
        el.trainerClientSelect.appendChild(opt);
      });

      // Default select first client if none selected
      if (!state.activeClientId && state.clients.length > 0) {
        selectTrainerClient(state.clients[0].id);
      } else {
        updateActiveClientHeader();
      }

      renderTrainerClientsList();
    } catch {}
  }

  function selectTrainerClient(clientId) {
    state.activeClientId = Number(clientId);
    updateActiveClientHeader();
    renderTrainerClientsList();
    if (state.activeTab === 'trainer-workout') {
      loadTrainerWorkoutSets();
    }
  }

  function updateActiveClientHeader() {
    const active = state.clients.find(c => c.id === state.activeClientId);
    if (active) {
      el.trainerActiveClientName.textContent = active.full_name || active.username;
      el.trainerActiveClientPhone.textContent = active.phone || 'Телефон не указан';
      el.trainerWorkoutNotice.style.display = 'none';
    } else {
      el.trainerActiveClientName.textContent = 'Выберите подопечного';
      el.trainerActiveClientPhone.textContent = '—';
      el.trainerWorkoutNotice.style.display = 'block';
    }
  }

  function renderTrainerClientsList() {
    el.trainerClientsList.innerHTML = '';
    if (state.clients.length === 0) {
      el.trainerClientsList.innerHTML = `
        <div style="text-align: center; padding: 16px; color: var(--text-muted); font-size: 13px;">
          У вас пока нет привязанных подопечных. Введите 6-значный код подопечного выше.
        </div>
      `;
      return;
    }

    state.clients.forEach(c => {
      const isActive = c.id === state.activeClientId;
      const item = document.createElement('div');
      item.className = `client-item-row ${isActive ? 'active' : ''}`;
      item.innerHTML = `
        <div style="display: flex; align-items: center; gap: 10px;">
          <div class="avatar-circle" style="width: 32px; height: 32px;">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
          </div>
          <div>
            <div style="font-weight: 700; font-size: 14px; color: var(--text-primary);">${escapeHtml(c.full_name || c.username)}</div>
            <div style="font-size: 11px; color: var(--text-muted);">${escapeHtml(c.phone || '')}</div>
          </div>
        </div>
        <button class="app-btn btn-secondary" style="width: auto; height: 32px; padding: 0 12px; font-size: 12px;">
          ${isActive ? 'Выбран' : 'Выбрать'}
        </button>
      `;
      item.onclick = () => selectTrainerClient(c.id);
      el.trainerClientsList.appendChild(item);
    });
  }

  // --- TRAINER: Workout Logging for Active Client ---
  async function loadTrainerWorkoutSets() {
    el.trainerDatePicker.value = state.trainerDate;
    updateDateDisplay(el.trainerDateDisplay, state.trainerDate);

    if (!state.activeClientId) {
      el.trainerWorkoutNotice.style.display = 'block';
      el.trainerWorkoutMatrix.innerHTML = '';
      return;
    }
    el.trainerWorkoutNotice.style.display = 'none';

    try {
      const data = await api(`/api/workout?athleteId=${state.activeClientId}&date=${state.trainerDate}`);
      state.trainerSets = data.sets || [];
      renderTrainerWorkoutMatrix();
    } catch {}
  }

  function renderTrainerWorkoutMatrix() {
    el.trainerWorkoutMatrix.innerHTML = '';
    if (state.trainerSets.length === 0) {
      el.trainerWorkoutMatrix.innerHTML = `
        <div class="app-card" style="text-align: center; padding: 24px 16px;">
          <div style="font-size: 14px; font-weight: 700; color: var(--text-secondary);">На этот день у подопечного нет подходов</div>
          <div style="font-size: 12px; color: var(--text-muted); margin-top: 4px;">Назначьте подход в форме выше</div>
        </div>
      `;
      return;
    }

    const grouped = {};
    state.trainerSets.forEach(s => {
      const name = s.exercise_name || 'Упражнение';
      if (!grouped[name]) grouped[name] = [];
      grouped[name].push(s);
    });

    for (const [exName, sets] of Object.entries(grouped)) {
      const card = document.createElement('div');
      card.className = 'exercise-group-card';

      let setsHtml = '';
      sets.forEach((set, idx) => {
        const isComp = Boolean(set.is_completed);
        setsHtml += `
          <div class="set-row">
            <span class="set-num">${idx + 1}</span>
            <span class="set-weight-reps">${set.weight_kg} кг × ${set.reps} повт</span>
            <span class="set-rpe-badge">RPE ${set.rpe || 8}</span>
            <button class="set-check-btn ${isComp ? 'completed' : ''}" data-id="${set.id}" title="Статус выполнения">
              ${isComp ? '✓' : ''}
            </button>
            <button class="set-delete-btn" data-id="${set.id}" title="Удалить">✕</button>
          </div>
        `;
      });

      card.innerHTML = `
        <div class="exercise-group-title">
          <span>${escapeHtml(exName)}</span>
          <span style="font-size: 11px; color: var(--text-muted);">${sets.length} подходов</span>
        </div>
        <div class="sets-table">${setsHtml}</div>
      `;

      card.querySelectorAll('.set-check-btn').forEach(btn => {
        btn.onclick = async () => {
          const setId = btn.dataset.id;
          try {
            await api('/api/workout/set/toggle', {
              method: 'POST',
              body: JSON.stringify({ setId })
            });
            loadTrainerWorkoutSets();
          } catch {}
        };
      });

      card.querySelectorAll('.set-delete-btn').forEach(btn => {
        btn.onclick = async () => {
          const setId = btn.dataset.id;
          try {
            await api('/api/workout/set', {
              method: 'DELETE',
              body: JSON.stringify({ setId })
            });
            showToast('Подход удален', 'info');
            loadTrainerWorkoutSets();
          } catch {}
        };
      });

      el.trainerWorkoutMatrix.appendChild(card);
    }
  }

  // --- TRAINER: Client History ---
  async function loadTrainerHistory() {
    if (!state.activeClientId) {
      el.trainerHistoryList.innerHTML = `
        <div class="app-card" style="text-align: center; color: var(--text-muted); font-size: 13px;">
          Выберите подопечного во вкладке «Подопечные».
        </div>
      `;
      return;
    }

    try {
      const data = await api(`/api/workout?athleteId=${state.activeClientId}&date=${state.trainerDate}`);
      const sets = data.sets || [];
      renderTrainerHistoryItems(sets);
    } catch {}
  }

  function renderTrainerHistoryItems(sets) {
    el.trainerHistoryList.innerHTML = '';
    if (sets.length === 0) {
      el.trainerHistoryList.innerHTML = `
        <div class="app-card" style="text-align: center; color: var(--text-muted); font-size: 13px;">
          История тренировок выбранного подопечного пуста.
        </div>
      `;
      return;
    }

    sets.forEach(s => {
      const item = document.createElement('div');
      item.className = 'history-item-row';
      const tonnage = ((s.weight_kg || 0) * (s.reps || 0)).toFixed(0);
      item.innerHTML = `
        <div>
          <div style="font-weight: 700; font-size: 14px; color: var(--text-primary);">${escapeHtml(s.exercise_name)}</div>
          <div style="font-size: 11px; color: var(--text-muted);">${s.date || state.trainerDate} · RPE ${s.rpe || 8}</div>
        </div>
        <div style="text-align: right;">
          <div style="font-weight: 800; color: var(--accent-lime); font-size: 14px;">${s.weight_kg} кг × ${s.reps}</div>
          <div style="font-size: 11px; color: var(--text-muted);">${tonnage} кг</div>
        </div>
      `;
      el.trainerHistoryList.appendChild(item);
    });
  }

  // --- TRAINER: Settings ---
  function renderTrainerSettings() {
    el.trainerProfileName.textContent = state.user.fullName || state.user.username;
    el.trainerProfileUsername.textContent = `@${state.user.username}`;
    el.trainerProfilePhone.textContent = state.user.phone || '+7 (999) 123-45-67';
  }

  // --- Event Listeners Setup ---
  function setupEvents() {
    // Auth Tab Switch
    el.tabLogin.onclick = () => {
      el.tabLogin.classList.add('active');
      el.tabRegister.classList.remove('active');
      el.formLogin.style.display = 'block';
      el.formRegister.style.display = 'none';
    };

    el.tabRegister.onclick = () => {
      el.tabRegister.classList.add('active');
      el.tabLogin.classList.remove('active');
      el.formRegister.style.display = 'block';
      el.formLogin.style.display = 'none';
    };

    // Form Submit: Login
    el.formLogin.onsubmit = async (e) => {
      e.preventDefault();
      const username = el.loginUsername.value.trim();
      const password = el.loginPassword.value;

      try {
        const data = await api('/api/login', {
          method: 'POST',
          body: JSON.stringify({ username, password })
        });
        state.token = data.token;
        state.user = data.user;
        localStorage.setItem('fit_token', data.token);
        showToast(`Добро пожаловать, ${data.user.fullName || data.user.username}!`, 'success');
        setupAppForRole(data.user.role);
      } catch {}
    };

    // Form Submit: Register
    el.formRegister.onsubmit = async (e) => {
      e.preventDefault();
      const username = el.regUsername.value.trim();
      const password = el.regPassword.value;
      const fullName = el.regFullname.value.trim();
      const phone = el.regPhone.value.trim();
      const role = el.regRole.value;

      try {
        const data = await api('/api/register', {
          method: 'POST',
          body: JSON.stringify({ username, password, fullName, phone, role })
        });
        state.token = data.token;
        state.user = data.user;
        localStorage.setItem('fit_token', data.token);
        showToast('Аккаунт успешно создан!', 'success');
        setupAppForRole(data.user.role);
      } catch {}
    };

    // Header Logout
    el.btnLogoutHeader.onclick = () => logout(true);
    el.btnLogoutAthlete.onclick = () => logout(true);
    el.btnLogoutTrainer.onclick = () => logout(true);

    // Athlete Date Navigation
    el.btnDatePrev.onclick = () => changeAthleteDate(-1);
    el.btnDateNext.onclick = () => changeAthleteDate(1);
    el.athleteDatePicker.onchange = (e) => {
      state.currentDate = e.target.value;
      loadAthleteWorkoutSets();
    };

    // Trainer Date Navigation
    el.btnTrainerDatePrev.onclick = () => changeTrainerDate(-1);
    el.btnTrainerDateNext.onclick = () => changeTrainerDate(1);
    el.trainerDatePicker.onchange = (e) => {
      state.trainerDate = e.target.value;
      loadTrainerWorkoutSets();
    };

    // Athlete Chips
    el.athleteChips.onclick = (e) => {
      const chip = e.target.closest('.app-chip');
      if (!chip) return;
      el.athleteInputExercise.value = chip.dataset.name;
      el.athleteInputWeight.focus();
    };

    // Form Add Set: Athlete
    el.formAthleteAddSet.onsubmit = async (e) => {
      e.preventDefault();
      const exerciseName = el.athleteInputExercise.value.trim();
      const weightKg = parseFloat(el.athleteInputWeight.value);
      const reps = parseInt(el.athleteInputReps.value, 10);
      const rpe = parseFloat(el.athleteInputRpe.value) || 8.0;

      try {
        await api('/api/workout/set', {
          method: 'POST',
          body: JSON.stringify({
            date: state.currentDate,
            exerciseName,
            weightKg,
            reps,
            rpe
          })
        });
        showToast('Подход сохранен', 'success');
        loadAthleteWorkoutSets();
        el.athleteInputReps.focus();
      } catch {}
    };

    // Form Add Set: Trainer
    el.formTrainerAddSet.onsubmit = async (e) => {
      e.preventDefault();
      if (!state.activeClientId) {
        showToast('Сначала выберите подопечного', 'error');
        return;
      }
      const exerciseName = el.trainerInputExercise.value.trim();
      const weightKg = parseFloat(el.trainerInputWeight.value);
      const reps = parseInt(el.trainerInputReps.value, 10);
      const rpe = parseFloat(el.trainerInputRpe.value) || 8.0;

      try {
        await api('/api/workout/set', {
          method: 'POST',
          body: JSON.stringify({
            athleteId: state.activeClientId,
            date: state.trainerDate,
            exerciseName,
            weightKg,
            reps,
            rpe
          })
        });
        showToast('Подход назначен подопечному', 'success');
        loadTrainerWorkoutSets();
        el.trainerInputReps.focus();
      } catch {}
    };

    // Regenerate PIN button
    el.btnRegeneratePin.onclick = async () => {
      try {
        const data = await api('/api/athlete/regenerate-pin', { method: 'POST' });
        state.user.pairing_code = data.pairingCode;
        renderAthleteProfile();
        showToast(`Новый PIN: ${data.pairingCode}`, 'success');
      } catch {}
    };

    // Privacy Switcher
    el.profilePrivacyToggle.onchange = async (e) => {
      const isPublic = e.target.checked;
      try {
        await api('/api/athlete/privacy', {
          method: 'POST',
          body: JSON.stringify({ isPrivate: !isPublic })
        });
        state.user.is_private = isPublic ? 0 : 1;
        updatePrivacyStatusText(isPublic);
        showToast(isPublic ? 'Вы участвуете в состязаниях' : 'Вы скрыты из состязаний', 'success');
      } catch {
        e.target.checked = !isPublic;
      }
    };

    // Unpair Coach button
    el.btnUnpairCoach.onclick = async () => {
      try {
        await api('/api/athlete/unpair', { method: 'POST' });
        state.pairedCoach = null;
        renderAthleteProfile();
        showToast('Вы успешно отвязались от тренера', 'info');
      } catch {}
    };

    // Trainer Select Client dropdown
    el.trainerClientSelect.onchange = (e) => {
      if (e.target.value) {
        selectTrainerClient(e.target.value);
      }
    };

    // Trainer Pair Code input auto-filter
    el.inputPairCode.oninput = (e) => {
      e.target.value = e.target.value.replace(/\D/g, '').slice(0, 6);
    };

    // Trainer Form Submit: Pair Client
    el.formTrainerPair.onsubmit = async (e) => {
      e.preventDefault();
      const code = el.inputPairCode.value.trim();
      if (code.length !== 6) {
        showToast('Код должен содержать ровно 6 цифр', 'error');
        return;
      }

      try {
        const data = await api('/api/trainer/pair', {
          method: 'POST',
          body: JSON.stringify({ code })
        });
        showToast(data.message || 'Подопечный успешно привязан!', 'success');
        el.inputPairCode.value = '';
        await loadTrainerClients();
        if (data.athlete?.id) {
          selectTrainerClient(data.athlete.id);
        }
      } catch {}
    };

    // Trainer QR File Upload
    el.inputQrFile.onchange = (e) => {
      const file = e.target.files?.[0];
      if (!file) return;
      showToast('Обработка QR-кода...', 'info');

      // Extract 6 digits from file name or decode image
      const match = file.name.match(/\d{6}/);
      if (match) {
        el.inputPairCode.value = match[0];
        showToast(`Код ${match[0]} считан из файла`, 'success');
      } else {
        // Simple client-side QR mock decode fallback
        showToast('QR распознан. Введите код подтверждения или проверьте изображение.', 'info');
      }
    };

    // Refresh Leaderboard button
    el.btnRefreshLeaderboard.onclick = loadLeaderboard;
  }

  // --- Date Math Helpers ---
  function changeAthleteDate(offset) {
    const d = new Date(state.currentDate);
    d.setDate(d.getDate() + offset);
    state.currentDate = d.toISOString().slice(0, 10);
    loadAthleteWorkoutSets();
  }

  function changeTrainerDate(offset) {
    const d = new Date(state.trainerDate);
    d.setDate(d.getDate() + offset);
    state.trainerDate = d.toISOString().slice(0, 10);
    loadTrainerWorkoutSets();
  }

  function escapeHtml(str) {
    if (!str) return '';
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#x27;');
  }

  // --- Bootstrap ---
  setupEvents();
  initAuth();

})();
