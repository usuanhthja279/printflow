# Native artwork scanning and approval verification

24 October 2026 — J.D.Printex — Java 21 / Gradle wrapper / PostgreSQL / Next.js. No Docker.

## Finding and correction

The previous scanner diagnosis relied on a malformed PDF containing a PDF header rather than a valid document. Its Defender exit 2 / 0x80508023 was not evidence that valid artwork could not be scanned. Defender health checks were normal. Valid synthetic PNG/PDF files scan successfully, including opaque `.bin` filenames in the application's restricted private storage.

The existing Java adapter also works with these valid files. No production scanner bypass, forced CLEAN record, antivirus setting change, replacement scanner installation or application rebuild was needed. The correction is a reproducible valid-file diagnostic, real end-to-end regression check and corrected documentation. The native tooling helper also no longer adds Docker executables to PATH.

## Actual checks

- `./scripts/Test-NativeArtworkScanner.ps1`: two bounded real Defender scans passed, using checked-in valid PNG and PDF fixtures, the same fixed arguments as the Java adapter and remediation disabled.
- `./scripts/Test-DesignApprovalBrowser.ps1`: one real desktop browser journey passed in **15.6 seconds** (workflow 13.4 seconds). Gradle fixture setup and cleanup passed separately in 23 and 21 seconds. No intercepted API responses or TEST_ONLY scan verdicts were used.
- The journey registered/verified a synthetic customer through local mail preview; issued and accepted a quotation; assigned a separate synthetic designer; uploaded a PDF reference and two distinct PNG design revisions; verified CLEAN and DEFENDER metadata; downloaded the PDF; denied customer access to the unpublished design (404); previewed the published PNG (HTTP 200, decoded width 160); recorded a location pin/comment and change request; published revision 2; and recorded exact-revision customer approval.
- Final state preserved both decisions in order (APPROVE, REQUEST_CHANGES), the image comment coordinates and IN_PROGRESS status. Reloaded customer work showed production **Not released**. The resulting synthetic screenshot was visually inspected.
- Initial new-test failures concerned a missing login/prefill wait, an Edge inspector response-body read and an expected 403 where the privacy boundary deliberately returns 404. Those harness issues were corrected; the final run above passed. No application security expectation was relaxed.
- Synthetic staff accounts were disabled and the private fixture credential file removed in cleanup. Existing owner credentials and enrolled authenticator were not read or changed. Fictional customer/enquiry/audit/artifact records remain local for history; no real mail or provider requests were sent.
- `npm.cmd run typecheck` passed. `python scripts/static_validate.py` passed its 39 structural checks. No application source change required a full backend/frontend rebuild or broad test rerun.

## Manual local review

1. Keep the existing native stack running, or use `./Start-PrintFlow.ps1 -SkipBuild`.
2. Owner: open an accepted enquiry and assign an enabled DESIGNER account. Existing valid quarantined files may be retried using **Retry scan**; malformed/unsupported files should be replaced with a valid export.
3. Designer: sign in separately at `/business`, open assigned work, upload a valid PNG/JPEG/PDF and wait for **Scan passed**. Choose a clean design, enter a revision summary and share the proof.
4. Customer: open the work under **Live works**; refresh the design workspace after another actor changes it. PNG/JPEG has an inline preview and location pins. PDF uses the authenticated download for external review. Add a comment, acknowledge review and request changes.
5. Designer: refresh, upload an updated file and share a new revision. Customer: refresh and approve that revision. Check the earlier decision remains in history and production stays unreleased.

The Windows scanner diagnostic requires PowerShell 7. Normal artwork uploads continue through the running application. Signature/type/size checks, per-actor quotas, private storage, hashes, role checks and fail-closed quarantine remain active.

This verifies clean artwork locally, not complete AV detection efficacy. An isolated standard detection check, maintained definitions, deployment-account verification, backups/retention and production release controls remain open. See REMAINING_FEATURES.md for the phase-wise backlog.
