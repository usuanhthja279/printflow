# Development-only SALES account. Credentials stay in ignored .env; never printed.
$ErrorActionPreference='Stop'
$taskStaffEnv=Join-Path (Split-Path $PSScriptRoot -Parent) '.env'
if (-not (Test-Path $taskStaffEnv)) { throw 'Initialize the local database first.' }
$taskStaffLines=Get-Content $taskStaffEnv
if (-not ($taskStaffLines -match '^APP_STAFF_BOOTSTRAP_PASSWORD=')) {
    $taskStaffBytes=New-Object byte[] 32
    $taskStaffRng=[Security.Cryptography.RandomNumberGenerator]::Create()
    try{$taskStaffRng.GetBytes($taskStaffBytes)}finally{$taskStaffRng.Dispose()}
    $taskStaffPassword=[Convert]::ToBase64String($taskStaffBytes).TrimEnd('=').Replace('+','-').Replace('/','_')
    @('APP_STAFF_BOOTSTRAP_ENABLED=true','APP_STAFF_BOOTSTRAP_USERNAME=staff@jdprintex.local',('APP_STAFF_BOOTSTRAP_PASSWORD='+$taskStaffPassword),'APP_SECURE_COOKIES=false') | Add-Content $taskStaffEnv
    Write-Host 'Generated local SALES credentials in ignored .env. Existing accounts are never reset at startup.'
}
# Separate owner identity and stable encryption key. Never overwrite either on restart.
$taskOwnerLines=Get-Content $taskStaffEnv
if (-not ($taskOwnerLines -match '^APP_OWNER_BOOTSTRAP_PASSWORD=')) {
    $taskOwnerBytes=New-Object byte[] 32
    $taskOwnerRng=[Security.Cryptography.RandomNumberGenerator]::Create()
    try{$taskOwnerRng.GetBytes($taskOwnerBytes)}finally{$taskOwnerRng.Dispose()}
    $taskOwnerPassword=[Convert]::ToBase64String($taskOwnerBytes).TrimEnd('=').Replace('+','-').Replace('/','_')
    @('APP_OWNER_BOOTSTRAP_ENABLED=true','APP_OWNER_BOOTSTRAP_USERNAME=owner@jdprintex.local',('APP_OWNER_BOOTSTRAP_PASSWORD='+$taskOwnerPassword)) | Add-Content $taskStaffEnv
    Write-Host 'Generated separate local owner credentials in ignored .env. Enroll MFA in the browser.'
}
if (-not ($taskOwnerLines -match '^APP_MFA_ENCRYPTION_KEY=')) {
    $taskMfaBytes=New-Object byte[] 32
    $taskMfaRng=[Security.Cryptography.RandomNumberGenerator]::Create()
    try{$taskMfaRng.GetBytes($taskMfaBytes)}finally{$taskMfaRng.Dispose()}
    ('APP_MFA_ENCRYPTION_KEY='+[Convert]::ToBase64String($taskMfaBytes)) | Add-Content $taskStaffEnv
}
$taskCustomerLines=Get-Content $taskStaffEnv
if (-not ($taskCustomerLines -match '^APP_MAIL_ENCRYPTION_KEY=')) {
    $taskMailBytes=New-Object byte[] 32
    $taskMailRng=[Security.Cryptography.RandomNumberGenerator]::Create()
    try{$taskMailRng.GetBytes($taskMailBytes)}finally{$taskMailRng.Dispose()}
    @('APP_MAIL_ENABLED=false','APP_CUSTOMER_ACCOUNTS_ENABLED=true',('APP_MAIL_ENCRYPTION_KEY='+[Convert]::ToBase64String($taskMailBytes))) | Add-Content $taskStaffEnv
    Write-Host 'Native customer account flows enabled with encrypted local outbox. Real email delivery remains disabled.'
}
foreach($taskStaffLine in Get-Content $taskStaffEnv) {
    if($taskStaffLine -match '^([A-Z][A-Z0-9_]*)=(.*)$'){[Environment]::SetEnvironmentVariable($Matches[1],$Matches[2],'Process')}
}
