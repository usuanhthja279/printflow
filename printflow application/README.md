> **25 October workflow redesign — Batch 1:** action-oriented customer hub, full-width proof studio, scoped owner inbox, compact production queue/detail drawer and evidence-first delivery. [Implementation and focused checks](docs/WORKFLOW_REDESIGN_BATCH1.md). Existing server gates and providers are unchanged.

> **Operations continuation — 24 October:** automatic designer routing, scoped printer production queues and private pickup/transit/delivery photo evidence are implemented. See [workflow and review steps](docs/OPERATIONS_WORKFLOW.md). Native Gradle, no Docker; verification is limited to the new operations slice and one browser journey.

> **UI review handoff — 24 October:** reference-inspired interactive public pages, scoped dashboard summaries, artwork sample search/comments, wide work/conversation layout and phone navigation are ready for manual review. See [implementation notes](docs/IMPLEMENTATION_REVIEW.md). Minimum final checks only; real Razorpay capture remains unverified.

> **24 October finance slice:** manual advance/balance receipts, immutable commercial invoice PDFs, production/dispatch payment rules and optional Razorpay TEST checkout are available. See [payments and invoices](docs/PAYMENTS_AND_INVOICES.md). Live gateway and statutory GST tax-invoice activation remain separate.

> **24 October PDF review:** clean artwork now has on-screen Preview, multipage PDF navigation/zoom, page-pinned comments and complete-page approval acknowledgements. A persistent least-privilege local test designer is available. See [PDF review and focused tests](docs/PDF_ARTWORK_REVIEW.md) and [the proposed entry/payment flows](docs/ORDER_ENTRY_AND_PAYMENT_FLOWS.md). Native Gradle, no Docker; use `-DisableMail` for synthetic browser tests, then restore normal real-mail startup.

> Phase 1 continuation: customer identity/recovery, CRM contacts, versioned product templates and privacy/guest safeguards are built as a development candidate. See [development status](docs/PHASE1_DEVELOPMENT_STATUS.md) and [manual testing checklist](docs/PHASE1_MANUAL_CHECKLIST.md). Provider and production release gates remain open.

# J.D.Printex — native foundation and first staff slice

> **Phase 2 development completed for review:** quotation drafts, item/tax pricing, controlled print vocabulary, immutable PDFs, withdrawal/amendments, commercial orders/artwork linkage, designer tasks, resolved feedback, revision comparison and evidenced offline approval. See [completion and testing](docs/PHASE2_COMPLETION.md) and the updated [phase-wise handbook](docs/DEVELOPMENT_HANDBOOK.md). Production/provider release gates remain open; Phase 3 operations have not started.

> **24 October continuation:** native artwork uploads now pass real Windows Defender scans. The focused designer/customer journey verifies private preview, pinned feedback, a replacement proof and final approval. See [native artwork verification](docs/NATIVE_ARTWORK_VERIFICATION.md), [scanner setup](docs/DESIGN_PROOF_SETUP.md) and [phase-wise remaining features](docs/REMAINING_FEATURES.md). Use `./scripts/Test-DesignApprovalBrowser.ps1` for that one native journey; no Docker is involved.

**Business:** J.D.Printex. The customer preview is tailored to wedding cards, hotel print materials, pamphlets/leaflets and offset gloss printing. See `docs/BUSINESS_START.md` for the owner-confirmed scope, process review and next development steps. PrintFlow remains the internal project name.

**Version:** 0.1.0 · **Created:** 2026-10-23 · **Status:** development prototype, **not production ready**.

Customer website, local enquiry capture and tracking, plus session-protected sales enquiries and follow-ups. PostgreSQL/Flyway persists accounts, assignments and activity. The native foundation and first staff slice are tested; the full Phase 1 and production launch controls are not complete.

> **Start here:** `CODEX_MASTER_PLAN.md` is the main handoff for Codex. It includes phases 0–7, diagrams, architecture, guardrails, workflows, prompts, tests, acceptance criteria and deliverables.

## Local development on Windows — native, no Docker

Requires Python 3.12+, Node.js 22+ and Java 21. Gradle is provided by the checked-in wrapper; Maven is not used.

```powershell
./Start-PrintFlow.ps1
# Subsequent starts with existing builds:
./Start-PrintFlow.ps1 -SkipBuild
# Frontend hot reload, existing backend build:
./Start-PrintFlow.ps1 -Dev -SkipBuild
# Stop services; keep database files:
./Stop-PrintFlow.ps1
```

