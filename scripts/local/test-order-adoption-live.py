"""Own local Chrome -> Java -> 13306 adoption. No external services, scheduling or PHP changes."""
import importlib.util,json,os,re,secrets,signal,socket,stat,subprocess,time,hashlib
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('staff',Path(__file__).with_name('setup-php-lab.py'));staff=importlib.util.module_from_spec(spec);spec.loader.exec_module(staff);staff.config.BASE='http://127.0.0.1:18082/api';sql=staff.config.sql
prefix='it_adopt_pc_'+secrets.token_hex(5)
def lit(value):return 'CONVERT(0x'+value.encode().hex()+' USING utf8mb4) COLLATE utf8mb4_unicode_ci'
try:
 with socket.create_connection(('127.0.0.1',18082),timeout=.2):raise AssertionError('Dedicated port occupied')
except ConnectionRefusedError:pass
main=sql("SELECT JSON_EXTRACT(config_value,'$.enabled') FROM lin_operational_config WHERE config_key='legacy-main-orders' AND scope_type='GLOBAL' AND scope_id=0 AND status='ACTIVE'");assert main in ('','false'),'Do not override another active Petlis page mode'
keys=['legacy-order-adoption','legacy-order-details'];old={};changed_keys=[]
for key in keys:
 value=sql("SELECT HEX(JSON_OBJECT('config_value',config_value,'status',status,'updated_at',DATE_FORMAT(updated_at,'%Y-%m-%d %H:%i:%s'))) FROM lin_operational_config WHERE config_key="+lit(key)+" AND scope_type='GLOBAL' AND scope_id=0")
 old[key]=json.loads(bytes.fromhex(value).decode()) if value else None
 assert not old[key] or (old[key]['config_value'] if isinstance(old[key]['config_value'],dict) else json.loads(old[key]['config_value'])).get('enabled') is False,'Do not override an active Petlis gate'
private=Path.home()/'.local/share/petlis/qa';private.mkdir(mode=0o700,parents=True,exist_ok=True)
process=None;hospital=doctor=lab=test=order=detail=None;proof={};columns={}
def names(table):
 if table not in columns:
  columns[table]=sql("SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='"+table+"' ORDER BY ORDINAL_POSITION").splitlines();assert columns[table] and all(re.fullmatch('[A-Za-z0-9_]+',n) for n in columns[table])
 return columns[table]
def expression(table):return 'JSON_OBJECT('+','.join("'"+n+"',`"+n+'`' for n in names(table))+')'
def snapshot():
 result={}
 for table in ['lin_lb_infohospital','lin_lb_listdiagnosisproject','lin_lb_diagnosisdetail','lin_lb_diagnosisdetail_info','lin_lb_paylog','lin_report_workflow','lin_user']:
  where=' WHERE ListDiagnosisProjectId<>'+str(order) if table=='lin_lb_listdiagnosisproject' else ''
  rows=sql('SELECT SHA2(CAST('+expression(table)+' AS CHAR),256) FROM '+table+where).splitlines();result[table]=hashlib.sha256('\n'.join(sorted(rows)).encode()).hexdigest()
 return result
