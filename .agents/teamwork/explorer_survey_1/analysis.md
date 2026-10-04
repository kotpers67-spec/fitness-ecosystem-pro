# Audit & Survey Analysis: R1 UI/UX Layout, Client Experience, SPA & PWA

**Author**: Survey Explorer 1 (`explorer_survey_1`)  
**Date**: 2026-10-04  
**Scope**: `F:\Projects\fitness-ecosystem-pro\web`  
**Test Suite Verification**: `npm test` executed — **68 of 68 tests PASS** (55/55 in `security.test.js`, 13/13 in `pin_2fa.test.js`).

---

## Executive Summary

A comprehensive, end-to-end audit was conducted on the Web Portal (`web`) spanning responsive UI/UX layouts, PWA configuration, Service Worker offline caching, the Mobile App Installation modal (v1.0.8 APKs), and compliance with Anti-Overlap Guard and Swiss/Clean dark UI guidelines.

### Overall Status:
- **Zero-Mocks Compliance**: 100% verified. Database queries (`db.js:583`) and leaderboards strictly use real completed workouts (`s.is_completed = 1`), with privacy enforcement (`u.is_private = 0`).
- **Security & Authorization**: 100% pass across all OWASP tests (Anti-SQLi, Anti-XSS, Rate Limiting, RBAC, IDOR protection, 5-min TTL, Telegram 2FA).
- **v1.0.8 APK Releases**: Download links point to active GitHub releases (`athlete-pro-v1.0.8.apk` and `trainer-pro-v1.0.8.apk`, HTTP 302 verified), and local binaries (~13 MB each) reside in `web/releases/`.
- **UI/UX & Anti-Overlap Defects**: 4 critical CSS class mismatches identified that break trainer client list layouts and status badges; bottom navigation bar scrolling bug; missing pre-cache assets in PWA Service Worker.

---

## 1. Screen-by-Screen Layout & Responsiveness Audit

### 1.1 Authentication Screen (`#screen-auth`)
- **Structure**: PRO badge header, direct APK download button (`.btn-open-download-modal`), 1-click Telegram login (`#btn-telegram-auth`), login/register switcher tabs, role selection radio cards (`athlete` vs `trainer`), and owner Telegram contacts (`@SantiLA213`, `@Spirit5449`).
- **Responsiveness**: Fits viewports down to 320px cleanly. On desktop (>520px), centered in 480px shell.
- **Accessibility & Anti-Overlap**: Inputs use native `autocomplete` attributes (`username`, `current-password`, `new-password`). Colors provide WCAG AAA contrast (Lime `#c8ff00` on `#0d0d0d`: 16.5:1; White on `#0d0d0d`: 18.8:1).

### 1.2 Athlete Workout Diary (`#screen-athlete-workout`)
- **Structure**: Date navigator (◀ / Сегодня / ▶), assignment notice, collapsible self-workout set addition form (with quick exercise chips), and dynamic exercise matrix.
- **Responsiveness & Anti-Overlap**:
  - `.triple-input-grid` (Weight / Reps / RPE) uses `grid-template-columns: 1fr 1fr 1fr; gap: 8px;`. On 320px screens, grid columns lack `min-width: 0;` which can cause input spin-arrows to squeeze labels.
  - `.set-row`: `.set-weight-reps` has `flex: 1` but lacks `min-width: 0` and lacks `.tabular-nums`. On narrow viewports (<360px), long set descriptions push against `.set-rpe-badge` and action buttons (`.set-check-btn`, `.set-delete-btn`).
  - `.exercise-group-title`: Flex space-between without `min-width: 0` on exercise name span; long exercise names can collide with the sets counter pill.

### 1.3 Athlete Progress & Charts (`#screen-athlete-history`)
- **Structure**:
  1. Body weight progress card: Current weight, start weight, Canvas chart (`#athlete-weight-canvas`), empty state.
  2. Exercise strength curves: Exercise dropdown, 3-Scale summary badges (Weight, Sets, Reps), Canvas chart (`#athlete-exercise-canvas`), 3-scale legend.
  3. History list of sets for the chosen date.
- **Canvas Rendering Quality**:
  - Canvas elements use hardcoded internal dimensions (`400x150` and `400x190`) with CSS `width: 100% !important; height: auto !important;`.
  - Coordinate rendering does not scale by `window.devicePixelRatio`. On High-DPI smartphone screens (Pixel 8 DPR ~2.625), chart lines, circles, and font labels appear slightly blurry.
- **History Items**: Left and right divs in `.history-item-row` lack `min-width: 0` and numeric stats lack `.tabular-nums`.

