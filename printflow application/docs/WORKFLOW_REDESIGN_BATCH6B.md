# J.D.Printex — Workflow Redesign Batch 6B

## Scope and status

Branch: `feature/workflow-redesign-batch6b`

Correction base: `9d1f94ec4641ed7b039d7cef472c0041ec6ccd64`
Status: Batch 6B implementation and focused acceptance complete on this branch. Not merged or deployed.

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
- Manual ChatGPT Plus handoff: export a contact-free design brief and current V2 JSON, copy a project-specific prompt, then paste or select generated JSON. The browser strictly validates V2 structure and limits, verifies every image reference against the project’s clean/scanned assets, previews variations and all-page differences, and requires explicit confirmation before local apply. Apply is undoable and still must save against the current optimistic revision. No content is sent to ChatGPT and no browser cookies or credentials are accessed.
- V2 documents reject missing/duplicate pages, invalid active-page projections and malformed V1/V2 combinations. Export and preflight inspect image headers with dimension/pixel bounds before decoding. Preflight caches each asset inspection per report, accounts for fit mode in aspect findings, and uses letter spacing in text wrapping.
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

All commands below ran on 26 Oct 2026 using Java 21, native PostgreSQL 17.11, the checked-in Gradle wrapper and no Docker.

- Focused Java unit tests: `.\gradlew.bat test --tests com.printflow.files.PrintDesignPdfTest --tests com.printflow.files.DesignPreflightServiceTest --tests com.printflow.files.LocalDesignAssistantProviderTest --tests com.printflow.files.PrintPreflightMetricsTest --rerun-tasks --no-build-cache` — **PASS**, 16 tests, 0 failures. Includes malformed V2 rejection, oversized PNG header rejection before pixel decode, positive letter-spacing width/wrapping, multipage export and PDF text/raster geometry.
- Isolated native PostgreSQL integration: temporary PostgreSQL 17.11 cluster at `127.0.0.1:55432`, database `printflow_batch6b`. Spring/Flyway validated and applied **all 31 migrations through V31**. No user database or Docker was used.
- Authenticated backend integration: `.\gradlew.bat integrationTest --tests com.printflow.DesignProofIntegrationTest --rerun-tasks --no-build-cache` — **PASS**, 15 tests, 0 failures. Synthetic customer, assigned/unassigned designers and MFA-verified owner used real local Spring HTTP sessions. Covered V1 draft read/convert/save to V2; multipage save/reopen; stale revision conflict; clean scanned image authorization and cross-project denial; repeated-asset preflight with fit-aware findings; persisted report history; customer/unassigned-designer acknowledgement denial; assigned designer/owner acknowledgement; and immutable exact-revision proof approval and edit invalidation.
- Frontend typecheck: `npm.cmd run typecheck -- --incremental false` — **PASS**.
- Next.js production build: `$env:PRINTFLOW_NEXT_DIST_DIR='.next-batch6b-build'; npm.cmd run build` — **PASS**.
- Desktop/mobile Playwright: `$env:PLAYWRIGHT_BASE_URL='http://localhost:3001'; npm.cmd exec -- playwright test e2e/batch6a-print-editor.spec.ts --project=desktop --project=mobile --max-failures=1 --reporter=line` — **PASS**, 4/4. Browser endpoints are synthetic fixtures; the separate authenticated PostgreSQL integration above exercises the real local backend. The test covers strict malformed import rejection, variation preview/diff, explicit apply, undo, export and customer privacy.
- `git diff --check` — **PASS** after final edits.

The temporary UI test server and isolated PostgreSQL cluster were stopped after verification. They do not change the normal local service configuration.

## Visual evidence

Actual browser screenshots are retained under `output/batch6b-visual-evidence/`, captured at desktop **1440×900** and mobile **390×844**. In addition to designer/customer editor screenshots, `manual-import-preview-desktop.png`, `manual-import-preview-mobile.png`, `manual-variation-cards-desktop.png`, and `manual-variation-cards-mobile.png` show the manual handoff and imported variation review. They use synthetic API fixtures, not real customer/backend data. Images were opened and inspected: the comparison sheets preserve page proportions, desktop conversation remains in the right column, and mobile stacks without body-level horizontal overflow (assertion passed). The compact project navigation is horizontally scrollable on mobile. `Reconnecting` is expected in browser fixtures because collaboration endpoints are intentionally not connected. `designer-editor-desktop.png` records the editor canvas at its current scroll position; the full A4 sheet requires scrolling.

## Remaining limitations

Preflight rules are general advisory checks, not printer-profile certification. Font availability/embedding is evaluated for the single bundled font. The frontend browser font and server embedded font are both Liberation Sans, but platform rasterization may differ slightly. The browser journey is synthetic; the backend role/security path was exercised with synthetic users against the real local database. No external ChatGPT generation or AI credentials are used. PDF/X certification and advanced prepress remain unsupported.
