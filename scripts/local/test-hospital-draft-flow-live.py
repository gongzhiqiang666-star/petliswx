"""Own doctor/hospitals/drafts on local13306, real browser, exact cleanup and own backend only."""
import importlib.util,json,os,re,secrets,signal,socket,subprocess,time,hashlib
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('staff',Path(__file__).with_name('setup-php-lab.py'));staff=importlib.util.module_from_spec(spec);spec.loader.exec_module(staff);staff.config.BASE='http://127.0.0.1:18082/api';sql=staff.config.sql
prefix='it_hflow_pc_'+secrets.token_hex(5)
def lit(s):return ("CONVERT('' USING utf8mb4)" if s=='' else 'CONVERT(0x'+s.encode().hex()+' USING utf8mb4)')+' COLLATE utf8mb4_unicode_ci'
key='legacy-hospital-history';where=" WHERE config_key='legacy-hospital-history' AND scope_type='GLOBAL' AND scope_id=0"
old_raw=sql("SELECT HEX(JSON_OBJECT('value',config_value,'status',status,'updated_at',DATE_FORMAT(updated_at,'%Y-%m-%d %H:%i:%s'))) FROM lin_operational_config"+where);old=json.loads(bytes.fromhex(old_raw).decode()) if old_raw else None
assert not old or old['status']!='ACTIVE' or (old['value'] if isinstance(old['value'],dict) else json.loads(old['value'])).get('enabled') is False,'Do not override an active gate'
try:
 with socket.create_connection(('127.0.0.1',18082),timeout=.2):raise AssertionError('Dedicated port occupied')
