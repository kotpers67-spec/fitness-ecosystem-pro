# DISPATCH — explorer_survey_web_6

You are the Web & Bot Survey Explorer.
Your working directory is: F:\Projects\fitness-ecosystem-pro\.agents\teamwork\explorer_survey_web_6
Original request path: F:\Projects\fitness-ecosystem-pro\.agents\teamwork\ORIGINAL_REQUEST.md (read section under timestamp 2026-10-04T20:25:27Z)
Target codebase: F:\Projects\fitness-ecosystem-pro\web

Mission:
Survey the Web backend and Telegram bot codebase to identify current state and exact changes required for:
1. Stateless Session Persistence (R1):
   - How sessions are currently stored and validated across server restarts/cold boots.
   - HMAC-SHA256 signing for session tokens with automatic re-hydration from database.
   - Database schema and session tables / records.
2. Direct PIN Authentication (R1):
   - Current `/api/auth/telegram/verify-otp` implementation.
   - Acceptance of `{ code: "123456" }` without requiring username.
   - Matching active OTPs and paired user PINs.
3. User profile data synchronization across Web, Bot, and Mobile:
   - 6-digit pairing PIN, photo, full name, phone, restrictions.
   - Endpoints used by mobile apps and bot.
4. Existing test suite:
   - Run `npm test` in `web/` to document baseline test pass/fail state.
   - Relevant test files for auth, sessions, OTP, PIN.

Output:
Write a comprehensive report to F:\Projects\fitness-ecosystem-pro\.agents\teamwork\explorer_survey_web_6\survey_report.md
Include: Current implementation files, gaps against requirements R1, exact proposed file edits/additions, baseline test results, and interface contracts.
Send completion message when done.

## 2026-10-04T20:29:19Z
You are the Web & Bot Survey Explorer.
Your working directory is F:\Projects\fitness-ecosystem-pro\.agents\teamwork\explorer_survey_web_6.
Read your dispatch instructions in F:\Projects\fitness-ecosystem-pro\.agents\teamwork\explorer_survey_web_6\DISPATCH.md.
Read the user request in F:\Projects\fitness-ecosystem-pro\.agents\teamwork\ORIGINAL_REQUEST.md (timestamp 2026-10-04T20:25:27Z).
Investigate F:\Projects\fitness-ecosystem-pro\web.
Focus on:
1. Stateless session persistence (HMAC-SHA256 signed session tokens, DB rehydration across cold boots).
2. Direct PIN verification endpoint /api/auth/telegram/verify-otp with code alone { code: "123456" }.
3. Synchronization of 6-digit pairing PIN, photo, full name, phone, restrictions across web and bot.
4. Current baseline test results (run `npm test` in `web/` and report exact output).
Produce F:\Projects\fitness-ecosystem-pro\.agents\teamwork\explorer_survey_web_6\survey_report.md and handoff.md.
Send message back to parent when complete.
