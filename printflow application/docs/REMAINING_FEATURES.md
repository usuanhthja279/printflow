# J.D.Printex — remaining development

24 October 2026. Follow DEVELOPMENT_HANDBOOK.md and requirement IDs in PHASE_BACKLOG.md. Native Java 21, checked-in Gradle wrapper, PostgreSQL and Next.js; no Docker. Current work remains a development candidate, not a production release.

## Available for local review

Owner/staff MFA and role scopes; intake decisions, assignment/follow-ups and Contacts grants; customer verification/recovery and intake-created account access; product templates; versioned quotations and immutable PDFs; item/rate/tax pricing, controlled print vocabulary, withdrawal/amendments, commercial orders and artwork-line links; private native-scanned artwork; designer tasks/assignment; multipage PDF/image preview, page/location comments, immutable proof approval and evidenced offline approval.

Customer enquiry stages, artwork-request visibility, earlier-enquiry linking modal, eight fictional sample categories and immutable reference-review copies are available. Business users can review reference comments and assign a designer during the quotation stage. Review notifications carry an annotated PDF and the appropriate customer/team workspace link. Accepted work has a wide desktop work/conversation layout, phone stacking and outgoing email history. See CUSTOMER_REVIEW_WORKSPACE.md and PDF_ARTWORK_REVIEW.md.

The bounded operational slice supports manually confirmed proof-gated production, QC/ready, dispatch and delivered events, delivery issue holds and a durable 48-hour completion timer. Customer/team lifecycle SMTP notifications and owner Email activity are implemented. Native Gmail SMTP delivery was configured and tested; automation uses disabled delivery and synthetic addresses. Incoming mailbox replies are not synchronized. See WORKFLOW_EMAIL_VERIFICATION.md and UI_END_TO_END_VERIFICATION.md.

## Remaining operational MVP work (Phase 3)

- PF-300 remaining: order-line job routing, QR/job-ticket PDFs, machine readiness/capacity and detailed QC checklists. Project-level printer queues, printing/finishing/QC/rework/holds and assignment are now implemented; see OPERATIONS_WORKFLOW.md.
- PF-310: stock, reservations, material issue/return and wastage records.
- PF-320 remaining: GST tax-invoice configuration, financial-year numbering, credit/refund notes, advanced allocations/reconciliation, accounts delegation and partial dispatch. Manual verified advance/balance receipts, immutable commercial invoice PDFs and production/dispatch payment gates are implemented; see PAYMENTS_AND_INVOICES.md.
- PF-330 remaining: GPS, partial/multi-stop delivery and complete pilot reconciliation/service handling. Assigned private pickup/transit/delivery photos, scanning, customer gallery and evidence gates are implemented. Milestones are staff confirmations, not automatic GPS tracking.

## Official service integrations (Phase 4)

Incoming Google Workspace/Gmail mailbox reply/thread sync; optional Google/Microsoft customer sign-in configuration; official WhatsApp notifications; live payment provider activation, refund/chargeback and settlement reconciliation. Razorpay test-mode checkout/captured-payment verification and signed callback/webhook deduplication are implemented with private test credentials configured; actual provider capture and captured public webhook delivery remain unverified. Show connected status only after configuration and a live provider check. The shared portal conversation works independently of mailbox reply sync.

## Later work (Phases 5–7)

Permission-based reuse/publication of actual customer portfolio work (current samples are fictional); direct source-file editing and advanced safe PDF preflight; vendor/procurement scopes; capacity/calendar and validated offset costing; margin/waste analytics; repeat-order tools; consent-based marketing and human-reviewed AI assistance. Multi-company SaaS is optional after the single-business workflow is established.

## Release gates

Owner acceptance, encrypted storage/backups and restore drill, retention/erasure execution, maintained scanning/detection validation, native CI/QA, monitoring and deployment hardening, provider consent/configuration and production security review. Current local clean-file checks are development evidence, not complete production certification.

Automatic designer allocation with owner availability/manual overrides is implemented. Reserved synthetic addresses use a separate TEST routing pool. See OPERATIONS_WORKFLOW.md and the screenshot evidence report for this slice.
