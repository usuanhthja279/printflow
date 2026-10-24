# J.D.Printex — Native Development and Deployment Handbook

**Status:** Implementation plan; no remote environment or production deployment is claimed.  
**Version:** 2.0 — 23 October 2026  
**Business/domain:** J.D.Printex — `jdprintex.in`  
**Application directory:** `printflow application/` beside this handbook  
**Build/runtime decision:** Java 21, checked-in Gradle wrapper, Node.js and native PostgreSQL 17. **No Docker or Docker Compose.**  
**Hosting plan:** Native Windows DEV and isolated local QA; a separate Ubuntu VPS for future production, subject to approval.

The historical filename is retained so existing links still work. HDPrintX and its domain are superseded by J.D.Printex and `jdprintex.in`. Domain ownership and DNS configuration must be verified by the operator; this document does not establish either.

This handbook supersedes earlier container-based deployment instructions. Existing Dockerfiles and Compose files are legacy references, not steps to follow or evidence of a working deployment. Do not install or start a container engine for this plan. Preserve working application code and Flyway migrations.

Do not buy services, change DNS, register a tunnel, expose the app, provision a VPS or deploy without explicit authorization. Repository-local preparation and tests can proceed. Never fabricate verification results.

## 1. Implementation brief

Read `AGENTS.md`, `CODEX_MASTER_PLAN.md`, `README.md` and `docs/` inside the application directory. Apply the owner's native-development decision wherever older documents specify containers. Phase 1 implementation has been requested; production release remains gated.

1. Preserve Java package names, internal PrintFlow names, endpoint contracts and migration history. Customer-facing branding is J.D.Printex.
2. Use `backend/gradlew.bat` on Windows and `bash backend/gradlew` on Linux, running tasks from the backend directory. Do not add a Maven build, `pom.xml` or `mvn` commands. Gradle's dependency repository does not make this a Maven build.
3. Keep the working native DEV scripts. Prepare isolated native QA with separate data, credentials, ports, sessions and process ownership checks.
4. Keep browser API requests same-origin through the Next.js `/api/*` proxy. Bind the API and database to loopback.
5. Prepare optional Cloudflare Access-protected named Tunnel configuration for invited testers; do not publish it automatically.
6. Prepare native CI and future systemd/Caddy deployment artifacts. Verify the native CI runner setup before claiming CI passes.
7. Complete owner/staff authentication, privacy, anti-abuse and release gates before accepting real customer data.
8. Record actual commands, results and remaining operator actions. Distinguish implemented DEV features from planned QA/production infrastructure.

## 2. Environment boundaries

- **DEV:** Windows; native Java, Node.js and PostgreSQL; frontend `127.0.0.1:3000`, API `127.0.0.1:8080`, database `127.0.0.1:5434`. Working startup scripts exist.
- **QA/UAT:** Planned separate native Windows environment; frontend `127.0.0.1:3001`, API `127.0.0.1:8081`, database `127.0.0.1:5435`. Use independent cluster/data directory, database role, passwords, signing/encryption keys, cookie name, logs and PID files. Do not assume the current DEV scripts support QA.
- **CI:** Disposable runner with Java 21, Gradle wrapper, Node.js and a directly installed/started PostgreSQL service. No container service block or dependency on the developer laptop/tunnel in the target workflow.
- **PROD:** Future independently hosted Ubuntu 24.04 LTS VPS; native Java/Node/PostgreSQL processes managed by systemd, Caddy for public HTTPS. No dependence on the Windows machine.

Use synthetic data in DEV/QA. Run only the environment needed on memory-constrained machines. Local previews become unavailable when the PC sleeps or its connector stops; they are not production hosting.

A Hostinger KVM 2 VPS remains a proposed host, not a purchased or provisioned resource. Verify current region, capacity, cost, renewal terms and backup options before ordering; no fixed price or capacity guarantee is made here.

## 3. Existing native DEV workflow

From the directory containing this handbook:

```powershell
Set-Location -LiteralPath '.\printflow application'
./Start-PrintFlow.ps1
# Reuse existing builds:
./Start-PrintFlow.ps1 -SkipBuild
# Frontend hot reload with an existing backend build:
./Start-PrintFlow.ps1 -Dev -SkipBuild
# Stop project processes, preserving database files:
./Stop-PrintFlow.ps1
```

Prerequisites are Java 21, Node.js 22+ and Python 3.12+. The checked-in wrapper supplies Gradle; no global build-tool installation is needed. `scripts/Use-PrintFlowTools.ps1` discovers an installed Java 21 runtime.

