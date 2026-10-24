$ErrorActionPreference='Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
. ./scripts/Use-PrintFlowTools.ps1
./scripts/Initialize-LocalDatabase.ps1
$env:DB_URL='jdbc:postgresql://127.0.0.1:5434/'+$env:POSTGRES_DB
$env:DB_USERNAME=$env:POSTGRES_USER
$env:DB_PASSWORD=$env:POSTGRES_PASSWORD
Remove-Item Env:APP_SECURE_COOKIES,Env:APP_PUBLIC_ENQUIRIES_ENABLED,Env:SPRING_PROFILES_ACTIVE -ErrorAction SilentlyContinue
$env:APP_RATE_NAMESPACE='integration-'+[guid]::NewGuid().ToString('N')
$env:APP_OWNER_BOOTSTRAP_ENABLED='false'
$env:APP_STAFF_BOOTSTRAP_ENABLED='false'
./backend/gradlew.bat -p backend build integrationTest
if($LASTEXITCODE -ne 0){throw 'Backend verification failed; inspect backend/build/reports/tests.'}
