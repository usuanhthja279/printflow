> **Customer signup correction:** signup/retry now sends fresh verification; guest acceptance provisions a customer-only temporary password with mandatory change/consent; new mail may reach any real address. Existing passwords are preserved. See [signup verification](CUSTOMER_SIGNUP_CORRECTION.md).

> **Lifecycle email/UI test slice — 24 October:** lifecycle notifications, secure quotation account setup, manual production/delivery milestones and the 48-hour issue-aware completion timer are now implemented for local review. Real delivery is enabled only for the chosen Gmail test inbox. Full manufacturing/delivery operations, photos/GPS and inbound mailbox sync remain separate. See [workflow verification and UI steps](WORKFLOW_EMAIL_VERIFICATION.md). This supersedes older “Phase 3 has not begun” wording for this limited slice.

> **Quotation review corrections — 24 October:** optional initial private artwork, fixed +91 phone entry, paper/size suggestions, waiting-for-acceptance state, explicit artwork requests and revision gating are implemented. Enquiry acknowledgement and quotation messages use stable conversation headers; real Gmail/WhatsApp delivery remains unverified/disabled. Owner can archive individual/selected enquiries including accepted work, retaining agreements and notifying with an explanation; this supersedes the earlier accepted-work dismissal restriction. Privacy administration is collapsed. Newly issued PDFs use blocks/grids; historical PDFs remain immutable. Evidence: QUOTATION_REVIEW_CORRECTIONS.md.

> **24 October fulfilment and access revision:** use [One-stage-at-a-time fulfilment](FULFILMENT_WORKFLOW.md) for the agreed lifecycle and [Staff/stage review](STAFF_STAGE_REVIEW.md) for current implementation evidence. Intake is owner/designer only; Sales sees assigned reviewed enquiries; other staff have dedicated assigned-task queues. Contacts requires an owner grant. The popup now presents its current stage and expandable history. Multi-page PDF review, actual production/delivery execution, location and the 48-hour completion timer remain planned. This supersedes conflicting role/shared-intake examples below.

> **24 October Phase 2 implementation:** commercial/design development now includes drafts, item/tax pricing, controlled vocabulary, immutable PDFs, withdrawal/amendments, commercial orders/artwork linkage and full designer review controls. See [Phase 2 completion and review](PHASE2_COMPLETION.md). Owner acceptance and cross-phase release gates remain open; Phase 3 operations have not begun. No expiry, native Gradle, no Docker.

> Active policy update: see Owner workflow corrections (24 October 2026) below. These supersede earlier expiry and manual status examples. Current evidence: LIVE_WORK_REFINEMENT_STATUS.md.

# J.D.Printex — phase-wise development handbook

Version 1.0 · 23 October 2026 · Active implementation roadmap

Business: J.D.Printex, intended domain `jdprintex.in`. Initial products: shaadi/wedding cards, hotel print items, pamphlets/leaflets and offset gloss printing. The business goal is growth: capture enquiries, follow up reliably, convert approved quotes into jobs and build repeat business.

This handbook orders development; it does not certify unfinished features or promise delivery dates. P0/P1/P2 in the SRS are **priorities**, not development phase numbers. Preserve requirement IDs in `REQUIREMENTS_TRACEABILITY.md` and the scope in `SRS.md`/`PHASE_BACKLOG.md`.

## Working rules for every phase

Use Java 21 + Spring Boot, the checked-in **Gradle wrapper**, Next.js/TypeScript and native PostgreSQL 17. No Maven build or Docker/Compose dependency. Windows DEV is native; future QA uses a separate native cluster and configuration; future production uses native Ubuntu services managed by systemd and Caddy. Follow the updated infrastructure handbook at `../../HDPrintX_Codex_Dev_QA_Prod_Cloudflare_Tunnel.md` for deployment boundaries.

Implement complete vertical slices: migration, service/permission checks, API, usable screen, failure states, meaningful tests and documentation. Add new Flyway migrations; never rewrite applied V1/V2/V3 history after deployment. Keep customer/private staff data and credentials out of source, screenshots, logs and AI prompts. Use synthetic fixtures.

