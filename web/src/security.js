/**
 * Security & Defense Module
 * Implements OWASP ASVS Level 2 defenses:
 * - Anti-SQL Injection (Strict Type & Pattern Enforcement + Parameterized Sanitization)
 * - Anti-XSS (Input and Output Sanitization, Safe Encoding)
 * - Timing-Safe Cryptographic Auth Comparison
 * - Token & Password Hardening (scrypt-based key derivation)
 * - Rate Limiting & Brute Force Shield
 */

const crypto = require('node:crypto');

// 1. Output Encoding / Anti-XSS
function escapeHtml(str) {
  if (typeof str !== 'string') return '';
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#x27;')
    .replace(/\//g, '&#x2F;');
}

// 2. Anti-SQL Injection Defense: Strict Input Validation Patterns
const VALIDATION_PATTERNS = {
  username: /^[\p{L}\p{N}_\-\.]{3,30}$/u,
  pairingCode: /^\d{6}$/,
  emailOrPhone: /^[a-zA-Z0-9_\-\.\+@]{3,60}$/,
  safeString: /^[\p{L}\p{N}\s\-_,\.!?()#+]{1,120}$/u,
  number: /^-?\d+(\.\d+)?$/,
  uuid: /^[a-fA-F0-9\-]{8,40}$/
};

// Check for typical SQL injection vectors in unparsed strings
const SQLI_REGEX = /('|"|;|--|\/\*|\*\/|\b(UNION|SELECT|INSERT|UPDATE|DELETE|DROP|ALTER|CREATE|EXEC|EXECUTE|TRUNCATE|DECLARE)\b)/i;

function hasSqlInjectionVector(input) {
  if (typeof input !== 'string') return false;
  return SQLI_REGEX.test(input);
}

// 3. Cryptographic Password Derivation (scrypt)
function hashPassword(password, salt = crypto.randomBytes(16).toString('hex')) {
  const derivedKey = crypto.scryptSync(password, salt, 64);
  return `${salt}:${derivedKey.toString('hex')}`;
}

function verifyPassword(password, storedHash) {
  if (!storedHash || !storedHash.includes(':')) return false;
  const [salt, keyHex] = storedHash.split(':');
  const targetBuffer = Buffer.from(keyHex, 'hex');
  const derivedBuffer = crypto.scryptSync(password, salt, 64);
  
  if (targetBuffer.length !== derivedBuffer.length) return false;
  return crypto.timingSafeEqual(targetBuffer, derivedBuffer);
}

// 4. Cryptographic Secure Token & PIN Generation
function generateToken() {
  return crypto.randomBytes(32).toString('hex');
}

function generateSecurePin() {
  return crypto.randomInt(100000, 1000000).toString();
}

// 5. Sliding-Window Rate Limiter
class RateLimiter {
  constructor(windowMs = 60000, maxRequests = 100) {
    this.windowMs = windowMs;
    this.maxRequests = maxRequests;
    this.requests = new Map();
  }

  isRateLimited(key) {
    const now = Date.now();
    const timestamps = this.requests.get(key) || [];
    const recent = timestamps.filter(t => now - t < this.windowMs);
    
    if (recent.length >= this.maxRequests) {
      this.requests.set(key, recent);
      return true;
    }
    
    recent.push(now);
    this.requests.set(key, recent);
    return false;
  }

  cleanup() {
    const now = Date.now();
    for (const [key, timestamps] of this.requests.entries()) {
      const recent = timestamps.filter(t => now - t < this.windowMs);
      if (recent.length === 0) {
        this.requests.delete(key);
      } else {
        this.requests.set(key, recent);
      }
    }
  }

  reset() {
    this.requests.clear();
  }
}

// 6. Security Response Headers
const SECURITY_HEADERS = {
  'Content-Security-Policy': "default-src 'self' 'unsafe-inline' https:; img-src 'self' data: https:; font-src 'self' https:;",
  'X-Content-Type-Options': 'nosniff',
  'X-Frame-Options': 'DENY',
  'X-XSS-Protection': '1; mode=block',
  'Referrer-Policy': 'strict-origin-when-cross-origin',
  'Strict-Transport-Security': 'max-age=31536000; includeSubDomains'
};

module.exports = {
  escapeHtml,
  VALIDATION_PATTERNS,
  hasSqlInjectionVector,
  hashPassword,
  verifyPassword,
  generateToken,
  generateSecurePin,
  RateLimiter,
  SECURITY_HEADERS
};
