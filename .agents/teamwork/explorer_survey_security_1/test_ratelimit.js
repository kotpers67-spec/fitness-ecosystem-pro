const http = require('node:http');
const { server, db } = require('../../../web/src/server');

function makeRequest(port, method, path, headers = {}, body = null) {
  return new Promise((resolve, reject) => {
    const req = http.request({
      hostname: '127.0.0.1',
      port,
      path,
      method,
      headers: {
        'Content-Type': 'application/json',
        ...headers
      }
    }, res => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        try {
          resolve({ status: res.statusCode, body: data ? JSON.parse(data) : null });
        } catch (e) {
          resolve({ status: res.statusCode, rawBody: data });
        }
      });
    });
    req.on('error', reject);
    if (body) req.write(typeof body === 'string' ? body : JSON.stringify(body));
    req.end();
  });
}

async function run() {
  server.listen(0, async () => {
    const port = server.address().port;
    console.log(`Rate limit test server running on port ${port}`);

    let hit429 = false;
    let attempt429 = -1;

    for (let i = 1; i <= 20; i++) {
      const res = await makeRequest(port, 'POST', '/api/login', {}, {
        username: 'nonexistent_user',
        password: 'wrong_password'
      });
      if (res.status === 429) {
        hit429 = true;
        attempt429 = i;
        console.log(`Request #${i} returned 429 Too Many Requests:`, res.body);
        break;
      }
    }

    console.log('Rate limiter triggered successfully:', hit429, 'at attempt #', attempt429);

    server.close();
    db.close();
    process.exit(0);
  });
}

run();
