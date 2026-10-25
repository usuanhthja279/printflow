# J.D.Printex Workflow Redesign — Batch 4

## Baseline and scope

- Branch: `feature/workflow-redesign-batch4`
- Base: Batch 3B commit `d8b0421b07d0368367c634169852f34ccd863cba`
- Native services: PostgreSQL 17 on `5432`, Spring API on `8080`, Next.js UI on `3000`; Docker was not used.
- This batch reuses the existing finance, invoice, payment gateway, fulfilment, photo scanning and authorization APIs. No database migration or API contract change was introduced.

The server remains authoritative for paise totals, payment agreements, receipt verification/correction, invoice immutability, production release, dispatch, delivery confirmation, scanned evidence, issue holds and completion. The UI presents existing projections and retains the current mutation and revision checks.

## Changes delivered

- Finance workspace now groups the current accepted quotation total, ledger payments and remaining balance; shows the quotation-bound payment agreement, current invoice readiness, next finance action, receipts and invoice history. INR display derives from integer paise values. Test receipts are explicitly marked simulated, including in the aggregate payment summary.
- Razorpay status distinguishes unavailable from TEST configuration and shows webhook configuration without exposing credentials. The existing server-verified checkout remains TEST-only.
- Owner payment agreement, explicit receipt verification, append-only receipt correction, immutable commercial invoice preview/download and non-GST wording remain intact. Customer view has no owner policy, receipt-verification or correction controls.
- Fulfilment now displays an ordered seven-step server-state trail, customer-review window and issue hold. Failed state mutations retain the server error while refreshing the authoritative state and preserving the entered note.
- Delivery photo cards and modal preserve original image proportions. A failed authenticated preview has a retry state; upload timestamps remain labelled as server upload time.
- WebSocket close code 1008 clears participant and cursor state, marks presence unavailable/revoked and does not retry. Cursor rendering accepts the revision from the typed event envelope as well as the legacy nested location.
- Focused regressions cover finance visibility and formatting, delivery step ordering, rejected dispatch message retention, separate pickup/delivery blockers, collaboration cursor replay/revocation, exact proof review and mobile overflow.

## APIs and role scope

No new endpoint was added. The UI uses the existing `GET /api/v1/design/enquiries/{id}/payments`, `/gateway`, `/fulfilment`, `/fulfilment/photos`, secured invoice document/preview routes and existing staff mutation routes. Owner and customer data scopes remain in the backend. Operator queue omissions are not treated as confirmations. Delivery actions and evidence waivers remain server-authorized.

The screen inventory is updated in [SCREEN_MAP.md](SCREEN_MAP.md). Contract details remain in [API_CONTRACT.md](API_CONTRACT.md) and [PAYMENTS_AND_INVOICES.md](PAYMENTS_AND_INVOICES.md).

## Verification results

| Check | Result |
|---|---|
| `npm.cmd run typecheck` | Passed before final finance wording refinement; rerun recorded below after final edits. |
| Focused Playwright finance/work/delivery scenarios, desktop and mobile | 8 passed. Scenarios: customer work overview, blocked finance, dispatch evidence/error retention, independent pickup/delivery evidence. Synthetic API fixtures; not provider or real-payment verification. |
| Focused Batch 3B collaboration reconnect/revocation | 4 passed across desktop and mobile before the final mobile viewport normalization; final rerun recorded below. |
| `./backend/gradlew.bat -p backend --no-daemon integrationTest --tests com.printflow.WorkflowMailIntegrationTest` | Passed against native PostgreSQL after explicitly passing the local `DB_URL`, `DB_USERNAME` and `DB_PASSWORD` process variables. This preserves the payment/production/evidence server gates. |
| `npm.cmd run build` | Passed. Compiled, type checked and generated all Next.js pages. Final rerun recorded below after the final finance label refinement. |
| Full native browser workflow harness (`operations-flow.spec.ts`) | Not run: `.local/browser-fixtures.json` is absent. The harness stops before sign-in; no owner/customer credentials were generated or read for this test. |
| Combined 28-case Playwright invocation | Not a clean result: it stopped on shared synthetic customer-session state in the older cross-spec sequence. Relevant finance/delivery and collaboration specs passed in isolated focused runs. The complete 28-case matrix is not claimed. |

SMTP was disabled for the local app during this pass. No real Razorpay payment, invoice email, webhook delivery, bank receipt, customer mail or courier event was attempted.

## Visual evidence

Actual Playwright browser screenshots are retained in `output/batch4-visual-evidence/`. Desktop screenshots are 1440×900 and mobile screenshots are 390×844. All are synthetic UI/API fixture states; no real customer records, secrets or payment details are included. The mobile and desktop files were inspected for cropping, overflow, text hierarchy and accessible action placement. The clean-photo preview displays its full aspect ratio. The tested dispatch failure remains visible beside the preserved note.

