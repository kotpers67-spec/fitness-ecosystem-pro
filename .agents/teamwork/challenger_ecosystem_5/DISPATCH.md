# Dispatch — challenger_ecosystem_5

**Recipient**: `challenger_ecosystem_5` (teamwork_preview_challenger)
**Working Directory**: `F:\Projects\fitness-ecosystem-pro\.agents\teamwork\challenger_ecosystem_5`
**Targets**: `F:\Projects\fitness-ecosystem-pro\web`, `athlete-app`, `trainer-app`
**Authoritative Request**: `F:\Projects\fitness-ecosystem-pro\.agents\teamwork\ORIGINAL_REQUEST.md`
**Project Reference**: `F:\Projects\fitness-ecosystem-pro\PROJECT.md`

## Mission
Perform empirical adversarial stress testing across the ecosystem:
1. **PIN Expiration & Replay Attack**:
   - Verify that generating a 6-digit PIN and attempting to pair after 301 seconds yields HTTP 400 with strict error message.
   - Verify that attempting to reuse an already paired/consumed PIN immediately yields HTTP 400.
2. **2FA Bypass Prevention**:
   - Attempt to bypass 2FA by directly requesting authenticated session routes or skipping OTP step; verify strict rejection (HTTP 401/403).
   - Test brute-forcing 2FA OTP codes; verify rate limiter triggers HTTP 429 lockout.
3. **Role & Privilege Escalation (IDOR)**:
   - Athlete attempting trainer-only routes (`/api/trainer/*`, `/api/trainer/assign-workout`, etc.) -> must return HTTP 403.
   - Athlete attempting to mutate another athlete's sets -> must return HTTP 403.
   - Attempting to switch role post-registration (`POST /api/user/role`) -> must return HTTP 403.
4. **Android Avatar CursorWindow Guard**:
   - Verify image processing logic in both apps strictly enforces <= 15 KB (15360 bytes) JPEG on any input resolution.
5. Execute or write an empirical adversarial challenge script in `web/tests/` to validate these vectors live against the server.
6. Deliver structured `report.md` and `handoff.md` with a clear verdict (APPROVE / REQUEST_CHANGES).
7. Send completion message via `send_message`.


## 2026-10-04T16:00:27Z
You are challenger_ecosystem_5. Your working directory is F:\Projects\fitness-ecosystem-pro\.agents\teamwork\challenger_ecosystem_5.
Read F:\Projects\fitness-ecosystem-pro\.agents\teamwork\challenger_ecosystem_5\DISPATCH.md and F:\Projects\fitness-ecosystem-pro\.agents\teamwork\ORIGINAL_REQUEST.md.
Empirically stress-test and challenge the ecosystem:
- 6-digit PIN 5-minute TTL (>300s rejected with 400)
- Single-use consumption (reuse rejected with 400)
- Role isolation & IDOR prevention (athlete accessing trainer routes blocked with 403, set ownership enforcement, role immutability)
- Telegram 2FA OTP bypass prevention & rate limiting lockout
- Avatar CursorWindow guard (<= 15KB JPEG)
Run live verification scripts against the server and codebase.
Write report.md and handoff.md in your working directory. Report your verdict (APPROVE / REQUEST_CHANGES) via send_message to parent.
