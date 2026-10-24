"""Synthetic Phase 0 HTTP/database checks. Never prints tracking credentials."""
import argparse
from concurrent.futures import ThreadPoolExecutor
import json
import os
from pathlib import Path
import subprocess
import time
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

parser = argparse.ArgumentParser()
parser.add_argument('--base', default='http://127.0.0.1:8080')
parser.add_argument('--prod-base')
parser.add_argument('--restart', action='store_true')
args = parser.parse_args()
if Path('.env').exists():
    for line in Path('.env').read_text().splitlines():
        if line and not line.startswith('#') and '=' in line:
            key, value = line.split('=', 1)
            os.environ.setdefault(key, value)
demo_key = os.environ.get('DEMO_ADMIN_KEY', 'local-demo-key-change-this-123456')

def request(method, path, body=None, headers=None, base=None):
    data = None if body is None else json.dumps(body).encode()
    req = Request((base or args.base) + path, data=data, method=method,
                  headers={'Content-Type': 'application/json', **(headers or {})})
    try:
        with urlopen(req, timeout=15) as response:
            return response.status, json.load(response)
    except HTTPError as error:
        return error.code, None

def ready(base):
    for _ in range(60):
        try:
            code, data = request('GET', '/actuator/health', base=base)
            if code == 200 and data['status'] == 'UP':
                return
        except (URLError, TimeoutError, ConnectionError):
            pass
        time.sleep(2)
    raise AssertionError('API did not become healthy')

def create(product='WEDDING_CARDS', **overrides):
    body = dict(customerName='Foundation Synthetic Customer', email='foundation@example.test',
                phone='9000000000', productCode=product, quantity=250,privacyConsent=True,privacyVersion='2026-10-23')
    body.update(overrides)
    code, data = request('POST', '/api/public/enquiries', body)
    return code, data

ready(args.base)
admin = {'X-Demo-Key': demo_key}
assert request('GET', '/api/demo/enquiries')[0] == 401
assert request('GET', '/api/demo/enquiries', headers={'X-Demo-Key': 'invalid'})[0] == 401
for product in ['WEDDING_CARDS', 'HOTEL_PRINT', 'BROCHURES', 'OFFSET_GLOSS']:
    code, _ = create(product)
    assert code == 201, ('catalogue create', product, code)
for invalid in [dict(quantity=0), dict(productCode='UNKNOWN'), dict(email='invalid')]:
    assert create(**invalid)[0] == 400
print('PASS: health, confirmed catalogue products, validation and owner-key negatives')

code, created = create()
assert code == 201
path = '/api/public/enquiries/' + created['publicId']
track = {'X-Tracking-Token': created['trackingToken']}
assert request('GET', path)[0] == 404
assert request('GET', path, headers={'X-Tracking-Token': 'invalid'})[0] == 404
_, listed = request('GET', '/api/demo/enquiries', headers=admin)
item = next(row for row in listed if row['publicId'] == created['publicId'])
status_path = '/api/demo/enquiries/' + item['id'] + '/status'
with ThreadPoolExecutor(max_workers=3) as pool:
    outcomes = list(pool.map(lambda status: request('PATCH', status_path, {'status': status}, admin)[0],
                             ['REVIEWING', 'QUOTED', 'CLOSED']))
assert all(code in (200, 400) for code in outcomes), outcomes
code, tracked = request('GET', path, headers=track)
assert code == 200 and tracked['status'] == 'CLOSED'
assert set(tracked) == {'publicId','productCode','quantity','status','createdAt','events'}
statuses = [event['status'] for event in tracked['events']]
rank = {'NEW': 0, 'REVIEWING': 1, 'QUOTED': 2, 'CLOSED': 3}
assert statuses == sorted(set(statuses), key=rank.get), statuses
assert request('PATCH', status_path, {'status': 'CLOSED'}, admin)[0] == 200
assert request('PATCH', status_path, {'status': 'NEW'}, admin)[0] == 400
assert request('GET', path, headers=track)[1]['events'] == tracked['events']
print('PASS: token confidentiality, public projection, concurrent transitions and idempotent closure')

if args.restart:
    subprocess.run(['pwsh', '-NoProfile', '-File', './Stop-PrintFlow.ps1'], check=True,
                   stdout=subprocess.DEVNULL, stderr=subprocess.STDOUT)
    subprocess.run(['pwsh', '-NoProfile', '-File', './Start-PrintFlow.ps1', '-SkipBuild', '-EnableDemo'], check=True,
                   stdout=subprocess.DEVNULL, stderr=subprocess.STDOUT)
    ready(args.base)
    assert request('GET', path, headers=track)[1] == tracked
    print('PASS: PostgreSQL and API restart preserve record, token and event history')

if args.prod_base:
    ready(args.prod_base)
    assert request('GET', '/api/demo/enquiries', headers=admin, base=args.prod_base)[0] == 404
    assert request('POST', '/api/public/enquiries',
                   {'customerName':'Synthetic','email':'test@example.test','phone':'9000000000',
                    'productCode':'WEDDING_CARDS','quantity':10}, base=args.prod_base)[0] == 503
    print('PASS: prod excludes demo endpoints and disables guest intake')
print('PASS: foundation HTTP checks (synthetic data only)')
