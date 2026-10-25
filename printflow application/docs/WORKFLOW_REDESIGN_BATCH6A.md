# J.D.Printex — Workflow Redesign Batch 6A

## Scope and isolation

- Branch: `feature/workflow-redesign-batch6a`
- Base: `4635400fb827f57c846a6956056f8c91a28e5344` (`feature/workflow-redesign-batch5`)
- Worktree: Codex-managed `workflow-redesign-batch6a`
- Scope: local, non-AI, interactive print-design editor foundation for a project customer and its assigned designer.
- No changes to the Batch 5 release checkout, no deployment, no Batch 6B features, and no live provider use.

## Phase 1 — repository audit

### Reusable UI and collaboration

| Existing component | Reuse in Batch 6A |
|---|---|
| `DesignProofs.tsx` | Project proof/artifact workspace, role-specific stage context and refresh after save/publish. The editor is an additional private draft workspace, not a replacement for proof review. |
| `ProofPdf.tsx` and `ProofImage.tsx` | Existing customer proof preview, page/image annotations and approval UI. Published editor output will enter these exact review components. |
| `ReferenceArtwork.tsx` | Existing scanned customer reference/sample selection and feedback. Reuse the artifact upload route for editor image layers. |
| `ProjectCollaborationProvider.tsx` | Existing authorized participant presence, proof cursors and revision notifications. The document itself will not be broadcast or concurrently merged; draft updates use optimistic REST revision checks and an after-commit change notice. |
| `LiveWork.tsx` and `DesignerProjectWorkspace.tsx` | Existing customer and assigned-designer project surfaces where the editor belongs. |
| `api`, `staffMutation` | Existing same-origin session/CSRF API requests and conflict response handling. |

### Reusable backend controls and workflow

- `DesignController` and `DesignProofs` own project-scoped workspace, artifact and proof routes. `DesignProofs.lead` checks customer ownership and exact assigned designer access; closed records reject writes. Batch 6A draft endpoints will restrict editing to those two roles and keep the document out of dashboard/list projections.
- `ArtifactStorage` writes opaque objects outside the web root with bounded streaming and SHA-256 verification. `ArtifactPolicy`, `MalwareScanner`, `storeUpload`, and the existing clean-preview/download routes provide the malware-scan and private-asset lifecycle. Editor image layers will reference only clean PNG/JPEG artifacts belonging to the same project.
- `proof_revisions`, `proof_decisions`, `proof_comments`, and `design_artifacts` are backed by immutable database triggers. The existing publish path requires an accepted quote, a clean verified DESIGN artifact, the expected current proof revision, and no production release. Customer decisions bind the latest proof to the exact artifact hash and quote. A new published revision makes the previous decision non-current without modifying it.
- `CollaborationChannelInterceptor` and `CollaborationBroker` authorize project subscriptions and cursor events against current sessions and assignments. Cursors/presence are ephemeral. The editor reuses the existing project presence status, but does not send live document edits over WebSocket; simultaneous saves are resolved with the project and draft row locks plus revision compare-and-set and explicit HTTP 409 conflict handling.
- PostgreSQL/Flyway is the source of truth. The base schema includes V28. Migration V29 adds one structured private draft row per project, with a schema version, monotonically increasing revision and last editor metadata.

### Editor boundary

The design document is versioned JSON (`schemaVersion`) independent of React/SVG markup. Its own incrementing draft revision supports save/reopen and concurrent edit conflicts. The screen renders that data as an interactive SVG in millimetre coordinates. Export creates a bounded PDF using the backend PDFBox dependency, stores it as a new privately scanned DESIGN artifact, then invokes the established immutable proof-publish route. The source draft is never the proof file, and editing a draft never changes an approved proof. A newly published proof follows the existing fresh customer-approval process.

The editor is an MVP artwork layout surface. It does not claim CMYK/PDF-X preflight, font outlining, press trapping, color-managed raster simulation, or safe simultaneous multi-user editing. Print dimensions, bleed and safe-area guides are represented explicitly so later prepress review can validate production requirements.

## Implementation

### Structured draft and secure export

