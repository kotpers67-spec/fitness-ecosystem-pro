# BRIEFING — 2026-10-03T21:05:00Z

## Mission
Athlete Pro User Directives & Zero-Mocks Refinement: remove mock participants from LeaderboardScreen, replace QrCodeView with shareable link/intent in AthleteSettingsScreen, audit seeds, pass tests, build release APK.

## 🔒 My Identity
- Archetype: worker
- Roles: implementer, qa
- Working directory: F:\Projects\fitness-ecosystem-pro\.agents\teamwork\worker_athlete_2
- Original parent: 193ba9da-86df-408a-8ad4-d32fb01dfd34
- Milestone: Athlete Pro User Directives & Zero-Mocks Refinement

## 🔒 Key Constraints
- Write ownership: athlete-app/** only (plus worker directory and releases/athlete-pro-v1.0.5.apk).
- Zero mocks: strictly remove mock participants ("Максим Громов", "Елена Соколова", "Дмитрий Воронов", "Ольга Морозова") from LeaderboardScreen.
- In competitions, leaderboards display real data only: current athlete (from actual workouts/tonnage) + real cloud athletes. Empty state if none or private.
- Replace QrCodeView in AthleteSettingsScreen with link card, copy to clipboard, and share intent.
- Database audit: ensure no mock/dummy user seeds exist.
- Build release APK and verify with unit tests.

## Current Parent
- Conversation ID: 193ba9da-86df-408a-8ad4-d32fb01dfd34
- Updated: not yet

## Task Summary
- **What to build**: Real leaderboards in LeaderboardScreen, share link & intent in AthleteSettingsScreen, verify DB zero-mocks, build and test release.
- **Success criteria**: Mock names removed, leaderboards only show real user/synced data or empty state, sharing works, testDebugUnitTest and assembleRelease pass, athlete-pro-v1.0.5.apk produced.
- **Interface contracts**: F:\Projects\fitness-ecosystem-pro\PROJECT.md
- **Code layout**: F:\Projects\fitness-ecosystem-pro\athlete-app/

## Change Tracker
- **Files modified**: none yet
- **Build status**: pending
- **Pending issues**: none

## Quality Status
- **Build/test result**: pending
- **Lint status**: pending
- **Tests added/modified**: pending

## Loaded Skills
- none
