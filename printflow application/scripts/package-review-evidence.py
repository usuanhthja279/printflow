"""Package explicitly selected review evidence; never claim an unfinished flow passed."""
from pathlib import Path
import hashlib, json, shutil, zipfile
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parent.parent
private = root / '.local'
out = root / 'output/review-2026-10-24'
bundle = out / 'J-D-Printex-Implementation-Review'
bundle.mkdir(parents=True, exist_ok=True)
groups = []
for source in sorted((private / 'razorpay-evidence/public').glob('design-experience-*')):
    if (source / 'views.json').exists():
        device = 'phone' if source.name.endswith('mobile') else 'desktop'
        groups.append(('Public experience — ' + device, source, 'public-' + device, None))
safe_flow_views = {'02-enquiry-saved.png', '04-account-activation.png', '05-quotation-preview.png',
                   '08-artwork-comments.png', '09-revision-requested.png', '10-proof-approved.png',
                   '12-production-blocked.png', '13-advance-banks.png', '13-advance-bank-selected.png'}
for source in (root / 'frontend/test-results').glob('razorpay-flow-*-desktop'):
    if (source / 'views.json').exists():
        groups.append(('Workflow reached before checkout automation stopped', source, 'workflow', safe_flow_views))
gallery, published = [], []
screens = 0
for title, source, folder, allowlist in groups:
    gallery.append('\n## ' + title + '\n')
    target = bundle / 'screenshots' / folder
    target.mkdir(parents=True, exist_ok=True)
    for view in json.loads((source / 'views.json').read_text(encoding='utf-8')):
        if allowlist is not None and view['file'] not in allowlist:
            continue
        src = source / view['file']
        assert src.parent == source and src.suffix == '.png' and src.is_file()
        dest = target / src.name
        shutil.copy2(src, dest)
        published.append(dest)
        screens += 1
        gallery.append(f"\n### {view['description']}\n\n![{view['description']}](screenshots/{folder}/{src.name})\n")
tests = []
for relative in ['test/TEST-com.printflow.customer.InvoicePdfTest.xml',
                 'test/TEST-com.printflow.identity.QuotationMailTest.xml',
                 'integrationTest/TEST-com.printflow.WorkflowMailIntegrationTest.xml']:
    source = root / 'backend/build/test-results' / relative
    if not source.exists():
        continue
    suite = ET.parse(source).getroot()
    for case in suite.findall('testcase'):
        tests.append({'suite': suite.attrib['name'], 'test': case.attrib['name'],
                      'passed': case.find('failure') is None and case.find('error') is None and case.find('skipped') is None})
metadata = {'date': '2026-10-24', 'screenshots': screens, 'focusedGradleTests': tests,
            'razorpayUiCaptureVerified': False, 'externalCapturedWebhookVerified': False,
            'invoiceInboxDeliveryVerified': False,
            'note': 'Further browser payment retries stopped at owner request. Earlier screenshots precede final UI polish.'}
(bundle / 'test-results.json').write_text(json.dumps(metadata, indent=2), encoding='utf-8')
published.append(bundle / 'test-results.json')
for name in ['UI_EXPERIENCE_REDESIGN.md', 'PAYMENTS_AND_INVOICES.md', 'IMPLEMENTATION_REVIEW.md']:
    dest = bundle / name
    shutil.copy2(root / 'docs' / name, dest)
    published.append(dest)
invoice_preview = root / 'backend/build/invoice-preview.png'
if invoice_preview.exists():
    dest = bundle / 'invoice-layout-preview.png'
    shutil.copy2(invoice_preview, dest)
    published.append(dest)
report = f'''# J.D.Printex implementation review

24 October 2026. Native Java 21 / Gradle / PostgreSQL / Next.js. No Docker.

The owner requested implementation first and minimum further testing. See IMPLEMENTATION_REVIEW.md for changes and manual testing steps.

## Actual verification

- {sum(t['passed'] for t in tests)} of {len(tests)} cases in the available focused Gradle XML reports passed: invoice PDF, mail attachment integrity and HTTP payment/workflow rules. Provider responses in integration cases are mocked.
- Earlier public desktop and phone interaction checks passed. Screenshots cover the homepage, finish selection, catalogue filtering, product details, quote entry and account/staff entry.
- The latest browser journey reached guest/account creation, quotation preview/acceptance, designer revision approval, payment rules and blocked unpaid production. Razorpay TEST checkout opened and a bank was selected. Automation then timed out looking for the provider payment button.
- Actual provider capture, balance receipt through the provider, external payment.captured webhook delivery and invoice inbox delivery are **not verified**. An unfinished checkout is not a successful payment.
- Earlier manual-receipt UI testing reached invoice preview and delivery; this does not prove Razorpay capture.
- Screenshots precede final mobile-menu, sample-search and conversation polish. These are actual views, not an exhaustive capture of every screen or the 88-view reference prototype.

## Contents and privacy

{screens} selected screenshots, an optional invoice layout image, implementation/finance notes and sanitized test summaries. No keys, webhook secrets, setup files, tokens, cookies, recovery codes, private invoice/email payloads or raw browser traces are included.

Commercial invoices are implemented. GST invoices, refunds/settlements, full inventory and detailed shop-floor routing remain future work.

## Temporary webhook

The user confirmed saving a Test Mode payment.captured webhook in the Dashboard. A separate private secret is stored locally. Public delivery is available only while the local relay and tunnel run. Stop them with scripts/Stop-TestWebhook.ps1 and disable the temporary Dashboard entry when finished. This is not a permanent endpoint.

## Gallery
'''
(bundle / 'README.md').write_text(report + '\n'.join(gallery), encoding='utf-8')
published.append(bundle / 'README.md')
checksum = bundle / 'SHA256SUMS.txt'
checksum.write_text('\n'.join(hashlib.sha256(p.read_bytes()).hexdigest() + '  ' + p.relative_to(bundle).as_posix() for p in sorted(published)) + '\n', encoding='utf-8')
published.append(checksum)
archive = out / (bundle.name + '.zip')
with zipfile.ZipFile(archive, 'w', zipfile.ZIP_DEFLATED) as z:
    for p in sorted(published):
        z.write(p, p.relative_to(out))
print(json.dumps({'zip': str(archive), 'report': str(bundle / 'README.md'), 'screenshots': screens, 'gradleCases': len(tests), 'bytes': archive.stat().st_size}))
