# J.D.Printex — Batch 3B collaboration

## Scope and implementation map

Batch 3B adds live presence, shared artwork pointers and refresh notifications to the existing customer/designer proof workspaces. It does not add a graphical editor, AI service, new approval mechanism or persistent cursor/comment store.

| Concern | Existing authoritative source | Batch 3B behavior |
|---|---|---|
| Proof revisions, comments, resolutions and decisions | Existing scoped DesignProofs REST APIs and PostgreSQL tables | Send post-commit versioned notices; clients reload the authorized proof workspace |
| Project conversation | Existing `/work` and `/work/messages` REST APIs and `work_messages` | Send post-commit message notice; clients reload chronological conversation |
| Identity and permissions | Existing `JSESSIONID`, `StaffPrincipal`, `StaffDirectory.current`, `DesignProofs.lead` | Reuse session/principal and exact customer/designer/sales/owner project scope on connection, room subscription, cursor validation and reauthorization |
| Presence and cursors | None (new ephemeral state only) | Spring simple STOMP broker, in-memory session/room registry, expiring role-only labels and normalized cursor position |

No database migration was added. A single Spring instance is required for shared presence/cursor delivery. The project assignment and account are rechecked on a 10-second lease; idle sockets expire after 45 seconds. Per-user tabs are grouped into one visible participant. Room sizes, packet size, cursor coordinates, cursor rate and PDF page bounds are limited.

## Transport and security

The transport is Spring WebSocket/STOMP, using Spring Boot's managed WebSocket starter and Spring Security messaging authorization. It is not proxied by the Next.js `/api` rewrite. Local browsers connect directly to the API's `/collaboration/ws` using the existing same-site HttpOnly session cookie. STOMP `CONNECT` carries the CSRF token from `/api/v1/auth/csrf`; no secret or session ID is placed in a URL. Exact local origins are configured in `APP_COLLABORATION_ALLOWED_ORIGINS`.

Production must route `wss://<site>/collaboration/ws` through the same-origin TLS reverse proxy to Spring and preserve the browser `Origin` and Upgrade headers. Set `NEXT_PUBLIC_COLLABORATION_URL` to that URL and allow only the exact HTTPS origin in Spring. See [COLLABORATION_PROXY.md](deployment/COLLABORATION_PROXY.md).

The client may only subscribe to its user destination for the selected enquiry and send heartbeat/cursor messages. The channel interceptor reuses project authorization and ignores user-supplied identity. Cursors are validated against a clean artwork revision belonging to that enquiry, including page and normalized 0–1 coordinates. Reauthorization removes/closes expired, disabled, password-changed, MFA-expired or no-longer-assigned sessions. Closed projects have no collaboration room. WebSocket authorization denies other message destinations.

Persistent notices use event envelope version 1: server event UUID, type, project ID, optional proof revision ID, server timestamp and minimal metadata. Notifications are emitted with `@TransactionalEventListener(AFTER_COMMIT)` for proof publication/comment/resolution/decision, task/assignment/file scan changes and project messages. They never approve proofs, alter files or change production. Clients re-fetch through existing scoped REST APIs, deduplicating by replacing snapshots rather than appending event payloads. A new proof is announced without silently switching the artwork; the user chooses “Load new revision.” Reconnect uses bounded exponential backoff, rechecks the current session, rejoins, refreshes proof/comments/conversation via a `sync.required` event, and resumes ephemeral state. REST remains usable when the socket is unavailable.

## Changed files

- `backend/build.gradle`, `backend/src/main/resources/application.yml`
- `backend/src/main/java/com/printflow/collaboration/` — broker, secure STOMP configuration/interceptor/handshake, controller and post-commit event publisher
- `backend/src/main/java/com/printflow/files/DesignProofs.java`, `backend/src/main/java/com/printflow/customer/WorkController.java`, `backend/src/main/java/com/printflow/identity/SecurityConfiguration.java`
- `backend/src/test/java/com/printflow/collaboration/CollaborationBrokerTest.java`
- `frontend/next.config.mjs`, `frontend/src/app/experience.css`, `frontend/src/components/ProjectCollaborationProvider.tsx`
- `frontend/src/components/DesignerProjectWorkspace.tsx`, `LiveWork.tsx`, `DesignProofs.tsx`, `ProofPdf.tsx`, `ProofImage.tsx`, `WorkConversation.tsx`
- `frontend/e2e/batch3-designer-workspace.spec.ts`
- `docs/API_CONTRACT.md`, `docs/ARCHITECTURE.md`, `docs/deployment/COLLABORATION_PROXY.md`

## Verification

Commands run from `printflow application/` unless stated otherwise. Native database URL in `backend/src/main/resources/application.yml` defaults to `jdbc:postgresql://localhost:5434/printflow`; `DB_URL` can override it. Initially ports 5434 and 5432 were unavailable and no PostgreSQL installation was present. With user approval, native PostgreSQL 17.11 was installed. Its Windows service runs on port 5434, but the execution account could not control that service or authenticate with the installer-generated superuser credential. A separate PostgreSQL cluster was therefore initialized under ignored `.local/postgres-batch3b/data-retry`, bound to `127.0.0.1:5436`; a generated application login is saved in user-level `DB_URL`, `DB_USERNAME` and `DB_PASSWORD`. No Docker was used.

