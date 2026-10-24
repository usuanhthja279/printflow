# Batch 2 correction pass — visual verification

Captured 25 October 2026 from the local application at `http://127.0.0.1:3000`. After images are actual Playwright browser screenshots using synthetic API responses; no customer records, payment provider or mail delivery were used. Desktop is 1440×900; mobile is 390×844. These captures are tied to the correction-pass source revision.

## Before and after evidence

“Before” images are actual Batch 2 gallery captures from before this correction pass. They show the prior interface behavior in related scenarios, but the fixture and viewport are not always identical. An exact operator-scoped baseline was not captured before this pass; its before image is labeled as a related drawer view. “After” images show the corrected scenarios from the focused regression run.

| Scenario | Before | After |
|---|---|---|
| Owner dispatch readiness | `before/01-ready-for-dispatch-prior.png` | `after/01-owner-ready-for-dispatch.png` |
| Missing current payment agreement | Same previous readiness view | `after/02-missing-payment-agreement.png` |
| Waived pickup-photo requirement | `before/03-photo-requirement-prior.png` | `after/03-waived-pickup-photo.png` |
| Printer-scoped READY job | `before/04-printer-scope-related-prior.png` (related view only) | `after/04-printer-ready-scoped.png` |
| HOLD filter | `before/05-hold-filter-prior.png` | `after/05-hold-filter.png` |
| QC/rework filter | `before/06-qc-rework-prior.png` | `after/06-qc-rework-filter.png` |
| Selected job after state change | Prior job drawer in `before/04-printer-scope-related-prior.png` (not the same state) | `after/07-refreshed-selected-job.png` |
| Mobile production lane | `before/08-mobile-production-prior.png` | `after/08-mobile-production-lane.png` |

## Visual inspection findings

- Owner dispatch checks identify current payment agreement, current quotation invoice and pickup evidence separately. A missing agreement reads “Required”; waiver reads “Waived” with a policy explanation; a scanned photo reads “Scanned.”
- The printer-scoped drawer omits owner financial/invoice checklists, displays an owner-only notice, and reports missing permitted pickup evidence separately. Its stage label now reads “Production complete,” so it does not imply owner dispatch clearance.
- HOLD results contain only held jobs. QC/rework results exclude held jobs; the independent badges and result counts match their respective filters.
- The refreshed selected-job screenshot shows the updated PRINTING stage and current operation state after the queue mutation.
- Mobile lane navigation stays within the viewport and selects the requested lane. The horizontal lane strip clips neighboring chips at its edges; this communicates that the strip can scroll, but a subtle scroll cue could improve discoverability.
- The drawer is tall and scrolls vertically. On smaller screens, later evidence sections require scrolling; controls remain reachable.

## Automated verification

- `npm.cmd run build` — passed (Next.js production build, TypeScript, static page generation and route finalization).
- `npm.cmd run test:e2e -- e2e/batch2-corrections.spec.ts --project=desktop --project=mobile --max-failures=1` — 2 passed, one desktop and one mobile project. It covers owner refresh counts, owner readiness, current-terms blocking despite full payment, waived photos, operator scope, hold/QC separation, unassigned counts, and selected-job refresh. Desktop and mobile screenshots are stored in `after/`.
- `..\scripts\Test-PhaseTwo.ps1 -SkipBuild -TestClass 'com.printflow.WorkflowMailIntegrationTest.routingProductionQueueAndDeliveryPhotosRespectScopesAndGates'` — passed against native PostgreSQL on port 5434. No Docker was used. This focused integration test covers omitted operator-only fields and a fully paid order without current payment terms; backend release remains blocked until terms are current.

The browser regression used the already-running local development UI (port 3000) with synthetic API data; the production build was verified separately. Razorpay, email delivery and real business records are outside this test.
