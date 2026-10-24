#!/usr/bin/env python3
"""Structural checks only. Not a replacement for Gradle, TypeScript or native runtime tests."""
from pathlib import Path
import json
import argparse

parser = argparse.ArgumentParser()
parser.add_argument('--archive', action='store_true', help='Also reject local .env in distributable archives')
args = parser.parse_args()

root = Path(__file__).resolve().parents[1]
required = [
    'README.md', 'AGENTS.md', 'CODEX_MASTER_PLAN.md', 'docs/DEVELOPMENT_HANDBOOK.md',
    'frontend/package.json', 'frontend/next.config.mjs',
    'frontend/src/app/page.tsx', 'frontend/src/app/request-quote/page.tsx',
    'frontend/src/app/track/page.tsx', 'frontend/src/app/business/page.tsx',
    'backend/build.gradle', 'backend/settings.gradle', 'backend/gradlew',
    'backend/gradlew.bat', 'backend/gradle/wrapper/gradle-wrapper.jar',
    'backend/gradle/wrapper/gradle-wrapper.properties', 'backend/src/main/resources/application.yml',
    'backend/src/main/resources/db/migration/V1__create_enquiries.sql',
    'backend/src/main/java/com/printflow/enquiry/EnquiryService.java',
    'docs/SRS.md', 'docs/ARCHITECTURE.md', 'docs/FLOWCHARTS.md',
    'docs/PHASE_BACKLOG.md', 'docs/INTEGRATIONS.md', 'docs/QA_TEST_PLAN.md',
    'scripts/smoke.ps1', 'scripts/smoke.sh', '.github/workflows/ci.yml',
    'backend/src/main/java/com/printflow/identity/SecurityConfiguration.java',
    'backend/src/main/java/com/printflow/enquiry/StaffEnquiryService.java',
    'backend/src/main/resources/db/migration/V2__staff_and_followups.sql',
    'backend/src/integrationTest/java/com/printflow/StaffWorkflowIntegrationTest.java',
    'frontend/src/components/StaffWorkspace.tsx', 'frontend/e2e/foundation.spec.ts',
    'scripts/Initialize-LocalDatabase.ps1', 'scripts/Initialize-LocalStaff.ps1',
    'scripts/Test-Backend.ps1', 'docs/LOCAL_DEVELOPMENT.md',
    'docs/REQUIREMENTS_TRACEABILITY.md'
]
for f in required:
    assert (root / f).is_file(), f'Missing {f}'
pkg = json.loads((root / 'frontend/package.json').read_text())
assert pkg['dependencies']['next'].startswith('16.')
assert 'JavaLanguageVersion.of(21)' in (root / 'backend/build.gradle').read_text()
assert 'distributionSha256Sum=' in (root / 'backend/gradle/wrapper/gradle-wrapper.properties').read_text()
if args.archive:
    assert not (root / '.env').exists(), 'Do not ship real/secrets .env in archive'
flyway = (root / 'backend/src/main/resources/db/migration/V1__create_enquiries.sql').read_text()
assert 'CREATE TABLE enquiries' in flyway and 'CREATE TABLE enquiry_events' in flyway
java = (root / 'backend/src/main/java/com/printflow/enquiry/EnquiryService.java').read_text()
assert 'TokenUtil.sha256(token)' in java and 'events.save' in java
controller = (root / 'backend/src/main/java/com/printflow/enquiry/DemoEnquiryController.java').read_text()
assert '@Profile("!prod")' in controller and 'X-Demo-Key' in controller
assert (root / 'CODEX_MASTER_PLAN.md').read_text().count('### PHASE ') >= 8
assert (root / 'docs/FLOWCHARTS.md').read_text().count('```mermaid') >= 6
print(f'PASS: {len(required)} expected files, JSON/Gradle config, schema, dev-only demo and docs sanity checks')
print('LIMITATION: No build/run tests performed by this script.')
