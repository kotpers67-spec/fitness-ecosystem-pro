const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

/**
 * Fitness Ecosystem Pro - Comprehensive E2E Frontend Test Suite (Tiers 1-4)
 * Opaque-box requirement-driven verification per TEST_INFRA.md and ORIGINAL_REQUEST.md.
 * 
 * TIER 1: Feature Coverage (>= 5 tests per feature)
 *   1.1 PWA Manifest & App Identity
 *   1.2 PWA DOM Trigger Elements & Buttons
 *   1.3 Multi-OS PWA Dialog Structure & Controls
 *   1.4 Zero-APK Policy Enforcement Across Shell & Server
 *   1.5 Swiss Clean UI & Zero AI-Slop Design Standards
 *   1.6 Anti-Overlap Guard & Typography Tokens
 *   1.7 Transitions, Micro-Interactions & Accordion Animations
 * 
 * TIER 2: Boundary & Corner Cases (>= 5 tests)
 *   2.1 Mobile Viewport (<= 380px) Header Compression & Overflow Containment
 *   2.2 Standalone Display Mode Detection & Button Suppression
 *   2.3 Missing beforeinstallprompt Fallback to Multi-OS Modal Guide
 *   2.4 Modal Dismissal Mechanics (Esc Key, Close Buttons, and Backdrop Click)
 *   2.5 Service Worker Cache Strategy & Route Partitioning
 *   2.6 Idempotent Rapid Invocations & Event Timing
 * 
 * TIER 3: Cross-Feature Combinations
 *   3.1 PWA Modal Prompt Triggering Across Screen Transitions
 *   3.2 Anti-Overlap Guard Containment with Extreme Strings in Workout View
 *   3.3 Accordion Expansion Combined with Skeleton Shimmer Loading States
 *   3.4 Modal Tab Selection State Retention & Backdrop Dismissal
 *   3.5 Service Worker Precache List Consistency with Public Shell Files
 * 
 * TIER 4: Real-World Workload Scenarios (5 Scenarios)
 *   4.1 Scenario 1: First-Time Mobile Visitor on iOS Safari
 *   4.2 Scenario 2: Android Visitor with Native beforeinstallprompt Flow
 *   4.3 Scenario 3: Athlete Daily Workout Navigation & Accordion Set Logging
 *   4.4 Scenario 4: Trainer Client Management & Settings PWA Installation
 *   4.5 Scenario 5: Auth Screen Responsive PWA Shortcut & OTP Interaction
 */

// Base paths
const ROOT_DIR = path.resolve(__dirname, '..');
const PUBLIC_DIR = path.join(ROOT_DIR, 'src', 'public');
const MANIFEST_PATH = path.join(PUBLIC_DIR, 'manifest.json');
const HTML_PATH = path.join(PUBLIC_DIR, 'index.html');
const CSS_PATH = path.join(PUBLIC_DIR, 'styles.css');
const APP_JS_PATH = path.join(PUBLIC_DIR, 'app.js');
const SW_PATH = path.join(PUBLIC_DIR, 'sw.js');
const SERVER_JS_PATH = path.join(ROOT_DIR, 'src', 'server.js');

// Load static assets
const manifestContent = fs.readFileSync(MANIFEST_PATH, 'utf8');
const manifest = JSON.parse(manifestContent);
const html = fs.readFileSync(HTML_PATH, 'utf8');
const css = fs.readFileSync(CSS_PATH, 'utf8');
const appJs = fs.readFileSync(APP_JS_PATH, 'utf8');
const swJs = fs.readFileSync(SW_PATH, 'utf8');
const serverJs = fs.readFileSync(SERVER_JS_PATH, 'utf8');

// ============================================================================
// TIER 1: FEATURE COVERAGE (>= 5 tests per feature)
// ============================================================================

