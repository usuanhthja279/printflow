param([switch]$SkipBuild,[string[]]$TestClass=@('com.printflow.QuotationIntegrationTest','com.printflow.DesignProofIntegrationTest','com.printflow.LiveWorkIntegrationTest'))
$ErrorActionPreference='Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
. ./scripts/Use-PrintFlowTools.ps1
./scripts/Initialize-LocalDatabase.ps1
$env:DB_URL='jdbc:postgresql://127.0.0.1:5434/'+$env:POSTGRES_DB
$env:DB_USERNAME=$env:POSTGRES_USER
$env:DB_PASSWORD=$env:POSTGRES_PASSWORD
Remove-Item Env:APP_SECURE_COOKIES,Env:APP_PUBLIC_ENQUIRIES_ENABLED,Env:SPRING_PROFILES_ACTIVE -ErrorAction SilentlyContinue
$env:APP_RATE_NAMESPACE='phase-two-'+[guid]::NewGuid().ToString('N')
$env:APP_OWNER_NOTIFICATION_EMAIL='owner@example.test'
$env:APP_OWNER_BOOTSTRAP_ENABLED='false'
$env:APP_STAFF_BOOTSTRAP_ENABLED='false'
$taskPhaseTwoArguments=@('integrationTest')
foreach($taskTestClass in $TestClass){$taskPhaseTwoArguments+=@('--tests',$taskTestClass)}
if(-not $SkipBuild){$taskPhaseTwoArguments=@('build')+$taskPhaseTwoArguments}
./backend/gradlew.bat -p backend @taskPhaseTwoArguments
if($LASTEXITCODE -ne 0){throw 'Phase 2 checks failed; inspect backend/build/reports/tests.'}
