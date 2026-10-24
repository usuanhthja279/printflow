"""Native staff persistence/session checks; credentials remain local and are never printed."""
import argparse
from datetime import datetime, timedelta, timezone
import http.cookiejar
import json
from pathlib import Path
import subprocess
import time
import urllib.error
import urllib.parse
import urllib.request

parser=argparse.ArgumentParser()
parser.add_argument('--restart',action='store_true')
args=parser.parse_args()
config=dict(line.split('=',1) for line in Path('.env').read_text().splitlines() if line and not line.startswith('#') and '=' in line)
cookies=http.cookiejar.CookieJar()
client=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(cookies))
base='http://127.0.0.1:8080'

def call(method,path,body=None,headers=None):
    data=None if body is None else json.dumps(body).encode()
    req=urllib.request.Request(base+path,data=data,method=method,headers={'Content-Type':'application/json',**(headers or {})})
    try:
        with client.open(req,timeout=15) as response:return response.status,json.load(response)
    except urllib.error.HTTPError as error:return error.code,None

def csrf():
    code,data=call('GET','/api/v1/auth/csrf')
    assert code==200
    return {data['headerName']:data['token']}

def login():
    headers=csrf()
    headers['Content-Type']='application/x-www-form-urlencoded'
    body=urllib.parse.urlencode({'username':config['APP_STAFF_BOOTSTRAP_USERNAME'],'password':config['APP_STAFF_BOOTSTRAP_PASSWORD']}).encode()
    req=urllib.request.Request(base+'/api/v1/auth/session',data=body,headers=headers,method='POST')
    with client.open(req,timeout=15) as response:assert response.status==200

def ready():
    for _ in range(60):
        try:
            if call('GET','/actuator/health')[0]==200:return
        except (urllib.error.URLError,ConnectionError,TimeoutError):pass
        time.sleep(1)
    raise AssertionError('Native API health timeout')

ready()
assert call('GET','/api/v1/staff/enquiries')[0]==401
assert call('GET','/api/demo/enquiries')[0]==404
login()
request={'enquiry':{'customerName':'Staff Persistence Synthetic','email':'persistence@example.test',
         'phone':'9000000000','productCode':'HOTEL_PRINT','quantity':250},'source':'PHONE'}
code,created=call('POST','/api/v1/staff/enquiries',request,csrf())
assert code==201
lead=created['lead']
path='/api/v1/staff/enquiries/'+lead['id']
assert call('PATCH',path+'/follow-up',{'revision':lead['revision'],'priority':'HIGH','nextAction':'Incomplete plan'},csrf())[0]==400
due=(datetime.now(timezone.utc)+timedelta(days=1)).replace(microsecond=0).isoformat().replace('+00:00','Z')
code,lead=call('PATCH',path+'/follow-up',{'revision':lead['revision'],'priority':'HIGH','nextAction':'Call synthetic customer','followUpAt':due},csrf())
assert code==200
code,lead=call('PATCH',path+'/status',{'revision':lead['revision'],'status':'REVIEWING'},csrf())
assert code==200
assert len(call('GET',path+'/activities')[1])==3
print('PASS: secure staff login, default demo exclusion, complete-plan validation and staff audit')
if args.restart:
    subprocess.run(['pwsh','-NoProfile','-File','./Stop-PrintFlow.ps1'],check=True,stdout=subprocess.DEVNULL,stderr=subprocess.STDOUT)
    subprocess.run(['pwsh','-NoProfile','-File','./Start-PrintFlow.ps1','-SkipBuild'],check=True,stdout=subprocess.DEVNULL,stderr=subprocess.STDOUT)
    ready()
    assert call('GET','/api/v1/auth/session')[0]==401
    login()
    code,leads=call('GET','/api/v1/staff/enquiries')
    assert code==200
    saved=next(item for item in leads if item['id']==lead['id'])
    for key in ['assignedStaffId','nextAction','followUpAt','priority','revision','status']:
        assert saved[key]==lead[key],key
    assert len(call('GET',path+'/activities')[1])==3
    print('PASS: native restart revokes old session and preserves staff account, assignment, plan, revision and audit')
code,tracked=call('GET','/api/public/enquiries/'+lead['publicId'],headers={'X-Tracking-Token':created['trackingToken']})
assert code==200 and tracked['status']=='REVIEWING' and len(tracked['events'])==2
assert not any(field in tracked for field in ['email','phone','nextAction','followUpAt','assignedStaffId'])
assert call('POST','/api/v1/auth/logout',headers=csrf())[0]==200
assert call('GET','/api/v1/staff/enquiries')[0]==401
print('PASS: public tracking stays private; logout invalidates staff session')
