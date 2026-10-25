# Local Windows testing guide

This project runs natively on Windows for local development. Use PostgreSQL, Spring Boot and Next.js directly; Docker and Compose are not part of the supported local test loop.

## Expected local services

| Service | Default address | Startup source |
| --- | --- | --- |
| PostgreSQL | `127.0.0.1:5432` | Existing native PostgreSQL Windows service |
| Spring API | `127.0.0.1:8080` | `backend/Start-PrintFlow.ps1` or `backend/gradlew.bat bootRun` |
| Next.js UI | `127.0.0.1:3000` | `frontend/npm.cmd run dev` |

Check listeners and health before starting anything. A process can already belong to another checkout. Do not kill or restart it based only on a port number. Inspect the owning PID and command line first. To run a separate worktree UI, choose an unused port (for example 3001) and point Playwright at that exact origin.

## Java and Gradle

The backend requires Java 21. A JDK may be installed even when a fresh PowerShell session has no `JAVA_HOME` or `java` command. Search the user's JDK locations first:

```powershell
Get-Command java -ErrorAction SilentlyContinue
Get-ChildItem "$env:USERPROFILE\.jdks" -Directory -ErrorAction SilentlyContinue
Get-ChildItem "$env:USERPROFILE\.gradle\jdks" -Directory -ErrorAction SilentlyContinue
```

Set the discovered JDK only for the current PowerShell command/session; do not install a duplicate JDK to fix a missing environment variable:

```powershell
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\dragonwell-21.0.12"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
Set-Location backend
.\gradlew.bat compileJava
```

Run one Gradle invocation at a time per checkout. The wrapper and Gradle daemon hold build locks while a task is running. Wait for completion before starting the next compile or test. For a focused test, use the existing test class name, for example:

```powershell
.\gradlew.bat test --tests 'com.printflow.files.PrintDesignPdfTest'
```

Use the wrapper checked into `backend`; do not switch to Maven or Docker.

## Database environment and test isolation

The app reads its local database settings from the ignored `.env` file. Keep credentials private. PowerShell does not automatically export dotenv entries to a Gradle process, so map the configured `POSTGRES_*` values to the Spring/Gradle names (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`) in the current process without echoing the values. The default local URL uses PostgreSQL port 5432.

For database integration checks:

1. Confirm PostgreSQL is already listening on 5432 and check its service health.
2. Create or select a dedicated database such as `printflow_batch6a`; never run test cleanup against the regular `printflow` database.
3. Point only the test process at the dedicated database. Let Flyway create its schema from the checked-in migrations.
4. Use synthetic accounts, enquiries, uploads and test inbox addresses. Do not use real customer files or secrets in fixtures.
5. Verify the connection URL's database name before dropping temporary test data. Do not reset, truncate or remove the normal local database.

## UI and Playwright

Use the lockfile-pinned frontend dependencies. On a network-restricted machine, check the local npm cache before retrying package retrieval. Avoid linking a worktree's `node_modules` to another checkout: `npm ci` removes/replaces packages, and Windows junctions can cause it to modify the target checkout. Let each worktree own its dependency directory.

Start the UI only if the chosen port is free. Run a focused spec first:

```powershell
Set-Location frontend
$env:PLAYWRIGHT_BASE_URL = 'http://127.0.0.1:3000'
npm.cmd run test:e2e -- e2e/<focused-spec>.spec.ts --max-failures=1
```

If the spec targets the installed Microsoft Edge channel and fails before a test starts with `EPERM`/`EACCES`, retry that exact spec once with approved execution permission. Do not repeatedly launch browsers or rebuild for a browser executable permission failure. If localhost dev-resource checks reject `127.0.0.1`, use `http://localhost:<port>` for the isolated test server or configure the origin explicitly in development.

For visual UI work, capture and inspect actual app screenshots at 1440 × 900 and 390 × 844. Keep evidence under the active worktree's `output/` folder and record synthetic-vs-real data, role, route and viewport.

## End-of-task cleanup

Stop servers started by the current task after verification, unless the user explicitly asks to keep them running. Record the PID when launching a background process, then verify its command line and PID before stopping it. Do not stop a pre-existing PostgreSQL, Spring or Next process just because it uses the usual port. Remove only temporary databases or files whose exact target has been verified and which were created by the current task.
