# J.D.Printex — Batch 3B collaboration

## Scope

This pass fixes inbound STOMP `SEND` handling, restores the currently viewed proof/page after every successful collaboration subscription, and validates the work against the running native PostgreSQL/Spring/UI stack. It does not change proof approval, rejection, or production release authority: those remain REST/database workflows. No database migration, Docker, merge, or deployment was used.

### Multi-staff presence polish

Each collaboration connection now receives a random pseudonymous participant ID scoped to that socket session. The ID is included in presence and cursor events and is used for UI list keys and cursor replacement, so multiple staff members with the shared “J.D.Printex team” label remain independently visible. It reveals neither the account name nor the HTTP session ID.

When the socket closes or presence is otherwise unavailable, the UI immediately clears displayed participants and cursors. After reconnecting, it shows “Presence updating…” until a fresh server presence snapshot arrives, then restores the current participants. A focused Playwright regression checks both sides of that transition.

## Corrections

- `CollaborationChannelInterceptor` handles client application `SEND` frames while retaining authenticated sessions, CSRF, allowed destinations, exact per-project authorization, subscription checks, and cross-project isolation.
- `ProjectCollaborationProvider` sends its latest proof/page viewing context only after the project subscription is established, including on reconnect.
- The broker replays an existing participant cursor when the recipient publishes/replays the matching proof/page context.
- Focused regression coverage uses actual STOMP-decoded `SEND`, denied cross-project `SUBSCRIBE`, invalid destination, expired session and owner MFA cases; broker tests cover cursor replay. The real browser test uses valid STOMP header syntax for cross-project denial.

## Local services and persistence

Validated the checked-in Windows startup path `Start-PrintFlow.ps1` → `scripts/Initialize-LocalDatabase.ps1` → `scripts/Initialize-LocalStaff.ps1`. PostgreSQL 17 is configured to listen only on `127.0.0.1:5432`; its persistent data directory is `.local/postgres-data` (`PG_VERSION` is 17). The initializer reuses that data directory when present rather than reinitializing it. `pg_ctl status -D .local/postgres-data` reported the server running. The backend and UI were also confirmed running on 8080 and 3000; `/actuator/health` returned `UP`, and `http://127.0.0.1:3000/` returned HTTP 200. Test fixture credentials and proof images were saved only under ignored `.local` and removed by cleanup.

The actual production UI build completed successfully through the checked-in startup script. The application remained available during the acceptance run. Mail delivery was disabled for the synthetic test identities.

## Real-backend two-browser acceptance

Command:

```powershell
cd frontend
npm.cmd run test:e2e -- e2e/batch3b-real-collaboration.spec.ts --project=desktop --max-failures=1
```

Result: **1 passed** against native PostgreSQL and Spring, with two separate browser contexts authenticated as a synthetic customer and the assigned synthetic designer. The test verified:

- Presence and shared customer/designer cursors on the same proof.
- Customer proof comment delivery and project conversation message visibility to the designer.
- New proof revision notification and explicit customer loading of the revision.
- Forced socket disconnection, reconnection, project subscription, and context replay without a manual page change.
- A syntactically valid STOMP subscription to another project receives `ERROR`; the customer REST request to an unrelated project returns 404.
- Removing designer assignment ends collaboration and the designer project page becomes unavailable.
- Disabling the customer account revokes subsequent collaboration access after the revalidation interval.
- Owner login without recent MFA reports `mfaRequired: true` and `mfaRecent: false`.

The test fixture was cleaned; its local credentials file is absent after the run.

## Focused verification

