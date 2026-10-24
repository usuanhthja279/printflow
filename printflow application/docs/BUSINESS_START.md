# J.D.Printex — business scope and development starting point

Reviewed 23 October 2026. PrintFlow is the internal project name. Native foundation verification and the first staff slice are now complete locally; this remains a development prototype.

**Later same-day update:** the owner requested Gradle instead of Maven and native development instead of Docker. Native Phase 0 verification is now complete; the first staff login/follow-up slice has been implemented and passed its initial unit, PostgreSQL security/workflow and desktop/mobile checks. See PHASE0_VERIFICATION.md and STAFF_SLICE_VERIFICATION.md for current evidence. The original review findings below are retained as the starting assessment, not current claims that these fixes remain blocked.

## Confirmed business direction

The owner confirmed the display name **J.D.Printex**, offering shaadi/wedding cards, hotel printable items, pamphlets, leaflets and offset gloss printing. The objective is business growth, without a specific operational bottleneck identified yet. Location, company registration, contacts, prices, actual equipment and service capacity are unconfirmed. Menus and table cards are illustrative hotel enquiry types, not a confirmed exhaustive service list.

## Original Phase 0 starter assessment

The existing Java/Spring Boot, Next.js/TypeScript and PostgreSQL starter is a useful foundation. Continue this codebase rather than duplicating the application. It already has a public homepage, catalogue, enquiry form, token tracking, a local demo owner board, Flyway schema and CI definitions. Static code review shows enquiry creation and its initial event share a transaction, tracking tokens are hashed, public tracking uses a separate data projection, and demo endpoints are excluded by the production profile. These properties have not yet been verified against a live backend here.

There are no real customer/staff accounts, itemized quotes, artwork uploads/approvals, orders, production jobs, inventory, invoices, payments, dispatch records or connected communication providers. A QUOTED enquiry status is only a label; it does not create a quotation. The comprehensive documentation describes a target system, not completed features.

## Complete business process to implement

1. **Discover and enquire:** customer finds a relevant product/service, selects it and requests a quote. Staff can later record walk-in, phone and messaging enquiries with source attribution. Record product, quantity, size, paper/finish needs, artwork readiness and required date; allow customers to ask for guidance. Current code captures only a subset, with no structured deadline or artwork readiness.
2. **Qualify and follow up:** sales confirms contact details, feasibility and missing specifications, assigns responsibility and records the next action. No silent merging of customers from matching phone/social details. Assignment, source and follow-up scheduling are absent today.
3. **Estimate and quote:** estimator checks paper, plates, waste, press time, finishing, outsourcing and delivery; an authorized person approves the itemized price and terms. Start with human-reviewed manual quotes. Customer approval must reference an immutable quote version. No prices or equipment capabilities should be invented.
4. **Prepare and approve artwork:** designer uploads private artwork, issues proof revisions and captures explicit approval of the exact file/version/specification. A replacement file requires a fresh approval. Current code has no file processing.
5. **Check release gates:** confirm approved quote and proof, the agreed advance or credit arrangement, material readiness and machine capacity. Any override needs an authorized actor and recorded reason. Keep commercial, payment, artwork and production states separate.
6. **Produce:** create jobs from approved order items, route through relevant prepress/press/finishing operations, assign staff, record output and waste, and support outsourced operations. One order may require several jobs. Vendors receive only their scoped files, deadlines and purchase costs.
7. **Inspect and rework:** record required checks, good/rejected quantities and rework decisions before accepting production. Exact checklists need owner/supervisor input by product.
8. **Invoice, pack and dispatch:** accounts issues the configured invoice and records authorized manual payments; reconcile remaining balance. Record pickup/delivery quantities and evidence, including partial dispatches. Tax and credit rules need accountant/owner confirmation before implementation.
9. **Close and retain:** reconcile quantities and financial balance, retain approved versions under a defined policy, and make repeat enquiries easy. Reminders and marketing require confirmed contact permissions; no automatic messaging is implemented.

## Initial review findings and priorities

