# PrintFlow Studio — Software Requirements Specification (SRS)
Version 1.0 / 2026-10-23 · **Target scope: phases 0–7**

## 1. Product intent and boundaries

PrintFlow is a configurable order, workflow and print production platform for a **single** small-to-medium offset printing company initially. It unifies locally sourced enquiries (phone, walk-in, email, WhatsApp, Facebook/Instagram, website), quote and negotiation, artwork/prepress proof approval, paper and press production, partner outsourcing, payments, delivery, repeat orders, customer/vendor collaboration and analytics. Later SaaS is optional, not part of MVP.

### Stakeholders and roles
Visitor; individual customer; customer-organisation administrator; owner/super admin; sales; estimator; designer/prepress; production manager; machine operator; inventory/procurement; accounts; delivery staff; invited vendor; system administrator. Every actor receives minimum rights to job/order records, with audience tagging on comments/files (CUSTOMER, INTERNAL, VENDOR).

### Global business rules
- All channels can create leads; customer can order without visiting website (staff-assisted workflow is a first-class path).
- **Quotation:** immutable accepted revision; pricing override approval based on limits; expiry and tax rules configurable.
- **Artwork:** approval linked to immutable version, content hash, specs, user identity and timestamp; late changes reopen approval.
- **Production:** order != production job; one order can have multiple items, split jobs and external operations; release checks proof, commercial and stock conditions.
- **Payments:** records are append-only ledger-like financial events and idempotent; confirmation by verified webhook or authorised accounts entry.
- **Delivery:** partial quantity and multiple dispatches permitted; accept completed only after QC and delivery/collection condition as configured.
- **Vendor:** least-privilege job-pack access, never internal selling prices/margin or other customer records.
- **Communications:** social profile != trusted customer identity; matching and consent require evidence and human reconciliation when ambiguous.

## 2. Functional requirements — authoritative IDs

### FR-001–013 · Identity, catalogue, CRM, sales
- **FR-001 P0:** Customer account creation through verified phone/email (Phase 1; Phase 0 is anonymous prototype).
- **FR-002 P0:** Allow guest quotation intake, then invite secure account activation.
- **FR-003 P0:** Store customer contacts, addresses, organization membership, permission to contact.
- **FR-004 P0:** Employee creates lead for call, walk-in, email or paper note, with source and responsible staff.
- **FR-005 P0:** Lead workflow with assignee, source, priority, next action, due date, activity history.
- **FR-006 P0:** Public product catalogue, categories, product landing pages and request quotation path.
- **FR-007 P0:** Dynamic product-specific options/validation with beginner/pro modes for print specifications.
- **FR-008 P0:** Attach artwork, reference pictures, customer instructions, technical docs to enquiry/item.
- **FR-009 P0:** Versioned quote creation, revisions, negotiations, validity and final approvals.
- **FR-010 P0:** Convert accepted quote into order with exact accepted specification references.
- **FR-011 P0:** Customer dashboard/search for enquiries, active/historic orders and milestones.
- **FR-012 P1:** One-click reorder from prior completed items and approved artwork with fresh quote.
- **FR-013 P1:** Corporate accounts with multiple contacts, price contracts, credit limits and approval roles.

### FR-014–023 · Pricing and quote intelligence
- **FR-014 P0:** Staff creates manual itemized quote incl quantity, paper, finish, delivery and taxes.
- **FR-015 P0:** Generate immutable branded quote PDF with revision, terms, expiry and tracking reference.
- **FR-016 P0:** Controlled specification vocabulary: paper type/GSM/size, colour coverage, press, binding, finishing.
- **FR-017 P1:** Configurable cost books for substrate, plates, makeready, press time, ink, labour and overhead.
- **FR-018 P1:** Determine imposition/yield, required sheets, wastage, duplex/configuration and job route.
- **FR-019 P1:** Multi-quantity and stock-size alternatives with per-unit price comparison.
- **FR-020 P1:** Calculation of gross margin, tax, discounts, transport, outsourced work.
- **FR-021 P1:** Escalated owner approval for discounts or margin below company thresholds.
- **FR-022 P1:** Quote vs actual cost per completed job with reasons for variance.
- **FR-023 P2:** Explainable cost-saving/configuration suggestions, confirmed by estimator.

