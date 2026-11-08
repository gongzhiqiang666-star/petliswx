"""Profile UI -> real Java -> isolated MySQL. Discovery list only is restricted to our fixture."""
import importlib.util,json,secrets,subprocess,urllib.request
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('staff',Path(__file__).with_name('setup-php-lab.py'));staff=importlib.util.module_from_spec(spec);spec.loader.exec_module(staff)
staff.config.BASE='http://127.0.0.1:18082/api';sql=staff.config.sql
prefix='it_export_live_'+secrets.token_hex(6)
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
key='legacy-main-orders'
old=sql("SELECT config_value FROM lin_operational_config WHERE config_key='legacy-main-orders' AND scope_type='GLOBAL' AND scope_id=0")
assert not old or json.loads(old).get('enabled') is False
old_export=sql("SELECT config_value FROM lin_operational_config WHERE config_key='legacy-order-export' AND scope_type='GLOBAL' AND scope_id=0");assert not old_export or json.loads(old_export).get('enabled') is False
admin=None;orders=[];details=[];test=None
private=Path.home()/'.local/share/petlis/qa';private.mkdir(mode=0o700,parents=True,exist_ok=True)
def seed(name):
 global prefix
 saved=prefix;prefix=name
 try:
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
   return int(sql('INSERT INTO lin_lb_listdiagnosisproject('+','.join(names)+') VALUES('+','.join(values)+');SELECT LAST_INSERT_ID()'))
 finally:prefix=saved
