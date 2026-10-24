# Phase 0 API contract

Base URL: browser uses same-origin `/api`. Next.js rewrites to the Java service. Body/response JSON UTF-8. ISO 8601 instants are UTC. Errors use a simple `{code,message,timestamp}` response where the custom handler applies. Phase 1 will standardize RFC 9457 Problem Details / OpenAPI.

## GET `/api/public/catalog`
Returns `[{"code":"WEDDING_CARDS","name":"Shaadi & Wedding Cards","description":"..."}, ...]`. Owner-confirmed service codes: `WEDDING_CARDS`, `HOTEL_PRINT`, `BROCHURES` (displayed as Pamphlets & Leaflets), `OFFSET_GLOSS`. Existing `CARDS`, `BOOKS`, `PACKAGING`, `CUSTOM` codes are retained for compatibility; their availability still needs owner confirmation.

## POST `/api/public/enquiries`

**Only enabled when `APP_PUBLIC_ENQUIRIES_ENABLED=true`** (local dev). `201 Created` body:

```json
{
  "publicId": "e69c161a-a0d3-4daa-9c24-947e40da81a1",
  "trackingToken": "example-only-random-opaque-token-not-a-real-secret",
  "status": "NEW",
  "createdAt": "2026-10-23T10:20:00Z"
}
```

Example request:

```json
{
 "customerName":"Example Customer","email":"hello@example.com","phone":"9876543210",
 "productCode":"BROCHURES","quantity":5000,"size":"A4","paperGsm":170,
 "printSides":"DOUBLE","finish":"MATTE","notes":"Need next week"
}
```

Validation: name <=140 and required; email RFC basic, max 254; phone 7–32 allowed punctuation; known product; quantity 1–10,000,000; paperGsm optional 40–600; additional strings length restricted. `400` validation error. `503` when production intake feature disabled. The token is displayed once and stored server-side only as SHA-256 digest.

## GET `/api/public/enquiries/{publicId}`

Requires `X-Tracking-Token: <opaque token>` header. Wrong token or unknown ID -> `404`. Returns:

```json
{
 "publicId":"e69c161a-a0d3-4daa-9c24-947e40da81a1",
 "productCode":"BROCHURES","quantity":5000,"status":"REVIEWING",
 "createdAt":"2026-10-23T10:20:00Z",
 "events":[
   {"status":"NEW","at":"2026-10-23T10:20:00Z"},
   {"status":"REVIEWING","at":"2026-10-23T10:24:00Z"}
 ]
}
```

No customer email, phone, notes or staff identity are returned via public tracking.

## GET `/api/demo/enquiries`

Development/QA only, requires `X-Demo-Key`. Lists up to 100 most recent `DemoEnquiry` objects, including private data **for local fictional data only**. Incorrect/missing key -> `401`. Route is not registered when `SPRING_PROFILES_ACTIVE=prod`.

## PATCH `/api/demo/enquiries/{id}/status`

Development/QA only, same key. JSON: `{ "status": "REVIEWING" }`. Valid status transitions: `NEW` -> `REVIEWING`, `QUOTED`, `CLOSED`; `REVIEWING` -> `QUOTED`, `CLOSED`; `QUOTED` -> `CLOSED`. Closed is final. Same-state update is idempotent. Transition advances event history transactionally.

## GET `/actuator/health`
Spring Actuator health (`UP` once DB migrations and service are ready). No internal health details exposed publicly.

## Future API design rules
Use `/api/v1/...` with OpenAPI, stable semantic DTOs, pagination/filtering/sorting, idempotency keys for money/creation, `409` for invalid state, 403 for lack of permission vs 404 resource confidentiality when needed, audit correlation IDs and trace headers.

## Staff slice — session authentication and follow-ups

Implemented locally after the verified native Phase 0 gate. OWNER and SALES are supported CRM roles; other roles cannot access enquiries. Owner MFA/account management is described below; customer accounts remain pending. Separate local SALES and first-owner bootstraps run only in dev/QA and never overwrite credentials. Demo endpoints remain a legacy dev/QA test facility and are disabled by default.