Each slice closes only after its acceptance checks pass and evidence is written. Keep unimplemented provider connections disabled and labelled pending. No real customers, public preview, DNS changes, purchases or production deployment are implied by a development phase.

## Current position

Native Phase 0 and the Phase 1 identity/enquiry/contact/product-template slices have recorded local verification. Phase 2 commercial/design implementation is ready for owner review: see `PHASE2_COMPLETION.md`. Formal Phase 1 release gates (provider configuration, manual acceptance, privacy/backup/environment hardening) remain open separately; no production release or Phase 3 operational implementation is claimed.

Customer identity, CRM contacts, product templates, privacy request flows and abuse/file policy foundations are now implemented as a development candidate. See `PHASE1_DEVELOPMENT_STATUS.md` and the owner-led `PHASE1_MANUAL_CHECKLIST.md`. Manual acceptance, provider configuration and environment/release hardening remain open. Google Workspace/Gmail is the selected email provider; no live connection or delivery is claimed.

## Phase 0 — reproducible native foundation

**Business outcome:** a developer can start, inspect and stop the prototype without a container engine.

Scope: native PostgreSQL initialization, Gradle backend, Next.js frontend, health checks, Flyway migrations, public catalogue, synthetic enquiry creation and token-gated tracking. DEV bootstrap secrets are generated privately. Default production intake remains disabled; legacy demo endpoints remain non-production only.

Acceptance:

- A clean native build starts on loopback ports 3000/8080/5434.
- Unit/integration/frontend checks pass, migrations apply and data survives a service restart.
- Tracking requires its secret token and omits private follow-up/assignment information.
- Start/stop scripts identify only project processes and preserve data.

Evidence: `PHASE0_VERIFICATION.md`, `LOCAL_DEVELOPMENT.md`, `STAFF_SLICE_VERIFICATION.md`. Native QA and remote CI are separate gates; do not infer their completion from DEV tests.

## Phase 1 — secure access, customers and enquiry conversion

**Business outcome:** owner and staff can capture every enquiry, assign responsibility and make the next follow-up visible, with verified customer identity and controlled access before release.

### 1A. Owner and staff identity — current implementation slice

- Individual BCrypt-password accounts, CSRF-protected session login/logout and bounded password-login attempts.
- Mandatory owner authenticator enrollment/challenge, expiring enrollment, encrypted MFA seed, replay-resistant TOTP and single-use hashed recovery codes.
- Owner-only account creation, role/access changes and password reset, requiring recent MFA. New/reset staff passwords must change at sign-in.
- Owner cannot disable or change their own role through administration; preserve at least one enabled owner. Changes revoke existing sessions.
- Store security activity without passwords, MFA material or recovery codes; audit rows reject normal update/delete operations.
- Register roles OWNER, SALES, ESTIMATOR, DESIGNER, SUPERVISOR, OPERATOR, INVENTORY, ACCOUNTS and DELIVERY. A registered role does not imply its later workspace exists. Full enquiry management is limited to OWNER/assigned SALES after intake; OWNER/DESIGNER review incoming intake. DESIGNER sees assigned design work; other roles have scoped assigned-task queues. Contacts requires an owner grant for every non-owner.

Acceptance: anonymous/sales users cannot administer accounts; password-only owner sessions cannot read business data; wrong/replayed MFA fails; recovery codes work once; throttling persists; forced-password gate cannot be bypassed; disable/reset/role change invalidates sessions; no credential fields in account responses; self-access changes fail.

Local owner onboarding: run native startup, retrieve `APP_OWNER_BOOTSTRAP_USERNAME` and password privately from ignored `.env`, sign in at `/business`, enroll an authenticator and save recovery codes privately. Never share the setup key or codes in chat. Back up the MFA encryption key securely; changing/losing it prevents seed decryption. Production bootstrap/provisioning is not implemented by the DEV bootstrap.

Remaining identity hardening: tested owner lockout/recovery operations, persistent distributed password-login throttling if required, session store/timeout UX, additional role-permission tests and a production account-provisioning procedure. Do not introduce a public owner-registration or MFA-bypass endpoint.

### 1B. Customer identity and Google Workspace delivery — next

Requirements: FR-001, FR-002, FR-011, NFR-006/NFR-016.