The startup script discovers Java 21, downloads a checksum-pinned portable PostgreSQL 17 development bundle if needed, initializes an isolated cluster and random local credentials, builds the backend with Gradle and starts native services with hidden windows. No Windows service or Docker engine is required. Database files and logs are under ignored `.local/`; downloaded tools are under ignored `.tools/`. `.env` holds local credentials and is never committed.

Open <http://127.0.0.1:3000>. The API is at <http://127.0.0.1:8080>, PostgreSQL at `127.0.0.1:5434`. All listeners stay local. Use fictional data only. Sign in at `/business` using `APP_STAFF_BOOTSTRAP_USERNAME` and `APP_STAFF_BOOTSTRAP_PASSWORD` in ignored `.env`. Credentials are generated locally and never printed. Separate owner credentials use `APP_OWNER_BOOTSTRAP_USERNAME`/`APP_OWNER_BOOTSTRAP_PASSWORD`; sign in at `/business` and enroll your authenticator privately. Mail uses the privately configured test-recipient restriction; use `-DisableMail` for synthetic automation. See [local development](docs/LOCAL_DEVELOPMENT.md) for lifecycle, memory limits and verification commands.

## Verify the foundation

```powershell
. ./scripts/Use-PrintFlowTools.ps1
./scripts/Test-Backend.ps1
python scripts/static_validate.py
python scripts/verify_staff.py --restart
cd frontend
npm run typecheck
npm run build
npm run test:e2e
```

Browser tests use installed Microsoft Edge by default and synthetic records. Run builds while stopped; run HTTP/browser checks against the running native stack. Production-profile boundary checks use `scripts/Start-ProductionBoundaryCheck.ps1` temporarily on loopback port 8082. Docker Compose files are legacy packaging references; the local start/stop and verification paths do not use Docker. Container runtime verification is not required for the owner-requested native development setup.

## Current functional scope

| Area | Actually implemented | Remaining work |
|---|---|---|
| Public website | Homepage, product categories, navigation | Extend Phase 1 |
| Enquiry request | Validated form → Spring API → PostgreSQL | Phase 1 advanced spec UI |
| Tracking and customer access | Secret-token tracking, verified customer accounts, recovery and linked enquiries | Live email/provider configuration and release controls |
| Staff access | BCrypt sessions/CSRF, owner MFA/recovery, staff administration, role/ownership checks and revocation | Owner MFA/account administration added; remaining operational role modules |
| Follow-ups and contacts | Scoped queue, manual intake, claim/release, reminders, contacts and product templates | Imports, expanded tasks and real notification delivery |
| Database | Flyway V1–V10: identity, enquiry, quotations/documents, commercial orders, proof/tasks and print vocabulary | Phase 3 production/inventory/finance schema |
| Environments | Native PostgreSQL/Java/Node; no Docker in the active workflow | Production infrastructure |
| QA | Unit, PostgreSQL HTTP/security, production-boundary and desktop/mobile tests | Broader Phase 1+ suites |
| Artwork | Private native-scanned files, raster review/comparison, designer tasks/deadlines, resolved comments and portal/evidenced offline approval | Advanced sandboxed PDF editing/preflight and operational readiness rules |
| Quotations and orders | Drafts, item/tax pricing, controlled specifications, immutable PDFs, withdrawal/amendments, accepted commercial orders and line artwork linkage | Phase 3 manufacturing/inventory/finance/delivery |
| External providers, production and billing | Mail/OIDC adapters require setup; production/payment/delivery stages display their unstarted state | Real integrations, manufacturing, inventory, invoices and dispatch |

**Local development candidate:** customer/staff identity, owner MFA, contact/privacy request flows, quotation and proof collaboration are implemented. Provider configuration, complete abuse/privacy fulfillment, tested backups and deployment hardening remain open. Public intake defaults disabled outside explicit local development. Demo routes are disabled by native startup and excluded by the production profile. Use synthetic data until launch controls are verified.

## Important routes

- `/` — customer landing page
- `/request-quote` — local demo enquiry form
- `/track` — secure token-gated enquiry progress
- `/business` — session-protected sales workspace/sign-in
- `/api/public/catalog` — product catalogue API
- `POST /api/public/enquiries` — local intake (feature-gated)
- `GET /api/public/enquiries/{publicId}` — requires `X-Tracking-Token`
- `/api/v1/auth/*` and `/api/v1/staff/enquiries` — staff sessions and scoped lead workflow
- `/api/demo/enquiries` — legacy **dev/QA only**, disabled by default; explicit synthetic regression use only
- `/actuator/health` — Spring Boot health endpoint