### FR-024–032 · Artwork and prepress
- **FR-024 P0:** Private artwork upload with version and order-item linkage.
- **FR-025 P0:** Design assignment, deadlines, task notes and correction workflow.
- **FR-026 P0:** Secure proof viewer and external customer sharing.
- **FR-027 P0:** Explicit proof approve/reject/request-change with timestamp and identity.
- **FR-028 P0:** Immutable version history; previous approvals not silently overwritten.
- **FR-029 P1:** Page-specific proof comments, pin annotations and comment resolution.
- **FR-030 P1:** PDF checks: dimensions, bleed, fonts, color mode/profiles, effective image resolution, PDF/X where appropriate.
- **FR-031 P2:** Compare PDFs/raster previews of versions with clear limitations.
- **FR-032 P2:** Approved production file package with checksums, machine specs, restrictions and downloadable ticket.

### FR-033–045 · Production
- **FR-033 P0:** Production jobs/print tickets created from released order items.
- **FR-034 P0:** Job operations with assignees, due dates and dependencies.
- **FR-035 P0:** Start/pause/finish operation with quantity produced, waste and issue notes.
- **FR-036 P0:** Product routing: prepress, plates, press, drying, lamination, cutting, binding, QC, packing; configurable.
- **FR-037 P0:** Private staff comments, instructions, attachments, handover notes.
- **FR-038 P1:** Machine registry: printable area, colors, sheets, speed, setup, availability and downtime.
- **FR-039 P1:** Schedule against equipment capacity, calendar and job priorities.
- **FR-040 P1:** Reserve paper/consumables for job with shortages and release/override.
- **FR-041 P1:** Actual run/setup/downtime and material consumption recording.
- **FR-042 P1:** QC checklists and inspection acceptance/defect/rework loop.
- **FR-043 P1:** Partial production, multiple plants/vendors and subcontract workflows.
- **FR-044 P1:** QR job ticket and mobile-friendly operation scan.
- **FR-045 P2:** Optimization suggestion for compatible press runs, only with operator approval.

### FR-046–057 · Stock, finance and logistics
- **FR-046 P0:** Stock inventory and movement entry with reason and quantity.
- **FR-047 P0:** Packing list, pickup/dispatch, delivery confirmation and proof.
- **FR-048 P0:** Accounts staff manually record cash, UPI and bank payments with audit.
- **FR-049 P0:** Generate compliant configurable invoices, receipts, balances (validate jurisdiction and tax eligibility).
- **FR-050 P1:** Suppliers, vendors, agreed rates and qualification metadata.
- **FR-051 P1:** Purchase order, approval, goods receipt, subcontract expense.
- **FR-052 P1:** Stock reservations, reorder points, waste and procurement alerts.
- **FR-053 P1:** Payment links via supported provider, signed webhooks and idempotent settlement.
- **FR-054 P1:** Partial payments, refunds, credit notes, customer credit and ageing.
- **FR-055 P1:** Split dispatches, delivery attempts, courier partner and proof-of-delivery.
- **FR-056 P1:** Accounting export, Tally/other integrations only after confirmed API availability.
- **FR-057 P1:** Sales, quote conversion, profitability, overdue, waste and utilization dashboards.

### FR-058–071 · Communication, collaboration and administration
- **FR-058 P0:** All order-relevant message and activity events linked to one workspace.
- **FR-059 P0:** Internal tasks, assignments, deadlines, priority and reminders.
- **FR-060 P0:** Audience visibility control on notes: INTERNAL not returned to customer/vendor.
- **FR-061 P0:** Immutable audit record for security-sensitive changes, approvals and money.
- **FR-062 P0:** Employee identities and RBAC; fine-grained ownership/resource authorization.
- **FR-063 P1:** Email integration, deduplicated threads/attachments, drafts and authorized sending.
- **FR-064 P1:** Official WhatsApp platform, opt-in, templates, webhooks and conversation policies.
- **FR-065 P1:** Supported Facebook/Instagram and lead-ad integration, with permissions review.
- **FR-066 P1:** Vendor scoped work package, invitation, acceptance, due date, costs and completion.
- **FR-067 P1:** Task checklists, comments, shared materials, internal/external mentions and handovers.
- **FR-068 P1:** Post-delivery review request subject to customer permission and platform policy.
- **FR-069 P2:** Authorized cloud telephony provider, calls, call-status callbacks and follow-up.
- **FR-070 P2:** Campaign content and publishing with required platform permissions, approval, attribution.
- **FR-071 P2:** AI-assisted extraction and suggestions with evidence, edits and human sign-off.

