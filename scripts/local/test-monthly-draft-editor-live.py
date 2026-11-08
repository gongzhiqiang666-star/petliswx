"""Monthly draft editing -> real Java PDF -> isolated local MySQL; synthetic fixture only.
Requires the owned QA backend on18082 with PDF engine configured; it does not start shared services.
"""
import importlib.util,json,secrets,subprocess,urllib.request
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('staff',Path(__file__).with_name('setup-php-lab.py'));staff=importlib.util.module_from_spec(spec);spec.loader.exec_module(staff)
staff.config.BASE='http://127.0.0.1:18082/api';sql=staff.config.sql
prefix='it_monthly_edit_'+secrets.token_hex(6)
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
pdf_enabled=os.environ.get('PETLIS_QA_MONTHLY_PDF')=='true'
for key in ['legacy-monthly-reports','legacy-order-details']+(['legacy-html-pdf'] if pdf_enabled else []):
 value=sql('SELECT config_value FROM lin_operational_config WHERE config_key='+literal(key)+" AND scope_type='GLOBAL' AND scope_id=0")
 assert not value or json.loads(value).get('enabled') is False
 old[key]=value
fd,journal=tempfile.mkstemp(prefix='monthly-',suffix='.json',dir=private);os.close(fd)
order=None;hospital=None;test=None;ids=[];report=None;mail=None
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

 sql(f'UPDATE lin_lb_listdiagnosisproject SET InfoHospitalId={hospital},ReceiveSampleDt="2024-02-29 12:00:00",SampleStatus=2 WHERE ListDiagnosisProjectId={order}')
 for key in old:
  sql("INSERT INTO lin_operational_config(config_key,config_value,description,updated_by) VALUES("+literal(key)+','+literal(json.dumps({'enabled':True,'writeEnabled':True}))+",'monthly UI fixture',(SELECT MIN(id) FROM lin_user)) ON DUPLICATE KEY UPDATE config_value=VALUES(config_value)")
 for index,(price,exempt,paid) in enumerate([('12.50',0,0),('7.25',0,2),('4.00',0,4),('50.00',1,0)]):
  command={'testTypeId':test,'originalPrice':price,'originalDiscount':'10','testPrice':price,'detailRemark':prefix,'modelId':0,'mailTitle':'合成财务测试','isSendMail':0,'isCheckPass':exempt,'payStatus':paid,'diagnosisStatus':0,'mailReport':'<p>mail original</p>','reportResult':'<p>report original</p>','requestKey':prefix+'_detail_'+str(index)}
  ids.append(int(staff.call(f'/admin/legacy/order-details/orders/{order}',command,token)))
 report=int(sql('INSERT INTO lin_lb_dictfinancereport(model_name,model_content) VALUES('+literal(prefix)+','+literal('<h1>{结算单位}</h1><p>{结算月份} {结束时间} 总费用 {总费用} {总费用大写}</p>{表格内容}')+');SELECT LAST_INSERT_ID()'))
 mail=int(sql('INSERT INTO lin_lb_dictfinancemailreport(model_name,model_content,IsActive) VALUES('+literal(prefix)+','+literal('<p>邮件 {总费用}</p>')+',0);SELECT LAST_INSERT_ID()'))
 run=subprocess.run(['node','scripts/editor-parity/legacy-monthly-report-live.js'],cwd=ROOT,input=json.dumps({'token':token,'prefix':prefix,'hospital':hospital,'report':report,'mail':mail,'editor':True,'pdf':pdf_enabled,'output':str(private/(prefix+'.pdf')),'screenshot':str(private/(prefix+'.png'))}),capture_output=True,text=True,timeout=120)
 if run.returncode:raise AssertionError('Monthly browser failed: '+run.stderr[-1400:])
 proof=json.loads(run.stdout)
 if pdf_enabled:
  text=subprocess.run(['pdftotext',str(private/(prefix+'.pdf')),'-'],capture_output=True,text=True,check=True).stdout
  assert 'REPORT_EDIT_END' in text and '12.50' in text and 'MAIL_EDIT_ONLY' not in text
  # Currency can wrap within a narrow source cell; verify that cell by its x position,
  # instead of treating interleaved neighboring date text as lost money digits.
  import xml.etree.ElementTree as ET
  bbox=subprocess.run(['pdftotext','-bbox',str(private/(prefix+'.pdf')),'-'],capture_output=True,text=True,check=True).stdout
  tree=ET.fromstring(bbox);ns={'x':'http://www.w3.org/1999/xhtml'};amounts=[]
  for page in tree.findall('.//x:page',ns):
   words=page.findall('x:word',ns);money=[w for w in words if '￥' in (w.text or '')]
   if money:
    x=float(money[0].get('xMin'));amounts.append(''.join(w.text or '' for w in words if abs(float(w.get('xMin'))-x)<12))
  assert sorted(amounts)==sorted(['￥12.50','￥7.25','￥4.00','￥50.00']),amounts
  proof['pdfTextMatchesEditedBillAndAmounts']=True
 assert sql(f'SELECT COUNT(*) FROM lin_lb_noticemail WHERE HospId={hospital}')=='0'
 assert sql(f'SELECT SampleStatus FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId={order}')=='2'
 assert sql(f'SELECT amount FROM lin_lb_infohospital WHERE InfoHospitalId={hospital}')=='20.00'
 proof['noNoticeOrParentOrBalanceChanges']=True