Build verified-email registration/invitation and password recovery using expiring, single-use hashed tokens, generic anti-enumeration responses and rate limits. Link verified customers only to their own enquiries; never claim historic guest enquiries using an unverified email match. Require tracking proof or reviewed staff linking. Customer dashboards must enforce ownership in the backend.

Implement an email adapter/outbox with disabled-by-default delivery, retry limits and safe failure visibility. Configure Google Workspace/Gmail only after administrator configuration; use approved SMTP/relay/API credentials and TLS. Separate service notifications from marketing consent. Never log password-reset/verification links or expose an unrestricted mail inbox.

Acceptance: expired/reused tokens fail; cross-customer reads/writes fail; registration/reset responses do not reveal account existence; duplicate sends are controlled; failed delivery does not grant verified status. Real provider delivery remains pending until approved recipients and credentials are tested.

### 1C. Contacts and CRM follow-ups

Requirements: FR-003, FR-004, FR-005.

Existing slice: manual walk-in/phone/message source, assigned staff, claim/release, status progression, priority, next action/time, overdue queue and activity. Owner oversight/reassignment extends this slice.

Remaining: reusable customer/contact records, addresses, organizations, permission-to-contact evidence, duplicate suggestions and explicit audited merge/linking; backend pagination/search/filtering and assignment notifications/tasks. A source labelled WhatsApp records provenance, not a provider connection. Do not silently merge matching names or contacts.

Acceptance: owner can assign only eligible enabled staff; sales cannot view another person's assigned lead; stale revision conflicts preserve newer work; reassignment and follow-up changes are audited; public tracking never exposes internal notes. Pagination must not lose or duplicate rows under its documented ordering.

### 1D. Product catalogue and guided specifications

Requirements: FR-006, FR-007; controlled vocabulary groundwork for FR-016.

Build owner-maintained templates and public landing pages for the four confirmed product families. Guided mode asks understandable questions and permits “need advice”; professional mode captures validated size, GSM, sides, colour/finish and delivery needs. Preserve submitted template/specification versions so later edits cannot change an existing request.

Do not invent price lists or production capabilities. Collect owner-approved paper, finish, size and minimum-quantity options. Maintain legacy product codes safely where existing records use them.

Acceptance: server validation matches the product template; invalid combinations fail clearly; both modes produce the same canonical specification; product edits do not rewrite old enquiries; mobile forms and keyboard navigation work.

### 1E. Privacy, abuse and file foundations

Requirements: FR-002, NFR-006/NFR-007/NFR-011 and PF-140.

Add versioned privacy/consent records, retention/export/erasure request procedures, guest rate limits and bot protection with accessible fallback. Keep production guest intake disabled until configured and verified. Add private storage interfaces, quotas and deny-by-default permissions before implementing Phase 2 artwork upload. Browser file-type checks alone are insufficient.

Acceptance: production defaults fail closed; anonymous bursts and oversized inputs are bounded; CSRF is enforced on session mutations; consent evidence is stored; no public object enumeration or unsafe file execution. Full artwork revision/proof workflows belong to Phase 2.

### Phase 1 exit gate

Complete 1A–1E, requirements traceability and security review; demonstrate owner/staff/customer isolation, invite/reset recovery, mobile enquiry and follow-up paths, verified native QA separation, tested backups and applicable privacy/abuse controls. Record pending external configuration separately. Phase 1 closure is not automatic authorization to publish.

## Phase 2 — quotations, artwork and approvals

**Business outcome:** staff can send an exact quote and proof, capture approval and create an order without re-entering specifications.

Dependencies: Phase 1 identity, CRM, product schemas and file permissions. Epics PF-200–230; FR-008–010, FR-014–016, FR-024–028 and applicable file security.

Build manual itemized quotes with quantity, paper/finish, delivery, tax and terms; versioned branded PDFs; validity/revision controls; immutable acceptance of a specific quote/specification. Add private artwork versions, design assignments, proof review and explicit approve/reject/change requests. Approval must bind identity, timestamp and the exact content hash. Convert the accepted version to an order transactionally.

Acceptance: an old link cannot approve a newer version; unauthorized viewers cannot download artwork; uploads are scanned/validated and bounded; repeated conversion is idempotent; accepted totals and specifications remain immutable. Public totals omit internal cost/margin data. No AI-generated prices or legally assumed tax rules.

