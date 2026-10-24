"""Package only the focused synthetic redesign views, not private runtime data."""
from pathlib import Path
import hashlib
import json
import shutil
import zipfile
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parent.parent
results = root / 'frontend/test-results'
assert json.loads((results / '.last-run.json').read_text(encoding='utf-8'))['status'] == 'passed'
suite = ET.parse(root / 'backend/build/test-results/integrationTest/TEST-com.printflow.WorkflowMailIntegrationTest.xml').getroot()
assert suite.get('tests') == '1' and suite.get('failures') == '0' and suite.get('errors') == '0'
out = root / 'output/redesign-batch1-2026-10-25'
bundle = out / 'J-D-Printex-Redesign-Batch1'
captions = {
    '01-customer-project.png': 'Customer project hub: the next action, five server-derived stages and conversation are visible together on desktop; phone stacks the same content.',
    '02-proof-studio.png': 'Current proof studio: page rail, canvas, pins and revision-specific comments/approval. Both PDF pages were explicitly reviewed before confirmation; stale responses require fresh review.',
    '03-finance-readiness.png': 'Finance readiness: verified advance, current agreement, invoice and balance/credit are separate checks. Quarantined artwork cannot be approved.',
    '04-production-queue.png': 'Production queue: real scoped lanes on desktop; phone lane selector shows vertical cards with assignment, machine label, due date and urgency.',
    '05-production-hold.png': 'Production detail drawer: HOLD preserves QC and its reason. Resume uses the next revision; stale updates retain the note and clear confirmation.',
    '06-delivery-evidence.png': 'Evidence before dispatch: quarantined pickup evidence does not satisfy the server gate; a rejected action preserves the operator note.',
    '07-owner-action-inbox.png': 'Owner action inbox: loaded intake, sales assignment and overdue follow-ups are prioritized without inventing finance or designer data. Module headings change with navigation.',
    '08-original-photo-preview.png': 'Original private photo preview: the synthetic rescan response allows full-original review. The filename and server upload time are not described as uploader identity or capture location.'
}
copied = []
for device in ['desktop', 'mobile']:
    destination = bundle / 'screenshots' / device
    destination.mkdir(parents=True, exist_ok=True)
    for filename in captions:
        matches = [p for p in results.glob('workflow-redesign-*-' + device + '/' + filename)]
        assert len(matches) == 1, (device, filename, matches)
        target = destination / filename
        shutil.copyfile(matches[0], target)
        copied.append(target)

summary = {
    'branch': 'feature/workflow-redesign-batch1',
    'scope': 'Batch 1 presentation, synthetic UI responses, no production deployment',
    'browserChecks': 12,
    'devices': ['1366x900 desktop', '390x664 phone emulation, DPR 3'],
    'screenshots': len(copied),
    'gradleTest': suite.find('testcase').get('name'),
    'gradleFailures': 0,
    'typescript': 'passed',
    'staticValidation': '39 checks passed',
    'realMailOrPaymentsExercised': False
}
(bundle / 'verification.json').write_text(json.dumps(summary, indent=2) + '\n', encoding='utf-8')
guide = '''# J.D.Printex — Workflow redesign, Batch 1

25 October 2026. These are actual screenshots of the implemented Next.js UI with intercepted synthetic API responses. They demonstrate presentation and interaction boundaries, not real customer activity, money movement, email delivery or a complete live end-to-end order.

Implemented on `feature/workflow-redesign-batch1`. Native Gradle and running PostgreSQL; no Docker. Only the frontend switched to hot reload. The existing API/database services were reused. No production build or deployment was performed for this presentation batch.

## Verification

- `npm.cmd run typecheck`: passed.
- `npm.cmd run test:e2e -- workflow-redesign.spec.ts --project=desktop --project=mobile --max-failures=1`: 12 passed.
- `./backend/gradlew.bat -p backend integrationTest --rerun --tests com.printflow.WorkflowMailIntegrationTest.routingProductionQueueAndDeliveryPhotosRespectScopesAndGates`: one real HTTP/PostgreSQL regression passed; compiler outputs reused.
- `python scripts/static_validate.py`: 39 checks passed. Structural validation does not replace runtime tests.
- Keyboard escape/focus restoration, labeled controls, page acknowledgement, stale responses, quarantine, advance blockers, hold/resume payloads, failed dispatch, original-photo preview and phone body width were checked. This is not a full accessibility audit.

The initial sandbox browser launch was blocked before page tests executed. Diagnostic runs then corrected a Next.js route-announcer selector, an inconsistent scan fixture and a missing asynchronous resume wait. Product fixes keep stale-proof feedback inside the studio and reset confirmations after rejected/stale operations. Only the final successful screenshots are packaged.

## Deliberate limits

No new backend flow, endpoint, schema, inventory, preflight bypass, reorder, amendment case, split dispatch or connected provider was introduced. Owner exceptions use only loaded enquiry data. Finance and exact-proof readiness are separate. Public PF reference, requested/agreed delivery date and uploader identity are not present in the work/photo DTOs used here; the UI does not invent them. Actual agreements remain in quotation details. The PDF fixture has two pages; very long/complex PDFs and real phone hardware remain manual review items. Earlier real-provider payment/email limitations remain unchanged.

## View significance

'''
for filename, caption in captions.items():
    guide += '### ' + filename[:-4].replace('-', ' ').title() + '\n\n' + caption + '\n\n'
    guide += '[Desktop](screenshots/desktop/' + filename + ') · [Phone](screenshots/mobile/' + filename + ')\n\n'
