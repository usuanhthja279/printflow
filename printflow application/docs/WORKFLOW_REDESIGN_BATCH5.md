# J.D.Printex Workflow Redesign — Batch 5

## Scope and status

- Repository: `usuanhthja279/printflow`
- Base: `feature/workflow-redesign-batch4` at `b6bd201ab9dcdb1f4254b09a980b80e9a61de04a`
- Working branch: `feature/workflow-redesign-batch5`
- Scope: public website refinement, guided quotation and a basic official Meta WhatsApp assistant.
- No merge, deployment or Batch 6 work is authorized.
- WhatsApp outbound is disabled by default. No provider credentials were found or used at audit time.

## Phase 0 audit — before implementation

### What already works

- The public site has a J.D.Printex homepage, responsive navigation, live product catalogue, product family listing/details, interactive illustrative finish preview, enquiry tracking and customer account links.
- The quote request submits to the Spring public enquiry API (or authenticated customer API), loads the backend catalogue and versioned templates, preserves product preselection, supports Guided/Professional specification modes, leaves known-unknown specifications unset, requests privacy consent, supports guest verification, and uploads artwork through the existing private malware-scanned intake route.
- A successful enquiry returns a real public reference plus a one-time tracking token. Artwork upload failure is separated from enquiry creation and has an attachment retry, avoiding a second enquiry for that case.
- Staff can create phone/email/WhatsApp-labelled intake records through the existing CRM and source enum. That source label is not a connected WhatsApp provider.
- Spring has session/CSRF controls, role and owner MFA checks, persistent request limits, privacy-consent validation, encrypted/private file handling and mail/payment controls. Flyway currently has migrations V1–V25. The product catalogue and enabled templates are the authority for customer-facing product selection.

### Improvements needed

- The current request form is a two-part long form rather than a four-step wizard. It needs explicit product, specification, artwork/contact and review steps, intended use, unknown-specification summary, distinct optional WhatsApp consent, and a clear final-pricing statement.
- Public product discovery and contact actions should be driven by enabled catalogue entries and configured business identity. Do not add invented business claims or fallback live-contact numbers.
- Submission currently has only client-side `busy` protection. It lacks a server idempotency key/result contract for browser/network retries. The solution must preserve one-time token delivery and avoid replaying a token or enquiry after an ambiguous response.
- WhatsApp has no Cloud API provider, webhook, inbox, consent ledger, message persistence, durable outbox, bot simulator or staff conversation UI. Existing source tagging alone is not integration infrastructure.

### Reuse plan

- Reuse `CatalogController`/`ProductTemplates` as the source of allowed products and specification choices.
- Reuse `CreateEnquiry`, `PublicEnquiryController`, customer-authenticated enquiry creation, `EnquiryService`, `StaffEnquiryService`, source attribution, privacy policy versioning and existing artifact scanning/upload APIs. Extend the existing intake path for idempotency rather than adding parallel enquiry creation.
- Reuse the existing J.D.Printex layout, navigation, PrintStudio/PrintPreview, product cards, form controls, accessible modal/panel styles and StaffWorkspace tabs.
- Implement WhatsApp behind isolated backend configuration and a provider adapter. Webhook intake must persist a deduplicated event before returning success. Keep the simulator independent from the live provider adapter.
- Add minimal Flyway tables for conversations, messages/events, consent/suppression, handoff and outbound idempotency/status. Do not bind WhatsApp sender phone numbers to customer accounts or reveal private project data.

### Missing provider infrastructure

- No Meta WhatsApp app, business account/phone-number ID, Graph API version, access token, app secret, webhook verification token, public HTTPS webhook, approved templates or verified business number are configured in this local environment.
- Therefore this batch can implement and test signed webhook/provider contracts, deterministic bot behavior, persistence, staff inbox and website simulator locally, but cannot claim provider-configured or live-verified behavior. Real outbound stays disabled.
- The official Meta Cloud API and current policy requirements must be documented and rechecked before provider activation. Do not use WhatsApp Web automation, scraping or an unofficial messaging gateway.

### Security constraints retained

