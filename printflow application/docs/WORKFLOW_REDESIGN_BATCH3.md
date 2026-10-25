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

Added `/designer/work/{id}`. It confirms the current session is an active designer and requests a server-authorized project projection for the requested internal ID. Every subsequent operation still uses existing per-record API authorization. The page includes a full-width project header, sticky section navigation, inline proof workspace and a project conversation rail on desktop; the rail stacks after the work on narrow screens.

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

### Initial verification record

- `npm.cmd run typecheck` — passed after implementation edits.
- `npm.cmd run build` — passed. The first sandboxed run could not create a prerender directory under `.next` (`EPERM`); the same build passed when rerun with approved normal workspace write access. Next reported all routes, including `/designer/work/[id]`.
- `gradlew.bat integrationTest --tests com.printflow.DesignProofIntegrationTest` — Java and backend/integration-test sources compiled. The suite did not execute: Spring could not connect to native PostgreSQL at the configured test address. Ports 5434 and 5432 were both unavailable. No Docker was started.
- `npm.cmd run test:e2e -- e2e/batch3-designer-workspace.spec.ts --project=desktop --max-failures=1` — not completed. The first Edge launch failed in its temporary site-list write (`error = -5`). The one approved retry reached the test and dashboard filter/search assertions, then found the missing synthetic `/api/v1/design/intake` fixture; that fixture was added. The next Edge launch again exited during temporary site-list writes. Per `AGENTS.md`, the blocked launch was not retried again.
- `git diff --check` — passed.

### Initial screenshot limitation

The synthetic Playwright test is configured to retain desktop dashboard (1440x900), desktop project (1440x900) and mobile project (390x844) captures under `output/batch3-visual-evidence/synthetic/`, and checks mobile horizontal overflow. No screenshots were produced because the browser could not launch on the fixed rerun. The in-app browser opened the local app but showed the staff sign-in screen; I did not enter account credentials. Therefore the redesigned screens have not yet received visual inspection, and there are no screenshot references to claim as captured evidence. The test uses synthetic fixtures and is not a real-backend UI verification.

### Initial limitation

Run the focused Gradle integration test with the local PostgreSQL service available, then rerun the single Playwright spec after the Edge temporary-file launch issue is resolved. Inspect and retain the generated screenshots before treating visual verification as complete. The added integration assertion specifically covers current queue projection, proof feedback resolution, task due date, latest revision, exact approval and removal when designer assignment is cleared.

## Batch 3 focused correction pass — 25 October 2026

### Corrections

- Fixed the Playwright project-ID fixture mismatch. The project URL, queue row, proof workspace and direct project response now use the same `ids[2]` ID; the customer scenario consistently uses its separate customer project ID.
- Changed proof artwork and feedback element IDs to include enquiry and proof revision IDs. Only the current revision owns the customer-feedback navigation target. Older revisions render their artwork when selected, and each annotation navigation now selects the exact proof, page and comment.
- Added `GET /api/v1/design/enquiries/{id}/designer-project`. It uses the same minimal projection as the queue, but authorizes directly against the requested project’s current designer assignment. The dashboard remains capped at 100; an assigned designer can open older assigned projects without relying on dashboard inclusion. Existing server-side role, assignment and status checks are preserved.
- Added a focus sequence to PDF page navigation so selecting the same comment again returns to its page after a manual page change.
- Separated artwork marking from annotation-pin controls. PDF and image pins are independent keyboard-operable buttons; Enter/Space selects the existing comment and does not create a new pin. Coordinate positions remain normalized against the rendered artwork when zoomed.
- Added an integration regression for a project outside the 100-row dashboard queue, plus direct-route role and assignment denial assertions. Existing proof lifecycle integration tests already cover exact hash/quote approval binding, stale rejection, customer/designer separation and assignment scope.

### Commands and actual results

Commands below ran from `printflow application/`; the focused browser runs used the already running local Next.js development server at `http://localhost:3000`.

