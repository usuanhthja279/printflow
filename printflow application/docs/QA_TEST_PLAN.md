# QA and acceptance plan

## Current native staff slice

`./scripts/Test-Backend.ps1` runs nine unit tests and PostgreSQL-backed HTTP tests for session rotation/HttpOnly cookies, CSRF negatives, invalid login, unsupported role denial, manual source capture, assigned-record isolation, stale revision conflicts, transactional activity, assignment hand-off, public tracking privacy, account disable/revocation and logout. Additional prod-profile tests verify bootstrap/demo exclusion even when enabled, guest intake disabled by default and Secure/HttpOnly/SameSite cookie defaults.

`./scripts/Test-Browser.ps1` privately loads local bootstrap credentials and runs real desktop/mobile Edge workflows: all four service links, public form, staff login, claim, status update, overdue follow-up, reload persistence, internal history, public tracking, manual phone enquiry and logout. Screenshots are saved under ignored frontend/test-results. No provider messages are sent.

`python scripts/verify_staff.py --restart` checks persistence of the staff account, assignment, next action/date, priority, revision and activity across native PostgreSQL/API restart, verifies that the old session is revoked and that public tracking remains private. Legacy Phase 0 smoke scripts require explicit -EnableDemo startup and remain separate from the current staff UI.

Current evidence and remaining Phase 1 work are in STAFF_SLICE_VERIFICATION.md. The original Phase 0 scenarios below describe the pre-staff foundation and are retained as historical regression context.

## Phase 0 implemented automated checks

- `backend/src/test/.../TokenUtilTest`: random tokens, hash match and mismatches.
- `backend/src/test/.../EnquiryServiceTest`: happy-path persistence calls, rejection of unknown catalog product.
- Added in stabilization (execution pending Java/Gradle): all status transition combinations, same-status retries including CLOSED without extra events, and wrong/missing tracking token rejection before history lookup.
- `scripts/smoke.ps1` and `scripts/smoke.sh`: health, catalog, persisted enquiry, token tracking, dev owner status advancement, event timeline.
- `scripts/static_validate.py`: static presence/schema sanity validation (NOT equivalent to Java/Next compilation).

## Phase 0 manual E2E
1. Run local Docker stack, verify homepage and product cards.
2. Submit valid enquiry, observe ID/token; refresh owner demo, status NEW appears.
3. Test invalid form (quantity 0, missing email); backend must reject invalid input.
4. In owner demo change NEW -> REVIEWING -> QUOTED.
5. Track with correct token: reflect updated status and timeline.
6. Track with wrong token: 404, no private data leaked.
7. Demo endpoint without/incorrect key: 401. Using `SPRING_PROFILES_ACTIVE=prod` removes it completely.
8. Test responsive layout on mobile, broken API, validation error, no data, refresh.
9. Restart stack without volumes deletion: enquiries remain persisted.
10. Open each J.D.Printex homepage service card; confirm the enquiry form selects wedding cards, hotel print materials, pamphlets/leaflets or offset gloss respectively. Submit each using fictional data; verify owner and tracking views display the matching product. An unknown `?product=` value must leave the valid default selected.
11. Repeat a CLOSED -> CLOSED API update; expect success with no additional event. CLOSED -> REVIEWING must fail without changing the record.

## Roadmap QA suites

| Layer | Tools | Target validations |
|---|---|---|
| Java logic | JUnit 5, Mockito | Price and lifecycle invariants |
| DB integration | Testcontainers + PostgreSQL | Migrations, constraint checks, transactions |
| API contract | OpenAPI and tests | Status codes, DTOs, backward compatibility |
| UI | Vitest/RTL | Dynamic field interactions and permission-hidden views |
| E2E | Playwright | Enquiry -> quote -> proof -> job -> QC -> invoice -> delivery |
| Performance | k6 | Mixed quote/upload/status load against target SLOs |
| Security | OWASP ASVS + ZAP | Injection, upload attack, BOLA, CSRF/session issues |
| Accessiblity | axe + manual | Keyboard, WCAG 2.2 AA |
| Recovery | Backup restore drills | RPO/RTO validation |
| Provider contracts | Mock signed payloads + sandbox | Token revoke, duplicate webhook, out-of-order events |