- Live provider credentials remain server-side and are never committed or exposed to Next.js.
- Verify webhook signatures over the raw request bytes with constant-time comparison; enforce size/schema limits; persist deduplication keys transactionally; log neither raw payloads nor message content unnecessarily.
- Contact phone number alone is not identity proof. No automatic customer/order linking and no disclosure of quote, proof, invoice, payment or delivery data through WhatsApp.
- WhatsApp communication opt-in is distinct from website privacy consent. STOP/UNSUBSCRIBE suppress business-initiated messages immediately. Free-form replies require a valid customer-service window; otherwise require an eligible approved template and explicit consent.
- Owner MFA and SALES queue scope remain enforced. Other staff roles and customers receive no WhatsApp inbox access. Public API endpoints remain rate-limited and protected from duplicate submission.

### Deferred backlog preserved

Gmail inbound mailbox synchronization, advanced settlements, portfolio campaigns and repeat ordering are deferred by this approved Batch 5 scope. This does not remove their entries from the broader workflow roadmap. Marketing campaigns, bulk sending, payment links and automatic production notifications are also out of scope.

## Implementation and verification log

### Implemented

- Public discovery now uses enabled catalogue data for the homepage, product listing and product detail pages. Product detail supports interactive finish samples and contextual quote/contact actions. The website copy avoids invented prices and live-contact details.
- The public/customer request form is a four-step quote wizard: product/purpose, specifications, artwork/contact and review/submit. Unknown values stay unknown, intended use is captured, privacy consent is required, WhatsApp consent is optional and separate, and the review screen explains that final pricing is provided after staff review.
- Public/customer enquiry creation accepts an idempotency key. Identical retries return the original public result; reusing a key with changed content is rejected. Artwork attachment failure can be retried without creating another enquiry. Artwork still uses the existing private scan-and-release flow.
- Configured WhatsApp click-to-chat is separate from WhatsApp Cloud API sending. The official Graph API adapter, signed/deduplicated webhook handling, encrypted message/contact persistence, deterministic menu/handoff, opt-out suppression, service-window rules, durable outbox and owner/sales inbox are implemented. An authenticated owner-only simulator supports local testing without a provider call. Live sending and public contact links remain disabled by default.
- Added local WhatsApp setup/security documents and the required configuration variables in `.env.example`. The local staff initializer provisions a dedicated WhatsApp encryption key without displaying it.
- Added an optional `PRINTFLOW_NEXT_DIST_DIR` override so builds can run alongside an existing standalone app which holds `.next/standalone` open. The default remains `.next` for normal starts. Updated the native backend test helper to use PostgreSQL 5432 and documented Windows test recovery steps in `AGENTS.md`.

### Verification performed

| Check | Result |
|---|---|
| Focused WhatsApp unit tests (`MetaWebhookSignatureTest`, `WhatsappOutboundPolicyTest`, `WhatsappSettingsTest`) | **Passed** with Gradle 9.8.2 / Java 21. |
| `StaffScopeIntegrationTest` | **Passed: 9 tests, 0 failures** against the existing project-local PostgreSQL 17 database on port 5432. Test shell loaded `POSTGRES_*` privately from ignored `.env` and mapped them to Spring `DB_*`; no credential values were printed. |
| `npm.cmd run typecheck` | **Passed.** |
| `PRINTFLOW_NEXT_DIST_DIR=.next-batch5 npm.cmd run build` (PowerShell environment variable) | **Passed.** The default output was held by a standalone server; the separate build kept the existing output/service untouched. The isolated standalone preview used the same override and its static assets. |
| Focused Playwright: `batch5-public-website.spec.ts` and `batch5-whatsapp-inbox.spec.ts` | **Passed: 4 tests** (desktop and mobile, Edge). The tests use synthetic API fixtures and cover discovery, all four quote steps, idempotent retry, separate consent, attachment retry, inbox handoff/reply, simulator and opt-out. They are not a live Meta-provider or real-customer acceptance test. |
| Screenshots | Actual browser captures at configured desktop `1440×900` and mobile `390×844`; reviewed for clipping, layout, wrapping and horizontal overflow. See `../output/batch5-visual-evidence/README.md`. |

