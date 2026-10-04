const fs = require('fs');

function replaceInFile(filePath, pairs) {
  let content = fs.readFileSync(filePath, 'utf8');
  for (const [from, to] of pairs) {
    content = content.split(from).join(to);
  }
  fs.writeFileSync(filePath, content, 'utf8');
  console.log('Updated: ' + filePath);
}

const WEB = 'F:/Projects/fitness-ecosystem-pro/web';

replaceInFile(WEB + '/src/public/index.html', [
  ['v1.0.10', 'v2.0.0'],
  ['v1.0.9',  'v2.0.0'],
  ['athlete-pro-v1.0.10', 'athlete-pro-v2.0.0'],
  ['trainer-pro-v1.0.10', 'trainer-pro-v2.0.0'],
  ['(v1.0.10)', '(v2.0.0)'],
]);

replaceInFile(WEB + '/src/server.js', [
  ['v1.0.10', 'v2.0.0'],
  ['v1.0.9',  'v2.0.0'],
  ['athlete-pro-v1.0.10', 'athlete-pro-v2.0.0'],
  ['trainer-pro-v1.0.10', 'trainer-pro-v2.0.0'],
]);

replaceInFile(WEB + '/package.json', [
  ['"version": "1.0.0"', '"version": "2.0.0"'],
]);

replaceInFile(WEB + '/src/bot.js', [
  ['v1.0.10', 'v2.0.0'],
  ['v1.0.9',  'v2.0.0'],
]);

console.log('All web/bot version strings updated to 2.0.0');
