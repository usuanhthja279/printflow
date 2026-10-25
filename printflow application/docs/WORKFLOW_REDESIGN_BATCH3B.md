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

Commands run from `printflow application/` unless stated otherwise. Native database URL in `backend/src/main/resources/application.yml` defaults to `jdbc:postgresql://localhost:5434/printflow`; `DB_URL` can override it. The preflight checked ports 5434 and 5432; both were unavailable. No Docker was used.

| Check | Result |
|---|---|
| Frontend TypeScript | `npm.cmd run typecheck` — passed |
| Frontend production | `npm.cmd run build` — passed (Next.js 16.4.2; static generation completed) |
| Focused desktop/mobile browser tests | `PLAYWRIGHT_BASE_URL=http://localhost:3000 npm.cmd run test:e2e -- e2e/batch3-designer-workspace.spec.ts --project=desktop --project=mobile --max-failures=1` — 4 passed (designer and customer flows on desktop and mobile). Covers revision navigation, zoomed PDF/image normalized annotation coordinates, keyboard pin selection, stale approval rejection, mobile overflow, and screenshot capture. Requires the already-running local frontend; run with approved Windows execution access if Edge cannot launch inside the restricted process. |
| Backend unit tests + integration source compilation | `gradlew.bat test integrationTestClasses` — passed; the 3 `CollaborationBrokerTest` cases passed and integration test sources compiled. |
| PostgreSQL-backed `DesignProofIntegrationTest` and other auth/proof lifecycle tests | Not run: native PostgreSQL on both configured/default port 5434 and common port 5432 was unavailable. Source compilation is not a database test pass. |
| Real backend, two independently authenticated browser contexts | Not run: API/PostgreSQL services are unavailable. No live participants or event delivery are claimed. |

The focused browser suite uses intercepted synthetic REST/file fixtures. It verifies desktop/mobile designer and customer proof UI regressions, separate role data, exact revision/page comment navigation, zoomed PDF/image annotation coordinates, keyboard pin activation, stale-revision rejection and mobile overflow. It does not test actual STOMP delivery or backend authorization. Browser launch first failed in the restricted process before test startup; the documented approved retry ran the actual spec. During that run, the customer collaboration provider's named export import was corrected. The PDF coordinate assertion was also made against its normalized inline values, which are stable across desktop/mobile device scale factors.

## Visual evidence

The actual browser captures in `output/batch3b-visual-evidence/ui-fallback/` show the reconnecting state for designer and customer at 1440×900 and 390×844. They use synthetic application fixtures and show the unavailable state because the backend WebSocket is not running in this environment. They are not evidence of two-user presence, remote cursors or live synchronization. The Batch 3 proof-workspace screenshots remain in `output/batch3-visual-evidence/synthetic/`. The refreshed customer screenshots show the selected enquiry/proof panel and reconnect state rather than the dashboard top. All four images were inspected: desktop panels fit the viewport, the mobile project title/status wrap cleanly, and no horizontal overflow, clipping or overlapping collaboration label was observed. The mobile captures are viewport screenshots at 390×844; desktop captures are 1440×900.

Evidence inventory, dimensions and test commands are indexed in [`output/batch3b-visual-evidence/`](../output/batch3b-visual-evidence/). A ZIP package is provided at `output/JD-Printex-Batch3B-Visual-Evidence.zip`.

Inspection checklist: review desktop/mobile viewer layouts, visible connection state and scroll fit; inspect proof/artwork screenshots for clipping, overlap and pin alignment. Final findings and screenshot dimensions are pending the latest capture run. Gallery, manifest, test report and zip are under `output/batch3b-visual-evidence/`.

## Known limitations and merge readiness

- This code has not been run against a live Spring/PostgreSQL session. STOMP negotiation, CSRF CONNECT, authorization denial, cross-project isolation, assignment revocation, session expiry and after-commit delivery need native-backend integration verification.
- The requested two-browser-context end-to-end scenarios (presence/disconnect/expiry, PDF/image cursors, comments/resolution, chat, proof notice/decision, reconnect, cross-origin denial and mobile collaboration) remain unverified. The required unrelated-customer, removed-designer, MFA/session and project isolation negative tests also remain blocked pending a database-backed application environment.
- The in-memory broker is single-instance. Multi-instance deployment requires a shared relay before load balancing sockets. Production reverse-proxy setup is documented but not configured or deployed.
- Typing indicators are omitted; they were optional and no message text is sent over the live channel.
- Batch 6A editable-canvas integration point: keep comments tied to the existing immutable proof revision and normalized page/image coordinates; a future editor may consume the same `presence.snapshot`/`cursor.moved` overlay, but cannot replace source files or mutate approvals.
- **Merge readiness: blocked.** Native PostgreSQL integration/security tests, a real two-user WebSocket journey, and live-presence/cursor evidence remain unverified. The inspected screenshots only establish the graceful unavailable/reconnecting UI. No merge or deployment is performed in this task.