Commands run (from the `printflow application` project root unless shown):

```powershell
.\scripts\Initialize-LocalDatabase.ps1
```

The focused Gradle unit + cached-integration check was run from `backend` with Java 21 loaded using `..\scripts\Use-PrintFlowTools.ps1`. The actual integration rerun loaded ignored `.env` settings into the process, mapped the local `POSTGRES_*` values to `DB_*`, then ran:

```powershell
.\gradlew.bat --no-daemon integrationTest --rerun-tasks --tests com.printflow.StaffScopeIntegrationTest
```

The focused unit selectors were `com.printflow.whatsapp.MetaWebhookSignatureTest`, `com.printflow.whatsapp.WhatsappOutboundPolicyTest` and `com.printflow.whatsapp.WhatsappSettingsTest` under Gradle `test`. Frontend checks were:

```powershell
npm.cmd run typecheck
$env:PRINTFLOW_NEXT_DIST_DIR='.next-batch5'; npm.cmd run build
$env:PLAYWRIGHT_BASE_URL='http://127.0.0.1:3001'; npm.cmd run test:e2e -- e2e/batch5-public-website.spec.ts e2e/batch5-whatsapp-inbox.spec.ts --max-failures=1
```

The isolated preview ran the generated `frontend/.next-batch5/standalone/server.js` with `PORT=3001`, `HOSTNAME=127.0.0.1` and `PRINTFLOW_NEXT_DIST_DIR=.next-batch5`; the temporary build output and preview process were removed/stopped after screenshots were captured. The original `.next` tree and any app service were left untouched.

### Test recovery findings

- The first forced integration run could not connect because PostgreSQL was stopped. Starting the existing data directory with `scripts/Initialize-LocalDatabase.ps1` brought the project-local database back without reinitializing its contents.
- A subsequent attempt used Spring's placeholder credentials and failed authentication. The test process must import the ignored local `.env` and map `POSTGRES_DB/USER/PASSWORD` to `DB_URL/DB_USERNAME/DB_PASSWORD`. `scripts/Test-Backend.ps1` now uses port `5432` rather than the stale `5434`.
- A default Next build hit `EBUSY` because the standalone output directory was held open. A temporary alternate output initially hit a Windows worker permission failure, then the one approved retry succeeded. The recovery path is documented in `AGENTS.md`; the user’s original standalone output was not stopped or overwritten.
- Initial Playwright failures were selector drift (current copy and duplicate text in sidebar/thread) and checking an async upload counter before the UI displayed completion. The focused assertions were aligned with the current UI and await visible result state before checking counters. All four final cases passed.

### Visual notes and limitations

- Desktop layouts use the available width well; the dark homepage hero, catalogue cards, quote wizard summary and staff inbox remain visually distinct and readable.
- At 390px the wizard and inbox stack into a single column and the tests report no horizontal overflow. Consent copy and long customer messages wrap; lower conversation/simulator content requires vertical scrolling as expected.
- WhatsApp implementation is **not provider-configured or live-verified**. There are no Meta business assets, public HTTPS webhook or approved templates configured. Outbound remains disabled and the local simulator does not send provider messages. Free-form replies outside the service window remain blocked; approved-template sending is deferred.
- Gmail mailbox synchronization, campaigns, payment links/reminders and WhatsApp order-status disclosure remain deferred. The existing REST workflow and permissions remain the source of truth for enquiries, quotations and orders.

## Local E1 acceptance correction — 2026-10-25

### Corrections

- Fixed bot takeover handling so either persisted `handoff_status=HUMAN` or `bot_state=HANDOFF` prevents automated menu/greeting replies. STOP remains handled before this guard, and the existing assigned-sales authorization is still required to reply or resume bot control.
- Added a provider-status event ledger for signed callbacks that arrive before a provider message ID has been attached. Events are applied after acceptance is recorded, and status precedence prevents a late SENT update from downgrading DELIVERED/READ.
- Added bounded recovery for interrupted sends: `SENDING` rows older than two minutes become `UNKNOWN` and are not resent automatically. Ambiguous provider failures and 5xx responses also remain `UNKNOWN`; only an explicit rate-limit response uses the bounded retry path. This avoids blind duplicate delivery when acceptance is uncertain.
- Added migration `V28__whatsapp_outbox_crash_and_status_recovery.sql` for send-start timestamps and unapplied provider callbacks.

