# Batch 3B focused verification

All commands run from `printflow application/` unless noted. No Docker was used.

- `frontend`: `npm.cmd run typecheck` — passed.
- `frontend`: `npm.cmd run build` — passed; Next.js production compilation, typecheck and static generation completed.
- `frontend`: `$env:PLAYWRIGHT_BASE_URL='http://localhost:3000'; npm.cmd run test:e2e -- e2e/batch3-designer-workspace.spec.ts --project=desktop --project=mobile --max-failures=1` — passed, 4 tests (designer/customer × desktop/mobile). App UI uses synthetic API/file fixtures.
- `backend`: `.\gradlew.bat test --tests 'com.printflow.collaboration.CollaborationBrokerTest'` — passed, 3 tests.
- `backend`: `.\gradlew.bat integrationTest --tests 'com.printflow.DesignProofIntegrationTest' --tests 'com.printflow.LiveWorkIntegrationTest'` — passed, 17 tests (12 proof and 5 live-work) against native PostgreSQL 17.11. Flyway applied 25 migrations.
- PostgreSQL setup: installed native PostgreSQL 17.11 after approval. The Windows service on 5434 could not be controlled/authenticated from this execution account, so a separate loopback-only test cluster runs from `.local/postgres-batch3b/data-retry` on port 5436. User-level `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` point to it.
- `backend`: `.\gradlew.bat bootRun` — passed after fixing the collaboration bean cycle by injecting `ObjectProvider<SimpMessagingTemplate>`; `/actuator/health` returned HTTP 200.
- `frontend`: `PLAYWRIGHT_BASE_URL='http://localhost:3000' npm.cmd run test:e2e -- e2e/batch3-designer-workspace.spec.ts --project=desktop --project=mobile --max-failures=1` — passed, 4 tests after API startup. These use synthetic REST/auth fixtures and prove reconnect fallback, not successful authenticated STOMP delivery. Frontend HTTP returned 200.
- Successful two-user STOMP delivery, authorization/revocation and cursor/event fan-out remain unverified.
- Follow-up relaunch check: PostgreSQL on 5436 had stopped with the temporary task shell. `bootRun` was retried with the private user-level DB settings and Java 21, but Flyway again received connection refused. The existing Next dev server was already running on port 3000; a duplicate start selected 3001 and exited because the existing project server owns its lock. At final check the original API health endpoint and PostgreSQL ports 5434/5436 did not respond. A persistent PostgreSQL service/session is still needed to keep the backend available for UI testing.

The first Playwright start was blocked before test code by the Windows restricted execution context. The approved retry ran the actual focused spec. The customer provider named import and normalized PDF pin assertions were corrected in the previous pass. Starting Spring against PostgreSQL exposed a circular dependency among the broker, channel interceptor and WebSocket configuration; it was fixed using lazy `ObjectProvider` resolution without enabling circular references.
