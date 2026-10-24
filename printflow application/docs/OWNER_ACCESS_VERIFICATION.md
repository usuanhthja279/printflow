# Owner access and staff administration — local verification

23 October 2026 · J.D.Printex · native Gradle/PostgreSQL/Node workflow · development only

## Implemented

V3 adds OWNER and operational role identifiers, forced-password state, auth versions, encrypted authenticator seeds, persistent MFA replay/attempt counters, hashed single-use recovery codes and security activity. Existing V1/V2 migration contents and SALES credentials are preserved. Normal UPDATE/DELETE of security and lead audit rows is rejected by triggers; a database administrator still has privileged control.

A separate dev/QA-only first-owner bootstrap and native secret generation are implemented. OWNER must enroll/verify MFA before private business access. Account creation, role/enable/disable changes and password resets require verification within five minutes. Existing sessions are rejected after password, role, access or auth-version changes. New/reset staff passwords must change before business use. Self-access mutation is blocked; last-enabled-owner protection is serialized in the database. No public owner registration or MFA reset/bypass exists.

Owner enquiry oversight/reassignment uses the existing locked revision checks, eligible enabled OWNER/SALES targets and actor-attributed activity. Sales ownership isolation remains enforced. The `/business` UI includes onboarding/challenge/recovery acknowledgement, forced password change, staff controls, security activity and owner assignment.

## Commands and actual results

- `./scripts/Test-Backend.ps1`: **passed**, Gradle build plus **11 unit tests and 9 PostgreSQL HTTP/security integration tests**, zero failures/errors in final XML reports.
- TOTP unit tests pass the six SHA-1 RFC 6238 time vectors reduced to six digits; malformed codes and bounded clock skew are checked.
- `OwnerAccessIntegrationTest`: real HTTP/CSRF login, owner password-only denial, SALES admin denial, MFA enrollment/encryption, single-use recovery and TOTP replay rejection, persistent throttling, staff create/forced change, role/disable/reset revocation, self-access rejection, audit immutability and owner reassignment/eligibility/stale-version checks passed.
- Production boundary checks additionally confirm owner bootstrap is absent even when its enable flag is set in the prod test configuration. Intake defaults remain disabled and cookies default Secure/HttpOnly/SameSite Strict.
- Frontend `npm run typecheck` and `npm run build`: **passed**. Native Windows standalone output required the normal elevated file-link permissions in this environment.
- `./scripts/Test-Browser.ps1`: **6 passed** across desktop and mobile Edge. Existing customer/staff/manual-intake/tracking flows pass against the native backend. Owner password login reaches the real MFA gate; protected data stays inaccessible after reload. Staff-management controls use explicitly synthetic API fixtures for UI create/role/disable/enable/reset tests; this is not claimed as a fully enrolled-owner browser/backend end-to-end test.
- Owner mobile gate and synthetic desktop/mobile staff-management screenshots were visually inspected. No real authenticator seed or recovery code was captured. Screenshots are ignored under `frontend/test-results/`.
- `python scripts/static_validate.py`: **passed**; structural checks only. Modified PowerShell scripts passed parser checks.

During verification, a new test initially expected 200 for enquiry creation; corrected to the existing 201 contract. The first browser run began before frontend readiness and had two connection failures; the runner now waits for web/API readiness, and the final six-test run passed. Neither failure is hidden or treated as a successful run.

`python scripts/verify_staff.py --restart`: **passed**. Native restart invalidated old sessions and preserved staff account, assignment, next-action/time, revision and activity. Public token tracking remained private and logout revoked the new session.

## Infrastructure changes and limits

The CI definition now starts/stops native PostgreSQL 17 on Ubuntu 24.04 instead of declaring a container service. It checks `/usr/lib/postgresql/17/bin` and fails if absent. No remote GitHub workflow execution or runner compatibility result is claimed. Native QA separation, Cloudflare Access/tunnels and production systemd/Caddy setup are still planned, not provisioned.

The local owner has deliberately **not** been enrolled by automation. Retrieve credentials privately from ignored `.env`, enroll your own authenticator at `/business` and save recovery codes. Preserve the encryption key securely. Owner lockout/recovery and production provisioning need a tested operator procedure. Password-login counters and sessions remain single-process/in-memory; MFA throttling/replay state persists in PostgreSQL. Staff/account and enquiry lists retain documented limits rather than full backend pagination.

Only OWNER and SALES have business enquiry modules. Customer identity, Google Workspace delivery, reusable contact/address/organization records, configurable product templates, guest privacy/anti-abuse, secure file groundwork and complete production release controls remain unfinished Phase 1 work. See `DEVELOPMENT_HANDBOOK.md` for the ordered slices and exit gates. This result does not close Phase 1 or authorize real customer use/deployment.
