param([ValidateSet('desktop','mobile')][string]$Project='desktop',[switch]$KeepFixtures)
$ErrorActionPreference='Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
. ./scripts/Use-PrintFlowTools.ps1
./scripts/Initialize-LocalDatabase.ps1
./scripts/Initialize-LocalStaff.ps1
$taskStatus=Invoke-RestMethod 'http://127.0.0.1:3000/api/public/identity/status'
if($taskStatus.deliveryEnabled){throw 'Start native services with -DisableMail for synthetic PDF browser checks.'}
$env:APP_MAIL_ENABLED='false';$env:APP_OWNER_NOTIFICATION_EMAIL='owner@example.test'
$env:DB_URL='jdbc:postgresql://127.0.0.1:5434/'+$env:POSTGRES_DB;$env:DB_USERNAME=$env:POSTGRES_USER;$env:DB_PASSWORD=$env:POSTGRES_PASSWORD
$taskCredentialPath=Join-Path (Get-Location) '.local/test-designer-credentials.json'
if(-not(Test-Path $taskCredentialPath)){[IO.File]::WriteAllText($taskCredentialPath,'')}
$taskAcl=Get-Acl -LiteralPath $taskCredentialPath;if(-not $taskAcl.AreAccessRulesProtected){$taskAcl.SetAccessRuleProtection($true,$false);$taskAcl.SetAccessRule((New-Object Security.AccessControl.FileSystemAccessRule([Security.Principal.WindowsIdentity]::GetCurrent().Name,'FullControl','Allow')));Set-Acl -LiteralPath $taskCredentialPath -AclObject $taskAcl}
./backend/gradlew.bat -p backend browserFixtures
if($LASTEXITCODE -ne 0){throw 'Fixture preparation failed'}
$env:PHASE1_BROWSER_FIXTURES='true';$env:CREATE_LOCAL_TEST_DESIGNER='true'
try{./scripts/Test-Browser.ps1 -PlaywrightArguments @('pdf-review.spec.ts',('--project='+$Project))}finally{Remove-Item Env:PHASE1_BROWSER_FIXTURES,Env:CREATE_LOCAL_TEST_DESIGNER -ErrorAction SilentlyContinue;if(-not $KeepFixtures){./backend/gradlew.bat -p backend browserFixtures -PfixtureMode=cleanup;if($LASTEXITCODE -ne 0){throw 'Fixture cleanup failed'}}}
