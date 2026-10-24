# J.D.Printex — Workflow Redesign Batch 3

## Checkpoint A: implementation map before changes

### Existing behavior

- `DesignerWorkspace` calls `GET /api/v1/design/queue`, displays a few counts/search results, and opens the selected job in a modal.
- That modal embeds `DesignProofs`; the proof panel can open another modal for the current proof. Design tasks, reference artwork, revision history and conversation already exist as separate components.
- The backend already stores immutable proof revisions/decisions/comments, scanned artifacts, task revisions and comment resolutions. Proof publication and customer decision APIs validate the current accepted quote, clean scan, exact file hash/revision and page review. Project-level authorization is checked by `DesignProofs.lead()` for every resource access.
- The designer queue currently returns only id, customer, product, quantity and enquiry status. It cannot produce useful proof-action filters or revision/feedback counts without extra requests.
- Customer artwork review already uses authenticated PDF.js/image previews, normalized pin coordinates and shared proof comments. Customer approval is exact-revision/hash-bound and does not release production.

### Current APIs

- `GET /api/v1/design/queue` — assigned non-new, non-closed enquiries; designer-only.
- `GET /api/v1/design/enquiries/{id}` — scoped proof workspace and team task projection.
- Existing `/artifacts`, `/proofs`, `/proofs/{id}/comments`, `/proofs/{id}/decision`, `/tasks`, and `/comments/{id}/resolve` routes cover upload, scan, publish, review and resolution.
- `GET /api/v1/design/enquiries/{id}/work` and project messages support the project conversation.

### Required UI changes

- Replace the modal-driven designer list with a searchable dashboard, derived action filters/counts and cards showing public reference, current proof/scan/feedback/task information and next action.
- Add `/designer/work/{id}` as a full workspace route. The proof studio opens inline there, avoiding a workspace modal around a second proof modal. Keep per-project conversation in a desktop side rail and stack it on mobile.
- Label the workspace sections and connect navigation to reference files, upload/scan, current proof, feedback, tasks, history and conversation. Preserve the existing owner assignment tools in the owner workspace.
- Keep current customer preview, comments, page acknowledgement, decisions and approvals on existing shared APIs; improve visible revision/author/time/resolution context without adding a second comment store.

### Backend necessity

A small additive projection for the existing designer-only queue is needed: public reference, current latest proof decision/scan/agreement state, unresolved current-proof comment count, accepted-quote availability and open-task due date. This avoids N+1 workspace requests and allows counts to use the same authoritative data as the cards. No migration or workflow transition is needed.

### Security and regression risk

The primary risk is exposing information outside designer assignment scope. The projection will remain inside the existing designer-only queue query filtered by `designer_id`; project pages will still fetch through the existing per-record authorization checks. Only public reference, customer-facing project details and designer task/proof state will be added. Owner-only prices and finance data stay out. Existing file scan, immutability, approval and production gates remain authoritative.

### Planned verification

- Focused Playwright synthetic tests for designer queue filters/search, authorized workspace, inline proof, customer review actions, page/pin interaction, stale conflicts and responsive layout; use the existing native proof lifecycle browser test for the real local stack when credentials/service state permit.
- Focused `DesignProofIntegrationTest` methods for queue projection/assignment scoping plus existing proof authorization and exact-revision test coverage.
- `npm.cmd run typecheck`, Next.js production build, focused Gradle integration test(s), and retained screenshots from the test run. Use native services only; no Docker.

Checkpoint A implementation map is complete. Later test/evidence sections will record actual outcomes and limitations.

## Checkpoint B: implementation completed

### Designer action dashboard

`DesignerWorkspace.tsx` now shows a searchable queue with filters for all work, designer action, customer review, requested changes, approved proofs and closed history. Counts and card state derive from the queue projection. Search accepts the same displayed `PF-XXXXXXXX` public reference as the business queue. Cards identify the latest proof/scan, unresolved current-proof comments, next open task due date and a plain-language next action.

