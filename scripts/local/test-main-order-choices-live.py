"""Profile UI -> real Java -> isolated MySQL. Discovery list only is restricted to our fixture."""
import importlib.util,json,secrets,subprocess,urllib.request
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('staff',Path(__file__).with_name('setup-php-lab.py'));staff=importlib.util.module_from_spec(spec);spec.loader.exec_module(staff)
staff.config.BASE='http://127.0.0.1:18082/api';sql=staff.config.sql
prefix='it_main_live_'+secrets.token_hex(6)
def literal(value):return 'CONVERT(0x'+value.encode().hex()+' USING utf8mb4)'
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
key='legacy-main-orders'
old=sql("SELECT config_value FROM lin_operational_config WHERE config_key='legacy-main-orders' AND scope_type='GLOBAL' AND scope_id=0")
assert not old or json.loads(old).get('enabled') is False
order=None;group=None;variety=None;hospital=None
try:
 group=int(sql('INSERT INTO lin_lb_kuhuirightdatagroup(GroupName,GroupRemark,StartWord,StartNO,ParentIID,CreateDt,UpdateDt) VALUES('+literal(prefix)+",'','Q-','99999','0',NOW(),NOW());SELECT LAST_INSERT_ID()"))
 variety=int(sql('INSERT INTO lin_lb_infovariety(Variety_CN,Variety_EN,createtime,updatetime) VALUES('+literal(prefix)+",'Breed reference',UNIX_TIMESTAMP(),UNIX_TIMESTAMP());SELECT LAST_INSERT_ID()"))
 hospital=int(sql('INSERT INTO lin_lb_infohospital(HospitalName,HospitalName_en,PayType,amount) VALUES('+literal(prefix)+','+literal(prefix)+',1,0);SELECT LAST_INSERT_ID()'))
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
 sql("INSERT INTO lin_operational_config(config_key,config_value,description,updated_by) VALUES('legacy-main-orders',"+literal(json.dumps({'enabled':True,'writeEnabled':True}))+",'main UI fixture',(SELECT MIN(id) FROM lin_user)) ON DUPLICATE KEY UPDATE config_value=VALUES(config_value)")
 run=subprocess.run(['node','scripts/editor-parity/main-order-choices-live.js'],cwd=ROOT,input=json.dumps({'token':token,'order':order,'prefix':prefix,'group':group,'variety':variety,'hospital':hospital}),capture_output=True,text=True,timeout=120)
 if run.returncode:raise AssertionError('Main order browser failed: '+run.stderr[-1400:])
 proof=json.loads(run.stdout)
 assert sql(f"SELECT CONCAT(AnimalName,'|',MasterName,'|',CollectDt,'|',TestFee,'|',IsPayMoney,'|',SampleStatus) FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId={order}")=='主单修改宠物|主单修改主人|2024-05-06 00:00:00|125.5|1|2'
 assert sql(f"SELECT COUNT(*) FROM lin_audit_record WHERE resource_type='LEGACY_MAIN_ORDER' AND CAST(resource_id AS UNSIGNED)={order}")=='1'
 assert sql(f'SELECT CONCAT(CreateDataTypeIId,"|",AnimalTypeId,"|",InfoHospitalId,"|",AnimalType_en) FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId={order}')==f'{group}|{variety}|{hospital}|Breed reference'
 proof['persistedDatesMoneyPaymentAndStatusVerified']=True
finally:
 if old:sql('UPDATE lin_operational_config SET config_value='+literal(old)+" WHERE config_key='legacy-main-orders' AND scope_type='GLOBAL' AND scope_id=0")
 else:sql("DELETE FROM lin_operational_config WHERE config_key='legacy-main-orders' AND scope_type='GLOBAL' AND scope_id=0 AND description='main UI fixture'")
 if order:
  assert sql(f'SELECT UserNO FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId={order}')==prefix
  sql(f"DELETE FROM lin_audit_record WHERE resource_type='LEGACY_MAIN_ORDER' AND CAST(resource_id AS UNSIGNED)={order}; DELETE FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId={order} AND UserNO="+literal(prefix))
 if hospital:sql(f'DELETE FROM lin_lb_infohospital WHERE InfoHospitalId={hospital} AND HospitalName='+literal(prefix))
 if variety:sql(f'DELETE FROM lin_lb_infovariety WHERE IID={variety} AND Variety_CN='+literal(prefix))
 if group:sql(f'DELETE FROM lin_lb_kuhuirightdatagroup WHERE IID={group} AND GroupName='+literal(prefix))
proof.update(gateRestored=True,fixturesRemoved=True)
print(json.dumps(proof))