def parent():return json.loads(bytes.fromhex(sql('SELECT HEX(CAST('+expression('lin_lb_listdiagnosisproject')+' AS CHAR)) FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId='+str(order))).decode())
try:
 hospital=int(sql('INSERT INTO lin_lb_infohospital(HospitalName,HospitalName_en,PayType,amount,IsActive,CityId) VALUES('+lit(prefix)+','+lit(prefix)+',1,20,1,1);SELECT LAST_INSERT_ID()'))
 doctor=int(sql('INSERT INTO lin_user(username,nickname,status,hospital_id) VALUES('+lit(prefix)+",'合成送检医师','normal',"+str(hospital)+');SELECT LAST_INSERT_ID()'))
 lab=int(sql('INSERT INTO lin_laboratory(name,code,active) VALUES('+lit(prefix)+','+lit(prefix)+',1);SELECT LAST_INSERT_ID()'))
 test=int(sql('INSERT INTO lin_lb_infotesttype(TestName,TestNO,Price,CtoG,GtoC,DiscountReal,DiscountNext,TextModel,CreateDt,type,IsActive) VALUES('+lit(prefix)+','+lit(prefix)+",0,0,0,0,0,'',NOW(),1,1);SELECT LAST_INSERT_ID()"))
 sql(f'INSERT INTO lin_lab_test(laboratory_id,test_type_id,price,active) VALUES({lab},{test},8.25,1)')
 fields=[];values=[]
 for line in sql("SELECT COLUMN_NAME,DATA_TYPE,IS_NULLABLE,IFNULL(COLUMN_DEFAULT,'<NULL>'),EXTRA FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='lin_lb_listdiagnosisproject' ORDER BY ORDINAL_POSITION").splitlines():
  name,kind,nullable,default,extra=(line.split('\t')+['']*5)[:5]
  if 'auto_increment' in extra:continue
  given={'UserNO':lit(prefix),'AnimalName':lit(prefix),'MasterName':lit(prefix),'DoctorName':lit('合成送检医师'),'InfoHospitalId':str(hospital),'SampleStatus':'0','CreateSampleId':'0','TestFee':'0'}
  if name not in given and (nullable=='YES' or default!='<NULL>'):continue
  assert re.fullmatch('[A-Za-z0-9_]+',name);fields.append('`'+name+'`')
  if name in given:value=given[name]
  elif kind in ['int','tinyint','smallint','mediumint','bigint','decimal','double','float']:value='0'
  elif kind in ['datetime','timestamp','date']:value=lit('1900-01-01')
  elif kind in ['varchar','char','text','mediumtext','longtext']:value=lit(prefix)
  else:raise AssertionError('Unexpected fixture field')
  values.append(value)
 order=int(sql('INSERT INTO lin_lb_listdiagnosisproject('+','.join(fields)+') VALUES('+','.join(values)+');SELECT LAST_INSERT_ID()'))
 for key in keys:
  sql('INSERT INTO lin_operational_config(config_key,config_value,description,updated_by) VALUES('+lit(key)+','+lit('{"enabled":true,"writeEnabled":true}')+','+lit(prefix)+','+str(doctor)+") ON DUPLICATE KEY UPDATE config_value=VALUES(config_value),status='ACTIVE'")
  changed_keys.append(key)
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
 mapping=Path.home()/'.local/share/petlis/php-preview/app/.petlis-private/bridge.php';info=mapping.stat();assert stat.S_ISREG(info.st_mode) and info.st_uid==os.getuid() and not info.st_mode&0o077;text=mapping.read_text();credentials={}
 for key in ['username','password']:
  found=re.search(r"'"+key+r"'\s*=>\s*'((?:\\.|[^'])*)'",text);assert found;credentials[key]=found.group(1).replace("\\'","'").replace('\\\\','\\')
 session=staff.call('/auth/password/login',{'account':credentials['username'],'password':credentials['password']});assert 'ADMIN' in session['roles']
 detail=int(staff.call(f'/admin/legacy/order-details/orders/{order}',{'testTypeId':test,'originalPrice':'8.25','originalDiscount':'10','testPrice':'8.25','detailRemark':prefix,'modelId':0,'mailTitle':'owned test','isSendMail':0,'isCheckPass':0,'payStatus':0,'diagnosisStatus':0,'mailReport':'owned mail','reportResult':'owned report','requestKey':prefix+'_detail'},session['token']))
 before=snapshot();parent_before=parent();fixture={'token':session['token'],'order':order,'doctor':doctor,'lab':lab,'beforeScreenshot':str(private/(prefix+'-before.png')),'afterScreenshot':str(private/(prefix+'-after.png'))}
 result=subprocess.run(['node','scripts/editor-parity/legacy-order-adoption-live.js'],cwd=ROOT,input=json.dumps(fixture),capture_output=True,text=True,timeout=100);assert result.returncode==0,'Chrome acceptance failed: '+result.stderr[-1200:];proof=json.loads(result.stdout)
 assert snapshot()==before,'Unrelated clinical, funds, reports or accounts changed';parent_before['CreateSampleId']=doctor;parent_before['TestFee']=8.25;assert parent()==parent_before,'Unexpected parent field mutation'
 assert sql(f'SELECT CONCAT(user_id,\',\',laboratory_id,\',\',CAST(payable_amount AS CHAR)) FROM lin_order_extension WHERE order_id={order}')==f'{doctor},{lab},8.25'
 assert sql("SELECT COUNT(*) FROM lin_audit_record WHERE resource_type='LEGACY_ORDER_ADOPTION_PREPARATION' AND resource_id="+lit(str(order))+' AND request_id='+lit(proof['requestKey']))=='1'
 proof.update(parentOnlyDoctorAndConfirmedTotalChanged=True,originalDetailBodiesAndFundsUnchanged=True,oneAdoptionAudit=True)
