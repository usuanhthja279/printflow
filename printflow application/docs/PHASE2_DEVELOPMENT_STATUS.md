> **Superseded slice report:** Phase 2 commercial/design implementation has since been completed for owner review. See PHASE2_COMPLETION.md for drafts, item pricing, PDFs, withdrawal/amendments, commercial orders and final checks. The validity/expiry and scanner limitations below are historical and have been superseded.

# Phase 2A.1 development candidate

23 October 2026 — native Java 21/Gradle/PostgreSQL/Next.js; no Docker.

## Delivered

Manual all-inclusive INR quotations with immutable numbered versions and print specification snapshots; optional customer-requested date, proposed delivery/collection date, inclusive IST validity and commercial terms. Issuing supersedes the previous open offer. Expected-version checking and enquiry-row locks prevent races. Owners/assigned salespeople may issue; linked customers can accept an exact current unexpired version or request changes. Acceptance preserves the proposed date as the agreed commitment. A repeat acceptance adds no duplicate event; accepted offers cannot be overwritten. Database triggers protect commercial history and response events.

Staff enquiry popup and customer enquiry details expose the complete slice. Guest enquiries remain supported and require secure account claiming before portal acceptance. Customer display excludes private assignments/follow-ups. Readable specification labels replace raw snapshot JSON.

## Actual verification

- Gradle build compiled/package succeeded and 17 unit checks passed. The first integration compilation found an accidentally copied helper referencing an unavailable mail fixture; it was removed. The subsequent focused `integrationTest --tests com.printflow.QuotationIntegrationTest` passed all four tests.
- HTTP integration checks: exact-version approval, stale superseded rejection, repeated approval, accepted-version protection, database immutability, customer isolation, assignment/role restrictions, missing CSRF, amount validation, unassigned sales claim requirement, change requests, expired acceptance and concurrent issue with exactly one winner.
- Frontend production build/type compilation passed. Rebuilt successfully after the final readable specification display refinement.
- `scripts/Test-PhaseOneBrowser.ps1 -Project desktop`: one real native-backend journey passed in 22.4 seconds (workflow 20.2s; fixture preparation/cleanup separate). Separate synthetic owner/customer sessions issued INR 1,250.50, accepted v1, checked agreed date and prevented revised issuance. Existing registration, enquiry/profile, CRM, snapshot/privacy and recovery steps also passed. Synthetic accounts were disabled and private fixture file removed afterward; actual owner MFA unchanged.
- Desktop quotation screenshot inspected. Mobile quotation acceptance and final display refinement have not had a separate browser walkthrough in this iteration. The final refinement is presentation-only and passed the production build.

## Boundaries and next work

This is the first Phase 2 slice, not all of Phase 2 or a production release. Still pending: editable drafts, rich line/tax pricing and documents, quote withdrawal, accepted-order change agreements, designer uploads/revisions/comments/proof approval, order conversion and production release. Price is a staff-entered all-inclusive total; no automatic costing or tax calculation is claimed. Payment rules and document timing need real business policy confirmation. No external message/payment/provider was sent or activated. Existing native QA, backup, privacy/provider and deployment gates remain open.

## Manual review

Owner: open an enquiry, scroll to Quotations & delivery agreement, enter scope/total/date/validity/terms, review and issue. Customer: open the linked enquiry, review the exact version and specifications, then request changes or agree and accept. Owner: reopen details to see accepted agreement. Use synthetic data only. Accepted quotations cannot be amended until the explicit change-agreement slice exists.

24 October: the next design-proof slice is implemented as a development candidate. See PHASE2B_DEVELOPMENT_STATUS.md and DESIGN_PROOF_SETUP.md; native scanner clearance is still outstanding.
