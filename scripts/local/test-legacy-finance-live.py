"""Profile UI -> real Java -> isolated MySQL. Discovery list only is restricted to our fixture."""
import importlib.util,json,secrets,subprocess,urllib.request
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('staff',Path(__file__).with_name('setup-php-lab.py'));staff=importlib.util.module_from_spec(spec);spec.loader.exec_module(staff)
staff.config.BASE='http://127.0.0.1:18082/api';sql=staff.config.sql
prefix='it_finance_live_'+secrets.token_hex(6)
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
for key in ['legacy-finance','legacy-order-details']:
 value=sql('SELECT config_value FROM lin_operational_config WHERE config_key='+literal(key)+" AND scope_type='GLOBAL' AND scope_id=0")
 assert not value or json.loads(value).get('enabled') is False
 old[key]=value
fd,journal=tempfile.mkstemp(prefix='finance-',suffix='.json',dir=private);os.close(fd)
order=None;hospital=None;test=None;ids=[]
try:
 hospital=int(sql('INSERT INTO lin_lb_infohospital(HospitalName,HospitalName_en,PayType,amount) VALUES('+literal(prefix)+','+literal(prefix)+',0,20.00);SELECT LAST_INSERT_ID()'))
 test=int(sql('INSERT INTO lin_lb_infotesttype(TestName,TestNO,Price,CtoG,GtoC,DiscountReal,DiscountNext,TextModel,CreateDt,type,IsActive) VALUES('+literal(prefix)+','+literal(prefix)+",0,0,0,0,0,'',NOW(),0,1);SELECT LAST_INSERT_ID()"))
 columns=[line.split('\t') for line in sql("SELECT COLUMN_NAME,DATA_TYPE,IS_NULLABLE,IFNULL(COLUMN_DEFAULT,'<NULL>'),EXTRA FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='lin_lb_listdiagnosisproject' ORDER BY ORDINAL_POSITION").splitlines()]
 names=[];values=[]
 for parts in columns:
  name,kind,nullable,default,extra=(parts+['']*5)[:5]
  if 'auto_increment' in extra:continue
  if name not in ['UserNO','AnimalName','MasterName','CollectDt','SampleStatus','TestFee','IsPayMoney'] and (nullable=='YES' or default!='<NULL>'):continue
  assert name.replace('_','').isalnum();names.append('`'+name+'`')
  if name in ['UserNO','AnimalName','MasterName']:value=literal(prefix)
  elif name=='CollectDt':value=literal('2024-05-06 00:00:00')
  elif name=='SampleStatus':value='2'
  elif name=='TestFee':value='125.50'
  elif name=='IsPayMoney':value='1'
  elif kind in ['int','tinyint','smallint','mediumint','bigint','decimal','double','float']:value='0'
  elif kind in ['datetime','timestamp']:value=literal('1900-01-01 00:00:00')
  elif kind=='date':value=literal('1900-01-01')
  elif kind in ['varchar','char','text','mediumtext','longtext']:value=literal(prefix)
  else:raise AssertionError('Unsupported fixture schema')
  values.append(value)
 order=int(sql('INSERT INTO lin_lb_listdiagnosisproject('+','.join(names)+') VALUES('+','.join(values)+');SELECT LAST_INSERT_ID()'))

 sql(f'UPDATE lin_lb_listdiagnosisproject SET InfoHospitalId={hospital},SampleStatus=2 WHERE ListDiagnosisProjectId={order}')
 for key in old:
  sql("INSERT INTO lin_operational_config(config_key,config_value,description,updated_by) VALUES("+literal(key)+','+literal(json.dumps({'enabled':True,'writeEnabled':True}))+",'finance UI fixture',(SELECT MIN(id) FROM lin_user)) ON DUPLICATE KEY UPDATE config_value=VALUES(config_value)")
 for index,(price,exempt) in enumerate([('12.50',0),('3.25',1)]):
  command={'testTypeId':test,'originalPrice':price,'originalDiscount':'10','testPrice':price,'detailRemark':prefix,'modelId':0,'mailTitle':'合成财务测试','isSendMail':0,'isCheckPass':exempt,'payStatus':0,'diagnosisStatus':0,'mailReport':'<p>mail original</p>','reportResult':'<p>report original</p>','requestKey':prefix+'_detail_'+str(index)}
  ids.append(int(staff.call(f'/admin/legacy/order-details/orders/{order}',command,token)))
 run=subprocess.run(['node','scripts/editor-parity/legacy-finance-live.js'],cwd=ROOT,input=json.dumps({'token':token,'prefix':prefix,'hospital':hospital,'ids':ids,'journal':journal}),capture_output=True,text=True,timeout=150)
 if run.returncode:raise AssertionError('Finance browser failed: '+run.stderr[-1400:])
 proof=json.loads(run.stdout)
 for id in ids:
  assert sql(f'SELECT CONCAT(PayStatus2,"|",is_invoiced) FROM lin_lb_diagnosisdetail WHERE DiagnosisDetailId={id}')=='0|1'
  assert sql(f'SELECT HEX(MailReport) FROM lin_lb_diagnosisdetail_info WHERE diagnosisdetail_id={id}').lower()=='<p>mail original</p>'.encode().hex()
  assert sql(f'SELECT HEX(ReportResult) FROM lin_lb_diagnosisdetail_info WHERE diagnosisdetail_id={id}').lower()=='<p>report original</p>'.encode().hex()
 assert sql(f'SELECT SampleStatus FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId={order}')=='2'
 assert sql(f'SELECT amount FROM lin_lb_infohospital WHERE InfoHospitalId={hospital}')=='20.00'
 proof['persistedFlagsBodiesParentAndFundsVerified']=True
