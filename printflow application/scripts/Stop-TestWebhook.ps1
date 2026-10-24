$ErrorActionPreference='Stop'
$taskRoot=Split-Path $PSScriptRoot -Parent
foreach($taskEntry in @(@{Pid='webhook-relay.pid';Match='webhook-relay.mjs'},@{Pid='webhook-tunnel.pid';Match='cloudflared'})){
    $taskPidPath=Join-Path $taskRoot ('.local/'+$taskEntry.Pid)
    if(Test-Path $taskPidPath){
        $taskSavedPid=[int](Get-Content -LiteralPath $taskPidPath)
        $taskProcess=Get-CimInstance Win32_Process -Filter "ProcessId=$taskSavedPid" -ErrorAction SilentlyContinue
        if($taskProcess -and $taskProcess.CommandLine -like ('*'+$taskEntry.Match+'*')){Stop-Process -Id $taskSavedPid}
        Remove-Item -LiteralPath $taskPidPath
    }
}
'Temporary test webhook relay/tunnel stopped. Remove or disable its URL in Razorpay Test Mode when finished.'
