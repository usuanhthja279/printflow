param([ValidateSet("desktop","mobile")][string]$Project="desktop",[ValidateSet("workflow","vocabulary","staff-stages","quote-review")][string]$Check="workflow")
$ErrorActionPreference='Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
. ./scripts/Use-PrintFlowTools.ps1
./scripts/Initialize-LocalDatabase.ps1
./scripts/Initialize-LocalStaff.ps1
$env:DB_URL='jdbc:postgresql://127.0.0.1:5434/'+$env:POSTGRES_DB
$env:DB_USERNAME=$env:POSTGRES_USER
$env:DB_PASSWORD=$env:POSTGRES_PASSWORD
if($env:APP_MAIL_ENABLED -eq 'true'){throw 'Real mail must be disabled for synthetic browser tests.'}
./backend/gradlew.bat -p backend browserFixtures
if($LASTEXITCODE -ne 0){throw 'Synthetic browser fixture preparation failed.'}
$env:PHASE1_BROWSER_FIXTURES='true'
try { $taskPhaseTwoBrowserFile=if($Check -eq 'quote-review'){'quotation-review.spec.ts'}elseif($Check -eq 'staff-stages'){'staff-stage-access.spec.ts'}elseif($Check -eq 'vocabulary'){'phase-two-vocabulary.spec.ts'}else{'phase-two-commercial-design.spec.ts'}; ./scripts/Test-Browser.ps1 -PlaywrightArguments @($taskPhaseTwoBrowserFile,('--project='+$Project)) }
finally {
    Remove-Item Env:PHASE1_BROWSER_FIXTURES -ErrorAction SilentlyContinue
    ./backend/gradlew.bat -p backend browserFixtures -PfixtureMode=cleanup
    if($LASTEXITCODE -ne 0){throw 'Fixture cleanup failed. Disable browser-phase1 accounts before further use.'}
}
