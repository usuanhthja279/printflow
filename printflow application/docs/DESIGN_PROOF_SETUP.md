# Native private design file and proof setup

24 October 2026. No Docker or Maven. Use the checked-in Gradle wrapper and existing native start scripts.

## Local scanner result

The earlier diagnosis used a malformed synthetic PDF, not a complete PDF document. Its exit 2 / 0x80508023 did not establish that the scanner was unavailable for real artwork. Defender is healthy and targeted native scans now return exit 0 for valid synthetic PNG and PDF files, including opaque private filenames. Antivirus settings were not changed. Nonzero exits still quarantine files; never substitute a fake success command or manually mark records CLEAN.

Run `./scripts/Test-NativeArtworkScanner.ps1` for a bounded native diagnostic against checked-in valid synthetic artwork. Run `./scripts/Test-DesignApprovalBrowser.ps1` against the running local stack for the actual application flow with separate synthetic owner, designer and customer sessions. The latter uses the Java adapter and real Defender, not mocked scan verdicts; it prepares/disables its own staff accounts and leaves your enrolled owner/authenticator unchanged. See NATIVE_ARTWORK_VERIFICATION.md for the latest run evidence.

## Configuration

`APP_ARTIFACT_ROOT=.local/private-artifacts` is the native default. Use a dedicated private directory, outside public web directories and source distribution. Do not set it to an empty value, workspace root, filesystem root or a symlink. The app uses UUID filenames and restricts directory/file permissions to the service account (POSIX mode 700/600 or Windows owner ACL). No real artwork belongs in source control. Files are not application-encrypted at rest: deployment needs an encrypted volume/backups and deliberate retention/access rules.

Windows defaults to `APP_ARTIFACT_SCANNER=DEFENDER`, using `C:/Program Files/Windows Defender/MpCmdRun.exe`. An installed platform executable can be selected with `APP_ARTIFACT_SCANNER_PATH`. The adapter performs a targeted custom scan with remediation disabled; only exit 0 clears a file. Nonzero exits, a missing executable, a timeout or integrity failure keep it quarantined. Defender exit 2 is ambiguous, so it is never interpreted as clean. Files are not executed or rendered by the application.

Native ClamAV alternative: install a trusted native distribution and maintain its signature database with its supported updater. Set `APP_ARTIFACT_SCANNER=CLAMAV` and `APP_ARTIFACT_SCANNER_PATH` to the absolute clamscan executable path. The adapter uses bounded file/expanded sizes, alerts on scan limits and fails if definitions are older than seven days. Exit 0 is clean, 1 rejected, other codes quarantine. `NONE` always quarantines. Restart native services after private environment changes, then use Retry scan in the staff design workspace. A successful synthetic scan and an isolated standard AV detection test remain release checks; no real provider/service connectivity is inferred from unit fixtures.

In production an explicit APP_ARTIFACT_ROOT is mandatory. Provision it for the dedicated service account with adequate disk space, encrypted backups, retention policy and monitored capacity. Keep the scanner executable/definitions maintained and use service/proxy upload timeouts, connection/concurrency limits and disk monitoring. Scans are bounded synchronous subprocesses (45 seconds, at most two in flight); a queued isolated worker is a later scaling improvement. The app is not a substitute for PDF preflight or content disarm/reconstruction. Clean PNG/JPEG files have private inline previews with bounded image dimensions; PDFs download for external review. Automatic print release is not implemented.

## Usage

Owner can create an individual DESIGNER account under Staff, then assign that designer within an enquiry's Design files & proof review section. The designer signs in through /business and sees only assigned open jobs. Owner/assigned sales retain coordination access. Guest customers must securely link their enquiry before using the customer workspace.

Customer uploads are shared REFERENCE files. Staff/designer DESIGN uploads are drafts, hidden from the customer until explicitly shared as a proof. PDF/PNG/JPEG only; filename, extension, MIME and signature must match. Maximum 20 MiB per file, 100 files and 200 MiB per enquiry; persisted actor quotas also apply. A quarantine file is unavailable to everyone until a successful scan. Clean bytes are SHA-256 checked before download, publication and approval. Reads require the current authenticated enquiry scope; no public/static object URL exists. Downloads are attachments with no-store, nosniff and sandbox headers.

Proof sharing requires an accepted quotation and clean design. Each shared proof is an immutable numbered revision bound to that quote and file. Comments reference a revision and optional page/side (not a verified PDF page coordinate). A customer explicitly acknowledges review, then approves or requests changes. Changes require an explanation. Team members cannot impersonate customer approval. Publishing a replacement requires renewed approval while earlier decisions remain immutable history. Production still needs its own release workflow. Offline proof approval recording, comment resolution and automated readiness checks for inventory/payments/scheduling remain later work. PNG/JPEG image pins are implemented.

Primary references: [Microsoft targeted scans](https://learn.microsoft.com/en-us/defender-endpoint/command-line-arguments-microsoft-defender-antivirus), [ClamAV command options](https://github.com/Cisco-Talos/clamav/blob/main/docs/man/clamscan.1.in), [Next.js proxy body configuration](https://nextjs.org/docs/pages/api-reference/config/next-config-js/proxyClientMaxBodySize).

24 October UI continuation: clean raster proofs may be shown inline with normalized review pins, with a 25-million-pixel / 20,000-pixel-side preview guard. This supersedes the earlier blanket no-inline-preview statement for PNG/JPEG only. PDFs remain downloads; AV clearance and integrity checks still apply. Mailbox sync and source-artwork editing are not introduced by review pins.
