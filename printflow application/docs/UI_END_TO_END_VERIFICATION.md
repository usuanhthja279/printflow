# Focused native UI and mail verification — 24 October 2026

## Journey and evidence

One synthetic guest enquiry was exercised through the real Next.js UI and native Spring API using separate owner, assigned designer and customer browser sessions. There were no intercepted API responses, artificial CLEAN scan verdicts, Docker services or changes to the existing real order.

The focused desktop journey passed in 22.9 seconds (26.9 seconds including runner setup):

- Submit enquiry; owner accepts and provisions the guest account.
- Read the synthetic customer acceptance email, sign in with its temporary password, verify that normal customer APIs are blocked, choose a permanent password and give privacy consent.
- Issue quotation v1; show waiting for acceptance; customer accepts and reaches Live works.
- Assign the isolated designer, upload proof v1, pass real Windows Defender scanning and preview it inline as the customer.
- Add pinned artwork feedback, request changes, upload/scan proof v2, share and approve that exact replacement.
- Owner releases production and confirms quality completion, dispatch and delivery. Each milestone was checked in both owner and customer UI.
- Owner Email activity and outbox checks verify enquiry, acceptance, quotation, artwork request, review, customer comment, change request, proof approval and operational notifications. Initial runs corrected test selectors for historical previews and public/internal enquiry references; no application permissions were weakened.

`frontend/e2e/workflow-mail.spec.ts` is the repeatable mail-disabled synthetic journey, run with `scripts/Test-WorkflowMailBrowser.ps1`. It creates no actual print or courier job outside the local application.

## Focused Gradle checks

`./scripts/Test-PhaseTwo.ps1 -SkipBuild -TestClass com.printflow.WorkflowMailIntegrationTest` passed all four tests. Coverage includes repeated signup/latest-password verification, stale/replayed links, restricted temporary credential activation, existing password preservation, production proof/quote gates, scoped specialist permissions, stale/duplicate status actions, delivery issue holds, restarted review windows and idempotent completion.

## Live dispatcher check

For this explicit mail test, only the successful synthetic enquiry's generated customer/team outbox rows were routed to a dedicated plus-address alias of the owner's authorized Gmail mailbox. This preserves a fictional customer identity and avoids using or resetting the owner's real customer account. The normal encrypted outbox, native scheduled dispatcher, Gmail STARTTLS transport, thread headers and owner Email activity are used. No historical business mail was replayed. Temporary access credentials were already invalidated by successful activation before the test messages were sent.

The real 48-hour rule was not shortened. Only the synthetic delivered record's stored timestamp was advanced to verify the native scheduler across service restart; it reached CLOSED / COMPLETED without an open issue. All real order timestamps remained unchanged.

`frontend/e2e/workflow-smtp.spec.ts` is guarded separately and requires explicit fixture/mail-test flags plus the private evidence metadata and authorized test recipient. It checks owner-visible SENT metadata and customer-visible Completed after the synthetic clock test. SENT means Gmail SMTP acceptance, not proof of inbox placement or reading.

## Limits

Production, quality, dispatch and delivery remain manually confirmed milestones. Phone photos, GPS evidence, multipage in-app PDF annotation and inbound Gmail reply synchronization remain outside the implemented flow. The exercised proof previews were PNG images. Physical printing and transport were simulated through clearly labelled local test notes.

Final SMTP/UI result and cleanup:

Final result: the owner SMTP/completion UI check passed in 7.5 seconds (12.3 seconds including runner setup). All 25 notifications across 15 event kinds reached SENT on their first attempt, with no failure code: ENQUIRY, NEW_ENQUIRY, INTAKE, QUOTE, ARTWORK, QUOTE_ACCEPT, REVIEW (two revisions), COMMENT, PROOF_CHANGE, PROOF_OK, PRODUCTION, READY, TRANSIT, DELIVERED and COMPLETED. Customer/team audiences are distinct. Twenty-four went to the dedicated test alias; the final owner completion notification used the configured owner mailbox. Gmail SMTP acceptance was checked, not mailbox Inbox/Spam placement.

The first SMTP UI wait was shorter than the cumulative native batching/transport time and timed out while unsent rows still had zero attempts and no failure code. The check's wait allowance was increased; no email was resent to force a pass and no dispatcher security or status gate was relaxed. The final check verified both all-SENT owner table rows and the customer's Completed state/closure message.

The native stack remains running with real mail enabled. Temporary fixture accounts/credentials and interrupted browser attempts were cleaned or archived; the existing business order was preserved. Synthetic mail metadata remains as audit evidence. No application behaviour change was required by this journey; the changes are focused regression tests and verification documentation.

## Focused workflow UI corrections — 25 October 2026

The customer artwork workspace now uses the stable `workflow.fulfilmentState` code to decide whether the proof workspace stays expanded. It is expanded while fulfilment is `NOT_STARTED` and moves into collapsed review history after fulfilment begins. The customer next-action card now identifies `OVERPAID` and `TERMS_PENDING` explicitly when a job is `READY`, explaining that business reconciliation or terms confirmation is required before dispatch. Balance-due and verified-payment messaging remain distinct. No backend transitions, API contracts, or authorization/payment gates changed.

Verification:

- `npm.cmd run test:e2e -- e2e/workflow-redesign.spec.ts --grep "artwork workspace expands|ready-for-dispatch payment exceptions"` — passed all 4 desktop/mobile checks.
- `npm.cmd run build` — passed, including TypeScript and static page generation.
- `./scripts/Test-PhaseTwo.ps1 -SkipBuild -TestClass com.printflow.LiveWorkIntegrationTest` — Gradle `BUILD SUCCESSFUL`; all 4 LiveWork integration tests passed using native PostgreSQL. No Docker was used.

These checks cover the two corrected UI decisions and the existing work API projection that supplies workflow codes. Full end-to-end business verification remains documented above; these targeted corrections did not repeat that full journey.