- `GET /api/v1/auth/csrf` — public, creates an anonymous session as needed; returns `{token,headerName}` with no-store. Use the returned token/header on login and all authenticated mutations.
- `POST /api/v1/auth/session` — form URL-encoded `username` and `password`, CSRF header required. Returns 200 `{code:"SIGNED_IN"}` or generic 401; missing/invalid CSRF gives 403. Session identifier rotates on success. Attempts are limited per normalized username and source IP; 429 includes Retry-After. Fetch a fresh CSRF token after signing in.
- `GET /api/v1/auth/session` — authenticated `{id,username,role,mustChangePassword,mfaEnrolled,mfaRequired,mfaRecent}`; never password hashes. Anonymous/expired/disabled accounts return 401. Disabled accounts or changed roles invalidate existing sessions on their next request.
- `POST /api/v1/auth/logout` — CSRF required; invalidates the session and removes its cookie. Returns 200 `{code:"SIGNED_OUT"}`.
- `GET /api/v1/staff/enquiries` — SALES sees latest 100 leads assigned to the caller or unassigned; MFA-verified OWNER sees latest 100 across assignments. Other staff members' assigned records are excluded.
- `POST /api/v1/staff/enquiries` — SALES or MFA-verified OWNER + CSRF. Body `{enquiry: <CreateEnquiry>, source: "WALK_IN"}`. Sources: WEBSITE, WALK_IN, PHONE, EMAIL, WHATSAPP, SOCIAL, REFERRAL, OTHER. These are manual labels, not connected providers. Returns 201 `{lead,trackingToken}`, assigned to the caller. Existing contact/spec validation applies, including required email and phone. Token is displayed once and not stored in plaintext.
- `PATCH /api/v1/staff/enquiries/{id}/assignment` — `{revision: <current>, claim: true|false}`. Claim unassigned records, or return own records to the pool. An assigned record belonging to someone else returns 404. A stale revision returns 409.
- `PATCH /api/v1/staff/enquiries/{id}/follow-up` — `{revision,priority:"LOW"|"NORMAL"|"HIGH",nextAction,followUpAt}`. Caller must own the lead. Next action is at most 500 characters; action and ISO instant must both be supplied or both cleared. Past times are allowed to record overdue work. Closed records cannot be edited. Internal previous/new plans are appended to activity history transactionally.
- `PATCH /api/v1/staff/enquiries/{id}/status` — `{revision,status}`. Caller must own the lead; existing forward-only enquiry transitions apply. Closing clears the follow-up plan. Status events update public tracking, while private activity details stay internal.
- `GET /api/v1/staff/enquiries/{id}/activities` — SALES or MFA-verified OWNER, same visibility rules; returns immutable-through-API `{id,actorId,action,details,createdAt}` entries. No edit/delete route exists.

Staff LeadView contains id, publicId, customerName, email, phone, productCode, quantity, notes, status, source, priority, assignedStaffId, nextAction, followUpAt, revision and createdAt. Public TrackingView remains unchanged and excludes assignment, follow-up and internal history. Resource checks and revision comparison run under a database row lock; JPA versions are advanced before returning the response.

Session cookies are HttpOnly and SameSite=Strict, with a 30-minute idle timeout. Secure defaults true; only the ignored native development configuration turns it off for loopback HTTP. Sessions and password-login rate counters are single-instance/in-memory and do not persist across API restarts. Owner MFA attempt counters persist in PostgreSQL. Customer identity, shared password-login limits, privacy/anti-abuse and production operations remain incomplete.

## Owner access and staff administration — V3

All mutations below require a fresh session CSRF token. OWNER business APIs require MFA within 30 minutes; staff administration mutations additionally require verification within five minutes. `mustChangePassword` restricts the account to auth endpoints until changed. Role, enabled state, password hash, forced-change state and auth version are checked on each authenticated request; changes invalidate stale sessions.

