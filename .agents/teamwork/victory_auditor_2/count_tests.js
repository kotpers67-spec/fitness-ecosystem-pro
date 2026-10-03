const fs = require('fs');
const path = require('path');

function summarize(dir, label) {
  if (!fs.existsSync(dir)) {
    console.log(`${label}: Directory not found (${dir})`);
    return;
  }
  const files = fs.readdirSync(dir).filter(f => f.endsWith('.xml'));
  let total = 0, failures = 0, errors = 0, skipped = 0;
  for (const f of files) {
    const content = fs.readFileSync(path.join(dir, f), 'utf8');
    const testsMatch = content.match(/tests="(\d+)"/);
    const failMatch = content.match(/failures="(\d+)"/);
    const errMatch = content.match(/errors="(\d+)"/);
    const skipMatch = content.match(/skipped="(\d+)"/);
    if (testsMatch) total += parseInt(testsMatch[1], 10);
    if (failMatch) failures += parseInt(failMatch[1], 10);
    if (errMatch) errors += parseInt(errMatch[1], 10);
    if (skipMatch) skipped += parseInt(skipMatch[1], 10);
  }
  console.log(`${label}: Total=${total}, Failures=${failures}, Errors=${errors}, Skipped=${skipped}`);
}

summarize('F:/Projects/fitness-ecosystem-pro/trainer-app/app/build/test-results/testDebugUnitTest', 'Trainer App');
summarize('F:/Projects/fitness-ecosystem-pro/athlete-app/app/build/test-results/testDebugUnitTest', 'Athlete App');
