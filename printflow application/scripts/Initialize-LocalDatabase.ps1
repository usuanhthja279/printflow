$ErrorActionPreference='Stop'
$taskRoot=Split-Path $PSScriptRoot -Parent
$taskTools=Join-Path $taskRoot '.tools'
$taskPg=Join-Path $taskTools 'postgresql-17.11'
$taskLocal=Join-Path $taskRoot '.local'
$taskData=Join-Path $taskLocal 'postgres-data'
New-Item -ItemType Directory -Path $taskTools,$taskLocal -Force | Out-Null
if (-not (Test-Path (Join-Path $taskPg 'bin\postgres.exe'))) {
    $taskArchive=Join-Path $taskTools 'postgresql-17.11.jar'
    $taskUrl='https://repo.maven.apache.org/maven2/io/zonky/test/postgres/embedded-postgres-binaries-windows-amd64/17.11.0/embedded-postgres-binaries-windows-amd64-17.11.0.jar'
    Invoke-WebRequest -Uri $taskUrl -OutFile $taskArchive -TimeoutSec 120
    $taskHash=(Get-FileHash -LiteralPath $taskArchive -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($taskHash -ne '98040fae18dd9633ff95932125b0cecf0a45a1a9312e216e2a33ad03a31d4251') { throw 'PostgreSQL development bundle checksum mismatch' }
    $taskPython="import io,tarfile,zipfile,sys; from pathlib import Path; p=Path(sys.argv[1]); p.mkdir(exist_ok=True); z=zipfile.ZipFile(sys.argv[2]); t=tarfile.open(fileobj=io.BytesIO(z.read('postgres-windows-x86_64.txz')),mode='r:xz'); t.extractall(p,filter='data')"
    python -c $taskPython $taskPg $taskArchive
    if ($LASTEXITCODE -ne 0) { throw 'PostgreSQL extraction failed (Python 3.12+ required).' }
}
if (-not (Test-Path (Join-Path $taskRoot '.env'))) {
    function New-LocalSecret {
        $taskBytes=New-Object byte[] 32
        $taskRng=[Security.Cryptography.RandomNumberGenerator]::Create()
        try { $taskRng.GetBytes($taskBytes) } finally { $taskRng.Dispose() }
        return [Convert]::ToBase64String($taskBytes).TrimEnd('=').Replace('+','-').Replace('/','_')
    }
    @('POSTGRES_DB=printflow','POSTGRES_USER=printflow',('POSTGRES_PASSWORD='+(New-LocalSecret)),('DEMO_ADMIN_KEY='+(New-LocalSecret)),'PUBLIC_ENQUIRIES_ENABLED=true') | Set-Content (Join-Path $taskRoot '.env') -Encoding ascii
    Write-Host 'Created ignored local .env with random development credentials.'
}
foreach ($taskLine in Get-Content (Join-Path $taskRoot '.env')) {
    if ($taskLine -match '^([A-Z][A-Z0-9_]*)=(.*)$') { [Environment]::SetEnvironmentVariable($Matches[1],$Matches[2],'Process') }
}
if (-not (Test-Path (Join-Path $taskData 'PG_VERSION'))) {
    $taskPwFile=Join-Path $taskLocal 'init-password.tmp'
    try {
        [IO.File]::WriteAllText($taskPwFile,$env:POSTGRES_PASSWORD)
        & (Join-Path $taskPg 'bin\initdb.exe') -D $taskData -U $env:POSTGRES_USER --encoding=UTF8 --locale=C --auth=scram-sha-256 --pwfile=$taskPwFile
        if ($LASTEXITCODE -ne 0) { throw 'Native PostgreSQL initialization failed' }
    } finally { if (Test-Path $taskPwFile) { Remove-Item -LiteralPath $taskPwFile } }
    # Native runtime is an isolated, development-only cluster. Never use real customer data.
    if ($env:POSTGRES_DB -notmatch '^[a-z][a-z0-9_]*$') { throw 'Local database name must use lowercase letters, digits and underscores.' }
    ('CREATE DATABASE '+$env:POSTGRES_DB+';') | & (Join-Path $taskPg 'bin\postgres.exe') --single -D $taskData template1
    if ($LASTEXITCODE -ne 0) { throw 'Native development database creation failed' }
    @("listen_addresses = '127.0.0.1'","port = 5432","shared_buffers = '32MB'","max_connections = 20","work_mem = '2MB'","maintenance_work_mem = '32MB'","logging_collector = off") | Add-Content (Join-Path $taskData 'postgresql.conf')
}
# Enforce the project-local listener even for a cluster initialized by an older script version.
$taskConfigPath=Join-Path $taskData 'postgresql.conf'
$taskConfigLines=Get-Content -LiteralPath $taskConfigPath | Where-Object { $_ -notmatch '^\s*(listen_addresses|port)\s*=' }
$taskConfigLines | Set-Content -LiteralPath $taskConfigPath
@("listen_addresses = '127.0.0.1'",'port = 5432') | Add-Content -LiteralPath $taskConfigPath
& (Join-Path $taskPg 'bin\pg_ctl.exe') status -D $taskData 2>$null | Out-Null
if ($LASTEXITCODE -ne 0) {
    & (Join-Path $taskPg 'bin\pg_ctl.exe') start -D $taskData -l (Join-Path $taskLocal 'postgres.log') -w -t 60
    if ($LASTEXITCODE -ne 0) { throw 'Native PostgreSQL did not start. See .local/postgres.log.' }
}
Write-Host 'Native PostgreSQL ready on 127.0.0.1:5432 (local test data only).'
