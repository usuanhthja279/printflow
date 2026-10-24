# PrintFlow Architecture — v0.1

## Guiding decision

### Implemented staff slice (23 October 2026)

Default local execution now uses native PostgreSQL 17, Java 21/Gradle and Node; Docker is optional packaging. The identity module uses Spring Security session authentication, BCrypt(12), CSRF, session fixation protection and bounded login throttling. Native bootstrap creates a SALES account only, is dev/QA gated and never resets existing accounts. Disabled users and role changes invalidate their sessions on the next request. No OWNER/admin access is implemented without the later MFA/account-management work.

Staff enquiry mutations require SALES at the HTTP boundary and owner/pool checks in the application service. Reads show only the caller's assignments and shared unassigned leads; other assigned records return 404. Mutations take a pessimistic row lock and compare an optimistic revision. Staff follow-up/assignment events are appended via JdbcTemplate in the same JPA transaction; public status events remain separate projections. Activity edit/delete APIs are absent. One company is supported; organization/tenant membership is not claimed.

Session state and login rate windows are in memory for the single native API process. Restart revokes sessions. PostgreSQL holds user password hashes, lead assignments, follow-up plans and append-only-through-API activity. This is a tested first staff slice, not completion of the full production identity/privacy design.

Use a **modular monolith**, not microservices. A single Spring Boot JVM hosts business modules and one PostgreSQL transaction boundary. Background workers and PDF preflight are separate processes when introduced. A single Next.js frontend presents customer, business, operator and partner routes with backend-enforced authorization (Phase 1).

### Phase 0 actual implementation

- Next.js customer homepage, validated enquiry page, token tracking, development-only owner prototype.
- Next server rewrites same-origin `/api/*` to Spring Boot; browsers never need to call Docker `api:8080` directly.
- Spring Boot `catalog` and `enquiry` domain, JPA repository, Flyway V1 migration, tracking token SHA-256 digest, status events.
- PostgreSQL persistent Docker volume; `/actuator/health` checks service readiness.
- No production auth yet. In production profile, `/api/demo/*` is not mapped. Public enquiry writes disabled by default.

### Target components

| Module | Responsibility | Business invariant |
|---|---|---|
| Identity | Customer login, employee SSO/local login, RBAC, vendor invitations | Deny by default; ownership checks |
| CRM | Contacts, leads, communication, campaigns | Deduplicate conservatively; retain source provenance |
| Catalogue | Printed products, configurable options, templates | Product validity and compatible specifications |
| Estimation | Price rules, materials, machine costs, quotes | Accepted quote versions are immutable |
| Order | Commercial order, item lines, milestones, history | State machine guarded against invalid jumps |
| Artwork | Uploads, preflight, annotations, proof approval | Approval ties to immutable file checksum/version |
| Production | Jobs, routing, operations, schedules, QC, job tickets | Jobs release only after authorised preconditions |
| Inventory | Stock, reservations, issue, waste | No negative reserved available stock without override |
| Procurement | Supplier orders, receipt, subcontracting | Vendor scope cannot expose confidential sales data |
| Finance | Payments, invoices, credit notes, ledgers | Exactly-once financial effect via idempotency keys |
| Fulfillment | Packing, delivery, pickup, proof | Partial deliveries preserve open quantity |
| Collaboration | Tasks, messages, mentions, comments, attachments | Explicit audience: customer/internal/vendor |
| Integrations | OAuth, webhooks, outbox, retries, channel normalization | Provider failure never loses business event |
| Reporting | Revenue, margin, utilization, overdue jobs | Snapshot consistency and explainable metrics |

### Architecture rules

