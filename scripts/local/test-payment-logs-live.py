"""Actual PC -> Java -> isolated DB payment-log maintenance. No provider calls or money posting."""
import importlib.util,json,secrets,subprocess,socket,time,os,signal,re,stat,hashlib,sys,shutil
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('staff',Path(__file__).with_name('setup-php-lab.py'));staff=importlib.util.module_from_spec(spec);spec.loader.exec_module(staff)
staff.config.BASE='http://127.0.0.1:18082/api';sql=staff.config.sql
prefix='it_paylog_live_'+secrets.token_hex(5)
import_mode='--import' in sys.argv
batch_mode='--batch' in sys.argv
query_mode='--query' in sys.argv
assert sum([import_mode,batch_mode,query_mode])<=1
def literal(value):return 'CONVERT(0x'+value.encode().hex()+' USING utf8mb4) COLLATE utf8mb4_unicode_ci'
try:
 with socket.create_connection(('127.0.0.1',18082),timeout=.2):raise AssertionError('Dedicated test port already in use')
except ConnectionRefusedError:pass
old=sql("SELECT HEX(JSON_OBJECT('config_value',config_value,'status',status,'updated_at',DATE_FORMAT(updated_at,'%Y-%m-%d %H:%i:%s'))) FROM lin_operational_config WHERE config_key='legacy-payment-logs' AND scope_type='GLOBAL' AND scope_id=0")
old=json.loads(bytes.fromhex(old).decode()) if old else None
assert not old or json.loads(old['config_value']).get('enabled') is False,'Do not override an enabled business gate'
def snapshot(table):
 assert table in ['lin_lb_infohospital','lin_lb_listdiagnosisproject','lin_lb_diagnosisdetail','lin_lb_payordermain','lin_lb_paylog']
 columns=sql("SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='"+table+"' ORDER BY ORDINAL_POSITION").splitlines()
 assert columns and all(re.fullmatch('[A-Za-z0-9_]+',c) for c in columns)
 rows=sql('SELECT SHA2(CAST(JSON_OBJECT('+','.join("'"+c+"',`"+c+'`' for c in columns)+') AS CHAR),256) FROM '+table+(' WHERE TradeNoUser<>'+literal(prefix) if table=='lin_lb_paylog' else '')).splitlines()
 return hashlib.sha256('\n'.join(sorted(rows)).encode()).hexdigest()
