const fs = require('fs');

function replaceInFile(filePath, pairs) {
  let content = fs.readFileSync(filePath, 'utf8');
  for (const [from, to] of pairs) {
    content = content.split(from).join(to);
  }
  fs.writeFileSync(filePath, content, 'utf8');
  console.log('Updated: ' + filePath);
}

const BASE = 'F:/Projects/fitness-ecosystem-pro';

// athlete-app build.gradle.kts
replaceInFile(BASE + '/athlete-app/app/build.gradle.kts', [
  ['versionCode = 10', 'versionCode = 20'],
  ['versionName = "1.0.10"', 'versionName = "2.0.0"'],
  // in case it was already partially updated
  ['versionCode = 20', 'versionCode = 20'],
]);

// trainer-app build.gradle.kts
replaceInFile(BASE + '/trainer-app/app/build.gradle.kts', [
  ['versionCode = 10', 'versionCode = 20'],
  ['versionName = "1.0.10"', 'versionName = "2.0.0"'],
]);

// athlete UpdateService fallback
replaceInFile(BASE + '/athlete-app/app/src/main/java/com/athleteapp/pro/data/update/AthleteUpdateService.kt', [
  ['"1.0.9"', '"2.0.0"'],
  ['"1.0.10"', '"2.0.0"'],
  ['fallbackVersion = "2.0.0"\nval isFallback', 'fallbackVersion = "2.0.0"\nval isFallback'], // idempotent
]);

// trainer UpdateService fallback
replaceInFile(BASE + '/trainer-app/app/src/main/java/com/trainerapp/pro/data/update/UpdateService.kt', [
  ['"1.0.9"', '"2.0.0"'],
  ['"1.0.10"', '"2.0.0"'],
]);

console.log('All mobile version strings updated to 2.0.0');