## Phase 3 — production, basic inventory, finance and delivery MVP

**Business outcome:** complete a real operational workflow in a controlled pilot, from approved order to quality check, invoice and dispatch.

Dependencies: approved Phase 2 order/proof. Epics PF-300–330; production, basic inventory, billing and delivery requirements in the SRS.

Create print jobs/tickets per order item, operation queues, assignees and dependencies; start/pause/finish with produced/waste quantities; basic stock issues, QC/rework and accountable handoffs. Add invoices, manually recorded payments/balances, split dispatch and customer-visible milestones. Customer progress must follow persisted operational events rather than cosmetic percentages.

Acceptance: no production release without required approvals; operation/stock updates handle concurrency; quantities, waste, invoice totals and balances reconcile; unauthorized roles cannot bypass QC or financial controls; customer views exclude staff-only data. Owner completes a representative end-to-end pilot and recovery drill.

This is the first operational MVP candidate. Production release additionally needs the infrastructure handbook's security, backup, monitoring and explicit approval gates.

## Phase 4 — reliable communications and payment integrations

**Business outcome:** reduce repeated manual entry and missed communications with supported, tested provider connections.

Epics PF-400–440. Build durable outbox/inbox, idempotency keys, retries/dead-letter reporting and provider health before automating Google Workspace conversations, official WhatsApp, payment links/webhooks or supported Meta lead ingestion. Basic identity email from Phase 1 precedes this broader conversation integration.

Acceptance: duplicate/out-of-order webhooks do not create duplicate leads/payments; outages recover without silent loss; inbound identity/signatures are verified; approved scopes, consent and templates are enforced; settlement reconciles to provider records. “Connected” requires a successful official-provider test. Never automate staff/customer messages without the relevant authorization.

## Phase 5 — outsourcing, procurement and advanced scheduling

**Business outcome:** coordinate vendors, purchasing, equipment and delivery while preserving commercial privacy.

Epics PF-500–520 and corporate-account controls where required. Add vendor invitations and scoped job packages, purchase/reservation workflows, machine calendars, advanced mobile queues and delivery coordination. Keep vendor access narrower than internal staff access; exclude unrelated customers, internal costs and margins.

Acceptance: vendor resource-isolation tests pass; competing reservations cannot over-issue stock or capacity; invitations expire/revoke; handoffs and exceptions are auditable. Confirm actual machines, vendor agreements and shop roles with the owner before configuring routes.

## Phase 6 — validated offset costing, preflight and analytics

**Business outcome:** improve quotation consistency and margins using measured shop data.

Epics PF-600–620; FR-017–023 and advanced prepress. Build versioned cost books for substrates, plates, makeready, machine time, waste, finishing, labour and overhead; deterministic imposition/yield calculations, quantity alternatives and approval thresholds. Add sandboxed preflight checks and reconcile quote-versus-actual analytics.

Acceptance: estimator-approved historical jobs match documented tolerances; calculation inputs/rules are reproducible; changed costs do not rewrite old quotes; margin access is restricted; malicious PDF fixtures remain isolated. Performance claims require measured workloads. Suggestions must show assumptions and require staff approval.

## Phase 7 — optional assistance and growth tools

**Business outcome:** measured gains in lead conversion, repeat business or staff efficiency after core operations are reliable.

Epics PF-700–720. Consider AI-assisted enquiry summaries, human-reviewed cost/route alternatives, consent-based review/marketing insights and supported publishing. Multi-company SaaS is a separate discovery/security decision, not an assumed requirement for this shop.

Acceptance: demonstrate benefit against a baseline; restrict model access to permitted data; defend against prompt injection; redact sensitive content; require approval for prices, commitments and outgoing messages. Keep deterministic business rules authoritative.

## Cross-phase environment and release work

Native QA, container-free CI, private Cloudflare previews and native VPS deployment follow the infrastructure handbook. Implement and test each separately; DEV success does not validate QA or production. Keep `.env` files, cluster data, logs and provider secrets separate. Preserve Google Workspace DNS when configuring `jdprintex.in`.

