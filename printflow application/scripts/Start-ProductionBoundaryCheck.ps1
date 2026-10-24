# Temporary native prod-profile instance for foundation checks; no public exposure.
$ErrorActionPreference='Stop'
$taskRoot=Split-Path $PSScriptRoot -Parent
Set-Location $taskRoot
. ./scripts/Use-PrintFlowTools.ps1
foreach ($taskLine in Get-Content '.env') {
    if ($taskLine -match '^([A-Z][A-Z0-9_]*)=(.*)$') { [Environment]::SetEnvironmentVariable($Matches[1],$Matches[2],'Process') }
}
$env:DB_URL='jdbc:postgresql://127.0.0.1:5434/'+$env:POSTGRES_DB
$env:DB_USERNAME=$env:POSTGRES_USER
$env:DB_PASSWORD=$env:POSTGRES_PASSWORD
$env:SPRING_PROFILES_ACTIVE='prod'
$env:APP_SECURE_COOKIES='true'
$env:APP_DEMO_ENABLED='true'
$env:APP_DEMO_KEY=$env:DEMO_ADMIN_KEY
Remove-Item Env:APP_PUBLIC_ENQUIRIES_ENABLED -ErrorAction SilentlyContinue
$env:PORT='8082'
$env:SERVER_ADDRESS='127.0.0.1'
$taskJar=Join-Path $taskRoot 'backend\build\libs\printflow-api.jar'
$taskCheck=Start-Process -FilePath (Join-Path $env:JAVA_HOME 'bin\java.exe') -ArgumentList @('-Xms32m','-Xmx256m','-jar',('"'+$taskJar+'"')) -WindowStyle Hidden -WorkingDirectory $taskRoot -PassThru -RedirectStandardOutput (Join-Path $taskRoot '.local\prod-check.log') -RedirectStandardError (Join-Path $taskRoot '.local\prod-check.error.log')
$taskCheck.Id | Set-Content '.local/prod-check.pid'
Write-Host 'Temporary native prod boundary check started at 127.0.0.1:8082.'