## Release gate

No production release with unresolved critical/high security risks that threaten customer data, P0 regression, incorrect financial posting, unverified approval or failed backup restore. Features require functional tests, API tests, authorization negatives, migrations, docs, owner/UAT sign-off and operations runbook updates.

## Domain-driven acceptance examples

- FR-009: customer accepts QuoteVersion 3; version 2/3 remain immutable, change creates version 4 and asks for fresh consent.
- FR-027: changing PDF after signed proof forces sign-off on the new hash; old signature cannot authorize revised job.
- FR-040: available stock 400 sheets and two overlapping 350-sheet reservations cannot both succeed.
- FR-053: payment webhook replay 100 times creates only one settlement.
- FR-066: Vendor A cannot access Vendor B package or customer selling price even by altering URL.
- FR-065: incoming social lead with unverified matching information cannot silently merge accounts.

## Acceptance evidence

Every release ticket should contain test output, screenshots for customer/staff UI, PR link, database migration IDs, relevant audit-event example, and owner/QA decision.

## Owner/account extension

Run native Gradle `build integrationTest` using `scripts/Test-Backend.ps1`. OwnerAccessIntegrationTest covers password-only MFA gate, owner vs SALES permissions, encrypted enrollment, TOTP/recovery replay, persistent attempt limit, forced-password gate, password/role/access revocation, self-access prohibition, audit mutation rejection and owner assignment eligibility/revision conflicts. ProductionBoundaryIntegrationTest excludes both local bootstraps in prod.

Run `scripts/Test-Browser.ps1` after starting the native stack. Owner gate checks use the real local password login without enrolling the owner's authenticator. Staff-management UI controls are tested separately with synthetic API fixtures on desktop/mobile; do not describe these as a full enrolled-owner browser/backend test. Production owner recovery, all role modules and remaining customer/privacy/provider flows need additional tests before Phase 1 closure.

## Focused design slice evidence
See PHASE2B_DEVELOPMENT_STATUS.md: 17 unit checks, five design HTTP integration checks, two production-boundary checks, four desktop/mobile synthetic UI checks. The earlier native scan diagnosis used a malformed PDF. Valid artwork now clears the real adapter; NATIVE_ARTWORK_VERIFICATION.md records the real native browser journey. Production launch still requires maintained signatures, an isolated detection fixture check, private volume/backups/restore and retention verification. Avoid repeating the full suite without a new regression concern.

## Live-work refinement checks
Focused checks: LiveWorkIntegrationTest, QuotationIntegrationTest and DesignProofIntegrationTest (14 total) cover lifecycle/date/acceptance/closure/account/conversation scope and existing proof integrity. customer-live-work.spec.ts covers profile photo, account popup, offline quotation/archive and customer review pins at desktop/mobile sizes. See LIVE_WORK_REFINEMENT_STATUS.md for actual runs and provider/scanner limits.
# Focused Phase 2 completion checks

Run `./scripts/Test-PhaseTwo.ps1` for the build/unit checks plus quotation/order, design-proof and live-work HTTP suites. To repeat only an affected class, pass `-TestClass com.printflow.QuotationIntegrationTest` (or another scoped class), optionally `-SkipBuild`. Run `./scripts/Test-PhaseTwoBrowser.ps1` for one real native desktop commercial/design journey, `-Project mobile` for the same mobile flow, or `-Check vocabulary` for the small owner option-settings check. Real providers stay disabled; synthetic browser fixtures leave the enrolled owner untouched and clean up their staff credentials.

Phase 2 coverage includes partial draft restoration/conflicts, exact entered tax rounding/totals, immutable PDF content and paging, amendments without lost acceptance history, withdrawal, idempotent order conversion, private artwork-to-line linkage, deadlines/tasks, feedback resolution/comparison, rejection and evidence-backed offline approval. See PHASE2_COMPLETION.md for commands actually run. Production/backup/provider/advanced PDF processing gates remain separate.


