# Repository instructions for Codex / automated contributors

1. Begin with `CODEX_MASTER_PLAN.md`, `README.md` and `docs/`. Treat phase instructions and requirement IDs as binding for scope.
2. Do **not** start Phase 1 until Phase 0 build/test/verification is actually complete and user asks. Phase 0 must not be described as production-ready.
3. Java 21 + Spring Boot modular monolith, Next.js 16 TypeScript, PostgreSQL 17 + Flyway, native Windows development and native deployment services. Use the checked-in Gradle wrapper, not Maven; no Docker/Compose in the active workflow (owner decision). Prefer backward-compatible migrations.
4. Auth: `/api/demo` is for LOCAL DEV/QA only and excluded in `prod` profile; replace with RBAC Phase 1. Public intake must remain disabled in production until anti-abuse/privacy.
5. Never expose real payment, email, WhatsApp, Facebook/Instagram integration statuses as connected until configured and tested through official provider APIs.
6. No API keys, OAuth secrets, customer files or real user PII in repo, logs, issues or AI prompts.
7. Add tests and update requirement traceability, API contracts, screens and architecture docs when changing behaviour.
8. Preserve immutable approvals, clear status transitions, private internal cost data, and external-vendor ACL boundaries.
9. On a networked Windows machine first run exact Phase 0 verification; generate `package-lock.json` and use `npm ci` afterward.
10. Report actual commands run, what passed/failed, and any limitations. Do not fabricate a test pass.

## Efficient implementation and verification

- Trace the full affected flow before editing: customer, business/staff and designer screens, API calls, status transitions, file access and email notifications. Identify shared dependencies and stage gates early.
- Batch related changes into one coherent implementation pass before building. Avoid repeated exploratory edits followed by full builds.
- Check test fixtures and selectors early: account roles, email-capable recipients, starting statuses, default form values and uniquely scoped controls. Separate fixture or selector failures from product defects.
- Keep testing focused on the changed behaviour and its critical regressions. Use the checked-in Gradle wrapper for backend checks; use native services, no Docker or Compose.
- Reuse running services where possible. Build or restart only when changed code or configuration requires it; test-only or selector fixes should not trigger another application build or restart.
- After focused checks pass, broaden or repeat verification only for new changes, failures or unresolved risks. Report concrete results and remaining limitations.

### Local verification recovery notes

- Verify each local service by checking its listener and health endpoint; a stale `.local/*.pid` file is not proof that a process is alive. Inspect the process command line before stopping anything, and preserve the user's running services.
- Native database startup reads the ignored `.env`, but a Gradle shell also needs `DB_URL`, `DB_USERNAME` and `DB_PASSWORD` mapped from `POSTGRES_*`. The checked-in local database uses port `5432`; do not rely on Spring's placeholder password or stale port numbers in test scripts.
- A running Next standalone server holds `frontend/.next/standalone` open, so a default production build can fail with Windows `EBUSY`. Keep that server running and use the supported `PRINTFLOW_NEXT_DIST_DIR` setting for an isolated build (for example `.next-batch5`), with that same setting when launching its standalone preview. Copy static assets beneath the configured dist directory in the standalone tree. Revert any Next-generated `tsconfig.json` include additions and keep alternate build directories ignored.
- When browser tests fail, read the first assertion and saved page snapshot before rebuilding. Scope locators when list summaries duplicate conversation content; wait for async UI confirmation before asserting request counters; update stale selectors to current visible copy. Re-run only the focused spec after these fixes.

### Windows browser-test startup and recovery

- The frontend Playwright config targets installed Microsoft Edge (`msedge`) by default; `PLAYWRIGHT_CHANNEL` is the explicit override. Run tests through the project Playwright config instead of manually launching Edge or attaching to the user's interactive Edge profile.
- If Edge/Playwright fails before a test starts with a filesystem sandbox `EPERM`/`EACCES` launch error, classify it as an execution-permission failure, not an app defect. Retry the same focused spec once with the approved execution permissions. Do not repeat the same blocked launch, rebuild the app, or restart services for a browser-executable permission error.
- If the approved retry still fails, inspect the first launch error and confirm the configured Edge channel/executable is available. Change browser channel only when the configured browser is unavailable and the replacement is supported in this environment; record the channel used. Do not delete browser profiles, kill the user's Edge processes, or install browsers speculatively.
- Separate startup failures from page/test failures: first confirm the existing web service responds at `PLAYWRIGHT_BASE_URL` (default `http://127.0.0.1:3000`), then run one relevant spec with `--max-failures=1`. Fix a selector or synthetic fixture from the failing assertion and rerun that spec only; do not rebuild/restart unless application code/configuration changed.
- The first Batch 1 browser attempt was blocked by the filesystem sandbox before page checks; subsequent focused Playwright runs completed. Use this recovery path rather than repeating exploratory Edge launches.

### Local Windows testing and process ownership

- Before a Gradle command, resolve Java 21 instead of assuming a fresh shell inherits `JAVA_HOME`. Check `Get-Command java`, then common user-managed locations such as `$env:USERPROFILE\.jdks\*` and `$env:USERPROFILE\.gradle\jdks\*`. For an installed JDK, set `$env:JAVA_HOME` and prepend its `bin` to `$env:Path` in the same PowerShell command that invokes `backend\gradlew.bat`. Do not install another JDK just because a new shell has no Java path.
- Use native PostgreSQL on port 5432, Spring API on 8080 and the UI on 3000 for normal local work. Read `.env` values into the current process without printing them; map `POSTGRES_*` to `DB_URL`, `DB_USERNAME` and `DB_PASSWORD` where Gradle needs those names. Do not copy secrets into tracked files, command output or test evidence.
- Batch migrations and backend changes, then run one focused Gradle command. Avoid overlapping Gradle invocations against the same checkout; wait for the current command to finish before starting another to prevent daemon/build locks. Do not use Docker or Compose.
- Isolate temporary acceptance data in a dedicated test database/schema and synthetic users/artifacts. Never reset, truncate or drop the user's normal local database. Verify the target database name and port before any cleanup.
- Keep build and browser-test outputs in the active worktree. Reuse installed dependencies only when package versions match the lockfile; do not use writable junctions to another checkout's `node_modules` because npm cleanup can follow them and Next may resolve outside the worktree.
- Stop only processes started for the current task, by recorded process ID and verified command line. Do not stop an already running UI/API/PostgreSQL service belonging to another checkout unless the user explicitly asks and the target is confirmed. Leave task-started servers stopped by default at completion; keep them running only when the user explicitly asks.

## Visual evidence for UI changes

- For every significant screen or interaction change, capture the actual app in a browser before and after when the baseline is available. Use 1440 × 900 desktop and 390 × 844 mobile viewports for responsive screens.
- Inspect the captured images themselves for layout, wrapping, overflow, clipping, hierarchy, contrast and unreachable controls; a passing screenshot command or DOM assertion alone is not visual verification.
- Keep screenshots tied to the tested revision and record the route, role, state, viewport and whether data is synthetic or from the local backend. Do not include credentials, session tokens, private customer data or artwork without permission.
- If actual screenshot capture or image inspection is unavailable, say so explicitly and mark visual verification incomplete. Never fabricate or substitute generated mockups for browser evidence.
