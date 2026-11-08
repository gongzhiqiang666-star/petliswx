"""Normal new-system login, capability gate and read-only original dictionary UI. No PHP business bridge."""
import importlib.util,json,subprocess,time,urllib.request,urllib.error
from pathlib import Path
spec=importlib.util.spec_from_file_location('staff',Path(__file__).with_name('setup-php-lab.py'));staff=importlib.util.module_from_spec(spec);spec.loader.exec_module(staff)
root=Path.home()/'.local/share/petlis';B=str(Path.home()/'.codex/skills/gstack/browse/dist/browse')
def b(*args):
 p=subprocess.run([B,*args],capture_output=True,text=True);assert p.returncode==0,'Local dictionary browser action failed';return p.stdout.strip()
def wait(expr):
 for _ in range(80):
  if b('js',expr)=='true':return
  time.sleep(.15)
 raise AssertionError('Dictionary UI did not reach expected state')
def request(path,token,data=None,method=None):
 req=urllib.request.Request(staff.config.BASE+path,data=None if data is None else json.dumps(data).encode(),headers={'Content-Type':'application/json','Authorization':'Bearer '+token},method=method)
 try:
  with staff.opener.open(req,timeout=15) as response:r=json.load(response)
 except urllib.error.HTTPError as error:
  raw=error.read()
  try:r=json.loads(raw)
  except (ValueError,UnicodeError):r={'code':error.code}
  if not isinstance(r,dict):r={}
  r['code']=error.code
 return r

for _ in range(40):
 try:
  with staff.opener.open(staff.config.BASE+'/auth/capabilities',timeout=1) as r:assert r.status==200
  break
 except OSError:time.sleep(.25)
else:raise AssertionError('Petlis backend not ready')
admin=staff.administrator(4);key='legacy-test-catalog'
assert staff.config.sql("SELECT JSON_UNQUOTE(JSON_EXTRACT(config_value,'$.enabled')) FROM lin_operational_config WHERE config_key='legacy-test-catalog' AND scope_type='GLOBAL' AND scope_id=0")=='false'
def gate(enabled):
 version=int(staff.config.sql("SELECT version FROM lin_operational_config WHERE config_key='legacy-test-catalog' AND scope_type='GLOBAL' AND scope_id=0"))
 assert request('/admin/operations/configs/'+key,admin,{'value':{'enabled':enabled,'writeEnabled':False,'deleteEnabled':False},'version':version,'reason':'原折扣核价只读验收'},'PUT')['code']==200
proof={'module':'M09','businessWrites':False}
try:
 gate(True);before=staff.config.sql("SELECT COUNT(*) FROM lin_audit_record WHERE resource_type='LEGACY_TEST_CATALOG'")
 for price,discount,expected in [('100.00','7.55','75.00'),('12.59','1','1.25'),('125.50','其他折扣','125.50'),('1.005','原价','1.00'),('-0.005','原价','-0.01')]:
  r=request('/admin/legacy/test-catalog/quote',admin,{'price':price,'discount':discount},'POST');assert r['code']==200 and r['data']['price']==expected
 assert request('/admin/legacy/test-catalog/quote',admin,{'price':'NaN','discount':'9'},'POST')['code']==400
 assert staff.config.sql("SELECT COUNT(*) FROM lin_audit_record WHERE resource_type='LEGACY_TEST_CATALOG'")==before
 doctor=json.loads((root/'qa/native-sf-history-private.json').read_text());dt=staff.call('/auth/password/login',{'account':doctor['account'],'password':doctor['password']})['token'];assert request('/admin/legacy/test-catalog/quote',dt,{'price':'100','discount':'9'},'POST')['code']==403
 proof.update(quoteCases=5,originalStepsAndDisplay=True,invalidRejected=True,doctorDenied=True,noBusinessAuditOrDataWrites=True)
finally:
 gate(False);assert request('/admin/legacy/test-catalog/quote',admin,{'price':'100','discount':'9'},'POST')['code']==409;proof['gatesRestoredClosed']=True
staff.save_private(root/'qa/legacy-pricing-api.json',proof);print(json.dumps(proof))
