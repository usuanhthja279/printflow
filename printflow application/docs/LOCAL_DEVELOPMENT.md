# Native Windows development

J.D.Printex runs locally without Docker. Requires Java 21, Node.js 22+, Python 3.12+ and an internet connection for first-time dependencies. Gradle 9.8.2 is pinned by its checked-in wrapper; no Maven installation is used.

## Commands

From the application folder:

```powershell
./Start-PrintFlow.ps1
./Stop-PrintFlow.ps1
./Start-PrintFlow.ps1 -SkipBuild
# Frontend live reload, with the existing backend JAR:
./Start-PrintFlow.ps1 -Dev -SkipBuild
```

Stop before rebuilding. Default startup builds a standalone Next.js preview and starts the backend JAR; `-Dev` uses Next.js live reload. Backend changes require a Gradle build and restart. Start/stop operations preserve database files.

Services bind to loopback only: website `127.0.0.1:3000`, API `127.0.0.1:8080`, project-local PostgreSQL `127.0.0.1:5432`. Temporary production-profile verification uses `127.0.0.1:8082`; it is stopped by the regular stop script. The project-managed local PostgreSQL cluster is separate from any system PostgreSQL service.

## Resources and files

- API Java heap: 64–384 MB; metaspace capped at 192 MB. Total process memory exceeds heap size.
- Gradle: daemon disabled, two workers, 256 MB heap; it runs during builds, not as the application server.
- Next.js build: two workers. Live reload generally uses more memory than the standalone preview.
- PostgreSQL: 32 MB shared buffers, 20 maximum connections. Set backend connection pool below that limit when adding workers or check instances.
- `.tools/`: ignored development runtime downloads.
- `.local/postgres-data/`: persistent local database. Do not delete to stop the app.
- `.local/*.log`: local service diagnostics; `.local/*.pid`: project process references.
- `.env`: ignored, randomly generated local database/demo credentials. Keep private; do not paste into chat or commit.

The PostgreSQL development cluster is isolated under this project and currently uses its initialization account for the application. This is a development convenience, not the production database-role design. Use synthetic data. A commercial deployment needs a restricted database role, backups/restore and the remaining security/privacy controls.

## Staff account and follow-ups

Open `/business` for the staff sign-in page. First native startup creates a development SALES account; its username/password are the `APP_STAFF_BOOTSTRAP_USERNAME` and `APP_STAFF_BOOTSTRAP_PASSWORD` entries in ignored `.env`. Read that file locally; never paste credentials into chat or issues. Passwords are stored as BCrypt hashes in PostgreSQL. Startup will not overwrite/reset an existing account, even if bootstrap environment values change.

The sales workspace supports manual enquiries, unassigned leads, claiming/returning assignments, status changes, follow-up time/next action, priority, overdue filters and internal activity. Reminders stay inside the workspace; no email or WhatsApp is sent. Other sales users' assigned enquiries are hidden. Owner/admin MFA, account administration, customer accounts, full CRM and production launch controls remain pending.

Default startup disables `/api/demo`. `./Start-PrintFlow.ps1 -SkipBuild -EnableDemo` explicitly enables the legacy dev-only API solely for synthetic Phase 0 regression checks; `/business` always uses staff sessions. Demo routes are absent under the production profile. Do not enable the legacy API for real staff/customer data.

Run `./scripts/Test-Backend.ps1` for unit tests, real PostgreSQL migrations and HTTP security/workflow integration tests. Run browser tests with the ignored bootstrap username/password loaded into the process environment. The suite uses installed Microsoft Edge and synthetic records; credentials are not printed. Legacy `smoke.ps1` and `verify_foundation.py` require explicit `-EnableDemo` startup. Run the `--restart` check separately from a temporary prod-profile check because restart stops that temporary instance.

## Portable PostgreSQL provenance

