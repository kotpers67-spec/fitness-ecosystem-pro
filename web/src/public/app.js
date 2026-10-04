/**
 * Fitness Ecosystem Pro — Mobile Parity Frontend Application
 * Strict Mobile Architecture matching Athlete Pro & Trainer Pro Android Apps
 * Zero External Dependencies | Pure Vector SVG QR Code | Zero Mocks | Google Drive Cloud Sync
 */

(function () {
  'use strict';

  function getInitialToken() {
    let token = localStorage.getItem('fit_token');
    if (!token && typeof document !== 'undefined' && document.cookie) {
      const match = document.cookie.match(/(?:^|;\s*)fit_token=([^;]+)/);
      if (match) token = match[1];
    }
    return token || null;
  }

  function saveAuthToken(token) {
    state.token = token;
    if (token) {
      localStorage.setItem('fit_token', token);
      if (typeof document !== 'undefined') {
        document.cookie = `fit_token=${token}; max-age=31536000; path=/; SameSite=Lax`;
      }
    } else {
      localStorage.removeItem('fit_token');
      if (typeof document !== 'undefined') {
        document.cookie = `fit_token=; max-age=0; path=/`;
      }
    }
  }

  // --- Global Application State ---
  const state = {
    token: getInitialToken(),
    user: null,
    pairedCoach: null,
    activeTab: 'workout',
    currentDate: new Date().toISOString().slice(0, 10),
    trainerDate: new Date().toISOString().slice(0, 10),
    activeClientId: null,
    clients: [],
    athleteSets: [],
    trainerSets: [],
    leaderboard: [],
    athleteAvatarBase64: null,
    trainerAvatarBase64: null,
    pinTimerInterval: null,
    pending2FAUserId: null,
    login2faTimerInterval: null,
    linkTgTimerInterval: null,
    athleteSelectedExercise: null,
    trainerSelectedExercise: null,
    deferredInstallPrompt: null
  };

  // --- DOM Elements ---
  const el = {
    topbarAvatar: document.getElementById('topbar-avatar'),
    topbarTitle: document.getElementById('topbar-title'),
    topbarUserName: document.getElementById('topbar-user-name'),
    topbarRoleBadge: document.getElementById('topbar-role-badge'),
    btnSyncHeader: document.getElementById('btn-sync-header'),
    btnSyncAthlete: document.getElementById('btn-sync-athlete'),
    btnSyncTrainer: document.getElementById('btn-sync-trainer'),
    athleteSyncStatusText: document.getElementById('athlete-sync-status-text'),
    trainerSyncStatusText: document.getElementById('trainer-sync-status-text'),
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
    btnTelegramAuth: document.getElementById('btn-telegram-auth'),
    dialogTelegramAuth: document.getElementById('dialog-telegram-auth'),
    tgStepOneclick: document.getElementById('tg-step-oneclick'),
    btnTgOneclickOpen: document.getElementById('btn-tg-oneclick-open'),
    tgOneclickStatusText: document.getElementById('tg-oneclick-status-text'),
    tgOneclickQrContainer: document.getElementById('tg-oneclick-qr-container'),
    tgOneclickQrSvg: document.getElementById('tg-oneclick-qr-svg'),
    btnToggleTgManualCode: document.getElementById('btn-toggle-tg-manual-code'),
    btnTgBackToOneclick: document.getElementById('btn-tg-back-to-oneclick'),
    tgStepRequest: document.getElementById('tg-step-request'),
    tgStepVerify: document.getElementById('tg-step-verify'),
    tgStepProfile: document.getElementById('tg-step-profile'),
    formTgRequest: document.getElementById('form-tg-request'),
    tgInputUsername: document.getElementById('tg-input-username'),
    formTgVerify: document.getElementById('form-tg-verify'),
    tgInputCode: document.getElementById('tg-input-code'),
    tgTimerDisplay: document.getElementById('tg-timer-display'),
    btnTgBackToStep1: document.getElementById('btn-tg-back-to-step1'),
    formTgProfile: document.getElementById('form-tg-profile'),
    tgProfileName: document.getElementById('tg-profile-name'),
    tgProfilePhone: document.getElementById('tg-profile-phone'),

    // Athlete Workout
    athleteDateDisplay: document.getElementById('athlete-date-display'),
    athleteDatePicker: document.getElementById('athlete-date-picker'),
    btnAthleteDatePrev: document.getElementById('btn-athlete-date-prev'),
    btnAthleteDateNext: document.getElementById('btn-athlete-date-next'),
    athleteAssignmentNotice: document.getElementById('athlete-assignment-notice'),
    athleteAddSetBox: document.getElementById('athlete-add-set-box'),
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
    athleteAvatarPreview: document.getElementById('athlete-avatar-preview'),
    athleteAvatarInput: document.getElementById('athlete-avatar-input'),
    athleteEditName: document.getElementById('athlete-edit-name'),
    athleteEditPhone: document.getElementById('athlete-edit-phone'),
    btnSaveAthleteProfile: document.getElementById('btn-save-athlete-profile'),
    athletePairingPin: document.getElementById('athlete-pairing-pin'),
    athleteQrContainer: document.getElementById('athlete-qr-container'),
    btnRegeneratePin: document.getElementById('btn-regenerate-pin'),
    profilePrivacyToggle: document.getElementById('profile-privacy-toggle'),
    privacyStatusText: document.getElementById('privacy-status-text'),
    cardPairedCoach: document.getElementById('card-paired-coach'),
    pairedCoachAvatar: document.getElementById('paired-coach-avatar'),
    pairedCoachName: document.getElementById('paired-coach-name'),
    pairedCoachPhone: document.getElementById('paired-coach-phone'),
    btnCallCoach: document.getElementById('btn-call-coach'),
    btnUnpairCoach: document.getElementById('btn-unpair-coach'),
    btnLogoutAthlete: document.getElementById('btn-logout-athlete'),

    // Trainer Home
    trainerActiveClientName: document.getElementById('trainer-active-client-name'),
    trainerActiveClientAvatar: document.getElementById('trainer-active-client-avatar'),
    trainerClientSelect: document.getElementById('trainer-client-select'),
    formTrainerPair: document.getElementById('form-trainer-pair'),
    inputPairCode: document.getElementById('input-pairing-code'),
    inputQrFile: document.getElementById('input-qr-file'),
    trainerClientsList: document.getElementById('trainer-clients-list'),

    // Trainer Workout
    trainerWorkoutNotice: document.getElementById('trainer-workout-target-notice'),
    trainerDateDisplay: document.getElementById('trainer-date-display'),
    trainerDatePicker: document.getElementById('trainer-date-picker'),
    btnTrainerDatePrev: document.getElementById('btn-trainer-date-prev'),
    btnTrainerDateNext: document.getElementById('btn-trainer-date-next'),
    trainerAllowSelfWorkout: document.getElementById('trainer-allow-self-workout'),
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
    trainerFullName: document.getElementById('trainer-full-name'),
    trainerUsername: document.getElementById('trainer-username'),
    trainerPhone: document.getElementById('trainer-phone'),
    trainerAvatarPreview: document.getElementById('trainer-avatar-preview'),
    trainerAvatarInput: document.getElementById('trainer-avatar-input'),
    trainerEditName: document.getElementById('trainer-edit-name'),
    trainerEditPhone: document.getElementById('trainer-edit-phone'),
    btnSaveTrainerProfile: document.getElementById('btn-save-trainer-profile'),
    btnLogoutTrainer: document.getElementById('btn-logout-trainer'),

    // 2FA & Telegram Link Elements
    pinCountdownText: document.getElementById('pin-countdown-text'),
    athleteTgStatus: document.getElementById('athlete-tg-status'),
    btnLinkTgAthlete: document.getElementById('btn-link-tg-athlete'),
    athlete2faToggle: document.getElementById('athlete-2fa-toggle'),
    trainerTgStatus: document.getElementById('trainer-tg-status'),
    btnLinkTgTrainer: document.getElementById('btn-link-tg-trainer'),
    trainer2faToggle: document.getElementById('trainer-2fa-toggle'),
    dialog2faVerify: document.getElementById('dialog-2fa-verify'),
    login2faTimerDisplay: document.getElementById('login-2fa-timer-display'),
    formLogin2fa: document.getElementById('form-login-2fa'),
    login2faInputCode: document.getElementById('login-2fa-input-code'),
    dialogLinkTelegram: document.getElementById('dialog-link-telegram'),
    formLinkTgRequest: document.getElementById('form-link-tg-request'),
    linkTgInputUsername: document.getElementById('link-tg-input-username'),
    linkStep1: document.getElementById('link-step-1'),
    linkStep2: document.getElementById('link-step-2'),
    linkTgTimerDisplay: document.getElementById('link-tg-timer-display'),
    formLinkTgConfirm: document.getElementById('form-link-tg-confirm'),
    linkTgInputCode: document.getElementById('link-tg-input-code'),
    btnLinkTgBack: document.getElementById('btn-link-tg-back'),
    btnLinkTgDeeplink: document.getElementById('btn-link-tg-deeplink'),
    btnOpenTgBotLink: document.getElementById('btn-open-tg-bot-link'),

    // Progress & 3-Scale Charts & Weight
    athleteCurrentWeightVal: document.getElementById('athlete-current-weight-val'),
    athleteStartWeightVal: document.getElementById('athlete-start-weight-val'),
    athleteWeightCanvas: document.getElementById('athlete-weight-canvas'),
    athleteWeightEmpty: document.getElementById('athlete-weight-empty'),
    athleteExerciseCanvas: document.getElementById('athlete-exercise-canvas'),
    athleteExerciseChartEmpty: document.getElementById('athlete-exercise-chart-empty'),
    scaleMaxWeight: document.getElementById('scale-max-weight'),
    scaleTotalSets: document.getElementById('scale-total-sets'),
    scaleAvgReps: document.getElementById('scale-avg-reps'),
    btnOpenAddWeight: document.getElementById('btn-open-add-weight'),
    dialogAddWeight: document.getElementById('dialog-add-weight'),
    btnCloseWeightDialog: document.getElementById('btn-close-weight-dialog'),
    btnCancelWeightDialog: document.getElementById('btn-cancel-weight-dialog'),
    formAddWeight: document.getElementById('form-add-weight'),
    inputWeightDate: document.getElementById('input-weight-date'),
    inputWeightValue: document.getElementById('input-weight-value'),

    // Trainer Progress & Charts
    trainerCurrentWeightVal: document.getElementById('trainer-current-weight-val'),
    trainerStartWeightVal: document.getElementById('trainer-start-weight-val'),
    trainerWeightCanvas: document.getElementById('trainer-weight-canvas'),
    trainerWeightEmpty: document.getElementById('trainer-weight-empty'),
    trainerExerciseCanvas: document.getElementById('trainer-exercise-canvas'),
    trainerExerciseChartEmpty: document.getElementById('trainer-exercise-chart-empty'),
    trainerScaleMaxWeight: document.getElementById('trainer-scale-max-weight'),
    trainerScaleTotalSets: document.getElementById('trainer-scale-total-sets'),
    trainerScaleAvgReps: document.getElementById('trainer-scale-avg-reps'),
    btnTrainerAddWeight: document.getElementById('btn-trainer-add-weight'),

    // PWA & Mobile App Download
    pwaInstallBanner: document.getElementById('pwa-install-banner'),
    btnPwaInstall: document.getElementById('btn-pwa-install'),
    btnPwaDismiss: document.getElementById('btn-pwa-dismiss'),
    btnOpenDownloadModal: document.getElementById('btn-open-download-modal'),
    dialogDownloadApp: document.getElementById('dialog-download-app'),
    btnCloseDownloadDialog: document.getElementById('btn-close-download-dialog'),
    btnTriggerPwaInstallDialog: document.getElementById('btn-trigger-pwa-install-dialog')
  };

  // --- API Client Helper ---
  async function api(path, options = {}) {
    const headers = {
      'Content-Type': 'application/json',
      ...(options.headers || {})
    };

    if (state.token) {
      headers['Authorization'] = `Bearer ${state.token}`;
    }

    let response;
    try {
      response = await fetch(path, { ...options, headers });
    } catch (err) {
      showToast('Ошибка сети. Проверьте соединение с сервером', 'error');
      throw err;
    }

    let json = null;
    try {
      json = await response.json();
    } catch (_) {}

    if (!response.ok) {
      const errMsg = json?.error || `Ошибка сервера (${response.status})`;
      if (response.status === 401 && path !== '/api/login' && path !== '/api/register') {
        logout(true);
        throw new Error('Сессия завершена. Пожалуйста, войдите снова.');
      }
      showToast(errMsg, 'error');
      throw new Error(errMsg);
    }

    return json;
  }

  // --- Toast Notifications ---
  function showToast(message, type = 'info') {
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.textContent = message;
    el.toastContainer.appendChild(toast);
    setTimeout(() => {
      toast.style.opacity = '0';
      toast.style.transform = 'translateY(10px)';
      toast.style.transition = 'all 0.2s';
      setTimeout(() => toast.remove(), 200);
    }, 3000);
  }

  // --- Image Resizing for Safe CursorWindow SQLite & Cloud Parity (<15 KB) ---
  function resizeImageFile(file, maxWidth = 128, maxHeight = 128) {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = (e) => {
        const img = new Image();
        img.onload = () => {
          let w = img.width;
          let h = img.height;
          if (w > h) {
            if (w > maxWidth) {
              h = Math.round((h * maxWidth) / w);
              w = maxWidth;
            }
          } else {
            if (h > maxHeight) {
              w = Math.round((w * maxHeight) / h);
              h = maxHeight;
            }
          }
          const canvas = document.createElement('canvas');
          canvas.width = w;
          canvas.height = h;
          const ctx = canvas.getContext('2d');
          ctx.drawImage(img, 0, 0, w, h);
          resolve(canvas.toDataURL('image/jpeg', 0.75));
        };
        img.onerror = reject;
        img.src = e.target.result;
      };
      reader.onerror = reject;
      reader.readAsDataURL(file);
    });
  }

  // --- Auth & Session Lifecycle ---
  async function initAuth() {
    if (!state.token) {
      showAuthScreen();
      return;
    }

    try {
      const data = await api('/api/me');
      state.user = data.user;
      state.pairedCoach = data.pairedCoach || null;
      setupAppForRole(state.user.role);
    } catch (err) {
      // Only reset session if server explicitly responded with 401 Unauthorized
      if (err?.message?.includes('401') || err?.message?.includes('Сессия завершена')) {
        logout(false);
      } else {
        // On offline/transient network errors, keep cached session alive
        console.warn('[Fitness Pro] Offline or network error during initAuth:', err.message);
      }
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
    state.athleteAvatarBase64 = null;
    state.trainerAvatarBase64 = null;
    saveAuthToken(null);
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
    if (el.btnSyncHeader) el.btnSyncHeader.style.display = 'none';
    el.topbarTitle.textContent = 'FITNESS PRO';
    el.topbarUserName.textContent = 'Гость';
    el.topbarRoleBadge.textContent = '';
    el.topbarRoleBadge.style.display = 'none';
    el.topbarAvatar.innerHTML = `<svg width="22" height="22" viewBox="0 0 24 24" fill="currentColor"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>`;
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

  // --- Setup App Shell based on Role (Strictly Immutable) ---
  function setupAppForRole(role) {
    el.screenAuth.style.display = 'none';
    el.bottomNav.style.display = 'flex';
    el.btnLogoutHeader.style.display = 'flex';
    if (el.btnSyncHeader) el.btnSyncHeader.style.display = 'inline-flex';

    const displayName = state.user.fullName || state.user.full_name || state.user.username;
    el.topbarUserName.textContent = displayName;

    if (state.user.avatar_base64 || state.user.avatarBase64) {
      const b64 = state.user.avatar_base64 || state.user.avatarBase64;
      el.topbarAvatar.innerHTML = `<img src="${b64}" style="width:100%;height:100%;object-fit:cover;border-radius:9999px;">`;
    }

    if (role === 'trainer') {
      el.topbarTitle.textContent = 'TRAINER PRO';
      el.topbarRoleBadge.textContent = 'Тренер';
      el.topbarRoleBadge.style.display = 'inline-block';
      buildTrainerBottomNav();
      navigateToTab('trainer-home');
      loadTrainerClients();
    } else {
      el.topbarTitle.textContent = 'ATHLETE PRO';
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
      <div class="nav-items-wrapper">
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
      </div>
    `;
    setupBottomNavEvents();
  }

  function buildTrainerBottomNav() {
    el.bottomNav.innerHTML = `
      <div class="nav-items-wrapper">
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
            <svg viewBox="0 0 24 24"><path d="M3.5 18.49l6-6.01 4 4L22 6.92l-1.41-1.41-7.09 7.97-4-4L2 16.99z"/></svg>
          </span>
          <span class="nav-item-label">История</span>
        </button>
        <button class="nav-item" data-tab="trainer-settings">
          <span class="nav-item-icon">
            <svg viewBox="0 0 24 24"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
          </span>
          <span class="nav-item-label">Настройки</span>
        </button>
      </div>
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

  function navigateToTab(tabName) {
    state.activeTab = tabName;
    hideAllScreens();

    el.bottomNav.querySelectorAll('.nav-item').forEach(btn => {
      btn.classList.toggle('active', btn.dataset.tab === tabName);
    });

    switch (tabName) {
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
      const isSelfAllowed = Boolean(data.isSelfAllowed);

      // Strict athlete permissions:
      // Athlete cannot create workouts unless trainer explicitly allowed self workout
      if (el.athleteAddSetBox) {
        el.athleteAddSetBox.style.display = isSelfAllowed ? 'block' : 'none';
      }
      if (el.athleteAssignmentNotice) {
        el.athleteAssignmentNotice.style.display = isSelfAllowed ? 'none' : 'flex';
      }

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
          <div style="font-size: 14px; font-weight: 700; color: var(--text-secondary);">На этот день упражнений нет</div>
          <div style="font-size: 12px; color: var(--text-muted); margin-top: 4px;">План формируется вашим тренером</div>
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
            <button class="set-check-btn ${isComp ? 'completed' : ''}" data-id="${set.id}" title="Отметить выполнение">
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

  // --- Chart Drawing Engine (Pure HTML5 Canvas / Jetpack Compose LineChart Parity) ---

  /**
   * Draw Body Weight Progress Line Chart
   */
  function drawWeightChart(canvas, emptyEl, historyData) {
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    const width = canvas.width;
    const height = canvas.height;
    ctx.clearRect(0, 0, width, height);

    if (!historyData || historyData.length < 2) {
      if (emptyEl) emptyEl.style.display = 'block';
      canvas.style.display = 'none';
      return;
    }
    if (emptyEl) emptyEl.style.display = 'none';
    canvas.style.display = 'block';

    const padding = { top: 25, bottom: 35, left: 45, right: 25 };
    const chartW = width - padding.left - padding.right;
    const chartH = height - padding.top - padding.bottom;

    const weights = historyData.map(d => Number(d.weight_kg) || 0);
    const minWeight = Math.floor(Math.min(...weights) - 1);
    const maxWeight = Math.ceil(Math.max(...weights) + 1);
    const range = Math.max(1, maxWeight - minWeight);

    // Draw horizontal grid lines & labels
    ctx.strokeStyle = 'rgba(255, 255, 255, 0.08)';
    ctx.lineWidth = 1;
    ctx.fillStyle = '#71717a';
    ctx.font = '10px system-ui, sans-serif';
    ctx.textAlign = 'right';

    for (let i = 0; i <= 3; i++) {
      const y = padding.top + (chartH * (i / 3));
      const val = (maxWeight - (range * (i / 3))).toFixed(1);

      ctx.beginPath();
      ctx.moveTo(padding.left, y);
      ctx.lineTo(width - padding.right, y);
      ctx.stroke();

      ctx.fillText(`${val} кг`, padding.left - 6, y + 3);
    }

    // Coordinates of points
    const stepX = chartW / (historyData.length - 1);
    const points = historyData.map((d, idx) => {
      const x = padding.left + (idx * stepX);
      const y = padding.top + chartH - (((Number(d.weight_kg) - minWeight) / range) * chartH);
      return { x, y, weight: d.weight_kg, date: d.date };
    });

    // Area Gradient Fill
    const grad = ctx.createLinearGradient(0, padding.top, 0, height - padding.bottom);
    grad.addColorStop(0, 'rgba(200, 255, 0, 0.25)');
    grad.addColorStop(1, 'rgba(200, 255, 0, 0.0)');

    ctx.beginPath();
    ctx.moveTo(points[0].x, height - padding.bottom);
    points.forEach(pt => ctx.lineTo(pt.x, pt.y));
    ctx.lineTo(points[points.length - 1].x, height - padding.bottom);
    ctx.closePath();
    ctx.fillStyle = grad;
    ctx.fill();

    // Smooth Line
    ctx.beginPath();
    ctx.strokeStyle = '#c8ff00';
    ctx.lineWidth = 3;
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';
    points.forEach((pt, idx) => {
      if (idx === 0) ctx.moveTo(pt.x, pt.y);
      else ctx.lineTo(pt.x, pt.y);
    });
    ctx.stroke();

    // Data Circles & Date Labels
    ctx.textAlign = 'center';
    points.forEach((pt, idx) => {
      ctx.fillStyle = '#c8ff00';
      ctx.beginPath();
      ctx.arc(pt.x, pt.y, 4.5, 0, Math.PI * 2);
      ctx.fill();
      ctx.strokeStyle = '#0d0d0d';
      ctx.lineWidth = 1.5;
      ctx.stroke();

      // Show dates for first, last, or every few points
      if (idx === 0 || idx === points.length - 1 || idx % Math.ceil(points.length / 4) === 0) {
        ctx.fillStyle = '#a1a1aa';
        const shortDate = pt.date ? pt.date.slice(5) : '';
        ctx.fillText(shortDate, pt.x, height - 10);
      }
    });
  }

  /**
   * Draw Multi-Scale Exercise Progress Chart (3 Scales: Weight, Sets, Reps)
   */
  function drawExerciseMultiScaleChart(canvas, emptyEl, timelineData, scaleElements = {}) {
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    const width = canvas.width;
    const height = canvas.height;
    ctx.clearRect(0, 0, width, height);

    if (!timelineData || timelineData.length === 0) {
      if (emptyEl) emptyEl.style.display = 'block';
      canvas.style.display = 'none';
      if (scaleElements.maxWeight) scaleElements.maxWeight.textContent = '— кг';
      if (scaleElements.totalSets) scaleElements.totalSets.textContent = '—';
      if (scaleElements.avgReps) scaleElements.avgReps.textContent = '—';
      return;
    }
    if (emptyEl) emptyEl.style.display = 'none';
    canvas.style.display = 'block';

    // Group timeline data by workout date
    const byDate = new Map();
    timelineData.forEach(s => {
      const d = s.date || 'Дата';
      if (!byDate.has(d)) byDate.set(d, []);
      byDate.get(d).push(s);
    });

    const dates = Array.from(byDate.keys());
    const dayStats = dates.map(d => {
      const sets = byDate.get(d);
      const maxW = Math.max(...sets.map(s => Number(s.weight_kg) || 0));
      const totalSetsCount = sets.length;
      const avgR = Math.round(sets.reduce((sum, s) => sum + (Number(s.reps) || 0), 0) / totalSetsCount);
      return { date: d, maxWeight: maxW, setsCount: totalSetsCount, avgReps: avgR };
    });

    // Compute Summary Values for 3 Scales Badges
    const overallMaxWeight = Math.max(...dayStats.map(s => s.maxWeight));
    const overallTotalSets = timelineData.length;
    const overallAvgReps = Math.round(timelineData.reduce((sum, s) => sum + (Number(s.reps) || 0), 0) / (timelineData.length || 1));

    if (scaleElements.maxWeight) scaleElements.maxWeight.textContent = `${overallMaxWeight} кг`;
    if (scaleElements.totalSets) scaleElements.totalSets.textContent = `${overallTotalSets} подх`;
    if (scaleElements.avgReps) scaleElements.avgReps.textContent = `${overallAvgReps} повт`;

    const padding = { top: 30, bottom: 40, left: 45, right: 25 };
    const chartW = width - padding.left - padding.right;
    const chartH = height - padding.top - padding.bottom;

    // Scale 1: Weight (0 .. maxWeight + 10%)
    const maxScaleWeight = Math.max(10, Math.ceil(overallMaxWeight * 1.15));
    // Scale 2 & 3: Sets and Reps (0 .. maxReps + 2)
    const maxRepVal = Math.max(10, Math.max(...dayStats.map(s => Math.max(s.setsCount, s.avgReps))) + 2);

    // Draw horizontal grid lines
    ctx.strokeStyle = 'rgba(255, 255, 255, 0.08)';
    ctx.lineWidth = 1;
    ctx.fillStyle = '#71717a';
    ctx.font = '10px system-ui, sans-serif';
    ctx.textAlign = 'right';

    for (let i = 0; i <= 3; i++) {
      const y = padding.top + (chartH * (i / 3));
      const wVal = Math.round(maxScaleWeight * (1 - i / 3));

      ctx.beginPath();
      ctx.moveTo(padding.left, y);
      ctx.lineTo(width - padding.right, y);
      ctx.stroke();

      ctx.fillText(`${wVal} кг`, padding.left - 6, y + 3);
    }

    const n = dayStats.length;
    const stepX = n > 1 ? chartW / (n - 1) : 0;

    const pointsWeight = [];
    const pointsSets = [];
    const pointsReps = [];

    dayStats.forEach((st, idx) => {
      const x = n > 1 ? padding.left + (idx * stepX) : padding.left + (chartW / 2);
      const yWeight = padding.top + chartH - ((st.maxWeight / maxScaleWeight) * chartH);
      const ySets = padding.top + chartH - ((st.setsCount / maxRepVal) * chartH);
      const yReps = padding.top + chartH - ((st.avgReps / maxRepVal) * chartH);

      pointsWeight.push({ x, y: yWeight, val: st.maxWeight, date: st.date });
      pointsSets.push({ x, y: ySets, val: st.setsCount, date: st.date });
      pointsReps.push({ x, y: yReps, val: st.avgReps, date: st.date });
    });

    // Helper: Draw curve
    function drawSeries(points, color, strokeW = 2.5) {
      if (points.length === 0) return;
      ctx.beginPath();
      ctx.strokeStyle = color;
      ctx.lineWidth = strokeW;
      ctx.lineCap = 'round';
      ctx.lineJoin = 'round';
      points.forEach((pt, idx) => {
        if (idx === 0) ctx.moveTo(pt.x, pt.y);
        else ctx.lineTo(pt.x, pt.y);
      });
      ctx.stroke();

      // Circles
      ctx.fillStyle = color;
      points.forEach(pt => {
        ctx.beginPath();
        ctx.arc(pt.x, pt.y, 4, 0, Math.PI * 2);
        ctx.fill();
        ctx.strokeStyle = '#0d0d0d';
        ctx.lineWidth = 1.5;
        ctx.stroke();
      });
    }

    // 1. Draw Sets Series (Cyan #38bdf8)
    drawSeries(pointsSets, '#38bdf8', 2);
    // 2. Draw Reps Series (Rose #f43f5e)
    drawSeries(pointsReps, '#f43f5e', 2);
    // 3. Draw Weight Series (Neon Lime #c8ff00)
    drawSeries(pointsWeight, '#c8ff00', 3);

    // Draw Date Axis Labels
    ctx.textAlign = 'center';
    ctx.fillStyle = '#a1a1aa';
    pointsWeight.forEach((pt, idx) => {
      if (idx === 0 || idx === pointsWeight.length - 1 || idx % Math.ceil(pointsWeight.length / 5) === 0) {
        const shortDate = pt.date ? pt.date.slice(5) : '';
        ctx.fillText(shortDate, pt.x, height - 12);
      }
    });
  }

  // --- ATHLETE: Progress & History (Charts & Logs) ---
  async function loadAthleteHistory() {
    try {
      // 1. Load Body Weight History
      loadAthleteWeightProgress();

      // 2. Load Athlete Exercises List for Dropdown
      const exRes = await api('/api/progress/exercises');
      const exercises = exRes.exercises || [];

      el.athleteHistorySelect.innerHTML = '<option value="">Выберите упражнение для графика...</option>';
      exercises.forEach(name => {
        const opt = document.createElement('option');
        opt.value = name;
        opt.textContent = name;
        el.athleteHistorySelect.appendChild(opt);
      });

      // Select default exercise if available
      if (exercises.length > 0) {
        if (!state.athleteSelectedExercise || !exercises.includes(state.athleteSelectedExercise)) {
          state.athleteSelectedExercise = exercises[0];
        }
        el.athleteHistorySelect.value = state.athleteSelectedExercise;
        loadAthleteExerciseTimeline(state.athleteSelectedExercise);
      } else {
        drawExerciseMultiScaleChart(el.athleteExerciseCanvas, el.athleteExerciseChartEmpty, [], {
          maxWeight: el.scaleMaxWeight,
          totalSets: el.scaleTotalSets,
          avgReps: el.scaleAvgReps
        });
      }

      // 3. Load Recent Sets for table/history list
      const data = await api(`/api/workout?date=${state.currentDate}`);
      renderHistoryItems(data.sets || []);
    } catch (err) {
      console.warn('Failed loading athlete progress:', err);
    }
  }

  async function loadAthleteWeightProgress() {
    try {
      const res = await api('/api/progress/anthropometry');
      const history = res.history || [];

      if (history.length > 0) {
        const latest = history[history.length - 1];
        const start = history[0];
        el.athleteCurrentWeightVal.textContent = `${Number(latest.weight_kg).toFixed(1)} кг`;
        el.athleteStartWeightVal.textContent = `${Number(start.weight_kg).toFixed(1)} кг`;
      } else {
        el.athleteCurrentWeightVal.textContent = '—';
        el.athleteStartWeightVal.textContent = '—';
      }

      drawWeightChart(el.athleteWeightCanvas, el.athleteWeightEmpty, history);
    } catch {}
  }

  async function loadAthleteExerciseTimeline(exerciseName) {
    if (!exerciseName) {
      drawExerciseMultiScaleChart(el.athleteExerciseCanvas, el.athleteExerciseChartEmpty, [], {
        maxWeight: el.scaleMaxWeight,
        totalSets: el.scaleTotalSets,
        avgReps: el.scaleAvgReps
      });
      return;
    }
    try {
      const res = await api(`/api/progress/exercise?exercise=${encodeURIComponent(exerciseName)}`);
      const timeline = res.timeline || [];
      drawExerciseMultiScaleChart(el.athleteExerciseCanvas, el.athleteExerciseChartEmpty, timeline, {
        maxWeight: el.scaleMaxWeight,
        totalSets: el.scaleTotalSets,
        avgReps: el.scaleAvgReps
      });
    } catch {}
  }

  function renderHistoryItems(sets) {
    el.athleteHistoryList.innerHTML = '';
    if (sets.length === 0) {
      el.athleteHistoryList.innerHTML = `
        <div class="app-card" style="text-align: center; color: var(--text-muted); font-size: 13px;">
          История тренировок за выбранную дату пуста.
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
      const myName = (state.user?.fullName || state.user?.full_name || state.user?.username || '').toLowerCase();
      const meInBoard = state.leaderboard.find(u => (u.name || '').toLowerCase() === myName);

      const workouts = meInBoard ? meInBoard.workoutsCount || 0 : 0;
      const tonnageKg = meInBoard ? meInBoard.totalTonnage || 0 : 0;
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
          Нет завершенных тренировок в состязании.
        </div>
      `;
      return;
    }

    state.leaderboard.forEach((entry, idx) => {
      const myName = (state.user?.fullName || state.user?.full_name || state.user?.username || '').toLowerCase();
      const isMe = (entry.name || '').toLowerCase() === myName;
      const rank = entry.rank || (idx + 1);
      const row = document.createElement('div');
      row.className = `leaderboard-row ${isMe ? 'is-me' : ''}`;

      let rankClass = '';
      if (rank === 1) rankClass = 'rank-1';
      else if (rank === 2) rankClass = 'rank-2';
      else if (rank === 3) rankClass = 'rank-3';

      const cleanName = String(entry.name || '').replace(/\s*\((Web|Mobile Athlete|Mobile)\)/gi, '').trim();
      const displayName = isMe ? `${escapeHtml(cleanName)} (ВЫ)` : escapeHtml(cleanName);
      const tonnage = entry.totalTonnage || entry.tonnage_kg || 0;
      const tonnageStr = tonnage >= 1000 ? `${(tonnage / 1000).toFixed(1)} т` : `${tonnage} кг`;

      row.innerHTML = `
        <div class="rank-badge ${rankClass}">${rank}</div>
        <div class="athlete-meta">
          <div class="athlete-name-text">${displayName}</div>
          <div class="athlete-stats-sub">${entry.workoutsCount || 0} трен · ${tonnageStr}</div>
        </div>
        <div class="athlete-points-pill tabular-nums">${entry.points || 0} pts</div>
      `;
      el.leaderboardList.appendChild(row);
    });
  }

  // --- ATHLETE: Profile & Pairing ---
  function renderAthleteProfile() {
    const fullName = state.user.fullName || state.user.full_name || state.user.username;
    el.profileFullName.textContent = fullName;
    el.profileUsername.textContent = `@${state.user.username}`;
    el.profilePhone.textContent = state.user.phone || 'Телефон не указан';

    if (el.athleteEditName) el.athleteEditName.value = state.user.fullName || state.user.full_name || '';
    if (el.athleteEditPhone) el.athleteEditPhone.value = state.user.phone || '';

    const avatarB64 = state.user.avatar_base64 || state.user.avatarBase64;
    if (avatarB64 && el.athleteAvatarPreview) {
      el.athleteAvatarPreview.innerHTML = `<img src="${avatarB64}" alt="Avatar">`;
    }

    // 6-digit PIN display
    const pin = state.user.pairingCode || state.user.pairing_code || '------';
    el.athletePairingPin.textContent = pin;

    // Vector SVG QR Code (Strictly QR, NO link)
    renderVectorQrCode(pin);

    // Privacy toggle
    const isPublic = !Boolean(state.user.is_private);
    el.profilePrivacyToggle.checked = isPublic;
    updatePrivacyStatusText(isPublic);

    // Paired coach card
    const coachName = state.pairedCoach?.full_name || state.user.coach_name || state.user.coachName;
    const coachPhone = state.pairedCoach?.phone || state.user.coach_phone || state.user.coachPhone;
    if (coachName) {
      el.cardPairedCoach.style.display = 'block';
      el.pairedCoachName.textContent = coachName;
      el.pairedCoachPhone.textContent = coachPhone || 'Телефон не указан';
      if (el.pairedCoachAvatar) {
        if (state.pairedCoach?.avatar_base64) {
          el.pairedCoachAvatar.innerHTML = `<img src="${state.pairedCoach.avatar_base64}" style="width:100%;height:100%;object-fit:cover;border-radius:9999px;">`;
        } else {
          el.pairedCoachAvatar.textContent = coachName.slice(0, 1).toUpperCase();
        }
      }
      if (el.btnCallCoach) {
        const cleanPhone = (coachPhone || '').replace(/[^\d+]/g, '');
        if (cleanPhone) {
          el.btnCallCoach.href = `tel:${cleanPhone}`;
          el.btnCallCoach.style.display = 'inline-flex';
        } else {
          el.btnCallCoach.style.display = 'none';
        }
      }
    } else {
      el.cardPairedCoach.style.display = 'none';
    }

    // 5-minute Live PIN Countdown Timer
    startPinCountdown();

    // Telegram and 2FA status
    renderTelegramAnd2FAStatus('athlete');
  }

  function startPinCountdown() {
    if (state.pinTimerInterval) {
      clearInterval(state.pinTimerInterval);
      state.pinTimerInterval = null;
    }

    const created = state.user?.pairingCodeCreatedAt || state.user?.pairing_code_created_at || Date.now();
    const updateCountdown = () => {
      const elapsed = Math.floor((Date.now() - created) / 1000);
      const remaining = Math.max(0, 300 - elapsed);
      const m = Math.floor(remaining / 60);
      const s = remaining % 60;
      const mm = String(m).padStart(2, '0');
      const ss = String(s).padStart(2, '0');

      if (el.pinCountdownText) {
        el.pinCountdownText.textContent = `${mm}:${ss}`;
      }

      if (remaining <= 0) {
        clearInterval(state.pinTimerInterval);
        state.pinTimerInterval = null;
        autoRegenerateAthletePin();
      }
    };

    updateCountdown();
    state.pinTimerInterval = setInterval(updateCountdown, 1000);
  }

  async function autoRegenerateAthletePin() {
    try {
      const data = await api('/api/athlete/regenerate-pin', { method: 'POST' });
      state.user.pairingCode = data.pairingCode;
      state.user.pairing_code = data.pairingCode;
      state.user.pairingCodeCreatedAt = data.pairingCodeCreatedAt || Date.now();
      state.user.pairing_code_created_at = data.pairingCodeCreatedAt || Date.now();
      renderAthleteProfile();
      showToast('PIN-код обновлен (действует 5 минут)', 'info');
    } catch (_) {}
  }

  function normalizeUser(u) {
    if (!u) return u;
    const tgUsername = u.telegram_username || u.telegramUsername || (u.username && u.username.startsWith('tg_') ? u.username.replace(/^tg_/, '') : '');
    const tgId = u.telegram_id || u.telegramId || '';
    const is2Fa = Boolean(u.two_factor_enabled === 1 || u.two_factor_enabled === true || u.twoFactorEnabled);
    u.telegram_username = tgUsername;
    u.telegramUsername = tgUsername;
    u.telegram_id = tgId;
    u.telegramId = tgId;
    u.two_factor_enabled = is2Fa ? 1 : 0;
    u.twoFactorEnabled = is2Fa;
    return u;
  }

  function renderTelegramAnd2FAStatus(role) {
    if (!state.user) return;
    normalizeUser(state.user);
    const tgUsername = state.user.telegram_username;
    const tgId = state.user.telegram_id;
    const isTgLinked = Boolean(tgUsername || tgId || (state.user.username && state.user.username.startsWith('tg_')));

    const statusEl = role === 'athlete' ? el.athleteTgStatus : el.trainerTgStatus;
    const toggleEl = role === 'athlete' ? el.athlete2faToggle : el.trainer2faToggle;
    const btnLink = role === 'athlete' ? el.btnLinkTgAthlete : el.btnLinkTgTrainer;

    if (statusEl) {
      if (isTgLinked) {
        statusEl.textContent = tgUsername ? `@${tgUsername}` : (tgId ? `ID: ${tgId}` : 'Привязан');
        statusEl.classList.add('linked');
      } else {
        statusEl.textContent = 'Не привязан';
        statusEl.classList.remove('linked');
      }
    }

    if (btnLink) {
      btnLink.textContent = isTgLinked ? '🔄 Сменить Telegram' : '✈ Привязать Telegram';
      btnLink.disabled = false;
    }

    if (toggleEl) {
      toggleEl.checked = Boolean(state.user.two_factor_enabled);
      toggleEl.disabled = !isTgLinked;
    }
  }

  function updatePrivacyStatusText(isPublic) {
    el.privacyStatusText.textContent = isPublic 
      ? 'Вы участвуете в состязаниях и ваш рейтинг виден'
      : 'Приватный режим включен. Ваш рейтинг скрыт';
  }

  async function renderVectorQrCode(pinCode) {
    if (!el.athleteQrContainer) return;
    el.athleteQrContainer.innerHTML = '';

    if (!pinCode || pinCode === '------') {
      el.athleteQrContainer.innerHTML = '<div style="color:var(--text-muted);font-size:12px;">Код формируется...</div>';
      return;
    }

    if (typeof window.generateQrSvg === 'function') {
      const svg = window.generateQrSvg(pinCode, { size: 200, color: '#000000', background: '#ffffff', margin: 4 });
      if (svg) {
        el.athleteQrContainer.innerHTML = svg;
        return;
      }
    }

    try {
      const res = await fetch(`/api/qr-svg?text=${encodeURIComponent(pinCode)}`);
      if (res.ok) {
        const svg = await res.text();
        el.athleteQrContainer.innerHTML = svg;
        return;
      }
    } catch (_) {}

    el.athleteQrContainer.innerHTML = `<div style="font-size:24px;font-weight:900;color:var(--accent-lime);">${pinCode}</div>`;
  }

  // --- TRAINER: Clients & Management ---
  async function loadTrainerClients() {
    try {
      const data = await api('/api/trainer/clients');
      state.clients = data.clients || [];
      renderTrainerClientsList();
      populateTrainerClientDropdown();
    } catch {}
  }

  function renderTrainerClientsList() {
    el.trainerClientsList.innerHTML = '';
    if (state.clients.length === 0) {
      el.trainerClientsList.innerHTML = `
        <div style="text-align: center; padding: 24px; color: var(--text-muted); font-size: 13px;">
          У вас пока нет привязанных подопечных.<br>Введите 6-значный код подопечного выше, чтобы подключить его.
        </div>
      `;
      return;
    }

    state.clients.forEach(c => {
      const isSelected = state.activeClientId === c.id;
      const card = document.createElement('div');
      card.className = `client-item-card ${isSelected ? 'selected' : ''}`;
      
      const initials = (c.full_name || c.username || 'А').slice(0, 2).toUpperCase();
      const avatarHtml = c.avatar_base64 
        ? `<img src="${c.avatar_base64}" style="width:100%;height:100%;object-fit:cover;border-radius:9999px;">`
        : initials;

      card.innerHTML = `
        <div class="client-item-left">
          <div class="client-avatar-badge">${avatarHtml}</div>
          <div>
            <div class="client-name">${escapeHtml(c.full_name || c.username)}</div>
            <div class="client-sub">${c.phone || `@${c.username}`} · ${c.sessions_count || 0} трен</div>
          </div>
        </div>
        <div class="client-item-actions">
          <button class="app-btn btn-primary btn-sm btn-select-client" data-id="${c.id}">
            ${isSelected ? 'Выбран' : 'Выбрать'}
          </button>
          <button class="app-btn btn-danger btn-sm btn-unpair-client" data-id="${c.id}" title="Отвязать">✕</button>
        </div>
      `;

      card.querySelector('.btn-select-client').onclick = () => selectTrainerClient(c.id);
      card.querySelector('.btn-unpair-client').onclick = () => unpairTrainerClient(c.id);
      el.trainerClientsList.appendChild(card);
    });
  }

  function populateTrainerClientDropdown() {
    el.trainerClientSelect.innerHTML = '<option value="">Выбрать...</option>';
    state.clients.forEach(c => {
      const opt = document.createElement('option');
      opt.value = c.id;
      opt.textContent = c.full_name || c.username;
      if (state.activeClientId === c.id) {
        opt.selected = true;
      }
      el.trainerClientSelect.appendChild(opt);
    });
  }

  function selectTrainerClient(clientId) {
    state.activeClientId = Number(clientId);
    const client = state.clients.find(c => c.id === state.activeClientId);
    const cardRestrictions = document.getElementById('card-trainer-restrictions');
    const inputRestrictions = document.getElementById('input-athlete-restrictions');

    if (client) {
      el.trainerActiveClientName.textContent = client.full_name || client.username;
      if (client.avatar_base64 && el.trainerActiveClientAvatar) {
        el.trainerActiveClientAvatar.innerHTML = `<img src="${client.avatar_base64}" style="width:100%;height:100%;object-fit:cover;border-radius:9999px;">`;
      }
      el.trainerWorkoutNotice.style.display = 'none';
      if (cardRestrictions && inputRestrictions) {
        cardRestrictions.style.display = 'block';
        inputRestrictions.value = client.restrictions || '';
      }
      populateTrainerClientDropdown();
      renderTrainerClientsList();
      showToast(`Выбран подопечный: ${client.full_name || client.username}`, 'info');
    } else {
      if (cardRestrictions) cardRestrictions.style.display = 'none';
    }
  }

  async function unpairTrainerClient(clientId) {
    if (!confirm('Отвязать этого подопечного?')) return;
    try {
      await api('/api/trainer/unpair', {
        method: 'POST',
        body: JSON.stringify({ athleteId: clientId })
      });
      if (state.activeClientId === clientId) {
        state.activeClientId = null;
        el.trainerActiveClientName.textContent = 'Нет подопечных';
      }
      showToast('Связь разорвана', 'info');
      loadTrainerClients();
    } catch {}
  }

  // --- TRAINER: Workout Assignment Logic ---
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
      if (el.trainerAllowSelfWorkout) {
        el.trainerAllowSelfWorkout.checked = Boolean(data.isSelfAllowed);
      }
      renderTrainerWorkoutMatrix();
    } catch {}
  }

  function renderTrainerWorkoutMatrix() {
    el.trainerWorkoutMatrix.innerHTML = '';
    if (state.trainerSets.length === 0) {
      el.trainerWorkoutMatrix.innerHTML = `
        <div class="app-card" style="text-align: center; padding: 24px 16px;">
          <div style="font-size: 14px; font-weight: 700; color: var(--text-secondary);">На этот день упражнений нет</div>
          <div style="font-size: 12px; color: var(--text-muted); margin-top: 4px;">Назначьте упражнение подопечному выше</div>
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
            <span class="status-pill ${isComp ? 'done' : 'pending'}">${isComp ? 'Сделано' : 'Назначено'}</span>
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

  // --- TRAINER: History & Charts ---
  async function loadTrainerHistory() {
    if (!state.activeClientId) {
      el.trainerHistoryList.innerHTML = `
        <div class="app-card" style="text-align: center; color: var(--text-muted); font-size: 13px;">
          Сначала выберите подопечного во вкладке «Подопечные».
        </div>
      `;
      return;
    }

    try {
      // 1. Client Weight Progress
      loadTrainerClientWeightProgress();

      // 2. Client Exercises Dropdown & Chart
      const exRes = await api(`/api/progress/exercises?athleteId=${state.activeClientId}`);
      const exercises = exRes.exercises || [];

      el.trainerHistorySelect.innerHTML = '<option value="">Выберите упражнение для графика...</option>';
      exercises.forEach(name => {
        const opt = document.createElement('option');
        opt.value = name;
        opt.textContent = name;
        el.trainerHistorySelect.appendChild(opt);
      });

      if (exercises.length > 0) {
        if (!state.trainerSelectedExercise || !exercises.includes(state.trainerSelectedExercise)) {
          state.trainerSelectedExercise = exercises[0];
        }
        el.trainerHistorySelect.value = state.trainerSelectedExercise;
        loadTrainerExerciseTimeline(state.trainerSelectedExercise);
      } else {
        drawExerciseMultiScaleChart(el.trainerExerciseCanvas, el.trainerExerciseChartEmpty, [], {
          maxWeight: el.trainerScaleMaxWeight,
          totalSets: el.trainerScaleTotalSets,
          avgReps: el.trainerScaleAvgReps
        });
      }

      // 3. Client sets for table
      const data = await api(`/api/workout?athleteId=${state.activeClientId}&date=${state.trainerDate}`);
      renderTrainerHistoryItems(data.sets || []);
    } catch (err) {
      console.warn('Failed loading trainer client history:', err);
    }
  }

  async function loadTrainerClientWeightProgress() {
    if (!state.activeClientId) return;
    try {
      const res = await api(`/api/progress/anthropometry?athleteId=${state.activeClientId}`);
      const history = res.history || [];

      if (history.length > 0) {
        const latest = history[history.length - 1];
        const start = history[0];
        if (el.trainerCurrentWeightVal) el.trainerCurrentWeightVal.textContent = `${Number(latest.weight_kg).toFixed(1)} кг`;
        if (el.trainerStartWeightVal) el.trainerStartWeightVal.textContent = `${Number(start.weight_kg).toFixed(1)} кг`;
      } else {
        if (el.trainerCurrentWeightVal) el.trainerCurrentWeightVal.textContent = '—';
        if (el.trainerStartWeightVal) el.trainerStartWeightVal.textContent = '—';
      }

      drawWeightChart(el.trainerWeightCanvas, el.trainerWeightEmpty, history);
    } catch {}
  }

  async function loadTrainerExerciseTimeline(exerciseName) {
    if (!state.activeClientId || !exerciseName) {
      drawExerciseMultiScaleChart(el.trainerExerciseCanvas, el.trainerExerciseChartEmpty, [], {
        maxWeight: el.trainerScaleMaxWeight,
        totalSets: el.trainerScaleTotalSets,
        avgReps: el.trainerScaleAvgReps
      });
      return;
    }
    try {
      const res = await api(`/api/progress/exercise?athleteId=${state.activeClientId}&exercise=${encodeURIComponent(exerciseName)}`);
      const timeline = res.timeline || [];
      drawExerciseMultiScaleChart(el.trainerExerciseCanvas, el.trainerExerciseChartEmpty, timeline, {
        maxWeight: el.trainerScaleMaxWeight,
        totalSets: el.trainerScaleTotalSets,
        avgReps: el.trainerScaleAvgReps
      });
    } catch {}
  }

  function renderTrainerHistoryItems(sets) {
    el.trainerHistoryList.innerHTML = '';
    if (sets.length === 0) {
      el.trainerHistoryList.innerHTML = `
        <div class="app-card" style="text-align: center; color: var(--text-muted); font-size: 13px;">
          История тренировок подопечного за выбранную дату пуста.
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

  // --- TRAINER: Settings & Profile Rendering ---
  function renderTrainerSettings() {
    const fullName = state.user.fullName || state.user.full_name || state.user.username;
    if (el.trainerFullName) el.trainerFullName.textContent = fullName;
    if (el.trainerUsername) el.trainerUsername.textContent = `@${state.user.username}`;
    if (el.trainerPhone) el.trainerPhone.textContent = state.user.phone || 'Телефон не указан';

    if (el.trainerEditName) el.trainerEditName.value = state.user.fullName || state.user.full_name || '';
    if (el.trainerEditPhone) el.trainerEditPhone.value = state.user.phone || '';

    const avatarB64 = state.user.avatar_base64 || state.user.avatarBase64;
    if (avatarB64 && el.trainerAvatarPreview) {
      el.trainerAvatarPreview.innerHTML = `<img src="${avatarB64}" alt="Avatar">`;
    }

    // Telegram and 2FA status
    renderTelegramAnd2FAStatus('trainer');
  }

  // --- Event Listeners Setup ---
  function setupEvents() {
    // Topbar & Logout
    el.btnLogoutHeader.onclick = () => logout(true);
    el.btnLogoutAthlete.onclick = () => logout(true);
    el.btnLogoutTrainer.onclick = () => logout(true);

    // Auth Switcher tabs
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

    // Role selector card toggles
    document.querySelectorAll('.role-card').forEach(card => {
      card.onclick = () => {
        document.querySelectorAll('.role-card').forEach(c => c.classList.remove('selected'));
        card.classList.add('selected');
        const radio = card.querySelector('input[type="radio"]');
        if (radio) radio.checked = true;
      };
    });

    // Login Form Submit (with 2FA support)
    el.formLogin.onsubmit = async (e) => {
      e.preventDefault();
      const username = el.loginUsername.value.trim();
      const password = el.loginPassword.value;
      if (!username || !password) return;

      try {
        const data = await api('/api/login', {
          method: 'POST',
          body: JSON.stringify({ username, password })
        });

        if (data.require2FA) {
          state.pending2FAUserId = data.userId;
          open2FALoginModal(data.expiresInSeconds || 300);
          showToast(data.message || 'Введите 6-значный код 2FA из Telegram', 'info');
          return;
        }

        state.user = data.user;
        saveAuthToken(data.token);
        setupAppForRole(data.user.role);
        showToast(`Добро пожаловать, ${data.user.fullName || data.user.username}!`, 'success');
      } catch {}
    };

    // 2FA Verification Modal Handlers
    function open2FALoginModal(durationSeconds = 300) {
      if (el.login2faInputCode) el.login2faInputCode.value = '';
      if (state.login2faTimerInterval) clearInterval(state.login2faTimerInterval);
      let remaining = durationSeconds;
      const updateBadge = () => {
        const m = Math.floor(remaining / 60);
        const s = remaining % 60;
        const mm = String(m).padStart(2, '0');
        const ss = String(s).padStart(2, '0');
        if (el.login2faTimerDisplay) {
          el.login2faTimerDisplay.textContent = `⏱ Действует: ${mm}:${ss}`;
        }
        if (remaining <= 0) {
          clearInterval(state.login2faTimerInterval);
          state.login2faTimerInterval = null;
          if (el.login2faTimerDisplay) {
            el.login2faTimerDisplay.textContent = '❌ Срок действия кода истек (5 минут)';
          }
        }
        remaining--;
      };
      updateBadge();
      state.login2faTimerInterval = setInterval(updateBadge, 1000);
      el.dialog2faVerify?.showModal();
      el.login2faInputCode?.focus();
    }

    if (el.formLogin2fa) {
      el.formLogin2fa.onsubmit = async (e) => {
        e.preventDefault();
        const code = el.login2faInputCode.value.trim();
        if (!code || !state.pending2FAUserId) return;
        try {
          const data = await api('/api/login/2fa', {
            method: 'POST',
            body: JSON.stringify({ userId: state.pending2FAUserId, code })
          });
          if (state.login2faTimerInterval) clearInterval(state.login2faTimerInterval);
          el.dialog2faVerify?.close();
          state.user = data.user;
          saveAuthToken(data.token);
          setupAppForRole(data.user.role);
          showToast(`Вход выполнен: ${data.user.fullName || data.user.username}!`, 'success');
        } catch {}
      };
    }

    document.querySelectorAll('.btn-close-2fa').forEach(btn => {
      btn.onclick = () => {
        if (state.login2faTimerInterval) clearInterval(state.login2faTimerInterval);
        el.dialog2faVerify?.close();
      };
    });

    // Telegram Account Linking Modal Handlers
    function openLinkTelegramModal() {
      if (el.linkStep1) el.linkStep1.style.display = 'block';
      if (el.linkStep2) el.linkStep2.style.display = 'none';
      if (el.linkTgInputUsername) el.linkTgInputUsername.value = '';
      if (el.linkTgInputCode) el.linkTgInputCode.value = '';
      el.dialogLinkTelegram?.showModal();
      el.linkTgInputUsername?.focus();
    }

    if (el.btnLinkTgAthlete) el.btnLinkTgAthlete.onclick = openLinkTelegramModal;
    if (el.btnLinkTgTrainer) el.btnLinkTgTrainer.onclick = openLinkTelegramModal;

    document.querySelectorAll('.btn-close-link-tg').forEach(btn => {
      btn.onclick = () => {
        if (state.linkTgTimerInterval) clearInterval(state.linkTgTimerInterval);
        el.dialogLinkTelegram?.close();
      };
    });

    if (el.btnLinkTgDeeplink) {
      el.btnLinkTgDeeplink.onclick = async () => {
        try {
          const res = await api('/api/user/telegram/link-token', { method: 'POST' });
          if (res.deepLink) {
            window.open(res.deepLink, '_blank');
            showToast('Открываем Telegram бота... Нажмите START для привязки', 'info');

            // Automatic polling while user is in Telegram
            if (state.linkPollingInterval) clearInterval(state.linkPollingInterval);
            let pollAttempts = 0;
            state.linkPollingInterval = setInterval(async () => {
              pollAttempts++;
              if (pollAttempts > 60 || !state.token) {
                clearInterval(state.linkPollingInterval);
                return;
              }
              try {
                const check = await api('/api/me');
                if (check && check.user && (check.user.telegram_id || check.user.telegram_username)) {
                  clearInterval(state.linkPollingInterval);
                  state.user = normalizeUser(check.user);
                  el.dialogLinkTelegram?.close();
                  if (state.user.role === 'athlete') renderAthleteProfile();
                  else renderTrainerSettings();
                  showToast('✅ Telegram успешно привязан!', 'success');
                }
              } catch (_) {}
            }, 2000);
          } else if (res.token) {
            showToast(`Токен: ${res.token}. Отправьте в бота: /link ${res.token}`, 'info');
          }
        } catch (_) {}
      };
    }

    if (el.btnLinkTgBack) {
      el.btnLinkTgBack.onclick = () => {
        if (state.linkTgTimerInterval) clearInterval(state.linkTgTimerInterval);
        if (el.linkStep1) el.linkStep1.style.display = 'block';
        if (el.linkStep2) el.linkStep2.style.display = 'none';
      };
    }

    if (el.formLinkTgRequest) {
      el.formLinkTgRequest.onsubmit = async (e) => {
        e.preventDefault();
        const username = el.linkTgInputUsername.value.trim();
        if (!username) return;
        try {
          const res = await api('/api/user/telegram/link-request', {
            method: 'POST',
            body: JSON.stringify({ username })
          });
          state.linkTgPendingUsername = username;
          if (res.botLink && el.btnOpenTgBotLink) {
            el.btnOpenTgBotLink.href = res.botLink;
          } else if (res.botUsername && el.btnOpenTgBotLink) {
            el.btnOpenTgBotLink.href = `https://t.me/${res.botUsername}?start=link`;
          }

          showToast(res.message || 'Перейдите в бота и нажмите START для получения кода', 'info');

          el.linkStep1.style.display = 'none';
          el.linkStep2.style.display = 'block';

          let remaining = res.expiresInSeconds || 300;
          if (state.linkTgTimerInterval) clearInterval(state.linkTgTimerInterval);
          const updateLinkTimer = () => {
            const m = Math.floor(remaining / 60);
            const s = remaining % 60;
            const mm = String(m).padStart(2, '0');
            const ss = String(s).padStart(2, '0');
            if (el.linkTgTimerDisplay) el.linkTgTimerDisplay.textContent = `⏱ Действует: ${mm}:${ss}`;
            if (remaining <= 0) {
              clearInterval(state.linkTgTimerInterval);
              if (el.linkTgTimerDisplay) el.linkTgTimerDisplay.textContent = '❌ Код истек';
            }
            remaining--;
          };
          updateLinkTimer();
          state.linkTgTimerInterval = setInterval(updateLinkTimer, 1000);
          el.linkTgInputCode?.focus();
        } catch (_) {}
      };
    }

    if (el.formLinkTgConfirm) {
      el.formLinkTgConfirm.onsubmit = async (e) => {
        e.preventDefault();
        const code = el.linkTgInputCode.value.trim();
        if (!code || !state.linkTgPendingUsername) return;
        try {
          const res = await api('/api/user/telegram/link-confirm', {
            method: 'POST',
            body: JSON.stringify({ username: state.linkTgPendingUsername, code })
          });
          if (state.linkTgTimerInterval) clearInterval(state.linkTgTimerInterval);
          el.dialogLinkTelegram?.close();
          state.user.telegram_username = res.telegramUsername;
          state.user.telegramUsername = res.telegramUsername;
          renderTelegramAnd2FAStatus(state.user.role);
          showToast(res.message || 'Telegram успешно привязан!', 'success');
        } catch (_) {}
      };
    }

    // 2FA Toggle Switch Handlers
    const handle2faToggle = async (toggleEl) => {
      const enabled = toggleEl.checked;
      try {
        const res = await api('/api/user/2fa', {
          method: 'POST',
          body: JSON.stringify({ enabled })
        });
        state.user.two_factor_enabled = res.twoFactorEnabled ? 1 : 0;
        state.user.twoFactorEnabled = res.twoFactorEnabled;
        showToast(res.twoFactorEnabled ? '2FA включена! Все входы требуют код из Telegram' : '2FA выключена', 'info');
      } catch (err) {
        toggleEl.checked = !enabled; // Revert switch on error
      }
    };

    if (el.athlete2faToggle) el.athlete2faToggle.onchange = () => handle2faToggle(el.athlete2faToggle);
    if (el.trainer2faToggle) el.trainer2faToggle.onchange = () => handle2faToggle(el.trainer2faToggle);

    el.formRegister.onsubmit = async (e) => {
      e.preventDefault();
      if (state.isRegistering) return;
      state.isRegistering = true;
      const btnSubmit = document.getElementById('btn-submit-register');
      if (btnSubmit) btnSubmit.disabled = true;

      const username = el.regUsername.value.trim();
      const fullName = el.regFullname.value.trim();
      const phone = el.regPhone.value.trim();
      const telegram = document.getElementById('reg-telegram')?.value.trim() || '';
      const password = el.regPassword.value;
      const role = document.querySelector('input[name="reg-role"]:checked')?.value || 'athlete';

      try {
        const data = await api('/api/register', {
          method: 'POST',
          body: JSON.stringify({ username, password, role, fullName, phone, telegram })
        });

        if (data.pendingApproval) {
          el.formRegister.style.display = 'none';
          let pendingBox = document.getElementById('register-pending-notice');
          if (!pendingBox) {
            pendingBox = document.createElement('div');
            pendingBox.id = 'register-pending-notice';
            pendingBox.className = 'pending-approval-box';
            el.formRegister.parentNode.insertBefore(pendingBox, el.formRegister.nextSibling);
          }
          pendingBox.innerHTML = `
            <div class="pending-approval-title">⏳ ЗАЯВКА НА РАССМОТРЕНИИ</div>
            <p class="pending-approval-text">
              Ваша заявка на создание аккаунта тренера принята!
            </p>
            <p class="pending-approval-text" style="font-size: 12px; margin-top: 8px;">
              В течение 72 часов ваша заявка будет обработана, мы свяжемся если будет необходима дополнительная информация.
            </p>
            <div class="owners-links-row" style="margin-top: 10px;">
              <a href="https://t.me/SantiLA213" target="_blank" class="owner-chip">💬 @SantiLA213</a>
              <a href="https://t.me/Spirit5449" target="_blank" class="owner-chip">💬 @Spirit5449</a>
            </div>
          `;
          pendingBox.style.display = 'block';
          showToast('Заявка на аккаунт тренера отправлена!', 'info');
          return;
        }

        state.user = data.user;
        saveAuthToken(data.token);
        setupAppForRole(data.user.role);
        showToast('Аккаунт атлета успешно создан!', 'success');
      } catch {} finally {
        state.isRegistering = false;
        if (btnSubmit) btnSubmit.disabled = false;
      }
    };

    // Telegram 1-Click & 3-Step OTP Authentication Flow
    function resetTgAuthModal() {
      if (state.tgTimerInterval) {
        clearInterval(state.tgTimerInterval);
        state.tgTimerInterval = null;
      }
      if (state.tgSessionPollInterval) {
        clearInterval(state.tgSessionPollInterval);
        state.tgSessionPollInterval = null;
      }
      if (el.tgStepOneclick) el.tgStepOneclick.style.display = 'block';
      if (el.tgStepRequest) el.tgStepRequest.style.display = 'none';
      if (el.tgStepVerify) el.tgStepVerify.style.display = 'none';
      if (el.tgStepProfile) el.tgStepProfile.style.display = 'none';
      if (el.tgInputCode) el.tgInputCode.value = '';
      if (el.tgOneclickStatusText) el.tgOneclickStatusText.textContent = 'Ожидание перехода в Telegram...';
    }

    function startOneClickPolling(sessionId) {
      if (state.tgSessionPollInterval) clearInterval(state.tgSessionPollInterval);
      state.tgSessionPollInterval = setInterval(async () => {
        try {
          const res = await api(`/api/auth/telegram/session-status?sessionId=${encodeURIComponent(sessionId)}`);
          if (res.status === 'AUTHORIZED' && res.token) {
            clearInterval(state.tgSessionPollInterval);
            state.tgSessionPollInterval = null;
            state.token = res.token;
            state.user = res.user;
            saveAuthToken(res.token);
            resetTgAuthModal();
            el.dialogTelegramAuth?.close();
            setupAppForRole(res.user.role);
            showToast(`Вход выполнен в 1 клик: ${res.user.fullName || res.user.username}!`, 'success');
          } else if (res.status === 'EXPIRED') {
            clearInterval(state.tgSessionPollInterval);
            state.tgSessionPollInterval = null;
            if (el.tgOneclickStatusText) el.tgOneclickStatusText.textContent = 'Срок действия сессии истёк. Нажмите «Открыть Telegram» заново.';
          }
        } catch (_) {}
      }, 1000);
    }

    function startTgTimer(durationSeconds = 300) {
      if (state.tgTimerInterval) clearInterval(state.tgTimerInterval);
      let remaining = durationSeconds;
      const updateBadge = () => {
        const m = Math.floor(remaining / 60);
        const s = remaining % 60;
        const mm = String(m).padStart(2, '0');
        const ss = String(s).padStart(2, '0');
        if (el.tgTimerDisplay) {
          el.tgTimerDisplay.textContent = `⏱ Действует: ${mm}:${ss}`;
        }
        if (remaining <= 0) {
          clearInterval(state.tgTimerInterval);
          state.tgTimerInterval = null;
          if (el.tgTimerDisplay) {
            el.tgTimerDisplay.textContent = '❌ Срок действия кода истек (5 минут)';
          }
          showToast('Срок действия кода истек. Запросите код повторно.', 'error');
        }
        remaining--;
      };
      updateBadge();
      state.tgTimerInterval = setInterval(updateBadge, 1000);
    }

    // Telegram Fast Auth Button (1-Click Primary Flow)
    if (el.btnTelegramAuth) {
      el.btnTelegramAuth.onclick = async () => {
        // Mini App auto-login if running inside Telegram WebApp
        if (window.Telegram?.WebApp?.initData) {
          showToast('Авторизация через Telegram Mini App...', 'info');
          try {
            const data = await api('/api/auth/telegram', {
              method: 'POST',
              body: JSON.stringify({ initData: window.Telegram.WebApp.initData })
            });
            state.token = data.token;
            state.user = data.user;
            saveAuthToken(data.token);
            setupAppForRole(data.user.role);
            showToast(`Вход выполнен: ${data.user.fullName || data.user.username}!`, 'success');
            return;
          } catch (_) {}
        }

        // Initialize 1-click session
        resetTgAuthModal();
        if (el.dialogTelegramAuth) {
          el.dialogTelegramAuth.showModal();
        }

        try {
          const res = await api('/api/auth/telegram/session-init', { method: 'POST' });
          if (res.sessionId && res.botUrl) {
            state.tgSessionId = res.sessionId;
            if (el.btnTgOneclickOpen) {
              el.btnTgOneclickOpen.href = res.botUrl;
              el.btnTgOneclickOpen.onclick = (e) => {
                // Ensure link opens in telegram app/browser
                window.open(res.botUrl, '_blank');
              };
            }
            if (res.qrSvg && el.tgOneclickQrSvg && el.tgOneclickQrContainer) {
              el.tgOneclickQrSvg.innerHTML = res.qrSvg;
              el.tgOneclickQrContainer.style.display = 'block';
            }
            startOneClickPolling(res.sessionId);
          }
        } catch (_) {
          showToast('Не удалось инициализировать сессию входа. Используйте ручной ввод.', 'error');
        }
      };
    }

    // Toggle between 1-click and manual code entry
    if (el.btnToggleTgManualCode) {
      el.btnToggleTgManualCode.onclick = () => {
        if (state.tgSessionPollInterval) {
          clearInterval(state.tgSessionPollInterval);
          state.tgSessionPollInterval = null;
        }
        if (el.tgStepOneclick) el.tgStepOneclick.style.display = 'none';
        if (el.tgStepRequest) el.tgStepRequest.style.display = 'block';
        el.tgInputUsername?.focus();
      };
    }

    if (el.btnTgBackToOneclick) {
      el.btnTgBackToOneclick.onclick = () => {
        if (el.tgStepRequest) el.tgStepRequest.style.display = 'none';
        if (el.tgStepOneclick) el.tgStepOneclick.style.display = 'block';
        if (state.tgSessionId) startOneClickPolling(state.tgSessionId);
      };
    }

    // Close Dialog buttons
    document.querySelectorAll('.btn-close-tg').forEach(btn => {
      btn.onclick = () => {
        resetTgAuthModal();
        el.dialogTelegramAuth?.close();
      };
    });

    // Step 1: Request OTP code
    if (el.formTgRequest) {
      el.formTgRequest.onsubmit = async (e) => {
        e.preventDefault();
        const username = el.tgInputUsername.value.trim();
        if (!username) return;

        showToast('Отправка кода в Telegram...', 'info');
        try {
          const res = await api('/api/auth/telegram/request-otp', {
            method: 'POST',
            body: JSON.stringify({ username })
          });
          state.tgPendingUsername = res.telegramUsername;
          showToast(res.message || 'Код отправлен в бота Telegram', 'success');

          // Transition to Step 2
          el.tgStepRequest.style.display = 'none';
          el.tgStepVerify.style.display = 'block';
          startTgTimer(res.expiresInSeconds || 300);
          el.tgInputCode.value = '';
          el.tgInputCode.focus();

          const botHint = document.getElementById('tg-bot-link-hint');
          if (botHint) {
            const bName = res.botUsername || 'fitnessecosystemBOT';
            botHint.innerHTML = `<a href="https://t.me/${bName}?start=login" target="_blank" class="app-btn btn-secondary" style="text-decoration:none;display:flex;align-items:center;justify-content:center;gap:6px;font-size:13px;padding:9px 12px;background:#2AABEE;color:#fff;border-radius:10px;font-weight:600;margin-top:6px;"><span>🤖</span><span>Открыть бота @${bName} для получения кода</span></a>`;
            botHint.style.display = 'block';
          }
        } catch (_) {}
      };
    }

    // Back to Step 1
    if (el.btnTgBackToStep1) {
      el.btnTgBackToStep1.onclick = () => {
        if (state.tgTimerInterval) clearInterval(state.tgTimerInterval);
        el.tgStepVerify.style.display = 'none';
        el.tgStepRequest.style.display = 'block';
        el.tgInputUsername.focus();
      };
    }

    // Auto-filter OTP code input (only 6 digits)
    if (el.tgInputCode) {
      el.tgInputCode.oninput = (e) => {
        e.target.value = e.target.value.replace(/\D/g, '').slice(0, 6);
      };
    }

    // Step 2: Verify OTP code
    if (el.formTgVerify) {
      el.formTgVerify.onsubmit = async (e) => {
        e.preventDefault();
        const code = el.tgInputCode.value.trim();
        if (code.length !== 6) {
          showToast('Код должен содержать ровно 6 цифр', 'error');
          return;
        }

        showToast('Проверка кода...', 'info');
        try {
          const res = await api('/api/auth/telegram/verify-otp', {
            method: 'POST',
            body: JSON.stringify({
              username: state.tgPendingUsername,
              code
            })
          });

          if (state.tgTimerInterval) {
            clearInterval(state.tgTimerInterval);
            state.tgTimerInterval = null;
          }

          if (!res.isNewUser && res.token) {
            // Existing user logged in
            state.user = res.user;
            saveAuthToken(res.token);
            el.dialogTelegramAuth?.close();
            setupAppForRole(res.user.role);
            showToast(`С возвращением, ${res.user.fullName || res.user.username}!`, 'success');
          } else {
            // New user: transition to Step 3 (Fill account profile)
            el.tgStepVerify.style.display = 'none';
            el.tgStepProfile.style.display = 'block';
            el.tgProfileName.focus();
            showToast('Код подтвержден! Заполните ваш профиль.', 'success');
          }
        } catch (_) {}
      };
    }

    // Step 3: Complete profile (New user)
    if (el.formTgProfile) {
      el.formTgProfile.onsubmit = async (e) => {
        e.preventDefault();
        const fullName = el.tgProfileName.value.trim();
        const phone = el.tgProfilePhone.value.trim();
        const role = document.querySelector('input[name="tg-new-role"]:checked')?.value || 'athlete';

        if (!fullName) {
          showToast('Укажите ваше ФИО', 'error');
          return;
        }

        showToast('Создание профиля...', 'info');
        try {
          const res = await api('/api/auth/telegram/complete-profile', {
            method: 'POST',
            body: JSON.stringify({
              username: state.tgPendingUsername,
              fullName,
              phone,
              role
            })
          });

          state.user = res.user;
          saveAuthToken(res.token);
          el.dialogTelegramAuth?.close();
          setupAppForRole(res.user.role);
          showToast(`Добро пожаловать в Fitness Pro, ${res.user.fullName}!`, 'success');
        } catch (_) {}
      };
    }

    // Athlete Date Navigator
    el.btnAthleteDatePrev.onclick = () => changeAthleteDate(-1);
    el.btnAthleteDateNext.onclick = () => changeAthleteDate(1);
    el.athleteDatePicker.onchange = (e) => {
      if (e.target.value) {
        state.currentDate = e.target.value;
        loadAthleteWorkoutSets();
      }
    };

    // Athlete Quick Chips
    document.querySelectorAll('#athlete-chips .app-chip').forEach(chip => {
      chip.onclick = () => {
        el.athleteInputExercise.value = chip.dataset.name;
        el.athleteInputWeight.focus();
      };
    });

    // Athlete Add Set Submit
    el.formAthleteAddSet.onsubmit = async (e) => {
      e.preventDefault();
      const exerciseName = el.athleteInputExercise.value.trim();
      const weightKg = parseFloat(el.athleteInputWeight.value) || 0;
      const reps = parseInt(el.athleteInputReps.value, 10) || 1;
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
        showToast('Подход добавлен', 'success');
        loadAthleteWorkoutSets();
        el.athleteInputReps.focus();
      } catch {}
    };

    // Athlete Profile Photo Selection
    if (el.athleteAvatarInput) {
      el.athleteAvatarInput.onchange = async (e) => {
        const file = e.target.files?.[0];
        if (!file) return;
        try {
          state.athleteAvatarBase64 = await resizeImageFile(file, 128, 128);
          if (el.athleteAvatarPreview) {
            el.athleteAvatarPreview.innerHTML = `<img src="${state.athleteAvatarBase64}" alt="Avatar">`;
          }
          showToast('Фото выбрано. Нажмите «Сохранить изменения»', 'info');
        } catch (_) {
          showToast('Не удалось обработать фото', 'error');
        }
      };
    }

    // Athlete Profile Save
    if (el.btnSaveAthleteProfile) {
      el.btnSaveAthleteProfile.onclick = async () => {
        const fullName = el.athleteEditName.value.trim();
        const phone = el.athleteEditPhone.value.trim();
        try {
          const res = await api('/api/user/profile', {
            method: 'POST',
            body: JSON.stringify({
              fullName,
              phone,
              avatarBase64: state.athleteAvatarBase64 || state.user.avatar_base64 || ''
            })
          });
          state.user = res.user;
          renderAthleteProfile();
          showToast('Профиль атлета обновлен!', 'success');
        } catch {}
      };
    }

    // Trainer Date Navigator
    el.btnTrainerDatePrev.onclick = () => changeTrainerDate(-1);
    el.btnTrainerDateNext.onclick = () => changeTrainerDate(1);
    el.trainerDatePicker.onchange = (e) => {
      if (e.target.value) {
        state.trainerDate = e.target.value;
        loadTrainerWorkoutSets();
      }
    };

    // Trainer Add / Assign Set Submit
    el.formTrainerAddSet.onsubmit = async (e) => {
      e.preventDefault();
      if (!state.activeClientId) {
        showToast('Сначала выберите подопечного!', 'error');
        return;
      }
      const exerciseName = el.trainerInputExercise.value.trim();
      const weightKg = parseFloat(el.trainerInputWeight.value) || 0;
      const reps = parseInt(el.trainerInputReps.value, 10) || 1;
      const rpe = parseFloat(el.trainerInputRpe.value) || 8.0;
      const isSelfAllowed = Boolean(el.trainerAllowSelfWorkout?.checked);

      try {
        await api('/api/trainer/assign-workout', {
          method: 'POST',
          body: JSON.stringify({
            athleteId: state.activeClientId,
            date: state.trainerDate,
            isSelfAllowed,
            exercises: [{ exerciseName, weightKg, reps }]
          })
        });
        showToast('Упражнение назначено подопечному', 'success');
        loadTrainerWorkoutSets();
        el.trainerInputReps.focus();
      } catch {}
    };

    // Trainer Athlete Restrictions Form
    const formRestrictions = document.getElementById('form-trainer-restrictions');
    if (formRestrictions) {
      formRestrictions.onsubmit = async (evt) => {
        evt.preventDefault();
        if (!state.activeClientId) {
          showToast('Подопечный не выбран', 'error');
          return;
        }
        const restrictions = document.getElementById('input-athlete-restrictions').value;
        try {
          await api('/api/trainer/athlete-restrictions', {
            method: 'POST',
            body: JSON.stringify({ athleteId: state.activeClientId, restrictions })
          });
          const client = state.clients.find(c => c.id === state.activeClientId);
          if (client) client.restrictions = restrictions;
          showToast('Ограничения и травмы атлета сохранены', 'success');
        } catch (err) {
          showToast(err.message || 'Ошибка сохранения', 'error');
        }
      };
    }

    // Trainer Profile Photo Selection
    if (el.trainerAvatarInput) {
      el.trainerAvatarInput.onchange = async (e) => {
        const file = e.target.files?.[0];
        if (!file) return;
        try {
          state.trainerAvatarBase64 = await resizeImageFile(file, 128, 128);
          if (el.trainerAvatarPreview) {
            el.trainerAvatarPreview.innerHTML = `<img src="${state.trainerAvatarBase64}" alt="Avatar">`;
          }
          showToast('Фото выбрано. Нажмите «Сохранить изменения»', 'info');
        } catch (_) {
          showToast('Не удалось обработать фото', 'error');
        }
      };
    }

    // Trainer Profile Save
    if (el.btnSaveTrainerProfile) {
      el.btnSaveTrainerProfile.onclick = async () => {
        const fullName = el.trainerEditName.value.trim();
        const phone = el.trainerEditPhone.value.trim();
        try {
          const res = await api('/api/user/profile', {
            method: 'POST',
            body: JSON.stringify({
              fullName,
              phone,
              avatarBase64: state.trainerAvatarBase64 || state.user.avatar_base64 || ''
            })
          });
          state.user = res.user;
          renderTrainerSettings();
          showToast('Профиль тренера обновлен!', 'success');
        } catch {}
      };
    }

    // Regenerate PIN button
    el.btnRegeneratePin.onclick = async () => {
      try {
        const data = await api('/api/athlete/regenerate-pin', { method: 'POST' });
        state.user.pairingCode = data.pairingCode;
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
        state.user.coach_name = '';
        state.user.coach_phone = '';
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

    // Trainer Form Submit: Pair Client (Local + Google Drive Cloud)
    el.formTrainerPair.onsubmit = async (e) => {
      e.preventDefault();
      const code = el.inputPairCode.value.trim();
      if (code.length !== 6) {
        showToast('Код должен содержать ровно 6 цифр', 'error');
        return;
      }

      showToast('Поиск подопечного в системе и облаке...', 'info');
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

      const match = file.name.match(/\d{6}/);
      if (match) {
        el.inputPairCode.value = match[0];
        showToast(`Код ${match[0]} считан из файла`, 'success');
      } else {
        showToast('Код распознан. Нажмите «Связать»', 'info');
      }
    };

    // Refresh Leaderboard button
    el.btnRefreshLeaderboard.onclick = loadLeaderboard;

    // --- Chart Exercise Select Change Handlers ---
    if (el.athleteHistorySelect) {
      el.athleteHistorySelect.onchange = (e) => {
        state.athleteSelectedExercise = e.target.value;
        loadAthleteExerciseTimeline(state.athleteSelectedExercise);
      };
    }
    if (el.trainerHistorySelect) {
      el.trainerHistorySelect.onchange = (e) => {
        state.trainerSelectedExercise = e.target.value;
        loadTrainerExerciseTimeline(state.trainerSelectedExercise);
      };
    }

    // --- Body Weight Dialog (Athlete & Trainer) ---
    function openWeightDialog() {
      if (!el.dialogAddWeight) return;
      if (el.inputWeightDate) el.inputWeightDate.value = new Date().toISOString().slice(0, 10);
      if (el.inputWeightValue) el.inputWeightValue.value = '';
      if (typeof el.dialogAddWeight.showModal === 'function') {
        el.dialogAddWeight.showModal();
      } else {
        el.dialogAddWeight.style.display = 'block';
      }
    }

    function closeWeightDialog() {
      if (!el.dialogAddWeight) return;
      if (typeof el.dialogAddWeight.close === 'function') {
        el.dialogAddWeight.close();
      } else {
        el.dialogAddWeight.style.display = 'none';
      }
    }

    if (el.btnOpenAddWeight) el.btnOpenAddWeight.onclick = openWeightDialog;
    if (el.btnTrainerAddWeight) el.btnTrainerAddWeight.onclick = () => {
      if (!state.activeClientId) {
        showToast('Сначала выберите подопечного', 'error');
        return;
      }
      openWeightDialog();
    };
    if (el.btnCloseWeightDialog) el.btnCloseWeightDialog.onclick = closeWeightDialog;
    if (el.btnCancelWeightDialog) el.btnCancelWeightDialog.onclick = closeWeightDialog;

    if (el.formAddWeight) {
      el.formAddWeight.onsubmit = async (e) => {
        e.preventDefault();
        const weightKg = parseFloat(el.inputWeightValue.value);
        const date = el.inputWeightDate.value || new Date().toISOString().slice(0, 10);
        if (!weightKg || weightKg <= 0) {
          showToast('Укажите корректный вес', 'error');
          return;
        }

        try {
          const body = { weightKg, date };
          if (state.user?.role === 'trainer' && state.activeClientId) {
            body.athleteId = state.activeClientId;
          }
          await api('/api/progress/anthropometry', {
            method: 'POST',
            body: JSON.stringify(body)
          });
          showToast('Замер веса сохранен', 'success');
          closeWeightDialog();

          if (state.user?.role === 'athlete') {
            loadAthleteWeightProgress();
          } else {
            loadTrainerClientWeightProgress();
          }
        } catch (err) {
          showToast(err.message || 'Ошибка сохранения замера', 'error');
        }
      };
    }

    // --- PWA Installation & APK Download Modals ---
    function openDownloadDialog() {
      if (!el.dialogDownloadApp) return;
      if (typeof el.dialogDownloadApp.showModal === 'function') {
        el.dialogDownloadApp.showModal();
      } else {
        el.dialogDownloadApp.style.display = 'block';
      }
    }

    function closeDownloadDialog() {
      if (!el.dialogDownloadApp) return;
      if (typeof el.dialogDownloadApp.close === 'function') {
        el.dialogDownloadApp.close();
      } else {
        el.dialogDownloadApp.style.display = 'none';
      }
    }

    document.querySelectorAll('.btn-open-download-modal, #btn-open-download-modal').forEach(btn => {
      btn.onclick = openDownloadDialog;
    });
    if (el.btnCloseDownloadDialog) el.btnCloseDownloadDialog.onclick = closeDownloadDialog;

    // Trigger PWA Installation Prompt
    async function triggerPwaInstall() {
      if (state.deferredInstallPrompt) {
        state.deferredInstallPrompt.prompt();
        const { outcome } = await state.deferredInstallPrompt.userChoice;
        if (outcome === 'accepted') {
          showToast('Приложение Fitness Pro установлено!', 'success');
        }
        state.deferredInstallPrompt = null;
        if (el.pwaInstallBanner) el.pwaInstallBanner.style.display = 'none';
      } else {
        // Fallback instructions for iOS Safari or Chrome when already installed/not supported
        const isIos = /iPad|iPhone|iPod/.test(navigator.userAgent) && !window.MSStream;
        if (isIos) {
          showToast('Нажмите «Поделиться» (□ с ↑) ➔ «На экран Домой»', 'info');
        } else {
          showToast('Для установки нажмите меню браузера (⋮) ➔ «Установить приложение»', 'info');
        }
      }
    }

    if (el.btnPwaInstall) el.btnPwaInstall.onclick = triggerPwaInstall;
    if (el.btnTriggerPwaInstallDialog) el.btnTriggerPwaInstallDialog.onclick = triggerPwaInstall;
    if (el.btnPwaDismiss) {
      el.btnPwaDismiss.onclick = () => {
        if (el.pwaInstallBanner) el.pwaInstallBanner.style.display = 'none';
        localStorage.setItem('pwa_dismissed', 'true');
      };
    }

    // Listen to browser PWA install event (Chrome, Edge, Samsung Internet, Android)
    window.addEventListener('beforeinstallprompt', (e) => {
      e.preventDefault();
      state.deferredInstallPrompt = e;
      if (localStorage.getItem('pwa_dismissed') !== 'true') {
        if (el.pwaInstallBanner) el.pwaInstallBanner.style.display = 'flex';
      }
    });

    window.addEventListener('appinstalled', () => {
      state.deferredInstallPrompt = null;
      if (el.pwaInstallBanner) el.pwaInstallBanner.style.display = 'none';
      showToast('Приложение Fitness Pro успешно установлено!', 'success');
    });

    // Bi-Directional Cloud Sync Handler
    let isSyncing = false;
    async function triggerCloudSync() {
      if (isSyncing) return;
      isSyncing = true;
      document.querySelectorAll('.sync-icon').forEach(icon => icon.classList.add('spinning'));
      showToast('Облачная синхронизация...', 'info');

      try {
        const res = await api('/api/sync', { method: 'POST' });
        const timeStr = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
        const statusMsg = `Синхронизировано в ${timeStr}`;

        if (el.athleteSyncStatusText) el.athleteSyncStatusText.textContent = statusMsg;
        if (el.trainerSyncStatusText) el.trainerSyncStatusText.textContent = statusMsg;

        showToast('Данные успешно синхронизированы с облаком!', 'success');

        // Refresh currently active views with fresh data
        if (state.user?.role === 'athlete') {
          await loadAthleteWorkoutSets();
          await renderAthleteProfile();
        } else if (state.user?.role === 'trainer') {
          await loadTrainerClients();
          if (state.activeClientId) {
            await loadTrainerWorkoutSets();
          }
        }
      } catch (err) {
        showToast(err.message || 'Ошибка синхронизации с облаком', 'error');
      } finally {
        isSyncing = false;
        document.querySelectorAll('.sync-icon').forEach(icon => icon.classList.remove('spinning'));
      }
    }

    if (el.btnSyncHeader) el.btnSyncHeader.onclick = triggerCloudSync;
    if (el.btnSyncAthlete) el.btnSyncAthlete.onclick = triggerCloudSync;
    if (el.btnSyncTrainer) el.btnSyncTrainer.onclick = triggerCloudSync;

    // Logout Action Handlers
    if (el.btnLogoutHeader) el.btnLogoutHeader.onclick = () => logout(true);
    if (el.btnLogoutAthlete) el.btnLogoutAthlete.onclick = () => logout(true);
    if (el.btnLogoutTrainer) el.btnLogoutTrainer.onclick = () => logout(true);

    // Auto-refresh profile and 2FA status when returning from Telegram bot
    window.addEventListener('focus', async () => {
      if (state.token && state.user) {
        try {
          const fresh = await api('/api/me');
          if (fresh && fresh.user) {
            state.user = normalizeUser(fresh.user);
            if (state.user.role === 'athlete') renderAthleteProfile();
            else renderTrainerSettings();
          }
        } catch (_) {}
      }
    });

    // Register Service Worker for PWA
    if ('serviceWorker' in navigator) {
      navigator.serviceWorker.register('/sw.js').catch(err => {
        console.warn('PWA ServiceWorker registration notice:', err);
      });
    }
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
