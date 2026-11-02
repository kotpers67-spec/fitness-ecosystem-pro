/**
 * Authentication Routes & Dispatcher Handlers
 * 100% Backwards Compatible with existing calls:
 * - POST /api/auth (Multi-scheme dispatcher)
 * - POST /api/auth/login, POST /api/login
 * - POST /api/auth/token
 * - POST /api/auth/guest-login
 * - GET /api/auth/me, GET /api/me
 * - POST /api/auth/logout, POST /api/logout
 * - POST /api/auth/register, POST /api/register
 */

const { dispatchAuth, setAuthCookie, clearAuthCookie, normalizeUserProfile } = require('../services/authDispatcher');

function registerAuthRoutes({
  req,
  res,
  pathname,
  body,
  context,
  sendJson,
  sendError
}) {
  // 1. Unified Multi-Scheme Dispatcher: POST /api/auth
  if ((pathname === '/api/auth' || pathname === '/api/auth/login' || pathname === '/api/login') && req.method === 'POST') {
    // If incoming body specifies scheme or secret, or standard login
    const scheme = body.scheme || 'basic';
    const params = {
      ...body,
      scheme
    };

    return dispatchAuth(params, context).then(result => {
      if (result.token) {
        setAuthCookie(res, result.token, result.persist);
      }
      return sendJson(res, result.status, result.data);
    });
  }

  // 2. Token-Based Fast Re-Auth: POST /api/auth/token
  if (pathname === '/api/auth/token' && req.method === 'POST') {
    let token = body.token;
    if (!token && req.headers['authorization']) {
      token = req.headers['authorization'].replace(/^Bearer\s+/i, '').trim();
    }
    if (!token && req.headers['cookie']) {
      const match = req.headers['cookie'].match(/(?:^|;\s*)fit_token=([^;]+)/);
      if (match) token = match[1];
    }

    return dispatchAuth({ scheme: 'token', token, persist: body.persist }, context).then(result => {
      if (result.token) {
        setAuthCookie(res, result.token, result.persist);
      }
      return sendJson(res, result.status, result.data);
    });
  }

  // 3. Anonymous / Guest Auth: POST /api/auth/guest-login
  if (pathname === '/api/auth/guest-login' && req.method === 'POST') {
    return dispatchAuth({ scheme: 'anonymous', desiredName: body.desiredName, persist: body.persist }, context).then(result => {
      if (result.token) {
        setAuthCookie(res, result.token, result.persist);
      }
      return sendJson(res, result.status, result.data);
    });
  }

  // 4. Logout: POST /api/auth/logout & POST /api/logout
  if ((pathname === '/api/auth/logout' || pathname === '/api/logout') && req.method === 'POST') {
    let token = (req.headers['authorization'] || '').replace(/^Bearer\s+/i, '').trim();
    if (!token && req.headers['cookie']) {
      const match = req.headers['cookie'].match(/(?:^|;\s*)fit_token=([^;]+)/);
      if (match) token = match[1];
    }
    if (token) context.db.deleteAuthToken(token);
    clearAuthCookie(res);
    return Promise.resolve(sendJson(res, 200, { success: true }));
  }

  // Not handled here
  return null;
}

module.exports = {
  registerAuthRoutes
};
