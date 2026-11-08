"""Profile UI -> real Java -> isolated MySQL. Discovery list only is restricted to our fixture."""
import importlib.util,json,secrets,subprocess,urllib.request,os,signal,socket,time
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('staff',Path(__file__).with_name('setup-php-lab.py'));staff=importlib.util.module_from_spec(spec);spec.loader.exec_module(staff)
staff.config.BASE='http://127.0.0.1:18082/api';sql=staff.config.sql
prefix='it_main_note_'+secrets.token_hex(6)
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
key='legacy-main-orders'
old_raw=sql("SELECT HEX(JSON_OBJECT('config_value',config_value,'status',status,'updated_at',DATE_FORMAT(updated_at,'%Y-%m-%d %H:%i:%s'))) FROM lin_operational_config WHERE config_key='legacy-main-orders' AND scope_type='GLOBAL' AND scope_id=0")
old=json.loads(bytes.fromhex(old_raw).decode()) if old_raw else None
assert not old or old['status']!='ACTIVE' or (old['config_value'] if isinstance(old['config_value'],dict) else json.loads(old['config_value'])).get('enabled') is False
try:
 with socket.create_connection(('127.0.0.1',18082),timeout=.2):raise AssertionError('Dedicated port occupied; do not stop another process')
except ConnectionRefusedError:pass
process=None;proof={}
private=Path.home()/'.local/share/petlis/qa';private.mkdir(mode=0o700,parents=True,exist_ok=True)
image='data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jGJkAAAAASUVORK5CYII='
html='<p><strong>合成送检备注</strong></p><table border="1"><tbody><tr><td colspan="2"><p>样本冷藏</p></td></tr></tbody></table><p><img src="'+image+'" width="12" height="12" alt="合成示意图"></p>'
order=None
try:
 command='set -a; source "$HOME/.local/share/petlis/mysql/backend.env"; set +a; [[ "$DB_URL" == "jdbc:mysql://127.0.0.1:13306/laboklin?"* ]] || exit 17; export JAVA_HOME="$HOME/.local/share/petlis/tools/Home" MAVEN_HOME="$HOME/.local/share/petlis/tools/libexec" APP_SCHEDULING_ENABLED=false DB_MIGRATIONS_ENABLED=false PDF_STORAGE_PATH="$HOME/.local/share/petlis/reports"; exec ./petlis-backend/mvnw -o -q -f petlis-backend/pom.xml spring-boot:run -Dspring-boot.run.profiles=dev -Dspring-boot.run.arguments=--server.port=18082'
 fd=os.open(private/(prefix+'.log'),os.O_WRONLY|os.O_CREAT|os.O_EXCL|os.O_NOFOLLOW,0o600)
 with os.fdopen(fd,'w') as log:process=subprocess.Popen(['/bin/zsh','-c',command],cwd=ROOT,stdout=log,stderr=subprocess.STDOUT,start_new_session=True)
 for attempt in range(60):
  assert process.poll() is None,'Owned backend exited'
  try:
   with staff.opener.open(staff.config.BASE+'/auth/capabilities',timeout=1) as response:
    if response.status==200:break
  except Exception:time.sleep(.5)
 else:raise AssertionError('Backend readiness timeout')
 session=staff.call('/auth/password/login',{'account':credentials['username'],'password':credentials['password']});assert 'ADMIN' in session['roles'];token=session['token']
 columns=[line.split('\t') for line in sql("SELECT COLUMN_NAME,DATA_TYPE,IS_NULLABLE,IFNULL(COLUMN_DEFAULT,'<NULL>'),EXTRA FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='lin_lb_listdiagnosisproject' ORDER BY ORDINAL_POSITION").splitlines()]
 names=[];values=[]
 for parts in columns:
  name,kind,nullable,default,extra=(parts+['']*5)[:5]
  if 'auto_increment' in extra:continue
  if name not in ['UserNO','AnimalName','MasterName','CollectDt','SampleStatus','TestFee','IsPayMoney','Remark'] and (nullable=='YES' or default!='<NULL>'):continue
  assert name.replace('_','').isalnum();names.append('`'+name+'`')
  if name in ['UserNO','AnimalName','MasterName']:value=literal(prefix)
  elif name=='CollectDt':value=literal('2024-05-06 00:00:00')
  elif name=='SampleStatus':value='2'
  elif name=='TestFee':value='125.50'
  elif name=='IsPayMoney':value='1'
  elif name=='Remark':value=literal(html)
  elif kind in ['int','tinyint','smallint','mediumint','bigint','decimal','double','float']:value='0'
  elif kind in ['datetime','timestamp']:value=literal('1900-01-01 00:00:00')
  elif kind=='date':value=literal('1900-01-01')
  elif kind in ['varchar','char','text','mediumtext','longtext']:value=literal(prefix)
  else:raise AssertionError('Unsupported fixture schema')
  values.append(value)
 order=int(sql('INSERT INTO lin_lb_listdiagnosisproject('+','.join(names)+') VALUES('+','.join(values)+');SELECT LAST_INSERT_ID()'))
 sql("INSERT INTO lin_operational_config(config_key,config_value,description,updated_by) VALUES('legacy-main-orders',"+literal(json.dumps({'enabled':True,'writeEnabled':True}))+",'main remark UI fixture',(SELECT MIN(id) FROM lin_user)) ON DUPLICATE KEY UPDATE config_value=VALUES(config_value),status='ACTIVE'")
 parent_columns=sql("SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='lin_lb_listdiagnosisproject' ORDER BY ORDINAL_POSITION").splitlines()
 assert all(re.fullmatch('[A-Za-z0-9_]+',n) for n in parent_columns)
 expression='JSON_OBJECT('+','.join("'"+n+"',`"+n+'`' for n in parent_columns)+')'
 def parent():return json.loads(bytes.fromhex(sql('SELECT HEX(CAST('+expression+' AS CHAR)) FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId='+str(order))).decode())
 before=parent()
 run=subprocess.run(['node','scripts/editor-parity/main-order-remark-live.js'],cwd=ROOT,input=json.dumps({'token':token,'order':order,'prefix':prefix,'image':image,'screenshot':str(private/(prefix+'.png'))}),capture_output=True,text=True,timeout=120)
 if run.returncode:raise AssertionError('Main order browser failed: '+run.stderr[-1400:])
 proof=json.loads(run.stdout)
 assert sql(f"SELECT CONCAT(AnimalName,'|',MasterName,'|',CollectDt,'|',TestFee,'|',IsPayMoney,'|',SampleStatus) FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId={order}")=='合成备注验收动物|'+prefix+'|2024-05-06 00:00:00|125.5|1|2'
 assert sql(f"SELECT COUNT(*) FROM lin_audit_record WHERE resource_type='LEGACY_MAIN_ORDER' AND CAST(resource_id AS UNSIGNED)={order}")=='2'
 saved_html=proof.pop('html');assert sql(f'SELECT HEX(Remark) FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId={order}')==saved_html.encode().hex().upper()
 before.update(AnimalName='合成备注验收动物',Remark=saved_html);assert parent()==before,'Other parent fields changed'
 assert sql("SELECT COUNT(*) FROM lin_audit_record WHERE resource_type='LEGACY_MAIN_ORDER' AND resource_id="+literal(str(order))+" AND request_id="+literal(proof['requestKey']))=='1'
 proof['persistedDatesMoneyPaymentStatusAndHtmlVerified']=True