| Check | Result |
|---|---|
| Frontend TypeScript | `npm.cmd run typecheck` — passed |
| Frontend production | `npm.cmd run build` — passed (Next.js 16.4.2; static generation completed) |
| Focused desktop/mobile browser tests | `PLAYWRIGHT_BASE_URL=http://localhost:3000 npm.cmd run test:e2e -- e2e/batch3-designer-workspace.spec.ts --project=desktop --project=mobile --max-failures=1` — 4 passed with PostgreSQL and Spring running. Covers designer/customer proof UI, revision navigation, PDF/image annotation coordinates, keyboard pin selection, stale approval rejection, mobile overflow and reconnect fallback. REST/auth are synthetic fixtures; these tests do not claim authenticated socket delivery. |
| Collaboration broker unit tests | `gradlew.bat test --tests com.printflow.collaboration.CollaborationBrokerTest` — passed (3 tests). |
| PostgreSQL-backed proof and live-work integration | `gradlew.bat integrationTest --tests com.printflow.DesignProofIntegrationTest --tests com.printflow.LiveWorkIntegrationTest` — passed (17 tests: 12 proof, 5 live-work) against PostgreSQL 17.11; Flyway validated/applied 25 migrations. |
| Spring API startup and health | `gradlew.bat bootRun` — passed after resolving a collaboration bean cycle by injecting `ObjectProvider<SimpMessagingTemplate>` and resolving it only when sending. API listens on port 8080; `/actuator/health` returned HTTP 200. |
| Frontend HTTP | `http://127.0.0.1:3000` — HTTP 200. |
| Real backend, two independently authenticated browser contexts | Not run. Authenticated presence, cursor delivery, revocation and after-commit event fan-out remain unverified. |

The focused browser suite uses intercepted synthetic REST/file fixtures. It verifies desktop/mobile designer and customer proof UI regressions, separate role data, exact revision/page comment navigation, zoomed PDF/image annotation coordinates, keyboard pin activation, stale-revision rejection and mobile overflow. It does not test successful authenticated STOMP delivery. Browser launch first failed before test startup; the approved retry ran the actual spec. The customer provider import and normalized PDF pin assertion were corrected in the prior pass. Starting the real API exposed the dependency cycle among the broker, WebSocket configuration and channel interceptor; `ObjectProvider<SimpMessagingTemplate>` broke that cycle without enabling circular references.

## Visual evidence

The actual browser captures in `output/batch3b-visual-evidence/ui-fallback/` show reconnecting state for designer and customer at 1440×900 and 390×844. They use synthetic application fixtures; the synthetic browser session cannot authenticate to the real socket, so they are not evidence of two-user presence, remote cursors or live synchronization. The Batch 3 proof-workspace screenshots remain in `output/batch3-visual-evidence/synthetic/`. The customer captures show the selected enquiry/proof panel and reconnect state. All four images were inspected: desktop panels fit the viewport, mobile text wraps cleanly, and no horizontal overflow, clipping or overlapping collaboration label was observed.

Evidence inventory, dimensions and test commands are indexed in [`output/batch3b-visual-evidence/`](../output/batch3b-visual-evidence/). A ZIP package is provided at `output/JD-Printex-Batch3B-Visual-Evidence.zip`.

Inspection checklist: desktop/mobile viewer layouts, visible connection state and scroll fit reviewed; proof/artwork screenshots show no observed clipping, overlap or mobile horizontal overflow. Dimensions were verified as 1440×900 and 390×844. Gallery, manifest, test report and ZIP are under `output/batch3b-visual-evidence/`.

## Known limitations and merge readiness

- During verification, Spring started against native PostgreSQL and the proof/live-work integration suites passed. The temporary PostgreSQL process did not remain available after its task shell ended; a later `bootRun` therefore exited with connection refused. At the latest check, ports 5434 and 5436 did not respond and Spring health was unavailable. Keep the PostgreSQL service in a persistent Windows service/session before testing again. Dedicated STOMP integration checks remain needed for successful CSRF CONNECT, two-user presence, cross-project denial, assignment/session revocation, cursors, and after-commit event fan-out.
- The requested two-browser-context scenarios (presence/disconnect/expiry, PDF/image cursors, comments/resolution, chat, proof notice/decision, reconnect, cross-origin denial and mobile collaboration) remain unverified, as do unrelated-customer, removed-designer, MFA/session and project-isolation negative cases.
- The in-memory broker is single-instance. Multi-instance deployment requires a shared relay before load balancing sockets. Production reverse-proxy setup is documented but not configured or deployed.
- Typing indicators are omitted; they were optional and no message text is sent over the live channel.
- Batch 6A editable-canvas integration point: keep comments tied to the existing immutable proof revision and normalized page/image coordinates; a future editor may consume the same `presence.snapshot`/`cursor.moved` overlay, but cannot replace source files or mutate approvals.
- **Merge readiness: blocked.** Native proof/live-work integration tests passed during the test session, but the local database/API are not currently persistent or available from the task shell. A real two-user authenticated WebSocket journey, security-negative cases and live-presence/cursor evidence remain unverified. Screenshots establish the reconnect fallback only. No merge or deployment is performed in this task.