except ConnectionRefusedError:pass
private=Path.home()/'.local/share/petlis/qa';private.mkdir(mode=0o700,parents=True,exist_ok=True)
process=None;hospitals=[];orders=[];doctor=None;test=None;variety=sex=sample=None;changed=False;proof={}
image='data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jGJkAAAAASUVORK5CYII='
html='<p><strong>合成送检备注</strong></p><table border="1"><tbody><tr><td colspan="2"><p>冷藏样本</p></td></tr></tbody></table><p><img src="'+image+'" width="12" height="12"></p>'
try:
 for suffix in ['own','foreign']:
  hospitals.append(int(sql('INSERT INTO lin_lb_infohospital(HospitalName,HospitalName_en,IsActive,CityId,PayType,amount) VALUES('+lit(prefix+'_'+suffix)+','+lit(prefix+'_'+suffix)+',1,1,1,0);SELECT LAST_INSERT_ID()')))
 password=secrets.token_hex(18)+'A7';salt=secrets.token_hex(5);first=hashlib.md5(password.encode()).hexdigest();hashed=hashlib.md5((first+salt).encode()).hexdigest()
 doctor=int(sql('INSERT INTO lin_user(username,nickname,status,hospital_id,password,salt,loginfailure) VALUES('+lit(prefix)+",'合成医院医生','normal',"+str(hospitals[0])+','+lit(hashed)+','+lit(salt)+',0);SELECT LAST_INSERT_ID()'))
 columns=[(line.split('\t')+['']*5)[:5] for line in sql("SELECT COLUMN_NAME,DATA_TYPE,IS_NULLABLE,IFNULL(COLUMN_DEFAULT,'<NULL>'),EXTRA FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='lin_lb_listdiagnosisproject' ORDER BY ORDINAL_POSITION").splitlines()]
 for hospital in hospitals:
  names=[];values=[];given={'UserNO':lit(prefix+'_'+str(hospital)),'AnimalName':lit('合成动物'),'MasterName':lit('合成畜主'),'DoctorName':lit('合成医师'),'InfoHospitalId':str(hospital),'SampleStatus':'-2','CreateSampleId':'0','TestFee':'0','Remark':lit(html)}
  for name,kind,nullable,default,extra in columns:
   if 'auto_increment' in extra:continue
   if name not in given and (nullable=='YES' or default!='<NULL>'):continue
   assert re.fullmatch('[A-Za-z0-9_]+',name);names.append('`'+name+'`')
   if name in given:value=given[name]
   elif kind in ['int','tinyint','smallint','mediumint','bigint','decimal','double','float']:value='0'
   elif kind in ['datetime','timestamp','date']:value=lit('1900-01-01')
   elif kind in ['varchar','char','text','mediumtext','longtext']:value=lit(prefix)
   else:raise AssertionError('Unexpected fixture field')
   values.append(value)
  orders.append(int(sql('INSERT INTO lin_lb_listdiagnosisproject('+','.join(names)+') VALUES('+','.join(values)+');SELECT LAST_INSERT_ID()')))
 variety=int(sql('INSERT INTO lin_lb_infovariety(Variety_CN,Variety_EN,createtime,updatetime) VALUES('+lit(prefix+'_breed')+','+lit(prefix+'_breed_en')+',100,100);SELECT LAST_INSERT_ID()'))
 sex=int(sql('INSERT INTO lin_lb_dictsex(SexName,SexName_en,IsActive,createtime,updatetime) VALUES('+lit(prefix+'_sex')+','+lit(prefix+'_sex_en')+',0,100,100);SELECT LAST_INSERT_ID()'))
 sample=int(sql('INSERT INTO lin_lb_infosampletype(SampleName,SampleName_en,IsActive,createtime,updatetime) VALUES('+lit(prefix+'_sample')+','+lit(prefix+'_sample_en')+',0,100,100);SELECT LAST_INSERT_ID()'))
 test=int(sql('INSERT INTO lin_lb_infotesttype(TestName,TestNO,Price,CtoG,GtoC,DiscountReal,DiscountNext,TextModel,CreateDt,type,IsActive) VALUES('+lit(prefix)+','+lit(prefix)+",0,0,0,0,0,'',NOW(),1,1);SELECT LAST_INSERT_ID()"))
 sql(f'INSERT INTO lin_lb_infotesttype_price(InfoTestTypeid,price,IsActive) VALUES({test},8.25,1)')
 sql('INSERT INTO lin_operational_config(config_key,config_value,description,updated_by) VALUES('+lit(key)+','+lit(json.dumps({'enabled':True,'writeEnabled':True,'draftEnabled':True,'draftWriteEnabled':True,'draftCreateEnabled':True,'draftItemsEnabled':True,'draftSubmitEnabled':True,'previewVarietyMode':'LEGACY'}))+','+lit(prefix)+','+str(doctor)+") ON DUPLICATE KEY UPDATE config_value=VALUES(config_value),status='ACTIVE'");changed=True
 command='set -a; source "$HOME/.local/share/petlis/mysql/backend.env"; set +a; [[ "$DB_URL" == "jdbc:mysql://127.0.0.1:13306/laboklin?"* ]] || exit 17; export JAVA_HOME="$HOME/.local/share/petlis/tools/Home" MAVEN_HOME="$HOME/.local/share/petlis/tools/libexec" APP_SCHEDULING_ENABLED=false DB_MIGRATIONS_ENABLED=false PDF_STORAGE_PATH="$HOME/.local/share/petlis/reports"; exec ./petlis-backend/mvnw -o -q -f petlis-backend/pom.xml spring-boot:run -Dspring-boot.run.profiles=dev -Dspring-boot.run.arguments=--server.port=18082'
 fd=os.open(private/(prefix+'.log'),os.O_WRONLY|os.O_CREAT|os.O_EXCL|os.O_NOFOLLOW,0o600)
 with os.fdopen(fd,'w') as log:process=subprocess.Popen(['/bin/zsh','-c',command],cwd=ROOT,stdout=log,stderr=subprocess.STDOUT,start_new_session=True)
 for attempt in range(60):
  assert process.poll() is None,'Owned backend exited'
  try:
   with staff.opener.open(staff.config.BASE+'/auth/capabilities',timeout=1) as r:
    if r.status==200:break
  except Exception:time.sleep(.5)
 else:raise AssertionError('Readiness timeout')
 session=staff.call('/auth/password/login',{'account':prefix,'password':password});assert set(session['roles'])=={'DOCTOR'}
 all_names=[c[0] for c in columns];assert all(re.fullmatch('[A-Za-z0-9_]+',n) for n in all_names)
 expression='JSON_OBJECT('+','.join("'"+n+"',`"+n+'`' for n in all_names)+')'
 def parent(order):return json.loads(bytes.fromhex(sql('SELECT HEX(CAST('+expression+' AS CHAR)) FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId='+str(order))).decode())
 before=[parent(i) for i in orders]
 funds_before=sql(f"SELECT HEX(JSON_OBJECT('money',money,'score',score)) FROM lin_user WHERE id={doctor}")+'|'+sql(f'SELECT HEX(JSON_OBJECT(\'amount\',amount)) FROM lin_lb_infohospital WHERE InfoHospitalId={hospitals[0]}')
 f={'token':session['token'],'user':session['user'],'hospital':hospitals[0],'foreignHospital':hospitals[1],'order':orders[0],'foreignOrder':orders[1],'image':image,'prefix':prefix,'test':test,'variety':variety,'sex':sex,'sample':sample}
 result=subprocess.run(['node','scripts/editor-parity/hospital-draft-flow-live.js'],cwd=ROOT,input=json.dumps(f),capture_output=True,text=True,timeout=100);assert result.returncode==0,'Chrome acceptance failed: '+result.stderr[-1300:];proof=json.loads(result.stdout)
 created=proof['createdOrder'];row=parent(created)
 assert row['InfoHospitalId']==hospitals[0] and row['SampleStatus']==0 and row['UserNO']=='' and row['CreateSampleId']==0 and row['TestFee']==0 and row['IsPayMoney']==0
 assert row['AnimalTypeId']==variety and row['AnimalType']==prefix+'_breed' and row['AnimalType_en']==prefix+'_breed_en' and row['DictSexId']==sex and row['InfoSampleTypeId']==sample
 assert sql(f"SELECT HEX(JSON_OBJECT('money',money,'score',score)) FROM lin_user WHERE id={doctor}")+'|'+sql(f'SELECT HEX(JSON_OBJECT(\'amount\',amount)) FROM lin_lb_infohospital WHERE InfoHospitalId={hospitals[0]}')==funds_before
 assert row['MasterName']==prefix+'_owner' and row['MasterName_en']==prefix+'_owner' and row['AnimalName']==prefix+'_updated' and row['Remark']==''
 assert parent(orders[0])==before[0] and parent(orders[1])==before[1]
 for resource,keyname in [('LEGACY_HOSPITAL_DRAFT_ITEMS','addRequestKey'),('LEGACY_HOSPITAL_DRAFT_SUBMIT','submitRequestKey')]:
  assert sql('SELECT COUNT(*) FROM lin_audit_record WHERE actor_user_id='+str(doctor)+' AND resource_type='+lit(resource)+' AND resource_id='+lit(str(created))+' AND request_id='+lit(proof[keyname]))=='1'
 assert sql(f'SELECT COUNT(*) FROM lin_lb_diagnosisdetail WHERE ListDiagnosisProjectId={created}')=='1'
 assert sql(f'SELECT CAST(TestPrice AS CHAR) FROM lin_lb_diagnosisdetail WHERE ListDiagnosisProjectId={created}')=='8.25'
 assert sql(f'SELECT COUNT(*) FROM lin_order_extension WHERE order_id={created}')=='0'
 assert sql(f'SELECT COUNT(*) FROM lin_lb_notification WHERE order_id={created} AND old_status=-2 AND new_status=0')=='1'
 proof_path=private/(prefix+'-proof.json');fd=os.open(proof_path,os.O_WRONLY|os.O_CREAT|os.O_EXCL|os.O_NOFOLLOW,0o600)
 with os.fdopen(fd,'w') as out:json.dump(proof,out)
 proof.update(originalSubmitOnlyPendingState=True,originalPricePreserved=True,unrelatedOrdersUnchanged=True,oneAddAndSubmitAudit=True,oneOriginalStatusNotification=True,noWorkflowAdoptionOrPayment=True,twoFundBalancesAndScoreUnchanged=True)
