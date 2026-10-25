# J.D.Printex Workflow Redesign — Batch 4

## Scope and branch

- Repository: `usuanhthja279/printflow`
- Branch: `feature/workflow-redesign-batch4`
- Latest implementation commit: `8cb22ed62f75c92ea7f639c8590dbfbb7bf4ad6c`
- No merge, deployment, Batch 5 work or unrelated redesign is included.
- Local services use PostgreSQL 17 on `5432`, Spring on `8080` and Next.js on `3000`. Docker was not used.

## Batch 4 corrections already delivered

The payment ledger calculation and all backend workflow gates remain authoritative and unchanged.

- The payments API exposes `testPaymentsIncludedInTotals`, allowing the UI to state whether TEST receipts contribute to the displayed aggregate under the active profile.
- The fulfilment API exposes server-validated pickup and delivery evidence readiness (`SCANNED`, `WAIVED`, `PENDING`, `INVALID` or `NOT_APPLICABLE`). The UI distinguishes an authorized waiver from a successful scan and only advances Dispatch when the server readiness projection permits it.
- Existing payment authorization, invoice immutability, proof revision checks, production release, dispatch and delivery validation remain intact.

## Previously recorded verification

These are results recorded during the earlier Batch 4 correction pass. They are retained as historical evidence and were not all reproduced in the acceptance attempt below.

- `npm.cmd run typecheck` — passed.
- Focused synthetic finance, work, delivery, waiver and collaboration Playwright runs — previously recorded as passing in isolated runs. The scenarios use synthetic API fixtures; they do not establish a complete authenticated backend journey.
- `PaymentsProfileTest` and `WorkflowMailIntegrationTest.routingProductionQueueAndDeliveryPhotosRespectScopesAndGates` — previously recorded as passing against local PostgreSQL.
- `npm.cmd run build` — previously recorded as passing after the correction.
- Corresponding synthetic desktop/mobile screenshots and visual notes are retained in [`output/batch4-visual-evidence/README.md`](../output/batch4-visual-evidence/README.md). They are not real authenticated customer/order evidence.

## Latest authenticated acceptance attempt — 25 October 2026

**Status: INCOMPLETE; not ready for merge review.** This section supersedes the earlier acceptance attempt recorded above. Local service startup, builds, and focused backend tests completed. The real browser journey reached delivery and customer photo review, where a delivery-photo preview failure was exposed.

### Environment and safety

- Existing PostgreSQL 17.11 remained on `127.0.0.1:5432`; Spring was healthy on `8080`; Next.js returned HTTP 200 on `3000`. The API log confirms Flyway validated 25 migrations and used the existing `printflow` database. Existing PostgreSQL data was retained.
- The earlier `EBUSY` build attempt was caused by the running Next process holding `.next/standalone`. I stopped the application with the project stop script using `-KeepDatabase`, built, and started the app again. PostgreSQL was not stopped or recreated.
- `npm.cmd run typecheck` and `npm.cmd run build` passed. The build was run with the app stopped, avoiding the prior Windows file lock.
- Authenticated UI tests used newly generated synthetic staff/customer fixtures. `APP_MAIL_ENABLED=false`; outgoing notifications were checked as local outbox records only. No real email or payment was sent. Fixture cleanup archived only synthetic test enquiries and disabled the generated staff accounts. User accounts and unrelated records were not changed.
- SMTP and payment gateways were not exercised. No Docker was used. Login throttling and workflow authorization were not weakened.

### Focused code corrections

- `StaffEnquiryService.requireVisible` now permits SALES staff to access reviewed, unassigned enquiries so they can claim them, consistent with `docs/API_CONTRACT.md`. NEW intake and records assigned to another staff member remain hidden; owner behavior is unchanged.
- `StaffScopeIntegrationTest` now inserts a real synthetic issued quote row before queueing a quote notification, satisfying the existing foreign key. It also asserts the intended two-thread policy: enquiry notifications remain in the intake thread; the first quote starts a separate order thread; later order notifications reply within that order thread.
- `StaffWorkflowIntegrationTest` now leaves the assisted lead in REVIEWING after cleanup so subsequent ordered tests can verify the documented sales claim flow instead of receiving a misleading 404 from a reset NEW record.
- `operations-flow.spec.ts` now inherits the Playwright project's viewport for staff contexts, selects the matching desktop/mobile one-time recovery code, finds the tested job by its own production reference when other jobs share a queue, checks the printer's privacy-preserving “Production complete” state and uses the rendered delivered-stage punctuation.

