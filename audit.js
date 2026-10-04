const fs = require('fs');
const path = require('path');

const BASE = 'F:/Projects/fitness-ecosystem-pro';

// Find deploy URL in all JS files
const webSrcFiles = ['web/src/server.js', 'web/src/bot.js'];
for (const f of webSrcFiles) {
  const content = fs.readFileSync(path.join(BASE, f), 'utf8');
  const matches = content.match(/https?:\/\/[^\s"'`]+onrender[^\s"'`]*/gi) || [];
  const envMatches = content.match(/BASE_URL|SERVER_URL|APP_URL|WEBHOOK_URL/gi) || [];
  if (matches.length || envMatches.length) {
    console.log(f + ':');
    matches.forEach(m => console.log('  RENDER:', m));
    envMatches.slice(0, 5).forEach(m => console.log('  ENV:', m));
  }
}

// Check .env file
try {
  const env = fs.readFileSync(path.join(BASE, 'web/.env'), 'utf8');
  console.log('\n.env file:', env.substring(0, 500));
} catch(e) {
  console.log('\nNo .env file found');
}

// Find RPE in web files
const rpeFiles = [
  'web/src/public/app.js',
  'web/src/public/index.html', 
  'web/src/server.js',
  'web/src/db.js'
];
console.log('\n=== RPE occurrences ===');
for (const f of rpeFiles) {
  const content = fs.readFileSync(path.join(BASE, f), 'utf8');
  const lines = content.split('\n');
  lines.forEach((line, i) => {
    if (/rpe/i.test(line)) {
      console.log(`${f}:${i+1}: ${line.trim()}`);
    }
  });
}
