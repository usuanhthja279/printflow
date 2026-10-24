# DEV, QA, UAT and production environments

## Windows setup (local developer)
Native local development is the default: Java 21 + checked-in Gradle wrapper + Node.js 22+ + portable PostgreSQL 17. Python 3.12+ extracts the development database bundle. Run `./Start-PrintFlow.ps1`; no Docker is needed. See `LOCAL_DEVELOPMENT.md` for lifecycle and resource limits. Docker/Compose are not part of the active plan. See `DEVELOPMENT_HANDBOOK.md` and the root infrastructure handbook.

## Environment table
| Env | Deployment | Data | Inbound | CI and gating |
|---|---|---|---|---|
| Local | Native PostgreSQL/Java/Node | Generated fictional data | loopback only | Unit, manual smoke |
| Shared DEV | Private/staff-accessible stack | Synthetic | TLS/private network | Merge + integration |
| QA/UAT | Isolated production-like stack | Synthetic/anonymized | staff + invited testers | E2E, security, owner sign-off |
| PROD | Cloud platform, TLS, secret store | Real business data only after Phase 1 hardening | public HTTPS via proxy | Manual approval, smoke, monitoring |

**Do not reuse QA databases, credentials or artifact buckets in PROD.** Do not make QA and owner demo public. Existing Compose files are historical references, not the active deployment path. Production is planned as native systemd/Caddy/PostgreSQL services.

### Secrets and configuration

Keep passwords, encryption keys, provider API tokens, OAuth client secrets, database credentials and production base URLs in environment-specific secret management. `.env` is ignored; `.env.example` is a deliberately insecure local example. Do not expose provider keys using `NEXT_PUBLIC_*`, build args or web bundles.

### Data and storage

- PostgreSQL managed or private network; enable backups, PITR where available, encrypted offsite copies.
- Artwork/object storage private, versioned and life-cycle managed; never expose public buckets by default.
- Separate secrets and storage per environment; encrypted backups and restore drills.
- Phase 0 uses local Postgres volume; it is not a substitute for off-host backups.

### CI/CD

1. PR: syntax, formatting, backend tests (`./gradlew.bat test`), frontend install+typecheck+build, secrets/dependency scans.
2. Merge to main: build immutable native release artifacts with Git SHA; promote only to an implemented, approved environment.
3. Release candidate: deploy the tested release artifacts to native QA; run Playwright E2E, smoke, load and penetration test where appropriate.
4. UAT: printing owner approves representative workflows and signed checklist.
5. PROD: gated promotion, backup snapshot, backward compatible Flyway migrations, roll forward or rollback plan.
6. Post-deploy: smoke paths, provider webhook checks, error and latency monitoring, rollback criteria.

### Git branch model
`main` (releasable), `feature/PF-XXX-...`, `hotfix/...`. Short-lived PR branches with requirements/tests and screenshots. Separate environment **by deployment**, not one git branch per environment.

### Before enabling real production users (blocking work)

- Replace `/api/demo` with authenticated, authorized staff endpoints. Add secure password/OTP login, sessions, MFA for owners/admins, and BOLA tests.
- Add anti-bot and abuse rate limits to guest enquiry creation, CAPTCHA/alternative, CSRF design and activity monitoring.
- Legal/privacy reviewed notice, consent, personal data lifecycle, incident response and audit retention.
- External TLS/reverse proxy, WAF where justified, secret manager and hardened native systemd runtime.
- Secure artwork upload with scanning, quotas, MIME/content verification, antivirus and isolated PDF tooling.
- Backups **tested**, documented restore and incident runbooks; logging scrubs sensitive data.
- A business domain and DNS ownership validation; connectivity and cost budget approved.

### Disaster recovery targets
Proposed starting **RPO <= 1 hour, RTO <= 4 hours**; only claim after restore drills confirm it. A warm failover setup may be needed for strict promises. Primary emergency procedure: stop writes, assess backup and DB consistency, restore DB and objects in isolated environment, reconcile payment/provider state before reopening.

### Native isolation and CI status

DEV scripts are implemented. Native QA needs separate PostgreSQL data/credentials/ports, cookie names, frontend output/proxy configuration and process lifecycle scripts; it is not implemented by changing a port alone. Follow the infrastructure handbook for the acceptance checks.

CI now declares a native PostgreSQL 17 process on the Ubuntu 24.04 runner, rather than a container service. It requires `/usr/lib/postgresql/17/bin` from the runner image and fails explicitly if missing. No remote CI run is claimed. Validate the runner installation and dependency/security scans before treating CI as a release gate.

Production infrastructure, private Cloudflare Access/tunnel setup, native QA verification and offsite restore drills remain pending. No real environment is provisioned by these documents.
