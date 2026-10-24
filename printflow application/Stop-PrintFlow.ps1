# Stops only native processes belonging to this project; preserves local data.
param([switch]$KeepDatabase)
$ErrorActionPreference='Stop'
Set-Location $PSScriptRoot
$taskLocal=Join-Path $PSScriptRoot '.local'
foreach ($taskName in @('web','api','prod-check')) {
    $taskPidFile=Join-Path $taskLocal ($taskName+'.pid')
    if (Test-Path $taskPidFile) {
        $taskSavedPid=[int](Get-Content $taskPidFile)
        $taskProcess=Get-CimInstance Win32_Process -Filter "ProcessId=$taskSavedPid"
        if ($taskProcess) {
            if ($taskProcess.CommandLine -notlike ('*'+$PSScriptRoot+'*')) { throw "PID reused by another program: $taskName. Refusing to stop it." }
            Stop-Process -Id $taskSavedPid
        }
        Remove-Item -LiteralPath $taskPidFile
    }
}
if (-not $KeepDatabase) {
    $taskPg=Join-Path $PSScriptRoot '.tools\postgresql-17.11\bin\pg_ctl.exe'
    $taskData=Join-Path $taskLocal 'postgres-data'
    if (Test-Path (Join-Path $taskData 'postmaster.pid')) {
        & $taskPg stop -D $taskData -m fast -w -t 30
        if ($LASTEXITCODE -ne 0) { throw 'Native PostgreSQL shutdown failed.' }
    }
}
Write-Host 'Native PrintFlow processes stopped. Database files preserved.'