- `POST /api/v1/auth/mfa/enroll` — OWNER only, not already enrolled; returns private `{secret,uri}` with no-store. Pending setup is bound to this session and expires in five minutes. Requires a configured 32-byte base64 `APP_MFA_ENCRYPTION_KEY` or returns 503.
- `POST /api/v1/auth/mfa/confirm` — `{code}` six-digit authenticator code; stores encrypted seed and returns eight recovery codes once. Save privately; no later retrieval endpoint.
- `POST /api/v1/auth/mfa/verify` — `{code}` current authenticator code or single-use recovery code. TOTP uses 30-second steps and a +/-1-step clock window; accepted steps cannot be replayed across sessions. Six MFA attempts per five-minute database window, then 429. Success marks only the current session verified.
- `POST /api/v1/auth/password` — `{currentPassword,newPassword}`; at least 14 characters and at most 72 UTF-8 bytes; must differ from current password. OWNER must pass MFA first. Success invalidates the session; sign in again. Password-change attempts are bounded.
- `GET /api/v1/owner/staff` — up to 500 account summaries `{id,username,role,enabled,mustChangePassword,mfaEnrolled}`; excludes passwords, seed and recovery material.
- `POST /api/v1/owner/staff` — `{username,role,temporaryPassword}`; creates an individual account with forced password change. Existing normalized username returns 409. No password returned.
- `PATCH /api/v1/owner/staff/{id}/access` — `{role,enabled}`; increments auth version. Self changes fail 409; at least one enabled owner is preserved under an administration lock.
- `POST /api/v1/owner/staff/{id}/password` — `{password}`; resets another account, forces change and invalidates old sessions. Own password uses the auth endpoint instead.
- `GET /api/v1/owner/activity` — latest 100 security audit entries `{id,actorId,targetId,action,createdAt}`. Passwords/codes are excluded. Normal UPDATE/DELETE operations are blocked by database triggers; this is not tamper-proof against a database administrator.
- `PATCH /api/v1/owner/enquiries/{id}/assignment` — `{revision,staffId}`; nullable staffId returns to pool. Only enabled OWNER/SALES accounts can receive a lead. Stale revision returns 409. Changes append actor-attributed activity. Owner oversight does not remove the explicit-assignment requirement for follow-up/status editing.

Roles registered by V3: OWNER, SALES, ESTIMATOR, DESIGNER, SUPERVISOR, OPERATOR, INVENTORY, ACCOUNTS, DELIVERY. Only OWNER/SALES business modules are implemented here. New OWNER accounts must change their temporary password after MFA enrollment. Bootstrap is not a production provisioning mechanism. No owner MFA reset/bypass endpoint exists.

## Phase 1 customer, CRM and product extension — V4

Customer accounts use role CUSTOMER in the shared credential table and a separate customer profile. They cannot enter staff/owner routes. All session mutations, including public identity signup/reset requests, require CSRF; only guest enquiry intake is exempt. Customer signup is disabled by default outside explicit native configuration. In prod, registration/recovery additionally require approved privacy configuration and enabled email delivery.

Identity routes under `/api/public/identity`: GET `status`; POST `register` (`name,email,phone,password,consent:true,privacyVersion:"2026-10-23"`), `resend`/`forgot` (`email`), `verify` (`token`), `reset` (`token,password`). Register/resend/forgot return generic 202. Tokens expire after 30 minutes, are single-use SHA-256 digests and are transported in email URL fragments. New token requests supersede earlier links. Verification enables a new account; password reset revokes sessions. Password policy remains 14 characters minimum/72 UTF-8 bytes maximum. Request limits persist in PostgreSQL; no account-existence response is exposed.

`CustomerMail` stores encrypted queued bodies, clears sent/expired payloads and keeps actual SMTP delivery disabled by default. SMTP requires STARTTLS, private credentials and a configured HTTPS public URL. GET `/api/v1/owner/local-mail` is native dev/QA only, excluded when prod is active, MFA-protected and limited to fictional @example.test recipients while real delivery is disabled. No live provider success is claimed.

Customer routes under `/api/v1/customer`: GET/PUT `profile`; GET/POST `enquiries`; GET `enquiries/{id}`; POST `claim` (`publicId,trackingToken`); GET/POST `privacy` (`kind:"EXPORT"|"ERASURE"`). Enquiry reads return customer-owned public specifications/status/history, never assignment, staff next action or private activity. New enquiries use profile identity, not client-supplied alternative email/name. Claim requires both the tracking token and matching verified email. Profile input is `name,phone,address,organization`. List cursor is optional `before=<id>`, 50 rows descending by created_at/id.

Owner privacy routes: GET `/api/v1/owner/privacy`; PATCH `/{id}` with `status:"RESOLVED"|"REJECTED",resolution`. These record an operator-reviewed decision; they do not automatically erase records or deliver exports. A customer cannot open duplicate requests of the same kind while one is OPEN.