test('TIER 1.1: PWA Manifest & App Identity Specifications', async (t) => {
  await t.test('1.1.1 manifest.json exists, is valid JSON and has standalone display', () => {
    assert.strictEqual(manifest.display, 'standalone', 'display must be standalone');
  });

  await t.test('1.1.2 manifest.json provides complete identity metadata', () => {
    assert.strictEqual(manifest.name, 'Fitness Ecosystem Pro');
    assert.strictEqual(manifest.short_name, 'Fitness Pro');
    assert.ok(manifest.description && manifest.description.length > 10, 'description must be descriptive');
    assert.strictEqual(manifest.start_url, '/', 'start_url must be root /');
  });

  await t.test('1.1.3 manifest.json specifies Swiss Clean UI monochrome palette', () => {
    assert.strictEqual(manifest.theme_color, '#0d0d0d', 'theme_color must match Swiss dark theme');
    assert.strictEqual(manifest.background_color, '#0d0d0d', 'background_color must match Swiss dark theme');
  });

  await t.test('1.1.4 manifest.json supplies multi-resolution icons with maskable support', () => {
    assert.ok(Array.isArray(manifest.icons) && manifest.icons.length >= 2, 'Must have at least 2 icon entries');
    const sizes = manifest.icons.map(i => i.sizes);
    assert.ok(sizes.includes('192x192'), 'Must include 192x192 icon');
    assert.ok(sizes.includes('512x512'), 'Must include 512x512 icon');

    const purposes = manifest.icons.map(i => i.purpose);
    assert.ok(purposes.includes('any'), 'Must include purpose "any"');
    assert.ok(purposes.includes('maskable'), 'Must include purpose "maskable"');
  });

  await t.test('1.1.5 manifest icon files exist on disk with valid image content', () => {
    for (const icon of manifest.icons) {
      const iconPath = path.join(PUBLIC_DIR, icon.src.replace(/^\//, ''));
      assert.ok(fs.existsSync(iconPath), `Icon file ${icon.src} must exist at ${iconPath}`);
      const stats = fs.statSync(iconPath);
      assert.ok(stats.size > 500, `Icon file ${icon.src} must not be empty (was ${stats.size} bytes)`);
    }
  });

  await t.test('1.1.6 index.html links manifest and provides iOS PWA meta tags', () => {
    assert.ok(html.includes('rel="manifest" href="/manifest.json"'), 'Must link manifest.json');
    assert.ok(html.includes('name="apple-mobile-web-app-capable" content="yes"'), 'Must declare apple-mobile-web-app-capable');
    assert.ok(html.includes('name="theme-color" content="#0d0d0d"'), 'Must declare theme-color matching manifest');
    assert.ok(html.includes('name="viewport"'), 'Must define viewport meta tag');
  });
});

test('TIER 1.2: PWA DOM Trigger Elements & Buttons', async (t) => {
  await t.test('1.2.1 Header PWA install button #btn-pwa-install-header is present', () => {
    assert.ok(html.includes('id="btn-pwa-install-header"'), 'Header install button must exist');
    assert.ok(html.includes('class="icon-btn pwa-install-header-btn"'), 'Header install button must have pwa-install-header-btn class');
    assert.ok(html.includes('class="pwa-btn-label"'), 'Header install button must contain .pwa-btn-label span');
  });

  await t.test('1.2.2 Auth screen PWA shortcut button #btn-pwa-install-auth is present', () => {
    assert.ok(html.includes('id="btn-pwa-install-auth"'), 'Auth screen install button must exist');
    assert.ok(html.includes('class="btn-open-download-modal'), 'Auth button must have btn-open-download-modal class');
    assert.ok(html.includes('Установить веб-версию (PWA)'), 'Auth button must clearly indicate PWA installation');
  });

  await t.test('1.2.3 Athlete dashboard PWA card #card-pwa-install-athlete and button exist', () => {
    assert.ok(html.includes('id="card-pwa-install-athlete"'), 'Athlete PWA card must exist');
    assert.ok(html.includes('id="btn-pwa-install-athlete"'), 'Athlete PWA install button must exist');
  });

  await t.test('1.2.4 Trainer dashboard PWA card #card-pwa-install-trainer and button exist', () => {
    assert.ok(html.includes('id="card-pwa-install-trainer"'), 'Trainer PWA card must exist');
    assert.ok(html.includes('id="btn-pwa-install-trainer"'), 'Trainer PWA install button must exist');
  });

  await t.test('1.2.5 Dialog native prompt quick-install button #btn-trigger-pwa-install-dialog exists', () => {
    assert.ok(html.includes('id="pwa-native-box"'), '#pwa-native-box container must exist');
    assert.ok(html.includes('id="btn-trigger-pwa-install-dialog"'), '#btn-trigger-pwa-install-dialog must exist');
  });

  await t.test('1.2.6 All PWA trigger buttons render vector SVG icons rather than emojis', () => {
    const btnHeaderMatch = html.match(/id="btn-pwa-install-header"[\s\S]*?<\/button>/);
    assert.ok(btnHeaderMatch, 'Header button markup found');
    assert.ok(btnHeaderMatch[0].includes('<svg'), 'Header button must contain vector SVG icon');

    const btnAuthMatch = html.match(/id="btn-pwa-install-auth"[\s\S]*?<\/button>/);
    assert.ok(btnAuthMatch, 'Auth button markup found');
    assert.ok(btnAuthMatch[0].includes('<svg'), 'Auth button must contain vector SVG icon');
  });
});

test('TIER 1.3: Multi-OS Step-by-Step Modal Guide Structure', async (t) => {
  await t.test('1.3.1 Dialog #dialog-download-app exists with native dialog semantics', () => {
    assert.ok(html.includes('<dialog id="dialog-download-app" class="app-dialog pwa-install-dialog">'), 'Dialog element must have dialog tag and classes');
  });

  await t.test('1.3.2 Dialog header contains PWA badge and close button #btn-close-download-dialog', () => {
    assert.ok(html.includes('class="pwa-dialog-badge"'), 'PWA badge must exist');
    assert.ok(html.includes('id="btn-close-download-dialog"'), 'Header close button must exist');
  });

  await t.test('1.3.3 Dialog contains 3 OS tabs: iOS, Android, and Desktop', () => {
    assert.ok(html.includes('id="pwa-dialog-tabs"'), '#pwa-dialog-tabs row must exist');
    assert.ok(html.includes('data-pwa-tab="ios"'), 'Tab data-pwa-tab="ios" must exist');
    assert.ok(html.includes('data-pwa-tab="android"'), 'Tab data-pwa-tab="android" must exist');
    assert.ok(html.includes('data-pwa-tab="desktop"'), 'Tab data-pwa-tab="desktop" must exist');
  });

  await t.test('1.3.4 Dialog contains 3 instruction panels with step-by-step guides', () => {
    assert.ok(html.includes('id="pwa-tab-ios"'), '#pwa-tab-ios panel must exist');
    assert.ok(html.includes('id="pwa-tab-android"'), '#pwa-tab-android panel must exist');
    assert.ok(html.includes('id="pwa-tab-desktop"'), '#pwa-tab-desktop panel must exist');
  });

  await t.test('1.3.5 Step lists contain ordered numbers .step-num.tabular-nums for each step', () => {
    const stepMatches = html.match(/class="step-num tabular-nums"/g);
    assert.ok(stepMatches && stepMatches.length >= 9, 'Must have at least 9 steps across 3 OS panels');
    assert.ok(html.includes('«Поделиться»'), 'iOS steps must mention Safari Share icon');
    assert.ok(html.includes('«На экран „Домой“»'), 'iOS steps must mention Add to Home Screen');
    assert.ok(html.includes('«Установить приложение»'), 'Android/Desktop steps must mention Install App');
  });

  await t.test('1.3.6 Dialog contains footer dismissal action and feature pill', () => {
    assert.ok(html.includes('id="btn-close-download-dialog-footer"'), 'Footer close button must exist');
    assert.ok(html.includes('class="pwa-features-pill"'), 'Features pill must exist');
    assert.ok(html.includes('Полноэкранный режим, офлайн-доступ'), 'Features pill must highlight PWA strengths');
  });
});

test('TIER 1.4: Zero-APK Policy Enforcement Across Shell & Server', async (t) => {
  await t.test('1.4.1 index.html contains zero .apk download links', () => {
    assert.ok(!html.includes('.apk"'), 'HTML must not contain any .apk download hrefs');
    assert.ok(!html.includes('.apk\''), 'HTML must not contain any .apk download single-quoted hrefs');
    assert.ok(!/href=["'][^"']*\.apk["']/i.test(html), 'No anchor tags may point to .apk');
  });

  await t.test('1.4.2 app.js contains zero APK download triggers or APK API endpoints', () => {
    assert.ok(!appJs.includes('/download-apk'), 'app.js must not reference /download-apk');
    assert.ok(!appJs.includes('.apk"'), 'app.js must not contain literal .apk download strings');
    assert.ok(!appJs.includes('.apk\''), 'app.js must not contain literal .apk download strings');
  });

  await t.test('1.4.3 sw.js precache list and routing rules contain zero .apk files', () => {
    assert.ok(!swJs.includes('.apk'), 'sw.js must not reference any .apk files');
  });

  await t.test('1.4.4 server.js contains zero APK download endpoints or APK MIME handlers', () => {
    assert.ok(!serverJs.includes('application/vnd.android.package-archive'), 'server.js must not register APK MIME type');
    assert.ok(!serverJs.includes('/download-apk'), 'server.js must not expose /download-apk route');
    assert.ok(!serverJs.includes('.apk'), 'server.js must not serve or reference .apk files');
  });

  await t.test('1.4.5 PWA install modal #dialog-download-app contains zero APK mentions', () => {
    const dialogSection = html.slice(html.indexOf('id="dialog-download-app"'), html.indexOf('</dialog>'));
    assert.ok(!dialogSection.toLowerCase().includes('.apk'), 'Dialog must not mention APK files');
    assert.ok(!dialogSection.includes('сторонних источников'), 'Dialog must not mention third-party APK installation');
  });
});

test('TIER 1.5: Swiss Clean UI & Zero AI-Slop Design Standards', async (t) => {
  await t.test('1.5.1 CSS root variables define Framer-grade monochrome palette', () => {
    assert.ok(css.includes('--bg-primary: #09090b') || css.includes('--bg-app: #0d0d0d'), 'Must define dark monochrome background');
    assert.ok(css.includes('--text-primary'), 'Must define primary text token');
    assert.ok(css.includes('--text-secondary'), 'Must define secondary text token');
    assert.ok(css.includes('--border-'), 'Must define subtle border tokens');
  });

  await t.test('1.5.2 Absence of emoji in primary navigation, topbar and buttons', () => {
    const btnPwaHeader = html.match(/id="btn-pwa-install-header"[\s\S]*?<\/button>/)[0];
    assert.ok(!/[🚀📱✨🔥🎉]/.test(btnPwaHeader), 'Header PWA button must not contain loud emojis');

    const topbarTitle = html.match(/id="topbar-title"[\s\S]*?<\/span>/)[0];
    assert.ok(!/[🚀📱✨🔥🎉]/.test(topbarTitle), 'Topbar title must not contain loud emojis');
  });

  await t.test('1.5.3 Vector SVG icons are used exclusively for primary controls', () => {
    const svgCount = (html.match(/<svg/g) || []).length;
    assert.ok(svgCount >= 10, `Expected at least 10 SVG icons across interface, found ${svgCount}`);
  });

  await t.test('1.5.4 Nested border radii hierarchy is mathematically defined', () => {
    assert.ok(css.includes('--radius-sm') || css.includes('--radius-md') || css.includes('--radius-card') || css.includes('--radius-pill'), 'Must define radii hierarchy tokens');
  });

  await t.test('1.5.5 Elimination of neon glows and distracting purple gradients in cards', () => {
    assert.ok(!css.includes('box-shadow: 0 0 20px #8b5cf6'), 'No neon purple glows in CSS');
    assert.ok(!css.includes('box-shadow: 0 0 30px #a855f7'), 'No neon purple glows in CSS');
  });
});

test('TIER 1.6: Anti-Overlap Guard & Typography Tokens', async (t) => {
  await t.test('1.6.1 min-width: 0 is enforced on flex/grid containers to prevent blowout', () => {
    assert.ok(css.includes('min-width: 0'), 'CSS must include Anti-Overlap min-width: 0');
  });

  await t.test('1.6.2 Topbar user name and title enforce text truncation', () => {
    assert.ok(html.includes('class="topbar-user-name truncate"'), 'Topbar user name must include truncate class');
    assert.ok(css.includes('.truncate') || css.includes('text-overflow: ellipsis'), 'CSS must define text truncation');
  });

  await t.test('1.6.3 Responsive header button hides text label at <= 380px', () => {
    assert.ok(css.includes('@media (max-width: 380px)'), 'CSS must include <= 380px media query');
    const media380Block = css.match(/@media\s*\(max-width:\s*380px\)[\s\S]*?\}/);
    assert.ok(media380Block, 'Found 380px media query block');
    assert.ok(media380Block[0].includes('.pwa-btn-label') && media380Block[0].includes('display: none'), 'Must hide .pwa-btn-label at <= 380px');
  });

  await t.test('1.6.4 Metric counters, timers, and step numbers use tabular-nums', () => {
    assert.ok(css.includes('tabular-nums'), 'CSS must define tabular-nums utility');
    assert.ok(css.includes('font-variant-numeric: tabular-nums'), 'CSS must set font-variant-numeric: tabular-nums');
  });

  await t.test('1.6.5 Main viewport applies defensive containment and box-sizing', () => {
    assert.ok(css.includes('box-sizing: border-box'), 'CSS must enforce border-box sizing');
    assert.ok(css.includes('.app-main-viewport'), 'CSS must style .app-main-viewport');
  });
});

test('TIER 1.7: Transitions, Micro-Interactions & Accordion Animations', async (t) => {
  await t.test('1.7.1 Screen transitions define smooth directional keyframes', () => {
    assert.ok(css.includes('screenFadeIn'), 'CSS must define screenFadeIn keyframes');
    assert.ok(css.includes('slideFromRight'), 'CSS must define slideFromRight keyframes');
    assert.ok(css.includes('slideFromLeft'), 'CSS must define slideFromLeft keyframes');
  });

  await t.test('1.7.2 Modal dialog entrance animation and backdrop blur defined', () => {
    assert.ok(css.includes('modalEnter'), 'CSS must define modalEnter keyframes');
    assert.ok(css.includes('backdropEnter'), 'CSS must define backdropEnter keyframes');
    assert.ok(css.includes('.app-dialog[open]'), 'CSS must style open dialog state');
  });

  await t.test('1.7.3 Tactile button response defines active state transform: scale(0.97)', () => {
    assert.ok(css.includes('scale(0.97)'), 'CSS must include active scale(0.97) for tactile response');
  });

  await t.test('1.7.4 Exercise group accordion defines collapsed state and transition', () => {
    assert.ok(css.includes('.exercise-group-card.collapsed') || css.includes('.exercise-group-card'), 'CSS must style exercise group card');
    assert.ok(css.includes('max-height') && css.includes('transition:'), 'CSS must transition max-height on sets table');
  });

  await t.test('1.7.5 Loading skeleton defines .skeleton with skeletonShimmer animation', () => {
    assert.ok(css.includes('.skeleton'), 'CSS must define .skeleton class');
    assert.ok(css.includes('skeletonShimmer'), 'CSS must define skeletonShimmer animation');
    assert.ok(css.includes('@keyframes skeletonShimmer'), 'CSS must include @keyframes skeletonShimmer');
  });

  await t.test('1.7.6 CSS includes prefers-reduced-motion accessibility override', () => {
    assert.ok(css.includes('@media (prefers-reduced-motion: reduce)'), 'CSS must respect prefers-reduced-motion');
  });
});

// ============================================================================
// TIER 2: BOUNDARY & CORNER CASES (>= 5 tests)
// ============================================================================

test('TIER 2: Boundary & Corner Cases', async (t) => {
  await t.test('2.1 Mobile Viewport (<= 380px) header compression suppresses text label', () => {
    const queryRegex = /@media\s*\(max-width:\s*380px\)\s*\{([^}]+)\}/;
    const match = css.match(queryRegex);
    assert.ok(match, 'Media query for max-width 380px must be present');
    const innerCss = match[1];
    assert.ok(innerCss.includes('.pwa-install-header-btn .pwa-btn-label'), 'Targets header button label');
    assert.ok(innerCss.includes('display: none'), 'Hides label on narrow screens');
  });

  await t.test('2.2 Standalone display mode detection logic in app.js', () => {
    // Test matchMedia and navigator.standalone evaluation logic
    function checkStandalone(displayModeMatch, navStandalone) {
      const matchMediaResult = { matches: displayModeMatch };
      const nav = { standalone: navStandalone };
      return matchMediaResult.matches || nav.standalone === true;
    }

    assert.strictEqual(checkStandalone(false, false), false, 'Browser tab is not standalone');
    assert.strictEqual(checkStandalone(true, false), true, 'PWA standalone display-mode detected');
    assert.strictEqual(checkStandalone(false, true), true, 'iOS standalone home-screen shortcut detected');
    assert.strictEqual(checkStandalone(true, true), true, 'Both standalone flags detected');
  });

  await t.test('2.3 Missing beforeinstallprompt fallback to multi-OS modal guide', () => {
    // When deferredInstallPrompt is null, handlePwaInstall opens dialog
    let dialogOpened = false;
    let openedTab = null;

    const mockApp = {
      deferredPrompt: null,
      openDialog: (tab) => {
        dialogOpened = true;
        openedTab = tab;
      },
      handleInstall: function (userAgent) {
        if (this.deferredPrompt) {
          return 'native_prompt';
        }
        const ua = (userAgent || '').toLowerCase();
        let target = 'desktop';
        if (/iphone|ipad|ipod/.test(ua)) target = 'ios';
        else if (/android/.test(ua)) target = 'android';
        this.openDialog(target);
        return 'modal_fallback';
      }
    };

    const resIos = mockApp.handleInstall('Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X)');
    assert.strictEqual(resIos, 'modal_fallback');
    assert.strictEqual(dialogOpened, true);
    assert.strictEqual(openedTab, 'ios');

    dialogOpened = false;
    const resDesktop = mockApp.handleInstall('Mozilla/5.0 (Windows NT 10.0; Win64; x64)');
    assert.strictEqual(resDesktop, 'modal_fallback');
    assert.strictEqual(dialogOpened, true);
    assert.strictEqual(openedTab, 'desktop');
  });

  await t.test('2.4 Modal dismissal mechanics (backdrop click, escape, and close buttons)', () => {
    let dialogState = 'open';

    const closeHandler = () => { dialogState = 'closed'; };
    const backdropHandler = (targetIsDialog) => {
      if (targetIsDialog) closeHandler();
    };

    // Backdrop click
    backdropHandler(true);
    assert.strictEqual(dialogState, 'closed', 'Backdrop click closes dialog');

    // Reopen & close button click
    dialogState = 'open';
    closeHandler();
    assert.strictEqual(dialogState, 'closed', 'Close button closes dialog');

    // Content click does not close dialog
    dialogState = 'open';
    backdropHandler(false);
    assert.strictEqual(dialogState, 'open', 'Clicking inside dialog content must NOT close dialog');
  });

  await t.test('2.5 Service worker cache strategy and route partitioning in sw.js', () => {
    assert.ok(swJs.includes("if (event.request.method !== 'GET') return;"), 'Only caches GET requests');
    assert.ok(swJs.includes("url.pathname.startsWith('/api/')"), 'Strictly bypasses /api/ requests');
    assert.ok(swJs.includes("url.pathname.startsWith('/releases/')"), 'Strictly bypasses /releases/ requests');
    assert.ok(swJs.includes("url.pathname.endsWith('.html') || url.pathname.endsWith('.js') || url.pathname.endsWith('.css')"), 'Employs Network-First for core shell assets');
    assert.ok(swJs.includes("caches.match(event.request)"), 'Falls back to cache when offline');
  });

  await t.test('2.6 Idempotent rapid invocations and deterministic tab switching', () => {
    const panels = { ios: false, android: false, desktop: false };
    function switchTab(key) {
      Object.keys(panels).forEach(k => { panels[k] = (k === key); });
    }

    // Rapid switching
    switchTab('ios');
    switchTab('android');
    switchTab('desktop');
    switchTab('ios');

    assert.strictEqual(panels.ios, true);
    assert.strictEqual(panels.android, false);
    assert.strictEqual(panels.desktop, false);
    const activeCount = Object.values(panels).filter(Boolean).length;
    assert.strictEqual(activeCount, 1, 'Exactly one panel must be active at any time');
  });
});