Before a release, record the source revision, immutable artifacts/checksums, schema version, test reports, privacy/security sign-off, tested restore evidence, owner UAT and rollback/roll-forward procedure. Do not expose API/database ports publicly. Application access control remains mandatory behind Cloudflare Access.

## Per-slice handoff checklist

1. State the business outcome, requirement IDs, dependencies and exclusions.
2. Implement a usable route with backend authorization and safe validation/errors.
3. Add backward-compatible schema changes and document API/screen/architecture changes.
4. Run relevant Gradle, frontend, integration and browser checks; record failures and actual limits.
5. Update traceability and this phase status. Provide concise operator steps only where accounts/secrets or external approval are required.

## Native verification commands

From `printflow application/`, stop running application builds before replacing output:

```powershell
./Stop-PrintFlow.ps1 -KeepDatabase
./scripts/Test-Backend.ps1
python scripts/static_validate.py
Push-Location frontend
npm run typecheck
npm run build
Pop-Location
./Start-PrintFlow.ps1 -SkipBuild
./scripts/Test-Browser.ps1
python scripts/verify_staff.py --restart
```

Use the existing wrapper rather than a globally installed Gradle. Linux CI runs `bash gradlew --no-daemon build integrationTest` from `backend/` against a native disposable PostgreSQL instance. Never substitute a documentation checklist for actual test output.

Owner preference: keep automated implementation checks focused; use PHASE1_MANUAL_CHECKLIST.md for hands-on testing. The broader review/bug-fix pass follows the Phase 1 development handoff.

## Owner vision review and authorized Phase 2 start — 23 October 2026

Assessment: 8/10 as a business product concept. Workflow relevance and collaboration are strong; delivery risk comes from scope, vague statuses, approval shortcuts and provider dependencies. This is a judgment of the concept, not a market validation score. Differentiate through accurate commitments, clear next actions and easy repeat work. Establish actual conversion, turnaround and on-time delivery baselines before claiming growth.

Keep: one customer/order workspace, versioned proofs, negotiated dates, customer conversation, production/inventory links, payment records and consent-controlled portfolio. Improve: separate commercial/design/production/payment/fulfilment states; customer requested vs proposed vs accepted dates; line-level partial completion; exact-version acceptance and visible responsibility. Add later: repeat-order drafts, release-readiness checks, change requests, delay reasons, lost-enquiry reasons and owner exceptions. Defer automatic social posting, unrestricted mailbox sync, AI pricing, advanced scheduling and multi-company SaaS. Remove silent approval bypasses, automatic print release after a proof click, one catch-all In progress status and mandatory online payment for every customer. Offline confirmation must identify the recording staff member and evidence, never impersonate a customer.

Owner has authorized continued development. Begin Phase 2A while existing production/provider/manual release gates remain open. This does not authorize deployment or real customer messaging. Native Java 21 / Gradle / PostgreSQL / Next.js remains mandatory; no Docker/Maven workflow.

### Phase 2A.1 — quotation versions and customer acceptance (first implementation)

Staff manually enter an all-inclusive INR total, scope, terms, proposed delivery date, and quote-valid-until date. Optional customer-requested date is explicitly different from the proposed commitment. Issuing creates an immutable numbered version and specification snapshot; a stale version counter cannot overwrite a newer offer. Only an owner or assigned salesperson can issue; another salesperson cannot access assigned work. A linked authenticated customer sees quote history and may accept the exact current unexpired version or request changes. Guest enquiries must be securely claimed before portal acceptance. Accepted versions cannot be silently replaced. Acceptance records the proposed date as the agreed commitment; it does not create an invoice, take payment, generate a production order or release a machine job. No notifications are claimed until configured.

Acceptance checks: immutable commercial fields; stale issue/accept conflicts; expired quote rejection; repeat acceptance safe; cross-customer isolation; role/assignment enforcement; server-validated amounts and dates; CSRF/session/MFA protection; usable staff and customer screens. Tests stay focused on this slice.

### Subsequent slices

