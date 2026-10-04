const fs = require('fs');
const BASE = 'F:/Projects/fitness-ecosystem-pro';

// ---- app.js ----
let appJs = fs.readFileSync(BASE + '/web/src/public/app.js', 'utf8');

// Remove athleteInputRpe reference
appJs = appJs.replace(/\s*athleteInputRpe:.*\n/g, '\n');
appJs = appJs.replace(/\s*trainerInputRpe:.*\n/g, '\n');

// Remove RPE display from set cards
appJs = appJs.replace(/\s*<div[^>]*>\s*\$\{[^}]*\}\s*·\s*RPE\s*\$\{[^}]*\}\s*<\/div>/g, '');

// Remove rpe variable reads
appJs = appJs.replace(/\s*const rpe = parseFloat\([^)]+\)[^;]*;\n/g, '\n');
appJs = appJs.replace(/\s*rpe[,\n]/g, (m) => m.replace('rpe,', '').replace('rpe\n', '\n'));

fs.writeFileSync(BASE + '/web/src/public/app.js', appJs, 'utf8');
console.log('app.js RPE removed');

// ---- index.html ----
let html = fs.readFileSync(BASE + '/web/src/public/index.html', 'utf8');

// Remove the RPE label+input block (they appear together)
html = html.replace(/<label[^>]*>\s*RPE\s*<\/label>\s*\n?\s*<input[^>]*id="athlete-input-rpe"[^>]*>/g, '');
html = html.replace(/<label[^>]*>\s*RPE\s*<\/label>\s*\n?\s*<input[^>]*id="trainer-input-rpe"[^>]*>/g, '');

fs.writeFileSync(BASE + '/web/src/public/index.html', html, 'utf8');
console.log('index.html RPE removed');

// ---- server.js ---- 
let server = fs.readFileSync(BASE + '/web/src/server.js', 'utf8');

// Remove rpe from destructuring and usage - replace with hardcoded default silently
server = server.replace(/const \{ date, exerciseName, weightKg, reps, rpe \}/g, 'const { date, exerciseName, weightKg, reps }');
server = server.replace(/const rpeVal = Math\.min\(10, Math\.max\(1, Number\(rpe\) \|\| 8\.0\)\);\n\s*/g, 'const rpeVal = 8.0;\n          ');
server = server.replace(/const rpe = Math\.min\(10, Math\.max\(1, Number\(body\.rpe\) \|\| 8\.0\)\);\n/g, 'const rpe = 8.0;\n');
server = server.replace(/return sendJson\(res, 200, \{ success: true, setId, weightKg: weight, reps, rpe \}\)/g, 'return sendJson(res, 200, { success: true, setId, weightKg: weight, reps })');

fs.writeFileSync(BASE + '/web/src/server.js', server, 'utf8');
console.log('server.js RPE removed from API responses');

// ---- db.js ---- leave RPE column in DB schema (removing it would break existing DBs), just make it internal default
console.log('db.js: RPE column kept internally (schema migration risk), just hidden from UI');

console.log('\nAll RPE UI references removed successfully');