CRM routes under `/api/v1/staff/contacts`: GET with `search`/`before`, POST, PUT `/{id}`, GET `/duplicates?email=&phone=`, POST `/{id}/merge` and POST `/{id}/enquiries/{leadId}`. Contact fields: name,email,phone,organization,address,permissionToContact,consentNote,revision. SALES sees own contacts; OWNER sees all. Updates/linking require current revisions. Merge requires explicit target/revisions and the same responsible staff; target details survive, enquiry links move, source is archived and both contacts gain activity. Permission-to-contact requires evidence; it does not trigger marketing messages.

Staff enquiry GET now accepts `search`, `filter:ALL|MINE|UNASSIGNED|OVERDUE` and `before=<id>` for stable descending created_at/id cursor pages of 100. The response remains an array for compatibility. Metrics in the UI describe loaded records, not global counts. New inserts appear on refresh; loading older pages uses the last record cursor.

Product routes: GET `/api/public/catalog`, GET `/api/public/templates/{code}`, GET `/api/v1/owner/templates`, PUT `/api/v1/owner/templates/{code}`. Templates contain name/description, active state, min/max quantity, optional GSM/sizes, finishes and revision. Edits require current revision and preserve UNSURE. Enquiry creation validates configured options and saves a template snapshot/version. Historical product codes remain accepted for compatibility; they are not advertised in the active public catalogue.

Guest enquiry input now includes `privacyConsent:true`, `privacyVersion:"2026-10-23"`, an empty optional `website` honeypot and optional `challengeToken`. GET `/api/public/enquiries/policy` supplies challenge requirement/site key. Production requires approved privacy settings and configured server-validated Turnstile with matching hostname/action; no configuration or provider success is claimed. Native DEV uses persistent quotas/honeypot without an external challenge. API bodies are bounded to 128 KiB; unknown-length bodies are bounded during reads. Trust/proxy-aware client addressing must be validated before deployment; raw forwarded headers are not trusted.

Artwork upload routes remain absent. `ArtifactPolicy` and `PrivateArtifactStore` define Phase 1 admission/private-storage boundaries; only matching PDF/PNG/JPEG can enter quarantine, never be downloaded before a clean scan. Actual storage/scanning/revision workflows are Phase 2.

## Customer provider sign-in

GET `/api/public/sso/providers` returns configured provider availability and authorization URLs. GET `/api/public/sso/pending` reads only this session's pending signup. POST `/api/public/sso/complete` requires CSRF and name/email/phone/current privacy consent. Google verified, unchanged email may activate a new CUSTOMER; Microsoft signup requires email verification in the initiating browser. Verification/resend cannot bypass this browser binding. Existing email accounts are never automatically linked.

GET `/api/v1/customer/sso` lists linked providers. POST `/api/v1/customer/sso/link/{provider}` requires the current password and CSRF before authorization. Identity is keyed by provider/issuer/subject, with no staff promotion. Authorization uses state, nonce and S256 PKCE; callback is `/api/public/sso/callback/{registrationId}`. Provider tokens are not retained. Credentials and live consent testing remain required; see SOCIAL_SIGN_IN_SETUP.md.

## Phase 2A.1 quotations
GET `/api/v1/staff/enquiries/{id}/quotes` and `/api/v1/customer/enquiries/{id}/quotes`: latest 100 versions descending, customer-visible commercial fields/specification snapshot only. Assignment/customer ownership checked server-side; no-store.
POST staff quote route: `expectedVersion,scope,totalPaise,terms,requestedDate?,proposedDate,validUntil`. INR integer paise (1..100000000000); dates use ISO dates and expiry is inclusive in Asia/Kolkata. Proposed date must be current/future and validity current/future, no later than proposal date. Owner or assigned SALES may issue for an open enquiry; unassigned SALES must claim first. Same-enquiry writes serialize; stale counters return 409. Issue supersedes an outstanding ISSUED version and preserves all commercial content/specification snapshots. Accepted quotations cannot be replaced in this slice.
POST `/api/v1/customer/enquiries/{id}/quotes/{quote}/decision`: `action:ACCEPT|REQUEST_CHANGES,note` (max 1000; nonblank for changes). Only linked customer may respond to current unexpired ISSUED version. Repeating the same final action is safe and adds no event; conflicting/stale responses return 409. Acceptance preserves the proposed date as the agreement; no production/payment/email/order side effect. Quote content and events have database immutability guards. This route records commercial acceptance, not a digital-signature certification.

