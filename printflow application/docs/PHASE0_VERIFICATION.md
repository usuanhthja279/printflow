# Phase 0 verification

## Native foundation completion — 23 October 2026

**Current local foundation status: VERIFIED using native Windows services.** The owner explicitly requested replacing Maven with Gradle and replacing Docker local development with native processes. This supersedes the original container-runtime local gate; optional container runtime/remote CI remain unverified, and no production-readiness claim is made.

- Reused installed Dragonwell Java 21.0.12; added JDK discovery script. Migrated the backend to checksum-pinned Gradle 9.8.2 wrapper; removed pom.xml; migrated Dockerfile, CI and active commands. Official wrapper JAR and distribution checksums verified.
- `./backend/gradlew.bat -p backend build` — PASS on Windows, six JUnit tests, no failures. Gradle test/bootJar also passed inside the optional Linux image build. Container startup subsequently failed with a Docker 502/engine loss and was abandoned per owner preference. Docker is not started by native scripts.
- Native portable PostgreSQL 17.11 initialized with SCRAM credentials and loopback binding. Flyway V1 applied; restart logs show migration checksum validation and schema version 1. Hibernate validates the real database schema.
- Fixed runtime startup defects: added Spring Boot 4 Flyway starter, mapped digest to SQL CHAR to match V1, removed the obsolete Jackson 2 WRITE_DATES_AS_TIMESTAMPS property rejected by Jackson 3. Existing migration was preserved.
- `npm run typecheck`, `npm run build` — PASS; standalone preview starts with two build workers. Node 22 container frontend build also passed before native migration.
- `scripts/smoke.ps1` — PASS for health, catalogue, create, owner update and tracking. Fixed the scaffold's nested PowerShell REST-array handling and unblocked its downloaded-file execution marker.
- `python -u scripts/verify_foundation.py --prod-base http://127.0.0.1:8082` — PASS: catalogue products, invalid inputs, missing/wrong demo key, missing/wrong tracking token, public DTO confidentiality, real concurrent status requests, idempotent closure, production demo exclusion and default intake-disabled behavior.
- `python -u scripts/verify_foundation.py --restart` — PASS: native database/API restart preserves the exact enquiry, tracking token and history. Initial captured-pipe restart attempt hung and was interrupted; fixed the subprocess output handling, restarted services and completed the check successfully.
- `npm run test:e2e` — PASS: two browser tests, desktop and mobile (Microsoft Edge), real form submission through Next.js to PostgreSQL, all four service links, owner update and customer timeline. Corrected the test to await navigation and target the exact product combobox; no application behavior was weakened to pass it.
- `python scripts/static_validate.py` — PASS: 28 required artifacts; normal local validation permits ignored .env, archive mode separately rejects it. Native PowerShell lifecycle scripts parse successfully.

Start/stop uses only native PostgreSQL, Java and Node, with persistent ignored .local data. Java heap, build workers and PostgreSQL buffers are capped as documented in LOCAL_DEVELOPMENT.md. The owner has explicitly requested proceeding to secure staff access/follow-ups after this gate; Phase 1 starts with a scoped staff slice, not the entire production release.

---

## Earlier Windows stabilization — historical checkpoint

**Status at this earlier checkpoint: frontend build verified; Phase 0 incomplete.** The historical authoring checks below are retained separately and are not new results.

Actual environment: Windows PowerShell, Node **v24.15.0**, npm **11.12.1**. Locked frontend packages include Next **16.4.2**, React/React DOM **19.2.7** and TypeScript **5.9.3**. Java and Maven were not found on PATH. The frontend passed on Node 24; the Node 22 Docker/CI environment remains unverified.

### Commands and actual results

- `python scripts/static_validate.py` — PASS: 23 required artifacts and structural conventions. Repeated after changes, PASS. This is not Java or integrated verification.
- `docker compose config --quiet` — PASS. No local .env was created. Local Compose defaults were used only for configuration validation.
- `docker info --format '{{.ServerVersion}}'` and `docker compose up --build -d` — FAILED: Docker Desktop Linux engine named pipe absent.
- `docker desktop start` — FAILED: Docker Desktop CLI could not locate registry key `SOFTWARE\\Docker Inc.\\Docker Desktop`. `docker desktop status` also failed to retrieve engine status. No containers or database were started.
- Initial `npm view` queries — FAILED with DNS ENOTFOUND within the restricted environment. Registry access succeeded outside that restriction.
- From frontend: `npm install --package-lock-only --ignore-scripts --no-audit --no-fund --fetch-retries=0 --fetch-timeout=20000` — PASS; generated package-lock.json without changing requested versions.
- From frontend: `npm ci --no-audit --no-fund --fetch-retries=0 --fetch-timeout=20000` — PASS; installed 29 packages. No dependency vulnerability audit was performed by these commands.
- From frontend: initial `npm run typecheck` — FAILED at business/page.tsx: a string status was passed to the status-choice union. Fixed DemoEnquiry.status with a shared EnquiryStatus union.
- From frontend: `npm run typecheck` — PASS after the fix and again after the build. An accidental invocation from the application root failed because that directory has no package.json; the subsequent frontend-directory invocation is the reported pass.
- From frontend: initial `npm run build` — compiled and typechecked, then FAILED with EPERM creating generated page output under .next. Rerun outside the filesystem restriction — PASS, prerendered `/`, `/request-quote`, `/track`, `/business` and the not-found page. Next updated tsconfig.json and next-env.d.ts to its required/generated settings.
- `node --check frontend/next.config.mjs` — PASS.
- From backend: `mvn -B test` — FAILED to execute: mvn command not found. Java build/tests, dependency resolution and packaging remain unverified.
- From frontend: `npm run start -- --hostname 127.0.0.1` — local frontend preview ready at http://127.0.0.1:3000. Next emitted its standalone-output warning; this is a local preview, not Docker runtime verification. Initial sandbox loopback checks were denied/timed out; the server was restarted outside that restriction.
- PowerShell `Invoke-WebRequest` checks against the accessible preview — PASS: HTTP 200 plus J.D.Printex branding for `/`, `/request-quote?product=WEDDING_CARDS`, `/track`, `/business`; PASS: homepage contains all four expected product query links. These are HTTP checks, not browser interaction tests.