finally:
 if process and process.poll() is None:
  os.killpg(process.pid,signal.SIGTERM)
  try:process.wait(timeout=8)
  except subprocess.TimeoutExpired:os.killpg(process.pid,signal.SIGKILL);process.wait(timeout=5)
 if changed:
  if old:
   value=old['value'];value=json.dumps(value,ensure_ascii=False) if isinstance(value,dict) else value
   sql('UPDATE lin_operational_config SET config_value='+lit(value)+',status='+lit(old['status'])+',updated_at='+lit(old['updated_at'])+where)
  else:sql('DELETE FROM lin_operational_config'+where+' AND description='+lit(prefix))
 if doctor:
  created_ids=sql("SELECT resource_id FROM lin_audit_record WHERE actor_user_id="+str(doctor)+" AND resource_type='LEGACY_HOSPITAL_DRAFT_CREATE'").splitlines()
  for value in created_ids:
   assert value.isdecimal();order=int(value)
   assert sql(f"SELECT CONCAT(InfoHospitalId,'|',MasterName,'|',UserNO) FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId={order}")==str(hospitals[0])+'|'+prefix+'_owner'+'|'
   orders.append(order)
  detail_ids=set()
  for order in orders:
   for value in sql(f'SELECT DiagnosisDetailId FROM lin_lb_diagnosisdetail WHERE ListDiagnosisProjectId={order}').splitlines():detail_ids.add(int(value))
   for value in sql("SELECT JSON_UNQUOTE(JSON_EXTRACT(after_json,'$.detailId')) FROM lin_audit_record WHERE actor_user_id="+str(doctor)+" AND resource_type='LEGACY_HOSPITAL_DRAFT_ITEMS' AND resource_id="+lit(str(order))).splitlines():
    assert value.isdecimal();detail_ids.add(int(value))
  for detail in detail_ids:sql(f'DELETE FROM lin_lb_diagnosisdetail_info WHERE diagnosisdetail_id={detail}')
 for index,order in enumerate(orders):
  hospital=hospitals[min(index,1)] if index<2 else hospitals[0]
  expected=prefix+'_'+str(hospital) if index<2 else ''
  assert sql(f'SELECT UserNO FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId={order}')==expected
  sql("DELETE FROM lin_audit_record WHERE resource_type IN ('LEGACY_HOSPITAL_DRAFT','LEGACY_HOSPITAL_DRAFT_CREATE','LEGACY_HOSPITAL_DRAFT_ITEMS','LEGACY_HOSPITAL_DRAFT_SUBMIT') AND resource_id="+lit(str(order))+f' AND actor_user_id={doctor};DELETE FROM lin_lb_diagnosisdetail WHERE ListDiagnosisProjectId={order};DELETE FROM lin_lb_notification WHERE order_id={order};DELETE FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId={order} AND UserNO='+lit(expected))
 if variety:sql(f'DELETE FROM lin_lb_infovariety WHERE IID={variety} AND Variety_CN='+lit(prefix+'_breed'))
 if sex:sql(f'DELETE FROM lin_lb_dictsex WHERE DictSexId={sex} AND SexName='+lit(prefix+'_sex'))
 if sample:sql(f'DELETE FROM lin_lb_infosampletype WHERE InfoSampleTypeId={sample} AND SampleName='+lit(prefix+'_sample'))
 if test:sql(f'DELETE FROM lin_lb_infotesttype_price WHERE InfoTestTypeid={test};DELETE FROM lin_lb_infotesttype WHERE InfoTestTypeId={test} AND TestName='+lit(prefix))
 if doctor:sql(f'DELETE FROM lin_doctor_profile WHERE user_id={doctor};DELETE FROM lin_user WHERE id={doctor} AND username='+lit(prefix))
 for hospital in hospitals:sql(f'DELETE FROM lin_lb_infohospital WHERE InfoHospitalId={hospital} AND HospitalName LIKE '+lit(prefix+'_%'))
assert sql("SELECT HEX(JSON_OBJECT('value',config_value,'status',status,'updated_at',DATE_FORMAT(updated_at,'%Y-%m-%d %H:%i:%s'))) FROM lin_operational_config"+where)==old_raw
try:
 with socket.create_connection(('127.0.0.1',18082),timeout=.2):raise AssertionError('Owned port still open')
except ConnectionRefusedError:pass
assert sql('SELECT COUNT(*) FROM lin_user WHERE username='+lit(prefix))=='0';assert sql('SELECT COUNT(*) FROM lin_lb_listdiagnosisproject WHERE UserNO LIKE '+lit(prefix+'_%'))=='0';assert sql('SELECT COUNT(*) FROM lin_lb_infohospital WHERE HospitalName LIKE '+lit(prefix+'_%'))=='0'
assert sql('SELECT COUNT(*) FROM lin_lb_listdiagnosisproject WHERE MasterName='+lit(prefix+'_owner'))=='0'
proof.update(gateRestored=True,fixturesRemoved=True,backendStopped=True);print(json.dumps(proof))
