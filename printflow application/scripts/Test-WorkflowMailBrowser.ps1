param([ValidateSet("workflow","signup","review","razorpay","operations")][string]$Check="workflow",[ValidateSet("desktop","mobile","both")][string]$Project="desktop")
$ErrorActionPreference='Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
. ./scripts/Use-PrintFlowTools.ps1
./scripts/Initialize-LocalDatabase.ps1
./scripts/Initialize-LocalStaff.ps1
$taskIdentityStatus=$null;for($taskTry=0;$taskTry -lt 30;$taskTry++){try{$taskIdentityStatus=Invoke-RestMethod 'http://127.0.0.1:3000/api/public/identity/status' -TimeoutSec 2;break}catch{Start-Sleep -Seconds 1}};if($null -eq $taskIdentityStatus){throw 'Native API/web did not become ready'}
if($taskIdentityStatus.deliveryEnabled){throw 'Start the native stack with -DisableMail for synthetic workflow checks. No real mail may be sent by this harness.'}
$env:APP_MAIL_ENABLED='false';$env:APP_OWNER_NOTIFICATION_EMAIL='owner@example.test'
$env:DB_URL='jdbc:postgresql://127.0.0.1:5434/'+$env:POSTGRES_DB;$env:DB_USERNAME=$env:POSTGRES_USER;$env:DB_PASSWORD=$env:POSTGRES_PASSWORD
function Invoke-WorkflowFixture([string]$Mode='prepare'){
    $taskClasspathFile=Join-Path $PWD '.local/integration-classpath.txt'
    $taskClassFile=Join-Path $PWD 'backend/build/classes/java/integrationTest/com/printflow/BrowserFixtures.class'
    if((Test-Path $taskClasspathFile) -and (Test-Path $taskClassFile) -and (Get-Item $taskClasspathFile).LastWriteTime -gt (Get-Item 'backend/build.gradle').LastWriteTime){
        # BrowserFixtures resolves ../.local relative to the backend, as Gradle's JavaExec does.
        Push-Location (Join-Path $PSScriptRoot '../backend')
        try { & java -Xmx192m -cp (Get-Content $taskClasspathFile -Raw) com.printflow.BrowserFixtures $Mode }
        finally { Pop-Location }
    }else{./backend/gradlew.bat -p backend browserFixtures ("-PfixtureMode="+$Mode)}
    if($LASTEXITCODE -ne 0){throw 'Native browser fixture operation failed'}
}
Invoke-WorkflowFixture
$env:PHASE1_BROWSER_FIXTURES='true'
$taskWorkflowSpec=if($Check -eq 'signup'){'customer-signup.spec.ts'}elseif($Check -eq 'razorpay'){'razorpay-flow.spec.ts'}elseif($Check -eq 'operations'){'operations-flow.spec.ts'}elseif($Check -eq 'review'){'review-workspace.spec.ts'}else{'workflow-mail.spec.ts'}
$taskWorkflowArgs=@($taskWorkflowSpec)
if($Project -eq 'both'){$taskWorkflowArgs+=@('--project=desktop','--project=mobile')}else{$taskWorkflowArgs+=('--project='+$Project)}
try{./scripts/Test-Browser.ps1 -PlaywrightArguments $taskWorkflowArgs}finally{Remove-Item Env:PHASE1_BROWSER_FIXTURES -ErrorAction SilentlyContinue;Invoke-WorkflowFixture 'cleanup'}