2A.2: editable drafts, richer item pricing/tax presentation, negotiated changes to accepted commitments, documents and controlled quote withdrawal. Confirm actual accounting/tax rules before invoice implementation.
2B: private scanned files, designer revision history, anchored comments and exact proof sign-off; recorded offline confirmation; readiness checklist.
3: accepted quote to order lines, inventory and operation queues, QC/rework, manual payment allocation, partial dispatch/collection and delivery evidence.
4: configured Gmail inbound/outbound conversations, meaningful notifications, hosted payments and reconciliation. Basic About/approved portfolio content may ship independently earlier.
Later: repeat-order campaigns, consent-controlled publishing, capacity/costing assistance and AI suggestions evaluated for measured benefit.

Open policy choices remain: guest versus account-required submission; actual advance/credit terms; authority for offline/exception approvals; delivery vs dispatch promise; in-house vs outsourced tasks. Phase 2A.1 preserves guest intake, uses no payment assumptions and labels its proposed date as delivery-at-customer / collection date. See PRODUCT_VISION_FROM_OWNER_NOTES.md.

Phase 2A.1 is now implemented as a development candidate. Actual checks and pending work: PHASE2_DEVELOPMENT_STATUS.md. This does not mark Phase 2 complete.

## Phase 2B.1 — private files and exact proof approval (24 October 2026)
Implemented as a development candidate: private PDF/PNG/JPEG reference and design uploads; native malware-scanner adapter with fail-closed quarantine; immutable numbered proof revisions; comments attached to the current revision and optional page/side; customer acknowledgement and exact-file/quote approval or change request; owner designer assignment and scoped designer queue. Sharing a new revision invalidates effective readiness while retaining previous immutable decisions. There is no automatic production release or staff impersonation of customer approval.

Native follow-up: the earlier exit 2 / 0x80508023 came from a malformed synthetic PDF. Valid PNG/PDF artwork now clears the real Java/Defender adapter; nonzero results still quarantine files. Setup and repeatable checks: DESIGN_PROOF_SETUP.md and NATIVE_ARTWORK_VERIFICATION.md. Native Gradle remains mandatory; no Docker.

Private raster rendering and visual pin annotations are implemented. Pending: PDF rendering/editing, comment resolution, recorded offline proof approvals, designer deadlines, retention/erasure execution, order-line linkage and production release/material/payment/scheduling checks. The phase-wise actionable list is REMAINING_FEATURES.md. These are not claimed complete by the first proof slice.

## Owner workflow corrections — 24 October 2026
Quotation submission is the only staff route to QUOTED; exact customer acceptance or evidence-backed PHONE/EMAIL acceptance recorded by staff moves work to IN_PROGRESS. Staff cannot manually select these statuses. Quotation expiry is removed; historical validity values are retained but no longer enforce expiry. Customer-requested delivery date is captured at intake and preserved in the quotation; otherwise staff may enter it, defaulting to today in Asia/Kolkata. Dates cross the API as date-only strings.

Accepted quotations appear in Live works with a dedicated customer page for current stages, public logs, assigned designer public name/business contact, portal conversation and exact-proof review. Image review pins request changes from the designer; source artwork and approvals are not overwritten. Actual mailbox sync, vector/PDF editing, machine release, payment and delivery events remain separate future capabilities and must not be simulated.

Staff/owner may reject or remove an enquiry with a reviewed meaningful customer-visible explanation. Removal archives rather than physically erasing approvals. Closed records sort last with matching cursor order. Profile, sanitized profile photos, sign-in connections and privacy controls move into the account popup. Customer header quote shortcut and avatar chevron are removed; logged-out staff/owner entry remains.

24 October local Gmail setup preparation: native DEV permits exact loopback HTTP only with explicit recipient restriction and a send-after activation boundary; historical and non-allowed mail is held. Production keeps HTTPS. Enable-LocalGmail.ps1 prompts privately, sends one setup test, saves settings on SMTP acceptance and restarts native services. Temporary mailbox is selected; private credential and real delivery verification remain pending. See GMAIL_LOCAL_SETUP.md.

Gmail activation update: system SPRING_MAIL_USERNAME / SPRING_MAIL_PASSWORD are now supported (SPRING_MAIL_PASSWROD compatibility alias). Google SMTP accepted the setup test and local mail is enabled for the selected inbox only, with historical queue hold. Actual inbox receipt/UI flow remains user validation. Proof/production/delivery milestone notifications remain planned. GMAIL_LOCAL_SETUP.md supersedes the earlier pending-credential note.