## Phase 2B.1 private design files
All `/api/v1/design/**` routes require a current CUSTOMER/OWNER/SALES/DESIGNER session, CSRF for writes and owner MFA where applicable. Enquiry scope is checked for every operation: linked customer, assigned designer, owner, or assigned/unassigned-visible salesperson (writes require sales assignment). Responses are no-store.

GET `/enquiries/{id}` returns scoped artifacts, proof revision history, up to 500 latest comments and commercial/current-proof approval flags; customers never receive draft DESIGN file metadata or designer identity. GET `/queue` is DESIGNER-only and returns only assigned open enquiries. POST `/enquiries/{id}/designer` with `designerId` (or null) is OWNER-only and requires an enabled DESIGNER; it increments enquiry revision, so refresh other enquiry edits afterward.

POST `/enquiries/{id}/artifacts?kind=REFERENCE|DESIGN` sends raw binary, exact MIME Content-Type and percent-encoded UTF-8 filename in X-File-Name. Not multipart or JSON/base64. Customer may upload REFERENCE only. Only matching PDF/PNG/JPEG signatures/extensions admitted; 20 MiB/file, 100 files/200 MiB/enquiry, persistent actor quotas. Only this exact POST path has the larger body limit; JSON remains 128 KiB. Response 201 includes QUARANTINED/CLEAN/REJECTED status. Scan failures do not clear files. POST `/enquiries/{id}/artifacts/{file}/scan` retries for authorized team members. GET `.../{file}/download` requires a clean verdict and SHA-256 integrity check; attachment-only, no-store/nosniff/sandbox. There are no public static URLs.

POST `/enquiries/{id}/proofs` body `artifactId,expectedRevision,summary`: requires clean DESIGN bytes plus an accepted quotation; locks enquiry, checks latest counter, and creates a new immutable revision bound to quote/file. Customers cannot publish. POST `.../proofs/{proof}/comments`: `body,pageNumber?` (1..10000). Only current undecided proof accepts comments. POST `.../proofs/{proof}/decision`: customer-only `action:APPROVE|REQUEST_CHANGES,note,quoteId,artifactHash,reviewed:true`; request changes requires note. Only latest clean proof with matching file hash/quote can receive a decision. Repeated same decision is safe; other conflicts return 409. Prior approvals/comments remain append-only. A replacement revision needs new approval; no payment, invoice, production or notification side effect.

## Active owner corrections — 24 October 2026 (supersedes earlier quotation expiry/status UI contracts)
Quotation issue input no longer includes validUntil. requestedDate is preserved from enquiry intake, else supplied staff date, else today IST. proposedDate is required and current/future. Output requested_date/proposed_date are explicit YYYY-MM-DD strings. Historical valid_until is retained only for immutable history; it no longer expires offers. acceptedOffline:true requires acceptanceSource PHONE|EMAIL plus agreement evidence of at least 20 characters. Recorded actor/channel/evidence persist on the accepted version and remain immutable. This is staff-recorded confirmation, not a customer portal click. Issuing sets QUOTED; exact portal or recorded offline acceptance sets IN_PROGRESS. PATCH staff status only supports REVIEWING; quote/acceptance/closure routes own other transitions.

Enquiry creation accepts optional requestedDate. Staff LeadView includes requestedDate. Customer list/detail include live_work, closure_kind and closure_message. Staff/customer queues sort open records before CLOSED and use a matching composite cursor. POST `/api/v1/staff/enquiries/{id}/resolve` takes revision, kind REJECTED|REMOVED and a meaningful customer-visible message (20..1000 characters). Removal archives CLOSED with reason, clears reminders and preserves immutable records. Token-protected guest tracking also includes closure reason.

GET `/api/v1/design/enquiries/{id}/work` returns scoped live-work status/stages, designer public display name, configured public team contact, public work logs and conversation messages. Internal enquiry follow-up/actor IDs/passwords are excluded. Production, payment and delivery show their actual unimplemented/not-recorded states. POST `/work/messages` accepts body (max 3000), records PORTAL channel server-side and requires accepted/open work, scope and CSRF. Email channel is reserved for a future trusted mailbox adapter; mailConnected remains false. Customers cannot post an email-labelled message.