1. Backend owns domain validation and access decisions. Client validation is additional UX only.
2. Controllers -> application services -> repository/domain, never direct repositories from controllers.
3. All new schema changes via Flyway; JPA `ddl-auto=validate`.
4. Store money as `BigDecimal` with currency and explicit rounding; never `double`.
5. Store timestamps in UTC, display using configured business timezone (`Asia/Kolkata` default); statuses as versioned defined enums.
6. Preserve quote, artwork and order versions; create event records on important transitions.
7. Connectors parse raw webhooks into typed internal events with idempotency keys and signature verification.
8. External endpoints have strict allowlists, rate limits, validation and threat controls; upload processing isolated.
9. Use UUIDs internally, opaque references externally; no enumeration-based public lookups.
10. Consent, data retention and deleted-account workflow must be defined before live operation.

### Core model

Customer -> Enquiry -> Quote -> QuoteVersion -> Order -> OrderItem -> ProductionJob -> ProductionOperation -> QC -> Shipment.

ArtworkVersion and ProofApproval attach to OrderItem (with quotation specification checksum) rather than floating independently. ProductionJob can split into multiple jobs, with internal or outsourced operations. Payment and Invoice belong to commercial Order, not solely one ProductionJob. Subcontract packages are redacted views of specific ProductionOperations.

### Lifecycle invariants

- Quote acceptance approves one specific quote version and amount.
- Proof acceptance approves exact artwork object version, checksum, and specifications.
- Job release is blocked until approval, payment/credit condition and material-readiness gates pass (configurable for operator override).
- Internal notes and costs never appear in customer/vendor serializers.
- No financial event is considered paid solely because a browser reached a success URL.

## Threat model (priority)