try:
 for suffix in ['allowed','foreign','negative']:
  orders.append(seed(prefix+'_'+suffix))
 group=900000000+orders[0]
 for order,scope,status in [(orders[0],group,2),(orders[1],group+1,2),(orders[2],group,-1)]:
  sql(f'UPDATE lin_lb_listdiagnosisproject SET CreateDataTypeIId={scope},SampleStatus={status} WHERE ListDiagnosisProjectId={order}')
 admin=int(sql("INSERT INTO lin_admin(username,datagroup,status) VALUES("+literal('it_scope_'+str(orders[0]))+','+literal(str(group))+",'normal');SELECT LAST_INSERT_ID()"))
 config={'enabled':True,'writeEnabled':False,'adminMappings':{str(session['userId']):admin}}
 sql("INSERT INTO lin_operational_config(config_key,config_value,description,updated_by) VALUES('legacy-main-orders',"+literal(json.dumps(config))+",'main scope UI fixture',(SELECT MIN(id) FROM lin_user)) ON DUPLICATE KEY UPDATE config_value=VALUES(config_value)")
 sql("INSERT INTO lin_operational_config(config_key,config_value,description,updated_by) VALUES('legacy-order-export','{\"enabled\":true}','order export UI fixture',(SELECT MIN(id) FROM lin_user)) ON DUPLICATE KEY UPDATE config_value=VALUES(config_value)")
 test=int(sql('INSERT INTO lin_lb_infotesttype(TestName,TestNO,Price,CtoG,GtoC,DiscountReal,DiscountNext,TextModel,CreateDt,type,IsActive) VALUES('+literal(prefix)+','+literal(prefix)+",0,0,0,0,0,'',NOW(),0,1);SELECT LAST_INSERT_ID()"))
 detail_columns=[(line.split('\t')+['']*5)[:5] for line in sql("SELECT COLUMN_NAME,DATA_TYPE,IS_NULLABLE,IFNULL(COLUMN_DEFAULT,'<NULL>'),EXTRA FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='lin_lb_diagnosisdetail' ORDER BY ORDINAL_POSITION").splitlines()]
 for order in orders:
  names=[];values=[];given={'ListDiagnosisProjectId':str(order),'InfoTestTypeId':str(test),'TestPrice':'12.50','PayStatus2':'2','IsCheckPass':'0','IsActive':'1'}
  for name,kind,nullable,default,extra in detail_columns:
   if 'auto_increment' in extra:continue
   if name not in given and (nullable=='YES' or default!='<NULL>'):continue
   assert re.fullmatch('[A-Za-z0-9_]+',name);names.append('`'+name+'`')
   if name in given:value=given[name]
   elif kind in ['int','tinyint','smallint','mediumint','bigint','decimal','double','float']:value='0'
   elif kind in ['datetime','timestamp','date']:value=literal('1900-01-01')
   elif kind in ['varchar','char','text','mediumtext','longtext']:value=literal(prefix)
   else:raise AssertionError('Unexpected detail fixture field')
   values.append(value)
  details.append(int(sql('INSERT INTO lin_lb_diagnosisdetail('+','.join(names)+') VALUES('+','.join(values)+');SELECT LAST_INSERT_ID()')))
 sql(f"UPDATE lin_lb_listdiagnosisproject SET ChipNO='000001234567890123456789',DoctorName='=SUM(1,2)' WHERE ListDiagnosisProjectId={orders[0]}")
 before=sql('SELECT ListDiagnosisProjectId,HEX(JSON_OBJECT(\'SampleStatus\',SampleStatus,\'IsPayMoney\',IsPayMoney,\'TestFee\',TestFee,\'ChipNO\',ChipNO,\'DoctorName\',DoctorName)) FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId IN ('+','.join(map(str,orders))+') ORDER BY ListDiagnosisProjectId')
 run=subprocess.run(['node','scripts/editor-parity/order-export-live.js'],cwd=ROOT,input=json.dumps({'token':token,'allowed':orders[0],'excluded':orders[1:],'output':str(private/(prefix+'.xlsx')),'emptyOutput':str(private/(prefix+'-empty.xlsx'))}),capture_output=True,text=True,timeout=120)
 if run.returncode:raise AssertionError('Scoped list browser failed: '+run.stderr[-1400:])
 proof=json.loads(run.stdout)
 import zipfile,xml.etree.ElementTree as ET
 ns={'s':'http://schemas.openxmlformats.org/spreadsheetml/2006/main'}
 def cells(path):
  with zipfile.ZipFile(path) as archive:
   shared=ET.fromstring(archive.read('xl/sharedStrings.xml'));strings=[''.join(node.itertext()) for node in shared.findall('s:si',ns)]
   sheet=ET.fromstring(archive.read('xl/worksheets/sheet1.xml'));result=[]
   for row in sheet.findall('s:sheetData/s:row',ns):
    values={}
    for cell in row.findall('s:c',ns):
     assert cell.find('s:f',ns) is None,'Unexpected spreadsheet formula';value=cell.find('s:v',ns);values[cell.get('r')]=strings[int(value.text)] if cell.get('t')=='s' and value is not None else value.text if value is not None else ''
    result.append(values)
   return result
 rows=cells(private/(prefix+'.xlsx'));assert len(rows)==2 and len(rows[0])==28
 assert rows[1]['A2']==str(orders[0]) and rows[1]['B2']==str(details[0]);assert rows[1]['F2']=='000001234567890123456789';assert rows[1]['X2']=='12.50' and rows[1]['Y2']=='转账';assert rows[1]['AB2']=='=SUM(1,2)';assert rows[1]['L2']=='2024-05-06 00:00:00';assert len(cells(private/(prefix+'-empty.xlsx')))==1
 after=sql('SELECT ListDiagnosisProjectId,HEX(JSON_OBJECT(\'SampleStatus\',SampleStatus,\'IsPayMoney\',IsPayMoney,\'TestFee\',TestFee,\'ChipNO\',ChipNO,\'DoctorName\',DoctorName)) FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId IN ('+','.join(map(str,orders))+') ORDER BY ListDiagnosisProjectId');assert before==after
 proof.update(original28Columns=True,rawDateAndPricePreserved=True,longChipAndFormulaLikeTextPreserved=True,actualWorkbookScopeVerified=True,noBusinessChanges=True)
finally:
 if old_export:sql('UPDATE lin_operational_config SET config_value='+literal(old_export)+" WHERE config_key='legacy-order-export' AND scope_type='GLOBAL' AND scope_id=0")
 else:sql("DELETE FROM lin_operational_config WHERE config_key='legacy-order-export' AND description='order export UI fixture'")
 for order in orders:sql(f'DELETE FROM lin_lb_diagnosisdetail WHERE ListDiagnosisProjectId={order}')
 if test:sql(f'DELETE FROM lin_lb_infotesttype WHERE InfoTestTypeId={test} AND TestName='+literal(prefix))
 if old:sql('UPDATE lin_operational_config SET config_value='+literal(old)+" WHERE config_key='legacy-main-orders' AND scope_type='GLOBAL' AND scope_id=0")
 else:sql("DELETE FROM lin_operational_config WHERE config_key='legacy-main-orders' AND scope_type='GLOBAL' AND scope_id=0 AND description='main scope UI fixture'")
 for order in orders:
  assert sql(f'SELECT UserNO FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId={order}').startswith(prefix+'_')
  sql(f'DELETE FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId={order} AND UserNO LIKE '+literal(prefix+'_%'))
 if admin:sql(f'DELETE FROM lin_admin WHERE id={admin} AND username='+literal('it_scope_'+str(orders[0])))
proof.update(gateRestored=True,fixturesRemoved=True)
print(json.dumps(proof))