Proof comments accept optional paired anchorX/anchorY normalized 0..1 coordinates plus pageNumber. Preview GET `/api/v1/design/enquiries/{id}/artifacts/{file}/preview` authorizes scope, CLEAN verdict and SHA-256 exactly like download; only PNG/JPEG can be rendered, with no-store/nosniff/sandbox headers. PDFs remain downloads. Pins request designer changes; files/approvals are not overwritten.

GET `/api/v1/auth/appearance` returns current-user version/has_image/public_name. POST `/appearance/image` accepts a base64 PNG/JPEG thumbnail, at most 64 KiB and 256x256 input pixels; server decodes with metadata ignored and produces a fresh 128x128 PNG. GET `/appearance/image` returns only the signed-in user's private PNG; DELETE resets it. PUT `/appearance` changes own publicName (max 100), used for designer display, never their private login address. All writes require session and CSRF. Original full-resolution profile files never reach storage.

Inline PNG/JPEG previews additionally inspect dimensions without decoding pixels and reject above 25 million pixels or a 20,000-pixel side. Oversized or unreadable previews must use authorized download; no original bytes or approval are rewritten.
# Phase 2 completion additions — 24 October 2026

All staff mutations require session/CSRF, current enabled role, enquiry assignment/claim and owner MFA where applicable. Customer reads require linkage; inaccessible records return 404. Existing contracts below remain unless explicitly superseded here.

- `GET/PUT /api/v1/staff/enquiries/{id}/quote-draft`: private partial draft. PUT `{expectedRevision,payload}` (maximum serialized payload 30,000 characters), conflict on a stale revision. Draft changes do not issue an offer.
- Quote issue additionally accepts up to 30 `lines`: `{description,quantity,unitPaise,taxBps,specifications}`. Tax is an explicitly entered percentage in basis points, with half-up rounding per line to paise. `totalPaise` must exactly match the computed sum; a missing line list retains all-inclusive compatibility. Controlled specification keys are validated against the configured print vocabulary.
- `GET /api/public/print-vocabulary`, `GET /api/v1/owner/print-vocabulary`; owner `PUT .../{category}` with `{expectedRevision,choices}` retains UNSURE, unique uppercase option codes and immutable configuration audit. Existing snapshots are unaffected.
- `POST /api/v1/staff/enquiries/{id}/quotes/{quote}/withdraw` with `{reason}` of 20..1000 meaningful characters. Only ISSUED offers can be withdrawn; accepted content is never deleted.
- Accepted-work amendments use existing issue endpoint with `amendmentOf` equal to the current accepted quote and `amendmentReason` of at least 20 characters. Pending amendments leave the old agreement active. Exact new acceptance moves the old agreement to REPLACED without changing its evidence, and requires a proof bound to the new accepted agreement. No expiry is introduced.
- `GET /api/v1/{staff|customer}/enquiries/{id}/quotes/{quote}/document`: generated PDF attachment, no-store/nosniff/sandbox. Bytes and SHA-256 are saved at issue; historical quotations without generated bytes return 404.
- `GET /api/v1/{staff|customer}/enquiries/{id}/orders`: scoped commercial order history with exact copied lines/artwork linkage. `POST /api/v1/staff/enquiries/{id}/orders` accepts `{quoteId}` for the current ACCEPTED quote, returns `{id,created}` and is idempotent under repeated/concurrent conversion. No production release occurs.
- `POST /api/v1/staff/enquiries/{id}/order-artwork`: `{orderLineId,artifactId}` requires a current order and clean artwork of the same enquiry; append-only linkage is idempotent.
- `POST /api/v1/design/enquiries/{id}/tasks`: team-only `{id?,expectedRevision,title,notes,dueDate,state}`. New revision is zero; stale update conflicts. State TODO/IN_PROGRESS/DONE. Up to 100 tasks per enquiry; customer projections omit private task data.
- `POST /api/v1/design/enquiries/{id}/comments/{comment}/resolve`: `{note}` of 10..1000 characters; append-only resolution preserves the original comment. Customer may only resolve their own feedback; scoped team may resolve enquiry feedback.
- Proof customer decision also accepts REJECT with a nonblank explanation. All decisions require the exact current accepted quote, file hash and reviewed flag; old agreement proofs cannot be approved.
- `POST /api/v1/design/enquiries/{id}/proofs/{proof}/offline-approval`: owner/assigned sales only, `{source:PHONE|EMAIL,evidence,evidenceArtifactId,quoteId,artifactHash,reviewed:true}`. Evidence text >=20 characters and a CLEAN REFERENCE artifact in the same enquiry are required. Proof/evidence bytes are integrity checked. Immutable decision source/recording actor remain distinct from customer portal approval.


