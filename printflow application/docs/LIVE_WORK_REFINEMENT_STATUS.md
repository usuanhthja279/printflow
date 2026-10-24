# Owner-requested customer and quotation refinement

24 October 2026 — J.D.Printex — native Java 21 / Gradle / PostgreSQL / Next.js. No Docker.

## Implemented

- Customer header quote shortcut and avatar chevron removed. The dashboard keeps its main request action. Default avatar uses local generated SVG geometry; private normalized PNG profile photos can replace/reset it.
- Contact details, photo controls, sign-in provider linking and privacy requests moved out of the dashboard into the profile popup. Designer public-name editing appears only in designer profiles, separate from private login identity.
- Quotation date output is explicitly YYYY-MM-DD; UI handles older date formats without Invalid Date. User-entered requested delivery date persists from enquiry to quotation. Otherwise staff enters it or uses today in Asia/Kolkata. Expiry input/rule removed; historical immutable validity data remains stored but does not expire offers.
- Sending a complete quote sets QUOTED. Exact portal acceptance, or evidence-backed phone/email acceptance recorded by owner/assigned sales, sets IN_PROGRESS. Staff cannot select QUOTED/IN_PROGRESS/CLOSED directly through the status route. Offline agreement is explicitly labelled with channel, evidence and recording actor; it does not impersonate a portal click or send an unconfigured email.
- Accepted quotations appear in Live works and open /customer/work/{id}: actual stage indicators, designer public name/configured business contact, shared portal conversation, public quote/proof logs and design review. Closed accepted work remains accessible through its work page. Production, payment and delivery are truthfully Not released / Not recorded / Not started until their workflows exist.
- Clean image proofs support private inline preview and normalized review pins attached to exact proof/page comments. Request changes sends that revision back for designer review. This is review markup, not vector/PDF source editing; approved file bytes and prior decisions remain immutable. PDFs remain downloads.
- Reject/remove requires a reviewed customer-visible explanation of 20..1000 characters. Removal archives CLOSED, clears reminders and preserves approvals/history. Customer details and secret-token guest tracking expose the explanation. Staff/customer lists sort closed rows last with a matching composite cursor.

## Actual verification

Gradle build and 17 unit checks passed. Focused LiveWorkIntegrationTest (5), QuotationIntegrationTest (4) and DesignProofIntegrationTest (5) passed together: 14 HTTP integration checks, including status gates, ISO dates, offline evidence/actor, historical no-expiry acceptance, customer scope, shared messages, private re-encoded photos, archival explanation/order and prior proof integrity. Final backend packaging passed after small image-metadata refinements. A separate raster preview dimension/scope check is recorded below when finished.

Six desktop/mobile customer-live-work.spec.ts checks passed in 23.8 seconds using synthetic API fixtures. They exercise clean dashboard/header, profile persistence/photo, live work/conversation/image pins, offline quotation and reasoned archive. Synthetic profile and review-pin screenshots inspected. Frontend production build/type compilation passed, including the dynamic work page. Initial TSX handler/fragment syntax issues were corrected before successful builds; no failing checks are being represented as passes.

The focused real-backend desktop journey also passed in 23.5 seconds (workflow 20.7 seconds; synthetic fixture setup/cleanup separate): registration, verification/login, profile popup persistence, enquiry/template snapshot, quote issuance and acceptance redirect to live work, contact/privacy review and password recovery. Synthetic browser accounts were disabled and the private fixture file removed afterward. The actual enrolled owner/MFA was not read or changed. A final presentation-only build hid irrelevant designer naming controls from customer profiles; no broad suite rerun was used for that refinement.

## Limits

Native scanner follow-up corrected the earlier malformed-PDF diagnosis: valid PNG/PDF uploads now clear the real Java/Defender adapter. See NATIVE_ARTWORK_VERIFICATION.md for the focused real journey and DESIGN_PROOF_SETUP.md for diagnostics. A nonzero scan still quarantines that file. Business mailbox sync is not connected; portal messages are not emails and existing SMTP alone does not import replies. Public contact fields appear only if configured; private staff login addresses are not exposed.

This is a development continuation, not formal manufacturing order conversion or production release. Payment/delivery execution, advanced PDF/vector editing, email integration, release/material/capacity checks and environment/backup/privacy release gates remain open. No real external message or payment was sent.

Final targeted check: `integrationTest --tests com.printflow.DesignProofIntegrationTest.rasterPreviewIsPrivateAndBoundsDecodedDimensions` passed (one HTTP check). It verifies hidden drafts, authorized published raster preview, cross-customer denial and rejection of a 36-million-pixel image header. Final Gradle bootJar and native restart succeeded.
