# J.D.Printex — Batch 2 Final Correction

## Scope and behavior

The QC/rework attention predicate now distinguishes current work from its audit history. Owner queue rows expose additive `active_rework` metadata when a rework event exists and the current production stage is PRINTING, FINISHING or QC. QC-stage jobs remain in the attention queue even without rework history. Jobs on HOLD are excluded from QC/rework attention and stay in the separate HOLD queue. READY jobs with historical rework remain visible with a “Rework history ×N” badge and are not treated as needing QC attention.

Dashboard counts, production filter counts and filtered results use the same shared predicate. The server derives the advisory field from existing queue data; no migration or workflow transition was added. Operator queue responses continue to omit rework metadata and historical count. Production authorization, payment checks, proof approval and dispatch validation are unchanged.

## Changed files

- `backend/src/main/java/com/printflow/customer/ProductionQueue.java` — owner-only current `active_rework` advisory; omitted for operators.
- `backend/src/integrationTest/java/com/printflow/WorkflowMailIntegrationTest.java` — API and lifecycle assertions for rework, QC, READY and role scoping.
- `frontend/src/lib/production.ts` — shared QC/rework attention predicate.
- `frontend/src/components/OwnerActionCentre.tsx` and `frontend/src/components/ProductionBoard.tsx` — shared counts and results.
- `frontend/src/components/WorkflowUI.tsx` — explicit historical rework label.
- `frontend/e2e/batch2-corrections.spec.ts` — synthetic role/state regression coverage and desktop/mobile screenshot capture.
- `docs/API_CONTRACT.md`, `docs/REQUIREMENTS_TRACEABILITY.md`, `docs/ARCHITECTURE.md`, `docs/SCREEN_MAP.md` — contract and implementation notes.

## Verification performed

- Focused Playwright: `npm.cmd run test:e2e -- e2e/batch2-corrections.spec.ts --project=desktop --project=mobile --max-failures=1` — 2 passed (desktop and mobile). This exercised synthetic fixtures for current QC, held QC, active rework, READY with historical rework, owner refresh, role-scoped operator data, filters and counts.
- Next.js production build: `npm.cmd run build` — passed.
- Focused Java/PostgreSQL integration test: `.\scripts\Test-PhaseTwo.ps1 -SkipBuild -TestClass 'com.printflow.WorkflowMailIntegrationTest.routingProductionQueueAndDeliveryPhotosRespectScopesAndGates'` — passed (1 test) against the running native PostgreSQL service; no Docker.
- `git diff --check` — run before commit.

The synthetic Playwright fixtures validate UI behavior and are not real-backend data. The Java integration test validates the owner/operator queue contract and production gate flow against local PostgreSQL.

## Screenshot evidence and visual inspection

The passing Playwright run captured these views at 1440 × 900 desktop and 390 × 844 mobile, using synthetic fixtures:

- `frontend/test-results/batch2-corrections-Batch-2-feccc-ueue-exceptions-and-refresh-desktop/05-hold-filter.png`
- `frontend/test-results/batch2-corrections-Batch-2-feccc-ueue-exceptions-and-refresh-desktop/06-qc-rework-filter.png`
- `frontend/test-results/batch2-corrections-Batch-2-feccc-ueue-exceptions-and-refresh-desktop/07-completed-rework-history.png`
- `frontend/test-results/batch2-corrections-Batch-2-feccc-ueue-exceptions-and-refresh-mobile/05-hold-filter.png`
- `frontend/test-results/batch2-corrections-Batch-2-feccc-ueue-exceptions-and-refresh-mobile/06-qc-rework-filter.png`
- `frontend/test-results/batch2-corrections-Batch-2-feccc-ueue-exceptions-and-refresh-mobile/07-completed-rework-history.png`

The images were visually inspected. Desktop status labels, one held result, two QC/rework results and the READY historical badge were correct without clipping. On mobile, the HOLD and READY cards and rework-history badge were readable; the QC screenshot shows the vertically stacked Printing and Quality check lanes, with the lower QC card partly outside the captured viewport. The focused card can be reached by scrolling. A follow-up attempt to improve only that mobile capture hit a Playwright mobile lane DOM visibility assertion; the next Edge launch was blocked before test startup by temporary profile filesystem permissions. The successful two-project run above is the final app-behavior verification; the later test-only capture experiment was reverted. Playwright cleaned its transient screenshot folder during that failed relaunch, so the six final PNGs are not retained in this checkout. Earlier Batch 2 correction screenshots are retained in `output/JD-Printex-Batch2-Correction-Evidence.zip` and are not represented as screenshots of this final semantic correction.

## Remaining limitations

`active_rework` is an owner-facing advisory derived from existing rework history plus current production stage; it does not replace server workflow authority. The UI reports current QC work even where the queue API has no rework history. A real shop-floor rework lifecycle beyond the existing state model would require a dedicated authoritative unresolved-rework state, which is outside this no-migration correction.