24 October lifecycle-mail continuation: V13 adds one-time customer setup with quotation, lifecycle customer/team notifications, owner Email activity, proof-gated manually confirmed production/QC/dispatch/delivery milestones, delivery issue holds and durable 48-hour completion. This supersedes earlier “milestone emails/manual operations/timer not implemented” notes only for this bounded slice. Photos/GPS, stock/payment production controls, inbound Gmail sync and multi-page artwork viewer remain pending. Scope, APIs and verification: WORKFLOW_EMAIL_VERIFICATION.md.

24 October signup correction: V14 repairs repeated unverified signup/resend, binds the latest chosen password to verification, and creates guest access at intake acceptance. Customer-only email carries a 24-hour temporary password with forced activation/change and privacy consent; existing passwords are retained. New mail is allowed to any real address by explicit owner request; held/synthetic mail is not replayed. This supersedes earlier recipient-only/link-only account policy. See CUSTOMER_SIGNUP_CORRECTION.md.

24 October full native UI/mail verification: guest intake → temporary credential activation → quotation acceptance → assigned designer → real Defender scans → pinned comments/change request → proof v2 approval → production/QC → dispatch/delivery → accelerated synthetic 48-hour completion passed. All 25 generated customer/team notifications reached SMTP SENT on first attempt; owner Email activity and customer Completed UI were verified. Four focused Gradle workflow tests passed; no Docker. Real timestamps/orders were preserved and test accounts/records cleaned. Exact evidence, safe mail routing and scope limits: UI_END_TO_END_VERIFICATION.md.

24 October quotation email enhancement: V15 binds outbox QUOTE messages to the immutable issued quotation. Native dispatch verifies its PDF hash and attaches that exact version; branded escaped HTML/plain alternatives include the customer workspace link, sender name and short product/reference subjects with existing thread headers. No API route or approval gate changes. Details and focused evidence: QUOTATION_EMAILS.md.

24 October owner flow refinement: guest/registered × online/phone/email entry paths converge on one order. Notification checkpoints, configurable deposit/credit/dispatch balance policy, invoice timing reference, safe sample publication, automatic designer assignment and intake-role reconciliation are specified in ORDER_ENTRY_AND_PAYMENT_FLOWS.md. Those later features remain planned; multipage PDF review and the local test designer are the active slice.

24 October PDF review slice: native resource-limited PDF inspection and authenticated local-worker multipage canvas preview, per-page location comments, full-document page acknowledgements tied to immutable revision/hash/quote, reference/file Preview dialogs and a persistent least-privilege local test designer are implemented. This supersedes earlier multipage PDF viewer pending notes only for PDF/image preview/comment review; source editing, advanced preflight, gallery reuse, delivery photos/GPS and payment/WhatsApp integration remain separate. API, migration, role and focused evidence: PDF_ARTWORK_REVIEW.md. Owner-proposed entry/payment/notification paths: ORDER_ENTRY_AND_PAYMENT_FLOWS.md.

## 24 October owner decision: two email threads
Intake receipt through acceptance/account access uses “Enquiry received”; first quotation through final delivery uses “Your print order”. Subjects keep the short product name and PF reference; action/version details belong inside each email. Roots/replies are isolated per recipient, and existing sent threads remain unchanged. See INTEGRATIONS.md and WORKFLOW_EMAIL_VERIFICATION.md for behavior and verification.

## Remaining customer browser comments
The artwork-request stage now supports fictional category sample selection, normally scanned references, page/image pinned review requests and annotated-copy emails to the customer and assigned team. Customer proofs wait for quotation acceptance. The live-work page uses a desktop work/conversation grid with ordered stages and outgoing email history. External mailbox reply import remains unconnected. See CUSTOMER_REVIEW_WORKSPACE.md for exact gates, privacy, limits and focused native checks.


### PF-320 owner-authorized finance slice (24 October)
Manual verified advance/balance payments, owner-configured production/dispatch gates, immutable commercial invoice PDF/email and Razorpay test-only checkout are implemented for local review. No live gateway or statutory GST tax-invoice claim. See PAYMENTS_AND_INVOICES.md for API, setup, security and focused checks.