`GET /api/v1/design/queue` now returns the compact projection in one database query. It still rejects non-designers and filters by the logged-in `designer_id`; it adds no customer email, phone, price or finance fields. Closed assigned records are included for history. Unresolved comments are counted only for the latest proof revision, not historical revisions. The latest proof carries the accepted-agreement state used by existing proof APIs.

### Dedicated project workspace and proof navigation

Added `/designer/work/{id}`. It confirms the current session is an active designer and checks that the requested internal ID is present in the designer-scoped queue before loading the project. Every subsequent operation still uses existing per-record API authorization. The page includes a full-width project header, sticky section navigation, inline proof workspace and a project conversation rail on desktop; the rail stacks after the work on narrow screens.

The shared proof UI now labels feedback with author, time, page and resolution state. Anchored comments have a “Locate annotation on artwork” control; PDF comments switch to the referenced page, and image comments scroll to the canvas. Clicking a rendered pin takes the user to the corresponding comment. The source file, immutable proof history, page-by-page customer acknowledgement, hash-bound approval and server-side production gates are unchanged.

### Changed files

- `backend/src/main/java/com/printflow/files/DesignProofs.java`
- `backend/src/integrationTest/java/com/printflow/DesignProofIntegrationTest.java`
- `frontend/src/app/designer/work/[id]/page.tsx`
- `frontend/src/app/experience.css`
- `frontend/src/components/DesignerWorkspace.tsx`
- `frontend/src/components/DesignerProjectWorkspace.tsx`
- `frontend/src/components/DesignProofs.tsx`
- `frontend/src/components/DesignManagement.tsx`
- `frontend/src/components/ProofImage.tsx`
- `frontend/src/components/ProofPdf.tsx`
- `frontend/src/components/ReferenceArtwork.tsx`
- `frontend/e2e/batch3-designer-workspace.spec.ts`
- `docs/API_CONTRACT.md`, `docs/ARCHITECTURE.md`, `docs/SCREEN_MAP.md`, `docs/REQUIREMENTS_TRACEABILITY.md`

No database migration or production workflow transition was added.

### Verification actually run

- `npm.cmd run typecheck` — passed after implementation edits.
- `npm.cmd run build` — passed. The first sandboxed run could not create a prerender directory under `.next` (`EPERM`); the same build passed when rerun with approved normal workspace write access. Next reported all routes, including `/designer/work/[id]`.
- `gradlew.bat integrationTest --tests com.printflow.DesignProofIntegrationTest` — Java and backend/integration-test sources compiled. The suite did not execute: Spring could not connect to native PostgreSQL at the configured test address. Ports 5434 and 5432 were both unavailable. No Docker was started.
- `npm.cmd run test:e2e -- e2e/batch3-designer-workspace.spec.ts --project=desktop --max-failures=1` — not completed. The first Edge launch failed in its temporary site-list write (`error = -5`). The one approved retry reached the test and dashboard filter/search assertions, then found the missing synthetic `/api/v1/design/intake` fixture; that fixture was added. The next Edge launch again exited during temporary site-list writes. Per `AGENTS.md`, the blocked launch was not retried again.
- `git diff --check` — passed.

### Screenshot and visual evidence

The synthetic Playwright test is configured to retain desktop dashboard (1440x900), desktop project (1440x900) and mobile project (390x844) captures under `output/batch3-visual-evidence/synthetic/`, and checks mobile horizontal overflow. No screenshots were produced because the browser could not launch on the fixed rerun. The in-app browser opened the local app but showed the staff sign-in screen; I did not enter account credentials. Therefore the redesigned screens have not yet received visual inspection, and there are no screenshot references to claim as captured evidence. The test uses synthetic fixtures and is not a real-backend UI verification.

### Remaining verification limitation

Run the focused Gradle integration test with the local PostgreSQL service available, then rerun the single Playwright spec after the Edge temporary-file launch issue is resolved. Inspect and retain the generated screenshots before treating visual verification as complete. The added integration assertion specifically covers current queue projection, proof feedback resolution, task due date, latest revision, exact approval and removal when designer assignment is cleared.
