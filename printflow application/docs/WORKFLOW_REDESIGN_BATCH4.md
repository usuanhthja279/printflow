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

## Final authenticated acceptance attempt — 25 October 2026

**Status: INCOMPLETE; not ready for controlled merge review.** The required authenticated printer-to-delivery journey and live-backend acceptance checks did not complete.

The earlier real-browser attempt reached customer activation, quotation acceptance, design assignment, proof feedback and approval, payment agreement, a verified advance, invoice issuance and owner production preparation. It stopped at printer login with the configured 15-minute sign-in throttle. This pass made no additional sign-in attempts, did not reset or weaken the throttle, and did not restart Spring to get around it.

A local synthetic checkpoint file exists at `.local/workflow-ui-evidence.json`, but the ignored `.local/browser-fixtures.json` required by `frontend/e2e/operations-flow.spec.ts` is absent. The stored checkpoint was not validated against PostgreSQL during this pass, so it must not be treated as a verified resumable order. The existing browser test also does not yet resume from that checkpoint; it starts its journey from intake when enabled. No new order or credentials were created in this pass.

### Current verification results

| Check | Command or observation | Result |
|---|---|---|
| Security/profile/PDF unit tests | `./backend/gradlew.bat -p backend --no-daemon test --tests com.printflow.identity.LoginThrottleTest --tests com.printflow.customer.PaymentsProfileTest --tests com.printflow.customer.InvoicePdfTest` | **Passed** (`BUILD SUCCESSFUL`). The throttle regression tests ran; no login throttles were changed. |
| Frontend typecheck | `npm.cmd run typecheck` | **Passed.** |
| Next.js production build | `npm.cmd run build` | **Failed** before compilation: Windows returned `EBUSY` while Next attempted to remove `.next/standalone`, which is in use by the running UI process. No Next.js build output or running app process was forcibly removed. |
| Focused synthetic Playwright check | `npm.cmd run test:e2e -- e2e/workflow-redesign.spec.ts --grep "failed scan and unpaid finance" --project=desktop --max-failures=1` | Initial Edge launch failed on temporary-profile permissions. The one approved retry launched Edge, but the page remained at “Opening your project…” and the expected next-action region never appeared. **Failed at the first next-action assertion.** |
| PostgreSQL integration checks | `integrationTest` for `WorkflowMailIntegrationTest.routingProductionQueueAndDeliveryPhotosRespectScopesAndGates`, `WorkflowMailIntegrationTest.confirmedProductionDeliveryIssueAndCompletionQueueOneNotificationPerEvent`, `StaffWorkflowIntegrationTest.disabledAccountsAndLogoutInvalidateSessions`, `StaffWorkflowIntegrationTest.repeatedLoginAttemptsAreRateLimited`, and `StaffScopeIntegrationTest.*` | **Failed at Spring context initialization** because Flyway could not connect to PostgreSQL (`ConnectException: connection refused`). These tests did not execute their assertions. |
| Local health | Read-only `GET http://127.0.0.1:3000/` and `GET http://127.0.0.1:8080/actuator/health` | UI returned **200**. API health timed out after 4 seconds. |
| Real authenticated multi-role flow | `frontend/e2e/operations-flow.spec.ts` | **Not run**: its ignored browser-fixture file is absent, and the earlier printer login was throttled. No role login was attempted in this pass. |
| Authenticated screenshots | Owner, printer, delivery staff and customer at desktop/mobile sizes | **Not captured in this pass.** Existing retained screenshots are synthetic fixture views only. |

The local PostgreSQL startup script reported readiness after automatic recovery of the existing data directory. The startup log recorded shutdown and recovery after the stale PID marker was cleared; subsequent integration tests still could not connect. Database-backed checkpoint validation, issue-open/completion-hold tests and PostgreSQL persistence acceptance therefore remain unverified. SMTP was kept disabled; no real payment, email, webhook or external delivery was performed.

## Remaining acceptance work

1. Restore stable PostgreSQL connectivity on `5432` and confirm the existing checkpoint against persisted order, quotation, proof, payment, invoice and production state without modifying it.
2. Add checkpoint-aware resume behavior to `operations-flow.spec.ts`; do not create duplicate intake, quotation or artwork when a valid checkpoint exists.
3. After the existing throttle has expired naturally and normal credentials/fixtures are available, continue with the assigned printer and delivery roles. Reuse authenticated browser contexts within the run and stop immediately if the throttle is still active.
4. Complete the production stages, invalid-evidence rejection, scanned pickup evidence, dispatch, transit, delivery evidence, customer gallery and review-window checks. Run the negative authorization and stale-revision scenarios using existing integration coverage.
5. Capture and inspect real authenticated desktop and mobile screenshots for owner, printer, delivery staff and customer. Keep them separate from synthetic screenshots and temporary Playwright output.
6. Rerun focused Playwright checks, the Next.js production build and the relevant Gradle/PostgreSQL integration tests. Update this report with actual outcomes before requesting merge review.

No code defect or security bypass was introduced during this acceptance attempt. The branch remains unmerged and undeployed.