| Check | Command | Result |
|---|---|---|
| STOMP interceptor and broker regressions | `gradlew.bat test --tests com.printflow.collaboration.CollaborationChannelInterceptorTest --tests com.printflow.collaboration.CollaborationBrokerTest --rerun-tasks` | Passed: 11 tests (6 interceptor, 5 broker), 0 failures. Includes same-label staff participants retaining separate cursor IDs. |
| Existing proof/workflow integration | `gradlew.bat integrationTest --tests com.printflow.DesignProofIntegrationTest --tests com.printflow.LiveWorkIntegrationTest --tests com.printflow.ProductionBoundaryIntegrationTest --no-daemon` with local DB and secure cookie settings | Passed: 19 tests (12 proof, 5 live-work, 2 production-boundary), 0 failures. |
| Proof/mail/payment/production lifecycle regression | `gradlew.bat integrationTest --tests com.printflow.WorkflowMailIntegrationTest --no-daemon` | Passed: 9 tests, including payment and production/delivery gates, 0 failures. |
| Designer/customer proof UI | `npm.cmd run test:e2e -- e2e/batch3-designer-workspace.spec.ts --project=desktop --project=mobile --max-failures=1` | Passed: 6 tests, 0 failures. Includes subscription context replay, stale proof approval, immediate presence clearing during reconnect, and desktop/mobile proof workspace. |
| Real authenticated collaboration | `npm.cmd run test:e2e -- e2e/batch3b-real-collaboration.spec.ts --project=desktop --max-failures=1` | Passed: 1 real-backend test, 0 failures after waiting for fresh presence snapshots; connected desktop/mobile screenshots refreshed. |
| Next.js production build | `npm.cmd run build` | Passed; compiled, type checks and static generation completed. Spring/UI restarted with rebuilt artifacts; PostgreSQL remained running. |
| Service readiness | `pg_ctl status`; `GET /actuator/health`; `GET /` | PostgreSQL running; Spring `UP`; UI HTTP 200. |

An earlier aggregate production-boundary attempt inherited insecure cookie settings from the local development environment and failed its Secure-cookie assertion. The focused integration classes were rerun with secure cookies and strict SameSite explicitly enabled; all 19 passed. No workflow security check was disabled to make tests pass.

## Connected visual evidence

Actual captures from the two authenticated browsers are retained in `output/batch3b-visual-evidence/connected/`:

- Desktop full page: `customer-connected-desktop.png` (1440×6003), `designer-connected-desktop.png` (1440×5731).
- Desktop viewport: `customer-connected-desktop-viewport.png`, `designer-connected-desktop-viewport.png` (both 1440×900).
- Mobile full page: `customer-connected-mobile.png` (390×7718), `designer-connected-mobile.png` (390×7768).
- Mobile viewport: `customer-connected-mobile-viewport.png`, `designer-connected-mobile-viewport.png` (both 390×844).

Inspected the four viewport captures. Each shows the other authenticated participant’s labelled cursor on the synthetic wedding-card artwork; the desktop views also show the shared project conversation. The customer and designer see their own role-specific view. Artwork, comments and conversation remain legible without overlap. The mobile section navigation is horizontally scrollable; the page itself has no horizontal overflow (asserted `scrollWidth <= 390`). Full-page captures include the same connected proof and conversation content. These screenshots are real connected-backend evidence; older reconnect-fallback and synthetic screenshots remain in their separate directories.

## Changed files in this correction

- `backend/src/main/java/com/printflow/collaboration/CollaborationChannelInterceptor.java`
- `backend/src/main/java/com/printflow/collaboration/CollaborationBroker.java`
- `backend/src/test/java/com/printflow/collaboration/CollaborationChannelInterceptorTest.java`
- `backend/src/test/java/com/printflow/collaboration/CollaborationBrokerTest.java`
- `backend/src/integrationTest/java/com/printflow/CollaborationBrowserFixture.java`
- `backend/build.gradle`
- `frontend/src/components/ProjectCollaborationProvider.tsx`
- `frontend/e2e/batch3-designer-workspace.spec.ts`
- `frontend/e2e/batch3b-real-collaboration.spec.ts`
- `output/batch3b-visual-evidence/connected/*.png`

## Remaining limitation

The collaboration presence/cursor registry is intentionally in memory and currently requires a single Spring instance. A multi-instance deployment needs a shared broker/relay. No production proxy or deployment configuration was changed. Approval, rejection, and production release continue to be authoritative server-side REST/database actions and were covered by the focused existing regression suites.
