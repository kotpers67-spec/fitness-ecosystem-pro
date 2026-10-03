# Task Assignment: Security Worker (Milestone 3)

**Working Directory**: F:\Projects\fitness-ecosystem-pro\.agents\teamwork\worker_security_1
**Original Request**: F:\Projects\fitness-ecosystem-pro\.agents\teamwork\ORIGINAL_REQUEST.md
**Project Plan**: F:\Projects\fitness-ecosystem-pro\PROJECT.md
**Security Explorer Report**: F:\Projects\fitness-ecosystem-pro\.agents\teamwork\explorer_survey_security_1\handoff.md
**Web Worker Report**: F:\Projects\fitness-ecosystem-pro\.agents\teamwork\worker_web_1\handoff.md

## Exclusive File Ownership
You exclusively own and may edit:
- `web/tests/security.test.js`
- `web/package.json`

Do NOT touch `trainer-app/**`, `athlete-app/**`, or `web/src/public/**`.

## Mandatory Integrity Warning
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

## Objective & Implementation Steps
1. Place the complete security penetration test suite into `web/tests/security.test.js`:
   - Take the tested 26-test suite from `F:\Projects\fitness-ecosystem-pro\.agents\teamwork\explorer_survey_security_1\proposed_security.test.js`.
   - Ensure it tests:
     a) SQL Injection on login, register, workout sets, pairing, and query params.
     b) XSS tag escaping on inputs and security headers.
     c) Rate Limiting brute-force protection on /api/login and /api/register (verifying 429).
     d) Role Isolation (athlete cannot access /api/trainer/clients, /api/trainer/pair, etc.).
     e) Path Traversal protection.
     f) Token authentication and tampering rejection.
2. Ensure package.json `"test:security"` and `"test"` commands correctly point to `node --test tests/security.test.js`.
3. Run the security test suite against `web/tests/security.test.js` using the local Node runtime:
   `& "C:\Users\kotpe\AppData\Local\OpenAI\Codex\runtimes\cua_node\45309f9050f7314b\bin\node.exe" --test F:\Projects\fitness-ecosystem-pro\web\tests\security.test.js`
   and verify 100% PASS with 0 failures (0 vulnerabilities).

Write your full report to `F:\Projects\fitness-ecosystem-pro\.agents\teamwork\worker_security_1\handoff.md`.
Report back when complete.

## 2026-10-03T19:31:21Z
You are the Security Worker for Milestone 3.
Working Directory: F:\Projects\fitness-ecosystem-pro\.agents\teamwork\worker_security_1
Original Request: F:\Projects\fitness-ecosystem-pro\.agents\teamwork\ORIGINAL_REQUEST.md
Project Plan: F:\Projects\fitness-ecosystem-pro\PROJECT.md
Dispatch Instructions: F:\Projects\fitness-ecosystem-pro\.agents\teamwork\worker_security_1\DISPATCH.md
Security Explorer Report: F:\Projects\fitness-ecosystem-pro\.agents\teamwork\explorer_survey_security_1\handoff.md
Web Worker Report: F:\Projects\fitness-ecosystem-pro\.agents\teamwork\worker_web_1\handoff.md

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

Place the full, comprehensive security test suite into F:\Projects\fitness-ecosystem-pro\web\tests\security.test.js.
It must cover:
1. SQL Injection on all endpoints
2. XSS tag escaping & security headers
3. Rate Limiting on /api/login and /api/register
4. Role Isolation (athlete cannot access trainer endpoints)
5. Token tampering rejection
6. Path traversal rejection

Run the security test suite against web/tests/security.test.js using:
& "C:\Users\kotpe\AppData\Local\OpenAI\Codex\runtimes\cua_node\45309f9050f7314b\bin\node.exe" --test F:\Projects\fitness-ecosystem-pro\web\tests\security.test.js

Ensure 100% PASS with 0 vulnerabilities.
Write your full report to F:\Projects\fitness-ecosystem-pro\.agents\teamwork\worker_security_1\handoff.md and report back when finished.
