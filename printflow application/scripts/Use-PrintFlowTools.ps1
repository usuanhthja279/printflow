# Dot-source this file: . ./scripts/Use-PrintFlowTools.ps1
$ErrorActionPreference = 'Stop'
$taskJavaCandidates = @()
if ($env:JAVA_HOME) { $taskJavaCandidates += $env:JAVA_HOME }
$taskJavaCandidates += @(Get-ChildItem (Join-Path $env:USERPROFILE '.jdks') -Directory -ErrorAction SilentlyContinue | Sort-Object Name -Descending | Select-Object -ExpandProperty FullName)
foreach ($taskJdkSearchRoot in @('C:\Program Files\Eclipse Adoptium','C:\Program Files\Java','C:\Program Files\Microsoft')) {
    $taskJavaCandidates += @(Get-ChildItem $taskJdkSearchRoot -Directory -ErrorAction SilentlyContinue | Select-Object -ExpandProperty FullName)
}
$taskJavaFound = $false
foreach ($taskCandidate in $taskJavaCandidates) {
    $taskJava = Join-Path $taskCandidate 'bin\java.exe'
    $taskRelease = Join-Path $taskCandidate 'release'
    if ((Test-Path $taskJava) -and (Test-Path $taskRelease) -and
        (Select-String -LiteralPath $taskRelease -Pattern '^JAVA_VERSION="21[.\"]' -Quiet)) {
        $env:JAVA_HOME = $taskCandidate
        $env:PATH = (Join-Path $taskCandidate 'bin') + ';' + $env:PATH
        $taskJavaFound = $true
        break
    }
}
if (-not $taskJavaFound) { throw 'Java 21 JDK not found. Install a JDK 21 or set JAVA_HOME to it.' }
Write-Host "Java 21 ready: $env:JAVA_HOME"
Write-Host 'Backend build: cd backend; ./gradlew.bat build'