## Focused staff/stage regression — 24 October

Run `scripts/Test-PhaseTwo.ps1 -TestClass com.printflow.StaffScopeIntegrationTest` for four real PostgreSQL/HTTP access cases: intake scope/assignment/optional rejection; owner Contacts grant/revoke and session invalidation; owner archival/public explanation/outbox deduplication; specialist task projection/isolation/version conflicts. Regression checks also cover quotations and live-work permissions. Run `scripts/Test-PhaseTwoBrowser.ps1 -Check staff-stages` for one synthetic owner/Sales/printer journey through intake-only view, quotation-to-artwork transition, dedicated printer task, queue archival and Contacts grant/revoke. The harness cleans synthetic credentials and disables accounts. No real provider delivery or actual manufacture is claimed.

Quotation correction checks: `Test-PhaseTwo.ps1 -TestClass com.printflow.QuotationIntegrationTest,com.printflow.StaffScopeIntegrationTest`; `Test-PhaseTwoBrowser.ps1 -Check quote-review`. The browser creates a synthetic enquiry with a valid PNG through the real native scanner, checks denied wrong-token upload, fixed phone prefix, template suggestions, waiting/revision history and bulk archival. Visual PDF fixture: backend/build/reports/quotation-preview.pdf, rendered with Poppler; old issued PDFs must not be regenerated. Test account cleanup does not physically delete business history.

24 October local Gmail setup preparation: native DEV permits exact loopback HTTP only with explicit recipient restriction and a send-after activation boundary; historical and non-allowed mail is held. Production keeps HTTPS. Enable-LocalGmail.ps1 prompts privately, sends one setup test, saves settings on SMTP acceptance and restarts native services. Temporary mailbox is selected; private credential and real delivery verification remain pending. See GMAIL_LOCAL_SETUP.md.

24 October lifecycle-mail continuation: V13 adds one-time customer setup with quotation, lifecycle customer/team notifications, owner Email activity, proof-gated manually confirmed production/QC/dispatch/delivery milestones, delivery issue holds and durable 48-hour completion. This supersedes earlier “milestone emails/manual operations/timer not implemented” notes only for this bounded slice. Photos/GPS, stock/payment production controls, inbound Gmail sync and multi-page artwork viewer remain pending. Scope, APIs and verification: WORKFLOW_EMAIL_VERIFICATION.md.

24 October signup correction: V14 repairs repeated unverified signup/resend, binds the latest chosen password to verification, and creates guest access at intake acceptance. Customer-only email carries a 24-hour temporary password with forced activation/change and privacy consent; existing passwords are retained. New mail is allowed to any real address by explicit owner request; held/synthetic mail is not replayed. This supersedes earlier recipient-only/link-only account policy. See CUSTOMER_SIGNUP_CORRECTION.md.

24 October full native UI/mail verification: guest intake → temporary credential activation → quotation acceptance → assigned designer → real Defender scans → pinned comments/change request → proof v2 approval → production/QC → dispatch/delivery → accelerated synthetic 48-hour completion passed. All 25 generated customer/team notifications reached SMTP SENT on first attempt; owner Email activity and customer Completed UI were verified. Four focused Gradle workflow tests passed; no Docker. Real timestamps/orders were preserved and test accounts/records cleaned. Exact evidence, safe mail routing and scope limits: UI_END_TO_END_VERIFICATION.md.

24 October quotation email enhancement: V15 binds outbox QUOTE messages to the immutable issued quotation. Native dispatch verifies its PDF hash and attaches that exact version; branded escaped HTML/plain alternatives include the customer workspace link, sender name and short product/reference subjects with existing thread headers. No API route or approval gate changes. Details and focused evidence: QUOTATION_EMAILS.md.

