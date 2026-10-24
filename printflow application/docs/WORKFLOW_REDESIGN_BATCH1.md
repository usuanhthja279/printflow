# Workflow redesign — Batch 1

24 October 2026. Branch `feature/workflow-redesign-batch1`, based on `feature/printing-platform` (`bc20f99`). The supplied Markdown is the design brief; illustrative prototype data is not application data. Existing unrelated deletions of older reference boards are left untouched.

## Implementation map, before editing

- `LiveWork`: customer next action from the authorized work DTO, public reference, five stages, main workspace and right conversation rail; collapsed history.
- `WorkflowUI` / `workflow.css`: shared next action, readiness, stage navigation, activity, scan status, job cards and action bar.
- `DesignProofs`, `ProofPdf`, `ProofImage`: full-width studio with page rail, current canvas and comments/approval rail; preserve payloads, revision/hash checks, page acknowledgements and history.
- `Modal`, `DesignerWorkspace`: optional studio/drawer layouts retaining native dialog focus and escape behavior.
- `StaffWorkspace`: contextual module title and prioritized exceptions derived only from loaded scoped enquiries; no invented proof/payment exceptions.
- `ProductionBoard`, `ProductionJobPanel`: compact desktop lanes, mobile lane selector, detail drawer and existing confirmed/revisioned operations. HOLD overlays the current stage.
- `PaymentsPanel`: finance readiness separate from proof/release readiness; verified ledger and Razorpay TEST remain explicit.
- `FulfilmentPanel`, `DeliveryPhotoPanel`: evidence before milestone actions, scan blockers, full original photos and server upload provenance; server remains authoritative.
- `DesignerRoutingPanel`, `WorkConversation`: availability/workload and explicit channel labels; inbound mailbox replies remain unconnected.

## Scope and verification

Presentation only, Batch 1. No new backend flows, statuses, schema, prices, integrations, deployment, inventory, print-ready bypass or reorders. Native Gradle, no Docker. Focused responsive/interaction checks use synthetic authorized DTO fixtures and do not claim provider delivery or money capture. Results and screenshots are appended after verification.

## Recorded verification — 25 October 2026

- `npm.cmd run typecheck`: passed on final source.
- `npm.cmd run test:e2e -- workflow-redesign.spec.ts --project=desktop --project=mobile --max-failures=1`: **12 passed in 32.1 seconds**. Six cases at desktop and phone sizes, with 16 representative PNGs. All API traffic intercepted; no live-provider calls or real mail from the browser suite.
- `./backend/gradlew.bat -p backend integrationTest --rerun --tests com.printflow.WorkflowMailIntegrationTest.routingProductionQueueAndDeliveryPhotosRespectScopesAndGates`: **passed**, one native HTTP/PostgreSQL case executed; existing compiler output reused. Covers scopes, designer routing, optimistic production operations, holds/rework, evidence scanning and milestone gates. Its scanner/provider are mocked; the earlier native-scanner walkthrough remains separate evidence.
- `python scripts/static_validate.py`: **39 passed** structural checks.
- Final runtime: API UP, web /business HTTP 200, normal mail delivery configuration still enabled. Browser tests use synthetic intercepted data; the regression server disables mail. Backend/database were not restarted. Frontend remains in native hot-reload mode at http://127.0.0.1:3000.
- No production build, migration, Docker or deployment. No broad backend suite.

Diagnostic attempts: the first browser launch was blocked by the filesystem sandbox before executing page checks. Subsequent focused attempts fixed a route-announcer selector, a scan fixture inconsistent with the work projection, and an asynchronous resume assertion. Product fixes keep proof errors inside the studio and clear confirmation after stale/rejected proof, production and fulfilment actions. Notes remain available after failures.

## Existing DTO limitations

The photo `display_name` is the original filename; uploader identity is not included in the current response. The work projection's id is an internal workspace id, so the heading labels it as a workspace identifier, not a public PF reference. Requested/agreed delivery dates remain in the quotation details. These limitations are explicit rather than filled with inferred data. The PDF test uses two pages; long/complex documents and real device testing remain manual review items. The tests include labeled controls, keyboard escape/focus restoration and phone overflow checks, not a complete WCAG audit.

## Review bundle

Run `python scripts/package-redesign-evidence.py` after the focused checks. Output: `output/redesign-batch1-2026-10-25/J-D-Printex-Redesign-Batch1.zip`, containing 16 screenshots, portable HTML gallery, Markdown view significance, implementation map, original Markdown brief and checksums. These are implemented UI views with synthetic DTOs, not a full live end-to-end order. No secrets, cookies, raw browser traces or customer files are packaged.

Batch 1 stops here. Proposed new backend flows remain deferred. Existing user deletions of older root-level design references are untouched. Source changes are local on the new feature branch and have not been pushed.

## Files changed

- `MANIFEST.sha256`
- `README.md`
- `docs/API_CONTRACT.md`
- `docs/ARCHITECTURE.md`
- `docs/QA_TEST_PLAN.md`
- `docs/REQUIREMENTS_TRACEABILITY.md`
- `docs/SCREEN_MAP.md`
- `docs/WORKFLOW_REDESIGN_BATCH1.md`
- `docs/WORKFLOW_REDESIGN_SPEC.md`
- `frontend/e2e/workflow-redesign.spec.ts`
- `frontend/next-env.d.ts`
- `frontend/src/app/layout.tsx`
- `frontend/src/app/workflow.css`
- `frontend/src/components/DeliveryPhotoPanel.tsx`
- `frontend/src/components/DesignProofs.tsx`
- `frontend/src/components/DesignerRoutingPanel.tsx`
- `frontend/src/components/DesignerWorkspace.tsx`
- `frontend/src/components/FulfilmentPanel.tsx`
- `frontend/src/components/LiveWork.tsx`
- `frontend/src/components/Modal.tsx`
- `frontend/src/components/OwnerActionCentre.tsx`
- `frontend/src/components/PaymentsPanel.tsx`
- `frontend/src/components/ProductionBoard.tsx`
- `frontend/src/components/ProductionJobPanel.tsx`
- `frontend/src/components/ProofImage.tsx`
- `frontend/src/components/ProofPdf.tsx`
- `frontend/src/components/StaffWorkspace.tsx`
- `frontend/src/components/WorkConversation.tsx`
- `frontend/src/components/WorkflowUI.tsx`
- `scripts/package-redesign-evidence.py`