- PostgreSQL preflight: `Test-NetConnection localhost -Port 5434 -InformationLevel Quiet` and port `5432` — both returned `False`. Native PostgreSQL was unavailable, so no Docker was used and no database-backed integration test was started.
- `$env:JAVA_HOME='C:\Users\usuan\.jdks\dragonwell-21.0.12'; $env:PATH="$env:JAVA_HOME\bin;$env:PATH"; .\gradlew.bat integrationTestClasses` — passed; application and integration-test Java sources compiled.
- `$env:JAVA_HOME='C:\Users\usuan\.jdks\dragonwell-21.0.12'; $env:PATH="$env:JAVA_HOME\bin;$env:PATH"; .\gradlew.bat integrationTest --tests com.printflow.DesignProofIntegrationTest` — not run after preflight found both PostgreSQL ports unavailable. `DesignProofIntegrationTest`, including the >100 assigned-project regression, remains runtime-unverified against PostgreSQL.
- `npm.cmd run typecheck` — passed.
- `npm.cmd run build` — passed. An initial restricted run could not create a `.next` prerender directory (`EPERM`); the build completed successfully on the approved retry.
- `$env:PLAYWRIGHT_BASE_URL='http://localhost:3000'; npm.cmd run test:e2e -- e2e/batch3-designer-workspace.spec.ts --project=desktop --project=mobile --max-failures=1` — **4 passed** after the final test synchronization change (designer and customer scenarios on desktop and mobile).
- `$env:PLAYWRIGHT_BASE_URL='http://localhost:3000'; npm.cmd run test:e2e -- e2e/design-proofs.spec.ts --project=desktop --project=mobile --max-failures=1` — **4 passed** in the focused combined proof-lifecycle run (2 tests on each viewport project).
- `$env:JAVA_HOME='C:\Users\usuan\.jdks\dragonwell-21.0.12'; $env:PATH="$env:JAVA_HOME\bin;$env:PATH"; .\gradlew.bat integrationTestClasses` — passed; repeated here as the source-compilation check for the new integration regression. The integration runtime itself is blocked only by unavailable native PostgreSQL.
- `git diff --check` — passed before final evidence staging; repeated before commit.

Playwright assertions passed for separate designer/customer sessions; queue filters/search; direct designer project access; duplicate DOM IDs; two revisions with distinct PDF/image comments; repeated PDF comment navigation after manual page change; keyboard activation of an existing pin without creating a new annotation; zoomed PDF/image normalized coordinates; preview error; mobile horizontal overflow; stale approval HTTP 409 followed by approval of the unchanged exact revision.

### Screenshot evidence and inspection

Real browser captures use synthetic API/file fixtures (not the real backend) and are retained in `output/batch3-visual-evidence/synthetic/`. The `*-viewport.png` captures are saved with CSS scale at the requested viewport dimensions. Full-page captures are kept beside them for extended workflow context. The named viewport captures are included with the correction commit as review evidence.

- `designer-dashboard-desktop-viewport.png` — 1440×900.
- `designer-project-desktop-viewport.png` — 1440×900; includes the current artwork and right-side project conversation.
- `designer-multiple-revisions-annotation-desktop-viewport.png` — 1440×900.
- `designer-preview-error-desktop-viewport.png` — 1440×900; the test restores desktop dimensions before capturing the error state.
- `customer-multiple-revisions-desktop-viewport.png`, `customer-stale-approval-desktop-viewport.png`, `customer-approved-proof-desktop-viewport.png` — 1440×900.
- `designer-proof-studio-mobile-viewport.png`, `customer-proof-studio-mobile-viewport.png` — 390×844.

Inspected the actual dashboard, project, customer revision, stale-approval, approved-proof, preview-error and mobile captures. Each viewport image is 1440×900 or 390×844 as listed. No body-level horizontal overflow, clipped controls, overlap or visibly displaced image pin was observed. The revision section navigation intentionally scrolls horizontally on mobile; the rest of the page remains within 390 CSS pixels. Keyboard focus is visible on annotation controls. The mobile proof workspace continues vertically below the captured viewport as expected. The artwork image is a synthetic solid-color fixture: it verifies preview, zoom and normalized pin behavior, but is not a production design. Screenshots prove synthetic UI behavior only, not real-backend access decisions.

### Remaining limitations

- Database-backed Java integration execution and real-backend UI authorization could not be verified because native PostgreSQL was unavailable on ports 5434 and 5432. The integration-test sources compiled, and synthetic API tests cover the authorization scenarios in the browser. Run `gradlew.bat integrationTest --tests com.printflow.DesignProofIntegrationTest` when native PostgreSQL is available.
- No Docker, merge, or deployment was performed. The screenshot fixture is synthetic and does not represent production artwork or real-backend data.