tables=['lin_lb_infohospital','lin_lb_listdiagnosisproject','lin_lb_diagnosisdetail','lin_lb_payordermain','lin_lb_paylog'];before={t:snapshot(t) for t in tables}
process=None;fixture_dir=None;actor=None;private=Path.home()/'.local/share/petlis/qa';private.mkdir(mode=0o700,parents=True,exist_ok=True)
try:
 if import_mode:
  fixture_dir=private/(prefix+'-files');fixture_dir.mkdir(mode=0o700)
  env=os.environ.copy();env.update(JAVA_HOME=str(Path.home()/'.local/share/petlis/tools/Home'),MAVEN_HOME=str(Path.home()/'.local/share/petlis/tools/libexec'))
  cp=fixture_dir/'classpath.txt'
  built=subprocess.run(['./petlis-backend/mvnw','-o','-q','-f','petlis-backend/pom.xml','dependency:build-classpath','-Dmdep.outputFile='+str(cp)],cwd=ROOT,env=env,capture_output=True,text=True,timeout=45);assert built.returncode==0,'Fixture classpath generation failed'
  made=subprocess.run([str(Path(env['JAVA_HOME'])/'bin/java'),'--class-path',cp.read_text().strip(),'scripts/local/PaymentLogImportFixtures.java',str(fixture_dir),prefix],cwd=ROOT,capture_output=True,text=True,timeout=30);assert made.returncode==0,'Synthetic workbook generation failed: '+made.stderr[-500:]
 sql("INSERT INTO lin_operational_config(config_key,config_value,description,updated_by) VALUES('legacy-payment-logs','{\"enabled\":true,\"writeEnabled\":true,\"deleteEnabled\":true,\"importEnabled\":true,\"batchEnabled\":true,\"exportEnabled\":true}','payment log UI fixture',(SELECT MIN(id) FROM lin_user)) ON DUPLICATE KEY UPDATE config_value=VALUES(config_value),status='ACTIVE'")
 command='set -a; source "$HOME/.local/share/petlis/mysql/backend.env"; set +a; [[ "$DB_URL" == "jdbc:mysql://127.0.0.1:13306/laboklin?"* ]] || exit 17; export JAVA_HOME="$HOME/.local/share/petlis/tools/Home" MAVEN_HOME="$HOME/.local/share/petlis/tools/libexec" APP_SCHEDULING_ENABLED=false DB_MIGRATIONS_ENABLED=false PDF_STORAGE_PATH="$HOME/.local/share/petlis/reports"; exec ./petlis-backend/mvnw -o -q -f petlis-backend/pom.xml spring-boot:run -Dspring-boot.run.profiles=dev -Dspring-boot.run.arguments=--server.port=18082'
 fd=os.open(private/(prefix+'.log'),os.O_WRONLY|os.O_CREAT|os.O_EXCL|os.O_NOFOLLOW,0o600)
 with os.fdopen(fd,'w') as log:process=subprocess.Popen(['/bin/zsh','-c',command],cwd=ROOT,stdout=log,stderr=subprocess.STDOUT,start_new_session=True)
 for attempt in range(60):
  assert process.poll() is None,'Dedicated backend exited; see private log'
  try:
   with staff.opener.open(staff.config.BASE+'/auth/capabilities',timeout=1) as response:
    if response.status==200 and json.load(response).get('code')==200:break
  except Exception:time.sleep(.5)
 else:raise AssertionError('Dedicated backend readiness timeout')
 mapping=Path.home()/'.local/share/petlis/php-preview/app/.petlis-private/bridge.php';info=mapping.stat();assert stat.S_ISREG(info.st_mode) and info.st_uid==os.getuid() and not info.st_mode&0o077
 text=mapping.read_text();credentials={}
 for key in ['username','password']:
  match=re.search(r"'"+key+r"'\s*=>\s*'((?:\\.|[^'])*)'",text);assert match;credentials[key]=match.group(1).replace("\\'","'").replace('\\\\','\\')
 session=staff.call('/auth/password/login',{'account':credentials['username'],'password':credentials['password']});assert 'ADMIN' in session['roles'];actor=int(session['userId'])
 run=subprocess.run(['node','scripts/editor-parity/payment-log-query-live.js' if query_mode else 'scripts/editor-parity/payment-log-batch-live.js' if batch_mode else 'scripts/editor-parity/payment-log-import-live.js' if import_mode else 'scripts/editor-parity/payment-logs-live.js'],cwd=ROOT,input=json.dumps({'token':session['token'],'prefix':prefix,'screenshot':str(private/(prefix+'.png')),'fixtureDir':str(fixture_dir) if fixture_dir else None,'exportBase':str(private/prefix)}),capture_output=True,text=True,timeout=90)
 assert run.returncode==0,'Browser failed: '+run.stderr[-1200:]
 proof=json.loads(run.stdout)
 assert sql('SELECT COUNT(*) FROM lin_lb_paylog WHERE TradeNoUser='+literal(prefix))==('23' if query_mode else '4' if import_mode else '0')
 assert sql("SELECT COUNT(*) FROM lin_audit_record WHERE actor_user_id="+str(session['userId'])+" AND resource_type='LEGACY_PAYMENT_LOG' AND JSON_EXTRACT(after_json,'$.canonicalRequest.command.fields.TradeNoUser')="+literal(prefix))==('23' if query_mode else '6' if import_mode else '4' if batch_mode else '2')
 assert before=={t:snapshot(t) for t in tables},'Unrelated business records changed'
 if query_mode:
  import openpyxl
  def read_export(suffix):
   file=private/(prefix+suffix+'.xlsx');assert file.is_file() and not file.is_symlink()
   with file.open('rb') as source:wb=openpyxl.load_workbook(source,read_only=True,data_only=False);sheet=wb.worksheets[0];rows=list(sheet.iter_rows());wb.close()
   assert rows[0][0].value=='PayLogId';return rows
  filtered=read_export('-filtered');draft=read_export('-draft');full=read_export('-full')
  assert len(filtered)==22 and len(draft)==22 and len(full)==24
  assert [r[5].value for r in filtered[1:]]==[str(i)+'.00' for i in range(21)]
  assert [[c.value for c in row] for row in draft]==[[c.value for c in row] for row in filtered]
  assert full[1][5].value=='9999999999999999.99' and full[1][3].value=='000123' and full[1][7].value=='=1+1' and full[1][7].data_type=='s'
  assert all(r[4].value==prefix for r in full[1:])
  proof.update(downloadedWorkbooksReadBack=True,allPagesExported=True,appliedQueryPreserved=True,exactStringValuesVerified=True)
 proof.update(unrelatedBusinessRecordsUnchanged=True)
