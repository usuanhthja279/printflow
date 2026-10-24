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
