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

import secrets,re
for _ in range(40):
 try:
  with staff.opener.open(staff.config.BASE+'/auth/capabilities',timeout=1) as r:assert r.status==200
  break
 except OSError:time.sleep(.25)
else:raise AssertionError('Backend not ready')
admin=staff.administrator(4);key='legacy-order-details';checkpoint=root/'qa/render-http-private.json'
assert not checkpoint.exists(),'Existing render fixture checkpoint requires review'
prefix='QA_RENDER_'+secrets.token_hex(5);state={'prefix':prefix};staff.save_private(checkpoint,state)
if staff.config.sql("SELECT COUNT(*) FROM lin_operational_config WHERE config_key='legacy-order-details' AND scope_type='GLOBAL' AND scope_id=0")=='0':staff.call('/admin/operations/configs',{'key':key,'value':{'enabled':False,'writeEnabled':False},'description':'原明细与模板迁移，本机受控验收'},admin)
assert staff.config.sql("SELECT JSON_UNQUOTE(JSON_EXTRACT(config_value,'$.enabled')) FROM lin_operational_config WHERE config_key='legacy-order-details' AND scope_type='GLOBAL' AND scope_id=0")=='false'
def gate(enabled):
 version=int(staff.config.sql("SELECT version FROM lin_operational_config WHERE config_key='legacy-order-details' AND scope_type='GLOBAL' AND scope_id=0"))
 assert request('/admin/operations/configs/'+key,admin,{'value':{'enabled':enabled,'writeEnabled':False},'version':version,'reason':'本机全合成模板渲染HTTP验收'},'PUT')['code']==200
proof={'scope':'synthetic local order/templates; public test key only','existingBusinessRowsTouched':False,'linksVisited':False}
try:
 names=[];values=[]
 columns=staff.config.sql("SELECT COLUMN_NAME,DATA_TYPE,IS_NULLABLE,IFNULL(COLUMN_DEFAULT,'__MISSING__'),EXTRA FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='lin_lb_listdiagnosisproject' ORDER BY ORDINAL_POSITION")
 for line in columns.splitlines():
  fields=line.split('\t');fields+=['']*(5-len(fields));name,kind,nullable,default,extra=fields;assert re.fullmatch('[A-Za-z0-9_]+',name)
  if 'auto_increment' in extra or nullable=='YES' or default!='__MISSING__':continue
  names.append('`'+name+'`')
  if kind in ['int','tinyint','smallint','mediumint','bigint','decimal','double','float']:values.append('0')
  elif kind in ['datetime','timestamp']:values.append("'1900-01-01 00:00:00'")
  elif kind=='date':values.append("'1900-01-01'")
  else:assert kind in ['varchar','char','text','mediumtext','longtext'];values.append("'"+prefix+"'")
 staff.config.sql('INSERT INTO lin_lb_listdiagnosisproject('+','.join(names)+') VALUES('+','.join(values)+')')
 order=int(staff.config.sql("SELECT ListDiagnosisProjectId FROM lin_lb_listdiagnosisproject WHERE UserNO='"+prefix+"'"));state['orderId']=order;staff.save_private(checkpoint,state)
 label=prefix+'宠物';report='X{'+label+'}|{报告链接}';mail='X{用户付款链接}|{'+label+'}'
 staff.config.sql("INSERT INTO lin_lb_dictbindkey(BindTable,ColumnName,DescName) VALUES('ListDiagnosisProject','AnimalName','"+label+"')")
 staff.config.sql("INSERT INTO lin_lb_infotesttypemodel(ModelName,ModelInfo,InfoTestTypeId,CreateDt) VALUES('"+prefix+"','"+report+"',0,NOW())")
 report_id=int(staff.config.sql("SELECT InfoTestTypeModelId FROM lin_lb_infotesttypemodel WHERE ModelName='"+prefix+"'"));state['reportId']=report_id;staff.save_private(checkpoint,state)
 staff.config.sql("INSERT INTO lin_lb_dictmaildiagnosisreport(model_type,model_content) VALUES('"+prefix+"','"+mail+"')")
 mail_id=int(staff.config.sql("SELECT id FROM lin_lb_dictmaildiagnosisreport WHERE model_type='"+prefix+"'"));state['mailId']=mail_id;staff.save_private(checkpoint,state)
 gate(True)
 reference=json.loads(Path('docs/php-parity/link-reference.json').read_text());token=next(r['token'] for r in reference['rows'] if r['input']=='0');expected='http://127.0.0.1:8082/api/Project/View/Detail.aspx?CID='+token
 for kind,tid in [('report',report_id),('mail',mail_id)]:
  r=request('/admin/legacy/order-details/orders/'+str(order)+'/render/'+kind+'/'+str(tid),admin);assert r['code']==200;r=r['data']['html'];assert expected in r and prefix in r;assert '{'+label+'}' not in r
  if kind=='mail':assert '附件-报告地址:' in r and '{用户付款链接}' not in r
 assert staff.config.sql("SELECT ModelInfo FROM lin_lb_infotesttypemodel WHERE InfoTestTypeModelId="+str(report_id))==report
 assert staff.config.sql('SELECT COUNT(*) FROM lin_lb_diagnosisdetail WHERE ListDiagnosisProjectId='+str(order))=='0'
 doctor=json.loads((root/'qa/native-sf-history-private.json').read_text());dt=staff.call('/auth/password/login',{'account':doctor['account'],'password':doctor['password']})['token'];assert request('/admin/legacy/order-details/orders/'+str(order)+'/render/report/'+str(report_id),dt)['code']==403
 proof.update(orderId=order,reportId=report_id,mailId=mail_id,reportAndMailHttpRendered=True,originalCipherMatches=True,originalTemplatesUnchanged=True,noDetailCreated=True,doctorDenied=True)
finally:
 gate(False)
 if state.get('orderId'):
  assert staff.config.sql('SELECT UserNO FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId='+str(state['orderId']))==prefix
  staff.config.sql("DELETE FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId="+str(state['orderId'])+" AND UserNO='"+prefix+"'")
 staff.config.sql("DELETE FROM lin_lb_dictbindkey WHERE BindTable='ListDiagnosisProject' AND DescName='"+prefix+"宠物'")
 staff.config.sql("DELETE FROM lin_lb_infotesttypemodel WHERE ModelName='"+prefix+"'")
 staff.config.sql("DELETE FROM lin_lb_dictmaildiagnosisreport WHERE model_type='"+prefix+"'")
 proof['gatesClosedAndOwnFixturesRemoved']=True;state['fixturesRemoved']=True;staff.save_private(checkpoint,state)
state['completed']=True;staff.save_private(checkpoint,state);staff.save_private(root/'qa/render-http.json',proof);print(json.dumps(proof))