## 3. Non-functional requirements

These are **proposed launch SLOs**, to be validated under representative load. No SLO in this document asserts measured results for the Phase 0 bundle.

| ID | Category | Acceptance target |
|---|---|---|
| NFR-001 | Availability | Target 99.5% monthly availability for initial production |
| NFR-002 | UX response | Typical page interactions p95 <= 2 s excluding large files |
| NFR-003 | API | p95 ordinary read API <= 500 ms under reference load |
| NFR-004 | Capacity | Reference model 50 workers, 500 active customers, 1,000 orders/month, validated with business |
| NFR-005 | Scale | Web/API stateless horizontally scalable; file and DB persistence external |
| NFR-006 | Security | TLS, RBAC, item authorization, safe auth sessions, encryption and rate limiting |
| NFR-007 | File safety | AV scan, sandboxed PDF, quotas, private storage and expiring links |
| NFR-008 | Resilience | Outbox and idempotent payment/webhook processing, retry with dead-letter reporting |
| NFR-009 | Backup | Automated encrypted DB/backups, separate restore check |
| NFR-010 | Recovery | Initial proposed RPO 1 hour, RTO 4 hours |
| NFR-011 | Auditing | Actor, action, resource, version and timestamp of meaningful changes |
| NFR-012 | UX | Mobile accessible customer PWA; touch-oriented production UI |
| NFR-013 | Accessibility | WCAG 2.2 AA relevant paths, manual + automated checks |
| NFR-014 | Language | English first, translation keys ready for Bengali/Hindi |
| NFR-015 | Quality | Typed APIs, modular code, Flyway, automated tests and code review |
| NFR-016 | Telemetry | Logs, traces/metrics, incident alarms; no PII/tokens in logs |
| NFR-017 | Portability | Native Java/Node/PostgreSQL on Windows DEV/QA and Ubuntu production; Gradle builds, no Docker |
| NFR-018 | Privacy | Consent/retention/deletion/export workflows, Indian legal review |
| NFR-019 | Integration | Provider outage isolated; queue, retry and visible state |
| NFR-020 | Continuity | Printable job tickets / manual outage procedure; reconciliation after recovery |

## 4. Feature acceptance templates

Every feature's story must include: actor, preconditions, trigger, inputs, field validations, API schema, state transitions, authorization matrix, happy path, negative path, audit event, notification, retry/idempotency if applicable, observability, fixtures, security threats, UAT scenario and rollback/recovery considerations.

## 5. Printing-specific validation rules

- Offer easy and advanced specifications; never assume a customer understands GSM, bleed or colour separations.
- Yield/imposition depends on press printable area, grip, grain, parent sheet, binding signatures, duplex handling and wastage; do not use a naive sheet formula for all products.
- Cost and delivery estimates should be owner-reviewed until verified against real production history.
- Printed colour cannot be guaranteed from arbitrary monitor-rendered PDF previews; request physical/calibrated proof if needed.
- Support product-specific dimension constraints, spot colours, plate counts, lamination, binding and folding.
- Signed off design, print quantity and finished size must match the job ticket's version.

## 6. Out of scope for Phase 0

Real users and staff accounts, real production deployment, payments, messaging, email/social integrations, PDF uploads, automated offset pricing, company financial reports, press scheduling, vendor access and customer personal data processing are not implemented. Phase 0 does include an isolated **demonstration enquiry CRUD slice** to prove the intended end-to-end technical integration.