The startup script downloads a checksum-pinned portable PostgreSQL development bundle if missing, initializes the isolated cluster, generates local credentials and builds/starts native services with hidden windows. Ignored `.tools/` contains downloaded tools; ignored `.local/` contains data, logs and PID files. Ignored `.env` contains credentials. Never paste its contents into chat, logs or Git.

Open `http://127.0.0.1:3000`; the staff workspace is `/business`. Local bootstrap credentials are in `.env`; they must not be hardcoded in source or shown in documentation. The current sales login is not evidence that owner MFA, customer identity or all Phase 1 roles are complete.

The legacy demo API is disabled by default and excluded from the `prod` profile. Use `-EnableDemo` only for explicit synthetic regression checks. Never use a demo key as production authentication.

### Builds and checks

Stop application processes before replacing their build output. From the application directory:

```powershell
./Stop-PrintFlow.ps1
. ./scripts/Use-PrintFlowTools.ps1
./scripts/Test-Backend.ps1
python scripts/static_validate.py
Push-Location frontend
npm ci --no-audit --no-fund
npm run typecheck
npm run build
Pop-Location
./Start-PrintFlow.ps1 -SkipBuild
./scripts/Test-Browser.ps1
python scripts/verify_staff.py --restart
```

`Test-Backend.ps1` prepares the native test database and runs Gradle build/integration checks. Direct backend tasks, once the test database environment is configured, are:

```powershell
Push-Location backend
./gradlew.bat --no-daemon build integrationTest
Pop-Location
```

Use `scripts/Start-ProductionBoundaryCheck.ps1` for temporary loopback production-profile checks on port 8082; follow `docs/LOCAL_DEVELOPMENT.md` for cleanup. A local production-profile test is not a deployment.

Consult `docs/PHASE0_VERIFICATION.md` and `docs/STAFF_SLICE_VERIFICATION.md` for recorded evidence. This handbook update itself does not rerun or certify application tests.

### Memory budget

Retain the existing bounded Gradle/JVM, PostgreSQL connection and frontend worker settings. Avoid concurrent full builds and multiple unnecessary environments. Do not reintroduce a container engine to solve native process lifecycle issues. Diagnose project logs and port ownership before restarting anything; never kill unrelated Java, Node.js or PostgreSQL processes.

## 4. Native QA implementation requirements — pending

Prepare explicit QA initialization/start/stop scripts rather than routing the DEV startup to another port alone. Required separation:

- Private `.env.qa`, independently generated secrets, `qa` Spring profile and disabled legacy demo endpoints by default.
- Separate PostgreSQL cluster at an ignored QA data path, port 5435, database and restricted application role. Initialization must refuse to overwrite an existing cluster.
- Separate web/API ports 3001/8081, logs, PID files and a distinct session cookie name. Cookies are not isolated merely by using different ports on the same hostname.
- Separate frontend build/output or release workspace. Next.js proxy configuration must target QA API 8081; a DEV build with a baked-in API target must not silently query DEV.
- Separate runtime configuration for QA; production secrets/data are forbidden. Seed only synthetic records.
- Start/stop scripts must validate process ownership, expected ports, data directory and readiness before mutation.

Verify that a QA-created record never appears in DEV, DEV sessions cannot authorize QA, QA restart preserves QA data, and stopping QA leaves DEV intact. Do not describe QA as implemented until these checks pass.

## 5. Optional private testing with Cloudflare

Planned hostnames:

- `dev.jdprintex.in` → named Tunnel → `http://127.0.0.1:3000`.
- `qa.jdprintex.in` → named Tunnel → `http://127.0.0.1:3001`.
- `jdprintex.in`, `www.jdprintex.in`, and optionally `app.jdprintex.in` → future VPS after release approval.

No separate public API hostname is needed initially. Forward the frontend only; Next.js proxies API requests internally. Do not expose API/database ports, PostgreSQL administration, debug interfaces or unrestricted actuator endpoints.

### Operator-controlled sequence

