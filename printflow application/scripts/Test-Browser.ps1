param([string[]]$PlaywrightArguments=@())
$ErrorActionPreference='Stop'
$taskBrowserRoot=Split-Path $PSScriptRoot -Parent
foreach($taskBrowserLine in Get-Content (Join-Path $taskBrowserRoot '.env')) {
    if($taskBrowserLine -match '^(APP_STAFF_BOOTSTRAP_USERNAME|APP_STAFF_BOOTSTRAP_PASSWORD|APP_OWNER_BOOTSTRAP_USERNAME|APP_OWNER_BOOTSTRAP_PASSWORD)=(.*)$') {
        [Environment]::SetEnvironmentVariable($Matches[1],$Matches[2],'Process')
    }
}
$taskBrowserReady=$false
for($taskBrowserAttempt=0;$taskBrowserAttempt -lt 30;$taskBrowserAttempt++) {
    try {
        if((Invoke-WebRequest 'http://127.0.0.1:3000/business' -TimeoutSec 2).StatusCode -eq 200 -and (Invoke-RestMethod 'http://127.0.0.1:8080/actuator/health' -TimeoutSec 2).status -eq 'UP') {$taskBrowserReady=$true;break}
    } catch {}
    Start-Sleep -Seconds 1
}
if(-not $taskBrowserReady){throw 'Native web/API are not ready. Start the stack before browser verification.'}
Push-Location (Join-Path $taskBrowserRoot 'frontend')
try { npm.cmd run test:e2e -- @PlaywrightArguments; if($LASTEXITCODE -ne 0){throw 'Browser verification failed; inspect frontend/test-results.'} }
finally { Pop-Location }
