# J.D.Printex Workflow Redesign — Batch 4

## Scope and branch

- Repository: `usuanhthja279/printflow`
- Branch: `feature/workflow-redesign-batch4`
- Branch baseline for this acceptance pass: `c8f16a849316129b2f571c1f634be10d8876675e`
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

## Authenticated delivery-photo diagnosis and acceptance — 25 October 2026

**Status: focused acceptance passed** for the customer delivery-photo defect, Sales claim discovery, desktop/mobile lifecycle journey and delivery issue/completion regression. This is local test evidence only; the branch remains unmerged and undeployed.

### Diagnosis

The original failed browser request did not record an HTTP status or response headers, and the Spring log had no request-level entry or exception for that GET. Therefore the exact historical status and content type cannot be recovered. The artifact named in that earlier screenshot was `e5679dde-5b0c-477a-9da6-c0de2660b8ec`; its database record was DELIVERY/CLEAN, 27,013 bytes, and the stored file's SHA-256 matched the database value (`66cda6acf50aeefa288e9b02c694de3124949ef515925e7a9eadc62a4e9ab503`). This rules out a scan-pending state or a storage/hash mismatch for that artifact, but cannot identify the original HTTP failure code.

In authenticated reproductions, pickup, transit and delivery preview requests returned `200 image/png`; the image bytes loaded to natural width 960. The focused browser test also injected one controlled, one-time `503 text/plain` response for the delivery thumbnail. The UI retried once using the same authenticated preview route with a cache-busting attempt parameter; the retry returned `200 image/png` and the image rendered. This demonstrates recovery from a transient request failure. The evidence is consistent with a transient browser/network load race in the original failure; it does not prove the unavailable historical status was 503.

No authentication, authorization, scan, storage validation or server-side delivery gate was loosened. The client retry only repeats the existing private authenticated GET once. A second failure still displays the existing unavailable state and manual retry control.

### Changes in this correction

- `DeliveryPhotoPanel.tsx` retries a failed thumbnail or full-size preview once; after a second failure the current unavailable/manual-retry UI remains.
- `operations-flow.spec.ts` records artifact ID, HTTP status, content type, byte count and error body for preview requests; it asserts all three thumbnails and full-size previews load after delivery. A controlled transient 503 verifies the retry.
- `StaffEnquiryService` exposes reviewed unassigned leads to SALES staff for claiming. NEW enquiries and records assigned to other staff remain hidden. `StaffWorkflowIntegrationTest` checks visibility before/after claim for both staff members.

### Verification results

