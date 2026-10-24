# Phase 1 development candidate — handoff

23 October 2026 · J.D.Printex · Java 21/Gradle/native PostgreSQL/Next.js · no Docker

## Implemented continuation

- Customer registration, expiring/single-use email verification and password recovery/reset; generic account-existence responses, persistent request quotas, profile identity and session revocation.
- Disabled-by-default Google Workspace/Gmail SMTP adapter with encrypted outbox, bounded delivery/retries and payload expiration. Native-only owner-MFA preview supports fictional @example.test messages. No real email was sent or provider connection verified.
- Customer enquiry dashboard/details, secure historic guest linking with matching verified email plus tracking proof, profile/address/organization editing and privacy requests.
- Reusable staff-scoped CRM contacts, permission evidence, duplicate suggestions, revision-safe editing, explicit audited merges and enquiry linking.
- Backend cursor search/filter pagination for staff enquiries and contacts; stable created_at/id ordering.
- Editable product templates for the four confirmed categories, public catalogue/product pages, guided/professional specifications and server validation with immutable submitted snapshots.
- Versioned development privacy notice, recorded guest acknowledgement, honeypot, persistent quotas, bounded API request bodies, production Turnstile adapter and fail-closed configuration gates.
- Artifact admission policy/private-store interface: type/signature/size/path checks and quarantine-only admission. Actual upload/storage/scanning/revision workflows remain Phase 2, with no public upload/download route.
- Existing enrolled owner MFA and staff accounts preserved. No real owner seed was read or changed by this work.

## Actual checks

Final `scripts/Test-Backend.ps1` (Gradle build/integrationTest): PASS. Unit tests: 13; integration tests: 13; zero failures/errors. The focused PhaseOneIntegrationTest covers registration/verification/reset, unverified login denial, token reuse, session revocation, customer isolation, verified-email/token claiming, guest consent/honeypot, privacy requests/review, contact scope/edit/merge/linking, template snapshots/revisions and request body limits. Existing owner/staff/production-boundary tests also pass.

Frontend `npm run typecheck` and `npm run build`: PASS. Builds include /customer, /products, /products/[code], /privacy, /request-quote and /business modules.

The owner subsequently confirmed customer verification/sign-in, enquiry creation/viewing, owner follow-ups/Overdue and REVIEWING status without private follow-up leakage. At the owner's request Codex then ran only the remaining targeted desktop journey: `scripts/Test-PhaseOneBrowser.ps1`, **1 passed in 15.3 seconds** (12.8-second workflow; native fixture setup/cleanup separate). This used the real native backend and a temporary synthetic owner, not mocked API responses. It checked signup/verification, customer prefill and enquiry creation, profile persistence, template save/snapshot preservation, customer denial from owner/staff APIs, contact creation/edit/duplicates, required contact-permission evidence, privacy request/review and password reset/new-password login. Synthetic login cleanup runs in finally; the actual enrolled owner is not modified.

The first targeted run found a real customer-name prefill mapping bug, now fixed and rebuilt. New-enquiry reset also retains the signed-in customer's identity. Subsequent stops were test selectors (dropdown accessible label and Next.js route-announcer alert); these were corrected, and the final focused run passed. Fixture preparation now uses direct JDBC rather than starting another Spring application, and the runner targets only the selected desktop/mobile journey.

The full browser suite and new mobile journey were not rerun. Existing 13 unit/13 integration passes remain the backend evidence; no unnecessary full backend rerun was added for the frontend mapping fix. The broader review and external release gates remain separate.

## Manual acceptance and external gates

The owner walkthrough and focused desktop checks above passed; remaining optional/mobile variants and formal acceptance remain open. Google Workspace credentials/delivery, Turnstile site/secret/hostname validation, final privacy notice/retention decisions, trusted proxy client-address setup, native QA separation, production owner provisioning/recovery, backup/restore and deployment remain unverified release work. Production accounts/intake stay fail-closed without their required configuration. This development candidate is not approval for real customer use or a claim of production readiness.

Privacy resolution records operator review; export delivery/erasure fulfillment is deliberate manual work, not automatic deletion. Organization fields do not implement multi-user corporate membership/credit contracts (later scope). Operational roles are registered but receive their workspaces in later phases. No automatic marketing/provider messaging is enabled.

## Account UI and provider continuation

Owner/customer header cleanup, profile dropdowns, enquiry modal and authentication spacing refinements are implemented. Optional customer Google/Microsoft OIDC and secure explicit linking are implemented behind disabled configuration. Live providers remain unconnected. The latest focused backend checks passed 17 unit tests, four social-account integration tests and one OAuth configuration integration test. Desktop/mobile UI checks: six passed. See UI_REFINEMENT_VERIFICATION.md for this continuation and SOCIAL_SIGN_IN_SETUP.md for deployment configuration.

Owner subsequently authorized the bounded Phase 2 quotation slice. See PHASE2_DEVELOPMENT_STATUS.md; existing release gates remain open.
