> Superseded dismissal rule: owner may now archive accepted/closed work with a cancellation explanation, retaining agreements. See QUOTATION_REVIEW_CORRECTIONS.md for the newer bulk-archive and quotation changes.

# Staff access and staged-work review — 24 October 2026

## Implemented for local review

- Incoming enquiry requirements and optional accept/reject comments are restricted to owner/designer. Owner selects an enabled reviewer; designer may take their own intake. Decisions preserve actor, time, assignment and comments in history.
- Sales sees only assigned reviewed enquiries. Printer, delivery and other specialists see role-matched assigned tasks, with restricted projections and server checks.
- Owner grants Contacts per staff member. Grant/revoke invalidates that member’s sessions; no customer/commercial access follows automatically from a task role.
- Owner queue Delete archives unaccepted work with a standard customer-visible explanation and deduplicated encrypted rejection email. Accepted work requires a reviewed resolution. Local real email remains disabled; no phone call is made.
- Enquiry popup progresses from intake to quotation to artwork/review to an approved-proof handoff. Assignment, reminders, agreements and logs are expandable. Customer work keeps agreement/log history collapsed.

## Verification

Native Gradle build and 18 unit tests passed. Four StaffScopeIntegrationTest HTTP/database checks passed after the final intake-audit/closure changes. Earlier focused regression passed six quotation and five live-work integration checks; eight design-proof integration checks also passed. Next.js production build/type check passed.

Desktop staff/stage browser check passed (one real journey, 14.1 seconds including runner setup). Two earlier harness failures were corrected: an asynchronous controlled checkbox needed a retrying state assertion, and the expected Contacts heading was “Customer contacts”. No server authorization was weakened. This deliberately checks the changed access/UI slice rather than repeating all prior proof-upload journeys. Synthetic accounts are disabled and the private credentials file removed by the harness. Real owner MFA and credentials remain unchanged.

## Remaining work

This is the first implementation slice of [the fulfilment workflow](FULFILMENT_WORKFLOW.md). Multi-page in-app PDF review/page comments, assigned in-app notifications/status requests, formal production release/QC, pickup/drop-off photos, optional device location and the durable 48-hour delivered-review timer remain planned. Generic task state does not confirm manufacture or delivery. Current PDF review uses private download; raster preview/pins work inline. Gmail delivery/provider configuration, wider release hardening and owner acceptance remain open.

Use Java 21, the checked-in Gradle wrapper and native PostgreSQL; no Docker.

Final native health: API UP; /business and /customer HTTP 200. Synthetic fixture cleanup completed.