- Added `PrintDesign.Document` schema version 1 with trim dimensions in millimetres, bleed, safe margin, ordered elements and template ID. The React editor renders this data as SVG; the data format does not depend on UI markup.
- Added `GET`/`PUT /api/v1/design/enquiries/{id}/editor-draft` and `POST /api/v1/design/enquiries/{id}/editor-export`. Draft read/write requires customer ownership or exact designer assignment. A customer can only reference that project's clean REFERENCE images; the designer can reference clean same-project PNG/JPEG files. Draft documents are private JSON in V29, size/layer bounded and never exposed in queues.
- A stale revision returns HTTP 409. A real concurrent customer/designer save test proves that exactly one save wins. A conflict leaves the local edits visible and asks the user to reload before continuing.
- Export is designer-only, rechecks assignment, accepted quotation, current proof revision, fulfilment state and image artifact hashes, then renders a bounded single-page PDFBox document. The PDF is stored and scanned as a private DESIGN artifact. The UI then calls the existing proof-publish endpoint; it does not write `proof_revisions` directly. The existing immutable hash/quote approval flow remains authoritative.
- The editor opens for customers and assigned designers on the existing project pages. It includes starter layouts, mm dimensions, text, rectangles, ellipses, clean image layers, selection/reordering, drag, resize, rotation fields, alignment guides, zoom, pan, undo/redo, save/reload, conflict handling, bleed/safe guides and designer export/publish. Customer UI can save drafts but has no publish action.
- Added a Windows local testing guide and `AGENTS.md` notes for Java discovery, per-shell `JAVA_HOME`, native ports/database mapping, worktree dependency isolation, single Gradle runs and stopping only task-owned processes.

## Verification and evidence

### Commands and actual results

All backend commands ran in the isolated Batch 6A worktree with the existing Dragonwell Java 21 JDK. Integration checks used the native PostgreSQL service on port 5432 and a separate synthetic database named `printflow_batch6a`; no Docker was used. UI checks used a Batch 6A Next dev server on port 3001 with mocked/synthetic project APIs, not real customer records. That server was stopped after testing.

- `backend: .\gradlew.bat compileJava` — PASS.
- `backend: .\gradlew.bat test --tests 'com.printflow.files.PrintDesignPdfTest'` — PASS (2 focused tests: PDF trim/bleed page size, text ordering, embedded clean image, invalid image reference and duplicate layer rejection).
- `backend: .\gradlew.bat integrationTest --tests 'com.printflow.DesignProofIntegrationTest.printEditorDraftsArePrivateRevisionCheckedAndPublishOnlyThroughImmutableProofWorkflow'` — PASS on native PostgreSQL; includes default wedding template, customer/designer scope, foreign-customer denial, scanned project image, stale and concurrent save conflict, private export, immutable proof publish, exact approval and edit rejection after approval.
- Existing `acceptedAmendmentInvalidatesReadinessAndArtworkLinksStayScoped` is a separate focused regression for new-proof approval invalidation; run result to be added after the final pass.
- `frontend: npm.cmd run typecheck` — PASS.
- `frontend: npx.cmd playwright test e2e/batch6a-print-editor.spec.ts --project=desktop --project=mobile --max-failures=1` with `PLAYWRIGHT_BASE_URL=http://localhost:3001` — PASS, 4 cases (designer and customer at desktop and mobile). UI requests were synthetic fixtures; they are not real-backend browser journeys.
- `frontend: npm.cmd run build` with `PRINTFLOW_NEXT_DIST_DIR=.next-batch6a` — PASS. Next's generated `tsconfig.json` and `next-env.d.ts` path changes were reverted after the build.

### Screenshot evidence and visual inspection

Saved under `output/batch6a-visual-evidence/`:

- `designer-editor-desktop.png` — 1440 × 900, synthetic assigned designer, canvas/layers and conversation column.
- `designer-editor-mobile.png` — 390 × 844, synthetic assigned designer, responsive toolbar and canvas; body has no horizontal overflow.
- `customer-editor-desktop.png` — 1440 × 900, synthetic customer, private draft and no publish button.
- `customer-editor-mobile.png` — 390 × 844, synthetic customer, responsive editor and no horizontal overflow.

