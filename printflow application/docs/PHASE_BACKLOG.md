# Phase backlog, dependencies and release gates

Phases are **dependency ordered**, not promises of fixed durations. Use 1–2 week sprints and adapt based on the shop's actual workflows. IDs are suggested Jira/GitHub project issues.

| Epic | Phase | Feature slice | Depends on | Completion gate |
|---|---|---|---|---|
| PF-000 | 0 | SRS, architecture, ADRs and route inventory | Owner walkthrough | Written sign-off / assumptions log |
| PF-001 | 0 | Bootable Spring + Next + PG local stack | PF-000 | Health, migrations, navigation work |
| PF-002 | 0 | Thin enquiry workflow + demo owner update | PF-001 | DB persisted, tracking token, smoke tests |
| PF-003 | 0 | CI, scripts, QA/Prod environment design | PF-001 | PR checks and deployment docs |
| PF-100 | 1 | Identity: customer OTP, secure staff login, sessions, MFA admin | PF-001 | BOLA/CSRF tests pass, `/api/demo` removed |
| PF-110 | 1 | Role/tenant/company resource permissions | PF-100 | Role matrix enforced backend |
| PF-120 | 1 | Contacts, CRM, import/offline enquiry, tasks | PF-110 | Full employee-created enquiry path |
| PF-130 | 1 | Product templates, dimensions, options, validations | PF-120 | Owner approves real product configuration |
| PF-140 | 1 | Secure artifact storage, upload guardrails, consent and rate limits | PF-100 | Secure upload tests, privacy sign-off |
| PF-200 | 2 | Quotes and versions, manual item pricing | PF-130 | Accepted version immutable |
| PF-210 | 2 | Approval workflow, PDF documents, notifications interface | PF-200 | Signed accept/reject audit |
| PF-220 | 2 | Design requests, file revision and proof sign-off | PF-140,PF-210 | Spec/proof hash approvals |
| PF-230 | 2 | Convert accepted quote -> commercial order | PF-210,PF-220 | No manual reentry, correct totals |
| PF-300 | 3 | Jobs from order item, operation state, QR ticket basics | PF-230 | Approved order to shop-floor queue |
| PF-310 | 3 | Basic inventory, issue and QC | PF-300 | Waste/defect and handoffs tracked |
| PF-320 | 3 | Manual finances, invoices and split dispatch | PF-230,PF-300 | Closed pilot job and reconciled balance |
| PF-330 | 3 | Customer progress page and staff dashboard | PF-300 | Owner pilot acceptance; first MVP candidate |
| PF-400 | 4 | Outbox/inbox/event normalization, provider tokens and retries | PF-100 | Duplicate/outage tests pass |
| PF-410 | 4 | Email integration | PF-400 | Reply linked to order, send approved drafts |
| PF-420 | 4 | Official WhatsApp integration | PF-400 | Templates/consent/24h policy enforced |
| PF-430 | 4 | Payment provider links/webhooks | PF-320,PF-400 | Settlement idempotency / reconciling |
| PF-440 | 4 | Supported Meta Lead Ads / Facebook / Instagram | PF-400 | Approved scopes and lead conversion |
| PF-500 | 5 | Vendor invitations, permissions, scoped packages | PF-110,PF-300 | Outsourced job and data isolation |
| PF-510 | 5 | Procurement, reservations and machine calendar | PF-310 | Stock/booking concurrency tests |
| PF-520 | 5 | Advanced operator mobile queue and delivery | PF-500 | Multi-stage live job completion |
| PF-600 | 6 | Configurable offset-costing engine and validation fixtures | PF-130,PF-510 | Estimator signs off past job matches |
| PF-610 | 6 | Preflight worker, safe PDF sandbox and annotations | PF-140 | Safe PDF fixture suite |
| PF-620 | 6 | Margin/waste/machine and customer analytics | PF-600 | Reconcile dashboard to DB records |
| PF-700 | 7 | AI assisted enquiries/cost alternatives/production suggestion | PF-400,PF-600 | Human review and measured benefit |
| PF-710 | 7 | Marketing/review analytics, advanced publishing | PF-440 | Consent and provider scope checks |
| PF-720 | 7 | Multi-company SaaS exploration only if needed | All relevant modules | Dedicated tenancy/security review |

## MVP release gate (end Phase 3)

One fictional/pilot customer can submit or staff-enter an enquiry; seller issues quote; customer signs exact quotation and proof; responsible manager releases job; operator produces; QC checks; accounts invoices and records balance; customer receives delivery and sees accurate progress. Full access control, audit, backup and protection in place before any real users.

## Prioritization principle
Build functional end-to-end vertical slices first. No AI price guess, campaign scheduler, payment processor or public production environment before secure CRM, price versioning and operational workflows are validated.

## Authorized continuation — Phase 2A.1
Owner authorized Phase 2 development after the product-vision review. PF-200/PF-210 begin with bounded manually priced, immutable quote versions and exact-version portal acceptance, proposed/agreed date and change requests. Rich line/tax pricing, drafts, PDF, withdrawal, changes to accepted agreements, order conversion and proof workflow remain pending. Existing production/provider release gates remain unchanged.

## Phase 2B.1 continuation
Private bounded uploads, native scan/quarantine boundary, explicit clean-file publication, immutable proof revisions, comments and customer decisions are implemented as a development candidate. Owner-assigned designer queue, private raster previews and image pin comments are included. Real native PNG/PDF scan clearance is now verified; the earlier failure used a malformed PDF probe. PDF editing/rendering, comment resolution, offline proof approval evidence, designer deadlines, accepted order changes and order conversion remain open. See PHASE2B_DEVELOPMENT_STATUS.md, NATIVE_ARTWORK_VERIFICATION.md and REMAINING_FEATURES.md.

## Owner corrections — quotation-controlled live works
Delivered as a development candidate: quotation-driven QUOTED/IN_PROGRESS, evidence-backed offline acceptance, no expiry, ISO date rendering, closed-last/archive reasons, customer profile modal/photos, accepted-work page, public conversation/logs/designer display and image review pins. Native AV clearance is now verified separately. Formal manufacturing order conversion, production/payment/delivery operations, mailbox sync and vector/PDF editing remain pending.
# Current delivery — Phase 2 implementation

24 October 2026: PF-200/210/220/230 are implemented for owner review, including draft/itemized quotes, PDFs, withdrawal/amendments, immutable commercial orders/artwork linkage, designer deadlines, comment resolution, comparison and evidenced offline proof agreement. See PHASE2_COMPLETION.md. Earlier pending slice notes below are historical; owner review and deployment/provider/privacy/backup release gates remain separate. Next implementation phase after review: PF-300–330 production/inventory/finance/delivery. No Docker.

