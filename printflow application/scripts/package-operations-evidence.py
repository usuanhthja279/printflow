"""Export the completed synthetic operations journey and view-significance notes only."""
from pathlib import Path
import hashlib, json, shutil, zipfile
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parent.parent
sources = [p for p in (root / 'frontend/test-results').glob('operations-flow-*-desktop') if (p / 'result.json').exists()]
assert len(sources) == 1, 'A completed operations browser run is required'
source = sources[0]
result = json.loads((source / 'result.json').read_text(encoding='utf-8'))
assert result.get('completed') is True
views = json.loads((source / 'views.json').read_text(encoding='utf-8'))
assert len(views) == result['views']
suite = ET.parse(root / 'backend/build/test-results/integrationTest/TEST-com.printflow.WorkflowMailIntegrationTest.xml').getroot()
cases = [c for c in suite.findall('testcase') if c.get('name', '').startswith('routingProductionQueueAndDeliveryPhotosRespectScopesAndGates')]
assert len(cases) == 1 and cases[0].find('failure') is None and cases[0].find('error') is None
out = root / 'output/operations-review-2026-10-24'
bundle = out / 'J-D-Printex-Operations-Views'
images = bundle / 'screenshots'
images.mkdir(parents=True, exist_ok=True)
files = []

def redesign(title):
    title = title.lower()
    if 'phone' in title:
        return 'Review thumb reach, visible actions and horizontal scrolling. This is a narrow viewport capture from the same session, not a separate device suite.'
    if any(word in title for word in ['photo', 'pickup', 'transit', 'delivery evidence']):
        return 'Review thumbnail size, milestone grouping, captions and the prominence of the next delivery action. Images are clearly labelled fictional fixtures.'
    if any(word in title for word in ['production', 'printing', 'finishing', 'quality', 'dispatch', 'printer']):
        return 'Review lane density, stage/hold visibility and the placement of the next operation. Machine names are descriptive labels, not capacity reservations.'
    if 'designer' in title:
        return 'Review availability/workload clarity and the distinction between automatic routing and an owner override.'
    if any(word in title for word in ['proof', 'feedback']):
        return 'Review artwork space, comment placement, revision identity and approval prominence.'
    if any(word in title for word in ['invoice', 'quotation', 'payment', 'advance']):
        return 'Review commercial information hierarchy and document preview/acceptance placement.'
    if 'email' in title:
        return 'Review whether queued, held and sent notifications are easy to distinguish. Real SMTP is disabled during this run.'
    return 'Review the primary action, supporting guidance and how clearly the view leads to the next stage.'

gallery = []
for i, view in enumerate(views, 1):
    src = source / view['file']
    assert src.parent == source and src.suffix == '.png' and src.is_file()
    dest = images / src.name
    shutil.copy2(src, dest)
    files.append(dest)
    gallery.append(f"\n## {i:02d}. {view['title']}\n\n{view['significance']}\n\nRedesign review: {redesign(view['title'])}\n\n![{view['title']}](screenshots/{src.name})\n")