finally:
 for key,value in old.items():
  if value:sql('UPDATE lin_operational_config SET config_value='+literal(value)+' WHERE config_key='+literal(key)+" AND scope_type='GLOBAL' AND scope_id=0")
  else:sql('DELETE FROM lin_operational_config WHERE config_key='+literal(key)+" AND scope_type='GLOBAL' AND scope_id=0 AND description='monthly UI fixture'")
 if order:
  assert sql(f'SELECT UserNO FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId={order}')==prefix
  # Recover a creation whose response might have been interrupted, using this fixture's known request keys.
  for index in range(4):
   audit=sql(f"SELECT HEX(after_json) FROM lin_audit_record WHERE actor_user_id={int(session['userId'])} AND resource_type='LEGACY_ORDER_DETAIL' AND request_id="+literal(prefix+'_detail_'+str(index)))
   if audit:
    id=int(json.loads(bytes.fromhex(audit).decode())['detailId']);assert sql(f'SELECT ListDiagnosisProjectId FROM lin_lb_diagnosisdetail WHERE DiagnosisDetailId={id}')==str(order)
    sql(f'DELETE FROM lin_lb_diagnosisdetail_info WHERE diagnosisdetail_id={id};DELETE FROM lin_lb_diagnosisdetail WHERE DiagnosisDetailId={id} AND ListDiagnosisProjectId={order}')
    sql(f"DELETE FROM lin_audit_record WHERE actor_user_id={int(session['userId'])} AND resource_type='LEGACY_ORDER_DETAIL' AND request_id="+literal(prefix+'_detail_'+str(index)))
  sql(f'DELETE FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId={order} AND UserNO='+literal(prefix))

 if hospital:sql(f'DELETE FROM lin_lb_infohospital WHERE InfoHospitalId={hospital} AND HospitalName='+literal(prefix))
 if test:sql(f'DELETE FROM lin_lb_infotesttype WHERE InfoTestTypeId={test} AND TestName='+literal(prefix))
 if report:sql(f'DELETE FROM lin_lb_dictfinancereport WHERE id={report} AND model_name='+literal(prefix))
 if mail:sql(f'DELETE FROM lin_lb_dictfinancemailreport WHERE id={mail} AND model_name='+literal(prefix))
 Path(journal).unlink()
proof.update(gatesRestored=True,fixturesRemoved=True)
print(json.dumps(proof))