The full evidence manifest, HTML gallery and ZIP are in that directory / adjacent output package. Screenshot state/significance is described in its README.

## Limitations and review notes

- The 28 visual scenarios in the Batch 4 brief are not all separately captured. The evidence set focuses on customer finance, the seven-stage work overview, owner dispatch blocking, photo preview and responsive variants. Owner receipt correction, invoice PDF, credit/overpayment, explicit waiver, issue resolution, completed state and API-denial screenshots still need a configured native browser fixture account.
- The checked-in native operations test could not be exercised because its ignored fixture JSON was missing. This does not affect the successful PostgreSQL-backed integration test or the synthetic Playwright checks, but the complete browser lifecycle remains unverified in this pass.
- No owner readiness projection change was needed: existing queue and selected-job refresh paths already use server projections. No backend gate, invoice contract, ledger schema, WebSocket authorization rule or state transition was weakened.

## Final verification

Append exact final command results here after the final post-edit reruns. Do not treat this branch as deployed; no merge or deployment is part of Batch 4.
## Final verification (25 October 2026)

- `npm.cmd run typecheck` — passed.
- `npm.cmd run test:e2e -- e2e/workflow-redesign.spec.ts --grep "project next action|failed scan and unpaid finance|delivery scan evidence precedes confirmation|missing pickup and delivery photos" --project=desktop --project=mobile --max-failures=1` — passed twice after the final UI edits, 8 cases each run. Final run captures retained directly in the evidence gallery at 1440×900 and 390×844.
- `npm.cmd run test:e2e -- e2e/workflow-redesign.spec.ts --grep "failed scan and unpaid finance" --project=desktop --project=mobile --max-failures=1` — 2 passed after the final finance TEST-status wording change.
- `npm.cmd run test:e2e -- e2e/batch3-designer-workspace.spec.ts --grep "designer dashboard and scoped project workspace|customer proof review|collaboration replays|revoked collaboration" --project=desktop --project=mobile --max-failures=1` — 8 passed, including 1008 revocation cleanup, cursor replay, multiple revisions, exact approval and mobile proof review.
- `./scripts/Use-PrintFlowTools.ps1; ./scripts/Initialize-LocalDatabase.ps1; $env:DB_URL='jdbc:postgresql://127.0.0.1:5432/'+$env:POSTGRES_DB; $env:DB_USERNAME=$env:POSTGRES_USER; $env:DB_PASSWORD=$env:POSTGRES_PASSWORD; ./backend/gradlew.bat -p backend --no-daemon integrationTest --tests com.printflow.WorkflowMailIntegrationTest` — BUILD SUCCESSFUL against native PostgreSQL 5432.
- `npm.cmd run build` — passed after final changes: optimized compilation, TypeScript and static page generation completed.
- Local app restored with `./Start-PrintFlow.ps1 -SkipBuild -DisableMail`; health check: API `UP`, UI `200`, PostgreSQL `5432` listening.
- `git diff --check` — passed (line-ending conversion warnings only; no whitespace errors).

The final workflow screenshot run required one approved retry after Edge failed to launch under the default execution sandbox; the retry passed all 8 cases. Visual inspection found both Pickup evidence and Dispatch marked current while the pickup scan was pending. The stepper was corrected so only Pickup evidence is current until it passes; the final production build and focused stepper regression then passed (2/2 desktop/mobile). Final captures were inspected directly: customer finance remains legible, the seven-step production drawer is ordered and scrollable, the preview shows the whole synthetic image, and mobile finance/work screens show no horizontal body overflow. The rejected dispatch message remains visible with its customer note. `output/batch4-visual-evidence/README.md` records the screenshot set and inspection notes.

The Batch 3B designer/customer/collaboration suite was run in isolation after normalizing the synthetic proof fixture. The combined 28-case cross-spec command is not claimed as passed: an older shared customer-session fixture interaction stopped that combined invocation. The relevant focused scenarios passed in isolated spec runs.

`operations-flow.spec.ts` cannot start in this checkout because its ignored `.local/browser-fixtures.json` is absent. No credentials were created/read for it; therefore the real authenticated browser lifecycle and owner finance workspace remain unverified here. PostgreSQL-backed `WorkflowMailIntegrationTest` and synthetic Playwright flows did run. The 32-scenario matrix in the supplied Batch 4 brief was not exhaustively executed; outstanding capture gaps are listed above.

Final build and focused UI outcomes replace the earlier provisional entries above.
