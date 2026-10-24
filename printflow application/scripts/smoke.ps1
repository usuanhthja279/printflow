# PrintFlow Phase 0 API smoke test (PowerShell 5.1+)
param(
    [string]$ApiBase = 'http://localhost:8080',
    [string]$DemoKey = $env:DEMO_ADMIN_KEY
)
$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrWhiteSpace($DemoKey) -and (Test-Path '.env')) {
    $line = Get-Content '.env' | Where-Object { $_ -match '^DEMO_ADMIN_KEY=' } | Select-Object -First 1
    if ($line) { $DemoKey = $line.Substring('DEMO_ADMIN_KEY='.Length).Trim() }
}
if ([string]::IsNullOrWhiteSpace($DemoKey)) {
    throw 'Set DEMO_ADMIN_KEY or run from repository root containing the local .env file.'
}
Write-Host '[1/5] Health...'
$health = Invoke-RestMethod -Uri "$ApiBase/actuator/health" -Method Get
if ($health.status -ne 'UP') { throw "Health unexpected: $($health.status)" }
Write-Host '[2/5] Catalogue...'
$catalog = Invoke-RestMethod -Uri "$ApiBase/api/public/catalog"
if ($catalog.Count -lt 5) { throw 'Missing print products' }
Write-Host '[3/5] Create enquiry...'
$request = @{customerName='PrintFlow Smoke Test';email='smoke@example.com';phone='9876543210';productCode='BROCHURES';quantity=5000;privacyConsent=$true;privacyVersion='2026-10-23';size='A4';paperGsm=170;printSides='DOUBLE';finish='MATTE';notes='Synthetic API smoke record'} | ConvertTo-Json
$created = Invoke-RestMethod -Uri "$ApiBase/api/public/enquiries" -Method Post -ContentType 'application/json' -Body $request
if (-not $created.publicId -or -not $created.trackingToken) { throw 'Missing tracking credentials' }
Write-Host "Created: $($created.publicId)"
Write-Host '[4/5] Demo owner list and advance status...'
$demoHeaders = @{'X-Demo-Key' = $DemoKey}
$enquiries = Invoke-RestMethod -Uri "$ApiBase/api/demo/enquiries" -Headers $demoHeaders
$item = $enquiries | Where-Object { $_.publicId -eq $created.publicId } | Select-Object -First 1
if (-not $item) { throw 'Created enquiry not visible in owner demo' }
$updated = Invoke-RestMethod -Uri "$ApiBase/api/demo/enquiries/$($item.id)/status" -Method Patch -Headers $demoHeaders -ContentType 'application/json' -Body '{"status":"REVIEWING"}'
if ($updated.status -ne 'REVIEWING') { throw 'Status not advanced' }
Write-Host '[5/5] Secure public tracking...'
$trackHeaders = @{'X-Tracking-Token' = $created.trackingToken}
$tracked = Invoke-RestMethod -Uri "$ApiBase/api/public/enquiries/$($created.publicId)" -Headers $trackHeaders
if ($tracked.status -ne 'REVIEWING') { throw 'Customer status not synchronized' }
if (@($tracked.events).Count -lt 2) { throw 'Status event history missing' }
Write-Host 'PASS: health, products, persisted enquiry, owner update, customer tracking.' -ForegroundColor Green
Write-Host 'Additional negative tests: incorrect token -> 404; incorrect owner key -> 401.'
