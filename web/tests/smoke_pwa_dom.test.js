const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

test('PWA Manifest & DOM Smoke Validation Suite', async (t) => {
  const publicDir = path.join(__dirname, '../src/public');
  const manifestPath = path.join(publicDir, 'manifest.json');
  const htmlPath = path.join(publicDir, 'index.html');
  const swPath = path.join(publicDir, 'sw.js');
  const cssPath = path.join(publicDir, 'styles.css');
  const appJsPath = path.join(publicDir, 'app.js');

  await t.test('1. manifest.json is valid, standalone and has icons', () => {
    assert.ok(fs.existsSync(manifestPath), 'manifest.json must exist');
    const manifest = JSON.parse(fs.readFileSync(manifestPath, 'utf8'));
    assert.strictEqual(manifest.display, 'standalone');
    assert.ok(manifest.name, 'manifest must have name');
    assert.ok(manifest.short_name, 'manifest must have short_name');
    assert.ok(Array.isArray(manifest.icons) && manifest.icons.length >= 2, 'manifest must have app icons');
  });

  await t.test('2. sw.js is present and contains offline cache event listeners', () => {
    assert.ok(fs.existsSync(swPath), 'sw.js must exist');
    const swContent = fs.readFileSync(swPath, 'utf8');
    assert.ok(swContent.includes('self.addEventListener(\'install\''), 'sw.js must handle install');
    assert.ok(swContent.includes('self.addEventListener(\'fetch\''), 'sw.js must handle fetch');
  });

  await t.test('3. index.html contains PWA buttons, dialog and zero APK links', () => {
    assert.ok(fs.existsSync(htmlPath), 'index.html must exist');
    const html = fs.readFileSync(htmlPath, 'utf8');

    // Header, Auth, and Dialog buttons
    assert.ok(html.includes('id="btn-pwa-install-header"'), 'Header install button must exist');
    assert.ok(html.includes('id="btn-pwa-install-auth"'), 'Auth screen install button must exist');
    assert.ok(html.includes('id="dialog-download-app"'), 'PWA installation dialog must exist');
    assert.ok(html.includes('id="card-pwa-install-athlete"'), 'Athlete PWA card must exist');
    assert.ok(html.includes('id="card-pwa-install-trainer"'), 'Trainer PWA card must exist');

    // Verify ZERO .apk download links inside the HTML
    assert.ok(!html.includes('.apk"'), 'HTML must not contain any .apk download links');
  });

  await t.test('4. styles.css enforces Anti-Overlap Guard and Motion Animations', () => {
    assert.ok(fs.existsSync(cssPath), 'styles.css must exist');
    const css = fs.readFileSync(cssPath, 'utf8');

    assert.ok(css.includes('min-width: 0'), 'CSS must include Anti-Overlap min-width: 0');
    assert.ok(css.includes('tabular-nums'), 'CSS must include tabular-nums');
    assert.ok(css.includes('screenFadeIn') || css.includes('slideFromRight'), 'CSS must include smooth screen transitions');
    assert.ok(css.includes('prefers-reduced-motion'), 'CSS must respect prefers-reduced-motion');
  });

  await t.test('5. app.js binds beforeinstallprompt and PWA install handlers', () => {
    assert.ok(fs.existsSync(appJsPath), 'app.js must exist');
    const js = fs.readFileSync(appJsPath, 'utf8');

    assert.ok(js.includes('beforeinstallprompt'), 'app.js must listen for beforeinstallprompt');
    assert.ok(js.includes('handlePwaInstall'), 'app.js must provide handlePwaInstall');
    assert.ok(js.includes('isStandalonePwa'), 'app.js must check standalone mode');
  });
});