1. Verify domain ownership, authoritative DNS and existing Google Workspace MX/TXT records. Preserve email records when changing DNS providers.
2. Configure separate Cloudflare Access self-hosted applications covering every path on each preview hostname. Use explicit permitted identities, default deny, and no bypass rule. Protect `/api/*` as well as pages.
3. Prepare a named tunnel with exact hostname ingress entries and a final catch-all rejection. Validate Access JWTs at the connector using required Access validation, the correct team name and application audience.
4. Keep credentials/configuration containing secrets outside Git. Do not commit tunnel tokens, `cert.pem`, credential JSON, real allowlist emails or API keys.
5. Only after authorized configuration and policy checks, create DNS routes and start the connector. Do not use an unauthenticated temporary public link.
6. Test signed-out denial, unauthorized-user denial, authorized access, both API and page coverage, and inability to bypass Access at the origin. Shut down the connector if any negative case fails.

Cloudflare Access supplements application authorization; it does not replace staff/customer sessions or RBAC. Keep same-origin checks, CSRF and cookie protections. Configure exact permitted development origins where needed; do not disable origin protections globally. Remote QA over HTTPS must use appropriately secure cookies.

Tunnel installation, registration, DNS and remote access verification are pending operator authorization/configuration. Do not claim any preview hostname is live.

## 6. Native CI and release artifacts

Target CI must run independently of the Windows PC:

1. Check out source and install pinned Java/Node versions plus PostgreSQL 17 using a native runner package/service or a verified binary distribution.
2. Create a disposable database and restricted test credentials. Start PostgreSQL on loopback, wait for readiness and export test DB environment variables privately.
3. Run `bash gradlew --no-daemon build integrationTest` from `backend/`.
4. Run frontend `npm ci`, typecheck and build; run browser tests against a native synthetic stack when the workflow supports it.
5. Run structural, secret and dependency checks; retain reports without credentials or customer data.
6. Package the executable backend JAR, frontend runtime/build output and release metadata with source SHA and checksums. Verify frontend proxy configuration for its destination environment.

The workflow now starts PostgreSQL 17 directly on the Ubuntu 24.04 runner and stops it after the backend checks. It requires the runner image to provide `/usr/lib/postgresql/17/bin` and fails explicitly if absent. No remote CI execution is claimed; runner compatibility, browser CI and additional quality/security scans remain to be verified.

Promote a tested release artifact rather than building arbitrary source on production. Any environment-dependent frontend build must be declared and tested; do not falsely label differing builds as identical artifacts. Use a manual protected production workflow with owner approval, restricted credentials, migration/backup gates and post-deploy checks. PR jobs must never receive production secrets.

## 7. Future native VPS deployment — gated plan

No production provisioning or deployment is authorized by this handbook.

### Runtime and isolation

- Ubuntu 24.04 LTS, supported Java 21, Node.js runtime, PostgreSQL 17 and Caddy installed directly; no container runtime.
- Dedicated unprivileged application account, versioned release directories and systemd units for Spring Boot and Next.js. Use absolute working directories, bounded resources, restart limits and restricted filesystem permissions.
- Environment files/secrets outside the release tree and Git, readable only by the required service accounts. Do not place secrets in systemd command arguments or frontend public variables.
- Caddy is the only public web entry point. Bind Next.js, Spring Boot and PostgreSQL to loopback; allow only required SSH/HTTPS traffic through the firewall.
- Separate migration and runtime database roles where supported. No superuser application credentials.
- Explicit `prod` profile, secure HttpOnly/SameSite cookies, public base URL, trusted proxy configuration, restricted diagnostics and sanitized logs. Disable bootstrap/demo features.

Caddy should forward approved public hostnames to the local Next.js service. Next.js forwards `/api/*` to the private API. Obtain valid HTTPS certificates; if Cloudflare proxies production traffic, use validated origin TLS rather than weakening encryption. Do not publish production DNS before the origin is ready and approved.

### Deploy and rollback procedure

1. Verify release approval, checksums, test reports, capacity, configuration and working restore procedure.
2. Take a consistent backup; document current release and Flyway schema version.
3. Stage the immutable release. Apply reviewed backward-compatible migrations under controlled credentials; never rewrite applied migration history.
4. Start/restart services, check health and synthetic customer/staff flows, then observe errors and latency.
5. Roll back application artifacts only when compatible with the migrated schema. Do not automatically reverse destructive migrations or discard new customer data; use a reviewed recovery/roll-forward plan.

### Backups and operations

Use full PostgreSQL backup tools (`pg_dump`/`pg_restore` or an approved physical backup/PITR solution), encrypted offsite storage, retention, monitoring and scheduled restore drills. The slim portable DEV bundle does not include every production backup tool; do not assume those executables are available.