### Changes verified or awaiting execution

- J.D.Printex branding, owner-confirmed homepage categories, API/fallback catalogue additions and service links that preselect valid enquiry product codes. Existing catalogue codes remain accepted; no schema migration required.
- Frontend Dockerfile and CI now use npm ci with the generated lockfile. Actual image and remote CI runs are pending; no Git repository is initialized in this folder.
- Same-status updates now return unchanged even when CLOSED. Added tests for status transitions, repeated updates without extra events and wrong/missing tracking-token rejection before history access. **These Java tests have not run.**
- Business discovery answers and the process/development review are in DISCOVERY_QUESTIONS.md and BUSINESS_START.md.

### Remaining Phase 0 gate

Restore Docker Desktop Linux engine access or provide Java 21/Maven and PostgreSQL. Then execute backend test/verify/package, Docker build/start, Flyway migration/schema validation, both positive and negative API checks, prod-profile exclusion, disabled intake defaults, restart persistence, and browser/mobile/product-selection QA. Address concurrent status updates with database-backed tests. Run a clean Node 22 build through Docker/CI. No claim of backend correctness, production readiness, owner UAT or Phase 0 completion is made here. No Phase 1 feature work was started.

---

## Historical authoring-environment evidence

**Date:** 2026-10-23. **Status:** static verification completed; end-to-end Docker deployment **not executed**. Codex must run the actual integration build with network connectivity before marking Phase 0 complete.

| Check | Status | Evidence |
|---|---|---|
| Project structural validator | PASS | `python scripts/static_validate.py` (23 expected artifacts and conventions) |
| `backend/pom.xml` XML parsing | PASS | Python XML parser |
| `frontend/package.json` JSON parsing | PASS | Python JSON parser |
| YAML parsability | PASS | PyYAML for Compose and GitHub Actions, Spring YAML |
| Java syntax | PASS | JavaCompiler `JavacTask.parse()` of 15 Java source files, 0 syntax diagnostics |
| Tracking token code | PASS | `javac` compiled `TokenUtil.java`; standalone harness verifies token generation and digest match |
| TypeScript/TSX parse | PASS | TypeScript `transpileModule` across 8 TS/TSX source files, 0 syntax diagnostics |
| Next config parse | PASS | `node --check` |
| Bash smoke syntax | PASS | `bash -n scripts/smoke.sh` |
| Mermaid diagram presence | PASS | At least six diagrams; render not visually validated |
| Actual Spring Boot Maven compile/test | **NOT RUN** | Maven missing; remote Maven Central could not be resolved |
| Actual Next.js npm install/build/typecheck | **NOT RUN** | Remote NPM registry inaccessible; dependency packages unavailable |
| Docker Compose build/start | **NOT RUN** | Docker binary/daemon unavailable in authoring environment |
| Live PostgreSQL migration/API smoke | **NOT RUN** | Depends on Docker/Maven build |
| Browser responsive/E2E QA | **NOT RUN** | Depends on running app/browser automation |

## Next verification assignment (Codex)

1. Open in networked Windows environment with Docker Desktop.
2. `Copy-Item .env.example .env`, `docker compose config`, `docker compose up --build -d`.
3. `docker compose ps`, `docker compose logs api`, `docker compose logs web`.
4. `./scripts/smoke.ps1` from repo root. Expect health, catalog, insert, owner status update and tracking success.
5. Submit visually through `/request-quote`, unlock `/business`, update, track at `/track`.
6. `cd backend; mvn -B verify`, `cd frontend; npm install; npm run typecheck; npm run build`.
7. Create and commit package-lock.json; change Docker/CI to `npm ci`, test clean rebuild.
8. Confirm negative cases: wrong token, invalid quantity, wrong demo key, prod profile excludes demo route, database data persists after restart.
9. Add Playwright and Spring integration tests if appropriate; record **actual** screenshots, command output, dependency versions, timings and any remaining failures in this file.

**No production go-live approval is implied by these static checks.** Phase 1 security controls are required first.
