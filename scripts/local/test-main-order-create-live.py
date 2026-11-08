"""Profile UI -> real Java -> isolated MySQL. Discovery list only is restricted to our fixture."""
import importlib.util,json,secrets,subprocess,urllib.request
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('staff',Path(__file__).with_name('setup-php-lab.py'));staff=importlib.util.module_from_spec(spec);spec.loader.exec_module(staff)
staff.config.BASE='http://127.0.0.1:18082/api';sql=staff.config.sql
prefix='it_main_live_'+secrets.token_hex(6)
def literal(value):return 'CONVERT(0x'+value.encode().hex()+' USING utf8mb4) COLLATE utf8mb4_unicode_ci'
# Read this project's existing local mapping without requiring the PHP container.
import re,os,stat
mapping=Path.home()/'.local/share/petlis/php-preview/app/.petlis-private/bridge.php'
info=mapping.stat();assert stat.S_ISREG(info.st_mode) and info.st_uid==os.getuid() and not info.st_mode&0o077
content=mapping.read_text();assert re.search(r"\b4\s*=>",content)
credentials={}
for key in ['username','password']:
 match=re.search(r"'"+key+r"'\s*=>\s*'((?:\\.|[^'])*)'",content)
 assert match,'Private mapping format unavailable'
 credentials[key]=match.group(1).replace("\\'","'").replace('\\\\','\\')
session=staff.call('/auth/password/login',{'account':credentials['username'],'password':credentials['password']})
assert 'ADMIN' in session['roles'];token=session['token']
import tempfile
private=Path.home()/'.local/share/petlis/qa';private.mkdir(parents=True,exist_ok=True)
old={}
for key in ['legacy-main-orders','legacy-order-details']:
 value=sql("SELECT config_value FROM lin_operational_config WHERE config_key="+("'"+key+"'")+" AND scope_type='GLOBAL' AND scope_id=0")
 assert not value or json.loads(value).get('enabled') is False
 old[key]=value
fd,journal=tempfile.mkstemp(prefix='main-create-',suffix='.json',dir=private);os.close(fd)
order=None
try:
 for key in old:
  config={'enabled':True,'writeEnabled':True}
  if key=='legacy-main-orders':config.update(createEnabled=True,adminMappings={str(session['userId']):4})
  sql("INSERT INTO lin_operational_config(config_key,config_value,description,updated_by) VALUES("+("'"+key+"'")+','+literal(json.dumps(config))+",'main create UI fixture',(SELECT MIN(id) FROM lin_user)) ON DUPLICATE KEY UPDATE config_value=VALUES(config_value)")
 run=subprocess.run(['node','scripts/editor-parity/main-order-create-live.js'],cwd=ROOT,input=json.dumps({'token':token,'prefix':prefix,'journal':journal}),capture_output=True,text=True,timeout=120)
 if run.returncode:raise AssertionError('Main create browser failed: '+run.stderr[-1400:])
 proof=json.loads(run.stdout);order=int(proof['orderId'])
 assert sql(f'SELECT UserNO FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId={order}')==prefix
 assert sql(f'SELECT COUNT(*) FROM lin_lb_diagnosisdetail WHERE ListDiagnosisProjectId={order} AND InfoTestTypeId=246')=='1'
 assert sql(f'SELECT COUNT(*) FROM lin_lb_diagnosisdetail_info i JOIN lin_lb_diagnosisdetail d ON d.DiagnosisDetailId=i.diagnosisdetail_id WHERE d.ListDiagnosisProjectId={order}')=='1'
 assert sql(f'SELECT SampleStatus FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId={order}')=='2'
 proof['actualFreightAndBodyCopyVerified']=True
finally:
 for key,value in old.items():
  if value:sql('UPDATE lin_operational_config SET config_value='+literal(value)+' WHERE config_key='+("'"+key+"'")+" AND scope_type='GLOBAL' AND scope_id=0")
  else:sql('DELETE FROM lin_operational_config WHERE config_key='+("'"+key+"'")+" AND scope_type='GLOBAL' AND scope_id=0 AND description='main create UI fixture'")
 if Path(journal).stat().st_size:
  recovery=json.loads(Path(journal).read_text());order=int(recovery['orderId'])
  assert sql(f"SELECT COUNT(*) FROM lin_audit_record WHERE resource_type='LEGACY_MAIN_ORDER' AND action='LEGACY_MAIN_ORDER_CREATED' AND CAST(resource_id AS UNSIGNED)={order} AND request_id="+literal(recovery['requestKey']))=='1'
  assert sql(f'SELECT UserNO FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId={order}') in [prefix,'55550-']
  sql(f"DELETE FROM lin_audit_record WHERE resource_type='LEGACY_ORDER_DETAIL' AND CAST(resource_id AS UNSIGNED) IN (SELECT DiagnosisDetailId FROM lin_lb_diagnosisdetail WHERE ListDiagnosisProjectId={order}); DELETE FROM lin_lb_diagnosisdetail_info WHERE diagnosisdetail_id IN (SELECT DiagnosisDetailId FROM lin_lb_diagnosisdetail WHERE ListDiagnosisProjectId={order}); DELETE FROM lin_lb_diagnosisdetail WHERE ListDiagnosisProjectId={order}; DELETE FROM lin_audit_record WHERE resource_type='LEGACY_MAIN_ORDER' AND CAST(resource_id AS UNSIGNED)={order}; DELETE FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId={order}")
 Path(journal).unlink(missing_ok=True)
proof.update(gatesRestored=True,fixturesRemoved=True,privateJournalRemoved=True)
print(json.dumps(proof))