### E1 results

| Check | Result |
|---|---|
| Native PostgreSQL integration (`StaffScopeIntegrationTest`) | **Passed: 12 tests, 0 failures, 0 errors** against the project-local PostgreSQL 17.11 database on `127.0.0.1:5432`. This includes menu then human handoff, inbound persistence without bot reply, assigned staff reply, return to bot, STOP while staff-controlled, duplicate signed webhook delivery, outbox crash recovery and early delivered-status callback. |
| Synthetic real-backend quote | **Passed** via the running Spring HTTP API against native PostgreSQL. The test verifies one saved enquiry, privacy and separate WhatsApp consent, same-key idempotent replay, changed-payload conflict, invalid artwork rejection followed by successful artwork retry, and no duplicate enquiry. The clean scanned artifact and synthetic enquiry are retained in the ignored local test database as evidence; their contact data uses `example.test` and is not a real customer. |
| WhatsApp focused security unit tests | **Passed**: `MetaWebhookSignatureTest`, `WhatsappOutboundPolicyTest`, `WhatsappSettingsTest`. |
| Frontend typecheck | **Passed**: `npm.cmd run typecheck`. |
| Next.js production build | **Passed**: `PRINTFLOW_NEXT_DIST_DIR=.next-e1 npm.cmd run build` (PowerShell process environment). Build output was isolated to avoid the running default standalone directory. |
| Focused desktop/mobile Playwright | **Passed: 4 tests** (2 desktop, 2 mobile) against the isolated production preview on port 3001: public quote flow/idempotent UI retry/artwork retry and WhatsApp inbox handoff/reply/resume/opt-out. These UI cases use synthetic API fixtures; the real-backend quote acceptance is separately covered by the PostgreSQL integration test above. |
| Live Meta WhatsApp | **Not run by design.** `WHATSAPP_ENABLED` remains false; no provider send, deployment, merge or live configuration was performed. |

### Exact verification commands

From `backend`, the integration run loaded ignored local `.env` values into process variables, mapped `POSTGRES_DB/USER/PASSWORD` to Spring `DB_URL/DB_USERNAME/DB_PASSWORD`, and started PostgreSQL using the checked-in script before invoking Gradle in the same managed session:

```powershell
& '..\scripts\Initialize-LocalDatabase.ps1'
# Import ignored ..\.env variables; map POSTGRES_* to DB_* for Spring
. '..\scripts\Use-PrintFlowTools.ps1'
.\gradlew.bat --no-daemon integrationTest --tests com.printflow.StaffScopeIntegrationTest --rerun-tasks
.\gradlew.bat --no-daemon test --tests com.printflow.whatsapp.MetaWebhookSignatureTest --tests com.printflow.whatsapp.WhatsappOutboundPolicyTest --tests com.printflow.whatsapp.WhatsappSettingsTest
```

From `frontend`:

```powershell
npm.cmd run typecheck
$env:PRINTFLOW_NEXT_DIST_DIR='.next-e1'; npm.cmd run build
$env:PLAYWRIGHT_BASE_URL='http://127.0.0.1:3001'; npm.cmd run test:e2e -- e2e/batch5-public-website.spec.ts e2e/batch5-whatsapp-inbox.spec.ts --max-failures=1
```

Playwright used the standalone output built in `.next-e1` with its static assets and a temporary local preview at port 3001. The checked-in screenshot suite captured desktop `1440×900` and mobile `390×844` views under `output/batch5-visual-evidence/`; these are browser-fixture screenshots, not evidence of real Meta messaging.

### Readiness

**E1 local acceptance: ready for user testing.** Database-backed workflow, safe local outbox recovery, signed webhook handling, production build and focused desktop/mobile UI checks all passed. Live WhatsApp delivery remains deliberately disabled and is outside E1 acceptance; provider credentials/webhook infrastructure and a separately authorized live verification are still required before any production activation.
