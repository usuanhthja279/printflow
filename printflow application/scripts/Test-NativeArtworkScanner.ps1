#requires -Version 7.0
# Native diagnostic only: valid synthetic artwork, never a substitute for per-upload scanning.
$ErrorActionPreference='Stop'
$taskScannerRoot=Split-Path $PSScriptRoot -Parent
$taskScannerMode='DEFENDER'
$taskScannerExe='C:/Program Files/Windows Defender/MpCmdRun.exe'
foreach($taskScannerLine in Get-Content (Join-Path $taskScannerRoot '.env')) {
    if($taskScannerLine -match '^APP_ARTIFACT_SCANNER=(.+)$'){$taskScannerMode=$Matches[1].Trim()}
    if($taskScannerLine -match '^APP_ARTIFACT_SCANNER_PATH=(.+)$'){$taskScannerExe=$Matches[1].Trim()}
}
if($taskScannerMode -ne 'DEFENDER'){throw 'This Windows diagnostic requires the Defender adapter. Use the configured scanner vendor diagnostic for another engine.'}
if(-not (Test-Path -LiteralPath $taskScannerExe -PathType Leaf)){throw 'Native Defender executable is unavailable.'}
foreach($taskArtworkName in @('proof-v1.png','reference.pdf')) {
    $taskArtworkPath=Join-Path $taskScannerRoot ('frontend/e2e/fixtures/'+$taskArtworkName)
    $taskProcessInfo=[System.Diagnostics.ProcessStartInfo]::new()
    $taskProcessInfo.FileName=$taskScannerExe
    $taskProcessInfo.UseShellExecute=$false
    $taskProcessInfo.CreateNoWindow=$true
    $taskProcessInfo.WorkingDirectory=Split-Path $taskScannerExe -Parent
    foreach($taskArgument in @('-Scan','-ScanType','3','-File',$taskArtworkPath,'-DisableRemediation')){$taskProcessInfo.ArgumentList.Add($taskArgument)}
    $taskProcessInfo.RedirectStandardOutput=$true
    $taskProcessInfo.RedirectStandardError=$true
    $taskScanProcess=[System.Diagnostics.Process]::new()
    $taskScanProcess.StartInfo=$taskProcessInfo
    try {
        [void]$taskScanProcess.Start()
        $taskScanOutput=$taskScanProcess.StandardOutput.ReadToEndAsync()
        $taskScanError=$taskScanProcess.StandardError.ReadToEndAsync()
        if(-not $taskScanProcess.WaitForExit(45000)){$taskScanProcess.Kill($true);throw 'Native scan timed out; keep uploads quarantined.'}
        if($taskScanProcess.ExitCode -ne 0){throw ('Native scan did not clear valid fixture '+$taskArtworkName+' (exit '+$taskScanProcess.ExitCode+'). Keep uploads quarantined.')}
        Write-Host ('PASS: native Defender cleared valid synthetic '+$taskArtworkName)
    } finally {$taskScanProcess.Dispose()}
}
Write-Host 'Diagnostic passed. The application still scans each private upload independently.'