Back up artwork/object storage and necessary configuration as those features are introduced. Test recovery into an isolated environment, verify record counts and application workflows, and record actual recovery time/data loss. Proposed RPO/RTO values are targets until demonstrated. A local database directory or VPS disk snapshot alone is not a tested disaster-recovery strategy.

## 8. Configuration, secrets and Google Workspace email

Separate DEV, QA, CI and PROD configuration. Keep all populated environment files, database data, logs containing private information, provider credentials and backups out of Git. Example files must contain placeholders only.

Current native DEV configuration is loaded by the existing scripts. Preserve `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, Spring profile, application flags and bootstrap settings as implemented; inspect code before introducing new configuration names. Frontend internal API destinations are private runtime/build configuration, never provider secrets in `NEXT_PUBLIC_*`.

The owner selected **Google Workspace / Gmail** for verification and password-reset email. Plan authenticated Gmail SMTP with STARTTLS on port 587 and supported credentials, or an approved Workspace relay/API arrangement after administrator review. Keep the sending address and credentials private, and preserve required mail DNS records. Do not use the account's ordinary password as an application secret.

Email flows, credentials and real delivery verification are not established by this choice. Keep delivery disabled until implemented, configured and tested with approved recipients. Never label Google Workspace/Gmail as connected merely because an example setting exists. Verification/reset tokens must be expiring, single-use, safely stored and absent from logs.

## 9. Acceptance gates and evidence

**Native DEV:** Gradle wrapper builds with Java 21; native PostgreSQL migrations and integration tests pass; frontend checks pass; staff/public synthetic workflows and restart persistence pass; listeners remain loopback-only; no container engine is needed.

**Native QA:** Separate clusters, credentials, sessions, processes and builds are demonstrated; no DEV/QA cross-access; QA stop/restart leaves DEV intact; owner UAT uses synthetic data.

**Private preview:** Explicit authorization, domain/DNS verification, Access default-deny policies, connector JWT validation and external negative tests pass before inviting testers.

**Production readiness:** Owner MFA and role administration, customer identity as required, ownership/BOLA tests, privacy/consent, guest anti-abuse, TLS, secure secrets, verified backups/restores, monitoring, incident response and owner sign-off are complete. Future uploads/payments/providers need their own applicable security gates. Default-disabled public intake must not be enabled merely to bypass a failed check.

Report each gate as passed, failed, pending or not applicable, with evidence. Foundation/staff-slice results do not imply full Phase 1 completion, remote CI success, live email, protected tunnel availability or production eligibility.

## 10. Planned deliverables and troubleshooting

Existing DEV scripts and verification documents are the starting point. Pending deliverables are native QA lifecycle scripts and examples, verified native PostgreSQL CI, private tunnel examples, systemd/Caddy templates, a protected manual release workflow and tested backup/restore runbooks. Do not invent filenames and describe them as present before creating them.

For native startup failures, inspect `.local/` logs and the actual process/port owner. For database failures, check the intended cluster, credentials and readiness without printing secrets. For API failures, verify the Next.js proxy destination and Spring readiness. For tunnel failures, check the local frontend and Access/ingress policies; never disable authentication to make a preview work. For environment cross-talk, stop the affected environment and correct its data paths, cookie name and proxy build configuration before continuing.

Final implementation reports must list changed files, actual commands/results, untested external dependencies and exact operator actions. A documentation edit is not a deployment or a successful restore drill.

## 11. Official implementation references

Recheck relevant official documentation when implementing provider or runtime configuration:

- Gradle Wrapper: https://docs.gradle.org/current/userguide/gradle_wrapper.html
- Spring Boot Gradle plugin: https://docs.spring.io/spring-boot/gradle-plugin/index.html
- PostgreSQL backup and restore: https://www.postgresql.org/docs/17/backup.html
- Next.js self-hosting: https://nextjs.org/docs/app/guides/self-hosting
- Caddy reverse proxy: https://caddyserver.com/docs/caddyfile/directives/reverse_proxy
- Cloudflare named tunnels: https://developers.cloudflare.com/cloudflare-one/networks/connectors/cloudflare-tunnel/do-more-with-tunnels/local-management/create-local-tunnel/
- Cloudflare Access origin parameters: https://developers.cloudflare.com/tunnel/reference/origin-parameters/
- GitHub protected deployment environments: https://docs.github.com/en/actions/how-tos/deploy/configure-and-manage-deployments/manage-environments
- Google Workspace application email options: https://support.google.com/a/answer/176600