The images were opened and inspected. The desktop grid keeps the canvas and layer panel beside the right conversation column. Mobile stacks the layer controls after the canvas and retains reachable toolbar controls. Existing sticky project navigation overlaps the top edge when the page is scrolled; the editor's own content remains visible beneath it. Collaboration labels show reconnecting in synthetic screenshots because the browser fixture does not establish a real STOMP session.

### Remaining limits

- This is a single-page layout MVP. It does not provide multipage composition, CMYK/PDF-X preflight, outlined fonts, press trapping, color management or calibrated 3D/print simulation.
- The canvas can reuse only clean project-scoped PNG/JPEG artifacts. PDF files are not rasterized into editable layers.
- Presence is shown through the existing collaboration provider, while document changes use optimistic saves; no live canvas synchronization or merge is claimed.
- Production publishing still requires the existing accepted quote, designer authorization, malware scan, no-fulfilment-release rule and exact proof decision workflow. Local UI screenshots use synthetic APIs; backend scope/export/publish was verified separately against native PostgreSQL.

## Batch 6A correction and completion pass — 2026-10-26

### Corrections implemented

- Corrected PDF text placement to convert the editor's top-left millimetre coordinates to PDF points once. The baseline uses the shared font ascent; positive SVG clockwise rotation is reproduced around the same layer center in PDF coordinates.
- The editor and PDF exporter now use the same bundled Liberation Sans regular font. The authenticated `/api/v1/design/editor-font` route serves the exact PDFBox font resource to the SVG editor. Unsupported glyphs are rejected with a clear validation error instead of producing a misleading proof.
- Both renderers wrap at word boundaries (and long words by glyph) using the layer's measured width, preserve explicit line breaks, and clip ink to the text layer bounds. Line count is clipped to the configured text-box height.
- Safe-margin editing clamps to the lower of the backend 50 mm maximum and half the shorter trim dimension, including when canvas dimensions are reduced.
- Drag and resize history records the document snapshot from pointer-down. Desktop browser coverage moves and resizes layers with actual mouse pointer input, then verifies Undo and Redo restore the respective geometry.
- Simultaneous first saves rely on the existing unique enquiry constraint with `ON CONFLICT DO NOTHING`; the losing request receives the normal 409 conflict response. PostgreSQL coverage creates a fresh draft race and verifies one 200, one 409, and one saved row.
- Added a browser raster comparison: the Java PDF unit test emits a test-only PDF under `backend/build/test-output/`; Playwright renders that actual exporter output with PDF.js and compares its foreground pixels with the same document rendered by the editor SVG at the PDF's physical page scale. The test requires foreground intersection-over-union above 0.72. The fixture is generated during tests and is not a committed artifact.

### Implemented MVP and advanced work still pending

| Implemented in Batch 6A | Advanced/editor-production work pending |
|---|---|
| One-page print canvas with trim dimensions in mm, configurable bleed and safe margin | Multipage documents, page rearrangement and spreads |
| Shared regular font, explicit newlines, width-based wrapping, text-box clipping and unsupported-glyph rejection | Font chooser, bold/italic, reliable bidi/complex-script layout, per-font licensing and font embedding policy |
| Text, rectangle, ellipse and scanned project PNG/JPEG layers; layer order, move, resize and rotation | Crop/contain controls, richer vector tools, PDF artwork import/rasterization and image preflight |
| Alignment guides, zoom, pan, undo/redo, private save/reopen and optimistic conflicts | Snapping controls, keyboard layer manipulation, cross-session edit merging and autosave |
| Designer export through the scanned private-artifact and immutable proof-publish workflow | CMYK/spot colors, ICC color management, PDF/X, trapping, imposition and calibrated printer output validation |

The SVG/PDF comparison verifies placement and basic wrapping for the supported regular font; it does not certify a physical press result. A4 and business-card trim/bleed page sizes, top and bottom text, multiline/wrapped text, rotation, near-trim/safe-area placement and raster bounds are covered in the focused tests. Authorization, malware scanning, exact proof hashes, approval invalidation and the production-release gates remain backend controlled and unchanged.