finally:
 if process and process.poll() is None:
  os.killpg(process.pid,signal.SIGTERM)
  try:process.wait(timeout=8)
  except subprocess.TimeoutExpired:os.killpg(process.pid,signal.SIGKILL);process.wait(timeout=5)
 if old:
  value=old['config_value'];value=json.dumps(value,ensure_ascii=False) if isinstance(value,dict) else value
  sql('UPDATE lin_operational_config SET config_value='+literal(value)+',status='+literal(old['status'])+',updated_at='+literal(old['updated_at'])+" WHERE config_key='legacy-main-orders' AND scope_type='GLOBAL' AND scope_id=0")
 else:sql("DELETE FROM lin_operational_config WHERE config_key='legacy-main-orders' AND scope_type='GLOBAL' AND scope_id=0 AND description='main remark UI fixture'")
 if order:
  assert sql(f'SELECT UserNO FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId={order}')==prefix
  sql(f"DELETE FROM lin_lb_notification WHERE order_id={order};DELETE FROM lin_audit_record WHERE resource_type='LEGACY_MAIN_ORDER' AND CAST(resource_id AS UNSIGNED)={order}; DELETE FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId={order} AND UserNO="+literal(prefix))
assert sql("SELECT HEX(JSON_OBJECT('config_value',config_value,'status',status,'updated_at',DATE_FORMAT(updated_at,'%Y-%m-%d %H:%i:%s'))) FROM lin_operational_config WHERE config_key='legacy-main-orders' AND scope_type='GLOBAL' AND scope_id=0")==old_raw
try:
 with socket.create_connection(('127.0.0.1',18082),timeout=.2):raise AssertionError('Owned backend port still open')
except ConnectionRefusedError:pass
assert not sql('SELECT ListDiagnosisProjectId FROM lin_lb_listdiagnosisproject WHERE UserNO='+literal(prefix))
proof.update(gateRestored=True,fixturesRemoved=True,backendStopped=True)
print(json.dumps(proof))