report = f'''# J.D.Printex — operations journey and view review

24 October 2026 · Asia/Calcutta · Native Java 21, Gradle, PostgreSQL and Next.js. No Docker.

## Result

The final focused desktop browser journey passed in 35.0 seconds (37.3-second runner) through delivery, with {len(views)} captured views including narrow-screen captures. The focused Gradle operations integration case passed. Frontend production build/TypeScript passed. Broad suites were not repeated.

The journey created a fictional enquiry, activated the intake-created customer account, previewed/accepted the quotation, confirmed automatic designer assignment, requested a pinned design revision, approved the revised proof, recorded verified test advance/balance receipts, previewed the invoice, assigned a printer/delivery worker, exercised a production hold and rework, passed QC, verified the missing-pickup-photo dispatch hold, uploaded pickup/transit/delivery images and confirmed delivery. The customer reviewed the private gallery/full image and final work overview.

Manual receipts represent synthetic confirmed funds; no real funds or provider capture were exercised. Real SMTP was disabled. Outgoing lifecycle messages were checked as queued records. The browser used native Defender scanning; the Gradle case used a scanner test double to exercise quarantine/rescan. The durable 48-hour completion period was not waited out in this browser run.

## Minimum verification

- Gradle: integrationTest --tests com.printflow.WorkflowMailIntegrationTest.routingProductionQueueAndDeliveryPhotosRespectScopesAndGates bootJar.
- Frontend: npm.cmd run build.
- Browser: scripts/Test-WorkflowMailBrowser.ps1 -Check operations -Project desktop.
- New view captures are indexed below with their significance and a redesign review prompt. Phone captures resize an existing session; a separate full mobile run was intentionally omitted.

The first focused test exposed a missing optional assignment-field default; the second exposed reuse of a PDF-specific immutable-artifact trigger for photos. Both were corrected, then the same focused case passed. The UI walkthrough also corrected a stale owner-stage projection that hid delivery assignment after customer acceptance. The same focused Gradle case additionally verifies production notices include the assigned printer. Selector/fixture failures, if any, are not counted as product passes.

## Concrete redesign observations

- The owner dashboard header, summaries and toolbar push most desktop queue cards below the first viewport. A compact operations header would expose more of the print-floor board.
- Phone production lanes require horizontal movement; the captured Ready lane shows the real card. Stage jump controls could make that movement clearer.
- Gallery thumbnails crop images to keep rows aligned. Full-photo preview preserves the whole image; consider whether contain/cover controls would help evidence review.
- Long synthetic staff identifiers demonstrate why concise public staff names and compact references matter on cards.
- Delivery forms and evidence compete for vertical space. Grouping the required photo and next milestone action could make the immediate task easier to find.
- Preserve the wide work/conversation split; it keeps customer communication beside the active work.

These are review observations from the captured views, not additional changes silently applied after the test.

## Scope and privacy

The production board is project-level. Inventory, order-line operation routing, QR/job-ticket PDFs, real capacity scheduling, statutory GST invoices, GPS, partial dispatch and live payment settlement remain separate. Photos show upload times and staff statements; no location or capture-time verification is claimed.

This pack includes only the selected screenshots, view index, this report and OPERATIONS_WORKFLOW.md. Passwords, tracking-code elements, unrelated queue rows and non-synthetic designer cards are masked. No credentials, webhook secrets, browser cookies/traces, private mail payloads or real customer artwork are included. Screenshots show actual application state with fictional fixtures.

## Manual review starting point

Open http://127.0.0.1:3000/business. Owner modules: DESIGN ROUTING and PRODUCTION. The printer's work view opens the assigned production board; delivery staff use their assigned task. Customers see photos inside Production & delivery on their work page. See OPERATIONS_WORKFLOW.md for exact rules and APIs.

## View-by-view walkthrough
'''
report_path = bundle / 'README.md'
report_path.write_text(report + '\n'.join(gallery), encoding='utf-8')
files.append(report_path)
index = bundle / 'views.json'
index.write_text(json.dumps(views, indent=2), encoding='utf-8')
files.append(index)
doc = bundle / 'OPERATIONS_WORKFLOW.md'
shutil.copy2(root / 'docs/OPERATIONS_WORKFLOW.md', doc)
files.append(doc)
checks = bundle / 'verification.json'
checks.write_text(json.dumps({'date':'2026-10-24','browserCompleted':True,'screenshots':len(views),
                             'focusedGradleCase':cases[0].get('name'),'focusedGradlePassed':True,
                             'realMailSentDuringBrowserTest':False,'razorpayCaptureTested':False}, indent=2), encoding='utf-8')
files.append(checks)
checksum = bundle / 'SHA256SUMS.txt'
checksum.write_text('\n'.join(hashlib.sha256(p.read_bytes()).hexdigest() + '  ' + p.relative_to(bundle).as_posix() for p in sorted(files)) + '\n', encoding='utf-8')
files.append(checksum)
archive = out / (bundle.name + '.zip')
with zipfile.ZipFile(archive, 'w', zipfile.ZIP_DEFLATED) as z:
    for path in sorted(files):
        z.write(path, path.relative_to(out))
print(json.dumps({'zip':str(archive),'report':str(report_path),'screenshots':len(views),'bytes':archive.stat().st_size}))