- Broken Object Level Authorization (guessing IDs to access another customer's order).
- Malicious SVG/PDF/ZIP upload, malware, decompression bombs and overlarge media.
- Spoofed webhook and repeated payment notifications.
- Business-email compromise and cross-channel identity mismatch.
- Privilege escalation through invited external vendor membership.
- Missing audit on quote/proof/price changes.
- Data export via reports and log leakage of names, phone numbers or tokens.

Phase 0 mitigates some demonstration risks (opaque tracking token digest, dev-gated owner route), but **is not a production security baseline**. Phase 1 must implement full identity, authorization, anti-abuse, secure storage, privacy controls and deployment hardening.

## Scaling plan

Phase 1-3: native web/API services, private PostgreSQL and object storage as introduced, simple outbox jobs; metrics. Phase 4-5: move outbound delivery/retries to worker pool and external queue if throughput merits it. Phase 6-7: PDF processor isolated with memory/time limits and CPU autoscaling. Avoid distributed transactions: persistent outbox plus idempotent consumers.

## Decision references

- [ADR-001](ADR/ADR-001-modular-monolith.md)
- [ADR-002](ADR/ADR-002-security-boundary.md)
- [ADR-003](ADR/ADR-003-communication-outbox.md)

## Owner identity extension (V3)

Owner business access is a two-stage session: password authentication, then mandatory authenticator/recovery verification. Enrollment state lives briefly in the HTTP session; persisted seeds use AES-256-GCM with random IVs and a private environment key. Recovery codes are random 128-bit values stored as SHA-256 digests. PostgreSQL row locks serialize MFA replay-counter consumption and persistent attempt windows. Sessions remain in memory, so restart signs users out.

A second request filter checks database account state and auth version against the principal before accessing private routes. Password-reset/change, role changes and disable operations revoke stale principals. Administration requires recent owner verification; self-access mutation is prohibited and last-owner checks use a transaction advisory lock. Security/lead audit records reject normal UPDATE/DELETE, while database administrators remain privileged.

The separate dev/QA owner bootstrap creates a first enabled owner only when configured and no enabled owner exists. It never silently promotes the existing sales account or re-enables a disabled account. No bootstrap bean is loaded in prod. Native setup generates a stable private MFA key and separate credentials in ignored `.env`. Production provisioning, tested lockout recovery, distributed sessions/login counters and customer identity remain pending.

## Phase 1 customer/CRM/catalogue extension

V4 adds customer profiles, hashed identity tokens, encrypted mail outbox, persistent request quotas, staff-owned CRM contacts with audited merge/link operations, privacy requests and versioned product configuration. No applied migration was rewritten. Customer credentials reuse the session/principal revocation boundary with role CUSTOMER; staff administration excludes those accounts. Customer queries constrain customer_user_id on the server. Guest claiming requires verified-email match plus token proof.

SMTP is a disabled-by-default adapter with bounded timeouts/retries and expiring encrypted payloads. Development preview is owner-MFA protected, fictional-recipient-only and unavailable in prod. Identity URLs use fragments removed by the customer page after capture. Email provider credentials and a separate stable mail encryption key remain private environment configuration.

Product template versions are snapshotted into enquiries before returning creation results. Owner edits append audit snapshots and never mutate submitted specifications. CRM merge preserves a selected survivor and moves links under ordered locks; it is never automatic. List cursors use stable created_at/id ordering. Privacy decisions are reviewed workflow records, not an unreviewed deletion operation.

A request-body filter bounds API payloads; persistent quotas and honeypot protect native guest intake. Production also requires server-side Turnstile validation and approved privacy configuration. Trusted client-address/proxy setup, provider connectivity, native QA and production recovery remain release work. Artwork admission stays quarantined behind a storage interface with no exposed upload/download implementation until Phase 2.

## Customer external identities

V5 adds stable external identity mappings and a verification browser-binding digest. Spring Security OIDC validates provider responses; local account mapping permits CUSTOMER only, rejects automatic email linking and requires password-confirmed explicit linking. Untrusted provider email needs an application email proof in the initiating session. OAuth requires explicitly configured SameSite=Lax cookies for cross-site callbacks; CSRF protection remains active. Default development remains Strict with providers disabled. Native Gradle/PostgreSQL operation is unchanged.

## Phase 2A.1 quotation module
V6 adds `quote_versions` and append-only `quote_events`. `Quotations` uses JdbcTemplate transactions with an enquiry-row lock to serialize issue and customer response. Unique version and accepted-version indexes plus immutable-content triggers preserve the exact accepted offer. Staff visibility follows enquiry assignment; customer access requires the linked customer ID. Existing HTTP role, CSRF, current-session and owner-MFA guards apply. Quotation state is independent of the legacy enquiry status: acceptance does not silently mark production or create an order. All-inclusive manual pricing is deliberately bounded before item pricing/tax/invoice rules.

## Phase 2B.1 design proof module
V7 adds immutable proof revisions/decisions/comments/events and immutable artifact identity/hash metadata with mutable scan state. DesignProofs owns enquiry ACLs, row-lock serialization, per-actor quotas and cross-resource binding. ArtifactStorage streams bounded raw uploads to UUID objects outside public directories, restricts service-account permissions and checks hashes through an open no-follow file channel before clean reads. NativeMalwareScanner executes a fixed native scanner argument list without a shell; at most two scans, 45-second timeout, no scanner output/customer filename logging. Errors remain quarantined. Storage/database rollback removes a newly staged object; committed history is retained. No file parser or inline preview executes in the web process.

Downloads authorize each request using current account/enquiry scope and stream only verified clean objects. There is no bearer object URL. Scanning is currently synchronous and can hold the enquiry lock/DB connection; deploy with upload/connection timeouts and bounded capacity; a durable isolated scanner queue remains a later scaling step. File bytes need private volume/backup protection; application-level at-rest encryption and restore/retention validation are not implemented here.

## Live-work/account refinement
V8 adds requested/closure data, recorded acceptance channel/evidence/actor, private sanitized avatar data/public display name, normalized proof-comment anchors and append-only public work_messages/work_events. It replaces the quotation trigger function to permit acceptance metadata only during the one-way ISSUED transition. Accepted quotation/enquiry changes serialize through the existing enquiry lock. New quote validity is null; historical values remain untouched and expiry enforcement is removed per owner instruction.

Live works are accepted-quotation projects, not manufacturing orders or automatically released print jobs. Public logs combine explicit work events and proof decisions; internal follow-up logs are not copied. Shared portal conversation is kept distinct from future email ingestion. Designer exposure uses a separately editable public_name and optional public business contacts, not staff login addresses. Raster proofs use authenticated clean/integrity-checked image responses; PDF rendering/vector editing remain out of scope.

Account photos use a separate small-image transformation boundary: browser crops a thumbnail, server validates dimensions/type/size, ignores metadata and re-encodes a fresh PNG into private account storage. This never clears an artwork quarantine or permits arbitrary uploaded files. Generated avatars are local SVG geometry; no external avatar service is contacted.
# Phase 2 completion architecture — 24 October 2026

V9 adds mutable optimistic quote drafts; immutable item/document/amendment identity on quote versions; transactional immutable commercial orders/lines and append-only clean artwork linkage; designer tasks; immutable comment resolutions; and explicit offline proof source/evidence. V10 adds owner-versioned controlled print vocabulary and immutable configuration audit. Existing applied migrations remain unchanged.

Quote issue validates line quantities/rates/tax totals, captures specifications and generates a local text-only PDF with Apache PDFBox 3.0.8 ([official release](https://pdfbox.apache.org/download.cgi)). PDF bytes and SHA-256 are immutable database content; downloads authenticate and recheck the hash. No HTML, network resources or customer PDF parsing is involved in quotation generation. Supported English-first text is font-checked; unsupported glyphs abort issuing rather than silently altering content. Uploaded artwork still follows the separate native AV/private-storage boundary.

An enquiry row lock serializes draft save/issue, acceptance/amendment replacement, order conversion, task writes and proof decisions. Old accepted evidence is immutable when marked REPLACED; new acceptance changes the effective agreement. Proof readiness/decision guards require the current accepted quote, clean exact hash and latest revision. Repeat/concurrent conversion returns the same order for that accepted quote. Amended agreements create distinct historical commercial order versions; manufacturing/payment/inventory operations remain Phase 3.

Customer projections omit private drafts/tasks/assignments and unpublished designs. Designer access remains assigned-enquiry only, without quotation prices/order administration or authority to record offline customer approval. Owner/assigned sales may record offline proof agreement only with a clean shared evidence object, exact quote/hash acknowledgement and immutable source/actor/time. No silent approval or production release is introduced.


## 24 October least-privilege intake/stages

V11 adds Contacts capability and authentication-version revocation, row-locked intake decisions, assigned specialist tasks/events and deduplicated encrypted rejection outbox payloads (guest notifications may have no user id). Authorization applies at controller/service level, not just navigation. Sales scope excludes NEW and unassigned work; designer intake/assigned-work scope differs from commercial management. Specialist task projections omit unrelated customer/commercial data. Intake decision activities and work events preserve audit context. Queue dismissal archives rather than deleting agreements/history.

Current-stage rendering derives agreement/proof readiness and hides irrelevant forms; it does not create manufacturing state. Formal production release, delivery evidence/GPS and durable completion scheduling remain separate planned services described in FULFILMENT_WORKFLOW.md.

V12 initial-artwork admission uses the existing hash-scoped intake token for upload only, no guest read permission, a 24-hour NEW-only window and native scanner/private store rollback. Nullable attribution means an unauthenticated intake submitter; it is never represented as a verified customer or staff member. Outbox conversation metadata is stable/deduplicated and encrypted payloads retain expiry/retry behaviour. MIME delivery adds thread headers; no external provider integration is claimed. Bulk archival is transactional and retains immutable commercial/design history.

24 October lifecycle-mail continuation: V13 adds one-time customer setup with quotation, lifecycle customer/team notifications, owner Email activity, proof-gated manually confirmed production/QC/dispatch/delivery milestones, delivery issue holds and durable 48-hour completion. This supersedes earlier “milestone emails/manual operations/timer not implemented” notes only for this bounded slice. Photos/GPS, stock/payment production controls, inbound Gmail sync and multi-page artwork viewer remain pending. Scope, APIs and verification: WORKFLOW_EMAIL_VERIFICATION.md.

24 October signup correction: V14 repairs repeated unverified signup/resend, binds the latest chosen password to verification, and creates guest access at intake acceptance. Customer-only email carries a 24-hour temporary password with forced activation/change and privacy consent; existing passwords are retained. New mail is allowed to any real address by explicit owner request; held/synthetic mail is not replayed. This supersedes earlier recipient-only/link-only account policy. See CUSTOMER_SIGNUP_CORRECTION.md.

24 October quotation email enhancement: V15 binds outbox QUOTE messages to the immutable issued quotation. Native dispatch verifies its PDF hash and attaches that exact version; branded escaped HTML/plain alternatives include the customer workspace link, sender name and short product/reference subjects with existing thread headers. No API route or approval gate changes. Details and focused evidence: QUOTATION_EMAILS.md.

24 October PDF review slice: native resource-limited PDF inspection and authenticated local-worker multipage canvas preview, per-page location comments, full-document page acknowledgements tied to immutable revision/hash/quote, reference/file Preview dialogs and a persistent least-privilege local test designer are implemented. This supersedes earlier multipage PDF viewer pending notes only for PDF/image preview/comment review; source editing, advanced preflight, gallery reuse, delivery photos/GPS and payment/WhatsApp integration remain separate. API, migration, role and focused evidence: PDF_ARTWORK_REVIEW.md. Owner-proposed entry/payment/notification paths: ORDER_ENTRY_AND_PAYMENT_FLOWS.md.

## Email phase threading (24 October)

V18 adds mail_outbox.thread_phase and thread_root_id. CustomerMail serializes queueing on the enquiry row and scopes prior-message lookup to enquiry, normalized recipient and phase. Quotation PDF integrity/version binding is unchanged. Existing rows remain LEGACY; only new messages participate in the two-phase policy.

## Customer reference review workspace
V19 binds immutable reference feedback and generated PDF hashes to the source artifact and enquiry. A bounded child JVM creates the copy, avoiding untrusted document parsing in the API process. Email copy lookup binds both feedback ID and enquiry; all existing proof/quotation immutability gates remain. Customer work layout separates the active work column and shared conversation/email metadata sidebar. See CUSTOMER_REVIEW_WORKSPACE.md.


## PF-320 payments and invoices — 24 October 2026
Owner-configured advance/credit rules, immutable verified receipts/corrections, commercial invoice PDF preview/email and server-side production/dispatch gates replace the earlier Not recorded/payment-unimplemented placeholder. Customer and assigned sales are read-only; owner confirms manual finances. Optional Razorpay TEST checkout uses server-owned amounts, HMAC plus captured-payment API verification and deduplicated callbacks/webhooks. Live gateway payments and GST tax invoicing are not enabled. Routes, schema, screen behavior, security boundaries and focused verification: PAYMENTS_AND_INVOICES.md.


24 October visual experience refresh: public hero/catalogue/product-finish interactions, live quote brief, scoped customer/owner overview cards, role navigation and work/payment visual hierarchy are implemented without replacing business state. See UI_EXPERIENCE_REDESIGN.md; screenshot/test evidence is packaged separately.

## 24 October operations continuation

Owner-authorized automatic designer routing, project-level production queues and private delivery photo evidence are implemented. PF-300/PF-330 now include printing/finishing/QC/rework/hold progression, scoped printer assignment and pickup/transit/delivery evidence. Existing proof, quotation, finance and dispatch gates remain. See OPERATIONS_WORKFLOW.md for APIs, migrations V24/V25, role scopes, upload limits, notifications and the focused Gradle/browser commands. Full shop-floor routing, inventory, capacity reservations, GPS and partial dispatch remain separate.