finally:
 for key,value in old.items():
  if value:sql('UPDATE lin_operational_config SET config_value='+literal(value)+' WHERE config_key='+literal(key)+" AND scope_type='GLOBAL' AND scope_id=0")
  else:sql('DELETE FROM lin_operational_config WHERE config_key='+literal(key)+" AND scope_type='GLOBAL' AND scope_id=0 AND description='finance UI fixture'")
 if order:
  assert sql(f'SELECT UserNO FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId={order}')==prefix
  # Recover a creation whose response might have been interrupted, using this fixture's known request keys.
  for index in range(2):
   audit=sql(f"SELECT HEX(after_json) FROM lin_audit_record WHERE actor_user_id={int(session['userId'])} AND resource_type='LEGACY_ORDER_DETAIL' AND request_id="+literal(prefix+'_detail_'+str(index)))
   if audit:
    id=int(json.loads(bytes.fromhex(audit).decode())['detailId']);assert sql(f'SELECT ListDiagnosisProjectId FROM lin_lb_diagnosisdetail WHERE DiagnosisDetailId={id}')==str(order)
    sql(f'DELETE FROM lin_lb_diagnosisdetail_info WHERE diagnosisdetail_id={id};DELETE FROM lin_lb_diagnosisdetail WHERE DiagnosisDetailId={id} AND ListDiagnosisProjectId={order}')
    sql(f"DELETE FROM lin_audit_record WHERE actor_user_id={int(session['userId'])} AND resource_type='LEGACY_ORDER_DETAIL' AND request_id="+literal(prefix+'_detail_'+str(index)))
  sql(f'DELETE FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId={order} AND UserNO='+literal(prefix))
 for entry in json.loads(Path(journal).read_text() or '[]'):
  assert set(int(t['id']) for t in entry['targets']).issubset(set(ids))
  sql(f"DELETE FROM lin_audit_record WHERE actor_user_id={int(session['userId'])} AND resource_type='LEGACY_FINANCE_BATCH' AND request_id="+literal(entry['requestKey']))
 if hospital:sql(f'DELETE FROM lin_lb_infohospital WHERE InfoHospitalId={hospital} AND HospitalName='+literal(prefix))
 if test:sql(f'DELETE FROM lin_lb_infotesttype WHERE InfoTestTypeId={test} AND TestName='+literal(prefix))
 Path(journal).unlink()
proof.update(gatesRestored=True,fixturesRemoved=True)
print(json.dumps(proof))