finally:
 if process and process.poll() is None:
  os.killpg(process.pid,signal.SIGTERM)
  try:process.wait(timeout=8)
  except subprocess.TimeoutExpired:os.killpg(process.pid,signal.SIGKILL);process.wait(timeout=5)
 if order:
  assert sql(f'SELECT UserNO FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId={order}')==prefix
  sql(f'DELETE FROM lin_order_extension WHERE order_id={order}')
  sql("DELETE FROM lin_audit_record WHERE resource_id="+lit(str(order))+" AND ((resource_type='ORDER' AND action IN ('LEGACY_PREPARED_SUBMITTED','LEGACY_ORDER_ADOPTED')) OR resource_type='LEGACY_ORDER_ADOPTION_PREPARATION')")
 if order:
  owned_details=sql(f'SELECT DiagnosisDetailId FROM lin_lb_diagnosisdetail WHERE ListDiagnosisProjectId={order} AND DetailRemark='+lit(prefix)).splitlines()
  for value in owned_details:
   assert value.isdecimal();owned=int(value)
   sql(f'DELETE FROM lin_lb_diagnosisdetail_info WHERE diagnosisdetail_id={owned};DELETE FROM lin_lb_diagnosisdetail WHERE DiagnosisDetailId={owned} AND ListDiagnosisProjectId={order}')
   sql("DELETE FROM lin_audit_record WHERE resource_type='LEGACY_ORDER_DETAIL' AND resource_id="+lit(str(owned))+' AND request_id='+lit(prefix+'_detail'))
 if order:sql(f'DELETE FROM lin_lb_notification WHERE order_id={order} AND animal_name='+lit(prefix)+f';DELETE FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId={order} AND UserNO='+lit(prefix))
 if lab and test:sql(f'DELETE FROM lin_lab_test WHERE laboratory_id={lab} AND test_type_id={test}')
 if lab:sql(f'DELETE FROM lin_laboratory WHERE id={lab} AND code='+lit(prefix))
 if test:sql(f'DELETE FROM lin_lb_infotesttype WHERE InfoTestTypeId={test} AND TestNO='+lit(prefix))
 for key,previous in old.items():
  if key not in changed_keys:continue
  if previous:sql('UPDATE lin_operational_config SET config_value='+lit(json.dumps(previous['config_value']) if isinstance(previous['config_value'],dict) else previous['config_value'])+',status='+lit(previous['status'])+',updated_at='+lit(previous['updated_at'])+' WHERE config_key='+lit(key)+" AND scope_type='GLOBAL' AND scope_id=0")
  else:sql('DELETE FROM lin_operational_config WHERE config_key='+lit(key)+' AND description='+lit(prefix))
 if doctor:sql(f'DELETE FROM lin_user WHERE id={doctor} AND username='+lit(prefix))
 if hospital:sql(f'DELETE FROM lin_lb_infohospital WHERE InfoHospitalId={hospital} AND HospitalName='+lit(prefix))
proof.update(gatesRestored=True,fixturesRemoved=True,backendStopped=True);print(json.dumps(proof))
