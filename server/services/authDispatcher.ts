/**
 * Multi-Scheme Authentication Dispatcher Service (TypeScript)
 * Server-side authentication handling for basic, token, and anonymous schemes.
 */

import crypto from 'node:crypto';

export type AuthScheme = 'basic' | 'token' | 'anonymous' | 'anon';

export interface AuthDispatchParams {
  scheme?: AuthScheme;
  username?: string;
  password?: string;
  secret?: string; // username:password or Base64
  token?: string;
  desiredName?: string;
  persist?: boolean;
  role?: 'athlete' | 'trainer';
}

export interface AuthUser {
  id: number;
  username: string;
  role: 'athlete' | 'trainer';
  fullName: string;
  phone?: string;
  avatarBase64?: string;
  pairingCode?: string;
  pairingCodeCreatedAt?: number;
  clientUuid?: string;
  restrictions?: string;
  coachName?: string;
  coachPhone?: string;
  isPrivate?: boolean;
  telegramId?: string;
  telegramUsername?: string;
  twoFactorEnabled?: boolean;
  lastSeenAt?: number;
  isGuest?: boolean;
}

export interface AuthDispatchResult {
  status: number;
  token?: string;
  persist?: boolean;
  data: {
    success?: boolean;
    scheme?: string;
    token?: string;
    user?: AuthUser;
    require2FA?: boolean;
    userId?: number;
    error?: string;
    message?: string;
    isAnonymous?: boolean;
    suggestRegister?: boolean;
    suggestTelegram?: boolean;
    userNotFound?: boolean;
    invalidPassword?: boolean;
    expiresInSeconds?: number;
    debugCode?: string;
  };
}

export const RU_TO_EN_MAP: Record<string, string> = {
  'й': 'q', 'ц': 'w', 'у': 'e', 'к': 'r', 'е': 't', 'н': 'y', 'г': 'u', 'ш': 'i', 'щ': 'o', 'з': 'p', 'х': '[', 'ъ': ']',
  'ф': 'a', 'ы': 's', 'в': 'd', 'а': 'f', 'п': 'g', 'р': 'h', 'о': 'j', 'л': 'k', 'д': 'l', 'ж': ';', 'э': '\'',
  'я': 'z', 'ч': 'x', 'с': 'c', 'м': 'v', 'и': 'b', 'т': 'n', 'ь': 'm', 'б': ',', 'ю': '.',
  'Й': 'Q', 'Ц': 'W', 'У': 'E', 'К': 'R', 'Е': 'T', 'Н': 'Y', 'Г': 'U', 'Ш': 'I', 'Щ': 'O', 'З': 'P', 'Х': '{', 'Ъ': '}',
  'Ф': 'A', 'Ы': 'S', 'В': 'D', 'А': 'F', 'П': 'G', 'Р': 'H', 'О': 'J', 'Л': 'K', 'Д': 'L', 'Ж': ':', 'Э': '"',
  'Я': 'Z', 'Ч': 'X', 'С': 'C', 'М': 'V', 'И': 'B', 'Т': 'N', 'Ь': 'M', 'Б': '<', 'Ю': '>',
  'ё': '`', 'Ё': '~'
};

export function ruToEn(str: string): string {
  if (typeof str !== 'string') return '';
  return str.split('').map(ch => RU_TO_EN_MAP[ch] || ch).join('');
}

export function validateLoginPolicy(username?: string): { valid: boolean; sanitized?: string; converted?: string; error?: string } {
  if (!username || typeof username !== 'string') {
    return { valid: false, error: 'Введите логин и пароль' };
  }
  const clean = username.trim();
  if (!clean) {
    return { valid: false, error: 'Введите логин и пароль' };
  }
  if (/('|"|;|--|\/\*|\*\/|\b(UNION|SELECT|INSERT|UPDATE|DELETE|DROP|ALTER|CREATE|EXEC|EXECUTE|TRUNCATE|DECLARE)\b)/i.test(clean)) {
    return { valid: false, error: 'Некорректный логин' };
  }
  if (clean.length < 3 || clean.length > 30) {
    return { valid: false, error: 'Логин должен содержать от 3 до 30 символов' };
  }
  const converted = ruToEn(clean);
  return { valid: true, sanitized: clean, converted };
}

export function validatePasswordPolicy(password?: string): { valid: boolean; error?: string } {
  if (!password || typeof password !== 'string') {
    return { valid: false, error: 'Введите логин и пароль' };
  }
  if (password.length < 6) {
    return { valid: false, error: 'Пароль должен содержать минимум 6 символов' };
  }
  return { valid: true };
}

export function parseSecret(secret?: string): { username: string; password: string } | null {
  if (!secret || typeof secret !== 'string') return null;
  let raw = secret.trim();
  if (!raw.includes(':')) {
    try {
      const decoded = Buffer.from(raw, 'base64').toString('utf8');
      if (decoded.includes(':')) {
        raw = decoded;
      }
    } catch (_) {}
  }
  const colonIndex = raw.indexOf(':');
  if (colonIndex !== -1) {
    return {
      username: raw.slice(0, colonIndex).trim(),
      password: raw.slice(colonIndex + 1)
    };
  }
  return null;
}

export function setAuthCookie(res: any, token: string, persist = false): void {
  if (!res || typeof res.setHeader !== 'function' || !token) return;
  let cookieHeader = `fit_token=${token}; Path=/; HttpOnly; SameSite=Lax`;
  if (persist) {
    const THIRTY_DAYS_SECONDS = 30 * 24 * 60 * 60;
    const expiresDate = new Date(Date.now() + THIRTY_DAYS_SECONDS * 1000).toUTCString();
    cookieHeader += `; Max-Age=${THIRTY_DAYS_SECONDS}; Expires=${expiresDate}`;
  }
  res.setHeader('Set-Cookie', cookieHeader);
}

export function clearAuthCookie(res: any): void {
  if (!res || typeof res.setHeader !== 'function') return;
  res.setHeader('Set-Cookie', 'fit_token=; Path=/; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT; HttpOnly; SameSite=Lax');
}