### Verification for this correction pass

Commands run in the Batch 6A worktree (native PostgreSQL on port 5432, dedicated `printflow_batch6a` database, Java 21 from the installed Dragonwell JDK; no Docker):

- `backend: .\\gradlew.bat test --tests 'com.printflow.files.PrintDesignPdfTest'` — PASS, 5 focused tests. Generates the transient comparison PDF in the ignored backend build directory.
- `backend: .\\gradlew.bat integrationTest --tests 'com.printflow.DesignProofIntegrationTest.simultaneousFirstEditorDraftSavesReturnOneSuccessAndOneConflict' --tests 'com.printflow.DesignProofIntegrationTest.printEditorDraftsArePrivateRevisionCheckedAndPublishOnlyThroughImmutableProofWorkflow'` — PASS, both methods on native PostgreSQL. Covers authenticated font delivery, simultaneous first saves, private scope, stale/conflicting saves, scanned same-project assets, export, immutable publish, exact approval and edit rejection after approval.
- `frontend: npm.cmd run typecheck` — PASS.
- `frontend: PLAYWRIGHT_BASE_URL=http://127.0.0.1:3001 npx.cmd playwright test e2e/batch6a-print-editor.spec.ts --project=desktop --project=mobile --max-failures=1` — PASS, 4 cases. Includes PDF/SVG pixel overlap, desktop pointer drag/resize Undo/Redo, customer private-draft restrictions and mobile overflow assertion. API data is synthetic; these are not authenticated real-backend browser journeys.
- `frontend: PRINTFLOW_NEXT_DIST_DIR=.next-batch6a-correction npm.cmd run build` — PASS; Next.js production compilation, TypeScript, static generation and route finalization completed. Generated `tsconfig.json` and `next-env.d.ts` path edits were reverted.

Refreshed screenshots are stored under `output/batch6a-visual-evidence/` and inspected after the final desktop/mobile test run. Desktop shows the design canvas beside layers and the existing conversation column. Mobile stacks editor controls and has no body-level horizontal overflow. The fixed project navigation remains horizontally scrollable on narrow screens. Synthetic test screenshots show “Reconnecting” because the fixtures deliberately reject collaboration transport; live collaboration is outside this pass.

### Final verification limits

- This pass does not add real-backend customer/designer browser journeys or test physical printer output. Backend authorization and proof lifecycle are covered by the focused PostgreSQL integration test; browser journeys remain mocked.
- Only Liberation Sans regular is supported. Other fonts/styles and complex-script shaping remain pending; unsupported code points are rejected before PDF output.
- Text wrapping is width-measured and clipped but does not provide typography controls such as kerning adjustment, widow/orphan handling or fine leading.
- The editor remains a one-page layout MVP and does not provide CMYK/PDF-X production preflight, press trapping or imposition.

## Batch 6A MVP close-out

**Implementation status: COMPLETE** for the Batch 6A editor foundation on `feature/workflow-redesign-batch6a`. The A4 safe-margin control now applies `min(50 mm, half the shorter trim dimension)`, matching backend validation. The editor regression enters 75 mm on an A4 canvas, observes the UI clamp to 50 mm, and saves successfully with the synthetic API enforcing the same 50 mm contract.

Close-out verification on 2026-10-26:

- `npm.cmd run typecheck` — PASS.
- `PLAYWRIGHT_BASE_URL=http://127.0.0.1:3001 npx.cmd playwright test e2e/batch6a-print-editor.spec.ts --project=desktop --project=mobile --max-failures=1` — PASS, 4 cases across desktop/mobile; includes the A4 safe-margin regression.
- `PRINTFLOW_NEXT_DIST_DIR=.next-batch6a-close npm.cmd run build` — PASS; optimized compilation, TypeScript, static page generation and route finalization completed.

No backend validation, proof revision, authorization, hash approval or production gate was changed. Advanced editor work remains in `docs/REMAINING_FEATURES.md`. The Batch 6A screenshots are synthetic UI evidence; comprehensive cross-module visual acceptance remains a separate implementation-freeze gate.
