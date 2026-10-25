# Native Windows development: no Docker required.
param([switch]$SkipBuild,[switch]$Dev,[switch]$EnableDemo,[switch]$DisableMail)
$ErrorActionPreference='Stop'
Set-Location $PSScriptRoot
. ./scripts/Use-PrintFlowTools.ps1
./scripts/Initialize-LocalDatabase.ps1
./scripts/Initialize-LocalStaff.ps1
if($DisableMail){$env:APP_MAIL_ENABLED='false';$env:APP_OWNER_NOTIFICATION_EMAIL='owner@example.test'}
# Existing desktop sessions may predate Windows user-environment credential updates.
foreach($taskGatewayVariable in @('RAZORPAY_KEY_ID','RAZORPAY_KEY_SECRET','RAZORPAY_WEBHOOK_SECRET')) {
    if(-not [Environment]::GetEnvironmentVariable($taskGatewayVariable,'Process')) {
        $taskGatewayValue=[Environment]::GetEnvironmentVariable($taskGatewayVariable,'User')
        if($taskGatewayValue){[Environment]::SetEnvironmentVariable($taskGatewayVariable,$taskGatewayValue,'Process')}
    }
}
Remove-Variable taskGatewayValue -ErrorAction SilentlyContinue
$taskLocal=Join-Path $PSScriptRoot '.local'
$taskApiJar=Join-Path $PSScriptRoot 'backend\build\libs\printflow-api.jar'
$env:API_INTERNAL_URL='http://127.0.0.1:8080'
if (-not $SkipBuild -or -not (Test-Path $taskApiJar)) {
    & ./backend/gradlew.bat -p backend --no-daemon build
    if ($LASTEXITCODE -ne 0) { throw 'Gradle backend build failed' }
}
if (-not (Test-Path 'frontend/node_modules/next')) {
    Push-Location frontend
    try { npm.cmd ci --no-audit --no-fund; if ($LASTEXITCODE -ne 0) { throw 'Frontend install failed' } } finally { Pop-Location }
}
if (-not $Dev -and (-not $SkipBuild -or -not (Test-Path 'frontend/.next/standalone/server.js'))) {
    Push-Location frontend
    try { npm.cmd run build; if ($LASTEXITCODE -ne 0) { throw 'Frontend build failed' } } finally { Pop-Location }
}
$env:SPRING_PROFILES_ACTIVE='dev'
$env:DB_URL='jdbc:postgresql://127.0.0.1:5432/'+$env:POSTGRES_DB
$env:DB_USERNAME=$env:POSTGRES_USER
$env:DB_PASSWORD=$env:POSTGRES_PASSWORD
$env:APP_DEMO_ENABLED=if($EnableDemo){'true'}else{'false'}
$env:APP_DEMO_KEY=$env:DEMO_ADMIN_KEY
$env:APP_PUBLIC_ENQUIRIES_ENABLED=$env:PUBLIC_ENQUIRIES_ENABLED
$env:SERVER_ADDRESS='127.0.0.1'
$env:PORT='8080'
$taskApiPidFile=Join-Path $taskLocal 'api.pid'
$taskOccupied=Get-NetTCPConnection -State Listen -LocalPort 8080,3000 -ErrorAction SilentlyContinue
if($taskOccupied){throw 'Port 8080 or 3000 is already in use. Stop the existing service before starting PrintFlow.'}
$taskWebPidFile=Join-Path $taskLocal 'web.pid'
foreach ($taskPidFile in @($taskApiPidFile,$taskWebPidFile)) {
    if (Test-Path $taskPidFile) {
        $taskSavedPid=Get-Content $taskPidFile
        if (Get-Process -Id $taskSavedPid -ErrorAction SilentlyContinue) { throw 'PrintFlow process already running. Run ./Stop-PrintFlow.ps1 before restarting.' }
    }
}
$taskApi=Start-Process -FilePath (Join-Path $env:JAVA_HOME 'bin\java.exe') -ArgumentList @('-Xms64m','-Xmx384m','-XX:MaxMetaspaceSize=192m','-jar',('"'+$taskApiJar+'"')) -WorkingDirectory $PSScriptRoot -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $taskLocal 'api.log') -RedirectStandardError (Join-Path $taskLocal 'api.error.log')
$taskApi.Id | Set-Content $taskApiPidFile
$taskHealthy=$false
for ($taskAttempt=0; $taskAttempt -lt 60; $taskAttempt++) {
    try { $taskHealth=Invoke-RestMethod 'http://127.0.0.1:8080/actuator/health' -TimeoutSec 2; if ($taskHealth.status -eq 'UP') { $taskHealthy=$true; break } } catch {}
    if ($taskApi.HasExited) { throw 'API exited; inspect .local/api.log and .local/api.error.log.' }
    Start-Sleep -Seconds 1
}
if (-not $taskHealthy) { throw 'API health timeout; inspect .local/api.log.' }
$env:PORT='3000'
$env:HOSTNAME='127.0.0.1'
if ($Dev) {
    $taskNodeScript=Join-Path $PSScriptRoot 'frontend\node_modules\next\dist\bin\next'
    $taskWebArgs=@(('"'+$taskNodeScript+'"'),'dev','--hostname','127.0.0.1','--port','3000')
    $taskWebDir=Join-Path $PSScriptRoot 'frontend'
} else {
    Copy-Item -LiteralPath 'frontend/.next/static' -Destination 'frontend/.next/standalone/.next' -Recurse -Force
    Copy-Item -LiteralPath 'frontend/public' -Destination 'frontend/.next/standalone' -Recurse -Force
    $taskNodeScript=Join-Path $PSScriptRoot 'frontend\.next\standalone\server.js'
    $taskWebArgs=@(('"'+$taskNodeScript+'"'))
    $taskWebDir=Join-Path $PSScriptRoot 'frontend\.next\standalone'
}
$taskWeb=Start-Process -FilePath (Get-Command node.exe).Source -ArgumentList $taskWebArgs -WorkingDirectory $taskWebDir -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $taskLocal 'web.log') -RedirectStandardError (Join-Path $taskLocal 'web.error.log')
$taskWeb.Id | Set-Content $taskWebPidFile
$taskWebReady=$false
for($taskAttempt=0;$taskAttempt -lt 30;$taskAttempt++) {
    try{if((Invoke-WebRequest 'http://127.0.0.1:3000/' -TimeoutSec 2).StatusCode -eq 200){$taskWebReady=$true;break}}catch{}
    if($taskWeb.HasExited){throw 'Frontend exited; inspect .local/web.error.log.'}
    Start-Sleep -Seconds 1
}
if(-not $taskWebReady){throw 'Frontend readiness timeout; inspect .local/web.error.log.'}
Write-Host 'J.D.Printex: http://127.0.0.1:3000 | API: http://127.0.0.1:8080'
Write-Host 'Native PostgreSQL data is preserved in .local/postgres-data. Use only synthetic data.'
Write-Host 'Stop: ./Stop-PrintFlow.ps1 | Hot reload frontend: ./Start-PrintFlow.ps1 -Dev -SkipBuild'
