"""Synthetic order only, real Java + admin UI; restore flag and remove only our fixture rows."""
import importlib.util,json,secrets,subprocess,urllib.request,urllib.error
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('staff',Path(__file__).with_name('setup-php-lab.py'));staff=importlib.util.module_from_spec(spec);spec.loader.exec_module(staff)
BASE='http://127.0.0.1:18082/api';staff.config.BASE=BASE
sql=staff.config.sql
prefix='it_detail_ui_'+secrets.token_hex(6)
def literal(value):
 if value is None:return 'NULL'
 if isinstance(value,int):return str(value)
 return 'CONVERT(0x'+str(value).encode().hex()+' USING utf8mb4)'
def request(path,token,data=None,method='GET'):
 req=urllib.request.Request(BASE+path,data=None if data is None else json.dumps(data).encode(),headers={'Content-Type':'application/json','Authorization':'Bearer '+token},method=method)
 with staff.opener.open(req,timeout=30) as response:
  value=json.load(response)
 if value.get('code')!=200:raise AssertionError('Local API did not complete; details withheld')
 return value['data']
# Credentials read from this project's private existing mapping, never persisted or printed.
token=staff.administrator(4)
existing=sql("SELECT config_value FROM lin_operational_config WHERE config_key='legacy-order-details' AND scope_type='GLOBAL' AND scope_id=0 AND status='ACTIVE'")
assert existing,'Existing capability configuration required'
old=json.loads(existing);assert old.get('enabled') is False,'Refuse to overwrite an already enabled session'
actor=int(sql("SELECT user_id FROM lin_user_role WHERE role_code='ADMIN' AND is_active=1 ORDER BY id LIMIT 1"));hospital=int(sql('SELECT InfoHospitalId FROM lin_lb_infohospital ORDER BY InfoHospitalId LIMIT 1'))
order=test=model=None
try:
 columns=[line.split('\t') for line in sql("SELECT COLUMN_NAME,DATA_TYPE,IS_NULLABLE,IFNULL(COLUMN_DEFAULT,'<NULL>'),EXTRA FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='lin_lb_listdiagnosisproject' ORDER BY ORDINAL_POSITION").splitlines()]
 names=[];values=[]
 for parts in columns:
  name,kind,nullable,default,extra=(parts+['']*5)[:5]
  if 'auto_increment' in extra:continue
  if name not in ['UserNO','CreateSampleId','InfoHospitalId'] and (nullable=='YES' or default!='<NULL>'):continue
  assert name.replace('_','').isalnum()
  names.append('`'+name+'`')
  if name=='UserNO':value=prefix
  elif name=='CreateSampleId':value=actor
  elif name=='InfoHospitalId':value=hospital
  elif kind in ['int','tinyint','smallint','mediumint','bigint','decimal','double','float']:value=0
  elif kind in ['datetime','timestamp']:value='1900-01-01 00:00:00'
  elif kind=='date':value='1900-01-01'
  elif kind in ['varchar','char','text','mediumtext','longtext']:value=prefix
  else:raise AssertionError('Unsupported fixture field')
  values.append(literal(value))
 order=int(sql('INSERT INTO lin_lb_listdiagnosisproject('+','.join(names)+') VALUES('+','.join(values)+'); SELECT LAST_INSERT_ID()'))
 test=int(sql("INSERT INTO lin_lb_infotesttype(TestName,TestNO,Price,CtoG,GtoC,DiscountReal,DiscountNext,TextModel,CreateDt,type,IsActive) VALUES("+literal(prefix)+','+literal(prefix)+",0,0,0,0,0,'',NOW(),0,1);SELECT LAST_INSERT_ID()"))
 sql(f'INSERT INTO lin_lb_infotesttype_price(InfoTestTypeid,price,IsActive) VALUES({test},200.00,1)')
 model=int(sql('INSERT INTO lin_lb_infotesttypemodel(ModelName,ModelInfo,InfoTestTypeId,CreateDt) VALUES('+literal(prefix)+','+literal('<p>合成验收报告</p><table><tr><td>检测值</td><td>正常</td></tr></table>')+f',{test},NOW());SELECT LAST_INSERT_ID()'))
 sql("UPDATE lin_operational_config SET config_value="+literal(json.dumps({'enabled':True,'writeEnabled':True,'updateEnabled':True}))+" WHERE config_key='legacy-order-details' AND scope_type='GLOBAL' AND scope_id=0")
 run=subprocess.run(['node','scripts/editor-parity/admin-detail-live.js'],cwd=ROOT,input=json.dumps({'token':token,'prefix':prefix,'orderId':order,'testId':test,'templateId':model}),text=True,capture_output=True,timeout=150)
 if run.returncode:raise AssertionError('Live browser acceptance failed: '+run.stderr[-1600:])
 proof=json.loads(run.stdout);row=request(f'/admin/legacy/order-details/orders/{order}/{proof["detailId"]}',token)
 assert row['TestPrice']=='175.25' and '首次保存结果' in row['ReportResult'] and '修改保存结果' in row['ReportResult']
 assert sql(f'SELECT SampleStatus FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId={order}')=='0'
 assert row['PDFHttpUrl'].startswith(f'/Content/FilePdf/{order}_{proof["detailId"]}/')
 proof.update(persistedPriceAndBodyVerified=True,emptySendDateKeepsParent=True,pdfPathCallback=True,actualPdfFileVerified=False)
finally:
 sql('UPDATE lin_operational_config SET config_value='+literal(existing)+" WHERE config_key='legacy-order-details' AND scope_type='GLOBAL' AND scope_id=0")
 if order:
  assert sql(f'SELECT UserNO FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId={order}')==prefix
  sql(f'DELETE FROM lin_lb_diagnosisdetail_info WHERE diagnosisdetail_id IN (SELECT DiagnosisDetailId FROM lin_lb_diagnosisdetail WHERE ListDiagnosisProjectId={order}); DELETE FROM lin_audit_record WHERE resource_type=\'LEGACY_ORDER_DETAIL\' AND CAST(resource_id AS UNSIGNED) IN (SELECT DiagnosisDetailId FROM lin_lb_diagnosisdetail WHERE ListDiagnosisProjectId={order}); DELETE FROM lin_lb_diagnosisdetail WHERE ListDiagnosisProjectId={order}; DELETE FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId={order} AND UserNO='+literal(prefix))
 if model:sql(f'DELETE FROM lin_lb_infotesttypemodel WHERE InfoTestTypeModelId={model} AND ModelName='+literal(prefix))
 if test:sql(f'DELETE FROM lin_lb_infotesttype_price WHERE InfoTestTypeid={test}; DELETE FROM lin_lb_infotesttype WHERE InfoTestTypeId={test} AND TestNO='+literal(prefix))
 assert sql("SELECT config_value FROM lin_operational_config WHERE config_key='legacy-order-details' AND scope_type='GLOBAL' AND scope_id=0")==existing
proof.update(gatesRestored=True,syntheticFixturesRemoved=True)
print(json.dumps(proof,ensure_ascii=False))
