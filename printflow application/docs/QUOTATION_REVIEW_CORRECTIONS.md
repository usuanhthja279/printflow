# Enquiry and quotation review corrections — 24 October 2026

## Changes

Optional initial artwork uses private PDF/PNG/JPEG attachment after the enquiry is saved. The UI distinguishes an enquiry saved successfully from attachment/scan success, so an upload failure does not create a duplicate enquiry on retry. Guest upload requires the enquiry's tracking token plus CSRF, NEW status and a 24-hour window; five initial attachments maximum, 20 MiB per file and rate limits. Files stay inaccessible until a successful native scan. Guest upload attribution is explicitly unauthenticated, with no guest download authority.

Phone entry on Request a quote fixes +91 outside the ten-digit number input. Quotation paper/GSM and finished size suggest product-template options and allow custom dimensions/weights. Separate controlled sides/binding/finishing choices remain; duplicate free-text finish/binding is removed.

An issued quotation says In review — waiting for customer acceptance. No revised form is shown and the server rejects replacement while an ISSUED offer awaits a response. Withdraw or receive a customer change request first; those offers appear in expandable history. Explicit accepted-agreement amendments retain their existing safeguards.

If clean reference artwork is absent, quotation preparation offers Ask the customer to share artwork. A later request is available while waiting; an existing clean reference and a one-hour duplicate guard avoid redundant requests. The request is logged and queued for email. Initial acknowledgement and quotation emails carry a stable subject, root Message-ID, In-Reply-To and References. Actual Gmail client threading and delivery require provider configuration/testing; no inbound mailbox synchronization or WhatsApp send is claimed. Commercial terms are the agreement text, not automation instructions.

New quotation PDFs use branded section blocks, date and price grids, pagination and footers. Existing issued PDF bytes and hashes remain immutable. The sample includes no real customer data.

Owner Delete now archives accepted/closed work as well as enquiries, with a customer-visible cancellation/rejection explanation. Agreement and proof history are retained. Multi-select deletion is transactional: stale revisions roll back the batch; notification outbox entries deduplicate. Privacy administration remains available in a collapsed Administration section for customer export/erasure requests.

## Verification

Native Gradle build and 19 unit tests passed. Six quotation and four staff-scope HTTP/database tests passed; after adding atomic batch/thread checks the five staff-scope tests passed. Frontend production build/type check passed. Both pages of the synthetic grid PDF were rendered with Poppler and visually inspected; long-document pagination/unit checks passed.

The desktop quotation-review browser journey passed in 10.9 seconds including runner setup. It used the real native scanner, verified a denied wrong-token upload, the fixed phone prefix, specification suggestions, acceptance-waiting/revision history and bulk archive. An earlier test-only locator timeout was corrected by matching the artwork label including its help text and setting a short action timeout. Synthetic credentials were cleaned up. Native Java 21/Gradle/PostgreSQL; no Docker. Gmail/WhatsApp provider delivery is not configured or verified. Broader fulfilment work remains in FULFILMENT_WORKFLOW.md.

## Requested local data cleanup

Archived 225 other enquiries (including accumulated synthetic test records), preserving enquiry ed42979f-9099-461c-94a6-cef5a25fbfeb and its existing INR 59,000 quotation. Verified the owner queue contains exactly that one preserved enquiry. Customer explanations and deduplicated encrypted rejection/cancellation emails were prepared locally; real delivery was disabled. No quotation/proof/audit records were physically deleted.