### Verification results

| Check | Actual command or observation | Result |
|---|---|---|
| Frontend types | `npm.cmd run typecheck` | **Passed.** |
| Next.js production build | `npm.cmd run build` with the local UI stopped | **Passed.** |
| Focused Java unit tests | `./backend/gradlew.bat -p backend --no-daemon test --tests com.printflow.identity.LoginThrottleTest --tests com.printflow.customer.PaymentsProfileTest --tests com.printflow.customer.InvoicePdfTest --tests com.printflow.collaboration.CollaborationChannelInterceptorTest --tests com.printflow.collaboration.CollaborationBrokerTest` | **Passed: 17 tests, 0 failures/errors.** Includes the sign-in throttle suite; limits were not changed. |
| Focused Java/PostgreSQL integration tests | `./backend/gradlew.bat -p backend --no-daemon integrationTest --tests com.printflow.WorkflowMailIntegrationTest --tests com.printflow.StaffWorkflowIntegrationTest --tests com.printflow.StaffScopeIntegrationTest --tests com.printflow.DesignProofIntegrationTest --tests com.printflow.ProductionBoundaryIntegrationTest` | **Passed: 34 tests, 0 failures/errors**, against the native PostgreSQL database on port 5432. XML reports: `backend/build/test-results/integrationTest/TEST-com.printflow.{WorkflowMail,StaffWorkflow,StaffScope,DesignProof,ProductionBoundary}IntegrationTest.xml`. |
| Authenticated desktop journey | `PHASE1_BROWSER_FIXTURES=true npm.cmd run test:e2e -- e2e/operations-flow.spec.ts --project=desktop --max-failures=1` with native fixture prepare/cleanup | **Failed late, after 1.1 minutes.** It created and accepted a quote, activated a customer, assigned design, reviewed and approved a revised proof, recorded synthetic advance/balance entries and invoice, exercised production hold/print/finish/QC/rework/READY, blocked dispatch without pickup evidence, scanned pickup/transit/delivery photos, confirmed delivery and rendered the customer gallery. At gallery review, the delivery image showed **“Preview unavailable”** while pickup and transit images loaded. The expected delivery preview button was therefore absent. This is a product issue found by the test, not a passing journey. |
| Authenticated mobile journey | Prior mobile retry of this flow | **Failed before owner MFA**; only blank failure captures were produced. No mobile role flow or mobile screenshot is claimed. The mobile test was not retried after stopping to avoid repeated login attempts. |
| Browser service health after test | Existing local UI/API processes and startup log | **Healthy before run.** Spring log confirms PostgreSQL 17.11 at 5432; earlier probe confirmed UI HTTP 200 and API health UP. |

### Screenshot review

Thirty-four real authenticated desktop captures are retained at [`output/batch4-visual-evidence/authenticated/desktop`](../output/batch4-visual-evidence/authenticated/desktop/README.md). All are 1440×900 and contain fictional test data. They cover intake through delivery and each file's significance is described in the gallery index.

I inspected the READY/dispatch gate, the missing-pickup block and the customer gallery. The readiness panels are legible; dispatch remains blocked without a scanned pickup photo. The gallery correctly distinguishes scan-passed photos, but the delivery thumbnail is replaced by “Preview unavailable”. This also prevented inspection of full-size delivery preview and final email activity. Current mobile evidence does not meet the requested acceptance requirement.

### Remaining acceptance work

1. Diagnose why the customer's delivery-stage photo preview fails while the delivery record is scan-passed and the other milestones preview successfully. Add a focused authenticated regression asserting all three preview images load.
2. Rerun the desktop journey through full-size preview and email activity after fixing the gallery defect.
3. Run a controlled authenticated mobile journey with appropriate fixture handling, then inspect and retain actual mobile screenshots.
4. Recheck delivery review-window completion and customer issue flow using focused existing backend tests or a controlled browser fixture; the browser run did not wait 48 hours.
5. Update this report with the corrected preview and mobile evidence before requesting merge review.

The branch remains unmerged and undeployed. Backend security checks, payment calculations, and server-side production/delivery gates were not changed by this correction pass.