(bundle / 'README.md').write_text(guide, encoding='utf-8')
html = '<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width"><title>J.D.Printex Batch 1 review</title><style>body{font:15px system-ui;background:#f4f6f9;color:#0b2233;max-width:1440px;margin:auto;padding:32px}h1{font-size:32px}section{background:white;border:1px solid #dde5ed;border-radius:12px;padding:24px;margin:24px 0}.views{display:flex;gap:24px;align-items:flex-start}figure{margin:0;flex:1;min-width:0}figure:last-child{max-width:300px}img{max-width:100%;height:auto;border:1px solid #e1e7ee}figcaption{margin:12px 0;color:#65798d}@media(max-width:700px){.views{flex-direction:column}}</style><h1>J.D.Printex · Batch 1 review</h1><p>Implemented views with synthetic intercepted API data. No live money, email delivery or deployment. See README.md for commands and limitations.</p>'
for filename, caption in captions.items():
    html += '<section><h2>' + filename[:-4].replace('-', ' ').title() + '</h2><p>' + caption + '</p><div class="views">'
    for device in ['desktop', 'mobile']:
        url = 'screenshots/' + device + '/' + filename
        html += '<figure><a href="' + url + '"><img loading="lazy" alt="' + device + ' ' + filename + '" src="' + url + '"></a><figcaption>' + device + '</figcaption></figure>'
    html += '</div></section>'
(bundle / 'index.html').write_text(html + '</html>', encoding='utf-8')
shutil.copyfile(root / 'docs/WORKFLOW_REDESIGN_BATCH1.md', bundle / 'IMPLEMENTATION.md')
shutil.copyfile(root / 'docs/WORKFLOW_REDESIGN_SPEC.md', bundle / 'DESIGN_BRIEF.md')
files = sorted(p for p in bundle.rglob('*') if p.is_file() and p.name != 'SHA256SUMS')
(bundle / 'SHA256SUMS').write_text(''.join(hashlib.sha256(p.read_bytes()).hexdigest() + '  ' + p.relative_to(bundle).as_posix() + '\n' for p in files), encoding='utf-8')
archive = out / (bundle.name + '.zip')
with zipfile.ZipFile(archive, 'w', zipfile.ZIP_DEFLATED) as z:
    for p in sorted(bundle.rglob('*')):
        if p.is_file(): z.write(p, p.relative_to(out))
with zipfile.ZipFile(archive) as z:
    assert z.testzip() is None
    assert len([n for n in z.namelist() if n.endswith('.png')]) == 16
print(json.dumps({'archive': str(archive), 'screenshots': len(copied), 'bytes': archive.stat().st_size}))
