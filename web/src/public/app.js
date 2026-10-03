/**
 * Fitness Ecosystem Pro — Single Page Application Core
 * Swiss Clean UI + Anti-Overlap Guard + Zero External Dependencies
 */

(function () {
  'use strict';

  // --- State Management ---
  const state = {
    token: localStorage.getItem('fit_token') || null,
    user: null,
    pairedCoach: null,
    activeTab: 'workouts',
    currentDate: new Date().toISOString().slice(0, 10),
    clients: [],
    sets: []
  };

  // --- DOM Elements Cache ---
  const el = {
    headerActions: document.getElementById('header-auth-actions'),
    userDisplayName: document.getElementById('user-display-name'),
    userRolePill: document.getElementById('user-role-pill'),
    btnToggleRole: document.getElementById('btn-toggle-role'),
    btnLogout: document.getElementById('btn-logout'),

    authSection: document.getElementById('auth-section'),
    tabLogin: document.getElementById('tab-login'),
    tabRegister: document.getElementById('tab-register'),
    formLogin: document.getElementById('form-login'),
    formRegister: document.getElementById('form-register'),

    dashboardSection: document.getElementById('dashboard-section'),
    mainNavTabs: document.getElementById('main-nav-tabs'),

    // Athlete views
    viewAthleteWorkouts: document.getElementById('view-athlete-workouts'),
    viewAthletePairing: document.getElementById('view-athlete-pairing'),
    workoutDatePicker: document.getElementById('workout-date-picker'),
    quickChips: document.getElementById('quick-exercise-chips'),
    lastStatsBanner: document.getElementById('last-stats-banner'),
    formAddSet: document.getElementById('form-add-set'),
    inputExerciseName: document.getElementById('input-exercise-name'),
    inputWeight: document.getElementById('input-weight'),
    inputReps: document.getElementById('input-reps'),
    inputRpe: document.getElementById('input-rpe'),
    setsContainer: document.getElementById('sets-container'),
    dailyTonnageLabel: document.getElementById('daily-tonnage-label'),

    athleteQrContainer: document.getElementById('athlete-qr-container'),
    athletePinCode: document.getElementById('athlete-pin-code'),
    btnCopyPin: document.getElementById('btn-copy-pin'),
    btnCopyLink: document.getElementById('btn-copy-link'),
    btnRegeneratePin: document.getElementById('btn-regenerate-pin'),
    coachInfoContainer: document.getElementById('coach-info-container'),
    privacyToggleCheckbox: document.getElementById('privacy-toggle-checkbox'),

    // Trainer views
    viewTrainerClients: document.getElementById('view-trainer-clients'),
    formTrainerPair: document.getElementById('form-trainer-pair'),
    trainerPairCode: document.getElementById('trainer-pair-code'),
    trainerClientsContainer: document.getElementById('trainer-clients-container'),

    // Leaderboard
    viewLeaderboard: document.getElementById('view-leaderboard'),
    leaderboardTbody: document.getElementById('leaderboard-tbody'),
    btnRefreshLeaderboard: document.getElementById('btn-refresh-leaderboard'),

    toastContainer: document.getElementById('toast-container')
  };

  // --- Toast System ---
  function showToast(message, type = 'info') {
    const toast = document.createElement('div');
    toast.className = 'toast';
    if (type === 'error') {
      toast.style.borderColor = 'rgba(244, 63, 94, 0.4)';
      toast.style.color = '#fda4af';
    } else if (type === 'success') {
      toast.style.borderColor = 'rgba(16, 185, 129, 0.4)';
      toast.style.color = '#6ee7b7';
    }
    toast.textContent = message;
    el.toastContainer.appendChild(toast);
    setTimeout(() => {
      toast.style.opacity = '0';
      toast.style.transition = 'opacity 0.2s ease';
      setTimeout(() => toast.remove(), 200);
    }, 3200);
  }

  // --- API Client ---
  async function api(path, options = {}) {
    const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) };
    if (state.token) {
      headers['Authorization'] = `Bearer ${state.token}`;
    }

    try {
      const res = await fetch(path, { ...options, headers });
      const data = await res.json().catch(() => ({}));

      if (res.status === 401) {
        logout();
        throw new Error('Сессия завершена. Пожалуйста, войдите снова.');
      }
      if (!res.ok) {
        throw new Error(data.error || `Ошибка сервера: ${res.status}`);
      }
      return data;
    } catch (err) {
      showToast(err.message, 'error');
      throw err;
    }
  }

  // --- Auth & Session ---
  async function initAuth() {
    if (!state.token) {
      showAuthScreen();
      return;
    }

    try {
      const data = await api('/api/me');
      state.user = data.user;
      state.pairedCoach = data.pairedCoach;
      showDashboard();
    } catch {
      logout();
    }
  }

  function logout() {
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
    showToast('Вы вышли из системы');
  }

  function showAuthScreen() {
    el.headerActions.style.display = 'none';
    el.dashboardSection.style.display = 'none';
    el.authSection.style.display = 'flex';
  }

  function showDashboard() {
    el.authSection.style.display = 'none';
    el.headerActions.style.display = 'flex';
    el.dashboardSection.style.display = 'block';

    // Header info
    el.userDisplayName.textContent = state.user.full_name || state.user.username;
    el.userRolePill.textContent = state.user.role === 'trainer' ? 'Тренер' : 'Атлет';
    el.userRolePill.className = `role-pill ${state.user.role}`;
    el.btnToggleRole.textContent = state.user.role === 'trainer' ? 'Режим Атлета' : 'Режим Тренера';

    renderNavTabs();
    switchTab(state.user.role === 'trainer' ? 'clients' : 'workouts');
  }

  // --- Role Switcher ---
  async function toggleRole() {
    const targetRole = state.user.role === 'trainer' ? 'athlete' : 'trainer';
    try {
      const data = await api('/api/user/role', {
        method: 'POST',
        body: JSON.stringify({ role: targetRole })
      });
      state.user = data.user;
      showToast(`Роль изменена: ${targetRole === 'trainer' ? 'Тренер' : 'Атлет'}`, 'success');
      showDashboard();
    } catch {}
  }

  // --- Navigation Tabs ---
  function renderNavTabs() {
    el.mainNavTabs.innerHTML = '';
    const isTrainer = state.user.role === 'trainer';

    const tabs = isTrainer
      ? [
          { id: 'clients', label: 'Мои подопечные' },
          { id: 'leaderboard', label: 'Лидерборд & Соревнования' }
        ]
      : [
          { id: 'workouts', label: 'Дневник тренировок' },
          { id: 'pairing', label: 'Связь с тренером & QR' },
          { id: 'leaderboard', label: 'Лидерборд & Соревнования' }
        ];

    tabs.forEach(t => {
      const btn = document.createElement('button');
      btn.className = `tab-btn ${state.activeTab === t.id ? 'active' : ''}`;
      btn.textContent = t.label;
      btn.onclick = () => switchTab(t.id);
      el.mainNavTabs.appendChild(btn);
    });
  }

  function switchTab(tabId) {
    state.activeTab = tabId;

    // Update active class on tab buttons
    Array.from(el.mainNavTabs.children).forEach(btn => {
      const isActive = (btn.textContent.includes('подопечные') && tabId === 'clients') ||
                       (btn.textContent.includes('Дневник') && tabId === 'workouts') ||
                       (btn.textContent.includes('Связь') && tabId === 'pairing') ||
                       (btn.textContent.includes('Лидерборд') && tabId === 'leaderboard');
      btn.classList.toggle('active', isActive);
    });

    // Hide all views
    el.viewAthleteWorkouts.style.display = 'none';
    el.viewAthletePairing.style.display = 'none';
    el.viewTrainerClients.style.display = 'none';
    el.viewLeaderboard.style.display = 'none';

    if (tabId === 'workouts') {
      el.viewAthleteWorkouts.style.display = 'grid';
      loadWorkoutSets();
    } else if (tabId === 'pairing') {
      el.viewAthletePairing.style.display = 'grid';
      renderPairingView();
    } else if (tabId === 'clients') {
      el.viewTrainerClients.style.display = 'grid';
      loadTrainerClients();
    } else if (tabId === 'leaderboard') {
      el.viewLeaderboard.style.display = 'grid';
      loadLeaderboard();
    }
  }

  // --- View: Athlete Workouts ---
  async function loadWorkoutSets() {
    el.workoutDatePicker.value = state.currentDate;
    try {
      const data = await api(`/api/workout?date=${state.currentDate}`);
      state.sets = data.sets || [];
      renderWorkoutSets();
    } catch {}
  }

  function renderWorkoutSets() {
    el.setsContainer.innerHTML = '';
    let totalTonnage = 0;

    if (state.sets.length === 0) {
      el.setsContainer.innerHTML = `
        <div class="empty-state">
          <div class="empty-state-title">Нет подходов на эту дату</div>
          <p style="font-size: 13px;">Выберите упражнение и добавьте первый подход выше.</p>
        </div>
      `;
      el.dailyTonnageLabel.textContent = 'Тоннаж: 0 кг';
      return;
    }

    state.sets.forEach((set, index) => {
      const isCompleted = Boolean(set.is_completed);
      const tonnage = set.weight_kg * set.reps;
      if (isCompleted) totalTonnage += tonnage;

      const row = document.createElement('div');
      row.className = `workout-row ${isCompleted ? 'completed' : ''}`;
      row.innerHTML = `
        <div style="display: flex; align-items: center; gap: 12px; min-width: 0; flex: 1;">
          <input type="checkbox" class="custom-checkbox" ${isCompleted ? 'checked' : ''} aria-label="Отметить подход">
          <span style="font-family: var(--font-mono); font-size: 12px; color: var(--text-muted); width: 20px;">#${index + 1}</span>
          <div class="truncate" style="flex: 1;">
            <div class="exercise-title truncate" style="font-weight: 600; font-size: 14px;" title="${set.exercise_name}">${set.exercise_name}</div>
          </div>
        </div>
        <div class="set-metrics">
          <span class="metric-tag tabular-nums">${set.weight_kg} кг × ${set.reps}</span>
          <span class="rpe-badge tabular-nums">RPE ${set.rpe}</span>
          <button class="btn btn-secondary btn-sm btn-delete-set" style="padding: 4px 8px; color: var(--accent-rose);" title="Удалить подход">✕</button>
        </div>
      `;

      // Checkbox toggle
      const checkbox = row.querySelector('.custom-checkbox');
      checkbox.onchange = async () => {
        try {
          await api('/api/workout/set/toggle', {
            method: 'POST',
            body: JSON.stringify({ setId: set.id, isCompleted: checkbox.checked })
          });
          set.is_completed = checkbox.checked ? 1 : 0;
          renderWorkoutSets();
        } catch {
          checkbox.checked = !checkbox.checked;
        }
      };

      // Delete set
      const btnDelete = row.querySelector('.btn-delete-set');
      btnDelete.onclick = async () => {
        if (!confirm(`Удалить подход ${set.exercise_name} (${set.weight_kg} кг)?`)) return;
        try {
          await api('/api/workout/set', {
            method: 'DELETE',
            body: JSON.stringify({ setId: set.id })
          });
          state.sets = state.sets.filter(s => s.id !== set.id);
          renderWorkoutSets();
          showToast('Подход удалён');
        } catch {}
      };

      el.setsContainer.appendChild(row);
    });

    el.dailyTonnageLabel.textContent = `Тоннаж: ${totalTonnage.toLocaleString('ru-RU')} кг`;
  }

  async function checkLastExerciseStats(exerciseName) {
    if (!exerciseName) {
      el.lastStatsBanner.style.display = 'none';
      return;
    }
    // We can infer last stats from loaded sets or workout history
    const prev = state.sets.find(s => s.exercise_name.toLowerCase() === exerciseName.toLowerCase());
    if (prev) {
      el.lastStatsBanner.style.display = 'block';
      el.lastStatsBanner.innerHTML = `<strong>Предыдущий подход:</strong> ${prev.weight_kg} кг × ${prev.reps} (RPE ${prev.rpe})`;
    } else {
      el.lastStatsBanner.style.display = 'none';
    }
  }

  // --- View: Athlete Pairing & QR ---
  function renderPairingView() {
    const pin = state.user.pairing_code || '------';
    el.athletePinCode.textContent = pin;

    // Pure client-side SVG QR code generation
    if (typeof window.generateQrSvg === 'function') {
      const shareUrl = `https://fitnessapp.pro/pair?code=${pin}`;
      el.athleteQrContainer.innerHTML = window.generateQrSvg(shareUrl, {
        size: 210,
        color: '#ffffff',
        background: '#000000',
        margin: 2
      });
    }

    // Coach status
    renderCoachStatus();

    // Privacy setting
    const isPublic = !Boolean(state.user.is_private);
    el.privacyToggleCheckbox.checked = isPublic;
    updatePrivacySliderUI(isPublic);
  }

  function updatePrivacySliderUI(isPublic) {
    const slider = document.getElementById('privacy-toggle-slider');
    if (isPublic) {
      slider.style.backgroundColor = 'var(--accent-emerald)';
    } else {
      slider.style.backgroundColor = '#262626';
    }
  }

  function renderCoachStatus() {
    if (state.pairedCoach) {
      el.coachInfoContainer.innerHTML = `
        <div style="background-color: var(--bg-primary); border: 1px solid var(--border-subtle); border-radius: var(--radius-md); padding: 16px;">
          <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; gap: 12px;">
            <div style="min-width: 0;">
              <div class="truncate" style="font-weight: 700; font-size: 16px;">${state.pairedCoach.full_name}</div>
              <div style="font-size: 13px; color: var(--text-muted);">${state.pairedCoach.phone || 'Телефон не указан'}</div>
            </div>
            <span class="role-pill trainer">Ваш тренер</span>
          </div>
          <div style="display: flex; gap: 8px;">
            ${state.pairedCoach.phone ? `<a href="tel:${state.pairedCoach.phone}" class="btn btn-secondary btn-sm" style="flex: 1; text-decoration: none;">Позвонить</a>` : ''}
            <button id="btn-unpair-coach" class="btn btn-danger btn-sm" style="flex: 1;">Отвязать тренера</button>
          </div>
        </div>
      `;

      const btnUnpair = document.getElementById('btn-unpair-coach');
      if (btnUnpair) {
        btnUnpair.onclick = async () => {
          if (!confirm(`Вы действительно хотите отвязать тренера ${state.pairedCoach.full_name}?`)) return;
          try {
            await api('/api/athlete/unpair', { method: 'POST' });
            state.pairedCoach = null;
            renderCoachStatus();
            showToast('Тренер успешно отвязан', 'success');
          } catch {}
        };
      }
    } else {
      el.coachInfoContainer.innerHTML = `
        <div class="empty-state" style="padding: 24px;">
          <div class="empty-state-title" style="font-size: 14px;">Тренер не привязан</div>
          <p style="font-size: 12px; max-width: 320px;">
            Покажите QR-код тренеру или продиктуйте 6-значный PIN выше, чтобы открыть доступ к вашему дневнику.
          </p>
        </div>
      `;
    }
  }

  // --- View: Trainer Clients ---
  async function loadTrainerClients() {
    try {
      const data = await api('/api/trainer/clients');
      state.clients = data.clients || [];
      renderTrainerClients();
    } catch {}
  }

  function renderTrainerClients() {
    el.trainerClientsContainer.innerHTML = '';

    if (state.clients.length === 0) {
      el.trainerClientsContainer.innerHTML = `
        <div class="bento-col-12">
          <div class="empty-state">
            <div class="empty-state-title">У вас пока нет подопечных</div>
            <p style="font-size: 13px;">Введите 6-значный PIN атлета в форме выше, чтобы привязать его.</p>
          </div>
        </div>
      `;
      return;
    }

    state.clients.forEach(client => {
      const card = document.createElement('div');
      card.className = 'bento-col-4';
      card.innerHTML = `
        <div class="bento-card" style="padding: 20px;">
          <div style="display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 14px; gap: 8px;">
            <div style="min-width: 0; flex: 1;">
              <h3 class="truncate" style="font-size: 16px; font-weight: 700;" title="${client.full_name}">${client.full_name}</h3>
              <p style="font-size: 12px; color: var(--text-muted);">${client.phone || 'Без телефона'}</p>
            </div>
            <span class="role-pill athlete">Атлет</span>
          </div>

          <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin-bottom: 16px; background-color: var(--bg-primary); padding: 10px; border-radius: var(--radius-md); border: 1px solid var(--border-subtle);">
            <div>
              <div class="label-micro">Тренировок</div>
              <div class="tabular-nums" style="font-size: 18px; font-weight: 700;">${client.sessions_count || 0}</div>
            </div>
            <div>
              <div class="label-micro">Тоннаж</div>
              <div class="tabular-nums" style="font-size: 18px; font-weight: 700;">${Math.round(client.total_tonnage || 0).toLocaleString('ru-RU')} кг</div>
            </div>
          </div>

          <div style="display: flex; gap: 8px;">
            ${client.phone ? `<a href="tel:${client.phone}" class="btn btn-secondary btn-sm" style="flex: 1; text-decoration: none;">Звонок</a>` : ''}
            <button class="btn btn-danger btn-sm btn-unpair-client" style="flex: 1;">Отвязать</button>
          </div>
        </div>
      `;

      const btnUnpair = card.querySelector('.btn-unpair-client');
      btnUnpair.onclick = async () => {
        if (!confirm(`Отвязать подопечного ${client.full_name}?`)) return;
        try {
          await api('/api/trainer/unpair', {
            method: 'POST',
            body: JSON.stringify({ athleteId: client.id })
          });
          state.clients = state.clients.filter(c => c.id !== client.id);
          renderTrainerClients();
          showToast(`Атлет ${client.full_name} отвязан`, 'success');
        } catch {}
      };

      el.trainerClientsContainer.appendChild(card);
    });
  }

  // --- View: Leaderboard (Zero Mocks) ---
  async function loadLeaderboard() {
    try {
      const data = await api('/api/leaderboard');
      renderLeaderboard(data.leaderboard || []);
    } catch {}
  }

  function renderLeaderboard(list) {
    el.leaderboardTbody.innerHTML = '';

    if (list.length === 0) {
      el.leaderboardTbody.innerHTML = `
        <tr>
          <td colspan="5" style="text-align: center; padding: 48px 16px; color: var(--text-muted);">
            В таблице пока нет публичных атлетов. Включите публичный профиль и проведите тренировку!
          </td>
        </tr>
      `;
      return;
    }

    list.forEach((entry, idx) => {
      const rank = idx + 1;
      let medal = String(rank);
      if (rank === 1) medal = '🥇 1';
      else if (rank === 2) medal = '🥈 2';
      else if (rank === 3) medal = '🥉 3';

      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td style="font-weight: 700; font-family: var(--font-mono);">${medal}</td>
        <td style="font-weight: 600; color: var(--text-primary);">${entry.full_name}</td>
        <td style="text-align: right;" class="tabular-nums">${entry.workouts_count || 0}</td>
        <td style="text-align: right; font-weight: 600;" class="tabular-nums">${Math.round(entry.total_tonnage || 0).toLocaleString('ru-RU')}</td>
        <td style="text-align: right; font-weight: 700; color: var(--accent-emerald);" class="tabular-nums">${Math.round(entry.points || 0)}</td>
      `;
      el.leaderboardTbody.appendChild(tr);
    });
  }

  // --- Event Listeners Initialization ---
  function setupEvents() {
    // Auth Mode Tabs
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

    // Login Form Submit
    el.formLogin.onsubmit = async (e) => {
      e.preventDefault();
      const username = document.getElementById('login-username').value.trim();
      const password = document.getElementById('login-password').value;

      try {
        const data = await api('/api/login', {
          method: 'POST',
          body: JSON.stringify({ username, password })
        });
        state.token = data.token;
        state.user = data.user;
        localStorage.setItem('fit_token', data.token);
        showToast(`Добро пожаловать, ${data.user.fullName || data.user.username}!`, 'success');
        showDashboard();
      } catch {}
    };

    // Register Form Submit
    el.formRegister.onsubmit = async (e) => {
      e.preventDefault();
      const username = document.getElementById('reg-username').value.trim();
      const password = document.getElementById('reg-password').value;
      const fullName = document.getElementById('reg-fullname').value.trim();
      const phone = document.getElementById('reg-phone').value.trim();
      const role = document.getElementById('reg-role').value;

      try {
        const data = await api('/api/register', {
          method: 'POST',
          body: JSON.stringify({ username, password, fullName, phone, role })
        });
        state.token = data.token;
        state.user = data.user;
        localStorage.setItem('fit_token', data.token);
        showToast('Аккаунт успешно создан!', 'success');
        showDashboard();
      } catch {}
    };

    // Header Actions
    el.btnToggleRole.onclick = toggleRole;
    el.btnLogout.onclick = logout;

    // Date Picker Change
    el.workoutDatePicker.onchange = (e) => {
      state.currentDate = e.target.value;
      loadWorkoutSets();
    };

    // Quick Exercise Chips
    el.quickChips.onclick = (e) => {
      const chip = e.target.closest('.chip');
      if (!chip) return;
      const name = chip.dataset.exercise;
      el.inputExerciseName.value = name;
      checkLastExerciseStats(name);
      el.inputWeight.focus();
    };

    el.inputExerciseName.oninput = (e) => {
      checkLastExerciseStats(e.target.value.trim());
    };

    // Add Workout Set Form
    el.formAddSet.onsubmit = async (e) => {
      e.preventDefault();
      const exerciseName = el.inputExerciseName.value.trim();
      const weightKg = parseFloat(el.inputWeight.value);
      const reps = parseInt(el.inputReps.value, 10);
      const rpe = parseFloat(el.inputRpe.value) || 8.0;

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
        loadWorkoutSets();
        // Keep exercise & weight for convenience, reset focus
        el.inputReps.focus();
      } catch {}
    };

    // Copy PIN button
    el.btnCopyPin.onclick = async () => {
      const pin = state.user?.pairing_code;
      if (!pin) return;
      await navigator.clipboard.writeText(pin);
      showToast('6-значный PIN скопирован в буфер', 'success');
    };

    // Copy Link button
    el.btnCopyLink.onclick = async () => {
      const pin = state.user?.pairing_code;
      if (!pin) return;
      const link = `https://fitnessapp.pro/pair?code=${pin}`;
      await navigator.clipboard.writeText(link);
      showToast('Ссылка для тренера скопирована', 'success');
    };

    // Regenerate PIN button
    el.btnRegeneratePin.onclick = async () => {
      try {
        const data = await api('/api/athlete/regenerate-pin', { method: 'POST' });
        state.user.pairing_code = data.pairingCode;
        renderPairingView();
        showToast('PIN успешно обновлён', 'success');
      } catch {}
    };

    // Privacy Toggle
    el.privacyToggleCheckbox.onchange = async (e) => {
      const isPublic = e.target.checked;
      try {
        await api('/api/athlete/privacy', {
          method: 'POST',
          body: JSON.stringify({ isPrivate: !isPublic })
        });
        state.user.is_private = isPublic ? 0 : 1;
        updatePrivacySliderUI(isPublic);
        showToast(isPublic ? 'Профиль теперь виден в лидерборде' : 'Профиль скрыт из лидерборда', 'success');
      } catch {
        e.target.checked = !isPublic;
      }
    };

    // Trainer Pairing Form
    el.trainerPairCode.oninput = (e) => {
      // Auto-filter non-digits, no dashes required
      e.target.value = e.target.value.replace(/\D/g, '').slice(0, 6);
    };

    el.formTrainerPair.onsubmit = async (e) => {
      e.preventDefault();
      const code = el.trainerPairCode.value.trim();
      if (code.length !== 6) {
        showToast('Введите ровно 6 цифр PIN-кода', 'error');
        return;
      }
      try {
        const data = await api('/api/trainer/pair', {
          method: 'POST',
          body: JSON.stringify({ code })
        });
        showToast(data.message || 'Атлет успешно привязан!', 'success');
        el.trainerPairCode.value = '';
        loadTrainerClients();
      } catch {}
    };

    // Refresh Leaderboard
    el.btnRefreshLeaderboard.onclick = loadLeaderboard;
  }

  // --- Bootstrap ---
  setupEvents();
  initAuth();

})();