| Check | Actual command or observation | Result |
|---|---|---|
| Native backend | Existing PostgreSQL 17.11 on `127.0.0.1:5432`, Spring API `8080` health `UP`, Next.js `3000`; no Docker | **Healthy.** Existing local database was retained. |
| Frontend types/build | `npm.cmd run typecheck`; `npm.cmd run build` | **Passed.** Both ran after the code changes; build used the stopped-UI procedure to avoid Windows `.next/standalone` locking. |
| Focused Java/PostgreSQL regressions | `./backend/gradlew.bat -p backend --no-daemon integrationTest --tests com.printflow.StaffWorkflowIntegrationTest --tests com.printflow.WorkflowMailIntegrationTest.confirmedProductionDeliveryIssueAndCompletionQueueOneNotificationPerEvent` | **Passed.** Includes reviewed/unassigned Sales discovery and claiming, plus delivery issue/review-window completion and one-notification-per-event behavior. |
| Authenticated desktop journey | `PHASE1_BROWSER_FIXTURES=true npm.cmd run test:e2e -- e2e/operations-flow.spec.ts --project=desktop --max-failures=1`, with `BrowserFixtures prepare/cleanup` against native PostgreSQL and `APP_MAIL_ENABLED=false` | **Passed: 1 test.** Ran the full synthetic authenticated journey from intake through quote, account activation, artwork/proof, payments/invoice, production, dispatch evidence, delivery, customer gallery/full previews and owner email activity. Desktop viewport: 1440×900. |
| Authenticated mobile journey | `PHASE1_BROWSER_FIXTURES=true npm.cmd run test:e2e -- e2e/operations-flow.spec.ts --project=mobile --max-failures=1`, with the same native fixture prepare/cleanup | **Passed: 1 test.** Same full journey and private image assertions at 390×844. |
| Preview requests (desktop) | [`photo-preview-responses.json`](../output/batch4-visual-evidence/authenticated/desktop/photo-preview-responses.json) | Delivery artifact `e8bb247a-3a2a-44a2-b20f-d62d6f724bb1`: controlled first response `503 text/plain` (39 bytes), retry `200 image/png` (27,013 bytes). Pickup `19b2e37d-d259-4cec-a0e3-8db3cebad115`: `200 image/png` (27,067 bytes). Transit `acac1bc8-d631-4041-8ed6-40057774a8fa`: `200 image/png` (26,525 bytes). All thumbnail and full-size assertions passed. |
| Preview requests (mobile) | [`photo-preview-responses.json`](../output/batch4-visual-evidence/authenticated/mobile/photo-preview-responses.json) | Delivery artifact `f7062e18-cb32-4d62-8fb6-da34109b7dc2`: controlled first response `503 text/plain` (39 bytes), retry `200 image/png` (27,013 bytes). Pickup `996c5769-0961-4fda-8f6c-c28fc2c7041f`: `200 image/png` (27,067 bytes). Transit `7e1fa4b8-eb42-43bb-a1e8-35a58b0df4cc`: `200 image/png` (26,525 bytes). All thumbnail and full-size assertions passed. |
| Delivery issue and completion | Focused `WorkflowMailIntegrationTest.confirmedProductionDeliveryIssueAndCompletionQueueOneNotificationPerEvent` | **Passed.** This is an integration test, not a browser wait through the 48-hour customer review interval. |
| Sales discovery and claim | Focused `StaffWorkflowIntegrationTest` | **Passed.** A reviewed unassigned lead appears in both eligible Sales queues; after claim it leaves the other queue and appears as assigned to the claimer. NEW and another user's assigned leads remain excluded. |

All UI workflow fixtures and photo bytes are synthetic. Mail delivery was disabled; notifications were verified in the local outbox, not sent to real recipients. No real payment was made. The 48-hour window is covered by the focused integration test's controlled time path, not by waiting in a browser. Spring does not log every successful private image GET at INFO, so the HTTP instrumentation above comes from the authenticated browser response observer; no corresponding Spring error was present during the reproductions.

### Screenshot evidence and inspection

The full 40-view desktop run is retained under [`output/batch4-visual-evidence/authenticated/desktop`](../output/batch4-visual-evidence/authenticated/desktop/). The full 40-view mobile run is retained under [`output/batch4-visual-evidence/authenticated/mobile`](../output/batch4-visual-evidence/authenticated/mobile/). The browser results include `views.json`, `result.json` and `photo-preview-responses.json` for each viewport.

I inspected the desktop delivery gallery, full-size delivery preview and final email activity, and the mobile delivery gallery/full-size preview captures. The desktop gallery displays all three scan-passed milestones in aligned cards; the full-size modal contains the whole image with no clipping. Mobile gallery and preview fit the 390 px viewport and retain readable captions/actions. Email activity masks recipient values. Earlier screenshots in the run also show the server-backed delivery readiness and evidence gates; the journey confirms delivery, and the focused integration regression confirms issue and completion behavior.

The initial desktop retry without the fixture flag was skipped; the next launch attempt hit the documented Edge temporary-profile permission failure before test start. The same focused run was retried once with approved execution permissions and fresh native fixtures, then passed. These harness startup attempts did not reach or modify the application workflow.

The branch remains `feature/workflow-redesign-batch4`; it was not merged or deployed. No Batch 5 work was started.
