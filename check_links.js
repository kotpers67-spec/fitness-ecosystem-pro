const fs = require('fs');
const h = fs.readFileSync('F:/Projects/fitness-ecosystem-pro/web/src/public/index.html', 'utf8');
const links = Array.from(h.matchAll(/href="([^"]*v2\.0\.0[^"]*\.apk)"/g)).map(m => m[1]);
console.log('APK download links in HTML:');
links.forEach(l => console.log(' ', l));

// Also check the server.js fallback tag
const s = fs.readFileSync('F:/Projects/fitness-ecosystem-pro/web/src/server.js', 'utf8');
const fallbackMatch = s.match(/tag = match \? match\[0\] : '([^']+)'/);
console.log('Server fallback tag:', fallbackMatch ? fallbackMatch[1] : 'NOT FOUND');
