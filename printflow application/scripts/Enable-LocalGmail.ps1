# Run interactively in a private PowerShell window. Never paste an app password into chat.
param([string]$Sender,[string]$TestRecipient)
$ErrorActionPreference='Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
function Read-SystemMailValue([string]$Name){foreach($taskScope in @('Process','User','Machine')){$taskValue=[Environment]::GetEnvironmentVariable($Name,$taskScope);if(-not [string]::IsNullOrWhiteSpace($taskValue)){return $taskValue}};return ''}
$taskSystemSender=Read-SystemMailValue 'SPRING_MAIL_USERNAME'
$taskSystemPassword=Read-SystemMailValue 'SPRING_MAIL_PASSWORD'
if(-not $taskSystemPassword){$taskSystemPassword=Read-SystemMailValue 'SPRING_MAIL_PASSWROD'}
$taskUseSystemCredentials=!!$taskSystemSender -and !!$taskSystemPassword
if(-not $Sender){$Sender=if($taskSystemSender){$taskSystemSender}else{Read-Host 'Gmail / Google Workspace sender address'}}
if($taskUseSystemCredentials -and $Sender -ne $taskSystemSender){throw 'The chosen sender must match SPRING_MAIL_USERNAME for system credentials.'}

if(-not $TestRecipient){$TestRecipient=Read-Host 'First test recipient address (use your own inbox)'}
foreach($taskAddress in @($Sender,$TestRecipient)){
 $taskParsed=[System.Net.Mail.MailAddress]::new($taskAddress)
 if($taskParsed.Address -ne $taskAddress -or $taskAddress -match '[\r\n]' -or $taskAddress -match '\.(test|invalid)$'){throw 'Use a real email address, without a display name.'}
}
if($taskUseSystemCredentials){$taskPassword=$taskSystemPassword.Replace(' ','');[Environment]::SetEnvironmentVariable('SPRING_MAIL_USERNAME',$taskSystemSender,'Process');[Environment]::SetEnvironmentVariable('SPRING_MAIL_PASSWORD',$taskPassword,'Process')}else{
 $taskPrivatePassword=Read-Host 'Google app password (hidden; not your normal Google password)' -AsSecureString
 $taskCredential=[PSCredential]::new($Sender,$taskPrivatePassword)
 $taskPassword=$taskCredential.GetNetworkCredential().Password.Replace(' ','')
}
if($taskPassword -notmatch '^[A-Za-z0-9]{16}$'){throw 'The configured credential must be a 16-character Google app password.'}
$taskSmtp=[System.Net.Mail.SmtpClient]::new('smtp.gmail.com',587)
$taskSmtp.EnableSsl=$true
$taskSmtp.Timeout=15000
$taskSmtp.UseDefaultCredentials=$false
$taskSmtp.Credentials=[System.Net.NetworkCredential]::new($Sender,$taskPassword)
$taskMessage=[System.Net.Mail.MailMessage]::new($Sender,$TestRecipient)
$taskMessage.Subject='J.D.Printex — mail delivery setup test'
$taskMessage.Body='This is the requested J.D.Printex setup test. Local development sends only to the chosen test inbox. Please confirm receipt, including checking Spam. No older queued messages are being sent.'
$taskMessage.SubjectEncoding=[Text.Encoding]::UTF8
$taskMessage.BodyEncoding=[Text.Encoding]::UTF8
try{$taskSmtp.Send($taskMessage)}catch{throw 'Google did not accept the test message. Configuration was not changed. Check the app password, Google 2-Step Verification and Workspace policy.'}finally{$taskMessage.Dispose();$taskSmtp.Dispose()}
$taskEnv=Join-Path (Get-Location) '.env'
if(-not(Test-Path -LiteralPath $taskEnv)){throw 'Native .env is missing; initialize local development first.'}
$taskValues=[ordered]@{APP_MAIL_ENABLED='true';APP_MAIL_HOST='smtp.gmail.com';APP_MAIL_PORT='587';APP_MAIL_USERNAME=$(if($taskUseSystemCredentials){''}else{$Sender});APP_MAIL_PASSWORD=$(if($taskUseSystemCredentials){''}else{$taskPassword});APP_MAIL_ALLOWED_RECIPIENTS=$TestRecipient.ToLowerInvariant();APP_MAIL_SEND_AFTER=[DateTime]::UtcNow.ToString('o');APP_PUBLIC_BASE_URL='http://127.0.0.1:3000'}
$taskText=[IO.File]::ReadAllText($taskEnv)
foreach($taskKey in $taskValues.Keys){
 $taskPattern='(?m)^'+[regex]::Escape($taskKey)+'=.*$'
 $taskReplacement=$taskKey+'='+$taskValues[$taskKey]
 if([regex]::IsMatch($taskText,$taskPattern)){$taskText=[regex]::Replace($taskText,$taskPattern,[System.Text.RegularExpressions.MatchEvaluator]{param($taskMatch) $taskReplacement})}else{$taskText+="`n"+$taskReplacement}
}
[IO.File]::WriteAllText($taskEnv,$taskText,[Text.UTF8Encoding]::new($false))
$taskSystemPassword=$null;$taskPassword=$null;$taskCredential=$null;$taskPrivatePassword=$null;$taskText=$null;$taskValues=$null
Write-Host 'Google accepted the setup test. Confirm it arrived in the chosen inbox.'
Write-Host 'Private native configuration saved. Older queued messages are held; only the selected inbox is allowed.'
./Stop-PrintFlow.ps1 -KeepDatabase
./Start-PrintFlow.ps1 -SkipBuild
Write-Host 'Create a new enquiry using the selected real test email to verify delivery through the app.'
