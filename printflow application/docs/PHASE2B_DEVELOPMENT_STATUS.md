> **Current Phase 2 continuation:** task deadlines, comment resolution, comparison, offline proof evidence and commercial order-item linkage are implemented. See PHASE2_COMPLETION.md for the final scope/checks. Remaining-scope notes in this earlier slice report are historical.

# Phase 2B.1 development candidate — design proof collaboration

24 October 2026. Java 21, Gradle wrapper, native PostgreSQL/Next.js. No Docker or Maven.

## Implemented

Private PDF/PNG/JPEG reference/design uploads with filename/type/signature/size checks, per-actor quotas, 100-file/200-MiB enquiry allowance, opaque UUID storage, restricted OS permissions and SHA-256 verification. Native Defender/ClamAV adapters, bounded scan subprocesses and fail-closed quarantine. Clean files have authenticated attachment downloads only; private drafts remain hidden from customers. Quarantined/rejected files cannot be shared or approved.

Owner assigns an enabled DESIGNER account. Designers see only assigned open work. Owner/assigned sales coordinate uploads and proof sharing. A clean design plus accepted quotation creates an immutable numbered proof revision, exact hash and quotation binding. Current revision comments carry optional page/side reference. Linked customer explicitly acknowledges review and approves or requests changes. Immutable decisions include actor/time/hash/quote; duplicate same decision is safe. Replacing a proof requires renewed approval and preserves previous history. No automatic production release, payment or external notification occurs.

## Actual checks

- Gradle build passed, including 17 existing unit checks.
- Five focused DesignProofIntegrationTest HTTP checks passed on native PostgreSQL: quarantine/download guards, MIME/signature/path validation, CSRF, scoped customer/designer/sales access, clean attachment headers and bytes, altered file rejection, exact-version comments/approval, immutable DB history, replacement approval invalidation, idempotency, wrong hash/quote scope, stale revisions, request changes, accepted-quote requirement, concurrent publication and both known/streamed oversized uploads.
- Two ProductionBoundaryIntegrationTest checks passed with explicit private test storage and native development cookie overrides cleared. An initial run inherited APP_SECURE_COOKIES=false from local DEV and failed the existing Secure-cookie assertion; the test harness environment was corrected, not the expected security property.
- Frontend production build/type compilation passed. Four focused Playwright tests passed across desktop/mobile in 15.3 seconds: clean upload/share/comment/approval and fail-closed quarantine. These UI tests use synthetic API/scan fixtures. An initial select-control locator was corrected. Desktop/mobile screenshots inspected. No real owner MFA was read or changed.
- Final backend packaging and frontend build passed after small filename/private-directory/role-help refinements. One attempted build while the server still held its output hit an EBUSY file lock; services were stopped and the final build succeeded. No broad test rerun was needed for these refinements.

## Native scanner correction — 24 October 2026

The earlier failure used a malformed synthetic PDF and did not establish failure for valid artwork. Valid PNG and PDF fixtures now clear the installed Defender; real browser uploads through the Java adapter return CLEAN with scan_engine DEFENDER. Quarantine remains fail closed for nonzero exits/timeouts; no antivirus settings or stored scan verdicts were bypassed. See NATIVE_ARTWORK_VERIFICATION.md for the actual approval journey result and DESIGN_PROOF_SETUP.md for the reproducible diagnostic and retry procedure. TEST_ONLY verdicts remain confined to integration-test configuration.

## Remaining scope

Private PNG/JPEG previews and image review pins have since been implemented. PDF rendering/editing, comment resolution, designer deadlines/task planning, offline proof approval evidence, order-item conversion/linkage, isolated malware detection validation, encrypted-volume backups/restore, retention/erasure execution and production release/material/payment/scheduling remain open. File signatures plus AV do not certify PDF print quality or safe content in every reader. PDFs download for external review. See REMAINING_FEATURES.md. This is a development candidate, not all of Phase 2 or permission for production/customer rollout.
