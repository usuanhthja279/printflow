# J.D.Printex workflow redesign — Batch 2

Batch 2 improves the owner’s operational overview and the production command centre using the existing enquiry, production, payment, proof and delivery workflows. The UI reads authorized server data and links to the existing workflow actions. It adds no schema or state transition, and it leaves the backend’s role, revision, proof, payment and evidence gates authoritative.

## What changed

- The owner Enquiries screen now shows live counts for new enquiries, quotation preparation, customer decisions, accepted work awaiting designer assignment, production release, holds/QC/rework and dispatch blockers. Each count links to its relevant queue. Unavailable API data appears as unavailable rather than as zero.
- The production board has filters for release, unassigned, hold, QC/rework and dispatch blockers, plus the existing urgency and search controls. A desktop breakpoint uses three lanes on narrower desktop/tablet widths and six on wide screens. Phone view remains one column with its lane selector.
- Job cards surface hold, urgency, rework and dispatch-blocked signals. The job drawer shows the assigned printer, machine, due date and owner-only readiness checks, with the exact reason reported by the authorized queue API. Existing operation controls continue to use the current revision and confirmation fields.
- The owner designer-routing view includes the number of accepted active projects waiting for assignment. It uses a read-only count; existing automatic/manual assignment behavior is unchanged.
- `GET /api/v1/owner/design-routing/summary` is owner-only. `GET /api/v1/production/queue` adds advisory fields for owners. Operator responses remain limited to assigned jobs and omit finance, invoice, artwork, photo and rework summary fields.
- `printflow application/AGENTS.md` now requires actual browser screenshots and visual inspection for significant UI work, including 1440×900 desktop and 390×844 mobile views, scenario/role/data-source labeling, and explicit reporting when image inspection could not be completed.

## Verification

UI fixtures use synthetic values and intercept every `/api/**` request. No email, Razorpay or local business database writes occur in the browser screenshot tests.

Commands run:

```powershell
npm.cmd run build
npm.cmd run test:e2e -- e2e/batch2-evidence.spec.ts --project=desktop --project=mobile
..\scripts\Test-PhaseTwo.ps1 -SkipBuild -TestClass 'com.printflow.WorkflowMailIntegrationTest.routingProductionQueueAndDeliveryPhotosRespectScopesAndGates'
```

The final production build passed. The focused Playwright suite passed in desktop and mobile projects and captured all 16 required scenarios at both target viewports (the desktop-only board shot is named `03`, and its mobile equivalent is `04`). The focused Gradle integration test passed against native PostgreSQL. No Docker was used. The integration test verifies owner/operator scope, current proof/payment release readiness, dispatch blockers, and the blocker clearing after a clean pickup scan. Build output and screenshots are in the evidence package.

## Visual inspection

Actual screenshots were opened and inspected after capture. The six wide desktop lanes fit without clipping at 1440 CSS pixels. The mobile board stays within 390 CSS pixels and uses its lane selector. The owner summary cards retain readable labels, and the drawer visibly distinguishes confirmed and blocked prerequisites, including pending pickup-photo scan. Printer assignment, hold/recovery and QC rework controls remain within the scrollable drawer. No horizontal overflow was detected in the mobile production screen.

The before set comes from the checked-in Batch 1 gallery (1366×900 desktop and 390×844 mobile); the after set is from this revision (1440×900 and 390×844). Different widths and synthetic fixtures mean the comparison is for layout and hierarchy, not identical live data. See `output/batch2-visual-evidence/index.html`, `VISUAL_TEST_REPORT.md` and `TEST_RESULTS.md`.

## Changed files

- `printflow application/AGENTS.md`
- `backend/src/main/java/com/printflow/customer/Payments.java`
- `backend/src/main/java/com/printflow/customer/ProductionQueue.java`
- `backend/src/main/java/com/printflow/files/DesignerRouting.java`
- `backend/src/main/java/com/printflow/files/DesignerRoutingController.java`
- `backend/src/integrationTest/java/com/printflow/WorkflowMailIntegrationTest.java`
- `frontend/src/app/workflow.css`
- `frontend/src/components/DesignerRoutingPanel.tsx`
- `frontend/src/components/OwnerActionCentre.tsx`
- `frontend/src/components/ProductionBoard.tsx`
- `frontend/src/components/StaffWorkspace.tsx`
- `frontend/src/components/WorkflowUI.tsx`
- `frontend/e2e/batch2-evidence.spec.ts`
- `docs/API_CONTRACT.md`, `docs/ARCHITECTURE.md`, `docs/SCREEN_MAP.md`, `docs/REQUIREMENTS_TRACEABILITY.md`

## Limits and follow-up

Queue readiness is informational and can change between refresh and action; the existing server checks remain the source of truth. Counts reflect the authorized queue’s current 200-job limit and the current visible enquiry page. This batch does not add shop capacity planning, inventory, machine reservations, production splitting, live GPS, or multi-shipment tracking. The screenshots cover role-based workflow states with synthetic API data; the Gradle integration test covers the changed owner/operator response and production gates against native PostgreSQL.

The complete screenshot package is `output/JD-Printex-Batch2-Visual-Evidence.zip`.

## Focused correction pass

Operator status copy no longer implies dispatch clearance when owner-only checks are omitted. Payment-term readiness, HOLD/QC split, unassigned counts, refresh behavior and photo waivers are corrected with regression coverage. Evidence and test results: [correction pass](WORKFLOW_REDESIGN_BATCH2_CORRECTIONS.md).
