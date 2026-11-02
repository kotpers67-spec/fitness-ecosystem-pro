/**
 * Authentication Routes Definition (TypeScript)
 * Endpoints:
 * - POST /api/auth (Multi-scheme Dispatcher)
 * - POST /api/auth/login, POST /api/login
 * - POST /api/auth/token
 * - POST /api/auth/guest-login
 * - GET /api/auth/me, GET /api/me
 * - POST /api/auth/logout, POST /api/logout
 * - POST /api/auth/register, POST /api/register
 */

import { AuthDispatchParams, AuthDispatchResult, setAuthCookie, clearAuthCookie } from '../services/authDispatcher';

export interface AuthRouteContext {
  db: any;
  cloudSyncService?: any;
  tgBotInstance?: any;
  isTrainerApprovalRequired?: () => boolean;
  send2FAOtp?: (bot: any, tgId: string, otp: string) => Promise<any>;
  generateSecureOtpWithRetry?: (store: any) => string;
  telegramOtpStore?: any;
}

export interface RouteHandlerOptions {
  req: any;
  res: any;
  pathname: string;
  body: AuthDispatchParams;
  context: AuthRouteContext;
  sendJson: (res: any, status: number, data: any) => void;
  sendError: (res: any, status: number, message: string) => void;
  dispatchAuth: (params: AuthDispatchParams, context: AuthRouteContext) => Promise<AuthDispatchResult>;
}

export async function handleAuthRoute({
  req,
  res,
  pathname,
  body,
  context,
  sendJson,
  dispatchAuth
}: RouteHandlerOptions): Promise<boolean> {
  // 1. Multi-Scheme Dispatcher: POST /api/auth or POST /api/auth/login or POST /api/login
  if ((pathname === '/api/auth' || pathname === '/api/auth/login' || pathname === '/api/login') && req.method === 'POST') {
    const scheme = body.scheme || 'basic';
    const result = await dispatchAuth({ ...body, scheme }, context);
    if (result.token) {
      setAuthCookie(res, result.token, result.persist);
    }
    sendJson(res, result.status, result.data);
    return true;
  }

  // 2. Token Scheme: POST /api/auth/token
  if (pathname === '/api/auth/token' && req.method === 'POST') {
    let token = body.token;
    if (!token && req.headers['authorization']) {
      token = req.headers['authorization'].replace(/^Bearer\s+/i, '').trim();
    }
    if (!token && req.headers['cookie']) {
      const match = req.headers['cookie'].match(/(?:^|;\s*)fit_token=([^;]+)/);
      if (match) token = match[1];
    }
    const result = await dispatchAuth({ scheme: 'token', token, persist: body.persist }, context);
    if (result.token) {
      setAuthCookie(res, result.token, result.persist);
    }
    sendJson(res, result.status, result.data);
    return true;
  }

  // 3. Anonymous Scheme: POST /api/auth/guest-login
  if (pathname === '/api/auth/guest-login' && req.method === 'POST') {
    const result = await dispatchAuth({ scheme: 'anonymous', desiredName: body.desiredName, persist: body.persist }, context);
    if (result.token) {
      setAuthCookie(res, result.token, result.persist);
    }
    sendJson(res, result.status, result.data);
    return true;
  }

  // 4. Logout: POST /api/auth/logout or POST /api/logout
  if ((pathname === '/api/auth/logout' || pathname === '/api/logout') && req.method === 'POST') {
    let token = (req.headers['authorization'] || '').replace(/^Bearer\s+/i, '').trim();
    if (!token && req.headers['cookie']) {
      const match = req.headers['cookie'].match(/(?:^|;\s*)fit_token=([^;]+)/);
      if (match) token = match[1];
    }
    if (token) context.db.deleteAuthToken(token);
    clearAuthCookie(res);
    sendJson(res, 200, { success: true });
    return true;
  }

  return false;
}