finally:
 if process and process.poll() is None:
  os.killpg(process.pid,signal.SIGTERM)
  try:process.wait(timeout=8)
  except subprocess.TimeoutExpired:os.killpg(process.pid,signal.SIGKILL);process.wait(timeout=5)
 # Select only records/audits owning this unpredictable test marker, including failures before UI deletion.
 ids=sql("SELECT resource_id FROM lin_audit_record WHERE resource_type='LEGACY_PAYMENT_LOG' AND JSON_EXTRACT(after_json,'$.canonicalRequest.command.fields.TradeNoUser')="+literal(prefix)).splitlines()
 if actor is not None and (import_mode or batch_mode):
  owned=set(ids)
  groups=sql("SELECT id,HEX(after_json) FROM lin_audit_record WHERE resource_type IN ('LEGACY_PAYMENT_LOG_IMPORT','LEGACY_PAYMENT_LOG_BATCH') AND actor_user_id="+str(actor)).splitlines()
  for line in groups:
   audit,encoded=line.split('\t');assert audit.isdecimal() and int(audit)>0
   result=json.loads(bytes.fromhex(encoded).decode());batch_ids={str(i) for i in result.get('result',{}).get('ids',[])}
   if batch_ids and batch_ids<=owned:sql("DELETE FROM lin_audit_record WHERE resource_type IN ('LEGACY_PAYMENT_LOG_IMPORT','LEGACY_PAYMENT_LOG_BATCH') AND id="+audit+" AND actor_user_id="+str(actor))
 for value in ids:
  assert value.isdecimal() and int(value)>0
  sql("DELETE FROM lin_audit_record WHERE resource_type='LEGACY_PAYMENT_LOG' AND resource_id="+literal(value))
 sql('DELETE FROM lin_lb_paylog WHERE TradeNoUser='+literal(prefix))
 if old:sql('UPDATE lin_operational_config SET config_value='+literal(old['config_value'])+',status='+literal(old['status'])+',updated_at='+literal(old['updated_at'])+" WHERE config_key='legacy-payment-logs' AND scope_type='GLOBAL' AND scope_id=0")
 else:sql("DELETE FROM lin_operational_config WHERE config_key='legacy-payment-logs' AND description='payment log UI fixture'")
 if fixture_dir and fixture_dir.exists():
  assert fixture_dir.parent==private and fixture_dir.name==prefix+'-files' and not fixture_dir.is_symlink();shutil.rmtree(fixture_dir)
proof.update(gateRestored=True,fixturesRemoved=True,backendStopped=True)
print(json.dumps(proof))