24 October PDF review slice: native resource-limited PDF inspection and authenticated local-worker multipage canvas preview, per-page location comments, full-document page acknowledgements tied to immutable revision/hash/quote, reference/file Preview dialogs and a persistent least-privilege local test designer are implemented. This supersedes earlier multipage PDF viewer pending notes only for PDF/image preview/comment review; source editing, advanced preflight, gallery reuse, delivery photos/GPS and payment/WhatsApp integration remain separate. API, migration, role and focused evidence: PDF_ARTWORK_REVIEW.md. Owner-proposed entry/payment/notification paths: ORDER_ENTRY_AND_PAYMENT_FLOWS.md.

## Focused review/email checks (24 October)

Run native Gradle test --tests com.printflow.identity.QuotationMailTest and integrationTest --tests com.printflow.WorkflowMailIntegrationTest. Verify first intake/work messages have no In-Reply-To; subsequent messages reference only their same-recipient phase. Duplicate event IDs must not enqueue another email. Frontend npm run build checks the PDF preview and quotation terms/filter changes. Do not infer Gmail delivery or inbox grouping from MIME tests. New full UI regression for the 17 comments remains a separate verification task.

## Focused customer review checks
Run Gradle ReferenceReviewTest and identity.QuotationMailTest; DesignProofIntegrationTest plus LiveWorkIntegrationTest protect scope/approval behavior. Start native app with -SkipBuild -DisableMail, then scripts/Test-WorkflowMailBrowser.ps1 -Check review -Project both. This one focused journey checks new sample/reference/stage/layout behavior; it never sends real mail. Restore normal startup after fixture cleanup.


## PF-320 payments and invoices — 24 October 2026
Owner-configured advance/credit rules, immutable verified receipts/corrections, commercial invoice PDF preview/email and server-side production/dispatch gates replace the earlier Not recorded/payment-unimplemented placeholder. Customer and assigned sales are read-only; owner confirms manual finances. Optional Razorpay TEST checkout uses server-owned amounts, HMAC plus captured-payment API verification and deduplicated callbacks/webhooks. Live gateway payments and GST tax invoicing are not enabled. Routes, schema, screen behavior, security boundaries and focused verification: PAYMENTS_AND_INVOICES.md.


24 October visual experience refresh: public hero/catalogue/product-finish interactions, live quote brief, scoped customer/owner overview cards, role navigation and work/payment visual hierarchy are implemented without replacing business state. See UI_EXPERIENCE_REDESIGN.md; screenshot/test evidence is packaged separately.

## 24 October implementation-first UI completion

Reference-inspired UI covers interactive public print previews, searchable enabled products, scoped customer/business/designer summaries, stage-aware artwork samples and comments, wide work/conversation layout and phone navigation. Final polish includes sample/designer search, stage links and conversation send/refresh states. See IMPLEMENTATION_REVIEW.md for scope and manual review. One final frontend build/type check; prior focused Gradle results reused. Real Razorpay UI capture, captured external webhooks and invoice inbox delivery remain unverified; broad retries stopped at the owner’s request.

## 24 October operations continuation

Owner-authorized automatic designer routing, project-level production queues and private delivery photo evidence are implemented. PF-300/PF-330 now include printing/finishing/QC/rework/hold progression, scoped printer assignment and pickup/transit/delivery evidence. Existing proof, quotation, finance and dispatch gates remain. See OPERATIONS_WORKFLOW.md for APIs, migrations V24/V25, role scopes, upload limits, notifications and the focused Gradle/browser commands. Full shop-floor routing, inventory, capacity reservations, GPS and partial dispatch remain separate.


## 25 October workflow redesign — Batch 1

Focused workflow-redesign.spec.ts intercepts all API traffic using synthetic DTOs: desktop/phone proof page acknowledgements and stale response, quarantine/advance blockers, scoped owner inbox, production HOLD/RESUME revision payloads, drawer focus/escape, dispatch failure and original scanned photo preview. These presentation tests do not claim real provider delivery or capture. Native Gradle operations regression checks the real HTTP/database gates separately. See [implementation map and evidence](WORKFLOW_REDESIGN_BATCH1.md).