The pinned PostgreSQL **17.11** Windows development bundle is published by [Zonky embedded-postgres-binaries](https://github.com/zonkyio/embedded-postgres-binaries), a lightweight distribution intended for tests. It supplies native `postgres`, `initdb` and `pg_ctl` binaries, not a virtual machine or container. The 23,812,556-byte archive is fetched over HTTPS from its published artifact repository and checked against pinned SHA-256 `98040fae18dd9633ff95932125b0cecf0a45a1a9312e216e2a33ad03a31d4251` before extraction. Installed system PostgreSQL or an [EDB PostgreSQL distribution](https://www.postgresql.org/download/windows/) can be configured separately if required later.

Gradle distribution and wrapper checksums follow the [official wrapper verification mechanism](https://docs.gradle.org/current/userguide/gradle_wrapper.html). The wrapper JAR was checked before execution; the distribution checksum remains pinned in `backend/gradle/wrapper/gradle-wrapper.properties`.

## Verification

```powershell
. ./scripts/Use-PrintFlowTools.ps1
./scripts/Test-Backend.ps1
python scripts/static_validate.py
python scripts/verify_staff.py --restart
cd frontend
npm run typecheck
../scripts/Test-Browser.ps1
```

`--restart` stops/restarts native services and asserts the same token, enquiry and event history still work. It does not use Docker. `static_validate.py --archive` adds the archive-only rule rejecting `.env`; normal validation allows an ignored local environment.

Dockerfiles and Compose files remain optional packaging examples. Docker image builds have run, but the container runtime was not verified after the owner chose native development. No local task starts Docker or WSL.

## Owner onboarding and staff administration

Native startup now generates a separate first-owner identity and stable MFA encryption key in ignored `.env`. Retrieve `APP_OWNER_BOOTSTRAP_USERNAME` and `APP_OWNER_BOOTSTRAP_PASSWORD` privately. Sign in at `/business`, add the displayed time-based setup key to your authenticator, confirm a code and save the eight recovery codes securely. The automated browser test stops at the MFA gate and does not enroll this account for you.

Existing SALES credentials and accounts are preserved. Bootstrap never overwrites passwords, promotes sales accounts or silently re-enables disabled owners. Owner bootstrap exists only in dev/QA; production needs a reviewed provisioning procedure. If no enabled owner can sign in, do not add a public bypass; use a tested, authorized recovery runbook (still pending).

Verified owners can create staff, change roles, enable/disable accounts, reset passwords, inspect security activity and reassign enquiries. New/reset passwords require a change at first sign-in. Staff administration needs MFA verified within five minutes; use the re-verification button. Disabled/reset/role-changed sessions are invalidated on the next request. Only OWNER/SALES have enquiry workflows; other role workspaces await later modules.

Keep `.env` and its MFA key private and backed up securely. Losing or changing the key prevents stored seed decryption. Recovery codes are single-use; do not share them or authenticator setup screenshots. MFA counters/replay state survive restart, while HTTP sessions intentionally do not. Google Workspace email delivery and customer account recovery remain pending.

24 October local Gmail setup preparation: native DEV permits exact loopback HTTP only with explicit recipient restriction and a send-after activation boundary; historical and non-allowed mail is held. Production keeps HTTPS. Enable-LocalGmail.ps1 prompts privately, sends one setup test, saves settings on SMTP acceptance and restarts native services. Temporary mailbox is selected; private credential and real delivery verification remain pending. See GMAIL_LOCAL_SETUP.md.

Gmail activation update: system SPRING_MAIL_USERNAME / SPRING_MAIL_PASSWORD are now supported (SPRING_MAIL_PASSWROD compatibility alias). Google SMTP accepted the setup test and local mail is enabled for the selected inbox only, with historical queue hold. Actual inbox receipt/UI flow remains user validation. Proof/production/delivery milestone notifications remain planned. GMAIL_LOCAL_SETUP.md supersedes the earlier pending-credential note.