### 1.4 Competitions & Leaderboard (`#screen-athlete-leaderboard`)
- **Structure**: Header with refresh button (`#btn-refresh-leaderboard`), 3-card Bento metrics grid (Workouts, Tonnage, Points), dynamic ranking table with top-3 gold/silver/bronze badges and `(ВЫ)` highlight.
- **Zero-Mocks**: Verified `db.getLeaderboard()` (`db.js:583-598`):
  ```sql
  SELECT u.id, u.full_name, u.avatar_base64,
         COUNT(DISTINCT ws.id) as workouts_count,
         COALESCE(SUM(s.weight_kg * s.reps), 0) as total_tonnage,
         ROUND(COUNT(DISTINCT ws.id) * 100 + COALESCE(SUM(s.weight_kg * s.reps), 0) * 0.1) as points
  FROM users u
  JOIN workout_sessions ws ON u.id = ws.athlete_id
  JOIN workout_sets s ON ws.id = s.session_id AND s.is_completed = 1
  WHERE u.role = 'athlete' AND u.is_private = 0
  GROUP BY u.id, u.full_name, u.avatar_base64
  HAVING workouts_count > 0 AND total_tonnage > 0
  ORDER BY points DESC, total_tonnage DESC
  ```
  Strictly real athletes with finished workouts; private athletes filtered out.
- **Anti-Overlap**: `.athlete-meta` has `min-width: 0`, but `.athlete-name-text` lacks `truncate` + tooltip or `break-words`.

### 1.5 Athlete Profile (`#screen-athlete-profile`)
- **Structure**: Avatar/Name/Phone card with Base64 image compression (<15 KB), 6-digit dynamic pairing PIN with countdown badge (`⏱ Действует: 05:00`), vector SVG QR code (pure digits, no external URLs), Telegram 2FA card, privacy switcher, cloud sync trigger, paired coach card, and mobile apps download trigger.
- **Anti-Overlap**: Vector QR code rendered cleanly via inline SVG. Countdown badge is styled with monospace font and tabular numbers.

### 1.6 Trainer Screens (`#screen-trainer-home`, `#screen-trainer-workout`, `#screen-trainer-history`, `#screen-trainer-settings`)
- **Critical UI Bugs Discovered**:
  1. **Trainer Clients List Broken Layout**: In `app.js:1274-1296`, dynamically created cards have classes `.client-item-card`, `.client-item-left`, `.client-avatar-badge`, `.client-name`, `.client-sub`, `.client-item-actions`, `.btn-sm`.
     - In `styles.css`, NONE of these classes exist!
     - Because `.app-btn` has `width: 100%; height: 44px;`, buttons `.btn-select-client` ("Выбрать") and `.btn-unpair-client` ("✕") take 100% width and stack vertically, completely breaking the card layout.
  2. **Trainer Active Client Picker Class Mismatch**: In `index.html:538-539`, classes are `picker-label` and `picker-name`. In `styles.css:947-955`, classes were named `.active-client-name` and `.active-client-phone`. Result: `<h3 class="picker-name">` has no CSS and renders with browser default unstyled `<h3>` margins and fonts.
  3. **Trainer Workout Matrix Unstyled Status Pills**: In `app.js:1410`, `<span class="status-pill ${isComp ? 'done' : 'pending'}">` is created, but `.status-pill`, `.status-pill.done`, `.status-pill.pending` are completely absent in `styles.css`. Renders as plain unstyled inline text directly next to delete button.

---

## 2. PWA Implementation & Service Worker Audit

### 2.1 Manifest Configuration (`manifest.json`)
- File path: `web/src/public/manifest.json`
- `name`: "Fitness Ecosystem Pro", `short_name`: "Fitness Pro"
- `start_url`: "/", `display`: "standalone"
- `theme_color`: "#0d0d0d", `background_color`: "#0d0d0d", `orientation`: "portrait"
- Icons defined: 192x192 (any & maskable), 512x512 (any & maskable).
- **Assessment**: Manifest is fully compliant with W3C Web App Manifest standards.

### 2.2 Service Worker (`sw.js`)
- Cache name: `fitpro-v1`
- `ASSETS_TO_CACHE`:
  ```javascript
  const ASSETS_TO_CACHE = [
    '/',
    '/index.html',
    '/styles.css',
    '/app.js',
    '/qr.js',
    '/manifest.json'
  ];
  ```
- **Defects Discovered**:
  1. **Missing Pre-Cache Assets**: `qrcode.min.js`, `icon-192.png`, `icon-512.png`, and `icon.svg` are omitted from `ASSETS_TO_CACHE`. If user installs PWA and disconnects before navigating, `qrcode.min.js` is unavailable offline.
  2. **Fetch Catch-Handler Flaw**: In `sw.js:47-51`:
     ```javascript
     }).catch(() => {
       if (event.request.headers.get('accept')?.includes('text/html')) {
         return caches.match('/index.html');
       }
     })
     ```
     If an un-cached non-HTML static asset fails network fetch while offline, `.catch()` returns `undefined`, triggering an uncaught Promise rejection inside the browser Service Worker.
  3. **Offline Auth Session State**: `localStorage` only caches `fit_token`, not `state.user`. When launched offline, `initAuth()` calls `api('/api/me')` which fails. The catch block simply logs a console warning without rendering the cached dashboard, leaving the user on an uninitialized/auth screen.
  4. **Missing Inline Install Banner in HTML**: `app.js:2470-2476` attempts to display `el.pwaInstallBanner` (`id="pwa-install-banner"`), but that element does not exist in `index.html`. Only the modal install button (`#btn-trigger-pwa-install-dialog`) exists.