- **Blocking — integrated execution unverified:** no accessible Docker engine and no Java/Maven on PATH. Frontend build success does not validate database migrations, Spring dependency resolution or API behavior.
- **Blocking for real users — identity and privacy:** the demo key is not staff authentication; public intake has no anti-abuse/consent controls. Retain the local-only boundary until Phase 1 controls and launch checks are complete.
- **High — business discovery:** catalogue availability, quote rules, advance/credit terms, actual machinery, outsourced steps and dispatch practice still need confirmation. Generic starter categories remain for compatibility and must be reviewed before launch.
- **High — concurrent status changes:** status mutation has no explicit row lock or optimistic version. Concurrent operators could overwrite a newer state or record conflicting history. Address with a database-backed concurrency regression test during stabilization, before multi-user use.
- **Medium — state retries:** CLOSED -> CLOSED incorrectly failed despite the idempotency contract. Fixed with early same-state return; regression tests added, but not executed because Java/Maven are unavailable.
- **Medium — frontend type error:** the owner board used a free-form status string where a fixed union was required. Fixed with the shared EnquiryStatus type; typecheck and production build now pass.
- **Medium — reproducibility:** package-lock.json was absent and CI/Docker used npm install. Lockfile generated from the registry and both switched to npm ci; clean dependency installation succeeded. Docker build itself remains unverified.
- **Medium — verification gaps:** smoke scripts cover a happy path; production profile, negative HTTP tests, restart persistence, browser behavior and database concurrency still need executable coverage/evidence.
- **Medium — structural validator versus local setup:** static_validate.py rejects any .env file although local startup creates an ignored .env. This archive-oriented assertion should be separated from normal local validation before using both commands together.
- **Medium — UI defaults:** the brochure-specific A4/170 GSM defaults did not fit all products. Size and GSM now start unspecified, finishing asks for guidance, and service links preselect known product codes. Product-specific fields remain Phase 1 work.
- **Documentation — provenance:** the supplied MANIFEST.sha256 belongs to the original package and no longer describes edited files. It is not current validation evidence. The folder has no Git repository; CI cannot run remotely until source control and a remote are configured.

## Initial branding work

Customer branding and metadata now use J.D.Printex. Homepage cards focus on wedding cards, hotel print materials, pamphlets/leaflets and offset gloss. The same codes exist in backend and fallback catalogues; old codes remain accepted, so existing records do not need a migration. Homepage service links preselect the enquiry product. The website labels later workflow capabilities as planned. Business answers are recorded in DISCOVERY_QUESTIONS.md.

## Development order

**Finish Phase 0:** restore a working Docker Desktop Linux engine or install Java 21/Maven with a PostgreSQL instance; run the documented build, migrations, API/security negatives and persistence checks. Add database concurrency coverage. Verify the new product-selection flow in a browser. Record actual results in PHASE0_VERIFICATION.md.

**Phase 1, after the repository's verification and explicit owner gate:** real staff identity/RBAC, secure guest intake, customer/contact records, source and follow-up workflow, and product-specific specification capture. This is the first useful staff-operated business foundation (FR-001–007, FR-062; later-phase requirements remain later).

**Phase 2:** manual versioned quotes, secure artwork/proof approvals and conversion to orders (FR-008–010, FR-014–016, FR-024–028).

**Phase 3:** production jobs, basic stock/material issue, quality checks, manual payment records, invoices and dispatch. Validate one complete synthetic enquiry-to-delivery scenario before a commercial pilot. Integrations and advanced automated pricing follow their existing later-phase gates.

## Growth focus and acceptance

Start by making services easy to discover and enquiries easy to submit. Prepare owner-approved portfolio examples, service descriptions and contact details when available. For wedding cards, capture event deadline and artwork needs; for hospitality, capture item type and repeat requirements; for pamphlets/leaflets, capture size, quantity and finish. These are proposed fields pending discovery, not implemented features.

Once the secure CRM exists, measure enquiry source, qualified enquiries, time to first response, quotes sent, accepted quotes, repeat customers and on-time deliveries. Establish a baseline before setting growth targets. The current demo does not measure these metrics. Automatic follow-ups, SEO work, paid promotion and provider integrations need their own implementation and verification; none are claimed connected or complete.

The next owner decisions are service area/contact details; approved portfolio; most common quantities/specifications; who quotes and follows up; advance/credit and proof sign-off rules; and first pilot scope. Do not request credentials or real customer artwork in project documents.