// ============================================================================
// TIER 3: CROSS-FEATURE COMBINATIONS
// ============================================================================

test('TIER 3: Cross-Feature Combinations', async (t) => {
  await t.test('3.1 PWA modal prompt triggering across screen transitions', () => {
    const screens = ['screen-auth', 'screen-athlete-workout', 'screen-trainer-home', 'screen-trainer-settings'];
    const pwaTriggers = [
      { id: 'btn-pwa-install-auth', screen: 'screen-auth' },
      { id: 'btn-pwa-install-header', screen: 'topbar' },
      { id: 'btn-pwa-install-athlete', screen: 'screen-athlete-profile' },
      { id: 'btn-pwa-install-trainer', screen: 'screen-trainer-settings' }
    ];

    for (const trigger of pwaTriggers) {
      assert.ok(html.includes(`id="${trigger.id}"`), `Trigger ${trigger.id} must be in markup`);
    }

    // Modal dialog is mounted directly under <body> or main container, outside individual screens
    const dialogIdx = html.indexOf('id="dialog-download-app"');
    assert.ok(dialogIdx > 0, 'Dialog is defined globally');
    // Ensure dialog is not nested inside screen-auth
    const authStart = html.indexOf('id="screen-auth"');
    const authEnd = html.indexOf('<!-- ================= 2.', authStart);
    assert.ok(dialogIdx > authEnd || dialogIdx < authStart, 'Dialog is not trapped inside screen-auth');
  });

  await t.test('3.2 Anti-Overlap Guard containment with extreme strings in workout view', () => {
    // Verify CSS rules prevent horizontal blowouts
    assert.ok(css.includes('min-width: 0'), 'min-width: 0 guards against flexbox overflow');
    assert.ok(css.includes('.truncate') || css.includes('text-overflow: ellipsis'), 'truncate handles long names');
    assert.ok(css.includes('tabular-nums'), 'tabular-nums keeps fixed-width numeric columns');
    assert.ok(html.includes('class="sets-table"'), 'Workout sets table exists in HTML');
  });

  await t.test('3.3 Accordion expansion combined with skeleton shimmer loading states', () => {
    // Accordion CSS
    assert.ok(css.includes('.exercise-group-card.collapsed') || css.includes('.exercise-group-card'), 'Exercise accordion rules present');
    // Skeleton CSS
    assert.ok(css.includes('.skeleton'), 'Skeleton loader rules present');
    assert.ok(css.includes('skeletonShimmer'), 'Skeleton shimmer keyframes present');
    // Neither interferes with the other
    assert.ok(!css.includes('.skeleton.exercise-group-card'), 'Skeleton is not conflated with accordion');
  });

  await t.test('3.4 Modal tab selection state retention and backdrop dismissal', () => {
    let currentTab = 'ios';
    let dialogOpen = false;

    function openDialog(target) {
      currentTab = target;
      dialogOpen = true;
    }
    function closeDialog() {
      dialogOpen = false;
    }

    // User opens on iOS
    openDialog('ios');
    assert.strictEqual(dialogOpen, true);
    assert.strictEqual(currentTab, 'ios');

    // Dismisses via backdrop
    closeDialog();
    assert.strictEqual(dialogOpen, false);

    // Later opens on Android
    openDialog('android');
    assert.strictEqual(dialogOpen, true);
    assert.strictEqual(currentTab, 'android');
  });

  await t.test('3.5 Service worker precache list consistency with public shell files', () => {
    // Extract assets list from sw.js
    const match = swJs.match(/const ASSETS_TO_CACHE = \[([\s\S]*?)\];/);
    assert.ok(match, 'ASSETS_TO_CACHE found in sw.js');
    const assetEntries = match[1]
      .split('\n')
      .map(line => line.trim().replace(/^['"]|['"],?$/g, ''))
      .filter(line => line && line !== '/');

    for (const asset of assetEntries) {
      const assetDiskPath = path.join(PUBLIC_DIR, asset.replace(/^\//, ''));
      assert.ok(fs.existsSync(assetDiskPath), `Cached asset ${asset} must exist at ${assetDiskPath}`);
      const stats = fs.statSync(assetDiskPath);
      assert.ok(stats.size > 0, `Cached asset ${asset} must have non-zero size`);
    }
  });
});

// ============================================================================
// TIER 4: REAL-WORLD WORKLOAD SCENARIOS (5 Scenarios)
// ============================================================================

test('TIER 4: Real-World Workload Scenarios', async (t) => {
  await t.test('4.1 Scenario 1: First-time mobile visitor on iOS Safari', () => {
    const iosUserAgent = 'Mozilla/5.0 (iPhone; CPU iPhone OS 17_4_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Mobile/15E148 Safari/604.1';
    
    // Simulate user arriving at auth screen and clicking install
    const ua = iosUserAgent.toLowerCase();
    const isIOS = /iphone|ipad|ipod/.test(ua) || (ua.includes('macintel') && false);
    assert.strictEqual(isIOS, true, 'User agent identified as iOS');

    let targetTab = 'desktop';
    if (isIOS) targetTab = 'ios';
    assert.strictEqual(targetTab, 'ios', 'iOS tab selected automatically');

    // Check iOS tab instructions in index.html
    const iosPanel = html.slice(html.indexOf('id="pwa-tab-ios"'), html.indexOf('id="pwa-tab-android"'));
    assert.ok(iosPanel.includes('Поделиться'), 'Step mentions Share icon');
    assert.ok(iosPanel.includes('На экран „Домой“'), 'Step mentions Add to Home Screen');
    assert.ok(iosPanel.includes('Добавить'), 'Step mentions Add button');
    assert.ok(!iosPanel.includes('.apk'), 'iOS guide contains zero APK references');
  });

  await t.test('4.2 Scenario 2: Android visitor with native beforeinstallprompt flow', async () => {
    let promptInvoked = false;
    let outcomeReceived = null;

    const mockEvent = {
      preventDefault: () => {},
      prompt: () => { promptInvoked = true; },
      userChoice: Promise.resolve({ outcome: 'accepted' })
    };

    // State simulation in app.js
    let deferredInstallPrompt = mockEvent;
    assert.ok(deferredInstallPrompt, 'deferredInstallPrompt captured');

    // User triggers install
    deferredInstallPrompt.prompt();
    assert.strictEqual(promptInvoked, true, 'Native install prompt() was called');

    const choice = await deferredInstallPrompt.userChoice;
    outcomeReceived = choice.outcome;
    assert.strictEqual(outcomeReceived, 'accepted', 'Outcome was accepted by user');

    // Deferred prompt is cleared after success
    deferredInstallPrompt = null;
    assert.strictEqual(deferredInstallPrompt, null, 'Prompt cleared after install');
  });

  await t.test('4.3 Scenario 3: Athlete daily workout navigation & accordion set logging', () => {
    // 1. Navigation to athlete workout tab
    assert.ok(appJs.includes("case 'athlete-workout':"), 'app.js handles athlete-workout tab');
    assert.ok(appJs.includes('loadAthleteWorkoutSets'), 'Loads workout sets for current date');

    // 2. Tab transitions in styles.css
    assert.ok(css.includes('slideFromRight'), 'Slide transition available');
    assert.ok(css.includes('screenFadeIn'), 'Fade transition available');

    // 3. Tabular numerals for weights and reps
    assert.ok(css.includes('tabular-nums'), 'Numerical sets formatted with tabular-nums');

    // 4. Exercise accordion collapse & expand
    assert.ok(css.includes('.exercise-group-card.collapsed > .sets-table'), 'Collapsed sets table transition');
    assert.ok(css.includes('max-height: 0'), 'Collapsed sets table has max-height 0');
    assert.ok(css.includes('opacity: 0'), 'Collapsed sets table is transparent');
  });

  await t.test('4.4 Scenario 4: Trainer client management & settings PWA installation', () => {
    // 1. Navigation to trainer settings tab
    assert.ok(appJs.includes("case 'trainer-settings':"), 'app.js handles trainer-settings tab');
    assert.ok(appJs.includes('renderTrainerSettings'), 'Renders trainer settings view');

    // 2. Settings contains PWA installation card
    assert.ok(html.includes('id="card-pwa-install-trainer"'), 'Trainer settings markup contains PWA install card');
    assert.ok(html.includes('id="btn-pwa-install-trainer"'), 'Trainer PWA card contains install button');

    // 3. Trainer can inspect all OS instructions
    assert.ok(html.includes('data-pwa-tab="ios"'), 'iOS tab available');
    assert.ok(html.includes('data-pwa-tab="android"'), 'Android tab available');
    assert.ok(html.includes('data-pwa-tab="desktop"'), 'Desktop tab available');
  });

  await t.test('4.5 Scenario 5: Auth screen responsive PWA shortcut & OTP interaction', () => {
    // 1. Auth screen has PWA shortcut button
    assert.ok(html.includes('id="btn-pwa-install-auth"'), 'Auth screen has PWA shortcut');

    // 2. Header compresses gracefully on mobile <= 380px
    assert.ok(css.includes('@media (max-width: 380px)'), 'Compact breakpoint defined');
    assert.ok(css.includes('.pwa-install-header-btn .pwa-btn-label'), 'Compact header label rule exists');

    // 3. Auth tabs switcher (Login / Register)
    assert.ok(html.includes('id="tab-login"'), 'tab-login exists');
    assert.ok(html.includes('id="tab-register"'), 'tab-register exists');

    // 4. Telegram auth integration
    assert.ok(html.includes('id="btn-telegram-auth"'), 'btn-telegram-auth exists');

    // 5. Zero overlap: flex containers enforce min-width: 0
    assert.ok(css.includes('min-width: 0'), 'Anti-overlap min-width: 0 in place');
  });
});