## 24 October staff scope and stage revision

Migration V11 introduces Contacts grants, intake decisions, encrypted rejection notifications and assigned staff tasks. Session/account views expose `contactsAccess`; never expose credentials. Owner `PATCH /api/v1/owner/staff/{id}/permissions` accepts `{contactsAccess}` and requires recent MFA; grant/revoke increments authentication version, invalidating existing sessions. Every Contacts endpoint enforces this capability in addition to record scope.

`GET /api/v1/design/intake` returns minimal incoming requirements to owner/designer only. `POST /api/v1/design/intake/{id}/accept` accepts `{revision,comment?,reviewerId?}`; owner can choose an enabled owner/designer, designer can accept only for themselves. `POST .../{id}/reject` accepts `{revision,comment?}`; blank comments use a meaningful standard explanation. Decisions lock/version-check the enquiry and preserve actor/time/history. Sales cannot see NEW records or bypass intake by issuing a quote. Designers cannot take another assigned designer's intake.

Owner `POST /api/v1/owner/enquiries/{id}/dismiss` accepts `{revision}` for queue archival with a standard customer-visible reason and deduplicated encrypted rejection email. Accepted work requires reviewed resolution; no physical/audit-history deletion. A queued message is not a delivered message. Local real mail remains disabled.

`GET /api/v1/auth/jobs` returns only own role-matched tasks on live work; `PATCH /api/v1/auth/jobs/{id}` records expected revision and TODO/IN_PROGRESS/DONE. Owner GET/POST `/api/v1/owner/enquiries/{id}/jobs` lists/assigns an enabled specialist's task (staffId,title,notes,dueDate). These are generic tasks, not manufacturing or delivery transitions. See FULFILMENT_WORKFLOW.md for future APIs and gates.

## Intake artwork, communication and archival correction

V12 makes guest upload attribution nullable while preserving immutable file/audit metadata; adds message-id/reply/subject fields and unique message ids to the encrypted outbox. `POST /api/public/enquiries/{publicId}/artwork` requires CSRF plus `X-Tracking-Token`, `X-File-Name`, matching PDF/PNG/JPEG Content-Type and raw bytes. Intake must be enabled (production privacy gate also applies); token authorizes upload only for NEW enquiries for 24 hours. Up to five initial attachments, per-IP/per-enquiry limits and 20 MiB/file. No guest download. Native scanning, quarantine and clean-read integrity remain mandatory.

Quotation issuance rejects an outstanding ISSUED offer (409); withdraw or receive a customer change request before revising. Optional `requestArtwork` on issuance records/queues a request if no clean reference exists; `POST /api/v1/staff/enquiries/{id}/artwork-request` reuses scoped commercial permission, clean-file detection and a one-hour duplicate guard. Enquiry acknowledgement, quote and artwork outbox entries use a stable subject/root Message-ID with In-Reply-To/References on delivery. Client threading is best effort; Gmail delivery is not configured or verified. Commercial terms remain the actual agreement terms, not a way to request automation.

Owner dismiss now also archives accepted/closed work, preserving the accepted document and a cancellation explanation. `POST /api/v1/owner/enquiries/dismiss-selected` accepts `{enquiries:[{id,revision}]}` (1–100): atomic version checks and deterministic locks. `POST /api/v1/owner/enquiries/archive-except` accepts `{preserveId}` for an explicit administrative cleanup, at most 5000 records; it locks the table and preserves the specified existing active record. All other records are archived with customer explanations and deduplicated emails. No physical deletion or WhatsApp delivery is implied.

24 October local Gmail setup preparation: native DEV permits exact loopback HTTP only with explicit recipient restriction and a send-after activation boundary; historical and non-allowed mail is held. Production keeps HTTPS. Enable-LocalGmail.ps1 prompts privately, sends one setup test, saves settings on SMTP acceptance and restarts native services. Temporary mailbox is selected; private credential and real delivery verification remain pending. See GMAIL_LOCAL_SETUP.md.

