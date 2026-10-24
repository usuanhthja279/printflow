# Customer artwork review workspace — 24 October 2026

The remaining browser comments are implemented as one enquiry-to-design flow. Native Java 21/Gradle/PostgreSQL and Next.js; no Docker.

## Customer behavior

- Request quote keeps guided advice and recoverable initial artwork uploads. A saved enquiry is never resubmitted merely to retry its attachment.
- Enquiries show readable Status plus Stage / next action. Review and quotation preparation identify the business as the next actor; after a quotation change request the stage returns to business quotation preparation.
- Existing reference files remain visible before quotation. Designer proofs are hidden until an accepted quotation. Further customer reference upload, sample selection and annotation open only for an issued quotation with a business artwork request, or an accepted quotation. Final proof approval, production release and closure make this reference workflow read-only.
- Link an earlier enquiry is a button beside Request a quote and opens an accessible modal. Its verified-email and tracking-token checks are unchanged.
- The fictional sample catalog has one authored layout for each of eight product categories. No previous customer’s files are published. Selecting a layout makes a separate, normally scanned REFERENCE artifact in this enquiry.
- PDF and image references support location comments. PDF pins include their page. A review request saves the note, all pins and the exact source hash immutably. It is a request for designer work, not a source-file edit or proof approval.
- Business reference review and owner designer assignment are available in the quotation stage, before customer acceptance.
- Notifications include the customer, configured owner and assigned staff/designer. They contain the review details and a generated annotated PDF copy. The original artifact remains unchanged. A local-only designer username cannot receive real email until replaced by a real address, but can read the review in the assigned workspace.
- The work page uses available desktop width, important work on the left and project conversation/outgoing email history on the right. Small screens stack the conversation below the work. Navigation links jump to artwork, fulfilment, quotation and history. Progress always reads Quotation → Design → Payment → Production → Delivery. Payment remains honestly labelled Not recorded until payment functionality is implemented.
- Work log and commercial agreement disclosures have consistent panel padding/alignment. Portal messages queue project notifications to the customer and assigned team. Outgoing email history exposes subject/event/delivery state, not account tokens, recipient lists or decrypted mail. External mailbox replies are not imported; the shared portal conversation is the supported in-app exchange.

## Earlier comments retained

Business queue status filtering, additional quotation items, semicolon-separated commercial-term tags/common choices, and authenticated multipage quotation PDF Preview remain available. Sending a quotation change request now refreshes the enquiry instead of navigating prematurely into live work. Refresh enquiry details reloads current stage and reference controls.

## Backend and privacy

V21 records team audience explicitly so staff/designer mail links to /business while customer mail links to /customer. V20 widens descriptive mail event names to 32 characters. V19 adds immutable reference_feedback (source ID/hash, note, page pins, review PDF/hash) and mail_outbox.feedback_id. The server validates ownership/assignment, stage, clean REFERENCE kind, page bounds, finite normalized coordinates and payload limits. Review documents and sample endpoints use existing enquiry ACLs; review documents are no-store, inline PDF, nosniff and sandboxed. Review downloads and email attachments verify the stored copy hash.

PDF exports run in a child JVM with a 192 MiB heap, two concurrent slots and 20-second timeout. Sources are already scanned and hash-verified. Output uses a new PDF document with source page rendering resources but without original page annotations, additional page actions or document open actions; numbered marks and Unicode comment annotations are added. Images are checked before decoding (25 million pixel limit). Output is limited to 15 MiB. Job files stay in the private artifact directory and are removed. Unsupported/oversized exports fail without recording a partial review or queuing its emails.

## Focused verification

ReferenceReviewTest checks all eight sample images, multipage/Unicode PDF pins, unchanged source bytes, discarded document actions, image export and invalid-input cleanup. QuotationMailTest checks exact review-copy MIME attachment and hash failure. DesignProofIntegrationTest adds artwork-request stage gating, CSRF/other-customer denial, immutable records and designer notification binding. Existing design/work integration checks protect approval and conversation scopes.

The mail-disabled browser harness runs review-workspace.spec.ts with -Project both for desktop/mobile: pending-stage visibility, linking modal, common terms, business artwork request, sample selection, image pin, PDF page-two pin, annotated preview, assigned-designer notification, quotation acceptance, progress sequence, conversation and responsive layout. Synthetic accounts/mail are cleaned before restoring normal email delivery. Final outcomes are recorded after verification; no real customer mail is sent by these tests.

Desktop and phone review checks passed together: 23.7s and 26.2s (54.6s runner). The phone viewport capture check also passed (24.9s / 27.4s runner), and actual viewport screenshots were inspected. The stitched full-page mobile capture contains a browser capture artifact; use the viewport evidence for visual review. The reference copy preview was inspected and its numbered pin rendered correctly. Real SMTP remained disabled.

Final native evidence: all three ReferenceReviewTest cases and all five QuotationMailTest cases passed; the focused reference-stage/scope/designer-notification case passed including the final-proof lock. Fourteen existing design/work cases passed in the earlier regression run. The desktop account → quotation preview/acceptance → real Defender scans → proof change/approval → production/QC → transit/delivered journey passed on the final app in 17.1s (19.3s runner). No real SMTP was used in browser tests; synthetic accounts, records and unsent payloads were cleaned before restoring configured delivery. Native services and public delivery status are checked on restoration.
