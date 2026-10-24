# Implementation handoff — 24 October 2026

The owner requested implementation first, minimum further tests and manual review after completion. This covers the current customer → business → designer → payment → delivery workflow and its reference-inspired UI. Production release is not part of this work.

## Available to try

- Public homepage: interactive print families, finish exploration and responsive print mockups. Live enabled catalogue search/filtering, configured product specifications and product-specific quote links.
- Quote request: guided/advanced specifications, unknown/advice choices, a live brief summary and reference attachment capture. Signup verification and intake-created customer account activation remain part of the existing workflow.
- Customer/business: stage-aware enquiry views, status filters, readable progress, quotation item/terms controls, PDF previews and earlier-enquiry linking modal. Real scoped dashboard counts.
- Artwork: fictional samples across eight categories, category/search selection, copying to the enquiry, uploaded image/multipage PDF preview, location/page comments, annotated review copies and designer review requests. A reference is not a final approved proof.
- Designer: scoped assigned-work summaries/search, reference coordination, proof revisions and the project conversation beside the review workspace on wide screens.
- Accepted work: full-width desktop workspace, quotation → design → payment → production → delivery sequence, clickable section links, opening of linked quotation details, aligned history and conversation/email history on the right. Phone layouts stack content and provide a navigation menu.
- Conversation: message/email shortcuts, customer/team styling, empty state, character counter, send/refresh feedback and outgoing mail state. External mailbox replies are not synchronized.
- Payments: owner-agreed advance and balance/credit rules, verified offline receipts/corrections, Razorpay TEST checkout, callback/webhook capture verification and deduplication. No live-money activation.
- Invoices/fulfilment: immutable commercial PDF preview/download and email attachment generation. Approved proof, accepted quotation and current advance rules gate production; invoice and balance or approved credit gate dispatch. Recorded production, QC, transit and delivery milestones are available.

## Start manual review

Open http://127.0.0.1:3000. Explore Products, then request a quotation with a reference or select a sample when artwork is requested. Accept the enquiry in the business workspace, issue a quotation, activate/sign in as the customer and accept it. Assign the test designer; request a revision with a pinned comment, then approve the new proof. As owner, agree the advance rules, record a verified manual payment or use Razorpay TEST checkout as customer, issue the invoice and proceed through production/delivery. Balance or explicitly approved credit is required before dispatch.

Real mail remains restricted to the previously approved private test recipient. Synthetic automated journeys disable SMTP. Windows user environment variables provide gateway credentials. Never include their values in bug reports or screenshots.

## Minimum verification and known limits

The last focused Gradle run passed InvoicePdfTest, QuotationMailTest and WorkflowMailIntegrationTest. Public desktop/phone interactions and the earlier manual-receipt workflow passed. Final UI changes receive one frontend build/type check and service readiness check; broad suites are not repeated.

The latest Razorpay UI attempt reached test-bank selection and stopped on a provider button selector timeout. Captured provider payments, external captured webhooks and invoice inbox delivery are unverified. Keep these in manual testing. Earlier screenshots precede final navigation/conversation polish.

Remaining larger features: GST invoices/financial-year numbering, credit/refund notes and settlement reconciliation, incoming mailbox sync, official WhatsApp, stock/reservations, detailed order-line routing/capacity and GPS. Automatic designer allocation, project-level production queues and private transit/delivery photos were subsequently implemented in OPERATIONS_WORKFLOW.md. The 88-view reference illustrates future modules; this implementation does not advertise them as working.

Final checks: `npm.cmd run build` passed compilation, TypeScript and all page generation. `python scripts/static_validate.py` passed 39 expected-file/config/schema/document checks. The first build met a Windows lock on the running standalone directory; the project processes were stopped once, then the build passed. No backend code changed in this final polish, so the existing 16 focused Gradle cases were reused. Native startup with `./Start-PrintFlow.ps1 -SkipBuild` completed successfully.