24 October lifecycle-mail continuation: V13 adds one-time customer setup with quotation, lifecycle customer/team notifications, owner Email activity, proof-gated manually confirmed production/QC/dispatch/delivery milestones, delivery issue holds and durable 48-hour completion. This supersedes earlier “milestone emails/manual operations/timer not implemented” notes only for this bounded slice. Photos/GPS, stock/payment production controls, inbound Gmail sync and multi-page artwork viewer remain pending. Scope, APIs and verification: WORKFLOW_EMAIL_VERIFICATION.md.

24 October signup correction: V14 repairs repeated unverified signup/resend, binds the latest chosen password to verification, and creates guest access at intake acceptance. Customer-only email carries a 24-hour temporary password with forced activation/change and privacy consent; existing passwords are retained. New mail is allowed to any real address by explicit owner request; held/synthetic mail is not replayed. This supersedes earlier recipient-only/link-only account policy. See CUSTOMER_SIGNUP_CORRECTION.md.

24 October quotation email enhancement: V15 binds outbox QUOTE messages to the immutable issued quotation. Native dispatch verifies its PDF hash and attaches that exact version; branded escaped HTML/plain alternatives include the customer workspace link, sender name and short product/reference subjects with existing thread headers. No API route or approval gate changes. Details and focused evidence: QUOTATION_EMAILS.md.

24 October PDF review slice: native resource-limited PDF inspection and authenticated local-worker multipage canvas preview, per-page location comments, full-document page acknowledgements tied to immutable revision/hash/quote, reference/file Preview dialogs and a persistent least-privilege local test designer are implemented. This supersedes earlier multipage PDF viewer pending notes only for PDF/image preview/comment review; source editing, advanced preflight, gallery reuse, delivery photos/GPS and payment/WhatsApp integration remain separate. API, migration, role and focused evidence: PDF_ARTWORK_REVIEW.md. Owner-proposed entry/payment/notification paths: ORDER_ENTRY_AND_PAYMENT_FLOWS.md.

## Review refinements (24 October)

GET /api/v1/staff/enquiries accepts status=ALL|NEW|REVIEWING|QUOTED|IN_PROGRESS|CLOSED alongside search, filter and before. Filtering occurs before pagination and preserves role visibility. Existing quotation document endpoints now also supply the authenticated in-app PDF preview. No new public file route is introduced. Email grouping is server-managed; clients cannot supply Message-ID, References or phase.

## Reference artwork reviews (24 October)
All routes below require existing enquiry owner/assignment access. GET /api/v1/design/enquiries/{id}/samples returns fictional category examples; GET .../samples/{code}/preview returns no-store PNG; POST .../samples/{code} selects and scans an enquiry-owned reference. POST .../references/{file}/feedback accepts {note, pins:[{page,x,y,text}]} with <=50 pins and normalized coordinates; it records an immutable request and queues scoped annotated-copy notifications. GET .../reference-feedback/{feedback}/document returns its verified private PDF. Workspace adds artworkRequested, referenceEditable, productCode and referenceFeedback. Customer enquiry lists/details add customer_stage. Work data adds outgoing email metadata only; no credentials or recipients.


## PF-320 payments and invoices — 24 October 2026
Owner-configured advance/credit rules, immutable verified receipts/corrections, commercial invoice PDF preview/email and server-side production/dispatch gates replace the earlier Not recorded/payment-unimplemented placeholder. Customer and assigned sales are read-only; owner confirms manual finances. Optional Razorpay TEST checkout uses server-owned amounts, HMAC plus captured-payment API verification and deduplicated callbacks/webhooks. Live gateway payments and GST tax invoicing are not enabled. Routes, schema, screen behavior, security boundaries and focused verification: PAYMENTS_AND_INVOICES.md.


Workflow errors now return a no-store structured ACTION_NOT_AVAILABLE message with the original HTTP status and customer-safe reason, so production/payment holds are actionable in the UI. Razorpay checkout/webhook/ledger rules are unchanged.

## 24 October operations continuation

Owner-authorized automatic designer routing, project-level production queues and private delivery photo evidence are implemented. PF-300/PF-330 now include printing/finishing/QC/rework/hold progression, scoped printer assignment and pickup/transit/delivery evidence. Existing proof, quotation, finance and dispatch gates remain. See OPERATIONS_WORKFLOW.md for APIs, migrations V24/V25, role scopes, upload limits, notifications and the focused Gradle/browser commands. Full shop-floor routing, inventory, capacity reservations, GPS and partial dispatch remain separate.
