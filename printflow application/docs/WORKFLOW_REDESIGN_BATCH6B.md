# J.D.Printex — Workflow Redesign Batch 6B

## Scope and status

Branch: `feature/workflow-redesign-batch6b`

Base: `29f35b3` (Batch 6A line)
Status: implementation candidate; do not merge or deploy.

This batch extends the Batch 6A editor with versioned multipage documents, per-page print geometry, richer typography and image treatment, advisory preflight snapshots, and a provider boundary for design suggestions. Existing project authorization, malware scanning, immutable proof publishing, exact proof hashes, stale revision checks, approval invalidation and production gates remain authoritative.

## Reused components and data flow

- `PrintDesignEditor.tsx` remains the single customer/designer editor and uses the existing same-origin API/CSRF helpers and collaboration context.
- `DesignProofs` continues to own project authorization, stage checks, revision checks, artifact verification and immutable proof publication. Editor image layers resolve only clean image artifacts belonging to the same enquiry.
- `PrintDesignPdf` remains the server PDF renderer. V1 drafts normalize deterministically to V2; saves persist V2. PDF export consumes every V2 page and verified image asset.
- `DesignProofs` stores preflight snapshots and append-only audit events in the project scope. Preflight and suggestions do not approve a proof or release production.
- Database changes are additive: V30 permits schema versions 1 and 2 and promotes documents already containing pages; V31 stores report snapshots, draft/hash/ruleset binding, requester and optional human acknowledgement.

## Implemented

- V2 versioned page list, active page, page names, dimensions, bleed and safe margin; add, duplicate, delete, reorder and switch pages; portrait/landscape switching; thumbnail rail and optional adjacent-page preview. V1 documents remain readable and convert without replacing layer IDs.
- Server-side limits of 100 pages, 100 layers per page and 500 total layers, with existing coordinate, dimension and asset restrictions.
- Multipage PDF export with per-page trim, bleed, crop and media boxes. Existing scanned/immutable artifact workflow remains in force.
- Liberation Sans Regular is bundled/embedded. Editor/PDF include wrapping, alignment, line spacing, character spacing, clipping and matching image CONTAIN/COVER/STRETCH geometry. Image opacity and replacement are supported. Only the bundled regular font is offered; unsupported glyphs produce a preflight error. Complex-script shaping is explicitly uncertified.
- Image preflight verifies scanned private storage/hash, decoding, physical-size effective DPI and image/frame aspect difference. Resolution warnings are advisory; RGB-to-CMYK, ICC profiles, spot colors, trapping and PDF/X are reported as unsupported/not checked rather than passed.
- Persistent preflight snapshots bind enquiry, draft revision, content hash and ruleset version; authorized owner/designer acknowledgements are stored separately. Reports are advisory and explicitly return `isProductionApproval: false`.
- Provider-neutral suggestion interface with a deterministic local provider for safe layout suggestions. A saved revision is required; suggestions must be previewed and explicitly applied, become ordinary undoable draft edits, and are rejected when stale. No external service is called and no AI credential is required.
- Mobile editor retains a single-column layout and compact page navigation.

## Deliberately not claimed

- No external/OpenAI suggestion provider is configured; external calls are disabled. The present provider is deterministic local logic, not generative AI.
- No arbitrary PDF import/rasterization, advanced crop positioning, font styling beyond regular, licensed font catalog management, complex Indic shaping, CMYK conversion, ICC validation, spot separation, trapping, PDF/X certification, imposition or press-specific ruleset is implemented.
- The facing-page panel is a visual preview only; it does not modify page geometry or impose spreads.
- A successful preflight is not print approval. Final production remains subject to existing proof approval and staff release checks.

## Routes and contracts

- `GET/PUT /api/v1/design/enquiries/{id}/editor-draft`: V2 page document; V1 input is accepted and normalized. PUT retains expected-revision conflict handling and existing editor role/stage/project authorization.
- `POST /api/v1/design/enquiries/{id}/editor-export`: exports a verified multipage PDF for the expected draft and proof revision. Publishing still uses the existing proof route and immutable scanned artifact.
- `POST /api/v1/design/enquiries/{id}/editor-preflight`: `{expectedDraftRevision}`. Persists a report snapshot and returns findings, `contentHash`, `rulesetVersion`, `draftRevision`, and `isProductionApproval:false`.
- `GET /api/v1/design/enquiries/{id}/preflight-reports`: project-scoped history (maximum 50).
- `POST /api/v1/design/enquiries/{id}/preflight-reports/{report}/acknowledge`: owner or assigned designer acknowledgement with a required note. This does not satisfy another workflow gate.
- `POST /api/v1/design/enquiries/{id}/editor-assistant/suggestions`: `{expectedDraftRevision}`. Current provider returns local suggestions only and does not transmit project data externally.

## Focused verification

Commands are recorded here only when actually run. Java tests use the checked-in Gradle wrapper and local Java 21; Docker is not used.

- `backend` PowerShell: `$env:JAVA_HOME='C:\Users\usuan\.jdks\dragonwell-21.0.12'; .\gradlew.bat test --tests com.printflow.files.PrintDesignPdfTest --tests com.printflow.files.DesignPreflightServiceTest --tests com.printflow.files.LocalDesignAssistantProviderTest --tests com.printflow.files.PrintPreflightMetricsTest` — **PASS**, 13 test cases, 0 failures on 26 Oct 2026.
- `frontend`: `npm.cmd run typecheck -- --incremental false` — **PASS** on 26 Oct 2026.
- `frontend` PowerShell: `$env:PLAYWRIGHT_BASE_URL='http://localhost:3001'; npm.cmd exec -- playwright test e2e/batch6a-print-editor.spec.ts --project=desktop --project=mobile --reporter=line` — **PASS**, 4/4 on 26 Oct 2026. Covers designer page/layer interactions, image fitting, draft save/revision conflict, suggestion preview/apply/undo, proof publishing, customer private draft and no customer publish control. The fixtures use synthetic API responses. The temporary Next dev server on port 3001 was stopped afterward.
- `frontend` PowerShell: `$env:PRINTFLOW_NEXT_DIST_DIR='.next-batch6b-build'; npm.cmd run build` — **PASS** on 26 Oct 2026.
- Native PostgreSQL integration tests — **not run**. `Test-NetConnection 127.0.0.1 -Port 5432` returned `False`; no PostgreSQL backend was started. Playwright routes are synthetic and do not represent authenticated real-backend acceptance.

## Visual evidence

Actual browser screenshots are retained under `output/batch6b-visual-evidence/`: `designer-editor-desktop.png`, `designer-editor-mobile.png`, `customer-editor-desktop.png`, and `customer-editor-mobile.png`. They use synthetic API fixtures, not real customer/backend data. Inspected all four: page controls and editor remain in the viewport; the desktop customer layout keeps conversation in its side column; phone UI stacks without body-level horizontal overflow (assertion passed). At the captured scroll position only the top portion of the large A4 canvas is visible and the user must scroll to inspect the full sheet. `Reconnecting` is expected in these synthetic screens because collaboration endpoints are intentionally not connected. The phone screenshot places the top project navigation in a horizontally clipped rail; controls remain reachable by horizontal scrolling, but this can be improved in a later visual polish pass.

## Remaining limitations

Preflight rules are general advisory checks, not printer-profile certification. Font availability/embedding is evaluated for the single bundled font. The frontend browser font and server embedded font are both Liberation Sans, but platform rasterization may differ slightly. Continue testing real PostgreSQL migrations and authorization against assigned/unassigned projects before treating this branch as accepted.