## One-command scripts

- `Start-PrintFlow.ps1`: starts native services; initializes local tools/data/credentials and builds when needed.
- `Stop-PrintFlow.ps1`: stops only project processes and preserves database files; `-KeepDatabase` keeps PostgreSQL running.
- `scripts/Use-PrintFlowTools.ps1`: dot-source to discover Java 21 in the current shell.
- `scripts/Test-Backend.ps1`: Gradle build, unit tests and PostgreSQL HTTP/security integration tests.
- `python scripts/verify_staff.py --restart`: staff persistence, session revocation and public tracking after a native restart.
- `cd frontend; npm run test:e2e`: desktop/mobile Edge workflows. Load local staff bootstrap credentials into the process environment first; see `scripts/Test-Browser.ps1`.
- `scripts/Test-Browser.ps1`: loads local credentials privately and runs browser checks against the running app.

Legacy `scripts/smoke.ps1` / `verify_foundation.py` require explicit `-EnableDemo` startup; the current staff UI never uses a demo key. See the development guide for temporary production-profile checks. Follow the native-only infrastructure handbook; do not use Docker for the active development/deployment plan.

## Repo documents

- [Phase-wise development handbook](docs/DEVELOPMENT_HANDBOOK.md) — phases 0–7, current Phase 1 slices and acceptance gates.


- `CODEX_MASTER_PLAN.md` — master specification & phase prompts for Codex
- `docs/SRS.md` — functional and non-functional requirements
- `docs/ARCHITECTURE.md` — boundaries, component decisions, data and security
- `docs/FLOWCHARTS.md` — Mermaid diagrams of business, deployment and interactions
- `docs/ENVIRONMENTS.md` — Windows, DEV, QA, UAT, PROD, CI/CD, backups
- `docs/INTEGRATIONS.md` — social, WhatsApp, email, phone and external vendor interfaces
- `docs/QA_TEST_PLAN.md` — tests, acceptance gates, threat cases
- `docs/SCREEN_MAP.md` — customer, owner, worker, vendor UI inventory
- `docs/PHASE_BACKLOG.md` — epics, milestones, dependencies, exit criteria
- `docs/DISCOVERY_QUESTIONS.md` — owner interviews, printing equipment, quote rules, and open decisions
- `docs/ADR/` — architectural decision records
- `docs/API_CONTRACT.md` — current working APIs and planned conventions

## Verification and known limitations

Java 21 / Gradle 9.8.2 / Spring Boot 4.1.1 / Next.js 16 / PostgreSQL 17. Native foundation evidence is in `docs/PHASE0_VERIFICATION.md`; staff-slice evidence is in `docs/STAFF_SLICE_VERIFICATION.md`; owner access evidence and limits are in `docs/OWNER_ACCESS_VERIFICATION.md`. All requirement IDs remain tracked in `docs/REQUIREMENTS_TRACEABILITY.md`. No remote CI run, production deployment or live provider connection is claimed.

## License and operations

This is starter code for customization. Verify licences and security updates for dependencies before deployment. Do not commit `.env` or real customer files. See `docs/ENVIRONMENTS.md` for operational hardening and deployment limitations.

Quotation development continuation: see [Phase 2 status](docs/PHASE2_DEVELOPMENT_STATUS.md), [updated handbook](docs/DEVELOPMENT_HANDBOOK.md) and [owner-notes product vision](docs/PRODUCT_VISION_FROM_OWNER_NOTES.md).

Design uploads and proof collaboration: [Phase 2B status](docs/PHASE2B_DEVELOPMENT_STATUS.md), [native scanner/private file setup](docs/DESIGN_PROOF_SETUP.md). Current local Defender scans fail; uploads remain quarantined until successful native scanning.

Customer/quotation refinement: [current behavior and verification](docs/LIVE_WORK_REFINEMENT_STATUS.md). Accepted quotations open Live works; account settings are in the profile popup; native AV/mailbox setup remains pending.

## Customer review workspace
Stage-wise reference/proof visibility, eight fictional sample categories, page/image comments, annotated review-copy emails and a responsive desktop work/conversation grid are available for native review. Details and focused Gradle/browser checks: [customer review workspace](docs/CUSTOMER_REVIEW_WORKSPACE.md).
