# J.D.Printex — native staff slice verification

23 October 2026. **Status: first staff access/follow-up slice verified locally; full Phase 1 and production launch remain incomplete.** Native Phase 0 was completed first, as recorded in PHASE0_VERIFICATION.md. The owner authorized proceeding and directed use of Gradle and native local development rather than Maven/Docker.

## Delivered behavior

- Native PostgreSQL 17.11 + Java 21 + Node; checked-in, checksum-pinned Gradle 9.8.2 wrapper is the only backend build. No pom.xml or local Maven requirement remains. Docker/Compose are optional packaging references and never used by native start/stop/test scripts.
- `/business` is a real sales sign-in/workspace, replacing the demo-key UI. Passwords use BCrypt(12). Sessions rotate on login, have HttpOnly/SameSite=Strict cookies and a 30-minute idle timeout; Secure is default and explicitly off only in native dev configuration. Mutations require CSRF. Login has bounded account/IP throttling. Disabled accounts and changed roles lose their session on the next request; logout invalidates the server session.
- SALES can view own assignments and shared unassigned enquiries, claim/release, capture manual source/contact/spec data, set priority and next action/date, filter overdue work, advance status and read internal activity. OPERATOR cannot access CRM APIs. Other staff assignments are excluded/404 even with altered IDs. Row locks plus revision checks prevent stale edits.
- V2 adds staff, assignments, source, follow-up, revision and activity without modifying V1. Mutations and audit run transactionally. Public status tracking does not expose contacts, follow-up, assignment or internal history. Closing clears the follow-up plan.
- Bootstrap creates only a development SALES account, only under dev/QA, and never overwrites an existing account. Generated credentials remain in ignored `.env`, never in reports or logs. Demo APIs are disabled by default and excluded in prod even if the flag is enabled.

## Actual validation

- `./scripts/Test-Backend.ps1` — PASS. Gradle build and **9 unit + 8 PostgreSQL HTTP/integration tests**, zero failures or skips in XML reports. Unit tests cover token/service state invariants and concurrent/expiring login throttles. HTTP suites boot real Spring Security and PostgreSQL, verifying CSRF, invalid credentials, session rotation, password hashing, unsupported role denial, manual intake, cross-staff isolation, revision conflict, audit counts, hand-off/closure, public privacy, disable/logout and login limits.
- ProductionBoundaryIntegrationTest — PASS, included in the eight integration tests. Prod profile excludes local bootstrap and demo controllers even with enabled flags, requires staff authentication, disables guest intake by default and issues Secure/HttpOnly/SameSite=Strict cookies with no-store CSRF responses.
- Frontend `npm run typecheck` and `npm run build` — PASS. Native standalone preview builds and starts; two build workers. The build uses the loopback native API address.
- `npm run test:e2e` — PASS, **two full desktop/mobile Edge workflows**: category preselection, public submission, staff login, claim/status change, overdue follow-up, reload persistence, internal history, customer tracking, manual PHONE enquiry and logout. Corrected a test-only exact-label/select locator after its first timeout; assertions were preserved. Browser screenshots were visually inspected. A small existing logo dot layout defect was corrected; the current build then passed both browser workflows through `./scripts/Test-Browser.ps1` (2 passed in 24.5 s).
- `python -u scripts/verify_staff.py --restart` — PASS. Real native database/API restart preserves the staff account, assignment, action/date, priority, status, revision and activity. The old session is rejected after restart; re-login works; original customer tracking token/history still works; internal data stays private; logout revokes access. Invalid incomplete follow-up plan returns 400.
- `python scripts/static_validate.py` — PASS, 39 required artifacts/conventions. All eight native PowerShell lifecycle/setup/test scripts parse. The archive-only `.env` rule is separate from normal local validation.
- Docker processes were absent in the checked host snapshot. At that snapshot the API/Node working sets were approximately 293/83 MB and summed PostgreSQL working sets 116 MB. These are observations, not total memory guarantees; shared pages may be counted more than once. Java's 384 MB heap cap does not cap all native process memory.

Final host checks confirmed health UP and all three listeners bound to 127.0.0.1 (ports 3000, 8080 and 5434), with no Docker process running.

Reports are under ignored `backend/build/reports/tests/{test,integrationTest}`. Browser evidence is under ignored `frontend/test-results`; logs and native data under `.local`. Existing synthetic test records are preserved. No real customer files, provider keys or real user PII were introduced.

## Remaining scope and limits

This is a single-company, single-API staff slice. Full owner/admin MFA, user provisioning/reset/OTP/recovery, customer identity/organizations, full role matrix, CRM merge/pagination, complete auth audit and privacy/anti-abuse/retention are pending. Sessions and rate counters are in memory; API restart intentionally revokes sessions. There is no external reminder delivery, real quote generation, artwork approval, production, invoicing or live provider integration. Manual intake still requires both email and phone under the inherited validation rules.

Production still requires TLS/secret management, restricted database roles, monitored backups with restore evidence, security/privacy review and remaining Phase 1 acceptance. The portable database uses a development cluster/account; the bootstrap password is retained only for local setup. CI now configures a remote ephemeral PostgreSQL service for Gradle integration tests, but no remote CI execution is claimed because this folder has no Git repository/remote. Optional container runtime is unverified and not required for the requested native development workflow.

Start `./Start-PrintFlow.ps1 -SkipBuild`, stop `./Stop-PrintFlow.ps1`, and open `/business`. Read the two local APP_STAFF_BOOTSTRAP username/password values from `.env` privately. See LOCAL_DEVELOPMENT.md and REQUIREMENTS_TRACEABILITY.md for operational details and explicit partial requirement status.