---

## 3. Mobile App Installation Modal & APK Verification

### 3.1 Modal Content & Controls (`dialog-download-app`)
- Path: `web/src/public/index.html:1008-1062`
- **Athlete Pro Card**:
  - Title: "Athlete Pro для Android"
  - Version: `v1.0.8 (Latest)`
  - URL: `https://github.com/kotpers67-spec/fitness-ecosystem-pro/releases/download/v1.0.8/athlete-pro-v1.0.8.apk`
  - Verification: `curl -I` returned `HTTP/1.1 302 Found` with redirect to GitHub release asset.
- **Trainer Pro Card**:
  - Title: "Trainer Pro для Android"
  - Version: `v1.0.8 (Latest)`
  - URL: `https://github.com/kotpers67-spec/fitness-ecosystem-pro/releases/download/v1.0.8/trainer-pro-v1.0.8.apk`
  - Verification: `curl -I` returned `HTTP/1.1 302 Found` with redirect to GitHub release asset.
- **PWA Card**:
  - Includes step-by-step instructions for iOS Safari and Android Chrome.
  - Button `#btn-trigger-pwa-install-dialog` triggers `state.deferredInstallPrompt.prompt()`.
- **Dialog Controls**:
  - Bottom button `#btn-close-download-dialog` properly invokes `dialog.close()`.
  - **Minor observations**:
    * Dialog header lacks an 'X' icon button (unlike `#dialog-add-weight`).
    * Dialog lacks a backdrop click-to-close listener.
    * In `server.js:1542`, the fallback tag for local `/releases/` route redirects to `v1.0.5` or `v1.0.6` instead of `v1.0.8` if a requested file is absent.
    * Both APKs exist locally in `web/releases/`: `athlete-pro-v1.0.8.apk` (13,042,913 bytes) and `trainer-pro-v1.0.8.apk` (13,193,983 bytes). Pointing modal links to `/releases/...` would allow local offline downloads without requiring external GitHub access.

---

## 4. Anti-Overlap Guard & UI/UX Standards Compliance

| Checkpoint | Status | Details |
|---|---|---|
| **Framer-Grade Dark Palette** | PASS | `#0d0d0d`, `#171717`, `#222222`, `#c8ff00`, `border-white/10`. |
| **Contrast Ratios (WCAG)** | PASS | Accent lime 16.5:1, White 18.8:1, Secondary text 6.2:1. |
| **Desktop Responsiveness** | WARNING | Shell constrained to fixed `max-width: 480px`. No multi-column or wide layout for desktop. |
| **Sticky Bottom Navigation** | FAIL | Shell uses `min-height: 100vh;` without `height: 100vh; overflow: hidden;`. On long content, `.app-bottom-nav` (`position: absolute; bottom: 0`) scrolls off-screen to the end of document. |
| **Truncation & Tooltips** | WARNING | Only `#topbar-user-name` has `.truncate`. Missing dynamic `title` tooltip. Other names lack truncation. |
| **Tabular Numbers** | WARNING | Used on PIN and Bento cards, but missing on `.set-weight-reps` and history items. |
| **Flex/Grid child `min-w-0`** | WARNING | Missing on `.triple-input-grid` items, `.set-weight-reps`, and `.history-item-row` divs. |
| **Dynamic CSS Class Mismatches** | FAIL | Missing styles for `.client-item-card`, `.picker-name`, and `.status-pill`. |

---

## 5. Prioritized Remediation Roadmap

1. **Fix Missing CSS Classes**:
   - Add `.client-item-card`, `.client-item-left`, `.client-avatar-badge`, `.client-name`, `.client-sub`, `.client-item-actions`, `.btn-sm` to `styles.css`.
   - Add `.picker-name` and `.picker-label` to `styles.css` (or alias `.active-client-name`).
   - Add `.status-pill`, `.status-pill.done`, `.status-pill.pending` to `styles.css`.
2. **Fix Sticky Bottom Navigation**:
   - Add `height: 100vh; height: 100dvh; overflow: hidden;` to `.mobile-app-shell` so `.app-main-viewport` scrolls internally, keeping `.app-bottom-nav` reliably docked at the bottom of the screen.
3. **PWA Offline Asset Hardening**:
   - Add `'/qrcode.min.js'`, `'/icon-192.png'`, `'/icon-512.png'`, `'/icon.svg'` to `ASSETS_TO_CACHE` in `sw.js`.
   - Update `sw.js` fetch handler to avoid returning `undefined` on failed non-HTML fetches.
   - Cache user profile in `localStorage` for offline dashboard viewing.
4. **Anti-Overlap Guard Reinforcement**:
   - Set dynamic `title` on truncated elements (`el.topbarUserName.title = displayName`).
   - Add `min-w-0` and `tabular-nums` to `.set-weight-reps` and `.history-item-row`.
   - Scale canvas charts by `window.devicePixelRatio` for crisp rendering on Pixel 8 / Retina screens.
